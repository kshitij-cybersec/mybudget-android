package com.mybudget.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mybudget.data.local.dao.CategoryDao
import com.mybudget.data.local.entity.CategoryEntity
import com.mybudget.data.local.dao.TransactionDao
import com.mybudget.data.local.entity.TransactionEntity
import com.mybudget.domain.CategoryCatalog
import com.mybudget.domain.CategoryType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ManualEntryViewModel(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao
) : ViewModel() {
    val categories: StateFlow<List<CategoryEntity>> = categoryDao.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun saveTransaction(
        description: String,
        amount: Double,
        isExpense: Boolean,
        category: CategoryEntity?,
        currency: String,
        timestamp: Long,
        onComplete: () -> Unit
    ) {
        val transaction = TransactionEntity(
            description = description,
            amount = amount,
            isIncome = !isExpense,
            tag = category?.name ?: CategoryCatalog.UNCATEGORIZED,
            categoryId = category?.id,
            timestamp = timestamp,
            currency = currency
        )

        viewModelScope.launch(Dispatchers.IO) {
            transactionDao.insertTransaction(transaction)
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    fun createCustomCategory(
        name: String,
        basicCategory: String,
        isExpense: Boolean,
        onComplete: (CategoryEntity) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val type = if (isExpense) CategoryType.EXPENSE else CategoryType.INCOME
            val trimmedName = name.trim()
            val existing = categoryDao.findCategoryByNameAndType(trimmedName, type)
            val category = existing ?: run {
                val newCategory = CategoryEntity(
                    name = trimmedName,
                    emoji = CategoryCatalog.emojiForBasicCategory(basicCategory, type),
                    keywords = "",
                    basicCategory = basicCategory,
                    type = type,
                    isCustom = true
                )
                val id = categoryDao.insertCategory(newCategory)
                newCategory.copy(id = id)
            }
            withContext(Dispatchers.Main) {
                onComplete(category)
            }
        }
    }
}

class ManualEntryViewModelFactory(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ManualEntryViewModel(transactionDao, categoryDao) as T
    }
}
