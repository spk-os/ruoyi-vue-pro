-- SPK-OS Cortext-IPD 4级版本模型前端菜单（项目与版本 + 操作按钮）
-- 设计文档 §7.2 / §14.1 S1。父菜单 6800「SPK 研发」。
-- 幂等：ON CONFLICT (id) DO NOTHING。组件 spk/ipd/projects/index；详情页 detail.vue 走 remaining.ts 静态路由。
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES
  (6860, '项目与版本', 'spk-delivery:ipd-project:query', 2, 5, 6800, 'spk-projects', 'ep:files', 'spk/ipd/projects/index', 'SpkIpdProjects', 0, true, true, true, '1', now(), '1', now(), 0),
  (6861, '项目创建', 'spk-delivery:ipd-project:create', 3, 1, 6860, '', '', '', NULL, 0, true, true, true, '1', now(), '1', now(), 0),
  (6862, '项目更新', 'spk-delivery:ipd-project:update', 3, 2, 6860, '', '', '', NULL, 0, true, true, true, '1', now(), '1', now(), 0),
  (6863, '项目归档', 'spk-delivery:ipd-project:archive', 3, 3, 6860, '', '', '', NULL, 0, true, true, true, '1', now(), '1', now(), 0)
ON CONFLICT (id) DO NOTHING;
