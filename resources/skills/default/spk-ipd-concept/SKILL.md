---
name: spk-ipd-concept
description: >
  IPD 概念阶段阶段级 skill（回退绑定）。Charter/$APPEALS 8 维度/初步 ROI/CBB 机会/CDCP/TR1。
  当 activity_def.skills 为空时按 stage=concept 回退到本 skill。本 skill 承载概念阶段
  通用方法论；具体 Activity 细粒度 skill 见 ipd-concept-signal-aggregation 等子 skill。
author: SPK-OS Team
version: 1.0.0
tags: [ipd, concept, stage-fallback]
---

# spk-ipd-concept — IPD 概念阶段执行方法论

> 概念阶段目标：从市场机会与客户需求出发，形成可投资的产品概念，经 TR1/CDCP 门决策是否进入计划阶段。
> 本 skill 是概念阶段 stage 级回退 skill，承载 6 大关键活动方法论。具体 Activity（机会信号聚合/
> 客户需求分析/竞争格局/价值主张/财务评估/技术风险 CBB/决策包/TR1 评审）各有细粒度 skill，
> activity_def.skills 优先取细粒度 skill，空则回退本 skill。

## 关键活动（对齐设计文档 04-01 概念阶段 8 Activity）

1. **机会信号聚合**（ACT-03-02-01）：MRS/VOC/竞品 3 Worker 并行采集 → Lead 聚合去重聚类 →
   输出 OpportunitySignalSet（每信号带 source_uri/locator/license/content_hash/confidence）。
2. **客户需求深度分析**（ACT-03-02-02）：访谈/工单/案例 3 Worker 并行 → 提取需求 + 反例搜索 →
   输出 CustomerNeedBrief（每需求带 customer_quote/customer_id_hash/counter_evidence/sample_limitation）。
3. **竞争格局分析**（ACT-03-02-03）：竞品识别筛选分类 → N Worker 并行抓取 → 竞争矩阵 + 空白点 →
   Verifier 反方审查 → 输出 CompetitiveLandscape（每证据带 source/snapshot/content_hash/second_source）。
4. **概念与价值主张生成**（ACT-03-02-04）：综合 SignalSet/NeedBrief/Landscape →
   Jobs-to-be-done + 价值主张画布 → 输出 ConceptBrief。
5. **初步财务评估**（ACT-03-02-05）：市场规模估算 + ROI/敏感性分析 → 输出 FinancialAssessment。
6. **技术风险与 CBB 识别**（ACT-03-02-06）：技术栈风险 + CBB 复用机会（TRL≥6 才纳入）→
   输出 TechRiskAndCBB。
7. **TR1/CDCP 决策包组装**（ACT-03-02-07）：聚合前 6 产物 → 证据包组装 → 输出 DecisionPackage。
8. **TR1 评审支持**（ACT-03-02-08）：独立验证决策包完整性 → 输出 TR1ReviewReport。

## 铁律

- **DCP 必人审**：CDCP 投资决策不交给模型，由 IPMT Chair 签署。
- **TR 后置独立 verifier**：TR1 人审概念成立性，TR2-6 后置自动 gate；verifier 不归开发。
- **上下文隔离**：每 Activity 独立 conversation/snapshot/evidence，不串读。
- **来源可追溯**：每条数据带 source_uri + locator + captured_at + content_hash。
- **反例必搜**：需求/竞品判断必须有 counter_evidence，单源判断标记 single_source_limitation。

## 产物（概念阶段 8 Artifact）

| Activity | Artifact | 关键字段 |
|---|---|---|
| 01 信号聚合 | OpportunitySignalSet | signals[{source_uri,locator,license,content_hash,cluster_id,confidence}],clusters[] |
| 02 需求分析 | CustomerNeedBrief | needs[{customer_quotes,counter_evidence,sample_limitation,confidence}],segments[] |
| 03 竞争格局 | CompetitiveLandscape | competitors[],matrix,gaps[],evidence[{source,snapshot,second_source}] |
| 04 价值主张 | ConceptBrief | concept,value_proposition,differentiation,target_segment |
| 05 财务评估 | FinancialAssessment | market_size,roi,sensitivity_analysis |
| 06 技术风险 | TechRiskAndCBB | risks[],cbb_opportunities[],trl_assessment |
| 07 决策包 | DecisionPackage | evidence_chain,signoff_chain,hash_chain |
| 08 TR1 评审 | TR1ReviewReport | review_findings,gate_recommendation |

## 验收标准（概念阶段通用）

- 来源可追溯：每条数据有 URI + locator + content_hash
- 许可合规：外部数据有 license，遵守 robots.txt
- 反例充分：关键判断有 counter_evidence
- 置信度标注：所有判断有 confidence，低置信度标 needs_review
- 签署完整：对应 Owner 已签署 admitted

## 失败处理

- 同一快照重试一次；解析仍失败则人工标注
- 批量数据漂移升级 Data Steward
- 回滚到前一签名产物并撤销派生簇
