---
templateVersion: "1.0.0"
activityId: "ACT-03-02-06"
activityName: "技术风险与 CBB 识别"
artifactType: "TechnicalRiskAndCBBMap"
projectId: "{{required:项目ID}}"
processInstanceId: "{{required:流程实例ID}}"
runId: "{{required:本次执行ID}}"
baselineVersion: "{{required:输入基线版本}}"
producerActorId: "{{required:产出Agent或人员ID}}"
createdAt: "{{required:ISO-8601时间}}"
status: "{{required:DRAFT|READY_FOR_VERIFY|BLOCKED}}"
sourceDesign: "/root/share/SPK-OS/24_houre_anly/IPD/05-kimi-k3-design/04-六阶段每阶段-具体设计与Activity执行配置表/04-01-概念阶段.md"
---

# TechnicalRiskAndCBBMap · 技术风险与 CBB 识别

> 使用方式：复制本文件到交付目录后填写；所有 `{{required:...}}` 必须替换为真实值。不得把本模板本身当作交付证据。

## 1. 文档控制

| 项目 | 填写值 |
|---|---|
| 项目 / 版本 | {{required:项目名称与版本}} |
| Activity | ACT-03-02-06 / 技术风险与 CBB 识别 |
| 交付物类型 | TechnicalRiskAndCBBMap |
| 文档版本 / 哈希 | {{required:版本号}} / {{required:内容SHA-256}} |
| 负责人 / 状态 | {{required:Owner}} / {{required:当前状态}} |

## 2. 执行摘要

- 结论：{{required:用可验证语言写结论}}
- 范围：{{required:本次覆盖与明确不覆盖范围}}
- 关键数字：{{required:关键指标及计算口径}}
- 阻断项：{{required:无，或列出阻断项ID与原因}}

## 3. 输入与基线

| 输入ID | 类型 | 版本 / 哈希 | 来源与授权 | 用途 |
|---|---|---|---|---|
| {{required:IN-001}} | {{required:输入类型}} | {{required:不可变版本或哈希}} | {{required:来源URI及授权记录}} | {{required:用于哪项分析}} |

## 4. 业务内容

| 分析维度 | 真实内容 / 数值 | 来源 / 证据ID | Owner |
|---|---|---|---|
| 技术风险 | {{required:技术风险的真实内容}} | {{required:技术风险来源或证据ID}} | {{required:Owner}} |
| CBB 候选 | {{required:CBB 候选的真实内容}} | {{required:CBB 候选来源或证据ID}} | {{required:Owner}} |
| 契约匹配 | {{required:契约匹配的真实内容}} | {{required:契约匹配来源或证据ID}} | {{required:Owner}} |
| 缓解措施 | {{required:缓解措施的真实内容}} | {{required:缓解措施来源或证据ID}} | {{required:Owner}} |

### 4.1 结论与选择

| 决策点 | 候选项 | 选择及理由 | 反证 / 不确定性 |
|---|---|---|---|
| {{required:决策点}} | {{required:候选方案}} | {{required:选择、取舍和量化依据}} | {{required:反例、未知项或失效条件}} |

## 5. 方法、假设与约束

| ID | 类型 | 内容 | 验证方法 / 失效条件 | 状态 |
|---|---|---|---|---|
| ASM-001 | {{required:方法或假设或约束}} | {{required:具体内容}} | {{required:如何验证以及何时失效}} | {{required:OPEN|VALIDATED|REJECTED}} |

## 6. 追溯矩阵

| 上游对象ID | 本文条目ID | Claim ID | 证据ID | 下游对象 / 决策 |
|---|---|---|---|---|
| {{required:需求或信号ID}} | {{required:本文条目ID}} | {{required:CLM-001}} | {{required:EVD-001}} | {{required:下游产物、Gate或动作}} |

## 7. 风险与开放项

| ID | 风险 / 开放项 | 概率 | 影响 | Owner | 缓解 / 截止时间 |
|---|---|---|---|---|---|
| RSK-001 | {{required:真实风险或“无已知风险”及依据}} | {{required:概率}} | {{required:影响}} | {{required:Owner}} | {{required:动作与ISO-8601截止时间}} |

## 8. 证据清单

| 证据ID | 类型 | 路径 / URI | SHA-256 | 采集命令 / 来源 | 采集时间 |
|---|---|---|---|---|---|
| EVD-001 | {{required:原始数据、日志、报告或回执}} | {{required:可访问路径或URI}} | {{required:64位SHA-256}} | {{required:可复现命令或权威来源}} | {{required:ISO-8601时间}} |

## 9. 验收执行记录

| 验收项ID | 验收标准 | 执行方法 / 命令 | 结果 | 证据ID | 执行者 |
|---|---|---|---|---|---|
| AC-001 | {{required:来自源设计的可判定标准}} | {{required:真实执行步骤或命令}} | {{required:PASS|FAIL|BLOCKED及实测值}} | {{required:EVD-001}} | {{required:执行者ID}} |

## 10. 独立验证

- 要求：必须由不同于 producer 的验证者执行并签署 VerificationReceipt。
- 验证者：{{required:验证者ID或NOT_REQUIRED及依据}}
- Verdict：{{required:PASS|FAIL|NOT_REQUIRED}}
- 验证回执：{{required:VerificationReceipt路径或不适用依据}}

## 11. 人工决策与授权

- 人工 Gate：非固定Gate；涉及不可逆、高风险或外部承诺时升级人审。
- 决策：{{required:APPROVED|REJECTED|CONDITIONAL|NOT_REQUIRED}}
- 决策人 / 角色：{{required:人员ID与角色，或不适用依据}}
- 条件、意见与签署记录：{{required:HumanApprovalRecord路径或完整说明}}

## 12. 发布清单

- [ ] 主交付物已生成，文件哈希已登记
- [ ] Claims 均链接到真实 Evidence
- [ ] 输入、输出及下游对象可追溯
- [ ] 验收命令已真实执行并保存原始回执
- [ ] 独立验证与人审条件已满足
- [ ] 已生成 activity-result.json，且 `validate_delivery.py` 通过

