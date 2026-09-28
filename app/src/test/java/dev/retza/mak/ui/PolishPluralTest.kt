package dev.retza.mak.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class PolishPluralTest {
    @Test
    fun selectsPolishNumeralFormsAcrossTeensAndHundreds() {
        val cases = listOf(
            1 to "kierunek",
            2 to "kierunki",
            4 to "kierunki",
            5 to "kierunków",
            12 to "kierunków",
            14 to "kierunków",
            21 to "kierunków",
            22 to "kierunki",
            25 to "kierunków",
            112 to "kierunków",
            0 to "kierunków"
        )

        cases.forEach { (count, expected) ->
            assertEquals(expected, polishPlural(count, "kierunek", "kierunki", "kierunków"))
        }
    }
}
