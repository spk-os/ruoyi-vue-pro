## ACT-03-05-07 用户场景验证

### 基础信息

| 字段 | 内容 |
|---|---|
| **Activity ID** | ACT-03-05-07 |
| **Activity 名称** | 用户场景验证 |
| **所属阶段** | 验证 |
| **Lead Agent 角色** | Verification Orchestrator |
| **执行位置** | task_system |
| **是否使用 Worker Agent** | ✓（VOC Agent 聚类反馈并标情绪） |
| **是否需要 Independent Verifier** | ✓（DCS 校验样本/同意/场景覆盖/区间） |
| **所需模型能力** | CROSS_SOURCE_REASONING + GROUNDED_EXTRACTION |
| **所需 Skill** | ipd-verify-user-validation / spk-voc-analyzer |
| **所需 Tool/MCP** | mcp-spk-os-artifact-query / mcp-filesystem-read |
| **所需 Knowledge** | KN-IPD-USER-VALIDATION-SCHEMA-050 / KN-STATISTICAL-SAMPLING-051 |
| **所需 Information** | INFO-BETA-PACK（Beta 包）/ INFO-USER-PERSONAS（用户画像）/ INFO-CONSENT-RECORDS（同意记录） |
| **可读取的 Memory 范围** | project:spk-os-ipd, agent_role:verification, capability:cross_source_reasoning |
| **人工责任** | Product/VOC Owner 判断代表性和接受度 |
| **失败处理** | 采集失败同参与者仅按同意重试；样本不足降级探索性结论；隐私/伤害风险升级 Research Ethics/Data Owner；回滚并删除超范围数据 |
| **对应 Flowable 节点** | task_user_validation (ipd-qualification.bpmn) |
| **证据来源** | RunReceipt + UserValidationReport + ArtifactManifest |

### 资源与工具详情

#### 工具调用顺序与效率策略

```
Step 1: 批准场景/招募方案
├─ Product Manager 确定用户画像分层：
│  ├─ SME 开发者（1-10 人团队）
│  ├─ 企业 DevOps（100+ 人团队）
│  ├─ 独立贡献者（个人开发者）
│  └─ 每层 ≥10 人
├─ 确定场景覆盖要求：
│  ├─ 关键用户旅程（必须覆盖）
│  ├─ 边缘场景（尽量覆盖）
│  └─ 异常场景（选择性覆盖）
├─ 确定招募方案和同意流程
└─ 输出：验证方案

Step 2: 收集同意
├─ 向 Beta 用户发送参与邀请
├─ 收集 NDA 和反馈协议签署
├─ 记录数据使用同意
├─ DCS 校验同意完整性
└─ 输出：同意记录

Step 3: 执行代表性任务
├─ 为每个画像设计代表性任务
├─ 用户在真实环境中执行任务
├─ 采集：
│  ├─ 任务完成率
│  ├─ 任务完成时间
│  ├─ 错误率
│  ├─ 用户满意度评分
│  └─ 定性反馈（原话）
└─ 输出：原始反馈数据

Step 4: VOC Agent 聚类反馈
├─ 调用 spk-voc-analyzer.analyze(raw_feedback)
│  ├─ 聚类反馈类型：
│  │  ├─ 正面反馈
│  │  ├─ 负面反馈
│  │  ├─ 功能请求
│  │  └─ Bug 报告
│  ├─ 标注情绪（正面/中性/负面）
│  ├─ 提取关键主题
│  ├─ 保留少数意见（不平均化）
│  └─ 输出：反馈分析报告

Step 5: DCS 统计计算
├─ 调用 DCS POST /api/v1/wilson-interval
│  ├─ 对每个场景计算 Wilson 95% 置信区间
│  ├─ 输入：successes, trials, confidence_level=0.95
│  ├─ 输出：lower, upper, point_estimate, signature
│  └─ 签名：sha256(...)
├─ 调用 DCS POST /api/v1/sample-representativeness
│  ├─ 检查样本分布是否覆盖所有关键画像
│  ├─ 检查每层样本量是否 ≥10
│  └─ 输出：代表性检查报告
└─ 输出：统计报告

Step 6: Product Manager 签署
├─ 审查反馈分析报告 + 统计报告
├─ 判断场景验证结论
├─ 标注代表性不足的场景
├─ **不把小样本外推为总体**
├─ 签署验证报告
└─ 输出：UserValidationReport（已签署）
```

### 输出 Artifact 模板

#### Artifact 名称
`UserValidationReport`

#### UserValidationReport Schema

```json
{
  "report_id": "uvr-20260905-001",
  "version": "1.0.0",
  "created_at": "2026-09-05T10:00:00Z",
  "signed_by": {
    "user_id": "user-product-001",
    "role": "Product Manager",
    "signed_at": "2026-09-05T14:00:00Z"
  },
  "beta_program": {
    "duration": "2026-08-29 to 2026-09-04",
    "total_participants": 50,
    "consent_records": 50,
    "nda_signed": 50
  },
  "personas": [
    {
      "persona": "SME 开发者",
      "target_count": 15,
      "actual_count": 18,
      "representative": true
    },
    {
      "persona": "企业 DevOps",
      "target_count": 15,
      "actual_count": 16,
      "representative": true
    },
    {
      "persona": "独立贡献者",
      "target_count": 15,
      "actual_count": 16,
      "representative": true
    }
  ],
  "scenarios": [
    {
      "scenario_id": "SC-001",
      "name": "代码审查工作流",
      "personas_covered": ["SME 开发者", "企业 DevOps", "独立贡献者"],
      "task_completion_rate": 0.92,
      "avg_completion_time_sec": 45,
      "satisfaction": {
        "point_estimate": 0.85,
        "wilson_95_lower": 0.72,
        "wilson_95_upper": 0.92
      },
      "feedback_summary": {
        "positive": 38,
        "negative": 5,
        "feature_requests": 7,
        "bug_reports": 2
      },
      "key_themes": [
        "审查速度快，结果准确",
        "希望支持自定义规则",
        "UI 可以更直观"
      ],
      "minority_opinions": [
        "对于大型 monorepo 性能不够理想（2 人反馈）"
      ],
      "limitations": "样本以 TypeScript 开发者为主，其他语言覆盖不足"
    }
  ],
  "overall_assessment": {
    "satisfaction_point": 0.85,
    "satisfaction_interval": "[0.72, 0.92]",
    "critical_issues": 0,
    "recommendation": "场景验证通过，但需关注大型 monorepo 性能问题"
  },
  "dcs_receipts": {
    "wilson_interval": {"solver_version": "wilson-interval-v1", "signature": "sha256:..."},
    "representativeness": {"solver_version": "sample-representativeness-v1", "signature": "sha256:..."}
  }
}
```

---
