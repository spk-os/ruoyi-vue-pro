package cn.iocoder.yudao.server.spk.e2e;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SPK-OS IPD 端到端全流程慢测：从流程发起到所有审批门 APPROVE 推进，直到 FlowRun COMPLETED。
 * <p>
 * 与烟测（发起到首审批门）、分层（状态机/产物）互补——本类验证"完整流程"端到端：
 * 每个审批门（concept→requirement→architecture→…→CDCP/TR/CCB）逐个 APPROVE，
 * fast-mode 桩 serviceTask 在门间自动推进，直到无 todo 且 FlowRun 终态 COMPLETED。
 * <p>
 * 慢测用 {@code @Tag("e2e-full")} 隔离：默认 surefire 排除 e2e,e2e-full，仅大改时手动跑：
 *   mvn test -pl yudao-server -Dtest=SpkIpdFullFlowE2eTest -Dsurefire.excludedGroups=
 * <p>
 * 轮询策略：每个 poll 周期若存在 todo 则 APPROVE 首个，随后断言 status=COMPLETED。
 * COMPLETED 后无 todo 可推 → 断言通过退出；未完成则继续 poll（serviceTask 门间异步推进需时间）。
 *
 * @author SPK-OS
 */
@Tag("e2e-full")
class SpkIpdFullFlowE2eTest extends SpkIpdE2eBase {

    @Test
    void 全流程慢测_发起到COMPLETED全审批门推进() {
        long uid = System.nanoTime();
        Long flowRunId = provisionFlowRun(uid);
        assertNotNull(flowRunId, "FlowRun id 为空");

        // 幂等启动（同步至首个 receiveTask 即返 RUNNING + processInstanceId）
        Map<String, Object> start = post("/spk/ipd/flow-runs/" + flowRunId + "/start",
                Map.of(), "e2e-full-start-" + flowRunId + "-" + uid);
        assertEquals("RUNNING", start.get("flowRunStatus"), "start 后非 RUNNING");
        String pid = processInstanceIdOf(flowRunId);
        assertNotNull(pid, "flow_run 无 process_instance_id");

        // 全流程推进：每个 poll 有 todo 就 APPROVE，直到 status=COMPLETED（fast-mode 桩门间毫秒级推进）
        // atMost 10min 兜底（多阶段多审批门 + serviceTask 异步）；pollInterval 2s 让 serviceTask 门间推进
        Awaitility.await()
                .atMost(Duration.ofMinutes(10))
                .pollInterval(Duration.ofSeconds(2))
                .untilAsserted(() -> {
                    if (hasTodo(flowRunId)) {
                        approveFirstTodo(flowRunId, "E2E 全流程通过");
                    }
                    Map<String, Object> run = get("/spk/ipd/flow-runs/" + flowRunId);
                    assertEquals("COMPLETED", run.get("status"),
                            "FlowRun 未完成（仍有审批门待推或 serviceTask 卡住）：" + run);
                });

        // COMPLETED 后产物齐全验证
        // timeline events 覆盖所有阶段（BPM 历史活动全执行）
        Map<String, Object> timeline = get("/spk/ipd/flow-runs/" + flowRunId + "/timeline");
        List<Map<String, Object>> events = extractList(timeline.get("events"));
        assertTrue(!events.isEmpty(), "全流程 timeline events 为空");

        // decisions 历史：每个审批门一条不可变决策
        List<Map<String, Object>> decs = getList("/spk/ipd/flow-runs/" + flowRunId + "/decisions");
        assertTrue(!decs.isEmpty(), "全流程 decisions 为空（审批门决策未落）");

        // 产物清单 + 证据链非空
        List<Map<String, Object>> artifacts = getList("/spk/ipd/flow-runs/" + flowRunId + "/artifacts");
        assertTrue(!artifacts.isEmpty(), "全流程 artifacts 为空");
        List<Map<String, Object>> evidence = getList("/spk/ipd/flow-runs/" + flowRunId + "/evidence");
        assertTrue(!evidence.isEmpty(), "全流程 evidence 为空");

        // 兜底直查 contract 行（每阶段 dispatchActivityAsync 落一行+，COMPLETED 应有多行）
        assertTrue(contractCountByPid(pid) > 0,
                "全流程 spk_task_contract 无 process_instance_id=" + pid + " 的行");
    }
}
