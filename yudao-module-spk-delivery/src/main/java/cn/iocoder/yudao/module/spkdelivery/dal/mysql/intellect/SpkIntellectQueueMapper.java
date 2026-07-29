package cn.iocoder.yudao.module.spkdelivery.dal.mysql.intellect;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.intellect.SpkIntellectQueueDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * SPK-OS OR 池需求队列 Mapper
 *
 * @author SPK-OS
 */
@Mapper
public interface SpkIntellectQueueMapper extends BaseMapperX<SpkIntellectQueueDO> {

    default SpkIntellectQueueDO selectByDedupHash(String dedupHash) {
        return selectOne(new LambdaQueryWrapperX<SpkIntellectQueueDO>()
                .eq(SpkIntellectQueueDO::getDedupHash, dedupHash));
    }

    default List<SpkIntellectQueueDO> selectListByStatus(String status) {
        return selectList(new LambdaQueryWrapperX<SpkIntellectQueueDO>()
                .eq(SpkIntellectQueueDO::getStatus, status));
    }

}
