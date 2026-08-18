---
name: spk-ipd-commercial-release
description: 编排 Agent 驱动的商业软件产品完整 IPD 交付，从 01 概念、02 计划、03 开发、04 验证、05 发布到 06 生命周期，保持产物、证据、审批、风险和基线连续。当创建完整产品交付、恢复跨阶段流程或审计全生命周期时使用。
---

# 商业软件 IPD 全产品交付

这是六阶段总编排入口。它维护产品级状态、选择编号阶段 Skill，并保证任何阶段都不能脱离前后契约孤立完成。

## 读取顺序

1. 读取 `references/end-to-end-playbook.md`。
2. 读取 `../references/activity-catalog.json` 和 `../references/agent-execution-contract.md`。
3. 按当前阶段调用 `spk-ipd-01-concept` 至 `spk-ipd-06-lifecycle`。
4. 阶段 Skill 再按 catalog 路由具体 Activity Skill；不要跳过这一层直接猜测动作。

## 输入就绪

确认产品目标、商业发布范围、目标市场/客户、版本策略、流程实例、预算、RACI、授权边界、数据合规、仓库与环境。创建 Product Delivery Ledger，任何没有 ledger 的运行禁止开始。

## 执行工作流

1. 建立六阶段状态和 50 个 Activity 的依赖图，全部初始为 NOT_READY。
2. 从当前有效门禁和签名基线恢复状态；没有历史时从 01 概念开始。
3. 每次只激活依赖已验证的阶段 Skill，阶段内并发由阶段手册控制。
4. 持续维护需求—设计—代码—测试—发布—运行信号追溯和 Claim—Evidence 图。
5. 门禁前冻结基线，等待具名人类签署；Agent 只准备、质询、复算和记录。
6. 阶段完成后执行双向移交确认，并订阅上游证据/假设失效事件。
7. GA 后不结束流程，进入 06 生命周期，持续吸收价值、安全、反馈、维护和 EOL 信号。

## 完成定义

完整产品交付没有单一“一次完成”状态：GA 表示进入运营，生命周期 COMPLETE 只适用于已完成 LDCP、客户迁移、法定义务和可恢复归档的具体版本。任一阶段都必须以验证后的 Artifact 和签名决策为事实来源。

## 失败处理

使用最小影响子图回滚；生产、客户、审批、隐私、安全和不可逆动作触发人工接管。若无法证明当前基线，状态回退为 BLOCKED，不通过重新生成文档伪造连续性。

## 移交

输出 Product Delivery Ledger、六阶段 baseline manifests、50 Activity 索引、决策/条件/风险台账、运行与客户责任、生命周期触发器和审计入口。

