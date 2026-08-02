# 表达式 EL 模块（expression_el）

## 概述

`expression_el` 模块是 RuoYi-Vue-Plus BPM 工作流引擎中 **Flowable EL（表达式语言）自定义函数** 的实现模块。它基于 Flowable 的 `AbstractFlowableVariableExpressionFunction` 抽象类，提供在流程表达式（如网关条件、任务监听器表达式）中可直接调用的自定义函数。

当前模块的核心功能是：

- **根据流程变量类型自动转换参数值**：`VariableConvertByTypeExpressionFunction` 提供 `convertByType` 函数，可根据流程变量的实际类型，将传入的参数值转换为匹配的类型。

> ⚠️ **注意**：该组件已标记为 `@Deprecated`（自代码标注起），预计在 2027 年移除。当前的审批候选人计算已统一迁移到 `BpmTaskCandidateStrategy` 策略模式实现。

## 架构位置

`expression_el` 模块在 BPM 工作流引擎中的位置如下：

```mermaid
graph TD
    subgraph "BPM 流程引擎（Flowable）"
        direction TB
        
        BpmFlowableConfiguration["BpmFlowableConfiguration<br/>流程引擎配置"]
        
        subgraph "表达式体系"
            direction LR
            ExpressionManagement["expression_management<br/>表达式管理（CRUD）"]
            ExpressionCandidate["expression_candidate<br/>候选表达式（已废弃示例）"]
            ExpressionEL["expression_el<br/>EL 自定义函数（当前模块）"]
        end
        
        subgraph "审批人策略体系"
            BpmTaskCandidateStrategy["BpmTaskCandidateStrategy<br/>策略接口"]
            DeptStrategies["部门策略组"]
            UserStrategies["用户策略组"]
            FormStrategies["表单策略组"]
            OtherStrategies["其他策略组"]
        end
    end

    ExpressionManagement -->|"定义表达式"| BpmProcessExpressionDO
    ExpressionEL -->|"提供自定义函数"| FlowableEngine["Flowable 引擎"]
    ExpressionCandidate -->|"已废弃示例"| FlowableEngine
    
    BpmFlowableConfiguration -->|"注册自定义函数"| ExpressionEL
    BpmFlowableConfiguration -->|"注册策略"| BpmTaskCandidateStrategy
    
    classDef deprecated fill:#fdd,stroke:#f66,stroke-width:2px
    class ExpressionCandidate,ExpressionEL deprecated
```

## 组件关系

```mermaid
classDiagram
    class AbstractFlowableVariableExpressionFunction {
        <<Flowable 抽象类>>
    }
    
    class VariableConvertByTypeExpressionFunction {
        +convertByType(VariableContainer, String, Object) Object
    }
    
    class BpmFlowableConfiguration {
        +bpmProcessEngineConfigurationConfigurer()
        +setCustomFlowableFunctionDelegates()
    }
    
    AbstractFlowableVariableExpressionFunction <|-- VariableConvertByTypeExpressionFunction
    BpmFlowableConfiguration --> VariableConvertByTypeExpressionFunction : 注册自定义函数
    VariableConvertByTypeExpressionFunction --> BpmProcessExpressionDO : 引用流程变量
```

## 核心组件说明

### 1. VariableConvertByTypeExpressionFunction

**文件路径**: `yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/framework/flowable/core/el/VariableConvertByTypeExpressionFunction.java`

**作用**：将流程 EL 表达式中的参数值，根据流程变量的实际类型进行类型转换。

**注册方式**：
- 通过 `@Component` 注解自动注册为 Spring Bean
- 在 `BpmFlowableConfiguration` 中，通过 `bpmProcessEngineConfigurationConfigurer()` 方法，将自定义函数委托注册到 Flowable 引擎的 `SpringProcessEngineConfiguration` 中：
  ```java
  configuration.setCustomFlowableFunctionDelegates(
      ListUtil.toList(customFlowableFunctionDelegates.stream().iterator())
  );
  ```

**函数签名**：
```java
public static Object convertByType(VariableContainer variableContainer, String variableName, Object parmaValue)
```

| 参数 | 类型 | 说明 |
|------|------|------|
| `variableContainer` | `VariableContainer` | Flowable 的变量容器，包含当前流程的所有变量 |
| `variableName` | `String` | 流程变量的名称 |
| `parmaValue` | `Object` | 需要转换的参数值 |

**转换逻辑**：
1. 从 `variableContainer` 中获取指定名称（`variableName`）的流程变量
2. 如果流程变量不为空且参数值不为空：
   - 如果参数值**不是**字符串类型，但流程变量是字符串类型 → 将参数值通过 `toString()` 转为字符串
3. 其他情况保持原值返回

**使用场景**（在 BPMN XML 中，通常在条件表达式中使用）：
```
${convertByType(execution, 'days', 5)}
```

### 2. 关联组件

| 组件 | 文件路径 | 关系说明 |
|------|---------|---------|
| `BpmFlowableConfiguration` | `bpm/framework/flowable/config/BpmFlowableConfiguration.java` | 流程引擎配置类，负责注册自定义函数到 Flowable |
| `BpmProcessExpressionDO` | `bpm/dal/dataobject/definition/BpmProcessExpressionDO.java` | 流程表达式数据对象，持久化表达式定义 |
| `BpmProcessExpressionServiceImpl` | `bpm/service/definition/BpmProcessExpressionServiceImpl.java` | 表达式管理服务，提供 CRUD 操作 |

## 数据流

```mermaid
sequenceDiagram
    participant U as 用户
    participant BPMN as BPMN 流程定义
    participant FE as Flowable 引擎
    participant VCF as VariableConvertByTypeExpressionFunction
    
    U->>BPMN: 设计流程，配置条件表达式
    BPMN->>FE: 部署流程定义
    Note over FE: 流程运行至网关/表达式节点
    FE->>VCF: 调用 convertByType(execution, varName, paramValue)
    VCF->>VCF: 获取流程变量 variableContainer.getVariable(variableName)
    alt 流程变量是 String 类型且参数值非 String
        VCF->>VCF: 将参数值 toString() 转换
    else 其他情况
        VCF->>VCF: 保持原值返回
    end
    VCF-->>FE: 返回转换后的值
    FE-->>BPMN: 根据返回值决定流程走向
```

## 与相关模块的集成

### 与其他 BPM 模块的联系

```mermaid
graph LR
    subgraph "expression_el（当前模块）"
        VCF["VariableConvertByTypeExpressionFunction"]
    end
    
    subgraph "expression_management"
        Controller["BpmProcessExpressionController"]
        Service["BpmProcessExpressionService"]
    end
    
    subgraph "expression_candidate"
        LeaderExp["BpmTaskAssignLeaderExpression（已废弃）"]
        StartUserExp["BpmTaskAssignStartUserExpression（已废弃）"]
    end
    
    subgraph "candidate 策略体系"
        Strategy["BpmTaskCandidateStrategy"]
        ExpStrategy["BpmTaskCandidateExpressionStrategy"]
    end

    VCF -.->|"已废弃"| LeaderExp
    VCF -.->|"已废弃"| StartUserExp
    Controller --> Service
    ExpStrategy --> VCF
```

### 相关文档参考

- [表达式管理模块](expression_management.md) — 表达式的 CRUD 管理界面与后台服务
- [表达式候选人模块](expression_candidate.md) — 候选表达式的实现示例（已废弃）
- [BPM 流程引擎配置](../bpm_flowable_config.md) — `BpmFlowableConfiguration` 的完整配置说明

## 配置说明

自定义函数通过 `BpmFlowableConfiguration` 注册到 Flowable 引擎，无需独立配置。`VariableConvertByTypeExpressionFunction` 作为 Spring Bean 被自动扫描并加载。

```java
// BpmFlowableConfiguration 中的注册逻辑
@Bean
public EngineConfigurationConfigurer<SpringProcessEngineConfiguration> bpmProcessEngineConfigurationConfigurer(
        ObjectProvider<FlowableEventListener> listeners,
        ObjectProvider<FlowableFunctionDelegate> customFlowableFunctionDelegates,
        BpmActivityBehaviorFactory bpmActivityBehaviorFactory) {
    return configuration -> {
        // ... 其他配置
        configuration.setCustomFlowableFunctionDelegates(
            ListUtil.toList(customFlowableFunctionDelegates.stream().iterator())
        );
    };
}
```

## 废弃说明

`VariableConvertByTypeExpressionFunction` 已标记为 `@Deprecated`，原因如下：

1. **类型转换不再需要**：Flowable 引擎后续版本已经内置了类型转换能力
2. **表达式函数被策略模式替代**：审批人计算已从 EL 表达式迁移到 `BpmTaskCandidateStrategy` 策略模式
3. **维护负担**：无调用方的代码徒增维护成本

> 🗓️ 预计移除时间：2027 年（代码注释标注）
