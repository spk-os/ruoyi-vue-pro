package cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdactivity;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * IPD Activity 定义注册中心 DO
 * <p>
 * 279 项 Canonical Activity 的唯一权威定义源（GAP-1）。Task Router 按 activity_id + version
 * 加载，判决执行位置/Lead/Worker/Verifier。依 SPK-OS-Cortext-IPD.md §4.1。
 *
 * @author SPK-OS
 */
@TableName("spk_ipd_activity_def")
@KeySequence("spk_ipd_activity_def_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpkIpdActivityDefDO extends BaseDO {

    @TableId
    private Long id;
    /**
     * Activity 业务标识，如 ACT-03-02-01
     */
    private String activityId;
    /**
     * 版本，默认 1.0.0
     */
    private String version;
    /**
     * 名称
     */
    private String name;
    /**
     * 阶段 concept/plan/develop/qualify/launch/lifecycle
     */
    private String stage;
    /**
     * 描述
     */
    private String description;
    /**
     * 关联 spk_agent_def.code
     */
    private String leadAgentCode;
    /**
     * 执行位置 task_system/lead_agent_internal
     */
    private String executionLocation;
    /**
     * 是否使用 Worker Agent（0 否 1 是）
     */
    private Integer useWorkerAgent;
    /**
     * 是否使用独立 Verifier（0 否 1 是）
     */
    private Integer useIndependentVerifier;
    /**
     * Verifier 类型 TR/DCP/Audit
     */
    private String verifierType;
    /**
     * 能力标签数组 JSON
     */
    private String modelCapabilities;
    /**
     * Skill 列表 JSON
     */
    private String skills;
    /**
     * 工具白名单 JSON
     */
    private String tools;
    /**
     * 知识引用 JSON
     */
    private String knowledgeRefs;
    /**
     * Information 引用 JSON
     */
    private String informationRefs;
    /**
     * Memory 作用域 JSON
     */
    private String memoryScope;
    /**
     * 输入产物类型 JSON
     */
    private String inputArtifactTypes;
    /**
     * 输出产物类型
     */
    private String outputArtifactType;
    /**
     * 输出模板引用
     */
    private String outputTemplateRef;
    /**
     * 验收标准 JSON
     */
    private String acceptanceCriteria;
    /**
     * 人工审批角色
     */
    private String humanApproverRole;
    /**
     * 失败处置 JSON
     */
    private String failureHandling;
    /**
     * Flowable 节点 id
     */
    private String flowableNodeId;
    /**
     * Prompt 模板（P1 直接用作派发 prompt）
     */
    private String promptTemplate;
    /**
     * 黄金用例 JSON
     */
    private String goldenCases;
    /**
     * 状态 draft/active/deprecated
     */
    private String status;
    /**
     * 审批人
     */
    private String approvedBy;

}
