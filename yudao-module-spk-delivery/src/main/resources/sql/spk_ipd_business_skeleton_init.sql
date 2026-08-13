-- =====================================================================
-- SPK-OS Cortext-IPD Core 业务骨架建表（S1 / M1）
-- 依 SPK-OS-Cortext-IPD-Core-UCD&API.md section 9.3-9.4 / 9.6
-- 4 级业务模型：Project -> MajorRelease(Vx) -> Version(Vx.ss) -> FlowRun
--   + IssueCase/IssueVersionRel（问题流）+ LegacyMapping（旧实例映射）+ CommandLog（幂等命令）
-- 幂等：CREATE TABLE IF NOT EXISTS + CREATE SEQUENCE IF NOT EXISTS，重跑不破坏数据。
-- PG 约定（与 spk_ipd_activity_def 一致）：
--   id bigint 无 default（由 @KeySequence nextval 注入）；deleted smallint default 0；
--   tenant_id bigint default 0；时间 timestamp without time zone；JSON 存 text。
-- tenant_id=0 是默认值，运行时 TenantDatabaseInterceptor 自动注入当前租户；psql 直插须显式写 1。
-- =====================================================================

CREATE SEQUENCE IF NOT EXISTS spk_ipd_project_seq;
CREATE SEQUENCE IF NOT EXISTS spk_ipd_major_release_seq;
CREATE SEQUENCE IF NOT EXISTS spk_ipd_version_seq;
CREATE SEQUENCE IF NOT EXISTS spk_ipd_flow_run_seq;
CREATE SEQUENCE IF NOT EXISTS spk_ipd_issue_case_seq;
CREATE SEQUENCE IF NOT EXISTS spk_ipd_issue_version_rel_seq;
CREATE SEQUENCE IF NOT EXISTS spk_ipd_legacy_mapping_seq;
CREATE SEQUENCE IF NOT EXISTS spk_ipd_command_log_seq;

-- ---------- 9.3.1 spk_ipd_project 项目 ----------
CREATE TABLE IF NOT EXISTS "spk_ipd_project" (
  "id"                        BIGINT       NOT NULL,
  "project_no"                VARCHAR(32)  NOT NULL,
  "project_code"              VARCHAR(64) NOT NULL,
  "name"                      VARCHAR(128) NOT NULL,
  "description"               TEXT,
  "objective"                 TEXT         NOT NULL,
  "owner_user_id"            BIGINT       NOT NULL,
  "status"                    VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
  "health"                    VARCHAR(16) NOT NULL DEFAULT 'UNKNOWN',
  "planned_start_at"         TIMESTAMP,
  "planned_end_at"           TIMESTAMP,
  "actual_start_at"          TIMESTAMP,
  "actual_end_at"            TIMESTAMP,
  "current_major_release_id" BIGINT,
  "lock_version"             INT          NOT NULL DEFAULT 0,
  "creator"                  VARCHAR(64) DEFAULT '',
  "create_time"              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater"                  VARCHAR(64) DEFAULT '',
  "update_time"              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted"                  SMALLINT     NOT NULL DEFAULT 0,
  "tenant_id"                BIGINT       NOT NULL DEFAULT 0,
  CONSTRAINT "spk_ipd_project_pkey" PRIMARY KEY ("id")
);
CREATE UNIQUE INDEX IF NOT EXISTS "uk_spk_ipd_project_no"     ON "spk_ipd_project" ("tenant_id","project_no") WHERE "deleted" = 0;
CREATE UNIQUE INDEX IF NOT EXISTS "uk_spk_ipd_project_code"  ON "spk_ipd_project" ("tenant_id","project_code") WHERE "deleted" = 0;

-- ---------- 9.3.2 spk_ipd_major_release 大版本 Vx ----------
CREATE TABLE IF NOT EXISTS "spk_ipd_major_release" (
  "id"                   BIGINT       NOT NULL,
  "project_id"           BIGINT       NOT NULL,
  "major_no"             INT          NOT NULL,
  "version_label"        VARCHAR(24)  NOT NULL,
  "name"                 VARCHAR(128) NOT NULL,
  "objective"            TEXT         NOT NULL,
  "scope_summary"        TEXT,
  "owner_user_id"        BIGINT       NOT NULL,
  "status"               VARCHAR(24) NOT NULL DEFAULT 'PLANNING',
  "baseline_version_id"  BIGINT,
  "planned_start_at"     TIMESTAMP,
  "planned_end_at"       TIMESTAMP,
  "actual_start_at"      TIMESTAMP,
  "actual_end_at"        TIMESTAMP,
  "lock_version"         INT          NOT NULL DEFAULT 0,
  "creator"              VARCHAR(64) DEFAULT '',
  "create_time"          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater"              VARCHAR(64) DEFAULT '',
  "update_time"          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted"              SMALLINT     NOT NULL DEFAULT 0,
  "tenant_id"            BIGINT       NOT NULL DEFAULT 0,
  CONSTRAINT "spk_ipd_major_release_pkey" PRIMARY KEY ("id")
);
CREATE UNIQUE INDEX IF NOT EXISTS "uk_spk_ipd_major_rel" ON "spk_ipd_major_release" ("tenant_id","project_id","major_no") WHERE "deleted" = 0;
CREATE INDEX IF NOT EXISTS "idx_spk_ipd_major_project" ON "spk_ipd_major_release" ("project_id");

-- ---------- 9.3.3 spk_ipd_version 交付版本 Vx.ss ----------
CREATE TABLE IF NOT EXISTS "spk_ipd_version" (
  "id"                  BIGINT       NOT NULL,
  "project_id"          BIGINT       NOT NULL,
  "major_release_id"    BIGINT       NOT NULL,
  "major_no"            INT          NOT NULL,
  "minor_no"            INT          NOT NULL,
  "version_no"          VARCHAR(24)  NOT NULL,
  "version_type"        VARCHAR(24)  NOT NULL,
  "baseline_flag"       SMALLINT    NOT NULL DEFAULT 0,
  "name"                VARCHAR(128),
  "objective"           TEXT         NOT NULL,
  "scope_summary"       TEXT,
  "owner_user_id"       BIGINT       NOT NULL,
  "status"              VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
  "delivery_readiness"  VARCHAR(24) NOT NULL DEFAULT 'NOT_READY',
  "health"              VARCHAR(16) NOT NULL DEFAULT 'UNKNOWN',
  "planned_start_at"    TIMESTAMP,
  "planned_end_at"      TIMESTAMP,
  "actual_start_at"     TIMESTAMP,
  "actual_end_at"       TIMESTAMP,
  "released_at"         TIMESTAMP,
  "source_version_id"   BIGINT,
  "lock_version"        INT          NOT NULL DEFAULT 0,
  "creator"             VARCHAR(64) DEFAULT '',
  "create_time"         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater"             VARCHAR(64) DEFAULT '',
  "update_time"         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted"             SMALLINT     NOT NULL DEFAULT 0,
  "tenant_id"           BIGINT       NOT NULL DEFAULT 0,
  CONSTRAINT "spk_ipd_version_pkey" PRIMARY KEY ("id")
);
CREATE UNIQUE INDEX IF NOT EXISTS "uk_spk_ipd_version_no" ON "spk_ipd_version" ("tenant_id","project_id","major_no","minor_no") WHERE "deleted" = 0;
CREATE INDEX IF NOT EXISTS "idx_spk_ipd_version_major" ON "spk_ipd_version" ("major_release_id");

-- ---------- 9.3.4 spk_ipd_issue_case 问题 ----------
CREATE TABLE IF NOT EXISTS "spk_ipd_issue_case" (
  "id"               BIGINT       NOT NULL,
  "case_no"          VARCHAR(32)  NOT NULL,
  "project_id"       BIGINT       NOT NULL,
  "issue_type"       VARCHAR(24)  NOT NULL,
  "severity"         VARCHAR(8)   NOT NULL,
  "title"            VARCHAR(255) NOT NULL,
  "description"      TEXT,
  "source"           VARCHAR(24)  NOT NULL DEFAULT 'MANUAL',
  "external_system"  VARCHAR(32),
  "external_id"      VARCHAR(64),
  "external_url"     VARCHAR(512),
  "owner_user_id"    BIGINT,
  "status"           VARCHAR(24)  NOT NULL DEFAULT 'OPEN',
  "root_cause"       TEXT,
  "resolution"       TEXT,
  "detected_at"      TIMESTAMP,
  "resolved_at"      TIMESTAMP,
  "closed_at"        TIMESTAMP,
  "lock_version"     INT          NOT NULL DEFAULT 0,
  "creator"          VARCHAR(64) DEFAULT '',
  "create_time"      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater"          VARCHAR(64) DEFAULT '',
  "update_time"      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted"          SMALLINT     NOT NULL DEFAULT 0,
  "tenant_id"        BIGINT       NOT NULL DEFAULT 0,
  CONSTRAINT "spk_ipd_issue_case_pkey" PRIMARY KEY ("id")
);
CREATE UNIQUE INDEX IF NOT EXISTS "uk_spk_ipd_issue_no" ON "spk_ipd_issue_case" ("tenant_id","case_no") WHERE "deleted" = 0;
CREATE INDEX IF NOT EXISTS "idx_spk_ipd_issue_project" ON "spk_ipd_issue_case" ("project_id");

CREATE TABLE IF NOT EXISTS "spk_ipd_issue_version_rel" (
  "id"             BIGINT      NOT NULL,
  "issue_case_id"  BIGINT      NOT NULL,
  "version_id"     BIGINT      NOT NULL,
  "relation_type"  VARCHAR(24) NOT NULL,
  "creator"        VARCHAR(64) DEFAULT '',
  "create_time"    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater"        VARCHAR(64) DEFAULT '',
  "update_time"    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted"        SMALLINT    NOT NULL DEFAULT 0,
  "tenant_id"      BIGINT      NOT NULL DEFAULT 0,
  CONSTRAINT "spk_ipd_issue_version_rel_pkey" PRIMARY KEY ("id")
);
CREATE UNIQUE INDEX IF NOT EXISTS "uk_spk_ipd_issue_ver_rel" ON "spk_ipd_issue_version_rel" ("tenant_id","issue_case_id","version_id","relation_type") WHERE "deleted" = 0;

-- ---------- 9.4.2 spk_ipd_flow_run 流程运行 ----------
CREATE TABLE IF NOT EXISTS "spk_ipd_flow_run" (
  "id"                      BIGINT       NOT NULL,
  "run_no"                  VARCHAR(32)  NOT NULL,
  "project_id"              BIGINT       NOT NULL,
  "major_release_id"        BIGINT,
  "version_id"             BIGINT,
  "issue_case_id"           BIGINT,
  "flow_type"              VARCHAR(32)  NOT NULL,
  "profile_id"             BIGINT       NOT NULL,
  "profile_version"        INT          NOT NULL,
  "profile_snapshot_json"  TEXT         NOT NULL,
  "tailoring_snapshot_json" TEXT,
  "business_key"           VARCHAR(128) NOT NULL,
  "process_instance_id"    VARCHAR(64),
  "status"                 VARCHAR(24)  NOT NULL DEFAULT 'DRAFT',
  "current_stage"          VARCHAR(32),
  "current_activity"       VARCHAR(64),
  "health"                 VARCHAR(16) NOT NULL DEFAULT 'UNKNOWN',
  "block_reason"           TEXT,
  "attempt_no"             INT          NOT NULL DEFAULT 1,
  "supersedes_flow_run_id" BIGINT,
  "planned_start_at"       TIMESTAMP,
  "planned_end_at"         TIMESTAMP,
  "started_at"             TIMESTAMP,
  "ended_at"               TIMESTAMP,
  "last_engine_sync_at"    TIMESTAMP,
  "lock_version"            INT          NOT NULL DEFAULT 0,
  "creator"                VARCHAR(64) DEFAULT '',
  "create_time"            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater"                VARCHAR(64) DEFAULT '',
  "update_time"            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted"                SMALLINT     NOT NULL DEFAULT 0,
  "tenant_id"              BIGINT       NOT NULL DEFAULT 0,
  CONSTRAINT "spk_ipd_flow_run_pkey" PRIMARY KEY ("id")
);
CREATE UNIQUE INDEX IF NOT EXISTS "uk_spk_ipd_flow_run_no"    ON "spk_ipd_flow_run" ("run_no") WHERE "deleted" = 0;
CREATE UNIQUE INDEX IF NOT EXISTS "uk_spk_ipd_flow_run_pid"  ON "spk_ipd_flow_run" ("process_instance_id") WHERE "deleted" = 0 AND "process_instance_id" IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS "uk_spk_ipd_flow_run_bk"   ON "spk_ipd_flow_run" ("tenant_id","business_key") WHERE "deleted" = 0;
CREATE INDEX IF NOT EXISTS "idx_spk_ipd_flow_run_ver" ON "spk_ipd_flow_run" ("project_id","version_id","status");
CREATE INDEX IF NOT EXISTS "idx_spk_ipd_flow_run_issue" ON "spk_ipd_flow_run" ("issue_case_id","status");

-- ---------- 9.6 spk_ipd_command_log 幂等命令审计 ----------
CREATE TABLE IF NOT EXISTS "spk_ipd_command_log" (
  "id"               BIGINT       NOT NULL,
  "idempotency_key"  VARCHAR(128) NOT NULL,
  "command_type"     VARCHAR(48)  NOT NULL,
  "target_type"      VARCHAR(32)  NOT NULL,
  "target_id"        VARCHAR(64),
  "payload_hash"     VARCHAR(64)  NOT NULL,
  "payload_json"     TEXT,
  "status"           VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
  "result_json"      TEXT,
  "error_code"       VARCHAR(64),
  "error_msg"        TEXT,
  "requested_by"     BIGINT,
  "requested_at"     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "finished_at"      TIMESTAMP,
  "creator"          VARCHAR(64) DEFAULT '',
  "create_time"      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater"          VARCHAR(64) DEFAULT '',
  "update_time"      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted"          SMALLINT     NOT NULL DEFAULT 0,
  "tenant_id"        BIGINT       NOT NULL DEFAULT 0,
  CONSTRAINT "spk_ipd_command_log_pkey" PRIMARY KEY ("id")
);
CREATE UNIQUE INDEX IF NOT EXISTS "uk_spk_ipd_cmd_idem" ON "spk_ipd_command_log" ("tenant_id","idempotency_key") WHERE "deleted" = 0;

-- ---------- 9.6 spk_ipd_legacy_mapping 旧实例映射 ----------
CREATE TABLE IF NOT EXISTS "spk_ipd_legacy_mapping" (
  "id"             BIGINT       NOT NULL,
  "legacy_type"    VARCHAR(32)  NOT NULL,
  "legacy_id"      VARCHAR(64)  NOT NULL,
  "project_id"     BIGINT,
  "version_id"     BIGINT,
  "flow_run_id"    BIGINT,
  "mapping_status" VARCHAR(24)  NOT NULL DEFAULT 'NEEDS_MAPPING',
  "rule_version"   VARCHAR(16),
  "confidence"     VARCHAR(16),
  "evidence_json"  TEXT,
  "confirmed_by"   BIGINT,
  "confirmed_at"   TIMESTAMP,
  "creator"        VARCHAR(64) DEFAULT '',
  "create_time"    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater"        VARCHAR(64) DEFAULT '',
  "update_time"    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted"        SMALLINT     NOT NULL DEFAULT 0,
  "tenant_id"      BIGINT       NOT NULL DEFAULT 0,
  CONSTRAINT "spk_ipd_legacy_mapping_pkey" PRIMARY KEY ("id")
);
CREATE UNIQUE INDEX IF NOT EXISTS "uk_spk_ipd_legacy_map" ON "spk_ipd_legacy_mapping" ("tenant_id","legacy_type","legacy_id") WHERE "deleted" = 0;
CREATE INDEX IF NOT EXISTS "idx_spk_ipd_legacy_project" ON "spk_ipd_legacy_mapping" ("project_id");




