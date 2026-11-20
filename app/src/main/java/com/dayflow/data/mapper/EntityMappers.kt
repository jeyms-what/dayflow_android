package com.dayflow.data.mapper

import com.dayflow.core.database.entity.*
import com.dayflow.domain.model.*

private inline fun <reified T : Enum<T>> enumOrDefault(
    value: String,
    default: T
): T = runCatching { enumValueOf<T>(value) }.getOrDefault(default)

fun TaskEntity.toDomain() = Task(
    id = id,
    title = title,
    description = description,
    estimatedDurationMinutes = estimatedDurationMinutes,
    priority = priority,
    energyLevel = enumOrDefault(energyLevel, EnergyLevel.MEDIUM),
    deadlineAtEpochMillis = deadlineAtEpochMillis,
    status = enumOrDefault(status, TaskStatus.OPEN),
    completedAtEpochMillis = completedAtEpochMillis,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis
)

fun Task.toEntity(existingCreatedAt: Long = createdAtEpochMillis) = TaskEntity(
    id = id,
    title = title,
    description = description,
    estimatedDurationMinutes = estimatedDurationMinutes,
    priority = priority,
    energyLevel = energyLevel.name,
    deadlineAtEpochMillis = deadlineAtEpochMillis,
    status = status.name,
    completedAtEpochMillis = completedAtEpochMillis,
    createdAtEpochMillis = existingCreatedAt,
    updatedAtEpochMillis = updatedAtEpochMillis
)

fun HabitEntity.toDomain() = Habit(
    id = id,
    title = title,
    description = description,
    frequencyPeriod = enumOrDefault(frequencyPeriod, FrequencyPeriod.DAY),
    targetCount = targetCount,
    selectedDaysMask = selectedDaysMask,
    startDateIso = startDateIso,
    endDateIso = endDateIso,
    preferredStartMinuteOfDay = preferredStartMinuteOfDay,
    durationMinutes = durationMinutes,
    energyLevel = enumOrDefault(energyLevel, EnergyLevel.MEDIUM),
    reminderEnabled = reminderEnabled,
    isArchived = isArchived,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis
)

fun Habit.toEntity(existingCreatedAt: Long = createdAtEpochMillis) = HabitEntity(
    id = id,
    title = title,
    description = description,
    frequencyPeriod = frequencyPeriod.name,
    targetCount = targetCount,
    selectedDaysMask = selectedDaysMask,
    startDateIso = startDateIso,
    endDateIso = endDateIso,
    preferredStartMinuteOfDay = preferredStartMinuteOfDay,
    durationMinutes = durationMinutes,
    energyLevel = energyLevel.name,
    reminderEnabled = reminderEnabled,
    isArchived = isArchived,
    createdAtEpochMillis = existingCreatedAt,
    updatedAtEpochMillis = updatedAtEpochMillis
)

fun HabitDailyProgressEntity.toDomain() = HabitProgress(
    habitId = habitId,
    localDateIso = localDateIso,
    timeZoneId = timeZoneId,
    completionCount = completionCount,
    targetCountAtTime = targetCountAtTime,
    frequencyPeriodAtTime = enumOrDefault(
        frequencyPeriodAtTime,
        FrequencyPeriod.DAY
    ),
    updatedAtEpochMillis = updatedAtEpochMillis
)

fun ScheduleBlockEntity.toDomain() = ScheduleBlock(
    id = id,
    blockType = enumOrDefault(blockType, ScheduleBlockType.TASK),
    taskId = taskId,
    habitId = habitId,
    title = titleSnapshot,
    startAtEpochMillis = startAtEpochMillis,
    endAtEpochMillis = endAtEpochMillis,
    timeZoneId = timeZoneId,
    status = enumOrDefault(status, ScheduleBlockStatus.PLANNED),
    source = enumOrDefault(source, ScheduleSource.MANUAL),
    isMovable = isMovable,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis
)

fun ScheduleBlock.toEntity() = ScheduleBlockEntity(
    id = id,
    blockType = blockType.name,
    taskId = taskId,
    habitId = habitId,
    titleSnapshot = title,
    startAtEpochMillis = startAtEpochMillis,
    endAtEpochMillis = endAtEpochMillis,
    timeZoneId = timeZoneId,
    status = status.name,
    source = source.name,
    isMovable = isMovable,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis
)

fun ReminderEntity.toDomain() = TaskReminder(
    id = id,
    scheduleBlockId = scheduleBlockId,
    triggerAtEpochMillis = triggerAtEpochMillis,
    status = enumOrDefault(status, ReminderStatus.SCHEDULED),
    snoozeUntilEpochMillis = snoozeUntilEpochMillis,
    soundUri = soundUri,
    vibrationEnabled = vibrationEnabled,
    snoozeMinutes = snoozeMinutes,
    maxRingDurationSeconds = maxRingDurationSeconds,
    requestCode = requestCode,
    lastTriggeredAtEpochMillis = lastTriggeredAtEpochMillis,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis
)

fun TaskReminder.toEntity() = ReminderEntity(
    id = id,
    scheduleBlockId = scheduleBlockId,
    triggerAtEpochMillis = triggerAtEpochMillis,
    status = status.name,
    snoozeUntilEpochMillis = snoozeUntilEpochMillis,
    soundUri = soundUri,
    vibrationEnabled = vibrationEnabled,
    snoozeMinutes = snoozeMinutes,
    maxRingDurationSeconds = maxRingDurationSeconds,
    requestCode = requestCode,
    lastTriggeredAtEpochMillis = lastTriggeredAtEpochMillis,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis
)

fun AiScheduleProposalEntity.toDomain(
    blocks: List<AiProposalBlockEntity>
) = ScheduleProposal(
    id = id,
    engine = engine,
    modelId = modelId,
    status = enumOrDefault(status, ProposalStatus.DRAFT),
    summary = summary,
    inputSnapshotJson = inputSnapshotJson,
    baseDataFingerprint = baseDataFingerprint,
    promptVersion = promptVersion,
    blocks = blocks.map {
        ProposedScheduleBlock(
            id = it.id,
            blockType = enumOrDefault(it.blockType, ScheduleBlockType.TASK),
            taskId = it.taskId,
            habitId = it.habitId,
            title = it.titleSnapshot,
            startAtEpochMillis = it.startAtEpochMillis,
            endAtEpochMillis = it.endAtEpochMillis,
            timeZoneId = it.timeZoneId,
            rationale = it.rationale,
            confidence = it.confidence,
            position = it.position
        )
    },
    createdAtEpochMillis = createdAtEpochMillis,
    appliedAtEpochMillis = appliedAtEpochMillis
)

fun ScheduleProposal.toEntity() = AiScheduleProposalEntity(
    id = id,
    engine = engine,
    modelId = modelId,
    status = status.name,
    summary = summary,
    inputSnapshotJson = inputSnapshotJson,
    baseDataFingerprint = baseDataFingerprint,
    promptVersion = promptVersion,
    createdAtEpochMillis = createdAtEpochMillis,
    appliedAtEpochMillis = appliedAtEpochMillis
)

fun ProposedScheduleBlock.toEntity(proposalId: String) = AiProposalBlockEntity(
    id = id,
    proposalId = proposalId,
    blockType = blockType.name,
    taskId = taskId,
    habitId = habitId,
    titleSnapshot = title,
    startAtEpochMillis = startAtEpochMillis,
    endAtEpochMillis = endAtEpochMillis,
    timeZoneId = timeZoneId,
    rationale = rationale,
    confidence = confidence,
    position = position
)
