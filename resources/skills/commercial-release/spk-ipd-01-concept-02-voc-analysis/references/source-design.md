## ACT-03-02-02 客户需求深度分析

### 基础信息

| 字段 | 内容 |
|---|---|
| **Activity ID** | ACT-03-02-02 |
| **Activity 名称** | 客户需求深度分析 |
| **所属阶段** | 概念 |
| **Lead Agent 角色** | MarketInsight Lead |
| **执行位置** | task_system |
| **是否使用 Worker Agent** | ✓（3 个并行：访谈/工单/成功案例） |
| **是否需要 Independent Verifier** | — |
| **所需模型能力** | MARKET_RESEARCH + GROUNDED_EXTRACTION |
| **所需 Skill** | ipd-concept-voc-analysis / spk-crm-query / spk-text-clustering |
| **所需 Tool/MCP** | mcp-spk-os-voc-query / mcp-hermes-web-search / mcp-filesystem-read |
| **所需 Knowledge** | KN-IPD-VOC-002 |
| **所需 Information** | INFO-VOC-2026-08 |
| **可读取的 Memory 范围** | project:spk-os-ipd, agent_role:market_insight, capability:market_research |
| **人工责任** | Research Lead / Product Manager 签署 |
| **失败处理** | 客户原声缺失则标记 insufficient_voc；隐私字段未脱敏则阻断；样本偏差升级 Research Lead |
| **对应 Flowable 节点** | task_customer_need_analysis (ipd-concept.bpmn) |
| **证据来源** | RunReceipt + ToolReceipt + ArtifactManifest |

### 资源与工具详情

#### 数据源清单

| 数据源 | 类型 | 获取方式 | 频率 | 质量保障 |
|---|---|---|---|---|
| CRM 客户访谈记录 | 内部系统 | mcp-spk-os-voc-query 直连 | 实时 | 客户授权，脱敏 |
| 客户工单系统 | 内部系统 | mcp-spk-os-voc-query 直连 | 实时 | 分类标签，优先级 |
| 客户成功案例 | 内部文档 | mcp-filesystem-read | 每月更新 | 客户授权，匿名化 |
| 客户流失分析 | 内部报告 | mcp-filesystem-read | 每季度 | 原因分类，数据验证 |
| 销售一线反馈 | 内部系统 | mcp-spk-os-voc-query 直连 | 每周 | 销售代表标注 |
| 客户咨询记录 | 内部系统 | mcp-spk-os-voc-query 直连 | 实时 | 问题分类，满意度 |

#### 工具调用顺序与效率策略

```
Step 1: 并行启动 3 个 Worker Agent
├─ Worker-1: 访谈记录分析
│  ├─ 调用 mcp-spk-os-voc-query.list_interviews(since=last_quarter)
│  ├─ 调用 mcp-spk-os-voc-query.get_interview_detail(id)
│  └─ 输出：访谈原声列表（带客户 ID 脱敏）
│
├─ Worker-2: 工单分析
│  ├─ 调用 mcp-spk-os-voc-query.list_tickets(category=["feature_request", "bug"], priority=["high", "medium"])
│  ├─ 调用 mcp-spk-os-voc-query.get_ticket_detail(id)
│  └─ 输出：工单需求列表（带频率统计）
│
└─ Worker-3: 成功案例与流失分析
   ├─ 调用 mcp-filesystem-read 读取成功案例库
   ├─ 调用 mcp-filesystem-read 读取流失分析报告
   └─ 输出：成功因素 + 流失原因列表

Step 2: Lead Agent 聚合分析
├─ 调用 ipd-concept-voc-analysis.extract_needs(sources) 提取需求
├─ 调用 spk-text-clustering.cluster(needs, method="topic_modeling") 需求聚类
├─ 调用 ipd-concept-voc-analysis.find_counter_evidence(needs) 搜索反例
└─ 输出：需求簇 + 反例

Step 3: 质量标注
├─ 每条需求标注：customer_quote + customer_id_hash + interview_date + segment + counter_evidence + sample_limitation
├─ 隐私字段脱敏（客户名称 → hash，联系方式 → 删除）
└─ 输出：CustomerNeedBrief（草稿）

Step 4: Research Lead / Product Manager 人工审核
├─ 审核需求真实性（是否有原声支持）
├─ 审核样本代表性（是否覆盖主要客户群）
├─ 审核反例充分性（是否有足够反例）
└─ 输出：CustomerNeedBrief（已签署）
```

### 输出 Artifact 模板

#### Artifact 名称
`CustomerNeedBrief`

#### 文件结构
```
customer_need_brief/
├── manifest.yaml
├── need_brief.json
├── interviews/              # 原始访谈记录（脱敏）
│   ├── interview-001.json
│   └── interview-001.txt
├── tickets/                 # 原始工单（脱敏）
│   └── ticket-001.json
├── cases/                   # 成功案例
│   └── case-001.md
├── counter_evidence/        # 反例
│   └── counter-001.json
└── README.md
```

#### need_brief.json Schema

```json
{
  "brief_id": "cnb-20260802-001",
  "version": "1.0.0",
  "created_at": "2026-08-02T11:00:00Z",
  "created_by": "agent-market-insight-001",
  "signed_by": [
    {
      "user_id": "user-research-lead-001",
      "signed_at": "2026-08-02T15:00:00Z",
      "decision": "approved"
    },
    {
      "user_id": "user-pm-001",
      "signed_at": "2026-08-02T15:30:00Z",
      "decision": "approved"
    }
  ],
  "statistics": {
    "total_needs": 34,
    "high_priority_needs": 12,
    "customer_segments": 4,
    "interviews_analyzed": 28,
    "tickets_analyzed": 156,
    "counter_evidence_count": 8
  },
  "needs": [
    {
      "need_id": "need-001",
      "title": "希望云服务集成 AI 能力",
      "description": "客户希望在现有云服务基础上，直接调用 AI 大模型能力，无需自己搭建",
      "customer_quotes": [
        {
          "quote": "我们希望云平台能直接提供 AI 推理服务，而不是让我们自己部署模型",
          "customer_id_hash": "hash_customer_001",
          "interview_date": "2026-07-15",
          "interview_id": "interview-001",
          "locator": "00:15:30"
        }
      ],
      "customer_segments": ["enterprise", "mid_size"],
      "frequency": 18,
      "priority": "high",
      "counter_evidence": [
        {
          "evidence": "部分小型客户表示不需要 AI 能力，更关注成本",
          "source": "ticket-089",
          "impact": "low"
        }
      ],
      "sample_limitation": "样本主要来自中大型企业，小型企业样本不足",
      "related_signals": ["sig-001", "sig-015"],
      "confidence": 0.85
    }
  ],
  "segments": [
    {
      "segment_id": "enterprise",
      "name": "大型企业",
      "customer_count": 12,
      "top_needs": ["need-001", "need-005", "need-012"]
    }
  ]
}
```

#### README.md 模板

```markdown
# 客户需求简报 CNB-20260802-001

## 概览
- **分析时间**：2026-07-01 ~ 2026-08-02
- **需求总数**：34 条
- **高优先级**：12 条
- **客户细分**：4 个

## TOP 5 高优先级需求

### 1. 希望云服务集成 AI 能力（18 次提及）
**客户细分**：大型企业、中型企业
**客户原声**：
> "我们希望云平台能直接提供 AI 推理服务，而不是让我们自己部署模型" — 某大型制造企业 CTO，2026-07-15

**反例**：
> "部分小型客户表示不需要 AI 能力，更关注成本" — 工单 #089

**样本限制**：样本主要来自中大型企业，小型企业样本不足

### 2. ...（略）

## 客户细分洞察
- **大型企业**（12 家）：最关注 AI 能力、安全性、定制化
- **中型企业**（15 家）：最关注性价比、易用性、快速部署
- **小型企业**（8 家）：最关注成本、标准化、自助服务
- **初创企业**（5 家）：最关注灵活性、按需付费、技术支持

## 签署
- **Research Lead**：李四
- **Product Manager**：王五
- **签署时间**：2026-08-02 15:30
```

### 节点关联与展示

#### 在整体流程中的位置

```
ACT-03-02-01 机会信号聚合 → ACT-03-02-02 客户需求深度分析 → ACT-03-02-04 概念与价值主张生成
                                    ↓
                            CustomerNeedBrief
                                    ↓
                            ACT-03-02-05 初步财务评估
```

#### 上游依赖
| 上游 Activity | 依赖内容 | 依赖方式 |
|---|---|---|
| ACT-03-02-01 机会信号聚合 | OpportunitySignalSet 中的 VOC 信号簇 | 作为输入，深度分析客户痛点 |

#### 下游消费
| 下游 Activity | 消费内容 | 消费方式 |
|---|---|---|
| ACT-03-02-04 概念与价值主张生成 | CustomerNeedBrief 中的高优先级需求 | 作为输入，生成产品概念 |
| ACT-03-02-05 初步财务评估 | CustomerNeedBrief 中的需求频率和优先级 | 作为输入，估算市场规模 |

#### 可视化展示

**在 SPK-OS Dashboard 的展示方式**：
1. **需求热力图**：按客户细分和优先级展示需求分布
2. **客户原声墙**：展示代表性客户原声（脱敏）
3. **反例警示**：高亮显示有反例的需求
4. **样本覆盖度**：展示样本在各细分市场的覆盖度

---
