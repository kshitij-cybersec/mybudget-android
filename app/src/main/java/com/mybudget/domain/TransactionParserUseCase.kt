package com.mybudget.domain

import com.mybudget.data.local.entity.TransactionEntity
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

data class ParsedStatementResult(
    val transactions: List<TransactionEntity>,
    val detectedOpeningBalance: Double?
)

class TransactionParserUseCase {

    private val categoryDictionary = mapOf(
        "swiggy" to "Food & Dining", "zomato" to "Food & Dining",
        "kfc" to "Food & Dining", "dominos" to "Food & Dining",
        "popeyes" to "Food & Dining", "burger king" to "Food & Dining",
        "mcdonald" to "Food & Dining", "pizza" to "Food & Dining",
        "deliveroo" to "Food & Dining", "cafe" to "Food & Dining",
        "coffee" to "Food & Dining", "street food" to "Food & Dining",
        "uber" to "Transportation", "rapido" to "Transportation",
        "ola" to "Transportation", "metro" to "Transportation",
        "irctc" to "Transportation", "petrol" to "Transportation",
        "amazon" to "Shopping", "flipkart" to "Shopping",
        "myntra" to "Shopping", "ekart" to "Shopping",
        "tesco" to "Groceries", "lidl" to "Groceries", 
        "aldi" to "Groceries", "marks & spencer" to "Groceries",
        "spar" to "Groceries", "lotts" to "Groceries", "supermarket" to "Groceries",
        "netflix" to "Entertainment", "spotify" to "Entertainment",
        "hotstar" to "Entertainment", "bigtree" to "Entertainment",
        "google" to "Subscriptions", "apple" to "Subscriptions",
        "salary" to "Income", "dividend" to "Income",
        "interest" to "Income", "int.pd" to "Income",
        "refund" to "Income", "cashback" to "Income",
        "rev-upi" to "Income", "top-up" to "Income",
        "atm" to "Cash Withdrawal", "cash wdl" to "Cash Withdrawal",
        "electricity" to "Utilities", "broadband" to "Utilities",
        "recharge" to "Utilities", "jio" to "Utilities",
        "airtel" to "Utilities", "bsnl" to "Utilities",
        "insurance" to "Insurance", "lic" to "Insurance",
        "emi" to "Loan", "loan" to "Loan",
        "rent" to "Housing", "sweep" to "Sweep/FD",
        "neft" to "Transfer", "imps" to "Transfer",
        "upi" to "Transfer", "rtgs" to "Transfer",
        "nach" to "Auto-Debit", "bill paid" to "Bills",
        "college" to "Education", "university" to "Education", "dcu" to "Education"
    )

    private data class TxLine(
        val line: String, val dateStr: String,
        val amounts: List<Double>, val desc: String
    )

    private val amountRegex = Regex("""(?<!\d)(\d{1,3}(?:[.,\s]\d{3})*[.,]\d{2})(?!\d)""")

    private val dateRegex = Regex(
        """(\d{1,2}[-/.]\d{1,2}[-/.]\d{2,4}""" +
        """|(\d{1,2})[-/.\s]+(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)(?!\w)(?:[-/.\s]+\d{2,4})?)""",
        RegexOption.IGNORE_CASE
    )

    private val endMarkers = listOf(
        "account summary", "end of statement", "commonly used narrations",
        "important information", "discrepancy in the statement"
    )

    fun parseRawText(rawText: String, defaultCurrency: String): List<TransactionEntity> {
        return parseStatement(rawText, defaultCurrency).transactions
    }

    fun parseStatement(rawText: String, defaultCurrency: String): ParsedStatementResult {
        val allLines = rawText.split("\n", "\r").map { it.trim() }.filter { it.isNotEmpty() }

        // 1. Stop at end-of-transactions markers
        val endIdx = allLines.indexOfFirst { line ->
            endMarkers.any { line.lowercase(Locale.getDefault()).contains(it) }
        }.let { if (it < 0) allLines.size else it }
        val lines = allLines.subList(0, endIdx)

        // 2. Extract opening balance
        var openingBalance: Double? = null
        for (line in lines) {
            if (line.lowercase(Locale.getDefault()).contains("opening balance")) {
                val amts = amountRegex.findAll(line).map { normalizeAmount(it.value) }
                    .filter { it >= 1.0 }.toList()
                if (amts.isNotEmpty()) openingBalance = amts.last()
                break
            }
        }

        // 3. Collect transaction lines (date + amounts)
        val txLines = mutableListOf<TxLine>()
        for (line in lines) {
            if (isNoiseLine(line)) continue
            val dateMatch = dateRegex.find(line) ?: continue
            val dateStr = dateMatch.value
            val amounts = amountRegex.findAll(line).map { normalizeAmount(it.value) }
                .filter { it >= 1.0 }.toList()
            if (amounts.size < 2) continue

            var desc = line.replace(dateStr, "")
            amountRegex.findAll(line).forEach { desc = desc.replaceFirst(it.value, " ") }
            desc = desc.replace(Regex("""^\d+\s+"""), "")
                .replace(Regex("""[A-Z]+-\d{8,}"""), "")
                .replace(Regex("""[€${'$'}£₹]"""), "")
                .replace(Regex("""\s{2,}"""), " ").trim()
                .trim('-', '/', '|', ':', '.', ' ')

            txLines.add(TxLine(line, dateStr, amounts, desc))
        }

        if (txLines.isEmpty()) return ParsedStatementResult(emptyList(), openingBalance)

        // 4. Balance-delta method
        val results = mutableListOf<TransactionEntity>()
        var prevBalance: Double

        if (openingBalance != null) {
            prevBalance = openingBalance
        } else {
            // No opening balance: handle first line separately
            val first = txLines[0]
            if (first.amounts.size >= 2) {
                val txAmt = first.amounts.first()
                val bal = first.amounts.last()
                val isInc = inferIncomeFromText(first.line, first.desc)
                results.add(buildTx(first, txAmt, isInc, defaultCurrency))
                prevBalance = bal
            } else {
                prevBalance = first.amounts.last()
            }
            // Process rest starting from index 1
            for (i in 1 until txLines.size) {
                val tl = txLines[i]
                val curBal = tl.amounts.last()
                val delta = curBal - prevBalance
                if (abs(delta) < 0.01) { prevBalance = curBal; continue }
                results.add(buildTx(tl, abs(delta), delta > 0, defaultCurrency))
                prevBalance = curBal
            }
            return ParsedStatementResult(results, openingBalance)
        }

        // Process ALL lines when opening balance is known
        for (tl in txLines) {
            val curBal = tl.amounts.last()
            val delta = curBal - prevBalance
            if (abs(delta) < 0.01) { prevBalance = curBal; continue }
            results.add(buildTx(tl, abs(delta), delta > 0, defaultCurrency))
            prevBalance = curBal
        }
        return ParsedStatementResult(results, openingBalance)
    }

    private fun isNoiseLine(line: String): Boolean {
        if (line.length < 10) return true
        val l = line.lowercase(Locale.getDefault())
        if (l.contains("account no")) return true
        if (l.contains("account statement")) return true
        if (l.contains("ifsc") || l.contains("micr")) return true
        if (l.contains("branch") && !l.contains("upi")) return true
        if (Regex("""page\s+\d""").containsMatchIn(l)) return true
        if (l.contains("statement generated")) return true
        if (l.contains("savings account transactions")) return true
        if (l.contains("opening balance")) return true
        if (Regex("""^[\s*\-=_#|]+$""").matches(line)) return true
        if (Regex("""^[A-Z\s]+$""").matches(line) && line.length < 40) return true
        val headers = listOf("particulars", "chq/ref", "withdrawal (dr", "deposit (cr")
        if (headers.count { l.contains(it) } >= 2) return true
        return false
    }

    private fun inferIncomeFromText(line: String, desc: String): Boolean {
        val l = line.lowercase(Locale.getDefault())
        return l.contains("rev-upi") || l.contains("refund") ||
               l.contains("recd:imps") || l.contains("neftinw") ||
               l.contains("sweep trf from") || l.contains("fd premat") ||
               l.contains("int.pd")
    }

    private fun buildTx(
        tl: TxLine, amount: Double, isIncome: Boolean, currency: String
    ): TransactionEntity {
        val lower = tl.line.lowercase(Locale.getDefault())
        var category = CategoryCatalog.UNCATEGORIZED
        for ((kw, cat) in categoryDictionary) {
            if (lower.contains(kw)) { category = cat; break }
        }
        val cleanDesc = tl.desc.replace(Regex("""\d{10,}"""), "")
            .replace(Regex("""\s{2,}"""), " ").trim()
            .ifBlank { if (isIncome) "Credit" else "Debit" }

        return TransactionEntity(
            amount = amount, isIncome = isIncome, categoryId = null,
            tag = category,
            timestamp = parseTimestamp(tl.dateStr),
            currency = currency,
            description = cleanDesc
        )
    }

    private fun normalizeAmount(amount: String): Double {
        val s = amount.replace("+", "").replace("-", "").replace(Regex("""[^\d.,]"""), "")
        if (s.isEmpty()) return 0.0
        
        val lastComma = s.lastIndexOf(',')
        val lastDot = s.lastIndexOf('.')
        val lastSeparator = maxOf(lastComma, lastDot)
        
        if (lastSeparator == -1) return s.toDoubleOrNull() ?: 0.0
        
        val beforeDec = s.substring(0, lastSeparator).replace(".", "").replace(",", "")
        val afterDec = s.substring(lastSeparator + 1)
        
        return "$beforeDec.$afterDec".toDoubleOrNull() ?: 0.0
    }

    private fun parseTimestamp(dateStr: String): Long {
        var cleaned = dateStr.replace(",", "").trim()
        if (!cleaned.matches(Regex(".*\\d{4}.*")) && !cleaned.matches(Regex(".*\\d{1,2}[-/.]\\d{1,2}[-/.]\\d{2}.*"))) {
            val year = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
            cleaned = "$cleaned $year"
        }
        
        val formats = listOf(
            "dd/MM/yyyy", "dd-MM-yyyy", "dd.MM.yyyy",
            "dd/MM/yy", "dd-MM-yy", "dd MMM yyyy", "d MMM yyyy",
            "dd-MMM-yyyy", "d-MMM-yyyy", "dd/MMM/yyyy", "d/MMM/yyyy"
        )
        for (fmt in formats) {
            try {
                val p = SimpleDateFormat(fmt, Locale.ENGLISH).apply { isLenient = false }
                p.parse(cleaned)?.let { return it.time }
            } catch (_: ParseException) {}
        }
        return System.currentTimeMillis()
    }
}
