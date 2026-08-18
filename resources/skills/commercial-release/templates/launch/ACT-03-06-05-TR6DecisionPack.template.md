---
templateVersion: "1.0.0"
activityId: "ACT-03-06-05"
activityName: "TR6 证据包组装与评审"
artifactType: "TR6DecisionPack"
projectId: "{{required:项目ID}}"
processInstanceId: "{{required:流程实例ID}}"
runId: "{{required:执行ID}}"
baselineVersion: "{{required:输入基线版本}}"
producerActorId: "{{required:产出者ID}}"
createdAt: "{{required:ISO-8601时间}}"
status: "{{required:DRAFT|READY_FOR_VERIFY|BLOCKED}}"
sourceDesign: "/root/share/SPK-OS/24_houre_anly/IPD/05-kimi-k3-design/04-六阶段每阶段-具体设计与Activity执行配置表/04-05-发布阶段.md"
---

# TR6DecisionPack · TR6 证据包组装与评审

> 复制后填写。所有 `{{required:...}}` 都必须替换为可核验的真实值；空白、规则说明或本模板不能作为交付物。

## 1. 文档控制

| 项目 | 填写值 |
|---|---|
| 项目 / 发布版本 | {{required:项目名称与发布版本}} |
| Activity | ACT-03-06-05 / TR6 证据包组装与评审 |
| 文档版本 / SHA-256 | {{required:版本}} / {{required:64位SHA-256}} |
| Owner / 状态 | {{required:Owner}} / {{required:状态}} |

## 2. 执行摘要

- 结论：{{required:可判定且可证伪的结论}}
- 覆盖范围：{{required:包含和不包含的范围}}
- 关键量化结果：{{required:数值、单位和口径}}
- 阻断项：{{required:无，或阻断项ID、原因和Owner}}

## 3. 输入与基线

| 输入ID | 类型 | 版本 / 哈希 | 来源 / 授权 | 用途 |
|---|---|---|---|---|
| IN-001 | {{required:输入类型}} | {{required:不可变版本或哈希}} | {{required:URI与授权记录}} | {{required:本活动中的用途}} |

## 4. 业务内容

| 条目ID | 维度 | 真实内容 / 实测值 | 来源 / 证据ID | Owner |
|---|---|---|---|---|
| BIZ-01 | 发布资格 | {{required:发布资格的真实内容或实测值}} | {{required:来源或证据ID}} | {{required:Owner}} |
| BIZ-02 | 证据完整性 | {{required:证据完整性的真实内容或实测值}} | {{required:来源或证据ID}} | {{required:Owner}} |
| BIZ-03 | 风险 | {{required:风险的真实内容或实测值}} | {{required:来源或证据ID}} | {{required:Owner}} |
| BIZ-04 | 评审议题 | {{required:评审议题的真实内容或实测值}} | {{required:来源或证据ID}} | {{required:Owner}} |
| BIZ-05 | 人类决定 | {{required:人类决定的真实内容或实测值}} | {{required:来源或证据ID}} | {{required:Owner}} |

### 4.1 方案与决策依据

| 决策点 | 候选方案 | 选择 / 取舍 | 量化依据 | 反证 / 失效条件 |
|---|---|---|---|---|
| {{required:决策点}} | {{required:候选项}} | {{required:选择及原因}} | {{required:指标、口径和实测值}} | {{required:反例、不确定性或触发条件}} |

## 5. 方法、假设与约束

| ID | 类型 | 内容 | 验证方法 | 失效条件 / 状态 |
|---|---|---|---|---|
| ASM-001 | {{required:方法|假设|约束}} | {{required:具体内容}} | {{required:验证步骤或命令}} | {{required:失效条件与状态}} |

## 6. 追溯矩阵

| 上游对象ID | 本文条目ID | Claim ID | 证据ID | 下游对象 / Gate |
|---|---|---|---|---|
| {{required:上游ID}} | {{required:BIZ-01}} | {{required:CLM-001}} | {{required:EVD-001}} | {{required:下游产物或决策}} |

## 7. 风险与开放项

| ID | 内容 | 概率 | 影响 | Owner | 缓解 / 截止时间 |
|---|---|---|---|---|---|
| RSK-001 | {{required:风险，或“无已知风险”及依据}} | {{required:概率}} | {{required:影响}} | {{required:Owner}} | {{required:行动和ISO-8601时间}} |

## 8. 证据清单

| 证据ID | 类型 | 路径 / URI | SHA-256 | 采集命令 / 来源 | 时间 |
|---|---|---|---|---|---|
| EVD-001 | {{required:证据类型}} | {{required:真实路径或URI}} | {{required:64位SHA-256}} | {{required:可复现命令或权威来源}} | {{required:ISO-8601}} |

## 9. 验收执行记录

| 验收项ID | 可判定标准 | 执行步骤 / 命令 | 实测结果 | 证据ID | 执行者 |
|---|---|---|---|---|---|
| AC-001 | {{required:源设计验收标准}} | {{required:真实执行步骤或命令}} | {{required:PASS|FAIL|BLOCKED与实测值}} | {{required:EVD-001}} | {{required:执行者ID}} |

## 10. 独立验证

- 要求：必须由不同于 producer 的主体复核。
- 验证者 / Verdict：{{required:验证者ID或NOT_REQUIRED}} / {{required:PASS|FAIL|NOT_REQUIRED}}
- VerificationReceipt：{{required:回执路径或不适用依据}}

## 11. 人工决策与授权

- Gate：必须人工决定，Agent 不得代签。
- 决策 / 决策人：{{required:APPROVED|REJECTED|CONDITIONAL|NOT_REQUIRED}} / {{required:人类ID、角色或不适用依据}}
- 条件与 HumanApprovalRecord：{{required:条件、意见及签署记录路径}}

## 12. 发布清单

- [ ] 主交付物、清单和哈希已生成
- [ ] 所有 Claim 均有真实 Evidence
- [ ] 输入—结论—证据—下游可追溯
- [ ] 验收已执行并保留原始回执
- [ ] 独立验证和人审条件已满足
- [ ] activity-result.json 已通过 `validate_delivery.py`

