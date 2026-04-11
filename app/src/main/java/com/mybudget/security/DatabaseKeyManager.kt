package com.mybudget.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom
import android.util.Base64

object DatabaseKeyManager {
    private const val PREFS_NAME = "secure_db_prefs"
    private const val KEY_DB_PASSPHRASE = "db_passphrase"

    fun getDatabasePassphrase(context: Context): ByteArray {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val sharedPreferences = EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        var base64Key = sharedPreferences.getString(KEY_DB_PASSPHRASE, null)
        if (base64Key == null) {
            val random = SecureRandom()
            val newKey = ByteArray(32)
            random.nextBytes(newKey)
            base64Key = Base64.encodeToString(newKey, Base64.NO_WRAP)
            sharedPreferences.edit().putString(KEY_DB_PASSPHRASE, base64Key).apply()
        }

        return Base64.decode(base64Key, Base64.NO_WRAP)
    }
}
