package com.wbtracker.app.util

object WbArticleExtractor {
    private val regexes = listOf(
        Regex("""catalog/(\d{5,12})""", RegexOption.IGNORE_CASE),       // https://wildberries.ru/catalog/12345678/detail.aspx
        Regex("""product/(\d{5,12})""", RegexOption.IGNORE_CASE),       // https://wb.ru/product/12345678
        Regex("""card/(\d{5,12})""", RegexOption.IGNORE_CASE),          // https://wb.ru/card/12345678
        Regex("""[?&]nm=(\d{5,12})""", RegexOption.IGNORE_CASE),        // ?nm=12345678 or &nm=12345678
        Regex("""[?&]article=(\d{5,12})""", RegexOption.IGNORE_CASE),   // ?article=12345678
        Regex("""[?&]itemId=(\d{5,12})""", RegexOption.IGNORE_CASE),    // ?itemId=12345678
        Regex("""[?&]id=(\d{5,12})""", RegexOption.IGNORE_CASE),        // ?id=12345678
        Regex("""\b(\d{5,12})\b""")                                    // Чистый артикул: 12345678
    )

    fun extractArticleId(input: String): Long? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null

        for (regex in regexes) {
            val match = regex.find(trimmed)
            if (match != null) {
                return match.groupValues[1].toLongOrNull()
            }
        }
        return trimmed.filter { it.isDigit() }.toLongOrNull()
    }
}
