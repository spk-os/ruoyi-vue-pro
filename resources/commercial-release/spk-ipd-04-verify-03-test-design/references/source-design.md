## ACT-03-05-03 设计多层测试

### 基础信息

| 字段 | 内容 |
|---|---|
| **Activity ID** | ACT-03-05-03 |
| **Activity 名称** | 设计多层测试 |
| **所属阶段** | 验证 |
| **Lead Agent 角色** | Verification Orchestrator |
| **执行位置** | task_system |
| **是否使用 Worker Agent** | ✓（Test Designer Agent 生成候选用例 + Evidence Critic Agent 审查遗漏） |
| **是否需要 Independent Verifier** | ✓（DCS 计算覆盖 + Evidence Critic Agent 独立审查） |
| **所需模型能力** | CROSS_SOURCE_REASONING + STRUCTURED_DRAFTING + COUNTERARGUMENT_GENERATION |
| **所需 Skill** | ipd-verify-test-design / spk-test-designer / spk-evidence-critic |
| **所需 Tool/MCP** | mcp-spk-os-artifact-query / mcp-filesystem-read |
| **所需 Knowledge** | KN-ISO-29119-4-TEST-TECH-041 / KN-ISO-25010-QUALITY-042 / KN-ISTQB-TEST-DESIGN-043 |
| **所需 Information** | INFO-RVM-RTM（验证矩阵）/ INFO-RISK-REGISTER（风险清单）/ INFO-INTERFACE-SPECS（接口规格） |
| **可读取的 Memory 范围** | project:spk-os-ipd, agent_role:verification, capability:structured_drafting |
| **人工责任** | Test Architect 审批策略与 Oracle；SE 确认关键路径覆盖 |
| **失败处理** | 生成重复用例时按风险维度重试；仍不足人工设计；安全/性能不可测升级 Test Board；回滚未批准用例集 |
| **对应 Flowable 节点** | task_test_design (ipd-qualification.bpmn) |
| **证据来源** | RunReceipt + ArtifactManifest + HumanApprovalRecord |

### 资源与工具详情

#### 输入资源

| 资源 | 来源 | 获取方式 | 质量要求 |
|---|---|---|---|
| RVM_RTM_Baseline | ACT-03-05-02 产物 | mcp-spk-os-artifact-query.get() | SE+Test Architect 签署 |
| 风险清单 | 项目管理 | mcp-spk-os-artifact-query.get() | 含风险等级和缓解措施 |
| 接口规格 | 架构基线 | mcp-filesystem-read(/api/specs/) | OpenAPI/Protobuf 定义 |
| ISO 25010 质量模型 | Knowledge 库 | mcp-filesystem-read(/knowledge/iso-25010/) | 9 项质量特性 |
| 历史缺陷模式 | AgentMemory | memory.query(scope="defect_pattern") | 含根因和修复方法 |

#### 工具调用顺序与效率策略

```
Step 1: 确定测试分层策略
├─ Test Architect 确定分层比例（基于风险和变更频率）：
│  ├─ 小测试（单元）：70%（快速反馈，高覆盖）
│  ├─ 中测试（集成）：20%（接口验证，契约测试）
│  └─ 大测试（端到端）：10%（关键场景，用户旅程）
├─ 按 ISO 25010 质量特性分层：
│  ├─ 功能适用性：单元 + 集成 + 端到端
│  ├─ 性能效率：性能测试 + 负载测试 + 压力测试
│  ├─ 兼容性：兼容性测试矩阵
│  ├─ 可用性：可用性测试 + 无障碍测试
│  ├─ 可靠性：故障注入 + 恢复测试
│  ├─ 安全性：渗透测试 + 权限测试 + 注入测试
│  ├─ 可维护性：代码质量指标 + 重构测试
│  └─ 可移植性：跨平台测试
└─ 输出：测试分层策略

Step 2: Test Designer Agent 生成候选用例
├─ 调用 spk-test-designer.generate(rvm, risk_register, interface_specs, layer_strategy)
│  ├─ 对每条需求生成候选测试用例：
│  │  ├─ 等价类划分（有效/无效输入，至少 2+2）
│  │  ├─ 边界值分析（最小/最大/边界±1）
│  │  ├─ 状态转换（关键状态机，完整状态覆盖）
│  │  ├─ 属性测试（不变量验证，随机输入）
│  │  ├─ 反例测试（负面场景，至少 2 个/需求）
│  │  ├─ 故障注入（异常路径，降级场景）
│  │  └─ 端到端场景（关键用户旅程）
│  ├─ 每个用例含：
│  │  ├─ test_id, requirement_id, method
│  │  ├─ 前置条件
│  │  ├─ 输入数据
│  │  ├─ 期望输出（Oracle）
│  │  │  **Oracle 规则**：
│  │  │  - 数值型：精确值或范围
│  │  │  - 行为型：引用确定性规则或人工判定标准
│  │  │  - 禁止"应该正常工作"等模糊表述
│  │  ├─ 后置条件
│  │  ├─ 环境要求
│  │  └─ 执行时间预估
│  └─ 输出：候选用例集
└─ 输出：MultiLayerTestDesign（草稿）

Step 3: DCS 覆盖计算
├─ 调用 DCS POST /api/v1/test-coverage-calc
│  ├─ 输入：候选用例集 + RVM
│  ├─ 计算：
│  │  □ 需求覆盖率（有测试用例的需求比例）
│  │  □ 方法覆盖率（RVM 中每种验证方法都有对应用例）
│  │  □ 风险覆盖率（高风险项至少 2 种互补验证）
│  │  □ 负面场景比例（≥20%）
│  │  □ 边界值覆盖率（每个边界至少 1 个用例）
│  ├─ 输出：覆盖报告 + 签名
│  └─ 签名：sha256(...)
└─ 输出：DCS 覆盖报告

Step 4: Evidence Critic Agent 独立审查
├─ 使用不同模型族（与 Test Designer Agent 隔离）
├─ 审查用例集完整性：
│  ├─ 寻找遗漏的负面场景
│  ├─ 寻找遗漏的边界条件
│  ├─ 检查 Oracle 是否足够精确
│  ├─ 检查同源偏差风险（测试与代码是否由同一 AI 生成）
│  └─ 检查关键路径是否有至少两种互补验证
├─ 生成审查报告（遗漏列表 + 建议补充）
├─ **不投票，不批准，只质询**
└─ 输出：CriticReport

Step 5: Test Architect 审批
├─ 审查候选用例集 + DCS 覆盖报告 + Critic 报告
├─ 处理 Critic 发现的遗漏（补充/标记理由）
├─ 确认 Oracle 来源正确
├─ 确认覆盖 RVM 中所有方法
├─ 签署用例集
└─ 输出：MultiLayerTestDesign（已签署）
```

#### 效率优化策略

| 策略 | 说明 | 预期效果 |
|---|---|---|
| 风险分层 | 高风险项更多测试资源 | 优化测试投入产出比 |
| ISO 25010 全覆盖 | 8 项质量特性逐一覆盖 | 避免非功能需求遗漏 |
| Evidence Critic 独立审查 | 不同模型族交叉验证 | 减少同源偏差 |
| 负面场景强制比例 | ≥20% 用例为负面/反例 | 防止"只测正常路径" |

### 输出 Artifact 模板

#### Artifact 名称
`MultiLayerTestDesign`

#### 文件结构
```
multi_layer_test_design/
├── manifest.yaml
├── test_design.json              # 主数据文件
├── test_cases/                   # 用例详情
│   ├── unit/
│   ├── integration/
│   ├── system/
│   └── e2e/
├── coverage_report.json          # DCS 覆盖报告
├── critic_report.json            # Evidence Critic 报告
└── README.md
```

#### test_design.json Schema

```json
{
  "design_id": "mtd-20260828-001",
  "version": "1.0.0",
  "created_at": "2026-08-28T14:00:00Z",
  "created_by": "agent-test-designer-001",
  "signed_by": {
    "user_id": "user-test-architect-001",
    "role": "Test Architect",
    "signed_at": "2026-08-28T17:00:00Z"
  },
  "rvm_ref": "rvm-20260828-001",
  "layer_strategy": {
    "small_tests_pct": 70,
    "medium_tests_pct": 20,
    "large_tests_pct": 10,
    "rationale": "基于 DORA 研究和项目风险画像"
  },
  "statistics": {
    "total_test_cases": 180,
    "unit_tests": 126,
    "integration_tests": 36,
    "e2e_tests": 18,
    "negative_scenarios": 42,
    "negative_pct": 23.3,
    "boundary_tests": 35,
    "fault_injection_tests": 12
  },
  "test_cases": [
    {
      "test_id": "TC-PERF-001",
      "requirement_id": "REQ-001",
      "method": "test",
      "layer": "system",
      "type": "performance",
      "preconditions": "系统已部署到 staging 环境，负载为 100 并发用户",
      "input": "提交 1000 行代码的 PR 进行审查",
      "oracle": {
        "source": "deterministic_rule",
        "value": "p95_latency_ms < 500",
        "measurement": "从提交到返回结果的 P95 延迟"
      },
      "postconditions": "审查结果已返回，日志已记录",
      "environment": "staging",
      "estimated_duration_sec": 60,
      "risk_level": "high"
    },
    {
      "test_id": "TC-SEC-001",
      "requirement_id": "REQ-042",
      "method": "test",
      "layer": "integration",
      "type": "security",
      "preconditions": "Agent 服务账号已创建，无审批权限",
      "input": "Agent 使用服务账号调用审批 API",
      "oracle": {
        "source": "deterministic_rule",
        "value": "HTTP 403 Forbidden, error_code=INSUFFICIENT_PERMISSIONS",
        "measurement": "API 响应状态码和错误码"
      },
      "postconditions": "审批状态未改变，审计日志已记录",
      "environment": "staging",
      "estimated_duration_sec": 5,
      "risk_level": "high",
      "negative_scenario": true
    }
  ],
  "coverage": {
    "requirement_coverage_pct": 100,
    "method_coverage_pct": 100,
    "risk_coverage_pct": 100,
    "negative_scenario_pct": 23.3,
    "boundary_coverage_pct": 100
  },
  "critic_findings": [
    {
      "finding_id": "CF-001",
      "severity": "major",
      "description": "缺少并发用户 >100 时的竞态条件测试",
      "affected_requirements": ["REQ-001", "REQ-015"],
      "suggestion": "补充 3 个并发竞态测试用例",
      "resolution": "accepted",
      "supplementary_tests": ["TC-CONC-001", "TC-CONC-002", "TC-CONC-003"]
    }
  ],
  "dcs_receipt": {
    "solver_version": "test-coverage-calc-v1",
    "input_hash": "sha256:...",
    "signature": "sha256:..."
  }
}
```

### 节点关联与展示

#### 在整体流程中的位置

```
ACT-03-05-02 建立 RVM/RTM → ACT-03-05-03 设计多层测试 → ACT-03-05-04 准备环境与数据
                                ↓
                        MultiLayerTestDesign
                                ↓
                        [180 个用例，覆盖 45 条需求]
```

---
