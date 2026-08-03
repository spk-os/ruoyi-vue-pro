package cn.iocoder.yudao.module.spkdelivery.dal.dataobject.modelcapability;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Model Capability Profile DO —— 模型能力注册表（GAP-6）
 * <p>
 * capability_id → provider/model 映射，按 priority 选最优。依 §4.5。
 *
 * @author SPK-OS
 */
@TableName("spk_model_capability_profile")
@KeySequence("spk_model_capability_profile_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpkModelCapabilityProfileDO extends BaseDO {

    @TableId
    private Long id;
    /**
     * 能力标识，如 FINANCIAL_REASONING / INDEPENDENT_VERIFICATION
     */
    private String capabilityId;
    private String provider;
    private String model;
    private String deploymentId;
    private Integer contextLength;
    private String priceCardUri;
    private String priceCardHash;
    /**
     * 评测档案 JSON
     */
    private String evalProfile;
    /**
     * domestic/international/any
     */
    private String dataPolicy;
    private String region;
    /**
     * 同能力内优先级，小者优先
     */
    private Integer priority;
    /**
     * active/disabled
     */
    private String status;
    private LocalDateTime validAt;
    private LocalDateTime expiresAt;

}
