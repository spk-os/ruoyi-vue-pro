package cn.iocoder.yudao.module.spkdelivery.service.flowconfig;

import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.governance.SpkFlowConfigSnapshotRespVO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdactivity.SpkIpdActivityDefDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdProcessProfileDO;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdactivity.SpkIpdActivityDefMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness.SpkIpdProcessProfileMapper;
import cn.iocoder.yudao.module.spkdelivery.service.delivery.DeliveryPathResolver;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * SPK-OS 流程配置聚合服务。设计文档 §B（流程配置聚合页读端 + 可编辑）。
 * <p>一站式聚合 Profile + activity_def（按 stage 分组）+ skill 目录 + 目录模板，供前端 flow-config 四 tab 渲染。
 * 同时提供 activity_def.skills / envRequirements 的行内保存。
 * <p>skill 目录扫描 /root/.claude/skills/spk-*（设计文档 §7 清单兜底，扫描失败不阻断）。
 *
 * @author SPK-OS
 */
@Slf4j
@Service
public class SpkFlowConfigService {

    /** skill 文件系统根目录 */
    private static final String SKILLS_DIR = "/root/.claude/skills";

    /** 设计文档 §7 skill 兜底清单（文件系统扫描失败时返回） */
    private static final List<String> SKILL_FALLBACK = Arrays.asList(
            "spk-ipd-concept", "spk-ipd-plan", "spk-ipd-develop",
            "spk-ipd-verify", "spk-ipd-launch", "spk-ipd-tr-gate");

    @Resource
    private SpkIpdProcessProfileMapper profileMapper;
    @Resource
    private SpkIpdActivityDefMapper activityDefMapper;

    /**
     * 流程配置聚合快照（一站式读端）。
     *
     * @param flowType FULL_RELEASE/INCREMENT_RELEASE/ISSUE_RESOLUTION；空则取 FULL_RELEASE
     */
    public SpkFlowConfigSnapshotRespVO getSnapshot(String flowType) {
        String ft = (flowType == null || flowType.isBlank()) ? "FULL_RELEASE" : flowType;
        SpkFlowConfigSnapshotRespVO resp = new SpkFlowConfigSnapshotRespVO();
        // Profile
        SpkIpdProcessProfileDO profile = profileMapper.selectByFlowType(ft);
        if (profile != null) {
            resp.setProfile(toProfileMap(profile));
            resp.setDirTemplate(profile.getDeliveryDirTemplate() != null
                    ? profile.getDeliveryDirTemplate() : DeliveryPathResolver.DEFAULT_DIR_TEMPLATE);
            resp.setDefaultProjectRootPattern(profile.getDefaultProjectRootPattern() != null
                    ? profile.getDefaultProjectRootPattern() : DeliveryPathResolver.DEFAULT_ROOT_PATTERN);
            resp.setEnvProfile(profile.getEnvProfile() != null ? profile.getEnvProfile() : "native-ai");
            resp.setDefaultSkillBindings(profile.getDefaultSkillBindings() != null
                    ? profile.getDefaultSkillBindings() : DeliveryPathResolver.DEFAULT_SKILL_BINDINGS);
        } else {
            // 未接入治理：用默认值兜底，前端标"未接入治理"
            resp.setDirTemplate(DeliveryPathResolver.DEFAULT_DIR_TEMPLATE);
            resp.setDefaultProjectRootPattern(DeliveryPathResolver.DEFAULT_ROOT_PATTERN);
            resp.setEnvProfile("native-ai");
            resp.setDefaultSkillBindings(DeliveryPathResolver.DEFAULT_SKILL_BINDINGS);
        }
        // activity_def 按 stage 分组（取 active 定义）
        resp.setActivityDefsByStage(groupActivityDefsByStage());
        // skill 目录
        resp.setSkillCatalog(scanSkillCatalog());
        return resp;
    }

    /**
     * 行内保存 activity_def 的 skills / envRequirements（流程配置页 tab3/tab4 编辑）。
     * 只更新这两个字段，其余不动。
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateActivityDefBindings(Long id, String skills, String envRequirements) {
        SpkIpdActivityDefDO def = activityDefMapper.selectById(id);
        if (def == null) {
            throw new IllegalArgumentException("Activity 定义不存在：id=" + id);
        }
        SpkIpdActivityDefDO update = new SpkIpdActivityDefDO();
        update.setId(id);
        update.setSkills(skills);
        update.setEnvRequirements(envRequirements);
        activityDefMapper.updateById(update);
        log.info("[updateActivityDefBindings][id={} skills={} envRequirements={} 已更新]",
                id, skills, envRequirements);
    }

    /** Profile DO → 前端可读 Map（含新治理字段） */
    private Map<String, Object> toProfileMap(SpkIpdProcessProfileDO p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("profileCode", p.getProfileCode());
        m.put("name", p.getName());
        m.put("flowType", p.getFlowType());
        m.put("processDefinitionKey", p.getProcessDefinitionKey());
        m.put("status", p.getStatus());
        m.put("currentVersion", p.getCurrentVersion());
        m.put("description", p.getDescription());
        m.put("deliveryDirTemplate", p.getDeliveryDirTemplate());
        m.put("defaultProjectRootPattern", p.getDefaultProjectRootPattern());
        m.put("envProfile", p.getEnvProfile());
        m.put("defaultSkillBindings", p.getDefaultSkillBindings());
        return m;
    }

    /** activity_def 按 stage 分组，每组按 activityId 升序；输出精简字段供前端表格渲染 */
    private Map<String, List<Map<String, Object>>> groupActivityDefsByStage() {
        List<SpkIpdActivityDefDO> all = activityDefMapper.selectListByStatus("active");
        Map<String, List<Map<String, Object>>> grouped = new TreeMap<>();
        for (SpkIpdActivityDefDO d : all) {
            String stage = d.getStage() != null ? d.getStage() : "unknown";
            grouped.computeIfAbsent(stage, k -> new ArrayList<>()).add(toActivityDefMap(d));
        }
        return grouped;
    }

    private Map<String, Object> toActivityDefMap(SpkIpdActivityDefDO d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", d.getId());
        m.put("activityId", d.getActivityId());
        m.put("name", d.getName());
        m.put("stage", d.getStage());
        m.put("outputArtifactType", d.getOutputArtifactType());
        m.put("useIndependentVerifier", d.getUseIndependentVerifier());
        m.put("verifierType", d.getVerifierType());
        m.put("executionLocation", d.getExecutionLocation());
        m.put("skills", d.getSkills());
        m.put("envRequirements", d.getEnvRequirements());
        m.put("promptTemplate", d.getPromptTemplate());
        return m;
    }

    /**
     * 扫描 /root/.claude/skills/ 下 spk-* 目录作为可选 skill 目录。
     * 扫描失败/无权限返回设计文档 §7 兜底清单，不阻断前端渲染。
     */
    private List<String> scanSkillCatalog() {
        List<String> result = new ArrayList<>();
        try {
            Path dir = Paths.get(SKILLS_DIR);
            if (Files.isDirectory(dir)) {
                try (var stream = Files.list(dir)) {
                    stream.filter(Files::isDirectory)
                            .map(p -> p.getFileName().toString())
                            .filter(n -> n.startsWith("spk-"))
                            .sorted()
                            .forEach(result::add);
                }
            }
        } catch (Exception e) {
            log.warn("[scanSkillCatalog][扫描 {} 失败用兜底清单：{}]", SKILLS_DIR, e.getMessage());
        }
        if (result.isEmpty()) {
            result.addAll(SKILL_FALLBACK);
        } else {
            // 确保兜底清单项都在（即使文件系统未建也可见）
            for (String s : SKILL_FALLBACK) {
                if (!result.contains(s)) {
                    result.add(s);
                }
            }
        }
        return result;
    }
}
