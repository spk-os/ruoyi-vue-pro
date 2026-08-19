package cn.iocoder.yudao.module.spkdelivery.service.ipdmonitor;

import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdmonitor.vo.SpkIpdMonitorRespVO;

import java.util.Map;

/**
 * SPK-OS Cortext-IPD 项目维度监控 Service（设计文档 §9.3 / 诉求 §4）。
 * <p>
 * 纯只读聚合：流程列表（含产物/证据计数）+ 汇总 + 集成健康。
 *
 * @author SPK-OS
 */
public interface SpkIpdMonitorService {

    /**
     * 项目维度监控：projectId 为空时聚合全部。
     */
    SpkIpdMonitorRespVO monitor(Long projectId);

    /**
     * AI 成本与会话聚合（设计文档 §4.5 Tab6）。
     * <p>
     * 真实三跳聚合：flow_run.pid → task_contract.contractId → run_receipt。
     * 按 模型 / Activity / 日趋势 聚合 sessions·tasks·成功率·Token·成本。
     * projectId 为空时聚合全部 run_receipt（全局）。无收据时返回空结构，不伪造。
     */
    Map<String, Object> costs(Long projectId);

}
