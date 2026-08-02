# WenDuoDuo PPT API 模块文档

## 1. 模块概述

WenDuoDuo PPT API 模块是 Yudao AI 模块中用于集成文多多（Docmee）PPT 生成服务的核心组件。该模块提供了与文多多 PPT 生成 API 的完整交互能力，支持 PPT 模板查询、大纲内容生成、PPT 文件创建等功能。

### 主要功能
- PPT 模板管理：分页查询 PPT 模板，支持过滤条件
- 大纲内容生成：根据指定参数生成 PPT 大纲内容
- PPT 文件创建：基于模板和大纲生成最终 PPT 文件
- Token 管理：获取和刷新 API 访问令牌

### 架构定位

WenDuoDuoPptApi 模块位于 AI 框架层，主要被 AI 业务模块（如写作、工作流等）调用，提供 PPT 生成功能。

```
┌─────────────────────────────────────────────────────────┐
│              AI 业务模块 (AiWrite, AiWorkflow)          │
│                    ↓ 依赖调用                         │
└─────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────┐
│         WenDuoDuo PPT API 模块 (WenDuoDuoPptApi)       │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐     │
│  │ 请求参数对象 │  │ 响应结果对象 │  │ 工具类       │     │
│  └─────────────┘  └─────────────┘  └─────────────┘     │
│                    ↓ HTTP 调用                         │
└─────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────┐
│           文多多 PPT 生成服务 (Docmee Service)          │
└─────────────────────────────────────────────────────────┘

## 2. 核心类说明

### WenDuoDuoPptApi

**描述**: 文多多 PPT API 客户端，负责与文多多 PPT 生成服务进行交互。

**依赖关系**:
- 使用 `WebClient` 进行 HTTP 请求
- 使用 `JsonUtils` 进行 JSON 序列化/反序列化
- 使用 `Assert` 进行参数校验

**主要方法**:

| 方法名 | 描述 | 返回类型 |
|--------|------|----------|
| `createApiToken` | 创建 API Token | String |
| `createTask` | 创建 PPT 生成任务 | ApiResponse |
| `getOptions` | 获取生成选项 | Map<String, Object> |
| `getTemplatePage` | 分页查询 PPT 模板 | PagePptTemplateInfo |
| `createOutline` | 生成大纲内容流 | Flux<Map<String, Object>> |
| `updateOutline` | 修改大纲内容流 | Flux<Map<String, Object>> |
| `create` | 生成 PPT 文件 | PptInfo |

**代码示例**:
```java
// 初始化 WenDuoDuoPptApi
WenDuoDuoPptApi pptApi = new WenDuoDuoPptApi("your-api-token");

// 创建 PPT
PptCreateRequest request = new PptCreateRequest(
    "task-id", 
    "template-id", 
    "# 标题\n\n内容"
);
PptInfo pptInfo = pptApi.create(request);
```

### 请求参数对象

所有请求参数均使用 Java Record 定义，包含以下主要类型：

#### CreateTokenRequest
**描述**: 创建 Token 的请求参数

```java
public record CreateTokenRequest(
    String apiKey,      // API Key
    String uid,         // 用户 ID (可选)
    Integer limit       // 限制数量 (可选)
) { }
```

#### TemplateQueryRequest
**描述**: 模板查询请求参数

```java
public record TemplateQueryRequest(
    int page,           // 页码
    int size,           // 每页大小
    Filter filters      // 过滤条件
) { }

public record Filter(
    int type,           // 模板类型
    String category,    // 分类
    String style,       // 风格
    String themeColor   // 主题色
) { }
```

#### PptCreateRequest
**描述**: 创建 PPT 的请求参数

```java
public record PptCreateRequest(
    String id,          // 任务 ID
    String templateId,  // 模板 ID
    String markdown     // Markdown 内容
) { }
```

### 响应结果对象

#### ApiResponse
**描述**: API 通用响应格式

```java
public record ApiResponse(
    Integer code,       // 状态码
    String message,     // 消息
    Map<String, Object> data // 数据
) { }
```

#### PptInfo
**描述**: PPT 信息实体

```java
public record PptInfo(
    String id,          // PPT ID
    String name,        // 名称
    String subject,     // 主题
    String coverUrl,    // 封面 URL
    String fileUrl,     // 文件 URL
    String templateId,  // 模板 ID
    String pptxProperty, // PPTX 属性
    String userId,      // 用户 ID
    String userName,    // 用户名
    int companyId,      // 公司 ID
    LocalDateTime updateTime, // 更新时间
    LocalDateTime createTime,  // 创建时间
    String createUser,  // 创建者
    String updateUser   // 更新者
) { }
```

#### PagePptTemplateInfo
**描述**: PPT 模板分页信息

```java
public record PagePptTemplateInfo(
    List<PptTemplateInfo> data,   // 模板列表
    String total                // 总数
) { }
```

## 3. API 接口流程

### PPT 生成流程

```
客户端 → WenDuoDuoPptApi: 创建 PPT 请求 (PptCreateRequest)
WenDuoDuoPptApi → DocmeeService: POST /api/ppt/v2/generatePptx
DocmeeService → WenDuoDuoPptApi: 响应 (ApiResponse)
WenDuoDuoPptApi → 客户端: PptInfo 对象

说明: 响应中包含 PPT 文件下载链接等信息
```

### 大纲内容生成流程

```
客户端 → WenDuoDuoPptApi: 创建大纲请求 (CreateOutlineRequest)
WenDuoDuoPptApi → DocmeeService: POST /api/ppt/v2/generateContent
DocmeeService → WenDuoDuoPptApi: 内容流 (Flux<Map<String, Object>>)
WenDuoDuoPptApi → 客户端: 实时内容流

说明: 支持流式处理，逐步获取生成的内容
```

## 4. 错误处理机制

WenDuoDuoPptApi 实现了统一的错误处理策略：

1. **状态码检查**: 使用 `STATUS_PREDICATE` 判断 HTTP 响应是否成功
2. **异常日志**: 记录详细的请求和响应信息
3. **错误封装**: 抛出 `IllegalStateException` 并包含错误详情

```java
private final Function<Object, Function<ClientResponse, Mono<? extends Throwable>>> EXCEPTION_FUNCTION =
    reqParam -> response -> response.bodyToMono(String.class).handle((responseBody, sink) -> {
        HttpRequest request = response.request();
        log.error("[WenDuoDuoPptApi] 调用失败！请求方式:[{}]，请求地址:[{}]，请求参数:[{}]，响应数据: [{}]",
                request.getMethod(), request.getURI(), reqParam, responseBody);
        sink.error(new IllegalStateException("[WenDuoDuoPptApi] 调用失败！"));
    });
```

## 5. 配置和使用

### Maven 依赖
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>
```

### Spring Boot 配置
```yaml
wen-duo-duo:
  api:
    token: your-api-token
    base-url: https://docmee.cn
```

### Bean 注册
```java
@Configuration
public class WenDuoDuoPptApiConfiguration {
    
    @Value("${wen-duo-duo.api.token}")
    private String token;
    
    @Bean
    public WenDuoDuoPptApi wenDuoDuoPptApi() {
        return new WenDuoDuoPptApi(token);
    }
}
```

## 6. 与其他模块的关系

### 依赖关系
- **AI 模块**: `AiWriteController`, `AiWorkflowController` 使用该 API 生成 PPT
- **工具模块**: 使用 `JsonUtils` 进行 JSON 处理
- **Web 模块**: 通过 WebClient 进行 HTTP 调用

### 被依赖关系
- 作为 AI 模块的工具类，为其他 AI 功能提供 PPT 生成能力

## 7. 注意事项

1. **Token 管理**: 需要确保 Token 的有效性，过期时需要重新创建
2. **流式处理**: 大纲生成功能返回的是 Flux，需要正确处理流式响应
3. **文件上传**: 创建任务时支持多文件上传，注意 MultipartFile 的处理
4. **异常处理**: 生产环境中建议捕获并处理 IllegalStateException
5. **性能优化**: 对于大量模板查询，建议使用分页查询

## 8. 参考文档

- [文多多 PPT 生成 API 文档](https://docmee.cn/open-platform/api)
- [Spring WebClient 官方文档](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/reactive/function/client/WebClient.html)
- [Reactor Flux 文档](https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Flux.html)
