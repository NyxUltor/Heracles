from __future__ import annotations

import json
from pathlib import Path
from typing import Any

from fastapi import FastAPI, HTTPException, Request
from fastapi.responses import FileResponse, JSONResponse
from fastapi.staticfiles import StaticFiles

from data.session_store import load_sessions
from data.settings_store import load_settings
from utils.export import export_session_log, write_autosave


BASE_DIR = Path(__file__).resolve().parent.parent
WEB_DIR = BASE_DIR / "web"

app = FastAPI(title="Heracles API")
app.mount("/web", StaticFiles(directory=WEB_DIR), name="web")


@app.get("/api/exercises")
def get_exercises():
    exercises_path = BASE_DIR / "data" / "exercises.json"
    if not exercises_path.exists():
        return []
    try:
        return json.loads(exercises_path.read_text(encoding="utf-8"))
    except Exception:
        raise HTTPException(status_code=500, detail="failed to read exercises")


@app.get("/api/sessions")
def list_sessions():
    settings = load_settings(BASE_DIR / "data" / "config.json")
    logs_dir = (BASE_DIR / settings.get("export_dir", "logs"))
    sessions = load_sessions(logs_dir)
    return sessions


@app.post("/api/sessions")
async def save_session(request: Request):
    payload = await request.json()
    settings = load_settings(BASE_DIR / "data" / "config.json")
    logs_dir = (BASE_DIR / settings.get("export_dir", "logs"))

    # compute volume fallback
    volume = float(payload.get("volume", 0) or 0)

    try:
        path = export_session_log(logs_dir, payload, volume)
        return {"ok": True, "filename": path.name}
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc))


@app.post("/api/draft")
async def save_draft(request: Request):
    payload = await request.json()
    settings = load_settings(BASE_DIR / "data" / "config.json")
    logs_dir = (BASE_DIR / settings.get("export_dir", "logs"))
    draft = logs_dir / "autosave.json"
    try:
        write_autosave(draft, payload)
        return {"ok": True}
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc))


@app.get("/")
def index():
    index_path = WEB_DIR / "index.html"
    return FileResponse(index_path)


if __name__ == "__main__":
    import uvicorn

    uvicorn.run("api.app:app", host="0.0.0.0", port=8000, reload=True)
