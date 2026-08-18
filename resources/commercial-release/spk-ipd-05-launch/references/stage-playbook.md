# 05 发布阶段运行手册

## 阶段目标

通过 TR6 与 GA，运营接收且稳定观察达标。本阶段关键路径：`01/02/03 并行准备 → 04 渐进发布 → 05(TR6) → 06(GA/移交/观察)`。

## 调度图

`01/02/03 并行准备 → 04 渐进发布 → 05(TR6) → 06(GA/移交/观察)`

调度器必须用真实 Artifact 状态解析依赖，不以菜单点击、任务已创建或 Agent 声称完成作为节点完成信号。并行节点使用独立工作区；共享基线、同一文件或同一外部写资源必须加锁。

## Activity 路由

| 序号 | Activity ID | 活动 | 执行 Skill | 主产物 | 人工责任 |
|---|---|---|---|---|---|
| 01 | ACT-03-06-01 | Release Manifest 构建与验证 | `spk-ipd-05-launch-01-manifest` | `ReleaseManifest` | Release Manager 最终审批 Manifest 并签署 |
| 02 | ACT-03-06-02 | 配置与数据迁移验证 | `spk-ipd-05-launch-02-migration` | `MigrationVerificationPack` | DBA/Platform 审核迁移方案、执行回滚演练；Data Owner 授权数据使用 |
| 03 | ACT-03-06-03 | 运营就绪验证 | `spk-ipd-05-launch-03-readiness` | `OperationalReadinessRecord` | SRE Lead 确认运营就绪、签署 Runbook；LMT Lead 确认接收能力 |
| 04 | ACT-03-06-04 | 渐进发布策略执行 | `spk-ipd-05-launch-04-rollout` | `ProgressiveRolloutRecord` | Release Commander 保留人工中止/恢复权；高风险扩流需人工确认 |
| 05 | ACT-03-06-05 | TR6 证据包组装与评审 | `spk-ipd-05-launch-05-tr6` | `TR6DecisionPack` | 具名 TR6 评委签署（**AI 无投票/签署权**） |
| 06 | ACT-03-06-06 | GA 决策、移交与稳定观察 | `spk-ipd-05-launch-06-ga-handoff` | `GA_Handoff_StabilityRecord` | GA Authority 签署 GA 决定；PDT Lead 和 LMT Lead 双方签署移交协议；LMT Lead 确认稳定期结束 |

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

