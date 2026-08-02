# BPM 消息通知模块 (message_2)

## 概述

`BpmMessageServiceImpl` 是工作流（BPM）模块中的消息通知服务实现，负责在流程审批的关键生命周期节点自动向相关用户发送短信通知。该模块通过集成系统短信发送 API（`SmsSendApi`），在流程实例审批通过、审批驳回、任务分配、任务超时等场景下，向发起人或审批人发送模板化短信通知，实现流程状态的实时触达。

---

## 架构图

### 模块层级关系

```mermaid
graph TB
    subgraph "BPM 业务流程层"
        BpmProcessInstanceServiceImpl[流程实例服务]
        BpmTaskServiceImpl[任务服务]
    end

    subgraph "消息通知模块 (message_2)"
        BpmMessageServiceImpl[消息通知服务实现]
        BpmMessageConvert[消息转换器]
        BpmMessageEnum[消息枚举定义]
        DTOs[消息DTO]
    end

    subgraph "系统基础设施层"
        SmsSendApi[短信发送API]
        WebProperties[Web配置]
    end

    BpmProcessInstanceServiceImpl -->|审批通过/驳回时调用| BpmMessageServiceImpl
    BpmTaskServiceImpl -->|任务创建/超时时调用| BpmMessageServiceImpl
    BpmMessageServiceImpl --> BpmMessageConvert
    BpmMessageServiceImpl --> BpmMessageEnum
    BpmMessageServiceImpl --> DTOs
    BpmMessageServiceImpl --> SmsSendApi
    BpmMessageServiceImpl --> WebProperties
```

### 核心组件依赖关系

```mermaid
classDiagram
    class BpmMessageServiceImpl {
        - SmsSendApi smsSendApi
        - WebProperties webProperties
        + sendMessageWhenProcessInstanceApprove(reqDTO)
        + sendMessageWhenProcessInstanceReject(reqDTO)
        + sendMessageWhenTaskAssigned(reqDTO)
        + sendMessageWhenTaskTimeout(reqDTO)
        - getProcessInstanceDetailUrl(taskId)
    }

    class BpmMessageConvert {
        <<interface>>
        + convert(userId, templateCode, templateParams) SmsSendSingleToUserReqDTO
    }

    class BpmMessageEnum {
        <<enum>>
        + PROCESS_INSTANCE_APPROVE
        + PROCESS_INSTANCE_REJECT
        + TASK_ASSIGNED
        + TASK_TIMEOUT
    }

    class SmsSendApi {
        <<interface>>
        + sendSingleSmsToAdmin(reqDTO)
    }

    class WebProperties {
        + getAdminUi()
    }

    class DTO_Approve {
        + String processInstanceId
        + String processInstanceName
        + Long startUserId
    }

    class DTO_Reject {
        + String processInstanceId
        + String processInstanceName
        + Long startUserId
        + String reason
    }

    class DTO_TaskCreated {
        + String processInstanceId
        + String processInstanceName
        + Long startUserId
        + String startUserNickname
        + String taskId
        + String taskName
        + Long assigneeUserId
    }

    class DTO_TaskTimeout {
        + String processInstanceId
        + String processInstanceName
        + String taskId
        + String taskName
        + Long assigneeUserId
    }

    BpmMessageServiceImpl ..> BpmMessageConvert : 使用
    BpmMessageServiceImpl ..> BpmMessageEnum : 引用
    BpmMessageServiceImpl ..> DTO_Approve : 参数
    BpmMessageServiceImpl ..> DTO_Reject : 参数
    BpmMessageServiceImpl ..> DTO_TaskCreated : 参数
    BpmMessageServiceImpl ..> DTO_TaskTimeout : 参数
    BpmMessageServiceImpl --> SmsSendApi : 调用
    BpmMessageServiceImpl --> WebProperties : 读取
```

---

## 核心功能

### 1. 流程审批通过通知 (`sendMessageWhenProcessInstanceApprove`)

当流程实例被审批通过时，向流程发起人发送短信通知。

**通知参数：**
| 参数 | 说明 | 来源 |
|------|------|------|
| processInstanceName | 流程实例名称 | 请求DTO |
| detailUrl | 详情链接 | 通过 WebProperties 拼接生成 |

### 2. 流程审批驳回通知 (`sendMessageWhenProcessInstanceReject`)

当流程实例被审批驳回时，向流程发起人发送短信通知，包含驳回原因。

**通知参数：**
| 参数 | 说明 | 来源 |
|------|------|------|
| processInstanceName | 流程实例名称 | 请求DTO |
| reason | 驳回原因 | 请求DTO |
| detailUrl | 详情链接 | 通过 WebProperties 拼接生成 |

### 3. 任务分配通知 (`sendMessageWhenTaskAssigned`)

当任务被分配给审批人时，向审批人发送短信通知。

**通知参数：**
| 参数 | 说明 | 来源 |
|------|------|------|
| processInstanceName | 流程实例名称 | 请求DTO |
| taskName | 任务名称 | 请求DTO |
| startUserNickname | 发起人昵称 | 请求DTO |
| detailUrl | 详情链接 | 通过 WebProperties 拼接生成 |

### 4. 任务超时通知 (`sendMessageWhenTaskTimeout`)

当任务审批超时时，向审批人发送超时提醒短信。

**通知参数：**
| 参数 | 说明 | 来源 |
|------|------|------|
| processInstanceName | 流程实例名称 | 请求DTO |
| taskName | 任务名称 | 请求DTO |
| detailUrl | 详情链接 | 通过 WebProperties 拼接生成 |

---

## 消息类型枚举

```mermaid
graph LR
    subgraph "BpmMessageEnum"
        A[PROCESS_INSTANCE_APPROVE] -->|模板: bpm_process_instance_approve| SMS1[发送给申请人]
        B[PROCESS_INSTANCE_REJECT] -->|模板: bpm_process_instance_reject| SMS2[发送给申请人]
        C[TASK_ASSIGNED] -->|模板: bpm_task_assigned| SMS3[发送给审批人]
        D[TASK_TIMEOUT] -->|模板: bpm_task_timeout| SMS4[发送给审批人]
    end
```

每种消息类型对应一个短信模板标识（`smsTemplateCode`），关联 `SmsTemplateDO` 的 `code` 属性，通过系统短信模块进行模板渲染和发送。

---

## 数据流

### 流程审批消息发送时序

```mermaid
sequenceDiagram
    participant Caller as 流程/任务服务
    participant BpmMessageServiceImpl as 消息通知服务
    participant BpmMessageConvert as 消息转换器
    participant SmsSendApi as 短信发送API
    participant AdminUser as 管理员用户

    Caller->>BpmMessageServiceImpl: 调用消息发送方法
    Note over Caller,BpmMessageServiceImpl: 例如: sendMessageWhenTaskAssigned(reqDTO)

    BpmMessageServiceImpl->>BpmMessageServiceImpl: 组装 templateParams (模板参数)
    BpmMessageServiceImpl->>BpmMessageServiceImpl: 调用 getProcessInstanceDetailUrl() 生成详情链接
    BpmMessageServiceImpl->>BpmMessageConvert: convert(userId, templateCode, templateParams)
    BpmMessageConvert->>BpmMessageConvert: 转换为 SmsSendSingleToUserReqDTO
    BpmMessageConvert-->>BpmMessageServiceImpl: 返回转换后的DTO

    BpmMessageServiceImpl->>SmsSendApi: sendSingleSmsToAdmin(reqDTO)
    SmsSendApi->>SmsSendApi: 通过短信通道发送短信
    SmsSendApi-->>AdminUser: 管理员收到短信通知
    SmsSendApi-->>BpmMessageServiceImpl: 返回发送结果
```

### 详情链接生成逻辑

```mermaid
flowchart LR
    A[WebProperties.getAdminUi().getUrl()] --> B[拼接 /bpm/process-instance/detail?id=]
    C[流程实例ID] --> B
    B --> D[完整详情URL]
    D --> E[作为模板参数传入短信]
```

---

## 模块依赖

| 依赖模块 | 依赖组件 | 用途 |
|---------|---------|------|
| [system](system.md) | `SmsSendApi` | 短信发送接口，用于向管理员用户发送短信 |
| [system](system.md) | `SmsSendSingleToUserReqDTO` | 短信发送请求DTO |
| framework-web | `WebProperties` | 获取管理后台UI的基础URL，用于拼接流程详情链接 |
| [message_3](message_3.md) | `BpmMessageConvertImpl` | MapStruct 生成的转换器，将消息参数转换为短信发送请求DTO |
| 内部枚举 | `BpmMessageEnum` | 定义消息类型及对应的短信模板标识 |
| 内部DTO | `BpmMessageSendWhenProcessInstanceApproveReqDTO` | 审批通过通知请求参数 |
| 内部DTO | `BpmMessageSendWhenProcessInstanceRejectReqDTO` | 审批驳回通知请求参数 |
| 内部DTO | `BpmMessageSendWhenTaskCreatedReqDTO` | 任务分配通知请求参数 |
| 内部DTO | `BpmMessageSendWhenTaskTimeoutReqDTO` | 任务超时通知请求参数 |

---

## 调用链路

```mermaid
graph TB
    subgraph "调用方"
        A1[BpmProcessInstanceServiceImpl] -->|审批通过| B[审批通过通知]
        A2[BpmProcessInstanceServiceImpl] -->|审批驳回| C[审批驳回通知]
        A3[BpmTaskServiceImpl] -->|任务创建| D[任务分配通知]
        A4[BpmTaskServiceImpl] -->|任务超时| E[任务超时通知]
    end

    subgraph "消息服务"
        B --> BpmMessageServiceImpl
        C --> BpmMessageServiceImpl
        D --> BpmMessageServiceImpl
        E --> BpmMessageServiceImpl
    end

    subgraph "下游"
        BpmMessageServiceImpl --> SmsSendApi
        SmsSendApi --> SmsChannelImpl[短信通道实现]
        SmsChannelImpl --> ExternalSMS[外部短信服务商]
    end
```

---

## 配置说明

短信模块需要预先配置短信模板，模板标识与 `BpmMessageEnum` 中定义一致：

| 消息枚举 | 模板标识 | 模板参数 |
|---------|---------|---------|
| PROCESS_INSTANCE_APPROVE | `bpm_process_instance_approve` | processInstanceName, detailUrl |
| PROCESS_INSTANCE_REJECT | `bpm_process_instance_reject` | processInstanceName, reason, detailUrl |
| TASK_ASSIGNED | `bpm_task_assigned` | processInstanceName, taskName, startUserNickname, detailUrl |
| TASK_TIMEOUT | `bpm_task_timeout` | processInstanceName, taskName, detailUrl |

管理后台 UI 地址通过 `WebProperties` 配置，格式为：`${admin-ui-url}/bpm/process-instance/detail?id=${processInstanceId}`。

---

## 相关文档

- [流程实例服务](task_5.md) - BpmProcessInstanceServiceImpl，流程审批的调用方
- [任务服务](task_5.md) - BpmTaskServiceImpl，任务分配与超时的调用方
- [系统短信模块](dto_29.md) - SmsSendApi 及其实现
- [消息转换器](message_3.md) - BpmMessageConvertImpl
