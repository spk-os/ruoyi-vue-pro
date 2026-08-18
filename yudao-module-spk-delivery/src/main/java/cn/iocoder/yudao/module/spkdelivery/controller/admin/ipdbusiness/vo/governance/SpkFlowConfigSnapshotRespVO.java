package cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.governance;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 流程配置聚合快照 Resp（一站式读端，供前端 flow-config 四 tab 渲染）。
 * <p>设计文档 §B（流程配置聚合页）。聚合 Profile + activity_def（按 stage 分组）+ skill 目录 + 目录模板。
 *
 * @author SPK-OS
 */
@Schema(description = "管理后台 - 流程配置聚合快照 Resp")
@Data
public class SpkFlowConfigSnapshotRespVO {

    @Schema(description = "当前 flowType 的已发布 Profile（无则 null，前端标未接入治理）")
    private Map<String, Object> profile;

    @Schema(description = "交付目录结构模板 JSON（.flow/asset/src/docs 树，可编辑）")
    private String dirTemplate;

    @Schema(description = "默认项目根路径模板，{businessKey} 占位")
    private String defaultProjectRootPattern;

    @Schema(description = "节点环境默认 native-ai/omnigent-sandbox")
    private String envProfile;

    @Schema(description = "按 stage 默认 skill 映射 JSON")
    private String defaultSkillBindings;

    @Schema(description = "activity_def 按 stage 分组（stage → 定义列表）")
    private Map<String, List<Map<String, Object>>> activityDefsByStage;

    @Schema(description = "可选 skill 目录（扫描 /root/.claude/skills/spk-* + 设计文档 §7 兜底）")
    private List<String> skillCatalog;

    @Schema(description = "skill 根目录（DB 单行配置，前端「节点 skill 与环境」页可改；派发层 resolveSkillPath 用此路径）")
    private String skillsRoot;

    @Schema(description = "默认 skill 环境 default/test/commercial-release/prototype-release（派发层 resolveSkillEnv 三级链末级回退）")
    private String defaultSkillEnv;

    @Schema(description = "按 env 分组的 skill 目录：env → [{name, description, dir}]（扫描 skillsRoot 下各 env 子目录含 SKILL.md 的目录）")
    private Map<String, List<Map<String, Object>>> envCatalog;
}
