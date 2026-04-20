package com.mybudget.data.local.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

class SettingsManager(private val context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_user_settings",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        val KEY_CURRENCY_DS = stringPreferencesKey("preferred_currency")
        val KEY_BUDGET_CYCLE_DS = stringPreferencesKey("budget_cycle")
        val KEY_BIOMETRIC_ENABLED_DS = booleanPreferencesKey("biometric_enabled")
        val KEY_ONBOARDING_COMPLETED_DS = booleanPreferencesKey("onboarding_completed")
        val KEY_APP_THEME_DS = stringPreferencesKey("app_theme")
        val KEY_OPENING_BALANCE_DS = doublePreferencesKey("opening_balance")
        val KEY_BASE_SAFE_TO_SPEND_DS = doublePreferencesKey("base_safe_to_spend")

        const val KEY_CURRENCY = "preferred_currency"
        const val KEY_BUDGET_CYCLE = "budget_cycle"
        const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        const val KEY_APP_THEME = "app_theme"
        const val KEY_OPENING_BALANCE = "opening_balance"
        const val KEY_BASE_SAFE_TO_SPEND = "base_safe_to_spend"
    }

    init {
        // Migrate from DataStore to EncryptedSharedPreferences
        val isMigrated = sharedPreferences.getBoolean("is_migrated_from_datastore", false)
        if (!isMigrated) {
            runBlocking(Dispatchers.IO) {
                try {
                    val prefs = context.dataStore.data.first()
                    sharedPreferences.edit().apply {
                        putString(KEY_CURRENCY, prefs[KEY_CURRENCY_DS] ?: "USD")
                        putString(KEY_BUDGET_CYCLE, prefs[KEY_BUDGET_CYCLE_DS] ?: "Monthly")
                        putBoolean(KEY_BIOMETRIC_ENABLED, prefs[KEY_BIOMETRIC_ENABLED_DS] ?: false)
                        putBoolean(KEY_ONBOARDING_COMPLETED, prefs[KEY_ONBOARDING_COMPLETED_DS] ?: false)
                        putString(KEY_APP_THEME, prefs[KEY_APP_THEME_DS] ?: "System")
                        putFloat(KEY_OPENING_BALANCE, (prefs[KEY_OPENING_BALANCE_DS] ?: 0.0).toFloat())
                        putFloat(KEY_BASE_SAFE_TO_SPEND, (prefs[KEY_BASE_SAFE_TO_SPEND_DS] ?: 500.0).toFloat())
                        putBoolean("is_migrated_from_datastore", true)
                        apply()
                    }
                } catch (e: Exception) {
                    // Ignore migration errors mostly means file doesnt exist
                }
            }
        }
    }

    private fun <T> getFlow(key: String, defaultValue: T, getter: () -> T): Flow<T> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
            if (key == changedKey) {
                trySend(getter())
            }
        }
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
        trySend(getter())
        awaitClose { sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    val preferredCurrencyFlow: Flow<String> = getFlow(KEY_CURRENCY, "USD") {
        sharedPreferences.getString(KEY_CURRENCY, "USD") ?: "USD"
    }

    val budgetCycleFlow: Flow<String> = getFlow(KEY_BUDGET_CYCLE, "Monthly") {
        sharedPreferences.getString(KEY_BUDGET_CYCLE, "Monthly") ?: "Monthly"
    }

    val biometricEnabledFlow: Flow<Boolean> = getFlow(KEY_BIOMETRIC_ENABLED, false) {
        sharedPreferences.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    val onboardingCompletedFlow: Flow<Boolean> = getFlow(KEY_ONBOARDING_COMPLETED, false) {
        sharedPreferences.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    val appThemeFlow: Flow<String> = getFlow(KEY_APP_THEME, "System") {
        sharedPreferences.getString(KEY_APP_THEME, "System") ?: "System"
    }

    val openingBalanceFlow: Flow<Double> = getFlow(KEY_OPENING_BALANCE, 0.0) {
        sharedPreferences.getFloat(KEY_OPENING_BALANCE, 0.0f).toDouble()
    }

    val baseSafeToSpendFlow: Flow<Double> = getFlow(KEY_BASE_SAFE_TO_SPEND, 500.0) {
        sharedPreferences.getFloat(KEY_BASE_SAFE_TO_SPEND, 500.0f).toDouble()
    }

    suspend fun saveCurrency(currency: String) = withContext(Dispatchers.IO) {
        sharedPreferences.edit().putString(KEY_CURRENCY, currency).apply()
    }

    suspend fun saveBudgetCycle(cycle: String) = withContext(Dispatchers.IO) {
        sharedPreferences.edit().putString(KEY_BUDGET_CYCLE, cycle).apply()
    }

    suspend fun setBiometricEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        sharedPreferences.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    suspend fun setOnboardingCompleted(completed: Boolean) = withContext(Dispatchers.IO) {
        sharedPreferences.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
    }

    suspend fun saveAppTheme(theme: String) = withContext(Dispatchers.IO) {
        sharedPreferences.edit().putString(KEY_APP_THEME, theme).apply()
    }

    suspend fun saveOpeningBalance(balance: Double) = withContext(Dispatchers.IO) {
        sharedPreferences.edit().putFloat(KEY_OPENING_BALANCE, balance.toFloat()).apply()
    }

    suspend fun saveBaseSafeToSpend(amount: Double) = withContext(Dispatchers.IO) {
        sharedPreferences.edit().putFloat(KEY_BASE_SAFE_TO_SPEND, amount.toFloat()).apply()
    }
}
