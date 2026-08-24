# 03 开发阶段运行手册

## 阶段目标

通过 TR3/TR4，签名制品与开发证据满足进入验证条件。本阶段关键路径：`01 → 02 → 03 → 04 → 05 → 06 → 07；08 贯穿；09 → 10(TR3/TR4) → 11`。

## 调度图

`01 → 02 → 03 → 04 → 05 → 06 → 07；08 贯穿；09 → 10(TR3/TR4) → 11`

调度器必须用真实 Artifact 状态解析依赖，不以菜单点击、任务已创建或 Agent 声称完成作为节点完成信号。并行节点使用独立工作区；共享基线、同一文件或同一外部写资源必须加锁。

## Activity 路由

| 序号 | Activity ID | 活动 | 执行 Skill | 主产物 | 人工责任 |
|---|---|---|---|---|---|
| 01 | ACT-03-04-01 | 基线确认 | `spk-ipd-03-develop-01-baseline-confirm` | `DevelopmentBaselineManifest` | Configuration Manager / PDT Lead 确认开发基线 |
| 02 | ACT-03-04-02 | 开发包切片 | `spk-ipd-03-develop-02-packet-slicing` | `DevelopmentPackageSet` | PDT / 架构师 / 测试负责人联合审批切片方案 |
| 03 | ACT-03-04-03 | Context 构建 | `spk-ipd-03-develop-03-context-build` | `ContextBundle` | Developer / Reviewer 确认上下文相关性 |
| 04 | ACT-03-04-04 | 实现草案 | `spk-ipd-03-develop-04-implementation` | `ImplementationDraft` | Developer 承担作者责任，审查并修改 Agent 产出 |
| 05 | ACT-03-04-05 | 单元/组件验证 | `spk-ipd-03-develop-05-unit-test` | `UnitComponentEvidence` | Developer/Test Owner 决定修复或豁免申请 |
| 06 | ACT-03-04-06 | 同行审查 | `spk-ipd-03-develop-06-peer-review` | `PeerReviewRecord` | 非作者 Reviewer 批准/拒绝 |
| 07 | ACT-03-04-07 | 持续集成 | `spk-ipd-03-develop-07-ci` | `CIRunEvidence` | Repo Owner 管理阈值与例外 |
| 08 | ACT-03-04-08 | 变更控制 | `spk-ipd-03-develop-08-change-control` | `ChangeDecisionRecord` | CCB 具名成员批准/拒绝（**AI 禁写 CCB 状态**） |
| 09 | ACT-03-04-09 | 证据汇编 | `spk-ipd-03-develop-09-evidence-pack` | `DevelopmentEvidencePack` | PQA / Configuration Manager 认证完整性 |
| 10 | ACT-03-04-10 | TR3/TR4 评审 | `spk-ipd-03-develop-10-tr-review` | `TR3_TR4_DecisionRecord` | 具名评委投票/签署（**AI 无投票权**） |
| 11 | ACT-03-04-11 | 经验回流 | `spk-ipd-03-develop-11-learning` | `DevelopmentLearningSet` | PDT Retro Owner 接受/拒绝改进项 |

## 阶段入口

- 上一阶段 baseline manifest 的签名和 SHA-256 均有效。
- 上一门禁的条件项已关闭，或已明确授权在本阶段处理且有 owner/期限。
- 项目范围、目标版本、流程实例、资源承诺和授权边界一致。
- 每个 READY Activity 的 REQUIRED 输入可以定位、读取和验证。
- Agent、Worker、Verifier 与人工责任人的身份隔离满足原设计。

## 编排规则

1. 为每个 Activity 创建唯一 runId 和不可共享的工作目录。
   实现活动另外遵守 `../../references/requirement-agent-contract.md`：每个可实现 REQ 必须是独立 Agent run，并由流程自动启动与重跑。
2. 读取 catalog 后再加载 Activity Skill，禁止凭名称猜测执行逻辑。
3. 先执行确定性工具与数据采集，再运行模型推理；模型输出必须验证。
4. 同一 Activity 最多按其规约重试；超过预算升级，不创建新的“成功”运行掩盖失败。
5. 任何 Artifact 更新都生成新版本与哈希，并使旧版本的下游消费关系失效。
6. 每日检查 critical path、阻断、证据缺口、人工待签、成本/token 与风险变化。
7. 阶段门前冻结材料；冻结后变更只能走 CCB 并重新验证受影响节点。

## 阶段出口检查

- 11 个 Activity 的主产物、活动结果、输入/证据/运行清单齐全。
- 所有强制模板字段已填充，Schema、哈希、链接和可重放验证通过。
- Requirement ↔ Code/Artifact ↔ Test/Evidence 追溯无孤儿。
- 人工责任记录包含具名主体、角色、决定、时间、基线哈希和签名。
- 独立 Verifier 与 producer 身份隔离，挑战和处理结果均落盘。
- 开放项不会阻断出口，且每项有 owner、期限、触发器和升级路径。
- 下一阶段对移交包完成机器解析与责任接收。

## 恢复和回滚

恢复运行时读取 ledger 和最后签名检查点，逐个复验文件哈希、外部资源版本和权限；不从聊天摘要推测状态。局部失败回滚本 Activity，基线级失败回滚整个受影响子图。人工否决保持证据并生成新候选版本，不覆写历史决策。
