package com.wbtracker.app.ui.screen.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wbtracker.app.domain.model.PriceStats
import com.wbtracker.app.domain.model.Product
import com.wbtracker.app.domain.model.ReviewStats
import com.wbtracker.app.domain.usecase.GetPriceStatsUseCase
import com.wbtracker.app.domain.usecase.GetReviewStatsUseCase
import com.wbtracker.app.domain.usecase.GetTrackedProductsUseCase
import com.wbtracker.app.domain.usecase.StopTrackingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class PriceStatsUiState {
    object Loading : PriceStatsUiState()
    data class Success(val stats: PriceStats) : PriceStatsUiState()
    data class Error(val message: String) : PriceStatsUiState()
}

sealed class ReviewStatsUiState {
    object Loading : ReviewStatsUiState()
    data class Success(val stats: ReviewStats) : ReviewStatsUiState()
    data class Error(val message: String) : ReviewStatsUiState()
}

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getPriceStats: GetPriceStatsUseCase,
    private val getReviewStats: GetReviewStatsUseCase,
    private val getTrackedProducts: GetTrackedProductsUseCase,
    private val stopTracking: StopTrackingUseCase,
) : ViewModel() {
    val articleId: Long = checkNotNull(savedStateHandle["articleId"])

    val product: StateFlow<Product?> = getTrackedProducts()
        .map { list -> list.find { it.id == articleId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _priceStats = MutableStateFlow<PriceStatsUiState>(PriceStatsUiState.Loading)
    val priceStats: StateFlow<PriceStatsUiState> = _priceStats.asStateFlow()

    private val _reviewStats = MutableStateFlow<ReviewStatsUiState>(ReviewStatsUiState.Loading)
    val reviewStats: StateFlow<ReviewStatsUiState> = _reviewStats.asStateFlow()

    init {
        loadStats()
    }
    
    fun loadStats() {
        loadPriceStats()
        loadReviewStats()
    }

    fun loadPriceStats() {
        viewModelScope.launch {
            _priceStats.value = PriceStatsUiState.Loading
            try {
                val stats = getPriceStats(articleId)
                if (stats != null) {
                    _priceStats.value = PriceStatsUiState.Success(stats)
                } else {
                    _priceStats.value = PriceStatsUiState.Error("Данные не найдены")
                }
            } catch (e: Exception) {
                _priceStats.value = PriceStatsUiState.Error(e.localizedMessage ?: "Ошибка загрузки")
            }
        }
    }

    fun loadReviewStats() {
        viewModelScope.launch {
            _reviewStats.value = ReviewStatsUiState.Loading
            try {
                val stats = getReviewStats(articleId)
                if (stats != null) {
                    _reviewStats.value = ReviewStatsUiState.Success(stats)
                } else {
                    _reviewStats.value = ReviewStatsUiState.Error("Данные не найдены")
                }
            } catch (e: Exception) {
                _reviewStats.value = ReviewStatsUiState.Error(e.localizedMessage ?: "Ошибка загрузки")
            }
        }
    }
    
    fun removeProduct(onRemoved: () -> Unit) {
        viewModelScope.launch {
            stopTracking(articleId)
            onRemoved()
        }
    }
}
