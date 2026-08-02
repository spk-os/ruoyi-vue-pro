package cn.iocoder.yudao.module.spkdelivery.dal.mysql.ccb;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ccb.SpkCcbRecordDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * SPK-OS CCB 变更 Mapper
 *
 * @author SPK-OS
 */
@Mapper
public interface SpkCcbRecordMapper extends BaseMapperX<SpkCcbRecordDO> {

    default List<SpkCcbRecordDO> selectListByInstanceId(String instanceId) {
        return selectList(new LambdaQueryWrapperX<SpkCcbRecordDO>()
                .eq(SpkCcbRecordDO::getInstanceId, instanceId)
                .orderByDesc(SpkCcbRecordDO::getId));
    }

}
