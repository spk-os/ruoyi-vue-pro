# 业务控制器模块文档 (Business Controller Module)

## 1. 模块概述

业务控制器模块是 CRM（客户关系管理）系统中的核心模块，主要负责**商机（Business Opportunity）**的管理。该模块提供了完整的商机生命周期管理功能，包括商机的创建、查询、更新、删除、转移以及商机状态的管理。

### 模块定位

业务控制器模块包含两个主要控制器：
- **CrmBusinessController**: 负责商机的 CRUD、查询、导出和转移操作
- **CrmBusinessStatusController**: 负责商机状态组和具体状态的管理

模块架构遵循标准的三层架构：表现层（Controller）→ 业务层（Service）→ 数据访问层（DAO/DO）。

### 核心功能

- **商机管理**：创建、查询、更新、删除、转移商机
- **商机状态管理**：维护商机状态组（如：潜在客户、已成交、已流失等）及其具体状态
- **数据导出**：支持商机数据的 Excel 导出
- **关联查询**：基于客户、联系人等维度查询商机

---

## 2. 架构设计

### 2.1 整体架构

业务控制器模块采用标准的三层架构：

```
表现层 (Controller)
├── CrmBusinessController: 商机管理
└── CrmBusinessStatusController: 状态组管理

业务层 (Service)
├── CrmBusinessService: 商机业务逻辑
└── CrmBusinessStatusService: 状态组业务逻辑

数据访问层 (DAO/DO)
├── CrmBusinessDO: 商机数据对象
├── CrmBusinessProductDO: 商机产品数据对象
├── CrmBusinessStatusTypeDO: 商机状态组数据对象
└── CrmBusinessStatusDO: 商机状态数据对象
```

### 2.2 组件关系

- CrmBusinessController 依赖 CrmBusinessService 处理商机业务
- CrmBusinessStatusController 依赖 CrmBusinessStatusService 处理状态组业务
- 两个控制器都依赖系统模块的 DeptApi 和 AdminUserApi 获取部门和用户信息
- CrmBusinessService 依赖 CrmCustomerService、CrmProductService 获取关联数据
- 服务层通过 Mapper 接口与数据库交互

---

## 3. 核心组件说明

### 3.1 CrmBusinessController

**路径**: `yudao-module-crm/src/main/java/cn/iocoder/yudao/module/crm/controller/admin/business/CrmBusinessController.java`

**功能描述**: 提供商机的完整 CRUD 操作、分页查询、导出 Excel、商机转移等功能。

#### 接口列表

| HTTP 方法 | 路径 | 描述 | 权限 |
|-----------|------|------|------|
| POST | `/crm/business/create` | 创建商机 | `crm:business:create` |
| PUT | `/crm/business/update` | 更新商机 | `crm:business:update` |
| PUT | `/crm/business/update-status` | 更新商机状态 | `crm:business:update` |
| DELETE | `/crm/business/delete` | 删除商机 | `crm:business:delete` |
| GET | `/crm/business/get` | 获取商机详情 | `crm:business:query` |
| GET | `/crm/business/simple-all-list` | 获取商机精简列表 | `crm:business:query` |
| GET | `/crm/business/list-by-customer` | 按客户获取商机列表 | `crm:business:query` |
| GET | `/crm/business/list-by-contact` | 按联系人获取商机列表 | `crm:business:query` |
| GET | `/crm/business/page` | 获取商机分页 | `crm:business:query` |
| GET | `/crm/business/page-by-customer` | 按客户获取商机分页 | - |
| GET | `/crm/business/page-by-contact` | 按联系人获取商机分页 | `crm:business:query` |
| GET | `/crm/business/export-excel` | 导出商机 Excel | `crm:business:export` |
| PUT | `/crm/business/transfer` | 商机转移 | `crm:business:update` |

#### 关键方法说明

- **`createBusiness()`**: 创建新的商机，验证请求参数后调用服务层创建
- **`updateBusiness()`**: 更新现有商机信息
- **`updateBusinessStatus()`**: 更新商机的状态（如从"潜在客户"变为"已成交"）
- **`getBusiness()`**: 获取商机详细信息，包括关联的产品信息
- **`buildBusinessDetail()`**: 构建商机详情 VO，关联查询客户、创建人、负责人、部门、状态等信息
- **`exportBusinessExcel()`**: 导出商机数据到 Excel 文件
- **`transferBusiness()`**: 将商机转移给其他负责人

### 3.2 CrmBusinessStatusController

**路径**: `yudao-module-crm/src/main/java/cn/iocoder/yudao/module/crm/controller/admin/business/CrmBusinessStatusController.java`

**功能描述**: 管理商机状态组（Status Type）和具体状态（Status）。商机状态组用于分类管理不同阶段的商机状态。

#### 接口列表

| HTTP 方法 | 路径 | 描述 | 权限 |
|-----------|------|------|------|
| POST | `/crm/business-status/create` | 创建商机状态组 | `crm:business-status:create` |
| PUT | `/crm/business-status/update` | 更新商机状态组 | `crm:business-status:update` |
| DELETE | `/crm/business-status/delete` | 删除商机状态组 | `crm:business-status:delete` |
| GET | `/crm/business-status/get` | 获取商机状态组详情 | `crm:business-status:query` |
| GET | `/crm/business-status/page` | 获取商机状态组分页 | `crm:business-status:query` |
| GET | `/crm/business-status/type-simple-list` | 获取可用的商机状态组列表 | - |
| GET | `/crm/business-status/status-simple-list` | 获取指定状态组下的具体状态列表 | - |

#### 关键方法说明

- **`createBusinessStatus()`**: 创建新的商机状态组
- **`getBusinessStatusType()`**: 获取状态组详情，包含该组下的所有具体状态
- **`getBusinessStatusPage()`**: 分页查询状态组，关联查询创建人和所属部门
- **`getBusinessStatusTypeSimpleList()`**: 获取当前用户部门可用的状态组列表（过滤掉不匹配的部门）
- **`getBusinessStatusSimpleList()`**: 获取指定状态组下的所有具体状态

---

## 4. 数据对象与视图对象

### 4.1 数据对象 (DO)

**关系说明**：
- CrmBusinessDO 与 CrmBusinessProductDO：一对多关系（一个商机包含多个产品）
- CrmBusinessDO 与 CrmCustomerDO：属于关系（商机属于某个客户）
- CrmBusinessDO 与 CrmBusinessStatusTypeDO：状态组关系（商机属于某个状态组）
- CrmBusinessDO 与 CrmBusinessStatusDO：当前状态关系（商机当前处于某个状态）
- CrmBusinessStatusTypeDO 与 CrmBusinessStatusDO：一对多关系（一个状态组包含多个具体状态）

**CrmBusinessDO 关键字段**：
- id: 主键
- name: 商机名称
- customerId: 客户ID
- contactId: 联系人ID
- creator: 创建人
- ownerUserId: 负责人ID
- statusTypeId: 状态组ID
- statusId: 状态ID
- endStatus: 是否结束（0=未结束，1=结束）
- probability: 成交概率（0-100）
- closeTime: 关闭时间
- expectedRevenue: 预期收入
- actualRevenue: 实际收入
- description: 描述

**CrmBusinessProductDO 关键字段**：
- id: 主键
- businessId: 商机ID
- productId: 产品ID
- quantity: 数量
- unitPrice: 单价

**CrmBusinessStatusTypeDO 关键字段**：
- id: 主键
- name: 状态组名称（如：销售阶段、售前阶段）
- creator: 创建人
- deptIds: 关联的部门ID列表

**CrmBusinessStatusDO 关键字段**：
- id: 主键
- typeId: 状态组ID
- name: 状态名称（如：潜在客户、方案报价、已成交）
- isEnd: 是否结束状态（true表示该状态为结束状态）

### 4.2 视图对象 (VO)

#### 请求 VO (Request VO)

| VO 名称 | 用途 |
|---------|------|
| `CrmBusinessSaveReqVO` | 创建/更新商机的请求参数 |
| `CrmBusinessUpdateStatusReqVO` | 更新商机状态的请求参数 |
| `CrmBusinessPageReqVO` | 分页查询商机的请求参数 |
| `CrmBusinessTransferReqVO` | 商机转移的请求参数 |
| `CrmBusinessStatusSaveReqVO` | 创建/更新状态组的请求参数 |

#### 响应 VO (Response VO)

| VO 名称 | 用途 |
|---------|------|
| `CrmBusinessRespVO` | 商机详情响应，包含产品信息 |
| `CrmBusinessRespVO.Product` | 商机中的产品信息 |
| `CrmBusinessStatusRespVO` | 状态组响应，包含状态列表 |
| `CrmBusinessStatusRespVO.Status` | 具体状态信息 |

---

## 5. 服务层说明

### 5.1 CrmBusinessService

**路径**: `yudao-module-crm/src/main/java/cn/iocoder/yudao/module/crm/service/business/CrmBusinessServiceImpl.java`

**核心方法**:

- `createBusiness(CrmBusinessSaveReqVO, Long)`: 创建商机，关联产品和负责人
- `updateBusiness(CrmBusinessSaveReqVO)`: 更新商机信息
- `updateBusinessStatus(CrmBusinessUpdateStatusReqVO)`: 更新商机状态
- `deleteBusiness(Long)`: 删除商机
- `getBusiness(Long)`: 获取商机详情
- `getBusinessPage(CrmBusinessPageReqVO, Long)`: 分页查询商机
- `getBusinessListByCustomerId(Long)`: 按客户 ID 获取商机列表
- `getBusinessListByContact(Long)`: 按联系人 ID 获取商机列表
- `getBusinessProductListByBusinessId(Long)`: 获取商机的产品列表
- `getBusinessStatusName(Long, Integer)`: 获取状态名称
- `transferBusiness(CrmBusinessTransferReqVO, Long)`: 转移商机给其他负责人

### 5.2 CrmBusinessStatusService

**路径**: `yudao-module-crm/src/main/java/cn/iocoder/yudao/module/crm/service/business/CrmBusinessStatusServiceImpl.java`

**核心方法**:

- `createBusinessStatus(CrmBusinessStatusSaveReqVO)`: 创建状态组
- `updateBusinessStatus(CrmBusinessStatusSaveReqVO)`: 更新状态组
- `deleteBusinessStatusType(Long)`: 删除状态组
- `getBusinessStatusType(Long)`: 获取状态组详情
- `getBusinessStatusTypePage(PageParam)`: 分页查询状态组
- `getBusinessStatusTypeList()`: 获取所有状态组列表
- `getBusinessStatusListByTypeId(Long)`: 获取指定状态组下的状态列表
- `getBusinessStatusMap(Set<Long>)`: 获取状态 ID 到状态的映射
- `getBusinessStatusTypeMap(Set<Long>)`: 获取状态组 ID 到状态组的映射

---

## 6. 权限控制

业务控制器模块使用 Spring Security 的 `@PreAuthorize` 注解进行权限控制，通过自定义的 `ss.hasPermission('xxx')` 表达式进行权限校验。

### 权限点说明

| 权限点 | 描述 | 适用接口 |
|--------|------|----------|
| `crm:business:create` | 创建商机 | `createBusiness` |
| `crm:business:update` | 更新商机 | `updateBusiness`, `updateBusinessStatus`, `transferBusiness` |
| `crm:business:delete` | 删除商机 | `deleteBusiness` |
| `crm:business:query` | 查询商机 | `getBusiness`, `getSimpleContactList`, `getBusinessListByCustomer`, `getBusinessListByContact`, `getBusinessPage`, `getBusinessPageByCustomer`, `getBusinessContactPage` |
| `crm:business:export` | 导出商机 | `exportBusinessExcel` |
| `crm:business-status:create` | 创建状态组 | `createBusinessStatus` |
| `crm:business-status:update` | 更新状态组 | `updateBusinessStatus` |
| `crm:business-status:delete` | 删除状态组 | `deleteBusinessStatusType` |
| `crm:business-status:query` | 查询状态组 | `getBusinessStatusType`, `getBusinessStatusPage`, `getBusinessStatusTypeSimpleList`, `getBusinessStatusSimpleList` |

---

## 7. 数据流分析

### 7.1 创建商机数据流

```
用户 ->> CrmBusinessController: POST /crm/business/create (CrmBusinessSaveReqVO)
CrmBusinessController ->> CrmBusinessService: createBusiness(reqVO, userId)
CrmBusinessService ->> CrmBusinessMapper: insert(businessDO)
CrmBusinessMapper -->> CrmBusinessService: 插入成功，返回id
CrmBusinessService ->> CrmBusinessProductMapper: insert批量产品记录
CrmBusinessProductMapper -->> CrmBusinessService: 插入成功
CrmBusinessService -->> CrmBusinessController: 返回商机ID
CrmBusinessController -->> User: CommonResult<Long>
```

### 7.2 获取商机详情数据流

```
用户 ->> CrmBusinessController: GET /crm/business/get?id=123
CrmBusinessController ->> CrmBusinessService: getBusiness(id)
CrmBusinessService ->> CrmBusinessMapper: selectById(id)
CrmBusinessMapper -->> CrmBusinessService: 返回CrmBusinessDO
alt 构建详情
    CrmBusinessService ->> CrmCustomerService: getCustomerMap(customerIds)
    CrmCustomerService -->> CrmBusinessService: 返回客户Map
    CrmBusinessService ->> AdminUserApi: getUserMap(creator, ownerUserId)
    AdminUserApi -->> CrmBusinessService: 返回用户Map
    CrmBusinessService ->> DeptApi: getDeptMap(deptIds)
    DeptApi -->> CrmBusinessService: 返回部门Map
    CrmBusinessService ->> CrmBusinessStatusTypeService: getStatusTypeMap(statusTypeIds)
    CrmBusinessStatusTypeService -->> CrmBusinessService: 返回状态组Map
    CrmBusinessService ->> CrmBusinessStatusService: getStatusMap(statusIds)
    CrmBusinessStatusService -->> CrmBusinessService: 返回状态Map
    CrmBusinessService ->> CrmBusinessService: getBusinessProductListByBusinessId(businessId)
    CrmBusinessService ->> CrmProductService: getProductMap(productIds)
    CrmProductService -->> CrmBusinessService: 返回产品Map
end
CrmBusinessService -->> CrmBusinessController: CrmBusinessRespVO
CrmBusinessController -->> User: CommonResult<CrmBusinessRespVO>
```

### 7.3 导出商机 Excel 数据流

```
用户 ->> CrmBusinessController: GET /crm/business/export-excel (CrmBusinessPageReqVO)
CrmBusinessController ->> CrmBusinessService: getBusinessPage(exportReqVO, userId)
CrmBusinessService -->> CrmBusinessController: PageResult<CrmBusinessDO>
CrmBusinessController ->> ExcelUtils: write(response, "商机.xls", "数据", CrmBusinessRespVO.class, buildBusinessDetailList(list))
ExcelUtils -->> CrmBusinessController: 写入响应流
CrmBusinessController -->> User: Excel文件下载
```

---

## 8. 依赖模块

业务控制器模块依赖以下核心模块：

| 模块 | 依赖说明 |
|------|----------|
| **yudao-module-crm** | 本模块本身，包含服务层和数据访问层 |
| **yudao-module-system** | 依赖系统模块的部门 API (`DeptApi`) 和用户 API (`AdminUserApi`) |
| **yudao-framework-common** | 依赖通用工具类（BeanUtils、CollectionUtils、MapUtils、NumberUtils 等） |
| **yudao-framework-excel** | 依赖 Excel 工具类 (`ExcelUtils`) 进行数据导出 |
| **yudao-framework-apilog** | 依赖操作日志注解 (`@ApiAccessLog`) |
| **yudao-framework-security** | 依赖权限校验工具 (`SecurityFrameworkUtils.getLoginUserId()`) |

---

## 9. 异常处理

模块中使用了统一的异常处理机制：

- **`ServiceExceptionUtil.exception()`**: 抛出业务异常，如 `CUSTOMER_NOT_EXISTS`（客户不存在）
- **空值检查**: 在 `getBusiness()` 方法中检查商机是否存在
- **参数校验**: 使用 `@Valid` 注解进行请求参数校验，由 Spring MVC 自动处理

---

## 10. 使用示例

### 10.1 创建商机

**请求**:
```http
POST /crm/business/create
Content-Type: application/json

{
  "name": "某公司ERP系统项目",
  "customerId": 1001,
  "contactId": 2001,
  "ownerUserId": 10,
  "statusTypeId": 1,
  "statusId": 2,
  "probability": 80,
  "expectedRevenue": 50000.00,
  "description": "企业级ERP系统开发项目",
  "productList": [
    {
      "productId": 101,
      "quantity": 2,
      "unitPrice": 25000.00
    }
  ]
}
```

**响应**:
```json
{
  "code": 0,
  "message": "成功",
  "data": 10001
}
```

### 10.2 获取商机详情

**请求**:
```http
GET /crm/business/get?id=10001
```

**响应**:
```json
{
  "code": 0,
  "message": "成功",
  "data": {
    "id": 10001,
    "name": "某公司ERP系统项目",
    "customerId": 1001,
    "customerName": "某科技有限公司",
    "contactId": 2001,
    "creator": "admin",
    "creatorName": "张三",
    "ownerUserId": 10,
    "ownerUserName": "李四",
    "ownerUserDeptName": "销售部",
    "statusTypeId": 1,
    "statusTypeName": "销售阶段",
    "statusId": 2,
    "statusName": "方案报价",
    "probability": 80,
    "expectedRevenue": 50000.00,
    "actualRevenue": null,
    "products": [
      {
        "productId": 101,
        "productNo": "ERP-2024",
        "productName": "企业版ERP系统",
        "unit": "套",
        "quantity": 2,
        "unitPrice": 25000.00
      }
    ]
  }
}
```

### 10.3 更新商机状态

**请求**:
```http
PUT /crm/business/update-status
Content-Type: application/json

{
  "id": 10001,
  "statusId": 3
}
```

**响应**:
```json
{
  "code": 0,
  "message": "成功",
  "data": true
}
```

---

## 11. 相关模块参考

- [yudao-module-crm.md](yudao-module-crm.md) - CRM 模块整体文档
- [yudao-module-system.md](yudao-module-system.md) - 系统基础模块（用户、部门、权限等）
- [yudao-framework-common.md](yudao-framework-common.md) - 通用工具类库
- [yudao-framework-excel.md](yudao-framework-excel.md) - Excel 处理工具
