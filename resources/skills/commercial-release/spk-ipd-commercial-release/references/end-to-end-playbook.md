# 商业软件 IPD 端到端运行手册

## 六阶段主链

`01 概念 → TR1/CDCP → 02 计划 → TR2/PDCP → 03 开发 → TR3/TR4 → 04 验证 → TR5 → 05 发布 → TR6/GA → 06 生命周期 → LDCP/EOL`

| 阶段 | 编排 Skill | 核心出口 |
|---|---|---|
| 01 概念 | `spk-ipd-01-concept` | 经验证的机会、概念、商业与技术证据，TR1/CDCP 决策 |
| 02 计划 | `spk-ipd-02-plan` | PRS、架构、资源、CBB、验证策略，TR2/PDCP 决策 |
| 03 开发 | `spk-ipd-03-develop` | 签名制品、开发与 CI 证据，TR3/TR4 决策 |
| 04 验证 | `spk-ipd-04-verify` | RTM、测试、缺陷、用户与残余风险证据，TR5 决策 |
| 05 发布 | `spk-ipd-05-launch` | 可回滚发布、运营接收、TR6/GA 与稳定观察 |
| 06 生命周期 | `spk-ipd-06-lifecycle` | 反馈、价值、安全、维护、下一代回流及 LDCP/EOL |

## Product Delivery Ledger

Ledger 每次状态变化追加记录，不覆写历史。至少含 product/project/version/process/activity/run IDs，当前阶段与节点，输入/产物/证据哈希，producer/verifier/approver，门禁决定与条件，风险、预算、重试、回滚检查点、下游消费者和失效状态。聊天记录、看板列和模型摘要都不是 ledger 的替代品。

## 状态机

`NOT_READY → READY → RUNNING → AWAITING_VERIFICATION → AWAITING_HUMAN → VERIFIED → HANDED_OFF`

异常分支为 `BLOCKED`、`FAILED`、`INVALIDATED`、`ROLLED_BACK`。只有真实输入和依赖恢复后才能离开 BLOCKED；只有新验证回执才能离开 INVALIDATED；不得手工把失败状态改绿。

## 跨阶段控制

- 每一阶段只消费上一阶段签名 baseline manifest。
- 需求、架构、代码、测试、制品、发布与运行信号保持双向追溯。
- Artifact 更新必须触发影响分析；影响门禁结论时重开门禁。
- Agent 不能写 DCP/TR/GA/LDCP 最终状态，不能代替 CCB 和风险接受者。
- 每次模型调用记录模型与 Skill 版本；每次工具调用记录真实退出码和原始证据。
- 成本或 token 优化不得删除必要证据、复核、回滚或人工授权。
- 生产变更采用最小暴露、护栏、暂停和已演练回滚。

## 审批与自动返工

所有门禁遵守 `../../references/approval-rework-contract.md`。IPD 流程负责自动启动 Omnigent→Claude Code；审批人只基于冻结产物和证据决定。APPROVE 进入下一节点，REJECT 必须带原因并在同一流程实例内自动回到阶段入口、重跑、独立验证和再次送审，禁止把返工变成审批人手工管理 Agent 会话。

## 跨阶段恢复

从最后有效签名基线开始，依次验证 ledger 链、文件摘要、外部资源版本、凭据权限和未关闭条件。若任一环节无法复核，从该环节起把下游标为 INVALIDATED 并重跑；不依靠上次 Agent 的自然语言总结恢复。
