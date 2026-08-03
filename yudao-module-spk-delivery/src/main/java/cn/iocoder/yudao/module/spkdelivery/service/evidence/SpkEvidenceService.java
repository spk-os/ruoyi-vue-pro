package cn.iocoder.yudao.module.spkdelivery.service.evidence;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.evidence.SpkEvidenceRecordDO;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.evidence.SpkEvidenceRecordMapper;
import cn.iocoder.yudao.module.spkdelivery.enums.SpkEvidenceTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * SPK-OS Evidence Service —— 证据中心（追加式，哈希链）
 * <p>
 * 任何组件只能 append：写入时读上一条 row_hash 作为 prev_hash，基于 payload + prev_hash 计算
 * 本行 row_hash。支持前向回放校验。依 SPK-OS-Cortext-IPD.md §4.4 / §5.6。
 * <p>
 * 铁律 4：证据先于截图——没有 evidence 的 Activity 不得标记 completed。
 *
 * @author SPK-OS
 */
@Slf4j
@Service
public class SpkEvidenceService {

    /** 哈希链创世占位（首条记录的 prev_hash） */
    public static final String GENESIS_PREV_HASH = "0".repeat(64);

    @Resource
    private SpkEvidenceRecordMapper evidenceMapper;

    /**
     * 追加一条证据。
     *
     * @param activityRunId    ActivityRun id
     * @param processInstanceId 流程实例 id
     * @param evidenceType      见 {@link SpkEvidenceTypeEnum}
     * @param refId             关联 receipt/approval id
     * @param payload           载荷对象（将序列化为稳定 JSON）
     * @return 写入后的证据记录
     */
    public SpkEvidenceRecordDO append(String activityRunId, String processInstanceId,
                                     String evidenceType, String refId, Object payload) {
        String payloadJson = payload == null ? "{}" : JsonUtils.toJsonString(payload);
        // 1. 取上一条 row_hash
        SpkEvidenceRecordDO prev = evidenceMapper.selectLastByActivityRunId(activityRunId);
        String prevHash = (prev != null && prev.getRowHash() != null) ? prev.getRowHash() : GENESIS_PREV_HASH;
        // 2. 计算本行哈希
        String rowHash = computeRowHash(payloadJson, prevHash);
        // 3. 落库（只 INSERT）
        SpkEvidenceRecordDO record = SpkEvidenceRecordDO.builder()
                .evidenceId(UUID.randomUUID().toString())
                .activityRunId(activityRunId)
                .processInstanceId(processInstanceId)
                .evidenceType(evidenceType)
                .refId(refId)
                .payload(payloadJson)
                .prevHash(prevHash)
                .rowHash(rowHash)
                .occurredAt(LocalDateTime.now())
                .build();
        evidenceMapper.insert(record);
        log.info("[append][activityRunId={} type={} refId={} rowHash={}]",
                activityRunId, evidenceType, refId, rowHash.substring(0, 12));
        return record;
    }

    /**
     * 回放校验：逐条重算 row_hash，任一不一致即链断裂。
     */
    public boolean verifyChain(String activityRunId) {
        List<SpkEvidenceRecordDO> chain = evidenceMapper.selectListByActivityRunId(activityRunId);
        String expectedPrev = GENESIS_PREV_HASH;
        for (SpkEvidenceRecordDO r : chain) {
            if (!expectedPrev.equals(r.getPrevHash())) {
                log.warn("[verifyChain][chain broken at id={} expectedPrev={} actualPrev={}]",
                        r.getId(), expectedPrev, r.getPrevHash());
                return false;
            }
            String recomputed = computeRowHash(r.getPayload(), r.getPrevHash());
            if (!recomputed.equals(r.getRowHash())) {
                log.warn("[verifyChain][hash mismatch at id={} expected={} actual={}]",
                        r.getId(), recomputed, r.getRowHash());
                return false;
            }
            expectedPrev = r.getRowHash();
        }
        return true;
    }

    public List<SpkEvidenceRecordDO> listByActivityRunId(String activityRunId) {
        return evidenceMapper.selectListByActivityRunId(activityRunId);
    }

    public List<SpkEvidenceRecordDO> listByProcessInstanceId(String processInstanceId) {
        return evidenceMapper.selectListByProcessInstanceId(processInstanceId);
    }

    /**
     * 计算本行哈希：SHA-256(payload || prev_hash)
     */
    private static String computeRowHash(String payloadJson, String prevHash) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            // 载荷先按字段名排序后再哈希，避免 Map 顺序抖动
            String stablePayload = stabilizePayload(payloadJson);
            md.update(stablePayload.getBytes(StandardCharsets.UTF_8));
            md.update(prevHash.getBytes(StandardCharsets.UTF_8));
            return toHex(md.digest());
        } catch (Exception e) {
            throw new RuntimeException("计算证据哈希失败", e);
        }
    }

    /**
     * 把 payload JSON 反序列化为有序 Map 再序列化，保证字段顺序稳定。
     */
    @SuppressWarnings("unchecked")
    private static String stabilizePayload(String payloadJson) {
        if (payloadJson == null || payloadJson.isEmpty()) {
            return "{}";
        }
        try {
            Map<String, Object> map = JsonUtils.parseMap(payloadJson);
            Map<String, Object> ordered = new LinkedHashMap<>();
            map.keySet().stream().sorted().forEach(k -> ordered.put(k, map.get(k)));
            return JsonUtils.toJsonString(ordered);
        } catch (Exception e) {
            // 非 JSON 载荷，原样返回
            return payloadJson;
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

}
