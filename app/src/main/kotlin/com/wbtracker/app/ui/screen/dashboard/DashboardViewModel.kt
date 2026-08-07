package com.wbtracker.app.ui.screen.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wbtracker.app.domain.model.Product
import com.wbtracker.app.domain.usecase.AddProductUseCase
import com.wbtracker.app.domain.usecase.CalculateMonthlySavingsUseCase
import com.wbtracker.app.domain.usecase.GetTrackedProductsUseCase
import com.wbtracker.app.domain.usecase.StopTrackingUseCase
import com.wbtracker.app.domain.usecase.ToggleFavoriteUseCase
import com.wbtracker.app.util.WbArticleExtractor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getTrackedProducts: GetTrackedProductsUseCase,
    private val stopTrackingUseCase: StopTrackingUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val calculateMonthlySavings: CalculateMonthlySavingsUseCase,
    private val addProductUseCase: AddProductUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isAdding = MutableStateFlow(false)
    val isAdding: StateFlow<Boolean> = _isAdding.asStateFlow()

    private val _addError = MutableStateFlow<String?>(null)
    val addError: StateFlow<String?> = _addError.asStateFlow()

    private val _addSuccess = MutableStateFlow<String?>(null)
    val addSuccess: StateFlow<String?> = _addSuccess.asStateFlow()

    val detectedArticleId: StateFlow<Long?> = _searchQuery.combine(MutableStateFlow(Unit)) { query, _ ->
        WbArticleExtractor.extractArticleId(query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val allProducts = getTrackedProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val products: StateFlow<List<Product>> = combine(allProducts, _searchQuery) { list, query ->
        if (query.isBlank()) {
            list
        } else {
            list.filter { product ->
                product.title.contains(query, ignoreCase = true) ||
                product.brand.contains(query, ignoreCase = true) ||
                product.category.contains(query, ignoreCase = true) ||
                product.id.toString().contains(query)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hotDiscounts: StateFlow<List<Product>> = allProducts
        .combine(_searchQuery) { list, _ ->
            list.filter { p -> p.basicPrice > p.walletPrice && p.basicPrice > 0 }
                .sortedByDescending { p ->
                    (((p.basicPrice - p.walletPrice) / p.basicPrice) * 100).roundToInt()
                }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalSavings: StateFlow<Long> = allProducts
        .combine(_searchQuery) { list, _ ->
            calculateMonthlySavings(list)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        _addError.value = null
    }

    fun addProductFromSearch() {
        val query = _searchQuery.value
        val articleId = WbArticleExtractor.extractArticleId(query) ?: run {
            _addError.value = "Не удалось распознать артикул WB"
            return
        }

        viewModelScope.launch {
            _isAdding.value = true
            _addError.value = null
            _addSuccess.value = null

            addProductUseCase(articleId)
                .onSuccess {
                    _isAdding.value = false
                    _searchQuery.value = ""
                    _addSuccess.value = "Товар $articleId успешно добавлен!"
                }
                .onFailure {
                    _isAdding.value = false
                    _addError.value = it.message ?: "Ошибка загрузки товара"
                }
        }
    }

    fun clearAddMessages() {
        _addError.value = null
        _addSuccess.value = null
    }

    fun stopTracking(articleId: Long) {
        viewModelScope.launch { stopTrackingUseCase(articleId) }
    }

    fun toggleFavorite(articleId: Long) {
        viewModelScope.launch { toggleFavoriteUseCase(articleId) }
    }
}

