package cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdactivity;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdactivity.vo.SpkIpdActivityDefPageReqVO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdactivity.SpkIpdActivityDefDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * IPD Activity 定义 Mapper
 *
 * @author SPK-OS
 */
@Mapper
public interface SpkIpdActivityDefMapper extends BaseMapperX<SpkIpdActivityDefDO> {

    default SpkIpdActivityDefDO selectByActivityIdAndVersion(String activityId, String version) {
        return selectOne(new LambdaQueryWrapperX<SpkIpdActivityDefDO>()
                .eq(SpkIpdActivityDefDO::getActivityId, activityId)
                .eq(SpkIpdActivityDefDO::getVersion, version));
    }

    /**
     * 按一组 activityId 批量查定义（取每条最新版本即可，Cockpit 只需中文名）。
     * 同一 activityId 多版本时，由调用方按 activityId 去重保留首条。
     */
    default List<SpkIpdActivityDefDO> selectListByActivityIds(Collection<String> activityIds) {
        if (activityIds == null || activityIds.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        return selectList(new LambdaQueryWrapperX<SpkIpdActivityDefDO>()
                .in(SpkIpdActivityDefDO::getActivityId, activityIds));
    }

    default List<SpkIpdActivityDefDO> selectListByStage(String stage) {
        return selectList(new LambdaQueryWrapperX<SpkIpdActivityDefDO>()
                .eq(SpkIpdActivityDefDO::getStage, stage)
                .orderByAsc(SpkIpdActivityDefDO::getActivityId));
    }

    default List<SpkIpdActivityDefDO> selectListByStatus(String status) {
        return selectList(new LambdaQueryWrapperX<SpkIpdActivityDefDO>()
                .eq(SpkIpdActivityDefDO::getStatus, status)
                .orderByAsc(SpkIpdActivityDefDO::getActivityId));
    }

    default PageResult<SpkIpdActivityDefDO> selectPage(SpkIpdActivityDefPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SpkIpdActivityDefDO>()
                .likeIfPresent(SpkIpdActivityDefDO::getActivityId, reqVO.getActivityId())
                .likeIfPresent(SpkIpdActivityDefDO::getName, reqVO.getName())
                .eqIfPresent(SpkIpdActivityDefDO::getStage, reqVO.getStage())
                .eqIfPresent(SpkIpdActivityDefDO::getStatus, reqVO.getStatus())
                .eqIfPresent(SpkIpdActivityDefDO::getLeadAgentCode, reqVO.getLeadAgentCode())
                .orderByDesc(SpkIpdActivityDefDO::getId));
    }

}
