package com.wbtracker.app.domain.repository

import com.wbtracker.app.domain.model.Product
import com.wbtracker.app.domain.model.PriceStats
import com.wbtracker.app.domain.model.ReviewStats
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    suspend fun addProduct(articleId: Long): Result<Unit>
    fun getTrackedProducts(): Flow<List<Product>>
    suspend fun refreshProduct(articleId: Long): Result<Unit>
    suspend fun stopTracking(articleId: Long)
    suspend fun getPriceStats(articleId: Long): PriceStats?
    suspend fun getReviewStats(articleId: Long): ReviewStats?
    suspend fun getAllTrackedIds(): List<Long>
}
