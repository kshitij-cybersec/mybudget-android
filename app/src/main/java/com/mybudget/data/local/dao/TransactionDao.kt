package com.mybudget.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mybudget.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactionsSync(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE timestamp BETWEEN :startDate AND :endDate ORDER BY timestamp DESC")
    fun getTransactionsBetween(startDate: Long, endDate: Long): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM transactions
            WHERE timestamp = :timestamp
              AND amount = :amount
              AND isIncome = :isIncome
              AND currency = :currency
              AND description = :description
            LIMIT 1
        )
        """
    )
    fun transactionExists(
        timestamp: Long,
        amount: Double,
        isIncome: Boolean,
        currency: String,
        description: String
    ): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTransactions(transactions: List<TransactionEntity>): List<Long>

    @Update
    fun updateTransaction(transaction: TransactionEntity): Int

    @Delete
    fun deleteTransaction(transaction: TransactionEntity): Int
}
