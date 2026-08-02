-- ============================================================
-- AI 模块表 DDL（PostgreSQL）— 从本地 DO 反推
-- ============================================================
-- 来源：从 yudao-module-ai 的 *DO.java 反推生成（非社区 MySQL 脚本转换）
-- 数据库：PostgreSQL（docker-db_postgres-1 / 127.0.0.1:5433 / db=yudao）
-- 安全：仅 DROP IF EXISTS + CREATE IF NOT EXISTS（幂等）；无 INSERT、无外键约束、无 ENGINE
-- 约定：见同目录 ../_CONVENTIONS.md（tenant_id 全表必加、序列取 @KeySequence 字面值、JSON→text）
-- 运行：psql -1 -v ON_ERROR_STOP=1 -f 03_ai_tables.sql
-- ============================================================

-- ===================== ai_api_key =====================
-- AI API 秘钥 DO
DROP TABLE IF EXISTS "ai_api_key";
CREATE TABLE IF NOT EXISTS "ai_api_key" (
    "id"          bigint      NOT NULL,
    "name"        varchar(128),
    "api_key"     varchar(255),
    "platform"    varchar(32),
    "url"         varchar(500),
    "status"      int4,
    "creator"     varchar(64) DEFAULT '',
    "create_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"     varchar(64) DEFAULT '',
    "update_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"     int2        NOT NULL DEFAULT 0,
    "tenant_id"   bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "ai_api_key" IS 'AI API 秘钥 DO';
COMMENT ON COLUMN "ai_api_key"."name" IS '名称';
COMMENT ON COLUMN "ai_api_key"."api_key" IS '密钥';
COMMENT ON COLUMN "ai_api_key"."platform" IS '平台';
COMMENT ON COLUMN "ai_api_key"."url" IS 'API 地址';
COMMENT ON COLUMN "ai_api_key"."status" IS '状态';
CREATE SEQUENCE IF NOT EXISTS ai_api_key_seq;

-- ===================== ai_chat_conversation =====================
-- AI Chat 对话 DO
DROP TABLE IF EXISTS "ai_chat_conversation";
CREATE TABLE IF NOT EXISTS "ai_chat_conversation" (
    "id"              bigint      NOT NULL,
    "user_id"         bigint,
    "title"           varchar(255),
    "pinned"          int2        NOT NULL DEFAULT 0,
    "pinned_time"     timestamp,
    "role_id"         bigint,
    "model_id"        bigint,
    "model"           varchar(255),
    "system_message"  text,
    "temperature"     numeric(20,4),
    "max_tokens"      int4,
    "max_contexts"    int4,
    "creator"     varchar(64) DEFAULT '',
    "create_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"     varchar(64) DEFAULT '',
    "update_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"     int2        NOT NULL DEFAULT 0,
    "tenant_id"   bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "ai_chat_conversation" IS 'AI Chat 对话 DO';
COMMENT ON COLUMN "ai_chat_conversation"."user_id" IS '用户编号';
COMMENT ON COLUMN "ai_chat_conversation"."title" IS '对话标题';
COMMENT ON COLUMN "ai_chat_conversation"."pinned" IS '是否置顶';
COMMENT ON COLUMN "ai_chat_conversation"."pinned_time" IS '置顶时间';
COMMENT ON COLUMN "ai_chat_conversation"."role_id" IS '角色编号';
COMMENT ON COLUMN "ai_chat_conversation"."model_id" IS '模型编号';
COMMENT ON COLUMN "ai_chat_conversation"."model" IS '模型标志';
COMMENT ON COLUMN "ai_chat_conversation"."system_message" IS '角色设定';
COMMENT ON COLUMN "ai_chat_conversation"."temperature" IS '温度参数';
COMMENT ON COLUMN "ai_chat_conversation"."max_tokens" IS '单条回复的最大 Token 数量';
COMMENT ON COLUMN "ai_chat_conversation"."max_contexts" IS '上下文的最大 Message 数量';
CREATE SEQUENCE IF NOT EXISTS ai_chat_conversation_seq;
CREATE INDEX IF NOT EXISTS idx_ai_chat_conversation_user_id ON "ai_chat_conversation" ("user_id");
CREATE INDEX IF NOT EXISTS idx_ai_chat_conversation_role_id ON "ai_chat_conversation" ("role_id");
CREATE INDEX IF NOT EXISTS idx_ai_chat_conversation_model_id ON "ai_chat_conversation" ("model_id");

-- ===================== ai_chat_message =====================
-- AI Chat 消息 DO
DROP TABLE IF EXISTS "ai_chat_message";
CREATE TABLE IF NOT EXISTS "ai_chat_message" (
    "id"                bigint      NOT NULL,
    "conversation_id"   bigint,
    "reply_id"          bigint,
    "type"              varchar(32),
    "user_id"           bigint,
    "role_id"           bigint,
    "model"             varchar(255),
    "model_id"          bigint,
    "content"           text,
    "reasoning_content" text,
    "use_context"       int2        NOT NULL DEFAULT 0,
    "segment_ids"       varchar(255),
    "web_search_pages"  text,
    "attachment_urls"   varchar(255),
    "creator"     varchar(64) DEFAULT '',
    "create_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"     varchar(64) DEFAULT '',
    "update_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"     int2        NOT NULL DEFAULT 0,
    "tenant_id"   bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "ai_chat_message" IS 'AI Chat 消息 DO';
COMMENT ON COLUMN "ai_chat_message"."conversation_id" IS '对话编号';
COMMENT ON COLUMN "ai_chat_message"."reply_id" IS '回复消息编号';
COMMENT ON COLUMN "ai_chat_message"."type" IS '消息类型';
COMMENT ON COLUMN "ai_chat_message"."user_id" IS '用户编号';
COMMENT ON COLUMN "ai_chat_message"."role_id" IS '角色编号';
COMMENT ON COLUMN "ai_chat_message"."model" IS '模型标志';
COMMENT ON COLUMN "ai_chat_message"."model_id" IS '模型编号';
COMMENT ON COLUMN "ai_chat_message"."content" IS '聊天内容';
COMMENT ON COLUMN "ai_chat_message"."reasoning_content" IS '推理内容';
COMMENT ON COLUMN "ai_chat_message"."use_context" IS '是否携带上下文';
COMMENT ON COLUMN "ai_chat_message"."segment_ids" IS '知识库段落编号数组';
COMMENT ON COLUMN "ai_chat_message"."web_search_pages" IS '联网搜索的网页内容数组';
COMMENT ON COLUMN "ai_chat_message"."attachment_urls" IS '附件 URL 数组';
CREATE SEQUENCE IF NOT EXISTS ai_chat_message_seq;
CREATE INDEX IF NOT EXISTS idx_ai_chat_message_conversation_id ON "ai_chat_message" ("conversation_id");
CREATE INDEX IF NOT EXISTS idx_ai_chat_message_user_id ON "ai_chat_message" ("user_id");
CREATE INDEX IF NOT EXISTS idx_ai_chat_message_model_id ON "ai_chat_message" ("model_id");

-- ===================== ai_chat_role =====================
-- AI 聊天角色 DO
DROP TABLE IF EXISTS "ai_chat_role";
CREATE TABLE IF NOT EXISTS "ai_chat_role" (
    "id"               bigint      NOT NULL,
    "name"             varchar(128),
    "avatar"           varchar(500),
    "category"         varchar(32),
    "description"      text,
    "system_message"   text,
    "user_id"          bigint,
    "model_id"         bigint,
    "knowledge_ids"    varchar(255),
    "tool_ids"         varchar(255),
    "mcp_client_names" varchar(255),
    "public_status"    int2        NOT NULL DEFAULT 0,
    "sort"             int4,
    "status"           int4,
    "creator"     varchar(64) DEFAULT '',
    "create_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"     varchar(64) DEFAULT '',
    "update_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"     int2        NOT NULL DEFAULT 0,
    "tenant_id"   bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "ai_chat_role" IS 'AI 聊天角色 DO';
COMMENT ON COLUMN "ai_chat_role"."name" IS '角色名称';
COMMENT ON COLUMN "ai_chat_role"."avatar" IS '角色头像';
COMMENT ON COLUMN "ai_chat_role"."category" IS '角色分类';
COMMENT ON COLUMN "ai_chat_role"."description" IS '角色描述';
COMMENT ON COLUMN "ai_chat_role"."system_message" IS '角色设定';
COMMENT ON COLUMN "ai_chat_role"."user_id" IS '用户编号';
COMMENT ON COLUMN "ai_chat_role"."model_id" IS '模型编号';
COMMENT ON COLUMN "ai_chat_role"."knowledge_ids" IS '引用的知识库编号列表';
COMMENT ON COLUMN "ai_chat_role"."tool_ids" IS '引用的工具编号列表';
COMMENT ON COLUMN "ai_chat_role"."mcp_client_names" IS '引用的 MCP Client 名字列表';
COMMENT ON COLUMN "ai_chat_role"."public_status" IS '是否公开';
COMMENT ON COLUMN "ai_chat_role"."sort" IS '排序值';
COMMENT ON COLUMN "ai_chat_role"."status" IS '状态';
CREATE SEQUENCE IF NOT EXISTS ai_chat_role_seq;
CREATE INDEX IF NOT EXISTS idx_ai_chat_role_user_id ON "ai_chat_role" ("user_id");
CREATE INDEX IF NOT EXISTS idx_ai_chat_role_model_id ON "ai_chat_role" ("model_id");

-- ===================== ai_image =====================
-- AI 绘画 DO
DROP TABLE IF EXISTS "ai_image";
CREATE TABLE IF NOT EXISTS "ai_image" (
    "id"            bigint      NOT NULL,
    "user_id"       bigint,
    "prompt"        text,
    "platform"      varchar(32),
    "model_id"      bigint,
    "model"         varchar(255),
    "width"         int4,
    "height"        int4,
    "status"        int4,
    "finish_time"   timestamp,
    "error_message" text,
    "pic_url"       varchar(500),
    "public_status" int2        NOT NULL DEFAULT 0,
    "options"       text,
    "buttons"       text,
    "task_id"       varchar(64),
    "creator"     varchar(64) DEFAULT '',
    "create_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"     varchar(64) DEFAULT '',
    "update_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"     int2        NOT NULL DEFAULT 0,
    "tenant_id"   bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "ai_image" IS 'AI 绘画 DO';
COMMENT ON COLUMN "ai_image"."user_id" IS '用户编号';
COMMENT ON COLUMN "ai_image"."prompt" IS '提示词';
COMMENT ON COLUMN "ai_image"."platform" IS '平台';
COMMENT ON COLUMN "ai_image"."model_id" IS '模型编号';
COMMENT ON COLUMN "ai_image"."model" IS '模型标识';
COMMENT ON COLUMN "ai_image"."width" IS '图片宽度';
COMMENT ON COLUMN "ai_image"."height" IS '图片高度';
COMMENT ON COLUMN "ai_image"."status" IS '生成状态';
COMMENT ON COLUMN "ai_image"."finish_time" IS '完成时间';
COMMENT ON COLUMN "ai_image"."error_message" IS '绘画错误信息';
COMMENT ON COLUMN "ai_image"."pic_url" IS '图片地址';
COMMENT ON COLUMN "ai_image"."public_status" IS '是否公开';
COMMENT ON COLUMN "ai_image"."options" IS '绘制参数';
COMMENT ON COLUMN "ai_image"."buttons" IS 'mj buttons 按钮';
COMMENT ON COLUMN "ai_image"."task_id" IS '任务编号';
CREATE SEQUENCE IF NOT EXISTS ai_image_seq;
CREATE INDEX IF NOT EXISTS idx_ai_image_user_id ON "ai_image" ("user_id");
CREATE INDEX IF NOT EXISTS idx_ai_image_model_id ON "ai_image" ("model_id");

-- ===================== ai_knowledge =====================
-- AI 知识库 DO
DROP TABLE IF EXISTS "ai_knowledge";
CREATE TABLE IF NOT EXISTS "ai_knowledge" (
    "id"                   bigint      NOT NULL,
    "name"                 varchar(128),
    "description"          text,
    "embedding_model_id"   bigint,
    "embedding_model"      varchar(255),
    "top_k"                int4,
    "similarity_threshold" numeric(20,4),
    "status"               int4,
    "creator"     varchar(64) DEFAULT '',
    "create_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"     varchar(64) DEFAULT '',
    "update_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"     int2        NOT NULL DEFAULT 0,
    "tenant_id"   bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "ai_knowledge" IS 'AI 知识库 DO';
COMMENT ON COLUMN "ai_knowledge"."name" IS '知识库名称';
COMMENT ON COLUMN "ai_knowledge"."description" IS '知识库描述';
COMMENT ON COLUMN "ai_knowledge"."embedding_model_id" IS '向量模型编号';
COMMENT ON COLUMN "ai_knowledge"."embedding_model" IS '模型标识';
COMMENT ON COLUMN "ai_knowledge"."top_k" IS 'topK';
COMMENT ON COLUMN "ai_knowledge"."similarity_threshold" IS '相似度阈值';
COMMENT ON COLUMN "ai_knowledge"."status" IS '状态';
CREATE SEQUENCE IF NOT EXISTS ai_knowledge_seq;

-- ===================== ai_knowledge_document =====================
-- AI 知识库-文档 DO
DROP TABLE IF EXISTS "ai_knowledge_document";
CREATE TABLE IF NOT EXISTS "ai_knowledge_document" (
    "id"                 bigint      NOT NULL,
    "knowledge_id"       bigint,
    "name"               varchar(128),
    "url"                varchar(500),
    "content"            text,
    "content_length"     int4,
    "tokens"             int4,
    "segment_max_tokens" int4,
    "retrieval_count"    int4,
    "status"              int4,
    "creator"     varchar(64) DEFAULT '',
    "create_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"     varchar(64) DEFAULT '',
    "update_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"     int2        NOT NULL DEFAULT 0,
    "tenant_id"   bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "ai_knowledge_document" IS 'AI 知识库-文档 DO';
COMMENT ON COLUMN "ai_knowledge_document"."knowledge_id" IS '知识库编号';
COMMENT ON COLUMN "ai_knowledge_document"."name" IS '文档名称';
COMMENT ON COLUMN "ai_knowledge_document"."url" IS '文件 URL';
COMMENT ON COLUMN "ai_knowledge_document"."content" IS '内容';
COMMENT ON COLUMN "ai_knowledge_document"."content_length" IS '文档长度';
COMMENT ON COLUMN "ai_knowledge_document"."tokens" IS '文档 token 数量';
COMMENT ON COLUMN "ai_knowledge_document"."segment_max_tokens" IS '分片最大 Token 数';
COMMENT ON COLUMN "ai_knowledge_document"."retrieval_count" IS '召回次数';
COMMENT ON COLUMN "ai_knowledge_document"."status" IS '状态';
CREATE SEQUENCE IF NOT EXISTS ai_knowledge_document_seq;
CREATE INDEX IF NOT EXISTS idx_ai_knowledge_document_knowledge_id ON "ai_knowledge_document" ("knowledge_id");

-- ===================== ai_knowledge_segment =====================
-- AI 知识库-文档分段 DO
DROP TABLE IF EXISTS "ai_knowledge_segment";
CREATE TABLE IF NOT EXISTS "ai_knowledge_segment" (
    "id"              bigint      NOT NULL,
    "knowledge_id"    bigint,
    "document_id"     bigint,
    "content"         text,
    "content_length"  int4,
    "vector_id"       varchar(64),
    "tokens"          int4,
    "retrieval_count" int4,
    "status"          int4,
    "creator"     varchar(64) DEFAULT '',
    "create_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"     varchar(64) DEFAULT '',
    "update_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"     int2        NOT NULL DEFAULT 0,
    "tenant_id"   bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "ai_knowledge_segment" IS 'AI 知识库-文档分段 DO';
COMMENT ON COLUMN "ai_knowledge_segment"."knowledge_id" IS '知识库编号';
COMMENT ON COLUMN "ai_knowledge_segment"."document_id" IS '文档编号';
COMMENT ON COLUMN "ai_knowledge_segment"."content" IS '切片内容';
COMMENT ON COLUMN "ai_knowledge_segment"."content_length" IS '切片内容长度';
COMMENT ON COLUMN "ai_knowledge_segment"."vector_id" IS '向量库的编号';
COMMENT ON COLUMN "ai_knowledge_segment"."tokens" IS 'token 数量';
COMMENT ON COLUMN "ai_knowledge_segment"."retrieval_count" IS '召回次数';
COMMENT ON COLUMN "ai_knowledge_segment"."status" IS '状态';
CREATE SEQUENCE IF NOT EXISTS ai_knowledge_segment_seq;
CREATE INDEX IF NOT EXISTS idx_ai_knowledge_segment_knowledge_id ON "ai_knowledge_segment" ("knowledge_id");
CREATE INDEX IF NOT EXISTS idx_ai_knowledge_segment_document_id ON "ai_knowledge_segment" ("document_id");

-- ===================== ai_mind_map =====================
-- AI 思维导图 DO
DROP TABLE IF EXISTS "ai_mind_map";
CREATE TABLE IF NOT EXISTS "ai_mind_map" (
    "id"               bigint      NOT NULL,
    "user_id"          bigint,
    "platform"         varchar(32),
    "model_id"         bigint,
    "model"            varchar(255),
    "prompt"           text,
    "generated_content" text,
    "error_message"    text,
    "creator"     varchar(64) DEFAULT '',
    "create_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"     varchar(64) DEFAULT '',
    "update_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"     int2        NOT NULL DEFAULT 0,
    "tenant_id"   bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "ai_mind_map" IS 'AI 思维导图 DO';
COMMENT ON COLUMN "ai_mind_map"."user_id" IS '用户编号';
COMMENT ON COLUMN "ai_mind_map"."platform" IS '平台';
COMMENT ON COLUMN "ai_mind_map"."model_id" IS '模型编号';
COMMENT ON COLUMN "ai_mind_map"."model" IS '模型';
COMMENT ON COLUMN "ai_mind_map"."prompt" IS '生成内容提示';
COMMENT ON COLUMN "ai_mind_map"."generated_content" IS '生成的内容';
COMMENT ON COLUMN "ai_mind_map"."error_message" IS '错误信息';
CREATE SEQUENCE IF NOT EXISTS ai_mind_map_seq;
CREATE INDEX IF NOT EXISTS idx_ai_mind_map_user_id ON "ai_mind_map" ("user_id");
CREATE INDEX IF NOT EXISTS idx_ai_mind_map_model_id ON "ai_mind_map" ("model_id");

-- ===================== ai_model =====================
-- AI 模型 DO
DROP TABLE IF EXISTS "ai_model";
CREATE TABLE IF NOT EXISTS "ai_model" (
    "id"           bigint      NOT NULL,
    "key_id"       bigint,
    "name"         varchar(128),
    "model"        varchar(255),
    "platform"     varchar(32),
    "type"         int4,
    "sort"         int4,
    "status"       int4,
    "temperature"  numeric(20,4),
    "max_tokens"   int4,
    "max_contexts" int4,
    "creator"     varchar(64) DEFAULT '',
    "create_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"     varchar(64) DEFAULT '',
    "update_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"     int2        NOT NULL DEFAULT 0,
    "tenant_id"   bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "ai_model" IS 'AI 模型 DO';
COMMENT ON COLUMN "ai_model"."key_id" IS 'API 秘钥编号';
COMMENT ON COLUMN "ai_model"."name" IS '模型名称';
COMMENT ON COLUMN "ai_model"."model" IS '模型标志';
COMMENT ON COLUMN "ai_model"."platform" IS '平台';
COMMENT ON COLUMN "ai_model"."type" IS '类型';
COMMENT ON COLUMN "ai_model"."sort" IS '排序值';
COMMENT ON COLUMN "ai_model"."status" IS '状态';
COMMENT ON COLUMN "ai_model"."temperature" IS '温度参数';
COMMENT ON COLUMN "ai_model"."max_tokens" IS '单条回复的最大 Token 数量';
COMMENT ON COLUMN "ai_model"."max_contexts" IS '上下文的最大 Message 数量';
CREATE SEQUENCE IF NOT EXISTS ai_model_seq;
CREATE INDEX IF NOT EXISTS idx_ai_model_key_id ON "ai_model" ("key_id");

-- ===================== ai_music =====================
-- AI 音乐 DO
DROP TABLE IF EXISTS "ai_music";
CREATE TABLE IF NOT EXISTS "ai_music" (
    "id"            bigint      NOT NULL,
    "user_id"       bigint,
    "title"         varchar(255),
    "lyric"         text,
    "image_url"     varchar(500),
    "audio_url"     varchar(500),
    "video_url"     varchar(500),
    "status"        int4,
    "generate_mode" int4,
    "description"   text,
    "platform"      varchar(32),
    "model"         varchar(255),
    "tags"          text,
    "duration"      numeric(20,4),
    "public_status" int2        NOT NULL DEFAULT 0,
    "task_id"       varchar(64),
    "error_message" text,
    "creator"     varchar(64) DEFAULT '',
    "create_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"     varchar(64) DEFAULT '',
    "update_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"     int2        NOT NULL DEFAULT 0,
    "tenant_id"   bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "ai_music" IS 'AI 音乐 DO';
COMMENT ON COLUMN "ai_music"."user_id" IS '用户编号';
COMMENT ON COLUMN "ai_music"."title" IS '音乐名称';
COMMENT ON COLUMN "ai_music"."lyric" IS '歌词';
COMMENT ON COLUMN "ai_music"."image_url" IS '图片地址';
COMMENT ON COLUMN "ai_music"."audio_url" IS '音频地址';
COMMENT ON COLUMN "ai_music"."video_url" IS '视频地址';
COMMENT ON COLUMN "ai_music"."status" IS '音乐状态';
COMMENT ON COLUMN "ai_music"."generate_mode" IS '生成模式';
COMMENT ON COLUMN "ai_music"."description" IS '描述词';
COMMENT ON COLUMN "ai_music"."platform" IS '平台';
COMMENT ON COLUMN "ai_music"."model" IS '模型';
COMMENT ON COLUMN "ai_music"."tags" IS '音乐风格标签';
COMMENT ON COLUMN "ai_music"."duration" IS '音乐时长';
COMMENT ON COLUMN "ai_music"."public_status" IS '是否公开';
COMMENT ON COLUMN "ai_music"."task_id" IS '任务编号';
COMMENT ON COLUMN "ai_music"."error_message" IS '错误信息';
CREATE SEQUENCE IF NOT EXISTS ai_music_seq;
CREATE INDEX IF NOT EXISTS idx_ai_music_user_id ON "ai_music" ("user_id");

-- ===================== ai_tool =====================
-- AI 工具 DO
DROP TABLE IF EXISTS "ai_tool";
CREATE TABLE IF NOT EXISTS "ai_tool" (
    "id"          bigint      NOT NULL,
    "name"        varchar(128),
    "description" text,
    "status"      int4,
    "creator"     varchar(64) DEFAULT '',
    "create_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"     varchar(64) DEFAULT '',
    "update_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"     int2        NOT NULL DEFAULT 0,
    "tenant_id"   bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "ai_tool" IS 'AI 工具 DO';
COMMENT ON COLUMN "ai_tool"."name" IS '工具名称';
COMMENT ON COLUMN "ai_tool"."description" IS '工具描述';
COMMENT ON COLUMN "ai_tool"."status" IS '状态';
CREATE SEQUENCE IF NOT EXISTS ai_tool_seq;

-- ===================== ai_workflow =====================
-- AI 工作流 DO
-- 注意：@KeySequence 字面值为 "ai_workflow"（无 _seq 后缀），序列名取字面值
DROP TABLE IF EXISTS "ai_workflow";
CREATE TABLE IF NOT EXISTS "ai_workflow" (
    "id"     bigint      NOT NULL,
    "name"   varchar(128),
    "code"   varchar(32),
    "graph"  text,
    "remark" text,
    "status" int4,
    "creator"     varchar(64) DEFAULT '',
    "create_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"     varchar(64) DEFAULT '',
    "update_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"     int2        NOT NULL DEFAULT 0,
    "tenant_id"   bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "ai_workflow" IS 'AI 工作流 DO';
COMMENT ON COLUMN "ai_workflow"."name" IS '工作流名称';
COMMENT ON COLUMN "ai_workflow"."code" IS '工作流标识';
COMMENT ON COLUMN "ai_workflow"."graph" IS '工作流模型 JSON 数据';
COMMENT ON COLUMN "ai_workflow"."remark" IS '备注';
COMMENT ON COLUMN "ai_workflow"."status" IS '状态';
CREATE SEQUENCE IF NOT EXISTS ai_workflow;

-- ===================== ai_write =====================
-- AI 写作 DO
DROP TABLE IF EXISTS "ai_write";
CREATE TABLE IF NOT EXISTS "ai_write" (
    "id"                bigint      NOT NULL,
    "user_id"           bigint,
    "type"              int4,
    "platform"          varchar(32),
    "model_id"          bigint,
    "model"             varchar(255),
    "prompt"            text,
    "generated_content" text,
    "original_content"  text,
    "length"            int4,
    "format"            int4,
    "tone"              int4,
    "language"          int4,
    "error_message"     text,
    "creator"     varchar(64) DEFAULT '',
    "create_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"     varchar(64) DEFAULT '',
    "update_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"     int2        NOT NULL DEFAULT 0,
    "tenant_id"   bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
COMMENT ON TABLE "ai_write" IS 'AI 写作 DO';
COMMENT ON COLUMN "ai_write"."user_id" IS '用户编号';
COMMENT ON COLUMN "ai_write"."type" IS '写作类型';
COMMENT ON COLUMN "ai_write"."platform" IS '平台';
COMMENT ON COLUMN "ai_write"."model_id" IS '模型编号';
COMMENT ON COLUMN "ai_write"."model" IS '模型';
COMMENT ON COLUMN "ai_write"."prompt" IS '生成内容提示';
COMMENT ON COLUMN "ai_write"."generated_content" IS '生成的内容';
COMMENT ON COLUMN "ai_write"."original_content" IS '原文';
COMMENT ON COLUMN "ai_write"."length" IS '长度提示词';
COMMENT ON COLUMN "ai_write"."format" IS '格式提示词';
COMMENT ON COLUMN "ai_write"."tone" IS '语气提示词';
COMMENT ON COLUMN "ai_write"."language" IS '语言提示词';
COMMENT ON COLUMN "ai_write"."error_message" IS '错误信息';
CREATE SEQUENCE IF NOT EXISTS ai_write_seq;
CREATE INDEX IF NOT EXISTS idx_ai_write_user_id ON "ai_write" ("user_id");
CREATE INDEX IF NOT EXISTS idx_ai_write_model_id ON "ai_write" ("model_id");
