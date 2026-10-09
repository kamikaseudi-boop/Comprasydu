package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shopping_items")
data class ShoppingItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val listId: Long,
    val name: String,
    val quantity: Double,
    val unitPrice: Double,
    val isPurchased: Boolean = false,
    val unit: String = "un",
    val createdAt: Long = System.currentTimeMillis()
) {
    val totalValue: Double
        get() = quantity * unitPrice
}
