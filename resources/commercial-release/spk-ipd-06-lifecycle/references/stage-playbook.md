# 06 生命周期阶段运行手册

## 阶段目标

持续价值与安全闭环；EOL 时通过 LDCP、完成迁移和归档。本阶段关键路径：`01 → 02；03 并行持续 → 04/05 → 06；07 → 08(LDCP) → 09 → 10`。

## 调度图

`01 → 02；03 并行持续 → 04/05 → 06；07 → 08(LDCP) → 09 → 10`

调度器必须用真实 Artifact 状态解析依赖，不以菜单点击、任务已创建或 Agent 声称完成作为节点完成信号。并行节点使用独立工作区；共享基线、同一文件或同一外部写资源必须加锁。

## Activity 路由

| 序号 | Activity ID | 活动 | 执行 Skill | 主产物 | 人工责任 |
|---|---|---|---|---|---|
| 01 | ACT-03-07-01 | Lifecycle Signal Ingestion | `spk-ipd-06-lifecycle-01-signal` | `LifecycleSignalSet` | LMT Lead 审核 Signal 质量、处理升级；Data Steward 确认数据用途与口径 |
| 02 | ACT-03-07-02 | Feedback Clustering & Analysis | `spk-ipd-06-lifecycle-02-feedback` | `FeedbackThemeSet` | Product Manager 审核聚类结果、确认优先级、处理误合并申诉 |
| 03 | ACT-03-07-03 | Vulnerability-SBOM Correlation | `spk-ipd-06-lifecycle-03-vulnerability` | `VulnerabilityExposureRecord` | Security Lead 审核漏洞评估、签署 VEX 声明、承诺修复时限或风险接受 |
| 04 | ACT-03-07-04 | Maintenance Train Management | `spk-ipd-06-lifecycle-04-maintenance` | `MaintenanceTrainPlan` | Release Manager 审批维护版本、签署发布；LMT Lead 承诺列车日期 |
| 05 | ACT-03-07-05 | Quarterly Value Review | `spk-ipd-06-lifecycle-05-value-review` | `QuarterlyValueReview` | LMT Lead 评议；IPMT 接收行动并决定投资方向 |
| 06 | ACT-03-07-06 | Next-Gen Requirement Reflux | `spk-ipd-06-lifecycle-06-reflux` | `NextGenRequirementCandidateSet` | Product Owner 审核候选需求、确认回流、决定接受/拒绝/再研究 |
| 07 | ACT-03-07-07 | EOL Assessment | `spk-ipd-06-lifecycle-07-eol` | `EOLAssessment` | LMT Lead 审核 EOL 评估；法务/安全/客户成功确认无遗漏义务；IPMT 决定 |
| 08 | ACT-03-07-08 | LDCP Decision Package | `spk-ipd-06-lifecycle-08-ldcp` | `LDCPDecisionRecord` | 具名 IPMT 评委签署 LDCP 决定（**AI 无投票权，系统层面禁止 AI 写入 LDCP 状态**） |
| 09 | ACT-03-07-09 | Customer Migration Execution | `spk-ipd-06-lifecycle-09-migration` | `CustomerMigrationLedger` | Customer Success 客户沟通和迁移支持；客户授权人验收迁移；Legal 确认合同合规 |
| 10 | ACT-03-07-10 | Knowledge Archival & Postmortem | `spk-ipd-06-lifecycle-10-archival` | `LifecycleArchiveAndPostmortem` | LMT Lead 审核归档、签署 Postmortem；Knowledge Steward 审核知识晋级；PQA 批准归档发布 |

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

- 10 个 Activity 的主产物、活动结果、输入/证据/运行清单齐全。
- 所有强制模板字段已填充，Schema、哈希、链接和可重放验证通过。
- Requirement ↔ Code/Artifact ↔ Test/Evidence 追溯无孤儿。
- 人工责任记录包含具名主体、角色、决定、时间、基线哈希和签名。
- 独立 Verifier 与 producer 身份隔离，挑战和处理结果均落盘。
- 开放项不会阻断出口，且每项有 owner、期限、触发器和升级路径。
- 下一阶段对移交包完成机器解析与责任接收。

## 恢复和回滚

恢复运行时读取 ledger 和最后签名检查点，逐个复验文件哈希、外部资源版本和权限；不从聊天摘要推测状态。局部失败回滚本 Activity，基线级失败回滚整个受影响子图。人工否决保持证据并生成新候选版本，不覆写历史决策。

