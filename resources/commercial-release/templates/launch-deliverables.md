# 发布阶段交付物模板索引

每个主交付物可使用 Markdown、JSON 或工程文件实现，但必须保留下表字段语义，并在 `activity-result.json` 中登记路径与 SHA-256。所有条目还必须包含 metadata、inputs、method、assumptions、risks、openItems、traceability、acceptanceChecks。

| Activity | 主交付物类型 | 必备业务字段 |
|---|---|---|
| ACT-03-06-01 | `ReleaseManifest` | 制品/签名/SBOM/配置/兼容/来源 |
| ACT-03-06-02 | `MigrationVerificationPack` | 迁移/回滚/校验/性能/数据对账 |
| ACT-03-06-03 | `OperationalReadinessRecord` | SLO/监控/告警/runbook/值守/容量 |
| ACT-03-06-04 | `ProgressiveRolloutRecord` | 分群/阈值/观测/暂停/回滚/人类授权 |
| ACT-03-06-05 | `TR6DecisionPack` | 发布资格/证据完整性/风险/人类评审 |
| ACT-03-06-06 | `GA_Handoff_StabilityRecord` | GA决定/移交/稳定窗口/异常/退出条件 |

## 可填写实体模板

以下文件才是 发布阶段可复制填写的交付文档模板；上表仅用于解释必备字段。

| Activity | 主交付物类型 | 实体模板 |
|---|---|---|
| ACT-03-06-01 | `ReleaseManifest` | [复制填写](./launch/ACT-03-06-01-ReleaseManifest.template.md) |
| ACT-03-06-02 | `MigrationVerificationPack` | [复制填写](./launch/ACT-03-06-02-MigrationVerificationPack.template.md) |
| ACT-03-06-03 | `OperationalReadinessRecord` | [复制填写](./launch/ACT-03-06-03-OperationalReadinessRecord.template.md) |
| ACT-03-06-04 | `ProgressiveRolloutRecord` | [复制填写](./launch/ACT-03-06-04-ProgressiveRolloutRecord.template.md) |
| ACT-03-06-05 | `TR6DecisionPack` | [复制填写](./launch/ACT-03-06-05-TR6DecisionPack.template.md) |
| ACT-03-06-06 | `GA_Handoff_StabilityRecord` | [复制填写](./launch/ACT-03-06-06-GA_Handoff_StabilityRecord.template.md) |

不得以此索引本身作为交付物；必须复制对应实体模板并填入真实内容和证据。未知内容明确标记为风险/阻塞并指定 owner，不得使用 TODO、TBD、待补、示例值或未渲染的 `{{required:...}}`。
