-- 流程治理菜单 v2（Phase1 A/B）：模型库→流程管理改名 + 新增「流程配置」页
-- 幂等。PG 方言。父目录 6980。

-- A. 6984 模板库→流程管理（component_name 保持不变，避免路由折叠 坑#17/#18）
UPDATE system_menu SET name = '流程管理' WHERE id = 6984 AND deleted = 0;

-- B. 6987「流程配置」聚合页（path 相对不带 /，坑#17；component_name 全局唯一，坑#18）
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES
  (6987, '流程配置', 'spk-delivery:ipd-governance:query', 2, 0, 6980, 'flow-config', 'ep:tools', 'spk/ipd/governance/flow-config/index', 'SpkIpdGovernanceFlowConfig', 0, true, true, true, '1', now(), '1', now(), 0)
ON CONFLICT (id) DO UPDATE SET
  name=EXCLUDED.name, path=EXCLUDED.path, component=EXCLUDED.component, component_name=EXCLUDED.component_name, icon=EXCLUDED.icon, update_time=now(), updater='1';

-- role_menu 授权超管(role_id=1)
INSERT INTO system_role_menu (id, role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
VALUES (6987, 1, 6987, '1', now(), '1', now(), 0, 1)
ON CONFLICT (id) DO NOTHING;
