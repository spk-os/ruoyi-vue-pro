## ACT-03-07-09 Customer Migration Execution

### 基础信息

| 字段 | 内容 |
|---|---|
| **Activity ID** | ACT-03-07-09 |
| **Activity 名称** | Customer Migration Execution（客户迁移执行） |
| **所属阶段** | 生命周期管理 |
| **Lead Agent 角色** | Lifecycle Orchestrator |
| **执行位置** | task_system |
| **是否使用 Worker Agent** | ✓（Migration Advisor 定制方案 + Migration Tracker 追踪进度） |
| **是否需要 Independent Verifier** | ✓（DCS 校验资格/通知期限/容量/数据校验 + Customer Success/Legal/客户授权人批准） |
| **所需模型能力** | STRUCTURED_DRAFTING + GROUNDED_EXTRACTION |
| **所需 Skill** | ipd-lifecycle-migration / spk-migration-advisor / spk-migration-tracker |
| **所需 Tool/MCP** | mcp-crm-query / mcp-contract-query / mcp-spk-os-artifact-query |
| **所需 Knowledge** | KN-IPD-MIGRATION-EXEC-SCHEMA-087 |
| **所需 Information** | INFO-LDCP-DECISION（LDCP 决策记录）/ INFO-MIGRATION-PLAN（迁移方案）/ INFO-CUSTOMER-LIST（客户清单） |
| **可读取的 Memory 范围** | project:spk-os-ipd, agent_role:lifecycle, capability:structured_drafting |
| **人工责任** | Customer Success 客户沟通和迁移支持；客户授权人验收迁移；Legal 确认合同合规 |
| **失败处理** | 单客户技术失败按批准 Runbook 重试；仍失败保持旧版支持；数据差异/重大投诉升级 Incident/Legal；回滚该客户到源版本 |
| **对应 Flowable 节点** | task_customer_migration (ipd-lifecycle.bpmn) |
| **证据来源** | RunReceipt + CustomerMigrationLedger + ArtifactManifest |

### 资源与工具详情

#### 工具调用顺序与效率策略

```
Step 1: 锁定 LDCP 条件
├─ 从 LDCPDecisionRecord 中提取迁移条件
├─ 确认 EOS/EoS/EOL 日期
├─ 确认通知期限要求
└─ 输出：迁移约束清单

Step 2: 客户/版本分群
├─ 从 CRM 中提取所有受影响客户
├─ 按 cohort 分群：
│  ├─ Wave 1：内部用户 + 设计合作伙伴（低风险，快速验证）
│  ├─ Wave 2：中小企业客户（标准迁移流程）
│  ├─ Wave 3：企业客户（1v1 迁移支持）
│  └─ Wave 4：特殊合同客户（需 Legal 逐户确认）
├─ 对每个客户记录：
│  ├─ 当前版本
│  ├─ 目标版本
│  ├─ 合同到期日
│  ├─ 通知期限要求
│  └─ 特殊需求
└─ 输出：客户分群清单

Step 3: 核对合同通知
├─ DCS 校验每个客户的通知期限：
│  ├─ 调用 DCS POST /api/v1/migration-notice-check
│  │  ├─ 输入：customer_id, contract_eol_clause, planned_notification_date
│  │  ├─ 检查：通知期 ≥ max(90 天, 合同约定)
│  │  ├─ 输出：合规/不合规 + 最早可通知日期
│  │  └─ 签名：sha256(...)
│  └─ 不合规的客户延迟到满足通知期后
└─ 输出：通知合规报告

Step 4: 演练技术迁移/回滚
├─ 在 staging 环境演练：
│  ├─ 数据导出（从 v1.x 导出）
│  ├─ 数据导入（到 v2.x 导入）
│  ├─ 数据校验（行数/校验和/关键业务数据比对）
│  ├─ 回滚演练（从 v2.x 回退到 v1.x）
│  └─ 记录演练时间和问题
└─ 输出：迁移演练报告

Step 5: 批准通信
├─ Migration Advisor 生成客户通信草案：
│  ├─ 退市通知邮件（含时间线、迁移指南、支持联系方式）
│  ├─ 迁移指南文档（步骤、FAQ、已知问题）
│  ├─ 定制建议（根据客户使用场景）
│  └─ 输出：通信草案
├─ Product/Marketing/Legal 审阅通信
├─ 批准通信内容
└─ 输出：已批准通信

Step 6: 分波执行迁移
├─ Wave 1（内部 + 设计合作伙伴）：
│  ├─ 发送通知
│  ├─ 提供迁移工具和指导
│  ├─ 客户执行迁移
│  ├─ 数据校验
│  ├─ 客户验收签署
│  └─ 记录迁移状态
├─ Wave 2-4 依次执行
├─ 每个客户：
│  ├─ 客户授权人验收迁移
│  ├─ 数据校验通过
│  ├─ 签署迁移完成确认
│  └─ **AI 不代客户同意**
├─ 拒绝迁移的客户进入人工例外流程
└─ 输出：CustomerMigrationLedger

Step 7: 关闭
├─ 确认所有客户迁移完成或进入例外流程
├─ 确认通知期已满
├─ 确认数据处置合规
├─ 停止旧版本服务
└─ 输出：迁移完成报告
```

### 输出 Artifact 模板

#### Artifact 名称
`CustomerMigrationLedger`

#### CustomerMigrationLedger Schema

```json
{
  "ledger_id": "cml-20270401-001",
  "version": "1.0.0",
  "ldcp_ref": "ldcp-20270201-001",
  "created_at": "2027-04-01T10:00:00Z",
  "waves": [
    {
      "wave_id": 1,
      "name": "内部 + 设计合作伙伴",
      "start_date": "2027-04-01",
      "end_date": "2027-04-15",
      "customers": [
        {
          "customer_id": "CUST-001",
          "customer_name": "Internal Dev Team",
          "cohort": "internal",
          "source_version": "1.9.5",
          "target_version": "2.0.1",
          "contract_expiry": "N/A",
          "notification_sent_at": "2027-04-01T09:00:00Z",
          "notification_period_days": 90,
          "notification_compliant": true,
          "migration_status": "completed",
          "migration_completed_at": "2027-04-05T14:00:00Z",
          "data_validation": {
            "row_count_match": true,
            "checksum_match": true,
            "business_data_verified": true
          },
          "customer_sign_off": {
            "user_id": "user-internal-dev-lead",
            "signed_at": "2027-04-05T15:00:00Z"
          },
          "issues": [],
          "rollback_available": true
        }
      ]
    }
  ],
  "statistics": {
    "total_customers": 45,
    "migrated": 38,
    "pending": 5,
    "refused": 2,
    "migration_rate_pct": 84.4,
    "exceptions": [
      {
        "customer_id": "CUST-040",
        "reason": "合同到期自然结束（2027-06-30）",
        "status": "contract_expiry"
      },
      {
        "customer_id": "CUST-041",
        "reason": "客户要求延期，已安排 1v1 支持",
        "status": "deferred_with_support"
      }
    ]
  },
  "dcs_receipt": {
    "solver_version": "migration-notice-check-v1",
    "signature": "sha256:..."
  }
}
```

---
