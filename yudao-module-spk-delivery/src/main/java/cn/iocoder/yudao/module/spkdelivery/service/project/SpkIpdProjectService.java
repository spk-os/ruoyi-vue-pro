package cn.iocoder.yudao.module.spkdelivery.service.project;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.taskcontract.SpkTaskContractDO;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.taskcontract.SpkTaskContractMapper;
import cn.iocoder.yudao.module.spkdelivery.service.cockpit.SpkIpdCockpitService;
import cn.iocoder.yudao.module.spkdelivery.service.delivery.DeliveryPathResolver;
import cn.iocoder.yudao.module.spkdelivery.service.delivery.FlowStateWriter;
import cn.iocoder.yudao.module.spkdelivery.service.integration.SpkGiteaIntegrationService;
import cn.iocoder.yudao.module.spkdelivery.service.integration.SpkPlaneIntegrationService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.HistoryService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.module.spkdelivery.service.ipdbusiness.SpkIpdBusinessConstants.FLOW_FULL_RELEASE;
import static cn.iocoder.yudao.module.spkdelivery.service.ipdbusiness.SpkIpdBusinessConstants.flowKeyOf;

/**
 * SPK-OS Cortext-IPD 项目服务
 * <p>
 * 项目的"身份证"是 businessKey（如 ipd-2026-0042）；运行态载体是 BPM 流程实例 processInstanceId。
 * 本服务提供：发起 IPD 主流程、查项目总览（复用 Cockpit 泳道）、查各阶段进度、
 * 以及代理 Plane/Gitea/Omnigent 集成产物的只读视图（设计文档 §4 集成层）。
 *
 * @author SPK-OS
 */
@Slf4j
@Service
@Validated
public class SpkIpdProjectService {

    /**
     * IPD 主流程定义 key。D1：改经 flowKeyOf(FULL_RELEASE) 解析为 spkIpdFlowFull（修 G1/G8）。
     * 项目级入口默认发起全量发布流程；保留旧 spkIpdFlow key 仅用于历史实例查询兼容。
     */
    public static final String IPD_FLOW_KEY = flowKeyOf(FLOW_FULL_RELEASE);

    /** 六阶段固定顺序（与 spk_ipd_activity_def.stage 枚举对齐） */
    private static final List<String> STAGE_ORDER = Arrays.asList(
            "concept", "plan", "develop", "qualify", "launch", "lifecycle");

    @Resource
    private BpmProcessInstanceApi processInstanceApi;
    @Resource
    private SpkTaskContractMapper contractMapper;
    @Resource
    private SpkIpdCockpitService cockpitService;
    @Resource
    private SpkPlaneIntegrationService planeService;
    @Resource
    private SpkGiteaIntegrationService giteaService;
    @Resource
    private HistoryService historyService;
    @Resource
    private DeliveryPathResolver deliveryPathResolver;
    @Resource
    private FlowStateWriter flowStateWriter;
    // 默认 skill 环境（spk-delivery.skill.default-env）：startProject 未显式传 skillEnv 时归一到此值，
    // 让配置 default-env=test 在 e2e 真正生效（而非硬编码 default）。route 内 resolveSkillEnv 读流程变量解析。
    @org.springframework.beans.factory.annotation.Value("${spk-delivery.skill.default-env:default}")
    private String defaultSkillEnv;

    /**
     * 发起 IPD 主流程（同步）。
     * <p>
     * <b>为何能同步</b>：spk-ipd-flow.json 全部触发器已改 type=2（HTTP_CALLBACK）。{@code createProcessInstance}
     * 同步执行至第一个 wait state（CDCP 前的 concept 首个 receiveTask）即返回——serviceTask 触发器发 HTTP
     * 到 /run，/run 调 {@code dispatchActivityAsync} 立即返 dispatched（fire-and-forget），流程卡 receiveTask
     * 等 LLM 后台回调推进。实测 {@code createProcessInstance} ≈ 0.06s 返回（见 7ae82efb 轨迹）。
     * <p>
     * <b>历史</b>：type1 时代触发器同步跑 LLM，{@code createProcessInstance} 阻塞 ~3min 才到 CDCP 门，
     * 故曾改为异步发起 + businessKey 轮询（startRecords 内存态）。type2 根治后同步即可，免去内存态轮询
     * （startRecords 易因服务重启丢失 → 前端 not_found 卡死，且 6min 轮询纯冗余）。
     *
     * @param businessKey  项目业务 key（ipd-2026-0042）；为空则自动生成
     * @param projectName  项目名（写入流程变量，概念阶段 agent 可读）
     * @param payload      附加 payload（OR 池原始需求等，可空）
     * @param mode         运行模式 test/product（写入流程变量 spk_mode，route 内只读；
     *                      product=concept 阶段起执行真实交付动作；空=test）
     * @return {businessKey, processInstanceId, mode}
     */
    public Map<String, Object> start(String businessKey, String projectName, String payload, String mode) {
        return start(businessKey, projectName, payload, mode, null, null);
    }

    /**
     * 发起 IPD 主流程（同步），支持指定交付根目录。
     *
     * @param deliveryRoot 交付根目录（用户在 LaunchWizard 选择/修改）；空则按 Profile.defaultProjectRootPattern
     *                     渲染 {businessKey}。非空时过 {@link DeliveryPathResolver#sanitizeRoot} 安全校验，
     *                     违规则抛 {@code IPD_DELIVERY_ROOT_INVALID}（坑#路径注入：禁 .. 与越出 Delivery 根）。
     */
    public Map<String, Object> start(String businessKey, String projectName, String payload, String mode,
                                     String deliveryRoot) {
        return start(businessKey, projectName, payload, mode, deliveryRoot, null);
    }

    /**
     * 发起 IPD 主流程（同步），支持指定交付根目录 + skill 环境。
     *
     * @param skillEnv skill 环境（default/test/commercial-release/prototype-release，写入流程变量 spk_skill_env，
     *                 route 内 resolveSkillEnv 只读解析 → {skillsRoot}/{env}/{skillName}/SKILL.md；
     *                 空/null 归一为 default。与 spk_mode 正交：env 决定用哪套 skill 文件，mode 决定 prompt 轻量化）
     */
    public Map<String, Object> start(String businessKey, String projectName, String payload, String mode,
                                     String deliveryRoot, String skillEnv) {
        if (businessKey == null || businessKey.isBlank()) {
            businessKey = "ipd-" + System.currentTimeMillis();
        }
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId == null) {
            userId = 1L; // 兜底：system 用户（与 spk-delivery.self.system-user-id 对齐）
        }

        BpmProcessInstanceCreateReqDTO createReq = new BpmProcessInstanceCreateReqDTO();
        createReq.setProcessDefinitionKey(IPD_FLOW_KEY);
        createReq.setBusinessKey(businessKey);
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("businessKey", businessKey);
        variables.put("projectName", projectName);
        if (payload != null && !payload.isBlank()) {
            variables.put("projectPayload", payload);
        }
        // spk_mode：test=桩仅测试 / product=真实交付。route 内只读 getVariable（后台独立线程，不触写锁，
        // 遵守 [[flowable-sync-trigger-deadlock]] 铁律）。空/null 归一为 test。
        String normMode = (mode == null || mode.isBlank()) ? "test" : mode.trim().toLowerCase();
        variables.put("spk_mode", normMode);
        // spk_skill_env：决定 route 用哪套 skill 文件（default/test/commercial-release/prototype-release）。
        // 空/null 归一到配置 spk-delivery.skill.default-env（e2e 设 test 即生效），而非硬编码 default。
        // route 内 resolveSkillEnv 只读解析。
        String normEnv = (skillEnv == null || skillEnv.isBlank()) ? defaultSkillEnv : skillEnv.trim();
        variables.put("spk_skill_env", normEnv);
        createReq.setVariables(variables);

        // 同步发起：type2 后 ~0.06s 到首 receiveTask 即返 processInstanceId
        String processInstanceId = processInstanceApi.createProcessInstance(userId, createReq);

        // E：项目启动创建交付目录骨架（.flow/ asset/ src/ docs/ + project.yaml + manifest.json）
        // 失败降级记 warn 不阻断流程发起——目录是产物落盘前提，但流程仍可跑（DB 是关键路径，FS 是增强）。
        // deliveryRoot 非空时过安全校验（禁 .. 与越出 Delivery 根，坑#路径注入），违规则抛 IPD_DELIVERY_ROOT_INVALID。
        String root = null;
        try {
            root = (deliveryRoot != null && !deliveryRoot.isBlank())
                    ? deliveryPathResolver.sanitizeRoot(deliveryRoot)
                    : deliveryPathResolver.resolveProjectRoot(null, businessKey);
            flowStateWriter.provisionProject(businessKey, root, projectName, IPD_FLOW_KEY, normMode);
        } catch (IllegalArgumentException ie) {
            // 路径安全校验失败：拒绝发起（坑#路径注入铁律，不降级）
            throw cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception(
                    cn.iocoder.yudao.module.spkdelivery.enums.ErrorCodeConstants.IPD_DELIVERY_ROOT_INVALID);
        } catch (Exception e) {
            log.warn("[start][businessKey={} 交付目录初始化失败降级 root={}：{}]",
                    businessKey, root, e.getMessage());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessKey", businessKey);
        result.put("processInstanceId", processInstanceId);
        result.put("mode", normMode);
        result.put("deliveryRoot", root);
        log.info("[start][businessKey={} projectName={} mode={} userId={} processInstanceId={} deliveryRoot={}]",
                businessKey, projectName, normMode, userId, processInstanceId, root);
        return result;
    }

    /**
     * 项目总览：泳道 + 汇总（复用 Cockpit）。
     */
    public Map<String, Object> getProject(String processInstanceId) {
        Map<String, Object> swimlane = cockpitService.swimlane(processInstanceId);
        // 附加六阶段进度
        swimlane.put("phases", getPhases(processInstanceId));
        return swimlane;
    }

    /**
     * 最新 IPD 流程实例（按发起时间倒序）。
     * <p>
     * 供「IPD 项目」/「IPD 监控台」进入界面默认载入：查 spkIpdFlow 流程定义下 start_time 最新的实例，
     * 返回 processInstanceId / businessKey / projectName / startTime / running。
     * 无实例时返回空 map（前端按 not_found 处理，不阻塞页面）。
     */
    public Map<String, Object> getLatest() {
        List<HistoricProcessInstance> list = historyService.createHistoricProcessInstanceQuery()
                .processDefinitionKey(IPD_FLOW_KEY)
                .orderByProcessInstanceStartTime().desc()
                .listPage(0, 1);
        Map<String, Object> m = new LinkedHashMap<>();
        if (list == null || list.isEmpty()) {
            m.put("status", "not_found");
            return m;
        }
        HistoricProcessInstance pi = list.get(0);
        String pid = pi.getId();
        // projectName 是 start/intake 时写入的流程变量（act_ru_variable / act_hi_varinst）
        String projectName = null;
        try {
            List<HistoricVariableInstance> vars = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(pid).variableName("projectName").list();
            if (vars != null && !vars.isEmpty() && vars.get(0).getValue() != null) {
                projectName = String.valueOf(vars.get(0).getValue());
            }
        } catch (Exception e) {
            log.warn("[getLatest][读 projectName 变量失败 pid={} {}]", pid, e.getMessage());
        }
        m.put("status", "ok");
        m.put("processInstanceId", pid);
        m.put("businessKey", pi.getBusinessKey());
        m.put("projectName", projectName);
        m.put("startTime", pi.getStartTime() == null ? null
                : java.time.LocalDateTime.ofInstant(pi.getStartTime().toInstant(), java.time.ZoneId.systemDefault()).toString());
        m.put("running", pi.getEndTime() == null);
        return m;
    }

    /**
     * 各阶段进度：按 phase 聚合合同 done/running/failed/total，并给出阶段状态。
     */
    public List<Map<String, Object>> getPhases(String processInstanceId) {
        List<SpkTaskContractDO> contracts = contractMapper.selectListByProcessInstanceId(processInstanceId);
        // 按 nodeKey 去重：intervene rerun 生成新合同（同 nodeKey 新 activityRunId），只统计每个节点最新合同，
        // 否则已 rerun 恢复的节点仍计旧 failed 合同，导致阶段状态误报 blocked。
        Map<String, SpkTaskContractDO> latestByNode = new LinkedHashMap<>();
        for (SpkTaskContractDO c : contracts) {
            String key = c.getNodeKey() != null ? c.getNodeKey() : c.getActivityRunId();
            latestByNode.put(key, c); // 后者覆盖前者 = 最新（contracts 已按 queuedAt ASC）
        }
        Map<String, int[]> acc = new LinkedHashMap<>(); // phase -> [done,running,failed,total]
        for (SpkTaskContractDO c : latestByNode.values()) {
            String phase = c.getPhase() != null ? c.getPhase() : "unknown";
            int[] a = acc.computeIfAbsent(phase, k -> new int[4]);
            a[3]++;
            String s = c.getStatus() == null ? "" : c.getStatus().toLowerCase();
            if (s.contains("done") || s.contains("completed") || s.contains("pass")) {
                a[0]++;
            } else if (s.contains("run")) {
                a[1]++;
            } else if (s.contains("fail")) {
                a[2]++;
            }
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (String stage : STAGE_ORDER) {
            int[] a = acc.getOrDefault(stage, new int[4]);
            rows.add(phaseRow(stage, a));
        }
        // 未归类的 unknown 阶段尾巴
        if (acc.containsKey("unknown")) {
            rows.add(phaseRow("unknown", acc.get("unknown")));
        }
        return rows;
    }

    private Map<String, Object> phaseRow(String stage, int[] a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("phase", stage);
        m.put("total", a[3]);
        m.put("done", a[0]);
        m.put("running", a[1]);
        m.put("failed", a[2]);
        String status;
        if (a[3] == 0) {
            status = "pending";
        } else if (a[0] == a[3]) {
            status = "completed";
        } else if (a[2] > 0) {
            status = "blocked";
        } else if (a[1] > 0 || a[0] > 0) {
            status = "running";
        } else {
            status = "pending";
        }
        m.put("status", status);
        return m;
    }

    /**
     * Plane 需求代理（Dashboard 需求 Tab）：返回 project 下 issue 列表 JSON。
     */
    public String getRequirements(int limit) {
        return planeService.listIssues(limit);
    }

    /**
     * Gitea PR/CI 代理：返回最近 CI 运行状态 + repo URL。
     */
    public Map<String, Object> getGitPr() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("repoUrl", giteaService.getRepoUrl());
        m.put("ciStatus", giteaService.getCiRunStatus());
        return m;
    }

    /**
     * Gitea Release 代理（占位：返回 repo releases 页 URL；后端不拉列表以省流量）。
     */
    public Map<String, Object> getGitRelease() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("releasesUrl", giteaService.getRepoUrl() + "/releases");
        return m;
    }

}
