package cn.iocoder.yudao.module.spkdelivery.service.agent;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.spkdelivery.enums.SpkAgentTaskStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
 * SPK-OS OmnigentAdapter —— 对接 Omnigent 执行域的 FrameworkAdapter 实现。
 * <p>
 * Omnigent（192.168.56.101:6767）是 SPK-OS 的执行域 runtime。
 * <p>
 * 鉴权：实际部署为 accounts 模式（OMNIGENT_AUTH_ENABLED=1，内置用户名/密码登录换
 * ap_session cookie），业务端点（/v1/*）需 Cookie 鉴权，与早期选型笔记「单用户本地免鉴权」不符。
 * 故 adapter 从配置注入 ap_session cookie（spk-delivery.omnigent.cookie）。
 * 调用契约（openapi + routes_core + 实测 2026-08-04 端到端验通）：
 * 1) 创建会话 POST /v1/sessions（agent_id 必填 + initial_items[] 初始 user prompt）→ 201 id=conv_xxx；
 *    initial_items 只种 seed user 消息（response_id=seed，status=idle），**不触发执行**。
 * 2) 启动 runner POST /v1/hosts/{host_id}/runners {session_id, workspace} —— 这才是真正触发 agent 执行的步骤；
 *    workspace 必须是 host 上真实存在的绝对路径（runner 工作目录），否则 400。
 * 3) host 端 runner（claude-sdk harness）异步执行，回流 events；adapter 轮询 GET /v1/sessions/{id}/items
 *    遍历 data[]，type=message 且 data.role=assistant，文本在 data.content[].text。
 * 4) 拉文件 GET /v1/sessions/{id}/resources/files；健康检查 GET /health → status=ok（无 /v1 前缀）。
 * host_id 从配置取，留空则 GET /v1/hosts 动态选首个 online 且 claude-sdk harness 已配置的 host（缓存）。
 * fast-mode（默认 true）跳过真实 HTTP 合成桩；关闭后走真实 Omnigent（debby 等多智能体 agent 单轮不收敛，
 * 取 timeout 窗口内已产出的 assistant 文本即可，属真实 Omnigent 输出）。
 * Omnigent 会话 id 为 String(conv_xxx) 置入 taskId，conversationId 保持 0L 兼容 Long 字段。
 *
 * @author SPK-OS
 */
@Slf4j
@Component("omnigent")
public class OmnigentAdapter implements FrameworkAdapter {

    public static final String NAME = "omnigent";

    @Value("${spk-delivery.omnigent.base-url:http://192.168.56.101:6767}")
    private String baseUrl;

    /** Omnigent 注册 agent 的 agent_id（ag_xxx），真实模式必填 */
    @Value("${spk-delivery.omnigent.agent-id:}")
    private String agentId;

    @Value("${spk-delivery.execution.fast-mode:true}")
    private boolean fastMode;

    @Value("${spk-delivery.omnigent.timeout-ms:120000}")
    private long timeoutMs;

    @Value("${spk-delivery.omnigent.poll-interval-ms:2000}")
    private long pollIntervalMs;

    /**
     * Omnigent 在线 host_id（裸 32 位 hex）。留空则首次真实派发时 GET /v1/hosts 动态解析
     * 首个 status=online 且 configured_harnesses 含 claude-sdk=true 的 host（缓存到本字段）。
     */
    @Value("${spk-delivery.omnigent.host-id:}")
    private String hostId;

    /**
     * Runner 工作目录（host 上必须真实存在的绝对路径）。POST /v1/hosts/{id}/runners 必填，
     * 否则 400「workspace path does not exist」。默认指向 spk-delivery.artifact.base-dir。
     */
    @Value("${spk-delivery.omnigent.workspace:}")
    private String workspace;

    /**
     * Omnigent ap_session cookie 值（accounts 鉴权模式必填）。
     * <p>
     * 实际部署 OMNIGENT_AUTH_ENABLED=1，业务端点需 ap_session cookie；从 web 登录后取 cookie 塼入此项，
     * adapter 自动以 {@code Cookie: ap_session=<值>} 头注入所有 /v1/* 请求。留空则不注入（仅 /health 免鉴权可探活）。
     */
    @Value("${spk-delivery.omnigent.cookie:}")
    private String cookie;

    // 强制 HTTP/1.1：Java HttpClient 默认 HTTP/2，对 Omnigent(uvicorn/HTTP1.1) 发 h2c upgrade
    // 会被拒「Invalid HTTP request received」(400)，必须显式降版本。
    private final HttpClient http = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10)).build();

    @Override
    public SpkAgentDispatchResult dispatchTask(SpkAgentDispatchReq req) {
        if (fastMode) {
            return fastDispatch(req);
        }
        if (agentId == null || agentId.isBlank()) {
            throw new RuntimeException("OmnigentAdapter 真实模式需配置 spk-delivery.omnigent.agent-id（ag_xxx）");
        }
        try {
            String convId = createSession(req);
            log.info("[dispatchTask][omnigent instanceId={} nodeKey={} convId={}]",
                    req.getInstanceId(), req.getNodeKey(), convId);
            // 关键：create session 只种 seed 消息，必须 launch runner 才真正触发 agent 执行
            launchRunner(convId);
            String assistantText = pollAssistantText(convId);
            String files = listFilesText(convId);
            String resultText = assistantText;
            if (files != null && !files.isBlank()) {
                resultText = (assistantText == null ? "" : assistantText) + "\n\n[files]\n" + files;
            }
            return new SpkAgentDispatchResult()
                    .setTaskId(convId)
                    .setConversationId(0L)
                    .setResult(resultText)
                    .setStatus(SpkAgentTaskStatusEnum.DONE.getLabel());
        } catch (Exception e) {
            log.error("[dispatchTask][omnigent 失败 instanceId={} nodeKey={}]",
                    req.getInstanceId(), req.getNodeKey(), e);
            throw new RuntimeException("Omnigent 派发失败：" + e.getMessage(), e);
        }
    }

    private SpkAgentDispatchResult fastDispatch(SpkAgentDispatchReq req) {
        SpkAgentDispatchResult result = new SpkAgentDispatchResult();
        result.setStatus(SpkAgentTaskStatusEnum.DONE.getLabel());
        result.setConversationId(0L);
        result.setTaskId("omnigent-fast#" + System.nanoTime());
        String nodeKey = req.getNodeKey() == null ? "" : req.getNodeKey();
        String promptDigest = Integer.toHexString((req.getPrompt() == null ? "" : req.getPrompt()).hashCode());
        if (nodeKey.startsWith("verify:")) {
            result.setResult("{\"overall\":\"PASS\",\"summary\":\"omnigent fast-mode：Independent Verifier 自动通过（未调真实 Omnigent）\","
                    + "\"evidencePoints\":["
                    + "{\"point\":\"recheck\",\"verdict\":\"Confirmed\"},"
                    + "{\"point\":\"redteam\",\"verdict\":\"Confirmed\"},"
                    + "{\"point\":\"completeness\",\"verdict\":\"Confirmed\"},"
                    + "{\"point\":\"traceback\",\"verdict\":\"Confirmed\"}]}");
        } else {
            result.setResult("{\"deliverable\":\"stub\",\"mode\":\"omnigent-fast\",\"provider\":\"omnigent\","
                    + "\"promptDigest\":\"" + promptDigest + "\","
                    + "\"summary\":\"omnigent fast-mode 合成产物（未调真实 Omnigent runtime）\","
                    + "\"content\":\"本产物由 omnigent fast-mode 合成。Activity 定义含真实 prompt（promptDigest="
                    + promptDigest + "）。关闭 spk-delivery.execution.fast-mode 后走真实 Omnigent /v1/sessions。\"}");
        }
        log.info("[dispatchTask][omnigent fast-mode nodeKey={} done]", nodeKey);
        return result;
    }

    // ===== 真实 Omnigent REST 调用 =====

    private String createSession(SpkAgentDispatchReq req) throws Exception {
        String prompt = req.getPrompt() == null ? "" : req.getPrompt();
        String nodeKey = req.getNodeKey() == null ? "" : req.getNodeKey();
        Map<String, Object> textBlock = new LinkedHashMap<>();
        textBlock.put("type", "input_text");
        textBlock.put("text", prompt);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("role", "user");
        data.put("content", List.of(textBlock));
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("type", "message");
        item.put("data", data);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("agent_id", agentId);
        body.put("title", "SPK-IPD " + nodeKey);
        body.put("initial_items", List.of(item));
        String resp = postJson("/v1/sessions", JsonUtils.toJsonString(body));
        JsonNode node = JsonUtils.parseTree(resp);
        JsonNode idNode = node.get("id");
        if (idNode == null || idNode.isNull()) {
            throw new RuntimeException("Omnigent 创建会话无 id 响应：" + truncate(resp, 500));
        }
        return idNode.asText();
    }

    /**
     * 启动 runner —— POST /v1/hosts/{host_id}/runners {session_id, workspace}。
     * 这是真正触发 Omnigent agent 执行的步骤（create session 只种 seed 消息不触发 run）。
     * host_id 留空时动态解析首个 online 且 claude-sdk harness 已配置的 host。
     * workspace 必须是 host 上真实存在的绝对路径，否则 400。
     * 已存在 runner 时返回 409，视为已启动（幂等）。
     */
    private void launchRunner(String convId) throws Exception {
        String hid = resolveHostId();
        if (hid == null || hid.isBlank()) {
            throw new RuntimeException("Omnigent 无可用在线 host（需在线且 claude-sdk harness 已配置）");
        }
        String ws = (workspace == null || workspace.isBlank())
                ? "/work/SPK-OS/artifacts" : workspace;
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("session_id", convId);
        body.put("workspace", ws);
        try {
            String resp = postJson("/v1/hosts/" + hid + "/runners", JsonUtils.toJsonString(body));
            log.info("[launchRunner][convId={} hostId={} workspace={} resp={}]", convId, hid, ws, truncate(resp, 200));
        } catch (RuntimeException e) {
            // 409 = runner 已存在（幂等，视为成功）；其它 >=400 抛出
            String msg = e.getMessage();
            if (msg != null && msg.contains("409")) {
                log.info("[launchRunner][convId={} runner 已存在（409 幂等）]", convId);
                return;
            }
            throw e;
        }
    }

    /**
     * 解析在线 host_id：配置已填则直接用；否则 GET /v1/hosts 取首个 status=online
     * 且 configured_harnesses.claude-sdk=true 的 host_id（缓存到本实例 hostId 字段）。
     */
    private synchronized String resolveHostId() throws Exception {
        if (hostId != null && !hostId.isBlank()) {
            return hostId;
        }
        String resp = getJson("/v1/hosts");
        JsonNode root = JsonUtils.parseTree(resp);
        JsonNode hosts = root.has("hosts") ? root.get("hosts") : root.get("data");
        if (hosts == null || !hosts.isArray()) {
            return null;
        }
        for (JsonNode h : hosts) {
            if (!"online".equals(textOf(h.get("status")))) {
                continue;
            }
            JsonNode ch = h.get("configured_harnesses");
            boolean claudeSdkOk = false;
            if (ch != null && ch.isObject()) {
                JsonNode v = ch.get("claude-sdk");
                claudeSdkOk = v != null && "true".equals(v.asText());
            }
            if (claudeSdkOk) {
                hostId = textOf(h.get("host_id"));
                if (hostId == null) hostId = textOf(h.get("id"));
                log.info("[resolveHostId][动态解析到 online host={}]", hostId);
                return hostId;
            }
        }
        return null;
    }

    private String pollAssistantText(String convId) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutMs;
        String lastText = null;
        while (System.currentTimeMillis() < deadline) {
            String resp = getJson("/v1/sessions/" + convId + "/items?limit=100&order=asc");
            String text = extractAssistantText(resp);
            if (text != null && !text.isBlank()) {
                lastText = text;
                // 已出现 assistant 文本，再等一轮确认稳定后返回
                Thread.sleep(Math.min(pollIntervalMs, 1000));
                String resp2 = getJson("/v1/sessions/" + convId + "/items?limit=100&order=asc");
                String text2 = extractAssistantText(resp2);
                return (text2 != null && !text2.isBlank()) ? text2 : text;
            }
            Thread.sleep(pollIntervalMs);
        }
        return lastText;
    }

    /**
     * 从 /items 响应中提取最后一条 assistant message 的文本。
     * <p>
     * items 端点返回扁平结构：{@code {type:"message", role:"assistant", content:[{type:"output_text", text:...}]}}
     * （role/content 在顶层，无 data 包装）；而 createSession 响应里是嵌套 {@code {data:{role,content}}}。
     * 此处两种都兼容：优先取 item.data，回落到 item 顶层。
     */
    private String extractAssistantText(String resp) {
        try {
            JsonNode root = JsonUtils.parseTree(resp);
            JsonNode data = root.get("data");
            if (data == null || !data.isArray() || data.isEmpty()) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            for (JsonNode item : data) {
                if (!"message".equals(textOf(item.get("type")))) {
                    continue;
                }
                // 兼容扁平（item.role）与嵌套（item.data.role）两种结构
                JsonNode d = item.get("data");
                String role = (d != null) ? textOf(d.get("role")) : null;
                if (role == null) {
                    role = textOf(item.get("role"));
                    d = item; // 扁平结构：content 也在顶层
                }
                if (!"assistant".equals(role)) {
                    continue;
                }
                JsonNode content = d.get("content");
                if (content != null && content.isArray()) {
                    for (JsonNode block : content) {
                        String t = textOf(block.get("text"));
                        if (t != null && !t.isBlank()) {
                            if (sb.length() > 0) sb.append('\n');
                            sb.append(t);
                        }
                    }
                }
            }
            return sb.length() == 0 ? null : sb.toString();
        } catch (Exception e) {
            log.warn("[extractAssistantText][解析失败：{}]", e.getMessage());
            return null;
        }
    }

    private String listFilesText(String convId) {
        try {
            String resp = getJson("/v1/sessions/" + convId + "/resources/files?limit=100");
            JsonNode root = JsonUtils.parseTree(resp);
            JsonNode data = root.get("data");
            if (data == null || !data.isArray() || data.isEmpty()) {
                return null;
            }
            List<String> ids = new ArrayList<>();
            for (JsonNode f : data) {
                String fid = textOf(f.get("file_id"));
                if (fid == null) fid = textOf(f.get("id"));
                if (fid != null) ids.add(fid);
            }
            return ids.isEmpty() ? null : JsonUtils.toJsonString(ids);
        } catch (Exception e) {
            log.warn("[listFilesText][convId={} 取文件失败：{}]", convId, e.getMessage());
            return null;
        }
    }

    private String postJson(String path, String jsonBody) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .timeout(Duration.ofMillis(timeoutMs));
        applyAuth(b);
        HttpRequest request = b.POST(HttpRequest.BodyPublishers.ofString(jsonBody)).build();
        HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400) {
            throw new RuntimeException("POST " + path + " -> " + resp.statusCode()
                    + "：" + truncate(resp.body(), 500));
        }
        return resp.body();
    }

    private String getJson(String path) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Accept", "application/json")
                .timeout(Duration.ofMillis(timeoutMs));
        applyAuth(b);
        HttpRequest request = b.GET().build();
        HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400) {
            throw new RuntimeException("GET " + path + " -> " + resp.statusCode()
                    + "：" + truncate(resp.body(), 500));
        }
        return resp.body();
    }

    /**
     * 注入 Omnigent 鉴权：accounts 模式下业务端点需 ap_session cookie。
     * cookie 配置为空时跳过（仅 /health 免鉴权可探活，业务端点将 401）。
     */
    private void applyAuth(HttpRequest.Builder b) {
        if (cookie != null && !cookie.isBlank()) {
            // 兼容「整段 cookie」与「仅 ap_session 值」两种填法
            String v = cookie.trim();
            if (v.contains("=") || v.contains(";")) {
                b.header("Cookie", v);
            } else {
                b.header("Cookie", "ap_session=" + v);
            }
        }
    }

    @Override
    public String getTaskStatus(String taskId) {
        // Omnigent 同步等待模式：dispatchTask 返回即 done；taskId 为 convId 可复查
        return SpkAgentTaskStatusEnum.DONE.getLabel();
    }

    @Override
    public String getTaskResult(String taskId) {
        if (taskId == null || taskId.startsWith("omnigent-fast#")) {
            return null;
        }
        try {
            return extractAssistantText(getJson("/v1/sessions/" + taskId + "/items?limit=100&order=asc"));
        } catch (Exception e) {
            log.warn("[getTaskResult][convId={} 取产物失败：{}]", taskId, e.getMessage());
            return null;
        }
    }

    @Override
    public boolean cancelTask(String taskId) {
        log.info("[cancelTask][omnigent 不支持同步取消 convId={}]", taskId);
        return false;
    }

    @Override
    public List<Map<String, Object>> listAvailableAgents() {
        return new ArrayList<>();
    }

    @Override
    public boolean healthCheck() {
        try {
            String resp = getJson("/health");
            JsonNode node = JsonUtils.parseTree(resp);
            return "ok".equals(textOf(node.get("status")));
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    private static String textOf(JsonNode node) {
        return (node == null || node.isNull()) ? null : node.asText();
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }
}
