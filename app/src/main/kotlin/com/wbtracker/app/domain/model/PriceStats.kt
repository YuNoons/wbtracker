package com.wbtracker.app.domain.model

data class PriceStats(
    val currentPrice: Double,
    val minPrice: Double,
    val maxPrice: Double,
    val avgPrice: Double,
    val priceHistory: List<PricePoint>
)
