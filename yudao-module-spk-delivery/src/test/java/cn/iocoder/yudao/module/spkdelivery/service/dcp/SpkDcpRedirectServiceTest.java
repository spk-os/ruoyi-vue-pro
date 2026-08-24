package cn.iocoder.yudao.module.spkdelivery.service.dcp;

import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.dcp.SpkDcpRedirectLogDO;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.dcp.SpkDcpRedirectLogMapper;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ChangeActivityStateBuilder;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpkDcpRedirectServiceTest {

    @Test
    void redirectMarksRunningTaskCanceledBeforeFlowableStateChange() throws Exception {
        RuntimeService runtimeService = mock(RuntimeService.class);
        BpmTaskService bpmTaskService = mock(BpmTaskService.class);
        SpkDcpRedirectLogMapper redirectLogMapper = mock(SpkDcpRedirectLogMapper.class);
        ChangeActivityStateBuilder builder = mock(ChangeActivityStateBuilder.class);
        Task task = mock(Task.class);

        when(task.getId()).thenReturn("task-pdcp");
        when(runtimeService.getActiveActivityIds("process-1")).thenReturn(List.of("n_pdcp"));
        when(bpmTaskService.getRunningTaskListByProcessInstanceId("process-1", null, null))
                .thenReturn(List.of(task));
        when(runtimeService.createChangeActivityStateBuilder()).thenReturn(builder);
        when(builder.processInstanceId("process-1")).thenReturn(builder);
        when(builder.moveActivityIdsToSingleActivityId(List.of("n_pdcp"), "n_plan_t1"))
                .thenReturn(builder);

        SpkDcpRedirectService service = new SpkDcpRedirectService();
        set(service, "runtimeService", runtimeService);
        set(service, "bpmTaskService", bpmTaskService);
        set(service, "dcpRedirectLogMapper", redirectLogMapper);

        service.redirect("process-1", "PDCP", "n_plan_t1", Map.of("reason", "Aegis 未通过"));

        InOrder order = inOrder(bpmTaskService, builder);
        order.verify(bpmTaskService).processTaskCanceled("task-pdcp");
        order.verify(builder).changeState();
        verify(runtimeService).setVariables(eq("process-1"), any());
        verify(redirectLogMapper).insert(any(SpkDcpRedirectLogDO.class));
    }

    private static void set(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
