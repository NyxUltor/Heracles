"""Export and autosave helpers for Hercules."""

from __future__ import annotations

from datetime import datetime
from pathlib import Path
from typing import Any


def build_session_filename(volume: float, timestamp: datetime | None = None) -> str:
	"""Build the default export filename for a session log."""
	timestamp = timestamp or datetime.now()
	volume_k = int(volume / 1000)
	date_part = timestamp.strftime("%d-%m-%Y")
	return f"{date_part}-{volume_k}k.json"


def export_session_log(log_dir: Path, session_payload: dict[str, Any], volume: float, timestamp: datetime | None = None) -> Path:
	"""Write a session export file using the default Hercules naming scheme."""
	log_dir.mkdir(parents=True, exist_ok=True)
	file_name = build_session_filename(volume=volume, timestamp=timestamp)
	output_path = log_dir / file_name
	output_path.write_text(
		__import__("json").dumps(session_payload, indent=2, ensure_ascii=False),
		encoding="utf-8",
	)
	return output_path


def write_autosave(draft_path: Path, payload: dict[str, Any]) -> None:
	"""Persist the current in-progress session so a crash does not lose it."""
	draft_path.parent.mkdir(parents=True, exist_ok=True)
	draft_path.write_text(
		__import__("json").dumps(payload, indent=2, ensure_ascii=False),
		encoding="utf-8",
	)
