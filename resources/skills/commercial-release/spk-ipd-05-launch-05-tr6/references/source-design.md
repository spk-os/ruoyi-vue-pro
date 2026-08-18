## ACT-03-06-05 TR6 证据包组装与评审

### 基础信息

| 字段 | 内容 |
|---|---|
| **Activity ID** | ACT-03-06-05 |
| **Activity 名称** | TR6 证据包组装与评审 |
| **所属阶段** | 发布 |
| **Lead Agent 角色** | Release Orchestrator |
| **执行位置** | task_system |
| **是否使用 Worker Agent** | ✓（Evidence Assembly Agent 组装 + Completeness Checker 验证 + Evidence Critic Agent 质询） |
| **是否需要 Independent Verifier** | ✓（DCS 校验 manifest/迁移/就绪/rollout 证据及权限 + 具名 TR6 评委签署） |
| **所需模型能力** | STRUCTURED_DRAFTING + COUNTERARGUMENT_GENERATION + SAFETY_REVIEW |
| **所需 Skill** | ipd-release-tr6 / spk-evidence-assembler / spk-completeness-checker / spk-evidence-critic |
| **所需 Tool/MCP** | mcp-spk-os-artifact-query / mcp-filesystem-read |
| **所需 Knowledge** | KN-IPD-TR6-CHECKLIST-066 / KN-GAO-INDEPENDENT-VERIFY-035 |
| **所需 Information** | INFO-RELEASE-MANIFEST + INFO-MIGRATION-VERIFICATION + INFO-OPERATIONAL-READINESS + INFO-PROGRESSIVE-ROLLOUT |
| **可读取的 Memory 范围** | project:spk-os-ipd, agent_role:release, capability:structured_drafting |
| **人工责任** | 具名 TR6 评委签署（**AI 无投票/签署权**） |
| **失败处理** | 取件失败按 digest 重试；缺签名即 not_ready；法定人数/证据冲突升级 Chair/PQA；否决时回滚到 review_pending 和前版包 |
| **对应 Flowable 节点** | task_tr6_assembly + task_tr6_review (ipd-release.bpmn) |
| **证据来源** | RunReceipt + TR6DecisionPack + ArtifactManifest |

### 资源与工具详情

#### 工具调用顺序与效率策略

```
Step 1: Evidence Assembly Agent 组装证据包
├─ 调用 spk-evidence-assembler.assemble(manifest, migration, readiness, rollout)
│  ├─ 从 Manifest Store 拉取已签署 Manifest
│  ├─ 从 Migration Store 拉取 MigrationVerificationPack
│  ├─ 从 Readiness Store 拉取 OperationalReadinessRecord
│  ├─ 从 Rollout Store 拉取 ProgressiveRolloutRecord
│  ├─ 从各阶段归档拉取 TR1-TR5 报告
│  ├─ 从 DCP Store 拉取 CDCP/PDCP/ADCP 记录
│  ├─ 从 Security 拉取安全审计报告
│  ├─ 计算每个证据的 hash
│  ├─ 生成 TR6 证据包 manifest
│  └─ 输出：TR6EvidencePack（草稿）
└─ 输出：TR6EvidencePack（草稿）

Step 2: Completeness Checker 验证完整性
├─ 调用 spk-completeness-checker.check(tr6_pack, tr6_checklist)
│  ├─ 对照 TR6 checklist 逐项验证：
│  │  □ Manifest 已签署且完整
│  │  □ 迁移验证通过（含回滚演练）
│  │  □ 运营就绪验证通过（含故障演练）
│  │  □ 渐进发布完成（100% 流量或回滚）
│  │  □ TR1-TR5 报告全部已签署
│  │  □ DCP 记录全部已签署
│  │  □ 安全审计报告已签署
│  │  □ 所有 hash 签名有效
│  │  □ 无过期豁免
│  ├─ 交叉引用一致性校验：
│  │  □ Manifest 中的 digest 与 TR5 验证制品一致
│  │  □ Manifest 中的 digest 与 rollout 部署的 digest 一致
│  │  □ 所有版本号一致
│  │  □ 所有签名有效
│  ├─ 输出：完整性报告（缺失项高亮）
│  └─ 签名：sha256(...)
└─ 输出：CompletenessReport

Step 3: Evidence Critic Agent 独立质询
├─ 使用不同模型族（与所有作者 Agent 隔离）
├─ 审查证据包完整性：
│  ├─ 追溯链是否完整（需求→代码→测试→证据→发布）
│  ├─ 证据是否可重放（版本信息完整）
│  ├─ 例外是否合理（豁免有补偿控制）
│  ├─ 风险是否已处置（残余风险可接受）
│  ├─ 安全证据是否充分
│  ├─ 迁移是否可回滚或前向修复
│  ├─ 运营就绪是否覆盖所有责任域
│  └─ 渐进发布是否按策略执行
├─ 生成质询报告（问题列表 + 证据引用）
├─ **不投票，不批准，只质询**
└─ 输出：IndependentReviewReport

Step 4: DCS 校验
├─ 调用 DCS POST /api/v1/tr6-validate
│  ├─ 检查：
│  │  □ 评委权限有效
│  │  □ 法定人数满足
│  │  □ Evidence Critic 与作者 Agent 使用不同模型族
│  │  □ 所有证据包组件的 hash 有效
│  │  □ 无过期豁免
│  ├─ 输出：DCS 校验报告 + 签名
│  └─ 签名：sha256(...)
└─ 输出：DCS 校验报告

Step 5: TR6 评审委员会评审
├─ yudao/Flowable 触发 TR6 评审会议
├─ 分配评审任务给评审委员
├─ 提供证据包链接和摘要
├─ 评委审查：
│  ├─ 证据包完整性报告
│  ├─ Evidence Critic 质询报告
│  ├─ DCS 校验报告
│  └─ 原始证据（按需查阅）
├─ 对质询问题逐一答复
├─ 评委投票：
│  ├─ Go：通过，进入 GA 决策
│  ├─ Conditional Go：通过，但有附加条件
│  └─ No-Go：不通过，返回修复
├─ **AI 无投票权**
├─ **系统层面禁止 AI 写入 TR6 状态**
└─ 输出：TR6DecisionPack（已签署）
```

### 输出 Artifact 模板

#### Artifact 名称
`TR6DecisionPack`

#### TR6DecisionPack Schema

```json
{
  "pack_id": "tr6-20260912-001",
  "version": "1.0.0",
  "created_at": "2026-09-12T10:00:00Z",
  "evidence_index": {
    "manifest": {"id": "rel-20260910-001", "hash": "sha256:...", "signed": true},
    "migration": {"id": "mvp-20260910-001", "hash": "sha256:...", "signed": true},
    "readiness": {"id": "orr-20260910-001", "hash": "sha256:...", "signed": true},
    "rollout": {"id": "prr-20260911-001", "hash": "sha256:...", "signed": true},
    "tr_reports": [
      {"id": "TR1-2026-001", "hash": "sha256:...", "signed": true},
      {"id": "TR2-2026-001", "hash": "sha256:...", "signed": true},
      {"id": "TR3-2026-001", "hash": "sha256:...", "signed": true},
      {"id": "TR4-2026-001", "hash": "sha256:...", "signed": true},
      {"id": "TR5-2026-001", "hash": "sha256:...", "signed": true}
    ],
    "dcp_records": [
      {"id": "CDCP-2026-001", "hash": "sha256:...", "signed": true},
      {"id": "PDCP-2026-001", "hash": "sha256:...", "signed": true},
      {"id": "ADCP-2026-001", "hash": "sha256:...", "signed": true}
    ],
    "security_audit": {"id": "SA-2026-003", "hash": "sha256:...", "signed": true}
  },
  "completeness_report": {
    "checklist_coverage_pct": 100,
    "missing_items": [],
    "cross_reference_valid": true,
    "dcs_signature": "sha256:..."
  },
  "independent_review": {
    "agent_id": "agent-evidence-critic-001",
    "model_id": "gpt-5",
    "model_family": "openai",
    "isolated_from_authors": true,
    "challenges": [
      {
        "challenge_id": "CH-001",
        "description": "渐进发布 Cohort 3 的 error_rate 略升（0.0014 vs 0.0012），虽然 p=0.18 不显著，但需要确认是否有趋势性退化",
        "evidence_ref": "prr-20260911-001/cohort-3",
        "severity": "minor"
      }
    ],
    "report_hash": "sha256:..."
  },
  "challenge_responses": [
    {
      "challenge_id": "CH-001",
      "response": "Cohort 4 的 error_rate 回落到 0.0012，确认 Cohort 3 的略升为偶发抖刺，非趋势性退化",
      "supplementary_evidence": "prr-20260911-001/cohort-4",
      "accepted": true
    }
  ],
  "votes": [
    {
      "user_id": "user-tr6-chair-001",
      "role": "TR6 Chair",
      "vote": "go",
      "rationale": "所有证据完整，质询已答复，渐进发布成功",
      "voted_at": "2026-09-12T14:00:00Z"
    },
    {
      "user_id": "user-tr6-member-001",
      "role": "TR6 Member (Engineering)",
      "vote": "go",
      "rationale": "技术证据充分，迁移和回滚已演练",
      "voted_at": "2026-09-12T14:15:00Z"
    },
    {
      "user_id": "user-tr6-member-002",
      "role": "TR6 Member (Operations)",
      "vote": "go",
      "rationale": "运营就绪验证通过，故障演练成功",
      "voted_at": "2026-09-12T14:30:00Z"
    },
    {
      "user_id": "user-tr6-member-003",
      "role": "TR6 Member (Security)",
      "vote": "go",
      "rationale": "安全审计通过，无 Critical/High 未解决",
      "voted_at": "2026-09-12T14:45:00Z"
    },
    {
      "user_id": "user-tr6-member-004",
      "role": "TR6 Member (Product)",
      "vote": "go",
      "rationale": "产品功能验证通过，客户支持材料就绪",
      "voted_at": "2026-09-12T15:00:00Z"
    }
  ],
  "decision": "go",
  "conditions": [],
  "dissent": [],
  "dcs_receipt": {
    "solver_version": "tr6-validate-v1",
    "input_hash": "sha256:...",
    "signature": "sha256:..."
  }
}
```

---
