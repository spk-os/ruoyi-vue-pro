-- 项目扩展（Phase1 E）：交付根目录（项目启动选根目录）
-- 幂等。PG 方言。
ALTER TABLE spk_ipd_project ADD COLUMN IF NOT EXISTS delivery_root varchar(500);
