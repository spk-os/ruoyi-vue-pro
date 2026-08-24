package cn.iocoder.yudao.module.spkdelivery.service.delivery;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdProcessProfileDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdProjectDO;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness.SpkIpdProcessProfileMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SPK-OS 交付目录路径解析器。设计文档 §E（交付目录规则核心）。
 * <p>
 * 把"项目→真实文件系统路径"的解析收口到一处，确保全流程状态、产物、文档落盘位置一致、可还原、防越权。
 * <ul>
 *   <li>项目根：项目 DO.delivery_root &gt; Profile.defaultProjectRootPattern 渲染 {businessKey} &gt; 默认根</li>
 *   <li>四子目录：.flow/（流程状态） asset/（产物） src/（项目代码） docs/（长文档）</li>
 *   <li>状态文件：&lt;root&gt;/.flow/state-&lt;activityRunId&gt;.json + &lt;root&gt;/.flow/manifest.json</li>
 *   <li>产物文件：&lt;root&gt;/asset/&lt;stage&gt;/&lt;artifactId&gt;.md</li>
 * </ul>
 * <b>路径安全铁律</b>：resolve 后必须位于 Cortex 受管根目录的项目子目录内，禁 {@code ..} 与路径分隔符注入。
 * businessKey / activityRunId / artifactId 等分段禁止含 {@code /} {@code \} {@code ..}。
 *
 * @author SPK-OS
 */
@Slf4j
@Service
public class DeliveryPathResolver {

    /** 默认交付根目录前缀。 */
    public static final String DELIVERY_ROOT_BASE = "/work/SPK-OS/Delivery";

    /**
     * Cortex 允许管理的三个精确根：常规交付、开发工作区、商用部署目录。
     * 使用 {@link Path#startsWith(Path)} 做路径边界判断，避免字符串前缀绕过。
     */
    private static final List<Path> ALLOWED_ROOTS = List.of(
            Paths.get(DELIVERY_ROOT_BASE).toAbsolutePath().normalize(),
            Paths.get("/work/SPK-OS/dev").toAbsolutePath().normalize(),
            Paths.get("/work/SPK-OS/soft/spk").toAbsolutePath().normalize());

    /** 默认项目根路径模板（Profile 未配置时兜底） */
    public static final String DEFAULT_ROOT_PATTERN = "/work/SPK-OS/Delivery/project/{businessKey}";

    /** 四子目录名 */
    public static final String DIR_FLOW = ".flow";
    public static final String DIR_ASSET = "asset";
    public static final String DIR_SRC = "src";
    public static final String DIR_DOCS = "docs";

    /** 默认交付目录结构模板 JSON（.flow/asset/src/docs 树），供 Profile 初始化与前端编辑器预填 */
    public static final String DEFAULT_DIR_TEMPLATE = "["
            + "{\"name\":\".flow\",\"kind\":\"flow\",\"desc\":\"全流程状态文件，可还原任意时刻流程状态\"},"
            + "{\"name\":\"asset\",\"kind\":\"asset\",\"desc\":\"各阶段交付产物（按 stage/版本隔离）\"},"
            + "{\"name\":\"src\",\"kind\":\"src\",\"desc\":\"项目代码与构建产物\"},"
            + "{\"name\":\"docs\",\"kind\":\"docs\",\"desc\":\"长文档（Gitea 镜像 + 设计文档）\"}"
            + "]";

    /** 默认按 stage 的 skill 映射（对齐设计文档 §7 清单） */
    public static final String DEFAULT_SKILL_BINDINGS = "{"
            + "\"concept\":\"spk-ipd-concept\","
            + "\"plan\":\"spk-ipd-plan\","
            + "\"develop\":\"spk-ipd-develop\","
            + "\"qualify\":\"spk-ipd-verify\","
            + "\"launch\":\"spk-ipd-launch\","
            + "\"lifecycle\":\"spk-ipd-tr-gate\""
            + "}";

    @Value("${spk-delivery.delivery.default-root-pattern:" + DEFAULT_ROOT_PATTERN + "}")
    private String configuredRootPattern;

    @Resource
    private SpkIpdProcessProfileMapper profileMapper;

    /**
     * 解析项目交付根目录绝对路径。
     * <p>优先级：项目 DO.delivery_root &gt; Profile.defaultProjectRootPattern 渲染 {businessKey} &gt; 配置/默认根。
     * 任一来源都过 {@link #sanitizeRoot} 安全校验。
     */
    public String resolveProjectRoot(SpkIpdProjectDO project, String businessKey) {
        if (project != null && project.getDeliveryRoot() != null && !project.getDeliveryRoot().isBlank()) {
            return sanitizeRoot(project.getDeliveryRoot());
        }
        // businessKey 兜底生成
        String bk = (businessKey != null && !businessKey.isBlank()) ? businessKey
                : (project != null && project.getProjectNo() != null ? project.getProjectNo() : "unknown");
        String pattern = resolveRootPattern(project);
        return sanitizeRoot(renderPattern(pattern, bk));
    }

    /**
     * route/finish 接线点只持有 pid + businessKey（无 Project DO）。
     * <p>用 businessKey 走 Profile.defaultProjectRootPattern 渲染根目录——intake/projectService.start 一句话
     * 发起的流程无 Project 行，delivery_root 完全由 Profile 默认根渲染；治理路径（FlowRunService.start）
     * 的 Project DO 有 delivery_root 时由调用方传 Project DO 走 {@link #resolveProjectRoot}。
     * <p>查不到根目录或安全校验失败一律返回 null，绝不抛错阻断主流程（FlowStateWriter 写文件是增强非关键路径）。
     */
    public String resolveProjectRootByBusinessKey(String processInstanceId, String businessKey) {
        if (businessKey == null || businessKey.isBlank()) {
            return null;
        }
        try {
            String pattern = resolveRootPattern(null);
            return sanitizeRoot(renderPattern(pattern, businessKey));
        } catch (Exception e) {
            log.warn("[resolveProjectRootByBusinessKey][pid={} bk={} 解析根目录失败降级 null：{}]",
                    processInstanceId, businessKey, e.getMessage());
            return null;
        }
    }

    /** 取 Profile.defaultProjectRootPattern（项目无 profileId 时用全局配置/默认） */
    private String resolveRootPattern(SpkIpdProjectDO project) {
        // 项目当前主版本关联的 Profile（若治理层已绑定）
        if (project != null && project.getCurrentMajorReleaseId() != null) {
            // 简化：Profile 按 flowType 全量，这里取 BASELINE 全量发布的默认 Profile
            SpkIpdProcessProfileDO profile = profileMapper.selectByFlowType("FULL_RELEASE");
            if (profile != null && profile.getDefaultProjectRootPattern() != null
                    && !profile.getDefaultProjectRootPattern().isBlank()) {
                return profile.getDefaultProjectRootPattern();
            }
        }
        SpkIpdProcessProfileDO profile = profileMapper.selectByFlowType("FULL_RELEASE");
        if (profile != null && profile.getDefaultProjectRootPattern() != null
                && !profile.getDefaultProjectRootPattern().isBlank()) {
            return profile.getDefaultProjectRootPattern();
        }
        return configuredRootPattern != null && !configuredRootPattern.isBlank()
                ? configuredRootPattern : DEFAULT_ROOT_PATTERN;
    }

    /** 渲染 {businessKey} 占位符 */
    private String renderPattern(String pattern, String businessKey) {
        if (pattern == null || pattern.isBlank()) {
            return DEFAULT_ROOT_PATTERN.replace("{businessKey}", businessKey);
        }
        return pattern.replace("{businessKey}", businessKey);
    }

    /**
     * 安全校验 + 规范化交付根路径。禁 {@code ..}、禁相对路径、resolve 后必须以 DELIVERY_ROOT_BASE 为前缀。
     * 违规则抛 {@link IllegalArgumentException}（由调用方决定降级或拒绝）。
     */
    public String sanitizeRoot(String root) {
        if (root == null || root.isBlank()) {
            throw new IllegalArgumentException("交付根目录不能为空");
        }
        if (root.contains("..") || root.contains("\\")) {
            throw new IllegalArgumentException("交付根目录禁止含 .. 或反斜杠：" + root);
        }
        Path normalized = Paths.get(root).normalize().toAbsolutePath();
        String abs = normalized.toString();
        boolean allowed = ALLOWED_ROOTS.stream().anyMatch(normalized::startsWith);
        if (!allowed || ALLOWED_ROOTS.stream().anyMatch(normalized::equals)) {
            throw new IllegalArgumentException("交付根目录须位于受管项目子目录下：" + abs);
        }
        return abs;
    }

    /** 子目录：.flow / asset / src / docs */
    public Path resolveChild(String root, String kind) {
        String dir = switch (kind) {
            case DIR_FLOW -> DIR_FLOW;
            case DIR_ASSET -> DIR_ASSET;
            case DIR_SRC -> DIR_SRC;
            case DIR_DOCS -> DIR_DOCS;
            default -> throw new IllegalArgumentException("未知子目录类型：" + kind);
        };
        return Paths.get(root, dir);
    }

    /** 状态文件：&lt;root&gt;/.flow/state-&lt;activityRunId&gt;.json */
    public Path resolveFlowStateFile(String root, String activityRunId) {
        assertSegment(activityRunId, "activityRunId");
        return Paths.get(root, DIR_FLOW, "state-" + activityRunId + ".json");
    }

    /** 项目级 manifest：&lt;root&gt;/.flow/manifest.json */
    public Path resolveFlowManifest(String root) {
        return Paths.get(root, DIR_FLOW, "manifest.json");
    }

    /** 迭代级 FlowRun 子目录：&lt;root&gt;/.flow/&lt;flowRunId&gt;/ （每次 run 一子目录，Phase2） */
    public Path resolveFlowRunDir(String root, String flowRunId) {
        assertSegment(flowRunId, "flowRunId");
        return Paths.get(root, DIR_FLOW, flowRunId);
    }

    /** 产物文件：&lt;root&gt;/asset/&lt;stage&gt;/&lt;artifactId&gt;.md */
    public Path resolveArtifactFile(String root, String stage, String artifactId) {
        assertSegment(stage, "stage");
        assertSegment(artifactId, "artifactId");
        return Paths.get(root, DIR_ASSET, stage, artifactId + ".md");
    }

    /** docs 长文档：&lt;root&gt;/docs/&lt;stage&gt;/&lt;runId&gt;.md（Gitea 镜像到本地） */
    public Path resolveDocFile(String root, String stage, String runId) {
        assertSegment(stage, "stage");
        assertSegment(runId, "runId");
        return Paths.get(root, DIR_DOCS, stage, runId + ".md");
    }

    /** 迭代产物目录：&lt;root&gt;/asset/&lt;majorRelease&gt;/&lt;version&gt;/ （Phase2 H） */
    public Path resolveIterationDir(String root, String majorRelease, String version) {
        assertSegment(majorRelease, "majorRelease");
        assertSegment(version, "version");
        return Paths.get(root, DIR_ASSET, majorRelease, version);
    }

    /** 迭代产物文件：&lt;root&gt;/asset/&lt;majorRelease&gt;/&lt;version&gt;/&lt;stage&gt;/&lt;artifactId&gt;.md （Phase2 H） */
    public Path resolveIterationArtifactFile(String root, String majorRelease, String version,
                                             String stage, String artifactId) {
        assertSegment(majorRelease, "majorRelease");
        assertSegment(version, "version");
        assertSegment(stage, "stage");
        assertSegment(artifactId, "artifactId");
        return Paths.get(root, DIR_ASSET, majorRelease, version, stage, artifactId + ".md");
    }

    /** 迭代 FlowRun 状态文件：&lt;root&gt;/.flow/&lt;flowRunId&gt;/state-&lt;activityRunId&gt;.json（Phase2 H，每次 run 独立子目录） */
    public Path resolveFlowRunStateFile(String root, String flowRunId, String activityRunId) {
        assertSegment(flowRunId, "flowRunId");
        assertSegment(activityRunId, "activityRunId");
        return Paths.get(root, DIR_FLOW, flowRunId, "state-" + activityRunId + ".json");
    }

    /**
     * 校验单段路径不含分隔符/..（防注入）。允许字母数字 _ - . /（点只在非首尾）。
     */
    private void assertSegment(String segment, String label) {
        if (segment == null || segment.isBlank()) {
            throw new IllegalArgumentException(label + " 不能为空");
        }
        if (segment.contains("/") || segment.contains("\\") || segment.contains("..")) {
            throw new IllegalArgumentException(label + " 禁止含路径分隔符或 ..：" + segment);
        }
    }

    /** 把 manifest 写成 JSON 字符串（供 FlowStateWriter 落盘） */
    public String toJson(Object o) {
        return JsonUtils.toJsonString(o);
    }

    /** 项目级 manifest 骨架 */
    public Map<String, Object> manifestSkeleton(String projectId, String businessKey, String deliveryRoot) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("projectId", projectId);
        m.put("businessKey", businessKey);
        m.put("deliveryRoot", deliveryRoot);
        m.put("createdAt", System.currentTimeMillis());
        m.put("flowRuns", new java.util.ArrayList<>());
        m.put("iterations", new java.util.ArrayList<>());
        return m;
    }
}
