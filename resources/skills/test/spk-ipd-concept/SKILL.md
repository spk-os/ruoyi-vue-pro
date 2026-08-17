---
name: spk-ipd-concept
description: 概念阶段回退 skill（test 环境桩版，E2E fast-mode 用）。精简方法论骨架，保留 JSON Schema + 验收标准，去除外部 MCP 依赖。真实版见 default/spk-ipd-concept。
author: SPK-OS Team
version: 1.0.0
tags: [ipd, concept, stage-fallback, test-stub]
---

# spk-ipd-concept（test 桩版）

> E2E 测试用桩 skill。验证 skillPath 解析 + injectSkillInstruction 注入机制真实工作。
> fast-mode 下不调真实 LLM/外部 MCP，本 skill 仅保证注入路径正确。

## 概念阶段 8 Activity（方法论骨架）

01 机会信号聚合 → 02 客户需求分析 → 03 竞争格局 → 04 价值主张 →
05 财务评估 → 06 技术风险 CBB → 07 决策包 → 08 TR1 评审

## 产物骨架（OpportunitySignalSet 精简 schema）

```json
{"signal_set_id":"str","version":"1.0.0","statistics":{"total_signals":0},
 "signals":[{"signal_id":"str","source_uri":"str","locator":"str","captured_at":"ISO",
  "license":"str","content_hash":"sha256:","confidence":0.0,"needs_review":false}],
 "clusters":[{"cluster_id":"str","topic":"str","signal_count":0}],
 "signed_by":{"decision":"admitted"}}
```

## 验收标准（桩）

- 来源可追溯：每 signal 有 source_uri + locator + content_hash（非空）
- 许可合规：每 signal 有 license（非空）
- 置信度：confidence ∈ [0,1]，<0.6 → needs_review=true
- 聚类质量：每 cluster signal_count ≥ 3
- 签署：signed_by.decision=admitted
