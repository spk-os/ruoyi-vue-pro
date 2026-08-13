package cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.actor.SpkIpdProjectActorPageReqVO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdProjectActorDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * IPD 项目参与者 Mapper
 *
 * @author SPK-OS
 */
@Mapper
public interface SpkIpdProjectActorMapper extends BaseMapperX<SpkIpdProjectActorDO> {

    default List<SpkIpdProjectActorDO> selectListByProject(Long projectId, Long versionId) {
        return selectList(new LambdaQueryWrapperX<SpkIpdProjectActorDO>()
                .eq(SpkIpdProjectActorDO::getProjectId, projectId)
                .eq(SpkIpdProjectActorDO::getVersionId, versionId)
                .orderByAsc(SpkIpdProjectActorDO::getBusinessRole));
    }

    default List<SpkIpdProjectActorDO> selectByActor(String actorType, Long actorId) {
        return selectList(new LambdaQueryWrapperX<SpkIpdProjectActorDO>()
                .eq(SpkIpdProjectActorDO::getActorType, actorType)
                .eq(SpkIpdProjectActorDO::getActorId, actorId));
    }

    default PageResult<SpkIpdProjectActorDO> selectPage(SpkIpdProjectActorPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SpkIpdProjectActorDO>()
                .eqIfPresent(SpkIpdProjectActorDO::getProjectId, reqVO.getProjectId())
                .eqIfPresent(SpkIpdProjectActorDO::getVersionId, reqVO.getVersionId())
                .eqIfPresent(SpkIpdProjectActorDO::getActorType, reqVO.getActorType())
                .eqIfPresent(SpkIpdProjectActorDO::getActorId, reqVO.getActorId())
                .eqIfPresent(SpkIpdProjectActorDO::getBusinessRole, reqVO.getBusinessRole())
                .eqIfPresent(SpkIpdProjectActorDO::getStatus, reqVO.getStatus())
                .orderByDesc(SpkIpdProjectActorDO::getUpdateTime));
    }
}
