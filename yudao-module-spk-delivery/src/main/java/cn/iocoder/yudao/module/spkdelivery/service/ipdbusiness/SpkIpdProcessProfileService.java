package cn.iocoder.yudao.module.spkdelivery.service.ipdbusiness;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.governance.SpkIpdProcessProfilePageReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.governance.SpkIpdProcessProfileSaveReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.governance.SpkIpdSnapshotSchemaRespVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.governance.SpkIpdTrimRuleSaveReqVO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdEngineInstanceDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdFailedJobDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdGovernanceAuditDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdProcessProfileDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdProcessProfileVersionDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdTrimRuleDO;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness.SpkIpdEngineInstanceMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness.SpkIpdFailedJobMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness.SpkIpdGovernanceAuditMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness.SpkIpdProcessProfileMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness.SpkIpdProcessProfileVersionMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness.SpkIpdTrimRuleMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.spkdelivery.enums.ErrorCodeConstants.*;

/**
 * IPD 流程治理服务。设计文档 §7.2 第 4 顶层表面 / §6.3 / §9.5。
 * <p>
 * Profile 模板 CRUD + 版本发布/回滚 + 裁剪规则 + 引擎档案/失败作业/审计查询。
 * 管理员配置产物（非运行数据）：Profile 模板可建真实种子，运行统计无数据如实返回空。
 * 发布语义：DRAFT 版本发布 → PUBLISHED，旧已发布版本置 SUPERSEDED，Profile.current_version 更新。
 *
 * @author SPK-OS
 */
@Slf4j
@Service
@Validated
public class SpkIpdProcessProfileService {

    @Resource private SpkIpdProcessProfileMapper profileMapper;
    @Resource private SpkIpdProcessProfileVersionMapper profileVersionMapper;
    @Resource private SpkIpdTrimRuleMapper trimRuleMapper;
    @Resource private SpkIpdEngineInstanceMapper engineInstanceMapper;
    @Resource private SpkIpdFailedJobMapper failedJobMapper;
    @Resource private SpkIpdGovernanceAuditMapper governanceAuditMapper;

    /* ============ Profile CRUD ============ */
    @Transactional(rollbackFor = Exception.class)
    public Long createProfile(SpkIpdProcessProfileSaveReqVO req) {
        if (profileMapper.selectByProfileCode(req.getProfileCode()) != null) {
            throw exception(IPD_PROFILE_CODE_DUPLICATE);
        }
        SpkIpdProcessProfileDO p = SpkIpdProcessProfileDO.builder()
                .profileCode(req.getProfileCode()).name(req.getName())
                .flowType(req.getFlowType()).description(req.getDescription())
                // D1：按 flowType 自动绑定 BPM 流程定义 key（修 G1/G3），不可手改
                .processDefinitionKey(SpkIpdBusinessConstants.flowKeyOf(req.getFlowType()))
                .status(req.getStatus() == null ? "DRAFT" : req.getStatus())
                .currentVersion(0).lockVersion(0).build();
        profileMapper.insert(p);
        audit("PROFILE_CREATE", p.getId(), null, p);
        return p.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(SpkIpdProcessProfileSaveReqVO req) {
        SpkIpdProcessProfileDO p = getProfileOrThrow(req.getId());
        SpkIpdProcessProfileDO before = cloneProfile(p);
        if (req.getFlowType() != null && !req.getFlowType().equals(p.getFlowType())) {
            throw exception(IPD_PROFILE_FLOW_TYPE_MISMATCH);
        }
        p.setName(req.getName());
        p.setDescription(req.getDescription());
        if (req.getStatus() != null) p.setStatus(req.getStatus());
        profileMapper.updateById(p);
        audit("PROFILE_UPDATE", p.getId(), before, p);
    }

    public SpkIpdProcessProfileDO getProfile(Long id) {
        return getProfileOrThrow(id);
    }

    public PageResult<SpkIpdProcessProfileDO> pageProfile(SpkIpdProcessProfilePageReqVO req) {
        return profileMapper.selectPage(req, new LambdaQueryWrapperX<SpkIpdProcessProfileDO>()
                .eqIfPresent(SpkIpdProcessProfileDO::getFlowType, req.getFlowType())
                .eqIfPresent(SpkIpdProcessProfileDO::getStatus, req.getStatus())
                .and(req.getKeyword() != null && !req.getKeyword().isBlank(), w -> w
                        .like(SpkIpdProcessProfileDO::getProfileCode, req.getKeyword())
                        .or().like(SpkIpdProcessProfileDO::getName, req.getKeyword()))
                .orderByDesc(SpkIpdProcessProfileDO::getUpdateTime));
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteProfile(Long id) {
        getProfileOrThrow(id);
        profileMapper.deleteById(id);
        audit("PROFILE_DELETE", id, null, null);
    }
    /* ============ Profile 版本 ============ */
    // METHODS_VERSION
    /** 新建草稿版本：版本号 = 现有最大 +1，snapshot 不可变。 */
    @Transactional(rollbackFor = Exception.class)
    public Long createVersion(Long profileId, String snapshotJson, String compatibilityHash) {
        SpkIpdProcessProfileDO p = getProfileOrThrow(profileId);
        List<SpkIpdProcessProfileVersionDO> all = profileVersionMapper.selectListByProfileId(profileId);
        int next = all.isEmpty() ? 1 : all.get(0).getVersion() + 1;
        SpkIpdProcessProfileVersionDO v = SpkIpdProcessProfileVersionDO.builder()
                .profileId(profileId).version(next).snapshotJson(snapshotJson)
                .status("DRAFT").compatibilityHash(compatibilityHash).lockVersion(0).build();
        profileVersionMapper.insert(v);
        audit("VERSION_CREATE", v.getId(), null, v);
        return v.getId();
    }

    /** 发布版本：DRAFT→PUBLISHED，旧已发布版本置 SUPERSEDED，Profile.current_version 更新。 */
    @Transactional(rollbackFor = Exception.class)
    public void publishVersion(Long versionId) {
        SpkIpdProcessProfileVersionDO v = getVersionOrThrow(versionId);
        if ("PUBLISHED".equals(v.getStatus())) {
            throw exception(IPD_PROFILE_VERSION_ALREADY_PUBLISHED);
        }
        SpkIpdProcessProfileVersionDO before = cloneVersion(v);
        // 旧已发布版本置 SUPERSEDED
        SpkIpdProcessProfileVersionDO prevPublished = profileVersionMapper.selectPublishedByProfileId(v.getProfileId());
        if (prevPublished != null && !prevPublished.getId().equals(v.getId())) {
            prevPublished.setStatus("SUPERSEDED");
            prevPublished.setSupersedesVersionId(v.getId());
            profileVersionMapper.updateById(prevPublished);
        }
        String operator = operatorId();
        v.setStatus("PUBLISHED");
        v.setPublishedBy(operator);
        v.setPublishedAt(LocalDateTime.now());
        if (prevPublished != null) v.setSupersedesVersionId(prevPublished.getId());
        profileVersionMapper.updateById(v);
        // 同步 Profile.current_version
        SpkIpdProcessProfileDO p = getProfileOrThrow(v.getProfileId());
        // D1：发布时把 Profile 绑定的 BPM key 固化进版本（回滚可定位历史流程定义，修 G3）
        v.setProcessDefinitionKey(p.getProcessDefinitionKey());
        p.setCurrentVersion(v.getVersion());
        p.setStatus("PUBLISHED");
        p.setPublishedBy(operator);
        p.setPublishedAt(v.getPublishedAt());
        profileMapper.updateById(p);
        audit("VERSION_PUBLISH", v.getId(), before, v);
    }

    /** 回滚：将指定版本重新置为当前已发布（旧当前置 SUPERSEDED）。仅已发布版本可回滚。 */
    @Transactional(rollbackFor = Exception.class)
    public void rollbackVersion(Long versionId) {
        SpkIpdProcessProfileVersionDO v = getVersionOrThrow(versionId);
        if (!"PUBLISHED".equals(v.getStatus()) && !"SUPERSEDED".equals(v.getStatus())) {
            throw exception(IPD_PROFILE_VERSION_NOT_PUBLISHED);
        }
        SpkIpdProcessProfileVersionDO before = cloneVersion(v);
        SpkIpdProcessProfileVersionDO cur = profileVersionMapper.selectPublishedByProfileId(v.getProfileId());
        if (cur != null && !cur.getId().equals(v.getId())) {
            cur.setStatus("SUPERSEDED");
            cur.setSupersedesVersionId(v.getId());
            profileVersionMapper.updateById(cur);
        }
        v.setStatus("PUBLISHED");
        v.setPublishedBy(operatorId());
        v.setPublishedAt(LocalDateTime.now());
        profileVersionMapper.updateById(v);
        SpkIpdProcessProfileDO p = getProfileOrThrow(v.getProfileId());
        p.setCurrentVersion(v.getVersion());
        profileMapper.updateById(p);
        audit("VERSION_ROLLBACK", v.getId(), before, v);
    }

    public List<SpkIpdProcessProfileVersionDO> listVersions(Long profileId) {
        return profileVersionMapper.selectListByProfileId(profileId);
    }

    public SpkIpdProcessProfileVersionDO getVersion(Long versionId) {
        return getVersionOrThrow(versionId);
    }

    /** 供 FlowRunService 启动时引用：按 flowType 取当前已发布 Profile + 版本。无则抛错。 */
    public PublishedProfile getPublishedForFlowType(String flowType) {
        SpkIpdProcessProfileDO p = profileMapper.selectByFlowType(flowType);
        if (p == null) {
            throw exception(IPD_PROFILE_NOT_PUBLISHED);
        }
        SpkIpdProcessProfileVersionDO v = profileVersionMapper.selectPublishedByProfileId(p.getId());
        if (v == null) {
            throw exception(IPD_PROFILE_NOT_PUBLISHED);
        }
        // D1：返回 Profile 绑定的 BPM key，供 FlowRunService.start 按 flowType 启动对应流程（修 G1）
        String key = v.getProcessDefinitionKey() != null ? v.getProcessDefinitionKey()
                : (p.getProcessDefinitionKey() != null ? p.getProcessDefinitionKey()
                : SpkIpdBusinessConstants.flowKeyOf(flowType));
        return new PublishedProfile(p.getId(), v.getId(), v.getVersion(), key, v.getSnapshotJson());
    }

    /** 已发布 Profile 的轻量值对象（profileId/versionId/version/processDefinitionKey/snapshot）。 */
    public record PublishedProfile(Long profileId, Long versionId, Integer version,
                                   String processDefinitionKey, String snapshotJson) {}
    /* ============ 裁剪规则 ============ */
    // METHODS_TRIM
    @Transactional(rollbackFor = Exception.class)
    public Long saveTrimRule(SpkIpdTrimRuleSaveReqVO req) {
        getVersionOrThrow(req.getProfileVersionId());
        SpkIpdTrimRuleDO r = SpkIpdTrimRuleDO.builder()
                .id(req.getId()).profileVersionId(req.getProfileVersionId())
                .stage(req.getStage()).activityDefId(req.getActivityDefId())
                .trimCondition(req.getTrimCondition()).action(req.getAction())
                .reason(req.getReason()).lockVersion(0).build();
        if (req.getId() == null) {
            trimRuleMapper.insert(r);
        } else {
            trimRuleMapper.updateById(r);
        }
        audit("TRIM_CHANGE", r.getId(), null, r);
        return r.getId();
    }

    public List<SpkIpdTrimRuleDO> listTrimRules(Long profileVersionId) {
        return trimRuleMapper.selectListByProfileVersionId(profileVersionId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteTrimRule(Long id) {
        trimRuleMapper.deleteById(id);
        audit("TRIM_DELETE", id, null, null);
    }
    /* ============ 治理查询 ============ */
    // METHODS_QUERY
    /** 引擎实例档案：按 flowRunId 查；无数据返回 null（前端标"样本不足"）。 */
    public SpkIpdEngineInstanceDO getEngineInstanceByFlowRunId(Long flowRunId) {
        return engineInstanceMapper.selectByFlowRunId(flowRunId);
    }

    public List<SpkIpdFailedJobDO> listFailedJobs(String status) {
        return failedJobMapper.selectListByStatus(status);
    }

    public List<SpkIpdGovernanceAuditDO> listAudit(String actionType, Long refId) {
        return governanceAuditMapper.selectListByActionTypeAndRefId(actionType, refId);
    }

    /**
     * D2：按 flowType 返回 snapshotJson 结构契约（阶段/门/DCP/TR/活动）。
     * 供前端版本编辑器按字段构造强类型快照。三种 flowType 各自不同阶段集，体现流程差异。
     */
    public SpkIpdSnapshotSchemaRespVO buildSnapshotSchema(String flowType) {
        SpkIpdSnapshotSchemaRespVO resp = new SpkIpdSnapshotSchemaRespVO();
        resp.setMode(flowType);
        List<SpkIpdSnapshotSchemaRespVO.StageDef> stages = new java.util.ArrayList<>();
        if (SpkIpdBusinessConstants.FLOW_INCREMENT_RELEASE.equals(flowType)) {
            // 轻量增量：跳概念阶段，DCP 合并，TR 仅关键 2 个
            stages.add(stage("plan", gates(1, 2), List.of("PDCP"), List.of("tr3"),
                    List.of("plan.scope", "plan.schedule")));
            stages.add(stage("develop", gates(3, 4), List.of("ADCP"), List.of("tr4"),
                    List.of("develop.impl", "develop.unit_test")));
            stages.add(stage("qualify", gates(5, 6), List.of(), List.of("tr5"),
                    List.of("qualify.integration", "qualify.regression")));
            stages.add(stage("launch", gates(7, 8), List.of("LDCP"), List.of(),
                    List.of("launch.deploy", "launch.monitor")));
        } else if (SpkIpdBusinessConstants.FLOW_ISSUE_RESOLUTION.equals(flowType)) {
            // 问题处置：四段轻流程，每段一审批一触发器
            stages.add(stage("root_cause", List.of(), List.of("RCDCP"), List.of("tr_rc"),
                    List.of("root_cause.analyze")));
            stages.add(stage("fix_develop", List.of(), List.of("FDCP"), List.of("tr_fix"),
                    List.of("fix_develop.patch")));
            stages.add(stage("verify", List.of(), List.of("VDCP"), List.of("tr_verify"),
                    List.of("verify.repro")));
            stages.add(stage("close", List.of(), List.of("CDCP_CLOSE"), List.of(),
                    List.of("close.archive")));
        } else {
            // 默认 FULL_RELEASE：完整六阶段 + 4 DCP + 6 TR + g1-g8
            stages.add(stage("concept", gates(1, 1), List.of("CDCP"), List.of("tr2"),
                    List.of("concept.charter", "concept.feasibility")));
            stages.add(stage("plan", gates(2, 2), List.of("PDCP"), List.of("tr3"),
                    List.of("plan.scope", "plan.schedule", "plan.resource")));
            stages.add(stage("develop", gates(3, 4), List.of("ADCP"), List.of("tr4"),
                    List.of("develop.impl", "develop.unit_test", "develop.integration")));
            stages.add(stage("qualify", gates(5, 6), List.of(), List.of("tr5"),
                    List.of("qualify.integration", "qualify.regression", "qualify.system")));
            stages.add(stage("launch", gates(7, 7), List.of("LDCP"), List.of("tr6"),
                    List.of("launch.deploy", "launch.monitor")));
            stages.add(stage("lifecycle", gates(8, 8), List.of(), List.of(),
                    List.of("lifecycle.handover")));
        }
        resp.setStages(stages);
        List<SpkIpdSnapshotSchemaRespVO.TrimRuleDef> trims = new java.util.ArrayList<>();
        SpkIpdSnapshotSchemaRespVO.TrimRuleDef t = new SpkIpdSnapshotSchemaRespVO.TrimRuleDef();
        t.setAction("SKIP"); t.setTrimCondition("mode==INCREMENT_RELEASE && stage==concept");
        t.setReason("增量发布跳过概念阶段");
        trims.add(t);
        resp.setTrimRules(trims);
        return resp;
    }

    private SpkIpdSnapshotSchemaRespVO.StageDef stage(String name, List<String> gates,
                                                      List<String> dcps, List<String> trs, List<String> activities) {
        SpkIpdSnapshotSchemaRespVO.StageDef s = new SpkIpdSnapshotSchemaRespVO.StageDef();
        s.setStage(name); s.setGates(gates); s.setDcps(dcps); s.setTrs(trs); s.setActivities(activities);
        return s;
    }

    private List<String> gates(int from, int to) {
        List<String> list = new java.util.ArrayList<>();
        for (int i = from; i <= to; i++) list.add("g" + i);
        return list;
    }

    // ---------- 内部辅助 ----------
    private SpkIpdProcessProfileDO getProfileOrThrow(Long id) {
        SpkIpdProcessProfileDO p = profileMapper.selectById(id);
        if (p == null) throw exception(IPD_PROFILE_NOT_EXISTS);
        return p;
    }

    private SpkIpdProcessProfileVersionDO getVersionOrThrow(Long id) {
        SpkIpdProcessProfileVersionDO v = profileVersionMapper.selectById(id);
        if (v == null) throw exception(IPD_PROFILE_VERSION_NOT_EXISTS);
        return v;
    }

    private SpkIpdProcessProfileDO cloneProfile(SpkIpdProcessProfileDO p) {
        return SpkIpdProcessProfileDO.builder()
                .id(p.getId()).profileCode(p.getProfileCode()).name(p.getName())
                .flowType(p.getFlowType()).description(p.getDescription()).status(p.getStatus())
                .currentVersion(p.getCurrentVersion()).publishedBy(p.getPublishedBy())
                .publishedAt(p.getPublishedAt()).lockVersion(p.getLockVersion()).build();
    }

    private SpkIpdProcessProfileVersionDO cloneVersion(SpkIpdProcessProfileVersionDO v) {
        return SpkIpdProcessProfileVersionDO.builder()
                .id(v.getId()).profileId(v.getProfileId()).version(v.getVersion())
                .snapshotJson(v.getSnapshotJson()).status(v.getStatus())
                .compatibilityHash(v.getCompatibilityHash()).publishedBy(v.getPublishedBy())
                .publishedAt(v.getPublishedAt()).supersedesVersionId(v.getSupersedesVersionId())
                .lockVersion(v.getLockVersion()).build();
    }

    private String operatorId() {
        Long uid = SecurityFrameworkUtils.getLoginUserId();
        return uid != null ? uid.toString() : "1";
    }

    private void audit(String actionType, Long refId, Object before, Object after) {
        try {
            SpkIpdGovernanceAuditDO a = SpkIpdGovernanceAuditDO.builder()
                    .actionType(actionType).refId(refId).operatorId(operatorId())
                    .beforeJson(before == null ? null : before.toString())
                    .afterJson(after == null ? null : after.toString())
                    .lockVersion(0).build();
            governanceAuditMapper.insert(a);
        } catch (Exception e) {
            log.warn("[audit][治理审计写入失败 action={} ref={}：{}]", actionType, refId, e.getMessage());
        }
    }
}
