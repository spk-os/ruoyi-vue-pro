package cn.iocoder.yudao.module.spkdelivery.dal.mysql.agent;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.agent.SpkAgentTaskDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * SPK-OS Agent 任务 Mapper
 *
 * @author SPK-OS
 */
@Mapper
public interface SpkAgentTaskMapper extends BaseMapperX<SpkAgentTaskDO> {

    default SpkAgentTaskDO selectByInstanceIdAndNodeKey(String instanceId, String nodeKey) {
        return selectOne(new LambdaQueryWrapperX<SpkAgentTaskDO>()
                .eq(SpkAgentTaskDO::getInstanceId, instanceId)
                .eq(SpkAgentTaskDO::getNodeKey, nodeKey));
    }

    default SpkAgentTaskDO selectByTaskId(String taskId) {
        return selectOne(new LambdaQueryWrapperX<SpkAgentTaskDO>()
                .eq(SpkAgentTaskDO::getTaskId, taskId));
    }

    default List<SpkAgentTaskDO> selectListByInstanceId(String instanceId) {
        return selectList(new LambdaQueryWrapperX<SpkAgentTaskDO>()
                .eq(SpkAgentTaskDO::getInstanceId, instanceId));
    }

}
