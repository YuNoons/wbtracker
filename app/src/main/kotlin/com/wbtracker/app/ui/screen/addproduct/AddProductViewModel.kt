package com.wbtracker.app.ui.screen.addproduct

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wbtracker.app.domain.model.Product
import com.wbtracker.app.domain.usecase.AddProductUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AddProductState {
    object Idle : AddProductState()
    object Loading : AddProductState()
    data class Preview(val product: Product) : AddProductState()
    data class Error(val message: String) : AddProductState()
    object Success : AddProductState()
}

@HiltViewModel
class AddProductViewModel @Inject constructor(
    private val addProduct: AddProductUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow<AddProductState>(AddProductState.Idle)
    val state: StateFlow<AddProductState> = _state.asStateFlow()

    fun extractArticleFromInput(input: String): Long? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null
        
        val isDigitsOnly = trimmed.all { it.isDigit() }
        val isValidUrl = trimmed.contains("wildberries.ru", ignoreCase = true) || 
                         trimmed.contains("wb.ru", ignoreCase = true)
                         
        if (!isDigitsOnly && !isValidUrl) return null
        
        // 1. Пытаемся найти цифры после /catalog/ (например, wildberries.ru/catalog/211605634/detail.aspx)
        val catalogRegex = Regex("""catalog/(\d+)""")
        val catalogMatch = catalogRegex.find(trimmed)
        if (catalogMatch != null) {
            return catalogMatch.groupValues[1].toLongOrNull()
        }

        // 2. Пытаемся найти любую последовательность из 6-12 цифр в тексте
        val numberRegex = Regex("""\b(\d{6,12})\b""")
        val numberMatch = numberRegex.find(trimmed)
        if (numberMatch != null) {
            return numberMatch.groupValues[1].toLongOrNull()
        }

        return trimmed.toLongOrNull()
    }

    fun addProductByInput(input: String) {
        val articleId = extractArticleFromInput(input) ?: run {
            _state.value = AddProductState.Error("Не удалось распознать артикул. Введите цифры или ссылку WB.")
            return
        }
        viewModelScope.launch {
            _state.value = AddProductState.Loading
            addProduct(articleId)
                .onSuccess { _state.value = AddProductState.Success }
                .onFailure { _state.value = AddProductState.Error(it.message ?: "Ошибка получения данных") }
        }
    }
}
