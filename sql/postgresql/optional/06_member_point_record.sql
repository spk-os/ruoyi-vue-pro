-- ============================================================
-- MEMBER 模块补丁表 DDL（PostgreSQL）— 从本地 DO 反推
-- ============================================================
-- 来源：从 yudao-module-member 的 MemberPointRecordDO 反推生成（非社区 MySQL 脚本转换）
-- 数据库：PostgreSQL（docker-db_postgres-1 / 127.0.0.1:5433 / db=yudao）
-- 安全：仅 DROP IF EXISTS + CREATE IF NOT EXISTS（幂等）；无 INSERT、无外键约束、无 ENGINE
-- 约定：见同目录 _CONVENTIONS.md（tenant_id 全表必加、序列取 @KeySequence 字面值）
-- 背景：member_point_record 表在 postgres 库缺失，MemberPointRecordDO 查询报 relation does not exist
-- ============================================================

-- ===================== member_point_record =====================
-- 用户积分记录
DROP TABLE IF EXISTS member_point_record;
CREATE TABLE IF NOT EXISTS member_point_record (
    "id"            int8      NOT NULL,
    "user_id"       int8      NULL DEFAULT NULL,
    "biz_id"        varchar(64)  NULL DEFAULT NULL,
    "biz_type"      int4      NULL DEFAULT NULL,
    "title"         varchar(255) NULL DEFAULT NULL,
    "description"   varchar(500) NULL DEFAULT NULL,
    "point"         int4      NULL DEFAULT NULL,
    "total_point"   int4      NULL DEFAULT NULL,
    "creator"       varchar(64) DEFAULT '',
    "create_time"   timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"       varchar(64) DEFAULT '',
    "update_time"   timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"       int2      NOT NULL DEFAULT 0,
    "tenant_id"     int8      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE member_point_record IS '用户积分记录';
COMMENT ON COLUMN member_point_record.user_id IS '用户编号（对应 MemberUserDO 的 id）';
COMMENT ON COLUMN member_point_record.biz_id IS '业务编码';
COMMENT ON COLUMN member_point_record.biz_type IS '业务类型（枚举 MemberPointBizTypeEnum）';
COMMENT ON COLUMN member_point_record.title IS '积分标题';
COMMENT ON COLUMN member_point_record.description IS '积分描述';
COMMENT ON COLUMN member_point_record.point IS '变动积分（正数获得，负数消耗）';
COMMENT ON COLUMN member_point_record.total_point IS '变动后的积分';

CREATE SEQUENCE IF NOT EXISTS member_point_record_seq;

-- 外键引用列：user_id
CREATE INDEX IF NOT EXISTS idx_member_point_record_user_id ON member_point_record (user_id);
