package cn.iocoder.yudao.module.spkdelivery.service.ipdoverview;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdoverview.vo.SpkIpdOverviewRespVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdoverview.vo.SpkIpdOverviewRespVO.AttentionItem;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdFlowRunDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdIssueCaseDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdMajorReleaseDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdProjectDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdVersionDO;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness.SpkIpdFlowRunMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness.SpkIpdIssueCaseMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness.SpkIpdMajorReleaseMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness.SpkIpdProjectMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness.SpkIpdVersionMapper;
import cn.iocoder.yudao.module.spkdelivery.framework.monitoring.SpkIpdMetrics;
import cn.iocoder.yudao.module.spkdelivery.service.ipdbusiness.SpkStageResolver;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;

/**
 * SPK-OS Cortext-IPD 总览聚合 Service 实现（设计文档 §9.2 / 诉求 §2）。
 * <p>
 * 计数与待办全部走既有 Mapper 的只读查询，聚到一张快照；不碰 Flowable 运行时，不新增写路径。
 * 单次首屏：projects + majors + versions + flows + issues 全量读入后内存分组（P1 数据量 < 1k 行可接受），
 * 避免发起 N 次 selectCount 往返；数据规模增长后再换 group-by SQL。
 *
 * @author SPK-OS
 */
@Service
public class SpkIpdOverviewServiceImpl implements SpkIpdOverviewService {

    @Resource
    private SpkIpdProjectMapper projectMapper;
    @Resource
    private SpkIpdMajorReleaseMapper majorReleaseMapper;
    @Resource
    private SpkIpdVersionMapper versionMapper;
    @Resource
    private SpkIpdFlowRunMapper flowRunMapper;
    @Resource
    private SpkIpdIssueCaseMapper issueCaseMapper;
    @Resource
    private SpkIpdMetrics metrics;
    @Resource
    private SpkStageResolver stageResolver;

    @Override
    public SpkIpdOverviewRespVO snapshot() {
        SpkIpdOverviewRespVO resp = new SpkIpdOverviewRespVO();

        List<SpkIpdProjectDO> projects = projectMapper.selectList(null);
        List<SpkIpdMajorReleaseDO> majors = majorReleaseMapper.selectList(null);
        List<SpkIpdVersionDO> versions = versionMapper.selectList(null);
        List<SpkIpdFlowRunDO> flows = flowRunMapper.selectList(
                new LambdaQueryWrapperX<SpkIpdFlowRunDO>()
                        .orderByDesc(SpkIpdFlowRunDO::getStartedAt).last("limit 200"));
        List<SpkIpdIssueCaseDO> issues = issueCaseMapper.selectList(
                new LambdaQueryWrapperX<SpkIpdIssueCaseDO>()
                        .orderByDesc(SpkIpdIssueCaseDO::getCreateTime).last("limit 200"));

        resp.setProjectCounts(groupCount(projects, SpkIpdProjectDO::getStatus, "UNKNOWN"));
        resp.setVersionCounts(groupCount(versions, SpkIpdVersionDO::getStatus, "DRAFT"));
        resp.setFlowRunCounts(groupCount(flows, SpkIpdFlowRunDO::getStatus, "DRAFT"));
        resp.setIssueCounts(groupCount(issues, SpkIpdIssueCaseDO::getStatus, "OPEN"));
        resp.setIssueSeverityCounts(groupCount(issues, SpkIpdIssueCaseDO::getSeverity, "P3"));
        resp.setFlowHealthCounts(groupCount(flows, SpkIpdFlowRunDO::getHealth, "UNKNOWN"));

        long activeFlows = flows.stream().filter(f -> "RUNNING".equals(f.getStatus())).count();
        long blockedFlows = flows.stream().filter(f -> "BLOCKED".equals(f.getStatus())
                || "BLOCKED".equalsIgnoreCase(String.valueOf(f.getHealth()))).count();
        long openIssues = issues.stream().filter(i -> "OPEN".equals(i.getStatus())
                || "REOPENED".equals(i.getStatus()) || "TRIAGED".equals(i.getStatus())).count();
        resp.setActiveFlowCount(activeFlows);
        resp.setBlockedFlowCount(blockedFlows);
        resp.setOpenIssueCount(openIssues);

        // AI 使用：复用内存计数器 + 当前积压（计数器可能为 0，仅作展示）
        Map<String, Object> aiUsage = new LinkedHashMap<>();
        try {
            aiUsage.putAll(metrics.snapshot());
        } catch (Exception ignore) {
            // 计数器初始化异常不影响总览
        }
        resp.setAiUsage(aiUsage);

        resp.setAttentionItems(buildAttention(flows, versions, issues));
        resp.setRecentFlows(buildRecentFlows(flows, projects));
        resp.setRoadmap(buildRoadmap(projects, majors, versions));
        // Phase2 J：迭代×子流程矩阵视图（同源 SpkStageResolver，行=迭代，列=flowType）
        resp.setIterationMatrix(buildIterationMatrix(flows, versions));
        return resp;
    }

    /**
     * Phase2 J：迭代×子流程矩阵。行=迭代（majorNo/versionNo，ISSUE_RESOLUTION 无版本归 "ISSUE" 行），
     * 列=flowType，单元格=FlowRun 状态+currentStage（SpkStageResolver 实时算）+health。
     * 与 monitor.buildIterationMatrix 同构（overview 无 artifact/evidence mapper，单元格不计产物数）。
     */
    private List<Map<String, Object>> buildIterationMatrix(List<SpkIpdFlowRunDO> flows,
                                                            List<SpkIpdVersionDO> versions) {
        java.util.Map<Long, SpkIpdVersionDO> vMap = new java.util.HashMap<>();
        if (versions != null) {
            for (SpkIpdVersionDO v : versions) {
                vMap.put(v.getId(), v);
            }
        }
        java.util.Map<String, Map<String, Object>> rowMap = new java.util.LinkedHashMap<>();
        for (SpkIpdFlowRunDO f : flows) {
            String key;
            String majorNo = null;
            String versionNo = null;
            if (f.getVersionId() != null) {
                SpkIpdVersionDO v = vMap.get(f.getVersionId());
                if (v != null) {
                    majorNo = v.getMajorNo() == null ? "?" : String.valueOf(v.getMajorNo());
                    versionNo = v.getVersionNo() == null ? "?" : v.getVersionNo();
                } else {
                    majorNo = "?";
                    versionNo = "v" + f.getVersionId();
                }
                key = majorNo + "|" + versionNo;
            } else {
                key = "ISSUE|" + (f.getIssueCaseId() == null ? "?" : f.getIssueCaseId());
            }
            Map<String, Object> row = rowMap.get(key);
            if (row == null) {
                row = new LinkedHashMap<>();
                row.put("iterationKey", key);
                row.put("majorNo", majorNo);
                row.put("versionNo", versionNo);
                row.put("versionId", f.getVersionId());
                row.put("issueCaseId", f.getIssueCaseId());
                row.put("projectId", f.getProjectId());
                row.put("cells", new LinkedHashMap<String, Map<String, Object>>());
                rowMap.put(key, row);
            }
            @SuppressWarnings("unchecked")
            Map<String, Map<String, Object>> cells = (Map<String, Map<String, Object>>) row.get("cells");
            Map<String, Object> cell = new LinkedHashMap<>();
            cell.put("flowRunId", f.getId());
            cell.put("runNo", f.getRunNo());
            cell.put("status", f.getStatus());
            cell.put("currentStage", stageResolver.resolveCurrentStage(
                    f.getProcessInstanceId(), f.getCurrentStage(), f.getStatus()));
            cell.put("health", f.getHealth());
            cells.put(f.getFlowType(), cell);
        }
        return new ArrayList<>(rowMap.values());
    }

    /** 按字段分组计数，空值归入 defaultKey。 */
    private <T> Map<String, Long> groupCount(List<T> list,
                                             java.util.function.Function<T, String> keyFn, String defaultKey) {
        return list.stream().collect(Collectors.groupingBy(
                x -> { String k = keyFn.apply(x); return k == null ? defaultKey : k; },
                LinkedHashMap::new, Collectors.counting()));
    }

    private List<AttentionItem> buildAttention(List<SpkIpdFlowRunDO> flows,
                                                List<SpkIpdVersionDO> versions,
                                                List<SpkIpdIssueCaseDO> issues) {
        List<AttentionItem> items = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        // 阻断流程
        flows.stream().filter(f -> "BLOCKED".equals(f.getStatus())).forEach(f -> {
            AttentionItem it = new AttentionItem();
            it.setType("BLOCKED_FLOW"); it.setSeverity("CRITICAL"); it.setRefId(f.getId());
            it.setTitle("流程 " + (f.getRunNo() == null ? f.getId() : f.getRunNo()) + " 被阻断");
            it.setDetail(f.getBlockReason() == null ? "需人工介入" : f.getBlockReason());
            items.add(it);
        });
        // 逾期版本（计划完成 < now 且未 RELEASED/CANCELLED）
        versions.stream().filter(v -> v.getPlannedEndAt() != null && v.getPlannedEndAt().isBefore(now)
                && !"RELEASED".equals(v.getStatus()) && !"CANCELLED".equals(v.getStatus())).forEach(v -> {
            AttentionItem it = new AttentionItem();
            it.setType("OVERDUE_VERSION"); it.setSeverity("WARN"); it.setRefId(v.getId());
            it.setTitle("版本 " + v.getVersionNo() + " 已逾期");
            it.setDetail("计划完成 " + v.getPlannedEndAt() + "，当前 " + v.getStatus());
            items.add(it);
        });
        // P0/P1 未关闭问题
        issues.stream().filter(i -> ("P0".equals(i.getSeverity()) || "P1".equals(i.getSeverity()))
                && !"CLOSED".equals(i.getStatus())).forEach(i -> {
            AttentionItem it = new AttentionItem();
            it.setType("SEVERE_ISSUE"); it.setSeverity(i.getSeverity()); it.setRefId(i.getId());
            it.setTitle("问题 " + i.getCaseNo() + " " + i.getSeverity());
            it.setDetail(i.getTitle() == null ? "" : i.getTitle());
            items.add(it);
        });
        items.sort(Comparator.comparing(AttentionItem::getSeverity));
        return items;
    }

    private List<Map<String, Object>> buildRecentFlows(List<SpkIpdFlowRunDO> flows,
                                                        List<SpkIpdProjectDO> projects) {
        Map<Long, SpkIpdProjectDO> projMap = convertMap(projects, SpkIpdProjectDO::getId);
        return flows.stream().sorted(Comparator
                .comparing(SpkIpdFlowRunDO::getStartedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(8).map(f -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", f.getId());
                    m.put("runNo", f.getRunNo());
                    m.put("flowType", f.getFlowType());
                    m.put("status", f.getStatus());
                    // Bug2-A：currentStage 实时算（DO 静态值永停 concept，读端覆盖；与 buildCard 同源 SpkStageResolver）
                    m.put("currentStage", stageResolver.resolveCurrentStage(f.getProcessInstanceId(), f.getCurrentStage(), f.getStatus()));
                    m.put("health", f.getHealth());
                    m.put("startedAt", f.getStartedAt());
                    SpkIpdProjectDO p = f.getProjectId() == null ? null : projMap.get(f.getProjectId());
                    m.put("projectName", p == null ? null : p.getName());
                    return m;
                }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildRoadmap(List<SpkIpdProjectDO> projects,
                                                    List<SpkIpdMajorReleaseDO> majors,
                                                    List<SpkIpdVersionDO> versions) {
        Map<Long, List<SpkIpdMajorReleaseDO>> majorsByProject = majors.stream()
                .collect(Collectors.groupingBy(SpkIpdMajorReleaseDO::getProjectId));
        Map<Long, List<SpkIpdVersionDO>> versionsByMajor = versions.stream()
                .collect(Collectors.groupingBy(SpkIpdVersionDO::getMajorReleaseId));
        return projects.stream()
                .filter(p -> "ACTIVE".equals(p.getStatus()))
                .sorted(Comparator.comparing(SpkIpdProjectDO::getId).reversed())
                .limit(6).map(p -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("projectId", p.getId());
                    m.put("projectName", p.getName());
                    m.put("projectCode", p.getProjectCode());
                    m.put("health", p.getHealth());
                    m.put("ownerUserId", p.getOwnerUserId());
                    List<Map<String, Object>> majorList = new ArrayList<>();
                    for (SpkIpdMajorReleaseDO mj : majorsByProject.getOrDefault(p.getId(), new ArrayList<>())) {
                        Map<String, Object> mm = new LinkedHashMap<>();
                        mm.put("majorReleaseId", mj.getId());
                        mm.put("majorNo", mj.getMajorNo());
                        mm.put("name", mj.getName());
                        mm.put("status", mj.getStatus());
                        List<Map<String, Object>> vlist = new ArrayList<>();
                        for (SpkIpdVersionDO v : versionsByMajor.getOrDefault(mj.getId(), new ArrayList<>())) {
                            Map<String, Object> vm = new LinkedHashMap<>();
                            vm.put("versionId", v.getId());
                            vm.put("versionNo", v.getVersionNo());
                            vm.put("versionType", v.getVersionType());
                            vm.put("status", v.getStatus());
                            vlist.add(vm);
                        }
                        mm.put("versions", vlist);
                        majorList.add(mm);
                    }
                    m.put("majors", majorList);
                    return m;
                }).collect(Collectors.toList());
    }
}
