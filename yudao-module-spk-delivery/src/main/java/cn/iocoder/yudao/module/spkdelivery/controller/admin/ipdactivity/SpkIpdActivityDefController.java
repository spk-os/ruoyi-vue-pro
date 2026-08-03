package cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdactivity;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdactivity.vo.SpkIpdActivityDefPageReqVO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdactivity.SpkIpdActivityDefDO;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdactivity.SpkIpdActivityDefMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * IPD Activity 定义 Controller
 *
 * @author SPK-OS
 */
@Tag(name = "SPK IPD Activity 定义")
@RestController
@RequestMapping("/spk/ipd-activity-def")
@Validated
public class SpkIpdActivityDefController {

    @Resource
    private SpkIpdActivityDefMapper activityDefMapper;

    @GetMapping("/page")
    @Operation(summary = "分页查询 IPD Activity 定义")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-activity-def:query')")
    public CommonResult<PageResult<SpkIpdActivityDefDO>> page(SpkIpdActivityDefPageReqVO reqVO) {
        return success(activityDefMapper.selectPage(reqVO));
    }

    @GetMapping("/get-by-activity-id")
    @Operation(summary = "按 activityId+version 查询 Activity 定义")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-activity-def:query')")
    public CommonResult<SpkIpdActivityDefDO> getByActivityId(
            @Parameter(description = "Activity 业务标识") @RequestParam("activityId") String activityId,
            @Parameter(description = "版本") @RequestParam(value = "version", required = false) String version) {
        return success(activityDefMapper.selectByActivityIdAndVersion(activityId, version != null ? version : "1.0.0"));
    }

    @GetMapping("/list-by-stage")
    @Operation(summary = "按阶段查询 Activity 定义")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-activity-def:query')")
    public CommonResult<List<SpkIpdActivityDefDO>> listByStage(
            @Parameter(description = "阶段") @RequestParam("stage") String stage) {
        return success(activityDefMapper.selectListByStage(stage));
    }

}
