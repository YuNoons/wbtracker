package com.wbtracker.app.domain.model

data class Product(
    val id: Long,
    val title: String,
    val brand: String,
    val seller: String,
    val category: String,
    val thumbnailUrl: String,
    val currentPrice: Double,
    val basicPrice: Double,
    val walletPrice: Double,
    val initialWalletPrice: Double = 0.0,
    val rating: Double?,
    val reviewsCount: Int?,
    val isInStock: Boolean,
    val lastUpdatedAt: Long,
    val isFavorite: Boolean = false,
    val targetPrice: Double? = null,
    val targetEnabled: Boolean = false
)
