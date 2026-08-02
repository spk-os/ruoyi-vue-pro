# Yudao Security Auto Configuration 模块文档

## 模块概述

Yudao Security Auto Configuration 模块是 Yudao 框架的安全认证和授权核心组件，基于 Spring Security 构建，提供了统一的身份认证、授权管理和安全防护机制。该模块通过自动配置方式简化了 Spring Security 的使用，提供了 Token-based 认证、权限控制、安全异常处理等功能。

该模块主要包含两个核心配置类：
1. `YudaoSecurityAutoConfiguration` - Spring Security 的自动配置类，负责创建安全相关的 Bean
2. `YudaoWebSecurityConfigurerAdapter` - 自定义的 Spring Security 配置适配器，负责配置安全过滤链和访问控制规则

## 核心组件

### 1. YudaoSecurityAutoConfiguration

负责创建 Spring Security 所需的基础组件：

- **AuthenticationEntryPoint** - 认证失败处理器
- **AccessDeniedHandler** - 权限不足处理器  
- **PasswordEncoder** - 密码加密器（使用 BCrypt）
- **TokenAuthenticationFilter** - Token 认证过滤器
- **SecurityFrameworkService** - 安全框架服务
- **SecurityContextHolder 配置** - 设置使用 TransmittableThreadLocal 作为 Security 上下文策略

### 2. YudaoWebSecurityConfigurerAdapter

自定义的 Spring Security 配置，实现了：

- 自定义安全过滤链配置
- 基于注解的免登录 URL 自动识别
- 自定义授权规则定制点（通过 AuthorizeRequestsCustomizer 接口）
- Token 认证过滤器的注入位置配置
- 跨域、CSRF、Session 管理等安全配置

### 3. 关键支持组件

#### TransmittableThreadLocalSecurityContextHolderStrategy
实现了 Spring Security 的 SecurityContextHolderStrategy 接口，使用 TransmittableThreadLocal 来保证在异步场景下 Security Context 能够正确传播。

#### SecurityFrameworkUtils
提供安全框架相关的工具方法，主要包括：
- 从请求中获取 Token 的工具方法
- 设置和获取当前登录用户信息
- Token 前缀常量定义（Bearer）

## 架构设计

### 整体架构

Spring Boot 应用 -> YudaoSecurityAutoConfiguration
Spring Boot 应用 -> YudaoWebSecurityConfigurerAdapter

YudaoSecurityAutoConfiguration 创建的组件:
- AuthenticationEntryPoint -> AccessDeniedHandler
- PasswordEncoder -> TokenAuthenticationFilter
- SecurityFrameworkService -> SecurityContextHolder配置

YudaoWebSecurityConfigurerAdapter 配置的组件:
- 安全过滤链配置 -> 免登录 URL 自动识别
- 自定义授权规则 -> Token 过滤器注入

### 核心流程

#### 1. 认证流程
客户端 --> HTTP Request (带 Token) --> TokenAuthenticationFilter
    --> SecurityFrameworkUtils.obtainAuthorization()
    --> OAuth2TokenCommonApi.checkAccessToken()
    --> SecurityFrameworkUtils.setLoginUser()
    --> SecurityContextHolder.setAuthentication()
    --> 继续过滤链 --> 目标Endpoint

#### 2. 授权流程
客户端 --> HTTP Request --> 安全过滤链
    --> 检查请求路径
    --> 匹配免登录规则? --> 直接放行
    --> 匹配自定义规则? --> 应用自定义授权规则
        --> 授权通过 --> 继续处理
        --> 授权失败 --> 返回 403 错误
    --> 默认规则 --> 要求必须认证
        --> 已认证 --> 继续处理
        --> 未认证 --> 返回 401 错误

## 配置说明

### SecurityProperties 配置项

| 配置项 | 说明 | 默认值 | 必填 |
|--------|------|--------|------|
| yudao.security.token-header | HTTP 请求时，访问令牌的请求 Header | Authorization | 是 |
| yudao.security.token-parameter | HTTP 请求时，访问令牌的请求参数 | token | 是 |
| yudao.security.mock-enable | mock 模式的开关（仅开发环境开启） | false | 是 |
| yudao.security.mock-secret | mock 模式的密钥 | test | 是 |
| yudao.security.permit-all-urls | 免登录的 URL 列表 | 空列表 | 否 |
| yudao.security.password-encoder-length | PasswordEncoder 加密复杂度 | 4 | 否 |

### 自定义授权规则

通过实现 `AuthorizeRequestsCustomizer` 接口，可以自定义额外的授权规则：

```java
@Component
public class CustomAuthorizeRequestsCustomizer implements AuthorizeRequestsCustomizer {
    @Override
    public void customize(AuthorizationManagerRequestMatcherRegistry registry) {
        // 自定义路径权限配置
        registry.requestMatchers("/admin/**").hasRole("ADMIN");
        registry.requestMatchers("/user/**").hasAnyRole("USER", "ADMIN");
    }
}
```

## 与其他模块的关系

### 依赖关系

Security模块 --> OAuth2模块
Security模块 --> 系统框架模块
Security模块 --> Web框架模块

OAuth2模块 --> OAuth2TokenCommonApi
系统框架模块 --> SecurityFrameworkUtils, WebFrameworkUtils
Web框架模块 --> WebProperties

### 被依赖情况
Security模块为整个Yudao框架提供安全基础设施，被以下模块依赖：
- 系统管理模块（用户认证、权限管理）
- 微信公众号/小程序模块（OAuth2授权）
- 支付模块（安全验证）
- 其他需要身份验证的业务模块

## 使用说明

### 基础使用
1. 在application.yml中配置安全相关参数：
```yaml
yudao:
  security:
    token-header: Authorization
    token-parameter: token
    mock-enable: false  # 生产环境必须设为false
    mock-secret: your-secret-key
    permit-all-urls:
      - /swagger-ui/**
      - /v2/api-docs
      - /swagger-resources/**
    password-encoder-length: 4
```

### 自定义免登录路径
除了通过 `yudao.security.permit-all-urls` 配置外，还可以通过在Controller或方法上添加 `@PermitAll` 注解来实现免登录访问：
```java
@RestController
@RequestMapping("/public")
public class PublicController {
    
    @GetMapping("/info")
    @PermitAll  // 此接口免登录访问
    public CommonResult<String> getPublicInfo() {
        return success("公开信息");
    }
}
```

### 自定义授权规则
实现 `AuthorizeRequestsCustomizer` 接口来添加自定义的授权规则：
```java
@Component
public class CustomSecurityConfig implements AuthorizeRequestsCustomizer {
    @Override
    public void customize(AuthorizationManagerRequestMatcherRegistry registry) {
        // 只有ADMIN角色可以访问/admin开头的接口
        registry.requestMatchers("/admin/**").hasRole("ADMIN");
        
        // 指定IP可以访问监控接口
        servletRequestMatchers(request -> 
            request.getRemoteAddr().equals("127.0.0.1") && 
            request.getRequestURI().startsWith("/monitor")
        ).permitAll();
    }
}
```

## 重要设计说明

### 为什么需要自定义 SecurityConfigurerAdapter
在Yudao框架中，我们选择自定义 `YudaoWebSecurityConfigurerAdapter` 而不是直接使用 Spring Boot 的自动配置，主要原因是：

1. **避免冲突**：Spring Boot 的自动配置可能与我们的定制需求冲突
2. **控制顺序**：通过 `@AutoConfigureOrder(-1)` 确保我们的配置在 Spring Security 自动配置之前加载
3. **定制灵活性**：可以完全控制安全过滤链的构建过程
4. **一键换包支持**：避免一键改包后，org.* 基础包无法生效的问题

### Token认证机制
1. 从请求头或参数中提取Token
2. 调用OAuth2服务验证Token有效性
3. 将用户信息存储到SecurityContext中
4. 支持开发环境的模拟登录功能（需通过配置开启）

### 安全上下文传播
使用 `TransmittableThreadLocalSecurityContextHolderStrategy` 来确保在异步处理（如Spring @Async、WebFlux等）场景下，Security Context 能够正确传播到子线程中。

## 注意事项

1. **生产环境安全**：生产环境必须将 `yudao.security.mock-enable` 设置为 `false`，并设置复杂的 `mock-secret` 值
2. **性能考虑**：Token验证会调用OAuth2服务，建议在OAuth2服务端实现适当的缓存机制
3. **异常处理**：认证和授权异常会通过全局异常处理器统一返回JSON格式的错误信息
4. **异步场景**：该实现已经考虑了异步场景的Context传播，但在使用自定义线程池时仍需注意传播机制

## 与相关模块的集成

### 与OAuth2模块的集成
Security模块依赖OAuth2模块的 `OAuth2TokenCommonApi` 来验证Token的有效性，获取用户信息。

### 与系统框架模块的集成
通过 `SecurityFrameworkService` 和 `SecurityFrameworkUtils` 与系统框架模块进行交互，获取权限信息、用户详情等。

### 与Web模块的集成
使用 `WebProperties` 获取应用的基础路径配置，构建完整的API路径用于权限匹配。

## 最佳实践

1. **最小权限原则**：只授予用户完成其工作所需的最小权限
2. **定期审计**：定期审查权限配置和访问日志
3. **密码策略**：生产环境中使用足够强度的密码加密（建议password-encoder-length >= 10）
4. **日志监控**：监控认证失败和授权拒绝事件，及时发现安全威胁
5. **避免硬编码**：所有安全相关配置都应通过外部配置文件管理，避免硬编码在代码中
6. **定期轮换**：定期更换密钥和敏感配置项

## 常见问题

### Q: 如何在微服务架构中使用此安全模块？
A: 在微服务架构中，每个服务都应该引入此安全模块，并配置相同的OAuth2服务地址，以实现统一的身份认证。

### Q: 如何自定义登录成功后的处理逻辑？
A: 可以通过实现 `AuthenticationSuccessHandler` 接口并将其注册到Spring Security中来自定义登录成功后的处理逻辑。

### Q: 为什么我的自定义过滤器没有生效？
A: 确保自定义过滤器的注册顺序正确，通常应该在 `UsernamePasswordAuthenticationFilter` 之前或之后，具体取决于过滤器的作用。

### Q: 如何禁用某个端点的所有安全检查？
A: 在对应的Controller或方法上添加 `@PermitAll` 注解，或者将该路径添加到 `yudao.security.permit-all-urls` 配置中。