## ACT-03-05-09 TR5 资格评审

### 基础信息

| 字段 | 内容 |
|---|---|
| **Activity ID** | ACT-03-05-09 |
| **Activity 名称** | TR5 资格评审 |
| **所属阶段** | 验证 |
| **Lead Agent 角色** | Verification Orchestrator |
| **执行位置** | task_system |
| **是否使用 Worker Agent** | ✓（Evidence Assembly Agent 汇编 + Evidence Critic Agent 独立质询） |
| **是否需要 Independent Verifier** | ✓（DCS 校验材料/权限/法定人数/签名 + Evidence Critic Agent） |
| **所需模型能力** | CROSS_SOURCE_REASONING + COUNTERARGUMENT_GENERATION + SAFETY_REVIEW |
| **所需 Skill** | ipd-verify-tr5-review / spk-evidence-assembler / spk-independent-reviewer |
| **所需 Tool/MCP** | mcp-spk-os-artifact-query / mcp-filesystem-read |
| **所需 Knowledge** | KN-IPD-TR5-CHECKLIST-054 / KN-GAO-INDEPENDENT-VERIFY-035 |
| **所需 Information** | 所有验证阶段的证据 Artifact |
| **可读取的 Memory 范围** | project:spk-os-ipd, agent_role:verification, capability:cross_source_reasoning |
| **人工责任** | 具名评委签署 go/conditional/no-go（**AI 无投票/签署权**） |
| **失败处理** | Critic 不可用降级人工独立审查；法定人数不足延期；关键风险争议升级 ADCP Chair/PQA；无有效签名回滚 review_pending |
| **对应 Flowable 节点** | task_tr5_review (ipd-qualification.bpmn) |
| **证据来源** | RunReceipt + TR5QualificationDecision + ArtifactManifest |

### 资源与工具详情

#### 工具调用顺序与效率策略

```
Step 1: Evidence Assembly Agent 汇编资格包
├─ 收集所有验证阶段的证据：
│  ├─ VerificationBaselineManifest（ACT-03-05-01）
│  ├─ RVM_RTM_Baseline（ACT-03-05-02）
│  ├─ MultiLayerTestDesign（ACT-03-05-03）
│  ├─ ReplayableTestEnvironment（ACT-03-05-04）
│  ├─ TestExecutionEvidence（ACT-03-05-05）
│  ├─ DefectClosureRecord（ACT-03-05-06）
│  ├─ UserValidationReport（ACT-03-05-07）
│  └─ ResidualRiskReport（ACT-03-05-08）
├─ 计算每个证据的 hash
├─ 生成资格包 manifest
├─ 签名资格包
└─ 输出：QualificationPackage

Step 2: DCS 完整性校验
├─ 调用 DCS POST /api/v1/qualification-validate
│  ├─ 检查：
│  │  □ 所有必填组件存在
│  │  □ 所有 hash 签名有效
│  │  □ 无过期豁免
│  │  □ 评委权限有效
│  │  □ 法定人数满足
│  │  □ Evidence Critic 与作者 Agent 使用不同模型族
│  ├─ 输出：完整性报告 + 签名
│  └─ 签名：sha256(...)
└─ 输出：DCS 完整性报告

Step 3: Evidence Critic Agent 独立质询
├─ 使用不同模型族（与所有作者 Agent 隔离）
├─ 不读取作者的隐藏推理
├─ 审查资格包完整性：
│  ├─ 追溯链是否完整（需求→代码→测试→证据）
│  ├─ 证据是否可重放（版本信息完整）
│  ├─ 例外是否合理（豁免有补偿控制）
│  ├─ 风险是否已处置（残余风险可接受）
│  ├─ 安全证据是否充分（OWASP Top 10 覆盖）
│  ├─ 用户验证是否代表性（样本覆盖关键画像）
│  └─ 环境差异是否披露（与生产差异已记录）
├─ 生成质询报告（问题列表 + 证据引用）
├─ **不投票，不批准，只质询**
└─ 输出：IndependentReviewReport

Step 4: 评委审查与质询答复
├─ 评委查看资格包 + Independent Review Report
├─ 对质询问题逐一答复
├─ 记录答复和补充证据
└─ 输出：质询答复记录

Step 5: 评委投票与签署
├─ 每个评委独立投票：
│  ├─ Go：通过，进入发布阶段
│  ├─ Conditional Go：通过，但有附加条件
│  └─ No-Go：不通过，需要修改后重新评审
├─ 记录投票理由和异议
├─ **AI 无投票权**
├─ **系统层面禁止 AI 推进 ADCP 状态**
└─ 输出：TR5QualificationDecision（已签署）

Step 6: 发布行动
├─ 如果 Go/Conditional Go：
│  ├─ 更新 Flowable 状态
│  ├─ 发布行动项（条件对应的后续工作）
│  └─ 进入发布阶段（ACT-03-06-*）
├─ 如果 No-Go：
│  ├─ 记录需要修改的内容
│  ├─ 回退到验证阶段
│  └─ 重新执行受影响的测试
└─ 输出：行动项列表
```

### 输出 Artifact 模板

#### Artifact 名称
`TR5QualificationDecision`

#### TR5QualificationDecision Schema

```json
{
  "decision_id": "tr5-20260907-001",
  "version": "1.0.0",
  "qualification_package_ref": "qp-20260907-001",
  "package_hash": "sha256:...",
  "independent_review": {
    "agent_id": "agent-evidence-critic-001",
    "model_id": "gpt-5",
    "model_family": "openai",
    "isolated_from_authors": true,
    "challenges": [
      {
        "challenge_id": "CH-001",
        "description": "残余风险报告中未包含模型 API 延迟超 SLA 时的降级路径测试证据",
        "evidence_ref": "rrr-20260906-001/R-002",
        "severity": "minor"
      },
      {
        "challenge_id": "CH-002",
        "description": "用户验证中 Python 开发者样本量不足（仅 3 人）",
        "evidence_ref": "uvr-20260905-001/SC-003",
        "severity": "minor"
      }
    ],
    "report_hash": "sha256:..."
  },
  "challenge_responses": [
    {
      "challenge_id": "CH-001",
      "response": "降级路径已在 TC-DEGRADE-001~003 中测试，补充证据到资格包",
      "supplementary_evidence": "tee-20260907-001",
      "accepted": true
    },
    {
      "challenge_id": "CH-002",
      "response": "Python 开发者样本量不足，将 Python 支持标记为 Beta 功能",
      "supplementary_evidence": "N/A",
      "accepted": true
    }
  ],
  "votes": [
    {
      "user_id": "user-tr5-chair-001",
      "role": "TR5 Chair",
      "vote": "conditional_go",
      "rationale": "核心功能验证充分，Python 支持标记为 Beta 可接受",
      "conditions": [
        "在发布文档中明确标注 Python 支持为 Beta 功能",
        "W-001（大型 monorepo 性能）到期前必须解决"
      ],
      "voted_at": "2026-09-07T15:00:00Z"
    },
    {
      "user_id": "user-tr5-member-001",
      "role": "TR5 Member (QA)",
      "vote": "go",
      "rationale": "测试覆盖充分，缺陷已收敛",
      "voted_at": "2026-09-07T15:15:00Z"
    },
    {
      "user_id": "user-tr5-member-002",
      "role": "TR5 Member (Security)",
      "vote": "go",
      "rationale": "安全测试覆盖 OWASP Top 10，无 Critical/High 未解决",
      "voted_at": "2026-09-07T15:30:00Z"
    },
    {
      "user_id": "user-tr5-member-003",
      "role": "TR5 Member (Product)",
      "vote": "go",
      "rationale": "用户满意度 85%，置信区间 [72%, 92%]，可接受",
      "voted_at": "2026-09-07T15:45:00Z"
    }
  ],
  "decision": "conditional_go",
  "conditions": [
    {
      "condition_id": "COND-001",
      "description": "在发布文档中明确标注 Python 支持为 Beta 功能",
      "owner": "user-product-001",
      "deadline": "2026-09-15"
    },
    {
      "condition_id": "COND-002",
      "description": "W-001（大型 monorepo 性能豁免）到期前必须解决",
      "owner": "user-dev-lead-001",
      "deadline": "2026-12-31"
    }
  ],
  "dissent": [],
  "dcs_receipt": {
    "solver_version": "qualification-validate-v1",
    "input_hash": "sha256:...",
    "signature": "sha256:..."
  }
}
```

---

## 验证阶段 Activity 汇总

| Activity ID | 名称 | Lead Agent | Worker | Verifier | 模型能力 |
|---|---|---|---|---|---|
| ACT-03-05-01 | 冻结验证基线 | Verify Orchestrator | — | ✓(DCS+独立复核) | GROUNDED_EXTRACTION |
| ACT-03-05-02 | 建立 RVM/RTM | Verify Orchestrator | ✓(Trace Agent) | ✓(DCS+SE) | CROSS_SOURCE_REASONING + STRUCTURED_DRAFTING |
| ACT-03-05-03 | 设计多层测试 | Verify Orchestrator | ✓(Test Designer+Critic) | ✓(DCS+Critic) | CROSS_SOURCE_REASONING + STRUCTURED_DRAFTING + COUNTERARGUMENT |
| ACT-03-05-04 | 准备环境与数据 | Verify Orchestrator | ✓(Env Agent) | ✓(DCS) | GROUNDED_EXTRACTION + STRUCTURED_DRAFTING |
| ACT-03-05-05 | 执行与采证 | Verify Orchestrator | ✓(Failure Insight) | ✓(确定性Executor) | GROUNDED_EXTRACTION |
| ACT-03-05-06 | 缺陷分诊闭环 | Verify Orchestrator | ✓(Defect Analyst) | ✓(DCS) | CROSS_SOURCE_REASONING + GROUNDED_EXTRACTION |
| ACT-03-05-07 | 用户场景验证 | Verify Orchestrator | ✓(VOC Agent) | ✓(DCS) | CROSS_SOURCE_REASONING + GROUNDED_EXTRACTION |
| ACT-03-05-08 | 计算残余风险 | Verify Orchestrator | ✓(Risk Narrative) | ✓(DCS) | CROSS_SOURCE_REASONING + STRUCTURED_DRAFTING |
| ACT-03-05-09 | TR5 资格评审 | Verify Orchestrator | ✓(Assembly+Critic) | ✓(DCS+评委) | CROSS_SOURCE_REASONING + COUNTERARGUMENT_GENERATION |

---

## 验证阶段产物关联图

```
[开发阶段 TR4 Go]
        ↓
ACT-03-05-01 冻结验证基线 → VerificationBaselineManifest
        ↓
ACT-03-05-02 建立 RVM/RTM → RVM_RTM_Baseline
        ↓
ACT-03-05-03 设计多层测试 → MultiLayerTestDesign
        ↓
ACT-03-05-04 准备环境与数据 → ReplayableTestEnvironment
        ↓
ACT-03-05-05 执行与采证 → TestExecutionEvidence
        ↓
ACT-03-05-06 缺陷分诊闭环 → DefectClosureRecord
        ↓
[如有缺陷修复，重新执行 05→06]
        ↓
ACT-03-05-07 用户场景验证 → UserValidationReport
        ↓
ACT-03-05-08 计算残余风险 → ResidualRiskReport
        ↓
ACT-03-05-09 TR5 资格评审 → TR5QualificationDecision
        ↓
[进入发布阶段]
```

---

## 验证阶段关键约束总结

| 约束 | 说明 | 来源 |
|---|---|---|
| V&V 严格区分 | Verification（按规定正确实现）≠ Validation（实现正确产品） | NASA SE Handbook, R1 CL-001 |
| AI 不能自证 | AI 生成的需求/实现/测试不能互相印证成独立证据 | NASA V&V 独立性, R1 CL-003 |
| Oracle 确定性 | 测试 Oracle 必须由规则或专家确定，禁止 ai_generated | ISTQB, R2 AC-04 |
| Evidence Critic 独立 | 使用不同模型族，隔离上下文，不投票只质询 | GAO 独立验证, R2 ADR-01 |
| TR5 AI 无投票权 | 系统层面禁止 AI 推进 ADCP 状态 | R2 AC-08, EU AI Act |
| 证据不可变 | SHA-256 签名，原始日志不可覆盖 | NIST SSDF, R2 AC-03 |
| Flaky 隔离 | flaky 率超阈值自动隔离，不以重跑通过覆盖 | Google Testing, R2 ADR-02 |
| 缺陷 AI 不关闭 | AI 聚类但不关闭缺陷，关闭者不同于修复作者 | R2 AC-05 |
| Beta 代表性 | 分层抽样，每层 ≥10 人，Wilson 95% 置信区间 | R2 ADR-03 |
| 豁免治理 | 含残余风险、到期日、补偿控制和具名批准 | R2 AC-06 |
| 失败回退 | 所有 Activity 可回退到纯人工路径 ≤30 分钟 | R2 AC-10 |
| 环境可重放 | 固定镜像/IaC/seed/数据，可销毁并重建 | R2 AC-03 |
