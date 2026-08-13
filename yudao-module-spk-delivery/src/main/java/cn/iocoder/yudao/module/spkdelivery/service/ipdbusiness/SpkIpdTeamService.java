package cn.iocoder.yudao.module.spkdelivery.service.ipdbusiness;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.actor.SpkIpdActorCandidateRespVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.actor.SpkIpdProjectActorPageReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.actor.SpkIpdProjectActorSaveReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.assignment.SpkIpdAssignmentCreateReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.assignment.SpkIpdAssignmentPageReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.assignment.SpkIpdAssignmentReassignReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.team.SpkIpdTeamRespVO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdAssignmentDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdProjectActorDO;

import java.util.List;

/**
 * IPD 团队、参与者与分派服务（设计文档 §3.3 / §5.1 团队与智能体 / §10.7）。
 * <p>
 * 项目参与者统一模型（HUMAN/AGENT/SQUAD）+ 任务分派（Assignment）+ 候选检索。
 * 人引用系统用户（AdminUserApi 只读），Agent 引用 spk_agent_def，编队引用 spk_agent_squad。
 *
 * @author SPK-OS
 */
public interface SpkIpdTeamService {

    // ---------- 参与者 ----------

    /** 列出项目/版本作用域参与者（含显示名） */
    List<SpkIpdTeamRespVO.ActorRow> listActors(Long projectId, Long versionId);

    PageResult<SpkIpdProjectActorDO> pageActors(SpkIpdProjectActorPageReqVO reqVO);

    SpkIpdProjectActorDO saveActor(SpkIpdProjectActorSaveReqVO reqVO);

    void deleteActor(Long id);

    /** 候选检索：人从 RuoYi 用户，Agent/编队从本地定义 */
    List<SpkIpdActorCandidateRespVO> candidates(String actorType, String role, String q);

    // ---------- 分派 ----------

    SpkIpdAssignmentDO createAssignment(SpkIpdAssignmentCreateReqVO reqVO);

    PageResult<SpkIpdAssignmentDO> pageAssignments(SpkIpdAssignmentPageReqVO reqVO);

    SpkIpdAssignmentDO reassign(Long assignmentId, SpkIpdAssignmentReassignReqVO reqVO);

    /** 团队统一页聚合：人/Agent/编队 + 负载与产出 */
    SpkIpdTeamRespVO team(Long projectId, Long versionId);
}
