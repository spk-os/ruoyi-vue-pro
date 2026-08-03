package cn.iocoder.yudao.module.spkdelivery.dal.mysql.artifact;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.artifact.SpkArtifactManifestDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * Artifact Manifest Mapper
 *
 * @author SPK-OS
 */
@Mapper
public interface SpkArtifactManifestMapper extends BaseMapperX<SpkArtifactManifestDO> {

    default SpkArtifactManifestDO selectByArtifactId(String artifactId) {
        return selectOne(new LambdaQueryWrapperX<SpkArtifactManifestDO>()
                .eq(SpkArtifactManifestDO::getArtifactId, artifactId));
    }

    default List<SpkArtifactManifestDO> selectListByActivityRunId(String activityRunId) {
        return selectList(new LambdaQueryWrapperX<SpkArtifactManifestDO>()
                .eq(SpkArtifactManifestDO::getActivityRunId, activityRunId)
                .orderByAsc(SpkArtifactManifestDO::getArtifactType)
                .orderByDesc(SpkArtifactManifestDO::getVersion));
    }

    default List<SpkArtifactManifestDO> selectListByProcessInstanceId(String processInstanceId) {
        return selectList(new LambdaQueryWrapperX<SpkArtifactManifestDO>()
                .eq(SpkArtifactManifestDO::getProcessInstanceId, processInstanceId)
                .orderByDesc(SpkArtifactManifestDO::getId));
    }

    default SpkArtifactManifestDO selectLatestByRunAndType(String activityRunId, String artifactType) {
        // 版本递增场景：同一 run+type 可有多版本，必须 LIMIT 1 取最新，否则 selectOne 抛 TooManyResultsException。
        return selectOne(new LambdaQueryWrapperX<SpkArtifactManifestDO>()
                .eq(SpkArtifactManifestDO::getActivityRunId, activityRunId)
                .eq(SpkArtifactManifestDO::getArtifactType, artifactType)
                .orderByDesc(SpkArtifactManifestDO::getVersion)
                .last("LIMIT 1"));
    }

}
