## ACT-03-02-05 初步财务评估

### 基础信息

| 字段 | 内容 |
|---|---|
| **Activity ID** | ACT-03-02-05 |
| **Activity 名称** | 初步财务评估 |
| **所属阶段** | 概念 |
| **Lead Agent 角色** | Finance Lead |
| **执行位置** | task_system |
| **是否使用 Worker Agent** | — |
| **是否需要 Independent Verifier** | ✓（复算） |
| **所需模型能力** | FINANCIAL_REASONING + STRUCTURED_EXTRACTION |
| **所需 Skill** | ipd-concept-financial-modeling / spk-financial-model / spk-excel-query |
| **所需 Tool/MCP** | mcp-spk-os-finance-query / mcp-filesystem-read / mcp-spk-os-artifact-query |
| **所需 Knowledge** | KN-IPD-FINANCE-001 |
| **所需 Information** | — |
| **可读取的 Memory 范围** | project:spk-os-ipd, agent_role:finance, capability:financial_reasoning |
| **人工责任** | Finance Owner 签署 |
| **失败处理** | 财务模型不可复算则重新建模；假设不明确则升级 Finance Owner；Verifier 挑战则重新复算 |
| **对应 Flowable 节点** | task_financial_evaluation (ipd-concept.bpmn) |
| **证据来源** | RunReceipt + ArtifactManifest + VerificationReceipt |

### 资源与工具详情

#### 输入资源

| 资源 | 来源 | 获取方式 |
|---|---|---|
| ConceptOptionSet | ACT-03-02-04 产物 | mcp-spk-os-artifact-query.get(artifact_id) |
| 历史项目财务数据 | 财务系统 | mcp-spk-os-finance-query 直连 |
| 行业平均毛利率 | 行业报告 | mcp-filesystem-read |
| 研发投入基准 | 财务系统 | mcp-spk-os-finance-query |
| 销售费用基准 | 财务系统 | mcp-spk-os-finance-query |

#### 工具调用顺序

```
Step 1: 加载签名输入
├─ 调用 mcp-spk-os-artifact-query.get(ConceptOptionSet)
└─ 验证签名和 hash

Step 2: 构建 TAM/SAM/SOM 模型
├─ 调用 spk-financial-model.build_tam_sam_som(market_data, concept_options)
└─ 输出：TAM/SAM/SOM 估算

Step 3: 预测 3 年营收和成本
├─ 调用 spk-financial-model.forecast_revenue(tam_sam_som, adoption_rate)
├─ 调用 spk-financial-model.forecast_cost(r_and_d, sales, operations)
└─ 输出：3 年财务预测

Step 4: 计算盈亏平衡点
├─ 调用 spk-financial-model.calculate_breakeven(revenue, cost)
└─ 输出：盈亏平衡点

Step 5: 列出所有假设
├─ 明确列出：市场规模假设、增长率假设、毛利率假设、成本假设
└─ 输出：假设清单

Step 6: Verifier 独立复算
├─ Verifier 独立运行财务模型
├─ 对比结果差异
└─ 输出：VerificationReceipt

Step 7: Finance Owner 签署
└─ 输出：InitialBusinessCase（已签署）
```

### 输出 Artifact 模板

#### Artifact 名称
`InitialBusinessCase`

#### 文件结构
```
initial_business_case/
├── manifest.yaml
├── business_case.json
├── financial_model/         # 财务模型
│   ├── tam_sam_som.json
│   ├── revenue_forecast.json
│   ├── cost_forecast.json
│   └── breakeven.json
├── assumptions/             # 假设清单
│   └── assumptions.json
├── verification/            # 复算报告
│   └── verifier-report.json
└── README.md
```

#### business_case.json Schema

```json
{
  "case_id": "ibc-20260802-001",
  "version": "1.0.0",
  "created_at": "2026-08-02T14:00:00Z",
  "created_by": "agent-finance-001",
  "signed_by": {
    "user_id": "user-finance-owner-001",
    "signed_at": "2026-08-02T18:00:00Z",
    "decision": "admitted"
  },
  "verification_receipt": {
    "verifier_id": "agent-verifier-finance-001",
    "verified_at": "2026-08-02T17:00:00Z",
    "result": "pass",
    "variance": "<5%"
  },
  "concept_option_ref": "cos-20260802-001",
  "tam_sam_som": {
    "tam": {"value": 1000, "unit": "亿元", "source": "Gartner 2026"},
    "sam": {"value": 300, "unit": "亿元", "assumption": "目标细分占 TAM 30%"},
    "som": {"value": 30, "unit": "亿元", "assumption": "3 年内占 SAM 10%"}
  },
  "financial_forecast": {
    "year_1": {"revenue": 5, "cost": 8, "profit": -3},
    "year_2": {"revenue": 15, "cost": 18, "profit": -3},
    "year_3": {"revenue": 30, "cost": 25, "profit": 5}
  },
  "breakeven": {
    "year": 3,
    "month": 6,
    "cumulative_revenue": 50
  },
  "assumptions": [
    {
      "assumption_id": "assume-001",
      "description": "市场规模年增长率 20%",
      "source": "Gartner 2026",
      "confidence": "high"
    }
  ]
}
```

### 节点关联与展示

#### 在整体流程中的位置

```
ACT-03-02-04 概念与价值主张生成 → ACT-03-02-05 初步财务评估 → ACT-03-02-07 TR1/CDCP 决策包组装
                                          ↓
                                  InitialBusinessCase
```

#### 上游依赖
- ACT-03-02-04 ConceptOptionSet

#### 下游消费
- ACT-03-02-07 TR1/CDCP 决策包组装（财务摘要是 Evidence Pack 的一部分）

#### 可视化展示
- **TAM/SAM/SOM 漏斗图**：可视化展示市场规模
- **3 年财务预测图**：折线图展示营收、成本、利润
- **盈亏平衡分析图**：可视化展示盈亏平衡点

---
