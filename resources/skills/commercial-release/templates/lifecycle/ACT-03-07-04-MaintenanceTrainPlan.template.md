---
templateVersion: "1.0.0"
activityId: "ACT-03-07-04"
activityName: "Maintenance Train Management"
artifactType: "MaintenanceTrainPlan"
projectId: "{{required:项目ID}}"
processInstanceId: "{{required:流程实例ID}}"
runId: "{{required:执行ID}}"
baselineVersion: "{{required:在服版本或生命周期基线}}"
producerActorId: "{{required:产出者ID}}"
createdAt: "{{required:ISO-8601时间}}"
status: "{{required:DRAFT|READY_FOR_VERIFY|BLOCKED}}"
sourceDesign: "/root/share/SPK-OS/24_houre_anly/IPD/05-kimi-k3-design/04-六阶段每阶段-具体设计与Activity执行配置表/04-06-生命周期管理阶段.md"
---

# MaintenanceTrainPlan · Maintenance Train Management

> 复制后填写，替换全部 `{{required:...}}`。分析必须基于真实在服信号、客户授权数据和可追溯证据。

## 1. 文档控制

| 项目 | 填写值 |
|---|---|
| 产品 / 在服版本 | {{required:产品与版本范围}} |
| Activity | ACT-03-07-04 / Maintenance Train Management |
| 观察窗口 | {{required:开始与结束ISO-8601时间}} |
| 文档版本 / SHA-256 | {{required:版本}} / {{required:内容SHA-256}} |
| Owner / 状态 | {{required:Owner}} / {{required:状态}} |

## 2. 执行摘要

- 结论：{{required:可验证的生命周期结论或决策建议}}
- 范围 / 样本：{{required:版本、客户群、区域、样本量和排除项}}
- 关键指标：{{required:数值、单位、口径、基线与趋势}}
- 阻断 / 异常：{{required:无，或事件ID、影响和Owner}}

## 3. 输入与基线

| 输入ID | 信号 / 数据类型 | 时间窗 / 版本 | 来源与授权 | 质量 / 完整性 |
|---|---|---|---|---|
| IN-001 | {{required:遥测、工单、安全、商业或客户数据}} | {{required:窗口、版本或哈希}} | {{required:来源URI和授权/隐私依据}} | {{required:覆盖率、缺失率和偏差}} |

## 4. 业务内容

| 条目ID | 维度 | 真实数据 / 分析 / 动作 | 来源 / 证据ID | Owner |
|---|---|---|---|---|
| LC-1 | 范围 | {{required:范围的真实数据、分析或动作}} | {{required:来源或证据ID}} | {{required:Owner}} |
| LC-2 | 依赖 | {{required:依赖的真实数据、分析或动作}} | {{required:来源或证据ID}} | {{required:Owner}} |
| LC-3 | 窗口 | {{required:窗口的真实数据、分析或动作}} | {{required:来源或证据ID}} | {{required:Owner}} |
| LC-4 | 回归 | {{required:回归的真实数据、分析或动作}} | {{required:来源或证据ID}} | {{required:Owner}} |
| LC-5 | 回滚 | {{required:回滚的真实数据、分析或动作}} | {{required:来源或证据ID}} | {{required:Owner}} |

### 4.1 分群、趋势与行动

| 对象 / 分群 | 基线 | 当前值 | 趋势 / 差异 | 建议动作 | 触发阈值 |
|---|---|---|---|---|---|
| {{required:版本、客户群或对象}} | {{required:基线值及口径}} | {{required:当前实测值}} | {{required:趋势与统计显著性}} | {{required:具体行动}} | {{required:升级、回滚或复审阈值}} |

## 5. 方法、假设与约束

| ID | 方法 / 假设 / 约束 | 内容 | 验证方法 | 失效条件 |
|---|---|---|---|---|
| ASM-001 | {{required:类型}} | {{required:具体内容}} | {{required:交叉验证或复算方法}} | {{required:结论失效条件}} |

## 6. 追溯矩阵

| 信号 / 事件ID | 分析条目ID | Claim ID | 证据ID | 需求 / 维护 / EOL动作 |
|---|---|---|---|---|
| {{required:SIG或EVT-ID}} | {{required:LC-1}} | {{required:CLM-001}} | {{required:EVD-001}} | {{required:下游工作项、版本或决策}} |

## 7. 风险与开放项

| ID | 风险 / 开放项 | 概率 | 影响 | Owner | 处置 / SLA / 截止 |
|---|---|---|---|---|---|
| RSK-001 | {{required:安全、客户、合规或运营风险}} | {{required:概率}} | {{required:影响与受影响范围}} | {{required:Owner}} | {{required:动作和时间}} |

## 8. 证据清单

| 证据ID | 类型 | 路径 / URI | SHA-256 | 查询 / 采集方法 | 时间窗 |
|---|---|---|---|---|---|
| EVD-001 | {{required:遥测、工单、SBOM、账单、回执或报告}} | {{required:真实路径或URI}} | {{required:64位SHA-256}} | {{required:可复现查询或权威来源}} | {{required:ISO-8601窗口}} |

## 9. 验收执行记录

| 验收项ID | 可判定标准 | 执行 / 复算方法 | 实测结果 | 判定 | 证据ID |
|---|---|---|---|---|---|
| AC-001 | {{required:源设计验收标准}} | {{required:实际查询、脚本或审查步骤}} | {{required:数值、样本和口径}} | {{required:PASS|FAIL|BLOCKED}} | {{required:EVD-001}} |

## 10. 独立验证

- 要求：必须由不同于产出者的主体复核数据、计算与结论。
- 验证者 / Verdict：{{required:验证者ID或NOT_REQUIRED}} / {{required:PASS|FAIL|NOT_REQUIRED}}
- VerificationReceipt：{{required:回执路径或不适用依据}}

## 11. 人工决策与授权

- Gate：非固定Gate；客户通知、风险接受、退市或不可逆动作须人审。
- 决策 / 决策人：{{required:APPROVED|REJECTED|CONDITIONAL|NOT_REQUIRED}} / {{required:人类ID、角色或依据}}
- 条件、客户授权与 HumanApprovalRecord：{{required:签署/授权记录及条件}}

## 12. 发布清单

- [ ] 数据来源、窗口、授权、口径和质量已登记
- [ ] Claims 均连接到不可变 Evidence
- [ ] 信号—分析—行动—需求/维护/退市可追溯
- [ ] 查询、复算或验证已执行并保存回执
- [ ] 独立验证及必要人审已完成
- [ ] activity-result.json 已通过 `validate_delivery.py`

