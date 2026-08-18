## ACT-03-07-05 Quarterly Value Review

### 基础信息

| 字段 | 内容 |
|---|---|
| **Activity ID** | ACT-03-07-05 |
| **Activity 名称** | Quarterly Value Review（季度价值评审） |
| **所属阶段** | 生命周期管理 |
| **Lead Agent 角色** | Lifecycle Orchestrator |
| **执行位置** | task_system |
| **是否使用 Worker Agent** | ✓（Value Analyst 多维评分/趋势分析 + Data Collector 自动采集） |
| **是否需要 Independent Verifier** | ✓（DCS 重算价值/服务/财务/风险指标 + LMT/IPMT 解释并决定行动） |
| **所需模型能力** | CROSS_SOURCE_REASONING + STRUCTURED_DRAFTING |
| **所需 Skill** | ipd-lifecycle-value-review / spk-value-analyst |
| **所需 Tool/MCP** | mcp-finance-query / mcp-monitoring-query / mcp-crm-query / mcp-spk-os-artifact-query |
| **所需 Knowledge** | KN-IPD-VALUE-SCHEMA-078 / KN-ISO-55001-VALUE-079 / KN-DORA-METRICS-080 |
| **所需 Information** | INFO-REVENUE-COST（收入/成本）/ INFO-SLO-TRENDS（SLO 趋势）/ INFO-CUSTOMER-METRICS（客户指标）/ INFO-STRATEGIC-ALIGNMENT（战略对齐） |
| **可读取的 Memory 范围** | project:spk-os-ipd, agent_role:lifecycle, capability:cross_source_reasoning |
| **人工责任** | LMT Lead 评议；IPMT 接收行动并决定投资方向 |
| **失败处理** | 数据延迟重取一次；仍不完整标 provisional；重大财务/服务冲突升级 IPMT；否决时回滚行动与 Scorecard 认证状态 |
| **对应 Flowable 节点** | task_quarterly_review (ipd-lifecycle.bpmn) |
| **证据来源** | RunReceipt + QuarterlyValueReview + ArtifactManifest |

### 资源与工具详情

#### 工具调用顺序与效率策略

```
Step 1: 冻结季度窗口
├─ 确定评审季度（如 Q3 2026）
├─ 冻结数据时间窗（2026-07-01 至 2026-09-30）
├─ 锁定产品版本范围
└─ 输出：季度窗口定义

Step 2: Data Collector 自动采集
├─ 从各系统拉取四维数据：
│  ├─ 财务维度：
│  │  ├─ MRR/ARR（月度/年度经常性收入）
│  │  ├─ 客户获取成本（CAC）
│  │  ├─ 客户生命周期价值（LTV）
│  │  ├─ 毛利率
│  │  └─ 支持成本（人力 + 基础设施）
│  ├─ 客户维度：
│  │  ├─ NPS（净推荐值）
│  │  ├─ 客户流失率
│  │  ├─ 客户增长率
│  │  ├─ 活跃用户数
│  │  └─ 客户满意度（CSAT）
│  ├─ 技术维度：
│  │  ├─ SLO 达标率（可用性、延迟、错误率）
│  │  ├─ 安全债务（未修复 CVE 数量和严重度）
│  │  ├─ 技术债务（代码质量指标趋势）
│  │  ├─ 依赖健康度（EOL 依赖数量）
│  │  └─ 发布频率和变更失败率（DORA 指标）
│  └─ 战略维度：
│      ├─ 路线图对齐度（当前版本与下一代路线图的匹配度）
│      ├─ 市场竞争力（市场份额趋势）
│      ├─ 合规状态（法规变化影响）
│      └─ CBB 贡献（本产品贡献的 CBB 被其他产品使用的次数）
└─ 输出：四维数据集

Step 3: DCS 重算指标
├─ 调用 DCS POST /api/v1/value-scorecard
│  ├─ 对每个维度计算：
│  │  ├─ 当前值
│  │  ├─ 趋势（↗ ↘ →）
│  │  ├─ 与上季度对比
│  │  ├─ 与目标对比
│  │  ├─ Wilson 95% 置信区间（如适用）
│  │  └─ 敏感性分析（关键假设变化 ±10% 的影响）
│  ├─ 计算综合评分（加权平均，权重由批准策略给出）
│  ├─ 标注数据缺失项
│  ├─ 输出：Scorecard + 签名
│  └─ 签名：sha256(...)
└─ 输出：DCS Scorecard

Step 4: Value Analyst 生成趋势分析
├─ 调用 spk-value-analyst.analyze(scorecard, historical_data)
│  ├─ 生成趋势分析：
│  │  ├─ 各维度的季度环比变化
│  │  ├─ 年度同比变化
│  │  ├─ 关键驱动因素识别
│  │  ├─ 反事实分析（如果不做 X 会怎样）
│  │  └─ 风险预警（哪些指标在恶化）
│  ├─ 生成行动建议：
│  │  ├─ invest_grow：价值高且增长，追加投资
│  │  ├─ maintain：价值稳定，维持现状
│  │  ├─ harvest_freeze：价值下降但仍有客户，冻结新功能
│  │  ├─ replace_migrate：有替代方案，启动迁移
│  │  └─ retire：价值低且无增长，启动退市评估
│  └─ 输出：趋势分析报告
└─ 输出：QuarterlyValueReview（草稿）

Step 5: LMT 评议 + IPMT 接收
├─ LMT Lead 审查：
│  ├─ Scorecard 数据准确性
│  ├─ 趋势分析合理性
│  ├─ 行动建议可行性
│  ├─ 标注分歧和未决项
│  └─ 签署评议意见
├─ IPMT 审查：
│  ├─ 接收季度评审报告
│  ├─ 决定投资方向（invest/maintain/harvest/replace/retire）
│  ├─ 分配行动项和 Owner
│  └─ 签署行动决定
├─ **AI 不决定追加投资或退市**
└─ 输出：QuarterlyValueReview（已签署）
```

### 输出 Artifact 模板

#### Artifact 名称
`QuarterlyValueReview`

#### QuarterlyValueReview Schema

```json
{
  "review_id": "qvr-2026-q3-001",
  "version": "1.0.0",
  "quarter": "Q3-2026",
  "product_id": "aurora",
  "created_at": "2026-10-05T10:00:00Z",
  "signed_by": [
    {"user_id": "user-lmt-lead-001", "role": "LMT Lead", "signed_at": "2026-10-05T14:00:00Z"},
    {"user_id": "user-ipmt-chair-001", "role": "IPMT Chair", "signed_at": "2026-10-06T10:00:00Z"}
  ],
  "scorecard": {
    "financial": {
      "weight_pct": 30,
      "score": 82,
      "trend": "up",
      "metrics": {
        "mrr_usd": {"value": 125000, "previous": 112000, "change_pct": 11.6},
        "arr_usd": {"value": 1500000, "previous": 1344000, "change_pct": 11.6},
        "gross_margin_pct": {"value": 72, "previous": 70, "change_pct": 2.9},
        "support_cost_usd": {"value": 35000, "previous": 32000, "change_pct": 9.4}
      }
    },
    "customer": {
      "weight_pct": 25,
      "score": 75,
      "trend": "up",
      "metrics": {
        "nps": {"value": 52, "previous": 48, "change": 4},
        "churn_rate_pct": {"value": 2.1, "previous": 2.5, "change": -0.4},
        "active_users": {"value": 1250, "previous": 1100, "change_pct": 13.6},
        "csat": {"value": 4.2, "previous": 4.0, "change": 0.2}
      }
    },
    "technical": {
      "weight_pct": 25,
      "score": 68,
      "trend": "stable",
      "metrics": {
        "slo_availability_pct": {"value": 99.93, "target": 99.9, "achieved": true},
        "slo_latency_p99_ms": {"value": 328, "target": 500, "achieved": true},
        "open_cves": {"critical": 0, "high": 0, "medium": 3, "low": 8},
        "eol_dependencies": 1,
        "deployment_frequency": {"value": "weekly", "dora_level": "high"},
        "change_failure_rate_pct": {"value": 5, "dora_level": "high"}
      }
    },
    "strategic": {
      "weight_pct": 20,
      "score": 80,
      "trend": "up",
      "metrics": {
        "roadmap_alignment_pct": 85,
        "market_share_trend": "growing",
        "cbb_contributions": 3,
        "compliance_status": "compliant"
      }
    },
    "composite_score": 76,
    "composite_trend": "up"
  },
  "sensitivity_analysis": {
    "key_assumptions": [
      {"assumption": "客户增长率保持 10%+", "impact_if_wrong": "财务评分下降 15 分"},
      {"assumption": "v2.0 AI 引擎按时交付", "impact_if_wrong": "战略评分下降 20 分"}
    ]
  },
  "recommendation": "invest_grow",
  "ipmt_decision": {
    "decision": "invest_grow",
    "actions": [
      {"action": "启动 v2.0 AI 引擎开发", "owner": "user-pdt-lead-001", "deadline": "2026-11-01"},
      {"action": "解决 1 个 EOL 依赖（Node.js 18→20）", "owner": "user-lmt-lead-001", "deadline": "2026-12-31"}
    ],
    "next_review": "Q4-2026"
  },
  "data_completeness": {
    "financial": "complete",
    "customer": "complete",
    "technical": "complete",
    "strategic": "complete"
  },
  "dcs_receipt": {
    "solver_version": "value-scorecard-v1",
    "input_hash": "sha256:...",
    "signature": "sha256:..."
  }
}
```

---
