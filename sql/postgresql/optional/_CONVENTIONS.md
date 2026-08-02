# 从 DO 反推 PostgreSQL DDL — 共享约定（4 模块代理必读）

> 本文件由主会话基于实测锁定，4 个子代理（crm/erp/ai/mp）生成 `sql/postgresql/optional/0X_<module>_tables.sql` 时**必须严格遵守**。

## 背景
yudao 运行在 PostgreSQL（`docker-db_postgres-1` / `127.0.0.1:5433` / db=yudao）。AI/ERP/CRM/MP 76 张表在 postgres 库全部不存在，需从本地 checkout 的 DO 类反推 DDL 补齐。模板参照 `sql/postgresql/spk-delivery.sql`。

## DO → 列映射规则（逐 DO 读取后产出）

### 1. 表名
- `@TableName(value = "xxx")` 的 value 即表名（去掉 autoResultMap）。

### 2. 序列名（主键）
- 取 `@KeySequence("yyy")` 的**字面值 yyy**，**不要**自作主张加 `_seq`。多数是 `<table>_seq`，但有例外（如 `AiWorkflowDO` 的 `@KeySequence("ai_workflow")`，序列名就是 `ai_workflow`）。
- 全部 DO 都带 @KeySequence（PG 下 IdType.INPUT，靠序列 nextval 取号）。

### 3. 列名
- 驼峰转下划线（MyBatis-Plus `map-underscore-to-camel-case`）。DO 字段无 `@TableField(value=...)` 显式列名（只可能有 `@TableField(typeHandler=...)`，此时列名仍按字段名驼峰转下划线）。

### 4. Java 类型 → PG 列类型
| Java 类型 | PG 列类型 |
|---|---|
| `Long` | `bigint` |
| `Integer` | `int4` |
| `Short` | `int2` |
| `Boolean` | `int2 NOT NULL DEFAULT 0`（yudao 用 0/1，非 true/false） |
| `BigDecimal`（金额） | `numeric(24,6)`；比例/系数类用 `numeric(24,6)` |
| `BigDecimal`（其他） | `numeric(20,4)` |
| `LocalDateTime` | `timestamp` |
| `LocalDate` | `date` |
| `String`（短：name/code/no/title/type/status/枚举） | `varchar(N)`，N 按语义给保守上限 |
| `String`（长内容：content/remark/desc/graph/result/prompt/lyric/segments） | `text` |
| JSON 列（`Jackson3TypeHandler` / `@TableField(typeHandler=Jackson3TypeHandler.class)` 或字段是 JSONObject/Map/复杂对象） | `text` |
| `List<Long>`/`List<String>`（`LongListTypeHandler`/`StringListTypeHandler`）逗号拼接 | `varchar(255)` |

### 5. varchar 长度惯例（保守上限，宁可宽）
- openid/unionId/appId: `varchar(64)`
- mobile: `varchar(20)`；email: `varchar(50)`；微信号: `varchar(64)`
- 短编码/code/status/type: `varchar(32)`（若明确短可用 16，status 类见 spk 用 varchar(16)）
- name/title/no: `varchar(128)` 或 `varchar(255)`
- url/path: `varchar(500)`
- creator/updater: `varchar(64)`（审计字段，见骨架）
- 其余拿不准的短字符串: `varchar(255)`；拿不准且可能长的: `text`

### 6. 审计/软删/租户列（每表必加，固定尾部，对齐 spk-delivery.sql 实测）
列序尾部固定为：
```sql
    "creator"     varchar(64) DEFAULT '',
    "create_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"     varchar(64) DEFAULT '',
    "update_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"     int2        NOT NULL DEFAULT 0,
    "tenant_id"   bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
```
> 注意：DO 继承 `BaseDO`（无 tenantId 字段）的表**也要加 tenant_id 列**——`TenantDatabaseInterceptor` 会对所有"已注册、非 TenantBaseDO、无 @TenantIgnore"的实体注入 tenant_id，AI/ERP/CRM/MP 76 表都不在 `ignore-tables` 列表，故 76 表一律加 tenant_id（bigint NOT NULL DEFAULT 0）。这与 `spk-delivery.sql` 注释"全部带 tenant_id 兼容多租户拦截器"一致。

### 7. 注释 COMMENT
- 表注释：取 DO 类的 JavaDoc 首行（`/** AI 工作流 DO */` → 表名中文）。
- 列注释：取字段上方 JavaDoc `/** xxx */` 文本（去掉 `枚举 {@link ...}` 后缀可保留为简短说明）。

## DDL 骨架（每表一节，幂等）
```sql
-- ===================== <table> =====================
-- <表中文注释>
DROP TABLE IF EXISTS <table>;
CREATE TABLE IF NOT EXISTS <table> (
    "id"          bigint      NOT NULL,
    -- 业务列（按 DO 字段顺序，驼峰转下划线）
    "xxx"         varchar(255),
    ...
    -- 审计/软删/租户（固定尾部）
    "creator"     varchar(64) DEFAULT '',
    "create_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"     varchar(64) DEFAULT '',
    "update_time" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"     int2        NOT NULL DEFAULT 0,
    "tenant_id"   bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON TABLE <table> IS '<表中文注释>';
COMMENT ON COLUMN <table>.<col> IS '<列中文注释>';
-- （逐业务列加 COMMENT；审计/软删/租户列不加 COMMENT）

CREATE SEQUENCE IF NOT EXISTS <keysequence字面值>;

-- 必要索引（唯一键带 tenant_id；外键普通索引）
CREATE UNIQUE INDEX IF NOT EXISTS idx_<table>_<col> ON <table> (<col>, tenant_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_<table>_<col2> ON <table> (<col2>);
```

### 索引规则
- DO 上无索引注解，索引按语义加。
- **唯一键**：业务唯一字段加 `CREATE UNIQUE INDEX ... ON <table> (<col>, tenant_id) WHERE deleted = 0;`（带 tenant_id + 软删过滤，多租户隔离）。仅在语义明确的唯一字段加：如 `mp_user.openid(+account_id)`、`mp_account.app_id`、`crm_contract.no`、`ai_chat_role.code`、`erp_product.no`、`erp_supplier.no`、`erp_customer.no`、`erp_warehouse.no`、`erp_product_category.name(+parent_id)` 等。拿不准唯一性就**不加唯一索引**（宁可漏加不加错）。
- **外键普通索引**：明显的外键引用列加普通索引：`ai_chat_message.conversation_id/user_id/model_id`、erp 各 *Item 表的 in/order/product/warehouse id、`crm_*_product.product_id`、`crm_contact_business.contact_id/business_id`、`crm_business_product.business_id/product_id`、`crm_contract_product.contract_id/product_id`、`crm_owner_record.biz_type/biz_id/owner_user_id`、`crm_permission.biz_type/biz_id/user_id`、`crm_followup_record.biz_type/biz_id`、`mp_user.account_id` 等。拿不准就少加。
- 不追齐官方索引全集，功能正确优先。

## 文件头模板（每个 .sql 文件开头）
```sql
-- ============================================================
-- <模块大写> 模块表 DDL（PostgreSQL）— 从本地 DO 反推
-- ============================================================
-- 来源：从 yudao-module-<module> 的 *DO.java 反推生成（非社区 MySQL 脚本转换）
-- 数据库：PostgreSQL（docker-db_postgres-1 / 127.0.0.1:5433 / db=yudao）
-- 安全：仅 DROP IF EXISTS + CREATE IF NOT EXISTS（幂等）；无 INSERT、无外键约束、无 ENGINE
-- 约定：见同目录 ../_CONVENTIONS.md（tenant_id 全表必加、序列取 @KeySequence 字面值、JSON→text）
-- 运行：psql -1 -v ON_ERROR_STOP=1 -f 0X_<module>_tables.sql
-- ============================================================
```

## 输出要求
- 每模块输出一个文件，表顺序按 DO 字母序或按子包聚合。
- 文件内每表一节，用 `-- =====================` 分隔。
- **完成后自检**：统计本文件 CREATE TABLE 数 = 该模块 DO 数（CRM=21, ERP=33, AI=14, MP=8）；每表都有 tenant_id + deleted + 序列 + PRIMARY KEY。
- 文件路径：`/work/SPK-OS/soft/basic/ruoyi/sql/postgresql/optional/0X_<module>_tables.sql`

## 注意
- DO 字段可能继承自基类（`BaseDO`：id/creator/create_time/updater/update_time/deleted；`TenantBaseDO`：额外 tenantId）。这些基类字段已在骨架固定尾部处理，**不要**在业务列区重复输出。仅输出 DO 自身声明的业务字段。
- `id` 字段已在骨架首行，不要在业务列重复。
- 若某 DO 有 `@TableField(exist = false)` 字段，**跳过**（非 DB 列）。
- 若 DO 有 `@TableName(autoResultMap = true)` 仅影响 TypeHandler，不影响列名。
