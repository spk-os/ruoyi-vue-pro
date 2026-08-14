package cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdDecisionRecordDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * IPD 决策记录 Mapper
 *
 * @author SPK-OS
 */
@Mapper
public interface SpkIpdDecisionRecordMapper extends BaseMapperX<SpkIpdDecisionRecordDO> {

    default List<SpkIpdDecisionRecordDO> selectListByProcessInstanceId(String processInstanceId) {
        return selectList(new LambdaQueryWrapperX<SpkIpdDecisionRecordDO>()
                .eq(SpkIpdDecisionRecordDO::getProcessInstanceId, processInstanceId)
                .orderByDesc(SpkIpdDecisionRecordDO::getCreateTime));
    }

    default List<SpkIpdDecisionRecordDO> selectListByFlowRunId(Long flowRunId) {
        return selectList(new LambdaQueryWrapperX<SpkIpdDecisionRecordDO>()
                .eq(SpkIpdDecisionRecordDO::getFlowRunId, flowRunId)
                .orderByDesc(SpkIpdDecisionRecordDO::getCreateTime));
    }

    default List<SpkIpdDecisionRecordDO> selectListByTaskId(String taskId) {
        return selectList(new LambdaQueryWrapperX<SpkIpdDecisionRecordDO>()
                .eq(SpkIpdDecisionRecordDO::getTaskId, taskId)
                .orderByDesc(SpkIpdDecisionRecordDO::getCreateTime));
    }
}
