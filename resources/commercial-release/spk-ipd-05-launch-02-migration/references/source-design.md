## ACT-03-06-02 配置与数据迁移验证

### 基础信息

| 字段 | 内容 |
|---|---|
| **Activity ID** | ACT-03-06-02 |
| **Activity 名称** | 配置与数据迁移验证 |
| **所属阶段** | 发布 |
| **Lead Agent 角色** | Release Orchestrator |
| **执行位置** | task_system |
| **是否使用 Worker Agent** | ✓（Migration Analyst 分析影响 + Migration Validator 执行测试） |
| **是否需要 Independent Verifier** | ✓（DCS 数据一致性校验 + DBA/Data Owner 双签） |
| **所需模型能力** | CROSS_SOURCE_REASONING + STRUCTURED_DRAFTING |
| **所需 Skill** | ipd-release-migration / spk-migration-analyst / spk-migration-validator |
| **所需 Tool/MCP** | mcp-spk-os-artifact-query / mcp-database-query / mcp-filesystem-read |
| **所需 Knowledge** | KN-IPD-MIGRATION-SCHEMA-058 / KN-DATABASE-MIGRATION-PATTERNS-059 |
| **所需 Information** | INFO-RELEASE-MANIFEST（已签署 Manifest）/ INFO-STAGING-ENV（staging 环境）/ INFO-PROD-SCHEMA（生产 schema） |
| **可读取的 Memory 范围** | project:spk-os-ipd, agent_role:release, capability:cross_source_reasoning |
| **人工责任** | DBA/Platform 审核迁移方案、执行回滚演练；Data Owner 授权数据使用 |
| **失败处理** | 基础设施失败在干净副本重试一次；数据不一致不得继续；丢数/驻留异常升级 DBA/Security；回滚到迁移前快照并验证恢复 |
| **对应 Flowable 节点** | task_config_verify (ipd-release.bpmn) |
| **证据来源** | RunReceipt + MigrationVerificationPack + ArtifactManifest |

### 资源与工具详情

#### 输入资源

| 资源 | 来源 | 获取方式 | 质量要求 |
|---|---|---|---|
| ReleaseManifest | ACT-03-06-01 产物 | mcp-spk-os-artifact-query.get() | Release Manager 签署 |
| 迁移脚本 | Manifest.configuration.migration_scripts | mcp-filesystem-read() | 含 digest |
| 生产 schema | 数据库 | mcp-database-query.get_schema(prod) | 当前版本 |
| staging 环境 | DevOps | mcp-spk-os-env-query.get(staging) | 生产等价 |
| 数据授权 | Data Owner | mcp-spk-os-artifact-query.get(data_authorization) | 含脱敏规则 |

#### 工具调用顺序与效率策略

```
Step 1: 固定源/目标 schema
├─ 从 Manifest 中提取当前配置版本和目标配置版本
├─ 从生产数据库获取当前 schema（源）
├─ 从迁移脚本解析目标 schema
├─ 计算 schema diff
└─ 输出：Schema Diff 报告

Step 2: Migration Analyst 分析影响
├─ 调用 spk-migration-analyst.analyze(schema_diff, migration_scripts)
│  ├─ 分析迁移影响：
│  │  ├─ 受影响的表和列
│  │  ├─ 数据量估算
│  │  ├─ 迁移时间估算
│  │  ├─ 兼容性窗口（旧客户端是否兼容新 schema）
│  │  └─ 风险点（不可逆操作、大表迁移、索引重建）
│  ├─ 生成迁移测试用例候选：
│  │  ├─ 正向迁移测试（空库/小库/大库）
│  │  ├─ 回滚测试（迁移后回滚）
│  │  ├─ 兼容性测试（旧客户端访问新 schema）
│  │  ├─ 性能测试（迁移后查询性能）
│  │  └─ 数据完整性测试（行数/校验和/约束）
│  └─ 输出：迁移影响分析 + 测试用例候选
└─ 输出：MigrationImpactAnalysis

Step 3: 准备脱敏副本
├─ 从生产数据库创建脱敏副本（staging 环境）
│  ├─ 应用脱敏规则（PII 替换、数值扰动）
│  ├─ 保留数据分布特征
│  ├─ 记录脱敏规则 hash
│  └─ 输出：脱敏副本
├─ Data Owner 授权脱敏副本使用
└─ 输出：DataAuthorizationRecord

Step 4: Migration Validator 执行迁移测试
├─ 调用 spk-migration-validator.execute(migration_scripts, test_cases, staging_env)
│  ├─ 4.1 备份当前状态
│  │  ├─ 创建迁移前快照
│  │  ├─ 记录行数/校验和/约束
│  │  └─ 输出：迁移前快照
│  ├─ 4.2 执行 dry-run（不实际修改）
│  │  ├─ 模拟迁移执行
│  │  ├─ 检查 SQL 语法和逻辑
│  │  └─ 输出：dry-run 报告
│  ├─ 4.3 执行实际迁移
│  │  ├─ 在脱敏副本上执行迁移脚本
│  │  ├─ 记录执行时间和日志
│  │  └─ 输出：迁移执行报告
│  ├─ 4.4 核对数据完整性
│  │  ├─ 调用 DCS POST /api/v1/migration-verify
│  │  │  ├─ 行数校验（迁移前后行数对比）
│  │  │  ├─ 校验和比对（关键列的 checksum）
│  │  │  ├─ 约束验证（外键、唯一性、非空）
│  │  │  ├─ 输出：数据完整性报告 + 签名
│  │  │  └─ 签名：sha256(...)
│  │  └─ 输出：数据完整性报告
│  ├─ 4.5 演练回滚
│  │  ├─ 执行回滚脚本（如有）
│  │  ├─ 验证回滚后数据与迁移前一致
│  │  ├─ 记录回滚时间
│  │  ├─ 如果 rollback_safe=false，验证前向修复方案
│  │  └─ 输出：回滚演练报告
│  ├─ 4.6 性能与驻留检查
│  │  ├─ 执行关键查询性能测试
│  │  ├─ 检查数据驻留合规（不跨区域）
│  │  └─ 输出：性能与驻留报告
│  └─ 输出：MigrationVerificationPack（草稿）
└─ 输出：MigrationVerificationPack（草稿）

Step 5: DBA + Data Owner 双签
├─ DBA 审查：
│  ├─ 迁移影响分析
│  ├─ 迁移执行报告
│  ├─ 数据完整性报告
│  ├─ 回滚演练报告
│  └─ 签署迁移方案
├─ Data Owner 审查：
│  ├─ 脱敏规则有效性
│  ├─ 数据驻留合规
│  └─ 签署数据授权
└─ 输出：MigrationVerificationPack（已签署）
```

#### 效率优化策略

| 策略 | 说明 | 预期效果 |
|---|---|---|
| 脱敏副本 | 使用脱敏数据而非生产数据 | 保护隐私 + 可重复测试 |
| dry-run | 先模拟再实际执行 | 提前发现语法和逻辑错误 |
| DCS 数据完整性校验 | 确定性计算行数/校验和/约束 | 消除人工检查遗漏 |
| 回滚演练 | 实际执行回滚并验证 | 确保回滚可行 |

### 输出 Artifact 模板

#### Artifact 名称
`MigrationVerificationPack`

#### 文件结构
```
migration_verification_pack/
├── manifest.yaml
├── pack.json                     # 主数据文件
├── schema_diff.json              # Schema 差异
├── impact_analysis.json          # 影响分析
├── test_results/                 # 测试结果
│   ├── dry_run.json
│   ├── migration_execution.json
│   ├── data_integrity.json
│   ├── rollback_rehearsal.json
│   └── performance.json
├── dcs_receipts/
│   └── migration_verify.json
└── README.md
```

#### pack.json Schema

```json
{
  "pack_id": "mvp-20260910-001",
  "version": "1.0.0",
  "created_at": "2026-09-10T12:00:00Z",
  "created_by": "agent-migration-validator-001",
  "signed_by": [
    {"user_id": "user-dba-001", "role": "DBA", "signed_at": "2026-09-10T14:00:00Z"},
    {"user_id": "user-data-owner-001", "role": "Data Owner", "signed_at": "2026-09-10T14:30:00Z"}
  ],
  "manifest_ref": "rel-20260910-001",
  "migration_scripts": [
    {
      "script": "migrate_v1_to_v2.sql",
      "digest": "sha256:m3n4o5p6q7r8...",
      "rollback_safe": true,
      "forward_fix_available": true
    }
  ],
  "schema_diff": {
    "tables_added": 2,
    "tables_modified": 5,
    "columns_added": 12,
    "columns_modified": 3,
    "indexes_added": 4,
    "indexes_removed": 1
  },
  "impact_analysis": {
    "affected_tables": ["users", "reviews", "code_diffs"],
    "estimated_data_volume_gb": 15.3,
    "estimated_migration_time_min": 8,
    "compatibility_window": ">=1.0.0",
    "risk_points": [
      {
        "risk": "users 表新增 email_verified 列需要回填",
        "mitigation": "默认值 false，后台异步验证"
      }
    ]
  },
  "test_results": {
    "dry_run": {
      "status": "pass",
      "sql_syntax_valid": true,
      "logic_issues": 0
    },
    "migration_execution": {
      "status": "pass",
      "duration_sec": 485,
      "rows_migrated": 1250000,
      "errors": 0
    },
    "data_integrity": {
      "status": "pass",
      "row_count_match": true,
      "checksum_match": true,
      "constraints_valid": true,
      "dcs_signature": "sha256:..."
    },
    "rollback_rehearsal": {
      "status": "pass",
      "rollback_duration_sec": 120,
      "data_restored": true,
      "checksum_after_rollback": "sha256:...",
      "matches_pre_migration": true
    },
    "performance": {
      "status": "pass",
      "key_queries": [
        {"query": "SELECT * FROM users WHERE id = ?", "before_ms": 5, "after_ms": 6, "regression_pct": 20},
        {"query": "SELECT * FROM reviews WHERE user_id = ?", "before_ms": 12, "after_ms": 11, "regression_pct": -8}
      ],
      "overall_regression_pct": 5
    }
  },
  "data_authorization": {
    "authorization_ref": "auth-20260910-001",
    "desensitization_rules_hash": "sha256:...",
    "data_residency_compliant": true,
    "authorized_by": "user-data-owner-001"
  },
  "conclusion": {
    "migration_safe": true,
    "rollback_tested": true,
    "performance_acceptable": true,
    "recommendation": "proceed"
  }
}
```

### 节点关联与展示

#### 在整体流程中的位置

```
ACT-03-06-01 Release Manifest → ACT-03-06-02 配置与数据迁移验证
                                        ↓
                                MigrationVerificationPack
                                        ↓
                                [与 ACT-03-06-03 并行]
                                        ↓
                                ACT-03-06-04 渐进发布
```

---
