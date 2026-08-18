## ACT-03-02-07 TR1/CDCP 决策包组装

### 基础信息

| 字段 | 内容 |
|---|---|
| **Activity ID** | ACT-03-02-07 |
| **Activity 名称** | TR1/CDCP 决策包组装 |
| **所属阶段** | 概念 |
| **Lead Agent 角色** | Concept Lead |
| **执行位置** | task_system |
| **是否使用 Worker Agent** | — |
| **是否需要 Independent Verifier** | ✓（DCP-Verifier） |
| **所需模型能力** | STRUCTURED_DRAFTING + GROUNDED_EXTRACTION |
| **所需 Skill** | ipd-concept-evidence-pack / spk-artifact-assembly |
| **所需 Tool/MCP** | mcp-spk-os-artifact-query / mcp-filesystem-read |
| **所需 Knowledge** | KN-IPD-CDCP-TPL-001 |
| **所需 Information** | — |
| **可读取的 Memory 范围** | project:spk-os-ipd, agent_role:concept, capability:structured_drafting |
| **人工责任** | PDT Lead / Configuration Manager 签署 |
| **失败处理** | claim 无可定位证据则阻断；Evidence Pack 不完整则重新组装；Verifier 挑战则重新核证 |
| **对应 Flowable 节点** | task_tr1_cdcp_pack (ipd-concept.bpmn) |
| **证据来源** | RunReceipt + ArtifactManifest + VerificationReceipt |

### 资源与工具详情

#### 输入资源

| 资源 | 来源 | 获取方式 |
|---|---|---|
| OpportunitySignalSet | ACT-03-02-01 产物 | mcp-spk-os-artifact-query.get |
| CustomerNeedBrief | ACT-03-02-02 产物 | mcp-spk-os-artifact-query.get |
| CompetitiveLandscape | ACT-03-02-03 产物 | mcp-spk-os-artifact-query.get |
| ConceptOptionSet | ACT-03-02-04 产物 | mcp-spk-os-artifact-query.get |
| InitialBusinessCase | ACT-03-02-05 产物 | mcp-spk-os-artifact-query.get |
| TechnicalRiskAndCBBMap | ACT-03-02-06 产物 | mcp-spk-os-artifact-query.get |

#### 工具调用顺序

```
Step 1: 加载所有签名输入
├─ 调用 mcp-spk-os-artifact-query.get(所有 6 个 Artifact)
└─ 验证签名和 hash

Step 2: 提取每个 claim 的 locator
├─ 从每个 Artifact 中提取关键 claim
├─ 为每个 claim 标注 locator（来源位置）
└─ 输出：claim-locator 映射

Step 3: 组装 Evidence Pack
├─ 按 CDCP 模板组装：市场机会 + 客户需求 + 竞争格局 + 概念选项 + 财务评估 + 技术风险
├─ 列出财务摘要
├─ 列出风险
├─ 列出备选方案
├─ 列出反方意见
└─ 输出：TR1_CDCP_EvidencePack（草稿）

Step 4: Verifier 独立核证
├─ Verifier 检查每个 claim 是否有可定位证据
├─ Verifier 检查 Evidence Pack 完整性
└─ 输出：VerificationReceipt

Step 5: PDT Lead / Configuration Manager 签署
└─ 输出：TR1_CDCP_EvidencePack（已签署）
```

### 输出 Artifact 模板

#### Artifact 名称
`TR1_CDCP_EvidencePack`

#### 文件结构
```
tr1_cdcp_evidence_pack/
├── manifest.yaml
├── evidence_pack.json
├── claims/                  # 所有 claim
│   ├── claim-001.json
│   └── ...
├── evidence/                # 所有证据
│   ├── evidence-001.json
│   └── ...
├── summary/                 # 摘要
│   ├── financial_summary.md
│   ├── risk_summary.md
│   └── alternative_summary.md
└── README.md
```

#### evidence_pack.json Schema

```json
{
  "pack_id": "tcep-20260802-001",
  "version": "1.0.0",
  "created_at": "2026-08-02T16:00:00Z",
  "created_by": "agent-concept-001",
  "signed_by": [
    {"user_id": "user-pdt-lead-001", "signed_at": "2026-08-02T20:00:00Z"},
    {"user_id": "user-config-mgr-001", "signed_at": "2026-08-02T20:30:00Z"}
  ],
  "verification_receipt": {
    "verifier_id": "agent-verifier-dcp-001",
    "verified_at": "2026-08-02T19:00:00Z",
    "result": "pass",
    "completeness": "100%"
  },
  "input_artifacts": [
    {"artifact_id": "oss-20260802-001", "type": "OpportunitySignalSet"},
    {"artifact_id": "cnb-20260802-001", "type": "CustomerNeedBrief"},
    {"artifact_id": "cl-20260802-001", "type": "CompetitiveLandscape"},
    {"artifact_id": "cos-20260802-001", "type": "ConceptOptionSet"},
    {"artifact_id": "ibc-20260802-001", "type": "InitialBusinessCase"},
    {"artifact_id": "trcm-20260802-001", "type": "TechnicalRiskAndCBBMap"}
  ],
  "claims": [
    {
      "claim_id": "claim-001",
      "description": "云计算 AI 化是未来 3 年的主要趋势",
      "source_artifact": "oss-20260802-001",
      "locator": "clusters[0]",
      "evidence_refs": ["evidence-001", "evidence-002"]
    }
  ],
  "financial_summary": {
    "tam": "1000 亿元",
    "som": "30 亿元",
    "breakeven": "第 3 年第 6 个月"
  },
  "risks": [
    {"risk": "市场竞争激烈", "mitigation": "差异化定位"}
  ],
  "alternatives": [
    {"option": "option-a", "score": 4.3},
    {"option": "option-b", "score": 3.8}
  ],
  "counter_opinions": [
    {"opinion": "小型企业市场可能不需要 AI 云服务", "source": "Verifier"}
  ]
}
```

### 节点关联与展示

#### 在整体流程中的位置

```
ACT-03-02-01~06 → ACT-03-02-07 TR1/CDCP 决策包组装 → ACT-03-02-08 TR1 评审支持
                          ↓
                  TR1_CDCP_EvidencePack
```

#### 上游依赖
- 所有前 6 项 Activity 的产物

#### 下游消费
- ACT-03-02-08 TR1 评审支持（Evidence Pack 是 TR1 评审的输入）

#### 可视化展示
- **Evidence Pack 目录树**：可视化展示所有 claim 和 evidence 的关系
- **证据链视图**：点击任意 claim，展示完整的证据链

---
