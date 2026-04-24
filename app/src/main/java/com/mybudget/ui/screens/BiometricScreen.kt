package com.mybudget.ui.screens

import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.mybudget.security.BiometricUtils

@Composable
fun BiometricScreen(activity: FragmentActivity, onUnlocked: () -> Unit) {
    val executor = remember { ContextCompat.getMainExecutor(activity) }
    val biometricAvailable = remember(activity) {
        BiometricUtils.isAuthenticationAvailable(activity)
    }

    val failedAttempts = remember { mutableIntStateOf(0) }
    val lockoutUntil = remember { mutableLongStateOf(0L) }

    val promptInfo = remember {
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Budget Tracker")
            .setSubtitle("Authenticate to access your offline data")
            .setAllowedAuthenticators(BiometricUtils.allowedAuthenticators())
            .build()
    }

    val biometricPromptRef = remember { arrayOfNulls<BiometricPrompt>(1) }

    val biometricPrompt = remember {
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(
                        activity,
                        "Authentication Error: $errString",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    failedAttempts.intValue = 0
                    lockoutUntil.longValue = 0L
                    onUnlocked()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    failedAttempts.intValue += 1
                    if (failedAttempts.intValue >= 3) {
                        lockoutUntil.longValue = System.currentTimeMillis() + 30000L // 30s lockout
                        failedAttempts.intValue = 0
                        biometricPromptRef[0]?.cancelAuthentication()
                        Toast.makeText(
                            activity,
                            "Too many attempts. Please wait 30 seconds.",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(activity, "Authentication Failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
        biometricPromptRef[0] = prompt
        prompt
    }

    LaunchedEffect(biometricAvailable) {
        val currentTime = System.currentTimeMillis()
        if (biometricAvailable && currentTime >= lockoutUntil.longValue) {
            biometricPrompt.authenticate(promptInfo)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        IconButton(
            onClick = {
                val currentTime = System.currentTimeMillis()
                if (currentTime < lockoutUntil.longValue) {
                    val remainingSeconds = (lockoutUntil.longValue - currentTime) / 1000
                    Toast.makeText(
                        activity,
                        "Locked out. Try again in $remainingSeconds seconds.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else if (biometricAvailable) {
                    biometricPrompt.authenticate(promptInfo)
                }
            },
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Unlock",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(50.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Budget Tracker Locked",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (biometricAvailable) {
                "Tap the lock to authenticate"
            } else {
                "Biometric authentication is unavailable on this device"
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 16.sp
        )

        if (!biometricAvailable) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Secure screen lock is required.",
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
