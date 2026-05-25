# Heracles Planned Changes

- [x] Change log storage to use an absolute path setting instead of a relative one.
- [x] Keep existing log files when the storage location changes by moving them to the new directory.
- [x] Open a loaded session directly in Logger when the user taps Load from Sessions.
- [x] Rename saved session files to `YYYY-MM-DD-<session-index>-<volume-k>.json`.
- [x] Keep log/session data in a readable JSON format.
- [x] Add numeric input mode options in Settings: keyboard and scrubber, with multi-select allowed.
- [x] Remove app icon customization option from Settings.
- [x] Add Tracker screen with radar graph, volume/bodyweight line chart, and saved bodyweight history.
- [x] Save logger bodyweight into workout logs and the tracker history file.
- [ ] Add time-related logging features to the UI roadmap.
- [ ] Add pre-built routines to the feature roadmap.
- [ ] new UI design

## Recent updates (v0.5)

- [x] Prevent scrubber producing negative values (clamp to >= 0)
- [x] Make scrubber continuous and increase precision (smaller pixel step)
- [x] Add editable scrubber sensitivity field in Settings and widen range
- [x] Allow keyboard entry for bodyweight and duration even when keyboard mode is off
- [x] Prevent simultaneous vertical scroll and scrub (disable parent scroll while scrubbing)
- [x] Auto-scroll Logger to newly added exercise
- [x] Add Session export to external storage (/sdcard/HeraclesSessions) via Settings
- [x] Format total session volume to two decimal places in Logger
- [x] Release v0.5 published with these changes
