package cn.iocoder.yudao.module.spkdelivery.controller.admin.agent.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 智能体运行负载聚合 VO（§7.1 第 3 视图）
 *
 * <p>按 agent_def 维度聚合：运行中 / 成功 / 失败 / 接管 / 成本 token / 最近错误。
 * 数据稀疏时真实返回空列表，前端标注"样本不足"，绝不造假。
 *
 * @author SPK-OS
 */
@Schema(description = "智能体运行负载聚合 VO")
@Data
public class SpkAgentLoadStatsVO {

    @Schema(description = "智能体定义 ID")
    private Long agentDefId;

    @Schema(description = "智能体名")
    private String agentName;

    @Schema(description = "智能体编码")
    private String agentCode;

    @Schema(description = "运行中任务数")
    private Integer running;

    @Schema(description = "成功任务数")
    private Integer succeeded;

    @Schema(description = "失败任务数")
    private Integer failed;

    @Schema(description = "人工介入次数")
    private Integer intervened;

    @Schema(description = "累计消耗 token 数")
    private Long totalTokens;

    @Schema(description = "任务总数")
    private Integer total;

    @Schema(description = "成功率（0-1，样本不足为 null")
    private Double successRate;

    @Schema(description = "最近失败原因")
    private String lastError;

    @Schema(description = "最近活动时间")
    private LocalDateTime lastActivity;

}
