package com.mybudget.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val isIncome: Boolean,
    val categoryId: Long?,
    val tag: String,
    val timestamp: Long,
    val currency: String,
    val description: String
)
