package cn.iocoder.yudao.module.spkdelivery.service.verifier;

import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * SPK-OS JSON Schema 子集校验器（SpkVerifierService Layer 1 结构校验用）。
 * <p>
 * 自实现而非引入 com.networknt:json-schema-validator，原因是 yudao Boot4 运行时使用
 * Jackson 3（包 {@code tools.jackson}），而 networknt 3.0.1 依赖 Jackson 2（包
 * {@code com.fasterxml.jackson}），两者 JsonNode 不互通，强行引入会造成双 Jackson 与
 * 包冲突。本校验器覆盖 IPD 产物 schema.json 实际使用的关键字子集，足以做真实结构校验
 * （非桩），缺漏的关键字仅跳过不报错，schema.json 增强时按需补关键字分支即可。
 *
 * <p>支持关键字：{@code type / required / properties / items / minItems / minLength /
 * minimum / maximum / enum / pattern}（顶级与 items/properties 递归）。忽略
 * {@code $schema / title / description / additionalProperties} 等元数据关键字。
 *
 * @author SPK-OS
 */
public final class SpkJsonSchemaValidator {

    private SpkJsonSchemaValidator() {
    }

    /**
     * 用 schema 校验 instance，返回错误信息列表（空表示通过）。
     * 每条错误格式：{@code <jsonPointer> : <message>}。
     */
    public static List<String> validate(JsonNode schema, JsonNode instance) {
        List<String> errors = new ArrayList<>();
        if (schema == null || schema.isMissingNode() || schema.isNull()) {
            return errors; // 无 schema 视为通过
        }
        validateNode(schema, instance, "", errors);
        return errors;
    }

    private static void validateNode(JsonNode schema, JsonNode node, String path, List<String> errors) {
        if (schema == null || schema.isMissingNode()) {
            return;
        }
        // type
        JsonNode typeNode = schema.path("type");
        if (typeNode.isTextual()) {
            checkType(typeNode.asText(), node, path, errors);
        }
        // enum
        JsonNode enumNode = schema.path("enum");
        if (enumNode.isArray()) {
            boolean ok = false;
            for (JsonNode e : enumNode) {
                if (nodesEqual(e, node)) {
                    ok = true;
                    break;
                }
            }
            if (!ok) {
                errors.add(path + " : 值不在 enum 允许集合 " + enumNode + "（实际=" + node + "）");
            }
        }
        // pattern（仅对字符串）
        JsonNode patternNode = schema.path("pattern");
        if (patternNode.isTextual() && node.isTextual()) {
            try {
                if (!Pattern.compile(patternNode.asText()).matcher(node.asText()).find()) {
                    errors.add(path + " : 不匹配 pattern " + patternNode.asText() + "（实际=" + truncate(node.asText(), 60) + "）");
                }
            } catch (Exception ignore) {
                // pattern 自身非法则跳过
            }
        }
        // 数值范围
        JsonNode minNode = schema.path("minimum");
        if (minNode.isNumber() && node.isNumber() && node.asDouble() < minNode.asDouble()) {
            errors.add(path + " : 小于 minimum " + minNode.asDouble() + "（实际=" + node.asDouble() + "）");
        }
        JsonNode maxNode = schema.path("maximum");
        if (maxNode.isNumber() && node.isNumber() && node.asDouble() > maxNode.asDouble()) {
            errors.add(path + " : 大于 maximum " + maxNode.asDouble() + "（实际=" + node.asDouble() + "）");
        }
        // 字符串长度
        JsonNode minLen = schema.path("minLength");
        if (minLen.isNumber() && node.isTextual() && node.asText().length() < minLen.asInt()) {
            errors.add(path + " : 字符串长度 < minLength " + minLen.asInt() + "（实际=" + node.asText().length() + "）");
        }
        // 数组项数
        JsonNode minItems = schema.path("minItems");
        if (minItems.isNumber() && node.isArray() && node.size() < minItems.asInt()) {
            errors.add(path + " : 数组项数 < minItems " + minItems.asInt() + "（实际=" + node.size() + "）");
        }
        // required（仅对 object）
        JsonNode reqNode = schema.path("required");
        if (reqNode.isArray() && node.isObject()) {
            for (JsonNode r : reqNode) {
                String key = r.asText();
                JsonNode child = node.path(key);
                if (child.isMissingNode() || child.isNull()) {
                    errors.add(path + "/" + key + " : 缺失必填字段");
                }
            }
        }
        // properties（递归 object 子字段）
        JsonNode propsNode = schema.path("properties");
        if (propsNode.isObject() && node.isObject()) {
            // Jackson 3 把 fieldNames() 从 JsonNode 移到 ObjectNode，用 forEachEntry 遍历属性
            final JsonNode parentNode = node;
            final String base = path;
            propsNode.forEachEntry((propName, childSchema) -> {
                JsonNode child = parentNode.path(propName);
                if (!child.isMissingNode()) {
                    validateNode(childSchema, child,
                            base.isEmpty() ? "/" + propName : base + "/" + propName, errors);
                }
            });
        }
        // items（递归 array 元素）
        JsonNode itemsNode = schema.path("items");
        if (itemsNode.isObject() && node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                validateNode(itemsNode, node.get(i), path + "/" + i, errors);
            }
        }
    }

    private static void checkType(String type, JsonNode node, String path, List<String> errors) {
        boolean ok;
        switch (type) {
            case "object": ok = node.isObject(); break;
            case "array": ok = node.isArray(); break;
            case "string": ok = node.isTextual(); break;
            case "integer": ok = node.isIntegralNumber(); break;
            case "number": ok = node.isNumber(); break;
            case "boolean": ok = node.isBoolean(); break;
            case "null": ok = node.isNull(); break;
            default: ok = true; // 未知类型不报错
        }
        if (!ok) {
            errors.add(path + " : 类型应为 " + type + "（实际=" + describe(node) + "）");
        }
    }

    private static boolean nodesEqual(JsonNode a, JsonNode b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.isTextual() && b.isTextual()) {
            return a.asText().equals(b.asText());
        }
        if (a.isNumber() && b.isNumber()) {
            return a.asDouble() == b.asDouble();
        }
        if (a.isBoolean() && b.isBoolean()) {
            return a.asBoolean() == b.asBoolean();
        }
        return a.equals(b);
    }

    private static String describe(JsonNode node) {
        if (node == null || node.isMissingNode()) {
            return "missing";
        }
        if (node.isObject()) {
            return "object";
        }
        if (node.isArray()) {
            return "array";
        }
        if (node.isTextual()) {
            return "string";
        }
        if (node.isNumber()) {
            return "number";
        }
        if (node.isBoolean()) {
            return "boolean";
        }
        if (node.isNull()) {
            return "null";
        }
        return node.toString();
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
