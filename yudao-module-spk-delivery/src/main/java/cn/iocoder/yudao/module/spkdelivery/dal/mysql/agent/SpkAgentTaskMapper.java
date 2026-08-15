package cn.iocoder.yudao.module.spkdelivery.dal.mysql.agent;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.agent.vo.SpkAgentLoadStatsVO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.agent.SpkAgentTaskDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * SPK-OS Agent 任务 Mapper
 *
 * @author SPK-OS
 */
@Mapper
public interface SpkAgentTaskMapper extends BaseMapperX<SpkAgentTaskDO> {

    default SpkAgentTaskDO selectByInstanceIdAndNodeKey(String instanceId, String nodeKey) {
        return selectOne(new LambdaQueryWrapperX<SpkAgentTaskDO>()
                .eq(SpkAgentTaskDO::getInstanceId, instanceId)
                .eq(SpkAgentTaskDO::getNodeKey, nodeKey));
    }

    default SpkAgentTaskDO selectByTaskId(String taskId) {
        return selectOne(new LambdaQueryWrapperX<SpkAgentTaskDO>()
                .eq(SpkAgentTaskDO::getTaskId, taskId));
    }

    default List<SpkAgentTaskDO> selectListByInstanceId(String instanceId) {
        return selectList(new LambdaQueryWrapperX<SpkAgentTaskDO>()
                .eq(SpkAgentTaskDO::getInstanceId, instanceId));
    }

    /**
     * 按智能体定义维度聚合运行负载（§7.1 第 3 视图）。
     * <p>真实数据源：spk_task_contract（IPD 每次 Activity 运行都落一行，含 lead_agent_id/status/started/finished/failure_reason）。
     * JOIN spk_agent_def 取名称；按 lead_agent_id 分组统计 running/done/failed/接管/token。
     * 数据稀疏时返回空列表（不造假，前端标"样本不足"）。
     * <p>PG 方言：COUNT(*) FILTER (WHERE ...) 聚合，COALESCE 兜底 null。
     */
    @Select("SELECT c.lead_agent_id AS agentDefId, d.name AS agentName, d.code AS agentCode, " +
            "COUNT(*) AS total, " +
            "COUNT(*) FILTER (WHERE c.status = 'running' OR c.status = 'queued') AS running, " +
            "COUNT(*) FILTER (WHERE c.status = 'done') AS succeeded, " +
            "COUNT(*) FILTER (WHERE c.status = 'failed') AS failed, " +
            "COUNT(*) FILTER (WHERE c.attempt_no > 1) AS intervened, " +
            "0 AS totalTokens, " +
            "MAX(c.update_time) AS lastActivity " +
            "FROM spk_task_contract c LEFT JOIN spk_agent_def d ON c.lead_agent_id = d.id " +
            "WHERE c.deleted = 0 AND c.lead_agent_id IS NOT NULL " +
            "GROUP BY c.lead_agent_id, d.name, d.code " +
            "ORDER BY total DESC")
    List<SpkAgentLoadStatsVO> selectLoadStats();

    /**
     * 取某智能体最近一次失败原因（前端 lastError 列展示）。
     */
    @Select("SELECT c.failure_reason FROM spk_task_contract c " +
            "WHERE c.deleted = 0 AND c.lead_agent_id = #{agentDefId} AND c.status = 'failed' " +
            "ORDER BY c.update_time DESC LIMIT 1")
    String selectLastErrorByDefId(Long agentDefId);

}
