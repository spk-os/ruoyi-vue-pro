package cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdProcessProfileDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * IPD 流程模板 Profile Mapper
 *
 * @author SPK-OS
 */
@Mapper
public interface SpkIpdProcessProfileMapper extends BaseMapperX<SpkIpdProcessProfileDO> {

    default SpkIpdProcessProfileDO selectByProfileCode(String profileCode) {
        return selectOne(SpkIpdProcessProfileDO::getProfileCode, profileCode);
    }

    default SpkIpdProcessProfileDO selectByFlowType(String flowType) {
        return selectOne(new LambdaQueryWrapperX<SpkIpdProcessProfileDO>()
                .eq(SpkIpdProcessProfileDO::getFlowType, flowType)
                .eq(SpkIpdProcessProfileDO::getStatus, "PUBLISHED"));
    }
}
