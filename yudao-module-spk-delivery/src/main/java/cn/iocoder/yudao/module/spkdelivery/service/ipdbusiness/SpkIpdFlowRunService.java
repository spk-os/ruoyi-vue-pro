package cn.iocoder.yudao.module.spkdelivery.service.ipdbusiness;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.flowrun.SpkIpdFlowRunPageReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.flowrun.SpkIpdFlowRunPreflightReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.version.SpkIpdReadinessRespVO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdCommandLogDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdEngineInstanceDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdFailedJobDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdFlowRunDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdIssueCaseDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdProcessProfileDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdVersionDO;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness.SpkIpdEngineInstanceMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness.SpkIpdFailedJobMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness.SpkIpdFlowRunMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness.SpkIpdIssueCaseMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness.SpkIpdVersionMapper;
import cn.iocoder.yudao.module.spkdelivery.enums.SpkActivityStageEnum;
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
    private SpkIpdProcessProfileService processProfileService;
    @Resource
    private HistoryService historyService;
    @Resource
    private SpkStageResolver stageResolver;
    // Phase2 I：跨迭代产物追溯与回滚——读 manifest 快照重置 current_stage
    @Resource
    private cn.iocoder.yudao.module.spkdelivery.service.delivery.DeliveryPathResolver deliveryPathResolver;
    @Resource
    private cn.iocoder.yudao.module.spkdelivery.service.delivery.FlowStateWriter flowStateWriter;
    // D4：引擎实例档案 / 失败作业真实回写（修 G4/G5，此前全模块零写入致运行统计页永远空）
    @Resource
    private SpkIpdEngineInstanceMapper engineInstanceMapper;
    @Resource
    private SpkIpdFailedJobMapper failedJobMapper;
    private final ObjectMapper json = new ObjectMapper();
    // 默认 skill 环境（spk-delivery.skill.default-env）：start 未显式传 skillEnv 时归一到此值，
    // 让配置 default-env=test 在 e2e 真正生效（而非硬编码 default）。route 内 resolveSkillEnv 读流程变量解析。
    @org.springframework.beans.factory.annotation.Value("${spk-delivery.skill.default-env:default}")
    private String defaultSkillEnv;

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
     * Profile 档案引用 ProcessProfile 当前已发布版本（§7.2 第 4 顶层表面）：
     * - 命中已发布 Profile → 写 profileId/profileVersion/snapshot（真实治理产物）。
     * - 无已发布 Profile → 降级用最小档案（profileId=0/profileVersion=1），不阻断创建，前端按 profileId=0 标注"未接入治理"。
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
        // 解析当前已发布 Profile（治理产物，管理员在流程治理配置后生效）
        SpkIpdProcessProfileService.PublishedProfile pub = resolvePublishedProfile(req.getFlowType());
        SpkIpdFlowRunDO run = SpkIpdFlowRunDO.builder()
                .projectId(req.getProjectId())
                .majorReleaseId(req.getVersionId() == null ? null
                        : versionMapper.selectById(req.getVersionId()).getMajorReleaseId())
                .versionId(req.getVersionId())
                .issueCaseId(req.getIssueCaseId())
                .flowType(req.getFlowType())
                .profileId(pub == null ? 0L : pub.profileId())
                .profileVersion(pub == null ? 1 : pub.version())
                .profileSnapshotJson(pub == null ? minimalProfileSnapshot(req) : pub.snapshotJson())
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
        return start(flowRunId, idempotencyKey, null, null);
    }

    /**
     * 幂等启动 Flowable，支持启动时选择运行模式（test/product）与 skill 环境
     * （default/test/commercial-release/prototype-release），二者写入流程变量 spk_mode / spk_skill_env，
     * route 内只读解析（与 [[flowable-sync-trigger-deadlock]] 铁律一致：仅 setVariables 于 createProcessInstance 同步路径，
     * 不在触发器回调内 set）。空/null 归一：mode→test，skillEnv→default。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> start(Long flowRunId, String idempotencyKey, String mode, String skillEnv) {
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
            // D1：按 flowType 经治理层解析已发布 Profile，取固化 key + versionId（修 G1——三种 flowType 各走对应真实 BPM 流程）
            // 治理层未发布 Profile 时降级用 flowKeyOf(flowType) 直取默认 key，不阻断启动。
            SpkIpdProcessProfileService.PublishedProfile pub = resolvePublishedProfile(run.getFlowType());
            String flowKey = (pub != null && pub.processDefinitionKey() != null)
                    ? pub.processDefinitionKey() : flowKeyOf(run.getFlowType());
            BpmProcessInstanceCreateReqDTO createReq = new BpmProcessInstanceCreateReqDTO();
            createReq.setProcessDefinitionKey(flowKey);
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
            // spk_mode / spk_skill_env：启动时选择（端点 query 参传入）。route 内只读 getVariable 解析。
            //   mode：test=桩仅测试 / product=真实交付（buildPrompt 按此分叉）；空归 test。
            //   skillEnv：决定用哪套 skill 文件；空归 default（与 spk-delivery.skill.default-env 配置一致）。
            //   二者正交：mode 决定 prompt 轻量化，env 决定 skill 文件分区。
            String normMode = (mode == null || mode.isBlank()) ? "test" : mode.trim().toLowerCase();
            variables.put("spk_mode", normMode);
            // skillEnv：决定用哪套 skill 文件；空/null 归一到配置 spk-delivery.skill.default-env（e2e 设 test
            // 即生效），而非硬编码 default——让 default-env 配置真正驱动 E2E 走 test skill。
            String normEnv = (skillEnv == null || skillEnv.isBlank()) ? defaultSkillEnv : skillEnv.trim();
            variables.put("spk_skill_env", normEnv);
            createReq.setVariables(variables);
            // 同步发起：type2 后至首个 receiveTask 即返 processInstanceId
            String processInstanceId = processInstanceApi.createProcessInstance(userId, createReq);

            run.setProcessInstanceId(processInstanceId);
            run.setStatus("RUNNING");
            run.setStartedAt(LocalDateTime.now());
            run.setCurrentStage("concept");
            flowRunMapper.updateById(run);
            // Phase2 E/H：治理 FlowRun 启动时初始化交付目录骨架（.flow/ asset/ src/ docs/ + project.yaml + manifest.json）。
            // 优先用 Project DO.delivery_root（plan §F：LaunchWizard 选根目录落库），否则按 businessKey 渲染 Profile 默认根。
            // 失败降级记 warn 不阻断流程发起（DB 是关键路径，FS 是增强）；路径安全违抛 IPD_DELIVERY_ROOT_INVALID（坑#路径注入铁律不降级）。
            String deliveryRoot = null;
            try {
                cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdProjectDO project =
                        run.getProjectId() == null ? null : projectBusinessService.getProject(run.getProjectId());
                deliveryRoot = deliveryPathResolver.resolveProjectRoot(project, run.getBusinessKey());
                if (deliveryRoot != null) {
                    String projectName = project != null && project.getName() != null
                            ? project.getName() : "IPD-Project-" + run.getProjectId();
                    flowStateWriter.provisionProject(run.getBusinessKey(), deliveryRoot,
                            projectName, flowKey, "product");
                }
            } catch (IllegalArgumentException ie) {
                throw cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception(
                        cn.iocoder.yudao.module.spkdelivery.enums.ErrorCodeConstants.IPD_DELIVERY_ROOT_INVALID);
            } catch (Exception e) {
                log.warn("[start][flowRunId={} 交付目录初始化失败降级 root={}：{}]",
                        flowRunId, deliveryRoot, e.getMessage());
            }
            // D4：写引擎实例档案（修 G4——此前全模块零写入致运行统计页永远空）
            writeEngineInstance(run, processInstanceId, flowKey, pub);
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
            // D4：启动失败写 failed_job 档案（修 G5——此前失败作业无登记）
            writeFailedJob("FLOW_START", flowRunId, null, "START_FAIL", e.getMessage());
            log.error("[start][flowRunId={} 启动失败 {}]", flowRunId, e.getMessage());
            throw exception(IPD_FLOW_START_FAIL);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public SpkIpdFlowRunDO cancel(Long flowRunId, String reason, String idempotencyKey) {
        if (reason == null || reason.isBlank()) {
            throw exception(IPD_FLOW_RUN_REASON_REQUIRED);
        }
        SpkIpdFlowRunDO run = getFlowRunOrThrow(flowRunId);
        // 幂等登记：同 key 重复取消返回当前态（审计 + 防重复点击并发）
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("flowRunId", flowRunId);
        payload.put("reason", reason);
        SpkIpdCommandService.CommandEnvelope cmd = commandService.enlist(
                idempotencyKey, CMD_CANCEL_FLOW, "FLOW_RUN", String.valueOf(flowRunId), writeJson(payload));
        if (!cmd.isNew()) {
            return run;
        }
        commandService.markRunning(cmd.commandId());
        try {
            // 状态机：终态（COMPLETED/CANCELLED/FAILED/SUPERSEDED）不可取消
            if (STATUS_COMPLETED.equals(run.getStatus()) || STATUS_CANCELLED.equals(run.getStatus())
                    || STATUS_FAILED.equals(run.getStatus()) || STATUS_SUPERSEDED.equals(run.getStatus())) {
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
            run.setStatus(STATUS_CANCELLED);
            run.setBlockReason(reason);
            run.setEndedAt(LocalDateTime.now());
            flowRunMapper.updateById(run);
            commandService.markSuccess(cmd.commandId(),
                    "{\"status\":\"CANCELLED\",\"flowRunId\":" + flowRunId + "}");
            return run;
        } catch (Exception e) {
            commandService.markFailed(cmd.commandId(), "CANCEL_FAIL", e.getMessage());
            throw e;
        }
    }

    /**
     * 人工阻断：置 BLOCKED 并记理由，<b>不取消引擎实例</b>（区别于 cancel）。
     * 设计文档 §6.4 状态机：仅 RUNNING/WAITING_APPROVAL/STARTING/READY 可阻断；终态与 BLOCKED/DRAFT 拒绝。
     * 幂等（Idempotency-Key + CommandLog 审计），理由必填。
     */
    @Transactional(rollbackFor = Exception.class)
    public SpkIpdFlowRunDO block(Long flowRunId, String reason, String idempotencyKey) {
        if (reason == null || reason.isBlank()) {
            throw exception(IPD_FLOW_RUN_REASON_REQUIRED);
        }
        SpkIpdFlowRunDO run = getFlowRunOrThrow(flowRunId);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("flowRunId", flowRunId);
        payload.put("reason", reason);
        SpkIpdCommandService.CommandEnvelope cmd = commandService.enlist(
                idempotencyKey, CMD_BLOCK_FLOW, "FLOW_RUN", String.valueOf(flowRunId), writeJson(payload));
        if (!cmd.isNew()) {
            return run;
        }
        commandService.markRunning(cmd.commandId());
        try {
            if (STATUS_COMPLETED.equals(run.getStatus()) || STATUS_CANCELLED.equals(run.getStatus())
                    || STATUS_FAILED.equals(run.getStatus()) || STATUS_SUPERSEDED.equals(run.getStatus())
                    || STATUS_BLOCKED.equals(run.getStatus()) || STATUS_DRAFT.equals(run.getStatus())) {
                throw exception(IPD_FLOW_RUN_NOT_BLOCKABLE);
            }
            run.setStatus(STATUS_BLOCKED);
            run.setBlockReason("BLOCK:" + reason);
            flowRunMapper.updateById(run);
            commandService.markSuccess(cmd.commandId(),
                    "{\"status\":\"BLOCKED\",\"flowRunId\":" + flowRunId + "}");
            log.info("[block][flowRunId={} reason={}]", flowRunId, reason);
            return run;
        } catch (Exception e) {
            commandService.markFailed(cmd.commandId(), "BLOCK_FAIL", e.getMessage());
            throw e;
        }
    }

    /**
     * 解除阻断：BLOCKED → RUNNING，清空当前 blockReason。
     * 设计文档 §6.4：仅 BLOCKED 可解除。幂等 + 理由必填（审计可追溯）。
     */
    @Transactional(rollbackFor = Exception.class)
    public SpkIpdFlowRunDO unblock(Long flowRunId, String reason, String idempotencyKey) {
        if (reason == null || reason.isBlank()) {
            throw exception(IPD_FLOW_RUN_REASON_REQUIRED);
        }
        SpkIpdFlowRunDO run = getFlowRunOrThrow(flowRunId);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("flowRunId", flowRunId);
        payload.put("reason", reason);
        SpkIpdCommandService.CommandEnvelope cmd = commandService.enlist(
                idempotencyKey, CMD_UNBLOCK_FLOW, "FLOW_RUN", String.valueOf(flowRunId), writeJson(payload));
        if (!cmd.isNew()) {
            return run;
        }
        commandService.markRunning(cmd.commandId());
        try {
            if (!STATUS_BLOCKED.equals(run.getStatus())) {
                throw exception(IPD_FLOW_RUN_NOT_UNBLOCKABLE);
            }
            run.setStatus(STATUS_RUNNING);
            run.setBlockReason(null);
            flowRunMapper.updateById(run);
            commandService.markSuccess(cmd.commandId(),
                    "{\"status\":\"RUNNING\",\"flowRunId\":" + flowRunId + "}");
            log.info("[unblock][flowRunId={} reason={}]", flowRunId, reason);
            return run;
        } catch (Exception e) {
            commandService.markFailed(cmd.commandId(), "UNBLOCK_FAIL", e.getMessage());
            throw e;
        }
    }

    /**
     * 重试：仅 FAILED 可重试。创建新 attempt（新 FlowRun 行，attemptNo+1，supersedesFlowRunId 指向旧），
     * 旧行置 SUPERSEDED。<b>新行为 DRAFT，需再调 start() 启动</b>（不在互锁事务内写 Flowable）。
     * 设计文档 §6.4 / §11.4：retry 走 attempt 模型，不覆盖旧证据/旧产物。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> retry(Long flowRunId, String idempotencyKey) {
        SpkIpdFlowRunDO old = getFlowRunOrThrow(flowRunId);
        if (!STATUS_FAILED.equals(old.getStatus())) {
            throw exception(IPD_FLOW_RUN_NOT_RETRYABLE);
        }
        String payloadJson = "{\"flowRunId\":" + flowRunId + "}";
        SpkIpdCommandService.CommandEnvelope cmd = commandService.enlist(
                idempotencyKey, CMD_RETRY_FLOW, "FLOW_RUN", String.valueOf(flowRunId), payloadJson);
        Map<String, Object> r = new LinkedHashMap<>();
        if (!cmd.isNew()) {
            r.put("flowRunId", flowRunId);
            r.put("commandId", cmd.commandId());
            r.put("commandStatus", cmd.log().getStatus());
            r.put("idempotent", true);
            return r;
        }
        commandService.markRunning(cmd.commandId());
        try {
            // 旧行置 SUPERSEDED
            old.setStatus(STATUS_SUPERSEDED);
            flowRunMapper.updateById(old);
            // 新 attempt：复制业务上下文，attemptNo+1，supersedes 指向旧
            SpkIpdFlowRunDO fresh = SpkIpdFlowRunDO.builder()
                    .projectId(old.getProjectId())
                    .majorReleaseId(old.getMajorReleaseId())
                    .versionId(old.getVersionId())
                    .issueCaseId(old.getIssueCaseId())
                    .flowType(old.getFlowType())
                    .profileId(old.getProfileId())
                    .profileVersion(old.getProfileVersion())
                    .profileSnapshotJson(old.getProfileSnapshotJson())
                    .tailoringSnapshotJson(old.getTailoringSnapshotJson())
                    .status(STATUS_DRAFT)
                    .health(HEALTH_UNKNOWN)
                    .attemptNo((old.getAttemptNo() == null ? 0 : old.getAttemptNo()) + 1)
                    .supersedesFlowRunId(old.getId())
                    .plannedStartAt(old.getPlannedStartAt())
                    .plannedEndAt(old.getPlannedEndAt())
                    .build();
            String tmpNo = "FR-TMP-" + System.nanoTime();
            fresh.setRunNo(tmpNo);
            fresh.setBusinessKey("IPD:" + tmpNo);
            flowRunMapper.insert(fresh);
            fresh.setRunNo(runNo(fresh.getId()));
            fresh.setBusinessKey(businessKey(fresh.getRunNo()));
            flowRunMapper.updateById(fresh);
            commandService.markSuccess(cmd.commandId(),
                    "{\"newFlowRunId\":" + fresh.getId() + "}");
            r.put("flowRunId", fresh.getId());
            r.put("supersededFlowRunId", old.getId());
            r.put("commandId", cmd.commandId());
            r.put("commandStatus", "SUCCESS");
            r.put("attemptNo", fresh.getAttemptNo());
            r.put("flowRunStatus", STATUS_DRAFT);
            log.info("[retry][old={} new={} attemptNo={}]", old.getId(), fresh.getId(), fresh.getAttemptNo());
            return r;
        } catch (Exception e) {
            commandService.markFailed(cmd.commandId(), "RETRY_FAIL", e.getMessage());
            throw e;
        }
    }

    public SpkIpdFlowRunDO getFlowRun(Long flowRunId) {
        SpkIpdFlowRunDO run = getFlowRunOrThrow(flowRunId);
        assertFlowRunAccess(run);
        enrichCurrent(run);
        return run;
    }

    /**
     * Phase2 I：跨迭代回滚——按 (projectId, majorReleaseId, versionId) 定位该迭代 FlowRun 的
     * .flow/manifest 快照，恢复 current_stage/current_activity 到 manifest 记录的最新阶段，并置 BLOCKED
     * 待人工重执行（区别于 retry 的新 attempt 模型：回滚不新建行，原地重置到迭代快照阶段，保留旧证据/产物）。
     * <p>设计文档 §I：manifest 维护跨迭代索引（pid/flowRunId/flowType/迭代号/状态/产物指针/evidence 指针），
     * 可还原任一历史迭代全流程状态。失败抛业务异常，不静默。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> rollbackToIteration(Long projectId, Long majorReleaseId, Long versionId) {
        if (projectId == null || versionId == null) {
            throw exception(IPD_ITERATION_NOT_FOUND);
        }
        // 定位该迭代最新一条 FlowRun（按 id 倒序）
        List<SpkIpdFlowRunDO> runs = flowRunMapper.selectList(
                new cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX<SpkIpdFlowRunDO>()
                        .eq(SpkIpdFlowRunDO::getProjectId, projectId)
                        .eq(SpkIpdFlowRunDO::getVersionId, versionId)
                        .eq(majorReleaseId != null, SpkIpdFlowRunDO::getMajorReleaseId, majorReleaseId)
                        .orderByDesc(SpkIpdFlowRunDO::getId));
        SpkIpdFlowRunDO run = (runs == null || runs.isEmpty()) ? null : runs.get(0);
        if (run == null) {
            throw exception(IPD_ITERATION_NOT_FOUND);
        }
        // 仅 FAILED/BLOCKED/CANCELLED/SUPERSEDED 可回滚（RUNNING 不可原地重置，DRAFT 无快照）
        String st = run.getStatus();
        if (!STATUS_FAILED.equals(st) && !STATUS_BLOCKED.equals(st)
                && !STATUS_CANCELLED.equals(st) && !STATUS_SUPERSEDED.equals(st)) {
            throw exception(IPD_ITERATION_NOT_ROLLBACKABLE);
        }
        // 读 manifest 快照定位该 pid 的最新阶段
        String root = deliveryPathResolver.resolveProjectRootByBusinessKey(
                run.getProcessInstanceId(), run.getBusinessKey());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("flowRunId", run.getId());
        result.put("runNo", run.getRunNo());
        result.put("beforeStage", run.getCurrentStage());
        result.put("beforeStatus", st);
        String restoreStage = run.getCurrentStage();
        String restoreActivity = run.getCurrentActivity();
        if (root != null) {
            Map<String, Object> manifest = flowStateWriter.readManifest(root);
            Object flowRunsObj = manifest.get("flowRuns");
            if (flowRunsObj instanceof List<?> list) {
                for (Object o : list) {
                    if (!(o instanceof Map<?, ?> fr)) {
                        continue;
                    }
                    if (run.getProcessInstanceId() != null
                            && run.getProcessInstanceId().equals(fr.get("pid"))) {
                        Object cs = fr.get("currentStage");
                        Object ca = fr.get("currentActivity");
                        if (cs != null) {
                            restoreStage = cs.toString();
                        }
                        if (ca != null) {
                            restoreActivity = ca.toString();
                        }
                        break;
                    }
                }
            }
        }
        if (restoreStage == null) {
            // 既无 manifest 快照也无 DO 静态阶段，无法恢复
            throw exception(IPD_ITERATION_ROLLBACK_NO_SNAPSHOT);
        }
        // 原地重置：current_stage/activity 回到 manifest 快照，置 BLOCKED 待重执行，清 endedAt
        run.setCurrentStage(restoreStage);
        run.setCurrentActivity(restoreActivity);
        run.setStatus(STATUS_BLOCKED);
        run.setBlockReason("rollback-to-iteration:" + (run.getVersionId() == null ? "?" : run.getVersionId()));
        run.setEndedAt(null);
        flowRunMapper.updateById(run);
        result.put("afterStage", restoreStage);
        result.put("afterActivity", restoreActivity);
        result.put("afterStatus", STATUS_BLOCKED);
        result.put("deliveryRoot", root);
        log.info("[rollbackToIteration][flowRunId={} {}→{} reset stage {}]",
                run.getId(), st, STATUS_BLOCKED, restoreStage);
        return result;
    }

    /**
     * 实时覆盖 DO 的 currentStage/currentActivity：历史遗留 DO 仅 start 置 "concept" 后无回写点，
     * 从 task_contract 取最新活动 phase/activityId 覆盖；COMPLETED → lifecycle。不碰引擎，纯读。
     */
    private void enrichCurrent(SpkIpdFlowRunDO run) {
        if (run == null) {
            return;
        }
        run.setCurrentStage(stageResolver.resolveCurrentStage(
                run.getProcessInstanceId(), run.getCurrentStage(), run.getStatus()));
        run.setCurrentActivity(stageResolver.resolveCurrentActivity(
                run.getProcessInstanceId(), run.getCurrentActivity()));
    }

    /**
     * 归属校验（P0 §6）：当前登录用户须为 FlowRun 所属项目的 owner；admin/system 兜底用户绕过。
     * 旧数据无 projectId 或项目未设 owner 时不阻断，避免误拦（多成员表留 S2）。
     */
    public void assertFlowRunAccess(SpkIpdFlowRunDO run) {
        if (run == null) {
            return;
        }
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        // 未登录或 system 兜底用户（与 start 的 1L 兜底对齐）由 RBAC 层兜底，不二次拦截
        if (userId == null || userId == 1L) {
            return;
        }
        Long pid = run.getProjectId();
        if (pid == null) {
            return; // 旧数据无项目归属，不阻断读取
        }
        var proj = projectBusinessService.getProject(pid);
        if (proj == null) {
            return; // 项目不存在由其它校验处理
        }
        Long owner = proj.getOwnerUserId();
        if (owner != null && !owner.equals(userId)) {
            throw exception(IPD_FLOW_RUN_ACCESS_DENIED);
        }
    }

    /** 按引擎实例反查 FlowRun（供旧路由/回调兼容） */
    public SpkIpdFlowRunDO getByProcessInstanceId(String processInstanceId) {
        return flowRunMapper.selectByProcessInstanceId(processInstanceId);
    }

    /**
     * 流程正常结束（BpmProcessInstanceStatusEnum.APPROVE）回写 FlowRun 终态：COMPLETED + endedAt。
     * <p>
     * 由 {@code SpkIpdFlowFinishListener} 在流程实例 APPROVE 事件中回调，镜像
     * {@code BpmOALeaveStatusListener.onEvent → leaveService.updateLeaveStatus} 模式。
     * <p>
     * P0 补救：此前 {@code start} 置 RUNNING 后无人再置 COMPLETED，导致流程跑完 FlowRun 永远 RUNNING
     * （驾驶舱显示错误、C-14 违规）。本方法闭合状态机至终态。
     * <b>终态保护</b>：已 COMPLETED/CANCELLED/FAILED/SUPERSEDED 的不重复置位（人工取消/失败优先）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void markCompletedByInstance(String processInstanceId) {
        if (processInstanceId == null || processInstanceId.isBlank()) {
            return;
        }
        SpkIpdFlowRunDO run = flowRunMapper.selectByProcessInstanceId(processInstanceId);
        if (run == null) {
            log.warn("[markCompletedByInstance][未找到 processInstanceId={} 的 FlowRun]", processInstanceId);
            return;
        }
        if (STATUS_COMPLETED.equals(run.getStatus()) || STATUS_CANCELLED.equals(run.getStatus())
                || STATUS_FAILED.equals(run.getStatus()) || STATUS_SUPERSEDED.equals(run.getStatus())) {
            return;
        }
        run.setStatus(STATUS_COMPLETED);
        run.setEndedAt(LocalDateTime.now());
        // Bug2-B：终态回写 current_stage=lifecycle（start 置 concept 后无推进回写点，DO 静态值
        // 永停 concept；中间阶段靠读端 enrichCurrent 实时算覆盖，终态在此落库快照）
        run.setCurrentStage(SpkActivityStageEnum.LIFECYCLE.getCode());
        flowRunMapper.updateById(run);
        log.info("[markCompletedByInstance][flowRunId={} processInstanceId={} → COMPLETED]",
                run.getId(), processInstanceId);
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
        // Bug2-A：currentStage/currentActivity 实时算（DO 静态值永停 concept，读端覆盖）
        result.put("currentStage", stageResolver.resolveCurrentStage(
                run.getProcessInstanceId(), run.getCurrentStage(), run.getStatus()));
        result.put("currentActivity", stageResolver.resolveCurrentActivity(
                run.getProcessInstanceId(), run.getCurrentActivity()));
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
        // D1：降级快照也按 flowType 写 key（修 G1——此前三种 flowType 都写 IPD_FLOW_KEY）
        snap.put("processDefinitionKey", flowKeyOf(req.getFlowType()));
        snap.put("flowType", req.getFlowType());
        snap.put("profileVersion", 1);
        snap.put("note", "未匹配已发布 ProcessProfile，使用最小降级档案；请在流程治理配置发布后生效");
        return writeJson(snap);
    }

    /**
     * 解析 flowType 对应的当前已发布 Profile；无则返回 null（降级，不阻断创建）。
     */
    private SpkIpdProcessProfileService.PublishedProfile resolvePublishedProfile(String flowType) {
        if (flowType == null || flowType.isBlank()) {
            return null;
        }
        try {
            return processProfileService.getPublishedForFlowType(flowType);
        } catch (Exception e) {
            log.warn("[resolvePublishedProfile][flowType={} 无已发布 Profile，降级最小档案：{}]", flowType, e.getMessage());
            return null;
        }
    }

    /**
     * D4：写引擎实例档案（修 G4）。启动成功后登记 processInstanceId↔flowRunId↔profileVersionId。
     * 写入失败不阻断主流程（仅告警），引擎实例是运维统计产物，非业务强一致数据。
     */
    private void writeEngineInstance(SpkIpdFlowRunDO run, String processInstanceId, String flowKey,
                                     SpkIpdProcessProfileService.PublishedProfile pub) {
        try {
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("flowKey", flowKey);
            meta.put("flowType", run.getFlowType());
            if (run.getProfileId() != null) meta.put("profileId", run.getProfileId());
            if (run.getProfileVersion() != null) meta.put("profileVersionNo", run.getProfileVersion());
            SpkIpdEngineInstanceDO ei = SpkIpdEngineInstanceDO.builder()
                    .processInstanceId(processInstanceId)
                    .flowRunId(run.getId())
                    .profileVersionId(pub == null ? null : pub.versionId())
                    .engineHealth("HEALTHY")
                    .lastSyncedAt(LocalDateTime.now())
                    .metaJson(writeJson(meta))
                    .lockVersion(0)
                    .build();
            engineInstanceMapper.insert(ei);
        } catch (Exception e) {
            log.warn("[writeEngineInstance][flowRunId={} 写引擎实例档案失败：{}]", run.getId(), e.getMessage());
        }
    }

    /**
     * D4：写失败作业档案（修 G5）。启动/触发器入口失败登记，供运行统计页失败作业区展示。
     * refId 存 flowRunId（failed_job 表无独立 processInstanceId 列，processInstanceId 已在 engine_instance 登记）。
     */
    private void writeFailedJob(String jobType, Long flowRunId, String processInstanceId,
                                String reasonCode, String reason) {
        try {
            String reasonText = reasonCode + (reason == null || reason.isBlank() ? "" : ": " + reason);
            SpkIpdFailedJobDO job = SpkIpdFailedJobDO.builder()
                    .jobType(jobType)
                    .refId(flowRunId)
                    .reason(reasonText.substring(0, Math.min(reasonText.length(), 500)))
                    .retryCount(0)
                    .status("FAILED")
                    .lockVersion(0)
                    .build();
            failedJobMapper.insert(job);
        } catch (Exception e) {
            log.warn("[writeFailedJob][jobType={} flowRunId={} 写失败作业档案失败：{}]",
                    jobType, flowRunId, e.getMessage());
        }
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


