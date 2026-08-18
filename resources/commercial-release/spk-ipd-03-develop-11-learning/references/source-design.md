## ACT-03-04-11 经验回流

### 基础信息

| 字段 | 内容 |
|---|---|
| **Activity ID** | ACT-03-04-11 |
| **Activity 名称** | 经验回流 |
| **所属阶段** | 开发 |
| **Lead Agent 角色** | Development Orchestrator |
| **执行位置** | task_system |
| **是否使用 Worker Agent** | ✓（Learning Agent 聚类证据化经验） |
| **是否需要 Independent Verifier** | — |
| **所需模型能力** | CROSS_SOURCE_REASONING + STRUCTURED_DRAFTING |
| **所需 Skill** | ipd-dev-learning / spk-retrospective-analyzer |
| **所需 Tool/MCP** | mcp-spk-os-artifact-query / mcp-agentmemory-write |
| **所需 Knowledge** | KN-IPD-LEARNING-SCHEMA-036 |
| **所需 Information** | 所有开发包的证据和决策记录 |
| **可读取的 Memory 范围** | project:spk-os-ipd, agent_role:development, capability:cross_source_reasoning |
| **人工责任** | PDT Retro Owner 接受/拒绝改进项 |
| **失败处理** | 聚类质量差更换特征重试；仍失败人工主题化；隐私/归责风险升级 PQA/HR；被否决项回滚为草稿并撤销知识链接 |
| **对应 Flowable 节点** | task_learning (ipd-development.bpmn) |
| **证据来源** | RunReceipt + DevelopmentLearningSet + ArtifactManifest |

### 资源与工具详情

#### 工具调用顺序与效率策略

```
Step 1: 收集决策/失败/返工数据
├─ 从所有开发包中收集：
│  ├─ CI 门禁失败记录和修复方法
│  ├─ 代码审查中发现的问题模式
│  ├─ CCB 变更决策和理由
│  ├─ 测试中发现的缺陷模式
│  ├─ 工期偏差（预估 vs 实际）
│  └─ AI 辅助效果数据（生产率校准）
└─ 输出：原始经验数据

Step 2: Learning Agent 聚类与因果分析
├─ 调用 spk-retrospective-analyzer.analyze(raw_data)
│  ├─ 聚类相似事件（按类型/原因/影响）
│  ├─ 抽取因果假设（不是相关性）
│  ├─ 标注适用边界（什么条件下成立）
│  ├─ 标注反例（什么条件下不成立）
│  ├─ 生成改进建议（含 owner 和期限）
│  └─ 输出：经验分析草稿
├─ DCS 去标识和查重
│  ├─ 移除个人身份信息
│  ├─ 检查与已有经验是否重复
│  └─ 输出：去标识后的经验集
└─ 输出：DevelopmentLearningSet（草稿）

Step 3: 团队复盘
├─ PDT Retro Owner 组织复盘会议
├─ 审查 Learning Agent 的分析
├─ 接受/拒绝/修改改进建议
├─ 分配 owner 和期限
└─ 输出：DevelopmentLearningSet（已签署）

Step 4: 入知识库
├─ 将接受的经验写入 AgentMemory（Lesson scope）
├─ 将可重复步骤注册为 Skill
├─ 设置有效期和复查日
└─ 输出：知识库更新
```

### 输出 Artifact 模板

#### Artifact 名称
`DevelopmentLearningSet`

#### DevelopmentLearningSet Schema

```json
{
  "learning_id": "dls-20260825-001",
  "version": "1.0.0",
  "created_at": "2026-08-25T17:00:00Z",
  "created_by": "agent-learning-001",
  "signed_by": {
    "user_id": "user-retro-owner-001",
    "role": "Retro Owner",
    "signed_at": "2026-08-26T10:00:00Z"
  },
  "dev_packets_covered": ["DP-001", "DP-002", "DP-003", "DP-004", "DP-005"],
  "learnings": [
    {
      "learning_id": "L-001",
      "category": "ci_failure_pattern",
      "event_evidence": ["ci-20260818-dp001-001", "ci-20260819-dp001-002"],
      "causal_hypothesis": "tree-sitter 对 Rust 语言的语法支持不完整，导致解析失败",
      "applicable_boundary": "仅影响 Rust 代码解析，其他语言不受影响",
      "counter_examples": ["Python/TypeScript/Java 解析均正常"],
      "action": {
        "description": "在 CI 配置中添加 Rust 语法支持的预检查",
        "owner": "user-devops-001",
        "deadline": "2026-09-15",
        "status": "accepted"
      },
      "confidence": 0.85,
      "review_date": "2026-11-25"
    },
    {
      "learning_id": "L-002",
      "category": "ai_productivity",
      "event_evidence": ["impl-20260818-dp001-001", "impl-20260819-dp002-001"],
      "causal_hypothesis": "AI 辅助在边界清晰的 CRUD 模块上提高效率 30%，但在复杂算法重构上效率降低 15%",
      "applicable_boundary": "仅适用于本项目的 TypeScript 代码，不外推到 Python/Go",
      "counter_examples": ["DP-002 的 ML 管道重构，AI 辅助反而增加了 15% 时间"],
      "action": {
        "description": "对简单模块优先使用 AI 辅助，复杂算法重构使用建议模式",
        "owner": "user-pdt-lead-001",
        "deadline": "2026-09-30",
        "status": "accepted"
      },
      "confidence": 0.70,
      "review_date": "2026-12-25"
    }
  ],
  "statistics": {
    "total_learnings": 8,
    "accepted": 6,
    "rejected": 1,
    "deferred": 1,
    "skills_created": 2
  }
}
```

---

## 开发阶段 Activity 汇总

| Activity ID | 名称 | Lead Agent | Worker | Verifier | 模型能力 |
|---|---|---|---|---|---|
| ACT-03-04-01 | 基线确认 | Dev Orchestrator | — | ✓(DCS+独立复核) | GROUNDED_EXTRACTION |
| ACT-03-04-02 | 开发包切片 | Dev Orchestrator | — | ✓(DCS图约束) | CROSS_SOURCE_REASONING + STRUCTURED_DRAFTING |
| ACT-03-04-03 | Context 构建 | Dev Orchestrator | — | ✓(DCS ACL) | GROUNDED_EXTRACTION |
| ACT-03-04-04 | 实现草案 | Dev Orchestrator | ✓(Impl+Test) | — | STRUCTURED_DRAFTING + CODE_OR_FORMULA_ASSISTANCE |
| ACT-03-04-05 | 单元/组件验证 | Dev Orchestrator | ✓(Test Agent) | ✓(确定性runner) | CODE_OR_FORMULA_ASSISTANCE + GROUNDED_EXTRACTION |
| ACT-03-04-06 | 同行审查 | Dev Orchestrator | ✓(Review Agent) | ✓(DCS独立性) | CROSS_SOURCE_REASONING + SAFETY_REVIEW |
| ACT-03-04-07 | 持续集成 | Dev Orchestrator | ✓(CI Insight) | ✓(确定性流水线) | GROUNDED_EXTRACTION |
| ACT-03-04-08 | 变更控制 | Dev Orchestrator | ✓(Impact Agent) | ✓(DCS图遍历) | CROSS_SOURCE_REASONING + STRUCTURED_DRAFTING |
| ACT-03-04-09 | 证据汇编 | Dev Orchestrator | ✓(Evidence Agent) | ✓(DCS+PQA) | STRUCTURED_DRAFTING + GROUNDED_EXTRACTION |
| ACT-03-04-10 | TR3/TR4 评审 | Dev Orchestrator | ✓(Independent Review) | ✓(DCS+评委) | CROSS_SOURCE_REASONING + COUNTERARGUMENT_GENERATION |
| ACT-03-04-11 | 经验回流 | Dev Orchestrator | ✓(Learning Agent) | — | CROSS_SOURCE_REASONING + STRUCTURED_DRAFTING |

---

## 开发阶段产物关联图

```
[计划阶段 PDCP Go]
        ↓
ACT-03-04-01 基线确认 → DevelopmentBaselineManifest
        ↓
ACT-03-04-02 开发包切片 → DevelopmentPackageSet
        ↓
[每个开发包并行执行]
        ↓
ACT-03-04-03 Context 构建 → ContextBundle
        ↓
ACT-03-04-04 实现草案 → ImplementationDraft (PR)
        ↓
ACT-03-04-05 单元/组件验证 → UnitComponentEvidence
        ↓
ACT-03-04-06 同行审查 → PeerReviewRecord
        ↓
ACT-03-04-07 持续集成 → CIRunEvidence
        ↓
[如有基线变更] → ACT-03-04-08 变更控制 → ChangeDecisionRecord
        ↓
[所有包完成后]
ACT-03-04-09 证据汇编 → DevelopmentEvidencePack
        ↓
ACT-03-04-10 TR3/TR4 评审 → TR3_TR4_DecisionRecord
        ↓
ACT-03-04-11 经验回流 → DevelopmentLearningSet
        ↓
[进入验证阶段]
```

---

## 开发阶段关键约束总结

| 约束 | 说明 | 来源 |
|---|---|---|
| 保护分支禁写 | Agent 只能建分支/PR，不能直接合并 | R1 DEV-R02, R2 AC-05 |
| CI 确定性门禁 | 每层门禁由程序按固定规则判定，AI 不能修改阈值 | R1 DEV-R04, R2 AC-03 |
| CCB AI 禁写 | 系统层面禁止 AI Agent 写入 CCB 状态字段 | R1 CL-009, R2 AC-04 |
| TR AI 无投票权 | Independent Review Agent 只质询不投票 | R2 §10, R4 §2 |
| 作者-审查者分离 | 非作者 Reviewer 批准，禁止自审 | R2 AC-05, ISO 12207 |
| 证据不可变 | SHA-256 签名，摘要不替代原证据 | R2 AC-07, NIST SSDF |
| Context 最小权限 | 六层 Context Pack，ACL 控制检索范围 | R1 §11, R2 §11 |
| 开发包粒度 | ≤2 人周，纵向切片，可独立验证 | R2 ADR-03, DORA |
| 失败回退 | 所有 Activity 可回退到纯人工路径 ≤30 分钟 | R2 AC-10 |
| Provenance 完整 | 代码/测试/文档/SBOM/构建证明同步版本化 | R1 DEV-R03, SLSA |
