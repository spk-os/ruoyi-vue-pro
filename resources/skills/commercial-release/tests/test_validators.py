#!/usr/bin/env python3
"""商业发布 IPD Skill 包的契约测试。"""

from __future__ import annotations

import hashlib
import importlib.util
import json
import re
import tempfile
import unittest
from pathlib import Path


PACK_ROOT = Path(__file__).resolve().parents[1]
REPO_ROOT = PACK_ROOT.parents[2]


def load_module(name: str, path: Path):
    spec = importlib.util.spec_from_file_location(name, path)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)
    return module


class SkillPackTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.pack_validator = load_module(
            "validate_skill_pack", PACK_ROOT / "scripts" / "validate_skill_pack.py"
        )
        cls.delivery_validator = load_module(
            "validate_delivery", PACK_ROOT / "scripts" / "validate_delivery.py"
        )

    def test_catalog_is_complete_and_traceable(self):
        catalog = json.loads((PACK_ROOT / "references" / "activity-catalog.json").read_text())
        activities = catalog["activities"]
        self.assertEqual(50, len(activities))
        self.assertEqual(
            {"concept": 8, "plan": 6, "develop": 11, "verify": 9, "launch": 6, "lifecycle": 10},
            {stage: sum(a["stage"] == stage for a in activities) for stage in catalog["stageCounts"]},
        )
        self.assertEqual([], self.pack_validator.validate_pack(PACK_ROOT))

    def test_activity_skills_use_stage_and_activity_numbers(self):
        catalog = json.loads((PACK_ROOT / "references" / "activity-catalog.json").read_text())
        expected_stage_numbers = {
            "concept": "01", "plan": "02", "develop": "03",
            "verify": "04", "launch": "05", "lifecycle": "06",
        }
        for activity in catalog["activities"]:
            self.assertEqual(expected_stage_numbers[activity["stage"]], activity["stageNo"])
            self.assertRegex(activity["activityNo"], r"^\d{2}$")
            self.assertRegex(
                activity["skill"],
                rf"^spk-ipd-{activity['stageNo']}-{activity['stage']}-{activity['activityNo']}-[a-z0-9-]+$",
            )
            self.assertLessEqual(len(activity["skill"]), 64)

    def test_each_activity_is_an_executable_skill_not_a_clone_shell(self):
        catalog = json.loads((PACK_ROOT / "references" / "activity-catalog.json").read_text())
        source_hashes = set()
        execution_hashes = set()
        for activity in catalog["activities"]:
            skill_root = PACK_ROOT / activity["skill"]
            entry = (skill_root / "SKILL.md").read_text(encoding="utf-8")
            spec = (skill_root / "references" / "execution-spec.md").read_text(encoding="utf-8")
            source = (skill_root / "references" / "source-design.md").read_text(encoding="utf-8")
            cases = (skill_root / "references" / "examples.md").read_text(encoding="utf-8")

            for token in ("读取顺序", "输入就绪", "执行工作流", "完成定义", "失败处理", "移交"):
                self.assertIn(token, entry, f"{activity['skill']} 缺 {token}")
            for token in (
                "目标与非目标", "输入契约", "工具与执行顺序", "领域执行算法",
                "输出契约", "验收与完成判定", "失败、重试、回滚与升级", "上下游移交",
            ):
                self.assertIn(token, spec, f"{activity['skill']} 缺 {token}")
            self.assertIn(activity["id"], source)
            self.assertIn(activity["name"], source)
            self.assertGreaterEqual(len(source.splitlines()), 80, activity["skill"])
            self.assertGreaterEqual(len(re.findall(r"^## 案例 [1-5]：", cases, re.M)), 5, activity["skill"])
            self.assertNotRegex(entry + spec + cases, r"\b(?:TODO|TBD|FIXME)\b")

            source_hashes.add(hashlib.sha256(source.encode()).hexdigest())
            execution_hashes.add(hashlib.sha256(spec.encode()).hexdigest())
        self.assertEqual(50, len(source_hashes), "source-design 发生克隆")
        self.assertEqual(50, len(execution_hashes), "execution-spec 发生克隆")

    def test_catalog_source_anchors_are_verifiable(self):
        catalog = json.loads((PACK_ROOT / "references" / "activity-catalog.json").read_text())
        for activity in catalog["activities"]:
            source_path = Path(activity["source"])
            lines = source_path.read_text(encoding="utf-8").splitlines()
            start, end = activity["sourceLines"]["start"], activity["sourceLines"]["end"]
            section = "\n".join(lines[start - 1:end]).rstrip() + "\n"
            self.assertTrue(section.startswith(f"## {activity['id']} "))
            self.assertEqual(activity["sourceSha256"], hashlib.sha256(section.encode()).hexdigest())
            copied = (PACK_ROOT / activity["skill"] / "references" / "source-design.md").read_text(encoding="utf-8")
            self.assertEqual(section, copied)

    def test_skill_entrypoints_fit_runtime_injection_limit(self):
        catalog = json.loads((PACK_ROOT / "references" / "activity-catalog.json").read_text())
        for activity in catalog["activities"]:
            path = PACK_ROOT / activity["skill"] / "SKILL.md"
            self.assertLessEqual(len(path.read_text(encoding="utf-8")), 8192, str(path))
            body = path.read_text(encoding="utf-8")
            self.assertIn("完成定义", body, str(path))
            self.assertIn("禁止", body, str(path))
            self.assertIn("证据", body, str(path))

    def test_runtime_routes_commercial_release_to_numbered_stage_orchestrators(self):
        router = (
            REPO_ROOT
            / "yudao-module-spk-delivery/src/main/java/cn/iocoder/yudao/module/spkdelivery/"
              "service/router/SpkTaskRouterService.java"
        ).read_text(encoding="utf-8")
        expected = {
            "concept": "spk-ipd-01-concept",
            "plan": "spk-ipd-02-plan",
            "develop": "spk-ipd-03-develop",
            "qualify": "spk-ipd-04-verify",
            "launch": "spk-ipd-05-launch",
            "lifecycle": "spk-ipd-06-lifecycle",
        }
        self.assertIn("resolveSkillNameForEnv(skillName, skillEnv, def.getStage())", router)
        self.assertIn('"commercial-release".equals(normEnv)', router)
        for stage, skill in expected.items():
            self.assertIn(f'"{stage}", "{skill}"', router)

    def test_every_activity_has_a_fillable_document_template(self):
        catalog = json.loads((PACK_ROOT / "references" / "activity-catalog.json").read_text())
        for activity in catalog["activities"]:
            self.assertIn("template", activity, activity["id"])
            path = PACK_ROOT / activity["template"]
            self.assertTrue(path.is_file(), f"{activity['id']} 缺模板：{path}")
            body = path.read_text(encoding="utf-8")
            self.assertIn(activity["id"], body)
            self.assertIn(activity["artifact"], body)
            self.assertGreaterEqual(body.count("{{"), 12, str(path))
            for section in ("输入与基线", "业务内容", "追溯矩阵", "证据清单", "验收执行记录"):
                self.assertIn(section, body, f"{path} 缺章节：{section}")

    def test_unrendered_template_variables_fail_delivery_validation(self):
        with tempfile.TemporaryDirectory() as temp:
            result = self.make_delivery(Path(temp))
            data = json.loads(result.read_text())
            data["claims"][0]["text"] = "{{required:填写真实结论}}"
            result.write_text(json.dumps(data, ensure_ascii=False))
            errors = self.delivery_validator.validate_delivery(result, PACK_ROOT)
            self.assertTrue(any("模板变量" in e for e in errors), errors)

    def make_delivery(self, root: Path, *, activity_id: str = "ACT-03-02-01") -> Path:
        catalog = json.loads((PACK_ROOT / "references" / "activity-catalog.json").read_text())
        activity = next(a for a in catalog["activities"] if a["id"] == activity_id)
        artifact = root / "artifacts" / "OpportunitySignalSet.json"
        evidence = root / "evidence" / "source.json"
        artifact.parent.mkdir(parents=True)
        evidence.parent.mkdir(parents=True)
        artifact.write_text('{"signals":[{"source":"crm://case/1","observedAt":"2026-08-18T08:00:00Z"}]}')
        evidence.write_text('{"source":"crm://case/1","retrievedAt":"2026-08-18T08:01:00Z"}')
        digest = lambda path: hashlib.sha256(path.read_bytes()).hexdigest()
        result = {
            "schemaVersion": "1.0",
            "activityId": activity_id,
            "stage": activity["stage"],
            "status": "COMPLETE",
            "producer": {"actorId": "agent-market-1", "role": "MarketAnalysisAgent"},
            "skill": {"name": activity["skill"], "version": "2.0.0"},
            "execution": {
                "runId": "run-001",
                "startedAt": "2026-08-18T08:00:00Z",
                "finishedAt": "2026-08-18T08:02:00Z",
                "model": "model-with-version",
                "tools": [{"name": "crm-read", "version": "1.2", "exitCode": 0}],
            },
            "artifacts": [{
                "artifactId": "artifact-1", "type": activity["artifact"],
                "path": "artifacts/OpportunitySignalSet.json", "sha256": digest(artifact),
            }],
            "evidence": [{
                "evidenceId": "evidence-1", "type": "SourceSnapshot",
                "path": "evidence/source.json", "sha256": digest(evidence),
            }],
            "claims": [{"claimId": "claim-1", "text": "存在可复核客户信号", "evidenceIds": ["evidence-1"]}],
            "traceability": [{"sourceId": "crm://case/1", "targetId": "artifact-1", "relation": "supports"}],
            "verifier": {"required": False, "actorId": "", "verdict": "NOT_REQUIRED", "receiptPath": ""},
            "humanApproval": {"required": False, "status": "NOT_REQUIRED", "recordPath": ""},
        }
        if activity["independentVerifier"]:
            receipt = root / "receipts" / "verification.json"
            receipt.parent.mkdir(parents=True, exist_ok=True)
            receipt.write_text(json.dumps({
                "schemaVersion": "1.0", "activityId": activity_id, "runId": "run-001",
                "producerActorId": "agent-market-1", "verifierActorId": "verifier-1",
                "verdict": "PASS", "checks": [{"id": "check-1", "status": "PASS"}],
                "verifiedAt": "2026-08-18T08:03:00Z", "signature": "sig-verifier-1",
            }, ensure_ascii=False))
            result["verifier"] = {
                "required": True, "actorId": "verifier-1", "verdict": "PASS",
                "receiptPath": "receipts/verification.json",
            }
        if activity["humanGate"]:
            approval = root / "receipts" / "human-approval.json"
            approval.parent.mkdir(parents=True, exist_ok=True)
            approval.write_text(json.dumps({
                "schemaVersion": "1.0", "decisionId": "decision-001", "activityId": activity_id,
                "authorizedActor": {"actorId": "owner-1", "role": activity["humanResponsibility"]},
                "decision": "APPROVED", "scope": "activity baseline",
                "signedAt": "2026-08-18T08:04:00Z", "signature": "sig-owner-1",
            }, ensure_ascii=False))
            result["humanApproval"] = {
                "required": True, "status": "APPROVED",
                "recordPath": "receipts/human-approval.json",
            }
        path = root / "activity-result.json"
        path.write_text(json.dumps(result, ensure_ascii=False, indent=2))
        return path

    def test_valid_delivery_passes(self):
        with tempfile.TemporaryDirectory() as temp:
            result = self.make_delivery(Path(temp))
            self.assertEqual([], self.delivery_validator.validate_delivery(result, PACK_ROOT))

    def test_placeholder_and_hash_tamper_fail(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            result = self.make_delivery(root)
            data = json.loads(result.read_text())
            data["claims"][0]["text"] = "TBD"
            result.write_text(json.dumps(data, ensure_ascii=False))
            (root / "artifacts" / "OpportunitySignalSet.json").write_text("tampered")
            errors = self.delivery_validator.validate_delivery(result, PACK_ROOT)
            self.assertTrue(any("占位符" in e for e in errors), errors)
            self.assertTrue(any("sha256" in e for e in errors), errors)

    def test_human_gate_and_independent_verifier_cannot_be_faked(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            result = self.make_delivery(root, activity_id="ACT-03-02-08")
            data = json.loads(result.read_text())
            data["skill"]["name"] = "spk-ipd-01-concept-08-tr1-review-support"
            data["artifacts"][0]["type"] = "TR1DecisionRecord"
            data["producer"]["actorId"] = "agent-reviewer"
            data["verifier"] = {
                "required": True, "actorId": "agent-reviewer", "verdict": "PASS", "receiptPath": ""
            }
            data["humanApproval"] = {"required": True, "status": "APPROVED", "recordPath": ""}
            result.write_text(json.dumps(data, ensure_ascii=False))
            errors = self.delivery_validator.validate_delivery(result, PACK_ROOT)
            self.assertTrue(any("独立验证" in e for e in errors), errors)
            self.assertTrue(any("人工审批" in e for e in errors), errors)


if __name__ == "__main__":
    unittest.main()
