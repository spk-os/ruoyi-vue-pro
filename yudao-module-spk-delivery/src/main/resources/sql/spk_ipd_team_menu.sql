-- SPK-OS Cortext-IPD 团队与智能体统一菜单（设计文档 §5.1 / 诉求团队态势）
-- 统一页：人员/Agent/编队/负载与产出；旧 agent/agent-squad 页降为二级组件复用。
-- 幂等：ON CONFLICT (id) DO NOTHING。父菜单 6800「SPK 研发」。
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES
  (6890, '团队与智能体', 'spk-delivery:ipd-project:query', 2, 3, 6800, 'ipd-team', 'ep:user-filled', 'spk/ipd/team/index', 'SpkIpdTeam', 0, true, true, true, '1', now(), '1', now(), 0)
ON CONFLICT (id) DO NOTHING;
