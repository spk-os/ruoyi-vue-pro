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
