## ACT-03-05-08 计算残余风险

### 基础信息

| 字段 | 内容 |
|---|---|
| **Activity ID** | ACT-03-05-08 |
| **Activity 名称** | 计算残余风险 |
| **所属阶段** | 验证 |
| **Lead Agent 角色** | Verification Orchestrator |
| **执行位置** | task_system |
| **是否使用 Worker Agent** | ✓（Risk Narrative Agent 汇总风险证据） |
| **是否需要 Independent Verifier** | ✓（DCS 计算 Wilson 区间/flaky/暴露） |
| **所需模型能力** | CROSS_SOURCE_REASONING + STRUCTURED_DRAFTING |
| **所需 Skill** | ipd-verify-residual-risk / spk-risk-calculator |
| **所需 Tool/MCP** | mcp-spk-os-artifact-query / mcp-filesystem-read |
| **所需 Knowledge** | KN-IPD-RISK-SCHEMA-052 / KN-RISK-QUANTIFICATION-053 |
| **所需 Information** | 所有验证阶段的证据 Artifact |
| **可读取的 Memory 范围** | project:spk-os-ipd, agent_role:verification, capability:cross_source_reasoning |
| **人工责任** | Risk Owner 评估接受/缓解/豁免 |
| **失败处理** | 计算失败同版本重试；不可用双人复算；高影响未知或过期豁免升级 Risk Board；否决时回滚豁免并恢复未接受状态 |
| **对应 Flowable 节点** | task_residual_risk (ipd-qualification.bpmn) |
| **证据来源** | RunReceipt + ResidualRiskReport + ArtifactManifest |

### 资源与工具详情

#### 工具调用顺序与效率策略

```
Step 1: 锁定全部证据
├─ 锁定 TestExecutionEvidence 版本
├─ 锁定 DefectClosureRecord 版本
├─ 锁定 UserValidationReport 版本
├─ 锁定环境差异报告
├─ 计算所有证据的整体 hash
└─ 输出：证据锁定清单

Step 2: 按风险事件聚合
├─ 从风险清单中提取所有已识别风险
├─ 对每个风险事件聚合相关证据：
│  ├─ 测试覆盖情况（是否测试了缓解措施）
│  ├─ 缺陷状态（相关缺陷是否已关闭）
│  ├─ 用户反馈（用户是否报告了相关问题）
│  ├─ 环境差异（测试环境与生产的差异是否影响风险）
│  └─ Flaky 测试（是否有 flaky 测试掩盖了相关问题）
└─ 输出：风险-证据映射

Step 3: DCS 计算风险区间
├─ 调用 DCS POST /api/v1/risk-calculate
│  ├─ 对每个风险事件计算：
│  │  ├─ 发生概率（基于测试数据和缺陷数据）
│  │  ├─ 影响程度（基于需求影响分析）
│  │  ├─ Wilson 置信区间
│  │  ├─ 控制有效性（缓解措施是否有效）
│  │  └─ 残余风险等级
│  ├─ 计算整体残余风险
│  ├─ 标注未知项（证据不足的风险）
│  ├─ 输出：风险计算报告 + 签名
│  └─ 签名：sha256(...)
├─ **数值均可重算**
└─ 输出：DCS 风险计算报告

Step 4: Risk Narrative Agent 汇总
├─ 调用 spk-risk-calculator.narrate(risk_evidence, dcs_results)
│  ├─ 生成风险叙事：
│  │  ├─ 每个风险的当前状态
│  │  ├─ 控制措施有效性评估
│  │  ├─ 未知项和不确定性
│  │  ├─ 豁免建议（如需要）
│  │  └─ 补偿控制措施建议
│  └─ 输出：风险叙事草稿
└─ 输出：残余风险报告草稿

Step 5: Risk Owner 评估与签署
├─ Risk Owner 审查风险报告
├─ 对每个风险做出决定：
│  ├─ 接受：风险在可接受范围内
│  ├─ 缓解：需要额外控制措施
│  ├─ 豁免：暂时接受，设定到期日和补偿控制
│  └─ 拒绝：风险不可接受，阻断发布
├─ 豁免必须含：
│  ├─ 残余风险描述
│  ├─ 到期日
│  ├─ 补偿控制措施
│  └─ 具名批准人
├─ **AI 不作接受决定**
├─ 签署风险报告
└─ 输出：ResidualRiskReport（已签署）
```

### 输出 Artifact 模板

#### Artifact 名称
`ResidualRiskReport`

#### ResidualRiskReport Schema

```json
{
  "report_id": "rrr-20260906-001",
  "version": "1.0.0",
  "created_at": "2026-09-06T10:00:00Z",
  "signed_by": {
    "user_id": "user-risk-owner-001",
    "role": "Risk Owner",
    "signed_at": "2026-09-06T14:00:00Z"
  },
  "evidence_refs": {
    "test_execution": "tee-20260830-001",
    "defect_closure": "dcr-20260830-001",
    "user_validation": "uvr-20260905-001",
    "environment_diff": "rte-20260829-001"
  },
  "risks": [
    {
      "risk_id": "R-001",
      "description": "大型 monorepo (>10K files) 审查延迟超标",
      "probability": {
        "point_estimate": 0.15,
        "wilson_95_lower": 0.08,
        "wilson_95_upper": 0.25
      },
      "impact": "medium",
      "residual_level": "medium",
      "control_effectiveness": "partial",
      "evidence": "2 名 Beta 用户反馈，Wilson 区间 [0.08, 0.25]",
      "decision": "waiver",
      "waiver": {
        "waiver_id": "W-001",
        "residual_risk": "大型 monorepo 场景 P95 延迟可能达到 800ms",
        "expires_at": "2026-12-31",
        "compensating_controls": [
          "在文档中标注大型 monorepo 的性能限制",
          "提供分批审查的 workaround",
          "在 v1.1 路线图中优先解决"
        ],
        "approved_by": "user-risk-owner-001"
      }
    },
    {
      "risk_id": "R-002",
      "description": "模型 API 延迟超 SLA 时的降级路径",
      "probability": {
        "point_estimate": 0.05,
        "wilson_95_lower": 0.02,
        "wilson_95_upper": 0.10
      },
      "impact": "low",
      "residual_level": "low",
      "control_effectiveness": "effective",
      "evidence": "降级路径已实现并通过测试",
      "decision": "accept"
    }
  ],
  "unknown_items": [
    {
      "item_id": "UNK-001",
      "description": "多语言支持完整性（仅 TypeScript 充分测试）",
      "reason": "Beta 样本以 TypeScript 开发者为主，其他语言覆盖不足",
      "recommendation": "在 v1.1 中补充 Python/Go/Java 的专项测试"
    }
  ],
  "overall_assessment": {
    "total_risks": 8,
    "accepted": 5,
    "waivers": 2,
    "rejected": 0,
    "mitigation_required": 1,
    "unknown_items": 1,
    "recommendation": "条件通过，需关注 W-001 和 W-002 的到期日"
  },
  "dcs_receipt": {
    "solver_version": "risk-calculate-v1",
    "input_hash": "sha256:...",
    "signature": "sha256:..."
  }
}
```

---
