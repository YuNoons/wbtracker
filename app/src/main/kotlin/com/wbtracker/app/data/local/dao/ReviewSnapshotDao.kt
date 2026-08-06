package com.wbtracker.app.data.local.dao

import androidx.room.*
import com.wbtracker.app.data.local.entity.ReviewSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewSnapshotDao {
    @Insert
    suspend fun insertSnapshot(snapshot: ReviewSnapshotEntity)

    @Query("SELECT * FROM review_snapshots WHERE productId = :productId ORDER BY timestamp DESC")
    fun getReviewHistory(productId: Long): Flow<List<ReviewSnapshotEntity>>

    @Query("SELECT * FROM review_snapshots WHERE productId = :productId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestSnapshot(productId: Long): ReviewSnapshotEntity?

    @Query("DELETE FROM review_snapshots WHERE productId = :productId AND timestamp < :before")
    suspend fun deleteOldSnapshots(productId: Long, before: Long)
}
