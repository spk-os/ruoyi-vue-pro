-- =====================================================================
-- SPK-OS Cortext-IPD Activity P2 扩展（18 个新 Activity，补齐 28 全量）
-- 既有 10 个见 spk_ipd_activity_def_init.sql；本文件只追加 18 个新 Activity。
-- flowable_node_id 与 spk-ipd-flow.json 新增节点一一对应。
-- 幂等：先按 activity_id 清除本批 18 个，再重建。
DELETE FROM "spk_ipd_activity_def" WHERE "activity_id" IN (
  'ACT-03-02-03','ACT-03-02-04','ACT-03-02-05',
  'ACT-03-03-03','ACT-03-03-04','ACT-03-03-05',
  'ACT-03-04-03','ACT-03-04-04','ACT-03-04-05','ACT-03-04-06',
  'ACT-03-05-03','ACT-03-05-04','ACT-03-05-05',
  'ACT-03-06-02','ACT-03-06-03','ACT-03-06-04',
  'ACT-03-07-02','ACT-03-07-03'
);

INSERT INTO "spk_ipd_activity_def"
("id","activity_id","version","name","stage","lead_agent_code","execution_location",
"use_worker_agent","use_independent_verifier","verifier_type","model_capabilities",
"output_artifact_type","flowable_node_id","prompt_template","status","tenant_id") VALUES

-- ========== 概念阶段新增 3 ==========
(nextval('spk_ipd_activity_def_seq'),'ACT-03-02-03','1.0.0','可行性分析','concept','lead-sys-arch','task_system',0,0,NULL,'["sys-architecture"]','feasibility-report','n_concept_3',
 '你是 IPD 概念阶段可行性分析 Lead。基于需求洞察与概念选项，从技术可行性、资源可行性、市场可行性与法规合规性四维评估每个概念方案，给出可行性结论与风险排序，输出《可行性分析报告》。','active',1),
(nextval('spk_ipd_activity_def_seq'),'ACT-03-02-04','1.0.0','商业案例','concept','lead-pm','task_system',0,0,NULL,'["pm-orchestration"]','business-case','n_concept_4',
 '你是 IPD 概念阶段商业案例 Lead。基于推荐概念方案，构建商业案例：市场规模与 TAM/SAM/SOM、竞品分析、收入预测、成本结构、ROI 与回收期、投资建议，输出《商业案例》。','active',1),
(nextval('spk_ipd_activity_def_seq'),'ACT-03-02-05','1.0.0','概念决策准备','concept','lead-concept-options','task_system',0,0,NULL,'["concept-design"]','concept-decision-brief','n_concept_5',
 '你是 IPD 概念阶段决策准备 Lead。汇总需求洞察、概念选项、可行性、商业案例为 CDCP 决策材料：一页纸决策摘要、推荐方案、关键风险与缓解措施、决策项清单，输出《概念决策简报》供 CDCP 评审。','active',1),

-- ========== 计划阶段新增 3 ==========
(nextval('spk_ipd_activity_def_seq'),'ACT-03-03-03','1.0.0','风险评估','plan','lead-pm','task_system',0,0,NULL,'["pm-orchestration"]','risk-assessment','n_plan_3',
 '你是 IPD 计划阶段风险评估 Lead。识别项目全生命周期的技术/进度/资源/外部风险，评估概率与影响，制定缓解与应急策略，输出风险登记册与《风险评估报告》。','active',1),
(nextval('spk_ipd_activity_def_seq'),'ACT-03-03-04','1.0.0','计划排期','plan','lead-proj-plan','task_system',0,0,NULL,'["proj-planning"]','schedule-plan','n_plan_4',
 '你是 IPD 计划阶段排期 Lead。基于 WBS 与里程碑（CDCP/PDCP/ADCP/GA/LDCP），用关键路径法排期，给出甘特图数据、资源直方图与关键路径标记，输出《计划排期表》。','active',1),
(nextval('spk_ipd_activity_def_seq'),'ACT-03-03-05','1.0.0','IP策略','plan','lead-arch-design','task_system',0,0,NULL,'["architecture"]','ip-strategy','n_plan_5',
 '你是 IPD 计划阶段 IP 策略 Lead。识别可申请专利点、商标、开源依赖与许可证合规风险，制定知识产权保护策略与开源治理方案，输出《知识产权策略书》。','active',1),

-- ========== 开发阶段新增 4 ==========
(nextval('spk_ipd_activity_def_seq'),'ACT-03-04-03','1.0.0','单元测试','develop','lead-coding','task_system',0,0,NULL,'["coding"]','unit-test-report','n_dev_3',
 '你是 IPD 开发阶段单元测试 Lead。基于详细设计与代码骨架，编写单元测试用例矩阵（含边界与异常），执行并记录覆盖率（语句/分支）与通过率，输出《单元测试报告》。','active',1),
(nextval('spk_ipd_activity_def_seq'),'ACT-03-04-04','1.0.0','代码评审','develop','lead-coding','task_system',0,1,'SEC','["coding"]','code-review-report','n_dev_4',
 '你是 IPD 开发阶段代码评审 Lead。对编码实现与单元测试做安全与质量评审：漏洞扫描、注入风险、鉴权绕过、代码异味、依赖漏洞，给出整改清单与严重度排序，输出《代码评审报告》。','active',1),
(nextval('spk_ipd_activity_def_seq'),'ACT-03-04-05','1.0.0','集成构建','develop','lead-coding','task_system',0,0,NULL,'["coding"]','integration-build','n_dev_5',
 '你是 IPD 开发阶段集成构建 Lead。配置 CI 流水线（构建/扫描/打包/制品归档），定义模块间接口契约，执行首次集成构建并记录产物哈希，输出《集成构建记录》。','active',1),
(nextval('spk_ipd_activity_def_seq'),'ACT-03-04-06','1.0.0','开发文档','develop','lead-detail-design','task_system',0,0,NULL,'["detail-design"]','dev-doc','n_dev_6',
 '你是 IPD 开发阶段文档 Lead。编写用户手册、API 文档、运维手册与发布说明草稿，确保文档与代码同步，输出《开发文档集》。','active',1),

-- ========== 验证阶段新增 3 ==========
(nextval('spk_ipd_activity_def_seq'),'ACT-03-05-03','1.0.0','系统测试','qualify','lead-integration-verify','task_system',0,1,'TR','["integration-test"]','system-test-report','n_qual_2',
 '你是 IPD 验证阶段系统测试 Lead。端到端系统测试：功能回归、兼容性、可恢复性，记录缺陷分级与通过率，给出系统测试结论，输出《系统测试报告》。','active',1),
(nextval('spk_ipd_activity_def_seq'),'ACT-03-05-04','1.0.0','性能测试','qualify','lead-integration-verify','task_system',0,0,NULL,'["integration-test"]','perf-test-report','n_qual_3',
 '你是 IPD 验证阶段性能测试 Lead。压测并发/吞吐/延迟/稳定性，识别瓶颈与容量上限，给出 SLA 达标结论，输出《性能测试报告》。','active',1),
(nextval('spk_ipd_activity_def_seq'),'ACT-03-05-05','1.0.0','安全测试','qualify','lead-coding','task_system',0,1,'SEC','["coding"]','security-test-report','n_qual_4',
 '你是 IPD 验证阶段安全测试 Lead。渗透测试、依赖漏洞复测、配置审计，给出风险等级与修复优先级，输出《安全测试报告》。','active',1),

-- ========== 发布阶段新增 3 ==========
(nextval('spk_ipd_activity_def_seq'),'ACT-03-06-02','1.0.0','发布包','launch','lead-launch','task_system',0,0,NULL,'["go-to-market"]','release-package','n_launch_2',
 '你是 IPD 发布阶段发布包 Lead。组装 GA 发布包：二进制/镜像、校验和、签名、变更说明、回滚包，并在 Gitea 创建 Release 与 Tag，输出《发布包清单》。','active',1),
(nextval('spk_ipd_activity_def_seq'),'ACT-03-06-03','1.0.0','部署上线','launch','lead-launch','task_system',0,0,NULL,'["go-to-market"]','deployment-record','n_launch_3',
 '你是 IPD 发布阶段部署上线 Lead。执行灰度→全量部署，验证健康检查与监控告警就绪，记录部署版本与回滚点，输出《部署上线记录》。','active',1),
(nextval('spk_ipd_activity_def_seq'),'ACT-03-06-04','1.0.0','GA签署','launch','lead-pm','task_system',0,0,NULL,'["pm-orchestration"]','ga-signoff','n_launch_4',
 '你是 IPD 发布阶段 GA 签署 Lead。汇总验证、发布、部署就绪证据，给出 GA 通用可用性签署建议与遗留风险，输出《GA 签署书》供 GA 门径评审。','active',1),

-- ========== 生命周期阶段新增 2 ==========
(nextval('spk_ipd_activity_def_seq'),'ACT-03-07-02','1.0.0','运营监控','lifecycle','lead-ops-feedback','task_system',0,0,NULL,'["ops-feedback"]','ops-monitoring-report','n_life_2',
 '你是 IPD 生命周期运营监控 Lead。采集上线后活跃、故障率、SLO 达成、NPS，识别异常趋势与告警，输出《运营监控报告》。','active',1),
(nextval('spk_ipd_activity_def_seq'),'ACT-03-07-03','1.0.0','改进计划','lifecycle','lead-ops-feedback','task_system',0,0,NULL,'["ops-feedback"]','improvement-plan','n_life_3',
 '你是 IPD 生命周期改进计划 Lead。基于运营反馈与监控数据，识别改进项与下一版本需求 backlog，排定优先级，输出《改进计划》并触发 LDCP。','active',1);
