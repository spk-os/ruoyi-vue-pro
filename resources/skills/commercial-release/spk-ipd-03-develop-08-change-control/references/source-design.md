## ACT-03-04-08 变更控制

### 基础信息

| 字段 | 内容 |
|---|---|
| **Activity ID** | ACT-03-04-08 |
| **Activity 名称** | 变更控制 |
| **所属阶段** | 开发 |
| **Lead Agent 角色** | Development Orchestrator |
| **执行位置** | task_system |
| **是否使用 Worker Agent** | ✓（Change Impact Agent 生成影响矩阵和方案草案） |
| **是否需要 Independent Verifier** | ✓（DCS 遍历追溯和依赖图） |
| **所需模型能力** | CROSS_SOURCE_REASONING + STRUCTURED_DRAFTING |
| **所需 Skill** | ipd-dev-change-control / spk-impact-analyzer |
| **所需 Tool/MCP** | mcp-spk-os-artifact-query / mcp-git-query / mcp-codegraph-explore |
| **所需 Knowledge** | KN-IEEE-828-CM-017 / KN-CMMI-V3-CM-030 |
| **所需 Information** | INFO-DEV-BASELINE（已锁定基线）/ INFO-PROPOSED-CHANGE（变更提案） |
| **可读取的 Memory 范围** | project:spk-os-ipd, agent_role:development, capability:cross_source_reasoning |
| **人工责任** | CCB 具名成员批准/拒绝（**AI 禁写 CCB 状态**） |
| **失败处理** | 图计算失败同快照重试；不完整则降级人工 walk-through；跨产品/合规影响升级 Architecture/IPMT；拒绝或撤回时回滚所有派生基线 |
| **对应 Flowable 节点** | task_change_control (ipd-development.bpmn) |
| **证据来源** | RunReceipt + ChangeDecisionRecord + ArtifactManifest |

### 资源与工具详情

#### 工具调用顺序与效率策略

```
Step 1: 登记变更请求
├─ 从 PR 或 Issue 中提取变更提案
├─ 绑定来源（为什么需要变更）
├─ 分类变更类型（需求变更/架构变更/接口变更/计划变更）
└─ 输出：ChangeRequest

Step 2: Change Impact Agent 计算影响矩阵
├─ 调用 spk-impact-analyzer.analyze(change_request, baseline, dependency_graph)
│  ├─ 遍历需求追溯链：受影响的需求
│  ├─ 遍历架构依赖图：受影响的组件和接口
│  ├─ 遍历代码依赖图：受影响的代码文件
│  ├─ 遍历测试追溯链：受影响的测试用例
│  ├─ 遍历计划依赖：受影响的进度和成本
│  ├─ 遍历风险登记册：受影响的风险
│  └─ 输出：影响矩阵
├─ 调用 DCS POST /api/v1/change-impact
│  ├─ 输入：baseline_commit, proposed_changes, dependency_graph
│  ├─ 输出：affected_interfaces, affected_cbbs, affected_tests, risk_score, signature
│  └─ 签名：sha256(...)
└─ 输出：影响矩阵（含 DCS 签名）

Step 3: 生成变更方案草案
├─ 调用 spk-impact-analyzer.generate_options(impact_matrix)
│  ├─ 选项 A：接受变更（含成本/风险/进度影响）
│  ├─ 选项 B：修改变更（减少影响范围）
│  ├─ 选项 C：拒绝变更（保持基线不变）
│  ├─ 每个选项含：成本估算、风险评估、进度影响、回退方案
│  └─ 输出：变更方案草案
└─ 输出：ChangeImpactPack

Step 4: CCB 评审（人工，AI 禁写）
├─ CCB 审查影响包和方案草案
├─ CCB 做出决定：
│  ├─ 批准：更新基线，继续开发
│  ├─ 修改：要求调整变更范围后重新评审
│  └─ 拒绝：关闭 PR，保持基线不变
├─ **关键约束**：CCB 状态只允许具名 CCB 成员通过 yudao 写入
├─ **系统层面禁止任何 AI Agent 写入 CCB 状态字段**
├─ 记录决定理由和异议
└─ 输出：ChangeDecisionRecord（已签署）

Step 5: 批准后基线更新
├─ 更新受影响的基线制品
├─ 创建新版本（不覆盖旧版本）
├─ 更新追溯矩阵
├─ 通知受影响的开发包负责人
└─ 输出：更新后的基线
```

### 输出 Artifact 模板

#### Artifact 名称
`ChangeDecisionRecord`

#### ChangeDecisionRecord Schema

```json
{
  "change_id": "cr-20260820-001",
  "version": "1.0.0",
  "created_at": "2026-08-20T10:00:00Z",
  "created_by": "agent-change-impact-001",
  "request": {
    "source": "PR #45",
    "type": "interface_change",
    "description": "DP-002 需要修改 DP-001 的 DiffParser 接口，新增 context_lines 参数",
    "requester": "user-dev-002"
  },
  "impact_analysis": {
    "affected_requirements": ["REQ-001", "REQ-003"],
    "affected_interfaces": ["IF-DIFF-01"],
    "affected_code_files": ["src/parser/diff-parser.ts", "src/parser/types.ts"],
    "affected_tests": ["test/parser/diff-parser.test.ts (3 tests)"],
    "affected_dev_packets": ["DP-001", "DP-003"],
    "affected_schedule": "+2 days",
    "affected_cost": "+¥15,000",
    "risk_score": 0.35,
    "dcs_signature": "sha256:..."
  },
  "options": [
    {
      "option": "accept",
      "description": "接受接口变更，更新 DP-001 和 DP-003",
      "cost": "¥15,000",
      "schedule_impact": "+2 days",
      "risk": "low"
    },
    {
      "option": "modify",
      "description": "将 context_lines 作为可选参数，默认值保持向后兼容",
      "cost": "¥8,000",
      "schedule_impact": "+1 day",
      "risk": "low"
    },
    {
      "option": "reject",
      "description": "拒绝变更，DP-002 使用现有接口",
      "cost": "¥0",
      "schedule_impact": "0",
      "risk": "medium (DP-002 功能受限)"
    }
  ],
  "ccb_decision": {
    "decision": "accept",
    "selected_option": "modify",
    "decided_by": [
      {"user_id": "user-ccb-chair-001", "role": "CCB Chair", "vote": "approve"},
      {"user_id": "user-ccb-member-001", "role": "CCB Member", "vote": "approve"},
      {"user_id": "user-ccb-member-002", "role": "CCB Member", "vote": "approve"}
    ],
    "decided_at": "2026-08-20T14:00:00Z",
    "rationale": "选择修改方案：向后兼容的可选参数，影响最小",
    "conditions": ["DP-001 更新后需重新运行测试"],
    "dissent": []
  },
  "baseline_update": {
    "old_baseline": "dbl-20260818-001",
    "new_baseline": "dbl-20260820-001",
    "updated_artifacts": ["ad-20260817-001 → v1.1.0", "vs-20260817-001 → v1.1.0"]
  }
}
```

---
