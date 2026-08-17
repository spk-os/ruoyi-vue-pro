-- SPK-OS Cortext-IPD 流程治理菜单（§7.2 第 4 顶层表面）
-- 父菜单 6800「SPK 研发」。幂等。
-- D6 重建：6980 由单页菜单改为目录（type=1，path=ipd-governance 相对，坑 §6.1），下挂三独立路由页：
--   6984 模板库  → spk/ipd/governance/templates/index → /spk/ipd-governance/templates
--   6985 治理规则 → spk/ipd/governance/profiles/index  → /spk/ipd-governance/profiles
--   6986 版本管理 → spk/ipd/governance/versions/index  → /spk/ipd-governance/versions
-- 权限前缀 spk-delivery:ipd-governance:*。6981-6983 为按钮权限（type=3）保留。

-- 6980 目录：已存在则升级为目录形态（DO UPDATE 修 type/path/component）；新建则直接落目录。
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES
  (6980, '流程治理', 'spk-delivery:ipd-governance:query', 1, 6, 6800, 'ipd-governance', 'ep:set-up', NULL, NULL, 0, true, true, true, '1', now(), '1', now(), 0)
ON CONFLICT (id) DO UPDATE SET
  type=EXCLUDED.type, path=EXCLUDED.path, component=EXCLUDED.component, component_name=EXCLUDED.component_name, icon=EXCLUDED.icon, always_show=EXCLUDED.always_show, update_time=now(), updater='1';

-- 6981-6983 按钮权限 + 6984-6986 三独立路由菜单（新建幂等）
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES
  (6981, '模板创建', 'spk-delivery:ipd-governance:create', 3, 1, 6980, '', '', '', NULL, 0, true, true, true, '1', now(), '1', now(), 0),
  (6982, '模板更新/发布/回滚', 'spk-delivery:ipd-governance:update', 3, 2, 6980, '', '', '', NULL, 0, true, true, true, '1', now(), '1', now(), 0),
  (6983, '模板/规则删除', 'spk-delivery:ipd-governance:delete', 3, 3, 6980, '', '', '', NULL, 0, true, true, true, '1', now(), '1', now(), 0),
  (6984, '模板库', 'spk-delivery:ipd-governance:query', 2, 1, 6980, 'templates', 'ep:document-copy', 'spk/ipd/governance/templates/index', 'SpkIpdGovernanceTemplates', 0, true, true, true, '1', now(), '1', now(), 0),
  (6985, '治理规则', 'spk-delivery:ipd-governance:query', 2, 2, 6980, 'profiles', 'ep:set-up', 'spk/ipd/governance/profiles/index', 'SpkIpdGovernanceProfiles', 0, true, true, true, '1', now(), '1', now(), 0),
  (6986, '版本管理', 'spk-delivery:ipd-governance:query', 2, 3, 6980, 'versions', 'ep:document', 'spk/ipd/governance/versions/index', 'SpkIpdGovernanceVersions', 0, true, true, true, '1', now(), '1', now(), 0)
ON CONFLICT (id) DO NOTHING;

-- role_menu 授权超管(role_id=1)
INSERT INTO system_role_menu (id, role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
VALUES
  (6980, 1, 6980, '1', now(), '1', now(), 0, 1),
  (6981, 1, 6981, '1', now(), '1', now(), 0, 1),
  (6982, 1, 6982, '1', now(), '1', now(), 0, 1),
  (6983, 1, 6983, '1', now(), '1', now(), 0, 1),
  (6984, 1, 6984, '1', now(), '1', now(), 0, 1),
  (6985, 1, 6985, '1', now(), '1', now(), 0, 1),
  (6986, 1, 6986, '1', now(), '1', now(), 0, 1)
ON CONFLICT (id) DO NOTHING;
