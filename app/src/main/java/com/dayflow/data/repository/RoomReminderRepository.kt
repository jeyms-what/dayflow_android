package com.dayflow.data.repository

import javax.inject.Inject
import androidx.room.withTransaction
import com.dayflow.core.database.DayflowDatabase
import com.dayflow.data.mapper.toDomain
import com.dayflow.data.mapper.toEntity
import com.dayflow.domain.model.ReminderStatus
import com.dayflow.domain.model.ScheduleBlockStatus
import com.dayflow.domain.model.ScheduleBlockType
import com.dayflow.domain.model.TaskStatus
import com.dayflow.domain.repository.ReminderRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomReminderRepository @Inject constructor(
    private val database: DayflowDatabase
) : ReminderRepository {

    private val reminderDao = database.reminderDao()

    override fun observePendingReminders() =
        reminderDao.observePendingReminders().map { rows -> rows.map { it.toDomain() } }

    override suspend fun getReminder(reminderId: String) =
        reminderDao.getById(reminderId)?.toDomain()

    override suspend fun getReminderForBlock(blockId: String) =
        reminderDao.getByBlockId(blockId)?.toDomain()

    override suspend fun markFiring(
        reminderId: String,
        triggeredAtEpochMillis: Long
    ): Boolean {
        require(triggeredAtEpochMillis > 0L)
        return reminderDao.markFiring(reminderId, triggeredAtEpochMillis) == 1
    }

    override suspend fun saveReminder(reminder: com.dayflow.domain.model.TaskReminder) {
        require(reminder.id.isNotBlank() && reminder.scheduleBlockId.isNotBlank())
        require(reminder.triggerAtEpochMillis > 0L)
        require(reminder.snoozeMinutes > 0)
        require(reminder.maxRingDurationSeconds > 0)

        val block = database.scheduleDao().getById(reminder.scheduleBlockId)
            ?: throw IllegalArgumentException("Cannot remind for a missing schedule block.")

        require(block.status !in listOf(
            ScheduleBlockStatus.COMPLETED.name,
            ScheduleBlockStatus.CANCELLED.name,
            ScheduleBlockStatus.SKIPPED.name
        )) { "Cannot create a reminder for an inactive schedule block." }

        reminderDao.upsert(reminder.toEntity())
    }

    override suspend fun snoozeReminder(
        reminderId: String,
        newTriggerAt: Long
    ) {
        require(newTriggerAt > System.currentTimeMillis()) {
            "Snooze time must be in the future."
        }
        val current = reminderDao.getById(reminderId)
            ?: throw IllegalArgumentException("Reminder was not found.")
        check(current.status == ReminderStatus.FIRING.name ||
            current.status == ReminderStatus.SCHEDULED.name ||
            current.status == ReminderStatus.SNOOZED.name) {
            "This reminder can no longer be snoozed."
        }

        val now = System.currentTimeMillis()
        val updated = reminderDao.updateReminderState(
            id = reminderId,
            status = ReminderStatus.SNOOZED.name,
            triggerAt = newTriggerAt,
            snoozeUntil = newTriggerAt,
            updatedAt = now
        )
        check(updated == 1) { "Reminder could not be snoozed." }
    }

    override suspend fun dismissReminder(reminderId: String) {
        val current = reminderDao.getById(reminderId)
            ?: throw IllegalArgumentException("Reminder was not found.")
        check(current.status in listOf(
            ReminderStatus.FIRING.name,
            ReminderStatus.SCHEDULED.name,
            ReminderStatus.SNOOZED.name
        )) { "This reminder can no longer be dismissed." }
        val updated = reminderDao.updateReminderState(
            id = reminderId,
            status = ReminderStatus.DISMISSED.name,
            triggerAt = current.triggerAtEpochMillis,
            snoozeUntil = null,
            updatedAt = System.currentTimeMillis()
        )
        check(updated == 1) { "Reminder could not be dismissed." }
    }

    override suspend fun completeReminder(reminderId: String) {
        database.withTransaction {
            val reminder = reminderDao.getById(reminderId)
                ?: throw IllegalArgumentException("Reminder was not found.")
            check(reminder.status in listOf(
                ReminderStatus.FIRING.name,
                ReminderStatus.SCHEDULED.name,
                ReminderStatus.SNOOZED.name
            )) { "This reminder has already been handled." }

            val block = database.scheduleDao().getById(reminder.scheduleBlockId)
                ?: throw IllegalArgumentException("Scheduled block was not found.")
            check(
                block.status == ScheduleBlockStatus.PLANNED.name ||
                    block.status == ScheduleBlockStatus.IN_PROGRESS.name
            ) { "This scheduled activity has already been completed." }

            val now = System.currentTimeMillis()
            val reminderUpdated = reminderDao.updateReminderState(
                id = reminderId,
                status = ReminderStatus.COMPLETED.name,
                triggerAt = reminder.triggerAtEpochMillis,
                snoozeUntil = null,
                updatedAt = now
            )
            check(reminderUpdated == 1) { "Reminder could not be completed." }

            val blockUpdated = database.scheduleDao().updateStatus(
                blockId = block.id,
                status = ScheduleBlockStatus.COMPLETED.name,
                updatedAt = now
            )
            check(blockUpdated == 1) { "Scheduled block could not be completed." }

            when (block.blockType) {
                ScheduleBlockType.TASK.name -> {
                    val taskId = block.taskId
                        ?: throw IllegalStateException("Task block is missing its task ID.")
                    val taskUpdated = database.taskDao().updateStatus(
                        taskId = taskId,
                        status = TaskStatus.COMPLETED.name,
                        completedAt = now,
                        updatedAt = now
                    )
                    check(taskUpdated == 1) { "Task was already removed or completed." }
                }
                ScheduleBlockType.HABIT.name -> {
                    val habitId = block.habitId
                        ?: throw IllegalStateException("Habit block is missing its habit ID.")
                    val habit = database.habitDao().getById(habitId)
                        ?: throw IllegalArgumentException("Habit was not found.")
                    check(!habit.isArchived) { "Habit is archived." }

                    val zone = ZoneId.of(block.timeZoneId)
                    val date = Instant.ofEpochMilli(block.startAtEpochMillis)
                        .atZone(zone)
                        .toLocalDate()
                    requireHabitDateIsEligible(habit, date)

                    database.habitDao().incrementDailyProgress(
                        habitId = habitId,
                        localDateIso = date.toString(),
                        timeZoneId = block.timeZoneId,
                        targetCount = habit.targetCount,
                        frequencyPeriod = habit.frequencyPeriod,
                        updatedAt = now
                    )
                }
            }
        }
    }

    private fun requireHabitDateIsEligible(
        habit: com.dayflow.core.database.entity.HabitEntity,
        date: LocalDate
    ) {
        require(!date.isBefore(LocalDate.parse(habit.startDateIso))) {
            "Habit had not started when this block was scheduled."
        }
        require(habit.endDateIso == null ||
            !date.isAfter(LocalDate.parse(habit.endDateIso))) {
            "Habit had already ended when this block was scheduled."
        }
        if (habit.selectedDaysMask != 0) {
            val dayBit = 1 shl (date.dayOfWeek.value - 1)
            require(habit.selectedDaysMask and dayBit != 0) {
                "Habit is not enabled for this weekday."
            }
        }
    }
}
