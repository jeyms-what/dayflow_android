package com.dayflow.feature.today.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dayflow.domain.model.Note
import com.dayflow.domain.model.WorkoutSession
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotesScreen(viewModel: TodayViewModel, contentPadding: PaddingValues) {
    val notes by viewModel.notes.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var selected by remember { mutableStateOf<Note?>(null) }
    var editorOpen by rememberSaveable { mutableStateOf(false) }
    val filtered = notes.filter { query.isBlank() || it.title.contains(query, true) || it.content.contains(query, true) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = contentPadding.calculateBottomPadding() + 92.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Your second brain", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Capture ideas, plans, reflections, and training notes.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Search notes") }, singleLine = true)
                Button(onClick = { selected = null; editorOpen = true }, modifier = Modifier.fillMaxWidth()) { Text("Create a note") }
            }
        }
        if (filtered.isEmpty()) item {
            EmptyWorkspaceCard(if (query.isBlank()) "Nothing written yet" else "No matching notes", "Your notes are stored privately on this device.")
        }
        items(filtered, key = { it.id }) { note ->
            Card(onClick = { selected = note; editorOpen = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(note.title.ifBlank { "Untitled note" }, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        if (note.isPinned) Text("PINNED", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                    }
                    Text(note.content.ifBlank { "No additional text" }, maxLines = 4, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Edited " + SimpleDateFormat("MMM d · h:mm a", Locale.getDefault()).format(Date(note.updatedAtEpochMillis)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
    if (editorOpen) NoteEditorDialog(
        note = selected,
        onDismiss = { editorOpen = false },
        onSave = { title, body, pinned -> viewModel.saveNote(selected?.id, title, body, pinned); editorOpen = false },
        onDelete = { selected?.let { viewModel.deleteNote(it.id) }; editorOpen = false }
    )
}

@Composable
private fun NoteEditorDialog(note: Note?, onDismiss: () -> Unit, onSave: (String, String, Boolean) -> Unit, onDelete: () -> Unit) {
    var title by rememberSaveable(note?.id) { mutableStateOf(note?.title.orEmpty()) }
    var body by rememberSaveable(note?.id) { mutableStateOf(note?.content.orEmpty()) }
    var pinned by rememberSaveable(note?.id) { mutableStateOf(note?.isPinned ?: false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (note == null) "New note" else "Edit note") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true)
                OutlinedTextField(body, { body = it }, label = { Text("Write something…") }, minLines = 5)
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(pinned, { pinned = it }); Text("Pin this note") }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(title, body, pinned) }, enabled = title.isNotBlank() || body.isNotBlank()) { Text("Save") } },
        dismissButton = { Row { if (note != null) TextButton(onClick = onDelete) { Text("Delete") }; TextButton(onClick = onDismiss) { Text("Cancel") } } }
    )
}

@Composable
fun TrainingScreen(viewModel: TodayViewModel, contentPadding: PaddingValues) {
    val sessions by viewModel.workoutSessions.collectAsState()
    var dialogOpen by rememberSaveable { mutableStateOf(false) }
    val weekCount = sessions.count { it.startedAtEpochMillis >= System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000 }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = contentPadding.calculateBottomPadding() + 92.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Train with intention.", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Log your lifts, track sessions, and keep training notes alongside your day.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TrainingStat("TOTAL SESSIONS", sessions.size.toString(), Modifier.weight(1f))
                    TrainingStat("LAST 7 DAYS", weekCount.toString(), Modifier.weight(1f))
                }
                Button(onClick = { dialogOpen = true }, modifier = Modifier.fillMaxWidth()) { Text("Log a workout") }
                Text("Recent sessions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
        if (sessions.isEmpty()) item {
            EmptyWorkspaceCard("Your training starts here", "Log a session with an exercise, muscle group, sets, reps, and weight. Workout history stays on your device.")
        }
        items(sessions, key = { it.id }) { session -> WorkoutCard(session, viewModel) }
    }
    if (dialogOpen) WorkoutDialog(
        onDismiss = { dialogOpen = false },
        onSave = { title, exercise, group, sets, reps, weight, notes ->
            viewModel.saveWorkout(title, exercise, group, sets, reps, weight, notes)
            dialogOpen = false
        }
    )
}

@Composable
private fun TrainingStat(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun WorkoutCard(session: WorkoutSession, viewModel: TodayViewModel) {
    val sets by viewModel.observeWorkoutSets(session.id).collectAsState(initial = emptyList())
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(session.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(SimpleDateFormat("EEE, MMM d · h:mm a", Locale.getDefault()).format(Date(session.startedAtEpochMillis)), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
            sets.firstOrNull()?.let { first ->
                Text("${first.exerciseName} · ${first.muscleGroup}", style = MaterialTheme.typography.bodyMedium)
            }
            if (sets.isNotEmpty()) {
                Text("${sets.size} sets · " + sets.joinToString { "${it.reps} reps @ ${it.weightKg} kg" }, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            if (session.notes.isNotBlank()) Text(session.notes, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(session.status.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun WorkoutDialog(onDismiss: () -> Unit, onSave: (String, String, String, Int, Int, Double, String) -> Unit) {
    var title by rememberSaveable { mutableStateOf("Strength session") }
    var exercise by rememberSaveable { mutableStateOf("") }
    var group by rememberSaveable { mutableStateOf("Chest") }
    var sets by rememberSaveable { mutableStateOf("3") }
    var reps by rememberSaveable { mutableStateOf("10") }
    var weight by rememberSaveable { mutableStateOf("0") }
    var notes by rememberSaveable { mutableStateOf("") }
    val valid = title.isNotBlank() && exercise.isNotBlank() && group.isNotBlank() &&
        (sets.toIntOrNull() ?: 0) in 1..20 && (reps.toIntOrNull() ?: 0) in 1..100 &&
        (weight.toDoubleOrNull() ?: -1.0) >= 0.0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log workout") },
        text = {
            Column(modifier = Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Session name") }, singleLine = true)
                OutlinedTextField(exercise, { exercise = it }, label = { Text("Exercise") }, singleLine = true)
                OutlinedTextField(group, { group = it }, label = { Text("Muscle group") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(sets, { sets = it.filter(Char::isDigit).take(2) }, modifier = Modifier.weight(1f), label = { Text("Sets") }, singleLine = true)
                    OutlinedTextField(reps, { reps = it.filter(Char::isDigit).take(3) }, modifier = Modifier.weight(1f), label = { Text("Reps") }, singleLine = true)
                }
                OutlinedTextField(weight, { weight = it.filter { ch -> ch.isDigit() || ch == '.' }.take(7) }, label = { Text("Weight (kg)") }, singleLine = true)
                OutlinedTextField(notes, { notes = it }, label = { Text("Session notes") }, minLines = 2)
            }
        },
        confirmButton = { TextButton(onClick = { onSave(title, exercise, group, sets.toInt(), reps.toInt(), weight.toDouble(), notes) }, enabled = valid) { Text("Save workout") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun EmptyWorkspaceCard(title: String, body: String) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
