package cn.iocoder.yudao.module.spkdelivery.controller.admin.agentdef;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.agentdef.vo.SpkAgentDefCreateReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.agentdef.vo.SpkAgentDefPageReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.agentdef.vo.SpkAgentDefRespVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.agentdef.vo.SpkAgentDefStatusReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.agentdef.vo.SpkAgentDefUpdateReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.agentdef.vo.SpkAgentDefWakeReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.agentdef.vo.SpkAgentDefWakeRespVO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.agentdef.SpkAgentDefDO;
import cn.iocoder.yudao.module.spkdelivery.service.agentdef.SpkAgentDefService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * SPK-OS 智能体定义 Controller
 *
 * <p>对标 Paddock {@code /api/agents}：本地智能体定义 CRUD + 状态 + 隐藏 + 唤醒（单轮对话）。
 * 不依赖 openclaw，唤醒直接走 yudao-module-ai 内核。
 *
 * @author SPK-OS
 */
@Tag(name = "SPK 智能体定义")
@RestController
@RequestMapping("/spk/agent-def")
@Validated
public class SpkAgentDefController {

    @Resource
    private SpkAgentDefService agentDefService;

    @PostMapping("/create")
    @Operation(summary = "创建智能体")
    @PreAuthorize("@ss.hasPermission('spk-delivery:agent-def:create')")
    public CommonResult<Long> create(@Valid @RequestBody SpkAgentDefCreateReqVO reqVO) {
        return success(agentDefService.create(reqVO).getId());
    }

    @PutMapping("/update")
    @Operation(summary = "更新智能体")
    @PreAuthorize("@ss.hasPermission('spk-delivery:agent-def:update')")
    public CommonResult<Boolean> update(@Valid @RequestBody SpkAgentDefUpdateReqVO reqVO) {
        agentDefService.update(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除智能体")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('spk-delivery:agent-def:delete')")
    public CommonResult<Boolean> delete(@RequestParam("id") Long id) {
        agentDefService.delete(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取智能体")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('spk-delivery:agent-def:query')")
    public CommonResult<SpkAgentDefRespVO> get(@RequestParam("id") Long id) {
        SpkAgentDefDO agent = agentDefService.get(id);
        return success(BeanUtils.toBean(agent, SpkAgentDefRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询智能体")
    @PreAuthorize("@ss.hasPermission('spk-delivery:agent-def:query')")
    public CommonResult<PageResult<SpkAgentDefRespVO>> page(@Valid SpkAgentDefPageReqVO reqVO) {
        PageResult<SpkAgentDefDO> pageResult = agentDefService.getPage(reqVO);
        return success(BeanUtils.toBean(pageResult, SpkAgentDefRespVO.class));
    }

    @GetMapping("/list")
    @Operation(summary = "智能体列表（Squad 选成员用）")
    @PreAuthorize("@ss.hasPermission('spk-delivery:agent-def:query')")
    public CommonResult<List<SpkAgentDefRespVO>> list() {
        List<SpkAgentDefDO> list = agentDefService.getList();
        return success(BeanUtils.toBean(list, SpkAgentDefRespVO.class));
    }

    @PutMapping("/change-status")
    @Operation(summary = "变更智能体状态（idle/busy/offline/error）")
    @PreAuthorize("@ss.hasPermission('spk-delivery:agent-def:update')")
    public CommonResult<Boolean> changeStatus(@Valid @RequestBody SpkAgentDefStatusReqVO reqVO) {
        agentDefService.changeStatus(reqVO);
        return success(true);
    }

    @PostMapping("/hide")
    @Operation(summary = "隐藏智能体")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('spk-delivery:agent-def:update')")
    public CommonResult<Boolean> hide(@RequestParam("id") Long id) {
        agentDefService.toggleHidden(id, 1);
        return success(true);
    }

    @DeleteMapping("/hide")
    @Operation(summary = "取消隐藏智能体")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('spk-delivery:agent-def:update')")
    public CommonResult<Boolean> unhide(@RequestParam("id") Long id) {
        agentDefService.toggleHidden(id, 0);
        return success(true);
    }

    @PostMapping("/wake")
    @Operation(summary = "唤醒智能体（单轮对话，本地实现）")
    @PreAuthorize("@ss.hasPermission('spk-delivery:agent-def:wake')")
    public CommonResult<SpkAgentDefWakeRespVO> wake(@Valid @RequestBody SpkAgentDefWakeReqVO reqVO) {
        return success(agentDefService.wake(reqVO.getId(), reqVO.getMessage()));
    }

}
