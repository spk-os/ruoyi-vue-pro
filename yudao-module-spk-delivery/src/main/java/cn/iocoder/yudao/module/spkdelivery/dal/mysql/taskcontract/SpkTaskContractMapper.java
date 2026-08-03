package cn.iocoder.yudao.module.spkdelivery.dal.mysql.taskcontract;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.taskcontract.SpkTaskContractDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * Task Contract Mapper
 *
 * @author SPK-OS
 */
@Mapper
public interface SpkTaskContractMapper extends BaseMapperX<SpkTaskContractDO> {

    default SpkTaskContractDO selectByContractId(String contractId) {
        return selectOne(new LambdaQueryWrapperX<SpkTaskContractDO>()
                .eq(SpkTaskContractDO::getContractId, contractId));
    }

    default SpkTaskContractDO selectByActivityRunId(String activityRunId) {
        return selectOne(new LambdaQueryWrapperX<SpkTaskContractDO>()
                .eq(SpkTaskContractDO::getActivityRunId, activityRunId));
    }

    default List<SpkTaskContractDO> selectListByProcessInstanceId(String processInstanceId) {
        return selectList(new LambdaQueryWrapperX<SpkTaskContractDO>()
                .eq(SpkTaskContractDO::getProcessInstanceId, processInstanceId)
                .orderByAsc(SpkTaskContractDO::getQueuedAt));
    }

    default List<SpkTaskContractDO> selectListByStatus(String status) {
        return selectList(new LambdaQueryWrapperX<SpkTaskContractDO>()
                .eq(SpkTaskContractDO::getStatus, status));
    }

    /**
     * 统计某 Lead Agent 当前 running 合同数（负载评估）
     */
    default Long countRunningByLeadAgentId(Long leadAgentId) {
        return selectCount(new LambdaQueryWrapperX<SpkTaskContractDO>()
                .eq(SpkTaskContractDO::getLeadAgentId, leadAgentId)
                .eq(SpkTaskContractDO::getStatus, "running"));
    }

}
