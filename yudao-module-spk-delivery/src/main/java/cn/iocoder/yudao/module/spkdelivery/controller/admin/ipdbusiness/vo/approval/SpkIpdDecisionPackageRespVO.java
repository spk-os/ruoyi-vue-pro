package cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.approval;

import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.artifact.SpkArtifactManifestDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ccb.SpkCcbRecordDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.dcp.SpkDcpRedirectLogDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.evidence.SpkEvidenceRecordDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.gate.SpkGateRecordDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdDecisionRecordDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdEvidenceWaiverDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * IPD 决策包 Response VO（设计文档 §7.9 / §10.8）
 * <p>
 * 一次返回业务摘要、差异、证据、风险、历史决策、候选动作。
 *
 * @author SPK-OS
 */
@Schema(description = "管理后台 - IPD 决策包 Response VO")
@Data
public class SpkIpdDecisionPackageRespVO {

    @Schema(description = "页头摘要：项目/版本/类型/阶段/门禁/提交人/当前审批人/计划日期/阻断数/等待时长")
    private Map<String, Object> header;

    @Schema(description = "决策摘要六区：目标与范围/计划与投入/技术与质量/风险与例外/必需产物/审批影响")
    private List<SummarySection> summary;

    @Schema(description = "必需产物清单与状态")
    private List<RequiredArtifact> requiredArtifacts;

    @Schema(description = "门禁记录")
    private List<SpkGateRecordDO> gates;

    @Schema(description = "证据链")
    private List<SpkEvidenceRecordDO> evidence;

    @Schema(description = "证据哈希链是否校验通过")
    private Boolean chainValid;

    @Schema(description = "产物清单")
    private List<SpkArtifactManifestDO> artifacts;

    @Schema(description = "CCB 变更台账")
    private List<SpkCcbRecordDO> ccb;

    @Schema(description = "DCP 重定向记录")
    private List<SpkDcpRedirectLogDO> dcpRedirects;

    @Schema(description = "历史决策")
    private List<SpkIpdDecisionRecordDO> decisions;

    @Schema(description = "证据豁免")
    private List<SpkIpdEvidenceWaiverDO> waivers;

    @Schema(description = "强阻断项（缺必需产物/门禁未过/证据链失败且无豁免）")
    private List<String> blockingItems;

    @Schema(description = "候选动作：APPROVE/REJECT/REDIRECT/RETURN 及是否可用")
    private List<CandidateAction> candidateActions;

    @Schema(description = "决策包快照 hash（sha256:...），前端决策时原样回传")
    private String decisionPackageHash;

    @Schema(description = "决策包构建时间")
    private String builtAt;

    @Data
    public static class SummarySection {
        @Schema(description = "区标题")
        private String title;
        @Schema(description = "行：label/value/type(info|warning|danger|success)")
        private List<SummaryRow> rows;
    }

    @Data
    public static class SummaryRow {
        private String label;
        private String value;
        private String type;
    }

    @Data
    public static class RequiredArtifact {
        @Schema(description = "产物/证据引用")
        private String ref;
        @Schema(description = "类型 ARTIFACT/GATE/TR/TEST/RELEASE")
        private String type;
        @Schema(description = "状态 PASS/MISSING/WARN/FAIL")
        private String status;
        @Schema(description = "是否已签名")
        private Boolean signed;
        @Schema(description = "扫描状态")
        private String scanStatus;
        @Schema(description = "Verifier 结论")
        private String verifier;
        @Schema(description = "hash 校验是否通过")
        private Boolean hashOk;
        @Schema(description = "是否构成阻断")
        private Boolean blocking;
    }

    @Data
    public static class CandidateAction {
        @Schema(description = "APPROVE/REJECT/REDIRECT/RETURN")
        private String decision;
        @Schema(description = "是否可用")
        private Boolean enabled;
        @Schema(description = "不可用原因")
        private String reason;
    }
}
