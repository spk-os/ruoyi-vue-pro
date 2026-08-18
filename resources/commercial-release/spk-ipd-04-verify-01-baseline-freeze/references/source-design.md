## ACT-03-05-01 冻结验证基线

### 基础信息

| 字段 | 内容 |
|---|---|
| **Activity ID** | ACT-03-05-01 |
| **Activity 名称** | 冻结验证基线 |
| **所属阶段** | 验证 |
| **Lead Agent 角色** | Verification Orchestrator |
| **执行位置** | task_system |
| **是否使用 Worker Agent** | — |
| **是否需要 Independent Verifier** | ✓（DCS 验签 + 独立复核） |
| **所需模型能力** | GROUNDED_EXTRACTION |
| **所需 Skill** | ipd-verify-baseline-freeze / spk-manifest-builder |
| **所需 Tool/MCP** | mcp-spk-os-artifact-query / mcp-git-query / mcp-filesystem-read |
| **所需 Knowledge** | KN-IPD-VERIFY-BASELINE-SCHEMA-037 / KN-NASA-SE-HANDBOOK-038 |
| **所需 Information** | INFO-TR4-OUTPUT（TR4 通过包）/ INFO-DEV-BASELINE（开发基线）/ INFO-BUILD-ARTIFACTS（构建物） |
| **可读取的 Memory 范围** | project:spk-os-ipd, agent_role:verification, capability:grounded_extraction |
| **人工责任** | QA Lead + Configuration Manager 联合锁定验证基线 |
| **失败处理** | 取件失败按 digest 重试；仍失败不冻结；签名/hash 冲突升级 CM/PQA；回滚到上一批准验证 manifest |
| **对应 Flowable 节点** | task_baseline_freeze (ipd-qualification.bpmn) |
| **证据来源** | RunReceipt + ArtifactManifest + GitTag |

### 资源与工具详情

#### 输入资源

| 资源 | 来源 | 获取方式 | 质量要求 |
|---|---|---|---|
| TR4 决策记录 | 开发阶段 ACT-03-04-10 产物 | mcp-spk-os-artifact-query.get(artifact_type="TR3_TR4_DecisionRecord", tr_type="TR4") | 必须含 Go/Conditional Go 决定 |
| 开发证据包 | 开发阶段 ACT-03-04-09 产物 | mcp-spk-os-artifact-query.get(artifact_type="DevelopmentEvidencePack") | PQA+CM 签署 |
| 构建物/SBOM | CI/CD 管线 | mcp-spk-os-artifact-query.get(artifact_type="CIRunEvidence") | 含 SBOM + 签名 |
| 代码仓库 | Git | mcp-git-query.get_repo_tags() | 含开发阶段基线 tag |
| 测试策略 | 计划阶段产物 | mcp-spk-os-artifact-query.get(artifact_type="VerificationStrategy") | QA Lead 签署 |
| 环境拓扑 | DevOps | mcp-filesystem-read(/config/environments/) | 含与生产差异 |

#### 工具调用顺序与效率策略

```
Step 1: TR4 Go 验证
├─ 调用 mcp-spk-os-artifact-query.get(TR3_TR4_DecisionRecord, tr_type="TR4")
├─ 校验 TR4 决定为 Go 或 Conditional Go（否则阻断）
├─ 提取 TR4 条件列表（Conditional Go 的附加条件）
├─ 检查所有条件的当前状态
└─ 输出：TR4 Go 确认 + 条件清单

Step 2: 盘点所有验证对象
├─ 从 TR4 证据包中提取所有引用的 Artifact ID
├─ 逐个调用 mcp-spk-os-artifact-query.get(artifact_id) 取回制品
├─ 对每个制品计算 SHA-256 hash
├─ 与 TR4 证据包中记录的 hash 比对
├─ 校验每个制品的签名完整性
├─ 额外盘点：
│  ├─ 构建物版本（commit SHA + SBOM hash）
│  ├─ 测试代码版本
│  ├─ 测试数据版本
│  ├─ 环境配置版本
│  └─ 模型版本（如有 AI 组件）
└─ 输出：验证对象盘点清单

Step 3: DCS 验签与依赖闭合检查
├─ 调用 DCS POST /api/v1/verify-baseline-check
│  ├─ 输入：所有制品的 hash + 签名 + 依赖关系图
│  ├─ 检查：
│  │  □ 每个制品的 hash 与 TR4 记录一致
│  │  □ 每个制品的签名完整（必需角色均已签署）
│  │  □ 制品间依赖闭合（需求→代码→测试→证据 追溯链完整）
│  │  □ 构建物 SBOM 完整且许可证合规
│  │  □ 环境配置版本与测试策略匹配
│  ├─ 输出：验证报告 + 签名
│  └─ 签名：sha256(input + output + solver_version + timestamp)
└─ 输出：DCS 验证报告

Step 4: 标记开放例外
├─ 列出 TR4 Conditional Go 的附加条件
├─ 检查每个条件的当前状态
├─ 未满足的条件标记为"开放例外"
├─ 列出验证基线与开发基线的差异（如有变更）
└─ 输出：例外清单

Step 5: QA Lead + CM 人工审核与冻结
├─ QA Lead 审查验证对象盘点清单
├─ CM 审查 hash 匹配状态和 DCS 验证报告
├─ 确认开放例外的处置方案
├─ 联合签署验证基线冻结
└─ 输出：VerificationBaselineManifest（已签署）

Step 6: Git Tag 创建与基线锁定
├─ 创建 Git tag: baseline/tr4-{project_id}-{date}
├─ 附加 GPG 签名
├─ yudao 记录验证基线状态为 "frozen"
├─ 冻结后只经变更控制更新
└─ 输出：Git Tag + 基线冻结记录
```

#### 效率优化策略

| 策略 | 说明 | 预期效果 |
|---|---|---|
| 批量 hash 计算 | 一次性取回所有制品并并行计算 hash | 比逐个取回快 5 倍 |
| DCS 依赖闭合检查 | 确定性图算法验证追溯链完整性 | 消除人工检查遗漏 |
| 环境配置 diff | 自动计算测试环境与生产环境差异 | 减少人工比对成本 |

### 输出 Artifact 模板

#### Artifact 名称
`VerificationBaselineManifest`

#### 文件结构
```
verification_baseline_manifest/
├── manifest.yaml                 # ArtifactManifest
├── baseline.json                 # 主数据文件
├── artifacts/                    # 所有验证对象的引用
│   ├── build_artifact.json
│   ├── test_code.json
│   ├── test_data.json
│   ├── environment_config.json
│   └── sbom.json
├── dcs_receipts/                 # DCS 验证签名
│   └── baseline_verify.json
├── exceptions/                   # 开放例外清单
│   └── open_exceptions.json
├── environment_diff/             # 环境差异报告
│   └── staging_vs_prod.yaml
├── git_tag.json                  # Git Tag 信息
└── README.md
```

#### baseline.json Schema

```json
{
  "baseline_id": "vbl-20260828-001",
  "version": "1.0.0",
  "as_of": "2026-08-28",
  "created_at": "2026-08-28T10:00:00Z",
  "created_by": "agent-verification-orchestrator-001",
  "signed_by": [
    {
      "user_id": "user-qa-lead-001",
      "role": "QA Lead",
      "signed_at": "2026-08-28T11:00:00Z",
      "decision": "approved"
    },
    {
      "user_id": "user-cm-001",
      "role": "Configuration Manager",
      "signed_at": "2026-08-28T11:30:00Z",
      "decision": "approved"
    }
  ],
  "tr4_ref": {
    "decision_id": "tr4-20260825-001",
    "decision": "go",
    "decided_at": "2026-08-25T16:00:00Z"
  },
  "verification_objects": [
    {
      "artifact_id": "build-20260825-001",
      "type": "BuildArtifact",
      "version": "1.0.0-rc3",
      "commit_sha": "e5f6g7h8i9j0k1l2m3n4",
      "hash": "sha256:abc123...",
      "hash_match": true,
      "sbom_hash": "sha256:def456...",
      "sbom_complete": true
    },
    {
      "artifact_id": "test-code-20260825-001",
      "type": "TestCode",
      "version": "1.0.0",
      "commit_sha": "e5f6g7h8i9j0k1l2m3n4",
      "hash": "sha256:ghi789...",
      "hash_match": true,
      "test_count": 168
    },
    {
      "artifact_id": "test-data-20260825-001",
      "type": "TestData",
      "version": "1.0.0",
      "hash": "sha256:jkl012...",
      "hash_match": true,
      "data_classification": "synthetic",
      "authorization_ref": "auth-20260820-001"
    },
    {
      "artifact_id": "env-config-20260825-001",
      "type": "EnvironmentConfig",
      "version": "1.0.0",
      "hash": "sha256:mno345...",
      "hash_match": true,
      "environments": ["integration", "staging", "representative-user"]
    }
  ],
  "dependency_closure": {
    "requirements_to_code": "complete",
    "code_to_tests": "complete",
    "tests_to_evidence": "complete",
    "all_traces_valid": true
  },
  "open_exceptions": [
    {
      "exception_id": "EXC-V01",
      "tr4_condition": "补充安全渗透测试报告",
      "status": "in_progress",
      "owner": "user-security-lead-001",
      "deadline": "2026-09-10",
      "impact": "medium"
    }
  ],
  "environment_diff_summary": {
    "staging_vs_prod": {
      "config_differences": 5,
      "data_differences": 2,
      "network_differences": 1,
      "critical_differences": 0
    }
  },
  "git_tag": {
    "tag_name": "baseline/tr4-aurora-20260828",
    "commit_sha": "e5f6g7h8i9j0k1l2m3n4",
    "gpg_signed": true,
    "created_at": "2026-08-28T12:00:00Z"
  },
  "dcs_receipt": {
    "solver_version": "verify-baseline-check-v1",
    "input_hash": "sha256:...",
    "signature": "sha256:..."
  }
}
```

### 节点关联与展示

#### 在整体流程中的位置

```
[开发阶段 TR4 Go] → ACT-03-05-01 冻结验证基线 → ACT-03-05-02 建立 RVM/RTM
                              ↓
                      VerificationBaselineManifest
                              ↓
                      [验证基线冻结，后续变更走 CCB]
```

#### 上游依赖
| 上游 Activity | 依赖内容 | 依赖方式 |
|---|---|---|
| ACT-03-04-10 TR3/TR4 评审 | TR4 DecisionRecord（Go 决定） | 作为准入条件 |
| ACT-03-04-09 证据汇编 | DevelopmentEvidencePack | 作为基线制品 |
| ACT-03-04-07 持续集成 | CIRunEvidence（含 SBOM） | 作为构建物引用 |

#### 下游消费
| 下游 Activity | 消费内容 | 消费方式 |
|---|---|---|
| ACT-03-05-02 建立 RVM/RTM | VerificationBaselineManifest | 作为追溯的基线锚点 |
| ACT-03-05-04 准备环境与数据 | 环境配置版本 | 作为环境准备的基准 |
| ACT-03-05-05 执行与采证 | 构建物版本 + 测试代码版本 | 作为执行的固定版本 |

---
