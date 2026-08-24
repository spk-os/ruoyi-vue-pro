package cn.iocoder.yudao.module.spkdelivery.service.ipdbusiness;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskApproveReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskPageReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskReturnReqVO;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.approval.SpkIpdApprovalTaskPageReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.approval.SpkIpdApprovalTaskRespVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.approval.SpkIpdDecisionPackageRespVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.approval.SpkIpdDecisionReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.approval.SpkIpdEvidenceWaiverReqVO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.artifact.SpkArtifactManifestDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.aegis.SpkAegisReviewDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ccb.SpkCcbRecordDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.dcp.SpkDcpRedirectLogDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.evidence.SpkEvidenceRecordDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.gate.SpkGateRecordDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdDecisionRecordDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdEvidenceWaiverDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdFlowRunDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdMajorReleaseDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdProjectDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdVersionDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.taskcontract.SpkTaskContractDO;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.artifact.SpkArtifactManifestMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.aegis.SpkAegisReviewMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ccb.SpkCcbRecordMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.dcp.SpkDcpRedirectLogMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.evidence.SpkEvidenceRecordMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.gate.SpkGateRecordMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness.SpkIpdDecisionRecordMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness.SpkIpdEvidenceWaiverMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.taskcontract.SpkTaskContractMapper;
import cn.iocoder.yudao.module.spkdelivery.service.context.SpkContextBuilderService;
import cn.iocoder.yudao.module.spkdelivery.service.dcp.SpkDcpRedirectService;
import cn.iocoder.yudao.module.spkdelivery.service.evidence.SpkEvidenceService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.spkdelivery.enums.ErrorCodeConstants.*;

/**
 * IPD 审批与决策包服务实现（设计文档 §10.8 / §7.9）。
 * <p>
 * 复用原生 BPM 待办/已办与 approve/reject/return，仅做业务包装：
 * - 待办/已办分页：复用 {@link BpmTaskService#getTaskTodoPage} / {@link BpmTaskService#getTaskDonePage}，
 *   按 processInstanceId 回填 FlowRun/项目/版本/阶段/门禁/健康等业务摘要；
 * - 决策包：聚合证据/产物/门禁/CCB/DCP 重定向/历史决策/豁免，计算快照 hash，给出阻断项与候选动作；
 * - 决策：先写不可变 {@link SpkIpdDecisionRecordDO}；APPROVE/RETURN 复用原生 BPM，
 *   REJECT 按固定策略回到阶段自动节点，让流程重新触发 Agent 并再次提交审批。
 * <p>
 * 铁律：审批人不手工启动/接管 Agent；驳回必须自动返工，未知审批节点 fail-closed。
 *
 * @author SPK-OS
 */
@Service
@Validated
@Slf4j
public class SpkIpdApprovalServiceImpl implements SpkIpdApprovalService {

    @Resource
    private BpmTaskService bpmTaskService;
    @Resource
    private SpkIpdFlowRunService flowRunService;
    @Resource
    private SpkIpdProjectBusinessService projectBusinessService;
    @Resource
    private SpkEvidenceService evidenceService;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private SpkArtifactManifestMapper artifactManifestMapper;
    @Resource
    private SpkAegisReviewMapper aegisReviewMapper;
    @Resource
    private SpkEvidenceRecordMapper evidenceRecordMapper;
    @Resource
    private SpkGateRecordMapper gateRecordMapper;
    @Resource
    private SpkTaskContractMapper taskContractMapper;
    @Resource
    private SpkCcbRecordMapper ccbRecordMapper;
    @Resource
    private SpkDcpRedirectLogMapper dcpRedirectLogMapper;
    @Resource
    private SpkIpdDecisionRecordMapper decisionRecordMapper;
    @Resource
    private SpkIpdEvidenceWaiverMapper evidenceWaiverMapper;
    @Resource
    private SpkDcpRedirectService dcpRedirectService;

    @Override
    public PageResult<SpkIpdApprovalTaskRespVO> pageApprovalTasks(Long userId, SpkIpdApprovalTaskPageReqVO req) {
        BpmTaskPageReqVO bpmReq = new BpmTaskPageReqVO();
        bpmReq.setPageNo(req.getPageNo());
        bpmReq.setPageSize(req.getPageSize());
        bpmReq.setName(req.getName());
        bpmReq.setCategory(req.getCategory());
        // 默认覆盖三 flowType 流程（spkIpdFlowFull/Increment/Issue）。62dabe2e92 三 flowType 路由三
        // 独立 BPM 流程后，旧单 key spkIpdFlow 已废弃；若仍按旧单 key 过滤，CDCP/PDCP/ADCP/GA/LDCP
        // 等 userTask 审批门 todo 0 匹配 → approval-tasks 返空 → 慢测 hasTodo=false 不 approve
        // → receiveTask/userTask 永卡 → FlowRun 永卡 RUNNING/concept。前端可显式传 processDefinitionKey 覆盖。
        if (req.getProcessDefinitionKey() != null) {
            bpmReq.setProcessDefinitionKey(req.getProcessDefinitionKey());
        } else {
            bpmReq.setProcessDefinitionKeyIn(java.util.List.of(
                    SpkIpdBusinessConstants.IPD_FLOW_KEY_FULL,
                    SpkIpdBusinessConstants.IPD_FLOW_KEY_INCREMENT,
                    SpkIpdBusinessConstants.IPD_FLOW_KEY_ISSUE));
        }
        bpmReq.setStatus(req.getStatus());
        bpmReq.setCreateTime(req.getCreateTime());

        boolean done = "done".equalsIgnoreCase(req.getType());
        PageResult<? extends Object> page = done
                ? bpmTaskService.getTaskDonePage(userId, bpmReq)
                : bpmTaskService.getTaskTodoPage(userId, bpmReq);

        // 收集 processInstanceId → FlowRun
        Set<String> instanceIds = new LinkedHashSet<>();
        for (Object t : page.getList()) {
            instanceIds.add(done ? ((HistoricTaskInstance) t).getProcessInstanceId()
                    : ((Task) t).getProcessInstanceId());
        }
        Map<String, SpkIpdFlowRunDO> runMap = new LinkedHashMap<>();
        for (String pid : instanceIds) {
            SpkIpdFlowRunDO run = flowRunService.getByProcessInstanceId(pid);
        flowRunService.assertFlowRunAccess(run); // P0 §6 归属校验：仅项目 owner 可读
            if (run != null) {
                runMap.put(pid, run);
            }
        }
        // 批量取用户昵称
        Set<Long> userIds = new LinkedHashSet<>();
        for (Object t : page.getList()) {
            String as = done ? ((HistoricTaskInstance) t).getAssignee() : ((Task) t).getAssignee();
            Long uid = parseLong(as);
            if (uid != null) userIds.add(uid);
        }
        Map<Long, AdminUserRespDTO> userMap = userIds.isEmpty()
                ? Collections.emptyMap() : adminUserApi.getUserMap(userIds);

        List<SpkIpdApprovalTaskRespVO> list = new ArrayList<>(page.getList().size());
        for (Object t : page.getList()) {
            list.add(toRespVO(t, done, runMap, userMap));
        }
        return new PageResult<>(list, page.getTotal());
    }

    private SpkIpdApprovalTaskRespVO toRespVO(Object t, boolean done,
                                              Map<String, SpkIpdFlowRunDO> runMap,
                                              Map<Long, AdminUserRespDTO> userMap) {
        SpkIpdApprovalTaskRespVO vo = new SpkIpdApprovalTaskRespVO();
        String pid;
        if (done) {
            HistoricTaskInstance h = (HistoricTaskInstance) t;
            vo.setTaskId(h.getId());
            vo.setName(h.getName());
            vo.setTaskDefinitionKey(h.getTaskDefinitionKey());
            vo.setProcessInstanceId(h.getProcessInstanceId());
            vo.setProcessDefinitionId(h.getProcessDefinitionId());
            vo.setCreateTime(toLdt(h.getCreateTime()));
            vo.setEndTime(toLdt(h.getEndTime()));
            vo.setAssigneeUserId(parseLong(h.getAssignee()));
            vo.setOwnerUserId(parseLong(h.getOwner()));
            pid = h.getProcessInstanceId();
        } else {
            Task tk = (Task) t;
            vo.setTaskId(tk.getId());
            vo.setName(tk.getName());
            vo.setTaskDefinitionKey(tk.getTaskDefinitionKey());
            vo.setProcessInstanceId(tk.getProcessInstanceId());
            vo.setProcessDefinitionId(tk.getProcessDefinitionId());
            vo.setCreateTime(toLdt(tk.getCreateTime()));
            vo.setAssigneeUserId(parseLong(tk.getAssignee()));
            vo.setOwnerUserId(parseLong(tk.getOwner()));
            vo.setSuspended(tk.isSuspended());
            pid = tk.getProcessInstanceId();
        }
        if (vo.getAssigneeUserId() != null && userMap.containsKey(vo.getAssigneeUserId())) {
            vo.setAssigneeNickname(userMap.get(vo.getAssigneeUserId()).getNickname());
        }
        if (vo.getOwnerUserId() != null && userMap.containsKey(vo.getOwnerUserId())) {
            vo.setOwnerNickname(userMap.get(vo.getOwnerUserId()).getNickname());
        }
        SpkIpdFlowRunDO run = runMap.get(pid);
        if (run != null) {
            vo.setFlowRunId(run.getId());
            vo.setRunNo(run.getRunNo());
            vo.setFlowType(run.getFlowType());
            vo.setCurrentStage(run.getCurrentStage());
            vo.setHealth(run.getHealth());
            vo.setBusinessKey(run.getBusinessKey());
            vo.setProjectId(run.getProjectId());
            vo.setVersionId(run.getVersionId());
            if (run.getProjectId() != null) {
                SpkIpdProjectDO proj = projectBusinessService.getProject(run.getProjectId());
                if (proj != null) vo.setProjectName(proj.getName());
            }
            if (run.getVersionId() != null) {
                SpkIpdVersionDO ver = projectBusinessService.getVersion(run.getVersionId());
                if (ver != null) vo.setVersionLabel(ver.getVersionNo());
            }
            // 当前门禁：取该实例最新一条 GateRecord
            List<SpkGateRecordDO> gates = gateRecordMapper.selectListByInstanceId(pid);
            if (gates != null && !gates.isEmpty()) {
                vo.setCurrentGate(gates.get(0).getGate());
            }
            if (run.getStartedAt() != null) {
                vo.setWaitDurationMs(Duration.between(run.getStartedAt(), LocalDateTime.now()).toMillis());
            }
        }
        return vo;
    }

    private static Long parseLong(String s) {
        if (s == null || s.isEmpty()) return null;
        try { return Long.parseLong(s); } catch (NumberFormatException e) { return null; }
    }

    private static LocalDateTime toLdt(java.util.Date date) {
        if (date == null) return null;
        return LocalDateTime.ofInstant(date.toInstant(), java.time.ZoneId.systemDefault());
    }

    @Override
    public SpkIpdDecisionPackageRespVO getDecisionPackage(String taskId) {
        Task task = bpmTaskService.getTask(taskId);
        if (task == null) {
            throw exception(IPD_TASK_ALREADY_COMPLETED);
        }
        String pid = task.getProcessInstanceId();
        SpkIpdFlowRunDO run = flowRunService.getByProcessInstanceId(pid);
        flowRunService.assertFlowRunAccess(run); // P0 §6 归属校验：仅项目 owner 可读

        List<SpkGateRecordDO> gates = gateRecordMapper.selectListByInstanceId(pid);
        List<SpkEvidenceRecordDO> evidence = evidenceRecordMapper.selectListByProcessInstanceId(pid);
        List<SpkArtifactManifestDO> artifacts = artifactManifestMapper.selectListByProcessInstanceId(pid);
        List<SpkTaskContractDO> contracts = taskContractMapper.selectListByProcessInstanceId(pid);
        List<SpkCcbRecordDO> ccb = ccbRecordMapper.selectListByInstanceId(pid);
        List<SpkDcpRedirectLogDO> redirects = dcpRedirectLogMapper.selectListByInstanceId(pid);
        List<SpkIpdDecisionRecordDO> decisions = decisionRecordMapper.selectListByProcessInstanceId(pid);
        List<SpkIpdEvidenceWaiverDO> waivers = evidenceWaiverMapper.selectListByProcessInstanceId(pid);
        SpkAegisReviewDO latestAegis = aegisReviewMapper.selectByInstanceId(pid);

        // 证据哈希链校验：按 activityRunId 分组逐条 verifyChain
        boolean chainValid = true;
        Set<String> activityRunIds = evidence.stream().map(SpkEvidenceRecordDO::getActivityRunId)
                .filter(s -> s != null && !s.isEmpty()).collect(Collectors.toSet());
        for (String arid : activityRunIds) {
            if (!evidenceService.verifyChain(arid)) {
                chainValid = false;
                break;
            }
        }

        SpkIpdDecisionPackageRespVO pkg = new SpkIpdDecisionPackageRespVO();
        pkg.setGates(gates);
        pkg.setEvidence(evidence);
        pkg.setChainValid(chainValid);
        pkg.setArtifacts(artifacts);
        pkg.setCcb(ccb);
        pkg.setDcpRedirects(redirects);
        pkg.setDecisions(decisions);
        pkg.setWaivers(waivers);
        String approvalStage = resolveApprovalStage(task.getTaskDefinitionKey(),
                run != null ? run.getCurrentStage() : null);
        List<SpkArtifactManifestDO> currentStageArtifacts = selectCurrentStageArtifacts(
                artifacts, contracts, decisions, approvalStage);
        pkg.setRequiredArtifacts(buildRequiredArtifacts(currentStageArtifacts, gates, evidence, chainValid,
                approvalStage, run != null ? run.getFlowType() : null, latestAegis));
        pkg.setHeader(buildHeader(task, run, proj(run), ver(run), approvalStage));
        pkg.setSummary(buildSummary(run, proj(run), ver(run), artifacts, currentStageArtifacts,
                gates, ccb, redirects, approvalStage));
        pkg.setBlockingItems(buildBlockingItems(pkg.getRequiredArtifacts(), gates, chainValid, waivers));
        pkg.setCandidateActions(buildCandidateActions(pkg.getBlockingItems(), redirects));
        pkg.setDecisionPackageHash(computeHash(pkg));
        pkg.setBuiltAt(LocalDateTime.now().toString());
        return pkg;
    }

    private SpkIpdProjectDO proj(SpkIpdFlowRunDO run) {
        return run != null && run.getProjectId() != null ? projectBusinessService.getProject(run.getProjectId()) : null;
    }

    private SpkIpdVersionDO ver(SpkIpdFlowRunDO run) {
        return run != null && run.getVersionId() != null ? projectBusinessService.getVersion(run.getVersionId()) : null;
    }

    /**
     * 人审任务本身是审批阶段的权威来源。FlowRun.currentStage 由异步活动快照维护，
     * 在 receiveTask -> userTask 的边界可能仍是上一审批阶段；若直接使用会把 PDCP
     * 的 plan 产物误判成 concept 缺失。未知任务才回退 FlowRun，保持兼容且 fail-closed。
     */
    static String resolveApprovalStage(String taskDefinitionKey, String flowRunStage) {
        return SpkIpdReworkPolicy.resolve(taskDefinitionKey)
                .map(SpkIpdReworkPolicy.Route::stage)
                .orElse(flowRunStage);
    }

    private Map<String, Object> buildHeader(Task task, SpkIpdFlowRunDO run,
                                             SpkIpdProjectDO proj, SpkIpdVersionDO ver,
                                             String approvalStage) {
        Map<String, Object> h = new LinkedHashMap<>();
        h.put("taskId", task.getId());
        h.put("taskName", task.getName());
        h.put("taskDefinitionKey", task.getTaskDefinitionKey());
        h.put("processInstanceId", task.getProcessInstanceId());
        h.put("projectName", proj != null ? proj.getName() : null);
        h.put("projectId", run != null ? run.getProjectId() : null);
        h.put("versionLabel", ver != null ? ver.getVersionNo() : null);
        h.put("versionId", run != null ? run.getVersionId() : null);
        h.put("versionType", ver != null ? ver.getVersionType() : null);
        h.put("flowType", run != null ? run.getFlowType() : null);
        h.put("stage", approvalStage);
        // 当前门禁
        String gate = null;
        if (run != null) {
            List<SpkGateRecordDO> gs = gateRecordMapper.selectListByInstanceId(run.getProcessInstanceId());
            if (gs != null && !gs.isEmpty()) gate = gs.get(0).getGate();
        }
        h.put("currentGate", gate);
        h.put("health", run != null ? run.getHealth() : null);
        h.put("businessKey", run != null ? run.getBusinessKey() : null);
        // 提交人/当前审批人
        Long approver = parseLong(task.getAssignee());
        h.put("approverUserId", approver);
        if (approver != null) {
            AdminUserRespDTO u = adminUserApi.getUser(approver);
            if (u != null) h.put("approverNickname", u.getNickname());
        }
        h.put("plannedStartAt", ver != null ? ver.getPlannedStartAt() : null);
        h.put("plannedEndAt", ver != null ? ver.getPlannedEndAt() : null);
        if (run != null && run.getStartedAt() != null) {
            h.put("waitDurationMs", Duration.between(run.getStartedAt(), LocalDateTime.now()).toMillis());
        }
        return h;
    }

    /**
     * 只把当前审批轮次、当前阶段且已成功完成合同的产物作为审批必需产物。
     * <p>
     * 自动重试会为同一 Activity 产生新的 activityRunId；失败尝试的 draft 仍保留在
     * {@code pkg.artifacts} 与决策包 hash 中供审计，但不得污染当前审批。上一条不可变
     * 审批决策是新审批轮次的时间边界；开发阶段 fan-out 的多个 REQ 合同因此仍会全部保留。
     */
    static List<SpkArtifactManifestDO> selectCurrentStageArtifacts(
            List<SpkArtifactManifestDO> artifacts, List<SpkTaskContractDO> contracts,
            List<SpkIpdDecisionRecordDO> decisions, String currentStage) {
        if (artifacts == null || contracts == null || currentStage == null || currentStage.isBlank()) {
            return Collections.emptyList();
        }
        LocalDateTime cycleStartedAt = decisions == null ? null : decisions.stream()
                .map(SpkIpdDecisionRecordDO::getCreateTime)
                .filter(t -> t != null)
                .max(LocalDateTime::compareTo)
                .orElse(null);
        Set<String> completedRunIds = contracts.stream()
                .filter(c -> c.getActivityRunId() != null && !c.getActivityRunId().isBlank())
                .filter(c -> currentStage.equalsIgnoreCase(c.getPhase()))
                .filter(c -> "done".equalsIgnoreCase(c.getStatus()))
                .filter(c -> cycleStartedAt == null || (c.getQueuedAt() != null
                        && !c.getQueuedAt().isBefore(cycleStartedAt)))
                .map(SpkTaskContractDO::getActivityRunId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (completedRunIds.isEmpty()) {
            return Collections.emptyList();
        }
        return artifacts.stream()
                .filter(a -> completedRunIds.contains(a.getActivityRunId()))
                .collect(Collectors.toList());
    }

    static List<SpkIpdDecisionPackageRespVO.RequiredArtifact> buildRequiredArtifacts(
            List<SpkArtifactManifestDO> artifacts, List<SpkGateRecordDO> gates,
            List<SpkEvidenceRecordDO> evidence, boolean chainValid, String currentStage,
            String flowType, SpkAegisReviewDO latestAegis) {
        List<SpkIpdDecisionPackageRespVO.RequiredArtifact> list = new ArrayList<>();
        // 产物
        for (SpkArtifactManifestDO a : artifacts) {
            SpkIpdDecisionPackageRespVO.RequiredArtifact r = new SpkIpdDecisionPackageRespVO.RequiredArtifact();
            r.setRef(a.getArtifactId());
            r.setType("ARTIFACT");
            String st = a.getStatus();
            r.setStatus("READY".equalsIgnoreCase(st) || "SIGNED".equalsIgnoreCase(st) ? "PASS" : ("WARN".equalsIgnoreCase(st) ? "WARN" : "MISSING"));
            r.setSigned(a.getSignedBy() != null && !a.getSignedBy().isEmpty());
            r.setScanStatus(a.getScanStatus());
            r.setHashOk(a.getContentHash() != null && !a.getContentHash().isEmpty());
            r.setBlocking(!"PASS".equals(r.getStatus()) || (a.getScanStatus() != null && a.getScanStatus().contains("FAIL")));
            list.add(r);
        }
        // 流程已到人工审批但当前阶段没有成功合同产物时必须 fail-closed，不能仅凭历史产物放行。
        if (artifacts.isEmpty()) {
            SpkIpdDecisionPackageRespVO.RequiredArtifact r = new SpkIpdDecisionPackageRespVO.RequiredArtifact();
            r.setRef("current-stage-artifact:" + (currentStage == null ? "unknown" : currentStage));
            r.setType("ARTIFACT");
            r.setStatus("MISSING");
            r.setSigned(false);
            r.setHashOk(false);
            r.setBlocking(true);
            list.add(r);
        }
        // 门禁
        for (SpkGateRecordDO g : gates) {
            SpkIpdDecisionPackageRespVO.RequiredArtifact r = new SpkIpdDecisionPackageRespVO.RequiredArtifact();
            r.setRef(g.getGate());
            r.setType("GATE");
            r.setStatus(Boolean.TRUE.equals(g.getPass()) ? "PASS" : "FAIL");
            r.setBlocking(!"PASS".equals(r.getStatus()));
            list.add(r);
        }
        // 证据链
        if (!evidence.isEmpty()) {
            SpkIpdDecisionPackageRespVO.RequiredArtifact r = new SpkIpdDecisionPackageRespVO.RequiredArtifact();
            r.setRef("evidence-chain");
            r.setType("EVIDENCE");
            r.setStatus(chainValid ? "PASS" : "FAIL");
            r.setHashOk(chainValid);
            r.setBlocking(!chainValid);
            list.add(r);
        }
        // 商用完整/增量流程的 DCP 必须显式纳入当前阶段 Aegis 结论。仅有已签主产物
        // 不能掩盖 TR 语义复核的 ERROR/CONDITIONAL/FAIL；旧阶段或旧产物之前的审查也
        // 不能冒充当前轮次结果。
        if (requiresAegis(flowType, currentStage)) {
            SpkIpdDecisionPackageRespVO.RequiredArtifact r = new SpkIpdDecisionPackageRespVO.RequiredArtifact();
            boolean stageMatches = aegisMatchesStage(latestAegis, currentStage);
            LocalDateTime latestArtifactAt = artifacts.stream()
                    .map(SpkArtifactManifestDO::getCreateTime)
                    .filter(java.util.Objects::nonNull)
                    .max(LocalDateTime::compareTo)
                    .orElse(null);
            boolean currentCycle = stageMatches && (latestArtifactAt == null || latestAegis.getCreateTime() == null
                    || !latestAegis.getCreateTime().isBefore(latestArtifactAt));
            String verdict = currentCycle ? latestAegis.getVerdict() : null;
            boolean pass = "pass".equalsIgnoreCase(verdict);
            r.setRef(currentCycle ? "aegis:" + latestAegis.getReviewId() : "aegis-review:" + currentStage);
            r.setType("AEGIS");
            r.setStatus(pass ? "PASS" : (verdict == null ? "MISSING" : verdict.toUpperCase(java.util.Locale.ROOT)));
            r.setVerifier(verdict);
            r.setSigned(currentCycle);
            r.setHashOk(currentCycle);
            r.setBlocking(!pass);
            list.add(r);
        }
        return list;
    }

    static boolean requiresAegis(String flowType, String stage) {
        if (stage == null || flowType == null) {
            return false;
        }
        if ("FULL_RELEASE".equalsIgnoreCase(flowType)) {
            return Set.of("concept", "plan", "develop", "verify", "launch")
                    .contains(stage.toLowerCase(java.util.Locale.ROOT));
        }
        return "INCREMENT".equalsIgnoreCase(flowType)
                && Set.of("plan", "develop").contains(stage.toLowerCase(java.util.Locale.ROOT));
    }

    static boolean aegisMatchesStage(SpkAegisReviewDO review, String stage) {
        if (review == null || review.getNodeKey() == null || stage == null) {
            return false;
        }
        String node = review.getNodeKey().toLowerCase(java.util.Locale.ROOT);
        return switch (stage.toLowerCase(java.util.Locale.ROOT)) {
            case "concept" -> node.startsWith("n_concept_");
            case "plan" -> node.startsWith("n_plan_");
            case "develop" -> node.startsWith("n_dev_") || node.startsWith("n_develop_");
            case "verify" -> node.startsWith("n_qual_") || node.startsWith("n_verify_");
            case "launch" -> node.startsWith("n_launch_");
            default -> false;
        };
    }

    private List<String> buildBlockingItems(List<SpkIpdDecisionPackageRespVO.RequiredArtifact> required,
                                           List<SpkGateRecordDO> gates, boolean chainValid,
                                           List<SpkIpdEvidenceWaiverDO> waivers) {
        Set<String> waivedRefs = waivers == null ? Collections.emptySet()
                : waivers.stream().filter(w -> "GRANTED".equalsIgnoreCase(w.getStatus()))
                .map(SpkIpdEvidenceWaiverDO::getEvidenceRef).collect(Collectors.toSet());
        List<String> blocks = new ArrayList<>();
        for (SpkIpdDecisionPackageRespVO.RequiredArtifact r : required) {
            if (Boolean.TRUE.equals(r.getBlocking()) && !waivedRefs.contains(r.getRef())) {
                blocks.add(r.getRef() + "(" + r.getStatus() + ")");
            }
        }
        return blocks;
    }

    private List<SpkIpdDecisionPackageRespVO.CandidateAction> buildCandidateActions(
            List<String> blockingItems, List<SpkDcpRedirectLogDO> redirects) {
        List<SpkIpdDecisionPackageRespVO.CandidateAction> actions = new ArrayList<>();
        boolean goEnabled = blockingItems.isEmpty();
        SpkIpdDecisionPackageRespVO.CandidateAction go = new SpkIpdDecisionPackageRespVO.CandidateAction();
        go.setDecision("APPROVE");
        go.setEnabled(goEnabled);
        go.setReason(goEnabled ? null : "必需产物/门禁/证据未齐套且无豁免：" + String.join(";", blockingItems));
        actions.add(go);
        SpkIpdDecisionPackageRespVO.CandidateAction no = new SpkIpdDecisionPackageRespVO.CandidateAction();
        no.setDecision("REJECT");
        no.setEnabled(true);
        actions.add(no);
        SpkIpdDecisionPackageRespVO.CandidateAction rd = new SpkIpdDecisionPackageRespVO.CandidateAction();
        rd.setDecision("REDIRECT");
        int redirCount = redirects == null ? 0 : redirects.stream()
                .mapToInt(r -> r.getRedirectCount() == null ? 0 : r.getRedirectCount()).sum();
        rd.setEnabled(redirCount < 3);
        rd.setReason(redirCount < 3 ? null : "DCP 重定向次数超限(" + redirCount + ")");
        actions.add(rd);
        SpkIpdDecisionPackageRespVO.CandidateAction ret = new SpkIpdDecisionPackageRespVO.CandidateAction();
        ret.setDecision("RETURN");
        ret.setEnabled(true);
        actions.add(ret);
        return actions;
    }

    private List<SpkIpdDecisionPackageRespVO.SummarySection> buildSummary(
            SpkIpdFlowRunDO run, SpkIpdProjectDO proj, SpkIpdVersionDO ver,
            List<SpkArtifactManifestDO> artifacts, List<SpkArtifactManifestDO> currentStageArtifacts,
            List<SpkGateRecordDO> gates,
            List<SpkCcbRecordDO> ccb, List<SpkDcpRedirectLogDO> redirects,
            String approvalStage) {
        List<SpkIpdDecisionPackageRespVO.SummarySection> sections = new ArrayList<>();
        // 目标与范围
        sections.add(section("目标与范围",
                row("项目", proj != null ? proj.getName() : "-", null),
                row("版本", ver != null ? ver.getVersionNo() : "-", null),
                row("版本类型", ver != null ? ver.getVersionType() : "-", null),
                row("版本目标", ver != null ? ver.getObjective() : "-", null),
                row("范围摘要", ver != null ? ver.getScopeSummary() : "-", null)));
        // 计划与投入
        sections.add(section("计划与投入",
                row("计划开始", ver != null ? String.valueOf(ver.getPlannedStartAt()) : "-", null),
                row("计划结束", ver != null ? String.valueOf(ver.getPlannedEndAt()) : "-", null),
                row("流程类型", run != null ? run.getFlowType() : "-", null),
                row("运行编号", run != null ? run.getRunNo() : "-", null),
                row("当前阶段", approvalStage != null ? approvalStage : "-", null)));
        // 技术与质量
        int gatePass = (int) (gates == null ? 0 : gates.stream().filter(g -> Boolean.TRUE.equals(g.getPass())).count());
        int gateTotal = gates == null ? 0 : gates.size();
        sections.add(section("技术与质量",
                row("门禁通过", gatePass + "/" + gateTotal, gateTotal > 0 && gatePass < gateTotal ? "warning" : "success"),
                row("产物数量", String.valueOf(artifacts == null ? 0 : artifacts.size()), null),
                row("健康", run != null ? run.getHealth() : "-", "CRITICAL".equals(run != null ? run.getHealth() : null) ? "danger" : null)));
        // 风险与例外
        sections.add(section("风险与例外",
                row("CCB 变更", String.valueOf(ccb == null ? 0 : ccb.size()), ccb != null && !ccb.isEmpty() ? "warning" : null),
                row("DCP 重定向", String.valueOf(redirects == null ? 0 : redirects.size()), redirects != null && !redirects.isEmpty() ? "warning" : null),
                row("阻断原因", run != null ? run.getBlockReason() : "-", null)));
        // 必需产物
        SpkIpdDecisionPackageRespVO.SummarySection ra = section("必需产物",
                row("当前审批轮次", String.valueOf(currentStageArtifacts == null ? 0 : currentStageArtifacts.size()), null),
                row("历史审计总数", String.valueOf(artifacts == null ? 0 : artifacts.size()), null));
        if (currentStageArtifacts != null) {
            for (SpkArtifactManifestDO a : currentStageArtifacts) {
                ra.getRows().add(row(a.getArtifactType(), a.getStatus(),
                        "MISSING".equalsIgnoreCase(a.getStatus()) ? "danger" : null));
            }
        }
        sections.add(ra);
        // 审批影响
        sections.add(section("审批影响",
                row("Go", "推进至下一阶段/门禁", "success"),
                row("No-Go", "驳回至发起人/返工", "danger"),
                row("Redirect", "回退至目标节点重做", "warning")));
        return sections;
    }

    private SpkIpdDecisionPackageRespVO.SummarySection section(String title, SpkIpdDecisionPackageRespVO.SummaryRow... rows) {
        SpkIpdDecisionPackageRespVO.SummarySection s = new SpkIpdDecisionPackageRespVO.SummarySection();
        s.setTitle(title);
        s.setRows(new ArrayList<>(Arrays.asList(rows)));
        return s;
    }

    private SpkIpdDecisionPackageRespVO.SummaryRow row(String label, String value, String type) {
        SpkIpdDecisionPackageRespVO.SummaryRow r = new SpkIpdDecisionPackageRespVO.SummaryRow();
        r.setLabel(label);
        r.setValue(value);
        r.setType(type);
        return r;
    }

    /**
     * 决策包快照 hash：对关键聚合字段做规范化 JSON 后 sha256。
     * <p>
     * 稳定性铁律：hash 输入不得含 now() 派生的易变量。buildHeader 把 waitDurationMs
     * （= Duration.between(startedAt, now())）放进 header，若原样纳入 hash，则前后两次
     * getDecisionPackage（间隔数百 ms~几 s）算出的 hash 必不同，createDecision 重算校验
     * 永远 IPD_DECISION_PACKAGE_HASH_MISMATCH（1050116083"决策包已变化"），前端无法审批。
     * 故复制 header 剔除 waitDurationMs 后再 hash；后续新增 now() 派生字段亦须在此剔除。
     */
    private String computeHash(SpkIpdDecisionPackageRespVO pkg) {
        Map<String, Object> stableHeader = new LinkedHashMap<>();
        if (pkg.getHeader() != null) {
            stableHeader.putAll(pkg.getHeader());
            stableHeader.remove("waitDurationMs");
        }
        Map<String, Object> normalized = new LinkedHashMap<>();
        normalized.put("header", stableHeader);
        normalized.put("requiredArtifacts", pkg.getRequiredArtifacts());
        normalized.put("gates", pkg.getGates());
        normalized.put("evidence", pkg.getEvidence());
        normalized.put("artifacts", pkg.getArtifacts());
        normalized.put("ccb", pkg.getCcb());
        normalized.put("dcpRedirects", pkg.getDcpRedirects());
        normalized.put("decisions", pkg.getDecisions());
        normalized.put("waivers", pkg.getWaivers());
        normalized.put("chainValid", pkg.getChainValid());
        return "sha256:" + SpkContextBuilderService.sha256(JsonUtils.toJsonString(normalized));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SpkIpdDecisionRecordDO createDecision(Long userId, String taskId, SpkIpdDecisionReqVO req) {
        String decision = req.getDecision() == null ? null : req.getDecision().toUpperCase();
        if (!"APPROVE".equals(decision) && !"REJECT".equals(decision)
                && !"REDIRECT".equals(decision) && !"RETURN".equals(decision)) {
            throw exception(IPD_DECISION_INVALID);
        }
        // 校验任务仍属于当前用户且未被处理
        Task task = bpmTaskService.getTask(taskId);
        if (task == null) {
            throw exception(IPD_TASK_ALREADY_COMPLETED);
        }
        Long assignee = parseLong(task.getAssignee());
        if (assignee == null || !assignee.equals(userId)) {
            throw exception(IPD_TASK_NOT_BELONG_TO_USER);
        }
        if ((decision.equals("REDIRECT") || decision.equals("RETURN"))
                && (req.getRedirectTargetTaskKey() == null || req.getRedirectTargetTaskKey().isEmpty())) {
            throw exception(IPD_REDIRECT_TARGET_REQUIRED);
        }
        if (decision.equals("REJECT") && (req.getReason() == null || req.getReason().isBlank())) {
            throw exception(IPD_FLOW_RUN_REASON_REQUIRED);
        }

        SpkIpdFlowRunDO run = flowRunService.getByProcessInstanceId(task.getProcessInstanceId());

        // APPROVE：重算决策包 hash 校验 + 必需证据/门禁检查
        if ("APPROVE".equals(decision)) {
            SpkIpdDecisionPackageRespVO pkg = getDecisionPackage(taskId);
            if (req.getDecisionPackageHash() != null && !req.getDecisionPackageHash().isEmpty()
                    && !req.getDecisionPackageHash().equals(pkg.getDecisionPackageHash())) {
                throw exception(IPD_DECISION_PACKAGE_HASH_MISMATCH);
            }
            List<String> blocks = pkg.getBlockingItems();
            boolean force = Boolean.TRUE.equals(req.getForceOverride());
            if (blocks != null && !blocks.isEmpty()) {
                if (!force) {
                    throw exception(IPD_REQUIRED_ARTIFACT_MISSING);
                }
                log.warn("[createDecision][强制放行 taskId={} blocks={} by userId={}]", taskId, blocks, userId);
            }
        }

        // 写不可变决策记录（先于 BPM 动作，保证审计完整）
        SpkIpdDecisionRecordDO record = SpkIpdDecisionRecordDO.builder()
                .taskId(taskId)
                .processInstanceId(task.getProcessInstanceId())
                .flowRunId(run != null ? run.getId() : null)
                .projectId(run != null ? run.getProjectId() : null)
                .versionId(run != null ? run.getVersionId() : null)
                .decision(decision)
                .reason(req.getReason())
                .conditionsJson(req.getConditions() == null ? null : JsonUtils.toJsonString(req.getConditions()))
                .decisionPackageHash(req.getDecisionPackageHash())
                .flowableVariablesJson(req.getFlowableVariables() == null ? null : JsonUtils.toJsonString(req.getFlowableVariables()))
                .redirectTargetTaskKey(req.getRedirectTargetTaskKey())
                .deciderUserId(userId)
                .build();
        decisionRecordMapper.insert(record);

        // APPROVE/RETURN 复用原生 BPM；REJECT 不使用会结束流程的通用 reject handler，
        // 而是回到阶段自动执行节点，由 Flowable 重新触发 Omnigent 后再次进入审批。
        switch (decision) {
            case "APPROVE" -> {
                BpmTaskApproveReqVO a = new BpmTaskApproveReqVO();
                a.setId(taskId);
                a.setReason(req.getReason());
                a.setVariables(req.getFlowableVariables());
                a.setNextAssignees(req.getNextAssignees());
                bpmTaskService.approveTask(userId, a);
            }
            case "REJECT" -> {
                SpkIpdReworkPolicy.Route route = SpkIpdReworkPolicy
                        .resolve(task.getTaskDefinitionKey())
                        .orElseThrow(() -> exception(DCP_REDIRECT_FAIL));
                Map<String, Object> reworkVariables = new LinkedHashMap<>();
                reworkVariables.put(SpkIpdReworkVariables.GATE, route.gate());
                reworkVariables.put(SpkIpdReworkVariables.TARGET_STAGE, route.stage());
                reworkVariables.put(SpkIpdReworkVariables.REASON, req.getReason().trim());
                if (record.getId() != null) {
                    reworkVariables.put(SpkIpdReworkVariables.DECISION_ID, record.getId());
                }
                dcpRedirectService.redirect(task.getProcessInstanceId(), route.gate(), route.targetNode(),
                        reworkVariables);
            }
            case "RETURN", "REDIRECT" -> {
                BpmTaskReturnReqVO r = new BpmTaskReturnReqVO();
                r.setId(taskId);
                r.setTargetTaskDefinitionKey(req.getRedirectTargetTaskKey());
                r.setReason(req.getReason());
                bpmTaskService.returnTask(userId, r);
            }
        }
        return record;
    }

    @Override
    public List<SpkIpdDecisionRecordDO> listDecisionsByFlowRun(Long flowRunId) {
        if (flowRunId == null) return Collections.emptyList();
        return decisionRecordMapper.selectListByFlowRunId(flowRunId);
    }

    @Override
    public SpkIpdEvidenceWaiverDO createEvidenceWaiver(Long userId, String taskId, SpkIpdEvidenceWaiverReqVO req) {
        Task task = bpmTaskService.getTask(taskId);
        if (task == null) {
            throw exception(IPD_TASK_ALREADY_COMPLETED);
        }
        SpkIpdFlowRunDO run = flowRunService.getByProcessInstanceId(task.getProcessInstanceId());
        SpkIpdEvidenceWaiverDO w = SpkIpdEvidenceWaiverDO.builder()
                .taskId(taskId)
                .processInstanceId(task.getProcessInstanceId())
                .flowRunId(run != null ? run.getId() : null)
                .projectId(run != null ? run.getProjectId() : null)
                .versionId(run != null ? run.getVersionId() : null)
                .evidenceRef(req.getEvidenceRef())
                .evidenceType(req.getEvidenceType())
                .dueAt(req.getDueAt())
                .reason(req.getReason())
                .ownerUserId(req.getOwnerUserId())
                .grantedByUserId(userId)
                .status("GRANTED")
                .build();
        evidenceWaiverMapper.insert(w);
        return w;
    }

    @Override
    public List<SpkArtifactManifestDO> getArtifacts(Long flowRunId) {
        SpkIpdFlowRunDO run = flowRunService.getFlowRun(flowRunId);
        if (run == null || run.getProcessInstanceId() == null) return Collections.emptyList();
        return artifactManifestMapper.selectListByProcessInstanceId(run.getProcessInstanceId());
    }

    @Override
    public List<SpkEvidenceRecordDO> getEvidence(Long flowRunId) {
        SpkIpdFlowRunDO run = flowRunService.getFlowRun(flowRunId);
        if (run == null || run.getProcessInstanceId() == null) return Collections.emptyList();
        return evidenceRecordMapper.selectListByProcessInstanceId(run.getProcessInstanceId());
    }
}
