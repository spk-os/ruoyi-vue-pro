# Agent 执行与交付契约

## 状态机

`INTAKE → PREFLIGHT → EXECUTE → SELF_CHECK → INDEPENDENT_VERIFY → HUMAN_DECISION → PUBLISH_EVIDENCE → COMPLETE`

任一步缺输入、权限、证据或可恢复执行条件时进入 `BLOCKED`；确定失败且已保留证据时进入 `FAILED`。不得用降级文本把失败改写为完成。独立验证和人类决策仅在 catalog 要求或风险触发时进入，但不可绕过。

## 交付目录

```text
<activity-run>/
├── activity-result.json
├── artifacts/               # 主产物及附件
├── evidence/                # 原始快照、命令回执、测试结果
├── verification/            # VerificationReceipt（需要时）
└── approvals/               # HumanApprovalRecord（需要时）
```

`activity-result.json` 是唯一入口，必须符合 `schemas/activity-result.schema.json`。路径必须相对交付目录、不得逃逸；文件哈希使用小写 SHA-256。

## 执行规则

1. 输入以 ID、版本和哈希冻结；来源必须可定位并带采集时间。
2. 每项事实声明建立 `claimId → evidenceIds`；证据再追溯到产物字段或决策项。
3. Prompt、模型、Tool/MCP、Skill、代码和基线版本全部记录，秘密只记引用不记明文。
4. 能由编译器、测试框架、Schema、策略引擎、哈希或查询复核的内容必须用确定性工具，不接受 LLM 自称通过。
5. 重试必须有上限、退避和幂等键；连续失败应保留最后回执并升级。涉及外部写入必须先确认授权和回滚点。
6. Independent Verifier 只接收验收标准、原始输入、产物和证据，不能继承 producer 的推理草稿；actorId 不得相同。
7. 人审记录必须包含 decisionId、authorizedActor、decision、signedAt、scope、conditions 和 signature/provenance。Agent 生成的“批准”无效。

## 完成定义

只有以下条件全部满足才可 `COMPLETE`：主产物类型与 catalog 相符；不存在占位符；声明有证据；文件哈希一致；工具执行成功；要求的独立验证为 PASS；要求的人审为 APPROVED；未解决风险有 owner、期限和处置。验证脚本通过是必要条件，不是业务正确性的充分条件。
