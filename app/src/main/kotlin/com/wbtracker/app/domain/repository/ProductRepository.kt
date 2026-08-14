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
    suspend fun restoreProduct(articleId: Long)
    suspend fun toggleFavorite(articleId: Long)
    suspend fun setFavorite(articleId: Long, isFavorite: Boolean)
    suspend fun setTargetPrice(articleId: Long, price: Double, enabled: Boolean)
    suspend fun getPriceStats(articleId: Long): PriceStats?
    suspend fun getReviewStats(articleId: Long): ReviewStats?
    suspend fun getAllTrackedIds(): List<Long>
    suspend fun importBackupJson(jsonString: String): Result<Int>
}
