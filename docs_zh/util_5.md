# TracerFrameworkUtils 链路追踪工具类

## 模块概述

TracerFrameworkUtils 是 Yudao 框架监控模块中的链路追踪工具类，位于 `yudao-framework/yudao-spring-boot-starter-monitor` 模块中。该工具类提供了将异常信息记录到 OpenTelemetry Span 中的功能，是框架链路追踪体系的重要组成部分。

该工具类主要用于在业务切面（BizTraceAspect）中捕获异常并将其记录到追踪 Span 中，从而在分布式追踪系统中能够看到完整的错误堆栈信息。

## 系统架构

```mermaid
graph TD
    A[业务方法] --> B{BizTraceAspect 切面}
    B -->|正常执行| C[返回结果]
    B -->|异常发生| D[TracerFrameworkUtils.onError()]
    D --> E[记录异常到 Span]
    E --> F[设置 Span 状态为 ERROR]
    E --> G[记录异常堆栈]
    F --> H[结束 Span]
    G --> H
    H --> I[OpenTelemetry 追踪系统]
    
    subgraph 监控模块组件
        J[YudaoTracerAutoConfiguration]
        K[TracerProperties]
        L[YudaoMetricsAutoConfiguration]
        M[TraceFilter]
        N[TracerFrameworkUtils]
    end
    
    style N fill:#f9f,stroke:#333
```

## 核心功能

TracerFrameworkUtils 提供以下核心功能：

1. **异常记录到 Span**：将 Java 异常信息记录到 OpenTelemetry Span 中
2. **状态设置**：根据异常情况设置 Span 的状态为 ERROR
3. **堆栈追踪记录**：将异常的完整堆栈信息作为属性记录到 Span 中
4. **原因异常处理**：优先使用异常的根 cause 消息，如果不存在则使用异常自身消息

## 详细设计

### 类结构

```java
package cn.iocoder.yudao.framework.tracer.core.util;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * 链路追踪 Util
 *
 * @author 芋道源码
 */
public class TracerFrameworkUtils {

    /**
     * 将异常记录到 Span 中，参考自 com.aliyuncs.utils.TraceUtils
     *
     * @param throwable 异常
     * @param span Span
     */
    public static void onError(Throwable throwable, Span span) {
        // 忽略无效 Span
        if (span == null || !span.getSpanContext().isValid()) {
            return;
        }
        // 标记异常状态
        if (throwable == null) {
            span.setStatus(StatusCode.ERROR);
            return;
        }

        // 记录异常事件
        span.recordException(throwable);
        String message = throwable.getCause() != null ? throwable.getCause().getMessage() : throwable.getMessage();
        span.setStatus(StatusCode.ERROR, message == null ? "" : message);
        // 记录异常堆栈
        StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));
        span.setAttribute("error.stack", sw.toString());
    }

}
```

### 关键实现细节

1. **Span 有效性检查**：在记录异常之前，首先检查 Span 是否为 null 或无效，避免空指针异常
2. **空异常处理**：当传入的异常为 null 时，仅将 Span 状态设置为 ERROR，不记录其他信息
3. **异常事件记录**：使用 `span.recordException(throwable)` 记录异常事件
4. **状态设置**：将 Span 状态设置为 ERROR，并使用异常消息（优先使用根 cause 的消息）作为状态描述
5. **堆栈追踪**：将完整的异常堆栈信息写入 StringWriter，然后作为名为 "error.stack" 的属性记录到 Span 中

## 架构集成

TracerFrameworkUtils 在 Yudao 框架的链路追踪体系中扮演着重要角色，主要与以下组件协作：

### 与 BizTraceAspect 的集成

TracerFrameworkUtils 被 BizTraceAspect 切面类调用，用于在业务方法执行过程中捕获异常并记录到追踪 Span 中：

```java
@Around(value = "@annotation(trace)")
public Object around(ProceedingJoinPoint joinPoint, BizTrace trace) throws Throwable {
    // 创建 span
    String operationName = getOperationName(joinPoint, trace);
    Span span = tracer.spanBuilder(operationName)
            .setAttribute("component", "biz")
            .startSpan();
    try (Scope ignored = span.makeCurrent()) {
        // 执行原有方法
        return joinPoint.proceed();
    } catch (Throwable throwable) {
        // 关键点：使用 TracerFrameworkUtils 记录异常到 Span
        TracerFrameworkUtils.onError(throwable, span);
        throw throwable;
    } finally {
        // 设置 Span 的 biz 属性
        setBizTag(span, joinPoint, trace);
        // 完成 Span
        span.end();
    }
}
```

### 与监控模块的关系

TracerFrameworkUtils 属于 yudao-spring-boot-starter-monitor 模块，该模块还包含：
- YudaoTracerAutoConfiguration：OpenTelemetry 自动配置类
- YudaoMetricsAutoConfiguration：Metrics 自动配置类
- TracerProperties：追踪配置属性类
- TraceFilter：用于在 HTTP 响应头中设置 trace-id 的过滤器

## 使用场景

TracerFrameworkUtils 主要用于以下场景：

1. **业务方法异常追踪**：在使用 @BizTrace 注解的业务方法中，当方法抛出异常时，自动将异常信息记录到对应的追踪 Span
2. **分布式系统错误诊断**：在微服务架构中，通过追踪系统可以跟踪请求在各服务之间的传播路径，而异常信息的记录有助于快速定位问题根源
3. **日志与追踪关联**：将异常的完整堆栈信息记录到 Span 中，使得在追踪系统中查看 Span 时能够看到详细的错误信息，而不仅仅是错误消息

## 与其他模块的关系

TracerFrameworkUtils 与以下模块和组件协作工作：

| 模块/组件 | 关系 | 说明 |
|----------|------|------|
| BizTraceAspect | 调用关系 | BizTraceAspect 切面在捕获到异常后调用 TracerFrameworkUtils.onError 方法 |
| YudaoTracerAutoConfiguration | 同模块 | 提供 Tracer Bean 的自动配置 |
| TraceFilter | 同模块 | 负责在 HTTP 响应中设置 trace-id |
| TracerUtils | 间接关联 | 提供获取当前 trace-id 的工具方法（在 TraceFilter 中使用） |
| OpenTelemetry API | 依赖关系 | 使用 io.opentelemetry.api.trace.Span 和 StatusCode 等核心类 |

## 最佳实践

1. **异常不为 null 的前提**：虽然方法内部有 null 检查，但在调用时最好确保传入的异常不为 null，以获得完整的错误信息记录
2. **Span 有效性**：确保在记录异常时 Span 仍然有效，避免在 Span 已经结束后尝试记录异常
3. **性能考虑**：异常堆栈的获取和字符串写入是相对昂贵的操作，但由于仅在异常情况下执行，对正常路径性能影响可忽略不计
4. **敏感信息注意**：异常消息和堆栈中可能包含敏感信息，在生产环境中使用时需注意信息安全

## 示例代码

虽然 TracerFrameworkUtils 通常不被直接调用，但以下示例展示了其使用方式：

```java
import io.opentelemetry.api.trace.Span;
import cn.iocoder.yudao.framework.tracer.core.util.TracerFrameworkUtils;

// 假设我们已经获得了一个有效的 Span
Span span = ...;

// 正常情况下的示例：记录一个实际的异常
try {
    // 可能抛出异常的业务逻辑
    riskyOperation();
} catch (Exception e) {
    // 使用 TracerFrameworkUtils 记录异常到 Span
    TracerFrameworkUtils.onError(e, span);
    // 重新抛出异常或进行其他处理
    throw e;
}

// 特殊情况：当异常为 null 时
TracerFrameworkUtils.onError(null, span); // 仅将 Span 状态设置为 ERROR
```

## 总结

TracerFrameworkUtils 是 Yudao 框架监控模块中的一个小而专注的工具类，专门用于将异常信息记录到 OpenTelemetry Span 中。尽管其实现相对简单，但它在框架的链路追踪体系中发挥着重要作用，使得开发者和运维人员能够在分布式追踪系统中看到完整的异常堆栈信息，从而快速定位和解决生产环境中的问题。

该工具类体现了 Yudao 框架在可观测性方面的设计理念：通过自动化的方式将关键的运维信息（如异常堆栈）与追踪数据关联，而无需业务开发者手动插入复杂的追踪代码。