---
name: spk-ipd-03-develop
description: 编排商业软件 IPD 03 开发阶段的完整交付，按依赖调度 11 个 Activity Skill、控制人工签署与独立验证并形成阶段基线。当产品进入开发阶段、阶段恢复或阶段出口审计时使用。
---

# 03 开发阶段编排

本 Skill 调度本阶段全部 Activity，不直接替代 Activity Skill 生成产物。

## 读取顺序

1. 读取 `references/stage-playbook.md`，确认阶段图、入口、并发和出口。
2. 读取 `../references/activity-catalog.json` 和 `../references/approval-rework-contract.md`，以 catalog 的 Skill、模板、人工责任和来源摘要为运行时真相。
3. 为当前节点调用 catalog 指定的编号 Activity Skill；该 Skill 会继续加载原始设计和执行规约。
4. 跨阶段执行时由 `spk-ipd-commercial-release` 统一维护 Product Delivery Ledger。

## 输入就绪

必须取得上一阶段签名基线、阶段门决策及条件项、当前项目/版本/流程实例、团队 RACI、环境与数据授权、预算和风险阈值。发现基线漂移、未关闭阻断条件或无授权签名时禁止启动。

## 执行工作流

1. 创建阶段运行清单，把每个 Activity 置为 `NOT_READY`，解析依赖后转为 `READY`。
2. 仅调度 READY 节点；无共享写入且输入已冻结的节点可并行，其他节点串行。
3. 每个节点完成后验证主产物、证据、人工记录和下游解析，再转为 `VERIFIED`。
4. Activity 失败时局部重试；输入或基线失效时沿依赖图撤销所有受影响节点，禁止只改汇总状态。
5. 在阶段出口生成 baseline manifest、证据索引、开放项、风险和决策包；运行包级验证器。
6. 由具名责任人/评委完成原设计要求的签署。Agent 无投票权、审批权或代签权。
7. 出口条件满足后锁定阶段基线，并把适用范围、失效条件与未关闭条件移交下一阶段。

## 完成定义

所有必需 Activity 均为 VERIFIED，catalog 中的人工责任和独立验证已真实履行，追溯无孤儿，阶段出口为“通过 TR3/TR4，签名制品与开发证据满足进入验证条件”，且下一阶段确认可解析，才允许阶段 COMPLETE。

## 失败处理

依赖失败时停止下游调度；写操作失败回滚到最近签名基线；门禁材料、法定人数、权限或签名不满足时保持 `AWAITING_HUMAN/BLOCKED`。REJECT 后由流程自动回到阶段入口、启动 Agent 返工并再次送审，审批人不接管执行。不得为了推进流程修改门禁结果。

## 移交

移交阶段 manifest、全部 Artifact 摘要与哈希、Claim/Evidence 索引、决策记录、条件项、残余风险、失效传播订阅和下一责任人。
