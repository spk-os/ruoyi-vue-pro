package cn.iocoder.yudao.module.spkdelivery.service.context;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdactivity.SpkIpdActivityDefDO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SPK-OS Context Builder —— 装配 ContextManifest（Evidence 三件套之一：输入）
 * <p>
 * 汇聚 activity def 元数据 + 输入产物引用 + knowledge/memory/information 占位 + scratchpad_uri，
 * 落本地文件（OPEN-3 兜底，后续可替换 MinIO），返回 URI 供 TaskContract 引用。依 §5.4。
 *
 * @author SPK-OS
 */
@Slf4j
@Service
public class SpkContextBuilderService {

    /** 上下文产物落地根目录（OPEN-3：本地文件系统过渡） */
    @Value("${spk-delivery.artifact.base-dir:/work/SPK-OS/artifacts}")
    private String baseDir;

    /**
     * 装配并持久化 ContextManifest。
     *
     * @param activityRunId    ActivityRun id
     * @param activityDef      Activity 定义
     * @param inputRefs        输入产物引用（artifactId 列表）
     * @return manifest 文件 URI（file://...）
     */
    public String build(String activityRunId, SpkIpdActivityDefDO activityDef, List<String> inputRefs) {
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("schemaVersion", "1.0");
        manifest.put("activityRunId", activityRunId);
        manifest.put("activityId", activityDef.getActivityId());
        manifest.put("activityVersion", activityDef.getVersion());
        manifest.put("stage", activityDef.getStage());
        manifest.put("name", activityDef.getName());
        manifest.put("leadAgentCode", activityDef.getLeadAgentCode());
        manifest.put("modelCapabilities", parseArray(activityDef.getModelCapabilities()));
        manifest.put("skills", parseArray(activityDef.getSkills()));
        manifest.put("tools", parseArray(activityDef.getTools()));
        manifest.put("knowledgeRefs", parseArray(activityDef.getKnowledgeRefs()));
        manifest.put("informationRefs", parseArray(activityDef.getInformationRefs()));
        manifest.put("memoryScope", parseArray(activityDef.getMemoryScope()));
        manifest.put("inputArtifactTypes", parseArray(activityDef.getInputArtifactTypes()));
        manifest.put("outputArtifactType", activityDef.getOutputArtifactType());
        manifest.put("acceptanceCriteria", parseArray(activityDef.getAcceptanceCriteria()));
        manifest.put("inputRefs", inputRefs);
        manifest.put("promptTemplate", activityDef.getPromptTemplate());
        // scratchpad：同 run 目录下的可写区
        manifest.put("scratchpadUri", "file://" + baseDir + "/" + activityRunId + "/scratchpad.json");
        manifest.put("builtAt", LocalDateTime.now().toString());

        String json = JsonUtils.toJsonString(manifest);
        return persist(activityRunId, "context-manifest.json", json);
    }

    /**
     * 落盘并返回 file:// URI。
     */
    private String persist(String activityRunId, String fileName, String content) {
        try {
            Path dir = Paths.get(baseDir, activityRunId);
            Files.createDirectories(dir);
            Path file = dir.resolve(fileName);
            Files.writeString(file, content, StandardCharsets.UTF_8);
            String uri = "file://" + file.toAbsolutePath();
            log.info("[persist][activityRunId={} uri={} bytes={}]", activityRunId, uri, content.length());
            return uri;
        } catch (Exception e) {
            throw new RuntimeException("持久化 ContextManifest 失败: " + activityRunId, e);
        }
    }

    public static String sha256(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("计算 SHA-256 失败", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> parseArray(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return JsonUtils.parseObject(json, List.class);
        } catch (Exception e) {
            return List.of(json);
        }
    }

}
