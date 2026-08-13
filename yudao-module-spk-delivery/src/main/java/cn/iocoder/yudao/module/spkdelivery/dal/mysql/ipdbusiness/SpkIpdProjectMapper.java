package cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.project.SpkIpdProjectPageReqVO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdProjectDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * IPD 项目 Mapper
 *
 * @author SPK-OS
 */
@Mapper
public interface SpkIpdProjectMapper extends BaseMapperX<SpkIpdProjectDO> {

    default SpkIpdProjectDO selectByProjectCode(String projectCode) {
        return selectOne(SpkIpdProjectDO::getProjectCode, projectCode);
    }

    default SpkIpdProjectDO selectByProjectNo(String projectNo) {
        return selectOne(SpkIpdProjectDO::getProjectNo, projectNo);
    }

    default PageResult<SpkIpdProjectDO> selectPage(SpkIpdProjectPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SpkIpdProjectDO>()
                .likeIfPresent(SpkIpdProjectDO::getName, reqVO.getName())
                .eqIfPresent(SpkIpdProjectDO::getProjectCode, reqVO.getProjectCode())
                .eqIfPresent(SpkIpdProjectDO::getOwnerUserId, reqVO.getOwnerUserId())
                .eqIfPresent(SpkIpdProjectDO::getStatus, reqVO.getStatus())
                .eqIfPresent(SpkIpdProjectDO::getHealth, reqVO.getHealth())
                .orderByDesc(SpkIpdProjectDO::getUpdateTime));
    }
}
