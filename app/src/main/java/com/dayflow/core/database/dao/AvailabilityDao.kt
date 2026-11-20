package com.dayflow.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.dayflow.core.database.entity.AvailabilityWindowEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AvailabilityDao {
    @Upsert
    suspend fun upsert(window: AvailabilityWindowEntity)

    @Query("""
        SELECT * FROM availability_windows
        WHERE isEnabled = 1
        ORDER BY startMinute ASC
    """)
    fun observeEnabledWindows(): Flow<List<AvailabilityWindowEntity>>

    @Query("SELECT * FROM availability_windows WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): AvailabilityWindowEntity?

    @Query("DELETE FROM availability_windows WHERE id = :id")
    suspend fun deleteById(id: String): Int
}
