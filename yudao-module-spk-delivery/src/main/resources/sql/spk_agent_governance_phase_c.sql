-- ============================================================
-- SPK-OS Phase C：智能体运行负载 + 治理字段 DDL
-- 对照设计基线 §7.1 第 3 视图 / §9.5 治理表面
-- 幂等：列存在则跳过（DO $$ ... IF NOT EXISTS）
-- ============================================================

-- 1) spk_agent_def 补治理字段
ALTER TABLE spk_agent_def ADD COLUMN IF NOT EXISTS version INTEGER DEFAULT 1;
ALTER TABLE spk_agent_def ADD COLUMN IF NOT EXISTS owner VARCHAR(64);
ALTER TABLE spk_agent_def ADD COLUMN IF NOT EXISTS security_level VARCHAR(16);
ALTER TABLE spk_agent_def ADD COLUMN IF NOT EXISTS lifecycle VARCHAR(16) DEFAULT 'register';
ALTER TABLE spk_agent_def ADD COLUMN IF NOT EXISTS lock_version INTEGER DEFAULT 0;
COMMENT ON COLUMN spk_agent_def.version IS '模板版本号（治理发布/回滚引用）';
COMMENT ON COLUMN spk_agent_def.owner IS '负责人用户 id';
COMMENT ON COLUMN spk_agent_def.security_level IS '安全级别 LOW/MEDIUM/HIGH/CRITICAL';
COMMENT ON COLUMN spk_agent_def.lifecycle IS '生命周期 register/trial/ready/retire（与运行态 status 正交）';
COMMENT ON COLUMN spk_agent_def.lock_version IS '乐观锁版本';

-- 2) spk_agent_squad 补编排治理字段
ALTER TABLE spk_agent_squad ADD COLUMN IF NOT EXISTS orchestration_mode VARCHAR(16) DEFAULT 'serial';
ALTER TABLE spk_agent_squad ADD COLUMN IF NOT EXISTS concurrency INTEGER DEFAULT 1;
ALTER TABLE spk_agent_squad ADD COLUMN IF NOT EXISTS timeout INTEGER DEFAULT 0;
ALTER TABLE spk_agent_squad ADD COLUMN IF NOT EXISTS degradation VARCHAR(16) DEFAULT 'fallback';
ALTER TABLE spk_agent_squad ADD COLUMN IF NOT EXISTS version INTEGER DEFAULT 1;
ALTER TABLE spk_agent_squad ADD COLUMN IF NOT EXISTS publish_state VARCHAR(16) DEFAULT 'draft';
ALTER TABLE spk_agent_squad ADD COLUMN IF NOT EXISTS lock_version INTEGER DEFAULT 0;
COMMENT ON COLUMN spk_agent_squad.orchestration_mode IS '编排模式 serial/parallel/concurrent';
COMMENT ON COLUMN spk_agent_squad.concurrency IS '并发上限';
COMMENT ON COLUMN spk_agent_squad.timeout IS '单成员超时秒';
COMMENT ON COLUMN spk_agent_squad.degradation IS '降级策略 skip/fallback/block';
COMMENT ON COLUMN spk_agent_squad.version IS '编队模板版本号';
COMMENT ON COLUMN spk_agent_squad.publish_state IS '发布状态 draft/published/superseded';
COMMENT ON COLUMN spk_agent_squad.lock_version IS '乐观锁版本';

-- 3) spk_agent_task 补运行负载聚合字段
ALTER TABLE spk_agent_task ADD COLUMN IF NOT EXISTS agent_def_id BIGINT;
ALTER TABLE spk_agent_task ADD COLUMN IF NOT EXISTS squad_id BIGINT;
ALTER TABLE spk_agent_task ADD COLUMN IF NOT EXISTS started_at TIMESTAMP;
ALTER TABLE spk_agent_task ADD COLUMN IF NOT EXISTS finished_at TIMESTAMP;
ALTER TABLE spk_agent_task ADD COLUMN IF NOT EXISTS cost_tokens INTEGER DEFAULT 0;
COMMENT ON COLUMN spk_agent_task.agent_def_id IS '智能体定义 ID（聚合维度，dispatch 回填）';
COMMENT ON COLUMN spk_agent_task.squad_id IS '小队 ID（聚合维度）';
COMMENT ON COLUMN spk_agent_task.started_at IS '实际开始执行时间';
COMMENT ON COLUMN spk_agent_task.finished_at IS '实际结束时间';
COMMENT ON COLUMN spk_agent_task.cost_tokens IS '本次运行消耗 token 数';

-- 聚合查询索引（按 agent_def_id + status）
CREATE INDEX IF NOT EXISTS idx_spk_agent_task_def_status ON spk_agent_task (agent_def_id, status) WHERE deleted = 0;
