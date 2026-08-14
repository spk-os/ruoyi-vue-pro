package cn.iocoder.yudao.module.spkdelivery.service.ipdworkbench;

import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdworkbench.vo.SpkIpdCommandVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdworkbench.vo.SpkIpdWorkbenchRespVO;

/**
 * IPD 指挥工作台服务（设计文档 §7.5 / §10.9）。
 * <p>
 * 聚合行动队列（待办/Agent 异常/DCP/TR/证据缺口/同步失败）与阶段看板；
 * 自然语言命令仅解析为只读预览，确认后执行白名单领域命令（幂等）。
 *
 * @author SPK-OS
 */
public interface SpkIpdWorkbenchService {

    /** 工作台快照：上下文 + 行动队列 + 阶段看板 */
    SpkIpdWorkbenchRespVO getWorkbench(Long userId, Long projectId, Long versionId);

    /** 行动队列 */
    SpkIpdWorkbenchRespVO getInbox(Long userId, Long projectId);

    /** 阶段/任务看板 */
    SpkIpdWorkbenchRespVO getBoard(Long projectId);

    /** 自然语言命令解析（只读预览） */
    SpkIpdCommandVO.ParseResp parseCommand(SpkIpdCommandVO.ParseReq req);

    /** 命令执行（白名单、幂等） */
    SpkIpdCommandVO.ExecuteResp executeCommand(Long userId, SpkIpdCommandVO.ExecuteReq req);
}
