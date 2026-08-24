package cn.iocoder.yudao.module.spkdelivery.service.dcp;

import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.dcp.SpkDcpRedirectLogDO;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.dcp.SpkDcpRedirectLogMapper;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.spkdelivery.enums.ErrorCodeConstants.DCP_REDIRECT_FAIL;
import static cn.iocoder.yudao.module.spkdelivery.enums.ErrorCodeConstants.DCP_REDIRECT_LIMIT_EXCEEDED;
import static cn.iocoder.yudao.module.spkdelivery.service.ipdbusiness.SpkIpdReworkVariables.COUNT;

/**
 * SPK-OS DCP 回退服务
 * <p>
 * DCP 三态：
 * <ul>
 *   <li>Go：网关放行（流程自然推进）</li>
 *   <li>No-Go：cancel 流程实例（由发起人取消）</li>
 *   <li>Redirect：经 createChangeActivityStateBuilder 把当前活动移回目标节点 + rCount++</li>
 * </ul>
 *
 * @author SPK-OS
 */
@Slf4j
@Service
@Validated
public class SpkDcpRedirectService {

    /** 单 DCP 最大回退次数 */
    private static final int MAX_REDIRECT = 5;

    @Resource
    private SpkDcpRedirectLogMapper dcpRedirectLogMapper;
    @Resource
    private RuntimeService runtimeService;
    @Resource
    private BpmTaskService bpmTaskService;

    /**
     * DCP Redirect：把当前活动移回 targetNode，rCount++
     *
     * @param instanceId   流程实例编号
     * @param dcp          DCP 标识 cdc/pdc/adc/ldc
     * @param targetNode   回退目标节点 key
     */
    public void redirect(String instanceId, String dcp, String targetNode) {
        redirect(instanceId, dcp, targetNode, Map.of());
    }

    /** DCP Redirect，并在迁移前写入下一轮自动执行所需的返工上下文。 */
    public void redirect(String instanceId, String dcp, String targetNode, Map<String, Object> reworkVariables) {
        // 1. 查/建回退日志，校验次数
        SpkDcpRedirectLogDO redirectLog = dcpRedirectLogMapper.selectByInstanceIdAndDcp(instanceId, dcp);
        int count = redirectLog == null ? 0 : (redirectLog.getRedirectCount() == null ? 0 : redirectLog.getRedirectCount());
        if (count >= MAX_REDIRECT) {
            throw exception(DCP_REDIRECT_LIMIT_EXCEEDED);
        }
        count++;
        // 2. 取当前活动 id 列表
        List<String> currentActivityIds = runtimeService.getActiveActivityIds(instanceId);
        if (currentActivityIds == null || currentActivityIds.isEmpty()) {
            throw exception(DCP_REDIRECT_FAIL);
        }
        // 3. 经 createChangeActivityStateBuilder 回退
        try {
            Map<String, Object> variables = new LinkedHashMap<>();
            if (reworkVariables != null) {
                variables.putAll(reworkVariables);
            }
            variables.put(COUNT, count);
            runtimeService.setVariables(instanceId, variables);

            // Flowable 8 的动态迁移会先删除运行时 Task，再分发 ACTIVITY_CANCELLED。
            // 通用 BPM 监听器收到该事件后会给 Task 写取消状态；若未提前写入，命令上下文里
            // 虽仍能查到已标记删除的 Task，setVariableLocal 却会抛出 "Task is already deleted"，
            // 导致整个 DCP 驳回事务回滚。与 BpmTaskServiceImpl.moveTaskToEnd 的既有顺序一致，
            // 必须在 changeState 前把即将迁移的运行中任务标记为取消。
            List<Task> runningTasks = bpmTaskService.getRunningTaskListByProcessInstanceId(instanceId, null, null);
            if (runningTasks != null) {
                runningTasks.forEach(task -> bpmTaskService.processTaskCanceled(task.getId()));
            }
            runtimeService.createChangeActivityStateBuilder()
                    .processInstanceId(instanceId)
                    .moveActivityIdsToSingleActivityId(currentActivityIds, targetNode)
                    .changeState();
        } catch (Exception e) {
            log.error("[redirect][回退失败 instanceId={} dcp={} target={}]", instanceId, dcp, targetNode, e);
            throw exception(DCP_REDIRECT_FAIL);
        }
        // 4. 写回退日志
        if (redirectLog == null) {
            dcpRedirectLogMapper.insert(SpkDcpRedirectLogDO.builder()
                    .instanceId(instanceId)
                    .dcp(dcp)
                    .redirectCount(count)
                    .targetNode(targetNode)
                    .build());
        } else {
            redirectLog.setRedirectCount(count);
            redirectLog.setTargetNode(targetNode);
            dcpRedirectLogMapper.updateById(redirectLog);
        }
        log.info("[redirect][instanceId={} dcp={} count={} target={}]", instanceId, dcp, count, targetNode);
    }

    /**
     * 按流程实例查询全部 DCP 回退日志（用于详情页 IPD 产物 tab）
     */
    public List<SpkDcpRedirectLogDO> getListByInstanceId(String instanceId) {
        return dcpRedirectLogMapper.selectListByInstanceId(instanceId);
    }

}
