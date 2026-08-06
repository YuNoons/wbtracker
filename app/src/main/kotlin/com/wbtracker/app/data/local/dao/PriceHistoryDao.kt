package com.wbtracker.app.data.local.dao

import androidx.room.*
import com.wbtracker.app.data.local.entity.PriceHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PriceHistoryDao {
    @Insert
    suspend fun insertPrice(price: PriceHistoryEntity)

    @Query("SELECT * FROM price_history WHERE productId = :productId ORDER BY timestamp DESC")
    fun getPriceHistory(productId: Long): Flow<List<PriceHistoryEntity>>

    @Query("SELECT * FROM price_history WHERE productId = :productId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestPrice(productId: Long): PriceHistoryEntity?

    @Query("SELECT MIN(sellerPrice) FROM price_history WHERE productId = :productId")
    suspend fun getMinPrice(productId: Long): Double?

    @Query("SELECT MAX(sellerPrice) FROM price_history WHERE productId = :productId")
    suspend fun getMaxPrice(productId: Long): Double?

    @Query("SELECT AVG(sellerPrice) FROM price_history WHERE productId = :productId AND timestamp > :since")
    suspend fun getAvgPriceSince(productId: Long, since: Long): Double?

    @Query("DELETE FROM price_history WHERE productId = :productId AND timestamp < :before")
    suspend fun deleteOldHistory(productId: Long, before: Long)
}
