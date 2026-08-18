## ACT-03-07-10 Knowledge Archival & Postmortem

### 基础信息

| 字段 | 内容 |
|---|---|
| **Activity ID** | ACT-03-07-10 |
| **Activity 名称** | Knowledge Archival & Postmortem（知识归档与复盘） |
| **所属阶段** | 生命周期管理 |
| **Lead Agent 角色** | Lifecycle Orchestrator |
| **执行位置** | task_system |
| **是否使用 Worker Agent** | ✓（Knowledge Archiver 知识提取/格式化/索引 + Archival Validator 验证完整性） |
| **是否需要 Independent Verifier** | ✓（DCS 校验保留期/hash/脱敏/引用 + LMT/PQA 批准归档/行动） |
| **所需模型能力** | STRUCTURED_DRAFTING + CROSS_SOURCE_REASONING |
| **所需 Skill** | ipd-lifecycle-archival / spk-knowledge-archiver / spk-postmortem-facilitator |
| **所需 Tool/MCP** | mcp-spk-os-artifact-query / mcp-filesystem-read / mcp-agentmemory-write |
| **所需 Knowledge** | KN-IPD-ARCHIVAL-SCHEMA-088 / KN-GOOGLE-POSTMORTEM-089 |
| **所需 Information** | 全生命周期数据（信号/聚类/漏洞/维护/价值评审/需求回流/EOL/LDCP/迁移） |
| **可读取的 Memory 范围** | project:spk-os-ipd, agent_role:lifecycle, capability:structured_drafting |
| **人工责任** | LMT Lead 审核归档、签署 Postmortem；Knowledge Steward 审核知识晋级；PQA 批准归档发布 |
| **失败处理** | 索引失败按 digest 重建；保留策略冲突停止删除；隐私/legal hold 异常升级 Legal/PQA；人工否决时回滚归档发布并恢复 hold |
| **对应 Flowable 节点** | task_knowledge_archival (ipd-lifecycle.bpmn) |
| **证据来源** | RunReceipt + LifecycleArchiveAndPostmortem + ArtifactManifest |

### 资源与工具详情

#### 工具调用顺序与效率策略

```
Step 1: 冻结最终事件
├─ 收集全生命周期的关键事件：
│  ├─ GA 发布日期和稳定观察结果
│  ├─ 所有维护列车记录
│  ├─ 所有季度价值评审
│  ├─ 所有漏洞处置记录
│  ├─ 所有反馈聚类和需求回流
│  ├─ EOL 评估和 LDCP 决策
│  ├─ 客户迁移记录
│  └─ 所有事故和复盘
├─ 计算整体事件线 hash
└─ 输出：冻结事件线

Step 2: 分类证据/决策/客户义务
├─ Knowledge Archiver 分类：
│  ├─ 经验教训（做得好的 + 可以改进的）
│  ├─ 架构决策（ADR 记录）
│  ├─ CBB 使用记录（使用的 CBB 和适配经验）
│  ├─ 缺陷模式（高频缺陷根因和修复方式）
│  ├─ 客户反馈模式（高频需求和痛点）
│  ├─ 安全事件模式（漏洞类型和处置经验）
│  ├─ 迁移经验（客户迁移的成功和失败模式）
│  └─ 决策记录（季度评审和 LDCP 的关键决策）
└─ 输出：分类知识集合

Step 3: 脱敏与保留策略检查
├─ DCS 校验：
│  ├─ 调用 DCS POST /api/v1/archival-validate
│  │  ├─ 检查：
│  │  │  □ 客户 PII 已脱敏
│  │  │  □ 敏感商业数据已脱敏
│  │  │  □ 保留策略已定义（每类知识的保留期）
│  │  │  □ legal hold 已检查（无待决诉讼涉及的数据）
│  │  │  □ 删除请求已检查（无 GDPR 删除请求涉及的数据）
│  │  │  □ 归档内容 hash 有效
│  │  │  □ 引用链完整（每条知识可追溯到原始事件）
│  │  ├─ 输出：校验报告 + 签名
│  │  └─ 签名：sha256(...)
│  └─ 保留策略冲突 → 停止删除，升级 Legal
└─ 输出：DCS 校验报告

Step 4: 无责复盘
├─ 调用 spk-postmortem-facilitator.facilitate(lifecycle_events)
│  ├─ 按 Google SRE 无责复盘方法论：
│  │  ├─ 时间线：关键事件时间线
│  │  ├─ 影响：对客户、业务、团队的影响
│  │  ├─ 根因：系统性原因（不归责个人）
│  │  ├─ 做得好的：哪些决策和实践值得保留
│  │  ├─ 可以改进的：哪些地方可以做得更好
│  │  ├─ 行动项：具体改进措施（含 Owner 和期限）
│  │  └─ 知识晋级候选：哪些经验可以成为组织知识
│  └─ 输出：Postmortem 报告草稿
├─ LMT Lead 主持复盘会议
├─ 团队审查和补充
├─ 分配行动项（含 Owner 和期限）
└─ 输出：Postmortem 报告（已签署）

Step 5: 签名归档
├─ 生成归档 manifest：
│  ├─ 不可变 manifest（含所有归档内容的 hash）
│  ├─ 保留/删除规则
│  ├─ 事件线
│  ├─ 因果假设/反例
│  ├─ 行动 Owner/期限
│  └─ 签名
├─ LMT Lead 签署归档
├─ Knowledge Steward 审核知识晋级
├─ PQA 批准归档发布
├─ 设置复查日期
└─ 输出：LifecycleArchiveAndPostmortem（已签署）

Step 6: 知识晋级
├─ 经 Knowledge Steward 审核的知识晋级：
│  ├─ 跨 3 次发布复现的处置模式 → Git Knowledge
│  ├─ 具备权限模型、测试和回滚的步骤 → Skill
│  ├─ 客户敏感内容 → 不晋级
│  ├─ 临时异常解释 → 不晋级
│  └─ 错误记忆标 superseded，不删除
├─ 写入 AgentMemory（Lesson scope）
├─ 关闭生命周期实例
├─ 链接 Git 资产与记忆（不复制状态）
└─ 输出：知识晋级记录
```

### 输出 Artifact 模板

#### Artifact 名称
`LifecycleArchiveAndPostmortem`

#### LifecycleArchiveAndPostmortem Schema

```json
{
  "archive_id": "lap-20280115-001",
  "version": "1.0.0",
  "product_id": "aurora",
  "product_scope": "v1.x",
  "lifecycle_period": {
    "ga_date": "2026-09-12",
    "eol_date": "2028-01-01",
    "duration_months": 16
  },
  "created_at": "2028-01-15T10:00:00Z",
  "signed_by": [
    {"user_id": "user-lmt-lead-001", "role": "LMT Lead", "signed_at": "2028-01-15T14:00:00Z"},
    {"user_id": "user-knowledge-steward-001", "role": "Knowledge Steward", "signed_at": "2028-01-15T15:00:00Z"},
    {"user_id": "user-pqa-001", "role": "PQA", "signed_at": "2028-01-15T16:00:00Z"}
  ],
  "archive_manifest": {
    "total_items": 45,
    "categories": {
      "lessons_learned": 12,
      "architecture_decisions": 8,
      "cbb_usage_records": 5,
      "defect_patterns": 7,
      "customer_feedback_patterns": 6,
      "security_incident_patterns": 3,
      "migration_experiences": 4
    },
    "manifest_hash": "sha256:...",
    "retention_rules": [
      {"category": "lessons_learned", "retention_years": 5},
      {"category": "architecture_decisions", "retention_years": 10},
      {"category": "cbb_usage_records", "retention_years": 10},
      {"category": "defect_patterns", "retention_years": 3},
      {"category": "customer_feedback_patterns", "retention_years": 3},
      {"category": "security_incident_patterns", "retention_years": 7},
      {"category": "migration_experiences", "retention_years": 5}
    ]
  },
  "postmortem": {
    "timeline": [
      {"date": "2026-09-12", "event": "GA 发布 v1.0"},
      {"date": "2026-10-15", "event": "v1.1 维护列车（3 个安全补丁）"},
      {"date": "2027-01-15", "event": "Q4 价值评审：评分 68/100，maintain"},
      {"date": "2027-04-15", "event": "Q1 价值评审：评分 55/100，harvest_freeze"},
      {"date": "2027-07-15", "event": "Q2 价值评审：评分 42/100，启动 EOL 评估"},
      {"date": "2027-10-01", "event": "LDCP 签署退市决定"},
      {"date": "2027-10-15", "event": "客户迁移启动"},
      {"date": "2028-01-01", "event": "EOL，停止支持"}
    ],
    "impact_summary": {
      "total_customers_served": 120,
      "peak_mrr_usd": 125000,
      "total_revenue_usd": 1800000,
      "total_maintenance_cost_usd": 240000,
      "security_incidents": 2,
      "p0_incidents": 0
    },
    "lessons_learned": {
      "done_well": [
        "渐进发布策略有效控制了变更风险",
        "SBOM/VEX 关联使漏洞响应时间缩短 60%",
        "维护列车双轨制平衡了安全和稳定性"
      ],
      "could_improve": [
        "大仓库性能问题应在 v1.0 验证阶段更充分测试",
        "客户迁移通知期应更早启动（建议 120 天）",
        "季度价值评分模型需要更多历史数据校准"
      ]
    },
    "key_decisions": [
      {
        "decision": "v1.3 紧急补丁（tree-sitter CVE）",
        "date": "2026-11-15",
        "outcome": "24 小时内修复，0 客户受影响",
        "reflection": "紧急列车流程高效，但回归测试可以更充分"
      },
      {
        "decision": "LDCP 退市决定",
        "date": "2027-10-01",
        "outcome": "84% 客户成功迁移，2 个客户合同自然结束",
        "reflection": "迁移就绪度评估准确，但 5 个客户需要额外支持"
      }
    ],
    "action_items": [
      {
        "action": "将大仓库性能测试加入验证阶段标准测试套件",
        "owner": "user-qa-lead-001",
        "deadline": "2028-03-01",
        "status": "accepted"
      },
      {
        "action": "将客户迁移通知期标准从 90 天调整为 120 天",
        "owner": "user-lmt-lead-001",
        "deadline": "2028-02-01",
        "status": "accepted"
      }
    ]
  },
  "knowledge_promotion": {
    "promoted_to_knowledge": [
      {"type": "lesson", "title": "紧急补丁列车 72h SLA 执行模式", "git_ref": "knowledge://lessons/emergency-patch-train"},
      {"type": "skill", "title": "SBOM-VEX 关联操作手册", "skill_id": "spk-sbom-vex-correlation"}
    ],
    "promoted_to_agentmemory": [
      {"scope": "Lesson", "content": "大仓库性能问题应在验证阶段充分测试，不能依赖生产环境发现"},
      {"scope": "Lesson", "content": "客户迁移 1v1 支持对企业客户至关重要，标准流程不够"}
    ]
  },
  "dcs_receipt": {
    "solver_version": "archival-validate-v1",
    "input_hash": "sha256:...",
    "signature": "sha256:..."
  }
}
```

---

## 生命周期管理阶段 Activity 汇总

| Activity ID | 名称 | Lead Agent | Worker | Verifier | 模型能力 |
|---|---|---|---|---|---|
| ACT-03-07-01 | Lifecycle Signal Ingestion | Lifecycle Orchestrator | ✓(Ingestion+Router) | ✓(DCS+Signal Owner) | GROUNDED_EXTRACTION + MULTILINGUAL_ANALYSIS |
| ACT-03-07-02 | Feedback Clustering & Analysis | Lifecycle Orchestrator | ✓(Feedback Analyst+Faithfulness) | ✓(DCS+Product Owner) | CROSS_SOURCE_REASONING + MULTILINGUAL_ANALYSIS |
| ACT-03-07-03 | Vulnerability-SBOM Correlation | Lifecycle Orchestrator | ✓(Vuln Analyst+SBOM Correlator) | ✓(DCS+Security Owner) | CROSS_SOURCE_REASONING + SAFETY_REVIEW |
| ACT-03-07-04 | Maintenance Train Management | Lifecycle Orchestrator | ✓(Train Planner+Regression Tester) | ✓(DCS+Release Manager) | STRUCTURED_DRAFTING + CROSS_SOURCE_REASONING |
| ACT-03-07-05 | Quarterly Value Review | Lifecycle Orchestrator | ✓(Value Analyst+Data Collector) | ✓(DCS+LMT/IPMT) | CROSS_SOURCE_REASONING + STRUCTURED_DRAFTING |
| ACT-03-07-06 | Next-Gen Requirement Reflux | Lifecycle Orchestrator | ✓(Reflux Agent+Provenance Tracker) | ✓(DCS+Product Owner) | CROSS_SOURCE_REASONING + STRUCTURED_DRAFTING |
| ACT-03-07-07 | EOL Assessment | Lifecycle Orchestrator | ✓(EOL Analyst+Migration Readiness) | ✓(DCS+LMT/Legal/Sales) | CROSS_SOURCE_REASONING + STRUCTURED_DRAFTING |
| ACT-03-07-08 | LDCP Decision Package | Lifecycle Orchestrator | ✓(Pack Agent+Checker+Critic) | ✓(DCS+IPMT评委) | STRUCTURED_DRAFTING + COUNTERARGUMENT_GENERATION |
| ACT-03-07-09 | Customer Migration Execution | Lifecycle Orchestrator | ✓(Migration Advisor+Tracker) | ✓(DCS+CS/Legal/客户) | STRUCTURED_DRAFTING + GROUNDED_EXTRACTION |
| ACT-03-07-10 | Knowledge Archival & Postmortem | Lifecycle Orchestrator | ✓(Archiver+Validator) | ✓(DCS+LMT/PQA) | STRUCTURED_DRAFTING + CROSS_SOURCE_REASONING |

---

## 生命周期管理阶段产物关联图

```
[发布阶段 GA + 稳定观察结束]
        ↓
ACT-03-07-01 Lifecycle Signal Ingestion（持续运行）
        ↓
LifecycleSignalSet
        ↓
┌──────────┬──────────┬──────────┬──────────┐
↓          ↓          ↓          ↓          ↓
ACT-07-02  ACT-07-03  ACT-07-05  ACT-07-07  ACT-07-04
反馈聚类   漏洞关联   季度评审   EOL评估    维护列车
ThemeSet   VulnRecord ValueReview EOLAssess  TrainPlan
    ↓          ↓          ↓          ↓          ↓
ACT-07-06  [紧急/季度   [投资方向   ACT-07-08  [补丁发布]
需求回流    补丁发布]    决策]      LDCP决策
RefluxSet                            ↓
    ↓                            ACT-07-09
[纳入下一代                      客户迁移
 概念阶段]                      MigrationLedger
                                   ↓
                               ACT-07-10
                               知识归档与复盘
                               ArchiveAndPostmortem
                                   ↓
                               [生命周期结束]
                               [经验回流到下一代概念阶段]
```

---

## 生命周期管理阶段关键约束总结

| 约束 | 说明 | 来源 |
|---|---|---|
| Signal 统一感知 | 异构运营信号统一为 LifecycleSignal Schema | R2 §1, ISO 55001 |
| 版本族谱 | 每个版本维护完整族谱图（parent/child/sibling） | R2 §1, ISO 12207 |
| 维护列车双轨制 | 紧急 72h + 季度 90d，均走 Release Manifest | R2 ADR-01, NIST SSDF |
| SBOM/VEX 持续关联 | 全版本 SBOM + VEX 签名链 | R2 AC-03, CISA |
| 反馈忠实度 ≥0.95 | 每个 cluster 保留原文引用和反例 | R2 AC-02, Anthropic |
| Provenance 完整 | W3C PROV-O 兼容，从工单到需求候选 | R2 AC-08, W3C PROV-O |
| 四维价值评分 | 财务/客户/技术/战略，DCS 可复算 | R2 §9, ISO 55001 |
| LDCP AI 无投票权 | 系统层面禁止 AI 写入 LDCP 状态 | R2 AC-07, OECD AI |
| 退市通知 ≥90 天 | 取 max(90 天, 合同约定) | R2 ADR-02, ISO 20000 |
| 客户迁移逐户跟踪 | 每客户有明确状态，AI 不代客户同意 | R2 §9, ISO 20000 |
| 无责复盘 | Google SRE 方法论，不归责个人 | R2 §9, Google SRE |
| 知识晋级审核 | 经 Knowledge Steward 审核后才入 Git Knowledge | R2 §26, ISO 12207 |
| 新产品定性评审 | <2 年产品允许定性评审替代量化评分 | R2 ADR-04, ISO 55001 |
| 失败回退 | 所有 Activity 可回退到纯人工路径 ≤30 分钟 | R2 AC-10 |
| CLOSED 不可重开 | 退市关闭不可重开，遗漏创建新纠正实例 | R1 §9.2, R4 §3 |
