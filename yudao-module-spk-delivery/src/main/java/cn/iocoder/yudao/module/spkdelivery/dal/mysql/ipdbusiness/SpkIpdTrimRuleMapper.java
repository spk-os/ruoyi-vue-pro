package cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdTrimRuleDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * IPD 裁剪规则 Mapper
 *
 * @author SPK-OS
 */
@Mapper
public interface SpkIpdTrimRuleMapper extends BaseMapperX<SpkIpdTrimRuleDO> {

    default List<SpkIpdTrimRuleDO> selectListByProfileVersionId(Long profileVersionId) {
        return selectList(new LambdaQueryWrapperX<SpkIpdTrimRuleDO>()
                .eq(SpkIpdTrimRuleDO::getProfileVersionId, profileVersionId)
                .orderByAsc(SpkIpdTrimRuleDO::getStage));
    }
}
