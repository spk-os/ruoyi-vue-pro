package cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdIssueVersionRelDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * IPD 问题-版本关联 Mapper
 *
 * @author SPK-OS
 */
@Mapper
public interface SpkIpdIssueVersionRelMapper extends BaseMapperX<SpkIpdIssueVersionRelDO> {

    default List<SpkIpdIssueVersionRelDO> selectListByIssue(Long issueCaseId) {
        return selectList(SpkIpdIssueVersionRelDO::getIssueCaseId, issueCaseId);
    }

    default SpkIpdIssueVersionRelDO selectOne(Long issueCaseId, Long versionId, String relationType) {
        return selectOne(SpkIpdIssueVersionRelDO::getIssueCaseId, issueCaseId,
                SpkIpdIssueVersionRelDO::getVersionId, versionId,
                SpkIpdIssueVersionRelDO::getRelationType, relationType);
    }
}
