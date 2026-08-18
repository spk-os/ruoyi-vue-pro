## ACT-03-05-05 执行与采证

### 基础信息

| 字段 | 内容 |
|---|---|
| **Activity ID** | ACT-03-05-05 |
| **Activity 名称** | 执行与采证 |
| **所属阶段** | 验证 |
| **Lead Agent 角色** | Verification Orchestrator |
| **执行位置** | task_system |
| **是否使用 Worker Agent** | ✓（Failure Insight Agent 仅解释异常） |
| **是否需要 Independent Verifier** | ✓（确定性 Executor 产生不可篡改的结果） |
| **所需模型能力** | GROUNDED_EXTRACTION |
| **所需 Skill** | ipd-verify-execute / spk-test-executor / spk-failure-insight |
| **所需 Tool/MCP** | mcp-ci-trigger / mcp-spk-os-artifact-query / mcp-filesystem-read |
| **所需 Knowledge** | KN-IPD-TEST-EXEC-SCHEMA-046 / KN-FLAKY-TEST-GOVERNANCE-047 |
| **所需 Information** | INFO-TEST-DESIGN（测试设计）/ INFO-TEST-ENV（测试环境）/ INFO-VERIFY-BASELINE（验证基线） |
| **可读取的 Memory 范围** | project:spk-os-ipd, agent_role:verification, capability:grounded_extraction |
| **人工责任** | Test Lead 判定测试结果有效性 |
| **失败处理** | 基础设施类失败同夹具重试一次；flaky 隔离并降级人工；疑似数据泄露升级 Security 并停跑；回滚环境到执行前快照 |
| **对应 Flowable 节点** | task_test_execution (ipd-qualification.bpmn) |
| **证据来源** | CIReceipt + TestExecutionEvidence + ArtifactManifest |

### 资源与工具详情

#### 工具调用顺序与效率策略

```
Step 1: 校验基线/环境一致性
├─ 校验构建物 hash 与验证基线一致
├─ 校验环境配置 hash 与环境准备记录一致
├─ 校验测试代码 hash 与验证基线一致
├─ 校验测试数据 hash 与数据准备记录一致
├─ 任何不一致则阻断执行
└─ 输出：一致性校验报告

Step 2: 确定性 Executor 执行测试
├─ 按固定 seed 执行所有测试用例
├─ 记录完整版本信息：
│  ├─ 代码版本（commit SHA）
│  ├─ 配置版本（config hash）
│  ├─ 数据版本（data hash）
│  ├─ 模型版本（如有 AI 组件）
│  ├─ Runner 镜像版本（含 digest）
│  └─ 时间戳
├─ 采集原始日志/指标/截图
├─ 每个测试结果计算 SHA-256 hash
├─ 存入不可变日志
├─ **禁止覆盖原始结果**
└─ 输出：原始执行日志

Step 3: Flaky 检测
├─ 调用 DCS POST /api/v1/flaky-detect
│  ├─ 输入：每个测试的最近 N 次结果
│  ├─ 计算 flaky 率
│  ├─ flaky 率超阈值的测试自动隔离
│  ├─ 输出：flaky 报告 + 签名
│  └─ 签名：sha256(...)
├─ **flaky 测试不以重跑通过覆盖**
└─ 输出：Flaky 报告

Step 4: Failure Insight Agent 解释失败（如有）
├─ 如果有失败测试：
│  ├─ 调用 spk-failure-insight.explain(failed_tests, logs, code, environment)
│  │  ├─ 分类失败原因：
│  │  │  ├─ 真失败（代码缺陷）
│  │  │  ├─ 测试问题（测试代码缺陷）
│  │  │  ├─ 环境问题（基础设施故障）
│  │  │  └─ 数据问题（测试数据异常）
│  │  ├─ 生成修复建议（不自动修复）
│  │  └─ 输出：失败分析报告
│  └─ Test Lead 判定有效性
└─ 输出：失败分析报告

Step 5: Test Lead 签署
├─ 审查执行结果和失败分析
├─ 判定每个失败的有效性
├─ 签署执行证据
└─ 输出：TestExecutionEvidence（已签署）
```

### 输出 Artifact 模板

#### Artifact 名称
`TestExecutionEvidence`

#### TestExecutionEvidence Schema

```json
{
  "evidence_id": "tee-20260829-001",
  "version": "1.0.0",
  "created_at": "2026-08-29T14:00:00Z",
  "baseline_ref": "vbl-20260828-001",
  "environment_ref": "rte-20260829-001",
  "test_design_ref": "mtd-20260828-001",
  "versions": {
    "build_commit": "e5f6g7h8i9j0k1l2m3n4",
    "build_hash": "sha256:abc123...",
    "test_code_hash": "sha256:ghi789...",
    "test_data_hash": "sha256:jkl012...",
    "env_config_hash": "sha256:pqr678...",
    "runner_image_digest": "sha256:stu901..."
  },
  "seed": 42,
  "results_summary": {
    "total": 168,
    "passed": 162,
    "failed": 4,
    "blocked": 0,
    "skipped": 0,
    "flaky_isolated": 2
  },
  "results": [
    {
      "test_id": "TC-PERF-001",
      "requirement_id": "REQ-001",
      "result": "pass",
      "oracle_source": "deterministic_rule",
      "oracle_value": "p95_latency_ms < 500",
      "actual_value": "p95_latency_ms = 180",
      "duration_sec": 62,
      "log_hash": "sha256:vwx234...",
      "timestamp": "2026-08-29T14:15:00Z"
    },
    {
      "test_id": "TC-SEC-001",
      "requirement_id": "REQ-042",
      "result": "pass",
      "oracle_source": "deterministic_rule",
      "oracle_value": "HTTP 403 Forbidden",
      "actual_value": "HTTP 403 Forbidden, error_code=INSUFFICIENT_PERMISSIONS",
      "duration_sec": 3,
      "log_hash": "sha256:yza567...",
      "timestamp": "2026-08-29T14:20:00Z"
    },
    {
      "test_id": "TC-AUTH-015",
      "requirement_id": "REQ-012",
      "result": "fail",
      "oracle_source": "deterministic_rule",
      "oracle_value": "token_refresh 成功返回新 token",
      "actual_value": "token_refresh 超时 (30s)",
      "duration_sec": 35,
      "log_hash": "sha256:bcd890...",
      "timestamp": "2026-08-29T14:25:00Z",
      "failure_analysis": {
        "category": "true_failure",
        "root_cause": "OAuth token 刷新存在竞态条件，并发请求时旧 token 被提前失效",
        "suggestion": "在 token 刷新期间添加分布式锁",
        "confidence": 0.85
      }
    }
  ],
  "flaky_tests": [
    {
      "test_id": "TC-NET-003",
      "flaky_rate": 0.30,
      "threshold": 0.10,
      "status": "isolated",
      "recommendation": "修复网络超时模拟的确定性问题"
    }
  ],
  "signed_by": {
    "user_id": "user-test-lead-001",
    "role": "Test Lead",
    "signed_at": "2026-08-29T16:00:00Z"
  },
  "replayable": true,
  "dcs_receipts": {
    "flaky_detect": {
      "solver_version": "flaky-detect-v1",
      "signature": "sha256:..."
    }
  }
}
```

---
