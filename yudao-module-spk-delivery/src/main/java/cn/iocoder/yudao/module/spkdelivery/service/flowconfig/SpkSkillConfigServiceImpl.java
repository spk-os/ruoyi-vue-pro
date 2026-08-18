package cn.iocoder.yudao.module.spkdelivery.service.flowconfig;

import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.flowconfig.SpkSkillConfigDO;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.flowconfig.SpkSkillConfigMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Skill 环境配置 Service 实现。
 * <p>
 * DB 优先 + yaml 兜底：getConfig() 读 spk_skill_config 单行，行/字段空时回退
 * {@code spk-delivery.skill.root} / {@code spk-delivery.skill.default-env} 两个
 * application.yaml 配置项（与原 SpkTaskRouterService @Value 默认值一致，零破坏既有部署）。
 * <p>
 * 不引入 @Cacheable：单行主键查询极轻；route 非高频热路径；updateConfig 写后下次 getConfig 天然读到新值，
 * 避免缓存失效时序坑（配置改动需即时生效是硬需求）。
 *
 * @author SPK-OS
 */
@Slf4j
@Service
public class SpkSkillConfigServiceImpl implements SpkSkillConfigService {

    /** skill 根目录 yaml 兜底（与派发层原 @Value 默认值一致） */
    @Value("${spk-delivery.skill.root:/work/SPK-OS/soft/basic/ruoyi/resources/skills}")
    private String yamlSkillsRoot;
    /** 默认 skill 环境 yaml 兜底（与派发层原 @Value 默认值一致） */
    @Value("${spk-delivery.skill.default-env:default}")
    private String yamlDefaultEnv;

    @Resource
    private SpkSkillConfigMapper skillConfigMapper;

    @Override
    public SpkSkillConfigDO getConfig() {
        SpkSkillConfigDO row = skillConfigMapper.selectSingleton();
        String root = (row != null && row.getSkillsRoot() != null && !row.getSkillsRoot().isBlank())
                ? row.getSkillsRoot() : yamlSkillsRoot;
        String env = (row != null && row.getDefaultEnv() != null && !row.getDefaultEnv().isBlank())
                ? row.getDefaultEnv() : yamlDefaultEnv;
        // 返回兜底后的快照（字段永非 null），调用方可安全直用
        return SpkSkillConfigDO.builder().id(1L).skillsRoot(root).defaultEnv(env).build();
    }

    @Override
    public void updateConfig(String skillsRoot, String defaultEnv) {
        SpkSkillConfigDO row = skillConfigMapper.selectSingleton();
        String newRoot = (skillsRoot != null && !skillsRoot.isBlank()) ? skillsRoot.trim()
                : (row != null ? row.getSkillsRoot() : yamlSkillsRoot);
        String newEnv = (defaultEnv != null && !defaultEnv.isBlank()) ? defaultEnv.trim()
                : (row != null ? row.getDefaultEnv() : yamlDefaultEnv);
        SpkSkillConfigDO upsert = SpkSkillConfigDO.builder()
                .id(1L).skillsRoot(newRoot).defaultEnv(newEnv).build();
        if (row == null) {
            skillConfigMapper.insert(upsert);
        } else {
            skillConfigMapper.updateById(upsert);
        }
        log.info("[updateConfig][skillsRoot={} defaultEnv={} 已更新]", newRoot, newEnv);
    }
}
