package cn.iocoder.yudao.module.spkdelivery.dal.mysql.evidence;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.evidence.SpkVerificationReceiptDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * Verification Receipt Mapper
 *
 * @author SPK-OS
 */
@Mapper
public interface SpkVerificationReceiptMapper extends BaseMapperX<SpkVerificationReceiptDO> {

    default SpkVerificationReceiptDO selectByReceiptId(String receiptId) {
        return selectOne(new LambdaQueryWrapperX<SpkVerificationReceiptDO>()
                .eq(SpkVerificationReceiptDO::getReceiptId, receiptId));
    }

    default List<SpkVerificationReceiptDO> selectByArtifactId(String artifactId) {
        return selectList(new LambdaQueryWrapperX<SpkVerificationReceiptDO>()
                .eq(SpkVerificationReceiptDO::getArtifactId, artifactId));
    }

    default List<SpkVerificationReceiptDO> selectListByActivityRunId(String activityRunId) {
        return selectList(new LambdaQueryWrapperX<SpkVerificationReceiptDO>()
                .eq(SpkVerificationReceiptDO::getActivityRunId, activityRunId)
                .orderByDesc(SpkVerificationReceiptDO::getId));
    }

}
