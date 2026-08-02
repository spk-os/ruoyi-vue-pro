-- ============================================================
-- MP 模块表 DDL（PostgreSQL）— 从本地 DO 反推
-- ============================================================
-- 来源：从 yudao-module-mp 的 *DO.java 反推生成（非社区 MySQL 脚本转换）
-- 数据库：PostgreSQL（docker-db_postgres-1 / 127.0.0.1:5433 / db=yudao）
-- 安全：仅 DROP IF EXISTS + CREATE IF NOT EXISTS（幂等）；无 INSERT、无外键约束、无 ENGINE
-- 约定：见同目录 ../_CONVENTIONS.md（tenant_id 全表必加、序列取 @KeySequence 字面值、JSON→text）
-- 运行：psql -1 -v ON_ERROR_STOP=1 -f 04_mp_tables.sql
-- ============================================================

-- ===================== mp_account =====================
-- 公众号账号
DROP TABLE IF EXISTS "mp_account";
CREATE TABLE IF NOT EXISTS "mp_account" (
    "id"           bigint      NOT NULL,
    "name"         varchar(255) NULL,
    "account"      varchar(64)  NULL,
    "app_id"       varchar(64)  NULL,
    "app_secret"   varchar(128) NULL,
    "token"        varchar(128) NULL,
    "aes_key"      varchar(128) NULL,
    "qr_code_url"  varchar(500) NULL,
    "remark"       text         NULL,
    "creator"      varchar(64)  DEFAULT '',
    "create_time"  timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"      varchar(64)  DEFAULT '',
    "update_time"  timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"      int2        NOT NULL DEFAULT 0,
    "tenant_id"    bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "mp_account" IS '公众号账号';
COMMENT ON COLUMN "mp_account"."name" IS '公众号名称';
COMMENT ON COLUMN "mp_account"."account" IS '公众号账号';
COMMENT ON COLUMN "mp_account"."app_id" IS '公众号 appid';
COMMENT ON COLUMN "mp_account"."app_secret" IS '公众号密钥';
COMMENT ON COLUMN "mp_account"."token" IS '公众号 token';
COMMENT ON COLUMN "mp_account"."aes_key" IS '消息加解密密钥';
COMMENT ON COLUMN "mp_account"."qr_code_url" IS '二维码图片 URL';
COMMENT ON COLUMN "mp_account"."remark" IS '备注';

CREATE SEQUENCE IF NOT EXISTS mp_account_seq;

CREATE UNIQUE INDEX IF NOT EXISTS idx_mp_account_app_id ON "mp_account" ("app_id", tenant_id) WHERE deleted = 0;

-- ===================== mp_material =====================
-- 公众号素材
DROP TABLE IF EXISTS "mp_material";
CREATE TABLE IF NOT EXISTS "mp_material" (
    "id"            bigint      NOT NULL,
    "account_id"    bigint      NULL,
    "app_id"        varchar(64)  NULL,
    "media_id"      varchar(64)  NULL,
    "type"          varchar(32)  NULL,
    "permanent"     int2        NOT NULL DEFAULT 0,
    "url"           varchar(500) NULL,
    "name"          varchar(255) NULL,
    "mp_url"        varchar(500) NULL,
    "title"         varchar(255) NULL,
    "introduction"  text         NULL,
    "creator"       varchar(64)  DEFAULT '',
    "create_time"   timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"       varchar(64)  DEFAULT '',
    "update_time"   timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"       int2        NOT NULL DEFAULT 0,
    "tenant_id"     bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "mp_material" IS '公众号素材';
COMMENT ON COLUMN "mp_material"."account_id" IS '公众号账号的编号';
COMMENT ON COLUMN "mp_material"."app_id" IS '公众号 appId';
COMMENT ON COLUMN "mp_material"."media_id" IS '公众号素材 id';
COMMENT ON COLUMN "mp_material"."type" IS '文件类型';
COMMENT ON COLUMN "mp_material"."permanent" IS '是否永久';
COMMENT ON COLUMN "mp_material"."url" IS '文件服务器的 URL';
COMMENT ON COLUMN "mp_material"."name" IS '名字';
COMMENT ON COLUMN "mp_material"."mp_url" IS '公众号文件 URL';
COMMENT ON COLUMN "mp_material"."title" IS '视频素材的标题';
COMMENT ON COLUMN "mp_material"."introduction" IS '视频素材的描述';

CREATE SEQUENCE IF NOT EXISTS mp_material_seq;

CREATE INDEX IF NOT EXISTS idx_mp_material_account_id ON "mp_material" ("account_id");

-- ===================== mp_menu =====================
-- 公众号菜单
DROP TABLE IF EXISTS "mp_menu";
CREATE TABLE IF NOT EXISTS "mp_menu" (
    "id"                    bigint      NOT NULL,
    "account_id"            bigint      NULL,
    "app_id"                varchar(64)  NULL,
    "name"                  varchar(128) NULL,
    "menu_key"              varchar(64)  NULL,
    "parent_id"             bigint      NULL,
    "type"                  varchar(32)  NULL,
    "url"                   varchar(1024) NULL,
    "mini_program_app_id"   varchar(64)  NULL,
    "mini_program_page_path" varchar(500) NULL,
    "article_id"            varchar(64)  NULL,
    "reply_message_type"    varchar(32)  NULL,
    "reply_content"         text         NULL,
    "reply_media_id"        varchar(64)  NULL,
    "reply_media_url"       varchar(500) NULL,
    "reply_title"           varchar(255) NULL,
    "reply_description"     text         NULL,
    "reply_thumb_media_id"  varchar(64)  NULL,
    "reply_thumb_media_url" varchar(500) NULL,
    "reply_articles"        text         NULL,
    "reply_music_url"       varchar(500) NULL,
    "reply_hq_music_url"    varchar(500) NULL,
    "creator"               varchar(64)  DEFAULT '',
    "create_time"           timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"               varchar(64)  DEFAULT '',
    "update_time"           timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"               int2        NOT NULL DEFAULT 0,
    "tenant_id"             bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "mp_menu" IS '公众号菜单';
COMMENT ON COLUMN "mp_menu"."account_id" IS '公众号账号的编号';
COMMENT ON COLUMN "mp_menu"."app_id" IS '公众号 appId';
COMMENT ON COLUMN "mp_menu"."name" IS '菜单名称';
COMMENT ON COLUMN "mp_menu"."menu_key" IS '菜单标识';
COMMENT ON COLUMN "mp_menu"."parent_id" IS '父菜单编号';
COMMENT ON COLUMN "mp_menu"."type" IS '按钮类型';
COMMENT ON COLUMN "mp_menu"."url" IS '网页链接';
COMMENT ON COLUMN "mp_menu"."mini_program_app_id" IS '小程序的 appId';
COMMENT ON COLUMN "mp_menu"."mini_program_page_path" IS '小程序的页面路径';
COMMENT ON COLUMN "mp_menu"."article_id" IS '跳转图文的媒体编号';
COMMENT ON COLUMN "mp_menu"."reply_message_type" IS '回复的消息类型';
COMMENT ON COLUMN "mp_menu"."reply_content" IS '回复的消息内容';
COMMENT ON COLUMN "mp_menu"."reply_media_id" IS '回复的媒体 id';
COMMENT ON COLUMN "mp_menu"."reply_media_url" IS '回复的媒体 URL';
COMMENT ON COLUMN "mp_menu"."reply_title" IS '回复的标题';
COMMENT ON COLUMN "mp_menu"."reply_description" IS '回复的描述';
COMMENT ON COLUMN "mp_menu"."reply_thumb_media_id" IS '回复的缩略图的媒体 id';
COMMENT ON COLUMN "mp_menu"."reply_thumb_media_url" IS '回复的缩略图的媒体 URL';
COMMENT ON COLUMN "mp_menu"."reply_articles" IS '回复的图文消息数组';
COMMENT ON COLUMN "mp_menu"."reply_music_url" IS '回复的音乐链接';
COMMENT ON COLUMN "mp_menu"."reply_hq_music_url" IS '回复的高质量音乐链接';

CREATE SEQUENCE IF NOT EXISTS mp_menu_seq;

CREATE INDEX IF NOT EXISTS idx_mp_menu_account_id ON "mp_menu" ("account_id");
CREATE INDEX IF NOT EXISTS idx_mp_menu_parent_id ON "mp_menu" ("parent_id");

-- ===================== mp_auto_reply =====================
-- 公众号消息自动回复
DROP TABLE IF EXISTS "mp_auto_reply";
CREATE TABLE IF NOT EXISTS "mp_auto_reply" (
    "id"                       bigint      NOT NULL,
    "account_id"               bigint      NULL,
    "app_id"                   varchar(64)  NULL,
    "type"                     int4        NULL,
    "request_keyword"          varchar(255) NULL,
    "request_match"            int4        NULL,
    "request_message_type"     varchar(32)  NULL,
    "response_message_type"    varchar(32)  NULL,
    "response_content"         text         NULL,
    "response_media_id"        varchar(64)  NULL,
    "response_media_url"       varchar(500) NULL,
    "response_title"           varchar(255) NULL,
    "response_description"     text         NULL,
    "response_thumb_media_id"  varchar(64)  NULL,
    "response_thumb_media_url" varchar(500) NULL,
    "response_articles"        text         NULL,
    "response_music_url"       varchar(500) NULL,
    "response_hq_music_url"    varchar(500) NULL,
    "creator"                  varchar(64)  DEFAULT '',
    "create_time"              timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"                  varchar(64)  DEFAULT '',
    "update_time"              timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"                  int2        NOT NULL DEFAULT 0,
    "tenant_id"                bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "mp_auto_reply" IS '公众号消息自动回复';
COMMENT ON COLUMN "mp_auto_reply"."account_id" IS '公众号账号的编号';
COMMENT ON COLUMN "mp_auto_reply"."app_id" IS '公众号 appId';
COMMENT ON COLUMN "mp_auto_reply"."type" IS '回复类型';
COMMENT ON COLUMN "mp_auto_reply"."request_keyword" IS '请求的关键字';
COMMENT ON COLUMN "mp_auto_reply"."request_match" IS '请求的关键字的匹配';
COMMENT ON COLUMN "mp_auto_reply"."request_message_type" IS '请求的消息类型';
COMMENT ON COLUMN "mp_auto_reply"."response_message_type" IS '回复的消息类型';
COMMENT ON COLUMN "mp_auto_reply"."response_content" IS '回复的消息内容';
COMMENT ON COLUMN "mp_auto_reply"."response_media_id" IS '回复的媒体 id';
COMMENT ON COLUMN "mp_auto_reply"."response_media_url" IS '回复的媒体 URL';
COMMENT ON COLUMN "mp_auto_reply"."response_title" IS '回复的标题';
COMMENT ON COLUMN "mp_auto_reply"."response_description" IS '回复的描述';
COMMENT ON COLUMN "mp_auto_reply"."response_thumb_media_id" IS '回复的缩略图的媒体 id';
COMMENT ON COLUMN "mp_auto_reply"."response_thumb_media_url" IS '回复的缩略图的媒体 URL';
COMMENT ON COLUMN "mp_auto_reply"."response_articles" IS '回复的图文消息';
COMMENT ON COLUMN "mp_auto_reply"."response_music_url" IS '回复的音乐链接';
COMMENT ON COLUMN "mp_auto_reply"."response_hq_music_url" IS '回复的高质量音乐链接';

CREATE SEQUENCE IF NOT EXISTS mp_auto_reply_seq;

CREATE INDEX IF NOT EXISTS idx_mp_auto_reply_account_id ON "mp_auto_reply" ("account_id");

-- ===================== mp_message =====================
-- 公众号消息
DROP TABLE IF EXISTS "mp_message";
CREATE TABLE IF NOT EXISTS "mp_message" (
    "id"               bigint      NOT NULL,
    "msg_id"           bigint      NULL,
    "account_id"       bigint      NULL,
    "app_id"           varchar(64)  NULL,
    "user_id"          bigint      NULL,
    "openid"           varchar(64)  NULL,
    "type"             varchar(32)  NULL,
    "send_from"        int4        NULL,
    "content"          text         NULL,
    "media_id"         varchar(64)  NULL,
    "media_url"        varchar(500) NULL,
    "recognition"      text         NULL,
    "format"           varchar(32)  NULL,
    "title"            varchar(255) NULL,
    "description"      text         NULL,
    "thumb_media_id"   varchar(64)  NULL,
    "thumb_media_url"  varchar(500) NULL,
    "url"              varchar(500) NULL,
    "location_x"       float8       NULL,
    "location_y"       float8       NULL,
    "scale"            float8       NULL,
    "label"            varchar(255) NULL,
    "articles"         text         NULL,
    "music_url"        varchar(500) NULL,
    "hq_music_url"     varchar(500) NULL,
    "event"            varchar(32)  NULL,
    "event_key"        varchar(255) NULL,
    "creator"          varchar(64)  DEFAULT '',
    "create_time"      timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"          varchar(64)  DEFAULT '',
    "update_time"      timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"          int2        NOT NULL DEFAULT 0,
    "tenant_id"        bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "mp_message" IS '公众号消息';
COMMENT ON COLUMN "mp_message"."msg_id" IS '微信公众号消息 id';
COMMENT ON COLUMN "mp_message"."account_id" IS '公众号账号的 ID';
COMMENT ON COLUMN "mp_message"."app_id" IS '公众号 appid';
COMMENT ON COLUMN "mp_message"."user_id" IS '公众号粉丝的编号';
COMMENT ON COLUMN "mp_message"."openid" IS '公众号粉丝标志';
COMMENT ON COLUMN "mp_message"."type" IS '消息类型';
COMMENT ON COLUMN "mp_message"."send_from" IS '消息来源';
COMMENT ON COLUMN "mp_message"."content" IS '消息内容';
COMMENT ON COLUMN "mp_message"."media_id" IS '媒体文件的编号';
COMMENT ON COLUMN "mp_message"."media_url" IS '媒体文件的 URL';
COMMENT ON COLUMN "mp_message"."recognition" IS '语音识别后文本';
COMMENT ON COLUMN "mp_message"."format" IS '语音格式';
COMMENT ON COLUMN "mp_message"."title" IS '标题';
COMMENT ON COLUMN "mp_message"."description" IS '描述';
COMMENT ON COLUMN "mp_message"."thumb_media_id" IS '缩略图的媒体 id';
COMMENT ON COLUMN "mp_message"."thumb_media_url" IS '缩略图的媒体 URL';
COMMENT ON COLUMN "mp_message"."url" IS '点击图文消息跳转链接';
COMMENT ON COLUMN "mp_message"."location_x" IS '地理位置维度';
COMMENT ON COLUMN "mp_message"."location_y" IS '地理位置经度';
COMMENT ON COLUMN "mp_message"."scale" IS '地图缩放大小';
COMMENT ON COLUMN "mp_message"."label" IS '详细地址';
COMMENT ON COLUMN "mp_message"."articles" IS '图文消息数组';
COMMENT ON COLUMN "mp_message"."music_url" IS '音乐链接';
COMMENT ON COLUMN "mp_message"."hq_music_url" IS '高质量音乐链接';
COMMENT ON COLUMN "mp_message"."event" IS '事件类型';
COMMENT ON COLUMN "mp_message"."event_key" IS '事件 Key';

CREATE SEQUENCE IF NOT EXISTS mp_message_seq;

CREATE INDEX IF NOT EXISTS idx_mp_message_account_id ON "mp_message" ("account_id");
CREATE INDEX IF NOT EXISTS idx_mp_message_user_id ON "mp_message" ("user_id");

-- ===================== mp_message_template =====================
-- 公众号模版消息
DROP TABLE IF EXISTS "mp_message_template";
CREATE TABLE IF NOT EXISTS "mp_message_template" (
    "id"               bigint      NOT NULL,
    "account_id"       bigint      NULL,
    "app_id"           varchar(64)  NULL,
    "template_id"      varchar(64)  NULL,
    "title"            varchar(255) NULL,
    "content"          text         NULL,
    "example"          text         NULL,
    "primary_industry" varchar(64)  NULL,
    "deputy_industry"  varchar(64)  NULL,
    "creator"          varchar(64)  DEFAULT '',
    "create_time"      timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"          varchar(64)  DEFAULT '',
    "update_time"      timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"          int2        NOT NULL DEFAULT 0,
    "tenant_id"        bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "mp_message_template" IS '公众号模版消息';
COMMENT ON COLUMN "mp_message_template"."account_id" IS '公众号账号的编号';
COMMENT ON COLUMN "mp_message_template"."app_id" IS '公众号 appId';
COMMENT ON COLUMN "mp_message_template"."template_id" IS '公众号模板 ID';
COMMENT ON COLUMN "mp_message_template"."title" IS '标题';
COMMENT ON COLUMN "mp_message_template"."content" IS '模板内容';
COMMENT ON COLUMN "mp_message_template"."example" IS '模板示例';
COMMENT ON COLUMN "mp_message_template"."primary_industry" IS '模板所属行业的一级行业';
COMMENT ON COLUMN "mp_message_template"."deputy_industry" IS '模板所属行业的二级行业';

CREATE SEQUENCE IF NOT EXISTS mp_message_template_seq;

CREATE INDEX IF NOT EXISTS idx_mp_message_template_account_id ON "mp_message_template" ("account_id");

-- ===================== mp_tag =====================
-- 公众号标签
DROP TABLE IF EXISTS "mp_tag";
CREATE TABLE IF NOT EXISTS "mp_tag" (
    "id"          bigint      NOT NULL,
    "tag_id"      bigint      NULL,
    "name"        varchar(255) NULL,
    "count"       int4        NULL,
    "account_id"  bigint      NULL,
    "app_id"      varchar(64)  NULL,
    "creator"     varchar(64)  DEFAULT '',
    "create_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"     varchar(64)  DEFAULT '',
    "update_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"     int2        NOT NULL DEFAULT 0,
    "tenant_id"   bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "mp_tag" IS '公众号标签';
COMMENT ON COLUMN "mp_tag"."tag_id" IS '公众号标签 id';
COMMENT ON COLUMN "mp_tag"."name" IS '标签名';
COMMENT ON COLUMN "mp_tag"."count" IS '此标签下粉丝数';
COMMENT ON COLUMN "mp_tag"."account_id" IS '公众号账号的编号';
COMMENT ON COLUMN "mp_tag"."app_id" IS '公众号 appId';

CREATE SEQUENCE IF NOT EXISTS mp_tag_seq;

CREATE INDEX IF NOT EXISTS idx_mp_tag_account_id ON "mp_tag" ("account_id");

-- ===================== mp_user =====================
-- 微信公众号粉丝
DROP TABLE IF EXISTS "mp_user";
CREATE TABLE IF NOT EXISTS "mp_user" (
    "id"                bigint      NOT NULL,
    "openid"            varchar(64)  NULL,
    "union_id"          varchar(64)  NULL,
    "subscribe_status"  int4        NULL,
    "subscribe_time"    timestamp   NULL,
    "unsubscribe_time"  timestamp   NULL,
    "nickname"          varchar(255) NULL,
    "head_image_url"    varchar(500) NULL,
    "language"          varchar(32)  NULL,
    "country"           varchar(64)  NULL,
    "province"          varchar(64)  NULL,
    "city"              varchar(64)  NULL,
    "remark"            text         NULL,
    "tag_ids"           varchar(255) NULL,
    "account_id"        bigint      NULL,
    "app_id"            varchar(64)  NULL,
    "creator"           varchar(64)  DEFAULT '',
    "create_time"       timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"           varchar(64)  DEFAULT '',
    "update_time"       timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"           int2        NOT NULL DEFAULT 0,
    "tenant_id"         bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "mp_user" IS '微信公众号粉丝';
COMMENT ON COLUMN "mp_user"."openid" IS '粉丝标识';
COMMENT ON COLUMN "mp_user"."union_id" IS '微信生态唯一标识';
COMMENT ON COLUMN "mp_user"."subscribe_status" IS '关注状态';
COMMENT ON COLUMN "mp_user"."subscribe_time" IS '关注时间';
COMMENT ON COLUMN "mp_user"."unsubscribe_time" IS '取消关注时间';
COMMENT ON COLUMN "mp_user"."nickname" IS '昵称';
COMMENT ON COLUMN "mp_user"."head_image_url" IS '头像地址';
COMMENT ON COLUMN "mp_user"."language" IS '语言';
COMMENT ON COLUMN "mp_user"."country" IS '国家';
COMMENT ON COLUMN "mp_user"."province" IS '省份';
COMMENT ON COLUMN "mp_user"."city" IS '城市';
COMMENT ON COLUMN "mp_user"."remark" IS '备注';
COMMENT ON COLUMN "mp_user"."tag_ids" IS '标签编号数组';
COMMENT ON COLUMN "mp_user"."account_id" IS '公众号账号的编号';
COMMENT ON COLUMN "mp_user"."app_id" IS '公众号 appId';

CREATE SEQUENCE IF NOT EXISTS mp_user_seq;

CREATE UNIQUE INDEX IF NOT EXISTS idx_mp_user_account_id_openid ON "mp_user" ("account_id", "openid", tenant_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_mp_user_account_id ON "mp_user" ("account_id");

