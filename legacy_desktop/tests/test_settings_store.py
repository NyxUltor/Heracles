from __future__ import annotations

import tempfile
from pathlib import Path
import unittest

from data.settings_store import DEFAULT_SETTINGS, load_settings, save_settings


class SettingsStoreTest(unittest.TestCase):
    def test_load_settings_returns_defaults_when_missing(self):
        with tempfile.TemporaryDirectory() as tmpdir:
            settings = load_settings(Path(tmpdir) / "settings.json")
            self.assertEqual(settings["units"], DEFAULT_SETTINGS["units"])
            self.assertEqual(settings["default_exercises"], DEFAULT_SETTINGS["default_exercises"])
            self.assertEqual(settings["custom_fields"]["field1_label"], DEFAULT_SETTINGS["custom_fields"]["field1_label"])
            self.assertEqual(settings["custom_fields"]["field1_placeholder"], DEFAULT_SETTINGS["custom_fields"]["field1_placeholder"])
            self.assertTrue(settings["custom_fields"]["field1_enabled"])
            self.assertFalse(settings["custom_fields"]["field2_enabled"])

    def test_save_and_reload_settings(self):
        with tempfile.TemporaryDirectory() as tmpdir:
            settings_path = Path(tmpdir) / "settings.json"
            custom_fields = dict(DEFAULT_SETTINGS["custom_fields"])
            custom_fields.update({"field1_label": "Session time", "field1_placeholder": "<Duration(minutes)>", "field1_value": "45 min", "field2_enabled": True, "field2_label": "field:1"})
            save_settings(settings_path, {**DEFAULT_SETTINGS, "units": "lb", "custom_fields": custom_fields})
            loaded = load_settings(settings_path)
            self.assertEqual(loaded["units"], "lb")
            self.assertEqual(loaded["custom_fields"]["field1_value"], "45 min")
            self.assertEqual(loaded["custom_fields"]["field1_label"], "Session time")
            self.assertTrue(loaded["custom_fields"]["field2_enabled"])
