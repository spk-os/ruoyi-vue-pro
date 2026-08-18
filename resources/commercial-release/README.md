# commercial-release · Agent 全产品软件 IPD Skill 包

本包支撑一个商业软件产品从机会发现到退市归档的完整交付，不是 50 份规则摘要。入口采用三级编排：

1. `spk-ipd-commercial-release`：六阶段全产品编排和 Product Delivery Ledger。
2. `spk-ipd-01-concept` 至 `spk-ipd-06-lifecycle`：阶段依赖、并发、门禁、恢复和移交。
3. `spk-ipd-<阶段号>-<阶段>-<活动号>-<动作>`：50 个 Activity 的真实执行入口。

## 六阶段入口

| 阶段 | 编排 Skill | Activity 数 | 阶段出口 |
|---|---|---:|---|
| 01 概念 | `spk-ipd-01-concept` | 8 | TR1/CDCP |
| 02 计划 | `spk-ipd-02-plan` | 6 | TR2/PDCP |
| 03 开发 | `spk-ipd-03-develop` | 11 | TR3/TR4 |
| 04 验证 | `spk-ipd-04-verify` | 9 | TR5 |
| 05 发布 | `spk-ipd-05-launch` | 6 | TR6/GA/稳定观察 |
| 06 生命周期 | `spk-ipd-06-lifecycle` | 10 | LDCP/EOL/迁移/归档 |

## 每个 Activity Skill 有什么

每个编号 Activity 目录包含：

- `SKILL.md`：触发条件、读取顺序、输入就绪、执行状态、完成定义、失败和移交。
- `references/execution-spec.md`：针对该活动的领域算法、工具时序、输出、验收、重试、回滚和升级。
- `references/source-design.md`：原设计中该 Activity 的逐行精确镜像，保留输入资源表、工具策略、JSON/Artifact Schema 和上下游关系。
- `references/examples.md`：正常、缺输入、证据冲突、工具失败、人工边界 5 类可复演案例。
- `../templates/<stage>/<ACT-ID>-<Artifact>.template.md`：可直接复制填写的正式交付文档模板。

`references/activity-catalog.json` 是唯一机器路由目录，记录阶段/活动编号、Skill、正式模板、人工责任、Verifier、原文行号和 SHA-256。

## 运行方式

完整产品从总入口开始；流程已经定位到某一阶段时调用阶段 Skill；只有流程已经明确给出 Activity ID 时才直接调用 Activity Skill。

Agent 必须先冻结输入清单，再执行领域算法和真实工具，随后填写正式模板、生成证据与回执并运行：

```bash
python3 scripts/validate_delivery.py /path/to/activity-result.json
```

发布或修改 Skill 包后运行：

```bash
./scripts/validate_all.sh
```

## 人与 Agent 的边界

Agent 可以采集、分析、实现、测试、复算、质询、组包和记录，但不能写入或代签 DCP、TR、CCB、GA、LDCP 以及原设计要求的 Owner 签署状态。catalog 的 `humanResponsibility` 和 `verifierPolicy` 直接从原始 Activity 设计提取；缺少真实签名或独立回执时不得 COMPLETE。

## 目录说明

- `references/`：catalog、全包执行契约、AI-Native 控制和支撑活动路由。
- `templates/<stage>/`：50 个可填写的实体模板；`*-deliverables.md` 只是导航。
- `schemas/`：ActivityResult、VerificationReceipt、HumanApprovalRecord 数据契约。
- `scripts/`：包级与单次交付验证器。
- `tests/`：编号、来源镜像、执行能力、模板、证据、人审与防篡改契约测试。

原设计来源：`/root/share/SPK-OS/24_houre_anly/IPD/05-kimi-k3-design/04-六阶段每阶段-具体设计与Activity执行配置表/`。原 Activity 编号和控制目标保持不变，AI-Native 改造增强执行、验证、恢复和协作能力。
