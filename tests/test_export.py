from __future__ import annotations

import json
import tempfile
from datetime import datetime
from pathlib import Path
import unittest

from utils.export import build_session_filename, export_session_log, write_autosave


class ExportHelpersTest(unittest.TestCase):
    def test_build_session_filename_uses_day_month_year_and_volume_k(self):
        stamp = datetime(2026, 5, 21, 14, 30, 0)
        self.assertEqual(build_session_filename(2500, stamp), "21-05-2026-2k.json")

    def test_export_and_autosave_write_json(self):
        with tempfile.TemporaryDirectory() as tmpdir:
            root = Path(tmpdir)
            log_dir = root / "logs"
            autosave_path = log_dir / "autosave.json"

            payload = {
                "saved_at": "2026-05-21T14:30:00",
                "volume": 2500,
                "exercises": [{"name": "Push Ups", "sets": [{"weight": "100", "reps": "10"}]}],
            }

            export_path = export_session_log(log_dir, payload, 2500, timestamp=datetime(2026, 5, 21, 14, 30, 0))
            write_autosave(autosave_path, payload)

            self.assertTrue(export_path.exists())
            self.assertTrue(autosave_path.exists())

            with export_path.open("r", encoding="utf-8") as handle:
                exported = json.load(handle)

            with autosave_path.open("r", encoding="utf-8") as handle:
                autosaved = json.load(handle)

            self.assertEqual(exported["volume"], 2500)
            self.assertEqual(autosaved["exercises"][0]["name"], "Push Ups")
