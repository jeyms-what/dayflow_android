package com.dayflow.feature.today.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
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
import com.dayflow.domain.model.WorkoutExerciseInput
import com.dayflow.domain.model.WorkoutSetInput
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotesScreen(viewModel: TodayViewModel, contentPadding: PaddingValues) {
    val notes by viewModel.notes.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var selected by remember { mutableStateOf<Note?>(null) }
    var selectedCategory by rememberSaveable { mutableStateOf("ALL") }
    var editorOpen by rememberSaveable { mutableStateOf(false) }
    val categories = listOf("ALL", "GENERAL", "IDEAS", "JOURNAL", "STUDY", "TRAINING")
    val filtered = notes.filter {
        (selectedCategory == "ALL" || it.category == selectedCategory) &&
            (query.isBlank() || it.title.contains(query, true) || it.content.contains(query, true))
    }
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
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
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
                        Text(
                            note.category.lowercase().replaceFirstChar { it.uppercase() },
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelSmall
                        )
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
        onSave = { title, body, pinned, category -> viewModel.saveNote(selected?.id, title, body, pinned, category); editorOpen = false },
        onDelete = { selected?.let { viewModel.deleteNote(it.id) }; editorOpen = false }
    )
}

@Composable
private fun NoteEditorDialog(
    note: Note?,
    onDismiss: () -> Unit,
    onSave: (String, String, Boolean, String) -> Unit,
    onDelete: () -> Unit
) {
    var title by rememberSaveable(note?.id) { mutableStateOf(note?.title.orEmpty()) }
    var body by rememberSaveable(note?.id) { mutableStateOf(note?.content.orEmpty()) }
    var pinned by rememberSaveable(note?.id) { mutableStateOf(note?.isPinned ?: false) }
    var category by rememberSaveable(note?.id) { mutableStateOf(note?.category ?: "GENERAL") }
    var categoryExpanded by rememberSaveable(note?.id) { mutableStateOf(false) }
    val categories = listOf("GENERAL", "IDEAS", "JOURNAL", "STUDY", "TRAINING")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (note == null) "New note" else "Edit note") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true)
                Box {
                    OutlinedButton(onClick = { categoryExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Category · ${category.lowercase().replaceFirstChar { it.uppercase() }}")
                    }
                    DropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                        categories.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.lowercase().replaceFirstChar { it.uppercase() }) },
                                onClick = { category = option; categoryExpanded = false }
                            )
                        }
                    }
                }
                OutlinedTextField(body, { body = it }, label = { Text("Write something…") }, minLines = 6)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(pinned, { pinned = it })
                    Text("Pin this note")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(title, body, pinned, category) },
                enabled = title.isNotBlank() || body.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (note != null) TextButton(onClick = onDelete) { Text("Delete") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

private data class WorkoutSetDraft(
    val id: Int,
    val reps: String = "10",
    val weightKg: String = "0"
)

private data class WorkoutExerciseDraft(
    val id: Int,
    val exerciseName: String = "",
    val muscleGroup: String = "Chest",
    val sets: List<WorkoutSetDraft> = listOf(WorkoutSetDraft(id = 0))
)

@Composable
fun TrainingScreen(viewModel: TodayViewModel, contentPadding: PaddingValues) {
    val sessions by viewModel.workoutSessions.collectAsState()
    var dialogOpen by rememberSaveable { mutableStateOf(false) }
    val weekCount = sessions.count {
        it.startedAtEpochMillis >= System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp, end = 20.dp, top = 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 92.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Train with intention.", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Build a training history that fits around the rest of your life.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TrainingStat("TOTAL SESSIONS", sessions.size.toString(), Modifier.weight(1f))
                    TrainingStat("LAST 7 DAYS", weekCount.toString(), Modifier.weight(1f))
                }
                RestTimerCard()
                Button(onClick = { dialogOpen = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Log a workout")
                }
                Text("Recent sessions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
        if (sessions.isEmpty()) item {
            EmptyWorkspaceCard(
                "Your training starts here",
                "Create a session, add several exercises, and record reps and weight for each set. Your training history stays on this device."
            )
        }
        items(sessions, key = { it.id }) { session -> WorkoutCard(session, viewModel) }
    }
    if (dialogOpen) WorkoutDialog(
        onDismiss = { dialogOpen = false },
        onSave = { title, durationMinutes, exercises, notes ->
            viewModel.saveWorkout(title, durationMinutes, exercises, notes)
            dialogOpen = false
        }
    )
}

@Composable
private fun RestTimerCard() {
    var selectedDuration by rememberSaveable { mutableStateOf(90) }
    var secondsRemaining by rememberSaveable { mutableStateOf(90) }
    var isRunning by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(isRunning, secondsRemaining) {
        if (isRunning && secondsRemaining > 0) {
            delay(1_000)
            secondsRemaining = (secondsRemaining - 1).coerceAtLeast(0)
        } else if (secondsRemaining == 0) {
            isRunning = false
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("REST TIMER", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Recover between sets", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                Text(
                    "%d:%02d".format(secondsRemaining / 60, secondsRemaining % 60),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(30, 60, 90, 120).forEach { seconds ->
                    FilterChip(
                        selected = selectedDuration == seconds,
                        onClick = {
                            selectedDuration = seconds
                            secondsRemaining = seconds
                            isRunning = false
                        },
                        label = { Text(if (seconds < 60) "${seconds}s" else "${seconds / 60}m") }
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        if (isRunning) {
                            isRunning = false
                        } else {
                            if (secondsRemaining == 0) secondsRemaining = selectedDuration
                            isRunning = true
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isRunning) "Pause" else if (secondsRemaining == 0) "Restart" else "Start rest")
                }
                OutlinedButton(
                    onClick = {
                        isRunning = false
                        secondsRemaining = selectedDuration
                    }
                ) { Text("Reset") }
            }
            Text(
                "Timer runs while this screen is open. It does not yet send a background notification.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
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
    val exercises = sets.groupBy { it.exerciseName to it.muscleGroup }
    val totalVolume = sets.sumOf { it.reps * it.weightKg }
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text(session.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                SimpleDateFormat("EEE, MMM d · h:mm a", Locale.getDefault()).format(Date(session.startedAtEpochMillis)),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                "${session.durationSeconds / 60} min session",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (exercises.isNotEmpty()) {
                exercises.forEach { (exercise, exerciseSets) ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(exercise.first, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(exercise.second, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        exerciseSets.forEach { set ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Set ${set.setNumber}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                Text("${set.reps} reps", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                Text("${set.weightKg} kg", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${sets.size} total sets", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                    Text("Volume ${"%.1f".format(totalVolume)} kg", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
            }
            if (session.notes.isNotBlank()) Text(session.notes, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun WorkoutDialog(
    onDismiss: () -> Unit,
    onSave: (String, Int, List<WorkoutExerciseInput>, String) -> Unit
) {
    var title by rememberSaveable { mutableStateOf("Strength session") }
    var durationText by rememberSaveable { mutableStateOf("60") }
    var notes by rememberSaveable { mutableStateOf("") }
    var nextExerciseId by remember { mutableStateOf(1) }
    var nextSetId by remember { mutableStateOf(1) }
    var exercises by remember {
        mutableStateOf(listOf(WorkoutExerciseDraft(id = 0)))
    }

    fun updateExercise(id: Int, transform: (WorkoutExerciseDraft) -> WorkoutExerciseDraft) {
        exercises = exercises.map { exercise ->
            if (exercise.id == id) transform(exercise) else exercise
        }
    }

    val valid = title.isNotBlank() &&
        (durationText.toIntOrNull() ?: 0) in 1..600 &&
        exercises.isNotEmpty() &&
        exercises.all { exercise ->
            exercise.exerciseName.isNotBlank() && exercise.muscleGroup.isNotBlank() &&
                exercise.sets.isNotEmpty() &&
                exercise.sets.all { set ->
                    (set.reps.toIntOrNull() ?: 0) in 1..100 &&
                        (set.weightKg.toDoubleOrNull() ?: -1.0) in 0.0..1000.0
                }
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log workout") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Session name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = durationText,
                    onValueChange = { durationText = it.filter(Char::isDigit).take(3) },
                    label = { Text("Duration (minutes)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                exercises.forEachIndexed { exerciseIndex, exercise ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("EXERCISE ${exerciseIndex + 1}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                if (exercises.size > 1) {
                                    TextButton(onClick = { exercises = exercises.filterNot { it.id == exercise.id } }) {
                                        Text("Remove")
                                    }
                                }
                            }
                            OutlinedTextField(
                                value = exercise.exerciseName,
                                onValueChange = { value -> updateExercise(exercise.id) { it.copy(exerciseName = value) } },
                                label = { Text("Exercise name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = exercise.muscleGroup,
                                onValueChange = { value -> updateExercise(exercise.id) { it.copy(muscleGroup = value) } },
                                label = { Text("Muscle group") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text("SETS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            exercise.sets.forEachIndexed { setIndex, set ->
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("${setIndex + 1}", modifier = Modifier.width(22.dp), style = MaterialTheme.typography.labelMedium)
                                    OutlinedTextField(
                                        value = set.reps,
                                        onValueChange = { value ->
                                            updateExercise(exercise.id) { current ->
                                                current.copy(sets = current.sets.map { if (it.id == set.id) it.copy(reps = value.filter(Char::isDigit).take(3)) else it })
                                            }
                                        },
                                        label = { Text("Reps") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = set.weightKg,
                                        onValueChange = { value ->
                                            updateExercise(exercise.id) { current ->
                                                current.copy(sets = current.sets.map { if (it.id == set.id) it.copy(weightKg = value.filter { ch -> ch.isDigit() || ch == '.' }.take(7)) else it })
                                            }
                                        },
                                        label = { Text("kg") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (exercise.sets.size > 1) {
                                        TextButton(onClick = {
                                            updateExercise(exercise.id) { current ->
                                                current.copy(sets = current.sets.filterNot { it.id == set.id })
                                            }
                                        }) { Text("×") }
                                    }
                                }
                            }
                            OutlinedButton(
                                onClick = {
                                    if (exercise.sets.size < 20) {
                                        val id = nextSetId++
                                        updateExercise(exercise.id) { it.copy(sets = it.sets + WorkoutSetDraft(id = id)) }
                                    }
                                },
                                enabled = exercise.sets.size < 20,
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Add set") }
                        }
                    }
                }
                OutlinedButton(
                    onClick = {
                        if (exercises.size < 20) {
                            val id = nextExerciseId++
                            exercises = exercises + WorkoutExerciseDraft(id = id)
                        }
                    },
                    enabled = exercises.size < 20,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Add another exercise") }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Workout notes") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val input = exercises.map { exercise ->
                        WorkoutExerciseInput(
                            exerciseName = exercise.exerciseName.trim(),
                            muscleGroup = exercise.muscleGroup.trim(),
                            sets = exercise.sets.map { set ->
                                WorkoutSetInput(
                                    reps = set.reps.toInt(),
                                    weightKg = set.weightKg.toDouble()
                                )
                            }
                        )
                    }
                    onSave(title, durationText.toInt(), input, notes)
                },
                enabled = valid
            ) { Text("Save workout") }
        },
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
