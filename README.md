Heracles (mobile-friendly)

This is a mobile-friendly fork of the Hercules desktop app.

Run locally:

```bash
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
python main.py

Run the web API (provides mobile frontend and persistence):

```bash
cd api
python -m api.app
```

Then open `http://<desktop-ip>:8000/` from your device browser.
```

Notes:
- UI uses a hamburger menu for navigation and larger touch targets.
- Keep in sync with the original `Hercules` project for core logic.