package cn.iocoder.yudao.module.spkdelivery.service.ipdbusiness;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
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
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * IPD 全局上下文选择器后端服务。设计文档 §5.4。
 * <p>
 * 三级选择器：项目 / 版本(Vx.ss) / 流程(FR)。项目必选；版本和流程允许“全部”。
 * 用户最近 5 个上下文保存在个人偏好——S1 用进程内 ConcurrentDeque 兜底（重启丢失），
 * S2 接入持久化偏好表后迁移；接口契约不变。
 *
 * @author SPK-OS
 */
@Slf4j
@Service
@Validated
public class SpkIpdContextSelectorService {

    @Resource
    private SpkIpdProjectMapper projectMapper;
    @Resource
    private SpkIpdMajorReleaseMapper majorReleaseMapper;
    @Resource
    private SpkIpdVersionMapper versionMapper;
    @Resource
    private SpkIpdFlowRunMapper flowRunMapper;

    /** 每用户最近上下文（最多 5 条），S1 进程内兜底 */
    private final Map<Long, ConcurrentLinkedDeque<Map<String, Object>>> recentByUser = new ConcurrentHashMap<>();

    /** 选择器用的项目轻量列表（id/projectNo/name/status/health） */
    public List<Map<String, Object>> listProjectsForPicker() {
        List<SpkIpdProjectDO> all = projectMapper.selectList(null);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (SpkIpdProjectDO p : all) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("projectId", p.getId());
            m.put("projectNo", p.getProjectNo());
            m.put("name", p.getName());
            m.put("status", p.getStatus());
            m.put("health", p.getHealth());
            rows.add(m);
        }
        return rows;
    }

    /** 选择器用的版本列表：按大版本分组，含 Vx.ss 全量（含已取消，前端置灰） */
    public List<Map<String, Object>> listVersionsForPicker(Long projectId) {
        if (projectId == null) {
            return Collections.emptyList();
        }
        List<SpkIpdMajorReleaseDO> majors = majorReleaseMapper.selectListByProject(projectId);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (SpkIpdMajorReleaseDO m : majors) {
            List<SpkIpdVersionDO> versions = versionMapper.selectListByMajorRelease(m.getId());
            for (SpkIpdVersionDO v : versions) {
                Map<String, Object> vm = new LinkedHashMap<>();
                vm.put("versionId", v.getId());
                vm.put("versionNo", v.getVersionNo());
                vm.put("versionType", v.getVersionType());
                vm.put("baselineFlag", v.getBaselineFlag());
                vm.put("majorReleaseId", m.getId());
                vm.put("versionLabel", m.getVersionLabel());
                vm.put("status", v.getStatus());
                rows.add(vm);
            }
        }
        return rows;
    }

    /** 选择器用的流程列表：按项目/版本过滤（仅非终态） */
    public List<Map<String, Object>> listFlowRunsForPicker(Long projectId, Long versionId) {
        List<SpkIpdFlowRunDO> all = flowRunMapper.selectList(null);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (SpkIpdFlowRunDO r : all) {
            if (projectId != null && !projectId.equals(r.getProjectId())) {
                continue;
            }
            if (versionId != null && !versionId.equals(r.getVersionId())) {
                continue;
            }
            if ("CANCELLED".equals(r.getStatus()) || "SUPERSEDED".equals(r.getStatus())) {
                continue;
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("flowRunId", r.getId());
            m.put("runNo", r.getRunNo());
            m.put("flowType", r.getFlowType());
            m.put("status", r.getStatus());
            m.put("currentStage", r.getCurrentStage());
            m.put("versionId", r.getVersionId());
            m.put("issueCaseId", r.getIssueCaseId());
            rows.add(m);
        }
        return rows;
    }

    /** 用户最近 5 个上下文 */
    public List<Map<String, Object>> recentContexts() {
        Long uid = currentUserId();
        ConcurrentLinkedDeque<Map<String, Object>> deque = recentByUser.get(uid);
        if (deque == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(deque);
    }

    /** 记录当前上下文到个人偏好（去重、最多 5 条） */
    public List<Map<String, Object>> saveRecentContext(Map<String, Object> ctx) {
        Long uid = currentUserId();
        ConcurrentLinkedDeque<Map<String, Object>> deque =
                recentByUser.computeIfAbsent(uid, k -> new ConcurrentLinkedDeque<>());
        Map<String, Object> entry = new LinkedHashMap<>(ctx);
        entry.put("visitedAt", LocalDateTime.now().toString());
        // 去重：同 projectId+versionId+flowRunId 视为同一上下文
        String key = ctxKey(entry);
        deque.removeIf(m -> key.equals(ctxKey(m)));
        deque.addFirst(entry);
        while (deque.size() > 5) {
            deque.removeLast();
        }
        return new ArrayList<>(deque);
    }

    private String ctxKey(Map<String, Object> m) {
        return str(m.get("projectId")) + "|" + str(m.get("versionId")) + "|" + str(m.get("flowRunId"));
    }

    private String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private Long currentUserId() {
        Long uid = SecurityFrameworkUtils.getLoginUserId();
        return uid != null ? uid : 1L;
    }
}
