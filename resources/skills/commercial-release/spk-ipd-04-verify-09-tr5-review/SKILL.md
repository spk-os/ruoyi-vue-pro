---
name: spk-ipd-04-verify-09-tr5-review
description: 执行商业软件 IPD 04 验证阶段 09「TR5 资格评审」，生成并验证 TR5QualificationDecision。当流程进入 ACT-03-05-09、需要补交该产物或修复其证据链时使用。
---

# 04.09 TR5 资格评审

本 Skill 是 `ACT-03-05-09` 的执行入口。它负责真实完成活动、生成 `TR5QualificationDecision`、采集证据并给出可机读状态；不只是说明规则。

## 读取顺序

1. 读取 `references/execution-spec.md`，取得本活动的输入、算法、完成条件和失败策略。
2. 读取 `references/source-design.md`，执行原设计中的资源表、工具顺序、Artifact Schema 和上下游约束。
3. 复制并填写 `../templates/verify/ACT-03-05-09-TR5QualificationDecision.template.md`；模板是正式交付文档骨架，不能提交空模板。
4. 读取 `../references/agent-execution-contract.md` 与 `../references/ai-native-controls.md`，应用全包证据和安全约束。
5. 遇到边界或失败分支时读取 `references/examples.md`，按相近案例处理但仍以本次真实证据为准。

## 输入就绪

- 解析 `projectId`、`processInstanceId`、`activityRunId`、`runId`、目标基线、工作目录和责任角色。
- 按执行规约逐项验证上游产物、数据源、版本、哈希、新鲜度、授权和验收标准。
- 任一强制输入缺失、哈希不符、权限不足或基线冲突时，将状态置为 `BLOCKED`；不得用模型常识补造。
- 建立本次 `input-manifest.json`，在任何分析或生成动作前冻结输入快照。

## 执行工作流

1. 建立隔离工作区及 `artifacts/`、`evidence/`、`logs/`、`receipts/`，记录模型、Skill、Prompt、工具和依赖版本。
2. 严格执行 `execution-spec.md` 的领域算法；每个中间结论同时登记 claim、来源证据、置信度和反证。
3. 按原设计的工具顺序调用真实工具。确定性提取、编译、计算、测试、Schema 校验优先于模型判断。
4. 使用正式模板生成 `TR5QualificationDecision`；所有必填字段填入本项目真实内容，保留假设、风险和开放项。
5. 执行结构、领域、追溯和独立复核（若 catalog 要求），保存原始输出与退出码。
6. 生成 ArtifactManifest、EvidenceManifest、RunReceipt 和 `activity-result.json`，计算每个文件的 SHA-256。
7. 运行 `python3 ../scripts/validate_delivery.py <交付目录>/activity-result.json`；修复所有错误后才可请求移交。

## 完成定义

仅当 `TR5QualificationDecision` 已按模板填满、执行规约中的验收项全部有实测结果、每个关键 claim 可直达证据、所有哈希匹配、无未处理冲突且下游能够消费时，状态才允许为 `COMPLETE`。禁止以“文档已生成”“大部分完成”或模型自评替代验收。

## 失败处理

工具失败按执行规约的重试预算处理；连续失败时保留现场并升级。发现错误写入时回滚到输入基线，禁止继续污染产物。需要人工门禁、越权访问、不可逆生产动作或低置信度重大决策时停止执行，不得代替授权人。

## 移交

提交主产物、输入清单、证据清单、验证回执、活动结果和开放项；向下游说明基线版本、可依赖结论、不得依赖内容、剩余风险与下一责任人。移交后若上游证据失效，按 Claim → Artifact → 下游活动传播失效。



