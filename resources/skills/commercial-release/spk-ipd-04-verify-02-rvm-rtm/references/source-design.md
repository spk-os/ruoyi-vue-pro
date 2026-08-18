## ACT-03-05-02 建立 RVM/RTM

### 基础信息

| 字段 | 内容 |
|---|---|
| **Activity ID** | ACT-03-05-02 |
| **Activity 名称** | 建立 RVM/RTM |
| **所属阶段** | 验证 |
| **Lead Agent 角色** | Verification Orchestrator |
| **执行位置** | task_system |
| **是否使用 Worker Agent** | ✓（Trace Agent 建议追溯链接） |
| **是否需要 Independent Verifier** | ✓（DCS 校验双向覆盖 + SE 审批 Oracle） |
| **所需模型能力** | CROSS_SOURCE_REASONING + STRUCTURED_DRAFTING |
| **所需 Skill** | ipd-verify-rvm-rtm / spk-traceability-builder |
| **所需 Tool/MCP** | mcp-spk-os-artifact-query / mcp-codegraph-explore / mcp-filesystem-read |
| **所需 Knowledge** | KN-IPD-RVM-SCHEMA-039 / KN-NASA-RVM-TEMPLATE-040 / KN-ISO-29119-TEST-TECH-041 |
| **所需 Information** | INFO-VERIFY-BASELINE（验证基线）/ INFO-PRS-BASELINE（需求基线）/ INFO-ARCHITECTURE（架构文档） |
| **可读取的 Memory 范围** | project:spk-os-ipd, agent_role:verification, capability:cross_source_reasoning |
| **人工责任** | SE 审批验证方法和 Oracle owner；Test Architect 审批测试策略 |
| **失败处理** | 自动匹配低置信时重试限定词；仍低则人工映射；关键需求无 Oracle 升级 QA；回滚错误链接与矩阵版本 |
| **对应 Flowable 节点** | task_rvm_review (ipd-qualification.bpmn) |
| **证据来源** | RunReceipt + ArtifactManifest + HumanApprovalRecord |

### 资源与工具详情

#### 输入资源

| 资源 | 来源 | 获取方式 | 质量要求 |
|---|---|---|---|
| VerificationBaselineManifest | ACT-03-05-01 产物 | mcp-spk-os-artifact-query.get() | QA+CM 签署 |
| PRSBaselineCandidate | 计划阶段产物（通过基线引用） | mcp-spk-os-artifact-query.get() | 含 45 条需求 + shall 语句 |
| ArchitectureDescription | 计划阶段产物（通过基线引用） | mcp-spk-os-artifact-query.get() | 含组件/接口/ADR |
| 威胁模型 | 安全团队 | mcp-filesystem-read(/security/threat-model/) | 覆盖 OWASP Top 10 |
| 风险清单 | 项目管理 | mcp-spk-os-artifact-query.get(artifact_type="RiskRegister") | 含残余风险 |
| 历史缺陷模式 | AgentMemory | memory.query(scope="defect_pattern") | 含根因分类 |

#### 工具调用顺序与效率策略

```
Step 1: 装载需求/架构/风险
├─ 从验证基线中提取需求基线、架构文档、风险清单
├─ 解析每条需求的 shall 语句和验收标准
├─ 标注需求优先级（high/medium/low）
├─ 加载威胁模型和历史缺陷模式
└─ 输出：验证 Context Pack

Step 2: Trace Agent 建议验证方法和 Oracle
├─ 调用 spk-traceability-builder.suggest_rvm(requirements, architecture, risks)
│  ├─ 区分 Verification（按规定正确实现）和 Validation（实现正确产品）
│  ├─ 为每条需求建议验证方法：
│  │  ├─ test：动态测试（适用于功能需求、性能需求）
│  │  ├─ analysis：静态分析（适用于架构需求、安全需求）
│  │  ├─ inspection：检查/审查（适用于文档需求、合规需求）
│  │  └─ demonstration：演示（适用于用户体验需求）
│  ├─ 关键需求（priority=high）建议至少两种互补方法
│  ├─ 为每条需求建议 Oracle 来源：
│  │  ├─ deterministic_rule：确定性规则（数值阈值、状态机规则）
│  │  ├─ human_expert：人类专家判定
│  │  └─ reference_implementation：参考实现
│  │  **禁止 ai_generated 作为 Oracle 来源**
│  ├─ 为每条需求标注验证环境
│  ├─ 为每条需求分配 owner
│  └─ 输出：RVM 草稿
├─ 建立 RTM 双向追溯链接：
│  ├─ 需求 → 设计 → 代码 → 测试（正向追溯）
│  ├─ 测试 → 代码 → 设计 → 需求（反向追溯）
│  └─ 输出：RTM 草稿

Step 3: DCS 覆盖检查（确定性）
├─ 调用 DCS POST /api/v1/rvm-rtm-validate
│  ├─ 输入：RVM 草稿 + RTM 草稿 + 需求全集
│  ├─ 检查：
│  │  □ 每条需求至少有一个验证方法
│  │  □ 关键需求至少有两种互补方法或含单一方法理由
│  │  □ 每条需求有 Oracle 来源（非 ai_generated）
│  │  □ RTM 双向孤儿率 = 0
│  │  □ 无需求无验证方法（正向孤儿）
│  │  □ 无测试无需求关联（反向孤儿）
│  │  □ 无循环依赖
│  ├─ 输出：验证报告 + 签名
│  └─ 签名：sha256(...)
└─ 输出：DCS 验证报告

Step 4: SE / Test Architect 审批
├─ SE 审查 RVM 中每条需求的验证方法和 Oracle
│  ├─ 确认或修改验证方法
│  ├─ 确认或修改 Oracle 来源
│  ├─ **关键需求的 Oracle 必须由规则或专家确定**
│  └─ 签署 RVM
├─ Test Architect 审查 RTM 的追溯完整性
│  ├─ 确认追溯链接正确
│  ├─ 消除所有孤儿
│  └─ 签署 RTM
└─ 输出：RVM_RTM_Baseline（已签署）
```

#### 效率优化策略

| 策略 | 说明 | 预期效果 |
|---|---|---|
| V&V 双矩阵 | 区分 Verification 和 Validation 方法 | 确保两个维度都被覆盖 |
| DCS 孤儿检查 | 确定性检查双向追溯完整性 | 消除人工检查遗漏 |
| 历史缺陷模式参考 | 参考历史缺陷模式建议测试重点 | 提高测试设计针对性 |

### 输出 Artifact 模板

#### Artifact 名称
`RVM_RTM_Baseline`

#### 文件结构
```
rvm_rtm_baseline/
├── manifest.yaml
├── rvm.json                      # 需求验证矩阵
├── rtm.json                      # 需求追溯矩阵
├── dcs_receipts/
│   └── rvm_rtm_validate.json
└── README.md
```

#### rvm.json Schema

```json
{
  "rvm_version": "1.0",
  "baseline_id": "vbl-20260828-001",
  "created_at": "2026-08-28T13:00:00Z",
  "created_by": "agent-trace-001",
  "signed_by": [
    {"user_id": "user-se-001", "role": "Systems Engineer", "signed_at": "2026-08-28T15:00:00Z"},
    {"user_id": "user-test-architect-001", "role": "Test Architect", "signed_at": "2026-08-28T15:30:00Z"}
  ],
  "statistics": {
    "total_requirements": 45,
    "verification_methods_assigned": 45,
    "validation_methods_assigned": 12,
    "high_priority_with_dual_methods": 15,
    "orphan_rate": 0
  },
  "entries": [
    {
      "requirement_id": "REQ-001",
      "shall_statement": "系统应在 <500ms 内完成代码审查并返回结果",
      "priority": "high",
      "verification_methods": [
        {
          "method": "test",
          "environment": "staging",
          "oracle_source": "deterministic_rule",
          "oracle_value": "p95_latency_ms < 500",
          "test_ids": ["TC-PERF-001", "TC-PERF-002"],
          "owner": "user-qa-lead-001"
        },
        {
          "method": "analysis",
          "environment": "design_review",
          "oracle_source": "human_expert",
          "oracle_value": "架构师确认算法复杂度满足 O(n log n)",
          "owner": "user-architect-001"
        }
      ],
      "validation_methods": [
        {
          "method": "demonstration",
          "environment": "representative-user",
          "oracle_source": "human_expert",
          "oracle_value": "用户确认审查结果在可接受时间内返回",
          "owner": "user-product-001"
        }
      ]
    },
    {
      "requirement_id": "REQ-042",
      "shall_statement": "授权审批不可由 Agent 完成",
      "priority": "high",
      "verification_methods": [
        {
          "method": "test",
          "environment": "staging",
          "oracle_source": "deterministic_rule",
          "oracle_value": "Agent 调用审批 API 时返回 403 Forbidden",
          "test_ids": ["TC-SEC-001", "TC-SEC-002", "TC-SEC-003"],
          "owner": "user-security-lead-001"
        },
        {
          "method": "analysis",
          "environment": "code_review",
          "oracle_source": "human_expert",
          "oracle_value": "安全审查确认权限检查覆盖所有审批路径",
          "owner": "user-security-lead-001"
        }
      ],
      "validation_methods": []
    }
  ],
  "dcs_receipt": {
    "solver_version": "rvm-rtm-validate-v1",
    "input_hash": "sha256:...",
    "signature": "sha256:...",
    "checks": {
      "all_requirements_covered": "pass (45/45)",
      "high_priority_dual_methods": "pass (15/15)",
      "oracle_sources_valid": "pass (no ai_generated)",
      "forward_orphans": "pass (0)",
      "reverse_orphans": "pass (0)"
    }
  }
}
```

### 节点关联与展示

#### 在整体流程中的位置

```
ACT-03-05-01 冻结验证基线 → ACT-03-05-02 建立 RVM/RTM → ACT-03-05-03 设计多层测试
                                ↓
                        RVM_RTM_Baseline
                                ↓
                        [每条需求有验证方法和 Oracle]
```

---
