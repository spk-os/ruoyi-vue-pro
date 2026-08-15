package cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdFailedJobDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * IPD 失败作业 Mapper
 *
 * @author SPK-OS
 */
@Mapper
public interface SpkIpdFailedJobMapper extends BaseMapperX<SpkIpdFailedJobDO> {

    default List<SpkIpdFailedJobDO> selectListByStatus(String status) {
        return selectList(new LambdaQueryWrapperX<SpkIpdFailedJobDO>()
                .eq(SpkIpdFailedJobDO::getStatus, status)
                .orderByAsc(SpkIpdFailedJobDO::getNextRetryAt));
    }
}
