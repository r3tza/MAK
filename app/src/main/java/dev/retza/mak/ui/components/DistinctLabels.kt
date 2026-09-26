package dev.retza.mak.ui.components

/**
 * Makes repeated labels distinguishable by appending their order among equal names,
 * e.g. "Semestr zimowy (1)" and "Semestr zimowy (2)". Unique labels stay unchanged.
 */
fun distinctLabels(names: List<String>): List<String> {
    val totals = names.groupingBy { it }.eachCount()
    val seen = mutableMapOf<String, Int>()
    return names.map { name ->
        if (totals.getValue(name) > 1) {
            val index = (seen[name] ?: 0) + 1
            seen[name] = index
            "$name ($index)"
        } else {
            name
        }
    }
}
