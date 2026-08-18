## ACT-03-04-04 实现草案

### 基础信息

| 字段 | 内容 |
|---|---|
| **Activity ID** | ACT-03-04-04 |
| **Activity 名称** | 实现草案 |
| **所属阶段** | 开发 |
| **Lead Agent 角色** | Development Orchestrator |
| **执行位置** | task_system |
| **是否使用 Worker Agent** | ✓（Implementation Agent + Test Agent 协作） |
| **是否需要 Independent Verifier** | — |
| **所需模型能力** | STRUCTURED_DRAFTING + CODE_OR_FORMULA_ASSISTANCE |
| **所需 Skill** | ipd-dev-implementation / spk-code-generator / spk-test-generator |
| **所需 Tool/MCP** | mcp-git-query / mcp-codegraph-explore / mcp-filesystem-read |
| **所需 Knowledge** | KN-IPD-CODING-STANDARDS-022 / KN-PROJECT-CODE-CONVENTIONS-023 |
| **所需 Information** | INFO-CONTEXT-BUNDLE（当前包的 Context Pack）/ INFO-CODE-REPO |
| **可读取的 Memory 范围** | project:spk-os-ipd, agent_role:development, capability:code_or_formula_assistance |
| **人工责任** | Developer 承担作者责任，审查并修改 Agent 产出 |
| **失败处理** | 生成语法失败允许一次定向修正；仍失败降级人工实现；敏感文件/大范围改写升级 Repo Owner；回滚沙箱分支与临时制品 |
| **对应 Flowable 节点** | task_implementation (ipd-development.bpmn) |
| **证据来源** | RunReceipt + GitDiff + ArtifactManifest |

### 资源与工具详情

#### 输入资源

| 资源 | 来源 | 获取方式 | 质量要求 |
|---|---|---|---|
| ContextBundle | ACT-03-04-03 产物 | 本地文件系统 | Developer 确认 |
| 代码仓库 | Git | mcp-git-query | 含目录结构 |
| 编码规范 | Knowledge 库 | mcp-filesystem-read | 项目编码规范 |
| CBB 使用指南 | CBB Registry | mcp-spk-os-cbb-query.get() | 含 API 示例 |

#### 工具调用顺序与效率策略

```
Step 1: 创建隔离分支
├─ 调用 mcp-git-query.create_branch(base="main", name="feature/dp-001-{timestamp}")
├─ 配置沙箱环境（仅允许修改 allowlist 目录）
└─ 输出：隔离分支

Step 2: Implementation Agent 生成代码草案
├─ 读取 ContextBundle 的六层内容
├─ 调用 spk-code-generator.generate(context_bundle, task)
│  ├─ 生成最小差异（只修改必需的文件）
│  ├─ 遵循编码规范和项目约定
│  ├─ 使用 CBB 的正确 API（从 CBB 文档获取）
│  ├─ 生成代码注释和文档
│  ├─ 记录生成 provenance（模型 ID、Prompt hash、Context hash）
│  └─ 输出：代码补丁（diff）
├─ 确定性预检：
│  ├─ formatter 自动格式化
│  ├─ linter 检查编码规范
│  ├─ compiler 检查语法
│  └─ 静态规则检查（不允许修改保护文件）
└─ 输出：代码草案（通过预检）

Step 3: Test Agent 生成测试草案
├─ 调用 spk-test-generator.generate(code_draft, requirements, test_strategy)
│  ├─ 边界值测试（空输入、极大输入、特殊字符）
│  ├─ 等价类测试（正常路径、异常路径）
│  ├─ 变异测试（修改关键逻辑，验证测试能否检测）
│  ├─ 契约测试（验证接口契约 compliance）
│  └─ 输出：测试代码
└─ 输出：测试草案

Step 4: Developer 审查与修改
├─ Developer 审查 Agent 产出的代码和测试
│  ├─ 检查逻辑正确性
│  ├─ 检查边界条件覆盖
│  ├─ 检查安全实践
│  ├─ 修改/补充不满意的部分
│  └─ 承担作者责任
├─ Developer 提交到隔离分支
├─ 创建 PR，关联开发包 ID 和需求 ID
│  ├─ PR 标题含 DP-001
│  ├─ PR 描述含 requirement_ids
│  └─ PR 关联 work_item_id
└─ 输出：ImplementationDraft（PR 草稿）
```

#### 效率优化策略

| 策略 | 说明 | 预期效果 |
|---|---|---|
| 最小差异原则 | Agent 只修改必需的文件 | 减少审查负担和冲突风险 |
| 确定性预检 | formatter/linter/compiler 自动修复 | 减少人工修复格式问题 |
| 沙箱隔离 | Agent 只能修改 allowlist 目录 | 防止意外修改 |
| Provenance 记录 | 记录模型 ID、Prompt hash、Context hash | 支持审计追溯 |

### 输出 Artifact 模板

#### Artifact 名称
`ImplementationDraft`

#### 文件结构
```
implementation_draft/
├── manifest.yaml
├── draft.json                    # 主数据文件
├── diff/                         # 代码差异
│   └── changes.patch
├── tests/                        # 测试代码
│   └── test_changes.patch
├── provenance/                   # 生成溯源
│   └── generation_receipt.json
└── README.md
```

#### draft.json Schema

```json
{
  "draft_id": "impl-20260818-dp001-001",
  "version": "1.0.0",
  "created_at": "2026-08-18T15:00:00Z",
  "created_by": "agent-implementation-001",
  "author_confirmed_by": {
    "user_id": "user-dev-001",
    "role": "Developer",
    "confirmed_at": "2026-08-18T16:00:00Z"
  },
  "dev_packet_ref": "DP-001",
  "context_bundle_ref": "ctx-20260818-dp001-001",
  "branch": "feature/dp-001-20260818",
  "pr_url": "https://gitea.example.com/aurora/aurora/pulls/42",
  "changes": {
    "files_modified": 5,
    "files_added": 3,
    "lines_added": 820,
    "lines_removed": 15,
    "allowed_files_only": true
  },
  "tests": {
    "test_files_added": 2,
    "test_cases_added": 45,
    "test_types": ["unit", "boundary", "contract"]
  },
  "provenance": {
    "model_id": "claude-sonnet-4",
    "model_version": "2026-07-15",
    "prompt_hash": "sha256:...",
    "context_bundle_hash": "sha256:...",
    "generation_timestamp": "2026-08-18T15:10:00Z",
    "human_modifications": "Developer added error handling for encoding detection failure"
  },
  "pre_checks": {
    "formatter": "pass",
    "linter": "pass",
    "compiler": "pass",
    "static_rules": "pass"
  },
  "known_risks": [
    "tree-sitter 对某些语言的语法支持可能不完整",
    "大文件 diff 解析的性能需要性能测试验证"
  ],
  "requirement_links": ["REQ-001", "REQ-002", "REQ-003", "REQ-004", "REQ-005"]
}
```

### 节点关联与展示

#### 在整体流程中的位置

```
ACT-03-04-03 Context 构建 → ACT-03-04-04 实现草案 → ACT-03-04-05 单元/组件验证
                                ↓
                        ImplementationDraft（PR 草稿）
                                ↓
                        ACT-03-04-06 同行审查
```

---
