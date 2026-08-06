package com.wbtracker.app.domain.usecase

import com.wbtracker.app.domain.model.PriceStats
import com.wbtracker.app.domain.repository.ProductRepository
import javax.inject.Inject

class GetPriceStatsUseCase @Inject constructor(
    private val repository: ProductRepository
) {
    suspend operator fun invoke(articleId: Long): PriceStats? = repository.getPriceStats(articleId)
}
