package com.dayflow.feature.today.presentation

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.ui.graphics.Color
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

private data class PendingTaskSchedule(
    val taskId: String,
    val startTime: LocalTime,
    val reminderEnabled: Boolean
)

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
    // Dayflow opens in its intended night palette; the user can still switch themes.
    var darkTheme by rememberSaveable { mutableStateOf(true) }
    var selectedTabName by rememberSaveable { mutableStateOf(DayflowTab.TODAY.name) }
    var showTaskDialog by rememberSaveable { mutableStateOf(false) }
    var showHabitDialog by rememberSaveable { mutableStateOf(false) }
    var showScheduleDialog by rememberSaveable { mutableStateOf(false) }
    var selectedTaskForSchedule by rememberSaveable { mutableStateOf<String?>(null) }
    var showScheduleHabitDialog by rememberSaveable { mutableStateOf(false) }
    var selectedHabitForSchedule by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingTaskSchedule by remember { mutableStateOf<PendingTaskSchedule?>(null) }
    val context = LocalContext.current

    fun finishPendingTaskSchedule() {
        val request = pendingTaskSchedule ?: return
        viewModel.scheduleTask(
            taskId = request.taskId,
            date = LocalDate.now(),
            startTime = request.startTime,
            reminderEnabled = request.reminderEnabled
        )
        pendingTaskSchedule = null
    }

    val exactAlarmSettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Keep the reminder even when the user declines exact-alarm access.
        // The scheduler will then use Android's inexact idle-allowed fallback.
        finishPendingTaskSchedule()
    }

    fun continueAfterNotificationPermission() {
        val request = pendingTaskSchedule ?: return
        if (
            request.reminderEnabled &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            (context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() != true)
        ) {
            val settingsIntent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            runCatching {
                exactAlarmSettingsLauncher.launch(settingsIntent)
            }.onFailure {
                finishPendingTaskSchedule()
            }
        } else {
            finishPendingTaskSchedule()
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            continueAfterNotificationPermission()
        } else {
            pendingTaskSchedule?.let { request ->
                viewModel.scheduleTask(
                    taskId = request.taskId,
                    date = LocalDate.now(),
                    startTime = request.startTime,
                    reminderEnabled = false
                )
            }
            pendingTaskSchedule = null
        }
    }

    fun beginTaskSchedule(
        taskId: String,
        startTime: LocalTime,
        reminderEnabled: Boolean
    ) {
        pendingTaskSchedule = PendingTaskSchedule(taskId, startTime, reminderEnabled)
        if (
            reminderEnabled &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            continueAfterNotificationPermission()
        }
    }

    val selectedTab = DayflowTab.valueOf(selectedTabName)
    val tasks by viewModel.tasks.collectAsState()
    val unscheduledTasks by viewModel.unscheduledTasks.collectAsState()
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
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 0.dp
                ) {
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
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
                    tasks = unscheduledTasks,
                    habits = habits,
                    viewModel = viewModel,
                    contentPadding = contentPadding,
                    onAddSchedule = {
                        selectedTaskForSchedule = null
                        showScheduleDialog = true
                    },
                    onScheduleTask = { taskId ->
                        selectedTaskForSchedule = taskId
                        showScheduleDialog = true
                    },
                    onScheduleHabit = { habitId ->
                        selectedHabitForSchedule = habitId
                        showScheduleHabitDialog = true
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
            tasks = unscheduledTasks,
            initialTaskId = selectedTaskForSchedule,
            onDismiss = {
                showScheduleDialog = false
                selectedTaskForSchedule = null
            },
            onSave = { taskId, startTime, reminderEnabled ->
                beginTaskSchedule(taskId, startTime, reminderEnabled)
                showScheduleDialog = false
                selectedTaskForSchedule = null
            }
        )
    }

    if (showScheduleHabitDialog) {
        ScheduleHabitDialog(
            habits = habits,
            initialHabitId = selectedHabitForSchedule,
            onDismiss = {
                showScheduleHabitDialog = false
                selectedHabitForSchedule = null
            },
            onSave = { habitId, startTime ->
                viewModel.scheduleHabit(
                    habitId = habitId,
                    date = LocalDate.now(),
                    startTime = startTime
                )
                showScheduleHabitDialog = false
                selectedHabitForSchedule = null
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
    val headline = when (title) {
        "Today" -> "Make space for what matters."
        "Habits" -> "Small steps add up."
        "Plan" -> "Give your day a rhythm."
        else -> "Notice how far you've come."
    }
    val dateLabel = LocalDate.now().format(
        DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())
    ).uppercase(Locale.getDefault())

    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SlimeMark()
                Column(
                    modifier = Modifier.padding(start = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        text = "Dayflow",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "PLAN · FOCUS · GROW",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.weight(1f))
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                            contentDescription = if (isDarkTheme) "Switch to light theme" else "Switch to dark theme",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "$title  ·  $dateLabel",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = headline,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

@Composable
private fun SlimeMark(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(width = 38.dp, height = 34.dp)
            .background(
                color = Color(0xFF83D9F5),
                shape = RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = 13.dp,
                    bottomEnd = 13.dp
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(3.dp)
                    .background(Color(0xFF17466B), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(3.dp)
                    .background(Color(0xFF17466B), CircleShape)
            )
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
        shape = RoundedCornerShape(26.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = "A FRESH START",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Make today count.",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Your day, organized around what matters.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.76f)
                    )
                }
                SlimeMark(Modifier.size(width = 48.dp, height = 43.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.07f),
                shape = RoundedCornerShape(15.dp)
            )
            .padding(horizontal = 13.dp, vertical = 11.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.76f)
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(width = 3.dp, height = 18.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        TextButton(onClick = onAction) {
            Text(action, fontWeight = FontWeight.SemiBold)
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
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.42f)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
    habits: List<Habit>,
    viewModel: TodayViewModel,
    contentPadding: PaddingValues,
    onAddSchedule: () -> Unit,
    onScheduleTask: (String) -> Unit,
    onScheduleHabit: (String) -> Unit
) {
    val today = LocalDate.now()
    val timelineFlow = remember(today) { viewModel.observeTimeline(today) }
    val scheduleBlocks by timelineFlow.collectAsState(initial = emptyList())

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
                ScheduleBlockCard(
                    block = block,
                    onComplete = { viewModel.completeScheduleBlock(block.id) }
                )
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

        if (tasks.isEmpty()) {
            item {
                EmptyCard(
                    title = "Everything is on your timeline",
                    description = "Add another task from Today when something new comes up."
                )
            }
        } else {
            items(tasks, key = { "unscheduled-" + it.id }) { task ->
                UnscheduledTaskCard(
                    task = task,
                    onSchedule = { onScheduleTask(task.id) }
                )
            }
        }

        item {
            SectionHeader(
                title = "Habits to plan",
                action = "Add habit block",
                onAction = { onScheduleHabit(habits.firstOrNull()?.id.orEmpty()) }
            )
        }

        if (habits.isEmpty()) {
            item {
                EmptyCard(
                    title = "No active habits",
                    description = "Create a habit first, then add a session to today's timeline."
                )
            }
        } else {
            items(habits, key = { "habit-" + it.id }) { habit ->
                UnscheduledHabitCard(
                    habit = habit,
                    onSchedule = { onScheduleHabit(habit.id) }
                )
            }
        }
    }
}

@Composable
private fun ScheduleBlockCard(
    block: ScheduleBlock,
    onComplete: () -> Unit
) {
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
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.42f)),
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
                FilledTonalButton(
                    onClick = onComplete,
                    enabled = block.status.name == "PLANNED" || block.status.name == "IN_PROGRESS"
                ) {
                    Text(if (block.status.name == "COMPLETED") "Done" else "Complete")
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
    onSave: (String, LocalTime, Boolean) -> Unit
) {
    var selectedTaskId by rememberSaveable { mutableStateOf(initialTaskId.orEmpty()) }
    var reminderEnabled by rememberSaveable { mutableStateOf(true) }
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

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = reminderEnabled,
                            onCheckedChange = { reminderEnabled = it }
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Ring at task start",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Uses an alarm sound and vibration. Android may request notification and precise-alarm access.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
                        onSave(task.id, time, reminderEnabled)
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
private fun UnscheduledHabitCard(
    habit: Habit,
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
                    text = habit.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                val frequency = if (habit.frequencyPeriod == FrequencyPeriod.DAY) {
                    "Daily · " + habit.targetCount + " time(s)"
                } else {
                    "Weekly · " + habit.targetCount + " session(s)"
                }
                Text(
                    text = habit.durationMinutes.toString() + " min · " + frequency,
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
private fun ScheduleHabitDialog(
    habits: List<Habit>,
    initialHabitId: String?,
    onDismiss: () -> Unit,
    onSave: (String, LocalTime) -> Unit
) {
    var selectedHabitId by rememberSaveable { mutableStateOf(initialHabitId.orEmpty()) }
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

    LaunchedEffect(habits, initialHabitId) {
        if (habits.none { it.id == selectedHabitId }) {
            selectedHabitId = initialHabitId
                ?.takeIf { id -> habits.any { it.id == id } }
                ?: habits.firstOrNull()?.id.orEmpty()
        }
    }

    val selectedHabit = habits.firstOrNull { it.id == selectedHabitId }
    val parsedStartTime = runCatching {
        LocalTime.parse(startTimeText.trim())
    }.getOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schedule a habit") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (habits.isEmpty()) {
                    Text(
                        text = "There are no active habits to schedule. Create a habit from the Habits screen first.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Habit",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Box {
                        OutlinedButton(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = selectedHabit?.title ?: "Select a habit",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            habits.forEach { habit ->
                                DropdownMenuItem(
                                    text = { Text(habit.title) },
                                    onClick = {
                                        selectedHabitId = habit.id
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
                        placeholder = { Text("07:30") },
                        supportingText = { Text("Use 24-hour time, for example 07:30.") },
                        singleLine = true,
                        isError = startTimeText.isNotBlank() && parsedStartTime == null
                    )

                    selectedHabit?.let { habit ->
                        Text(
                            text = "This session will last " + habit.durationMinutes + " minutes.",
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
                    val habit = selectedHabit
                    val time = parsedStartTime
                    if (habit != null && time != null) {
                        onSave(habit.id, time)
                    }
                },
                enabled = selectedHabit != null && parsedStartTime != null
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
