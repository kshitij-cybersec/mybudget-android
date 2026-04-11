package com.mybudget.domain

import com.mybudget.data.local.entity.CategoryEntity
import java.util.Locale

object CategoryType {
    const val EXPENSE = "EXPENSE"
    const val INCOME = "INCOME"
}

data class CategorySeed(
    val name: String,
    val emoji: String,
    val basicCategory: String,
    val type: String,
    val keywords: List<String>
)

object CategoryCatalog {
    const val UNCATEGORIZED = "Uncategorized"

    val defaultCategories = listOf(
        CategorySeed("Food & Dining", "🍽️", "Lifestyle", CategoryType.EXPENSE, listOf("swiggy", "zomato", "kfc", "dominos", "popeyes", "burger king", "mcdonald", "pizza", "deliveroo", "cafe", "coffee", "street food")),
        CategorySeed("Groceries", "🛒", "Essentials", CategoryType.EXPENSE, listOf("tesco", "lidl", "aldi", "marks & spencer", "spar", "lotts", "supermarket", "grocery")),
        CategorySeed("Transportation", "🚌", "Mobility", CategoryType.EXPENSE, listOf("uber", "rapido", "ola", "metro", "irctc", "petrol", "fuel", "bus", "train")),
        CategorySeed("Shopping", "🛍️", "Lifestyle", CategoryType.EXPENSE, listOf("amazon", "flipkart", "myntra", "ekart", "store", "mart")),
        CategorySeed("Entertainment", "🎬", "Lifestyle", CategoryType.EXPENSE, listOf("netflix", "spotify", "hotstar", "bigtree", "movie", "cinema")),
        CategorySeed("Subscriptions", "📱", "Bills", CategoryType.EXPENSE, listOf("google", "apple", "subscription", "premium")),
        CategorySeed("Utilities", "💡", "Bills", CategoryType.EXPENSE, listOf("electricity", "broadband", "recharge", "jio", "airtel", "bsnl", "wifi", "water", "gas")),
        CategorySeed("Insurance", "🛡️", "Protection", CategoryType.EXPENSE, listOf("insurance", "lic", "policy")),
        CategorySeed("Loan", "💳", "Finance", CategoryType.EXPENSE, listOf("emi", "loan")),
        CategorySeed("Housing", "🏠", "Essentials", CategoryType.EXPENSE, listOf("rent", "maintenance", "housing")),
        CategorySeed("Transfer", "🔁", "Finance", CategoryType.EXPENSE, listOf("neft", "imps", "upi", "rtgs", "transfer")),
        CategorySeed("Auto-Debit", "🧾", "Bills", CategoryType.EXPENSE, listOf("nach", "autopay", "auto debit")),
        CategorySeed("Bills", "📄", "Bills", CategoryType.EXPENSE, listOf("bill paid", "bill", "payment")),
        CategorySeed("Education", "🎓", "Growth", CategoryType.EXPENSE, listOf("college", "university", "dcu", "school", "course")),
        CategorySeed("Cash Withdrawal", "💵", "Cash", CategoryType.EXPENSE, listOf("atm", "cash wdl", "withdrawal")),
        CategorySeed("Medical", "💊", "Essentials", CategoryType.EXPENSE, listOf("hospital", "clinic", "pharmacy", "medicine")),
        CategorySeed("Income", "💰", "Income", CategoryType.INCOME, listOf("salary", "income", "credit")),
        CategorySeed("Refund", "↩️", "Income", CategoryType.INCOME, listOf("refund", "reversal")),
        CategorySeed("Cashback", "🎁", "Income", CategoryType.INCOME, listOf("cashback", "reward")),
        CategorySeed("Interest", "🏦", "Income", CategoryType.INCOME, listOf("interest", "int.pd")),
        CategorySeed("Dividend", "📈", "Income", CategoryType.INCOME, listOf("dividend")),
        CategorySeed("Transfer In", "⬇️", "Income", CategoryType.INCOME, listOf("rev-upi", "neftinw", "recd:imps", "transfer from")),
        CategorySeed("Investment Maturity", "📊", "Income", CategoryType.INCOME, listOf("sweep trf from", "fd premat", "sweep"))
    )

    fun toEntities(): List<CategoryEntity> = defaultCategories.map { seed ->
        CategoryEntity(
            name = seed.name,
            emoji = seed.emoji,
            keywords = seed.keywords.joinToString(","),
            basicCategory = seed.basicCategory,
            type = seed.type,
            isCustom = false
        )
    }

    fun basicGroupsFor(type: String): List<String> = defaultCategories
        .filter { it.type == type }
        .map { it.basicCategory }
        .distinct()
        .sorted()

    fun emojiForBasicCategory(basicCategory: String, type: String): String {
        return defaultCategories.firstOrNull {
            it.basicCategory.equals(basicCategory, ignoreCase = true) && it.type == type
        }?.emoji ?: if (type == CategoryType.INCOME) "💰" else "🏷️"
    }

    fun findMatchingCategory(
        description: String,
        suggestedTag: String,
        isIncome: Boolean,
        categories: List<CategoryEntity>
    ): CategoryEntity? {
        val type = if (isIncome) CategoryType.INCOME else CategoryType.EXPENSE
        val lowerHint = suggestedTag.trim().lowercase(Locale.getDefault())
        val lowerDescription = description.lowercase(Locale.getDefault())
        val eligible = categories.filter { it.type == type }

        eligible.firstOrNull { it.name.lowercase(Locale.getDefault()) == lowerHint }?.let { return it }

        return eligible.firstOrNull { category ->
            val keywords = category.keywords.split(",")
                .map { it.trim().lowercase(Locale.getDefault()) }
                .filter { it.isNotBlank() }
            lowerDescription.contains(category.name.lowercase(Locale.getDefault())) ||
                lowerHint.contains(category.name.lowercase(Locale.getDefault())) ||
                keywords.any { keyword -> lowerDescription.contains(keyword) || lowerHint.contains(keyword) }
        }
    }
}
