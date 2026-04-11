package com.mybudget.data.local.backup

import android.content.Context
import android.net.Uri
import com.mybudget.data.local.dao.TransactionDao
import com.mybudget.data.local.entity.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class BackupRestoreManager(
    private val context: Context,
    private val transactionDao: TransactionDao
) {
    suspend fun exportData(uri: Uri, transactions: List<TransactionEntity>): Boolean = withContext(Dispatchers.IO) {
        try {
            val jsonArray = JSONArray()
            transactions.forEach { tx ->
                val jsonObj = JSONObject().apply {
                    put("amount", tx.amount)
                    put("isIncome", tx.isIncome)
                    if (tx.categoryId != null) put("categoryId", tx.categoryId)
                    put("tag", tx.tag)
                    put("timestamp", tx.timestamp)
                    put("currency", tx.currency)
                    put("description", tx.description)
                }
                jsonArray.put(jsonObj)
            }

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(jsonArray.toString(4))
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun importData(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                InputStreamReader(inputStream).use { reader ->
                    reader.readText()
                }
            } ?: return@withContext false

            val jsonArray = JSONArray(jsonString)
            val newTransactions = mutableListOf<TransactionEntity>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val tx = TransactionEntity(
                    id = 0, // Auto-generate new IDs to avoid conflicts securely
                    amount = obj.getDouble("amount"),
                    isIncome = obj.getBoolean("isIncome"),
                    categoryId = if (obj.has("categoryId")) obj.getLong("categoryId") else null,
                    tag = obj.getString("tag"),
                    timestamp = obj.getLong("timestamp"),
                    currency = obj.getString("currency"),
                    description = obj.getString("description")
                )
                newTransactions.add(tx)
            }
            if (newTransactions.isNotEmpty()) {
                // Bulk insert safely, relying strictly on the DAO replacing conflicts if any
                transactionDao.insertTransactions(newTransactions)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
