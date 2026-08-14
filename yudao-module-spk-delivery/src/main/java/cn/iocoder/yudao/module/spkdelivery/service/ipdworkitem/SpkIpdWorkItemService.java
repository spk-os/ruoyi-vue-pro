package cn.iocoder.yudao.module.spkdelivery.service.ipdworkitem;

import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdworkitem.vo.SpkIpdWorkItemVO;

import java.util.List;

/**
 * Plane 工作项服务（设计文档 §10.7）。
 * <p>
 * 聚合 Plane 需求/任务/缺陷快照，绑定到项目/版本/流程/Activity；异步同步返回 commandId。
 * 不创建/编辑 Plane issue，只存映射与必要快照。
 *
 * @author SPK-OS
 */
public interface SpkIpdWorkItemService {

    /** 聚合项目下 Plane 工作项快照（合并 Plane 实时与本地绑定） */
    List<SpkIpdWorkItemVO.RespVO> getWorkItems(Long projectId);

    /** 绑定 Plane issue 到版本/流程/Activity（幂等 upsert） */
    SpkIpdWorkItemVO.RespVO linkWorkItem(Long projectId, SpkIpdWorkItemVO.LinkReqVO req);

    /** 异步同步 Plane 工作项快照，返回 commandId（幂等） */
    SpkIpdWorkItemVO.SyncRespVO syncWorkItems(Long projectId, String idempotencyKey);
}
