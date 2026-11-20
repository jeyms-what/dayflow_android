package com.dayflow.data.repository

import javax.inject.Inject
import androidx.room.withTransaction
import com.dayflow.core.database.DayflowDatabase
import com.dayflow.core.database.entity.ScheduleBlockEntity
import com.dayflow.data.mapper.toDomain
import com.dayflow.data.mapper.toEntity
import com.dayflow.domain.model.*
import com.dayflow.domain.repository.ScheduleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomScheduleRepository @Inject constructor(
    private val database: DayflowDatabase
) : ScheduleRepository {

    private val scheduleDao = database.scheduleDao()

    override fun observeTimeline(
        startAt: Long,
        endAt: Long
    ): Flow<List<ScheduleBlock>> {
        require(endAt > startAt) { "Timeline range must have a positive duration." }
        return scheduleDao.observeBetween(startAt, endAt)
            .map { rows -> rows.map { it.toDomain() } }
    }

    override suspend fun getBlock(id: String): ScheduleBlock? =
        scheduleDao.getById(id)?.toDomain()

    override suspend fun saveBlock(block: ScheduleBlock) {
        validateBlock(block)
        database.withTransaction {
            require(scheduleDao.getById(block.id) == null) {
                "Schedule block ID already exists."
            }
            require(
                !scheduleDao.hasOverlap(
                    startAt = block.startAtEpochMillis,
                    endAt = block.endAtEpochMillis,
                    excludeBlockId = null
                )
            ) { "The schedule block overlaps an existing block." }
            scheduleDao.upsert(block.toEntity())
        }
    }

    override suspend fun hasConflict(
        startAt: Long,
        endAt: Long,
        excludeBlockId: String?
    ): Boolean {
        require(endAt > startAt) { "Block end must be after its start." }
        return scheduleDao.hasOverlap(startAt, endAt, excludeBlockId)
    }

    override suspend fun completeBlock(
        blockId: String,
        completedAtEpochMillis: Long
    ): ScheduleBlock = database.withTransaction {
        require(blockId.isNotBlank()) { "Schedule block ID cannot be blank." }
        require(completedAtEpochMillis > 0L) { "Completion timestamp must be positive." }

        val block = scheduleDao.getById(blockId)
            ?: throw IllegalArgumentException("Schedule block was not found.")

        check(
            block.status == ScheduleBlockStatus.PLANNED.name ||
                block.status == ScheduleBlockStatus.IN_PROGRESS.name
        ) { "Only planned or in-progress blocks can be completed." }

        when (block.blockType) {
            ScheduleBlockType.TASK.name -> {
                require(!block.taskId.isNullOrBlank() && block.habitId == null) {
                    "Task blocks must reference exactly one task."
                }
                val task = database.taskDao().getById(block.taskId)
                    ?: throw IllegalArgumentException("Linked task was not found.")
                require(task.deletedAtEpochMillis == null) {
                    "The linked task has been deleted."
                }
                check(task.status == TaskStatus.OPEN.name) {
                    "The linked task is no longer open."
                }
                val updatedTask = database.taskDao().updateStatus(
                    taskId = task.id,
                    status = TaskStatus.COMPLETED.name,
                    completedAt = completedAtEpochMillis,
                    updatedAt = completedAtEpochMillis
                )
                check(updatedTask == 1) { "The task could not be completed." }
            }

            ScheduleBlockType.HABIT.name -> {
                require(!block.habitId.isNullOrBlank() && block.taskId == null) {
                    "Habit blocks must reference exactly one habit."
                }
                val habit = database.habitDao().getById(block.habitId)
                    ?: throw IllegalArgumentException("Linked habit was not found.")
                check(!habit.isArchived) { "The linked habit is archived." }

                // A scheduled habit session belongs to the date on which it was planned.
                val scheduledDate = java.time.Instant
                    .ofEpochMilli(block.startAtEpochMillis)
                    .atZone(java.time.ZoneId.of(block.timeZoneId))
                    .toLocalDate()

                require(!scheduledDate.isBefore(java.time.LocalDate.parse(habit.startDateIso))) {
                    "The habit was not active on the scheduled date."
                }
                val endDate = habit.endDateIso?.let(java.time.LocalDate::parse)
                require(endDate == null || !scheduledDate.isAfter(endDate)) {
                    "The habit was not active on the scheduled date."
                }

                database.habitDao().incrementDailyProgress(
                    habitId = habit.id,
                    localDateIso = scheduledDate.toString(),
                    timeZoneId = block.timeZoneId,
                    targetCount = habit.targetCount,
                    frequencyPeriod = habit.frequencyPeriod,
                    updatedAt = completedAtEpochMillis
                )
            }

            ScheduleBlockType.BREAK.name,
            ScheduleBlockType.EXTERNAL.name -> Unit

            else -> throw IllegalStateException("Unsupported schedule block type.")
        }

        val updatedBlockCount = scheduleDao.updateStatus(
            blockId = blockId,
            status = ScheduleBlockStatus.COMPLETED.name,
            updatedAt = completedAtEpochMillis
        )
        check(updatedBlockCount == 1) { "Schedule block could not be completed." }

        // Prevent a linked alarm from remaining pending after the activity is complete.
        database.reminderDao().getByBlockId(blockId)?.let { reminder ->
            database.reminderDao().updateReminderState(
                id = reminder.id,
                status = ReminderStatus.COMPLETED.name,
                triggerAt = completedAtEpochMillis,
                snoozeUntil = null,
                updatedAt = completedAtEpochMillis
            )
        }

        (scheduleDao.getById(blockId)
            ?: error("Completed schedule block could not be reloaded.")).toDomain()
    }

    /**
     * Adds a draft proposal's blocks to currently free time slots.
     *
     * This first implementation deliberately does not move or delete existing
     * blocks. Rescheduling/replacement needs an explicit list of movable block IDs
     * and will be added with the scheduling engine.
     */
    override suspend fun applyProposal(proposalId: String) {
        database.withTransaction {
            val proposal = database.aiProposalDao().getProposal(proposalId)
                ?: throw IllegalArgumentException("Schedule proposal was not found.")

            check(proposal.status == ProposalStatus.DRAFT.name) {
                "Only draft proposals can be applied."
            }

            val proposedBlocks = database.aiProposalDao().getProposalBlocks(proposalId)
                .sortedBy { it.position }

            check(proposedBlocks.isNotEmpty()) {
                "Cannot apply an empty schedule proposal."
            }

            proposedBlocks.forEach { proposed ->
                require(proposed.endAtEpochMillis > proposed.startAtEpochMillis) {
                    "Proposed block must have a positive duration."
                }
                require(proposed.titleSnapshot.isNotBlank()) {
                    "Proposed block title cannot be blank."
                }
                java.time.ZoneId.of(proposed.timeZoneId)

                when (proposed.blockType) {
                    ScheduleBlockType.TASK.name -> {
                        require(!proposed.taskId.isNullOrBlank() && proposed.habitId == null) {
                            "Task blocks must reference exactly one task."
                        }
                        val task = database.taskDao().getById(proposed.taskId)
                            ?: throw IllegalArgumentException("Proposed task no longer exists.")
                        require(task.deletedAtEpochMillis == null && task.status == TaskStatus.OPEN.name) {
                            "Proposed task is no longer available."
                        }
                    }
                    ScheduleBlockType.HABIT.name -> {
                        require(!proposed.habitId.isNullOrBlank() && proposed.taskId == null) {
                            "Habit blocks must reference exactly one habit."
                        }
                        val habit = database.habitDao().getById(proposed.habitId)
                            ?: throw IllegalArgumentException("Proposed habit no longer exists.")
                        require(!habit.isArchived) { "Proposed habit is archived." }
                    }
                    ScheduleBlockType.BREAK.name -> {
                        require(proposed.taskId == null && proposed.habitId == null) {
                            "Break blocks cannot reference tasks or habits."
                        }
                    }
                    else -> throw IllegalArgumentException(
                        "Unsupported AI proposal block type: ${proposed.blockType}"
                    )
                }

                require(scheduleDao.getById(proposed.id) == null) {
                    "A proposed schedule block ID already exists."
                }
                require(
                    !scheduleDao.hasOverlap(
                        startAt = proposed.startAtEpochMillis,
                        endAt = proposed.endAtEpochMillis,
                        excludeBlockId = null
                    )
                ) { "A proposed block overlaps an existing or previously applied block." }

                val now = System.currentTimeMillis()
                scheduleDao.upsert(
                    ScheduleBlockEntity(
                        id = proposed.id,
                        blockType = proposed.blockType,
                        taskId = proposed.taskId,
                        habitId = proposed.habitId,
                        titleSnapshot = proposed.titleSnapshot,
                        startAtEpochMillis = proposed.startAtEpochMillis,
                        endAtEpochMillis = proposed.endAtEpochMillis,
                        timeZoneId = proposed.timeZoneId,
                        status = ScheduleBlockStatus.PLANNED.name,
                        source = ScheduleSource.AI.name,
                        isMovable = true,
                        createdAtEpochMillis = now,
                        updatedAtEpochMillis = now
                    )
                )
            }

            val updated = database.aiProposalDao().updateProposalStatus(
                proposalId = proposalId,
                status = ProposalStatus.APPLIED.name,
                appliedAt = System.currentTimeMillis()
            )
            check(updated == 1) { "Proposal state changed before it could be applied." }
        }
    }

    private fun validateBlock(block: ScheduleBlock) {
        require(block.id.isNotBlank()) { "Block ID cannot be blank." }
        require(block.title.isNotBlank()) { "Block title cannot be blank." }
        require(block.endAtEpochMillis > block.startAtEpochMillis) {
            "Block end must be after its start."
        }
        java.time.ZoneId.of(block.timeZoneId)
        when (block.blockType) {
            ScheduleBlockType.TASK -> require(!block.taskId.isNullOrBlank() && block.habitId == null) {
                "Task blocks must reference exactly one task."
            }
            ScheduleBlockType.HABIT -> require(!block.habitId.isNullOrBlank() && block.taskId == null) {
                "Habit blocks must reference exactly one habit."
            }
            ScheduleBlockType.BREAK, ScheduleBlockType.EXTERNAL ->
                require(block.taskId == null && block.habitId == null) {
                    "Break and external blocks cannot reference tasks or habits."
                }
        }
    }
}
