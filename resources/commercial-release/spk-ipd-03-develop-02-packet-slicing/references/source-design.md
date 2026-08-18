## ACT-03-04-02 开发包切片

### 基础信息

| 字段 | 内容 |
|---|---|
| **Activity ID** | ACT-03-04-02 |
| **Activity 名称** | 开发包切片 |
| **所属阶段** | 开发 |
| **Lead Agent 角色** | Development Orchestrator |
| **执行位置** | task_system |
| **是否使用 Worker Agent** | — |
| **是否需要 Independent Verifier** | ✓（DCS 图约束检查） |
| **所需模型能力** | CROSS_SOURCE_REASONING + STRUCTURED_DRAFTING |
| **所需 Skill** | ipd-dev-packet-slicing / spk-vertical-slicer |
| **所需 Tool/MCP** | mcp-spk-os-artifact-query / mcp-spk-os-plan-query / mcp-filesystem-read |
| **所需 Knowledge** | KN-IPD-DEV-PACKET-SCHEMA-018 / KN-DORA-DEVOPS-019 |
| **所需 Information** | INFO-DEV-BASELINE（已锁定基线）/ INFO-RESOURCE-CALENDAR（资源日历） |
| **可读取的 Memory 范围** | project:spk-os-ipd, agent_role:development, capability:cross_source_reasoning |
| **人工责任** | PDT / 架构师 / 测试负责人联合审批切片方案 |
| **失败处理** | 切片同质或过大时调整约束重试；仍失败人工切片；循环依赖/关键 owner 缺失升级 PDT Lead；回滚包集及任务分派 |
| **对应 Flowable 节点** | task_dev_packet_slicing (ipd-development.bpmn) |
| **证据来源** | RunReceipt + ArtifactManifest + HumanApprovalRecord |

### 资源与工具详情

#### 输入资源

| 资源 | 来源 | 获取方式 | 质量要求 |
|---|---|---|---|
| DevelopmentBaselineManifest | ACT-03-04-01 产物 | mcp-spk-os-artifact-query.get() | CM+PDT 签署 |
| PRSBaselineCandidate | 计划阶段产物（通过基线引用） | mcp-spk-os-artifact-query.get() | 含 45 条需求 + 风险等级 |
| ArchitectureDescriptionCandidate | 计划阶段产物（通过基线引用） | mcp-spk-os-artifact-query.get() | 含容器/接口/ADR |
| CBBReuseDecision | 计划阶段产物（通过基线引用） | mcp-spk-os-artifact-query.get() | 含复用/改造/自研决策 |
| 资源日历 | HR/资源管理 | mcp-spk-os-plan-query.get_resource_calendar() | 含可用工时和技能矩阵 |
| 历史开发包数据 | AgentMemory | memory.query(scope="dev_packet_history") | 含历史包的工期和缺陷数据 |

#### 工具调用顺序与效率策略

```
Step 1: 加载基线与约束
├─ 调用 mcp-spk-os-artifact-query.get(DevelopmentBaselineManifest)
├─ 从基线中提取 PRS、Architecture、CBB Decision
├─ 加载资源日历和历史开发包数据
└─ 输出：切片 Context Pack

Step 2: Slicing Agent 生成候选切片方案
├─ 调用 spk-vertical-slicer.slice(prs, architecture, cbb_decision, constraints)
│  ├─ 纵向切片原则：每个包可独立开发、测试、交付
│  ├─ 按客户价值分组：每个包对应一个可交付的用户价值
│  ├─ 粒度约束：每个包预估 ≤2 人周（DORA 研究建议）
│  ├─ 绑定每个包：
│  │  ├─ 需求子集（requirement_ids）
│  │  ├─ 涉及的架构组件（containers）
│  │  ├─ 接口依赖（interfaces + contract_hash）
│  │  ├─ CBB 复用点（cbb_ids）
│  │  ├─ 风险登记（risks）
│  │  ├─ 测试策略（unit/integration/coverage_target）
│  │  ├─ 完成标准（done_criteria）
│  │  └─ 预估工作量（estimated_effort）
│  ├─ 识别包间依赖并排序（DAG）
│  └─ 输出：候选切片方案（DevelopmentPackageSet 草稿）

Step 3: DCS 图约束检查（确定性）
├─ 调用 DCS POST /api/v1/slice-validate
│  ├─ 输入：候选切片方案 + 需求全集 + 依赖图
│  ├─ 检查：
│  │  □ 需求覆盖：所有需求至少被一个开发包覆盖
│  │  □ 无循环依赖：包间依赖图为 DAG
│  │  □ 接口依赖方向一致：消费者在提供者之后
│  │  □ 粒度检查：每个包预估 ≤2 人周
│  │  □ 无孤立包：每个包至少有一个需求
│  ├─ 输出：验证报告 + 签名
│  └─ 签名：sha256(...)
└─ 输出：DCS 验证报告

Step 4: PDT / 架构师 / 测试负责人联合审批
├─ PDT Workshop 审查候选方案
│  ├─ 调整包边界（合并过小/拆分过大）
│  ├─ 确认每个包的负责人（owner）
│  ├─ 确认包间依赖排序
│  ├─ 确认接口 owner
│  └─ 确认测试策略
├─ 三方在 yudao 中联合签署切片方案
└─ 输出：DevelopmentPackageSet（已签署）

Step 5: yudao 创建子流程
├─ 为每个开发包创建子流程实例
├─ 绑定 SLA（每个包 ≤2 周）
├─ 绑定升级规则（超时自动升级到 PDT Lead）
└─ 输出：子流程实例列表
```

#### 效率优化策略

| 策略 | 说明 | 预期效果 |
|---|---|---|
| 纵向切片 | 每个包可独立验证，减少集成冲突 | 提高并行开发效率 |
| ≤2 人周粒度 | 来自 DORA 研究，小包降低变更风险 | 提高 CI 反馈速度 |
| DCS 图约束检查 | 确定性验证需求覆盖和无环 | 消除人工检查遗漏 |
| 历史数据参考 | 参考历史包的工期和缺陷数据 | 提高估算精度 |

### 输出 Artifact 模板

#### Artifact 名称
`DevelopmentPackageSet`

#### 文件结构
```
development_package_set/
├── manifest.yaml
├── package_set.json              # 主数据文件
├── packages/                     # 每个开发包的详细定义
│   ├── DP-001.json
│   ├── DP-002.json
│   └── ...
├── dependency_graph.json         # 包间依赖 DAG
├── dcs_receipts/
│   └── slice_validate.json
└── README.md
```

#### package_set.json Schema

```json
{
  "package_set_id": "dps-20260818-001",
  "version": "1.0.0",
  "created_at": "2026-08-18T13:00:00Z",
  "created_by": "agent-development-orchestrator-001",
  "signed_by": [
    {"user_id": "user-pdt-001", "role": "PDT Manager", "signed_at": "2026-08-18T16:00:00Z"},
    {"user_id": "user-architect-001", "role": "Architect", "signed_at": "2026-08-18T16:15:00Z"},
    {"user_id": "user-test-lead-001", "role": "Test Lead", "signed_at": "2026-08-18T16:30:00Z"}
  ],
  "baseline_ref": "dbl-20260818-001",
  "statistics": {
    "total_packages": 5,
    "total_requirements_covered": 45,
    "total_estimated_effort_weeks": 8.5,
    "max_package_effort_weeks": 2.0,
    "dependency_depth": 3
  },
  "packages": [
    {
      "id": "DP-001",
      "name": "代码差异解析引擎",
      "requirements": ["REQ-001", "REQ-002", "REQ-003", "REQ-004", "REQ-005"],
      "containers": ["Review Engine"],
      "interfaces": [
        {
          "id": "IF-DIFF-01",
          "spec_uri": "git://repo/api/diff.yaml",
          "version": "1.0.0",
          "contract_hash": "sha256:a1b2c3d4"
        }
      ],
      "cbb_usage": ["CBB-TREE-SITTER-v0.22"],
      "risks": ["R-001: tree-sitter 语言支持不完整"],
      "tests": {
        "unit_strategy": "边界值测试（空 diff、超大 diff、binary diff、rename diff）",
        "integration_strategy": "端到端 diff 解析 + AST 提取",
        "coverage_target": 85
      },
      "done_criteria": [
        "所有 REQ-001~005 验收标准通过",
        "单元测试覆盖率 ≥85%",
        "SAST 无 Critical/High 未解决",
        "接口契约测试通过",
        "代码审查完成（≥2 名非作者审查者）",
        "SBOM 生成且许可证合规"
      ],
      "owner": "dev-lead-01",
      "estimated_effort": "1.5 person-weeks",
      "dependencies": []
    },
    {
      "id": "DP-002",
      "name": "AI 审查推理层",
      "requirements": ["REQ-006", "REQ-007", "REQ-008", "REQ-009", "REQ-010"],
      "containers": ["ML Service"],
      "interfaces": [
        {
          "id": "IF-ML-01",
          "spec_uri": "git://repo/api/ml.yaml",
          "version": "1.0.0",
          "contract_hash": "sha256:e5f6g7h8"
        }
      ],
      "cbb_usage": [],
      "risks": ["R-002: ML 模型推理延迟超标", "R-003: 多模型并行管道复杂度"],
      "tests": {
        "unit_strategy": "模型推理单元测试 + Mock 外部模型",
        "integration_strategy": "端到端审查管道测试",
        "coverage_target": 80
      },
      "done_criteria": [
        "所有 REQ-006~010 验收标准通过",
        "单元测试覆盖率 ≥80%",
        "P95 推理延迟 < 30s",
        "多模型并行管道稳定运行"
      ],
      "owner": "dev-lead-02",
      "estimated_effort": "2.0 person-weeks",
      "dependencies": ["DP-001"]
    }
  ],
  "dependency_graph": {
    "nodes": ["DP-001", "DP-002", "DP-003", "DP-004", "DP-005"],
    "edges": [
      {"from": "DP-001", "to": "DP-002"},
      {"from": "DP-001", "to": "DP-003"},
      {"from": "DP-002", "to": "DP-004"},
      {"from": "DP-003", "to": "DP-005"}
    ],
    "critical_path": ["DP-001", "DP-002", "DP-004"],
    "max_parallelism": 2
  },
  "dcs_receipt": {
    "solver_version": "slice-validate-v1",
    "input_hash": "sha256:...",
    "signature": "sha256:...",
    "checks": {
      "requirement_coverage": "pass (45/45)",
      "no_cycles": "pass",
      "interface_direction": "pass",
      "granularity": "pass (max 2.0 weeks)"
    }
  }
}
```

### 节点关联与展示

#### 在整体流程中的位置

```
ACT-03-04-01 基线确认 → ACT-03-04-02 开发包切片 → ACT-03-04-03~11（各包并行开发）
                                ↓
                        DevelopmentPackageSet
                                ↓
                        [每个包创建子流程]
```

#### 上游依赖
| 上游 Activity | 依赖内容 | 依赖方式 |
|---|---|---|
| ACT-03-04-01 基线确认 | DevelopmentBaselineManifest | 作为切片的输入基线 |

#### 下游消费
| 下游 Activity | 消费内容 | 消费方式 |
|---|---|---|
| ACT-03-04-03~11 | 各开发包的定义 | 指导每个包的开发实施 |

---
