# 需求级 Agent 隔离与交付契约

本契约适用于所有包含多个可实现需求的商用软件项目，由 IPD 流程自动编排，不得转为审批人手工启动或管理 Agent。

## 强制调度

1. `DevelopmentPackageSet` 必须为每个可实现 `REQ-ID` 生成唯一的需求包，不得遗漏、重叠或把多个需求合并成无法独立验收的任务。
2. 每个需求包必须由一个独立 Agent run 执行，具有唯一 `agentRunId`、隔离工作区、分支、输入哈希、输出 commit 和测试证据。
3. 所有需求 Agent 必须由 Cortex→Omnigent→Claude Code 自动启动。启动回执须证明 runtime 为 `claude`、模型与项目策略完全一致；商用流程不得回退到其他 adapter、runtime 或模型。
4. Agent 只能修改其需求包授权的路径。共享文件修改必须由集成 Agent 按锁序合并，不得让并行 Agent 相互覆盖。
5. 单一需求 Agent 失败时仅重跑该需求包；超过重试预算则整个实现活动为 `BLOCKED`，不得用其他 Agent 的成功掩盖。

## 每个 REQ 的完成证据

每个 `REQ-ID` 必须同时提交：

- 需求与验收标准的冻结哈希；
- Agent/Omnigent/Claude Code 运行回执与模型证明；
- 独立分支、commit SHA 和变更路径清单；
- 实际执行的构建、单元/组件测试命令、退出码和原始日志哈希；
- `REQ ↔ design/API ↔ code/commit ↔ test/evidence` 追溯行；
- Agent 结论 `PASS|FAIL|BLOCKED`。只有 `PASS` 才能进入集成。

## 集成与独立验证

需求 Agent 全部 PASS 后，由不同 run 的集成 Agent 在新工作区合并，执行整体构建和回归。独立 Verifier 必须核对 Agent 数量与需求数量一致、运行隔离、模型路由、每个 REQ 证据和集成结果。任一缺失或非 PASS 都必须阻断 TR3/TR4。

