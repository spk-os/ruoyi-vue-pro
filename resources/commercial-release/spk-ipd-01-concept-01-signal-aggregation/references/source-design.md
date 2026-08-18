## ACT-03-02-01 机会信号聚合

### 基础信息

| 字段 | 内容 |
|---|---|
| **Activity ID** | ACT-03-02-01 |
| **Activity 名称** | 机会信号聚合 |
| **所属阶段** | 概念 |
| **Lead Agent 角色** | MarketInsight Lead |
| **执行位置** | task_system |
| **是否使用 Worker Agent** | ✓（3 个并行：MRS/VOC/竞品） |
| **是否需要 Independent Verifier** | — |
| **所需模型能力** | MARKET_RESEARCH + STRUCTURED_EXTRACTION |
| **所需 Skill** | ipd-concept-signal-aggregation / spk-info-ingest / spk-web-scrape |
| **所需 Tool/MCP** | mcp-spk-os-info-ingest / mcp-hermes-web-search / mcp-filesystem-read |
| **所需 Knowledge** | KN-IPD-MRS-001 / KN-IPD-VOC-002 |
| **所需 Information** | INFO-MARKET-2026-08 / INFO-COMPETITIVE-2026-08 / INFO-VOC-2026-08 |
| **可读取的 Memory 范围** | project:spk-os-ipd, agent_role:market_insight, capability:market_research |
| **人工责任** | Market Owner 签署 admitted |
| **失败处理** | 同一快照重试一次；解析仍失败则人工标注；批量漂移升级 Data Steward；回滚到前一签名 SignalSet 并撤销派生簇 |
| **对应 Flowable 节点** | task_signal_aggregation (ipd-concept.bpmn) |
| **证据来源** | RunReceipt + ToolReceipt + ArtifactManifest |

### 资源与工具详情

#### 数据源清单

| 数据源 | 类型 | 获取方式 | 频率 | 质量保障 |
|---|---|---|---|---|
| MRS 内部市场报告 | 内部系统 | mcp-spk-os-info-ingest 直连 | 每周同步 | 授权访问，hash 校验 |
| VOC 客户之声平台 | 内部系统 | mcp-spk-os-voc-query API | 每日同步 | 客户授权，脱敏处理 |
| 行业分析报告（Gartner/IDC/艾瑞） | 付费订阅 | mcp-spk-os-info-ingest + 人工导入 | 每月更新 | 版权许可，来源标注 |
| 竞品官网/博客/新闻稿 | 公开网页 | mcp-playwright-browser 抓取 | 每周抓取 | 快照存档，robots.txt 遵守 |
| 招投标网站（中国政府采购网/千里马） | 公开数据 | mcp-hermes-web-search + mcp-playwright-browser | 每日监控 | 关键词过滤，去重 |
| 政策法规库（国务院/工信部） | 公开数据 | mcp-hermes-web-search | 每周监控 | 官方来源，版本控制 |
| 技术趋势（arXiv/GitHub Trending） | 公开数据 | mcp-hermes-web-search | 每周抓取 | 时间戳，热度排序 |

#### 工具调用顺序与效率策略

```
Step 1: 并行启动 3 个 Worker Agent
├─ Worker-1: MRS 信号采集
│  ├─ 调用 mcp-spk-os-info-ingest.query(source="mrs", since=last_run)
│  ├─ 调用 mcp-filesystem-read 读取本地 MRS 缓存
│  └─ 输出：原始 MRS 信号列表（带 hash）
│
├─ Worker-2: VOC 信号采集
│  ├─ 调用 mcp-spk-os-voc-query.list(sentiment=["negative", "suggestion"], limit=500)
│  ├─ 调用 mcp-spk-os-voc-query.get_detail(id) 获取完整内容
│  └─ 输出：原始 VOC 信号列表（带客户 ID 脱敏）
│
└─ Worker-3: 竞品/行业信号采集
   ├─ 调用 mcp-hermes-web-search.query("竞品名 + 新产品/融资/合作") 获取候选 URL
   ├─ 调用 mcp-playwright-browser.navigate(url) 抓取页面快照
   ├─ 调用 mcp-playwright-browser.screenshot() 保存视觉证据
   └─ 输出：原始外部信号列表（带 URL + 快照路径）

Step 2: Lead Agent 聚合
├─ 调用 ipd-concept-signal-aggregation.entity_extract(signals) 统一实体
├─ 调用 spk-info-ingest.dedup(signals, strategy="source_fingerprint") 去重
├─ 调用 spk-info-ingest.cluster(signals, method="topic_modeling") 主题聚类
└─ 输出：聚合信号簇

Step 3: 质量标注
├─ 每条信号标注：source_uri + locator + captured_at + license + content_hash + cluster_id + confidence
├─ 低置信度信号（<0.6）标记为 needs_review
└─ 输出：OpportunitySignalSet（草稿）

Step 4: Market Owner 人工审核
├─ Owner 在 yudao 审批工作台查看 SignalSet
├─ Owner 可：admit / reject / request_revision
└─ 输出：OpportunitySignalSet（已签署）
```

#### 效率优化策略

| 策略 | 说明 | 预期效果 |
|---|---|---|
| 增量采集 | 只采集上次运行后的新信号 | 减少 80% 重复处理 |
| 并行 Worker | 3 个 Worker 并行，总耗时 = max(单个 Worker) | 比串行快 3 倍 |
| 本地缓存 | MRS/VOC 数据本地缓存 24 小时 | 减少 API 调用 50% |
| 智能过滤 | 关键词 + 语义过滤低质量信号 | 减少人工审核 60% |

### 输出 Artifact 模板

#### Artifact 名称
`OpportunitySignalSet`

#### 存储位置
`/work/SPK-OS/artifacts/opportunity_signal_set/{artifact_id}/{version}/`

#### 文件结构
```
opportunity_signal_set/
├── manifest.yaml                 # ArtifactManifest（见 07）
├── signal_set.json               # 主数据文件
├── signals/                      # 原始信号快照
│   ├── mrs/
│   │   ├── signal-001.json
│   │   └── signal-001.html       # 原始网页快照
│   ├── voc/
│   │   └── signal-002.json
│   └── external/
│       ├── signal-003.json
│       └── signal-003.png        # 截图证据
├── clusters/                     # 聚类结果
│   └── cluster-001.json
└── README.md                     # 人类可读摘要
```

#### signal_set.json Schema

```json
{
  "signal_set_id": "oss-20260802-001",
  "version": "1.0.0",
  "created_at": "2026-08-02T10:00:00Z",
  "created_by": "agent-market-insight-001",
  "signed_by": {
    "user_id": "user-market-owner-001",
    "signed_at": "2026-08-02T14:00:00Z",
    "decision": "admitted"
  },
  "statistics": {
    "total_signals": 156,
    "mrs_count": 45,
    "voc_count": 67,
    "external_count": 44,
    "clusters_count": 12,
    "high_confidence_count": 89,
    "needs_review_count": 23
  },
  "signals": [
    {
      "signal_id": "sig-001",
      "source_type": "mrs",
      "source_uri": "mrs://report/2026-07-30/cloud-market",
      "locator": "page 5, paragraph 3",
      "captured_at": "2026-07-30T08:00:00Z",
      "license": "internal_use",
      "content_hash": "sha256:abc123...",
      "raw_content_ref": "signals/mrs/signal-001.json",
      "extracted_entities": {
        "companies": ["华为云", "阿里云"],
        "technologies": ["AI 大模型", "推理优化"],
        "markets": ["企业级云计算"]
      },
      "cluster_id": "cluster-001",
      "cluster_topic": "云计算 AI 化趋势",
      "confidence": 0.85,
      "needs_review": false,
      "summary": "华为云和阿里云都在加大 AI 大模型推理优化投入"
    }
  ],
  "clusters": [
    {
      "cluster_id": "cluster-001",
      "topic": "云计算 AI 化趋势",
      "signal_count": 23,
      "representative_signals": ["sig-001", "sig-015", "sig-042"],
      "keywords": ["AI 大模型", "推理优化", "云计算"],
      "trend": "rising",
      "market_size_estimate": "large",
      "urgency": "high"
    }
  ]
}
```

#### README.md 模板

```markdown
# 机会信号集 OSS-20260802-001

## 概览
- **采集时间**：2026-07-30 ~ 2026-08-02
- **信号总数**：156 条
- **聚类数**：12 个
- **高置信度**：89 条（57%）

## TOP 5 机会簇

### 1. 云计算 AI 化趋势（23 条信号）
**关键词**：AI 大模型、推理优化、云计算
**趋势**：上升
**紧急度**：高
**代表信号**：
- 华为云发布盘古大模型 5.0，推理成本降低 50%（MRS-2026-07-30）
- 阿里云推出通义千问企业版，主打推理优化（竞品官网-2026-07-31）
- 客户访谈：70% 受访企业希望云服务集成 AI 能力（VOC-2026-08-01）

### 2. ...（略）

## 数据来源分布
- MRS：45 条（29%）
- VOC：67 条（43%）
- 外部：44 条（28%）

## 质量指标
- 来源可追溯率：100%
- 高置信度率：57%
- 需人工复核：23 条（15%）

## 签署
- **Market Owner**：张三
- **签署时间**：2026-08-02 14:00
- **决定**：admitted
```

#### 验收标准

| 检查项 | 标准 | 验证方式 |
|---|---|---|
| 来源可追溯 | 每条信号有 URI + locator + captured_at + content_hash | Verifier 抽查 10% |
| 许可合规 | 所有信号有 license 字段，外部信号遵守 robots.txt | 自动检查 |
| 聚类质量 | 每个 cluster 至少 3 条信号，主题明确 | 人工审核 |
| 置信度标注 | 所有信号有 confidence 字段，低置信度标记 needs_review | 自动检查 |
| 签署完整 | Market Owner 已签署 admitted | 系统检查 |

### 节点关联与展示

#### 在整体流程中的位置

```
[外部数据源] → ACT-03-02-01 机会信号聚合 → ACT-03-02-02 客户需求深度分析
                    ↓
            OpportunitySignalSet
                    ↓
            ACT-03-02-04 概念与价值主张生成
```

#### 上游依赖
- 无（这是概念阶段的第一个 Activity）

#### 下游消费
| 下游 Activity | 消费内容 | 消费方式 |
|---|---|---|
| ACT-03-02-02 客户需求深度分析 | OpportunitySignalSet 中的 VOC 信号簇 | 作为输入，深度分析客户痛点 |
| ACT-03-02-04 概念与价值主张生成 | OpportunitySignalSet 中的高置信度机会簇 | 作为输入，生成产品概念 |

#### 可视化展示

**在 SPK-OS Dashboard 的展示方式**：

1. **流程视图**：在 IPD 概念阶段流程图中，ACT-03-02-01 显示为第一个节点，状态为"已完成"（绿色）。
2. **产物视图**：点击节点，展开 OpportunitySignalSet 的 README.md，可下载完整 signal_set.json。
3. **证据链视图**：点击任意信号，显示完整的证据链（source_uri → 原始快照 → 提取实体 → 聚类结果）。
4. **统计视图**：显示信号来源分布饼图、聚类主题词云、置信度分布直方图。

**在 yudao 审批工作台的展示方式**：

1. Market Owner 看到待审批任务："ACT-03-02-01 机会信号聚合 - 等待签署"。
2. 点击进入，看到 README.md 摘要 + TOP 5 机会簇 + 质量指标。
3. 可下载完整 signal_set.json 和原始快照。
4. 可查看每个信号的详细证据链。
5. 操作按钮：[Admit] [Reject] [Request Revision]。

---
