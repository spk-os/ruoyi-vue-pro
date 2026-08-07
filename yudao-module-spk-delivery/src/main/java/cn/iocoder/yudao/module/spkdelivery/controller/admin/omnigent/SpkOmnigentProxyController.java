package cn.iocoder.yudao.module.spkdelivery.controller.admin.omnigent;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.spkdelivery.service.integration.SpkOmnigentProxyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * SPK-OS Omnigent 会话代理 Controller
 * <p>
 * 端点前缀 /spk/ipd/omnigent-proxy：
 * <ul>
 *   <li>GET /session/{sid} —— 代理 Omnigent GET /v1/sessions/{sid}（session 元数据，iframe 嵌入用）。</li>
 *   <li>GET /session/{sid}/stream —— SSE 转发 assistant 文本流（5min 超时，前端 EventSource 订阅）。</li>
 * </ul>
 * 鉴权在后端注入 ap_session cookie，前端不接触 Omnigent 凭证；RBAC 走 spk-delivery:ipd-cockpit:query。
 *
 * @author SPK-OS
 */
@Tag(name = "SPK Omnigent 代理")
@RestController
@RequestMapping("/spk/ipd/omnigent-proxy")
@Validated
public class SpkOmnigentProxyController {

    @Resource
    private SpkOmnigentProxyService proxyService;

    @GetMapping("/session/{sid}")
    @Operation(summary = "Omnigent session 元数据代理（iframe 嵌入用）")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-cockpit:query')")
    public CommonResult<String> session(
            @Parameter(description = "Omnigent session id") @PathVariable("sid") String sid) {
        return success(proxyService.getSession(sid));
    }

    @GetMapping(value = "/session/{sid}/stream", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Omnigent session SSE 流转发（5min 超时）")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-cockpit:query')")
    public SseEmitter stream(
            @Parameter(description = "Omnigent session id") @PathVariable("sid") String sid) {
        return proxyService.streamSession(sid);
    }

}
