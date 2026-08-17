---
name: spk-ipd-verify
description: >
  IPD 验证阶段 stage 级回退 skill。冻结验证基线→建 RVM/RTM→设计多层测试→准备环境数据→执行采证 5 活动，
  经 TR5/GA 门进入发布。可复现是核心。activity_def.skills 空时按 stage=qualify 回退。
author: SPK-OS Team
version: 1.0.0
tags: [ipd, verify, stage-fallback]
---

# spk-ipd-verify — IPD 验证阶段执行方法论

> 验证阶段目标：证明产品符合需求规格——冻结验证基线、建立需求-验证矩阵（RVM/RTM）、
> 设计多层测试（单元/集成/系统/验收）、准备可复现环境与数据、执行并采证，经 TR5/GA 门进入发布。
> 本 skill 是 stage 级回退；细粒度 skill 见 ipd-verify-baseline-freeze / ipd-verify-rvm-rtm /
> ipd-verify-test-design / ipd-verify-env-prep / ipd-verify-execute。

## 关键活动（对齐设计文档 04-04 验证阶段 5 Activity）

1. **冻结验证基线**（ACT-03-05-01）：冻结被测对象（基线 manifest + 依赖锁），输出 VerificationBaselineManifest。
2. **建立 RVM/RTM**（ACT-03-05-02）：需求→测试用例双向追溯矩阵，输出 RVM_RTM_Baseline；
   spk-traceability-builder。
3. **设计多层测试**（ACT-03-05-03）：单元/集成/系统/验收四层用例 + 异常/边界/性能，
   输出 MultiLayerTestDesign；spk-test-designer + spk-evidence-critic。
4. **准备环境与数据**（ACT-03-05-04）：可复现环境（容器化 + 配置即代码）+ 测试数据工厂，
   输出 ReplayableTestEnvironment；spk-env-provisioner + spk-data-factory。
5. **执行与采证**（ACT-03-05-05）：跑用例 + 采证（日志/截图/录像）+ 失败洞察，
   输出 TestExecutionEvidence；spk-test-executor + spk-failure-insight。

## 执行方法论

- **可复现**：环境容器化 + 数据工厂 + 配置即代码，任意时刻可重建。
- **追溯闭环**：每需求至少一条测试覆盖（RTM 无遗漏）；每测试反查需求。
- **多层互补**：单元快反馈、集成链路、系统端到端、验收用户视角，不互相替代。
- **采证不可伪造**：证据含时间戳 + 环境 hash + 截图/日志，失败归因到根因。
- **GA 门**：通过率 + 覆盖率 + 缺陷分级收敛 + 性能基线达成才放行。

## 输出产物

| Activity | Artifact | 关键字段 |
|---|---|---|
| 基线冻结 | VerificationBaselineManifest | 被测 hash + 依赖锁 |
| RVM/RTM | RVM_RTM_Baseline | 需求↔用例双向链 |
| 测试设计 | MultiLayerTestDesign | 四层用例 + 异常/边界 |
| 环境数据 | ReplayableTestEnvironment | 容器 + 数据工厂 + 配置 |
| 执行采证 | TestExecutionEvidence | 通过率 + 证据 + 失败归因 |

## 验收要点

- RTM：每需求有覆盖（零遗漏）。
- 环境：可一键复现（非"在我机器上能跑"）。
- 证据：真实采证含时间戳/hash（非截图伪造）。
- GA 门：缺陷分级收敛（致命/严重归零）。
