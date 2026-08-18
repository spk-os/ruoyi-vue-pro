## ACT-03-04-07 持续集成

### 基础信息

| 字段 | 内容 |
|---|---|
| **Activity ID** | ACT-03-04-07 |
| **Activity 名称** | 持续集成 |
| **所属阶段** | 开发 |
| **Lead Agent 角色** | Development Orchestrator |
| **执行位置** | task_system |
| **是否使用 Worker Agent** | ✓（CI Insight Agent 仅解释失败） |
| **是否需要 Independent Verifier** | ✓（确定性流水线产生不可篡改的结果） |
| **所需模型能力** | GROUNDED_EXTRACTION |
| **所需 Skill** | ipd-dev-ci / spk-ci-insight / spk-sbom-generator |
| **所需 Tool/MCP** | mcp-ci-trigger / mcp-git-query |
| **所需 Knowledge** | KN-SLSA-SUPPLY-CHAIN-028 / KN-NIST-SSDF-029 |
| **所需 Information** | INFO-PR（已审查的 PR）/ INFO-CI-CONFIG |
| **可读取的 Memory 范围** | project:spk-os-ipd, agent_role:development, capability:grounded_extraction |
| **人工责任** | Repo Owner 管理阈值与例外 |
| **失败处理** | runner 故障在干净节点重试一次；仍故障降级人工排队但不置绿；供应链异常升级 Security/Platform；回滚部署制品到上次绿 build |
| **对应 Flowable 节点** | task_ci (ipd-development.bpmn) |
| **证据来源** | CIReceipt + ArtifactManifest |

### 资源与工具详情

#### 工具调用顺序与效率策略

```
Step 1: 固定 commit/镜像
├─ 锁定 commit SHA
├─ 锁定 runner 镜像版本（含 digest）
├─ 锁定依赖版本
└─ 输出：固定环境清单

Step 2: 分层确定性门禁执行
├─ Layer 1: 编译（compile）
│  ├─ 编译所有源代码
│  ├─ 检查编译警告
│  └─ 输出：编译结果 + 日志 hash
├─ Layer 2: 单元测试（unit_test）
│  ├─ 运行所有单元测试
│  ├─ 计算覆盖率
│  ├─ 阈值检查：覆盖率 ≥ 组织设定值
│  └─ 输出：测试结果 + 覆盖率 + 日志 hash
├─ Layer 3: 静态分析（SAST）
│  ├─ 运行 SAST 工具（SonarQube/Semgrep）
│  ├─ 阈值检查：Critical=0, High=0
│  └─ 输出：SAST 结果 + 日志 hash
├─ Layer 4: 依赖检查（SCA）
│  ├─ 检查已知漏洞依赖
│  ├─ 检查许可证合规
│  ├─ 生成 SBOM
│  └─ 输出：SCA 结果 + SBOM + 日志 hash
├─ Layer 5: 集成测试（integration_test）
│  ├─ 运行集成测试
│  ├─ 契约测试
│  └─ 输出：集成测试结果 + 日志 hash
├─ Layer 6: 性能烟测（performance_smoke）
│  ├─ 运行性能烟测
│  ├─ 阈值检查：P95 延迟 < 组织设定值
│  └─ 输出：性能结果 + 日志 hash
├─ 每层失败即停，不继续下一层
└─ 输出：CIRunEvidence（草稿）

Step 3: CI Insight Agent 解释失败（如有）
├─ 如果有失败层：
│  ├─ 调用 spk-ci-insight.explain(failed_layer, logs, code)
│  │  ├─ 分析失败根因
│  │  ├─ 生成修复建议（不自动修复）
│  │  └─ 输出：失败解释报告
│  └─ Developer 根据建议修复
└─ 输出：修复后的代码（如有修复）

Step 4: 签名与归档
├─ 调用 DCS POST /api/v1/ci-sign
│  ├─ 输入：所有层的结果 + 日志 hash + SBOM hash
│  ├─ 输出：整体签名
│  └─ 签名：sha256(...)
├─ 归档 CI 报告和所有日志
└─ 输出：CIRunEvidence（已签名）
```

### 输出 Artifact 模板

#### Artifact 名称
`CIRunEvidence`

#### CIRunEvidence Schema

```json
{
  "ci_id": "ci-20260818-dp001-001",
  "version": "1.0.0",
  "commit": "e5f6g7h8i9j0k1l2m3n4",
  "pipeline_version": "v1",
  "runner_image_digest": "sha256:...",
  "pipeline_stages": [
    {"name": "compile", "status": "pass", "duration_sec": 45, "log_hash": "sha256:..."},
    {"name": "unit_test", "status": "pass", "tests_run": 342, "tests_passed": 342, "coverage_pct": 87.3, "threshold": 80, "log_hash": "sha256:..."},
    {"name": "sast", "status": "pass", "findings": {"critical": 0, "high": 0, "medium": 2, "low": 5}, "threshold": {"critical": 0, "high": 0}, "log_hash": "sha256:..."},
    {"name": "sca", "status": "pass", "vulnerable_deps": 0, "license_conflicts": 0, "sbom_hash": "sha256:...", "log_hash": "sha256:..."},
    {"name": "integration_test", "status": "pass", "tests_run": 28, "tests_passed": 28, "log_hash": "sha256:..."},
    {"name": "performance_smoke", "status": "pass", "p95_latency_ms": 180, "threshold_ms": 500, "log_hash": "sha256:..."}
  ],
  "overall": "pass",
  "sbom": {
    "format": "CycloneDX 1.5",
    "total_components": 127,
    "license_summary": {"MIT": 95, "Apache-2.0": 28, "BSD-3": 4},
    "known_vulnerabilities": 0
  },
  "signature": "sha256:...",
  "timestamp": "2026-08-18T19:00:00Z",
  "approved_exceptions": [],
  "signed_by": {
    "user_id": "user-repo-owner-001",
    "role": "Repo Owner",
    "signed_at": "2026-08-18T19:15:00Z"
  }
}
```

---
