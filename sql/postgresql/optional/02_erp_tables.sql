-- ============================================================
-- ERP 模块表 DDL（PostgreSQL）— 从本地 DO 反推
-- ============================================================
-- 来源：从 yudao-module-erp 的 *DO.java 反推生成（非社区 MySQL 脚本转换）
-- 数据库：PostgreSQL（docker-db_postgres-1 / 127.0.0.1:5433 / db=yudao）
-- 安全：仅 DROP IF EXISTS + CREATE IF NOT EXISTS（幂等）；无 INSERT、无外键约束、无 ENGINE
-- 约定：见同目录 ../_CONVENTIONS.md（tenant_id 全表必加、序列取 @KeySequence 字面值、JSON→text）
-- 运行：psql -1 -v ON_ERROR_STOP=1 -f 02_erp_tables.sql
-- ============================================================

-- ===================== erp_account =====================
-- ERP 结算账户
DROP TABLE IF EXISTS "erp_account";
CREATE TABLE IF NOT EXISTS "erp_account" (
    "id"             bigint      NOT NULL,
    "name"           varchar(128) NULL,
    "no"             varchar(128) NULL,
    "remark"         text        NULL,
    "status"         int4        NULL,
    "sort"           int4        NULL,
    "default_status" int2        NOT NULL DEFAULT 0,
    "creator"     varchar(64) DEFAULT '',
    "create_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"     varchar(64) DEFAULT '',
    "update_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"     int2        NOT NULL DEFAULT 0,
    "tenant_id"   bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "erp_account" IS 'ERP 结算账户';
COMMENT ON COLUMN "erp_account"."name" IS '账户名称';
COMMENT ON COLUMN "erp_account"."no" IS '账户编码';
COMMENT ON COLUMN "erp_account"."remark" IS '备注';
COMMENT ON COLUMN "erp_account"."status" IS '开启状态';
COMMENT ON COLUMN "erp_account"."sort" IS '排序';
COMMENT ON COLUMN "erp_account"."default_status" IS '是否默认';
CREATE SEQUENCE IF NOT EXISTS erp_account_seq;

-- ===================== erp_finance_payment =====================
-- ERP 付款单
DROP TABLE IF EXISTS "erp_finance_payment";
CREATE TABLE IF NOT EXISTS "erp_finance_payment" (
    "id"             bigint      NOT NULL,
    "no"             varchar(128) NULL,
    "status"         int4        NULL,
    "payment_time"   timestamp   NULL,
    "finance_user_id" bigint    NULL,
    "supplier_id"    bigint      NULL,
    "account_id"     bigint      NULL,
    "total_price"    numeric(24,6) NULL,
    "discount_price" numeric(24,6) NULL,
    "payment_price"  numeric(24,6) NULL,
    "remark"         text        NULL,
    "creator"     varchar(64) DEFAULT '',
    "create_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"     varchar(64) DEFAULT '',
    "update_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"     int2        NOT NULL DEFAULT 0,
    "tenant_id"   bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "erp_finance_payment" IS 'ERP 付款单';
COMMENT ON COLUMN "erp_finance_payment"."no" IS '付款单号';
COMMENT ON COLUMN "erp_finance_payment"."status" IS '付款状态';
COMMENT ON COLUMN "erp_finance_payment"."payment_time" IS '付款时间';
COMMENT ON COLUMN "erp_finance_payment"."finance_user_id" IS '财务人员编号';
COMMENT ON COLUMN "erp_finance_payment"."supplier_id" IS '供应商编号';
COMMENT ON COLUMN "erp_finance_payment"."account_id" IS '付款账户编号';
COMMENT ON COLUMN "erp_finance_payment"."total_price" IS '合计价格，单位：元';
COMMENT ON COLUMN "erp_finance_payment"."discount_price" IS '优惠金额，单位：元';
COMMENT ON COLUMN "erp_finance_payment"."payment_price" IS '实付金额，单位：分';
COMMENT ON COLUMN "erp_finance_payment"."remark" IS '备注';
CREATE SEQUENCE IF NOT EXISTS erp_finance_payment_seq;
CREATE INDEX IF NOT EXISTS idx_erp_finance_payment_supplier_id ON "erp_finance_payment" ("supplier_id");
CREATE INDEX IF NOT EXISTS idx_erp_finance_payment_account_id ON "erp_finance_payment" ("account_id");

-- ===================== erp_finance_payment_item =====================
-- ERP 付款项
DROP TABLE IF EXISTS "erp_finance_payment_item";
CREATE TABLE IF NOT EXISTS "erp_finance_payment_item" (
    "id"             bigint      NOT NULL,
    "payment_id"     bigint      NULL,
    "biz_type"       int4        NULL,
    "biz_id"         bigint      NULL,
    "biz_no"         varchar(128) NULL,
    "total_price"    numeric(24,6) NULL,
    "paid_price"     numeric(24,6) NULL,
    "payment_price"  numeric(24,6) NULL,
    "remark"         text        NULL,
    "creator"     varchar(64) DEFAULT '',
    "create_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"     varchar(64) DEFAULT '',
    "update_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"     int2        NOT NULL DEFAULT 0,
    "tenant_id"   bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "erp_finance_payment_item" IS 'ERP 付款项';
COMMENT ON COLUMN "erp_finance_payment_item"."payment_id" IS '付款单编号';
COMMENT ON COLUMN "erp_finance_payment_item"."biz_type" IS '业务类型';
COMMENT ON COLUMN "erp_finance_payment_item"."biz_id" IS '业务编号';
COMMENT ON COLUMN "erp_finance_payment_item"."biz_no" IS '业务单号';
COMMENT ON COLUMN "erp_finance_payment_item"."total_price" IS '应付金额，单位：分';
COMMENT ON COLUMN "erp_finance_payment_item"."paid_price" IS '已付金额，单位：分';
COMMENT ON COLUMN "erp_finance_payment_item"."payment_price" IS '本次付款，单位：分';
COMMENT ON COLUMN "erp_finance_payment_item"."remark" IS '备注';
CREATE SEQUENCE IF NOT EXISTS erp_finance_payment_item_seq;
CREATE INDEX IF NOT EXISTS idx_erp_finance_payment_item_payment_id ON "erp_finance_payment_item" ("payment_id");

-- ===================== erp_finance_receipt =====================
-- ERP 收款单
DROP TABLE IF EXISTS "erp_finance_receipt";
CREATE TABLE IF NOT EXISTS "erp_finance_receipt" (
    "id"             bigint      NOT NULL,
    "no"             varchar(128) NULL,
    "status"         int4        NULL,
    "receipt_time"   timestamp   NULL,
    "finance_user_id" bigint    NULL,
    "customer_id"    bigint      NULL,
    "account_id"     bigint      NULL,
    "total_price"    numeric(24,6) NULL,
    "discount_price" numeric(24,6) NULL,
    "receipt_price"  numeric(24,6) NULL,
    "remark"         text        NULL,
    "creator"     varchar(64) DEFAULT '',
    "create_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"     varchar(64) DEFAULT '',
    "update_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"     int2        NOT NULL DEFAULT 0,
    "tenant_id"   bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "erp_finance_receipt" IS 'ERP 收款单';
COMMENT ON COLUMN "erp_finance_receipt"."no" IS '收款单号';
COMMENT ON COLUMN "erp_finance_receipt"."status" IS '收款状态';
COMMENT ON COLUMN "erp_finance_receipt"."receipt_time" IS '收款时间';
COMMENT ON COLUMN "erp_finance_receipt"."finance_user_id" IS '财务人员编号';
COMMENT ON COLUMN "erp_finance_receipt"."customer_id" IS '客户编号';
COMMENT ON COLUMN "erp_finance_receipt"."account_id" IS '收款账户编号';
COMMENT ON COLUMN "erp_finance_receipt"."total_price" IS '合计价格，单位：元';
COMMENT ON COLUMN "erp_finance_receipt"."discount_price" IS '优惠金额，单位：元';
COMMENT ON COLUMN "erp_finance_receipt"."receipt_price" IS '实付金额，单位：分';
COMMENT ON COLUMN "erp_finance_receipt"."remark" IS '备注';
CREATE SEQUENCE IF NOT EXISTS erp_finance_receipt_seq;
CREATE INDEX IF NOT EXISTS idx_erp_finance_receipt_customer_id ON "erp_finance_receipt" ("customer_id");
CREATE INDEX IF NOT EXISTS idx_erp_finance_receipt_account_id ON "erp_finance_receipt" ("account_id");

-- ===================== erp_finance_receipt_item =====================
-- ERP 收款项
DROP TABLE IF EXISTS "erp_finance_receipt_item";
CREATE TABLE IF NOT EXISTS "erp_finance_receipt_item" (
    "id"             bigint      NOT NULL,
    "receipt_id"     bigint      NULL,
    "biz_type"       int4        NULL,
    "biz_id"         bigint      NULL,
    "biz_no"         varchar(128) NULL,
    "total_price"    numeric(24,6) NULL,
    "receipted_price" numeric(24,6) NULL,
    "receipt_price"  numeric(24,6) NULL,
    "remark"         text        NULL,
    "creator"     varchar(64) DEFAULT '',
    "create_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"     varchar(64) DEFAULT '',
    "update_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"     int2        NOT NULL DEFAULT 0,
    "tenant_id"   bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "erp_finance_receipt_item" IS 'ERP 收款项';
COMMENT ON COLUMN "erp_finance_receipt_item"."receipt_id" IS '收款单编号';
COMMENT ON COLUMN "erp_finance_receipt_item"."biz_type" IS '业务类型';
COMMENT ON COLUMN "erp_finance_receipt_item"."biz_id" IS '业务编号';
COMMENT ON COLUMN "erp_finance_receipt_item"."biz_no" IS '业务单号';
COMMENT ON COLUMN "erp_finance_receipt_item"."total_price" IS '应收金额，单位：分';
COMMENT ON COLUMN "erp_finance_receipt_item"."receipted_price" IS '已收金额，单位：分';
COMMENT ON COLUMN "erp_finance_receipt_item"."receipt_price" IS '本次收款，单位：分';
COMMENT ON COLUMN "erp_finance_receipt_item"."remark" IS '备注';
CREATE SEQUENCE IF NOT EXISTS erp_finance_receipt_item_seq;
CREATE INDEX IF NOT EXISTS idx_erp_finance_receipt_item_receipt_id ON "erp_finance_receipt_item" ("receipt_id");

-- ===================== erp_product_category =====================
-- ERP 产品分类
DROP TABLE IF EXISTS "erp_product_category";
CREATE TABLE IF NOT EXISTS "erp_product_category" (
    "id"         bigint      NOT NULL,
    "parent_id"  bigint      NULL,
    "name"       varchar(128) NULL,
    "code"       varchar(32)  NULL,
    "sort"       int4        NULL,
    "status"     int4        NULL,
    "creator"     varchar(64) DEFAULT '',
    "create_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"     varchar(64) DEFAULT '',
    "update_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"     int2        NOT NULL DEFAULT 0,
    "tenant_id"   bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "erp_product_category" IS 'ERP 产品分类';
COMMENT ON COLUMN "erp_product_category"."parent_id" IS '父分类编号';
COMMENT ON COLUMN "erp_product_category"."name" IS '分类名称';
COMMENT ON COLUMN "erp_product_category"."code" IS '分类编码';
COMMENT ON COLUMN "erp_product_category"."sort" IS '分类排序';
COMMENT ON COLUMN "erp_product_category"."status" IS '开启状态';
CREATE SEQUENCE IF NOT EXISTS erp_product_category_seq;
CREATE UNIQUE INDEX IF NOT EXISTS idx_erp_product_category_name ON "erp_product_category" ("name", "parent_id", tenant_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_erp_product_category_parent_id ON "erp_product_category" ("parent_id");

-- ===================== erp_product =====================
-- ERP 产品
DROP TABLE IF EXISTS "erp_product";
CREATE TABLE IF NOT EXISTS "erp_product" (
    "id"             bigint      NOT NULL,
    "name"           varchar(128) NULL,
    "bar_code"       varchar(64)  NULL,
    "category_id"    bigint      NULL,
    "unit_id"        bigint      NULL,
    "status"         int4        NULL,
    "standard"       varchar(255) NULL,
    "remark"         text        NULL,
    "expiry_day"     int4        NULL,
    "weight"         numeric(20,4) NULL,
    "purchase_price" numeric(24,6) NULL,
    "sale_price"     numeric(24,6) NULL,
    "min_price"      numeric(24,6) NULL,
    "creator"     varchar(64) DEFAULT '',
    "create_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"     varchar(64) DEFAULT '',
    "update_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"     int2        NOT NULL DEFAULT 0,
    "tenant_id"   bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "erp_product" IS 'ERP 产品';
COMMENT ON COLUMN "erp_product"."name" IS '产品名称';
COMMENT ON COLUMN "erp_product"."bar_code" IS '产品条码';
COMMENT ON COLUMN "erp_product"."category_id" IS '产品分类编号';
COMMENT ON COLUMN "erp_product"."unit_id" IS '单位编号';
COMMENT ON COLUMN "erp_product"."status" IS '产品状态';
COMMENT ON COLUMN "erp_product"."standard" IS '产品规格';
COMMENT ON COLUMN "erp_product"."remark" IS '产品备注';
COMMENT ON COLUMN "erp_product"."expiry_day" IS '保质期天数';
COMMENT ON COLUMN "erp_product"."weight" IS '基础重量（kg）';
COMMENT ON COLUMN "erp_product"."purchase_price" IS '采购价格，单位：元';
COMMENT ON COLUMN "erp_product"."sale_price" IS '销售价格，单位：元';
COMMENT ON COLUMN "erp_product"."min_price" IS '最低价格，单位：元';
CREATE SEQUENCE IF NOT EXISTS erp_product_seq;
CREATE UNIQUE INDEX IF NOT EXISTS idx_erp_product_bar_code ON "erp_product" ("bar_code", tenant_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_erp_product_category_id ON "erp_product" ("category_id");
CREATE INDEX IF NOT EXISTS idx_erp_product_unit_id ON "erp_product" ("unit_id");

-- ===================== erp_product_unit =====================
-- ERP 产品单位
DROP TABLE IF EXISTS "erp_product_unit";
CREATE TABLE IF NOT EXISTS "erp_product_unit" (
    "id"         bigint      NOT NULL,
    "name"       varchar(128) NULL,
    "status"     int4        NULL,
    "creator"     varchar(64) DEFAULT '',
    "create_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"     varchar(64) DEFAULT '',
    "update_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"     int2        NOT NULL DEFAULT 0,
    "tenant_id"   bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "erp_product_unit" IS 'ERP 产品单位';
COMMENT ON COLUMN "erp_product_unit"."name" IS '单位名字';
COMMENT ON COLUMN "erp_product_unit"."status" IS '单位状态';
CREATE SEQUENCE IF NOT EXISTS erp_product_unit_seq;

-- ===================== erp_purchase_in =====================
-- ERP 采购入库
DROP TABLE IF EXISTS "erp_purchase_in";
CREATE TABLE IF NOT EXISTS "erp_purchase_in" (
    "id"                 bigint      NOT NULL,
    "no"                 varchar(128) NULL,
    "status"             int4        NULL,
    "supplier_id"        bigint      NULL,
    "account_id"         bigint      NULL,
    "in_time"            timestamp   NULL,
    "order_id"           bigint      NULL,
    "order_no"           varchar(128) NULL,
    "total_count"        numeric(20,4) NULL,
    "total_price"        numeric(24,6) NULL,
    "payment_price"      numeric(24,6) NULL,
    "total_product_price" numeric(24,6) NULL,
    "total_tax_price"    numeric(24,6) NULL,
    "discount_percent"   numeric(24,6) NULL,
    "discount_price"     numeric(24,6) NULL,
    "other_price"        numeric(24,6) NULL,
    "file_url"           varchar(500) NULL,
    "remark"             text        NULL,
    "creator"     varchar(64) DEFAULT '',
    "create_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"     varchar(64) DEFAULT '',
    "update_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"     int2        NOT NULL DEFAULT 0,
    "tenant_id"   bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "erp_purchase_in" IS 'ERP 采购入库';
COMMENT ON COLUMN "erp_purchase_in"."no" IS '采购入库单号';
COMMENT ON COLUMN "erp_purchase_in"."status" IS '入库状态';
COMMENT ON COLUMN "erp_purchase_in"."supplier_id" IS '供应商编号';
COMMENT ON COLUMN "erp_purchase_in"."account_id" IS '结算账户编号';
COMMENT ON COLUMN "erp_purchase_in"."in_time" IS '入库时间';
COMMENT ON COLUMN "erp_purchase_in"."order_id" IS '采购订单编号';
COMMENT ON COLUMN "erp_purchase_in"."order_no" IS '采购订单号';
COMMENT ON COLUMN "erp_purchase_in"."total_count" IS '合计数量';
COMMENT ON COLUMN "erp_purchase_in"."total_price" IS '最终合计价格，单位：元';
COMMENT ON COLUMN "erp_purchase_in"."payment_price" IS '已支付金额，单位：元';
COMMENT ON COLUMN "erp_purchase_in"."total_product_price" IS '合计产品价格，单位：元';
COMMENT ON COLUMN "erp_purchase_in"."total_tax_price" IS '合计税额，单位：元';
COMMENT ON COLUMN "erp_purchase_in"."discount_percent" IS '优惠率，百分比';
COMMENT ON COLUMN "erp_purchase_in"."discount_price" IS '优惠金额，单位：元';
COMMENT ON COLUMN "erp_purchase_in"."other_price" IS '其它金额，单位：元';
COMMENT ON COLUMN "erp_purchase_in"."file_url" IS '附件地址';
COMMENT ON COLUMN "erp_purchase_in"."remark" IS '备注';
CREATE SEQUENCE IF NOT EXISTS erp_purchase_in_seq;
CREATE INDEX IF NOT EXISTS idx_erp_purchase_in_supplier_id ON "erp_purchase_in" ("supplier_id");
CREATE INDEX IF NOT EXISTS idx_erp_purchase_in_account_id ON "erp_purchase_in" ("account_id");
CREATE INDEX IF NOT EXISTS idx_erp_purchase_in_order_id ON "erp_purchase_in" ("order_id");

-- ===================== erp_purchase_in_items =====================
-- ERP 采购入库项
DROP TABLE IF EXISTS "erp_purchase_in_items";
CREATE TABLE IF NOT EXISTS "erp_purchase_in_items" (
    "id"               bigint      NOT NULL,
    "in_id"            bigint      NULL,
    "order_item_id"    bigint      NULL,
    "warehouse_id"     bigint      NULL,
    "product_id"       bigint      NULL,
    "product_unit_id"  bigint      NULL,
    "product_price"    numeric(24,6) NULL,
    "count"            numeric(20,4) NULL,
    "total_price"      numeric(24,6) NULL,
    "tax_percent"      numeric(24,6) NULL,
    "tax_price"        numeric(24,6) NULL,
    "remark"           text        NULL,
    "creator"     varchar(64) DEFAULT '',
    "create_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"     varchar(64) DEFAULT '',
    "update_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"     int2        NOT NULL DEFAULT 0,
    "tenant_id"   bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "erp_purchase_in_items" IS 'ERP 采购入库项';
COMMENT ON COLUMN "erp_purchase_in_items"."in_id" IS '采购入库编号';
COMMENT ON COLUMN "erp_purchase_in_items"."order_item_id" IS '采购订单项编号';
COMMENT ON COLUMN "erp_purchase_in_items"."warehouse_id" IS '仓库编号';
COMMENT ON COLUMN "erp_purchase_in_items"."product_id" IS '产品编号';
COMMENT ON COLUMN "erp_purchase_in_items"."product_unit_id" IS '产品单位编号';
COMMENT ON COLUMN "erp_purchase_in_items"."product_price" IS '产品单位单价，单位：元';
COMMENT ON COLUMN "erp_purchase_in_items"."count" IS '数量';
COMMENT ON COLUMN "erp_purchase_in_items"."total_price" IS '总价，单位：元';
COMMENT ON COLUMN "erp_purchase_in_items"."tax_percent" IS '税率，百分比';
COMMENT ON COLUMN "erp_purchase_in_items"."tax_price" IS '税额，单位：元';
COMMENT ON COLUMN "erp_purchase_in_items"."remark" IS '备注';
CREATE SEQUENCE IF NOT EXISTS erp_purchase_in_items_seq;
CREATE INDEX IF NOT EXISTS idx_erp_purchase_in_items_in_id ON "erp_purchase_in_items" ("in_id");
CREATE INDEX IF NOT EXISTS idx_erp_purchase_in_items_warehouse_id ON "erp_purchase_in_items" ("warehouse_id");
CREATE INDEX IF NOT EXISTS idx_erp_purchase_in_items_product_id ON "erp_purchase_in_items" ("product_id");

-- ===================== erp_purchase_order =====================
-- ERP 采购订单
DROP TABLE IF EXISTS "erp_purchase_order";
CREATE TABLE IF NOT EXISTS "erp_purchase_order" (
    "id"                 bigint      NOT NULL,
    "no"                 varchar(128) NULL,
    "status"             int4        NULL,
    "supplier_id"        bigint      NULL,
    "account_id"         bigint      NULL,
    "order_time"         timestamp   NULL,
    "total_count"        numeric(20,4) NULL,
    "total_price"        numeric(24,6) NULL,
    "total_product_price" numeric(24,6) NULL,
    "total_tax_price"    numeric(24,6) NULL,
    "discount_percent"   numeric(24,6) NULL,
    "discount_price"     numeric(24,6) NULL,
    "deposit_price"      numeric(24,6) NULL,
    "file_url"           varchar(500) NULL,
    "remark"             text        NULL,
    "in_count"           numeric(20,4) NULL,
    "return_count"       numeric(20,4) NULL,
    "creator"     varchar(64) DEFAULT '',
    "create_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"     varchar(64) DEFAULT '',
    "update_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"     int2        NOT NULL DEFAULT 0,
    "tenant_id"   bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "erp_purchase_order" IS 'ERP 采购订单';
COMMENT ON COLUMN "erp_purchase_order"."no" IS '采购订单号';
COMMENT ON COLUMN "erp_purchase_order"."status" IS '采购状态';
COMMENT ON COLUMN "erp_purchase_order"."supplier_id" IS '供应商编号';
COMMENT ON COLUMN "erp_purchase_order"."account_id" IS '结算账户编号';
COMMENT ON COLUMN "erp_purchase_order"."order_time" IS '下单时间';
COMMENT ON COLUMN "erp_purchase_order"."total_count" IS '合计数量';
COMMENT ON COLUMN "erp_purchase_order"."total_price" IS '最终合计价格，单位：元';
COMMENT ON COLUMN "erp_purchase_order"."total_product_price" IS '合计产品价格，单位：元';
COMMENT ON COLUMN "erp_purchase_order"."total_tax_price" IS '合计税额，单位：元';
COMMENT ON COLUMN "erp_purchase_order"."discount_percent" IS '优惠率，百分比';
COMMENT ON COLUMN "erp_purchase_order"."discount_price" IS '优惠金额，单位：元';
COMMENT ON COLUMN "erp_purchase_order"."deposit_price" IS '定金金额，单位：元';
COMMENT ON COLUMN "erp_purchase_order"."file_url" IS '附件地址';
COMMENT ON COLUMN "erp_purchase_order"."remark" IS '备注';
COMMENT ON COLUMN "erp_purchase_order"."in_count" IS '采购入库数量';
COMMENT ON COLUMN "erp_purchase_order"."return_count" IS '采购退货数量';
CREATE SEQUENCE IF NOT EXISTS erp_purchase_order_seq;
CREATE INDEX IF NOT EXISTS idx_erp_purchase_order_supplier_id ON "erp_purchase_order" ("supplier_id");
CREATE INDEX IF NOT EXISTS idx_erp_purchase_order_account_id ON "erp_purchase_order" ("account_id");

-- ===================== erp_purchase_order_items =====================
-- ERP 采购订单项
DROP TABLE IF EXISTS "erp_purchase_order_items";
CREATE TABLE IF NOT EXISTS "erp_purchase_order_items" (
    "id"               bigint      NOT NULL,
    "order_id"         bigint      NULL,
    "product_id"       bigint      NULL,
    "product_unit_id"  bigint      NULL,
    "product_price"    numeric(24,6) NULL,
    "count"            numeric(20,4) NULL,
    "total_price"      numeric(24,6) NULL,
    "tax_percent"      numeric(24,6) NULL,
    "tax_price"        numeric(24,6) NULL,
    "remark"           text        NULL,
    "in_count"         numeric(20,4) NULL,
    "return_count"     numeric(20,4) NULL,
    "creator"     varchar(64) DEFAULT '',
    "create_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"     varchar(64) DEFAULT '',
    "update_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"     int2        NOT NULL DEFAULT 0,
    "tenant_id"   bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "erp_purchase_order_items" IS 'ERP 采购订单项';
COMMENT ON COLUMN "erp_purchase_order_items"."order_id" IS '采购订单编号';
COMMENT ON COLUMN "erp_purchase_order_items"."product_id" IS '产品编号';
COMMENT ON COLUMN "erp_purchase_order_items"."product_unit_id" IS '产品单位编号';
COMMENT ON COLUMN "erp_purchase_order_items"."product_price" IS '产品单位单价，单位：元';
COMMENT ON COLUMN "erp_purchase_order_items"."count" IS '数量';
COMMENT ON COLUMN "erp_purchase_order_items"."total_price" IS '总价，单位：元';
COMMENT ON COLUMN "erp_purchase_order_items"."tax_percent" IS '税率，百分比';
COMMENT ON COLUMN "erp_purchase_order_items"."tax_price" IS '税额，单位：元';
COMMENT ON COLUMN "erp_purchase_order_items"."remark" IS '备注';
COMMENT ON COLUMN "erp_purchase_order_items"."in_count" IS '采购入库数量';
COMMENT ON COLUMN "erp_purchase_order_items"."return_count" IS '采购退货数量';
CREATE SEQUENCE IF NOT EXISTS erp_purchase_order_items_seq;
CREATE INDEX IF NOT EXISTS idx_erp_purchase_order_items_order_id ON "erp_purchase_order_items" ("order_id");
CREATE INDEX IF NOT EXISTS idx_erp_purchase_order_items_product_id ON "erp_purchase_order_items" ("product_id");

-- ===================== erp_purchase_return =====================
-- ERP 采购退货
DROP TABLE IF EXISTS "erp_purchase_return";
CREATE TABLE IF NOT EXISTS "erp_purchase_return" (
    "id"                 bigint      NOT NULL,
    "no"                 varchar(128) NULL,
    "status"             int4        NULL,
    "supplier_id"        bigint      NULL,
    "account_id"         bigint      NULL,
    "return_time"        timestamp   NULL,
    "order_id"           bigint      NULL,
    "order_no"           varchar(128) NULL,
    "total_count"        numeric(20,4) NULL,
    "total_price"        numeric(24,6) NULL,
    "refund_price"       numeric(24,6) NULL,
    "total_product_price" numeric(24,6) NULL,
    "total_tax_price"    numeric(24,6) NULL,
    "discount_percent"   numeric(24,6) NULL,
    "discount_price"     numeric(24,6) NULL,
    "other_price"        numeric(24,6) NULL,
    "file_url"           varchar(500) NULL,
    "remark"             text        NULL,
    "creator"     varchar(64) DEFAULT '',
    "create_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"     varchar(64) DEFAULT '',
    "update_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"     int2        NOT NULL DEFAULT 0,
    "tenant_id"   bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "erp_purchase_return" IS 'ERP 采购退货';
COMMENT ON COLUMN "erp_purchase_return"."no" IS '采购退货单号';
COMMENT ON COLUMN "erp_purchase_return"."status" IS '退货状态';
COMMENT ON COLUMN "erp_purchase_return"."supplier_id" IS '供应商编号';
COMMENT ON COLUMN "erp_purchase_return"."account_id" IS '结算账户编号';
COMMENT ON COLUMN "erp_purchase_return"."return_time" IS '退货时间';
COMMENT ON COLUMN "erp_purchase_return"."order_id" IS '采购订单编号';
COMMENT ON COLUMN "erp_purchase_return"."order_no" IS '采购订单号';
COMMENT ON COLUMN "erp_purchase_return"."total_count" IS '合计数量';
COMMENT ON COLUMN "erp_purchase_return"."total_price" IS '最终合计价格，单位：元';
COMMENT ON COLUMN "erp_purchase_return"."refund_price" IS '已退款金额，单位：元';
COMMENT ON COLUMN "erp_purchase_return"."total_product_price" IS '合计产品价格，单位：元';
COMMENT ON COLUMN "erp_purchase_return"."total_tax_price" IS '合计税额，单位：元';
COMMENT ON COLUMN "erp_purchase_return"."discount_percent" IS '优惠率，百分比';
COMMENT ON COLUMN "erp_purchase_return"."discount_price" IS '优惠金额，单位：元';
COMMENT ON COLUMN "erp_purchase_return"."other_price" IS '其它金额，单位：元';
COMMENT ON COLUMN "erp_purchase_return"."file_url" IS '附件地址';
COMMENT ON COLUMN "erp_purchase_return"."remark" IS '备注';
CREATE SEQUENCE IF NOT EXISTS erp_purchase_return_seq;
CREATE INDEX IF NOT EXISTS idx_erp_purchase_return_supplier_id ON "erp_purchase_return" ("supplier_id");
CREATE INDEX IF NOT EXISTS idx_erp_purchase_return_order_id ON "erp_purchase_return" ("order_id");

-- ===================== erp_purchase_return_items =====================
-- ERP 采购退货项
DROP TABLE IF EXISTS "erp_purchase_return_items";
CREATE TABLE IF NOT EXISTS "erp_purchase_return_items" (
    "id"               bigint      NOT NULL,
    "return_id"        bigint      NULL,
    "order_item_id"    bigint      NULL,
    "warehouse_id"     bigint      NULL,
    "product_id"       bigint      NULL,
    "product_unit_id"  bigint      NULL,
    "product_price"    numeric(24,6) NULL,
    "count"            numeric(20,4) NULL,
    "total_price"      numeric(24,6) NULL,
    "tax_percent"      numeric(24,6) NULL,
    "tax_price"        numeric(24,6) NULL,
    "remark"           text        NULL,
    "creator"     varchar(64) DEFAULT '',
    "create_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"     varchar(64) DEFAULT '',
    "update_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"     int2        NOT NULL DEFAULT 0,
    "tenant_id"   bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "erp_purchase_return_items" IS 'ERP 采购退货项';
COMMENT ON COLUMN "erp_purchase_return_items"."return_id" IS '采购退货编号';
COMMENT ON COLUMN "erp_purchase_return_items"."order_item_id" IS '采购订单项编号';
COMMENT ON COLUMN "erp_purchase_return_items"."warehouse_id" IS '仓库编号';
COMMENT ON COLUMN "erp_purchase_return_items"."product_id" IS '产品编号';
COMMENT ON COLUMN "erp_purchase_return_items"."product_unit_id" IS '产品单位编号';
COMMENT ON COLUMN "erp_purchase_return_items"."product_price" IS '产品单位单价，单位：元';
COMMENT ON COLUMN "erp_purchase_return_items"."count" IS '数量';
COMMENT ON COLUMN "erp_purchase_return_items"."total_price" IS '总价，单位：元';
COMMENT ON COLUMN "erp_purchase_return_items"."tax_percent" IS '税率，百分比';
COMMENT ON COLUMN "erp_purchase_return_items"."tax_price" IS '税额，单位：元';
COMMENT ON COLUMN "erp_purchase_return_items"."remark" IS '备注';
CREATE SEQUENCE IF NOT EXISTS erp_purchase_return_items_seq;
CREATE INDEX IF NOT EXISTS idx_erp_purchase_return_items_return_id ON "erp_purchase_return_items" ("return_id");
CREATE INDEX IF NOT EXISTS idx_erp_purchase_return_items_warehouse_id ON "erp_purchase_return_items" ("warehouse_id");
CREATE INDEX IF NOT EXISTS idx_erp_purchase_return_items_product_id ON "erp_purchase_return_items" ("product_id");

-- ===================== erp_supplier =====================
-- ERP 供应商
DROP TABLE IF EXISTS "erp_supplier";
CREATE TABLE IF NOT EXISTS "erp_supplier" (
    "id"           bigint      NOT NULL,
    "name"         varchar(128) NULL,
    "contact"      varchar(64)  NULL,
    "mobile"       varchar(20)  NULL,
    "telephone"    varchar(20)  NULL,
    "email"        varchar(50)  NULL,
    "fax"          varchar(32)  NULL,
    "remark"       text        NULL,
    "status"       int4        NULL,
    "sort"         int4        NULL,
    "tax_no"       varchar(64)  NULL,
    "tax_percent"  numeric(24,6) NULL,
    "bank_name"    varchar(128) NULL,
    "bank_account" varchar(64)  NULL,
    "bank_address" varchar(255) NULL,
    "creator"     varchar(64) DEFAULT '',
    "create_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"     varchar(64) DEFAULT '',
    "update_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"     int2        NOT NULL DEFAULT 0,
    "tenant_id"   bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "erp_supplier" IS 'ERP 供应商';
COMMENT ON COLUMN "erp_supplier"."name" IS '供应商名称';
COMMENT ON COLUMN "erp_supplier"."contact" IS '联系人';
COMMENT ON COLUMN "erp_supplier"."mobile" IS '手机号码';
COMMENT ON COLUMN "erp_supplier"."telephone" IS '联系电话';
COMMENT ON COLUMN "erp_supplier"."email" IS '电子邮箱';
COMMENT ON COLUMN "erp_supplier"."fax" IS '传真';
COMMENT ON COLUMN "erp_supplier"."remark" IS '备注';
COMMENT ON COLUMN "erp_supplier"."status" IS '开启状态';
COMMENT ON COLUMN "erp_supplier"."sort" IS '排序';
COMMENT ON COLUMN "erp_supplier"."tax_no" IS '纳税人识别号';
COMMENT ON COLUMN "erp_supplier"."tax_percent" IS '税率';
COMMENT ON COLUMN "erp_supplier"."bank_name" IS '开户行';
COMMENT ON COLUMN "erp_supplier"."bank_account" IS '开户账号';
COMMENT ON COLUMN "erp_supplier"."bank_address" IS '开户地址';
CREATE SEQUENCE IF NOT EXISTS erp_supplier_seq;

-- ===================== erp_customer =====================
-- ERP 客户
DROP TABLE IF EXISTS "erp_customer";
CREATE TABLE IF NOT EXISTS "erp_customer" (
    "id"           bigint      NOT NULL,
    "name"         varchar(128) NULL,
    "contact"      varchar(64)  NULL,
    "mobile"       varchar(20)  NULL,
    "telephone"    varchar(20)  NULL,
    "email"        varchar(50)  NULL,
    "fax"          varchar(32)  NULL,
    "remark"       text        NULL,
    "status"       int4        NULL,
    "sort"         int4        NULL,
    "tax_no"       varchar(64)  NULL,
    "tax_percent"  numeric(24,6) NULL,
    "bank_name"    varchar(128) NULL,
    "bank_account" varchar(64)  NULL,
    "bank_address" varchar(255) NULL,
    "creator"     varchar(64) DEFAULT '',
    "create_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"     varchar(64) DEFAULT '',
    "update_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"     int2        NOT NULL DEFAULT 0,
    "tenant_id"   bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "erp_customer" IS 'ERP 客户';
COMMENT ON COLUMN "erp_customer"."name" IS '客户名称';
COMMENT ON COLUMN "erp_customer"."contact" IS '联系人';
COMMENT ON COLUMN "erp_customer"."mobile" IS '手机号码';
COMMENT ON COLUMN "erp_customer"."telephone" IS '联系电话';
COMMENT ON COLUMN "erp_customer"."email" IS '电子邮箱';
COMMENT ON COLUMN "erp_customer"."fax" IS '传真';
COMMENT ON COLUMN "erp_customer"."remark" IS '备注';
COMMENT ON COLUMN "erp_customer"."status" IS '开启状态';
COMMENT ON COLUMN "erp_customer"."sort" IS '排序';
COMMENT ON COLUMN "erp_customer"."tax_no" IS '纳税人识别号';
COMMENT ON COLUMN "erp_customer"."tax_percent" IS '税率';
COMMENT ON COLUMN "erp_customer"."bank_name" IS '开户行';
COMMENT ON COLUMN "erp_customer"."bank_account" IS '开户账号';
COMMENT ON COLUMN "erp_customer"."bank_address" IS '开户地址';
CREATE SEQUENCE IF NOT EXISTS erp_customer_seq;

-- ===================== erp_sale_order =====================
-- ERP 销售订单
DROP TABLE IF EXISTS "erp_sale_order";
CREATE TABLE IF NOT EXISTS "erp_sale_order" (
    "id"                 bigint      NOT NULL,
    "no"                 varchar(128) NULL,
    "status"             int4        NULL,
    "customer_id"        bigint      NULL,
    "account_id"         bigint      NULL,
    "sale_user_id"       bigint      NULL,
    "order_time"         timestamp   NULL,
    "total_count"        numeric(20,4) NULL,
    "total_price"        numeric(24,6) NULL,
    "total_product_price" numeric(24,6) NULL,
    "total_tax_price"    numeric(24,6) NULL,
    "discount_percent"   numeric(24,6) NULL,
    "discount_price"     numeric(24,6) NULL,
    "deposit_price"      numeric(24,6) NULL,
    "file_url"           varchar(500) NULL,
    "remark"             text        NULL,
    "out_count"          numeric(20,4) NULL,
    "return_count"       numeric(20,4) NULL,
    "creator"     varchar(64) DEFAULT '',
    "create_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"     varchar(64) DEFAULT '',
    "update_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"     int2        NOT NULL DEFAULT 0,
    "tenant_id"   bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "erp_sale_order" IS 'ERP 销售订单';
COMMENT ON COLUMN "erp_sale_order"."no" IS '销售订单号';
COMMENT ON COLUMN "erp_sale_order"."status" IS '销售状态';
COMMENT ON COLUMN "erp_sale_order"."customer_id" IS '客户编号';
COMMENT ON COLUMN "erp_sale_order"."account_id" IS '结算账户编号';
COMMENT ON COLUMN "erp_sale_order"."sale_user_id" IS '销售员编号';
COMMENT ON COLUMN "erp_sale_order"."order_time" IS '下单时间';
COMMENT ON COLUMN "erp_sale_order"."total_count" IS '合计数量';
COMMENT ON COLUMN "erp_sale_order"."total_price" IS '最终合计价格，单位：元';
COMMENT ON COLUMN "erp_sale_order"."total_product_price" IS '合计产品价格，单位：元';
COMMENT ON COLUMN "erp_sale_order"."total_tax_price" IS '合计税额，单位：元';
COMMENT ON COLUMN "erp_sale_order"."discount_percent" IS '优惠率，百分比';
COMMENT ON COLUMN "erp_sale_order"."discount_price" IS '优惠金额，单位：元';
COMMENT ON COLUMN "erp_sale_order"."deposit_price" IS '定金金额，单位：元';
COMMENT ON COLUMN "erp_sale_order"."file_url" IS '附件地址';
COMMENT ON COLUMN "erp_sale_order"."remark" IS '备注';
COMMENT ON COLUMN "erp_sale_order"."out_count" IS '销售出库数量';
COMMENT ON COLUMN "erp_sale_order"."return_count" IS '销售退货数量';
CREATE SEQUENCE IF NOT EXISTS erp_sale_order_seq;
CREATE INDEX IF NOT EXISTS idx_erp_sale_order_customer_id ON "erp_sale_order" ("customer_id");
CREATE INDEX IF NOT EXISTS idx_erp_sale_order_account_id ON "erp_sale_order" ("account_id");

-- ===================== erp_sale_order_items =====================
-- ERP 销售订单项
DROP TABLE IF EXISTS "erp_sale_order_items";
CREATE TABLE IF NOT EXISTS "erp_sale_order_items" (
    "id"               bigint      NOT NULL,
    "order_id"         bigint      NULL,
    "product_id"       bigint      NULL,
    "product_unit_id"  bigint      NULL,
    "product_price"    numeric(24,6) NULL,
    "count"            numeric(20,4) NULL,
    "total_price"      numeric(24,6) NULL,
    "tax_percent"      numeric(24,6) NULL,
    "tax_price"        numeric(24,6) NULL,
    "remark"           text        NULL,
    "out_count"        numeric(20,4) NULL,
    "return_count"     numeric(20,4) NULL,
    "creator"     varchar(64) DEFAULT '',
    "create_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"     varchar(64) DEFAULT '',
    "update_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"     int2        NOT NULL DEFAULT 0,
    "tenant_id"   bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "erp_sale_order_items" IS 'ERP 销售订单项';
COMMENT ON COLUMN "erp_sale_order_items"."order_id" IS '销售订单编号';
COMMENT ON COLUMN "erp_sale_order_items"."product_id" IS '产品编号';
COMMENT ON COLUMN "erp_sale_order_items"."product_unit_id" IS '产品单位编号';
COMMENT ON COLUMN "erp_sale_order_items"."product_price" IS '产品单位单价，单位：元';
COMMENT ON COLUMN "erp_sale_order_items"."count" IS '数量';
COMMENT ON COLUMN "erp_sale_order_items"."total_price" IS '总价，单位：元';
COMMENT ON COLUMN "erp_sale_order_items"."tax_percent" IS '税率，百分比';
COMMENT ON COLUMN "erp_sale_order_items"."tax_price" IS '税额，单位：元';
COMMENT ON COLUMN "erp_sale_order_items"."remark" IS '备注';
COMMENT ON COLUMN "erp_sale_order_items"."out_count" IS '销售出库数量';
COMMENT ON COLUMN "erp_sale_order_items"."return_count" IS '销售退货数量';
CREATE SEQUENCE IF NOT EXISTS erp_sale_order_items_seq;
CREATE INDEX IF NOT EXISTS idx_erp_sale_order_items_order_id ON "erp_sale_order_items" ("order_id");
CREATE INDEX IF NOT EXISTS idx_erp_sale_order_items_product_id ON "erp_sale_order_items" ("product_id");

-- ===================== erp_sale_out =====================
-- ERP 销售出库
DROP TABLE IF EXISTS "erp_sale_out";
CREATE TABLE IF NOT EXISTS "erp_sale_out" (
    "id"                 bigint      NOT NULL,
    "no"                 varchar(128) NULL,
    "status"             int4        NULL,
    "customer_id"        bigint      NULL,
    "account_id"         bigint      NULL,
    "sale_user_id"       bigint      NULL,
    "out_time"           timestamp   NULL,
    "order_id"           bigint      NULL,
    "order_no"           varchar(128) NULL,
    "total_count"        numeric(20,4) NULL,
    "total_price"        numeric(24,6) NULL,
    "receipt_price"      numeric(24,6) NULL,
    "total_product_price" numeric(24,6) NULL,
    "total_tax_price"    numeric(24,6) NULL,
    "discount_percent"   numeric(24,6) NULL,
    "discount_price"     numeric(24,6) NULL,
    "other_price"        numeric(24,6) NULL,
    "file_url"           varchar(500) NULL,
    "remark"             text        NULL,
    "creator"     varchar(64) DEFAULT '',
    "create_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"     varchar(64) DEFAULT '',
    "update_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"     int2        NOT NULL DEFAULT 0,
    "tenant_id"   bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "erp_sale_out" IS 'ERP 销售出库';
COMMENT ON COLUMN "erp_sale_out"."no" IS '销售出库单号';
COMMENT ON COLUMN "erp_sale_out"."status" IS '出库状态';
COMMENT ON COLUMN "erp_sale_out"."customer_id" IS '客户编号';
COMMENT ON COLUMN "erp_sale_out"."account_id" IS '结算账户编号';
COMMENT ON COLUMN "erp_sale_out"."sale_user_id" IS '销售员编号';
COMMENT ON COLUMN "erp_sale_out"."out_time" IS '出库时间';
COMMENT ON COLUMN "erp_sale_out"."order_id" IS '销售订单编号';
COMMENT ON COLUMN "erp_sale_out"."order_no" IS '销售订单号';
COMMENT ON COLUMN "erp_sale_out"."total_count" IS '合计数量';
COMMENT ON COLUMN "erp_sale_out"."total_price" IS '最终合计价格，单位：元';
COMMENT ON COLUMN "erp_sale_out"."receipt_price" IS '已收款金额，单位：元';
COMMENT ON COLUMN "erp_sale_out"."total_product_price" IS '合计产品价格，单位：元';
COMMENT ON COLUMN "erp_sale_out"."total_tax_price" IS '合计税额，单位：元';
COMMENT ON COLUMN "erp_sale_out"."discount_percent" IS '优惠率，百分比';
COMMENT ON COLUMN "erp_sale_out"."discount_price" IS '优惠金额，单位：元';
COMMENT ON COLUMN "erp_sale_out"."other_price" IS '其它金额，单位：元';
COMMENT ON COLUMN "erp_sale_out"."file_url" IS '附件地址';
COMMENT ON COLUMN "erp_sale_out"."remark" IS '备注';
CREATE SEQUENCE IF NOT EXISTS erp_sale_out_seq;
CREATE INDEX IF NOT EXISTS idx_erp_sale_out_customer_id ON "erp_sale_out" ("customer_id");
CREATE INDEX IF NOT EXISTS idx_erp_sale_out_account_id ON "erp_sale_out" ("account_id");
CREATE INDEX IF NOT EXISTS idx_erp_sale_out_order_id ON "erp_sale_out" ("order_id");

-- ===================== erp_sale_out_items =====================
-- ERP 销售出库项
DROP TABLE IF EXISTS "erp_sale_out_items";
CREATE TABLE IF NOT EXISTS "erp_sale_out_items" (
    "id"               bigint      NOT NULL,
    "out_id"           bigint      NULL,
    "order_item_id"    bigint      NULL,
    "warehouse_id"     bigint      NULL,
    "product_id"       bigint      NULL,
    "product_unit_id"  bigint      NULL,
    "product_price"    numeric(24,6) NULL,
    "count"            numeric(20,4) NULL,
    "total_price"      numeric(24,6) NULL,
    "tax_percent"      numeric(24,6) NULL,
    "tax_price"        numeric(24,6) NULL,
    "remark"           text        NULL,
    "creator"     varchar(64) DEFAULT '',
    "create_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"     varchar(64) DEFAULT '',
    "update_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"     int2        NOT NULL DEFAULT 0,
    "tenant_id"   bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "erp_sale_out_items" IS 'ERP 销售出库项';
COMMENT ON COLUMN "erp_sale_out_items"."out_id" IS '销售出库编号';
COMMENT ON COLUMN "erp_sale_out_items"."order_item_id" IS '销售订单项编号';
COMMENT ON COLUMN "erp_sale_out_items"."warehouse_id" IS '仓库编号';
COMMENT ON COLUMN "erp_sale_out_items"."product_id" IS '产品编号';
COMMENT ON COLUMN "erp_sale_out_items"."product_unit_id" IS '产品单位编号';
COMMENT ON COLUMN "erp_sale_out_items"."product_price" IS '产品单位单价，单位：元';
COMMENT ON COLUMN "erp_sale_out_items"."count" IS '数量';
COMMENT ON COLUMN "erp_sale_out_items"."total_price" IS '总价，单位：元';
COMMENT ON COLUMN "erp_sale_out_items"."tax_percent" IS '税率，百分比';
COMMENT ON COLUMN "erp_sale_out_items"."tax_price" IS '税额，单位：元';
COMMENT ON COLUMN "erp_sale_out_items"."remark" IS '备注';
CREATE SEQUENCE IF NOT EXISTS erp_sale_out_items_seq;
CREATE INDEX IF NOT EXISTS idx_erp_sale_out_items_out_id ON "erp_sale_out_items" ("out_id");
CREATE INDEX IF NOT EXISTS idx_erp_sale_out_items_warehouse_id ON "erp_sale_out_items" ("warehouse_id");
CREATE INDEX IF NOT EXISTS idx_erp_sale_out_items_product_id ON "erp_sale_out_items" ("product_id");

-- ===================== erp_sale_return =====================
-- ERP 销售退货
DROP TABLE IF EXISTS "erp_sale_return";
CREATE TABLE IF NOT EXISTS "erp_sale_return" (
    "id"                 bigint      NOT NULL,
    "no"                 varchar(128) NULL,
    "status"             int4        NULL,
    "customer_id"        bigint      NULL,
    "account_id"         bigint      NULL,
    "sale_user_id"       bigint      NULL,
    "return_time"        timestamp   NULL,
    "order_id"           bigint      NULL,
    "order_no"           varchar(128) NULL,
    "total_count"        numeric(20,4) NULL,
    "total_price"        numeric(24,6) NULL,
    "refund_price"       numeric(24,6) NULL,
    "total_product_price" numeric(24,6) NULL,
    "total_tax_price"    numeric(24,6) NULL,
    "discount_percent"   numeric(24,6) NULL,
    "discount_price"     numeric(24,6) NULL,
    "other_price"        numeric(24,6) NULL,
    "file_url"           varchar(500) NULL,
    "remark"             text        NULL,
    "creator"     varchar(64) DEFAULT '',
    "create_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"     varchar(64) DEFAULT '',
    "update_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"     int2        NOT NULL DEFAULT 0,
    "tenant_id"   bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "erp_sale_return" IS 'ERP 销售退货';
COMMENT ON COLUMN "erp_sale_return"."no" IS '销售退货单号';
COMMENT ON COLUMN "erp_sale_return"."status" IS '退货状态';
COMMENT ON COLUMN "erp_sale_return"."customer_id" IS '客户编号';
COMMENT ON COLUMN "erp_sale_return"."account_id" IS '结算账户编号';
COMMENT ON COLUMN "erp_sale_return"."sale_user_id" IS '销售员编号';
COMMENT ON COLUMN "erp_sale_return"."return_time" IS '退货时间';
COMMENT ON COLUMN "erp_sale_return"."order_id" IS '销售订单编号';
COMMENT ON COLUMN "erp_sale_return"."order_no" IS '销售订单号';
COMMENT ON COLUMN "erp_sale_return"."total_count" IS '合计数量';
COMMENT ON COLUMN "erp_sale_return"."total_price" IS '最终合计价格，单位：元';
COMMENT ON COLUMN "erp_sale_return"."refund_price" IS '已退款金额，单位：元';
COMMENT ON COLUMN "erp_sale_return"."total_product_price" IS '合计产品价格，单位：元';
COMMENT ON COLUMN "erp_sale_return"."total_tax_price" IS '合计税额，单位：元';
COMMENT ON COLUMN "erp_sale_return"."discount_percent" IS '优惠率，百分比';
COMMENT ON COLUMN "erp_sale_return"."discount_price" IS '优惠金额，单位：元';
COMMENT ON COLUMN "erp_sale_return"."other_price" IS '其它金额，单位：元';
COMMENT ON COLUMN "erp_sale_return"."file_url" IS '附件地址';
COMMENT ON COLUMN "erp_sale_return"."remark" IS '备注';
CREATE SEQUENCE IF NOT EXISTS erp_sale_return_seq;
CREATE INDEX IF NOT EXISTS idx_erp_sale_return_customer_id ON "erp_sale_return" ("customer_id");
CREATE INDEX IF NOT EXISTS idx_erp_sale_return_order_id ON "erp_sale_return" ("order_id");

-- ===================== erp_sale_return_items =====================
-- ERP 销售退货项
DROP TABLE IF EXISTS "erp_sale_return_items";
CREATE TABLE IF NOT EXISTS "erp_sale_return_items" (
    "id"               bigint      NOT NULL,
    "return_id"        bigint      NULL,
    "order_item_id"    bigint      NULL,
    "warehouse_id"     bigint      NULL,
    "product_id"       bigint      NULL,
    "product_unit_id"  bigint      NULL,
    "product_price"    numeric(24,6) NULL,
    "count"            numeric(20,4) NULL,
    "total_price"      numeric(24,6) NULL,
    "tax_percent"      numeric(24,6) NULL,
    "tax_price"        numeric(24,6) NULL,
    "remark"           text        NULL,
    "creator"     varchar(64) DEFAULT '',
    "create_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"     varchar(64) DEFAULT '',
    "update_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"     int2        NOT NULL DEFAULT 0,
    "tenant_id"   bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "erp_sale_return_items" IS 'ERP 销售退货项';
COMMENT ON COLUMN "erp_sale_return_items"."return_id" IS '销售退货编号';
COMMENT ON COLUMN "erp_sale_return_items"."order_item_id" IS '销售订单项编号';
COMMENT ON COLUMN "erp_sale_return_items"."warehouse_id" IS '仓库编号';
COMMENT ON COLUMN "erp_sale_return_items"."product_id" IS '产品编号';
COMMENT ON COLUMN "erp_sale_return_items"."product_unit_id" IS '产品单位编号';
COMMENT ON COLUMN "erp_sale_return_items"."product_price" IS '产品单位单价，单位：元';
COMMENT ON COLUMN "erp_sale_return_items"."count" IS '数量';
COMMENT ON COLUMN "erp_sale_return_items"."total_price" IS '总价，单位：元';
COMMENT ON COLUMN "erp_sale_return_items"."tax_percent" IS '税率，百分比';
COMMENT ON COLUMN "erp_sale_return_items"."tax_price" IS '税额，单位：元';
COMMENT ON COLUMN "erp_sale_return_items"."remark" IS '备注';
CREATE SEQUENCE IF NOT EXISTS erp_sale_return_items_seq;
CREATE INDEX IF NOT EXISTS idx_erp_sale_return_items_return_id ON "erp_sale_return_items" ("return_id");
CREATE INDEX IF NOT EXISTS idx_erp_sale_return_items_warehouse_id ON "erp_sale_return_items" ("warehouse_id");
CREATE INDEX IF NOT EXISTS idx_erp_sale_return_items_product_id ON "erp_sale_return_items" ("product_id");

-- ===================== erp_stock_check =====================
-- ERP 库存盘点单
DROP TABLE IF EXISTS "erp_stock_check";
CREATE TABLE IF NOT EXISTS "erp_stock_check" (
    "id"           bigint      NOT NULL,
    "no"           varchar(128) NULL,
    "check_time"   timestamp   NULL,
    "total_count"  numeric(20,4) NULL,
    "total_price"  numeric(24,6) NULL,
    "status"       int4        NULL,
    "remark"       text        NULL,
    "file_url"     varchar(500) NULL,
    "creator"     varchar(64) DEFAULT '',
    "create_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"     varchar(64) DEFAULT '',
    "update_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"     int2        NOT NULL DEFAULT 0,
    "tenant_id"   bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "erp_stock_check" IS 'ERP 库存盘点单';
COMMENT ON COLUMN "erp_stock_check"."no" IS '盘点单号';
COMMENT ON COLUMN "erp_stock_check"."check_time" IS '盘点时间';
COMMENT ON COLUMN "erp_stock_check"."total_count" IS '合计数量';
COMMENT ON COLUMN "erp_stock_check"."total_price" IS '合计金额，单位：元';
COMMENT ON COLUMN "erp_stock_check"."status" IS '状态';
COMMENT ON COLUMN "erp_stock_check"."remark" IS '备注';
COMMENT ON COLUMN "erp_stock_check"."file_url" IS '附件 URL';
CREATE SEQUENCE IF NOT EXISTS erp_stock_check_seq;

-- ===================== erp_stock_check_item =====================
-- ERP 库存盘点单项
DROP TABLE IF EXISTS "erp_stock_check_item";
CREATE TABLE IF NOT EXISTS "erp_stock_check_item" (
    "id"              bigint      NOT NULL,
    "check_id"        bigint      NULL,
    "warehouse_id"    bigint      NULL,
    "product_id"      bigint      NULL,
    "product_unit_id" bigint      NULL,
    "product_price"   numeric(24,6) NULL,
    "stock_count"     numeric(20,4) NULL,
    "actual_count"    numeric(20,4) NULL,
    "count"           numeric(20,4) NULL,
    "total_price"     numeric(24,6) NULL,
    "remark"          text        NULL,
    "creator"     varchar(64) DEFAULT '',
    "create_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"     varchar(64) DEFAULT '',
    "update_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"     int2        NOT NULL DEFAULT 0,
    "tenant_id"   bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "erp_stock_check_item" IS 'ERP 库存盘点单项';
COMMENT ON COLUMN "erp_stock_check_item"."check_id" IS '盘点编号';
COMMENT ON COLUMN "erp_stock_check_item"."warehouse_id" IS '仓库编号';
COMMENT ON COLUMN "erp_stock_check_item"."product_id" IS '产品编号';
COMMENT ON COLUMN "erp_stock_check_item"."product_unit_id" IS '产品单位编号';
COMMENT ON COLUMN "erp_stock_check_item"."product_price" IS '产品单价';
COMMENT ON COLUMN "erp_stock_check_item"."stock_count" IS '账面数量（当前库存）';
COMMENT ON COLUMN "erp_stock_check_item"."actual_count" IS '实际数量（实际库存）';
COMMENT ON COLUMN "erp_stock_check_item"."count" IS '盈亏数量';
COMMENT ON COLUMN "erp_stock_check_item"."total_price" IS '合计金额，单位：元';
COMMENT ON COLUMN "erp_stock_check_item"."remark" IS '备注';
CREATE SEQUENCE IF NOT EXISTS erp_stock_check_item_seq;
CREATE INDEX IF NOT EXISTS idx_erp_stock_check_item_check_id ON "erp_stock_check_item" ("check_id");
CREATE INDEX IF NOT EXISTS idx_erp_stock_check_item_warehouse_id ON "erp_stock_check_item" ("warehouse_id");
CREATE INDEX IF NOT EXISTS idx_erp_stock_check_item_product_id ON "erp_stock_check_item" ("product_id");

-- ===================== erp_stock =====================
-- ERP 产品库存
DROP TABLE IF EXISTS "erp_stock";
CREATE TABLE IF NOT EXISTS "erp_stock" (
    "id"           bigint      NOT NULL,
    "product_id"   bigint      NULL,
    "warehouse_id" bigint     NULL,
    "count"        numeric(20,4) NULL,
    "creator"     varchar(64) DEFAULT '',
    "create_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"     varchar(64) DEFAULT '',
    "update_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"     int2        NOT NULL DEFAULT 0,
    "tenant_id"   bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "erp_stock" IS 'ERP 产品库存';
COMMENT ON COLUMN "erp_stock"."product_id" IS '产品编号';
COMMENT ON COLUMN "erp_stock"."warehouse_id" IS '仓库编号';
COMMENT ON COLUMN "erp_stock"."count" IS '库存数量';
CREATE SEQUENCE IF NOT EXISTS erp_stock_seq;
CREATE UNIQUE INDEX IF NOT EXISTS idx_erp_stock_product_warehouse ON "erp_stock" ("product_id", "warehouse_id", tenant_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_erp_stock_product_id ON "erp_stock" ("product_id");
CREATE INDEX IF NOT EXISTS idx_erp_stock_warehouse_id ON "erp_stock" ("warehouse_id");

-- ===================== erp_stock_in =====================
-- ERP 其它入库单
DROP TABLE IF EXISTS "erp_stock_in";
CREATE TABLE IF NOT EXISTS "erp_stock_in" (
    "id"           bigint      NOT NULL,
    "no"           varchar(128) NULL,
    "supplier_id"  bigint      NULL,
    "in_time"      timestamp   NULL,
    "total_count"  numeric(20,4) NULL,
    "total_price"  numeric(24,6) NULL,
    "status"       int4        NULL,
    "remark"       text        NULL,
    "file_url"     varchar(500) NULL,
    "creator"     varchar(64) DEFAULT '',
    "create_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"     varchar(64) DEFAULT '',
    "update_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"     int2        NOT NULL DEFAULT 0,
    "tenant_id"   bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "erp_stock_in" IS 'ERP 其它入库单';
COMMENT ON COLUMN "erp_stock_in"."no" IS '入库单号';
COMMENT ON COLUMN "erp_stock_in"."supplier_id" IS '供应商编号';
COMMENT ON COLUMN "erp_stock_in"."in_time" IS '入库时间';
COMMENT ON COLUMN "erp_stock_in"."total_count" IS '合计数量';
COMMENT ON COLUMN "erp_stock_in"."total_price" IS '合计金额，单位：元';
COMMENT ON COLUMN "erp_stock_in"."status" IS '状态';
COMMENT ON COLUMN "erp_stock_in"."remark" IS '备注';
COMMENT ON COLUMN "erp_stock_in"."file_url" IS '附件 URL';
CREATE SEQUENCE IF NOT EXISTS erp_stock_in_seq;
CREATE INDEX IF NOT EXISTS idx_erp_stock_in_supplier_id ON "erp_stock_in" ("supplier_id");

-- ===================== erp_stock_in_item =====================
-- ERP 其它入库单项
DROP TABLE IF EXISTS "erp_stock_in_item";
CREATE TABLE IF NOT EXISTS "erp_stock_in_item" (
    "id"              bigint      NOT NULL,
    "in_id"           bigint      NULL,
    "warehouse_id"    bigint      NULL,
    "product_id"      bigint      NULL,
    "product_unit_id" bigint      NULL,
    "product_price"   numeric(24,6) NULL,
    "count"           numeric(20,4) NULL,
    "total_price"     numeric(24,6) NULL,
    "remark"          text        NULL,
    "creator"     varchar(64) DEFAULT '',
    "create_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"     varchar(64) DEFAULT '',
    "update_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"     int2        NOT NULL DEFAULT 0,
    "tenant_id"   bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "erp_stock_in_item" IS 'ERP 其它入库单项';
COMMENT ON COLUMN "erp_stock_in_item"."in_id" IS '入库编号';
COMMENT ON COLUMN "erp_stock_in_item"."warehouse_id" IS '仓库编号';
COMMENT ON COLUMN "erp_stock_in_item"."product_id" IS '产品编号';
COMMENT ON COLUMN "erp_stock_in_item"."product_unit_id" IS '产品单位编号';
COMMENT ON COLUMN "erp_stock_in_item"."product_price" IS '产品单价';
COMMENT ON COLUMN "erp_stock_in_item"."count" IS '产品数量';
COMMENT ON COLUMN "erp_stock_in_item"."total_price" IS '合计金额，单位：元';
COMMENT ON COLUMN "erp_stock_in_item"."remark" IS '备注';
CREATE SEQUENCE IF NOT EXISTS erp_stock_in_item_seq;
CREATE INDEX IF NOT EXISTS idx_erp_stock_in_item_in_id ON "erp_stock_in_item" ("in_id");
CREATE INDEX IF NOT EXISTS idx_erp_stock_in_item_warehouse_id ON "erp_stock_in_item" ("warehouse_id");
CREATE INDEX IF NOT EXISTS idx_erp_stock_in_item_product_id ON "erp_stock_in_item" ("product_id");

-- ===================== erp_stock_move =====================
-- ERP 库存调拨单
DROP TABLE IF EXISTS "erp_stock_move";
CREATE TABLE IF NOT EXISTS "erp_stock_move" (
    "id"           bigint      NOT NULL,
    "no"           varchar(128) NULL,
    "move_time"    timestamp   NULL,
    "total_count"  numeric(20,4) NULL,
    "total_price"  numeric(24,6) NULL,
    "status"       int4        NULL,
    "remark"       text        NULL,
    "file_url"     varchar(500) NULL,
    "creator"     varchar(64) DEFAULT '',
    "create_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"     varchar(64) DEFAULT '',
    "update_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"     int2        NOT NULL DEFAULT 0,
    "tenant_id"   bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "erp_stock_move" IS 'ERP 库存调拨单';
COMMENT ON COLUMN "erp_stock_move"."no" IS '调拨单号';
COMMENT ON COLUMN "erp_stock_move"."move_time" IS '调拨时间';
COMMENT ON COLUMN "erp_stock_move"."total_count" IS '合计数量';
COMMENT ON COLUMN "erp_stock_move"."total_price" IS '合计金额，单位：元';
COMMENT ON COLUMN "erp_stock_move"."status" IS '状态';
COMMENT ON COLUMN "erp_stock_move"."remark" IS '备注';
COMMENT ON COLUMN "erp_stock_move"."file_url" IS '附件 URL';
CREATE SEQUENCE IF NOT EXISTS erp_stock_move_seq;

-- ===================== erp_stock_move_item =====================
-- ERP 库存调拨单项
DROP TABLE IF EXISTS "erp_stock_move_item";
CREATE TABLE IF NOT EXISTS "erp_stock_move_item" (
    "id"                bigint      NOT NULL,
    "move_id"           bigint      NULL,
    "from_warehouse_id" bigint      NULL,
    "to_warehouse_id"   bigint      NULL,
    "product_id"        bigint      NULL,
    "product_unit_id"   bigint      NULL,
    "product_price"     numeric(24,6) NULL,
    "count"             numeric(20,4) NULL,
    "total_price"       numeric(24,6) NULL,
    "remark"            text        NULL,
    "creator"     varchar(64) DEFAULT '',
    "create_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"     varchar(64) DEFAULT '',
    "update_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"     int2        NOT NULL DEFAULT 0,
    "tenant_id"   bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "erp_stock_move_item" IS 'ERP 库存调拨单项';
COMMENT ON COLUMN "erp_stock_move_item"."move_id" IS '调拨编号';
COMMENT ON COLUMN "erp_stock_move_item"."from_warehouse_id" IS '调出仓库编号';
COMMENT ON COLUMN "erp_stock_move_item"."to_warehouse_id" IS '调入仓库编号';
COMMENT ON COLUMN "erp_stock_move_item"."product_id" IS '产品编号';
COMMENT ON COLUMN "erp_stock_move_item"."product_unit_id" IS '产品单位编号';
COMMENT ON COLUMN "erp_stock_move_item"."product_price" IS '产品单价';
COMMENT ON COLUMN "erp_stock_move_item"."count" IS '产品数量';
COMMENT ON COLUMN "erp_stock_move_item"."total_price" IS '合计金额，单位：元';
COMMENT ON COLUMN "erp_stock_move_item"."remark" IS '备注';
CREATE SEQUENCE IF NOT EXISTS erp_stock_move_item_seq;
CREATE INDEX IF NOT EXISTS idx_erp_stock_move_item_move_id ON "erp_stock_move_item" ("move_id");
CREATE INDEX IF NOT EXISTS idx_erp_stock_move_item_product_id ON "erp_stock_move_item" ("product_id");

-- ===================== erp_stock_out =====================
-- ERP 其它出库单
DROP TABLE IF EXISTS "erp_stock_out";
CREATE TABLE IF NOT EXISTS "erp_stock_out" (
    "id"           bigint      NOT NULL,
    "no"           varchar(128) NULL,
    "customer_id"  bigint      NULL,
    "out_time"     timestamp   NULL,
    "total_count"  numeric(20,4) NULL,
    "total_price"  numeric(24,6) NULL,
    "status"       int4        NULL,
    "remark"       text        NULL,
    "file_url"     varchar(500) NULL,
    "creator"     varchar(64) DEFAULT '',
    "create_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"     varchar(64) DEFAULT '',
    "update_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"     int2        NOT NULL DEFAULT 0,
    "tenant_id"   bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "erp_stock_out" IS 'ERP 其它出库单';
COMMENT ON COLUMN "erp_stock_out"."no" IS '出库单号';
COMMENT ON COLUMN "erp_stock_out"."customer_id" IS '客户编号';
COMMENT ON COLUMN "erp_stock_out"."out_time" IS '出库时间';
COMMENT ON COLUMN "erp_stock_out"."total_count" IS '合计数量';
COMMENT ON COLUMN "erp_stock_out"."total_price" IS '合计金额，单位：元';
COMMENT ON COLUMN "erp_stock_out"."status" IS '状态';
COMMENT ON COLUMN "erp_stock_out"."remark" IS '备注';
COMMENT ON COLUMN "erp_stock_out"."file_url" IS '附件 URL';
CREATE SEQUENCE IF NOT EXISTS erp_stock_out_seq;
CREATE INDEX IF NOT EXISTS idx_erp_stock_out_customer_id ON "erp_stock_out" ("customer_id");

-- ===================== erp_stock_out_item =====================
-- ERP 其它出库单项
DROP TABLE IF EXISTS "erp_stock_out_item";
CREATE TABLE IF NOT EXISTS "erp_stock_out_item" (
    "id"              bigint      NOT NULL,
    "out_id"          bigint      NULL,
    "warehouse_id"    bigint      NULL,
    "product_id"      bigint      NULL,
    "product_unit_id" bigint      NULL,
    "product_price"   numeric(24,6) NULL,
    "count"           numeric(20,4) NULL,
    "total_price"     numeric(24,6) NULL,
    "remark"          text        NULL,
    "creator"     varchar(64) DEFAULT '',
    "create_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"     varchar(64) DEFAULT '',
    "update_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"     int2        NOT NULL DEFAULT 0,
    "tenant_id"   bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "erp_stock_out_item" IS 'ERP 其它出库单项';
COMMENT ON COLUMN "erp_stock_out_item"."out_id" IS '出库编号';
COMMENT ON COLUMN "erp_stock_out_item"."warehouse_id" IS '仓库编号';
COMMENT ON COLUMN "erp_stock_out_item"."product_id" IS '产品编号';
COMMENT ON COLUMN "erp_stock_out_item"."product_unit_id" IS '产品单位编号';
COMMENT ON COLUMN "erp_stock_out_item"."product_price" IS '产品单价';
COMMENT ON COLUMN "erp_stock_out_item"."count" IS '产品数量';
COMMENT ON COLUMN "erp_stock_out_item"."total_price" IS '合计金额，单位：元';
COMMENT ON COLUMN "erp_stock_out_item"."remark" IS '备注';
CREATE SEQUENCE IF NOT EXISTS erp_stock_out_item_seq;
CREATE INDEX IF NOT EXISTS idx_erp_stock_out_item_out_id ON "erp_stock_out_item" ("out_id");
CREATE INDEX IF NOT EXISTS idx_erp_stock_out_item_warehouse_id ON "erp_stock_out_item" ("warehouse_id");
CREATE INDEX IF NOT EXISTS idx_erp_stock_out_item_product_id ON "erp_stock_out_item" ("product_id");

-- ===================== erp_stock_record =====================
-- ERP 产品库存明细
DROP TABLE IF EXISTS "erp_stock_record";
CREATE TABLE IF NOT EXISTS "erp_stock_record" (
    "id"            bigint      NOT NULL,
    "product_id"    bigint      NULL,
    "warehouse_id"  bigint      NULL,
    "count"         numeric(20,4) NULL,
    "total_count"   numeric(20,4) NULL,
    "biz_type"      int4        NULL,
    "biz_id"        bigint      NULL,
    "biz_item_id"   bigint      NULL,
    "biz_no"        varchar(128) NULL,
    "creator"     varchar(64) DEFAULT '',
    "create_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"     varchar(64) DEFAULT '',
    "update_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"     int2        NOT NULL DEFAULT 0,
    "tenant_id"   bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "erp_stock_record" IS 'ERP 产品库存明细';
COMMENT ON COLUMN "erp_stock_record"."product_id" IS '产品编号';
COMMENT ON COLUMN "erp_stock_record"."warehouse_id" IS '仓库编号';
COMMENT ON COLUMN "erp_stock_record"."count" IS '出入库数量';
COMMENT ON COLUMN "erp_stock_record"."total_count" IS '总库存量';
COMMENT ON COLUMN "erp_stock_record"."biz_type" IS '业务类型';
COMMENT ON COLUMN "erp_stock_record"."biz_id" IS '业务编号';
COMMENT ON COLUMN "erp_stock_record"."biz_item_id" IS '业务项编号';
COMMENT ON COLUMN "erp_stock_record"."biz_no" IS '业务单号';
CREATE SEQUENCE IF NOT EXISTS erp_stock_record_seq;
CREATE INDEX IF NOT EXISTS idx_erp_stock_record_product_id ON "erp_stock_record" ("product_id");
CREATE INDEX IF NOT EXISTS idx_erp_stock_record_warehouse_id ON "erp_stock_record" ("warehouse_id");
CREATE INDEX IF NOT EXISTS idx_erp_stock_record_biz_id ON "erp_stock_record" ("biz_id");

-- ===================== erp_warehouse =====================
-- ERP 仓库
DROP TABLE IF EXISTS "erp_warehouse";
CREATE TABLE IF NOT EXISTS "erp_warehouse" (
    "id"               bigint      NOT NULL,
    "name"             varchar(128) NULL,
    "address"          varchar(255) NULL,
    "sort"             bigint      NULL,
    "remark"           text        NULL,
    "principal"        varchar(64)  NULL,
    "warehouse_price"  numeric(24,6) NULL,
    "truckage_price"   numeric(24,6) NULL,
    "status"           int4        NULL,
    "default_status"   int2        NOT NULL DEFAULT 0,
    "creator"     varchar(64) DEFAULT '',
    "create_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"     varchar(64) DEFAULT '',
    "update_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"     int2        NOT NULL DEFAULT 0,
    "tenant_id"   bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "erp_warehouse" IS 'ERP 仓库';
COMMENT ON COLUMN "erp_warehouse"."name" IS '仓库名称';
COMMENT ON COLUMN "erp_warehouse"."address" IS '仓库地址';
COMMENT ON COLUMN "erp_warehouse"."sort" IS '排序';
COMMENT ON COLUMN "erp_warehouse"."remark" IS '备注';
COMMENT ON COLUMN "erp_warehouse"."principal" IS '负责人';
COMMENT ON COLUMN "erp_warehouse"."warehouse_price" IS '仓储费，单位：元';
COMMENT ON COLUMN "erp_warehouse"."truckage_price" IS '搬运费，单位：元';
COMMENT ON COLUMN "erp_warehouse"."status" IS '开启状态';
COMMENT ON COLUMN "erp_warehouse"."default_status" IS '是否默认';
CREATE SEQUENCE IF NOT EXISTS erp_warehouse_seq;
