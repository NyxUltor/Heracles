"""Export and autosave helpers for Hercules."""

from __future__ import annotations

from datetime import datetime
from pathlib import Path
from typing import Any
import os
import tempfile


def build_session_filename(volume: float, timestamp: datetime | None = None) -> str:
	"""Build the default export filename for a session log."""
	timestamp = timestamp or datetime.now()
	volume_k = int(volume / 1000)
	date_part = timestamp.strftime("%d-%m-%Y")
	return f"{date_part}-{volume_k}k.json"


def _atomic_write(path: Path, data: str) -> None:
	"""Atomically write `data` to `path` using a temporary file and os.replace."""
	path.parent.mkdir(parents=True, exist_ok=True)
	fd, tmp = tempfile.mkstemp(dir=str(path.parent))
	try:
		with os.fdopen(fd, "w", encoding="utf-8") as handle:
			handle.write(data)
		os.replace(tmp, str(path))
	finally:
		# Ensure tmp removed if something went wrong and it still exists
		try:
			if os.path.exists(tmp):
				os.remove(tmp)
		except Exception:
			pass


def export_session_log(log_dir: Path, session_payload: dict[str, Any], volume: float, timestamp: datetime | None = None) -> Path:
	"""Write a session export file using the default Hercules naming scheme."""
	file_name = build_session_filename(volume=volume, timestamp=timestamp)
	output_path = log_dir / file_name
	data = __import__("json").dumps(session_payload, indent=2, ensure_ascii=False)
	_atomic_write(output_path, data)
	return output_path


def write_autosave(draft_path: Path, payload: dict[str, Any]) -> None:
	"""Persist the current in-progress session so a crash does not lose it."""
	data = __import__("json").dumps(payload, indent=2, ensure_ascii=False)
	_atomic_write(draft_path, data)
