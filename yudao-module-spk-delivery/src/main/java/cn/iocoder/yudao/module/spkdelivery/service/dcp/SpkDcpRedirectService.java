package cn.iocoder.yudao.module.spkdelivery.service.dcp;

import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.dcp.SpkDcpRedirectLogDO;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.dcp.SpkDcpRedirectLogMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.spkdelivery.enums.ErrorCodeConstants.DCP_REDIRECT_FAIL;
import static cn.iocoder.yudao.module.spkdelivery.enums.ErrorCodeConstants.DCP_REDIRECT_LIMIT_EXCEEDED;

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

    /**
     * DCP Redirect：把当前活动移回 targetNode，rCount++
     *
     * @param instanceId   流程实例编号
     * @param dcp          DCP 标识 cdc/pdc/adc/ldc
     * @param targetNode   回退目标节点 key
     */
    public void redirect(String instanceId, String dcp, String targetNode) {
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

}
