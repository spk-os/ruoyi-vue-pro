-- =====================================================================
-- SPK-OS Cortext-IPD Activity P3 扩展（三 flowType 14 个新 Activity）
-- 62dabe2e92 三 flowType 路由三真实 BPM 流程后，BPMN body 用 ACT-FULL-*/INCR-*/ISSUE-*
-- 新命名，但 def 表旧 28 行仍是 ACT-03-xx-xx → route 0 匹配 → "Activity 定义不存在"
-- → receiveTask 永卡 → FlowRun 永卡 RUNNING/concept。本文件补 14 行对齐 BPMN 命名。
-- flowable_node_id 与 spk-ipd-flow-{full,increment,issue}.json 的 serviceTask id 一一对应。
-- 幂等：先按 activity_id 清除本批 14 个，再重建。tenant_id=1（def 走租户过滤）。
-- =====================================================================

DELETE FROM "spk_ipd_activity_def" WHERE "activity_id" IN (
  'ACT-FULL-CONCEPT-1','ACT-FULL-PLAN-1','ACT-FULL-DEV-1','ACT-FULL-QUAL-1','ACT-FULL-LAUNCH-1','ACT-FULL-LIFE-1',
  'ACT-INCR-PLAN-1','ACT-INCR-DEV-1','ACT-INCR-QUAL-1','ACT-INCR-LAUNCH-1',
  'ACT-ISSUE-RC-1','ACT-ISSUE-FIX-1','ACT-ISSUE-VERIFY-1','ACT-ISSUE-CLOSE-1'
);

INSERT INTO "spk_ipd_activity_def"
("id","activity_id","version","name","stage","lead_agent_code","execution_location",
"use_worker_agent","use_independent_verifier","verifier_type","model_capabilities",
"output_artifact_type","flowable_node_id","prompt_template","status","tenant_id") VALUES

-- ========== FULL_RELEASE 6 阶段（每阶段 1 个代表 activity）==========
(nextval('spk_ipd_activity_def_seq'),'ACT-FULL-CONCEPT-1','1.0.0','概念阶段·需求洞察','concept','lead-req-insight','task_system',0,1,'TR','["req-analysis"]','req-insight-report','n_concept_t1',
 '你是 IPD 完整发布概念阶段需求洞察 Lead。基于项目立项输入，识别目标用户、核心痛点、关键场景与隐性需求，输出结构化《需求洞察报告》：用户画像、痛点排序、场景清单、需求条目（含优先级与验收标准）。','active',1),
(nextval('spk_ipd_activity_def_seq'),'ACT-FULL-PLAN-1','1.0.0','计划阶段·范围与进度','plan','lead-proj-plan','task_system',0,1,'TR','["proj-planning"]','project-plan','n_plan_t1',
 '你是 IPD 完整发布计划阶段项目计划 Lead。基于已选定的概念方案，制定端到端项目计划：WBS、里程碑（CDCP/PDCP/ADCP/GA/LDCP）、资源矩阵、关键路径与风险登记，输出《项目计划书》。','active',1),
(nextval('spk_ipd_activity_def_seq'),'ACT-FULL-DEV-1','1.0.0','开发阶段·实现与单元测试','develop','lead-coding','task_system',0,1,'SEC','["coding"]','code-package','n_dev_t1',
 '你是 IPD 完整发布开发阶段编码实现 Lead。基于详细设计，生成可编译运行的核心代码骨架（含必要注释与 README），并完成单元测试覆盖关键路径，列出 TODO 与遗留风险，输出《代码包》。','active',1),
(nextval('spk_ipd_activity_def_seq'),'ACT-FULL-QUAL-1','1.0.0','验证阶段·集成与回归','qualify','lead-integration-verify','task_system',0,1,'TR','["integration-test"]','system-test-report','n_qual_t1',
 '你是 IPD 完整发布验证阶段集成验证 Lead。设计集成测试用例矩阵（接口/链路/异常），执行并记录通过率与缺陷，完成系统级回归验证，给出 GA 就绪度评估，输出《系统测试报告》。','active',1),
(nextval('spk_ipd_activity_def_seq'),'ACT-FULL-LAUNCH-1','1.0.0','发布阶段·部署与监控','launch','lead-launch','task_system',0,1,'TR','["go-to-market"]','release-package','n_launch_t1',
 '你是 IPD 完整发布发布阶段上市发布 Lead。基于系统测试结论，完成发布包构建、部署上线与上线监控就绪，输出《发布包》与部署记录。','active',1),
(nextval('spk_ipd_activity_def_seq'),'ACT-FULL-LIFE-1','1.0.0','生命周期·移交运维','lifecycle','lead-ops-feedback','task_system',0,1,'TR','["ops-feedback"]','ops-monitoring-report','n_life_t1',
 '你是 IPD 完整发布生命周期阶段运营反馈 Lead。基于上线后运营数据（活跃/故障/NPS），完成移交运维、建立监控基线并识别改进项，输出《运营监控报告》并触发 LDCP。','active',1),

-- ========== INCREMENT_RELEASE 4 阶段（增量跳过 concept/lifecycle）==========
(nextval('spk_ipd_activity_def_seq'),'ACT-INCR-PLAN-1','1.0.0','计划阶段·范围裁剪','plan','lead-proj-plan','task_system',0,0,NULL,'["proj-planning"]','project-plan','n_plan_t1',
 '你是 IPD 增量发布计划阶段项目计划 Lead。基于基线版本与增量需求，制定增量范围裁剪与排期计划：增量 WBS、回归范围、资源矩阵与风险登记，输出《增量项目计划书》。','active',1),
(nextval('spk_ipd_activity_def_seq'),'ACT-INCR-DEV-1','1.0.0','开发阶段·实现','develop','lead-coding','task_system',0,1,'SEC','["coding"]','code-package','n_dev_t1',
 '你是 IPD 增量发布开发阶段编码实现 Lead。基于增量设计，生成增量代码改动（含注释与 README），完成单元测试覆盖改动路径，列出 TODO 与遗留风险，输出《增量代码包》。','active',1),
(nextval('spk_ipd_activity_def_seq'),'ACT-INCR-QUAL-1','1.0.0','验证阶段·回归','qualify','lead-integration-verify','task_system',0,1,'TR','["integration-test"]','system-test-report','n_qual_t1',
 '你是 IPD 增量发布验证阶段回归验证 Lead。设计增量回归测试用例矩阵，执行并记录通过率与缺陷，给出增量 GA 就绪度评估，输出《增量系统测试报告》。','active',1),
(nextval('spk_ipd_activity_def_seq'),'ACT-INCR-LAUNCH-1','1.0.0','发布阶段·部署','launch','lead-launch','task_system',0,0,NULL,'["go-to-market"]','release-package','n_launch_t1',
 '你是 IPD 增量发布发布阶段上市发布 Lead。基于增量测试结论，完成增量发布包构建、灰度部署与监控就绪，输出《增量发布包》与部署记录。','active',1),

-- ========== ISSUE_RESOLUTION 4 步（根因→修复→验证→关闭）==========
(nextval('spk_ipd_activity_def_seq'),'ACT-ISSUE-RC-1','1.0.0','根因分析·定位','lifecycle','lead-ops-feedback','task_system',0,0,NULL,'["ops-feedback"]','ops-feedback-report','n_rc_t1',
 '你是 IPD 问题解决根因分析 Lead。基于问题现象与日志，定位根因（代码/配置/环境/数据），评估影响范围与复发概率，给出处置建议，输出《根因分析报告》。','active',1),
(nextval('spk_ipd_activity_def_seq'),'ACT-ISSUE-FIX-1','1.0.0','修复开发·补丁','develop','lead-coding','task_system',0,1,'SEC','["coding"]','code-package','n_fix_t1',
 '你是 IPD 问题解决修复开发 Lead。基于根因分析，生成修复补丁代码（含注释与回归用例），列出副作用与遗留风险，输出《修复代码包》。','active',1),
(nextval('spk_ipd_activity_def_seq'),'ACT-ISSUE-VERIFY-1','1.0.0','验证·复现确认','qualify','lead-integration-verify','task_system',0,1,'TR','["integration-test"]','integration-test-report','n_verify_t1',
 '你是 IPD 问题解决验证 Lead。基于修复补丁，复现原问题确认已修复，执行相关回归用例确认无新增缺陷，给出验证结论，输出《问题验证报告》。','active',1),
(nextval('spk_ipd_activity_def_seq'),'ACT-ISSUE-CLOSE-1','1.0.0','关闭·归档','launch','lead-pm','task_system',0,0,NULL,'["pm-orchestration"]','ga-signoff','n_close_t1',
 '你是 IPD 问题解决关闭归档 Lead。基于验证结论，完成问题关闭签署、归档知识库与改进项跟踪，输出《问题关闭签署》。','active',1);
