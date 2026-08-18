# ACT-03-02-04「概念与价值主张生成」执行规约

## 目标与非目标

目标是在受控基线上完成 `概念与价值主张生成`，产出可验证、可复算、可追溯的 `ConceptOptionSet`，使下游无需猜测即可继续。非目标是替代授权人审批、用模型记忆补齐事实、只写分析摘要，或在没有证据时宣称完成。

来源锚点：`/root/share/SPK-OS/24_houre_anly/IPD/05-kimi-k3-design/04-六阶段每阶段-具体设计与Activity执行配置表/04-01-概念阶段.md` 第 924-1092 行；复制件 SHA-256：`fc5dcebad6a957d9d3b47c78899134c6aa2a80c0e9a2120302b2b1014a34e38a`。

## 输入契约

从 `source-design.md` 的“输入资源”表逐项创建输入记录。每项至少包含 `sourceUri`、`version/revision`、`observedAt`、`retrievedAt`、`owner`、`authScope`、`sha256`、`freshnessVerdict`。上游 Artifact 必须匹配 catalog 类型和本次流程基线。

输入分为 `REQUIRED`、`CONDITIONAL`、`OPTIONAL`。REQUIRED 不可用即 BLOCKED；CONDITIONAL 条件满足却不可用同样 BLOCKED；OPTIONAL 缺失须说明对置信度和范围的影响。不得静默跳过资源。

## 工具与执行顺序

按 `source-design.md` 的“工具调用顺序与效率策略”调用真实连接器和确定性工具，再让模型归纳或生成。记录工具名、版本、参数摘要、输入哈希、起止时间、exitCode、输出路径和重试次数。只读采集可并行；依赖前序输出、写状态或共享限流的调用必须串行。

空集或部分结果不等于“没有问题”；检查权限、分页、时间窗、过滤条件和服务状态。模型输出必须通过 Schema、规则、计算或独立 Reviewer 验证。

## 领域执行算法

1. 冻结机会、客户、竞争事实及假设与约束。
2. 生成至少三个有实质差异的产品概念。
3. 描述目标客户、场景、价值、能力、差异化和明确不做事项。
4. 把价值主张拆成成功指标、最小实验和最迟验证时间。
5. 按客户价值、战略、可行性、财务、风险和速度加权评分。
6. 开展反事实和预演失败并记录取舍。
7. 形成保留少数意见和进入条件的 ConceptOptionSet。

## 输出契约

主交付物为 `ConceptOptionSet`，正式骨架位于 `../templates/concept/ACT-03-02-04-ConceptOptionSet.template.md`。同时产出 `input-manifest.json`、`artifact-manifest.json`、`evidence-manifest.json`、`run-receipt.json`、`activity-result.json`。具体字段、目录结构和示例以 `source-design.md` 的“输出 Artifact 模板”为准；通用字段以包级 Schema 为准，冲突时取更严格约束并登记。

数值写明单位、口径、期间和计算式；决策写明备选项与排除理由；UNKNOWN 保持显式；外链保留取得时间和内容摘要。

## 验收与完成判定

- 原设计 Artifact Schema 的必填字段、枚举、数量下限和关联关系全部通过。
- 领域算法 7 步均有执行证据；跳过步骤须有不适用依据和批准人。
- 关键 claim 证据覆盖率 100%，URI、文件、行或记录定位可访问且哈希一致。
- 至少一次反证检查和一次可重放验证；需独立验证时 producer 与 verifier 不得相同。
- 模板无未渲染变量、示例值、空章节或待补标记，下游字段可直接解析。
- `validate_delivery.py` 返回 0，人工门禁（若要求）具有真实签署记录。

## 失败、重试、回滚与升级

读取失败先检查授权、分页、时间窗和版本，再按 1/2/4 分钟退避，最多 3 次并保留错误响应。验证失败不得改写结论绕过；从最近有效检查点重跑。发生写入副作用时使用事务或版本回退；没有可验证回滚路径则禁止执行。

强制输入缺失/冲突、敏感数据越权、重大结论低置信度、预算将超限、需要人工批准或可能影响生产客户时立即升级。升级包包含现状、已尝试动作、证据、影响、选项和明确请求。

## 上下游移交

采用 `source-design.md`“节点关联与展示”的依赖与消费者。传递 Artifact ID、版本、SHA-256、Claim/Evidence 索引、适用范围、失效条件、开放项和 owner。下游确认可解析才算完成；来源更新、假设推翻或验证失败时创建失效事件并通知所有消费方。


