package cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdMajorReleaseDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * IPD 大版本 Mapper
 *
 * @author SPK-OS
 */
@Mapper
public interface SpkIpdMajorReleaseMapper extends BaseMapperX<SpkIpdMajorReleaseDO> {

    default SpkIpdMajorReleaseDO selectByProjectAndMajor(Long projectId, Integer majorNo) {
        return selectOne(SpkIpdMajorReleaseDO::getProjectId, projectId,
                SpkIpdMajorReleaseDO::getMajorNo, majorNo);
    }

    default List<SpkIpdMajorReleaseDO> selectListByProject(Long projectId) {
        return selectList(SpkIpdMajorReleaseDO::getProjectId, projectId);
    }
}
