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
    "new_charter_seed" boolean NULL DEFAULT false,
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
    "pass" boolean NULL,
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
