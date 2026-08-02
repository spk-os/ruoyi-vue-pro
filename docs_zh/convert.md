# Convert 模块文档

## 概述

Convert 模块是 yudao-framework 中用于 Excel 导入导出功能的数据转换组件集合。该模块提供了一系列自定义转换器，用于在 Excel 数据与 Java 对象之间进行双向转换，特别针对数据字典、JSON、金额和地区等特殊数据类型进行了优化处理。

这些转换器实现了 EasyExcel 的 `Converter` 接口，能够无缝集成到框架的 Excel 导入导出功能中，为开发者提供了便捷的数据格式转换能力。

## 核心功能

Convert 模块主要提供以下五种转换器：

1. **DictConvert** - 单值数据字典转换器
2. **MultiDictConvert** - 多值数据字典转换器
3. **JsonConvert** - JSON 数据转换器
4. **MoneyConvert** - 金额（分转元）转换器
5. **AreaConvert** - 地区编码转换器

## 架构设计

### 组件关系

Convert 模块中的各转换器相互独立，但共享一些通用的依赖和工具类。它们都实现了 `cn.idev.excel.converters.Converter` 接口，这是 EasyExcel 框架提供的标准转换器接口。

```
类图关系说明：
- Converter 是 EasyExcel 提供的标准转换器接口
- DictConvert、MultiDictConvert、JsonConvert、MoneyConvert、AreaConvert 是其实现类
- DictFrameworkUtils 为 DictConvert 和 MultiDictConvert 提供字典查询功能
- AreaUtils 为 AreaConvert 提供地区编码转换功能
- JsonUtils 为 JsonConvert 提供 JSON 序列化功能
```

### 与系统的集成

Convert 模块主要被 ExcelUtils 类使用，在 Excel 导入导出过程中自动应用相应的转换器。当在实体类的字段上使用 `@ExcelProperty(converter = XXXConvert.class)` 注解时，ExcelUtils 会在处理该字段时调用对应的转换器进行数据转换。

```
序列图说明：
1. 导出流程：Controller调用ExcelUtils.write() → ExcelUtils调用converter.convertToExcelData() → converter通过工具类获取标签 → 返回WriteCellData → Excel生成文件
2. 导入流程：Controller调用ExcelUtils.read() → ExcelUtils调用converter.convertToJavaData() → converter通过工具类解析值 → 返回Java对象 → ExcelUtils返回数据列表
```

## 详细组件说明

### 1. DictConvert (单值数据字典转换器)

**作用**：将数据库中存储的字典值（如数字ID）与 Excel 中显示的字典标签（如中文描述）进行双向转换。

**使用场景**：适用于单选场景，如性别（1=男, 2=女）、状态（0=禁用, 1=正常）等。

**配置方式**：
```java
@ExcelProperty(converter = DictConvert.class)
@DictFormat(type = "sys_user_sex") // 指定字典类型
private Integer sex;
```

**工作原理**：
- Excel → Java：读取 Excel 中的标签（如"男"），通过 DictFrameworkUtils 查询对应的值（如"1"），然后转换为字段类型
- Java → Excel：将字段值（如"1"）通过 DictFrameworkUtils 转换为对应的标签（如"男"）写入 Excel

### 2. MultiDictConvert (多值数据字典转换器)

**作用**：处理多值字典场景，数据库中使用半角逗号分隔存储多个值（如"1,2"），Excel 中使用顿号分隔显示（如"男、女"）。

**使用场景**：适用于多选场景，如用户可以同时具有多个角色、多个兴趣爱好等。

**配置方式**：
```java
@ExcelProperty(converter = MultiDictConvert.class)
@DictFormat(type = "sys_role") // 指定字典类型
private List<Integer> roleIds;
```

**特殊处理**：
- 支持多种Java类型：String、数组、Collection（ArrayList、LinkedHashSet等）
- Excel 分隔符：顿号（、"）
- 数据库分隔符：半角逗号（,）
- 自动处理空值和空格

### 3. JsonConvert (JSON转换器)

**作用**：将 Java 对象序列化为 JSON 字符串写入 Excel，读取 Excel 中的 JSON 字符串反序列化为 Java 对象。

**使用场景**：适用于需要在 Excel 中存储复杂结构数据的场景，如存储配置信息、扩展属性等。

**配置方式**：
```java
@ExcelProperty(converter = JsonConvert.class)
private Map<String, Object> extAttrs;
```

**注意**：由于 Excel 单元格有字符数限制，不适合存储过大的 JSON 数据。

### 4. MoneyConvert (金额转换器)

**作用**：在数据库中以“分”为单位存储金额整数，在 Excel 中以“元”为单位显示小数（保留两位小数）。

**使用场景**：财务系统中金额字段的标准处理方式。

**配置方式**：
```java
@ExcelProperty(converter = MoneyConvert.class)
private Integer amount; // 数据库中存储的分值
```

**转换规则**：
- Java → Excel：amount(分) / 100 = 金额(元)，保留两位小数
- Excel → Java：金额(元) * 100 = amount(分)，取整数部分

### 5. AreaConvert (地区转换器)

**作用**：将地区名称（如"北京市海淀区"）转换为对应的地区编码（如"110108"），反之亦然。

**使用场景**：需要在 Excel 中展示地区名称但在数据库中存储地区编码的场景。

**配置方式**：
```java
@ExcelProperty(converter = AreaConvert.class)
private String areaCode; // 地区编码
```

**依赖**：使用 AreaUtils 进行地区名称和编码之间的转换。

## 使用示例

### 示例1：用户导入导出（包含单值和多值字典）

```java
@Data
public class UserExcelVO {
    @ExcelProperty("用户ID")
    private Long id;
    
    @ExcelProperty("用户名")
    private String username;
    
    @ExcelProperty("性别")
    @DictFormat(type = "sys_user_sex") // 字典类型：用户性别
    private Integer sex;
    
    @ExcelProperty("角色")
    @DictFormat(type = "sys_role") // 字典类型：系统角色
    @ExcelProperty(converter = MultiDictConvert.class)
    private List<Integer> roleIds;
    
    @ExcelProperty("账户状态")
    @DictFormat(type = "common_status") // 字典类型：通用状态
    private Integer status;
}
```

### 示例2：订单导入导出（包含金额和地区）

```java
@Data
public class OrderExcelVO {
    @ExcelProperty("订单ID")
    private Long id;
    
    @ExcelProperty("订单金额(元)")
    @ExcelProperty(converter = MoneyConvert.class)
    private Integer amount; // 数据库中存储的分值
    
    @ExcelProperty("收货地区")
    @ExcelProperty(converter = AreaConvert.class)
    private String areaCode; // 地区编码
    
    @ExcelProperty("订单状态")
    @DictFormat(type = "order_status")
    private Integer status;
}
```

### 示例3：产品配置导入导出（包含JSON）

```java
@Data
public class ProductExcelVO {
    @ExcelProperty("产品ID")
    private Long id;
    
    @ExcelProperty("产品名称")
    private String name;
    
    @ExcelProperty("产品属性(JSON)")
    @ExcelProperty(converter = JsonConvert.class)
    private Map<String, Object> attrs; // 存储产品的扩展属性
    
    @ExcelProperty("是否上架")
    @DictFormat(type = "common_status")
    private Integer status;
}
```

## 错误处理与日志

所有转换器在转换过程中都有完善的错误处理机制：

1. **空值处理**：当输入为 null 或空字符串时，直接返回空值或默认值
2. **转换失败处理**：当无法找到对应的字典值或标签时，记录错误日志并返回 null 或空字符串
3. **格式验证**：在多值转换器中，会自动过滤掉空值和仅包含空格的项

例如，在 DictConvert 中：
```java
if (value == null) {
    log.error("[convertToJavaData][type({}) 解析不掉 label({})]", type, label);
    return null;
}
```

在 MultiDictConvert 中：
```java
if (StrUtil.isBlank(label)) {
    continue; // 跳过空值或仅含空格的项
}
```

## 性能考虑

1. **缓存机制**：转换器内部不进行缓存，依赖于底层的 DictFrameworkUtils 和 AreaUtils 等工具类的缓存机制
2. **对象创建**：在转换过程中会创建临时对象（如列表、数组），但这些对象生命周期很短，对性能影响可忽略不计
3. **日志开关**：错误日志仅在转换失败时输出，正常情况下不会产生日志开销

## 与其他模块的关系

Convert 模块主要依赖以下模块的功能：

1. **dict模块**：提供 DictFrameworkUtils 进行数据字典的查询和转换
2. **ip模块**：提供 AreaUtils 进行地区名称和编码之间的转换
3. **common模块**：提供 JsonUtils 进行 JSON 序列化和反序列化
4. **excel模块**：提供 ExcelUtils 作为转换器的使用入口

```
依赖关系说明：
- Convert模块依赖dict模块的DictFrameworkUtils（用于字典查询）
- Convert模块依赖ip模块/AreaUtils（用于地区编码转换）
- Convert模块依赖common模块/JsonUtils（用于JSON序列化）
- Convert模块被excel模块的ExcelUtils使用，而ExcelUtils又使用EasyExcel框架进行实际的Excel文件操作
```

## 最佳实践

1. **正确指定字典类型**：在使用 DictConvert 和 MultiDictConvert 时，必须通过 @DictFormat 注解指定正确的字典类型
2. **注意金额单位**：使用 MoneyConvert 时，确保数据库字段存储的是“分”单位的整数
3. **JSON大小限制**：使用 JsonConvert 时，注意 Excel 单元格的字符数限制（大约 32K 字符），避免存储过大的 JSON 数据
4. **地区编码一致性**：使用 AreaConvert 时，确保使用的地区编码与 AreaUtils 支持的编码体系一致
5. **错误日志监控**：生产环境中建议监控转换器的错误日志，及时发现数据不一致问题

## 常见问题

**Q：为什么我的字典转换总是返回 null？**
A：请检查 @DictFormat 中指定的字典类型是否正确，以及对应的字典数据是否已在系统中初始化。

**Q：多值字典转换时，Excel 中显示的是 ID 而不是标签？**
A：请确认同时使用了 @ExcelProperty(converter = MultiDictConvert.class) 和 @DictFormat 注解。

**Q：金额转换后显示不正确？**
A：请确认数据库中存储的是“分”单位的整数，而不是“元”单位的小数。

**Q：地区转换找不到对应的编码？**
A：请检查地区名称是否完整匹配（包括省市区），以及 AreaUtils 是否支持该地区编码体系。

## 结论

Convert 模块为 yudao-framework 的 Excel 导入导出功能提供了强大的数据转换能力。通过这五种专门的转换器，开发者可以轻松处理数据字典、JSON、金额和地区等常见的特殊数据类型转换需求，显著提高了数据交互的便利性和准确性。

这些转换器设计简洁易用，同时具备良好的错误处理和日志记录机制，能够在各种业务场景中可靠地工作。