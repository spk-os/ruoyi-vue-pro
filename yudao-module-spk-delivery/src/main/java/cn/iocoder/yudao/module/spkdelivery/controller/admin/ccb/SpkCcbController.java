package cn.iocoder.yudao.module.spkdelivery.controller.admin.ccb;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ccb.SpkCcbRecordDO;
import cn.iocoder.yudao.module.spkdelivery.service.ccb.SpkCcbService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * SPK-OS CCB Controller
 *
 * @author SPK-OS
 */
@Tag(name = "SPK CCB 变更")
@RestController
@RequestMapping("/spk/ccb")
@Validated
public class SpkCcbController {

    @Resource
    private SpkCcbService ccbService;

    @PostMapping("/register")
    @Operation(summary = "登记 CCB 变更记录")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ccb:register')")
    public CommonResult<SpkCcbRecordDO> register(
            @RequestParam(value = "changeId", required = false) String changeId,
            @RequestParam("processInstanceId") String processInstanceId,
            @RequestParam(value = "changeRequest", required = false) String changeRequest,
            @RequestParam(value = "impact", required = false) String impact,
            @RequestParam(value = "decision", required = false) String decision) {
        return success(ccbService.register(changeId, processInstanceId, changeRequest, impact, decision));
    }

}
