package cn.iocoder.yudao.module.spkdelivery.service.ipdbusiness;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdCommandLogDO;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness.SpkIpdCommandLogMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.spkdelivery.enums.ErrorCodeConstants.IPD_COMMAND_CONFLICT;

/**
 * IPD 幂等命令审计服务。设计文档 §9.6 / §10.1。
 * <p>
 * 同租户同 idempotency_key + 同 payload 返回原结果；同 key 不同 payload 报冲突。
 * 启动、重试、取消、决策等写命令必须先经过本服务登记，再执行业务逻辑。
 *
 * @author SPK-OS
 */
@Slf4j
@Service
public class SpkIpdCommandService {

    @Resource
    private SpkIpdCommandLogMapper commandLogMapper;

    /**
     * 登记一条命令记录（PENDING）。若同 idempotencyKey 已存在：
     * <ul>
     *   <li>payloadHash 一致 → 返回既有记录（调用方应回放/返回原结果，不重复执行）</li>
     *   <li>payloadHash 不一致 → 抛 IPD_COMMAND_CONFLICT</li>
     * </ul>
     *
     * @return 既有（已存在）或新登记的命令记录；isNew=true 表示首次，需执行；isNew=false 表示命中幂等。
     */
    public CommandEnvelope enlist(String idempotencyKey, String commandType, String targetType,
                                   String targetId, String payloadJson) {
        SpkIpdCommandLogDO exist = commandLogMapper.selectByIdempotencyKey(idempotencyKey);
        if (exist != null) {
            String hash = hash(payloadJson);
            if (!hash.equals(exist.getPayloadHash())) {
                throw exception(IPD_COMMAND_CONFLICT);
            }
            return new CommandEnvelope(exist, false);
        }
        SpkIpdCommandLogDO log = SpkIpdCommandLogDO.builder()
                .idempotencyKey(idempotencyKey)
                .commandType(commandType)
                .targetType(targetType)
                .targetId(targetId)
                .payloadHash(hash(payloadJson))
                .payloadJson(payloadJson)
                .status("PENDING")
                .requestedBy(currentUserId())
                .requestedAt(LocalDateTime.now())
                .build();
        commandLogMapper.insert(log);
        return new CommandEnvelope(log, true);
    }

    public void markRunning(Long commandId) {
        updateStatus(commandId, "RUNNING", null, null, null, false);
    }

    public void markSuccess(Long commandId, String resultJson) {
        updateStatus(commandId, "SUCCESS", resultJson, null, null, true);
    }

    public void markFailed(Long commandId, String errorCode, String errorMsg) {
        updateStatus(commandId, "FAILED", null, errorCode, errorMsg, true);
    }

    private void updateStatus(Long commandId, String status, String resultJson,
                              String errorCode, String errorMsg, boolean finished) {
        SpkIpdCommandLogDO patch = new SpkIpdCommandLogDO();
        patch.setId(commandId);
        patch.setStatus(status);
        patch.setResultJson(resultJson);
        patch.setErrorCode(errorCode);
        patch.setErrorMsg(errorMsg);
        if (finished) {
            patch.setFinishedAt(LocalDateTime.now());
        }
        commandLogMapper.updateById(patch);
    }

    public SpkIpdCommandLogDO get(Long commandId) {
        return commandLogMapper.selectById(commandId);
    }

    private static String hash(String payload) {
        if (payload == null) {
            payload = "";
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(payload.hashCode());
        }
    }

    private Long currentUserId() {
        Long uid = SecurityFrameworkUtils.getLoginUserId();
        return uid != null ? uid : 1L;
    }

    /** 命令登记结果 */
    public record CommandEnvelope(SpkIpdCommandLogDO log, boolean isNew) {
        public Long commandId() {
            return log.getId();
        }
    }
}
