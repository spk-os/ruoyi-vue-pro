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
