package cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdLegacyMappingDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * IPD 旧实例映射 Mapper
 *
 * @author SPK-OS
 */
@Mapper
public interface SpkIpdLegacyMappingMapper extends BaseMapperX<SpkIpdLegacyMappingDO> {

    default SpkIpdLegacyMappingDO selectByLegacy(String legacyType, String legacyId) {
        return selectOne(SpkIpdLegacyMappingDO::getLegacyType, legacyType,
                SpkIpdLegacyMappingDO::getLegacyId, legacyId);
    }

    default List<SpkIpdLegacyMappingDO> selectListNeedsMapping() {
        return selectList(SpkIpdLegacyMappingDO::getMappingStatus, "NEEDS_MAPPING");
    }
}
