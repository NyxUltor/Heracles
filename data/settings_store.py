"""Persistent app settings for Hercules."""

from __future__ import annotations

import json
from pathlib import Path
from typing import Any


DEFAULT_SETTINGS: dict[str, Any] = {
	"export_dir": "logs",
	"retention_days": None,
	"units": "kg",
	"background_image": "",
	"restore_mode": "ask",
	"autosave_enabled": True,
	"default_exercises": ["Push Ups", "Pull Ups"],
	"show_row_totals": True,
	# Customizable auxiliary fields shown on the logger footer (off by default)
	"custom_fields": {
		"field1_enabled": True,
		"field1_label": "workout duration(main)",
		"field1_placeholder": "Duration(minutes)",
		"field1_value": None,
		"field2_enabled": False,
		"field2_label": "field:1",
		"field2_placeholder": "field:1",
		"field2_value": None,
	},
}


def _normalize_custom_fields(settings: dict[str, Any], source: dict[str, Any] | None) -> None:
	original_custom_fields = source.get("custom_fields") if isinstance(source, dict) else None
	custom_fields = original_custom_fields if isinstance(original_custom_fields, dict) else {}

	defaults = DEFAULT_SETTINGS["custom_fields"]
	merged = dict(defaults)
	merged.update(custom_fields)

	# Backward compatibility with the previous single-field keys.
	legacy_label = source.get("workout_duration_label") if isinstance(source, dict) else None
	legacy_value = source.get("workout_duration") if isinstance(source, dict) else None
	if legacy_label is not None and (not isinstance(original_custom_fields, dict) or "field1_label" not in original_custom_fields):
		merged["field1_label"] = legacy_label or merged["field1_label"]
	if legacy_value is not None and (not isinstance(original_custom_fields, dict) or "field1_value" not in original_custom_fields):
		merged["field1_value"] = legacy_value

	settings["custom_fields"] = merged
	settings.pop("workout_duration_label", None)
	settings.pop("workout_duration", None)


def load_settings(settings_path: Path) -> dict[str, Any]:
	"""Load settings, falling back to defaults when missing or invalid."""
	if not settings_path.exists():
		return dict(DEFAULT_SETTINGS)

	try:
		with settings_path.open("r", encoding="utf-8") as handle:
			data = json.load(handle)
	except (OSError, json.JSONDecodeError):
		return dict(DEFAULT_SETTINGS)

	settings = dict(DEFAULT_SETTINGS)
	settings.update(data if isinstance(data, dict) else {})
	_normalize_custom_fields(settings, data if isinstance(data, dict) else None)
	return settings


def save_settings(settings_path: Path, settings: dict[str, Any]) -> None:
	"""Write settings to disk as JSON."""
	settings_path.parent.mkdir(parents=True, exist_ok=True)
	with settings_path.open("w", encoding="utf-8") as handle:
		json.dump(settings, handle, indent=2, ensure_ascii=False)
