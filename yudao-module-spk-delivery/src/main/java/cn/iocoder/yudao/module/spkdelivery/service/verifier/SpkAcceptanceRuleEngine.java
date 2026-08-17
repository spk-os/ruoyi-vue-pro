package cn.iocoder.yudao.module.spkdelivery.service.verifier;

import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * SPK-OS 验收规则引擎（SpkVerifierService Layer 2 确定性检查用）。
 * <p>
 * 对 {@code verifier-scripts/<ref>/acceptance.json} 中每条规则的 {@code assert} 谓词求值，
 * 返回是否通过 + 失败原因。规则 DSL 设计为最小但真实可执行（非自然语言臆测）：
 *
 * <pre>
 * assert := { "op": <Operator>, ...operands }
 * Operators:
 *   - and      : { of: [assert, ...] }                 全部成立
 *   - or       : { of: [assert, ...] }                 任一成立
 *   - not      : { assert: <assert> }                  非
 *   - implies  : { if: <assert>, then: <assert> }      if→then（!if || then）
 *   - present  : { path: "a.b" }                       路径存在且非 null
 *   - notNull  : { path: "a.b" }                       同 present
 *   - nonEmpty  : { path: "a.b" }                      非空字符串/非空数组对象/存在数值布尔
 *   - range    : { path: "a.b", min: 0, max: 1 }       数值在闭区间
 *   - equals   : { path: "a.b", value: <literal> }     相等（数值/字符串/布尔）
 *   - in       : { path: "a.b", values: [...] }        取值在集合
 *   - minItems : { path: "a.b", min: 1 }               数组长度 >= min
 *   - every    : { path: "arr", item: <assert> }       数组每个元素满足 item（相对元素）
 *   - some     : { path: "arr", item: <assert> }       数组至少一个元素满足 item
 * path 以点分段，仅按 object key 导航；item 内 path 相对当前数组元素。
 * </pre>
 *
 * <p>设计取舍：path 不支持 {@code [*]} 通配（避免与 every 语义重叠造成歧义）；
 * 规则文件可同时保留人类可读 {@code check} 字段用于回执展示，{@code assert} 才是机器执行体。
 *
 * @author SPK-OS
 */
public final class SpkAcceptanceRuleEngine {

    private SpkAcceptanceRuleEngine() {
    }

    /**
     * 对一条规则求值。
     *
     * @param rule    规则节点（含 severity/assert）
     * @param artifact 被校验产物（已解析为 JsonNode）
     * @return 求值结果（passed + 失败原因）
     */
    public static Result evaluate(JsonNode rule, JsonNode artifact) {
        Result r = new Result();
        r.id = text(rule, "id");
        r.name = text(rule, "name");
        r.severity = text(rule, "severity");
        if (r.severity == null || r.severity.isBlank()) {
            r.severity = "conditional";
        }
        JsonNode assertNode = rule.path("assert");
        if (assertNode.isMissingNode() || assertNode.isNull()) {
            // 无 assert 谓词 → 视为信息性规则，跳过确定性检查（交由 Layer 3 LLM）
            r.passed = true;
            r.reason = "无 assert 谓词，跳过确定性检查";
            return r;
        }
        try {
            List<String> failures = new ArrayList<>();
            boolean ok = eval(artifact, assertNode, failures);
            r.passed = ok;
            r.reason = ok ? null : String.join("; ", failures);
        } catch (Exception e) {
            r.passed = false;
            r.reason = "规则求值异常：" + e.getMessage();
        }
        return r;
    }

    // ---------- 谓词求值 ----------

    private static boolean eval(JsonNode node, JsonNode a, List<String> failures) {
        if (a == null || a.isMissingNode() || a.isNull()) {
            return true;
        }
        String op = text(a, "op");
        if (op == null || op.isBlank()) {
            return true;
        }
        switch (op) {
            case "and": {
                JsonNode of = a.path("of");
                if (!of.isArray()) {
                    return true;
                }
                boolean all = true;
                for (JsonNode sub : of) {
                    if (!eval(node, sub, failures)) {
                        all = false;
                    }
                }
                return all;
            }
            case "or": {
                JsonNode of = a.path("of");
                if (!of.isArray()) {
                    return true;
                }
                List<String> subFailures = new ArrayList<>();
                for (JsonNode sub : of) {
                    if (eval(node, sub, subFailures)) {
                        return true;
                    }
                }
                failures.addAll(subFailures);
                return false;
            }
            case "not": {
                JsonNode inner = a.has("assert") ? a.path("assert") : a.path("of");
                List<String> subFailures = new ArrayList<>();
                boolean innerOk = eval(node, inner, subFailures);
                return !innerOk;
            }
            case "implies": {
                JsonNode ifA = a.path("if");
                List<String> ifFailures = new ArrayList<>();
                boolean ifOk = eval(node, ifA, ifFailures);
                if (!ifOk) {
                    return true; // 前件不成立 → 蕴含式真
                }
                JsonNode thenA = a.path("then");
                List<String> thenFailures = new ArrayList<>();
                boolean thenOk = eval(node, thenA, thenFailures);
                if (!thenOk) {
                    failures.addAll(thenFailures);
                    return false;
                }
                return true;
            }
            case "present":
            case "notNull": {
                JsonNode t = navigate(node, text(a, "path"));
                boolean ok = !t.isMissingNode() && !t.isNull();
                if (!ok) {
                    failures.add(op + "(" + text(a, "path") + ") 字段缺失或为 null");
                }
                return ok;
            }
            case "nonEmpty": {
                JsonNode t = navigate(node, text(a, "path"));
                boolean ok = nonEmpty(t);
                if (!ok) {
                    failures.add("nonEmpty(" + text(a, "path") + ") 为空");
                }
                return ok;
            }
            case "range": {
                JsonNode t = navigate(node, text(a, "path"));
                if (!t.isNumber()) {
                    failures.add("range(" + text(a, "path") + ") 非数值：" + describe(t));
                    return false;
                }
                double v = t.asDouble();
                JsonNode min = a.path("min");
                JsonNode max = a.path("max");
                boolean ok = true;
                if (min.isNumber() && v < min.asDouble()) {
                    ok = false;
                }
                if (max.isNumber() && v > max.asDouble()) {
                    ok = false;
                }
                if (!ok) {
                    failures.add("range(" + text(a, "path") + ")=" + v + " 越界 [" + min + "," + max + "]");
                }
                return ok;
            }
            case "equals": {
                JsonNode t = navigate(node, text(a, "path"));
                JsonNode val = a.path("value");
                boolean ok = nodesEqual(val, t);
                if (!ok) {
                    failures.add("equals(" + text(a, "path") + ")=" + describe(t) + " 期望 " + describe(val));
                }
                return ok;
            }
            case "in": {
                JsonNode t = navigate(node, text(a, "path"));
                JsonNode vals = a.path("values");
                boolean ok = false;
                if (vals.isArray()) {
                    for (JsonNode v : vals) {
                        if (nodesEqual(v, t)) {
                            ok = true;
                            break;
                        }
                    }
                }
                if (!ok) {
                    failures.add("in(" + text(a, "path") + ")=" + describe(t) + " 不在 " + vals);
                }
                return ok;
            }
            case "minItems": {
                JsonNode t = navigate(node, text(a, "path"));
                if (!t.isArray()) {
                    failures.add("minItems(" + text(a, "path") + ") 非数组：" + describe(t));
                    return false;
                }
                int min = a.path("min").asInt(0);
                boolean ok = t.size() >= min;
                if (!ok) {
                    failures.add("minItems(" + text(a, "path") + ")=" + t.size() + " < " + min);
                }
                return ok;
            }
            case "every": {
                JsonNode arr = navigate(node, text(a, "path"));
                if (!arr.isArray()) {
                    failures.add("every(" + text(a, "path") + ") 非数组：" + describe(arr));
                    return false;
                }
                JsonNode item = a.path("item");
                boolean all = true;
                int idx = 0;
                for (JsonNode el : arr) {
                    List<String> sub = new ArrayList<>();
                    if (!eval(el, item, sub)) {
                        all = false;
                        failures.add("[" + idx + "] " + String.join("; ", sub));
                    }
                    idx++;
                }
                return all;
            }
            case "some": {
                JsonNode arr = navigate(node, text(a, "path"));
                if (!arr.isArray()) {
                    failures.add("some(" + text(a, "path") + ") 非数组：" + describe(arr));
                    return false;
                }
                JsonNode item = a.path("item");
                List<String> anyFailures = new ArrayList<>();
                for (JsonNode el : arr) {
                    if (eval(el, item, anyFailures)) {
                        return true;
                    }
                    anyFailures.clear();
                }
                failures.add("some(" + text(a, "path") + ") 无任一元素满足");
                return false;
            }
            default:
                // 未知 op 跳过（不报错，交由后续扩展）
                return true;
        }
    }

    // ---------- 工具 ----------

    /** 按点分路径导航；仅 object key，遇 array 自动跳过（every/some 负责数组迭代）。 */
    private static JsonNode navigate(JsonNode node, String path) {
        if (node == null || path == null || path.isBlank()) {
            return missing();
        }
        JsonNode cur = node;
        for (String seg : path.split("\\.")) {
            if (seg.isEmpty()) {
                continue;
            }
            cur = cur.path(seg);
            if (cur.isMissingNode()) {
                return cur;
            }
        }
        return cur;
    }

    private static boolean nonEmpty(JsonNode t) {
        if (t == null || t.isMissingNode() || t.isNull()) {
            return false;
        }
        if (t.isTextual()) {
            return !t.asText().isBlank();
        }
        if (t.isArray() || t.isObject()) {
            return t.size() > 0;
        }
        return true; // 数值/布尔存在即非空
    }

    private static boolean nodesEqual(JsonNode a, JsonNode b) {
        if (a == null || b == null || a.isMissingNode() || b.isMissingNode()) {
            return false;
        }
        if (a.isBoolean() || b.isBoolean()) {
            return a.isBoolean() && b.isBoolean() && a.asBoolean() == b.asBoolean();
        }
        if (a.isNumber() && b.isNumber()) {
            return a.asDouble() == b.asDouble();
        }
        if (a.isTextual() && b.isTextual()) {
            return a.asText().equals(b.asText());
        }
        return a.equals(b);
    }

    private static String text(JsonNode node, String field) {
        JsonNode n = node == null ? null : node.path(field);
        return (n == null || n.isMissingNode() || n.isNull()) ? null : n.asText();
    }

    private static String describe(JsonNode node) {
        if (node == null || node.isMissingNode()) {
            return "missing";
        }
        if (node.isNull()) {
            return "null";
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isArray()) {
            return "array[" + node.size() + "]";
        }
        if (node.isObject()) {
            return "object";
        }
        return node.toString();
    }

    private static JsonNode missing() {
        // tools.jackson MissingNode 单例
        return tools.jackson.databind.node.MissingNode.getInstance();
    }

    /** 求值结果。 */
    public static final class Result {
        public String id;
        public String name;
        /** fail / conditional。 */
        public String severity;
        public boolean passed;
        public String reason;

        public boolean isFail() {
            return "fail".equalsIgnoreCase(severity);
        }
    }
}
