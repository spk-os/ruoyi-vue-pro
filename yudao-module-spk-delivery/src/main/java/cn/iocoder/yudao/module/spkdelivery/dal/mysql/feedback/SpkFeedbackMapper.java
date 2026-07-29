package cn.iocoder.yudao.module.spkdelivery.dal.mysql.feedback;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.feedback.SpkFeedbackDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * SPK-OS R7 反馈 Mapper
 *
 * @author SPK-OS
 */
@Mapper
public interface SpkFeedbackMapper extends BaseMapperX<SpkFeedbackDO> {

    default List<SpkFeedbackDO> selectListByInstanceId(String instanceId) {
        return selectList(new LambdaQueryWrapperX<SpkFeedbackDO>()
                .eq(SpkFeedbackDO::getInstanceId, instanceId));
    }

}
