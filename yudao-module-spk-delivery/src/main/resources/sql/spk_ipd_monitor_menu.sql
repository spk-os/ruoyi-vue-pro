-- SPK-OS Cortext-IPD 项目维度监控菜单（设计文档 §9.3 / 诉求 §4 监控台）
-- 项目维度聚合：流程列表（含产物/证据计数）+ 汇总 + 集成健康。
-- 幂等：ON CONFLICT (id) DO NOTHING。父菜单 6800「SPK 研发」。
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES
  (6880, 'IPD 监控', 'spk-delivery:ipd-cockpit:query', 2, 2, 6800, 'ipd-monitor', 'ep:monitor', 'spk/ipd/monitor/index', 'SpkIpdMonitor', 0, true, true, true, '1', now(), '1', now(), 0)
ON CONFLICT (id) DO NOTHING;
