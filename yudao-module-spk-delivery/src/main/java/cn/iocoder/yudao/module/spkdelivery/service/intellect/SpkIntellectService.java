package cn.iocoder.yudao.module.spkdelivery.service.intellect;

import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.intellect.SpkIntellectQueueDO;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.intellect.SpkIntellectQueueMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.spkdelivery.enums.ErrorCodeConstants.INTELLECT_REQ_NOT_EXISTS;
import static cn.iocoder.yudao.module.spkdelivery.service.ipdbusiness.SpkIpdBusinessConstants.FLOW_FULL_RELEASE;
import static cn.iocoder.yudao.module.spkdelivery.service.ipdbusiness.SpkIpdBusinessConstants.flowKeyOf;

/**
 * SPK-OS OR 池需求队列服务
 *
 * @author SPK-OS
 */
@Slf4j
@Service
@Validated
public class SpkIntellectService {

    /**
     * 修 G8：此前硬编码 "spk-ipd-flow"（连字符）与部署 key spkIpdFlow（驼峰）/新 key spkIpdFlowFull 不匹配，
     * 该入口启动的实例永不被 FinishListener 捕获。统一经 flowKeyOf(FULL_RELEASE) 解析。
     */
    public static final String IPD_FLOW_KEY = flowKeyOf(FLOW_FULL_RELEASE);

    @Resource
    private SpkIntellectQueueMapper intellectQueueMapper;
    @Resource
    private BpmProcessInstanceApi processInstanceApi;

    public List<SpkIntellectQueueDO> getList(String status) {
        return intellectQueueMapper.selectListByStatus(status);
    }

    /**
     * 从 OR 池取出一个需求并发起 IPD 流程
     */
    public String pick(Long id, Long userId) {
        SpkIntellectQueueDO req = intellectQueueMapper.selectById(id);
        if (req == null) {
            throw exception(INTELLECT_REQ_NOT_EXISTS);
        }
        // 1. 标记处理中
        req.setStatus("processing");
        intellectQueueMapper.updateById(req);
        // 2. 发起 IPD 主流程
        BpmProcessInstanceCreateReqDTO createReq = new BpmProcessInstanceCreateReqDTO();
        createReq.setProcessDefinitionKey(IPD_FLOW_KEY);
        createReq.setBusinessKey("intellect-" + req.getReqId());
        Map<String, Object> variables = new HashMap<>();
        variables.put("intellectReqId", req.getReqId());
        variables.put("intellectPayload", req.getPayload());
        createReq.setVariables(variables);
        String instanceId = processInstanceApi.createProcessInstance(userId, createReq);
        req.setStatus("picked");
        intellectQueueMapper.updateById(req);
        log.info("[pick][reqId={} instanceId={} 已发起 IPD 流程]", req.getReqId(), instanceId);
        return instanceId;
    }

    public SpkIntellectQueueDO register(String source, String reqId, String payload) {
        SpkIntellectQueueDO req = SpkIntellectQueueDO.builder()
                .source(source)
                .reqId(reqId)
                .status("pending")
                .dedupHash(reqId == null ? null : Integer.toHexString(reqId.hashCode()))
                .payload(payload)
                .build();
        intellectQueueMapper.insert(req);
        return req;
    }

}
