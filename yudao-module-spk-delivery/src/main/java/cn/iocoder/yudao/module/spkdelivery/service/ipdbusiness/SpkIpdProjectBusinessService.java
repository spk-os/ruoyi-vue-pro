package cn.iocoder.yudao.module.spkdelivery.service.ipdbusiness;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.majorrelease.SpkIpdMajorReleaseCreateReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.majorrelease.SpkIpdMajorReleaseUpdateReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.project.SpkIpdProjectCreateReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.project.SpkIpdProjectPageReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.project.SpkIpdProjectUpdateReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.version.SpkIpdReadinessRespVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.version.SpkIpdVersionCreateReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.version.SpkIpdVersionUpdateReqVO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdFlowRunDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdMajorReleaseDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdProjectDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdVersionDO;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness.SpkIpdFlowRunMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness.SpkIpdMajorReleaseMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness.SpkIpdProjectMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness.SpkIpdVersionMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.spkdelivery.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.module.spkdelivery.service.ipdbusiness.SpkIpdBusinessConstants.*;

/**
 * IPD 业务骨架核心服务：Project / MajorRelease / Version CRUD + 状态机 + 路线图 + 就绪度。
 * 设计文档 section 2 / 9.3 / 10.2-10.3 / 11.1-11.3。
 * <p>
 * 4 级业务模型：Project -> MajorRelease(Vx) -> Version(Vx.ss) -> FlowRun。
 * 健康度由聚合规则计算，永不直接涂色；版本号结构化存整数，禁止只存字符串后拆分。
 *
 * @author SPK-OS
 */
@Slf4j
@Service
@Validated
public class SpkIpdProjectBusinessService {

    @Resource
    private SpkIpdProjectMapper projectMapper;
    @Resource
    private SpkIpdMajorReleaseMapper majorReleaseMapper;
    @Resource
    private SpkIpdVersionMapper versionMapper;
    @Resource
    private SpkIpdFlowRunMapper flowRunMapper;

    // ==================== 项目 ====================

    @Transactional(rollbackFor = Exception.class)
    public SpkIpdProjectDO createProject(SpkIpdProjectCreateReqVO req) {
        if (projectMapper.selectByProjectCode(req.getProjectCode()) != null) {
            throw exception(IPD_PROJECT_CODE_DUPLICATE);
        }
        SpkIpdProjectDO project = SpkIpdProjectDO.builder()
                .projectCode(req.getProjectCode())
                .name(req.getName())
                .description(req.getDescription())
                .objective(req.getObjective())
                .ownerUserId(req.getOwnerUserId())
                .status("DRAFT")
                .health(HEALTH_UNKNOWN)
                .plannedStartAt(req.getPlannedStartAt())
                .plannedEndAt(req.getPlannedEndAt())
                .build();
        // project_no 依赖自增 id 且列有 NOT NULL+唯一约束，先插占位值再回填真实编号
        project.setProjectNo("PRJ-TMP-" + System.nanoTime());
        projectMapper.insert(project);
        project.setProjectNo(projectNo(project.getId()));
        projectMapper.updateById(project);
        return project;
    }

    @Transactional(rollbackFor = Exception.class)
    public SpkIpdProjectDO updateProject(Long projectId, SpkIpdProjectUpdateReqVO req) {
        SpkIpdProjectDO project = getProjectOrThrow(projectId);
        if (!"DRAFT".equals(project.getStatus())) {
            throw exception(IPD_PROJECT_NOT_DRAFT);
        }
        copyNonNull(req, project, "name", "description", "objective", "ownerUserId", "plannedStartAt", "plannedEndAt");
        project.setLockVersion(req.getLockVersion());
        projectMapper.updateById(project);
        return project;
    }

    public SpkIpdProjectDO getProject(Long projectId) {
        return getProjectOrThrow(projectId);
    }

    public PageResult<SpkIpdProjectDO> pageProjects(SpkIpdProjectPageReqVO req) {
        return projectMapper.selectPage(req);
    }

    @Transactional(rollbackFor = Exception.class)
    public SpkIpdProjectDO activate(Long projectId) {
        SpkIpdProjectDO project = getProjectOrThrow(projectId);
        project.setStatus("ACTIVE");
        if (project.getActualStartAt() == null) {
            project.setActualStartAt(LocalDateTime.now());
        }
        projectMapper.updateById(project);
        return project;
    }

    @Transactional(rollbackFor = Exception.class)
    public SpkIpdProjectDO pause(Long projectId) {
        SpkIpdProjectDO project = getProjectOrThrow(projectId);
        project.setStatus("PAUSED");
        projectMapper.updateById(project);
        return project;
    }

    @Transactional(rollbackFor = Exception.class)
    public SpkIpdProjectDO archive(Long projectId) {
        SpkIpdProjectDO project = getProjectOrThrow(projectId);
        // 仅无活跃版本/流程时允许归档
        List<SpkIpdMajorReleaseDO> majors = majorReleaseMapper.selectListByProject(projectId);
        boolean hasActive = majors.stream().anyMatch(m ->
                "PLANNING".equals(m.getStatus()) || "ACTIVE".equals(m.getStatus()) || "MAINTENANCE".equals(m.getStatus()));
        if (hasActive) {
            throw exception(IPD_PROJECT_HAS_ACTIVE);
        }
        project.setStatus("ARCHIVED");
        project.setActualEndAt(LocalDateTime.now());
        projectMapper.updateById(project);
        return project;
    }

    private SpkIpdProjectDO getProjectOrThrow(Long projectId) {
        SpkIpdProjectDO p = projectMapper.selectById(projectId);
        if (p == null) {
            throw exception(IPD_PROJECT_NOT_EXISTS);
        }
        return p;
    }

    // ==================== 大版本 Vx ====================

    @Transactional(rollbackFor = Exception.class)
    public SpkIpdMajorReleaseDO createMajorRelease(Long projectId, SpkIpdMajorReleaseCreateReqVO req) {
        getProjectOrThrow(projectId);
        if (majorReleaseMapper.selectByProjectAndMajor(projectId, req.getMajorNo()) != null) {
            throw exception(IPD_MAJOR_NO_DUPLICATE);
        }
        SpkIpdMajorReleaseDO major = SpkIpdMajorReleaseDO.builder()
                .projectId(projectId)
                .majorNo(req.getMajorNo())
                .versionLabel(versionLabel(req.getMajorNo()))
                .name(req.getName())
                .objective(req.getObjective())
                .scopeSummary(req.getScopeSummary())
                .ownerUserId(req.getOwnerUserId())
                .status("PLANNING")
                .plannedStartAt(req.getBaselinePlan() == null ? null : req.getBaselinePlan().getPlannedStartAt())
                .plannedEndAt(req.getBaselinePlan() == null ? null : req.getBaselinePlan().getPlannedEndAt())
                .build();
        majorReleaseMapper.insert(major);
        // 可同时创建 Vx.0 基线版本草稿
        if (Boolean.TRUE.equals(req.getCreateBaselineVersion())) {
            SpkIpdVersionCreateReqVO bReq = new SpkIpdVersionCreateReqVO();
            bReq.setVersionType(VERSION_BASELINE);
            bReq.setMinorNo(0);
            bReq.setName(req.getName());
            bReq.setObjective(req.getObjective());
            bReq.setScopeSummary(req.getScopeSummary());
            bReq.setOwnerUserId(req.getOwnerUserId());
            if (req.getBaselinePlan() != null) {
                bReq.setPlannedStartAt(req.getBaselinePlan().getPlannedStartAt());
                bReq.setPlannedEndAt(req.getBaselinePlan().getPlannedEndAt());
            }
            SpkIpdVersionDO baseline = createVersion(projectId, major.getId(), bReq);
            major.setBaselineVersionId(baseline.getId());
            majorReleaseMapper.updateById(major);
        }
        return major;
    }

    @Transactional(rollbackFor = Exception.class)
    public SpkIpdMajorReleaseDO updateMajorRelease(Long majorReleaseId, SpkIpdMajorReleaseUpdateReqVO req) {
        SpkIpdMajorReleaseDO major = getMajorOrThrow(majorReleaseId);
        copyNonNull(req, major, "name", "objective", "scopeSummary", "ownerUserId", "plannedStartAt", "plannedEndAt");
        major.setLockVersion(req.getLockVersion());
        majorReleaseMapper.updateById(major);
        return major;
    }

    @Transactional(rollbackFor = Exception.class)
    public SpkIpdMajorReleaseDO closeMajorRelease(Long majorReleaseId) {
        SpkIpdMajorReleaseDO major = getMajorOrThrow(majorReleaseId);
        major.setStatus("CLOSED");
        major.setActualEndAt(LocalDateTime.now());
        majorReleaseMapper.updateById(major);
        return major;
    }

    public List<SpkIpdMajorReleaseDO> listMajorReleases(Long projectId) {
        getProjectOrThrow(projectId);
        return majorReleaseMapper.selectListByProject(projectId);
    }

    public SpkIpdMajorReleaseDO getMajorRelease(Long majorReleaseId) {
        return getMajorOrThrow(majorReleaseId);
    }

    private SpkIpdMajorReleaseDO getMajorOrThrow(Long majorReleaseId) {
        SpkIpdMajorReleaseDO m = majorReleaseMapper.selectById(majorReleaseId);
        if (m == null) {
            throw exception(IPD_MAJOR_NOT_EXISTS);
        }
        return m;
    }

    // ==================== 交付版本 Vx.ss ====================

    @Transactional(rollbackFor = Exception.class)
    public SpkIpdVersionDO createVersion(Long projectId, Long majorReleaseId, SpkIpdVersionCreateReqVO req) {
        SpkIpdMajorReleaseDO major = getMajorOrThrow(majorReleaseId);
        if (!major.getProjectId().equals(projectId)) {
            throw exception(IPD_CONTEXT_MISMATCH);
        }
        String vtype = req.getVersionType();
        int minorNo = req.getMinorNo() == null ? nextMinorNo(majorReleaseId, vtype) : req.getMinorNo();
        // 基线版本：minorNo 必须为 0 且同一大版本只能一个
        if (VERSION_BASELINE.equals(vtype)) {
            if (minorNo != 0) {
                throw exception(IPD_VERSION_NO_DUPLICATE);
            }
            if (versionMapper.selectBaselineByMajorRelease(majorReleaseId) != null) {
                throw exception(IPD_VERSION_BASELINE_EXISTS);
            }
        }
        if (versionMapper.selectByMajorAndMinor(projectId, major.getMajorNo(), minorNo) != null) {
            throw exception(IPD_VERSION_NO_DUPLICATE);
        }
        SpkIpdVersionDO version = SpkIpdVersionDO.builder()
                .projectId(projectId)
                .majorReleaseId(majorReleaseId)
                .majorNo(major.getMajorNo())
                .minorNo(minorNo)
                .versionNo(versionNo(major.getMajorNo(), minorNo))
                .versionType(vtype)
                .baselineFlag(VERSION_BASELINE.equals(vtype) ? 1 : 0)
                .name(req.getName())
                .objective(req.getObjective())
                .scopeSummary(req.getScopeSummary())
                .ownerUserId(req.getOwnerUserId())
                .status("DRAFT")
                .deliveryReadiness(READINESS_NOT_READY)
                .health(HEALTH_UNKNOWN)
                .sourceVersionId(req.getSourceVersionId())
                .plannedStartAt(req.getPlannedStartAt())
                .plannedEndAt(req.getPlannedEndAt())
                .build();
        versionMapper.insert(version);
        return version;
    }

    private int nextMinorNo(Long majorReleaseId, String versionType) {
        if (VERSION_BASELINE.equals(versionType)) {
            return 0;
        }
        List<SpkIpdVersionDO> list = versionMapper.selectListByMajorRelease(majorReleaseId);
        int max = 0;
        for (SpkIpdVersionDO v : list) {
            if (v.getMinorNo() != null && v.getMinorNo() > max) {
                max = v.getMinorNo();
            }
        }
        return max + 1;
    }

    @Transactional(rollbackFor = Exception.class)
    public SpkIpdVersionDO updateVersion(Long versionId, SpkIpdVersionUpdateReqVO req) {
        SpkIpdVersionDO version = getVersionOrThrow(versionId);
        copyNonNull(req, version, "name", "objective", "scopeSummary", "ownerUserId", "plannedStartAt", "plannedEndAt");
        version.setLockVersion(req.getLockVersion());
        versionMapper.updateById(version);
        return version;
    }

    public SpkIpdVersionDO getVersion(Long versionId) {
        return getVersionOrThrow(versionId);
    }

    public List<SpkIpdVersionDO> listVersions(Long majorReleaseId) {
        getMajorOrThrow(majorReleaseId);
        return versionMapper.selectListByMajorRelease(majorReleaseId);
    }

    @Transactional(rollbackFor = Exception.class)
    public SpkIpdVersionDO readyVersion(Long versionId) {
        SpkIpdVersionDO version = getVersionOrThrow(versionId);
        SpkIpdReadinessRespVO r = readiness(versionId);
        if (!Boolean.TRUE.equals(r.getReady())) {
            throw exception(IPD_FLOW_RUN_NOT_READY);
        }
        version.setStatus("READY");
        version.setDeliveryReadiness(READINESS_NOT_READY);
        versionMapper.updateById(version);
        return version;
    }

    @Transactional(rollbackFor = Exception.class)
    public SpkIpdVersionDO cancelVersion(Long versionId) {
        SpkIpdVersionDO version = getVersionOrThrow(versionId);
        if ("RELEASED".equals(version.getStatus()) || "RUNNING".equals(version.getStatus())) {
            throw exception(IPD_VERSION_RUNNING);
        }
        version.setStatus("CANCELLED");
        versionMapper.updateById(version);
        return version;
    }

    private SpkIpdVersionDO getVersionOrThrow(Long versionId) {
        SpkIpdVersionDO v = versionMapper.selectById(versionId);
        if (v == null) {
            throw exception(IPD_VERSION_NOT_EXISTS);
        }
        return v;
    }

    // ==================== 就绪度 / 预检 ====================

    /**
     * 版本启动就绪度。设计文档 section 10.3 /ready、section 10.5 preflight。
     * checks 逐项可解释：PASS/WARN/BLOCK。S1 实现：版本范围、负责人、流程类型兼容、无活跃流。
     * 外部绑定（Plane/Gitea）在 S2/S4 接入前返回 WARN，不阻断。
     */
    public SpkIpdReadinessRespVO readiness(Long versionId) {
        SpkIpdVersionDO version = getVersionOrThrow(versionId);
        SpkIpdReadinessRespVO resp = new SpkIpdReadinessRespVO();
        List<SpkIpdReadinessRespVO.Check> checks = new ArrayList<>();
        boolean ready = true;

        SpkIpdReadinessRespVO.Check scope = new SpkIpdReadinessRespVO.Check();
        scope.setCode("VERSION_SCOPE_READY");
        boolean scopeOk = !"DRAFT".equals(version.getStatus()) || version.getObjective() != null;
        scope.setStatus(scopeOk ? "PASS" : "BLOCK");
        scope.setMessage(scopeOk ? "版本范围已就绪" : "版本目标缺失");
        if (!scopeOk) ready = false;
        checks.add(scope);

        SpkIpdReadinessRespVO.Check owner = new SpkIpdReadinessRespVO.Check();
        owner.setCode("ACCOUNTABLE_OWNER");
        boolean ownerOk = version.getOwnerUserId() != null;
        owner.setStatus(ownerOk ? "PASS" : "BLOCK");
        owner.setMessage(ownerOk ? "版本负责人已绑定" : "缺少版本负责人");
        owner.setAction(ownerOk ? null : "OPEN_TEAM");
        if (!ownerOk) ready = false;
        checks.add(owner);

        SpkIpdReadinessRespVO.Check active = new SpkIpdReadinessRespVO.Check();
        active.setCode("NO_ACTIVE_FLOW");
        boolean activeOk = flowRunMapper.selectActiveByVersion(versionId).isEmpty();
        active.setStatus(activeOk ? "PASS" : "BLOCK");
        active.setMessage(activeOk ? "无活跃主交付流" : "已存在活跃主交付流");
        if (!activeOk) ready = false;
        checks.add(active);

        // 外部绑定：S1 暂不接入，WARN 不阻断
        SpkIpdReadinessRespVO.Check binding = new SpkIpdReadinessRespVO.Check();
        binding.setCode("PLANE_BINDING");
        binding.setStatus("WARN");
        binding.setMessage("Plane/Gitea 绑定校验将在 S2/S4 接入");
        checks.add(binding);

        resp.setReady(ready);
        resp.setChecks(checks);
        resp.setEffectiveStages(effectiveStages(version.getVersionType()));
        return resp;
    }

    /**
     * 按版本类型推导生效阶段。基线走完整 6 阶段；增量裁剪到 TR5；热修走问题流最小集。
     */
    private List<String> effectiveStages(String versionType) {
        if (VERSION_INCREMENT.equals(versionType)) {
            return List.of("NEEDS", "ARCH_REFRESH", "SYSTEM_DESIGN", "DEVELOP", "VERIFY_TR5");
        }
        if (VERSION_HOTFIX.equals(versionType)) {
            return List.of("ROOT_CAUSE", "FIX_DEVELOP", "VERIFY", "RELEASE");
        }
        return List.of("CONCEPT", "PLAN", "DEVELOP", "VERIFY", "LAUNCH", "LIFECYCLE");
    }

    /**
     * 路线图聚合：大版本→版本→主流程。只返回摘要，避免无界返回。
     * 设计文档 section 10.2 /roadmap。
     */
    public Map<String, Object> roadmap(Long projectId) {
        SpkIpdProjectDO project = getProjectOrThrow(projectId);
        List<SpkIpdMajorReleaseDO> majors = majorReleaseMapper.selectListByProject(projectId);
        List<Map<String, Object>> majorRows = new ArrayList<>();
        for (SpkIpdMajorReleaseDO m : majors) {
            Map<String, Object> mr = new LinkedHashMap<>();
            mr.put("majorReleaseId", m.getId());
            mr.put("versionLabel", m.getVersionLabel());
            mr.put("majorNo", m.getMajorNo());
            mr.put("name", m.getName());
            mr.put("status", m.getStatus());
            mr.put("baselineVersionId", m.getBaselineVersionId());
            List<SpkIpdVersionDO> versions = versionMapper.selectListByMajorRelease(m.getId());
            List<Map<String, Object>> verRows = versions.stream().map(v -> {
                Map<String, Object> vr = new LinkedHashMap<>();
                vr.put("versionId", v.getId());
                vr.put("versionNo", v.getVersionNo());
                vr.put("versionType", v.getVersionType());
                vr.put("baselineFlag", v.getBaselineFlag());
                vr.put("status", v.getStatus());
                vr.put("deliveryReadiness", v.getDeliveryReadiness());
                vr.put("health", v.getHealth());
                return vr;
            }).collect(Collectors.toList());
            mr.put("versions", verRows);
            majorRows.add(mr);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("projectId", project.getId());
        result.put("projectNo", project.getProjectNo());
        result.put("name", project.getName());
        result.put("status", project.getStatus());
        result.put("health", project.getHealth());
        result.put("currentMajorReleaseId", project.getCurrentMajorReleaseId());
        result.put("majorReleases", majorRows);
        return result;
    }

    // ==================== 工具 ====================

    /**
     * 仅拷贝 req 中非 null 的字段到 DO，避免覆盖未传入的业务字段。
     */
    private void copyNonNull(Object src, Object dst, String... fields) {
        try {
            for (String f : fields) {
                java.lang.reflect.Field sf = src.getClass().getDeclaredField(f);
                sf.setAccessible(true);
                Object v = sf.get(src);
                if (v != null) {
                    java.lang.reflect.Field df = dst.getClass().getDeclaredField(f);
                    df.setAccessible(true);
                    df.set(dst, v);
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("copyNonNull 失败: " + e.getMessage(), e);
        }
    }
}


