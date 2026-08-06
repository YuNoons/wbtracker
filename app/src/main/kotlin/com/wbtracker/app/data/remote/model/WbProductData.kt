package com.wbtracker.app.data.remote.model

data class WbProductData(
    val articleId: Long,
    val title: String,
    val brand: String,
    val seller: String,
    val sellerId: Long,
    val category: String,
    val rootCategory: String,
    val vendorCode: String,
    val description: String,
    val basicPrice: Double,
    val sellerPrice: Double,
    val walletPrice: Double,
    val rating: Double?,
    val reviewsCount: Int?,
    val imagesCount: Int,
    val basketNum: String
)
