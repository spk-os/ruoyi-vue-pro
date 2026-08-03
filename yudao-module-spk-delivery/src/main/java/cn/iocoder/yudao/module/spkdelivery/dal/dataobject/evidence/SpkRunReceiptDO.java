package cn.iocoder.yudao.module.spkdelivery.dal.dataobject.evidence;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Run Receipt DO —— Agent 运行收据（Evidence 三件套之一，Lead 写）
 * <p>
 * 记录一次 Lead Agent 执行的模型/provider/token/cost/latency/产物 URI。依 §4.4。
 *
 * @author SPK-OS
 */
@TableName("spk_run_receipt")
@KeySequence("spk_run_receipt_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpkRunReceiptDO extends BaseDO {

    @TableId
    private Long id;
    private String runId;
    private String activityRunId;
    private String contractId;
    private Long leadAgentId;
    private String leadAgentCode;
    private String modelSnapshotId;
    private String capabilityId;
    private String provider;
    private String model;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    /**
     * token 用量 JSON {prompt,completion,total}
     */
    private String tokenUsage;
    private BigDecimal cost;
    private Integer latencyMs;
    /**
     * 状态 running/done/failed/timeout
     */
    private String status;
    private String failureReason;
    /**
     * 产物 URI 列表 JSON
     */
    private String artifactUris;
    /**
     * Worker 链接 JSON
     */
    private String workerLinks;
    /**
     * yudao AI 会话 id
     */
    private Long conversationId;

}
