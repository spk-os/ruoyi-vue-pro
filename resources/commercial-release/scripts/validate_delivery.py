#!/usr/bin/env python3
"""校验单次商业发布 IPD Activity 的产物、证据、验证和人审契约。"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
from pathlib import Path
from typing import Any


PLACEHOLDER_RE = re.compile(r"(?:\bTODO\b|\bTBD\b|\bFIXME\b|待补|示例值|placeholder)", re.IGNORECASE)
TEMPLATE_VAR_RE = re.compile(
    r"(?:\{\{[^{}]+\}\}|__REQUIRED__|\$\{[^{}]+\}|<REQUIRED:[^>]+>)",
    re.IGNORECASE,
)
SHA256_RE = re.compile(r"^[0-9a-f]{64}$")
REQUIRED_TOP = {
    "schemaVersion", "activityId", "stage", "status", "producer", "skill", "execution",
    "artifacts", "evidence", "claims", "traceability", "verifier", "humanApproval",
}


def _load_json(path: Path, errors: list[str], label: str) -> dict[str, Any] | None:
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        errors.append(f"{label} 无法解析：{exc}")
        return None
    if not isinstance(value, dict):
        errors.append(f"{label} 必须是 JSON object")
        return None
    return value


def _safe_file(root: Path, rel: Any, errors: list[str], label: str) -> Path | None:
    if not isinstance(rel, str) or not rel.strip():
        errors.append(f"{label} 路径为空")
        return None
    candidate = (root / rel).resolve()
    try:
        candidate.relative_to(root)
    except ValueError:
        errors.append(f"{label} 路径逃逸交付目录：{rel}")
        return None
    if not candidate.is_file():
        errors.append(f"{label} 文件不存在：{rel}")
        return None
    return candidate


def _verify_hash(path: Path, expected: Any, errors: list[str], label: str) -> None:
    if not isinstance(expected, str) or not SHA256_RE.fullmatch(expected):
        errors.append(f"{label} sha256 格式错误")
        return
    actual = hashlib.sha256(path.read_bytes()).hexdigest()
    if actual != expected:
        errors.append(f"{label} sha256 不匹配：expected={expected}, actual={actual}")


def _validate_receipt(path: Path, kind: str, activity_id: str, errors: list[str]) -> dict[str, Any] | None:
    receipt = _load_json(path, errors, kind)
    if receipt is None:
        return None
    if receipt.get("activityId") != activity_id:
        errors.append(f"{kind}.activityId 与本次 Activity 不一致")
    if not receipt.get("signature"):
        errors.append(f"{kind} 缺少 signature/provenance")
    return receipt


def validate_delivery(result_path: Path, pack_root: Path | None = None) -> list[str]:
    result_path = result_path.resolve()
    delivery_root = result_path.parent
    pack_root = (pack_root or Path(__file__).resolve().parents[1]).resolve()
    errors: list[str] = []
    result = _load_json(result_path, errors, "activity-result.json")
    if result is None:
        return errors
    missing = sorted(REQUIRED_TOP - result.keys())
    if missing:
        errors.append(f"activity-result.json 缺字段：{missing}")
    serialized = json.dumps(result, ensure_ascii=False)
    matches = sorted(set(m.group(0) for m in PLACEHOLDER_RE.finditer(serialized)))
    if matches:
        errors.append(f"交付结果包含占位符：{matches}")
    template_vars = sorted(set(m.group(0) for m in TEMPLATE_VAR_RE.finditer(serialized)))
    if template_vars:
        errors.append(f"交付结果包含未渲染模板变量：{template_vars}")

    catalog_data = _load_json(pack_root / "references" / "activity-catalog.json", errors, "activity catalog")
    if catalog_data is None:
        return errors
    catalog = {a.get("id"): a for a in catalog_data.get("activities", []) if isinstance(a, dict)}
    activity_id = result.get("activityId")
    activity = catalog.get(activity_id)
    if activity is None:
        errors.append(f"未知 Activity ID：{activity_id}")
        return errors
    if result.get("stage") != activity["stage"]:
        errors.append(f"stage 应为 {activity['stage']}，实际 {result.get('stage')}")
    skill = result.get("skill") if isinstance(result.get("skill"), dict) else {}
    if skill.get("name") != activity["skill"]:
        errors.append(f"Skill 应为 {activity['skill']}，实际 {skill.get('name')}")
    if not skill.get("version"):
        errors.append("缺少 Skill version")
    if result.get("status") not in {"COMPLETE", "FAILED", "BLOCKED"}:
        errors.append("status 只能是 COMPLETE/FAILED/BLOCKED")

    producer = result.get("producer") if isinstance(result.get("producer"), dict) else {}
    if not producer.get("actorId") or not producer.get("role"):
        errors.append("producer 必须包含 actorId 和 role")
    execution = result.get("execution") if isinstance(result.get("execution"), dict) else {}
    for key in ("runId", "startedAt", "finishedAt", "model"):
        if not execution.get(key):
            errors.append(f"execution 缺少 {key}")
    tools = execution.get("tools")
    if not isinstance(tools, list) or not tools:
        errors.append("execution.tools 不能为空")
    else:
        for index, tool in enumerate(tools):
            if not isinstance(tool, dict) or not tool.get("name") or not tool.get("version"):
                errors.append(f"execution.tools[{index}] 缺 name/version")
            if not isinstance(tool, dict) or tool.get("exitCode") != 0:
                errors.append(f"execution.tools[{index}] 未成功执行")

    artifacts = result.get("artifacts") if isinstance(result.get("artifacts"), list) else []
    evidence = result.get("evidence") if isinstance(result.get("evidence"), list) else []
    if not artifacts:
        errors.append("artifacts 不能为空")
    if not evidence:
        errors.append("evidence 不能为空")
    artifact_ids: set[str] = set()
    has_primary = False
    for index, item in enumerate(artifacts):
        if not isinstance(item, dict):
            errors.append(f"artifacts[{index}] 非 object")
            continue
        item_id = item.get("artifactId")
        if not item_id or item_id in artifact_ids:
            errors.append(f"artifacts[{index}] artifactId 为空或重复")
        artifact_ids.add(item_id)
        has_primary |= item.get("type") == activity["artifact"]
        path = _safe_file(delivery_root, item.get("path"), errors, f"artifacts[{index}]")
        if path:
            _verify_hash(path, item.get("sha256"), errors, f"artifacts[{index}]")
    if not has_primary:
        errors.append(f"缺少主交付物类型：{activity['artifact']}")

    evidence_ids: set[str] = set()
    for index, item in enumerate(evidence):
        if not isinstance(item, dict):
            errors.append(f"evidence[{index}] 非 object")
            continue
        item_id = item.get("evidenceId")
        if not item_id or item_id in evidence_ids:
            errors.append(f"evidence[{index}] evidenceId 为空或重复")
        evidence_ids.add(item_id)
        path = _safe_file(delivery_root, item.get("path"), errors, f"evidence[{index}]")
        if path:
            _verify_hash(path, item.get("sha256"), errors, f"evidence[{index}]")

    claims = result.get("claims") if isinstance(result.get("claims"), list) else []
    if not claims:
        errors.append("claims 不能为空")
    for index, claim in enumerate(claims):
        refs = claim.get("evidenceIds", []) if isinstance(claim, dict) else []
        if not refs:
            errors.append(f"claims[{index}] 没有 evidenceIds")
        unknown = sorted(set(refs) - evidence_ids)
        if unknown:
            errors.append(f"claims[{index}] 引用未知证据：{unknown}")
    if not isinstance(result.get("traceability"), list) or not result.get("traceability"):
        errors.append("traceability 不能为空")

    complete = result.get("status") == "COMPLETE"
    verifier_required = bool(activity["independentVerifier"])
    verifier = result.get("verifier") if isinstance(result.get("verifier"), dict) else {}
    if verifier_required and complete:
        if verifier.get("actorId") == producer.get("actorId") or not verifier.get("actorId"):
            errors.append("独立验证 actorId 必须存在且不同于 producer.actorId")
        if verifier.get("verdict") != "PASS":
            errors.append("独立验证必须 PASS 才能 COMPLETE")
        receipt_path = _safe_file(delivery_root, verifier.get("receiptPath"), errors, "独立验证 receipt")
        if receipt_path:
            receipt = _validate_receipt(receipt_path, "VerificationReceipt", activity_id, errors)
            if receipt and receipt.get("verifierActorId") != verifier.get("actorId"):
                errors.append("VerificationReceipt.verifierActorId 与 activity-result 不一致")

    approval_required = bool(activity["humanGate"])
    approval = result.get("humanApproval") if isinstance(result.get("humanApproval"), dict) else {}
    if approval_required and complete:
        if approval.get("status") != "APPROVED":
            errors.append("人工审批必须 APPROVED 才能 COMPLETE")
        record_path = _safe_file(delivery_root, approval.get("recordPath"), errors, "人工审批 record")
        if record_path:
            record = _validate_receipt(record_path, "HumanApprovalRecord", activity_id, errors)
            if record and record.get("decision") != "APPROVED":
                errors.append("HumanApprovalRecord.decision 必须为 APPROVED")
    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("result", type=Path, help="activity-result.json 路径")
    parser.add_argument("--pack-root", type=Path, default=Path(__file__).resolve().parents[1])
    args = parser.parse_args()
    errors = validate_delivery(args.result, args.pack_root)
    if errors:
        print("FAIL: Activity 交付校验失败")
        for error in errors:
            print(f"- {error}")
        return 1
    print("PASS: Activity 交付物、证据、追溯、验证与人审契约通过")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
