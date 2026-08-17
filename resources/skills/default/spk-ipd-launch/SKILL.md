---
name: spk-ipd-launch
description: >
  IPD 发布阶段 stage 级回退 skill。Release Manifest 构建→配置/数据迁移验证→运营就绪→渐进发布→TR6 证据包 5 活动，
  经 GA/LDCP 门进入生命周期。可回滚是底线。activity_def.skills 空时按 stage=launch 回退。
author: SPK-OS Team
version: 1.0.0
tags: [ipd, launch, stage-fallback]
---

# spk-ipd-launch — IPD 发布阶段执行方法论

> 发布阶段目标：把验证通过的产品安全送达用户——构建并验证 Release Manifest、配置与数据迁移验证、
> 运营就绪验证、渐进发布（灰度→全量）、TR6 证据包组装与评审，经 GA 门进入生命周期管理。
> 本 skill 是 stage 级回退；细粒度 skill 见 ipd-release-manifest / ipd-release-migration /
> ipd-release-readiness / ipd-release-rollout / ipd-release-tr6。

## 关键活动（对齐设计文档 04-05 发布阶段 5 Activity）

1. **Release Manifest 构建与验证**（ACT-03-06-01）：构建发布清单（制品+版本+依赖+签名），
   输出 ReleaseManifest；spk-manifest-builder + spk-policy-validator（合规策略校验）。
2. **配置与数据迁移验证**（ACT-03-06-02）：迁移脚本演练 + 回滚验证 + 数据一致性校对，
   输出 MigrationVerificationPack；spk-migration-analyst + spk-migration-validator。
3. **运营就绪验证**（ACT-03-06-03）：监控/告警/Runbook/值班/容量就绪，
   输出 OperationalReadinessRecord；spk-readiness-analyst + spk-observability-validator。
4. **渐进发布策略执行**（ACT-03-06-04）：灰度（canary）→小流量→全量，每阶段健康门 + 自动回滚，
   输出 ProgressiveRolloutRecord；spk-rollout-controller + spk-rollout-analyst。
5. **TR6 证据包组装与评审**（ACT-03-06-05）：汇总全周期证据 + 完整性检查 + 评审，
   输出 TR6DecisionPack；spk-evidence-assembler + spk-completeness-checker + spk-evidence-critic。

## 执行方法论

- **可回滚底线**：每发布阶段必须先验证回滚路径可行，再前推。
- **渐进灰度**：canary → 10% → 50% → 100%，每阶段健康门（错误率/延迟/业务指标）达标才进。
- **自动回滚**：健康门失败自动回滚，非人工介入。
- **迁移演练**：迁移脚本在生产镜像数据上演练（非直接上生产）。
- **运营就绪**：监控/告警/Runbook/值班四件套缺一不放行。
- **TR6 证据**：全周期证据可追溯，完整性检查无遗漏。

## 输出产物

| Activity | Artifact | 关键字段 |
|---|---|---|
| Release Manifest | ReleaseManifest | 制品+版本+依赖+签名+合规 |
| 迁移验证 | MigrationVerificationPack | 演练+回滚+一致性 |
| 运营就绪 | OperationalReadinessRecord | 监控/告警/Runbook/值班 |
| 渐进发布 | ProgressiveRolloutRecord | 阶段+健康门+回滚记录 |
| TR6 评审 | TR6DecisionPack | 全周期证据+完整性+评审 |

## 验收要点

- Manifest：制品签名可校验 + 依赖锁 + 合规策略通过。
- 迁移：演练成功 + 回滚路径验证 + 数据一致性。
- 就绪：监控/告警/Runbook/值班四件套齐全。
- 发布：每阶段健康门达标 + 回滚路径已验证。
- TR6：证据完整可追溯 + 评审通过。
