---
name: ipd-concept-signal-aggregation
description: >
  IPD 概念阶段 ACT-03-02-01 机会信号聚合 skill。3 Worker 并行采集 MRS/VOC/竞品信号，
  Lead 聚合去重聚类，输出 OpportunitySignalSet。每信号带 source_uri/locator/captured_at/
  license/content_hash/cluster_id/confidence。Market Owner 签署 admitted。
author: SPK-OS Team
version: 1.0.0
tags: [ipd, concept, signal-aggregation, ACT-03-02-01]
---

# ipd-concept-signal-aggregation — 机会信号聚合

> 概念阶段第一个 Activity。从 MRS 内部市场报告、VOC 客户之声、竞品/行业公开数据三类来源
> 并行采集机会信号，Lead 聚合去重聚类，质量标注后由 Market Owner 签署，输出
> OpportunitySignalSet 供下游需求分析（ACT-02）与价值主张（ACT-04）消费。

## 执行位置与角色

- Lead Agent：MarketInsight Lead（能力 MARKET_RESEARCH + STRUCTURED_EXTRACTION）
- Worker Agent：3 个并行（MRS 采集 / VOC 采集 / 竞品行业采集）
- Independent Verifier：本 Activity 不强制独立验证（由下游消费方校验来源可追溯）
- 人工责任：Market Owner 签署 admitted
- 失败处理：重试一次 → 人工标注 → 批量漂移升级 Data Steward → 回滚前一签名 SignalSet

## 数据源

| 数据源 | 类型 | 获取方式 | 质量保障 |
|---|---|---|---|
| MRS 内部市场报告 | 内部系统 | mcp-spk-os-info-ingest 直连 | 授权访问，hash 校验 |
| VOC 客户之声平台 | 内部系统 | mcp-spk-os-voc-query API | 客户授权，脱敏处理 |
| 行业分析报告 | 付费订阅 | mcp-spk-os-info-ingest + 人工导入 | 版权许可，来源标注 |
| 竞品官网/博客/新闻稿 | 公开网页 | mcp-playwright-browser 抓取 | 快照存档，robots.txt 遵守 |
| 招投标网站 | 公开数据 | mcp-hermes-web-search | 关键词过滤，去重 |
| 政策法规库 | 公开数据 | mcp-hermes-web-search | 官方来源，版本控制 |
| 技术趋势 | 公开数据 | mcp-hermes-web-search | 时间戳，热度排序 |

## 执行步骤

### Step 1: 并行启动 3 个 Worker Agent

- Worker-1（MRS 信号采集）：mcp-spk-os-info-ingest.query(source="mrs", since=last_run) +
  mcp-filesystem-read 读本地缓存 → 原始 MRS 信号列表（带 hash）
- Worker-2（VOC 信号采集）：mcp-spk-os-voc-query.list(sentiment=["negative","suggestion"], limit=500) +
  get_detail(id) → 原始 VOC 信号列表（带客户 ID 脱敏）
- Worker-3（竞品/行业信号采集）：mcp-hermes-web-search.query("竞品名 + 新产品/融资/合作") +
  mcp-playwright-browser.navigate+screenshot → 原始外部信号列表（带 URL + 快照路径）

### Step 2: Lead Agent 聚合

- ipd-concept-signal-aggregation.entity_extract(signals) 统一实体
- spk-info-ingest.dedup(signals, strategy="source_fingerprint") 去重
- spk-info-ingest.cluster(signals, method="topic_modeling") 主题聚类
- 输出：聚合信号簇

### Step 3: 质量标注

- 每条信号标注：source_uri + locator + captured_at + license + content_hash + cluster_id + confidence
- 低置信度信号（<0.6）标记为 needs_review
- 输出：OpportunitySignalSet（草稿）

### Step 4: Market Owner 人工审核

- Owner 在 yudao 审批工作台查看 SignalSet
- Owner 可：admit / reject / request_revision
- 输出：OpportunitySignalSet（已签署）

## 输出 Artifact：OpportunitySignalSet

### JSON Schema

```json
{
  "signal_set_id": "string",
  "version": "1.0.0",
  "created_at": "ISO8601",
  "created_by": "string",
  "signed_by": {"user_id":"string","signed_at":"ISO8601","decision":"admitted|rejected|revision"},
  "statistics": {
    "total_signals": "int", "mrs_count": "int", "voc_count": "int", "external_count": "int",
    "clusters_count": "int", "high_confidence_count": "int", "needs_review_count": "int"
  },
  "signals": [
    {
      "signal_id": "string",
      "source_type": "mrs|voc|external",
      "source_uri": "string (必填，来源可追溯)",
      "locator": "string (必填，定位信息如 page/paragraph/timestamp)",
      "captured_at": "ISO8601 (必填)",
      "license": "string (必填，许可合规)",
      "content_hash": "sha256:hex (必填)",
      "raw_content_ref": "string",
      "extracted_entities": {"companies":[],"technologies":[],"markets":[]},
      "cluster_id": "string",
      "cluster_topic": "string",
      "confidence": "float 0-1 (必填)",
      "needs_review": "bool",
      "summary": "string"
    }
  ],
  "clusters": [
    {"cluster_id":"string","topic":"string","signal_count":"int","representative_signals":[],"keywords":[],"trend":"rising|stable|falling","market_size_estimate":"small|medium|large","urgency":"low|medium|high"}
  ]
}
```

### 必填字段（结构校验）

- 顶层：signal_set_id, version, created_at, created_by, statistics, signals（非空数组）, clusters
- 每条 signal：signal_id, source_type, source_uri, locator, captured_at, license, content_hash, confidence
- 每个 cluster：cluster_id, topic, signal_count, trend

## 验收标准

| 检查项 | 标准 | 验证方式 |
|---|---|---|
| 来源可追溯 | 每条信号有 source_uri + locator + captured_at + content_hash | Verifier 抽查 10% |
| 许可合规 | 所有信号有 license 字段，外部信号遵守 robots.txt | 自动检查（license 非空） |
| 聚类质量 | 每个 cluster 至少 3 条信号，主题明确 | LLM 语义复核 |
| 置信度标注 | 所有信号有 confidence 字段，低置信度标记 needs_review | 自动检查（confidence∈[0,1]，<0.6→needs_review=true） |
| 签署完整 | Market Owner 已签署 admitted | 系统检查（signed_by.decision=admitted） |

## 下游消费

- ACT-03-02-02 客户需求深度分析：消费 VOC 信号簇深度分析客户痛点
- ACT-03-02-04 概念与价值主张生成：消费高置信度机会簇生成产品概念
- ACT-03-02-03 竞争格局分析：消费竞品信号作为竞品识别候选来源
