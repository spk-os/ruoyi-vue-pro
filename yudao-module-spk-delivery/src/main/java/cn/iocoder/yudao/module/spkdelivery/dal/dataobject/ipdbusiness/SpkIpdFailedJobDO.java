package cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * IPD 失败作业 DO。设计文档 §9.5.5。
 * <p>
 * 触发器/回调/同步等失败作业的登记与重试。
 * status: PENDING/RETRYING/RESOLVED/DEAD。
 *
 * @author SPK-OS
 */
@TableName("spk_ipd_failed_job")
@KeySequence("spk_ipd_failed_job_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpkIpdFailedJobDO extends BaseDO {

    @TableId
    private Long id;
    private String jobType;
    private Long refId;
    private String reason;
    private Integer retryCount;
    private String status;
    private LocalDateTime nextRetryAt;
    private Integer lockVersion;
}
