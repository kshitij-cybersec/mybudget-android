package com.mybudget.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mybudget.data.local.BudgetDatabase
import com.mybudget.domain.CategoryCatalog
import com.mybudget.domain.CategoryType
import com.mybudget.data.local.prefs.SettingsManager
import com.mybudget.ui.components.BasicCategoryDropdown
import com.mybudget.ui.components.CategoryPickerField
import com.mybudget.ui.viewmodel.ManualEntryViewModel
import com.mybudget.ui.viewmodel.ManualEntryViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualEntryScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val database = remember { BudgetDatabase.getDatabase(context) }
    val dao = remember(database) { database.transactionDao() }
    val categoryDao = remember(database) { database.categoryDao() }
    val settingsManager = remember { SettingsManager(context) }
    val viewModel: ManualEntryViewModel = viewModel(factory = ManualEntryViewModelFactory(dao, categoryDao))
    val preferredCurrency by settingsManager.preferredCurrencyFlow.collectAsState(initial = "USD")
    val categories by viewModel.categories.collectAsState()

    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var isExpense by remember { mutableStateOf(true) }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var showCreateCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    val categoryType = if (isExpense) CategoryType.EXPENSE else CategoryType.INCOME
    val filteredCategories = remember(categories, categoryType) {
        categories.filter { it.type == categoryType }
    }
    val selectedCategory = remember(filteredCategories, selectedCategoryId) {
        filteredCategories.firstOrNull { it.id == selectedCategoryId }
    }
    val basicCategoryOptions = remember(categoryType) { CategoryCatalog.basicGroupsFor(categoryType) }
    var selectedBasicCategory by remember(categoryType) {
        mutableStateOf(basicCategoryOptions.firstOrNull() ?: "Other")
    }

    if (showCreateCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCreateCategoryDialog = false },
            title = { Text("Create Category") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text("Category Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    BasicCategoryDropdown(
                        label = "Basic Category",
                        options = basicCategoryOptions,
                        selectedOption = selectedBasicCategory,
                        onOptionSelected = { selectedBasicCategory = it }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = newCategoryName.trim()
                        if (trimmed.isBlank()) {
                            Toast.makeText(context, "Enter a category name", Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }
                        viewModel.createCustomCategory(
                            name = trimmed,
                            basicCategory = selectedBasicCategory,
                            isExpense = isExpense
                        ) { category ->
                            selectedCategoryId = category.id
                            newCategoryName = ""
                            showCreateCategoryDialog = false
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateCategoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Transaction", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = {
                        isExpense = true
                        selectedCategoryId = null
                        selectedBasicCategory = CategoryCatalog.basicGroupsFor(CategoryType.EXPENSE).firstOrNull() ?: "Other"
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isExpense) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isExpense) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("Expense")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        isExpense = false
                        selectedCategoryId = null
                        selectedBasicCategory = CategoryCatalog.basicGroupsFor(CategoryType.INCOME).firstOrNull() ?: "Income"
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isExpense) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (!isExpense) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("Income")
                }
            }

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount ($preferredCurrency)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            CategoryPickerField(
                label = "Category",
                selectedCategory = selectedCategory,
                categories = filteredCategories,
                onCategorySelected = { selectedCategoryId = it.id },
                onCreateNew = { showCreateCategoryDialog = true },
                modifier = Modifier.fillMaxWidth(),
                supportingText = if (filteredCategories.isEmpty()) {
                    "No categories available yet. Create one."
                } else {
                    "Choose from saved categories or create your own."
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val parsedAmount = amount.toDoubleOrNull()
                    if (parsedAmount == null || parsedAmount <= 0) {
                        Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (description.isBlank()) {
                        Toast.makeText(context, "Please enter a description", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    viewModel.saveTransaction(
                        description = description,
                        amount = parsedAmount,
                        isExpense = isExpense,
                        category = selectedCategory,
                        currency = preferredCurrency,
                        timestamp = System.currentTimeMillis(),
                        onComplete = onBack
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Save Entry", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
