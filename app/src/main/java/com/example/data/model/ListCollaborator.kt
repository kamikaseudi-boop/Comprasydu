package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "list_collaborators",
    indices = [Index(value = ["listId", "collaboratorEmail"], unique = true)]
)
data class ListCollaborator(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val listId: Long,
    val collaboratorEmail: String,
    val canEdit: Boolean = true,
    val addedAt: Long = System.currentTimeMillis()
)
