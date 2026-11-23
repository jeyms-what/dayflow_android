package com.dayflow.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notes",
    indices = [Index(value = ["updatedAtEpochMillis"]), Index(value = ["isPinned"])]
)
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    val category: String = "GENERAL",
    val isPinned: Boolean = false,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long
)
