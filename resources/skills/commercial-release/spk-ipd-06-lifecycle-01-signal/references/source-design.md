## ACT-03-07-01 Lifecycle Signal Ingestion

### 基础信息

| 字段 | 内容 |
|---|---|
| **Activity ID** | ACT-03-07-01 |
| **Activity 名称** | Lifecycle Signal Ingestion（生命周期信号摄取） |
| **所属阶段** | 生命周期管理 |
| **Lead Agent 角色** | Lifecycle Orchestrator |
| **执行位置** | task_system |
| **是否使用 Worker Agent** | ✓（Signal Ingestion Agent 多源采集 + Signal Router 按类型分发） |
| **是否需要 Independent Verifier** | ✓（DCS 校验 schema/许可/时间/产品版本 + LMT Signal Owner 接受/隔离） |
| **所需模型能力** | GROUNDED_EXTRACTION + MULTILINGUAL_ANALYSIS |
| **所需 Skill** | ipd-lifecycle-signal / spk-signal-ingestion / spk-signal-router |
| **所需 Tool/MCP** | mcp-crm-query / mcp-monitoring-query / mcp-sbom-query / mcp-contract-query / mcp-dependency-query |
| **所需 Knowledge** | KN-IPD-SIGNAL-SCHEMA-069 / KN-ISO-55001-ASSET-MGMT-070 |
| **所需 Information** | INFO-PUBLISHED-PRODUCT（已发布产品）/ INFO-CUSTOMER-TICKETS（客户工单）/ INFO-SERVICE-TELEMETRY（服务遥测）/ INFO-SBOM-VEX（SBOM/VEX）/ INFO-VULNERABILITY-INTEL（漏洞情报）/ INFO-CONTRACTS（合同/SLA）/ INFO-REVENUE-COST（收入/成本）/ INFO-DEPENDENCY-VERSIONS（依赖版本） |
| **可读取的 Memory 范围** | project:spk-os-ipd, agent_role:lifecycle, capability:grounded_extraction |
| **人工责任** | LMT Lead 审核 Signal 质量、处理升级；Data Steward 确认数据用途与口径 |
| **失败处理** | 源不可达重试一次；仍失败保留游标降级人工导入；批量 schema 漂移升级 Data Steward；回滚派生信号到上一签名水位 |
| **对应 Flowable 节点** | task_signal_ingestion (ipd-lifecycle.bpmn) |
| **证据来源** | RunReceipt + LifecycleSignalSet + ArtifactManifest |

### 资源与工具详情

#### 输入资源

| 资源 | 来源 | 获取方式 | 频率 | 质量要求 |
|---|---|---|---|---|
| 客户工单 | CRM/Support | mcp-crm-query.get_tickets(product_id, since) | 实时/5min | 含 product_version、severity、原文 |
| 服务遥测 | APM/Monitoring | mcp-monitoring-query.get_metrics(product_id, window) | 实时/1min | 采样率 ≥1%，含 cohort |
| SBOM/VEX | 安全平台 | mcp-sbom-query.get(product_id, version) | 版本发布后 | SPDX/CycloneDX + VEX JSON |
| 漏洞情报 | CVE/NVD/CISA KEV | mcp-vulnerability-query.get_latest(since) | 24h 内更新 | 含 CVSS、affected_versions、exploit_status |
| 合同/SLA | 商务系统 | mcp-contract-query.get_active(product_id) | 合同变更时 | 含 EOL 条款、支持级别 |
| 收入/成本 | 财务系统 | mcp-finance-query.get(product_id, period) | 月度/季度 | 审计通过 |
| 依赖版本 | 技术平台 | mcp-dependency-query.get_eol_calendar(product_id) | 上游官方更新 | 含 EOL 日期、继任版本 |
| 用量/续费 | 产品分析 | mcp-analytics-query.get_usage(product_id, window) | 日度 | 含 cohort、版本分布 |

#### 工具调用顺序与效率策略

```
Step 1: 定义采集窗
├─ 确定本次摄取的时间范围（上次摄取时间 → 当前时间）
├─ 确定产品版本范围（所有活跃版本 + 最近 2 个已退市版本）
├─ 确定客户 cohort 范围（按合同级别、地区、行业分层）
└─ 输出：采集窗定义

Step 2: 多源并行抓取
├─ 并行调用 8 个数据源 API：
│  ├─ mcp-crm-query.get_tickets(product_id, since=last_ingestion)
│  ├─ mcp-monitoring-query.get_metrics(product_id, window)
│  ├─ mcp-sbom-query.get_all_versions(product_id)
│  ├─ mcp-vulnerability-query.get_latest(since=last_check)
│  ├─ mcp-contract-query.get_active(product_id)
│  ├─ mcp-finance-query.get(product_id, period=current_quarter)
│  ├─ mcp-dependency-query.get_eol_calendar(product_id)
│  └─ mcp-analytics-query.get_usage(product_id, window)
├─ 对每个来源保存原文 hash（SHA-256）
├─ 记录采集时间戳和来源 URI
└─ 输出：原始信号集合

Step 3: 归一化与去重
├─ 调用 spk-signal-ingestion.normalize(raw_signals)
│  ├─ 统一格式为 LifecycleSignal Schema：
│  │  ├─ signal_id: 唯一标识
│  │  ├─ kind: vulnerability | feedback | telemetry | contract | dependency | commercial
│  │  ├─ source: {uri, digest, system}
│  │  ├─ product_version: 精确版本号
│  │  ├─ customer_cohort: 客户分群标识
│  │  ├─ observed_at: 事件时间
│  │  ├─ severity: P0-P3
│  │  ├─ data_class: internal | confidential | public
│  │  ├─ numerator/denominator: 可量化指标的分子分母
│  │  └─ provenance: {activity, responsible}
│  ├─ 去重：
│  │  ├─ 同一客户+同一版本+同一问题 → 合并（保留所有原文引用）
│  │  ├─ 同一 CVE+同一组件版本 → 合并
│  │  └─ 同一遥测指标+同一时间窗 → 聚合
│  └─ 输出：归一化信号集合
└─ 输出：归一化信号集合

Step 4: DCS 校验
├─ 调用 DCS POST /api/v1/signal-validate
│  ├─ 检查：
│  │  □ 每条信号含必填字段（signal_id, kind, source, product_version, observed_at）
│  │  □ 时间戳在采集窗内
│  │  □ 产品版本在版本族谱中存在
│  │  □ 数据分类标签有效
│  │  □ 许可范围合规（不超出数据处理目的）
│  │  □ 无重复键冲突
│  ├─ 未知版本或无许可 → 隔离到死信队列
│  ├─ 输出：校验报告 + 签名
│  └─ 签名：sha256(...)
└─ 输出：DCS 校验报告

Step 5: Signal Router 分发
├─ 按 kind 分发到下游处理：
│  ├─ feedback → ACT-03-07-02 Feedback Clustering
│  ├─ vulnerability → ACT-03-07-03 Vulnerability-SBOM Correlation
│  ├─ telemetry → ACT-03-07-05 Quarterly Value Review（聚合后）
│  ├─ contract → ACT-03-07-07 EOL Assessment（事件触发）
│  ├─ dependency → ACT-03-07-04 Maintenance Train（EOL 触发）
│  └─ commercial → ACT-03-07-05 Quarterly Value Review
├─ P0/P1 信号直接路由事件流程（不等待批处理）
└─ 输出：分发记录

Step 6: LMT Signal Owner 确认
├─ LMT Lead 审查：
│  ├─ 信号质量（完整性、时效性、代表性）
│  ├─ 异常信号（异常峰值、缺数清单）
│  ├─ 隔离信号（死信队列中的信号）
│  └─ 确认观测窗
├─ 签署信号包
└─ 输出：LifecycleSignalSet（已签署）
```

#### 效率优化策略

| 策略 | 说明 | 预期效果 |
|---|---|---|
| 多源并行抓取 | 8 个数据源并行调用 | 比串行快 5-8 倍 |
| 增量摄取 | 只抓取上次摄取后的新数据 | 减少重复处理 |
| 死信队列 | 不合格信号隔离而非丢弃 | 保留审计线索 |
| P0/P1 直接路由 | 高优先级信号不等待批处理 | 缩短响应时间 |

### 输出 Artifact 模板

#### Artifact 名称
`LifecycleSignalSet`

#### 文件结构
```
lifecycle_signal_set/
├── manifest.yaml
├── signals.json                  # 主数据文件
├── dead_letter_queue.json        # 隔离信号
├── dcs_receipts/
│   └── signal_validate.json
└── README.md
```

#### signals.json Schema

```json
{
  "set_id": "lss-20260915-001",
  "version": "1.0.0",
  "created_at": "2026-09-15T08:00:00Z",
  "created_by": "agent-signal-ingestion-001",
  "signed_by": {
    "user_id": "user-lmt-lead-001",
    "role": "LMT Lead",
    "signed_at": "2026-09-15T08:30:00Z"
  },
  "ingestion_window": {
    "start": "2026-09-14T08:00:00Z",
    "end": "2026-09-15T08:00:00Z"
  },
  "product_id": "aurora",
  "active_versions": ["2.0.0", "2.0.1", "1.9.5"],
  "statistics": {
    "total_signals": 287,
    "by_kind": {
      "feedback": 42,
      "vulnerability": 1,
      "telemetry": 200,
      "contract": 3,
      "dependency": 2,
      "commercial": 39
    },
    "duplicates_merged": 15,
    "isolated": 5,
    "p0_signals": 0,
    "p1_signals": 1
  },
  "signals": [
    {
      "signal_id": "SIG-20260915-001",
      "kind": "feedback",
      "source": {
        "uri": "crm://ticket/TK-12345",
        "digest": "sha256:abc123...",
        "system": "zendesk"
      },
      "product_version": "2.0.0",
      "customer_cohort": "enterprise-devops",
      "observed_at": "2026-09-14T14:30:00Z",
      "severity": "P2",
      "data_class": "confidential",
      "numerator": null,
      "denominator": null,
      "raw_text_hash": "sha256:def456...",
      "provenance": {
        "activity": "ACT-03-07-01",
        "responsible": "LifecycleOps"
      },
      "status": "VERIFIED_SOURCE",
      "routed_to": "ACT-03-07-02"
    },
    {
      "signal_id": "SIG-20260915-042",
      "kind": "vulnerability",
      "source": {
        "uri": "cve://CVE-2026-XXXXX",
        "digest": "sha256:ghi789...",
        "system": "nvd"
      },
      "product_version": "2.0.0",
      "customer_cohort": "all",
      "observed_at": "2026-09-14T22:00:00Z",
      "severity": "P1",
      "data_class": "confidential",
      "numerator": null,
      "denominator": null,
      "provenance": {
        "activity": "ACT-03-07-01",
        "responsible": "LifecycleOps"
      },
      "status": "VERIFIED_SOURCE",
      "routed_to": "ACT-03-07-03",
      "immediate_escalation": true
    }
  ],
  "dead_letter_queue": [
    {
      "signal_id": "DLQ-20260915-001",
      "reason": "unknown_product_version",
      "original_source": "crm://ticket/TK-12400",
      "reported_version": "2.1.0-beta",
      "action": "isolated_for_manual_review"
    }
  ],
  "dcs_receipt": {
    "solver_version": "signal-validate-v1",
    "input_hash": "sha256:...",
    "signature": "sha256:..."
  }
}
```

### 节点关联与展示

#### 在整体流程中的位置

```
[发布阶段 GA + 稳定观察结束]
        ↓
ACT-03-07-01 Lifecycle Signal Ingestion（持续运行，每 5 分钟）
        ↓
LifecycleSignalSet
        ↓
┌──────────┬──────────┬──────────┬──────────┐
↓          ↓          ↓          ↓          ↓
ACT-07-02  ACT-07-03  ACT-07-05  ACT-07-07  ACT-07-04
反馈聚类   漏洞关联   季度评审   EOL评估    维护列车
```

#### 上游依赖
| 上游 Activity | 依赖内容 | 依赖方式 |
|---|---|---|
| ACT-03-06-06 GA 决策、移交与稳定观察 | GA_Handoff_StabilityRecord | 作为生命周期管理的启动条件 |
| 外部数据源 | 工单/遥测/漏洞/合同/依赖/财务 | 作为信号的原始来源 |

#### 下游消费
| 下游 Activity | 消费内容 | 消费方式 |
|---|---|---|
| ACT-03-07-02 Feedback Clustering | feedback 类信号 | 作为聚类的输入 |
| ACT-03-07-03 Vulnerability-SBOM Correlation | vulnerability 类信号 | 作为漏洞关联的触发 |
| ACT-03-07-04 Maintenance Train | dependency EOL 信号 | 作为维护列车的触发 |
| ACT-03-07-05 Quarterly Value Review | telemetry + commercial 信号 | 作为价值评审的数据源 |
| ACT-03-07-07 EOL Assessment | contract 事件 | 作为 EOL 评估的触发 |

---
