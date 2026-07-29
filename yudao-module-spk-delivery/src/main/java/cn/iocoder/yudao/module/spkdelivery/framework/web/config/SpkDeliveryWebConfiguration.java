package cn.iocoder.yudao.module.spkdelivery.framework.web.config;

import cn.iocoder.yudao.framework.swagger.config.YudaoSwaggerAutoConfiguration;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * spk-delivery 模块的 web 组件的 Configuration
 *
 * @author SPK-OS
 */
@Configuration(proxyBeanMethods = false)
public class SpkDeliveryWebConfiguration {

    /**
     * spk-delivery 模块的 API 分组
     */
    @Bean
    public GroupedOpenApi spkDeliveryGroupedOpenApi() {
        return YudaoSwaggerAutoConfiguration.buildGroupedOpenApi("spk-delivery");
    }

}
