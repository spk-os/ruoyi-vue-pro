package cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.flowrun.SpkIpdFlowRunPageReqVO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdFlowRunDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * IPD FlowRun Mapper
 *
 * @author SPK-OS
 */
@Mapper
public interface SpkIpdFlowRunMapper extends BaseMapperX<SpkIpdFlowRunDO> {

    default SpkIpdFlowRunDO selectByProcessInstanceId(String processInstanceId) {
        return selectOne(SpkIpdFlowRunDO::getProcessInstanceId, processInstanceId);
    }

    default SpkIpdFlowRunDO selectByBusinessKey(String businessKey) {
        return selectOne(SpkIpdFlowRunDO::getBusinessKey, businessKey);
    }

    /** 同一版本默认只能有一条活跃主交付流（DRAFT/READY/STARTING/RUNNING） */
    default List<SpkIpdFlowRunDO> selectActiveByVersion(Long versionId) {
        return selectList(new LambdaQueryWrapperX<SpkIpdFlowRunDO>()
                .eq(SpkIpdFlowRunDO::getVersionId, versionId)
                .in(SpkIpdFlowRunDO::getStatus, "DRAFT", "READY", "STARTING", "RUNNING", "BLOCKED"));
    }

    default PageResult<SpkIpdFlowRunDO> selectPage(SpkIpdFlowRunPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SpkIpdFlowRunDO>()
                .eqIfPresent(SpkIpdFlowRunDO::getProjectId, reqVO.getProjectId())
                .eqIfPresent(SpkIpdFlowRunDO::getVersionId, reqVO.getVersionId())
                .eqIfPresent(SpkIpdFlowRunDO::getIssueCaseId, reqVO.getIssueCaseId())
                .eqIfPresent(SpkIpdFlowRunDO::getFlowType, reqVO.getFlowType())
                .eqIfPresent(SpkIpdFlowRunDO::getStatus, reqVO.getStatus())
                .eqIfPresent(SpkIpdFlowRunDO::getCurrentStage, reqVO.getCurrentStage())
                .orderByDesc(SpkIpdFlowRunDO::getUpdateTime));
    }
}
