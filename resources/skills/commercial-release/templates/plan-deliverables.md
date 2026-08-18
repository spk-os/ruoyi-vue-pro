# 计划阶段交付物模板索引

每个主交付物可使用 Markdown、JSON 或工程文件实现，但必须保留下表字段语义，并在 `activity-result.json` 中登记路径与 SHA-256。所有条目还必须包含 metadata、inputs、method、assumptions、risks、openItems、traceability、acceptanceChecks。

| Activity | 主交付物类型 | 必备业务字段 |
|---|---|---|
| ACT-03-03-01 | `PRSBaselineCandidate` | 原子需求/验收标准/优先级/来源/变更规则 |
| ACT-03-03-02 | `ArchitectureDescriptionCandidate` | 上下文/容器/组件/接口/NFR/ADR/威胁 |
| ACT-03-03-03 | `IntegratedProjectPlan` | WBS/依赖/资源/里程碑/风险/预算 |
| ACT-03-03-04 | `CBBReuseDecision` | 契约匹配/版本/差距/成本/决策依据 |
| ACT-03-03-05 | `VerificationStrategy` | 测试层级/环境/数据/覆盖/退出准则 |
| ACT-03-03-06 | `TR2_PDCP_EvidencePack` | 需求-架构-计划-测试一致性/缺口/人类决定 |

## 可填写实体模板

以下文件才是 计划阶段可复制填写的交付文档模板；上表仅用于解释必备字段。

| Activity | 主交付物类型 | 实体模板 |
|---|---|---|
| ACT-03-03-01 | `PRSBaselineCandidate` | [复制填写](./plan/ACT-03-03-01-PRSBaselineCandidate.template.md) |
| ACT-03-03-02 | `ArchitectureDescriptionCandidate` | [复制填写](./plan/ACT-03-03-02-ArchitectureDescriptionCandidate.template.md) |
| ACT-03-03-03 | `IntegratedProjectPlan` | [复制填写](./plan/ACT-03-03-03-IntegratedProjectPlan.template.md) |
| ACT-03-03-04 | `CBBReuseDecision` | [复制填写](./plan/ACT-03-03-04-CBBReuseDecision.template.md) |
| ACT-03-03-05 | `VerificationStrategy` | [复制填写](./plan/ACT-03-03-05-VerificationStrategy.template.md) |
| ACT-03-03-06 | `TR2_PDCP_EvidencePack` | [复制填写](./plan/ACT-03-03-06-TR2_PDCP_EvidencePack.template.md) |

不得以此索引本身作为交付物；必须复制对应实体模板并填入真实内容和证据。未知内容明确标记为风险/阻塞并指定 owner，不得使用 TODO、TBD、待补、示例值或未渲染的 `{{required:...}}`。
