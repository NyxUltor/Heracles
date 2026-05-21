import json
import re
from pathlib import Path
from datetime import datetime

from PyQt6.QtCore import Qt
from PyQt6.QtWidgets import QWidget, QVBoxLayout, QHBoxLayout, QLabel, QLineEdit, QScrollArea, QFrame, QPushButton, QListWidget, QListWidgetItem, QMessageBox, QTabWidget, QGroupBox, QCheckBox, QComboBox, QSpinBox, QFileDialog, QFormLayout, QSizePolicy, QGraphicsDropShadowEffect, QMenu
from PyQt6.QtGui import QColor, QKeySequence, QShortcut, QFont
from ui.exercise_row import ExerciseRow
from utils.export import export_session_log, write_autosave
from utils.fuzzy import fuzzy_search
from data.session_store import load_sessions
from data.settings_store import DEFAULT_SETTINGS, load_settings, save_settings


SESSION_EXPORT_PATTERN = re.compile(r"^\d{2}-\d{2}-\d{4}-\d+k\.json$")


class SearchLineEdit(QLineEdit):
    def __init__(self, app):
        super().__init__()
        self.app = app

    def keyPressEvent(self, event):
        if event.key() == Qt.Key.Key_Down:
            if self.app.move_suggestion_selection(1):
                return
        elif event.key() == Qt.Key.Key_Up:
            if self.app.move_suggestion_selection(-1):
                return
        elif event.key() in (Qt.Key.Key_Return, Qt.Key.Key_Enter):
            if self.app.accept_selected_suggestion():
                return

        super().keyPressEvent(event)


class App(QWidget):
    def __init__(self):
        super().__init__()

        self.setWindowTitle("Hercules")
        self.setMinimumWidth(900)
        self.app_root = Path(__file__).resolve().parent.parent
        self.config_file = Path(__file__).resolve().parent.parent / "data" / "config.json"
        self.exercises_file = Path(__file__).resolve().parent.parent / "data" / "exercises.json"
        self.settings = load_settings(self.config_file)
        self.apply_settings(self.settings, persist=False)
        self.all_exercises = self.load_exercises()
        self.ensure_default_exercises()
        self.default_session_exercises = list(self.settings.get("default_exercises", DEFAULT_SETTINGS["default_exercises"]))

        # Main layout and title
        main_layout = QVBoxLayout(self)
        main_layout.setSpacing(10)
        main_layout.setContentsMargins(16, 16, 16, 16)

        title = QLabel("HERCULES")
        title.setObjectName("title")
        main_layout.addWidget(title)

        # Tabs (hidden) and hamburger menu for mobile-friendly navigation
        self.tabs = QTabWidget()
        # hide the default tab bar; we'll present a hamburger menu instead
        try:
            self.tabs.tabBar().hide()
        except Exception:
            pass

        header_row = QHBoxLayout()
        self.hamburger = QPushButton("\u2630")
        self.hamburger.setFixedSize(36, 36)
        self.hamburger.setObjectName("hamburgerButton")
        header_row.addWidget(self.hamburger)
        header_row.addStretch()
        main_layout.addLayout(header_row)
        main_layout.addWidget(self.tabs, 1)

        # Build a menu for navigation items
        self.nav_menu = QMenu()
        self.nav_menu.addAction("Logger", lambda: self.tabs.setCurrentIndex(0))
        self.nav_menu.addAction("Tracker", lambda: self.tabs.setCurrentIndex(1))
        self.nav_menu.addAction("Settings", lambda: self.tabs.setCurrentIndex(2))
        self.hamburger.clicked.connect(lambda: self.nav_menu.exec_(self.hamburger.mapToGlobal(self.hamburger.rect().bottomLeft())))

        # Logger tab container - reuse `self.layout` name for minimal changes
        logger_widget = QWidget()
        self.layout = QVBoxLayout(logger_widget)
        self.layout.setSpacing(10)
        self.layout.setContentsMargins(0, 0, 0, 0)

        # Add exercise section
        self.add_ex_container = self.build_add_exercise_section()
        self.layout.addWidget(self.add_ex_container)

        self.logger_actions = QHBoxLayout()
        self.logger_actions.setSpacing(8)
        self.save_button = QPushButton("Save Session")
        self.save_button.setObjectName("saveSessionButton")
        self.save_button.clicked.connect(self.export_current_session)
        self.logger_actions.addWidget(self.save_button)
        self.logger_actions.addStretch()
        self.layout.addLayout(self.logger_actions)

        self.scroll_area = QScrollArea()
        self.scroll_area.setWidgetResizable(True)
        self.scroll_area.setHorizontalScrollBarPolicy(Qt.ScrollBarPolicy.ScrollBarAlwaysOff)
        self.scroll_area.setFrameShape(QFrame.Shape.NoFrame)

        self.scroll_content = QWidget()
        self.scroll_content.setSizePolicy(QSizePolicy.Policy.Expanding, QSizePolicy.Policy.Minimum)
        self.scroll_layout = QVBoxLayout(self.scroll_content)
        self.scroll_layout.setSpacing(10)
        self.scroll_layout.setContentsMargins(0, 0, 0, 0)
        self.scroll_layout.setAlignment(Qt.AlignmentFlag.AlignTop)

        self.scroll_area.setWidget(self.scroll_content)
        self.layout.addWidget(self.scroll_area, 1)

        self.session_total = QLabel("SESSION TOTAL: 0")
        self.session_total.setObjectName("sessionTotal")
        self.session_total.setAlignment(Qt.AlignmentFlag.AlignLeft)
        # Slightly larger font and blue glow effect for prominence
        font = QFont()
        font.setPointSize(22)
        font.setBold(True)
        self.session_total.setFont(font)
        glow = QGraphicsDropShadowEffect(self)
        glow.setBlurRadius(18)
        glow.setColor(QColor("#00ffcc"))
        glow.setOffset(0, 0)
        self.session_total.setGraphicsEffect(glow)
        session_footer = QHBoxLayout()
        session_footer.addWidget(self.session_total)
        session_footer.addStretch()

        # Optional customizable bottom-right field (hidden by default)
        self.custom_field_panel = QFrame()
        self.custom_field_panel.setObjectName("customFieldPanel")
        cf_layout = QVBoxLayout(self.custom_field_panel)
        cf_layout.setContentsMargins(0, 0, 0, 0)
        cf_layout.setSpacing(2)
        self.custom_field_label = QLabel(self.settings.get("custom_fields", {}).get("field2_label", "field:1"))
        self.custom_field_label.setObjectName("customFieldLabel")
        cf_layout.addWidget(self.custom_field_label)
        self.custom_field_input = QLineEdit()
        self.custom_field_input.setObjectName("customFieldInput")
        self.custom_field_input.setMaximumWidth(200)
        self.custom_field_input.setPlaceholderText(self.settings.get("custom_fields", {}).get("field2_placeholder", DEFAULT_SETTINGS["custom_fields"]["field2_placeholder"]))
        self.custom_field_input.setText(str(self.settings.get("custom_fields", {}).get("field2_value", "") or ""))
        self.custom_field_input.editingFinished.connect(lambda: self.on_custom_field_changed())
        cf_layout.addWidget(self.custom_field_input)
        self.custom_field_panel.setVisible(bool(self.settings.get("custom_fields", {}).get("field2_enabled", False)))
        session_footer.addWidget(self.custom_field_panel)
        self.layout.addLayout(session_footer)

        # Populate rows without triggering autosave
        self._loading_rows = True
        self.rows = []
        for name in self.default_session_exercises:
            self.add_exercise(name)
        self._loading_rows = False

        # Add the logger tab and a tracker skeleton
        self.tabs.addTab(logger_widget, "Logger")
        tracker_widget = QWidget()
        tracker_layout = QVBoxLayout(tracker_widget)
        tracker_layout.setSpacing(10)
        tracker_layout.setContentsMargins(0, 0, 0, 0)

        tracker_header = QGroupBox("Session History")
        tracker_header_layout = QVBoxLayout(tracker_header)
        tracker_header_layout.setContentsMargins(12, 12, 12, 12)
        tracker_header_layout.addWidget(QLabel("Select a saved session to load it back into Logger."))

        self.tracker_list = QListWidget()
        self.tracker_list.currentItemChanged.connect(self.on_tracker_selection_changed)

        tracker_buttons = QHBoxLayout()
        self.tracker_refresh_button = QPushButton("Refresh")
        self.tracker_refresh_button.setObjectName("trackerRefreshButton")
        self.tracker_refresh_button.clicked.connect(self.refresh_tracker_sessions)
        self.tracker_load_button = QPushButton("Load")
        self.tracker_load_button.setObjectName("trackerLoadButton")
        self.tracker_load_button.clicked.connect(self.load_selected_session)
        self.tracker_compare_button = QPushButton("Compare")
        self.tracker_compare_button.setObjectName("trackerCompareButton")
        self.tracker_compare_button.clicked.connect(self.compare_selected_session)
        self.tracker_export_button = QPushButton("Export")
        self.tracker_export_button.setObjectName("trackerExportButton")
        self.tracker_export_button.clicked.connect(self.export_selected_session)

        tracker_buttons.addWidget(self.tracker_refresh_button)
        tracker_buttons.addWidget(self.tracker_load_button)
        tracker_buttons.addWidget(self.tracker_compare_button)
        tracker_buttons.addWidget(self.tracker_export_button)
        tracker_buttons.addStretch()

        self.tracker_details = QLabel("No session selected.")
        self.tracker_details.setWordWrap(True)

        tracker_layout.addWidget(tracker_header)
        tracker_layout.addWidget(self.tracker_list, 1)
        tracker_layout.addLayout(tracker_buttons)
        tracker_layout.addWidget(self.tracker_details)
        self.tabs.addTab(tracker_widget, "Tracker")

        settings_widget = self.build_settings_tab()
        self.tabs.addTab(settings_widget, "Settings")

        # Shortcuts for tab switching
        QShortcut(QKeySequence("Ctrl+1"), self).activated.connect(lambda: self.tabs.setCurrentIndex(0))
        QShortcut(QKeySequence("Ctrl+2"), self).activated.connect(lambda: self.tabs.setCurrentIndex(1))
        QShortcut(QKeySequence("Ctrl+3"), self).activated.connect(lambda: self.tabs.setCurrentIndex(2))

        self.setStyleSheet(self.get_styles(self.background_image))
        self.update_session_total()

        self.handle_startup_restore()

        self.refresh_tracker_sessions()

    def build_add_exercise_section(self):
        """Build the add-exercise UI with fuzzy search autocomplete."""
        container = QFrame()
        container.setObjectName("addExerciseContainer")
        layout = QHBoxLayout(container)
        layout.setSpacing(6)
        layout.setContentsMargins(0, 0, 0, 0)

        label = QLabel("Add Exercise:")
        label.setObjectName("addExerciseLabel")
        layout.addWidget(label)

        self.exercise_input = SearchLineEdit(self)
        self.exercise_input.setObjectName("exerciseInput")
        self.exercise_input.setPlaceholderText("Type exercise name...")
        self.exercise_input.setFixedHeight(32)
        self.exercise_input.setMaximumWidth(300)
        self.exercise_input.textChanged.connect(self.on_exercise_input_changed)
        self.exercise_input.returnPressed.connect(self.on_add_exercise_clicked)
        layout.addWidget(self.exercise_input)

        self.suggestions_list = QListWidget()
        self.suggestions_list.setObjectName("suggestionsList")
        self.suggestions_list.setMaximumHeight(120)
        self.suggestions_list.setMaximumWidth(300)
        self.suggestions_list.itemClicked.connect(self.on_suggestion_selected)
        self.suggestions_list.hide()
        layout.addWidget(self.suggestions_list)

        self.add_button = QPushButton("Add")
        self.add_button.setObjectName("addExerciseButton")
        self.add_button.setFixedSize(60, 32)
        self.add_button.clicked.connect(self.on_add_exercise_clicked)
        layout.addWidget(self.add_button)

        layout.addStretch()

        duration_panel = QFrame()
        duration_panel.setObjectName("workoutDurationPanel")
        duration_layout = QVBoxLayout(duration_panel)
        duration_layout.setContentsMargins(10, 0, 0, 0)
        duration_layout.setSpacing(2)
        self.workout_duration_panel = duration_panel

        custom_fields = self.settings.get("custom_fields", {})

        self.workout_duration_input = QLineEdit()
        self.workout_duration_input.setObjectName("workoutDurationInput")
        self.workout_duration_input.setPlaceholderText(custom_fields.get("field1_placeholder", DEFAULT_SETTINGS["custom_fields"]["field1_placeholder"]))
        self.workout_duration_input.setMaximumWidth(220)
        self.workout_duration_input.setText(str(custom_fields.get("field1_value", "") or ""))
        self.workout_duration_input.editingFinished.connect(self.on_workout_duration_changed)
        duration_layout.addWidget(self.workout_duration_input)

        self.workout_duration_panel.setVisible(bool(custom_fields.get("field1_enabled", True)))

        layout.addWidget(duration_panel)

        return container

    def on_workout_duration_changed(self):
        cf = self.settings.get("custom_fields", {})
        cf["field1_value"] = self.workout_duration_input.text().strip() or None
        self.settings["custom_fields"] = cf
        self.save_settings_from_ui(persist=False)
        self.update_session_total()

    def on_custom_field_changed(self):
        # Persist the live value into settings (non-persistent unless user saves)
        cf = self.settings.get("custom_fields", {})
        cf["field2_value"] = self.custom_field_input.text().strip() or None
        self.settings["custom_fields"] = cf
        self.save_settings_from_ui(persist=False)

    def on_exercise_input_changed(self, text):
        """Update suggestions as user types."""
        self.suggestions_list.clear()
        self.suggestions_list.setCurrentRow(-1)

        if not text.strip():
            self.suggestions_list.hide()
            return

        # Fuzzy search through existing exercises
        results = fuzzy_search(text, self.all_exercises)
        
        if results:
            self.suggestions_list.show()
            for score, exercise in results[:5]:  # Show top 5 matches
                item = QListWidgetItem(exercise)
                # Color by confidence
                if score >= 80:
                    item.setBackground(QColor("#2a4a2a"))
                elif score >= 50:
                    item.setBackground(QColor("#3a3a2a"))
                self.suggestions_list.addItem(item)
            self.suggestions_list.setCurrentRow(0)
        else:
            self.suggestions_list.hide()

    def move_suggestion_selection(self, delta):
        if not self.suggestions_list.isVisible() or self.suggestions_list.count() == 0:
            return False

        current_row = self.suggestions_list.currentRow()
        if current_row < 0:
            current_row = 0 if delta > 0 else self.suggestions_list.count() - 1
        else:
            current_row = max(0, min(self.suggestions_list.count() - 1, current_row + delta))

        self.suggestions_list.setCurrentRow(current_row)
        self.suggestions_list.scrollToItem(self.suggestions_list.currentItem())
        return True

    def accept_selected_suggestion(self):
        item = self.suggestions_list.currentItem()
        if item is None:
            return False

        self.on_suggestion_selected(item)
        return True

    def on_suggestion_selected(self, item):
        """Handle suggestion selection."""
        exercise_name = item.text()
        self.exercise_input.setText(exercise_name)
        self.suggestions_list.hide()
        self.on_add_exercise_clicked()

    def on_add_exercise_clicked(self):
        """Add a new exercise to the list."""
        exercise_name = self.exercise_input.text().strip()

        if not exercise_name:
            return

        # Add to exercises if not already there
        if exercise_name not in self.all_exercises:
            self.all_exercises.append(exercise_name)
            self.save_exercises()

        # Add exercise row if not already displayed
        if not any(row.name_label.text() == exercise_name for row in self.rows):
            self.add_exercise(exercise_name)
            # Auto-focus first weight field of newly added exercise
            new_row = self.rows[-1]
            w, _ = new_row.sets[0]
            w.setFocus()
            w.setCursorPosition(0)

        self.exercise_input.clear()
        self.suggestions_list.hide()

    def load_exercises(self):
        """Load exercises from JSON file."""
        try:
            with self.exercises_file.open("r", encoding="utf-8") as f:
                loaded = json.load(f)
                if isinstance(loaded, list) and loaded:
                    return loaded
        except (OSError, json.JSONDecodeError) as e:
            print("Error loading exercises:", e)

        return list(self.settings.get("default_exercises", DEFAULT_SETTINGS["default_exercises"]))

    def ensure_default_exercises(self):
        defaults = list(self.settings.get("default_exercises", DEFAULT_SETTINGS["default_exercises"]))
        missing = [exercise for exercise in defaults if exercise not in self.all_exercises]
        if missing:
            self.all_exercises = missing + self.all_exercises
            self.save_exercises()

    def save_exercises(self):
        """Save exercises to JSON file."""
        try:
            self.exercises_file.parent.mkdir(parents=True, exist_ok=True)
            with self.exercises_file.open("w", encoding="utf-8") as f:
                json.dump(self.all_exercises, f, indent=4, ensure_ascii=False)
        except OSError as e:
            print("Error saving exercises:", e)

    def add_exercise(self, name):
        row = ExerciseRow(name, on_delete=self.delete_exercise_row)
        row.on_total_changed = self.update_session_total
        # Link to next exercise for Enter navigation
        if self.rows:
            self.rows[-1].next_exercise_row = row
            row.prev_exercise_row = self.rows[-1]
        # Connect all weight/reps changes to session total update
        for weight, reps in row.sets:
            weight.textChanged.connect(self.update_session_total)
            reps.textChanged.connect(self.update_session_total)
        self.scroll_layout.addWidget(row)
        self.rows.append(row)
        self.update_session_total()

    def rebuild_navigation_links(self):
        for index, row in enumerate(self.rows):
            row.prev_exercise_row = self.rows[index - 1] if index > 0 else None
            row.next_exercise_row = self.rows[index + 1] if index < len(self.rows) - 1 else None

    def delete_exercise_row(self, row):
        if row not in self.rows:
            return

        if QMessageBox.question(self, "Delete exercise", f"Delete '{row.name_label.text()}' from this session?", QMessageBox.StandardButton.Yes | QMessageBox.StandardButton.No) != QMessageBox.StandardButton.Yes:
            return

        index = self.rows.index(row)
        self.rows.remove(row)
        self.scroll_layout.removeWidget(row)
        row.setParent(None)
        row.deleteLater()
        self.rebuild_navigation_links()

        if self.rows:
            focus_row = self.rows[min(index, len(self.rows) - 1)]
            focus_row.setFocus()

        self.update_session_total()

    def update_session_total(self):
        """Calculate session total from all non-empty exercises."""
        total = 0
        exercise_payload = []
        for row in self.rows:
            # Skip exercise if all sets are empty
            has_data = any(w.text() or r.text() for w, r in row.sets)
            if not has_data:
                continue

            exercise_payload.append(row.to_payload())
            
            # Sum this exercise's total
            last_weight = None
            for weight, reps in row.sets:
                try:
                    w_val = float(weight.text()) if weight.text() else last_weight
                    if w_val is not None and reps.text():
                        r_val = float(reps.text())
                        total += w_val * r_val
                        last_weight = w_val
                except ValueError:
                    continue
        
        unit_label = self.settings.get("units", DEFAULT_SETTINGS["units"])
        self.session_total.setText(f"SESSION TOTAL VOLUME ({unit_label}): {int(total)}")
        if exercise_payload and not getattr(self, "_loading_rows", False) and self.settings.get("autosave_enabled", True):
            self.autosave_current_session(exercise_payload, total)

    def _compute_session_volume(self):
        total = 0
        for row in self.rows:
            last_weight = None
            for weight, reps in row.sets:
                try:
                    w_val = float(weight.text()) if weight.text() else last_weight
                    if w_val is not None and reps.text():
                        r_val = float(reps.text())
                        total += w_val * r_val
                        last_weight = w_val
                except ValueError:
                    continue
        return total

    def _session_payload_from_rows(self):
        exercise_payload = [row.to_payload() for row in self.rows if any(w.text() or r.text() for w, r in row.sets)]
        return exercise_payload, int(self._compute_session_volume())

    def autosave_current_session(self, exercise_payload, total):
        if not self.settings.get("autosave_enabled", True) or not exercise_payload:
            return

        payload = {
            "saved_at": datetime.now().isoformat(timespec="seconds"),
            "volume": int(total),
            "units": self.settings.get("units", DEFAULT_SETTINGS["units"]),
            "workout_duration": self.workout_duration_input.text().strip() or None,
            "exercises": exercise_payload,
        }

        try:
            write_autosave(self.autosave_file, payload)
        except OSError as e:
            print("Error autosaving session:", e)

    def export_current_session(self):
        exercise_payload, total = self._session_payload_from_rows()

        if not exercise_payload:
            return

        session_payload = {
            "saved_at": datetime.now().isoformat(timespec="seconds"),
            "volume": int(total),
            "units": self.settings.get("units", DEFAULT_SETTINGS["units"]),
            "workout_duration": self.workout_duration_input.text().strip() or None,
            "exercises": exercise_payload,
        }

        try:
            export_session_log(self.logs_dir, session_payload, total, timestamp=datetime.now())
            self.cleanup_export_history()
        except OSError as e:
            print("Error exporting session:", e)

    def closeEvent(self, event):
        self.export_current_session()
        super().closeEvent(event)

    def handle_startup_restore(self):
        if not self.autosave_file.exists():
            return

        try:
            with self.autosave_file.open("r", encoding="utf-8") as handle:
                data = json.load(handle)
        except Exception as exc:
            print("Error reading autosave:", exc)
            return

        mode = self.settings.get("restore_mode", "ask")
        if mode == "no_restore":
            if self.autosave_file.exists():
                try:
                    self.autosave_file.unlink()
                except OSError:
                    pass
            return

        if mode == "current":
            return

        exercises = data.get("exercises", [])
        if not exercises:
            return

        if data.get("workout_duration") is not None:
            self.workout_duration_input.setText(str(data.get("workout_duration", "")))

        if mode == "restore":
            self._restore_from_payload(exercises, replace_current=True)
            return

        message = f"A saved draft from {data.get('saved_at', 'unknown')} was found. Restore it?"
        choice = QMessageBox.question(
            self,
            "Restore draft?",
            message,
            QMessageBox.StandardButton.Yes | QMessageBox.StandardButton.No,
        )
        if choice == QMessageBox.StandardButton.Yes:
            self._restore_from_payload(exercises, replace_current=True)

    def cleanup_export_history(self):
        retention_days = self.settings.get("retention_days")
        if retention_days in (None, "", 0):
            return

        try:
            retention_days = int(retention_days)
        except (TypeError, ValueError):
            return

        if retention_days <= 0:
            return

        cutoff = datetime.now().timestamp() - (retention_days * 24 * 60 * 60)
        for path in self.logs_dir.glob("*.json"):
            if path.name == "autosave.json":
                continue
            if not SESSION_EXPORT_PATTERN.match(path.name):
                continue
            try:
                if path.stat().st_mtime < cutoff:
                    path.unlink()
            except OSError:
                continue

    def clear_rows(self):
        while self.rows:
            row = self.rows.pop()
            row.setParent(None)
            row.deleteLater()

    def _restore_from_payload(self, exercises_payload, replace_current=False):
        """Restore UI rows from a saved exercises payload."""
        self._loading_rows = True
        try:
            if replace_current:
                self.clear_rows()

            for ex in exercises_payload:
                name = ex.get('name')
                if not name:
                    continue
                row = next((r for r in self.rows if r.name_label.text() == name), None)
                if row is None:
                    self.add_exercise(name)
                    row = self.rows[-1]

                sets = ex.get('sets', [])
                while len(row.sets) < len(sets):
                    row.add_set()

                for i, s in enumerate(sets):
                    try:
                        w, r = row.sets[i]
                        w.setText(s.get('weight', ''))
                        r.setText(s.get('reps', ''))
                    except Exception:
                        continue
        finally:
            self.update_session_total()
            self._loading_rows = False

    def refresh_tracker_sessions(self):
        self.tracker_list.clear()
        self.session_items = load_sessions(self.logs_dir)

        for session in self.session_items:
            label = f"{session['filename']}  |  {session['saved_at']}  |  {session['volume']} volume"
            item = QListWidgetItem(label)
            item.setData(Qt.ItemDataRole.UserRole, session)
            self.tracker_list.addItem(item)

        if self.tracker_list.count() > 0 and self.tracker_list.currentRow() < 0:
            self.tracker_list.setCurrentRow(0)

    def on_tracker_selection_changed(self, current, previous):
        if current is None:
            self.tracker_details.setText("No session selected.")
            return

        session = current.data(Qt.ItemDataRole.UserRole)
        if not session:
            self.tracker_details.setText("No session selected.")
            return

        exercise_count = len(session.get('exercises', []))
        self.tracker_details.setText(
            f"Saved at {session['saved_at']} with {session['volume']} total volume across {exercise_count} exercises."
        )

    def _selected_tracker_session(self):
        current = self.tracker_list.currentItem()
        if current is None:
            return None
        return current.data(Qt.ItemDataRole.UserRole)

    def load_selected_session(self):
        session = self._selected_tracker_session()
        if not session:
            QMessageBox.information(self, "Load session", "Select a saved session first.")
            return

        self._restore_from_payload(session.get('exercises', []), replace_current=True)
        self.tabs.setCurrentIndex(0)

    def compare_selected_session(self):
        session = self._selected_tracker_session()
        if not session:
            QMessageBox.information(self, "Compare session", "Select a saved session first.")
            return

        QMessageBox.information(
            self,
            "Compare session",
            "Comparison view is a placeholder for now. The selected session can already be loaded back into Logger.",
        )

    def export_selected_session(self):
        session = self._selected_tracker_session()
        if not session:
            QMessageBox.information(self, "Export session", "Select a saved session first.")
            return

        try:
            session_payload = {
                "saved_at": session.get('saved_at', datetime.now().isoformat(timespec='seconds')),
                "volume": session.get('volume', 0),
                "workout_duration": session.get('workout_duration'),
                "exercises": session.get('exercises', []),
            }
            export_session_log(self.logs_dir, session_payload, session_payload['volume'], timestamp=datetime.now())
            self.refresh_tracker_sessions()
        except OSError as e:
            QMessageBox.warning(self, "Export session", f"Could not export the selected session: {e}")

    def build_settings_tab(self):
        widget = QWidget()
        layout = QVBoxLayout(widget)
        layout.setSpacing(12)
        layout.setContentsMargins(0, 0, 0, 0)

        form_box = QGroupBox("General Settings")
        form_layout = QFormLayout(form_box)
        form_layout.setLabelAlignment(Qt.AlignmentFlag.AlignLeft)

        self.export_dir_input = QLineEdit()
        self.export_dir_input.setText(str(self.settings.get("export_dir", "logs")))
        export_dir_row = QHBoxLayout()
        self.export_dir_browse = QPushButton("Browse")
        self.export_dir_browse.clicked.connect(self.browse_export_dir)
        export_dir_row.addWidget(self.export_dir_input)
        export_dir_row.addWidget(self.export_dir_browse)
        form_layout.addRow("Export location", self._wrap_layout(export_dir_row))

        self.retention_days_input = QSpinBox()
        self.retention_days_input.setMinimum(0)
        self.retention_days_input.setMaximum(3650)
        retention_value = self.settings.get("retention_days")
        self.retention_days_input.setValue(int(retention_value) if retention_value not in (None, "") else 0)
        self.retention_days_input.setSpecialValueText("No limit")
        form_layout.addRow("Keep exports for (days)", self.retention_days_input)

        self.units_combo = QComboBox()
        self.units_combo.addItems(["kg", "lb", "custom"])
        self.units_combo.setCurrentText(self.settings.get("units", "kg"))
        form_layout.addRow("Units", self.units_combo)

        self.restore_mode_combo = QComboBox()
        self.restore_mode_combo.addItem("Ask on launch", "ask")
        self.restore_mode_combo.addItem("Restore automatically", "restore")
        self.restore_mode_combo.addItem("Open current session", "current")
        self.restore_mode_combo.addItem("No restore / no autosave", "no_restore")
        restore_mode = self.settings.get("restore_mode", "ask")
        index = self.restore_mode_combo.findData(restore_mode)
        self.restore_mode_combo.setCurrentIndex(index if index >= 0 else 0)
        form_layout.addRow("Launch restore", self.restore_mode_combo)

        self.autosave_checkbox = QCheckBox("Enable autosave")
        self.autosave_checkbox.setChecked(bool(self.settings.get("autosave_enabled", True)))
        form_layout.addRow("", self.autosave_checkbox)

        custom_box = QGroupBox("Customizable fields")
        custom_layout = QFormLayout(custom_box)

        custom_fields = self.settings.get("custom_fields", {})

        self.field1_visible_checkbox = QCheckBox("Show top-right field")
        self.field1_visible_checkbox.setChecked(bool(custom_fields.get("field1_enabled", True)))
        custom_layout.addRow("Field 1 visible", self.field1_visible_checkbox)

        self.field1_placeholder_input = QLineEdit()
        self.field1_placeholder_input.setText(custom_fields.get("field1_placeholder", DEFAULT_SETTINGS["custom_fields"]["field1_placeholder"]))
        custom_layout.addRow("Field 1 ghost text", self.field1_placeholder_input)

        self.field2_visible_checkbox = QCheckBox("Show bottom-right field")
        self.field2_visible_checkbox.setChecked(bool(custom_fields.get("field2_enabled", False)))
        custom_layout.addRow("Field 2 visible", self.field2_visible_checkbox)

        self.field2_placeholder_input = QLineEdit()
        self.field2_placeholder_input.setText(custom_fields.get("field2_placeholder", DEFAULT_SETTINGS["custom_fields"]["field2_placeholder"]))
        custom_layout.addRow("Field 2 ghost text", self.field2_placeholder_input)

        layout.addWidget(custom_box)

        self.background_input = QLineEdit()
        self.background_input.setText(self.settings.get("background_image", ""))
        background_row = QHBoxLayout()
        self.background_browse = QPushButton("Browse")
        self.background_browse.clicked.connect(self.browse_background_image)
        background_row.addWidget(self.background_input)
        background_row.addWidget(self.background_browse)
        form_layout.addRow("Background image", self._wrap_layout(background_row))

        self.default_exercises_input = QLineEdit()
        defaults_text = ", ".join(self.settings.get("default_exercises", DEFAULT_SETTINGS["default_exercises"]))
        self.default_exercises_input.setText(defaults_text)
        form_layout.addRow("Default exercises", self.default_exercises_input)

        layout.addWidget(form_box)

        buttons = QHBoxLayout()
        self.settings_save_button = QPushButton("Save Settings")
        self.settings_save_button.setObjectName("settingsSaveButton")
        self.settings_save_button.clicked.connect(lambda: self.save_settings_from_ui(persist=True))
        self.settings_apply_button = QPushButton("Apply")
        self.settings_apply_button.setObjectName("settingsApplyButton")
        self.settings_apply_button.clicked.connect(lambda: self.save_settings_from_ui(persist=False))
        buttons.addStretch()
        buttons.addWidget(self.settings_apply_button)
        buttons.addWidget(self.settings_save_button)
        layout.addLayout(buttons)

        info = QLabel("Changes here affect new saves immediately. Open current keeps the current Logger session untouched on launch.")
        info.setWordWrap(True)
        layout.addWidget(info)
        layout.addStretch()
        return widget

    def _wrap_layout(self, layout):
        container = QWidget()
        container.setLayout(layout)
        return container

    def browse_export_dir(self):
        selected = QFileDialog.getExistingDirectory(self, "Choose export location", str(self.export_dir_input.text() or self.app_root))
        if selected:
            self.export_dir_input.setText(selected)

    def browse_background_image(self):
        selected, _ = QFileDialog.getOpenFileName(self, "Choose background image", str(self.app_root), "Images (*.png *.jpg *.jpeg *.webp *.bmp)")
        if selected:
            self.background_input.setText(selected)

    def save_settings_from_ui(self, persist=False):
        retention_days = None if self.retention_days_input.value() == 0 else self.retention_days_input.value()
        default_exercises = [entry.strip() for entry in self.default_exercises_input.text().split(",") if entry.strip()]
        if not default_exercises:
            default_exercises = list(DEFAULT_SETTINGS["default_exercises"])

        workout_duration = self.workout_duration_input.text().strip()
        workout_duration_value = workout_duration or None

        settings = {
            "export_dir": self.export_dir_input.text().strip() or "logs",
            "retention_days": retention_days,
            "units": self.units_combo.currentText(),
            "background_image": self.background_input.text().strip(),
            "restore_mode": self.restore_mode_combo.currentData(),
            "autosave_enabled": self.autosave_checkbox.isChecked() and self.restore_mode_combo.currentData() != "no_restore",
            "default_exercises": default_exercises,
            "show_row_totals": True,
            "custom_fields": {
                "field1_enabled": bool(self.field1_visible_checkbox.isChecked()),
                "field1_placeholder": self.field1_placeholder_input.text().strip() or DEFAULT_SETTINGS["custom_fields"]["field1_placeholder"],
                "field1_value": workout_duration_value,
                "field2_enabled": bool(self.field2_visible_checkbox.isChecked()),
                "field2_placeholder": self.field2_placeholder_input.text().strip() or DEFAULT_SETTINGS["custom_fields"]["field2_placeholder"],
                "field2_value": self.custom_field_input.text().strip() or None,
            },
        }

        self.apply_settings(settings, persist=persist)
        self.ensure_default_exercises()
        if settings["restore_mode"] == "no_restore" and self.autosave_file.exists():
            try:
                self.autosave_file.unlink()
            except OSError:
                pass
        self.update_session_total()
        self.refresh_tracker_sessions()

    def apply_settings(self, settings, persist=False):
        merged = dict(DEFAULT_SETTINGS)
        merged.update(settings or {})
        self.settings = merged

        export_dir = Path(merged["export_dir"])
        if not export_dir.is_absolute():
            export_dir = self.app_root / export_dir

        self.logs_dir = export_dir
        self.autosave_file = self.logs_dir / "autosave.json"
        self.units = merged.get("units", DEFAULT_SETTINGS["units"])
        self.background_image = merged.get("background_image", "")
        self.autosave_enabled = bool(merged.get("autosave_enabled", True)) and merged.get("restore_mode", "ask") != "no_restore"
        self.restore_mode = merged.get("restore_mode", "ask")

        cf = merged.get("custom_fields", {})
        duration_input = getattr(self, "workout_duration_input", None)
        if duration_input is not None:
            duration_input.setPlaceholderText(cf.get("field1_placeholder", DEFAULT_SETTINGS["custom_fields"]["field1_placeholder"]))
            duration_input.setText(str(cf.get("field1_value", "") or ""))

        duration_panel = getattr(self, "workout_duration_panel", None)
        if duration_panel is not None:
            duration_panel.setVisible(bool(cf.get("field1_enabled", True)))

        # Apply custom secondary footer field settings
        cf_input = getattr(self, "custom_field_input", None)
        cf_panel = getattr(self, "custom_field_panel", None)
        if cf_input is not None:
            cf_input.setPlaceholderText(cf.get("field2_placeholder", DEFAULT_SETTINGS["custom_fields"]["field2_placeholder"]))
            cf_input.setText(str(cf.get("field2_value", "") or ""))
        if cf_panel is not None:
            cf_panel.setVisible(bool(cf.get("field2_enabled", False)))

        self.logs_dir.mkdir(parents=True, exist_ok=True)
        self.apply_background_image(self.background_image)

        if persist:
            save_settings(self.config_file, self.settings)

    def apply_background_image(self, image_path):
        if image_path and Path(image_path).exists():
            self.setStyleSheet(self.get_styles(image_path))
        else:
            self.setStyleSheet(self.get_styles())

    def get_styles(self, background_image=None):
        background_rule = "background-color: #121212;"
        if background_image and Path(background_image).exists():
            background_rule = f"background-color: #121212; background-image: url('{background_image}'); background-repeat: no-repeat; background-position: center; background-attachment: fixed;"

        style = """
        QWidget {{
            __BACKGROUND_RULE__
            color: #ffffff;
            font-family: Segoe UI;
            font-size: 16px;
        }}

        QScrollArea {{
            border: none;
            background-color: #121212;
        }}

        QScrollArea > QWidget > QWidget {{
            background-color: #121212;
        }}

        QLabel#title {{
            font-size: 24px;
            font-weight: bold;
            padding-bottom: 12px;
        }}

        QLabel#sessionTotal {{
            margin-top: 12px;
            font-size: 24px;
            font-weight: 900;
            color: #00ffcc;
        }}

        QFrame#workoutDurationPanel {{
            background-color: #171717;
            border: 1px solid #2a2a2a;
            border-radius: 8px;
            padding: 8px 10px 6px 10px;
        }}

        QLabel#workoutDurationLabel {{
            color: #b9b9b9;
            font-size: 11px;
            font-weight: 700;
            text-transform: uppercase;
        }}

        QLineEdit#workoutDurationInput {{
            min-width: 200px;
            background-color: #1e1e1e;
            border: 1px solid #333333;
            border-radius: 6px;
            padding: 10px;
            color: white;
            height: 42px;
        }}

        QLineEdit#workoutDurationInput:focus {{
            border: 1px solid #00ffcc;
        }}

        QGroupBox {{
            border: 1px solid #2a2a2a;
            border-radius: 8px;
            margin-top: 12px;
            padding-top: 10px;
            font-weight: 600;
        }}

        QGroupBox::title {{
            subcontrol-origin: margin;
            left: 10px;
            padding: 0 4px 0 4px;
        }}

        QLabel#addExerciseLabel {{
            font-weight: 600;
            color: #ffffff;
        }}

        QLineEdit#exerciseInput {{
            background-color: #1e1e1e;
            border: 1px solid #333333;
            border-radius: 6px;
            padding: 10px;
            color: white;
            selection-background-color: #00ffcc;
            height: 44px;
        }}

        QLineEdit#exerciseInput:focus {{
            border: 1px solid #00ffcc;
        }}

        QPushButton#addExerciseButton {{
            background-color: #1b1b1b;
            border: 1px solid #333333;
            border-radius: 8px;
            color: #00ffcc;
            font-weight: bold;
            padding: 12px 16px;
            min-height: 44px;
        }}

        QPushButton#addExerciseButton:hover {{
            border: 1px solid #00ffcc;
        }}

        QPushButton#saveSessionButton,
        QPushButton#settingsSaveButton,
        QPushButton#settingsApplyButton,
        QPushButton#trackerRefreshButton,
        QPushButton#trackerLoadButton,
        QPushButton#trackerCompareButton,
        QPushButton#trackerExportButton {{
            background-color: #1b1b1b;
            border: 1px solid #333333;
            border-radius: 8px;
            color: #ffffff;
            font-weight: bold;
            padding: 10px 14px;
        }}

        QPushButton#saveSessionButton:hover,
        QPushButton#settingsSaveButton:hover,
        QPushButton#settingsApplyButton:hover,
        QPushButton#trackerRefreshButton:hover,
        QPushButton#trackerLoadButton:hover,
        QPushButton#trackerCompareButton:hover,
        QPushButton#trackerExportButton:hover {{
            border: 1px solid #00ffcc;
        }}

        QComboBox, QSpinBox, QCheckBox, QLineEdit {{
            background-color: #1e1e1e;
            border: 1px solid #333333;
            border-radius: 6px;
            padding: 8px;
            color: white;
        }}

        QCheckBox {{
            border: none;
            padding: 2px;
            background: transparent;
        }}

        QListWidget#suggestionsList {{
            background-color: #1e1e1e;
            border: 1px solid #333333;
            border-radius: 6px;
            color: white;
        }}

        QListWidget#suggestionsList::item:hover {{
            background-color: #2a2a2a;
        }}

        QListWidget#suggestionsList::item:selected {{
            background-color: #00ffcc;
            color: black;
        }}
        """
        return style.replace("__BACKGROUND_RULE__", background_rule)
