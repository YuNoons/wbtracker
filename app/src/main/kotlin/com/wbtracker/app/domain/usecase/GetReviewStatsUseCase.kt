package com.wbtracker.app.domain.usecase

import com.wbtracker.app.domain.model.ReviewStats
import com.wbtracker.app.domain.repository.ProductRepository
import javax.inject.Inject

class GetReviewStatsUseCase @Inject constructor(
    private val repository: ProductRepository
) {
    suspend operator fun invoke(articleId: Long): ReviewStats? = repository.getReviewStats(articleId)
}
