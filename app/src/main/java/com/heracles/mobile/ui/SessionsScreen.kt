package com.heracles.mobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.heracles.mobile.AppViewModel

@Composable
fun HeraclesSessionsScreen(viewModel: AppViewModel) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
        itemsIndexed(viewModel.sessions, key = { index, session -> "${session.savedAt}-${session.id}-$index" }) { _, session ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Saved: ${session.savedAt}")
                    Text("Volume: ${session.volume}")
                    Text("Exercises: ${session.exercises.size}")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { viewModel.restoreFromSession(session) }) {
                            Text("Load")
                        }
                        Button(onClick = { viewModel.deleteSession(session) }) {
                            Text("Delete")
                        }
                    }
                }
            }
        }
    }
}