-- ============================================================
-- CRM 模块表 DDL（PostgreSQL）— 从本地 DO 反推
-- ============================================================
-- 来源：从 yudao-module-crm 的 *DO.java 反推生成（非社区 MySQL 脚本转换）
-- 数据库：PostgreSQL（docker-db_postgres-1 / 127.0.0.1:5433 / db=yudao）
-- 安全：仅 DROP IF EXISTS + CREATE IF NOT EXISTS（幂等）；无 INSERT、无外键约束、无 ENGINE
-- 约定：见同目录 ../_CONVENTIONS.md（tenant_id 全表必加、序列取 @KeySequence 字面值、JSON→text）
-- 运行：psql -1 -v ON_ERROR_STOP=1 -f 01_crm_tables.sql
-- ============================================================

-- ===================== crm_business =====================
-- CRM 商机
DROP TABLE IF EXISTS "crm_business";
CREATE TABLE IF NOT EXISTS "crm_business" (
    "id"                  bigint       NOT NULL,
    "name"                varchar(128) NULL,
    "customer_id"         bigint       NULL,
    "follow_up_status"    int2         NOT NULL DEFAULT 0,
    "contact_last_time"   timestamp    NULL,
    "contact_next_time"   timestamp    NULL,
    "owner_user_id"       bigint       NULL,
    "status_type_id"      bigint       NULL,
    "status_id"           bigint       NULL,
    "end_status"          int4         NULL,
    "end_remark"          varchar(255) NULL,
    "deal_time"           timestamp    NULL,
    "total_product_price" numeric(24,6) NULL,
    "discount_percent"    numeric(24,6) NULL,
    "total_price"         numeric(24,6) NULL,
    "remark"              varchar(255) NULL,
    "creator"             varchar(64)  DEFAULT '',
    "create_time"         timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"             varchar(64)  DEFAULT '',
    "update_time"         timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"             int2         NOT NULL DEFAULT 0,
    "tenant_id"           bigint       NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "crm_business" IS 'CRM 商机';
COMMENT ON COLUMN "crm_business"."name" IS '商机名称';
COMMENT ON COLUMN "crm_business"."customer_id" IS '客户编号';
COMMENT ON COLUMN "crm_business"."follow_up_status" IS '跟进状态';
COMMENT ON COLUMN "crm_business"."contact_last_time" IS '最后跟进时间';
COMMENT ON COLUMN "crm_business"."contact_next_time" IS '下次联系时间';
COMMENT ON COLUMN "crm_business"."owner_user_id" IS '负责人的用户编号';
COMMENT ON COLUMN "crm_business"."status_type_id" IS '商机状态组编号';
COMMENT ON COLUMN "crm_business"."status_id" IS '商机状态编号';
COMMENT ON COLUMN "crm_business"."end_status" IS '结束状态';
COMMENT ON COLUMN "crm_business"."end_remark" IS '结束时的备注';
COMMENT ON COLUMN "crm_business"."deal_time" IS '预计成交日期';
COMMENT ON COLUMN "crm_business"."total_product_price" IS '产品总金额，单位：元';
COMMENT ON COLUMN "crm_business"."discount_percent" IS '整单折扣，百分比';
COMMENT ON COLUMN "crm_business"."total_price" IS '商机总金额，单位：元';
COMMENT ON COLUMN "crm_business"."remark" IS '备注';
CREATE SEQUENCE IF NOT EXISTS crm_business_seq;
CREATE INDEX IF NOT EXISTS idx_crm_business_customer_id ON "crm_business" ("customer_id");
CREATE INDEX IF NOT EXISTS idx_crm_business_owner_user_id ON "crm_business" ("owner_user_id");
CREATE INDEX IF NOT EXISTS idx_crm_business_status_type_id ON "crm_business" ("status_type_id");
CREATE INDEX IF NOT EXISTS idx_crm_business_status_id ON "crm_business" ("status_id");

-- ===================== crm_business_product =====================
-- CRM 商机产品关联表
DROP TABLE IF EXISTS "crm_business_product";
CREATE TABLE IF NOT EXISTS "crm_business_product" (
    "id"             bigint       NOT NULL,
    "business_id"    bigint       NULL,
    "product_id"     bigint       NULL,
    "product_price"  numeric(24,6) NULL,
    "business_price" numeric(24,6) NULL,
    "count"          numeric(20,4) NULL,
    "total_price"    numeric(24,6) NULL,
    "creator"        varchar(64)  DEFAULT '',
    "create_time"    timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"        varchar(64)  DEFAULT '',
    "update_time"    timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"        int2         NOT NULL DEFAULT 0,
    "tenant_id"      bigint       NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "crm_business_product" IS 'CRM 商机产品关联表';
COMMENT ON COLUMN "crm_business_product"."business_id" IS '商机编号';
COMMENT ON COLUMN "crm_business_product"."product_id" IS '产品编号';
COMMENT ON COLUMN "crm_business_product"."product_price" IS '产品单价，单位：元';
COMMENT ON COLUMN "crm_business_product"."business_price" IS '商机价格，单位：元';
COMMENT ON COLUMN "crm_business_product"."count" IS '数量';
COMMENT ON COLUMN "crm_business_product"."total_price" IS '总计价格，单位：元';
CREATE SEQUENCE IF NOT EXISTS crm_business_product_seq;
CREATE INDEX IF NOT EXISTS idx_crm_business_product_business_id ON "crm_business_product" ("business_id");
CREATE INDEX IF NOT EXISTS idx_crm_business_product_product_id ON "crm_business_product" ("product_id");

-- ===================== crm_business_status =====================
-- CRM 商机状态
DROP TABLE IF EXISTS "crm_business_status";
CREATE TABLE IF NOT EXISTS "crm_business_status" (
    "id"      bigint       NOT NULL,
    "type_id" bigint       NULL,
    "name"    varchar(128) NULL,
    "percent" int4         NULL,
    "sort"    int4         NULL,
    "creator" varchar(64)  DEFAULT '',
    "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater" varchar(64)  DEFAULT '',
    "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted" int2         NOT NULL DEFAULT 0,
    "tenant_id" bigint     NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "crm_business_status" IS 'CRM 商机状态';
COMMENT ON COLUMN "crm_business_status"."type_id" IS '状态类型编号';
COMMENT ON COLUMN "crm_business_status"."name" IS '状态名';
COMMENT ON COLUMN "crm_business_status"."percent" IS '赢单率，百分比';
COMMENT ON COLUMN "crm_business_status"."sort" IS '排序';
CREATE SEQUENCE IF NOT EXISTS crm_business_status_seq;
CREATE INDEX IF NOT EXISTS idx_crm_business_status_type_id ON "crm_business_status" ("type_id");

-- ===================== crm_business_status_type =====================
-- CRM 商机状态组
DROP TABLE IF EXISTS "crm_business_status_type";
CREATE TABLE IF NOT EXISTS "crm_business_status_type" (
    "id"      bigint       NOT NULL,
    "name"    varchar(128) NULL,
    "dept_ids" varchar(255) NULL,
    "creator" varchar(64)  DEFAULT '',
    "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater" varchar(64)  DEFAULT '',
    "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted" int2         NOT NULL DEFAULT 0,
    "tenant_id" bigint     NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "crm_business_status_type" IS 'CRM 商机状态组';
COMMENT ON COLUMN "crm_business_status_type"."name" IS '状态类型名';
COMMENT ON COLUMN "crm_business_status_type"."dept_ids" IS '使用的部门编号';
CREATE SEQUENCE IF NOT EXISTS crm_business_status_type_seq;

-- ===================== crm_clue =====================
-- CRM 线索
DROP TABLE IF EXISTS "crm_clue";
CREATE TABLE IF NOT EXISTS "crm_clue" (
    "id"                   bigint       NOT NULL,
    "name"                 varchar(128) NULL,
    "follow_up_status"    int2         NOT NULL DEFAULT 0,
    "contact_last_time"   timestamp    NULL,
    "contact_last_content" text        NULL,
    "contact_next_time"   timestamp    NULL,
    "owner_user_id"       bigint       NULL,
    "transform_status"    int2         NOT NULL DEFAULT 0,
    "customer_id"         bigint       NULL,
    "mobile"              varchar(20)  NULL,
    "telephone"           varchar(20)  NULL,
    "qq"                  varchar(32)  NULL,
    "wechat"              varchar(64)  NULL,
    "email"               varchar(50)  NULL,
    "area_id"             int4         NULL,
    "detail_address"      varchar(500) NULL,
    "industry_id"         int4         NULL,
    "level"               int4         NULL,
    "source"              int4         NULL,
    "remark"              varchar(255) NULL,
    "creator"             varchar(64)  DEFAULT '',
    "create_time"         timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"             varchar(64)  DEFAULT '',
    "update_time"         timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"             int2         NOT NULL DEFAULT 0,
    "tenant_id"           bigint       NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "crm_clue" IS 'CRM 线索';
COMMENT ON COLUMN "crm_clue"."name" IS '线索名称';
COMMENT ON COLUMN "crm_clue"."follow_up_status" IS '跟进状态';
COMMENT ON COLUMN "crm_clue"."contact_last_time" IS '最后跟进时间';
COMMENT ON COLUMN "crm_clue"."contact_last_content" IS '最后跟进内容';
COMMENT ON COLUMN "crm_clue"."contact_next_time" IS '下次联系时间';
COMMENT ON COLUMN "crm_clue"."owner_user_id" IS '负责人的用户编号';
COMMENT ON COLUMN "crm_clue"."transform_status" IS '转化状态';
COMMENT ON COLUMN "crm_clue"."customer_id" IS '客户编号';
COMMENT ON COLUMN "crm_clue"."mobile" IS '手机号';
COMMENT ON COLUMN "crm_clue"."telephone" IS '电话';
COMMENT ON COLUMN "crm_clue"."qq" IS 'QQ';
COMMENT ON COLUMN "crm_clue"."wechat" IS 'wechat';
COMMENT ON COLUMN "crm_clue"."email" IS 'email';
COMMENT ON COLUMN "crm_clue"."area_id" IS '所在地';
COMMENT ON COLUMN "crm_clue"."detail_address" IS '详细地址';
COMMENT ON COLUMN "crm_clue"."industry_id" IS '所属行业';
COMMENT ON COLUMN "crm_clue"."level" IS '客户等级';
COMMENT ON COLUMN "crm_clue"."source" IS '客户来源';
COMMENT ON COLUMN "crm_clue"."remark" IS '备注';
CREATE SEQUENCE IF NOT EXISTS crm_clue_seq;
CREATE INDEX IF NOT EXISTS idx_crm_clue_owner_user_id ON "crm_clue" ("owner_user_id");
CREATE INDEX IF NOT EXISTS idx_crm_clue_customer_id ON "crm_clue" ("customer_id");

-- ===================== crm_contact_business =====================
-- CRM 联系人与商机的关联
DROP TABLE IF EXISTS "crm_contact_business";
CREATE TABLE IF NOT EXISTS "crm_contact_business" (
    "id"          bigint       NOT NULL,
    "contact_id"  bigint       NULL,
    "business_id" bigint       NULL,
    "creator"     varchar(64)  DEFAULT '',
    "create_time" timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"     varchar(64)  DEFAULT '',
    "update_time" timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"     int2         NOT NULL DEFAULT 0,
    "tenant_id"   bigint       NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "crm_contact_business" IS 'CRM 联系人与商机的关联';
COMMENT ON COLUMN "crm_contact_business"."contact_id" IS '联系人编号';
COMMENT ON COLUMN "crm_contact_business"."business_id" IS '商机编号';
CREATE SEQUENCE IF NOT EXISTS crm_contact_business_seq;
CREATE INDEX IF NOT EXISTS idx_crm_contact_business_contact_id ON "crm_contact_business" ("contact_id");
CREATE INDEX IF NOT EXISTS idx_crm_contact_business_business_id ON "crm_contact_business" ("business_id");

-- ===================== crm_contact =====================
-- CRM 联系人
DROP TABLE IF EXISTS "crm_contact";
CREATE TABLE IF NOT EXISTS "crm_contact" (
    "id"                   bigint       NOT NULL,
    "name"                 varchar(128) NULL,
    "customer_id"          bigint       NULL,
    "contact_last_time"    timestamp    NULL,
    "contact_last_content" text        NULL,
    "contact_next_time"    timestamp    NULL,
    "owner_user_id"        bigint       NULL,
    "mobile"               varchar(20)  NULL,
    "telephone"            varchar(20)  NULL,
    "email"                varchar(50)  NULL,
    "qq"                   bigint       NULL,
    "wechat"               varchar(64)  NULL,
    "area_id"              int4         NULL,
    "detail_address"       varchar(500) NULL,
    "sex"                  int4         NULL,
    "master"               int2         NOT NULL DEFAULT 0,
    "post"                 varchar(64)  NULL,
    "parent_id"            bigint       NULL,
    "remark"               varchar(255) NULL,
    "creator"              varchar(64)  DEFAULT '',
    "create_time"          timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"              varchar(64)  DEFAULT '',
    "update_time"          timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"              int2         NOT NULL DEFAULT 0,
    "tenant_id"            bigint       NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "crm_contact" IS 'CRM 联系人';
COMMENT ON COLUMN "crm_contact"."name" IS '联系人姓名';
COMMENT ON COLUMN "crm_contact"."customer_id" IS '客户编号';
COMMENT ON COLUMN "crm_contact"."contact_last_time" IS '最后跟进时间';
COMMENT ON COLUMN "crm_contact"."contact_last_content" IS '最后跟进内容';
COMMENT ON COLUMN "crm_contact"."contact_next_time" IS '下次联系时间';
COMMENT ON COLUMN "crm_contact"."owner_user_id" IS '负责人用户编号';
COMMENT ON COLUMN "crm_contact"."mobile" IS '手机号';
COMMENT ON COLUMN "crm_contact"."telephone" IS '电话';
COMMENT ON COLUMN "crm_contact"."email" IS '电子邮箱';
COMMENT ON COLUMN "crm_contact"."qq" IS 'QQ';
COMMENT ON COLUMN "crm_contact"."wechat" IS '微信';
COMMENT ON COLUMN "crm_contact"."area_id" IS '所在地';
COMMENT ON COLUMN "crm_contact"."detail_address" IS '详细地址';
COMMENT ON COLUMN "crm_contact"."sex" IS '性别';
COMMENT ON COLUMN "crm_contact"."master" IS '是否关键决策人';
COMMENT ON COLUMN "crm_contact"."post" IS '职位';
COMMENT ON COLUMN "crm_contact"."parent_id" IS '直属上级';
COMMENT ON COLUMN "crm_contact"."remark" IS '备注';
CREATE SEQUENCE IF NOT EXISTS crm_contact_seq;
CREATE INDEX IF NOT EXISTS idx_crm_contact_customer_id ON "crm_contact" ("customer_id");
CREATE INDEX IF NOT EXISTS idx_crm_contact_owner_user_id ON "crm_contact" ("owner_user_id");
CREATE INDEX IF NOT EXISTS idx_crm_contact_parent_id ON "crm_contact" ("parent_id");

-- ===================== crm_contract_config =====================
-- CRM 合同配置
DROP TABLE IF EXISTS "crm_contract_config";
CREATE TABLE IF NOT EXISTS "crm_contract_config" (
    "id"            bigint       NOT NULL,
    "notify_enabled" int2        NOT NULL DEFAULT 0,
    "notify_days"   int4         NULL,
    "creator"       varchar(64)  DEFAULT '',
    "create_time"   timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"       varchar(64)  DEFAULT '',
    "update_time"   timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"       int2         NOT NULL DEFAULT 0,
    "tenant_id"     bigint       NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "crm_contract_config" IS 'CRM 合同配置';
COMMENT ON COLUMN "crm_contract_config"."notify_enabled" IS '是否开启提前提醒';
COMMENT ON COLUMN "crm_contract_config"."notify_days" IS '提前提醒天数';
CREATE SEQUENCE IF NOT EXISTS crm_contract_config_seq;

-- ===================== crm_contract =====================
-- CRM 合同
DROP TABLE IF EXISTS "crm_contract";
CREATE TABLE IF NOT EXISTS "crm_contract" (
    "id"                  bigint       NOT NULL,
    "name"                varchar(128) NULL,
    "no"                  varchar(64)  NULL,
    "customer_id"         bigint       NULL,
    "business_id"         bigint       NULL,
    "contact_last_time"   timestamp    NULL,
    "owner_user_id"       bigint       NULL,
    "process_instance_id" varchar(64)  NULL,
    "audit_status"        int4         NULL,
    "order_date"          timestamp    NULL,
    "start_time"          timestamp    NULL,
    "end_time"            timestamp    NULL,
    "total_product_price" numeric(24,6) NULL,
    "discount_percent"    numeric(24,6) NULL,
    "total_price"         numeric(24,6) NULL,
    "sign_contact_id"     bigint       NULL,
    "sign_user_id"        bigint       NULL,
    "remark"              varchar(255) NULL,
    "creator"             varchar(64)  DEFAULT '',
    "create_time"         timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"             varchar(64)  DEFAULT '',
    "update_time"         timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"             int2         NOT NULL DEFAULT 0,
    "tenant_id"           bigint       NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "crm_contract" IS 'CRM 合同';
COMMENT ON COLUMN "crm_contract"."name" IS '合同名称';
COMMENT ON COLUMN "crm_contract"."no" IS '合同编号';
COMMENT ON COLUMN "crm_contract"."customer_id" IS '客户编号';
COMMENT ON COLUMN "crm_contract"."business_id" IS '商机编号';
COMMENT ON COLUMN "crm_contract"."contact_last_time" IS '最后跟进时间';
COMMENT ON COLUMN "crm_contract"."owner_user_id" IS '负责人的用户编号';
COMMENT ON COLUMN "crm_contract"."process_instance_id" IS '工作流编号';
COMMENT ON COLUMN "crm_contract"."audit_status" IS '审批状态';
COMMENT ON COLUMN "crm_contract"."order_date" IS '下单日期';
COMMENT ON COLUMN "crm_contract"."start_time" IS '开始时间';
COMMENT ON COLUMN "crm_contract"."end_time" IS '结束时间';
COMMENT ON COLUMN "crm_contract"."total_product_price" IS '产品总金额，单位：元';
COMMENT ON COLUMN "crm_contract"."discount_percent" IS '整单折扣';
COMMENT ON COLUMN "crm_contract"."total_price" IS '合同总金额，单位：分';
COMMENT ON COLUMN "crm_contract"."sign_contact_id" IS '客户签约人';
COMMENT ON COLUMN "crm_contract"."sign_user_id" IS '公司签约人';
COMMENT ON COLUMN "crm_contract"."remark" IS '备注';
CREATE SEQUENCE IF NOT EXISTS crm_contract_seq;
CREATE UNIQUE INDEX IF NOT EXISTS idx_crm_contract_no ON "crm_contract" ("no", tenant_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_crm_contract_customer_id ON "crm_contract" ("customer_id");
CREATE INDEX IF NOT EXISTS idx_crm_contract_business_id ON "crm_contract" ("business_id");
CREATE INDEX IF NOT EXISTS idx_crm_contract_owner_user_id ON "crm_contract" ("owner_user_id");

-- ===================== crm_contract_product =====================
-- CRM 合同产品关联表
DROP TABLE IF EXISTS "crm_contract_product";
CREATE TABLE IF NOT EXISTS "crm_contract_product" (
    "id"             bigint       NOT NULL,
    "contract_id"    bigint       NULL,
    "product_id"     bigint       NULL,
    "product_price"  numeric(24,6) NULL,
    "contract_price" numeric(24,6) NULL,
    "count"          numeric(20,4) NULL,
    "total_price"    numeric(24,6) NULL,
    "creator"        varchar(64)  DEFAULT '',
    "create_time"    timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"        varchar(64)  DEFAULT '',
    "update_time"    timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"        int2         NOT NULL DEFAULT 0,
    "tenant_id"      bigint       NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "crm_contract_product" IS 'CRM 合同产品关联表';
COMMENT ON COLUMN "crm_contract_product"."contract_id" IS '合同编号';
COMMENT ON COLUMN "crm_contract_product"."product_id" IS '产品编号';
COMMENT ON COLUMN "crm_contract_product"."product_price" IS '产品单价，单位：元';
COMMENT ON COLUMN "crm_contract_product"."contract_price" IS '合同价格，单位：元';
COMMENT ON COLUMN "crm_contract_product"."count" IS '数量';
COMMENT ON COLUMN "crm_contract_product"."total_price" IS '总计价格，单位：元';
CREATE SEQUENCE IF NOT EXISTS crm_contract_product_seq;
CREATE INDEX IF NOT EXISTS idx_crm_contract_product_contract_id ON "crm_contract_product" ("contract_id");
CREATE INDEX IF NOT EXISTS idx_crm_contract_product_product_id ON "crm_contract_product" ("product_id");

-- ===================== crm_customer =====================
-- CRM 客户
DROP TABLE IF EXISTS "crm_customer";
CREATE TABLE IF NOT EXISTS "crm_customer" (
    "id"                   bigint       NOT NULL,
    "name"                 varchar(128) NULL,
    "follow_up_status"     int2         NOT NULL DEFAULT 0,
    "contact_last_time"    timestamp    NULL,
    "contact_last_content" text        NULL,
    "contact_next_time"    timestamp    NULL,
    "owner_user_id"        bigint       NULL,
    "owner_time"           timestamp    NULL,
    "lock_status"          int2         NOT NULL DEFAULT 0,
    "deal_status"          int2         NOT NULL DEFAULT 0,
    "mobile"               varchar(20)  NULL,
    "telephone"            varchar(20)  NULL,
    "qq"                   varchar(32)  NULL,
    "wechat"               varchar(64)  NULL,
    "email"                varchar(50)  NULL,
    "area_id"              int4         NULL,
    "detail_address"      varchar(500) NULL,
    "industry_id"          int4         NULL,
    "level"                int4         NULL,
    "source"               int4         NULL,
    "remark"               varchar(255) NULL,
    "creator"              varchar(64)  DEFAULT '',
    "create_time"          timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"              varchar(64)  DEFAULT '',
    "update_time"          timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"              int2         NOT NULL DEFAULT 0,
    "tenant_id"            bigint       NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "crm_customer" IS 'CRM 客户';
COMMENT ON COLUMN "crm_customer"."name" IS '客户名称';
COMMENT ON COLUMN "crm_customer"."follow_up_status" IS '跟进状态';
COMMENT ON COLUMN "crm_customer"."contact_last_time" IS '最后跟进时间';
COMMENT ON COLUMN "crm_customer"."contact_last_content" IS '最后跟进内容';
COMMENT ON COLUMN "crm_customer"."contact_next_time" IS '下次联系时间';
COMMENT ON COLUMN "crm_customer"."owner_user_id" IS '负责人的用户编号';
COMMENT ON COLUMN "crm_customer"."owner_time" IS '成为负责人的时间';
COMMENT ON COLUMN "crm_customer"."lock_status" IS '锁定状态';
COMMENT ON COLUMN "crm_customer"."deal_status" IS '成交状态';
COMMENT ON COLUMN "crm_customer"."mobile" IS '手机';
COMMENT ON COLUMN "crm_customer"."telephone" IS '电话';
COMMENT ON COLUMN "crm_customer"."qq" IS 'QQ';
COMMENT ON COLUMN "crm_customer"."wechat" IS 'wechat';
COMMENT ON COLUMN "crm_customer"."email" IS 'email';
COMMENT ON COLUMN "crm_customer"."area_id" IS '所在地';
COMMENT ON COLUMN "crm_customer"."detail_address" IS '详细地址';
COMMENT ON COLUMN "crm_customer"."industry_id" IS '所属行业';
COMMENT ON COLUMN "crm_customer"."level" IS '客户等级';
COMMENT ON COLUMN "crm_customer"."source" IS '客户来源';
COMMENT ON COLUMN "crm_customer"."remark" IS '备注';
CREATE SEQUENCE IF NOT EXISTS crm_customer_seq;
CREATE INDEX IF NOT EXISTS idx_crm_customer_owner_user_id ON "crm_customer" ("owner_user_id");

-- ===================== crm_customer_limit_config =====================
-- 客户限制配置
DROP TABLE IF EXISTS "crm_customer_limit_config";
CREATE TABLE IF NOT EXISTS "crm_customer_limit_config" (
    "id"                bigint       NOT NULL,
    "type"              int4         NULL,
    "user_ids"          varchar(255) NULL,
    "dept_ids"          varchar(255) NULL,
    "max_count"         int4         NULL,
    "deal_count_enabled" int2        NOT NULL DEFAULT 0,
    "creator"           varchar(64)  DEFAULT '',
    "create_time"       timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"           varchar(64)  DEFAULT '',
    "update_time"       timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"           int2         NOT NULL DEFAULT 0,
    "tenant_id"         bigint       NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "crm_customer_limit_config" IS '客户限制配置';
COMMENT ON COLUMN "crm_customer_limit_config"."type" IS '规则类型';
COMMENT ON COLUMN "crm_customer_limit_config"."user_ids" IS '规则适用人群';
COMMENT ON COLUMN "crm_customer_limit_config"."dept_ids" IS '规则适用部门';
COMMENT ON COLUMN "crm_customer_limit_config"."max_count" IS '数量上限';
COMMENT ON COLUMN "crm_customer_limit_config"."deal_count_enabled" IS '成交客户是否占有拥有客户数';
CREATE SEQUENCE IF NOT EXISTS crm_customer_limit_config_seq;

-- ===================== crm_customer_pool_config =====================
-- 客户公海配置
DROP TABLE IF EXISTS "crm_customer_pool_config";
CREATE TABLE IF NOT EXISTS "crm_customer_pool_config" (
    "id"                 bigint       NOT NULL,
    "enabled"            int2         NOT NULL DEFAULT 0,
    "contact_expire_days" int4        NULL,
    "deal_expire_days"   int4         NULL,
    "notify_enabled"     int2         NOT NULL DEFAULT 0,
    "notify_days"        int4         NULL,
    "creator"            varchar(64)  DEFAULT '',
    "create_time"        timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"            varchar(64)  DEFAULT '',
    "update_time"        timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"            int2         NOT NULL DEFAULT 0,
    "tenant_id"          bigint       NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "crm_customer_pool_config" IS '客户公海配置';
COMMENT ON COLUMN "crm_customer_pool_config"."enabled" IS '是否启用客户公海';
COMMENT ON COLUMN "crm_customer_pool_config"."contact_expire_days" IS '未跟进放入公海天数';
COMMENT ON COLUMN "crm_customer_pool_config"."deal_expire_days" IS '未成交放入公海天数';
COMMENT ON COLUMN "crm_customer_pool_config"."notify_enabled" IS '是否开启提前提醒';
COMMENT ON COLUMN "crm_customer_pool_config"."notify_days" IS '提前提醒天数';
CREATE SEQUENCE IF NOT EXISTS crm_customer_pool_config_seq;

-- ===================== crm_follow_up_record =====================
-- 跟进记录
DROP TABLE IF EXISTS "crm_follow_up_record";
CREATE TABLE IF NOT EXISTS "crm_follow_up_record" (
    "id"           bigint       NOT NULL,
    "biz_type"     int4         NULL,
    "biz_id"       bigint       NULL,
    "type"         int4         NULL,
    "content"      text         NULL,
    "next_time"    timestamp    NULL,
    "pic_urls"     text         NULL,
    "file_urls"    text         NULL,
    "business_ids" varchar(255) NULL,
    "contact_ids"  varchar(255) NULL,
    "creator"      varchar(64)  DEFAULT '',
    "create_time"  timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"      varchar(64)  DEFAULT '',
    "update_time"  timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"      int2         NOT NULL DEFAULT 0,
    "tenant_id"    bigint       NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "crm_follow_up_record" IS '跟进记录';
COMMENT ON COLUMN "crm_follow_up_record"."biz_type" IS '数据类型';
COMMENT ON COLUMN "crm_follow_up_record"."biz_id" IS '数据编号';
COMMENT ON COLUMN "crm_follow_up_record"."type" IS '跟进类型';
COMMENT ON COLUMN "crm_follow_up_record"."content" IS '跟进内容';
COMMENT ON COLUMN "crm_follow_up_record"."next_time" IS '下次联系时间';
COMMENT ON COLUMN "crm_follow_up_record"."pic_urls" IS '图片';
COMMENT ON COLUMN "crm_follow_up_record"."file_urls" IS '附件';
COMMENT ON COLUMN "crm_follow_up_record"."business_ids" IS '关联的商机编号数组';
COMMENT ON COLUMN "crm_follow_up_record"."contact_ids" IS '关联的联系人编号数组';
CREATE SEQUENCE IF NOT EXISTS crm_follow_up_seq;
CREATE INDEX IF NOT EXISTS idx_crm_follow_up_record_biz_type ON "crm_follow_up_record" ("biz_type");
CREATE INDEX IF NOT EXISTS idx_crm_follow_up_record_biz_id ON "crm_follow_up_record" ("biz_id");

-- ===================== crm_performance_config =====================
-- CRM 业绩目标
DROP TABLE IF EXISTS "crm_performance_config";
CREATE TABLE IF NOT EXISTS "crm_performance_config" (
    "id"                     bigint       NOT NULL,
    "biz_type"               int4         NULL,
    "object_id"              bigint       NULL,
    "object_type"            int4         NULL,
    "year"                   int4         NULL,
    "year_target_price"      numeric(24,6) NULL,
    "january_target_price"   numeric(24,6) NULL,
    "february_target_price"  numeric(24,6) NULL,
    "march_target_price"     numeric(24,6) NULL,
    "april_target_price"     numeric(24,6) NULL,
    "may_target_price"       numeric(24,6) NULL,
    "june_target_price"      numeric(24,6) NULL,
    "july_target_price"      numeric(24,6) NULL,
    "august_target_price"    numeric(24,6) NULL,
    "september_target_price" numeric(24,6) NULL,
    "october_target_price"   numeric(24,6) NULL,
    "november_target_price"  numeric(24,6) NULL,
    "december_target_price"  numeric(24,6) NULL,
    "creator"                varchar(64)  DEFAULT '',
    "create_time"            timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"                varchar(64)  DEFAULT '',
    "update_time"            timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"                int2         NOT NULL DEFAULT 0,
    "tenant_id"              bigint       NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "crm_performance_config" IS 'CRM 业绩目标';
COMMENT ON COLUMN "crm_performance_config"."biz_type" IS '目标类型';
COMMENT ON COLUMN "crm_performance_config"."object_id" IS '目标对象编号';
COMMENT ON COLUMN "crm_performance_config"."object_type" IS '目标对象类型';
COMMENT ON COLUMN "crm_performance_config"."year" IS '年份';
COMMENT ON COLUMN "crm_performance_config"."year_target_price" IS '年度目标金额';
COMMENT ON COLUMN "crm_performance_config"."january_target_price" IS '一月目标金额';
COMMENT ON COLUMN "crm_performance_config"."february_target_price" IS '二月目标金额';
COMMENT ON COLUMN "crm_performance_config"."march_target_price" IS '三月目标金额';
COMMENT ON COLUMN "crm_performance_config"."april_target_price" IS '四月目标金额';
COMMENT ON COLUMN "crm_performance_config"."may_target_price" IS '五月目标金额';
COMMENT ON COLUMN "crm_performance_config"."june_target_price" IS '六月目标金额';
COMMENT ON COLUMN "crm_performance_config"."july_target_price" IS '七月目标金额';
COMMENT ON COLUMN "crm_performance_config"."august_target_price" IS '八月目标金额';
COMMENT ON COLUMN "crm_performance_config"."september_target_price" IS '九月目标金额';
COMMENT ON COLUMN "crm_performance_config"."october_target_price" IS '十月目标金额';
COMMENT ON COLUMN "crm_performance_config"."november_target_price" IS '十一月目标金额';
COMMENT ON COLUMN "crm_performance_config"."december_target_price" IS '十二月目标金额';
CREATE SEQUENCE IF NOT EXISTS crm_performance_config_seq;

-- ===================== crm_owner_record =====================
-- CRM 负责人变更记录
DROP TABLE IF EXISTS "crm_owner_record";
CREATE TABLE IF NOT EXISTS "crm_owner_record" (
    "id"                bigint       NOT NULL,
    "biz_type"          int4         NULL,
    "biz_id"            bigint       NULL,
    "pre_owner_user_id" bigint       NULL,
    "post_owner_user_id" bigint      NULL,
    "creator"           varchar(64)  DEFAULT '',
    "create_time"       timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"           varchar(64)  DEFAULT '',
    "update_time"       timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"           int2         NOT NULL DEFAULT 0,
    "tenant_id"         bigint       NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "crm_owner_record" IS 'CRM 负责人变更记录';
COMMENT ON COLUMN "crm_owner_record"."biz_type" IS 'CRM 业务类型';
COMMENT ON COLUMN "crm_owner_record"."biz_id" IS 'CRM 业务编号';
COMMENT ON COLUMN "crm_owner_record"."pre_owner_user_id" IS '变更前负责人';
COMMENT ON COLUMN "crm_owner_record"."post_owner_user_id" IS '变更后负责人';
CREATE SEQUENCE IF NOT EXISTS crm_owner_record_seq;
CREATE INDEX IF NOT EXISTS idx_crm_owner_record_biz_type ON "crm_owner_record" ("biz_type");
CREATE INDEX IF NOT EXISTS idx_crm_owner_record_biz_id ON "crm_owner_record" ("biz_id");

-- ===================== crm_permission =====================
-- CRM 数据权限
DROP TABLE IF EXISTS "crm_permission";
CREATE TABLE IF NOT EXISTS "crm_permission" (
    "id"       bigint       NOT NULL,
    "biz_type" int4         NULL,
    "biz_id"   bigint       NULL,
    "user_id"  bigint       NULL,
    "level"    int4         NULL,
    "creator"  varchar(64)  DEFAULT '',
    "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"  varchar(64)  DEFAULT '',
    "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"  int2         NOT NULL DEFAULT 0,
    "tenant_id" bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "crm_permission" IS 'CRM 数据权限';
COMMENT ON COLUMN "crm_permission"."biz_type" IS '数据类型';
COMMENT ON COLUMN "crm_permission"."biz_id" IS '数据编号';
COMMENT ON COLUMN "crm_permission"."user_id" IS '用户编号';
COMMENT ON COLUMN "crm_permission"."level" IS '权限级别';
CREATE SEQUENCE IF NOT EXISTS crm_permission_seq;
CREATE INDEX IF NOT EXISTS idx_crm_permission_biz_type ON "crm_permission" ("biz_type");
CREATE INDEX IF NOT EXISTS idx_crm_permission_biz_id ON "crm_permission" ("biz_id");
CREATE INDEX IF NOT EXISTS idx_crm_permission_user_id ON "crm_permission" ("user_id");

-- ===================== crm_product_category =====================
-- 产品分类
DROP TABLE IF EXISTS "crm_product_category";
CREATE TABLE IF NOT EXISTS "crm_product_category" (
    "id"        bigint       NOT NULL,
    "name"      varchar(128) NULL,
    "parent_id" bigint       NULL,
    "creator"   varchar(64)  DEFAULT '',
    "create_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"   varchar(64)  DEFAULT '',
    "update_time" timestamp  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"   int2         NOT NULL DEFAULT 0,
    "tenant_id" bigint       NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "crm_product_category" IS '产品分类';
COMMENT ON COLUMN "crm_product_category"."name" IS '分类名称';
COMMENT ON COLUMN "crm_product_category"."parent_id" IS '父级编号';
CREATE SEQUENCE IF NOT EXISTS crm_product_category_seq;
CREATE INDEX IF NOT EXISTS idx_crm_product_category_parent_id ON "crm_product_category" ("parent_id");

-- ===================== crm_product =====================
-- CRM 产品
DROP TABLE IF EXISTS "crm_product";
CREATE TABLE IF NOT EXISTS "crm_product" (
    "id"           bigint       NOT NULL,
    "name"         varchar(128) NULL,
    "no"           varchar(64)  NULL,
    "unit"         int4         NULL,
    "price"        numeric(24,6) NULL,
    "status"       int4         NULL,
    "category_id"  bigint       NULL,
    "description"  text         NULL,
    "owner_user_id" bigint      NULL,
    "creator"      varchar(64)  DEFAULT '',
    "create_time"  timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"      varchar(64)  DEFAULT '',
    "update_time"  timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"      int2         NOT NULL DEFAULT 0,
    "tenant_id"    bigint       NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "crm_product" IS 'CRM 产品';
COMMENT ON COLUMN "crm_product"."name" IS '产品名称';
COMMENT ON COLUMN "crm_product"."no" IS '产品编码';
COMMENT ON COLUMN "crm_product"."unit" IS '单位';
COMMENT ON COLUMN "crm_product"."price" IS '价格，单位：元';
COMMENT ON COLUMN "crm_product"."status" IS '状态';
COMMENT ON COLUMN "crm_product"."category_id" IS '产品分类 ID';
COMMENT ON COLUMN "crm_product"."description" IS '产品描述';
COMMENT ON COLUMN "crm_product"."owner_user_id" IS '负责人的用户编号';
CREATE SEQUENCE IF NOT EXISTS crm_product_seq;
CREATE INDEX IF NOT EXISTS idx_crm_product_category_id ON "crm_product" ("category_id");
CREATE INDEX IF NOT EXISTS idx_crm_product_owner_user_id ON "crm_product" ("owner_user_id");

-- ===================== crm_receivable =====================
-- 回款
DROP TABLE IF EXISTS "crm_receivable";
CREATE TABLE IF NOT EXISTS "crm_receivable" (
    "id"                 bigint       NOT NULL,
    "no"                 varchar(64)  NULL,
    "plan_id"            bigint       NULL,
    "customer_id"        bigint       NULL,
    "contract_id"        bigint       NULL,
    "owner_user_id"      bigint       NULL,
    "return_time"        timestamp    NULL,
    "return_type"        int4         NULL,
    "price"              numeric(24,6) NULL,
    "remark"             varchar(255) NULL,
    "process_instance_id" varchar(64) NULL,
    "audit_status"       int4         NULL,
    "creator"            varchar(64)  DEFAULT '',
    "create_time"        timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"            varchar(64)  DEFAULT '',
    "update_time"        timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"            int2         NOT NULL DEFAULT 0,
    "tenant_id"          bigint       NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "crm_receivable" IS '回款';
COMMENT ON COLUMN "crm_receivable"."no" IS '回款编号';
COMMENT ON COLUMN "crm_receivable"."plan_id" IS '回款计划编号';
COMMENT ON COLUMN "crm_receivable"."customer_id" IS '客户编号';
COMMENT ON COLUMN "crm_receivable"."contract_id" IS '合同编号';
COMMENT ON COLUMN "crm_receivable"."owner_user_id" IS '负责人编号';
COMMENT ON COLUMN "crm_receivable"."return_time" IS '回款日期';
COMMENT ON COLUMN "crm_receivable"."return_type" IS '回款方式';
COMMENT ON COLUMN "crm_receivable"."price" IS '计划回款金额，单位：元';
COMMENT ON COLUMN "crm_receivable"."remark" IS '备注';
COMMENT ON COLUMN "crm_receivable"."process_instance_id" IS '工作流编号';
COMMENT ON COLUMN "crm_receivable"."audit_status" IS '审批状态';
CREATE SEQUENCE IF NOT EXISTS crm_receivable_seq;
CREATE INDEX IF NOT EXISTS idx_crm_receivable_customer_id ON "crm_receivable" ("customer_id");
CREATE INDEX IF NOT EXISTS idx_crm_receivable_contract_id ON "crm_receivable" ("contract_id");
CREATE INDEX IF NOT EXISTS idx_crm_receivable_plan_id ON "crm_receivable" ("plan_id");

-- ===================== crm_receivable_plan =====================
-- CRM 回款计划
DROP TABLE IF EXISTS "crm_receivable_plan";
CREATE TABLE IF NOT EXISTS "crm_receivable_plan" (
    "id"            bigint       NOT NULL,
    "period"        int4         NULL,
    "customer_id"   bigint       NULL,
    "contract_id"   bigint       NULL,
    "owner_user_id" bigint       NULL,
    "return_time"   timestamp    NULL,
    "return_type"   int4         NULL,
    "price"         numeric(24,6) NULL,
    "receivable_id" bigint       NULL,
    "remind_days"   int4         NULL,
    "remind_time"   timestamp    NULL,
    "remark"        varchar(255) NULL,
    "creator"       varchar(64)  DEFAULT '',
    "create_time"   timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"       varchar(64)  DEFAULT '',
    "update_time"   timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"       int2         NOT NULL DEFAULT 0,
    "tenant_id"     bigint       NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "crm_receivable_plan" IS 'CRM 回款计划';
COMMENT ON COLUMN "crm_receivable_plan"."period" IS '期数';
COMMENT ON COLUMN "crm_receivable_plan"."customer_id" IS '客户编号';
COMMENT ON COLUMN "crm_receivable_plan"."contract_id" IS '合同编号';
COMMENT ON COLUMN "crm_receivable_plan"."owner_user_id" IS '负责人编号';
COMMENT ON COLUMN "crm_receivable_plan"."return_time" IS '计划回款日期';
COMMENT ON COLUMN "crm_receivable_plan"."return_type" IS '计划回款类型';
COMMENT ON COLUMN "crm_receivable_plan"."price" IS '计划回款金额，单位：元';
COMMENT ON COLUMN "crm_receivable_plan"."receivable_id" IS '回款编号';
COMMENT ON COLUMN "crm_receivable_plan"."remind_days" IS '提前几天提醒';
COMMENT ON COLUMN "crm_receivable_plan"."remind_time" IS '提醒日期';
COMMENT ON COLUMN "crm_receivable_plan"."remark" IS '备注';
CREATE SEQUENCE IF NOT EXISTS crm_receivable_plan_seq;
CREATE INDEX IF NOT EXISTS idx_crm_receivable_plan_customer_id ON "crm_receivable_plan" ("customer_id");
CREATE INDEX IF NOT EXISTS idx_crm_receivable_plan_contract_id ON "crm_receivable_plan" ("contract_id");
CREATE INDEX IF NOT EXISTS idx_crm_receivable_plan_receivable_id ON "crm_receivable_plan" ("receivable_id");
