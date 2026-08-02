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
