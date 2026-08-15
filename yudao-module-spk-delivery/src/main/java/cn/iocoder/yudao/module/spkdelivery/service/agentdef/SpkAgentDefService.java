package cn.iocoder.yudao.module.spkdelivery.service.agentdef;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.agentdef.vo.SpkAgentDefCreateReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.agentdef.vo.SpkAgentDefPageReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.agentdef.vo.SpkAgentDefStatusReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.agentdef.vo.SpkAgentDefUpdateReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.agentdef.vo.SpkAgentDefWakeRespVO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.agentdef.SpkAgentDefDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.agent.SpkAgentTaskDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.agentsquad.SpkAgentSquadDO;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.agent.SpkAgentTaskMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.agentdef.SpkAgentDefMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.agentsquad.SpkAgentSquadMapper;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.spkdelivery.enums.SpkAgentDefStatusEnum;
import cn.iocoder.yudao.module.spkdelivery.enums.SpkAgentRuntimeTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    @Resource
    private SpkAgentSquadMapper squadMapper;
    @Resource
    private SpkAgentTaskMapper agentTaskMapper;

    /**
     * 智能体管理 KPI 聚合（真实计数，不造假）。
     * <p>聚合 spk_agent_def 按运行时状态分布 + 编队数 + 任务执行数。
     * 现有状态枚举仅 offline/idle/busy/error；原型"注册/就绪/审批中/已退役"生命周期属后续扩展，
     * 此处如实按现有状态返回，未聚合的字段返回 0（真实 0，非冒充）。
     */
    public Map<String, Object> stats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        // 智能体按状态计数
        long total = 0;
        for (SpkAgentDefStatusEnum e : SpkAgentDefStatusEnum.values()) {
            long c = agentDefMapper.selectCount(new LambdaQueryWrapperX<SpkAgentDefDO>()
                    .eq(SpkAgentDefDO::getStatus, e.getLabel()));
            stats.put(e.getLabel(), c);
            total += c;
        }
        stats.put("totalAgents", total);
        // 编队数（启用/停用）
        long activeSquad = squadMapper.selectCount(new LambdaQueryWrapperX<SpkAgentSquadDO>()
                .eq(SpkAgentSquadDO::getStatus, "active"));
        long totalSquad = squadMapper.selectCount(null);
        stats.put("totalSquads", totalSquad);
        stats.put("activeSquads", activeSquad);
        // 任务执行数
        stats.put("totalTasks", agentTaskMapper.selectCount(null));
        return stats;
    }

    /**
     * 创建智能体
     */
    public SpkAgentDefDO create(SpkAgentDefCreateReqVO reqVO) {
        // 唯一性校验
        validateNameUnique(null, reqVO.getName());
        validateCodeUnique(null, reqVO.getCode());
        // 角色继承环检测：parentDefId 不能指自己或子孙
        validateParentChain(null, reqVO.getParentDefId());
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
                // 高级字段透传（mode 默认 local）
                .agentKind(reqVO.getAgentKind())
                .capabilityTags(reqVO.getCapabilityTags())
                .verifierType(reqVO.getVerifierType())
                .isolationLevel(reqVO.getIsolationLevel())
                .mode(reqVO.getMode() != null ? reqVO.getMode() : "local")
                .parentDefId(reqVO.getParentDefId())
                .omnigentAgentId(reqVO.getOmnigentAgentId())
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
        // 角色继承环检测：parentDefId 不能指自己或子孙
        validateParentChain(reqVO.getId(), reqVO.getParentDefId());
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
                // 高级字段透传（mode 默认 local）
                .agentKind(reqVO.getAgentKind())
                .capabilityTags(reqVO.getCapabilityTags())
                .verifierType(reqVO.getVerifierType())
                .isolationLevel(reqVO.getIsolationLevel())
                .mode(reqVO.getMode() != null ? reqVO.getMode() : "local")
                .parentDefId(reqVO.getParentDefId())
                .omnigentAgentId(reqVO.getOmnigentAgentId())
                .build();
        // 保留既有会话绑定
        update.setConversationId(agent.getConversationId());
        agentDefMapper.updateById(update);
    }

    /**
     * 角色继承解析：沿 parentDefId 链向上合并父智能体属性（运行时合并，不物化）。
     * <p>合并规则（子非空则覆盖父，capabilityTags 并集）：
     * <ul>
     *   <li>capabilityTags：并集（子扩展能力）</li>
     *   <li>toolsConfig：合并（子覆盖同名 key，简化为子覆盖父整体）</li>
     *   <li>soulContent/model/roleId/mode/omnigentAgentId/agentKind/verifierType：子非空覆盖父</li>
     * </ul>
     * 带环检测（visited Set），出现环直接返回当前 def（防死循环）。
     * 调用时机：SpkTaskRouterService 选出 lead 后对 lead 调用，得到 effective lead 装配派发参数。
     *
     * @param defId 智能体 id
     * @return 合并父链后的有效智能体（深拷贝，不改 DB）
     */
    public SpkAgentDefDO resolveEffective(Long defId) {
        if (defId == null) {
            return null;
        }
        SpkAgentDefDO def = agentDefMapper.selectById(defId);
        if (def == null) {
            return null;
        }
        // 无父继承直接返回
        if (def.getParentDefId() == null) {
            return def;
        }
        // 沿父链合并，带环检测
        java.util.Set<Long> visited = new java.util.HashSet<>();
        visited.add(def.getId());
        SpkAgentDefDO effective = cloneDef(def);
        Long parentId = def.getParentDefId();
        while (parentId != null && !visited.contains(parentId)) {
            visited.add(parentId);
            SpkAgentDefDO parent = agentDefMapper.selectById(parentId);
            if (parent == null) {
                break;
            }
            mergeFromParent(effective, parent);
            parentId = parent.getParentDefId();
        }
        return effective;
    }

    /** 浅拷贝 def（不改 DB，用于合并父链）。 */
    private SpkAgentDefDO cloneDef(SpkAgentDefDO src) {
        return SpkAgentDefDO.builder()
                .id(src.getId())
                .name(src.getName())
                .code(src.getCode())
                .role(src.getRole())
                .sessionKey(src.getSessionKey())
                .soulContent(src.getSoulContent())
                .workingMemory(src.getWorkingMemory())
                .status(src.getStatus())
                .model(src.getModel())
                .roleId(src.getRoleId())
                .conversationId(src.getConversationId())
                .toolsConfig(src.getToolsConfig())
                .config(src.getConfig())
                .runtimeType(src.getRuntimeType())
                .source(src.getSource())
                .hidden(src.getHidden())
                .lastActivity(src.getLastActivity())
                .agentKind(src.getAgentKind())
                .capabilityTags(src.getCapabilityTags())
                .verifierType(src.getVerifierType())
                .isolationLevel(src.getIsolationLevel())
                .mode(src.getMode())
                .parentDefId(src.getParentDefId())
                .omnigentAgentId(src.getOmnigentAgentId())
                .build();
    }

    /**
     * 把父属性合并进 effective（子非空则保留子，capabilityTags 取并集）。
     */
    @SuppressWarnings("unchecked")
    private void mergeFromParent(SpkAgentDefDO effective, SpkAgentDefDO parent) {
        // capabilityTags 并集
        java.util.List<String> merged = new java.util.ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (String tag : parseTagList(parent.getCapabilityTags())) {
            if (seen.add(tag)) merged.add(tag);
        }
        for (String tag : parseTagList(effective.getCapabilityTags())) {
            if (seen.add(tag)) merged.add(tag);
        }
        if (!merged.isEmpty()) {
            try {
                effective.setCapabilityTags(
                        cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(merged));
            } catch (Exception ignore) {
                // 保持原值
            }
        }
        // 子非空则覆盖父
        if (effective.getAgentKind() == null) effective.setAgentKind(parent.getAgentKind());
        if (effective.getVerifierType() == null) effective.setVerifierType(parent.getVerifierType());
        if (effective.getIsolationLevel() == null) effective.setIsolationLevel(parent.getIsolationLevel());
        if (effective.getMode() == null) effective.setMode(parent.getMode());
        if (effective.getOmnigentAgentId() == null) effective.setOmnigentAgentId(parent.getOmnigentAgentId());
        if (effective.getSoulContent() == null) effective.setSoulContent(parent.getSoulContent());
        if (effective.getModel() == null) effective.setModel(parent.getModel());
        if (effective.getRoleId() == null) effective.setRoleId(parent.getRoleId());
        if (effective.getToolsConfig() == null) effective.setToolsConfig(parent.getToolsConfig());
    }

    private static java.util.List<String> parseTagList(String json) {
        if (json == null || json.isBlank()) {
            return java.util.Collections.emptyList();
        }
        try {
            java.util.List<Object> list = cn.iocoder.yudao.framework.common.util.json.JsonUtils
                    .parseObject(json, java.util.List.class);
            java.util.List<String> res = new java.util.ArrayList<>();
            for (Object o : list) {
                res.add(String.valueOf(o));
            }
            return res;
        } catch (Exception e) {
            return java.util.Collections.emptyList();
        }
    }

    /**
     * 校验 parentDefId 不能指向自己或子孙（环检测）。
     */
    private void validateParentChain(Long selfId, Long parentDefId) {
        if (parentDefId == null) {
            return;
        }
        if (selfId != null && selfId.equals(parentDefId)) {
            throw exception(AGENT_DEF_PARENT_CYCLE);
        }
        // 向上走链，若回到 selfId 则成环
        java.util.Set<Long> visited = new java.util.HashSet<>();
        Long cursor = parentDefId;
        while (cursor != null && !visited.contains(cursor)) {
            visited.add(cursor);
            if (cursor.equals(selfId)) {
                throw exception(AGENT_DEF_PARENT_CYCLE);
            }
            SpkAgentDefDO p = agentDefMapper.selectById(cursor);
            if (p == null) {
                break;
            }
            cursor = p.getParentDefId();
        }
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
