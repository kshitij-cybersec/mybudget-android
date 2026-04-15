package com.mybudget.data.local.backup

import android.content.Context
import android.net.Uri
import com.mybudget.data.local.dao.TransactionDao
import com.mybudget.data.local.entity.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class BackupRestoreManager(
    private val context: Context,
    private val transactionDao: TransactionDao
) {
    private val secretKey = SecretKeySpec("MyBudgetV1SecretKeyForJSONBckp!!".toByteArray(Charsets.UTF_8), "AES")

    private fun encrypt(data: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
        val ivAndEncrypted = iv + encrypted
        return Base64.encodeToString(ivAndEncrypted, Base64.NO_WRAP)
    }

    private fun decrypt(base64Data: String): String {
        val ivAndEncrypted = Base64.decode(base64Data, Base64.NO_WRAP)
        val iv = ivAndEncrypted.copyOfRange(0, 12)
        val encrypted = ivAndEncrypted.copyOfRange(12, ivAndEncrypted.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
        val decrypted = cipher.doFinal(encrypted)
        return String(decrypted, Charsets.UTF_8)
    }
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

            val jsonString = jsonArray.toString(4)
            val encryptedData = encrypt(jsonString)

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(encryptedData)
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
            val encryptedString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val builder = java.lang.StringBuilder()
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalBytes = 0L
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    totalBytes += bytesRead
                    if (totalBytes > 10 * 1024 * 1024) return@withContext false // 10MB limit
                    builder.append(String(buffer, 0, bytesRead, Charsets.UTF_8))
                }
                builder.toString()
            } ?: return@withContext false

            if (encryptedString.isBlank()) return@withContext false

            val jsonString = decrypt(encryptedString)
            if (jsonString.isBlank() || !jsonString.trimStart().startsWith("[")) return@withContext false

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
