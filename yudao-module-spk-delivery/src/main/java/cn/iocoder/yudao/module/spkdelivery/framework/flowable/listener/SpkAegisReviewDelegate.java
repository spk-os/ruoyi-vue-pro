package cn.iocoder.yudao.module.spkdelivery.framework.flowable.listener;

import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.aegis.SpkAegisReviewDO;
import cn.iocoder.yudao.module.spkdelivery.service.aegis.SpkAegisReviewService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * SPK-OS SpkAegisReviewDelegate —— ADCP 前同步 LLM 审查的 Flowable JavaDelegate
 * <p>
 * 由 simpleModel AEGIS_REVIEW 节点经 serviceTask(delegateExpression="${spkAegisReviewDelegate}") 触发，
 * 或经 raw BPMN XML deploy 的 serviceTask 使用。execute 返回即推进（无 receiveTask）。
 * <p>
 * 默认 IPD 流程经 BPM HTTP_REQUEST 触发器调 /spk/aegis/review 驱动；本 Delegate 为 raw-BPMN 回退路径。
 *
 * @author SPK-OS
 */
@Slf4j
@Component("spkAegisReviewDelegate")
public class SpkAegisReviewDelegate implements JavaDelegate {

    public static final String VAR_AEGIS_VERDICT = "aegisVerdict";
    public static final String VAR_AEGIS_REPORT = "aegisReport";

    @Resource
    private SpkAegisReviewService aegisReviewService;
    @Resource
    private RuntimeService runtimeService;

    @Override
    public void execute(DelegateExecution execution) {
        String instanceId = execution.getProcessInstanceId();
        String nodeKey = execution.getCurrentFlowElement() != null ? execution.getCurrentFlowElement().getId() : null;
        SpkAegisReviewDO review = aegisReviewService.review(instanceId, nodeKey);
        Map<String, Object> vars = new HashMap<>();
        vars.put(VAR_AEGIS_VERDICT, review.getVerdict());
        vars.put(VAR_AEGIS_REPORT, review.getReport());
        runtimeService.setVariables(instanceId, vars);
    }

}
