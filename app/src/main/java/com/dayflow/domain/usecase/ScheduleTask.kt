package com.dayflow.domain.usecase

import com.dayflow.domain.model.ScheduleBlock
import com.dayflow.domain.model.ScheduleBlockStatus
import com.dayflow.domain.model.ScheduleBlockType
import com.dayflow.domain.model.ScheduleSource
import com.dayflow.domain.model.Task
import com.dayflow.domain.model.TaskStatus
import com.dayflow.domain.repository.ScheduleRepository
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

class ScheduleTask(
    private val repository: ScheduleRepository,
    private val now: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() }
) {
    fun createBlock(
        task: Task,
        date: LocalDate,
        startTime: LocalTime,
        timeZoneId: String
    ): ScheduleBlock {
        require(task.status == TaskStatus.OPEN) {
            "Only open tasks can be scheduled."
        }
        require(task.id.isNotBlank()) { "Task ID cannot be blank." }
        require(task.title.isNotBlank()) { "Task title cannot be blank." }
        require(task.estimatedDurationMinutes in 1..1440) {
            "Task duration must be between 1 and 1440 minutes."
        }

        val zone = ZoneId.of(timeZoneId)
        val start = date.atTime(startTime).atZone(zone).toInstant().toEpochMilli()
        val end = date.atTime(startTime)
            .plusMinutes(task.estimatedDurationMinutes.toLong())
            .atZone(zone)
            .toInstant()
            .toEpochMilli()

        require(end > start) { "Scheduled task must have a positive duration." }

        val timestamp = now()
        return ScheduleBlock(
            id = newId(),
            blockType = ScheduleBlockType.TASK,
            taskId = task.id,
            habitId = null,
            title = task.title,
            startAtEpochMillis = start,
            endAtEpochMillis = end,
            timeZoneId = zone.id,
            status = ScheduleBlockStatus.PLANNED,
            source = ScheduleSource.MANUAL,
            isMovable = true,
            createdAtEpochMillis = timestamp,
            updatedAtEpochMillis = timestamp
        )
    }

    suspend operator fun invoke(
        task: Task,
        date: LocalDate,
        startTime: LocalTime,
        timeZoneId: String
    ): ScheduleBlock {
        val block = createBlock(task, date, startTime, timeZoneId)
        repository.saveBlock(block)
        return block
    }
}
