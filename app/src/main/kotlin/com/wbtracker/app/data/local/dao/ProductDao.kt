package com.wbtracker.app.data.local.dao

import androidx.room.*
import com.wbtracker.app.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE isTracking = 1 ORDER BY addedAt DESC")
    fun getAllTrackedProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Long): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProduct(product: ProductEntity)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Query("UPDATE products SET isTracking = 0 WHERE id = :id")
    suspend fun stopTracking(id: Long)

    @Query("UPDATE products SET lastUpdatedAt = :timestamp WHERE id = :id")
    suspend fun updateLastUpdated(id: Long, timestamp: Long)

    @Query("SELECT id FROM products WHERE isTracking = 1")
    suspend fun getAllTrackedIds(): List<Long>
}
