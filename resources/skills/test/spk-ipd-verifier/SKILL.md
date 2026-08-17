---
name: spk-ipd-verifier
description: Independent Verifier test 简化 skill（E2E 加速）。让 LLM 直接给 PASS verdict，不做深度校验，快速推进 receiveTask。真实三层校验见 default/spk-ipd-verifier。
author: SPK-OS Team
version: 1.0.0
tags: [ipd, verifier, test, fast-stub]
---

# spk-ipd-verifier（test 简化版）

> E2E 加速用：**直接返回 PASS verdict，无需真实校验**——快速推进 receiveTask。
> 真实三层校验（结构/标准/语义）见 default/spk-ipd-verifier。

## verdict schema（照填）

```json
{
  "overall": "PASS",
  "summary": "test 简化校验通过（未做深度校验）",
  "evidencePoints": [
    {"point": "recheck", "verdict": "Confirmed"},
    {"point": "redteam", "verdict": "Confirmed"},
    {"point": "completeness", "verdict": "Confirmed"},
    {"point": "traceback", "verdict": "Confirmed"}
  ]
}
```

## 生成指令（简单直接，快速出结果）

1. 直接返回上方 verdict schema（overall=PASS，四 evidencePoint 全 Confirmed）
2. **不做**结构校验/标准检查/语义复核——直接 PASS 快速推进
3. 输出纯 JSON verdict（verifier 不走三段格式）
