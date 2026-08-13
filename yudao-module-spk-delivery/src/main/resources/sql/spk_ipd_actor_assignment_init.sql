-- =====================================================================
-- SPK-OS Cortext-IPD 参与者与分派建表（S5 / 设计文档 §9.5）
-- spk_ipd_project_actor : 项目/版本作用域下人/Agent/编队的统一参与关系
-- spk_ipd_assignment     : 将流程活动/Plane issue/BPM task/Agent task 分派给执行者
-- 幂等：CREATE TABLE IF NOT EXISTS + CREATE SEQUENCE IF NOT EXISTS，重跑不破坏数据。
-- PG 约定同 spk_ipd_business_skeleton_init.sql。
-- =====================================================================

CREATE SEQUENCE IF NOT EXISTS spk_ipd_project_actor_seq;
CREATE SEQUENCE IF NOT EXISTS spk_ipd_assignment_seq;

-- ---------- 9.5.1 spk_ipd_project_actor 项目参与者 ----------
CREATE TABLE IF NOT EXISTS "spk_ipd_project_actor" (
  "id"                BIGINT       NOT NULL,
  "project_id"        BIGINT       NOT NULL,
  "version_id"        BIGINT,
  "actor_type"        VARCHAR(16)  NOT NULL,            -- HUMAN/AGENT/SQUAD/SYSTEM
  "actor_id"          BIGINT       NOT NULL,           -- 系统用户ID / agentDefId / squadId
  "business_role"     VARCHAR(64)  NOT NULL,           -- IPD-PM/PO/ARCH/DEV/QA/RELEASE 等
  "accountable_flag"  SMALLINT     NOT NULL DEFAULT 0, -- 1=该作用域该角色的唯一 accountable
  "capacity_pct"      INT          NOT NULL DEFAULT 100,
  "effective_from"    TIMESTAMP,
  "effective_to"      TIMESTAMP,
  "status"            VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
  "lock_version"      INT          NOT NULL DEFAULT 0,
  "creator"           VARCHAR(64)  DEFAULT '',
  "create_time"       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater"           VARCHAR(64)  DEFAULT '',
  "update_time"       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted"           SMALLINT     NOT NULL DEFAULT 0,
  "tenant_id"         BIGINT       NOT NULL DEFAULT 0,
  CONSTRAINT "spk_ipd_project_actor_pkey" PRIMARY KEY ("id")
);
CREATE INDEX IF NOT EXISTS "idx_spk_ipd_pa_proj"   ON "spk_ipd_project_actor" ("tenant_id","project_id","version_id") WHERE "deleted" = 0;
CREATE INDEX IF NOT EXISTS "idx_spk_ipd_pa_actor" ON "spk_ipd_project_actor" ("tenant_id","actor_type","actor_id")  WHERE "deleted" = 0;

-- ---------- 9.5.2 spk_ipd_assignment 任务分派 ----------
CREATE TABLE IF NOT EXISTS "spk_ipd_assignment" (
  "id"                       BIGINT       NOT NULL,
  "project_id"               BIGINT,
  "version_id"               BIGINT,
  "flow_run_id"              BIGINT       NOT NULL,
  "activity_run_id"          VARCHAR(64),
  "work_item_type"           VARCHAR(24)  NOT NULL,   -- ACTIVITY/PLANE_ISSUE/BPM_TASK/AGENT_TASK
  "work_item_id"             VARCHAR(128) NOT NULL,
  "actor_type"               VARCHAR(16)  NOT NULL,   -- 执行者类型
  "actor_id"                 BIGINT       NOT NULL,  -- 执行者 ID
  "accountable_actor_type"  VARCHAR(16),             -- 唯一责任人类型（BPM 审批任务必须 HUMAN）
  "accountable_actor_id"    BIGINT,
  "status"                   VARCHAR(24) NOT NULL DEFAULT 'PLANNED', -- PLANNED/ASSIGNED/ACCEPTED/IN_PROGRESS/BLOCKED/DONE/CANCELLED
  "planned_effort"           DECIMAL(8,2),
  "actual_effort"            DECIMAL(8,2),
  "lock_version"             INT          NOT NULL DEFAULT 0,
  "creator"                  VARCHAR(64)  DEFAULT '',
  "create_time"              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater"                  VARCHAR(64)  DEFAULT '',
  "update_time"              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted"                  SMALLINT     NOT NULL DEFAULT 0,
  "tenant_id"                BIGINT       NOT NULL DEFAULT 0,
  CONSTRAINT "spk_ipd_assignment_pkey" PRIMARY KEY ("id")
);
CREATE INDEX IF NOT EXISTS "idx_spk_ipd_asg_flow"  ON "spk_ipd_assignment" ("tenant_id","flow_run_id")         WHERE "deleted" = 0;
CREATE INDEX IF NOT EXISTS "idx_spk_ipd_asg_proj"  ON "spk_ipd_assignment" ("tenant_id","project_id")         WHERE "deleted" = 0;
CREATE INDEX IF NOT EXISTS "idx_spk_ipd_asg_actor" ON "spk_ipd_assignment" ("tenant_id","actor_type","actor_id") WHERE "deleted" = 0;
