package cn.iocoder.yudao.module.spkdelivery.service.artifact;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.artifact.SpkArtifactManifestDO;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.artifact.SpkArtifactManifestMapper;
import cn.iocoder.yudao.module.spkdelivery.enums.SpkArtifactStatusEnum;
import cn.iocoder.yudao.module.spkdelivery.service.context.SpkContextBuilderService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.spkdelivery.enums.ErrorCodeConstants.ARTIFACT_HASH_MISMATCH;
import static cn.iocoder.yudao.module.spkdelivery.enums.ErrorCodeConstants.ARTIFACT_NOT_EXISTS;

/**
 * SPK-OS Artifact Service —— 产物不可变登记中心（Evidence 三件套之二：产物）
 * <p>
 * register(draft) → sign → 版本递增 → hash 校验 → secret 扫描桩。一旦 signed 不得修改，
 * 只能新增版本。同 activity_run_id + artifact_type + version 唯一。依 §4.3 / §5.5。
 *
 * @author SPK-OS
 */
@Slf4j
@Service
public class SpkArtifactService {

    @Value("${spk-delivery.artifact.base-dir:/work/SPK-OS/artifacts}")
    private String baseDir;

    @Resource
    private SpkArtifactManifestMapper artifactMapper;

    /**
     * 登记 draft 产物（Lead 写入）。
     *
     * @param activityRunId   ActivityRun id
     * @param processInstanceId 流程实例 id
     * @param artifactType    产物类型
     * @param content         产物正文（JSON 字符串）
     * @param summary         一句话摘要
     * @return 写入后的 manifest
     */
    public SpkArtifactManifestDO register(String activityRunId, String processInstanceId,
                                          String artifactType, String content, String summary) {
        // 1. 落盘产物正文
        String fileName = artifactType + ".json";
        String uri = persist(activityRunId, fileName, content);
        String contentHash = SpkContextBuilderService.sha256(content);
        // 2. 版本递增：取同 run + type 最大 version
        SpkArtifactManifestDO latest = artifactMapper.selectLatestByRunAndType(activityRunId, artifactType);
        int version = (latest != null && latest.getVersion() != null) ? latest.getVersion() + 1 : 1;
        // 3. 写 draft 记录
        String artifactId = "art-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        SpkArtifactManifestDO manifest = SpkArtifactManifestDO.builder()
                .artifactId(artifactId)
                .artifactType(artifactType)
                .activityRunId(activityRunId)
                .processInstanceId(processInstanceId)
                .uri(uri)
                .contentHash(contentHash)
                .version(version)
                .status(SpkArtifactStatusEnum.DRAFT.getLabel())
                .signerRequired(0)
                .metadata(content)
                .mime("application/json")
                .bytes((long) content.getBytes(StandardCharsets.UTF_8).length)
                .classification("internal")
                .scanStatus("pending")
                .supersedes(latest != null ? latest.getArtifactId() : null)
                .summary(summary)
                .build();
        artifactMapper.insert(manifest);
        log.info("[register][activityRunId={} type={} v={} artifactId={}]",
                activityRunId, artifactType, version, artifactId);
        return manifest;
    }

    /**
     * 签署产物（draft → signed，不可逆）。
     */
    public SpkArtifactManifestDO sign(String artifactId, String signedBy) {
        SpkArtifactManifestDO manifest = validateExists(artifactId);
        if (SpkArtifactStatusEnum.SIGNED.getLabel().equals(manifest.getStatus())) {
            return manifest;
        }
        // hash 校验：重读产物正文比对 contentHash
        if (!verifyHash(manifest)) {
            throw exception(ARTIFACT_HASH_MISMATCH);
        }
        manifest.setStatus(SpkArtifactStatusEnum.SIGNED.getLabel());
        manifest.setSignedBy(signedBy);
        manifest.setSignedAt(LocalDateTime.now());
        manifest.setScanStatus("clean");
        artifactMapper.updateById(manifest);
        log.info("[sign][artifactId={} by={}]", artifactId, signedBy);
        return manifest;
    }

    /**
     * 校验产物正文 hash 与登记 hash 一致（防篡改）。
     */
    public boolean verifyHash(SpkArtifactManifestDO manifest) {
        try {
            String content = readContent(manifest.getUri());
            String actual = SpkContextBuilderService.sha256(content);
            if (!actual.equals(manifest.getContentHash())) {
                log.warn("[verifyHash][mismatch artifactId={} expected={} actual={}]",
                        manifest.getArtifactId(), manifest.getContentHash(), actual);
                return false;
            }
            return true;
        } catch (Exception e) {
            log.error("[verifyHash][artifactId={} read fail]", manifest.getArtifactId(), e);
            return false;
        }
    }

    public SpkArtifactManifestDO validateExists(String artifactId) {
        SpkArtifactManifestDO manifest = artifactMapper.selectByArtifactId(artifactId);
        if (manifest == null) {
            throw exception(ARTIFACT_NOT_EXISTS);
        }
        return manifest;
    }

    public List<SpkArtifactManifestDO> listByActivityRunId(String activityRunId) {
        return artifactMapper.selectListByActivityRunId(activityRunId);
    }

    public List<SpkArtifactManifestDO> listByProcessInstanceId(String processInstanceId) {
        return artifactMapper.selectListByProcessInstanceId(processInstanceId);
    }

    private String persist(String activityRunId, String fileName, String content) {
        try {
            Path dir = Paths.get(baseDir, activityRunId);
            Files.createDirectories(dir);
            Path file = dir.resolve(fileName);
            Files.writeString(file, content, StandardCharsets.UTF_8);
            return "file://" + file.toAbsolutePath();
        } catch (Exception e) {
            throw new RuntimeException("持久化 Artifact 失败: " + activityRunId + "/" + fileName, e);
        }
    }

    private String readContent(String uri) throws Exception {
        if (uri == null || !uri.startsWith("file://")) {
            throw new IllegalStateException("仅支持 file:// URI: " + uri);
        }
        Path path = Paths.get(uri.substring("file://".length()));
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    /**
     * 摘要载荷（写入 Evidence 用）。
     */
    public Map<String, Object> summarize(SpkArtifactManifestDO m) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("artifactId", m.getArtifactId());
        map.put("artifactType", m.getArtifactType());
        map.put("version", m.getVersion());
        map.put("contentHash", m.getContentHash());
        map.put("status", m.getStatus());
        map.put("summary", m.getSummary());
        return map;
    }

    public String toJson(Object o) {
        return JsonUtils.toJsonString(o);
    }

}
