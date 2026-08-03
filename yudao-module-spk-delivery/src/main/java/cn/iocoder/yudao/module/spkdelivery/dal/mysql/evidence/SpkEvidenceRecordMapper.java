package cn.iocoder.yudao.module.spkdelivery.dal.mysql.evidence;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.evidence.SpkEvidenceRecordDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * Evidence Record Mapper（追加式，哈希链）
 *
 * @author SPK-OS
 */
@Mapper
public interface SpkEvidenceRecordMapper extends BaseMapperX<SpkEvidenceRecordDO> {

    default List<SpkEvidenceRecordDO> selectListByActivityRunId(String activityRunId) {
        return selectList(new LambdaQueryWrapperX<SpkEvidenceRecordDO>()
                .eq(SpkEvidenceRecordDO::getActivityRunId, activityRunId)
                .orderByAsc(SpkEvidenceRecordDO::getOccurredAt)
                .orderByAsc(SpkEvidenceRecordDO::getId));
    }

    default List<SpkEvidenceRecordDO> selectListByProcessInstanceId(String processInstanceId) {
        return selectList(new LambdaQueryWrapperX<SpkEvidenceRecordDO>()
                .eq(SpkEvidenceRecordDO::getProcessInstanceId, processInstanceId)
                .orderByAsc(SpkEvidenceRecordDO::getOccurredAt)
                .orderByAsc(SpkEvidenceRecordDO::getId));
    }

    default SpkEvidenceRecordDO selectLastByActivityRunId(String activityRunId) {
        // 哈希链取上一条：一个 activityRun 可能有多条证据（run/artifact/verification），
        // 必须显式 LIMIT 1，否则 selectOne 在多行时抛 TooManyResultsException。
        return selectOne(new LambdaQueryWrapperX<SpkEvidenceRecordDO>()
                .eq(SpkEvidenceRecordDO::getActivityRunId, activityRunId)
                .orderByDesc(SpkEvidenceRecordDO::getId)
                .last("LIMIT 1"));
    }

}
