package com.dayflow.domain.model

enum class TaskStatus { OPEN, COMPLETED, CANCELLED }
enum class EnergyLevel { LOW, MEDIUM, HIGH }
enum class FrequencyPeriod { DAY, WEEK }
enum class ScheduleBlockType { TASK, HABIT, BREAK, EXTERNAL }
enum class ScheduleBlockStatus { PLANNED, IN_PROGRESS, COMPLETED, SKIPPED, CANCELLED }
enum class ScheduleSource { MANUAL, AI, IMPORTED }
enum class ReminderStatus { SCHEDULED, FIRING, SNOOZED, DISMISSED, COMPLETED, CANCELLED }
enum class ProposalStatus { DRAFT, APPLIED, REJECTED, EXPIRED }

data class Task(
    val id: String,
    val title: String,
    val description: String?,
    val estimatedDurationMinutes: Int,
    val priority: Int,
    val energyLevel: EnergyLevel,
    val deadlineAtEpochMillis: Long?,
    val status: TaskStatus,
    val completedAtEpochMillis: Long?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long
)

data class Habit(
    val id: String,
    val title: String,
    val description: String?,
    val frequencyPeriod: FrequencyPeriod,
    val targetCount: Int,
    val selectedDaysMask: Int,
    val startDateIso: String,
    val endDateIso: String?,
    val preferredStartMinuteOfDay: Int?,
    val durationMinutes: Int,
    val energyLevel: EnergyLevel,
    val reminderEnabled: Boolean,
    val isArchived: Boolean,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long
)

data class HabitProgress(
    val habitId: String,
    val localDateIso: String,
    val timeZoneId: String,
    val completionCount: Int,
    val targetCountAtTime: Int,
    val frequencyPeriodAtTime: FrequencyPeriod,
    val updatedAtEpochMillis: Long
)

data class ScheduleBlock(
    val id: String,
    val blockType: ScheduleBlockType,
    val taskId: String?,
    val habitId: String?,
    val title: String,
    val startAtEpochMillis: Long,
    val endAtEpochMillis: Long,
    val timeZoneId: String,
    val status: ScheduleBlockStatus,
    val source: ScheduleSource,
    val isMovable: Boolean,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long
)

data class TaskReminder(
    val id: String,
    val scheduleBlockId: String,
    val triggerAtEpochMillis: Long,
    val status: ReminderStatus,
    val snoozeUntilEpochMillis: Long?,
    val soundUri: String?,
    val vibrationEnabled: Boolean,
    val snoozeMinutes: Int,
    val maxRingDurationSeconds: Int,
    val requestCode: Int,
    val lastTriggeredAtEpochMillis: Long?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long
)

data class ProposedScheduleBlock(
    val id: String,
    val blockType: ScheduleBlockType,
    val taskId: String?,
    val habitId: String?,
    val title: String,
    val startAtEpochMillis: Long,
    val endAtEpochMillis: Long,
    val timeZoneId: String,
    val rationale: String,
    val confidence: Float? = null,
    val position: Int
)

data class ScheduleProposal(
    val id: String,
    val engine: String,
    val modelId: String?,
    val status: ProposalStatus,
    val summary: String,
    val inputSnapshotJson: String,
    val baseDataFingerprint: String,
    val promptVersion: String?,
    val blocks: List<ProposedScheduleBlock>,
    val createdAtEpochMillis: Long,
    val appliedAtEpochMillis: Long?
)
