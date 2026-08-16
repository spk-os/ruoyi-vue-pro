package cn.iocoder.yudao.server.spk.e2e;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SPK-OS IPD 端到端烟测：验证"发起→start→首审批门→APPROVE 推进"整条管道通。
 * <p>
 * 覆盖：真 dev PG + 固定端口 48081 + 触发器 URL 回环 + fast-mode 桩 adapter + 异步 route +
 * 审批门 receiveTask 推进。跑通即证明 P0 修复后的端到端链路无阻塞。
 * <p>
 * 手动跑：mvn test -pl yudao-server -Dtest=SpkIpdSmokeE2eTest -DexcludedGroups=
 * 前置：dev 48080 实例停止（避免 Flowable 最新流程版本被覆盖回 48080）。
 *
 * @author SPK-OS
 */
@Tag("e2e")
class SpkIpdSmokeE2eTest extends SpkIpdE2eBase {

    @Test
    void 全链路烟测_发起到首审批门APPROVE推进() {
        long uid = System.nanoTime();

        // 1. 创建项目（projectCode 唯一后缀，不主动清库只断言自建行）
        Map<String, Object> proj = post("/spk/ipd/projects", Map.of(
                "projectCode", "E2E-SMOKE-" + uid,
                "name", "E2E烟测项目" + uid,
                "objective", "端到端集成测试烟测",
                "ownerUserId", 1,
                "plannedEndAt", "2026-12-31T00:00:00"));
        Long projectId = ((Number) proj.get("id")).longValue();
        assertNotNull(projectId, "项目 id 为空");
        assertEquals("E2E-SMOKE-" + uid, proj.get("projectCode"), "projectCode 未回显");

        // 2. 创建大版本 + 基线版本（majorNo 是 Integer 非 "Vx" 字符串；createBaselineVersion=true 自动建 Vx.0 基线）
        Map<String, Object> mr = post("/spk/ipd/projects/" + projectId + "/major-releases", Map.of(
                "majorNo", (int) (uid % 100000) + 1,
                "name", "烟测大版本",
                "objective", "烟测大版本目标",
                "scopeSummary", "smoke scope",
                "ownerUserId", 1,
                "createBaselineVersion", true));
        Long majorId = ((Number) mr.get("id")).longValue();
        Object baselineVersionIdRaw = mr.get("baselineVersionId");
        assertNotNull(baselineVersionIdRaw, "基线版本未自动创建（createBaselineVersion=true 未生效）");
        Long baselineVersionId = ((Number) baselineVersionIdRaw).longValue();
        // 烟测直接用基线版本跑 FULL_RELEASE（validateFlowTypeContext: FULL_RELEASE 只能绑 BASELINE，
        // INCREMENT 版本配 FULL_RELEASE → IPD_FLOW_TYPE_MISMATCH 1050116033）。
        // INCREMENT_RELEASE + INCREMENT 版本组合留给分层测试覆盖。
        Long versionId = baselineVersionId;

        // 3. 预检（不落运行只查可启动性；基线版本草稿 readiness 可能 NOT_READY，preflight 不阻断 createFlowRun）
        Map<String, Object> pf = post("/spk/ipd/flow-runs/preflight", Map.of(
                "projectId", projectId,
                "versionId", versionId,
                "flowType", "FULL_RELEASE"));
        assertNotNull(pf, "预检返回空");

        // 4. 创建 FlowRun 草稿
        Map<String, Object> fr = post("/spk/ipd/flow-runs", Map.of(
                "projectId", projectId,
                "versionId", versionId,
                "flowType", "FULL_RELEASE"));
        Long flowRunId = ((Number) fr.get("id")).longValue();
        assertNotNull(flowRunId, "FlowRun id 为空");

        // 5. 幂等启动（Idempotency-Key 头；start 同步至首个 receiveTask 即返 processInstanceId）
        Map<String, Object> start = post("/spk/ipd/flow-runs/" + flowRunId + "/start",
                Map.of(), "e2e-smoke-start-" + flowRunId + "-" + uid);
        assertNotNull(start, "start 返回空");

        // 6. 等待首审批门 receiveTask 卡住 → approval-tasks 出现 todo 任务
        //    fast-mode 桩同步跑完 serviceTask 到 receiveTask，应在数秒~数十秒内出现。
        //    approval-tasks PageReqVO 无 flowRunId 过滤参数，故 type=todo 拿当前用户全部待办，
        //    再按 RespVO.flowRunId client 端匹配本 FlowRun 的审批门任务。
        AtomicReference<Map<String, Object>> todoTask = new AtomicReference<>();
        Awaitility.await()
                .atMost(Duration.ofSeconds(180))
                .pollInterval(Duration.ofSeconds(3))
                .untilAsserted(() -> {
                    Map<String, Object> data = get("/spk/ipd/approval-tasks?type=todo");
                    List<Map<String, Object>> all = extractList(data);
                    List<Map<String, Object>> mine = all.stream()
                            .filter(t -> flowRunId.toString().equals(String.valueOf(t.get("flowRunId"))))
                            .toList();
                    assertTrue(!mine.isEmpty(),
                            "首审批门 todo 未出现（FlowRun=" + flowRunId
                                    + " 仍卡在 serviceTask/未到 receiveTask；当前用户 todo 总数=" + all.size() + ")");
                    todoTask.set(mine.get(0));
                });
        String taskId = taskIdOf(todoTask.get());
        assertNotNull(taskId, "审批任务 id 为空");

        // 8. 取决策包（candidateActions + decisionPackageHash）
        Map<String, Object> pkg = get("/spk/ipd/approval-tasks/" + taskId + "/decision-package");
        assertNotNull(pkg, "决策包返回空");
        String hash = pkg.get("decisionPackageHash") == null ? null : String.valueOf(pkg.get("decisionPackageHash"));

        // 9. 决策 APPROVE（decision 值优先取 candidateActions 首个 enabled.action，兜底 APPROVE；forceOverride=true）
        String decision = pickDecisionAction(pkg);
        Map<String, Object> decBody = new java.util.HashMap<>();
        decBody.put("decision", decision);
        decBody.put("reason", "E2E 烟测通过");
        if (hash != null) {
            decBody.put("decisionPackageHash", hash);
        }
        decBody.put("forceOverride", true);
        Map<String, Object> dec = post("/spk/ipd/approval-tasks/" + taskId + "/decisions", decBody);
        assertNotNull(dec, "决策返回空");

        // 10. 推进后断言：FlowRun 仍在运行/推进，decisions 列表非空，contract 行已落
        Awaitility.await()
                .atMost(Duration.ofSeconds(60))
                .pollInterval(Duration.ofSeconds(2))
                .untilAsserted(() -> {
                    List<Map<String, Object>> decs = getList("/spk/ipd/flow-runs/" + flowRunId + "/decisions");
                    assertTrue(!decs.isEmpty(), "决策未落 decisions 列表");
                });

        // 11. 验证流程推进产物：timeline 非空 + 兜底直查 contract 行
        //     SpkTaskContractDO 无 flowRunId 字段，关联键是 processInstanceId（列 process_instance_id），
        //     businessKey 是 "IPD:<runNo>" 非 projectId，故走 flowRunId→pid→contract 两步查（dispatchActivityAsync
        //     在 SpkTaskRouterService:136 落 contract.processInstanceId）。
        Map<String, Object> timeline = get("/spk/ipd/flow-runs/" + flowRunId + "/timeline");
        assertNotNull(timeline, "timeline 返回空（流程推进无产物）");

        String pid = jdbc.queryForObject(
                "SELECT process_instance_id FROM spk_ipd_flow_run WHERE id = ?", String.class, flowRunId);
        assertNotNull(pid, "spk_ipd_flow_run 无 process_instance_id（start 未起流程实例）");
        Integer contractCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM spk_task_contract WHERE process_instance_id = ?", Integer.class, pid);
        assertTrue(contractCount != null && contractCount > 0,
                "spk_task_contract 无 process_instance_id=" + pid + " 的行（dispatchActivityAsync 未落 contract）");
    }

    /** 从决策包 candidateActions 取首个 enabled 的 action 作为 decision 值；兜底 APPROVE。 */
    @SuppressWarnings("unchecked")
    private String pickDecisionAction(Map<String, Object> pkg) {
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
}
