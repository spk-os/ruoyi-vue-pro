package cn.iocoder.yudao.module.spkdelivery.dal.dataobject.flowconfig;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * Skill 环境配置 DO（单行配置表，id 恒为 1）。
 * <p>
 * 存 skill 根目录与默认 env，前端「节点 skill 与环境」配置页可改，派发层
 * {@code SpkTaskRouterService.resolveSkillEnv/Name/Path} 运行时读取（对齐 D2）。
 * 取代派发层原 {@code @Value} 硬注入——路径/env 改动前端即时生效，无需重启。
 * <p>
 * 与 {@code spk-delivery.skill.root} / {@code spk-delivery.skill.default-env}
 * 两个 application.yaml 配置项的关系：yaml 作 bootstrap 兜底（DB 无行/字段空时回退 yaml 值），
 * DB 有值则 DB 优先。这保证既有 yaml 部署零破坏。
 *
 * @author SPK-OS
 */
@TableName("spk_skill_config")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpkSkillConfigDO extends BaseDO {

    /**
     * 主键，恒为 1（单行配置表）
     */
    @TableId
    private Long id;

    /**
     * skill 文件系统根目录，env/skillName/SKILL.md 三段式解析的根。
     * 默认 /work/SPK-OS/soft/basic/ruoyi/resources/skills
     */
    private String skillsRoot;

    /**
     * 默认 skill 环境 default/test/commercial-release/prototype-release。
     * def.envRequirements.skillEnv 与流程变量 spk_skill_env 均未设时回退到此。
     */
    private String defaultEnv;
}
