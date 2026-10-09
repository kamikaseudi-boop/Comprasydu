package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cashier_audits")
data class CashierAudit(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val listId: Long,
    val cashierAmount: Double,
    val calculatedTotal: Double,
    val difference: Double,
    val statusText: String,
    val auditedAt: Long = System.currentTimeMillis()
)
