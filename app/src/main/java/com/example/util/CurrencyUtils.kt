package com.example.util

import java.text.NumberFormat
import java.util.Locale

object CurrencyUtils {
    private val brazilLocale = Locale("pt", "BR")
    private val currencyFormat = NumberFormat.getCurrencyInstance(brazilLocale)

    fun formatBrl(value: Double): String {
        return currencyFormat.format(value)
    }

    fun parseAmount(input: String): Double? {
        val clean = input.trim()
            .replace("R$", "")
            .replace(" ", "")
            .trim()

        if (clean.isEmpty()) return null

        val normalized = if (clean.contains(",") && clean.contains(".")) {
            clean.replace(".", "").replace(",", ".")
        } else if (clean.contains(",")) {
            clean.replace(",", ".")
        } else {
            clean
        }

        return normalized.toDoubleOrNull()
    }

    fun parseQuantity(input: String): Double? {
        val clean = input.trim().replace(",", ".").trim()
        return clean.toDoubleOrNull()
    }

    fun formatQuantity(quantity: Double, unit: String = "un"): String {
        val formattedNumber = if (quantity % 1.0 == 0.0) {
            quantity.toLong().toString()
        } else {
            String.format(brazilLocale, "%.2f", quantity).trimEnd('0').trimEnd(',')
        }
        return if (unit.isNotBlank()) "$formattedNumber $unit" else formattedNumber
    }
}
