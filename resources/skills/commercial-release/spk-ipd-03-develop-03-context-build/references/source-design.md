## ACT-03-04-03 Context 构建

### 基础信息

| 字段 | 内容 |
|---|---|
| **Activity ID** | ACT-03-04-03 |
| **Activity 名称** | Context 构建 |
| **所属阶段** | 开发 |
| **Lead Agent 角色** | Development Orchestrator |
| **执行位置** | task_system |
| **是否使用 Worker Agent** | — |
| **是否需要 Independent Verifier** | ✓（DCS ACL/版本/token 预算校验） |
| **所需模型能力** | GROUNDED_EXTRACTION |
| **所需 Skill** | ipd-dev-context-build / spk-context-pack-builder |
| **所需 Tool/MCP** | mcp-spk-os-artifact-query / mcp-git-query / mcp-codegraph-explore / mcp-filesystem-read |
| **所需 Knowledge** | KN-IPD-CONTEXT-PACK-SCHEMA-020 / KN-OWASP-LLM-TOP10-021 |
| **所需 Information** | INFO-DEV-PACKET（当前开发包定义）/ INFO-CODE-REPO（代码仓库） |
| **可读取的 Memory 范围** | project:spk-os-ipd, agent_role:development, capability:grounded_extraction |
| **人工责任** | Developer / Reviewer 确认上下文相关性 |
| **失败处理** | 检索超时按子域重试；不足则降级关键词清单；ACL 异常升级 Data/Repo Owner；回滚删除 bundle 和派生缓存 |
| **对应 Flowable 节点** | task_context_build (ipd-development.bpmn) |
| **证据来源** | RunReceipt + ArtifactManifest |

### 资源与工具详情

#### 输入资源

| 资源 | 来源 | 获取方式 | 质量要求 |
|---|---|---|---|
| 当前开发包定义 | ACT-03-04-02 产物 | mcp-spk-os-artifact-query.get(package_id) | 含需求/接口/风险/测试策略 |
| 代码仓库 | Git | mcp-git-query.get_repo_tree() | 含目录结构和关键文件 |
| 相关 ADR | 架构基线 | mcp-spk-os-artifact-query.get(adr_ids) | 含决策/替代/后果 |
| 接口契约 | 架构基线 | mcp-filesystem-read(/api/specs/) | OpenAPI/Protobuf 定义 |
| CBB 文档 | CBB Registry | mcp-spk-os-cbb-query.get(cbb_id) | 含使用指南和示例 |
| 历史决策 | AgentMemory | memory.query(scope="decision", project=current) | 含 CCB 决策和理由 |
| 编码规范 | Knowledge 库 | mcp-filesystem-read(/knowledge/coding-standards/) | 项目编码规范 |

#### 工具调用顺序与效率策略

```
Step 1: 解析开发包 ACL
├─ 从开发包定义中提取：
│  ├─ 需求 ID 列表
│  ├─ 涉及的架构组件（containers）
│  ├─ 接口 ID 列表
│  ├─ CBB ID 列表
│  └─ 代码目录 allowlist
└─ 输出：ACL 定义

Step 2: 按 ACL 检索最小证据
├─ 检索相关需求详情（从 PRS 基线）
├─ 检索相关 ADR（从架构基线）
├─ 检索接口契约（从 API specs）
├─ 检索 CBB 文档（从 Registry）
├─ 检索相关代码片段（从 Git，仅 allowlist 目录）
│  ├─ 使用 mcp-codegraph-explore 定位相关代码
│  └─ 提取关键函数/类/模块的签名和注释
├─ 检索历史决策（从 AgentMemory）
├─ 加入负面约束（不应做什么、已知陷阱）
└─ 输出：原始检索结果

Step 3: DCS 权限/版本/token 预算校验
├─ 调用 DCS POST /api/v1/context-validate
│  ├─ 检查：
│  │  □ ACL 合规：所有检索内容在 allowlist 范围内
│  │  □ 版本一致：所有引用与基线版本匹配
│  │  □ Token 预算：总 token 数不超过模型上下文窗口的 80%
│  │  □ 新鲜度：所有引用内容的时间戳在有效期内
│  │  □ 去指令化：外部数据（Issue 评论、网页内容）已去除指令性内容
│  ├─ 输出：验证报告 + 签名
│  └─ 签名：sha256(...)
└─ 输出：DCS 验证报告

Step 4: 构建六层 Context Pack
├─ Layer 1: authority.json（操作者、Agent、允许工具、数据域、到期时间）
├─ Layer 2: baseline.json（Charter/需求/架构/接口/风险的 Git URI 与 digest）
├─ Layer 3: task.yaml（目标、非目标、验收、约束、预算、失败升级）
├─ Layer 4: retrieval.jsonl（检索片段、来源、时间、可信等级、引用位置）
├─ Layer 5: policy.yaml（代码、安全、隐私、许可证、测试和合并策略）
├─ Layer 6: memory_refs.json（AgentMemory 检索到的历史决策引用）
└─ 输出：ContextBundle

Step 5: Developer / Reviewer 确认
├─ Developer 审查 Context Pack 的相关性和充分性
├─ 确认无越权内容
├─ 确认无遗漏关键信息
└─ 输出：ContextBundle（已确认）
```

#### 效率优化策略

| 策略 | 说明 | 预期效果 |
|---|---|---|
| 最小权限检索 | 只检索 ACL 允许的内容 | 减少 token 消耗和噪声 |
| 六层分离 | 每层独立管理，可按需加载 | 提高复用性 |
| 去指令化 | 外部数据去除指令性内容 | 防止提示注入 |
| Token 预算控制 | 总量不超过上下文窗口 80% | 留出推理空间 |

### 输出 Artifact 模板

#### Artifact 名称
`ContextBundle`

#### 文件结构
```
context_bundle/
├── manifest.yaml
├── bundle.json                   # 主数据文件
├── layers/
│   ├── authority.json
│   ├── baseline.json
│   ├── task.yaml
│   ├── retrieval.jsonl
│   ├── policy.yaml
│   └── memory_refs.json
├── dcs_receipts/
│   └── context_validate.json
└── README.md
```

#### bundle.json Schema

```json
{
  "bundle_id": "ctx-20260818-dp001-001",
  "version": "1.0.0",
  "created_at": "2026-08-18T14:00:00Z",
  "created_by": "agent-development-orchestrator-001",
  "confirmed_by": {
    "user_id": "user-dev-001",
    "role": "Developer",
    "confirmed_at": "2026-08-18T14:30:00Z"
  },
  "dev_packet_ref": "DP-001",
  "statistics": {
    "total_tokens": 12500,
    "context_window_utilization_pct": 62.5,
    "retrieval_sources": 23,
    "acl_violations": 0,
    "version_mismatches": 0
  },
  "layers": {
    "authority": {
      "operator": "user-dev-001",
      "agent": "agent-implementation-001",
      "allowed_tools": ["git", "ci_trigger", "code_search"],
      "data_scope": ["repo:aurora", "branch:feature/dp-001", "dirs:src/parser,src/ast"],
      "expires_at": "2026-08-18T18:00:00Z"
    },
    "baseline": {
      "charter": {"uri": "git://repo/charter.md", "digest": "sha256:abc123"},
      "requirements": [
        {"id": "REQ-001", "uri": "git://repo/req/001.md", "digest": "sha256:def456"},
        {"id": "REQ-002", "uri": "git://repo/req/002.md", "digest": "sha256:ghi789"}
      ],
      "architecture": {"uri": "git://repo/arch/v2.md", "digest": "sha256:jkl012"},
      "interfaces": [
        {"id": "IF-DIFF-01", "spec_uri": "git://repo/api/diff.yaml", "version": "1.0.0"}
      ]
    },
    "task": {
      "objective": "实现代码差异解析引擎，支持 AST 解析、hunk 提取、上下文标注",
      "non_objective": "不实现 UI 展示、不实现 GitHub 集成",
      "acceptance": ["REQ-001~005 验收标准全部通过"],
      "constraints": ["使用 tree-sitter v0.22", "TypeScript 实现"],
      "budget": "1.5 person-weeks",
      "failure_escalation": "编译失败→开发者修复；架构问题→架构师"
    },
    "retrieval_count": 23,
    "policy": {
      "code_style": "TypeScript strict mode",
      "security": "OWASP Top 10 合规",
      "license": "MIT only, no GPL",
      "test": "覆盖率 ≥85%, 变异测试 ≥60%"
    },
    "memory_refs_count": 3
  },
  "dcs_receipt": {
    "solver_version": "context-validate-v1",
    "input_hash": "sha256:...",
    "signature": "sha256:...",
    "checks": {
      "acl_compliance": "pass",
      "version_consistency": "pass",
      "token_budget": "pass (62.5%)",
      "freshness": "pass",
      "de_instructionalized": "pass"
    }
  }
}
```

### 节点关联与展示

#### 在整体流程中的位置

```
ACT-03-04-02 开发包切片 → ACT-03-04-03 Context 构建 → ACT-03-04-04 实现草案
                                ↓
                        ContextBundle（每个包一个）
```

---
