package cn.iocoder.yudao.module.spkdelivery.service.project;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.taskcontract.SpkTaskContractDO;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.taskcontract.SpkTaskContractMapper;
import cn.iocoder.yudao.module.spkdelivery.service.cockpit.SpkIpdCockpitService;
import cn.iocoder.yudao.module.spkdelivery.service.integration.SpkGiteaIntegrationService;
import cn.iocoder.yudao.module.spkdelivery.service.integration.SpkPlaneIntegrationService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    /** IPD 主流程定义 key（与 SpkIpdFlowDeployRunner / simpleModel 对齐） */
    public static final String IPD_FLOW_KEY = "spkIpdFlow";

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

    /**
     * 发起 IPD 主流程。
     *
     * @param businessKey  项目业务 key（ipd-2026-0042）；为空则自动生成
     * @param projectName  项目名（写入流程变量，概念阶段 agent 可读）
     * @param payload      附加 payload（OR 池原始需求等，可空）
     * @return {processInstanceId, businessKey}
     */
    public Map<String, Object> start(String businessKey, String projectName, String payload) {
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
        createReq.setVariables(variables);
        String instanceId = processInstanceApi.createProcessInstance(userId, createReq);
        log.info("[start][businessKey={} projectName={} userId={} instanceId={} 已发起 IPD 流程]",
                businessKey, projectName, userId, instanceId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("processInstanceId", instanceId);
        result.put("businessKey", businessKey);
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
     * 各阶段进度：按 phase 聚合合同 done/running/failed/total，并给出阶段状态。
     */
    public List<Map<String, Object>> getPhases(String processInstanceId) {
        List<SpkTaskContractDO> contracts = contractMapper.selectListByProcessInstanceId(processInstanceId);
        Map<String, int[]> acc = new LinkedHashMap<>(); // phase -> [done,running,failed,total]
        for (SpkTaskContractDO c : contracts) {
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
