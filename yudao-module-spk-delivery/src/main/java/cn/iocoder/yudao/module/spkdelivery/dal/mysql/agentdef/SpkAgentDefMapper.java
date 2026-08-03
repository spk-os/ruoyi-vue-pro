package cn.iocoder.yudao.module.spkdelivery.dal.mysql.agentdef;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.agentdef.vo.SpkAgentDefPageReqVO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.agentdef.SpkAgentDefDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * SPK-OS 智能体定义 Mapper
 *
 * @author SPK-OS
 */
@Mapper
public interface SpkAgentDefMapper extends BaseMapperX<SpkAgentDefDO> {

    default PageResult<SpkAgentDefDO> selectPage(SpkAgentDefPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SpkAgentDefDO>()
                .likeIfPresent(SpkAgentDefDO::getName, reqVO.getName())
                .likeIfPresent(SpkAgentDefDO::getCode, reqVO.getCode())
                .likeIfPresent(SpkAgentDefDO::getRole, reqVO.getRole())
                .eqIfPresent(SpkAgentDefDO::getStatus, reqVO.getStatus())
                .eqIfPresent(SpkAgentDefDO::getRuntimeType, reqVO.getRuntimeType())
                .eqIfPresent(SpkAgentDefDO::getHidden, reqVO.getHidden())
                .orderByDesc(SpkAgentDefDO::getId));
    }

    default SpkAgentDefDO selectByName(String name) {
        return selectOne(new LambdaQueryWrapperX<SpkAgentDefDO>().eq(SpkAgentDefDO::getName, name));
    }

    default SpkAgentDefDO selectByCode(String code) {
        return selectOne(new LambdaQueryWrapperX<SpkAgentDefDO>().eq(SpkAgentDefDO::getCode, code));
    }

    default List<SpkAgentDefDO> selectListByHidden(Integer hidden) {
        return selectList(new LambdaQueryWrapperX<SpkAgentDefDO>()
                .eq(SpkAgentDefDO::getHidden, hidden)
                .orderByAsc(SpkAgentDefDO::getId));
    }

    default List<SpkAgentDefDO> selectListByAgentKind(String agentKind) {
        return selectList(new LambdaQueryWrapperX<SpkAgentDefDO>()
                .eq(SpkAgentDefDO::getAgentKind, agentKind)
                .orderByAsc(SpkAgentDefDO::getId));
    }

    default List<SpkAgentDefDO> selectListByAgentKindAndVerifierType(String agentKind, String verifierType) {
        return selectList(new LambdaQueryWrapperX<SpkAgentDefDO>()
                .eq(SpkAgentDefDO::getAgentKind, agentKind)
                .eq(SpkAgentDefDO::getVerifierType, verifierType)
                .orderByAsc(SpkAgentDefDO::getId));
    }
}
