package cn.iocoder.yudao.module.spkdelivery.controller.admin.agentsquad;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.agentsquad.vo.SpkAgentSquadCreateReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.agentsquad.vo.SpkAgentSquadMemberReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.agentsquad.vo.SpkAgentSquadMemberRespVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.agentsquad.vo.SpkAgentSquadPageReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.agentsquad.vo.SpkAgentSquadRespVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.agentsquad.vo.SpkAgentSquadUpdateReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.agentsquad.vo.SpkAgentSquadWakeReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.agentsquad.vo.SpkAgentSquadWakeRespVO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.agentsquad.SpkAgentSquadDO;
import cn.iocoder.yudao.module.spkdelivery.service.agentsquad.SpkAgentSquadService;
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
 * SPK-OS 智能体编队 Controller
 *
 * <p>与 Paddock 的差别：独立编队实体（头表 + 成员表），支持成员管理与按顺序串行唤醒。
 *
 * @author SPK-OS
 */
@Tag(name = "SPK 智能体编队")
@RestController
@RequestMapping("/spk/agent-squad")
@Validated
public class SpkAgentSquadController {

    @Resource
    private SpkAgentSquadService squadService;

    @PostMapping("/create")
    @Operation(summary = "创建编队")
    @PreAuthorize("@ss.hasPermission('spk-delivery:agent-squad:create')")
    public CommonResult<Long> create(@Valid @RequestBody SpkAgentSquadCreateReqVO reqVO) {
        return success(squadService.create(reqVO).getId());
    }

    @PutMapping("/update")
    @Operation(summary = "更新编队")
    @PreAuthorize("@ss.hasPermission('spk-delivery:agent-squad:update')")
    public CommonResult<Boolean> update(@Valid @RequestBody SpkAgentSquadUpdateReqVO reqVO) {
        squadService.update(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除编队")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('spk-delivery:agent-squad:delete')")
    public CommonResult<Boolean> delete(@RequestParam("id") Long id) {
        squadService.delete(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取编队")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('spk-delivery:agent-squad:query')")
    public CommonResult<SpkAgentSquadRespVO> get(@RequestParam("id") Long id) {
        SpkAgentSquadDO squad = squadService.get(id);
        SpkAgentSquadRespVO resp = BeanUtils.toBean(squad, SpkAgentSquadRespVO.class);
        if (resp != null) {
            resp.setMemberCount(squadService.countMembers(id));
        }
        return success(resp);
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询编队")
    @PreAuthorize("@ss.hasPermission('spk-delivery:agent-squad:query')")
    public CommonResult<PageResult<SpkAgentSquadRespVO>> page(@Valid SpkAgentSquadPageReqVO reqVO) {
        PageResult<SpkAgentSquadDO> pageResult = squadService.getPage(reqVO);
        PageResult<SpkAgentSquadRespVO> respPage = BeanUtils.toBean(pageResult, SpkAgentSquadRespVO.class);
        if (respPage.getList() != null) {
            respPage.getList().forEach(r -> r.setMemberCount(squadService.countMembers(r.getId())));
        }
        return success(respPage);
    }

    @GetMapping("/list")
    @Operation(summary = "编队列表")
    @PreAuthorize("@ss.hasPermission('spk-delivery:agent-squad:query')")
    public CommonResult<List<SpkAgentSquadRespVO>> list() {
        List<SpkAgentSquadDO> list = squadService.getList();
        List<SpkAgentSquadRespVO> respList = BeanUtils.toBean(list, SpkAgentSquadRespVO.class);
        if (respList != null) {
            respList.forEach(r -> r.setMemberCount(squadService.countMembers(r.getId())));
        }
        return success(respList);
    }

    // ---------- 成员管理 ----------

    @GetMapping("/member/list")
    @Operation(summary = "获取编队成员列表")
    @Parameter(name = "squadId", description = "编队编号", required = true)
    @PreAuthorize("@ss.hasPermission('spk-delivery:agent-squad:query')")
    public CommonResult<List<SpkAgentSquadMemberRespVO>> memberList(@RequestParam("squadId") Long squadId) {
        return success(squadService.getMembers(squadId));
    }

    @PostMapping("/member/add")
    @Operation(summary = "新增编队成员")
    @PreAuthorize("@ss.hasPermission('spk-delivery:agent-squad:update')")
    public CommonResult<Boolean> addMember(@Valid @RequestBody SpkAgentSquadMemberReqVO reqVO) {
        squadService.addMember(reqVO);
        return success(true);
    }

    @PutMapping("/member/update")
    @Operation(summary = "更新编队成员")
    @PreAuthorize("@ss.hasPermission('spk-delivery:agent-squad:update')")
    public CommonResult<Boolean> updateMember(@Valid @RequestBody SpkAgentSquadMemberReqVO reqVO) {
        squadService.updateMember(reqVO);
        return success(true);
    }

    @DeleteMapping("/member/remove")
    @Operation(summary = "移除编队成员")
    @Parameter(name = "id", description = "成员编号", required = true)
    @PreAuthorize("@ss.hasPermission('spk-delivery:agent-squad:update')")
    public CommonResult<Boolean> removeMember(@RequestParam("id") Long id) {
        squadService.removeMember(id);
        return success(true);
    }

    // ---------- 编队唤醒 ----------

    @PostMapping("/wake")
    @Operation(summary = "唤醒编队（成员按顺序串行单轮对话）")
    @PreAuthorize("@ss.hasPermission('spk-delivery:agent-squad:wake')")
    public CommonResult<SpkAgentSquadWakeRespVO> wake(@Valid @RequestBody SpkAgentSquadWakeReqVO reqVO) {
        return success(squadService.wake(reqVO.getId(), reqVO.getMessage()));
    }

}
