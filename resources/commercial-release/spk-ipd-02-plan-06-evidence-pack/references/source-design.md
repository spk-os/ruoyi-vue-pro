## ACT-03-03-06 TR2/PDCP 决策包组装

### 基础信息

| 字段 | 内容 |
|---|---|
| **Activity ID** | ACT-03-03-06 |
| **Activity 名称** | TR2/PDCP 决策包组装 |
| **所属阶段** | 计划 |
| **Lead Agent 角色** | Planning Lead |
| **执行位置** | task_system |
| **是否使用 Worker Agent** | — |
| **是否需要 Independent Verifier** | ✓（DCP-Verifier + TR-Verifier） |
| **所需模型能力** | STRUCTURED_DRAFTING + GROUNDED_EXTRACTION |
| **所需 Skill** | ipd-plan-evidence-pack / spk-artifact-assembly / spk-tr2-checklist |
| **所需 Tool/MCP** | mcp-spk-os-artifact-query / mcp-filesystem-read |
| **所需 Knowledge** | KN-IPD-TR2-CHECKLIST-014 / KN-IPD-PDCP-TEMPLATE-015 |
| **所需 Information** | 前 5 项 Activity 的全部签名 Artifact |
| **可读取的 Memory 范围** | project:spk-os-ipd, agent_role:planning, capability:structured_drafting |
| **人工责任** | PDT Lead 提交；TR2 评审团队技术结论；IPMT 投资决策（**AI 禁写 DCP 状态**） |
| **失败处理** | 取件失败按 digest 重试；缺件降级为明确 not_ready；版本冲突升级 Configuration Manager；人工拒绝时回滚提交状态与包 manifest |
| **对应 Flowable 节点** | task_tr2_pdcp_pack (ipd-plan.bpmn) |
| **证据来源** | RunReceipt + ArtifactManifest + VerificationReceipt + HumanApprovalRecord |

### 资源与工具详情

#### 输入资源

| 资源 | 来源 | 获取方式 | 质量要求 |
|---|---|---|---|
| PRSBaselineCandidate | ACT-03-03-01 产物 | mcp-spk-os-artifact-query.get() | PM+SE 签署 |
| ArchitectureDescriptionCandidate | ACT-03-03-02 产物 | mcp-spk-os-artifact-query.get() | Chief Architect 签署 |
| IntegratedProjectPlan | ACT-03-03-03 产物 | mcp-spk-os-artifact-query.get() | PM+Resource Owners 签署 |
| CBBReuseDecision | ACT-03-03-04 产物 | mcp-spk-os-artifact-query.get() | SE/TDT/Legal/Security 签署 |
| VerificationStrategy | ACT-03-03-05 产物 | mcp-spk-os-artifact-query.get() | QA Lead 签署 |
| TR2 Checklist | Knowledge 库 | mcp-filesystem-read(/knowledge/ipd/tr2/checklist.md) | 预声明检查项 |
| PDCP 模板 | Knowledge 库 | mcp-filesystem-read(/knowledge/ipd/templates/pdcp/) | 决策包结构 |

#### 工具调用顺序与效率策略

```
Step 1: 锁定 Gate Manifest
├─ 调用 mcp-spk-os-artifact-query.get(所有 5 个 Artifact)
├─ 校验每个 Artifact 的签名和 hash
├─ 校验版本一致性（PRS 版本与架构版本匹配）
└─ 输出：Gate Manifest（资产清单 + hash + 版本）

Step 2: 组装 TR2 证据包
├─ 调用 spk-tr2-checklist.check(all_artifacts)
│  ├─ 逐项检查：
│  │  □ PRS 完整性：所有必需字段、模糊词清零、追溯覆盖
│  │  □ 架构覆盖度：质量属性场景、接口一致性、无循环依赖
│  │  □ 计划可行性：CPM 计算一致、Monte Carlo 分布合理、资源无超配
│  │  □ CBB 适配性：契约完整、许可合规、安全证据充分
│  │  □ 测试覆盖率：高风险 100%、整体 ≥95%
│  │  □ 追溯完整性：source→requirement→architecture→test 全链路
│  │  □ 基线一致性：所有资产 hash 与 Manifest 一致
│  ├─ 每项检查含：检查项、证据、通过/不通过
│  └─ 输出：TR2 检查报告
├─ 列出红黄缺口
│  ├─ 红色（阻断）：缺签名、缺必需 Artifact、高风险测试未覆盖
│  └─ 黄色（警告）：低覆盖率、未处置的模糊词建议
└─ 输出：TR2_CDCP_EvidencePack（草稿）

Step 3: 组装 PDCP 决策包
├─ 商业信息：
│  ├─ 财务评估更新（从概念阶段的 InitialBusinessCase 更新）
│  ├─ 市场分析更新
│  └─ 风险综合评估
├─ 技术摘要：
│  ├─ TR2 结论
│  ├─ 关键架构决策（ADR 摘要）
│  ├─ CBB 策略摘要
│  └─ 测试策略摘要
├─ 投资请求：
│  ├─ 预算（从 IntegratedProjectPlan 提取）
│  ├─ 资源需求
│  └─ 时间表（Monte Carlo P50/P80）
├─ LLM 生成执行摘要（面向 IPMT 的简明概述）
└─ 输出：PDCP 决策包（草稿）

Step 4: DCS 完整性校验
├─ 调用 DCS POST /api/v1/manifest-hash
│  ├─ 计算所有资产的 SHA-256 Manifest
│  └─ 输出：Manifest（含签名）
├─ 检查必需字段完整性
└─ 输出：完整性校验报告

Step 5: Verifier 独立核证
├─ TR-Verifier 独立审查 TR2 证据包
│  ├─ 只读输入，写验证结果
│  ├─ 不与作者 Agent 共用上下文
│  └─ 输出：TR2 VerificationReceipt
├─ DCP-Verifier 独立审查 PDCP 决策包
│  └─ 输出：PDCP VerificationReceipt
└─ 输出：验证报告

Step 6: PDT Lead 提交
├─ PDT Lead 确认材料完整性
├─ 提交 TR2 证据包到 TR2 评审团队
├─ 提交 PDCP 决策包到 IPMT
└─ 输出：TR2_PDCP_EvidencePack（已提交）

Step 7: TR2 评审（人工）
├─ TR2 评审团队审核证据包
├─ 给出技术结论（Pass/Conditional Pass/Fail）
├─ 记录技术评审意见
└─ 输出：TR2 技术结论

Step 8: PDCP 决策（人工，AI 禁写）
├─ IPMT 审核 PDCP 决策包
├─ IPMT 做出投资决策：
│  ├─ Go：批准进入开发阶段，锁定基线
│  ├─ Redirect：要求修改后重新评审
│  └─ Kill：终止项目
├─ **关键约束**：DCP 状态只允许具名 IPMT 成员通过 yudao 写入
├─ **系统层面禁止任何 AI Agent 写入 DCP 状态字段**
├─ 记录决策理由和异议
└─ 输出：PDCP 决策记录

Step 9: Go 后基线锁定
├─ Git 标记基线版本
├─ 目录保护生效（受控区域不可直接修改）
├─ 任何变化登记 CR（Change Request）
├─ 经 CCB 影响分析后生成新版本
└─ 输出：已锁定的计划基线
```

### 输出 Artifact 模板

#### Artifact 名称
`TR2_PDCP_EvidencePack`

#### 文件结构
```
tr2_pdcp_evidence_pack/
├── manifest.yaml
├── evidence_pack.json            # 主数据文件
├── tr2/                          # TR2 证据包
│   ├── checklist_result.json
│   ├── gaps.json
│   └── verification_receipt.json
├── pdcp/                         # PDCP 决策包
│   ├── business_case.json
│   ├── technical_summary.json
│   ├── investment_request.json
│   ├── executive_summary.md
│   └── verification_receipt.json
├── baseline_manifest.json        # 基线 Manifest（SHA-256）
└── README.md
```

#### evidence_pack.json Schema

```json
{
  "pack_id": "tpep-20260818-001",
  "version": "1.0.0",
  "as_of": "2026-08-18",
  "created_at": "2026-08-18T02:00:00Z",
  "created_by": "agent-planning-001",
  "submitted_by": {
    "user_id": "user-pdt-lead-001",
    "role": "PDT Lead",
    "submitted_at": "2026-08-18T03:00:00Z"
  },
  "input_artifacts": [
    {"artifact_id": "prs-20260817-001", "type": "PRSBaselineCandidate", "status": "approved", "hash": "sha256:..."},
    {"artifact_id": "ad-20260817-001", "type": "ArchitectureDescriptionCandidate", "status": "approved", "hash": "sha256:..."},
    {"artifact_id": "ipp-20260817-001", "type": "IntegratedProjectPlan", "status": "approved", "hash": "sha256:..."},
    {"artifact_id": "cbb-dec-20260817-001", "type": "CBBReuseDecision", "status": "approved", "hash": "sha256:..."},
    {"artifact_id": "vs-20260817-001", "type": "VerificationStrategy", "status": "approved", "hash": "sha256:..."}
  ],
  "tr2_result": {
    "checks": [
      {"item": "PRS 完整性", "result": "pass", "evidence_ref": "prs-20260817-001"},
      {"item": "架构覆盖度", "result": "pass", "evidence_ref": "ad-20260817-001"},
      {"item": "计划可行性", "result": "pass", "evidence_ref": "ipp-20260817-001"},
      {"item": "CBB 适配性", "result": "pass", "evidence_ref": "cbb-dec-20260817-001"},
      {"item": "测试覆盖率", "result": "pass", "evidence_ref": "vs-20260817-001"},
      {"item": "追溯完整性", "result": "pass", "evidence_ref": "rtm-coverage"},
      {"item": "基线一致性", "result": "pass", "evidence_ref": "manifest"}
    ],
    "red_gaps": [],
    "yellow_gaps": [
      {"item": "REQ-043/044 未绑定验证方法", "severity": "low", "resolution": "QA Lead 已批准豁免"}
    ],
    "technical_conclusion": "pass",
    "conclusion_by": "user-tr2-team-001",
    "conclusion_at": "2026-08-18T06:00:00Z"
  },
  "pdcp_result": {
    "investment_request": {
      "budget": 4800000,
      "currency": "CNY",
      "duration_p50_days": 142,
      "duration_p80_days": 163,
      "team_size": 12
    },
    "decision": "go",
    "decided_by": "user-ipmt-chair-001",
    "decided_at": "2026-08-18T10:00:00Z",
    "rationale": "市场机会明确，技术方案可行，财务回报合理，风险可控",
    "conditions": [
      "补充小型企业市场分析（来自 TR1 Conditional Go）",
      "每月向 IPMT 报告进度"
    ],
    "dissent": []
  },
  "baseline_locked": {
    "locked_at": "2026-08-18T10:30:00Z",
    "locked_fields": ["product_objectives", "budget_cap", "key_interfaces", "risk_tolerance", "near_term_plan"],
    "rolling_fields": ["far_term_wbs"],
    "change_control": "CCB required for any modification"
  }
}
```

#### README.md 模板

```markdown
# TR2/PDCP 决策包 TPEP-20260818-001

## 概览
- **组装时间**：2026-08-18
- **包含 Artifact**：5 个（全部已签署）
- **TR2 结论**：Pass（7/7 检查通过）
- **PDCP 决定**：Go

## TR2 技术评审
| 检查项 | 结果 | 证据 |
|---|---|---|
| PRS 完整性 | ✅ Pass | prs-20260817-001 |
| 架构覆盖度 | ✅ Pass | ad-20260817-001 |
| 计划可行性 | ✅ Pass | ipp-20260817-001 |
| CBB 适配性 | ✅ Pass | cbb-dec-20260817-001 |
| 测试覆盖率 | ✅ Pass | vs-20260817-001 |
| 追溯完整性 | ✅ Pass | RTM 覆盖率 95.6% |
| 基线一致性 | ✅ Pass | Manifest 哈希一致 |

## PDCP 投资决策
- **投资请求**：¥480 万，142 天（P50），12 人团队
- **决定**：Go
- **决定人**：IPMT Chair，2026-08-18 10:00
- **理由**：市场机会明确，技术方案可行，财务回报合理，风险可控
- **条件**：补充小型企业市场分析；每月向 IPMT 报告进度

## 基线锁定
- **锁定时间**：2026-08-18 10:30
- **锁定字段**：产品目标、预算上限、关键接口、风险容限、近期计划
- **滚动细化**：远期 WBS 以规划包保留
- **变更控制**：任何修改须经 CCB 审批

## 签署
- **PDT Lead**：提交，2026-08-18 03:00
- **TR2 评审团队**：Pass，2026-08-18 06:00
- **IPMT Chair**：Go，2026-08-18 10:00
```

### 节点关联与展示

#### 在整体流程中的位置

```
ACT-03-03-01~05 → ACT-03-03-06 TR2/PDCP 决策包组装 → [计划阶段结束，进入开发阶段]
                          ↓
                  TR2_PDCP_EvidencePack
                          ↓
                  PDCP Go → 基线锁定 → 开发阶段
                  PDCP Redirect → 修改后重新评审
                  PDCP Kill → 项目终止
```

#### 上游依赖
| 上游 Activity | 依赖内容 | 依赖方式 |
|---|---|---|
| ACT-03-03-01~05 | 全部 5 个签名 Artifact | 作为证据包的组成 |

#### 下游消费
| 下游 Activity | 消费内容 | 消费方式 |
|---|---|---|
| 开发阶段所有 Activity | 已锁定的计划基线（PRS/AD/WBS/IMS/COST） | 作为开发实施的约束 |
| 验证阶段 | VerificationStrategy | 作为测试执行的指导 |

#### 可视化展示

**在 SPK-OS Dashboard 的展示方式**：
1. **TR2 检查面板**：7 项检查的 PASS/FAIL 状态，点击展开证据
2. **PDCP 决策面板**：投资请求摘要 + 决定状态 + 条件列表
3. **基线锁定面板**：锁定字段列表 + 变更控制规则
4. **证据链视图**：从 PDCP 决定追溯到每个 Artifact 的完整证据链

**在 yudao 审批工作台的展示方式**：
1. TR2 评审团队看到待评审任务："ACT-03-03-06 TR2 技术评审"
2. IPMT 看到待决策任务："ACT-03-03-06 PDCP 投资决策"
3. **DCP 节点约束**：只有具名 IPMT 成员才能写入决策状态
4. 操作按钮：[Go] [Redirect] [Kill]（仅 IPMT 成员可见）

---

## 计划阶段 Activity 汇总

| Activity ID | 名称 | Lead Agent | 执行位置 | Worker | Verifier | 模型能力 |
|---|---|---|---|---|---|---|
| ACT-03-03-01 | 产品需求规格化 | Planning Lead | task_system | ✓(2) | — | GROUNDED_EXTRACTION + STRUCTURED_DRAFTING |
| ACT-03-03-02 | 架构描述生成 | Planning Lead | task_system | ✓(Gen+Critic) | — | CROSS_SOURCE_REASONING + COUNTERARGUMENT_GENERATION |
| ACT-03-03-03 | 项目计划编制 | Planning Lead | task_system | — | ✓(DCS复算) | STRUCTURED_DRAFTING + CODE_OR_FORMULA_ASSISTANCE |
| ACT-03-03-04 | CBB 复用决策 | Planning Lead | task_system | ✓(N并行) | — | GROUNDED_EXTRACTION + CROSS_SOURCE_REASONING + SAFETY_REVIEW |
| ACT-03-03-05 | 测试策略规划 | Planning Lead | task_system | — | — | GROUNDED_EXTRACTION + STRUCTURED_DRAFTING |
| ACT-03-03-06 | TR2/PDCP 决策包组装 | Planning Lead | task_system | — | ✓(DCP+TR) | STRUCTURED_DRAFTING + GROUNDED_EXTRACTION |

---

## 计划阶段产物关联图

```
[概念阶段 TR1DecisionRecord]
        ↓
ACT-03-03-01 产品需求规格化 → PRSBaselineCandidate
        ↓                           ↓
ACT-03-03-02 架构描述生成 → ArchitectureDescriptionCandidate
        ↓                           ↓
ACT-03-03-03 项目计划编制 → IntegratedProjectPlan
ACT-03-03-04 CBB 复用决策 → CBBReuseDecision
ACT-03-03-05 测试策略规划 → VerificationStrategy
        ↓                           ↓
ACT-03-03-06 TR2/PDCP 决策包组装 → TR2_PDCP_EvidencePack
        ↓
PDCP Go → 基线锁定 → [进入开发阶段]
PDCP Redirect → 修改后重新评审
PDCP Kill → 项目终止
```

---

## 计划阶段关键约束总结

| 约束 | 说明 | 来源 |
|---|---|---|
| DCS 独占数值计算 | CPM/EVM/Monte Carlo/RTM 覆盖率全部由确定性服务执行，LLM 仅解释 | R2 §13, R4 §4 |
| DCP AI 写入禁令 | 系统层面禁止 AI Agent 写入 DCP 状态字段 | R2 §23, NIST AI RMF |
| 基线分层锁定 | 承诺和接口固定，远期工作滚动细化 | R2 §24, R4 §4 |
| CBB 契约匹配 | 7 字段确定性比较，不依赖语义搜索 | R2 §9, R4 §4 |
| 高风险测试 100% 覆盖 | 高风险需求必须绑定至少两种互补验证方法 | R2 §4 AC-05 |
| Generator-Critic 迭代上限 | 架构描述最多 3 轮迭代，防止无限循环 | R2 §15 SOP-02 |
| 变更走 CCB | 基线锁定后任何修改须经 CCB 影响分析 | R2 §24, IEEE 828 |
