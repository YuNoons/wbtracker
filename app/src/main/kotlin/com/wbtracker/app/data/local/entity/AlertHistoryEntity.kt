package com.wbtracker.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alert_history")
data class AlertHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: Long,
    val productTitle: String,
    val thumbnailUrl: String,
    val triggeredPrice: Double,
    val targetPrice: Double,
    val alertType: String,
    val timestamp: Long,
    val isRead: Boolean = false,
    val oldPrice: Double? = null
)
