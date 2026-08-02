package cn.iocoder.yudao.module.spkdelivery.dal.mysql.agentsquad;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.agentsquad.vo.SpkAgentSquadPageReqVO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.agentsquad.SpkAgentSquadDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * SPK-OS 智能体编队 Mapper
 *
 * @author SPK-OS
 */
@Mapper
public interface SpkAgentSquadMapper extends BaseMapperX<SpkAgentSquadDO> {

    default PageResult<SpkAgentSquadDO> selectPage(SpkAgentSquadPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SpkAgentSquadDO>()
                .likeIfPresent(SpkAgentSquadDO::getName, reqVO.getName())
                .likeIfPresent(SpkAgentSquadDO::getCode, reqVO.getCode())
                .eqIfPresent(SpkAgentSquadDO::getStatus, reqVO.getStatus())
                .orderByDesc(SpkAgentSquadDO::getId));
    }

    default SpkAgentSquadDO selectByName(String name) {
        return selectOne(new LambdaQueryWrapperX<SpkAgentSquadDO>().eq(SpkAgentSquadDO::getName, name));
    }

    default SpkAgentSquadDO selectByCode(String code) {
        return selectOne(new LambdaQueryWrapperX<SpkAgentSquadDO>().eq(SpkAgentSquadDO::getCode, code));
    }

}
