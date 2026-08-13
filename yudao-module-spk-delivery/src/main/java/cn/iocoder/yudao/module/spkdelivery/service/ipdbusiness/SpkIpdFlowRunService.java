package cn.iocoder.yudao.module.spkdelivery.service.ipdbusiness;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.flowrun.SpkIpdFlowRunPageReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.flowrun.SpkIpdFlowRunPreflightReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.version.SpkIpdReadinessRespVO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdCommandLogDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdFlowRunDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdIssueCaseDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdVersionDO;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness.SpkIpdFlowRunMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness.SpkIpdIssueCaseMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness.SpkIpdVersionMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.HistoryService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.spkdelivery.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.module.spkdelivery.service.ipdbusiness.SpkIpdBusinessConstants.*;

/**
 * IPD FlowRun 服务：预检/创建/启动/取消/时间线。设计文档 section 9.4.2 / 10.5 / 11.4。
 * <p>
 * FlowRun 是业务运行，process_instance_id 是引擎运行；一条 FlowRun 最多绑定一个 Flowable 实例。
 * 启动幂等（Idempotency-Key + CommandLog），businessKey=IPD:{runNo}；type2 触发器使 createProcessInstance
 * 同步至首个 receiveTask 即返回（约 0.06s），不在 Flowable 事务内等待远程回调（遵守互锁铁律）。
 *
 * @author SPK-OS
 */
@Slf4j
@Service
@Validated
public class SpkIpdFlowRunService {

    @Resource
    private SpkIpdFlowRunMapper flowRunMapper;
    @Resource
    private SpkIpdVersionMapper versionMapper;
    @Resource
    private SpkIpdIssueCaseMapper issueCaseMapper;
    @Resource
    private BpmProcessInstanceApi processInstanceApi;
    @Resource
    private SpkIpdCommandService commandService;
    @Resource
    private SpkIpdProjectBusinessService projectBusinessService;
    @Resource
    private HistoryService historyService;
    private final ObjectMapper json = new ObjectMapper();

    /**
     * 预检：不落运行或仅落检查记录；返回可启动性与逐项检查。
     * 设计文档 section 10.5 /preflight。
     */
    public SpkIpdReadinessRespVO preflight(SpkIpdFlowRunPreflightReqVO req) {
        SpkIpdReadinessRespVO resp = new SpkIpdReadinessRespVO();
        List<SpkIpdReadinessRespVO.Check> checks = new ArrayList<>();
        boolean ready = true;

        // 流程类型与上下文校验
        SpkIpdReadinessRespVO.Check ctx = new SpkIpdReadinessRespVO.Check();
        ctx.setCode("CONTEXT_KEYS");
        boolean ctxOk = validateFlowTypeContext(req, false) == null;
        if (!ctxOk) {
            ctx.setStatus("BLOCK");
            ctx.setMessage(validateFlowTypeContext(req, false));
            ready = false;
        } else {
            ctx.setStatus("PASS");
            ctx.setMessage("上下文键一致");
        }
        checks.add(ctx);

        // 版本就绪度（仅 FULL/INCREMENT 需要）
        if (req.getVersionId() != null) {
            SpkIpdReadinessRespVO vr = projectBusinessService.readiness(req.getVersionId());
            checks.addAll(vr.getChecks());
            if (!Boolean.TRUE.equals(vr.getReady())) {
                ready = false;
            }
            resp.setEffectiveStages(vr.getEffectiveStages());
        } else {
            resp.setEffectiveStages(List.of("ROOT_CAUSE", "FIX_DEVELOP", "VERIFY", "RELEASE"));
        }

        resp.setReady(ready);
        resp.setChecks(checks);
        return resp;
    }

    /**
     * 校验流程类型与版本类型匹配，返回错误消息（null=通过）。
     */
    private String validateFlowTypeContext(SpkIpdFlowRunPreflightReqVO req, boolean strict) {
        String ft = req.getFlowType();
        if (FLOW_FULL_RELEASE.equals(ft) || FLOW_INCREMENT_RELEASE.equals(ft)) {
            if (req.getVersionId() == null) {
                return "FULL_RELEASE/INCREMENT_RELEASE 必须指定 versionId";
            }
            SpkIpdVersionDO v = versionMapper.selectById(req.getVersionId());
            if (v == null) {
                return "版本不存在";
            }
            if (FLOW_FULL_RELEASE.equals(ft) && !VERSION_BASELINE.equals(v.getVersionType())) {
                return "FULL_RELEASE 只能绑定 BASELINE 版本";
            }
            if (FLOW_INCREMENT_RELEASE.equals(ft) && VERSION_BASELINE.equals(v.getVersionType())) {
                return "INCREMENT_RELEASE 不可绑定 BASELINE 版本";
            }
        } else if (FLOW_ISSUE_RESOLUTION.equals(ft)) {
            if (req.getIssueCaseId() == null) {
                return "ISSUE_RESOLUTION 必须指定 issueCaseId";
            }
        } else {
            return "未知 flowType: " + ft;
        }
        return null;
    }

    /**
     * 创建运行草稿：选择版本、类型、档案、裁剪项。设计文档 section 10.5 /flow-runs。
     * S1 profile 仍待 S2 接入；以默认 spkIpdFlow 档案（profileVersion=1）建立最小快照。
     */
    @Transactional(rollbackFor = Exception.class)
    public SpkIpdFlowRunDO createFlowRun(SpkIpdFlowRunPreflightReqVO req) {
        String ctxErr = validateFlowTypeContext(req, false);
        if (ctxErr != null) {
            throw exception(IPD_FLOW_TYPE_MISMATCH);
        }
        // FULL/INCREMENT：同一版本默认只能有一条活跃主交付流
        if (req.getVersionId() != null && !flowRunMapper.selectActiveByVersion(req.getVersionId()).isEmpty()) {
            throw exception(IPD_FLOW_RUN_ACTIVE_EXISTS);
        }
        SpkIpdFlowRunDO run = SpkIpdFlowRunDO.builder()
                .projectId(req.getProjectId())
                .majorReleaseId(req.getVersionId() == null ? null
                        : versionMapper.selectById(req.getVersionId()).getMajorReleaseId())
                .versionId(req.getVersionId())
                .issueCaseId(req.getIssueCaseId())
                .flowType(req.getFlowType())
                .profileId(0L)
                .profileVersion(1)
                .profileSnapshotJson(minimalProfileSnapshot(req))
                .tailoringSnapshotJson(tailoringJson(req.getTailoring()))
                .status("DRAFT")
                .health(HEALTH_UNKNOWN)
                .attemptNo(1)
                .build();
        // run_no / businessKey 依赖自增 id 且列有 NOT NULL+唯一约束，先插占位值再回填
        String tmpNo = "FR-TMP-" + System.nanoTime();
        run.setRunNo(tmpNo);
        run.setBusinessKey("IPD:" + tmpNo);
        flowRunMapper.insert(run);
        run.setRunNo(runNo(run.getId()));
        run.setBusinessKey(businessKey(run.getRunNo()));
        flowRunMapper.updateById(run);
        return run;
    }

    /**
     * 幂等启动 Flowable。设计文档 section 10.5 /start 启动语义：
     * 1. 校验 Idempotency-Key 和 FlowRun READY/DRAFT 状态；事务置 STARTING 并写 Command。
     * 2. 事务外调用 Flowable，businessKey 幂等防重；成功回填 process_instance_id。
     * 3. 相同命令已完成则返回原 FlowRun，不重复创建实例。
     * <p>
     * 本实现：createProcessInstance 同步至首个 receiveTask 即返 processInstanceId（type2 已根治），
     * 故在同一方法内回填；写 Flowable 在事务外（processInstanceApi 内部独立事务）。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> start(Long flowRunId, String idempotencyKey) {
        SpkIpdFlowRunDO run = getFlowRunOrThrow(flowRunId);
        if (!"DRAFT".equals(run.getStatus()) && !"READY".equals(run.getStatus())) {
            throw exception(IPD_FLOW_RUN_NOT_READY);
        }
        String payloadJson = "{\"flowRunId\":" + flowRunId + "}";
        SpkIpdCommandService.CommandEnvelope cmd = commandService.enlist(
                idempotencyKey, CMD_START_FLOW, "FLOW_RUN", String.valueOf(flowRunId), payloadJson);
        if (!cmd.isNew()) {
            // 命中幂等：若已完成启动则返回原结果，否则返回当前命令状态
            SpkIpdCommandLogDO c = cmd.log();
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("flowRunId", flowRunId);
            r.put("commandId", c.getId());
            r.put("commandStatus", c.getStatus());
            r.put("processInstanceId", run.getProcessInstanceId());
            r.put("flowRunStatus", run.getStatus());
            r.put("idempotent", true);
            return r;
        }
        commandService.markRunning(cmd.commandId());
        try {
            run.setStatus("STARTING");
            flowRunMapper.updateById(run);

            Long userId = currentUserId();
            BpmProcessInstanceCreateReqDTO createReq = new BpmProcessInstanceCreateReqDTO();
            createReq.setProcessDefinitionKey(IPD_FLOW_KEY);
            createReq.setBusinessKey(run.getBusinessKey());
            Map<String, Object> variables = new LinkedHashMap<>();
            variables.put(VAR_BUSINESS_KEY, run.getBusinessKey());
            variables.put(VAR_FLOW_RUN_ID, run.getId());
            variables.put(VAR_PROJECT_ID, run.getProjectId());
            if (run.getVersionId() != null) {
                variables.put(VAR_VERSION_ID, run.getVersionId());
            }
            if (run.getIssueCaseId() != null) {
                variables.put(VAR_ISSUE_CASE_ID, run.getIssueCaseId());
            }
            variables.put(VAR_FLOW_TYPE, run.getFlowType());
            variables.put(VAR_PROFILE_VERSION, run.getProfileVersion());
            variables.put(VAR_TRACE_ID, idempotencyKey);
            // projectName 供概念阶段 agent 读取（兼容旧 SpkIpdProjectService 变量名）
            if (run.getProjectId() != null) {
                variables.put(VAR_PROJECT_NAME, "IPD-Project-" + run.getProjectId());
            }
            createReq.setVariables(variables);
            // 同步发起：type2 后至首个 receiveTask 即返 processInstanceId
            String processInstanceId = processInstanceApi.createProcessInstance(userId, createReq);

            run.setProcessInstanceId(processInstanceId);
            run.setStatus("RUNNING");
            run.setStartedAt(LocalDateTime.now());
            run.setCurrentStage("concept");
            flowRunMapper.updateById(run);
            commandService.markSuccess(cmd.commandId(),
                    "{\"processInstanceId\":\"" + processInstanceId + "\"}");
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("flowRunId", flowRunId);
            r.put("commandId", cmd.commandId());
            r.put("commandStatus", "SUCCESS");
            r.put("processInstanceId", processInstanceId);
            r.put("flowRunStatus", "RUNNING");
            r.put("businessKey", run.getBusinessKey());
            log.info("[start][flowRunId={} processInstanceId={} businessKey={}]",
                    flowRunId, processInstanceId, run.getBusinessKey());
            return r;
        } catch (Exception e) {
            commandService.markFailed(cmd.commandId(), "START_FAIL", e.getMessage());
            run.setStatus("FAILED");
            flowRunMapper.updateById(run);
            log.error("[start][flowRunId={} 启动失败 {}]", flowRunId, e.getMessage());
            throw exception(IPD_FLOW_START_FAIL);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public SpkIpdFlowRunDO cancel(Long flowRunId, String reason) {
        SpkIpdFlowRunDO run = getFlowRunOrThrow(flowRunId);
        if ("COMPLETED".equals(run.getStatus()) || "CANCELLED".equals(run.getStatus())) {
            throw exception(IPD_FLOW_RUN_NOT_CANCELLABLE);
        }
        // 取消业务流并调用引擎（若已绑定实例）
        if (run.getProcessInstanceId() != null) {
            try {
                historyService.createHistoricProcessInstanceQuery()
                        .processInstanceId(run.getProcessInstanceId()).singleResult();
                // 引擎侧取消由 BPM API 负责；S1 仅置业务态，引擎实例取消延后到 S2 BPM adapter
            } catch (Exception e) {
                log.warn("[cancel][引擎实例查询失败 pid={} {}]", run.getProcessInstanceId(), e.getMessage());
            }
        }
        run.setStatus("CANCELLED");
        run.setBlockReason(reason);
        run.setEndedAt(LocalDateTime.now());
        flowRunMapper.updateById(run);
        return run;
    }

    public SpkIpdFlowRunDO getFlowRun(Long flowRunId) {
        return getFlowRunOrThrow(flowRunId);
    }

    /** 按引擎实例反查 FlowRun（供旧路由/回调兼容） */
    public SpkIpdFlowRunDO getByProcessInstanceId(String processInstanceId) {
        return flowRunMapper.selectByProcessInstanceId(processInstanceId);
    }

    public PageResult<SpkIpdFlowRunDO> page(SpkIpdFlowRunPageReqVO req) {
        return flowRunMapper.selectPage(req);
    }

    /**
     * 时间线：合并 BPM 活动、FlowRun 状态。设计文档 section 10.5 /timeline。
     * S1 返回 Flowable 历史活动 + FlowRun 摘要；Agent/Gate/PR/CI 事件在 S3/S5 接入后合并。
     */
    public Map<String, Object> timeline(Long flowRunId) {
        SpkIpdFlowRunDO run = getFlowRunOrThrow(flowRunId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("flowRunId", run.getId());
        result.put("runNo", run.getRunNo());
        result.put("businessKey", run.getBusinessKey());
        result.put("status", run.getStatus());
        result.put("currentStage", run.getCurrentStage());
        result.put("health", run.getHealth());
        result.put("processInstanceId", run.getProcessInstanceId());

        List<Map<String, Object>> events = new ArrayList<>();
        if (run.getProcessInstanceId() != null) {
            List<HistoricActivityInstance> acts = historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(run.getProcessInstanceId())
                    .orderByHistoricActivityInstanceStartTime().asc()
                    .list();
            for (HistoricActivityInstance a : acts) {
                Map<String, Object> ev = new LinkedHashMap<>();
                ev.put("source", "BPM");
                ev.put("activityId", a.getActivityId());
                ev.put("activityName", a.getActivityName());
                ev.put("activityType", a.getActivityType());
                ev.put("startTime", a.getStartTime() == null ? null
                        : LocalDateTime.ofInstant(a.getStartTime().toInstant(), java.time.ZoneId.systemDefault()));
                ev.put("endTime", a.getEndTime() == null ? null
                        : LocalDateTime.ofInstant(a.getEndTime().toInstant(), java.time.ZoneId.systemDefault()));
                ev.put("assignee", a.getAssignee());
                events.add(ev);
            }
        }
        result.put("events", events);
        result.put("generatedAt", LocalDateTime.now());
        return result;
    }

    public SpkIpdCommandLogDO getCommand(Long commandId) {
        return commandService.get(commandId);
    }

    // ==================== 辅助 ====================

    private SpkIpdFlowRunDO getFlowRunOrThrow(Long flowRunId) {
        SpkIpdFlowRunDO r = flowRunMapper.selectById(flowRunId);
        if (r == null) {
            throw exception(IPD_FLOW_RUN_NOT_EXISTS);
        }
        return r;
    }

    private String minimalProfileSnapshot(SpkIpdFlowRunPreflightReqVO req) {
        Map<String, Object> snap = new LinkedHashMap<>();
        snap.put("processDefinitionKey", IPD_FLOW_KEY);
        snap.put("flowType", req.getFlowType());
        snap.put("profileVersion", 1);
        snap.put("note", "S1 默认档案；S2 接入完整 ProcessProfile 后替换");
        return writeJson(snap);
    }

    private String tailoringJson(SpkIpdFlowRunPreflightReqVO.Tailoring t) {
        if (t == null) {
            return null;
        }
        return writeJson(t);
    }

    private String writeJson(Object o) {
        try {
            return json.writeValueAsString(o);
        } catch (Exception e) {
            return null;
        }
    }

    private Long currentUserId() {
        Long uid = SecurityFrameworkUtils.getLoginUserId();
        return uid != null ? uid : 1L;
    }
}


