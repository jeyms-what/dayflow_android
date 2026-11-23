package com.dayflow.data.repository

import androidx.room.withTransaction
import com.dayflow.core.database.DayflowDatabase
import com.dayflow.core.database.entity.NoteEntity
import com.dayflow.core.database.entity.WorkoutSessionEntity
import com.dayflow.core.database.entity.WorkoutSetEntity
import com.dayflow.domain.model.Note
import com.dayflow.domain.model.WorkoutSession
import com.dayflow.domain.model.WorkoutSet
import com.dayflow.domain.repository.WorkspaceRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomWorkspaceRepository @Inject constructor(
    private val database: DayflowDatabase
) : WorkspaceRepository {
    private val noteDao = database.noteDao()
    private val workoutDao = database.workoutDao()

    override fun observeNotes(): Flow<List<Note>> =
        noteDao.observeNotes().map { rows -> rows.map { it.toDomain() } }

    override suspend fun saveNote(note: Note) {
        require(note.title.isNotBlank() || note.content.isNotBlank()) {
            "A note needs a title or some content."
        }
        val now = System.currentTimeMillis()
        noteDao.upsert(
            NoteEntity(
                id = note.id,
                title = note.title.trim(),
                content = note.content,
                category = note.category,
                isPinned = note.isPinned,
                createdAtEpochMillis = note.createdAtEpochMillis,
                updatedAtEpochMillis = now
            )
        )
    }

    override suspend fun deleteNote(id: String) {
        check(noteDao.delete(id) == 1) { "Note was not found." }
    }

    override fun observeWorkoutSessions(): Flow<List<WorkoutSession>> =
        workoutDao.observeSessions().map { rows -> rows.map { it.toDomain() } }

    override fun observeWorkoutSets(sessionId: String): Flow<List<WorkoutSet>> =
        workoutDao.observeSets(sessionId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun saveWorkoutSession(session: WorkoutSession, sets: List<WorkoutSet>) {
        require(session.title.isNotBlank()) { "Workout name cannot be blank." }
        require(sets.isNotEmpty()) { "A workout needs at least one set." }
        require(sets.all {
            it.sessionId == session.id &&
                it.exerciseName.isNotBlank() &&
                it.muscleGroup.isNotBlank() &&
                it.setNumber > 0 &&
                it.reps in 1..100 &&
                it.weightKg.isFinite() &&
                it.weightKg in 0.0..1000.0
        }) {
            "Workout sets contain invalid values."
        }
        database.withTransaction {
            workoutDao.insertSession(
                WorkoutSessionEntity(
                    id = session.id,
                    title = session.title.trim(),
                    startedAtEpochMillis = session.startedAtEpochMillis,
                    endedAtEpochMillis = session.endedAtEpochMillis,
                    durationSeconds = session.durationSeconds,
                    notes = session.notes,
                    status = session.status,
                    createdAtEpochMillis = session.createdAtEpochMillis
                )
            )
            sets.forEach { set ->
                workoutDao.insertSet(
                    WorkoutSetEntity(
                        id = set.id,
                        sessionId = set.sessionId,
                        exerciseName = set.exerciseName.trim(),
                        muscleGroup = set.muscleGroup,
                        setNumber = set.setNumber,
                        reps = set.reps,
                        weightKg = set.weightKg,
                        completedAtEpochMillis = set.completedAtEpochMillis
                    )
                )
            }
        }
    }
}

private fun NoteEntity.toDomain() = Note(
    id, title, content, category, isPinned, createdAtEpochMillis, updatedAtEpochMillis
)

private fun WorkoutSessionEntity.toDomain() = WorkoutSession(
    id, title, startedAtEpochMillis, endedAtEpochMillis, durationSeconds, notes, status, createdAtEpochMillis
)

private fun WorkoutSetEntity.toDomain() = WorkoutSet(
    id, sessionId, exerciseName, muscleGroup, setNumber, reps, weightKg, completedAtEpochMillis
)
