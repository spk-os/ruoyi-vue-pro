---
templateVersion: "1.0.0"
activityId: "ACT-03-05-04"
activityName: "准备环境与数据"
artifactType: "ReplayableTestEnvironment"
projectId: "{{required:项目ID}}"
processInstanceId: "{{required:流程实例ID}}"
runId: "{{required:验证执行ID}}"
baselineVersion: "{{required:冻结基线版本与哈希}}"
producerActorId: "{{required:验证产出者ID}}"
createdAt: "{{required:ISO-8601时间}}"
status: "{{required:DRAFT|READY_FOR_VERIFY|BLOCKED}}"
sourceDesign: "/root/share/SPK-OS/24_houre_anly/IPD/05-kimi-k3-design/04-六阶段每阶段-具体设计与Activity执行配置表/04-04-验证阶段.md"
---

# ReplayableTestEnvironment · 准备环境与数据

> 复制后填写。必须替换全部 `{{required:...}}`；测试计划、模拟结论或模板文字不能替代真实执行与原始回执。

## 1. 文档控制

| 项目 | 填写值 |
|---|---|
| 项目 / 候选版本 | {{required:项目与候选版本}} |
| Activity | ACT-03-05-04 / 准备环境与数据 |
| 验证基线 / SHA-256 | {{required:基线版本}} / {{required:基线哈希}} |
| Owner / 状态 | {{required:Owner}} / {{required:状态}} |

## 2. 执行摘要

- Verdict：{{required:PASS|FAIL|BLOCKED}}
- 覆盖范围：{{required:测试层级、场景、需求范围和排除项}}
- 实测指标：{{required:数值、单位、样本量和计算口径}}
- 阻断 / 未通过项：{{required:无，或缺陷ID、原因、Owner}}

## 3. 输入与基线

| 输入ID | 制品 / 配置 / 数据 | 版本 / 哈希 | 来源与授权 | 冻结时间 |
|---|---|---|---|---|
| IN-001 | {{required:输入对象}} | {{required:不可变版本和SHA-256}} | {{required:仓库、制品库或数据授权}} | {{required:ISO-8601}} |

## 4. 业务内容

| 条目ID | 验证维度 | 设计 / 实测结果 / 判定 | 原始证据ID | 状态 |
|---|---|---|---|---|
| VER-1 | IaC | {{required:IaC的设计、实测结果或判定}} | {{required:原始证据ID}} | {{required:PASS|FAIL|BLOCKED}} |
| VER-2 | 镜像 | {{required:镜像的设计、实测结果或判定}} | {{required:原始证据ID}} | {{required:PASS|FAIL|BLOCKED}} |
| VER-3 | 数据谱系 | {{required:数据谱系的设计、实测结果或判定}} | {{required:原始证据ID}} | {{required:PASS|FAIL|BLOCKED}} |
| VER-4 | 脱敏 | {{required:脱敏的设计、实测结果或判定}} | {{required:原始证据ID}} | {{required:PASS|FAIL|BLOCKED}} |
| VER-5 | 复现命令 | {{required:复现命令的设计、实测结果或判定}} | {{required:原始证据ID}} | {{required:PASS|FAIL|BLOCKED}} |

### 4.1 测试执行明细

| 测试ID | 需求 / 风险ID | 环境 | 数据集版本 | 命令 / 步骤 | 预期 | 实测 | Exit Code |
|---|---|---|---|---|---|---|---|
| {{required:TEST-001}} | {{required:REQ或RSK-ID}} | {{required:环境版本/hash}} | {{required:数据集ID/hash}} | {{required:真实执行命令}} | {{required:可判定预期}} | {{required:原始实测值}} | {{required:退出码}} |

## 5. 方法、假设与约束

| ID | 方法 / 假设 / 约束 | 内容 | 有效性验证 | 失效条件 |
|---|---|---|---|---|
| ASM-001 | {{required:类型}} | {{required:具体内容}} | {{required:验证步骤与证据ID}} | {{required:何时结论失效}} |

## 6. 追溯矩阵

| 需求 / 风险ID | 设计 / 代码对象 | 测试ID | 执行Run ID | 证据ID | 缺陷 / 结论 |
|---|---|---|---|---|---|
| {{required:REQ-ID}} | {{required:设计、接口或commit}} | {{required:TEST-001}} | {{required:RUN-ID}} | {{required:EVD-001}} | {{required:缺陷ID或PASS结论}} |

## 7. 风险与开放项

| ID | 残余风险 / 缺口 | 概率 | 影响 | 暴露值 / 口径 | Owner | 处置 / 截止 |
|---|---|---|---|---|---|---|
| RSK-001 | {{required:真实残余风险或无风险依据}} | {{required:概率}} | {{required:影响}} | {{required:计算值与口径}} | {{required:Owner}} | {{required:动作与时间}} |

## 8. 证据清单

| 证据ID | 类型 | 原始路径 / URI | SHA-256 | 生成命令 / 工具版本 | 时间 |
|---|---|---|---|---|---|
| EVD-001 | {{required:日志、报告、截图、指标或回执}} | {{required:真实可访问路径}} | {{required:64位SHA-256}} | {{required:命令与工具版本}} | {{required:ISO-8601}} |

## 9. 验收执行记录

| 验收项ID | 退出 / 通过标准 | 执行方法 | 实测值 | 判定 | 证据ID | 执行者 |
|---|---|---|---|---|---|---|
| AC-001 | {{required:源设计可判定标准}} | {{required:已执行步骤或命令}} | {{required:原始实测值}} | {{required:PASS|FAIL|BLOCKED}} | {{required:EVD-001}} | {{required:执行者ID}} |

## 10. 独立验证

- 要求：强制；验证者须不同于产出者，并核验原始回执和可复现性。
- 验证者 / Verdict：{{required:独立验证者ID}} / {{required:PASS|FAIL}}
- VerificationReceipt：{{required:已签署回执路径}}

## 11. 人工决策与授权

- Gate：非固定Gate；风险接受、豁免或不可逆动作必须人审。
- 决策 / 决策人：{{required:APPROVED|REJECTED|CONDITIONAL|NOT_REQUIRED}} / {{required:人类ID、角色或不适用依据}}
- 条件与 HumanApprovalRecord：{{required:签署记录路径及条件}}

## 12. 发布清单

- [ ] 基线、环境和数据可重放且哈希固定
- [ ] 所有测试已真实执行并保留原始回执
- [ ] 需求—实现—测试—证据—缺陷可追溯
- [ ] 失败、跳过项和残余风险均显式记录
- [ ] 独立验证及必要人审已完成
- [ ] activity-result.json 已通过 `validate_delivery.py`

