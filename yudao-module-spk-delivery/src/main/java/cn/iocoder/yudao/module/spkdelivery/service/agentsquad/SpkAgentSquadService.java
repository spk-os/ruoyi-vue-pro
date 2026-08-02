package cn.iocoder.yudao.module.spkdelivery.service.agentsquad;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.agentsquad.vo.SpkAgentSquadCreateReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.agentsquad.vo.SpkAgentSquadMemberReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.agentsquad.vo.SpkAgentSquadMemberRespVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.agentsquad.vo.SpkAgentSquadPageReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.agentsquad.vo.SpkAgentSquadUpdateReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.agentsquad.vo.SpkAgentSquadWakeRespVO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.agentdef.SpkAgentDefDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.agentsquad.SpkAgentSquadDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.agentsquad.SpkAgentSquadMemberDO;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.agentdef.SpkAgentDefMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.agentsquad.SpkAgentSquadMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.agentsquad.SpkAgentSquadMemberMapper;
import cn.iocoder.yudao.module.spkdelivery.enums.SpkAgentSquadStatusEnum;
import cn.iocoder.yudao.module.spkdelivery.service.agentdef.SpkAgentDefService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.spkdelivery.enums.ErrorCodeConstants.*;

/**
 * SPK-OS 智能体编队服务
 *
 * <p>与 Paddock 的差别：Paddock 无独立 squad 实体，本系统提供独立编队（头表 + 成员表），
 * 支持按顺序串行唤醒全部成员。
 *
 * @author SPK-OS
 */
@Slf4j
@Service
@Validated
public class SpkAgentSquadService {

    @Resource
    private SpkAgentSquadMapper squadMapper;
    @Resource
    private SpkAgentSquadMemberMapper memberMapper;
    @Resource
    private SpkAgentDefMapper agentDefMapper;
    @Resource
    private SpkAgentDefService agentDefService;

    // ---------- 编队 CRUD ----------

    public SpkAgentSquadDO create(SpkAgentSquadCreateReqVO reqVO) {
        validateNameUnique(null, reqVO.getName());
        validateCodeUnique(null, reqVO.getCode());
        SpkAgentSquadDO squad = SpkAgentSquadDO.builder()
                .name(reqVO.getName())
                .code(reqVO.getCode())
                .description(reqVO.getDescription())
                .status(reqVO.getStatus() != null ? reqVO.getStatus() : SpkAgentSquadStatusEnum.ACTIVE.getLabel())
                .config(reqVO.getConfig())
                .build();
        squadMapper.insert(squad);
        return squad;
    }

    public void update(SpkAgentSquadUpdateReqVO reqVO) {
        validateExists(reqVO.getId());
        validateNameUnique(reqVO.getId(), reqVO.getName());
        validateCodeUnique(reqVO.getId(), reqVO.getCode());
        SpkAgentSquadDO update = SpkAgentSquadDO.builder()
                .id(reqVO.getId())
                .name(reqVO.getName())
                .code(reqVO.getCode())
                .description(reqVO.getDescription())
                .status(reqVO.getStatus())
                .config(reqVO.getConfig())
                .build();
        squadMapper.updateById(update);
    }

    public void delete(Long id) {
        validateExists(id);
        memberMapper.deleteBySquadId(id);
        squadMapper.deleteById(id);
    }

    public SpkAgentSquadDO get(Long id) {
        return squadMapper.selectById(id);
    }

    public PageResult<SpkAgentSquadDO> getPage(SpkAgentSquadPageReqVO reqVO) {
        return squadMapper.selectPage(reqVO);
    }

    public List<SpkAgentSquadDO> getList() {
        return squadMapper.selectList();
    }

    public int countMembers(Long squadId) {
        List<SpkAgentSquadMemberDO> members = memberMapper.selectListBySquadId(squadId);
        return members == null ? 0 : members.size();
    }

    // ---------- 成员 CRUD ----------

    @Transactional(rollbackFor = Exception.class)
    public void addMember(SpkAgentSquadMemberReqVO reqVO) {
        validateExists(reqVO.getSquadId());
        // 智能体存在性校验
        if (agentDefMapper.selectById(reqVO.getAgentId()) == null) {
            throw exception(AGENT_DEF_NOT_EXISTS);
        }
        // 去重
        if (memberMapper.selectBySquadIdAndAgentId(reqVO.getSquadId(), reqVO.getAgentId()) != null) {
            throw exception(AGENT_SQUAD_MEMBER_DUPLICATE);
        }
        SpkAgentSquadMemberDO member = SpkAgentSquadMemberDO.builder()
                .squadId(reqVO.getSquadId())
                .agentId(reqVO.getAgentId())
                .role(reqVO.getRole())
                .sortOrder(reqVO.getSortOrder() != null ? reqVO.getSortOrder() : nextSortOrder(reqVO.getSquadId()))
                .build();
        memberMapper.insert(member);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateMember(SpkAgentSquadMemberReqVO reqVO) {
        SpkAgentSquadMemberDO existing = validateMemberExists(reqVO.getId());
        SpkAgentSquadMemberDO update = SpkAgentSquadMemberDO.builder()
                .id(reqVO.getId())
                .squadId(reqVO.getSquadId() != null ? reqVO.getSquadId() : existing.getSquadId())
                .agentId(reqVO.getAgentId() != null ? reqVO.getAgentId() : existing.getAgentId())
                .role(reqVO.getRole())
                .sortOrder(reqVO.getSortOrder())
                .build();
        memberMapper.updateById(update);
    }

    public void removeMember(Long memberId) {
        validateMemberExists(memberId);
        memberMapper.deleteById(memberId);
    }

    public List<SpkAgentSquadMemberRespVO> getMembers(Long squadId) {
        validateExists(squadId);
        List<SpkAgentSquadMemberDO> members = memberMapper.selectListBySquadId(squadId);
        if (members == null || members.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> agentIds = members.stream().map(SpkAgentSquadMemberDO::getAgentId).distinct()
                .collect(Collectors.toList());
        Map<Long, SpkAgentDefDO> agentMap = agentDefMapper.selectBatchIds(agentIds).stream()
                .collect(Collectors.toMap(SpkAgentDefDO::getId, a -> a));
        List<SpkAgentSquadMemberRespVO> result = new ArrayList<>(members.size());
        for (SpkAgentSquadMemberDO m : members) {
            SpkAgentSquadMemberRespVO resp = new SpkAgentSquadMemberRespVO();
            resp.setId(m.getId());
            resp.setSquadId(m.getSquadId());
            resp.setAgentId(m.getAgentId());
            resp.setRole(m.getRole());
            resp.setSortOrder(m.getSortOrder());
            SpkAgentDefDO a = agentMap.get(m.getAgentId());
            if (a != null) {
                resp.setAgentName(a.getName());
                resp.setAgentCode(a.getCode());
                resp.setAgentRole(a.getRole());
                resp.setAgentStatus(a.getStatus());
                resp.setAgentRuntimeType(a.getRuntimeType());
            }
            result.add(resp);
        }
        return result;
    }

    // ---------- 编队唤醒 ----------

    /**
     * 编队唤醒：按 sortOrder 顺序对每个成员串行执行单轮对话（本地实现）
     */
    @Transactional(rollbackFor = Exception.class)
    public SpkAgentSquadWakeRespVO wake(Long squadId, String message) {
        SpkAgentSquadDO squad = validateExists(squadId);
        List<SpkAgentSquadMemberDO> members = memberMapper.selectListBySquadId(squadId);
        if (members == null || members.isEmpty()) {
            throw exception(AGENT_SQUAD_NO_MEMBER);
        }
        SpkAgentSquadWakeRespVO resp = new SpkAgentSquadWakeRespVO();
        resp.setId(squad.getId());
        resp.setName(squad.getName());
        List<SpkAgentSquadWakeRespVO.MemberWake> results = new ArrayList<>(members.size());
        for (SpkAgentSquadMemberDO m : members) {
            SpkAgentSquadWakeRespVO.MemberWake mw = new SpkAgentSquadWakeRespVO.MemberWake();
            mw.setAgentId(m.getAgentId());
            try {
                var r = agentDefService.wake(m.getAgentId(), message);
                mw.setAgentName(r.getName());
                mw.setConversationId(r.getConversationId());
                mw.setContent(r.getContent());
                mw.setStatus(r.getStatus());
            } catch (Exception e) {
                log.error("[squad-wake][squadId={} agentId={} fail]", squadId, m.getAgentId(), e);
                SpkAgentDefDO a = agentDefMapper.selectById(m.getAgentId());
                mw.setAgentName(a != null ? a.getName() : null);
                mw.setStatus("error");
                mw.setError(e.getMessage());
            }
            results.add(mw);
        }
        resp.setResults(results);
        return resp;
    }

    // ---------- 校验 ----------

    public SpkAgentSquadDO validateExists(Long id) {
        if (id == null) {
            throw exception(AGENT_SQUAD_NOT_EXISTS);
        }
        SpkAgentSquadDO squad = squadMapper.selectById(id);
        if (squad == null) {
            throw exception(AGENT_SQUAD_NOT_EXISTS);
        }
        return squad;
    }

    private SpkAgentSquadMemberDO validateMemberExists(Long memberId) {
        if (memberId == null) {
            throw exception(AGENT_SQUAD_MEMBER_NOT_EXISTS);
        }
        SpkAgentSquadMemberDO member = memberMapper.selectById(memberId);
        if (member == null) {
            throw exception(AGENT_SQUAD_MEMBER_NOT_EXISTS);
        }
        return member;
    }

    private void validateNameUnique(Long id, String name) {
        SpkAgentSquadDO existing = squadMapper.selectByName(name);
        if (existing != null && !existing.getId().equals(id)) {
            throw exception(AGENT_SQUAD_NAME_DUPLICATE);
        }
    }

    private void validateCodeUnique(Long id, String code) {
        SpkAgentSquadDO existing = squadMapper.selectByCode(code);
        if (existing != null && !existing.getId().equals(id)) {
            throw exception(AGENT_SQUAD_CODE_DUPLICATE);
        }
    }

    private Integer nextSortOrder(Long squadId) {
        List<SpkAgentSquadMemberDO> members = memberMapper.selectListBySquadId(squadId);
        if (members == null || members.isEmpty()) {
            return 1;
        }
        return members.stream().map(SpkAgentSquadMemberDO::getSortOrder)
                .filter(java.util.Objects::nonNull)
                .max(Integer::compareTo).orElse(0) + 1;
    }

}
