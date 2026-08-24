# Agent 执行与交付契约

执行任何人审门前还必须读取 `approval-rework-contract.md`。流程自动启动 Omnigent→Claude Code；审批人只审产物并决定，不手工创建、续跑或监控 Agent 会话。

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
8. REJECT 原因必须非空；流程必须保留原决定并自动返工、重新验证、再次送审，禁止把 REJECT 解释为流程结束或要求审批人接管 Agent。
9. Cortex 必须在冻结提示中给出唯一主产物路径 `.ipd/output/<activityRunId>/<artifactType>.json`；Claude Code 必须把单个 JSON envelope `{"document":{...},"summary":"...","conclusion":"PASS|FAIL: ..."}` 写入该精确路径。该文件是唯一权威主产物，assistant 过程消息和最终文本均不得被 Cortex 当作产物；`document` 必须是与 catalog 产物类型匹配的 JSON 对象，不得写入 Markdown、代码围栏或前后说明。文件缺失、越界、空文件或 envelope 畸形时必须 fail-closed 并进入 Cortex 自动重试。
10. Activity 执行或独立核验失败后，Cortex 必须把失败摘要带入同一 Activity 的新 Omnigent→Claude Code 执行，有界重试并生成新 run、产物哈希和 VerificationReceipt；用尽重试后必须把 FlowRun 置为 `BLOCKED`，禁止无限停留为伪 `RUNNING`。

## 完成定义

只有以下条件全部满足才可 `COMPLETE`：主产物类型与 catalog 相符；不存在占位符；声明有证据；文件哈希一致；工具执行成功；要求的独立验证为 PASS；要求的人审为 APPROVED；未解决风险有 owner、期限和处置。验证脚本通过是必要条件，不是业务正确性的充分条件。
