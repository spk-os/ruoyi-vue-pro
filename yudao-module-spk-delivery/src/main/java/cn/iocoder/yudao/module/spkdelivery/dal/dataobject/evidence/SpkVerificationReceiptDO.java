package cn.iocoder.yudao.module.spkdelivery.dal.dataobject.evidence;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Verification Receipt DO —— Independent Verifier 验证收据（Evidence 之二）
 * <p>
 * 每个 evidence_point 结论 + 整体结论 PASS/CONDITIONAL/FAIL。依 §4.4。
 *
 * @author SPK-OS
 */
@TableName("spk_verification_receipt")
@KeySequence("spk_verification_receipt_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpkVerificationReceiptDO extends BaseDO {

    @TableId
    private Long id;
    private String receiptId;
    private String artifactId;
    private String activityRunId;
    private Long verifierId;
    private String verifierCode;
    /**
     * TR/DCP/Audit
     */
    private String verifierType;
    /**
     * recheck/redteam/completeness/traceback
     */
    private String verificationMethod;
    /**
     * 证据点列表 JSON [{point,verdict:Confirmed/Challenged/Missing/Contradicted}]
     */
    private String evidencePoints;
    /**
     * PASS/CONDITIONAL/FAIL
     */
    private String overallConclusion;
    private String modelSnapshotId;
    /**
     * 摘要
     */
    private String summary;
    private LocalDateTime signedAt;

}
