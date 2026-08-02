package cn.iocoder.yudao.module.spkdelivery.controller.admin.feedback;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.feedback.SpkFeedbackDO;
import cn.iocoder.yudao.module.spkdelivery.service.feedback.SpkFeedbackService;
import io.swagger.v3.oas.annotations.Operation;
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
 * SPK-OS R7 反馈 Controller
 *
 * @author SPK-OS
 */
@Tag(name = "SPK R7 反馈")
@RestController
@RequestMapping("/spk/feedback")
@Validated
public class SpkFeedbackController {

    @Resource
    private SpkFeedbackService feedbackService;

    @PostMapping("/collect")
    @Operation(summary = "采集 R7 反馈")
    @PreAuthorize("@ss.hasPermission('spk-delivery:feedback:collect')")
    public CommonResult<SpkFeedbackDO> collect(
            @RequestParam("processInstanceId") String processInstanceId,
            @RequestParam(value = "source", required = false) String source,
            @RequestParam(value = "content", required = false) String content,
            @RequestParam(value = "summary", required = false) String summary,
            @RequestParam(value = "newCharterSeed", required = false) Boolean newCharterSeed) {
        return success(feedbackService.collect(processInstanceId, source, content, summary, newCharterSeed));
    }

    @GetMapping("/list-by-instance")
    @Operation(summary = "按流程实例查询 R7 反馈（IPD 产物 tab）")
    @PreAuthorize("@ss.hasPermission('spk-delivery:feedback:query')")
    public CommonResult<List<SpkFeedbackDO>> listByInstance(
            @RequestParam("processInstanceId") String processInstanceId) {
        return success(feedbackService.getListByInstanceId(processInstanceId));
    }

}
