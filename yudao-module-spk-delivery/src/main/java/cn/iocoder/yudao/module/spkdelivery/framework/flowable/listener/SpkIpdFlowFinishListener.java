package cn.iocoder.yudao.module.spkdelivery.framework.flowable.listener;

import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEventListener;
import cn.iocoder.yudao.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import cn.iocoder.yudao.module.spkdelivery.service.feedback.SpkFeedbackService;
import cn.iocoder.yudao.module.spkdelivery.service.ipdbusiness.SpkIpdFlowRunService;
import cn.iocoder.yudao.module.spkdelivery.service.sunset.SpkSunsetService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * SPK-OS IPD 主流程结束监听器
 * <p>
 * 镜像 {@code BpmOALeaveStatusListener}：按 processDefinitionKey=spkIpdFlow 过滤。
 * <ul>
 *   <li>APPROVE(2)：流程正常结束 → 启动 R8 退市归档 + 采集 R7 反馈。</li>
 *   <li>REJECT(3)/CANCEL(4)：No-Go / 取消 → 仅记录，不触发退市。</li>
 * </ul>
 *
 * @author SPK-OS
 */
@Slf4j
@Configuration
public class SpkIpdFlowFinishListener extends BpmProcessInstanceStatusEventListener {

    public static final String PROCESS_KEY = "spkIpdFlow";

    @Resource
    private SpkSunsetService sunsetService;
    @Resource
    private SpkFeedbackService feedbackService;
    @Resource
    @Lazy // FlowRunService 间接依赖 BPM 引擎，延迟加载避免与监听器初始化循环
    private SpkIpdFlowRunService flowRunService;

    @Override
    protected String getProcessDefinitionKey() {
        return PROCESS_KEY;
    }

    @Override
    protected void onEvent(BpmProcessInstanceStatusEvent event) {
        String instanceId = event.getId();
        Integer status = event.getStatus();
        log.info("[onEvent][IPD 流程状态变更 instanceId={} status={} businessKey={} reason={}]",
                instanceId, status, event.getBusinessKey(), event.getReason());
        // 仅在流程正常通过（APPROVE）时回写 FlowRun COMPLETED + 启动 R8 退市 + 采集 R7 反馈
        if (!BpmProcessInstanceStatusEnum.APPROVE.getStatus().equals(status)) {
            return;
        }
        // P0：流程 APPROVE → 回写 FlowRun 终态 COMPLETED（此前无人置位致永远 RUNNING）
        try {
            flowRunService.markCompletedByInstance(instanceId);
        } catch (Exception e) {
            log.error("[onEvent][回写 FlowRun COMPLETED 失败 instanceId={}]", instanceId, e);
        }
        try {
            // R8 退市归档
            sunsetService.start(instanceId, null, "archiving");
        } catch (Exception e) {
            log.error("[onEvent][启动 R8 退市失败 instanceId={}]", instanceId, e);
        }
        try {
            // R7 反馈采集（种子）
            feedbackService.collect(instanceId, "ipd-finish", "IPD 流程正常结束，采集结项反馈", null, Boolean.FALSE);
        } catch (Exception e) {
            log.error("[onEvent][采集 R7 反馈失败 instanceId={}]", instanceId, e);
        }
    }

}
