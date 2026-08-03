package cn.iocoder.yudao.module.spkdelivery.dal.mysql.modelcapability;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.modelcapability.SpkModelCapabilityProfileDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * Model Capability Profile Mapper
 *
 * @author SPK-OS
 */
@Mapper
public interface SpkModelCapabilityProfileMapper extends BaseMapperX<SpkModelCapabilityProfileDO> {

    default List<SpkModelCapabilityProfileDO> selectListByCapabilityIdAndStatus(String capabilityId, String status) {
        return selectList(new LambdaQueryWrapperX<SpkModelCapabilityProfileDO>()
                .eq(SpkModelCapabilityProfileDO::getCapabilityId, capabilityId)
                .eq(SpkModelCapabilityProfileDO::getStatus, status)
                .orderByAsc(SpkModelCapabilityProfileDO::getPriority));
    }

    default List<SpkModelCapabilityProfileDO> selectListByStatus(String status) {
        return selectList(new LambdaQueryWrapperX<SpkModelCapabilityProfileDO>()
                .eq(SpkModelCapabilityProfileDO::getStatus, status)
                .orderByAsc(SpkModelCapabilityProfileDO::getCapabilityId)
                .orderByAsc(SpkModelCapabilityProfileDO::getPriority));
    }

}
