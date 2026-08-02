package cn.iocoder.yudao.module.spkdelivery.dal.dataobject.agentsquad;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * SPK-OS 智能体编队 DO
 *
 * <p>与 Paddock 的差别：Paddock 的 "Agent Squad" 仅是 agents 表的卡片列表视图，
 * 本系统将其显式建模为独立编队实体（头表 + 成员表 {@link SpkAgentSquadMemberDO}），
 * 支持把多个本地智能体编为一个小组、按顺序唤醒。
 *
 * @author SPK-OS
 */
@TableName("spk_agent_squad")
@KeySequence("spk_agent_squad_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpkAgentSquadDO extends BaseDO {

    @TableId
    private Long id;
    /**
     * 编队名称（唯一）
     */
    private String name;
    /**
     * 编队编码（唯一，程序引用）
     */
    private String code;
    /**
     * 描述
     */
    private String description;
    /**
     * 状态：active / disabled
     */
    private String status;
    /**
     * 杂项配置（JSON）
     */
    private String config;

}
