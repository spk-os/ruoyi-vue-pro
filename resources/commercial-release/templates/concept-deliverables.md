# 概念阶段交付物模板索引

每个主交付物可使用 Markdown、JSON 或工程文件实现，但必须保留下表字段语义，并在 `activity-result.json` 中登记路径与 SHA-256。所有条目还必须包含 metadata、inputs、method、assumptions、risks、openItems、traceability、acceptanceChecks。

| Activity | 主交付物类型 | 必备业务字段 |
|---|---|---|
| ACT-03-02-01 | `OpportunitySignalSet` | 信号来源/时间窗/去重聚类/置信度/反证 |
| ACT-03-02-02 | `CustomerNeedBrief` | 客户样本/原声证据/场景/痛点/优先级 |
| ACT-03-02-03 | `CompetitiveLandscape` | 竞品版本/能力证据/差异/风险/未知项 |
| ACT-03-02-04 | `ConceptOptionSet` | 目标客户/JTBD/价值主张/备选方案/取舍 |
| ACT-03-02-05 | `InitialBusinessCase` | 假设/口径/情景/现金流/敏感性 |
| ACT-03-02-06 | `TechnicalRiskAndCBBMap` | 技术风险/CBB 候选/契约匹配/缓解措施 |
| ACT-03-02-07 | `TR1_CDCP_EvidencePack` | 检查项/claims/证据索引/缺口/建议 |
| ACT-03-02-08 | `TR1DecisionRecord` | 议题/评审意见/人类决定/条件/签署 |

## 可填写实体模板

以下文件才是 概念阶段可复制填写的交付文档模板；上表仅用于解释必备字段。

| Activity | 主交付物类型 | 实体模板 |
|---|---|---|
| ACT-03-02-01 | `OpportunitySignalSet` | [复制填写](./concept/ACT-03-02-01-OpportunitySignalSet.template.md) |
| ACT-03-02-02 | `CustomerNeedBrief` | [复制填写](./concept/ACT-03-02-02-CustomerNeedBrief.template.md) |
| ACT-03-02-03 | `CompetitiveLandscape` | [复制填写](./concept/ACT-03-02-03-CompetitiveLandscape.template.md) |
| ACT-03-02-04 | `ConceptOptionSet` | [复制填写](./concept/ACT-03-02-04-ConceptOptionSet.template.md) |
| ACT-03-02-05 | `InitialBusinessCase` | [复制填写](./concept/ACT-03-02-05-InitialBusinessCase.template.md) |
| ACT-03-02-06 | `TechnicalRiskAndCBBMap` | [复制填写](./concept/ACT-03-02-06-TechnicalRiskAndCBBMap.template.md) |
| ACT-03-02-07 | `TR1_CDCP_EvidencePack` | [复制填写](./concept/ACT-03-02-07-TR1_CDCP_EvidencePack.template.md) |
| ACT-03-02-08 | `TR1DecisionRecord` | [复制填写](./concept/ACT-03-02-08-TR1DecisionRecord.template.md) |

不得以此索引本身作为交付物；必须复制对应实体模板并填入真实内容和证据。未知内容明确标记为风险/阻塞并指定 owner，不得使用 TODO、TBD、待补、示例值或未渲染的 `{{required:...}}`。
