package com.wbtracker.app.util

object WbArticleExtractor {
    private val wbDomainRegex = Regex("""(?i)\b(wildberries|wb)\.(ru|by|kz|am|kg|uz)\b""")

    private val regexes = listOf(
        Regex("""catalog/(\d{5,12})""", RegexOption.IGNORE_CASE),       // https://wildberries.ru/catalog/12345678/detail.aspx
        Regex("""product/(\d{5,12})""", RegexOption.IGNORE_CASE),       // https://wb.ru/product/12345678
        Regex("""card/(\d{5,12})""", RegexOption.IGNORE_CASE),          // https://wb.ru/card/12345678
        Regex("""[?&]nm=(\d{5,12})""", RegexOption.IGNORE_CASE),        // ?nm=12345678 or &nm=12345678
        Regex("""[?&]article=(\d{5,12})""", RegexOption.IGNORE_CASE),   // ?article=12345678
        Regex("""[?&]itemId=(\d{5,12})""", RegexOption.IGNORE_CASE),    // ?itemId=12345678
        Regex("""[?&]id=(\d{5,12})""", RegexOption.IGNORE_CASE)         // ?id=12345678
    )

    fun extractArticleId(input: String): Long? {
        val sanitized = input
            .replace("\uFEFF", "")
            .replace("\u200B", "")
            .replace("\r", "")
            .replace("\n", "")
            .trim()

        if (sanitized.isEmpty()) return null

        // Check if pure article number (must be 5 to 12 digits)
        if (sanitized.matches(Regex("""^\d{5,12}$"""))) {
            return sanitized.toLongOrNull()
        }

        // If input contains URL components or domain slashes
        val isUrlOrPath = sanitized.contains("://") ||
                sanitized.startsWith("www.", ignoreCase = true) ||
                sanitized.contains(".ru", ignoreCase = true) ||
                sanitized.contains(".com", ignoreCase = true) ||
                sanitized.contains("/")

        if (isUrlOrPath) {
            // Must belong to a valid Wildberries domain
            if (!wbDomainRegex.containsMatchIn(sanitized)) {
                return null
            }

            for (regex in regexes) {
                val match = regex.find(sanitized)
                if (match != null) {
                    val idStr = match.groupValues[1]
                    if (idStr.length in 5..12) {
                        return idStr.toLongOrNull()
                    }
                }
            }
        }

        return null
    }
}
