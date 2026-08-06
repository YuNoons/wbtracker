package com.wbtracker.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: Long,          // артикул WB (nm_id)
    val title: String,
    val brand: String,
    val seller: String,
    val sellerId: Long,
    val category: String,
    val rootCategory: String,
    val vendorCode: String,
    val description: String,
    val thumbnailUrl: String,          // первое фото
    val imagesCount: Int,
    val addedAt: Long = System.currentTimeMillis(),
    val lastUpdatedAt: Long = System.currentTimeMillis(),
    val isTracking: Boolean = true
)
