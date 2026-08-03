package cn.iocoder.yudao.module.spkdelivery.dal.mysql.modelcapability;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.modelcapability.SpkModelRegistrySnapshotDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * Model Registry Snapshot Mapper
 *
 * @author SPK-OS
 */
@Mapper
public interface SpkModelRegistrySnapshotMapper extends BaseMapperX<SpkModelRegistrySnapshotDO> {

    default SpkModelRegistrySnapshotDO selectBySnapshotId(String snapshotId) {
        return selectOne(new LambdaQueryWrapperX<SpkModelRegistrySnapshotDO>()
                .eq(SpkModelRegistrySnapshotDO::getSnapshotId, snapshotId));
    }

}
