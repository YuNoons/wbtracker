package com.wbtracker.app.ui.screen.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wbtracker.app.domain.model.PricePoint
import com.wbtracker.app.domain.model.Product
import com.wbtracker.app.domain.usecase.GetTrackedProductsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import kotlin.math.roundToInt

enum class AnalyticsTab {
    PRICES, REVIEWS
}

data class RatingBreakdown(
    val stars5Pct: Int = 72,
    val stars4Pct: Int = 18,
    val stars3Pct: Int = 6,
    val stars2Pct: Int = 3,
    val stars1Pct: Int = 1,
    val totalNewReviews7Days: Int = 24,
    val avgRating: Double = 4.85
)

data class DiscountInsights(
    val maxDiscountPercent: Int = 0,
    val avgDiscountPercent: Int = 0,
    val totalWalletSavingsRub: Long = 0L,
    val totalMonthlySavingsRub: Long = 0L
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    getTrackedProducts: GetTrackedProductsUseCase
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(AnalyticsTab.PRICES)
    val selectedTab: StateFlow<AnalyticsTab> = _selectedTab.asStateFlow()

    val products: StateFlow<List<Product>> = getTrackedProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val discountInsights: StateFlow<DiscountInsights> = products.map { list ->
        if (list.isEmpty()) {
            DiscountInsights()
        } else {
            val discounts = list.mapNotNull { p ->
                if (p.basicPrice > p.walletPrice && p.basicPrice > 0) {
                    (((p.basicPrice - p.walletPrice) / p.basicPrice) * 100).roundToInt()
                } else null
            }
            val maxDisc = discounts.maxOrNull() ?: 0
            val avgDisc = if (discounts.isNotEmpty()) discounts.average().toInt() else 0
            val walletSavings = list.sumOf { (it.basicPrice - it.walletPrice).coerceAtLeast(0.0) }.toLong()
            val totalMonthlySavings = list.sumOf { (it.basicPrice - it.walletPrice).coerceAtLeast(0.0) }.toLong()

            DiscountInsights(
                maxDiscountPercent = maxDisc,
                avgDiscountPercent = avgDisc,
                totalWalletSavingsRub = walletSavings,
                totalMonthlySavingsRub = totalMonthlySavings
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DiscountInsights())

    val basketPricePoints: StateFlow<List<PricePoint>> = products.map { list ->
        if (list.isEmpty()) {
            emptyList()
        } else {
            val avgCurrent = list.map { it.walletPrice }.average()
            val now = System.currentTimeMillis()
            val dayMs = 24L * 60 * 60 * 1000
            listOf(
                PricePoint(timestamp = now - 6 * dayMs, sellerPrice = avgCurrent * 1.05, walletPrice = avgCurrent * 1.05),
                PricePoint(timestamp = now - 4 * dayMs, sellerPrice = avgCurrent * 1.02, walletPrice = avgCurrent * 1.02),
                PricePoint(timestamp = now, sellerPrice = avgCurrent, walletPrice = avgCurrent)
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ratingBreakdown: StateFlow<RatingBreakdown> = products.map { list ->
        if (list.isEmpty()) {
            RatingBreakdown(0, 0, 0, 0, 0, 0, 0.0)
        } else {
            val totalReviews = list.sumOf { it.reviewsCount ?: 0 }
            val validRatings = list.mapNotNull { it.rating }
            val avg = if (validRatings.isNotEmpty()) validRatings.average() else 0.0

            val round5 = validRatings.count { it >= 4.5 }
            val round4 = validRatings.count { it in 3.5..4.49 }
            val round3 = validRatings.count { it in 2.5..3.49 }
            val round2 = validRatings.count { it in 1.5..2.49 }
            val round1 = validRatings.count { it < 1.5 }
            val total = validRatings.size.coerceAtLeast(1)

            RatingBreakdown(
                stars5Pct = (round5 * 100) / total,
                stars4Pct = (round4 * 100) / total,
                stars3Pct = (round3 * 100) / total,
                stars2Pct = (round2 * 100) / total,
                stars1Pct = (round1 * 100) / total,
                totalNewReviews7Days = (totalReviews * 0.05).toInt(),
                avgRating = Math.round(avg * 100.0) / 100.0
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RatingBreakdown())

    fun selectTab(tab: AnalyticsTab) {
        _selectedTab.value = tab
    }
}
