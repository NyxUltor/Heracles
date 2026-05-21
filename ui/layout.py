import json
from pathlib import Path
from datetime import datetime

from PyQt6.QtCore import Qt
from PyQt6.QtWidgets import QWidget, QVBoxLayout, QHBoxLayout, QLabel, QLineEdit, QScrollArea, QFrame, QPushButton, QListWidget, QListWidgetItem
from PyQt6.QtGui import QColor
from ui.exercise_row import ExerciseRow
from utils.export import export_session_log, write_autosave
from utils.fuzzy import fuzzy_search


class App(QWidget):
    def __init__(self):
        super().__init__()

        self.setWindowTitle("Hercules")
        self.setMinimumWidth(900)
        
        self.exercises_file = Path(__file__).resolve().parent.parent / "data" / "exercises.json"
        self.logs_dir = Path(__file__).resolve().parent.parent / "logs"
        self.autosave_file = self.logs_dir / "autosave.json"
        self.all_exercises = self.load_exercises()

        self.layout = QVBoxLayout(self)
        self.layout.setSpacing(10)
        self.layout.setContentsMargins(16, 16, 16, 16)

        title = QLabel("HERCULES")
        title.setObjectName("title")
        self.layout.addWidget(title)

        # Add exercise section
        add_ex_container = self.build_add_exercise_section()
        self.layout.addWidget(add_ex_container)

        self.scroll_area = QScrollArea()
        self.scroll_area.setWidgetResizable(True)
        self.scroll_area.setHorizontalScrollBarPolicy(Qt.ScrollBarPolicy.ScrollBarAlwaysOff)
        self.scroll_area.setFrameShape(QFrame.Shape.NoFrame)

        self.scroll_content = QWidget()
        self.scroll_layout = QVBoxLayout(self.scroll_content)
        self.scroll_layout.setSpacing(10)
        self.scroll_layout.setContentsMargins(0, 0, 0, 0)

        self.scroll_area.setWidget(self.scroll_content)
        self.layout.addWidget(self.scroll_area, 1)

        self.session_total = QLabel("SESSION TOTAL: 0")
        self.session_total.setObjectName("sessionTotal")
        self.layout.addWidget(self.session_total)

        self._loading_rows = True
        self.rows = []
        for name in self.all_exercises:
            self.add_exercise(name)

        self._loading_rows = False

        self.setStyleSheet(self.get_styles())
        self.update_session_total()

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

        self.exercise_input = QLineEdit()
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

        return container

    def on_exercise_input_changed(self, text):
        """Update suggestions as user types."""
        self.suggestions_list.clear()

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
        else:
            self.suggestions_list.hide()

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
                return json.load(f)
        except (OSError, json.JSONDecodeError) as e:
            print("Error loading exercises:", e)
            return ["Push Ups", "Pull Ups"]

    def save_exercises(self):
        """Save exercises to JSON file."""
        try:
            self.exercises_file.parent.mkdir(parents=True, exist_ok=True)
            with self.exercises_file.open("w", encoding="utf-8") as f:
                json.dump(self.all_exercises, f, indent=4, ensure_ascii=False)
        except OSError as e:
            print("Error saving exercises:", e)

    def add_exercise(self, name):
        row = ExerciseRow(name)
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
        
        self.session_total.setText(f"SESSION TOTAL: {int(total)}")
        if not getattr(self, "_loading_rows", False):
            self.autosave_current_session(exercise_payload, total)

    def autosave_current_session(self, exercise_payload, total):
        payload = {
            "saved_at": datetime.now().isoformat(timespec="seconds"),
            "volume": int(total),
            "exercises": exercise_payload,
        }

        try:
            write_autosave(self.autosave_file, payload)
        except OSError as e:
            print("Error autosaving session:", e)

    def export_current_session(self):
        exercise_payload = [row.to_payload() for row in self.rows if any(w.text() or r.text() for w, r in row.sets)]

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

        if not exercise_payload:
            return

        session_payload = {
            "saved_at": datetime.now().isoformat(timespec="seconds"),
            "volume": int(total),
            "exercises": exercise_payload,
        }

        try:
            export_session_log(self.logs_dir, session_payload, total, timestamp=datetime.now())
        except OSError as e:
            print("Error exporting session:", e)

    def closeEvent(self, event):
        self.export_current_session()
        super().closeEvent(event)

    def get_styles(self):
        return """
        QWidget {
            background-color: #121212;
            color: #ffffff;
            font-family: Segoe UI;
            font-size: 14px;
        }

        QScrollArea {
            border: none;
            background-color: #121212;
        }

        QScrollArea > QWidget > QWidget {
            background-color: #121212;
        }

        QLabel#title {
            font-size: 20px;
            font-weight: bold;
            padding-bottom: 10px;
        }

        QLabel#sessionTotal {
            margin-top: 15px;
            font-size: 16px;
            color: #00ffcc;
        }

        QLabel#addExerciseLabel {
            font-weight: 600;
            color: #ffffff;
        }

        QLineEdit#exerciseInput {
            background-color: #1e1e1e;
            border: 1px solid #333333;
            border-radius: 4px;
            padding: 6px;
            color: white;
            selection-background-color: #00ffcc;
        }

        QLineEdit#exerciseInput:focus {
            border: 1px solid #00ffcc;
        }

        QPushButton#addExerciseButton {
            background-color: #1b1b1b;
            border: 1px solid #333333;
            border-radius: 4px;
            color: #00ffcc;
            font-weight: bold;
        }

        QPushButton#addExerciseButton:hover {
            border: 1px solid #00ffcc;
        }

        QListWidget#suggestionsList {
            background-color: #1e1e1e;
            border: 1px solid #333333;
            border-radius: 4px;
            color: white;
        }

        QListWidget#suggestionsList::item:hover {
            background-color: #2a2a2a;
        }

        QListWidget#suggestionsList::item:selected {
            background-color: #00ffcc;
            color: black;
        }
        """
