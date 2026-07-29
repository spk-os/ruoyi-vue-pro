package cn.iocoder.yudao.module.spkdelivery.dal.mysql.gate;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.gate.SpkGateRecordDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * SPK-OS 门禁回调审计 Mapper
 *
 * @author SPK-OS
 */
@Mapper
public interface SpkGateRecordMapper extends BaseMapperX<SpkGateRecordDO> {

    default SpkGateRecordDO selectByInstanceIdAndGate(String instanceId, String gate) {
        return selectOne(new LambdaQueryWrapperX<SpkGateRecordDO>()
                .eq(SpkGateRecordDO::getInstanceId, instanceId)
                .eq(SpkGateRecordDO::getGate, gate)
                .orderByDesc(SpkGateRecordDO::getId)
                .last("LIMIT 1"));
    }

    default List<SpkGateRecordDO> selectListByInstanceId(String instanceId) {
        return selectList(new LambdaQueryWrapperX<SpkGateRecordDO>()
                .eq(SpkGateRecordDO::getInstanceId, instanceId));
    }

}
