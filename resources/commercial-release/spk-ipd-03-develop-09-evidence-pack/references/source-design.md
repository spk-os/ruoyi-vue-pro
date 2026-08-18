## ACT-03-04-09 证据汇编

### 基础信息

| 字段 | 内容 |
|---|---|
| **Activity ID** | ACT-03-04-09 |
| **Activity 名称** | 证据汇编 |
| **所属阶段** | 开发 |
| **Lead Agent 角色** | Development Orchestrator |
| **执行位置** | task_system |
| **是否使用 Worker Agent** | ✓（Evidence Agent 按 manifest 拉取和关联） |
| **是否需要 Independent Verifier** | ✓（DCS 验 digest/schema/trace + PQA 认证完整性） |
| **所需模型能力** | STRUCTURED_DRAFTING + GROUNDED_EXTRACTION |
| **所需 Skill** | ipd-dev-evidence-pack / spk-evidence-assembler |
| **所需 Tool/MCP** | mcp-spk-os-artifact-query / mcp-git-query / mcp-filesystem-read |
| **所需 Knowledge** | KN-IPD-EVIDENCE-PACK-SCHEMA-031 / KN-SLSA-PROVENANCE-032 |
| **所需 Information** | 所有开发包的全部证据 Artifact |
| **可读取的 Memory 范围** | project:spk-os-ipd, agent_role:development, capability:structured_drafting |
| **人工责任** | PQA / Configuration Manager 认证完整性 |
| **失败处理** | 单证据拉取失败按 digest 重试；缺失则明确红项；篡改/签名冲突升级 PQA/Security；回滚到上一签名 pack manifest |
| **对应 Flowable 节点** | task_evidence_assembly (ipd-development.bpmn) |
| **证据来源** | RunReceipt + ArtifactManifest + VerificationReceipt |

### 资源与工具详情

#### 工具调用顺序与效率策略

```
Step 1: 按包 manifest 拉取原始证据
├─ 遍历所有开发包
├─ 对每个包拉取：
│  ├─ ImplementationDraft（代码 PR + provenance）
│  ├─ UnitComponentEvidence（测试证据）
│  ├─ PeerReviewRecord（审查记录）
│  ├─ CIRunEvidence（CI 报告 + SBOM）
│  ├─ ChangeDecisionRecord（变更记录，如有）
│  └─ 所有原始日志（CI logs, test logs）
├─ 校验每个证据的 hash 与记录一致
└─ 输出：原始证据集合

Step 2: DCS 校验 digest/schema/trace
├─ 调用 DCS POST /api/v1/evidence-verify
│  ├─ 检查：
│  │  □ 所有证据的 hash 与 manifest 一致
│  │  □ 所有证据的 Schema 合规
│  │  □ requirement-to-evidence 追溯完整
│  │  □ 无孤立证据（每个证据至少关联一个需求）
│  │  □ 无重复证据（同一 commit 不应有两个 SBOM）
│  ├─ 输出：验证报告 + 签名
│  └─ 签名：sha256(...)
└─ 输出：DCS 验证报告

Step 3: 建立追溯索引
├─ 构建 requirement → code → test → evidence 的完整追溯链
├─ 标注缺口（未覆盖的需求、未关联的证据）
├─ 标注例外（已批准的豁免）
└─ 输出：追溯矩阵

Step 4: 独立复核
├─ PQA 验证证据包完整性
├─ Configuration Manager 验证版本一致性
├─ 签署证据包
└─ 输出：DevelopmentEvidencePack（已签署）
```

### 输出 Artifact 模板

#### Artifact 名称
`DevelopmentEvidencePack`

#### DevelopmentEvidencePack Schema

```json
{
  "pack_id": "dep-20260825-001",
  "version": "1.0.0",
  "created_at": "2026-08-25T10:00:00Z",
  "created_by": "agent-evidence-001",
  "signed_by": [
    {"user_id": "user-pqa-001", "role": "PQA", "signed_at": "2026-08-25T14:00:00Z"},
    {"user_id": "user-cm-001", "role": "Configuration Manager", "signed_at": "2026-08-25T14:30:00Z"}
  ],
  "dev_packets_covered": ["DP-001", "DP-002", "DP-003", "DP-004", "DP-005"],
  "evidence_inventory": {
    "implementation_drafts": 5,
    "unit_component_evidence": 5,
    "peer_review_records": 8,
    "ci_run_evidence": 12,
    "change_decision_records": 2,
    "total_raw_logs": 45
  },
  "traceability": {
    "total_requirements": 45,
    "traced_to_code": 45,
    "traced_to_test": 43,
    "traced_to_evidence": 43,
    "coverage_pct": 95.6,
    "gaps": [
      {"requirement_id": "REQ-043", "reason": "QA Lead 已批准豁免（低风险 cosmetic）"},
      {"requirement_id": "REQ-044", "reason": "QA Lead 已批准豁免（低风险 cosmetic）"}
    ]
  },
  "sbom_summary": {
    "total_components": 127,
    "known_vulnerabilities": 0,
    "license_compliance": "pass"
  },
  "manifest_hash": "sha256:...",
  "dcs_receipt": {
    "solver_version": "evidence-verify-v1",
    "input_hash": "sha256:...",
    "signature": "sha256:..."
  }
}
```

---
