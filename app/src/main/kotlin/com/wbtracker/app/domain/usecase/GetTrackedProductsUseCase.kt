package com.wbtracker.app.domain.usecase

import com.wbtracker.app.domain.model.Product
import com.wbtracker.app.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTrackedProductsUseCase @Inject constructor(
    private val repository: ProductRepository
) {
    operator fun invoke(): Flow<List<Product>> = repository.getTrackedProducts()
}
