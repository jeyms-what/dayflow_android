package com.dayflow.feature.today.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dayflow.core.designsystem.theme.DayflowTheme
import com.dayflow.domain.model.FrequencyPeriod
import com.dayflow.domain.model.Habit
import com.dayflow.domain.model.ScheduleBlock
import com.dayflow.domain.model.Task
import com.dayflow.domain.usecase.HabitProgressCalculator
import java.time.LocalDate
import java.time.LocalTime
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class DayflowTab(
    val title: String,
    val icon: ImageVector
) {
    TODAY("Today", Icons.Outlined.Home),
    HABITS("Habits", Icons.Outlined.Repeat),
    PLAN("Plan", Icons.Outlined.CalendarMonth),
    INSIGHTS("Insights", Icons.Outlined.Insights)
}

@Composable
fun DayflowApp(viewModel: TodayViewModel) {
    val systemDarkTheme = isSystemInDarkTheme()
    var darkTheme by rememberSaveable { mutableStateOf(systemDarkTheme) }
    var selectedTabName by rememberSaveable { mutableStateOf(DayflowTab.TODAY.name) }
    var showTaskDialog by rememberSaveable { mutableStateOf(false) }
    var showHabitDialog by rememberSaveable { mutableStateOf(false) }
    var showScheduleDialog by rememberSaveable { mutableStateOf(false) }
    var selectedTaskForSchedule by rememberSaveable { mutableStateOf<String?>(null) }

    val selectedTab = DayflowTab.valueOf(selectedTabName)
    val tasks by viewModel.tasks.collectAsState()
    val habits by viewModel.habits.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    DayflowTheme(darkTheme = darkTheme) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                DayflowHeader(
                    title = selectedTab.title,
                    isDarkTheme = darkTheme,
                    onToggleTheme = { darkTheme = !darkTheme }
                )
            },
            bottomBar = {
                NavigationBar {
                    DayflowTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = { selectedTabName = tab.name },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title
                                )
                            },
                            label = { Text(tab.title) },
                            alwaysShowLabel = true
                        )
                    }
                }
            },
            floatingActionButton = {
                when (selectedTab) {
                    DayflowTab.TODAY -> Button(
                        onClick = { showTaskDialog = true },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Add task")
                    }

                    DayflowTab.HABITS -> Button(
                        onClick = { showHabitDialog = true },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Add habit")
                    }

                    DayflowTab.PLAN -> Button(
                        onClick = {
                            selectedTaskForSchedule = null
                            showScheduleDialog = true
                        },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Schedule task")
                    }

                    else -> Unit
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { contentPadding ->
            when (selectedTab) {
                DayflowTab.TODAY -> TodayContent(
                    tasks = tasks,
                    habits = habits,
                    viewModel = viewModel,
                    contentPadding = contentPadding,
                    onAddTask = { showTaskDialog = true },
                    onAddHabit = { showHabitDialog = true },
                    onOpenHabits = { selectedTabName = DayflowTab.HABITS.name }
                )

                DayflowTab.HABITS -> HabitsContent(
                    habits = habits,
                    viewModel = viewModel,
                    contentPadding = contentPadding,
                    onAddHabit = { showHabitDialog = true }
                )

                DayflowTab.PLAN -> PlanContent(
                    tasks = tasks,
                    viewModel = viewModel,
                    contentPadding = contentPadding,
                    onAddSchedule = {
                        selectedTaskForSchedule = null
                        showScheduleDialog = true
                    },
                    onScheduleTask = { taskId ->
                        selectedTaskForSchedule = taskId
                        showScheduleDialog = true
                    }
                )

                DayflowTab.INSIGHTS -> ComingSoonContent(
                    title = "Your progress",
                    description = "Weekly summaries, streak insights, and the completion heatmap will appear here as the tracking experience grows.",
                    contentPadding = contentPadding
                )
            }
        }
    }

    if (showTaskDialog) {
        AddTaskDialog(
            onDismiss = { showTaskDialog = false },
            onSave = { title, duration ->
                viewModel.addTask(title, duration)
                showTaskDialog = false
            }
        )
    }

    if (showHabitDialog) {
        AddHabitDialog(
            onDismiss = { showHabitDialog = false },
            onSave = { title, period, target ->
                viewModel.addHabit(title, period, target)
                showHabitDialog = false
            }
        )
    }

    if (showScheduleDialog) {
        ScheduleTaskDialog(
            tasks = tasks,
            initialTaskId = selectedTaskForSchedule,
            onDismiss = {
                showScheduleDialog = false
                selectedTaskForSchedule = null
            },
            onSave = { taskId, startTime ->
                viewModel.scheduleTask(
                    taskId = taskId,
                    date = LocalDate.now(),
                    startTime = startTime
                )
                showScheduleDialog = false
                selectedTaskForSchedule = null
            }
        )
    }
}

@Composable
private fun DayflowHeader(
    title: String,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 22.dp, end = 14.dp, top = 18.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = LocalDate.now().format(
                        DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())
                    ).uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            IconButton(onClick = onToggleTheme) {
                Icon(
                    imageVector = if (isDarkTheme) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                    contentDescription = if (isDarkTheme) "Switch to light theme" else "Switch to dark theme"
                )
            }
        }
    }
}

@Composable
private fun TodayContent(
    tasks: List<Task>,
    habits: List<Habit>,
    viewModel: TodayViewModel,
    contentPadding: PaddingValues,
    onAddTask: () -> Unit,
    onAddHabit: () -> Unit,
    onOpenHabits: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = contentPadding.calculateTopPadding() + 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 92.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            DailyOverviewCard(
                taskCount = tasks.size,
                habitCount = habits.size
            )
        }

        item {
            SectionHeader(
                title = "Tasks",
                action = "Add",
                onAction = onAddTask
            )
        }

        if (tasks.isEmpty()) {
            item {
                EmptyCard(
                    title = "Nothing on your task list",
                    description = "Add a task to capture something you want to get done."
                )
            }
        } else {
            items(tasks, key = { it.id }) { task ->
                TaskCard(
                    task = task,
                    onComplete = { viewModel.completeTask(task.id) }
                )
            }
        }

        item {
            SectionHeader(
                title = "Habits",
                action = "See all",
                onAction = onOpenHabits
            )
        }

        if (habits.isEmpty()) {
            item {
                EmptyCard(
                    title = "Start with one small habit",
                    description = "Build consistency with a daily routine or a weekly target.",
                    action = "Add habit",
                    onAction = onAddHabit
                )
            }
        } else {
            items(habits.take(3), key = { it.id }) { habit ->
                HabitCard(
                    habit = habit,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
private fun HabitsContent(
    habits: List<Habit>,
    viewModel: TodayViewModel,
    contentPadding: PaddingValues,
    onAddHabit: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = contentPadding.calculateTopPadding() + 14.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Small actions, repeated over time.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (habits.isEmpty()) {
            item {
                EmptyCard(
                    title = "No habits yet",
                    description = "Create a daily habit or set a weekly target such as exercising three times a week.",
                    action = "Create habit",
                    onAction = onAddHabit
                )
            }
        } else {
            items(habits, key = { it.id }) { habit ->
                HabitCard(
                    habit = habit,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
private fun DailyOverviewCard(
    taskCount: Int,
    habitCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Make today count.",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Your day, organized around what matters.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryMetric(
                    value = taskCount.toString(),
                    label = "Open tasks",
                    modifier = Modifier.weight(1f)
                )
                SummaryMetric(
                    value = habitCount.toString(),
                    label = "Active habits",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SummaryMetric(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(14.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    action: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        TextButton(onClick = onAction) {
            Text(action)
        }
    }
}

@Composable
private fun TaskCard(
    task: Task,
    onComplete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = false,
                onCheckedChange = { checked ->
                    if (checked) onComplete()
                }
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${task.estimatedDurationMinutes} min  ·  ${priorityLabel(task.priority)} priority",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HabitCard(
    habit: Habit,
    viewModel: TodayViewModel
) {
    val progressFlow = remember(habit.id, habit.startDateIso) {
        viewModel.observeProgress(habit)
    }
    val progress by progressFlow.collectAsState(initial = emptyList())
    val today = LocalDate.now()
    val completedToday = HabitProgressCalculator.completedCountForDay(progress, today)
    val completedThisWeek = HabitProgressCalculator.completedCountForWeek(progress, today)
    val completed = if (habit.frequencyPeriod == FrequencyPeriod.DAY) {
        completedToday
    } else {
        completedThisWeek
    }
    val progressFraction = (completed.toFloat() / habit.targetCount.coerceAtLeast(1))
        .coerceIn(0f, 1f)
    val targetMet = completed >= habit.targetCount
    val streak = HabitProgressCalculator.currentStreak(habit, progress, today)
    val periodLabel = if (habit.frequencyPeriod == FrequencyPeriod.DAY) "today" else "this week"
    val targetLabel = if (habit.frequencyPeriod == FrequencyPeriod.DAY) {
        "${habit.targetCount} time${if (habit.targetCount == 1) "" else "s"} daily"
    } else {
        "${habit.targetCount} times per week"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = habit.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = targetLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FilledTonalButton(
                    onClick = { viewModel.completeHabit(habit.id) },
                    enabled = !targetMet
                ) {
                    Text(if (targetMet) "Completed" else "Log")
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        CircleShape
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressFraction)
                        .height(6.dp)
                        .background(
                            MaterialTheme.colorScheme.primary,
                            CircleShape
                        )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$completed / ${habit.targetCount} $periodLabel",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$streak ${if (habit.frequencyPeriod == FrequencyPeriod.DAY) "day" else "week"} streak",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun EmptyCard(
    title: String,
    description: String,
    action: String? = null,
    onAction: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (action != null && onAction != null) {
                TextButton(onClick = onAction) {
                    Text(action)
                }
            }
        }
    }
}


@Composable
private fun PlanContent(
    tasks: List<Task>,
    viewModel: TodayViewModel,
    contentPadding: PaddingValues,
    onAddSchedule: () -> Unit,
    onScheduleTask: (String) -> Unit
) {
    val today = LocalDate.now()
    val timelineFlow = remember(today) { viewModel.observeTimeline(today) }
    val scheduleBlocks by timelineFlow.collectAsState(initial = emptyList())
    val scheduledTaskIds = remember(scheduleBlocks) {
        scheduleBlocks.mapNotNull { it.taskId }.toSet()
    }
    val unscheduledTasks = remember(tasks, scheduledTaskIds) {
        tasks.filter { it.id !in scheduledTaskIds }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = contentPadding.calculateTopPadding() + 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 92.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "A clear view of what happens next.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            SectionHeader(
                title = "Today's timeline",
                action = "Add block",
                onAction = onAddSchedule
            )
        }

        if (scheduleBlocks.isEmpty()) {
            item {
                EmptyCard(
                    title = "Your timeline is open",
                    description = if (tasks.isEmpty()) {
                        "Create a task first, then place it into a time block."
                    } else {
                        "Choose one of your open tasks and give it a start time."
                    }
                )
            }
        } else {
            items(scheduleBlocks, key = { it.id }) { block ->
                ScheduleBlockCard(block = block)
            }
        }

        item {
            Text(
                text = "Unscheduled tasks",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (unscheduledTasks.isEmpty()) {
            item {
                EmptyCard(
                    title = "Everything is on your timeline",
                    description = "Add another task from Today when something new comes up."
                )
            }
        } else {
            items(unscheduledTasks, key = { "unscheduled-" + it.id }) { task ->
                UnscheduledTaskCard(
                    task = task,
                    onSchedule = { onScheduleTask(task.id) }
                )
            }
        }
    }
}

@Composable
private fun ScheduleBlockCard(block: ScheduleBlock) {
    val zone = ZoneId.of(block.timeZoneId)
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
    val start = Instant.ofEpochMilli(block.startAtEpochMillis)
        .atZone(zone)
        .format(timeFormatter)
    val end = Instant.ofEpochMilli(block.endAtEpochMillis)
        .atZone(zone)
        .format(timeFormatter)
    val status = block.status.name
        .lowercase(Locale.getDefault())
        .replaceFirstChar { it.titlecase(Locale.getDefault()) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.padding(top = 12.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = start,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = end,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 4.dp, height = 42.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = block.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = block.blockType.name.lowercase(Locale.getDefault()) + " · " + status,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun UnscheduledTaskCard(
    task: Task,
    onSchedule: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = task.estimatedDurationMinutes.toString() + " min · Priority " + task.priority,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledTonalButton(onClick = onSchedule) {
                Text("Schedule")
            }
        }
    }
}

@Composable
private fun ScheduleTaskDialog(
    tasks: List<Task>,
    initialTaskId: String?,
    onDismiss: () -> Unit,
    onSave: (String, LocalTime) -> Unit
) {
    var selectedTaskId by rememberSaveable { mutableStateOf(initialTaskId.orEmpty()) }
    var startTimeText by rememberSaveable {
        mutableStateOf(
            LocalTime.now()
                .plusMinutes(15)
                .withSecond(0)
                .withNano(0)
                .format(DateTimeFormatter.ofPattern("HH:mm"))
        )
    }
    var expanded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(tasks, initialTaskId) {
        if (tasks.none { it.id == selectedTaskId }) {
            selectedTaskId = initialTaskId
                ?.takeIf { id -> tasks.any { it.id == id } }
                ?: tasks.firstOrNull()?.id.orEmpty()
        }
    }

    val selectedTask = tasks.firstOrNull { it.id == selectedTaskId }
    val parsedStartTime = runCatching {
        LocalTime.parse(startTimeText.trim())
    }.getOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schedule a task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (tasks.isEmpty()) {
                    Text(
                        text = "There are no open tasks to schedule. Create a task from the Today screen first.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Task",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Box {
                        OutlinedButton(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = selectedTask?.title ?: "Select a task",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            tasks.forEach { task ->
                                DropdownMenuItem(
                                    text = { Text(task.title) },
                                    onClick = {
                                        selectedTaskId = task.id
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = startTimeText,
                        onValueChange = { startTimeText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Start time") },
                        placeholder = { Text("09:30") },
                        supportingText = { Text("Use 24-hour time, for example 09:30.") },
                        singleLine = true,
                        isError = startTimeText.isNotBlank() && parsedStartTime == null
                    )

                    selectedTask?.let { task ->
                        Text(
                            text = "This block will last " + task.estimatedDurationMinutes + " minutes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val task = selectedTask
                    val time = parsedStartTime
                    if (task != null && time != null) {
                        onSave(task.id, time)
                    }
                },
                enabled = selectedTask != null && parsedStartTime != null
            ) {
                Text("Add to timeline")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ComingSoonContent(
    title: String,
    description: String,
    contentPadding: PaddingValues
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 20.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "This feature is planned and is not available yet.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun AddTaskDialog(
    onDismiss: () -> Unit,
    onSave: (String, Int) -> Unit
) {
    var title by rememberSaveable { mutableStateOf("") }
    var duration by rememberSaveable { mutableStateOf("30") }
    val parsedDuration = duration.toIntOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("What needs to get done?") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = duration,
                    onValueChange = { value ->
                        if (value.all(Char::isDigit) && value.length <= 4) {
                            duration = value
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Estimated minutes") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title.trim(), parsedDuration ?: 30) },
                enabled = title.isNotBlank() && parsedDuration != null && parsedDuration in 1..1440
            ) {
                Text("Save task")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun AddHabitDialog(
    onDismiss: () -> Unit,
    onSave: (String, FrequencyPeriod, Int) -> Unit
) {
    var title by rememberSaveable { mutableStateOf("") }
    var period by rememberSaveable { mutableStateOf(FrequencyPeriod.DAY) }
    var target by rememberSaveable { mutableStateOf("1") }
    val parsedTarget = target.toIntOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New habit") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Habit name") },
                    singleLine = true
                )

                Text(
                    text = "Frequency",
                    style = MaterialTheme.typography.labelLarge
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = period == FrequencyPeriod.DAY,
                        onClick = {
                            period = FrequencyPeriod.DAY
                            target = "1"
                        }
                    )
                    Text("Daily")
                    Spacer(Modifier.size(12.dp))
                    RadioButton(
                        selected = period == FrequencyPeriod.WEEK,
                        onClick = {
                            period = FrequencyPeriod.WEEK
                            target = "3"
                        }
                    )
                    Text("Weekly")
                }

                OutlinedTextField(
                    value = target,
                    onValueChange = { value ->
                        if (value.all(Char::isDigit) && value.length <= 2) {
                            target = value
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(
                            if (period == FrequencyPeriod.DAY) "Times per day" else "Times per week"
                        )
                    },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title.trim(), period, parsedTarget ?: 1) },
                enabled = title.isNotBlank() && parsedTarget != null && parsedTarget in 1..20
            ) {
                Text("Save habit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun priorityLabel(priority: Int): String = when (priority) {
    1 -> "Low"
    2 -> "Normal"
    3 -> "High"
    4 -> "Urgent"
    else -> "Normal"
}
