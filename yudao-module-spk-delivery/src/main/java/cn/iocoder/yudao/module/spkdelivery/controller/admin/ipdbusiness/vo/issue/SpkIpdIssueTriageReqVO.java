package cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.issue;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * IPD 问题分诊 Request VO。设计文档 §10.6 /triage。
 *
 * @author SPK-OS
 */
@Schema(description = "管理后台 - IPD 问题分诊 Request VO")
@Data
public class SpkIpdIssueTriageReqVO {

    @Schema(description = "严重度 P0/P1/P2/P3")
    private String severity;

    @Schema(description = "受影响版本 ID 列表")
    private List<Long> affectedVersionIds;

    @Schema(description = "时限（小时）")
    private Integer slaHours;

    @Schema(description = "是否启动问题流")
    private Boolean startResolutionFlow;

    @Schema(description = "责任人")
    private Long ownerUserId;
}
