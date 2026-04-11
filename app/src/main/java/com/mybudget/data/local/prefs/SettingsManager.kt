package com.mybudget.data.local.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

class SettingsManager(private val context: Context) {

    companion object {
        val KEY_CURRENCY = stringPreferencesKey("preferred_currency")
        val KEY_BUDGET_CYCLE = stringPreferencesKey("budget_cycle")
        val KEY_BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val KEY_APP_THEME = stringPreferencesKey("app_theme")
        val KEY_OPENING_BALANCE = doublePreferencesKey("opening_balance")
        val KEY_BASE_SAFE_TO_SPEND = doublePreferencesKey("base_safe_to_spend")
    }

    val preferredCurrencyFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_CURRENCY] ?: "USD"
    }

    val budgetCycleFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_BUDGET_CYCLE] ?: "Monthly"
    }

    val biometricEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_BIOMETRIC_ENABLED] ?: false
    }

    val onboardingCompletedFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_ONBOARDING_COMPLETED] ?: false
    }

    val appThemeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_APP_THEME] ?: "System"
    }

    val openingBalanceFlow: Flow<Double> = context.dataStore.data.map { preferences ->
        preferences[KEY_OPENING_BALANCE] ?: 0.0
    }

    val baseSafeToSpendFlow: Flow<Double> = context.dataStore.data.map { preferences ->
        preferences[KEY_BASE_SAFE_TO_SPEND] ?: 500.0 // Default 500.0
    }

    suspend fun saveCurrency(currency: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_CURRENCY] = currency
        }
    }

    suspend fun saveBudgetCycle(cycle: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_BUDGET_CYCLE] = cycle
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_BIOMETRIC_ENABLED] = enabled
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun saveAppTheme(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_APP_THEME] = theme
        }
    }

    suspend fun saveOpeningBalance(balance: Double) {
        context.dataStore.edit { preferences ->
            preferences[KEY_OPENING_BALANCE] = balance
        }
    }

    suspend fun saveBaseSafeToSpend(amount: Double) {
        context.dataStore.edit { preferences ->
            preferences[KEY_BASE_SAFE_TO_SPEND] = amount
        }
    }
}
