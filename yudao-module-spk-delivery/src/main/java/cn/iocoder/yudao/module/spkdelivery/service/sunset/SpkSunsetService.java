package cn.iocoder.yudao.module.spkdelivery.service.sunset;

import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.sunset.SpkSunsetDO;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.sunset.SpkSunsetMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

/**
 * SPK-OS R8 退市服务
 *
 * @author SPK-OS
 */
@Service
@Validated
public class SpkSunsetService {

    @Resource
    private SpkSunsetMapper sunsetMapper;

    public SpkSunsetDO start(String instanceId, String sunsetReport, String archiveStatus) {
        SpkSunsetDO sunset = SpkSunsetDO.builder()
                .instanceId(instanceId)
                .sunsetReport(sunsetReport)
                .archiveStatus(archiveStatus == null ? "archiving" : archiveStatus)
                .build();
        sunsetMapper.insert(sunset);
        return sunset;
    }

}
