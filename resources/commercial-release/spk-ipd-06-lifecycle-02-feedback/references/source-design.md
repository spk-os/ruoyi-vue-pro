## ACT-03-07-02 Feedback Clustering & Analysis

### 基础信息

| 字段 | 内容 |
|---|---|
| **Activity ID** | ACT-03-07-02 |
| **Activity 名称** | Feedback Clustering & Analysis（反馈聚类与分析） |
| **所属阶段** | 生命周期管理 |
| **Lead Agent 角色** | Lifecycle Orchestrator |
| **执行位置** | task_system |
| **是否使用 Worker Agent** | ✓（Feedback Analyst 语义聚类 + Faithfulness Checker 验证忠实度） |
| **是否需要 Independent Verifier** | ✓（DCS 计算稳定度/样本分布/引用覆盖 + Product/VOC Owner 判定主题） |
| **所需模型能力** | CROSS_SOURCE_REASONING + MULTILINGUAL_ANALYSIS + STRUCTURED_DRAFTING |
| **所需 Skill** | ipd-lifecycle-feedback / spk-feedback-analyst / spk-faithfulness-checker |
| **所需 Tool/MCP** | mcp-spk-os-artifact-query / mcp-crm-query |
| **所需 Knowledge** | KN-IPD-FEEDBACK-SCHEMA-071 / KN-NLP-CLUSTERING-072 |
| **所需 Information** | INFO-LIFECYCLE-SIGNALS（feedback 类信号） |
| **可读取的 Memory 范围** | project:spk-os-ipd, agent_role:lifecycle, capability:cross_source_reasoning |
| **人工责任** | Product Manager 审核聚类结果、确认优先级、处理误合并申诉 |
| **失败处理** | 簇不稳定更换 seed/阈值重试；仍不稳定降级人工主题化；隐私/偏见异常升级 VOC/Data Owner；回滚到旧主题版本 |
| **对应 Flowable 节点** | task_feedback_clustering (ipd-lifecycle.bpmn) |
| **证据来源** | RunReceipt + FeedbackThemeSet + ArtifactManifest |

### 资源与工具详情

#### 工具调用顺序与效率策略

```
Step 1: 锁定信号快照
├─ 从 LifecycleSignalSet 中提取 feedback 类信号
├─ 锁定本次聚类的信号范围（时间窗 + 版本 + cohort）
├─ 计算信号集合的 hash
└─ 输出：锁定信号快照

Step 2: 脱敏处理
├─ 对客户原文进行脱敏：
│  ├─ 替换 PII（姓名、邮箱、电话）为匿名标识
│  ├─ 保留产品/版本/功能相关的关键信息
│  ├─ 保留情感表达和严重程度
│  └─ 记录脱敏规则 hash
└─ 输出：脱敏信号集合

Step 3: Feedback Analyst 语义聚类
├─ 调用 spk-feedback-analyst.cluster(signals, config)
│  ├─ 使用 embedding 模型将每条反馈转为向量
│  ├─ 使用 HDBSCAN 聚类算法进行聚类
│  │  ├─ 自动确定簇数量（不需要预设 k）
│  │  ├─ 识别噪声点（不属于任何簇的反馈）
│  │  └─ 支持不同密度的簇
│  ├─ 对每个簇：
│  │  ├─ 提取主题关键词（TF-IDF + LLM 摘要）
│  │  ├─ 选择代表性原文引用（≥3 条）
│  │  ├─ 选择反例/少数派意见（≥1 条）
│  │  ├─ 计算情感分布（正面/中性/负面比例）
│  │  ├─ 按客户群和版本分层统计
│  │  ├─ 计算簇稳定度（与上次聚类对比）
│  │  └─ 标注置信度（高/中/低）
│  └─ 输出：聚类结果草稿
└─ 输出：聚类结果草稿

Step 4: Faithfulness Checker 验证忠实度
├─ 调用 spk-faithfulness-checker.check(clusters, original_signals)
│  ├─ 对每个簇验证：
│  │  □ 每个摘要可追溯到原始信号（引用比对）
│  │  □ 代表性引用确实来自该簇（不是捏造）
│  │  □ 反例/少数派意见被保留（不被平均化）
│  │  □ 情感分析与原文一致（不翻转）
│  │  □ 客户群分层正确（不混淆）
│  ├─ 计算忠实度分数（引用可比对原文的比例）
│  ├─ 忠实度 < 0.90 → 阻断需求回流
│  └─ 输出：忠实度报告
└─ 输出：FaithfulnessReport

Step 5: DCS 统计计算
├─ 调用 DCS POST /api/v1/cluster-statistics
│  ├─ 计算：
│  │  □ 每个簇的大小和增长率
│  │  □ 簇间重叠率
│  │  □ 噪声点比例
│  │  □ 版本分布（每个簇中各版本的比例）
│  │  □ cohort 分布（每个簇中各客户群的比例）
│  │  □ 稳定度（与上次聚类的 Jaccard 相似度）
│  ├─ 输出：统计报告 + 签名
│  └─ 签名：sha256(...)
└─ 输出：DCS 统计报告

Step 6: Product/VOC Owner 签署
├─ Product Manager 审查：
│  ├─ 聚类结果（主题、代表引用、反例）
│  ├─ 忠实度报告
│  ├─ DCS 统计报告
│  ├─ 确认主题合理性
│  ├─ 调整 AI 偏差（如有）
│  ├─ 处理误合并申诉（拆分不合理的合并）
│  └─ 签署主题集合
└─ 输出：FeedbackThemeSet（已签署）
```

### 输出 Artifact 模板

#### Artifact 名称
`FeedbackThemeSet`

#### FeedbackThemeSet Schema

```json
{
  "set_id": "fts-20260915-001",
  "version": "1.0.0",
  "created_at": "2026-09-15T09:00:00Z",
  "created_by": "agent-feedback-analyst-001",
  "signed_by": {
    "user_id": "user-product-001",
    "role": "Product Manager",
    "signed_at": "2026-09-15T11:00:00Z"
  },
  "signal_snapshot_ref": "lss-20260915-001",
  "signal_count": 42,
  "faithfulness_score": 0.96,
  "themes": [
    {
      "theme_id": "TH-001",
      "name": "大仓库性能问题",
      "keywords": ["large repo", "slow", "timeout", "indexing", "performance"],
      "signal_count": 12,
      "representative_quotes": [
        {
          "signal_id": "SIG-20260915-003",
          "text": "Our monorepo has 15K files and the indexing takes over 5 minutes...",
          "customer_cohort": "enterprise-devops",
          "version": "2.0.0"
        },
        {
          "signal_id": "SIG-20260915-007",
          "text": "Review times are acceptable for small PRs but timeout on large ones",
          "customer_cohort": "enterprise-devops",
          "version": "2.0.0"
        }
      ],
      "minority_opinions": [
        {
          "signal_id": "SIG-20260915-015",
          "text": "Performance is fine for our 8K file repo, maybe it's a network issue",
          "customer_cohort": "sme-developer",
          "version": "2.0.0"
        }
      ],
      "sentiment": {
        "positive_pct": 0.08,
        "neutral_pct": 0.25,
        "negative_pct": 0.67
      },
      "cohort_distribution": {
        "enterprise-devops": 8,
        "sme-developer": 3,
        "independent-contributor": 1
      },
      "version_distribution": {
        "2.0.0": 10,
        "2.0.1": 2
      },
      "stability": {
        "jaccard_similarity_with_previous": 0.85,
        "trend": "growing",
        "previous_count": 8
      },
      "confidence": "high",
      "priority_candidate": "high"
    },
    {
      "theme_id": "TH-002",
      "name": "多语言支持需求",
      "keywords": ["Go", "Rust", "Python", "language support", "multi-language"],
      "signal_count": 8,
      "representative_quotes": [
        {
          "signal_id": "SIG-20260915-020",
          "text": "We'd love to see Go support, most of our backend is in Go",
          "customer_cohort": "enterprise-devops",
          "version": "2.0.0"
        }
      ],
      "minority_opinions": [],
      "sentiment": {
        "positive_pct": 0.75,
        "neutral_pct": 0.25,
        "negative_pct": 0.0
      },
      "confidence": "high",
      "priority_candidate": "medium"
    }
  ],
  "noise_signals": 3,
  "dcs_receipt": {
    "solver_version": "cluster-statistics-v1",
    "input_hash": "sha256:...",
    "signature": "sha256:..."
  }
}
```

---
