-- =====================================================================
-- SPK-OS Cortext-IPD 流程治理模块建表（§7.2 第 4 顶层表面 / §6.3 / §9.5）
-- 依 SPK-OS-Delivery-IPD.md v2.0.0
-- 6 张治理表：ProcessProfile / ProfileVersion / TrimRule / EngineInstance / FailedJob / GovernanceAudit
-- + ActivityDef 追加 required_artifacts 列（按 spk_ipd_activity_def_p2_extend.sql ALTER 模式）
-- 幂等：CREATE TABLE IF NOT EXISTS + CREATE SEQUENCE IF NOT EXISTS + ADD COLUMN IF NOT EXISTS，重跑不破坏数据。
-- PG 约定（与 spk_ipd_business_skeleton_init.sql 一致）：
--   id bigint 无 default（由 @KeySequence nextval 注入）；deleted smallint default 0；
--   tenant_id bigint default 0；时间 timestamp without time zone；JSON 存 text。
-- tenant_id=0 是默认值，运行时 TenantDatabaseInterceptor 自动注入当前租户；psql 直插须显式写 1。
-- =====================================================================

CREATE SEQUENCE IF NOT EXISTS spk_ipd_process_profile_seq;
CREATE SEQUENCE IF NOT EXISTS spk_ipd_process_profile_version_seq;
CREATE SEQUENCE IF NOT EXISTS spk_ipd_trim_rule_seq;
CREATE SEQUENCE IF NOT EXISTS spk_ipd_engine_instance_seq;
CREATE SEQUENCE IF NOT EXISTS spk_ipd_failed_job_seq;
CREATE SEQUENCE IF NOT EXISTS spk_ipd_governance_audit_seq;

-- ---------- §9.5.1 spk_ipd_process_profile 流程模板 ----------
-- 管理员配置产物（非运行数据）。一个 flow_type 对应一个已发布 Profile。
CREATE TABLE IF NOT EXISTS "spk_ipd_process_profile" (
  "id"               BIGINT       NOT NULL,
  "profile_code"     VARCHAR(64)  NOT NULL,
  "name"             VARCHAR(128) NOT NULL,
  "flow_type"        VARCHAR(32) NOT NULL,
  "description"      TEXT,
  "status"           VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
  "current_version"  INT          NOT NULL DEFAULT 0,
  "published_by"     VARCHAR(64),
  "published_at"     TIMESTAMP,
  "lock_version"     INT          NOT NULL DEFAULT 0,
  "creator"          VARCHAR(64) DEFAULT '',
  "create_time"      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater"          VARCHAR(64) DEFAULT '',
  "update_time"      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted"          SMALLINT     NOT NULL DEFAULT 0,
  "tenant_id"        BIGINT       NOT NULL DEFAULT 0,
  CONSTRAINT "spk_ipd_process_profile_pkey" PRIMARY KEY ("id")
);
CREATE UNIQUE INDEX IF NOT EXISTS "uk_spk_ipd_process_profile_code" ON "spk_ipd_process_profile" ("profile_code") WHERE "deleted" = 0;
CREATE INDEX IF NOT EXISTS "idx_spk_ipd_process_profile_type" ON "spk_ipd_process_profile" ("flow_type","status");

-- ---------- §9.5.2 spk_ipd_process_profile_version Profile 版本 ----------
-- 版本化快照：发布/回滚/兼容检查。status: DRAFT/PUBLISHED/SUPERSEDED。
CREATE TABLE IF NOT EXISTS "spk_ipd_process_profile_version" (
  "id"                     BIGINT       NOT NULL,
  "profile_id"             BIGINT       NOT NULL,
  "version"                INT          NOT NULL,
  "snapshot_json"          TEXT         NOT NULL,
  "status"                 VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
  "compatibility_hash"     VARCHAR(128),
  "published_by"           VARCHAR(64),
  "published_at"           TIMESTAMP,
  "supersedes_version_id"  BIGINT,
  "lock_version"           INT          NOT NULL DEFAULT 0,
  "creator"                VARCHAR(64) DEFAULT '',
  "create_time"            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater"                VARCHAR(64) DEFAULT '',
  "update_time"            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted"                SMALLINT     NOT NULL DEFAULT 0,
  "tenant_id"              BIGINT       NOT NULL DEFAULT 0,
  CONSTRAINT "spk_ipd_process_profile_version_pkey" PRIMARY KEY ("id")
);
CREATE UNIQUE INDEX IF NOT EXISTS "uk_spk_ipd_profile_version" ON "spk_ipd_process_profile_version" ("profile_id","version") WHERE "deleted" = 0;
CREATE INDEX IF NOT EXISTS "idx_spk_ipd_profile_version_status" ON "spk_ipd_process_profile_version" ("profile_id","status");

-- ---------- §9.5.3 spk_ipd_trim_rule 裁剪规则 ----------
-- 绑定 ProfileVersion。按 stage/activity_def_id 定义裁剪条件与动作。
CREATE TABLE IF NOT EXISTS "spk_ipd_trim_rule" (
  "id"                  BIGINT       NOT NULL,
  "profile_version_id"  BIGINT       NOT NULL,
  "stage"               VARCHAR(32),
  "activity_def_id"     VARCHAR(64),
  "trim_condition"     VARCHAR(128),
  "action"              VARCHAR(32) NOT NULL,
  "reason"              TEXT,
  "lock_version"         INT          NOT NULL DEFAULT 0,
  "creator"             VARCHAR(64) DEFAULT '',
  "create_time"         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater"             VARCHAR(64) DEFAULT '',
  "update_time"         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted"             SMALLINT     NOT NULL DEFAULT 0,
  "tenant_id"           BIGINT       NOT NULL DEFAULT 0,
  CONSTRAINT "spk_ipd_trim_rule_pkey" PRIMARY KEY ("id")
);
CREATE INDEX IF NOT EXISTS "idx_spk_ipd_trim_rule_pv" ON "spk_ipd_trim_rule" ("profile_version_id","stage");

-- ---------- §9.5.4 spk_ipd_engine_instance 引擎实例档案 ----------
-- Flowable process_instance 与 FlowRun/ProfileVersion 的绑定关系 + 健康度。
CREATE TABLE IF NOT EXISTS "spk_ipd_engine_instance" (
  "id"                   BIGINT       NOT NULL,
  "process_instance_id"  VARCHAR(64) NOT NULL,
  "flow_run_id"          BIGINT,
  "profile_version_id"   BIGINT,
  "engine_health"        VARCHAR(16) NOT NULL DEFAULT 'UNKNOWN',
  "last_synced_at"       TIMESTAMP,
  "meta_json"            TEXT,
  "lock_version"         INT          NOT NULL DEFAULT 0,
  "creator"              VARCHAR(64) DEFAULT '',
  "create_time"          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater"              VARCHAR(64) DEFAULT '',
  "update_time"          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted"              SMALLINT     NOT NULL DEFAULT 0,
  "tenant_id"            BIGINT       NOT NULL DEFAULT 0,
  CONSTRAINT "spk_ipd_engine_instance_pkey" PRIMARY KEY ("id")
);
CREATE UNIQUE INDEX IF NOT EXISTS "uk_spk_ipd_engine_instance_pid" ON "spk_ipd_engine_instance" ("process_instance_id") WHERE "deleted" = 0;
CREATE INDEX IF NOT EXISTS "idx_spk_ipd_engine_instance_run" ON "spk_ipd_engine_instance" ("flow_run_id");

-- ---------- §9.5.5 spk_ipd_failed_job 失败作业 ----------
-- 触发器/回调/同步等失败作业的登记与重试。status: PENDING/RETRYING/RESOLVED/DEAD。
CREATE TABLE IF NOT EXISTS "spk_ipd_failed_job" (
  "id"             BIGINT       NOT NULL,
  "job_type"       VARCHAR(64) NOT NULL,
  "ref_id"         BIGINT,
  "reason"         TEXT,
  "retry_count"    INT          NOT NULL DEFAULT 0,
  "status"         VARCHAR(24) NOT NULL DEFAULT 'PENDING',
  "next_retry_at"  TIMESTAMP,
  "lock_version"   INT          NOT NULL DEFAULT 0,
  "creator"        VARCHAR(64) DEFAULT '',
  "create_time"    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater"        VARCHAR(64) DEFAULT '',
  "update_time"    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted"        SMALLINT     NOT NULL DEFAULT 0,
  "tenant_id"      BIGINT       NOT NULL DEFAULT 0,
  CONSTRAINT "spk_ipd_failed_job_pkey" PRIMARY KEY ("id")
);
CREATE INDEX IF NOT EXISTS "idx_spk_ipd_failed_job_status" ON "spk_ipd_failed_job" ("status","next_retry_at");

-- ---------- §9.5.6 spk_ipd_governance_audit 治理审计 ----------
-- Profile 发布/回滚、裁剪变更、引擎同步等治理动作的前后态审计。
CREATE TABLE IF NOT EXISTS "spk_ipd_governance_audit" (
  "id"           BIGINT       NOT NULL,
  "action_type"  VARCHAR(64) NOT NULL,
  "ref_id"       BIGINT,
  "operator_id"  VARCHAR(64),
  "before_json"  TEXT,
  "after_json"   TEXT,
  "remark"       TEXT,
  "lock_version" INT          NOT NULL DEFAULT 0,
  "creator"      VARCHAR(64) DEFAULT '',
  "create_time"  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater"      VARCHAR(64) DEFAULT '',
  "update_time"  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted"      SMALLINT     NOT NULL DEFAULT 0,
  "tenant_id"    BIGINT       NOT NULL DEFAULT 0,
  CONSTRAINT "spk_ipd_governance_audit_pkey" PRIMARY KEY ("id")
);
CREATE INDEX IF NOT EXISTS "idx_spk_ipd_governance_audit_ref" ON "spk_ipd_governance_audit" ("action_type","ref_id");

-- ---------- ActivityDef 追加 required_artifacts 列（§9.3 治理字段） ----------
-- 该 Activity 产出/消费的必需制品类型列表（JSON 数组文本），供 Profile 引用做门禁校验。
ALTER TABLE "spk_ipd_activity_def" ADD COLUMN IF NOT EXISTS "required_artifacts" TEXT;
