/*
 File: ui/PrebuiltSessionsScreen.kt
 What it does: Shows imported prebuilt workout templates and actions to load, preview, or delete them.
 Main inputs: `prebuiltSessions`, pending text input, and selection state from the ViewModel.
 Main outputs: loading templates into an active session and repository-backed updates.
 Key functions/classes: prebuilt session screen composables and import/load handlers.
*/

package com.heracles.mobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.heracles.mobile.AppViewModel

@Composable
fun PrebuiltSessionsScreen(viewModel: AppViewModel) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        
        // Locked Input Container: Hard constraints prevent expanding past half the layout viewport
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5f) // Restricts card to exactly half the vertical screen profile
        ) {
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxSize(), 
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Pre-built sessions", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Paste a strict workout template here, import it, then load it into the logger as a ghost-filled draft.",
                    style = MaterialTheme.typography.bodySmall,
                )
                
                OutlinedTextField(
                    value = viewModel.pendingPrebuiltText,
                    onValueChange = viewModel::updatePendingPrebuiltText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f), // Forces the text box to take remaining available card interior space, enabling inner scrolling
                    minLines = 4,
                    label = { Text("Paste workout template") },
                    placeholder = {
                        Text(
                            "Workout: Push A\nBodyweight: 82.4\nDuration: 45\n\nExercise: Bench Press\nSet: 8 x 60",
                        )
                    },
                )
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.importPrebuiltSessionText() }) {
                        Text("Import")
                    }
                    OutlinedButton(onClick = { viewModel.updatePendingPrebuiltText("") }) {
                        Text("Clear")
                    }
                }
                if (viewModel.lastPrebuiltMessage.isNotBlank()) {
                    Text(viewModel.lastPrebuiltMessage, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // Bottom List Section: Takes up the remaining half of the viewport seamlessly
        if (viewModel.prebuiltSessions.isEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("No pre-built sessions yet")
                    Text("Import one from text to see it listed here.", style = MaterialTheme.typography.bodySmall)
                }
            }
            return
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
            items(viewModel.prebuiltSessions, key = { it.id }) { session ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(session.title, style = MaterialTheme.typography.titleMedium)
                        Text(session.createdAt, style = MaterialTheme.typography.bodySmall)
                        Text("Exercises: ${session.exercises.size}", style = MaterialTheme.typography.bodySmall)
                        Text(
                            session.sourceText.lineSequence().take(4).joinToString("\n"),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { viewModel.loadPrebuiltSession(session) }) {
                                Text("Load")
                            }
                            OutlinedButton(onClick = { viewModel.deletePrebuiltSession(session) }) {
                                Text("Delete")
                            }
                        }
                    }
                }
            }
        }
    }
}