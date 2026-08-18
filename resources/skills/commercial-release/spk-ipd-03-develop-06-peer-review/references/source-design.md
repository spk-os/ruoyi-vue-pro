## ACT-03-04-06 同行审查

### 基础信息

| 字段 | 内容 |
|---|---|
| **Activity ID** | ACT-03-04-06 |
| **Activity 名称** | 同行审查 |
| **所属阶段** | 开发 |
| **Lead Agent 角色** | Development Orchestrator |
| **执行位置** | task_system |
| **是否使用 Worker Agent** | ✓（Review Agent 标注风险候选） |
| **是否需要 Independent Verifier** | ✓（DCS 验证 reviewer 独立性） |
| **所需模型能力** | CROSS_SOURCE_REASONING + SAFETY_REVIEW |
| **所需 Skill** | ipd-dev-peer-review / spk-code-reviewer / spk-security-scanner |
| **所需 Tool/MCP** | mcp-git-query / mcp-codegraph-explore / mcp-filesystem-read |
| **所需 Knowledge** | KN-OWASP-TOP10-026 / KN-CWE-TOP25-027 / KN-PROJECT-CODING-STANDARDS-022 |
| **所需 Information** | INFO-IMPLEMENTATION-DRAFT（PR）/ INFO-UNIT-EVIDENCE（测试证据） |
| **可读取的 Memory 范围** | project:spk-os-ipd, agent_role:development, capability:cross_source_reasoning |
| **人工责任** | 非作者 Reviewer 批准/拒绝 |
| **失败处理** | Agent 不可用降级人工清单；reviewer 冲突转替补；重大安全问题升级 Security Reviewer；否决时回滚 PR 到上一审查提交 |
| **对应 Flowable 节点** | task_peer_review (ipd-development.bpmn) |
| **证据来源** | RunReceipt + PeerReviewRecord + ArtifactManifest |

### 资源与工具详情

#### 工具调用顺序与效率策略

```
Step 1: 验证 PR 范围
├─ 检查 PR 关联的开发包 ID 和需求 ID
├─ 检查 PR 只修改 allowlist 目录
├─ 检查 PR 的 diff 大小（过大则建议拆分）
└─ 输出：PR 范围验证

Step 2: 确定性检查
├─ 调用 DCS POST /api/v1/review-prerequisites
│  ├─ 检查：
│  │  □ 单元测试已通过（引用 UnitComponentEvidence）
│  │  □ CI 管线已通过（引用 CIRunEvidence）
│  │  □ 无作者自审（reviewer ≠ author）
│  │  □ 必需审查者数量 ≥2
│  └─ 输出：前置条件检查报告
└─ 输出：前置条件通过

Step 3: Review Agent 风险扫描
├─ 调用 spk-code-reviewer.review(pr_diff, requirements, coding_standards)
│  ├─ 审查维度：
│  │  □ 需求覆盖：代码是否实现了关联需求的所有验收标准？
│  │  □ 接口契约：是否违反了接口定义？
│  │  □ 安全：OWASP Top 10 / CWE Top 25 已知模式
│  │  □ 性能：N+1 查询、内存泄漏、死锁风险
│  │  □ 可维护性：命名、结构、注释是否符合规范？
│  ├─ 按严重程度分级：Critical / Major / Minor / Info
│  ├─ 每个问题含：文件、行号、描述、建议修复、依据来源
│  └─ 输出：风险扫描报告
├─ 调用 spk-security-scanner.scan(pr_diff)
│  ├─ SAST 静态分析
│  ├─ 秘密泄漏检测
│  └─ 输出：安全扫描报告
└─ 输出：Agent 审查报告

Step 4: 非作者 Reviewer 人工审查
├─ Reviewer 查看 PR diff + Agent 审查报告
├─ 逐行审查关键代码
├─ 处理 Agent 发现的问题（确认/误报/延期）
├─ 添加自己的审查意见
├─ 做出批准/拒绝/修改决定
└─ 输出：PeerReviewRecord（已签署）

Step 5: 处理审查意见
├─ Developer 处理所有审查意见
├─ 修复确认的问题
├─ 回复误报并说明理由
├─ 更新 PR
└─ 输出：更新后的 PR
```

### 输出 Artifact 模板

#### Artifact 名称
`PeerReviewRecord`

#### PeerReviewRecord Schema

```json
{
  "review_id": "pr-20260818-dp001-001",
  "version": "1.0.0",
  "pr_url": "https://gitea.example.com/aurora/aurora/pulls/42",
  "author": "user-dev-001",
  "reviewers": [
    {
      "user_id": "user-reviewer-001",
      "role": "Senior Developer",
      "independent": true,
      "reviewed_at": "2026-08-18T18:00:00Z",
      "decision": "approved",
      "comments": [
        {
          "file": "src/parser/diff-parser.ts",
          "line": 142,
          "severity": "major",
          "description": "未处理 encoding detection 失败场景",
          "suggestion": "添加 try-catch 并提供 fallback encoding",
          "source": "OWASP A04:2021",
          "resolution": "fixed",
          "fix_commit": "a1b2c3d4"
        }
      ]
    },
    {
      "user_id": "user-reviewer-002",
      "role": "Security Reviewer",
      "independent": true,
      "reviewed_at": "2026-08-18T18:30:00Z",
      "decision": "approved",
      "comments": []
    }
  ],
  "agent_review": {
    "total_findings": 5,
    "critical": 0,
    "major": 1,
    "minor": 2,
    "info": 2,
    "false_positives": 1
  },
  "dcs_checks": {
    "reviewer_independence": "pass",
    "required_reviewers": "pass (2/2)",
    "unit_test_passed": "pass",
    "ci_passed": "pass"
  }
}
```

---
