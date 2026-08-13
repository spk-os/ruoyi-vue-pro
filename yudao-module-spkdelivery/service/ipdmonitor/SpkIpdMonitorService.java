package cn.iocoder.yudao.module.spkdelivery.service.ipdmonitor;

import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdmonitor.vo.SpkIpdMonitorRespVO;

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

}
