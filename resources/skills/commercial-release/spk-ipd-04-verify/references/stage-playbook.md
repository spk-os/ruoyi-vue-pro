# 04 验证阶段运行手册

## 阶段目标

通过 TR5，需求覆盖、缺陷与残余风险达到发布资格。本阶段关键路径：`01 → 02 → 03 → 04 → 05 ↔ 06；07 可与 05 并行 → 08 → 09(TR5)`。

## 调度图

`01 → 02 → 03 → 04 → 05 ↔ 06；07 可与 05 并行 → 08 → 09(TR5)`

调度器必须用真实 Artifact 状态解析依赖，不以菜单点击、任务已创建或 Agent 声称完成作为节点完成信号。并行节点使用独立工作区；共享基线、同一文件或同一外部写资源必须加锁。

## Activity 路由

| 序号 | Activity ID | 活动 | 执行 Skill | 主产物 | 人工责任 |
|---|---|---|---|---|---|
| 01 | ACT-03-05-01 | 冻结验证基线 | `spk-ipd-04-verify-01-baseline-freeze` | `VerificationBaselineManifest` | QA Lead + Configuration Manager 联合锁定验证基线 |
| 02 | ACT-03-05-02 | 建立 RVM/RTM | `spk-ipd-04-verify-02-rvm-rtm` | `RVM_RTM_Baseline` | SE 审批验证方法和 Oracle owner；Test Architect 审批测试策略 |
| 03 | ACT-03-05-03 | 设计多层测试 | `spk-ipd-04-verify-03-test-design` | `MultiLayerTestDesign` | Test Architect 审批策略与 Oracle；SE 确认关键路径覆盖 |
| 04 | ACT-03-05-04 | 准备环境与数据 | `spk-ipd-04-verify-04-env-prep` | `ReplayableTestEnvironment` | Ops 执行环境部署；Data Owner 授权数据使用 |
| 05 | ACT-03-05-05 | 执行与采证 | `spk-ipd-04-verify-05-execute` | `TestExecutionEvidence` | Test Lead 判定测试结果有效性 |
| 06 | ACT-03-05-06 | 缺陷分诊闭环 | `spk-ipd-04-verify-06-defect-triage` | `DefectClosureRecord` | PDT/QA 独占严重度、优先级与关闭 |
| 07 | ACT-03-05-07 | 用户场景验证 | `spk-ipd-04-verify-07-user-validation` | `UserValidationReport` | Product/VOC Owner 判断代表性和接受度 |
| 08 | ACT-03-05-08 | 计算残余风险 | `spk-ipd-04-verify-08-residual-risk` | `ResidualRiskReport` | Risk Owner 评估接受/缓解/豁免 |
| 09 | ACT-03-05-09 | TR5 资格评审 | `spk-ipd-04-verify-09-tr5-review` | `TR5QualificationDecision` | 具名评委签署 go/conditional/no-go（**AI 无投票/签署权**） |

## 阶段入口

- 上一阶段 baseline manifest 的签名和 SHA-256 均有效。
- 上一门禁的条件项已关闭，或已明确授权在本阶段处理且有 owner/期限。
- 项目范围、目标版本、流程实例、资源承诺和授权边界一致。
- 每个 READY Activity 的 REQUIRED 输入可以定位、读取和验证。
- Agent、Worker、Verifier 与人工责任人的身份隔离满足原设计。

## 编排规则

1. 为每个 Activity 创建唯一 runId 和不可共享的工作目录。
2. 读取 catalog 后再加载 Activity Skill，禁止凭名称猜测执行逻辑。
3. 先执行确定性工具与数据采集，再运行模型推理；模型输出必须验证。
4. 同一 Activity 最多按其规约重试；超过预算升级，不创建新的“成功”运行掩盖失败。
5. 任何 Artifact 更新都生成新版本与哈希，并使旧版本的下游消费关系失效。
6. 每日检查 critical path、阻断、证据缺口、人工待签、成本/token 与风险变化。
7. 阶段门前冻结材料；冻结后变更只能走 CCB 并重新验证受影响节点。

## 阶段出口检查

- 9 个 Activity 的主产物、活动结果、输入/证据/运行清单齐全。
- 所有强制模板字段已填充，Schema、哈希、链接和可重放验证通过。
- Requirement ↔ Code/Artifact ↔ Test/Evidence 追溯无孤儿。
- 人工责任记录包含具名主体、角色、决定、时间、基线哈希和签名。
- 独立 Verifier 与 producer 身份隔离，挑战和处理结果均落盘。
- 开放项不会阻断出口，且每项有 owner、期限、触发器和升级路径。
- 下一阶段对移交包完成机器解析与责任接收。

## 恢复和回滚

恢复运行时读取 ledger 和最后签名检查点，逐个复验文件哈希、外部资源版本和权限；不从聊天摘要推测状态。局部失败回滚本 Activity，基线级失败回滚整个受影响子图。人工否决保持证据并生成新候选版本，不覆写历史决策。

