## ACT-03-07-04 Maintenance Train Management

### 基础信息

| 字段 | 内容 |
|---|---|
| **Activity ID** | ACT-03-07-04 |
| **Activity 名称** | Maintenance Train Management（维护列车管理） |
| **所属阶段** | 生命周期管理 |
| **Lead Agent 角色** | Lifecycle Orchestrator |
| **执行位置** | task_system |
| **是否使用 Worker Agent** | ✓（Train Planner 建议批次/依赖 + Regression Tester 自动回归） |
| **是否需要 Independent Verifier** | ✓（DCS 计算容量/冲突/Release Manifest 门禁 + LMT/Release Manager 承诺列车） |
| **所需模型能力** | STRUCTURED_DRAFTING + CROSS_SOURCE_REASONING |
| **所需 Skill** | ipd-lifecycle-maintenance / spk-train-planner / spk-regression-tester |
| **所需 Tool/MCP** | mcp-spk-os-artifact-query / mcp-ci-trigger / mcp-git-query |
| **所需 Knowledge** | KN-IPD-MAINTENANCE-SCHEMA-076 / KN-NIST-SSDF-MAINTENANCE-077 |
| **所需 Information** | INFO-VULN-EXPOSURE（漏洞暴露记录）/ INFO-VERSION-GENEALOGY（版本族谱）/ INFO-ENHANCEMENT-REQUESTS（增强需求） |
| **可读取的 Memory 范围** | project:spk-os-ipd, agent_role:lifecycle, capability:structured_drafting |
| **人工责任** | Release Manager 审批维护版本、签署发布；LMT Lead 承诺列车日期 |
| **失败处理** | 求解失败同快照重试；不可行降级人工协商；紧急漏洞与冻结窗冲突升级 LMT/IPMT；回滚到前一批准列车 |
| **对应 Flowable 节点** | task_patch_train (ipd-lifecycle.bpmn) |
| **证据来源** | RunReceipt + MaintenanceTrainPlan + ArtifactManifest |

### 资源与工具详情

#### 工具调用顺序与效率策略

```
Step 1: 分流安全/缺陷/增强
├─ 从 VulnerabilityExposureRecord 中提取待修复漏洞
├─ 从 DefectClosureRecord 中提取待修复缺陷
├─ 从 FeedbackThemeSet 中提取增强需求
├─ 分流：
│  ├─ 紧急列车：优先级 ≥70 的漏洞（72h SLA）
│  ├─ 季度列车：优先级 40-69 的漏洞 + 缺陷 + 增强（90d 周期）
│  └─ 不纳入：优先级 <40 的漏洞（监控）
└─ 输出：分流结果

Step 2: Train Planner 建议批次
├─ 调用 spk-train-planner.plan(patches, enhancements, genealogy, capacity)
│  ├─ 对每个列车：
│  │  ├─ 组合修复项（漏洞 + 缺陷 + 增强）
│  │  ├─ 计算依赖关系（修复 A 是否依赖修复 B）
│  │  ├─ 计算容量需求（开发工时、测试工时、审查工时）
│  │  ├─ 检查版本兼容性（补丁是否破坏向后兼容）
│  │  ├─ 生成 changelog 草稿
│  │  └─ 标注风险点
│  └─ 输出：列车计划草稿
└─ 输出：MaintenanceTrainPlan（草稿）

Step 3: DCS 门禁校验
├─ 调用 DCS POST /api/v1/train-validate
│  ├─ 检查：
│  │  □ 所有紧急漏洞在 72h SLA 内
│  │  □ 容量承诺已确认（开发/测试/审查工时可用）
│  │  □ 无依赖冲突（修复间无循环依赖）
│  │  □ 版本兼容性矩阵已验证
│  │  □ 回归测试套件已准备
│  │  □ 回滚包已准备
│  │  □ Release Manifest 门禁可通过（引用 ACT-03-06-01）
│  ├─ 输出：校验报告 + 签名
│  └─ 签名：sha256(...)
└─ 输出：DCS 校验报告

Step 4: LMT/Release Manager 承诺
├─ LMT Lead 审查列车计划
├─ Release Manager 确认：
│  ├─ 列车日期
│  ├─ 范围（修复项列表）
│  ├─ 容量承诺
│  └─ 风险接受
├─ 签署列车计划
└─ 输出：MaintenanceTrainPlan（已签署）

Step 5: 执行修复与回归
├─ 开发者修复漏洞/缺陷
├─ 调用 spk-regression-tester.run(fixes, regression_suite)
│  ├─ 执行受影响功能的回归测试套件
│  ├─ 执行完整单元测试
│  ├─ 执行集成测试
│  └─ 输出：回归测试报告
├─ 构建补丁版本
├─ 生成 Release Manifest（引用 ACT-03-06-01 流程）
├─ Release Manager 签署发布
└─ 输出：补丁版本 + Release Manifest

Step 6: 发布与观察
├─ 按 ACT-03-06-04 渐进发布流程部署补丁
├─ 观察窗内监控 SLO 和护栏指标
├─ 破阈值即执行已演练回滚
└─ 输出：发布记录 + 观察报告
```

### 输出 Artifact 模板

#### Artifact 名称
`MaintenanceTrainPlan`

#### MaintenanceTrainPlan Schema

```json
{
  "plan_id": "mtp-20260915-001",
  "version": "1.0.0",
  "created_at": "2026-09-15T12:00:00Z",
  "signed_by": [
    {"user_id": "user-lmt-lead-001", "role": "LMT Lead", "signed_at": "2026-09-15T13:00:00Z"},
    {"user_id": "user-release-manager-001", "role": "Release Manager", "signed_at": "2026-09-15T13:30:00Z"}
  ],
  "train_type": "emergency",
  "train_id": "EMERGENCY-2026-09-15",
  "sla_hours": 72,
  "sla_deadline": "2026-09-18T10:00:00Z",
  "target_versions": ["2.0.2", "1.9.6"],
  "patches": [
    {
      "patch_id": "PATCH-001",
      "type": "security",
      "cve_id": "CVE-2026-XXXXX",
      "description": "Upgrade tree-sitter from 0.22.3 to 0.22.4",
      "affected_component": "pkg:npm/tree-sitter",
      "priority_score": 78,
      "estimated_effort_hours": 4,
      "dependencies": [],
      "compatibility": "backward_compatible"
    }
  ],
  "capacity": {
    "development_hours": 4,
    "testing_hours": 2,
    "review_hours": 1,
    "total_hours": 7,
    "available": true
  },
  "regression_suite": {
    "total_tests": 342,
    "affected_tests": 45,
    "estimated_duration_min": 15
  },
  "rollback_plan": {
    "rollback_safe": true,
    "rollback_target": "2.0.1",
    "estimated_rollback_time_min": 10,
    "rehearsal_status": "passed"
  },
  "changelog_draft": "## Aurora v2.0.2 (Security Patch)\n\n### Security Fixes\n- Upgrade tree-sitter from 0.22.3 to 0.22.4 (CVE-2026-XXXXX)\n\n### Compatibility\n- Backward compatible with v2.0.x\n- No configuration changes required",
  "dcs_receipt": {
    "solver_version": "train-validate-v1",
    "input_hash": "sha256:...",
    "signature": "sha256:..."
  }
}
```

---
