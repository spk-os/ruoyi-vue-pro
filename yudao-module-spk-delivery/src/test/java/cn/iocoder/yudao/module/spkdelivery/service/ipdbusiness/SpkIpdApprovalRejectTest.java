package cn.iocoder.yudao.module.spkdelivery.service.ipdbusiness;

import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.approval.SpkIpdDecisionReqVO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdDecisionRecordDO;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness.SpkIpdDecisionRecordMapper;
import cn.iocoder.yudao.module.spkdelivery.service.dcp.SpkDcpRedirectService;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpkIpdApprovalRejectTest {

    @Test
    void rejectRedirectsLiveApprovalTaskToAutomaticStageEntryWithoutNativeReject() throws Exception {
        BpmTaskService bpmTaskService = mock(BpmTaskService.class);
        SpkIpdFlowRunService flowRunService = mock(SpkIpdFlowRunService.class);
        SpkIpdDecisionRecordMapper decisionMapper = mock(SpkIpdDecisionRecordMapper.class);
        SpkDcpRedirectService redirectService = mock(SpkDcpRedirectService.class);
        Task task = mock(Task.class);
        when(bpmTaskService.getTask("task-pdcp")).thenReturn(task);
        when(task.getAssignee()).thenReturn("7");
        when(task.getProcessInstanceId()).thenReturn("process-1");
        when(task.getTaskDefinitionKey()).thenReturn("n_pdcp");

        SpkIpdApprovalServiceImpl service = new SpkIpdApprovalServiceImpl();
        set(service, "bpmTaskService", bpmTaskService);
        set(service, "flowRunService", flowRunService);
        set(service, "decisionRecordMapper", decisionMapper);
        set(service, "dcpRedirectService", redirectService);
        SpkIpdDecisionReqVO request = new SpkIpdDecisionReqVO();
        request.setDecision("REJECT");
        request.setReason("计划产物未通过契约预检");

        SpkIpdDecisionRecordDO result = service.createDecision(7L, "task-pdcp", request);

        assertEquals("REJECT", result.getDecision());
        verify(decisionMapper).insert(any(SpkIpdDecisionRecordDO.class));
        verify(redirectService).redirect(eq("process-1"), eq("PDCP"), eq("n_plan_t1"),
                org.mockito.ArgumentMatchers.argThat((Map<String, Object> variables) ->
                        "plan".equals(variables.get(SpkIpdReworkVariables.TARGET_STAGE))
                                && "计划产物未通过契约预检".equals(
                                variables.get(SpkIpdReworkVariables.REASON))));
        verify(bpmTaskService, never()).rejectTask(anyLong(), any());
    }

    private static void set(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
