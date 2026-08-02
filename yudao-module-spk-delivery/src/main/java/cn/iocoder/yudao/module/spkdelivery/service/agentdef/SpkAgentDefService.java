package cn.iocoder.yudao.module.spkdelivery.service.agentdef;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.agentdef.vo.SpkAgentDefCreateReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.agentdef.vo.SpkAgentDefPageReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.agentdef.vo.SpkAgentDefStatusReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.agentdef.vo.SpkAgentDefUpdateReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.agentdef.vo.SpkAgentDefWakeRespVO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.agentdef.SpkAgentDefDO;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.agentdef.SpkAgentDefMapper;
import cn.iocoder.yudao.module.spkdelivery.enums.SpkAgentDefStatusEnum;
import cn.iocoder.yudao.module.spkdelivery.enums.SpkAgentRuntimeTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.spkdelivery.enums.ErrorCodeConstants.*;

/**
 * SPK-OS 智能体定义服务
 *
 * <p>对标 Paddock {@code /api/agents} 的纯本地 DB CRUD 部分（去掉 openclaw 副作用分支）。
 *
 * @author SPK-OS
 */
@Slf4j
@Service
@Validated
public class SpkAgentDefService {

    @Resource
    private SpkAgentDefMapper agentDefMapper;
    @Resource
    private SpkAgentDefWakeRunner wakeRunner;

    /**
     * 创建智能体
     */
    public SpkAgentDefDO create(SpkAgentDefCreateReqVO reqVO) {
        // 唯一性校验
        validateNameUnique(null, reqVO.getName());
        validateCodeUnique(null, reqVO.getCode());
        SpkAgentDefDO agent = SpkAgentDefDO.builder()
                .name(reqVO.getName())
                .code(reqVO.getCode())
                .role(reqVO.getRole())
                .sessionKey(reqVO.getSessionKey() != null ? reqVO.getSessionKey() : reqVO.getCode())
                .soulContent(reqVO.getSoulContent())
                .workingMemory(reqVO.getWorkingMemory())
                .status(reqVO.getStatus() != null ? reqVO.getStatus() : SpkAgentDefStatusEnum.OFFLINE.getLabel())
                .model(reqVO.getModel())
                .roleId(reqVO.getRoleId())
                .toolsConfig(reqVO.getToolsConfig())
                .config(reqVO.getConfig())
                .runtimeType(reqVO.getRuntimeType() != null ? reqVO.getRuntimeType()
                        : SpkAgentRuntimeTypeEnum.NATIVE.getType())
                .source(reqVO.getSource() != null ? reqVO.getSource() : "manual")
                .hidden(reqVO.getHidden() != null ? reqVO.getHidden() : 0)
                .lastActivity(reqVO.getLastActivity())
                .build();
        agentDefMapper.insert(agent);
        return agent;
    }

    /**
     * 更新智能体
     */
    public void update(SpkAgentDefUpdateReqVO reqVO) {
        SpkAgentDefDO agent = validateExists(reqVO.getId());
        validateNameUnique(reqVO.getId(), reqVO.getName());
        validateCodeUnique(reqVO.getId(), reqVO.getCode());
        SpkAgentDefDO update = SpkAgentDefDO.builder()
                .id(reqVO.getId())
                .name(reqVO.getName())
                .code(reqVO.getCode())
                .role(reqVO.getRole())
                .sessionKey(reqVO.getSessionKey() != null ? reqVO.getSessionKey() : reqVO.getCode())
                .soulContent(reqVO.getSoulContent())
                .workingMemory(reqVO.getWorkingMemory())
                .status(reqVO.getStatus())
                .model(reqVO.getModel())
                .roleId(reqVO.getRoleId())
                .toolsConfig(reqVO.getToolsConfig())
                .config(reqVO.getConfig())
                .runtimeType(reqVO.getRuntimeType())
                .source(reqVO.getSource())
                .hidden(reqVO.getHidden())
                .lastActivity(reqVO.getLastActivity())
                .build();
        // 保留既有会话绑定
        update.setConversationId(agent.getConversationId());
        agentDefMapper.updateById(update);
    }

    /**
     * 删除智能体
     */
    public void delete(Long id) {
        validateExists(id);
        agentDefMapper.deleteById(id);
    }

    /**
     * 切换隐藏
     */
    public void toggleHidden(Long id, Integer hidden) {
        validateExists(id);
        SpkAgentDefDO update = new SpkAgentDefDO();
        update.setId(id);
        update.setHidden(hidden);
        update.setLastSeen(LocalDateTime.now());
        agentDefMapper.updateById(update);
    }

    /**
     * 变更状态
     */
    public void changeStatus(SpkAgentDefStatusReqVO reqVO) {
        validateExists(reqVO.getId());
        SpkAgentDefDO update = new SpkAgentDefDO();
        update.setId(reqVO.getId());
        update.setStatus(reqVO.getStatus());
        update.setLastSeen(LocalDateTime.now());
        update.setLastActivity(reqVO.getLastActivity());
        agentDefMapper.updateById(update);
    }

    /**
     * 唤醒（单轮对话）—— 本地实现，不依赖 openclaw
     */
    @Transactional(rollbackFor = Exception.class)
    public SpkAgentDefWakeRespVO wake(Long id, String message) {
        SpkAgentDefDO agent = validateExists(id);
        // 1. 运行时类型校验
        if (!SpkAgentRuntimeTypeEnum.supportsWake(agent.getRuntimeType())) {
            throw exception(AGENT_DEF_RUNTIME_NOT_SUPPORT_WAKE);
        }
        // 2. 角色/会话校验
        if (agent.getRoleId() == null) {
            throw exception(AGENT_DEF_ROLE_REQUIRED);
        }
        // 3. 标记 busy 并落库
        SpkAgentDefDO busy = wakeRunner.touchBusy(agent, "wake: " + truncate(message, 100));
        agentDefMapper.updateById(SpkAgentDefDO.builder()
                .id(agent.getId())
                .status(busy.getStatus())
                .lastSeen(busy.getLastSeen())
                .lastActivity(busy.getLastActivity())
                .build());
        // 4. 调用内核
        SpkAgentDefWakeRunner.WakeResult result;
        try {
            result = wakeRunner.wake(agent, message);
        } catch (Exception e) {
            log.error("[wake][agentId={} fail]", agent.getId(), e);
            // 标记 error
            SpkAgentDefDO err = new SpkAgentDefDO();
            err.setId(agent.getId());
            err.setStatus(SpkAgentDefStatusEnum.ERROR.getLabel());
            err.setLastSeen(LocalDateTime.now());
            err.setLastActivity("wake 失败: " + truncate(e.getMessage(), 200));
            agentDefMapper.updateById(err);
            throw exception(AGENT_DEF_WAKE_FAIL);
        }
        // 5. 持久化 conversationId + idle
        SpkAgentDefDO done = wakeRunner.touchIdle(agent, "wake 完成");
        done.setConversationId(result.conversationId());
        agentDefMapper.updateById(done);

        SpkAgentDefWakeRespVO resp = new SpkAgentDefWakeRespVO();
        resp.setId(agent.getId());
        resp.setName(agent.getName());
        resp.setConversationId(result.conversationId());
        resp.setContent(result.content());
        resp.setStatus(result.status());
        return resp;
    }

    public SpkAgentDefDO get(Long id) {
        return agentDefMapper.selectById(id);
    }

    public PageResult<SpkAgentDefDO> getPage(SpkAgentDefPageReqVO reqVO) {
        return agentDefMapper.selectPage(reqVO);
    }

    public List<SpkAgentDefDO> getList() {
        return agentDefMapper.selectListByHidden(0);
    }

    // ---------- 校验 ----------

    public SpkAgentDefDO validateExists(Long id) {
        if (id == null) {
            throw exception(AGENT_DEF_NOT_EXISTS);
        }
        SpkAgentDefDO agent = agentDefMapper.selectById(id);
        if (agent == null) {
            throw exception(AGENT_DEF_NOT_EXISTS);
        }
        return agent;
    }

    private void validateNameUnique(Long id, String name) {
        SpkAgentDefDO existing = agentDefMapper.selectByName(name);
        if (existing != null && !existing.getId().equals(id)) {
            throw exception(AGENT_DEF_NAME_DUPLICATE);
        }
    }

    private void validateCodeUnique(Long id, String code) {
        SpkAgentDefDO existing = agentDefMapper.selectByCode(code);
        if (existing != null && !existing.getId().equals(id)) {
            throw exception(AGENT_DEF_CODE_DUPLICATE);
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

}
