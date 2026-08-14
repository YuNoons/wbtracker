package com.wbtracker.app.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.wbtracker.app.data.local.dao.AlertHistoryDao
import com.wbtracker.app.data.local.dao.NotificationRuleDao
import com.wbtracker.app.data.local.dao.PriceHistoryDao
import com.wbtracker.app.data.local.dao.ProductDao
import com.wbtracker.app.data.local.entity.AlertHistoryEntity
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
    private val alertHistoryDao: AlertHistoryDao,
    private val notificationHelper: WbNotificationHelper
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val trackedIds = repository.getAllTrackedIds()
            
            val drops = mutableListOf<Pair<String, Double>>()
            
            for (id in trackedIds) {
                val previousPrice = priceHistoryDao.getLatestPrice(id)?.primaryPrice

                repository.refreshProduct(id)
                
                val rules = notificationRuleDao.getActiveRulesForProduct(id)
                if (rules.isEmpty()) continue
                
                val product = productDao.getProductById(id) ?: continue
                val latestPrice = priceHistoryDao.getLatestPrice(id) ?: continue
                val newPrice = latestPrice.primaryPrice

                for (rule in rules) {
                    val targetPrice = rule.targetPrice
                    val isPriceDrop = previousPrice == null || newPrice < previousPrice
                    if (targetPrice != null && newPrice <= targetPrice && isPriceDrop) {
                        drops.add(Pair(product.title, newPrice))
                        notificationRuleDao.setRuleActive(rule.id, false)
                        alertHistoryDao.insert(
                            AlertHistoryEntity(
                                productId = product.id,
                                productTitle = product.title,
                                thumbnailUrl = product.thumbnailUrl,
                                triggeredPrice = newPrice,
                                targetPrice = targetPrice,
                                alertType = "price_drop",
                                timestamp = System.currentTimeMillis(),
                                isRead = false,
                                oldPrice = previousPrice
                            )
                        )
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
