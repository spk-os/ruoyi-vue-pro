package cn.iocoder.yudao.module.spkdelivery.service.agent;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.spkdelivery.enums.SpkAgentTaskStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SPK-OS HermesAdapter —— 经 Hermes（hermes-agent / hermes-studio-api）派发的 FrameworkAdapter 实现
 * （设计 §5.3 D2）。
 * <p>
 * 与 NativeAiAdapter（yudao-module-ai 内核）并列的第二个 runtime。dispatchTask 调 Hermes
 * {@code POST /api/chat-run/runs}（hermes-studio-api HTTP），携带 capability_tags 让 Hermes 侧按能力选模型；
 * 同步轮询 {@code GET /api/chat-run/runs/{runId}} 取产物。异步 callback 模式留接口位（taskId=runId，
 * 由 {@link SpkAgentTaskService#callback} 回写）。
 *
 * <p><b>部署</b>：默认不启用（设计 OPEN-2「P2 加 HermesAdapter」）。需在 application.yaml 配
 * {@code spk-delivery.hermes.base-url} 且 {@code spk-delivery.execution.adapter=hermes} 时生效。
 * {@link #healthCheck()} 探活失败 → Router 自动回退 native-ai，不阻断流程。
 *
 * <p><b>降级链</b>（设计 §15.5.5）：OmnigentAdapter 不可用 → HermesAdapter → NativeAiAdapter。
 *
 * <p><b>API 对齐</b>：请求/响应字段按设计 §5.3 契约；与 Hermes 实际联调时如字段名有出入，
 * 在本 Adapter 内做映射，不外泄到 Router。Hermes 当前未起（hermes-agent 8642 stopped）时
 * healthCheck 返回 false，dispatchTask 抛 RuntimeException 由上层降级。
 *
 * @author SPK-OS
 */
@Slf4j
@Component("hermes")
@ConditionalOnProperty(prefix = "spk-delivery.hermes", name = "base-url")
public class HermesAdapter implements FrameworkAdapter {

    public static final String NAME = "hermes";

    @Value("${spk-delivery.hermes.base-url:}")
    private String baseUrl;
    @Value("${spk-delivery.hermes.token:}")
    private String token;
    /** 同步轮询最长等待（秒）；超时抛异常由上层降级 NativeAi/Hermes 重试策略。 */
    @Value("${spk-delivery.hermes.timeout-ms:120000}")
    private long timeoutMs;
    @Value("${spk-delivery.hermes.poll-interval-ms:3000}")
    private long pollIntervalMs;

    private final HttpClient http = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10)).build();

    @Override
    public SpkAgentDispatchResult dispatchTask(SpkAgentDispatchReq req) {
        if (!healthCheck()) {
            throw new RuntimeException("Hermes 不可用（base-url=" + baseUrl + " 探活失败），上层应降级 native-ai");
        }
        try {
            String runId = createRun(req);
            log.info("[dispatchTask][hermes instanceId={} nodeKey={} runId={}]", req.getInstanceId(), req.getNodeKey(), runId);
            String resultText = pollRunResult(runId);
            return new SpkAgentDispatchResult()
                    .setTaskId(runId)
                    .setConversationId(0L)
                    .setResult(resultText)
                    .setStatus(SpkAgentTaskStatusEnum.DONE.getLabel());
        } catch (Exception e) {
            log.error("[dispatchTask][hermes 失败 instanceId={} nodeKey={}]",
                    req.getInstanceId(), req.getNodeKey(), e);
            throw new RuntimeException("Hermes 派发失败：" + e.getMessage(), e);
        }
    }

    /**
     * 创建 Hermes chat-run：POST /api/chat-run/runs。
     * 携带 capability_tags（设计 §5.3）让 Hermes 侧按能力选模型；idempotency_key 用 instanceId+nodeKey。
     */
    private String createRun(SpkAgentDispatchReq req) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("prompt", req.getPrompt() == null ? "" : req.getPrompt());
        body.put("node_key", req.getNodeKey() == null ? "" : req.getNodeKey());
        body.put("instance_id", req.getInstanceId());
        body.put("idempotency_key", (req.getInstanceId() == null ? "" : req.getInstanceId())
                + "#" + (req.getNodeKey() == null ? "" : req.getNodeKey()));
        String resp = postJson("/api/chat-run/runs", JsonUtils.toJsonString(body));
        JsonNode node = JsonUtils.parseTree(resp);
        JsonNode idNode = firstNonNull(node, "run_id", "id", "runId");
        if (idNode == null || idNode.isNull()) {
            throw new RuntimeException("Hermes 创建 run 无 id 响应：" + truncate(resp, 500));
        }
        return idNode.asText();
    }

    /**
     * 同步轮询 run 终态：GET /api/chat-run/runs/{runId}。
     * status=succeeded/failed/terminal 时取 result；超时抛异常。
     */
    private String pollRunResult(String runId) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            String resp = getJson("/api/chat-run/runs/" + runId);
            JsonNode node = JsonUtils.parseTree(resp);
            String status = textOf(firstNonNull(node, "status", "state"));
            if ("failed".equalsIgnoreCase(status) || "error".equalsIgnoreCase(status)) {
                throw new RuntimeException("Hermes run 失败：" + truncate(resp, 300));
            }
            if (isTerminal(status)) {
                JsonNode result = firstNonNull(node, "result", "output", "content");
                return result != null && !result.isNull() ? result.asText() : truncate(resp, 1000);
            }
            Thread.sleep(pollIntervalMs);
        }
        throw new RuntimeException("Hermes run 轮询超时（>" + timeoutMs + "ms）runId=" + runId);
    }

    private static boolean isTerminal(String status) {
        if (status == null) return false;
        String s = status.toLowerCase();
        return s.contains("success") || s.equals("done") || s.equals("completed") || s.equals("succeeded") || s.equals("terminal");
    }

    private static JsonNode firstNonNull(JsonNode node, String... keys) {
        if (node == null) return null;
        for (String k : keys) {
            JsonNode v = node.get(k);
            if (v != null && !v.isNull()) return v;
        }
        return null;
    }

    @Override
    public String getTaskStatus(String taskId) {
        if (taskId == null || taskId.isBlank()) {
            return SpkAgentTaskStatusEnum.RUNNING.getLabel();
        }
        try {
            String resp = getJson("/api/chat-run/runs/" + taskId);
            JsonNode node = JsonUtils.parseTree(resp);
            String status = textOf(firstNonNull(node, "status", "state"));
            return isTerminal(status) ? SpkAgentTaskStatusEnum.DONE.getLabel()
                    : SpkAgentTaskStatusEnum.RUNNING.getLabel();
        } catch (Exception e) {
            log.warn("[getTaskStatus][runId={} 取状态失败：{}]", taskId, e.getMessage());
            return SpkAgentTaskStatusEnum.RUNNING.getLabel();
        }
    }

    @Override
    public String getTaskResult(String taskId) {
        if (taskId == null || taskId.isBlank()) return null;
        try {
            String resp = getJson("/api/chat-run/runs/" + taskId);
            JsonNode node = JsonUtils.parseTree(resp);
            JsonNode result = firstNonNull(node, "result", "output", "content");
            return result != null && !result.isNull() ? result.asText() : null;
        } catch (Exception e) {
            log.warn("[getTaskResult][runId={} 取产物失败：{}]", taskId, e.getMessage());
            return null;
        }
    }

    @Override
    public boolean cancelTask(String taskId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/chat-run/runs/" + taskId))
                    .timeout(Duration.ofMillis(timeoutMs)).DELETE().build();
            applyAuth(HttpRequest.newBuilder().uri(URI.create(baseUrl + "/api/chat-run/runs/" + taskId)));
            HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString());
            return resp.statusCode() < 400;
        } catch (Exception e) {
            log.warn("[cancelTask][hermes 取消 runId={} 失败：{}]", taskId, e.getMessage());
            return false;
        }
    }

    @Override
    public List<Map<String, Object>> listAvailableAgents() {
        try {
            String resp = getJson("/api/agents");
            JsonNode node = JsonUtils.parseTree(resp);
            JsonNode arr = firstNonNull(node, "agents", "data", "items");
            List<Map<String, Object>> list = new ArrayList<>();
            if (arr != null && arr.isArray()) {
                for (JsonNode a : arr) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", textOf(a.get("id")));
                    m.put("name", textOf(a.get("name")));
                    m.put("code", textOf(a.get("code")));
                    list.add(m);
                }
            }
            return list;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @Override
    public boolean healthCheck() {
        if (baseUrl == null || baseUrl.isBlank()) return false;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/health"))
                    .timeout(Duration.ofSeconds(5)).GET().build();
            HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 400) return false;
            JsonNode node = JsonUtils.parseTree(resp.body());
            String status = textOf(firstNonNull(node, "status", "ok"));
            return "ok".equalsIgnoreCase(status) || resp.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    // ===== HTTP 工具（与 OmnigentAdapter 同构，HTTP/1.1 + token 鉴权） =====

    private String postJson(String path, String jsonBody) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .timeout(Duration.ofMillis(timeoutMs));
        applyAuth(b);
        HttpResponse<String> resp = http.send(b.POST(HttpRequest.BodyPublishers.ofString(jsonBody)).build(),
                HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400) {
            throw new RuntimeException("POST " + path + " -> " + resp.statusCode() + "：" + truncate(resp.body(), 500));
        }
        return resp.body();
    }

    private String getJson(String path) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Accept", "application/json")
                .timeout(Duration.ofMillis(timeoutMs));
        applyAuth(b);
        HttpResponse<String> resp = http.send(b.GET().build(), HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400) {
            throw new RuntimeException("GET " + path + " -> " + resp.statusCode() + "：" + truncate(resp.body(), 500));
        }
        return resp.body();
    }

    private void applyAuth(HttpRequest.Builder b) {
        if (token != null && !token.isBlank()) {
            String v = token.trim();
            if (v.toLowerCase().startsWith("bearer ")) {
                b.header("Authorization", v);
            } else {
                b.header("Authorization", "Bearer " + v);
            }
        }
    }

    private static String textOf(JsonNode node) {
        return (node == null || node.isNull()) ? null : node.asText();
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }
}
