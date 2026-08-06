package com.wbtracker.app.ui.screen.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wbtracker.app.domain.model.Product
import com.wbtracker.app.domain.usecase.GetTrackedProductsUseCase
import com.wbtracker.app.domain.usecase.StopTrackingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getTrackedProducts: GetTrackedProductsUseCase,
    private val stopTracking: StopTrackingUseCase,
) : ViewModel() {
    val products: StateFlow<List<Product>> = getTrackedProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun stopTracking(articleId: Long) {
        viewModelScope.launch { stopTracking.invoke(articleId) }
    }
}
