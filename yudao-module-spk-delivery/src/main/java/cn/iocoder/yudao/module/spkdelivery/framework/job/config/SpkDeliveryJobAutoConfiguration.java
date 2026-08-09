package cn.iocoder.yudao.module.spkdelivery.framework.job.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * spk-delivery 调度配置：开启 Spring @Scheduled，供 {@code SpkAgentTimeoutJob} 等周期任务。
 * <p>
 * 独立配置类，不与 web 配置混置；@EnableScheduling 全局生效一次即可。
 *
 * @author SPK-OS
 */
@Configuration
@EnableScheduling
public class SpkDeliveryJobAutoConfiguration {
}
