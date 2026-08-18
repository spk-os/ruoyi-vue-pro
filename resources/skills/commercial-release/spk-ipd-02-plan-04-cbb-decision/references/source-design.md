## ACT-03-03-04 CBB 复用决策

### 基础信息

| 字段 | 内容 |
|---|---|
| **Activity ID** | ACT-03-03-04 |
| **Activity 名称** | CBB 复用决策 |
| **所属阶段** | 计划 |
| **Lead Agent 角色** | Planning Lead |
| **执行位置** | task_system |
| **是否使用 Worker Agent** | ✓（按 CBB 候选并行，N=候选数） |
| **是否需要 Independent Verifier** | — |
| **所需模型能力** | GROUNDED_EXTRACTION + CROSS_SOURCE_REASONING + SAFETY_REVIEW |
| **所需 Skill** | ipd-plan-cbb-decision / spk-cbb-contract-matcher / spk-cbb-scorer |
| **所需 Tool/MCP** | mcp-spk-os-cbb-query / mcp-spk-os-artifact-query / mcp-filesystem-read |
| **所需 Knowledge** | KN-IPD-CBB-CONTRACT-010 / KN-SEI-PRODUCT-LINE-011 |
| **所需 Information** | INFO-PRSBASELINE / INFO-ARCHITECTURE / INFO-CBB-CATALOG |
| **可读取的 Memory 范围** | project:spk-os-ipd, agent_role:planning, capability:grounded_extraction |
| **人工责任** | SE / TDT / Legal / Security 签署复用决策 |
| **失败处理** | Registry 查询失败重试只读镜像；数据陈旧人工核验；许可/关键安全冲突升级 Legal/Security；回滚复用链接并恢复原架构分支 |
| **对应 Flowable 节点** | task_cbb_decision (ipd-plan.bpmn) |
| **证据来源** | RunReceipt + ToolReceipt + ArtifactManifest |

### 资源与工具详情

#### 输入资源

| 资源 | 来源 | 获取方式 | 质量要求 |
|---|---|---|---|
| PRSBaselineCandidate | ACT-03-03-01 产物 | mcp-spk-os-artifact-query.get() | PM+SE 签署 |
| ArchitectureDescriptionCandidate | ACT-03-03-02 产物 | mcp-spk-os-artifact-query.get() | Chief Architect 签署 |
| CBB 目录 | 平台团队 Registry | mcp-spk-os-cbb-query.list_all() | 每个 CBB 含完整 7 字段契约 |
| CBB 使用历史 | AgentMemory | memory.query(scope="cbb_usage") | 各项目的使用经验和问题 |
| 许可合规规则 | Knowledge 库 | mcp-filesystem-read(/knowledge/legal/license_compatibility.yaml) | 许可兼容性矩阵 |
| 安全基线 | Knowledge 库 | mcp-filesystem-read(/knowledge/security/cbb_security_baseline.yaml) | CBB 安全证据最小集 |

#### 工具调用顺序与效率策略

```
Step 1: 提取 CBB 能力需求
├─ 从 PRSBaselineCandidate 中提取含 cbb_dependency 的需求
├─ 从 ArchitectureDescriptionCandidate 中提取 CBB 使用点
├─ 合并为 CBB 能力需求列表
└─ 输出：CBB 能力需求清单

Step 2: 并行启动 N 个 Worker Agent（N=CBB 候选数）
├─ 每个 Worker 负责一个 CBB 候选的契约匹配
├─ Worker-i: 分析 CBB-i
│  ├─ 调用 mcp-spk-os-cbb-query.get(cbb_id) 获取完整契约
│  ├─ 调用 spk-cbb-contract-matcher.match(requirement, cbb_contract)
│  │  ├─ 逐字段比较 7 字段契约：
│  │  │  ① 接口规格（协议/方法/数据格式）
│  │  │  ② 质量属性（性能/可靠性/安全）
│  │  │  ③ 版本兼容性
│  │  │  ④ 许可条件（类型/限制/兼容性）
│  │  │  ⑤ 适配成本（改造工作量/测试工作量）
│  │  │  ⑥ 安全证据（SBOM/漏洞扫描/渗透测试）
│  │  │  ⑦ Owner 联系方式（维护团队/SLA/退出策略）
│  │  ├─ 对每个字段给出：match / partial_match / mismatch / unknown
│  │  └─ 输出：契约匹配报告
│  ├─ 调用 spk-cbb-scorer.score(match_report)
│  │  ├─ 计算综合评分（加权：接口 30% + 质量 20% + 许可 20% + 安全 15% + 适配 15%）
│  │  └─ 输出：CBB 评分卡
│  └─ 输出：CBB-i 的完整评估报告

Step 3: Lead Agent 聚合分析
├─ 合并所有 Worker 的评估报告
├─ 为每个需求生成 CBB 决策草稿
│  ├─ 选项：复用(直接使用) / 改造(修改后使用) / 替换(另选 CBB) / 自研
│  ├─ 依据：适配成本、时间影响、质量风险、维护成本、退出难度
│  └─ 全生命周期成本比较（复用 vs 改造 vs 自研）
└─ 输出：CBB 决策草稿

Step 4: SE / TDT / Legal / Security 人工审核
├─ SE 审核：技术适配性判断
├─ TDT 审核：开发团队能力匹配度
├─ Legal 审核：许可合规性（特别是开源组件的传染性许可）
├─ Security 审核：安全证据充分性（SBOM 完整性、已知漏洞）
├─ 四方共同决定：复用/改造/替换/自研
├─ 签署 CBB 决策记录
└─ 输出：CBBReuseDecision（已签署）
```

#### 效率优化策略

| 策略 | 说明 | 预期效果 |
|---|---|---|
| 契约匹配而非语义搜索 | 7 字段确定性比较，不依赖向量相似度 | 消除语义搜索的误匹配 |
| 并行 Worker | N 个 CBB 候选并行评估 | 比串行快 N 倍 |
| 全生命周期成本比较 | 不仅看初始适配成本，还看维护、升级、退出成本 | 避免短期节省长期负债 |
| 许可兼容性矩阵 | 确定性检查许可兼容性 | 避免法律风险 |

### 输出 Artifact 模板

#### Artifact 名称
`CBBReuseDecision`

#### 文件结构
```
cbb_reuse_decision/
├── manifest.yaml
├── decision.json                 # 主数据文件
├── evaluations/                  # 每个 CBB 的评估报告
│   ├── cbb-api-gateway-v2.json
│   ├── cbb-kafka-client-v3.json
│   └── ...
├── contract_matches/             # 契约匹配详情
│   └── match_reports.json
└── README.md
```

#### decision.json Schema

```json
{
  "decision_id": "cbb-dec-20260817-001",
  "version": "1.0.0",
  "created_at": "2026-08-17T20:00:00Z",
  "created_by": "agent-planning-001",
  "signed_by": [
    {"user_id": "user-se-001", "role": "System Engineer", "signed_at": "2026-08-17T23:00:00Z"},
    {"user_id": "user-tdt-001", "role": "TDT Lead", "signed_at": "2026-08-17T23:15:00Z"},
    {"user_id": "user-legal-001", "role": "Legal", "signed_at": "2026-08-17T23:30:00Z"},
    {"user_id": "user-security-001", "role": "Security", "signed_at": "2026-08-17T23:45:00Z"}
  ],
  "decisions": [
    {
      "requirement_id": "REQ-AURORA-PLAN-001",
      "cbb_id": "CBB-API-GATEWAY-v2",
      "decision": "reuse",
      "contract_match": {
        "interface": "match",
        "quality_attributes": "match",
        "version": "match",
        "license": "match",
        "adaptation_cost": "low",
        "security_evidence": "match",
        "owner": "match"
      },
      "score": 0.92,
      "risk_assessment": {
        "technical": "low - 接口兼容，已在 3 个项目中验证",
        "schedule": "low - 可直接使用，无需改造",
        "maintenance": "medium - 需要关注版本升级兼容性"
      },
      "rationale": "CBB-API-GATEWAY-v2 提供完整的 REST API 网关功能，接口规格与需求匹配，性能指标满足要求。已在 Aurora 的姊妹项目 Beta 和 Gamma 中稳定运行 6 个月。",
      "owner": "李四（平台团队）",
      "exit_strategy": "如 CBB 停止维护，可在 2 周内迁移到 Spring Cloud Gateway",
      "review_checkpoint": "2027-01-15（集成测试阶段复查）"
    },
    {
      "requirement_id": "REQ-AURORA-PLAN-005",
      "cbb_id": "CBB-ML-PIPELINE-v1",
      "decision": "replace",
      "contract_match": {
        "interface": "partial_match",
        "quality_attributes": "mismatch",
        "version": "match",
        "license": "match",
        "adaptation_cost": "high",
        "security_evidence": "match",
        "owner": "match"
      },
      "score": 0.45,
      "risk_assessment": {
        "technical": "high - 不支持多模型并行，改造工作量大",
        "schedule": "high - 改造预计 15 人天，超过自研成本",
        "maintenance": "high - 改造后与上游版本分叉"
      },
      "rationale": "CBB-ML-PIPELINE-v1 不支持多模型并行审查管道，改造成本（15 人天）接近自研（20 人天），且改造后与上游版本分叉，维护成本高。决定自研替代。",
      "alternative": "自研 ML Pipeline，基于 Ray 框架",
      "owner": "张三（AI 团队）",
      "exit_strategy": "N/A（自研）",
      "review_checkpoint": "2026-11-01（开发阶段中期检查）"
    }
  ],
  "summary": {
    "total_decisions": 6,
    "reuse_count": 4,
    "adapt_count": 1,
    "replace_count": 1,
    "self_develop_count": 0
  }
}
```

### 节点关联与展示

#### 在整体流程中的位置

```
ACT-03-03-01 + ACT-03-03-02 → ACT-03-03-04 CBB 复用决策 → ACT-03-03-06 TR2/PDCP 决策包组装
                                    ↓
                            CBBReuseDecision
```

#### 上游依赖
| 上游 Activity | 依赖内容 | 依赖方式 |
|---|---|---|
| ACT-03-03-01 产品需求规格化 | PRSBaselineCandidate 中的 cbb_dependency 字段 | 作为 CBB 能力需求 |
| ACT-03-03-02 架构描述生成 | ArchitectureDescriptionCandidate 中的 CBB 使用点 | 作为 CBB 集成需求 |

#### 下游消费
| 下游 Activity | 消费内容 | 消费方式 |
|---|---|---|
| ACT-03-03-06 TR2/PDCP 决策包组装 | CBBReuseDecision | 作为技术证据的一部分 |
| 开发阶段 | CBBReuseDecision 中的复用/改造/自研决策 | 指导开发实施 |

#### 可视化展示

**在 SPK-OS Dashboard 的展示方式**：
1. **CBB 决策矩阵**：按需求展示 CBB 决策（复用/改造/替换/自研），颜色编码
2. **契约匹配雷达图**：每个 CBB 的 7 字段匹配度雷达图
3. **全生命周期成本对比**：柱状图对比复用/改造/自研的全生命周期成本
4. **CBB 依赖图**：可视化展示系统对 CBB 的依赖关系

---
