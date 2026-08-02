-- ============================================================
-- MES 模块补丁表 DDL（PostgreSQL）— 从本地 DO 反推，8 子域合并
-- ============================================================
-- 来源：从 yudao-module-mes 的 *DO.java 反推生成（非社区 MySQL 脚本转换）
-- 数据库：PostgreSQL（docker-db_postgres-1 / 127.0.0.1:5433 / db=yudao）
-- 背景：MES 模块 100 张表在 postgres 库缺失，前端 MES 页面报"服务器错误"
-- 安全：仅 DROP IF EXISTS + CREATE IF NOT EXISTS（幂等）；无 INSERT、无外键约束、无 ENGINE
-- 约定：见同目录 ../_CONVENTIONS.md（tenant_id 全表必加、序列取 @KeySequence 字面值、JSON→text）
-- 子域：cal/tm(5) dv(12) md(14) pro(14) qc(10) wm-a(19) wm-b(13) wm-c(13) = 100
-- ============================================================


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


-- ============================================================
-- MES 模块表 DDL（PostgreSQL）— 从本地 DO 反推
-- ============================================================
-- 来源：从 yudao-module-mes 的 *DO.java 反推生成（非社区 MySQL 脚本转换）
-- 子域：dv（设备点检/保养/维修/类型/项目）
-- 数据库：PostgreSQL（docker-db_postgres-1 / 127.0.0.1:5433 / db=yudao）
-- 安全：仅 DROP IF EXISTS + CREATE IF NOT EXISTS（幂等）；无 INSERT、无外键约束、无 ENGINE
-- 约定：见同目录 ../_CONVENTIONS.md（tenant_id 全表必加、序列取 @KeySequence 字面值、JSON→text）
-- 运行：psql -1 -v ON_ERROR_STOP=1 -f dv.sql
-- ============================================================

-- ===================== mes_dv_check_plan =====================
-- MES 点检保养方案 DO
DROP TABLE IF EXISTS mes_dv_check_plan;
CREATE TABLE IF NOT EXISTS mes_dv_check_plan (
    id              bigint      NOT NULL,
    code            varchar(64),
    name            varchar(128),
    type            int4,
    start_date      timestamp,
    end_date        timestamp,
    cycle_type      int4,
    cycle_count     int4,
    status          int4,
    remark          varchar(500),
    creator         varchar(64) DEFAULT '',
    create_time     timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         varchar(64) DEFAULT '',
    update_time     timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         int2        NOT NULL DEFAULT 0,
    tenant_id       bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

COMMENT ON TABLE mes_dv_check_plan IS 'MES 点检保养方案 DO';
COMMENT ON COLUMN mes_dv_check_plan.code IS '方案编码';
COMMENT ON COLUMN mes_dv_check_plan.name IS '方案名称';
COMMENT ON COLUMN mes_dv_check_plan.type IS '方案类型（字典 MES_DV_SUBJECT_TYPE）';
COMMENT ON COLUMN mes_dv_check_plan.start_date IS '开始日期';
COMMENT ON COLUMN mes_dv_check_plan.end_date IS '结束日期';
COMMENT ON COLUMN mes_dv_check_plan.cycle_type IS '周期类型（字典 MES_DV_CYCLE_TYPE）';
COMMENT ON COLUMN mes_dv_check_plan.cycle_count IS '周期数量';
COMMENT ON COLUMN mes_dv_check_plan.status IS '状态（字典 MES_DV_CHECK_PLAN_STATUS）';
COMMENT ON COLUMN mes_dv_check_plan.remark IS '备注';

CREATE SEQUENCE IF NOT EXISTS mes_dv_check_plan_seq;

-- ===================== mes_dv_check_plan_machinery =====================
-- MES 点检保养方案设备 DO
DROP TABLE IF EXISTS mes_dv_check_plan_machinery;
CREATE TABLE IF NOT EXISTS mes_dv_check_plan_machinery (
    id              bigint      NOT NULL,
    plan_id         bigint,
    machinery_id    bigint,
    remark          varchar(500),
    creator         varchar(64) DEFAULT '',
    create_time     timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         varchar(64) DEFAULT '',
    update_time     timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         int2        NOT NULL DEFAULT 0,
    tenant_id       bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

COMMENT ON TABLE mes_dv_check_plan_machinery IS 'MES 点检保养方案设备 DO';
COMMENT ON COLUMN mes_dv_check_plan_machinery.plan_id IS '方案编号（关联 mes_dv_check_plan.id）';
COMMENT ON COLUMN mes_dv_check_plan_machinery.machinery_id IS '设备编号（关联 mes_dv_machinery.id）';
COMMENT ON COLUMN mes_dv_check_plan_machinery.remark IS '备注';

CREATE SEQUENCE IF NOT EXISTS mes_dv_check_plan_machinery_seq;
CREATE INDEX IF NOT EXISTS idx_mes_dv_check_plan_machinery_plan_id ON mes_dv_check_plan_machinery (plan_id);

-- ===================== mes_dv_check_plan_subject =====================
-- MES 点检保养方案项目 DO
DROP TABLE IF EXISTS mes_dv_check_plan_subject;
CREATE TABLE IF NOT EXISTS mes_dv_check_plan_subject (
    id              bigint      NOT NULL,
    plan_id         bigint,
    subject_id      bigint,
    remark          varchar(500),
    creator         varchar(64) DEFAULT '',
    create_time     timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         varchar(64) DEFAULT '',
    update_time     timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         int2        NOT NULL DEFAULT 0,
    tenant_id       bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

COMMENT ON TABLE mes_dv_check_plan_subject IS 'MES 点检保养方案项目 DO';
COMMENT ON COLUMN mes_dv_check_plan_subject.plan_id IS '方案编号（关联 mes_dv_check_plan.id）';
COMMENT ON COLUMN mes_dv_check_plan_subject.subject_id IS '项目编号（关联 mes_dv_subject.id）';
COMMENT ON COLUMN mes_dv_check_plan_subject.remark IS '备注';

CREATE SEQUENCE IF NOT EXISTS mes_dv_check_plan_subject_seq;
CREATE INDEX IF NOT EXISTS idx_mes_dv_check_plan_subject_plan_id ON mes_dv_check_plan_subject (plan_id);

-- ===================== mes_dv_check_record =====================
-- MES 设备点检记录 DO
DROP TABLE IF EXISTS mes_dv_check_record;
CREATE TABLE IF NOT EXISTS mes_dv_check_record (
    id              bigint      NOT NULL,
    plan_id         bigint,
    machinery_id    bigint,
    check_time      timestamp,
    user_id         bigint,
    status          int4,
    remark          varchar(500),
    creator         varchar(64) DEFAULT '',
    create_time     timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         varchar(64) DEFAULT '',
    update_time     timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         int2        NOT NULL DEFAULT 0,
    tenant_id       bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

COMMENT ON TABLE mes_dv_check_record IS 'MES 设备点检记录 DO';
COMMENT ON COLUMN mes_dv_check_record.plan_id IS '点检计划编号（关联 mes_dv_check_plan.id）';
COMMENT ON COLUMN mes_dv_check_record.machinery_id IS '设备编号（关联 mes_dv_machinery.id）';
COMMENT ON COLUMN mes_dv_check_record.check_time IS '点检时间';
COMMENT ON COLUMN mes_dv_check_record.user_id IS '点检人编号（关联 AdminUserDO.id）';
COMMENT ON COLUMN mes_dv_check_record.status IS '状态（字典 MES_DV_CHECK_RECORD_STATUS）';
COMMENT ON COLUMN mes_dv_check_record.remark IS '备注';

CREATE SEQUENCE IF NOT EXISTS mes_dv_check_record_seq;
CREATE INDEX IF NOT EXISTS idx_mes_dv_check_record_plan_id ON mes_dv_check_record (plan_id);
CREATE INDEX IF NOT EXISTS idx_mes_dv_check_record_machinery_id ON mes_dv_check_record (machinery_id);

-- ===================== mes_dv_check_record_line =====================
-- MES 设备点检记录明细 DO
DROP TABLE IF EXISTS mes_dv_check_record_line;
CREATE TABLE IF NOT EXISTS mes_dv_check_record_line (
    id              bigint      NOT NULL,
    record_id       bigint,
    subject_id      bigint,
    check_status    int4,
    check_result    text,
    remark          varchar(500),
    creator         varchar(64) DEFAULT '',
    create_time     timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         varchar(64) DEFAULT '',
    update_time     timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         int2        NOT NULL DEFAULT 0,
    tenant_id       bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

COMMENT ON TABLE mes_dv_check_record_line IS 'MES 设备点检记录明细 DO';
COMMENT ON COLUMN mes_dv_check_record_line.record_id IS '点检记录编号（关联 mes_dv_check_record.id）';
COMMENT ON COLUMN mes_dv_check_record_line.subject_id IS '点检项目编号（关联 mes_dv_subject.id）';
COMMENT ON COLUMN mes_dv_check_record_line.check_status IS '点检结果（字典 MES_DV_CHECK_RESULT）';
COMMENT ON COLUMN mes_dv_check_record_line.check_result IS '异常描述（仅 check_status 为异常时使用）';
COMMENT ON COLUMN mes_dv_check_record_line.remark IS '备注';

CREATE SEQUENCE IF NOT EXISTS mes_dv_check_record_line_seq;
CREATE INDEX IF NOT EXISTS idx_mes_dv_check_record_line_record_id ON mes_dv_check_record_line (record_id);

-- ===================== mes_dv_machinery =====================
-- MES 设备台账 DO
DROP TABLE IF EXISTS mes_dv_machinery;
CREATE TABLE IF NOT EXISTS mes_dv_machinery (
    id                  bigint      NOT NULL,
    code                varchar(64),
    name                varchar(128),
    brand               varchar(128),
    specification       varchar(255),
    machinery_type_id   bigint,
    workshop_id         bigint,
    status              int4,
    last_mainten_time   timestamp,
    last_check_time     timestamp,
    remark              varchar(500),
    creator             varchar(64) DEFAULT '',
    create_time         timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater             varchar(64) DEFAULT '',
    update_time         timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             int2        NOT NULL DEFAULT 0,
    tenant_id           bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

COMMENT ON TABLE mes_dv_machinery IS 'MES 设备台账 DO';
COMMENT ON COLUMN mes_dv_machinery.code IS '设备编码';
COMMENT ON COLUMN mes_dv_machinery.name IS '设备名称';
COMMENT ON COLUMN mes_dv_machinery.brand IS '品牌';
COMMENT ON COLUMN mes_dv_machinery.specification IS '规格型号';
COMMENT ON COLUMN mes_dv_machinery.machinery_type_id IS '设备类型编号（关联 mes_dv_machinery_type.id）';
COMMENT ON COLUMN mes_dv_machinery.workshop_id IS '所属车间编号（关联 mes_md_workshop.id）';
COMMENT ON COLUMN mes_dv_machinery.status IS '设备状态（字典 MES_DV_MACHINERY_STATUS）';
COMMENT ON COLUMN mes_dv_machinery.last_mainten_time IS '最近保养时间';
COMMENT ON COLUMN mes_dv_machinery.last_check_time IS '最近点检时间';
COMMENT ON COLUMN mes_dv_machinery.remark IS '备注';

CREATE SEQUENCE IF NOT EXISTS mes_dv_machinery_seq;
CREATE UNIQUE INDEX IF NOT EXISTS uk_mes_dv_machinery_code ON mes_dv_machinery (code, tenant_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_mes_dv_machinery_machinery_type_id ON mes_dv_machinery (machinery_type_id);
CREATE INDEX IF NOT EXISTS idx_mes_dv_machinery_workshop_id ON mes_dv_machinery (workshop_id);

-- ===================== mes_dv_machinery_type =====================
-- MES 设备类型 DO
DROP TABLE IF EXISTS mes_dv_machinery_type;
CREATE TABLE IF NOT EXISTS mes_dv_machinery_type (
    id          bigint      NOT NULL,
    code        varchar(64),
    name        varchar(128),
    parent_id   bigint,
    status      int4,
    sort        int4,
    remark      varchar(500),
    creator     varchar(64) DEFAULT '',
    create_time timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater     varchar(64) DEFAULT '',
    update_time timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     int2        NOT NULL DEFAULT 0,
    tenant_id   bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

COMMENT ON TABLE mes_dv_machinery_type IS 'MES 设备类型 DO';
COMMENT ON COLUMN mes_dv_machinery_type.code IS '类型编码';
COMMENT ON COLUMN mes_dv_machinery_type.name IS '类型名称';
COMMENT ON COLUMN mes_dv_machinery_type.parent_id IS '父类型编号（关联 mes_dv_machinery_type.id，0 为根）';
COMMENT ON COLUMN mes_dv_machinery_type.status IS '状态（枚举 CommonStatusEnum）';
COMMENT ON COLUMN mes_dv_machinery_type.sort IS '显示排序';
COMMENT ON COLUMN mes_dv_machinery_type.remark IS '备注';

CREATE SEQUENCE IF NOT EXISTS mes_dv_machinery_type_seq;
CREATE UNIQUE INDEX IF NOT EXISTS uk_mes_dv_machinery_type_code ON mes_dv_machinery_type (code, tenant_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_mes_dv_machinery_type_parent_id ON mes_dv_machinery_type (parent_id);

-- ===================== mes_dv_mainten_record =====================
-- MES 设备保养记录 DO
DROP TABLE IF EXISTS mes_dv_mainten_record;
CREATE TABLE IF NOT EXISTS mes_dv_mainten_record (
    id              bigint      NOT NULL,
    plan_id         bigint,
    machinery_id    bigint,
    mainten_time    timestamp,
    user_id         bigint,
    status          int4,
    remark          varchar(500),
    creator         varchar(64) DEFAULT '',
    create_time     timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         varchar(64) DEFAULT '',
    update_time     timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         int2        NOT NULL DEFAULT 0,
    tenant_id       bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

COMMENT ON TABLE mes_dv_mainten_record IS 'MES 设备保养记录 DO';
COMMENT ON COLUMN mes_dv_mainten_record.plan_id IS '计划编号（关联 mes_dv_check_plan.id）';
COMMENT ON COLUMN mes_dv_mainten_record.machinery_id IS '设备编号（关联 mes_dv_machinery.id）';
COMMENT ON COLUMN mes_dv_mainten_record.mainten_time IS '保养时间';
COMMENT ON COLUMN mes_dv_mainten_record.user_id IS '用户编号（关联 AdminUserDO.id）';
COMMENT ON COLUMN mes_dv_mainten_record.status IS '状态（字典 MES_MAINTEN_RECORD_STATUS）';
COMMENT ON COLUMN mes_dv_mainten_record.remark IS '备注';

CREATE SEQUENCE IF NOT EXISTS mes_dv_mainten_record_seq;
CREATE INDEX IF NOT EXISTS idx_mes_dv_mainten_record_plan_id ON mes_dv_mainten_record (plan_id);
CREATE INDEX IF NOT EXISTS idx_mes_dv_mainten_record_machinery_id ON mes_dv_mainten_record (machinery_id);

-- ===================== mes_dv_mainten_record_line =====================
-- MES 设备保养记录明细 DO
DROP TABLE IF EXISTS mes_dv_mainten_record_line;
CREATE TABLE IF NOT EXISTS mes_dv_mainten_record_line (
    id              bigint      NOT NULL,
    record_id       bigint,
    subject_id      bigint,
    status          int4,
    result          text,
    remark          varchar(500),
    creator         varchar(64) DEFAULT '',
    create_time     timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         varchar(64) DEFAULT '',
    update_time     timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         int2        NOT NULL DEFAULT 0,
    tenant_id       bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

COMMENT ON TABLE mes_dv_mainten_record_line IS 'MES 设备保养记录明细 DO';
COMMENT ON COLUMN mes_dv_mainten_record_line.record_id IS '保养记录编号（关联 mes_dv_mainten_record.id）';
COMMENT ON COLUMN mes_dv_mainten_record_line.subject_id IS '项目编号（关联 mes_dv_subject.id）';
COMMENT ON COLUMN mes_dv_mainten_record_line.status IS '保养结果（字典 MES_MAINTEN_STATUS）';
COMMENT ON COLUMN mes_dv_mainten_record_line.result IS '异常描述';
COMMENT ON COLUMN mes_dv_mainten_record_line.remark IS '备注';

CREATE SEQUENCE IF NOT EXISTS mes_dv_mainten_record_line_seq;
CREATE INDEX IF NOT EXISTS idx_mes_dv_mainten_record_line_record_id ON mes_dv_mainten_record_line (record_id);

-- ===================== mes_dv_repair =====================
-- MES 维修工单 DO
DROP TABLE IF EXISTS mes_dv_repair;
CREATE TABLE IF NOT EXISTS mes_dv_repair (
    id                  bigint      NOT NULL,
    code                varchar(64),
    name                varchar(128),
    machinery_id        bigint,
    require_date        timestamp,
    finish_date         timestamp,
    confirm_date        timestamp,
    result              int4,
    accepted_user_id    bigint,
    confirm_user_id     bigint,
    source_doc_type     int4,
    source_doc_id       bigint,
    source_doc_code     varchar(128),
    status              int4,
    remark              varchar(500),
    creator             varchar(64) DEFAULT '',
    create_time         timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater             varchar(64) DEFAULT '',
    update_time         timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             int2        NOT NULL DEFAULT 0,
    tenant_id           bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

COMMENT ON TABLE mes_dv_repair IS 'MES 维修工单 DO';
COMMENT ON COLUMN mes_dv_repair.code IS '维修工单编码';
COMMENT ON COLUMN mes_dv_repair.name IS '维修工单名称';
COMMENT ON COLUMN mes_dv_repair.machinery_id IS '设备编号（关联 mes_dv_machinery.id）';
COMMENT ON COLUMN mes_dv_repair.require_date IS '报修日期';
COMMENT ON COLUMN mes_dv_repair.finish_date IS '维修完成日期';
COMMENT ON COLUMN mes_dv_repair.confirm_date IS '验收日期';
COMMENT ON COLUMN mes_dv_repair.result IS '维修结果（字典 MES_DV_REPAIR_RESULT）';
COMMENT ON COLUMN mes_dv_repair.accepted_user_id IS '维修人用户编号（关联 AdminUserDO.id）';
COMMENT ON COLUMN mes_dv_repair.confirm_user_id IS '验收人用户编号（关联 AdminUserDO.id）';
COMMENT ON COLUMN mes_dv_repair.source_doc_type IS '来源单据类型（预留字段，暂未使用）';
COMMENT ON COLUMN mes_dv_repair.source_doc_id IS '来源单据编号（预留字段，暂未使用）';
COMMENT ON COLUMN mes_dv_repair.source_doc_code IS '来源单据编码（预留字段，暂未使用）';
COMMENT ON COLUMN mes_dv_repair.status IS '状态（字典 MES_DV_REPAIR_STATUS）';
COMMENT ON COLUMN mes_dv_repair.remark IS '备注';

CREATE SEQUENCE IF NOT EXISTS mes_dv_repair_seq;
CREATE UNIQUE INDEX IF NOT EXISTS uk_mes_dv_repair_code ON mes_dv_repair (code, tenant_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_mes_dv_repair_machinery_id ON mes_dv_repair (machinery_id);

-- ===================== mes_dv_repair_line =====================
-- MES 维修工单行 DO
DROP TABLE IF EXISTS mes_dv_repair_line;
CREATE TABLE IF NOT EXISTS mes_dv_repair_line (
    id                  bigint      NOT NULL,
    repair_id           bigint,
    subject_id          bigint,
    malfunction         text,
    malfunction_url     varchar(500),
    description         text,
    remark              varchar(500),
    creator             varchar(64) DEFAULT '',
    create_time         timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater             varchar(64) DEFAULT '',
    update_time         timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             int2        NOT NULL DEFAULT 0,
    tenant_id           bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

COMMENT ON TABLE mes_dv_repair_line IS 'MES 维修工单行 DO';
COMMENT ON COLUMN mes_dv_repair_line.repair_id IS '维修工单编号（关联 mes_dv_repair.id）';
COMMENT ON COLUMN mes_dv_repair_line.subject_id IS '点检保养项目编号（关联 mes_dv_subject.id）';
COMMENT ON COLUMN mes_dv_repair_line.malfunction IS '故障描述';
COMMENT ON COLUMN mes_dv_repair_line.malfunction_url IS '故障图片 URL';
COMMENT ON COLUMN mes_dv_repair_line.description IS '维修描述';
COMMENT ON COLUMN mes_dv_repair_line.remark IS '备注';

CREATE SEQUENCE IF NOT EXISTS mes_dv_repair_line_seq;
CREATE INDEX IF NOT EXISTS idx_mes_dv_repair_line_repair_id ON mes_dv_repair_line (repair_id);

-- ===================== mes_dv_subject =====================
-- MES 点检保养项目 DO
DROP TABLE IF EXISTS mes_dv_subject;
CREATE TABLE IF NOT EXISTS mes_dv_subject (
    id          bigint      NOT NULL,
    code        varchar(64),
    name        varchar(128),
    type        int4,
    content     text,
    standard    varchar(500),
    status      int4,
    remark      varchar(500),
    creator     varchar(64) DEFAULT '',
    create_time timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater     varchar(64) DEFAULT '',
    update_time timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     int2        NOT NULL DEFAULT 0,
    tenant_id   bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

COMMENT ON TABLE mes_dv_subject IS 'MES 点检保养项目 DO';
COMMENT ON COLUMN mes_dv_subject.code IS '项目编码';
COMMENT ON COLUMN mes_dv_subject.name IS '项目名称';
COMMENT ON COLUMN mes_dv_subject.type IS '项目类型（字典 mes_dv_subject_type）';
COMMENT ON COLUMN mes_dv_subject.content IS '项目内容';
COMMENT ON COLUMN mes_dv_subject.standard IS '标准';
COMMENT ON COLUMN mes_dv_subject.status IS '状态（枚举 CommonStatusEnum）';
COMMENT ON COLUMN mes_dv_subject.remark IS '备注';

CREATE SEQUENCE IF NOT EXISTS mes_dv_subject_seq;
CREATE UNIQUE INDEX IF NOT EXISTS uk_mes_dv_subject_code ON mes_dv_subject (code, tenant_id) WHERE deleted = 0;


-- ============================================================
-- MES 模块表 DDL（PostgreSQL）— 从本地 DO 反推
-- ============================================================
-- 来源：从 yudao-module-mes 的 *DO.java 反推生成（非社区 MySQL 脚本转换）
-- 数据库：PostgreSQL（docker-db_postgres-1 / 127.0.0.1:5433 / db=yudao）
-- 安全：仅 DROP IF EXISTS + CREATE IF NOT EXISTS（幂等）；无 INSERT、无外键约束、无 ENGINE
-- 约定：见同目录 ../_CONVENTIONS.md（tenant_id 全表必加、序列取 @KeySequence 字面值、JSON→text）
-- 子域：MES md 主数据
-- 运行：psql -1 -v ON_ERROR_STOP=1 -f md.sql
-- ============================================================

-- ===================== mes_md_client =====================
-- MES 客户 DO
DROP TABLE IF EXISTS "mes_md_client";
CREATE TABLE IF NOT EXISTS "mes_md_client" (
    "id"                  int8        NOT NULL,
    "code"                varchar(32)  NULL DEFAULT NULL,
    "name"                varchar(128) NULL DEFAULT NULL,
    "nickname"            varchar(128) NULL DEFAULT NULL,
    "english_name"        varchar(128) NULL DEFAULT NULL,
    "description"         text         NULL DEFAULT NULL,
    "logo"                varchar(500) NULL DEFAULT NULL,
    "type"                int4         NULL DEFAULT NULL,
    "address"             varchar(255) NULL DEFAULT NULL,
    "website"             varchar(500) NULL DEFAULT NULL,
    "email"               varchar(50)  NULL DEFAULT NULL,
    "telephone"           varchar(20)  NULL DEFAULT NULL,
    "contact1_name"       varchar(64)  NULL DEFAULT NULL,
    "contact1_telephone"  varchar(20)  NULL DEFAULT NULL,
    "contact1_email"      varchar(50)  NULL DEFAULT NULL,
    "contact2_name"       varchar(64)  NULL DEFAULT NULL,
    "contact2_telephone"  varchar(20)  NULL DEFAULT NULL,
    "contact2_email"      varchar(50)  NULL DEFAULT NULL,
    "credit_code"         varchar(64)  NULL DEFAULT NULL,
    "status"              int4         NULL DEFAULT NULL,
    "remark"              varchar(500) NULL DEFAULT NULL,
    "creator"             varchar(64)  NULL DEFAULT '',
    "create_time"         timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"             varchar(64)  NULL DEFAULT '',
    "update_time"         timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"             int2         NOT NULL DEFAULT 0,
    "tenant_id"           int8         NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_md_client" IS 'MES 客户 DO';
COMMENT ON COLUMN "mes_md_client"."code" IS '客户编码';
COMMENT ON COLUMN "mes_md_client"."name" IS '客户名称';
COMMENT ON COLUMN "mes_md_client"."nickname" IS '客户简称';
COMMENT ON COLUMN "mes_md_client"."english_name" IS '客户英文名称';
COMMENT ON COLUMN "mes_md_client"."description" IS '客户简介';
COMMENT ON COLUMN "mes_md_client"."logo" IS '客户LOGO地址';
COMMENT ON COLUMN "mes_md_client"."type" IS '客户类型';
COMMENT ON COLUMN "mes_md_client"."address" IS '客户地址';
COMMENT ON COLUMN "mes_md_client"."website" IS '客户官网地址';
COMMENT ON COLUMN "mes_md_client"."email" IS '客户邮箱地址';
COMMENT ON COLUMN "mes_md_client"."telephone" IS '客户电话';
COMMENT ON COLUMN "mes_md_client"."contact1_name" IS '联系人1';
COMMENT ON COLUMN "mes_md_client"."contact1_telephone" IS '联系人1-电话';
COMMENT ON COLUMN "mes_md_client"."contact1_email" IS '联系人1-邮箱';
COMMENT ON COLUMN "mes_md_client"."contact2_name" IS '联系人2';
COMMENT ON COLUMN "mes_md_client"."contact2_telephone" IS '联系人2-电话';
COMMENT ON COLUMN "mes_md_client"."contact2_email" IS '联系人2-邮箱';
COMMENT ON COLUMN "mes_md_client"."credit_code" IS '统一社会信用代码';
COMMENT ON COLUMN "mes_md_client"."status" IS '状态';
COMMENT ON COLUMN "mes_md_client"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS "mes_md_client_seq";

-- ===================== mes_md_item =====================
-- MES 物料产品 DO
DROP TABLE IF EXISTS "mes_md_item";
CREATE TABLE IF NOT EXISTS "mes_md_item" (
    "id"                int8          NOT NULL,
    "code"              varchar(32)   NULL DEFAULT NULL,
    "name"              varchar(128)  NULL DEFAULT NULL,
    "specification"     varchar(255)  NULL DEFAULT NULL,
    "unit_measure_id"   int8          NULL DEFAULT NULL,
    "item_type_id"      int8          NULL DEFAULT NULL,
    "status"            int4          NULL DEFAULT NULL,
    "safe_stock_flag"   int2          NOT NULL DEFAULT 0,
    "min_stock"         numeric(14,2) NULL DEFAULT NULL,
    "max_stock"         numeric(14,2) NULL DEFAULT NULL,
    "high_value"        int2          NOT NULL DEFAULT 0,
    "batch_flag"        int2          NOT NULL DEFAULT 0,
    "remark"            varchar(500)  NULL DEFAULT NULL,
    "creator"           varchar(64)   NULL DEFAULT '',
    "create_time"       timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"           varchar(64)   NULL DEFAULT '',
    "update_time"       timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"           int2          NOT NULL DEFAULT 0,
    "tenant_id"         int8          NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_md_item" IS 'MES 物料产品 DO';
COMMENT ON COLUMN "mes_md_item"."code" IS '物料编码';
COMMENT ON COLUMN "mes_md_item"."name" IS '物料名称';
COMMENT ON COLUMN "mes_md_item"."specification" IS '规格型号';
COMMENT ON COLUMN "mes_md_item"."unit_measure_id" IS '计量单位编号';
COMMENT ON COLUMN "mes_md_item"."item_type_id" IS '物料分类编号';
COMMENT ON COLUMN "mes_md_item"."status" IS '状态';
COMMENT ON COLUMN "mes_md_item"."safe_stock_flag" IS '是否启用安全库存';
COMMENT ON COLUMN "mes_md_item"."min_stock" IS '最低库存量';
COMMENT ON COLUMN "mes_md_item"."max_stock" IS '最高库存量';
COMMENT ON COLUMN "mes_md_item"."high_value" IS '是否高值物料';
COMMENT ON COLUMN "mes_md_item"."batch_flag" IS '是否启用批次管理';
COMMENT ON COLUMN "mes_md_item"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS "mes_md_item_seq";

-- 唯一索引：物料编码 + 租户 + 软删过滤
CREATE UNIQUE INDEX IF NOT EXISTS "idx_mes_md_item_code" ON "mes_md_item" ("code", "tenant_id") WHERE "deleted" = 0;
-- 外键普通索引
CREATE INDEX IF NOT EXISTS "idx_mes_md_item_unit_measure_id" ON "mes_md_item" ("unit_measure_id");
CREATE INDEX IF NOT EXISTS "idx_mes_md_item_item_type_id" ON "mes_md_item" ("item_type_id");

-- ===================== mes_md_item_batch_config =====================
-- MES 物料批次属性配置 DO
DROP TABLE IF EXISTS "mes_md_item_batch_config";
CREATE TABLE IF NOT EXISTS "mes_md_item_batch_config" (
    "id"                         int8  NOT NULL,
    "item_id"                    int8  NULL DEFAULT NULL,
    "produce_date_flag"          int2  NOT NULL DEFAULT 0,
    "expire_date_flag"           int2  NOT NULL DEFAULT 0,
    "receipt_date_flag"          int2  NOT NULL DEFAULT 0,
    "vendor_flag"                int2  NOT NULL DEFAULT 0,
    "client_flag"                int2  NOT NULL DEFAULT 0,
    "sales_order_code_flag"      int2  NOT NULL DEFAULT 0,
    "purchase_order_code_flag"   int2  NOT NULL DEFAULT 0,
    "work_order_flag"            int2  NOT NULL DEFAULT 0,
    "task_flag"                  int2  NOT NULL DEFAULT 0,
    "workstation_flag"           int2  NOT NULL DEFAULT 0,
    "tool_flag"                  int2  NOT NULL DEFAULT 0,
    "mold_flag"                  int2  NOT NULL DEFAULT 0,
    "lot_number_flag"            int2  NOT NULL DEFAULT 0,
    "quality_status_flag"        int2  NOT NULL DEFAULT 0,
    "creator"                    varchar(64) NULL DEFAULT '',
    "create_time"                timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"                    varchar(64) NULL DEFAULT '',
    "update_time"                timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"                    int2        NOT NULL DEFAULT 0,
    "tenant_id"                  int8        NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_md_item_batch_config" IS 'MES 物料批次属性配置 DO';
COMMENT ON COLUMN "mes_md_item_batch_config"."item_id" IS '物料编号';
COMMENT ON COLUMN "mes_md_item_batch_config"."produce_date_flag" IS '批次属性-生产日期';
COMMENT ON COLUMN "mes_md_item_batch_config"."expire_date_flag" IS '批次属性-有效期';
COMMENT ON COLUMN "mes_md_item_batch_config"."receipt_date_flag" IS '批次属性-入库日期';
COMMENT ON COLUMN "mes_md_item_batch_config"."vendor_flag" IS '批次属性-供应商';
COMMENT ON COLUMN "mes_md_item_batch_config"."client_flag" IS '批次属性-客户';
COMMENT ON COLUMN "mes_md_item_batch_config"."sales_order_code_flag" IS '批次属性-销售订单编号';
COMMENT ON COLUMN "mes_md_item_batch_config"."purchase_order_code_flag" IS '批次属性-采购订单编号';
COMMENT ON COLUMN "mes_md_item_batch_config"."work_order_flag" IS '批次属性-生产工单';
COMMENT ON COLUMN "mes_md_item_batch_config"."task_flag" IS '批次属性-生产任务';
COMMENT ON COLUMN "mes_md_item_batch_config"."workstation_flag" IS '批次属性-工作站';
COMMENT ON COLUMN "mes_md_item_batch_config"."tool_flag" IS '批次属性-工具';
COMMENT ON COLUMN "mes_md_item_batch_config"."mold_flag" IS '批次属性-模具';
COMMENT ON COLUMN "mes_md_item_batch_config"."lot_number_flag" IS '批次属性-生产批号';
COMMENT ON COLUMN "mes_md_item_batch_config"."quality_status_flag" IS '批次属性-质量状态';

CREATE SEQUENCE IF NOT EXISTS "mes_md_item_batch_config_seq";

CREATE INDEX IF NOT EXISTS "idx_mes_md_item_batch_config_item_id" ON "mes_md_item_batch_config" ("item_id");

-- ===================== mes_md_item_type =====================
-- MES 物料产品分类 DO
DROP TABLE IF EXISTS "mes_md_item_type";
CREATE TABLE IF NOT EXISTS "mes_md_item_type" (
    "id"              int8         NOT NULL,
    "code"            varchar(32)  NULL DEFAULT NULL,
    "name"            varchar(128) NULL DEFAULT NULL,
    "parent_id"       int8         NULL DEFAULT NULL,
    "item_or_product" varchar(32)  NULL DEFAULT NULL,
    "sort"            int4         NULL DEFAULT NULL,
    "status"          int4         NULL DEFAULT NULL,
    "remark"          varchar(500) NULL DEFAULT NULL,
    "creator"         varchar(64)  NULL DEFAULT '',
    "create_time"     timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"         varchar(64)  NULL DEFAULT '',
    "update_time"     timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"         int2         NOT NULL DEFAULT 0,
    "tenant_id"       int8         NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_md_item_type" IS 'MES 物料产品分类 DO';
COMMENT ON COLUMN "mes_md_item_type"."code" IS '分类编码';
COMMENT ON COLUMN "mes_md_item_type"."name" IS '分类名称';
COMMENT ON COLUMN "mes_md_item_type"."parent_id" IS '父分类编号';
COMMENT ON COLUMN "mes_md_item_type"."item_or_product" IS '物料/产品标识';
COMMENT ON COLUMN "mes_md_item_type"."sort" IS '显示排序';
COMMENT ON COLUMN "mes_md_item_type"."status" IS '状态';
COMMENT ON COLUMN "mes_md_item_type"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS "mes_md_item_type_seq";

CREATE INDEX IF NOT EXISTS "idx_mes_md_item_type_parent_id" ON "mes_md_item_type" ("parent_id");

-- ===================== mes_md_product_bom =====================
-- MES 产品 BOM DO
DROP TABLE IF EXISTS "mes_md_product_bom";
CREATE TABLE IF NOT EXISTS "mes_md_product_bom" (
    "id"          int8           NOT NULL,
    "item_id"     int8           NULL DEFAULT NULL,
    "bom_item_id" int8           NULL DEFAULT NULL,
    "quantity"    numeric(24,6)  NULL DEFAULT NULL,
    "status"      int4           NULL DEFAULT NULL,
    "remark"      varchar(500)   NULL DEFAULT NULL,
    "creator"     varchar(64)    NULL DEFAULT '',
    "create_time" timestamp      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"     varchar(64)    NULL DEFAULT '',
    "update_time" timestamp      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"     int2           NOT NULL DEFAULT 0,
    "tenant_id"   int8           NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_md_product_bom" IS 'MES 产品 BOM DO';
COMMENT ON COLUMN "mes_md_product_bom"."item_id" IS '物料产品编号';
COMMENT ON COLUMN "mes_md_product_bom"."bom_item_id" IS 'BOM物料编号';
COMMENT ON COLUMN "mes_md_product_bom"."quantity" IS '物料使用比例';
COMMENT ON COLUMN "mes_md_product_bom"."status" IS '是否启用';
COMMENT ON COLUMN "mes_md_product_bom"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS "mes_md_product_bom_seq";

CREATE INDEX IF NOT EXISTS "idx_mes_md_product_bom_item_id" ON "mes_md_product_bom" ("item_id");
CREATE INDEX IF NOT EXISTS "idx_mes_md_product_bom_bom_item_id" ON "mes_md_product_bom" ("bom_item_id");

-- ===================== mes_md_product_sip =====================
-- MES 产品SIP DO
DROP TABLE IF EXISTS "mes_md_product_sip";
CREATE TABLE IF NOT EXISTS "mes_md_product_sip" (
    "id"          int8         NOT NULL,
    "item_id"     int8         NULL DEFAULT NULL,
    "sort"        int4         NULL DEFAULT NULL,
    "process_id"  int8         NULL DEFAULT NULL,
    "title"       varchar(128) NULL DEFAULT NULL,
    "description" text         NULL DEFAULT NULL,
    "url"         varchar(500) NULL DEFAULT NULL,
    "remark"      varchar(500) NULL DEFAULT NULL,
    "creator"     varchar(64)  NULL DEFAULT '',
    "create_time" timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"     varchar(64)  NULL DEFAULT '',
    "update_time" timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"     int2         NOT NULL DEFAULT 0,
    "tenant_id"   int8         NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_md_product_sip" IS 'MES 产品SIP DO';
COMMENT ON COLUMN "mes_md_product_sip"."item_id" IS '物料产品编号';
COMMENT ON COLUMN "mes_md_product_sip"."sort" IS '排列顺序';
COMMENT ON COLUMN "mes_md_product_sip"."process_id" IS '工序编号';
COMMENT ON COLUMN "mes_md_product_sip"."title" IS '标题';
COMMENT ON COLUMN "mes_md_product_sip"."description" IS '详细描述';
COMMENT ON COLUMN "mes_md_product_sip"."url" IS '图片地址';
COMMENT ON COLUMN "mes_md_product_sip"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS "mes_md_product_sip_seq";

CREATE INDEX IF NOT EXISTS "idx_mes_md_product_sip_item_id" ON "mes_md_product_sip" ("item_id");

-- ===================== mes_md_product_sop =====================
-- MES 产品SOP DO
DROP TABLE IF EXISTS "mes_md_product_sop";
CREATE TABLE IF NOT EXISTS "mes_md_product_sop" (
    "id"          int8         NOT NULL,
    "item_id"     int8         NULL DEFAULT NULL,
    "sort"        int4         NULL DEFAULT NULL,
    "process_id"  int8         NULL DEFAULT NULL,
    "title"       varchar(128) NULL DEFAULT NULL,
    "description" text         NULL DEFAULT NULL,
    "url"         varchar(500) NULL DEFAULT NULL,
    "remark"      varchar(500) NULL DEFAULT NULL,
    "creator"     varchar(64)  NULL DEFAULT '',
    "create_time" timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"     varchar(64)  NULL DEFAULT '',
    "update_time" timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"     int2         NOT NULL DEFAULT 0,
    "tenant_id"   int8         NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_md_product_sop" IS 'MES 产品SOP DO';
COMMENT ON COLUMN "mes_md_product_sop"."item_id" IS '物料产品编号';
COMMENT ON COLUMN "mes_md_product_sop"."sort" IS '排列顺序';
COMMENT ON COLUMN "mes_md_product_sop"."process_id" IS '工序编号';
COMMENT ON COLUMN "mes_md_product_sop"."title" IS '标题';
COMMENT ON COLUMN "mes_md_product_sop"."description" IS '详细描述';
COMMENT ON COLUMN "mes_md_product_sop"."url" IS '图片地址';
COMMENT ON COLUMN "mes_md_product_sop"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS "mes_md_product_sop_seq";

CREATE INDEX IF NOT EXISTS "idx_mes_md_product_sop_item_id" ON "mes_md_product_sop" ("item_id");

-- ===================== mes_md_unit_measure =====================
-- MES 计量单位 DO
DROP TABLE IF EXISTS "mes_md_unit_measure";
CREATE TABLE IF NOT EXISTS "mes_md_unit_measure" (
    "id"           int8           NOT NULL,
    "code"         varchar(32)    NULL DEFAULT NULL,
    "name"         varchar(128)   NULL DEFAULT NULL,
    "primary_flag" int2           NOT NULL DEFAULT 0,
    "primary_id"   int8           NULL DEFAULT NULL,
    "change_rate"  numeric(24,6)  NULL DEFAULT NULL,
    "status"       int4           NULL DEFAULT NULL,
    "remark"       varchar(500)   NULL DEFAULT NULL,
    "creator"      varchar(64)    NULL DEFAULT '',
    "create_time"  timestamp      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"      varchar(64)    NULL DEFAULT '',
    "update_time"  timestamp      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"      int2           NOT NULL DEFAULT 0,
    "tenant_id"    int8           NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_md_unit_measure" IS 'MES 计量单位 DO';
COMMENT ON COLUMN "mes_md_unit_measure"."code" IS '单位编码';
COMMENT ON COLUMN "mes_md_unit_measure"."name" IS '单位名称';
COMMENT ON COLUMN "mes_md_unit_measure"."primary_flag" IS '是否主单位';
COMMENT ON COLUMN "mes_md_unit_measure"."primary_id" IS '主单位编号';
COMMENT ON COLUMN "mes_md_unit_measure"."change_rate" IS '与主单位换算比例';
COMMENT ON COLUMN "mes_md_unit_measure"."status" IS '状态';
COMMENT ON COLUMN "mes_md_unit_measure"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS "mes_md_unit_measure_seq";

CREATE INDEX IF NOT EXISTS "idx_mes_md_unit_measure_primary_id" ON "mes_md_unit_measure" ("primary_id");

-- ===================== mes_md_vendor =====================
-- MES 供应商 DO
DROP TABLE IF EXISTS "mes_md_vendor";
CREATE TABLE IF NOT EXISTS "mes_md_vendor" (
    "id"                  int8         NOT NULL,
    "code"                varchar(32)  NULL DEFAULT NULL,
    "name"                varchar(128) NULL DEFAULT NULL,
    "nickname"            varchar(128) NULL DEFAULT NULL,
    "english_name"        varchar(128) NULL DEFAULT NULL,
    "description"         text         NULL DEFAULT NULL,
    "logo"                varchar(500) NULL DEFAULT NULL,
    "level"               varchar(32)  NULL DEFAULT NULL,
    "score"               int4         NULL DEFAULT NULL,
    "address"             varchar(255) NULL DEFAULT NULL,
    "website"             varchar(500) NULL DEFAULT NULL,
    "email"               varchar(50)  NULL DEFAULT NULL,
    "telephone"           varchar(20)  NULL DEFAULT NULL,
    "contact1_name"       varchar(64)  NULL DEFAULT NULL,
    "contact1_telephone"  varchar(20)  NULL DEFAULT NULL,
    "contact1_email"      varchar(50)  NULL DEFAULT NULL,
    "contact2_name"       varchar(64)  NULL DEFAULT NULL,
    "contact2_telephone"  varchar(20)  NULL DEFAULT NULL,
    "contact2_email"      varchar(50)  NULL DEFAULT NULL,
    "credit_code"         varchar(64)  NULL DEFAULT NULL,
    "status"              int4         NULL DEFAULT NULL,
    "remark"              varchar(500) NULL DEFAULT NULL,
    "creator"             varchar(64)  NULL DEFAULT '',
    "create_time"         timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"             varchar(64)  NULL DEFAULT '',
    "update_time"         timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"             int2         NOT NULL DEFAULT 0,
    "tenant_id"           int8         NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_md_vendor" IS 'MES 供应商 DO';
COMMENT ON COLUMN "mes_md_vendor"."code" IS '供应商编码';
COMMENT ON COLUMN "mes_md_vendor"."name" IS '供应商名称';
COMMENT ON COLUMN "mes_md_vendor"."nickname" IS '供应商简称';
COMMENT ON COLUMN "mes_md_vendor"."english_name" IS '供应商英文名称';
COMMENT ON COLUMN "mes_md_vendor"."description" IS '供应商简介';
COMMENT ON COLUMN "mes_md_vendor"."logo" IS '供应商LOGO地址';
COMMENT ON COLUMN "mes_md_vendor"."level" IS '供应商等级';
COMMENT ON COLUMN "mes_md_vendor"."score" IS '供应商评分';
COMMENT ON COLUMN "mes_md_vendor"."address" IS '供应商地址';
COMMENT ON COLUMN "mes_md_vendor"."website" IS '供应商官网地址';
COMMENT ON COLUMN "mes_md_vendor"."email" IS '供应商邮箱地址';
COMMENT ON COLUMN "mes_md_vendor"."telephone" IS '供应商电话';
COMMENT ON COLUMN "mes_md_vendor"."contact1_name" IS '联系人1';
COMMENT ON COLUMN "mes_md_vendor"."contact1_telephone" IS '联系人1-电话';
COMMENT ON COLUMN "mes_md_vendor"."contact1_email" IS '联系人1-邮箱';
COMMENT ON COLUMN "mes_md_vendor"."contact2_name" IS '联系人2';
COMMENT ON COLUMN "mes_md_vendor"."contact2_telephone" IS '联系人2-电话';
COMMENT ON COLUMN "mes_md_vendor"."contact2_email" IS '联系人2-邮箱';
COMMENT ON COLUMN "mes_md_vendor"."credit_code" IS '统一社会信用代码';
COMMENT ON COLUMN "mes_md_vendor"."status" IS '状态';
COMMENT ON COLUMN "mes_md_vendor"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS "mes_md_vendor_seq";

-- ===================== mes_md_workshop =====================
-- MES 车间 DO
DROP TABLE IF EXISTS "mes_md_workshop";
CREATE TABLE IF NOT EXISTS "mes_md_workshop" (
    "id"             int8           NOT NULL,
    "code"           varchar(32)    NULL DEFAULT NULL,
    "name"           varchar(128)   NULL DEFAULT NULL,
    "area"           numeric(14,2)  NULL DEFAULT NULL,
    "charge_user_id" int8           NULL DEFAULT NULL,
    "status"         int4           NULL DEFAULT NULL,
    "remark"         varchar(500)   NULL DEFAULT NULL,
    "creator"        varchar(64)    NULL DEFAULT '',
    "create_time"    timestamp      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"        varchar(64)    NULL DEFAULT '',
    "update_time"    timestamp      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"        int2           NOT NULL DEFAULT 0,
    "tenant_id"      int8           NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_md_workshop" IS 'MES 车间 DO';
COMMENT ON COLUMN "mes_md_workshop"."code" IS '车间编码';
COMMENT ON COLUMN "mes_md_workshop"."name" IS '车间名称';
COMMENT ON COLUMN "mes_md_workshop"."area" IS '面积（平方米）';
COMMENT ON COLUMN "mes_md_workshop"."charge_user_id" IS '负责人用户编号';
COMMENT ON COLUMN "mes_md_workshop"."status" IS '状态';
COMMENT ON COLUMN "mes_md_workshop"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS "mes_md_workshop_seq";

-- ===================== mes_md_workstation =====================
-- MES 工作站 DO
DROP TABLE IF EXISTS "mes_md_workstation";
CREATE TABLE IF NOT EXISTS "mes_md_workstation" (
    "id"           int8         NOT NULL,
    "code"         varchar(32)  NULL DEFAULT NULL,
    "name"         varchar(128) NULL DEFAULT NULL,
    "address"      varchar(255) NULL DEFAULT NULL,
    "workshop_id"  int8         NULL DEFAULT NULL,
    "process_id"   int8         NULL DEFAULT NULL,
    "warehouse_id" int8         NULL DEFAULT NULL,
    "location_id"  int8         NULL DEFAULT NULL,
    "area_id"      int8         NULL DEFAULT NULL,
    "status"       int4         NULL DEFAULT NULL,
    "remark"       varchar(500) NULL DEFAULT NULL,
    "creator"      varchar(64)  NULL DEFAULT '',
    "create_time"  timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"      varchar(64)  NULL DEFAULT '',
    "update_time"  timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"      int2         NOT NULL DEFAULT 0,
    "tenant_id"    int8         NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_md_workstation" IS 'MES 工作站 DO';
COMMENT ON COLUMN "mes_md_workstation"."code" IS '工作站编码';
COMMENT ON COLUMN "mes_md_workstation"."name" IS '工作站名称';
COMMENT ON COLUMN "mes_md_workstation"."address" IS '工作站地点';
COMMENT ON COLUMN "mes_md_workstation"."workshop_id" IS '所在车间编号';
COMMENT ON COLUMN "mes_md_workstation"."process_id" IS '工序编号';
COMMENT ON COLUMN "mes_md_workstation"."warehouse_id" IS '线边库编号';
COMMENT ON COLUMN "mes_md_workstation"."location_id" IS '库区编号';
COMMENT ON COLUMN "mes_md_workstation"."area_id" IS '库位编号';
COMMENT ON COLUMN "mes_md_workstation"."status" IS '状态';
COMMENT ON COLUMN "mes_md_workstation"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS "mes_md_workstation_seq";

CREATE INDEX IF NOT EXISTS "idx_mes_md_workstation_workshop_id" ON "mes_md_workstation" ("workshop_id");
CREATE INDEX IF NOT EXISTS "idx_mes_md_workstation_process_id" ON "mes_md_workstation" ("process_id");

-- ===================== mes_md_workstation_machine =====================
-- MES 设备资源 DO
DROP TABLE IF EXISTS "mes_md_workstation_machine";
CREATE TABLE IF NOT EXISTS "mes_md_workstation_machine" (
    "id"            int8         NOT NULL,
    "workstation_id" int8         NULL DEFAULT NULL,
    "machinery_id"  int8         NULL DEFAULT NULL,
    "quantity"      int4         NULL DEFAULT NULL,
    "remark"        varchar(500) NULL DEFAULT NULL,
    "creator"       varchar(64)  NULL DEFAULT '',
    "create_time"   timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"       varchar(64)  NULL DEFAULT '',
    "update_time"   timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"       int2         NOT NULL DEFAULT 0,
    "tenant_id"     int8         NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_md_workstation_machine" IS 'MES 设备资源 DO';
COMMENT ON COLUMN "mes_md_workstation_machine"."workstation_id" IS '工作站编号';
COMMENT ON COLUMN "mes_md_workstation_machine"."machinery_id" IS '设备编号';
COMMENT ON COLUMN "mes_md_workstation_machine"."quantity" IS '数量';
COMMENT ON COLUMN "mes_md_workstation_machine"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS "mes_md_workstation_machine_seq";

CREATE INDEX IF NOT EXISTS "idx_mes_md_workstation_machine_workstation_id" ON "mes_md_workstation_machine" ("workstation_id");

-- ===================== mes_md_workstation_tool =====================
-- MES 工装夹具资源 DO
DROP TABLE IF EXISTS "mes_md_workstation_tool";
CREATE TABLE IF NOT EXISTS "mes_md_workstation_tool" (
    "id"            int8         NOT NULL,
    "workstation_id" int8         NULL DEFAULT NULL,
    "tool_type_id"  int8         NULL DEFAULT NULL,
    "quantity"      int4         NULL DEFAULT NULL,
    "remark"        varchar(500) NULL DEFAULT NULL,
    "creator"       varchar(64)  NULL DEFAULT '',
    "create_time"   timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"       varchar(64)  NULL DEFAULT '',
    "update_time"   timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"       int2         NOT NULL DEFAULT 0,
    "tenant_id"     int8         NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_md_workstation_tool" IS 'MES 工装夹具资源 DO';
COMMENT ON COLUMN "mes_md_workstation_tool"."workstation_id" IS '工作站编号';
COMMENT ON COLUMN "mes_md_workstation_tool"."tool_type_id" IS '工具类型编号';
COMMENT ON COLUMN "mes_md_workstation_tool"."quantity" IS '数量';
COMMENT ON COLUMN "mes_md_workstation_tool"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS "mes_md_workstation_tool_seq";

CREATE INDEX IF NOT EXISTS "idx_mes_md_workstation_tool_workstation_id" ON "mes_md_workstation_tool" ("workstation_id");

-- ===================== mes_md_workstation_worker =====================
-- MES 人力资源 DO
DROP TABLE IF EXISTS "mes_md_workstation_worker";
CREATE TABLE IF NOT EXISTS "mes_md_workstation_worker" (
    "id"            int8         NOT NULL,
    "workstation_id" int8         NULL DEFAULT NULL,
    "post_id"       int8         NULL DEFAULT NULL,
    "quantity"      int4         NULL DEFAULT NULL,
    "remark"        varchar(500) NULL DEFAULT NULL,
    "creator"       varchar(64)  NULL DEFAULT '',
    "create_time"   timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"       varchar(64)  NULL DEFAULT '',
    "update_time"   timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"       int2         NOT NULL DEFAULT 0,
    "tenant_id"     int8         NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_md_workstation_worker" IS 'MES 人力资源 DO';
COMMENT ON COLUMN "mes_md_workstation_worker"."workstation_id" IS '工作站编号';
COMMENT ON COLUMN "mes_md_workstation_worker"."post_id" IS '岗位编号';
COMMENT ON COLUMN "mes_md_workstation_worker"."quantity" IS '数量';
COMMENT ON COLUMN "mes_md_workstation_worker"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS "mes_md_workstation_worker_seq";

CREATE INDEX IF NOT EXISTS "idx_mes_md_workstation_worker_workstation_id" ON "mes_md_workstation_worker" ("workstation_id");


-- ============================================================
-- MES 模块表 DDL（PostgreSQL）— 从本地 DO 反推
-- ============================================================
-- 来源：从 yudao-module-mes 的 *DO.java 反推生成（非社区 MySQL 脚本转换）
-- 子域：MES pro 生产子域（mes_pro_*）
-- 数据库：PostgreSQL（docker-db_postgres-1 / 127.0.0.1:5433 / db=yudao）
-- 安全：仅 DROP IF EXISTS + CREATE IF NOT EXISTS（幂等）；无 INSERT、无外键约束、无 ENGINE
-- 约定：见同目录 ../_CONVENTIONS.md（tenant_id 全表必加、序列取 @KeySequence 字面值、BigDecimal 数量类用 numeric(14,2)）
-- 基准：对齐 sql/postgresql/yudao-modules.sql 中 mes_pro_feedback 表 DDL（int8/numeric(14,2)/int2/varchar）
-- 运行：psql -1 -v ON_ERROR_STOP=1 -f pro.sql
-- ============================================================

-- ===================== mes_pro_andon_config =====================
-- MES 安灯呼叫配置
DROP TABLE IF EXISTS "mes_pro_andon_config";
CREATE TABLE IF NOT EXISTS "mes_pro_andon_config" (
    "id"               int8 NOT NULL,
    "reason"           varchar(255) NULL DEFAULT NULL,
    "level"            int2 NULL DEFAULT NULL,
    "handler_role_id"  int8 NULL DEFAULT NULL,
    "handler_user_id"  int8 NULL DEFAULT NULL,
    "remark"           varchar(500) NULL DEFAULT NULL,
    "creator"          varchar(64) NULL DEFAULT '',
    "create_time"      timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"          varchar(64) NULL DEFAULT '',
    "update_time"      timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"          int2 NOT NULL DEFAULT 0,
    "tenant_id"        int8 NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_pro_andon_config" IS 'MES 安灯呼叫配置';
COMMENT ON COLUMN "mes_pro_andon_config"."id" IS '编号';
COMMENT ON COLUMN "mes_pro_andon_config"."reason" IS '呼叫原因';
COMMENT ON COLUMN "mes_pro_andon_config"."level" IS '级别';
COMMENT ON COLUMN "mes_pro_andon_config"."handler_role_id" IS '处置人角色编号';
COMMENT ON COLUMN "mes_pro_andon_config"."handler_user_id" IS '处置人编号';
COMMENT ON COLUMN "mes_pro_andon_config"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS mes_pro_andon_config_seq;

-- ===================== mes_pro_andon_record =====================
-- MES 安灯呼叫记录
DROP TABLE IF EXISTS "mes_pro_andon_record";
CREATE TABLE IF NOT EXISTS "mes_pro_andon_record" (
    "id"               int8 NOT NULL,
    "config_id"        int8 NULL DEFAULT NULL,
    "workstation_id"   int8 NULL DEFAULT NULL,
    "user_id"          int8 NULL DEFAULT NULL,
    "work_order_id"    int8 NULL DEFAULT NULL,
    "process_id"       int8 NULL DEFAULT NULL,
    "reason"           varchar(255) NULL DEFAULT NULL,
    "level"            int2 NULL DEFAULT NULL,
    "status"           int2 NULL DEFAULT NULL,
    "handle_time"      timestamp NULL DEFAULT NULL,
    "handler_user_id"  int8 NULL DEFAULT NULL,
    "remark"           varchar(500) NULL DEFAULT NULL,
    "creator"          varchar(64) NULL DEFAULT '',
    "create_time"      timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"          varchar(64) NULL DEFAULT '',
    "update_time"      timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"          int2 NOT NULL DEFAULT 0,
    "tenant_id"        int8 NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_pro_andon_record" IS 'MES 安灯呼叫记录';
COMMENT ON COLUMN "mes_pro_andon_record"."id" IS '编号';
COMMENT ON COLUMN "mes_pro_andon_record"."config_id" IS '安灯配置编号';
COMMENT ON COLUMN "mes_pro_andon_record"."workstation_id" IS '工作站编号';
COMMENT ON COLUMN "mes_pro_andon_record"."user_id" IS '发起用户编号';
COMMENT ON COLUMN "mes_pro_andon_record"."work_order_id" IS '生产工单编号';
COMMENT ON COLUMN "mes_pro_andon_record"."process_id" IS '工序编号';
COMMENT ON COLUMN "mes_pro_andon_record"."reason" IS '呼叫原因（快照值，不随配置变更）';
COMMENT ON COLUMN "mes_pro_andon_record"."level" IS '级别（快照值）';
COMMENT ON COLUMN "mes_pro_andon_record"."status" IS '处置状态';
COMMENT ON COLUMN "mes_pro_andon_record"."handle_time" IS '处置时间';
COMMENT ON COLUMN "mes_pro_andon_record"."handler_user_id" IS '处置人编号';
COMMENT ON COLUMN "mes_pro_andon_record"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS mes_pro_andon_record_seq;

-- ===================== mes_pro_card =====================
-- MES 生产流转卡
DROP TABLE IF EXISTS "mes_pro_card";
CREATE TABLE IF NOT EXISTS "mes_pro_card" (
    "id"                   int8 NOT NULL,
    "code"                 varchar(64) NULL DEFAULT NULL,
    "work_order_id"        int8 NULL DEFAULT NULL,
    "item_id"              int8 NULL DEFAULT NULL,
    "batch_code"           varchar(64) NULL DEFAULT NULL,
    "transfered_quantity"  numeric(14,2) NULL DEFAULT NULL,
    "status"               int2 NULL DEFAULT NULL,
    "remark"               varchar(500) NULL DEFAULT NULL,
    "creator"              varchar(64) NULL DEFAULT '',
    "create_time"          timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"              varchar(64) NULL DEFAULT '',
    "update_time"          timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"              int2 NOT NULL DEFAULT 0,
    "tenant_id"            int8 NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_pro_card" IS 'MES 生产流转卡';
COMMENT ON COLUMN "mes_pro_card"."id" IS '编号';
COMMENT ON COLUMN "mes_pro_card"."code" IS '流转卡编码';
COMMENT ON COLUMN "mes_pro_card"."work_order_id" IS '生产工单编号';
COMMENT ON COLUMN "mes_pro_card"."item_id" IS '产品物料编号';
COMMENT ON COLUMN "mes_pro_card"."batch_code" IS '批次号';
COMMENT ON COLUMN "mes_pro_card"."transfered_quantity" IS '流转数量';
COMMENT ON COLUMN "mes_pro_card"."status" IS '状态';
COMMENT ON COLUMN "mes_pro_card"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS mes_pro_card_seq;

-- ===================== mes_pro_card_process =====================
-- MES 流转卡工序记录
DROP TABLE IF EXISTS "mes_pro_card_process";
CREATE TABLE IF NOT EXISTS "mes_pro_card_process" (
    "id"                   int8 NOT NULL,
    "card_id"              int8 NULL DEFAULT NULL,
    "sort"                 int4 NULL DEFAULT NULL,
    "process_id"           int8 NULL DEFAULT NULL,
    "input_time"           timestamp NULL DEFAULT NULL,
    "output_time"          timestamp NULL DEFAULT NULL,
    "input_quantity"       numeric(14,2) NULL DEFAULT NULL,
    "output_quantity"      numeric(14,2) NULL DEFAULT NULL,
    "unqualified_quantity" numeric(14,2) NULL DEFAULT NULL,
    "workstation_id"       int8 NULL DEFAULT NULL,
    "user_id"              int8 NULL DEFAULT NULL,
    "ipqc_id"              int8 NULL DEFAULT NULL,
    "remark"               varchar(500) NULL DEFAULT NULL,
    "creator"              varchar(64) NULL DEFAULT '',
    "create_time"          timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"              varchar(64) NULL DEFAULT '',
    "update_time"          timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"              int2 NOT NULL DEFAULT 0,
    "tenant_id"            int8 NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_pro_card_process" IS 'MES 流转卡工序记录';
COMMENT ON COLUMN "mes_pro_card_process"."id" IS '编号';
COMMENT ON COLUMN "mes_pro_card_process"."card_id" IS '流转卡编号';
COMMENT ON COLUMN "mes_pro_card_process"."sort" IS '序号';
COMMENT ON COLUMN "mes_pro_card_process"."process_id" IS '工序编号';
COMMENT ON COLUMN "mes_pro_card_process"."input_time" IS '进入工序时间';
COMMENT ON COLUMN "mes_pro_card_process"."output_time" IS '出工序时间';
COMMENT ON COLUMN "mes_pro_card_process"."input_quantity" IS '投人数量';
COMMENT ON COLUMN "mes_pro_card_process"."output_quantity" IS '产出数量';
COMMENT ON COLUMN "mes_pro_card_process"."unqualified_quantity" IS '不合格品数量';
COMMENT ON COLUMN "mes_pro_card_process"."workstation_id" IS '工位编号';
COMMENT ON COLUMN "mes_pro_card_process"."user_id" IS '操作人编号';
COMMENT ON COLUMN "mes_pro_card_process"."ipqc_id" IS '过程检验单编号';
COMMENT ON COLUMN "mes_pro_card_process"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS mes_pro_card_process_seq;

-- ===================== mes_pro_process =====================
-- MES 生产工序
DROP TABLE IF EXISTS "mes_pro_process";
CREATE TABLE IF NOT EXISTS "mes_pro_process" (
    "id"          int8 NOT NULL,
    "code"        varchar(64) NULL DEFAULT NULL,
    "name"        varchar(128) NULL DEFAULT NULL,
    "attention"   text NULL DEFAULT NULL,
    "status"      int2 NULL DEFAULT NULL,
    "remark"      varchar(500) NULL DEFAULT NULL,
    "creator"     varchar(64) NULL DEFAULT '',
    "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"     varchar(64) NULL DEFAULT '',
    "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"     int2 NOT NULL DEFAULT 0,
    "tenant_id"   int8 NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_pro_process" IS 'MES 生产工序';
COMMENT ON COLUMN "mes_pro_process"."id" IS '编号';
COMMENT ON COLUMN "mes_pro_process"."code" IS '工序编码';
COMMENT ON COLUMN "mes_pro_process"."name" IS '工序名称';
COMMENT ON COLUMN "mes_pro_process"."attention" IS '工艺要求';
COMMENT ON COLUMN "mes_pro_process"."status" IS '状态';
COMMENT ON COLUMN "mes_pro_process"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS mes_pro_process_seq;

-- ===================== mes_pro_process_content =====================
-- MES 生产工序内容
DROP TABLE IF EXISTS "mes_pro_process_content";
CREATE TABLE IF NOT EXISTS "mes_pro_process_content" (
    "id"          int8 NOT NULL,
    "process_id"  int8 NULL DEFAULT NULL,
    "sort"        int4 NULL DEFAULT NULL,
    "content"     text NULL DEFAULT NULL,
    "device"      varchar(255) NULL DEFAULT NULL,
    "material"    varchar(255) NULL DEFAULT NULL,
    "doc_url"     varchar(500) NULL DEFAULT NULL,
    "remark"      varchar(500) NULL DEFAULT NULL,
    "creator"     varchar(64) NULL DEFAULT '',
    "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"     varchar(64) NULL DEFAULT '',
    "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"     int2 NOT NULL DEFAULT 0,
    "tenant_id"   int8 NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_pro_process_content" IS 'MES 生产工序内容';
COMMENT ON COLUMN "mes_pro_process_content"."id" IS '编号';
COMMENT ON COLUMN "mes_pro_process_content"."process_id" IS '工序编号';
COMMENT ON COLUMN "mes_pro_process_content"."sort" IS '顺序编号';
COMMENT ON COLUMN "mes_pro_process_content"."content" IS '步骤说明';
COMMENT ON COLUMN "mes_pro_process_content"."device" IS '辅助设备';
COMMENT ON COLUMN "mes_pro_process_content"."material" IS '辅助材料';
COMMENT ON COLUMN "mes_pro_process_content"."doc_url" IS '材料文档 URL';
COMMENT ON COLUMN "mes_pro_process_content"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS mes_pro_process_content_seq;

-- ===================== mes_pro_route =====================
-- MES 工艺路线
DROP TABLE IF EXISTS "mes_pro_route";
CREATE TABLE IF NOT EXISTS "mes_pro_route" (
    "id"          int8 NOT NULL,
    "code"        varchar(64) NULL DEFAULT NULL,
    "name"        varchar(128) NULL DEFAULT NULL,
    "description" text NULL DEFAULT NULL,
    "status"      int2 NULL DEFAULT NULL,
    "remark"      varchar(500) NULL DEFAULT NULL,
    "creator"     varchar(64) NULL DEFAULT '',
    "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"     varchar(64) NULL DEFAULT '',
    "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"     int2 NOT NULL DEFAULT 0,
    "tenant_id"   int8 NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_pro_route" IS 'MES 工艺路线';
COMMENT ON COLUMN "mes_pro_route"."id" IS '编号';
COMMENT ON COLUMN "mes_pro_route"."code" IS '工艺路线编码';
COMMENT ON COLUMN "mes_pro_route"."name" IS '工艺路线名称';
COMMENT ON COLUMN "mes_pro_route"."description" IS '工艺路线说明';
COMMENT ON COLUMN "mes_pro_route"."status" IS '状态';
COMMENT ON COLUMN "mes_pro_route"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS mes_pro_route_seq;

-- ===================== mes_pro_route_product =====================
-- MES 工艺路线产品
DROP TABLE IF EXISTS "mes_pro_route_product";
CREATE TABLE IF NOT EXISTS "mes_pro_route_product" (
    "id"              int8 NOT NULL,
    "route_id"        int8 NULL DEFAULT NULL,
    "item_id"         int8 NULL DEFAULT NULL,
    "quantity"        int4 NULL DEFAULT NULL,
    "production_time" numeric(14,2) NULL DEFAULT NULL,
    "time_unit_type"  varchar(32) NULL DEFAULT NULL,
    "remark"          varchar(500) NULL DEFAULT NULL,
    "creator"         varchar(64) NULL DEFAULT '',
    "create_time"     timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"         varchar(64) NULL DEFAULT '',
    "update_time"     timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"         int2 NOT NULL DEFAULT 0,
    "tenant_id"       int8 NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_pro_route_product" IS 'MES 工艺路线产品';
COMMENT ON COLUMN "mes_pro_route_product"."id" IS '编号';
COMMENT ON COLUMN "mes_pro_route_product"."route_id" IS '工艺路线编号';
COMMENT ON COLUMN "mes_pro_route_product"."item_id" IS '产品物料编号';
COMMENT ON COLUMN "mes_pro_route_product"."quantity" IS '生产数量';
COMMENT ON COLUMN "mes_pro_route_product"."production_time" IS '生产用时';
COMMENT ON COLUMN "mes_pro_route_product"."time_unit_type" IS '时间单位';
COMMENT ON COLUMN "mes_pro_route_product"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS mes_pro_route_product_seq;

-- ===================== mes_pro_route_product_bom =====================
-- MES 工艺路线产品 BOM
DROP TABLE IF EXISTS "mes_pro_route_product_bom";
CREATE TABLE IF NOT EXISTS "mes_pro_route_product_bom" (
    "id"          int8 NOT NULL,
    "route_id"    int8 NULL DEFAULT NULL,
    "process_id"  int8 NULL DEFAULT NULL,
    "product_id"  int8 NULL DEFAULT NULL,
    "item_id"     int8 NULL DEFAULT NULL,
    "quantity"    numeric(14,2) NULL DEFAULT NULL,
    "remark"      varchar(500) NULL DEFAULT NULL,
    "creator"     varchar(64) NULL DEFAULT '',
    "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"     varchar(64) NULL DEFAULT '',
    "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"     int2 NOT NULL DEFAULT 0,
    "tenant_id"   int8 NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_pro_route_product_bom" IS 'MES 工艺路线产品 BOM';
COMMENT ON COLUMN "mes_pro_route_product_bom"."id" IS '编号';
COMMENT ON COLUMN "mes_pro_route_product_bom"."route_id" IS '工艺路线编号';
COMMENT ON COLUMN "mes_pro_route_product_bom"."process_id" IS '工序编号';
COMMENT ON COLUMN "mes_pro_route_product_bom"."product_id" IS '产品物料编号';
COMMENT ON COLUMN "mes_pro_route_product_bom"."item_id" IS 'BOM 物料编号';
COMMENT ON COLUMN "mes_pro_route_product_bom"."quantity" IS '用料比例';
COMMENT ON COLUMN "mes_pro_route_product_bom"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS mes_pro_route_product_bom_seq;

-- ===================== mes_pro_task_issue =====================
-- MES 生产任务投料
DROP TABLE IF EXISTS "mes_pro_task_issue";
CREATE TABLE IF NOT EXISTS "mes_pro_task_issue" (
    "id"                 int8 NOT NULL,
    "task_id"            int8 NULL DEFAULT NULL,
    "work_order_id"      int8 NULL DEFAULT NULL,
    "workstation_id"     int8 NULL DEFAULT NULL,
    "source_doc_type"    varchar(32) NULL DEFAULT NULL,
    "source_doc_id"      int8 NULL DEFAULT NULL,
    "source_line_id"     int8 NULL DEFAULT NULL,
    "source_doc_code"    varchar(64) NULL DEFAULT NULL,
    "batch_code"         varchar(64) NULL DEFAULT NULL,
    "item_id"            int8 NULL DEFAULT NULL,
    "unit_measure_id"    int8 NULL DEFAULT NULL,
    "issued_quantity"    numeric(14,2) NULL DEFAULT NULL,
    "available_quantity" numeric(14,2) NULL DEFAULT NULL,
    "used_quantity"      numeric(14,2) NULL DEFAULT NULL,
    "remark"             varchar(500) NULL DEFAULT NULL,
    "creator"            varchar(64) NULL DEFAULT '',
    "create_time"        timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"            varchar(64) NULL DEFAULT '',
    "update_time"        timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"            int2 NOT NULL DEFAULT 0,
    "tenant_id"          int8 NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_pro_task_issue" IS 'MES 生产任务投料';
COMMENT ON COLUMN "mes_pro_task_issue"."id" IS '编号';
COMMENT ON COLUMN "mes_pro_task_issue"."task_id" IS '生产任务编号';
COMMENT ON COLUMN "mes_pro_task_issue"."work_order_id" IS '生产工单编号';
COMMENT ON COLUMN "mes_pro_task_issue"."workstation_id" IS '工作站编号';
COMMENT ON COLUMN "mes_pro_task_issue"."source_doc_type" IS '来源单据类型';
COMMENT ON COLUMN "mes_pro_task_issue"."source_doc_id" IS '来源单据编号';
COMMENT ON COLUMN "mes_pro_task_issue"."source_line_id" IS '来源单据行编号';
COMMENT ON COLUMN "mes_pro_task_issue"."source_doc_code" IS '来源单据编码';
COMMENT ON COLUMN "mes_pro_task_issue"."batch_code" IS '投料批次';
COMMENT ON COLUMN "mes_pro_task_issue"."item_id" IS '产品物料编号';
COMMENT ON COLUMN "mes_pro_task_issue"."unit_measure_id" IS '单位编号';
COMMENT ON COLUMN "mes_pro_task_issue"."issued_quantity" IS '总投料数量';
COMMENT ON COLUMN "mes_pro_task_issue"."available_quantity" IS '当前可用数量';
COMMENT ON COLUMN "mes_pro_task_issue"."used_quantity" IS '当前使用数量';
COMMENT ON COLUMN "mes_pro_task_issue"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS mes_pro_task_issue_seq;

-- ===================== mes_pro_work_order =====================
-- MES 生产工单
DROP TABLE IF EXISTS "mes_pro_work_order";
CREATE TABLE IF NOT EXISTS "mes_pro_work_order" (
    "id"                  int8 NOT NULL,
    "code"                varchar(64) NULL DEFAULT NULL,
    "name"                varchar(128) NULL DEFAULT NULL,
    "type"                int2 NULL DEFAULT NULL,
    "order_source_type"   int2 NULL DEFAULT NULL,
    "order_source_code"   varchar(64) NULL DEFAULT NULL,
    "product_id"          int8 NULL DEFAULT NULL,
    "quantity"            numeric(14,2) NULL DEFAULT NULL,
    "quantity_produced"   numeric(14,2) NULL DEFAULT NULL,
    "quantity_changed"    numeric(14,2) NULL DEFAULT NULL,
    "quantity_scheduled"  numeric(14,2) NULL DEFAULT NULL,
    "client_id"           int8 NULL DEFAULT NULL,
    "vendor_id"           int8 NULL DEFAULT NULL,
    "batch_code"          varchar(64) NULL DEFAULT NULL,
    "request_date"        timestamp NULL DEFAULT NULL,
    "parent_id"           int8 NULL DEFAULT NULL,
    "finish_date"         timestamp NULL DEFAULT NULL,
    "cancel_date"         timestamp NULL DEFAULT NULL,
    "status"              int2 NULL DEFAULT NULL,
    "remark"              varchar(500) NULL DEFAULT NULL,
    "creator"             varchar(64) NULL DEFAULT '',
    "create_time"         timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"             varchar(64) NULL DEFAULT '',
    "update_time"         timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"             int2 NOT NULL DEFAULT 0,
    "tenant_id"           int8 NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_pro_work_order" IS 'MES 生产工单';
COMMENT ON COLUMN "mes_pro_work_order"."id" IS '编号';
COMMENT ON COLUMN "mes_pro_work_order"."code" IS '工单编码';
COMMENT ON COLUMN "mes_pro_work_order"."name" IS '工单名称';
COMMENT ON COLUMN "mes_pro_work_order"."type" IS '工单类型';
COMMENT ON COLUMN "mes_pro_work_order"."order_source_type" IS '来源类型';
COMMENT ON COLUMN "mes_pro_work_order"."order_source_code" IS '来源单据编号';
COMMENT ON COLUMN "mes_pro_work_order"."product_id" IS '产品编号';
COMMENT ON COLUMN "mes_pro_work_order"."quantity" IS '生产数量';
COMMENT ON COLUMN "mes_pro_work_order"."quantity_produced" IS '已生产数量';
COMMENT ON COLUMN "mes_pro_work_order"."quantity_changed" IS '调整数量';
COMMENT ON COLUMN "mes_pro_work_order"."quantity_scheduled" IS '已排产数量';
COMMENT ON COLUMN "mes_pro_work_order"."client_id" IS '客户编号';
COMMENT ON COLUMN "mes_pro_work_order"."vendor_id" IS '供应商编号';
COMMENT ON COLUMN "mes_pro_work_order"."batch_code" IS '批次号';
COMMENT ON COLUMN "mes_pro_work_order"."request_date" IS '需求日期';
COMMENT ON COLUMN "mes_pro_work_order"."parent_id" IS '父工单编号';
COMMENT ON COLUMN "mes_pro_work_order"."finish_date" IS '完成时间';
COMMENT ON COLUMN "mes_pro_work_order"."cancel_date" IS '取消时间';
COMMENT ON COLUMN "mes_pro_work_order"."status" IS '工单状态';
COMMENT ON COLUMN "mes_pro_work_order"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS mes_pro_work_order_seq;

-- 工单编码唯一索引（带租户隔离 + 软删过滤）
CREATE UNIQUE INDEX IF NOT EXISTS idx_mes_pro_work_order_code ON "mes_pro_work_order" ("code", tenant_id) WHERE deleted = 0;

-- ===================== mes_pro_work_order_bom =====================
-- MES 生产工单 BOM
DROP TABLE IF EXISTS "mes_pro_work_order_bom";
CREATE TABLE IF NOT EXISTS "mes_pro_work_order_bom" (
    "id"            int8 NOT NULL,
    "work_order_id"  int8 NULL DEFAULT NULL,
    "item_id"        int8 NULL DEFAULT NULL,
    "quantity"       numeric(14,2) NULL DEFAULT NULL,
    "remark"         varchar(500) NULL DEFAULT NULL,
    "creator"        varchar(64) NULL DEFAULT '',
    "create_time"    timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"        varchar(64) NULL DEFAULT '',
    "update_time"    timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"        int2 NOT NULL DEFAULT 0,
    "tenant_id"      int8 NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_pro_work_order_bom" IS 'MES 生产工单 BOM';
COMMENT ON COLUMN "mes_pro_work_order_bom"."id" IS '编号';
COMMENT ON COLUMN "mes_pro_work_order_bom"."work_order_id" IS '生产工单编号';
COMMENT ON COLUMN "mes_pro_work_order_bom"."item_id" IS 'BOM 物料编号';
COMMENT ON COLUMN "mes_pro_work_order_bom"."quantity" IS '预计使用量';
COMMENT ON COLUMN "mes_pro_work_order_bom"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS mes_pro_work_order_bom_seq;

-- ===================== mes_pro_work_record =====================
-- MES 用户工作站绑定关系（当前快照）
DROP TABLE IF EXISTS "mes_pro_work_record";
CREATE TABLE IF NOT EXISTS "mes_pro_work_record" (
    "id"             int8 NOT NULL,
    "user_id"        int8 NULL DEFAULT NULL,
    "workstation_id" int8 NULL DEFAULT NULL,
    "type"           int2 NULL DEFAULT NULL,
    "clock_in_time"  timestamp NULL DEFAULT NULL,
    "clock_out_time" timestamp NULL DEFAULT NULL,
    "remark"         varchar(500) NULL DEFAULT NULL,
    "creator"        varchar(64) NULL DEFAULT '',
    "create_time"    timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"        varchar(64) NULL DEFAULT '',
    "update_time"    timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"        int2 NOT NULL DEFAULT 0,
    "tenant_id"      int8 NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_pro_work_record" IS 'MES 用户工作站绑定关系（当前快照）';
COMMENT ON COLUMN "mes_pro_work_record"."id" IS '编号';
COMMENT ON COLUMN "mes_pro_work_record"."user_id" IS '用户编号';
COMMENT ON COLUMN "mes_pro_work_record"."workstation_id" IS '工作站编号';
COMMENT ON COLUMN "mes_pro_work_record"."type" IS '当前状态';
COMMENT ON COLUMN "mes_pro_work_record"."clock_in_time" IS '上工时间';
COMMENT ON COLUMN "mes_pro_work_record"."clock_out_time" IS '下工时间';
COMMENT ON COLUMN "mes_pro_work_record"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS mes_pro_work_record_seq;

-- ===================== mes_pro_work_record_log =====================
-- MES 上下工记录流水
DROP TABLE IF EXISTS "mes_pro_work_record_log";
CREATE TABLE IF NOT EXISTS "mes_pro_work_record_log" (
    "id"             int8 NOT NULL,
    "user_id"        int8 NULL DEFAULT NULL,
    "workstation_id" int8 NULL DEFAULT NULL,
    "type"           int2 NULL DEFAULT NULL,
    "remark"         varchar(500) NULL DEFAULT NULL,
    "creator"        varchar(64) NULL DEFAULT '',
    "create_time"    timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"        varchar(64) NULL DEFAULT '',
    "update_time"    timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"        int2 NOT NULL DEFAULT 0,
    "tenant_id"      int8 NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_pro_work_record_log" IS 'MES 上下工记录流水';
COMMENT ON COLUMN "mes_pro_work_record_log"."id" IS '编号';
COMMENT ON COLUMN "mes_pro_work_record_log"."user_id" IS '用户编号';
COMMENT ON COLUMN "mes_pro_work_record_log"."workstation_id" IS '工作站编号';
COMMENT ON COLUMN "mes_pro_work_record_log"."type" IS '操作类型';
COMMENT ON COLUMN "mes_pro_work_record_log"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS mes_pro_work_record_log_seq;


-- ============================================================
-- MES 模块表 DDL（PostgreSQL）— 从本地 DO 反推
-- ============================================================
-- 来源：从 yudao-module-mes 的 *DO.java 反推生成（非社区 MySQL 脚本转换）
-- 子域：qc 质量管理（mes_qc_*）
-- 数据库：PostgreSQL（docker-db_postgres-1 / 127.0.0.1:5433 / db=yudao）
-- 安全：仅 DROP IF EXISTS + CREATE IF NOT EXISTS（幂等）；无 INSERT、无外键约束、无 ENGINE
-- 约定：见同目录 ../_CONVENTIONS.md（tenant_id 全表必加、序列取 @KeySequence 字面值、JSON→text）
-- 运行：psql -1 -v ON_ERROR_STOP=1 -f qc.sql
-- ============================================================

-- ===================== mes_qc_defect =====================
-- MES 缺陷类型
DROP TABLE IF EXISTS mes_qc_defect;
CREATE TABLE IF NOT EXISTS mes_qc_defect (
    "id"          bigint      NOT NULL,
    "code"        varchar(64),
    "name"        varchar(128),
    "type"        int2,
    "level"       int2,
    "remark"      varchar(500),
    "creator"     varchar(64) DEFAULT '',
    "create_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"     varchar(64) DEFAULT '',
    "update_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"     int2        NOT NULL DEFAULT 0,
    "tenant_id"   bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE mes_qc_defect IS 'MES 缺陷类型';
COMMENT ON COLUMN mes_qc_defect."id" IS '编号';
COMMENT ON COLUMN mes_qc_defect."code" IS '缺陷编码';
COMMENT ON COLUMN mes_qc_defect."name" IS '缺陷描述';
COMMENT ON COLUMN mes_qc_defect."type" IS '检测项类型';
COMMENT ON COLUMN mes_qc_defect."level" IS '缺陷等级';
COMMENT ON COLUMN mes_qc_defect."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS mes_qc_defect_seq;

CREATE UNIQUE INDEX IF NOT EXISTS idx_mes_qc_defect_code ON mes_qc_defect (code, tenant_id) WHERE deleted = 0;

-- ===================== mes_qc_defect_record =====================
-- MES 质检缺陷记录
DROP TABLE IF EXISTS mes_qc_defect_record;
CREATE TABLE IF NOT EXISTS mes_qc_defect_record (
    "id"          bigint      NOT NULL,
    "qc_type"     int2,
    "qc_id"       bigint,
    "line_id"     bigint,
    "name"        varchar(128),
    "level"       int2,
    "quantity"    int4,
    "remark"      varchar(500),
    "creator"     varchar(64) DEFAULT '',
    "create_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"     varchar(64) DEFAULT '',
    "update_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"     int2        NOT NULL DEFAULT 0,
    "tenant_id"   bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE mes_qc_defect_record IS 'MES 质检缺陷记录';
COMMENT ON COLUMN mes_qc_defect_record."id" IS '编号';
COMMENT ON COLUMN mes_qc_defect_record."qc_type" IS '检验类型';
COMMENT ON COLUMN mes_qc_defect_record."qc_id" IS '检验单 ID';
COMMENT ON COLUMN mes_qc_defect_record."line_id" IS '检验行 ID';
COMMENT ON COLUMN mes_qc_defect_record."name" IS '缺陷描述';
COMMENT ON COLUMN mes_qc_defect_record."level" IS '缺陷等级';
COMMENT ON COLUMN mes_qc_defect_record."quantity" IS '缺陷数量';
COMMENT ON COLUMN mes_qc_defect_record."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS mes_qc_defect_record_seq;

CREATE INDEX IF NOT EXISTS idx_mes_qc_defect_record_qc_id ON mes_qc_defect_record (qc_id);
CREATE INDEX IF NOT EXISTS idx_mes_qc_defect_record_line_id ON mes_qc_defect_record (line_id);

-- ===================== mes_qc_indicator =====================
-- MES 质检指标
DROP TABLE IF EXISTS mes_qc_indicator;
CREATE TABLE IF NOT EXISTS mes_qc_indicator (
    "id"                   bigint      NOT NULL,
    "code"                 varchar(64),
    "name"                 varchar(128),
    "type"                 int2,
    "tool"                 varchar(64),
    "result_type"          int2,
    "result_specification" varchar(255),
    "remark"               varchar(500),
    "creator"              varchar(64) DEFAULT '',
    "create_time"          timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"              varchar(64) DEFAULT '',
    "update_time"          timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"              int2        NOT NULL DEFAULT 0,
    "tenant_id"            bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE mes_qc_indicator IS 'MES 质检指标';
COMMENT ON COLUMN mes_qc_indicator."id" IS '编号';
COMMENT ON COLUMN mes_qc_indicator."code" IS '检测项编码';
COMMENT ON COLUMN mes_qc_indicator."name" IS '检测项名称';
COMMENT ON COLUMN mes_qc_indicator."type" IS '检测项类型';
COMMENT ON COLUMN mes_qc_indicator."tool" IS '检测工具';
COMMENT ON COLUMN mes_qc_indicator."result_type" IS '结果值类型';
COMMENT ON COLUMN mes_qc_indicator."result_specification" IS '结果值属性';
COMMENT ON COLUMN mes_qc_indicator."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS mes_qc_indicator_seq;

CREATE UNIQUE INDEX IF NOT EXISTS idx_mes_qc_indicator_code ON mes_qc_indicator (code, tenant_id) WHERE deleted = 0;

-- ===================== mes_qc_indicator_result_detail =====================
-- MES 检验结果明细记录
DROP TABLE IF EXISTS mes_qc_indicator_result_detail;
CREATE TABLE IF NOT EXISTS mes_qc_indicator_result_detail (
    "id"           bigint      NOT NULL,
    "result_id"    bigint,
    "indicator_id" bigint,
    "value"        text,
    "remark"       varchar(500),
    "creator"      varchar(64) DEFAULT '',
    "create_time"  timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"      varchar(64) DEFAULT '',
    "update_time"  timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"      int2        NOT NULL DEFAULT 0,
    "tenant_id"    bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE mes_qc_indicator_result_detail IS 'MES 检验结果明细记录';
COMMENT ON COLUMN mes_qc_indicator_result_detail."id" IS '编号';
COMMENT ON COLUMN mes_qc_indicator_result_detail."result_id" IS '关联检验结果 ID';
COMMENT ON COLUMN mes_qc_indicator_result_detail."indicator_id" IS '检测指标 ID';
COMMENT ON COLUMN mes_qc_indicator_result_detail."value" IS '检测值';
COMMENT ON COLUMN mes_qc_indicator_result_detail."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS mes_qc_indicator_result_detail_seq;

CREATE INDEX IF NOT EXISTS idx_mes_qc_indicator_result_detail_result_id ON mes_qc_indicator_result_detail (result_id);
CREATE INDEX IF NOT EXISTS idx_mes_qc_indicator_result_detail_indicator_id ON mes_qc_indicator_result_detail (indicator_id);

-- ===================== mes_qc_ipqc_line =====================
-- MES 过程检验单行
DROP TABLE IF EXISTS mes_qc_ipqc_line;
CREATE TABLE IF NOT EXISTS mes_qc_ipqc_line (
    "id"                bigint      NOT NULL,
    "ipqc_id"           bigint,
    "indicator_id"      bigint,
    "tool"              varchar(64),
    "check_method"      varchar(255),
    "standard_value"    numeric(20,4),
    "unit_measure_id"   bigint,
    "max_threshold"     numeric(20,4),
    "min_threshold"     numeric(20,4),
    "critical_quantity" int4,
    "major_quantity"     int4,
    "minor_quantity"     int4,
    "remark"            varchar(500),
    "creator"           varchar(64) DEFAULT '',
    "create_time"       timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"           varchar(64) DEFAULT '',
    "update_time"       timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"           int2        NOT NULL DEFAULT 0,
    "tenant_id"         bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE mes_qc_ipqc_line IS 'MES 过程检验单行';
COMMENT ON COLUMN mes_qc_ipqc_line."id" IS '编号';
COMMENT ON COLUMN mes_qc_ipqc_line."ipqc_id" IS '过程检验单 ID';
COMMENT ON COLUMN mes_qc_ipqc_line."indicator_id" IS '检测指标 ID';
COMMENT ON COLUMN mes_qc_ipqc_line."tool" IS '检测工具';
COMMENT ON COLUMN mes_qc_ipqc_line."check_method" IS '检测方法';
COMMENT ON COLUMN mes_qc_ipqc_line."standard_value" IS '标准值';
COMMENT ON COLUMN mes_qc_ipqc_line."unit_measure_id" IS '计量单位 ID';
COMMENT ON COLUMN mes_qc_ipqc_line."max_threshold" IS '误差上限';
COMMENT ON COLUMN mes_qc_ipqc_line."min_threshold" IS '误差下限';
COMMENT ON COLUMN mes_qc_ipqc_line."critical_quantity" IS '致命缺陷数量';
COMMENT ON COLUMN mes_qc_ipqc_line."major_quantity" IS '严重缺陷数量';
COMMENT ON COLUMN mes_qc_ipqc_line."minor_quantity" IS '轻微缺陷数量';
COMMENT ON COLUMN mes_qc_ipqc_line."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS mes_qc_ipqc_line_seq;

CREATE INDEX IF NOT EXISTS idx_mes_qc_ipqc_line_ipqc_id ON mes_qc_ipqc_line (ipqc_id);
CREATE INDEX IF NOT EXISTS idx_mes_qc_ipqc_line_indicator_id ON mes_qc_ipqc_line (indicator_id);

-- ===================== mes_qc_iqc_line =====================
-- MES 来料检验单行
DROP TABLE IF EXISTS mes_qc_iqc_line;
CREATE TABLE IF NOT EXISTS mes_qc_iqc_line (
    "id"                bigint      NOT NULL,
    "iqc_id"            bigint,
    "indicator_id"      bigint,
    "tool"              varchar(64),
    "check_method"      varchar(255),
    "standard_value"    numeric(20,4),
    "unit_measure_id"   bigint,
    "max_threshold"     numeric(20,4),
    "min_threshold"     numeric(20,4),
    "critical_quantity" int4,
    "major_quantity"     int4,
    "minor_quantity"     int4,
    "remark"            varchar(500),
    "creator"           varchar(64) DEFAULT '',
    "create_time"       timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"           varchar(64) DEFAULT '',
    "update_time"       timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"           int2        NOT NULL DEFAULT 0,
    "tenant_id"         bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE mes_qc_iqc_line IS 'MES 来料检验单行';
COMMENT ON COLUMN mes_qc_iqc_line."id" IS '编号';
COMMENT ON COLUMN mes_qc_iqc_line."iqc_id" IS '来料检验单 ID';
COMMENT ON COLUMN mes_qc_iqc_line."indicator_id" IS '检测指标 ID';
COMMENT ON COLUMN mes_qc_iqc_line."tool" IS '检测工具';
COMMENT ON COLUMN mes_qc_iqc_line."check_method" IS '检测方法';
COMMENT ON COLUMN mes_qc_iqc_line."standard_value" IS '标准值';
COMMENT ON COLUMN mes_qc_iqc_line."unit_measure_id" IS '计量单位 ID';
COMMENT ON COLUMN mes_qc_iqc_line."max_threshold" IS '误差上限';
COMMENT ON COLUMN mes_qc_iqc_line."min_threshold" IS '误差下限';
COMMENT ON COLUMN mes_qc_iqc_line."critical_quantity" IS '致命缺陷数量';
COMMENT ON COLUMN mes_qc_iqc_line."major_quantity" IS '严重缺陷数量';
COMMENT ON COLUMN mes_qc_iqc_line."minor_quantity" IS '轻微缺陷数量';
COMMENT ON COLUMN mes_qc_iqc_line."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS mes_qc_iqc_line_seq;

CREATE INDEX IF NOT EXISTS idx_mes_qc_iqc_line_iqc_id ON mes_qc_iqc_line (iqc_id);
CREATE INDEX IF NOT EXISTS idx_mes_qc_iqc_line_indicator_id ON mes_qc_iqc_line (indicator_id);

-- ===================== mes_qc_oqc_line =====================
-- MES 出货检验单行
DROP TABLE IF EXISTS mes_qc_oqc_line;
CREATE TABLE IF NOT EXISTS mes_qc_oqc_line (
    "id"                bigint      NOT NULL,
    "oqc_id"            bigint,
    "indicator_id"      bigint,
    "tool"              varchar(64),
    "check_method"      varchar(255),
    "standard_value"    numeric(20,4),
    "unit_measure_id"   bigint,
    "max_threshold"     numeric(20,4),
    "min_threshold"     numeric(20,4),
    "critical_quantity" int4,
    "major_quantity"     int4,
    "minor_quantity"     int4,
    "remark"            varchar(500),
    "creator"           varchar(64) DEFAULT '',
    "create_time"       timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"           varchar(64) DEFAULT '',
    "update_time"       timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"           int2        NOT NULL DEFAULT 0,
    "tenant_id"         bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE mes_qc_oqc_line IS 'MES 出货检验单行';
COMMENT ON COLUMN mes_qc_oqc_line."id" IS '编号';
COMMENT ON COLUMN mes_qc_oqc_line."oqc_id" IS '出货检验单 ID';
COMMENT ON COLUMN mes_qc_oqc_line."indicator_id" IS '检测指标 ID';
COMMENT ON COLUMN mes_qc_oqc_line."tool" IS '检测工具';
COMMENT ON COLUMN mes_qc_oqc_line."check_method" IS '检测方法';
COMMENT ON COLUMN mes_qc_oqc_line."standard_value" IS '标准值';
COMMENT ON COLUMN mes_qc_oqc_line."unit_measure_id" IS '计量单位 ID';
COMMENT ON COLUMN mes_qc_oqc_line."max_threshold" IS '误差上限';
COMMENT ON COLUMN mes_qc_oqc_line."min_threshold" IS '误差下限';
COMMENT ON COLUMN mes_qc_oqc_line."critical_quantity" IS '致命缺陷数量';
COMMENT ON COLUMN mes_qc_oqc_line."major_quantity" IS '严重缺陷数量';
COMMENT ON COLUMN mes_qc_oqc_line."minor_quantity" IS '轻微缺陷数量';
COMMENT ON COLUMN mes_qc_oqc_line."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS mes_qc_oqc_line_seq;

CREATE INDEX IF NOT EXISTS idx_mes_qc_oqc_line_oqc_id ON mes_qc_oqc_line (oqc_id);
CREATE INDEX IF NOT EXISTS idx_mes_qc_oqc_line_indicator_id ON mes_qc_oqc_line (indicator_id);

-- ===================== mes_qc_rqc_line =====================
-- MES 退货检验单行
DROP TABLE IF EXISTS mes_qc_rqc_line;
CREATE TABLE IF NOT EXISTS mes_qc_rqc_line (
    "id"                bigint      NOT NULL,
    "rqc_id"            bigint,
    "indicator_id"      bigint,
    "tool"              varchar(64),
    "check_method"      varchar(255),
    "standard_value"    numeric(20,4),
    "unit_measure_id"   bigint,
    "max_threshold"     numeric(20,4),
    "min_threshold"     numeric(20,4),
    "critical_quantity" int4,
    "major_quantity"     int4,
    "minor_quantity"     int4,
    "remark"            varchar(500),
    "creator"           varchar(64) DEFAULT '',
    "create_time"       timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"           varchar(64) DEFAULT '',
    "update_time"       timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"           int2        NOT NULL DEFAULT 0,
    "tenant_id"         bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE mes_qc_rqc_line IS 'MES 退货检验单行';
COMMENT ON COLUMN mes_qc_rqc_line."id" IS '编号';
COMMENT ON COLUMN mes_qc_rqc_line."rqc_id" IS '退货检验单 ID';
COMMENT ON COLUMN mes_qc_rqc_line."indicator_id" IS '检测指标 ID';
COMMENT ON COLUMN mes_qc_rqc_line."tool" IS '检测工具';
COMMENT ON COLUMN mes_qc_rqc_line."check_method" IS '检测方法';
COMMENT ON COLUMN mes_qc_rqc_line."standard_value" IS '标准值';
COMMENT ON COLUMN mes_qc_rqc_line."unit_measure_id" IS '计量单位 ID';
COMMENT ON COLUMN mes_qc_rqc_line."max_threshold" IS '误差上限';
COMMENT ON COLUMN mes_qc_rqc_line."min_threshold" IS '误差下限';
COMMENT ON COLUMN mes_qc_rqc_line."critical_quantity" IS '致命缺陷数量';
COMMENT ON COLUMN mes_qc_rqc_line."major_quantity" IS '严重缺陷数量';
COMMENT ON COLUMN mes_qc_rqc_line."minor_quantity" IS '轻微缺陷数量';
COMMENT ON COLUMN mes_qc_rqc_line."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS mes_qc_rqc_line_seq;

CREATE INDEX IF NOT EXISTS idx_mes_qc_rqc_line_rqc_id ON mes_qc_rqc_line (rqc_id);
CREATE INDEX IF NOT EXISTS idx_mes_qc_rqc_line_indicator_id ON mes_qc_rqc_line (indicator_id);

-- ===================== mes_qc_template_indicator =====================
-- MES 质检方案-检测指标项
DROP TABLE IF EXISTS mes_qc_template_indicator;
CREATE TABLE IF NOT EXISTS mes_qc_template_indicator (
    "id"               bigint      NOT NULL,
    "template_id"      bigint,
    "indicator_id"     bigint,
    "check_method"     varchar(255),
    "standard_value"   numeric(20,4),
    "unit_measure_id"  bigint,
    "threshold_max"    numeric(20,4),
    "threshold_min"    numeric(20,4),
    "doc_url"          varchar(500),
    "remark"           varchar(500),
    "creator"          varchar(64) DEFAULT '',
    "create_time"      timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"          varchar(64) DEFAULT '',
    "update_time"      timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"          int2        NOT NULL DEFAULT 0,
    "tenant_id"        bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE mes_qc_template_indicator IS 'MES 质检方案-检测指标项';
COMMENT ON COLUMN mes_qc_template_indicator."id" IS '编号';
COMMENT ON COLUMN mes_qc_template_indicator."template_id" IS '质检方案编号';
COMMENT ON COLUMN mes_qc_template_indicator."indicator_id" IS '质检指标编号';
COMMENT ON COLUMN mes_qc_template_indicator."check_method" IS '检测方法';
COMMENT ON COLUMN mes_qc_template_indicator."standard_value" IS '标准值';
COMMENT ON COLUMN mes_qc_template_indicator."unit_measure_id" IS '计量单位编号';
COMMENT ON COLUMN mes_qc_template_indicator."threshold_max" IS '误差上限';
COMMENT ON COLUMN mes_qc_template_indicator."threshold_min" IS '误差下限';
COMMENT ON COLUMN mes_qc_template_indicator."doc_url" IS '说明图 URL';
COMMENT ON COLUMN mes_qc_template_indicator."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS mes_qc_template_indicator_seq;

CREATE INDEX IF NOT EXISTS idx_mes_qc_template_indicator_template_id ON mes_qc_template_indicator (template_id);
CREATE INDEX IF NOT EXISTS idx_mes_qc_template_indicator_indicator_id ON mes_qc_template_indicator (indicator_id);

-- ===================== mes_qc_template_item =====================
-- MES 质检方案-产品关联
DROP TABLE IF EXISTS mes_qc_template_item;
CREATE TABLE IF NOT EXISTS mes_qc_template_item (
    "id"                  bigint      NOT NULL,
    "template_id"         bigint,
    "item_id"             bigint,
    "quantity_check"      int4,
    "quantity_unqualified" int4,
    "critical_rate"       numeric(20,4),
    "major_rate"          numeric(20,4),
    "minor_rate"          numeric(20,4),
    "remark"              varchar(500),
    "creator"             varchar(64) DEFAULT '',
    "create_time"         timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"             varchar(64) DEFAULT '',
    "update_time"         timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"             int2        NOT NULL DEFAULT 0,
    "tenant_id"           bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE mes_qc_template_item IS 'MES 质检方案-产品关联';
COMMENT ON COLUMN mes_qc_template_item."id" IS '编号';
COMMENT ON COLUMN mes_qc_template_item."template_id" IS '质检方案编号';
COMMENT ON COLUMN mes_qc_template_item."item_id" IS '产品物料编号';
COMMENT ON COLUMN mes_qc_template_item."quantity_check" IS '最低检测数';
COMMENT ON COLUMN mes_qc_template_item."quantity_unqualified" IS '最大不合格数（0=不启用）';
COMMENT ON COLUMN mes_qc_template_item."critical_rate" IS '最大致命缺陷率（%，0=不允许）';
COMMENT ON COLUMN mes_qc_template_item."major_rate" IS '最大严重缺陷率（%，0=不允许）';
COMMENT ON COLUMN mes_qc_template_item."minor_rate" IS '最大轻微缺陷率（%）';
COMMENT ON COLUMN mes_qc_template_item."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS mes_qc_template_item_seq;

CREATE INDEX IF NOT EXISTS idx_mes_qc_template_item_template_id ON mes_qc_template_item (template_id);
CREATE INDEX IF NOT EXISTS idx_mes_qc_template_item_item_id ON mes_qc_template_item (item_id);


-- ============================================================
-- MES 模块表 DDL（PostgreSQL）— 从本地 DO 反推
-- ============================================================
-- 来源：从 yudao-module-mes 的 *DO.java 反推生成（非社区 MySQL 脚本转换）
-- 模块：MES wm 仓储子域 A（arrival/barcode/misc/outsource/package/product 单据与明细）
-- 数据库：PostgreSQL（docker-db_postgres-1 / 127.0.0.1:5433 / db=yudao）
-- 安全：仅 DROP IF EXISTS + CREATE IF NOT EXISTS（幂等）；无 INSERT、无外键约束、无 ENGINE
-- 约定：见同目录 ../_CONVENTIONS.md（tenant_id 全表必加、序列取 @KeySequence 字面值、JSON→text）
-- 类型对齐基准 yudao-modules.sql mes_pro_feedback：int8/numeric(14,2)/varchar(N)/int2、固定尾部
-- 运行：psql -1 -v ON_ERROR_STOP=1 -f wm_a.sql
-- ============================================================

-- ===================== mes_wm_arrival_notice_line =====================
-- MES 到货通知单行
DROP TABLE IF EXISTS "mes_wm_arrival_notice_line";
CREATE TABLE IF NOT EXISTS "mes_wm_arrival_notice_line" (
    "id"                  int8          NOT NULL,
    "notice_id"           int8          NULL DEFAULT NULL,
    "item_id"             int8          NULL DEFAULT NULL,
    "arrival_quantity"    numeric(14,2) NULL DEFAULT NULL,
    "qualified_quantity"  numeric(14,2) NULL DEFAULT NULL,
    "iqc_check_flag"      int2          NOT NULL DEFAULT 0,
    "iqc_id"              int8          NULL DEFAULT NULL,
    "remark"              varchar(500)  NULL DEFAULT NULL,
    "creator"             varchar(64)   DEFAULT '',
    "create_time"         timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"             varchar(64)   DEFAULT '',
    "update_time"         timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"             int2          NOT NULL DEFAULT 0,
    "tenant_id"           int8          NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_wm_arrival_notice_line" IS 'MES 到货通知单行';
COMMENT ON COLUMN "mes_wm_arrival_notice_line"."notice_id" IS '到货通知单编号';
COMMENT ON COLUMN "mes_wm_arrival_notice_line"."item_id" IS '物料编号';
COMMENT ON COLUMN "mes_wm_arrival_notice_line"."arrival_quantity" IS '到货数量';
COMMENT ON COLUMN "mes_wm_arrival_notice_line"."qualified_quantity" IS '合格数量';
COMMENT ON COLUMN "mes_wm_arrival_notice_line"."iqc_check_flag" IS '是否需要来料检验';
COMMENT ON COLUMN "mes_wm_arrival_notice_line"."iqc_id" IS '来料检验单编号';
COMMENT ON COLUMN "mes_wm_arrival_notice_line"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS "mes_wm_arrival_notice_line_seq";

CREATE INDEX IF NOT EXISTS "idx_mes_wm_arrival_notice_line_notice_id" ON "mes_wm_arrival_notice_line" ("notice_id");
CREATE INDEX IF NOT EXISTS "idx_mes_wm_arrival_notice_line_item_id" ON "mes_wm_arrival_notice_line" ("item_id");
CREATE INDEX IF NOT EXISTS "idx_mes_wm_arrival_notice_line_iqc_id" ON "mes_wm_arrival_notice_line" ("iqc_id");

-- ===================== mes_wm_barcode =====================
-- MES 条码清单
DROP TABLE IF EXISTS "mes_wm_barcode";
CREATE TABLE IF NOT EXISTS "mes_wm_barcode" (
    "id"          int8          NOT NULL,
    "config_id"   int8          NULL DEFAULT NULL,
    "format"      int2          NULL DEFAULT NULL,
    "biz_type"    int2          NULL DEFAULT NULL,
    "content"     text          NULL DEFAULT NULL,
    "biz_id"      int8          NULL DEFAULT NULL,
    "biz_code"    varchar(64)   NULL DEFAULT NULL,
    "biz_name"    varchar(128)  NULL DEFAULT NULL,
    "status"      int2          NULL DEFAULT NULL,
    "remark"      varchar(500)  NULL DEFAULT NULL,
    "creator"     varchar(64)   DEFAULT '',
    "create_time" timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"     varchar(64)   DEFAULT '',
    "update_time" timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"     int2          NOT NULL DEFAULT 0,
    "tenant_id"   int8          NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_wm_barcode" IS 'MES 条码清单';
COMMENT ON COLUMN "mes_wm_barcode"."config_id" IS '条码配置编号';
COMMENT ON COLUMN "mes_wm_barcode"."format" IS '条码格式';
COMMENT ON COLUMN "mes_wm_barcode"."biz_type" IS '业务类型';
COMMENT ON COLUMN "mes_wm_barcode"."content" IS '条码内容（核心字段，前端根据此内容生成条码图片）';
COMMENT ON COLUMN "mes_wm_barcode"."biz_id" IS '业务编号';
COMMENT ON COLUMN "mes_wm_barcode"."biz_code" IS '业务编码';
COMMENT ON COLUMN "mes_wm_barcode"."biz_name" IS '业务名称';
COMMENT ON COLUMN "mes_wm_barcode"."status" IS '状态';
COMMENT ON COLUMN "mes_wm_barcode"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS "mes_wm_barcode_seq";

CREATE INDEX IF NOT EXISTS "idx_mes_wm_barcode_config_id" ON "mes_wm_barcode" ("config_id");
CREATE INDEX IF NOT EXISTS "idx_mes_wm_barcode_biz_id" ON "mes_wm_barcode" ("biz_id");

-- ===================== mes_wm_barcode_config =====================
-- MES 条码配置
DROP TABLE IF EXISTS "mes_wm_barcode_config";
CREATE TABLE IF NOT EXISTS "mes_wm_barcode_config" (
    "id"                int8          NOT NULL,
    "format"            int2          NULL DEFAULT NULL,
    "biz_type"          int2          NULL DEFAULT NULL,
    "content_format"    varchar(255)  NULL DEFAULT NULL,
    "content_example"   varchar(255)  NULL DEFAULT NULL,
    "auto_generate_flag" int2         NOT NULL DEFAULT 0,
    "default_template"  varchar(255)  NULL DEFAULT NULL,
    "status"            int2          NULL DEFAULT NULL,
    "remark"            varchar(500)  NULL DEFAULT NULL,
    "creator"           varchar(64)   DEFAULT '',
    "create_time"       timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"           varchar(64)   DEFAULT '',
    "update_time"       timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"           int2          NOT NULL DEFAULT 0,
    "tenant_id"         int8          NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_wm_barcode_config" IS 'MES 条码配置';
COMMENT ON COLUMN "mes_wm_barcode_config"."format" IS '条码格式';
COMMENT ON COLUMN "mes_wm_barcode_config"."biz_type" IS '业务类型';
COMMENT ON COLUMN "mes_wm_barcode_config"."content_format" IS '内容格式模板（支持 {BUSINESSCODE} 占位符）';
COMMENT ON COLUMN "mes_wm_barcode_config"."content_example" IS '内容样例';
COMMENT ON COLUMN "mes_wm_barcode_config"."auto_generate_flag" IS '是否自动生成';
COMMENT ON COLUMN "mes_wm_barcode_config"."default_template" IS '默认打印模板';
COMMENT ON COLUMN "mes_wm_barcode_config"."status" IS '状态';
COMMENT ON COLUMN "mes_wm_barcode_config"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS "mes_wm_barcode_config_seq";

-- ===================== mes_wm_misc_issue =====================
-- MES 杂项出库单
DROP TABLE IF EXISTS "mes_wm_misc_issue";
CREATE TABLE IF NOT EXISTS "mes_wm_misc_issue" (
    "id"              int8          NOT NULL,
    "code"            varchar(64)   NULL DEFAULT NULL,
    "name"            varchar(128)  NULL DEFAULT NULL,
    "type"            int2          NULL DEFAULT NULL,
    "source_doc_type" varchar(32)   NULL DEFAULT NULL,
    "source_doc_id"   int8          NULL DEFAULT NULL,
    "source_doc_code" varchar(64)   NULL DEFAULT NULL,
    "issue_date"      timestamp     NULL DEFAULT NULL,
    "status"          int2          NULL DEFAULT NULL,
    "remark"          varchar(500)  NULL DEFAULT NULL,
    "creator"         varchar(64)   DEFAULT '',
    "create_time"     timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"         varchar(64)   DEFAULT '',
    "update_time"     timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"         int2          NOT NULL DEFAULT 0,
    "tenant_id"       int8          NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_wm_misc_issue" IS 'MES 杂项出库单';
COMMENT ON COLUMN "mes_wm_misc_issue"."code" IS '出库单编号';
COMMENT ON COLUMN "mes_wm_misc_issue"."name" IS '出库单名称';
COMMENT ON COLUMN "mes_wm_misc_issue"."type" IS '杂项类型';
COMMENT ON COLUMN "mes_wm_misc_issue"."source_doc_type" IS '来源单据类型';
COMMENT ON COLUMN "mes_wm_misc_issue"."source_doc_id" IS '来源单据 ID';
COMMENT ON COLUMN "mes_wm_misc_issue"."source_doc_code" IS '来源单据编号';
COMMENT ON COLUMN "mes_wm_misc_issue"."issue_date" IS '出库日期';
COMMENT ON COLUMN "mes_wm_misc_issue"."status" IS '状态';
COMMENT ON COLUMN "mes_wm_misc_issue"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS "mes_wm_misc_issue_seq";

CREATE UNIQUE INDEX IF NOT EXISTS "uk_mes_wm_misc_issue_code" ON "mes_wm_misc_issue" ("code", tenant_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS "idx_mes_wm_misc_issue_source_doc_id" ON "mes_wm_misc_issue" ("source_doc_id");

-- ===================== mes_wm_misc_issue_detail =====================
-- MES 杂项出库明细
DROP TABLE IF EXISTS "mes_wm_misc_issue_detail";
CREATE TABLE IF NOT EXISTS "mes_wm_misc_issue_detail" (
    "id"               int8          NOT NULL,
    "issue_id"         int8          NULL DEFAULT NULL,
    "line_id"          int8          NULL DEFAULT NULL,
    "material_stock_id" int8        NULL DEFAULT NULL,
    "item_id"          int8          NULL DEFAULT NULL,
    "quantity"         numeric(14,2) NULL DEFAULT NULL,
    "batch_id"         int8          NULL DEFAULT NULL,
    "batch_code"       varchar(64)   NULL DEFAULT NULL,
    "warehouse_id"     int8          NULL DEFAULT NULL,
    "location_id"      int8          NULL DEFAULT NULL,
    "area_id"          int8          NULL DEFAULT NULL,
    "remark"           varchar(500)  NULL DEFAULT NULL,
    "creator"          varchar(64)   DEFAULT '',
    "create_time"      timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"          varchar(64)   DEFAULT '',
    "update_time"      timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"          int2          NOT NULL DEFAULT 0,
    "tenant_id"        int8          NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_wm_misc_issue_detail" IS 'MES 杂项出库明细';
COMMENT ON COLUMN "mes_wm_misc_issue_detail"."issue_id" IS '出库单ID';
COMMENT ON COLUMN "mes_wm_misc_issue_detail"."line_id" IS '行ID';
COMMENT ON COLUMN "mes_wm_misc_issue_detail"."material_stock_id" IS '库存记录ID';
COMMENT ON COLUMN "mes_wm_misc_issue_detail"."item_id" IS '物料ID';
COMMENT ON COLUMN "mes_wm_misc_issue_detail"."quantity" IS '出库数量';
COMMENT ON COLUMN "mes_wm_misc_issue_detail"."batch_id" IS '批次ID';
COMMENT ON COLUMN "mes_wm_misc_issue_detail"."batch_code" IS '批次号';
COMMENT ON COLUMN "mes_wm_misc_issue_detail"."warehouse_id" IS '仓库ID';
COMMENT ON COLUMN "mes_wm_misc_issue_detail"."location_id" IS '库区ID';
COMMENT ON COLUMN "mes_wm_misc_issue_detail"."area_id" IS '库位ID';
COMMENT ON COLUMN "mes_wm_misc_issue_detail"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS "mes_wm_misc_issue_detail_seq";

CREATE INDEX IF NOT EXISTS "idx_mes_wm_misc_issue_detail_issue_id" ON "mes_wm_misc_issue_detail" ("issue_id");
CREATE INDEX IF NOT EXISTS "idx_mes_wm_misc_issue_detail_line_id" ON "mes_wm_misc_issue_detail" ("line_id");
CREATE INDEX IF NOT EXISTS "idx_mes_wm_misc_issue_detail_item_id" ON "mes_wm_misc_issue_detail" ("item_id");
CREATE INDEX IF NOT EXISTS "idx_mes_wm_misc_issue_detail_warehouse_id" ON "mes_wm_misc_issue_detail" ("warehouse_id");

-- ===================== mes_wm_misc_issue_line =====================
-- MES 杂项出库单行
DROP TABLE IF EXISTS "mes_wm_misc_issue_line";
CREATE TABLE IF NOT EXISTS "mes_wm_misc_issue_line" (
    "id"                 int8          NOT NULL,
    "issue_id"           int8          NULL DEFAULT NULL,
    "source_doc_line_id" int8          NULL DEFAULT NULL,
    "material_stock_id"  int8          NULL DEFAULT NULL,
    "item_id"            int8          NULL DEFAULT NULL,
    "quantity"           numeric(14,2) NULL DEFAULT NULL,
    "batch_id"           int8          NULL DEFAULT NULL,
    "batch_code"         varchar(64)   NULL DEFAULT NULL,
    "warehouse_id"       int8          NULL DEFAULT NULL,
    "location_id"        int8          NULL DEFAULT NULL,
    "area_id"            int8          NULL DEFAULT NULL,
    "remark"             varchar(500)  NULL DEFAULT NULL,
    "creator"            varchar(64)   DEFAULT '',
    "create_time"        timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"            varchar(64)   DEFAULT '',
    "update_time"        timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"            int2          NOT NULL DEFAULT 0,
    "tenant_id"           int8          NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_wm_misc_issue_line" IS 'MES 杂项出库单行';
COMMENT ON COLUMN "mes_wm_misc_issue_line"."issue_id" IS '出库单编号';
COMMENT ON COLUMN "mes_wm_misc_issue_line"."source_doc_line_id" IS '来源单据行ID';
COMMENT ON COLUMN "mes_wm_misc_issue_line"."material_stock_id" IS '库存记录ID';
COMMENT ON COLUMN "mes_wm_misc_issue_line"."item_id" IS '物料编号';
COMMENT ON COLUMN "mes_wm_misc_issue_line"."quantity" IS '出库数量';
COMMENT ON COLUMN "mes_wm_misc_issue_line"."batch_id" IS '批次编号';
COMMENT ON COLUMN "mes_wm_misc_issue_line"."batch_code" IS '批次号';
COMMENT ON COLUMN "mes_wm_misc_issue_line"."warehouse_id" IS '仓库编号';
COMMENT ON COLUMN "mes_wm_misc_issue_line"."location_id" IS '库区编号';
COMMENT ON COLUMN "mes_wm_misc_issue_line"."area_id" IS '库位编号';
COMMENT ON COLUMN "mes_wm_misc_issue_line"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS "mes_wm_misc_issue_line_seq";

CREATE INDEX IF NOT EXISTS "idx_mes_wm_misc_issue_line_issue_id" ON "mes_wm_misc_issue_line" ("issue_id");
CREATE INDEX IF NOT EXISTS "idx_mes_wm_misc_issue_line_item_id" ON "mes_wm_misc_issue_line" ("item_id");
CREATE INDEX IF NOT EXISTS "idx_mes_wm_misc_issue_line_warehouse_id" ON "mes_wm_misc_issue_line" ("warehouse_id");

-- ===================== mes_wm_misc_receipt =====================
-- MES 杂项入库单
DROP TABLE IF EXISTS "mes_wm_misc_receipt";
CREATE TABLE IF NOT EXISTS "mes_wm_misc_receipt" (
    "id"              int8          NOT NULL,
    "code"            varchar(64)   NULL DEFAULT NULL,
    "name"            varchar(128)  NULL DEFAULT NULL,
    "type"            int2          NULL DEFAULT NULL,
    "source_doc_type" varchar(32)   NULL DEFAULT NULL,
    "source_doc_id"   int8          NULL DEFAULT NULL,
    "source_doc_code" varchar(64)   NULL DEFAULT NULL,
    "receipt_date"    timestamp     NULL DEFAULT NULL,
    "status"          int2          NULL DEFAULT NULL,
    "remark"          varchar(500)  NULL DEFAULT NULL,
    "creator"         varchar(64)   DEFAULT '',
    "create_time"     timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"         varchar(64)   DEFAULT '',
    "update_time"     timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"         int2          NOT NULL DEFAULT 0,
    "tenant_id"       int8          NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_wm_misc_receipt" IS 'MES 杂项入库单';
COMMENT ON COLUMN "mes_wm_misc_receipt"."code" IS '入库单编码';
COMMENT ON COLUMN "mes_wm_misc_receipt"."name" IS '入库单名称';
COMMENT ON COLUMN "mes_wm_misc_receipt"."type" IS '杂项类型';
COMMENT ON COLUMN "mes_wm_misc_receipt"."source_doc_type" IS '来源单据类型';
COMMENT ON COLUMN "mes_wm_misc_receipt"."source_doc_id" IS '来源单据 ID';
COMMENT ON COLUMN "mes_wm_misc_receipt"."source_doc_code" IS '来源单据编码';
COMMENT ON COLUMN "mes_wm_misc_receipt"."receipt_date" IS '入库日期';
COMMENT ON COLUMN "mes_wm_misc_receipt"."status" IS '状态';
COMMENT ON COLUMN "mes_wm_misc_receipt"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS "mes_wm_misc_receipt_seq";

CREATE UNIQUE INDEX IF NOT EXISTS "uk_mes_wm_misc_receipt_code" ON "mes_wm_misc_receipt" ("code", tenant_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS "idx_mes_wm_misc_receipt_source_doc_id" ON "mes_wm_misc_receipt" ("source_doc_id");

-- ===================== mes_wm_misc_receipt_detail =====================
-- MES 杂项入库明细
DROP TABLE IF EXISTS "mes_wm_misc_receipt_detail";
CREATE TABLE IF NOT EXISTS "mes_wm_misc_receipt_detail" (
    "id"            int8          NOT NULL,
    "receipt_id"    int8          NULL DEFAULT NULL,
    "line_id"       int8          NULL DEFAULT NULL,
    "item_id"       int8          NULL DEFAULT NULL,
    "quantity"      numeric(14,2) NULL DEFAULT NULL,
    "batch_code"    varchar(64)   NULL DEFAULT NULL,
    "warehouse_id"  int8          NULL DEFAULT NULL,
    "location_id"   int8          NULL DEFAULT NULL,
    "area_id"       int8          NULL DEFAULT NULL,
    "remark"        varchar(500)  NULL DEFAULT NULL,
    "creator"       varchar(64)   DEFAULT '',
    "create_time"   timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"       varchar(64)   DEFAULT '',
    "update_time"   timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"       int2          NOT NULL DEFAULT 0,
    "tenant_id"     int8          NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_wm_misc_receipt_detail" IS 'MES 杂项入库明细';
COMMENT ON COLUMN "mes_wm_misc_receipt_detail"."receipt_id" IS '入库单ID';
COMMENT ON COLUMN "mes_wm_misc_receipt_detail"."line_id" IS '行ID';
COMMENT ON COLUMN "mes_wm_misc_receipt_detail"."item_id" IS '物料ID';
COMMENT ON COLUMN "mes_wm_misc_receipt_detail"."quantity" IS '入库数量';
COMMENT ON COLUMN "mes_wm_misc_receipt_detail"."batch_code" IS '批次号';
COMMENT ON COLUMN "mes_wm_misc_receipt_detail"."warehouse_id" IS '仓库ID';
COMMENT ON COLUMN "mes_wm_misc_receipt_detail"."location_id" IS '库区ID';
COMMENT ON COLUMN "mes_wm_misc_receipt_detail"."area_id" IS '库位ID';
COMMENT ON COLUMN "mes_wm_misc_receipt_detail"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS "mes_wm_misc_receipt_detail_seq";

CREATE INDEX IF NOT EXISTS "idx_mes_wm_misc_receipt_detail_receipt_id" ON "mes_wm_misc_receipt_detail" ("receipt_id");
CREATE INDEX IF NOT EXISTS "idx_mes_wm_misc_receipt_detail_line_id" ON "mes_wm_misc_receipt_detail" ("line_id");
CREATE INDEX IF NOT EXISTS "idx_mes_wm_misc_receipt_detail_item_id" ON "mes_wm_misc_receipt_detail" ("item_id");
CREATE INDEX IF NOT EXISTS "idx_mes_wm_misc_receipt_detail_warehouse_id" ON "mes_wm_misc_receipt_detail" ("warehouse_id");

-- ===================== mes_wm_misc_receipt_line =====================
-- MES 杂项入库单行
DROP TABLE IF EXISTS "mes_wm_misc_receipt_line";
CREATE TABLE IF NOT EXISTS "mes_wm_misc_receipt_line" (
    "id"            int8          NOT NULL,
    "receipt_id"    int8          NULL DEFAULT NULL,
    "item_id"       int8          NULL DEFAULT NULL,
    "quantity"      numeric(14,2) NULL DEFAULT NULL,
    "batch_code"    varchar(64)   NULL DEFAULT NULL,
    "warehouse_id"  int8          NULL DEFAULT NULL,
    "location_id"   int8          NULL DEFAULT NULL,
    "area_id"       int8          NULL DEFAULT NULL,
    "remark"        varchar(500)  NULL DEFAULT NULL,
    "creator"       varchar(64)   DEFAULT '',
    "create_time"   timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"       varchar(64)   DEFAULT '',
    "update_time"   timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"       int2          NOT NULL DEFAULT 0,
    "tenant_id"     int8          NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_wm_misc_receipt_line" IS 'MES 杂项入库单行';
COMMENT ON COLUMN "mes_wm_misc_receipt_line"."receipt_id" IS '入库单编号';
COMMENT ON COLUMN "mes_wm_misc_receipt_line"."item_id" IS '物料编号';
COMMENT ON COLUMN "mes_wm_misc_receipt_line"."quantity" IS '入库数量';
COMMENT ON COLUMN "mes_wm_misc_receipt_line"."batch_code" IS '批次号';
COMMENT ON COLUMN "mes_wm_misc_receipt_line"."warehouse_id" IS '仓库编号';
COMMENT ON COLUMN "mes_wm_misc_receipt_line"."location_id" IS '库区编号';
COMMENT ON COLUMN "mes_wm_misc_receipt_line"."area_id" IS '库位编号';
COMMENT ON COLUMN "mes_wm_misc_receipt_line"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS "mes_wm_misc_receipt_line_seq";

CREATE INDEX IF NOT EXISTS "idx_mes_wm_misc_receipt_line_receipt_id" ON "mes_wm_misc_receipt_line" ("receipt_id");
CREATE INDEX IF NOT EXISTS "idx_mes_wm_misc_receipt_line_item_id" ON "mes_wm_misc_receipt_line" ("item_id");
CREATE INDEX IF NOT EXISTS "idx_mes_wm_misc_receipt_line_warehouse_id" ON "mes_wm_misc_receipt_line" ("warehouse_id");

-- ===================== mes_wm_outsource_issue =====================
-- MES 外协发料单
DROP TABLE IF EXISTS "mes_wm_outsource_issue";
CREATE TABLE IF NOT EXISTS "mes_wm_outsource_issue" (
    "id"           int8          NOT NULL,
    "code"         varchar(64)   NULL DEFAULT NULL,
    "name"         varchar(128)  NULL DEFAULT NULL,
    "vendor_id"    int8          NULL DEFAULT NULL,
    "work_order_id" int8         NULL DEFAULT NULL,
    "issue_date"   timestamp     NULL DEFAULT NULL,
    "status"       int2          NULL DEFAULT NULL,
    "remark"       varchar(500)  NULL DEFAULT NULL,
    "creator"      varchar(64)   DEFAULT '',
    "create_time"  timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"      varchar(64)   DEFAULT '',
    "update_time"  timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"      int2          NOT NULL DEFAULT 0,
    "tenant_id"    int8          NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_wm_outsource_issue" IS 'MES 外协发料单';
COMMENT ON COLUMN "mes_wm_outsource_issue"."code" IS '发料单编号';
COMMENT ON COLUMN "mes_wm_outsource_issue"."name" IS '发料单名称';
COMMENT ON COLUMN "mes_wm_outsource_issue"."vendor_id" IS '供应商ID';
COMMENT ON COLUMN "mes_wm_outsource_issue"."work_order_id" IS '生产工单ID';
COMMENT ON COLUMN "mes_wm_outsource_issue"."issue_date" IS '发料日期';
COMMENT ON COLUMN "mes_wm_outsource_issue"."status" IS '单据状态';
COMMENT ON COLUMN "mes_wm_outsource_issue"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS "mes_wm_outsource_issue_seq";

CREATE UNIQUE INDEX IF NOT EXISTS "uk_mes_wm_outsource_issue_code" ON "mes_wm_outsource_issue" ("code", tenant_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS "idx_mes_wm_outsource_issue_vendor_id" ON "mes_wm_outsource_issue" ("vendor_id");
CREATE INDEX IF NOT EXISTS "idx_mes_wm_outsource_issue_work_order_id" ON "mes_wm_outsource_issue" ("work_order_id");

-- ===================== mes_wm_outsource_issue_detail =====================
-- MES 外协发料单明细
DROP TABLE IF EXISTS "mes_wm_outsource_issue_detail";
CREATE TABLE IF NOT EXISTS "mes_wm_outsource_issue_detail" (
    "id"                int8          NOT NULL,
    "line_id"           int8          NULL DEFAULT NULL,
    "issue_id"          int8          NULL DEFAULT NULL,
    "material_stock_id" int8          NULL DEFAULT NULL,
    "item_id"           int8          NULL DEFAULT NULL,
    "quantity"          numeric(14,2) NULL DEFAULT NULL,
    "batch_id"          int8          NULL DEFAULT NULL,
    "warehouse_id"      int8          NULL DEFAULT NULL,
    "location_id"       int8          NULL DEFAULT NULL,
    "area_id"           int8          NULL DEFAULT NULL,
    "remark"            varchar(500)  NULL DEFAULT NULL,
    "creator"           varchar(64)   DEFAULT '',
    "create_time"        timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"           varchar(64)   DEFAULT '',
    "update_time"        timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"            int2          NOT NULL DEFAULT 0,
    "tenant_id"          int8          NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_wm_outsource_issue_detail" IS 'MES 外协发料单明细';
COMMENT ON COLUMN "mes_wm_outsource_issue_detail"."line_id" IS '行ID';
COMMENT ON COLUMN "mes_wm_outsource_issue_detail"."issue_id" IS '发料单ID';
COMMENT ON COLUMN "mes_wm_outsource_issue_detail"."material_stock_id" IS '库存ID';
COMMENT ON COLUMN "mes_wm_outsource_issue_detail"."item_id" IS '物料ID';
COMMENT ON COLUMN "mes_wm_outsource_issue_detail"."quantity" IS '数量';
COMMENT ON COLUMN "mes_wm_outsource_issue_detail"."batch_id" IS '批次ID';
COMMENT ON COLUMN "mes_wm_outsource_issue_detail"."warehouse_id" IS '仓库ID';
COMMENT ON COLUMN "mes_wm_outsource_issue_detail"."location_id" IS '库位ID';
COMMENT ON COLUMN "mes_wm_outsource_issue_detail"."area_id" IS '库区ID';
COMMENT ON COLUMN "mes_wm_outsource_issue_detail"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS "mes_wm_outsource_issue_detail_seq";

CREATE INDEX IF NOT EXISTS "idx_mes_wm_outsource_issue_detail_issue_id" ON "mes_wm_outsource_issue_detail" ("issue_id");
CREATE INDEX IF NOT EXISTS "idx_mes_wm_outsource_issue_detail_line_id" ON "mes_wm_outsource_issue_detail" ("line_id");
CREATE INDEX IF NOT EXISTS "idx_mes_wm_outsource_issue_detail_item_id" ON "mes_wm_outsource_issue_detail" ("item_id");
CREATE INDEX IF NOT EXISTS "idx_mes_wm_outsource_issue_detail_warehouse_id" ON "mes_wm_outsource_issue_detail" ("warehouse_id");

-- ===================== mes_wm_outsource_issue_line =====================
-- MES 外协发料单行
DROP TABLE IF EXISTS "mes_wm_outsource_issue_line";
CREATE TABLE IF NOT EXISTS "mes_wm_outsource_issue_line" (
    "id"                int8          NOT NULL,
    "issue_id"          int8          NULL DEFAULT NULL,
    "material_stock_id" int8          NULL DEFAULT NULL,
    "item_id"           int8          NULL DEFAULT NULL,
    "quantity"          numeric(14,2) NULL DEFAULT NULL,
    "batch_id"          int8          NULL DEFAULT NULL,
    "remark"            varchar(500)  NULL DEFAULT NULL,
    "creator"           varchar(64)   DEFAULT '',
    "create_time"       timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"           varchar(64)   DEFAULT '',
    "update_time"       timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"           int2          NOT NULL DEFAULT 0,
    "tenant_id"         int8          NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_wm_outsource_issue_line" IS 'MES 外协发料单行';
COMMENT ON COLUMN "mes_wm_outsource_issue_line"."issue_id" IS '发料单ID';
COMMENT ON COLUMN "mes_wm_outsource_issue_line"."material_stock_id" IS '库存ID';
COMMENT ON COLUMN "mes_wm_outsource_issue_line"."item_id" IS '物料ID';
COMMENT ON COLUMN "mes_wm_outsource_issue_line"."quantity" IS '发料数量';
COMMENT ON COLUMN "mes_wm_outsource_issue_line"."batch_id" IS '批次ID';
COMMENT ON COLUMN "mes_wm_outsource_issue_line"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS "mes_wm_outsource_issue_line_seq";

CREATE INDEX IF NOT EXISTS "idx_mes_wm_outsource_issue_line_issue_id" ON "mes_wm_outsource_issue_line" ("issue_id");
CREATE INDEX IF NOT EXISTS "idx_mes_wm_outsource_issue_line_item_id" ON "mes_wm_outsource_issue_line" ("item_id");

-- ===================== mes_wm_outsource_receipt_detail =====================
-- MES 外协入库明细
DROP TABLE IF EXISTS "mes_wm_outsource_receipt_detail";
CREATE TABLE IF NOT EXISTS "mes_wm_outsource_receipt_detail" (
    "id"            int8          NOT NULL,
    "line_id"       int8          NULL DEFAULT NULL,
    "receipt_id"    int8          NULL DEFAULT NULL,
    "item_id"       int8          NULL DEFAULT NULL,
    "quantity"      numeric(14,2) NULL DEFAULT NULL,
    "batch_id"      int8          NULL DEFAULT NULL,
    "warehouse_id"  int8          NULL DEFAULT NULL,
    "location_id"   int8          NULL DEFAULT NULL,
    "area_id"       int8          NULL DEFAULT NULL,
    "remark"        varchar(500)  NULL DEFAULT NULL,
    "creator"       varchar(64)   DEFAULT '',
    "create_time"   timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"       varchar(64)   DEFAULT '',
    "update_time"   timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"       int2          NOT NULL DEFAULT 0,
    "tenant_id"     int8          NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_wm_outsource_receipt_detail" IS 'MES 外协入库明细';
COMMENT ON COLUMN "mes_wm_outsource_receipt_detail"."line_id" IS '入库单行编号';
COMMENT ON COLUMN "mes_wm_outsource_receipt_detail"."receipt_id" IS '入库单编号';
COMMENT ON COLUMN "mes_wm_outsource_receipt_detail"."item_id" IS '物料编号';
COMMENT ON COLUMN "mes_wm_outsource_receipt_detail"."quantity" IS '上架数量';
COMMENT ON COLUMN "mes_wm_outsource_receipt_detail"."batch_id" IS '批次编号';
COMMENT ON COLUMN "mes_wm_outsource_receipt_detail"."warehouse_id" IS '仓库编号';
COMMENT ON COLUMN "mes_wm_outsource_receipt_detail"."location_id" IS '库区编号';
COMMENT ON COLUMN "mes_wm_outsource_receipt_detail"."area_id" IS '库位编号';
COMMENT ON COLUMN "mes_wm_outsource_receipt_detail"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS "mes_wm_outsource_receipt_detail_seq";

CREATE INDEX IF NOT EXISTS "idx_mes_wm_outsource_receipt_detail_receipt_id" ON "mes_wm_outsource_receipt_detail" ("receipt_id");
CREATE INDEX IF NOT EXISTS "idx_mes_wm_outsource_receipt_detail_line_id" ON "mes_wm_outsource_receipt_detail" ("line_id");
CREATE INDEX IF NOT EXISTS "idx_mes_wm_outsource_receipt_detail_item_id" ON "mes_wm_outsource_receipt_detail" ("item_id");
CREATE INDEX IF NOT EXISTS "idx_mes_wm_outsource_receipt_detail_warehouse_id" ON "mes_wm_outsource_receipt_detail" ("warehouse_id");

-- ===================== mes_wm_outsource_receipt_line =====================
-- MES 外协入库单行
DROP TABLE IF EXISTS "mes_wm_outsource_receipt_line";
CREATE TABLE IF NOT EXISTS "mes_wm_outsource_receipt_line" (
    "id"              int8          NOT NULL,
    "receipt_id"      int8          NULL DEFAULT NULL,
    "item_id"         int8          NULL DEFAULT NULL,
    "quantity"        numeric(14,2) NULL DEFAULT NULL,
    "batch_id"        int8          NULL DEFAULT NULL,
    "batch_code"      varchar(64)   NULL DEFAULT NULL,
    "production_date" timestamp     NULL DEFAULT NULL,
    "expire_date"     timestamp     NULL DEFAULT NULL,
    "lot_number"      varchar(64)   NULL DEFAULT NULL,
    "remark"          varchar(500)  NULL DEFAULT NULL,
    "iqc_id"          int8          NULL DEFAULT NULL,
    "iqc_check_flag"  int2          NOT NULL DEFAULT 0,
    "quality_status"  int2          NULL DEFAULT NULL,
    "creator"         varchar(64)   DEFAULT '',
    "create_time"     timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"         varchar(64)   DEFAULT '',
    "update_time"     timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"         int2          NOT NULL DEFAULT 0,
    "tenant_id"       int8          NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_wm_outsource_receipt_line" IS 'MES 外协入库单行';
COMMENT ON COLUMN "mes_wm_outsource_receipt_line"."receipt_id" IS '入库单编号';
COMMENT ON COLUMN "mes_wm_outsource_receipt_line"."item_id" IS '物料编号';
COMMENT ON COLUMN "mes_wm_outsource_receipt_line"."quantity" IS '入库数量';
COMMENT ON COLUMN "mes_wm_outsource_receipt_line"."batch_id" IS '批次编号';
COMMENT ON COLUMN "mes_wm_outsource_receipt_line"."batch_code" IS '批次号';
COMMENT ON COLUMN "mes_wm_outsource_receipt_line"."production_date" IS '生产日期';
COMMENT ON COLUMN "mes_wm_outsource_receipt_line"."expire_date" IS '有效期';
COMMENT ON COLUMN "mes_wm_outsource_receipt_line"."lot_number" IS '生产批号';
COMMENT ON COLUMN "mes_wm_outsource_receipt_line"."remark" IS '备注';
COMMENT ON COLUMN "mes_wm_outsource_receipt_line"."iqc_id" IS '来料检验单编号';
COMMENT ON COLUMN "mes_wm_outsource_receipt_line"."iqc_check_flag" IS '是否需要质检';
COMMENT ON COLUMN "mes_wm_outsource_receipt_line"."quality_status" IS '质量状态';

CREATE SEQUENCE IF NOT EXISTS "mes_wm_outsource_receipt_line_seq";

CREATE INDEX IF NOT EXISTS "idx_mes_wm_outsource_receipt_line_receipt_id" ON "mes_wm_outsource_receipt_line" ("receipt_id");
CREATE INDEX IF NOT EXISTS "idx_mes_wm_outsource_receipt_line_item_id" ON "mes_wm_outsource_receipt_line" ("item_id");
CREATE INDEX IF NOT EXISTS "idx_mes_wm_outsource_receipt_line_iqc_id" ON "mes_wm_outsource_receipt_line" ("iqc_id");

-- ===================== mes_wm_package =====================
-- MES 装箱单
DROP TABLE IF EXISTS "mes_wm_package";
CREATE TABLE IF NOT EXISTS "mes_wm_package" (
    "id"                 int8          NOT NULL,
    "code"               varchar(64)   NULL DEFAULT NULL,
    "parent_id"          int8          NULL DEFAULT NULL,
    "package_date"       timestamp     NULL DEFAULT NULL,
    "sales_order_code"   varchar(64)   NULL DEFAULT NULL,
    "invoice_code"       varchar(64)   NULL DEFAULT NULL,
    "client_id"          int8          NULL DEFAULT NULL,
    "length"             numeric(14,2) NULL DEFAULT NULL,
    "width"              numeric(14,2) NULL DEFAULT NULL,
    "height"             numeric(14,2) NULL DEFAULT NULL,
    "size_unit_id"       int8          NULL DEFAULT NULL,
    "net_weight"         numeric(14,2) NULL DEFAULT NULL,
    "gross_weight"       numeric(14,2) NULL DEFAULT NULL,
    "weight_unit_id"     int8          NULL DEFAULT NULL,
    "inspector_user_id"  int8          NULL DEFAULT NULL,
    "status"             int2          NULL DEFAULT NULL,
    "remark"             varchar(500)  NULL DEFAULT NULL,
    "creator"            varchar(64)   DEFAULT '',
    "create_time"        timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"            varchar(64)   DEFAULT '',
    "update_time"        timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"            int2          NOT NULL DEFAULT 0,
    "tenant_id"          int8          NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_wm_package" IS 'MES 装箱单';
COMMENT ON COLUMN "mes_wm_package"."code" IS '装箱单编号';
COMMENT ON COLUMN "mes_wm_package"."parent_id" IS '父箱 ID';
COMMENT ON COLUMN "mes_wm_package"."package_date" IS '装箱日期';
COMMENT ON COLUMN "mes_wm_package"."sales_order_code" IS '销售订单编号';
COMMENT ON COLUMN "mes_wm_package"."invoice_code" IS '发票编号';
COMMENT ON COLUMN "mes_wm_package"."client_id" IS '客户 ID';
COMMENT ON COLUMN "mes_wm_package"."length" IS '箱长度';
COMMENT ON COLUMN "mes_wm_package"."width" IS '箱宽度';
COMMENT ON COLUMN "mes_wm_package"."height" IS '箱高度';
COMMENT ON COLUMN "mes_wm_package"."size_unit_id" IS '尺寸单位 ID';
COMMENT ON COLUMN "mes_wm_package"."net_weight" IS '净重';
COMMENT ON COLUMN "mes_wm_package"."gross_weight" IS '毛重';
COMMENT ON COLUMN "mes_wm_package"."weight_unit_id" IS '重量单位 ID';
COMMENT ON COLUMN "mes_wm_package"."inspector_user_id" IS '检查员用户 ID';
COMMENT ON COLUMN "mes_wm_package"."status" IS '状态';
COMMENT ON COLUMN "mes_wm_package"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS "mes_wm_package_seq";

CREATE UNIQUE INDEX IF NOT EXISTS "uk_mes_wm_package_code" ON "mes_wm_package" ("code", tenant_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS "idx_mes_wm_package_parent_id" ON "mes_wm_package" ("parent_id");
CREATE INDEX IF NOT EXISTS "idx_mes_wm_package_client_id" ON "mes_wm_package" ("client_id");

-- ===================== mes_wm_package_line =====================
-- MES 装箱明细
DROP TABLE IF EXISTS "mes_wm_package_line";
CREATE TABLE IF NOT EXISTS "mes_wm_package_line" (
    "id"                int8          NOT NULL,
    "package_id"        int8          NULL DEFAULT NULL,
    "material_stock_id" int8          NULL DEFAULT NULL,
    "item_id"           int8          NULL DEFAULT NULL,
    "quantity"          numeric(14,2) NULL DEFAULT NULL,
    "work_order_id"     int8          NULL DEFAULT NULL,
    "expire_date"       timestamp     NULL DEFAULT NULL,
    "remark"            varchar(500)  NULL DEFAULT NULL,
    "creator"           varchar(64)   DEFAULT '',
    "create_time"       timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"           varchar(64)   DEFAULT '',
    "update_time"       timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"           int2          NOT NULL DEFAULT 0,
    "tenant_id"         int8          NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_wm_package_line" IS 'MES 装箱明细';
COMMENT ON COLUMN "mes_wm_package_line"."package_id" IS '装箱单 ID';
COMMENT ON COLUMN "mes_wm_package_line"."material_stock_id" IS '库存记录 ID';
COMMENT ON COLUMN "mes_wm_package_line"."item_id" IS '产品物料 ID';
COMMENT ON COLUMN "mes_wm_package_line"."quantity" IS '装箱数量';
COMMENT ON COLUMN "mes_wm_package_line"."work_order_id" IS '生产工单 ID';
COMMENT ON COLUMN "mes_wm_package_line"."expire_date" IS '有效期';
COMMENT ON COLUMN "mes_wm_package_line"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS "mes_wm_package_line_seq";

CREATE INDEX IF NOT EXISTS "idx_mes_wm_package_line_package_id" ON "mes_wm_package_line" ("package_id");
CREATE INDEX IF NOT EXISTS "idx_mes_wm_package_line_item_id" ON "mes_wm_package_line" ("item_id");
CREATE INDEX IF NOT EXISTS "idx_mes_wm_package_line_work_order_id" ON "mes_wm_package_line" ("work_order_id");

-- ===================== mes_wm_product_issue =====================
-- MES 领料出库单
DROP TABLE IF EXISTS "mes_wm_product_issue";
CREATE TABLE IF NOT EXISTS "mes_wm_product_issue" (
    "id"             int8          NOT NULL,
    "code"           varchar(64)   NULL DEFAULT NULL,
    "name"           varchar(128)  NULL DEFAULT NULL,
    "workstation_id" int8          NULL DEFAULT NULL,
    "work_order_id"  int8          NULL DEFAULT NULL,
    "task_id"        int8          NULL DEFAULT NULL,
    "issue_date"     timestamp     NULL DEFAULT NULL,
    "required_time"  timestamp     NULL DEFAULT NULL,
    "status"         int2          NULL DEFAULT NULL,
    "remark"         varchar(500)  NULL DEFAULT NULL,
    "creator"        varchar(64)   DEFAULT '',
    "create_time"    timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"        varchar(64)   DEFAULT '',
    "update_time"    timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"        int2          NOT NULL DEFAULT 0,
    "tenant_id"      int8          NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_wm_product_issue" IS 'MES 领料出库单';
COMMENT ON COLUMN "mes_wm_product_issue"."code" IS '领料单编号';
COMMENT ON COLUMN "mes_wm_product_issue"."name" IS '领料单名称';
COMMENT ON COLUMN "mes_wm_product_issue"."workstation_id" IS '工作站 ID';
COMMENT ON COLUMN "mes_wm_product_issue"."work_order_id" IS '生产工单 ID';
COMMENT ON COLUMN "mes_wm_product_issue"."task_id" IS '生产任务 ID';
COMMENT ON COLUMN "mes_wm_product_issue"."issue_date" IS '领料日期';
COMMENT ON COLUMN "mes_wm_product_issue"."required_time" IS '需求时间';
COMMENT ON COLUMN "mes_wm_product_issue"."status" IS '状态';
COMMENT ON COLUMN "mes_wm_product_issue"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS "mes_wm_product_issue_seq";

CREATE UNIQUE INDEX IF NOT EXISTS "uk_mes_wm_product_issue_code" ON "mes_wm_product_issue" ("code", tenant_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS "idx_mes_wm_product_issue_work_order_id" ON "mes_wm_product_issue" ("work_order_id");
CREATE INDEX IF NOT EXISTS "idx_mes_wm_product_issue_task_id" ON "mes_wm_product_issue" ("task_id");
CREATE INDEX IF NOT EXISTS "idx_mes_wm_product_issue_workstation_id" ON "mes_wm_product_issue" ("workstation_id");

-- ===================== mes_wm_product_issue_detail =====================
-- MES 领料出库明细
DROP TABLE IF EXISTS "mes_wm_product_issue_detail";
CREATE TABLE IF NOT EXISTS "mes_wm_product_issue_detail" (
    "id"                int8          NOT NULL,
    "issue_id"          int8          NULL DEFAULT NULL,
    "line_id"           int8          NULL DEFAULT NULL,
    "material_stock_id" int8          NULL DEFAULT NULL,
    "item_id"           int8          NULL DEFAULT NULL,
    "quantity"          numeric(14,2) NULL DEFAULT NULL,
    "batch_id"          int8          NULL DEFAULT NULL,
    "batch_code"        varchar(64)   NULL DEFAULT NULL,
    "warehouse_id"      int8          NULL DEFAULT NULL,
    "location_id"       int8          NULL DEFAULT NULL,
    "area_id"           int8          NULL DEFAULT NULL,
    "remark"            varchar(500)  NULL DEFAULT NULL,
    "creator"           varchar(64)   DEFAULT '',
    "create_time"       timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"           varchar(64)   DEFAULT '',
    "update_time"       timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"           int2          NOT NULL DEFAULT 0,
    "tenant_id"         int8          NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_wm_product_issue_detail" IS 'MES 领料出库明细';
COMMENT ON COLUMN "mes_wm_product_issue_detail"."issue_id" IS '领料单ID';
COMMENT ON COLUMN "mes_wm_product_issue_detail"."line_id" IS '行ID';
COMMENT ON COLUMN "mes_wm_product_issue_detail"."material_stock_id" IS '库存记录ID';
COMMENT ON COLUMN "mes_wm_product_issue_detail"."item_id" IS '物料ID';
COMMENT ON COLUMN "mes_wm_product_issue_detail"."quantity" IS '领料数量';
COMMENT ON COLUMN "mes_wm_product_issue_detail"."batch_id" IS '批次ID';
COMMENT ON COLUMN "mes_wm_product_issue_detail"."batch_code" IS '批次号';
COMMENT ON COLUMN "mes_wm_product_issue_detail"."warehouse_id" IS '仓库ID';
COMMENT ON COLUMN "mes_wm_product_issue_detail"."location_id" IS '库区ID';
COMMENT ON COLUMN "mes_wm_product_issue_detail"."area_id" IS '库位ID';
COMMENT ON COLUMN "mes_wm_product_issue_detail"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS "mes_wm_product_issue_detail_seq";

CREATE INDEX IF NOT EXISTS "idx_mes_wm_product_issue_detail_issue_id" ON "mes_wm_product_issue_detail" ("issue_id");
CREATE INDEX IF NOT EXISTS "idx_mes_wm_product_issue_detail_line_id" ON "mes_wm_product_issue_detail" ("line_id");
CREATE INDEX IF NOT EXISTS "idx_mes_wm_product_issue_detail_item_id" ON "mes_wm_product_issue_detail" ("item_id");
CREATE INDEX IF NOT EXISTS "idx_mes_wm_product_issue_detail_warehouse_id" ON "mes_wm_product_issue_detail" ("warehouse_id");

-- ===================== mes_wm_product_issue_line =====================
-- MES 领料出库单行
DROP TABLE IF EXISTS "mes_wm_product_issue_line";
CREATE TABLE IF NOT EXISTS "mes_wm_product_issue_line" (
    "id"         int8          NOT NULL,
    "issue_id"   int8          NULL DEFAULT NULL,
    "item_id"    int8          NULL DEFAULT NULL,
    "quantity"   numeric(14,2) NULL DEFAULT NULL,
    "batch_id"   int8          NULL DEFAULT NULL,
    "remark"     varchar(500)  NULL DEFAULT NULL,
    "creator"    varchar(64)   DEFAULT '',
    "create_time" timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"    varchar(64)   DEFAULT '',
    "update_time" timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"    int2          NOT NULL DEFAULT 0,
    "tenant_id"  int8          NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_wm_product_issue_line" IS 'MES 领料出库单行';
COMMENT ON COLUMN "mes_wm_product_issue_line"."issue_id" IS '领料单 ID';
COMMENT ON COLUMN "mes_wm_product_issue_line"."item_id" IS '物料 ID';
COMMENT ON COLUMN "mes_wm_product_issue_line"."quantity" IS '领料数量';
COMMENT ON COLUMN "mes_wm_product_issue_line"."batch_id" IS '批次 ID';
COMMENT ON COLUMN "mes_wm_product_issue_line"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS "mes_wm_product_issue_line_seq";

CREATE INDEX IF NOT EXISTS "idx_mes_wm_product_issue_line_issue_id" ON "mes_wm_product_issue_line" ("issue_id");
CREATE INDEX IF NOT EXISTS "idx_mes_wm_product_issue_line_item_id" ON "mes_wm_product_issue_line" ("item_id");



-- ============================================================
-- MES 模块表 DDL（PostgreSQL）— 从本地 DO 反推
-- ============================================================
-- 来源：从 yudao-module-mes 的 *DO.java 反推生成（非社区 MySQL 脚本转换）
-- 子域：MES wm 仓储子域 B（product_receipt / product_sales / return_issue / return_sales / return_vendor / sales_notice）
-- 数据库：PostgreSQL（docker-db_postgres-1 / 127.0.0.1:5433 / db=yudao）
-- 安全：仅 DROP IF EXISTS + CREATE IF NOT EXISTS（幂等）；无 INSERT、无外键约束、无 ENGINE
-- 约定：见同目录 ../_CONVENTIONS.md（tenant_id 全表必加、序列取 @KeySequence 字面值、JSON→text）
-- 运行：psql -1 -v ON_ERROR_STOP=1 -f wm_b.sql
-- ============================================================

-- ===================== mes_wm_product_receipt =====================
-- MES 产品收货（入库）单 DO
DROP TABLE IF EXISTS "mes_wm_product_receipt";
CREATE TABLE IF NOT EXISTS "mes_wm_product_receipt" (
    "id"            bigint      NOT NULL,
    "code"          varchar(64) NULL DEFAULT NULL,
    "name"          varchar(128) NULL DEFAULT NULL,
    "work_order_id" bigint      NULL DEFAULT NULL,
    "item_id"       bigint      NULL DEFAULT NULL,
    "receipt_date"  timestamp   NULL DEFAULT NULL,
    "status"        int4        NULL DEFAULT NULL,
    "remark"        varchar(500) NULL DEFAULT NULL,
    "creator"       varchar(64) DEFAULT '',
    "create_time"   timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"       varchar(64) DEFAULT '',
    "update_time"   timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"       int2        NOT NULL DEFAULT 0,
    "tenant_id"     bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_wm_product_receipt" IS 'MES 产品收货（入库）单 DO';
COMMENT ON COLUMN "mes_wm_product_receipt"."id" IS '编号';
COMMENT ON COLUMN "mes_wm_product_receipt"."code" IS '收货单编码';
COMMENT ON COLUMN "mes_wm_product_receipt"."name" IS '收货单名称';
COMMENT ON COLUMN "mes_wm_product_receipt"."work_order_id" IS '生产工单编号';
COMMENT ON COLUMN "mes_wm_product_receipt"."item_id" IS '产品物料编号';
COMMENT ON COLUMN "mes_wm_product_receipt"."receipt_date" IS '收货日期';
COMMENT ON COLUMN "mes_wm_product_receipt"."status" IS '状态';
COMMENT ON COLUMN "mes_wm_product_receipt"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS mes_wm_product_receipt_seq;

CREATE INDEX IF NOT EXISTS idx_mes_wm_product_receipt_work_order_id ON "mes_wm_product_receipt" ("work_order_id");
CREATE INDEX IF NOT EXISTS idx_mes_wm_product_receipt_item_id ON "mes_wm_product_receipt" ("item_id");

-- ===================== mes_wm_product_receipt_detail =====================
-- MES 产品收货（入库）单明细 DO
DROP TABLE IF EXISTS "mes_wm_product_receipt_detail";
CREATE TABLE IF NOT EXISTS "mes_wm_product_receipt_detail" (
    "id"            bigint      NOT NULL,
    "line_id"       bigint      NULL DEFAULT NULL,
    "receipt_id"    bigint      NULL DEFAULT NULL,
    "item_id"       bigint      NULL DEFAULT NULL,
    "quantity"      numeric(14,2) NULL DEFAULT NULL,
    "batch_id"      bigint      NULL DEFAULT NULL,
    "warehouse_id"  bigint      NULL DEFAULT NULL,
    "location_id"   bigint      NULL DEFAULT NULL,
    "area_id"       bigint      NULL DEFAULT NULL,
    "remark"        varchar(500) NULL DEFAULT NULL,
    "creator"       varchar(64) DEFAULT '',
    "create_time"   timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"       varchar(64) DEFAULT '',
    "update_time"   timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"       int2        NOT NULL DEFAULT 0,
    "tenant_id"     bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_wm_product_receipt_detail" IS 'MES 产品收货（入库）单明细 DO';
COMMENT ON COLUMN "mes_wm_product_receipt_detail"."id" IS '编号';
COMMENT ON COLUMN "mes_wm_product_receipt_detail"."line_id" IS '收货单行编号';
COMMENT ON COLUMN "mes_wm_product_receipt_detail"."receipt_id" IS '收货单编号';
COMMENT ON COLUMN "mes_wm_product_receipt_detail"."item_id" IS '物料编号';
COMMENT ON COLUMN "mes_wm_product_receipt_detail"."quantity" IS '上架数量';
COMMENT ON COLUMN "mes_wm_product_receipt_detail"."batch_id" IS '批次编号';
COMMENT ON COLUMN "mes_wm_product_receipt_detail"."warehouse_id" IS '仓库编号';
COMMENT ON COLUMN "mes_wm_product_receipt_detail"."location_id" IS '库区编号';
COMMENT ON COLUMN "mes_wm_product_receipt_detail"."area_id" IS '库位编号';
COMMENT ON COLUMN "mes_wm_product_receipt_detail"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS mes_wm_product_receipt_detail_seq;

CREATE INDEX IF NOT EXISTS idx_mes_wm_product_receipt_detail_receipt_id ON "mes_wm_product_receipt_detail" ("receipt_id");
CREATE INDEX IF NOT EXISTS idx_mes_wm_product_receipt_detail_line_id ON "mes_wm_product_receipt_detail" ("line_id");
CREATE INDEX IF NOT EXISTS idx_mes_wm_product_receipt_detail_item_id ON "mes_wm_product_receipt_detail" ("item_id");

-- ===================== mes_wm_product_receipt_line =====================
-- MES 产品收货（入库）单行 DO
DROP TABLE IF EXISTS "mes_wm_product_receipt_line";
CREATE TABLE IF NOT EXISTS "mes_wm_product_receipt_line" (
    "id"               bigint      NOT NULL,
    "receipt_id"       bigint      NULL DEFAULT NULL,
    "item_id"          bigint      NULL DEFAULT NULL,
    "material_stock_id" bigint    NULL DEFAULT NULL,
    "quantity"         numeric(14,2) NULL DEFAULT NULL,
    "batch_id"         bigint      NULL DEFAULT NULL,
    "batch_code"       varchar(64) NULL DEFAULT NULL,
    "remark"           varchar(500) NULL DEFAULT NULL,
    "creator"          varchar(64) DEFAULT '',
    "create_time"      timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"          varchar(64) DEFAULT '',
    "update_time"      timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"          int2        NOT NULL DEFAULT 0,
    "tenant_id"        bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_wm_product_receipt_line" IS 'MES 产品收货（入库）单行 DO';
COMMENT ON COLUMN "mes_wm_product_receipt_line"."id" IS '编号';
COMMENT ON COLUMN "mes_wm_product_receipt_line"."receipt_id" IS '收货单编号';
COMMENT ON COLUMN "mes_wm_product_receipt_line"."item_id" IS '物料编号';
COMMENT ON COLUMN "mes_wm_product_receipt_line"."material_stock_id" IS '库存物资记录编号';
COMMENT ON COLUMN "mes_wm_product_receipt_line"."quantity" IS '收货数量';
COMMENT ON COLUMN "mes_wm_product_receipt_line"."batch_id" IS '批次编号';
COMMENT ON COLUMN "mes_wm_product_receipt_line"."batch_code" IS '批次号';
COMMENT ON COLUMN "mes_wm_product_receipt_line"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS mes_wm_product_receipt_line_seq;

CREATE INDEX IF NOT EXISTS idx_mes_wm_product_receipt_line_receipt_id ON "mes_wm_product_receipt_line" ("receipt_id");
CREATE INDEX IF NOT EXISTS idx_mes_wm_product_receipt_line_item_id ON "mes_wm_product_receipt_line" ("item_id");

-- ===================== mes_wm_product_sales =====================
-- MES 销售出库单 DO
DROP TABLE IF EXISTS "mes_wm_product_sales";
CREATE TABLE IF NOT EXISTS "mes_wm_product_sales" (
    "id"                 bigint      NOT NULL,
    "code"               varchar(64) NULL DEFAULT NULL,
    "name"               varchar(128) NULL DEFAULT NULL,
    "client_id"          bigint      NULL DEFAULT NULL,
    "sales_order_code"  varchar(64) NULL DEFAULT NULL,
    "notice_id"         bigint      NULL DEFAULT NULL,
    "sales_date"         timestamp   NULL DEFAULT NULL,
    "contact_name"       varchar(64) NULL DEFAULT NULL,
    "contact_telephone"  varchar(20) NULL DEFAULT NULL,
    "contact_address"    varchar(500) NULL DEFAULT NULL,
    "carrier"            varchar(128) NULL DEFAULT NULL,
    "shipping_number"    varchar(64) NULL DEFAULT NULL,
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

COMMENT ON TABLE "mes_wm_product_sales" IS 'MES 销售出库单 DO';
COMMENT ON COLUMN "mes_wm_product_sales"."id" IS '编号';
COMMENT ON COLUMN "mes_wm_product_sales"."code" IS '出库单号';
COMMENT ON COLUMN "mes_wm_product_sales"."name" IS '出库单名称';
COMMENT ON COLUMN "mes_wm_product_sales"."client_id" IS '客户ID';
COMMENT ON COLUMN "mes_wm_product_sales"."sales_order_code" IS '销售订单号';
COMMENT ON COLUMN "mes_wm_product_sales"."notice_id" IS '发货通知单 ID';
COMMENT ON COLUMN "mes_wm_product_sales"."sales_date" IS '出库日期';
COMMENT ON COLUMN "mes_wm_product_sales"."contact_name" IS '联系人';
COMMENT ON COLUMN "mes_wm_product_sales"."contact_telephone" IS '联系电话';
COMMENT ON COLUMN "mes_wm_product_sales"."contact_address" IS '收货地址';
COMMENT ON COLUMN "mes_wm_product_sales"."carrier" IS '承运商';
COMMENT ON COLUMN "mes_wm_product_sales"."shipping_number" IS '运输单号';
COMMENT ON COLUMN "mes_wm_product_sales"."status" IS '状态';
COMMENT ON COLUMN "mes_wm_product_sales"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS mes_wm_product_sales_seq;

CREATE INDEX IF NOT EXISTS idx_mes_wm_product_sales_client_id ON "mes_wm_product_sales" ("client_id");
CREATE INDEX IF NOT EXISTS idx_mes_wm_product_sales_notice_id ON "mes_wm_product_sales" ("notice_id");

-- ===================== mes_wm_return_issue =====================
-- MES 生产退料单 DO
DROP TABLE IF EXISTS "mes_wm_return_issue";
CREATE TABLE IF NOT EXISTS "mes_wm_return_issue" (
    "id"             bigint      NOT NULL,
    "code"           varchar(64) NULL DEFAULT NULL,
    "name"           varchar(128) NULL DEFAULT NULL,
    "work_order_id"  bigint      NULL DEFAULT NULL,
    "workstation_id" bigint      NULL DEFAULT NULL,
    "type"           int4        NULL DEFAULT NULL,
    "return_date"    timestamp   NULL DEFAULT NULL,
    "status"         int4        NULL DEFAULT NULL,
    "remark"         varchar(500) NULL DEFAULT NULL,
    "creator"        varchar(64) DEFAULT '',
    "create_time"    timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"        varchar(64) DEFAULT '',
    "update_time"    timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"        int2        NOT NULL DEFAULT 0,
    "tenant_id"      bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_wm_return_issue" IS 'MES 生产退料单 DO';
COMMENT ON COLUMN "mes_wm_return_issue"."id" IS '编号';
COMMENT ON COLUMN "mes_wm_return_issue"."code" IS '退料单编号';
COMMENT ON COLUMN "mes_wm_return_issue"."name" IS '退料单名称';
COMMENT ON COLUMN "mes_wm_return_issue"."work_order_id" IS '生产工单 ID';
COMMENT ON COLUMN "mes_wm_return_issue"."workstation_id" IS '工作站 ID';
COMMENT ON COLUMN "mes_wm_return_issue"."type" IS '退料类型';
COMMENT ON COLUMN "mes_wm_return_issue"."return_date" IS '退料日期';
COMMENT ON COLUMN "mes_wm_return_issue"."status" IS '状态';
COMMENT ON COLUMN "mes_wm_return_issue"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS mes_wm_return_issue_seq;

CREATE INDEX IF NOT EXISTS idx_mes_wm_return_issue_work_order_id ON "mes_wm_return_issue" ("work_order_id");
CREATE INDEX IF NOT EXISTS idx_mes_wm_return_issue_workstation_id ON "mes_wm_return_issue" ("workstation_id");

-- ===================== mes_wm_return_issue_detail =====================
-- MES 生产退料明细 DO
DROP TABLE IF EXISTS "mes_wm_return_issue_detail";
CREATE TABLE IF NOT EXISTS "mes_wm_return_issue_detail" (
    "id"               bigint      NOT NULL,
    "issue_id"         bigint      NULL DEFAULT NULL,
    "line_id"          bigint      NULL DEFAULT NULL,
    "material_stock_id" bigint    NULL DEFAULT NULL,
    "item_id"          bigint      NULL DEFAULT NULL,
    "quantity"         numeric(14,2) NULL DEFAULT NULL,
    "batch_id"         bigint      NULL DEFAULT NULL,
    "batch_code"       varchar(64) NULL DEFAULT NULL,
    "warehouse_id"     bigint      NULL DEFAULT NULL,
    "location_id"      bigint      NULL DEFAULT NULL,
    "area_id"          bigint      NULL DEFAULT NULL,
    "remark"           varchar(500) NULL DEFAULT NULL,
    "creator"          varchar(64) DEFAULT '',
    "create_time"      timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"          varchar(64) DEFAULT '',
    "update_time"      timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"          int2        NOT NULL DEFAULT 0,
    "tenant_id"        bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_wm_return_issue_detail" IS 'MES 生产退料明细 DO';
COMMENT ON COLUMN "mes_wm_return_issue_detail"."id" IS '编号';
COMMENT ON COLUMN "mes_wm_return_issue_detail"."issue_id" IS '退料单 ID';
COMMENT ON COLUMN "mes_wm_return_issue_detail"."line_id" IS '行 ID';
COMMENT ON COLUMN "mes_wm_return_issue_detail"."material_stock_id" IS '库存记录 ID';
COMMENT ON COLUMN "mes_wm_return_issue_detail"."item_id" IS '物料 ID';
COMMENT ON COLUMN "mes_wm_return_issue_detail"."quantity" IS '退料数量';
COMMENT ON COLUMN "mes_wm_return_issue_detail"."batch_id" IS '批次 ID';
COMMENT ON COLUMN "mes_wm_return_issue_detail"."batch_code" IS '批次号';
COMMENT ON COLUMN "mes_wm_return_issue_detail"."warehouse_id" IS '仓库 ID';
COMMENT ON COLUMN "mes_wm_return_issue_detail"."location_id" IS '库区 ID';
COMMENT ON COLUMN "mes_wm_return_issue_detail"."area_id" IS '库位 ID';
COMMENT ON COLUMN "mes_wm_return_issue_detail"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS mes_wm_return_issue_detail_seq;

CREATE INDEX IF NOT EXISTS idx_mes_wm_return_issue_detail_issue_id ON "mes_wm_return_issue_detail" ("issue_id");
CREATE INDEX IF NOT EXISTS idx_mes_wm_return_issue_detail_line_id ON "mes_wm_return_issue_detail" ("line_id");
CREATE INDEX IF NOT EXISTS idx_mes_wm_return_issue_detail_item_id ON "mes_wm_return_issue_detail" ("item_id");

-- ===================== mes_wm_return_sales =====================
-- MES 销售退货单 DO
DROP TABLE IF EXISTS "mes_wm_return_sales";
CREATE TABLE IF NOT EXISTS "mes_wm_return_sales" (
    "id"               bigint      NOT NULL,
    "code"             varchar(64) NULL DEFAULT NULL,
    "name"             varchar(128) NULL DEFAULT NULL,
    "sales_order_code" varchar(64) NULL DEFAULT NULL,
    "client_id"        bigint      NULL DEFAULT NULL,
    "return_date"      timestamp   NULL DEFAULT NULL,
    "return_reason"    varchar(500) NULL DEFAULT NULL,
    "status"           int4        NULL DEFAULT NULL,
    "remark"           varchar(500) NULL DEFAULT NULL,
    "creator"          varchar(64) DEFAULT '',
    "create_time"      timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"          varchar(64) DEFAULT '',
    "update_time"      timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"          int2        NOT NULL DEFAULT 0,
    "tenant_id"        bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_wm_return_sales" IS 'MES 销售退货单 DO';
COMMENT ON COLUMN "mes_wm_return_sales"."id" IS '编号';
COMMENT ON COLUMN "mes_wm_return_sales"."code" IS '退货单编号';
COMMENT ON COLUMN "mes_wm_return_sales"."name" IS '退货单名称';
COMMENT ON COLUMN "mes_wm_return_sales"."sales_order_code" IS '销售订单编号';
COMMENT ON COLUMN "mes_wm_return_sales"."client_id" IS '客户 ID';
COMMENT ON COLUMN "mes_wm_return_sales"."return_date" IS '退货日期';
COMMENT ON COLUMN "mes_wm_return_sales"."return_reason" IS '退货原因';
COMMENT ON COLUMN "mes_wm_return_sales"."status" IS '状态';
COMMENT ON COLUMN "mes_wm_return_sales"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS mes_wm_return_sales_seq;

CREATE INDEX IF NOT EXISTS idx_mes_wm_return_sales_client_id ON "mes_wm_return_sales" ("client_id");

-- ===================== mes_wm_return_sales_detail =====================
-- MES 销售退货明细 DO
DROP TABLE IF EXISTS "mes_wm_return_sales_detail";
CREATE TABLE IF NOT EXISTS "mes_wm_return_sales_detail" (
    "id"           bigint      NOT NULL,
    "return_id"    bigint      NULL DEFAULT NULL,
    "line_id"      bigint      NULL DEFAULT NULL,
    "item_id"      bigint      NULL DEFAULT NULL,
    "quantity"     numeric(14,2) NULL DEFAULT NULL,
    "batch_id"     bigint      NULL DEFAULT NULL,
    "batch_code"   varchar(64) NULL DEFAULT NULL,
    "warehouse_id" bigint      NULL DEFAULT NULL,
    "location_id"  bigint      NULL DEFAULT NULL,
    "area_id"      bigint      NULL DEFAULT NULL,
    "remark"       varchar(500) NULL DEFAULT NULL,
    "creator"      varchar(64) DEFAULT '',
    "create_time"  timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"      varchar(64) DEFAULT '',
    "update_time"  timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"      int2        NOT NULL DEFAULT 0,
    "tenant_id"    bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_wm_return_sales_detail" IS 'MES 销售退货明细 DO';
COMMENT ON COLUMN "mes_wm_return_sales_detail"."id" IS '编号';
COMMENT ON COLUMN "mes_wm_return_sales_detail"."return_id" IS '退货单 ID';
COMMENT ON COLUMN "mes_wm_return_sales_detail"."line_id" IS '行 ID';
COMMENT ON COLUMN "mes_wm_return_sales_detail"."item_id" IS '物料 ID';
COMMENT ON COLUMN "mes_wm_return_sales_detail"."quantity" IS '数量';
COMMENT ON COLUMN "mes_wm_return_sales_detail"."batch_id" IS '批次 ID';
COMMENT ON COLUMN "mes_wm_return_sales_detail"."batch_code" IS '批次号';
COMMENT ON COLUMN "mes_wm_return_sales_detail"."warehouse_id" IS '仓库 ID';
COMMENT ON COLUMN "mes_wm_return_sales_detail"."location_id" IS '库区 ID';
COMMENT ON COLUMN "mes_wm_return_sales_detail"."area_id" IS '库位 ID';
COMMENT ON COLUMN "mes_wm_return_sales_detail"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS mes_wm_return_sales_detail_seq;

CREATE INDEX IF NOT EXISTS idx_mes_wm_return_sales_detail_return_id ON "mes_wm_return_sales_detail" ("return_id");
CREATE INDEX IF NOT EXISTS idx_mes_wm_return_sales_detail_line_id ON "mes_wm_return_sales_detail" ("line_id");
CREATE INDEX IF NOT EXISTS idx_mes_wm_return_sales_detail_item_id ON "mes_wm_return_sales_detail" ("item_id");

-- ===================== mes_wm_return_vendor =====================
-- MES 供应商退货单 DO
DROP TABLE IF EXISTS "mes_wm_return_vendor";
CREATE TABLE IF NOT EXISTS "mes_wm_return_vendor" (
    "id"                  bigint      NOT NULL,
    "code"                varchar(64) NULL DEFAULT NULL,
    "name"                varchar(128) NULL DEFAULT NULL,
    "purchase_order_code" varchar(64) NULL DEFAULT NULL,
    "vendor_id"           bigint      NULL DEFAULT NULL,
    "return_date"         timestamp   NULL DEFAULT NULL,
    "return_reason"       varchar(500) NULL DEFAULT NULL,
    "transport_code"      varchar(64) NULL DEFAULT NULL,
    "transport_telephone" varchar(20) NULL DEFAULT NULL,
    "status"              int4        NULL DEFAULT NULL,
    "remark"              varchar(500) NULL DEFAULT NULL,
    "creator"             varchar(64) DEFAULT '',
    "create_time"         timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"             varchar(64) DEFAULT '',
    "update_time"         timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"             int2        NOT NULL DEFAULT 0,
    "tenant_id"           bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_wm_return_vendor" IS 'MES 供应商退货单 DO';
COMMENT ON COLUMN "mes_wm_return_vendor"."id" IS '编号';
COMMENT ON COLUMN "mes_wm_return_vendor"."code" IS '退货单编号';
COMMENT ON COLUMN "mes_wm_return_vendor"."name" IS '退货单名称';
COMMENT ON COLUMN "mes_wm_return_vendor"."purchase_order_code" IS '采购订单编号';
COMMENT ON COLUMN "mes_wm_return_vendor"."vendor_id" IS '供应商 ID';
COMMENT ON COLUMN "mes_wm_return_vendor"."return_date" IS '退货日期';
COMMENT ON COLUMN "mes_wm_return_vendor"."return_reason" IS '退货原因';
COMMENT ON COLUMN "mes_wm_return_vendor"."transport_code" IS '物流单号';
COMMENT ON COLUMN "mes_wm_return_vendor"."transport_telephone" IS '物流联系电话';
COMMENT ON COLUMN "mes_wm_return_vendor"."status" IS '状态';
COMMENT ON COLUMN "mes_wm_return_vendor"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS mes_wm_return_vendor_seq;

CREATE INDEX IF NOT EXISTS idx_mes_wm_return_vendor_vendor_id ON "mes_wm_return_vendor" ("vendor_id");

-- ===================== mes_wm_return_vendor_detail =====================
-- MES 供应商退货明细 DO
DROP TABLE IF EXISTS "mes_wm_return_vendor_detail";
CREATE TABLE IF NOT EXISTS "mes_wm_return_vendor_detail" (
    "id"               bigint      NOT NULL,
    "return_id"        bigint      NULL DEFAULT NULL,
    "line_id"          bigint      NULL DEFAULT NULL,
    "material_stock_id" bigint    NULL DEFAULT NULL,
    "item_id"          bigint      NULL DEFAULT NULL,
    "quantity"         numeric(14,2) NULL DEFAULT NULL,
    "batch_id"         bigint      NULL DEFAULT NULL,
    "batch_code"       varchar(64) NULL DEFAULT NULL,
    "warehouse_id"     bigint      NULL DEFAULT NULL,
    "location_id"      bigint      NULL DEFAULT NULL,
    "area_id"          bigint      NULL DEFAULT NULL,
    "remark"           varchar(500) NULL DEFAULT NULL,
    "creator"          varchar(64) DEFAULT '',
    "create_time"      timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"          varchar(64) DEFAULT '',
    "update_time"      timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"          int2        NOT NULL DEFAULT 0,
    "tenant_id"        bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_wm_return_vendor_detail" IS 'MES 供应商退货明细 DO';
COMMENT ON COLUMN "mes_wm_return_vendor_detail"."id" IS '编号';
COMMENT ON COLUMN "mes_wm_return_vendor_detail"."return_id" IS '退货单 ID';
COMMENT ON COLUMN "mes_wm_return_vendor_detail"."line_id" IS '行 ID';
COMMENT ON COLUMN "mes_wm_return_vendor_detail"."material_stock_id" IS '库存记录 ID';
COMMENT ON COLUMN "mes_wm_return_vendor_detail"."item_id" IS '物料 ID';
COMMENT ON COLUMN "mes_wm_return_vendor_detail"."quantity" IS '退货数量';
COMMENT ON COLUMN "mes_wm_return_vendor_detail"."batch_id" IS '批次 ID';
COMMENT ON COLUMN "mes_wm_return_vendor_detail"."batch_code" IS '批次号';
COMMENT ON COLUMN "mes_wm_return_vendor_detail"."warehouse_id" IS '仓库 ID';
COMMENT ON COLUMN "mes_wm_return_vendor_detail"."location_id" IS '库区 ID';
COMMENT ON COLUMN "mes_wm_return_vendor_detail"."area_id" IS '库位 ID';
COMMENT ON COLUMN "mes_wm_return_vendor_detail"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS mes_wm_return_vendor_detail_seq;

CREATE INDEX IF NOT EXISTS idx_mes_wm_return_vendor_detail_return_id ON "mes_wm_return_vendor_detail" ("return_id");
CREATE INDEX IF NOT EXISTS idx_mes_wm_return_vendor_detail_line_id ON "mes_wm_return_vendor_detail" ("line_id");
CREATE INDEX IF NOT EXISTS idx_mes_wm_return_vendor_detail_item_id ON "mes_wm_return_vendor_detail" ("item_id");

-- ===================== mes_wm_return_vendor_line =====================
-- MES 供应商退货单行 DO
DROP TABLE IF EXISTS "mes_wm_return_vendor_line";
CREATE TABLE IF NOT EXISTS "mes_wm_return_vendor_line" (
    "id"         bigint      NOT NULL,
    "return_id"  bigint      NULL DEFAULT NULL,
    "item_id"    bigint      NULL DEFAULT NULL,
    "quantity"   numeric(14,2) NULL DEFAULT NULL,
    "batch_id"   bigint      NULL DEFAULT NULL,
    "batch_code" varchar(64) NULL DEFAULT NULL,
    "remark"     varchar(500) NULL DEFAULT NULL,
    "creator"    varchar(64) DEFAULT '',
    "create_time" timestamp  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"    varchar(64) DEFAULT '',
    "update_time" timestamp  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"    int2        NOT NULL DEFAULT 0,
    "tenant_id"  bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_wm_return_vendor_line" IS 'MES 供应商退货单行 DO';
COMMENT ON COLUMN "mes_wm_return_vendor_line"."id" IS '编号';
COMMENT ON COLUMN "mes_wm_return_vendor_line"."return_id" IS '退货单 ID';
COMMENT ON COLUMN "mes_wm_return_vendor_line"."item_id" IS '物料 ID';
COMMENT ON COLUMN "mes_wm_return_vendor_line"."quantity" IS '退货数量';
COMMENT ON COLUMN "mes_wm_return_vendor_line"."batch_id" IS '批次 ID';
COMMENT ON COLUMN "mes_wm_return_vendor_line"."batch_code" IS '批次号';
COMMENT ON COLUMN "mes_wm_return_vendor_line"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS mes_wm_return_vendor_line_seq;

CREATE INDEX IF NOT EXISTS idx_mes_wm_return_vendor_line_return_id ON "mes_wm_return_vendor_line" ("return_id");
CREATE INDEX IF NOT EXISTS idx_mes_wm_return_vendor_line_item_id ON "mes_wm_return_vendor_line" ("item_id");

-- ===================== mes_wm_sales_notice =====================
-- MES 发货通知单 DO
DROP TABLE IF EXISTS "mes_wm_sales_notice";
CREATE TABLE IF NOT EXISTS "mes_wm_sales_notice" (
    "id"                  bigint      NOT NULL,
    "code"                varchar(64) NULL DEFAULT NULL,
    "name"                varchar(128) NULL DEFAULT NULL,
    "sales_order_code"    varchar(64) NULL DEFAULT NULL,
    "client_id"           bigint      NULL DEFAULT NULL,
    "sales_date"          timestamp   NULL DEFAULT NULL,
    "recipient_name"      varchar(64) NULL DEFAULT NULL,
    "recipient_telephone" varchar(20) NULL DEFAULT NULL,
    "recipient_address"   varchar(500) NULL DEFAULT NULL,
    "status"              int4        NULL DEFAULT NULL,
    "remark"              varchar(500) NULL DEFAULT NULL,
    "creator"             varchar(64) DEFAULT '',
    "create_time"         timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"             varchar(64) DEFAULT '',
    "update_time"         timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"             int2        NOT NULL DEFAULT 0,
    "tenant_id"           bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_wm_sales_notice" IS 'MES 发货通知单 DO';
COMMENT ON COLUMN "mes_wm_sales_notice"."id" IS '编号';
COMMENT ON COLUMN "mes_wm_sales_notice"."code" IS '通知单编码';
COMMENT ON COLUMN "mes_wm_sales_notice"."name" IS '通知单名称';
COMMENT ON COLUMN "mes_wm_sales_notice"."sales_order_code" IS '销售订单编号';
COMMENT ON COLUMN "mes_wm_sales_notice"."client_id" IS '客户编号';
COMMENT ON COLUMN "mes_wm_sales_notice"."sales_date" IS '发货日期';
COMMENT ON COLUMN "mes_wm_sales_notice"."recipient_name" IS '收货人';
COMMENT ON COLUMN "mes_wm_sales_notice"."recipient_telephone" IS '联系方式';
COMMENT ON COLUMN "mes_wm_sales_notice"."recipient_address" IS '收货地址';
COMMENT ON COLUMN "mes_wm_sales_notice"."status" IS '状态';
COMMENT ON COLUMN "mes_wm_sales_notice"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS mes_wm_sales_notice_seq;

CREATE INDEX IF NOT EXISTS idx_mes_wm_sales_notice_client_id ON "mes_wm_sales_notice" ("client_id");

-- ===================== mes_wm_sales_notice_line =====================
-- MES 发货通知单行 DO
DROP TABLE IF EXISTS "mes_wm_sales_notice_line";
CREATE TABLE IF NOT EXISTS "mes_wm_sales_notice_line" (
    "id"              bigint      NOT NULL,
    "notice_id"       bigint      NULL DEFAULT NULL,
    "item_id"         bigint      NULL DEFAULT NULL,
    "batch_id"        bigint      NULL DEFAULT NULL,
    "batch_code"      varchar(64) NULL DEFAULT NULL,
    "quantity"        numeric(14,2) NULL DEFAULT NULL,
    "oqc_check_flag"  int2        NOT NULL DEFAULT 0,
    "remark"          varchar(500) NULL DEFAULT NULL,
    "creator"         varchar(64) DEFAULT '',
    "create_time"     timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"         varchar(64) DEFAULT '',
    "update_time"     timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"         int2        NOT NULL DEFAULT 0,
    "tenant_id"       bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_wm_sales_notice_line" IS 'MES 发货通知单行 DO';
COMMENT ON COLUMN "mes_wm_sales_notice_line"."id" IS '编号';
COMMENT ON COLUMN "mes_wm_sales_notice_line"."notice_id" IS '发货通知单编号';
COMMENT ON COLUMN "mes_wm_sales_notice_line"."item_id" IS '物料编号';
COMMENT ON COLUMN "mes_wm_sales_notice_line"."batch_id" IS '批次编号';
COMMENT ON COLUMN "mes_wm_sales_notice_line"."batch_code" IS '批次号';
COMMENT ON COLUMN "mes_wm_sales_notice_line"."quantity" IS '发货数量';
COMMENT ON COLUMN "mes_wm_sales_notice_line"."oqc_check_flag" IS '是否检验';
COMMENT ON COLUMN "mes_wm_sales_notice_line"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS mes_wm_sales_notice_line_seq;

CREATE INDEX IF NOT EXISTS idx_mes_wm_sales_notice_line_notice_id ON "mes_wm_sales_notice_line" ("notice_id");
CREATE INDEX IF NOT EXISTS idx_mes_wm_sales_notice_line_item_id ON "mes_wm_sales_notice_line" ("item_id");


-- ============================================================
-- MES 模块表 DDL（PostgreSQL）— 从本地 DO 反推
-- ============================================================
-- 来源：从 yudao-module-mes 的 *DO.java 反推生成（非社区 MySQL 脚本转换）
-- 子域：wm 仓储子域 C（sn 序列号 / stock_taking 盘点 / transaction 事务 / transfer 转移 / warehouse 仓库主数据）
-- 数据库：PostgreSQL（docker-db_postgres-1 / 127.0.0.1:5433 / db=yudao）
-- 安全：仅 DROP IF EXISTS + CREATE IF NOT EXISTS（幂等）；无 INSERT、无外键约束、无 ENGINE
-- 约定：见同目录 ../_CONVENTIONS.md（tenant_id 全表必加、序列取 @KeySequence 字面值、JSON→text）
-- 运行：psql -1 -v ON_ERROR_STOP=1 -f wm_c.sql
-- ============================================================

-- ===================== mes_wm_sn =====================
-- MES SN 码 DO
DROP TABLE IF EXISTS "mes_wm_sn";
CREATE TABLE IF NOT EXISTS "mes_wm_sn" (
    "id"            int8      NOT NULL,
    "uuid"          varchar(64),
    "code"          varchar(64),
    "item_id"       int8,
    "batch_code"    varchar(64),
    "work_order_id" int8,
    "creator"       varchar(64) DEFAULT '',
    "create_time"   timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"       varchar(64) DEFAULT '',
    "update_time"   timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"       int2        NOT NULL DEFAULT 0,
    "tenant_id"     int8        NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_wm_sn" IS 'MES SN 码 DO';
COMMENT ON COLUMN "mes_wm_sn"."id" IS '编号';
COMMENT ON COLUMN "mes_wm_sn"."uuid" IS '批次 UUID（用于标记同一批次生成的 SN 码）';
COMMENT ON COLUMN "mes_wm_sn"."code" IS 'SN 码（唯一）';
COMMENT ON COLUMN "mes_wm_sn"."item_id" IS '物料编号';
COMMENT ON COLUMN "mes_wm_sn"."batch_code" IS '批次号';
COMMENT ON COLUMN "mes_wm_sn"."work_order_id" IS '生产工单编号';

CREATE SEQUENCE IF NOT EXISTS "mes_wm_sn_seq";

CREATE UNIQUE INDEX IF NOT EXISTS "idx_mes_wm_sn_code" ON "mes_wm_sn" ("code", tenant_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS "idx_mes_wm_sn_item_id" ON "mes_wm_sn" ("item_id");

-- ===================== mes_wm_stock_taking_plan =====================
-- MES 盘点方案 DO
DROP TABLE IF EXISTS "mes_wm_stock_taking_plan";
CREATE TABLE IF NOT EXISTS "mes_wm_stock_taking_plan" (
    "id"          int8      NOT NULL,
    "code"        varchar(32),
    "name"        varchar(128),
    "type"        int4,
    "start_time"  timestamp,
    "end_time"    timestamp,
    "blind_flag"  int2       NOT NULL DEFAULT 0,
    "frozen"      int2       NOT NULL DEFAULT 0,
    "status"      int4,
    "remark"      varchar(500),
    "creator"       varchar(64) DEFAULT '',
    "create_time"   timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"       varchar(64) DEFAULT '',
    "update_time"   timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"       int2        NOT NULL DEFAULT 0,
    "tenant_id"     int8        NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_wm_stock_taking_plan" IS 'MES 盘点方案 DO';
COMMENT ON COLUMN "mes_wm_stock_taking_plan"."id" IS '编号';
COMMENT ON COLUMN "mes_wm_stock_taking_plan"."code" IS '方案编码';
COMMENT ON COLUMN "mes_wm_stock_taking_plan"."name" IS '方案名称';
COMMENT ON COLUMN "mes_wm_stock_taking_plan"."type" IS '盘点类型';
COMMENT ON COLUMN "mes_wm_stock_taking_plan"."start_time" IS '计划开始时间';
COMMENT ON COLUMN "mes_wm_stock_taking_plan"."end_time" IS '计划结束时间';
COMMENT ON COLUMN "mes_wm_stock_taking_plan"."blind_flag" IS '是否盲盘';
COMMENT ON COLUMN "mes_wm_stock_taking_plan"."frozen" IS '是否冻结库存';
COMMENT ON COLUMN "mes_wm_stock_taking_plan"."status" IS '状态';
COMMENT ON COLUMN "mes_wm_stock_taking_plan"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS "mes_wm_stock_taking_plan_seq";

CREATE UNIQUE INDEX IF NOT EXISTS "idx_mes_wm_stock_taking_plan_code" ON "mes_wm_stock_taking_plan" ("code", tenant_id) WHERE deleted = 0;

-- ===================== mes_wm_stock_taking_plan_param =====================
-- MES 盘点方案参数 DO
DROP TABLE IF EXISTS "mes_wm_stock_taking_plan_param";
CREATE TABLE IF NOT EXISTS "mes_wm_stock_taking_plan_param" (
    "id"          int8      NOT NULL,
    "plan_id"     int8,
    "type"        int4,
    "value_id"    int8,
    "value_code"  varchar(32),
    "value_name"  varchar(128),
    "remark"      varchar(500),
    "creator"       varchar(64) DEFAULT '',
    "create_time"   timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"       varchar(64) DEFAULT '',
    "update_time"   timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"       int2        NOT NULL DEFAULT 0,
    "tenant_id"     int8        NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_wm_stock_taking_plan_param" IS 'MES 盘点方案参数 DO';
COMMENT ON COLUMN "mes_wm_stock_taking_plan_param"."id" IS '编号';
COMMENT ON COLUMN "mes_wm_stock_taking_plan_param"."plan_id" IS '盘点方案编号';
COMMENT ON COLUMN "mes_wm_stock_taking_plan_param"."type" IS '参数值类型';
COMMENT ON COLUMN "mes_wm_stock_taking_plan_param"."value_id" IS '参数值编号';
COMMENT ON COLUMN "mes_wm_stock_taking_plan_param"."value_code" IS '参数值编码';
COMMENT ON COLUMN "mes_wm_stock_taking_plan_param"."value_name" IS '参数值名称';
COMMENT ON COLUMN "mes_wm_stock_taking_plan_param"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS "mes_wm_stock_taking_plan_param_seq";

CREATE INDEX IF NOT EXISTS "idx_mes_wm_stock_taking_plan_param_plan_id" ON "mes_wm_stock_taking_plan_param" ("plan_id");

-- ===================== mes_wm_stock_taking_task =====================
-- MES 盘点任务 DO
DROP TABLE IF EXISTS "mes_wm_stock_taking_task";
CREATE TABLE IF NOT EXISTS "mes_wm_stock_taking_task" (
    "id"           int8      NOT NULL,
    "code"         varchar(32),
    "name"         varchar(128),
    "taking_date"  timestamp,
    "type"         int4,
    "user_id"      int8,
    "plan_id"      int8,
    "blind_flag"   int2       NOT NULL DEFAULT 0,
    "frozen"       int2       NOT NULL DEFAULT 0,
    "start_time"   timestamp,
    "end_time"     timestamp,
    "status"       int4,
    "remark"       varchar(500),
    "creator"       varchar(64) DEFAULT '',
    "create_time"   timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"       varchar(64) DEFAULT '',
    "update_time"   timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"       int2        NOT NULL DEFAULT 0,
    "tenant_id"     int8        NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_wm_stock_taking_task" IS 'MES 盘点任务 DO';
COMMENT ON COLUMN "mes_wm_stock_taking_task"."id" IS '编号';
COMMENT ON COLUMN "mes_wm_stock_taking_task"."code" IS '任务编码';
COMMENT ON COLUMN "mes_wm_stock_taking_task"."name" IS '任务名称';
COMMENT ON COLUMN "mes_wm_stock_taking_task"."taking_date" IS '盘点日期';
COMMENT ON COLUMN "mes_wm_stock_taking_task"."type" IS '盘点类型';
COMMENT ON COLUMN "mes_wm_stock_taking_task"."user_id" IS '盘点人编号';
COMMENT ON COLUMN "mes_wm_stock_taking_task"."plan_id" IS '盘点计划编号';
COMMENT ON COLUMN "mes_wm_stock_taking_task"."blind_flag" IS '是否盲盘';
COMMENT ON COLUMN "mes_wm_stock_taking_task"."frozen" IS '是否冻结库存';
COMMENT ON COLUMN "mes_wm_stock_taking_task"."start_time" IS '开始时间';
COMMENT ON COLUMN "mes_wm_stock_taking_task"."end_time" IS '结束时间';
COMMENT ON COLUMN "mes_wm_stock_taking_task"."status" IS '任务状态';
COMMENT ON COLUMN "mes_wm_stock_taking_task"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS "mes_wm_stock_taking_task_seq";

CREATE UNIQUE INDEX IF NOT EXISTS "idx_mes_wm_stock_taking_task_code" ON "mes_wm_stock_taking_task" ("code", tenant_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS "idx_mes_wm_stock_taking_task_plan_id" ON "mes_wm_stock_taking_task" ("plan_id");

-- ===================== mes_wm_stock_taking_task_line =====================
-- MES 盘点任务行 DO
DROP TABLE IF EXISTS "mes_wm_stock_taking_task_line";
CREATE TABLE IF NOT EXISTS "mes_wm_stock_taking_task_line" (
    "id"                int8      NOT NULL,
    "task_id"           int8,
    "material_stock_id" int8,
    "item_id"           int8,
    "batch_id"          int8,
    "batch_code"        varchar(64),
    "quantity"          numeric(14,2),
    "taking_quantity"   numeric(14,2),
    "warehouse_id"      int8,
    "location_id"       int8,
    "area_id"           int8,
    "status"            int4,
    "remark"            varchar(500),
    "creator"       varchar(64) DEFAULT '',
    "create_time"   timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"       varchar(64) DEFAULT '',
    "update_time"   timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"       int2        NOT NULL DEFAULT 0,
    "tenant_id"     int8        NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_wm_stock_taking_task_line" IS 'MES 盘点任务行 DO';
COMMENT ON COLUMN "mes_wm_stock_taking_task_line"."id" IS '编号';
COMMENT ON COLUMN "mes_wm_stock_taking_task_line"."task_id" IS '盘点任务编号';
COMMENT ON COLUMN "mes_wm_stock_taking_task_line"."material_stock_id" IS '库存编号';
COMMENT ON COLUMN "mes_wm_stock_taking_task_line"."item_id" IS '物料编号';
COMMENT ON COLUMN "mes_wm_stock_taking_task_line"."batch_id" IS '批次编号';
COMMENT ON COLUMN "mes_wm_stock_taking_task_line"."batch_code" IS '批次编码';
COMMENT ON COLUMN "mes_wm_stock_taking_task_line"."quantity" IS '在库数量';
COMMENT ON COLUMN "mes_wm_stock_taking_task_line"."taking_quantity" IS '盘点数量';
COMMENT ON COLUMN "mes_wm_stock_taking_task_line"."warehouse_id" IS '仓库编号';
COMMENT ON COLUMN "mes_wm_stock_taking_task_line"."location_id" IS '库位编号';
COMMENT ON COLUMN "mes_wm_stock_taking_task_line"."area_id" IS '库区编号';
COMMENT ON COLUMN "mes_wm_stock_taking_task_line"."status" IS '盘点状态';
COMMENT ON COLUMN "mes_wm_stock_taking_task_line"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS "mes_wm_stock_taking_task_line_seq";

CREATE INDEX IF NOT EXISTS "idx_mes_wm_stock_taking_task_line_task_id" ON "mes_wm_stock_taking_task_line" ("task_id");
CREATE INDEX IF NOT EXISTS "idx_mes_wm_stock_taking_task_line_warehouse_id" ON "mes_wm_stock_taking_task_line" ("warehouse_id");

-- ===================== mes_wm_stock_taking_task_result =====================
-- MES 盘点结果 DO
DROP TABLE IF EXISTS "mes_wm_stock_taking_task_result";
CREATE TABLE IF NOT EXISTS "mes_wm_stock_taking_task_result" (
    "id"                int8      NOT NULL,
    "task_id"           int8,
    "line_id"           int8,
    "material_stock_id" int8,
    "item_id"           int8,
    "batch_id"          int8,
    "batch_code"        varchar(64),
    "warehouse_id"      int8,
    "location_id"       int8,
    "area_id"           int8,
    "quantity"          numeric(14,2),
    "taking_quantity"   numeric(14,2),
    "remark"            varchar(500),
    "creator"       varchar(64) DEFAULT '',
    "create_time"   timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"       varchar(64) DEFAULT '',
    "update_time"   timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"       int2        NOT NULL DEFAULT 0,
    "tenant_id"     int8        NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_wm_stock_taking_task_result" IS 'MES 盘点结果 DO';
COMMENT ON COLUMN "mes_wm_stock_taking_task_result"."id" IS '编号';
COMMENT ON COLUMN "mes_wm_stock_taking_task_result"."task_id" IS '盘点任务编号';
COMMENT ON COLUMN "mes_wm_stock_taking_task_result"."line_id" IS '盘点任务行编号';
COMMENT ON COLUMN "mes_wm_stock_taking_task_result"."material_stock_id" IS '库存编号';
COMMENT ON COLUMN "mes_wm_stock_taking_task_result"."item_id" IS '物料编号';
COMMENT ON COLUMN "mes_wm_stock_taking_task_result"."batch_id" IS '批次编号';
COMMENT ON COLUMN "mes_wm_stock_taking_task_result"."batch_code" IS '批次编码';
COMMENT ON COLUMN "mes_wm_stock_taking_task_result"."warehouse_id" IS '仓库编号';
COMMENT ON COLUMN "mes_wm_stock_taking_task_result"."location_id" IS '库位编号';
COMMENT ON COLUMN "mes_wm_stock_taking_task_result"."area_id" IS '库区编号';
COMMENT ON COLUMN "mes_wm_stock_taking_task_result"."quantity" IS '在库数量';
COMMENT ON COLUMN "mes_wm_stock_taking_task_result"."taking_quantity" IS '盘点数量';
COMMENT ON COLUMN "mes_wm_stock_taking_task_result"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS "mes_wm_stock_taking_task_result_seq";

CREATE INDEX IF NOT EXISTS "idx_mes_wm_stock_taking_task_result_task_id" ON "mes_wm_stock_taking_task_result" ("task_id");
CREATE INDEX IF NOT EXISTS "idx_mes_wm_stock_taking_task_result_line_id" ON "mes_wm_stock_taking_task_result" ("line_id");

-- ===================== mes_wm_transaction =====================
-- MES 库存事务流水 DO
DROP TABLE IF EXISTS "mes_wm_transaction";
CREATE TABLE IF NOT EXISTS "mes_wm_transaction" (
    "id"                     int8      NOT NULL,
    "type"                   int4,
    "biz_type"               int4,
    "biz_id"                 int8,
    "biz_code"               varchar(64),
    "biz_line_id"            int8,
    "material_stock_id"      int8,
    "related_transaction_id" int8,
    "item_id"                int8,
    "quantity"               numeric(14,2),
    "batch_id"               int8,
    "batch_code"             varchar(64),
    "warehouse_id"           int8,
    "location_id"            int8,
    "area_id"                int8,
    "transaction_time"       timestamp,
    "erp_time"               timestamp,
    "receipt_time"           timestamp,
    "creator"       varchar(64) DEFAULT '',
    "create_time"   timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"       varchar(64) DEFAULT '',
    "update_time"   timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"       int2        NOT NULL DEFAULT 0,
    "tenant_id"     int8        NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_wm_transaction" IS 'MES 库存事务流水 DO';
COMMENT ON COLUMN "mes_wm_transaction"."id" IS '编号';
COMMENT ON COLUMN "mes_wm_transaction"."type" IS '事务类型';
COMMENT ON COLUMN "mes_wm_transaction"."biz_type" IS '业务类型';
COMMENT ON COLUMN "mes_wm_transaction"."biz_id" IS '来源业务主单 ID';
COMMENT ON COLUMN "mes_wm_transaction"."biz_code" IS '来源业务单号';
COMMENT ON COLUMN "mes_wm_transaction"."biz_line_id" IS '来源业务行 ID';
COMMENT ON COLUMN "mes_wm_transaction"."material_stock_id" IS '库存记录 ID';
COMMENT ON COLUMN "mes_wm_transaction"."related_transaction_id" IS '关联的事务 ID';
COMMENT ON COLUMN "mes_wm_transaction"."item_id" IS '物料 ID';
COMMENT ON COLUMN "mes_wm_transaction"."quantity" IS '本次变动数量';
COMMENT ON COLUMN "mes_wm_transaction"."batch_id" IS '批次 ID';
COMMENT ON COLUMN "mes_wm_transaction"."batch_code" IS '批次号';
COMMENT ON COLUMN "mes_wm_transaction"."warehouse_id" IS '仓库 ID';
COMMENT ON COLUMN "mes_wm_transaction"."location_id" IS '库区 ID';
COMMENT ON COLUMN "mes_wm_transaction"."area_id" IS '库位 ID';
COMMENT ON COLUMN "mes_wm_transaction"."transaction_time" IS '事务发生时间';
COMMENT ON COLUMN "mes_wm_transaction"."erp_time" IS 'ERP 账期';
COMMENT ON COLUMN "mes_wm_transaction"."receipt_time" IS '入库时间';

CREATE SEQUENCE IF NOT EXISTS "mes_wm_transaction_seq";

CREATE INDEX IF NOT EXISTS "idx_mes_wm_transaction_warehouse_id" ON "mes_wm_transaction" ("warehouse_id");
CREATE INDEX IF NOT EXISTS "idx_mes_wm_transaction_biz_id" ON "mes_wm_transaction" ("biz_id");

-- ===================== mes_wm_transfer =====================
-- MES 转移单 DO
DROP TABLE IF EXISTS "mes_wm_transfer";
CREATE TABLE IF NOT EXISTS "mes_wm_transfer" (
    "id"                    int8      NOT NULL,
    "code"                  varchar(32),
    "name"                  varchar(128),
    "type"                  int4,
    "delivery_flag"         int2       NOT NULL DEFAULT 0,
    "recipient_name"        varchar(64),
    "recipient_telephone"   varchar(20),
    "destination_address"   varchar(500),
    "carrier"               varchar(64),
    "shipping_number"       varchar(64),
    "confirm_flag"          int2       NOT NULL DEFAULT 0,
    "transfer_date"         timestamp,
    "status"                int4,
    "remark"                varchar(500),
    "creator"       varchar(64) DEFAULT '',
    "create_time"   timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"       varchar(64) DEFAULT '',
    "update_time"   timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"       int2        NOT NULL DEFAULT 0,
    "tenant_id"     int8        NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_wm_transfer" IS 'MES 转移单 DO';
COMMENT ON COLUMN "mes_wm_transfer"."id" IS '编号';
COMMENT ON COLUMN "mes_wm_transfer"."code" IS '转移单编号';
COMMENT ON COLUMN "mes_wm_transfer"."name" IS '转移单名称';
COMMENT ON COLUMN "mes_wm_transfer"."type" IS '转移单类型';
COMMENT ON COLUMN "mes_wm_transfer"."delivery_flag" IS '是否配送';
COMMENT ON COLUMN "mes_wm_transfer"."recipient_name" IS '收货人';
COMMENT ON COLUMN "mes_wm_transfer"."recipient_telephone" IS '联系方式';
COMMENT ON COLUMN "mes_wm_transfer"."destination_address" IS '目的地';
COMMENT ON COLUMN "mes_wm_transfer"."carrier" IS '承运商';
COMMENT ON COLUMN "mes_wm_transfer"."shipping_number" IS '运输单号';
COMMENT ON COLUMN "mes_wm_transfer"."confirm_flag" IS '是否已确认';
COMMENT ON COLUMN "mes_wm_transfer"."transfer_date" IS '转移日期';
COMMENT ON COLUMN "mes_wm_transfer"."status" IS '状态';
COMMENT ON COLUMN "mes_wm_transfer"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS "mes_wm_transfer_seq";

CREATE UNIQUE INDEX IF NOT EXISTS "idx_mes_wm_transfer_code" ON "mes_wm_transfer" ("code", tenant_id) WHERE deleted = 0;

-- ===================== mes_wm_transfer_detail =====================
-- MES 调拨明细 DO
DROP TABLE IF EXISTS "mes_wm_transfer_detail";
CREATE TABLE IF NOT EXISTS "mes_wm_transfer_detail" (
    "id"             int8      NOT NULL,
    "line_id"        int8,
    "transfer_id"    int8,
    "item_id"        int8,
    "quantity"       numeric(14,2),
    "batch_id"       int8,
    "to_warehouse_id" int8,
    "to_location_id" int8,
    "to_area_id"     int8,
    "remark"         varchar(500),
    "creator"       varchar(64) DEFAULT '',
    "create_time"   timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"       varchar(64) DEFAULT '',
    "update_time"   timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"       int2        NOT NULL DEFAULT 0,
    "tenant_id"     int8        NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_wm_transfer_detail" IS 'MES 调拨明细 DO';
COMMENT ON COLUMN "mes_wm_transfer_detail"."id" IS '编号';
COMMENT ON COLUMN "mes_wm_transfer_detail"."line_id" IS '转移单行编号';
COMMENT ON COLUMN "mes_wm_transfer_detail"."transfer_id" IS '转移单编号';
COMMENT ON COLUMN "mes_wm_transfer_detail"."item_id" IS '物料编号';
COMMENT ON COLUMN "mes_wm_transfer_detail"."quantity" IS '上架数量';
COMMENT ON COLUMN "mes_wm_transfer_detail"."batch_id" IS '批次编号';
COMMENT ON COLUMN "mes_wm_transfer_detail"."to_warehouse_id" IS '移入仓库编号';
COMMENT ON COLUMN "mes_wm_transfer_detail"."to_location_id" IS '移入库区编号';
COMMENT ON COLUMN "mes_wm_transfer_detail"."to_area_id" IS '移入库位编号';
COMMENT ON COLUMN "mes_wm_transfer_detail"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS "mes_wm_transfer_detail_seq";

CREATE INDEX IF NOT EXISTS "idx_mes_wm_transfer_detail_line_id" ON "mes_wm_transfer_detail" ("line_id");
CREATE INDEX IF NOT EXISTS "idx_mes_wm_transfer_detail_transfer_id" ON "mes_wm_transfer_detail" ("transfer_id");

-- ===================== mes_wm_transfer_line =====================
-- MES 转移单行 DO
DROP TABLE IF EXISTS "mes_wm_transfer_line";
CREATE TABLE IF NOT EXISTS "mes_wm_transfer_line" (
    "id"                int8      NOT NULL,
    "transfer_id"      int8,
    "material_stock_id" int8,
    "item_id"           int8,
    "quantity"          numeric(14,2),
    "batch_id"          int8,
    "from_warehouse_id" int8,
    "from_location_id"  int8,
    "from_area_id"      int8,
    "remark"            varchar(500),
    "creator"       varchar(64) DEFAULT '',
    "create_time"   timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"       varchar(64) DEFAULT '',
    "update_time"   timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"       int2        NOT NULL DEFAULT 0,
    "tenant_id"     int8        NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_wm_transfer_line" IS 'MES 转移单行 DO';
COMMENT ON COLUMN "mes_wm_transfer_line"."id" IS '编号';
COMMENT ON COLUMN "mes_wm_transfer_line"."transfer_id" IS '转移单编号';
COMMENT ON COLUMN "mes_wm_transfer_line"."material_stock_id" IS '库存记录编号';
COMMENT ON COLUMN "mes_wm_transfer_line"."item_id" IS '物料编号';
COMMENT ON COLUMN "mes_wm_transfer_line"."quantity" IS '转移数量';
COMMENT ON COLUMN "mes_wm_transfer_line"."batch_id" IS '批次编号';
COMMENT ON COLUMN "mes_wm_transfer_line"."from_warehouse_id" IS '移出仓库编号';
COMMENT ON COLUMN "mes_wm_transfer_line"."from_location_id" IS '移出库区编号';
COMMENT ON COLUMN "mes_wm_transfer_line"."from_area_id" IS '移出库位编号';
COMMENT ON COLUMN "mes_wm_transfer_line"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS "mes_wm_transfer_line_seq";

CREATE INDEX IF NOT EXISTS "idx_mes_wm_transfer_line_transfer_id" ON "mes_wm_transfer_line" ("transfer_id");

-- ===================== mes_wm_warehouse =====================
-- MES 仓库 DO
DROP TABLE IF EXISTS "mes_wm_warehouse";
CREATE TABLE IF NOT EXISTS "mes_wm_warehouse" (
    "id"              int8      NOT NULL,
    "code"            varchar(32),
    "name"            varchar(128),
    "address"         varchar(500),
    "area"            numeric(20,4),
    "charge_user_id"  int8,
    "frozen"          int2       NOT NULL DEFAULT 0,
    "remark"          varchar(500),
    "creator"       varchar(64) DEFAULT '',
    "create_time"   timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"       varchar(64) DEFAULT '',
    "update_time"   timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"       int2        NOT NULL DEFAULT 0,
    "tenant_id"     int8        NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_wm_warehouse" IS 'MES 仓库 DO';
COMMENT ON COLUMN "mes_wm_warehouse"."id" IS '编号';
COMMENT ON COLUMN "mes_wm_warehouse"."code" IS '仓库编码';
COMMENT ON COLUMN "mes_wm_warehouse"."name" IS '仓库名称';
COMMENT ON COLUMN "mes_wm_warehouse"."address" IS '仓库地址';
COMMENT ON COLUMN "mes_wm_warehouse"."area" IS '面积';
COMMENT ON COLUMN "mes_wm_warehouse"."charge_user_id" IS '负责人用户编号';
COMMENT ON COLUMN "mes_wm_warehouse"."frozen" IS '是否冻结';
COMMENT ON COLUMN "mes_wm_warehouse"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS "mes_wm_warehouse_seq";

CREATE UNIQUE INDEX IF NOT EXISTS "idx_mes_wm_warehouse_code" ON "mes_wm_warehouse" ("code", tenant_id) WHERE deleted = 0;

-- ===================== mes_wm_warehouse_area =====================
-- MES 库位 DO
DROP TABLE IF EXISTS "mes_wm_warehouse_area";
CREATE TABLE IF NOT EXISTS "mes_wm_warehouse_area" (
    "id"                  int8      NOT NULL,
    "code"                varchar(32),
    "name"                varchar(128),
    "location_id"         int8,
    "area"                numeric(20,4),
    "max_load"            numeric(20,4),
    "position_x"          int4,
    "position_y"          int4,
    "position_z"          int4,
    "status"              int4,
    "frozen"              int2       NOT NULL DEFAULT 0,
    "allow_item_mixing"   int2       NOT NULL DEFAULT 0,
    "allow_batch_mixing"  int2       NOT NULL DEFAULT 0,
    "remark"              varchar(500),
    "creator"       varchar(64) DEFAULT '',
    "create_time"   timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"       varchar(64) DEFAULT '',
    "update_time"   timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"       int2        NOT NULL DEFAULT 0,
    "tenant_id"     int8        NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_wm_warehouse_area" IS 'MES 库位 DO';
COMMENT ON COLUMN "mes_wm_warehouse_area"."id" IS '编号';
COMMENT ON COLUMN "mes_wm_warehouse_area"."code" IS '库位编码';
COMMENT ON COLUMN "mes_wm_warehouse_area"."name" IS '库位名称';
COMMENT ON COLUMN "mes_wm_warehouse_area"."location_id" IS '库区编号';
COMMENT ON COLUMN "mes_wm_warehouse_area"."area" IS '面积';
COMMENT ON COLUMN "mes_wm_warehouse_area"."max_load" IS '最大载重';
COMMENT ON COLUMN "mes_wm_warehouse_area"."position_x" IS '位置 X';
COMMENT ON COLUMN "mes_wm_warehouse_area"."position_y" IS '位置 Y';
COMMENT ON COLUMN "mes_wm_warehouse_area"."position_z" IS '位置 Z';
COMMENT ON COLUMN "mes_wm_warehouse_area"."status" IS '状态';
COMMENT ON COLUMN "mes_wm_warehouse_area"."frozen" IS '是否冻结';
COMMENT ON COLUMN "mes_wm_warehouse_area"."allow_item_mixing" IS '是否允许物料混放';
COMMENT ON COLUMN "mes_wm_warehouse_area"."allow_batch_mixing" IS '是否允许批次混放';
COMMENT ON COLUMN "mes_wm_warehouse_area"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS "mes_wm_warehouse_area_seq";

CREATE UNIQUE INDEX IF NOT EXISTS "idx_mes_wm_warehouse_area_code" ON "mes_wm_warehouse_area" ("code", tenant_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS "idx_mes_wm_warehouse_area_location_id" ON "mes_wm_warehouse_area" ("location_id");

-- ===================== mes_wm_warehouse_location =====================
-- MES 库区 DO
DROP TABLE IF EXISTS "mes_wm_warehouse_location";
CREATE TABLE IF NOT EXISTS "mes_wm_warehouse_location" (
    "id"            int8      NOT NULL,
    "code"          varchar(32),
    "name"          varchar(128),
    "warehouse_id"  int8,
    "area"          numeric(20,4),
    "frozen"        int2       NOT NULL DEFAULT 0,
    "remark"        varchar(500),
    "creator"       varchar(64) DEFAULT '',
    "create_time"   timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"       varchar(64) DEFAULT '',
    "update_time"   timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"       int2        NOT NULL DEFAULT 0,
    "tenant_id"     int8        NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "mes_wm_warehouse_location" IS 'MES 库区 DO';
COMMENT ON COLUMN "mes_wm_warehouse_location"."id" IS '编号';
COMMENT ON COLUMN "mes_wm_warehouse_location"."code" IS '库区编码';
COMMENT ON COLUMN "mes_wm_warehouse_location"."name" IS '库区名称';
COMMENT ON COLUMN "mes_wm_warehouse_location"."warehouse_id" IS '仓库编号';
COMMENT ON COLUMN "mes_wm_warehouse_location"."area" IS '面积';
COMMENT ON COLUMN "mes_wm_warehouse_location"."frozen" IS '是否冻结';
COMMENT ON COLUMN "mes_wm_warehouse_location"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS "mes_wm_warehouse_location_seq";

CREATE UNIQUE INDEX IF NOT EXISTS "idx_mes_wm_warehouse_location_code" ON "mes_wm_warehouse_location" ("code", tenant_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS "idx_mes_wm_warehouse_location_warehouse_id" ON "mes_wm_warehouse_location" ("warehouse_id");

