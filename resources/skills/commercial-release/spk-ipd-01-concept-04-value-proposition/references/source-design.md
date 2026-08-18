## ACT-03-02-04 概念与价值主张生成

### 基础信息

| 字段 | 内容 |
|---|---|
| **Activity ID** | ACT-03-02-04 |
| **Activity 名称** | 概念与价值主张生成 |
| **所属阶段** | 概念 |
| **Lead Agent 角色** | Concept Lead |
| **执行位置** | lead_internal |
| **是否使用 Worker Agent** | — |
| **是否需要 Independent Verifier** | — |
| **所需模型能力** | LONG_CONTEXT_SYNTHESIS + STRUCTURED_DRAFTING |
| **所需 Skill** | ipd-concept-value-proposition / spk-jobs-to-be-done / spk-value-proposition-canvas |
| **所需 Tool/MCP** | mcp-filesystem-read / mcp-spk-os-artifact-query |
| **所需 Knowledge** | KN-IPD-CONCEPT-TPL-003 / KN-IPD-VALUE-CANVAS-004 / KN-IPD-JTBD-005 |
| **所需 Information** | — |
| **可读取的 Memory 范围** | project:spk-os-ipd, agent_role:concept, capability:long_context_synthesis |
| **人工责任** | PDT Lead 签署 |
| **失败处理** | 概念选项不足 3 个则重新生成；评分权重不明确则升级 PDT Lead |
| **对应 Flowable 节点** | task_concept_generation (ipd-concept.bpmn) |
| **证据来源** | RunReceipt + ArtifactManifest |

### 资源与工具详情

#### 输入资源

| 资源 | 来源 | 获取方式 |
|---|---|---|
| OpportunitySignalSet | ACT-03-02-01 产物 | mcp-spk-os-artifact-query.get(artifact_id) |
| CustomerNeedBrief | ACT-03-02-02 产物 | mcp-spk-os-artifact-query.get(artifact_id) |
| CompetitiveLandscape | ACT-03-02-03 产物 | mcp-spk-os-artifact-query.get(artifact_id) |
| 历史 Charter 模板 | Knowledge 库 | mcp-filesystem-read(/knowledge/ipd/templates/charter/) |
| Jobs-to-be-Done 框架 | Knowledge 库 | mcp-filesystem-read(/knowledge/ipd/methods/jtbd.md) |
| Value Proposition Canvas | Knowledge 库 | mcp-filesystem-read(/knowledge/ipd/methods/vpc.md) |

#### 工具调用顺序

```
Step 1: 加载签名输入
├─ 调用 mcp-spk-os-artifact-query.get(OpportunitySignalSet)
├─ 调用 mcp-spk-os-artifact-query.get(CustomerNeedBrief)
├─ 调用 mcp-spk-os-artifact-query.get(CompetitiveLandscape)
└─ 验证签名和 hash

Step 2: 应用 Jobs-to-be-Done 框架
├─ 从 CustomerNeedBrief 提取客户任务（jobs）
├─ 识别每个 job 的痛点（pains）和期望收益（gains）
└─ 输出：JTBD 分析表

Step 3: 应用 Value Proposition Canvas
├─ 基于 JTBD 分析，构建价值主张画布
├─ 匹配产品功能与客户收益
├─ 匹配产品特性与痛点缓解
└─ 输出：VPC 画布

Step 4: 生成 3 个概念选项
├─ 概念 A：高端差异化（基于 CompetitiveLandscape 的空白点）
├─ 概念 B：性价比优先（基于 CustomerNeedBrief 的高频需求）
├─ 概念 C：创新颠覆（基于 OpportunitySignalSet 的新兴趋势）
└─ 每个概念含：价值主张 + 目标细分 + 差异化 + 风险 + 评分

Step 5: PDT Lead 签署
└─ 输出：ConceptOptionSet（已签署）
```

### 输出 Artifact 模板

#### Artifact 名称
`ConceptOptionSet`

#### 文件结构
```
concept_option_set/
├── manifest.yaml
├── option_set.json
├── jtbd/                    # Jobs-to-be-Done 分析
│   └── jtbd-analysis.json
├── vpc/                     # Value Proposition Canvas
│   └── vpc-canvas.json
├── options/                 # 3 个概念选项
│   ├── option-a.md
│   ├── option-b.md
│   └── option-c.md
└── README.md
```

#### option_set.json Schema

```json
{
  "option_set_id": "cos-20260802-001",
  "version": "1.0.0",
  "created_at": "2026-08-02T13:00:00Z",
  "created_by": "agent-concept-001",
  "signed_by": {
    "user_id": "user-pdt-lead-001",
    "signed_at": "2026-08-02T17:00:00Z",
    "decision": "admitted"
  },
  "inputs": {
    "opportunity_signal_set_ref": "oss-20260802-001",
    "customer_need_brief_ref": "cnb-20260802-001",
    "competitive_landscape_ref": "cl-20260802-001"
  },
  "jtbd_analysis": {
    "jobs": [
      {
        "job_id": "job-001",
        "description": "快速部署 AI 能力",
        "pains": ["部署复杂", "成本高", "技术门槛高"],
        "gains": ["快速上线", "降低成本", "提升效率"]
      }
    ]
  },
  "options": [
    {
      "option_id": "option-a",
      "name": "企业级 AI 云服务平台",
      "value_proposition": "为大型企业提供一站式 AI 云服务，包括模型训练、推理、部署",
      "target_segment": "大型企业",
      "differentiation": "自研 AI 芯片 + 全栈云服务",
      "risks": ["市场竞争激烈", "技术门槛高"],
      "scores": {
        "market_size": 5,
        "technical_feasibility": 4,
        "competitive_advantage": 4,
        "financial_return": 4,
        "weighted_total": 4.3
      },
      "weights": {
        "market_size": 0.3,
        "technical_feasibility": 0.2,
        "competitive_advantage": 0.3,
        "financial_return": 0.2
      }
    }
  ]
}
```

### 节点关联与展示

#### 在整体流程中的位置

```
ACT-03-02-01/02/03 → ACT-03-02-04 概念与价值主张生成 → ACT-03-02-05 初步财务评估
                              ↓
                      ConceptOptionSet
                              ↓
                      ACT-03-02-06 技术风险与 CBB 识别
```

#### 上游依赖
- ACT-03-02-01 OpportunitySignalSet
- ACT-03-02-02 CustomerNeedBrief
- ACT-03-02-03 CompetitiveLandscape

#### 下游消费
- ACT-03-02-05 初步财务评估（基于概念选项估算财务）
- ACT-03-02-06 技术风险与 CBB 识别（基于概念选项识别技术风险）

#### 可视化展示
- **概念对比表**：3 个概念在价值主张、目标细分、差异化、风险、评分上的对比
- **JTBD 画布**：可视化展示客户任务、痛点、收益
- **VPC 画布**：可视化展示价值主张与客户需求的匹配

---
