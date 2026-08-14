package cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdEvidenceWaiverDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * IPD 证据豁免 Mapper
 *
 * @author SPK-OS
 */
@Mapper
public interface SpkIpdEvidenceWaiverMapper extends BaseMapperX<SpkIpdEvidenceWaiverDO> {

    default List<SpkIpdEvidenceWaiverDO> selectListByProcessInstanceId(String processInstanceId) {
        return selectList(new LambdaQueryWrapperX<SpkIpdEvidenceWaiverDO>()
                .eq(SpkIpdEvidenceWaiverDO::getProcessInstanceId, processInstanceId)
                .orderByDesc(SpkIpdEvidenceWaiverDO::getCreateTime));
    }

    default List<SpkIpdEvidenceWaiverDO> selectListByTaskId(String taskId) {
        return selectList(new LambdaQueryWrapperX<SpkIpdEvidenceWaiverDO>()
                .eq(SpkIpdEvidenceWaiverDO::getTaskId, taskId)
                .orderByDesc(SpkIpdEvidenceWaiverDO::getCreateTime));
    }

    default List<SpkIpdEvidenceWaiverDO> selectListByFlowRunId(Long flowRunId) {
        return selectList(new LambdaQueryWrapperX<SpkIpdEvidenceWaiverDO>()
                .eq(SpkIpdEvidenceWaiverDO::getFlowRunId, flowRunId)
                .orderByDesc(SpkIpdEvidenceWaiverDO::getCreateTime));
    }
}
