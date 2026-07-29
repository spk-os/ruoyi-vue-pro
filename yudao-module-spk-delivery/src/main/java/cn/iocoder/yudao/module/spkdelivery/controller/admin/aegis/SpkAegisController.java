package cn.iocoder.yudao.module.spkdelivery.controller.admin.aegis;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.aegis.SpkAegisReviewDO;
import cn.iocoder.yudao.module.spkdelivery.service.aegis.SpkAegisReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * SPK-OS Aegis 审查 Controller
 * <p>
 * /review：BPM HTTP_REQUEST 触发器同步调用，回写 aegisVerdict/aegisReport 变量。
 * /manual：LLM 不可用时人工出结论。
 *
 * @author SPK-OS
 */
@Tag(name = "SPK Aegis 审查")
@RestController
@RequestMapping("/spk/aegis")
@Validated
public class SpkAegisController {

    @Resource
    private SpkAegisReviewService aegisReviewService;

    @PostMapping("/review")
    @PermitAll
    @Operation(summary = "Aegis 同步审查（BPM HTTP_REQUEST 触发器调用，回写 aegisVerdict/aegisReport）")
    public CommonResult<Map<String, Object>> review(
            @Parameter(description = "流程实例编号") @RequestParam("processInstanceId") String processInstanceId,
            @Parameter(description = "BPM 节点 key") @RequestParam(value = "nodeKey", required = false) String nodeKey) {
        SpkAegisReviewDO review = aegisReviewService.review(processInstanceId, nodeKey);
        Map<String, Object> data = new HashMap<>();
        data.put("aegisVerdict", review.getVerdict());
        data.put("aegisReport", review.getReport());
        return success(data);
    }

    @PostMapping("/manual")
    @PermitAll
    @Operation(summary = "Aegis 人工复核（LLM 不可用时出结论）")
    public CommonResult<Map<String, Object>> manual(
            @Parameter(description = "流程实例编号") @RequestParam("processInstanceId") String processInstanceId,
            @Parameter(description = "BPM 节点 key") @RequestParam(value = "nodeKey", required = false) String nodeKey,
            @Parameter(description = "裁决 pass/fail/conditional") @RequestParam("verdict") String verdict,
            @Parameter(description = "审查报告") @RequestParam(value = "report", required = false) String report) {
        SpkAegisReviewDO review = aegisReviewService.manualReview(processInstanceId, nodeKey, verdict, report);
        Map<String, Object> data = new HashMap<>();
        data.put("aegisVerdict", review.getVerdict());
        data.put("aegisReport", review.getReport());
        return success(data);
    }

}
