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
