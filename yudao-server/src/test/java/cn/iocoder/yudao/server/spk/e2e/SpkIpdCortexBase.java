package cn.iocoder.yudao.server.spk.e2e;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SPK-OS IPD 对外接口真实端到端测试基座（spk-cortex 真实服务）。
 * <p>
 * 与 {@link SpkIpdE2eBase}（{@code @SpringBootTest} 内嵌 48081 + e2e-test 桩 profile）的本质区别：
 * <ul>
 *   <li><b>不起内嵌服务</b>：纯 {@link RestTemplate} 调真实在跑的 spk-cortex（systemd, 48080）对外
 *       admin-api，验证的是真实部署的服务，不是测试自起的服务自调——杜绝"测试服务与生产服务行为不一致"。</li>
 *   <li><b>不连 DB</b>：不注入 {@code DataSource}/{@code JdbcTemplate}，铁律"只调 spk-cortex 接口，
 *       不往 DB 灌数据造假"。processInstanceId 等通过对外接口 GET flow-run 获取。</li>
 *   <li><b>真实模式</b>：依赖 spk-cortex 跑 dev profile（adapter=omnigent + fast-mode=false）真实调 LLM；
 *       mode=test 仅 prompt 轻量化，产物仍是真实 LLM 报告，<b>非</b> fast-mode 桩 {@code {"overall":"PASS"}}。</li>
 * </ul>
 * baseUrl 用 {@code -Dcortex.base.url} 覆盖（默认 {@code http://192.168.56.101:48080}）。
 * 登录 admin/admin123 + tenant-id:1；dev 关闭验证码。
 *
 * @author SPK-OS
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class SpkIpdCortexBase {

    protected static final String BASE = "/admin-api";

    /** 真实 spk-cortex 服务地址；可用 -Dcortex.base.url 覆盖。 */
    protected static final String CORTEX_URL =
            System.getProperty("cortex.base.url", "http://192.168.56.101:48080");

    protected RestTemplate restTemplate;
    protected String token;

    @BeforeAll
    void baseSetUp() {
        restTemplate = new RestTemplate();
        token = login("admin", "admin123");
        assertNotNull(token, "登录未拿到 accessToken，spk-cortex 管道未通：" + CORTEX_URL);
    }

    protected String url(String path) {
        return CORTEX_URL + BASE + path;
    }

    // ==================== HTTP helpers ====================

    /** 登录，返回 accessToken。 */
    @SuppressWarnings("unchecked")
    protected String login(String username, String password) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set("tenant-id", "1");
        Map<String, String> body = Map.of("username", username, "password", password);
        ResponseEntity<Map> resp = restTemplate.postForEntity(
                url("/system/auth/login"), new HttpEntity<>(body, h), Map.class);
        Map<String, Object> r = resp.getBody();
        assertNotNull(r, "登录返回空");
        assertEquals(0, r.get("code"), "登录失败: " + r);
        Map<String, Object> data = (Map<String, Object>) r.get("data");
        return data == null ? null : (String) data.get("accessToken");
    }

    /** 鉴权头：Bearer + tenant-id:1。 */
    protected HttpHeaders authHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(token);
        h.set("tenant-id", "1");
        return h;
    }

    /** POST（带鉴权），剥 CommonResult 壳返回 data。 */
    protected Map<String, Object> post(String path, Object body) {
        return post(path, body, null);
    }

    /** POST（带鉴权 + Idempotency-Key），剥 CommonResult 壳返回 data。 */
    @SuppressWarnings("unchecked")
    protected Map<String, Object> post(String path, Object body, String idempotencyKey) {
        HttpHeaders h = authHeaders();
        if (idempotencyKey != null) {
            h.set("Idempotency-Key", idempotencyKey);
        }
        ResponseEntity<Map> resp = restTemplate.postForEntity(
                url(path), new HttpEntity<>(body, h), Map.class);
        Map<String, Object> r = resp.getBody();
        assertNotNull(r, "POST " + path + " 返回空");
        assertEquals(0, r.get("code"), "POST " + path + " 业务失败: " + r);
        return (Map<String, Object>) r.get("data");
    }

    /** GET（带鉴权），剥 CommonResult 壳返回 data。 */
    @SuppressWarnings("unchecked")
    protected Map<String, Object> get(String path) {
        ResponseEntity<Map> resp = restTemplate.exchange(
                url(path), HttpMethod.GET, new HttpEntity<>(authHeaders()), Map.class);
        Map<String, Object> r = resp.getBody();
        assertNotNull(r, "GET " + path + " 返回空");
        assertEquals(0, r.get("code"), "GET " + path + " 业务失败: " + r);
        return (Map<String, Object>) r.get("data");
    }

    /** GET（带鉴权），剥 CommonResult 壳返回列表 data（兼容裸 List 与 PageResult{list:[...]}）。 */
    @SuppressWarnings("unchecked")
    protected List<Map<String, Object>> getList(String path) {
        ResponseEntity<Map> resp = restTemplate.exchange(
                url(path), HttpMethod.GET, new HttpEntity<>(authHeaders()), Map.class);
        Map<String, Object> r = resp.getBody();
        assertNotNull(r, "GET " + path + " 返回空");
        assertEquals(0, r.get("code"), "GET " + path + " 业务失败: " + r);
        return extractList(r.get("data"));
    }

    // ==================== 数据形态 helpers ====================

    @SuppressWarnings("unchecked")
    protected List<Map<String, Object>> extractList(Object data) {
        if (data instanceof List) {
            return (List<Map<String, Object>>) data;
        }
        if (data instanceof Map) {
            Map<String, Object> m = (Map<String, Object>) data;
            Object list = m.get("list");
            if (list instanceof List) {
                return (List<Map<String, Object>>) list;
            }
            Object records = m.get("records");
            if (records instanceof List) {
                return (List<Map<String, Object>>) records;
            }
        }
        return List.of();
    }

    protected String taskIdOf(Map<String, Object> task) {
        Object id = task.get("id");
        if (id == null) {
            id = task.get("taskId");
        }
        return id == null ? null : String.valueOf(id);
    }

    // ==================== IPD 流程 helpers（纯接口，不连 DB）====================

    /**
     * 通过对外接口创建一个可启动的 IPD 流程上下文：项目 + 大版本（自动建基线版本）+ preflight + FlowRun 草稿。
     * 全部走 spk-cortex admin-api，不连 DB。uid 保证 projectCode/majorNo 全局唯一。
     */
    protected Long provisionFlowRun(long uid) {
        Map<String, Object> proj = post("/spk/ipd/projects", Map.of(
                "projectCode", "E2ECORTEX-" + uid,
                "name", "E2E真实项目" + uid,
                "objective", "对外接口真实端到端测试",
                "ownerUserId", 1,
                "plannedEndAt", 1798588800000L)); // = 2026-12-31 00:00:00 UTC+8 毫秒时间戳；字符串 ISO 会致 TimestampLocalDateTimeDeserializer 回退 0→落 1970
        Long projectId = ((Number) proj.get("id")).longValue();

        Map<String, Object> mr = post("/spk/ipd/projects/" + projectId + "/major-releases", Map.of(
                "majorNo", (int) (uid % 100000) + 1,
                "name", "E2E真实大版本" + uid,
                "objective", "E2E真实大版本目标",
                "scopeSummary", "e2e-cortex scope",
                "ownerUserId", 1,
                "createBaselineVersion", true));
        Long baselineVersionId = ((Number) mr.get("baselineVersionId")).longValue();

        post("/spk/ipd/flow-runs/preflight", Map.of(
                "projectId", projectId, "versionId", baselineVersionId, "flowType", "FULL_RELEASE"));
        Map<String, Object> fr = post("/spk/ipd/flow-runs", Map.of(
                "projectId", projectId, "versionId", baselineVersionId, "flowType", "FULL_RELEASE"));
        return ((Number) fr.get("id")).longValue();
    }

    /** 等待并返回当前 flowRun 的首个 todo 审批任务（接口轮询，真实 LLM 慢需较长 atMost）。 */
    @SuppressWarnings("unchecked")
    protected Map<String, Object> awaitFirstTodo(Long flowRunId, Duration atMost) {
        AtomicReference<Map<String, Object>> t = new AtomicReference<>();
        Awaitility.await()
                .atMost(atMost)
                .pollInterval(Duration.ofSeconds(3))
                .untilAsserted(() -> {
                    Map<String, Object> data = get("/spk/ipd/approval-tasks?type=todo");
                    List<Map<String, Object>> all = extractList(data);
                    List<Map<String, Object>> mine = all.stream()
                            .filter(x -> flowRunId.toString().equals(String.valueOf(x.get("flowRunId"))))
                            .toList();
                    assertTrue(!mine.isEmpty(),
                            "todo 审批门未出现（FlowRun=" + flowRunId
                                    + " 仍卡在 serviceTask 真实 LLM 执行中；当前用户 todo 总数=" + all.size() + ")");
                    t.set(mine.get(0));
                });
        return t.get();
    }

    @SuppressWarnings("unchecked")
    protected boolean hasTodo(Long flowRunId) {
        Map<String, Object> data = get("/spk/ipd/approval-tasks?type=todo");
        return extractList(data).stream()
                .anyMatch(x -> flowRunId.toString().equals(String.valueOf(x.get("flowRunId"))));
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> firstTodo(Long flowRunId) {
        Map<String, Object> data = get("/spk/ipd/approval-tasks?type=todo");
        return extractList(data).stream()
                .filter(x -> flowRunId.toString().equals(String.valueOf(x.get("flowRunId"))))
                .findFirst()
                .orElse(null);
    }

    /**
     * APPROVE 当前 flowRun 的首个 todo 审批门（纯接口）：取决策包 → candidateActions 首个 enabled.action
     * + decisionPackageHash + forceOverride=true → POST decisions。返回 false 表示无 todo 可推。
     */
    protected boolean approveFirstTodo(Long flowRunId, String reason) {
        Map<String, Object> task = firstTodo(flowRunId);
        if (task == null) {
            return false;
        }
        String taskId = taskIdOf(task);
        Map<String, Object> pkg = get("/spk/ipd/approval-tasks/" + taskId + "/decision-package");
        String hash = pkg.get("decisionPackageHash") == null ? null
                : String.valueOf(pkg.get("decisionPackageHash"));
        String decision = pickDecisionAction(pkg);
        Map<String, Object> body = new HashMap<>();
        body.put("decision", decision);
        body.put("reason", reason);
        if (hash != null) {
            body.put("decisionPackageHash", hash);
        }
        body.put("forceOverride", true);
        post("/spk/ipd/approval-tasks/" + taskId + "/decisions", body);
        return true;
    }

    @SuppressWarnings("unchecked")
    protected String pickDecisionAction(Map<String, Object> pkg) {
        List<Map<String, Object>> actions = extractList(pkg.get("candidateActions"));
        for (Map<String, Object> a : actions) {
            Object enabled = a.get("enabled");
            if (enabled == null || Boolean.parseBoolean(String.valueOf(enabled))) {
                Object action = a.get("action");
                if (action != null) {
                    return String.valueOf(action);
                }
            }
        }
        return "APPROVE";
    }

    /** 通过对外接口 GET flow-run 取 processInstanceId（不连 DB）。 */
    protected String processInstanceIdOf(Long flowRunId) {
        Map<String, Object> run = get("/spk/ipd/flow-runs/" + flowRunId);
        Object pid = run.get("processInstanceId");
        return pid == null ? null : String.valueOf(pid);
    }
}
