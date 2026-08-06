package com.wbtracker.app.di

import android.content.Context
import androidx.room.Room
import com.wbtracker.app.data.local.WbDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): WbDatabase =
        Room.databaseBuilder(context, WbDatabase::class.java, "wb_tracker.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideProductDao(db: WbDatabase) = db.productDao()
    @Provides fun providePriceHistoryDao(db: WbDatabase) = db.priceHistoryDao()
    @Provides fun provideReviewSnapshotDao(db: WbDatabase) = db.reviewSnapshotDao()
    @Provides fun provideNotificationRuleDao(db: WbDatabase) = db.notificationRuleDao()
}
