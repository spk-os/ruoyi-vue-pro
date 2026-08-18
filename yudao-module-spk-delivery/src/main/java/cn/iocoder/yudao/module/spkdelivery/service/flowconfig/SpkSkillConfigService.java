package cn.iocoder.yudao.module.spkdelivery.service.flowconfig;

import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.flowconfig.SpkSkillConfigDO;

/**
 * Skill 环境配置 Service。
 * <p>
 * 单行配置表 {@code spk_skill_config} 的读写门面：派发层
 * {@code SpkTaskRouterService} 与配置页 {@code SpkFlowConfigService} 共用此门面，
 * 保证 skillsRoot/defaultEnv 单一数据源（D2）。
 * <p>
 * 读端 {@link #getConfig()} 返回值字段永非 null（DB 无行/字段空时回退 application.yaml
 * {@code spk-delivery.skill.root} / {@code spk-delivery.skill.default-env} 兜底），
 * 故调用方无需再处理空值。单行主键查询极轻，route 非热路径，不引入额外缓存层——
 * {@link #updateConfig} 写后天然即时生效（每次 route 实时读 DB）。
 *
 * @author SPK-OS
 */
public interface SpkSkillConfigService {

    /**
     * 读 skill 环境配置（含兜底，字段永非 null）。
     */
    SpkSkillConfigDO getConfig();

    /**
     * 改 skillsRoot / defaultEnv（单行 upsert，id 恒为 1）。
     * 任一参数为 null/空 表示不改（保留原值）。
     */
    void updateConfig(String skillsRoot, String defaultEnv);
}
