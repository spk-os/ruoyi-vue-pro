package cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdworkitem;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdworkitem.vo.SpkIpdWorkItemVO;
import cn.iocoder.yudao.module.spkdelivery.service.ipdworkitem.SpkIpdWorkItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * Plane 工作项 Controller（设计文档 §10.7）。
 *
 * @author SPK-OS
 */
@Tag(name = "SPK 研发 - Plane 工作项")
@RestController
@RequestMapping("/spk/ipd")
@Validated
public class SpkIpdWorkItemController {

    @Resource
    private SpkIpdWorkItemService workItemService;

    @GetMapping("/projects/{projectId}/work-items")
    @Operation(summary = "聚合项目下 Plane 需求/任务/缺陷快照")
    public CommonResult<List<SpkIpdWorkItemVO.RespVO>> getWorkItems(@PathVariable("projectId") Long projectId) {
        return success(workItemService.getWorkItems(projectId));
    }

    @PostMapping("/projects/{projectId}/work-items/link")
    @Operation(summary = "将 Plane issue 绑定到版本/流程/Activity")
    public CommonResult<SpkIpdWorkItemVO.RespVO> linkWorkItem(@PathVariable("projectId") Long projectId,
                                                             @Valid @RequestBody SpkIpdWorkItemVO.LinkReqVO req) {
        return success(workItemService.linkWorkItem(projectId, req));
    }

    @PostMapping("/projects/{projectId}/work-items/sync")
    @Operation(summary = "异步同步 Plane 工作项快照，返回 commandId")
    public CommonResult<SpkIpdWorkItemVO.SyncRespVO> syncWorkItems(@PathVariable("projectId") Long projectId,
                                                                  @RequestParam(required = false) String idempotencyKey) {
        return success(workItemService.syncWorkItems(projectId, idempotencyKey));
    }
}
