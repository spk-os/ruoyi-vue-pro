## ACT-03-06-06 GA 决策、移交与稳定观察

### 基础信息

| 字段 | 内容 |
|---|---|
| **Activity ID** | ACT-03-06-06 |
| **Activity 名称** | GA 决策、移交与稳定观察 |
| **所属阶段** | 发布 |
| **Lead Agent 角色** | Release Orchestrator |
| **执行位置** | task_system |
| **是否使用 Worker Agent** | ✓（Handoff Agent 生成清单 + Stability Monitor Agent 监控稳定期） |
| **是否需要 Independent Verifier** | ✓（DCS 校验 GA 权限/移交字段/SLO/观察窗口 + GA Authority + PDT/LMT 双方签署） |
| **所需模型能力** | STRUCTURED_DRAFTING + GROUNDED_EXTRACTION |
| **所需 Skill** | ipd-release-ga-handoff / spk-handoff-agent / spk-stability-monitor |
| **所需 Tool/MCP** | mcp-spk-os-artifact-query / mcp-monitoring-query / mcp-filesystem-read |
| **所需 Knowledge** | KN-IPD-GA-HANDOFF-SCHEMA-067 / KN-SLO-MONITORING-068 |
| **所需 Information** | INFO-TR6-OUTPUT（TR6 决策包）/ INFO-SLO-BASELINE（SLO 基线）/ INFO-KNOWN-ISSUES（Known Issues） |
| **可读取的 Memory 范围** | project:spk-os-ipd, agent_role:release, capability:structured_drafting |
| **人工责任** | GA Authority 签署 GA 决定；PDT Lead 和 LMT Lead 双方签署移交协议；LMT Lead 确认稳定期结束 |
| **失败处理** | 清单生成失败降级人工模板；未接收项保持 PDT 责任；稳定期严重回归升级 Incident/IPMT；人工否决或护栏破线回滚发布及责任切换 |
| **对应 Flowable 节点** | task_ga_decision + task_handoff + task_stabilization + task_stabilization_confirm (ipd-release.bpmn) |
| **证据来源** | RunReceipt + GA_Handoff_StabilityRecord + ArtifactManifest |

### 资源与工具详情

#### 工具调用顺序与效率策略

```
Step 1: GA 决策
├─ 核对 TR6 条件（如有 Conditional Go）
├─ GA Authority（IPMT）审核：
│  ├─ TR6 决策包
│  ├─ 渐进发布结果
│  ├─ 残余风险报告
│  └─ 移交清单草案
├─ GA Authority 签署 GA 决定：
│  ├─ Go：正式发布
│  ├─ Conditional Go：正式发布，但有附加条件
│  └─ No-Go：延迟发布
├─ **AI 无签署权**
├─ **系统层面禁止 AI 写入 GA 状态**
└─ 输出：GA_Record（已签署）

Step 2: Handoff Agent 生成移交清单
├─ 调用 spk-handoff-agent.generate(manifest, tr6, slo_baseline, known_issues)
│  ├─ 从 Manifest 拉取发布信息
│  ├─ 从稳定期监控拉取 SLO 基线数据
│  ├─ 从 Issue Tracker 拉取 Known Issues
│  ├─ 从 SRE 拉取 Runbook 和 on-call 信息
│  ├─ 生成移交清单 (Handoff Protocol JSON)：
│  │  ├─ SLO 基线（可用性、延迟、错误率）
│  │  ├─ Known Issues（ID、严重度、描述、变通方案、预计修复）
│  │  ├─ Runbook 列表（URI）
│  │  ├─ On-Call 排班（轮值周期、当前 on-call、升级路径）
│  │  ├─ 培训完成状态
│  │  ├─ 客户 FAQ（URI）
│  │  └─ 升级路径（触发条件、联系人、SLA）
│  ├─ 生成 GA 公告草案：
│  │  ├─ 版本号
│  │  ├─ 主要功能
│  │  ├─ 已知限制
│  │  └─ 升级路径
│  └─ 输出：移交清单 + GA 公告草案
└─ 输出：HandoffProtocol（草稿）+ GA_Announcement（草稿）

Step 3: PDT→LMT 移交签署
├─ PDT Lead 和 LMT Lead 逐项确认移交清单：
│  ├─ SLO 基线是否准确
│  ├─ Known Issues 是否完整
│  ├─ Runbook 是否可执行
│  ├─ On-Call 排班是否就绪
│  ├─ 培训是否完成
│  └─ 升级路径是否清晰
├─ PDT Lead 签署移交（交出责任）
├─ LMT Lead 签署接收（接收责任）
├─ **双方签名齐全**
├─ yudao 记录责任转移时点
└─ 输出：HandoffProtocol（已签署）

Step 4: GA 公告发布
├─ 产品/法务/合规/支持审阅 GA 公告
├─ 确认公告内容准确（不夸大 AI 能力，不泄露未发布漏洞）
├─ 发布 GA 公告
└─ 输出：GA_Announcement（已发布）

Step 5: 稳定观察期（7-14 天）
├─ Stability Monitor Agent 持续监控：
│  ├─ SLO 指标（可用性、延迟、错误率）
│  ├─ 告警触发情况
│  ├─ 客户反馈（工单、NPS）
│  ├─ 安全事件
│  └─ 性能趋势
├─ 每日生成稳定报告：
│  ├─ SLO 达标率
│  ├─ 事件数量和严重度
│  ├─ 客户反馈摘要
│  └─ 趋势分析
├─ 异常时告警并升级：
│  ├─ SLO 跌破目标 → 告警 + 自动创建 incident
│  ├─ P0/P1 事件 → 升级 LMT Lead + SRE Lead
│  └─ 严重回归 → 升级 Incident/IPMT，考虑回滚
└─ 输出：每日稳定报告

Step 6: 稳定期结束确认
├─ LMT Lead 审核稳定报告
├─ 确认稳定期达标条件：
│  ├─ SLO 持续达标（7-14 天）
│  ├─ 0 个 P0/P1 事件（或已妥善处置）
│  ├─ 客户反馈稳定（NPS 不降或上升）
│  └─ 无严重安全事件
├─ LMT Lead 签署稳定期结束
├─ 进入常规运营
├─ 发布阶段正式完成
└─ 输出：StabilityReport（已签署）
```

### 输出 Artifact 模板

#### Artifact 名称
`GA_Handoff_StabilityRecord`

#### GA_Handoff_StabilityRecord Schema

```json
{
  "record_id": "ghs-20260925-001",
  "version": "1.0.0",
  "ga_record": {
    "ga_id": "ga-20260912-001",
    "decision": "go",
    "decided_by": {
      "user_id": "user-ipmt-chair-001",
      "role": "IPMT Chair",
      "signed_at": "2026-09-12T16:00:00Z"
    },
    "conditions": [],
    "announcement": {
      "published_at": "2026-09-12T17:00:00Z",
      "uri": "https://blog.spk-os.io/aurora-2.0-ga",
      "reviewed_by": ["product", "legal", "compliance", "support"]
    }
  },
  "handoff_record": {
    "handoff_id": "ho-20260912-001",
    "from_team": "PDT",
    "to_team": "LMT",
    "slo_baseline": {
      "availability": 0.9993,
      "latency_p50_ms": 180,
      "latency_p99_ms": 335,
      "error_rate": 0.0012
    },
    "known_issues": [
      {
        "issue_id": "ISS-042",
        "severity": "P2",
        "description": "大仓库（>10K files）索引偶尔超时",
        "workaround": "分批索引或增加超时时间",
        "fix_eta": "v2.0.1 (2026-10-15)"
      }
    ],
    "runbooks": [
      "runbook://aurora/restart",
      "runbook://aurora/db-rollback",
      "runbook://aurora/scale-up",
      "runbook://aurora/cache-clear",
      "runbook://aurora/log-investigation",
      "runbook://aurora/emergency-hotfix",
      "runbook://aurora/data-recovery",
      "runbook://aurora/incident-response"
    ],
    "on_call_schedule": {
      "rotation": "weekly",
      "current_on_call": "user-zhang-san",
      "escalation_path": [
        {"level": "L1", "contact": "on-call engineer", "sla_min": 15},
        {"level": "L2", "contact": "SRE Lead", "sla_min": 30},
        {"level": "L3", "contact": "CTO", "sla_min": 60}
      ]
    },
    "training_completed": true,
    "customer_faq_uri": "https://docs.spk-os.io/aurora/faq",
    "sign_off": {
      "pdt_lead": {
        "user_id": "user-pdt-lead-001",
        "signed_at": "2026-09-12T18:00:00Z"
      },
      "lmt_lead": {
        "user_id": "user-lmt-lead-001",
        "signed_at": "2026-09-12T18:30:00Z"
      }
    }
  },
  "stability_record": {
    "observation_period": {
      "start": "2026-09-12T18:30:00Z",
      "end": "2026-09-26T18:30:00Z",
      "duration_days": 14
    },
    "slo_performance": {
      "availability": {
        "target": 0.999,
        "actual": 0.9994,
        "achieved": true
      },
      "latency_p99_ms": {
        "target": 500,
        "actual": 328,
        "achieved": true
      },
      "error_rate": {
        "target": 0.005,
        "actual": 0.0011,
        "achieved": true
      }
    },
    "incidents": {
      "p0_count": 0,
      "p1_count": 0,
      "p2_count": 2,
      "p2_details": [
        {"issue_id": "ISS-045", "description": "特定浏览器下 UI 渲染异常", "status": "fixed_in_v2.0.1"},
        {"issue_id": "ISS-046", "description": "大文件上传偶发超时", "status": "workaround_available"}
      ]
    },
    "customer_feedback": {
      "nps_before": 45,
      "nps_after": 52,
      "nps_change": 7,
      "support_tickets": 12,
      "critical_issues": 0
    },
    "security_events": 0,
    "daily_reports": [
      {"date": "2026-09-13", "slo_achieved": true, "incidents": 0},
      {"date": "2026-09-14", "slo_achieved": true, "incidents": 0},
      {"date": "2026-09-15", "slo_achieved": true, "incidents": 1, "note": "P2 ISS-045"},
      {"date": "2026-09-16", "slo_achieved": true, "incidents": 0},
      {"date": "2026-09-17", "slo_achieved": true, "incidents": 0},
      {"date": "2026-09-18", "slo_achieved": true, "incidents": 0},
      {"date": "2026-09-19", "slo_achieved": true, "incidents": 0},
      {"date": "2026-09-20", "slo_achieved": true, "incidents": 0},
      {"date": "2026-09-21", "slo_achieved": true, "incidents": 0},
      {"date": "2026-09-22", "slo_achieved": true, "incidents": 0},
      {"date": "2026-09-23", "slo_achieved": true, "incidents": 0},
      {"date": "2026-09-24", "slo_achieved": true, "incidents": 0},
      {"date": "2026-09-25", "slo_achieved": true, "incidents": 1, "note": "P2 ISS-046"},
      {"date": "2026-09-26", "slo_achieved": true, "incidents": 0}
    ],
    "conclusion": {
      "stability_achieved": true,
      "confirmed_by": {
        "user_id": "user-lmt-lead-001",
        "role": "LMT Lead",
        "signed_at": "2026-09-26T19:00:00Z"
      },
      "next_phase": "regular_operations"
    }
  },
  "dcs_receipt": {
    "solver_version": "ga-handoff-validate-v1",
    "input_hash": "sha256:...",
    "signature": "sha256:..."
  }
}
```

---

## 发布阶段 Activity 汇总

| Activity ID | 名称 | Lead Agent | Worker | Verifier | 模型能力 |
|---|---|---|---|---|---|
| ACT-03-06-01 | Release Manifest 构建与验证 | Release Orchestrator | ✓(Manifest+Policy) | ✓(DCS+独立复核) | GROUNDED_EXTRACTION + STRUCTURED_DRAFTING |
| ACT-03-06-02 | 配置与数据迁移验证 | Release Orchestrator | ✓(Migration Analyst+Validator) | ✓(DCS+DBA/Data Owner) | CROSS_SOURCE_REASONING + STRUCTURED_DRAFTING |
| ACT-03-06-03 | 运营就绪验证 | Release Orchestrator | ✓(Readiness+Observability) | ✓(DCS+SRE/LMT) | GROUNDED_EXTRACTION + STRUCTURED_DRAFTING |
| ACT-03-06-04 | 渐进发布策略执行 | Release Orchestrator | ✓(Rollout Controller+Analyst) | ✓(DCS+Commander) | CROSS_SOURCE_REASONING + GROUNDED_EXTRACTION |
| ACT-03-06-05 | TR6 证据包组装与评审 | Release Orchestrator | ✓(Assembly+Checker+Critic) | ✓(DCS+评委) | STRUCTURED_DRAFTING + COUNTERARGUMENT_GENERATION |
| ACT-03-06-06 | GA 决策、移交与稳定观察 | Release Orchestrator | ✓(Handoff+Stability Monitor) | ✓(DCS+GA Authority+PDT/LMT) | STRUCTURED_DRAFTING + GROUNDED_EXTRACTION |

---

## 发布阶段产物关联图

```
[验证阶段 TR5 Go + ADCP Go]
        ↓
ACT-03-06-01 Release Manifest 构建与验证 → ReleaseManifest（不可变）
        ↓
┌───────────────────┬───────────────────┐
↓                   ↓                   ↓
ACT-03-06-02    ACT-03-06-03    [等待 02+03 完成]
配置与迁移验证   运营就绪验证
MigrationPack   ReadinessRecord
        ↓                   ↓
        └───────────────────┘
                    ↓
            ACT-03-06-04 渐进发布
            ProgressiveRolloutRecord
                    ↓
            ACT-03-06-05 TR6 证据包
            TR6DecisionPack
                    ↓
            ACT-03-06-06 GA + 移交 + 稳定观察
            GA_Handoff_StabilityRecord
                    ↓
            [发布阶段完成，进入生命周期管理]
```

---

## 发布阶段关键约束总结

| 约束 | 说明 | 来源 |
|---|---|---|
| Release Unit 不可分 | 制品+配置+迁移+证据+责任，任何字段缺失阻断发布 | R2 CL-001, ISO 90003 |
| Manifest 不可变 | 签署后不可修改，变更走 CCB | R2 §14, NIST SSDF |
| 禁止浮动 tag | 只能用 digest，不能用 `latest` | R2 AC-02, SLSA |
| digest 端到端一致 | TR5 验证 = Manifest = 生产部署 | R2 AC-02, Google SRE |
| SLSA L3 默认 | 降级到 L2 需签署 risk acceptance | R2 AC-03, SLSA |
| 渐进发布默认 | 影响 >1000 用户的发布必须渐进 | R2 ADR-03, Google SRE |
| 任何指标未知=停止 | 宁可误停不误扩 | R2 ADR-03, SRE Workbook |
| Wilson 置信区间 | 考虑样本量不确定性，优于简单阈值 | R2 §14, 统计学原理 |
| AI 无 DCP/GA/TR6 签署权 | 系统层面禁止 AI 写入 | R2 AC-06, OECD AI |
| 移交是结构化协议 | JSON，不是文档 | R2 §18, ISO 90003 |
| 稳定观察期 ≥7 天 | GA 不是终点，稳定期结束才是 | R2 AC-09, DORA |
| 回滚演练成功率 ≥95% | 含数据层 | R2 AC-08, Google SRE |
| 数据库 expand/contract | 默认 rollback_safe 或 forward_fix | R2 ADR-02, Google SRE |
| 失败回退 | 所有 Activity 可回退到纯人工路径 ≤30 分钟 | R2 AC-10 |
