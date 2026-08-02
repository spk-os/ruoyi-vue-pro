# OTA 升级模块文档

## 概述

OTA（Over-The-Air）升级模块负责设备的固件远程升级管理，是 IoT 系统中非常重要的功能模块。

## 核心 DTO

### IotDeviceOtaUpgradeReqDTO

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IotDeviceOtaUpgradeReqDTO {

    /** 固件版本号 */
    private String version;

    /** 固件文件下载地址 */
    private String fileUrl;

    /** 固件文件大小（字节） */
    private Long fileSize;

    /** 固件文件摘要算法 */
    private String fileDigestAlgorithm;

    /** 固件文件摘要值 */
    private String fileDigestValue;
}
```

## 与 config_53 的关系

OTA 升级和配置推送都是下行消息，使用相似的 DTO 结构：

| 特性 | CONFIG_PUSH | OTA_UPGRADE |
|------|-------------|-------------|
| 用途 | 下发配置参数 | 下发固件升级信息 |
| DTO | IotDeviceConfigPushReqDTO | IotDeviceOtaUpgradeReqDTO |
| 签名 | 支持 | 支持（通过 digest） |
| 文件大小 | configSize | fileSize |
