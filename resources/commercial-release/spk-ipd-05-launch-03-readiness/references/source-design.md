## ACT-03-06-03 运营就绪验证

### 基础信息

| 字段 | 内容 |
|---|---|
| **Activity ID** | ACT-03-06-03 |
| **Activity 名称** | 运营就绪验证 |
| **所属阶段** | 发布 |
| **Lead Agent 角色** | Release Orchestrator |
| **执行位置** | task_system |
| **是否使用 Worker Agent** | ✓（Readiness Analyst 汇总缺口 + Observability Validator 验证可观测性） |
| **是否需要 Independent Verifier** | ✓（DCS 校验 SLO/告警/Runbook/轮值/权限 + SRE Lead 签署） |
| **所需模型能力** | GROUNDED_EXTRACTION + STRUCTURED_DRAFTING |
| **所需 Skill** | ipd-release-readiness / spk-readiness-analyst / spk-observability-validator |
| **所需 Tool/MCP** | mcp-spk-os-artifact-query / mcp-monitoring-query / mcp-filesystem-read |
| **所需 Knowledge** | KN-IPD-READINESS-SCHEMA-060 / KN-SRE-PRACTICES-061 / KN-RUNBOOK-TEMPLATES-062 |
| **所需 Information** | INFO-RELEASE-MANIFEST（已签署 Manifest）/ INFO-SLO-DEFINITION（SLO 定义）/ INFO-ALERT-RULES（告警规则）/ INFO-RUNBOOKS（Runbook）/ INFO-ON-CALL（On-Call 排班） |
| **可读取的 Memory 范围** | project:spk-os-ipd, agent_role:release, capability:grounded_extraction |
| **人工责任** | SRE Lead 确认运营就绪、签署 Runbook；LMT Lead 确认接收能力 |
| **失败处理** | 演练环境故障重试一次；告警缺口降级 not_ready；无 owner/关键容量风险升级 LMT/IPMT；回滚就绪状态并恢复旧 Runbook |
| **对应 Flowable 节点** | task_ops_ready (ipd-release.bpmn) |
| **证据来源** | RunReceipt + OperationalReadinessRecord + ArtifactManifest |

### 资源与工具详情

#### 工具调用顺序与效率策略

```
Step 1: 盘点运营材料
├─ Readiness Analyst 盘点：
│  ├─ SLO 定义（可用性、延迟、错误率）
│  ├─ 告警规则（覆盖所有 SLO）
│  ├─ Runbook（服务重启、数据库回滚、紧急扩容等）
│  ├─ 容量规划（当前容量 vs 预期负载）
│  ├─ On-Call 排班（轮值周期、当前 on-call、升级路径）
│  ├─ Dashboard（SLO Dashboard、Canary Dashboard）
│  ├─ 客户支持材料（FAQ、培训材料、升级路径）
│  └─ 合规材料（许可证、出口、隐私、数据驻留）
├─ 标注缺失项和缺口
└─ 输出：运营材料盘点报告

Step 2: Observability Validator 验证可观测性
├─ 调用 spk-observability-validator.validate(slo, alerts, dashboards)
│  ├─ 注入测试告警（5 个）
│  │  ├─ 模拟 SLO 跌破阈值
│  │  ├─ 验证告警是否正确触发
│  │  ├─ 验证告警路由是否正确（L1/L2/L3）
│  │  └─ 输出：告警注入测试结果
│  ├─ 验证 Dashboard
│  │  ├─ SLO Dashboard 是否展示所有 SLO
│  │  ├─ Canary Dashboard 是否展示护栏指标
│  │  └─ 输出：Dashboard 验证结果
│  ├─ 检查日志管道
│  │  ├─ 日志是否正确采集
│  │  ├─ 日志是否正确索引
│  │  └─ 输出：日志管道验证结果
│  └─ 输出：可观测性验证报告
└─ 输出：ObservabilityValidationReport

Step 3: 执行故障演练
├─ 选择 3 个 P1 场景进行故障演练：
│  ├─ 场景 1：数据库主节点故障
│  │  ├─ 触发故障（kill 主节点进程）
│  │  ├─ 验证自动故障转移
│  │  ├─ 验证告警触发
│  │  ├─ 验证 Runbook 步骤可执行
│  │  └─ 记录恢复时间
│  ├─ 场景 2：应用服务 OOM
│  │  ├─ 触发故障（限制内存）
│  │  ├─ 验证自动重启
│  │  ├─ 验证告警触发
│  │  └─ 记录恢复时间
│  └─ 场景 3：网络分区
│      ├─ 触发故障（iptables 规则）
│      ├─ 验证降级策略
│      ├─ 验证告警触发
│      └─ 记录恢复时间
├─ 记录演练证据（截图、日志、时间戳）
└─ 输出：故障演练报告

Step 4: DCS 校验
├─ 调用 DCS POST /api/v1/readiness-validate
│  ├─ 检查：
│  │  □ SLO 定义完整（含目标值、测量方法、错误预算）
│  │  □ 告警规则覆盖所有 SLO
│  │  □ Runbook 覆盖所有 P1 场景
│  │  □ On-Call 排班已确认（含升级路径）
│  │  □ 容量规划已审核（当前容量 ≥ 预期负载 * 1.5）
│  │  □ 客户支持材料已审核
│  │  □ 合规材料已签署
│  ├─ 输出：DCS 校验报告 + 签名
│  └─ 签名：sha256(...)
└─ 输出：DCS 校验报告

Step 5: SRE Lead + LMT Lead 签署
├─ SRE Lead 审查：
│  ├─ 运营材料盘点报告
│  ├─ 可观测性验证报告
│  ├─ 故障演练报告
│  ├─ DCS 校验报告
│  ├─ 签署运营就绪
│  └─ 签署 Runbook
├─ LMT Lead 审查：
│  ├─ 确认接收能力（人员、技能、工具）
│  ├─ 确认培训完成
│  └─ 签署接收确认
└─ 输出：OperationalReadinessRecord（已签署）
```

### 输出 Artifact 模板

#### Artifact 名称
`OperationalReadinessRecord`

#### OperationalReadinessRecord Schema

```json
{
  "record_id": "orr-20260910-001",
  "version": "1.0.0",
  "created_at": "2026-09-10T13:00:00Z",
  "signed_by": [
    {"user_id": "user-sre-lead-001", "role": "SRE Lead", "signed_at": "2026-09-10T16:00:00Z"},
    {"user_id": "user-lmt-lead-001", "role": "LMT Lead", "signed_at": "2026-09-10T16:30:00Z"}
  ],
  "manifest_ref": "rel-20260910-001",
  "slo": {
    "definitions": [
      {
        "name": "availability",
        "target": 0.999,
        "measurement": "successful_requests / total_requests",
        "window": "30d",
        "error_budget": 0.001
      },
      {
        "name": "latency_p99",
        "target_ms": 500,
        "measurement": "p99(response_time_ms)",
        "window": "5m"
      },
      {
        "name": "error_rate",
        "target": 0.005,
        "measurement": "error_requests / total_requests",
        "window": "5m"
      }
    ],
    "dashboard_ref": "grafana://dashboard/slo-aurora"
  },
  "alerts": {
    "total_rules": 12,
    "coverage_pct": 100,
    "injection_test": {
      "total_injected": 5,
      "triggered_correctly": 5,
      "routing_correct": 5
    },
    "alert_rules_ref": "prometheus://rules/aurora-alerts.yaml"
  },
  "runbooks": {
    "total": 8,
    "p1_scenarios_covered": 3,
    "runbook_list": [
      {"name": "服务重启", "uri": "runbook://aurora/restart", "tested": true},
      {"name": "数据库回滚", "uri": "runbook://aurora/db-rollback", "tested": true},
      {"name": "紧急扩容", "uri": "runbook://aurora/scale-up", "tested": true},
      {"name": "缓存清理", "uri": "runbook://aurora/cache-clear", "tested": false},
      {"name": "日志排查", "uri": "runbook://aurora/log-investigation", "tested": false}
    ]
  },
  "capacity": {
    "current_capacity_rps": 1000,
    "expected_load_rps": 500,
    "headroom_pct": 100,
    "scaling_strategy": "auto-scaling based on CPU/memory"
  },
  "on_call": {
    "rotation": "weekly",
    "current_on_call": "user-zhang-san",
    "escalation_path": [
      {"level": "L1", "contact": "on-call engineer", "sla_min": 15},
      {"level": "L2", "contact": "SRE Lead", "sla_min": 30},
      {"level": "L3", "contact": "CTO", "sla_min": 60}
    ],
    "schedule_ref": "pagerduty://schedule/aurora-sre"
  },
  "fault_drill": {
    "scenarios_tested": 3,
    "results": [
      {
        "scenario": "数据库主节点故障",
        "recovery_time_sec": 45,
        "alert_triggered": true,
        "runbook_executable": true
      },
      {
        "scenario": "应用服务 OOM",
        "recovery_time_sec": 30,
        "alert_triggered": true,
        "runbook_executable": true
      },
      {
        "scenario": "网络分区",
        "recovery_time_sec": 120,
        "alert_triggered": true,
        "runbook_executable": true
      }
    ]
  },
  "customer_support": {
    "faq_uri": "https://docs.spk-os.io/aurora/faq",
    "training_completed": true,
    "training_date": "2026-09-08",
    "escalation_paths": [
      {"trigger": "P0 incident", "contact": "SRE on-call", "sla_min": 15},
      {"trigger": "P1 incident", "contact": "SRE Lead", "sla_min": 30},
      {"trigger": "customer complaint", "contact": "Customer Success", "sla_hour": 4}
    ]
  },
  "compliance": {
    "license_compliance": "pass",
    "export_compliance": "pass",
    "privacy_compliance": "pass",
    "data_residency_compliance": "pass"
  },
  "dcs_receipt": {
    "solver_version": "readiness-validate-v1",
    "input_hash": "sha256:...",
    "signature": "sha256:..."
  },
  "conclusion": {
    "readiness_status": "ready",
    "exceptions": [],
    "recommendation": "proceed_to_rollout"
  }
}
```

---
