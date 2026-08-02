# 客户模块 (Customer Module)

## 1. 模块概述

客户模块是 CRM 系统中的核心模块之一，主要负责客户信息的管理、客户公海规则的配置以及客户限制规则的设置。通过该模块，企业可以实现客户资源的合理分配、跟踪和管理，提高销售效率。

## 2. 架构概览

客户模块采用分层架构，主要包括以下层次：

- **Controller 层**：负责接收前端请求，调用 Service 层处理业务逻辑，并返回响应。
- **Service 层**：处理具体的业务逻辑，如客户创建、更新、删除、公海操作等。
- **DAO 层**：负责与数据库交互，进行数据的持久化操作。
- **VO/DO 层**：VO (View Object) 用于前端展示，DO (Data Object) 用于数据库存储。

### 架构图

```
前端 --> Controller 层 --> Service 层 --> DAO 层 --> 数据库
Controller 层 --> VO
DAO 层 --> DO
```

## 3. 功能模块

客户模块包含以下三个子模块：

### 3.1 客户公海配置 (Customer Pool Configuration)

负责配置客户公海的规则，如客户进入公海的条件、时间等。详见 [客户公海配置文档](customer_pool_config.md)。

### 3.2 客户管理 (Customer Management)

提供客户信息的增删改查、导入导出、转移、锁定、公海操作等功能。详见 [客户管理文档](customer_management.md)。

### 3.3 客户限制配置 (Customer Limit Configuration)

负责配置客户管理的限制规则，如哪些用户和部门可以管理特定的客户。详见 [客户限制配置文档](customer_limit_config.md)。

## 4. 与其他模块的集成

- **系统模块 (System Module)**：用户、部门、权限等基础数据由系统模块提供，客户模块通过 API 调用系统模块的数据。
- **CRM 模块 (CRM Module)**：客户模块是 CRM 模块的一部分，与 CRM 中的其他模块（如线索、合同等）有数据关联。
- **消息模块 (Message Module)**：在客户操作过程中，可能会触发消息通知，与消息模块集成。

## 5. 参考文档

- [CRM 模块文档](crm.md)
- [系统模块文档](system.md)