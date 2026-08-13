package cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * IPD 项目参与者 DO
 * <p>
 * 项目/版本作用域下人、Agent 或 Agent 编队的统一参与关系。设计文档 §3.3 / §9.5。
 * actor_type=HUMAN 时 actor_id 引用系统用户；AGENT 引用 spk_agent_def；SQUAD 引用编队。
 * 同一作用域同一业务角色只能有一个 accountable（accountable_flag=1），由 Service 校验。
 *
 * @author SPK-OS
 */
@TableName("spk_ipd_project_actor")
@KeySequence("spk_ipd_project_actor_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpkIpdProjectActorDO extends BaseDO {

    @TableId
    private Long id;
    private Long projectId;
    /** 版本作用域；null 表示项目级 */
    private Long versionId;
    /** HUMAN/AGENT/SQUAD/SYSTEM */
    private String actorType;
    private Long actorId;
    /** IPD-PM/PO/ARCH/DEV/QA/RELEASE/DCP_DECIDER 等 */
    private String businessRole;
    /** 1=该作用域该角色的唯一 accountable */
    private Integer accountableFlag;
    /** 容量百分比 0-100 */
    private Integer capacityPct;
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;
    /** ACTIVE/INACTIVE */
    private String status;
    private Integer lockVersion;
}
