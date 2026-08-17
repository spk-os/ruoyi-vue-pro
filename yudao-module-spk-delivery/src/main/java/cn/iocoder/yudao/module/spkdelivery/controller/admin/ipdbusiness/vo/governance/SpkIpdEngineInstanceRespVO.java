package cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.governance;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * IPD 引擎实例档案 RespVO（D4）。供运行统计页展示 processInstanceId↔flowRunId↔profileVersionId 绑定。
 *
 * @author SPK-OS
 */
@Schema(description = "管理后台 - IPD 引擎实例档案")
@Data
public class SpkIpdEngineInstanceRespVO {

    @Schema(description = "主键")
    private Long id;
    @Schema(description = "Flowable 流程实例 id")
    private String processInstanceId;
    @Schema(description = "关联 FlowRun id")
    private Long flowRunId;
    @Schema(description = "关联 Profile 版本 id")
    private Long profileVersionId;
    @Schema(description = "引擎健康度 HEALTHY/DEGRADED/STOPPED")
    private String engineHealth;
    @Schema(description = "最近同步时间")
    private LocalDateTime lastSyncedAt;
    @Schema(description = "元信息 JSON（flowKey/flowType/profileId 等）")
    private String metaJson;
}
