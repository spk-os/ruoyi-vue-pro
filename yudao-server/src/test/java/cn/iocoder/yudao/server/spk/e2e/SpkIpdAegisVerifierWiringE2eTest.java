package cn.iocoder.yudao.server.spk.e2e;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.agentdef.SpkAgentDefDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.aegis.SpkAegisReviewDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.artifact.SpkArtifactManifestDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.taskcontract.SpkTaskContractDO;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.agentdef.SpkAgentDefMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.artifact.SpkArtifactManifestMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.taskcontract.SpkTaskContractMapper;
import cn.iocoder.yudao.module.spkdelivery.service.aegis.SpkAegisReviewService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Aegis review → 三层校验 接线端到端集成测试（遗留风险#1 闭合验证）。
 * <p>
 * 验证 FULL_RELEASE 流程的 TR 节点经 {@code /spk/aegis/review} → {@link SpkAegisReviewService#review}
 * 真实触发 {@link cn.iocoder.yudao.module.spkdelivery.service.verifier.SpkVerifierService#verify} 三层校验
 * （Layer1 结构/schema + Layer2 验收/acceptance + Layer3 LLM 聚合），而非仅走单次 LLM reviewer。
 * <p>
 * 不跑完整 BPMN（慢测 1103s），改为种真实 contract+artifact（ACT-FULL-CONCEPT-1 / req-insight-report，
 * def 已 useIndependentVerifier=1+verifierType=TR）后直接调 review，断言：
 * ① 三层 receipt 落 spk_verification_receipt，evidence_points 含"结构校验"+"验收"双层证据；
 * ② aegis review 报告含"三层独立核证"段，verdict 反映三层 FAIL 结论（stub 缺必填→Layer1 FAIL）。
 * <p>
 * 手动跑：mvn test -pl yudao-server -Dtest=SpkIpdAegisVerifierWiringE2eTest -Dsurefire.excludedGroups=
 *
 * @author SPK-OS
 */
@Tag("e2e")
class SpkIpdAegisVerifierWiringE2eTest extends SpkIpdE2eBase {

    @Autowired
    private SpkAegisReviewService aegisReviewService;
    @Autowired
    private SpkTaskContractMapper taskContractMapper;
    @Autowired
    private SpkArtifactManifestMapper artifactManifestMapper;
    @Autowired
    private SpkAgentDefMapper agentDefMapper;

    @Test
    void aegis_review触发三层校验_概念产物落receipt含结构与验收证据() {
        long uid = System.nanoTime();
        String pid = "pid-aw-" + uid;
        String activityRunId = "run-aw-" + uid;
        String contractId = "ct-aw-" + uid;
        String artifactId = "art-aw-" + uid;

        // 测试主线程无 HTTP 请求→无租户上下文，MyBatis TenantLineInnerInterceptor 抛"不存在租户编号"
        // （同 SpkIpdVerifierLayerE2eTest）。SPK IPD 单租户 tenant_id=1，显式包裹整条调用栈。
        Long prevTenant = TenantContextHolder.getTenantId();
        TenantContextHolder.setTenantId(1L);
        try {
            // 前置：verifier-tr 必须存在（aegis review 按 TR 选 verifier；DB 已存 roleId=1）
            SpkAgentDefDO verifier = agentDefMapper.selectByCode("verifier-tr");
            assertNotNull(verifier, "verifier-tr agent def 不存在（pickVerifier 将返回 null 跳过三层校验）");

            // 种 contract：ACT-FULL-CONCEPT-1（def useIndependentVerifier=1 / verifierType=TR）
            SpkTaskContractDO contract = SpkTaskContractDO.builder()
                    .contractId(contractId)
                    .activityRunId(activityRunId)
                    .activityId("ACT-FULL-CONCEPT-1")
                    .activityVersion("1.0.0")
                    .processInstanceId(pid)
                    .executionMode("task_system")
                    .leadAgentId(verifier.getId())
                    .leadAgentCode("lead-req-insight")
                    .nodeKey("n_concept_t1")
                    .status("succeeded")
                    .build();
            taskContractMapper.insert(contract);

            // 种 artifact：req-insight-report，metadata 故意缺 schema 必填字段（report_id/persona/...）
            // → Layer1 结构校验 FAIL；artifactJson 可解析 → Layer2 验收规则在其上求值。
            String stubMeta = "{\"deliverable\":\"stub\",\"mode\":\"fast\","
                    + "\"summary\":\"E2E aegis-wiring 三层校验桩（非真实 ReqInsightReport）\"}";
            SpkArtifactManifestDO artifact = SpkArtifactManifestDO.builder()
                    .artifactId(artifactId)
                    .artifactType("req-insight-report")
                    .activityRunId(activityRunId)
                    .processInstanceId(pid)
                    .uri("ipd://" + artifactId)
                    .contentHash("e2e-stub-" + uid)
                    .metadata(stubMeta)
                    .summary("概念阶段·需求洞察 产物（E2E aegis-wiring 桩）")
                    .build();
            artifactManifestMapper.insert(artifact);

            // 触发：aegis review（FULL flow TR 节点经 /spk/aegis/review 即调本方法 → runIndependentVerifier）
            SpkAegisReviewDO review = aegisReviewService.review(pid, "n_concept_tr2");
            assertNotNull(review, "aegis review 返回 null");

            // 断言1：三层校验收据落库，evidence_points 含 Layer1 结构 + Layer2 验收 双层证据
            List<Map<String, Object>> receipts = jdbc.queryForList(
                    "SELECT overall_conclusion, evidence_points, summary FROM spk_verification_receipt "
                            + "WHERE activity_run_id = ? AND deleted = 0", activityRunId);
            assertFalse(receipts.isEmpty(),
                    "三层校验 receipt 未落库（aegis review 未触发 SpkVerifierService.verify，遗留风险#1 未闭合）");
            Map<String, Object> rrow = receipts.get(0);
            assertNotNull(rrow.get("overall_conclusion"), "落库 overall_conclusion 为空（aggregate 未产出结论）");
            String ev = String.valueOf(rrow.get("evidence_points"));
            assertTrue(ev.contains("结构校验"),
                    "evidence_points 未含 Layer1 结构校验证据（schema.json 未加载/求值）：" + ev);
            assertTrue(ev.contains("验收"),
                    "evidence_points 未含 Layer2 验收证据（acceptance.json assert 未求值）：" + ev);

            // 断言2：aegis review 报告含"三层独立核证"段，verdict 反映三层 FAIL 结论
            assertTrue(review.getEvidence().contains("三层独立核证"),
                    "aegis evidence 未含三层独立核证段（runIndependentVerifier 未把收据并入报告）：" + review.getEvidence());
            assertEquals("fail", review.getVerdict(),
                    "aegis verdict 未反映三层 FAIL 结论（stub 缺必填应致 Layer1 FAIL→verdict=fail）：" + review.getVerdict());
        } finally {
            // 清理本测试 fixture（receipt/evidence/artifact/contract/aegis_review，合成 id 非业务数据，可逆）
            jdbc.update("DELETE FROM spk_verification_receipt WHERE activity_run_id = ?", activityRunId);
            jdbc.update("DELETE FROM spk_evidence_record WHERE activity_run_id = ?", activityRunId);
            jdbc.update("DELETE FROM spk_artifact_manifest WHERE activity_run_id = ?", activityRunId);
            jdbc.update("DELETE FROM spk_task_contract WHERE activity_run_id = ?", activityRunId);
            jdbc.update("DELETE FROM spk_aegis_review WHERE instance_id = ?", pid);
            if (prevTenant != null) {
                TenantContextHolder.setTenantId(prevTenant);
            } else {
                TenantContextHolder.clear();
            }
        }
    }
}
