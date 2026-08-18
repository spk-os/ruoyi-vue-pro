---
templateVersion: "1.0.0"
activityId: "ACT-03-04-08"
activityName: "变更控制"
artifactType: "ChangeDecisionRecord"
projectId: "{{required:项目ID}}"
processInstanceId: "{{required:流程实例ID}}"
runId: "{{required:执行ID}}"
baselineVersion: "{{required:开发基线版本}}"
producerActorId: "{{required:产出者ID}}"
createdAt: "{{required:ISO-8601时间}}"
status: "{{required:DRAFT|READY_FOR_VERIFY|BLOCKED}}"
sourceDesign: "/root/share/SPK-OS/24_houre_anly/IPD/05-kimi-k3-design/04-六阶段每阶段-具体设计与Activity执行配置表/04-03-开发阶段.md"
---

# ChangeDecisionRecord · 变更控制

> 复制后填写并提交；必须替换所有 `{{required:...}}`。仅有过程描述、规则或计划，不能替代真实代码、命令回执和证据。

## 1. 文档控制

| 项目 | 填写值 |
|---|---|
| 项目 / 版本 / 提交 | {{required:项目、版本和commit SHA}} |
| Activity | ACT-03-04-08 / 变更控制 |
| 交付物 | ChangeDecisionRecord |
| 文档版本 / SHA-256 | {{required:版本}} / {{required:内容SHA-256}} |
| Owner / 状态 | {{required:Owner}} / {{required:状态}} |

## 2. 执行摘要

- 结论：{{required:可验证结论}}
- 变更范围：{{required:模块、文件、接口与不覆盖项}}
- 实测结果：{{required:关键指标、数量、单位和口径}}
- 阻断项：{{required:无，或问题ID、原因和Owner}}

## 3. 输入与基线

| 输入ID | 类型 | 版本 / 哈希 | 来源 / 授权 | 用途 |
|---|---|---|---|---|
| IN-001 | {{required:需求、架构、代码或配置}} | {{required:不可变版本/commit/hash}} | {{required:仓库路径、URI与授权}} | {{required:本活动用途}} |

## 4. 业务内容

| 条目ID | 维度 | 真实变更 / 实测内容 | 代码、命令或证据ID | Owner |
|---|---|---|---|---|
| BIZ-1 | 变更请求 | {{required:变更请求的真实内容或实测值}} | {{required:关联代码、命令或证据ID}} | {{required:Owner}} |
| BIZ-2 | 影响 | {{required:影响的真实内容或实测值}} | {{required:关联代码、命令或证据ID}} | {{required:Owner}} |
| BIZ-3 | 选项 | {{required:选项的真实内容或实测值}} | {{required:关联代码、命令或证据ID}} | {{required:Owner}} |
| BIZ-4 | CCB人类决定 | {{required:CCB人类决定的真实内容或实测值}} | {{required:关联代码、命令或证据ID}} | {{required:Owner}} |
| BIZ-5 | 生效条件 | {{required:生效条件的真实内容或实测值}} | {{required:关联代码、命令或证据ID}} | {{required:Owner}} |

### 4.1 变更明细与可复现操作

| 对象 / 文件 | Before | After | 执行命令 / 迁移步骤 | 回滚步骤 |
|---|---|---|---|---|
| {{required:对象或文件路径}} | {{required:变更前版本/行为}} | {{required:变更后版本/行为}} | {{required:可复现命令}} | {{required:已验证的回滚命令或步骤}} |

## 5. 方法、假设与约束

| ID | 类型 | 内容 | 验证方法 | 失效条件 / 状态 |
|---|---|---|---|---|
| ASM-001 | {{required:方法|假设|约束}} | {{required:具体内容}} | {{required:验证命令或审查方法}} | {{required:失效条件和状态}} |

## 6. 追溯矩阵

| 需求 / 缺陷ID | 设计 / ADR | 代码 / Commit | 测试ID | 证据ID | 下游产物 |
|---|---|---|---|---|---|
| {{required:REQ-ID}} | {{required:ADR或设计ID}} | {{required:文件与commit SHA}} | {{required:TEST-ID}} | {{required:EVD-001}} | {{required:制品或Gate}} |

## 7. 风险与开放项

| ID | 风险 / 技术债 | 概率 | 影响 | Owner | 缓解 / 截止时间 |
|---|---|---|---|---|---|
| RSK-001 | {{required:真实风险或“无已知风险”及依据}} | {{required:概率}} | {{required:影响}} | {{required:Owner}} | {{required:行动与ISO-8601时间}} |

## 8. 证据清单

| 证据ID | 类型 | 路径 / URI | SHA-256 | 生成命令 / 来源 | 时间 |
|---|---|---|---|---|---|
| EVD-001 | {{required:日志、测试、扫描、制品或回执}} | {{required:真实路径或URI}} | {{required:64位SHA-256}} | {{required:真实执行命令或工具版本}} | {{required:ISO-8601}} |

## 9. 验收执行记录

| 验收项ID | 标准 | 环境 | 命令 / 步骤 | Exit Code / 实测值 | 结果 | 证据ID |
|---|---|---|---|---|---|---|
| AC-001 | {{required:可判定标准}} | {{required:环境版本与配置hash}} | {{required:已执行命令}} | {{required:退出码和实测值}} | {{required:PASS|FAIL|BLOCKED}} | {{required:EVD-001}} |

## 10. 独立验证

- 要求：必须由非产出者复核真实代码、运行结果与证据。
- 验证者 / Verdict：{{required:验证者ID或NOT_REQUIRED}} / {{required:PASS|FAIL|NOT_REQUIRED}}
- VerificationReceipt：{{required:回执路径或不适用依据}}

## 11. 人工决策与授权

- Gate：必须人工决定，Agent 不得代表 CCB/TR 签字。
- 决策 / 决策人：{{required:APPROVED|REJECTED|CONDITIONAL|NOT_REQUIRED}} / {{required:人类ID与角色或依据}}
- 条件与 HumanApprovalRecord：{{required:签署记录路径及条件}}

## 12. 发布清单

- [ ] 代码、配置或产物真实存在并记录不可变版本
- [ ] 测试、扫描和验证命令已执行并保留原始回执
- [ ] 需求—设计—代码—测试—证据可追溯
- [ ] 回滚或恢复路径已验证
- [ ] 独立验证和人审条件已满足
- [ ] activity-result.json 已通过 `validate_delivery.py`

