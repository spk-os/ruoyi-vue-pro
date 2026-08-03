package cn.iocoder.yudao.module.spkdelivery.dal.dataobject.artifact;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Artifact Manifest DO —— 产物不可变登记中心
 * <p>
 * 一旦 status=signed 不得修改，只能新增版本（version+1，supersedes 指旧版）。
 * 同 activity_run_id + artifact_type + version 唯一。依 §4.3。
 *
 * @author SPK-OS
 */
@TableName("spk_artifact_manifest")
@KeySequence("spk_artifact_manifest_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpkArtifactManifestDO extends BaseDO {

    @TableId
    private Long id;
    private String artifactId;
    /**
     * 产物类型，如 OpportunitySignalSet / ConceptOptionSet / PRSBaseline
     */
    private String artifactType;
    private String activityRunId;
    private String processInstanceId;
    /**
     * 存储 URI，如 file://spk-artifacts/{run}/{type}.json
     */
    private String uri;
    /**
     * 内容 SHA-256
     */
    private String contentHash;
    private Integer version;
    /**
     * 状态 draft/signed/superseded/quarantined
     */
    private String status;
    private Integer signerRequired;
    private String signedBy;
    private LocalDateTime signedAt;
    /**
     * 依赖 JSON
     */
    private String dependencies;
    /**
     * 元数据 JSON
     */
    private String metadata;
    private String mime;
    private Long bytes;
    /**
     * 密级 public/internal/confidential/secret
     */
    private String classification;
    /**
     * 扫描状态 pending/clean/quarantined
     */
    private String scanStatus;
    /**
     * 上一版本 artifact_id
     */
    private String supersedes;
    /**
     * 摘要（产物一句话描述，前端列表展示用）
     */
    private String summary;

}
