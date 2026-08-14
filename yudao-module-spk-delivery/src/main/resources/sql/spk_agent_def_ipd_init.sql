-- =====================================================================
-- SPK-OS Cortext-IPD 智能体定义初始化（P1）
-- 12 Lead Agent + 3 Independent Verifier，全部 runtime_type=native，roleId=1
-- 复用 yudao AiChatRole(id=1) 保证可执行；agent_kind/capability_tags/
-- verifier_type/isolation_level 由 spk-delivery.sql 的 ALTER 提供
-- 幂等：先按 code 清除本批，再用 nextval 重建
-- =====================================================================

-- ----------------------------
-- per-agent 双模式 + 角色继承接线（mode/parent_def_id/omnigent_agent_id）
-- mode: local(默认,走 NativeAiAdapter) / omnigent(走 OmnigentAdapter 真实沙箱)
-- parent_def_id: 继承父智能体（运行时合并解析）
-- omnigent_agent_id: mode=omnigent 时 Omnigent 侧 agent-id 映射，空回退全局配置
-- ----------------------------
ALTER TABLE "spk_agent_def" ADD COLUMN IF NOT EXISTS "mode" VARCHAR(16) DEFAULT 'local';
ALTER TABLE "spk_agent_def" ADD COLUMN IF NOT EXISTS "parent_def_id" BIGINT;
ALTER TABLE "spk_agent_def" ADD COLUMN IF NOT EXISTS "omnigent_agent_id" VARCHAR(64);
COMMENT ON COLUMN "spk_agent_def"."mode" IS '执行模式 local/omnigent';
COMMENT ON COLUMN "spk_agent_def"."parent_def_id" IS '继承父智能体 id';
COMMENT ON COLUMN "spk_agent_def"."omnigent_agent_id" IS 'Omnigent 侧 agent-id 映射';


DELETE FROM "spk_agent_def" WHERE "code" IN (
  'lead-req-insight','lead-concept-options','lead-proj-plan','lead-arch-design',
  'lead-detail-design','lead-coding','lead-integration-verify','lead-beta-verify',
  'lead-launch','lead-ops-feedback','lead-pm','lead-sys-arch',
  'verifier-tr','verifier-re','verifier-sec'
);

-- ----------------------------
-- 12 Lead Agent
-- ----------------------------
INSERT INTO "spk_agent_def" ("id","name","code","role","status","model","role_id","runtime_type","source","agent_kind","capability_tags","verifier_type","isolation_level","tenant_id") VALUES
(nextval('spk_agent_def_seq'),'需求洞察 Lead','lead-req-insight','需求洞察','idle','glm-5.2',1,'native','manual','lead','["req-analysis"]',NULL,'process',0),
(nextval('spk_agent_def_seq'),'概念选项 Lead','lead-concept-options','概念选项','idle','glm-5.2',1,'native','manual','lead','["concept-design"]',NULL,'process',0),
(nextval('spk_agent_def_seq'),'项目计划 Lead','lead-proj-plan','项目计划','idle','glm-5.2',1,'native','manual','lead','["proj-planning"]',NULL,'process',0),
(nextval('spk_agent_def_seq'),'架构设计 Lead','lead-arch-design','架构设计','idle','glm-5.2',1,'native','manual','lead','["architecture"]',NULL,'process',0),
(nextval('spk_agent_def_seq'),'详细设计 Lead','lead-detail-design','详细设计','idle','glm-5.2',1,'native','manual','lead','["detail-design"]',NULL,'process',0),
(nextval('spk_agent_def_seq'),'编码实现 Lead','lead-coding','编码实现','idle','glm-5.2',1,'native','manual','lead','["coding"]',NULL,'process',0),
(nextval('spk_agent_def_seq'),'集成验证 Lead','lead-integration-verify','集成验证','idle','glm-5.2',1,'native','manual','lead','["integration-test"]',NULL,'process',0),
(nextval('spk_agent_def_seq'),'Beta 验证 Lead','lead-beta-verify','Beta 验证','idle','glm-5.2',1,'native','manual','lead','["beta-test"]',NULL,'process',0),
(nextval('spk_agent_def_seq'),'上市发布 Lead','lead-launch','上市发布','idle','glm-5.2',1,'native','manual','lead','["go-to-market"]',NULL,'process',0),
(nextval('spk_agent_def_seq'),'运营反馈 Lead','lead-ops-feedback','运营反馈','idle','glm-5.2',1,'native','manual','lead','["ops-feedback"]',NULL,'process',0),
(nextval('spk_agent_def_seq'),'项目经理 Lead','lead-pm','项目经理','idle','glm-5.2',1,'native','manual','lead','["pm-orchestration"]',NULL,'process',0),
(nextval('spk_agent_def_seq'),'系统架构师 Lead','lead-sys-arch','系统架构师','idle','glm-5.2',1,'native','manual','lead','["sys-architecture"]',NULL,'process',0);

-- ----------------------------
-- 3 Independent Verifier（逻辑隔离：独立 roleId/会话 + 独立 evidence 路径）
-- verifier_type：TR=技术评审 / RE=需求评审 / SEC=安全评审
-- ----------------------------
INSERT INTO "spk_agent_def" ("id","name","code","role","status","model","role_id","runtime_type","source","agent_kind","capability_tags","verifier_type","isolation_level","tenant_id") VALUES
(nextval('spk_agent_def_seq'),'技术评审 Verifier','verifier-tr','技术评审','idle','glm-5.2',1,'native','manual','verifier','["review"]','TR','process',0),
(nextval('spk_agent_def_seq'),'需求评审 Verifier','verifier-re','需求评审','idle','glm-5.2',1,'native','manual','verifier','["review"]','RE','process',0),
(nextval('spk_agent_def_seq'),'安全评审 Verifier','verifier-sec','安全评审','idle','glm-5.2',1,'native','manual','verifier','["review"]','SEC','process',0);

-- ----------------------------
-- per-agent 双模式接线示范：
-- lead-arch-design  → mode=omnigent + omnigent_agent_id=spk-architect（验证 Omnigent 真实沙箱链 + 多 agent 映射）
-- lead-req-insight  → mode=omnigent + omnigent_agent_id=spk-reporter（验证 Omnigent 真实文件回流 + conv 续跑）
-- 其余 lead 默认 mode=local（走 NativeAiAdapter 本地 LLM 链）
-- ----------------------------
UPDATE "spk_agent_def" SET "mode"='omnigent',"omnigent_agent_id"='spk-architect' WHERE "code"='lead-arch-design';
UPDATE "spk_agent_def" SET "mode"='omnigent',"omnigent_agent_id"='spk-reporter'  WHERE "code"='lead-req-insight';

