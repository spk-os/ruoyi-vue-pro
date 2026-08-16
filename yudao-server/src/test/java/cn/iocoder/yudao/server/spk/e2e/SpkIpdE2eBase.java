package cn.iocoder.yudao.server.spk.e2e;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SPK-OS IPD 端到端集成测试基座。
 * <p>
 * 设计要点：
 * <ul>
 *   <li>{@code webEnvironment = DEFINED_PORT}：触发器 URL 回环依赖固定端口 48081（见
 *       application-e2e-test.yaml 闭环命脉注释）。{@code @LocalServerPort}（Spring Boot 4 新包
 *       {@code web.server}）注入 48081。</li>
 *   <li>{@code @TestInstance(PER_CLASS)}：使非 static {@code @BeforeAll} 能访问注入字段（port/dataSource），
 *       登录一次性完成而非每方法重复登录。</li>
 *   <li>Spring Boot 4 已移除 TestRestTemplate，改用标准 {@link RestTemplate} + no-op error handler：
 *       4xx/5xx 不抛异常，走 CommonResult.code 断言（yudao 异常统一 200 + CommonResult）。</li>
 *   <li>{@code @ActiveProfiles({"dev","e2e-test"})}：叠加在 dev 之上复用真 PG/Redis/exclude，
 *       e2e-test 只覆盖 adapter=native-ai + fast-mode=true + port + quartz 关闭 + self.base-url 回环。</li>
 *   <li>断言走纯 API（timeline/artifacts/evidence/decisions 即"每个环节产物与信息"验证口），
 *       JdbcTemplate 仅兜底直查 spk_task_contract 行。</li>
 *   <li>登录用 admin/admin123 + tenant-id:1；dev 关闭验证码。</li>
 * </ul>
 *
 * @author SPK-OS
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles({"dev", "e2e-test"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class SpkIpdE2eBase {

    protected static final String BASE = "/admin-api";

    @LocalServerPort
    protected int port;

    @Autowired
    protected DataSource dataSource;

    protected RestTemplate restTemplate;
    protected JdbcTemplate jdbc;
    protected String token;

    @BeforeAll
    void baseSetUp() {
        restTemplate = new RestTemplate();
        // yudao 异常统一返回 200 + CommonResult(code!=0)，RestTemplate 默认不因业务码抛异常；
        // admin 持有效 token 访问受保护端点不会 401，故无需自定义 error handler。
        jdbc = new JdbcTemplate(dataSource);
        token = login("admin", "admin123");
        assertNotNull(token, "登录未拿到 accessToken，管道未通");
    }

    private String url(String path) {
        return "http://127.0.0.1:" + port + BASE + path;
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

    /**
     * POST（带鉴权 + Idempotency-Key），<b>不</b>剥壳也<b>不</b>断言业务码——返回完整 CommonResult
     * （含 code/data/msg）。用于断言"应被状态机拒绝"的失败路径：调用方断言 {@code code != 0}。
     */
    @SuppressWarnings("unchecked")
    protected Map<String, Object> postRaw(String path, Object body, String idempotencyKey) {
        HttpHeaders h = authHeaders();
        if (idempotencyKey != null) {
            h.set("Idempotency-Key", idempotencyKey);
        }
        ResponseEntity<Map> resp = restTemplate.postForEntity(
                url(path), new HttpEntity<>(body, h), Map.class);
        Map<String, Object> r = resp.getBody();
        assertNotNull(r, "POST " + path + " 返回空");
        return r;
    }

    /** GET（带鉴权），剥 CommonResult 壳返回 data（假设为 Map，用于单对象/分页 PageResult 端点）。 */
    @SuppressWarnings("unchecked")
    protected Map<String, Object> get(String path) {
        ResponseEntity<Map> resp = restTemplate.exchange(
                url(path), HttpMethod.GET, new HttpEntity<>(authHeaders()), Map.class);
        Map<String, Object> r = resp.getBody();
        assertNotNull(r, "GET " + path + " 返回空");
        assertEquals(0, r.get("code"), "GET " + path + " 业务失败: " + r);
        return (Map<String, Object>) r.get("data");
    }

    /** GET（带鉴权），剥 CommonResult 壳返回列表 data（兼容裸 List 与 PageResult{list:[...]} 两种形态）。 */
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

    /**
     * 从 CommonResult.data 抽取列表：兼容裸 List 与 PageResult（{list:[...]}）两种形态。
     */
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

    /** 取审批任务 id（兼容 id / taskId 两种字段名）。 */
    protected String taskIdOf(Map<String, Object> task) {
        Object id = task.get("id");
        if (id == null) {
            id = task.get("taskId");
        }
        return id == null ? null : String.valueOf(id);
    }

    // ==================== Awaitility helpers ====================

    protected void awaitAtMost(Duration d) {
        Awaitility.setDefaultTimeout(d);
    }

    // ==================== IPD 流程 helpers（分层/全流程慢测共用） ====================

    /**
     * 创建一个可启动的 IPD 流程上下文：项目 + 大版本（自动建基线版本）+ preflight + FlowRun 草稿，
     * 返回 FlowRun id。{@code uid} 用于保证 projectCode/majorNo 全局唯一（不主动清库只断言自建行）。
     * <p>
     * FULL_RELEASE 只能绑 BASELINE（validateFlowTypeContext 校验），故烟测/分层均用基线版本跑 FULL_RELEASE，
     * INCREMENT_RELEASE + INCREMENT 版本组合留给专项测试。
     */
    protected Long provisionFlowRun(long uid) {
        Map<String, Object> proj = post("/spk/ipd/projects", Map.of(
                "projectCode", "E2E-" + uid,
                "name", "E2E项目" + uid,
                "objective", "端到端集成测试",
                "ownerUserId", 1,
                "plannedEndAt", "2026-12-31T00:00:00"));
        Long projectId = ((Number) proj.get("id")).longValue();

        Map<String, Object> mr = post("/spk/ipd/projects/" + projectId + "/major-releases", Map.of(
                "majorNo", (int) (uid % 100000) + 1,
                "name", "E2E大版本" + uid,
                "objective", "E2E大版本目标",
                "scopeSummary", "e2e scope",
                "ownerUserId", 1,
                "createBaselineVersion", true));
        Long baselineVersionId = ((Number) mr.get("baselineVersionId")).longValue();

        post("/spk/ipd/flow-runs/preflight", Map.of(
                "projectId", projectId, "versionId", baselineVersionId, "flowType", "FULL_RELEASE"));
        Map<String, Object> fr = post("/spk/ipd/flow-runs", Map.of(
                "projectId", projectId, "versionId", baselineVersionId, "flowType", "FULL_RELEASE"));
        return ((Number) fr.get("id")).longValue();
    }

    /**
     * 等待并返回当前 flowRun 的首个 todo 审批任务（client 端按 flowRunId 过滤 approval-tasks?type=todo）。
     * 超时抛 {@link org.awaitility.core.ConditionTimeoutError}——todo 不出现说明管道未通，测试理应失败。
     * fast-mode 桩同步跑完 serviceTask 到 receiveTask，应在数秒~数十秒内出现。
     */
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
                                    + " 仍卡在 serviceTask/未到 receiveTask；当前用户 todo 总数=" + all.size() + ")");
                    t.set(mine.get(0));
                });
        return t.get();
    }

    /** 立即查询当前 flowRun 是否还有 todo 审批门（非阻塞，供全流程慢测循环判断流程是否已尽）。 */
    @SuppressWarnings("unchecked")
    protected boolean hasTodo(Long flowRunId) {
        Map<String, Object> data = get("/spk/ipd/approval-tasks?type=todo");
        return extractList(data).stream()
                .anyMatch(x -> flowRunId.toString().equals(String.valueOf(x.get("flowRunId"))));
    }

    /** 立即取当前 flowRun 的首个 todo 审批任务，无则返回 null（非阻塞）。 */
    @SuppressWarnings("unchecked")
    protected Map<String, Object> firstTodo(Long flowRunId) {
        Map<String, Object> data = get("/spk/ipd/approval-tasks?type=todo");
        return extractList(data).stream()
                .filter(x -> flowRunId.toString().equals(String.valueOf(x.get("flowRunId"))))
                .findFirst()
                .orElse(null);
    }

    /**
     * APPROVE 当前 flowRun 的首个 todo 审批门（非阻塞取，无则返回 false）：取决策包 →
     * candidateActions 首个 enabled.action + decisionPackageHash + forceOverride=true → POST decisions。
     * 供全流程慢测循环：返回 false 表示无 todo 可推（流程已尽或推进中 serviceTask 尚未到下一 receiveTask）。
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
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("decision", decision);
        body.put("reason", reason);
        if (hash != null) {
            body.put("decisionPackageHash", hash);
        }
        body.put("forceOverride", true);
        post("/spk/ipd/approval-tasks/" + taskId + "/decisions", body);
        return true;
    }

    /** 从决策包 candidateActions 取首个 enabled 的 action 作为 decision 值；兜底 APPROVE。 */
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

    /**
     * 兜底直查 spk_task_contract 行：SpkTaskContractDO 无 flowRunId 字段，关联键是 processInstanceId
     * （列 process_instance_id），businessKey 是 "IPD:<runNo>" 非 projectId，故走 flowRunId→pid→contract 两步查。
     */
    protected String processInstanceIdOf(Long flowRunId) {
        return jdbc.queryForObject(
                "SELECT process_instance_id FROM spk_ipd_flow_run WHERE id = ?", String.class, flowRunId);
    }

    protected int contractCountByPid(String pid) {
        Integer c = jdbc.queryForObject(
                "SELECT COUNT(*) FROM spk_task_contract WHERE process_instance_id = ?", Integer.class, pid);
        return c == null ? 0 : c;
    }
}
