package cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.issue.SpkIpdIssueCasePageReqVO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdIssueCaseDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * IPD 问题 Mapper
 *
 * @author SPK-OS
 */
@Mapper
public interface SpkIpdIssueCaseMapper extends BaseMapperX<SpkIpdIssueCaseDO> {

    default SpkIpdIssueCaseDO selectByCaseNo(String caseNo) {
        return selectOne(SpkIpdIssueCaseDO::getCaseNo, caseNo);
    }

    /** 外部引用去重：同 externalSystem + externalId 视为同一问题 */
    default SpkIpdIssueCaseDO selectByExternal(String externalSystem, String externalId) {
        return selectOne(SpkIpdIssueCaseDO::getExternalSystem, externalSystem,
                SpkIpdIssueCaseDO::getExternalId, externalId);
    }

    default PageResult<SpkIpdIssueCaseDO> selectPage(SpkIpdIssueCasePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SpkIpdIssueCaseDO>()
                .eqIfPresent(SpkIpdIssueCaseDO::getProjectId, reqVO.getProjectId())
                .likeIfPresent(SpkIpdIssueCaseDO::getTitle, reqVO.getTitle())
                .eqIfPresent(SpkIpdIssueCaseDO::getIssueType, reqVO.getIssueType())
                .eqIfPresent(SpkIpdIssueCaseDO::getSeverity, reqVO.getSeverity())
                .eqIfPresent(SpkIpdIssueCaseDO::getStatus, reqVO.getStatus())
                .orderByDesc(SpkIpdIssueCaseDO::getUpdateTime));
    }
}
