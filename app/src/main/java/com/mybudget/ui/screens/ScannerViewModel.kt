package com.mybudget.ui.screens

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mybudget.data.local.BudgetDatabase
import com.mybudget.data.local.entity.CategoryEntity
import com.mybudget.data.local.entity.TransactionEntity
import com.mybudget.data.local.parser.ImageOcrExtractor
import com.mybudget.data.local.parser.PdfTextExtractor
import com.mybudget.data.local.prefs.SettingsManager
import com.mybudget.domain.CategoryCatalog
import com.mybudget.domain.CategoryType
import com.mybudget.domain.TransactionParserUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DraftTransactionItem(
    val localId: Int,
    val transaction: TransactionEntity,
    val requiresCategory: Boolean
)

class ScannerViewModel(application: Application) : AndroidViewModel(application) {
    private val transactionDao = BudgetDatabase.getDatabase(application).transactionDao()
    private val categoryDao = BudgetDatabase.getDatabase(application).categoryDao()
    private val pdfExtractor = PdfTextExtractor(application)
    private val ocrExtractor = ImageOcrExtractor(application)
    private val parserUseCase = TransactionParserUseCase()
    private val settingsManager = SettingsManager(application)

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing

    val categories: StateFlow<List<CategoryEntity>> = categoryDao.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _draftTransactions = MutableStateFlow<List<DraftTransactionItem>>(emptyList())
    val draftTransactions: StateFlow<List<DraftTransactionItem>> = _draftTransactions

    private val _scanMessage = MutableStateFlow("")
    val scanMessage: StateFlow<String> = _scanMessage

    private val _rawExtractedText = MutableStateFlow("")
    val rawExtractedText: StateFlow<String> = _rawExtractedText

    private val _detectedOpeningBalance = MutableStateFlow<String?>(null)
    val detectedOpeningBalance: StateFlow<String?> = _detectedOpeningBalance

    fun processPdf(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _isProcessing.value = true
            _scanMessage.value = "Extracting text layers..."

            // 1. Try standard PDFBox extraction
            var rawText = pdfExtractor.extractTextFromUri(uri)

            // 2. Fallback to Deep OCR if text is virtually empty (image-based PDF)
            if (rawText.trim().length < 50 && !rawText.contains("ERROR")) {
                _scanMessage.value = "Image PDF detected. Running ML Kit OCR..."
                rawText = ocrExtractor.extractTextFromScannedPdf(uri)
            }

            if (rawText.contains("ERROR")) {
                _scanMessage.value = "Failed to process PDF."
                _isProcessing.value = false
                return@launch
            }

            _scanMessage.value = "Analyzing transaction mapping..."
            val currency = settingsManager.preferredCurrencyFlow.first()
            _rawExtractedText.value = rawText
            val parsedResult = parserUseCase.parseStatement(rawText, currency)
            val parsedList = parsedResult.transactions
            _detectedOpeningBalance.value = parsedResult.detectedOpeningBalance?.let { "%.2f".format(it) }
            val availableCategories = categories.value.ifEmpty { categoryDao.getAllCategories().first() }
            val drafts = parsedList.mapIndexed { index, tx ->
                val matchedCategory = CategoryCatalog.findMatchingCategory(
                    description = tx.description,
                    suggestedTag = tx.tag,
                    isIncome = tx.isIncome,
                    categories = availableCategories
                )
                val categorizedTx = matchedCategory?.let {
                    tx.copy(categoryId = it.id, tag = it.name)
                } ?: tx.copy(categoryId = null, tag = CategoryCatalog.UNCATEGORIZED)
                DraftTransactionItem(
                    localId = index,
                    transaction = categorizedTx,
                    requiresCategory = matchedCategory == null
                )
            }

            val creditCount = drafts.count { it.transaction.isIncome }
            val debitCount = drafts.count { !it.transaction.isIncome }
            val uncategorizedCount = drafts.count { it.requiresCategory }
            _draftTransactions.value = drafts
            _scanMessage.value = if (uncategorizedCount > 0) {
                "Found ${drafts.size} transactions ($creditCount credits, $debitCount debits). $uncategorizedCount item(s) are still uncategorized and can be fixed later."
            } else {
                "Found ${drafts.size} transactions ($creditCount credits, $debitCount debits)."
            }
            _isProcessing.value = false
        }
    }

    fun updateDetectedOpeningBalance(value: String) {
        _detectedOpeningBalance.value = value
    }

    fun approveTransactions() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = _draftTransactions.value
            if (list.isNotEmpty()) {
                transactionDao.insertTransactions(list.map { it.transaction })
                _detectedOpeningBalance.value?.toDoubleOrNull()?.let { settingsManager.saveOpeningBalance(it) }
                _draftTransactions.value = emptyList()
                _scanMessage.value = "Approved and saved!"
            }
        }
    }

    fun updateDraftCategory(localId: Int, category: CategoryEntity) {
        _draftTransactions.value = _draftTransactions.value.map { item ->
            if (item.localId == localId) {
                item.copy(
                    transaction = item.transaction.copy(
                        categoryId = category.id,
                        tag = category.name
                    ),
                    requiresCategory = false
                )
            } else {
                item
            }
        }
    }

    fun createCustomCategory(
        name: String,
        basicCategory: String,
        isIncome: Boolean,
        onComplete: (CategoryEntity) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val type = if (isIncome) CategoryType.INCOME else CategoryType.EXPENSE
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

    fun clearDrafts() {
        _draftTransactions.value = emptyList()
        _detectedOpeningBalance.value = null
        _scanMessage.value = ""
    }
}
