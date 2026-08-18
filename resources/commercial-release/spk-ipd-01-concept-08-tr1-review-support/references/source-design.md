## ACT-03-02-08 TR1 评审支持

### 基础信息

| 字段 | 内容 |
|---|---|
| **Activity ID** | ACT-03-02-08 |
| **Activity 名称** | TR1 评审支持 |
| **所属阶段** | 概念 |
| **Lead Agent 角色** | Governance Lead |
| **执行位置** | lead_internal |
| **是否使用 Worker Agent** | — |
| **是否需要 Independent Verifier** | ✓（TR-Verifier） |
| **所需模型能力** | INDEPENDENT_VERIFICATION |
| **所需 Skill** | ipd-tr1-support / spk-tr-checklist |
| **所需 Tool/MCP** | mcp-spk-os-artifact-query / mcp-filesystem-read |
| **所需 Knowledge** | KN-IPD-TR1-CHECKLIST-008 |
| **所需 Information** | — |
| **可读取的 Memory 范围** | project:spk-os-ipd, agent_role:governance, capability:independent_verification |
| **人工责任** | 具名评委 / IPMT Chair 签署 |
| **失败处理** | 法定人数不足则延期；独立性冲突则升级 Chair；评审记录不完整则重新评审 |
| **对应 Flowable 节点** | task_tr1_review (ipd-concept.bpmn) |
| **证据来源** | RunReceipt + ArtifactManifest + VerificationReceipt + HumanApprovalRecord |

### 资源与工具详情

#### 输入资源

| 资源 | 来源 | 获取方式 |
|---|---|---|
| TR1_CDCP_EvidencePack | ACT-03-02-07 产物 | mcp-spk-os-artifact-query.get |
| TR1 Checklist | Knowledge 库 | mcp-filesystem-read(/knowledge/ipd/tr1/checklist.md) |

#### 工具调用顺序

```
Step 1: 加载签名输入
├─ 调用 mcp-spk-os-artifact-query.get(TR1_CDCP_EvidencePack)
└─ 验证签名和 hash

Step 2: 调用 TR1 Checklist Skill 逐项核对
├─ 检查市场机会是否充分
├─ 检查客户需求是否明确
├─ 检查竞争格局是否清晰
├─ 检查概念选项是否合理
├─ 检查财务评估是否可信
├─ 检查技术风险是否可控
└─ 输出：Checklist 核对结果

Step 3: TR-Verifier 独立执行 TR1 评审
├─ Verifier 独立审查 Evidence Pack
├─ Verifier 生成评审意见
└─ 输出：VerificationReceipt

Step 4: 每个评委独立投票
├─ 每个评委查看 Evidence Pack 和 Verifier 报告
├─ 每个评委独立投票（Go/No-Go/Conditional Go）
├─ 记录每个评委的投票和理由
└─ 输出：投票记录

Step 5: IPMT Chair 签署
└─ 输出：TR1DecisionRecord（已签署）
```

### 输出 Artifact 模板

#### Artifact 名称
`TR1DecisionRecord`

#### 文件结构
```
tr1_decision_record/
├── manifest.yaml
├── decision_record.json
├── checklist/               # Checklist 核对结果
│   └── checklist-result.json
├── votes/                   # 投票记录
│   ├── vote-judge-001.json
│   └── ...
├── verification/            # Verifier 报告
│   └── verifier-report.json
└── README.md
```

#### decision_record.json Schema

```json
{
  "record_id": "tdr-20260802-001",
  "version": "1.0.0",
  "created_at": "2026-08-02T17:00:00Z",
  "created_by": "agent-governance-001",
  "signed_by": {
    "user_id": "user-ipmt-chair-001",
    "signed_at": "2026-08-02T21:00:00Z",
    "decision": "go"
  },
  "evidence_pack_ref": "tcep-20260802-001",
  "verification_receipt": {
    "verifier_id": "agent-verifier-tr1-001",
    "verified_at": "2026-08-02T20:00:00Z",
    "result": "pass"
  },
  "checklist_result": {
    "market_opportunity": "pass",
    "customer_need": "pass",
    "competitive_landscape": "pass",
    "concept_options": "pass",
    "financial_evaluation": "pass",
    "technical_risk": "conditional_pass"
  },
  "votes": [
    {
      "judge_id": "user-judge-001",
      "vote": "go",
      "rationale": "市场机会明确，技术风险可控",
      "voted_at": "2026-08-02T20:30:00Z"
    },
    {
      "judge_id": "user-judge-002",
      "vote": "conditional_go",
      "rationale": "需要补充小型企业市场分析",
      "voted_at": "2026-08-02T20:35:00Z"
    }
  ],
  "dissent": [
    {
      "judge_id": "user-judge-002",
      "concern": "小型企业市场分析不足",
      "resolution": "作为 Conditional Go 的条件"
    }
  ],
  "final_decision": "conditional_go",
  "conditions": [
    "补充小型企业市场分析",
    "在下一次评审前完成"
  ]
}
```

### 节点关联与展示

#### 在整体流程中的位置

```
ACT-03-02-07 TR1/CDCP 决策包组装 → ACT-03-02-08 TR1 评审支持 → [概念阶段结束，进入计划阶段]
                                          ↓
                                  TR1DecisionRecord
```

#### 上游依赖
- ACT-03-02-07 TR1_CDCP_EvidencePack

#### 下游消费
- 计划阶段的所有 Activity（TR1 决策是进入计划阶段的前提）

#### 可视化展示
- **TR1 Checklist 核对表**：可视化展示每项检查的结果
- **投票分布图**：饼图展示投票分布
- **不同意意见列表**：高亮展示不同意意见和解决方案

---

## 概念阶段 Activity 汇总

| Activity ID | 名称 | Lead Agent | 执行位置 | Worker | Verifier | 模型能力 |
|---|---|---|---|---|---|---|
| ACT-03-02-01 | 机会信号聚合 | MarketInsight Lead | task_system | ✓ | — | MARKET_RESEARCH + STRUCTURED_EXTRACTION |
| ACT-03-02-02 | 客户需求深度分析 | MarketInsight Lead | task_system | ✓ | — | MARKET_RESEARCH + GROUNDED_EXTRACTION |
| ACT-03-02-03 | 竞争格局分析 | Strategy Lead | task_system | ✓ | ✓ | MARKET_RESEARCH + CROSS_SOURCE_REASONING + COUNTERARGUMENT_GENERATION |
| ACT-03-02-04 | 概念与价值主张生成 | Concept Lead | lead_internal | — | — | LONG_CONTEXT_SYNTHESIS + STRUCTURED_DRAFTING |
| ACT-03-02-05 | 初步财务评估 | Finance Lead | task_system | — | ✓ | FINANCIAL_REASONING + STRUCTURED_EXTRACTION |
| ACT-03-02-06 | 技术风险与 CBB 识别 | Architect Lead | task_system | ✓ | — | SOFTWARE_ENGINEERING + LONG_CONTEXT_SYNTHESIS |
| ACT-03-02-07 | TR1/CDCP 决策包组装 | Concept Lead | task_system | — | ✓ | STRUCTURED_DRAFTING + GROUNDED_EXTRACTION |
| ACT-03-02-08 | TR1 评审支持 | Governance Lead | lead_internal | — | ✓ | INDEPENDENT_VERIFICATION |

---

## 概念阶段产物关联图

```
外部数据源 → ACT-03-02-01 机会信号聚合 → OpportunitySignalSet
                                              ↓
VOC 数据 → ACT-03-02-02 客户需求深度分析 → CustomerNeedBrief
                                              ↓
竞品数据 → ACT-03-02-03 竞争格局分析 → CompetitiveLandscape
                                              ↓
                    ACT-03-02-04 概念与价值主张生成 → ConceptOptionSet
                                              ↓
                    ACT-03-02-05 初步财务评估 → InitialBusinessCase
                                              ↓
                    ACT-03-02-06 技术风险与 CBB 识别 → TechnicalRiskAndCBBMap
                                              ↓
                    ACT-03-02-07 TR1/CDCP 决策包组装 → TR1_CDCP_EvidencePack
                                              ↓
                    ACT-03-02-08 TR1 评审支持 → TR1DecisionRecord
                                              ↓
                                        [进入计划阶段]
```
