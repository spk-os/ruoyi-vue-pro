-- SPK-OS Cortext-IPD UCD 重新设计 v4.0 菜单归位迁移（设计文档 §7）
-- 幂等 UPDATE：备份高频页提回主菜单 + 改名 + 重排 sort + 隐藏被合并的独立入口。
-- 不删任何菜单行，不改 component（前端 index.vue 与 component 映射已就位）。
-- 父菜单 6800「SPK 研发」。被隐藏的页面能力由合并方页面 + 路由重定向承载，不丢失功能。

-- ① 主菜单改名 + 重排（v4.0 六项，按 IPD 流程排序）
UPDATE system_menu SET name='研发总览',   sort=0 WHERE id=6960 AND deleted=0;
UPDATE system_menu SET name='项目空间',   sort=1 WHERE id=6823 AND deleted=0;
UPDATE system_menu SET name='监控与证据', sort=4 WHERE id=6880 AND deleted=0;

-- ② 从备份提回主菜单（parent_id 6970 -> 6800）
UPDATE system_menu SET parent_id=6800, name='我的审批',     sort=2 WHERE id=6900 AND deleted=0;
UPDATE system_menu SET parent_id=6800, name='指挥工作台',   sort=3 WHERE id=6920 AND deleted=0;
UPDATE system_menu SET parent_id=6800, name='团队与智能体', sort=5 WHERE id=6890 AND deleted=0;

-- ③ v4.0 隐藏被合并的独立入口（页面保留，路由重定向到合并方）
-- 流程中心 6940 并入「我的审批」，路由 /spk/backup/ipd-workflow 重定向到 /spk/ipd-approval
UPDATE system_menu SET visible=false WHERE id=6940 AND deleted=0;
-- 智能体管理目录 6810 + 智能体 6811 + 智能体编队 6812 并入「团队与智能体·Agent/编队 Tab」
UPDATE system_menu SET visible=false WHERE id IN (6810, 6811, 6812) AND deleted=0;
-- Plane 工作项 6950 降为「项目空间·需求与计划 Tab」
UPDATE system_menu SET visible=false WHERE id=6950 AND deleted=0;
-- 流程治理目录 6980 隐藏（4 子页 6987/6984/6985/6986 保留 visible=true，由「我的审批·流程模板·配置」Tab 复用其组件）
UPDATE system_menu SET visible=false WHERE id=6980 AND deleted=0;
