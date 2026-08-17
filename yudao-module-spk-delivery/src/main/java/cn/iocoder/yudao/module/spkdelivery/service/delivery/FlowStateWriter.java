package cn.iocoder.yudao.module.spkdelivery.service.delivery;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SPK-OS 流程状态写盘器。设计文档 §E（交付目录规则核心）。
 * <p>
 * 把每个 Activity 的状态原子追加写到 {@code <root>/.flow/state-<activityRunId>.json}，并维护项目级
 * {@code <root>/.flow/manifest.json} 索引——后者聚合所有 FlowRun/pid/阶段/产物/evidence 指针，可还原全流程。
 * <p>
 * <b>铁律</b>：本类只做文件 IO，<b>绝不调 {@code runtimeService.setVariables}</b>（遵守
 * {@code [[flowable-sync-trigger-deadlock]]} 互锁铁律——route 线程内只写文件，不触 BPM 写锁）。
 * <b>并发安全</b>：先写 .tmp 再 atomic rename 防并发写坏；按 root 加对象锁串行化同项目写（不同项目天然隔离）。
 * 所有写失败一律 try/catch 降级记 warn，<b>不阻断主流程</b>（状态落盘是增强，DB 才是关键路径）。
 *
 * @author SPK-OS
 */
@Slf4j
@Service
public class FlowStateWriter {

    @Resource
    private DeliveryPathResolver pathResolver;

    /** 按 root 串行化写（同项目内顺序写，跨项目并发） */
    private final Map<String, Object> rootLocks = new ConcurrentHashMap<>();

    private Object lockFor(String root) {
        return rootLocks.computeIfAbsent(root, k -> new Object());
    }

    /**
     * 原子追加写一次 Activity 状态到 {@code <root>/.flow/state-<activityRunId>.json}，并更新 manifest 索引。
     * <p>route 标记 done / finish 终态 / 阶段推进 时调用。写失败降级，不抛错。
     *
     * @param processInstanceId 流程实例 id
     * @param businessKey       业务键（解析根目录用）
     * @param state             状态载荷（stage/activityId/activityRunId/contractId/artifactId/...）
     */
    public void appendState(String processInstanceId, String businessKey, Map<String, Object> state) {
        if (state == null || processInstanceId == null) {
            return;
        }
        String root = pathResolver.resolveProjectRootByBusinessKey(processInstanceId, businessKey);
        if (root == null) {
            return;
        }
        Object activityRunId = state.get("activityRunId");
        String runId = activityRunId != null ? activityRunId.toString() : "anon-" + System.currentTimeMillis();
        try {
            synchronized (lockFor(root)) {
                // 1. 确保子目录存在
                Path flowDir = pathResolver.resolveChild(root, DeliveryPathResolver.DIR_FLOW);
                Files.createDirectories(flowDir);
                // 2. 原子写 state 文件（先 .tmp 再 rename）
                Path stateFile = pathResolver.resolveFlowStateFile(root, runId);
                writeAtomic(stateFile, JsonUtils.toJsonString(state));
                // 3. 更新 manifest 索引
                updateManifest(root, processInstanceId, businessKey, state, runId);
            }
        } catch (Exception e) {
            log.warn("[appendState][pid={} runId={} 写 .flow 状态失败降级：{}]",
                    processInstanceId, runId, e.getMessage());
        }
    }

    /**
     * 项目启动时初始化交付目录骨架：创建 .flow/ asset/ src/ docs/ 四子目录 + project.yaml + manifest.json。
     * <p>幂等：已存在则跳过。写失败抛 RuntimeException 让调用方感知（启动期目录是后续落盘前提）。
     */
    public void provisionProject(String businessKey, String root, String projectName,
                                 String flowKey, String mode) {
        if (root == null) {
            throw new IllegalArgumentException("交付根目录不能为空");
        }
        try {
            synchronized (lockFor(root)) {
                Files.createDirectories(pathResolver.resolveChild(root, DeliveryPathResolver.DIR_FLOW));
                Files.createDirectories(pathResolver.resolveChild(root, DeliveryPathResolver.DIR_ASSET));
                Files.createDirectories(pathResolver.resolveChild(root, DeliveryPathResolver.DIR_SRC));
                Files.createDirectories(pathResolver.resolveChild(root, DeliveryPathResolver.DIR_DOCS));
                // project.yaml：项目元信息（businessKey/flowKey/adapter/phases/deliveryRoot）
                Map<String, Object> projectYaml = new LinkedHashMap<>();
                projectYaml.put("businessKey", businessKey);
                projectYaml.put("name", projectName);
                projectYaml.put("flowKey", flowKey);
                projectYaml.put("mode", mode);
                projectYaml.put("deliveryRoot", root);
                projectYaml.put("createdAt", System.currentTimeMillis());
                writeAtomic(Path.of(root, "project.yaml"), toYamlLike(projectYaml));
                // manifest.json 骨架（若已存在不覆盖，保留历史）
                Path manifest = pathResolver.resolveFlowManifest(root);
                if (!Files.exists(manifest)) {
                    Map<String, Object> skel = pathResolver.manifestSkeleton(null, businessKey, root);
                    writeAtomic(manifest, JsonUtils.toJsonString(skel));
                }
            }
            log.info("[provisionProject][businessKey={} root={} 四子目录+project.yaml+manifest.json 已就绪]",
                    businessKey, root);
        } catch (Exception e) {
            throw new RuntimeException("初始化交付目录失败: " + root, e);
        }
    }

    /** 产物全文镜像到 &lt;root&gt;/asset/&lt;stage&gt;/&lt;artifactId&gt;.md（SpkArtifactService.register 调） */
    public void mirrorArtifact(String root, String stage, String artifactId, String content) {
        if (root == null || content == null) {
            return;
        }
        try {
            Path file = pathResolver.resolveArtifactFile(root, stage, artifactId);
            Files.createDirectories(file.getParent());
            writeAtomic(file, content);
        } catch (Exception e) {
            log.warn("[mirrorArtifact][root={} stage={} artifactId={} 镜像产物失败降级：{}]",
                    root, stage, artifactId, e.getMessage());
        }
    }

    /** docs 长文档镜像到 &lt;root&gt;/docs/&lt;stage&gt;/&lt;runId&gt;.md */
    public void mirrorDoc(String root, String stage, String runId, String content) {
        if (root == null || content == null) {
            return;
        }
        try {
            Path file = pathResolver.resolveDocFile(root, stage, runId);
            Files.createDirectories(file.getParent());
            writeAtomic(file, content);
        } catch (Exception e) {
            log.warn("[mirrorDoc][root={} stage={} runId={} 镜像文档失败降级：{}]",
                    root, stage, runId, e.getMessage());
        }
    }

    // ===== 内部 =====

    /** 原子写：先写 .tmp 再 rename（防并发写坏） */
    private void writeAtomic(Path target, String content) throws Exception {
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        Files.writeString(tmp, content == null ? "" : content, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    /** 更新 manifest：追加/合并 flowRuns 条目（按 pid 去重，同 pid 更新最新阶段/状态/产物指针） */
    @SuppressWarnings("unchecked")
    private void updateManifest(String root, String pid, String businessKey,
                                Map<String, Object> state, String runId) throws Exception {
        Path manifest = pathResolver.resolveFlowManifest(root);
        Map<String, Object> manifestMap;
        if (Files.exists(manifest)) {
            String raw = Files.readString(manifest, StandardCharsets.UTF_8);
            manifestMap = JsonUtils.parseObject(raw, Map.class);
            if (manifestMap == null) {
                manifestMap = pathResolver.manifestSkeleton(null, businessKey, root);
            }
        } else {
            manifestMap = pathResolver.manifestSkeleton(null, businessKey, root);
        }
        List<Map<String, Object>> flowRuns = (List<Map<String, Object>>) manifestMap
                .computeIfAbsent("flowRuns", k -> new ArrayList<>());
        // 按 pid 合并：找到同 pid 条目则更新，否则追加
        Map<String, Object> entry = null;
        for (Map<String, Object> fr : flowRuns) {
            if (pid.equals(fr.get("pid"))) {
                entry = fr;
                break;
            }
        }
        if (entry == null) {
            entry = new LinkedHashMap<>();
            entry.put("pid", pid);
            entry.put("businessKey", businessKey);
            entry.put("states", new ArrayList<>());
            flowRuns.add(entry);
        }
        entry.put("currentStage", state.get("stage"));
        entry.put("currentActivity", state.get("activityId"));
        entry.put("status", state.get("status"));
        entry.put("updatedAt", System.currentTimeMillis());
        List<Map<String, Object>> states = (List<Map<String, Object>>) entry.get("states");
        if (states == null) {
            states = new ArrayList<>();
            entry.put("states", states);
        }
        // 追加本次状态快照指针（轻量，只存 runId + stage + verdict + timestamp）
        Map<String, Object> snap = new LinkedHashMap<>();
        snap.put("activityRunId", runId);
        snap.put("stage", state.get("stage"));
        snap.put("activityId", state.get("activityId"));
        snap.put("contractId", state.get("contractId"));
        snap.put("artifactId", state.get("artifactId"));
        snap.put("evidenceRunId", state.get("evidenceRunId"));
        snap.put("verdict", state.get("verdict"));
        snap.put("operator", state.get("operator"));
        snap.put("timestamp", state.get("timestamp"));
        states.add(snap);
        manifestMap.put("flowRuns", flowRuns);
        writeAtomic(manifest, JsonUtils.toJsonString(manifestMap));
    }

    /** 极简 YAML-like 序列化（避免引入 snakeyaml 依赖；project.yaml 仅人工/工具可读） */
    private String toYamlLike(Map<String, Object> m) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> e : m.entrySet()) {
            sb.append(e.getKey()).append(": ").append(e.getValue()).append('\n');
        }
        return sb.toString();
    }

    /** 读取 manifest（供 monitor/overview 读端聚合还原全流程，Phase2 用） */
    @SuppressWarnings("unchecked")
    public Map<String, Object> readManifest(String root) {
        if (root == null) {
            return Collections.emptyMap();
        }
        try {
            Path manifest = pathResolver.resolveFlowManifest(root);
            if (!Files.exists(manifest)) {
                return Collections.emptyMap();
            }
            String raw = Files.readString(manifest, StandardCharsets.UTF_8);
            Map<String, Object> m = JsonUtils.parseObject(raw, Map.class);
            return m == null ? Collections.emptyMap() : m;
        } catch (Exception e) {
            log.warn("[readManifest][root={} 读 manifest 失败：{}]", root, e.getMessage());
            return Collections.emptyMap();
        }
    }
}
