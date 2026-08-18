package cn.iocoder.yudao.module.spkdelivery.service.flowconfig;

import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.governance.SpkFlowConfigSnapshotRespVO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.flowconfig.SpkSkillConfigDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdactivity.SpkIpdActivityDefDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdProcessProfileDO;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdactivity.SpkIpdActivityDefMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness.SpkIpdProcessProfileMapper;
import cn.iocoder.yudao.module.spkdelivery.service.delivery.DeliveryPathResolver;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
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
 * <p>skill 目录扫描 skillsRoot（经 {@link SpkSkillConfigService} 读 DB 单行配置，前端可改）下各 env 子目录，
 * 按 env 分组返回含 SKILL.md 的 skill 列表（D3：对齐派发层 resolveSkillPath 三段式解析）。
 *
 * @author SPK-OS
 */
@Slf4j
@Service
public class SpkFlowConfigService {

    /** skill 目录扫描时读 frontmatter 的最大行数（name/description 在头几行） */
    private static final int FRONTMATTER_SCAN_LINES = 12;

    @Resource
    private SpkIpdProcessProfileMapper profileMapper;
    @Resource
    private SpkIpdActivityDefMapper activityDefMapper;
    @Resource
    private SpkSkillConfigService skillConfigService;

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
        // skill 环境配置（D3：路径+默认 env 从 DB 读，envCatalog 按 env 分组扫描）
        SpkSkillConfigDO cfg = skillConfigService.getConfig();
        resp.setSkillsRoot(cfg.getSkillsRoot());
        resp.setDefaultSkillEnv(cfg.getDefaultEnv());
        resp.setEnvCatalog(scanEnvCatalog(cfg.getSkillsRoot()));
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

    /**
     * 改 skill 环境配置（skillsRoot / defaultEnv）。委托 {@link SpkSkillConfigService}（D2/D3）。
     * 单行 upsert，写后派发层 route 下次读 DB 即取新值（即时生效）。
     * 任一参数 null/空表示不改（保留原值）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateSkillConfig(String skillsRoot, String defaultEnv) {
        skillConfigService.updateConfig(skillsRoot, defaultEnv);
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
     * 扫描 skillsRoot 下各 env 子目录（default/test/commercial-release/prototype-release 等），
     * 每个 env 下列出含 SKILL.md 的 skill 目录，读 frontmatter 的 name/description。
     * 返回 env → [{name, description, dir}] 的有序 Map（env 名升序）。
     * <p>扫描失败/路径不存在返回空 Map，不阻断前端渲染（对齐派发层 resolveSkillPath 失败回退不阻断）。
     * <p>对齐派发层 {@code SpkTaskRouterService.resolveSkillPath} 的 {@code {skillsRoot}/{env}/{skillName}/SKILL.md}
     * 三段式解析——前端据 envCatalog[env] 动态列下拉 + 复刻三级回退命中状态。
     */
    private Map<String, List<Map<String, Object>>> scanEnvCatalog(String skillsRoot) {
        Map<String, List<Map<String, Object>>> catalog = new TreeMap<>();
        if (skillsRoot == null || skillsRoot.isBlank()) {
            return catalog;
        }
        Path root = Paths.get(skillsRoot);
        if (!Files.isDirectory(root)) {
            log.warn("[scanEnvCatalog][skillsRoot={} 非目录，返回空 catalog]", skillsRoot);
            return catalog;
        }
        try (var envStream = Files.list(root)) {
            // env 子目录按名排序
            List<Path> envDirs = new ArrayList<>();
            envStream.filter(Files::isDirectory).forEach(envDirs::add);
            envDirs.sort(java.util.Comparator.comparing(p -> p.getFileName().toString()));
            for (Path envDir : envDirs) {
                String env = envDir.getFileName().toString();
                List<Map<String, Object>> skills = new ArrayList<>();
                try (var skillStream = Files.list(envDir)) {
                    List<Path> skillDirs = new ArrayList<>();
                    skillStream.filter(Files::isDirectory).forEach(skillDirs::add);
                    skillDirs.sort(java.util.Comparator.comparing(p -> p.getFileName().toString()));
                    for (Path skillDir : skillDirs) {
                        Path skillMd = skillDir.resolve("SKILL.md");
                        if (!Files.isRegularFile(skillMd)) {
                            continue; // 跳过 README/非 skill 目录
                        }
                        Map<String, Object> info = new LinkedHashMap<>();
                        info.put("dir", skillDir.getFileName().toString());
                        info.put("name", skillDir.getFileName().toString());
                        info.put("description", "");
                        readFrontmatter(skillMd, info);
                        skills.add(info);
                    }
                } catch (Exception e) {
                    log.warn("[scanEnvCatalog][env={} 扫描失败：{}]", env, e.getMessage());
                }
                catalog.put(env, skills);
            }
        } catch (Exception e) {
            log.warn("[scanEnvCatalog][扫描 {} 失败：{}]", skillsRoot, e.getMessage());
        }
        return catalog;
    }

    /**
     * 读 SKILL.md frontmatter 的 name/description（前 N 行，简单行解析，不引 YAML 库）。
     * frontmatter 在 `---` 围栏内，name/description 各一行；读到第二个 `---` 或超行即止。
     * 解析失败保留 dir 名作 name，description 留空。
     */
    private void readFrontmatter(Path skillMd, Map<String, Object> info) {
        try (BufferedReader reader = Files.newBufferedReader(skillMd, StandardCharsets.UTF_8)) {
            boolean inFm = false;
            int lineNo = 0;
            String line;
            while ((line = reader.readLine()) != null && lineNo < FRONTMATTER_SCAN_LINES) {
                lineNo++;
                String trimmed = line.trim();
                if ("---".equals(trimmed)) {
                    if (inFm) {
                        break; // 围栏结束
                    }
                    inFm = true;
                    continue;
                }
                if (!inFm) {
                    continue;
                }
                if (trimmed.startsWith("name:")) {
                    String v = trimmed.substring(5).trim();
                    if (!v.isEmpty() && !"name".equals(info.get("name"))) {
                        info.put("name", v);
                    }
                } else if (trimmed.startsWith("description:")) {
                    String v = trimmed.substring(12).trim();
                    if (!v.isEmpty()) {
                        info.put("description", v);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[readFrontmatter][{} 读取失败：{}]", skillMd, e.getMessage());
        }
    }
}
