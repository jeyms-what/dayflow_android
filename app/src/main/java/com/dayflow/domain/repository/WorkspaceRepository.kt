package com.dayflow.domain.repository

import com.dayflow.domain.model.Note
import com.dayflow.domain.model.WorkoutSession
import com.dayflow.domain.model.WorkoutSet
import kotlinx.coroutines.flow.Flow

interface WorkspaceRepository {
    fun observeNotes(): Flow<List<Note>>
    suspend fun saveNote(note: Note)
    suspend fun deleteNote(id: String)

    fun observeWorkoutSessions(): Flow<List<WorkoutSession>>
    fun observeWorkoutSets(sessionId: String): Flow<List<WorkoutSet>>
    suspend fun saveWorkoutSession(session: WorkoutSession, sets: List<WorkoutSet>)
}
