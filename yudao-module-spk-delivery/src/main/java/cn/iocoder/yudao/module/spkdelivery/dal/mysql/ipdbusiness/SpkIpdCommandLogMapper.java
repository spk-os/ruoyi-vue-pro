package cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdCommandLogDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * IPD 幂等命令 Mapper
 *
 * @author SPK-OS
 */
@Mapper
public interface SpkIpdCommandLogMapper extends BaseMapperX<SpkIpdCommandLogDO> {

    default SpkIpdCommandLogDO selectByIdempotencyKey(String idempotencyKey) {
        return selectOne(SpkIpdCommandLogDO::getIdempotencyKey, idempotencyKey);
    }
}
