# test 环境 skills（E2E 加速用）

> E2E 默认用本环境 skill 跑（`spk-delivery.skill.default-env=test`）。完整一套 7 个 stage skill：
> concept / plan / develop / verify / launch / tr-gate(lifecycle) / verifier，与 default 对齐。
>
> **简化策略（加速 E2E）**：每个 skill 只给产物 schema 骨架 + 简单生成指令，让 LLM **快速生成符合格式的产物**，
> 字段填占位值即可，**不做业务深加工**——不查外部数据、不做深度推理。LLM 收到后直接出结果，加速端到端。
>
> 真实业务方法论见 `../default/<skill-name>/SKILL.md`（完整一套保留不动）。
>
> - fast-mode（E2E 默认）：桩不调 LLM，本 skill 保证 skillPath 解析 + inject 注入机制全 stage 命中。
> - 真实 LLM 模式（fast-mode=false）：本 skill 简化指令让 LLM 快速生成符合三段格式产物（verifier 走 JSON verdict）。
