package com.wbtracker.app.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.wbtracker.app.data.local.dao.NotificationRuleDao
import com.wbtracker.app.data.local.dao.PriceHistoryDao
import com.wbtracker.app.data.local.dao.ProductDao
import com.wbtracker.app.data.notification.WbNotificationHelper
import com.wbtracker.app.domain.repository.ProductRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class PriceUpdateWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: ProductRepository,
    private val notificationRuleDao: NotificationRuleDao,
    private val productDao: ProductDao,
    private val priceHistoryDao: PriceHistoryDao,
    private val notificationHelper: WbNotificationHelper
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val trackedIds = repository.getAllTrackedIds()
            
            val drops = mutableListOf<Pair<String, Double>>()
            
            for (id in trackedIds) {
                repository.refreshProduct(id)
                
                val rules = notificationRuleDao.getActiveRulesForProduct(id)
                if (rules.isEmpty()) continue
                
                val product = productDao.getProductById(id) ?: continue
                val latestPrice = priceHistoryDao.getLatestPrice(id) ?: continue
                
                for (rule in rules) {
                    val targetPrice = rule.targetPrice
                    if (targetPrice != null && latestPrice.walletPrice <= targetPrice) {
                        drops.add(Pair(product.title, latestPrice.walletPrice))
                        notificationRuleDao.setRuleActive(rule.id, false)
                    }
                }
            }
            
            if (drops.isNotEmpty()) {
                notificationHelper.showGroupedPriceDropNotification(context, drops)
            }
            
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
