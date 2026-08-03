-- =====================================================================
-- SPK-OS Cortext-IPD 模型能力画像初始化（P1）
-- 12 个 capability → provider/model 桩映射（全部 zhipu / glm-5.2，P1 统一）。
-- ModelCapabilityRegistry.freezeSnapshot 会把这批 active 画像序列化进
-- spk_model_registry_snapshot，作为每次 Activity 执行前的不可变快照。
-- 幂等：先按 capability_id 清除本批，再用 nextval 重建
-- =====================================================================

DELETE FROM "spk_model_capability_profile" WHERE "capability_id" IN (
  'req-analysis','concept-design','proj-planning','architecture','detail-design',
  'coding','integration-test','beta-test','go-to-market','ops-feedback',
  'pm-orchestration','sys-architecture','review'
);

INSERT INTO "spk_model_capability_profile"
("id","capability_id","provider","model","context_length","data_policy","region","priority","status","valid_at","tenant_id")
VALUES
(nextval('spk_model_capability_profile_seq'),'req-analysis','zhipu','glm-5.2',128000,'domestic','cn-east',100,'active',NOW(),0),
(nextval('spk_model_capability_profile_seq'),'concept-design','zhipu','glm-5.2',128000,'domestic','cn-east',100,'active',NOW(),0),
(nextval('spk_model_capability_profile_seq'),'proj-planning','zhipu','glm-5.2',128000,'domestic','cn-east',100,'active',NOW(),0),
(nextval('spk_model_capability_profile_seq'),'architecture','zhipu','glm-5.2',128000,'domestic','cn-east',100,'active',NOW(),0),
(nextval('spk_model_capability_profile_seq'),'detail-design','zhipu','glm-5.2',128000,'domestic','cn-east',100,'active',NOW(),0),
(nextval('spk_model_capability_profile_seq'),'coding','zhipu','glm-5.2',128000,'domestic','cn-east',90,'active',NOW(),0),
(nextval('spk_model_capability_profile_seq'),'integration-test','zhipu','glm-5.2',128000,'domestic','cn-east',100,'active',NOW(),0),
(nextval('spk_model_capability_profile_seq'),'beta-test','zhipu','glm-5.2',128000,'domestic','cn-east',100,'active',NOW(),0),
(nextval('spk_model_capability_profile_seq'),'go-to-market','zhipu','glm-5.2',128000,'domestic','cn-east',100,'active',NOW(),0),
(nextval('spk_model_capability_profile_seq'),'ops-feedback','zhipu','glm-5.2',128000,'domestic','cn-east',100,'active',NOW(),0),
(nextval('spk_model_capability_profile_seq'),'pm-orchestration','zhipu','glm-5.2',128000,'domestic','cn-east',100,'active',NOW(),0),
(nextval('spk_model_capability_profile_seq'),'sys-architecture','zhipu','glm-5.2',128000,'domestic','cn-east',100,'active',NOW(),0),
(nextval('spk_model_capability_profile_seq'),'review','zhipu','glm-5.2',128000,'domestic','cn-east',110,'active',NOW(),0);
