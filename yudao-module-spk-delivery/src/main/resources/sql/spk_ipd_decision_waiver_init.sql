-- =====================================================================
-- SPK-OS Cortext-IPD 审批与决策包（设计文档 §10.8 / §7.9）
-- spk_ipd_decision_record : 一次 APPROVE/REJECT/REDIRECT/RETURN 的不可变审计行（包装原生 BPM approve/reject/return）
-- spk_ipd_evidence_waiver : 必需产物/证据缺失时经授权的例外豁免
-- spk_ipd_work_item_link  : Plane issue → 版本/流程/Activity 绑定映射（设计文档 §10.7）
-- 幂等：CREATE TABLE IF NOT EXISTS + CREATE SEQUENCE IF NOT EXISTS，重跑不破坏数据。
-- PG 约定同 spk_ipd_business_skeleton_init.sql：id bigint 无 default（@KeySequence 注入）；deleted smallint default 0；tenant_id bigint default 0；timestamp without time zone；JSON 存 text。
-- =====================================================================

CREATE SEQUENCE IF NOT EXISTS spk_ipd_decision_record_seq;
CREATE SEQUENCE IF NOT EXISTS spk_ipd_evidence_waiver_seq;
CREATE SEQUENCE IF NOT EXISTS spk_ipd_work_item_link_seq;

-- ---------- 决策记录 ----------
CREATE TABLE IF NOT EXISTS "spk_ipd_decision_record" (
  "id"                       BIGINT       NOT NULL,
  "task_id"                  VARCHAR(64)  NOT NULL,
  "process_instance_id"      VARCHAR(64)  NOT NULL,
  "flow_run_id"              BIGINT,
  "project_id"               BIGINT,
  "version_id"               BIGINT,
  "decision"                 VARCHAR(16)  NOT NULL,            -- APPROVE/REJECT/REDIRECT/RETURN
  "reason"                   TEXT,
  "conditions_json"          TEXT,                              -- 跟进项 JSON 数组
  "decision_package_hash"    VARCHAR(128),                      -- sha256:... 决策包快照 hash
  "flowable_variables_json"  TEXT,                              -- 写入流程引擎的变量
  "redirect_target_task_key" VARCHAR(64),                       -- REDIRECT/RETURN 目标节点 key
  "decider_user_id"          BIGINT       NOT NULL,
  "lock_version"             INT          NOT NULL DEFAULT 0,
  "creator"                  VARCHAR(64)  DEFAULT '',
  "create_time"              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater"                  VARCHAR(64)  DEFAULT '',
  "update_time"              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted"                  SMALLINT     NOT NULL DEFAULT 0,
  "tenant_id"                BIGINT       NOT NULL DEFAULT 0,
  CONSTRAINT "spk_ipd_decision_record_pkey" PRIMARY KEY ("id")
);
CREATE INDEX IF NOT EXISTS "idx_spk_ipd_dr_task"      ON "spk_ipd_decision_record" ("task_id")                 WHERE "deleted" = 0;
CREATE INDEX IF NOT EXISTS "idx_spk_ipd_dr_instance"  ON "spk_ipd_decision_record" ("process_instance_id")   WHERE "deleted" = 0;
CREATE INDEX IF NOT EXISTS "idx_spk_ipd_dr_flowrun"   ON "spk_ipd_decision_record" ("flow_run_id")           WHERE "deleted" = 0;
CREATE INDEX IF NOT EXISTS "idx_spk_ipd_dr_project"   ON "spk_ipd_decision_record" ("project_id")            WHERE "deleted" = 0;

-- ---------- 证据豁免 ----------
CREATE TABLE IF NOT EXISTS "spk_ipd_evidence_waiver" (
  "id"                   BIGINT       NOT NULL,
  "task_id"              VARCHAR(64),
  "process_instance_id"  VARCHAR(64)  NOT NULL,
  "flow_run_id"          BIGINT,
  "project_id"           BIGINT,
  "version_id"           BIGINT,
  "evidence_ref"         VARCHAR(128) NOT NULL,            -- 缺失证据/产物引用
  "evidence_type"         VARCHAR(64),                      -- ARTIFACT/GATE/TR/TEST/RELEASE
  "due_at"               TIMESTAMP,                         -- 补证截止
  "reason"               TEXT         NOT NULL,             -- 豁免理由
  "owner_user_id"        BIGINT,                             -- 补证责任人
  "granted_by_user_id"   BIGINT       NOT NULL,             -- 授权人
  "status"               VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE', -- ACTIVE/EXPIRED/RESOLVED
  "lock_version"         INT          NOT NULL DEFAULT 0,
  "creator"              VARCHAR(64)  DEFAULT '',
  "create_time"          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater"              VARCHAR(64)  DEFAULT '',
  "update_time"          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted"              SMALLINT     NOT NULL DEFAULT 0,
  "tenant_id"            BIGINT       NOT NULL DEFAULT 0,
  CONSTRAINT "spk_ipd_evidence_waiver_pkey" PRIMARY KEY ("id")
);
CREATE INDEX IF NOT EXISTS "idx_spk_ipd_ew_task"      ON "spk_ipd_evidence_waiver" ("task_id")               WHERE "deleted" = 0;
CREATE INDEX IF NOT EXISTS "idx_spk_ipd_ew_instance"  ON "spk_ipd_evidence_waiver" ("process_instance_id")  WHERE "deleted" = 0;
CREATE INDEX IF NOT EXISTS "idx_spk_ipd_ew_flowrun"  ON "spk_ipd_evidence_waiver" ("flow_run_id")           WHERE "deleted" = 0;

-- ---------- Plane 工作项绑定 ----------
CREATE TABLE IF NOT EXISTS "spk_ipd_work_item_link" (
  "id"                  BIGINT       NOT NULL,
  "project_id"          BIGINT       NOT NULL,
  "version_id"          BIGINT,
  "flow_run_id"         BIGINT,
  "activity_code"       VARCHAR(64),
  "plane_workspace_id"  VARCHAR(64),
  "plane_project_id"    VARCHAR(64),
  "plane_issue_id"      VARCHAR(64)  NOT NULL,
  "plane_issue_seq"     VARCHAR(64),
  "link_type"           VARCHAR(16) NOT NULL DEFAULT 'REQUIREMENT', -- REQUIREMENT/TASK/DEFECT/MILESTONE
  "sync_status"         VARCHAR(16) NOT NULL DEFAULT 'PENDING',     -- PENDING/LINKED/SYNCED/ORPHAN
  "last_snapshot_json"  TEXT,
  "last_synced_at"      TIMESTAMP,
  "creator"             VARCHAR(64)  DEFAULT '',
  "create_time"         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater"             VARCHAR(64)  DEFAULT '',
  "update_time"         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted"             SMALLINT     NOT NULL DEFAULT 0,
  "tenant_id"           BIGINT       NOT NULL DEFAULT 1,
  CONSTRAINT "spk_ipd_work_item_link_pkey" PRIMARY KEY ("id")
);
CREATE INDEX IF NOT EXISTS "idx_spk_ipd_wil_project" ON "spk_ipd_work_item_link" ("project_id") WHERE "deleted" = 0;
CREATE INDEX IF NOT EXISTS "idx_spk_ipd_wil_issue"   ON "spk_ipd_work_item_link" ("project_id", "plane_issue_id") WHERE "deleted" = 0;
