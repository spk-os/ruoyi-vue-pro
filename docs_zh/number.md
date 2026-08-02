# 数字工具模块 (number)

## 概述

数字工具模块 (`number`) 是 Yudao 框架中负责数字处理与金额计算的通用工具模块。它提供了对 `Long`、`Integer` 类型的解析、数字有效性校验、经纬度距离计算、精确乘法运算以及金额的百分比计算、单位换算（分转元）等核心能力。

该模块是框架基础工具类的一部分，被**业务模块**（如交易、支付、CRM、ERP 等）广泛依赖，用于处理价格计算、金额格式化、地理距离测算等场景。

---

## 模块架构

```mermaid
graph TD
    subgraph "数字工具模块 (number)"
        NumberUtils[NumberUtils<br/>数字通用工具类]
        MoneyUtils[MoneyUtils<br/>金额专用工具类]
    end

    subgraph "外部依赖"
        Hutool[Hutool 工具库<br/>NumberUtil / Money / StrUtil]
    end

    subgraph "主要调用方"
        Trade[交易模块<br/>价格计算]
        Pay[支付模块<br/>金额处理]
        CRM[CRM 模块<br/>金额统计]
        ERP[ERP 模块<br/>财务计算]
        IoT[IoT 模块<br/>设备数据]
    end

    NumberUtils --> Hutool
    MoneyUtils --> Hutool
    Trade -.->|价格计算| MoneyUtils
    Pay -.->|金额处理| MoneyUtils
    CRM -.->|统计报表| MoneyUtils
    ERP -.->|财务计算| MoneyUtils
```

---

## 核心组件

### 1. NumberUtils —— 数字通用工具类

**文件：** `NumberUtils.java`

#### 类图

```mermaid
classDiagram
    class NumberUtils {
        +parseLong(String str) Long
        +parseInt(String str) Integer
        +isAllNumber(List~String~ values) boolean
        +getDistance(double lat1, double lng1, double lat2, double lng2) double
        +mul(BigDecimal... values) BigDecimal
    }
    NumberUtils ..> Hutool.NumberUtil : 依赖
    NumberUtils ..> Hutool.StrUtil : 依赖
    NumberUtils ..> Hutool.CollUtil : 依赖
```

#### 方法说明

| 方法 | 返回类型 | 说明 |
|------|----------|------|
| `parseLong(String)` | `Long` | 安全地将字符串解析为 `Long`，空字符串返回 `null` |
| `parseInt(String)` | `Integer` | 安全地将字符串解析为 `Integer`，空字符串返回 `null` |
| `isAllNumber(List<String>)` | `boolean` | 判断字符串列表中的所有元素是否均为合法数字 |
| `getDistance(lat1, lng1, lat2, lng2)` | `double` | 通过经纬度计算地球上两点之间的距离（单位：千米），基于 Haversine 公式 |
| `mul(BigDecimal...)` | `BigDecimal` | 提供精确的乘法运算，与 Hutool 的区别是：若任一参数为 `null` 则返回 `null` |

#### 使用场景

- **`parseLong` / `parseInt`**：用于从请求参数或配置项中安全提取数值，避免 `NumberFormatException`
- **`isAllNumber`**：批量校验输入参数是否为数字，常用于数据导入时的字段校验
- **`getDistance`**：地理位置服务，如计算用户与门店之间的距离、IoT 设备位置定位
- **`mul`**：需要避免 `null` 传播的精确乘法场景（金融计算中需严格控制 `null` 值）

---

### 2. MoneyUtils —— 金额专用工具类

**文件：** `MoneyUtils.java`

#### 类图

```mermaid
classDiagram
    class MoneyUtils {
        +PERCENT_100 BigDecimal
        +calculateRatePrice(Integer price, Double rate) Integer
        +calculateRatePriceFloor(Integer price, Double rate) Integer
        +calculator(Integer price, Integer count, Integer percent) Integer
        +calculateRatePrice(Number price, Number rate, int scale, RoundingMode mode) BigDecimal
        +fenToYuan(int fen) BigDecimal
        +fenToYuanStr(int fen) String
        +priceMultiply(BigDecimal price, BigDecimal count) BigDecimal
        +priceMultiplyPercent(BigDecimal price, BigDecimal percent) BigDecimal
    }
    MoneyUtils ..> Hutool.Money : 依赖
    MoneyUtils ..> Hutool.NumberUtil : 依赖
```

#### 方法说明

| 方法 | 返回类型 | 说明 |
|------|----------|------|
| `calculateRatePrice(Integer, Double)` | `Integer` | 计算百分比金额（四舍五入），例如 `价格 × 税率百分比 ÷ 100` |
| `calculateRatePriceFloor(Integer, Double)` | `Integer` | 计算百分比金额（向下取整），适用于优惠分摊等需舍弃零头的场景 |
| `calculator(Integer, Integer, Integer)` | `Integer` | 计算商品总价：`price × count × (percent / 100)`，`percent` 为折扣分（如 60.2% 传入 6020） |
| `calculateRatePrice(Number, Number, int, RoundingMode)` | `BigDecimal` | 通用的百分比金额计算，支持自定义精度和舍入模式 |
| `fenToYuan(int)` | `BigDecimal` | 分转元（返回 `BigDecimal`），如 `1分 → 0.01` 元 |
| `fenToYuanStr(int)` | `String` | 分转元（返回字符串），如 `1分 → "0.01"` |
| `priceMultiply(BigDecimal, BigDecimal)` | `BigDecimal` | 金额相乘，保留 2 位小数，四舍五入 |
| `priceMultiplyPercent(BigDecimal, BigDecimal)` | `BigDecimal` | 金额乘以百分比（除以 100），保留 2 位小数，四舍五入 |

#### 使用场景

- **价格计算**：在交易订单中计算商品总价、优惠分摊、税费计算
- **金额格式化**：将数据库存储的"分"单位金额转为前端展示的"元"单位
- **折扣计算**：秒杀、拼团、优惠券等营销活动的折扣金额计算
- **财务报表**：ERP 系统的财务模块中对金额进行精确乘除运算

---

## 数据流图

### 金额处理流程

```mermaid
sequenceDiagram
    participant Frontend as 前端
    participant Controller as Controller
    participant Service as Service
    participant MoneyUtils as MoneyUtils
    participant DB as 数据库

    Frontend ->> Controller: 提交订单（商品价格、数量、折扣）
    Controller ->> Service: 处理请求
    Service ->> MoneyUtils: calculator(price, count, percent)
    MoneyUtils -->> Service: 返回计算后金额（单位：分）
    Service ->> MoneyUtils: fenToYuan(金额)
    MoneyUtils -->> Service: 返回元单位字符串
    Service ->> DB: 存储金额
    Service -->> Controller: 返回响应
    Controller -->> Frontend: 展示金额（元）
```

### 地理距离计算流程

```mermaid
sequenceDiagram
    participant App as 移动端/设备
    participant Service as 服务层
    participant NumberUtils as NumberUtils

    App ->> Service: 上传当前位置（经纬度）
    Service ->> NumberUtils: getDistance(lat1, lng1, lat2, lng2)
    Note over NumberUtils: Haversine 公式计算
    NumberUtils -->> Service: 返回距离（千米）
    Service -->> App: 返回附近门店/设备信息
```

---

## 依赖关系矩阵

| 工具方法 | 主要依赖 | 可能调用的模块 |
|----------|----------|---------------|
| `NumberUtils.parseLong` | Hutool `StrUtil` | system、infra、所有模块 |
| `NumberUtils.parseInt` | Hutool `StrUtil` | 所有模块 |
| `NumberUtils.isAllNumber` | Hutool `CollUtil`、`NumberUtil` | 数据导入、批量校验 |
| `NumberUtils.getDistance` | `Math` | CRM（门店距离）、IoT（设备定位） |
| `NumberUtils.mul` | Hutool `NumberUtil` | 财务计算 |
| `MoneyUtils.calculateRatePrice` | Hutool `NumberUtil` | trade（交易价格）、promotion（营销） |
| `MoneyUtils.calculateRatePriceFloor` | Hutool `NumberUtil` | trade（优惠分摊） |
| `MoneyUtils.calculator` | Hutool `NumberUtil` | trade（商品总价） |
| `MoneyUtils.fenToYuan` | Hutool `Money` | pay（支付金额展示）、trade（订单金额） |
| `MoneyUtils.priceMultiply` | - | trade（金额相乘） |
| `MoneyUtils.priceMultiplyPercent` | - | trade（百分比金额） |

---

## 与其他模块的关联

本模块作为框架的基础工具模块，被多个业务模块引用：

| 相关模块 | 关联文件/链接 | 说明 |
|----------|--------------|------|
| [交易模块](trade.md) | `MoneyUtils` 用于订单价格计算、优惠分摊 | 订单金额计算 |
| [支付模块](pay.md) | `fenToYuan` / `fenToYuanStr` 用于金额展示 | 金额单位转换 |
| [CRM 模块](crm.md) | `getDistance` 用于门店距离计算 | 地理位置服务 |
| [ERP 模块](erp.md) | `mul`、`calculateRatePrice` 用于财务计算 | 精确财务运算 |
| [通用工具模块](util.md) | Hutool 工具库 | 底层依赖 |
| [数据校验模块](validation.md) | `isAllNumber` 用于批量数字校验 | 数据导入校验 |

> **提示：** 本模块不涉及任何数据库或缓存操作，为纯计算工具类，可在任何 Java 环境中直接使用。

---

## 使用示例

### 1. 安全字符串解析

```java
// 从请求参数中安全获取数值
String param = request.getParameter("userId");
Long userId = NumberUtils.parseLong(param); // 若 param 为空或 null，返回 null 而非异常

// 批量校验数字
List<String> values = Arrays.asList("100", "200", "abc");
boolean allNumber = NumberUtils.isAllNumber(values); // false
```

### 2. 金额计算

```java
// 计算商品总价：单价 100 元（10000 分），数量 2，折扣 80%
Integer total = MoneyUtils.calculator(10000, 2, 8000); // 16000 分 = 160 元

// 分转元展示
String yuan = MoneyUtils.fenToYuanStr(16000); // "160.00"

// 百分比金额计算（四舍五入）
Integer tax = MoneyUtils.calculateRatePrice(10000, 13.0); // 1300 分 = 13 元（13% 税率）

// 精确金额相乘
BigDecimal result = MoneyUtils.priceMultiply(new BigDecimal("99.99"), new BigDecimal("3"));
// 结果: 299.97
```

### 3. 地理距离计算

```java
// 计算北京到上海的距离
double distance = NumberUtils.getDistance(39.9042, 116.4074, 31.2304, 121.4737);
// 结果约为 1068.23 千米
```
