---
name: spk-ipd-verify
description: IPD 验证阶段 test 简化 skill（E2E 加速）。让 LLM 快速生成符合 schema 的产物骨架，不做业务深加工。真实业务方法论见 default/spk-ipd-verify。
author: SPK-OS Team
version: 1.0.0
tags: [ipd, verify, qualify, test, fast-stub]
---

# spk-ipd-verify（test 简化版）

> E2E 加速用：**直接快速生成符合下方 schema 的产物骨架，无需真实业务加工**——
> 字段填合理占位值即可，保证格式合规 + 可落库 + 可追溯。真实业务方法论见 default/spk-ipd-verify。

## 产物 schema（照填，勿空字段）

```json
{
  "artifact_type": "TestExecutionEvidence",
  "version": "1.0.0",
  "stage": "verify",
  "summary": "验证阶段产物（test 占位）",
  "content_markdown": "# 测试执行证据\n\n## 通过率\n- 100%\n## 覆盖率\n- 占位\n## 证据\n- 占位日志/截图",
  "evidence": [{"source": "占位源", "locator": "占位定位", "hash": "sha256:placeholder"}]
}
```

## 生成指令（简单直接，快速出结果）

1. 照 schema 生成产物，字段填合理占位值（勿空/null）
2. content_markdown 用 Markdown 组织（标题 + 简短章节即可，无需长文）
3. **不做**外部数据查询/业务分析/深度推理——快速生成
4. 输出遵循 adapter 三段格式（<<<DOCUMENT>>>/<SUMMARY>>/<CONCLUSION>>>），CONCLUSION 取 PASS
