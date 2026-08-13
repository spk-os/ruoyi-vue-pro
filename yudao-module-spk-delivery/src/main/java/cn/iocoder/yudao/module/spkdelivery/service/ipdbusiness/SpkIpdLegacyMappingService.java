package cn.iocoder.yudao.module.spkdelivery.service.ipdbusiness;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdFlowRunDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdLegacyMappingDO;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness.SpkIpdFlowRunMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness.SpkIpdLegacyMappingMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * IPD 旧实例到新业务对象的可审计映射服务。设计文档 §5.2 / §9.6 / §12.9。
 * <p>
 * 旧路由以 process_instance_id 为主键启动项目；新模型以 4 级业务对象为主轴。
 * 本服务把旧 process_instance_id 解析到 FlowRun（进而到 project/version），
 * 并登记未映射实例到 spk_ipd_legacy_mapping 供人工确认。
 * <p>
 * 规则：先查 FlowRun.process_instance_id（已迁移的旧实例）；命中即返回。
 * 未命中再查 legacy_mapping；仍未命中则登记 NEEDS_MAPPING。
 *
 * @author SPK-OS
 */
@Slf4j
@Service
@Validated
public class SpkIpdLegacyMappingService {

    @Resource
    private SpkIpdLegacyMappingMapper legacyMappingMapper;
    @Resource
    private SpkIpdFlowRunMapper flowRunMapper;

    /**
     * 解析旧 process_instance_id 到新业务上下文。
     * @return mappingStatus: MAPPED/NEEDS_MAPPING/UNMAPPABLE；MAPPED 时带 project/version/flowRun
     */
    public Map<String, Object> resolve(String processInstanceId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("legacyType", "PROCESS_INSTANCE");
        result.put("legacyId", processInstanceId);

        // 1. 先查 FlowRun（旧实例已被绑定到新 FlowRun）
        SpkIpdFlowRunDO run = flowRunMapper.selectByProcessInstanceId(processInstanceId);
        if (run != null) {
            result.put("mappingStatus", "MAPPED");
            result.put("flowRunId", run.getId());
            result.put("projectId", run.getProjectId());
            result.put("versionId", run.getVersionId());
            result.put("issueCaseId", run.getIssueCaseId());
            return result;
        }

        // 2. 再查 legacy_mapping
        SpkIpdLegacyMappingDO legacy = legacyMappingMapper.selectByLegacy("PROCESS_INSTANCE", processInstanceId);
        if (legacy != null) {
            result.put("mappingStatus", legacy.getMappingStatus());
            result.put("mappingId", legacy.getId());
            result.put("projectId", legacy.getProjectId());
            result.put("versionId", legacy.getVersionId());
            result.put("flowRunId", legacy.getFlowRunId());
            result.put("confidence", legacy.getConfidence());
            return result;
        }

        // 3. 未登记：落 NEEDS_MAPPING，等人工确认
        SpkIpdLegacyMappingDO record = SpkIpdLegacyMappingDO.builder()
                .legacyType("PROCESS_INSTANCE")
                .legacyId(processInstanceId)
                .mappingStatus("NEEDS_MAPPING")
                .ruleVersion("v1")
                .confidence("LOW")
                .build();
        legacyMappingMapper.insert(record);
        result.put("mappingStatus", "NEEDS_MAPPING");
        result.put("mappingId", record.getId());
        result.put("message", "旧实例尚未映射到新业务对象，已登记待人工确认");
        return result;
    }

    /** 待人工确认的未映射实例列表 */
    public List<SpkIpdLegacyMappingDO> listNeedsMapping() {
        return legacyMappingMapper.selectListNeedsMapping();
    }

    /**
     * 人工确认旧实例到业务对象的映射。
     */
    @Transactional(rollbackFor = Exception.class)
    public SpkIpdLegacyMappingDO confirm(Long mappingId, Long projectId, Long versionId, Long flowRunId) {
        SpkIpdLegacyMappingDO m = legacyMappingMapper.selectById(mappingId);
        if (m == null) {
            return null;
        }
        m.setProjectId(projectId);
        m.setVersionId(versionId);
        m.setFlowRunId(flowRunId);
        m.setMappingStatus("MAPPED");
        m.setConfidence("HIGH");
        m.setConfirmedBy(currentUserId());
        m.setConfirmedAt(LocalDateTime.now());
        legacyMappingMapper.updateById(m);
        log.info("[confirm][mappingId={} legacyId={} -> project/version/flowRun={}/{}/{}]",
                mappingId, m.getLegacyId(), projectId, versionId, flowRunId);
        return m;
    }

    private Long currentUserId() {
        Long uid = SecurityFrameworkUtils.getLoginUserId();
        return uid != null ? uid : 1L;
    }
}
