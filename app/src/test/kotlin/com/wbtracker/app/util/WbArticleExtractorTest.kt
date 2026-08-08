package com.wbtracker.app.util

import org.junit.Assert.*
import org.junit.Test

class WbArticleExtractorTest {

    @Test
    fun testValidPureDigits() {
        assertEquals(12345678L, WbArticleExtractor.extractArticleId("12345678"))
        assertEquals(12345L, WbArticleExtractor.extractArticleId("12345"))
        assertEquals(123456789012L, WbArticleExtractor.extractArticleId("123456789012"))
    }

    @Test
    fun testInvalidDigitLengths() {
        assertNull(WbArticleExtractor.extractArticleId("1234"))
        assertNull(WbArticleExtractor.extractArticleId("1234567890123"))
    }

    @Test
    fun testValidWildberriesUrls() {
        assertEquals(12345678L, WbArticleExtractor.extractArticleId("https://www.wildberries.ru/catalog/12345678/detail.aspx"))
        assertEquals(987654321L, WbArticleExtractor.extractArticleId("https://wb.ru/product/987654321"))
        assertEquals(55555555L, WbArticleExtractor.extractArticleId("https://wildberries.by/catalog/detail.aspx?nm=55555555"))
        assertEquals(77777777L, WbArticleExtractor.extractArticleId("https://wb.ru/card/77777777"))
    }

    @Test
    fun testInvalidNonWbUrls() {
        assertNull(WbArticleExtractor.extractArticleId("https://example.com/item/12345678"))
        assertNull(WbArticleExtractor.extractArticleId("https://ozon.ru/product/12345678"))
        assertNull(WbArticleExtractor.extractArticleId("https://google.com/search?q=12345678"))
    }

    @Test
    fun testSanitizationAndWhitespace() {
        assertEquals(12345678L, WbArticleExtractor.extractArticleId("\uFEFF  12345678 \n"))
    }
}
