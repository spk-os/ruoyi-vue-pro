package cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * IPD 幂等命令审计 DO。设计文档 §9.6。
 * 同租户同 idempotency_key + 同 payload 返回原结果；同 key 不同 payload 报冲突。
 * status: PENDING/RUNNING/SUCCESS/FAILED/CONFLICT。
 *
 * @author SPK-OS
 */
@TableName("spk_ipd_command_log")
@KeySequence("spk_ipd_command_log_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpkIpdCommandLogDO extends BaseDO {

    @TableId
    private Long id;
    private String idempotencyKey;
    private String commandType;
    private String targetType;
    private String targetId;
    private String payloadHash;
    private String payloadJson;
    private String status;
    private String resultJson;
    private String errorCode;
    private String errorMsg;
    private Long requestedBy;
    private LocalDateTime requestedAt;
    private LocalDateTime finishedAt;
}
