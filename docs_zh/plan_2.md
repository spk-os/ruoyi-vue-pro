# plan_2 模块文档 - MES 排班计划管理

## 1. 模块概述

**plan_2** 模块是 MES（制造执行系统）中的核心排班计划管理模块，负责生产排班计划的全生命周期管理。该模块提供了排班计划、班次、班组关联的创建、查询、更新、删除及导出功能，支持生产调度人员高效管理生产班次安排。

### 1.1 模块定位

```
┌─────────────────────────────────────────────────────────────┐
│                      MES 模块                                │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐          │
│  │ 计划管理    │  │ 班组管理    │  │ 日历管理    │          │
│  │ (plan_2)    │  │ (team)      │  │ (calendar)  │          │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘          │
│         │               │               │                   │
│    ┌────┴────┐    ┌────┴────┐    ┌────┴────┐              │
│  │ 排班计划  │  │ 计划班次  │  │ 计划班组  │              │
│  │ (plan)    │  │ (shift)   │  │ (team)    │              │
│  └───────────┘  └───────────┘  └───────────┘              │
└─────────────────────────────────────────────────────────────┘
```

### 1.2 核心功能

- **排班计划管理**：创建、更新、确认、删除排班计划，支持分页查询和 Excel 导出
- **班次管理**：为排班计划添加班次，支持班次的增删改查
- **班组关联管理**：将班组与排班计划进行关联，实现生产任务的分配
- **状态管理**：排班计划支持草稿、已确认等状态流转，确保数据一致性

---

## 2. 架构设计

### 2.1 系统架构

plan_2 模块采用标准的分层架构设计，遵循 Spring Boot + MyBatis Plus 的技术栈：

```
┌─────────────────────────────────────────────────────────────────┐
│                      前端层 (Vue3)                              │
│  ├─ 排班计划管理页面                                            │
│  ├─ 班次管理页面                                                │
│  └─ 班组关联管理页面                                            │
└──────────────────────┬──────────────────────────────────────────┘
                       │ HTTP/REST API
                       ▼
┌─────────────────────────────────────────────────────────────────┐
│                      控制器层 (Controller)                      │
│  ├─ MesCalPlanController        - 排班计划接口                  │
│  ├─ MesCalPlanShiftController   - 班次接口                      │
│  └─ MesCalPlanTeamController    - 班组关联接口                  │
└──────────────────────┬──────────────────────────────────────────┘
                       │ Service 调用
                       ▼
┌─────────────────────────────────────────────────────────────────┐
│                      服务层 (Service)                           │
│  ├─ MesCalPlanService           - 排班计划服务                  │
│  ├─ MesCalPlanShiftService      - 班次服务                      │
│  └─ MesCalPlanTeamService       - 班组关联服务                  │
└──────────────────────┬──────────────────────────────────────────┘
                       │ Mapper 调用
                       ▼
┌─────────────────────────────────────────────────────────────────┐
│                      数据访问层 (Mapper/DO)                     │
│  ├─ MesCalPlanDO              - 排班计划实体                    │
│  ├─ MesCalPlanShiftDO         - 班次实体                        │
│  └─ MesCalPlanTeamDO          - 计划班组关联实体                │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 数据模型

#### 2.2.1 排班计划 (MesCalPlanDO)

```
┌─────────────────────────────────────────────────────────────┐
│                    MesCalPlanDO                             │
├─────────────┬───────────────────────────────────────────────┤
│ 字段名      │ 类型          │ 描述                          │
├─────────────┼───────────────────────────────────────────────┤
│ id          │ Long          │ 主键                            │
│ code        │ String        │ 计划编码（唯一）                │
│ name        │ String        │ 计划名称                        │
│ calendarType│ Integer       │ 班组类型                        │
│ startDate   │ LocalDateTime │ 开始日期                        │
│ endDate     │ LocalDateTime │ 结束日期                        │
│ shiftType   │ Integer       │ 轮班方式（字典：MES_CAL_SHIFT_TYPE）│
│ shiftMethod │ Integer       │ 倒班方式（字典：MES_CAL_SHIFT_METHOD）│
│ shiftCount  │ Integer       │ 倒班天数                        │
│ status      │ Integer       │ 状态（字典：MES_CAL_PLAN_STATUS） │
│ remark      │ String        │ 备注                            │
└─────────────┴───────────────────────────────────────────────┘
```

#### 2.2.2 班次 (MesCalPlanShiftDO)

```
┌─────────────────────────────────────────────────────────────┐
│                    MesCalPlanShiftDO                        │
├─────────────┬───────────────────────────────────────────────┤
│ 字段名      │ 类型          │ 描述                          │
├─────────────┼───────────────────────────────────────────────┤
│ id          │ Long          │ 主键                            │
│ planId      │ Long          │ 计划编号（外键）                │
│ shiftNo     │ Integer       │ 班次编号（如 1、2、3）          │
│ startTime   │ LocalDateTime │ 开始时间                        │
│ endTime     │ LocalDateTime │ 结束时间                        │
│ remark      │ String        │ 备注                            │
└─────────────┴───────────────────────────────────────────────┘
```

#### 2.2.3 计划班组关联 (MesCalPlanTeamDO)

```
┌─────────────────────────────────────────────────────────────┐
│                    MesCalPlanTeamDO                         │
├─────────────┬───────────────────────────────────────────────┤
│ 字段名      │ 类型          │ 描述                          │
├─────────────┼───────────────────────────────────────────────┤
│ id          │ Long          │ 主键                            │
│ planId      │ Long          │ 计划编号（外键）                │
│ teamId      │ Long          │ 班组编号（外键）                │
│ remark      │ String        │ 备注                            │
└─────────────┴───────────────────────────────────────────────┘
```

### 2.3 状态流转图

```
┌─────────────────────────────────────────────────────────────────┐
│                    排班计划状态流转                             │
├─────────────────────────────────────────────────────────────────┤
│                                                               │
│  [创建]                                                         │
│       ▼                                                         │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐          │
│  │  草稿状态   │────▶│ 已确认状态  │────▶│  删除状态   │          │
│  │ (PREPARE)   │    │ (CONFIRMED) │    │ (DELETE)    │          │
│  └─────────────┘    └─────────────┘    └─────────────┘          │
│       ▲                                                       │
│       │ (更新，仅草稿状态可修改)                                │
│       └───────────────────────────────────────────────────────┘
│                                                               │
│  关键操作：                                                     │
│  - 创建：默认状态为草稿 (PREPARE)                               │
│  - 更新：仅草稿状态可修改                                     │
│  - 确认：草稿 → 已确认，校验班组数量与轮班方式匹配              │
│  - 删除：仅草稿状态可删除，级联删除班次和班组关联               │
│                                                               │
└─────────────────────────────────────────────────────────────────┘
```

---

## 3. 核心功能模块

### 3.1 排班计划管理 (MesCalPlanController)

**接口路径**: `/mes/cal/plan`

| 请求方法 | 接口路径 | 描述 | 权限 |
|---------|---------|------|------|
| POST | `/create` | 创建排班计划 | `mes:cal-plan:create` |
| PUT | `/update` | 更新排班计划 | `mes:cal-plan:update` |
| PUT | `/confirm` | 确认排班计划 | `mes:cal-plan:update` |
| DELETE | `/delete` | 删除排班计划 | `mes:cal-plan:delete` |
| GET | `/get` | 获取排班计划详情 | `mes:cal-plan:query` |
| GET | `/page` | 分页查询排班计划 | `mes:cal-plan:query` |
| GET | `/export-excel` | 导出排班计划 Excel | `mes:cal-plan:export` |

**核心逻辑**：
- 创建时自动验证计划编码唯一性，默认状态为草稿
- 更新时不允许修改已确认状态的计划
- 确认时校验班组数量与轮班方式是否匹配
- 删除时级联删除关联的班次和班组关联

### 3.2 班次管理 (MesCalPlanShiftController)

**接口路径**: `/mes/cal/plan-shift`

| 请求方法 | 接口路径 | 描述 | 权限 |
|---------|---------|------|------|
| POST | `/create` | 创建计划班次 | `mes:cal-plan:update` |
| PUT | `/update` | 更新计划班次 | `mes:cal-plan:update` |
| DELETE | `/delete` | 删除计划班次 | `mes:cal-plan:update` |
| GET | `/get` | 获取班次详情 | `mes:cal-plan:query` |
| GET | `/page` | 分页查询班次 | `mes:cal-plan:query` |
| GET | `/list-by-plan` | 按计划查询班次列表 | `mes:cal-plan:query` |

**核心逻辑**：
- 班次属于某个排班计划，通过 `planId` 关联
- 支持按计划查询班次列表，用于前端展示班次安排

### 3.3 班组关联管理 (MesCalPlanTeamController)

**接口路径**: `/mes/cal/plan-team`

| 请求方法 | 接口路径 | 描述 | 权限 |
|---------|---------|------|------|
| POST | `/create` | 创建计划班组关联 | `mes:cal-plan:update` |
| DELETE | `/delete` | 删除计划班组关联 | `mes:cal-plan:update` |
| GET | `/list-by-plan` | 按计划查询班组列表 | `mes:cal-plan:query` |

**核心逻辑**：
- 建立计划与班组的关联关系
- 查询时自动拼装班组编码和名称信息
- 确认排班计划前需校验班组数量是否满足轮班要求

---

## 4. 服务层设计

### 4.1 排班计划服务 (MesCalPlanService)

```
┌─────────────────────────────────────────────────────────────────┐
│                    MesCalPlanService                            │
├─────────────────────────────────────────────────────────────────┤
│ 方法列表：                                                      │
│ • createPlan(MesCalPlanSaveReqVO)     - 创建计划                │
│ • updatePlan(MesCalPlanSaveReqVO)     - 更新计划                │
│ • confirmPlan(Long id)                - 确认计划                │
│ • deletePlan(Long id)                 - 删除计划                │
│ • getPlan(Long id)                    - 获取计划详情            │
│ • getPlanPage(MesCalPlanPageReqVO)    - 分页查询                │
│ • validatePrepare(Long planId)        - 验证计划是否为草稿      │
│ • validateExists(Long id)             - 验证计划是否存在        │
│ • validateCodeUnique(Long id, String code) - 验证编码唯一性     │
│ • validateTeamCountForConfirm(Long planId, Integer shiftType) - 确认前校验班组数 │
└─────────────────────────────────────────────────────────────────┘
```

**关键特性**：
- 所有写操作均使用 `@Transactional` 保证数据一致性
- 创建时自动根据轮班方式生成默认班次
- 确认时生成班组排班记录（调用 `teamShiftService.generateTeamShiftRecords`）

### 4.2 班次服务 (MesCalPlanShiftService)

```
┌─────────────────────────────────────────────────────────────────┐
│                    MesCalPlanShiftService                       │
├─────────────────────────────────────────────────────────────────┤
│ 方法列表：                                                      │
│ • createPlanShift(MesCalPlanShiftSaveReqVO) - 创建班次          │
│ • updatePlanShift(MesCalPlanShiftSaveReqVO) - 更新班次          │
│ • deletePlanShift(Long id)              - 删除班次              │
│ • getPlanShift(Long id)                 - 获取班次详情          │
│ • getPlanShiftPage(MesCalPlanShiftPageReqVO) - 分页查询         │
│ • getPlanShiftListByPlan(Long planId)   - 按计划查询班次列表    │
│ • addDefaultPlanShift(Long planId, Integer shiftType) - 添加默认班次 │
│ • deletePlanShiftByPlanId(Long planId)  - 按计划删除班次        │
└─────────────────────────────────────────────────────────────────┘
```

### 4.3 班组关联服务 (MesCalPlanTeamService)

```
┌─────────────────────────────────────────────────────────────────┐
│                    MesCalPlanTeamService                        │
├─────────────────────────────────────────────────────────────────┤
│ 方法列表：                                                      │
│ • createPlanTeam(MesCalPlanTeamSaveReqVO) - 创建关联            │
│ • deletePlanTeam(Long id)             - 删除关联                │
│ • getPlanTeamListByPlan(Long planId)    - 按计划查询关联列表    │
│ • getPlanTeamCountByPlanId(Long planId) - 按计划查询关联数量    │
│ • deleteByPlanId(Long planId)         - 按计划删除所有关联      │
└─────────────────────────────────────────────────────────────────┘
```

---

## 5. 权限控制

plan_2 模块使用 Spring Security 的 `@PreAuthorize` 注解进行权限控制，所有接口均基于角色权限进行访问控制：

| 操作 | 权限标识 | 说明 |
|------|---------|------|
| 创建计划 | `mes:cal-plan:create` | 仅允许创建新计划 |
| 更新计划 | `mes:cal-plan:update` | 允许修改计划、班次、班组关联 |
| 删除计划 | `mes:cal-plan:delete` | 允许删除计划 |
| 查询计划 | `mes:cal-plan:query` | 允许查看计划详情和列表 |
| 导出 Excel | `mes:cal-plan:export` | 允许导出计划数据 |

---

## 6. 依赖关系

### 6.1 模块依赖

```
plan_2 (MES 排班计划)
├── 依赖: mes (MES 主模块)
│   ├── team (班组管理)
│   ├── shift (班次管理)
│   └── calendar (日历管理)
├── 依赖: system (系统模块)
│   ├── permission (权限管理)
│   └── dict (字典管理)
├── 依赖: common (通用工具)
│   ├── util (工具类)
│   └── framework (基础框架)
└── 依赖: mybatis (数据持久层)
    └── mapper (数据映射)
```

### 6.2 类依赖关系图

```
┌─────────────────────┐      ┌─────────────────────┐      ┌─────────────────────┐
│  MesCalPlanController│────▶│  MesCalPlanService  │────▶│  MesCalPlanMapper   │
└─────────────────────┘      └─────────────────────┘      └─────────────────────┘
       ▲                           ▲                           │
       │                           │                           ▼
       │                           │                  ┌─────────────────────┐
       │                           │                  │  MesCalPlanDO       │
       │                           │                  └─────────────────────┘
       │                           │
       │                           ▼
       │                  ┌─────────────────────┐
       │                  │ MesCalPlanShiftService│
       │                  └─────────────────────┘
       │                           ▲
       │                           │
       ▼                           │
┌─────────────────────┐      ┌─────────────────────┐
│ MesCalPlanTeamCtrl  │────▶ │ MesCalPlanTeamService│
└─────────────────────┘      └─────────────────────┘
```

---

## 7. 使用示例

### 7.1 创建排班计划

**请求**:
```http
POST /mes/cal/plan/create
Content-Type: application/json

{
  "code": "PLAN2024001",
  "name": "2024年1月生产计划",
  "calendarType": 1,
  "startDate": "2024-01-01T00:00:00",
  "endDate": "2024-01-31T23:59:59",
  "shiftType": 1,
  "shiftMethod": 1,
  "shiftCount": 3,
  "remark": "月度生产计划"
}
```

**响应**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": 1001
}
```

### 7.2 确认排班计划

**请求**:
```http
PUT /mes/cal/plan/confirm?id=1001
```

**响应**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": true
}
```

### 7.3 导出排班计划

**请求**:
```http
GET /mes/cal/plan/export-excel?code=PLAN2024001&page=1&size=10
```

**响应**: Excel 文件下载

---

## 8. 异常处理

plan_2 模块定义了以下业务异常：

| 异常码 | 异常描述 | 触发场景 |
|--------|---------|---------|
| CAL_PLAN_NOT_EXISTS | 计划不存在 | 查询/更新/删除不存在的计划 |
| CAL_PLAN_NOT_PREPARE | 计划非草稿状态 | 更新/删除已确认的计划 |
| CAL_PLAN_CODE_DUPLICATE | 计划编码重复 | 创建/更新时编码已存在 |
| CAL_PLAN_TEAM_COUNT_NOT_MATCH | 班组数量不匹配 | 确认计划时班组数不足 |
| CAL_TEAM_NOT_EXISTS | 班组不存在 | 关联不存在的班组 |
| CAL_TEAM_CODE_DUPLICATE | 班组编码重复 | 创建/更新时编码已存在 |

---

## 9. 扩展点

### 9.1 自动生成默认班次

在创建排班计划时，如果指定了 `shiftType`，系统会自动调用 `planShiftService.addDefaultPlanShift()` 生成默认班次，简化排班计划的创建流程。

### 9.2 生成班组排班记录

在确认排班计划时，系统会自动调用 `teamShiftService.generateTeamShiftRecords()` 生成班组排班记录，为后续的生产调度提供数据支持。

### 9.3 级联删除

删除排班计划时，会自动级联删除相关的班次和班组关联，保证数据一致性。

---

## 10. 相关文档

- [team.md] - 班组管理模块文档
- [shift.md] - 班次管理模块文档
- [calendar.md] - 日历管理模块文档
- [system.md] - 系统权限模块文档
- [common.md] - 通用工具模块文档
