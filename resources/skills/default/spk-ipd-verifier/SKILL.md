---
name: spk-ipd-verifier
description: >
  IPD Independent Verifier 三层校验 skill。先结构化 JSON Schema 校验产物必填字段/类型，
  再按 acceptanceCriteria 确定性规则检查验收标准，最后 LLM 语义复核四方法
  （recheck/redteam/completeness/traceback）。三层结果综合为 verdict。
author: SPK-OS Team
version: 1.0.0
tags: [ipd, verifier, three-layer-validation]
---

# spk-ipd-verifier — Independent Verifier 三层校验方法论

> Independent Verifier 物理隔离（独立 conversation/snapshot/evidence），对 Lead 产物执行三层校验：
> 结构 → 标准 → 语义。任一前置层 FAIL 即标记，LLM 语义复核作为补充证据而非唯一判据。
> 失败仍降级推进（不阻断流程），但 verdict 如实反映，写入 VerificationReceipt + evidence。

## 三层校验流程

### Layer 1: 结构化 JSON Schema 校验（确定性，先跑）

- 读取产物 content（artifact.metadata 全文）
- 按 artifactType 查找 schema：`resources/verifier-scripts/<artifactType>/schema.json`
- 校验：必填字段、字段类型、枚举值、数组非空
- 输出：`{schemaPass: bool, missingFields: [], typeErrors: []}`
- 结构不过 → 标记 STRUCTURE_FAIL，继续 Layer 2/3（不短路，收集全部证据）

### Layer 2: 验收标准规则检查（确定性，次跑）

- 读取 def.acceptanceCriteria JSON（如概念阶段 OpportunitySignalSet 的验收标准）
- 按规则脚本 `resources/verifier-scripts/<artifactType>/acceptance.json` 检查
- 典型规则（确定性，可代码检查）：
  - 来源可追溯：每 signal 有 source_uri + locator + captured_at + content_hash（非空）
  - 许可合规：每 signal 有 license（非空）
  - 置信度标注：每 signal 有 confidence ∈ [0,1]，<0.6 → needs_review=true
  - 聚类质量：每 cluster signal_count ≥ 3
  - 签署完整：signed_by.decision=admitted
- 输出：`{criteriaPass: bool, violations: [{rule, field, detail}]}`
- 标准不过 → 标记 CRITERIA_FAIL

### Layer 3: LLM 语义复核（概率性，最后跑）

- 把 Layer 1/2 结果 + 产物摘要喂入 LLM prompt
- 四方法（既有，降为补充证据）：
  - recheck：复算关键结论
  - redteam：找反例
  - completeness：查必填项缺失（与 Layer 1 互补，语义层面）
  - traceback：追溯证据链
- 输出：`{overall: PASS|CONDITIONAL|FAIL, summary, evidencePoints: [{point, verdict}]}`

## verdict 综合规则

| Layer1 结构 | Layer2 标准 | Layer3 语义 | 综合 verdict |
|---|---|---|---|
| PASS | PASS | PASS | PASS |
| PASS | PASS | CONDITIONAL/FAIL | CONDITIONAL（语义存疑） |
| PASS | FAIL | * | CONDITIONAL（标准不达标） |
| FAIL | * | * | FAIL（结构不合规，产物不可用） |
| LLM 调用失败 | * | ERROR | ERROR（降级推进，风险标记） |

- verdict 落 SpkVerificationReceiptDO.overallConclusion
- 三层明细落 evidencePoints JSON（含 schemaPass/criteriaPass/violations/llmVerdict）
- 失败仍降级推进（铁律：不阻断流程），但 verdict 如实反映

## 校验脚本位置

```
resources/verifier-scripts/
├── opportunity-signal-set/      # ACT-01 产物
│   ├── schema.json              # Layer 1 结构 schema
│   └── acceptance.json          # Layer 2 验收规则
├── customer-need-brief/         # ACT-02 产物
├── competitive-landscape/       # ACT-03 产物
└── ...
```

## 与现有 SpkVerifierService.verify 的关系

- 现有 verify 只跑 Layer 3（LLM 自由发挥），无 Layer 1/2
- 改造：verify 内先跑 Layer 1（结构）+ Layer 2（标准），再跑 Layer 3（LLM，喂入前两层结果）
- buildVerifyPrompt 改为：把 Layer 1/2 结果摘要 + 产物喂 LLM，要其语义复核并综合
- parseVerdict 解析综合 verdict + 三层明细
