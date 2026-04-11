package com.mybudget.ui.screens

import android.app.Application
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mybudget.data.local.entity.TransactionEntity
import com.mybudget.data.local.prefs.SettingsManager
import com.mybudget.domain.CategoryCatalog
import com.mybudget.domain.CategoryType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    settingsManager: SettingsManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: DashboardViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return DashboardViewModel(context.applicationContext as Application) as T
            }
        }
    )
    val transactions by viewModel.transactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val preferredCurrency by settingsManager.preferredCurrencyFlow.collectAsState(initial = "USD")
    val currencySymbol = remember(preferredCurrency) {
        when (preferredCurrency) {
            "INR" -> "Rs "
            "EUR" -> "EUR "
            "GBP" -> "GBP "
            else -> "$"
        }
    }

    var pendingDelete by remember { mutableStateOf<TransactionEntity?>(null) }
    var categoryEditorTx by remember { mutableStateOf<TransactionEntity?>(null) }

    pendingDelete?.let { tx ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete Transaction") },
            text = { Text("Delete \"${tx.description.ifEmpty { "Unknown" }}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTransaction(tx)
                        pendingDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }

    categoryEditorTx?.let { tx ->
        val type = if (tx.isIncome) CategoryType.INCOME else CategoryType.EXPENSE
        val options = categories.filter { it.type == type }
        AlertDialog(
            onDismissRequest = { categoryEditorTx = null },
            title = { Text("Categorize Transaction") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        tx.description.ifEmpty { "Transaction" },
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Choose a category now, or leave it as uncategorized and come back later.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                    LazyColumn(modifier = Modifier.height(220.dp)) {
                        item {
                            FilterChip(
                                selected = tx.tag == CategoryCatalog.UNCATEGORIZED,
                                onClick = {
                                    viewModel.updateTransaction(
                                        tx.copy(
                                            categoryId = null,
                                            tag = CategoryCatalog.UNCATEGORIZED
                                        )
                                    )
                                    categoryEditorTx = null
                                },
                                label = { Text(CategoryCatalog.UNCATEGORIZED) }
                            )
                        }
                        items(options, key = { it.id }) { category ->
                            FilterChip(
                                selected = tx.categoryId == category.id,
                                onClick = {
                                    viewModel.updateTransaction(
                                        tx.copy(
                                            categoryId = category.id,
                                            tag = category.name
                                        )
                                    )
                                    categoryEditorTx = null
                                },
                                label = { Text("${category.emoji} ${category.name}") },
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { categoryEditorTx = null }) { Text("Done") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("All Transactions", fontWeight = FontWeight.Bold)
                        Text("${transactions.size} entries", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No transactions yet", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
                    Text("Add entries manually or scan a passbook", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }
                items(transactions, key = { it.id }) { tx ->
                    SwipeToDeleteTransaction(
                        transaction = tx,
                        currencySymbol = currencySymbol,
                        onDeleteRequested = { pendingDelete = tx },
                        onEditCategoryRequested = { categoryEditorTx = tx }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteTransaction(
    transaction: TransactionEntity,
    currencySymbol: String,
    onDeleteRequested: () -> Unit,
    onEditCategoryRequested: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDeleteRequested()
            }
            false
        }
    )

    val bgColor by animateColorAsState(
        targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "swipe_bg"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) 1.2f else 0.9f,
        label = "icon_scale"
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(bgColor)
                    .padding(end = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.scale(iconScale)
                )
            }
        },
        content = {
            TransactionRow(
                tx = transaction,
                currencySymbol = currencySymbol,
                dateFormatter = dateFormatter,
                onDelete = onDeleteRequested,
                onEditCategory = onEditCategoryRequested
            )
        }
    )
}

@Composable
fun TransactionRow(
    tx: TransactionEntity,
    currencySymbol: String,
    dateFormatter: SimpleDateFormat,
    onDelete: () -> Unit,
    onEditCategory: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    tx.description.ifEmpty { "Unknown" },
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp
                )
                Text(
                    text = dateFormatter.format(Date(tx.timestamp)),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = tx.tag.ifEmpty { CategoryCatalog.UNCATEGORIZED },
                    fontSize = 12.sp,
                    color = if (tx.tag == CategoryCatalog.UNCATEGORIZED) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(onClick = onEditCategory)
                )
            }

            val amountColor = if (tx.isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            val sign = if (tx.isIncome) "+" else "-"
            Text(
                "$sign$currencySymbol${"%.2f".format(tx.amount)}",
                fontWeight = FontWeight.ExtraBold,
                color = amountColor,
                fontSize = 15.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
