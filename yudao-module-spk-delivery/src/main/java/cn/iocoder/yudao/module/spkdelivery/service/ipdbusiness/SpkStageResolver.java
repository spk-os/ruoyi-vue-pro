package cn.iocoder.yudao.module.spkdelivery.service.ipdbusiness;

import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.taskcontract.SpkTaskContractDO;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.taskcontract.SpkTaskContractMapper;
import cn.iocoder.yudao.module.spkdelivery.enums.SpkActivityStageEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

import static cn.iocoder.yudao.module.spkdelivery.service.ipdbusiness.SpkIpdBusinessConstants.STATUS_COMPLETED;

/**
 * SPK-OS IPD 当前阶段/活动实时解析器。
 * <p>
 * 历史遗留：FlowRun DO 的 {@code current_stage} 仅在 {@code start()} 置 "concept"，全仓无任何推进/终态
 * 回写点（{@code setCurrentStage} 仅 start 一处），导致 current_stage 永远停在 concept（含已 COMPLETED
 * 的运行），驾驶舱阶段流水线全失真。{@code current_activity} 同病（永为 null）。
 * <p>
 * 本解析器从 {@code task_contract}（按 queuedAt 升序）取最末（最新入队）活动的 {@code phase}/
 * {@code activityId} 实时覆盖 DO 静态值；终态（COMPLETED）回退 lifecycle。纯读 task_contract，
 * 不碰 BPM/Flowable 引擎语义（硬约束），无写入风险。与 {@code stageTimestamps}、{@code swimlane} 同源。
 *
 * @author SPK-OS
 */
@Component
public class SpkStageResolver {

    @Resource
    private SpkTaskContractMapper taskContractMapper;

    /**
     * 实时解析当前阶段。
     * <ul>
     *   <li>COMPLETED → lifecycle（终态，无论合同最末 phase）。</li>
     *   <li>pid 为空或无合同 → fallback（DO 静态值，未 start 时为 concept/null）。</li>
     *   <li>否则取最末（最新 queuedAt）合同的 phase；跳过 phase 为空的合同。</li>
     * </ul>
     */
    public String resolveCurrentStage(String processInstanceId, String fallback, String flowRunStatus) {
        if (STATUS_COMPLETED.equals(flowRunStatus)) {
            return SpkActivityStageEnum.LIFECYCLE.getCode();
        }
        if (processInstanceId == null || processInstanceId.isBlank()) {
            return fallback;
        }
        List<SpkTaskContractDO> contracts = taskContractMapper.selectListByProcessInstanceId(processInstanceId);
        if (contracts == null || contracts.isEmpty()) {
            return fallback;
        }
        // selectListByProcessInstanceId 按 queuedAt 升序，倒序找首个有 phase 的合同即最新阶段
        for (int i = contracts.size() - 1; i >= 0; i--) {
            String phase = contracts.get(i).getPhase();
            if (phase != null && !phase.isBlank()) {
                return phase;
            }
        }
        return fallback;
    }

    /**
     * 实时解析当前活动（activityId 优先，回退 nodeKey）。pid 为空或无合同 → fallback。
     */
    public String resolveCurrentActivity(String processInstanceId, String fallback) {
        if (processInstanceId == null || processInstanceId.isBlank()) {
            return fallback;
        }
        List<SpkTaskContractDO> contracts = taskContractMapper.selectListByProcessInstanceId(processInstanceId);
        if (contracts == null || contracts.isEmpty()) {
            return fallback;
        }
        for (int i = contracts.size() - 1; i >= 0; i--) {
            SpkTaskContractDO c = contracts.get(i);
            if (c.getActivityId() != null && !c.getActivityId().isBlank()) {
                return c.getActivityId();
            }
            if (c.getNodeKey() != null && !c.getNodeKey().isBlank()) {
                return c.getNodeKey();
            }
        }
        return fallback;
    }
}
