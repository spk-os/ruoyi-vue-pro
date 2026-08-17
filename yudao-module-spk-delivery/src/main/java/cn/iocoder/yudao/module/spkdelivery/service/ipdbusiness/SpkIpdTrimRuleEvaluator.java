package cn.iocoder.yudao.module.spkdelivery.service.ipdbusiness;

import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdFlowRunDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdProcessProfileVersionDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdTrimRuleDO;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness.SpkIpdFlowRunMapper;
import cn.iocoder.yudao.module.spkdelivery.enums.TrimAction;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * IPD 裁剪规则运行时评估器（D3）。
 * <p>
 * 在触发器/任务派发入口（/spk/agent-task/run、/spk/gate/dispatch、/spk/aegis/review）被调用，
 * 按 processInstanceId→FlowRun→ProfileVersion→TrimRules 链路解析当前活动应如何处理。
 * 修 G6：此前 TrimRule 是死数据，执行层从不读它；本评估器让治理层配置在运行时真实生效。
 *
 * <p>匹配优先级：activityDefId 级规则（最具体）&gt; stage 级规则（activityDefId 为空）。
 * FlowRun.profileVersion 存的是版本号（Integer），需反查 ProfileVersion 的 id（Long）才能查裁剪规则。
 *
 * @author SPK-OS
 */
@Slf4j
@Service
public class SpkIpdTrimRuleEvaluator {

    @Resource
    private SpkIpdFlowRunMapper flowRunMapper;
    @Resource
    private SpkIpdProcessProfileService processProfileService;

    /**
     * 评估某活动的裁剪决策。
     *
     * @param processInstanceId Flowable 实例 id（触发器入口从请求上下文取）
     * @param stage             IPD 阶段（concept/plan/develop/qualify/launch/lifecycle/support；可空）
     * @param activityDefId     活动/BPM 节点 id（可空；触发器端点常以 nodeKey/activityId 传入）
     */
    public TrimDecision evaluate(String processInstanceId, String stage, String activityDefId) {
        if (processInstanceId == null || processInstanceId.isBlank()) {
            return TrimDecision.through();
        }
        SpkIpdFlowRunDO run = flowRunMapper.selectByProcessInstanceId(processInstanceId);
        if (run == null || run.getProfileId() == null || run.getProfileId() <= 0) {
            // 降级档案（profileId=0，未接入治理）无裁剪规则，正常放行
            return TrimDecision.through();
        }
        Long versionId = resolveProfileVersionId(run);
        if (versionId == null) {
            return TrimDecision.through();
        }
        List<SpkIpdTrimRuleDO> rules = processProfileService.listTrimRules(versionId);
        if (rules == null || rules.isEmpty()) {
            return TrimDecision.through();
        }
        // 优先级 1：activityDefId 级规则（最具体，stage 不参与过滤——触发器端点常只持 nodeKey）
        if (activityDefId != null && !activityDefId.isBlank()) {
            for (SpkIpdTrimRuleDO r : rules) {
                if (activityDefId.equals(r.getActivityDefId())) {
                    return toDecision(r);
                }
            }
        }
        // 优先级 2：stage 级规则（activityDefId 为空、stage 匹配）
        if (stage != null && !stage.isBlank()) {
            for (SpkIpdTrimRuleDO r : rules) {
                String rid = r.getActivityDefId();
                if ((rid == null || rid.isBlank()) && stage.equalsIgnoreCase(r.getStage())) {
                    return toDecision(r);
                }
            }
        }
        return TrimDecision.through();
    }

    private TrimDecision toDecision(SpkIpdTrimRuleDO r) {
        TrimAction action = TrimAction.of(r.getAction());
        if (action == null) {
            log.warn("[evaluate][规则 id={} action={} 非法，忽略放行]", r.getId(), r.getAction());
            return TrimDecision.through();
        }
        return new TrimDecision(action, r.getReason(), r.getId());
    }

    /** FlowRun.profileVersion 是版本号（Integer），反查 ProfileVersion 的 id（Long） */
    private Long resolveProfileVersionId(SpkIpdFlowRunDO run) {
        Integer versionNo = run.getProfileVersion();
        if (versionNo == null || versionNo == 0) {
            return null;
        }
        List<SpkIpdProcessProfileVersionDO> versions = processProfileService.listVersions(run.getProfileId());
        if (versions == null) {
            return null;
        }
        for (SpkIpdProcessProfileVersionDO v : versions) {
            if (versionNo.equals(v.getVersion())) {
                return v.getId();
            }
        }
        return null;
    }
}
