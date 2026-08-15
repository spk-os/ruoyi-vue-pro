package cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdGovernanceAuditDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * IPD 治理审计 Mapper（append-only，仅插入与查询）
 *
 * @author SPK-OS
 */
@Mapper
public interface SpkIpdGovernanceAuditMapper extends BaseMapperX<SpkIpdGovernanceAuditDO> {

    default List<SpkIpdGovernanceAuditDO> selectListByActionTypeAndRefId(String actionType, Long refId) {
        return selectList(new LambdaQueryWrapperX<SpkIpdGovernanceAuditDO>()
                .eqIfPresent(SpkIpdGovernanceAuditDO::getActionType, actionType)
                .eqIfPresent(SpkIpdGovernanceAuditDO::getRefId, refId)
                .orderByDesc(SpkIpdGovernanceAuditDO::getCreateTime));
    }
}
