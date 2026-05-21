"""Session history helpers for Hercules."""

from __future__ import annotations

import json
import re
from datetime import datetime
from pathlib import Path
from typing import Any


_SESSION_PATTERN = re.compile(r"^\d{2}-\d{2}-\d{4}-\d+k\.json$")


def _session_sort_key(path: Path) -> tuple[float, str]:
	try:
		return (path.stat().st_mtime, path.name)
	except OSError:
		return (0.0, path.name)


def load_sessions(log_dir: Path) -> list[dict[str, Any]]:
	"""Load saved sessions from the export directory."""
	if not log_dir.exists():
		return []

	sessions: list[dict[str, Any]] = []
	for path in sorted(log_dir.glob("*.json"), key=_session_sort_key, reverse=True):
		if path.name == "autosave.json":
			continue
		if not _SESSION_PATTERN.match(path.name):
			continue

		try:
			with path.open("r", encoding="utf-8") as handle:
				payload = json.load(handle)
		except (OSError, json.JSONDecodeError):
			continue

		saved_at = payload.get("saved_at")
		if not saved_at:
			try:
				saved_at = datetime.fromtimestamp(path.stat().st_mtime).isoformat(timespec="seconds")
			except OSError:
				saved_at = "unknown"

		sessions.append(
			{
				"filename": path.name,
				"saved_at": saved_at,
				"volume": int(payload.get("volume", 0)),
				"workout_duration": payload.get("workout_duration"),
				"exercises": payload.get("exercises", []),
			}
		)

	return sessions
