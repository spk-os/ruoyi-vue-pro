## ACT-03-06-01 Release Manifest 构建与验证

### 基础信息

| 字段 | 内容 |
|---|---|
| **Activity ID** | ACT-03-06-01 |
| **Activity 名称** | Release Manifest 构建与验证 |
| **所属阶段** | 发布 |
| **Lead Agent 角色** | Release Orchestrator |
| **执行位置** | task_system |
| **是否使用 Worker Agent** | ✓（Manifest Assembly Agent 组装 + Policy Agent 校验） |
| **是否需要 Independent Verifier** | ✓（DCS 验签 + 独立复核） |
| **所需模型能力** | GROUNDED_EXTRACTION + STRUCTURED_DRAFTING |
| **所需 Skill** | ipd-release-manifest / spk-manifest-builder / spk-policy-validator |
| **所需 Tool/MCP** | mcp-spk-os-artifact-query / mcp-git-query / mcp-sbom-query / mcp-provenance-query |
| **所需 Knowledge** | KN-IPD-RELEASE-MANIFEST-SCHEMA-055 / KN-SLSA-PROVENANCE-056 / KN-SPDX-SBOM-057 |
| **所需 Information** | INFO-ADCP-OUTPUT（ADCP 批准记录）/ INFO-TR5-OUTPUT（TR5 验证报告）/ INFO-CANDIDATE-ARTIFACT（候选制品）/ INFO-SBOM（SBOM）/ INFO-PROVENANCE（SLSA attestation）/ INFO-CONFIG（配置版本） |
| **可读取的 Memory 范围** | project:spk-os-ipd, agent_role:release, capability:grounded_extraction |
| **人工责任** | Release Manager 最终审批 Manifest 并签署 |
| **失败处理** | 取件失败按 digest 重试；缺件降级为 not_ready；签名/SBOM 冲突升级 Supply Chain Security；回滚至上一批准 manifest |
| **对应 Flowable 节点** | task_manifest_build + task_manifest_verify + task_manifest_approve (ipd-release.bpmn) |
| **证据来源** | RunReceipt + ArtifactManifest + HumanApprovalRecord |

### 资源与工具详情

#### 输入资源

| 资源 | 来源 | 获取方式 | 质量要求 |
|---|---|---|---|
| ADCP 批准记录 | 验证阶段 ACT-03-05-09 产物 | mcp-spk-os-artifact-query.get(artifact_type="ADCP_DecisionRecord") | 必须含 IPMT Go 决定 + 条件列表 |
| TR5 验证报告 | 验证阶段 ACT-03-05-09 产物 | mcp-spk-os-artifact-query.get(artifact_type="TR5QualificationDecision") | 具名评委签署 |
| 候选制品 | CI/CD Pipeline | mcp-spk-os-artifact-query.get(artifact_type="BuildArtifact") | 含 digest + 构建日志 |
| SBOM | CI/CD Pipeline | mcp-sbom-query.get(build_id) | SPDX 2.3+ 格式 |
| SLSA Provenance | CI/CD Pipeline | mcp-provenance-query.get(build_id) | SLSA L3 合规 |
| 配置版本 | 配置管理系统 | mcp-filesystem-read(/config/release/) | 含迁移脚本 |
| TR1-TR4 报告索引 | 各阶段归档 | mcp-spk-os-artifact-query.list(artifact_type="TR*_DecisionRecord") | 全部已签署 |
| DCP 记录索引 | 各阶段归档 | mcp-spk-os-artifact-query.list(artifact_type="*_DCP_DecisionRecord") | CDCP/PDCP/ADCP |

#### 工具调用顺序与效率策略

```
Step 1: 锁定 ADCP 输入
├─ 调用 mcp-spk-os-artifact-query.get(ADCP_DecisionRecord)
├─ 校验 ADCP 决定为 Go 或 Conditional Go（否则阻断）
├─ 提取 ADCP 条件列表
├─ 检查所有条件的当前状态
└─ 输出：ADCP Go 确认 + 条件清单

Step 2: Manifest Assembly Agent 组装 Manifest 草案
├─ 调用 spk-manifest-builder.assemble(adcp, tr5, artifact, sbom, provenance, config)
│  ├─ 从 CI/CD 拉取候选制品 digest 和构建日志
│  ├─ 从 SBOM Store 拉取 SPDX SBOM
│  ├─ 从 Provenance Store 拉取 SLSA attestation
│  ├─ 从 Config Management 拉取配置版本和迁移脚本
│  ├─ 从 Evidence Store 拉取 TR1-TR5 报告索引
│  ├─ 从 DCP Store 拉取 CDCP/PDCP/ADCP 记录索引
│  ├─ 生成 Manifest 草案 (JSON)
│  │  ├─ 每个字段标注来源 (source_system + source_id)
│  │  ├─ 缺失字段标记为 null 并在 missing_fields 数组中列出
│  │  └─ **不编造任何数据——所有值必须来自输入**
│  └─ 输出：Manifest 草案
└─ 输出：ReleaseManifest（草稿）

Step 3: Policy Agent 自动策略校验
├─ 调用 spk-policy-validator.validate(manifest_draft)
│  ├─ 验证 SBOM 格式合规 (SPDX 2.3+)
│  ├─ 验证 SLSA level ≥ 3 (或标注降级理由 + risk acceptance)
│  ├─ 验证 digest 与 TR5 验证制品一致（端到端一致性）
│  ├─ 检查漏洞扫描结果 (Critical=0, High ≤ 3 with acceptance)
│  ├─ 检查许可证合规（GPL/LGPL 冲突检测）
│  ├─ 检查配置版本与迁移脚本一致性
│  ├─ 检查 TR 报告索引完整性（TR1-TR5 全部存在）
│  └─ 输出：校验报告
└─ 输出：Policy 校验报告

Step 4: DCS 确定性校验
├─ 调用 DCS POST /api/v1/digest/verify
│  ├─ 输入：artifact_ref, expected_digest (from TR5)
│  ├─ 重算 SHA-256 digest
│  ├─ 比对与 TR5 验证制品的 digest
│  ├─ 输出：match, actual_digest, verified_at
│  └─ 签名：sha256(...)
├─ 调用 DCS POST /api/v1/sbom/validate
│  ├─ 输入：sbom_uri, format="SPDX 2.3"
│  ├─ 校验 SBOM Schema 完整性
│  ├─ 检查组件数量和许可证
│  ├─ 输出：compliant, missing_fields, component_count, license_issues
│  └─ 签名：sha256(...)
├─ 调用 DCS POST /api/v1/signature/verify
│  ├─ 输入：manifest_json, public_key_id
│  ├─ 验证 Ed25519 签名
│  ├─ 输出：valid, signed_at
│  └─ 签名：sha256(...)
└─ 输出：DCS 校验报告

Step 5: 独立复核
├─ 独立 Verifier Agent（不同模型族）审查：
│  ├─ Manifest 完整性（所有必填字段存在）
│  ├─ Policy 校验报告（无 Critical/High 未解决）
│  ├─ DCS 校验报告（digest 一致、签名有效）
│  ├─ 标注缺失项和例外
│  └─ 输出：独立复核报告
└─ 输出：IndependentReviewReport

Step 6: Release Manager 审批与签署
├─ Release Manager 审查：
│  ├─ Manifest 草案
│  ├─ Policy 校验报告
│  ├─ DCS 校验报告
│  ├─ 独立复核报告
│  ├─ ADCP 条件清单和当前状态
│  └─ 缺失项和例外的处置方案
├─ Release Manager 签署 Manifest（Ed25519 签名）
├─ **关键约束**：Manifest 一旦签署即为不可变，后续变更必须走 CCB 流程
└─ 输出：ReleaseManifest（已签署）

Step 7: yudao 记录与触发下游
├─ yudao 记录 Manifest 审批状态
├─ 触发下游活动（ACT-03-06-02 和 ACT-03-06-03 可并行）
└─ 输出：Flowable 状态更新
```

#### 效率优化策略

| 策略 | 说明 | 预期效果 |
|---|---|---|
| 批量拉取 | 一次性从各系统拉取所有来源数据 | 比逐个拉取快 5 倍 |
| Policy Agent 自动校验 | 10+ 条策略规则自动执行 | 消除人工检查遗漏 |
| DCS 确定性校验 | digest/SBOM/签名由程序计算 | 消除 AI 心算错误 |
| 独立复核 | 不同模型族交叉验证 | 减少同源偏差 |

### 输出 Artifact 模板

#### Artifact 名称
`ReleaseManifest`

#### 文件结构
```
release_manifest/
├── manifest.json                 # 主数据文件（不可变）
├── policy_report.json            # Policy 校验报告
├── dcs_receipts/                 # DCS 校验签名
│   ├── digest_verify.json
│   ├── sbom_validate.json
│   └── signature_verify.json
├── independent_review.json       # 独立复核报告
└── README.md
```

#### manifest.json Schema

```json
{
  "$schema": "https://spk-os.io/schemas/release-manifest/v2.0.json",
  "manifest_id": "rel-20260910-001",
  "project_id": "aurora",
  "version": "2.0.0",
  "created_at": "2026-09-10T10:00:00Z",
  "created_by": "agent-manifest-assembly-001",
  "adcp_ref": {
    "decision_id": "adcp-20260907-001",
    "decision": "conditional_go",
    "decided_at": "2026-09-07T15:00:00Z",
    "conditions": [
      {
        "condition_id": "COND-ADCP-001",
        "description": "Python 支持标记为 Beta 功能",
        "status": "fulfilled",
        "fulfilled_at": "2026-09-08T10:00:00Z"
      }
    ]
  },
  "artifact": {
    "digest": "sha256:e4b1c7d8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6",
    "registry": "registry.spk-os.io",
    "image_ref": "registry.spk-os.io/aurora:2.0.0",
    "build_id": "build-20260910-001",
    "build_system": "github-actions",
    "build_log_uri": "artifact://build-logs/build-20260910-001.log",
    "source": {
      "system": "ci-cd-pipeline",
      "id": "build-20260910-001"
    }
  },
  "sbom": {
    "format": "SPDX 2.3",
    "uri": "https://sbom.spk-os.io/aurora-2.0.0.spdx.json",
    "digest": "sha256:a1b2c3d4e5f6...",
    "component_count": 342,
    "vulnerability_scan": {
      "tool": "trivy",
      "scan_time": "2026-09-10T09:30:00Z",
      "critical": 0,
      "high": 1,
      "medium": 5,
      "low": 12,
      "high_acceptance_ref": "risk-acceptance-20260909-001"
    },
    "license_summary": {
      "MIT": 280,
      "Apache-2.0": 45,
      "BSD-3-Clause": 12,
      "ISC": 5
    },
    "source": {
      "system": "sbom-store",
      "id": "aurora-2.0.0"
    }
  },
  "provenance": {
    "slsa_level": 3,
    "attestation_uri": "https://provenance.spk-os.io/build-20260910-001.intoto.jsonl",
    "digest": "sha256:f1e2d3c4b5a6...",
    "builder_id": "https://github.com/actions/runner",
    "source_ref": "a1b2c3d4e5f6g7h8i9j0",
    "recipe": {
      "type": "docker",
      "entrypoint": "Dockerfile",
      "build_args": {"VERSION": "2.0.0"}
    },
    "source": {
      "system": "provenance-store",
      "id": "build-20260910-001"
    }
  },
  "configuration": {
    "config_version": "2.0.0",
    "config_hash": "sha256:g7h8i9j0k1l2...",
    "migration_scripts": [
      {
        "script": "migrate_v1_to_v2.sql",
        "digest": "sha256:m3n4o5p6q7r8...",
        "rollback_safe": true,
        "forward_fix_available": true
      }
    ],
    "compatibility_window": ">=1.0.0",
    "new_parameters": [
      {
        "name": "review.parallel_count",
        "type": "integer",
        "default": 4,
        "description": "并行审查的 worker 数量"
      }
    ],
    "source": {
      "system": "config-management",
      "id": "config-v2.0.0"
    }
  },
  "evidence": {
    "tr_reports": [
      {"id": "TR1-2026-001", "type": "concept", "signed": true},
      {"id": "TR2-2026-001", "type": "plan", "signed": true},
      {"id": "TR3-2026-001", "type": "development", "signed": true},
      {"id": "TR4-2026-001", "type": "development", "signed": true},
      {"id": "TR5-2026-001", "type": "qualification", "signed": true}
    ],
    "dcp_records": [
      {"id": "CDCP-2026-001", "type": "concept", "signed": true},
      {"id": "PDCP-2026-001", "type": "plan", "signed": true},
      {"id": "ADCP-2026-001", "type": "availability", "signed": true}
    ],
    "test_coverage": 0.87,
    "security_audit": "SA-2026-003",
    "source": {
      "system": "evidence-store",
      "id": "aurora-2.0.0-evidence-index"
    }
  },
  "ownership": {
    "release_manager": {
      "user_id": "user-zhang-san",
      "role": "Release Manager",
      "signed_at": "2026-09-10T11:00:00Z"
    },
    "pdt_lead": "user-li-si",
    "sre_lead": "user-wang-wu",
    "responsibility_coverage": {
      "engineering": {"owner": "user-li-si", "signed": true, "signed_at": "2026-09-10T10:30:00Z"},
      "operations": {"owner": "user-wang-wu", "signed": true, "signed_at": "2026-09-10T10:35:00Z"},
      "security": {"owner": "user-zhao-liu", "signed": true, "signed_at": "2026-09-10T10:40:00Z"},
      "commercial": {"owner": "user-chen-qi", "signed": true, "signed_at": "2026-09-10T10:45:00Z"},
      "customer_service": {"owner": "user-sun-ba", "signed": true, "signed_at": "2026-09-10T10:50:00Z"}
    }
  },
  "rollback_target": {
    "version": "1.9.5",
    "digest": "sha256:x9y8z7w6v5u4...",
    "rollback_safe": true,
    "estimated_rollback_time_min": 15
  },
  "signature": {
    "algorithm": "Ed25519",
    "public_key_id": "key-release-001",
    "signature_hex": "deadbeefcafebabe...",
    "signed_at": "2026-09-10T11:00:00Z",
    "signer": "user-zhang-san"
  },
  "missing_fields": [],
  "exceptions": [
    {
      "exception_id": "EXC-R01",
      "description": "SLSA L3 provenance 中 builder_id 使用 GitHub Actions 共享 runner",
      "risk": "low",
      "acceptance_ref": "risk-acceptance-20260909-002",
      "expires_at": "2026-12-31"
    }
  ]
}
```

### 节点关联与展示

#### 在整体流程中的位置

```
[验证阶段 TR5 Go + ADCP Go]
        ↓
ACT-03-06-01 Release Manifest 构建与验证
        ↓
ReleaseManifest（已签署，不可变）
        ↓
┌───────────────────┬───────────────────┐
↓                   ↓                   ↓
ACT-03-06-02    ACT-03-06-03    [等待 02+03 完成]
配置与迁移验证   运营就绪验证
        ↓                   ↓
        └───────────────────┘
                    ↓
            ACT-03-06-04 渐进发布
```

#### 上游依赖
| 上游 Activity | 依赖内容 | 依赖方式 |
|---|---|---|
| ACT-03-05-09 TR5 资格评审 | TR5QualificationDecision（Go 决定） | 作为准入条件 |
| ACT-03-05-09 TR5 资格评审 | ADCP_DecisionRecord（Go 决定） | 作为准入条件 |
| CI/CD Pipeline | BuildArtifact + SBOM + Provenance | 作为制品来源 |

#### 下游消费
| 下游 Activity | 消费内容 | 消费方式 |
|---|---|---|
| ACT-03-06-02 配置与数据迁移验证 | configuration 字段 | 作为迁移验证的输入 |
| ACT-03-06-03 运营就绪验证 | ownership 字段 | 作为责任覆盖的基准 |
| ACT-03-06-04 渐进发布策略执行 | artifact.digest + rollback_target | 作为部署和回滚的基准 |
| ACT-03-06-05 TR6 证据包组装 | 整个 Manifest | 作为 TR6 证据包的核心 |

---
