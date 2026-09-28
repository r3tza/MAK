package dev.retza.mak.ui

internal fun polishPlural(count: Int, one: String, few: String, many: String): String {
    val lastDigit = count % 10
    val lastTwoDigits = count % 100
    val form = when {
        count == 1 -> one
        lastDigit in 2..4 && lastTwoDigits !in 12..14 -> few
        else -> many
    }
    return form
}
