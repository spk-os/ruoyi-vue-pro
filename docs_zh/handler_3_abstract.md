# AbstractSliderDesensitizationHandler 抽象类

## 概述

`AbstractSliderDesensitizationHandler` 是一个抽象类，实现了 `DesensitizationHandler` 接口，提供了滑动窗口方式的数据脱敏实现模板。该类通过保留前缀和后缀的一定长度，用指定的替换字符替换中间部分来实现脱敏。

`DesensitizationHandler` 接口定义了脱敏处理的契约，并提供了一个默认方法 `getDisable(T annotation)`，用于通过反射读取注解的 `disable` 属性来判断是否禁用脱敏（参见 [DesensitizationHandler 接口文档](#)）。

## 架构与组件关系

`AbstractSliderDesensitizationHandler` 位于数据脱敏框架的核心层，其架构关系如下：

1. **顶层接口**：`DesensitizationHandler<T>`
   - 定义了脱敏处理器的基本契约，包括 `desensitize` 方法和默认的 `getDisable` 方法。

2. **抽象实现**：`AbstractSliderDesensitizationHandler<T>`
   - 实现了 `DesensitizationHandler` 接口，提供了基于滑动窗口的脱敏算法模板。
   - 定义了三个抽象方法：`getPrefixKeep`、`getSuffixKeep` 和 `getReplacer`，由具体子类实现。

3. **具体实现**：
   - `PasswordDesensitization`：密码脱敏处理器
   - `FixedPhoneDesensitization`：固定电话脱敏处理器
   - `IdCardDesensitization`：身份证号脱敏处理器
   - `BankCardDesensitization`：银行卡号脱敏处理器
   - `MobileDesensitization`：手机号脱敏处理器
   - `ChineseNameDesensitization`：中文姓名脱敏处理器
   - `CarLicenseDesensitization`：车牌号脱敏处理器
   - `DefaultDesensitizationHandler`：默认脱敏处理器

> **说明**：
> - `DesensitizationHandler` 是脱敏处理器的顶级接口，定义了脱敏处理的基本契约。
> - `AbstractSliderDesensitizationHandler` 实现了接口中的通用脱敏逻辑，并留出三个抽象方法供具体实现类自定义。
> - 各具体实现类（如 `PasswordDesensitization` 等）继承自此抽象类，并实现抽象方法以定义具体的脱敏规则。

## 核心功能

### 主要方法

#### `desensitize` 方法（模板方法）

```java
@Override
public String desensitize(String origin, T annotation) {
    // 1. 判断是否禁用脱敏
    Object disable = SpringExpressionUtils.parseExpression(getDisable(annotation));
    if (Boolean.TRUE.equals(disable)) {
        return origin;
    }

    // 2. 执行脱敏
    int prefixKeep = getPrefixKeep(annotation);
    int suffixKeep = getSuffixKeep(annotation);
    String replacer = getReplacer(annotation);
    int length = origin.length();
    int interval = length - prefixKeep - suffixKeep;

    // 情况一：原始字符串长度小于等于前后缀保留字符串长度，则原始字符串全部替换
    if (interval <= 0) {
        return buildReplacerByLength(replacer, length);
    }

    // 情况二：原始字符串长度大于前后缀保留字符串长度，则替换中间字符串
    return origin.substring(0, prefixKeep) +
            buildReplacerByLength(replacer, interval) +
            origin.substring(prefixKeep + interval);
}
```

**说明**：
- 该方法是模板方法，定义了脱敏的整体流程。
- 首先通过 `getDisable(annotation)` 获取 SpEL 表达式并解析，判断是否禁用脱敏（如果禁用则直接返回原始字符串）。
- 然后通过抽象方法获取前缀保留长度、`getPrefixKeep`；后缀保留长度，`getSuffixKeep`；以及替换字符，`getReplacer`。
- 根据原始字符串长度和保留长度计算需要替换的中间部分长度。
- 如果中间部分长度小于等于0（即原始字符串长度不足以保留前后缀），则用替换字符填充整个字符串。
- 否则，保留前缀和后缀，用替换字符填充中间部分。

#### 辅助方法

- `buildReplacerByLength(String replacer, int length)`：根据指定长度生成替换字符串（例如，`repeated "*" 3 times` 返回 `"***"`）。

#### 抽象方法（需由子类实现）

| 方法名                 | 返回类型 | 说明                     |
|------------------------|----------|--------------------------|
| `getPrefixKeep`        | Integer  | 获取前缀保留长度         |
| `getSuffixKeep`        | Integer  | 获取后缀保留长度         |
| `getReplacer`          | String   | 获取替换字符             |

> **注意**：抽象方法中的 `T` 为具体注解类型（如 `PasswordDesensitization` 对应的注解类型），子类需根据实际注解类型实现这些方法。

## 使用示例

以 `MobileDesensitization` 为例（假设其注解类型为 `Mobile`）：

```java
@Component
public class MobileDesensitization extends AbstractSliderDesensitizationHandler<Mobile> {

    @Override
    public Integer getPrefixKeep(Mobile annotation) {
        return annotation.prefixKeep();
    }

    @Override
    public Integer getSuffixKeep(Mobile annotation) {
        return annotation.suffixKeep();
    }

    @Override
    public String getReplacer(Mobile annotation) {
        return annotation.replacer();
    }
}
```

在上述示例中：
- `Mobile` 注解需提供 `prefixKeep()`、`suffixKeep()` 和 `replacer()` 方法。
- 当调用 `desensitize` 方法时，会根据注解配置执行脱敏。

## 配置与扩展

### 自定义脱敏规则
通过实现抽象方法，可以灵活控制：
- 前缀保留字符数（如手机号前 3 位）
- 后缀保留字符数（如手机号后 4 位）
- 替换字符（如 `*` 或 `x`）

### 禁用脱敏
抽象类通过 `getDisable(annotation)` 方法获取 SpEL 表达式来判断是否禁用脱敏。该方法由 `DesensitizationHandler` 接口提供默认实现：通过反射调用注解的 `disable()` 方法获取表达式字符串，如果注解没有 `disable()` 方法则返回空字符串（即不禁止脱敏）。子类也可以重写 `getDisable` 方法以自定义禁用逻辑。

## 注意事项

1. 线程安全：本类无状态，天然线程安全。
2. 异常处理：未在方法中显式捕获异常，SpEL 表达式解析异常将向上抛出。
3. 性能：使用 `String.repeat` 方法（Java 11+）生成替换字符串，效率较高。

## 与其他模块的关系

- 依赖 [`SpringExpressionUtils`](../spring/SpringUtils.md) 进行 SpEL 表达式解析。
- 是 [`DesensitizationHandler`](../desensitize/core/base/handler/DesensitizationHandler.md) 的具体实现基类。
- 被各具体脱敏处理器（如手机号、身份证号等）继承使用。

## 更新日志

| 版本   | 日期       | 描述         |
|--------|------------|--------------|
| 1.0.0  | 2023-06-01 | 初始版本     |