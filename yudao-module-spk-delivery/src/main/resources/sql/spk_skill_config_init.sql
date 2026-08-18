-- spk_skill_config：Skill 环境配置单行表（D1）
-- 幂等：表不存在则建，行不存在则 seed（id=1 恒定，ON CONFLICT DO NOTHING）。
CREATE TABLE IF NOT EXISTS "spk_skill_config" (
  "id"          BIGINT       NOT NULL,
  "skills_root" VARCHAR(512),
  "default_env" VARCHAR(64),
  "creator"     VARCHAR(64) DEFAULT '',
  "create_time" TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater"     VARCHAR(64) DEFAULT '',
  "update_time" TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted"     SMALLINT     NOT NULL DEFAULT 0,
  "tenant_id"   BIGINT       NOT NULL DEFAULT 0,
  CONSTRAINT "spk_skill_config_pkey" PRIMARY KEY ("id")
);
COMMENT ON TABLE  "spk_skill_config" IS 'Skill 环境配置单行表（id 恒为 1，skillsRoot/defaultEnv 可前端改）';
COMMENT ON COLUMN "spk_skill_config"."skills_root" IS 'skill 根目录（env/skillName/SKILL.md 三段式解析的根）';
COMMENT ON COLUMN "spk_skill_config"."default_env" IS '默认 skill 环境 default/test/commercial-release/prototype-release';

INSERT INTO "spk_skill_config" ("id","skills_root","default_env","tenant_id")
VALUES (1, '/work/SPK-OS/soft/basic/ruoyi/resources/skills', 'default', 1)
ON CONFLICT (id) DO NOTHING;
