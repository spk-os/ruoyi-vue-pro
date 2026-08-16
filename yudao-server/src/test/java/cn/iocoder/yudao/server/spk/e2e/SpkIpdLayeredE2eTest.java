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
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * SPK-OS IPD 端到端分层测试：一个 flowRun 顺序覆盖状态机/幂等/骨架端点/产物验证口。
 * <p>
 * 与 {@link SpkIpdSmokeE2eTest}（发起到首审批门 APPROVE 推进管道烟测）互补：烟测验证"管道通"，
 * 本类验证"每个环节的产物与信息"——状态机前置校验、命令幂等、骨架端点显式标记、产物/证据/时间线齐全。
 * <p>
 * 设计取舍：fast-mode 桩 adapter 不产生 FAILED 终态路径（serviceTask 均成功），故 retry 仅测
 * 前置校验（非 FAILED 被拒）；retry 真实 attempt 落地需真实失败路径，留专项测试。
 * <p>
 * 手动跑：mvn test -pl yudao-server -Dtest=SpkIpdLayeredE2eTest -Dsurefire.excludedGroups=
 *
 * @author SPK-OS
 */
@Tag("e2e")
class SpkIpdLayeredE2eTest extends SpkIpdE2eBase {

    @Test
    void 分层覆盖_状态机幂等骨架产物() {
        long uid = System.nanoTime();
        Long flowRunId = provisionFlowRun(uid);
        assertNotNull(flowRunId, "FlowRun id 为空");

        // 1. 幂等启动（Idempotency-Key=k1）；start 同步至首个 receiveTask 即返 processInstanceId + RUNNING
        String k1 = "e2e-layered-start-" + flowRunId + "-" + uid;
        Map<String, Object> start = post("/spk/ipd/flow-runs/" + flowRunId + "/start",
                Map.of(), k1);
        assertNotNull(start, "start 返回空");
        assertEquals("RUNNING", start.get("flowRunStatus"), "start 后 flowRunStatus 非 RUNNING");
        Object processInstanceId = start.get("processInstanceId");
        assertNotNull(processInstanceId, "start 未返回 processInstanceId");

        // 2. 状态机前置校验：start 成功后 status=RUNNING，重复 start（无论同/异 key）均被
        //    IPD_FLOW_RUN_NOT_READY 拒。start 的幂等登记（enlist）在 status 校验之后，故幂等分支
        //    实际只在 STARTING 瞬态并发点击时触发——常态重复 start 由状态机先拒，这是设计使然。
        Map<String, Object> startDup = postRaw("/spk/ipd/flow-runs/" + flowRunId + "/start",
                Map.of(), k1);
        assertNotEquals(0, startDup.get("code"),
                "RUNNING 态重复 start 未被状态机拒绝（IPD_FLOW_RUN_NOT_READY）：" + startDup);

        // 3. 等首审批门 todo 出现（证明 serviceTask 跑完到 receiveTask，管道通）
        Map<String, Object> todo = awaitFirstTodo(flowRunId, Duration.ofSeconds(180));
        assertNotNull(todo, "首审批门 todo 未出现（serviceTask 未到 receiveTask）");

        // 4. 产物验证口：artifacts 非空（裸 List；fast-mode 桩产物已落 spk_artifact）
        List<Map<String, Object>> artifactList = getList("/spk/ipd/flow-runs/" + flowRunId + "/artifacts");
        assertTrue(!artifactList.isEmpty(), "artifacts 为空（fast-mode 桩未落产物）");

        // 5. 证据验证口：evidence 非空（裸 List；SpkEvidenceService.append 哈希链）
        List<Map<String, Object>> evidenceList = getList("/spk/ipd/flow-runs/" + flowRunId + "/evidence");
        assertTrue(!evidenceList.isEmpty(), "evidence 为空（无证据记录）");

        // 6. 时间线：events 含 BPM 活动（serviceTask 已执行）
        Map<String, Object> timeline = get("/spk/ipd/flow-runs/" + flowRunId + "/timeline");
        assertNotNull(timeline, "timeline 返回空");
        List<Map<String, Object>> events = extractList(timeline.get("events"));
        assertTrue(!events.isEmpty(), "timeline events 为空（无 BPM 活动记录）");

        // 7. 骨架端点：diagram/engineering 显式 skeleton=true（C-14 禁止假装可用）
        Map<String, Object> diagram = get("/spk/ipd/flow-runs/" + flowRunId + "/diagram");
        assertEquals(Boolean.TRUE, diagram.get("skeleton"), "diagram 未标 skeleton=true");
        Map<String, Object> engineering = get("/spk/ipd/flow-runs/" + flowRunId + "/engineering");
        assertEquals(Boolean.TRUE, engineering.get("skeleton"), "engineering 未标 skeleton=true");

        // 8. 状态机：block（RUNNING→BLOCKED；不取消引擎）
        Map<String, Object> blockBody = Map.of("reason", "E2E 人工阻断测试");
        Map<String, Object> blocked = post("/spk/ipd/flow-runs/" + flowRunId + "/block", blockBody,
                "e2e-layered-block-" + flowRunId + "-" + uid);
        assertEquals("BLOCKED", blocked.get("status"), "block 后 status 非 BLOCKED");

        // 9. 状态机前置校验：BLOCKED 非 FAILED → retry 应被拒（IPD_FLOW_RUN_NOT_RETRYABLE）
        Map<String, Object> retryOnBlocked = postRaw("/spk/ipd/flow-runs/" + flowRunId + "/retry",
                Map.of(), "e2e-layered-retry-blocked-" + flowRunId + "-" + uid);
        assertNotEquals(0, retryOnBlocked.get("code"),
                "BLOCKED 态 retry 未被状态机拒绝：" + retryOnBlocked);

        // 10. 状态机：unblock（BLOCKED→RUNNING；审计理由）
        Map<String, Object> unblockBody = Map.of("reason", "E2E 解除阻断测试");
        Map<String, Object> unblocked = post("/spk/ipd/flow-runs/" + flowRunId + "/unblock", unblockBody,
                "e2e-layered-unblock-" + flowRunId + "-" + uid);
        assertEquals("RUNNING", unblocked.get("status"), "unblock 后 status 非 RUNNING");

        // 11. 状态机：cancel（RUNNING→CANCELLED；调引擎取消）
        Map<String, Object> cancelBody = Map.of("reason", "E2E 取消测试");
        Map<String, Object> cancelled = post("/spk/ipd/flow-runs/" + flowRunId + "/cancel", cancelBody,
                "e2e-layered-cancel-" + flowRunId + "-" + uid);
        assertEquals("CANCELLED", cancelled.get("status"), "cancel 后 status 非 CANCELLED");

        // 12. 终态前置校验：CANCELLED → retry/block/start 均应被拒
        Map<String, Object> retryOnCancelled = postRaw("/spk/ipd/flow-runs/" + flowRunId + "/retry",
                Map.of(), "e2e-layered-retry-cancelled-" + flowRunId + "-" + uid);
        assertNotEquals(0, retryOnCancelled.get("code"),
                "CANCELLED 态 retry 未被状态机拒绝：" + retryOnCancelled);
        Map<String, Object> blockOnCancelled = postRaw("/spk/ipd/flow-runs/" + flowRunId + "/block",
                Map.of("reason", "should reject"), "e2e-layered-block-cancelled-" + flowRunId + "-" + uid);
        assertNotEquals(0, blockOnCancelled.get("code"),
                "CANCELLED 态 block 未被状态机拒绝：" + blockOnCancelled);
        Map<String, Object> startOnCancelled = postRaw("/spk/ipd/flow-runs/" + flowRunId + "/start",
                Map.of(), "e2e-layered-start-cancelled-" + flowRunId + "-" + uid);
        assertNotEquals(0, startOnCancelled.get("code"),
                "CANCELLED 态 start 未被状态机拒绝：" + startOnCancelled);

        // 13. cancel 幂等：同 Idempotency-Key 重复 cancel 不报错（返回当前态）
        Map<String, Object> cancelDup = post("/spk/ipd/flow-runs/" + flowRunId + "/cancel", cancelBody,
                "e2e-layered-cancel-" + flowRunId + "-" + uid);
        assertEquals("CANCELLED", cancelDup.get("status"), "重复 cancel 同 key 未返回 CANCELLED 态");

        // 14. 兜底直查 contract 行（dispatchActivityAsync 落 spk_task_contract，关联键 process_instance_id）
        String pid = processInstanceIdOf(flowRunId);
        assertNotNull(pid, "flow_run 无 process_instance_id");
        assertTrue(contractCountByPid(pid) > 0,
                "spk_task_contract 无 process_instance_id=" + pid + " 的行");
    }
}
