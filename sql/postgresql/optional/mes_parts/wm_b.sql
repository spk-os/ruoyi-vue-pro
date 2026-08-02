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
