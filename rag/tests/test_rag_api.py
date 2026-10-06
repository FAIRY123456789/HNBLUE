from __future__ import annotations

import os
import sys
import tempfile
import unittest
from pathlib import Path

from flask import Flask


ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "rag" / "examples" / "knowledge"
MODEL_APP_DIR = ROOT / "flask_model" / "carbon_model_api"
for entry in (ROOT, MODEL_APP_DIR):
    if str(entry) not in sys.path:
        sys.path.insert(0, str(entry))

from rag_api import create_rag_blueprint
from rag.hnblue_rag import write_artifacts


class RagApiTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.artifact_dir = tempfile.TemporaryDirectory()
        write_artifacts(SOURCE, Path(cls.artifact_dir.name))
        os.environ["HNBLUE_RAG_CHUNKS"] = str(Path(cls.artifact_dir.name) / "chunks.jsonl")
        os.environ["HNBLUE_RAG_SOURCE"] = str(SOURCE)
        application = Flask(__name__)
        application.register_blueprint(create_rag_blueprint(ROOT))
        cls.client = application.test_client()

    @classmethod
    def tearDownClass(cls):
        cls.artifact_dir.cleanup()

    def test_status_reports_loaded_portable_index(self):
        response = self.client.get("/api/rag/status")
        self.assertEqual(200, response.status_code)
        payload = response.get_json()
        self.assertTrue(payload["ok"])
        manifest = __import__("json").loads((Path(self.artifact_dir.name) / "index_manifest.json").read_text(encoding="utf-8"))
        self.assertEqual(manifest["chunks"], payload["chunks"])
        self.assertEqual(manifest["source_files"], payload["sourceFiles"])
        self.assertEqual(64, len(payload["indexSha256"]))

    def test_answer_returns_sources(self):
        response = self.client.post("/api/rag/answer", json={"message": "CatBoost 在红树林碳储量评估中有什么作用？"})
        self.assertEqual(200, response.status_code)
        payload = response.get_json()
        self.assertTrue(payload["ok"])
        self.assertEqual("retrieved", payload["evidenceStatus"])
        self.assertGreaterEqual(len(payload["sources"]), 1)

    def test_empty_question_is_rejected(self):
        response = self.client.post("/api/rag/answer", json={"message": " "})
        self.assertEqual(400, response.status_code)

    def test_unrelated_question_is_not_hallucinated(self):
        response = self.client.post("/api/rag/answer", json={"message": "请解释火星探测器轨道参数与量子纠缠实验"})
        self.assertEqual(200, response.status_code)
        payload = response.get_json()
        self.assertEqual("insufficient", payload["evidenceStatus"])
        self.assertEqual([], payload["sources"])


if __name__ == "__main__":
    unittest.main()
