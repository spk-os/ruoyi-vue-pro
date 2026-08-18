## ACT-03-03-01 产品需求规格化

### 基础信息

| 字段 | 内容 |
|---|---|
| **Activity ID** | ACT-03-03-01 |
| **Activity 名称** | 产品需求规格化 |
| **所属阶段** | 计划 |
| **Lead Agent 角色** | Planning Lead |
| **执行位置** | task_system |
| **是否使用 Worker Agent** | ✓（2 个并行：功能需求拆解 / 非功能需求拆解） |
| **是否需要 Independent Verifier** | — |
| **所需模型能力** | GROUNDED_EXTRACTION + STRUCTURED_DRAFTING |
| **所需 Skill** | ipd-plan-requirements-spec / spk-requirements-atomizer / spk-fuzzy-word-scanner |
| **所需 Tool/MCP** | mcp-spk-os-artifact-query / mcp-filesystem-read / mcp-spk-os-requirements-query |
| **所需 Knowledge** | KN-IPD-PRS-SCHEMA-001 / KN-IPD-FUZZY-WORDS-002 / KN-ISO-29148-003 |
| **所需 Information** | INFO-CDCP-OUTPUT（概念阶段 CDCP 决策记录）/ INFO-MRS（市场需求说明）/ INFO-APPEALS（$APPEALS 分析结果） |
| **可读取的 Memory 范围** | project:spk-os-ipd, agent_role:planning, capability:grounded_extraction |
| **人工责任** | Product Manager / SE 签署需求语义和优先级 |
| **失败处理** | 抽取失败按同快照重试一次；仍失败则人工拆解；来源冲突升级 Product Owner；回滚到 CDCP need 与上版 PRS，不保留未签名基线 |
| **对应 Flowable 节点** | task_requirements_spec (ipd-plan.bpmn) |
| **证据来源** | RunReceipt + ToolReceipt + ArtifactManifest |

### 资源与工具详情

#### 输入资源

| 资源 | 来源 | 获取方式 | 质量要求 |
|---|---|---|---|
| CDCP 决策记录 | 概念阶段 ACT-03-02-08 产物 | mcp-spk-os-artifact-query.get(artifact_type="TR1DecisionRecord") | 必须含 IPMT 签署的 Go/Conditional Go 决定 |
| 概念假设包 | 概念阶段 ACT-03-02-04 产物 | mcp-spk-os-artifact-query.get(artifact_type="ConceptOptionSet") | 必须含 PDT Lead 签署 |
| MRS 市场需求说明 | MM 流程输出 | mcp-filesystem-read(/knowledge/market/mrs/) | 含市场细分、客户痛点、市场规模区间 |
| $APPEALS 分析结果 | 需求管理团队 | mcp-spk-os-requirements-query.get_appeals() | 至少 3 个竞品的 8 维度评分 |
| 模糊词词库 | Knowledge 库 | mcp-filesystem-read(/knowledge/ipd/methods/fuzzy_words.yaml) | 含中英文模糊词 + 量化替代建议 |
| ISO 29148 需求属性框架 | Knowledge 库 | mcp-filesystem-read(/knowledge/standards/iso-29148-attributes.yaml) | 需求属性最小集定义 |
| 历史需求库 | 需求管理系统 | mcp-spk-os-requirements-query.list(project=*) | 用于去重检测 |

#### 工具调用顺序与效率策略

```
Step 1: 输入准入校验
├─ 调用 mcp-spk-os-artifact-query.get(TR1DecisionRecord) 获取 CDCP 决策
├─ 校验 CDCP 决定为 Go 或 Conditional Go（否则阻断）
├─ 调用 mcp-spk-os-artifact-query.get(ConceptOptionSet) 获取概念包
├─ 校验所有输入 Artifact 的签名和 hash
└─ 输出：Context Manifest（输入清单 + hash + 质量等级）

Step 2: 并行启动 2 个 Worker Agent
├─ Worker-1: 功能需求拆解
│  ├─ 读取 CDCP 输出包中的功能需求部分
│  ├─ 读取 MRS 中的功能需求
│  ├─ 读取 $APPEALS 中的功能维度差距
│  ├─ 调用 ipd-plan-requirements-spec.atomize(sources, type="functional")
│  │  ├─ LLM 将高层需求拆解为原子化 PRS 条目
│  │  ├─ 每条含：标题(≤120字符) + 描述 + 验收准则(≥20字符) + 优先级(must/should/could/wont)
│  │  └─ 标注每条需求的概念来源（引用具体段落 locator）
│  └─ 输出：功能需求 PRS 草稿（JSON 数组）
│
└─ Worker-2: 非功能需求拆解
   ├─ 读取 CDCP 输出包中的质量属性部分
   ├─ 读取概念阶段的技术风险和技术约束
   ├─ 调用 ipd-plan-requirements-spec.atomize(sources, type="non_functional")
   │  ├─ 性能需求：响应时间、吞吐量、并发数
   │  ├─ 安全需求：认证、授权、加密、审计
   │  ├─ 可靠性需求：可用性、容灾、恢复时间
   │  └─ 可维护性需求：可观测性、日志、升级策略
   └─ 输出：非功能需求 PRS 草稿（JSON 数组）

Step 3: Lead Agent 聚合与质量检查
├─ 合并两个 Worker 的 PRS 草稿
├─ 调用 spk-fuzzy-word-scanner.scan(prs_draft)
│  ├─ 确定性词库匹配（不依赖 LLM 判断）
│  ├─ 词库：["快速", "易用", "大量", "适当", "合理", "及时", "高效", "灵活", ...]
│  ├─ 对每条含模糊词的需求，生成量化替代建议
│  └─ 输出：模糊词报告 + 建议替代
├─ 调用 spk-requirements-atomizer.schema_validate(prs_draft)
│  ├─ 校验每条 PRS 的必需字段：id/title/source/priority/acceptance_criteria/owner/version
│  ├─ 校验 ID 唯一性（UUID 格式）
│  └─ 输出：Schema 校验报告
├─ 调用 spk-requirements-atomizer.dedup(prs_draft, existing_requirements)
│  ├─ 基于标题+描述向量相似度 >0.92 且来源相同
│  └─ 输出：去重报告
├─ 调用 spk-requirements-atomizer.build_rtm_seed(prs_draft, concept_sources)
│  ├─ 构建 source→requirement 的初始追溯矩阵
│  └─ 输出：RTM 种子矩阵

Step 4: Product Manager / SE 人工审核
├─ 在 yudao 审批工作台查看 PRS 草稿
├─ 审核每条需求的来源忠实度（是否真实反映概念文档意图）
├─ 处理模糊词建议（接受/修改/拒绝）
├─ 确认优先级排序（must/should/could/wont 分布是否合理）
├─ 处理去重建议（合并/保留/拆分）
├─ 签署需求基线
└─ 输出：PRSBaselineCandidate（已签署）

Step 5: yudao 状态更新
├─ 更新 PRS 状态为 approved
├─ 记录签署时间戳和身份
├─ 冻结 PRS 候选版本
└─ 初始化 source→requirement→verification RTM
```

#### 效率优化策略

| 策略 | 说明 | 预期效果 |
|---|---|---|
| 功能/非功能并行拆解 | 2 个 Worker 并行处理不同类型需求 | 比串行快 2 倍 |
| 确定性模糊词扫描 | 词库匹配而非 LLM 判断，<1s 完成 | 消除 LLM 幻觉风险 |
| 增量去重 | 只与已有需求库比较，不重新扫描全部 | 减少 80% 计算量 |
| RTM 种子预构建 | 在需求拆解时同步建立追溯关系 | 避免后续补建追溯的遗漏 |

### 输出 Artifact 模板

#### Artifact 名称
`PRSBaselineCandidate`

#### 存储位置
`/work/SPK-OS/artifacts/prs_baseline_candidate/{artifact_id}/{version}/`

#### 文件结构
```
prs_baseline_candidate/
├── manifest.yaml                 # ArtifactManifest（见 07 核心数据合同）
├── prs_baseline.json             # 主数据文件
├── sources/                      # 输入来源快照
│   ├── cdcp_decision.json
│   ├── concept_option_set.json
│   ├── mrs.json
│   └── appeals_analysis.json
├── quality_reports/              # 质量检查报告
│   ├── fuzzy_word_report.json
│   ├── schema_validation.json
│   ├── dedup_report.json
│   └── rtm_seed.json
├── requirements/                 # 每条需求的详细文件
│   ├── REQ-001.json
│   ├── REQ-002.json
│   └── ...
└── README.md                     # 人类可读摘要
```

#### prs_baseline.json Schema

```json
{
  "baseline_id": "prs-20260817-001",
  "version": "1.0.0",
  "as_of": "2026-08-17",
  "created_at": "2026-08-17T10:00:00Z",
  "created_by": "agent-planning-001",
  "signed_by": [
    {
      "user_id": "user-pm-001",
      "role": "Product Manager",
      "signed_at": "2026-08-17T14:00:00Z",
      "decision": "approved"
    },
    {
      "user_id": "user-se-001",
      "role": "System Engineer",
      "signed_at": "2026-08-17T14:30:00Z",
      "decision": "approved"
    }
  ],
  "input_artifacts": {
    "cdcp_decision_ref": "tdr-20260815-001",
    "concept_option_set_ref": "cos-20260815-001",
    "mrs_ref": "mrs-20260810-001",
    "appeals_ref": "appeals-20260812-001"
  },
  "statistics": {
    "total_requirements": 45,
    "functional_count": 32,
    "non_functional_count": 13,
    "priority_distribution": {
      "must": 18,
      "should": 15,
      "could": 10,
      "wont": 2
    },
    "risk_distribution": {
      "high": 12,
      "medium": 20,
      "low": 13
    },
    "fuzzy_words_found": 5,
    "fuzzy_words_resolved": 5,
    "duplicates_merged": 2,
    "rtm_coverage": {
      "total_sources": 28,
      "linked_sources": 28,
      "coverage_pct": 100.0
    }
  },
  "requirements": [
    {
      "id": "REQ-AURORA-PLAN-001",
      "title": "代码审查结果可通过 REST API 获取",
      "description": "系统应提供 RESTful API 端点，允许外部系统查询代码审查结果，支持按项目、分支、提交和文件路径过滤。",
      "type": "functional",
      "source": "概念文档 §3.2 功能需求-FR03",
      "source_locator": "concept_option_set/options/option-a.md:L45-L52",
      "priority": "must",
      "acceptance_criteria": "1. GET /api/v1/reviews 返回200状态码和JSON格式的审查列表\n2. 支持 project, branch, commit, file_path 四个查询参数\n3. 响应时间 P95 < 500ms\n4. 支持分页，每页默认20条，最大100条",
      "owner": "张三（后端负责人）",
      "version": "1.0",
      "risk_level": "medium",
      "cbb_dependency": "CBB-API-GATEWAY-v2",
      "traceability": {
        "concept_ref": "CP-001-FR03",
        "architecture_ref": null,
        "test_ref": null
      },
      "fuzzy_word_check": {
        "passed": true,
        "original_text": null,
        "suggestion": null
      }
    }
  ],
  "rtm_seed": {
    "type": "source_to_requirement",
    "links": [
      {
        "source_id": "CP-001-FR03",
        "source_type": "concept_option",
        "requirement_ids": ["REQ-AURORA-PLAN-001"],
        "link_confidence": 0.95
      }
    ]
  }
}
```

#### README.md 模板

```markdown
# 产品需求规格基线 PRS-20260817-001

## 概览
- **基线时间**：2026-08-17
- **需求总数**：45 条
- **功能需求**：32 条 | **非功能需求**：13 条
- **优先级分布**：Must 18 / Should 15 / Could 10 / Won't 2

## 质量检查
- **模糊词检测**：发现 5 条，全部已量化替代
  - REQ-012: "快速响应" → "P95 响应时间 < 30s"
  - REQ-023: "易于使用" → "新用户 30 分钟内完成首次配置"
- **Schema 校验**：45/45 通过（100%）
- **去重检测**：合并 2 条重复需求
- **追溯覆盖**：28/28 来源已关联（100%）

## 高风险需求（12 条）
| ID | 标题 | 风险原因 |
|---|---|---|
| REQ-001 | 代码审查结果 REST API | 性能要求严格 |
| REQ-005 | 多模型审查管道 | 技术复杂度高 |
| ... | ... | ... |

## 签署
- **Product Manager**：张三，2026-08-17 14:00，approved
- **System Engineer**：李四，2026-08-17 14:30，approved
```

#### 验收标准

| 检查项 | 标准 | 验证方式 |
|---|---|---|
| Schema 完整性 | 每条 PRS 含全部必需字段 | DCS 自动校验 |
| 模糊词清零 | 无未处置的模糊词 | DCS 词库扫描 |
| 来源追溯 | 每条需求有 source + source_locator | DCS 追溯检查 |
| ID 唯一性 | 无重复 ID | DCS UUID 校验 |
| 双向追溯 | source→requirement 和 requirement→source 均完整 | DCS RTM 检查 |
| 签署完整 | PM + SE 均已签署 approved | yudao 审计日志 |

### 节点关联与展示

#### 在整体流程中的位置

```
[概念阶段 TR1DecisionRecord] → ACT-03-03-01 产品需求规格化 → ACT-03-03-02 架构描述生成
                                        ↓
                                PRSBaselineCandidate
                                        ↓
                                ACT-03-03-03 项目计划编制
                                ACT-03-03-04 CBB 复用决策
                                ACT-03-03-05 测试策略规划
```

#### 上游依赖
| 上游 Activity | 依赖内容 | 依赖方式 |
|---|---|---|
| ACT-03-02-08 TR1 评审支持 | TR1DecisionRecord（CDCP 决策记录） | 作为输入准入条件 |
| ACT-03-02-04 概念与价值主张生成 | ConceptOptionSet（概念假设包） | 作为需求来源 |
| MM 流程 | MRS + $APPEALS | 作为市场需求输入 |

#### 下游消费
| 下游 Activity | 消费内容 | 消费方式 |
|---|---|---|
| ACT-03-03-02 架构描述生成 | PRSBaselineCandidate 中的功能/非功能需求 | 作为架构设计的输入 |
| ACT-03-03-03 项目计划编制 | PRSBaselineCandidate 中的需求数量和复杂度 | 作为 WBS 拆分和工期估算的依据 |
| ACT-03-03-04 CBB 复用决策 | PRSBaselineCandidate 中的功能需求 | 作为 CBB 能力需求的来源 |
| ACT-03-03-05 测试策略规划 | PRSBaselineCandidate 中的需求和风险等级 | 作为 RTM 的需求端 |

#### 可视化展示

**在 SPK-OS Dashboard 的展示方式**：
1. **需求分布热力图**：按优先级（must/should/could/wont）和风险等级（high/medium/low）展示需求分布
2. **追溯矩阵视图**：可视化展示 source→requirement 的关联关系，高亮未覆盖的来源
3. **模糊词处理面板**：展示发现的模糊词和量化替代建议，支持一键接受/修改
4. **需求详情面板**：点击任意需求，展示完整的 PRS 条目 + 来源证据链

**在 yudao 审批工作台的展示方式**：
1. PM/SE 看到待审批任务："ACT-03-03-01 产品需求规格化 - 等待签署"
2. 点击进入，看到 README.md 摘要 + 质量检查报告 + 高风险需求列表
3. 可逐条审核需求，处理模糊词建议和去重建议
4. 操作按钮：[Approve] [Reject] [Request Revision]

---
