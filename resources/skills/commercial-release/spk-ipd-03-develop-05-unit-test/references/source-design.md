## ACT-03-04-05 单元/组件验证

### 基础信息

| 字段 | 内容 |
|---|---|
| **Activity ID** | ACT-03-04-05 |
| **Activity 名称** | 单元/组件验证 |
| **所属阶段** | 开发 |
| **Lead Agent 角色** | Development Orchestrator |
| **执行位置** | task_system |
| **是否使用 Worker Agent** | ✓（Test Agent 建议边界用例和解释失败） |
| **是否需要 Independent Verifier** | ✓（确定性 runner 产生不可篡改的结果） |
| **所需模型能力** | CODE_OR_FORMULA_ASSISTANCE + GROUNDED_EXTRACTION |
| **所需 Skill** | ipd-dev-unit-test / spk-test-runner / spk-failure-analyzer |
| **所需 Tool/MCP** | mcp-ci-trigger / mcp-git-query / mcp-filesystem-read |
| **所需 Knowledge** | KN-ISO-29119-TEST-024 / KN-IPD-COVERAGE-POLICY-025 |
| **所需 Information** | INFO-IMPLEMENTATION-DRAFT（PR 草稿）/ INFO-CI-CONFIG（CI 配置） |
| **可读取的 Memory 范围** | project:spk-os-ipd, agent_role:development, capability:code_or_formula_assistance |
| **人工责任** | Developer/Test Owner 决定修复或豁免申请 |
| **失败处理** | 基础设施失败同环境重试一次；flaky 则隔离并降级人工判断；安全/数据破坏风险升级 Test Lead；回滚到前一绿 commit |
| **对应 Flowable 节点** | task_unit_component_verify (ipd-development.bpmn) |
| **证据来源** | RunReceipt + CIReceipt + ArtifactManifest |

### 资源与工具详情

#### 工具调用顺序与效率策略

```
Step 1: 固定代码/依赖/数据版本
├─ 锁定 commit SHA
├─ 锁定依赖版本（package-lock.json / requirements.txt）
├─ 锁定测试数据版本（fixture hash）
├─ 锁定随机种子（seed=42）
└─ 输出：固定版本清单

Step 2: 确定性 runner 执行测试
├─ 调用 mcp-ci-trigger.run_unit_tests(commit, seed, fixtures)
│  ├─ 运行单元测试
│  ├─ 运行组件契约测试
│  ├─ 采集覆盖率
│  ├─ 采集原始日志
│  └─ 输出：测试结果（pass/fail + 覆盖率 + 日志 hash）
├─ 调用 DCS POST /api/v1/test-coverage
│  ├─ 输入：covered_lines, total_lines, threshold
│  ├─ 输出：coverage_pct, passed, signature
│  └─ 签名：sha256(...)
└─ 输出：UnitComponentEvidence（草稿）

Step 3: Test Agent 分析失败（如有）
├─ 如果有失败测试：
│  ├─ 调用 spk-failure-analyzer.analyze(failed_tests, logs, code)
│  │  ├─ 分类失败原因（逻辑错误/环境问题/flaky/数据问题）
│  │  ├─ 生成修复建议（不自动修复）
│  │  └─ 输出：失败分析报告
│  └─ Developer 根据建议修复
└─ 输出：修复后的代码（如有修复）

Step 4: Test Owner 复核
├─ 审查测试结果和覆盖率
├─ 处理例外（误报标记，需安全人员审批）
├─ 签署证据
└─ 输出：UnitComponentEvidence（已签署）
```

### 输出 Artifact 模板

#### Artifact 名称
`UnitComponentEvidence`

#### UnitComponentEvidence Schema

```json
{
  "evidence_id": "uce-20260818-dp001-001",
  "version": "1.0.0",
  "created_at": "2026-08-18T17:00:00Z",
  "commit": "e5f6g7h8i9j0k1l2m3n4",
  "environment": {
    "os": "Ubuntu 22.04",
    "runtime": "Node.js 20.11",
    "dependencies_hash": "sha256:..."
  },
  "commands": [
    "npm test -- --seed=42 --coverage",
    "npm run test:contract"
  ],
  "results": {
    "unit_tests": {
      "total": 45,
      "passed": 45,
      "failed": 0,
      "skipped": 0,
      "duration_sec": 12.3
    },
    "contract_tests": {
      "total": 8,
      "passed": 8,
      "failed": 0
    },
    "coverage": {
      "lines_pct": 91.2,
      "branches_pct": 87.5,
      "functions_pct": 95.0,
      "threshold": 85,
      "passed": true
    }
  },
  "logs_hash": "sha256:...",
  "seed": 42,
  "reproducible": true,
  "signed_by": {
    "user_id": "user-test-owner-001",
    "role": "Test Owner",
    "signed_at": "2026-08-18T17:30:00Z"
  },
  "dcs_receipt": {
    "solver_version": "test-coverage-v1",
    "input_hash": "sha256:...",
    "signature": "sha256:..."
  }
}
```

---
