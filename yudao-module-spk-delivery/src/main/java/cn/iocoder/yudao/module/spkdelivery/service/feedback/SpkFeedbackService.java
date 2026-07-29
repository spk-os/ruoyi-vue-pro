package cn.iocoder.yudao.module.spkdelivery.service.feedback;

import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.feedback.SpkFeedbackDO;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.feedback.SpkFeedbackMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

/**
 * SPK-OS R7 反馈服务
 *
 * @author SPK-OS
 */
@Service
@Validated
public class SpkFeedbackService {

    @Resource
    private SpkFeedbackMapper feedbackMapper;

    public SpkFeedbackDO collect(String instanceId, String source, String content, String summary, Boolean newCharterSeed) {
        SpkFeedbackDO fb = SpkFeedbackDO.builder()
                .instanceId(instanceId)
                .source(source)
                .content(content)
                .summary(summary)
                .newCharterSeed(newCharterSeed)
                .build();
        feedbackMapper.insert(fb);
        return fb;
    }

}
