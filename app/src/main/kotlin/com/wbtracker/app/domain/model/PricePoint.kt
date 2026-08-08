package com.wbtracker.app.domain.model

data class PricePoint(
    val timestamp: Long,
    val sellerPrice: Double,
    val walletPrice: Double,
    val isInStock: Boolean = true
)
