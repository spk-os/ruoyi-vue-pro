# 生命周期阶段交付物模板索引

每个主交付物可使用 Markdown、JSON 或工程文件实现，但必须保留下表字段语义，并在 `activity-result.json` 中登记路径与 SHA-256。所有条目还必须包含 metadata、inputs、method、assumptions、risks、openItems、traceability、acceptanceChecks。

| Activity | 主交付物类型 | 必备业务字段 |
|---|---|---|
| ACT-03-07-01 | `LifecycleSignalSet` | 遥测/工单/安全/商业信号/时间窗 |
| ACT-03-07-02 | `FeedbackThemeSet` | 主题/样本/反例/趋势/置信度 |
| ACT-03-07-03 | `VulnerabilityExposureRecord` | CVE-SBOM-部署关联/可达性/优先级/处置 |
| ACT-03-07-04 | `MaintenanceTrainPlan` | 范围/依赖/窗口/回归/回滚 |
| ACT-03-07-05 | `QuarterlyValueReview` | 目标/价值/成本/SLO/偏差/行动 |
| ACT-03-07-06 | `NextGenRequirementCandidateSet` | 信号/候选需求/来源/价值/路由 |
| ACT-03-07-07 | `EOLAssessment` | 使用量/成本/风险/替代/合规/时间表 |
| ACT-03-07-08 | `LDCPDecisionRecord` | 退市选项/客户影响/证据/人类决定 |
| ACT-03-07-09 | `CustomerMigrationLedger` | 授权/客户批次/状态/对账/异常/回退 |
| ACT-03-07-10 | `LifecycleArchiveAndPostmortem` | 归档索引/保留策略/根因/学习/法律保留 |

## 可填写实体模板

以下文件才是 生命周期阶段可复制填写的交付文档模板；上表仅用于解释必备字段。

| Activity | 主交付物类型 | 实体模板 |
|---|---|---|
| ACT-03-07-01 | `LifecycleSignalSet` | [复制填写](./lifecycle/ACT-03-07-01-LifecycleSignalSet.template.md) |
| ACT-03-07-02 | `FeedbackThemeSet` | [复制填写](./lifecycle/ACT-03-07-02-FeedbackThemeSet.template.md) |
| ACT-03-07-03 | `VulnerabilityExposureRecord` | [复制填写](./lifecycle/ACT-03-07-03-VulnerabilityExposureRecord.template.md) |
| ACT-03-07-04 | `MaintenanceTrainPlan` | [复制填写](./lifecycle/ACT-03-07-04-MaintenanceTrainPlan.template.md) |
| ACT-03-07-05 | `QuarterlyValueReview` | [复制填写](./lifecycle/ACT-03-07-05-QuarterlyValueReview.template.md) |
| ACT-03-07-06 | `NextGenRequirementCandidateSet` | [复制填写](./lifecycle/ACT-03-07-06-NextGenRequirementCandidateSet.template.md) |
| ACT-03-07-07 | `EOLAssessment` | [复制填写](./lifecycle/ACT-03-07-07-EOLAssessment.template.md) |
| ACT-03-07-08 | `LDCPDecisionRecord` | [复制填写](./lifecycle/ACT-03-07-08-LDCPDecisionRecord.template.md) |
| ACT-03-07-09 | `CustomerMigrationLedger` | [复制填写](./lifecycle/ACT-03-07-09-CustomerMigrationLedger.template.md) |
| ACT-03-07-10 | `LifecycleArchiveAndPostmortem` | [复制填写](./lifecycle/ACT-03-07-10-LifecycleArchiveAndPostmortem.template.md) |

不得以此索引本身作为交付物；必须复制对应实体模板并填入真实内容和证据。未知内容明确标记为风险/阻塞并指定 owner，不得使用 TODO、TBD、待补、示例值或未渲染的 `{{required:...}}`。
