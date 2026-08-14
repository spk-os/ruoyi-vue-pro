package cn.iocoder.yudao.module.spkdelivery.service.ipdbusiness;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.approval.SpkIpdApprovalTaskPageReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.approval.SpkIpdApprovalTaskRespVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.approval.SpkIpdDecisionPackageRespVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.approval.SpkIpdDecisionReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.approval.SpkIpdEvidenceWaiverReqVO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.artifact.SpkArtifactManifestDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.evidence.SpkEvidenceRecordDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdDecisionRecordDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdEvidenceWaiverDO;

import java.util.List;

/**
 * IPD 审批与决策包服务（设计文档 §10.8 / §7.9）。
 * <p>
 * 在原生 BPM 待办/已办与 approve/reject/return 基础上做业务包装：
 * 决策包聚合（项目/版本/证据/产物/门禁/CCB/历史决策）、不可变决策记录、证据豁免。
 * 不改 BPM 引擎语义，只扩展。
 *
 * @author SPK-OS
 */
public interface SpkIpdApprovalService {

    /** 分页查询 IPD 审批待办/已办（type=todo/done） */
    PageResult<SpkIpdApprovalTaskRespVO> pageApprovalTasks(Long userId, SpkIpdApprovalTaskPageReqVO req);

    /** 决策包：业务摘要、差异、证据、风险、历史决策、候选动作 */
    SpkIpdDecisionPackageRespVO getDecisionPackage(String taskId);

    /** 写不可变决策并调用原生 BPM approve/reject/return */
    SpkIpdDecisionRecordDO createDecision(Long userId, String taskId, SpkIpdDecisionReqVO req);

    /** DCP/TR/干预历史（按 FlowRun） */
    List<SpkIpdDecisionRecordDO> listDecisionsByFlowRun(Long flowRunId);

    /** 证据豁免 */
    SpkIpdEvidenceWaiverDO createEvidenceWaiver(Long userId, String taskId, SpkIpdEvidenceWaiverReqVO req);

    /** FlowRun 产物清单 */
    List<SpkArtifactManifestDO> getArtifacts(Long flowRunId);

    /** FlowRun 证据及缺口 */
    List<SpkEvidenceRecordDO> getEvidence(Long flowRunId);
}
