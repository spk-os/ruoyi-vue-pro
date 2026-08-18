## ACT-03-02-03 竞争格局分析

### 基础信息

| 字段 | 内容 |
|---|---|
| **Activity ID** | ACT-03-02-03 |
| **Activity 名称** | 竞争格局分析 |
| **所属阶段** | 概念 |
| **Lead Agent 角色** | Strategy Lead |
| **执行位置** | task_system |
| **是否使用 Worker Agent** | ✓（按竞品并行，N=竞品数） |
| **是否需要 Independent Verifier** | ✓（反方审查） |
| **所需模型能力** | MARKET_RESEARCH + CROSS_SOURCE_REASONING + COUNTERARGUMENT_GENERATION |
| **所需 Skill** | ipd-concept-competitive-analysis / spk-web-scrape / spk-financial-extract |
| **所需 Tool/MCP** | mcp-hermes-web-search / mcp-spk-os-info-ingest / mcp-filesystem-read / mcp-playwright-browser |
| **所需 Knowledge** | KN-IPD-COMPETITIVE-001 |
| **所需 Information** | INFO-COMPETITIVE-2026-08 |
| **可读取的 Memory 范围** | project:spk-os-ipd, agent_role:strategy, capability:market_research |
| **人工责任** | Strategy Owner 签署 |
| **失败处理** | 竞品证据不足则标记 insufficient_evidence；Verifier 挑战则重新验证；证据冲突升级 Strategy Owner |
| **对应 Flowable 节点** | task_competitive_analysis (ipd-concept.bpmn) |
| **证据来源** | RunReceipt + ToolReceipt + ArtifactManifest + VerificationReceipt |

### 资源与工具详情

#### 竞品识别与选择

**竞品从哪里来**：

| 来源 | 方法 | 工具 | 输出 |
|---|---|---|---|
| 行业报告 | 从 Gartner/IDC/艾瑞报告中提取主要玩家 | mcp-filesystem-read 读取已订阅报告 | 候选竞品列表 |
| 客户访谈 | 从 VOC 中提取客户提到的替代方案 | mcp-spk-os-voc-query 查询"竞品/替代"关键词 | 候选竞品列表 |
| 招投标数据 | 从招投标网站提取中标厂商 | mcp-hermes-web-search + mcp-playwright-browser | 候选竞品列表 |
| 行业专家 | 人工咨询行业专家 | 人工输入 | 候选竞品列表 |
| 市场份额数据 | 从公开财报/市场研究提取 | mcp-hermes-web-search | 候选竞品列表 |

**如何确定竞品**：

```
Step 1: 候选竞品收集
├─ Lead Agent 调用 mcp-hermes-web-search 搜索 "云计算 主要厂商 2026"
├─ Lead Agent 调用 mcp-spk-os-voc-query 查询客户提到的替代方案
├─ Lead Agent 调用 mcp-filesystem-read 读取行业报告
└─ 输出：候选竞品列表（去重后 15-20 个）

Step 2: 竞品筛选
├─ Lead Agent 按以下标准筛选：
│  ├─ 市场份额 > 1%（从公开数据估算）
│  ├─ 目标客户重叠度 > 30%（从 VOC 分析）
│  ├─ 产品功能重叠度 > 50%（从官网分析）
│  └─ 活跃度（最近 6 个月有新动态）
├─ Strategy Owner 人工确认最终竞品列表
└─ 输出：确定竞品列表（通常 5-8 个）

Step 3: 竞品分类
├─ 直接竞品：产品功能、目标客户、价格区间高度重叠
├─ 间接竞品：满足相同需求但方式不同
├─ 潜在竞品：可能进入市场的新玩家
└─ 输出：分类后的竞品列表
```

#### 数据源清单

| 数据源 | 类型 | 获取方式 | 频率 | 质量保障 |
|---|---|---|---|---|
| 竞品官网 | 公开网页 | mcp-playwright-browser 抓取 | 每周 | 快照存档，版本对比 |
| 竞品财报 | 公开数据 | mcp-hermes-web-search + mcp-playwright-browser | 每季度 | 官方来源，数据验证 |
| 竞品专利库 | 公开数据 | mcp-hermes-web-search（Google Patents/国家知识产权局） | 每月 | 专利号验证 |
| 竞品招聘信息 | 公开数据 | mcp-hermes-web-search（LinkedIn/拉勾/BOSS直聘） | 每周 | 职位真实性验证 |
| 竞品客户评价 | 公开数据 | mcp-hermes-web-search（G2/Capterra/知乎） | 每月 | 评价真实性验证 |
| 行业分析报告 | 付费订阅 | mcp-filesystem-read | 每季度 | 版权许可 |
| 竞品新闻稿 | 公开数据 | mcp-hermes-web-search | 每日 | 官方来源验证 |

#### 工具调用顺序与效率策略

```
Step 1: 竞品识别（Lead Agent）
├─ 调用 mcp-hermes-web-search.query("云计算 主要厂商 2026 市场份额")
├─ 调用 mcp-spk-os-voc-query.query("客户提到的替代方案")
├─ 调用 mcp-filesystem-read 读取 Gartner/IDC 报告
├─ 去重、筛选、分类
└─ 输出：确定竞品列表（5-8 个）

Step 2: 并行启动 N 个 Worker Agent（N=竞品数）
├─ Worker-1: 分析竞品 A
│  ├─ 调用 mcp-playwright-browser.navigate("https://competitor-a.com")
│  ├─ 调用 mcp-playwright-browser.screenshot() 保存首页快照
│  ├─ 调用 mcp-playwright-browser.navigate("https://competitor-a.com/products")
│  ├─ 调用 mcp-playwright-browser.screenshot() 保存产品页快照
│  ├─ 调用 mcp-hermes-web-search.query("competitor-a 财报 2026 Q2")
│  ├─ 调用 mcp-playwright-browser.navigate(财报 URL)
│  ├─ 调用 spk-financial-extract.extract_revenue(财报快照)
│  ├─ 调用 mcp-hermes-web-search.query("competitor-a 专利 site:patents.google.com")
│  ├─ 调用 mcp-hermes-web-search.query("competitor-a 招聘 site:linkedin.com")
│  └─ 输出：竞品 A 完整信息（带快照和来源）
│
├─ Worker-2: 分析竞品 B
│  └─ ...（同上）
│
└─ Worker-N: 分析竞品 N
   └─ ...（同上）

Step 3: Lead Agent 聚合分析
├─ 调用 ipd-concept-competitive-analysis.entity_extract(competitors) 统一竞品名称
├─ 调用 ipd-concept-competitive-analysis.build_matrix(competitors, dimensions=["目标客户", "价格", "功能", "渠道", "交付能力"])
├─ 调用 ipd-concept-competitive-analysis.find_gaps(matrix) 识别空白点
└─ 输出：竞争矩阵 + 空白点分析

Step 4: Worker Agent 验证证据
├─ 每个 Worker 验证自己负责竞品的关键证据
├─ 检查：来源是否可定位、数据是否最新、是否有第二来源
└─ 输出：验证报告

Step 5: Lead Agent 合并冲突
├─ 合并所有 Worker 的验证报告
├─ 处理冲突：标记矛盾点、请求 Strategy Owner 裁决
└─ 输出：CompetitiveLandscape（草稿）

Step 6: Verifier 反方审查
├─ Verifier 抽查每个重要判断是否有可定位来源
├─ Verifier 挑战：如果没有第二来源，要求补充或标记为单源限定
├─ Verifier 生成反方观点：如果竞品有优势但被忽略，要求补充
└─ 输出：VerificationReceipt

Step 7: Strategy Owner 签署
├─ Owner 审核 CompetitiveLandscape
├─ Owner 处理 Verifier 提出的挑战
├─ Owner 签署 admitted
└─ 输出：CompetitiveLandscape（已签署）
```

#### 效率优化策略

| 策略 | 说明 | 预期效果 |
|---|---|---|
| 并行 Worker | N 个 Worker 并行，总耗时 = max(单个 Worker) | 比串行快 N 倍 |
| 快照缓存 | 竞品官网快照缓存 7 天 | 减少重复抓取 70% |
| 增量更新 | 只更新有变化的竞品 | 减少 80% 重复处理 |
| 智能筛选 | 先用 web-search 筛选，再用 playwright 深度抓取 | 减少 playwright 调用 60% |

### 输出 Artifact 模板

#### Artifact 名称
`CompetitiveLandscape`

#### 文件结构
```
competitive_landscape/
├── manifest.yaml
├── landscape.json             # 主数据文件
├── competitors/               # 竞品详细信息
│   ├── competitor-a/
│   │   ├── profile.json       # 竞品档案
│   │   ├── snapshots/         # 网页快照
│   │   │   ├── homepage-20260802.png
│   │   │   ├── products-20260802.png
│   │   │   └── pricing-20260802.png
│   │   ├── financials/        # 财务数据
│   │   │   └── q2-2026.json
│   │   ├── patents/           # 专利列表
│   │   │   └── patents.json
│   │   └── evidence/          # 证据
│   │       ├── evidence-001.json
│   │       └── evidence-001.png
│   ├── competitor-b/
│   └── ...
├── matrix/                    # 竞争矩阵
│   ├── matrix.json
│   └── matrix.html            # 可视化矩阵
├── gaps/                      # 空白点分析
│   └── gaps.json
├── verification/              # 验证报告
│   ├── worker-1-report.json
│   ├── worker-2-report.json
│   └── verifier-report.json
└── README.md
```

#### landscape.json Schema

```json
{
  "landscape_id": "cl-20260802-001",
  "version": "1.0.0",
  "created_at": "2026-08-02T12:00:00Z",
  "created_by": "agent-strategy-001",
  "signed_by": {
    "user_id": "user-strategy-owner-001",
    "signed_at": "2026-08-02T16:00:00Z",
    "decision": "admitted"
  },
  "verification_receipt": {
    "verifier_id": "agent-verifier-001",
    "verified_at": "2026-08-02T15:00:00Z",
    "result": "pass",
    "challenges_resolved": 3
  },
  "as_of_date": "2026-08-02",
  "statistics": {
    "total_competitors": 6,
    "direct_competitors": 4,
    "indirect_competitors": 2,
    "evidence_count": 45,
    "two_source_count": 38,
    "single_source_count": 7
  },
  "competitors": [
    {
      "competitor_id": "comp-a",
      "name": "华为云",
      "category": "direct",
      "profile": {
        "founded": "2017",
        "headquarters": "深圳",
        "employees": "10000+",
        "revenue_2025": "未公开",
        "market_share_estimate": "18%"
      },
      "target_customers": ["大型企业", "政府", "金融"],
      "pricing": {
        "model": "按需付费 + 包年包月",
        "entry_level": "¥0.5/小时",
        "enterprise": "定制报价",
        "evidence": ["evidence-001"]
      },
      "key_features": [
        {
          "feature": "盘古大模型",
          "description": "自研 AI 大模型，支持中文优化",
          "evidence": ["evidence-002", "evidence-003"],
          "confidence": "high"
        }
      ],
      "channels": ["直销", "合作伙伴", "云市场"],
      "delivery_capability": {
        "regions": ["中国大陆", "亚太", "欧洲"],
        "sla": "99.95%",
        "support": "7x24 中文支持"
      },
      "strengths": [
        "自研 AI 芯片和模型",
        "政府和金融行业优势",
        "全栈云服务"
      ],
      "weaknesses": [
        "国际市场份额较低",
        "价格相对较高"
      ],
      "recent_moves": [
        {
          "date": "2026-07-15",
          "event": "发布盘古大模型 5.0",
          "impact": "high",
          "evidence": ["evidence-010"]
        }
      ]
    }
  ],
  "matrix": {
    "dimensions": ["目标客户", "价格", "功能", "渠道", "交付能力"],
    "comparison": [
      {
        "dimension": "价格",
        "competitors": {
          "comp-a": {"score": 3, "note": "中高端定价"},
          "comp-b": {"score": 4, "note": "性价比优先"}
        }
      }
    ]
  },
  "gaps": [
    {
      "gap_id": "gap-001",
      "description": "小型企业市场缺乏低成本 AI 云服务",
      "evidence": ["comp-a 无此产品", "comp-b 无此产品"],
      "opportunity": "high"
    }
  ],
  "evidence": [
    {
      "evidence_id": "evidence-001",
      "type": "pricing_page",
      "source": "https://competitor-a.com/pricing",
      "snapshot": "competitors/competitor-a/snapshots/pricing-20260802.png",
      "captured_at": "2026-08-02T10:00:00Z",
      "content_hash": "sha256:abc123...",
      "verified_by": "worker-1",
      "second_source": "evidence-005"
    }
  ]
}
```

#### README.md 模板

```markdown
# 竞争格局分析 CL-20260802-001

## 概览
- **分析时间**：2026-08-02
- **竞品总数**：6 个
- **直接竞品**：4 个
- **间接竞品**：2 个
- **证据总数**：45 条
- **双源验证**：38 条（84%）

## 竞品列表

### 直接竞品

#### 1. 华为云
- **市场份额**：18%（估算）
- **目标客户**：大型企业、政府、金融
- **核心优势**：自研 AI 芯片和模型、政府和金融行业优势、全栈云服务
- **核心劣势**：国际市场份额较低、价格相对较高
- **最近动态**：2026-07-15 发布盘古大模型 5.0

#### 2. 阿里云
- ...（略）

### 间接竞品

#### 5. AWS
- ...（略）

## 竞争矩阵

| 维度 | 华为云 | 阿里云 | 腾讯云 | 百度云 |
|---|---|---|---|---|
| 目标客户 | 大型企业 | 全类型 | 中小企业 | 大型企业 |
| 价格 | 3/5 | 4/5 | 4/5 | 3/5 |
| AI 功能 | 5/5 | 4/5 | 3/5 | 5/5 |
| 渠道 | 4/5 | 5/5 | 4/5 | 3/5 |
| 交付能力 | 4/5 | 5/5 | 4/5 | 3/5 |

## 市场空白点

### 1. 小型企业市场缺乏低成本 AI 云服务
- **证据**：华为云、阿里云均无针对小型企业的低成本 AI 云服务
- **机会**：高

## 验证报告

### Worker 验证
- Worker-1（华为云）：15 条证据，全部通过
- Worker-2（阿里云）：12 条证据，全部通过
- ...

### Verifier 反方审查
- **挑战 1**：华为云市场份额 18% 只有单一来源 → 已补充第二来源
- **挑战 2**：阿里云价格优势未考虑隐藏成本 → 已补充分析
- **挑战 3**：缺乏对 AWS 国际市场的分析 → 已补充

## 签署
- **Strategy Owner**：赵六
- **签署时间**：2026-08-02 16:00
- **决定**：admitted
```

#### 验收标准

| 检查项 | 标准 | 验证方式 |
|---|---|---|
| 竞品覆盖 | 至少 5 个竞品，包括直接和间接 | 自动检查 |
| 证据可追溯 | 每条证据有 source + snapshot + content_hash | Verifier 抽查 20% |
| 双源验证 | 关键判断至少 2 个来源 | Verifier 全量检查 |
| 时效性 | 所有数据不超过 3 个月 | 自动检查 |
| 签署完整 | Strategy Owner 已签署 admitted | 系统检查 |

### 节点关联与展示

#### 在整体流程中的位置

```
ACT-03-02-01 机会信号聚合 → ACT-03-02-03 竞争格局分析 → ACT-03-02-04 概念与价值主张生成
                                    ↓
                            CompetitiveLandscape
                                    ↓
                            ACT-03-02-06 技术风险与 CBB 识别
```

#### 上游依赖
| 上游 Activity | 依赖内容 | 依赖方式 |
|---|---|---|
| ACT-03-02-01 机会信号聚合 | OpportunitySignalSet 中的竞品信号 | 作为竞品识别的候选来源 |

#### 下游消费
| 下游 Activity | 消费内容 | 消费方式 |
|---|---|---|
| ACT-03-02-04 概念与价值主张生成 | CompetitiveLandscape 中的竞争矩阵和空白点 | 作为输入，生成差异化概念 |
| ACT-03-02-06 技术风险与 CBB 识别 | CompetitiveLandscape 中的竞品技术栈 | 作为输入，识别技术风险 |

#### 可视化展示

**在 SPK-OS Dashboard 的展示方式**：

1. **竞争矩阵热力图**：按维度（价格/功能/渠道/交付能力）展示竞品对比，颜色深浅表示强弱。
2. **竞品雷达图**：每个竞品一个雷达图，展示在 5 个维度上的得分。
3. **市场空白点地图**：在二维图上（如价格 vs 功能）展示竞品分布和空白点。
4. **证据链视图**：点击任意竞品，展示完整的证据链（官网快照 → 财报数据 → 专利列表 → 客户评价）。
5. **时间线视图**：展示竞品的最近动态（新产品发布、融资、合作等）。

**在 yudao 审批工作台的展示方式**：

1. Strategy Owner 看到待审批任务："ACT-03-02-03 竞争格局分析 - 等待签署"。
2. 点击进入，看到 README.md 摘要 + 竞争矩阵 + 市场空白点 + 验证报告。
3. 可下载完整 landscape.json 和所有快照。
4. 可查看每个竞品的详细证据链。
5. 可查看 Verifier 的反方审查报告和挑战处理结果。
6. 操作按钮：[Admit] [Reject] [Request Revision]。

---


---
