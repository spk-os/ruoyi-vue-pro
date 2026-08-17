-- 流程配置主干扩展（Phase1 C）：给 spk_ipd_process_profile 增交付目录结构模板/默认根/环境默认/默认 skill 映射
-- 幂等。PG 方言。

ALTER TABLE spk_ipd_process_profile ADD COLUMN IF NOT EXISTS delivery_dir_template text;
ALTER TABLE spk_ipd_process_profile ADD COLUMN IF NOT EXISTS default_project_root_pattern varchar(255);
ALTER TABLE spk_ipd_process_profile ADD COLUMN IF NOT EXISTS env_profile varchar(64);
ALTER TABLE spk_ipd_process_profile ADD COLUMN IF NOT EXISTS default_skill_bindings text;

-- 默认值：对齐设计文档 §7 每阶段 skill 与附录 B 目录结构
UPDATE spk_ipd_process_profile
SET delivery_dir_template = '[{"name":".flow","children":[]},{"name":"asset","children":[{"name":"<stage>","children":[]}]},{"name":"src","children":[]},{"name":"docs","children":[{"name":"<stage>","children":[]}]}]'
WHERE delivery_dir_template IS NULL;

UPDATE spk_ipd_process_profile
SET default_project_root_pattern = '/work/SPK-OS/Delivery/project/{businessKey}'
WHERE default_project_root_pattern IS NULL;

UPDATE spk_ipd_process_profile
SET env_profile = 'native-ai'
WHERE env_profile IS NULL;

UPDATE spk_ipd_process_profile
SET default_skill_bindings = '{"concept":"spk-ipd-concept","plan":"spk-ipd-plan","develop":"spk-ipd-develop","qualify":"spk-ipd-verify","launch":"spk-ipd-launch","lifecycle":"spk-ipd-tr-gate"}'
WHERE default_skill_bindings IS NULL;
