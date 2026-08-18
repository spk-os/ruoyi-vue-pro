## ACT-03-06-04 渐进发布策略执行

### 基础信息

| 字段 | 内容 |
|---|---|
| **Activity ID** | ACT-03-06-04 |
| **Activity 名称** | 渐进发布策略执行 |
| **所属阶段** | 发布 |
| **Lead Agent 角色** | Release Orchestrator |
| **执行位置** | task_system |
| **是否使用 Worker Agent** | ✓（Rollout Controller Agent 执行 + Rollout Analyst Agent 解读指标） |
| **是否需要 Independent Verifier** | ✓（DCS 护栏指标计算和阈值判定 + Release Commander 人工确认） |
| **所需模型能力** | CROSS_SOURCE_REASONING + GROUNDED_EXTRACTION |
| **所需 Skill** | ipd-release-rollout / spk-rollout-controller / spk-rollout-analyst |
| **所需 Tool/MCP** | mcp-spk-os-artifact-query / mcp-monitoring-query / mcp-argo-rollouts |
| **所需 Knowledge** | KN-IPD-ROLLOUT-SCHEMA-063 / KN-PROGRESSIVE-DELIVERY-064 / KN-WILSON-INTERVAL-065 |
| **所需 Information** | INFO-RELEASE-MANIFEST（已签署 Manifest）/ INFO-ROLLOUT-STRATEGY（cohort 策略）/ INFO-GUARDRAIL-THRESHOLDS（护栏阈值）/ INFO-BASELINE-METRICS（基线指标） |
| **可读取的 Memory 范围** | project:spk-os-ipd, agent_role:release, capability:cross_source_reasoning |
| **人工责任** | Release Commander 保留人工中止/恢复权；高风险扩流需人工确认 |
| **失败处理** | 遥测短暂缺失只暂停并重取；持续缺失降级回滚；护栏突破/安全事件升级 Commander/Incident；回滚至 manifest 指定稳定版本 |
| **对应 Flowable 节点** | task_progressive_rollout (ipd-release.bpmn) |
| **证据来源** | RunReceipt + ProgressiveRolloutRecord + ArtifactManifest |

### 资源与工具详情

#### 工具调用顺序与效率策略

```
Step 1: 绑定 manifest/基线
├─ 从 Manifest 中提取 artifact.digest 和 rollback_target
├─ 从基线指标中提取当前 SLO 值
├─ 从护栏阈值中提取停止条件
├─ 配置 Argo Rollouts：
│  ├─ 设置 canary 镜像为 manifest.artifact.image_ref
│  ├─ 设置 stable 镜像为 rollback_target.image_ref
│  ├─ 配置 cohort 步骤（1% → 5% → 20% → 50% → 100%）
│  ├─ 配置观察窗（30min → 1h → 2h → 4h）
│  └─ 配置分析模板（guardrail-analysis）
└─ 输出：Argo Rollouts 配置

Step 2: Cohort 1 — 1% 流量，观察 30 分钟
├─ Rollout Controller Agent 部署 canary 到 1% 流量
├─ 启动观察窗计时（30 分钟）
├─ Stability Monitor Agent 开始监控
├─ 观察窗结束时：
│  ├─ 调用 DCS POST /api/v1/guardrail/evaluate
│  │  ├─ 输入：canary_metrics, baseline_metrics, thresholds
│  │  ├─ 方法：Wilson 95% 置信区间
│  │  ├─ 指标：
│  │  │  □ error_rate（错误率）
│  │  │  □ latency_p99（P99 延迟）
│  │  │  □ availability（可用性）
│  │  │  □ security_events（安全事件数）
│  │  ├─ 对每个指标计算：
│  │  │  □ canary vs baseline 的差值
│  │  │  □ Wilson 95% 置信区间
│  │  │  □ p-value（统计显著性）
│  │  │  □ effect_size（效应量）
│  │  │  □ verdict（pass/fail）
│  │  ├─ **关键规则**：任何指标未知（数据不足）= fail（宁可误停不误扩）
│  │  ├─ 输出：护栏判定报告 + 签名
│  │  └─ 签名：sha256(...)
│  ├─ Rollout Analyst Agent 生成分析报告：
│  │  ├─ 指标对比表（含置信区间和 p-value）
│  │  ├─ 异常检测结果（趋势性退化 vs 偶发抖刺）
│  │  ├─ 明确建议：继续扩流 / 暂停观察 / 停止并回滚
│  │  └─ 输出：Cohort 分析报告
│  └─ 判定结果：
│      ├─ [全部 pass] → 自动扩流到下一 cohort
│      └─ [任一 fail] → 自动停止扩流
│          ├─ 通知 Release Commander 和 SRE Lead
│          ├─ Rollout Analyst Agent 生成详细分析报告
│          └─ Release Commander 决定：
│              ├─ 修复后重试当前 cohort
│              ├─ 回滚到上一版本
│              └─ 标记为已知问题继续扩流（需签署 risk acceptance）
└─ 输出：Cohort 1 结果

Step 3: Cohort 2 — 5% 流量，观察 1 小时
├─ 重复 Step 2 的流程
├─ 观察窗延长到 1 小时（更多数据，更窄置信区间）
└─ 输出：Cohort 2 结果

Step 4: Cohort 3 — 20% 流量，观察 2 小时
├─ 重复 Step 2 的流程
├─ **高风险扩流**：20% → 50% 需要 Release Commander 人工确认
└─ 输出：Cohort 3 结果

Step 5: Cohort 4 — 50% 流量，观察 4 小时
├─ 重复 Step 2 的流程
├─ 观察窗延长到 4 小时（覆盖更多使用模式）
└─ 输出：Cohort 4 结果

Step 6: Full — 100% 流量
├─ Rollout Controller Agent 扩流到 100%
├─ 记录 rollout 完成状态
├─ 触发 TR6 证据包组装（ACT-03-06-05）
└─ 输出：ProgressiveRolloutRecord（已完成）
```

#### 效率优化策略

| 策略 | 说明 | 预期效果 |
|---|---|---|
| Wilson 置信区间 | 考虑样本量不确定性，优于简单阈值 | 减少误判 |
| 自动停止 | 护栏突破自动停止，不等待人工 | 快速止血 |
| 逐级扩流 | 1% → 5% → 20% → 50% → 100% | 分段控制爆炸半径 |
| 观察窗递增 | 30min → 1h → 2h → 4h | 更多数据，更窄置信区间 |

### 输出 Artifact 模板

#### Artifact 名称
`ProgressiveRolloutRecord`

#### ProgressiveRolloutRecord Schema

```json
{
  "record_id": "prr-20260911-001",
  "version": "1.0.0",
  "created_at": "2026-09-11T10:00:00Z",
  "manifest_ref": "rel-20260910-001",
  "rollout_strategy": {
    "tool": "argo-rollouts",
    "cohorts": [
      {"weight_pct": 1, "observation_min": 30},
      {"weight_pct": 5, "observation_min": 60},
      {"weight_pct": 20, "observation_min": 120},
      {"weight_pct": 50, "observation_min": 240},
      {"weight_pct": 100, "observation_min": 0}
    ],
    "guardrails": ["error_rate", "latency_p99", "availability", "security_events"],
    "statistical_method": "wilson_95_ci"
  },
  "cohorts": [
    {
      "cohort_id": 1,
      "weight_pct": 1,
      "start_time": "2026-09-11T10:00:00Z",
      "end_time": "2026-09-11T10:30:00Z",
      "observation_min": 30,
      "metrics": {
        "error_rate": {
          "baseline": 0.0012,
          "canary": 0.0010,
          "diff": -0.0002,
          "wilson_95_ci": [-0.0005, 0.0001],
          "p_value": 0.24,
          "effect_size": -0.15,
          "verdict": "pass"
        },
        "latency_p99_ms": {
          "baseline": 320,
          "canary": 310,
          "diff_ms": -10,
          "wilson_95_ci": [-25, 5],
          "p_value": 0.18,
          "effect_size": -0.08,
          "verdict": "pass"
        },
        "availability": {
          "baseline": 0.9995,
          "canary": 0.9993,
          "diff": -0.0002,
          "wilson_95_ci": [-0.0005, 0.0001],
          "p_value": 0.18,
          "effect_size": -0.10,
          "verdict": "pass"
        },
        "security_events": {
          "baseline": 0,
          "canary": 0,
          "verdict": "pass"
        }
      },
      "anomaly_detection": {
        "trend_degradation": false,
        "sporadic_spikes": 0,
        "correlation_anomalies": 0
      },
      "analyst_recommendation": "continue",
      "dcs_signature": "sha256:...",
      "decision": "continue",
      "decided_by": "auto"
    },
    {
      "cohort_id": 2,
      "weight_pct": 5,
      "start_time": "2026-09-11T10:30:00Z",
      "end_time": "2026-09-11T11:30:00Z",
      "observation_min": 60,
      "metrics": {
        "error_rate": {"baseline": 0.0012, "canary": 0.0011, "verdict": "pass"},
        "latency_p99_ms": {"baseline": 320, "canary": 325, "verdict": "pass"},
        "availability": {"baseline": 0.9995, "canary": 0.9994, "verdict": "pass"},
        "security_events": {"baseline": 0, "canary": 0, "verdict": "pass"}
      },
      "decision": "continue",
      "decided_by": "auto"
    },
    {
      "cohort_id": 3,
      "weight_pct": 20,
      "start_time": "2026-09-11T11:30:00Z",
      "end_time": "2026-09-11T13:30:00Z",
      "observation_min": 120,
      "metrics": {
        "error_rate": {"baseline": 0.0012, "canary": 0.0014, "verdict": "pass", "note": "略升但 Wilson CI 包含 0，p=0.18，不显著"},
        "latency_p99_ms": {"baseline": 320, "canary": 350, "verdict": "pass"},
        "availability": {"baseline": 0.9995, "canary": 0.9993, "verdict": "pass"},
        "security_events": {"baseline": 0, "canary": 0, "verdict": "pass"}
      },
      "decision": "continue",
      "decided_by": "user-release-commander-001",
      "human_confirmation_required": true
    },
    {
      "cohort_id": 4,
      "weight_pct": 50,
      "start_time": "2026-09-11T13:30:00Z",
      "end_time": "2026-09-11T17:30:00Z",
      "observation_min": 240,
      "metrics": {
        "error_rate": {"baseline": 0.0012, "canary": 0.0012, "verdict": "pass"},
        "latency_p99_ms": {"baseline": 320, "canary": 335, "verdict": "pass"},
        "availability": {"baseline": 0.9995, "canary": 0.9994, "verdict": "pass"},
        "security_events": {"baseline": 0, "canary": 0, "verdict": "pass"}
      },
      "decision": "continue",
      "decided_by": "auto"
    },
    {
      "cohort_id": 5,
      "weight_pct": 100,
      "start_time": "2026-09-11T17:30:00Z",
      "end_time": null,
      "observation_min": 0,
      "decision": "full_rollout",
      "decided_by": "auto"
    }
  ],
  "overall_status": "completed",
  "total_duration_hour": 7.5,
  "rollback_executed": false,
  "rollback_target": "1.9.5"
}
```

---
