-- =====================================================================
-- SPK-OS Cortext-IPD Activity 定义初始化（P1 + 真实 skill 绑定）
-- 10 个 Activity 与 spk-ipd-flow.json 的 flowable_node_id 一一对应，
-- 保证流程部署后 HTTP 触发器命中 activity_def（铁律：无 def 不得执行）。
-- 每个 Activity 真实 prompt + model_capabilities（与 Lead 的 capability_tags 全匹配）+
-- 真实 skill 绑定（skills JSON 数组，route resolveSkill 取首元素，resolveSkillPath 三级回退）+
-- output_template_ref（产物模板引用，对齐 output_artifact_type）+ acceptance_criteria
-- （验收规则 JSON，SpkVerifierService Layer 1/2 校验用，schemaRef 指向 verifier-scripts/<ref>/）。
-- 幂等：先按 activity_id 清除本批，再用 nextval 重建。
-- tenant_id=1：def 走租户过滤（DO extends BaseDO 无 @TenantIgnore），
--   psql 直插须显式写 1，否则运行时 tenant=1 查不到 → "Activity 定义不存在"。
-- 概念阶段两活动对齐设计文档 04-01：ACT-03-02-01=机会信号聚合(OpportunitySignalSet)、
-- ACT-03-02-02=客户需求深度分析(CustomerNeedBrief，承载 ```requirements 块供 Plane 录入)。
-- =====================================================================

DELETE FROM "spk_ipd_activity_def" WHERE "activity_id" IN (
  'ACT-03-02-01','ACT-03-02-02','ACT-03-03-01','ACT-03-03-02',
  'ACT-03-04-01','ACT-03-04-02','ACT-03-05-01','ACT-03-05-02',
  'ACT-03-06-01','ACT-03-07-01'
);

INSERT INTO "spk_ipd_activity_def"
("id","activity_id","version","name","stage","lead_agent_code","execution_location",
"use_worker_agent","use_independent_verifier","verifier_type","model_capabilities",
"skills","env_requirements","output_artifact_type","output_template_ref","acceptance_criteria",
"flowable_node_id","prompt_template","status","tenant_id")
VALUES
(nextval('spk_ipd_activity_def_seq'),'ACT-03-02-01','1.0.0','机会信号聚合','concept','lead-req-insight','task_system',0,0,NULL,'["req-analysis"]',
 '["ipd-concept-signal-aggregation"]',NULL,'opportunity-signal-set','opportunity-signal-set','{"schemaRef":"opportunity-signal-set"}','n_concept_1',
 '你是 IPD 概念阶段机会信号聚合 Lead（MarketInsight Lead）。基于项目立项输入，从 MRS 内部市场报告、VOC 客户之声、竞品/行业公开数据三类来源并行采集机会信号，Lead 聚合去重聚类，质量标注后输出 OpportunitySignalSet。\n\n【输出格式约束（必须严格遵循）】\n请按以下 JSON 结构输出 OpportunitySignalSet（只输出 JSON，不要额外解释）：\n{\n  "signal_set_id": "oss-<日期>-<序号>",\n  "version": "1.0.0",\n  "created_at": "<ISO 时间>",\n  "created_by": "agent-market-insight-001",\n  "statistics": {"total_signals": <int>, "mrs_count": <int>, "voc_count": <int>, "external_count": <int>, "clusters_count": <int>, "needs_review_count": <int>},\n  "signals": [{"signal_id":"sig-001","source_type":"mrs|voc|external","source_uri":"<来源URI>","locator":"<定位>","captured_at":"<ISO>","license":"<许可>","content_hash":"sha256:<hex>","confidence":<0~1>,"needs_review":<bool>,"cluster_id":"<簇id>","summary":"<摘要>"}],\n  "clusters": [{"cluster_id":"cls-001","topic":"<主题>","signal_count":<int>,"trend":"rising|stable|falling"}]\n}\n每条信号必须带 source_uri/locator/captured_at/license/content_hash/confidence；confidence<0.6 必须 needs_review=true。示例项目=智能家居中控。','active',1),

(nextval('spk_ipd_activity_def_seq'),'ACT-03-02-02','1.0.0','客户需求深度分析','concept','lead-concept-options','task_system',0,1,'TR','["concept-design"]',
 '["ipd-concept-voc-analysis"]',NULL,'customer-need-brief','customer-need-brief','{"schemaRef":"customer-need-brief"}','n_concept_2',
 '你是 IPD 概念阶段客户需求深度分析 Lead（MarketInsight Lead）。从客户访谈记录、工单系统、成功/失败案例三类来源并行采集客户原声，Lead 提取结构化需求 + 反例搜索验证 + 主题聚类，输出 CustomerNeedBrief。每需求可追溯到真实客户原话，反例搜索防臆造。\n\n【输出格式约束（必须严格遵循）】\n请按以下 JSON 结构输出 CustomerNeedBrief（只输出 JSON）：\n{\n  "need_brief_id":"cnb-<日期>-<序号>","version":"1.0.0","created_at":"<ISO>","created_by":"agent-market-insight-001",\n  "statistics":{"total_needs":<int>,"high_priority":<int>,"counter_evidence_count":<int>,"needs_review_count":<int>},\n  "needs":[{"need_id":"nd-001","scenario":"<场景>","priority":"P0|P1|P2","customer_quote":"<客户原话一字不差>","customer_id_hash":"<脱敏哈希>","counter_evidence":{"source":"<反例来源>","summary":"<反例摘要>"},"confidence":<0~1>,"needs_review":<bool>,"cluster_id":"<簇id>","summary":"<摘要>"}],\n  "clusters":[{"cluster_id":"cls-001","topic":"<主题>","need_count":<int>,"trend":"rising|stable|falling","representative_quote":"<代表原话>"}],\n  "sample_limitation":"<样本量与盲区声明>"\n}\n每需求必须带 customer_quote/customer_id_hash；confidence>=0.6 须有 counter_evidence.source；confidence<0.6 须 needs_review=true。\n\n【product 模式必填】报告末尾必须以如下 fenced code block 输出 IR 需求清单（供后端录入 Plane，缺块则跳过录入不报错）：\n```requirements\n[{"key":"IR-1","level":"IR","name":"需求名(<=40字)","description":"目标用户/场景/验收标准简述"}]\n```\n每条 IR 一行，key 用 IR-N 连续编号，level 固定 IR，name 精炼，description 含验收标准。','active',1),

(nextval('spk_ipd_activity_def_seq'),'ACT-03-03-01','1.0.0','项目计划','plan','lead-proj-plan','task_system',0,0,NULL,'["proj-planning"]',
 '["ipd-plan-project-planning"]',NULL,'integrated-project-plan','integrated-project-plan',NULL,'n_plan_1',
 '你是 IPD 计划阶段项目计划 Lead。基于已选定的概念方案，制定端到端项目计划：WBS、里程碑（CDCP/PDCP/ADCP/GA/LDCP）、资源矩阵、关键路径与风险登记，输出《集成项目计划》IntegratedProjectPlan。','active',1),

(nextval('spk_ipd_activity_def_seq'),'ACT-03-03-02','1.0.0','架构设计','plan','lead-arch-design','task_system',0,1,'TR','["architecture"]',
 '["ipd-plan-architecture-design"]',NULL,'architecture-description','architecture-description',NULL,'n_plan_2',
 '你是 IPD 计划阶段架构设计 Lead。基于概念方案与项目计划，设计系统总体架构（C4 四层：Context/Container/Component/Code + 关键 ADR 决策记录 + 接口契约 + 数据模型 + 非功能性保障），输出《架构描述候选》ArchitectureDescriptionCandidate。','active',1),

(nextval('spk_ipd_activity_def_seq'),'ACT-03-04-01','1.0.0','详细设计','develop','lead-detail-design','task_system',0,1,'TR','["detail-design"]',
 '["ipd-dev-implementation"]',NULL,'implementation-draft','implementation-draft',NULL,'n_dev_1',
 '你是 IPD 开发阶段实现草案 Lead。基于架构设计与 ContextBundle，对关键模块做详细设计：模块拆分、类/接口定义、时序图、数据库表结构、单元测试用例，生成可编译运行的核心代码骨架（含必要注释与 README），列出 TODO 与遗留风险，输出《实现草案》ImplementationDraft。','active',1),

(nextval('spk_ipd_activity_def_seq'),'ACT-03-04-02','1.0.0','编码实现','develop','lead-coding','task_system',0,1,'SEC','["coding"]',
 '["ipd-dev-unit-test"]',NULL,'code-package','code-package',NULL,'n_dev_2',
 '你是 IPD 开发阶段单元/组件验证 Lead。基于实现草案，跑单元测试与组件测试，失败归因到根因（非仅重跑），记录通过率与覆盖率，输出《代码包与单元验证证据》UnitComponentEvidence。','active',1),

(nextval('spk_ipd_activity_def_seq'),'ACT-03-05-01','1.0.0','集成验证','qualify','lead-integration-verify','task_system',0,1,'TR','["integration-test"]',
 '["ipd-verify-test-design"]',NULL,'integration-test-report','multi-layer-test-design',NULL,'n_qual_1',
 '你是 IPD 验证阶段多层测试设计 Lead。设计集成测试用例矩阵（接口/链路/异常/边界/性能），建立需求-测试追溯（RTM 无遗漏），执行并记录通过率与缺陷，给出集成验证结论，输出《集成验证报告》。','active',1),

(nextval('spk_ipd_activity_def_seq'),'ACT-03-05-02','1.0.0','Beta 验证','qualify','lead-beta-verify','task_system',0,1,'RE','["beta-test"]',
 '["ipd-verify-execute"]',NULL,'beta-test-report','test-execution-evidence',NULL,'n_qual_5',
 '你是 IPD 验证阶段执行采证 Lead。基于集成验证报告，设计 Beta 用户场景与验收脚本，在可复现环境执行并采证（日志/截图/时间戳+环境hash），记录用户反馈与可用性指标，给出 GA 就绪度评估，输出《Beta 验证报告》。','active',1),

(nextval('spk_ipd_activity_def_seq'),'ACT-03-06-01','1.0.0','上市发布','launch','lead-launch','task_system',0,0,NULL,'["go-to-market"]',
 '["ipd-release-rollout"]',NULL,'launch-plan','progressive-rollout-record',NULL,'n_launch_1',
 '你是 IPD 发布阶段渐进发布 Lead。基于 Beta 验证结论，制定上市计划（渠道、定价、营销、支持就绪、回滚预案）与渐进发布策略（canary→10%→50%→100%，每阶段健康门+自动回滚），输出《上市发布计划》。','active',1),

(nextval('spk_ipd_activity_def_seq'),'ACT-03-07-01','1.0.0','运营反馈','lifecycle','lead-ops-feedback','task_system',0,0,NULL,'["ops-feedback"]',
 '["ipd-lifecycle-signal"]',NULL,'ops-feedback-report','lifecycle-signal-set',NULL,'n_life_1',
 '你是 IPD 生命周期阶段信号采集 Lead。基于上市后运营数据（活跃/故障/NPS/性能），采集生命周期信号（带来源+时间戳），聚类分析改进项与下一版本需求，输出《运营反馈报告》并触发 LDCP。','active',1);
