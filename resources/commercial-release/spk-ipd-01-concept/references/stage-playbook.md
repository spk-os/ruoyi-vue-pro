# 01 概念阶段运行手册

## 阶段目标

通过 TR1/CDCP，概念、商业价值与技术风险获授权。本阶段关键路径：`01/02/03 可并行 → 04 → 05/06 可并行 → 07 → 08(TR1/CDCP)`。

## 调度图

`01/02/03 可并行 → 04 → 05/06 可并行 → 07 → 08(TR1/CDCP)`

调度器必须用真实 Artifact 状态解析依赖，不以菜单点击、任务已创建或 Agent 声称完成作为节点完成信号。并行节点使用独立工作区；共享基线、同一文件或同一外部写资源必须加锁。

## Activity 路由

| 序号 | Activity ID | 活动 | 执行 Skill | 主产物 | 人工责任 |
|---|---|---|---|---|---|
| 01 | ACT-03-02-01 | 机会信号聚合 | `spk-ipd-01-concept-01-signal-aggregation` | `OpportunitySignalSet` | Market Owner 签署 admitted |
| 02 | ACT-03-02-02 | 客户需求深度分析 | `spk-ipd-01-concept-02-voc-analysis` | `CustomerNeedBrief` | Research Lead / Product Manager 签署 |
| 03 | ACT-03-02-03 | 竞争格局分析 | `spk-ipd-01-concept-03-competitive-analysis` | `CompetitiveLandscape` | Strategy Owner 签署 |
| 04 | ACT-03-02-04 | 概念与价值主张生成 | `spk-ipd-01-concept-04-value-proposition` | `ConceptOptionSet` | PDT Lead 签署 |
| 05 | ACT-03-02-05 | 初步财务评估 | `spk-ipd-01-concept-05-financial-modeling` | `InitialBusinessCase` | Finance Owner 签署 |
| 06 | ACT-03-02-06 | 技术风险与 CBB 识别 | `spk-ipd-01-concept-06-tech-risk` | `TechnicalRiskAndCBBMap` | SE 与 TDT 签署 |
| 07 | ACT-03-02-07 | TR1/CDCP 决策包组装 | `spk-ipd-01-concept-07-evidence-pack` | `TR1_CDCP_EvidencePack` | PDT Lead / Configuration Manager 签署 |
| 08 | ACT-03-02-08 | TR1 评审支持 | `spk-ipd-01-concept-08-tr1-review-support` | `TR1DecisionRecord` | 具名评委 / IPMT Chair 签署 |

## 阶段入口

- 上一阶段 baseline manifest 的签名和 SHA-256 均有效。
- 上一门禁的条件项已关闭，或已明确授权在本阶段处理且有 owner/期限。
- 项目范围、目标版本、流程实例、资源承诺和授权边界一致。
- 每个 READY Activity 的 REQUIRED 输入可以定位、读取和验证。
- Agent、Worker、Verifier 与人工责任人的身份隔离满足原设计。

## 编排规则

1. 为每个 Activity 创建唯一 runId 和不可共享的工作目录。
2. 读取 catalog 后再加载 Activity Skill，禁止凭名称猜测执行逻辑。
3. 先执行确定性工具与数据采集，再运行模型推理；模型输出必须验证。
4. 同一 Activity 最多按其规约重试；超过预算升级，不创建新的“成功”运行掩盖失败。
5. 任何 Artifact 更新都生成新版本与哈希，并使旧版本的下游消费关系失效。
6. 每日检查 critical path、阻断、证据缺口、人工待签、成本/token 与风险变化。
7. 阶段门前冻结材料；冻结后变更只能走 CCB 并重新验证受影响节点。

## 阶段出口检查

- 8 个 Activity 的主产物、活动结果、输入/证据/运行清单齐全。
- 所有强制模板字段已填充，Schema、哈希、链接和可重放验证通过。
- Requirement ↔ Code/Artifact ↔ Test/Evidence 追溯无孤儿。
- 人工责任记录包含具名主体、角色、决定、时间、基线哈希和签名。
- 独立 Verifier 与 producer 身份隔离，挑战和处理结果均落盘。
- 开放项不会阻断出口，且每项有 owner、期限、触发器和升级路径。
- 下一阶段对移交包完成机器解析与责任接收。

## 恢复和回滚

恢复运行时读取 ledger 和最后签名检查点，逐个复验文件哈希、外部资源版本和权限；不从聊天摘要推测状态。局部失败回滚本 Activity，基线级失败回滚整个受影响子图。人工否决保持证据并生成新候选版本，不覆写历史决策。

