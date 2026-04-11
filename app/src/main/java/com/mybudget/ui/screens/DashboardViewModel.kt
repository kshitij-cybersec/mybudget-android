package com.mybudget.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mybudget.data.local.BudgetDatabase
import com.mybudget.data.local.entity.CategoryEntity
import com.mybudget.data.local.entity.TransactionEntity
import com.mybudget.data.local.prefs.SettingsManager
import com.mybudget.domain.SafeToSpendUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val transactionDao = BudgetDatabase.getDatabase(application).transactionDao()
    private val categoryDao = BudgetDatabase.getDatabase(application).categoryDao()
    private val settingsManager = SettingsManager(application)
    private val safeToSpendUseCase = SafeToSpendUseCase()

    val transactions: StateFlow<List<TransactionEntity>> = transactionDao.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val categories: StateFlow<List<CategoryEntity>> = categoryDao.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val totalBalance: StateFlow<Double> = MutableStateFlow(0.0)
    val safeToSpend: StateFlow<Double> = MutableStateFlow(0.0)
    val budgetCycle: StateFlow<String> = settingsManager.budgetCycleFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, "Monthly")

    init {
        viewModelScope.launch {
            combine(
                transactions, 
                settingsManager.openingBalanceFlow,
                settingsManager.baseSafeToSpendFlow,
                settingsManager.budgetCycleFlow
            ) { list, openingBalance, baseSafeToSpend, cycle ->
                // Custom data class not strictly needed, just return a list or Array
                listOf(list, openingBalance, baseSafeToSpend, cycle)
            }.collect { values ->
                @Suppress("UNCHECKED_CAST")
                val list = values[0] as List<TransactionEntity>
                val openingBalance = values[1] as Double
                val baseSafeToSpend = values[2] as Double
                val cycle = values[3] as String

                var net = openingBalance
                for (tx in list) {
                    if (tx.isIncome) net += tx.amount else net -= tx.amount
                }
                (totalBalance as MutableStateFlow).value = net

                val safeAmount = safeToSpendUseCase.calculateSafeToSpend(list, baseSafeToSpend, cycle)
                (safeToSpend as MutableStateFlow).value = safeAmount
            }
        }
    }

    fun updateBaseSafeToSpend(amount: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsManager.saveBaseSafeToSpend(amount)
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            transactionDao.deleteTransaction(transaction)
        }
    }

    fun updateTransaction(transaction: TransactionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            transactionDao.updateTransaction(transaction)
        }
    }
}
