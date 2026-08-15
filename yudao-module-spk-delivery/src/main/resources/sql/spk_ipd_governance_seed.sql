-- =====================================================================
-- SPK-OS Cortext-IPD 流程治理种子（§7.2 / §6.3）
-- 三个 flow_type 的真实 Profile 模板 + 初始已发布版本 v1。
-- 管理员配置产物（非运行数据、非造假）：定义默认阶段序列与裁剪策略基线。
-- 幂等：ON CONFLICT (id) DO NOTHING + profile_code 唯一索引兜底。tenant_id 显式 1。
-- snapshot_json 为 Profile 不可变快照（阶段序列 + 默认裁剪 + 门禁产物），供 FlowRun 启动时复制。
-- =====================================================================

-- 三模板 Profile 主行（id 段 7001/7002/7003）
INSERT INTO "spk_ipd_process_profile"
("id","profile_code","name","flow_type","description","status","current_version","published_by","published_at","tenant_id") VALUES
(7001,'FULL_RELEASE_V1','完整发布流程模板','FULL_RELEASE',
 'IPD 完整发布：概念→计划→开发→验证→发布→生命周期 6 阶段全量门径（CDCP/PDCP/ADCP/GA/LDCP）。',
 'PUBLISHED',1,'1',now(),1),
(7002,'INCREMENT_RELEASE_V1','增量发布流程模板','INCREMENT_RELEASE',
 'IPD 增量发布：基于基线版本的增量交付，裁剪概念阶段，从计划阶段起按 ADCP 门径推进。',
 'PUBLISHED',1,'1',now(),1),
(7003,'ISSUE_RESOLUTION_V1','问题流模板','ISSUE_RESOLUTION',
 'IPD 问题流：针对线上 Issue 的快速修复流，裁剪至开发+验证+热修复发布，TR4 门径。',
 'PUBLISHED',1,'1',now(),1)
ON CONFLICT (id) DO NOTHING;

-- 各 Profile 的 v1 已发布版本快照（id 段 7101/7102/7103）
-- snapshot_json 含 stages（阶段序列）+ gates（门径）+ defaultTrim（默认裁剪）+ requiredArtifacts（门禁产物）
INSERT INTO "spk_ipd_process_profile_version"
("id","profile_id","version","snapshot_json","status","compatibility_hash","published_by","published_at","tenant_id") VALUES
(7101,7001,1,
'{"profileCode":"FULL_RELEASE_V1","flowType":"FULL_RELEASE","stages":["concept","plan","develop","qualify","launch","lifecycle"],"gates":["CDCP","PDCP","ADCP","GA","LDCP"],"defaultTrim":{},"requiredArtifactsByGate":{"CDCP":["feasibility-report","concept-decision-brief"],"PDCP":["proj-plan","risk-assessment"],"ADCP":["integration-build","code-review-report"],"GA":["release-package","system-test-report"],"LDCP":["ops-monitoring-report","improvement-plan"]}}',
 'PUBLISHED','sha256:full-release-v1','1',now(),1),
(7102,7002,1,
'{"profileCode":"INCREMENT_RELEASE_V1","flowType":"INCREMENT_RELEASE","stages":["plan","develop","qualify","launch"],"gates":["ADCP","GA"],"defaultTrim":{"skipStages":["concept"],"reason":"增量发布基于既有基线，跳过概念阶段"},"requiredArtifactsByGate":{"ADCP":["integration-build","code-review-report"],"GA":["release-package","system-test-report"]}}',
 'PUBLISHED','sha256:increment-release-v1','1',now(),1),
(7103,7003,1,
'{"profileCode":"ISSUE_RESOLUTION_V1","flowType":"ISSUE_RESOLUTION","stages":["develop","qualify","launch"],"gates":["TR4"],"defaultTrim":{"skipStages":["concept","plan"],"reason":"问题流聚焦热修复，裁剪概念与计划阶段"},"requiredArtifactsByGate":{"TR4":["hotfix-report","security-test-report"]}}',
 'PUBLISHED','sha256:issue-resolution-v1','1',now(),1)
ON CONFLICT (id) DO NOTHING;

-- 增量发布与问题流的显式裁剪规则（绑定 ProfileVersion）
INSERT INTO "spk_ipd_trim_rule"
("id","profile_version_id","stage","activity_def_id","trim_condition","action","reason","tenant_id") VALUES
(7201,7102,'concept',NULL,'flowType=INCREMENT_RELEASE','SKIP','增量发布跳过概念阶段',1),
(7202,7103,'concept',NULL,'flowType=ISSUE_RESOLUTION','SKIP','问题流跳过概念阶段',1),
(7203,7103,'plan',NULL,'flowType=ISSUE_RESOLUTION','SKIP','问题流跳过计划阶段',1)
ON CONFLICT (id) DO NOTHING;
