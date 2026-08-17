package cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * IPD 流程模板 Profile DO。设计文档 §9.5.1 / §7.2 第 4 顶层表面。
 * <p>
 * 管理员配置产物（非运行数据）。一个 flow_type 对应一个已发布 Profile；
 * current_version 指向 {@link SpkIpdProcessProfileVersionDO} 当前发布版本。
 * status: DRAFT/PUBLISHED/DEPRECATED。
 *
 * @author SPK-OS
 */
@TableName("spk_ipd_process_profile")
@KeySequence("spk_ipd_process_profile_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpkIpdProcessProfileDO extends BaseDO {

    @TableId
    private Long id;
    /** 模板编码，如 FULL_RELEASE_V1；唯一 */
    private String profileCode;
    private String name;
    /** FULL_RELEASE/INCREMENT_RELEASE/ISSUE_RESOLUTION */
    private String flowType;
    /**
     * 该 flowType 绑定的 BPM 流程定义 key（如 spkIpdFlowFull）。
     * <p>建 Profile 时由 {@code SpkIpdBusinessConstants.flowKeyOf(flowType)} 自动填入，不可手改。
     * 修 G1/G3：治理层与 BPM 执行层的绑定字段。
     */
    private String processDefinitionKey;
    private String description;
    private String status;
    /** 当前发布版本号，指向 profile_version.version */
    private Integer currentVersion;
    private String publishedBy;
    private LocalDateTime publishedAt;
    private Integer lockVersion;
    /** 交付目录结构模板 JSON（默认 .flow/asset/src/docs 树），流程配置页可改 */
    private String deliveryDirTemplate;
    /** 默认项目根路径模板，{businessKey} 占位，项目启动选根目录的默认值 */
    private String defaultProjectRootPattern;
    /** 节点环境默认（native-ai / omnigent-sandbox），activity_def.envRequirements 为空时继承 */
    private String envProfile;
    /** 按 stage 默认 skill 映射 JSON，如 {"concept":"spk-ipd-concept",...} */
    private String defaultSkillBindings;
}
