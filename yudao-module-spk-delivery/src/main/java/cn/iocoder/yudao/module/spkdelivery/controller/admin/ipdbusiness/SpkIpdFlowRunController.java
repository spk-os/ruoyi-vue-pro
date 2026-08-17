package cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.flowrun.SpkIpdFlowRunPageReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.flowrun.SpkIpdFlowRunPreflightReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.version.SpkIpdReadinessRespVO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdCommandLogDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdFlowRunDO;
import cn.iocoder.yudao.module.spkdelivery.service.ipdbusiness.SpkIpdFlowRunService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * SPK-OS Cortext-IPD FlowRun Controller（4 级业务模型第 4 层：流程运行）。
 * 设计文档 §10.5。新业务 API 前缀 /spk/ipd。
 * <p>
 * FlowRun 是业务运行，process_instance_id 是引擎运行；一条 FlowRun 最多绑定一个 Flowable 实例。
 * 启动幂等（Idempotency-Key + CommandLog），businessKey=IPD:{runNo}。
 * S1 实现：preflight/create/start/cancel/timeline/activities/diagram 骨架；block/unblock/retry 状态机落地，
 * engineering/diagram 在 S3/S5 接入 Agent/PR/CI 后补全聚合。
 *
 * @author SPK-OS
 */
@Tag(name = "SPK IPD FlowRun")
@RestController
@RequestMapping("/spk/ipd")
@Validated
public class SpkIpdFlowRunController {

    @Resource
    private SpkIpdFlowRunService flowRunService;

    @PostMapping("/flow-runs/preflight")
    @Operation(summary = "预检：不落运行，返回可启动性与逐项检查")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:create')")
    public CommonResult<SpkIpdReadinessRespVO> preflight(@Valid @RequestBody SpkIpdFlowRunPreflightReqVO req) {
        return success(flowRunService.preflight(req));
    }

    @PostMapping("/flow-runs")
    @Operation(summary = "创建运行草稿，选择版本/类型/档案/裁剪项")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:create')")
    public CommonResult<SpkIpdFlowRunDO> createFlowRun(@Valid @RequestBody SpkIpdFlowRunPreflightReqVO req) {
        return success(flowRunService.createFlowRun(req));
    }

    @GetMapping("/flow-runs")
    @Operation(summary = "FlowRun 分页（项目/版本/类型/状态/阶段）")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:query')")
    public CommonResult<PageResult<SpkIpdFlowRunDO>> pageFlowRuns(@Valid SpkIpdFlowRunPageReqVO req) {
        return success(flowRunService.page(req));
    }

    @GetMapping("/flow-runs/{flowRunId}")
    @Operation(summary = "聚合运行摘要和引擎快照")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:query')")
    public CommonResult<SpkIpdFlowRunDO> getFlowRun(@PathVariable("flowRunId") Long flowRunId) {
        return success(flowRunService.getFlowRun(flowRunId));
    }

    @PostMapping("/flow-runs/{flowRunId}/start")
    @Operation(summary = "幂等启动 Flowable；返回当前命令状态")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:create')")
    public CommonResult<Map<String, Object>> start(@PathVariable("flowRunId") Long flowRunId,
                                                   @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                                   HttpServletRequest request) {
        // Idempotency-Key 缺失时用 traceId 兜底，保证幂等性
        String key = idempotencyKey != null ? idempotencyKey
                : "start-" + flowRunId + "-" + System.nanoTime();
        return success(flowRunService.start(flowRunId, key));
    }

    @PostMapping("/flow-runs/{flowRunId}/cancel")
    @Operation(summary = "取消业务流并调用引擎；要求理由；幂等（Idempotency-Key）")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:update')")
    public CommonResult<SpkIpdFlowRunDO> cancel(@PathVariable("flowRunId") Long flowRunId,
                                                @RequestBody(required = false) Map<String, String> body,
                                                @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        String reason = body == null ? null : body.get("reason");
        if (reason == null || reason.isBlank()) {
            reason = "用户取消";
        }
        String key = idempotencyKey != null ? idempotencyKey : "cancel-" + flowRunId + "-" + System.nanoTime();
        return success(flowRunService.cancel(flowRunId, reason, key));
    }

    @PostMapping("/flow-runs/{flowRunId}/retry")
    @Operation(summary = "重试：仅 FAILED 可重试；创建新 attempt，旧运行置 SUPERSEDED；幂等")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:update')")
    public CommonResult<Map<String, Object>> retry(@PathVariable("flowRunId") Long flowRunId,
                                                   @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        String key = idempotencyKey != null ? idempotencyKey : "retry-" + flowRunId + "-" + System.nanoTime();
        return success(flowRunService.retry(flowRunId, key));
    }

    @PostMapping("/flow-runs/{flowRunId}/block")
    @Operation(summary = "人工阻断：置 BLOCKED 不取消引擎；理由必填；幂等")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:update')")
    public CommonResult<SpkIpdFlowRunDO> block(@PathVariable("flowRunId") Long flowRunId,
                                               @RequestBody(required = false) Map<String, String> body,
                                               @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        String reason = body == null ? null : body.get("reason");
        String key = idempotencyKey != null ? idempotencyKey : "block-" + flowRunId + "-" + System.nanoTime();
        return success(flowRunService.block(flowRunId, reason, key));
    }

    @PostMapping("/flow-runs/{flowRunId}/unblock")
    @Operation(summary = "解除阻断：BLOCKED → RUNNING；理由必填（审计）；幂等")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:update')")
    public CommonResult<SpkIpdFlowRunDO> unblock(@PathVariable("flowRunId") Long flowRunId,
                                                 @RequestBody(required = false) Map<String, String> body,
                                                 @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        String reason = body == null ? null : body.get("reason");
        String key = idempotencyKey != null ? idempotencyKey : "unblock-" + flowRunId + "-" + System.nanoTime();
        return success(flowRunService.unblock(flowRunId, reason, key));
    }

    @PostMapping("/flow-runs/rollback-iteration")
    @Operation(summary = "Phase2：跨迭代回滚——按 (projectId/majorReleaseId/versionId) 定位迭代 FlowRun 的 .flow/manifest 快照，重置 current_stage 到快照阶段并置 BLOCKED 待重执行")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:update')")
    public CommonResult<Map<String, Object>> rollbackIteration(@RequestParam("projectId") Long projectId,
                                                                @RequestParam(value = "majorReleaseId", required = false) Long majorReleaseId,
                                                                @RequestParam("versionId") Long versionId) {
        return success(flowRunService.rollbackToIteration(projectId, majorReleaseId, versionId));
    }

    @GetMapping("/flow-runs/{flowRunId}/timeline")
    @Operation(summary = "合并 BPM、Agent、Gate、PR/CI、决策、产物事件")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:query')")
    public CommonResult<Map<String, Object>> timeline(@PathVariable("flowRunId") Long flowRunId) {
        return success(flowRunService.timeline(flowRunId));
    }

    @GetMapping("/flow-runs/{flowRunId}/activities")
    @Operation(summary = "活动定义、运行、分派、状态、计划/实际时间（S1 返回 Flowable 活动骨架）")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:query')")
    public CommonResult<Map<String, Object>> activities(@PathVariable("flowRunId") Long flowRunId) {
        return success(flowRunService.timeline(flowRunId));
    }

    @GetMapping("/flow-runs/{flowRunId}/diagram")
    @Operation(summary = "返回 Flowable 图和 SPK 状态叠加数据（骨架，skeleton=true）")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:query')")
    public CommonResult<Map<String, Object>> diagram(@PathVariable("flowRunId") Long flowRunId) {
        return success(java.util.Map.of(
                "flowRunId", flowRunId,
                "skeleton", true,
                "stage", "S1_SKELETON",
                "note", "diagram 叠加将在 S3 接入 Activity 运行状态后提供"));
    }

    @GetMapping("/flow-runs/{flowRunId}/engineering")
    @Operation(summary = "按 Activity 聚合 branch/commit/PR/CI/release（骨架，skeleton=true）")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:query')")
    public CommonResult<Map<String, Object>> engineering(@PathVariable("flowRunId") Long flowRunId) {
        return success(java.util.Map.of(
                "flowRunId", flowRunId,
                "skeleton", true,
                "stage", "S1_SKELETON",
                "note", "engineering 聚合将在 S5 接入 Gitea/PR/CI 后提供"));
    }

    @GetMapping("/flow-runs/{flowRunId}/commands/{commandId}")
    @Operation(summary = "查询异步命令执行结果")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:query')")
    public CommonResult<SpkIpdCommandLogDO> getCommand(@PathVariable("flowRunId") Long flowRunId,
                                                       @PathVariable("commandId") Long commandId) {
        return success(flowRunService.getCommand(commandId));
    }
}
