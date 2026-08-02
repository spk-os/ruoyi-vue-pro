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

