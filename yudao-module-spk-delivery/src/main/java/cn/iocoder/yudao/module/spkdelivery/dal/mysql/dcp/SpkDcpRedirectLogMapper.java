package cn.iocoder.yudao.module.spkdelivery.dal.mysql.dcp;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.dcp.SpkDcpRedirectLogDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * SPK-OS DCP 回退日志 Mapper
 *
 * @author SPK-OS
 */
@Mapper
public interface SpkDcpRedirectLogMapper extends BaseMapperX<SpkDcpRedirectLogDO> {

    default SpkDcpRedirectLogDO selectByInstanceIdAndDcp(String instanceId, String dcp) {
        return selectOne(new LambdaQueryWrapperX<SpkDcpRedirectLogDO>()
                .eq(SpkDcpRedirectLogDO::getInstanceId, instanceId)
                .eq(SpkDcpRedirectLogDO::getDcp, dcp)
                .orderByDesc(SpkDcpRedirectLogDO::getId)
                .last("LIMIT 1"));
    }

    default List<SpkDcpRedirectLogDO> selectListByInstanceId(String instanceId) {
        return selectList(new LambdaQueryWrapperX<SpkDcpRedirectLogDO>()
                .eq(SpkDcpRedirectLogDO::getInstanceId, instanceId)
                .orderByDesc(SpkDcpRedirectLogDO::getId));
    }

}
