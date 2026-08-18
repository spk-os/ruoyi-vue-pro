#!/usr/bin/env python3
"""离线校验 commercial-release v2 Skill 包的执行能力、来源追溯和命名。"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
from collections import Counter
from pathlib import Path


EXPECTED_COUNTS = {"concept": 8, "plan": 6, "develop": 11, "verify": 9, "launch": 6, "lifecycle": 10}
STAGE_NUMBERS = {
    "concept": "01", "plan": "02", "develop": "03",
    "verify": "04", "launch": "05", "lifecycle": "06",
}
ENTRY_TOKENS = ("读取顺序", "输入就绪", "执行工作流", "完成定义", "失败处理", "移交")
SPEC_TOKENS = (
    "目标与非目标", "输入契约", "工具与执行顺序", "领域执行算法",
    "输出契约", "验收与完成判定", "失败、重试、回滚与升级", "上下游移交",
)
TEMPLATE_TOKENS = ("输入与基线", "业务内容", "追溯矩阵", "证据清单", "验收执行记录")
REQUIRED_FILES = (
    "references/agent-execution-contract.md", "references/ai-native-controls.md",
    "references/support-activity-routing.md", "schemas/activity-result.schema.json",
    "schemas/verification-receipt.schema.json", "schemas/human-approval-record.schema.json",
    "templates/activity-result.template.json", "templates/verification-receipt.template.json",
    "templates/human-approval-record.template.json",
    "spk-ipd-commercial-release/SKILL.md",
    "spk-ipd-commercial-release/references/end-to-end-playbook.md",
)


def sha256_text(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def frontmatter_keys(text: str) -> set[str]:
    if not text.startswith("---\n"):
        return set()
    end = text.find("\n---\n", 4)
    if end < 0:
        return set()
    keys = set()
    for line in text[4:end].splitlines():
        if ":" in line:
            keys.add(line.split(":", 1)[0].strip())
    return keys


def validate_pack(root: Path) -> list[str]:
    root = root.resolve()
    errors: list[str] = []
    catalog_path = root / "references" / "activity-catalog.json"
    if not catalog_path.is_file():
        return ["缺少 references/activity-catalog.json"]
    try:
        catalog = json.loads(catalog_path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        return [f"activity catalog 无法解析：{exc}"]

    activities = catalog.get("activities", [])
    if len(activities) != 50:
        errors.append(f"核心 Activity 应为 50，实际 {len(activities)}")
    if catalog.get("stageCounts") != EXPECTED_COUNTS:
        errors.append(f"阶段数量错误：{catalog.get('stageCounts')}，期望 {EXPECTED_COUNTS}")
    actual_counts = Counter(a.get("stage") for a in activities)
    if dict(actual_counts) != EXPECTED_COUNTS:
        errors.append(f"Activity 阶段分布错误：{dict(actual_counts)}")

    for label, key in (("Activity ID", "id"), ("Skill", "skill"), ("阶段序号", "stageNo")):
        values = [a.get(key) for a in activities]
        if label != "阶段序号":
            duplicates = sorted(v for v, count in Counter(values).items() if count > 1)
            if duplicates:
                errors.append(f"{label} 重复：{duplicates}")

    source_digests: set[str] = set()
    execution_digests: set[str] = set()
    for activity in activities:
        required = {
            "id", "stage", "stageNo", "activityNo", "name", "skill", "artifact",
            "template", "source", "sourceLines", "sourceSha256", "humanResponsibility",
            "verifierPolicy",
        }
        missing = sorted(k for k in required if activity.get(k) in (None, ""))
        if missing:
            errors.append(f"{activity.get('id', '<unknown>')} 缺字段：{missing}")
            continue

        stage = activity["stage"]
        stage_no = STAGE_NUMBERS.get(stage)
        if activity["stageNo"] != stage_no:
            errors.append(f"{activity['id']} stageNo 应为 {stage_no}")
        expected = rf"^spk-ipd-{stage_no}-{stage}-{activity['activityNo']}-[a-z0-9-]+$"
        if not re.match(expected, activity["skill"]) or len(activity["skill"]) > 64:
            errors.append(f"{activity['id']} Skill 命名不合规：{activity['skill']}")

        skill_root = root / activity["skill"]
        entry_path = skill_root / "SKILL.md"
        spec_path = skill_root / "references" / "execution-spec.md"
        copy_path = skill_root / "references" / "source-design.md"
        examples_path = skill_root / "references" / "examples.md"
        for path in (entry_path, spec_path, copy_path, examples_path):
            if not path.is_file():
                errors.append(f"{activity['id']} 缺文件：{path.relative_to(root)}")
        if not all(p.is_file() for p in (entry_path, spec_path, copy_path, examples_path)):
            continue

        entry = entry_path.read_text(encoding="utf-8")
        spec = spec_path.read_text(encoding="utf-8")
        copied = copy_path.read_text(encoding="utf-8")
        examples = examples_path.read_text(encoding="utf-8")
        if frontmatter_keys(entry) != {"name", "description"}:
            errors.append(f"{activity['skill']} frontmatter 只允许 name/description")
        if len(entry) > 8192:
            errors.append(f"{activity['skill']}/SKILL.md 超过 8192 字符")
        for token in (*ENTRY_TOKENS, activity["id"], activity["artifact"]):
            if token not in entry:
                errors.append(f"{activity['skill']}/SKILL.md 缺标记：{token}")
        for token in (*SPEC_TOKENS, activity["id"], activity["artifact"]):
            if token not in spec:
                errors.append(f"{activity['skill']}/execution-spec.md 缺标记：{token}")
        if len(re.findall(r"^## 案例 [1-5]：", examples, re.M)) < 5:
            errors.append(f"{activity['skill']}/examples.md 少于 5 个案例")

        source_path = Path(activity["source"])
        if not source_path.is_file():
            errors.append(f"{activity['id']} 原始设计不存在：{source_path}")
        else:
            lines = source_path.read_text(encoding="utf-8").splitlines()
            start = activity["sourceLines"].get("start", 0)
            end = activity["sourceLines"].get("end", 0)
            if not (1 <= start <= end <= len(lines)):
                errors.append(f"{activity['id']} 来源行号非法：{start}-{end}")
            else:
                section = "\n".join(lines[start - 1:end]).rstrip() + "\n"
                digest = sha256_text(section)
                if digest != activity["sourceSha256"]:
                    errors.append(f"{activity['id']} sourceSha256 与原文不符")
                if copied != section:
                    errors.append(f"{activity['id']} source-design.md 不是原文精确镜像")
                if len(copied.splitlines()) < 80:
                    errors.append(f"{activity['id']} 原始 Activity 复制不足 80 行")
                source_digests.add(sha256_text(copied))
        execution_digests.add(sha256_text(spec))

        template = (root / activity["template"]).resolve()
        try:
            template.relative_to(root)
        except ValueError:
            errors.append(f"{activity['id']} 模板路径逃逸：{activity['template']}")
            continue
        if not template.is_file():
            errors.append(f"{activity['id']} 缺正式模板：{activity['template']}")
        else:
            body = template.read_text(encoding="utf-8")
            for token in (activity["id"], activity["artifact"], *TEMPLATE_TOKENS):
                if token not in body:
                    errors.append(f"{activity['id']} 模板缺标记：{token}")
            if body.count("{{") < 12:
                errors.append(f"{activity['id']} 模板可填写变量少于 12 个")
        if f"../{activity['template']}" not in entry or f"../{activity['template']}" not in spec:
            errors.append(f"{activity['id']} Skill 未指向 catalog 正式模板")

    if len(source_digests) != 50:
        errors.append(f"source-design 唯一段落应为 50，实际 {len(source_digests)}")
    if len(execution_digests) != 50:
        errors.append(f"execution-spec 唯一文件应为 50，实际 {len(execution_digests)}")

    for stage, stage_no in STAGE_NUMBERS.items():
        stage_skill = f"spk-ipd-{stage_no}-{stage}"
        path = root / stage_skill / "SKILL.md"
        playbook = root / stage_skill / "references" / "stage-playbook.md"
        if not path.is_file() or not playbook.is_file():
            errors.append(f"缺阶段编排 Skill：{stage_skill}")
            continue
        combined = path.read_text(encoding="utf-8") + playbook.read_text(encoding="utf-8")
        for activity in (a for a in activities if a["stage"] == stage):
            if activity["id"] not in combined or activity["skill"] not in combined:
                errors.append(f"{stage_skill} 未路由 {activity['id']} → {activity['skill']}")

    for rel in REQUIRED_FILES:
        if not (root / rel).is_file():
            errors.append(f"缺必需文件：{rel}")
    for shared in ("spk-ipd-tr-gate", "spk-ipd-verifier"):
        if not (root / shared / "SKILL.md").is_file():
            errors.append(f"缺共享治理 Skill：{shared}")
    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("root", nargs="?", type=Path, default=Path(__file__).resolve().parents[1])
    args = parser.parse_args()
    errors = validate_pack(args.root)
    if errors:
        print("FAIL: commercial-release v2 Skill 包校验失败")
        for error in errors:
            print(f"- {error}")
        return 1
    print("PASS: 6 阶段、50 Activity、原文镜像、执行规约、案例、模板与审批追溯完整")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
