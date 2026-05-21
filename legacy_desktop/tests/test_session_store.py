from __future__ import annotations

import json
import tempfile
from datetime import datetime
from pathlib import Path
import unittest

from data.session_store import load_sessions
from utils.export import export_session_log


class SessionStoreTest(unittest.TestCase):
    def test_load_sessions_skips_autosave_and_returns_latest_first(self):
        with tempfile.TemporaryDirectory() as tmpdir:
            log_dir = Path(tmpdir)

            first = {
                "saved_at": "2026-05-20T10:00:00",
                "volume": 1000,
                "exercises": [{"name": "Pull Ups", "sets": []}],
            }
            second = {
                "saved_at": "2026-05-21T10:00:00",
                "volume": 2000,
                "exercises": [{"name": "Squats", "sets": []}],
            }

            export_session_log(log_dir, first, 1000, timestamp=datetime(2026, 5, 20, 10, 0, 0))
            export_session_log(log_dir, second, 2000, timestamp=datetime(2026, 5, 21, 10, 0, 0))
            (log_dir / "autosave.json").write_text(json.dumps({"saved_at": "draft", "volume": 0, "exercises": []}), encoding="utf-8")

            sessions = load_sessions(log_dir)

            self.assertEqual([session["volume"] for session in sessions], [2000, 1000])
            self.assertEqual(sessions[0]["filename"], "21-05-2026-2k.json")
            self.assertEqual(sessions[1]["exercises"][0]["name"], "Pull Ups")
