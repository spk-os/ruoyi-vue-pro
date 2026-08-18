## ACT-03-05-04 准备环境与数据

### 基础信息

| 字段 | 内容 |
|---|---|
| **Activity ID** | ACT-03-05-04 |
| **Activity 名称** | 准备环境与数据 |
| **所属阶段** | 验证 |
| **Lead Agent 角色** | Verification Orchestrator |
| **执行位置** | task_system |
| **是否使用 Worker Agent** | ✓（Environment Agent 建议拓扑/diff） |
| **是否需要 Independent Verifier** | ✓（DCS 校验 IaC/镜像/seed/ACL/脱敏规则） |
| **所需模型能力** | GROUNDED_EXTRACTION + STRUCTURED_DRAFTING |
| **所需 Skill** | ipd-verify-env-prep / spk-env-provisioner / spk-data-factory |
| **所需 Tool/MCP** | mcp-spk-os-artifact-query / mcp-filesystem-read |
| **所需 Knowledge** | KN-IPD-ENV-SCHEMA-044 / KN-DATA-PRIVACY-POLICY-045 |
| **所需 Information** | INFO-TEST-DESIGN（测试设计）/ INFO-ENV-TOPOLOGY（环境拓扑）/ INFO-DATA-AUTH（数据授权） |
| **可读取的 Memory 范围** | project:spk-os-ipd, agent_role:verification, capability:grounded_extraction |
| **人工责任** | Ops 执行环境部署；Data Owner 授权数据使用 |
| **失败处理** | 构建失败在干净租户重试一次；仍失败降级已批准备用环境；隐私/驻留异常升级 Data Owner/Security；回滚并销毁环境和测试数据 |
| **对应 Flowable 节点** | task_env_data_prep (ipd-qualification.bpmn) |
| **证据来源** | RunReceipt + ArtifactManifest + DataAuthorizationRecord |

### 资源与工具详情

#### 工具调用顺序与效率策略

```
Step 1: 读取测试设计需求
├─ 从 MultiLayerTestDesign 中提取环境需求
├─ 识别每个测试层级的环境要求
├─ 识别数据需求（类型、量级、分布）
└─ 输出：环境需求清单

Step 2: Environment Agent 建议拓扑
├─ 调用 spk-env-provisioner.suggest(test_design, env_topology)
│  ├─ 生成环境计划：
│  │  ├─ 基础设施即代码（IaC）模板
│  │  ├─ 容器镜像版本（含 digest）
│  │  ├─ 配置参数（与生产差异标注）
│  │  ├─ 网络拓扑（隔离策略）
│  │  └─ 资源配额
│  ├─ 计算与生产环境的差异：
│  │  ├─ 配置差异（逐项列出）
│  │  ├─ 数据差异（合成 vs 真实）
│  │  ├─ 网络差异（延迟、带宽）
│  │  └─ 硬件差异（CPU、内存、GPU）
│  └─ 输出：环境计划草稿

Step 3: 准备测试数据
├─ 调用 spk-data-factory.prepare(data_requirements, privacy_policy)
│  ├─ 数据策略：
│  │  ├─ 优先使用合成数据（避免隐私风险）
│  │  ├─ 必要时使用脱敏真实数据（需 Data Owner 授权）
│  │  ├─ 记录数据来源和授权
│  │  └─ 确保数据分布代表性
│  ├─ 生成数据：
│  │  ├─ 固定随机种子（seed=42）确保可重放
│  │  ├─ 生成边界值数据
│  │  ├─ 生成异常数据
│  │  └─ 生成性能测试数据（量级匹配）
│  └─ 输出：测试数据集 + 数据授权记录

Step 4: DCS 校验
├─ 调用 DCS POST /api/v1/env-validate
│  ├─ 检查：
│  │  □ IaC 模板 hash 与计划一致
│  │  □ 容器镜像 digest 与计划一致
│  │  □ 随机种子已固定
│  │  □ ACL 配置正确（最小权限）
│  │  □ 脱敏规则已应用（如使用真实数据）
│  │  □ 数据驻留合规（不跨区域）
│  │  □ 环境可销毁并重建
│  ├─ 输出：验证报告 + 签名
│  └─ 签名：sha256(...)
└─ 输出：DCS 验证报告

Step 5: Ops + Data Owner 执行与授权
├─ Ops 按 IaC 模板部署环境
├─ Ops 执行冒烟验证（环境可用性检查）
├─ Data Owner 审查数据使用授权
├─ Data Owner 签署数据授权
├─ Ops 签署环境就绪
└─ 输出：ReplayableTestEnvironment（已签署）
```

### 输出 Artifact 模板

#### Artifact 名称
`ReplayableTestEnvironment`

#### ReplayableTestEnvironment Schema

```json
{
  "env_id": "rte-20260829-001",
  "version": "1.0.0",
  "created_at": "2026-08-29T09:00:00Z",
  "signed_by": [
    {"user_id": "user-ops-001", "role": "Ops Engineer", "signed_at": "2026-08-29T11:00:00Z"},
    {"user_id": "user-data-owner-001", "role": "Data Owner", "signed_at": "2026-08-29T11:30:00Z"}
  ],
  "environments": [
    {
      "name": "staging",
      "iac_digest": "sha256:abc123...",
      "container_images": [
        {"name": "aurora-api", "digest": "sha256:def456..."},
        {"name": "aurora-worker", "digest": "sha256:ghi789..."}
      ],
      "config_hash": "sha256:jkl012...",
      "seed": 42,
      "smoke_test_result": "pass"
    },
    {
      "name": "representative-user",
      "iac_digest": "sha256:mno345...",
      "container_images": [
        {"name": "aurora-api", "digest": "sha256:def456..."}
      ],
      "config_hash": "sha256:pqr678...",
      "seed": 42,
      "smoke_test_result": "pass"
    }
  ],
  "data_lineage": [
    {
      "dataset_id": "ds-001",
      "type": "synthetic",
      "generation_method": "spk-data-factory v1.2",
      "seed": 42,
      "record_count": 10000,
      "authorization_ref": "N/A (synthetic)"
    },
    {
      "dataset_id": "ds-002",
      "type": "desensitized_real",
      "source": "production_anonymized_20260820",
      "desensitization_rules_hash": "sha256:stu901...",
      "record_count": 5000,
      "authorization_ref": "auth-20260820-001",
      "authorized_by": "user-data-owner-001",
      "retention_days": 30
    }
  ],
  "production_diff": {
    "config_differences": [
      {"key": "database.pool_size", "staging": 10, "production": 50, "impact": "low"},
      {"key": "cache.ttl_seconds", "staging": 60, "production": 300, "impact": "medium"},
      {"key": "ml_model.endpoint", "staging": "staging-ml.example.com", "production": "ml.example.com", "impact": "low"}
    ],
    "data_differences": [
      {"aspect": "data_volume", "staging": "10K records", "production": "10M records", "impact": "medium"},
      {"aspect": "data_distribution", "staging": "synthetic uniform", "production": "real skewed", "impact": "medium"}
    ],
    "network_differences": [
      {"aspect": "latency", "staging": "<1ms (same DC)", "production": "5-50ms (cross DC)", "impact": "medium"}
    ],
    "critical_differences": 0
  },
  "acl": {
    "test_runner": ["read:code", "read:data", "write:test_results"],
    "agent": ["read:code", "read:test_results"],
    "human_reviewer": ["read:all"]
  },
  "replayable": true,
  "destroy_and_rebuild_tested": true,
  "lease": {
    "start": "2026-08-29T11:30:00Z",
    "end": "2026-09-12T23:59:59Z",
    "auto_destroy": true
  },
  "dcs_receipt": {
    "solver_version": "env-validate-v1",
    "input_hash": "sha256:...",
    "signature": "sha256:..."
  }
}
```

---
