# 02 计划阶段运行手册

## 阶段目标

通过 TR2/PDCP，需求、架构、资源与验证承诺形成基线。本阶段关键路径：`01/02 协同迭代 → 03/04/05 可并行收敛 → 06(TR2/PDCP)`。

## 调度图

`01/02 协同迭代 → 03/04/05 可并行收敛 → 06(TR2/PDCP)`

调度器必须用真实 Artifact 状态解析依赖，不以菜单点击、任务已创建或 Agent 声称完成作为节点完成信号。并行节点使用独立工作区；共享基线、同一文件或同一外部写资源必须加锁。

## Activity 路由

| 序号 | Activity ID | 活动 | 执行 Skill | 主产物 | 人工责任 |
|---|---|---|---|---|---|
| 01 | ACT-03-03-01 | 产品需求规格化 | `spk-ipd-02-plan-01-requirements-spec` | `PRSBaselineCandidate` | Product Manager / SE 签署需求语义和优先级 |
| 02 | ACT-03-03-02 | 架构描述生成 | `spk-ipd-02-plan-02-architecture-design` | `ArchitectureDescriptionCandidate` | Chief Architect 签署架构基线和 ADR |
| 03 | ACT-03-03-03 | 项目计划编制 | `spk-ipd-02-plan-03-project-planning` | `IntegratedProjectPlan` | PM + Resource Owners 签署计划基线和资源承诺 |
| 04 | ACT-03-03-04 | CBB 复用决策 | `spk-ipd-02-plan-04-cbb-decision` | `CBBReuseDecision` | SE / TDT / Legal / Security 签署复用决策 |
| 05 | ACT-03-03-05 | 测试策略规划 | `spk-ipd-02-plan-05-test-strategy` | `VerificationStrategy` | Test Architect / QA Lead 签署测试策略和豁免 |
| 06 | ACT-03-03-06 | TR2/PDCP 决策包组装 | `spk-ipd-02-plan-06-evidence-pack` | `TR2_PDCP_EvidencePack` | PDT Lead 提交；TR2 评审团队技术结论；IPMT 投资决策（**AI 禁写 DCP 状态**） |

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

- 6 个 Activity 的主产物、活动结果、输入/证据/运行清单齐全。
- 所有强制模板字段已填充，Schema、哈希、链接和可重放验证通过。
- Requirement ↔ Code/Artifact ↔ Test/Evidence 追溯无孤儿。
- 人工责任记录包含具名主体、角色、决定、时间、基线哈希和签名。
- 独立 Verifier 与 producer 身份隔离，挑战和处理结果均落盘。
- 开放项不会阻断出口，且每项有 owner、期限、触发器和升级路径。
- 下一阶段对移交包完成机器解析与责任接收。

## 恢复和回滚

恢复运行时读取 ledger 和最后签名检查点，逐个复验文件哈希、外部资源版本和权限；不从聊天摘要推测状态。局部失败回滚本 Activity，基线级失败回滚整个受影响子图。人工否决保持证据并生成新候选版本，不覆写历史决策。

