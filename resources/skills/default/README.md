# skills 目录 — IPD 流程真实执行能力载体

> 本目录是 IPD 流程「真正配置 skill + 输出结果校验机制」的物理载体。
> SpkTaskRouterService.route 通过 `skills-root` 配置指向本目录，按 `env/skillName/SKILL.md`
> 解析 skill 路径，由 NativeAiAdapter.injectSkillInstruction 把 SKILL.md 全文前置进 LLM prompt，
> 让 agent 真正遵循 skill 方法论执行任务（而非空壳派发）。

## 目录约定

```
resources/skills/
├── default/                    # 默认环境（开发/调试，真实 LLM 全量执行）
│   ├── spk-ipd-concept/        # 概念阶段回退 skill（stage→skill 映射兜底）
│   ├── ipd-concept-signal-aggregation/   # ACT-03-02-01 机会信号聚合（真实 skill 样板）
│   ├── spk-ipd-verifier/       # 三层校验 skill（结构 schema + 验收标准 + LLM 语义复核）
│   └── ...
├── test/                       # 测试环境（E2E 用，简化桩 skill，fast-mode 跑通机制）
├── commercial-release/         # 商业发布环境（严格 skill + 完整校验脚本，生产交付）
├── prototype-release/          # 原型发布环境（轻量 skill + 基础校验，快速验证）
└── README.md                   # 本文件
```

## env 维度（与 mode 维度正交）

- **env**（`spk_env_profile` 流程变量）：default / test / commercial-release / prototype-release
  — 决定 skill 目录选哪个环境的 SKILL.md。
- **mode**（`spk_mode` 流程变量）：test / product
  — 决定 prompt 装配策略（product 真实交付 / test 轻量化）与 Gitea 分支策略。
- 两维度独立：同一 product 流程可选不同 env 的 skill；route 内 `resolveSkillEnv` 读 env，
  `readMode` 读 mode，互不复用。

## 新增 skill 流程

1. 在对应 env 目录下建 `<skill-name>/SKILL.md`（skill 名与 activity_def.skills JSON 数组首元素一致）。
2. SKILL.md 须含：方法论步骤、输入产物、输出 Artifact JSON Schema、验收标准。
3. commercial-release/prototype-release 环境的 skill 内容可从严/从轻，但目录结构与 default 对齐。
4. route 自动按 env + skillName 解析，无需改代码。

## 校验脚本

- 产物结构化 JSON Schema 校验脚本：`resources/verifier-scripts/<artifactType>/schema.json`
- 验收标准规则脚本：`resources/verifier-scripts/<artifactType>/acceptance.json`
- SpkVerifierService.verify 三层校验依次执行，结果综合为 verdict。
