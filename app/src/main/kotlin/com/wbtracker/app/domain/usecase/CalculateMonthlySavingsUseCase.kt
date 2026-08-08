package com.wbtracker.app.domain.usecase

import com.wbtracker.app.domain.model.Product
import com.wbtracker.app.domain.repository.ProductRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class CalculateMonthlySavingsUseCase @Inject constructor(
    private val repository: ProductRepository
) {
    suspend operator fun invoke(products: List<Product>? = null): Long {
        val list = products ?: repository.getTrackedProducts().first()
        var totalSavings = 0L
        for (product in list) {
            val priceStats = repository.getPriceStats(product.id)
            if (priceStats != null && priceStats.priceHistory.isNotEmpty()) {
                val initialOrMaxPrice = priceStats.priceHistory.maxOf { if (it.walletPrice > 0) it.walletPrice else it.sellerPrice }
                val currentWalletPrice = product.walletPrice
                val diff = (initialOrMaxPrice - currentWalletPrice).toLong()
                if (diff > 0) {
                    totalSavings += diff
                }
            } else if (product.basicPrice > product.walletPrice && product.walletPrice > 0) {
                val diff = (product.basicPrice - product.walletPrice).toLong()
                if (diff > 0) {
                    totalSavings += diff
                }
            }
        }
        return totalSavings
    }
}

