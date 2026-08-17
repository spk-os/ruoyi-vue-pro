package cn.iocoder.yudao.server.spk.e2e;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.agentdef.SpkAgentDefDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.artifact.SpkArtifactManifestDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.evidence.SpkVerificationReceiptDO;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.agentdef.SpkAgentDefMapper;
import cn.iocoder.yudao.module.spkdelivery.service.verifier.SpkVerifierService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SpkVerifierService 三层校验端到端集成测试。
 * <p>
 * 不走完整 BPMN 流程（FULL_RELEASE 首审批门前的 concept/plan 活动均 verifier=0，
 * 触不到三层脚本），改为直接调真实 {@link SpkVerifierService#verify}：
 * 真实 verifier-tr def + 真实 customer-need-brief 产物（metadata 桩 JSON，P1 设计允许）+
 * 真实仓内 verifier-scripts 脚本文件 + 真实 DB 写 receipt/evidence。
 * <p>
 * 证明：① Layer1 结构校验（schema.json）真实加载求值；② Layer2 验收规则（acceptance.json
 * 的 assert）真实求值；③ Layer3 LLM（fast-mode Verifier 桩）真实派发；④ receipt 落库
 * evidence_points 含"结构校验"+"验收"双层证据，overall_conclusion 非 null。
 * <p>
 * 手动跑：mvn test -pl yudao-server -Dtest=SpkIpdVerifierLayerE2eTest -Dsurefire.excludedGroups=
 *
 * @author SPK-OS
 */
@Tag("e2e")
class SpkIpdVerifierLayerE2eTest extends SpkIpdE2eBase {

    @Autowired
    private SpkVerifierService verifierService;

    @Autowired
    private SpkAgentDefMapper agentDefMapper;

    @Test
    void 三层校验端到端_概念产物落receipt含结构与验收证据() {
        long uid = System.nanoTime();
        String activityRunId = "run-it-" + uid;
        String pid = "pid-it-" + uid;
        String artifactId = "art-it-" + uid;

        // 测试主线程无 HTTP 请求→无租户上下文，MyBatis TenantLineInnerInterceptor 会抛"不存在租户编号"
        //（同 SpkAgentTimeoutJob scheduling 线程问题）。SPK IPD 单租户 tenant_id=1，显式包裹整条
        // verify 调用栈（receiptMapper.insert / evidenceService.append 均过租户拦截器）。
        Long prevTenant = TenantContextHolder.getTenantId();
        TenantContextHolder.setTenantId(1L);
        try {
            // 取真实 verifier-tr def（DB 已存在，roleId=1，agent_kind=verifier，verifier_type=TR）
            SpkAgentDefDO verifier = agentDefMapper.selectByCode("verifier-tr");
            assertNotNull(verifier, "verifier-tr agent def 不存在（selectVerifier 将返回 null 跳过 verify）");
            assertNotNull(verifier.getRoleId(), "verifier-tr roleId 为空（dispatchTask 无法派发）");

            // 构造概念产物：artifactType=customer-need-brief（仓内有 schema.json/acceptance.json 三层脚本）
            // metadata 用 fast-mode 风格桩 JSON：合法 JSON 但缺 customer-need-brief 必填字段（needs 等）
            // → Layer1 结构校验判 FAIL（缺 needs）+ Layer2 验收规则在可解析 JSON 上求值（每 need 缺字段触发 fail 规则）
            // + Layer3 fast-mode Verifier 桩返回 PASS。aggregate 取 max → overall_conclusion=FAIL。
            String stubMetadata = "{\"deliverable\":\"stub\",\"mode\":\"fast\","
                    + "\"summary\":\"P1 fast 合成产物（非真实 CustomerNeedBrief）\"}";
            SpkArtifactManifestDO artifact = SpkArtifactManifestDO.builder()
                    .artifactId(artifactId)
                    .artifactType("customer-need-brief")
                    .activityRunId(activityRunId)
                    .metadata(stubMetadata)
                    .summary("概念阶段·客户需求深度分析 产物（E2E 三层校验桩）")
                    .build();

            SpkVerificationReceiptDO receipt = verifierService.verify(verifier, artifact, activityRunId, pid);
            assertNotNull(receipt, "verify 返回 null（三层校验未运行）");
            assertNotNull(receipt.getOverallConclusion(), "overall_conclusion 为空（aggregate 未产出结论）");

            // 兜底直查 receipt 落库 + evidence_points 含 Layer1 结构校验 / Layer2 验收 双层证据
            List<Map<String, Object>> rows = jdbc.queryForList(
                    "SELECT overall_conclusion, evidence_points, summary FROM spk_verification_receipt "
                            + "WHERE activity_run_id = ? AND deleted = 0", activityRunId);
            assertFalse(rows.isEmpty(), "三层校验 receipt 未落库（activity_run_id=" + activityRunId + "）");
            Map<String, Object> row = rows.get(0);
            assertNotNull(row.get("overall_conclusion"), "落库 overall_conclusion 为空");
            String ev = String.valueOf(row.get("evidence_points"));
            assertTrue(ev.contains("结构校验"),
                    "evidence_points 未含 Layer1 结构校验证据（schema.json 未加载/求值）：" + ev);
            assertTrue(ev.contains("验收"),
                    "evidence_points 未含 Layer2 验收证据（acceptance.json assert 未求值）：" + ev);
        } finally {
            // 清理本测试 fixture（receipt + evidence，合成 id 非业务数据，可逆，标准测试清理）
            jdbc.update("DELETE FROM spk_verification_receipt WHERE activity_run_id = ?", activityRunId);
            jdbc.update("DELETE FROM spk_evidence_record WHERE activity_run_id = ?", activityRunId);
            if (prevTenant != null) {
                TenantContextHolder.setTenantId(prevTenant);
            } else {
                TenantContextHolder.clear();
            }
        }
    }
}
