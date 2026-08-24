package cn.iocoder.yudao.module.spkdelivery.service.agent;

import cn.iocoder.yudao.module.bpm.api.task.BpmProcessTaskApi;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdFlowRunDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.taskcontract.SpkTaskContractDO;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness.SpkIpdFlowRunMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.taskcontract.SpkTaskContractMapper;
import cn.iocoder.yudao.module.spkdelivery.service.feedback.SpkFeedbackService;
import cn.iocoder.yudao.module.spkdelivery.service.router.SpkRouteResult;
import cn.iocoder.yudao.module.spkdelivery.service.router.SpkTaskRouterService;
import org.flowable.engine.RuntimeService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpkAgentTaskServiceImplTest {

    @Test
    void recoverKeepsOldEvidenceAndStartsNewBoundedCycleAsynchronously() throws Exception {
        SpkTaskContractMapper contractMapper = mock(SpkTaskContractMapper.class);
        SpkIpdFlowRunMapper flowRunMapper = mock(SpkIpdFlowRunMapper.class);
        RuntimeService runtimeService = mock(RuntimeService.class);
        BpmProcessTaskApi processTaskApi = mock(BpmProcessTaskApi.class);
        SpkTaskRouterService router = mock(SpkTaskRouterService.class);
        SpkFeedbackService feedbackService = mock(SpkFeedbackService.class);
        SpkAgentTaskServiceImpl service = new SpkAgentTaskServiceImpl();
        ReflectionTestUtils.setField(service, "taskContractMapper", contractMapper);
        ReflectionTestUtils.setField(service, "flowRunMapper", flowRunMapper);
        ReflectionTestUtils.setField(service, "runtimeService", runtimeService);
        ReflectionTestUtils.setField(service, "processTaskApi", processTaskApi);
        ReflectionTestUtils.setField(service, "taskRouterService", router);
        ReflectionTestUtils.setField(service, "feedbackService", feedbackService);

        SpkTaskContractDO oldContract = SpkTaskContractDO.builder()
                .activityRunId("old-run")
                .activityId("ACT-FULL-CONCEPT-1")
                .activityVersion("1.0.0")
                .processInstanceId("process-40")
                .businessKey("IPD:FR-40")
                .nodeKey("concept-agent")
                .receiveTaskKey("wait-concept")
                .attemptNo(3)
                .fencingToken(9L)
                .status("failed")
                .failureReason("旧配置超时")
                .build();
        SpkTaskContractDO newContract = SpkTaskContractDO.builder()
                .activityRunId("new-run")
                .processInstanceId("process-40")
                .receiveTaskKey("wait-concept")
                .build();
        when(contractMapper.selectByActivityRunId("old-run")).thenReturn(oldContract);
        when(contractMapper.selectByActivityRunId("new-run")).thenReturn(newContract);
        when(flowRunMapper.selectByProcessInstanceId("process-40"))
                .thenReturn(SpkIpdFlowRunDO.builder().id(40L).status("BLOCKED").build());
        when(flowRunMapper.recoverBlocked(40L)).thenReturn(1);
        when(runtimeService.getActiveActivityIds("process-40")).thenReturn(List.of("wait-concept"));

        CountDownLatch routeEntered = new CountDownLatch(1);
        CountDownLatch releaseRoute = new CountDownLatch(1);
        when(router.route(eq("ACT-FULL-CONCEPT-1"), eq("1.0.0"), eq("process-40"),
                any(), eq("IPD:FR-40"), eq("concept-agent"), eq("wait-concept"), anyList()))
                .thenAnswer(invocation -> {
                    routeEntered.countDown();
                    assertTrue(releaseRoute.await(3, TimeUnit.SECONDS));
                    return new SpkRouteResult()
                            .setActivityRunId("new-run")
                            .setAgentResult("{\"document\":{}}")
                            .setStatus("done");
                });

        try {
            assertNull(service.intervene("old-run", "recover", "已将 Omnigent 等待时间调为 60 分钟"));
            assertTrue(routeEntered.await(2, TimeUnit.SECONDS));
            verify(flowRunMapper).recoverBlocked(40L);
            verify(feedbackService).collect(eq("process-40"), eq("agent-infrastructure-recovery"),
                    eq("Activity ACT-FULL-CONCEPT-1 开启新的有界自动执行"),
                    eq("Cortex 基础设施修复后恢复：已将 Omnigent 等待时间调为 60 分钟"), eq(false));
            // 恢复调用没有改写已失败合同，且在后台新建执行开始前已经返回。
            verify(contractMapper, org.mockito.Mockito.never()).updateById(oldContract);
        } finally {
            releaseRoute.countDown();
        }

        verify(processTaskApi, timeout(3000)).triggerTask("process-40", "wait-concept");
        verify(contractMapper, timeout(3000)).updateById(newContract);
    }
}
