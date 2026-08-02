-- ============================================================
-- MES 模块表 DDL（PostgreSQL）— 从本地 DO 反推
-- ============================================================
-- 来源：从 yudao-module-mes 的 *DO.java 反推生成（非社区 MySQL 脚本转换）
-- 子域：cal（假期/班组/班组成员）+ tm（工具台账/工具类型）
-- 数据库：PostgreSQL（docker-db_postgres-1 / 127.0.0.1:5433 / db=yudao）
-- 安全：仅 DROP IF EXISTS + CREATE IF NOT EXISTS（幂等）；无 INSERT、无外键约束、无 ENGINE
-- 约定：见同目录 ../_CONVENTIONS.md（tenant_id 全表必加、序列取 @KeySequence 字面值、JSON→text）
-- 运行：psql -1 -v ON_ERROR_STOP=1 -f cal_tm.sql
-- ============================================================

-- ===================== mes_cal_holiday =====================
-- MES 假期设置 DO
DROP TABLE IF EXISTS "mes_cal_holiday";
CREATE TABLE IF NOT EXISTS "mes_cal_holiday" (
    "id"          bigint      NOT NULL,
    "day"         timestamp   NULL DEFAULT NULL,
    "type"        int4        NULL DEFAULT NULL,
    "remark"      varchar(500) NULL DEFAULT NULL,
    "creator"     varchar(64) DEFAULT '',
    "create_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"     varchar(64) DEFAULT '',
    "update_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"     int2        NOT NULL DEFAULT 0,
    "tenant_id"   bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_cal_holiday" IS 'MES 假期设置 DO';
COMMENT ON COLUMN "mes_cal_holiday"."id" IS '编号';
COMMENT ON COLUMN "mes_cal_holiday"."day" IS '日期';
COMMENT ON COLUMN "mes_cal_holiday"."type" IS '日期类型';
COMMENT ON COLUMN "mes_cal_holiday"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS "mes_cal_holiday_seq";

-- ===================== mes_cal_team =====================
-- MES 班组 DO
DROP TABLE IF EXISTS "mes_cal_team";
CREATE TABLE IF NOT EXISTS "mes_cal_team" (
    "id"            bigint      NOT NULL,
    "code"          varchar(32) NULL DEFAULT NULL,
    "name"          varchar(128) NULL DEFAULT NULL,
    "calendar_type" int4        NULL DEFAULT NULL,
    "remark"        varchar(500) NULL DEFAULT NULL,
    "creator"       varchar(64) DEFAULT '',
    "create_time"   timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"       varchar(64) DEFAULT '',
    "update_time"   timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"       int2        NOT NULL DEFAULT 0,
    "tenant_id"     bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_cal_team" IS 'MES 班组 DO';
COMMENT ON COLUMN "mes_cal_team"."id" IS '班组编号';
COMMENT ON COLUMN "mes_cal_team"."code" IS '班组编码';
COMMENT ON COLUMN "mes_cal_team"."name" IS '班组名称';
COMMENT ON COLUMN "mes_cal_team"."calendar_type" IS '班组类型';
COMMENT ON COLUMN "mes_cal_team"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS "mes_cal_team_seq";

CREATE UNIQUE INDEX IF NOT EXISTS "idx_mes_cal_team_code" ON "mes_cal_team" ("code", tenant_id) WHERE deleted = 0;

-- ===================== mes_cal_team_member =====================
-- MES 班组成员 DO
DROP TABLE IF EXISTS "mes_cal_team_member";
CREATE TABLE IF NOT EXISTS "mes_cal_team_member" (
    "id"          bigint      NOT NULL,
    "team_id"     bigint      NULL DEFAULT NULL,
    "user_id"     bigint      NULL DEFAULT NULL,
    "remark"      varchar(500) NULL DEFAULT NULL,
    "creator"     varchar(64) DEFAULT '',
    "create_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"     varchar(64) DEFAULT '',
    "update_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"     int2        NOT NULL DEFAULT 0,
    "tenant_id"   bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_cal_team_member" IS 'MES 班组成员 DO';
COMMENT ON COLUMN "mes_cal_team_member"."id" IS '班组成员编号';
COMMENT ON COLUMN "mes_cal_team_member"."team_id" IS '班组编号';
COMMENT ON COLUMN "mes_cal_team_member"."user_id" IS '用户编号';
COMMENT ON COLUMN "mes_cal_team_member"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS "mes_cal_team_member_seq";

CREATE INDEX IF NOT EXISTS "idx_mes_cal_team_member_team_id" ON "mes_cal_team_member" ("team_id");
CREATE INDEX IF NOT EXISTS "idx_mes_cal_team_member_user_id" ON "mes_cal_team_member" ("user_id");

-- ===================== mes_tm_tool =====================
-- MES 工具台账 DO
DROP TABLE IF EXISTS "mes_tm_tool";
CREATE TABLE IF NOT EXISTS "mes_tm_tool" (
    "id"                 bigint      NOT NULL,
    "code"               varchar(32) NULL DEFAULT NULL,
    "name"               varchar(128) NULL DEFAULT NULL,
    "brand"              varchar(128) NULL DEFAULT NULL,
    "specification"      varchar(255) NULL DEFAULT NULL,
    "tool_type_id"       bigint      NULL DEFAULT NULL,
    "quantity"           int4        NULL DEFAULT NULL,
    "available_quantity" int4        NULL DEFAULT NULL,
    "mainten_type"       int4        NULL DEFAULT NULL,
    "next_mainten_period" int4       NULL DEFAULT NULL,
    "next_mainten_date"  timestamp   NULL DEFAULT NULL,
    "status"             int4        NULL DEFAULT NULL,
    "remark"             varchar(500) NULL DEFAULT NULL,
    "creator"            varchar(64) DEFAULT '',
    "create_time"        timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"            varchar(64) DEFAULT '',
    "update_time"        timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"            int2        NOT NULL DEFAULT 0,
    "tenant_id"          bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_tm_tool" IS 'MES 工具台账 DO';
COMMENT ON COLUMN "mes_tm_tool"."id" IS '编号';
COMMENT ON COLUMN "mes_tm_tool"."code" IS '工具编码';
COMMENT ON COLUMN "mes_tm_tool"."name" IS '工具名称';
COMMENT ON COLUMN "mes_tm_tool"."brand" IS '品牌';
COMMENT ON COLUMN "mes_tm_tool"."specification" IS '型号规格';
COMMENT ON COLUMN "mes_tm_tool"."tool_type_id" IS '工具类型编号';
COMMENT ON COLUMN "mes_tm_tool"."quantity" IS '数量';
COMMENT ON COLUMN "mes_tm_tool"."available_quantity" IS '可用数量';
COMMENT ON COLUMN "mes_tm_tool"."mainten_type" IS '保养维护类型';
COMMENT ON COLUMN "mes_tm_tool"."next_mainten_period" IS '下次保养周期（次数）';
COMMENT ON COLUMN "mes_tm_tool"."next_mainten_date" IS '下次保养日期';
COMMENT ON COLUMN "mes_tm_tool"."status" IS '状态';
COMMENT ON COLUMN "mes_tm_tool"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS "mes_tm_tool_seq";

CREATE UNIQUE INDEX IF NOT EXISTS "idx_mes_tm_tool_code" ON "mes_tm_tool" ("code", tenant_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS "idx_mes_tm_tool_tool_type_id" ON "mes_tm_tool" ("tool_type_id");

-- ===================== mes_tm_tool_type =====================
-- MES 工具类型 DO
DROP TABLE IF EXISTS "mes_tm_tool_type";
CREATE TABLE IF NOT EXISTS "mes_tm_tool_type" (
    "id"             bigint      NOT NULL,
    "code"           varchar(32) NULL DEFAULT NULL,
    "name"           varchar(128) NULL DEFAULT NULL,
    "code_flag"      int2        NOT NULL DEFAULT 0,
    "mainten_type"   int4        NULL DEFAULT NULL,
    "mainten_period" int4        NULL DEFAULT NULL,
    "remark"         varchar(500) NULL DEFAULT NULL,
    "creator"        varchar(64) DEFAULT '',
    "create_time"    timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"        varchar(64) DEFAULT '',
    "update_time"    timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"        int2        NOT NULL DEFAULT 0,
    "tenant_id"      bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_tm_tool_type" IS 'MES 工具类型 DO';
COMMENT ON COLUMN "mes_tm_tool_type"."id" IS '编号';
COMMENT ON COLUMN "mes_tm_tool_type"."code" IS '类型编码';
COMMENT ON COLUMN "mes_tm_tool_type"."name" IS '类型名称';
COMMENT ON COLUMN "mes_tm_tool_type"."code_flag" IS '是否编码管理';
COMMENT ON COLUMN "mes_tm_tool_type"."mainten_type" IS '保养维护类型';
COMMENT ON COLUMN "mes_tm_tool_type"."mainten_period" IS '保养周期';
COMMENT ON COLUMN "mes_tm_tool_type"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS "mes_tm_tool_type_seq";

CREATE UNIQUE INDEX IF NOT EXISTS "idx_mes_tm_tool_type_code" ON "mes_tm_tool_type" ("code", tenant_id) WHERE deleted = 0;
