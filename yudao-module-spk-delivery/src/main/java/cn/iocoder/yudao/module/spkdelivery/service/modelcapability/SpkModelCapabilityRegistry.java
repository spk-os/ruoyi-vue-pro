package cn.iocoder.yudao.module.spkdelivery.service.modelcapability;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.modelcapability.SpkModelCapabilityProfileDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.modelcapability.SpkModelRegistrySnapshotDO;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.modelcapability.SpkModelCapabilityProfileMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.modelcapability.SpkModelRegistrySnapshotMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * SPK-OS Model Capability Registry —— 模型能力注册表 + 快照
 * <p>
 * Task Router 在派发前调用 {@link #freezeSnapshot} 冻结一份全量 profile，snapshot_id 写入
 * TaskContract，保证一次 ActivityRun 内模型路由一致性（GAP-6）。依 §4.5。
 * <p>
 * P1 桩：profile 表由 init SQL 灌入固定 capability→glm 映射；freezeSnapshot 序列化全部 active profile。
 *
 * @author SPK-OS
 */
@Slf4j
@Service
public class SpkModelCapabilityRegistry {

    @Resource
    private SpkModelCapabilityProfileMapper profileMapper;
    @Resource
    private SpkModelRegistrySnapshotMapper snapshotMapper;

    /**
     * 冻结当前全部 active profile 为一份快照。
     *
     * @param frozenBy 冻结方（router / verifier）
     * @return snapshotId
     */
    public String freezeSnapshot(String frozenBy) {
        List<SpkModelCapabilityProfileDO> profiles = profileMapper.selectListByStatus("active");
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("profiles", profiles);
        envelope.put("count", profiles.size());
        String profileJson = JsonUtils.toJsonString(envelope);
        String snapshotId = "snap-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        SpkModelRegistrySnapshotDO snapshot = SpkModelRegistrySnapshotDO.builder()
                .snapshotId(snapshotId)
                .profileJson(profileJson)
                .frozenBy(frozenBy)
                .validAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusYears(1))
                .build();
        snapshotMapper.insert(snapshot);
        log.info("[freezeSnapshot][by={} snapshotId={} profiles={}]", frozenBy, snapshotId, profiles.size());
        return snapshotId;
    }

    /**
     * 按 capability_id 选最优 active profile（priority 最小者）。
     */
    public SpkModelCapabilityProfileDO selectBest(String capabilityId) {
        List<SpkModelCapabilityProfileDO> list =
                profileMapper.selectListByCapabilityIdAndStatus(capabilityId, "active");
        return list.isEmpty() ? null : list.get(0);
    }

    public SpkModelRegistrySnapshotDO getSnapshot(String snapshotId) {
        return snapshotMapper.selectBySnapshotId(snapshotId);
    }

    public List<SpkModelCapabilityProfileDO> listActive() {
        return profileMapper.selectListByStatus("active");
    }

}
