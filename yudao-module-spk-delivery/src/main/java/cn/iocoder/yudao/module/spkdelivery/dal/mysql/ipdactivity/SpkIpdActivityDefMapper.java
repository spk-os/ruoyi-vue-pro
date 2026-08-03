package cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdactivity;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdactivity.vo.SpkIpdActivityDefPageReqVO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdactivity.SpkIpdActivityDefDO;
import org.apache.ibatis.annotations.Mapper;

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
