package cn.iocoder.yudao.module.spkdelivery.dal.mysql.contract;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.contract.SpkContractVersionDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * SPK-OS Workflow Contract 版本 Mapper
 *
 * @author SPK-OS
 */
@Mapper
public interface SpkContractVersionMapper extends BaseMapperX<SpkContractVersionDO> {

    default SpkContractVersionDO selectLatestByModelKey(String modelKey) {
        return selectOne(new LambdaQueryWrapperX<SpkContractVersionDO>()
                .eq(SpkContractVersionDO::getModelKey, modelKey)
                .orderByDesc(SpkContractVersionDO::getVersion)
                .last("LIMIT 1"));
    }

}
