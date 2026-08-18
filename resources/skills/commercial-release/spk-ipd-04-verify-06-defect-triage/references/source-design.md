## ACT-03-05-06 缺陷分诊闭环

### 基础信息

| 字段 | 内容 |
|---|---|
| **Activity ID** | ACT-03-05-06 |
| **Activity 名称** | 缺陷分诊闭环 |
| **所属阶段** | 验证 |
| **Lead Agent 角色** | Verification Orchestrator |
| **执行位置** | task_system |
| **是否使用 Worker Agent** | ✓（Defect Analyst Agent 聚类/建议根因） |
| **是否需要 Independent Verifier** | ✓（DCS 做相似度/趋势/状态约束） |
| **所需模型能力** | CROSS_SOURCE_REASONING + GROUNDED_EXTRACTION |
| **所需 Skill** | ipd-verify-defect-triage / spk-defect-analyst |
| **所需 Tool/MCP** | mcp-spk-os-artifact-query / mcp-git-query / mcp-filesystem-read |
| **所需 Knowledge** | KN-IPD-DEFECT-SCHEMA-048 / KN-DEFECT-CLASSIFICATION-049 |
| **所需 Information** | INFO-TEST-EVIDENCE（测试执行证据）/ INFO-CODE-REPO（代码仓库） |
| **可读取的 Memory 范围** | project:spk-os-ipd, agent_role:verification, capability:cross_source_reasoning |
| **人工责任** | PDT/QA 独占严重度、优先级与关闭 |
| **失败处理** | 误聚类时调低合并并重试；仍不确定保持独立缺陷；安全/系统性缺陷升级 Incident/CCB；关闭被否决则回滚为 reopened |
| **对应 Flowable 节点** | task_defect_triage (ipd-qualification.bpmn) |
| **证据来源** | RunReceipt + DefectClosureRecord + ArtifactManifest |

### 资源与工具详情

#### 工具调用顺序与效率策略

```
Step 1: 导入失败证据
├─ 从 TestExecutionEvidence 中提取所有失败测试
├─ 加载每个失败的原始日志和失败分析
├─ 关联失败测试到需求和代码
└─ 输出：失败证据集合

Step 2: Defect Analyst Agent 聚类
├─ 调用 spk-defect-analyst.cluster(failure_evidence)
│  ├─ 使用嵌入向量计算相似度
│  ├─ 聚类相似失败（同一根因的多个表现）
│  ├─ 为每个聚类建议根因
│  ├─ 建立 defect-to-test-to-requirement 链接
│  ├─ 标注聚类置信度
│  └─ 输出：缺陷聚类报告
├─ DCS 校验：
│  ├─ 相似度阈值检查
│  ├─ 状态约束检查（不重复合并已关闭缺陷）
│  └─ 趋势分析（新增/回归/持续）
└─ 输出：缺陷聚类报告（含 DCS 签名）

Step 3: PDT/QA 人工定级
├─ PDT/QA 审查聚类报告
├─ 为每个缺陷确定：
│  ├─ 严重度（P0/P1/P2/P3）
│  ├─ 优先级（修复顺序）
│  ├─ 影响需求
│  ├─ 修复负责人
│  └─ 是否需要 CCB 变更控制
├─ **AI 不能设定严重度或关闭缺陷**
└─ 输出：缺陷记录（已定级）

Step 4: 修复与回归
├─ 开发者修复缺陷
├─ 创建修复 PR
├─ 运行针对性回归测试（修复点 + 邻接场景）
├─ 回归测试通过
├─ 记录修复 commit SHA
└─ 输出：修复证据 + 回归证据

Step 5: 独立回归与关闭
├─ 独立测试人员验证修复有效性
├─ 确认回归测试通过
├─ QA 签署关闭
├─ **关闭者不同于修复作者**
└─ 输出：DefectClosureRecord（已签署）
```

### 输出 Artifact 模板

#### Artifact 名称
`DefectClosureRecord`

#### DefectClosureRecord Schema

```json
{
  "record_id": "dcr-20260830-001",
  "version": "1.0.0",
  "defects": [
    {
      "defect_id": "DEF-001",
      "cluster_id": "CL-001",
      "cluster_rationale": "OAuth token 刷新竞态条件的不同表现",
      "severity": "P1",
      "severity_set_by": "user-pdt-001",
      "priority": "high",
      "affected_requirements": ["REQ-012", "REQ-013"],
      "root_cause": "OAuth token 刷新时缺少分布式锁，并发请求导致旧 token 被提前失效",
      "root_cause_category": "concurrency",
      "original_failures": [
        {"test_id": "TC-AUTH-015", "evidence_ref": "tee-20260829-001"},
        {"test_id": "TC-AUTH-018", "evidence_ref": "tee-20260829-001"}
      ],
      "fix": {
        "commit_sha": "f1g2h3i4j5k6l7m8n9o0",
        "pr_url": "https://gitea.example.com/aurora/aurora/pulls/55",
        "description": "在 token 刷新路径添加 Redis 分布式锁",
        "author": "user-dev-002"
      },
      "regression": {
        "test_ids": ["TC-AUTH-015", "TC-AUTH-018", "TC-AUTH-020", "TC-AUTH-021"],
        "all_passed": true,
        "evidence_ref": "tee-20260830-001"
      },
      "closed_by": {
        "user_id": "user-qa-002",
        "role": "QA Engineer",
        "closed_at": "2026-08-30T15:00:00Z",
        "different_from_author": true
      }
    }
  ],
  "statistics": {
    "total_defects": 4,
    "clusters": 2,
    "p0_count": 0,
    "p1_count": 2,
    "p2_count": 2,
    "p3_count": 0,
    "closed": 4,
    "open": 0,
    "convergence_trend": "decreasing"
  },
  "dcs_receipt": {
    "solver_version": "defect-trend-v1",
    "signature": "sha256:..."
  }
}
```

---
