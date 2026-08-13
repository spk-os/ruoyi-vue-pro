package cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.assignment.SpkIpdAssignmentPageReqVO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdAssignmentDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * IPD 任务分派 Mapper
 *
 * @author SPK-OS
 */
@Mapper
public interface SpkIpdAssignmentMapper extends BaseMapperX<SpkIpdAssignmentDO> {

    default List<SpkIpdAssignmentDO> selectListByFlowRun(Long flowRunId) {
        return selectList(SpkIpdAssignmentDO::getFlowRunId, flowRunId);
    }

    default List<SpkIpdAssignmentDO> selectByActor(String actorType, Long actorId) {
        return selectList(new LambdaQueryWrapperX<SpkIpdAssignmentDO>()
                .eq(SpkIpdAssignmentDO::getActorType, actorType)
                .eq(SpkIpdAssignmentDO::getActorId, actorId));
    }

    default PageResult<SpkIpdAssignmentDO> selectPage(SpkIpdAssignmentPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SpkIpdAssignmentDO>()
                .eqIfPresent(SpkIpdAssignmentDO::getProjectId, reqVO.getProjectId())
                .eqIfPresent(SpkIpdAssignmentDO::getVersionId, reqVO.getVersionId())
                .eqIfPresent(SpkIpdAssignmentDO::getFlowRunId, reqVO.getFlowRunId())
                .eqIfPresent(SpkIpdAssignmentDO::getWorkItemType, reqVO.getWorkItemType())
                .eqIfPresent(SpkIpdAssignmentDO::getActorType, reqVO.getActorType())
                .eqIfPresent(SpkIpdAssignmentDO::getActorId, reqVO.getActorId())
                .eqIfPresent(SpkIpdAssignmentDO::getStatus, reqVO.getStatus())
                .orderByDesc(SpkIpdAssignmentDO::getUpdateTime));
    }
}
