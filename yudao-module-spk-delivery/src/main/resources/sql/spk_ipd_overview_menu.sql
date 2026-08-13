-- SPK-OS Cortext-IPD 总览菜单（设计文档 §9.2 / 诉求 §2 总览统计首入口）
-- 一级导航首项，聚合项目/版本/流程/问题/AI 成本六域 + 待办 + 路线图。
-- 幂等：ON CONFLICT (id) DO NOTHING。父菜单 6800「SPK 研发」。
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES
  (6870, 'IPD 总览', 'spk-delivery:ipd-project:query', 2, 0, 6800, 'ipd-overview', 'ep:data-board', 'spk/ipd/overview/index', 'SpkIpdOverview', 0, true, true, true, '1', now(), '1', now(), 0)
ON CONFLICT (id) DO NOTHING;
