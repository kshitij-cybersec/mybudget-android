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
}
