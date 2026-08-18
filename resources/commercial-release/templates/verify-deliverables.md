# 验证阶段交付物模板索引

每个主交付物可使用 Markdown、JSON 或工程文件实现，但必须保留下表字段语义，并在 `activity-result.json` 中登记路径与 SHA-256。所有条目还必须包含 metadata、inputs、method、assumptions、risks、openItems、traceability、acceptanceChecks。

| Activity | 主交付物类型 | 必备业务字段 |
|---|---|---|
| ACT-03-05-01 | `VerificationBaselineManifest` | 制品/配置/环境/测试集/哈希 |
| ACT-03-05-02 | `RVM_RTM_Baseline` | 需求-设计-代码-测试-证据三向追溯 |
| ACT-03-05-03 | `MultiLayerTestDesign` | 单元/组件/集成/系统/安全/性能/混沌 |
| ACT-03-05-04 | `ReplayableTestEnvironment` | IaC/镜像/数据谱系/脱敏/复现命令 |
| ACT-03-05-05 | `TestExecutionEvidence` | 真实执行/原始日志/统计/失败分析 |
| ACT-03-05-06 | `DefectClosureRecord` | 缺陷/严重度/根因/修复/回归/关闭依据 |
| ACT-03-05-07 | `UserValidationReport` | 真实场景/参与者授权/结果/反馈/限制 |
| ACT-03-05-08 | `ResidualRiskReport` | 风险/概率/影响/暴露/owner/接受依据 |
| ACT-03-05-09 | `TR5QualificationDecision` | 退出准则/残余风险/独立验证/人类签署 |

## 可填写实体模板

以下文件才是 验证阶段可复制填写的交付文档模板；上表仅用于解释必备字段。

| Activity | 主交付物类型 | 实体模板 |
|---|---|---|
| ACT-03-05-01 | `VerificationBaselineManifest` | [复制填写](./verify/ACT-03-05-01-VerificationBaselineManifest.template.md) |
| ACT-03-05-02 | `RVM_RTM_Baseline` | [复制填写](./verify/ACT-03-05-02-RVM_RTM_Baseline.template.md) |
| ACT-03-05-03 | `MultiLayerTestDesign` | [复制填写](./verify/ACT-03-05-03-MultiLayerTestDesign.template.md) |
| ACT-03-05-04 | `ReplayableTestEnvironment` | [复制填写](./verify/ACT-03-05-04-ReplayableTestEnvironment.template.md) |
| ACT-03-05-05 | `TestExecutionEvidence` | [复制填写](./verify/ACT-03-05-05-TestExecutionEvidence.template.md) |
| ACT-03-05-06 | `DefectClosureRecord` | [复制填写](./verify/ACT-03-05-06-DefectClosureRecord.template.md) |
| ACT-03-05-07 | `UserValidationReport` | [复制填写](./verify/ACT-03-05-07-UserValidationReport.template.md) |
| ACT-03-05-08 | `ResidualRiskReport` | [复制填写](./verify/ACT-03-05-08-ResidualRiskReport.template.md) |
| ACT-03-05-09 | `TR5QualificationDecision` | [复制填写](./verify/ACT-03-05-09-TR5QualificationDecision.template.md) |

不得以此索引本身作为交付物；必须复制对应实体模板并填入真实内容和证据。未知内容明确标记为风险/阻塞并指定 owner，不得使用 TODO、TBD、待补、示例值或未渲染的 `{{required:...}}`。
