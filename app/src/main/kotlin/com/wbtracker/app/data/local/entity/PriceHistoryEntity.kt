package com.wbtracker.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "price_history",
    foreignKeys = [ForeignKey(
        entity = ProductEntity::class,
        parentColumns = ["id"],
        childColumns = ["productId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("productId")]
)
data class PriceHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val basicPrice: Double,       // цена без скидок
    val sellerPrice: Double,      // цена продавца
    val walletPrice: Double,      // с WB Кошельком (~4.5% от sellerPrice)
    val isInStock: Boolean = true,
    val primaryPrice: Double = 0.0 // цена, которую отслеживаем
)
