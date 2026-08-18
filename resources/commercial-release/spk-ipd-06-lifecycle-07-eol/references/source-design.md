## ACT-03-07-07 EOL Assessment

### 基础信息

| 字段 | 内容 |
|---|---|
| **Activity ID** | ACT-03-07-07 |
| **Activity 名称** | EOL Assessment（退市评估） |
| **所属阶段** | 生命周期管理 |
| **Lead Agent 角色** | Lifecycle Orchestrator |
| **执行位置** | task_system |
| **是否使用 Worker Agent** | ✓（EOL Analyst 生成情景和影响 + Migration Readiness 检测就绪度） |
| **是否需要 Independent Verifier** | ✓（DCS 计算合同义务/版本人口/迁移容量/财务区间 + LMT/Legal/Sales 评估） |
| **所需模型能力** | CROSS_SOURCE_REASONING + STRUCTURED_DRAFTING |
| **所需 Skill** | ipd-lifecycle-eol / spk-eol-analyst / spk-migration-readiness |
| **所需 Tool/MCP** | mcp-spk-os-artifact-query / mcp-contract-query / mcp-crm-query / mcp-finance-query |
| **所需 Knowledge** | KN-IPD-EOL-SCHEMA-083 / KN-ISO-20000-EOL-084 |
| **所需 Information** | INFO-QUARTERLY-VALUE-REVIEW（季度价值评审）/ INFO-CONTRACTS（合同状态）/ INFO-MIGRATION-READINESS（迁移就绪度）/ INFO-VERSION-GENEALOGY（版本族谱） |
| **可读取的 Memory 范围** | project:spk-os-ipd, agent_role:lifecycle, capability:cross_source_reasoning |
| **人工责任** | LMT Lead 审核 EOL 评估；法务/安全/客户成功确认无遗漏义务；IPMT 决定 |
| **失败处理** | 客户映射缺失重取 CRM；仍缺标未知且不决策；法律/安全不可接受风险升级 IPMT/Legal；否决时回滚 EOL 候选状态 |
| **对应 Flowable 节点** | task_eol_assessment (ipd-lifecycle.bpmn) |
| **证据来源** | RunReceipt + EOLAssessment + ArtifactManifest |

### 资源与工具详情

#### 工具调用顺序与效率策略

```
Step 1: 盘点版本/客户/合同/漏洞/成本
├─ 从版本族谱中提取目标版本的所有信息：
│  ├─ 活跃客户数量和分布（按 cohort、地区、行业）
│  ├─ 合同状态（活跃合同数量、到期日期、EOL 条款）
│  ├─ 漏洞状态（未修复 CVE 数量和严重度）
│  ├─ 依赖状态（EOL 依赖数量和替代方案）
│  ├─ 维护成本（人力 + 基础设施 + 许可证）
│  └─ 收入趋势（MRR 变化、客户流失率）
└─ 输出：版本盘点报告

Step 2: 构造三情景
├─ EOL Analyst 构造三个情景：
│  ├─ 继续（Continue）：
│  │  ├─ 维持当前支持水平
│  │  ├─ 继续修复漏洞和缺陷
│  │  ├─ 估算未来 12 个月成本
│  │  └─ 估算未来 12 个月收入
│  ├─ 收缩（Shrink）：
│  │  ├─ 降低支持水平（仅安全补丁）
│  │  ├─ 冻结新功能
│  │  ├─ 估算节省的成本
│  │  └─ 估算收入影响
│  └─ 退市（Retire）：
│      ├─ 停止所有支持
│      ├─ 客户迁移到替代产品
│      ├─ 估算迁移成本
│      ├─ 估算合同终止成本
│      └─ 估算知识归档成本
├─ DCS 计算每个情景的财务区间：
│  ├─ 调用 DCS POST /api/v1/eol-scenario
│  │  ├─ 输入：三情景的成本/收入/风险数据
│  │  ├─ 计算 NPV（净现值）对比
│  │  ├─ 计算敏感性区间
│  │  ├─ 输出：情景对比报告 + 签名
│  │  └─ 签名：sha256(...)
│  └─ 输出：DCS 情景报告
└─ 输出：三情景分析

Step 3: 迁移就绪度评估
├─ 调用 spk-migration-readiness.assess(target_version, alternative_product, customers)
│  ├─ 检查：
│  │  □ 替代产品是否可用且功能覆盖 ≥80%
│  │  □ 迁移工具是否存在且已测试
│  │  □ 数据导出方案是否可行
│  │  □ 客户通知期是否满足 ≥90 天（或合同约定，取较大值）
│  │  □ 合同终止条款是否明确
│  │  □ 法规保留期是否满足
│  │  □ 关键客户（年合同 > $100K）是否有 1v1 迁移支持计划
│  └─ 输出：迁移就绪度报告
└─ 输出：MigrationReadinessReport

Step 4: 核对通知义务
├─ DCS 校验合同通知义务：
│  ├─ 遍历所有活跃合同
│  ├─ 提取 EOL 通知条款
│  ├─ 计算最短通知期（取 max(90 天, 合同约定)）
│  ├─ 检查法规保留期（数据保留、服务连续性）
│  └─ 输出：通知义务清单
└─ 输出：通知义务清单

Step 5: LMT/Legal/Sales 评估
├─ LMT Lead 审查 EOL 评估报告
├─ Legal 确认：
│  ├─ 合同终止条款合规
│  ├─ 通知期满足法律要求
│  ├─ 数据处置合规
│  └─ 签署法律意见
├─ Sales/Customer Success 确认：
│  ├─ 客户迁移方案可行
│  ├─ 关键客户逐户具名
│  ├─ 签署客户影响意见
│  └─ 漏签义务阻止提交 LDCP
├─ Security 确认：
│  ├─ 安全影响已评估
│  └─ 签署安全意见
└─ 输出：EOLAssessment（已签署）
```

### 输出 Artifact 模板

#### Artifact 名称
`EOLAssessment`

#### EOLAssessment Schema

```json
{
  "assessment_id": "eol-20270115-001",
  "version": "1.0.0",
  "product_id": "aurora",
  "target_version": "1.x",
  "created_at": "2027-01-15T10:00:00Z",
  "signed_by": [
    {"user_id": "user-lmt-lead-001", "role": "LMT Lead", "signed_at": "2027-01-15T14:00:00Z"},
    {"user_id": "user-legal-001", "role": "Legal Counsel", "signed_at": "2027-01-15T15:00:00Z"},
    {"user_id": "user-cs-lead-001", "role": "Customer Success Lead", "signed_at": "2027-01-15T15:30:00Z"},
    {"user_id": "user-security-lead-001", "role": "Security Lead", "signed_at": "2027-01-15T16:00:00Z"}
  ],
  "version_inventory": {
    "active_customers": 45,
    "cohort_distribution": {
      "enterprise-devops": 15,
      "sme-developer": 20,
      "independent-contributor": 10
    },
    "active_contracts": 12,
    "contracts_with_eol_clause": 10,
    "open_cves": {"critical": 0, "high": 1, "medium": 5},
    "eol_dependencies": 2,
    "monthly_maintenance_cost_usd": 8000,
    "monthly_revenue_usd": 30000
  },
  "scenarios": {
    "continue": {
      "description": "维持当前支持水平 12 个月",
      "cost_12m_usd": 96000,
      "revenue_12m_usd": 360000,
      "npv_usd": 264000,
      "risks": ["依赖 EOL 风险增加", "安全债务累积"]
    },
    "shrink": {
      "description": "仅安全补丁，冻结新功能",
      "cost_12m_usd": 48000,
      "revenue_12m_usd": 240000,
      "npv_usd": 192000,
      "risks": ["客户流失加速", "合同违约风险"]
    },
    "retire": {
      "description": "停止支持，迁移客户到 v2.x",
      "migration_cost_usd": 50000,
      "contract_termination_cost_usd": 20000,
      "archival_cost_usd": 5000,
      "total_cost_usd": 75000,
      "revenue_lost_12m_usd": 360000,
      "npv_comparison": "退市 NPV 比继续高 $180K（3 年期）",
      "risks": ["客户不满", "迁移失败"]
    }
  },
  "migration_readiness": {
    "alternative_available": true,
    "alternative_product": "aurora v2.x",
    "feature_coverage_pct": 92,
    "migration_tool_available": true,
    "migration_tool_tested": true,
    "data_export_feasible": true,
    "notification_period_days": 90,
    "key_customers_with_1v1_support": 3
  },
  "notification_obligations": {
    "min_notification_days": 90,
    "contracts_requiring_notice": 12,
    "regulatory_retention_days": 365,
    "data_disposition_requirements": ["客户数据导出", "90 天后删除", "审计日志保留 7 年"]
  },
  "recommendation": "retire",
  "dcs_receipt": {
    "solver_version": "eol-scenario-v1",
    "input_hash": "sha256:...",
    "signature": "sha256:..."
  }
}
```

---
