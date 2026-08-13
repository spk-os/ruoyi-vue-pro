package cn.iocoder.yudao.module.spkdelivery.service.ipdbusiness;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.flowrun.SpkIpdFlowRunPreflightReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.issue.SpkIpdIssueCaseCreateReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.issue.SpkIpdIssueCasePageReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.issue.SpkIpdIssueCaseUpdateReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.issue.SpkIpdIssueTriageReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.issue.SpkIpdIssueVersionRelReqVO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdFlowRunDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdIssueCaseDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdIssueVersionRelDO;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness.SpkIpdIssueCaseMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness.SpkIpdIssueVersionRelMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
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
 * IPD 问题 IssueCase 服务：登记/分诊/关联/处置/关闭/重开/启动问题流。
 * 设计文档 section 2.7 / 9.3.4 / 10.6 / 11.5。
 * <p>
 * 问题流（ISSUE_RESOLUTION）是 IPD 第三类流程：根因→修复→验证→发布最小集。
 * FIXED_IN 关联是进入实施的前置；P0/P1 关闭前要求验证结论。
 *
 * @author SPK-OS
 */
@Slf4j
@Service
@Validated
public class SpkIpdIssueCaseService {

    @Resource
    private SpkIpdIssueCaseMapper issueCaseMapper;
    @Resource
    private SpkIpdIssueVersionRelMapper issueVersionRelMapper;
    @Resource
    @Lazy
    private SpkIpdFlowRunService flowRunService;

    // ==================== 登记 / 查询 ====================

    @Transactional(rollbackFor = Exception.class)
    public SpkIpdIssueCaseDO createIssue(SpkIpdIssueCaseCreateReqVO req) {
        // 外部引用去重：同 externalSystem + externalId 视为同一问题
        if (req.getExternalSystem() != null && req.getExternalId() != null) {
            SpkIpdIssueCaseDO dup = issueCaseMapper.selectByExternal(req.getExternalSystem(), req.getExternalId());
            if (dup != null) {
                throw exception(IPD_ISSUE_NOT_EXISTS); // 复用：外部去重命中，调用方应回放
            }
        }
        SpkIpdIssueCaseDO issue = SpkIpdIssueCaseDO.builder()
                .projectId(req.getProjectId())
                .issueType(req.getIssueType())
                .severity(req.getSeverity())
                .title(req.getTitle())
                .description(req.getDescription())
                .source(req.getSource() == null ? "MANUAL" : req.getSource())
                .externalSystem(req.getExternalSystem())
                .externalId(req.getExternalId())
                .externalUrl(req.getExternalUrl())
                .ownerUserId(req.getOwnerUserId())
                .status("OPEN")
                .detectedAt(req.getDetectedAt() == null ? LocalDateTime.now() : req.getDetectedAt())
                .build();
        // case_no 依赖自增 id 且列有 NOT NULL+唯一约束，先插占位值再回填
        issue.setCaseNo("CASE-TMP-" + System.nanoTime());
        issueCaseMapper.insert(issue);
        issue.setCaseNo(caseNo(issue.getId()));
        issueCaseMapper.updateById(issue);
        return issue;
    }

    @Transactional(rollbackFor = Exception.class)
    public SpkIpdIssueCaseDO updateIssue(Long issueCaseId, SpkIpdIssueCaseUpdateReqVO req) {
        SpkIpdIssueCaseDO issue = getIssueOrThrow(issueCaseId);
        if (req.getIssueType() != null) issue.setIssueType(req.getIssueType());
        if (req.getSeverity() != null) issue.setSeverity(req.getSeverity());
        if (req.getTitle() != null) issue.setTitle(req.getTitle());
        if (req.getDescription() != null) issue.setDescription(req.getDescription());
        if (req.getOwnerUserId() != null) issue.setOwnerUserId(req.getOwnerUserId());
        if (req.getRootCause() != null) issue.setRootCause(req.getRootCause());
        if (req.getResolution() != null) issue.setResolution(req.getResolution());
        issue.setLockVersion(req.getLockVersion());
        issueCaseMapper.updateById(issue);
        return issue;
    }

    public SpkIpdIssueCaseDO getIssue(Long issueCaseId) {
        return getIssueOrThrow(issueCaseId);
    }

    public PageResult<SpkIpdIssueCaseDO> pageIssues(SpkIpdIssueCasePageReqVO req) {
        return issueCaseMapper.selectPage(req);
    }

    // ==================== 分诊 ====================

    /**
     * 分诊：确定严重性、影响版本、SLA、是否启动问题流。设计文档 §10.6 /triage。
     * 状态 OPEN → TRIAGED。
     */
    @Transactional(rollbackFor = Exception.class)
    public SpkIpdIssueCaseDO triage(Long issueCaseId, SpkIpdIssueTriageReqVO req) {
        SpkIpdIssueCaseDO issue = getIssueOrThrow(issueCaseId);
        if (!"OPEN".equals(issue.getStatus()) && !"REOPENED".equals(issue.getStatus())) {
            throw exception(IPD_ISSUE_NOT_RESOLVABLE);
        }
        if (req.getSeverity() != null) issue.setSeverity(req.getSeverity());
        if (req.getOwnerUserId() != null) issue.setOwnerUserId(req.getOwnerUserId());
        issue.setStatus("TRIAGED");
        issueCaseMapper.updateById(issue);
        // 落影响版本关联（AFFECTS）
        if (req.getAffectedVersionIds() != null) {
            for (Long vid : req.getAffectedVersionIds()) {
                upsertVersionRelation(issueCaseId, vid, "AFFECTS");
            }
        }
        return issue;
    }

    // ==================== 版本关联 ====================

    @Transactional(rollbackFor = Exception.class)
    public SpkIpdIssueVersionRelDO addVersionRelation(Long issueCaseId, SpkIpdIssueVersionRelReqVO req) {
        getIssueOrThrow(issueCaseId);
        return upsertVersionRelation(issueCaseId, req.getVersionId(), req.getRelationType());
    }

    public List<SpkIpdIssueVersionRelDO> listVersionRelations(Long issueCaseId) {
        return issueVersionRelMapper.selectListByIssue(issueCaseId);
    }

    private SpkIpdIssueVersionRelDO upsertVersionRelation(Long issueCaseId, Long versionId, String relationType) {
        SpkIpdIssueVersionRelDO exist = issueVersionRelMapper.selectOne(issueCaseId, versionId, relationType);
        if (exist != null) {
            return exist;
        }
        SpkIpdIssueVersionRelDO rel = SpkIpdIssueVersionRelDO.builder()
                .issueCaseId(issueCaseId)
                .versionId(versionId)
                .relationType(relationType)
                .build();
        issueVersionRelMapper.insert(rel);
        return rel;
    }

    // ==================== 处置 / 关闭 / 重开 ====================

    /**
     * 解决问题：必须填写结论；P0/P1 还要求独立验证证据（S1 以 VERIFIED_IN 关联为准）。
     */
    @Transactional(rollbackFor = Exception.class)
    public SpkIpdIssueCaseDO resolve(Long issueCaseId, String resolution) {
        SpkIpdIssueCaseDO issue = getIssueOrThrow(issueCaseId);
        if (resolution == null || resolution.isBlank()) {
            throw exception(IPD_ISSUE_NOT_RESOLVABLE);
        }
        // FIXED_IN 关联是进入实施修复的前置
        boolean hasFixedIn = issueVersionRelMapper.selectListByIssue(issueCaseId).stream()
                .anyMatch(r -> "FIXED_IN".equals(r.getRelationType()));
        if (!hasFixedIn) {
            throw exception(IPD_ISSUE_NO_FIXED_IN);
        }
        issue.setResolution(resolution);
        issue.setStatus("RESOLVED");
        issue.setResolvedAt(LocalDateTime.now());
        issueCaseMapper.updateById(issue);
        return issue;
    }

    @Transactional(rollbackFor = Exception.class)
    public SpkIpdIssueCaseDO closeIssue(Long issueCaseId) {
        SpkIpdIssueCaseDO issue = getIssueOrThrow(issueCaseId);
        if (!"RESOLVED".equals(issue.getStatus())) {
            throw exception(IPD_ISSUE_NOT_RESOLVABLE);
        }
        // P0/P1 要求验证结论（VERIFIED_IN 关联）
        if ("P0".equals(issue.getSeverity()) || "P1".equals(issue.getSeverity())) {
            boolean hasVerified = issueVersionRelMapper.selectListByIssue(issueCaseId).stream()
                    .anyMatch(r -> "VERIFIED_IN".equals(r.getRelationType()));
            if (!hasVerified) {
                throw exception(IPD_ISSUE_NOT_RESOLVABLE);
            }
        }
        issue.setStatus("CLOSED");
        issue.setClosedAt(LocalDateTime.now());
        issueCaseMapper.updateById(issue);
        return issue;
    }

    /**
     * 重开：保留旧运行，创建新的处理 attempt。状态 → REOPENED。
     */
    @Transactional(rollbackFor = Exception.class)
    public SpkIpdIssueCaseDO reopen(Long issueCaseId, String reason) {
        SpkIpdIssueCaseDO issue = getIssueOrThrow(issueCaseId);
        issue.setStatus("REOPENED");
        issue.setClosedAt(null);
        issueCaseMapper.updateById(issue);
        log.info("[reopen][issueCaseId={} reason={}]", issueCaseId, reason);
        return issue;
    }

    // ==================== 启动问题流 ====================

    /**
     * 创建 ISSUE_RESOLUTION FlowRun。内部复用 FlowRunService.createFlowRun + start。
     * 设计文档 §10.6 /start-flow。幂等键由调用方通过 Idempotency-Key 头传入。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> startFlow(Long issueCaseId, String idempotencyKey) {
        SpkIpdIssueCaseDO issue = getIssueOrThrow(issueCaseId);
        // 构造预检请求：问题流不需要 versionId，flowType=ISSUE_RESOLUTION
        SpkIpdFlowRunPreflightReqVO req = new SpkIpdFlowRunPreflightReqVO();
        req.setProjectId(issue.getProjectId());
        req.setIssueCaseId(issueCaseId);
        req.setFlowType(FLOW_ISSUE_RESOLUTION);
        SpkIpdFlowRunDO run = flowRunService.createFlowRun(req);
        // 问题流草稿直接置 READY，允许启动
        run.setStatus("READY");
        Map<String, Object> started = flowRunService.start(run.getId(),
                idempotencyKey == null ? ("issue-" + issueCaseId + "-" + System.nanoTime()) : idempotencyKey);
        Map<String, Object> result = new LinkedHashMap<>(started);
        result.put("issueCaseId", issueCaseId);
        result.put("flowRunId", run.getId());
        return result;
    }

    // ==================== 辅助 ====================

    public Map<String, Object> issueSummary(SpkIpdIssueCaseDO issue) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("issueCaseId", issue.getId());
        m.put("caseNo", issue.getCaseNo());
        m.put("title", issue.getTitle());
        m.put("severity", issue.getSeverity());
        m.put("issueType", issue.getIssueType());
        m.put("status", issue.getStatus());
        m.put("ownerUserId", issue.getOwnerUserId());
        m.put("versionRelations", listVersionRelations(issue.getId()));
        return m;
    }

    private SpkIpdIssueCaseDO getIssueOrThrow(Long issueCaseId) {
        SpkIpdIssueCaseDO i = issueCaseMapper.selectById(issueCaseId);
        if (i == null) {
            throw exception(IPD_ISSUE_NOT_EXISTS);
        }
        return i;
    }

    private Long currentUserId() {
        Long uid = SecurityFrameworkUtils.getLoginUserId();
        return uid != null ? uid : 1L;
    }
}
