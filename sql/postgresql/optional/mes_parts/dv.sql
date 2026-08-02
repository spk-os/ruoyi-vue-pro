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
