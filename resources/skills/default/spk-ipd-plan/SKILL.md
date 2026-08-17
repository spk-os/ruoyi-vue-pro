---
name: spk-ipd-plan
description: >
  IPD 计划阶段 stage 级回退 skill。从已选概念出发，完成 PRS 基线/架构描述/集成项目计划/
  CBB 复用决策/测试策略 5 大活动，经 PDCP 门进入开发。当 activity_def.skills 为空时按 stage=plan 回退。
author: SPK-OS Team
version: 1.0.0
tags: [ipd, plan, stage-fallback]
---

# spk-ipd-plan — IPD 计划阶段执行方法论

> 计划阶段目标：把概念阶段产出的产品概念转成可执行工程计划——需求规格化、架构描述、
> 项目计划、CBB 复用、测试策略，经 PDCP（Plan Decision Checkpoint）门进入开发阶段。
> 本 skill 是 stage 级回退，承载 5 大活动通用方法论；具体 Activity 细粒度 skill 见
> ipd-plan-requirements-spec / ipd-plan-architecture-design / ipd-plan-project-planning /
> ipd-plan-cbb-decision / ipd-plan-test-strategy。

## 关键活动（对齐设计文档 04-02 计划阶段 5 Activity）

1. **产品需求规格化**（ACT-03-03-01）：把概念阶段 IR/FR 拆成原子需求（PRSBaselineCandidate），
   每需求带可验证验收标准 + 优先级；spk-requirements-atomizer 拆解 + spk-fuzzy-word-scanner 查歧义词。
2. **架构描述生成**（ACT-03-03-02）：C4 模型（Context/Container/Component）+ ADR 关键决策，
   输出 ArchitectureDescriptionCandidate；spk-architecture-critic 反方审查。
3. **项目计划编制**（ACT-03-03-03）：WBS + 里程碑（CDCP/PDCP/ADCP/GA/LDCP）+ 关键路径 + 风险登记，
   输出 IntegratedProjectPlan；spk-wbs-generator + spk-risk-narrative。
4. **CBB 复用决策**（ACT-03-03-04）：识别可复用 CBB（TRL≥6 才纳入），合约匹配 + 评分，
   输出 CBBReuseDecision；spk-cbb-contract-matcher + spk-cbb-scorer。
5. **测试策略规划**（ACT-03-03-05）：RVM/RTM 雏形 + 多层测试策略 + 资源矩阵，
   输出 VerificationStrategy；spk-rtm-builder + spk-test-case-generator。

## 执行方法论

- **输入消费**：ConceptOptionSet（选定方案）+ InitialBusinessCase + TechnicalRiskAndCBBMap。
- **并行策略**：架构描述与项目计划可并行（依赖概念输出，互不阻塞）；CBB 决策依赖架构。
- **质量门**：每 Activity 产出经 TR2/PDCP 门评审，Verifier 反方审查（架构 critic / 计划风险）。
- **可追溯**：每需求→架构元素→WBS 工作包→测试用例，RTM 双向链。

## 输出产物

| Activity | Artifact | 关键字段 |
|---|---|---|
| 需求规格化 | PRSBaselineCandidate | 需求原子 + 验收标准 + 优先级 |
| 架构描述 | ArchitectureDescriptionCandidate | C4 四层 + ADR + 接口契约 |
| 项目计划 | IntegratedProjectPlan | WBS + 里程碑 + 关键路径 + 风险 |
| CBB 决策 | CBBReuseDecision | CBB 清单 + 复用评分 + 集成点 |
| 测试策略 | VerificationStrategy | RVM/RTM + 多层用例 + 资源 |

## 验收要点

- PRS：每需求有可验证验收标准（非"系统应快"歧义）。
- 架构：C4 四层齐全 + 关键决策有 ADR（含替代方案 + 取舍理由）。
- 计划：关键路径识别 + 每里程碑有准入/准出标准。
- CBB：复用项 TRL≥6，自制项说明不复用理由。
