/**
 * spkdelivery 包下，SPK-OS IPD 交付子系统。
 *
 * 基于芋道 yudao BPM + AI 内核承载 IPD 全流程：
 * agent 派任务（JavaDelegate 调 yudao-module-ai）+ G1-G8 门禁（HTTP_CALLBACK 触发器调 Gitea Actions）
 * + Aegis 同步审查（JavaDelegate LLM 裁决）+ Workflow Contract 版本治理 + FrameworkAdapter 多 runtime 适配。
 *
 * 1. Controller URL：以 /spk/ 开头，避免和其它 Module 冲突
 * 2. DataObject 表名：以 spk_ 开头，方便在数据库中区分
 */
package cn.iocoder.yudao.module.spkdelivery;
