package com.mybudget.ui.screens

import android.app.Application
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mybudget.domain.CategoryCatalog
import com.mybudget.domain.CategoryType
import com.mybudget.data.local.entity.TransactionEntity
import com.mybudget.data.local.prefs.SettingsManager
import com.mybudget.ui.components.BasicCategoryDropdown
import com.mybudget.ui.components.CategoryPickerField
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    settingsManager: SettingsManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: ScannerViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ScannerViewModel(context.applicationContext as Application) as T
            }
        }
    )

    val isProcessing by viewModel.isProcessing.collectAsState()
    val scanMessage by viewModel.scanMessage.collectAsState()
    val draftTransactions by viewModel.draftTransactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val rawText by viewModel.rawExtractedText.collectAsState()
    val detectedOpeningBalance by viewModel.detectedOpeningBalance.collectAsState()
    var showRawText by remember { mutableStateOf(false) }
    var createCategoryForDraftId by remember { mutableStateOf<Int?>(null) }
    var newCategoryName by remember { mutableStateOf("") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let { viewModel.processPdf(it) }
        }
    )

    val preferredCurrency by settingsManager.preferredCurrencyFlow.collectAsState(initial = "USD")
    val currencySymbol = remember(preferredCurrency) {
        when (preferredCurrency) {
            "INR" -> "Rs "
            "EUR" -> "EUR "
            "GBP" -> "GBP "
            else -> "$"
        }
    }
    val draftForNewCategory = remember(draftTransactions, createCategoryForDraftId) {
        draftTransactions.firstOrNull { it.localId == createCategoryForDraftId }
    }
    val createCategoryType = if (draftForNewCategory?.transaction?.isIncome == true) {
        CategoryType.INCOME
    } else {
        CategoryType.EXPENSE
    }
    val basicCategoryOptions = remember(createCategoryType) {
        CategoryCatalog.basicGroupsFor(createCategoryType)
    }
    var selectedBasicCategory by remember(createCategoryType) {
        mutableStateOf(basicCategoryOptions.firstOrNull() ?: "Other")
    }
    if (createCategoryForDraftId != null && draftForNewCategory != null) {
        AlertDialog(
            onDismissRequest = { createCategoryForDraftId = null },
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
                        val draft = draftForNewCategory ?: return@TextButton
                        val trimmed = newCategoryName.trim()
                        if (trimmed.isBlank()) return@TextButton
                        viewModel.createCustomCategory(
                            name = trimmed,
                            basicCategory = selectedBasicCategory,
                            isIncome = draft.transaction.isIncome
                        ) { category ->
                            viewModel.updateDraftCategory(draft.localId, category)
                            newCategoryName = ""
                            createCategoryForDraftId = null
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { createCategoryForDraftId = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Passbook Scanner") },
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = { filePickerLauncher.launch("application/pdf") },
                enabled = !isProcessing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Select PDF Statement")
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isProcessing) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(scanMessage, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (draftTransactions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Opening Balance",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Detected from the statement when possible. You can verify or edit it before saving.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = detectedOpeningBalance.orEmpty(),
                            onValueChange = { input ->
                                viewModel.updateDetectedOpeningBalance(
                                    input.filter { it.isDigit() || it == '.' }
                                )
                            },
                            label = { Text("Opening Balance ($currencySymbol)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }

            // Debug: show raw extracted text
            if (rawText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(onClick = { showRawText = !showRawText }) {
                        Text(if (showRawText) "Hide Raw Text" else "Show Raw Text (Debug)")
                    }
                    if (showRawText) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Raw PDF Text", rawText))
                                android.widget.Toast.makeText(context, "Copied to clipboard!", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        ) { Text("Copy All", fontSize = 12.sp) }
                    }
                }
                if (showRawText) {
                    Card(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        androidx.compose.foundation.text.selection.SelectionContainer {
                            LazyColumn(modifier = Modifier.padding(12.dp)) {
                                item {
                                    Text(
                                        rawText,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (draftTransactions.isNotEmpty()) {
                Text(
                    "Detected Transactions",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(draftTransactions) { tx ->
                        DraftTransactionCard(
                            draft = tx,
                            currencySymbol = currencySymbol,
                            categories = categories.filter {
                                it.type == if (tx.transaction.isIncome) CategoryType.INCOME else CategoryType.EXPENSE
                            },
                            onCategorySelected = { category ->
                                viewModel.updateDraftCategory(tx.localId, category)
                            },
                            onCreateCategory = {
                                selectedBasicCategory = CategoryCatalog.basicGroupsFor(
                                    if (tx.transaction.isIncome) CategoryType.INCOME else CategoryType.EXPENSE
                                ).firstOrNull() ?: "Other"
                                createCategoryForDraftId = tx.localId
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(onClick = { viewModel.clearDrafts() }) { Text("Discard") }
                    Button(onClick = { viewModel.approveTransactions() }) {
                        Text("Approve & Save")
                    }
                }
            }
        }
    }
}

@Composable
fun DraftTransactionCard(
    draft: DraftTransactionItem,
    currencySymbol: String,
    categories: List<com.mybudget.data.local.entity.CategoryEntity>,
    onCategorySelected: (com.mybudget.data.local.entity.CategoryEntity) -> Unit,
    onCreateCategory: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val tx = draft.transaction
    val selectedCategory = remember(categories, tx.categoryId) {
        categories.firstOrNull { it.id == tx.categoryId }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                tx.description.ifEmpty { "Transaction" },
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            CategoryPickerField(
                label = "Category",
                selectedCategory = selectedCategory,
                categories = categories,
                onCategorySelected = onCategorySelected,
                onCreateNew = onCreateCategory,
                modifier = Modifier.fillMaxWidth(),
                supportingText = if (draft.requiresCategory) {
                    "This transaction needs a category before it can be saved."
                } else {
                    "Review and adjust if needed."
                }
            )

            Spacer(modifier = Modifier.height(12.dp))
            val dateStr = dateFormatter.format(Date(tx.timestamp))
            val amountColor =
                if (tx.isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            val sign = if (tx.isIncome) "+" else "-"
            val amountLabel = "$sign$currencySymbol${"%.2f".format(tx.amount)}"

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    dateStr,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    amountLabel,
                    fontWeight = FontWeight.ExtraBold,
                    color = amountColor,
                    fontSize = 16.sp
                )
            }
        }
    }
}
