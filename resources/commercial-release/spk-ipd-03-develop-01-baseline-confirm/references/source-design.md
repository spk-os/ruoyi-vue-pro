## ACT-03-04-01 基线确认

### 基础信息

| 字段 | 内容 |
|---|---|
| **Activity ID** | ACT-03-04-01 |
| **Activity 名称** | 基线确认 |
| **所属阶段** | 开发 |
| **Lead Agent 角色** | Development Orchestrator |
| **执行位置** | task_system |
| **是否使用 Worker Agent** | — |
| **是否需要 Independent Verifier** | ✓（DCS 验签 + 独立复核） |
| **所需模型能力** | GROUNDED_EXTRACTION |
| **所需 Skill** | ipd-dev-baseline-confirm / spk-manifest-builder |
| **所需 Tool/MCP** | mcp-spk-os-artifact-query / mcp-git-query / mcp-filesystem-read |
| **所需 Knowledge** | KN-IPD-BASELINE-SCHEMA-016 / KN-IEEE-828-CM-017 |
| **所需 Information** | INFO-PDCP-OUTPUT（PDCP 决策记录）/ INFO-PLAN-BASELINE（计划基线）/ INFO-ARCH-BASELINE（架构基线）/ INFO-PRS-BASELINE（需求基线） |
| **可读取的 Memory 范围** | project:spk-os-ipd, agent_role:development, capability:grounded_extraction |
| **人工责任** | Configuration Manager / PDT Lead 确认开发基线 |
| **失败处理** | 制品取回失败按 digest 重试；仍失败降级人工核查且不批准；hash/签名冲突升级 CM；回滚到上一批准 manifest |
| **对应 Flowable 节点** | task_baseline_confirm (ipd-development.bpmn) |
| **证据来源** | RunReceipt + ArtifactManifest + GitTag |

### 资源与工具详情

#### 输入资源

| 资源 | 来源 | 获取方式 | 质量要求 |
|---|---|---|---|
| PDCP 决策记录 | 计划阶段 ACT-03-03-06 产物 | mcp-spk-os-artifact-query.get(artifact_type="TR2_PDCP_EvidencePack") | 必须含 IPMT Go 决定 |
| 计划基线 | 计划阶段 ACT-03-03-03 产物 | mcp-spk-os-artifact-query.get(artifact_type="IntegratedProjectPlan") | PM+Resource Owners 签署 |
| 架构基线 | 计划阶段 ACT-03-03-02 产物 | mcp-spk-os-artifact-query.get(artifact_type="ArchitectureDescriptionCandidate") | Chief Architect 签署 |
| 需求基线 | 计划阶段 ACT-03-03-01 产物 | mcp-spk-os-artifact-query.get(artifact_type="PRSBaselineCandidate") | PM+SE 签署 |
| 测试策略 | 计划阶段 ACT-03-03-05 产物 | mcp-spk-os-artifact-query.get(artifact_type="VerificationStrategy") | QA Lead 签署 |
| CBB 决策 | 计划阶段 ACT-03-03-04 产物 | mcp-spk-os-artifact-query.get(artifact_type="CBBReuseDecision") | SE/TDT/Legal/Security 签署 |
| Git 仓库配置 | DevOps | mcp-git-query.get_repo_config() | 保护分支已配置 |

#### 工具调用顺序与效率策略

```
Step 1: PDCP Go 验证
├─ 调用 mcp-spk-os-artifact-query.get(TR2_PDCP_EvidencePack)
├─ 校验 PDCP 决定为 Go（否则阻断）
├─ 提取 PDCP 条件列表（Conditional Go 的附加条件）
└─ 输出：PDCP Go 确认 + 条件清单

Step 2: 盘点所有基线制品
├─ 从 PDCP 证据包中提取所有引用的 Artifact ID
├─ 逐个调用 mcp-spk-os-artifact-query.get(artifact_id) 取回制品
├─ 对每个制品计算 SHA-256 hash
├─ 与 PDCP 证据包中记录的 hash 比对
├─ 校验每个制品的签名完整性（签署人、时间戳、角色）
└─ 输出：制品盘点清单（ID + hash + 签名状态 + 匹配状态）

Step 3: DCS 验签与依赖闭合检查
├─ 调用 DCS POST /api/v1/baseline-verify
│  ├─ 输入：所有制品的 hash + 签名 + 依赖关系图
│  ├─ 检查：
│  │  □ 每个制品的 hash 与 PDCP 记录一致
│  │  □ 每个制品的签名完整（必需角色均已签署）
│  │  □ 制品间依赖闭合（PRS→AD→Plan→Test 追溯链完整）
│  │  □ 无孤立制品（每个制品至少被一个下游引用）
│  ├─ 输出：验证报告 + 签名
│  └─ 签名：sha256(input + output + solver_version + timestamp)
└─ 输出：DCS 验证报告

Step 4: 标记开放例外
├─ 列出 PDCP Conditional Go 的附加条件
├─ 检查每个条件的当前状态（已满足/未满足/进行中）
├─ 未满足的条件标记为"开放例外"，绑定责任人和截止日期
└─ 输出：例外清单

Step 5: Configuration Manager / PDT Lead 人工审核
├─ 审查制品盘点清单和 hash 匹配状态
├─ 审查 DCS 验证报告
├─ 确认开放例外的处置方案
├─ 签署基线锁定
└─ 输出：DevelopmentBaselineManifest（已签署）

Step 6: Git Tag 创建与基线锁定
├─ 创建 Git tag: baseline/pdcp-{project_id}-{date}
├─ 附加 GPG 签名
├─ 配置保护分支规则：后续变更必须走 CCB 流程
├─ yudao 记录基线状态为 "locked"
└─ 输出：Git Tag + 基线锁定记录
```

#### 效率优化策略

| 策略 | 说明 | 预期效果 |
|---|---|---|
| 批量 hash 计算 | 一次性取回所有制品并并行计算 hash | 比逐个取回快 5 倍 |
| DCS 依赖闭合检查 | 确定性图算法验证追溯链完整性 | 消除人工检查遗漏 |
| 例外自动关联 | PDCP 条件自动映射到责任人 | 减少人工跟踪成本 |

### 输出 Artifact 模板

#### Artifact 名称
`DevelopmentBaselineManifest`

#### 文件结构
```
development_baseline_manifest/
├── manifest.yaml                 # ArtifactManifest
├── baseline.json                 # 主数据文件
├── artifacts/                    # 所有基线制品的引用
│   ├── prs_baseline.json
│   ├── architecture.json
│   ├── project_plan.json
│   ├── cbb_decision.json
│   ├── test_strategy.json
│   └── pdcp_evidence_pack.json
├── dcs_receipts/                 # DCS 验证签名
│   └── baseline_verify.json
├── exceptions/                   # 开放例外清单
│   └── open_exceptions.json
├── git_tag.json                  # Git Tag 信息
└── README.md
```

#### baseline.json Schema

```json
{
  "baseline_id": "dbl-20260818-001",
  "version": "1.0.0",
  "as_of": "2026-08-18",
  "created_at": "2026-08-18T11:00:00Z",
  "created_by": "agent-development-orchestrator-001",
  "signed_by": [
    {
      "user_id": "user-cm-001",
      "role": "Configuration Manager",
      "signed_at": "2026-08-18T12:00:00Z",
      "decision": "approved"
    },
    {
      "user_id": "user-pdt-lead-001",
      "role": "PDT Lead",
      "signed_at": "2026-08-18T12:30:00Z",
      "decision": "approved"
    }
  ],
  "pdcp_ref": {
    "evidence_pack_id": "tpep-20260818-001",
    "decision": "go",
    "decided_at": "2026-08-18T10:00:00Z"
  },
  "artifacts": [
    {
      "artifact_id": "prs-20260817-001",
      "type": "PRSBaselineCandidate",
      "version": "1.0.0",
      "hash": "sha256:abc123...",
      "hash_match": true,
      "signatures_complete": true,
      "signers": ["user-pm-001", "user-se-001"]
    },
    {
      "artifact_id": "ad-20260817-001",
      "type": "ArchitectureDescriptionCandidate",
      "version": "1.0.0",
      "hash": "sha256:def456...",
      "hash_match": true,
      "signatures_complete": true,
      "signers": ["user-chief-architect-001"]
    },
    {
      "artifact_id": "ipp-20260817-001",
      "type": "IntegratedProjectPlan",
      "version": "1.0.0",
      "hash": "sha256:ghi789...",
      "hash_match": true,
      "signatures_complete": true,
      "signers": ["user-pm-001", "user-resource-owner-001"]
    },
    {
      "artifact_id": "cbb-dec-20260817-001",
      "type": "CBBReuseDecision",
      "version": "1.0.0",
      "hash": "sha256:jkl012...",
      "hash_match": true,
      "signatures_complete": true,
      "signers": ["user-se-001", "user-tdt-001", "user-legal-001", "user-security-001"]
    },
    {
      "artifact_id": "vs-20260817-001",
      "type": "VerificationStrategy",
      "version": "1.0.0",
      "hash": "sha256:mno345...",
      "hash_match": true,
      "signatures_complete": true,
      "signers": ["user-qa-lead-001"]
    }
  ],
  "dependency_closure": {
    "prs_to_architecture": "complete",
    "architecture_to_plan": "complete",
    "prs_to_test_strategy": "complete",
    "all_traces_valid": true
  },
  "open_exceptions": [
    {
      "exception_id": "EXC-001",
      "pdcp_condition": "补充小型企业市场分析",
      "status": "in_progress",
      "owner": "user-market-analyst-001",
      "deadline": "2026-09-15",
      "impact": "low"
    }
  ],
  "git_tag": {
    "tag_name": "baseline/pdcp-aurora-20260818",
    "commit_sha": "e5f6g7h8i9j0",
    "gpg_signed": true,
    "created_at": "2026-08-18T12:45:00Z"
  },
  "dcs_receipt": {
    "solver_version": "baseline-verify-v1",
    "input_hash": "sha256:...",
    "signature": "sha256:..."
  }
}
```

### 节点关联与展示

#### 在整体流程中的位置

```
[计划阶段 PDCP Go] → ACT-03-04-01 基线确认 → ACT-03-04-02 开发包切片
                              ↓
                      DevelopmentBaselineManifest
                              ↓
                      [基线锁定，后续变更走 CCB]
```

#### 上游依赖
| 上游 Activity | 依赖内容 | 依赖方式 |
|---|---|---|
| ACT-03-03-06 TR2/PDCP 决策包组装 | TR2_PDCP_EvidencePack（Go 决定） | 作为准入条件 |
| ACT-03-03-01~05 | 全部 5 个签名 Artifact | 作为基线制品 |

#### 下游消费
| 下游 Activity | 消费内容 | 消费方式 |
|---|---|---|
| ACT-03-04-02 开发包切片 | DevelopmentBaselineManifest | 作为切片的输入基线 |
| 所有后续开发 Activity | 已锁定的基线 | 作为开发实施的约束 |

---
