package cn.iocoder.yudao.module.spkdelivery.service.aegis;

import cn.iocoder.yudao.module.ai.controller.admin.chat.vo.conversation.AiChatConversationCreateMyReqVO;
import cn.iocoder.yudao.module.ai.controller.admin.chat.vo.message.AiChatMessageSendReqVO;
import cn.iocoder.yudao.module.ai.controller.admin.chat.vo.message.AiChatMessageSendRespVO;
import cn.iocoder.yudao.module.ai.service.chat.AiChatConversationService;
import cn.iocoder.yudao.module.ai.service.chat.AiChatMessageService;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.agent.SpkAgentTaskDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.aegis.SpkAegisReviewDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.gate.SpkGateRecordDO;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.aegis.SpkAegisReviewMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.agent.SpkAgentTaskMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.gate.SpkGateRecordMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SPK-OS Aegis 审查服务
 * <p>
 * ADCP 前同步调用：聚合 G1-G6 门禁报告 + agent 产物 → 经 yudao-module-ai aegis-reviewer 角色
 * 做 LLM 裁决 → 落 spk_aegis_review。LLM 不可用时降级为人工经 /spk/aegis/review 出结论。
 *
 * @author SPK-OS
 */
@Slf4j
@Service
@Validated
public class SpkAegisReviewService {

    @Value("${spk-delivery.aegis.reviewer-role-id:0}")
    private Long reviewerRoleId;
    @Value("${spk-delivery.agent.system-user-id:1}")
    private Long systemUserId;

    @Resource
    private SpkAegisReviewMapper aegisReviewMapper;
    @Resource
    private SpkGateRecordMapper gateRecordMapper;
    @Resource
    private SpkAgentTaskMapper agentTaskMapper;
    @Resource
    private AiChatMessageService chatMessageService;
    @Resource
    private AiChatConversationService chatConversationService;

    private final Map<String, Long> conversationCache = new ConcurrentHashMap<>();

    /**
     * 同步执行 Aegis 审查（由 SpkAegisReviewDelegate 或 /spk/aegis/review 调用）
     */
    public SpkAegisReviewDO review(String instanceId, String nodeKey) {
        // 1. 聚合证据：G1-G6 门禁报告 + agent 产物
        List<SpkGateRecordDO> gates = gateRecordMapper.selectListByInstanceId(instanceId);
        List<SpkAgentTaskDO> tasks = agentTaskMapper.selectListByInstanceId(instanceId);
        StringBuilder evidence = new StringBuilder();
        evidence.append("## 门禁报告\n");
        gates.forEach(g -> evidence.append("- ").append(g.getGate()).append(": pass=").append(g.getPass())
                .append(" report=").append(g.getReport()).append('\n'));
        evidence.append("\n## Agent 产物\n");
        tasks.forEach(t -> evidence.append("- ").append(t.getNodeKey()).append(": ").append(t.getResult()).append('\n'));
        // 2. LLM 裁决（reviewer 角色未配置则跳过，降级 conditional）
        String verdict;
        String report;
        if (reviewerRoleId != null && reviewerRoleId > 0) {
            report = callReviewer(evidence.toString(), instanceId);
            verdict = extractVerdict(report);
        } else {
            report = evidence.toString();
            verdict = "conditional";
            log.warn("[review][未配置 aegis reviewer-role-id，降级 conditional verdict]");
        }
        // 3. 落库
        SpkAegisReviewDO review = SpkAegisReviewDO.builder()
                .reviewId(UUID.randomUUID().toString())
                .instanceId(instanceId)
                .nodeKey(nodeKey)
                .verdict(verdict)
                .report(report)
                .evidence(evidence.toString())
                .build();
        aegisReviewMapper.insert(review);
        log.info("[review][instanceId={} verdict={}]", instanceId, verdict);
        return review;
    }

    private String callReviewer(String evidence, String instanceId) {
        Long conversationId = conversationCache.computeIfAbsent(instanceId, k -> {
            AiChatConversationCreateMyReqVO vo = new AiChatConversationCreateMyReqVO();
            vo.setRoleId(reviewerRoleId);
            return chatConversationService.createChatConversationMy(vo, systemUserId);
        });
        AiChatMessageSendReqVO req = new AiChatMessageSendReqVO();
        req.setConversationId(conversationId);
        req.setContent("请对以下 IPD 阶段证据做 Aegis 审查，输出 verdict(pass/fail/conditional) 与报告：\n" + evidence);
        req.setUseContext(Boolean.TRUE);
        AiChatMessageSendRespVO resp = chatMessageService.sendMessage(req, systemUserId);
        return resp != null && resp.getReceive() != null ? resp.getReceive().getContent() : null;
    }

    private String extractVerdict(String report) {
        if (report == null) {
            return "conditional";
        }
        String lower = report.toLowerCase();
        if (lower.contains("pass") && !lower.contains("fail")) {
            return "pass";
        }
        if (lower.contains("fail")) {
            return "fail";
        }
        return "conditional";
    }

    /**
     * 人工复核（LLM 不可用时经 /spk/aegis/review 由人出结论）
     */
    public SpkAegisReviewDO manualReview(String instanceId, String nodeKey, String verdict, String report) {
        SpkAegisReviewDO review = SpkAegisReviewDO.builder()
                .reviewId(UUID.randomUUID().toString())
                .instanceId(instanceId)
                .nodeKey(nodeKey)
                .verdict(verdict)
                .report(report)
                .evidence("manual")
                .build();
        aegisReviewMapper.insert(review);
        return review;
    }

    /**
     * 按流程实例查询 Aegis 审查结论（用于详情页 IPD 产物 tab，取最近一条）
     */
    public SpkAegisReviewDO getByInstanceId(String instanceId) {
        return aegisReviewMapper.selectByInstanceId(instanceId);
    }

}
