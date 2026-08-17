---
name: spk-ipd-tr-gate
description: >
  IPD 生命周期阶段 stage 级回退 skill。Lifecycle Signal Ingestion→Feedback Clustering→
  Vulnerability-SBOM Correlation→Maintenance Train→Quarterly Value Review 5 活动。
  持续运营价值闭环。activity_def.skills 空时按 stage=lifecycle 回退。
author: SPK-OS Team
version: 1.0.0
tags: [ipd, lifecycle, tr-gate, stage-fallback]
---

# spk-ipd-tr-gate — IPD 生命周期阶段执行方法论

> 生命周期阶段目标：上市后持续运营价值闭环——采集生命周期信号（运营/反馈/漏洞）、聚类分析、
> 漏洞与 SBOM 关联、维护窗口管理、季度价值评审，触发下一版本 IPD 迭代。
> 本 skill 是 stage 级回退；细粒度 skill 见 ipd-lifecycle-signal / ipd-lifecycle-feedback /
> ipd-lifecycle-vulnerability / ipd-lifecycle-maintenance / ipd-lifecycle-value-review。

## 关键活动（对齐设计文档 04-06 生命周期阶段 5 Activity）

1. **Lifecycle Signal Ingestion**（ACT-03-07-01）：采集运营信号（活跃/故障/NPS/性能），
   输出 LifecycleSignalSet；spk-signal-ingestion + spk-signal-router。
2. **Feedback Clustering & Analysis**（ACT-03-07-02）：用户反馈聚类 + 主题分析 + 忠实度校验，
   输出 FeedbackAnalysis；spk-feedback-analyst + spk-faithfulness-checker。
3. **Vulnerability-SBOM Correlation**（ACT-03-07-03）：漏洞情报与 SBOM 关联 + 影响范围 + 修复优先级，
   输出 VulnerabilityImpactAssessment；spk-vuln-analyst + spk-sbom-correlator。
4. **Maintenance Train Management**（ACT-03-07-04）：维护窗口编排 + 回归测试 + 灰度补丁，
   输出 MaintenanceTrainRecord；spk-train-planner + spk-regression-tester。
5. **Quarterly Value Review**（ACT-03-07-05）：季度价值评审（ROI/留存/收入 vs 计划），
   输出 QuarterlyValueReview + 触发下一版本 IR；spk-value-analyst。

## 执行方法论

- **信号闭环**：运营/反馈/漏洞三类信号持续采集，非一次性。
- **忠实度校验**：反馈分析须 spk-faithfulness-checker 防 LLM 臆造用户原话。
- **SBOM 关联**：每漏洞定位到 SBOM 组件 + 影响范围 + 修复优先级（CVSS + 业务影响）。
- **维护窗口**：非破坏性——维护补丁经回归测试 + 灰度发布，等同小发布流程。
- **季度评审**：实际 vs 计划偏差 → 改进项 → 下一版本 IR 反哺概念阶段（闭环回 ACT-03-02-01）。

## 输出产物

| Activity | Artifact | 关键字段 |
|---|---|---|
| 信号采集 | LifecycleSignalSet | 活跃/故障/NPS/性能 + 时间戳 |
| 反馈聚类 | FeedbackAnalysis | 主题簇 + 忠实度校验 + 原话 |
| 漏洞关联 | VulnerabilityImpactAssessment | 漏洞↔SBOM + CVSS + 优先级 |
| 维护窗口 | MaintenanceTrainRecord | 窗口 + 回归 + 灰度补丁 |
| 季度评审 | QuarterlyValueReview | ROI/留存/收入 vs 计划 + 改进IR |

## 验收要点

- 信号：每类信号有来源 + 时间戳（非伪造）。
- 反馈：忠实度校验通过（引用真实用户原话 + sample_limitation）。
- 漏洞：每漏洞有 SBOM 组件归属 + 修复优先级。
- 维护：补丁经回归测试 + 灰度发布（非直推生产）。
- 评审：实际 vs 计划偏差量化 + 改进 IR 反哺概念阶段。
