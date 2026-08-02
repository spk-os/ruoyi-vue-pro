package cn.iocoder.yudao.module.spkdelivery.service.gate;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import cn.iocoder.yudao.module.bpm.api.task.BpmProcessTaskApi;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.gate.SpkGateRecordDO;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.gate.SpkGateRecordMapper;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.spkdelivery.enums.ErrorCodeConstants.GATE_CALLBACK_NODE_NOT_FOUND;
import static cn.iocoder.yudao.module.spkdelivery.enums.ErrorCodeConstants.GATE_CALLBACK_SIGNATURE_INVALID;

/**
 * SPK-OS 门禁服务
 * <p>
 * G1-G8 / TR2-6 门禁由 BPM HTTP_CALLBACK 触发器发起：
 * <ul>
 *   <li>触发器先 HTTP POST 到 {@code /spk/gate/dispatch}（本服务仅应答 200，落"已派发"审计，
 *       不推进流程——避免与触发器事务竞态；生产环境此处转发 Gitea Actions）。</li>
 *   <li>流程卡在触发器自动生成的 receiveTask，等待 CI 末步回调 {@code /spk/gate/callback}：
 *       写 {@code <gate>_report/<gate>_pass} 变量 + triggerTask 推进。</li>
 * </ul>
 *
 * @author SPK-OS
 */
@Slf4j
@Service
@Validated
public class SpkGateService {

    /** 门禁回调共享密钥（与 Gitea Actions secret 对齐） */
    @Value("${spk-delivery.gate.callback-secret:spk-os-gate-secret}")
    private String callbackSecret;

    @Resource
    private SpkGateRecordMapper gateRecordMapper;
    @Resource
    private RuntimeService runtimeService;
    @Resource
    private BpmProcessTaskApi processTaskApi;

    /**
     * 门禁派发应答（HTTP_CALLBACK 触发器调用）。
     * <p>
     * 仅落"已派发"审计并返回，不推进流程——receiveTask 由后续 /callback 推进。
     * 生产环境此处应转发至 Gitea Actions workflow_dispatch，CI 完成后回调 /callback。
     *
     * @param token          共享密钥
     * @param instanceId     流程实例编号
     * @param gate           门禁标识 g1..g8 / tr2..tr6
     * @param taskDefineKey  触发器自动传入的 receiveTask key（= callbackTaskDefineKey）
     * @param report         派发附带上文（可选）
     */
    public void onDispatch(String token, String instanceId, String gate, String taskDefineKey, String report) {
        // /dispatch 由 BPM HTTP_CALLBACK 触发器在本机回环调用（可信内部调用，无法携带 X-Spk-Token），
        // 故不校验签名；签名校验仅限 /callback（外部 Gitea Actions CI 回调）。
        SpkGateRecordDO record = SpkGateRecordDO.builder()
                .instanceId(instanceId)
                .gate(gate)
                .nodeKey(taskDefineKey)
                .report(report)
                .pass(null) // 已派发，待回调结论
                .callbackTime(null)
                .build();
        gateRecordMapper.insert(record);
        log.info("[onDispatch][instanceId={} gate={} taskDefineKey={} 已派发，等待 CI 回调]", instanceId, gate, taskDefineKey);
    }

    /**
     * 处理门禁回调（CI 完成后回写门禁结论并推进流程）。
     *
     * @param token      共享密钥
     * @param instanceId 流程实例编号
     * @param gate       门禁标识 g1..g8 / tr2..tr6
     * @param nodeKey    BPM 节点 key（receiveTask id）；为空则取当前活动 id 自动解析
     * @param report     门禁报告（JSON）
     * @param pass       是否通过
     */
    public void onCallback(String token, String instanceId, String gate, String nodeKey, String report, Boolean pass) {        if (callbackSecret != null && !callbackSecret.isEmpty() && !callbackSecret.equals(token)) {
            throw exception(GATE_CALLBACK_SIGNATURE_INVALID);
        }
        // 1. 解析 receiveTask key：未传则取当前活动 id（线性流程下即 receiveTask）
        String receiveTaskKey = nodeKey;
        if (receiveTaskKey == null || receiveTaskKey.isEmpty()) {
            List<String> activeIds = runtimeService.getActiveActivityIds(instanceId);
            if (activeIds == null || activeIds.isEmpty()) {
                throw exception(GATE_CALLBACK_NODE_NOT_FOUND);
            }
            receiveTaskKey = activeIds.get(0);
        }
        // 2. 更新最近一条派发记录的结论（无则补插一条审计）
        SpkGateRecordDO latest = gateRecordMapper.selectByInstanceIdAndGate(instanceId, gate);
        if (latest != null && latest.getPass() == null) {
            latest.setNodeKey(receiveTaskKey);
            latest.setReport(report);
            latest.setPass(pass);
            latest.setCallbackTime(LocalDateTime.now());
            gateRecordMapper.updateById(latest);
        } else {
            gateRecordMapper.insert(SpkGateRecordDO.builder()
                    .instanceId(instanceId).gate(gate).nodeKey(receiveTaskKey)
                    .report(report).pass(pass).callbackTime(LocalDateTime.now()).build());
        }
        // 3. 写流程变量：<gate>_report / <gate>_pass
        Map<String, Object> vars = new HashMap<>();
        vars.put(gate + "_report", report);
        vars.put(gate + "_pass", pass);
        runtimeService.setVariables(instanceId, vars);
        // 4. trigger receiveTask 推进
        processTaskApi.triggerTask(instanceId, receiveTaskKey);
        log.info("[onCallback][instanceId={} gate={} pass={} nodeKey={}]", instanceId, gate, pass, receiveTaskKey);
    }

    /**
     * 按流程实例查询全部门禁记录（用于详情页 IPD 产物 tab）
     */
    public List<SpkGateRecordDO> getListByInstanceId(String instanceId) {
        return gateRecordMapper.selectListByInstanceId(instanceId);
    }

}
