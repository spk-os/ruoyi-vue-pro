package cn.iocoder.yudao.module.spkdelivery.service.ipdbusiness;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskApproveReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskPageReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskRejectReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskReturnReqVO;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.approval.SpkIpdApprovalTaskPageReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.approval.SpkIpdApprovalTaskRespVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.approval.SpkIpdDecisionPackageRespVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.approval.SpkIpdDecisionReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.approval.SpkIpdEvidenceWaiverReqVO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.artifact.SpkArtifactManifestDO;
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
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ccb.SpkCcbRecordMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.dcp.SpkDcpRedirectLogMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.evidence.SpkEvidenceRecordMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.gate.SpkGateRecordMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness.SpkIpdDecisionRecordMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness.SpkIpdEvidenceWaiverMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.taskcontract.SpkTaskContractMapper;
import cn.iocoder.yudao.module.spkdelivery.service.context.SpkContextBuilderService;
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
 * - 决策：先写不可变 {@link SpkIpdDecisionRecordDO}，再调用原生 BPM approve/reject/return。
 * <p>
 * 铁律：不改 Flowable 引擎语义、不改 BpmTaskController/BpmTaskServiceImpl，只扩展。
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

    @Override
    public PageResult<SpkIpdApprovalTaskRespVO> pageApprovalTasks(Long userId, SpkIpdApprovalTaskPageReqVO req) {
        BpmTaskPageReqVO bpmReq = new BpmTaskPageReqVO();
        bpmReq.setPageNo(req.getPageNo());
        bpmReq.setPageSize(req.getPageSize());
        bpmReq.setName(req.getName());
        bpmReq.setCategory(req.getCategory());
        // 默认只看 IPD 主交付流；前端可显式覆盖
        bpmReq.setProcessDefinitionKey(
                req.getProcessDefinitionKey() != null ? req.getProcessDefinitionKey()
                        : SpkIpdBusinessConstants.IPD_FLOW_KEY);
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
        pkg.setRequiredArtifacts(buildRequiredArtifacts(artifacts, gates, evidence, chainValid));
        pkg.setHeader(buildHeader(task, run, proj(run), ver(run)));
        pkg.setSummary(buildSummary(run, proj(run), ver(run), artifacts, gates, ccb, redirects));
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

    private Map<String, Object> buildHeader(Task task, SpkIpdFlowRunDO run,
                                             SpkIpdProjectDO proj, SpkIpdVersionDO ver) {
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
        h.put("stage", run != null ? run.getCurrentStage() : null);
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

    private List<SpkIpdDecisionPackageRespVO.RequiredArtifact> buildRequiredArtifacts(
            List<SpkArtifactManifestDO> artifacts, List<SpkGateRecordDO> gates,
            List<SpkEvidenceRecordDO> evidence, boolean chainValid) {
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
        return list;
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
            List<SpkArtifactManifestDO> artifacts, List<SpkGateRecordDO> gates,
            List<SpkCcbRecordDO> ccb, List<SpkDcpRedirectLogDO> redirects) {
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
                row("当前阶段", run != null ? run.getCurrentStage() : "-", null)));
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
                row("产物总数", String.valueOf(artifacts == null ? 0 : artifacts.size()), null));
        if (artifacts != null) {
            for (SpkArtifactManifestDO a : artifacts) {
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

    /** 决策包快照 hash：对关键聚合字段做规范化 JSON 后 sha256。 */
    private String computeHash(SpkIpdDecisionPackageRespVO pkg) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        normalized.put("header", pkg.getHeader());
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

        // 调用原生 BPM approve/reject/return（复用原生语义，不改引擎）
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
                BpmTaskRejectReqVO r = new BpmTaskRejectReqVO();
                r.setId(taskId);
                r.setReason(req.getReason());
                bpmTaskService.rejectTask(userId, r);
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
