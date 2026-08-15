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

    // ==================== 编排治理字段（§7.1 / §9.5） ====================
    /**
     * 编排模式：SERIAL 串行 / PARALLEL 并行 / CONCURRENT 并发（wake 按策略分发）
     */
    private String orchestrationMode;
    /**
     * 并发上限（PARALLEL/CONCURRENT 生效，0 表示不限）
     */
    private Integer concurrency;
    /**
     * 单成员执行超时秒（0 表示不限）
     */
    private Integer timeout;
    /**
     * 降级策略：SKIP 跳过 / FALLBACK 回退主智能体 / BLOCK 阻塞等人工
     */
    private String degradation;
    /**
     * 编队模板版本号
     */
    private Integer version;
    /**
     * 发布状态：DRAFT / PUBLISHED / SUPERSEDED
     */
    private String publishState;
    /**
     * 锁版本（乐观锁，治理发布/回滚用）
     */
    private Integer lockVersion;

}
