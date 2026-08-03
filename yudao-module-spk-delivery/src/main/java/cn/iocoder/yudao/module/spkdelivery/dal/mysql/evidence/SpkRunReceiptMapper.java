package cn.iocoder.yudao.module.spkdelivery.dal.mysql.evidence;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.evidence.SpkRunReceiptDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * Run Receipt Mapper
 *
 * @author SPK-OS
 */
@Mapper
public interface SpkRunReceiptMapper extends BaseMapperX<SpkRunReceiptDO> {

    default SpkRunReceiptDO selectByRunId(String runId) {
        return selectOne(new LambdaQueryWrapperX<SpkRunReceiptDO>()
                .eq(SpkRunReceiptDO::getRunId, runId));
    }

    default SpkRunReceiptDO selectByActivityRunId(String activityRunId) {
        return selectOne(new LambdaQueryWrapperX<SpkRunReceiptDO>()
                .eq(SpkRunReceiptDO::getActivityRunId, activityRunId));
    }

    default List<SpkRunReceiptDO> selectListByProcessInstanceId(String processInstanceId) {
        // processInstanceId 经 payload 关联，P1 用 lead_agent 维度统计
        return selectList(new LambdaQueryWrapperX<SpkRunReceiptDO>()
                .orderByDesc(SpkRunReceiptDO::getId));
    }

}
