package cn.iocoder.yudao.module.spkdelivery.controller.admin.modelcapability;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.modelcapability.SpkModelCapabilityProfileDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.modelcapability.SpkModelRegistrySnapshotDO;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.modelcapability.SpkModelCapabilityProfileMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.modelcapability.SpkModelRegistrySnapshotMapper;
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
 * Model Capability Controller
 *
 * @author SPK-OS
 */
@Tag(name = "SPK Model Capability 模型能力")
@RestController
@RequestMapping("/spk/model-capability")
@Validated
public class SpkModelCapabilityController {

    @Resource
    private SpkModelCapabilityProfileMapper profileMapper;
    @Resource
    private SpkModelRegistrySnapshotMapper snapshotMapper;

    @GetMapping("/profile/list")
    @Operation(summary = "查询全部 active 模型能力画像")
    @PreAuthorize("@ss.hasPermission('spk-delivery:model-capability:query')")
    public CommonResult<List<SpkModelCapabilityProfileDO>> profileList() {
        return success(profileMapper.selectListByStatus("active"));
    }

    @GetMapping("/snapshot/get")
    @Operation(summary = "按 snapshotId 查询模型快照")
    @PreAuthorize("@ss.hasPermission('spk-delivery:model-capability:query')")
    public CommonResult<SpkModelRegistrySnapshotDO> snapshotGet(
            @Parameter(description = "snapshotId") @RequestParam("snapshotId") String snapshotId) {
        return success(snapshotMapper.selectBySnapshotId(snapshotId));
    }

}
