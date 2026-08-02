# 支付分页请求对象

## 概述

`ErpFinancePaymentPageReqVO` 是一个用于管理后台 ERP 付款单分页查询的请求对象。它扩展了 `PageParam` 类，用于处理分页参数。

## 字段说明

| 字段名 | 类型 | 必需 | 说明 |
|--------|------|------|------|
| no | String | 否 | 付款单编号，例如：XS001 |
| paymentTime | LocalDateTime[] | 否 | 付款时间 |
| supplierId | Long | 否 | 供应商编号，例如：1724 |
| creator | String | 否 | 创建者，例如：666 |
| financeUserId | String | 否 | 财务人员编号，例如：888 |
| accountId | Long | 否 | 结算账户编号，例如：31189 |
| status | Integer | 否 | 付款状态，例如：2 |
| remark | String | 否 | 备注，例如：你猜 |
| bizNo | String | 否 | 业务编号，例如：123 |

## 示例

```json
{
  "no": "XS001",
  "paymentTime": ["2023-01-01 00:00:00", "2023-12-31 23:59:59"],
  "supplierId": 1724,
  "creator": "666",
  "financeUserId": "888",
  "accountId": 31189,
  "status": 2,
  "remark": "你猜",
  "bizNo": "123"
}
```

## 使用场景

此对象用于后端 API 的分页查询接口，例如：

```java
@GetMapping("/page")
public CommonResult<PageResult<ErpFinancePaymentRespVO>> getPaymentPage(ErpFinancePaymentPageReqVO pageReqVO) {
    // 处理分页查询逻辑
}
```

## 相关模块

* [支付模块](../payment.md)