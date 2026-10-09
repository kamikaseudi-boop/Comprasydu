package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
data class UserAccount(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val email: String,
    val passwordHash: String,
    val salt: String,
    val phone: String = "",
    val securityQuestion: String = "Qual o nome do seu animal de estimação?",
    val securityAnswerHash: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
