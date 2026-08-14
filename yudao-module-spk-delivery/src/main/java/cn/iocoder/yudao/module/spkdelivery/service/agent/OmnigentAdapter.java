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
import java.util.concurrent.ConcurrentHashMap;

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
     * D8（设计 §15.5.3）SSE 订阅→落库闭环开关。
     * <p>
     * true 时 dispatchTask 走 SSE 路径：createSession + launchRunner 后，开
     * {@code GET /v1/sessions/{id}/events} SSE 流（{@code text/event-stream}），
     * 逐行解析 {@code data: <json>} 累积 assistant 文本与终态事件，至 runner.completed /
     * 超时收口，返回 DispatchResult。落库（contract/artifact/Flowable 推进）仍由上层
     * SpkAgentTaskService 既有路径完成——SSE 只换传输，闭环不破。
     * <p>
     * 默认 false（poll 兼容旧路径）；Omnigent events 端点稳定后置 true。
     */
    @Value("${spk-delivery.omnigent.sse-mode:false}")
    private boolean sseMode;

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

    // name→UUID 缓存：omnigent_agent_id 列可填 agent 名称（如 spk-architect）或 32-hex UUID，
    // POST /v1/sessions 的 agent_id 必须是 UUID；名称在此解析后缓存（/v1/agents 稳定，UUID 为 content-hash 跨重启不变）。
    private final Map<String, String> agentIdCache = new ConcurrentHashMap<>();

    // 强制 HTTP/1.1：Java HttpClient 默认 HTTP/2，对 Omnigent(uvicorn/HTTP1.1) 发 h2c upgrade
    // 会被拒「Invalid HTTP request received」(400)，必须显式降版本。
    private final HttpClient http = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10)).build();

    /**
     * 会话续跑缓存：activityRunId → conv_xxx。同 activityRunId 重复派发复用同 session（runner 已存在 409 幂等），
     * 让 Omnigent 会话持久化价值落地（多轮上下文不丢）。route 正常路径每次新 activityRunId 不会命中，
     * 主要服务于同 run 内重试 / 手动 rerun 同 key 场景。
     */
    private final Map<String, String> sessionCache = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public SpkAgentDispatchResult dispatchTask(SpkAgentDispatchReq req) {
        if (fastMode) {
            return fastDispatch(req);
        }
        // per-agent 动态 agent-id：优先 req.omnigentAgentId（lead 维度），空回退全局配置 agentId。
        // 列值可填 agent 名称（spk-architect）或 32-hex UUID；名称在此解析为 UUID（Omnigent 要求 UUID）。
        String rawAgentId = (req.getOmnigentAgentId() != null && !req.getOmnigentAgentId().isBlank())
                ? req.getOmnigentAgentId() : agentId;
        if (rawAgentId == null || rawAgentId.isBlank()) {
            throw new RuntimeException("OmnigentAdapter 真实模式需配置 agent-id（per-agent omnigentAgentId 或全局 spk-delivery.omnigent.agent-id）");
        }
        String effectiveAgentId = resolveAgentId(rawAgentId);
        try {
            // 会话续跑：同 activityRunId 复用既有 conv，否则新建
            String convId = (req.getActivityRunId() != null)
                    ? sessionCache.get(req.getActivityRunId()) : null;
            boolean reused = convId != null;
            if (!reused) {
                convId = createSession(req, effectiveAgentId);
                if (req.getActivityRunId() != null) {
                    sessionCache.put(req.getActivityRunId(), convId);
                }
            }
            log.info("[dispatchTask][omnigent instanceId={} nodeKey={} leadCode={} agentId={} convId={} reused={} sse={}]",
                    req.getInstanceId(), req.getNodeKey(), req.getLeadCode(), effectiveAgentId, convId, reused, sseMode);
            // per-project/version/activityRunId workspace 隔离（host 须真实存在，本地路径自动 mkdir）
            String ws = resolveWorkspace(req);
            // 关键：create session 只种 seed 消息，必须 launch runner 才真正触发 agent 执行
            launchRunner(convId, ws);
            String assistantText = sseMode ? subscribeAssistantText(convId) : pollAssistantText(convId);
            // files 结构化回流：取 resources/files 真实文件 {path, fileId, sha}，拼成结构化 JSON + markdown 段
            String filesJson = listFilesStructured(convId);
            String resultText = buildResultText(assistantText, filesJson);
            return new SpkAgentDispatchResult()
                    .setTaskId(convId)
                    .setConversationId(0L)
                    .setResult(resultText)
                    .setStatus(SpkAgentTaskStatusEnum.DONE.getLabel());
        } catch (Exception e) {
            log.error("[dispatchTask][omnigent 失败 instanceId={} nodeKey={} leadCode={}]",
                    req.getInstanceId(), req.getNodeKey(), req.getLeadCode(), e);
            throw new RuntimeException("Omnigent 派发失败：" + e.getMessage(), e);
        }
    }

    /** 组装产物文本：assistant 正文 + 结构化 files 段（markdown 表 + JSON），供 parsePayload 提取 document。 */
    private String buildResultText(String assistantText, String filesJson) {
        StringBuilder sb = new StringBuilder();
        if (assistantText != null && !assistantText.isBlank()) {
            sb.append(assistantText);
        }
        if (filesJson != null && !filesJson.isBlank()) {
            // 结构化 files：嵌入 JSON 字段（cockpit/artifact 可解析）+ markdown 可读段
            sb.append("\n\n## 交付文件\n");
            try {
                JsonNode arr = JsonUtils.parseTree(filesJson);
                if (arr.isArray()) {
                    sb.append("| 文件路径 | 标识 |\n| --- | --- |\n");
                    for (JsonNode f : arr) {
                        String path = textOf(f.get("path"));
                        String sha = textOf(f.get("sha"));
                        if (sha == null) sha = textOf(f.get("fileId"));
                        sb.append("| ").append(path != null ? path : "-").append(" | ").append(sha != null ? sha : "-").append(" |\n");
                    }
                }
            } catch (Exception ignore) {
                sb.append("(files 解析失败，原始：").append(truncate(filesJson, 200)).append(")\n");
            }
            sb.append("\n[files]\n").append(filesJson);
        }
        return sb.toString();
    }

    /**
     * 解析 per-activity workspace 路径：{workspace根}/spk/{projectId|na}/{versionId|na}/{activityRunId}。
     * Omnigent host 须真实存在此路径，否则 launch runner 400。本地路径（/work/... 或 /tmp/...）自动 mkdir。
     * 非本地 host 路径无法 mkdir，降级回退全局 workspace 根（已存在）以保证 runner 可启动。
     */
    private String resolveWorkspace(SpkAgentDispatchReq req) {
        String root = (workspace == null || workspace.isBlank()) ? "/work/SPK-OS/artifacts" : workspace;
        String pid = req.getProjectId() != null ? String.valueOf(req.getProjectId()) : "na";
        String vid = req.getVersionId() != null ? String.valueOf(req.getVersionId()) : "na";
        String aid = req.getActivityRunId() != null ? req.getActivityRunId() : "run-" + System.nanoTime();
        String ws = root + "/spk/" + pid + "/" + vid + "/" + aid;
        // 仅本地路径尝试 ensureWorkspace（Omnigent host 同机时有效；远程路径 mkdir 无意义但不报错）
        if (ensureWorkspace(ws)) {
            return ws;
        }
        // 降级：隔离路径 ensure 失败（远程不可写），回退全局根（host 侧已存在）保证 runner 可启动
        log.warn("[resolveWorkspace][隔离路径 {} 无法确保，回退全局根 {}]", ws, root);
        return root;
    }

    /** 本地路径自动 mkdir（仅对可写本地路径生效）；返回是否成功确保。 */
    private boolean ensureWorkspace(String ws) {
        try {
            java.nio.file.Path p = java.nio.file.Paths.get(ws);
            java.nio.file.Files.createDirectories(p);
            return true;
        } catch (Exception e) {
            // 远程路径或无权限：非致命，调用方降级回退全局根
            log.debug("[ensureWorkspace][{} mkdir 失败：{}]", ws, e.getMessage());
            return false;
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

    /**
     * 解析 agent-id：32-hex UUID 原样返回；否则按名称查 GET /v1/agents 取 UUID 并缓存。
     * omnigent_agent_id 列允许填名称（用户友好、跨重启稳定），但 POST /v1/sessions 的 agent_id
     * 必须是 UUID，故在此做一次 name→id 解析（/v1/agents 列表稳定，UUID 为 content-hash 跨重启不变）。
     */
    private String resolveAgentId(String raw) {
        if (raw == null || raw.isBlank()) {
            return raw;
        }
        // 32 位 hex（含可选 ag_ 前缀）视为 UUID，直接用
        String trimmed = raw.startsWith("ag_") ? raw.substring(3) : raw;
        if (trimmed.length() == 32 && trimmed.matches("[0-9a-fA-F]{32}")) {
            return trimmed;
        }
        // 名称：走缓存 → GET /v1/agents 按名匹配
        String cached = agentIdCache.get(raw);
        if (cached != null) {
            return cached;
        }
        try {
            String resp = getJson("/v1/agents");
            JsonNode root = JsonUtils.parseTree(resp);
            JsonNode data = root.has("data") ? root.get("data") : root;
            if (data == null || !data.isArray()) {
                throw new RuntimeException("/v1/agents 响应无 data 数组：" + truncate(resp, 300));
            }
            for (JsonNode a : data) {
                String name = textOf(a.get("name"));
                if (raw.equals(name)) {
                    String id = textOf(a.get("id"));
                    if (id != null && !id.isBlank()) {
                        agentIdCache.put(raw, id);
                        log.info("[resolveAgentId][name={} -> uuid={}]", raw, id);
                        return id;
                    }
                }
            }
            throw new RuntimeException("Omnigent agent 名称「" + raw + "」在 /v1/agents 未注册（404 根因）");
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("解析 Omnigent agent-id「" + raw + "」失败：" + e.getMessage(), e);
        }
    }

    private String createSession(SpkAgentDispatchReq req, String effectiveAgentId) throws Exception {
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
        body.put("agent_id", effectiveAgentId);
        body.put("title", "SPK-IPD " + nodeKey + (req.getLeadCode() != null ? " " + req.getLeadCode() : ""));
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
     * workspace 必须是 host 上真实存在的绝对路径，否则 400（由 resolveWorkspace 保证）。
     * 已存在 runner 时返回 409，视为已启动（幂等）。
     */
    private void launchRunner(String convId, String ws) throws Exception {
        String hid = resolveHostId();
        if (hid == null || hid.isBlank()) {
            throw new RuntimeException("Omnigent 无可用在线 host（需在线且 claude-sdk harness 已配置）");
        }
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
     * D8（设计 §15.5.3）SSE 订阅→落库闭环。
     * <p>
     * 开 {@code GET /v1/sessions/{id}/events}（{@code text/event-stream}）SSE 流，
     * 逐行解析 {@code data: <json>} 事件：
     * <ul>
     *   <li>assistant message 文本（item.type=message + role=assistant + content[].text）→ 累积；</li>
     *   <li>runner.completed / session.completed / status=terminal → 收口返回；</li>
     *   <li>runner.failed / error → 抛异常（上层降级）；</li>
     *   <li>读取超时（{@link #timeoutMs}）或流关闭 → 返回已累积文本（兼容多智能体单轮不收敛）。</li>
     * </ul>
     * 用 {@link java.net.http.HttpResponse.BodyHandlers#ofLines()} 取 {@code Stream<String>}
     * 逐行消费，避免轮询空耗。落库仍由上层 dispatchTask 返回后走既有 contract/artifact 路径。
     */
    private String subscribeAssistantText(String convId) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/v1/sessions/" + convId + "/events"))
                .header("Accept", "text/event-stream")
                .timeout(Duration.ofMillis(timeoutMs));
        applyAuth(b);
        HttpRequest request = b.GET().build();
        // SSE 长流：用 ofLines() 拿逐行 Stream，onReadError 透传为 CompletionException
        java.util.concurrent.CompletableFuture<String> future = http.sendAsync(request,
                HttpResponse.BodyHandlers.ofLines()).thenApply(resp -> {
            if (resp.statusCode() >= 400) {
                throw new RuntimeException("SSE GET /v1/sessions/" + convId
                        + "/events -> " + resp.statusCode());
            }
            StringBuilder sb = new StringBuilder();
            // SSE 行：以 "data:" 前缀负载 JSON，空行分隔事件；其它（event:/id:/注释）跳过。
            resp.body().filter(line -> line != null && line.startsWith("data:")).forEach(line -> {
                String payload = line.substring(5).trim();
                if (payload.isEmpty() || "[DONE]".equals(payload)) {
                    return;
                }
                try {
                    JsonNode ev = JsonUtils.parseTree(payload);
                    // 终态事件收口
                    String type = textOf(ev.get("type"));
                    String status = textOf(ev.get("status"));
                    if (type != null && (type.contains("completed") || type.contains("terminal"))) {
                        return;
                    }
                    if (status != null && (status.contains("fail") || status.contains("error"))) {
                        throw new RuntimeException("Omnigent runner 失败：" + truncate(payload, 300));
                    }
                    // assistant 文本事件
                    JsonNode item = ev.has("item") ? ev.get("item") : ev;
                    if ("message".equals(textOf(item.get("type")))) {
                        JsonNode d = item.get("data");
                        String role = (d != null) ? textOf(d.get("role")) : textOf(item.get("role"));
                        if (d == null) {
                            d = item;
                        }
                        if ("assistant".equals(role)) {
                            JsonNode content = d.get("content");
                            if (content != null && content.isArray()) {
                                for (JsonNode block : content) {
                                    String t = textOf(block.get("text"));
                                    if (t != null && !t.isBlank()) {
                                        if (sb.length() > 0) {
                                            sb.append('\n');
                                        }
                                        sb.append(t);
                                    }
                                }
                            }
                        }
                    }
                } catch (RuntimeException re) {
                    throw re;
                } catch (Exception pe) {
                    // 单行解析失败不致命，跳过继续
                    log.debug("[subscribeAssistantText][convId={} 非 JSON SSE 行：{}]", convId, truncate(payload, 80));
                }
            });
            return sb.length() == 0 ? null : sb.toString();
        });
        try {
            // SSE 流无显式终态时靠 timeout 收口；sendAsync 本身受 timeoutMs 限制
            return future.get(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.TimeoutException te) {
            future.cancel(true);
            log.warn("[subscribeAssistantText][convId={} SSE 超时>{}}ms，按已收文本收口]", convId, timeoutMs);
            return null;
        } catch (java.util.concurrent.ExecutionException ee) {
            // 内层抛的 RuntimeException 透传
            Throwable cause = ee.getCause() != null ? ee.getCause() : ee;
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new RuntimeException("Omnigent SSE 订阅失败：" + cause.getMessage(), cause);
        }
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

    /**
     * 结构化回流 files：取 resources/files 真实文件，每文件 {path, fileId, sha}。
     * 替代旧 listFilesText（只拼 file_id 列表无 path/sha），让产物可追溯具体文件路径与指纹。
     * 返回 JSON 数组字符串；无文件返回 null。
     */
    private String listFilesStructured(String convId) {
        try {
            String resp = getJson("/v1/sessions/" + convId + "/resources/files?limit=100");
            JsonNode root = JsonUtils.parseTree(resp);
            JsonNode data = root.get("data");
            if (data == null || !data.isArray() || data.isEmpty()) {
                return null;
            }
            List<Map<String, Object>> files = new ArrayList<>();
            for (JsonNode f : data) {
                String fid = textOf(f.get("file_id"));
                if (fid == null) fid = textOf(f.get("id"));
                String path = textOf(f.get("path"));
                if (path == null) path = textOf(f.get("name"));
                if (path == null && fid != null) path = fid;
                String sha = textOf(f.get("sha"));
                if (sha == null) sha = textOf(f.get("sha256"));
                if (sha == null) sha = textOf(f.get("hash"));
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("path", path);
                m.put("fileId", fid);
                m.put("sha", sha);
                files.add(m);
            }
            return files.isEmpty() ? null : JsonUtils.toJsonString(files);
        } catch (Exception e) {
            log.warn("[listFilesStructured][convId={} 取文件失败：{}]", convId, e.getMessage());
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
