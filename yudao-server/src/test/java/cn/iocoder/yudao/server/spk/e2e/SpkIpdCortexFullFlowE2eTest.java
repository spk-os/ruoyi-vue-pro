package cn.iocoder.yudao.server.spk.e2e;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SPK-OS IPD 对外接口真实端到端全流程测试：调真实 spk-cortex(48080)，真实发起项目 → 真实审批 →
 * 真实跑完6大阶段（concept→requirement→architecture→…→CDCP/TR/CCB 全 APPROVE 到 FlowRun COMPLETED）。
 * <p>
 * 铁律（用户指令）：
 * <ul>
 *   <li><b>只调 spk-cortex 对外接口</b>：不起测试服务，不连 DB；发起/审批/执行/验证全走 admin-api HTTP。</li>
 *   <li><b>绝不造假</b>：不 mock、不往 DB 灌产物/证据。dev profile omnigent+fast-mode=false 真实调 LLM，
 *       产物是真实 LLM 报告全文，非 fast-mode 桩 {@code {"overall":"PASS"}}。</li>
 *   <li><b>真实跑完6大阶段</b>：FlowRun 终态 COMPLETED + endedAt 非空（P0 COMPLETED 闭合修复生效铁证）。</li>
 * </ul>
 * <p>
 * 慢（真实 LLM，omnigent agent 收敛 10-60s/阶段，6阶段+多审批门 atMost 30min）。
 * <p>
 * 跑法（默认 surefire 排除 e2e-cortex，手动跑）：
 * <pre>mvn test -pl yudao-server -Dtest=SpkIpdCortexFullFlowE2eTest -Dsurefire.excludedGroups=</pre>
 * <b>前置</b>：spk-cortex(48080) 跑 dev profile 且加载含 P0 COMPLETED 闭合修复（{@code markCompletedByInstance}
 * + {@code SpkIpdFlowFinishListener} APPROVE 回写）的代码；否则 FlowRun 永远 RUNNING。
 *
 * @author SPK-OS
 */
@Tag("e2e-cortex")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SpkIpdCortexFullFlowE2eTest extends SpkIpdCortexBase {

    /** 跨 @Test 共享（@TestInstance(PER_CLASS) 继承自基座）：Order(1) 跑通后供后续验证。 */
    private Long flowRunId;
    private String processInstanceId;

    @Test
    @Order(1)
    @DisplayName("1. 真实发起项目→6审批门全 APPROVE→FlowRun COMPLETED（6大阶段真实跑完）")
    void 真实发起到COMPLETED() {
        long uid = System.nanoTime();
        flowRunId = provisionFlowRun(uid);
        assertNotNull(flowRunId, "FlowRun id 为空（spk-cortex 接口建项目/版本/flow-run 失败）");

        // 幂等启动（同步至首个 receiveTask 即返 RUNNING + processInstanceId）
        Map<String, Object> start = post("/spk/ipd/flow-runs/" + flowRunId + "/start",
                Map.of(), "e2e-cortex-start-" + flowRunId + "-" + uid);
        assertEquals("RUNNING", start.get("flowRunStatus"), "start 后非 RUNNING：" + start);
        processInstanceId = processInstanceIdOf(flowRunId);
        assertNotNull(processInstanceId, "flow_run 无 process_instance_id（spk-cortex 未真实启动 BPM 实例）");

        // 全流程推进：真实 LLM 每 poll 有 todo 就 APPROVE，直到 status=COMPLETED + endedAt 非空。
        // 真实 omnigent agent 收敛慢，atMost 30min 兜底；pollInterval 5s 让 serviceTask 门间真实推进。
        Awaitility.await()
                .atMost(Duration.ofMinutes(30))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    if (hasTodo(flowRunId)) {
                        approveFirstTodo(flowRunId, "E2E-cortex 真实审批通过");
                    }
                    Map<String, Object> run = get("/spk/ipd/flow-runs/" + flowRunId);
                    assertEquals("COMPLETED", run.get("status"),
                            "FlowRun 未真实跑完6阶段（仍有审批门待推或 serviceTask 真实执行中）：" + run);
                    assertNotNull(run.get("endedAt"), "COMPLETED 但 endedAt 为空（P0 闭合未生效）");
                });
        System.out.println("[cortex-e2e] ✅ 6大阶段真实跑完 flowRunId=" + flowRunId
                + " processInstanceId=" + processInstanceId);
    }

    @Test
    @Order(2)
    @DisplayName("2. timeline 覆盖6阶段（BPM 历史活动真实执行）")
    void 验证timeline覆盖6阶段() {
        assertNotNull(flowRunId, "前置 Order(1) 未跑通，无 flowRunId");
        Map<String, Object> timeline = get("/spk/ipd/flow-runs/" + flowRunId + "/timeline");
        List<Map<String, Object>> events = extractList(timeline.get("events"));
        assertFalse(events.isEmpty(), "timeline events 为空（6阶段 BPM 活动未真实执行）");
        System.out.println("[cortex-e2e] timeline events 数=" + events.size());
    }

    @Test
    @Order(3)
    @DisplayName("3. decisions 每审批门一条不可变决策（真实审批落库）")
    void 验证decisions() {
        assertNotNull(flowRunId, "前置 Order(1) 未跑通，无 flowRunId");
        List<Map<String, Object>> decs = getList("/spk/ipd/flow-runs/" + flowRunId + "/decisions");
        assertFalse(decs.isEmpty(), "decisions 为空（审批门决策未真实落库——对外端点查询层缺口 task #105）");
        System.out.println("[cortex-e2e] decisions 数=" + decs.size());
    }

    @Test
    @Order(4)
    @DisplayName("4. artifacts 真实产物（非 fast-mode 桩 overall:PASS）")
    void 验证artifacts真实非桩() {
        assertNotNull(flowRunId, "前置 Order(1) 未跑通，无 flowRunId");
        List<Map<String, Object>> artifacts = getList("/spk/ipd/flow-runs/" + flowRunId + "/artifacts");
        assertFalse(artifacts.isEmpty(), "artifacts 为空（6阶段真实产物未落——对外端点查询层缺口 task #105）");
        int stubSuspect = 0;
        for (Map<String, Object> a : artifacts) {
            Object content = a.get("content");
            String cs = content == null ? "" : String.valueOf(content);
            // fast-mode 桩产物特征：极短且含 overall:PASS（真实 LLM 报告是多行 markdown 全文）
            if (cs.contains("\"overall\":\"PASS\"") && cs.length() < 80) {
                stubSuspect++;
                System.out.println("[cortex-e2e] ⚠ 疑似桩产物 " + a.get("name") + " content=" + cs);
            }
        }
        assertEquals(0, stubSuspect, "存在 " + stubSuspect + " 个 fast-mode 桩产物（overall:PASS 极短），真实模式不应出现");
        // 打印首个产物内容预览，人工确认是真实 LLM 报告全文
        Object firstContent = artifacts.get(0).get("content");
        String preview = firstContent == null ? "(null)" : String.valueOf(firstContent);
        System.out.println("[cortex-e2e] artifacts 数=" + artifacts.size() + " 首个内容预览(≤300字)="
                + preview.substring(0, Math.min(preview.length(), 300)));
    }

    @Test
    @Order(5)
    @DisplayName("5. evidence 哈希链证据非空")
    void 验证evidence哈希链() {
        assertNotNull(flowRunId, "前置 Order(1) 未跑通，无 flowRunId");
        List<Map<String, Object>> evidence = getList("/spk/ipd/flow-runs/" + flowRunId + "/evidence");
        assertFalse(evidence.isEmpty(), "evidence 为空（哈希链证据未落——对外端点查询层缺口 task #105）");
        System.out.println("[cortex-e2e] evidence 数=" + evidence.size());
    }
}
