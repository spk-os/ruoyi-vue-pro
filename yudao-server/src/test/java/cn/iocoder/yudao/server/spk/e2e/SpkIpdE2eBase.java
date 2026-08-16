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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
}
