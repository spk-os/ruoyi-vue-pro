package cn.iocoder.yudao.module.spkdelivery.dal.mysql.aegis;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.aegis.SpkAegisReviewDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * SPK-OS Aegis 审查 Mapper
 *
 * @author SPK-OS
 */
@Mapper
public interface SpkAegisReviewMapper extends BaseMapperX<SpkAegisReviewDO> {

    default SpkAegisReviewDO selectByInstanceId(String instanceId) {
        return selectOne(new LambdaQueryWrapperX<SpkAegisReviewDO>()
                .eq(SpkAegisReviewDO::getInstanceId, instanceId)
                .orderByDesc(SpkAegisReviewDO::getId)
                .last("LIMIT 1"));
    }

}
