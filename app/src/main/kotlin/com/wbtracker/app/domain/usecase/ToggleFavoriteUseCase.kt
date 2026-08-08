package com.wbtracker.app.domain.usecase

import com.wbtracker.app.domain.repository.ProductRepository
import javax.inject.Inject

class ToggleFavoriteUseCase @Inject constructor(
    private val repository: ProductRepository
) {
    suspend operator fun invoke(articleId: Long) {
        repository.toggleFavorite(articleId)
    }
}
