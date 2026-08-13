package cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * IPD 问题 IssueCase DO。设计文档 §9.3.4。
 * issue_type: DEFECT/INCIDENT/CUSTOMER_ISSUE/TECH_DEBT/SECURITY/COMPLIANCE/CHANGE_REQUEST。
 * severity: P0/P1/P2/P3。status: OPEN/TRIAGED/IN_PROGRESS/RESOLVED/CLOSED/REOPENED（详见 §11.5）。
 *
 * @author SPK-OS
 */
@TableName("spk_ipd_issue_case")
@KeySequence("spk_ipd_issue_case_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpkIpdIssueCaseDO extends BaseDO {

    @TableId
    private Long id;
    private String caseNo;
    private Long projectId;
    private String issueType;
    private String severity;
    private String title;
    private String description;
    /** MANUAL/PLANE/GITEA/MONITOR/AGENT */
    private String source;
    private String externalSystem;
    private String externalId;
    private String externalUrl;
    private Long ownerUserId;
    private String status;
    private String rootCause;
    private String resolution;
    private LocalDateTime detectedAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime closedAt;
    private Integer lockVersion;
}
