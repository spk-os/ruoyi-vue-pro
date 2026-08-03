-- =====================================================================
-- SPK-OS Cortext-IPD Activity 定义初始化（P1）
-- 10 个 Activity 与 spk-ipd-flow.json 的 flowable_node_id 一一对应，
-- 保证流程部署后 HTTP 触发器命中 activity_def（铁律：无 def 不得执行）。
-- 每个 Activity 真实 prompt + model_capabilities（与 Lead 的 capability_tags 全匹配）。
-- 幂等：先按 activity_id 清除本批，再用 nextval 重建
-- =====================================================================

DELETE FROM "spk_ipd_activity_def" WHERE "activity_id" IN (
  'ACT-03-02-01','ACT-03-02-02','ACT-03-03-01','ACT-03-03-02',
  'ACT-03-04-01','ACT-03-04-02','ACT-03-05-01','ACT-03-05-02',
  'ACT-03-06-01','ACT-03-07-01'
);

INSERT INTO "spk_ipd_activity_def"
("id","activity_id","version","name","stage","lead_agent_code","execution_location",
"use_worker_agent","use_independent_verifier","verifier_type","model_capabilities",
"output_artifact_type","flowable_node_id","prompt_template","status","tenant_id")
VALUES
(nextval('spk_ipd_activity_def_seq'),'ACT-03-02-01','1.0.0','需求洞察','concept','lead-req-insight','task_system',0,0,NULL,'["req-analysis"]','req-insight-report','n_concept_1',
 '你是 IPD 概念阶段需求洞察 Lead。基于项目立项输入，识别目标用户、核心痛点、关键场景与隐性需求，输出结构化《需求洞察报告》：用户画像、痛点排序、场景清单、需求条目（含优先级与验收标准）。示例项目=智能家居中控。','active',0),

(nextval('spk_ipd_activity_def_seq'),'ACT-03-02-02','1.0.0','概念选项集','concept','lead-concept-options','task_system',0,1,'TR','["concept-design"]','concept-options-set','n_concept_2',
 '你是 IPD 概念阶段概念选项 Lead。基于需求洞察报告，生成 2~3 个差异化的产品概念方案（含价值主张、关键功能、技术路线、风险），对比优劣并给出推荐项，输出《概念选项集》。','active',0),

(nextval('spk_ipd_activity_def_seq'),'ACT-03-03-01','1.0.0','项目计划','plan','lead-proj-plan','task_system',0,0,NULL,'["proj-planning"]','project-plan','n_plan_1',
 '你是 IPD 计划阶段项目计划 Lead。基于已选定的概念方案，制定端到端项目计划：WBS、里程碑（CDCP/PDCP/ADCP/GA/LDCP）、资源矩阵、关键路径与风险登记，输出《项目计划书》。','active',0),

(nextval('spk_ipd_activity_def_seq'),'ACT-03-03-02','1.0.0','架构设计','plan','lead-arch-design','task_system',0,1,'TR','["architecture"]','architecture-design-doc','n_plan_2',
 '你是 IPD 计划阶段架构设计 Lead。基于概念方案与项目计划，设计系统总体架构（逻辑视图、部署视图、关键接口、数据模型、非功能性保障），输出《架构设计说明书》。','active',0),

(nextval('spk_ipd_activity_def_seq'),'ACT-03-04-01','1.0.0','详细设计','develop','lead-detail-design','task_system',0,1,'TR','["detail-design"]','detail-design-doc','n_dev_1',
 '你是 IPD 开发阶段详细设计 Lead。基于架构设计，对关键模块做详细设计：模块拆分、类/接口定义、时序图、数据库表结构、单元测试用例，输出《详细设计说明书》。','active',0),

(nextval('spk_ipd_activity_def_seq'),'ACT-03-04-02','1.0.0','编码实现','develop','lead-coding','task_system',0,1,'SEC','["coding"]','code-package','n_dev_2',
 '你是 IPD 开发阶段编码实现 Lead。基于详细设计，生成可编译运行的核心代码骨架（含必要注释与 README），列出 TODO 与遗留风险，输出《代码包》。','active',0),

(nextval('spk_ipd_activity_def_seq'),'ACT-03-05-01','1.0.0','集成验证','qualify','lead-integration-verify','task_system',0,1,'TR','["integration-test"]','integration-test-report','n_qual_1',
 '你是 IPD 验证阶段集成验证 Lead。设计集成测试用例矩阵（接口/链路/异常），执行并记录通过率与缺陷，给出集成验证结论，输出《集成验证报告》。','active',0),

(nextval('spk_ipd_activity_def_seq'),'ACT-03-05-02','1.0.0','Beta 验证','qualify','lead-beta-verify','task_system',0,1,'RE','["beta-test"]','beta-test-report','n_qual_2',
 '你是 IPD 验证阶段 Beta 验证 Lead。基于集成验证报告，设计 Beta 用户场景与验收脚本，记录用户反馈与可用性指标，给出 GA 就绪度评估，输出《Beta 验证报告》。','active',0),

(nextval('spk_ipd_activity_def_seq'),'ACT-03-06-01','1.0.0','上市发布','launch','lead-launch','task_system',0,0,NULL,'["go-to-market"]','launch-plan','n_launch_1',
 '你是 IPD 发布阶段上市发布 Lead。基于 Beta 验证结论，制定上市计划（渠道、定价、营销、支持就绪、回滚预案），输出《上市发布计划》。','active',0),

(nextval('spk_ipd_activity_def_seq'),'ACT-03-07-01','1.0.0','运营反馈','lifecycle','lead-ops-feedback','task_system',0,0,NULL,'["ops-feedback"]','ops-feedback-report','n_life_1',
 '你是 IPD 生命周期阶段运营反馈 Lead。基于上市后运营数据（活跃/故障/NPS），识别改进项与下一版本需求，输出《运营反馈报告》并触发 LDCP。','active',0);
