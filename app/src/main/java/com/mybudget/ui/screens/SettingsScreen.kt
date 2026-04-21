package com.mybudget.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Restore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.mybudget.data.local.BudgetDatabase
import com.mybudget.data.local.backup.BackupRestoreManager
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mybudget.data.local.prefs.SettingsManager
import com.mybudget.security.BiometricUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsManager: SettingsManager,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val biometricEnabled by settingsManager.biometricEnabledFlow.collectAsState(initial = false)
    val appTheme by settingsManager.appThemeFlow.collectAsState(initial = "System")
    val preferredCurrency by settingsManager.preferredCurrencyFlow.collectAsState(initial = "USD")
    val budgetCycle by settingsManager.budgetCycleFlow.collectAsState(initial = "Monthly")
    val openingBalance by settingsManager.openingBalanceFlow.collectAsState(initial = 0.0)
    var openingBalanceInput by remember(openingBalance) { mutableStateOf(if (openingBalance == 0.0) "" else "%.2f".format(openingBalance)) }
    val biometricAvailable = remember(context) {
        BiometricUtils.isAuthenticationAvailable(context)
    }
    val currencies = remember {
        listOf("USD ($)", "INR (Rs)", "EUR (EUR)", "GBP (GBP)")
    }
    val budgetCyclesList = remember { listOf("Daily", "Weekly", "Monthly") }
    var showBudgetCycleWarning by remember { mutableStateOf<String?>(null) }

    val transactionDao = remember(context) { BudgetDatabase.getDatabase(context).transactionDao() }
    val backupRestoreManager = remember(context) { BackupRestoreManager(context, transactionDao) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val success = withContext(Dispatchers.IO) {
                    val transactions = transactionDao.getAllTransactionsSync()
                    backupRestoreManager.exportData(uri, transactions)
                }
                val msg = if (success) "Backup saved successfully!" else "Failed to save backup."
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val success = backupRestoreManager.importData(uri)
                val msg = if (success) "Restored successfully!" else "Failed to restore backup."
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (showBudgetCycleWarning != null) {
        val targetCycle = showBudgetCycleWarning!!
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showBudgetCycleWarning = null },
            title = { Text("Change Budget Cycle?") },
            text = { Text("Changing your cycle from $budgetCycle to $targetCycle will securely reset your current Safe to Spend limit globally to 0.\n\nYou will need to enter a fresh target limit on your dashboard. Do you wish to continue?") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    coroutineScope.launch {
                        settingsManager.saveBudgetCycle(targetCycle)
                        settingsManager.saveBaseSafeToSpend(0.0)
                    }
                    showBudgetCycleWarning = null
                }) { Text("Confirm & Reset") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showBudgetCycleWarning = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Biometric Security",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Require fingerprint/face to open app",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = biometricEnabled,
                    onCheckedChange = { checked ->
                        if (checked && !biometricAvailable) {
                            Toast.makeText(
                                context,
                                "Set up a device credential or biometric first.",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            coroutineScope.launch {
                                settingsManager.setBiometricEnabled(checked)
                            }
                        }
                    },
                    enabled = biometricAvailable || biometricEnabled
                )
            }

            if (!biometricAvailable) {
                Text(
                    "Biometric lock is unavailable until this device has a screen lock, fingerprint, or face unlock configured.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider()

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "App Theme",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                val themes = listOf("System", "Light", "Dark")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    themes.forEach { theme ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                coroutineScope.launch { settingsManager.saveAppTheme(theme) }
                            }
                        ) {
                            RadioButton(
                                selected = appTheme == theme,
                                onClick = {
                                    coroutineScope.launch { settingsManager.saveAppTheme(theme) }
                                }
                            )
                            Text(text = theme, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            HorizontalDivider()

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Safe to Spend Cycle",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                var expandedCycle by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(
                        onClick = { expandedCycle = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(budgetCycle)
                    }
                    DropdownMenu(
                        expanded = expandedCycle,
                        onDismissRequest = { expandedCycle = false }
                    ) {
                        budgetCyclesList.forEach { selection ->
                            DropdownMenuItem(
                                text = { Text(selection) },
                                onClick = {
                                    if (selection != budgetCycle) {
                                        showBudgetCycleWarning = selection
                                    }
                                    expandedCycle = false
                                }
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Preferred Currency",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                var expanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val currentText = currencies.find { it.startsWith(preferredCurrency) } ?: currencies.first()
                        Text(currentText)
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        currencies.forEach { selection ->
                            val baseCode = selection.substringBefore(" ")
                            DropdownMenuItem(
                                text = { Text(selection) },
                                onClick = {
                                    coroutineScope.launch {
                                        settingsManager.saveCurrency(baseCode)
                                    }
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            // Opening Balance
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Opening Balance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "The amount already in your account before you started tracking. This is added to your total balance.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = openingBalanceInput,
                        onValueChange = { openingBalanceInput = it },
                        label = { Text("Amount ($preferredCurrency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    androidx.compose.material3.Button(
                        onClick = {
                            val parsed = openingBalanceInput.toDoubleOrNull()
                            if (parsed == null || parsed < 0) {
                                Toast.makeText(context, "Enter a valid positive number", Toast.LENGTH_SHORT).show()
                            } else {
                                coroutineScope.launch { settingsManager.saveOpeningBalance(parsed) }
                                Toast.makeText(context, "Opening balance saved!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) { Text("Save") }
                }
            }

            HorizontalDivider()

            // Backup & Restore
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Backup & Restore",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Export your transactions safely securely across devices. Your encryption key is device-specific, so use this option instead of transferring raw app data when switching phones.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = { exportLauncher.launch("MyBudget_Backup.json") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Backup", modifier = Modifier.padding(end = 6.dp))
                        Text("Backup DB")
                    }
                    androidx.compose.material3.Button(
                        onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = "Restore", modifier = Modifier.padding(end = 6.dp))
                        Text("Restore DB")
                    }
                }
            }
            // Add a little padding at the bottom for scrolling.
            Box(Modifier.padding(bottom = 32.dp))
        }
    }
}
