package cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdWorkItemLinkDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * Plane 工作项映射 Mapper（设计文档 §10.7）。
 *
 * @author SPK-OS
 */
@Mapper
public interface SpkIpdWorkItemLinkMapper extends BaseMapperX<SpkIpdWorkItemLinkDO> {

    default List<SpkIpdWorkItemLinkDO> selectListByProject(Long projectId) {
        return selectList(new LambdaQueryWrapperX<SpkIpdWorkItemLinkDO>()
                .eq(SpkIpdWorkItemLinkDO::getProjectId, projectId)
                .orderByDesc(SpkIpdWorkItemLinkDO::getCreateTime));
    }

    default SpkIpdWorkItemLinkDO selectByPlaneIssueId(Long projectId, String planeIssueId) {
        return selectOne(new LambdaQueryWrapperX<SpkIpdWorkItemLinkDO>()
                .eq(SpkIpdWorkItemLinkDO::getProjectId, projectId)
                .eq(SpkIpdWorkItemLinkDO::getPlaneIssueId, planeIssueId));
    }
}
