## ACT-03-04-10 TR3/TR4 评审

### 基础信息

| 字段 | 内容 |
|---|---|
| **Activity ID** | ACT-03-04-10 |
| **Activity 名称** | TR3/TR4 评审 |
| **所属阶段** | 开发 |
| **Lead Agent 角色** | Development Orchestrator |
| **执行位置** | task_system |
| **是否使用 Worker Agent** | ✓（Independent Review Agent 在隔离上下文质询） |
| **是否需要 Independent Verifier** | ✓（DCS 校验权限/法定人数/材料版本 + Independent Review Agent） |
| **所需模型能力** | CROSS_SOURCE_REASONING + COUNTERARGUMENT_GENERATION + SAFETY_REVIEW |
| **所需 Skill** | ipd-dev-tr-review / spk-independent-reviewer |
| **所需 Tool/MCP** | mcp-spk-os-artifact-query / mcp-filesystem-read |
| **所需 Knowledge** | KN-IPD-TR3-CHECKLIST-033 / KN-IPD-TR4-CHECKLIST-034 / KN-GAO-INDEPENDENT-VERIFY-035 |
| **所需 Information** | INFO-DEV-EVIDENCE-PACK（开发证据包） |
| **可读取的 Memory 范围** | project:spk-os-ipd, agent_role:development, capability:cross_source_reasoning |
| **人工责任** | 具名评委投票/签署（**AI 无投票权**） |
| **失败处理** | 检索失败降级人工翻包；法定人数不足延期；独立性或安全异议升级 Chair/PQA；无有效签名回滚到 review_pending |
| **对应 Flowable 节点** | task_tr3_review / task_tr4_review (ipd-development.bpmn) |
| **证据来源** | RunReceipt + TR3_TR4_DecisionRecord + ArtifactManifest |

### 资源与工具详情

#### 工具调用顺序与效率策略

```
Step 1: 锁定评审包
├─ 锁定 DevelopmentEvidencePack 版本
├─ 计算评审包的整体 hash
├─ 记录锁定时间戳
└─ 输出：锁定评审包

Step 2: DCS 校验评审前置条件
├─ 调用 DCS POST /api/v1/tr-prerequisites
│  ├─ 检查：
│  │  □ 评委权限：所有评委具有有效评审资格
│  │  □ 独立性：Independent Review Agent 与作者 Agent 使用不同模型/Prompt
│  │  □ 回避：无利益冲突的评委
│  │  □ 法定人数：评委数量满足组织要求
│  │  □ 材料版本：所有证据包版本一致
│  ├─ 输出：前置条件检查报告 + 签名
│  └─ 签名：sha256(...)
└─ 输出：DCS 前置条件报告

Step 3: Independent Review Agent 独立质询
├─ 使用不同模型/Prompt（与作者 Agent 隔离）
├─ 不读取作者的隐藏推理
├─ 审查证据包完整性
│  ├─ 追溯链是否完整
│  ├─ 证据是否可复现
│  ├─ 例外是否合理
│  ├─ 风险是否已处置
│  └─ 安全证据是否充分
├─ 生成质询报告（问题列表 + 证据引用）
├─ **不投票，不批准，只质询**
└─ 输出：IndependentReviewReport

Step 4: 评委审查与质询答复
├─ 评委查看证据包 + Independent Review Report
├─ 对质询问题逐一答复
├─ 记录答复和补充证据
└─ 输出：质询答复记录

Step 5: 评委投票与签署
├─ 每个评委独立投票：
│  ├─ Go：通过，进入下一阶段
│  ├─ Conditional Go：通过，但有附加条件
│  └─ No-Go：不通过，需要修改后重新评审
├─ 记录投票理由和异议
├─ **AI 无投票权**
└─ 输出：TR3_TR4_DecisionRecord（已签署）

Step 6: 发布行动
├─ 如果 Go：
│  ├─ 更新 Flowable 状态
│  ├─ 发布行动项（条件对应的后续工作）
│  └─ 进入下一阶段（TR3→继续开发 / TR4→进入验证阶段）
├─ 如果 No-Go：
│  ├─ 记录需要修改的内容
│  ├─ 回退到开发阶段
│  └─ 重新执行受影响的开发包
└─ 输出：行动项列表
```

### 输出 Artifact 模板

#### Artifact 名称
`TR3_TR4_DecisionRecord`

#### TR3_TR4_DecisionRecord Schema

```json
{
  "decision_id": "tr3-20260825-001",
  "tr_type": "TR3",
  "version": "1.0.0",
  "evidence_pack_ref": "dep-20260825-001",
  "evidence_pack_hash": "sha256:...",
  "independent_review": {
    "agent_id": "agent-independent-review-001",
    "model_id": "gpt-5",
    "model_family": "openai",
    "isolated_from_author": true,
    "challenges": [
      {
        "challenge_id": "CH-001",
        "description": "DP-004 缺少性能测试基线",
        "evidence_ref": "DP-004/CIRunEvidence",
        "severity": "major"
      }
    ],
    "report_hash": "sha256:..."
  },
  "challenge_responses": [
    {
      "challenge_id": "CH-001",
      "response": "已补充 DP-004 的性能测试基线，P95 = 180ms",
      "supplementary_evidence": "DP-004/CIRunEvidence-v2",
      "accepted": true
    }
  ],
  "votes": [
    {
      "user_id": "user-tr-chair-001",
      "role": "TR Chair",
      "vote": "go",
      "rationale": "所有质询已答复，证据完整",
      "voted_at": "2026-08-25T16:00:00Z"
    },
    {
      "user_id": "user-tr-member-001",
      "role": "TR Member",
      "vote": "go",
      "rationale": "技术风险可控",
      "voted_at": "2026-08-25T16:15:00Z"
    },
    {
      "user_id": "user-tr-member-002",
      "role": "TR Member",
      "vote": "conditional_go",
      "rationale": "需要补充安全渗透测试报告",
      "conditions": ["在验证阶段前完成安全渗透测试"],
      "voted_at": "2026-08-25T16:30:00Z"
    }
  ],
  "decision": "go",
  "conditions": ["在验证阶段前完成安全渗透测试"],
  "dissent": [],
  "dcs_receipt": {
    "solver_version": "tr-prerequisites-v1",
    "input_hash": "sha256:...",
    "signature": "sha256:..."
  }
}
```

---
