package cn.iocoder.yudao.module.spkdelivery.controller.admin.intellect;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.intellect.SpkIntellectQueueDO;
import cn.iocoder.yudao.module.spkdelivery.service.intellect.SpkIntellectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * SPK-OS OR 池 Controller
 *
 * @author SPK-OS
 */
@Tag(name = "SPK OR 池")
@RestController
@RequestMapping("/spk/intellect")
@Validated
public class SpkIntellectController {

    @Resource
    private SpkIntellectService intellectService;

    @GetMapping("/or-pool")
    @Operation(summary = "查询 OR 池需求队列")
    @PreAuthorize("@ss.hasPermission('spk-delivery:intellect:query')")
    public CommonResult<List<SpkIntellectQueueDO>> list(@Parameter(description = "状态") @RequestParam(value = "status", required = false) String status) {
        return success(intellectService.getList(status));
    }

    @PostMapping("/pick")
    @Operation(summary = "取出一个需求并发起 IPD 流程")
    @PreAuthorize("@ss.hasPermission('spk-delivery:intellect:pick')")
    public CommonResult<String> pick(
            @Parameter(description = "需求编号") @RequestParam("id") Long id,
            @Parameter(description = "发起人") @RequestParam(value = "userId", required = false) Long userId) {
        return success(intellectService.pick(id, userId));
    }

}
