---
name: spk-ipd-02-plan
description: 编排商业软件 IPD 02 计划阶段的完整交付，按依赖调度 6 个 Activity Skill、控制人工签署与独立验证并形成阶段基线。当产品进入计划阶段、阶段恢复或阶段出口审计时使用。
---

# 02 计划阶段编排

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

计划阶段的 `project-plan` 主产物必须逐字段遵循 Cortex 注入的 `schema.json`：需求不是摘要列表，而是含目标、范围、非目标、量化验收、影响、依赖、风险、结构化开发规格、结构化测试用例和完成定义的可执行合同；规格必须是含约束、验收与证据目标的对象；RTM 必须引用测试用例和证据；外部计划系统同步必须保留需求到 issue 的映射。写入唯一主产物文件后必须运行 prompt 中的 `validate_artifact.py --artifact-type project-plan --input <冻结路径>`，exitCode=0 才能返回；缺一项即由确定性核证判为 FAIL，禁止依赖模型自评放行。通过后只返回冻结文件 URI、SHA-256 和预检退出码小回执，禁止把完整 `project-plan` 正文再次塞入最终响应。

大计划文件必须采用传输安全的增量写法：Omnigent MCP 单次工具参数序列化后小于 `6000` UTF-8 字节；用多个小片段加短命令组装，或从通过当前验证器的上一版冻结计划复制后做最小结构化修订。遇到工具参数 JSON 解析/截断错误时必须进一步缩小调用，禁止以相近或更大的正文重复尝试。

## 完成定义

所有必需 Activity 均为 VERIFIED，catalog 中的人工责任和独立验证已真实履行，追溯无孤儿，阶段出口为“通过 TR2/PDCP，需求、架构、资源与验证承诺形成基线”，且下一阶段确认可解析，才允许阶段 COMPLETE。

## 失败处理

依赖失败时停止下游调度；写操作失败回滚到最近签名基线；门禁材料、法定人数、权限或签名不满足时保持 `AWAITING_HUMAN/BLOCKED`。REJECT 后由流程自动回到阶段入口、启动 Agent 返工并再次送审，审批人不接管执行。不得为了推进流程修改门禁结果。

## 移交

移交阶段 manifest、全部 Artifact 摘要与哈希、Claim/Evidence 索引、决策记录、条件项、残余风险、失效传播订阅和下一责任人。
