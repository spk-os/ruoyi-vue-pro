package cn.iocoder.yudao.module.iot.framework.tdengine.config;

import cn.iocoder.yudao.module.iot.service.device.message.IotDeviceMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * TDengine 表初始化的 Configuration
 *
 * @author alwayssuper
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TDengineTableInitRunner implements ApplicationRunner {

    private final IotDeviceMessageService deviceMessageService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            // 初始化设备消息表
            deviceMessageService.defineDeviceMessageStable();
        } catch (Exception ex) {
            // SPK-OS: TDengine 尚未部署（见 功能开启.md §4.3，按计划延后，且不随机器自启动）。
            // 初始化失败时仅告警，不退出系统——IoT 管理页（iot_device 等 11 张表已导入 PostgreSQL）
            // 仍可正常使用；仅设备时序消息存储在 TDengine 部署并配置 tdengine 数据源前不可用。
            log.warn("[run][TDengine 未就绪，跳过设备消息超级表初始化；IoT 管理功能不受影响，时序消息待 TDengine 部署后启用]", ex);
        }
    }

}
