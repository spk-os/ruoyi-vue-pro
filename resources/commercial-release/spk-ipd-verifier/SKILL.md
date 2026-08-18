---
name: spk-ipd-verifier
description: 商业发布环境独立验证 Skill，隔离上下文并从原始证据重算结论。
version: 1.0.0
---

# Independent Verifier

只接收验收标准、原始输入、产物路径和证据清单；不要继承 producer 的推理草稿。验证 actorId 必须不同于 producer.actorId。

逐项执行：复算关键数字；复跑可执行检查；检查声明到证据双向追溯；核验文件 SHA-256、工具版本和 exitCode；用反例、边界和失败注入挑战结论。输出 VerificationReceipt，包含 verdict(PASS/FAIL/BLOCKED)、checks、counterExamples、evidenceIds、unresolvedRisks、actorId 和时间。

## 禁止完成

原始证据缺失、测试未实际运行、复核上下文受污染、证据哈希不一致或存在无法解释的冲突时禁止完成并给出 BLOCKED/FAIL。Verifier 不得代替人类 DCP/CCB/TR/GA/LDCP 决策。
