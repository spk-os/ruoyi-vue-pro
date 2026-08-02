# 支付保存请求对象

## 概述

`ErpFinancePaymentSaveReqVO` 是一个用于管理后台 ERP 付款单新增或修改的请求对象。它包含了付款单的详细信息，包括付款项列表。

## 字段说明

### 付款单基本信息

| 字段名 | 类型 | 必需 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 编号，例如：23752 |
| paymentTime | LocalDateTime | 是 | 付款时间 |
| financeUserId | Long | 否 | 财务人员编号，例如：19690 |
| supplierId | Long | 是 | 供应商编号，例如：29399 |
| accountId | Long | 是 | 付款账户编号，例如：28989 |
| discountPrice | BigDecimal | 是 | 优惠金额，单位：元，例如：11600 |
| remark | String | 否 | 备注，例如：你猜 |

### 付款项列表

付款单保存请求对象包含一个 `items` 列表，每个项目的结构如下：

| 字段名 | 类型 | 必需 | 说明 |
|--------|------|------|------|
| id | Long | 否 | 付款项编号，例如：11756 |
| bizType | Integer | 是 | 业务类型，例如：1 |
| bizId | Long | 是 | 业务编号，例如：11756 |
| paidPrice | BigDecimal | 是 | 已付金额，单位：分，例如：10000 |
| paymentPrice | BigDecimal | 是 | 本次付款，单位：分，例如：10000 |
| remark | String | 否 | 备注，例如：随便 |

## 示例

```json
{
  "id": 23752,
  "paymentTime": "2023-01-01 00:00:00",
  "financeUserId": 19690,
  "supplierId": 29399,
  "accountId": 28989,
  "discountPrice": 11600,
  "remark": "你猜",
  "items": [
    {
      "id": 11756,
      "bizType": 1,
      "bizId": 11756,
      "paidPrice": 10000,
      "paymentPrice": 10000,
      "remark": "随便"
    }
  ]
}
```

## 使用场景

此对象用于后端 API 的新增或修改接口，例如：

```java
@PostMapping("/save")
public CommonResult<Long> savePayment(@RequestBody ErpFinancePaymentSaveReqVO saveReqVO) {
    // 处理保存付款单逻辑
}
```

## 相关模块

* [支付模块](../payment.md)