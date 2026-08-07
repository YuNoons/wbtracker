package com.wbtracker.app.ui.screen.addproduct

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wbtracker.app.domain.model.Product
import com.wbtracker.app.domain.usecase.AddProductUseCase
import com.wbtracker.app.util.WbArticleExtractor
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
        return WbArticleExtractor.extractArticleId(input)
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

    fun resetState() {
        _state.value = AddProductState.Idle
    }
}

