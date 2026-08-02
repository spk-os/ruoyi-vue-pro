# YudaoApiLogAutoConfiguration 模块文档

## 概述
YudaoApiLogAutoConfiguration 是 Yudao 框架中用于自动配置 API 访问日志记录功能的 Spring Boot 自动配置类。
它负责注册过滤器和拦截器来捕获和记录 HTTP 请求和响应的详细信息，包括用户信息、请求参数、响应结果、执行时间等。

## 核心功能
- 通过 `ApiAccessLogFilter` 过滤器记录 API 访问日志（包括请求和响应内容，支持敏感信息脱敏）。
- 通过 `ApiAccessLogInterceptor` 拦截器在开发环境下打印请求和响应日志（用于调试）。
- 支持通过配置 `yudao.access-log.enable` 来开启或关闭访问日志功能（默认开启）。
- 与框架的其他部分集成，如 Web 配置、日志服务等。

## 架构和组件关系

### 1. 模块内部结构
YudaoApiLogAutoConfiguration
├── ApiAccessLogFilter (过滤器，记录 API 访问日志)
└── ApiAccessLogInterceptor (拦截器，开发环境下打印调试日志)

### 2. 在整个系统中的位置
YudaoApiLogAutoConfiguration 是 yudao-spring-boot-starter-web 模块的一部分，它依赖于 yudao-common 模块中的日志服务和 Web 配置。

**模块依赖关系：**
- yudao-spring-boot-starter-web
  - YudaoApiLogAutoConfiguration
    - ApiAccessLogFilter
    - ApiAccessLogInterceptor
    - YudaoWebAutoConfiguration

- yudao-common
  - ApiAccessLogCommonApi
    - ApiAccessLogServiceImpl (实际的日志服务实现)
  - WebFilterOrderEnum
  - WebProperties
  - ServletUtils
  - SpringUtils

## 如何融入整体系统
- 当 Spring Boot 应用启动时，YudaoApiLogAutoConfiguration 会被自动加载（因为它被标注为 `@AutoConfiguration`）。
- 它在 YudaoWebAutoConfiguration 之后初始化（通过 `@AutoConfiguration(after = YudaoWebAutoConfiguration.class)`），确保 Web 配置已经完成。
- 过滤器 `ApiAccessLogFilter` 被注册到 Spring 的过滤器链中，顺序由 `WebFilterOrderEnum.API_ACCESS_LOG_FILTER` 决定（在 RequestBodyCacheFilter 之后，在 XSS Filter 之前）。
- 拦截器 `ApiAccessLogInterceptor` 被添加到 Spring MVC 的拦截器链中，仅在非生产环境下打印调试日志。
- 日志的实际记录通过 `ApiAccessLogCommonApi` 接口实现（通常是 `ApiAccessLogServiceImpl`），该服务会将日志异步保存到数据库或其他存储中。

## 配置说明
访问日志功能可以通过以下配置关闭：
```yaml
yudao:
  access-log:
    enable: false
```

## 数据流示例
**API 请求的日志记录流程：**
1. 客户端发送 HTTP 请求
2. ApiAccessLogFilter 过滤器捕获请求，记录开始时间和参数
3. ApiAccessLogInterceptor 拦截器在开发环境打印请求信息
4. 请求继续传递给控制器和业务服务
5. 控制器处理完成后，拦截器记录响应和耗时
6. ApiAccessLogFilter 过滤器构建日志 DTO 并异步调用 `createApiAccessLogAsync`
7. ApiAccessLogCommonApi 将日志异步保存到数据库
8. 最终返回 HTTP 响应给客户端

**注意事项：**
- 过滤器和拦截器的作用不同：过滤器负责实际的日志记录（包括异步保存），拦截器仅在开发环境用于调试打印。
- 日志记录过程中会对请求和响应中的敏感字段（如 password、token 等）进行脱敏处理。
- 访问日志的记录是异步的，不会影响主请求的响应时间。

## 结论
YudaoApiLogAutoConfiguration 模块为 Yudao 框架提供了完整的 API 访问日志解决方案，通过过滤器和拦截器的组合，实现了对 API 请求的全面记录和监控，有助于系统的审计、调试和性能分析。

以上即为 config_18 模块的完整文档。