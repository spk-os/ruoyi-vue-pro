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
     * <p>
     * 机制：{@link YudaoSwaggerAutoConfiguration#buildGroupedOpenApi(String, String)} 按
     * {@code /admin-api/{path}/**} + {@code /app-api/{path}/**} 匹配 controller 路径。
     * <b>注意 group 名与 path 不同</b>：group="spk-delivery" 是 Knife4j 下拉里的分组显示名，
     * 而 spk-delivery 的 22 个 controller 全部以 {@code /spk/...} 为前缀（/spk/ipd/project、
     * /spk/agent-task、/spk/gate、/spk/evidence …），无一以 /spk-delivery/ 开头。
     * 故 path 必须用 "spk"（匹配 /admin-api/spk/**），否则分组下扫描不到任何接口→文档页空白。
     */
    @Bean
    public GroupedOpenApi spkDeliveryGroupedOpenApi() {
        return YudaoSwaggerAutoConfiguration.buildGroupedOpenApi("spk-delivery", "spk");
    }

}
