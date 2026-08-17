---
name: ipd-concept-voc-analysis
description: >
  IPD 概念阶段 ACT-03-02-02 客户需求深度分析 skill。3 Worker 并行采集访谈/工单/案例，
  Lead 提取需求 + 反例搜索 + 主题聚类，输出 CustomerNeedBrief。每需求带 customer_quote/
  customer_id_hash/counter_evidence/sample_limitation。可追溯 + 反例校验防臆造。
author: SPK-OS Team
version: 1.0.0
tags: [ipd, concept, voc-analysis, ACT-03-02-02]
---

# ipd-concept-voc-analysis — 客户需求深度分析

> 概念阶段第二个 Activity。从客户访谈记录、工单系统、成功/失败案例三类来源并行采集
> 客户原声，Lead 提取结构化需求 + 反例搜索验证 + 主题聚类，输出 CustomerNeedBrief 供
> 下游竞争格局（ACT-03）、价值主张（ACT-04）消费。核心：每需求可追溯到真实客户原话，
> 反例搜索防臆造需求。

## 执行位置与角色

- Lead Agent：MarketInsight Lead（能力 VOC_ANALYSIS + STRUCTURED_EXTRACTION）
- Worker Agent：3 个并行（访谈采集 / 工单采集 / 案例采集）
- Independent Verifier：反例校验（counter_evidence 必须真实存在于源数据）
- 人工责任：Product Owner 签署 admitted
- 失败处理：重试一次 → 人工标注 → 反例缺失降级 needs_review → 回滚前一签名 NeedBrief

## 数据源

| 数据源 | 类型 | 获取方式 | 质量保障 |
|---|---|---|---|
| 客户访谈记录 | 内部系统 | mcp-spk-os-info-ingest + 人工导入 | 授权访问，客户 ID 脱敏 |
| 工单系统 | 内部系统 | mcp-spk-os-voc-query API | 客户授权，脱敏处理 |
| 成功/失败案例 | 内部知识库 | mcp-filesystem-read | 案例授权，来源标注 |

## 执行步骤

### Step 1: 并行启动 3 个 Worker Agent

- Worker-1（访谈采集）：读取访谈记录，提取客户原话片段（带 customer_id_hash + 场景）
- Worker-2（工单采集）：按 sentiment 取 negative/suggestion 拉工单，提取痛点原话
- Worker-3（案例采集）：读取成功/失败案例，提取隐性需求与反例

### Step 2: Lead 提取结构化需求

- 调用 ipd-concept-voc-analysis.entity_extract(quotes) 抽取需求条目
- 每需求标注：customer_quote（原话）/ customer_id_hash（脱敏）/ scenario / priority
- 反例搜索：spk-faithfulness-checker 验证 quote 真实存在于源数据

### Step 3: 主题聚类

- spk-text-clustering.cluster(needs, method=topic_modeling) 主题聚类
- 每簇标注：topic / need_count / trend / representative_quote

### Step 4: Product Owner 人工审核

- Owner 在审批工作台查看 NeedBrief
- admit / reject / request_revision

## 输出产物：CustomerNeedBrief

主数据文件 signal_set.json 结构（字段说明）：

- need_brief_id：需求集唯一编号（cnb-日期-序号）
- version / created_at / created_by：版本与溯源
- signed_by：签署（user_id / signed_at / decision=admitted|rejected|revision）
- statistics：total_needs / high_priority / counter_evidence_count / needs_review_count
- needs[]：每条需求
  - need_id / scenario / priority（P0/P1/P2）
  - customer_quote：客户原话（不得改写）
  - customer_id_hash：客户脱敏哈希
  - counter_evidence：反例证据（来源 + 摘要，证明需求非孤例）
  - confidence：0~1，<0.6 必须 needs_review=true
  - cluster_id / summary
- clusters[]：cluster_id / topic / need_count / trend / representative_quote

## 验收要点

- 来源可追溯：每需求 customer_quote + customer_id_hash 非空。
- 反例校验：counter_evidence 真实存在（非 LLM 臆造），faithfulness-checker 通过。
- 忠实度：customer_quote 不得改写原话（一字不差引用）。
- 置信度：confidence <0.6 标 needs_review。
- 样本局限：sample_limitation 标注样本量与盲区。
