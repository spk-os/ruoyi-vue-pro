package cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.governance;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * IPD Profile 版本 snapshotJson 结构契约（D2）。供前端版本编辑器按字段构造快照。
 * <p>
 * snapshotJson 是强类型治理快照（非 BPM simpleModel），结构：stages[] + trimRules[] + mode。
 *
 * @author SPK-OS
 */
@Schema(description = "管理后台 - Profile 版本 snapshot 结构契约")
@Data
public class SpkIpdSnapshotSchemaRespVO {

    @Schema(description = "版本模式 FULL_RELEASE/INCREMENT_RELEASE/ISSUE_RESOLUTION")
    private String mode;
    @Schema(description = "阶段定义列表")
    private List<StageDef> stages;
    @Schema(description = "裁剪规则占位（action 取值 SKIP/OPTIONAL/SIMPLIFY）")
    private List<TrimRuleDef> trimRules;

    @Data
    public static class StageDef {
        @Schema(description = "阶段标识 concept/plan/develop/qualify/launch/lifecycle/support")
        private String stage;
        @Schema(description = "门禁标识列表 g1..g8")
        private List<String> gates;
        @Schema(description = "DCP 评审点 CDCP/PDCP/ADCP/LDCP")
        private List<String> dcps;
        @Schema(description = "TR 技术评审点 tr2..tr6")
        private List<String> trs;
        @Schema(description = "活动定义 id 列表")
        private List<String> activities;
    }

    @Data
    public static class TrimRuleDef {
        @Schema(description = "阶段（可空，空表示活动级规则）")
        private String stage;
        @Schema(description = "活动/BPM 节点 id（可空，空表示整阶段裁剪）")
        private String activityDefId;
        @Schema(description = "裁剪条件表达式")
        private String trimCondition;
        @Schema(description = "动作 SKIP/OPTIONAL/SIMPLIFY")
        private String action;
        @Schema(description = "裁剪原因")
        private String reason;
    }
}
