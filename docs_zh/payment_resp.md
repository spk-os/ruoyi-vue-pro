# 支付响应对象

## 概述

`ErpFinancePaymentRespVO` 是一个用于管理后台 ERP 付款单响应的数据传输对象。它包含了付款单的详细信息，包括付款项列表。

## 字段说明

### 付款单基本信息

| 字段名 | 类型 | 必需 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 编号，例如：23752 |
| no | String | 是 | 付款单号，例如：FKD888 |
| status | Integer | 是 | 付款状态，例如：1 |
| paymentTime | LocalDateTime | 是 | 付款时间 |
| financeUserId | Long | 否 | 财务人员编号，例如：19690 |
| financeUserName | String | 否 | 财务人员名称，例如：张三 |
| supplierId | Long | 是 | 供应商编号，例如：29399 |
| supplierName | String | 是 | 供应商名称，例如：小番茄公司 |
| accountId | Long | 是 | 付款账户编号，例如：28989 |
| accountName | String | 是 | 付款账户名称，例如：张三 |
| totalPrice | BigDecimal | 是 | 合计价格，单位：元，例如：13832 |
| discountPrice | BigDecimal | 是 | 优惠金额，单位：元，例如：11600 |
| paymentPrice | BigDecimal | 是 | 实际价格，单位：元，例如：10000 |
| remark | String | 否 | 备注，例如：你猜 |
| creator | String | 否 | 创建人，例如：芋道 |
| creatorName | String | 否 | 创建人名称，例如：芋道 |
| createTime | LocalDateTime | 是 | 创建时间 |

### 付款项列表

付款单响应对象包含一个 `items` 列表，每个项目的结构如下：

| 字段名 | 类型 | 必需 | 说明 |
|--------|------|------|------|
| id | Long | 否 | 付款项编号，例如：11756 |
| bizType | Integer | 是 | 业务类型，例如：1 |
| bizId | Long | 是 | 业务编号，例如：11756 |
| bizNo | String | 是 | 业务单号，例如：11756 |
| totalPrice | BigDecimal | 是 | 应付金额，单位：分，例如：10000 |
| paidPrice | BigDecimal | 是 | 已付金额，单位：分，例如：10000 |
| paymentPrice | BigDecimal | 是 | 本次付款，单位：分，例如：10000 |
| remark | String | 否 | 备注，例如：随便 |

## 示例

```json
{
  "id": 23752,
  "no": "FKD888",
  "status": 1,
  "paymentTime": "2023-01-01 00:00:00",
  "financeUserId": 19690,
  "financeUserName": "张三",
  "supplierId": 29399,
  "supplierName": "小番茄公司",
  "accountId": 28989,
  "accountName": "张三",
  "totalPrice": 13832,
  "discountPrice": 11600,
  "paymentPrice": 10000,
  "remark": "你猜",
  "creator": "芋道",
  "creatorName": "芋道",
  "createTime": "2023-01-01 00:00:00",
  "items": [
    {
      "id": 11756,
      "bizType": 1,
      "bizId": 11756,
      "bizNo": "11756",
      "totalPrice": 10000,
      "paidPrice": 10000,
      "paymentPrice": 10000,
      "remark": "随便"
    }
  ]
}
```

## 使用场景

此对象用于后端 API 的响应，例如：

```java
@GetMapping("/{id}")
public CommonResult<ErpFinancePaymentRespVO> getPayment(@PathVariable Long id) {
    // 处理查询单个付款单逻辑
}
```

## 相关模块

* [支付模块](../payment.md)