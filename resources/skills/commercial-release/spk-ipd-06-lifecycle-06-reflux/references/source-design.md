## ACT-03-07-06 Next-Gen Requirement Reflux

### 基础信息

| 字段 | 内容 |
|---|---|
| **Activity ID** | ACT-03-07-06 |
| **Activity 名称** | Next-Gen Requirement Reflux（下一代需求回流） |
| **所属阶段** | 生命周期管理 |
| **Lead Agent 角色** | Lifecycle Orchestrator |
| **执行位置** | task_system |
| **是否使用 Worker Agent** | ✓（Reflux Agent 需求提取/格式化/去重 + Provenance Tracker 维护来源链） |
| **是否需要 Independent Verifier** | ✓（DCS 校验来源/去重/版本 + Product Owner 接受/拒绝/延后） |
| **所需模型能力** | CROSS_SOURCE_REASONING + STRUCTURED_DRAFTING |
| **所需 Skill** | ipd-lifecycle-reflux / spk-reflux-agent / spk-provenance-tracker |
| **所需 Tool/MCP** | mcp-spk-os-artifact-query / mcp-requirement-query |
| **所需 Knowledge** | KN-IPD-REFLUX-SCHEMA-081 / KN-W3C-PROV-O-082 |
| **所需 Information** | INFO-FEEDBACK-THEMES（反馈主题集合）/ INFO-ROADMAP（路线图）/ INFO-EXISTING-REQUIREMENTS（现有需求库） |
| **可读取的 Memory 范围** | project:spk-os-ipd, agent_role:lifecycle, capability:cross_source_reasoning |
| **人工责任** | Product Owner 审核候选需求、确认回流、决定接受/拒绝/再研究 |
| **失败处理** | 映射低置信则按产品域重试；仍低人工整理；跨代兼容冲突升级 Product/Architecture；被拒候选回滚为归档，不进入基线 |
| **对应 Flowable 节点** | task_requirement_refux (ipd-lifecycle.bpmn) |
| **证据来源** | RunReceipt + NextGenRequirementCandidateSet + ArtifactManifest |

### 资源与工具详情

#### 工具调用顺序与效率策略

```
Step 1: 选择经签名主题
├─ 从 FeedbackThemeSet 中提取高置信度主题
├─ 过滤掉忠实度 < 0.90 的主题
├─ 过滤掉噪声信号
└─ 输出：合格主题列表

Step 2: Reflux Agent 提取需求候选
├─ 调用 spk-reflux-agent.extract(themes, roadmap, existing_requirements)
│  ├─ 对每个合格主题：
│  │  ├─ 提取问题陈述（从代表性引用中归纳）
│  │  ├─ 提取期望结果（从客户描述中推断）
│  │  ├─ 提取边界条件（从反例和少数派意见中识别）
│  │  ├─ 生成候选验收标准
│  │  ├─ 与现有需求查重（相似度 > 0.85 标记为重复）
│  │  ├─ 与路线图对齐（是否已在计划中）
│  │  ├─ 评估价值/风险（基于影响客户数和严重度）
│  │  └─ 标注来源信号 ID（Provenance 链）
│  └─ 输出：需求候选草稿
└─ 输出：需求候选草稿

Step 3: Provenance Tracker 维护来源链
├─ 调用 spk-provenance-tracker.record(candidates, source_signals)
│  ├─ 为每个候选需求建立 W3C PROV-O 兼容的来源链：
│  │  ├─ Entity: 原始工单（signal_id, raw_text_hash）
│  │  ├─ Activity: 信号摄取（ACT-03-07-01）
│  │  ├─ Activity: 反馈聚类（ACT-03-07-02, theme_id）
│  │  ├─ Activity: 需求回流（ACT-03-07-06, candidate_id）
│  │  ├─ Agent: Reflux Agent（model_id, prompt_hash）
│  │  └─ Entity: 候选需求（candidate_id, version）
│  ├─ 确保每步可追溯：
│  │  □ 从候选需求可追溯到原始工单
│  │  □ 从原始工单可追溯到候选需求
│  │  □ 中间每一步的 Agent/人工操作都有记录
│  └─ 输出：Provenance 链
└─ 输出：ProvenanceChain

Step 4: DCS 校验
├─ 调用 DCS POST /api/v1/reflux-validate
│  ├─ 检查：
│  │  □ 每个候选需求有来源信号（无孤儿来源）
│  │  □ Provenance 链完整（每步有 PROV-O 记录）
│  │  □ 无重复需求（与现有需求库比对）
│  │  □ 无冲突需求（新需求不与现有需求矛盾）
│  │  □ 隐私最小化（候选需求中无 PII）
│  ├─ 输出：校验报告 + 签名
│  └─ 签名：sha256(...)
└─ 输出：DCS 校验报告

Step 5: Product Owner 审核
├─ Product Owner 审查每个候选需求：
│  ├─ 问题陈述是否准确
│  ├─ 期望结果是否合理
│  ├─ 验收标准是否可测试
│  ├─ 价值/风险评估是否合理
│  └─ 做出决定：
│      ├─ 接受：纳入下一代需求基线
│      ├─ 拒绝：记录理由，归档候选
│      ├─ 延后：标记为"需要更多研究"
│      └─ 修改：调整后重新评审
├─ **AI 不直接写入需求基线**
├─ 决定及理由写回 Flowable
└─ 输出：NextGenRequirementCandidateSet（已签署）
```

### 输出 Artifact 模板

#### Artifact 名称
`NextGenRequirementCandidateSet`

#### NextGenRequirementCandidateSet Schema

```json
{
  "set_id": "ngr-20260920-001",
  "version": "1.0.0",
  "created_at": "2026-09-20T10:00:00Z",
  "signed_by": {
    "user_id": "user-product-owner-001",
    "role": "Product Owner",
    "signed_at": "2026-09-20T14:00:00Z"
  },
  "source_themes_ref": "fts-20260915-001",
  "candidates": [
    {
      "candidate_id": "NR-007",
      "problem_statement": "大型 monorepo（>10K files）的代码审查索引时间过长，影响开发者工作流效率",
      "desired_outcome": "10K+ 文件的仓库索引时间 < 2 分钟，增量索引 < 30 秒",
      "boundary_conditions": [
        "不适用于 binary-heavy 仓库（>50% binary files）",
        "需要足够的内存（≥16GB）"
      ],
      "acceptance_criteria_candidate": [
        "10K 文件仓库首次索引 < 2 分钟",
        "增量索引（100 文件变更）< 30 秒",
        "索引过程不阻塞审查操作"
      ],
      "source_signals": ["SIG-20260915-003", "SIG-20260915-007", "SIG-20260915-011"],
      "source_theme": "TH-001",
      "affected_customers_count": 12,
      "value_risk_assessment": {
        "value_score": 75,
        "risk_score": 30,
        "rationale": "影响 12 个企业客户，严重度 P2，技术可行性高"
      },
      "duplicate_check": {
        "similar_existing_requirement": null,
        "is_duplicate": false
      },
      "roadmap_alignment": {
        "in_current_roadmap": false,
        "suggested_version": "v3.0"
      },
      "provenance": {
        "prov_o_uri": "provenance://NR-007",
        "chain": [
          {"type": "Entity", "id": "SIG-20260915-003", "role": "original_ticket"},
          {"type": "Activity", "id": "ACT-03-07-01", "role": "signal_ingestion"},
          {"type": "Activity", "id": "ACT-03-07-02/TH-001", "role": "feedback_clustering"},
          {"type": "Activity", "id": "ACT-03-07-06/NR-007", "role": "requirement_refux"},
          {"type": "Entity", "id": "NR-007", "role": "candidate_requirement"}
        ]
      },
      "po_decision": {
        "decision": "accepted",
        "rationale": "高价值需求，影响企业客户核心工作流，技术可行",
        "next_step": "纳入 v3.0 概念阶段 ACT-03-02-01"
      }
    }
  ],
  "statistics": {
    "total_candidates": 8,
    "accepted": 5,
    "rejected": 2,
    "deferred": 1,
    "provenance_completeness_pct": 100
  },
  "dcs_receipt": {
    "solver_version": "reflux-validate-v1",
    "input_hash": "sha256:...",
    "signature": "sha256:..."
  }
}
```

---
