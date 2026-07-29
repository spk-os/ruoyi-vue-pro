package cn.iocoder.yudao.module.spkdelivery.service.contract;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.contract.SpkContractVersionDO;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.contract.SpkContractVersionMapper;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * SPK-OS Workflow Contract 版本治理服务
 * <p>
 * 借鉴 Paddock workflow-contracts 的 hash/diff 思路：对 IPD 流程定义 simpleModel 做稳定化哈希
 * (递归排序键后序列化 → SHA256)，落 spk_contract_version；diff 对比上一版本，治理流程演进，
 * 缓解 yudao "改 simpleModel 须删模型重建" 带来的定义漂移风险。
 *
 * @author SPK-OS
 */
@Slf4j
@Service
@Validated
public class SpkContractVersionService {

    @Resource
    private SpkContractVersionMapper contractVersionMapper;

    /**
     * 对 simpleModel JSON 做稳定化序列化（递归按键名排序），消除键顺序差异，再算 SHA256。
     *
     * @param simpleModelJson simpleModel 的 JSON 字符串
     * @return SHA256 哈希
     */
    public String computeHash(String simpleModelJson) {
        Object normalized = stableize(JsonUtils.parseObject(simpleModelJson, Object.class));
        return DigestUtil.sha256Hex(JsonUtils.toJsonString(normalized));
    }

    /**
     * 计算某 modelKey 上一版本到当前快照的 diff。
     *
     * @param modelKey            流程模型 key
     * @param currentSimpleModel  当前 simpleModel JSON（可选；不传则只返回上一版本信息）
     * @return 版本 diff 结果
     */
    public ContractDiff diff(String modelKey, String currentSimpleModel) {
        SpkContractVersionDO latest = contractVersionMapper.selectLatestByModelKey(modelKey);
        ContractDiff result = new ContractDiff();
        result.setModelKey(modelKey);
        if (latest == null) {
            result.setPreviousVersion(0);
            result.setPreviousHash(null);
            result.setCurrentHash(currentSimpleModel == null ? null : computeHash(currentSimpleModel));
            result.setChanged(true);
            result.setDiffJson("[]");
            return result;
        }
        result.setPreviousVersion(latest.getVersion());
        result.setPreviousHash(latest.getHash());
        String currentHash = currentSimpleModel == null ? null : computeHash(currentSimpleModel);
        result.setCurrentHash(currentHash);
        boolean changed = currentHash != null && !currentHash.equals(latest.getHash());
        result.setChanged(changed);
        result.setDiffJson(changed ? buildDiff(latest.getSnapshotJson(), currentSimpleModel) : "[]");
        return result;
    }

    /**
     * 记录一版流程契约（hash + 快照 + 相对上一版的 diff）。
     *
     * @param modelKey           流程模型 key
     * @param simpleModelJson    simpleModel JSON 快照
     * @return 落库后的版本记录
     */
    public SpkContractVersionDO record(String modelKey, String simpleModelJson) {
        SpkContractVersionDO latest = contractVersionMapper.selectLatestByModelKey(modelKey);
        int nextVersion = latest == null ? 1 : (latest.getVersion() == null ? 1 : latest.getVersion() + 1);
        String hash = computeHash(simpleModelJson);
        String diffJson = latest == null ? "[]" :
                (hash.equals(latest.getHash()) ? "[]" : buildDiff(latest.getSnapshotJson(), simpleModelJson));
        SpkContractVersionDO version = SpkContractVersionDO.builder()
                .modelKey(modelKey)
                .hash(hash)
                .version(nextVersion)
                .diffJson(diffJson)
                .snapshotJson(simpleModelJson)
                .build();
        contractVersionMapper.insert(version);
        log.info("[record][modelKey={} version={} hash={} changed={}]", modelKey, nextVersion, hash, !"[]".equals(diffJson));
        return version;
    }

    // ======== 内部工具 ========

    /**
     * 递归排序 Map 的键，使序列化结果稳定（不依赖原始键顺序）。
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object stableize(Object o) {
        if (o instanceof Map) {
            TreeMap<String, Object> sorted = new TreeMap<>();
            ((Map<String, Object>) o).forEach((k, v) -> sorted.put(k, stableize(v)));
            return sorted;
        } else if (o instanceof List) {
            return o; // 列表保持原序（节点顺序语义重要）
        }
        return o;
    }

    /**
     * 简易结构 diff：对比两份 JSON 顶层键的差异（key 新增/删除/变更）。
     * 深度 JSON patch 留待后续接入 zjson-patch，MVP 用顶层差异足够治理。
     */
    @SuppressWarnings("unchecked")
    private String buildDiff(String oldJson, String newJson) {
        Map<String, Object> oldMap = oldJson == null ? Map.of() : JsonUtils.parseObject(oldJson, Map.class);
        Map<String, Object> newMap = newJson == null ? Map.of() : JsonUtils.parseObject(newJson, Map.class);
        Map<String, Object> diff = new LinkedHashMap<>();
        newMap.forEach((k, v) -> {
            if (!oldMap.containsKey(k)) {
                diff.put(k, "added");
            } else if (!String.valueOf(oldMap.get(k)).equals(String.valueOf(v))) {
                diff.put(k, "modified");
            }
        });
        oldMap.forEach((k, v) -> {
            if (!newMap.containsKey(k)) {
                diff.put(k, "removed");
            }
        });
        return JsonUtils.toJsonString(diff);
    }

    /**
     * 版本 diff 结果
     */
    @Data
    public static class ContractDiff {
        private String modelKey;
        private Integer previousVersion;
        private String previousHash;
        private String currentHash;
        private boolean changed;
        private String diffJson;
    }

}
