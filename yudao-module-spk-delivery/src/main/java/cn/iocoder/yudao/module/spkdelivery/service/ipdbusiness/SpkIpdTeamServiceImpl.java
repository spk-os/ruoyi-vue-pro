package cn.iocoder.yudao.module.spkdelivery.service.ipdbusiness;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.actor.SpkIpdActorCandidateRespVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.actor.SpkIpdProjectActorPageReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.actor.SpkIpdProjectActorSaveReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.assignment.SpkIpdAssignmentCreateReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.assignment.SpkIpdAssignmentPageReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.assignment.SpkIpdAssignmentReassignReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.team.SpkIpdTeamRespVO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.agentdef.SpkAgentDefDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.agentsquad.SpkAgentSquadDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdAssignmentDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdProjectActorDO;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.agentdef.SpkAgentDefMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.agentsquad.SpkAgentSquadMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness.SpkIpdAssignmentMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness.SpkIpdProjectActorMapper;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.spkdelivery.enums.ErrorCodeConstants.*;

/**
 * IPD 团队、参与者与分派服务实现（设计文档 section 3.3 / 5.1 / 10.7）。
 * 人引用系统用户（AdminUserApi 只读），Agent 引用 spk_agent_def，编队引用 spk_agent_squad。
 * 同一作用域同一业务角色只能一个 accountable；BPM 审批任务 accountable 必须 HUMAN。
 *
 * @author SPK-OS
 */
@Service
@Slf4j
public class SpkIpdTeamServiceImpl implements SpkIpdTeamService {

    @Resource
    private SpkIpdProjectActorMapper actorMapper;
    @Resource
    private SpkIpdAssignmentMapper assignmentMapper;
    @Resource
    private SpkAgentDefMapper agentDefMapper;
    @Resource
    private SpkAgentSquadMapper agentSquadMapper;
    @Resource
    private AdminUserApi adminUserApi;

    private static final Set<String> VALID_ACTOR_TYPES = Set.of("HUMAN", "AGENT", "SQUAD", "SYSTEM");

    // ==================== 参与者 ====================

    @Override
    public List<SpkIpdTeamRespVO.ActorRow> listActors(Long projectId, Long versionId) {
        List<SpkIpdProjectActorDO> actors = actorMapper.selectListByProject(projectId, versionId);
        return actors.stream()
                .map(a -> toActorRow(a, resolveName(a.getActorType(), a.getActorId())))
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public PageResult<SpkIpdProjectActorDO> pageActors(SpkIpdProjectActorPageReqVO reqVO) {
        return actorMapper.selectPage(reqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SpkIpdProjectActorDO saveActor(SpkIpdProjectActorSaveReqVO req) {
        if (!VALID_ACTOR_TYPES.contains(req.getActorType())) {
            throw exception(IPD_ACTOR_TYPE_INVALID);
        }
        List<SpkIpdProjectActorDO> existing = actorMapper.selectListByProject(req.getProjectId(), req.getVersionId());
        for (SpkIpdProjectActorDO a : existing) {
            if (a.getActorType().equals(req.getActorType())
                    && a.getActorId().equals(req.getActorId())
                    && a.getBusinessRole().equals(req.getBusinessRole())) {
                throw exception(IPD_ACTOR_DUPLICATE);
            }
        }
        if (req.getAccountableFlag() != null && req.getAccountableFlag() == 1) {
            for (SpkIpdProjectActorDO a : existing) {
                if (a.getBusinessRole().equals(req.getBusinessRole())
                        && a.getAccountableFlag() != null && a.getAccountableFlag() == 1) {
                    throw exception(IPD_ACTOR_ACCOUNTABLE_DUPLICATE);
                }
            }
        }
        SpkIpdProjectActorDO actor = SpkIpdProjectActorDO.builder()
                .projectId(req.getProjectId())
                .versionId(req.getVersionId())
                .actorType(req.getActorType())
                .actorId(req.getActorId())
                .businessRole(req.getBusinessRole())
                .accountableFlag(req.getAccountableFlag() == null ? 0 : req.getAccountableFlag())
                .capacityPct(req.getCapacityPct() == null ? 100 : req.getCapacityPct())
                .effectiveFrom(req.getEffectiveFrom())
                .effectiveTo(req.getEffectiveTo())
                .status(req.getStatus() == null ? "ACTIVE" : req.getStatus())
                .lockVersion(0)
                .build();
        actorMapper.insert(actor);
        return actor;
    }

    @Override
    public void deleteActor(Long id) {
        if (actorMapper.selectById(id) == null) {
            throw exception(IPD_ACTOR_NOT_EXISTS);
        }
        actorMapper.deleteById(id);
    }

    @Override
    public List<SpkIpdActorCandidateRespVO> candidates(String actorType, String role, String q) {
        List<SpkIpdActorCandidateRespVO> list = new ArrayList<>();
        String type = actorType == null ? "" : actorType.toUpperCase();
        String kw = q == null ? "" : q.trim();
        // 人：从 RuoYi 用户按昵称模糊查
        if (type.isEmpty() || "HUMAN".equals(type)) {
            try {
                List<AdminUserRespDTO> users = kw.isEmpty()
                        ? Collections.emptyList()
                        : adminUserApi.getUserListByNickname(kw);
                for (AdminUserRespDTO u : users) {
                    SpkIpdActorCandidateRespVO c = new SpkIpdActorCandidateRespVO();
                    c.setActorType("HUMAN");
                    c.setActorId(u.getId());
                    c.setName(u.getNickname());
                    c.setSubtitle(u.getDeptId() == null ? "用户" : ("部门 " + u.getDeptId()));
                    list.add(c);
                }
            } catch (Exception e) {
                log.warn("[candidates][HUMAN 查询失败：{}]", e.getMessage());
            }
        }
        // Agent：从本地定义模糊查
        if (type.isEmpty() || "AGENT".equals(type)) {
            List<SpkAgentDefDO> agents = agentDefMapper.selectList(
                    new cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX<SpkAgentDefDO>()
                            .likeIfPresent(SpkAgentDefDO::getName, kw)
                            .likeIfPresent(SpkAgentDefDO::getRole, role)
                            .eqIfPresent(SpkAgentDefDO::getHidden, 0)
                            .orderByDesc(SpkAgentDefDO::getId).last("limit 50"));
            for (SpkAgentDefDO a : agents) {
                SpkIpdActorCandidateRespVO c = new SpkIpdActorCandidateRespVO();
                c.setActorType("AGENT");
                c.setActorId(a.getId());
                c.setName(a.getName());
                c.setSubtitle(a.getCode() + " · " + (a.getRuntimeType() == null ? "" : a.getRuntimeType()));
                c.setBusinessRole(a.getRole());
                list.add(c);
            }
        }
        // 编队
        if (type.isEmpty() || "SQUAD".equals(type)) {
            List<SpkAgentSquadDO> squads = agentSquadMapper.selectList(
                    new cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX<SpkAgentSquadDO>()
                            .likeIfPresent(SpkAgentSquadDO::getName, kw)
                            .orderByDesc(SpkAgentSquadDO::getId).last("limit 50"));
            for (SpkAgentSquadDO s : squads) {
                SpkIpdActorCandidateRespVO c = new SpkIpdActorCandidateRespVO();
                c.setActorType("SQUAD");
                c.setActorId(s.getId());
                c.setName(s.getName());
                c.setSubtitle(s.getCode() + " · 编队");
                list.add(c);
            }
        }
        return list;
    }

    // ==================== 分派 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SpkIpdAssignmentDO createAssignment(SpkIpdAssignmentCreateReqVO req) {
        // BPM 审批任务 accountable 必须 HUMAN（Agent 不能伪装系统用户完成 DCP）
        if ("BPM_TASK".equals(req.getWorkItemType())) {
            String accType = req.getAccountableActorType() == null ? req.getActorType() : req.getAccountableActorType();
            if (!"HUMAN".equals(accType)) {
                throw exception(IPD_ASSIGNMENT_BPM_NOT_HUMAN);
            }
        }
        SpkIpdAssignmentDO a = SpkIpdAssignmentDO.builder()
                .projectId(req.getProjectId())
                .versionId(req.getVersionId())
                .flowRunId(req.getFlowRunId())
                .activityRunId(req.getActivityRunId())
                .workItemType(req.getWorkItemType())
                .workItemId(req.getWorkItemId())
                .actorType(req.getActorType())
                .actorId(req.getActorId())
                .accountableActorType(req.getAccountableActorType() == null ? req.getActorType() : req.getAccountableActorType())
                .accountableActorId(req.getAccountableActorId() == null ? req.getActorId() : req.getAccountableActorId())
                .status("ASSIGNED")
                .plannedEffort(req.getPlannedEffort())
                .actualEffort(req.getActualEffort())
                .lockVersion(0)
                .build();
        assignmentMapper.insert(a);
        return a;
    }

    @Override
    public PageResult<SpkIpdAssignmentDO> pageAssignments(SpkIpdAssignmentPageReqVO reqVO) {
        return assignmentMapper.selectPage(reqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SpkIpdAssignmentDO reassign(Long assignmentId, SpkIpdAssignmentReassignReqVO req) {
        SpkIpdAssignmentDO a = assignmentMapper.selectById(assignmentId);
        if (a == null) {
            throw exception(IPD_ASSIGNMENT_NOT_EXISTS);
        }
        if ("DONE".equals(a.getStatus()) || "CANCELLED".equals(a.getStatus())) {
            throw exception(IPD_ASSIGNMENT_NOT_REASSIGNABLE);
        }
        if ("BPM_TASK".equals(a.getWorkItemType())) {
            String accType = req.getAccountableActorType() == null ? req.getActorType() : req.getAccountableActorType();
            if (!"HUMAN".equals(accType)) {
                throw exception(IPD_ASSIGNMENT_BPM_NOT_HUMAN);
            }
        }
        // 转派：旧执行者留痕由审计覆盖，这里直接更新执行者
        a.setActorType(req.getActorType());
        a.setActorId(req.getActorId());
        if (req.getAccountableActorType() != null) {
            a.setAccountableActorType(req.getAccountableActorType());
            a.setAccountableActorId(req.getAccountableActorId());
        }
        a.setLockVersion((a.getLockVersion() == null ? 0 : a.getLockVersion()) + 1);
        assignmentMapper.updateById(a);
        return a;
    }

    // ==================== 团队统一页聚合 ====================

    @Override
    public SpkIpdTeamRespVO team(Long projectId, Long versionId) {
        SpkIpdTeamRespVO resp = new SpkIpdTeamRespVO();
        resp.setProjectId(projectId);
        resp.setVersionId(versionId);
        List<SpkIpdProjectActorDO> actors = projectId == null
                ? Collections.emptyList()
                : actorMapper.selectListByProject(projectId, versionId);
        List<SpkIpdTeamRespVO.ActorRow> people = new ArrayList<>();
        List<SpkIpdTeamRespVO.ActorRow> agents = new ArrayList<>();
        List<SpkIpdTeamRespVO.ActorRow> squads = new ArrayList<>();
        for (SpkIpdProjectActorDO a : actors) {
            SpkIpdTeamRespVO.ActorRow row = toActorRow(a, resolveName(a.getActorType(), a.getActorId()));
            switch (a.getActorType() == null ? "" : a.getActorType()) {
                case "HUMAN": people.add(row); break;
                case "AGENT": agents.add(row); break;
                case "SQUAD": squads.add(row); break;
                default: people.add(row);
            }
        }
        resp.setPeople(people);
        resp.setAgents(agents);
        resp.setSquads(squads);
        resp.setLoad(buildLoad(actors));
        Map<String, Long> summary = new LinkedHashMap<>();
        summary.put("people", (long) people.size());
        summary.put("agents", (long) agents.size());
        summary.put("squads", (long) squads.size());
        summary.put("actors", (long) actors.size());
        resp.setSummary(summary);
        return resp;
    }

    // ==================== 辅助 ====================

    private SpkIpdTeamRespVO.ActorRow toActorRow(SpkIpdProjectActorDO a, String name) {
        SpkIpdTeamRespVO.ActorRow row = new SpkIpdTeamRespVO.ActorRow();
        row.setId(a.getId());
        row.setActorType(a.getActorType());
        row.setActorId(a.getActorId());
        row.setName(name);
        row.setBusinessRole(a.getBusinessRole());
        row.setAccountableFlag(a.getAccountableFlag());
        row.setCapacityPct(a.getCapacityPct());
        row.setStatus(a.getStatus());
        row.setProjectId(a.getProjectId());
        row.setVersionId(a.getVersionId());
        row.setSubtitle(name);
        return row;
    }

    /** 解析参与者显示名：人→昵称，Agent→定义名，编队→编队名 */
    private String resolveName(String actorType, Long actorId) {
        if (actorId == null) return "";
        try {
            if ("HUMAN".equals(actorType)) {
                AdminUserRespDTO u = adminUserApi.getUser(actorId);
                return u == null ? ("用户#" + actorId) : u.getNickname();
            }
            if ("AGENT".equals(actorType)) {
                SpkAgentDefDO a = agentDefMapper.selectById(actorId);
                return a == null ? ("Agent#" + actorId) : a.getName();
            }
            if ("SQUAD".equals(actorType)) {
                SpkAgentSquadDO s = agentSquadMapper.selectById(actorId);
                return s == null ? ("编队#" + actorId) : s.getName();
            }
        } catch (Exception e) {
            log.warn("[resolveName][{}#{} 解析失败：{}]", actorType, actorId, e.getMessage());
        }
        return actorType + "#" + actorId;
    }

    /** 负载与产出：每个参与者的分派计数（按状态分组） */
    private List<SpkIpdTeamRespVO.LoadRow> buildLoad(List<SpkIpdProjectActorDO> actors) {
        List<SpkIpdTeamRespVO.LoadRow> rows = new ArrayList<>();
        for (SpkIpdProjectActorDO a : actors) {
            try {
                List<SpkIpdAssignmentDO> list = assignmentMapper.selectByActor(a.getActorType(), a.getActorId());
                SpkIpdTeamRespVO.LoadRow row = new SpkIpdTeamRespVO.LoadRow();
                row.setActorType(a.getActorType());
                row.setActorId(a.getActorId());
                row.setName(resolveName(a.getActorType(), a.getActorId()));
                row.setTotal((long) list.size());
                long planned = 0, running = 0, blocked = 0, done = 0;
                for (SpkIpdAssignmentDO asg : list) {
                    String s = asg.getStatus() == null ? "" : asg.getStatus();
                    switch (s) {
                        case "PLANNED": case "ASSIGNED": case "ACCEPTED": planned++; break;
                        case "IN_PROGRESS": running++; break;
                        case "BLOCKED": blocked++; break;
                        case "DONE": done++; break;
                        default:
                    }
                }
                row.setPlanned(planned);
                row.setRunning(running);
                row.setBlocked(blocked);
                row.setDone(done);
                rows.add(row);
            } catch (Exception ignore) {
            }
        }
        return rows;
    }

}
