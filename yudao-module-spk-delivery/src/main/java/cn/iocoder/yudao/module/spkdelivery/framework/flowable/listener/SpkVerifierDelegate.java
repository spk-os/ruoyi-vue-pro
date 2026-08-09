package cn.iocoder.yudao.module.spkdelivery.framework.flowable.listener;

import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.agentdef.SpkAgentDefDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.artifact.SpkArtifactManifestDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.evidence.SpkVerificationReceiptDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.taskcontract.SpkTaskContractDO;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.agentdef.SpkAgentDefMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.artifact.SpkArtifactManifestMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.taskcontract.SpkTaskContractMapper;
import cn.iocoder.yudao.module.spkdelivery.service.verifier.SpkVerifierService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * SPK-OS SpkVerifierDelegate —— DCP/TR/CCB 子流程中「独立核证」节点的 Flowable JavaDelegate
 * （设计 §6.4/§6.5/§6.6：dcp-*-verifier / tr-verifier / ccb-audit-verifier）。
 * <p>
 * 由 BPMN XML serviceTask(delegateExpression="${spkVerifierDelegate}") 触发。
 * 取最近一次 ActivityRun 的产物，按 verifier_type 选定 verifier 角色，
 * 调 {@link SpkVerifierService#verify} 产出 VerificationReceipt，
 * 把 {@code overallConclusion}（PASS/FAIL/CONDITIONAL）与 {@code summary} 写回流程变量
 * {@code verificationConclusion} / {@code verificationSummary}，供后续 exclusiveGateway 分支决策。
 *
 * <p><b>幂等/降级</b>：activityRunId/产物/verifier 任一缺失则写 CONCLUSION=SKIP 并记日志，
 * 不阻断流程（sub-process 部署态非活流程时常见，安全 no-op）。
 *
 * @author SPK-OS
 */
@Slf4j
@Component("spkVerifierDelegate")
public class SpkVerifierDelegate implements JavaDelegate {

    public static final String VAR_VERIFIER_TYPE = "verifierType";
    public static final String VAR_VERIFICATION_CONCLUSION = "verificationConclusion";
    public static final String VAR_VERIFICATION_SUMMARY = "verificationSummary";

    @Resource
    private SpkVerifierService verifierService;
    @Resource
    private SpkTaskContractMapper contractMapper;
    @Resource
    private SpkArtifactManifestMapper artifactMapper;
    @Resource
    private SpkAgentDefMapper agentDefMapper;

    @Override
    public void execute(DelegateExecution execution) {
        String instanceId = execution.getProcessInstanceId();
        String verifierType = Objects.toString(execution.getVariable(VAR_VERIFIER_TYPE), null);
        // 取本实例最近一次合同（即上一节点的产物所属 ActivityRun）
        SpkTaskContractDO contract = latestContract(instanceId);
        if (contract == null || contract.getActivityRunId() == null) {
            log.warn("[execute][无 ActivityRun 可核证 instanceId={} verifierType={}，写 SKIP]",
                    instanceId, verifierType);
            execution.setVariable(VAR_VERIFICATION_CONCLUSION, "SKIP");
            execution.setVariable(VAR_VERIFICATION_SUMMARY, "无前置 ActivityRun，跳过核证");
            return;
        }
        List<SpkArtifactManifestDO> arts = artifactMapper.selectListByActivityRunId(contract.getActivityRunId());
        if (arts == null || arts.isEmpty()) {
            log.warn("[execute][无产物可核证 activityRunId={}，写 SKIP]", contract.getActivityRunId());
            execution.setVariable(VAR_VERIFICATION_CONCLUSION, "SKIP");
            execution.setVariable(VAR_VERIFICATION_SUMMARY, "无前置产物，跳过核证");
            return;
        }
        SpkArtifactManifestDO artifact = arts.get(arts.size() - 1);
        SpkAgentDefDO verifier = pickVerifier(verifierType, contract);
        if (verifier == null) {
            log.warn("[execute][无可用 verifier verifierType={}，写 SKIP]", verifierType);
            execution.setVariable(VAR_VERIFICATION_CONCLUSION, "SKIP");
            execution.setVariable(VAR_VERIFICATION_SUMMARY, "无 verifier 角色匹配，跳过核证");
            return;
        }
        SpkVerificationReceiptDO receipt = verifierService.verify(verifier, artifact,
                contract.getActivityRunId(), instanceId);
        String conclusion = receipt != null && receipt.getOverallConclusion() != null
                ? receipt.getOverallConclusion() : "CONDITIONAL";
        String summary = receipt != null ? receipt.getSummary() : "";
        execution.setVariable(VAR_VERIFICATION_CONCLUSION, conclusion);
        execution.setVariable(VAR_VERIFICATION_SUMMARY, summary);
        log.info("[execute][核证完成 activityRunId={} verifierType={} conclusion={}]", contract.getActivityRunId(), verifierType, conclusion);
    }

    private SpkTaskContractDO latestContract(String instanceId) {
        List<SpkTaskContractDO> list = contractMapper.selectListByProcessInstanceId(instanceId);
        return (list == null || list.isEmpty()) ? null : list.get(list.size() - 1);
    }

    /**
     * 按 verifier_type（TR/SEC/RE/AUDIT）从 agent_def（agent_kind=verifier）选 verifier 角色；缺失回退任意 verifier。
     */
    private SpkAgentDefDO pickVerifier(String verifierType, SpkTaskContractDO contract) {
        List<SpkAgentDefDO> verifiers = agentDefMapper.selectListByAgentKind("verifier");
        if (verifiers == null || verifiers.isEmpty()) {
            return null;
        }
        if (verifierType != null && !verifierType.isBlank()) {
            String vt = verifierType.toUpperCase();
            for (SpkAgentDefDO v : verifiers) {
                if (vt.equals(v.getVerifierType())) {
                    return v;
                }
            }
        }
        return verifiers.get(0);
    }
}
