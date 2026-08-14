package cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Plane 工作项映射 DO（设计文档 §10.7 / §8 表清单）。
 * <p>
 * 将 Plane issue 绑定到项目/版本/流程/Activity；link_type=REQUIREMENT/TASK/DEFECT/MILESTONE。
 * 不创建/编辑 Plane issue 本身，只存必要快照与映射。
 *
 * @author SPK-OS
 */
@TableName("spk_ipd_work_item_link")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class SpkIpdWorkItemLinkDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private Long versionId;
    private Long flowRunId;
    private String activityCode;
    private String planeWorkspaceId;
    private String planeProjectId;
    private String planeIssueId;
    private String planeIssueSeq;
    private String linkType;
    private String syncStatus;
    private String lastSnapshotJson;
    private LocalDateTime lastSyncedAt;
    private String creator;
    private LocalDateTime createTime;
    private String updater;
    private LocalDateTime updateTime;
    private Integer deleted;
}
