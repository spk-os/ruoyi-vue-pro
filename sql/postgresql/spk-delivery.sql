-- =====================================================================
-- SPK-OS yudao-module-spk-delivery 表结构（PostgreSQL）
-- IPD 交付子系统：agent 任务 / Aegis 审查 / Workflow Contract / CCB /
-- OR 池 / R7 反馈 / R8 退市 / 门禁审计 / DCP 回退日志
--
-- 说明：
-- 1. 全部带 tenant_id int8 NOT NULL DEFAULT 0，兼容多租户拦截器
-- 2. 主键 id int8，IdType=INPUT（PostgreSQL 自动适配），每表配 *_seq 序列
-- 3. 软删除 deleted int2 NOT NULL DEFAULT 0；审计字段 creator/create_time/updater/update_time
-- =====================================================================

-- ----------------------------
-- 1. spk_agent_task：Agent 任务执行实例
-- ----------------------------
DROP TABLE IF EXISTS "spk_agent_task";
CREATE TABLE IF NOT EXISTS "spk_agent_task" (
    "id" int8 NOT NULL,
    "task_id" varchar(64) NULL,
    "role_id" int8 NULL,
    "conversation_id" int8 NULL,
    "prompt" text NULL,
    "status" varchar(16) NOT NULL DEFAULT 'running',
    "result" text NULL,
    "instance_id" varchar(64) NULL,
    "node_key" varchar(64) NULL,
    "receive_task_key" varchar(64) NULL,
    "ttl_expire_time" int8 NULL DEFAULT 0,
    "creator" varchar(64) NULL DEFAULT '',
    "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater" varchar(64) NULL DEFAULT '',
    "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted" int2 NOT NULL DEFAULT 0,
    "tenant_id" int8 NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "spk_agent_task" IS 'SPK-OS Agent 任务执行实例';
COMMENT ON COLUMN "spk_agent_task"."status" IS '状态 running/done/failed/cancelled';
CREATE SEQUENCE IF NOT EXISTS spk_agent_task_seq;

-- ----------------------------
-- 2. spk_aegis_review：Aegis 审查结论
-- ----------------------------
DROP TABLE IF EXISTS "spk_aegis_review";
CREATE TABLE IF NOT EXISTS "spk_aegis_review" (
    "id" int8 NOT NULL,
    "review_id" varchar(64) NULL,
    "instance_id" varchar(64) NULL,
    "verdict" varchar(16) NULL,
    "report" text NULL,
    "evidence" text NULL,
    "node_key" varchar(64) NULL,
    "creator" varchar(64) NULL DEFAULT '',
    "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater" varchar(64) NULL DEFAULT '',
    "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted" int2 NOT NULL DEFAULT 0,
    "tenant_id" int8 NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "spk_aegis_review" IS 'SPK-OS Aegis 审查结论';
COMMENT ON COLUMN "spk_aegis_review"."verdict" IS '裁决 pass/fail/conditional';
CREATE SEQUENCE IF NOT EXISTS spk_aegis_review_seq;

-- ----------------------------
-- 3. spk_contract_version：Workflow Contract 版本治理
-- ----------------------------
DROP TABLE IF EXISTS "spk_contract_version";
CREATE TABLE IF NOT EXISTS "spk_contract_version" (
    "id" int8 NOT NULL,
    "model_key" varchar(64) NOT NULL,
    "hash" varchar(64) NOT NULL,
    "version" int4 NOT NULL DEFAULT 1,
    "diff_json" text NULL,
    "snapshot_json" text NULL,
    "creator" varchar(64) NULL DEFAULT '',
    "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater" varchar(64) NULL DEFAULT '',
    "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted" int2 NOT NULL DEFAULT 0,
    "tenant_id" int8 NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "spk_contract_version" IS 'SPK-OS Workflow Contract 版本治理';
CREATE SEQUENCE IF NOT EXISTS spk_contract_version_seq;

-- ----------------------------
-- 4. spk_ccb_record：CCB 变更台账
-- ----------------------------
DROP TABLE IF EXISTS "spk_ccb_record";
CREATE TABLE IF NOT EXISTS "spk_ccb_record" (
    "id" int8 NOT NULL,
    "change_id" varchar(64) NULL,
    "instance_id" varchar(64) NULL,
    "change_request" text NULL,
    "impact" text NULL,
    "decision" varchar(16) NULL,
    "creator" varchar(64) NULL DEFAULT '',
    "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater" varchar(64) NULL DEFAULT '',
    "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted" int2 NOT NULL DEFAULT 0,
    "tenant_id" int8 NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "spk_ccb_record" IS 'SPK-OS CCB 变更台账';
COMMENT ON COLUMN "spk_ccb_record"."decision" IS '决议 approve/reject/defer';
CREATE SEQUENCE IF NOT EXISTS spk_ccb_record_seq;

-- ----------------------------
-- 5. spk_intellect_queue：OR 池需求队列
-- ----------------------------
DROP TABLE IF EXISTS "spk_intellect_queue";
CREATE TABLE IF NOT EXISTS "spk_intellect_queue" (
    "id" int8 NOT NULL,
    "source" varchar(32) NULL,
    "req_id" varchar(64) NULL,
    "status" varchar(16) NOT NULL DEFAULT 'pending',
    "dedup_hash" varchar(64) NULL,
    "payload" text NULL,
    "creator" varchar(64) NULL DEFAULT '',
    "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater" varchar(64) NULL DEFAULT '',
    "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted" int2 NOT NULL DEFAULT 0,
    "tenant_id" int8 NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "spk_intellect_queue" IS 'SPK-OS OR 池需求队列';
CREATE SEQUENCE IF NOT EXISTS spk_intellect_queue_seq;

-- ----------------------------
-- 6. spk_feedback：R7 反馈数据
-- ----------------------------
DROP TABLE IF EXISTS "spk_feedback";
CREATE TABLE IF NOT EXISTS "spk_feedback" (
    "id" int8 NOT NULL,
    "instance_id" varchar(64) NULL,
    "source" varchar(32) NULL,
    "content" text NULL,
    "summary" text NULL,
    "new_charter_seed" int2 NULL DEFAULT 0,
    "creator" varchar(64) NULL DEFAULT '',
    "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater" varchar(64) NULL DEFAULT '',
    "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted" int2 NOT NULL DEFAULT 0,
    "tenant_id" int8 NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "spk_feedback" IS 'SPK-OS R7 反馈数据';
CREATE SEQUENCE IF NOT EXISTS spk_feedback_seq;

-- ----------------------------
-- 7. spk_sunset：R8 退市数据
-- ----------------------------
DROP TABLE IF EXISTS "spk_sunset";
CREATE TABLE IF NOT EXISTS "spk_sunset" (
    "id" int8 NOT NULL,
    "instance_id" varchar(64) NULL,
    "sunset_report" text NULL,
    "archive_status" varchar(16) NULL,
    "creator" varchar(64) NULL DEFAULT '',
    "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater" varchar(64) NULL DEFAULT '',
    "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted" int2 NOT NULL DEFAULT 0,
    "tenant_id" int8 NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "spk_sunset" IS 'SPK-OS R8 退市数据';
CREATE SEQUENCE IF NOT EXISTS spk_sunset_seq;

-- ----------------------------
-- 8. spk_gate_record：门禁回调审计
-- ----------------------------
DROP TABLE IF EXISTS "spk_gate_record";
CREATE TABLE IF NOT EXISTS "spk_gate_record" (
    "id" int8 NOT NULL,
    "instance_id" varchar(64) NOT NULL,
    "node_key" varchar(64) NULL,
    "gate" varchar(16) NOT NULL,
    "report" text NULL,
    "pass" int2 NULL,
    "callback_time" timestamp NULL,
    "creator" varchar(64) NULL DEFAULT '',
    "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater" varchar(64) NULL DEFAULT '',
    "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted" int2 NOT NULL DEFAULT 0,
    "tenant_id" int8 NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "spk_gate_record" IS 'SPK-OS 门禁回调审计';
COMMENT ON COLUMN "spk_gate_record"."gate" IS '门禁标识 g1..g8 / tr2..tr6';
CREATE SEQUENCE IF NOT EXISTS spk_gate_record_seq;

-- ----------------------------
-- 9. spk_dcp_redirect_log：DCP 回退日志
-- ----------------------------
DROP TABLE IF EXISTS "spk_dcp_redirect_log";
CREATE TABLE IF NOT EXISTS "spk_dcp_redirect_log" (
    "id" int8 NOT NULL,
    "instance_id" varchar(64) NOT NULL,
    "dcp" varchar(16) NOT NULL,
    "redirect_count" int4 NOT NULL DEFAULT 0,
    "target_node" varchar(64) NULL,
    "creator" varchar(64) NULL DEFAULT '',
    "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater" varchar(64) NULL DEFAULT '',
    "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted" int2 NOT NULL DEFAULT 0,
    "tenant_id" int8 NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "spk_dcp_redirect_log" IS 'SPK-OS DCP 回退日志';
COMMENT ON COLUMN "spk_dcp_redirect_log"."dcp" IS 'DCP 标识 cdc/pdc/adc/ldc';
CREATE SEQUENCE IF NOT EXISTS spk_dcp_redirect_log_seq;

-- =====================================================================
-- Cortext-IPD 端到端交付子系统（P1）
-- Activity Registry / Task Contract / Artifact / Evidence 三件套 /
-- Model Capability Registry / 既有 Paddock 表扩展。依 SPK-OS-Cortext-IPD.md §4
-- JSON 一律存 text（PG）；int8 id + *_seq + tenant_id int8 + deleted int2
-- =====================================================================

-- ----------------------------
-- 10. spk_ipd_activity_def：IPD Activity 定义注册中心（GAP-1）
-- ----------------------------
DROP TABLE IF EXISTS "spk_ipd_activity_def";
CREATE TABLE IF NOT EXISTS "spk_ipd_activity_def" (
    "id" int8 NOT NULL,
    "activity_id" varchar(64) NOT NULL,
    "version" varchar(16) NOT NULL DEFAULT '1.0.0',
    "name" varchar(128) NOT NULL,
    "stage" varchar(16) NOT NULL,
    "description" text NULL,
    "lead_agent_code" varchar(64) NOT NULL,
    "execution_location" varchar(16) NOT NULL DEFAULT 'task_system',
    "use_worker_agent" int2 NOT NULL DEFAULT 0,
    "use_independent_verifier" int2 NOT NULL DEFAULT 0,
    "verifier_type" varchar(16) NULL,
    "model_capabilities" text NULL,
    "skills" text NULL,
    "tools" text NULL,
    "knowledge_refs" text NULL,
    "information_refs" text NULL,
    "memory_scope" text NULL,
    "input_artifact_types" text NULL,
    "output_artifact_type" varchar(64) NULL,
    "output_template_ref" varchar(64) NULL,
    "acceptance_criteria" text NULL,
    "human_approver_role" varchar(64) NULL,
    "failure_handling" text NULL,
    "flowable_node_id" varchar(64) NULL,
    "prompt_template" text NULL,
    "golden_cases" text NULL,
    "status" varchar(16) NOT NULL DEFAULT 'draft',
    "approved_by" varchar(64) NULL,
    "creator" varchar(64) NULL DEFAULT '',
    "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater" varchar(64) NULL DEFAULT '',
    "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted" int2 NOT NULL DEFAULT 0,
    "tenant_id" int8 NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "spk_ipd_activity_def" IS 'IPD Activity 定义注册中心';
COMMENT ON COLUMN "spk_ipd_activity_def"."stage" IS '阶段 concept/plan/develop/qualify/launch/lifecycle';
COMMENT ON COLUMN "spk_ipd_activity_def"."execution_location" IS '执行位置 task_system/lead_agent_internal';
COMMENT ON COLUMN "spk_ipd_activity_def"."status" IS '状态 draft/active/deprecated';
CREATE SEQUENCE IF NOT EXISTS spk_ipd_activity_def_seq;

-- ----------------------------
-- 11. spk_task_contract：Task Router 与 Lead Agent 之间的执行合同（GAP-2）
-- ----------------------------
DROP TABLE IF EXISTS "spk_task_contract";
CREATE TABLE IF NOT EXISTS "spk_task_contract" (
    "id" int8 NOT NULL,
    "contract_id" varchar(64) NOT NULL,
    "activity_run_id" varchar(64) NOT NULL,
    "activity_id" varchar(64) NOT NULL,
    "activity_version" varchar(16) NOT NULL,
    "process_instance_id" varchar(64) NOT NULL,
    "task_id" varchar(64) NULL,
    "business_key" varchar(64) NULL,
    "phase" varchar(16) NULL,
    "node_key" varchar(64) NULL,
    "execution_mode" varchar(16) NOT NULL,
    "lead_agent_id" int8 NOT NULL,
    "lead_agent_code" varchar(64) NOT NULL,
    "worker_required" int2 NOT NULL DEFAULT 0,
    "worker_squad_id" int8 NULL,
    "verifier_required" int2 NOT NULL DEFAULT 0,
    "verifier_type" varchar(16) NULL,
    "model_snapshot_id" varchar(64) NULL,
    "context_manifest_uri" varchar(512) NULL,
    "input_refs" text NULL,
    "output_spec" text NULL,
    "timeout_seconds" int4 NOT NULL DEFAULT 1800,
    "retry_policy" text NULL,
    "prompt" text NULL,
    "status" varchar(16) NOT NULL DEFAULT 'queued',
    "queued_at" timestamp NULL,
    "started_at" timestamp NULL,
    "finished_at" timestamp NULL,
    "failure_reason" text NULL,
    "creator" varchar(64) NULL DEFAULT '',
    "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater" varchar(64) NULL DEFAULT '',
    "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted" int2 NOT NULL DEFAULT 0,
    "tenant_id" int8 NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "spk_task_contract" IS 'Task Router 与 Lead Agent 执行合同';
COMMENT ON COLUMN "spk_task_contract"."execution_mode" IS '执行模式 task_system/lead_internal';
COMMENT ON COLUMN "spk_task_contract"."status" IS '状态 queued/running/done/failed/timeout/cancelled';
CREATE SEQUENCE IF NOT EXISTS spk_task_contract_seq;

-- ----------------------------
-- 12. spk_artifact_manifest：Artifact 不可变登记中心（GAP-4）
-- ----------------------------
DROP TABLE IF EXISTS "spk_artifact_manifest";
CREATE TABLE IF NOT EXISTS "spk_artifact_manifest" (
    "id" int8 NOT NULL,
    "artifact_id" varchar(64) NOT NULL,
    "artifact_type" varchar(64) NOT NULL,
    "activity_run_id" varchar(64) NOT NULL,
    "process_instance_id" varchar(64) NULL,
    "uri" varchar(512) NOT NULL,
    "content_hash" varchar(64) NOT NULL,
    "version" int4 NOT NULL DEFAULT 1,
    "status" varchar(16) NOT NULL DEFAULT 'draft',
    "signer_required" int2 NOT NULL DEFAULT 0,
    "signed_by" varchar(64) NULL,
    "signed_at" timestamp NULL,
    "dependencies" text NULL,
    "metadata" text NULL,
    "mime" varchar(64) NULL,
    "bytes" int8 NULL,
    "classification" varchar(32) NULL,
    "scan_status" varchar(16) NOT NULL DEFAULT 'pending',
    "supersedes" varchar(64) NULL,
    "summary" text NULL,
    "creator" varchar(64) NULL DEFAULT '',
    "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater" varchar(64) NULL DEFAULT '',
    "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted" int2 NOT NULL DEFAULT 0,
    "tenant_id" int8 NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "spk_artifact_manifest" IS 'Artifact 不可变登记中心';
COMMENT ON COLUMN "spk_artifact_manifest"."status" IS '状态 draft/signed/superseded/quarantined';
COMMENT ON COLUMN "spk_artifact_manifest"."scan_status" IS '扫描状态 pending/clean/quarantined';
CREATE SEQUENCE IF NOT EXISTS spk_artifact_manifest_seq;

-- ----------------------------
-- 13. spk_run_receipt：Agent 运行收据（Evidence 三件套之一，GAP-5）
-- ----------------------------
DROP TABLE IF EXISTS "spk_run_receipt";
CREATE TABLE IF NOT EXISTS "spk_run_receipt" (
    "id" int8 NOT NULL,
    "run_id" varchar(64) NOT NULL,
    "activity_run_id" varchar(64) NOT NULL,
    "contract_id" varchar(64) NULL,
    "lead_agent_id" int8 NULL,
    "lead_agent_code" varchar(64) NULL,
    "model_snapshot_id" varchar(64) NULL,
    "capability_id" varchar(64) NULL,
    "provider" varchar(64) NULL,
    "model" varchar(64) NULL,
    "started_at" timestamp NULL,
    "finished_at" timestamp NULL,
    "token_usage" text NULL,
    "cost" numeric(14,4) NULL,
    "latency_ms" int4 NULL,
    "status" varchar(16) NULL,
    "failure_reason" text NULL,
    "artifact_uris" text NULL,
    "worker_links" text NULL,
    "conversation_id" int8 NULL,
    "creator" varchar(64) NULL DEFAULT '',
    "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater" varchar(64) NULL DEFAULT '',
    "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted" int2 NOT NULL DEFAULT 0,
    "tenant_id" int8 NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "spk_run_receipt" IS 'Agent 运行收据（Lead 写）';
COMMENT ON COLUMN "spk_run_receipt"."token_usage" IS 'JSON {prompt,completion,total}';
CREATE SEQUENCE IF NOT EXISTS spk_run_receipt_seq;

-- ----------------------------
-- 14. spk_verification_receipt：Independent Verifier 验证收据（Evidence 之二）
-- ----------------------------
DROP TABLE IF EXISTS "spk_verification_receipt";
CREATE TABLE IF NOT EXISTS "spk_verification_receipt" (
    "id" int8 NOT NULL,
    "receipt_id" varchar(64) NOT NULL,
    "artifact_id" varchar(64) NOT NULL,
    "activity_run_id" varchar(64) NULL,
    "verifier_id" int8 NULL,
    "verifier_code" varchar(64) NULL,
    "verifier_type" varchar(16) NULL,
    "verification_method" varchar(64) NULL,
    "evidence_points" text NULL,
    "overall_conclusion" varchar(32) NULL,
    "model_snapshot_id" varchar(64) NULL,
    "summary" text NULL,
    "signed_at" timestamp NULL,
    "creator" varchar(64) NULL DEFAULT '',
    "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater" varchar(64) NULL DEFAULT '',
    "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted" int2 NOT NULL DEFAULT 0,
    "tenant_id" int8 NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "spk_verification_receipt" IS 'Independent Verifier 验证收据';
COMMENT ON COLUMN "spk_verification_receipt"."verification_method" IS 'recheck/redteam/completeness/traceback';
COMMENT ON COLUMN "spk_verification_receipt"."overall_conclusion" IS 'PASS/CONDITIONAL/FAIL';
CREATE SEQUENCE IF NOT EXISTS spk_verification_receipt_seq;

-- ----------------------------
-- 15. spk_evidence_record：证据中心（追加式，哈希链，Evidence 之三）
-- ----------------------------
DROP TABLE IF EXISTS "spk_evidence_record";
CREATE TABLE IF NOT EXISTS "spk_evidence_record" (
    "id" int8 NOT NULL,
    "evidence_id" varchar(64) NOT NULL,
    "activity_run_id" varchar(64) NULL,
    "process_instance_id" varchar(64) NULL,
    "evidence_type" varchar(32) NULL,
    "ref_id" varchar(64) NULL,
    "payload" text NULL,
    "prev_hash" varchar(64) NULL,
    "row_hash" varchar(64) NULL,
    "occurred_at" timestamp NULL,
    "creator" varchar(64) NULL DEFAULT '',
    "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater" varchar(64) NULL DEFAULT '',
    "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted" int2 NOT NULL DEFAULT 0,
    "tenant_id" int8 NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "spk_evidence_record" IS '证据中心（追加式，哈希链）';
COMMENT ON COLUMN "spk_evidence_record"."evidence_type" IS 'run/tool/verification/human_approval/decision/gate';
CREATE SEQUENCE IF NOT EXISTS spk_evidence_record_seq;

-- ----------------------------
-- 16. spk_model_capability_profile：模型能力注册表（GAP-6）
-- ----------------------------
DROP TABLE IF EXISTS "spk_model_capability_profile";
CREATE TABLE IF NOT EXISTS "spk_model_capability_profile" (
    "id" int8 NOT NULL,
    "capability_id" varchar(64) NOT NULL,
    "provider" varchar(64) NOT NULL,
    "model" varchar(64) NOT NULL,
    "deployment_id" varchar(64) NULL,
    "context_length" int4 NULL,
    "price_card_uri" varchar(512) NULL,
    "price_card_hash" varchar(64) NULL,
    "eval_profile" text NULL,
    "data_policy" varchar(32) NULL,
    "region" varchar(32) NULL,
    "priority" int4 NOT NULL DEFAULT 100,
    "status" varchar(16) NOT NULL DEFAULT 'active',
    "valid_at" timestamp NULL,
    "expires_at" timestamp NULL,
    "creator" varchar(64) NULL DEFAULT '',
    "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater" varchar(64) NULL DEFAULT '',
    "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted" int2 NOT NULL DEFAULT 0,
    "tenant_id" int8 NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "spk_model_capability_profile" IS '模型能力注册表';
COMMENT ON COLUMN "spk_model_capability_profile"."data_policy" IS 'domestic/international/any';
CREATE SEQUENCE IF NOT EXISTS spk_model_capability_profile_seq;

-- ----------------------------
-- 17. spk_model_registry_snapshot：执行前冻结的模型快照
-- ----------------------------
DROP TABLE IF EXISTS "spk_model_registry_snapshot";
CREATE TABLE IF NOT EXISTS "spk_model_registry_snapshot" (
    "id" int8 NOT NULL,
    "snapshot_id" varchar(64) NOT NULL,
    "profile_json" text NOT NULL,
    "frozen_by" varchar(64) NULL,
    "valid_at" timestamp NULL,
    "expires_at" timestamp NULL,
    "creator" varchar(64) NULL DEFAULT '',
    "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater" varchar(64) NULL DEFAULT '',
    "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted" int2 NOT NULL DEFAULT 0,
    "tenant_id" int8 NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "spk_model_registry_snapshot" IS '每次执行前冻结的模型快照';
CREATE SEQUENCE IF NOT EXISTS spk_model_registry_snapshot_seq;

-- ----------------------------
-- 18. 既有 Paddock 表扩展（GAP-9 / ActivityRun 承载）
-- ----------------------------
ALTER TABLE "spk_agent_def" ADD COLUMN IF NOT EXISTS "agent_kind" varchar(16) NOT NULL DEFAULT 'lead';
ALTER TABLE "spk_agent_def" ADD COLUMN IF NOT EXISTS "capability_tags" text NULL;
ALTER TABLE "spk_agent_def" ADD COLUMN IF NOT EXISTS "verifier_type" varchar(16) NULL;
ALTER TABLE "spk_agent_def" ADD COLUMN IF NOT EXISTS "isolation_level" varchar(16) NOT NULL DEFAULT 'process';

ALTER TABLE "spk_agent_task" ADD COLUMN IF NOT EXISTS "activity_run_id" varchar(64) NULL;
ALTER TABLE "spk_agent_task" ADD COLUMN IF NOT EXISTS "contract_id" varchar(64) NULL;
ALTER TABLE "spk_agent_task" ADD COLUMN IF NOT EXISTS "activity_id" varchar(64) NULL;
ALTER TABLE "spk_agent_task" ADD COLUMN IF NOT EXISTS "attempt_no" int4 NOT NULL DEFAULT 1;
ALTER TABLE "spk_agent_task" ADD COLUMN IF NOT EXISTS "claim_id" varchar(64) NULL;
ALTER TABLE "spk_agent_task" ADD COLUMN IF NOT EXISTS "fencing_token" int8 NULL;
ALTER TABLE "spk_agent_task" ADD COLUMN IF NOT EXISTS "worker_of" varchar(64) NULL;
ALTER TABLE "spk_agent_task" ADD COLUMN IF NOT EXISTS "artifact_uris" text NULL;
ALTER TABLE "spk_agent_task" ADD COLUMN IF NOT EXISTS "run_receipt_id" varchar(64) NULL;
ALTER TABLE "spk_agent_task" ADD COLUMN IF NOT EXISTS "verification_conclusion" varchar(16) NULL;
