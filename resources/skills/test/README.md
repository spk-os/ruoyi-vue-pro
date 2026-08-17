# test 环境 skills（E2E 测试用）

> E2E 测试（e2e-test profile + fast-mode）使用本环境 skill。skill 内容为简化桩，
> 保证 skillPath 解析 + NativeAiAdapter.injectSkill 注入机制真实工作即可，
> 不依赖外部数据源（MRS/VOC/web-search 在测试环境不可用）。
>
> 真实 skill 方法论见 `../default/<skill-name>/SKILL.md`。本环境 skill 可引用 default
> 内容的精简版（去除外部 MCP 依赖，保留方法论骨架 + JSON Schema + 验收标准）。
>
> 跑通机制后，可用 fast-mode 验证 skill 注入路径正确；切 default 环境验证真实 LLM 执行。
