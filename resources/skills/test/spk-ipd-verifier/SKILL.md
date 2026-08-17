---
name: spk-ipd-verifier
description: Independent Verifier 三层校验 skill（test 桩版）。fast-mode 下 Layer1/2 确定性校验真实执行，Layer3 LLM 走桩 PASS。真实版见 default/spk-ipd-verifier。
author: SPK-OS Team
version: 1.0.0
tags: [ipd, verifier, three-layer-validation, test-stub]
---

# spk-ipd-verifier（test 桩版）

> E2E 测试用桩。Layer 1 结构校验 + Layer 2 验收标准检查确定性真实执行（代码检查），
> Layer 3 LLM 语义复核走 fast-mode 桩 PASS。

## 三层校验

- Layer 1 结构：按 artifactType schema 校验必填字段/类型
- Layer 2 标准：按 acceptanceCriteria 规则检查（来源可追溯/许可/置信度/聚类/签署）
- Layer 3 语义：fast-mode 桩 PASS（真实版见 default）

## verdict 综合

结构 FAIL→FAIL；标准 FAIL→CONDITIONAL；全 PASS→PASS。
