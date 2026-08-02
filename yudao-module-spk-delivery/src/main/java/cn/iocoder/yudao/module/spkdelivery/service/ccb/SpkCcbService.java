package cn.iocoder.yudao.module.spkdelivery.service.ccb;

import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ccb.SpkCcbRecordDO;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ccb.SpkCcbRecordMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * SPK-OS CCB 变更台账服务
 *
 * @author SPK-OS
 */
@Service
@Validated
public class SpkCcbService {

    @Resource
    private SpkCcbRecordMapper ccbRecordMapper;

    public SpkCcbRecordDO register(String changeId, String instanceId, String changeRequest, String impact, String decision) {
        SpkCcbRecordDO record = SpkCcbRecordDO.builder()
                .changeId(changeId)
                .instanceId(instanceId)
                .changeRequest(changeRequest)
                .impact(impact)
                .decision(decision)
                .build();
        ccbRecordMapper.insert(record);
        return record;
    }

    /**
     * 按流程实例查询全部 CCB 变更台账（用于详情页 IPD 产物 tab）
     */
    public List<SpkCcbRecordDO> getListByInstanceId(String instanceId) {
        return ccbRecordMapper.selectListByInstanceId(instanceId);
    }

}
