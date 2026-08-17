---
name: spk-ipd-develop
description: >
  IPD 开发阶段 stage 级回退 skill。基线确认→开发包切片→Context 构建→实现草案→单元验证 5 活动，
  经 ADCP 门进入验证。每包垂直切片可独立演示。activity_def.skills 空时按 stage=develop 回退。
author: SPK-OS Team
version: 1.0.0
tags: [ipd, develop, stage-fallback]
---

# spk-ipd-develop — IPD 开发阶段执行方法论

> 开发阶段目标：按计划阶段产出（PRS + 架构 + 项目计划 + CBB + 测试策略）实施工程，
> 以垂直切片增量交付，每切片可独立演示与验证，经 ADCP 门进入验证阶段。
> 本 skill 是 stage 级回退，承载 5 大活动方法论；细粒度 skill 见
> ipd-dev-baseline-confirm / ipd-dev-packet-slicing / ipd-dev-context-build /
> ipd-dev-implementation / ipd-dev-unit-test。

## 关键活动（对齐设计文档 04-03 开发阶段 5 Activity）

1. **基线确认**（ACT-03-04-01）：冻结 PRS/架构/计划/CBB/测试策略五件套为开发基线，
   输出 DevelopmentBaselineManifest（含 hash 链）。
2. **开发包切片**（ACT-03-04-02）：按价值/依赖把 backlog 切成可独立交付的垂直切片包，
   输出 DevelopmentPackageSet；spk-vertical-slicer。
3. **Context 构建**（ACT-03-04-03）：为每包装配 ContextBundle（相关 PRS/架构片段/CBB 合约/历史），
   spk-context-pack-builder。
4. **实现草案**（ACT-03-04-04）：按 ContextBundle 生成代码骨架 + 单测 + TODO，
   输出 ImplementationDraft；spk-code-generator + spk-test-generator。
5. **单元/组件验证**（ACT-03-04-05）：跑单测 + 组件测，失败归因，输出 UnitComponentEvidence；
   spk-test-runner + spk-failure-analyzer。

## 执行方法论

- **垂直切片**：每包端到端可演示（DB→API→UI），非水平分层切片。
- **Context 隔离**：每包 ContextBundle 独立，避免上下文污染。
- **可重复构建**：基线 manifest 含依赖锁 + hash，任意时刻可重建。
- **失败归因**：单测失败 → spk-failure-analyzer 定位根因（非仅重跑）。
- **质量门**：每包单测通过率 + 覆盖率达标才合并；ADCP 门汇总全量证据。

## 输出产物

| Activity | Artifact | 关键字段 |
|---|---|---|
| 基线确认 | DevelopmentBaselineManifest | 五件套 hash + 版本锁 |
| 开发包切片 | DevelopmentPackageSet | 切片清单 + 依赖序 + 演示场景 |
| Context 构建 | ContextBundle | PRS/架构/CBB 片段 + 历史 |
| 实现草案 | ImplementationDraft | 代码骨架 + 单测 + TODO |
| 单元验证 | UnitComponentEvidence | 通过率 + 覆盖率 + 失败归因 |

## 验收要点

- 基线：五件套齐全 + hash 链可校验。
- 切片：每包可独立演示端到端场景。
- 实现：代码可编译 + 单测可跑 + TODO 有责任人。
- 证据：通过率/覆盖率真实（非伪造）。
