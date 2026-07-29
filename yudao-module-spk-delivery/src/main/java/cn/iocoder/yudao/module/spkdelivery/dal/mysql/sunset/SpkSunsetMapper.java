package cn.iocoder.yudao.module.spkdelivery.dal.mysql.sunset;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.sunset.SpkSunsetDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * SPK-OS R8 退市 Mapper
 *
 * @author SPK-OS
 */
@Mapper
public interface SpkSunsetMapper extends BaseMapperX<SpkSunsetDO> {

    default SpkSunsetDO selectByInstanceId(String instanceId) {
        return selectOne(new LambdaQueryWrapperX<SpkSunsetDO>()
                .eq(SpkSunsetDO::getInstanceId, instanceId));
    }

}
