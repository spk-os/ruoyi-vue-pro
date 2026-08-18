## ACT-03-02-06 技术风险与 CBB 识别

### 基础信息

| 字段 | 内容 |
|---|---|
| **Activity ID** | ACT-03-02-06 |
| **Activity 名称** | 技术风险与 CBB 识别 |
| **所属阶段** | 概念 |
| **Lead Agent 角色** | Architect Lead |
| **执行位置** | task_system |
| **是否使用 Worker Agent** | ✓（按技术域并行） |
| **是否需要 Independent Verifier** | — |
| **所需模型能力** | SOFTWARE_ENGINEERING + LONG_CONTEXT_SYNTHESIS |
| **所需 Skill** | ipd-concept-tech-risk / spk-cbb-query / spk-tech-radar |
| **所需 Tool/MCP** | mcp-spk-os-cbb-query / mcp-codegraph-explore / mcp-filesystem-read / mcp-spk-os-artifact-query |
| **所需 Knowledge** | KN-IPD-CBB-006 / KN-IPD-TECH-RADAR-007 |
| **所需 Information** | — |
| **可读取的 Memory 范围** | project:spk-os-ipd, agent_role:architect, capability:software_engineering |
| **人工责任** | SE 与 TDT 签署 |
| **失败处理** | 技术风险无缓解措施则阻断；CBB 无 owner 则阻断；技术域评估不完整则重新评估 |
| **对应 Flowable 节点** | task_tech_risk_cbb (ipd-concept.bpmn) |
| **证据来源** | RunReceipt + ToolReceipt + ArtifactManifest |

### 资源与工具详情

#### 输入资源

| 资源 | 来源 | 获取方式 |
|---|---|---|
| ConceptOptionSet | ACT-03-02-04 产物 | mcp-spk-os-artifact-query.get(artifact_id) |
| CBB 库 | CBB Registry | mcp-spk-os-cbb-query 直连 |
| 技术雷达 | Knowledge 库 | mcp-filesystem-read(/knowledge/tech/radar/) |
| 历史技术风险案例 | Knowledge 库 | mcp-filesystem-read(/knowledge/tech/risks/) |

#### 工具调用顺序

```
Step 1: 加载签名输入
├─ 调用 mcp-spk-os-artifact-query.get(ConceptOptionSet)
└─ 验证签名和 hash

Step 2: 并行启动 3-5 个 Worker Agent（按技术域）
├─ Worker-1: AI/ML 技术域
│  ├─ 调用 mcp-spk-os-cbb-query.query(domain="ai_ml")
│  ├─ 调用 mcp-codegraph-explore 分析现有代码库
│  └─ 输出：AI/ML 技术风险 + 可复用 CBB
├─ Worker-2: 云计算技术域
│  └─ ...（同上）
└─ Worker-3: 数据技术域
   └─ ...（同上）

Step 3: Lead Agent 聚合分析
├─ 合并所有 Worker 的技术风险和 CBB
├─ 识别跨域风险
└─ 输出：TechnicalRiskAndCBBMap（草稿）

Step 4: SE 与 TDT 签署
└─ 输出：TechnicalRiskAndCBBMap（已签署）
```

### 输出 Artifact 模板

#### Artifact 名称
`TechnicalRiskAndCBBMap`

#### 文件结构
```
technical_risk_and_cbb_map/
├── manifest.yaml
├── risk_cbb_map.json
├── domains/                 # 按技术域分类
│   ├── ai_ml/
│   │   ├── risks.json
│   │   └── cbbs.json
│   ├── cloud/
│   └── data/
└── README.md
```

#### risk_cbb_map.json Schema

```json
{
  "map_id": "trcm-20260802-001",
  "version": "1.0.0",
  "created_at": "2026-08-02T15:00:00Z",
  "created_by": "agent-architect-001",
  "signed_by": [
    {"user_id": "user-se-001", "signed_at": "2026-08-02T19:00:00Z"},
    {"user_id": "user-tdt-001", "signed_at": "2026-08-02T19:30:00Z"}
  ],
  "concept_option_ref": "cos-20260802-001",
  "domains": [
    {
      "domain": "ai_ml",
      "risks": [
        {
          "risk_id": "risk-001",
          "description": "大模型推理成本高",
          "probability": "high",
          "impact": "high",
          "mitigation": "使用模型压缩和量化技术",
          "owner": "AI 团队"
        }
      ],
      "cbbs": [
        {
          "cbb_id": "cbb-001",
          "name": "模型推理引擎",
          "version": "2.0",
          "owner": "AI 平台组",
          "reusability": "high",
          "evidence": ["已在 3 个项目中使用"]
        }
      ]
    }
  ]
}
```

### 节点关联与展示

#### 在整体流程中的位置

```
ACT-03-02-04 概念与价值主张生成 → ACT-03-02-06 技术风险与 CBB 识别 → ACT-03-02-07 TR1/CDCP 决策包组装
                                          ↓
                                  TechnicalRiskAndCBBMap
```

#### 上游依赖
- ACT-03-02-04 ConceptOptionSet

#### 下游消费
- ACT-03-02-07 TR1/CDCP 决策包组装（技术风险和 CBB 是 Evidence Pack 的一部分）

#### 可视化展示
- **技术风险矩阵**：按概率和影响展示风险
- **CBB 复用地图**：可视化展示可复用的 CBB

---
