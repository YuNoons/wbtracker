package com.wbtracker.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.wbtracker.app.data.local.entity.AlertHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertHistoryDao {
    @Query("SELECT * FROM alert_history ORDER BY timestamp DESC")
    suspend fun getAllAlerts(): List<AlertHistoryEntity>

    @Query("SELECT * FROM alert_history ORDER BY timestamp DESC")
    fun getAllAlertsFlow(): Flow<List<AlertHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alert: AlertHistoryEntity): Long

    @Query("UPDATE alert_history SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)

    @Query("DELETE FROM alert_history")
    suspend fun clearAll()
}
