package cn.iocoder.yudao.module.spkdelivery.dal.mysql.flowconfig;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.flowconfig.SpkSkillConfigDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * Skill 环境配置 Mapper（单行配置表）
 *
 * @author SPK-OS
 */
@Mapper
public interface SpkSkillConfigMapper extends BaseMapperX<SpkSkillConfigDO> {

    /**
     * 取单行配置（id 恒为 1）。无行返回 null（调用方回退 application.yaml 兜底）。
     */
    default SpkSkillConfigDO selectSingleton() {
        return selectById(1L);
    }
}
