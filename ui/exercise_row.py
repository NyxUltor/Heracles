from PyQt6.QtWidgets import QWidget, QHBoxLayout, QVBoxLayout, QLabel, QLineEdit, QFrame, QPushButton, QSizePolicy
from PyQt6.QtCore import Qt

class SetLineEdit(QLineEdit):
    """Custom QLineEdit with arrow key navigation between sets."""
    def __init__(self, exercise_row, set_index, is_weight):
        super().__init__()
        self.exercise_row = exercise_row
        self.set_index = set_index
        self.is_weight = is_weight

    def is_at_start(self):
        """Check if cursor is at the start of the field."""
        return self.cursorPosition() == 0

    def is_at_end(self):
        """Check if cursor is at the end of the field."""
        return self.cursorPosition() == len(self.text())

    def keyPressEvent(self, event):
        # Handle + key to create new set
        if event.text() == "+":
            self.exercise_row.add_set_and_focus()
            return

        # Arrow key navigation: only when at edges or field is empty
        if event.key() == Qt.Key.Key_Left:
            if self.text() == "" or (self.is_at_start() and not self.is_weight):
                # Move from R to W in same set
                w, _ = self.exercise_row.sets[self.set_index]
                w.setFocus()
                w.setCursorPosition(len(w.text()))
                return
        elif event.key() == Qt.Key.Key_Right:
            if self.text() == "" or (self.is_at_end() and self.is_weight):
                # Move from W to R in same set
                _, r = self.exercise_row.sets[self.set_index]
                r.setFocus()
                r.setCursorPosition(0)
                return
            elif self.text() == "" or (self.is_at_end() and not self.is_weight):
                # Move from R to next W (requires next set to exist)
                if self.set_index < len(self.exercise_row.sets) - 1:
                    w, _ = self.exercise_row.sets[self.set_index + 1]
                    w.setFocus()
                    w.setCursorPosition(0)
                    return
        elif event.key() == Qt.Key.Key_Up:
            if self.text() == "" or self.is_at_start():
                # Move within sets if not at first set
                if self.set_index > 0:
                    if self.is_weight:
                        w, _ = self.exercise_row.sets[self.set_index - 1]
                        w.setFocus()
                        w.setCursorPosition(len(w.text()))
                    else:
                        _, r = self.exercise_row.sets[self.set_index - 1]
                        r.setFocus()
                        r.setCursorPosition(len(r.text()))
                    return
                # At first set, move to previous exercise's last reps
                elif self.exercise_row.prev_exercise_row and self.is_weight:
                    prev_ex = self.exercise_row.prev_exercise_row
                    _, r = prev_ex.sets[-1]
                    r.setFocus()
                    r.setCursorPosition(len(r.text()))
                    return
        elif event.key() == Qt.Key.Key_Down:
            if self.text() == "" or self.is_at_end():
                # Move within sets if not at last set
                if self.set_index < len(self.exercise_row.sets) - 1:
                    if self.is_weight:
                        w, _ = self.exercise_row.sets[self.set_index + 1]
                        w.setFocus()
                        w.setCursorPosition(0)
                    else:
                        _, r = self.exercise_row.sets[self.set_index + 1]
                        r.setFocus()
                        r.setCursorPosition(0)
                    return
                # At last set, move to next exercise's first weight
                elif self.exercise_row.next_exercise_row and not self.is_weight:
                    next_ex = self.exercise_row.next_exercise_row
                    w, _ = next_ex.sets[0]
                    w.setFocus()
                    w.setCursorPosition(0)
                    return

        super().keyPressEvent(event)

class ExerciseRow(QWidget):
    def __init__(self, name):
        super().__init__()

        self.sets = []
        self.next_exercise_row = None  # Set by parent App for Enter navigation
        self.prev_exercise_row = None  # Set by parent App for Up navigation
        self.on_total_changed = None  # Optional callback when any set value changes

        self.layout = QHBoxLayout()
        self.layout.setSpacing(8)
        self.layout.setContentsMargins(0, 5, 0, 5)

        # Exercise Name
        self.name_label = QLabel(name)
        self.name_label.setFixedWidth(140)
        self.name_label.setObjectName("exerciseName")
        self.layout.addWidget(self.name_label)

        # Sets container
        self.sets_container = QHBoxLayout()
        self.sets_container.setSpacing(11)
        self.layout.addLayout(self.sets_container)

        # Plus button hidden; + key on keyboard creates new set instead
        self.add_button = QPushButton("+")
        self.add_button.setObjectName("addSetButton")
        self.add_button.setFixedSize(28, 28)
        self.add_button.setFocusPolicy(Qt.FocusPolicy.StrongFocus)
        self.add_button.clicked.connect(self.add_set_and_focus)
        self.add_button.hide()
        self.layout.addWidget(self.add_button)

        # Spacer line (visual balance)
        line = QFrame()
        line.setFrameShape(QFrame.Shape.VLine)
        line.setStyleSheet("color: #333;")
        self.layout.addWidget(line)

        # Total label
        self.total_label = QLabel("Total: 0")
        self.total_label.setFixedWidth(120)
        self.total_label.setAlignment(Qt.AlignmentFlag.AlignRight)
        self.total_label.setObjectName("totalLabel")
        self.layout.addWidget(self.total_label)

        self.setLayout(self.layout)

        # Start with 3 sets
        for _ in range(3):
            self.add_set()

        # Focus first input and position cursor at end
        first_weight = self.sets[0][0]
        first_weight.setFocus()
        first_weight.setCursorPosition(0)  # Position at end of empty field

        self.apply_styles()

    def add_set(self):
        # Create a container for one set.
        set_number = len(self.sets) + 1

        set_group = QWidget()
        set_group.setSizePolicy(QSizePolicy.Policy.Fixed, QSizePolicy.Policy.Fixed)
        set_group_layout = QVBoxLayout(set_group)
        set_group_layout.setSpacing(3)
        set_group_layout.setContentsMargins(0, 0, 0, 0)

        set_label = QLabel(f"Set {set_number}")
        set_label.setObjectName("setLabel")
        set_label.setAlignment(Qt.AlignmentFlag.AlignHCenter)
        set_group_layout.addWidget(set_label)

        set_frame = QFrame()
        set_frame.setObjectName("setFrame")
        set_frame.setSizePolicy(QSizePolicy.Policy.Fixed, QSizePolicy.Policy.Fixed)
        set_layout = QHBoxLayout()
        set_layout.setSpacing(1)
        set_layout.setContentsMargins(5, 2, 5, 2)

        current_set_index = len(self.sets)

        weight = SetLineEdit(self, current_set_index, is_weight=True)
        weight.setFixedWidth(34)
        weight.setAlignment(Qt.AlignmentFlag.AlignCenter)
        weight.setPlaceholderText("W")

        sep = QLabel("|")
        sep.setObjectName("sep")
        sep.setFixedWidth(6)
        sep.setAlignment(Qt.AlignmentFlag.AlignCenter)

        reps = SetLineEdit(self, current_set_index, is_weight=False)
        reps.setFixedWidth(34)
        reps.setAlignment(Qt.AlignmentFlag.AlignCenter)
        reps.setPlaceholderText("R")

        set_layout.addWidget(weight)
        set_layout.addWidget(sep)
        set_layout.addWidget(reps)

        set_frame.setLayout(set_layout)
        set_group_layout.addWidget(set_frame)
        self.sets_container.addWidget(set_group)

        self.sets.append((weight, reps))

        weight.returnPressed.connect(lambda: reps.setFocus())
        reps.returnPressed.connect(lambda: self.on_reps_return(current_set_index))

        weight.textChanged.connect(self.update_total)
        reps.textChanged.connect(self.update_total)
        
        # Also notify app-level total if callback is set
        if self.on_total_changed:
            weight.textChanged.connect(self.on_total_changed)
            reps.textChanged.connect(self.on_total_changed)

        return weight, reps

    def on_reps_return(self, set_index):
        """Handle Enter key on reps field: move to next set or next exercise."""
        if set_index < len(self.sets) - 1:
            # Not the last set, move to next set's weight
            next_weight, _ = self.sets[set_index + 1]
            next_weight.setFocus()
            next_weight.setCursorPosition(0)
        else:
            # Last set: move to next exercise if available
            if self.next_exercise_row:
                next_weight, _ = self.next_exercise_row.sets[0]
                next_weight.setFocus()
                next_weight.setCursorPosition(0)

    def add_set_and_focus(self):
        weight, _ = self.add_set()
        weight.setFocus()
        weight.setCursorPosition(len(weight.text()))  # Position at end

    def to_payload(self):
        return {
            "name": self.name_label.text(),
            "sets": [
                {"weight": weight.text(), "reps": reps.text()}
                for weight, reps in self.sets
            ],
        }

    def update_total(self):
        total = 0
        last_weight = None

        for i, (weight, reps) in enumerate(self.sets):
            try:
                # If weight is empty, use the previous set's weight
                w_val = float(weight.text()) if weight.text() else last_weight
                if w_val is not None:
                    r_val = float(reps.text())
                    total += w_val * r_val
                    last_weight = w_val
            except ValueError:
                continue

        self.total_label.setText(f"Total: {int(total)}")

    def apply_styles(self):
        self.setStyleSheet("""
        QLabel#exerciseName {
            font-weight: bold;
            color: #ffffff;
        }

        QLabel#totalLabel {
            color: #00ffcc;
            font-weight: bold;
        }

        QLabel#sep {
            color: #666666;
        }

        QLabel#setLabel {
            color: #9a9a9a;
            font-size: 11px;
            font-weight: 600;
        }

        QFrame#setFrame {
            background-color: #171717;
            border: 1px solid #2a2a2a;
            border-radius: 6px;
        }

        QLineEdit {
            background-color: #1e1e1e;
            border: 1px solid #333333;
            border-radius: 4px;
            padding: 3px;
            color: white;
        }

        QLineEdit:focus {
            border: 1px solid #00ffcc;
        }

        QPushButton#addSetButton {
            background-color: #1b1b1b;
            border: 1px solid #333333;
            border-radius: 14px;
            color: #00ffcc;
            font-weight: bold;
        }

        QPushButton#addSetButton:hover {
            border: 1px solid #00ffcc;
        }

        QPushButton#addSetButton:focus {
            border: 1px solid #00ffcc;
        }
        """)