package com.mybudget.security

import android.content.Context
import androidx.biometric.BiometricManager

object BiometricUtils {
    private const val AUTHENTICATORS =
        BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL

    fun allowedAuthenticators(): Int = AUTHENTICATORS

    fun isAuthenticationAvailable(context: Context): Boolean {
        return BiometricManager.from(context).canAuthenticate(AUTHENTICATORS) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun getPrefs(context: Context) = 
        context.getSharedPreferences("biometric_prefs", Context.MODE_PRIVATE)

    fun getFailedAttempts(context: Context): Int {
        return getPrefs(context).getInt("failed_attempts", 0)
    }

    fun recordFailedAttempt(context: Context): Int {
        val current = getFailedAttempts(context) + 1
        getPrefs(context).edit().putInt("failed_attempts", current).apply()
        return current
    }

    fun resetFailedAttempts(context: Context) {
        getPrefs(context).edit().putInt("failed_attempts", 0).putLong("lockout_until", 0L).apply()
    }

    fun clearFailedAttempts(context: Context) {
        getPrefs(context).edit().putInt("failed_attempts", 0).apply()
    }

    fun getLockoutUntil(context: Context): Long {
        return getPrefs(context).getLong("lockout_until", 0L)
    }

    fun setLockoutUntil(context: Context, timeInMillis: Long) {
        getPrefs(context).edit().putLong("lockout_until", timeInMillis).apply()
    }
}
