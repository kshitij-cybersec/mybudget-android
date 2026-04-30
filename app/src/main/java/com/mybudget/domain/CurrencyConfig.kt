package com.mybudget.domain

/**
 * Single source of truth for all supported currencies.
 *
 * Each entry maps an ISO-4217 code to its display symbol and human-friendly label.
 * Used by the onboarding screen, settings screen, dashboard, scanner, and
 * transaction list to keep currency information consistent.
 */
data class CurrencyInfo(
    val code: String,
    val symbol: String,
    val label: String          // e.g. "USD ($)"
)

object CurrencyConfig {

    /** Ordered list of all supported currencies. */
    val all: List<CurrencyInfo> = listOf(
        // Major Western currencies
        CurrencyInfo("USD", "$",    "USD ($)"),
        CurrencyInfo("EUR", "€",    "EUR (€)"),
        CurrencyInfo("GBP", "£",    "GBP (£)"),
        CurrencyInfo("CHF", "CHF ", "CHF (Fr)"),

        // South Asia
        CurrencyInfo("INR", "₹",    "INR (₹)"),
        CurrencyInfo("BDT", "৳",    "BDT (৳)"),

        // East & South-East Asia
        CurrencyInfo("JPY", "¥",    "JPY (¥)"),
        CurrencyInfo("CNY", "¥",    "CNY (¥)"),
        CurrencyInfo("KRW", "₩",    "KRW (₩)"),
        CurrencyInfo("SGD", "S$",   "SGD (S$)"),
        CurrencyInfo("THB", "฿",    "THB (฿)"),

        // Oceania
        CurrencyInfo("AUD", "A$",   "AUD (A$)"),
        CurrencyInfo("NZD", "NZ$",  "NZD (NZ$)"),

        // Americas
        CurrencyInfo("CAD", "C$",   "CAD (C$)"),
        CurrencyInfo("BRL", "R$",   "BRL (R$)"),
        CurrencyInfo("MXN", "MX$",  "MXN (MX$)"),

        // Middle East
        CurrencyInfo("AED", "د.إ",  "AED (د.إ)"),
        CurrencyInfo("SAR", "﷼",    "SAR (﷼)"),

        // Europe (non-EUR)
        CurrencyInfo("SEK", "kr ",  "SEK (kr)"),
        CurrencyInfo("PLN", "zł",   "PLN (zł)"),
        CurrencyInfo("TRY", "₺",    "TRY (₺)"),
        CurrencyInfo("RUB", "₽",    "RUB (₽)"),

        // Africa
        CurrencyInfo("ZAR", "R ",   "ZAR (R)"),
        CurrencyInfo("NGN", "₦",    "NGN (₦)")
    )

    /** Labels suitable for dropdown / picker lists. */
    val labels: List<String> = all.map { it.label }

    private val byCode: Map<String, CurrencyInfo> =
        all.associateBy { it.code }

    /**
     * Return the display symbol for the given ISO-4217 [code].
     * Falls back to "$" if the code is unknown.
     */
    fun symbolFor(code: String): String =
        byCode[code]?.symbol ?: "$"

    /**
     * Return the dropdown label for the given ISO-4217 [code].
     * Falls back to the first entry if the code is unknown.
     */
    fun labelFor(code: String): String =
        byCode[code]?.label ?: all.first().label
}
