## ACT-03-03-03 项目计划编制

### 基础信息

| 字段 | 内容 |
|---|---|
| **Activity ID** | ACT-03-03-03 |
| **Activity 名称** | 项目计划编制 |
| **所属阶段** | 计划 |
| **Lead Agent 角色** | Planning Lead |
| **执行位置** | task_system |
| **是否使用 Worker Agent** | — |
| **是否需要 Independent Verifier** | ✓（DCS 复算验证） |
| **所需模型能力** | STRUCTURED_DRAFTING + CODE_OR_FORMULA_ASSISTANCE |
| **所需 Skill** | ipd-plan-project-planning / spk-wbs-generator / spk-risk-narrative |
| **所需 Tool/MCP** | mcp-spk-os-artifact-query / mcp-filesystem-read / mcp-spk-os-plan-query |
| **所需 Knowledge** | KN-GAO-195G-007 / KN-GAO-16-89G-008 / KN-IPD-WBS-TEMPLATE-009 |
| **所需 Information** | INFO-PRSBASELINE / INFO-ARCHITECTURE / INFO-HISTORICAL-PROJECTS（历史项目数据）/ INFO-RESOURCE-CALENDAR（资源日历） |
| **可读取的 Memory 范围** | project:spk-os-ipd, agent_role:planning, capability:structured_drafting |
| **人工责任** | PM + Resource Owners 签署计划基线和资源承诺 |
| **失败处理** | 求解器失败同版本重试；不可解降级人工排程并标注；资源超配升级 Functional Manager；回滚到上一批准基线与承诺集 |
| **对应 Flowable 节点** | task_project_planning (ipd-plan.bpmn) |
| **证据来源** | RunReceipt + ToolReceipt + ArtifactManifest + VerificationReceipt |

### 资源与工具详情

#### 输入资源

| 资源 | 来源 | 获取方式 | 质量要求 |
|---|---|---|---|
| PRSBaselineCandidate | ACT-03-03-01 产物 | mcp-spk-os-artifact-query.get() | PM+SE 签署 |
| ArchitectureDescriptionCandidate | ACT-03-03-02 产物 | mcp-spk-os-artifact-query.get() | Chief Architect 签署 |
| 历史项目数据库 | PMO | mcp-spk-os-plan-query.get_historical_projects(similar_type) | 至少 5 个同类项目，含实际成本/工期/教训 |
| 资源日历 | HR/资源管理 | mcp-spk-os-plan-query.get_resource_calendar(team) | 含可用工时、假期、技能矩阵 |
| 费率版本 | 财务系统 | mcp-spk-os-plan-query.get_rate_card(version) | 含各角色人天费率 |
| GAO 成本/进度指南 | Knowledge 库 | mcp-filesystem-read(/knowledge/standards/gao-195g/) | 12 步成本估算法 |

#### 工具调用顺序与效率策略

```
Step 1: 加载签名输入和历史数据
├─ 调用 mcp-spk-os-artifact-query.get(PRSBaselineCandidate)
├─ 调用 mcp-spk-os-artifact-query.get(ArchitectureDescriptionCandidate)
├─ 调用 mcp-spk-os-plan-query.get_historical_projects(type="ai_tool", min_count=5)
├─ 调用 mcp-spk-os-plan-query.get_resource_calendar(team="aurora")
├─ 调用 mcp-spk-os-plan-query.get_rate_card(version="2026-Q3")
└─ 输出：计划编制 Context Pack

Step 2: LLM 生成 WBS 草稿
├─ 调用 spk-wbs-generator.generate(prs, architecture, historical_projects)
│  ├─ 分层结构: Level 1（阶段）→ Level 2（交付物）→ Level 3（工作包）
│  ├─ 每个工作包含：描述、预估工期（三点估算：乐观/可能/悲观）、所需资源、依赖关系
│  ├─ 参考历史项目的 WBS 结构和实际工期
│  └─ 输出：WBS 草稿（JSON 树结构）
└─ 输出：WBS 草稿

Step 3: DCS 执行 CPM 计算（确定性）
├─ 调用 DCS POST /api/v1/cpm
│  ├─ 输入：WBS（含工期和依赖关系）+ 工作日历
│  ├─ 计算：前向传递（最早开始/完成）+ 后向传递（最晚开始/完成）
│  ├─ 输出：关键路径、总工期、各任务浮动时间
│  └─ 签名：sha256(input + output + solver_version + timestamp)
└─ 输出：CPM 结果（含签名）

Step 4: DCS 执行 Monte Carlo 模拟（确定性）
├─ 调用 DCS POST /api/v1/monte-carlo
│  ├─ 输入：WBS（含三点估算分布）+ 迭代次数 + 随机种子
│  ├─ 工期分布：三角分布（最乐观/最可能/最悲观）
│  ├─ 输出：P10/P50/P80/P90 工期 + 直方图
│  └─ 签名：sha256(input + output + solver_version + timestamp)
└─ 输出：Monte Carlo 结果（含签名）

Step 5: DCS 计算 EVM 基线（确定性）
├─ 调用 DCS POST /api/v1/evm-baseline
│  ├─ 输入：WBS + 预算分配 + 费率版本
│  ├─ 输出：PV 曲线（计划值累计）、BAC（完工预算）
│  └─ 签名：sha256(...)
└─ 输出：EVM 基线（含签名）

Step 6: LLM 生成风险登记册草稿
├─ 调用 spk-risk-narrative.generate(prs, architecture, wbs, historical_lessons)
│  ├─ 识别：技术风险、资源风险、外部依赖风险、进度风险
│  ├─ 评估：概率×影响矩阵（5×5）
│  ├─ 缓解：策略描述和触发条件
│  ├─ 参考历史项目的教训
│  └─ 输出：风险登记册草稿
└─ 输出：风险登记册草稿

Step 7: 敏感性分析
├─ 调用 DCS POST /api/v1/sensitivity
│  ├─ 输入：WBS + 风险登记册
│  ├─ 输出：前三大工期驱动因素 + 前三大成本驱动因素
│  └─ 签名：sha256(...)
└─ 输出：敏感性分析报告

Step 8: PM + Resource Owners 人工审核
├─ PM 审核：
│  ├─ 确认工期假设的合理性（Monte Carlo P50 是否与经验判断一致）
│  ├─ 评估关键路径的可行性
│  ├─ 审批风险缓解策略
│  └─ 确认预算分配
├─ Resource Owners 审核：
│  ├─ 确认资源日历的准确性
│  ├─ 确认资源承诺（人天/技能/时间段）
│  └─ 签署资源承诺
├─ PM 签署计划基线
└─ 输出：IntegratedProjectPlan（已签署）

Step 9: yudao 状态更新
├─ 更新计划状态为 approved
├─ 生成基线 Manifest（含所有资产的 SHA-256）
└─ 记录签署时间戳和身份
```

#### 效率优化策略

| 策略 | 说明 | 预期效果 |
|---|---|---|
| DCS 独占数值计算 | CPM/EVM/Monte Carlo 全部由确定性服务执行 | 消除 LLM 数值幻觉 |
| 历史数据校准 | Monte Carlo 分布参数从历史项目校准 | 提高估算精度 |
| 三点估算 | 每个任务用乐观/可能/悲观三个值 | 比单点估算更真实 |
| 敏感性分析 | 自动识别前三大驱动因素 | 帮助 PM 聚焦关键风险 |

### 输出 Artifact 模板

#### Artifact 名称
`IntegratedProjectPlan`

#### 文件结构
```
integrated_project_plan/
├── manifest.yaml
├── plan.json                     # 主数据文件
├── wbs/                          # 工作分解结构
│   ├── wbs.json
│   └── wbs_gantt.html           # Gantt 图可视化
├── schedule/                     # 进度计划
│   ├── ims.json                  # 集成主进度
│   ├── critical_path.json
│   └── monte_carlo.json
├── cost/                         # 成本估算
│   ├── cost_baseline.json
│   ├── evm_baseline.json
│   └── sensitivity.json
├── risk/                         # 风险登记册
│   └── risk_register.json
├── resource/                     # 资源计划
│   ├── resource_allocation.json
│   └── capacity_evidence.json
├── dcs_receipts/                 # DCS 计算签名
│   ├── cpm_receipt.json
│   ├── monte_carlo_receipt.json
│   ├── evm_receipt.json
│   └── sensitivity_receipt.json
└── README.md
```

#### plan.json Schema

```json
{
  "plan_id": "ipp-20260817-001",
  "version": "1.0.0",
  "as_of": "2026-08-17",
  "created_at": "2026-08-17T19:00:00Z",
  "created_by": "agent-planning-001",
  "signed_by": [
    {
      "user_id": "user-pm-001",
      "role": "Project Manager",
      "signed_at": "2026-08-17T22:00:00Z",
      "decision": "approved"
    },
    {
      "user_id": "user-resource-owner-001",
      "role": "Resource Owner - Backend Team",
      "signed_at": "2026-08-17T22:30:00Z",
      "decision": "committed"
    }
  ],
  "input_artifacts": {
    "prs_baseline_ref": "prs-20260817-001",
    "architecture_ref": "ad-20260817-001"
  },
  "wbs_summary": {
    "total_tasks": 87,
    "level_1_count": 4,
    "level_2_count": 12,
    "level_3_count": 71
  },
  "schedule_summary": {
    "critical_path": ["ML模型训练(30d)", "集成测试(15d)", "性能调优(10d)", "UAT(10d)"],
    "total_duration_p50": 142,
    "monte_carlo": {
      "iterations": 10000,
      "seed": 42,
      "p10": 128,
      "p50": 142,
      "p80": 163,
      "p90": 178
    },
    "start_date": "2026-09-01",
    "end_date_p50": "2027-01-21",
    "end_date_p80": "2027-02-11"
  },
  "cost_summary": {
    "bac": 4800000,
    "currency": "CNY",
    "cost_breakdown": {
      "development": 2800000,
      "testing": 800000,
      "infrastructure": 600000,
      "management": 400000,
      "contingency": 200000
    }
  },
  "risk_summary": {
    "total_risks": 15,
    "high_risks": 3,
    "medium_risks": 7,
    "low_risks": 5,
    "top_risks": [
      {
        "risk_id": "RISK-001",
        "description": "ML 模型训练超期",
        "probability": 0.3,
        "impact": "high",
        "schedule_impact_days": 20,
        "mitigation": "预训练基础模型 + 并行训练",
        "trigger": "训练进度落后计划 >5 天"
      }
    ]
  },
  "sensitivity": {
    "top_schedule_drivers": [
      {"task": "ML模型训练", "impact_days": 20},
      {"task": "集成测试", "impact_days": 10},
      {"task": "性能调优", "impact_days": 8}
    ],
    "top_cost_drivers": [
      {"item": "开发人员工时", "impact_pct": 35},
      {"item": "GPU 云资源", "impact_pct": 20},
      {"item": "测试环境", "impact_pct": 12}
    ]
  },
  "dcs_receipts": {
    "cpm_solver_version": "planner-v1",
    "cpm_input_hash": "sha256:abc123...",
    "cpm_signature": "sha256:def456...",
    "monte_carlo_solver_version": "mc-v1",
    "monte_carlo_input_hash": "sha256:ghi789...",
    "monte_carlo_signature": "sha256:jkl012..."
  }
}
```

#### README.md 模板

```markdown
# 集成项目计划 IPP-20260817-001

## 概览
- **计划时间**：2026-08-17
- **WBS 任务数**：87 个（4 阶段 / 12 交付物 / 71 工作包）
- **计划开始**：2026-09-01
- **预计完成**：P50 = 2027-01-21（142 天）/ P80 = 2027-02-11（163 天）
- **总预算**：¥480 万

## 关键路径
ML模型训练(30d) → 集成测试(15d) → 性能调优(10d) → UAT(10d)

## Monte Carlo 工期分布
| 分位 | 工期（天） | 预计完成日期 |
|---|---|---|
| P10 | 128 | 2027-01-07 |
| P50 | 142 | 2027-01-21 |
| P80 | 163 | 2027-02-11 |
| P90 | 178 | 2027-02-26 |

## TOP 3 风险
1. **ML 模型训练超期**（概率 30%，影响 +20 天）→ 预训练基础模型 + 并行训练
2. **GPU 资源不足**（概率 20%，影响 +15 天）→ 预留弹性资源 + 多云备份
3. **集成测试发现架构缺陷**（概率 15%，影响 +10 天）→ 提前架构评审 + 原型验证

## 敏感性分析
- **工期驱动因素**：ML模型训练 > 集成测试 > 性能调优
- **成本驱动因素**：开发人员工时(35%) > GPU 云资源(20%) > 测试环境(12%)

## 签署
- **Project Manager**：王五，2026-08-17 22:00，approved
- **Resource Owner（后端）**：赵六，2026-08-17 22:30，committed
```

### 节点关联与展示

#### 在整体流程中的位置

```
ACT-03-03-01 + ACT-03-03-02 → ACT-03-03-03 项目计划编制 → ACT-03-03-06 TR2/PDCP 决策包组装
                                    ↓
                            IntegratedProjectPlan
```

#### 上游依赖
| 上游 Activity | 依赖内容 | 依赖方式 |
|---|---|---|
| ACT-03-03-01 产品需求规格化 | PRSBaselineCandidate（需求数量和复杂度） | 作为 WBS 拆分依据 |
| ACT-03-03-02 架构描述生成 | ArchitectureDescriptionCandidate（容器和接口） | 作为技术复杂度评估依据 |

#### 下游消费
| 下游 Activity | 消费内容 | 消费方式 |
|---|---|---|
| ACT-03-03-06 TR2/PDCP 决策包组装 | IntegratedProjectPlan（计划基线） | 作为 PDCP 投资决策的依据 |

#### 可视化展示

**在 SPK-OS Dashboard 的展示方式**：
1. **Gantt 图**：交互式甘特图，高亮关键路径，支持缩放和筛选
2. **Monte Carlo 分布图**：工期概率分布直方图，标注 P10/P50/P80/P90
3. **EVM 曲线**：PV（计划值）累计曲线，后续可叠加 EV（挣值）和 AC（实际成本）
4. **风险矩阵**：概率×影响热力图，标注 TOP 风险
5. **资源负荷图**：按时间段展示资源利用率，高亮超配区域

---
