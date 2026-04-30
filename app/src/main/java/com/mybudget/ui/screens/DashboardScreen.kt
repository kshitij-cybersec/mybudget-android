package com.mybudget.ui.screens

import android.app.Application
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mybudget.data.local.entity.CategoryEntity
import com.mybudget.data.local.entity.TransactionEntity
import com.mybudget.data.local.prefs.SettingsManager
import com.mybudget.domain.CurrencyConfig
import com.mybudget.domain.CategoryCatalog
import com.mybudget.domain.CategoryType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val ChartColors = listOf(
    Color(0xFF6C5CE7), Color(0xFFFF6B6B), Color(0xFF00CEC9),
    Color(0xFFFFA62F), Color(0xFFFF85A2), Color(0xFF74B9FF),
    Color(0xFFA29BFE), Color(0xFF55E6C1)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    settingsManager: SettingsManager,
    onNavigateToScanner: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToManualEntry: () -> Unit,
    onNavigateToAllTransactions: () -> Unit
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
    val totalBalance by viewModel.totalBalance.collectAsState()
    val safeToSpend by viewModel.safeToSpend.collectAsState()
    val budgetCycle by viewModel.budgetCycle.collectAsState()

    val preferredCurrency by settingsManager.preferredCurrencyFlow.collectAsState(initial = "USD")
    val currencySymbol = remember(preferredCurrency) {
        CurrencyConfig.symbolFor(preferredCurrency)
    }

    var showSafeToSpendDialog by remember { mutableStateOf(false) }

    if (showSafeToSpendDialog) {
        var newAmount by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSafeToSpendDialog = false },
            title = { Text("Set $budgetCycle Safe to Spend Limit") },
            text = {
                OutlinedTextField(
                    value = newAmount,
                    onValueChange = { newAmount = it.filter { char -> char.isDigit() || char == '.' } },
                    label = { Text("Amount ($currencySymbol)") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        newAmount.toDoubleOrNull()?.let { amount ->
                            viewModel.updateBaseSafeToSpend(amount)
                        }
                        showSafeToSpendDialog = false
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showSafeToSpendDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budget Tracker", fontWeight = FontWeight.Black) },
                actions = {
                    IconButton(onClick = onNavigateToScanner) {
                        Icon(Icons.Default.UploadFile, contentDescription = "Upload Passbook")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToManualEntry,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Default.Add, "Add") },
                text = { Text("Add Entry", fontWeight = FontWeight.Bold) }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }
            item { 
                BalanceCard(
                    totalBalance, safeToSpend, currencySymbol, budgetCycle,
                    onEditSafeToSpend = { showSafeToSpendDialog = true }
                ) 
            }
            item {
                ChartSection(
                    transactions = transactions,
                    categories = categories,
                    currencySymbol = currencySymbol,
                    onUpdateTransaction = viewModel::updateTransaction
                )
            }
            item {
                RecentTransactionsWidget(
                    transactions.take(5),
                    currencySymbol,
                    onNavigateToAllTransactions
                )
            }
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}

// ========================== BALANCE CARD ==========================

@Composable
fun BalanceCard(
    totalBalance: Double, 
    safeToSpend: Double, 
    currency: String,
    budgetCycle: String,
    onEditSafeToSpend: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Text("Total Balance", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = "$currency${"%.2f".format(totalBalance)}",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(24.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.size(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Safe to Spend ($budgetCycle)",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = onEditSafeToSpend,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Settings,
                                        contentDescription = "Edit Safe to Spend Limit",
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = "$currency${"%.2f".format(safeToSpend)}",
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 20.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==================== CHART SECTION WITH NAVIGATION ====================

enum class ChartPeriod(val label: String) {
    ALL("All"), DAILY("Day"), WEEKLY("Week"), MONTHLY("Month")
}

enum class ChartType(val label: String) {
    DONUT("Donut"), BAR("Bar")
}

@Composable
fun ChartSection(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    currencySymbol: String,
    onUpdateTransaction: (TransactionEntity) -> Unit
) {
    if (transactions.isEmpty()) return

    var selectedPeriod by remember { mutableStateOf(ChartPeriod.ALL) }
    var selectedChartType by remember { mutableStateOf(ChartType.DONUT) }
    var periodOffset by remember { mutableIntStateOf(0) } // 0 = current, -1 = previous, etc.

    // Reset offset when period type changes
    val periodLabel = remember(selectedPeriod, periodOffset) {
        getPeriodLabel(selectedPeriod, periodOffset)
    }

    val filtered by remember(transactions, selectedPeriod, periodOffset) {
        derivedStateOf { filterByPeriod(transactions, selectedPeriod, periodOffset) }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Spending Analytics",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Chart type toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChartType.entries.forEach { type ->
                    FilterChip(
                        selected = selectedChartType == type,
                        onClick = { selectedChartType = type },
                        label = { Text(type.label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Period selector with navigation arrows
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Period chips
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ChartPeriod.entries.forEach { period ->
                        FilterChip(
                            selected = selectedPeriod == period,
                            onClick = {
                                selectedPeriod = period
                                periodOffset = 0
                            },
                            label = { Text(period.label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.tertiary,
                                selectedLabelColor = MaterialTheme.colorScheme.onTertiary
                            )
                        )
                    }
                }
            }

            // Date navigation row (hidden for "All" period)
            if (selectedPeriod != ChartPeriod.ALL) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { periodOffset-- }) {
                        Icon(Icons.Default.ChevronLeft, "Previous")
                    }
                    Text(
                        periodLabel,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = { if (periodOffset < 0) periodOffset++ },
                        enabled = periodOffset < 0
                    ) {
                        Icon(Icons.Default.ChevronRight, "Next")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (selectedChartType) {
                ChartType.DONUT -> DonutChart(
                    transactions = filtered,
                    categories = categories,
                    currencySymbol = currencySymbol,
                    onUpdateTransaction = onUpdateTransaction
                )
                ChartType.BAR -> BarChart(filtered, currencySymbol)
            }
        }
    }
}

// ========================== DONUT CHART ==========================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonutChart(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    currencySymbol: String,
    onUpdateTransaction: (TransactionEntity) -> Unit
) {
    val expenses = transactions.filter { !it.isIncome }
    if (expenses.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No expenses in this period",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val grouped = remember(expenses) {
        expenses.groupBy { it.tag.ifEmpty { "Other" } }
            .mapValues { it.value.sumOf { tx -> tx.amount } }
            .toList()
            .sortedByDescending { it.second }
    }
    val total = grouped.sumOf { it.second }

    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var showAllCategories by remember { mutableStateOf(false) }
    var transactionToCategorize by remember { mutableStateOf<TransactionEntity?>(null) }

    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val textColor = MaterialTheme.colorScheme.onSurface
    val subTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val categoryLookup = remember(categories) {
        categories.associateBy { it.name.lowercase(Locale.getDefault()) }
    }
    val expenseCategories = remember(categories) {
        categories.filter { it.type == CategoryType.EXPENSE }
    }
    val legendItems = remember(grouped, categoryLookup) {
        grouped.mapIndexed { index, (label, value) ->
            val category = categoryLookup[label.lowercase(Locale.getDefault())]
            LegendCategoryItem(
                name = label,
                amount = value,
                percentage = if (total == 0.0) 0.0 else value / total * 100,
                basicCategory = category?.basicCategory ?: "Other",
                color = ChartColors[index % ChartColors.size]
            )
        }
    }
    val visibleItems = if (showAllCategories) legendItems else legendItems.take(6)

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier
                .size(160.dp)
                .pointerInput(grouped) {
                    detectTapGestures { offset ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val dx = offset.x - center.x
                        val dy = offset.y - center.y
                        val distance = kotlin.math.hypot(dx.toDouble(), dy.toDouble())
                        val radius = (kotlin.math.min(size.width, size.height).toFloat() - 32f) / 2
                        
                        if (distance > radius - 40f && distance < radius + 40f) {
                            val angle = Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            val adjustedAngle = (angle + 450f) % 360f
                            
                            var currentStart = 0f
                            for ((label, value) in grouped) {
                                val sweep = (value.toFloat() / total.toFloat()) * 360f
                                if (adjustedAngle >= currentStart && adjustedAngle < currentStart + sweep) {
                                    selectedCategory = label
                                    showBottomSheet = true
                                    break
                                }
                                currentStart += sweep
                            }
                        }
                    }
                }
            ) {
                val strokeWidth = 32f
                val radius = (size.minDimension - strokeWidth) / 2
                val topLeft = Offset(
                    (size.width - radius * 2) / 2,
                    (size.height - radius * 2) / 2
                )
                val arcSize = Size(radius * 2, radius * 2)

                // Background ring
                drawArc(
                    color = surfaceVariant,
                    startAngle = 0f, sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft, size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Slices
                var currentAngle = -90f
                grouped.forEachIndexed { index, (_, value) ->
                    val sweep = (value.toFloat() / total.toFloat()) * 360f
                    drawArc(
                        color = ChartColors[index % ChartColors.size],
                        startAngle = currentAngle,
                        sweepAngle = sweep.coerceAtLeast(2f), // minimum visible slice
                        useCenter = false,
                        topLeft = topLeft, size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                    )
                    currentAngle += sweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${grouped.size}",
                    fontWeight = FontWeight.Black, fontSize = 26.sp,
                    color = textColor
                )
                Text("Categories", fontSize = 11.sp, color = subTextColor)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        visibleItems
            .groupBy { if (showAllCategories) it.basicCategory else "" }
            .forEach { (basicCategory, items) ->
                if (showAllCategories && basicCategory.isNotBlank()) {
                    Text(
                        basicCategory,
                        color = subTextColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedCategory = item.name
                                showBottomSheet = true
                            }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(item.color)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(item.name, fontSize = 13.sp, color = textColor)
                                if (!showAllCategories) {
                                    Text(
                                        item.basicCategory,
                                        fontSize = 11.sp,
                                        color = subTextColor
                                    )
                                }
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "$currencySymbol${"%.2f".format(item.amount)}",
                                fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                            Text(
                                "${"%.1f".format(item.percentage)}%",
                                fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                                color = subTextColor
                            )
                        }
                    }
                }
            }

        if (legendItems.size > 6) {
            TextButton(
                onClick = { showAllCategories = !showAllCategories },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(if (showAllCategories) "Show Less" else "See Every Category")
            }
        }
    }

    transactionToCategorize?.let { tx ->
        AlertDialog(
            onDismissRequest = { transactionToCategorize = null },
            title = { Text("Categorize Transaction") },
            text = {
                LazyColumn(modifier = Modifier.height(220.dp)) {
                    item {
                        Text(
                            tx.description.ifEmpty { "Transaction" },
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                    item {
                        FilterChip(
                            selected = tx.tag == CategoryCatalog.UNCATEGORIZED,
                            onClick = {
                                onUpdateTransaction(
                                    tx.copy(
                                        categoryId = null,
                                        tag = CategoryCatalog.UNCATEGORIZED
                                    )
                                )
                                transactionToCategorize = null
                            },
                            label = { Text(CategoryCatalog.UNCATEGORIZED) }
                        )
                    }
                    items(expenseCategories.size) { index ->
                        val category = expenseCategories[index]
                        FilterChip(
                            selected = tx.categoryId == category.id,
                            onClick = {
                                onUpdateTransaction(
                                    tx.copy(
                                        categoryId = category.id,
                                        tag = category.name
                                    )
                                )
                                transactionToCategorize = null
                            },
                            label = { Text("${category.emoji} ${category.name}") },
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { transactionToCategorize = null }) {
                    Text("Done")
                }
            }
        )
    }

    if (showBottomSheet && selectedCategory != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                Text(
                    text = "$selectedCategory Transactions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                val catTransactions = expenses.filter { it.tag.ifEmpty { "Other" } == selectedCategory }
                LazyColumn(modifier = Modifier.padding(bottom = 40.dp)) {
                    items(catTransactions.size) { i ->
                        val tx = catTransactions[i]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(tx.description.ifEmpty { "Unknown" }, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                Text(
                                    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(tx.timestamp)),
                                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                TextButton(
                                    onClick = { transactionToCategorize = tx },
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Text(
                                        if (tx.tag == CategoryCatalog.UNCATEGORIZED) "Categorize" else "Change Category"
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    tx.tag.ifEmpty { CategoryCatalog.UNCATEGORIZED },
                                    fontSize = 12.sp,
                                    color = if (tx.tag == CategoryCatalog.UNCATEGORIZED) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    }
                                )
                                Text(
                                    "-$currencySymbol${"%.2f".format(tx.amount)}",
                                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class LegendCategoryItem(
    val name: String,
    val amount: Double,
    val percentage: Double,
    val basicCategory: String,
    val color: Color
)

// ========================== BAR CHART ==========================

@Composable
fun BarChart(transactions: List<TransactionEntity>, currencySymbol: String) {
    val totalIncome = transactions.filter { it.isIncome }.sumOf { it.amount }
    val totalExpense = transactions.filter { !it.isIncome }.sumOf { it.amount }
    val maxVal = maxOf(totalIncome, totalExpense, 1.0)

    if (totalIncome == 0.0 && totalExpense == 0.0) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No data in this period",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val incomeColor = Color(0xFF00CEC9)
    val expenseColor = Color(0xFFFF6B6B)
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Column {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .padding(top = 8.dp)
        ) {
            val barWidth = size.width * 0.25f
            val spacing = size.width * 0.15f
            val incomeX = size.width / 2 - barWidth - spacing / 2
            val expenseX = size.width / 2 + spacing / 2
            val maxBarH = size.height - 36f

            // Income bar background
            drawRoundRect(
                color = surfaceVariant,
                topLeft = Offset(incomeX, 0f),
                size = Size(barWidth, maxBarH),
                cornerRadius = CornerRadius(12f)
            )
            // Income bar fill
            val incomeH = (totalIncome / maxVal).toFloat() * maxBarH
            if (incomeH > 0) {
                drawRoundRect(
                    color = incomeColor,
                    topLeft = Offset(incomeX, maxBarH - incomeH),
                    size = Size(barWidth, incomeH),
                    cornerRadius = CornerRadius(12f)
                )
            }
            val incomeLbl = textMeasurer.measure("Income", labelStyle)
            drawText(
                incomeLbl,
                topLeft = Offset(
                    incomeX + barWidth / 2 - incomeLbl.size.width / 2,
                    maxBarH + 6f
                )
            )

            // Expense bar background
            drawRoundRect(
                color = surfaceVariant,
                topLeft = Offset(expenseX, 0f),
                size = Size(barWidth, maxBarH),
                cornerRadius = CornerRadius(12f)
            )
            // Expense bar fill
            val expenseH = (totalExpense / maxVal).toFloat() * maxBarH
            if (expenseH > 0) {
                drawRoundRect(
                    color = expenseColor,
                    topLeft = Offset(expenseX, maxBarH - expenseH),
                    size = Size(barWidth, expenseH),
                    cornerRadius = CornerRadius(12f)
                )
            }
            val expenseLbl = textMeasurer.measure("Expense", labelStyle)
            drawText(
                expenseLbl,
                topLeft = Offset(
                    expenseX + barWidth / 2 - expenseLbl.size.width / 2,
                    maxBarH + 6f
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Income", fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "$currencySymbol${"%.2f".format(totalIncome)}",
                    fontWeight = FontWeight.Bold, color = incomeColor, fontSize = 16.sp
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Expense", fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "$currencySymbol${"%.2f".format(totalExpense)}",
                    fontWeight = FontWeight.Bold, color = expenseColor, fontSize = 16.sp
                )
            }
        }
    }
}

// ========================== PERIOD HELPERS ==========================

private fun getPeriodLabel(period: ChartPeriod, offset: Int): String {
    if (period == ChartPeriod.ALL) return "All Time"
    val cal = Calendar.getInstance()
    val fmt: SimpleDateFormat
    when (period) {
        ChartPeriod.DAILY -> {
            cal.add(Calendar.DAY_OF_YEAR, offset)
            fmt = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())
        }
        ChartPeriod.WEEKLY -> {
            cal.add(Calendar.WEEK_OF_YEAR, offset)
            cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
            val start = SimpleDateFormat("dd MMM", Locale.getDefault()).format(cal.time)
            cal.add(Calendar.DAY_OF_WEEK, 6)
            val end = SimpleDateFormat("dd MMM", Locale.getDefault()).format(cal.time)
            return "$start – $end"
        }
        ChartPeriod.MONTHLY -> {
            cal.add(Calendar.MONTH, offset)
            fmt = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        }
        else -> return "All Time"
    }
    return fmt.format(cal.time)
}

private fun filterByPeriod(
    transactions: List<TransactionEntity>,
    period: ChartPeriod,
    offset: Int
): List<TransactionEntity> {
    if (period == ChartPeriod.ALL) return transactions
    val cal = Calendar.getInstance()

    when (period) {
        ChartPeriod.DAILY -> {
            cal.add(Calendar.DAY_OF_YEAR, offset)
            val startOfDay = cal.clone() as Calendar
            startOfDay.set(Calendar.HOUR_OF_DAY, 0)
            startOfDay.set(Calendar.MINUTE, 0)
            startOfDay.set(Calendar.SECOND, 0)
            startOfDay.set(Calendar.MILLISECOND, 0)
            val endOfDay = startOfDay.clone() as Calendar
            endOfDay.add(Calendar.DAY_OF_YEAR, 1)
            return transactions.filter {
                it.timestamp >= startOfDay.timeInMillis && it.timestamp < endOfDay.timeInMillis
            }
        }
        ChartPeriod.WEEKLY -> {
            cal.add(Calendar.WEEK_OF_YEAR, offset)
            cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val weekStart = cal.timeInMillis
            cal.add(Calendar.DAY_OF_WEEK, 7)
            val weekEnd = cal.timeInMillis
            return transactions.filter { it.timestamp in weekStart until weekEnd }
        }
        ChartPeriod.MONTHLY -> {
            cal.add(Calendar.MONTH, offset)
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val monthStart = cal.timeInMillis
            cal.add(Calendar.MONTH, 1)
            val monthEnd = cal.timeInMillis
            return transactions.filter { it.timestamp in monthStart until monthEnd }
        }
        else -> return transactions
    }
}

// ========================== RECENT TRANSACTIONS ==========================

@Composable
fun RecentTransactionsWidget(
    transactions: List<TransactionEntity>,
    currency: String,
    onViewAll: () -> Unit
) {
    if (transactions.isEmpty()) return
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Recent Transactions",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp
                )
                TextButton(onClick = onViewAll) {
                    Text(
                        "View All",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            transactions.forEach { tx ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            tx.description.ifEmpty { "Unknown" },
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${tx.tag} - ${dateFormatter.format(Date(tx.timestamp))}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    val amountColor =
                        if (tx.isIncome) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    val sign = if (tx.isIncome) "+" else "-"
                    Text(
                        "$sign$currency${"%.2f".format(tx.amount)}",
                        fontWeight = FontWeight.ExtraBold,
                        color = amountColor,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
