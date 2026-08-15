package cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdProcessProfileVersionDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * IPD Profile 版本 Mapper
 *
 * @author SPK-OS
 */
@Mapper
public interface SpkIpdProcessProfileVersionMapper extends BaseMapperX<SpkIpdProcessProfileVersionDO> {

    default SpkIpdProcessProfileVersionDO selectByProfileIdAndVersion(Long profileId, Integer version) {
        return selectOne(new LambdaQueryWrapperX<SpkIpdProcessProfileVersionDO>()
                .eq(SpkIpdProcessProfileVersionDO::getProfileId, profileId)
                .eq(SpkIpdProcessProfileVersionDO::getVersion, version));
    }

    /** 取 Profile 当前已发布版本 */
    default SpkIpdProcessProfileVersionDO selectPublishedByProfileId(Long profileId) {
        return selectOne(new LambdaQueryWrapperX<SpkIpdProcessProfileVersionDO>()
                .eq(SpkIpdProcessProfileVersionDO::getProfileId, profileId)
                .eq(SpkIpdProcessProfileVersionDO::getStatus, "PUBLISHED"));
    }

    default List<SpkIpdProcessProfileVersionDO> selectListByProfileId(Long profileId) {
        return selectList(new LambdaQueryWrapperX<SpkIpdProcessProfileVersionDO>()
                .eq(SpkIpdProcessProfileVersionDO::getProfileId, profileId)
                .orderByDesc(SpkIpdProcessProfileVersionDO::getVersion));
    }
}
