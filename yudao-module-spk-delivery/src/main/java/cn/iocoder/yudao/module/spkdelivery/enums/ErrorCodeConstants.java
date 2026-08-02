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

    // ========== 智能体编队（1-050-109-000） ==========
    ErrorCode AGENT_SQUAD_NOT_EXISTS = new ErrorCode(1_050_109_000, "智能体编队不存在");
    ErrorCode AGENT_SQUAD_NAME_DUPLICATE = new ErrorCode(1_050_109_001, "编队名称已存在");
    ErrorCode AGENT_SQUAD_CODE_DUPLICATE = new ErrorCode(1_050_109_002, "编队编码已存在");
    ErrorCode AGENT_SQUAD_MEMBER_NOT_EXISTS = new ErrorCode(1_050_109_003, "编队成员不存在");
    ErrorCode AGENT_SQUAD_MEMBER_DUPLICATE = new ErrorCode(1_050_109_004, "该智能体已在此编队中");
    ErrorCode AGENT_SQUAD_NO_MEMBER = new ErrorCode(1_050_109_005, "编队无可用成员，无法唤醒");

}
