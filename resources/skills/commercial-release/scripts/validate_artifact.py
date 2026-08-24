#!/usr/bin/env python3
"""在 Producer 返回前执行与 Cortex Layer1/Layer2 同源的产物预检。"""

from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path
from typing import Any


SAFE_SEGMENT = re.compile(r"^[A-Za-z0-9._-]+$")


def _load_json(path: Path, label: str, errors: list[str]) -> Any:
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        errors.append(f"{label} 无法解析：{exc}")
        return None


def _kind(value: Any) -> str:
    if value is None:
        return "null"
    if isinstance(value, bool):
        return "boolean"
    if isinstance(value, dict):
        return "object"
    if isinstance(value, list):
        return "array"
    if isinstance(value, str):
        return "string"
    if isinstance(value, int):
        return "integer"
    if isinstance(value, float):
        return "number"
    return type(value).__name__


def _type_matches(expected: str, value: Any) -> bool:
    return {
        "object": isinstance(value, dict),
        "array": isinstance(value, list),
        "string": isinstance(value, str),
        "integer": isinstance(value, int) and not isinstance(value, bool),
        "number": isinstance(value, (int, float)) and not isinstance(value, bool),
        "boolean": isinstance(value, bool),
        "null": value is None,
    }.get(expected, True)


def _json_equal(left: Any, right: Any) -> bool:
    if isinstance(left, bool) or isinstance(right, bool):
        return isinstance(left, bool) and isinstance(right, bool) and left == right
    if isinstance(left, (int, float)) and isinstance(right, (int, float)):
        return float(left) == float(right)
    return type(left) is type(right) and left == right


def _validate_schema(schema: Any, value: Any, path: str, errors: list[str]) -> None:
    if not isinstance(schema, dict):
        return
    expected = schema.get("type")
    if isinstance(expected, str) and not _type_matches(expected, value):
        errors.append(f"{path or '/'} : 类型应为 {expected}（实际={_kind(value)}）")

    enum = schema.get("enum")
    if isinstance(enum, list) and not any(_json_equal(item, value) for item in enum):
        errors.append(f"{path or '/'} : 值不在 enum 允许集合 {enum!r}（实际={value!r}）")

    pattern = schema.get("pattern")
    if isinstance(pattern, str) and isinstance(value, str):
        try:
            if re.search(pattern, value) is None:
                errors.append(f"{path or '/'} : 不匹配 pattern {pattern}（实际={value[:60]}）")
        except re.error:
            pass

    if isinstance(value, (int, float)) and not isinstance(value, bool):
        minimum = schema.get("minimum")
        maximum = schema.get("maximum")
        if isinstance(minimum, (int, float)) and value < minimum:
            errors.append(f"{path or '/'} : 小于 minimum {minimum}（实际={value}）")
        if isinstance(maximum, (int, float)) and value > maximum:
            errors.append(f"{path or '/'} : 大于 maximum {maximum}（实际={value}）")

    min_length = schema.get("minLength")
    if isinstance(min_length, (int, float)) and isinstance(value, str) and len(value) < int(min_length):
        errors.append(f"{path or '/'} : 字符串长度 < minLength {int(min_length)}（实际={len(value)}）")

    min_items = schema.get("minItems")
    if isinstance(min_items, (int, float)) and isinstance(value, list) and len(value) < int(min_items):
        errors.append(f"{path or '/'} : 数组项数 < minItems {int(min_items)}（实际={len(value)}）")

    if isinstance(value, dict):
        required = schema.get("required")
        if isinstance(required, list):
            for key in required:
                if isinstance(key, str) and (key not in value or value[key] is None):
                    errors.append(f"{path}/{key} : 缺失必填字段")
        properties = schema.get("properties")
        if isinstance(properties, dict):
            for key, child_schema in properties.items():
                if key in value:
                    _validate_schema(child_schema, value[key], f"{path}/{key}", errors)

    items = schema.get("items")
    if isinstance(items, dict) and isinstance(value, list):
        for index, item in enumerate(value):
            _validate_schema(items, item, f"{path}/{index}", errors)


def _navigate(value: Any, path: Any) -> tuple[bool, Any]:
    current = value
    if not isinstance(path, str) or not path:
        return True, current
    for segment in path.split("."):
        if not isinstance(current, dict) or segment not in current:
            return False, None
        current = current[segment]
    return True, current


def _non_empty(exists: bool, value: Any) -> bool:
    if not exists or value is None:
        return False
    if isinstance(value, (str, list, dict)):
        return len(value) > 0
    return True


def _eval_assert(value: Any, assertion: Any) -> tuple[bool, list[str]]:
    if not isinstance(assertion, dict):
        return True, []
    op = assertion.get("op")
    if not isinstance(op, str) or not op:
        return True, []

    if op == "and":
        failures: list[str] = []
        passed = True
        for child in assertion.get("of", []) if isinstance(assertion.get("of"), list) else []:
            child_passed, child_failures = _eval_assert(value, child)
            passed &= child_passed
            failures.extend(child_failures)
        return passed, failures
    if op == "or":
        all_failures: list[str] = []
        children = assertion.get("of", []) if isinstance(assertion.get("of"), list) else []
        for child in children:
            child_passed, child_failures = _eval_assert(value, child)
            if child_passed:
                return True, []
            all_failures.extend(child_failures)
        return False, all_failures
    if op == "not":
        inner = assertion.get("assert", assertion.get("of"))
        passed, _ = _eval_assert(value, inner)
        return (not passed), ([] if passed else [])
    if op == "implies":
        antecedent, _ = _eval_assert(value, assertion.get("if"))
        if not antecedent:
            return True, []
        return _eval_assert(value, assertion.get("then"))

    path = assertion.get("path")
    exists, target = _navigate(value, path)
    if op in {"present", "notNull"}:
        passed = exists and target is not None
        return passed, [] if passed else [f"{op}({path}) 字段缺失或为 null"]
    if op == "nonEmpty":
        passed = _non_empty(exists, target)
        return passed, [] if passed else [f"nonEmpty({path}) 为空"]
    if op == "range":
        if not exists or isinstance(target, bool) or not isinstance(target, (int, float)):
            return False, [f"range({path}) 非数值：{_kind(target)}"]
        minimum, maximum = assertion.get("min"), assertion.get("max")
        passed = not (isinstance(minimum, (int, float)) and target < minimum) and not (
            isinstance(maximum, (int, float)) and target > maximum
        )
        return passed, [] if passed else [f"range({path})={target} 越界 [{minimum},{maximum}]"]
    if op == "equals":
        passed = exists and _json_equal(assertion.get("value"), target)
        return passed, [] if passed else [f"equals({path})={target!r} 期望 {assertion.get('value')!r}"]
    if op == "in":
        values = assertion.get("values")
        passed = exists and isinstance(values, list) and any(_json_equal(item, target) for item in values)
        return passed, [] if passed else [f"in({path})={target!r} 不在 {values!r}"]
    if op == "minItems":
        minimum = assertion.get("min", 0)
        if not exists or not isinstance(target, list):
            return False, [f"minItems({path}) 非数组：{_kind(target)}"]
        passed = len(target) >= int(minimum) if isinstance(minimum, (int, float)) else True
        return passed, [] if passed else [f"minItems({path})={len(target)} < {minimum}"]
    if op in {"every", "some"}:
        if not exists or not isinstance(target, list):
            return False, [f"{op}({path}) 非数组：{_kind(target)}"]
        child = assertion.get("item")
        outcomes = [_eval_assert(item, child) for item in target]
        if op == "every":
            failures = [f"[{index}] {'; '.join(result[1])}" for index, result in enumerate(outcomes)
                        if not result[0]]
            return not failures, failures
        passed = any(result[0] for result in outcomes)
        return passed, [] if passed else [f"some({path}) 无任一元素满足"]
    return True, []


def validate_artifact(input_path: Path, contracts_root: Path, artifact_type: str) -> list[str]:
    errors: list[str] = []
    if not SAFE_SEGMENT.fullmatch(artifact_type):
        return ["artifact-type 含非法路径字符"]
    base = contracts_root.resolve()
    contract_dir = (base / artifact_type).resolve()
    try:
        contract_dir.relative_to(base)
    except ValueError:
        return ["artifact-type 越过 contracts-root"]

    envelope = _load_json(input_path.resolve(), "主产物", errors)
    if envelope is None:
        return errors
    if not isinstance(envelope, dict):
        return ["主产物必须是 JSON object envelope"]
    document = envelope.get("document")
    if not isinstance(document, dict):
        errors.append("主产物 document 必须是 JSON object")
    if not isinstance(envelope.get("summary"), str) or not envelope.get("summary", "").strip():
        errors.append("主产物 summary 必须是非空字符串")
    if not isinstance(envelope.get("conclusion"), str) or not envelope.get("conclusion", "").strip():
        errors.append("主产物 conclusion 必须是非空字符串")
    if not isinstance(document, dict):
        return errors

    schema = _load_json(contract_dir / "schema.json", "schema.json", errors)
    acceptance = _load_json(contract_dir / "acceptance.json", "acceptance.json", errors)
    if isinstance(schema, dict):
        _validate_schema(schema, document, "", errors)
    if isinstance(acceptance, dict):
        rules = acceptance.get("rules")
        if isinstance(rules, list):
            for rule in rules:
                if not isinstance(rule, dict):
                    continue
                passed, failures = _eval_assert(document, rule.get("assert"))
                if not passed:
                    errors.append(
                        f"验收规则 {rule.get('id', '(unknown)')} [{rule.get('severity', 'conditional')}]："
                        + "; ".join(failures)
                    )
    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--contracts-root", required=True, type=Path)
    parser.add_argument("--artifact-type", required=True)
    parser.add_argument("--input", required=True, type=Path)
    args = parser.parse_args()
    errors = validate_artifact(args.input, args.contracts_root, args.artifact_type)
    if errors:
        print("FAIL: Cortex 产物确定性预检未通过")
        for error in errors:
            print(f"- {error}")
        return 1
    print("PASS: Cortex envelope、Schema 与 Acceptance 预检通过")
    return 0


if __name__ == "__main__":
    sys.exit(main())
