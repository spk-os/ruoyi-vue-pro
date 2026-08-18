## ACT-03-03-05 测试策略规划

### 基础信息

| 字段 | 内容 |
|---|---|
| **Activity ID** | ACT-03-03-05 |
| **Activity 名称** | 测试策略规划 |
| **所属阶段** | 计划 |
| **Lead Agent 角色** | Planning Lead |
| **执行位置** | task_system |
| **是否使用 Worker Agent** | — |
| **是否需要 Independent Verifier** | — |
| **所需模型能力** | GROUNDED_EXTRACTION + STRUCTURED_DRAFTING |
| **所需 Skill** | ipd-plan-test-strategy / spk-rtm-builder / spk-test-case-generator |
| **所需 Tool/MCP** | mcp-spk-os-artifact-query / mcp-filesystem-read / mcp-spk-os-test-query |
| **所需 Knowledge** | KN-IPD-RTM-SCHEMA-012 / KN-IPD-TEST-STRATEGY-013 |
| **所需 Information** | INFO-PRSBASELINE / INFO-ARCHITECTURE / INFO-TEST-ENVIRONMENTS（验证环境清单） |
| **可读取的 Memory 范围** | project:spk-os-ipd, agent_role:planning, capability:grounded_extraction |
| **人工责任** | Test Architect / QA Lead 签署测试策略和豁免 |
| **失败处理** | 链接生成失败按需求分批重试；仍失败人工映射；关键需求无 Oracle 升级 QA Lead；回滚到上版 RTM 并撤销错误链接 |
| **对应 Flowable 节点** | task_test_strategy (ipd-plan.bpmn) |
| **证据来源** | RunReceipt + ToolReceipt + ArtifactManifest |

### 资源与工具详情

#### 输入资源

| 资源 | 来源 | 获取方式 | 质量要求 |
|---|---|---|---|
| PRSBaselineCandidate | ACT-03-03-01 产物 | mcp-spk-os-artifact-query.get() | PM+SE 签署，含风险等级 |
| ArchitectureDescriptionCandidate | ACT-03-03-02 产物 | mcp-spk-os-artifact-query.get() | Chief Architect 签署 |
| 验证环境清单 | 测试团队 | mcp-spk-os-test-query.list_environments() | 含环境能力、数据准备方式 |
| 历史测试数据 | 测试管理系统 | mcp-spk-os-test-query.get_history(project_type) | 含缺陷密度、覆盖率 |
| RTM Schema | Knowledge 库 | mcp-filesystem-read(/knowledge/ipd/schemas/rtm.yaml) | 追溯矩阵结构定义 |

#### 工具调用顺序与效率策略

```
Step 1: 加载签名输入
├─ 调用 mcp-spk-os-artifact-query.get(PRSBaselineCandidate)
├─ 调用 mcp-spk-os-artifact-query.get(ArchitectureDescriptionCandidate)
├─ 调用 mcp-spk-os-test-query.list_environments()
└─ 输出：测试策略 Context Pack

Step 2: 按需求风险分层绑定验证方法
├─ 调用 spk-rtm-builder.bind_verification_methods(prs, architecture)
│  ├─ 高风险需求：必须绑定至少两种互补验证方法（测试+分析 / 测试+检查 / 测试+演示）
│  ├─ 中风险需求：绑定至少一种验证方法
│  ├─ 低风险需求：建议验证方法
│  ├─ 验证方法类型：
│  │  ├─ 功能测试：自动化 Pytest + 手动探索测试
│  │  ├─ 性能测试：JMeter/Locust 负载测试 + P95 阈值
│  │  ├─ 安全测试：OWASP ZAP 扫描 + 渗透测试
│  │  ├─ 集成测试：接口契约测试 + 端到端测试
│  │  ├─ 分析：代码审查 + 静态分析
│  │  ├─ 检查：文档审查 + 配置审查
│  │  └─ 演示：功能演示 + 用户验收
│  └─ 输出：验证方法绑定表

Step 3: LLM 生成高风险需求的测试用例草稿
├─ 调用 spk-test-case-generator.generate(high_risk_requirements, architecture)
│  ├─ 每个测试用例含：测试目标、前置条件、步骤、预期结果、判定准则
│  ├─ 包含正例和负例
│  ├─ 包含边界值测试
│  └─ 输出：测试用例草稿集合

Step 4: 指定测试环境和数据
├─ 为每个验证方法指定：
│  ├─ 测试环境（开发/测试/预生产/生产镜像）
│  ├─ 测试数据准备方式（fixture/工厂/生产脱敏）
│  ├─ Oracle（判定结果的来源：自动化断言/人工判断/对比基准）
│  └─ Oracle Owner（负责判定结果的人/系统）
└─ 输出：环境-数据-Oracle 映射

Step 5: DCS 计算 RTM 覆盖率（确定性）
├─ 调用 DCS POST /api/v1/rtm-coverage
│  ├─ 输入：需求列表（含风险等级）+ 验证方法绑定
│  ├─ 输出：
│  │  ├─ 总需求数 / 已覆盖数 / 覆盖率
│  │  ├─ 高风险覆盖率（必须 = 100%）
│  │  ├─ 未覆盖的高风险需求列表
│  │  └─ 签名：sha256(...)
│  └─ 如果高风险覆盖率 < 100% → 阻断 TR2 就绪
└─ 输出：RTM 覆盖率报告

Step 6: Test Architect / QA Lead 人工审核
├─ 确认验证方法选择的合理性
├─ 确认测试环境可获得性
├─ 处理缺口（补充覆盖或风险接受）
├─ 审批 Oracle 和豁免
├─ 签署测试策略
└─ 输出：VerificationStrategy（已签署）
```

### 输出 Artifact 模板

#### Artifact 名称
`VerificationStrategy`

#### 文件结构
```
verification_strategy/
├── manifest.yaml
├── strategy.json                 # 主数据文件
├── rtm/                          # 需求追溯矩阵
│   └── rtm.json
├── test_cases/                   # 测试用例草稿
│   ├── tc-api-001.json
│   └── ...
├── environments/                 # 环境-数据-Oracle 映射
│   └── env_mapping.json
├── coverage/                     # 覆盖率报告
│   └── coverage_report.json
└── README.md
```

#### strategy.json Schema

```json
{
  "strategy_id": "vs-20260817-001",
  "version": "1.0.0",
  "created_at": "2026-08-17T21:00:00Z",
  "created_by": "agent-planning-001",
  "signed_by": {
    "user_id": "user-qa-lead-001",
    "role": "QA Lead",
    "signed_at": "2026-08-18T01:00:00Z",
    "decision": "approved"
  },
  "input_artifacts": {
    "prs_baseline_ref": "prs-20260817-001",
    "architecture_ref": "ad-20260817-001"
  },
  "coverage": {
    "total_requirements": 45,
    "covered": 43,
    "coverage_pct": 95.6,
    "high_risk_total": 12,
    "high_risk_covered": 12,
    "high_risk_coverage_pct": 100.0,
    "uncovered": ["REQ-043", "REQ-044"],
    "uncovered_risk_level": "low"
  },
  "verification_methods": [
    {
      "requirement_id": "REQ-AURORA-PLAN-001",
      "risk_level": "medium",
      "methods": [
        {
          "type": "functional_test",
          "description": "自动化 Pytest 测试 REST API 端点",
          "environment": "test",
          "data_source": "fixture",
          "oracle": "automated_assertion",
          "oracle_owner": "CI Pipeline"
        },
        {
          "type": "performance_test",
          "description": "JMeter 负载测试，验证 P95 < 500ms",
          "environment": "pre_production",
          "data_source": "production_anonymized",
          "oracle": "threshold_check",
          "oracle_owner": "Performance Team"
        }
      ],
      "test_case_refs": ["tc-api-001", "tc-api-perf-001"]
    }
  ],
  "exemptions": [
    {
      "requirement_id": "REQ-043",
      "reason": "低风险 cosmetic 需求，通过代码审查覆盖",
      "approved_by": "user-qa-lead-001",
      "alternative": "code_review"
    }
  ],
  "dcs_receipt": {
    "solver_version": "rtm-v1",
    "input_hash": "sha256:...",
    "signature": "sha256:..."
  }
}
```

### 节点关联与展示

#### 在整体流程中的位置

```
ACT-03-03-01 + ACT-03-03-02 → ACT-03-03-05 测试策略规划 → ACT-03-03-06 TR2/PDCP 决策包组装
                                    ↓
                            VerificationStrategy
```

#### 上游依赖
| 上游 Activity | 依赖内容 | 依赖方式 |
|---|---|---|
| ACT-03-03-01 产品需求规格化 | PRSBaselineCandidate（需求 + 风险等级） | 作为 RTM 的需求端 |
| ACT-03-03-02 架构描述生成 | ArchitectureDescriptionCandidate（接口 + 质量属性） | 作为集成测试和性能测试的依据 |

#### 下游消费
| 下游 Activity | 消费内容 | 消费方式 |
|---|---|---|
| ACT-03-03-06 TR2/PDCP 决策包组装 | VerificationStrategy（测试策略 + RTM） | 作为技术证据的一部分 |
| 验证阶段 | VerificationStrategy 中的测试用例和环境规划 | 指导测试执行 |

#### 可视化展示

**在 SPK-OS Dashboard 的展示方式**：
1. **RTM 追溯矩阵**：交互式矩阵，行=需求，列=验证方法，单元格=覆盖状态
2. **覆盖率仪表盘**：高风险/中风险/低风险的覆盖率进度条
3. **测试环境拓扑图**：可视化展示各环境的用途和数据流
4. **缺口面板**：高亮未覆盖的需求，支持一键补充

---
