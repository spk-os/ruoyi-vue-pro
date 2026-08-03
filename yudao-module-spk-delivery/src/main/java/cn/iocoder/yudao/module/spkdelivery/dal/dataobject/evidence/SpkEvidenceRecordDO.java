package cn.iocoder.yudao.module.spkdelivery.dal.dataobject.evidence;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Evidence Record DO —— 证据中心（追加式，哈希链）
 * <p>
 * 每条 prev_hash 指向前一条 row_hash，可前向校验。任何组件只能 INSERT，不能 UPDATE/DELETE。
 * 依 §4.4 / §5.6。
 *
 * @author SPK-OS
 */
@TableName("spk_evidence_record")
@KeySequence("spk_evidence_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpkEvidenceRecordDO extends BaseDO {

    @TableId
    private Long id;
    private String evidenceId;
    private String activityRunId;
    private String processInstanceId;
    /**
     * run/tool/verification/human_approval/decision/gate
     */
    private String evidenceType;
    /**
     * 关联 receipt / approval id
     */
    private String refId;
    /**
     * 载荷 JSON
     */
    private String payload;
    /**
     * 哈希链：前一条 row_hash
     */
    private String prevHash;
    /**
     * 本行哈希（基于 payload + prev_hash 计算）
     */
    private String rowHash;
    private LocalDateTime occurredAt;

}
