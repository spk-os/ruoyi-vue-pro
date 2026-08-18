## ACT-03-07-08 LDCP Decision Package

### 基础信息

| 字段 | 内容 |
|---|---|
| **Activity ID** | ACT-03-07-08 |
| **Activity 名称** | LDCP Decision Package（生命周期决策检查点决策包） |
| **所属阶段** | 生命周期管理 |
| **Lead Agent 角色** | Lifecycle Orchestrator |
| **执行位置** | task_system |
| **是否使用 Worker Agent** | ✓（Decision Pack Agent 组装 + Completeness Checker 验证 + Retirement Critic 独立质询） |
| **是否需要 Independent Verifier** | ✓（DCS 校验证据/权限/法定人数 + 具名 IPMT 评委签署） |
| **所需模型能力** | STRUCTURED_DRAFTING + COUNTERARGUMENT_GENERATION |
| **所需 Skill** | ipd-lifecycle-ldcp / spk-ldcp-pack-agent / spk-retirement-critic |
| **所需 Tool/MCP** | mcp-spk-os-artifact-query / mcp-filesystem-read |
| **所需 Knowledge** | KN-IPD-LDCP-SCHEMA-085 / KN-IPD-LDCP-STATE-MACHINE-086 |
| **所需 Information** | INFO-EOL-ASSESSMENT（退市评估）/ INFO-MIGRATION-PLAN（迁移方案）/ INFO-KNOWLEDGE-ARCHIVAL-PLAN（知识归档计划） |
| **可读取的 Memory 范围** | project:spk-os-ipd, agent_role:lifecycle, capability:structured_drafting |
| **人工责任** | 具名 IPMT 评委签署 LDCP 决定（**AI 无投票权，系统层面禁止 AI 写入 LDCP 状态**） |
| **失败处理** | 取件失败按 digest 重试；缺关键义务即 not_ready；法定人数/利益冲突升级 Chair/PQA；无有效签名回滚 review_pending |
| **对应 Flowable 节点** | task_ldcp_decision (ipd-lifecycle.bpmn) |
| **证据来源** | RunReceipt + LDCPDecisionRecord + ArtifactManifest |

### 资源与工具详情

#### LifecycleDecision 状态机

```
LifecycleDecision.option 只允许：
  invest_grow | maintain | harvest_freeze | replace_migrate | retire

决定记录状态流转：
  draft → evidence_frozen → awaiting_human → signed → effective → superseded|expired|closed

产品状态（由 Flowable 管理）：
  MAINTAINING → RETIREMENT_PROPOSED → LDCP_HUMAN → MIGRATING → CLOSED

确定性门禁止：
  □ draft/evidence_frozen → effective（跳过人签）
  □ 无 LDCP 人签进入 MIGRATING/CLOSED
  □ 条件过期仍 effective
  □ harvest_freeze 自动停止安全支持
  □ MIGRATING → MAINTAINING 无签名回滚
  □ CLOSED → 任意活动状态（不可重开）
```

#### 工具调用顺序与效率策略

```
Step 1: 冻结证据
├─ 冻结 EOL 评估报告 hash
├─ 冻结迁移方案 hash
├─ 冻结知识归档计划 hash
├─ 冻结季度价值评审 hash
├─ 计算整体证据包 hash
└─ 输出：冻结证据包

Step 2: Decision Pack Agent 组装决策包
├─ 汇总所有证据：
│  ├─ EOL 评估报告（三情景分析）
│  ├─ 迁移方案（客户分群、通知计划、数据处置）
│  ├─ 知识归档计划（经验教训、架构决策、CBB 使用）
│  ├─ 季度价值评审（最近 4 个季度）
│  ├─ 财务影响分析（退市 vs 继续的 NPV 对比）
│  └─ 风险台账（未关闭风险和豁免）
├─ 生成决策包摘要
└─ 输出：LDCP 决策包草稿

Step 3: Completeness Checker 验证
├─ 对照 LDCP checklist 逐项验证：
│  □ EOL 评估报告完整（三情景 + 财务区间）
│  □ 迁移方案完整（8 字段全覆盖）
│  □ 知识归档计划完整
│  □ 所有证据 hash 有效
│  □ 客户影响分析完整（逐客户清单）
│  □ 法律意见已签署
│  □ 安全意见已签署
│  □ 通知义务已核对
└─ 输出：完整性报告

Step 4: Retirement Critic 独立质询
├─ 使用不同模型族（与所有作者 Agent 隔离）
├─ 审查决策包：
│  ├─ 退市理由是否充分
│  ├─ 迁移方案是否可行
│  ├─ 客户影响是否被低估
│  ├─ 法律风险是否被遗漏
│  ├─ 知识归档是否完整
│  └─ 反事实分析（如果不退市会怎样）
├─ 生成质询报告
├─ **不投票，不批准，只质询**
└─ 输出：IndependentReviewReport

Step 5: IPMT 评委签署
├─ yudao/Flowable 触发 LDCP 评审会议
├─ 检查委员授权、法定人数和利益冲突
├─ 评委审查决策包 + Critic 报告
├─ 对质询问题逐一答复
├─ 评委从 5 个选项中选择一项：
│  ├─ invest_grow：继续投资，启动下一代开发
│  ├─ maintain：维持现状，下季度再评审
│  ├─ harvest_freeze：冻结新功能，仅安全补丁
│  ├─ replace_migrate：启动客户迁移到替代产品
│  └─ retire：正式退市
├─ 签署条件、有效期、reopen 触发器和 rollback target
├─ **AI 无投票权**
├─ **系统层面禁止 AI 写入 LDCP 状态**
└─ 输出：LDCPDecisionRecord（已签署）
```

### 输出 Artifact 模板

#### Artifact 名称
`LDCPDecisionRecord`

#### LDCPDecisionRecord Schema

```json
{
  "decision_id": "ldcp-20270201-001",
  "version": "1.0.0",
  "product_id": "aurora",
  "product_scope": "v1.x",
  "option": "retire",
  "status": "signed",
  "evidence_pack_digest": "sha256:...",
  "trigger_evidence": {
    "value_score_trend": "declining_3_quarters",
    "eol_assessment_ref": "eol-20270115-001",
    "dependency_eol_count": 2,
    "customer_migration_readiness": "ready"
  },
  "entry_checks": {
    "eol_assessment_complete": true,
    "migration_plan_complete": true,
    "legal_opinion_signed": true,
    "security_opinion_signed": true,
    "notification_obligations_met": true
  },
  "exit_checks": {
    "all_customers_migrated": "pending",
    "data_disposition_complete": "pending",
    "knowledge_archival_complete": "pending"
  },
  "conditions": [
    {
      "condition_id": "COND-LDCP-001",
      "description": "EOS (End of Sales) 日期：2027-04-01",
      "owner": "user-sales-lead-001",
      "valid_from": "2027-02-01",
      "valid_until": "2027-04-01"
    },
    {
      "condition_id": "COND-LDCP-002",
      "description": "EoS (End of Support) 日期：2027-10-01",
      "owner": "user-lmt-lead-001",
      "valid_from": "2027-04-01",
      "valid_until": "2027-10-01"
    },
    {
      "condition_id": "COND-LDCP-003",
      "description": "EOL (End of Life) 日期：2028-01-01",
      "owner": "user-lmt-lead-001",
      "valid_from": "2027-10-01",
      "valid_until": "2028-01-01"
    }
  ],
  "votes": [
    {
      "user_id": "user-ipmt-chair-001",
      "role": "IPMT Chair",
      "vote": "retire",
      "rationale": "价值评分连续 3 季度下降，迁移就绪度高，退市 NPV 优于继续",
      "voted_at": "2027-02-01T14:00:00Z"
    },
    {
      "user_id": "user-ipmt-member-001",
      "role": "IPMT Member (Finance)",
      "vote": "retire",
      "rationale": "维护成本上升，收入下降，退市经济性优于继续",
      "voted_at": "2027-02-01T14:15:00Z"
    },
    {
      "user_id": "user-ipmt-member-002",
      "role": "IPMT Member (Product)",
      "vote": "retire",
      "rationale": "v2.x 已覆盖 92% 功能，迁移工具已验证",
      "voted_at": "2027-02-01T14:30:00Z"
    }
  ],
  "dissent": [],
  "coi_attestations": [
    {"user_id": "user-ipmt-chair-001", "coi_declared": false},
    {"user_id": "user-ipmt-member-001", "coi_declared": false},
    {"user_id": "user-ipmt-member-002", "coi_declared": false}
  ],
  "reopen_triggers": [
    "迁移完成率 < 80% 且距 EoS < 60 天",
    "关键客户合同延期导致无法按时退市"
  ],
  "rollback_target": "maintain",
  "supersedes": null,
  "flowable_task_id": "task-ldcp-20270201-001",
  "dcs_receipt": {
    "solver_version": "ldcp-validate-v1",
    "input_hash": "sha256:...",
    "signature": "sha256:..."
  }
}
```

---
