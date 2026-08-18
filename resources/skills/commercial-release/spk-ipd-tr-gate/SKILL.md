---
name: spk-ipd-tr-gate
description: 商业发布环境技术评审与生命周期兼容回退 Skill，确保人审决定和独立证据复核。
version: 1.0.0
---

# TR / 生命周期兼容回退

若任务 `stage=lifecycle`，必须改用 `spk-ipd-06-lifecycle`，再按 Activity ID 路由编号 Skill；共享 TR Gate 只用于 TR1~TR6 的证据复核，不能作为生命周期阶段回退入口。

复核者必须与 producer 隔离，从原始证据重算关键指标，输出 PASS/FAIL/BLOCKED、逐项检查、缺口、风险与 evidenceIds。TR/DCP/CCB/GA/LDCP 的最终结论、签名和状态写入只能由授权人完成。

## 禁止完成

证据不可访问、哈希不一致、复核者与生产者同人、含占位符或缺人工签署记录时禁止完成。不得用 Agent 自评分替代独立验证，不得用会议纪要替代原始证据。
