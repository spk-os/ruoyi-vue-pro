package cn.iocoder.yudao.module.spkdelivery.enums;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

/**
 * SPK-OS Delivery 错误码枚举类
 * <p>
 * spk-delivery 系统，使用 1-050-000-000 段
 */
public interface ErrorCodeConstants {

    // ========== Agent 任务（1-050-100-000） ==========
    ErrorCode AGENT_TASK_NOT_EXISTS = new ErrorCode(1_050_100_000, "Agent 任务不存在");
    ErrorCode AGENT_TASK_DISPATCH_FAIL = new ErrorCode(1_050_100_001, "Agent 任务派发失败");
    ErrorCode AGENT_TASK_CALLBACK_FAIL = new ErrorCode(1_050_100_002, "Agent 任务回调失败");
    ErrorCode AGENT_TASK_ALREADY_DONE = new ErrorCode(1_050_100_003, "Agent 任务已结束，不可重复回调");
    ErrorCode AGENT_ROLE_NOT_EXISTS = new ErrorCode(1_050_100_004, "Agent 角色不存在");
    ErrorCode AGENT_RUNTIME_UNAVAILABLE = new ErrorCode(1_050_100_005, "Agent 运行时不可用");
    ErrorCode AGENT_RECOVERY_REASON_REQUIRED = new ErrorCode(1_050_100_006, "基础设施恢复必须填写修复原因");
    ErrorCode AGENT_RECOVERY_STATE_INVALID = new ErrorCode(1_050_100_007, "仅 BLOCKED FlowRun 可执行基础设施恢复");
    ErrorCode AGENT_RECOVERY_WAIT_STATE_MISSING = new ErrorCode(1_050_100_008, "基础设施恢复失败：流程未停留在预期 receiveTask");

    // ========== 门禁 G1-G8 / TR（1-050-101-000） ==========
    ErrorCode GATE_RECORD_NOT_EXISTS = new ErrorCode(1_050_101_000, "门禁记录不存在");
    ErrorCode GATE_CALLBACK_SIGNATURE_INVALID = new ErrorCode(1_050_101_001, "门禁回调签名校验失败");
    ErrorCode GATE_CALLBACK_NODE_NOT_FOUND = new ErrorCode(1_050_101_002, "门禁回调找不到对应流程节点");
    ErrorCode GATE_TRIGGER_FAIL = new ErrorCode(1_050_101_003, "门禁触发失败");

    // ========== Aegis 审查（1-050-102-000） ==========
    ErrorCode AEGIS_REVIEW_NOT_EXISTS = new ErrorCode(1_050_102_000, "Aegis 审查记录不存在");
    ErrorCode AEGIS_REVIEW_FAIL = new ErrorCode(1_050_102_001, "Aegis LLM 审查失败");
    ErrorCode AEGIS_OUTPUT_SCHEMA_INVALID = new ErrorCode(1_050_102_002, "Aegis 产物不符合 output_schema");

    // ========== DCP 决策门（1-050-103-000） ==========
    ErrorCode DCP_REDIRECT_FAIL = new ErrorCode(1_050_103_000, "DCP 回退失败");
    ErrorCode DCP_REDIRECT_LIMIT_EXCEEDED = new ErrorCode(1_050_103_001, "DCP 回退次数超限");
    ErrorCode DCP_INSTANCE_NOT_EXISTS = new ErrorCode(1_050_103_002, "IPD 流程实例不存在");

    // ========== CCB 变更（1-050-104-000） ==========
    ErrorCode CCB_RECORD_NOT_EXISTS = new ErrorCode(1_050_104_000, "CCB 变更记录不存在");

    // ========== OR 池需求队列（1-050-105-000） ==========
    ErrorCode INTELLECT_REQ_NOT_EXISTS = new ErrorCode(1_050_105_000, "OR 池需求不存在");
    ErrorCode INTELLECT_REQ_DUPLICATE = new ErrorCode(1_050_105_001, "OR 池需求已存在（去重命中）");

    // ========== R7 反馈 / R8 退市（1-050-106-000） ==========
    ErrorCode FEEDBACK_NOT_EXISTS = new ErrorCode(1_050_106_000, "R7 反馈记录不存在");
    ErrorCode SUNSET_NOT_EXISTS = new ErrorCode(1_050_106_001, "R8 退市记录不存在");

    // ========== Workflow Contract 版本治理（1-050-107-000） ==========
    ErrorCode CONTRACT_VERSION_NOT_EXISTS = new ErrorCode(1_050_107_000, "流程契约版本不存在");
    ErrorCode CONTRACT_HASH_MISMATCH = new ErrorCode(1_050_107_001, "流程契约 hash 不匹配");

    // ========== 智能体定义（1-050-108-000） ==========
    ErrorCode AGENT_DEF_NOT_EXISTS = new ErrorCode(1_050_108_000, "智能体定义不存在");
    ErrorCode AGENT_DEF_NAME_DUPLICATE = new ErrorCode(1_050_108_001, "智能体名称已存在");
    ErrorCode AGENT_DEF_CODE_DUPLICATE = new ErrorCode(1_050_108_002, "智能体编码已存在");
    ErrorCode AGENT_DEF_ROLE_REQUIRED = new ErrorCode(1_050_108_003, "智能体未关联 AI 角色，无法唤醒");
    ErrorCode AGENT_DEF_RUNTIME_NOT_SUPPORT_WAKE = new ErrorCode(1_050_108_004, "该运行时类型暂不支持本地唤醒");
    ErrorCode AGENT_DEF_WAKE_FAIL = new ErrorCode(1_050_108_005, "智能体唤醒失败");
    ErrorCode AGENT_DEF_PARENT_CYCLE = new ErrorCode(1_050_108_006, "父智能体继承链成环（不能指向自己或子孙）");

    // ========== 智能体编队（1-050-109-000） ==========
    ErrorCode AGENT_SQUAD_NOT_EXISTS = new ErrorCode(1_050_109_000, "智能体编队不存在");
    ErrorCode AGENT_SQUAD_NAME_DUPLICATE = new ErrorCode(1_050_109_001, "编队名称已存在");
    ErrorCode AGENT_SQUAD_CODE_DUPLICATE = new ErrorCode(1_050_109_002, "编队编码已存在");
    ErrorCode AGENT_SQUAD_MEMBER_NOT_EXISTS = new ErrorCode(1_050_109_003, "编队成员不存在");
    ErrorCode AGENT_SQUAD_MEMBER_DUPLICATE = new ErrorCode(1_050_109_004, "该智能体已在此编队中");
    ErrorCode AGENT_SQUAD_NO_MEMBER = new ErrorCode(1_050_109_005, "编队无可用成员，无法唤醒");

    // ========== IPD Activity 定义（1-050-110-000） ==========
    ErrorCode IPD_ACTIVITY_NOT_EXISTS = new ErrorCode(1_050_110_000, "IPD Activity 定义不存在");
    ErrorCode IPD_ACTIVITY_DISABLED = new ErrorCode(1_050_110_001, "IPD Activity 定义已停用");
    ErrorCode IPD_ACTIVITY_STAGE_INVALID = new ErrorCode(1_050_110_002, "IPD Activity 阶段非法");

    // ========== Task Contract / Router（1-050-111-000） ==========
    ErrorCode TASK_CONTRACT_NOT_EXISTS = new ErrorCode(1_050_111_000, "Task Contract 不存在");
    ErrorCode TASK_ROUTER_NO_LEAD = new ErrorCode(1_050_111_001, "Task Router 找不到匹配能力标签的 Lead Agent");
    ErrorCode TASK_ROUTER_LEAD_OVERLOADED = new ErrorCode(1_050_111_002, "Lead Agent 全部过载，无法承接 Activity");
    ErrorCode TASK_CONTRACT_TERMINAL = new ErrorCode(1_050_111_003, "Task Contract 已终态，不可重复执行");
    ErrorCode TASK_CONTRACT_THREE_PIECE_MISSING = new ErrorCode(1_050_111_004, "三件套缺失（ContextManifest/ArtifactManifest/RunReceipt），Activity 不得标记完成");

    // ========== Artifact 产物（1-050-112-000） ==========
    ErrorCode ARTIFACT_NOT_EXISTS = new ErrorCode(1_050_112_000, "Artifact Manifest 不存在");
    ErrorCode ARTIFACT_HASH_MISMATCH = new ErrorCode(1_050_112_001, "Artifact hash 校验失败");
    ErrorCode ARTIFACT_SECRET_SCAN_PENDING = new ErrorCode(1_050_112_002, "Artifact secret 扫描未通过");

    // ========== Evidence 证据链（1-050-113-000） ==========
    ErrorCode EVIDENCE_NOT_EXISTS = new ErrorCode(1_050_113_000, "证据记录不存在");
    ErrorCode EVIDENCE_CHAIN_BROKEN = new ErrorCode(1_050_113_001, "证据哈希链断裂，回放校验失败");

    // ========== Verification 验证（1-050-114-000） ==========
    ErrorCode VERIFICATION_RECEIPT_NOT_EXISTS = new ErrorCode(1_050_114_000, "Verification Receipt 不存在");
    ErrorCode VERIFICATION_FAIL = new ErrorCode(1_050_114_001, "独立验证失败");
    ErrorCode VERIFIER_NOT_EXISTS = new ErrorCode(1_050_114_002, "无可用 Independent Verifier");

    // ========== Model Capability（1-050-115-000） ==========
    ErrorCode MODEL_CAPABILITY_NOT_EXISTS = new ErrorCode(1_050_115_000, "模型能力画像不存在");
    ErrorCode MODEL_SNAPSHOT_NOT_EXISTS = new ErrorCode(1_050_115_001, "模型快照不存在");

    // ========== IPD 业务骨架 Project/Major/Version/FlowRun/Issue（1-050-116-000） ==========
    ErrorCode IPD_PROJECT_NOT_EXISTS = new ErrorCode(1_050_116_000, "IPD 项目不存在");
    ErrorCode IPD_PROJECT_CODE_DUPLICATE = new ErrorCode(1_050_116_001, "项目短码已存在");
    ErrorCode IPD_PROJECT_NOT_DRAFT = new ErrorCode(1_050_116_002, "项目非草稿状态，不可修改");
    ErrorCode IPD_PROJECT_HAS_ACTIVE = new ErrorCode(1_050_116_003, "存在活跃版本/流程，不可归档");
    ErrorCode IPD_MAJOR_NOT_EXISTS = new ErrorCode(1_050_116_010, "大版本不存在");
    ErrorCode IPD_MAJOR_NO_DUPLICATE = new ErrorCode(1_050_116_011, "大版本序号已存在");
    ErrorCode IPD_MAJOR_HAS_RUNNING = new ErrorCode(1_050_116_012, "大版本存在运行实例，禁止修改序号");
    ErrorCode IPD_VERSION_NOT_EXISTS = new ErrorCode(1_050_116_020, "交付版本不存在");
    ErrorCode IPD_VERSION_NO_DUPLICATE = new ErrorCode(1_050_116_021, "版本号已存在");
    ErrorCode IPD_VERSION_NOT_DRAFT = new ErrorCode(1_050_116_022, "版本非 DRAFT，不可改编号");
    ErrorCode IPD_VERSION_RUNNING = new ErrorCode(1_050_116_023, "版本已进入运行，不可取消");
    ErrorCode IPD_VERSION_BASELINE_EXISTS = new ErrorCode(1_050_116_024, "同一大版本已存在基线版本");
    ErrorCode IPD_FLOW_RUN_NOT_EXISTS = new ErrorCode(1_050_116_030, "FlowRun 不存在");
    ErrorCode IPD_FLOW_RUN_NOT_READY = new ErrorCode(1_050_116_031, "FlowRun 非 READY 状态，不可启动");
    ErrorCode IPD_FLOW_RUN_ACTIVE_EXISTS = new ErrorCode(1_050_116_032, "同一版本已存在活跃主交付流");
    ErrorCode IPD_FLOW_TYPE_MISMATCH = new ErrorCode(1_050_116_033, "流程类型与版本类型不匹配");
    ErrorCode IPD_FLOW_START_FAIL = new ErrorCode(1_050_116_034, "Flowable 流程启动失败");
    ErrorCode IPD_FLOW_RUN_NOT_CANCELLABLE = new ErrorCode(1_050_116_035, "FlowRun 当前状态不可取消");
    ErrorCode IPD_FLOW_RUN_NOT_BLOCKABLE = new ErrorCode(1_050_116_036, "FlowRun 当前状态不可阻断（仅运行中/等待审批可阻断）");
    ErrorCode IPD_FLOW_RUN_NOT_UNBLOCKABLE = new ErrorCode(1_050_116_037, "FlowRun 当前状态不可解除阻断（仅 BLOCKED 可解除）");
    ErrorCode IPD_FLOW_RUN_NOT_RETRYABLE = new ErrorCode(1_050_116_038, "FlowRun 当前状态不可重试（仅 FAILED 可重试）");
    ErrorCode IPD_FLOW_RUN_REASON_REQUIRED = new ErrorCode(1_050_116_039, "阻断/取消/重试必须填写理由");
    ErrorCode IPD_FLOW_RUN_ACCESS_DENIED = new ErrorCode(1_050_116_040, "无权访问该 FlowRun：不属于当前用户负责的项目");
    ErrorCode IPD_ISSUE_NOT_EXISTS = new ErrorCode(1_050_116_040, "问题不存在");
    ErrorCode IPD_ISSUE_NO_FIXED_IN = new ErrorCode(1_050_116_041, "进入实施修复前必须存在 FIXED_IN 关联");
    ErrorCode IPD_ISSUE_NOT_RESOLVABLE = new ErrorCode(1_050_116_042, "问题缺少验证结论，不可关闭");
    ErrorCode IPD_COMMAND_CONFLICT = new ErrorCode(1_050_116_050, "幂等键相同但负载不一致，命令冲突");
    ErrorCode IPD_CONTEXT_MISMATCH = new ErrorCode(1_050_116_051, "上下文键互相矛盾");

    // ========== IPD 参与者与分派（1-050-116-060） ==========
    ErrorCode IPD_ACTOR_NOT_EXISTS = new ErrorCode(1_050_116_060, "项目参与者不存在");
    ErrorCode IPD_ACTOR_DUPLICATE = new ErrorCode(1_050_116_061, "同一作用域已存在相同参与者");
    ErrorCode IPD_ACTOR_ACCOUNTABLE_DUPLICATE = new ErrorCode(1_050_116_062, "同一作用域同一角色已有唯一 accountable");
    ErrorCode IPD_ACTOR_TYPE_INVALID = new ErrorCode(1_050_116_063, "参与者类型非法，须为 HUMAN/AGENT/SQUAD/SYSTEM");
    ErrorCode IPD_ASSIGNMENT_NOT_EXISTS = new ErrorCode(1_050_116_070, "任务分派不存在");
    ErrorCode IPD_ASSIGNMENT_BPM_NOT_HUMAN = new ErrorCode(1_050_116_071, "BPM 审批任务只能分派给真人，Agent 不可作为 accountable");
    ErrorCode IPD_ASSIGNMENT_NOT_REASSIGNABLE = new ErrorCode(1_050_116_072, "任务分派当前状态不可转派");

    // ========== IPD 审批与决策包（1-050-116-080） ==========
    ErrorCode IPD_TASK_ALREADY_COMPLETED = new ErrorCode(1_050_116_080, "审批任务已被处理，不得生成孤立决策");
    ErrorCode IPD_TASK_NOT_BELONG_TO_USER = new ErrorCode(1_050_116_081, "审批任务不属于当前用户");
    ErrorCode IPD_DECISION_INVALID = new ErrorCode(1_050_116_082, "决策类型非法，须为 APPROVE/REJECT/REDIRECT/RETURN");
    ErrorCode IPD_DECISION_PACKAGE_HASH_MISMATCH = new ErrorCode(1_050_116_083, "决策包已变化，请刷新后重审");
    ErrorCode IPD_REQUIRED_ARTIFACT_MISSING = new ErrorCode(1_050_116_084, "必需产物/证据缺失或门禁未通过，禁止 Go");
    ErrorCode IPD_EVIDENCE_WAIVER_NOT_AUTHORIZED = new ErrorCode(1_050_116_085, "无证据豁免授权");
    ErrorCode IPD_REDIRECT_TARGET_REQUIRED = new ErrorCode(1_050_116_086, "REDIRECT/RETURN 必须指定目标节点 key");

    // ========== IPD 流程治理（1-050-116-090） ==========
    ErrorCode IPD_PROFILE_NOT_EXISTS = new ErrorCode(1_050_116_090, "流程模板 Profile 不存在");
    ErrorCode IPD_PROFILE_CODE_DUPLICATE = new ErrorCode(1_050_116_091, "Profile 编码已存在");
    ErrorCode IPD_PROFILE_NOT_PUBLISHED = new ErrorCode(1_050_116_092, "该流程类型暂无已发布 Profile，请在流程治理配置后重试");
    ErrorCode IPD_PROFILE_VERSION_NOT_EXISTS = new ErrorCode(1_050_116_093, "Profile 版本不存在");
    ErrorCode IPD_PROFILE_VERSION_ALREADY_PUBLISHED = new ErrorCode(1_050_116_094, "该版本已发布，不可重复发布");
    ErrorCode IPD_PROFILE_FLOW_TYPE_MISMATCH = new ErrorCode(1_050_116_095, "Profile flowType 与请求不符");
    ErrorCode IPD_PROFILE_VERSION_NOT_PUBLISHED = new ErrorCode(1_050_116_096, "该版本未发布，不可回滚/引用");

    // ========== IPD 交付目录与流程状态（1-050-116-097） ==========
    ErrorCode IPD_DELIVERY_ROOT_INVALID = new ErrorCode(1_050_116_097, "交付根目录非法：禁止 .. 与路径分隔符，且须位于 /work/SPK-OS/Delivery/ 下");
    ErrorCode IPD_ITERATION_NOT_FOUND = new ErrorCode(1_050_116_098, "迭代 FlowRun 不存在：未找到该项目下指定迭代的流程运行");
    ErrorCode IPD_ITERATION_ROLLBACK_NO_SNAPSHOT = new ErrorCode(1_050_116_099, "迭代回滚失败：该迭代 .flow/manifest 无快照可恢复");
    ErrorCode IPD_ITERATION_NOT_ROLLBACKABLE = new ErrorCode(1_050_116_100, "迭代当前状态不可回滚（仅 FAILED/BLOCKED/CANCELLED 可回滚重置到 manifest 快照阶段）");

}
