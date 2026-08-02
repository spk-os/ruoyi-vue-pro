package cn.iocoder.yudao.module.spkdelivery.dal.dataobject.agentsquad;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * SPK-OS 智能体编队成员 DO
 *
 * <p>编队与智能体多对多关系表（带顺序与编队内角色）。
 *
 * @author SPK-OS
 */
@TableName("spk_agent_squad_member")
@KeySequence("spk_agent_squad_member_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpkAgentSquadMemberDO extends BaseDO {

    @TableId
    private Long id;
    /**
     * 编队 id
     */
    private Long squadId;
    /**
     * 智能体定义 id（FK spk_agent_def）
     */
    private Long agentId;
    /**
     * 在编队中的角色（可不同于智能体自身 role）
     */
    private String role;
    /**
     * 顺序（升序，wake 按此顺序串行）
     */
    private Integer sortOrder;

}
