package cn.iocoder.yudao.module.spkdelivery.dal.mysql.agentsquad;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.agentsquad.SpkAgentSquadMemberDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * SPK-OS 智能体编队成员 Mapper
 *
 * @author SPK-OS
 */
@Mapper
public interface SpkAgentSquadMemberMapper extends BaseMapperX<SpkAgentSquadMemberDO> {

    default List<SpkAgentSquadMemberDO> selectListBySquadId(Long squadId) {
        return selectList(new LambdaQueryWrapperX<SpkAgentSquadMemberDO>()
                .eq(SpkAgentSquadMemberDO::getSquadId, squadId)
                .orderByAsc(SpkAgentSquadMemberDO::getSortOrder));
    }

    default SpkAgentSquadMemberDO selectBySquadIdAndAgentId(Long squadId, Long agentId) {
        return selectOne(new LambdaQueryWrapperX<SpkAgentSquadMemberDO>()
                .eq(SpkAgentSquadMemberDO::getSquadId, squadId)
                .eq(SpkAgentSquadMemberDO::getAgentId, agentId));
    }

    default int deleteBySquadId(Long squadId) {
        return delete(new LambdaQueryWrapperX<SpkAgentSquadMemberDO>()
                .eq(SpkAgentSquadMemberDO::getSquadId, squadId));
    }

}
