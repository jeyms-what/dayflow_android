package com.dayflow.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.dayflow.core.database.entity.WorkoutSessionEntity
import com.dayflow.core.database.entity.WorkoutSetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workout_sessions ORDER BY startedAtEpochMillis DESC")
    fun observeSessions(): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sets WHERE sessionId = :sessionId ORDER BY completedAtEpochMillis ASC")
    fun observeSets(sessionId: String): Flow<List<WorkoutSetEntity>>

    @Query("SELECT * FROM workout_sets WHERE sessionId = :sessionId ORDER BY completedAtEpochMillis ASC")
    suspend fun getSets(sessionId: String): List<WorkoutSetEntity>

    @Insert
    suspend fun insertSession(session: WorkoutSessionEntity)

    @Insert
    suspend fun insertSet(set: WorkoutSetEntity)

    @Query("DELETE FROM workout_sessions WHERE id = :id")
    suspend fun deleteSession(id: String): Int

    @Query("DELETE FROM workout_sets WHERE sessionId = :sessionId")
    suspend fun deleteSetsForSession(sessionId: String): Int
}
