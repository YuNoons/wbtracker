package com.wbtracker.app.ui.screen.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wbtracker.app.domain.model.Product
import com.wbtracker.app.domain.usecase.GetTrackedProductsUseCase
import com.wbtracker.app.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class CollectionCategory(val id: String, val title: String, val iconEmoji: String) {
    ALL("all", "Все", "⭐"),
    WANT_BUY("want_buy", "Хочу купить", "🛍️"),
    GIFTS("gifts", "Подарки", "🎁"),
    HOME("home", "Для дома", "🏠"),
    SPORT("sport", "Спорт", "⚽")
}

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    getTrackedProducts: GetTrackedProductsUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase
) : ViewModel() {

    private val _selectedCollection = MutableStateFlow(CollectionCategory.ALL)
    val selectedCollection: StateFlow<CollectionCategory> = _selectedCollection.asStateFlow()

    val favoriteProducts: StateFlow<List<Product>> = getTrackedProducts()
        .combine(_selectedCollection) { products, category ->
            val favorites = products.filter { it.isFavorite }
            when (category) {
                CollectionCategory.ALL -> favorites
                CollectionCategory.WANT_BUY -> favorites.filter { it.category.contains("Одежда", ignoreCase = true) || it.category.contains("Обувь", ignoreCase = true) }.ifEmpty { favorites }
                CollectionCategory.GIFTS -> favorites.filter { it.category.contains("Красота", ignoreCase = true) || it.category.contains("Подарки", ignoreCase = true) }.ifEmpty { favorites }
                CollectionCategory.HOME -> favorites.filter { it.category.contains("Дом", ignoreCase = true) || it.category.contains("Кухня", ignoreCase = true) }.ifEmpty { favorites }
                CollectionCategory.SPORT -> favorites.filter { it.category.contains("Спорт", ignoreCase = true) }.ifEmpty { favorites }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectCollection(category: CollectionCategory) {
        _selectedCollection.value = category
    }

    fun toggleFavorite(articleId: Long) {
        viewModelScope.launch {
            toggleFavoriteUseCase(articleId)
        }
    }
}
