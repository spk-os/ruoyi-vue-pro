package cn.iocoder.yudao.module.spkdelivery.service.integration;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.databind.JsonNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * SPK-OS Omnigent 会话代理 —— 给前端 iframe/SSE 转发 Omnigent session 视图。
 * <p>
 * Omnigent 业务端点需 ap_session cookie 鉴权（accounts 模式），浏览器无法直连，
 * 故由后端持 cookie 代理：GET session 元数据 + SSE 转发 items 流（5min）。
 * 设计文档 §4.6：iframe 嵌入 Omnigent session，经后端代理注入鉴权，RBAC 过滤。
 *
 * @author SPK-OS
 */
@Slf4j
@Service
public class SpkOmnigentProxyService {

    @Value("${spk-delivery.omnigent.base-url:http://192.168.56.101:6767}")
    private String baseUrl;
    @Value("${spk-delivery.omnigent.cookie:}")
    private String cookie;
    @Value("${spk-delivery.omnigent.poll-interval-ms:2000}")
    private long pollIntervalMs;

    // 强制 HTTP/1.1：Java HttpClient 默认 HTTP/2，对 Omnigent(uvicorn/HTTP1.1) 发 h2c upgrade
    // 会被拒「Invalid HTTP request received」(400)，必须显式降版本（与 OmnigentAdapter 一致）。
    private final HttpClient http = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10)).build();
    /** SSE 转发用的单线程调度池（session 级轻量轮询） */
    private final ScheduledExecutorService ssePool = Executors.newScheduledThreadPool(2, r -> {
        Thread t = new Thread(r, "omnigent-sse");
        t.setDaemon(true);
        return t;
    });

    /** 代理 GET /v1/sessions/{sid}：返回 session 元数据 JSON。 */
    public String getSession(String sid) {
        try {
            return getJson("/v1/sessions/" + sid);
        } catch (Exception e) {
            log.warn("[getSession][sid={} 失败：{}]", sid, e.getMessage());
            return "{\"error\":\"" + truncate(e.getMessage(), 200) + "\"}";
        }
    }

    /**
     * SSE 转发 /v1/sessions/{sid}/items：定时轮询并推送 assistant 文本事件，5min 超时。
     * <p>
     * Omnigent 无原生 SSE 流暴露给外部鉴权，故用轮询 + SseEmitter 模拟实时流；
     * 客户端按需断开，后端 scheduled 任务在 emitter complete/timeout 后自停。
     */
    public SseEmitter streamSession(String sid) {
        // 5min 超时（与设计文档 iframe token 5min 对齐）
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);
        final int[] seq = {0};
        final String[] lastText = {null};
        Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    seq[0]++;
                    String body = getJson("/v1/sessions/" + sid + "/items?limit=50");
                    String text = extractLatestAssistant(body);
                    if (text != null && !text.equals(lastText[0])) {
                        lastText[0] = text;
                        emitter.send(SseEmitter.event()
                                .id(String.valueOf(seq[0]))
                                .name("assistant")
                                .data(text));
                    }
                    // 周期心跳，便于前端保活
                    emitter.send(SseEmitter.event().name("tick").data(seq[0]));
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
            }
        };
        java.util.concurrent.ScheduledFuture<?>[] handle = new java.util.concurrent.ScheduledFuture<?>[1];
        handle[0] = ssePool.scheduleAtFixedRate(task, 0, pollIntervalMs, TimeUnit.MILLISECONDS);
        emitter.onCompletion(() -> { if (handle[0] != null) handle[0].cancel(false); });
        emitter.onTimeout(() -> { if (handle[0] != null) handle[0].cancel(false); emitter.complete(); });
        emitter.onError(e -> { if (handle[0] != null) handle[0].cancel(false); });
        return emitter;
    }

    /** 从 items 列表 JSON 抽取最新一条 assistant 文本。 */
    private String extractLatestAssistant(String itemsJson) {
        try {
            JsonNode root = JsonUtils.parseTree(itemsJson);
            JsonNode items = root.has("items") ? root.get("items") : root;
            if (items == null || !items.isArray() || items.isEmpty()) {
                return null;
            }
            for (int i = items.size() - 1; i >= 0; i--) {
                JsonNode it = items.get(i);
                JsonNode role = it.get("role");
                JsonNode type = it.get("type");
                boolean isAssistant = (role != null && "assistant".equals(role.asText()))
                        || (type != null && "assistant".equals(type.asText()));
                if (!isAssistant) {
                    continue;
                }
                JsonNode content = it.get("content");
                if (content != null && content.isTextual() && !content.asText().isBlank()) {
                    return content.asText();
                }
                // content 可能是数组结构（[{type:text,text:...}]）
                if (content != null && content.isArray()) {
                    for (JsonNode p : content) {
                        JsonNode t = p.get("text");
                        if (t != null && t.isTextual() && !t.asText().isBlank()) {
                            return t.asText();
                        }
                    }
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private String getJson(String path) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Accept", "application/json")
                .header("Cookie", cookieHeader())
                .timeout(Duration.ofSeconds(15)).GET().build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400) {
            throw new RuntimeException("GET " + path + " -> " + resp.statusCode() + "：" + truncate(resp.body(), 200));
        }
        return resp.body();
    }

    /** 兼容整段 cookie 与仅 ap_session 值两种填法。 */
    private String cookieHeader() {
        if (cookie == null || cookie.isBlank()) {
            return "";
        }
        String v = cookie.trim();
        return v.startsWith("ap_session=") ? v : "ap_session=" + v;
    }

    private static String truncate(String s, int max) {
        return s == null ? "" : (s.length() <= max ? s : s.substring(0, max));
    }
}
