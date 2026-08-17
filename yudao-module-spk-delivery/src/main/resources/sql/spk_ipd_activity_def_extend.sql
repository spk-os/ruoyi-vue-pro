-- activity_def 扩展（Phase1 D）：节点环境要求（skill 绑定复用既有 skills 列，不新增）
-- 幂等。PG 方言。
ALTER TABLE spk_ipd_activity_def ADD COLUMN IF NOT EXISTS env_requirements text;
