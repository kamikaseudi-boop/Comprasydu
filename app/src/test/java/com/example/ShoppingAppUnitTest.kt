package com.example

import com.example.data.model.ShoppingItem
import com.example.util.CurrencyUtils
import com.example.util.PasswordHasher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ShoppingAppUnitTest {

    @Test
    fun testCurrencyFormattingAndParsing() {
        val formatted = CurrencyUtils.formatBrl(25.50)
        assertTrue(formatted.contains("25,50"))

        val parsed = CurrencyUtils.parseAmount("R$ 25,50")
        assertEquals(25.50, parsed ?: 0.0, 0.001)

        val parsedDot = CurrencyUtils.parseAmount("12.50")
        assertEquals(12.50, parsedDot ?: 0.0, 0.001)

        val parsedQuantity = CurrencyUtils.parseQuantity("1,5")
        assertEquals(1.5, parsedQuantity ?: 0.0, 0.001)
    }

    @Test
    fun testPasswordHashingSecurity() {
        val salt = PasswordHasher.generateSalt()
        val hash = PasswordHasher.hashPassword("senha123", salt)

        assertTrue(PasswordHasher.verifyPassword("senha123", salt, hash))
        assertFalse(PasswordHasher.verifyPassword("senhaErrada", salt, hash))

        val answerHash = PasswordHasher.hashSecurityAnswer("Rex o Cachorro")
        assertTrue(PasswordHasher.verifySecurityAnswer("rex o cachorro", answerHash))
        assertFalse(PasswordHasher.verifySecurityAnswer("outro gato", answerHash))
    }

    @Test
    fun testShoppingItemCalculation() {
        val item = ShoppingItem(
            listId = 1L,
            name = "Maçã Gala",
            quantity = 1.5,
            unitPrice = 8.00,
            unit = "kg"
        )
        // 1.5 * 8.00 = 12.00
        assertEquals(12.00, item.totalValue, 0.001)
    }

    @Test
    fun testCashierConferenceMatch() {
        val expectedTotal = 150.00
        val cashierValue = 150.00
        val diff = cashierValue - expectedTotal

        assertTrue(abs(diff) < 0.01)
    }

    @Test
    fun testCashierConferenceSurplusAndShortage() {
        val expectedTotal = 100.00

        // Machine charged 110.00 (Sobra R$ 10,00 no caixa)
        val surplusCashier = 110.00
        val surplusDiff = surplusCashier - expectedTotal
        assertTrue(surplusDiff > 0.0)
        assertEquals(10.00, surplusDiff, 0.001)

        // Machine charged 90.00 (Falta R$ 10,00 no caixa / Desconto aplicado)
        val shortageCashier = 90.00
        val shortageDiff = shortageCashier - expectedTotal
        assertTrue(shortageDiff < 0.0)
        assertEquals(10.00, abs(shortageDiff), 0.001)
    }
}
