-- SPK-OS Cortext-IPD 流程治理菜单（§7.2 第 4 顶层表面）
-- 父菜单 6800「SPK 研发」。幂等：ON CONFLICT (id) DO NOTHING。
-- 视图 spk/ipd/governance/index（4 Tab：流程模板/裁剪规则/引擎档案/治理审计）。
-- 权限前缀 spk-delivery:ipd-governance:*。
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES
  (6980, '流程治理', 'spk-delivery:ipd-governance:query', 2, 6, 6800, 'spk-governance', 'ep:set-up', 'spk/ipd/governance/index', 'SpkIpdGovernance', 0, true, true, true, '1', now(), '1', now(), 0),
  (6981, '模板创建', 'spk-delivery:ipd-governance:create', 3, 1, 6980, '', '', '', NULL, 0, true, true, true, '1', now(), '1', now(), 0),
  (6982, '模板更新/发布/回滚', 'spk-delivery:ipd-governance:update', 3, 2, 6980, '', '', '', NULL, 0, true, true, true, '1', now(), '1', now(), 0),
  (6983, '模板/规则删除', 'spk-delivery:ipd-governance:delete', 3, 3, 6980, '', '', '', NULL, 0, true, true, true, '1', now(), '1', now(), 0)
ON CONFLICT (id) DO NOTHING;
