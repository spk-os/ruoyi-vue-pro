#!/usr/bin/env python3
"""Producer 主产物预检器回归测试。"""

from __future__ import annotations

import importlib.util
import json
import tempfile
import unittest
from pathlib import Path


PACK_ROOT = Path(__file__).resolve().parents[1]


def load_validator():
    path = PACK_ROOT / "scripts" / "validate_artifact.py"
    spec = importlib.util.spec_from_file_location("validate_artifact", path)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)
    return module


class ArtifactValidatorTests(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.validator = load_validator()

    def test_schema_and_fail_acceptance_are_checked_against_document(self):
        with tempfile.TemporaryDirectory() as raw:
            root = Path(raw)
            contract = root / "demo"
            contract.mkdir()
            (contract / "schema.json").write_text(json.dumps({
                "type": "object",
                "required": ["plan_id", "wbs"],
                "properties": {
                    "plan_id": {"type": "string", "minLength": 1},
                    "wbs": {"type": "array", "minItems": 1},
                },
            }), encoding="utf-8")
            (contract / "acceptance.json").write_text(json.dumps({
                "rules": [{
                    "id": "owner",
                    "severity": "fail",
                    "assert": {"op": "every", "path": "wbs", "item": {
                        "op": "nonEmpty", "path": "owner"
                    }},
                }],
            }), encoding="utf-8")
            artifact = root / "artifact.json"
            artifact.write_text(json.dumps({
                "document": {"plan_id": "P-1", "wbs": [{"owner": ""}]},
                "summary": "plan",
                "conclusion": "PASS",
            }), encoding="utf-8")

            errors = self.validator.validate_artifact(artifact, root, "demo")

            self.assertTrue(any("验收规则 owner [fail]" in error for error in errors))

    def test_valid_envelope_passes(self):
        with tempfile.TemporaryDirectory() as raw:
            root = Path(raw)
            contract = root / "demo"
            contract.mkdir()
            (contract / "schema.json").write_text(
                '{"type":"object","required":["id"],"properties":{"id":{"type":"string","minLength":1}}}',
                encoding="utf-8",
            )
            (contract / "acceptance.json").write_text('{"rules":[]}', encoding="utf-8")
            artifact = root / "artifact.json"
            artifact.write_text(
                '{"document":{"id":"ok"},"summary":"ok","conclusion":"PASS"}',
                encoding="utf-8",
            )

            self.assertEqual([], self.validator.validate_artifact(artifact, root, "demo"))


if __name__ == "__main__":
    unittest.main()
