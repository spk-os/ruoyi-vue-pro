package cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * IPD 证据豁免 DO —— 必需产物/证据缺失时经授权的例外。
 * <p>
 * 设计文档 §10.8：POST /approval-tasks/{taskId}/evidence-waivers，
 * 有权限的例外豁免；必须时限、理由、补证责任人。
 *
 * @author SPK-OS
 */
@TableName("spk_ipd_evidence_waiver")
@KeySequence("spk_ipd_evidence_waiver_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpkIpdEvidenceWaiverDO extends BaseDO {

    @TableId
    private Long id;
    private String taskId;
    private String processInstanceId;
    private Long flowRunId;
    private Long projectId;
    private Long versionId;
    /** 缺失证据/产物引用 */
    private String evidenceRef;
    /** ARTIFACT/GATE/TR/TEST/RELEASE */
    private String evidenceType;
    /** 补证截止 */
    private LocalDateTime dueAt;
    private String reason;
    /** 补证责任人 */
    private Long ownerUserId;
    /** 授权人 */
    private Long grantedByUserId;
    /** ACTIVE/EXPIRED/RESOLVED */
    private String status;
    private Integer lockVersion;

}
