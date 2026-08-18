## ACT-03-03-02 架构描述生成

### 基础信息

| 字段 | 内容 |
|---|---|
| **Activity ID** | ACT-03-03-02 |
| **Activity 名称** | 架构描述生成 |
| **所属阶段** | 计划 |
| **Lead Agent 角色** | Planning Lead |
| **执行位置** | task_system |
| **是否使用 Worker Agent** | ✓（Generator + Critic 双通道，非并行，迭代最多 3 轮） |
| **是否需要 Independent Verifier** | — |
| **所需模型能力** | CROSS_SOURCE_REASONING + COUNTERARGUMENT_GENERATION + STRUCTURED_DRAFTING |
| **所需 Skill** | ipd-plan-architecture-design / spk-c4-model-generator / spk-adr-generator / spk-architecture-critic |
| **所需 Tool/MCP** | mcp-spk-os-artifact-query / mcp-spk-os-cbb-query / mcp-codegraph-explore / mcp-filesystem-read |
| **所需 Knowledge** | KN-ISO-42010-004 / KN-IPD-C4-TEMPLATE-005 / KN-IPD-ADR-TEMPLATE-006 |
| **所需 Information** | INFO-PRSBASELINE（已批准的 PRS）/ INFO-CBB-CATALOG（CBB 目录） |
| **可读取的 Memory 范围** | project:spk-os-ipd, agent_role:planning, capability:cross_source_reasoning |
| **人工责任** | Chief Architect 签署架构基线和 ADR |
| **失败处理** | 上下文超限按子域分片重试；降级为人工建模；安全/数据驻留冲突升级 Architecture Board；回滚未批准 ADR 和视图分支 |
| **对应 Flowable 节点** | task_architecture_design (ipd-plan.bpmn) |
| **证据来源** | RunReceipt + ToolReceipt + ArtifactManifest |

### 资源与工具详情

#### 输入资源

| 资源 | 来源 | 获取方式 | 质量要求 |
|---|---|---|---|
| PRSBaselineCandidate | ACT-03-03-01 产物 | mcp-spk-os-artifact-query.get(artifact_type="PRSBaselineCandidate") | 必须含 PM+SE 签署 |
| CBB 目录 | 平台团队 Registry | mcp-spk-os-cbb-query.list_all() | 每个 CBB 含 7 字段契约 |
| 质量属性场景 | 概念阶段技术风险 | mcp-spk-os-artifact-query.get(artifact_type="TechnicalRiskAndCBBMap") | 含 SE/TDT 签署 |
| ISO 42010 关注点框架 | Knowledge 库 | mcp-filesystem-read(/knowledge/standards/iso-42010-concerns.yaml) | 关注点-观点-模型映射 |
| C4 模型模板 | Knowledge 库 | mcp-filesystem-read(/knowledge/ipd/templates/c4/) | Context/Container/Component 模板 |
| ADR 模板库 | Knowledge 库 | mcp-filesystem-read(/knowledge/ipd/templates/adr/) | 含状态/上下文/决策/替代/后果 |
| 现有代码库结构 | 代码仓库 | mcp-codegraph-explore.explore(repo) | 用于理解现有架构约束 |

#### 工具调用顺序与效率策略

```
Step 1: 加载签名输入
├─ 调用 mcp-spk-os-artifact-query.get(PRSBaselineCandidate)
├─ 调用 mcp-spk-os-cbb-query.list_all() 获取 CBB 目录
├─ 调用 mcp-spk-os-artifact-query.get(TechnicalRiskAndCBBMap)
├─ 校验所有输入的签名和 hash
└─ 输出：架构设计 Context Pack

Step 2: Generator Agent 生成候选架构（第 1 轮）
├─ 调用 spk-c4-model-generator.generate_context(prs, cbb_catalog)
│  ├─ 生成 C4 Context 图：系统与外部实体（用户、第三方系统、数据源）的关系
│  └─ 输出：Context 图描述（文字 + Mermaid）
├─ 调用 spk-c4-model-generator.generate_container(prs, cbb_catalog, context)
│  ├─ 生成 C4 Container 图：系统内部的容器（应用、数据库、消息队列等）
│  ├─ 每个容器标注：技术栈、职责、CBB 复用情况
│  └─ 输出：Container 图描述
├─ 调用 spk-adr-generator.generate(prs, containers, constraints)
│  ├─ 为每个重要架构决策生成 ADR 草稿
│  ├─ 每个 ADR 含：状态/上下文/决策/替代方案/后果
│  └─ 输出：ADR 集合
├─ 生成质量属性场景集合
│  ├─ 性能场景：负载模型、响应时间目标、吞吐量目标
│  ├─ 安全场景：威胁模型、认证授权、数据加密
│  ├─ 可靠性场景：故障模式、恢复策略、容灾方案
│  └─ 可维护性场景：可观测性、日志策略、升级路径
├─ 生成接口列表
│  ├─ 每个接口含：协议/方法/数据格式/错误处理/版本
│  └─ 输出：接口控制描述（ICD）草稿
└─ 输出：ArchitectureDescriptionCandidate（第 1 轮草稿）

Step 3: Critic Agent 审查架构（第 1 轮）
├─ 调用 spk-architecture-critic.review(architecture_draft, iso_42010_framework)
│  ├─ 基于 ISO/IEC/IEEE 42010 关注点框架逐项审查：
│  │  □ 每个关键质量属性是否有明确的场景描述？
│  │  □ 每个场景是否有对应的架构模型/视图？
│  │  □ 重要决策是否有替代方案评估记录？
│  │  □ 接口契约是否完整（协议/方法/数据格式/错误处理/版本）？
│  │  □ 是否存在循环依赖？
│  │  □ CBB 集成点的兼容性是否验证？
│  │  □ 安全威胁模型是否覆盖主要攻击面？
│  │  □ 部署视图是否描述运行环境要求？
│  ├─ 对每项检查给出 PASS/FAIL + 具体修改建议
│  └─ 输出：审查报告
└─ 如果全部 PASS → 跳到 Step 5

Step 4: Generator-Critic 迭代（最多 3 轮）
├─ 将 Critic 的修改建议反馈给 Generator
├─ Generator 修改架构描述
├─ Critic 重新审查
├─ 如果 3 轮后仍有 FAIL → 停止迭代，保留当前版本，升级 Chief Architect 手动审查
└─ 输出：最终 ArchitectureDescriptionCandidate

Step 5: DCS 确定性检查
├─ 调用 DCS /api/v1/interface-consistency
│  ├─ 检查所有接口签名的匹配性
│  └─ 输出：接口一致性报告
├─ 调用 DCS /api/v1/dependency-cycle
│  ├─ 检查依赖图中是否存在环
│  └─ 输出：依赖环检测报告
└─ 输出：确定性检查报告

Step 6: Chief Architect 人工审核
├─ 审核 C4 视图的完整性和合理性
├─ 审批每条 ADR（接受/修改/拒绝）
├─ 确认质量属性场景覆盖度
├─ 处理 Critic 遗留的 FAIL 项
├─ 签署架构基线
└─ 输出：ArchitectureDescriptionCandidate（已签署）
```

#### 效率优化策略

| 策略 | 说明 | 预期效果 |
|---|---|---|
| Generator-Critic 双通道 | Critic 基于 ISO 42010 框架系统性暴露 Generator 盲点 | 关注点覆盖率从 ~70% 提升到 ~95% |
| 迭代上限 3 轮 | 防止无限循环，3 轮后升级人工 | 保证收敛性 |
| C4 模型分层生成 | Context → Container → Component 逐层细化 | 减少单次 LLM 调用的上下文压力 |
| DCS 确定性检查 | 接口一致性和依赖环检测不依赖 LLM | 消除 LLM 在图计算上的幻觉 |

### 输出 Artifact 模板

#### Artifact 名称
`ArchitectureDescriptionCandidate`

#### 文件结构
```
architecture_description_candidate/
├── manifest.yaml
├── architecture.json             # 主数据文件
├── views/                        # C4 视图
│   ├── context.md                # Context 图（文字 + Mermaid）
│   ├── container.md              # Container 图
│   └── component/                # Component 图（按容器拆分）
│       ├── review-engine.md
│       └── api-gateway.md
├── interfaces/                   # 接口控制描述
│   └── icd.json
├── adr/                          # 架构决策记录
│   ├── ADR-001-event-driven.md
│   ├── ADR-002-multi-model-pipeline.md
│   └── ...
├── quality_attributes/           # 质量属性场景
│   └── quality_scenarios.json
├── critic_reports/               # Critic 审查报告
│   ├── round-1.json
│   ├── round-2.json
│   └── final.json
├── dcs_reports/                  # 确定性检查报告
│   ├── interface_consistency.json
│   └── dependency_cycle.json
└── README.md
```

#### architecture.json Schema

```json
{
  "architecture_id": "ad-20260817-001",
  "version": "1.0.0",
  "as_of": "2026-08-17",
  "created_at": "2026-08-17T15:00:00Z",
  "created_by": "agent-planning-001",
  "signed_by": {
    "user_id": "user-chief-architect-001",
    "role": "Chief Architect",
    "signed_at": "2026-08-17T18:00:00Z",
    "decision": "approved"
  },
  "input_artifacts": {
    "prs_baseline_ref": "prs-20260817-001",
    "cbb_catalog_ref": "cbb-catalog-20260815",
    "tech_risk_ref": "trcm-20260815-001"
  },
  "generator_critic_iterations": 2,
  "views": {
    "context": {
      "description": "Aurora 系统与外部实体的交互",
      "external_entities": [
        {"name": "Git 平台", "protocol": "WebSocket/REST", "direction": "bidirectional"},
        {"name": "用户（IDE 插件）", "protocol": "HTTPS", "direction": "inbound"},
        {"name": "用户（Web UI）", "protocol": "HTTPS", "direction": "inbound"},
        {"name": "通知服务", "protocol": "Kafka", "direction": "outbound"}
      ],
      "mermaid_diagram": "graph LR\n  User[用户] -->|HTTPS| Aurora\n  Git[Git平台] -->|WebSocket| Aurora\n  Aurora -->|Kafka| Notify[通知服务]"
    },
    "containers": [
      {
        "name": "Review Engine",
        "technology": "Python 3.12",
        "responsibility": "代码审查核心逻辑",
        "cbb_usage": ["CBB-ML-PIPELINE-v1"],
        "quality_attributes": {"performance": "P95 < 30s per review", "reliability": "99.9% uptime"}
      },
      {
        "name": "API Gateway",
        "technology": "Node.js 20",
        "responsibility": "REST API 路由、限流、认证",
        "cbb_usage": ["CBB-API-GATEWAY-v2"],
        "quality_attributes": {"performance": "P95 < 100ms", "security": "OAuth2 + API Key"}
      }
    ]
  },
  "interfaces": [
    {
      "interface_id": "IF-001",
      "name": "Review Results API",
      "protocol": "HTTPS/REST",
      "methods": ["GET /api/v1/reviews", "POST /api/v1/reviews"],
      "data_format": "JSON",
      "error_handling": "标准 HTTP 状态码 + 错误体",
      "version": "v1",
      "consumer": ["外部系统", "IDE 插件"],
      "provider": "API Gateway"
    }
  ],
  "adr_count": 5,
  "quality_attribute_coverage": {
    "performance": {"scenarios": 3, "status": "covered"},
    "security": {"scenarios": 2, "status": "covered"},
    "reliability": {"scenarios": 2, "status": "covered"},
    "maintainability": {"scenarios": 2, "status": "covered"},
    "scalability": {"scenarios": 1, "status": "covered"}
  },
  "dcs_checks": {
    "interface_consistency": "pass",
    "dependency_cycle": "pass"
  },
  "requirement_traceability": {
    "total_requirements": 45,
    "traced_to_architecture": 42,
    "coverage_pct": 93.3,
    "untraced": ["REQ-043", "REQ-044", "REQ-045"]
  }
}
```

#### ADR 模板示例

```markdown
# ADR-001: 使用事件驱动架构处理代码审查通知

## 状态
已批准 (2026-08-17)

## 上下文
代码审查完成后需要通知多个下游系统（CI/CD、IDE 插件、通知服务）。
需要支持异步处理以避免阻塞主审查流程。

## 决策
采用事件驱动架构，使用 Apache Kafka 作为事件总线：
- 审查完成事件发布到 kafka://reviews.completed topic
- 各下游系统作为消费者独立订阅和处理
- 事件 Schema 注册在 Schema Registry

## 替代方案
1. **直接 API 调用**: 简单但紧耦合，新增消费者需修改审查服务
2. **Webhook**: 可行但重试和顺序保证弱于 Kafka
3. **消息队列(RabbitMQ)**: 可行但团队更熟悉 Kafka 运维

## 后果
- ✅ 松耦合，新增消费者无需修改生产者
- ✅ Kafka 提供持久化和回放能力
- ⚠️ 引入 Kafka 运维复杂度
- ⚠️ 需要额外的 Schema Registry 组件

## 关联
- PRS: REQ-AURORA-PLAN-003, REQ-AURORA-PLAN-007
- CBB: CBB-KAFKA-CLIENT-v3
```

### 节点关联与展示

#### 在整体流程中的位置

```
ACT-03-03-01 产品需求规格化 → ACT-03-03-02 架构描述生成 → ACT-03-03-03 项目计划编制
                                    ↓
                            ArchitectureDescriptionCandidate
                                    ↓
                            ACT-03-03-04 CBB 复用决策
                            ACT-03-03-05 测试策略规划
```

#### 上游依赖
| 上游 Activity | 依赖内容 | 依赖方式 |
|---|---|---|
| ACT-03-03-01 产品需求规格化 | PRSBaselineCandidate | 作为架构设计的需求输入 |
| ACT-03-02-06 技术风险与 CBB 识别 | TechnicalRiskAndCBBMap | 作为技术约束和 CBB 候选 |

#### 下游消费
| 下游 Activity | 消费内容 | 消费方式 |
|---|---|---|
| ACT-03-03-03 项目计划编制 | ArchitectureDescriptionCandidate 中的容器和接口 | 作为 WBS 拆分和技术复杂度评估 |
| ACT-03-03-04 CBB 复用决策 | ArchitectureDescriptionCandidate 中的 CBB 使用点 | 作为 CBB 能力需求的来源 |
| ACT-03-03-05 测试策略规划 | ArchitectureDescriptionCandidate 中的接口和质量属性 | 作为集成测试和性能测试的依据 |

#### 可视化展示

**在 SPK-OS Dashboard 的展示方式**：
1. **C4 模型交互图**：可点击的 Context/Container 图，点击容器展示详细信息
2. **ADR 时间线**：按时间展示架构决策，每个 ADR 可展开查看详情
3. **质量属性覆盖矩阵**：热力图展示每个质量属性的场景覆盖度
4. **Critic 迭代面板**：展示 Generator-Critic 的迭代过程，每轮的 PASS/FAIL 状态
5. **接口依赖图**：可视化展示容器间的接口依赖关系，高亮循环依赖

---
