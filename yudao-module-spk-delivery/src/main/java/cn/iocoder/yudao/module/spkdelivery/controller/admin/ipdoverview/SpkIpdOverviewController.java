package cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdoverview;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdoverview.vo.SpkIpdOverviewRespVO;
import cn.iocoder.yudao.module.spkdelivery.service.ipdoverview.SpkIpdOverviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * SPK-OS Cortext-IPD 总览 Controller（设计文档 §9.2 / 诉求 §2 总览统计）。
 * <p>
 * 一级导航首入口：聚合项目/版本/流程/问题/AI 成本六域 + 待办 + 路线图，
 * 供总览仪表盘首屏渲染。纯只读，复用既有 Service 聚合，不新增写路径。
 *
 * @author SPK-OS
 */
@Tag(name = "SPK IPD 总览")
@RestController
@RequestMapping("/spk/ipd/overview")
@Validated
public class SpkIpdOverviewController {

    @Resource
    private SpkIpdOverviewService overviewService;

    @GetMapping
    @Operation(summary = "总览首屏快照：计数+待办+最近流程+路线图")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:query')")
    public CommonResult<SpkIpdOverviewRespVO> snapshot() {
        return success(overviewService.snapshot());
    }

}
