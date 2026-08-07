package com.wbtracker.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.wbtracker.app.data.local.dao.*
import com.wbtracker.app.data.local.entity.*

@Database(
    entities = [
        ProductEntity::class,
        PriceHistoryEntity::class,
        ReviewSnapshotEntity::class,
        NotificationRuleEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class WbDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun priceHistoryDao(): PriceHistoryDao
    abstract fun reviewSnapshotDao(): ReviewSnapshotDao
    abstract fun notificationRuleDao(): NotificationRuleDao
}
