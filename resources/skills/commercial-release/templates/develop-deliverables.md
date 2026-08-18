# 开发阶段交付物模板索引

每个主交付物可使用 Markdown、JSON 或工程文件实现，但必须保留下表字段语义，并在 `activity-result.json` 中登记路径与 SHA-256。所有条目还必须包含 metadata、inputs、method、assumptions、risks、openItems、traceability、acceptanceChecks。

| Activity | 主交付物类型 | 必备业务字段 |
|---|---|---|
| ACT-03-04-01 | `DevelopmentBaselineManifest` | 需求/架构/依赖/工具链/哈希 |
| ACT-03-04-02 | `DevelopmentPackageSet` | 纵向切片/接口/依赖/DoD/owner |
| ACT-03-04-03 | `ContextBundle` | 最小上下文/来源/版本/token预算/失效条件 |
| ACT-03-04-04 | `ImplementationDraft` | 代码变更/测试/迁移/安全/回滚 |
| ACT-03-04-05 | `UnitComponentEvidence` | 命令/环境/覆盖/失败/原始回执 |
| ACT-03-04-06 | `PeerReviewRecord` | 非作者评审/问题/修复/残余风险 |
| ACT-03-04-07 | `CIRunEvidence` | 构建/测试/扫描/SBOM/制品来源 |
| ACT-03-04-08 | `ChangeDecisionRecord` | 变更请求/影响/选项/CCB人类决定 |
| ACT-03-04-09 | `DevelopmentEvidencePack` | claims-证据-产物索引/完整性/缺口 |
| ACT-03-04-10 | `TR3_TR4_DecisionRecord` | 技术准入项/独立复核/人类评审结论 |
| ACT-03-04-11 | `DevelopmentLearningSet` | 事件/根因/可复用规则/适用边界/失效条件 |

## 可填写实体模板

以下文件才是 开发阶段可复制填写的交付文档模板；上表仅用于解释必备字段。

| Activity | 主交付物类型 | 实体模板 |
|---|---|---|
| ACT-03-04-01 | `DevelopmentBaselineManifest` | [复制填写](./develop/ACT-03-04-01-DevelopmentBaselineManifest.template.md) |
| ACT-03-04-02 | `DevelopmentPackageSet` | [复制填写](./develop/ACT-03-04-02-DevelopmentPackageSet.template.md) |
| ACT-03-04-03 | `ContextBundle` | [复制填写](./develop/ACT-03-04-03-ContextBundle.template.md) |
| ACT-03-04-04 | `ImplementationDraft` | [复制填写](./develop/ACT-03-04-04-ImplementationDraft.template.md) |
| ACT-03-04-05 | `UnitComponentEvidence` | [复制填写](./develop/ACT-03-04-05-UnitComponentEvidence.template.md) |
| ACT-03-04-06 | `PeerReviewRecord` | [复制填写](./develop/ACT-03-04-06-PeerReviewRecord.template.md) |
| ACT-03-04-07 | `CIRunEvidence` | [复制填写](./develop/ACT-03-04-07-CIRunEvidence.template.md) |
| ACT-03-04-08 | `ChangeDecisionRecord` | [复制填写](./develop/ACT-03-04-08-ChangeDecisionRecord.template.md) |
| ACT-03-04-09 | `DevelopmentEvidencePack` | [复制填写](./develop/ACT-03-04-09-DevelopmentEvidencePack.template.md) |
| ACT-03-04-10 | `TR3_TR4_DecisionRecord` | [复制填写](./develop/ACT-03-04-10-TR3_TR4_DecisionRecord.template.md) |
| ACT-03-04-11 | `DevelopmentLearningSet` | [复制填写](./develop/ACT-03-04-11-DevelopmentLearningSet.template.md) |

不得以此索引本身作为交付物；必须复制对应实体模板并填入真实内容和证据。未知内容明确标记为风险/阻塞并指定 owner，不得使用 TODO、TBD、待补、示例值或未渲染的 `{{required:...}}`。
