package com.wbtracker.app.data.worker

import androidx.work.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

import com.wbtracker.app.domain.repository.SyncScheduler

@Singleton
class WorkManagerScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) : SyncScheduler {
    companion object {
        const val WORK_NAME = "wb_price_update"
    }

    override fun schedulePeriodicUpdate(intervalHours: Long) {
        val request = PeriodicWorkRequestBuilder<PriceUpdateWorker>(intervalHours, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    override fun cancelUpdates() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
