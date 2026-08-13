package cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdVersionDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * IPD 交付版本 Mapper
 *
 * @author SPK-OS
 */
@Mapper
public interface SpkIpdVersionMapper extends BaseMapperX<SpkIpdVersionDO> {

    default SpkIpdVersionDO selectByMajorAndMinor(Long projectId, Integer majorNo, Integer minorNo) {
        return selectOne(SpkIpdVersionDO::getProjectId, projectId,
                SpkIpdVersionDO::getMajorNo, majorNo,
                SpkIpdVersionDO::getMinorNo, minorNo);
    }

    default List<SpkIpdVersionDO> selectListByMajorRelease(Long majorReleaseId) {
        return selectList(SpkIpdVersionDO::getMajorReleaseId, majorReleaseId);
    }

    default SpkIpdVersionDO selectBaselineByMajorRelease(Long majorReleaseId) {
        return selectOne(SpkIpdVersionDO::getMajorReleaseId, majorReleaseId,
                SpkIpdVersionDO::getBaselineFlag, 1);
    }
}
