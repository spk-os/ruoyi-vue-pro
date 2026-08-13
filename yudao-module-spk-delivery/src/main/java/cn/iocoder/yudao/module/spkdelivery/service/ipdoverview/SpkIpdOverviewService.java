package cn.iocoder.yudao.module.spkdelivery.service.ipdoverview;

import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdoverview.vo.SpkIpdOverviewRespVO;

/**
 * SPK-OS Cortext-IPD 总览聚合 Service（设计文档 §9.2）。
 * <p>
 * 纯只读聚合：复用既有 Project/Version/FlowRun/Issue Mapper 的 selectCount / selectList，
 * 不新增写路径、不触碰 Flowable 引擎，符合"复用原生、只做扩展"铁律。
 *
 * @author SPK-OS
 */
public interface SpkIpdOverviewService {

    /**
     * 总览首屏快照：计数 + 待办 + 最近流程 + 路线图。
     */
    SpkIpdOverviewRespVO snapshot();

}
