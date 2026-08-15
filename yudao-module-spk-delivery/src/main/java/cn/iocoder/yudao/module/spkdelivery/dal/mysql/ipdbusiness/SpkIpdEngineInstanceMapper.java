package cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdEngineInstanceDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * IPD 引擎实例档案 Mapper
 *
 * @author SPK-OS
 */
@Mapper
public interface SpkIpdEngineInstanceMapper extends BaseMapperX<SpkIpdEngineInstanceDO> {

    default SpkIpdEngineInstanceDO selectByProcessInstanceId(String processInstanceId) {
        return selectOne(SpkIpdEngineInstanceDO::getProcessInstanceId, processInstanceId);
    }

    default SpkIpdEngineInstanceDO selectByFlowRunId(Long flowRunId) {
        return selectOne(SpkIpdEngineInstanceDO::getFlowRunId, flowRunId);
    }
}
