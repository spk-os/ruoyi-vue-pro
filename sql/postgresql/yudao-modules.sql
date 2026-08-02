
-- ===== yudao-module-member =====
/*
 Yudao Database Transfer Tool

 Source Server Type    : MySQL

 Target Server Type    : PostgreSQL

 Date: 2026-07-28 23:44:51
*/


-- ----------------------------
-- Table structure for dual
-- ----------------------------
-- DROP TABLE IF EXISTS dual;
CREATE TABLE IF NOT EXISTS dual
(
    id int2
);

COMMENT ON TABLE dual IS '数据库连接的表';

-- ----------------------------
-- Records of dual
-- ----------------------------
-- @formatter:off
-- @formatter:on

-- ----------------------------
-- Table structure for "member_user"
-- ----------------------------
-- DROP TABLE IF EXISTS "member_user";
CREATE TABLE IF NOT EXISTS "member_user" (
    "id" int8 NOT NULL,
  "nickname" varchar(30) NOT NULL DEFAULT '',
  "name" varchar(30) NULL,
  sex int2 NULL,
  birthday timestamp NULL,
  area_id int4 NULL,
  mark varchar(255) NULL,
  point int4 NULL DEFAULT 0,
  "avatar" varchar(255) NOT NULL DEFAULT '',
  "status" int2 NOT NULL,
  "mobile" varchar(11) NOT NULL,
  "email" varchar(50) NULL,
  "password" varchar(100) NOT NULL DEFAULT '',
  "register_ip" varchar(32) NOT NULL,
  "login_ip" varchar(50) NULL DEFAULT '',
  "login_date" timestamp NULL DEFAULT NULL,
  "tag_ids" varchar(255) NULL DEFAULT NULL,
  "level_id" int8 NULL DEFAULT NULL,
  "experience" int8 NULL DEFAULT NULL,
  "group_id" int8 NULL DEFAULT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "member_user"."id" IS '';
COMMENT ON COLUMN "member_user"."nickname" IS '';
COMMENT ON COLUMN "member_user"."name" IS '';
COMMENT ON COLUMN "member_user".sex IS '';
COMMENT ON COLUMN "member_user".birthday IS '';
COMMENT ON COLUMN "member_user".area_id IS '';
COMMENT ON COLUMN "member_user".mark IS '';
COMMENT ON COLUMN "member_user".point IS '';
COMMENT ON COLUMN "member_user"."avatar" IS '';
COMMENT ON COLUMN "member_user"."status" IS '';
COMMENT ON COLUMN "member_user"."mobile" IS '';
COMMENT ON COLUMN "member_user"."email" IS '';
COMMENT ON COLUMN "member_user"."password" IS '';
COMMENT ON COLUMN "member_user"."register_ip" IS '';
COMMENT ON COLUMN "member_user"."login_ip" IS '';
COMMENT ON COLUMN "member_user"."login_date" IS '';
COMMENT ON COLUMN "member_user"."tag_ids" IS '';
COMMENT ON COLUMN "member_user"."level_id" IS '';
COMMENT ON COLUMN "member_user"."experience" IS '';
COMMENT ON COLUMN "member_user"."group_id" IS '';
COMMENT ON COLUMN "member_user"."creator" IS '';
COMMENT ON COLUMN "member_user"."create_time" IS '';
COMMENT ON COLUMN "member_user"."updater" IS '';
COMMENT ON COLUMN "member_user"."update_time" IS '';
COMMENT ON COLUMN "member_user"."deleted" IS '';
COMMENT ON COLUMN "member_user"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "member_address"
-- ----------------------------
-- DROP TABLE IF EXISTS "member_address";
CREATE TABLE IF NOT EXISTS "member_address" (
    "id" int8 NOT NULL,
  "user_id" int8 NOT NULL,
  "name" varchar(10) NOT NULL,
  "mobile" varchar(20) NOT NULL,
  "area_id" int8 NOT NULL,
  "detail_address" varchar(250) NOT NULL,
  "default_status" int2 NOT NULL,
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "creator" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "updater" varchar(64) NULL DEFAULT '',
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "member_address"."id" IS '';
COMMENT ON COLUMN "member_address"."user_id" IS '';
COMMENT ON COLUMN "member_address"."name" IS '';
COMMENT ON COLUMN "member_address"."mobile" IS '';
COMMENT ON COLUMN "member_address"."area_id" IS '';
COMMENT ON COLUMN "member_address"."detail_address" IS '';
COMMENT ON COLUMN "member_address"."default_status" IS '';
COMMENT ON COLUMN "member_address"."create_time" IS '';
COMMENT ON COLUMN "member_address"."creator" IS '';
COMMENT ON COLUMN "member_address"."update_time" IS '';
COMMENT ON COLUMN "member_address"."deleted" IS '';
COMMENT ON COLUMN "member_address"."updater" IS '';

-- ----------------------------
-- Table structure for "member_tag"
-- ----------------------------
-- DROP TABLE IF EXISTS "member_tag";
CREATE TABLE IF NOT EXISTS "member_tag" (
    "id" int8 NOT NULL,
  "name" text NOT NULL,
  "creator" text NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" text NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "member_tag"."id" IS '';
COMMENT ON COLUMN "member_tag"."name" IS '';
COMMENT ON COLUMN "member_tag"."creator" IS '';
COMMENT ON COLUMN "member_tag"."create_time" IS '';
COMMENT ON COLUMN "member_tag"."updater" IS '';
COMMENT ON COLUMN "member_tag"."update_time" IS '';
COMMENT ON COLUMN "member_tag"."deleted" IS '';
COMMENT ON COLUMN "member_tag"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "member_level"
-- ----------------------------
-- DROP TABLE IF EXISTS "member_level";
CREATE TABLE IF NOT EXISTS "member_level" (
    "id" int8 NOT NULL,
  "name" text NOT NULL,
  "experience" int4 NOT NULL,
  "level" int4 NOT NULL,
  "discount_percent" int4 NOT NULL,
  "icon" text NOT NULL,
  "background_url" text NOT NULL,
  "creator" text NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" text NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  "status" int2 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "member_level"."id" IS '';
COMMENT ON COLUMN "member_level"."name" IS '';
COMMENT ON COLUMN "member_level"."experience" IS '';
COMMENT ON COLUMN "member_level"."level" IS '';
COMMENT ON COLUMN "member_level"."discount_percent" IS '';
COMMENT ON COLUMN "member_level"."icon" IS '';
COMMENT ON COLUMN "member_level"."background_url" IS '';
COMMENT ON COLUMN "member_level"."creator" IS '';
COMMENT ON COLUMN "member_level"."create_time" IS '';
COMMENT ON COLUMN "member_level"."updater" IS '';
COMMENT ON COLUMN "member_level"."update_time" IS '';
COMMENT ON COLUMN "member_level"."deleted" IS '';
COMMENT ON COLUMN "member_level"."tenant_id" IS '';
COMMENT ON COLUMN "member_level"."status" IS '';

-- ----------------------------
-- Table structure for "member_group"
-- ----------------------------
-- DROP TABLE IF EXISTS "member_group";
CREATE TABLE IF NOT EXISTS "member_group" (
    "id" int8 NOT NULL,
  "name" text NOT NULL,
  "remark" text NOT NULL,
  "status" int2 NOT NULL DEFAULT 0,
  "creator" text NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" text NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "member_group"."id" IS '';
COMMENT ON COLUMN "member_group"."name" IS '';
COMMENT ON COLUMN "member_group"."remark" IS '';
COMMENT ON COLUMN "member_group"."status" IS '';
COMMENT ON COLUMN "member_group"."creator" IS '';
COMMENT ON COLUMN "member_group"."create_time" IS '';
COMMENT ON COLUMN "member_group"."updater" IS '';
COMMENT ON COLUMN "member_group"."update_time" IS '';
COMMENT ON COLUMN "member_group"."deleted" IS '';
COMMENT ON COLUMN "member_group"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "member_brokerage_record"
-- ----------------------------
-- DROP TABLE IF EXISTS "member_brokerage_record";
CREATE TABLE IF NOT EXISTS "member_brokerage_record" (
    "id" int4 NOT NULL,
  "user_id" int8 NOT NULL,
  "biz_id" text NOT NULL,
  "biz_type" text NOT NULL,
  "title" text NOT NULL,
  "price" int4 NOT NULL,
  "total_price" int4 NOT NULL,
  "description" text NOT NULL,
  "status" text NOT NULL,
  "frozen_days" int4 NOT NULL,
  "unfreeze_time" text NULL,
  "creator" text NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" text NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "member_brokerage_record"."id" IS '';
COMMENT ON COLUMN "member_brokerage_record"."user_id" IS '';
COMMENT ON COLUMN "member_brokerage_record"."biz_id" IS '';
COMMENT ON COLUMN "member_brokerage_record"."biz_type" IS '';
COMMENT ON COLUMN "member_brokerage_record"."title" IS '';
COMMENT ON COLUMN "member_brokerage_record"."price" IS '';
COMMENT ON COLUMN "member_brokerage_record"."total_price" IS '';
COMMENT ON COLUMN "member_brokerage_record"."description" IS '';
COMMENT ON COLUMN "member_brokerage_record"."status" IS '';
COMMENT ON COLUMN "member_brokerage_record"."frozen_days" IS '';
COMMENT ON COLUMN "member_brokerage_record"."unfreeze_time" IS '';
COMMENT ON COLUMN "member_brokerage_record"."creator" IS '';
COMMENT ON COLUMN "member_brokerage_record"."create_time" IS '';
COMMENT ON COLUMN "member_brokerage_record"."updater" IS '';
COMMENT ON COLUMN "member_brokerage_record"."update_time" IS '';
COMMENT ON COLUMN "member_brokerage_record"."deleted" IS '';
COMMENT ON COLUMN "member_brokerage_record"."tenant_id" IS '';


-- ===== yudao-module-report =====
/*
 Yudao Database Transfer Tool

 Source Server Type    : MySQL

 Target Server Type    : PostgreSQL

 Date: 2026-07-28 23:44:51
*/


-- ----------------------------
-- Table structure for dual
-- ----------------------------
-- DROP TABLE IF EXISTS dual;
CREATE TABLE IF NOT EXISTS dual
(
    id int2
);

COMMENT ON TABLE dual IS '数据库连接的表';

-- ----------------------------
-- Records of dual
-- ----------------------------
-- @formatter:off
-- @formatter:on

-- ----------------------------
-- Table structure for "report_go_view_project"
-- ----------------------------
-- DROP TABLE IF EXISTS "report_go_view_project";
CREATE TABLE IF NOT EXISTS "report_go_view_project" (
    "id" int8 NOT NULL,
  "name" text NOT NULL,
  "pic_url" text NULL,
  "content" text NULL,
  "status" text NOT NULL,
  "remark" text NULL,
  "creator" text NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" text NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "report_go_view_project"."id" IS '';
COMMENT ON COLUMN "report_go_view_project"."name" IS '';
COMMENT ON COLUMN "report_go_view_project"."pic_url" IS '';
COMMENT ON COLUMN "report_go_view_project"."content" IS '';
COMMENT ON COLUMN "report_go_view_project"."status" IS '';
COMMENT ON COLUMN "report_go_view_project"."remark" IS '';
COMMENT ON COLUMN "report_go_view_project"."creator" IS '';
COMMENT ON COLUMN "report_go_view_project"."create_time" IS '';
COMMENT ON COLUMN "report_go_view_project"."updater" IS '';
COMMENT ON COLUMN "report_go_view_project"."update_time" IS '';
COMMENT ON COLUMN "report_go_view_project"."deleted" IS '';


-- ===== yudao-module-pay =====
/*
 Yudao Database Transfer Tool

 Source Server Type    : MySQL

 Target Server Type    : PostgreSQL

 Date: 2026-07-28 23:44:51
*/


-- ----------------------------
-- Table structure for dual
-- ----------------------------
-- DROP TABLE IF EXISTS dual;
CREATE TABLE IF NOT EXISTS dual
(
    id int2
);

COMMENT ON TABLE dual IS '数据库连接的表';

-- ----------------------------
-- Records of dual
-- ----------------------------
-- @formatter:off
-- @formatter:on

-- ----------------------------
-- Table structure for "pay_app"
-- ----------------------------
-- DROP TABLE IF EXISTS "pay_app";
CREATE TABLE IF NOT EXISTS "pay_app" (
    "id" int8 NOT NULL,
  "app_key" varchar(64) NOT NULL,
  "name" varchar(64) NOT NULL,
  "status" int2 NOT NULL,
  "remark" varchar(255) NULL DEFAULT NULL,
  order_notify_url varchar(1024) NOT NULL,
  refund_notify_url varchar(1024) NOT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "pay_app"."id" IS '';
COMMENT ON COLUMN "pay_app"."app_key" IS '';
COMMENT ON COLUMN "pay_app"."name" IS '';
COMMENT ON COLUMN "pay_app"."status" IS '';
COMMENT ON COLUMN "pay_app"."remark" IS '';
COMMENT ON COLUMN "pay_app".order_notify_url IS '';
COMMENT ON COLUMN "pay_app".refund_notify_url IS '';
COMMENT ON COLUMN "pay_app"."creator" IS '';
COMMENT ON COLUMN "pay_app"."create_time" IS '';
COMMENT ON COLUMN "pay_app"."updater" IS '';
COMMENT ON COLUMN "pay_app"."update_time" IS '';
COMMENT ON COLUMN "pay_app"."deleted" IS '';
COMMENT ON TABLE "pay_app" IS '支付应用';

-- ----------------------------
-- Table structure for "pay_channel"
-- ----------------------------
-- DROP TABLE IF EXISTS "pay_channel";
CREATE TABLE IF NOT EXISTS "pay_channel" (
    "id" int8 NOT NULL,
  "code" varchar(32) NOT NULL,
  "status" int2 NOT NULL,
  "remark" varchar(255) NULL DEFAULT NULL,
  "fee_rate" double precision NOT NULL DEFAULT 0,
  "app_id" int8 NOT NULL,
  "config" varchar(10240) NOT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "pay_channel"."id" IS '';
COMMENT ON COLUMN "pay_channel"."code" IS '';
COMMENT ON COLUMN "pay_channel"."status" IS '';
COMMENT ON COLUMN "pay_channel"."remark" IS '';
COMMENT ON COLUMN "pay_channel"."fee_rate" IS '';
COMMENT ON COLUMN "pay_channel"."app_id" IS '';
COMMENT ON COLUMN "pay_channel"."config" IS '';
COMMENT ON COLUMN "pay_channel"."creator" IS '';
COMMENT ON COLUMN "pay_channel"."create_time" IS '';
COMMENT ON COLUMN "pay_channel"."updater" IS '';
COMMENT ON COLUMN "pay_channel"."update_time" IS '';
COMMENT ON COLUMN "pay_channel"."deleted" IS '';
COMMENT ON COLUMN "pay_channel"."tenant_id" IS '';
COMMENT ON TABLE "pay_channel" IS '支付渠道';

-- ----------------------------
-- Table structure for pay_order
-- ----------------------------
-- DROP TABLE IF EXISTS pay_order;
CREATE TABLE IF NOT EXISTS pay_order (
    "id" int8 NOT NULL,
  app_id int8 NOT NULL,
  channel_id int8 NULL DEFAULT NULL,
  channel_code varchar(32) NULL DEFAULT NULL,
  merchant_order_id varchar(64) NOT NULL,
  subject varchar(32) NOT NULL,
  body varchar(128) NOT NULL,
  notify_url varchar(1024) NOT NULL,
  price int8 NOT NULL,
  channel_fee_rate double precision NULL DEFAULT 0,
  channel_fee_price int8 NULL DEFAULT 0,
  status int2 NOT NULL,
  user_ip varchar(50) NOT NULL,
  user_id int8 NULL DEFAULT NULL,
  user_type int2 NULL DEFAULT NULL,
  expire_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  success_time timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  notify_time timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  extension_id int8 NULL DEFAULT NULL,
  no varchar(64) NULL,
  refund_price int8 NOT NULL,
  channel_user_id varchar(255) NULL DEFAULT NULL,
  channel_order_no varchar(64) NULL DEFAULT NULL,
  creator varchar(64) NULL DEFAULT '',
  create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater varchar(64) NULL DEFAULT '',
  update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted int2 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN pay_order."id" IS '';
COMMENT ON COLUMN pay_order.app_id IS '';
COMMENT ON COLUMN pay_order.channel_id IS '';
COMMENT ON COLUMN pay_order.channel_code IS '';
COMMENT ON COLUMN pay_order.merchant_order_id IS '';
COMMENT ON COLUMN pay_order.subject IS '';
COMMENT ON COLUMN pay_order.body IS '';
COMMENT ON COLUMN pay_order.notify_url IS '';
COMMENT ON COLUMN pay_order.price IS '';
COMMENT ON COLUMN pay_order.channel_fee_rate IS '';
COMMENT ON COLUMN pay_order.channel_fee_price IS '';
COMMENT ON COLUMN pay_order.status IS '';
COMMENT ON COLUMN pay_order.user_ip IS '';
COMMENT ON COLUMN pay_order.user_id IS '';
COMMENT ON COLUMN pay_order.user_type IS '';
COMMENT ON COLUMN pay_order.expire_time IS '';
COMMENT ON COLUMN pay_order.success_time IS '';
COMMENT ON COLUMN pay_order.notify_time IS '';
COMMENT ON COLUMN pay_order.extension_id IS '';
COMMENT ON COLUMN pay_order.no IS '';
COMMENT ON COLUMN pay_order.refund_price IS '';
COMMENT ON COLUMN pay_order.channel_user_id IS '';
COMMENT ON COLUMN pay_order.channel_order_no IS '';
COMMENT ON COLUMN pay_order.creator IS '';
COMMENT ON COLUMN pay_order.create_time IS '';
COMMENT ON COLUMN pay_order.updater IS '';
COMMENT ON COLUMN pay_order.update_time IS '';
COMMENT ON COLUMN pay_order.deleted IS '';
COMMENT ON TABLE pay_order IS '支付订单';

-- ----------------------------
-- Table structure for pay_order_extension
-- ----------------------------
-- DROP TABLE IF EXISTS pay_order_extension;
CREATE TABLE IF NOT EXISTS pay_order_extension (
    "id" int8 NOT NULL,
  no varchar(64) NOT NULL,
  order_id int8 NOT NULL,
  channel_id int8 NOT NULL,
  channel_code varchar(32) NOT NULL,
  user_ip varchar(50) NULL DEFAULT NULL,
  status int2 NOT NULL,
  channel_extras varchar(1024) NULL DEFAULT NULL,
  channel_error_code varchar(64) NULL,
  channel_error_msg varchar(64) NULL,
  channel_notify_data varchar(1024) NULL,
  creator varchar(64) NULL DEFAULT '',
  create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater varchar(64) NULL DEFAULT '',
  update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted int2 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN pay_order_extension."id" IS '';
COMMENT ON COLUMN pay_order_extension.no IS '';
COMMENT ON COLUMN pay_order_extension.order_id IS '';
COMMENT ON COLUMN pay_order_extension.channel_id IS '';
COMMENT ON COLUMN pay_order_extension.channel_code IS '';
COMMENT ON COLUMN pay_order_extension.user_ip IS '';
COMMENT ON COLUMN pay_order_extension.status IS '';
COMMENT ON COLUMN pay_order_extension.channel_extras IS '';
COMMENT ON COLUMN pay_order_extension.channel_error_code IS '';
COMMENT ON COLUMN pay_order_extension.channel_error_msg IS '';
COMMENT ON COLUMN pay_order_extension.channel_notify_data IS '';
COMMENT ON COLUMN pay_order_extension.creator IS '';
COMMENT ON COLUMN pay_order_extension.create_time IS '';
COMMENT ON COLUMN pay_order_extension.updater IS '';
COMMENT ON COLUMN pay_order_extension.update_time IS '';
COMMENT ON COLUMN pay_order_extension.deleted IS '';
COMMENT ON TABLE pay_order_extension IS '支付订单拓展';

-- ----------------------------
-- Table structure for pay_refund
-- ----------------------------
-- DROP TABLE IF EXISTS pay_refund;
CREATE TABLE IF NOT EXISTS pay_refund (
    "id" int8 NOT NULL,
  no varchar(64) NOT NULL,
  app_id int8 NOT NULL,
  channel_id int8 NOT NULL,
  channel_code varchar(32) NOT NULL,
  order_id int8 NOT NULL,
  order_no varchar(64) NOT NULL,
  merchant_order_id varchar(64) NOT NULL,
  merchant_refund_id varchar(64) NOT NULL,
  notify_url varchar(1024) NOT NULL,
  status int2 NOT NULL,
  pay_price int8 NOT NULL,
  refund_price int8 NOT NULL,
  reason varchar(256) NOT NULL,
  user_ip varchar(50) NULL DEFAULT NULL,
  user_id int8 NULL DEFAULT NULL,
  user_type int2 NULL DEFAULT NULL,
  channel_order_no varchar(64) NOT NULL,
  channel_refund_no varchar(64) NULL DEFAULT NULL,
  success_time timestamp NULL DEFAULT NULL,
  channel_error_code varchar(128) NULL DEFAULT NULL,
  channel_error_msg varchar(256) NULL DEFAULT NULL,
  channel_notify_data varchar(1024) NULL,
  creator varchar(64) NULL DEFAULT '',
  create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater varchar(64) NULL DEFAULT '',
  update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted int2 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN pay_refund."id" IS '';
COMMENT ON COLUMN pay_refund.no IS '';
COMMENT ON COLUMN pay_refund.app_id IS '';
COMMENT ON COLUMN pay_refund.channel_id IS '';
COMMENT ON COLUMN pay_refund.channel_code IS '';
COMMENT ON COLUMN pay_refund.order_id IS '';
COMMENT ON COLUMN pay_refund.order_no IS '';
COMMENT ON COLUMN pay_refund.merchant_order_id IS '';
COMMENT ON COLUMN pay_refund.merchant_refund_id IS '';
COMMENT ON COLUMN pay_refund.notify_url IS '';
COMMENT ON COLUMN pay_refund.status IS '';
COMMENT ON COLUMN pay_refund.pay_price IS '';
COMMENT ON COLUMN pay_refund.refund_price IS '';
COMMENT ON COLUMN pay_refund.reason IS '';
COMMENT ON COLUMN pay_refund.user_ip IS '';
COMMENT ON COLUMN pay_refund.user_id IS '';
COMMENT ON COLUMN pay_refund.user_type IS '';
COMMENT ON COLUMN pay_refund.channel_order_no IS '';
COMMENT ON COLUMN pay_refund.channel_refund_no IS '';
COMMENT ON COLUMN pay_refund.success_time IS '';
COMMENT ON COLUMN pay_refund.channel_error_code IS '';
COMMENT ON COLUMN pay_refund.channel_error_msg IS '';
COMMENT ON COLUMN pay_refund.channel_notify_data IS '';
COMMENT ON COLUMN pay_refund.creator IS '';
COMMENT ON COLUMN pay_refund.create_time IS '';
COMMENT ON COLUMN pay_refund.updater IS '';
COMMENT ON COLUMN pay_refund.update_time IS '';
COMMENT ON COLUMN pay_refund.deleted IS '';
COMMENT ON TABLE pay_refund IS '退款订单';

-- ----------------------------
-- Table structure for pay_notify_task
-- ----------------------------
-- DROP TABLE IF EXISTS pay_notify_task;
CREATE TABLE IF NOT EXISTS pay_notify_task (
    "id" int8 NOT NULL,
  app_id int8 NOT NULL,
  type int2 NOT NULL,
  data_id int8 NOT NULL,
  merchant_order_id varchar(64) NOT NULL,
  status int2 NOT NULL,
  next_notify_time timestamp NULL DEFAULT NULL,
  last_execute_time timestamp NULL DEFAULT NULL,
  notify_times int4 NOT NULL,
  max_notify_times int4 NOT NULL,
  notify_url varchar(1024) NOT NULL,
  creator varchar(64) NULL DEFAULT '',
  create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater varchar(64) NULL DEFAULT '',
  update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted int2 NOT NULL DEFAULT 0,
  tenant_id int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN pay_notify_task."id" IS '';
COMMENT ON COLUMN pay_notify_task.app_id IS '';
COMMENT ON COLUMN pay_notify_task.type IS '';
COMMENT ON COLUMN pay_notify_task.data_id IS '';
COMMENT ON COLUMN pay_notify_task.merchant_order_id IS '';
COMMENT ON COLUMN pay_notify_task.status IS '';
COMMENT ON COLUMN pay_notify_task.next_notify_time IS '';
COMMENT ON COLUMN pay_notify_task.last_execute_time IS '';
COMMENT ON COLUMN pay_notify_task.notify_times IS '';
COMMENT ON COLUMN pay_notify_task.max_notify_times IS '';
COMMENT ON COLUMN pay_notify_task.notify_url IS '';
COMMENT ON COLUMN pay_notify_task.creator IS '';
COMMENT ON COLUMN pay_notify_task.create_time IS '';
COMMENT ON COLUMN pay_notify_task.updater IS '';
COMMENT ON COLUMN pay_notify_task.update_time IS '';
COMMENT ON COLUMN pay_notify_task.deleted IS '';
COMMENT ON COLUMN pay_notify_task.tenant_id IS '';
COMMENT ON TABLE pay_notify_task IS '支付通知任务';

-- ----------------------------
-- Table structure for pay_notify_log
-- ----------------------------
-- DROP TABLE IF EXISTS pay_notify_log;
CREATE TABLE IF NOT EXISTS pay_notify_log (
    "id" int8 NOT NULL,
  task_id int8 NOT NULL,
  notify_times int4 NOT NULL,
  response varchar(1024) NOT NULL,
  status int2 NOT NULL,
  creator varchar(64) NULL DEFAULT '',
  create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater varchar(64) NULL DEFAULT '',
  update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted int2 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN pay_notify_log."id" IS '';
COMMENT ON COLUMN pay_notify_log.task_id IS '';
COMMENT ON COLUMN pay_notify_log.notify_times IS '';
COMMENT ON COLUMN pay_notify_log.response IS '';
COMMENT ON COLUMN pay_notify_log.status IS '';
COMMENT ON COLUMN pay_notify_log.creator IS '';
COMMENT ON COLUMN pay_notify_log.create_time IS '';
COMMENT ON COLUMN pay_notify_log.updater IS '';
COMMENT ON COLUMN pay_notify_log.update_time IS '';
COMMENT ON COLUMN pay_notify_log.deleted IS '';
COMMENT ON TABLE pay_notify_log IS '支付通知日志';

-- ----------------------------
-- Table structure for pay_transfer
-- ----------------------------
-- DROP TABLE IF EXISTS pay_transfer;
CREATE TABLE IF NOT EXISTS pay_transfer (
    "id" int8 NOT NULL,
  no varchar(64) NOT NULL,
  app_id int8 NOT NULL,
  channel_id int8 NOT NULL,
  channel_code varchar(32) NOT NULL,
  user_id int8 NULL DEFAULT NULL,
  user_type int2 NULL DEFAULT NULL,
  merchant_transfer_id varchar(64) NOT NULL,
  price int8 NOT NULL,
  subject varchar(256) NOT NULL,
  user_account varchar(256) NOT NULL,
  user_name varchar(64) NULL DEFAULT NULL,
  status int2 NOT NULL,
  notify_url varchar(1024) NULL DEFAULT NULL,
  user_ip varchar(50) NULL DEFAULT NULL,
  channel_transfer_no varchar(64) NULL DEFAULT NULL,
  success_time timestamp NULL DEFAULT NULL,
  channel_error_code varchar(128) NULL DEFAULT NULL,
  channel_error_msg varchar(256) NULL DEFAULT NULL,
  channel_notify_data varchar(1024) NULL DEFAULT NULL,
  channel_extras varchar(1024) NULL DEFAULT NULL,
  channel_package_info varchar(1024) NULL DEFAULT NULL,
  creator varchar(64) NULL DEFAULT '',
  create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater varchar(64) NULL DEFAULT '',
  update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted int2 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN pay_transfer."id" IS '';
COMMENT ON COLUMN pay_transfer.no IS '';
COMMENT ON COLUMN pay_transfer.app_id IS '';
COMMENT ON COLUMN pay_transfer.channel_id IS '';
COMMENT ON COLUMN pay_transfer.channel_code IS '';
COMMENT ON COLUMN pay_transfer.user_id IS '';
COMMENT ON COLUMN pay_transfer.user_type IS '';
COMMENT ON COLUMN pay_transfer.merchant_transfer_id IS '';
COMMENT ON COLUMN pay_transfer.price IS '';
COMMENT ON COLUMN pay_transfer.subject IS '';
COMMENT ON COLUMN pay_transfer.user_account IS '';
COMMENT ON COLUMN pay_transfer.user_name IS '';
COMMENT ON COLUMN pay_transfer.status IS '';
COMMENT ON COLUMN pay_transfer.notify_url IS '';
COMMENT ON COLUMN pay_transfer.user_ip IS '';
COMMENT ON COLUMN pay_transfer.channel_transfer_no IS '';
COMMENT ON COLUMN pay_transfer.success_time IS '';
COMMENT ON COLUMN pay_transfer.channel_error_code IS '';
COMMENT ON COLUMN pay_transfer.channel_error_msg IS '';
COMMENT ON COLUMN pay_transfer.channel_notify_data IS '';
COMMENT ON COLUMN pay_transfer.channel_extras IS '';
COMMENT ON COLUMN pay_transfer.channel_package_info IS '';
COMMENT ON COLUMN pay_transfer.creator IS '';
COMMENT ON COLUMN pay_transfer.create_time IS '';
COMMENT ON COLUMN pay_transfer.updater IS '';
COMMENT ON COLUMN pay_transfer.update_time IS '';
COMMENT ON COLUMN pay_transfer.deleted IS '';
COMMENT ON TABLE pay_transfer IS '转账单';


-- ===== yudao-module-iot/yudao-module-iot-biz =====
/*
 Yudao Database Transfer Tool

 Source Server Type    : MySQL

 Target Server Type    : PostgreSQL

 Date: 2026-07-28 23:44:51
*/


-- ----------------------------
-- Table structure for dual
-- ----------------------------
-- DROP TABLE IF EXISTS dual;
CREATE TABLE IF NOT EXISTS dual
(
    id int2
);

COMMENT ON TABLE dual IS '数据库连接的表';

-- ----------------------------
-- Records of dual
-- ----------------------------
-- @formatter:off
-- @formatter:on

-- ----------------------------
-- Table structure for "iot_scene_rule"
-- ----------------------------
-- DROP TABLE IF EXISTS "iot_scene_rule";
CREATE TABLE IF NOT EXISTS "iot_scene_rule" (
    "id" int8 NOT NULL,
  "name" varchar(255) NOT NULL DEFAULT '',
  "description" varchar(500) NULL DEFAULT NULL,
  "status" int2 NOT NULL DEFAULT 0,
  "triggers" text NULL,
  "actions" text NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "iot_scene_rule"."id" IS '';
COMMENT ON COLUMN "iot_scene_rule"."name" IS '';
COMMENT ON COLUMN "iot_scene_rule"."description" IS '';
COMMENT ON COLUMN "iot_scene_rule"."status" IS '';
COMMENT ON COLUMN "iot_scene_rule"."triggers" IS '';
COMMENT ON COLUMN "iot_scene_rule"."actions" IS '';
COMMENT ON COLUMN "iot_scene_rule"."creator" IS '';
COMMENT ON COLUMN "iot_scene_rule"."create_time" IS '';
COMMENT ON COLUMN "iot_scene_rule"."updater" IS '';
COMMENT ON COLUMN "iot_scene_rule"."update_time" IS '';
COMMENT ON COLUMN "iot_scene_rule"."deleted" IS '';
COMMENT ON COLUMN "iot_scene_rule"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "iot_product"
-- ----------------------------
-- DROP TABLE IF EXISTS "iot_product";
CREATE TABLE IF NOT EXISTS "iot_product" (
    "id" int8 NOT NULL,
  "name" varchar(255) NOT NULL DEFAULT '',
  "product_key" varchar(100) NOT NULL DEFAULT '',
  "protocol_type" int2 NOT NULL DEFAULT 0,
  "category_id" int8 NULL DEFAULT NULL,
  "description" varchar(500) NULL DEFAULT NULL,
  "data_format" int2 NOT NULL DEFAULT 0,
  "device_type" int2 NOT NULL DEFAULT 0,
  "net_type" int2 NOT NULL DEFAULT 0,
  "validate_type" int2 NOT NULL DEFAULT 0,
  "status" int2 NOT NULL DEFAULT 0,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "iot_product"."id" IS '';
COMMENT ON COLUMN "iot_product"."name" IS '';
COMMENT ON COLUMN "iot_product"."product_key" IS '';
COMMENT ON COLUMN "iot_product"."protocol_type" IS '';
COMMENT ON COLUMN "iot_product"."category_id" IS '';
COMMENT ON COLUMN "iot_product"."description" IS '';
COMMENT ON COLUMN "iot_product"."data_format" IS '';
COMMENT ON COLUMN "iot_product"."device_type" IS '';
COMMENT ON COLUMN "iot_product"."net_type" IS '';
COMMENT ON COLUMN "iot_product"."validate_type" IS '';
COMMENT ON COLUMN "iot_product"."status" IS '';
COMMENT ON COLUMN "iot_product"."creator" IS '';
COMMENT ON COLUMN "iot_product"."create_time" IS '';
COMMENT ON COLUMN "iot_product"."updater" IS '';
COMMENT ON COLUMN "iot_product"."update_time" IS '';
COMMENT ON COLUMN "iot_product"."deleted" IS '';
COMMENT ON COLUMN "iot_product"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "iot_device"
-- ----------------------------
-- DROP TABLE IF EXISTS "iot_device";
CREATE TABLE IF NOT EXISTS "iot_device" (
    "id" int8 NOT NULL,
  "device_name" varchar(255) NOT NULL DEFAULT '',
  "product_id" int8 NOT NULL,
  "device_key" varchar(100) NOT NULL DEFAULT '',
  "device_secret" varchar(100) NOT NULL DEFAULT '',
  "nickname" varchar(255) NULL DEFAULT NULL,
  "status" int2 NOT NULL DEFAULT 0,
  "status_last_update_time" timestamp NULL DEFAULT NULL,
  "last_online_time" timestamp NULL DEFAULT NULL,
  "last_offline_time" timestamp NULL DEFAULT NULL,
  "active_time" timestamp NULL DEFAULT NULL,
  "ip" varchar(50) NULL DEFAULT NULL,
  "firmware_version" varchar(50) NULL DEFAULT NULL,
  "device_type" int2 NOT NULL DEFAULT 0,
  "gateway_id" int8 NULL DEFAULT NULL,
  "sub_device_count" int4 NOT NULL DEFAULT 0,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "iot_device"."id" IS '';
COMMENT ON COLUMN "iot_device"."device_name" IS '';
COMMENT ON COLUMN "iot_device"."product_id" IS '';
COMMENT ON COLUMN "iot_device"."device_key" IS '';
COMMENT ON COLUMN "iot_device"."device_secret" IS '';
COMMENT ON COLUMN "iot_device"."nickname" IS '';
COMMENT ON COLUMN "iot_device"."status" IS '';
COMMENT ON COLUMN "iot_device"."status_last_update_time" IS '';
COMMENT ON COLUMN "iot_device"."last_online_time" IS '';
COMMENT ON COLUMN "iot_device"."last_offline_time" IS '';
COMMENT ON COLUMN "iot_device"."active_time" IS '';
COMMENT ON COLUMN "iot_device"."ip" IS '';
COMMENT ON COLUMN "iot_device"."firmware_version" IS '';
COMMENT ON COLUMN "iot_device"."device_type" IS '';
COMMENT ON COLUMN "iot_device"."gateway_id" IS '';
COMMENT ON COLUMN "iot_device"."sub_device_count" IS '';
COMMENT ON COLUMN "iot_device"."creator" IS '';
COMMENT ON COLUMN "iot_device"."create_time" IS '';
COMMENT ON COLUMN "iot_device"."updater" IS '';
COMMENT ON COLUMN "iot_device"."update_time" IS '';
COMMENT ON COLUMN "iot_device"."deleted" IS '';
COMMENT ON COLUMN "iot_device"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "iot_thing_model"
-- ----------------------------
-- DROP TABLE IF EXISTS "iot_thing_model";
CREATE TABLE IF NOT EXISTS "iot_thing_model" (
    "id" int8 NOT NULL,
  "product_id" int8 NOT NULL,
  "identifier" varchar(100) NOT NULL DEFAULT '',
  "name" varchar(255) NOT NULL DEFAULT '',
  "description" varchar(500) NULL DEFAULT NULL,
  "type" int2 NOT NULL DEFAULT 1,
  "property" text NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "iot_thing_model"."id" IS '';
COMMENT ON COLUMN "iot_thing_model"."product_id" IS '';
COMMENT ON COLUMN "iot_thing_model"."identifier" IS '';
COMMENT ON COLUMN "iot_thing_model"."name" IS '';
COMMENT ON COLUMN "iot_thing_model"."description" IS '';
COMMENT ON COLUMN "iot_thing_model"."type" IS '';
COMMENT ON COLUMN "iot_thing_model"."property" IS '';
COMMENT ON COLUMN "iot_thing_model"."creator" IS '';
COMMENT ON COLUMN "iot_thing_model"."create_time" IS '';
COMMENT ON COLUMN "iot_thing_model"."updater" IS '';
COMMENT ON COLUMN "iot_thing_model"."update_time" IS '';
COMMENT ON COLUMN "iot_thing_model"."deleted" IS '';
COMMENT ON COLUMN "iot_thing_model"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "iot_device_data"
-- ----------------------------
-- DROP TABLE IF EXISTS "iot_device_data";
CREATE TABLE IF NOT EXISTS "iot_device_data" (
    "id" int8 NOT NULL,
  "device_id" int8 NOT NULL,
  "product_id" int8 NOT NULL,
  "identifier" varchar(100) NOT NULL DEFAULT '',
  "type" int2 NOT NULL DEFAULT 1,
  "data" text NULL,
  "ts" int8 NOT NULL DEFAULT 0,
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "iot_device_data"."id" IS '';
COMMENT ON COLUMN "iot_device_data"."device_id" IS '';
COMMENT ON COLUMN "iot_device_data"."product_id" IS '';
COMMENT ON COLUMN "iot_device_data"."identifier" IS '';
COMMENT ON COLUMN "iot_device_data"."type" IS '';
COMMENT ON COLUMN "iot_device_data"."data" IS '';
COMMENT ON COLUMN "iot_device_data"."ts" IS '';
COMMENT ON COLUMN "iot_device_data"."create_time" IS '';

-- ----------------------------
-- Table structure for "iot_alert_config"
-- ----------------------------
-- DROP TABLE IF EXISTS "iot_alert_config";
CREATE TABLE IF NOT EXISTS "iot_alert_config" (
    "id" int8 NOT NULL,
  "name" varchar(255) NOT NULL DEFAULT '',
  "product_id" int8 NOT NULL,
  "device_id" int8 NULL DEFAULT NULL,
  "rule_id" int8 NULL DEFAULT NULL,
  "status" int2 NOT NULL DEFAULT 0,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "iot_alert_config"."id" IS '';
COMMENT ON COLUMN "iot_alert_config"."name" IS '';
COMMENT ON COLUMN "iot_alert_config"."product_id" IS '';
COMMENT ON COLUMN "iot_alert_config"."device_id" IS '';
COMMENT ON COLUMN "iot_alert_config"."rule_id" IS '';
COMMENT ON COLUMN "iot_alert_config"."status" IS '';
COMMENT ON COLUMN "iot_alert_config"."creator" IS '';
COMMENT ON COLUMN "iot_alert_config"."create_time" IS '';
COMMENT ON COLUMN "iot_alert_config"."updater" IS '';
COMMENT ON COLUMN "iot_alert_config"."update_time" IS '';
COMMENT ON COLUMN "iot_alert_config"."deleted" IS '';
COMMENT ON COLUMN "iot_alert_config"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "iot_alert_record"
-- ----------------------------
-- DROP TABLE IF EXISTS "iot_alert_record";
CREATE TABLE IF NOT EXISTS "iot_alert_record" (
    "id" int8 NOT NULL,
  "alert_config_id" int8 NOT NULL,
  "alert_name" varchar(255) NOT NULL DEFAULT '',
  "product_id" int8 NOT NULL,
  "device_id" int8 NULL DEFAULT NULL,
  "rule_id" int8 NULL DEFAULT NULL,
  "alert_data" text NULL,
  "alert_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deal_status" int2 NOT NULL DEFAULT 0,
  "deal_time" timestamp NULL DEFAULT NULL,
  "deal_user_id" int8 NULL DEFAULT NULL,
  "deal_remark" varchar(500) NULL DEFAULT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "iot_alert_record"."id" IS '';
COMMENT ON COLUMN "iot_alert_record"."alert_config_id" IS '';
COMMENT ON COLUMN "iot_alert_record"."alert_name" IS '';
COMMENT ON COLUMN "iot_alert_record"."product_id" IS '';
COMMENT ON COLUMN "iot_alert_record"."device_id" IS '';
COMMENT ON COLUMN "iot_alert_record"."rule_id" IS '';
COMMENT ON COLUMN "iot_alert_record"."alert_data" IS '';
COMMENT ON COLUMN "iot_alert_record"."alert_time" IS '';
COMMENT ON COLUMN "iot_alert_record"."deal_status" IS '';
COMMENT ON COLUMN "iot_alert_record"."deal_time" IS '';
COMMENT ON COLUMN "iot_alert_record"."deal_user_id" IS '';
COMMENT ON COLUMN "iot_alert_record"."deal_remark" IS '';
COMMENT ON COLUMN "iot_alert_record"."creator" IS '';
COMMENT ON COLUMN "iot_alert_record"."create_time" IS '';
COMMENT ON COLUMN "iot_alert_record"."updater" IS '';
COMMENT ON COLUMN "iot_alert_record"."update_time" IS '';
COMMENT ON COLUMN "iot_alert_record"."deleted" IS '';
COMMENT ON COLUMN "iot_alert_record"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "iot_ota_firmware"
-- ----------------------------
-- DROP TABLE IF EXISTS "iot_ota_firmware";
CREATE TABLE IF NOT EXISTS "iot_ota_firmware" (
    "id" int8 NOT NULL,
  "name" varchar(255) NOT NULL DEFAULT '',
  "product_id" int8 NOT NULL,
  "version" varchar(50) NOT NULL DEFAULT '',
  "description" varchar(500) NULL DEFAULT NULL,
  "file_url" varchar(500) NULL DEFAULT NULL,
  "file_size" int8 NOT NULL DEFAULT 0,
  "status" int2 NOT NULL DEFAULT 0,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "iot_ota_firmware"."id" IS '';
COMMENT ON COLUMN "iot_ota_firmware"."name" IS '';
COMMENT ON COLUMN "iot_ota_firmware"."product_id" IS '';
COMMENT ON COLUMN "iot_ota_firmware"."version" IS '';
COMMENT ON COLUMN "iot_ota_firmware"."description" IS '';
COMMENT ON COLUMN "iot_ota_firmware"."file_url" IS '';
COMMENT ON COLUMN "iot_ota_firmware"."file_size" IS '';
COMMENT ON COLUMN "iot_ota_firmware"."status" IS '';
COMMENT ON COLUMN "iot_ota_firmware"."creator" IS '';
COMMENT ON COLUMN "iot_ota_firmware"."create_time" IS '';
COMMENT ON COLUMN "iot_ota_firmware"."updater" IS '';
COMMENT ON COLUMN "iot_ota_firmware"."update_time" IS '';
COMMENT ON COLUMN "iot_ota_firmware"."deleted" IS '';
COMMENT ON COLUMN "iot_ota_firmware"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "iot_ota_task"
-- ----------------------------
-- DROP TABLE IF EXISTS "iot_ota_task";
CREATE TABLE IF NOT EXISTS "iot_ota_task" (
    "id" int8 NOT NULL,
  "name" varchar(255) NOT NULL DEFAULT '',
  "firmware_id" int8 NOT NULL,
  "product_id" int8 NOT NULL,
  "upgrade_type" int2 NOT NULL DEFAULT 0,
  "status" int2 NOT NULL DEFAULT 0,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "iot_ota_task"."id" IS '';
COMMENT ON COLUMN "iot_ota_task"."name" IS '';
COMMENT ON COLUMN "iot_ota_task"."firmware_id" IS '';
COMMENT ON COLUMN "iot_ota_task"."product_id" IS '';
COMMENT ON COLUMN "iot_ota_task"."upgrade_type" IS '';
COMMENT ON COLUMN "iot_ota_task"."status" IS '';
COMMENT ON COLUMN "iot_ota_task"."creator" IS '';
COMMENT ON COLUMN "iot_ota_task"."create_time" IS '';
COMMENT ON COLUMN "iot_ota_task"."updater" IS '';
COMMENT ON COLUMN "iot_ota_task"."update_time" IS '';
COMMENT ON COLUMN "iot_ota_task"."deleted" IS '';
COMMENT ON COLUMN "iot_ota_task"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "iot_ota_record"
-- ----------------------------
-- DROP TABLE IF EXISTS "iot_ota_record";
CREATE TABLE IF NOT EXISTS "iot_ota_record" (
    "id" int8 NOT NULL,
  "task_id" int8 NOT NULL,
  "firmware_id" int8 NOT NULL,
  "device_id" int8 NOT NULL,
  "status" int2 NOT NULL DEFAULT 0,
  "progress" int4 NOT NULL DEFAULT 0,
  "error_msg" varchar(500) NULL DEFAULT NULL,
  "start_time" timestamp NULL DEFAULT NULL,
  "end_time" timestamp NULL DEFAULT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "iot_ota_record"."id" IS '';
COMMENT ON COLUMN "iot_ota_record"."task_id" IS '';
COMMENT ON COLUMN "iot_ota_record"."firmware_id" IS '';
COMMENT ON COLUMN "iot_ota_record"."device_id" IS '';
COMMENT ON COLUMN "iot_ota_record"."status" IS '';
COMMENT ON COLUMN "iot_ota_record"."progress" IS '';
COMMENT ON COLUMN "iot_ota_record"."error_msg" IS '';
COMMENT ON COLUMN "iot_ota_record"."start_time" IS '';
COMMENT ON COLUMN "iot_ota_record"."end_time" IS '';
COMMENT ON COLUMN "iot_ota_record"."creator" IS '';
COMMENT ON COLUMN "iot_ota_record"."create_time" IS '';
COMMENT ON COLUMN "iot_ota_record"."updater" IS '';
COMMENT ON COLUMN "iot_ota_record"."update_time" IS '';
COMMENT ON COLUMN "iot_ota_record"."deleted" IS '';
COMMENT ON COLUMN "iot_ota_record"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "iot_data_rule"
-- ----------------------------
-- DROP TABLE IF EXISTS "iot_data_rule";
CREATE TABLE IF NOT EXISTS "iot_data_rule" (
    "id" int8 NOT NULL,
  "name" varchar(128) NOT NULL,
  "description" varchar(256) NULL DEFAULT '',
  "status" int4 NOT NULL,
  "source_configs" varchar(10000) NOT NULL,
  "sink_ids" varchar(512) NOT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "iot_data_rule"."id" IS '';
COMMENT ON COLUMN "iot_data_rule"."name" IS '';
COMMENT ON COLUMN "iot_data_rule"."description" IS '';
COMMENT ON COLUMN "iot_data_rule"."status" IS '';
COMMENT ON COLUMN "iot_data_rule"."source_configs" IS '';
COMMENT ON COLUMN "iot_data_rule"."sink_ids" IS '';
COMMENT ON COLUMN "iot_data_rule"."creator" IS '';
COMMENT ON COLUMN "iot_data_rule"."create_time" IS '';
COMMENT ON COLUMN "iot_data_rule"."updater" IS '';
COMMENT ON COLUMN "iot_data_rule"."update_time" IS '';
COMMENT ON COLUMN "iot_data_rule"."deleted" IS '';
COMMENT ON COLUMN "iot_data_rule"."tenant_id" IS '';


-- ===== yudao-module-mes =====
/*
 Yudao Database Transfer Tool

 Source Server Type    : MySQL

 Target Server Type    : PostgreSQL

 Date: 2026-07-28 23:44:51
*/


-- ----------------------------
-- Table structure for dual
-- ----------------------------
-- DROP TABLE IF EXISTS dual;
CREATE TABLE IF NOT EXISTS dual
(
    id int2
);

COMMENT ON TABLE dual IS '数据库连接的表';

-- ----------------------------
-- Records of dual
-- ----------------------------
-- @formatter:off
-- @formatter:on

-- ----------------------------
-- Table structure for "mes_cal_plan"
-- ----------------------------
-- DROP TABLE IF EXISTS "mes_cal_plan";
CREATE TABLE IF NOT EXISTS "mes_cal_plan" (
    "id" int8 NOT NULL,
  "code" varchar(64) NULL DEFAULT NULL,
  "name" varchar(255) NULL DEFAULT NULL,
  "calendar_type" int2 NULL DEFAULT NULL,
  "start_date" timestamp NULL DEFAULT NULL,
  "end_date" timestamp NULL DEFAULT NULL,
  "shift_type" int2 NULL DEFAULT NULL,
  "shift_method" int2 NULL DEFAULT NULL,
  "shift_count" int4 NULL DEFAULT NULL,
  "status" int2 NULL DEFAULT NULL,
  "remark" varchar(500) NULL DEFAULT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "mes_cal_plan"."id" IS '';
COMMENT ON COLUMN "mes_cal_plan"."code" IS '';
COMMENT ON COLUMN "mes_cal_plan"."name" IS '';
COMMENT ON COLUMN "mes_cal_plan"."calendar_type" IS '';
COMMENT ON COLUMN "mes_cal_plan"."start_date" IS '';
COMMENT ON COLUMN "mes_cal_plan"."end_date" IS '';
COMMENT ON COLUMN "mes_cal_plan"."shift_type" IS '';
COMMENT ON COLUMN "mes_cal_plan"."shift_method" IS '';
COMMENT ON COLUMN "mes_cal_plan"."shift_count" IS '';
COMMENT ON COLUMN "mes_cal_plan"."status" IS '';
COMMENT ON COLUMN "mes_cal_plan"."remark" IS '';
COMMENT ON COLUMN "mes_cal_plan"."creator" IS '';
COMMENT ON COLUMN "mes_cal_plan"."create_time" IS '';
COMMENT ON COLUMN "mes_cal_plan"."updater" IS '';
COMMENT ON COLUMN "mes_cal_plan"."update_time" IS '';
COMMENT ON COLUMN "mes_cal_plan"."deleted" IS '';
COMMENT ON COLUMN "mes_cal_plan"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "mes_cal_plan_shift"
-- ----------------------------
-- DROP TABLE IF EXISTS "mes_cal_plan_shift";
CREATE TABLE IF NOT EXISTS "mes_cal_plan_shift" (
    "id" int8 NOT NULL,
  "plan_id" int8 NULL DEFAULT NULL,
  "sort" int4 NULL DEFAULT NULL,
  "name" varchar(64) NULL DEFAULT NULL,
  "start_time" varchar(10) NULL DEFAULT NULL,
  "end_time" varchar(10) NULL DEFAULT NULL,
  "remark" varchar(500) NULL DEFAULT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "mes_cal_plan_shift"."id" IS '';
COMMENT ON COLUMN "mes_cal_plan_shift"."plan_id" IS '';
COMMENT ON COLUMN "mes_cal_plan_shift"."sort" IS '';
COMMENT ON COLUMN "mes_cal_plan_shift"."name" IS '';
COMMENT ON COLUMN "mes_cal_plan_shift"."start_time" IS '';
COMMENT ON COLUMN "mes_cal_plan_shift"."end_time" IS '';
COMMENT ON COLUMN "mes_cal_plan_shift"."remark" IS '';
COMMENT ON COLUMN "mes_cal_plan_shift"."creator" IS '';
COMMENT ON COLUMN "mes_cal_plan_shift"."create_time" IS '';
COMMENT ON COLUMN "mes_cal_plan_shift"."updater" IS '';
COMMENT ON COLUMN "mes_cal_plan_shift"."update_time" IS '';
COMMENT ON COLUMN "mes_cal_plan_shift"."deleted" IS '';
COMMENT ON COLUMN "mes_cal_plan_shift"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "mes_cal_plan_team"
-- ----------------------------
-- DROP TABLE IF EXISTS "mes_cal_plan_team";
CREATE TABLE IF NOT EXISTS "mes_cal_plan_team" (
    "id" int8 NOT NULL,
  "plan_id" int8 NULL DEFAULT NULL,
  "team_id" int8 NULL DEFAULT NULL,
  "remark" varchar(500) NULL DEFAULT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "mes_cal_plan_team"."id" IS '';
COMMENT ON COLUMN "mes_cal_plan_team"."plan_id" IS '';
COMMENT ON COLUMN "mes_cal_plan_team"."team_id" IS '';
COMMENT ON COLUMN "mes_cal_plan_team"."remark" IS '';
COMMENT ON COLUMN "mes_cal_plan_team"."creator" IS '';
COMMENT ON COLUMN "mes_cal_plan_team"."create_time" IS '';
COMMENT ON COLUMN "mes_cal_plan_team"."updater" IS '';
COMMENT ON COLUMN "mes_cal_plan_team"."update_time" IS '';
COMMENT ON COLUMN "mes_cal_plan_team"."deleted" IS '';
COMMENT ON COLUMN "mes_cal_plan_team"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "mes_cal_team_shift"
-- ----------------------------
-- DROP TABLE IF EXISTS "mes_cal_team_shift";
CREATE TABLE IF NOT EXISTS "mes_cal_team_shift" (
    "id" int8 NOT NULL,
  "plan_id" int8 NULL DEFAULT NULL,
  "team_id" int8 NULL DEFAULT NULL,
  "shift_id" int8 NULL DEFAULT NULL,
  "day" timestamp NULL DEFAULT NULL,
  "sort" int4 NULL DEFAULT NULL,
  "remark" varchar(500) NULL DEFAULT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "mes_cal_team_shift"."id" IS '';
COMMENT ON COLUMN "mes_cal_team_shift"."plan_id" IS '';
COMMENT ON COLUMN "mes_cal_team_shift"."team_id" IS '';
COMMENT ON COLUMN "mes_cal_team_shift"."shift_id" IS '';
COMMENT ON COLUMN "mes_cal_team_shift"."day" IS '';
COMMENT ON COLUMN "mes_cal_team_shift"."sort" IS '';
COMMENT ON COLUMN "mes_cal_team_shift"."remark" IS '';
COMMENT ON COLUMN "mes_cal_team_shift"."creator" IS '';
COMMENT ON COLUMN "mes_cal_team_shift"."create_time" IS '';
COMMENT ON COLUMN "mes_cal_team_shift"."updater" IS '';
COMMENT ON COLUMN "mes_cal_team_shift"."update_time" IS '';
COMMENT ON COLUMN "mes_cal_team_shift"."deleted" IS '';
COMMENT ON COLUMN "mes_cal_team_shift"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "mes_md_auto_code_rule"
-- ----------------------------
-- DROP TABLE IF EXISTS "mes_md_auto_code_rule";
CREATE TABLE IF NOT EXISTS "mes_md_auto_code_rule" (
    "id" int8 NOT NULL,
  "code" varchar(64) NOT NULL,
  "name" varchar(64) NOT NULL,
  "description" varchar(255) NULL DEFAULT NULL,
  "max_length" int4 NULL DEFAULT NULL,
  "padded" int2 NOT NULL DEFAULT 0,
  "padded_char" varchar(1) NULL DEFAULT NULL,
  "padded_method" int2 NULL DEFAULT NULL,
  "status" int2 NOT NULL,
  "remark" varchar(500) NULL DEFAULT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "mes_md_auto_code_rule"."id" IS '';
COMMENT ON COLUMN "mes_md_auto_code_rule"."code" IS '';
COMMENT ON COLUMN "mes_md_auto_code_rule"."name" IS '';
COMMENT ON COLUMN "mes_md_auto_code_rule"."description" IS '';
COMMENT ON COLUMN "mes_md_auto_code_rule"."max_length" IS '';
COMMENT ON COLUMN "mes_md_auto_code_rule"."padded" IS '';
COMMENT ON COLUMN "mes_md_auto_code_rule"."padded_char" IS '';
COMMENT ON COLUMN "mes_md_auto_code_rule"."padded_method" IS '';
COMMENT ON COLUMN "mes_md_auto_code_rule"."status" IS '';
COMMENT ON COLUMN "mes_md_auto_code_rule"."remark" IS '';
COMMENT ON COLUMN "mes_md_auto_code_rule"."creator" IS '';
COMMENT ON COLUMN "mes_md_auto_code_rule"."create_time" IS '';
COMMENT ON COLUMN "mes_md_auto_code_rule"."updater" IS '';
COMMENT ON COLUMN "mes_md_auto_code_rule"."update_time" IS '';
COMMENT ON COLUMN "mes_md_auto_code_rule"."deleted" IS '';
COMMENT ON COLUMN "mes_md_auto_code_rule"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "mes_md_auto_code_part"
-- ----------------------------
-- DROP TABLE IF EXISTS "mes_md_auto_code_part";
CREATE TABLE IF NOT EXISTS "mes_md_auto_code_part" (
    "id" int8 NOT NULL,
  "rule_id" int8 NOT NULL,
  "sort" int4 NOT NULL,
  "type" int2 NOT NULL,
  "length" int4 NOT NULL,
  "date_format" varchar(20) NULL DEFAULT NULL,
  "fix_character" varchar(64) NULL DEFAULT NULL,
  "serial_start_no" int4 NULL DEFAULT NULL,
  "serial_step" int4 NULL DEFAULT NULL,
  "cycle_flag" int2 NULL DEFAULT 0,
  "cycle_method" int2 NULL DEFAULT NULL,
  "remark" varchar(500) NULL DEFAULT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "mes_md_auto_code_part"."id" IS '';
COMMENT ON COLUMN "mes_md_auto_code_part"."rule_id" IS '';
COMMENT ON COLUMN "mes_md_auto_code_part"."sort" IS '';
COMMENT ON COLUMN "mes_md_auto_code_part"."type" IS '';
COMMENT ON COLUMN "mes_md_auto_code_part"."length" IS '';
COMMENT ON COLUMN "mes_md_auto_code_part"."date_format" IS '';
COMMENT ON COLUMN "mes_md_auto_code_part"."fix_character" IS '';
COMMENT ON COLUMN "mes_md_auto_code_part"."serial_start_no" IS '';
COMMENT ON COLUMN "mes_md_auto_code_part"."serial_step" IS '';
COMMENT ON COLUMN "mes_md_auto_code_part"."cycle_flag" IS '';
COMMENT ON COLUMN "mes_md_auto_code_part"."cycle_method" IS '';
COMMENT ON COLUMN "mes_md_auto_code_part"."remark" IS '';
COMMENT ON COLUMN "mes_md_auto_code_part"."creator" IS '';
COMMENT ON COLUMN "mes_md_auto_code_part"."create_time" IS '';
COMMENT ON COLUMN "mes_md_auto_code_part"."updater" IS '';
COMMENT ON COLUMN "mes_md_auto_code_part"."update_time" IS '';
COMMENT ON COLUMN "mes_md_auto_code_part"."deleted" IS '';
COMMENT ON COLUMN "mes_md_auto_code_part"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "mes_md_auto_code_record"
-- ----------------------------
-- DROP TABLE IF EXISTS "mes_md_auto_code_record";
CREATE TABLE IF NOT EXISTS "mes_md_auto_code_record" (
    "id" int8 NOT NULL,
  "rule_id" int8 NOT NULL,
  "result" varchar(64) NULL DEFAULT NULL,
  "serial_no" int8 NULL DEFAULT NULL,
  "input_char" varchar(64) NULL DEFAULT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "mes_md_auto_code_record"."id" IS '';
COMMENT ON COLUMN "mes_md_auto_code_record"."rule_id" IS '';
COMMENT ON COLUMN "mes_md_auto_code_record"."result" IS '';
COMMENT ON COLUMN "mes_md_auto_code_record"."serial_no" IS '';
COMMENT ON COLUMN "mes_md_auto_code_record"."input_char" IS '';
COMMENT ON COLUMN "mes_md_auto_code_record"."creator" IS '';
COMMENT ON COLUMN "mes_md_auto_code_record"."create_time" IS '';
COMMENT ON COLUMN "mes_md_auto_code_record"."updater" IS '';
COMMENT ON COLUMN "mes_md_auto_code_record"."update_time" IS '';
COMMENT ON COLUMN "mes_md_auto_code_record"."deleted" IS '';
COMMENT ON COLUMN "mes_md_auto_code_record"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "mes_wm_arrival_notice"
-- ----------------------------
-- DROP TABLE IF EXISTS "mes_wm_arrival_notice";
CREATE TABLE IF NOT EXISTS "mes_wm_arrival_notice" (
    "id" int8 NOT NULL,
  "code" varchar(64) NULL DEFAULT NULL,
  "name" varchar(255) NULL DEFAULT NULL,
  "purchase_order_code" varchar(64) NULL DEFAULT NULL,
  "vendor_id" int8 NULL DEFAULT NULL,
  "arrival_date" timestamp NULL DEFAULT NULL,
  "contact_name" varchar(64) NULL DEFAULT NULL,
  "contact_telephone" varchar(64) NULL DEFAULT NULL,
  "status" int2 NULL DEFAULT NULL,
  "remark" varchar(500) NULL DEFAULT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "mes_wm_arrival_notice"."id" IS '';
COMMENT ON COLUMN "mes_wm_arrival_notice"."code" IS '';
COMMENT ON COLUMN "mes_wm_arrival_notice"."name" IS '';
COMMENT ON COLUMN "mes_wm_arrival_notice"."purchase_order_code" IS '';
COMMENT ON COLUMN "mes_wm_arrival_notice"."vendor_id" IS '';
COMMENT ON COLUMN "mes_wm_arrival_notice"."arrival_date" IS '';
COMMENT ON COLUMN "mes_wm_arrival_notice"."contact_name" IS '';
COMMENT ON COLUMN "mes_wm_arrival_notice"."contact_telephone" IS '';
COMMENT ON COLUMN "mes_wm_arrival_notice"."status" IS '';
COMMENT ON COLUMN "mes_wm_arrival_notice"."remark" IS '';
COMMENT ON COLUMN "mes_wm_arrival_notice"."creator" IS '';
COMMENT ON COLUMN "mes_wm_arrival_notice"."create_time" IS '';
COMMENT ON COLUMN "mes_wm_arrival_notice"."updater" IS '';
COMMENT ON COLUMN "mes_wm_arrival_notice"."update_time" IS '';
COMMENT ON COLUMN "mes_wm_arrival_notice"."deleted" IS '';
COMMENT ON COLUMN "mes_wm_arrival_notice"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "mes_wm_outsource_receipt"
-- ----------------------------
-- DROP TABLE IF EXISTS "mes_wm_outsource_receipt";
CREATE TABLE IF NOT EXISTS "mes_wm_outsource_receipt" (
    "id" int8 NOT NULL,
  "code" varchar(64) NULL DEFAULT NULL,
  "name" varchar(255) NULL DEFAULT NULL,
  "work_order_id" int8 NULL DEFAULT NULL,
  "vendor_id" int8 NULL DEFAULT NULL,
  "receipt_date" timestamp NULL DEFAULT NULL,
  "status" int2 NULL DEFAULT NULL,
  "remark" varchar(500) NULL DEFAULT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "mes_wm_outsource_receipt"."id" IS '';
COMMENT ON COLUMN "mes_wm_outsource_receipt"."code" IS '';
COMMENT ON COLUMN "mes_wm_outsource_receipt"."name" IS '';
COMMENT ON COLUMN "mes_wm_outsource_receipt"."work_order_id" IS '';
COMMENT ON COLUMN "mes_wm_outsource_receipt"."vendor_id" IS '';
COMMENT ON COLUMN "mes_wm_outsource_receipt"."receipt_date" IS '';
COMMENT ON COLUMN "mes_wm_outsource_receipt"."status" IS '';
COMMENT ON COLUMN "mes_wm_outsource_receipt"."remark" IS '';
COMMENT ON COLUMN "mes_wm_outsource_receipt"."creator" IS '';
COMMENT ON COLUMN "mes_wm_outsource_receipt"."create_time" IS '';
COMMENT ON COLUMN "mes_wm_outsource_receipt"."updater" IS '';
COMMENT ON COLUMN "mes_wm_outsource_receipt"."update_time" IS '';
COMMENT ON COLUMN "mes_wm_outsource_receipt"."deleted" IS '';
COMMENT ON COLUMN "mes_wm_outsource_receipt"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "mes_qc_iqc"
-- ----------------------------
-- DROP TABLE IF EXISTS "mes_qc_iqc";
CREATE TABLE IF NOT EXISTS "mes_qc_iqc" (
    "id" int8 NOT NULL,
  "code" varchar(64) NULL DEFAULT NULL,
  "name" varchar(255) NULL DEFAULT NULL,
  "template_id" int8 NULL DEFAULT NULL,
  "source_doc_type" int4 NULL DEFAULT NULL,
  "source_doc_id" int8 NULL DEFAULT NULL,
  "source_line_id" int8 NULL DEFAULT NULL,
  "source_doc_code" varchar(64) NULL DEFAULT NULL,
  "vendor_id" int8 NULL DEFAULT NULL,
  "vendor_batch" varchar(64) NULL DEFAULT NULL,
  "item_id" int8 NULL DEFAULT NULL,
  "received_quantity" numeric(24,6) NULL DEFAULT NULL,
  "check_quantity" numeric(24,6) NULL DEFAULT NULL,
  "qualified_quantity" numeric(24,6) NULL DEFAULT NULL,
  "unqualified_quantity" numeric(24,6) NULL DEFAULT NULL,
  "critical_rate" numeric(10,2) NULL DEFAULT NULL,
  "major_rate" numeric(10,2) NULL DEFAULT NULL,
  "minor_rate" numeric(10,2) NULL DEFAULT NULL,
  "critical_quantity" int4 NULL DEFAULT NULL,
  "major_quantity" int4 NULL DEFAULT NULL,
  "minor_quantity" int4 NULL DEFAULT NULL,
  "check_result" int2 NULL DEFAULT NULL,
  "receive_date" timestamp NULL DEFAULT NULL,
  "inspect_date" timestamp NULL DEFAULT NULL,
  "inspector_user_id" int8 NULL DEFAULT NULL,
  "status" int2 NULL DEFAULT NULL,
  "remark" varchar(500) NULL DEFAULT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "mes_qc_iqc"."id" IS '';
COMMENT ON COLUMN "mes_qc_iqc"."code" IS '';
COMMENT ON COLUMN "mes_qc_iqc"."name" IS '';
COMMENT ON COLUMN "mes_qc_iqc"."template_id" IS '';
COMMENT ON COLUMN "mes_qc_iqc"."source_doc_type" IS '';
COMMENT ON COLUMN "mes_qc_iqc"."source_doc_id" IS '';
COMMENT ON COLUMN "mes_qc_iqc"."source_line_id" IS '';
COMMENT ON COLUMN "mes_qc_iqc"."source_doc_code" IS '';
COMMENT ON COLUMN "mes_qc_iqc"."vendor_id" IS '';
COMMENT ON COLUMN "mes_qc_iqc"."vendor_batch" IS '';
COMMENT ON COLUMN "mes_qc_iqc"."item_id" IS '';
COMMENT ON COLUMN "mes_qc_iqc"."received_quantity" IS '';
COMMENT ON COLUMN "mes_qc_iqc"."check_quantity" IS '';
COMMENT ON COLUMN "mes_qc_iqc"."qualified_quantity" IS '';
COMMENT ON COLUMN "mes_qc_iqc"."unqualified_quantity" IS '';
COMMENT ON COLUMN "mes_qc_iqc"."critical_rate" IS '';
COMMENT ON COLUMN "mes_qc_iqc"."major_rate" IS '';
COMMENT ON COLUMN "mes_qc_iqc"."minor_rate" IS '';
COMMENT ON COLUMN "mes_qc_iqc"."critical_quantity" IS '';
COMMENT ON COLUMN "mes_qc_iqc"."major_quantity" IS '';
COMMENT ON COLUMN "mes_qc_iqc"."minor_quantity" IS '';
COMMENT ON COLUMN "mes_qc_iqc"."check_result" IS '';
COMMENT ON COLUMN "mes_qc_iqc"."receive_date" IS '';
COMMENT ON COLUMN "mes_qc_iqc"."inspect_date" IS '';
COMMENT ON COLUMN "mes_qc_iqc"."inspector_user_id" IS '';
COMMENT ON COLUMN "mes_qc_iqc"."status" IS '';
COMMENT ON COLUMN "mes_qc_iqc"."remark" IS '';
COMMENT ON COLUMN "mes_qc_iqc"."creator" IS '';
COMMENT ON COLUMN "mes_qc_iqc"."create_time" IS '';
COMMENT ON COLUMN "mes_qc_iqc"."updater" IS '';
COMMENT ON COLUMN "mes_qc_iqc"."update_time" IS '';
COMMENT ON COLUMN "mes_qc_iqc"."deleted" IS '';
COMMENT ON COLUMN "mes_qc_iqc"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "mes_qc_ipqc"
-- ----------------------------
-- DROP TABLE IF EXISTS "mes_qc_ipqc";
CREATE TABLE IF NOT EXISTS "mes_qc_ipqc" (
    "id" int8 NOT NULL,
  "code" varchar(64) NULL DEFAULT NULL,
  "name" varchar(500) NULL DEFAULT NULL,
  "type" int2 NULL DEFAULT NULL,
  "template_id" int8 NULL DEFAULT NULL,
  "source_doc_type" int4 NULL DEFAULT NULL,
  "source_doc_id" int8 NULL DEFAULT NULL,
  "source_line_id" int8 NULL DEFAULT NULL,
  "source_doc_code" varchar(64) NULL DEFAULT NULL,
  "work_order_id" int8 NULL DEFAULT NULL,
  "task_id" int8 NULL DEFAULT NULL,
  "workstation_id" int8 NULL DEFAULT NULL,
  "process_id" int8 NULL DEFAULT NULL,
  "item_id" int8 NULL DEFAULT NULL,
  "check_quantity" numeric(14,2) NULL DEFAULT NULL,
  "qualified_quantity" numeric(14,2) NULL DEFAULT NULL,
  "unqualified_quantity" numeric(14,2) NULL DEFAULT NULL,
  "labor_scrap_quantity" numeric(14,2) NULL DEFAULT NULL,
  "material_scrap_quantity" numeric(14,2) NULL DEFAULT NULL,
  "other_scrap_quantity" numeric(14,2) NULL DEFAULT NULL,
  "critical_rate" numeric(14,2) NULL DEFAULT NULL,
  "major_rate" numeric(14,2) NULL DEFAULT NULL,
  "minor_rate" numeric(14,2) NULL DEFAULT NULL,
  "critical_quantity" int4 NULL DEFAULT NULL,
  "major_quantity" int4 NULL DEFAULT NULL,
  "minor_quantity" int4 NULL DEFAULT NULL,
  "check_result" int2 NULL DEFAULT NULL,
  "inspect_date" timestamp NULL DEFAULT NULL,
  "inspector_user_id" int8 NULL DEFAULT NULL,
  "status" int2 NULL DEFAULT NULL,
  "remark" varchar(500) NULL DEFAULT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "mes_qc_ipqc"."id" IS '';
COMMENT ON COLUMN "mes_qc_ipqc"."code" IS '';
COMMENT ON COLUMN "mes_qc_ipqc"."name" IS '';
COMMENT ON COLUMN "mes_qc_ipqc"."type" IS '';
COMMENT ON COLUMN "mes_qc_ipqc"."template_id" IS '';
COMMENT ON COLUMN "mes_qc_ipqc"."source_doc_type" IS '';
COMMENT ON COLUMN "mes_qc_ipqc"."source_doc_id" IS '';
COMMENT ON COLUMN "mes_qc_ipqc"."source_line_id" IS '';
COMMENT ON COLUMN "mes_qc_ipqc"."source_doc_code" IS '';
COMMENT ON COLUMN "mes_qc_ipqc"."work_order_id" IS '';
COMMENT ON COLUMN "mes_qc_ipqc"."task_id" IS '';
COMMENT ON COLUMN "mes_qc_ipqc"."workstation_id" IS '';
COMMENT ON COLUMN "mes_qc_ipqc"."process_id" IS '';
COMMENT ON COLUMN "mes_qc_ipqc"."item_id" IS '';
COMMENT ON COLUMN "mes_qc_ipqc"."check_quantity" IS '';
COMMENT ON COLUMN "mes_qc_ipqc"."qualified_quantity" IS '';
COMMENT ON COLUMN "mes_qc_ipqc"."unqualified_quantity" IS '';
COMMENT ON COLUMN "mes_qc_ipqc"."labor_scrap_quantity" IS '';
COMMENT ON COLUMN "mes_qc_ipqc"."material_scrap_quantity" IS '';
COMMENT ON COLUMN "mes_qc_ipqc"."other_scrap_quantity" IS '';
COMMENT ON COLUMN "mes_qc_ipqc"."critical_rate" IS '';
COMMENT ON COLUMN "mes_qc_ipqc"."major_rate" IS '';
COMMENT ON COLUMN "mes_qc_ipqc"."minor_rate" IS '';
COMMENT ON COLUMN "mes_qc_ipqc"."critical_quantity" IS '';
COMMENT ON COLUMN "mes_qc_ipqc"."major_quantity" IS '';
COMMENT ON COLUMN "mes_qc_ipqc"."minor_quantity" IS '';
COMMENT ON COLUMN "mes_qc_ipqc"."check_result" IS '';
COMMENT ON COLUMN "mes_qc_ipqc"."inspect_date" IS '';
COMMENT ON COLUMN "mes_qc_ipqc"."inspector_user_id" IS '';
COMMENT ON COLUMN "mes_qc_ipqc"."status" IS '';
COMMENT ON COLUMN "mes_qc_ipqc"."remark" IS '';
COMMENT ON COLUMN "mes_qc_ipqc"."creator" IS '';
COMMENT ON COLUMN "mes_qc_ipqc"."create_time" IS '';
COMMENT ON COLUMN "mes_qc_ipqc"."updater" IS '';
COMMENT ON COLUMN "mes_qc_ipqc"."update_time" IS '';
COMMENT ON COLUMN "mes_qc_ipqc"."deleted" IS '';
COMMENT ON COLUMN "mes_qc_ipqc"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "mes_pro_feedback"
-- ----------------------------
-- DROP TABLE IF EXISTS "mes_pro_feedback";
CREATE TABLE IF NOT EXISTS "mes_pro_feedback" (
    "id" int8 NOT NULL,
  "code" varchar(64) NULL DEFAULT NULL,
  "type" int2 NULL DEFAULT NULL,
  "channel" varchar(64) NULL DEFAULT NULL,
  "feedback_time" timestamp NULL DEFAULT NULL,
  "workstation_id" int8 NULL DEFAULT NULL,
  "route_id" int8 NULL DEFAULT NULL,
  "process_id" int8 NULL DEFAULT NULL,
  "work_order_id" int8 NULL DEFAULT NULL,
  "task_id" int8 NULL DEFAULT NULL,
  "item_id" int8 NULL DEFAULT NULL,
  "expire_date" timestamp NULL DEFAULT NULL,
  "lot_number" varchar(64) NULL DEFAULT NULL,
  "scheduled_quantity" numeric(14,2) NULL DEFAULT NULL,
  "feedback_quantity" numeric(14,2) NULL DEFAULT NULL,
  "qualified_quantity" numeric(14,2) NULL DEFAULT NULL,
  "unqualified_quantity" numeric(14,2) NULL DEFAULT NULL,
  "uncheck_quantity" numeric(14,2) NULL DEFAULT NULL,
  "labor_scrap_quantity" numeric(14,2) NULL DEFAULT NULL,
  "material_scrap_quantity" numeric(14,2) NULL DEFAULT NULL,
  "other_scrap_quantity" numeric(14,2) NULL DEFAULT NULL,
  "feedback_user_id" int8 NULL DEFAULT NULL,
  "approve_user_id" int8 NULL DEFAULT NULL,
  "status" int2 NULL DEFAULT NULL,
  "remark" varchar(500) NULL DEFAULT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "mes_pro_feedback"."id" IS '';
COMMENT ON COLUMN "mes_pro_feedback"."code" IS '';
COMMENT ON COLUMN "mes_pro_feedback"."type" IS '';
COMMENT ON COLUMN "mes_pro_feedback"."channel" IS '';
COMMENT ON COLUMN "mes_pro_feedback"."feedback_time" IS '';
COMMENT ON COLUMN "mes_pro_feedback"."workstation_id" IS '';
COMMENT ON COLUMN "mes_pro_feedback"."route_id" IS '';
COMMENT ON COLUMN "mes_pro_feedback"."process_id" IS '';
COMMENT ON COLUMN "mes_pro_feedback"."work_order_id" IS '';
COMMENT ON COLUMN "mes_pro_feedback"."task_id" IS '';
COMMENT ON COLUMN "mes_pro_feedback"."item_id" IS '';
COMMENT ON COLUMN "mes_pro_feedback"."expire_date" IS '';
COMMENT ON COLUMN "mes_pro_feedback"."lot_number" IS '';
COMMENT ON COLUMN "mes_pro_feedback"."scheduled_quantity" IS '';
COMMENT ON COLUMN "mes_pro_feedback"."feedback_quantity" IS '';
COMMENT ON COLUMN "mes_pro_feedback"."qualified_quantity" IS '';
COMMENT ON COLUMN "mes_pro_feedback"."unqualified_quantity" IS '';
COMMENT ON COLUMN "mes_pro_feedback"."uncheck_quantity" IS '';
COMMENT ON COLUMN "mes_pro_feedback"."labor_scrap_quantity" IS '';
COMMENT ON COLUMN "mes_pro_feedback"."material_scrap_quantity" IS '';
COMMENT ON COLUMN "mes_pro_feedback"."other_scrap_quantity" IS '';
COMMENT ON COLUMN "mes_pro_feedback"."feedback_user_id" IS '';
COMMENT ON COLUMN "mes_pro_feedback"."approve_user_id" IS '';
COMMENT ON COLUMN "mes_pro_feedback"."status" IS '';
COMMENT ON COLUMN "mes_pro_feedback"."remark" IS '';
COMMENT ON COLUMN "mes_pro_feedback"."creator" IS '';
COMMENT ON COLUMN "mes_pro_feedback"."create_time" IS '';
COMMENT ON COLUMN "mes_pro_feedback"."updater" IS '';
COMMENT ON COLUMN "mes_pro_feedback"."update_time" IS '';
COMMENT ON COLUMN "mes_pro_feedback"."deleted" IS '';
COMMENT ON COLUMN "mes_pro_feedback"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "mes_wm_product_produce"
-- ----------------------------
-- DROP TABLE IF EXISTS "mes_wm_product_produce";
CREATE TABLE IF NOT EXISTS "mes_wm_product_produce" (
    "id" int8 NOT NULL,
  "work_order_id" int8 NULL DEFAULT NULL,
  "feedback_id" int8 NULL DEFAULT NULL,
  "task_id" int8 NULL DEFAULT NULL,
  "workstation_id" int8 NULL DEFAULT NULL,
  "process_id" int8 NULL DEFAULT NULL,
  "produce_date" timestamp NULL DEFAULT NULL,
  "status" int4 NULL DEFAULT NULL,
  "remark" varchar(500) NULL DEFAULT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "mes_wm_product_produce"."id" IS '';
COMMENT ON COLUMN "mes_wm_product_produce"."work_order_id" IS '';
COMMENT ON COLUMN "mes_wm_product_produce"."feedback_id" IS '';
COMMENT ON COLUMN "mes_wm_product_produce"."task_id" IS '';
COMMENT ON COLUMN "mes_wm_product_produce"."workstation_id" IS '';
COMMENT ON COLUMN "mes_wm_product_produce"."process_id" IS '';
COMMENT ON COLUMN "mes_wm_product_produce"."produce_date" IS '';
COMMENT ON COLUMN "mes_wm_product_produce"."status" IS '';
COMMENT ON COLUMN "mes_wm_product_produce"."remark" IS '';
COMMENT ON COLUMN "mes_wm_product_produce"."creator" IS '';
COMMENT ON COLUMN "mes_wm_product_produce"."create_time" IS '';
COMMENT ON COLUMN "mes_wm_product_produce"."updater" IS '';
COMMENT ON COLUMN "mes_wm_product_produce"."update_time" IS '';
COMMENT ON COLUMN "mes_wm_product_produce"."deleted" IS '';
COMMENT ON COLUMN "mes_wm_product_produce"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "mes_wm_product_produce_line"
-- ----------------------------
-- DROP TABLE IF EXISTS "mes_wm_product_produce_line";
CREATE TABLE IF NOT EXISTS "mes_wm_product_produce_line" (
    "id" int8 NOT NULL,
  "produce_id" int8 NULL DEFAULT NULL,
  "feedback_id" int8 NULL DEFAULT NULL,
  "item_id" int8 NULL DEFAULT NULL,
  "quantity" numeric(12,2) NULL DEFAULT NULL,
  "batch_id" int8 NULL DEFAULT NULL,
  "batch_code" varchar(255) NULL DEFAULT NULL,
  "expire_date" timestamp NULL DEFAULT NULL,
  "lot_number" varchar(128) NULL DEFAULT NULL,
  "quality_status" int4 NULL DEFAULT NULL,
  "remark" varchar(500) NULL DEFAULT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "mes_wm_product_produce_line"."id" IS '';
COMMENT ON COLUMN "mes_wm_product_produce_line"."produce_id" IS '';
COMMENT ON COLUMN "mes_wm_product_produce_line"."feedback_id" IS '';
COMMENT ON COLUMN "mes_wm_product_produce_line"."item_id" IS '';
COMMENT ON COLUMN "mes_wm_product_produce_line"."quantity" IS '';
COMMENT ON COLUMN "mes_wm_product_produce_line"."batch_id" IS '';
COMMENT ON COLUMN "mes_wm_product_produce_line"."batch_code" IS '';
COMMENT ON COLUMN "mes_wm_product_produce_line"."expire_date" IS '';
COMMENT ON COLUMN "mes_wm_product_produce_line"."lot_number" IS '';
COMMENT ON COLUMN "mes_wm_product_produce_line"."quality_status" IS '';
COMMENT ON COLUMN "mes_wm_product_produce_line"."remark" IS '';
COMMENT ON COLUMN "mes_wm_product_produce_line"."creator" IS '';
COMMENT ON COLUMN "mes_wm_product_produce_line"."create_time" IS '';
COMMENT ON COLUMN "mes_wm_product_produce_line"."updater" IS '';
COMMENT ON COLUMN "mes_wm_product_produce_line"."update_time" IS '';
COMMENT ON COLUMN "mes_wm_product_produce_line"."deleted" IS '';
COMMENT ON COLUMN "mes_wm_product_produce_line"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "mes_wm_product_produce_detail"
-- ----------------------------
-- DROP TABLE IF EXISTS "mes_wm_product_produce_detail";
CREATE TABLE IF NOT EXISTS "mes_wm_product_produce_detail" (
    "id" int8 NOT NULL,
  "produce_id" int8 NULL DEFAULT NULL,
  "line_id" int8 NULL DEFAULT NULL,
  "item_id" int8 NULL DEFAULT NULL,
  "quantity" numeric(12,2) NULL DEFAULT NULL,
  "batch_id" int8 NULL DEFAULT NULL,
  "batch_code" varchar(255) NULL DEFAULT NULL,
  "warehouse_id" int8 NULL DEFAULT NULL,
  "location_id" int8 NULL DEFAULT NULL,
  "area_id" int8 NULL DEFAULT NULL,
  "remark" varchar(500) NULL DEFAULT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "mes_wm_product_produce_detail"."id" IS '';
COMMENT ON COLUMN "mes_wm_product_produce_detail"."produce_id" IS '';
COMMENT ON COLUMN "mes_wm_product_produce_detail"."line_id" IS '';
COMMENT ON COLUMN "mes_wm_product_produce_detail"."item_id" IS '';
COMMENT ON COLUMN "mes_wm_product_produce_detail"."quantity" IS '';
COMMENT ON COLUMN "mes_wm_product_produce_detail"."batch_id" IS '';
COMMENT ON COLUMN "mes_wm_product_produce_detail"."batch_code" IS '';
COMMENT ON COLUMN "mes_wm_product_produce_detail"."warehouse_id" IS '';
COMMENT ON COLUMN "mes_wm_product_produce_detail"."location_id" IS '';
COMMENT ON COLUMN "mes_wm_product_produce_detail"."area_id" IS '';
COMMENT ON COLUMN "mes_wm_product_produce_detail"."remark" IS '';
COMMENT ON COLUMN "mes_wm_product_produce_detail"."creator" IS '';
COMMENT ON COLUMN "mes_wm_product_produce_detail"."create_time" IS '';
COMMENT ON COLUMN "mes_wm_product_produce_detail"."updater" IS '';
COMMENT ON COLUMN "mes_wm_product_produce_detail"."update_time" IS '';
COMMENT ON COLUMN "mes_wm_product_produce_detail"."deleted" IS '';
COMMENT ON COLUMN "mes_wm_product_produce_detail"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "mes_qc_oqc"
-- ----------------------------
-- DROP TABLE IF EXISTS "mes_qc_oqc";
CREATE TABLE IF NOT EXISTS "mes_qc_oqc" (
    "id" int8 NOT NULL,
  "code" varchar(64) NOT NULL,
  "name" varchar(500) NOT NULL,
  "template_id" int8 NOT NULL,
  "source_doc_type" int4 NULL DEFAULT NULL,
  "source_doc_id" int8 NULL DEFAULT NULL,
  "source_line_id" int8 NULL DEFAULT NULL,
  "source_doc_code" varchar(64) NULL DEFAULT NULL,
  "client_id" int8 NOT NULL,
  "batch_code" varchar(64) NULL DEFAULT NULL,
  "item_id" int8 NOT NULL,
  "min_check_quantity" int4 NULL DEFAULT 1,
  "max_unqualified_quantity" int4 NULL DEFAULT 0,
  "out_quantity" numeric(14,2) NOT NULL,
  "check_quantity" numeric(14,2) NULL DEFAULT NULL,
  "qualified_quantity" numeric(14,2) NULL DEFAULT 0.00,
  "unqualified_quantity" numeric(14,2) NULL DEFAULT 0.00,
  "critical_rate" numeric(14,2) NULL DEFAULT 0.00,
  "major_rate" numeric(14,2) NULL DEFAULT 0.00,
  "minor_rate" numeric(14,2) NULL DEFAULT 0.00,
  "critical_quantity" int4 NULL DEFAULT 0,
  "major_quantity" int4 NULL DEFAULT 0,
  "minor_quantity" int4 NULL DEFAULT 0,
  "check_result" int2 NULL DEFAULT NULL,
  "out_date" timestamp NULL DEFAULT NULL,
  "inspect_date" timestamp NULL DEFAULT NULL,
  "inspector_user_id" int8 NULL DEFAULT NULL,
  "status" int2 NOT NULL DEFAULT 0,
  "remark" varchar(500) NULL DEFAULT '',
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "mes_qc_oqc"."id" IS '';
COMMENT ON COLUMN "mes_qc_oqc"."code" IS '';
COMMENT ON COLUMN "mes_qc_oqc"."name" IS '';
COMMENT ON COLUMN "mes_qc_oqc"."template_id" IS '';
COMMENT ON COLUMN "mes_qc_oqc"."source_doc_type" IS '';
COMMENT ON COLUMN "mes_qc_oqc"."source_doc_id" IS '';
COMMENT ON COLUMN "mes_qc_oqc"."source_line_id" IS '';
COMMENT ON COLUMN "mes_qc_oqc"."source_doc_code" IS '';
COMMENT ON COLUMN "mes_qc_oqc"."client_id" IS '';
COMMENT ON COLUMN "mes_qc_oqc"."batch_code" IS '';
COMMENT ON COLUMN "mes_qc_oqc"."item_id" IS '';
COMMENT ON COLUMN "mes_qc_oqc"."min_check_quantity" IS '';
COMMENT ON COLUMN "mes_qc_oqc"."max_unqualified_quantity" IS '';
COMMENT ON COLUMN "mes_qc_oqc"."out_quantity" IS '';
COMMENT ON COLUMN "mes_qc_oqc"."check_quantity" IS '';
COMMENT ON COLUMN "mes_qc_oqc"."qualified_quantity" IS '';
COMMENT ON COLUMN "mes_qc_oqc"."unqualified_quantity" IS '';
COMMENT ON COLUMN "mes_qc_oqc"."critical_rate" IS '';
COMMENT ON COLUMN "mes_qc_oqc"."major_rate" IS '';
COMMENT ON COLUMN "mes_qc_oqc"."minor_rate" IS '';
COMMENT ON COLUMN "mes_qc_oqc"."critical_quantity" IS '';
COMMENT ON COLUMN "mes_qc_oqc"."major_quantity" IS '';
COMMENT ON COLUMN "mes_qc_oqc"."minor_quantity" IS '';
COMMENT ON COLUMN "mes_qc_oqc"."check_result" IS '';
COMMENT ON COLUMN "mes_qc_oqc"."out_date" IS '';
COMMENT ON COLUMN "mes_qc_oqc"."inspect_date" IS '';
COMMENT ON COLUMN "mes_qc_oqc"."inspector_user_id" IS '';
COMMENT ON COLUMN "mes_qc_oqc"."status" IS '';
COMMENT ON COLUMN "mes_qc_oqc"."remark" IS '';
COMMENT ON COLUMN "mes_qc_oqc"."creator" IS '';
COMMENT ON COLUMN "mes_qc_oqc"."create_time" IS '';
COMMENT ON COLUMN "mes_qc_oqc"."updater" IS '';
COMMENT ON COLUMN "mes_qc_oqc"."update_time" IS '';
COMMENT ON COLUMN "mes_qc_oqc"."deleted" IS '';
COMMENT ON COLUMN "mes_qc_oqc"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "mes_qc_rqc"
-- ----------------------------
-- DROP TABLE IF EXISTS "mes_qc_rqc";
CREATE TABLE IF NOT EXISTS "mes_qc_rqc" (
    "id" int8 NOT NULL,
  "code" varchar(64) NOT NULL,
  "name" varchar(500) NOT NULL,
  "template_id" int8 NOT NULL,
  "source_doc_type" int4 NULL DEFAULT NULL,
  "source_doc_id" int8 NULL DEFAULT NULL,
  "source_line_id" int8 NULL DEFAULT NULL,
  "source_doc_code" varchar(64) NULL DEFAULT NULL,
  "type" int4 NULL DEFAULT NULL,
  "item_id" int8 NOT NULL,
  "batch_code" varchar(128) NULL DEFAULT NULL,
  "check_quantity" numeric(14,2) NULL DEFAULT NULL,
  "qualified_quantity" numeric(14,2) NULL DEFAULT 0.00,
  "unqualified_quantity" numeric(14,2) NULL DEFAULT 0.00,
  "critical_rate" numeric(14,2) NULL DEFAULT 0.00,
  "major_rate" numeric(14,2) NULL DEFAULT 0.00,
  "minor_rate" numeric(14,2) NULL DEFAULT 0.00,
  "critical_quantity" int4 NULL DEFAULT 0,
  "major_quantity" int4 NULL DEFAULT 0,
  "minor_quantity" int4 NULL DEFAULT 0,
  "check_result" int2 NULL DEFAULT NULL,
  "inspect_date" timestamp NULL DEFAULT NULL,
  "inspector_user_id" int8 NULL DEFAULT NULL,
  "status" int2 NOT NULL DEFAULT 0,
  "remark" varchar(500) NULL DEFAULT '',
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "mes_qc_rqc"."id" IS '';
COMMENT ON COLUMN "mes_qc_rqc"."code" IS '';
COMMENT ON COLUMN "mes_qc_rqc"."name" IS '';
COMMENT ON COLUMN "mes_qc_rqc"."template_id" IS '';
COMMENT ON COLUMN "mes_qc_rqc"."source_doc_type" IS '';
COMMENT ON COLUMN "mes_qc_rqc"."source_doc_id" IS '';
COMMENT ON COLUMN "mes_qc_rqc"."source_line_id" IS '';
COMMENT ON COLUMN "mes_qc_rqc"."source_doc_code" IS '';
COMMENT ON COLUMN "mes_qc_rqc"."type" IS '';
COMMENT ON COLUMN "mes_qc_rqc"."item_id" IS '';
COMMENT ON COLUMN "mes_qc_rqc"."batch_code" IS '';
COMMENT ON COLUMN "mes_qc_rqc"."check_quantity" IS '';
COMMENT ON COLUMN "mes_qc_rqc"."qualified_quantity" IS '';
COMMENT ON COLUMN "mes_qc_rqc"."unqualified_quantity" IS '';
COMMENT ON COLUMN "mes_qc_rqc"."critical_rate" IS '';
COMMENT ON COLUMN "mes_qc_rqc"."major_rate" IS '';
COMMENT ON COLUMN "mes_qc_rqc"."minor_rate" IS '';
COMMENT ON COLUMN "mes_qc_rqc"."critical_quantity" IS '';
COMMENT ON COLUMN "mes_qc_rqc"."major_quantity" IS '';
COMMENT ON COLUMN "mes_qc_rqc"."minor_quantity" IS '';
COMMENT ON COLUMN "mes_qc_rqc"."check_result" IS '';
COMMENT ON COLUMN "mes_qc_rqc"."inspect_date" IS '';
COMMENT ON COLUMN "mes_qc_rqc"."inspector_user_id" IS '';
COMMENT ON COLUMN "mes_qc_rqc"."status" IS '';
COMMENT ON COLUMN "mes_qc_rqc"."remark" IS '';
COMMENT ON COLUMN "mes_qc_rqc"."creator" IS '';
COMMENT ON COLUMN "mes_qc_rqc"."create_time" IS '';
COMMENT ON COLUMN "mes_qc_rqc"."updater" IS '';
COMMENT ON COLUMN "mes_qc_rqc"."update_time" IS '';
COMMENT ON COLUMN "mes_qc_rqc"."deleted" IS '';
COMMENT ON COLUMN "mes_qc_rqc"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "mes_wm_product_sales_line"
-- ----------------------------
-- DROP TABLE IF EXISTS "mes_wm_product_sales_line";
CREATE TABLE IF NOT EXISTS "mes_wm_product_sales_line" (
    "id" int8 NOT NULL,
  "sales_id" int8 NOT NULL,
  "notice_line_id" int8 NULL DEFAULT NULL,
  "item_id" int8 NOT NULL,
  "quantity" numeric(20,6) NOT NULL,
  "batch_id" int8 NULL DEFAULT NULL,
  "batch_code" varchar(255) NULL DEFAULT NULL,
  "material_stock_id" int8 NULL DEFAULT NULL,
  "oqc_check_flag" int2 NULL DEFAULT NULL,
  "oqc_id" int8 NULL DEFAULT NULL,
  "quality_status" int4 NULL DEFAULT NULL,
  "remark" varchar(500) NULL DEFAULT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "mes_wm_product_sales_line"."id" IS '';
COMMENT ON COLUMN "mes_wm_product_sales_line"."sales_id" IS '';
COMMENT ON COLUMN "mes_wm_product_sales_line"."notice_line_id" IS '';
COMMENT ON COLUMN "mes_wm_product_sales_line"."item_id" IS '';
COMMENT ON COLUMN "mes_wm_product_sales_line"."quantity" IS '';
COMMENT ON COLUMN "mes_wm_product_sales_line"."batch_id" IS '';
COMMENT ON COLUMN "mes_wm_product_sales_line"."batch_code" IS '';
COMMENT ON COLUMN "mes_wm_product_sales_line"."material_stock_id" IS '';
COMMENT ON COLUMN "mes_wm_product_sales_line"."oqc_check_flag" IS '';
COMMENT ON COLUMN "mes_wm_product_sales_line"."oqc_id" IS '';
COMMENT ON COLUMN "mes_wm_product_sales_line"."quality_status" IS '';
COMMENT ON COLUMN "mes_wm_product_sales_line"."remark" IS '';
COMMENT ON COLUMN "mes_wm_product_sales_line"."creator" IS '';
COMMENT ON COLUMN "mes_wm_product_sales_line"."create_time" IS '';
COMMENT ON COLUMN "mes_wm_product_sales_line"."updater" IS '';
COMMENT ON COLUMN "mes_wm_product_sales_line"."update_time" IS '';
COMMENT ON COLUMN "mes_wm_product_sales_line"."deleted" IS '';
COMMENT ON COLUMN "mes_wm_product_sales_line"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "mes_wm_return_issue_line"
-- ----------------------------
-- DROP TABLE IF EXISTS "mes_wm_return_issue_line";
CREATE TABLE IF NOT EXISTS "mes_wm_return_issue_line" (
    "id" int8 NOT NULL,
  "issue_id" int8 NOT NULL,
  "material_stock_id" int8 NULL DEFAULT NULL,
  "item_id" int8 NOT NULL,
  "quantity" numeric(12,2) NOT NULL DEFAULT 0.00,
  "batch_id" int8 NULL DEFAULT NULL,
  "batch_code" varchar(255) NULL DEFAULT NULL,
  "rqc_id" int8 NULL DEFAULT NULL,
  "rqc_check_flag" int2 NOT NULL DEFAULT 0,
  "quality_status" int4 NULL DEFAULT NULL,
  "remark" varchar(500) NULL DEFAULT '',
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "mes_wm_return_issue_line"."id" IS '';
COMMENT ON COLUMN "mes_wm_return_issue_line"."issue_id" IS '';
COMMENT ON COLUMN "mes_wm_return_issue_line"."material_stock_id" IS '';
COMMENT ON COLUMN "mes_wm_return_issue_line"."item_id" IS '';
COMMENT ON COLUMN "mes_wm_return_issue_line"."quantity" IS '';
COMMENT ON COLUMN "mes_wm_return_issue_line"."batch_id" IS '';
COMMENT ON COLUMN "mes_wm_return_issue_line"."batch_code" IS '';
COMMENT ON COLUMN "mes_wm_return_issue_line"."rqc_id" IS '';
COMMENT ON COLUMN "mes_wm_return_issue_line"."rqc_check_flag" IS '';
COMMENT ON COLUMN "mes_wm_return_issue_line"."quality_status" IS '';
COMMENT ON COLUMN "mes_wm_return_issue_line"."remark" IS '';
COMMENT ON COLUMN "mes_wm_return_issue_line"."creator" IS '';
COMMENT ON COLUMN "mes_wm_return_issue_line"."create_time" IS '';
COMMENT ON COLUMN "mes_wm_return_issue_line"."updater" IS '';
COMMENT ON COLUMN "mes_wm_return_issue_line"."update_time" IS '';
COMMENT ON COLUMN "mes_wm_return_issue_line"."deleted" IS '';
COMMENT ON COLUMN "mes_wm_return_issue_line"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "mes_wm_return_sales_line"
-- ----------------------------
-- DROP TABLE IF EXISTS "mes_wm_return_sales_line";
CREATE TABLE IF NOT EXISTS "mes_wm_return_sales_line" (
    "id" int8 NOT NULL,
  "return_id" int8 NOT NULL,
  "item_id" int8 NOT NULL,
  "quantity" numeric(12,2) NOT NULL DEFAULT 0.00,
  "batch_id" int8 NULL DEFAULT NULL,
  "batch_code" varchar(255) NULL DEFAULT NULL,
  "rqc_id" int8 NULL DEFAULT NULL,
  "rqc_check_flag" int2 NOT NULL DEFAULT 0,
  "quality_status" int4 NULL DEFAULT NULL,
  "remark" varchar(500) NULL DEFAULT '',
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "mes_wm_return_sales_line"."id" IS '';
COMMENT ON COLUMN "mes_wm_return_sales_line"."return_id" IS '';
COMMENT ON COLUMN "mes_wm_return_sales_line"."item_id" IS '';
COMMENT ON COLUMN "mes_wm_return_sales_line"."quantity" IS '';
COMMENT ON COLUMN "mes_wm_return_sales_line"."batch_id" IS '';
COMMENT ON COLUMN "mes_wm_return_sales_line"."batch_code" IS '';
COMMENT ON COLUMN "mes_wm_return_sales_line"."rqc_id" IS '';
COMMENT ON COLUMN "mes_wm_return_sales_line"."rqc_check_flag" IS '';
COMMENT ON COLUMN "mes_wm_return_sales_line"."quality_status" IS '';
COMMENT ON COLUMN "mes_wm_return_sales_line"."remark" IS '';
COMMENT ON COLUMN "mes_wm_return_sales_line"."creator" IS '';
COMMENT ON COLUMN "mes_wm_return_sales_line"."create_time" IS '';
COMMENT ON COLUMN "mes_wm_return_sales_line"."updater" IS '';
COMMENT ON COLUMN "mes_wm_return_sales_line"."update_time" IS '';
COMMENT ON COLUMN "mes_wm_return_sales_line"."deleted" IS '';
COMMENT ON COLUMN "mes_wm_return_sales_line"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "mes_wm_item_receipt"
-- ----------------------------
-- DROP TABLE IF EXISTS "mes_wm_item_receipt";
CREATE TABLE IF NOT EXISTS "mes_wm_item_receipt" (
    "id" int8 NOT NULL,
  "code" varchar(64) NOT NULL,
  "name" varchar(255) NULL DEFAULT NULL,
  "iqc_id" int8 NULL DEFAULT NULL,
  "notice_id" int8 NULL DEFAULT NULL,
  "vendor_id" int8 NULL DEFAULT NULL,
  "purchase_order_code" varchar(64) NULL DEFAULT NULL,
  "receipt_date" timestamp NULL DEFAULT NULL,
  "status" int2 NOT NULL DEFAULT 0,
  "remark" varchar(500) NULL DEFAULT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "mes_wm_item_receipt"."id" IS '';
COMMENT ON COLUMN "mes_wm_item_receipt"."code" IS '';
COMMENT ON COLUMN "mes_wm_item_receipt"."name" IS '';
COMMENT ON COLUMN "mes_wm_item_receipt"."iqc_id" IS '';
COMMENT ON COLUMN "mes_wm_item_receipt"."notice_id" IS '';
COMMENT ON COLUMN "mes_wm_item_receipt"."vendor_id" IS '';
COMMENT ON COLUMN "mes_wm_item_receipt"."purchase_order_code" IS '';
COMMENT ON COLUMN "mes_wm_item_receipt"."receipt_date" IS '';
COMMENT ON COLUMN "mes_wm_item_receipt"."status" IS '';
COMMENT ON COLUMN "mes_wm_item_receipt"."remark" IS '';
COMMENT ON COLUMN "mes_wm_item_receipt"."creator" IS '';
COMMENT ON COLUMN "mes_wm_item_receipt"."create_time" IS '';
COMMENT ON COLUMN "mes_wm_item_receipt"."updater" IS '';
COMMENT ON COLUMN "mes_wm_item_receipt"."update_time" IS '';
COMMENT ON COLUMN "mes_wm_item_receipt"."deleted" IS '';
COMMENT ON COLUMN "mes_wm_item_receipt"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "mes_wm_item_receipt_line"
-- ----------------------------
-- DROP TABLE IF EXISTS "mes_wm_item_receipt_line";
CREATE TABLE IF NOT EXISTS "mes_wm_item_receipt_line" (
    "id" int8 NOT NULL,
  "receipt_id" int8 NOT NULL,
  "arrival_notice_line_id" int8 NULL DEFAULT NULL,
  "item_id" int8 NOT NULL,
  "received_quantity" numeric(14,2) NULL DEFAULT NULL,
  "batch_id" int8 NULL DEFAULT NULL,
  "batch_code" varchar(255) NULL DEFAULT NULL,
  "production_date" timestamp NULL DEFAULT NULL,
  "expire_date" timestamp NULL DEFAULT NULL,
  "lot_number" varchar(128) NULL DEFAULT NULL,
  "remark" varchar(500) NULL DEFAULT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "mes_wm_item_receipt_line"."id" IS '';
COMMENT ON COLUMN "mes_wm_item_receipt_line"."receipt_id" IS '';
COMMENT ON COLUMN "mes_wm_item_receipt_line"."arrival_notice_line_id" IS '';
COMMENT ON COLUMN "mes_wm_item_receipt_line"."item_id" IS '';
COMMENT ON COLUMN "mes_wm_item_receipt_line"."received_quantity" IS '';
COMMENT ON COLUMN "mes_wm_item_receipt_line"."batch_id" IS '';
COMMENT ON COLUMN "mes_wm_item_receipt_line"."batch_code" IS '';
COMMENT ON COLUMN "mes_wm_item_receipt_line"."production_date" IS '';
COMMENT ON COLUMN "mes_wm_item_receipt_line"."expire_date" IS '';
COMMENT ON COLUMN "mes_wm_item_receipt_line"."lot_number" IS '';
COMMENT ON COLUMN "mes_wm_item_receipt_line"."remark" IS '';
COMMENT ON COLUMN "mes_wm_item_receipt_line"."creator" IS '';
COMMENT ON COLUMN "mes_wm_item_receipt_line"."create_time" IS '';
COMMENT ON COLUMN "mes_wm_item_receipt_line"."updater" IS '';
COMMENT ON COLUMN "mes_wm_item_receipt_line"."update_time" IS '';
COMMENT ON COLUMN "mes_wm_item_receipt_line"."deleted" IS '';
COMMENT ON COLUMN "mes_wm_item_receipt_line"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "mes_wm_item_receipt_detail"
-- ----------------------------
-- DROP TABLE IF EXISTS "mes_wm_item_receipt_detail";
CREATE TABLE IF NOT EXISTS "mes_wm_item_receipt_detail" (
    "id" int8 NOT NULL,
  "line_id" int8 NOT NULL,
  "receipt_id" int8 NOT NULL,
  "item_id" int8 NOT NULL,
  "quantity" numeric(14,2) NULL DEFAULT NULL,
  "batch_id" int8 NULL DEFAULT NULL,
  "warehouse_id" int8 NULL DEFAULT NULL,
  "location_id" int8 NULL DEFAULT NULL,
  "area_id" int8 NULL DEFAULT NULL,
  "remark" varchar(500) NULL DEFAULT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "mes_wm_item_receipt_detail"."id" IS '';
COMMENT ON COLUMN "mes_wm_item_receipt_detail"."line_id" IS '';
COMMENT ON COLUMN "mes_wm_item_receipt_detail"."receipt_id" IS '';
COMMENT ON COLUMN "mes_wm_item_receipt_detail"."item_id" IS '';
COMMENT ON COLUMN "mes_wm_item_receipt_detail"."quantity" IS '';
COMMENT ON COLUMN "mes_wm_item_receipt_detail"."batch_id" IS '';
COMMENT ON COLUMN "mes_wm_item_receipt_detail"."warehouse_id" IS '';
COMMENT ON COLUMN "mes_wm_item_receipt_detail"."location_id" IS '';
COMMENT ON COLUMN "mes_wm_item_receipt_detail"."area_id" IS '';
COMMENT ON COLUMN "mes_wm_item_receipt_detail"."remark" IS '';
COMMENT ON COLUMN "mes_wm_item_receipt_detail"."creator" IS '';
COMMENT ON COLUMN "mes_wm_item_receipt_detail"."create_time" IS '';
COMMENT ON COLUMN "mes_wm_item_receipt_detail"."updater" IS '';
COMMENT ON COLUMN "mes_wm_item_receipt_detail"."update_time" IS '';
COMMENT ON COLUMN "mes_wm_item_receipt_detail"."deleted" IS '';
COMMENT ON COLUMN "mes_wm_item_receipt_detail"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "mes_wm_item_consume"
-- ----------------------------
-- DROP TABLE IF EXISTS "mes_wm_item_consume";
CREATE TABLE IF NOT EXISTS "mes_wm_item_consume" (
    "id" int8 NOT NULL,
  "work_order_id" int8 NOT NULL,
  "task_id" int8 NOT NULL,
  "workstation_id" int8 NOT NULL,
  "process_id" int8 NOT NULL,
  "feedback_id" int8 NOT NULL,
  "consume_date" timestamp NOT NULL,
  "status" int2 NOT NULL DEFAULT 0,
  "remark" varchar(500) NULL DEFAULT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "mes_wm_item_consume"."id" IS '';
COMMENT ON COLUMN "mes_wm_item_consume"."work_order_id" IS '';
COMMENT ON COLUMN "mes_wm_item_consume"."task_id" IS '';
COMMENT ON COLUMN "mes_wm_item_consume"."workstation_id" IS '';
COMMENT ON COLUMN "mes_wm_item_consume"."process_id" IS '';
COMMENT ON COLUMN "mes_wm_item_consume"."feedback_id" IS '';
COMMENT ON COLUMN "mes_wm_item_consume"."consume_date" IS '';
COMMENT ON COLUMN "mes_wm_item_consume"."status" IS '';
COMMENT ON COLUMN "mes_wm_item_consume"."remark" IS '';
COMMENT ON COLUMN "mes_wm_item_consume"."creator" IS '';
COMMENT ON COLUMN "mes_wm_item_consume"."create_time" IS '';
COMMENT ON COLUMN "mes_wm_item_consume"."updater" IS '';
COMMENT ON COLUMN "mes_wm_item_consume"."update_time" IS '';
COMMENT ON COLUMN "mes_wm_item_consume"."deleted" IS '';
COMMENT ON COLUMN "mes_wm_item_consume"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "mes_wm_item_consume_line"
-- ----------------------------
-- DROP TABLE IF EXISTS "mes_wm_item_consume_line";
CREATE TABLE IF NOT EXISTS "mes_wm_item_consume_line" (
    "id" int8 NOT NULL,
  "consume_id" int8 NOT NULL,
  "item_id" int8 NOT NULL,
  "quantity" numeric(14,2) NOT NULL,
  "batch_id" int8 NULL DEFAULT NULL,
  "batch_code" varchar(128) NULL DEFAULT NULL,
  "remark" varchar(500) NULL DEFAULT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "mes_wm_item_consume_line"."id" IS '';
COMMENT ON COLUMN "mes_wm_item_consume_line"."consume_id" IS '';
COMMENT ON COLUMN "mes_wm_item_consume_line"."item_id" IS '';
COMMENT ON COLUMN "mes_wm_item_consume_line"."quantity" IS '';
COMMENT ON COLUMN "mes_wm_item_consume_line"."batch_id" IS '';
COMMENT ON COLUMN "mes_wm_item_consume_line"."batch_code" IS '';
COMMENT ON COLUMN "mes_wm_item_consume_line"."remark" IS '';
COMMENT ON COLUMN "mes_wm_item_consume_line"."creator" IS '';
COMMENT ON COLUMN "mes_wm_item_consume_line"."create_time" IS '';
COMMENT ON COLUMN "mes_wm_item_consume_line"."updater" IS '';
COMMENT ON COLUMN "mes_wm_item_consume_line"."update_time" IS '';
COMMENT ON COLUMN "mes_wm_item_consume_line"."deleted" IS '';
COMMENT ON COLUMN "mes_wm_item_consume_line"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "mes_wm_item_consume_detail"
-- ----------------------------
-- DROP TABLE IF EXISTS "mes_wm_item_consume_detail";
CREATE TABLE IF NOT EXISTS "mes_wm_item_consume_detail" (
    "id" int8 NOT NULL,
  "consume_id" int8 NOT NULL,
  "line_id" int8 NOT NULL,
  "material_stock_id" int8 NULL DEFAULT NULL,
  "item_id" int8 NOT NULL,
  "quantity" numeric(14,2) NOT NULL,
  "batch_id" int8 NULL DEFAULT NULL,
  "batch_code" varchar(128) NULL DEFAULT NULL,
  "warehouse_id" int8 NOT NULL,
  "location_id" int8 NOT NULL,
  "area_id" int8 NOT NULL,
  "remark" varchar(500) NULL DEFAULT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "mes_wm_item_consume_detail"."id" IS '';
COMMENT ON COLUMN "mes_wm_item_consume_detail"."consume_id" IS '';
COMMENT ON COLUMN "mes_wm_item_consume_detail"."line_id" IS '';
COMMENT ON COLUMN "mes_wm_item_consume_detail"."material_stock_id" IS '';
COMMENT ON COLUMN "mes_wm_item_consume_detail"."item_id" IS '';
COMMENT ON COLUMN "mes_wm_item_consume_detail"."quantity" IS '';
COMMENT ON COLUMN "mes_wm_item_consume_detail"."batch_id" IS '';
COMMENT ON COLUMN "mes_wm_item_consume_detail"."batch_code" IS '';
COMMENT ON COLUMN "mes_wm_item_consume_detail"."warehouse_id" IS '';
COMMENT ON COLUMN "mes_wm_item_consume_detail"."location_id" IS '';
COMMENT ON COLUMN "mes_wm_item_consume_detail"."area_id" IS '';
COMMENT ON COLUMN "mes_wm_item_consume_detail"."remark" IS '';
COMMENT ON COLUMN "mes_wm_item_consume_detail"."creator" IS '';
COMMENT ON COLUMN "mes_wm_item_consume_detail"."create_time" IS '';
COMMENT ON COLUMN "mes_wm_item_consume_detail"."updater" IS '';
COMMENT ON COLUMN "mes_wm_item_consume_detail"."update_time" IS '';
COMMENT ON COLUMN "mes_wm_item_consume_detail"."deleted" IS '';
COMMENT ON COLUMN "mes_wm_item_consume_detail"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "mes_wm_batch"
-- ----------------------------
-- DROP TABLE IF EXISTS "mes_wm_batch";
CREATE TABLE IF NOT EXISTS "mes_wm_batch" (
    "id" int8 NOT NULL,
  "code" varchar(128) NULL DEFAULT NULL,
  "item_id" int8 NULL DEFAULT NULL,
  "produce_date" timestamp NULL DEFAULT NULL,
  "expire_date" timestamp NULL DEFAULT NULL,
  "receipt_date" timestamp NULL DEFAULT NULL,
  "vendor_id" int8 NULL DEFAULT NULL,
  "client_id" int8 NULL DEFAULT NULL,
  "sales_order_code" varchar(64) NULL DEFAULT NULL,
  "purchase_order_code" varchar(64) NULL DEFAULT NULL,
  "work_order_id" int8 NULL DEFAULT NULL,
  "task_id" int8 NULL DEFAULT NULL,
  "workstation_id" int8 NULL DEFAULT NULL,
  "tool_id" int8 NULL DEFAULT NULL,
  "mold_id" int8 NULL DEFAULT NULL,
  "lot_number" varchar(128) NULL DEFAULT NULL,
  "quality_status" int4 NULL DEFAULT NULL,
  "remark" varchar(500) NULL DEFAULT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "mes_wm_batch"."id" IS '';
COMMENT ON COLUMN "mes_wm_batch"."code" IS '';
COMMENT ON COLUMN "mes_wm_batch"."item_id" IS '';
COMMENT ON COLUMN "mes_wm_batch"."produce_date" IS '';
COMMENT ON COLUMN "mes_wm_batch"."expire_date" IS '';
COMMENT ON COLUMN "mes_wm_batch"."receipt_date" IS '';
COMMENT ON COLUMN "mes_wm_batch"."vendor_id" IS '';
COMMENT ON COLUMN "mes_wm_batch"."client_id" IS '';
COMMENT ON COLUMN "mes_wm_batch"."sales_order_code" IS '';
COMMENT ON COLUMN "mes_wm_batch"."purchase_order_code" IS '';
COMMENT ON COLUMN "mes_wm_batch"."work_order_id" IS '';
COMMENT ON COLUMN "mes_wm_batch"."task_id" IS '';
COMMENT ON COLUMN "mes_wm_batch"."workstation_id" IS '';
COMMENT ON COLUMN "mes_wm_batch"."tool_id" IS '';
COMMENT ON COLUMN "mes_wm_batch"."mold_id" IS '';
COMMENT ON COLUMN "mes_wm_batch"."lot_number" IS '';
COMMENT ON COLUMN "mes_wm_batch"."quality_status" IS '';
COMMENT ON COLUMN "mes_wm_batch"."remark" IS '';
COMMENT ON COLUMN "mes_wm_batch"."creator" IS '';
COMMENT ON COLUMN "mes_wm_batch"."create_time" IS '';
COMMENT ON COLUMN "mes_wm_batch"."updater" IS '';
COMMENT ON COLUMN "mes_wm_batch"."update_time" IS '';
COMMENT ON COLUMN "mes_wm_batch"."deleted" IS '';
COMMENT ON COLUMN "mes_wm_batch"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "mes_wm_material_stock"
-- ----------------------------
-- DROP TABLE IF EXISTS "mes_wm_material_stock";
CREATE TABLE IF NOT EXISTS "mes_wm_material_stock" (
    "id" int8 NOT NULL,
  "item_type_id" int8 NULL DEFAULT NULL,
  "item_id" int8 NULL DEFAULT NULL,
  "batch_id" int8 NULL DEFAULT NULL,
  "batch_code" varchar(255) NULL DEFAULT NULL,
  "warehouse_id" int8 NULL DEFAULT NULL,
  "location_id" int8 NULL DEFAULT NULL,
  "area_id" int8 NULL DEFAULT NULL,
  "vendor_id" int8 NULL DEFAULT NULL,
  "quantity" numeric(14,2) NULL DEFAULT NULL,
  "receipt_time" timestamp NULL DEFAULT NULL,
  "frozen" int2 NULL DEFAULT 0,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "mes_wm_material_stock"."id" IS '';
COMMENT ON COLUMN "mes_wm_material_stock"."item_type_id" IS '';
COMMENT ON COLUMN "mes_wm_material_stock"."item_id" IS '';
COMMENT ON COLUMN "mes_wm_material_stock"."batch_id" IS '';
COMMENT ON COLUMN "mes_wm_material_stock"."batch_code" IS '';
COMMENT ON COLUMN "mes_wm_material_stock"."warehouse_id" IS '';
COMMENT ON COLUMN "mes_wm_material_stock"."location_id" IS '';
COMMENT ON COLUMN "mes_wm_material_stock"."area_id" IS '';
COMMENT ON COLUMN "mes_wm_material_stock"."vendor_id" IS '';
COMMENT ON COLUMN "mes_wm_material_stock"."quantity" IS '';
COMMENT ON COLUMN "mes_wm_material_stock"."receipt_time" IS '';
COMMENT ON COLUMN "mes_wm_material_stock"."frozen" IS '';
COMMENT ON COLUMN "mes_wm_material_stock"."creator" IS '';
COMMENT ON COLUMN "mes_wm_material_stock"."create_time" IS '';
COMMENT ON COLUMN "mes_wm_material_stock"."updater" IS '';
COMMENT ON COLUMN "mes_wm_material_stock"."update_time" IS '';
COMMENT ON COLUMN "mes_wm_material_stock"."deleted" IS '';
COMMENT ON COLUMN "mes_wm_material_stock"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "mes_pro_task"
-- ----------------------------
-- DROP TABLE IF EXISTS "mes_pro_task";
CREATE TABLE IF NOT EXISTS "mes_pro_task" (
    "id" int8 NOT NULL,
  "code" varchar(64) NULL DEFAULT NULL,
  "name" varchar(255) NULL DEFAULT NULL,
  "work_order_id" int8 NULL DEFAULT NULL,
  "workstation_id" int8 NULL DEFAULT NULL,
  "route_id" int8 NULL DEFAULT NULL,
  "process_id" int8 NULL DEFAULT NULL,
  "item_id" int8 NULL DEFAULT NULL,
  "quantity" numeric(14,2) NULL DEFAULT NULL,
  "produced_quantity" numeric(14,2) NULL DEFAULT NULL,
  "qualify_quantity" numeric(14,2) NULL DEFAULT NULL,
  "unqualify_quantity" numeric(14,2) NULL DEFAULT NULL,
  "changed_quantity" numeric(14,2) NULL DEFAULT NULL,
  "client_id" int8 NULL DEFAULT NULL,
  "start_time" timestamp NULL DEFAULT NULL,
  "duration" int4 NULL DEFAULT NULL,
  "end_time" timestamp NULL DEFAULT NULL,
  "color_code" varchar(20) NULL DEFAULT NULL,
  "finish_date" timestamp NULL DEFAULT NULL,
  "cancel_date" timestamp NULL DEFAULT NULL,
  "status" int4 NULL DEFAULT NULL,
  "remark" varchar(500) NULL DEFAULT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "mes_pro_task"."id" IS '';
COMMENT ON COLUMN "mes_pro_task"."code" IS '';
COMMENT ON COLUMN "mes_pro_task"."name" IS '';
COMMENT ON COLUMN "mes_pro_task"."work_order_id" IS '';
COMMENT ON COLUMN "mes_pro_task"."workstation_id" IS '';
COMMENT ON COLUMN "mes_pro_task"."route_id" IS '';
COMMENT ON COLUMN "mes_pro_task"."process_id" IS '';
COMMENT ON COLUMN "mes_pro_task"."item_id" IS '';
COMMENT ON COLUMN "mes_pro_task"."quantity" IS '';
COMMENT ON COLUMN "mes_pro_task"."produced_quantity" IS '';
COMMENT ON COLUMN "mes_pro_task"."qualify_quantity" IS '';
COMMENT ON COLUMN "mes_pro_task"."unqualify_quantity" IS '';
COMMENT ON COLUMN "mes_pro_task"."changed_quantity" IS '';
COMMENT ON COLUMN "mes_pro_task"."client_id" IS '';
COMMENT ON COLUMN "mes_pro_task"."start_time" IS '';
COMMENT ON COLUMN "mes_pro_task"."duration" IS '';
COMMENT ON COLUMN "mes_pro_task"."end_time" IS '';
COMMENT ON COLUMN "mes_pro_task"."color_code" IS '';
COMMENT ON COLUMN "mes_pro_task"."finish_date" IS '';
COMMENT ON COLUMN "mes_pro_task"."cancel_date" IS '';
COMMENT ON COLUMN "mes_pro_task"."status" IS '';
COMMENT ON COLUMN "mes_pro_task"."remark" IS '';
COMMENT ON COLUMN "mes_pro_task"."creator" IS '';
COMMENT ON COLUMN "mes_pro_task"."create_time" IS '';
COMMENT ON COLUMN "mes_pro_task"."updater" IS '';
COMMENT ON COLUMN "mes_pro_task"."update_time" IS '';
COMMENT ON COLUMN "mes_pro_task"."deleted" IS '';
COMMENT ON COLUMN "mes_pro_task"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "mes_pro_route_process"
-- ----------------------------
-- DROP TABLE IF EXISTS "mes_pro_route_process";
CREATE TABLE IF NOT EXISTS "mes_pro_route_process" (
    "id" int8 NOT NULL,
  "route_id" int8 NULL DEFAULT NULL,
  "process_id" int8 NULL DEFAULT NULL,
  "sort" int4 NULL DEFAULT NULL,
  "next_process_id" int8 NULL DEFAULT NULL,
  "link_type" int4 NULL DEFAULT NULL,
  "prepare_time" int4 NULL DEFAULT NULL,
  "wait_time" int4 NULL DEFAULT NULL,
  "color_code" varchar(20) NULL DEFAULT NULL,
  "key_flag" int2 NULL DEFAULT 0,
  "check_flag" int2 NULL DEFAULT 0,
  "remark" varchar(500) NULL DEFAULT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "mes_pro_route_process"."id" IS '';
COMMENT ON COLUMN "mes_pro_route_process"."route_id" IS '';
COMMENT ON COLUMN "mes_pro_route_process"."process_id" IS '';
COMMENT ON COLUMN "mes_pro_route_process"."sort" IS '';
COMMENT ON COLUMN "mes_pro_route_process"."next_process_id" IS '';
COMMENT ON COLUMN "mes_pro_route_process"."link_type" IS '';
COMMENT ON COLUMN "mes_pro_route_process"."prepare_time" IS '';
COMMENT ON COLUMN "mes_pro_route_process"."wait_time" IS '';
COMMENT ON COLUMN "mes_pro_route_process"."color_code" IS '';
COMMENT ON COLUMN "mes_pro_route_process"."key_flag" IS '';
COMMENT ON COLUMN "mes_pro_route_process"."check_flag" IS '';
COMMENT ON COLUMN "mes_pro_route_process"."remark" IS '';
COMMENT ON COLUMN "mes_pro_route_process"."creator" IS '';
COMMENT ON COLUMN "mes_pro_route_process"."create_time" IS '';
COMMENT ON COLUMN "mes_pro_route_process"."updater" IS '';
COMMENT ON COLUMN "mes_pro_route_process"."update_time" IS '';
COMMENT ON COLUMN "mes_pro_route_process"."deleted" IS '';
COMMENT ON COLUMN "mes_pro_route_process"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "mes_wm_product_sales_detail"
-- ----------------------------
-- DROP TABLE IF EXISTS "mes_wm_product_sales_detail";
CREATE TABLE IF NOT EXISTS "mes_wm_product_sales_detail" (
    "id" int8 NOT NULL,
  "line_id" int8 NOT NULL,
  "sales_id" int8 NOT NULL,
  "item_id" int8 NOT NULL,
  "quantity" numeric(14,2) NULL DEFAULT NULL,
  "material_stock_id" int8 NULL DEFAULT NULL,
  "batch_id" int8 NULL DEFAULT NULL,
  "batch_code" varchar(255) NULL DEFAULT NULL,
  "warehouse_id" int8 NULL DEFAULT NULL,
  "location_id" int8 NULL DEFAULT NULL,
  "area_id" int8 NULL DEFAULT NULL,
  "remark" varchar(500) NULL DEFAULT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "mes_wm_product_sales_detail"."id" IS '';
COMMENT ON COLUMN "mes_wm_product_sales_detail"."line_id" IS '';
COMMENT ON COLUMN "mes_wm_product_sales_detail"."sales_id" IS '';
COMMENT ON COLUMN "mes_wm_product_sales_detail"."item_id" IS '';
COMMENT ON COLUMN "mes_wm_product_sales_detail"."quantity" IS '';
COMMENT ON COLUMN "mes_wm_product_sales_detail"."material_stock_id" IS '';
COMMENT ON COLUMN "mes_wm_product_sales_detail"."batch_id" IS '';
COMMENT ON COLUMN "mes_wm_product_sales_detail"."batch_code" IS '';
COMMENT ON COLUMN "mes_wm_product_sales_detail"."warehouse_id" IS '';
COMMENT ON COLUMN "mes_wm_product_sales_detail"."location_id" IS '';
COMMENT ON COLUMN "mes_wm_product_sales_detail"."area_id" IS '';
COMMENT ON COLUMN "mes_wm_product_sales_detail"."remark" IS '';
COMMENT ON COLUMN "mes_wm_product_sales_detail"."creator" IS '';
COMMENT ON COLUMN "mes_wm_product_sales_detail"."create_time" IS '';
COMMENT ON COLUMN "mes_wm_product_sales_detail"."updater" IS '';
COMMENT ON COLUMN "mes_wm_product_sales_detail"."update_time" IS '';
COMMENT ON COLUMN "mes_wm_product_sales_detail"."deleted" IS '';
COMMENT ON COLUMN "mes_wm_product_sales_detail"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "mes_qc_indicator_result"
-- ----------------------------
-- DROP TABLE IF EXISTS "mes_qc_indicator_result";
CREATE TABLE IF NOT EXISTS "mes_qc_indicator_result" (
    "id" int8 NOT NULL,
  "code" varchar(64) NULL DEFAULT NULL,
  "qc_id" int8 NULL DEFAULT NULL,
  "qc_type" int4 NULL DEFAULT NULL,
  "item_id" int8 NULL DEFAULT NULL,
  "sn" varchar(128) NULL DEFAULT NULL,
  "remark" varchar(500) NULL DEFAULT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "mes_qc_indicator_result"."id" IS '';
COMMENT ON COLUMN "mes_qc_indicator_result"."code" IS '';
COMMENT ON COLUMN "mes_qc_indicator_result"."qc_id" IS '';
COMMENT ON COLUMN "mes_qc_indicator_result"."qc_type" IS '';
COMMENT ON COLUMN "mes_qc_indicator_result"."item_id" IS '';
COMMENT ON COLUMN "mes_qc_indicator_result"."sn" IS '';
COMMENT ON COLUMN "mes_qc_indicator_result"."remark" IS '';
COMMENT ON COLUMN "mes_qc_indicator_result"."creator" IS '';
COMMENT ON COLUMN "mes_qc_indicator_result"."create_time" IS '';
COMMENT ON COLUMN "mes_qc_indicator_result"."updater" IS '';
COMMENT ON COLUMN "mes_qc_indicator_result"."update_time" IS '';
COMMENT ON COLUMN "mes_qc_indicator_result"."deleted" IS '';
COMMENT ON COLUMN "mes_qc_indicator_result"."tenant_id" IS '';


-- ===== yudao-module-wms =====
/*
 Yudao Database Transfer Tool

 Source Server Type    : MySQL

 Target Server Type    : PostgreSQL

 Date: 2026-07-28 23:44:51
*/


-- ----------------------------
-- Table structure for dual
-- ----------------------------
-- DROP TABLE IF EXISTS dual;
CREATE TABLE IF NOT EXISTS dual
(
    id int2
);

COMMENT ON TABLE dual IS '数据库连接的表';

-- ----------------------------
-- Records of dual
-- ----------------------------
-- @formatter:off
-- @formatter:on

-- ----------------------------
-- Table structure for "wms_warehouse"
-- ----------------------------
-- DROP TABLE IF EXISTS "wms_warehouse";
CREATE TABLE IF NOT EXISTS "wms_warehouse" (
    "id" int8 NOT NULL,
  "code" varchar(20) NOT NULL,
  "name" varchar(50) NOT NULL,
  "remark" varchar(255) NULL DEFAULT NULL,
  "sort" int4 NULL DEFAULT 0,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "wms_warehouse"."id" IS '';
COMMENT ON COLUMN "wms_warehouse"."code" IS '';
COMMENT ON COLUMN "wms_warehouse"."name" IS '';
COMMENT ON COLUMN "wms_warehouse"."remark" IS '';
COMMENT ON COLUMN "wms_warehouse"."sort" IS '';
COMMENT ON COLUMN "wms_warehouse"."creator" IS '';
COMMENT ON COLUMN "wms_warehouse"."create_time" IS '';
COMMENT ON COLUMN "wms_warehouse"."updater" IS '';
COMMENT ON COLUMN "wms_warehouse"."update_time" IS '';
COMMENT ON COLUMN "wms_warehouse"."deleted" IS '';
COMMENT ON COLUMN "wms_warehouse"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "wms_merchant"
-- ----------------------------
-- DROP TABLE IF EXISTS "wms_merchant";
CREATE TABLE IF NOT EXISTS "wms_merchant" (
    "id" int8 NOT NULL,
  "code" varchar(20) NOT NULL,
  "name" varchar(60) NOT NULL,
  "type" int2 NOT NULL,
  "level" varchar(10) NULL DEFAULT NULL,
  "bank_name" varchar(255) NULL DEFAULT NULL,
  "bank_account" varchar(40) NULL DEFAULT NULL,
  "address" varchar(200) NULL DEFAULT NULL,
  "mobile" varchar(13) NULL DEFAULT NULL,
  "telephone" varchar(13) NULL DEFAULT NULL,
  "contact" varchar(30) NULL DEFAULT NULL,
  "email" varchar(50) NULL DEFAULT NULL,
  "remark" varchar(255) NULL DEFAULT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "wms_merchant"."id" IS '';
COMMENT ON COLUMN "wms_merchant"."code" IS '';
COMMENT ON COLUMN "wms_merchant"."name" IS '';
COMMENT ON COLUMN "wms_merchant"."type" IS '';
COMMENT ON COLUMN "wms_merchant"."level" IS '';
COMMENT ON COLUMN "wms_merchant"."bank_name" IS '';
COMMENT ON COLUMN "wms_merchant"."bank_account" IS '';
COMMENT ON COLUMN "wms_merchant"."address" IS '';
COMMENT ON COLUMN "wms_merchant"."mobile" IS '';
COMMENT ON COLUMN "wms_merchant"."telephone" IS '';
COMMENT ON COLUMN "wms_merchant"."contact" IS '';
COMMENT ON COLUMN "wms_merchant"."email" IS '';
COMMENT ON COLUMN "wms_merchant"."remark" IS '';
COMMENT ON COLUMN "wms_merchant"."creator" IS '';
COMMENT ON COLUMN "wms_merchant"."create_time" IS '';
COMMENT ON COLUMN "wms_merchant"."updater" IS '';
COMMENT ON COLUMN "wms_merchant"."update_time" IS '';
COMMENT ON COLUMN "wms_merchant"."deleted" IS '';
COMMENT ON COLUMN "wms_merchant"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "wms_item_brand"
-- ----------------------------
-- DROP TABLE IF EXISTS "wms_item_brand";
CREATE TABLE IF NOT EXISTS "wms_item_brand" (
    "id" int8 NOT NULL,
  "code" varchar(20) NOT NULL,
  "name" varchar(30) NOT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "wms_item_brand"."id" IS '';
COMMENT ON COLUMN "wms_item_brand"."code" IS '';
COMMENT ON COLUMN "wms_item_brand"."name" IS '';
COMMENT ON COLUMN "wms_item_brand"."creator" IS '';
COMMENT ON COLUMN "wms_item_brand"."create_time" IS '';
COMMENT ON COLUMN "wms_item_brand"."updater" IS '';
COMMENT ON COLUMN "wms_item_brand"."update_time" IS '';
COMMENT ON COLUMN "wms_item_brand"."deleted" IS '';
COMMENT ON COLUMN "wms_item_brand"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "wms_item_category"
-- ----------------------------
-- DROP TABLE IF EXISTS "wms_item_category";
CREATE TABLE IF NOT EXISTS "wms_item_category" (
    "id" int8 NOT NULL,
  "parent_id" int8 NOT NULL DEFAULT 0,
  "code" varchar(20) NOT NULL,
  "name" varchar(30) NOT NULL,
  "sort" int4 NULL DEFAULT 0,
  "status" int2 NOT NULL DEFAULT 1,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "wms_item_category"."id" IS '';
COMMENT ON COLUMN "wms_item_category"."parent_id" IS '';
COMMENT ON COLUMN "wms_item_category"."code" IS '';
COMMENT ON COLUMN "wms_item_category"."name" IS '';
COMMENT ON COLUMN "wms_item_category"."sort" IS '';
COMMENT ON COLUMN "wms_item_category"."status" IS '';
COMMENT ON COLUMN "wms_item_category"."creator" IS '';
COMMENT ON COLUMN "wms_item_category"."create_time" IS '';
COMMENT ON COLUMN "wms_item_category"."updater" IS '';
COMMENT ON COLUMN "wms_item_category"."update_time" IS '';
COMMENT ON COLUMN "wms_item_category"."deleted" IS '';
COMMENT ON COLUMN "wms_item_category"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "wms_item"
-- ----------------------------
-- DROP TABLE IF EXISTS "wms_item";
CREATE TABLE IF NOT EXISTS "wms_item" (
    "id" int8 NOT NULL,
  "code" varchar(20) NULL DEFAULT NULL,
  "name" varchar(60) NOT NULL,
  "category_id" int8 NOT NULL,
  "unit" varchar(20) NULL DEFAULT NULL,
  "brand_id" int8 NULL DEFAULT NULL,
  "remark" varchar(255) NULL DEFAULT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "wms_item"."id" IS '';
COMMENT ON COLUMN "wms_item"."code" IS '';
COMMENT ON COLUMN "wms_item"."name" IS '';
COMMENT ON COLUMN "wms_item"."category_id" IS '';
COMMENT ON COLUMN "wms_item"."unit" IS '';
COMMENT ON COLUMN "wms_item"."brand_id" IS '';
COMMENT ON COLUMN "wms_item"."remark" IS '';
COMMENT ON COLUMN "wms_item"."creator" IS '';
COMMENT ON COLUMN "wms_item"."create_time" IS '';
COMMENT ON COLUMN "wms_item"."updater" IS '';
COMMENT ON COLUMN "wms_item"."update_time" IS '';
COMMENT ON COLUMN "wms_item"."deleted" IS '';
COMMENT ON COLUMN "wms_item"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "wms_item_sku"
-- ----------------------------
-- DROP TABLE IF EXISTS "wms_item_sku";
CREATE TABLE IF NOT EXISTS "wms_item_sku" (
    "id" int8 NOT NULL,
  "name" varchar(255) NOT NULL,
  "item_id" int8 NOT NULL,
  "bar_code" varchar(64) NULL DEFAULT NULL,
  "code" varchar(64) NULL DEFAULT NULL,
  "length" numeric(10,1) NULL DEFAULT NULL,
  "width" numeric(10,1) NULL DEFAULT NULL,
  "height" numeric(10,1) NULL DEFAULT NULL,
  "gross_weight" numeric(10,3) NULL DEFAULT NULL,
  "net_weight" numeric(10,3) NULL DEFAULT NULL,
  "cost_price" numeric(16,2) NULL DEFAULT NULL,
  "selling_price" numeric(16,2) NULL DEFAULT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "wms_item_sku"."id" IS '';
COMMENT ON COLUMN "wms_item_sku"."name" IS '';
COMMENT ON COLUMN "wms_item_sku"."item_id" IS '';
COMMENT ON COLUMN "wms_item_sku"."bar_code" IS '';
COMMENT ON COLUMN "wms_item_sku"."code" IS '';
COMMENT ON COLUMN "wms_item_sku"."length" IS '';
COMMENT ON COLUMN "wms_item_sku"."width" IS '';
COMMENT ON COLUMN "wms_item_sku"."height" IS '';
COMMENT ON COLUMN "wms_item_sku"."gross_weight" IS '';
COMMENT ON COLUMN "wms_item_sku"."net_weight" IS '';
COMMENT ON COLUMN "wms_item_sku"."cost_price" IS '';
COMMENT ON COLUMN "wms_item_sku"."selling_price" IS '';
COMMENT ON COLUMN "wms_item_sku"."creator" IS '';
COMMENT ON COLUMN "wms_item_sku"."create_time" IS '';
COMMENT ON COLUMN "wms_item_sku"."updater" IS '';
COMMENT ON COLUMN "wms_item_sku"."update_time" IS '';
COMMENT ON COLUMN "wms_item_sku"."deleted" IS '';
COMMENT ON COLUMN "wms_item_sku"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "wms_inventory"
-- ----------------------------
-- DROP TABLE IF EXISTS "wms_inventory";
CREATE TABLE IF NOT EXISTS "wms_inventory" (
    "id" int8 NOT NULL,
  "sku_id" int8 NOT NULL,
  "warehouse_id" int8 NOT NULL,
  "area_id" int8 NOT NULL DEFAULT 0,
  "quantity" numeric(20,2) NOT NULL DEFAULT 0,
  "remark" varchar(255) NULL DEFAULT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_wms_inventory ON "wms_inventory" ("sku_id", "warehouse_id");

COMMENT ON COLUMN "wms_inventory"."id" IS '';
COMMENT ON COLUMN "wms_inventory"."sku_id" IS '';
COMMENT ON COLUMN "wms_inventory"."warehouse_id" IS '';
COMMENT ON COLUMN "wms_inventory"."area_id" IS '';
COMMENT ON COLUMN "wms_inventory"."quantity" IS '';
COMMENT ON COLUMN "wms_inventory"."remark" IS '';
COMMENT ON COLUMN "wms_inventory"."creator" IS '';
COMMENT ON COLUMN "wms_inventory"."create_time" IS '';
COMMENT ON COLUMN "wms_inventory"."updater" IS '';
COMMENT ON COLUMN "wms_inventory"."update_time" IS '';
COMMENT ON COLUMN "wms_inventory"."deleted" IS '';
COMMENT ON COLUMN "wms_inventory"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "wms_inventory_history"
-- ----------------------------
-- DROP TABLE IF EXISTS "wms_inventory_history";
CREATE TABLE IF NOT EXISTS "wms_inventory_history" (
    "id" int8 NOT NULL,
  "warehouse_id" int8 NOT NULL,
  "area_id" int8 NOT NULL DEFAULT 0,
  "sku_id" int8 NOT NULL,
  "quantity" numeric(20,2) NOT NULL DEFAULT 0,
  "before_quantity" numeric(20,2) NULL DEFAULT NULL,
  "after_quantity" numeric(20,2) NULL DEFAULT NULL,
  "batch_no" varchar(64) NULL DEFAULT NULL,
  "production_date" timestamp NULL DEFAULT NULL,
  "expiration_date" timestamp NULL DEFAULT NULL,
  "price" numeric(16,2) NULL DEFAULT NULL,
  "total_price" numeric(16,2) NULL DEFAULT NULL,
  "remark" varchar(255) NULL DEFAULT NULL,
  "order_id" int8 NULL DEFAULT NULL,
  "order_no" varchar(64) NULL DEFAULT NULL,
  "order_type" int4 NULL DEFAULT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "wms_inventory_history"."id" IS '';
COMMENT ON COLUMN "wms_inventory_history"."warehouse_id" IS '';
COMMENT ON COLUMN "wms_inventory_history"."area_id" IS '';
COMMENT ON COLUMN "wms_inventory_history"."sku_id" IS '';
COMMENT ON COLUMN "wms_inventory_history"."quantity" IS '';
COMMENT ON COLUMN "wms_inventory_history"."before_quantity" IS '';
COMMENT ON COLUMN "wms_inventory_history"."after_quantity" IS '';
COMMENT ON COLUMN "wms_inventory_history"."batch_no" IS '';
COMMENT ON COLUMN "wms_inventory_history"."production_date" IS '';
COMMENT ON COLUMN "wms_inventory_history"."expiration_date" IS '';
COMMENT ON COLUMN "wms_inventory_history"."price" IS '';
COMMENT ON COLUMN "wms_inventory_history"."total_price" IS '';
COMMENT ON COLUMN "wms_inventory_history"."remark" IS '';
COMMENT ON COLUMN "wms_inventory_history"."order_id" IS '';
COMMENT ON COLUMN "wms_inventory_history"."order_no" IS '';
COMMENT ON COLUMN "wms_inventory_history"."order_type" IS '';
COMMENT ON COLUMN "wms_inventory_history"."creator" IS '';
COMMENT ON COLUMN "wms_inventory_history"."create_time" IS '';
COMMENT ON COLUMN "wms_inventory_history"."updater" IS '';
COMMENT ON COLUMN "wms_inventory_history"."update_time" IS '';
COMMENT ON COLUMN "wms_inventory_history"."deleted" IS '';
COMMENT ON COLUMN "wms_inventory_history"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "wms_receipt_order"
-- ----------------------------
-- DROP TABLE IF EXISTS "wms_receipt_order";
CREATE TABLE IF NOT EXISTS "wms_receipt_order" (
    "id" int8 NOT NULL,
  "no" varchar(64) NOT NULL,
  "type" int4 NOT NULL,
  "order_time" timestamp NOT NULL,
  "status" int4 NOT NULL DEFAULT 0,
  "biz_order_no" varchar(64) NULL DEFAULT NULL,
  "merchant_id" int8 NULL DEFAULT NULL,
  "remark" varchar(255) NULL DEFAULT NULL,
  "warehouse_id" int8 NOT NULL,
  "area_id" int8 NOT NULL DEFAULT 0,
  "total_quantity" numeric(20,2) NOT NULL DEFAULT 0,
  "total_price" numeric(16,2) NULL DEFAULT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "wms_receipt_order"."id" IS '';
COMMENT ON COLUMN "wms_receipt_order"."no" IS '';
COMMENT ON COLUMN "wms_receipt_order"."type" IS '';
COMMENT ON COLUMN "wms_receipt_order"."order_time" IS '';
COMMENT ON COLUMN "wms_receipt_order"."status" IS '';
COMMENT ON COLUMN "wms_receipt_order"."biz_order_no" IS '';
COMMENT ON COLUMN "wms_receipt_order"."merchant_id" IS '';
COMMENT ON COLUMN "wms_receipt_order"."remark" IS '';
COMMENT ON COLUMN "wms_receipt_order"."warehouse_id" IS '';
COMMENT ON COLUMN "wms_receipt_order"."area_id" IS '';
COMMENT ON COLUMN "wms_receipt_order"."total_quantity" IS '';
COMMENT ON COLUMN "wms_receipt_order"."total_price" IS '';
COMMENT ON COLUMN "wms_receipt_order"."creator" IS '';
COMMENT ON COLUMN "wms_receipt_order"."create_time" IS '';
COMMENT ON COLUMN "wms_receipt_order"."updater" IS '';
COMMENT ON COLUMN "wms_receipt_order"."update_time" IS '';
COMMENT ON COLUMN "wms_receipt_order"."deleted" IS '';
COMMENT ON COLUMN "wms_receipt_order"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "wms_receipt_order_detail"
-- ----------------------------
-- DROP TABLE IF EXISTS "wms_receipt_order_detail";
CREATE TABLE IF NOT EXISTS "wms_receipt_order_detail" (
    "id" int8 NOT NULL,
  "order_id" int8 NOT NULL,
  "sku_id" int8 NOT NULL,
  "warehouse_id" int8 NOT NULL,
  "area_id" int8 NOT NULL DEFAULT 0,
  "batch_no" varchar(64) NULL DEFAULT NULL,
  "production_date" timestamp NULL DEFAULT NULL,
  "expiration_date" timestamp NULL DEFAULT NULL,
  "quantity" numeric(20,2) NOT NULL DEFAULT 0,
  "price" numeric(16,2) NULL DEFAULT NULL,
  "total_price" numeric(16,2) NULL DEFAULT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "wms_receipt_order_detail"."id" IS '';
COMMENT ON COLUMN "wms_receipt_order_detail"."order_id" IS '';
COMMENT ON COLUMN "wms_receipt_order_detail"."sku_id" IS '';
COMMENT ON COLUMN "wms_receipt_order_detail"."warehouse_id" IS '';
COMMENT ON COLUMN "wms_receipt_order_detail"."area_id" IS '';
COMMENT ON COLUMN "wms_receipt_order_detail"."batch_no" IS '';
COMMENT ON COLUMN "wms_receipt_order_detail"."production_date" IS '';
COMMENT ON COLUMN "wms_receipt_order_detail"."expiration_date" IS '';
COMMENT ON COLUMN "wms_receipt_order_detail"."quantity" IS '';
COMMENT ON COLUMN "wms_receipt_order_detail"."price" IS '';
COMMENT ON COLUMN "wms_receipt_order_detail"."total_price" IS '';
COMMENT ON COLUMN "wms_receipt_order_detail"."creator" IS '';
COMMENT ON COLUMN "wms_receipt_order_detail"."create_time" IS '';
COMMENT ON COLUMN "wms_receipt_order_detail"."updater" IS '';
COMMENT ON COLUMN "wms_receipt_order_detail"."update_time" IS '';
COMMENT ON COLUMN "wms_receipt_order_detail"."deleted" IS '';
COMMENT ON COLUMN "wms_receipt_order_detail"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "wms_shipment_order"
-- ----------------------------
-- DROP TABLE IF EXISTS "wms_shipment_order";
CREATE TABLE IF NOT EXISTS "wms_shipment_order" (
    "id" int8 NOT NULL,
  "no" varchar(64) NOT NULL,
  "type" int4 NOT NULL,
  "order_time" timestamp NOT NULL,
  "status" int4 NOT NULL DEFAULT 0,
  "biz_order_no" varchar(64) NULL DEFAULT NULL,
  "merchant_id" int8 NULL DEFAULT NULL,
  "remark" varchar(255) NULL DEFAULT NULL,
  "warehouse_id" int8 NOT NULL,
  "area_id" int8 NOT NULL DEFAULT 0,
  "total_quantity" numeric(20,2) NOT NULL DEFAULT 0,
  "total_price" numeric(16,2) NULL DEFAULT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "wms_shipment_order"."id" IS '';
COMMENT ON COLUMN "wms_shipment_order"."no" IS '';
COMMENT ON COLUMN "wms_shipment_order"."type" IS '';
COMMENT ON COLUMN "wms_shipment_order"."order_time" IS '';
COMMENT ON COLUMN "wms_shipment_order"."status" IS '';
COMMENT ON COLUMN "wms_shipment_order"."biz_order_no" IS '';
COMMENT ON COLUMN "wms_shipment_order"."merchant_id" IS '';
COMMENT ON COLUMN "wms_shipment_order"."remark" IS '';
COMMENT ON COLUMN "wms_shipment_order"."warehouse_id" IS '';
COMMENT ON COLUMN "wms_shipment_order"."area_id" IS '';
COMMENT ON COLUMN "wms_shipment_order"."total_quantity" IS '';
COMMENT ON COLUMN "wms_shipment_order"."total_price" IS '';
COMMENT ON COLUMN "wms_shipment_order"."creator" IS '';
COMMENT ON COLUMN "wms_shipment_order"."create_time" IS '';
COMMENT ON COLUMN "wms_shipment_order"."updater" IS '';
COMMENT ON COLUMN "wms_shipment_order"."update_time" IS '';
COMMENT ON COLUMN "wms_shipment_order"."deleted" IS '';
COMMENT ON COLUMN "wms_shipment_order"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "wms_shipment_order_detail"
-- ----------------------------
-- DROP TABLE IF EXISTS "wms_shipment_order_detail";
CREATE TABLE IF NOT EXISTS "wms_shipment_order_detail" (
    "id" int8 NOT NULL,
  "order_id" int8 NOT NULL,
  "sku_id" int8 NOT NULL,
  "warehouse_id" int8 NOT NULL,
  "area_id" int8 NOT NULL DEFAULT 0,
  "inventory_detail_id" int8 NULL DEFAULT NULL,
  "batch_no" varchar(64) NULL DEFAULT NULL,
  "production_date" timestamp NULL DEFAULT NULL,
  "expiration_date" timestamp NULL DEFAULT NULL,
  "quantity" numeric(20,2) NOT NULL DEFAULT 0,
  "price" numeric(16,2) NULL DEFAULT NULL,
  "total_price" numeric(16,2) NULL DEFAULT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "wms_shipment_order_detail"."id" IS '';
COMMENT ON COLUMN "wms_shipment_order_detail"."order_id" IS '';
COMMENT ON COLUMN "wms_shipment_order_detail"."sku_id" IS '';
COMMENT ON COLUMN "wms_shipment_order_detail"."warehouse_id" IS '';
COMMENT ON COLUMN "wms_shipment_order_detail"."area_id" IS '';
COMMENT ON COLUMN "wms_shipment_order_detail"."inventory_detail_id" IS '';
COMMENT ON COLUMN "wms_shipment_order_detail"."batch_no" IS '';
COMMENT ON COLUMN "wms_shipment_order_detail"."production_date" IS '';
COMMENT ON COLUMN "wms_shipment_order_detail"."expiration_date" IS '';
COMMENT ON COLUMN "wms_shipment_order_detail"."quantity" IS '';
COMMENT ON COLUMN "wms_shipment_order_detail"."price" IS '';
COMMENT ON COLUMN "wms_shipment_order_detail"."total_price" IS '';
COMMENT ON COLUMN "wms_shipment_order_detail"."creator" IS '';
COMMENT ON COLUMN "wms_shipment_order_detail"."create_time" IS '';
COMMENT ON COLUMN "wms_shipment_order_detail"."updater" IS '';
COMMENT ON COLUMN "wms_shipment_order_detail"."update_time" IS '';
COMMENT ON COLUMN "wms_shipment_order_detail"."deleted" IS '';
COMMENT ON COLUMN "wms_shipment_order_detail"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "wms_movement_order"
-- ----------------------------
-- DROP TABLE IF EXISTS "wms_movement_order";
CREATE TABLE IF NOT EXISTS "wms_movement_order" (
    "id" int8 NOT NULL,
  "no" varchar(64) NOT NULL,
  "order_time" timestamp NOT NULL,
  "status" int4 NOT NULL DEFAULT 0,
  "remark" varchar(255) NULL DEFAULT NULL,
  "source_warehouse_id" int8 NOT NULL,
  "target_warehouse_id" int8 NOT NULL,
  "total_quantity" numeric(20,2) NOT NULL DEFAULT 0,
  "total_price" numeric(16,2) NULL DEFAULT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "wms_movement_order"."id" IS '';
COMMENT ON COLUMN "wms_movement_order"."no" IS '';
COMMENT ON COLUMN "wms_movement_order"."order_time" IS '';
COMMENT ON COLUMN "wms_movement_order"."status" IS '';
COMMENT ON COLUMN "wms_movement_order"."remark" IS '';
COMMENT ON COLUMN "wms_movement_order"."source_warehouse_id" IS '';
COMMENT ON COLUMN "wms_movement_order"."target_warehouse_id" IS '';
COMMENT ON COLUMN "wms_movement_order"."total_quantity" IS '';
COMMENT ON COLUMN "wms_movement_order"."total_price" IS '';
COMMENT ON COLUMN "wms_movement_order"."creator" IS '';
COMMENT ON COLUMN "wms_movement_order"."create_time" IS '';
COMMENT ON COLUMN "wms_movement_order"."updater" IS '';
COMMENT ON COLUMN "wms_movement_order"."update_time" IS '';
COMMENT ON COLUMN "wms_movement_order"."deleted" IS '';
COMMENT ON COLUMN "wms_movement_order"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "wms_movement_order_detail"
-- ----------------------------
-- DROP TABLE IF EXISTS "wms_movement_order_detail";
CREATE TABLE IF NOT EXISTS "wms_movement_order_detail" (
    "id" int8 NOT NULL,
  "order_id" int8 NOT NULL,
  "sku_id" int8 NOT NULL,
  "source_warehouse_id" int8 NOT NULL,
  "target_warehouse_id" int8 NOT NULL,
  "inventory_detail_id" int8 NULL DEFAULT NULL,
  "batch_no" varchar(64) NULL DEFAULT NULL,
  "production_date" timestamp NULL DEFAULT NULL,
  "expiration_date" timestamp NULL DEFAULT NULL,
  "quantity" numeric(20,2) NOT NULL DEFAULT 0,
  "price" numeric(16,2) NULL DEFAULT NULL,
  "total_price" numeric(16,2) NULL DEFAULT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "wms_movement_order_detail"."id" IS '';
COMMENT ON COLUMN "wms_movement_order_detail"."order_id" IS '';
COMMENT ON COLUMN "wms_movement_order_detail"."sku_id" IS '';
COMMENT ON COLUMN "wms_movement_order_detail"."source_warehouse_id" IS '';
COMMENT ON COLUMN "wms_movement_order_detail"."target_warehouse_id" IS '';
COMMENT ON COLUMN "wms_movement_order_detail"."inventory_detail_id" IS '';
COMMENT ON COLUMN "wms_movement_order_detail"."batch_no" IS '';
COMMENT ON COLUMN "wms_movement_order_detail"."production_date" IS '';
COMMENT ON COLUMN "wms_movement_order_detail"."expiration_date" IS '';
COMMENT ON COLUMN "wms_movement_order_detail"."quantity" IS '';
COMMENT ON COLUMN "wms_movement_order_detail"."price" IS '';
COMMENT ON COLUMN "wms_movement_order_detail"."total_price" IS '';
COMMENT ON COLUMN "wms_movement_order_detail"."creator" IS '';
COMMENT ON COLUMN "wms_movement_order_detail"."create_time" IS '';
COMMENT ON COLUMN "wms_movement_order_detail"."updater" IS '';
COMMENT ON COLUMN "wms_movement_order_detail"."update_time" IS '';
COMMENT ON COLUMN "wms_movement_order_detail"."deleted" IS '';
COMMENT ON COLUMN "wms_movement_order_detail"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "wms_check_order"
-- ----------------------------
-- DROP TABLE IF EXISTS "wms_check_order";
CREATE TABLE IF NOT EXISTS "wms_check_order" (
    "id" int8 NOT NULL,
  "no" varchar(64) NOT NULL,
  "order_time" timestamp NOT NULL,
  "status" int4 NOT NULL DEFAULT 0,
  "remark" varchar(255) NULL DEFAULT NULL,
  "warehouse_id" int8 NOT NULL,
  "area_id" int8 NOT NULL DEFAULT 0,
  "total_quantity" numeric(20,2) NOT NULL DEFAULT 0,
  "total_price" numeric(16,2) NULL DEFAULT NULL,
  "actual_price" numeric(16,2) NULL DEFAULT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "wms_check_order"."id" IS '';
COMMENT ON COLUMN "wms_check_order"."no" IS '';
COMMENT ON COLUMN "wms_check_order"."order_time" IS '';
COMMENT ON COLUMN "wms_check_order"."status" IS '';
COMMENT ON COLUMN "wms_check_order"."remark" IS '';
COMMENT ON COLUMN "wms_check_order"."warehouse_id" IS '';
COMMENT ON COLUMN "wms_check_order"."area_id" IS '';
COMMENT ON COLUMN "wms_check_order"."total_quantity" IS '';
COMMENT ON COLUMN "wms_check_order"."total_price" IS '';
COMMENT ON COLUMN "wms_check_order"."actual_price" IS '';
COMMENT ON COLUMN "wms_check_order"."creator" IS '';
COMMENT ON COLUMN "wms_check_order"."create_time" IS '';
COMMENT ON COLUMN "wms_check_order"."updater" IS '';
COMMENT ON COLUMN "wms_check_order"."update_time" IS '';
COMMENT ON COLUMN "wms_check_order"."deleted" IS '';
COMMENT ON COLUMN "wms_check_order"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "wms_check_order_detail"
-- ----------------------------
-- DROP TABLE IF EXISTS "wms_check_order_detail";
CREATE TABLE IF NOT EXISTS "wms_check_order_detail" (
    "id" int8 NOT NULL,
  "order_id" int8 NOT NULL,
  "sku_id" int8 NOT NULL,
  "warehouse_id" int8 NOT NULL,
  "area_id" int8 NOT NULL DEFAULT 0,
  "inventory_id" int8 NULL DEFAULT NULL,
  "inventory_detail_id" int8 NULL DEFAULT NULL,
  "batch_no" varchar(64) NULL DEFAULT NULL,
  "production_date" timestamp NULL DEFAULT NULL,
  "expiration_date" timestamp NULL DEFAULT NULL,
  "receipt_time" timestamp NULL DEFAULT NULL,
  "quantity" numeric(20,2) NOT NULL DEFAULT 0,
  "check_quantity" numeric(20,2) NOT NULL DEFAULT 0,
  "price" numeric(16,2) NULL DEFAULT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "wms_check_order_detail"."id" IS '';
COMMENT ON COLUMN "wms_check_order_detail"."order_id" IS '';
COMMENT ON COLUMN "wms_check_order_detail"."sku_id" IS '';
COMMENT ON COLUMN "wms_check_order_detail"."warehouse_id" IS '';
COMMENT ON COLUMN "wms_check_order_detail"."area_id" IS '';
COMMENT ON COLUMN "wms_check_order_detail"."inventory_id" IS '';
COMMENT ON COLUMN "wms_check_order_detail"."inventory_detail_id" IS '';
COMMENT ON COLUMN "wms_check_order_detail"."batch_no" IS '';
COMMENT ON COLUMN "wms_check_order_detail"."production_date" IS '';
COMMENT ON COLUMN "wms_check_order_detail"."expiration_date" IS '';
COMMENT ON COLUMN "wms_check_order_detail"."receipt_time" IS '';
COMMENT ON COLUMN "wms_check_order_detail"."quantity" IS '';
COMMENT ON COLUMN "wms_check_order_detail"."check_quantity" IS '';
COMMENT ON COLUMN "wms_check_order_detail"."price" IS '';
COMMENT ON COLUMN "wms_check_order_detail"."creator" IS '';
COMMENT ON COLUMN "wms_check_order_detail"."create_time" IS '';
COMMENT ON COLUMN "wms_check_order_detail"."updater" IS '';
COMMENT ON COLUMN "wms_check_order_detail"."update_time" IS '';
COMMENT ON COLUMN "wms_check_order_detail"."deleted" IS '';
COMMENT ON COLUMN "wms_check_order_detail"."tenant_id" IS '';


-- ===== yudao-module-im =====
/*
 Yudao Database Transfer Tool

 Source Server Type    : MySQL

 Target Server Type    : PostgreSQL

 Date: 2026-07-28 23:44:51
*/


-- ----------------------------
-- Table structure for dual
-- ----------------------------
-- DROP TABLE IF EXISTS dual;
CREATE TABLE IF NOT EXISTS dual
(
    id int2
);

COMMENT ON TABLE dual IS '数据库连接的表';

-- ----------------------------
-- Records of dual
-- ----------------------------
-- @formatter:off
-- @formatter:on

-- ----------------------------
-- Table structure for "im_private_message"
-- ----------------------------
-- DROP TABLE IF EXISTS "im_private_message";
CREATE TABLE IF NOT EXISTS "im_private_message" (
    "id" int8 NOT NULL,
  "client_message_id" varchar(64) NULL DEFAULT NULL,
  "sender_id" int8 NOT NULL,
  "receiver_id" int8 NOT NULL,
  "type" int2 NOT NULL,
  "content" varchar(8192) NULL DEFAULT NULL,
  "status" int2 NOT NULL,
  "receipt_status" int2 NOT NULL DEFAULT 0,
  "send_time" timestamp NOT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_im_private_message ON "im_private_message" ("sender_id", "client_message_id", "tenant_id");

COMMENT ON COLUMN "im_private_message"."id" IS '';
COMMENT ON COLUMN "im_private_message"."client_message_id" IS '';
COMMENT ON COLUMN "im_private_message"."sender_id" IS '';
COMMENT ON COLUMN "im_private_message"."receiver_id" IS '';
COMMENT ON COLUMN "im_private_message"."type" IS '';
COMMENT ON COLUMN "im_private_message"."content" IS '';
COMMENT ON COLUMN "im_private_message"."status" IS '';
COMMENT ON COLUMN "im_private_message"."receipt_status" IS '';
COMMENT ON COLUMN "im_private_message"."send_time" IS '';
COMMENT ON COLUMN "im_private_message"."creator" IS '';
COMMENT ON COLUMN "im_private_message"."create_time" IS '';
COMMENT ON COLUMN "im_private_message"."updater" IS '';
COMMENT ON COLUMN "im_private_message"."update_time" IS '';
COMMENT ON COLUMN "im_private_message"."deleted" IS '';
COMMENT ON COLUMN "im_private_message"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "im_group_message"
-- ----------------------------
-- DROP TABLE IF EXISTS "im_group_message";
CREATE TABLE IF NOT EXISTS "im_group_message" (
    "id" int8 NOT NULL,
  "client_message_id" varchar(64) NULL DEFAULT NULL,
  "sender_id" int8 NOT NULL,
  "group_id" int8 NOT NULL,
  "type" int2 NOT NULL,
  "content" varchar(8192) NULL DEFAULT NULL,
  "status" int2 NOT NULL,
  "send_time" timestamp NOT NULL,
  "receiver_user_ids" text NULL DEFAULT NULL,
  "at_user_ids" varchar(1024) NULL DEFAULT NULL,
  "receipt_status" int2 NOT NULL DEFAULT 0,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_im_group_message ON "im_group_message" ("sender_id", "client_message_id", "tenant_id");

COMMENT ON COLUMN "im_group_message"."id" IS '';
COMMENT ON COLUMN "im_group_message"."client_message_id" IS '';
COMMENT ON COLUMN "im_group_message"."sender_id" IS '';
COMMENT ON COLUMN "im_group_message"."group_id" IS '';
COMMENT ON COLUMN "im_group_message"."type" IS '';
COMMENT ON COLUMN "im_group_message"."content" IS '';
COMMENT ON COLUMN "im_group_message"."status" IS '';
COMMENT ON COLUMN "im_group_message"."send_time" IS '';
COMMENT ON COLUMN "im_group_message"."receiver_user_ids" IS '';
COMMENT ON COLUMN "im_group_message"."at_user_ids" IS '';
COMMENT ON COLUMN "im_group_message"."receipt_status" IS '';
COMMENT ON COLUMN "im_group_message"."creator" IS '';
COMMENT ON COLUMN "im_group_message"."create_time" IS '';
COMMENT ON COLUMN "im_group_message"."updater" IS '';
COMMENT ON COLUMN "im_group_message"."update_time" IS '';
COMMENT ON COLUMN "im_group_message"."deleted" IS '';
COMMENT ON COLUMN "im_group_message"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "im_group"
-- ----------------------------
-- DROP TABLE IF EXISTS "im_group";
CREATE TABLE IF NOT EXISTS "im_group" (
    "id" int8 NOT NULL,
  "name" varchar(64) NOT NULL,
  "owner_user_id" int8 NOT NULL,
  "avatar" varchar(512) NULL DEFAULT NULL,
  "notice" varchar(2048) NULL DEFAULT NULL,
  "banned" int2 NULL DEFAULT 0,
  "banned_reason" varchar(512) NULL DEFAULT NULL,
  "banned_time" timestamp NULL DEFAULT NULL,
  "status" int2 NOT NULL,
  "dissolved_time" timestamp NULL DEFAULT NULL,
  "muted_all" int2 NULL DEFAULT 0,
  "join_approval" int2 NOT NULL DEFAULT 0,
  "pinned_message_ids" varchar(128) NULL DEFAULT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "im_group"."id" IS '';
COMMENT ON COLUMN "im_group"."name" IS '';
COMMENT ON COLUMN "im_group"."owner_user_id" IS '';
COMMENT ON COLUMN "im_group"."avatar" IS '';
COMMENT ON COLUMN "im_group"."notice" IS '';
COMMENT ON COLUMN "im_group"."banned" IS '';
COMMENT ON COLUMN "im_group"."banned_reason" IS '';
COMMENT ON COLUMN "im_group"."banned_time" IS '';
COMMENT ON COLUMN "im_group"."status" IS '';
COMMENT ON COLUMN "im_group"."dissolved_time" IS '';
COMMENT ON COLUMN "im_group"."muted_all" IS '';
COMMENT ON COLUMN "im_group"."join_approval" IS '';
COMMENT ON COLUMN "im_group"."pinned_message_ids" IS '';
COMMENT ON COLUMN "im_group"."creator" IS '';
COMMENT ON COLUMN "im_group"."create_time" IS '';
COMMENT ON COLUMN "im_group"."updater" IS '';
COMMENT ON COLUMN "im_group"."update_time" IS '';
COMMENT ON COLUMN "im_group"."deleted" IS '';
COMMENT ON COLUMN "im_group"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "im_group_member"
-- ----------------------------
-- DROP TABLE IF EXISTS "im_group_member";
CREATE TABLE IF NOT EXISTS "im_group_member" (
    "id" int8 NOT NULL,
  "group_id" int8 NOT NULL,
  "user_id" int8 NOT NULL,
  "display_user_name" varchar(64) NULL DEFAULT NULL,
  "group_remark" varchar(64) NULL DEFAULT NULL,
  "silent" int2 NULL DEFAULT 0,
  "status" int2 NOT NULL,
  "role" int2 NOT NULL DEFAULT 3,
  "join_time" timestamp NULL DEFAULT NULL,
  "add_source" int2 NULL DEFAULT NULL,
  "inviter_user_id" int8 NULL DEFAULT NULL,
  "quit_time" timestamp NULL DEFAULT NULL,
  "mute_end_time" timestamp NULL DEFAULT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_im_group_member ON "im_group_member" ("group_id", "user_id", "tenant_id");

COMMENT ON COLUMN "im_group_member"."id" IS '';
COMMENT ON COLUMN "im_group_member"."group_id" IS '';
COMMENT ON COLUMN "im_group_member"."user_id" IS '';
COMMENT ON COLUMN "im_group_member"."display_user_name" IS '';
COMMENT ON COLUMN "im_group_member"."group_remark" IS '';
COMMENT ON COLUMN "im_group_member"."silent" IS '';
COMMENT ON COLUMN "im_group_member"."status" IS '';
COMMENT ON COLUMN "im_group_member"."role" IS '';
COMMENT ON COLUMN "im_group_member"."join_time" IS '';
COMMENT ON COLUMN "im_group_member"."add_source" IS '';
COMMENT ON COLUMN "im_group_member"."inviter_user_id" IS '';
COMMENT ON COLUMN "im_group_member"."quit_time" IS '';
COMMENT ON COLUMN "im_group_member"."mute_end_time" IS '';
COMMENT ON COLUMN "im_group_member"."creator" IS '';
COMMENT ON COLUMN "im_group_member"."create_time" IS '';
COMMENT ON COLUMN "im_group_member"."updater" IS '';
COMMENT ON COLUMN "im_group_member"."update_time" IS '';
COMMENT ON COLUMN "im_group_member"."deleted" IS '';
COMMENT ON COLUMN "im_group_member"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "im_friend"
-- ----------------------------
-- DROP TABLE IF EXISTS "im_friend";
CREATE TABLE IF NOT EXISTS "im_friend" (
    "id" int8 NOT NULL,
  "user_id" int8 NOT NULL,
  "friend_user_id" int8 NOT NULL,
  "silent" int2 NULL DEFAULT 0,
  "display_name" varchar(64) NOT NULL DEFAULT '',
  "add_source" int2 NULL DEFAULT NULL,
  "pinned" int2 NULL DEFAULT 0,
  "blocked" int2 NULL DEFAULT 0,
  "status" int2 NOT NULL,
  "add_time" timestamp NULL DEFAULT NULL,
  "delete_time" timestamp NULL DEFAULT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_im_friend ON "im_friend" ("user_id", "friend_user_id", "tenant_id");

COMMENT ON COLUMN "im_friend"."id" IS '';
COMMENT ON COLUMN "im_friend"."user_id" IS '';
COMMENT ON COLUMN "im_friend"."friend_user_id" IS '';
COMMENT ON COLUMN "im_friend"."silent" IS '';
COMMENT ON COLUMN "im_friend"."display_name" IS '';
COMMENT ON COLUMN "im_friend"."add_source" IS '';
COMMENT ON COLUMN "im_friend"."pinned" IS '';
COMMENT ON COLUMN "im_friend"."blocked" IS '';
COMMENT ON COLUMN "im_friend"."status" IS '';
COMMENT ON COLUMN "im_friend"."add_time" IS '';
COMMENT ON COLUMN "im_friend"."delete_time" IS '';
COMMENT ON COLUMN "im_friend"."creator" IS '';
COMMENT ON COLUMN "im_friend"."create_time" IS '';
COMMENT ON COLUMN "im_friend"."updater" IS '';
COMMENT ON COLUMN "im_friend"."update_time" IS '';
COMMENT ON COLUMN "im_friend"."deleted" IS '';
COMMENT ON COLUMN "im_friend"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "im_friend_request"
-- ----------------------------
-- DROP TABLE IF EXISTS "im_friend_request";
CREATE TABLE IF NOT EXISTS "im_friend_request" (
    "id" int8 NOT NULL,
  "from_user_id" int8 NOT NULL,
  "to_user_id" int8 NOT NULL,
  "handle_result" int2 NOT NULL DEFAULT 0,
  "apply_content" varchar(255) NULL DEFAULT NULL,
  "handle_content" varchar(255) NULL DEFAULT NULL,
  "display_name" varchar(64) NULL DEFAULT NULL,
  "add_source" int2 NULL DEFAULT NULL,
  "handle_time" timestamp NULL DEFAULT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_im_friend_request ON "im_friend_request" ("from_user_id", "to_user_id", "tenant_id");

COMMENT ON COLUMN "im_friend_request"."id" IS '';
COMMENT ON COLUMN "im_friend_request"."from_user_id" IS '';
COMMENT ON COLUMN "im_friend_request"."to_user_id" IS '';
COMMENT ON COLUMN "im_friend_request"."handle_result" IS '';
COMMENT ON COLUMN "im_friend_request"."apply_content" IS '';
COMMENT ON COLUMN "im_friend_request"."handle_content" IS '';
COMMENT ON COLUMN "im_friend_request"."display_name" IS '';
COMMENT ON COLUMN "im_friend_request"."add_source" IS '';
COMMENT ON COLUMN "im_friend_request"."handle_time" IS '';
COMMENT ON COLUMN "im_friend_request"."creator" IS '';
COMMENT ON COLUMN "im_friend_request"."create_time" IS '';
COMMENT ON COLUMN "im_friend_request"."updater" IS '';
COMMENT ON COLUMN "im_friend_request"."update_time" IS '';
COMMENT ON COLUMN "im_friend_request"."deleted" IS '';
COMMENT ON COLUMN "im_friend_request"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "im_group_request"
-- ----------------------------
-- DROP TABLE IF EXISTS "im_group_request";
CREATE TABLE IF NOT EXISTS "im_group_request" (
    "id" int8 NOT NULL,
  "group_id" int8 NOT NULL,
  "user_id" int8 NOT NULL,
  "inviter_user_id" int8 NULL DEFAULT NULL,
  "apply_content" varchar(255) NULL DEFAULT NULL,
  "add_source" int2 NULL DEFAULT NULL,
  "handle_result" int2 NOT NULL DEFAULT 0,
  "handle_user_id" int8 NULL DEFAULT NULL,
  "handle_content" varchar(255) NULL DEFAULT NULL,
  "handle_time" timestamp NULL DEFAULT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_im_group_request ON "im_group_request" ("group_id", "user_id", "tenant_id");

COMMENT ON COLUMN "im_group_request"."id" IS '';
COMMENT ON COLUMN "im_group_request"."group_id" IS '';
COMMENT ON COLUMN "im_group_request"."user_id" IS '';
COMMENT ON COLUMN "im_group_request"."inviter_user_id" IS '';
COMMENT ON COLUMN "im_group_request"."apply_content" IS '';
COMMENT ON COLUMN "im_group_request"."add_source" IS '';
COMMENT ON COLUMN "im_group_request"."handle_result" IS '';
COMMENT ON COLUMN "im_group_request"."handle_user_id" IS '';
COMMENT ON COLUMN "im_group_request"."handle_content" IS '';
COMMENT ON COLUMN "im_group_request"."handle_time" IS '';
COMMENT ON COLUMN "im_group_request"."creator" IS '';
COMMENT ON COLUMN "im_group_request"."create_time" IS '';
COMMENT ON COLUMN "im_group_request"."updater" IS '';
COMMENT ON COLUMN "im_group_request"."update_time" IS '';
COMMENT ON COLUMN "im_group_request"."deleted" IS '';
COMMENT ON COLUMN "im_group_request"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "im_face_pack"
-- ----------------------------
-- DROP TABLE IF EXISTS "im_face_pack";
CREATE TABLE IF NOT EXISTS "im_face_pack" (
    "id" int8 NOT NULL,
  "name" varchar(64) NOT NULL,
  "icon" varchar(512) NULL DEFAULT NULL,
  "sort" int4 NOT NULL DEFAULT 0,
  "status" int2 NOT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "im_face_pack"."id" IS '';
COMMENT ON COLUMN "im_face_pack"."name" IS '';
COMMENT ON COLUMN "im_face_pack"."icon" IS '';
COMMENT ON COLUMN "im_face_pack"."sort" IS '';
COMMENT ON COLUMN "im_face_pack"."status" IS '';
COMMENT ON COLUMN "im_face_pack"."creator" IS '';
COMMENT ON COLUMN "im_face_pack"."create_time" IS '';
COMMENT ON COLUMN "im_face_pack"."updater" IS '';
COMMENT ON COLUMN "im_face_pack"."update_time" IS '';
COMMENT ON COLUMN "im_face_pack"."deleted" IS '';
COMMENT ON COLUMN "im_face_pack"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "im_face_pack_item"
-- ----------------------------
-- DROP TABLE IF EXISTS "im_face_pack_item";
CREATE TABLE IF NOT EXISTS "im_face_pack_item" (
    "id" int8 NOT NULL,
  "pack_id" int8 NOT NULL,
  "url" varchar(512) NOT NULL,
  "name" varchar(64) NULL DEFAULT NULL,
  "width" int4 NOT NULL DEFAULT 0,
  "height" int4 NOT NULL DEFAULT 0,
  "sort" int4 NOT NULL DEFAULT 0,
  "status" int2 NOT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "im_face_pack_item"."id" IS '';
COMMENT ON COLUMN "im_face_pack_item"."pack_id" IS '';
COMMENT ON COLUMN "im_face_pack_item"."url" IS '';
COMMENT ON COLUMN "im_face_pack_item"."name" IS '';
COMMENT ON COLUMN "im_face_pack_item"."width" IS '';
COMMENT ON COLUMN "im_face_pack_item"."height" IS '';
COMMENT ON COLUMN "im_face_pack_item"."sort" IS '';
COMMENT ON COLUMN "im_face_pack_item"."status" IS '';
COMMENT ON COLUMN "im_face_pack_item"."creator" IS '';
COMMENT ON COLUMN "im_face_pack_item"."create_time" IS '';
COMMENT ON COLUMN "im_face_pack_item"."updater" IS '';
COMMENT ON COLUMN "im_face_pack_item"."update_time" IS '';
COMMENT ON COLUMN "im_face_pack_item"."deleted" IS '';
COMMENT ON COLUMN "im_face_pack_item"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "im_rtc_call"
-- ----------------------------
-- DROP TABLE IF EXISTS "im_rtc_call";
CREATE TABLE IF NOT EXISTS "im_rtc_call" (
    "id" int8 NOT NULL,
  "room" varchar(64) NOT NULL,
  "conversation_type" int2 NOT NULL,
  "media_type" int2 NOT NULL,
  "inviter_user_id" int8 NOT NULL,
  "group_id" int8 NULL DEFAULT NULL,
  "status" int2 NOT NULL,
  "end_reason" int2 NULL DEFAULT NULL,
  "start_time" timestamp NOT NULL,
  "accept_time" timestamp NULL DEFAULT NULL,
  "end_time" timestamp NULL DEFAULT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "im_rtc_call"."id" IS '';
COMMENT ON COLUMN "im_rtc_call"."room" IS '';
COMMENT ON COLUMN "im_rtc_call"."conversation_type" IS '';
COMMENT ON COLUMN "im_rtc_call"."media_type" IS '';
COMMENT ON COLUMN "im_rtc_call"."inviter_user_id" IS '';
COMMENT ON COLUMN "im_rtc_call"."group_id" IS '';
COMMENT ON COLUMN "im_rtc_call"."status" IS '';
COMMENT ON COLUMN "im_rtc_call"."end_reason" IS '';
COMMENT ON COLUMN "im_rtc_call"."start_time" IS '';
COMMENT ON COLUMN "im_rtc_call"."accept_time" IS '';
COMMENT ON COLUMN "im_rtc_call"."end_time" IS '';
COMMENT ON COLUMN "im_rtc_call"."creator" IS '';
COMMENT ON COLUMN "im_rtc_call"."create_time" IS '';
COMMENT ON COLUMN "im_rtc_call"."updater" IS '';
COMMENT ON COLUMN "im_rtc_call"."update_time" IS '';
COMMENT ON COLUMN "im_rtc_call"."deleted" IS '';
COMMENT ON COLUMN "im_rtc_call"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "im_rtc_participant"
-- ----------------------------
-- DROP TABLE IF EXISTS "im_rtc_participant";
CREATE TABLE IF NOT EXISTS "im_rtc_participant" (
    "id" int8 NOT NULL,
  "call_id" int8 NOT NULL,
  "room" varchar(64) NOT NULL,
  "user_id" int8 NOT NULL,
  "role" int2 NOT NULL,
  "status" int2 NOT NULL,
  "invite_time" timestamp NOT NULL,
  "accept_time" timestamp NULL DEFAULT NULL,
  "leave_time" timestamp NULL DEFAULT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_im_rtc_participant ON "im_rtc_participant" ("room", "user_id", "tenant_id");

COMMENT ON COLUMN "im_rtc_participant"."id" IS '';
COMMENT ON COLUMN "im_rtc_participant"."call_id" IS '';
COMMENT ON COLUMN "im_rtc_participant"."room" IS '';
COMMENT ON COLUMN "im_rtc_participant"."user_id" IS '';
COMMENT ON COLUMN "im_rtc_participant"."role" IS '';
COMMENT ON COLUMN "im_rtc_participant"."status" IS '';
COMMENT ON COLUMN "im_rtc_participant"."invite_time" IS '';
COMMENT ON COLUMN "im_rtc_participant"."accept_time" IS '';
COMMENT ON COLUMN "im_rtc_participant"."leave_time" IS '';
COMMENT ON COLUMN "im_rtc_participant"."creator" IS '';
COMMENT ON COLUMN "im_rtc_participant"."create_time" IS '';
COMMENT ON COLUMN "im_rtc_participant"."updater" IS '';
COMMENT ON COLUMN "im_rtc_participant"."update_time" IS '';
COMMENT ON COLUMN "im_rtc_participant"."deleted" IS '';
COMMENT ON COLUMN "im_rtc_participant"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "im_face_user_item"
-- ----------------------------
-- DROP TABLE IF EXISTS "im_face_user_item";
CREATE TABLE IF NOT EXISTS "im_face_user_item" (
    "id" int8 NOT NULL,
  "user_id" int8 NOT NULL,
  "url" varchar(512) NOT NULL,
  "name" varchar(64) NULL DEFAULT NULL,
  "width" int4 NOT NULL DEFAULT 0,
  "height" int4 NOT NULL DEFAULT 0,
  "sort" int4 NOT NULL DEFAULT 0,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_im_face_user_item ON "im_face_user_item" ("user_id", "url", "deleted");

COMMENT ON COLUMN "im_face_user_item"."id" IS '';
COMMENT ON COLUMN "im_face_user_item"."user_id" IS '';
COMMENT ON COLUMN "im_face_user_item"."url" IS '';
COMMENT ON COLUMN "im_face_user_item"."name" IS '';
COMMENT ON COLUMN "im_face_user_item"."width" IS '';
COMMENT ON COLUMN "im_face_user_item"."height" IS '';
COMMENT ON COLUMN "im_face_user_item"."sort" IS '';
COMMENT ON COLUMN "im_face_user_item"."creator" IS '';
COMMENT ON COLUMN "im_face_user_item"."create_time" IS '';
COMMENT ON COLUMN "im_face_user_item"."updater" IS '';
COMMENT ON COLUMN "im_face_user_item"."update_time" IS '';
COMMENT ON COLUMN "im_face_user_item"."deleted" IS '';
COMMENT ON COLUMN "im_face_user_item"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "im_channel"
-- ----------------------------
-- DROP TABLE IF EXISTS "im_channel";
CREATE TABLE IF NOT EXISTS "im_channel" (
    "id" int8 NOT NULL,
  "code" varchar(64) NOT NULL,
  "name" varchar(64) NOT NULL,
  "avatar" varchar(512) NULL DEFAULT NULL,
  "sort" int4 NOT NULL DEFAULT 0,
  "status" int2 NOT NULL DEFAULT 0,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "im_channel"."id" IS '';
COMMENT ON COLUMN "im_channel"."code" IS '';
COMMENT ON COLUMN "im_channel"."name" IS '';
COMMENT ON COLUMN "im_channel"."avatar" IS '';
COMMENT ON COLUMN "im_channel"."sort" IS '';
COMMENT ON COLUMN "im_channel"."status" IS '';
COMMENT ON COLUMN "im_channel"."creator" IS '';
COMMENT ON COLUMN "im_channel"."create_time" IS '';
COMMENT ON COLUMN "im_channel"."updater" IS '';
COMMENT ON COLUMN "im_channel"."update_time" IS '';
COMMENT ON COLUMN "im_channel"."deleted" IS '';
COMMENT ON COLUMN "im_channel"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "im_channel_material"
-- ----------------------------
-- DROP TABLE IF EXISTS "im_channel_material";
CREATE TABLE IF NOT EXISTS "im_channel_material" (
    "id" int8 NOT NULL,
  "channel_id" int8 NOT NULL,
  "type" int2 NOT NULL,
  "title" varchar(128) NOT NULL,
  "cover_url" varchar(512) NULL DEFAULT NULL,
  "summary" varchar(255) NULL DEFAULT NULL,
  "content" text NULL DEFAULT NULL,
  "url" varchar(512) NULL DEFAULT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "im_channel_material"."id" IS '';
COMMENT ON COLUMN "im_channel_material"."channel_id" IS '';
COMMENT ON COLUMN "im_channel_material"."type" IS '';
COMMENT ON COLUMN "im_channel_material"."title" IS '';
COMMENT ON COLUMN "im_channel_material"."cover_url" IS '';
COMMENT ON COLUMN "im_channel_material"."summary" IS '';
COMMENT ON COLUMN "im_channel_material"."content" IS '';
COMMENT ON COLUMN "im_channel_material"."url" IS '';
COMMENT ON COLUMN "im_channel_material"."creator" IS '';
COMMENT ON COLUMN "im_channel_material"."create_time" IS '';
COMMENT ON COLUMN "im_channel_material"."updater" IS '';
COMMENT ON COLUMN "im_channel_material"."update_time" IS '';
COMMENT ON COLUMN "im_channel_material"."deleted" IS '';
COMMENT ON COLUMN "im_channel_material"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "im_channel_message"
-- ----------------------------
-- DROP TABLE IF EXISTS "im_channel_message";
CREATE TABLE IF NOT EXISTS "im_channel_message" (
    "id" int8 NOT NULL,
  "channel_id" int8 NOT NULL,
  "material_id" int8 NOT NULL,
  "type" int2 NOT NULL,
  "content" varchar(8192) NULL DEFAULT NULL,
  "receiver_user_ids" text NULL DEFAULT NULL,
  "send_time" timestamp NOT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "im_channel_message"."id" IS '';
COMMENT ON COLUMN "im_channel_message"."channel_id" IS '';
COMMENT ON COLUMN "im_channel_message"."material_id" IS '';
COMMENT ON COLUMN "im_channel_message"."type" IS '';
COMMENT ON COLUMN "im_channel_message"."content" IS '';
COMMENT ON COLUMN "im_channel_message"."receiver_user_ids" IS '';
COMMENT ON COLUMN "im_channel_message"."send_time" IS '';
COMMENT ON COLUMN "im_channel_message"."creator" IS '';
COMMENT ON COLUMN "im_channel_message"."create_time" IS '';
COMMENT ON COLUMN "im_channel_message"."updater" IS '';
COMMENT ON COLUMN "im_channel_message"."update_time" IS '';
COMMENT ON COLUMN "im_channel_message"."deleted" IS '';
COMMENT ON COLUMN "im_channel_message"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "im_conversation_read"
-- ----------------------------
-- DROP TABLE IF EXISTS "im_conversation_read";
CREATE TABLE IF NOT EXISTS "im_conversation_read" (
    "id" int8 NOT NULL,
  "user_id" int8 NOT NULL,
  "conversation_type" int2 NOT NULL,
  "target_id" int8 NOT NULL,
  "message_id" int8 NOT NULL,
  "read_time" timestamp NOT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_im_conversation_read ON "im_conversation_read" ("user_id", "conversation_type", "target_id", "tenant_id");

COMMENT ON COLUMN "im_conversation_read"."id" IS '';
COMMENT ON COLUMN "im_conversation_read"."user_id" IS '';
COMMENT ON COLUMN "im_conversation_read"."conversation_type" IS '';
COMMENT ON COLUMN "im_conversation_read"."target_id" IS '';
COMMENT ON COLUMN "im_conversation_read"."message_id" IS '';
COMMENT ON COLUMN "im_conversation_read"."read_time" IS '';
COMMENT ON COLUMN "im_conversation_read"."creator" IS '';
COMMENT ON COLUMN "im_conversation_read"."create_time" IS '';
COMMENT ON COLUMN "im_conversation_read"."updater" IS '';
COMMENT ON COLUMN "im_conversation_read"."update_time" IS '';
COMMENT ON COLUMN "im_conversation_read"."deleted" IS '';
COMMENT ON COLUMN "im_conversation_read"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "im_sensitive_word"
-- ----------------------------
-- DROP TABLE IF EXISTS "im_sensitive_word";
CREATE TABLE IF NOT EXISTS "im_sensitive_word" (
    "id" int8 NOT NULL,
  "word" varchar(128) NOT NULL,
  "status" int2 NOT NULL DEFAULT 0,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_im_sensitive_word ON "im_sensitive_word" ("word", "tenant_id");

COMMENT ON COLUMN "im_sensitive_word"."id" IS '';
COMMENT ON COLUMN "im_sensitive_word"."word" IS '';
COMMENT ON COLUMN "im_sensitive_word"."status" IS '';
COMMENT ON COLUMN "im_sensitive_word"."creator" IS '';
COMMENT ON COLUMN "im_sensitive_word"."create_time" IS '';
COMMENT ON COLUMN "im_sensitive_word"."updater" IS '';
COMMENT ON COLUMN "im_sensitive_word"."update_time" IS '';
COMMENT ON COLUMN "im_sensitive_word"."deleted" IS '';
COMMENT ON COLUMN "im_sensitive_word"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "system_users"
-- ----------------------------
-- DROP TABLE IF EXISTS "system_users";
CREATE TABLE IF NOT EXISTS "system_users" (
    "id" int8 NOT NULL,
  "username" varchar(30) NOT NULL DEFAULT '',
  "password" varchar(100) NOT NULL DEFAULT '',
  "nickname" varchar(30) NOT NULL DEFAULT '',
  "remark" varchar(500) NULL DEFAULT NULL,
  "dept_id" int8 NULL DEFAULT NULL,
  "post_ids" varchar(255) NULL DEFAULT NULL,
  "email" varchar(50) NULL DEFAULT '',
  "mobile" varchar(11) NULL DEFAULT '',
  "sex" int2 NULL DEFAULT 0,
  "avatar" varchar(100) NULL DEFAULT '',
  "status" int2 NOT NULL DEFAULT 0,
  "login_ip" varchar(50) NULL DEFAULT '',
  "login_date" timestamp NULL DEFAULT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "system_users"."id" IS '';
COMMENT ON COLUMN "system_users"."username" IS '';
COMMENT ON COLUMN "system_users"."password" IS '';
COMMENT ON COLUMN "system_users"."nickname" IS '';
COMMENT ON COLUMN "system_users"."remark" IS '';
COMMENT ON COLUMN "system_users"."dept_id" IS '';
COMMENT ON COLUMN "system_users"."post_ids" IS '';
COMMENT ON COLUMN "system_users"."email" IS '';
COMMENT ON COLUMN "system_users"."mobile" IS '';
COMMENT ON COLUMN "system_users"."sex" IS '';
COMMENT ON COLUMN "system_users"."avatar" IS '';
COMMENT ON COLUMN "system_users"."status" IS '';
COMMENT ON COLUMN "system_users"."login_ip" IS '';
COMMENT ON COLUMN "system_users"."login_date" IS '';
COMMENT ON COLUMN "system_users"."creator" IS '';
COMMENT ON COLUMN "system_users"."create_time" IS '';
COMMENT ON COLUMN "system_users"."updater" IS '';
COMMENT ON COLUMN "system_users"."update_time" IS '';
COMMENT ON COLUMN "system_users"."deleted" IS '';
COMMENT ON COLUMN "system_users"."tenant_id" IS '';


-- ===== yudao-module-mall/yudao-module-product =====
/*
 Yudao Database Transfer Tool

 Source Server Type    : MySQL

 Target Server Type    : PostgreSQL

 Date: 2026-07-28 23:44:51
*/


-- ----------------------------
-- Table structure for dual
-- ----------------------------
-- DROP TABLE IF EXISTS dual;
CREATE TABLE IF NOT EXISTS dual
(
    id int2
);

COMMENT ON TABLE dual IS '数据库连接的表';

-- ----------------------------
-- Records of dual
-- ----------------------------
-- @formatter:off
-- @formatter:on

-- ----------------------------
-- Table structure for product_sku
-- ----------------------------
-- DROP TABLE IF EXISTS product_sku;
CREATE TABLE IF NOT EXISTS product_sku (
    id int8 NOT NULL,
  spu_id int8 NOT NULL,
  properties varchar(512) NULL DEFAULT NULL,
  price int4 NOT NULL DEFAULT '-1',
  market_price int4 NULL DEFAULT NULL,
  cost_price int4 NOT NULL DEFAULT '-1',
  bar_code varchar(64) NULL DEFAULT NULL,
  pic_url varchar(256) NOT NULL,
  stock int4 NULL DEFAULT NULL,
  weight double precision NULL DEFAULT NULL,
  volume double precision NULL DEFAULT NULL,
  sub_commission_first_price int4 NULL DEFAULT NULL,
  sub_commission_second_price int4 NULL DEFAULT NULL,
  sales_count int4 NULL DEFAULT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);


COMMENT ON COLUMN product_sku.id IS '';
COMMENT ON COLUMN product_sku.spu_id IS '';
COMMENT ON COLUMN product_sku.properties IS '';
COMMENT ON COLUMN product_sku.price IS '';
COMMENT ON COLUMN product_sku.market_price IS '';
COMMENT ON COLUMN product_sku.cost_price IS '';
COMMENT ON COLUMN product_sku.bar_code IS '';
COMMENT ON COLUMN product_sku.pic_url IS '';
COMMENT ON COLUMN product_sku.stock IS '';
COMMENT ON COLUMN product_sku.weight IS '';
COMMENT ON COLUMN product_sku.volume IS '';
COMMENT ON COLUMN product_sku.sub_commission_first_price IS '';
COMMENT ON COLUMN product_sku.sub_commission_second_price IS '';
COMMENT ON COLUMN product_sku.sales_count IS '';
COMMENT ON COLUMN product_sku."creator" IS '';
COMMENT ON COLUMN product_sku."create_time" IS '';
COMMENT ON COLUMN product_sku."updater" IS '';
COMMENT ON COLUMN product_sku."update_time" IS '';
COMMENT ON COLUMN product_sku."deleted" IS '';
COMMENT ON COLUMN product_sku."tenant_id" IS '';

-- ----------------------------
-- Table structure for product_spu
-- ----------------------------
-- DROP TABLE IF EXISTS product_spu;
CREATE TABLE IF NOT EXISTS product_spu (
    id int8 NOT NULL,
  name varchar(128) NOT NULL,
  keyword varchar(256) NOT NULL,
  introduction varchar(256) NOT NULL,
  description text NOT NULL,
  bar_code varchar(64) NOT NULL,
  category_id int8 NOT NULL,
  brand_id int4 NULL DEFAULT NULL,
  pic_url varchar(256) NOT NULL,
  slider_pic_urls varchar(2000) NULL DEFAULT '',
  video_url varchar(256) NULL DEFAULT NULL,
  unit int2 NOT NULL,
  sort int4 NOT NULL DEFAULT 0,
  status int2 NOT NULL,
  spec_type int2 NOT NULL,
  price int4 NOT NULL DEFAULT '-1',
  market_price int4 NOT NULL,
  cost_price int4 NOT NULL DEFAULT '-1',
  stock int4 NOT NULL DEFAULT 0,
  delivery_template_id int8 NOT NULL,
  recommend_hot int2 NOT NULL,
  recommend_benefit int2 NOT NULL,
  recommend_best int2 NOT NULL,
  recommend_new int2 NOT NULL,
  recommend_good int2 NOT NULL,
  give_integral int4 NOT NULL,
  give_coupon_template_ids varchar(512) NULL DEFAULT '',
  sub_commission_type int2 NOT NULL,
  activity_orders varchar(16) NOT NULL DEFAULT '',
  sales_count int4 NULL DEFAULT 0,
  virtual_sales_count int4 NULL DEFAULT 0,
  browse_count int4 NULL DEFAULT 0,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);


COMMENT ON COLUMN product_spu.id IS '';
COMMENT ON COLUMN product_spu.name IS '';
COMMENT ON COLUMN product_spu.keyword IS '';
COMMENT ON COLUMN product_spu.introduction IS '';
COMMENT ON COLUMN product_spu.description IS '';
COMMENT ON COLUMN product_spu.bar_code IS '';
COMMENT ON COLUMN product_spu.category_id IS '';
COMMENT ON COLUMN product_spu.brand_id IS '';
COMMENT ON COLUMN product_spu.pic_url IS '';
COMMENT ON COLUMN product_spu.slider_pic_urls IS '';
COMMENT ON COLUMN product_spu.video_url IS '';
COMMENT ON COLUMN product_spu.unit IS '';
COMMENT ON COLUMN product_spu.sort IS '';
COMMENT ON COLUMN product_spu.status IS '';
COMMENT ON COLUMN product_spu.spec_type IS '';
COMMENT ON COLUMN product_spu.price IS '';
COMMENT ON COLUMN product_spu.market_price IS '';
COMMENT ON COLUMN product_spu.cost_price IS '';
COMMENT ON COLUMN product_spu.stock IS '';
COMMENT ON COLUMN product_spu.delivery_template_id IS '';
COMMENT ON COLUMN product_spu.recommend_hot IS '';
COMMENT ON COLUMN product_spu.recommend_benefit IS '';
COMMENT ON COLUMN product_spu.recommend_best IS '';
COMMENT ON COLUMN product_spu.recommend_new IS '';
COMMENT ON COLUMN product_spu.recommend_good IS '';
COMMENT ON COLUMN product_spu.give_integral IS '';
COMMENT ON COLUMN product_spu.give_coupon_template_ids IS '';
COMMENT ON COLUMN product_spu.sub_commission_type IS '';
COMMENT ON COLUMN product_spu.activity_orders IS '';
COMMENT ON COLUMN product_spu.sales_count IS '';
COMMENT ON COLUMN product_spu.virtual_sales_count IS '';
COMMENT ON COLUMN product_spu.browse_count IS '';
COMMENT ON COLUMN product_spu."creator" IS '';
COMMENT ON COLUMN product_spu."create_time" IS '';
COMMENT ON COLUMN product_spu."updater" IS '';
COMMENT ON COLUMN product_spu."update_time" IS '';
COMMENT ON COLUMN product_spu."deleted" IS '';
COMMENT ON COLUMN product_spu."tenant_id" IS '';

-- ----------------------------
-- Table structure for product_category
-- ----------------------------
-- DROP TABLE IF EXISTS product_category;
CREATE TABLE IF NOT EXISTS product_category (
    id int8 NOT NULL,
  parent_id int8 NOT NULL,
  name varchar(255) NOT NULL,
  pic_url varchar(255) NOT NULL,
  big_pic_url varchar(255) NULL DEFAULT NULL,
  sort int4 NULL DEFAULT 0,
  status int2 NOT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);


COMMENT ON COLUMN product_category.id IS '';
COMMENT ON COLUMN product_category.parent_id IS '';
COMMENT ON COLUMN product_category.name IS '';
COMMENT ON COLUMN product_category.pic_url IS '';
COMMENT ON COLUMN product_category.big_pic_url IS '';
COMMENT ON COLUMN product_category.sort IS '';
COMMENT ON COLUMN product_category.status IS '';
COMMENT ON COLUMN product_category."creator" IS '';
COMMENT ON COLUMN product_category."create_time" IS '';
COMMENT ON COLUMN product_category."updater" IS '';
COMMENT ON COLUMN product_category."update_time" IS '';
COMMENT ON COLUMN product_category."deleted" IS '';
COMMENT ON COLUMN product_category."tenant_id" IS '';

-- ----------------------------
-- Table structure for product_brand
-- ----------------------------
-- DROP TABLE IF EXISTS product_brand;
CREATE TABLE IF NOT EXISTS product_brand (
    id int8 NOT NULL,
  name varchar(255) NOT NULL,
  pic_url varchar(255) NOT NULL,
  sort int4 NULL DEFAULT 0,
  description varchar(1024) NULL DEFAULT NULL,
  status int2 NOT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);


COMMENT ON COLUMN product_brand.id IS '';
COMMENT ON COLUMN product_brand.name IS '';
COMMENT ON COLUMN product_brand.pic_url IS '';
COMMENT ON COLUMN product_brand.sort IS '';
COMMENT ON COLUMN product_brand.description IS '';
COMMENT ON COLUMN product_brand.status IS '';
COMMENT ON COLUMN product_brand."creator" IS '';
COMMENT ON COLUMN product_brand."create_time" IS '';
COMMENT ON COLUMN product_brand."updater" IS '';
COMMENT ON COLUMN product_brand."update_time" IS '';
COMMENT ON COLUMN product_brand."deleted" IS '';
COMMENT ON COLUMN product_brand."tenant_id" IS '';

-- ----------------------------
-- Table structure for product_property
-- ----------------------------
-- DROP TABLE IF EXISTS product_property;
CREATE TABLE IF NOT EXISTS product_property (
    id int8 NOT NULL,
  name varchar(64) NULL DEFAULT NULL,
  status int2 NULL DEFAULT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  remark varchar(255) NULL DEFAULT NULL,
  PRIMARY KEY ("id")
);


COMMENT ON COLUMN product_property.id IS '';
COMMENT ON COLUMN product_property.name IS '';
COMMENT ON COLUMN product_property.status IS '';
COMMENT ON COLUMN product_property."creator" IS '';
COMMENT ON COLUMN product_property."create_time" IS '';
COMMENT ON COLUMN product_property."updater" IS '';
COMMENT ON COLUMN product_property."update_time" IS '';
COMMENT ON COLUMN product_property."deleted" IS '';
COMMENT ON COLUMN product_property."tenant_id" IS '';
COMMENT ON COLUMN product_property.remark IS '';

-- ----------------------------
-- Table structure for product_property_value
-- ----------------------------
-- DROP TABLE IF EXISTS product_property_value;
CREATE TABLE IF NOT EXISTS product_property_value (
    id int8 NOT NULL,
  property_id int8 NULL DEFAULT NULL,
  name varchar(128) NULL DEFAULT NULL,
  status int2 NULL DEFAULT NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  remark varchar(255) NULL DEFAULT NULL,
  PRIMARY KEY ("id")
);


COMMENT ON COLUMN product_property_value.id IS '';
COMMENT ON COLUMN product_property_value.property_id IS '';
COMMENT ON COLUMN product_property_value.name IS '';
COMMENT ON COLUMN product_property_value.status IS '';
COMMENT ON COLUMN product_property_value."creator" IS '';
COMMENT ON COLUMN product_property_value."create_time" IS '';
COMMENT ON COLUMN product_property_value."updater" IS '';
COMMENT ON COLUMN product_property_value."update_time" IS '';
COMMENT ON COLUMN product_property_value."deleted" IS '';
COMMENT ON COLUMN product_property_value."tenant_id" IS '';
COMMENT ON COLUMN product_property_value.remark IS '';


-- ===== yudao-module-mall/yudao-module-promotion =====
/*
 Yudao Database Transfer Tool

 Source Server Type    : MySQL

 Target Server Type    : PostgreSQL

 Date: 2026-07-28 23:44:51
*/


-- ----------------------------
-- Table structure for dual
-- ----------------------------
-- DROP TABLE IF EXISTS dual;
CREATE TABLE IF NOT EXISTS dual
(
    id int2
);

COMMENT ON TABLE dual IS '数据库连接的表';

-- ----------------------------
-- Records of dual
-- ----------------------------
-- @formatter:off
-- @formatter:on

-- ----------------------------
-- Table structure for "market_activity"
-- ----------------------------
-- DROP TABLE IF EXISTS "market_activity";
CREATE TABLE IF NOT EXISTS "market_activity" (
    "id" int8 NOT NULL,
  "title" varchar(50) NOT NULL,
  "activity_type" int2 NOT NULL,
  "status" int2 NOT NULL,
  "start_time" timestamp NOT NULL,
  "end_time" timestamp NOT NULL,
  "invalid_time" timestamp NULL,
  "delete_time" timestamp NULL,
  "time_limited_discount" varchar(2000) NULL,
  "full_privilege" varchar(2000) NULL,
  "creator" varchar(64) NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "market_activity"."id" IS '';
COMMENT ON COLUMN "market_activity"."title" IS '';
COMMENT ON COLUMN "market_activity"."activity_type" IS '';
COMMENT ON COLUMN "market_activity"."status" IS '';
COMMENT ON COLUMN "market_activity"."start_time" IS '';
COMMENT ON COLUMN "market_activity"."end_time" IS '';
COMMENT ON COLUMN "market_activity"."invalid_time" IS '';
COMMENT ON COLUMN "market_activity"."delete_time" IS '';
COMMENT ON COLUMN "market_activity"."time_limited_discount" IS '';
COMMENT ON COLUMN "market_activity"."full_privilege" IS '';
COMMENT ON COLUMN "market_activity"."creator" IS '';
COMMENT ON COLUMN "market_activity"."create_time" IS '';
COMMENT ON COLUMN "market_activity"."updater" IS '';
COMMENT ON COLUMN "market_activity"."update_time" IS '';
COMMENT ON COLUMN "market_activity"."deleted" IS '';
COMMENT ON COLUMN "market_activity"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "promotion_coupon_template"
-- ----------------------------
-- DROP TABLE IF EXISTS "promotion_coupon_template";
CREATE TABLE IF NOT EXISTS "promotion_coupon_template" (
    "id" int8 NOT NULL,
  "name" text NOT NULL,
  "description" text NULL,
  "status" int4 NOT NULL,
  "total_count" int4 NOT NULL,
  "take_limit_count" int4 NOT NULL,
  "take_type" int4 NOT NULL,
  "use_price" int4 NOT NULL,
  "product_scope" int4 NOT NULL,
  "product_scope_values" text NULL,
  "validity_type" int4 NOT NULL,
  "valid_start_time" timestamp NULL,
  "valid_end_time" timestamp NULL,
  "fixed_start_term" int4 NULL,
  "fixed_end_term" int4 NULL,
  "discount_type" int4 NOT NULL,
  "discount_percent" int4 NULL,
  "discount_price" int4 NULL,
  "discount_limit_price" int4 NULL,
  "take_count" int4 NOT NULL DEFAULT 0,
  "use_count" int4 NOT NULL DEFAULT 0,
  "creator" text NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" text NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "promotion_coupon_template"."id" IS '';
COMMENT ON COLUMN "promotion_coupon_template"."name" IS '';
COMMENT ON COLUMN "promotion_coupon_template"."description" IS '';
COMMENT ON COLUMN "promotion_coupon_template"."status" IS '';
COMMENT ON COLUMN "promotion_coupon_template"."total_count" IS '';
COMMENT ON COLUMN "promotion_coupon_template"."take_limit_count" IS '';
COMMENT ON COLUMN "promotion_coupon_template"."take_type" IS '';
COMMENT ON COLUMN "promotion_coupon_template"."use_price" IS '';
COMMENT ON COLUMN "promotion_coupon_template"."product_scope" IS '';
COMMENT ON COLUMN "promotion_coupon_template"."product_scope_values" IS '';
COMMENT ON COLUMN "promotion_coupon_template"."validity_type" IS '';
COMMENT ON COLUMN "promotion_coupon_template"."valid_start_time" IS '';
COMMENT ON COLUMN "promotion_coupon_template"."valid_end_time" IS '';
COMMENT ON COLUMN "promotion_coupon_template"."fixed_start_term" IS '';
COMMENT ON COLUMN "promotion_coupon_template"."fixed_end_term" IS '';
COMMENT ON COLUMN "promotion_coupon_template"."discount_type" IS '';
COMMENT ON COLUMN "promotion_coupon_template"."discount_percent" IS '';
COMMENT ON COLUMN "promotion_coupon_template"."discount_price" IS '';
COMMENT ON COLUMN "promotion_coupon_template"."discount_limit_price" IS '';
COMMENT ON COLUMN "promotion_coupon_template"."take_count" IS '';
COMMENT ON COLUMN "promotion_coupon_template"."use_count" IS '';
COMMENT ON COLUMN "promotion_coupon_template"."creator" IS '';
COMMENT ON COLUMN "promotion_coupon_template"."create_time" IS '';
COMMENT ON COLUMN "promotion_coupon_template"."updater" IS '';
COMMENT ON COLUMN "promotion_coupon_template"."update_time" IS '';
COMMENT ON COLUMN "promotion_coupon_template"."deleted" IS '';

-- ----------------------------
-- Table structure for "promotion_coupon"
-- ----------------------------
-- DROP TABLE IF EXISTS "promotion_coupon";
CREATE TABLE IF NOT EXISTS "promotion_coupon" (
    "id" int8 NOT NULL,
  "template_id" int8 NOT NULL,
  "name" text NOT NULL,
  "status" int4 NOT NULL,
  "user_id" int8 NOT NULL,
  "take_type" int4 NOT NULL,
  "use_price" int4 NOT NULL,
  "valid_start_time" timestamp NOT NULL,
  "valid_end_time" timestamp NOT NULL,
  "product_scope" int4 NOT NULL,
  "product_scope_values" text NULL,
  "discount_type" int4 NOT NULL,
  "discount_percent" int4 NULL,
  "discount_price" int4 NULL,
  "discount_limit_price" int4 NULL,
  "use_order_id" int8 NULL,
  "use_time" timestamp NULL,
  "creator" text NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" text NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "promotion_coupon"."id" IS '';
COMMENT ON COLUMN "promotion_coupon"."template_id" IS '';
COMMENT ON COLUMN "promotion_coupon"."name" IS '';
COMMENT ON COLUMN "promotion_coupon"."status" IS '';
COMMENT ON COLUMN "promotion_coupon"."user_id" IS '';
COMMENT ON COLUMN "promotion_coupon"."take_type" IS '';
COMMENT ON COLUMN "promotion_coupon"."use_price" IS '';
COMMENT ON COLUMN "promotion_coupon"."valid_start_time" IS '';
COMMENT ON COLUMN "promotion_coupon"."valid_end_time" IS '';
COMMENT ON COLUMN "promotion_coupon"."product_scope" IS '';
COMMENT ON COLUMN "promotion_coupon"."product_scope_values" IS '';
COMMENT ON COLUMN "promotion_coupon"."discount_type" IS '';
COMMENT ON COLUMN "promotion_coupon"."discount_percent" IS '';
COMMENT ON COLUMN "promotion_coupon"."discount_price" IS '';
COMMENT ON COLUMN "promotion_coupon"."discount_limit_price" IS '';
COMMENT ON COLUMN "promotion_coupon"."use_order_id" IS '';
COMMENT ON COLUMN "promotion_coupon"."use_time" IS '';
COMMENT ON COLUMN "promotion_coupon"."creator" IS '';
COMMENT ON COLUMN "promotion_coupon"."create_time" IS '';
COMMENT ON COLUMN "promotion_coupon"."updater" IS '';
COMMENT ON COLUMN "promotion_coupon"."update_time" IS '';
COMMENT ON COLUMN "promotion_coupon"."deleted" IS '';

-- ----------------------------
-- Table structure for "promotion_reward_activity"
-- ----------------------------
-- DROP TABLE IF EXISTS "promotion_reward_activity";
CREATE TABLE IF NOT EXISTS "promotion_reward_activity" (
    "id" int8 NOT NULL,
  "name" text NOT NULL,
  "status" int4 NOT NULL,
  "start_time" timestamp NOT NULL,
  "end_time" timestamp NOT NULL,
  "remark" text NULL,
  "condition_type" int4 NOT NULL,
  "product_scope" int4 NOT NULL,
  "product_spu_ids" text NULL,
  "rules" text NULL,
  "creator" text NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" text NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "promotion_reward_activity"."id" IS '';
COMMENT ON COLUMN "promotion_reward_activity"."name" IS '';
COMMENT ON COLUMN "promotion_reward_activity"."status" IS '';
COMMENT ON COLUMN "promotion_reward_activity"."start_time" IS '';
COMMENT ON COLUMN "promotion_reward_activity"."end_time" IS '';
COMMENT ON COLUMN "promotion_reward_activity"."remark" IS '';
COMMENT ON COLUMN "promotion_reward_activity"."condition_type" IS '';
COMMENT ON COLUMN "promotion_reward_activity"."product_scope" IS '';
COMMENT ON COLUMN "promotion_reward_activity"."product_spu_ids" IS '';
COMMENT ON COLUMN "promotion_reward_activity"."rules" IS '';
COMMENT ON COLUMN "promotion_reward_activity"."creator" IS '';
COMMENT ON COLUMN "promotion_reward_activity"."create_time" IS '';
COMMENT ON COLUMN "promotion_reward_activity"."updater" IS '';
COMMENT ON COLUMN "promotion_reward_activity"."update_time" IS '';
COMMENT ON COLUMN "promotion_reward_activity"."deleted" IS '';

-- ----------------------------
-- Table structure for "promotion_discount_activity"
-- ----------------------------
-- DROP TABLE IF EXISTS "promotion_discount_activity";
CREATE TABLE IF NOT EXISTS "promotion_discount_activity" (
    "id" int8 NOT NULL,
  "name" text NOT NULL,
  "status" int4 NOT NULL,
  "start_time" timestamp NOT NULL,
  "end_time" timestamp NOT NULL,
  "remark" text NULL,
  "creator" text NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" text NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "promotion_discount_activity"."id" IS '';
COMMENT ON COLUMN "promotion_discount_activity"."name" IS '';
COMMENT ON COLUMN "promotion_discount_activity"."status" IS '';
COMMENT ON COLUMN "promotion_discount_activity"."start_time" IS '';
COMMENT ON COLUMN "promotion_discount_activity"."end_time" IS '';
COMMENT ON COLUMN "promotion_discount_activity"."remark" IS '';
COMMENT ON COLUMN "promotion_discount_activity"."creator" IS '';
COMMENT ON COLUMN "promotion_discount_activity"."create_time" IS '';
COMMENT ON COLUMN "promotion_discount_activity"."updater" IS '';
COMMENT ON COLUMN "promotion_discount_activity"."update_time" IS '';
COMMENT ON COLUMN "promotion_discount_activity"."deleted" IS '';

-- ----------------------------
-- Table structure for "promotion_discount_product"
-- ----------------------------
-- DROP TABLE IF EXISTS "promotion_discount_product";
CREATE TABLE IF NOT EXISTS "promotion_discount_product" (
    "id" int8 NOT NULL,
  "activity_id" int8 NOT NULL,
  "spu_id" int8 NOT NULL,
  "sku_id" int8 NOT NULL,
  "discount_type" int4 NOT NULL,
  "discount_percent" int4 NULL,
  "discount_price" int4 NULL,
  "activity_name" text NOT NULL,
  "activity_status" int4 NOT NULL,
  "activity_start_time" timestamp NOT NULL,
  "activity_end_time" timestamp NOT NULL,
  "creator" text NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" text NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "promotion_discount_product"."id" IS '';
COMMENT ON COLUMN "promotion_discount_product"."activity_id" IS '';
COMMENT ON COLUMN "promotion_discount_product"."spu_id" IS '';
COMMENT ON COLUMN "promotion_discount_product"."sku_id" IS '';
COMMENT ON COLUMN "promotion_discount_product"."discount_type" IS '';
COMMENT ON COLUMN "promotion_discount_product"."discount_percent" IS '';
COMMENT ON COLUMN "promotion_discount_product"."discount_price" IS '';
COMMENT ON COLUMN "promotion_discount_product"."activity_name" IS '';
COMMENT ON COLUMN "promotion_discount_product"."activity_status" IS '';
COMMENT ON COLUMN "promotion_discount_product"."activity_start_time" IS '';
COMMENT ON COLUMN "promotion_discount_product"."activity_end_time" IS '';
COMMENT ON COLUMN "promotion_discount_product"."creator" IS '';
COMMENT ON COLUMN "promotion_discount_product"."create_time" IS '';
COMMENT ON COLUMN "promotion_discount_product"."updater" IS '';
COMMENT ON COLUMN "promotion_discount_product"."update_time" IS '';
COMMENT ON COLUMN "promotion_discount_product"."deleted" IS '';

-- ----------------------------
-- Table structure for "promotion_seckill_activity"
-- ----------------------------
-- DROP TABLE IF EXISTS "promotion_seckill_activity";
CREATE TABLE IF NOT EXISTS "promotion_seckill_activity" (
    "id" int8 NOT NULL,
  "spu_id" int8 NOT NULL,
  "name" text NOT NULL,
  "status" int4 NOT NULL,
  "remark" text NULL,
  "start_time" text NOT NULL,
  "end_time" text NOT NULL,
  "sort" int4 NOT NULL,
  "config_ids" text NOT NULL,
  "order_count" int4 NOT NULL,
  "user_count" int4 NOT NULL,
  "total_price" int4 NOT NULL,
  "total_limit_count" int4 NULL,
  "single_limit_count" int4 NULL,
  "stock" int4 NULL,
  "total_stock" int4 NULL,
  "creator" text NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" text NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "promotion_seckill_activity"."id" IS '';
COMMENT ON COLUMN "promotion_seckill_activity"."spu_id" IS '';
COMMENT ON COLUMN "promotion_seckill_activity"."name" IS '';
COMMENT ON COLUMN "promotion_seckill_activity"."status" IS '';
COMMENT ON COLUMN "promotion_seckill_activity"."remark" IS '';
COMMENT ON COLUMN "promotion_seckill_activity"."start_time" IS '';
COMMENT ON COLUMN "promotion_seckill_activity"."end_time" IS '';
COMMENT ON COLUMN "promotion_seckill_activity"."sort" IS '';
COMMENT ON COLUMN "promotion_seckill_activity"."config_ids" IS '';
COMMENT ON COLUMN "promotion_seckill_activity"."order_count" IS '';
COMMENT ON COLUMN "promotion_seckill_activity"."user_count" IS '';
COMMENT ON COLUMN "promotion_seckill_activity"."total_price" IS '';
COMMENT ON COLUMN "promotion_seckill_activity"."total_limit_count" IS '';
COMMENT ON COLUMN "promotion_seckill_activity"."single_limit_count" IS '';
COMMENT ON COLUMN "promotion_seckill_activity"."stock" IS '';
COMMENT ON COLUMN "promotion_seckill_activity"."total_stock" IS '';
COMMENT ON COLUMN "promotion_seckill_activity"."creator" IS '';
COMMENT ON COLUMN "promotion_seckill_activity"."create_time" IS '';
COMMENT ON COLUMN "promotion_seckill_activity"."updater" IS '';
COMMENT ON COLUMN "promotion_seckill_activity"."update_time" IS '';
COMMENT ON COLUMN "promotion_seckill_activity"."deleted" IS '';
COMMENT ON COLUMN "promotion_seckill_activity"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "promotion_seckill_config"
-- ----------------------------
-- DROP TABLE IF EXISTS "promotion_seckill_config";
CREATE TABLE IF NOT EXISTS "promotion_seckill_config" (
    "id" int8 NOT NULL,
  "name" text NOT NULL,
  "start_time" text NOT NULL,
  "end_time" text NOT NULL,
  "pic_url" text NOT NULL,
  "status" int4 NOT NULL,
  "creator" text NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" text NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "promotion_seckill_config"."id" IS '';
COMMENT ON COLUMN "promotion_seckill_config"."name" IS '';
COMMENT ON COLUMN "promotion_seckill_config"."start_time" IS '';
COMMENT ON COLUMN "promotion_seckill_config"."end_time" IS '';
COMMENT ON COLUMN "promotion_seckill_config"."pic_url" IS '';
COMMENT ON COLUMN "promotion_seckill_config"."status" IS '';
COMMENT ON COLUMN "promotion_seckill_config"."creator" IS '';
COMMENT ON COLUMN "promotion_seckill_config"."create_time" IS '';
COMMENT ON COLUMN "promotion_seckill_config"."updater" IS '';
COMMENT ON COLUMN "promotion_seckill_config"."update_time" IS '';
COMMENT ON COLUMN "promotion_seckill_config"."deleted" IS '';
COMMENT ON COLUMN "promotion_seckill_config"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "promotion_combination_activity"
-- ----------------------------
-- DROP TABLE IF EXISTS "promotion_combination_activity";
CREATE TABLE IF NOT EXISTS "promotion_combination_activity" (
    "id" int8 NOT NULL,
  "name" text NOT NULL,
  "spu_id" int8 NULL,
  "total_limit_count" int4 NOT NULL,
  "single_limit_count" int4 NOT NULL,
  "start_time" text NOT NULL,
  "end_time" text NOT NULL,
  "user_size" int4 NOT NULL,
  "total_num" int4 NOT NULL,
  "success_num" int4 NOT NULL,
  "order_user_count" int4 NOT NULL,
  "virtual_group" int4 NOT NULL,
  "status" int4 NOT NULL,
  "limit_duration" int4 NOT NULL,
  "creator" text NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" text NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "promotion_combination_activity"."id" IS '';
COMMENT ON COLUMN "promotion_combination_activity"."name" IS '';
COMMENT ON COLUMN "promotion_combination_activity"."spu_id" IS '';
COMMENT ON COLUMN "promotion_combination_activity"."total_limit_count" IS '';
COMMENT ON COLUMN "promotion_combination_activity"."single_limit_count" IS '';
COMMENT ON COLUMN "promotion_combination_activity"."start_time" IS '';
COMMENT ON COLUMN "promotion_combination_activity"."end_time" IS '';
COMMENT ON COLUMN "promotion_combination_activity"."user_size" IS '';
COMMENT ON COLUMN "promotion_combination_activity"."total_num" IS '';
COMMENT ON COLUMN "promotion_combination_activity"."success_num" IS '';
COMMENT ON COLUMN "promotion_combination_activity"."order_user_count" IS '';
COMMENT ON COLUMN "promotion_combination_activity"."virtual_group" IS '';
COMMENT ON COLUMN "promotion_combination_activity"."status" IS '';
COMMENT ON COLUMN "promotion_combination_activity"."limit_duration" IS '';
COMMENT ON COLUMN "promotion_combination_activity"."creator" IS '';
COMMENT ON COLUMN "promotion_combination_activity"."create_time" IS '';
COMMENT ON COLUMN "promotion_combination_activity"."updater" IS '';
COMMENT ON COLUMN "promotion_combination_activity"."update_time" IS '';
COMMENT ON COLUMN "promotion_combination_activity"."deleted" IS '';
COMMENT ON COLUMN "promotion_combination_activity"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "promotion_combination_record"
-- ----------------------------
-- DROP TABLE IF EXISTS "promotion_combination_record";
CREATE TABLE IF NOT EXISTS "promotion_combination_record" (
    "id" int8 NOT NULL,
  "activity_id" int8 NOT NULL,
  "combination_price" int4 NOT NULL,
  "spu_id" int8 NOT NULL,
  "spu_name" text NOT NULL,
  "pic_url" text NULL,
  "sku_id" int8 NOT NULL,
  "count" int4 NOT NULL,
  "user_id" int8 NOT NULL,
  "nickname" text NULL,
  "avatar" text NULL,
  "head_id" int8 NOT NULL,
  "status" int4 NOT NULL,
  "order_id" int8 NOT NULL,
  "user_size" int4 NOT NULL,
  "user_count" int4 NOT NULL,
  "virtual_group" int2 NOT NULL,
  "expire_time" timestamp NULL,
  "start_time" timestamp NULL,
  "end_time" timestamp NULL,
  "creator" text NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" text NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "promotion_combination_record"."id" IS '';
COMMENT ON COLUMN "promotion_combination_record"."activity_id" IS '';
COMMENT ON COLUMN "promotion_combination_record"."combination_price" IS '';
COMMENT ON COLUMN "promotion_combination_record"."spu_id" IS '';
COMMENT ON COLUMN "promotion_combination_record"."spu_name" IS '';
COMMENT ON COLUMN "promotion_combination_record"."pic_url" IS '';
COMMENT ON COLUMN "promotion_combination_record"."sku_id" IS '';
COMMENT ON COLUMN "promotion_combination_record"."count" IS '';
COMMENT ON COLUMN "promotion_combination_record"."user_id" IS '';
COMMENT ON COLUMN "promotion_combination_record"."nickname" IS '';
COMMENT ON COLUMN "promotion_combination_record"."avatar" IS '';
COMMENT ON COLUMN "promotion_combination_record"."head_id" IS '';
COMMENT ON COLUMN "promotion_combination_record"."status" IS '';
COMMENT ON COLUMN "promotion_combination_record"."order_id" IS '';
COMMENT ON COLUMN "promotion_combination_record"."user_size" IS '';
COMMENT ON COLUMN "promotion_combination_record"."user_count" IS '';
COMMENT ON COLUMN "promotion_combination_record"."virtual_group" IS '';
COMMENT ON COLUMN "promotion_combination_record"."expire_time" IS '';
COMMENT ON COLUMN "promotion_combination_record"."start_time" IS '';
COMMENT ON COLUMN "promotion_combination_record"."end_time" IS '';
COMMENT ON COLUMN "promotion_combination_record"."creator" IS '';
COMMENT ON COLUMN "promotion_combination_record"."create_time" IS '';
COMMENT ON COLUMN "promotion_combination_record"."updater" IS '';
COMMENT ON COLUMN "promotion_combination_record"."update_time" IS '';
COMMENT ON COLUMN "promotion_combination_record"."deleted" IS '';

-- ----------------------------
-- Table structure for "promotion_article_category"
-- ----------------------------
-- DROP TABLE IF EXISTS "promotion_article_category";
CREATE TABLE IF NOT EXISTS "promotion_article_category" (
    "id" int8 NOT NULL,
  "name" text NOT NULL,
  "pic_url" text NULL,
  "status" int4 NOT NULL,
  "sort" int4 NOT NULL,
  "creator" text NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" text NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "promotion_article_category"."id" IS '';
COMMENT ON COLUMN "promotion_article_category"."name" IS '';
COMMENT ON COLUMN "promotion_article_category"."pic_url" IS '';
COMMENT ON COLUMN "promotion_article_category"."status" IS '';
COMMENT ON COLUMN "promotion_article_category"."sort" IS '';
COMMENT ON COLUMN "promotion_article_category"."creator" IS '';
COMMENT ON COLUMN "promotion_article_category"."create_time" IS '';
COMMENT ON COLUMN "promotion_article_category"."updater" IS '';
COMMENT ON COLUMN "promotion_article_category"."update_time" IS '';
COMMENT ON COLUMN "promotion_article_category"."deleted" IS '';
COMMENT ON COLUMN "promotion_article_category"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "promotion_article"
-- ----------------------------
-- DROP TABLE IF EXISTS "promotion_article";
CREATE TABLE IF NOT EXISTS "promotion_article" (
    "id" int8 NOT NULL,
  "category_id" int8 NOT NULL,
  "title" text NOT NULL,
  "author" text NULL,
  "pic_url" text NOT NULL,
  "introduction" text NULL,
  "browse_count" text NULL,
  "sort" int4 NOT NULL,
  "status" int4 NOT NULL,
  "spu_id" int8 NOT NULL,
  "recommend_hot" int2 NOT NULL,
  "recommend_banner" int2 NOT NULL,
  "content" text NOT NULL,
  "creator" text NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" text NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "promotion_article"."id" IS '';
COMMENT ON COLUMN "promotion_article"."category_id" IS '';
COMMENT ON COLUMN "promotion_article"."title" IS '';
COMMENT ON COLUMN "promotion_article"."author" IS '';
COMMENT ON COLUMN "promotion_article"."pic_url" IS '';
COMMENT ON COLUMN "promotion_article"."introduction" IS '';
COMMENT ON COLUMN "promotion_article"."browse_count" IS '';
COMMENT ON COLUMN "promotion_article"."sort" IS '';
COMMENT ON COLUMN "promotion_article"."status" IS '';
COMMENT ON COLUMN "promotion_article"."spu_id" IS '';
COMMENT ON COLUMN "promotion_article"."recommend_hot" IS '';
COMMENT ON COLUMN "promotion_article"."recommend_banner" IS '';
COMMENT ON COLUMN "promotion_article"."content" IS '';
COMMENT ON COLUMN "promotion_article"."creator" IS '';
COMMENT ON COLUMN "promotion_article"."create_time" IS '';
COMMENT ON COLUMN "promotion_article"."updater" IS '';
COMMENT ON COLUMN "promotion_article"."update_time" IS '';
COMMENT ON COLUMN "promotion_article"."deleted" IS '';
COMMENT ON COLUMN "promotion_article"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "promotion_diy_template"
-- ----------------------------
-- DROP TABLE IF EXISTS "promotion_diy_template";
CREATE TABLE IF NOT EXISTS "promotion_diy_template" (
    "id" int8 NOT NULL,
  "name" text NOT NULL,
  "used" int2 NOT NULL,
  "used_time" text NULL,
  "remark" text NULL,
  "preview_pic_urls" text NULL,
  "property" text NOT NULL,
  "creator" text NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" text NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "promotion_diy_template"."id" IS '';
COMMENT ON COLUMN "promotion_diy_template"."name" IS '';
COMMENT ON COLUMN "promotion_diy_template"."used" IS '';
COMMENT ON COLUMN "promotion_diy_template"."used_time" IS '';
COMMENT ON COLUMN "promotion_diy_template"."remark" IS '';
COMMENT ON COLUMN "promotion_diy_template"."preview_pic_urls" IS '';
COMMENT ON COLUMN "promotion_diy_template"."property" IS '';
COMMENT ON COLUMN "promotion_diy_template"."creator" IS '';
COMMENT ON COLUMN "promotion_diy_template"."create_time" IS '';
COMMENT ON COLUMN "promotion_diy_template"."updater" IS '';
COMMENT ON COLUMN "promotion_diy_template"."update_time" IS '';
COMMENT ON COLUMN "promotion_diy_template"."deleted" IS '';
COMMENT ON COLUMN "promotion_diy_template"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "promotion_diy_page"
-- ----------------------------
-- DROP TABLE IF EXISTS "promotion_diy_page";
CREATE TABLE IF NOT EXISTS "promotion_diy_page" (
    "id" int8 NOT NULL,
  "template_id" int8 NOT NULL,
  "name" text NOT NULL,
  "remark" text NULL,
  "preview_pic_urls" text NULL,
  "property" text NULL,
  "creator" text NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" text NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "promotion_diy_page"."id" IS '';
COMMENT ON COLUMN "promotion_diy_page"."template_id" IS '';
COMMENT ON COLUMN "promotion_diy_page"."name" IS '';
COMMENT ON COLUMN "promotion_diy_page"."remark" IS '';
COMMENT ON COLUMN "promotion_diy_page"."preview_pic_urls" IS '';
COMMENT ON COLUMN "promotion_diy_page"."property" IS '';
COMMENT ON COLUMN "promotion_diy_page"."creator" IS '';
COMMENT ON COLUMN "promotion_diy_page"."create_time" IS '';
COMMENT ON COLUMN "promotion_diy_page"."updater" IS '';
COMMENT ON COLUMN "promotion_diy_page"."update_time" IS '';
COMMENT ON COLUMN "promotion_diy_page"."deleted" IS '';
COMMENT ON COLUMN "promotion_diy_page"."tenant_id" IS '';


-- ===== yudao-module-mall/yudao-module-trade =====
/*
 Yudao Database Transfer Tool

 Source Server Type    : MySQL

 Target Server Type    : PostgreSQL

 Date: 2026-07-28 23:44:51
*/


-- ----------------------------
-- Table structure for dual
-- ----------------------------
-- DROP TABLE IF EXISTS dual;
CREATE TABLE IF NOT EXISTS dual
(
    id int2
);

COMMENT ON TABLE dual IS '数据库连接的表';

-- ----------------------------
-- Records of dual
-- ----------------------------
-- @formatter:off
-- @formatter:on

-- ----------------------------
-- Table structure for "trade_order"
-- ----------------------------
-- DROP TABLE IF EXISTS "trade_order";
CREATE TABLE IF NOT EXISTS "trade_order" (
    "id" int8 NOT NULL,
  "no" text NOT NULL,
  "type" int4 NOT NULL,
  "terminal" int4 NOT NULL,
  "user_id" int8 NOT NULL,
  "user_ip" text NOT NULL,
  "user_remark" text NULL,
  "status" int4 NOT NULL,
  "product_count" int4 NOT NULL,
  "cancel_type" int4 NULL,
  "remark" text NULL,
  "comment_status" int2 NULL,
  "brokerage_user_id" int8 NULL,
  "pay_status" int2 NOT NULL,
  "pay_time" timestamp NULL,
  "finish_time" timestamp NULL,
  "cancel_time" timestamp NULL,
  "total_price" int4 NULL,
  "order_price" int4 NULL,
  "discount_price" int4 NOT NULL,
  "delivery_price" int4 NOT NULL,
  "adjust_price" int4 NOT NULL,
  "pay_price" int4 NOT NULL,
  "delivery_type" int4 NOT NULL,
  "pay_order_id" int8 NULL,
  "pay_channel_code" text NULL,
  "delivery_template_id" int8 NULL,
  "logistics_id" int8 NULL,
  "logistics_no" text NULL,
  "delivery_time" timestamp NULL,
  "receive_time" timestamp NULL,
  "receiver_name" text NOT NULL,
  "receiver_mobile" text NOT NULL,
  "receiver_area_id" int4 NOT NULL,
  "receiver_post_code" int4 NULL,
  "receiver_detail_address" text NOT NULL,
  "pick_up_store_id" int8 NULL,
  "pick_up_verify_code" text NULL,
  "refund_status" int4 NULL,
  "refund_price" int4 NULL,
  "after_sale_status" int4 NULL,
  "coupon_id" int8 NOT NULL,
  "coupon_price" int4 NOT NULL,
  "use_point" int4 NULL,
  "point_price" int4 NOT NULL,
  "give_point" int4 NULL,
  "refund_point" int4 NULL,
  "vip_price" int4 NULL,
  "give_coupons_map" text NULL,
  "seckill_activity_id" int8 NULL,
  "bargain_activity_id" int8 NULL,
  "bargain_record_id" int8 NULL,
  "combination_activity_id" int8 NULL,
  "combination_head_id" int8 NULL,
  "combination_record_id" int8 NULL,
  "creator" text NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" text NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "trade_order"."id" IS '';
COMMENT ON COLUMN "trade_order"."no" IS '';
COMMENT ON COLUMN "trade_order"."type" IS '';
COMMENT ON COLUMN "trade_order"."terminal" IS '';
COMMENT ON COLUMN "trade_order"."user_id" IS '';
COMMENT ON COLUMN "trade_order"."user_ip" IS '';
COMMENT ON COLUMN "trade_order"."user_remark" IS '';
COMMENT ON COLUMN "trade_order"."status" IS '';
COMMENT ON COLUMN "trade_order"."product_count" IS '';
COMMENT ON COLUMN "trade_order"."cancel_type" IS '';
COMMENT ON COLUMN "trade_order"."remark" IS '';
COMMENT ON COLUMN "trade_order"."comment_status" IS '';
COMMENT ON COLUMN "trade_order"."brokerage_user_id" IS '';
COMMENT ON COLUMN "trade_order"."pay_status" IS '';
COMMENT ON COLUMN "trade_order"."pay_time" IS '';
COMMENT ON COLUMN "trade_order"."finish_time" IS '';
COMMENT ON COLUMN "trade_order"."cancel_time" IS '';
COMMENT ON COLUMN "trade_order"."total_price" IS '';
COMMENT ON COLUMN "trade_order"."order_price" IS '';
COMMENT ON COLUMN "trade_order"."discount_price" IS '';
COMMENT ON COLUMN "trade_order"."delivery_price" IS '';
COMMENT ON COLUMN "trade_order"."adjust_price" IS '';
COMMENT ON COLUMN "trade_order"."pay_price" IS '';
COMMENT ON COLUMN "trade_order"."delivery_type" IS '';
COMMENT ON COLUMN "trade_order"."pay_order_id" IS '';
COMMENT ON COLUMN "trade_order"."pay_channel_code" IS '';
COMMENT ON COLUMN "trade_order"."delivery_template_id" IS '';
COMMENT ON COLUMN "trade_order"."logistics_id" IS '';
COMMENT ON COLUMN "trade_order"."logistics_no" IS '';
COMMENT ON COLUMN "trade_order"."delivery_time" IS '';
COMMENT ON COLUMN "trade_order"."receive_time" IS '';
COMMENT ON COLUMN "trade_order"."receiver_name" IS '';
COMMENT ON COLUMN "trade_order"."receiver_mobile" IS '';
COMMENT ON COLUMN "trade_order"."receiver_area_id" IS '';
COMMENT ON COLUMN "trade_order"."receiver_post_code" IS '';
COMMENT ON COLUMN "trade_order"."receiver_detail_address" IS '';
COMMENT ON COLUMN "trade_order"."pick_up_store_id" IS '';
COMMENT ON COLUMN "trade_order"."pick_up_verify_code" IS '';
COMMENT ON COLUMN "trade_order"."refund_status" IS '';
COMMENT ON COLUMN "trade_order"."refund_price" IS '';
COMMENT ON COLUMN "trade_order"."after_sale_status" IS '';
COMMENT ON COLUMN "trade_order"."coupon_id" IS '';
COMMENT ON COLUMN "trade_order"."coupon_price" IS '';
COMMENT ON COLUMN "trade_order"."use_point" IS '';
COMMENT ON COLUMN "trade_order"."point_price" IS '';
COMMENT ON COLUMN "trade_order"."give_point" IS '';
COMMENT ON COLUMN "trade_order"."refund_point" IS '';
COMMENT ON COLUMN "trade_order"."vip_price" IS '';
COMMENT ON COLUMN "trade_order"."give_coupons_map" IS '';
COMMENT ON COLUMN "trade_order"."seckill_activity_id" IS '';
COMMENT ON COLUMN "trade_order"."bargain_activity_id" IS '';
COMMENT ON COLUMN "trade_order"."bargain_record_id" IS '';
COMMENT ON COLUMN "trade_order"."combination_activity_id" IS '';
COMMENT ON COLUMN "trade_order"."combination_head_id" IS '';
COMMENT ON COLUMN "trade_order"."combination_record_id" IS '';
COMMENT ON COLUMN "trade_order"."creator" IS '';
COMMENT ON COLUMN "trade_order"."create_time" IS '';
COMMENT ON COLUMN "trade_order"."updater" IS '';
COMMENT ON COLUMN "trade_order"."update_time" IS '';
COMMENT ON COLUMN "trade_order"."deleted" IS '';

-- ----------------------------
-- Table structure for "trade_order_item"
-- ----------------------------
-- DROP TABLE IF EXISTS "trade_order_item";
CREATE TABLE IF NOT EXISTS "trade_order_item" (
    "id" int8 NOT NULL,
  "user_id" int8 NOT NULL,
  "order_id" int8 NOT NULL,
  "cart_id" int4 NULL,
  "spu_id" int8 NOT NULL,
  "spu_name" text NOT NULL,
  "sku_id" int8 NOT NULL,
  "properties" text NULL,
  "pic_url" text NULL,
  "count" int4 NOT NULL,
  "comment_status" int2 NULL,
  "price" int4 NOT NULL,
  "discount_price" int4 NOT NULL,
  "delivery_price" int4 NULL,
  "adjust_price" int4 NULL,
  "pay_price" int4 NOT NULL,
  "coupon_price" int4 NULL,
  "point_price" int4 NULL,
  "use_point" int4 NULL,
  "give_point" int4 NULL,
  "vip_price" int4 NULL,
  "after_sale_id" int8 NULL,
  "after_sale_status" int4 NOT NULL,
  "creator" text NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" text NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "trade_order_item"."id" IS '';
COMMENT ON COLUMN "trade_order_item"."user_id" IS '';
COMMENT ON COLUMN "trade_order_item"."order_id" IS '';
COMMENT ON COLUMN "trade_order_item"."cart_id" IS '';
COMMENT ON COLUMN "trade_order_item"."spu_id" IS '';
COMMENT ON COLUMN "trade_order_item"."spu_name" IS '';
COMMENT ON COLUMN "trade_order_item"."sku_id" IS '';
COMMENT ON COLUMN "trade_order_item"."properties" IS '';
COMMENT ON COLUMN "trade_order_item"."pic_url" IS '';
COMMENT ON COLUMN "trade_order_item"."count" IS '';
COMMENT ON COLUMN "trade_order_item"."comment_status" IS '';
COMMENT ON COLUMN "trade_order_item"."price" IS '';
COMMENT ON COLUMN "trade_order_item"."discount_price" IS '';
COMMENT ON COLUMN "trade_order_item"."delivery_price" IS '';
COMMENT ON COLUMN "trade_order_item"."adjust_price" IS '';
COMMENT ON COLUMN "trade_order_item"."pay_price" IS '';
COMMENT ON COLUMN "trade_order_item"."coupon_price" IS '';
COMMENT ON COLUMN "trade_order_item"."point_price" IS '';
COMMENT ON COLUMN "trade_order_item"."use_point" IS '';
COMMENT ON COLUMN "trade_order_item"."give_point" IS '';
COMMENT ON COLUMN "trade_order_item"."vip_price" IS '';
COMMENT ON COLUMN "trade_order_item"."after_sale_id" IS '';
COMMENT ON COLUMN "trade_order_item"."after_sale_status" IS '';
COMMENT ON COLUMN "trade_order_item"."creator" IS '';
COMMENT ON COLUMN "trade_order_item"."create_time" IS '';
COMMENT ON COLUMN "trade_order_item"."updater" IS '';
COMMENT ON COLUMN "trade_order_item"."update_time" IS '';
COMMENT ON COLUMN "trade_order_item"."deleted" IS '';

-- ----------------------------
-- Table structure for "trade_after_sale"
-- ----------------------------
-- DROP TABLE IF EXISTS "trade_after_sale";
CREATE TABLE IF NOT EXISTS "trade_after_sale" (
    "id" int8 NOT NULL,
  "no" text NOT NULL,
  "status" int4 NOT NULL,
  "type" int4 NOT NULL,
  "way" int4 NOT NULL,
  "user_id" int8 NOT NULL,
  "apply_reason" text NOT NULL,
  "apply_description" text NULL,
  "apply_pic_urls" text NULL,
  "order_id" int8 NOT NULL,
  "order_no" text NOT NULL,
  "order_item_id" int8 NOT NULL,
  "spu_id" int8 NOT NULL,
  "spu_name" text NOT NULL,
  "sku_id" int8 NOT NULL,
  "properties" text NULL,
  "pic_url" text NULL,
  "count" int4 NOT NULL,
  "audit_time" text NULL,
  "audit_user_id" int8 NULL,
  "audit_reason" text NULL,
  "refund_price" int4 NOT NULL,
  "pay_refund_id" int8 NULL,
  "refund_time" text NULL,
  "logistics_id" int8 NULL,
  "logistics_no" text NULL,
  "delivery_time" text NULL,
  "receive_time" text NULL,
  "receive_reason" text NULL,
  "creator" text NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" text NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "trade_after_sale"."id" IS '';
COMMENT ON COLUMN "trade_after_sale"."no" IS '';
COMMENT ON COLUMN "trade_after_sale"."status" IS '';
COMMENT ON COLUMN "trade_after_sale"."type" IS '';
COMMENT ON COLUMN "trade_after_sale"."way" IS '';
COMMENT ON COLUMN "trade_after_sale"."user_id" IS '';
COMMENT ON COLUMN "trade_after_sale"."apply_reason" IS '';
COMMENT ON COLUMN "trade_after_sale"."apply_description" IS '';
COMMENT ON COLUMN "trade_after_sale"."apply_pic_urls" IS '';
COMMENT ON COLUMN "trade_after_sale"."order_id" IS '';
COMMENT ON COLUMN "trade_after_sale"."order_no" IS '';
COMMENT ON COLUMN "trade_after_sale"."order_item_id" IS '';
COMMENT ON COLUMN "trade_after_sale"."spu_id" IS '';
COMMENT ON COLUMN "trade_after_sale"."spu_name" IS '';
COMMENT ON COLUMN "trade_after_sale"."sku_id" IS '';
COMMENT ON COLUMN "trade_after_sale"."properties" IS '';
COMMENT ON COLUMN "trade_after_sale"."pic_url" IS '';
COMMENT ON COLUMN "trade_after_sale"."count" IS '';
COMMENT ON COLUMN "trade_after_sale"."audit_time" IS '';
COMMENT ON COLUMN "trade_after_sale"."audit_user_id" IS '';
COMMENT ON COLUMN "trade_after_sale"."audit_reason" IS '';
COMMENT ON COLUMN "trade_after_sale"."refund_price" IS '';
COMMENT ON COLUMN "trade_after_sale"."pay_refund_id" IS '';
COMMENT ON COLUMN "trade_after_sale"."refund_time" IS '';
COMMENT ON COLUMN "trade_after_sale"."logistics_id" IS '';
COMMENT ON COLUMN "trade_after_sale"."logistics_no" IS '';
COMMENT ON COLUMN "trade_after_sale"."delivery_time" IS '';
COMMENT ON COLUMN "trade_after_sale"."receive_time" IS '';
COMMENT ON COLUMN "trade_after_sale"."receive_reason" IS '';
COMMENT ON COLUMN "trade_after_sale"."creator" IS '';
COMMENT ON COLUMN "trade_after_sale"."create_time" IS '';
COMMENT ON COLUMN "trade_after_sale"."updater" IS '';
COMMENT ON COLUMN "trade_after_sale"."update_time" IS '';
COMMENT ON COLUMN "trade_after_sale"."deleted" IS '';

-- ----------------------------
-- Table structure for "trade_after_sale_log"
-- ----------------------------
-- DROP TABLE IF EXISTS "trade_after_sale_log";
CREATE TABLE IF NOT EXISTS "trade_after_sale_log" (
    "id" int8 NOT NULL,
  "user_id" int8 NOT NULL,
  "user_type" int4 NOT NULL,
  "after_sale_id" int8 NOT NULL,
  "order_id" int8 NOT NULL,
  "order_item_id" int8 NOT NULL,
  "before_status" int4 NULL,
  "after_status" int4 NOT NULL,
  "content" text NOT NULL,
  "creator" text NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" text NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "trade_after_sale_log"."id" IS '';
COMMENT ON COLUMN "trade_after_sale_log"."user_id" IS '';
COMMENT ON COLUMN "trade_after_sale_log"."user_type" IS '';
COMMENT ON COLUMN "trade_after_sale_log"."after_sale_id" IS '';
COMMENT ON COLUMN "trade_after_sale_log"."order_id" IS '';
COMMENT ON COLUMN "trade_after_sale_log"."order_item_id" IS '';
COMMENT ON COLUMN "trade_after_sale_log"."before_status" IS '';
COMMENT ON COLUMN "trade_after_sale_log"."after_status" IS '';
COMMENT ON COLUMN "trade_after_sale_log"."content" IS '';
COMMENT ON COLUMN "trade_after_sale_log"."creator" IS '';
COMMENT ON COLUMN "trade_after_sale_log"."create_time" IS '';
COMMENT ON COLUMN "trade_after_sale_log"."updater" IS '';
COMMENT ON COLUMN "trade_after_sale_log"."update_time" IS '';
COMMENT ON COLUMN "trade_after_sale_log"."deleted" IS '';

-- ----------------------------
-- Table structure for "trade_brokerage_user"
-- ----------------------------
-- DROP TABLE IF EXISTS "trade_brokerage_user";
CREATE TABLE IF NOT EXISTS "trade_brokerage_user" (
    "id" int8 NOT NULL,
  "bind_user_id" int8 NOT NULL,
  "bind_user_time" text NULL,
  "brokerage_enabled" int2 NOT NULL,
  "brokerage_time" text NULL,
  "price" int4 NOT NULL,
  "frozen_price" int4 NOT NULL,
  "creator" text NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" text NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "trade_brokerage_user"."id" IS '';
COMMENT ON COLUMN "trade_brokerage_user"."bind_user_id" IS '';
COMMENT ON COLUMN "trade_brokerage_user"."bind_user_time" IS '';
COMMENT ON COLUMN "trade_brokerage_user"."brokerage_enabled" IS '';
COMMENT ON COLUMN "trade_brokerage_user"."brokerage_time" IS '';
COMMENT ON COLUMN "trade_brokerage_user"."price" IS '';
COMMENT ON COLUMN "trade_brokerage_user"."frozen_price" IS '';
COMMENT ON COLUMN "trade_brokerage_user"."creator" IS '';
COMMENT ON COLUMN "trade_brokerage_user"."create_time" IS '';
COMMENT ON COLUMN "trade_brokerage_user"."updater" IS '';
COMMENT ON COLUMN "trade_brokerage_user"."update_time" IS '';
COMMENT ON COLUMN "trade_brokerage_user"."deleted" IS '';
COMMENT ON COLUMN "trade_brokerage_user"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "trade_brokerage_record"
-- ----------------------------
-- DROP TABLE IF EXISTS "trade_brokerage_record";
CREATE TABLE IF NOT EXISTS "trade_brokerage_record" (
    "id" int8 NOT NULL,
  "user_id" int8 NOT NULL,
  "biz_id" text NOT NULL,
  "biz_type" text NOT NULL,
  "title" text NOT NULL,
  "price" int4 NOT NULL,
  "total_price" int4 NOT NULL,
  "description" text NOT NULL,
  "status" text NOT NULL,
  "frozen_days" int4 NOT NULL,
  "unfreeze_time" text NULL,
  "source_user_level" int4 NULL,
  "source_user_id" int8 NULL,
  "creator" text NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" text NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "trade_brokerage_record"."id" IS '';
COMMENT ON COLUMN "trade_brokerage_record"."user_id" IS '';
COMMENT ON COLUMN "trade_brokerage_record"."biz_id" IS '';
COMMENT ON COLUMN "trade_brokerage_record"."biz_type" IS '';
COMMENT ON COLUMN "trade_brokerage_record"."title" IS '';
COMMENT ON COLUMN "trade_brokerage_record"."price" IS '';
COMMENT ON COLUMN "trade_brokerage_record"."total_price" IS '';
COMMENT ON COLUMN "trade_brokerage_record"."description" IS '';
COMMENT ON COLUMN "trade_brokerage_record"."status" IS '';
COMMENT ON COLUMN "trade_brokerage_record"."frozen_days" IS '';
COMMENT ON COLUMN "trade_brokerage_record"."unfreeze_time" IS '';
COMMENT ON COLUMN "trade_brokerage_record"."source_user_level" IS '';
COMMENT ON COLUMN "trade_brokerage_record"."source_user_id" IS '';
COMMENT ON COLUMN "trade_brokerage_record"."creator" IS '';
COMMENT ON COLUMN "trade_brokerage_record"."create_time" IS '';
COMMENT ON COLUMN "trade_brokerage_record"."updater" IS '';
COMMENT ON COLUMN "trade_brokerage_record"."update_time" IS '';
COMMENT ON COLUMN "trade_brokerage_record"."deleted" IS '';
COMMENT ON COLUMN "trade_brokerage_record"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "trade_brokerage_withdraw"
-- ----------------------------
-- DROP TABLE IF EXISTS "trade_brokerage_withdraw";
CREATE TABLE IF NOT EXISTS "trade_brokerage_withdraw" (
    "id" int4 NOT NULL,
  "user_id" int8 NOT NULL,
  "price" int4 NOT NULL,
  "fee_price" int4 NOT NULL,
  "total_price" int4 NOT NULL,
  "type" text NOT NULL,
  "name" text NULL,
  "account_no" text NULL,
  "bank_name" text NULL,
  "bank_address" text NULL,
  "account_qr_code_url" text NULL,
  "status" text NOT NULL,
  "audit_reason" text NULL,
  "audit_time" text NULL,
  "remark" text NULL,
  "creator" text NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" text NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "trade_brokerage_withdraw"."id" IS '';
COMMENT ON COLUMN "trade_brokerage_withdraw"."user_id" IS '';
COMMENT ON COLUMN "trade_brokerage_withdraw"."price" IS '';
COMMENT ON COLUMN "trade_brokerage_withdraw"."fee_price" IS '';
COMMENT ON COLUMN "trade_brokerage_withdraw"."total_price" IS '';
COMMENT ON COLUMN "trade_brokerage_withdraw"."type" IS '';
COMMENT ON COLUMN "trade_brokerage_withdraw"."name" IS '';
COMMENT ON COLUMN "trade_brokerage_withdraw"."account_no" IS '';
COMMENT ON COLUMN "trade_brokerage_withdraw"."bank_name" IS '';
COMMENT ON COLUMN "trade_brokerage_withdraw"."bank_address" IS '';
COMMENT ON COLUMN "trade_brokerage_withdraw"."account_qr_code_url" IS '';
COMMENT ON COLUMN "trade_brokerage_withdraw"."status" IS '';
COMMENT ON COLUMN "trade_brokerage_withdraw"."audit_reason" IS '';
COMMENT ON COLUMN "trade_brokerage_withdraw"."audit_time" IS '';
COMMENT ON COLUMN "trade_brokerage_withdraw"."remark" IS '';
COMMENT ON COLUMN "trade_brokerage_withdraw"."creator" IS '';
COMMENT ON COLUMN "trade_brokerage_withdraw"."create_time" IS '';
COMMENT ON COLUMN "trade_brokerage_withdraw"."updater" IS '';
COMMENT ON COLUMN "trade_brokerage_withdraw"."update_time" IS '';
COMMENT ON COLUMN "trade_brokerage_withdraw"."deleted" IS '';
COMMENT ON COLUMN "trade_brokerage_withdraw"."tenant_id" IS '';

-- ----------------------------
-- Table structure for "trade_delivery_express"
-- ----------------------------
-- DROP TABLE IF EXISTS "trade_delivery_express";
CREATE TABLE IF NOT EXISTS "trade_delivery_express" (
    "id" int4 NOT NULL,
  "code" text NULL,
  "name" text NULL,
  "logo" text NULL,
  "sort" int4 NOT NULL,
  "status" int4 NOT NULL,
  "creator" text NULL DEFAULT '',
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" text NULL DEFAULT '',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

COMMENT ON COLUMN "trade_delivery_express"."id" IS '';
COMMENT ON COLUMN "trade_delivery_express"."code" IS '';
COMMENT ON COLUMN "trade_delivery_express"."name" IS '';
COMMENT ON COLUMN "trade_delivery_express"."logo" IS '';
COMMENT ON COLUMN "trade_delivery_express"."sort" IS '';
COMMENT ON COLUMN "trade_delivery_express"."status" IS '';
COMMENT ON COLUMN "trade_delivery_express"."creator" IS '';
COMMENT ON COLUMN "trade_delivery_express"."create_time" IS '';
COMMENT ON COLUMN "trade_delivery_express"."updater" IS '';
COMMENT ON COLUMN "trade_delivery_express"."update_time" IS '';
COMMENT ON COLUMN "trade_delivery_express"."deleted" IS '';



-- =====================================================================
-- SPK-OS yudao-module-spk-delivery 表结构（PostgreSQL）
-- IPD 交付子系统：agent 任务 / Aegis 审查 / Workflow Contract / CCB /
-- OR 池 / R7 反馈 / R8 退市 / 门禁审计 / DCP 回退日志
-- 说明：全部带 tenant_id int8 NOT NULL DEFAULT 0；主键 id int8 + *_seq 序列
-- =====================================================================

-- ----------------------------
-- 1. spk_agent_task：Agent 任务执行实例
-- ----------------------------
CREATE TABLE IF NOT EXISTS "spk_agent_task" (
    "id" int8 NOT NULL,
    "task_id" varchar(64) NULL,
    "role_id" int8 NULL,
    "conversation_id" int8 NULL,
    "prompt" text NULL,
    "status" varchar(16) NOT NULL DEFAULT 'running',
    "result" text NULL,
    "instance_id" varchar(64) NULL,
    "node_key" varchar(64) NULL,
    "receive_task_key" varchar(64) NULL,
    "ttl_expire_time" int8 NULL DEFAULT 0,
    "creator" varchar(64) NULL DEFAULT '',
    "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater" varchar(64) NULL DEFAULT '',
    "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted" int2 NOT NULL DEFAULT 0,
    "tenant_id" int8 NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "spk_agent_task" IS 'SPK-OS Agent 任务执行实例';
COMMENT ON COLUMN "spk_agent_task"."status" IS '状态 running/done/failed/cancelled';
CREATE SEQUENCE IF NOT EXISTS spk_agent_task_seq;

-- ----------------------------
-- 2. spk_aegis_review：Aegis 审查结论
-- ----------------------------
CREATE TABLE IF NOT EXISTS "spk_aegis_review" (
    "id" int8 NOT NULL,
    "review_id" varchar(64) NULL,
    "instance_id" varchar(64) NULL,
    "verdict" varchar(16) NULL,
    "report" text NULL,
    "evidence" text NULL,
    "node_key" varchar(64) NULL,
    "creator" varchar(64) NULL DEFAULT '',
    "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater" varchar(64) NULL DEFAULT '',
    "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted" int2 NOT NULL DEFAULT 0,
    "tenant_id" int8 NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "spk_aegis_review" IS 'SPK-OS Aegis 审查结论';
COMMENT ON COLUMN "spk_aegis_review"."verdict" IS '裁决 pass/fail/conditional';
CREATE SEQUENCE IF NOT EXISTS spk_aegis_review_seq;

-- ----------------------------
-- 3. spk_contract_version：Workflow Contract 版本治理
-- ----------------------------
CREATE TABLE IF NOT EXISTS "spk_contract_version" (
    "id" int8 NOT NULL,
    "model_key" varchar(64) NOT NULL,
    "hash" varchar(64) NOT NULL,
    "version" int4 NOT NULL DEFAULT 1,
    "diff_json" text NULL,
    "snapshot_json" text NULL,
    "creator" varchar(64) NULL DEFAULT '',
    "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater" varchar(64) NULL DEFAULT '',
    "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted" int2 NOT NULL DEFAULT 0,
    "tenant_id" int8 NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "spk_contract_version" IS 'SPK-OS Workflow Contract 版本治理';
CREATE SEQUENCE IF NOT EXISTS spk_contract_version_seq;

-- ----------------------------
-- 4. spk_ccb_record：CCB 变更台账
-- ----------------------------
CREATE TABLE IF NOT EXISTS "spk_ccb_record" (
    "id" int8 NOT NULL,
    "change_id" varchar(64) NULL,
    "instance_id" varchar(64) NULL,
    "change_request" text NULL,
    "impact" text NULL,
    "decision" varchar(16) NULL,
    "creator" varchar(64) NULL DEFAULT '',
    "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater" varchar(64) NULL DEFAULT '',
    "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted" int2 NOT NULL DEFAULT 0,
    "tenant_id" int8 NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "spk_ccb_record" IS 'SPK-OS CCB 变更台账';
COMMENT ON COLUMN "spk_ccb_record"."decision" IS '决议 approve/reject/defer';
CREATE SEQUENCE IF NOT EXISTS spk_ccb_record_seq;

-- ----------------------------
-- 5. spk_intellect_queue：OR 池需求队列
-- ----------------------------
CREATE TABLE IF NOT EXISTS "spk_intellect_queue" (
    "id" int8 NOT NULL,
    "source" varchar(32) NULL,
    "req_id" varchar(64) NULL,
    "status" varchar(16) NOT NULL DEFAULT 'pending',
    "dedup_hash" varchar(64) NULL,
    "payload" text NULL,
    "creator" varchar(64) NULL DEFAULT '',
    "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater" varchar(64) NULL DEFAULT '',
    "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted" int2 NOT NULL DEFAULT 0,
    "tenant_id" int8 NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "spk_intellect_queue" IS 'SPK-OS OR 池需求队列';
CREATE SEQUENCE IF NOT EXISTS spk_intellect_queue_seq;

-- ----------------------------
-- 6. spk_feedback：R7 反馈数据
-- ----------------------------
CREATE TABLE IF NOT EXISTS "spk_feedback" (
    "id" int8 NOT NULL,
    "instance_id" varchar(64) NULL,
    "source" varchar(32) NULL,
    "content" text NULL,
    "summary" text NULL,
    "new_charter_seed" boolean NULL DEFAULT false,
    "creator" varchar(64) NULL DEFAULT '',
    "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater" varchar(64) NULL DEFAULT '',
    "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted" int2 NOT NULL DEFAULT 0,
    "tenant_id" int8 NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "spk_feedback" IS 'SPK-OS R7 反馈数据';
CREATE SEQUENCE IF NOT EXISTS spk_feedback_seq;

-- ----------------------------
-- 7. spk_sunset：R8 退市数据
-- ----------------------------
CREATE TABLE IF NOT EXISTS "spk_sunset" (
    "id" int8 NOT NULL,
    "instance_id" varchar(64) NULL,
    "sunset_report" text NULL,
    "archive_status" varchar(16) NULL,
    "creator" varchar(64) NULL DEFAULT '',
    "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater" varchar(64) NULL DEFAULT '',
    "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted" int2 NOT NULL DEFAULT 0,
    "tenant_id" int8 NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "spk_sunset" IS 'SPK-OS R8 退市数据';
CREATE SEQUENCE IF NOT EXISTS spk_sunset_seq;

-- ----------------------------
-- 8. spk_gate_record：门禁回调审计
-- ----------------------------
CREATE TABLE IF NOT EXISTS "spk_gate_record" (
    "id" int8 NOT NULL,
    "instance_id" varchar(64) NOT NULL,
    "node_key" varchar(64) NULL,
    "gate" varchar(16) NOT NULL,
    "report" text NULL,
    "pass" boolean NULL,
    "callback_time" timestamp NULL,
    "creator" varchar(64) NULL DEFAULT '',
    "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater" varchar(64) NULL DEFAULT '',
    "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted" int2 NOT NULL DEFAULT 0,
    "tenant_id" int8 NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "spk_gate_record" IS 'SPK-OS 门禁回调审计';
COMMENT ON COLUMN "spk_gate_record"."gate" IS '门禁标识 g1..g8 / tr2..tr6';
CREATE SEQUENCE IF NOT EXISTS spk_gate_record_seq;

-- ----------------------------
-- 9. spk_dcp_redirect_log：DCP 回退日志
-- ----------------------------
CREATE TABLE IF NOT EXISTS "spk_dcp_redirect_log" (
    "id" int8 NOT NULL,
    "instance_id" varchar(64) NOT NULL,
    "dcp" varchar(16) NOT NULL,
    "redirect_count" int4 NOT NULL DEFAULT 0,
    "target_node" varchar(64) NULL,
    "creator" varchar(64) NULL DEFAULT '',
    "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater" varchar(64) NULL DEFAULT '',
    "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted" int2 NOT NULL DEFAULT 0,
    "tenant_id" int8 NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "spk_dcp_redirect_log" IS 'SPK-OS DCP 回退日志';
COMMENT ON COLUMN "spk_dcp_redirect_log"."dcp" IS 'DCP 标识 cdc/pdc/adc/ldc';
CREATE SEQUENCE IF NOT EXISTS spk_dcp_redirect_log_seq;

-- ----------------------------
-- SPK-OS IPD 流程可视化：菜单 + 按钮权限（流程实例详情页「IPD 产物」tab + IPD 流程列表入口）
-- 复用 yudao 原生流程实例详情页，仅增量查询接口。超管（role_id=1）自动授权。
-- ----------------------------
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, updater, deleted)
VALUES (6800, 'SPK 研发', '', 1, 50, 0, '/spk', 'ep:cpu', NULL, NULL, 0, true, true, true, 'admin', 'admin', 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, updater, deleted)
VALUES (6801, 'IPD 流程', '', 2, 1, 6800, 'ipd-process', 'ep:set-up', 'spk/ipd/process/index', 'SpkIpdProcess', 0, true, true, true, 'admin', 'admin', 0)
ON CONFLICT (id) DO NOTHING;

-- IPD 流程列表查询按钮
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, updater, deleted)
VALUES (6802, 'IPD 流程查询', 'spk:ipd-process:query', 3, 1, 6801, '', '#', '', NULL, 0, true, true, true, 'admin', 'admin', 0)
ON CONFLICT (id) DO NOTHING;

-- 7 个产物域查询按钮权限（供流程实例详情页「IPD 产物」tab 调用聚合查询接口）
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, updater, deleted)
VALUES
(6803, '门禁记录查询', 'spk-delivery:gate:query',     3, 2,  6801, '', '#', '', NULL, 0, true, true, true, 'admin', 'admin', 0),
(6804, 'Agent 产物查询', 'spk-delivery:agent:query',   3, 3,  6801, '', '#', '', NULL, 0, true, true, true, 'admin', 'admin', 0),
(6805, 'Aegis 裁决查询', 'spk-delivery:aegis:query',  3, 4,  6801, '', '#', '', NULL, 0, true, true, true, 'admin', 'admin', 0),
(6806, 'CCB 台账查询',   'spk-delivery:ccb:query',    3, 5,  6801, '', '#', '', NULL, 0, true, true, true, 'admin', 'admin', 0),
(6807, 'DCP 回退查询',   'spk-delivery:dcp:query',    3, 6,  6801, '', '#', '', NULL, 0, true, true, true, 'admin', 'admin', 0),
(6808, 'R7 反馈查询',    'spk-delivery:feedback:query', 3, 7, 6801, '', '#', '', NULL, 0, true, true, true, 'admin', 'admin', 0),
(6809, 'R8 退市查询',    'spk-delivery:sunset:query',  3, 8, 6801, '', '#', '', NULL, 0, true, true, true, 'admin', 'admin', 0)
ON CONFLICT (id) DO NOTHING;

-- 超级管理员（role_id=1）自动授权以上菜单
INSERT INTO system_role_menu (id, role_id, menu_id, creator, updater, deleted, tenant_id)
VALUES
(6900, 1, 6800, 'admin', 'admin', 0, 1),
(6901, 1, 6801, 'admin', 'admin', 0, 1),
(6902, 1, 6802, 'admin', 'admin', 0, 1),
(6903, 1, 6803, 'admin', 'admin', 0, 1),
(6904, 1, 6804, 'admin', 'admin', 0, 1),
(6905, 1, 6805, 'admin', 'admin', 0, 1),
(6906, 1, 6806, 'admin', 'admin', 0, 1),
(6907, 1, 6807, 'admin', 'admin', 0, 1),
(6908, 1, 6808, 'admin', 'admin', 0, 1),
(6909, 1, 6809, 'admin', 'admin', 0, 1)
ON CONFLICT (id) DO NOTHING;
