package cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdworkbench;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdworkbench.vo.SpkIpdCommandVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdworkbench.vo.SpkIpdWorkbenchRespVO;
import cn.iocoder.yudao.module.spkdelivery.service.ipdworkbench.SpkIpdWorkbenchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * IPD 指挥工作台 Controller（设计文档 §7.5 / §10.9）。
 *
 * @author SPK-OS
 */
@Tag(name = "SPK 研发 - IPD 指挥工作台")
@RestController
@RequestMapping("/spk/ipd/workbench")
@Validated
public class SpkIpdWorkbenchController {

    @Resource
    private SpkIpdWorkbenchService workbenchService;

    @GetMapping("/snapshot")
    @Operation(summary = "工作台快照：上下文+行动队列+阶段看板")
    public CommonResult<SpkIpdWorkbenchRespVO> getWorkbench(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long versionId) {
        return success(workbenchService.getWorkbench(SecurityFrameworkUtils.getLoginUserId(),
                projectId, versionId));
    }

    @GetMapping("/inbox")
    @Operation(summary = "行动队列")
    public CommonResult<SpkIpdWorkbenchRespVO> getInbox(
            @RequestParam(required = false) Long projectId) {
        return success(workbenchService.getInbox(SecurityFrameworkUtils.getLoginUserId(),
                projectId));
    }

    @GetMapping("/board")
    @Operation(summary = "阶段/任务看板")
    public CommonResult<SpkIpdWorkbenchRespVO> getBoard(
            @RequestParam(required = false) Long projectId) {
        return success(workbenchService.getBoard(projectId));
    }

    @PostMapping("/commands/parse")
    @Operation(summary = "自然语言命令解析为只读预览")
    public CommonResult<SpkIpdCommandVO.ParseResp> parseCommand(@Valid @RequestBody SpkIpdCommandVO.ParseReq req) {
        return success(workbenchService.parseCommand(req));
    }

    @PostMapping("/commands/execute")
    @Operation(summary = "执行已确认的幂等白名单命令")
    public CommonResult<SpkIpdCommandVO.ExecuteResp> executeCommand(@Valid @RequestBody SpkIpdCommandVO.ExecuteReq req) {
        return success(workbenchService.executeCommand(SecurityFrameworkUtils.getLoginUserId(), req));
    }
}
