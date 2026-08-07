package com.wbtracker.app.data.repository

import com.wbtracker.app.data.local.WbDatabase
import com.wbtracker.app.data.local.dao.PriceHistoryDao
import com.wbtracker.app.data.local.dao.ProductDao
import com.wbtracker.app.data.local.dao.ReviewSnapshotDao
import com.wbtracker.app.data.local.entity.PriceHistoryEntity
import com.wbtracker.app.data.local.entity.ProductEntity
import com.wbtracker.app.data.local.entity.ReviewSnapshotEntity
import com.wbtracker.app.data.remote.WbApiService
import com.wbtracker.app.domain.model.PricePoint
import com.wbtracker.app.domain.model.PriceStats
import com.wbtracker.app.domain.model.Product
import com.wbtracker.app.domain.model.ReviewPoint
import com.wbtracker.app.domain.model.ReviewStats
import com.wbtracker.app.domain.repository.ProductRepository
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject

class ProductRepositoryImpl @Inject constructor(
    private val database: WbDatabase,
    private val productDao: ProductDao,
    private val priceHistoryDao: PriceHistoryDao,
    private val reviewSnapshotDao: ReviewSnapshotDao,
    private val wbApiService: WbApiService
) : ProductRepository {

    override suspend fun addProduct(articleId: Long): Result<Unit> {
        return refreshProduct(articleId)
    }

    override fun getTrackedProducts(): Flow<List<Product>> {
        return productDao.getAllTrackedProducts().map { entities ->
            entities.map { entity ->
                val latestPrice = priceHistoryDao.getLatestPrice(entity.id)
                val latestReview = reviewSnapshotDao.getLatestSnapshot(entity.id)
                val sPrice = latestPrice?.sellerPrice ?: 0.0
                val wPrice = latestPrice?.walletPrice?.takeIf { it > 0 } ?: sPrice
                Product(
                    id = entity.id,
                    title = entity.title,
                    brand = entity.brand,
                    seller = entity.seller,
                    category = entity.category,
                    thumbnailUrl = entity.thumbnailUrl,
                    currentPrice = sPrice,
                    basicPrice = latestPrice?.basicPrice ?: 0.0,
                    walletPrice = wPrice,
                    rating = latestReview?.rating,
                    reviewsCount = latestReview?.reviewsCount,
                    isInStock = latestPrice?.isInStock ?: true,
                    lastUpdatedAt = entity.lastUpdatedAt,
                    isFavorite = entity.isFavorite
                )
            }
        }
    }

    override suspend fun getPriceStats(articleId: Long): PriceStats? {
        val historyFlow = priceHistoryDao.getPriceHistory(articleId)
        val history = historyFlow.first()
        if (history.isEmpty()) return null

        val currentPrice = history.first().sellerPrice
        val minPrice = priceHistoryDao.getMinPrice(articleId) ?: currentPrice
        val maxPrice = priceHistoryDao.getMaxPrice(articleId) ?: currentPrice
        val avgPrice = priceHistoryDao.getAvgPriceSince(articleId, 0L) ?: currentPrice

        val points = history.map {
            PricePoint(
                timestamp = it.timestamp,
                sellerPrice = it.sellerPrice,
                walletPrice = it.walletPrice,
                isInStock = it.isInStock
            )
        }

        return PriceStats(
            currentPrice = currentPrice,
            minPrice = minPrice,
            maxPrice = maxPrice,
            avgPrice = avgPrice,
            priceHistory = points
        )
    }

    override suspend fun getReviewStats(articleId: Long): ReviewStats? {
        val historyFlow = reviewSnapshotDao.getReviewHistory(articleId)
        val history = historyFlow.first()
        if (history.isEmpty()) return null

        val currentRating = history.first().rating
        val currentReviewsCount = history.first().reviewsCount
        val minRating = reviewSnapshotDao.getMinRating(articleId) ?: currentRating
        val maxRating = reviewSnapshotDao.getMaxRating(articleId) ?: currentRating
        val avgRating = reviewSnapshotDao.getAvgRatingSince(articleId, 0L) ?: currentRating

        val points = history.map {
            ReviewPoint(
                timestamp = it.timestamp,
                rating = it.rating,
                reviewsCount = it.reviewsCount
            )
        }

        return ReviewStats(
            currentRating = currentRating,
            currentReviewsCount = currentReviewsCount,
            minRating = minRating,
            maxRating = maxRating,
            avgRating = avgRating,
            reviewHistory = points
        )
    }

    override suspend fun getAllTrackedIds(): List<Long> {
        return productDao.getAllTrackedIds()
    }

    private fun formatRussianError(e: Throwable): Exception {
        val msg = e.message ?: ""
        return when {
            e is UnknownHostException || msg.contains("Unable to resolve host", ignoreCase = true) || msg.contains("No address associated", ignoreCase = true) ->
                Exception("Не удалось подключиться к серверу WB (ошибка DNS/сети)")
            e is SocketTimeoutException || msg.contains("timeout", ignoreCase = true) ->
                Exception("Превышено время ожидания ответа от сервера WB")
            e is javax.net.ssl.SSLHandshakeException || e is javax.net.ssl.SSLException || msg.contains("SSL", ignoreCase = true) || msg.contains("cert", ignoreCase = true) ->
                Exception("Ошибка TLS/SSL соединения WB: ${e.localizedMessage}")
            msg.contains("не найден", ignoreCase = true) ->
                Exception(msg)
            else ->
                Exception("Ошибка загрузки: ${e.localizedMessage}")
        }
    }

    override suspend fun refreshProduct(articleId: Long): Result<Unit> {
        try {
            val cardInfoResult = wbApiService.fetchCardInfo(articleId)

            if (cardInfoResult.isFailure) {
                val origErr = cardInfoResult.exceptionOrNull() ?: Exception("Товар $articleId не найден в каталоге WB")
                return Result.failure(formatRussianError(origErr))
            }

            val cardResultData = cardInfoResult.getOrNull()!!
            val cardInfo = cardResultData.json
            val actualBasketNum = cardResultData.basketNum

            val detailInfo = wbApiService.fetchProductDetail(articleId).getOrNull()
            val sellerInfo = wbApiService.fetchSellerInfo(articleId, actualBasketNum).getOrNull()

            val title = cardInfo.optString("imt_name", "Товар $articleId")
            val sellingObj = cardInfo.optJSONObject("selling")
            val brand = sellingObj?.optString("brand_name") ?: "Не указан"
            var seller = brand
            var sellerId = sellingObj?.optLong("supplier_id") ?: 0L

            if (sellerInfo != null) {
                seller = sellerInfo.optString("supplierName").takeIf { !it.isNullOrEmpty() }
                    ?: sellerInfo.optString("supplierFullName").takeIf { !it.isNullOrEmpty() }
                    ?: seller
                val sId = sellerInfo.optLong("supplierId", 0L)
                if (sId != 0L) sellerId = sId
            }

            val category = cardInfo.optString("subj_name", "")
            val rootCategory = cardInfo.optString("subj_root_name", "")
            val vendorCode = cardInfo.optString("vendor_code", "")
            val description = cardInfo.optString("description", "")

            val mediaObj = cardInfo.optJSONObject("media")
            val photoCount = mediaObj?.optInt("photo_count", 0) ?: 0
            val vol = articleId / 100000
            val part = articleId / 1000
            val thumbnailUrl = "https://basket-$actualBasketNum.wbbasket.ru/vol$vol/part$part/$articleId/images/big/1.webp"

            var rating: Double? = null
            var reviewsCount: Int? = null
            var basicPrice = 0.0
            var sellerPrice = 0.0
            var walletPrice = 0.0

            if (detailInfo != null) {
                val productsArray = detailInfo.optJSONArray("products")
                    ?: detailInfo.optJSONObject("data")?.optJSONArray("products")
                if (productsArray != null && productsArray.length() > 0) {
                    val prod = productsArray.getJSONObject(0)
                    val r = prod.optDouble("reviewRating", prod.optDouble("rating", 0.0))
                    if (r > 0) rating = r
                    val fc = prod.optInt("feedbacks", 0)
                    if (fc > 0) reviewsCount = fc

                    val sizesArray = prod.optJSONArray("sizes")
                    if (sizesArray != null) {
                        for (i in 0 until sizesArray.length()) {
                            val sizeObj = sizesArray.optJSONObject(i) ?: continue
                            val priceObj = sizeObj.optJSONObject("price") ?: continue
                            val productKopecks = priceObj.optDouble("product", priceObj.optDouble("priceU", 0.0))
                            val totalKopecks = priceObj.optDouble("total", priceObj.optDouble("salePriceU", productKopecks))
                            val basicKopecks = priceObj.optDouble("basic", 0.0)
                            val walletKopecks = priceObj.optDouble("wallet", priceObj.optDouble("cpay", if (totalKopecks > 0) totalKopecks else productKopecks))

                            val rawSeller = if (totalKopecks > 0) totalKopecks else productKopecks
                            if (rawSeller > 0) {
                                basicPrice = (if (basicKopecks > 0) basicKopecks else rawSeller) / 100.0
                                sellerPrice = rawSeller / 100.0
                                walletPrice = (if (walletKopecks > 0) walletKopecks else rawSeller) / 100.0
                                break
                            }
                        }
                    }

                    if (sellerPrice == 0.0) {
                        val salePriceU = prod.optDouble("salePriceU", 0.0)
                        val priceU = prod.optDouble("priceU", 0.0)
                        val rawSeller = if (salePriceU > 0) salePriceU else priceU
                        if (rawSeller > 0) {
                            sellerPrice = rawSeller / 100.0
                            basicPrice = (if (priceU > 0) priceU else rawSeller) / 100.0
                            walletPrice = sellerPrice
                        }
                    }
                }
            }

            val isInStock = (sellerPrice > 0)

            val product = ProductEntity(
                id = articleId,
                title = title,
                brand = brand,
                seller = seller,
                sellerId = sellerId,
                category = category,
                rootCategory = rootCategory,
                vendorCode = vendorCode,
                description = description,
                thumbnailUrl = thumbnailUrl,
                imagesCount = photoCount,
                lastUpdatedAt = System.currentTimeMillis(),
                isTracking = true
            )

            database.withTransaction {
                val existing = productDao.getProductById(articleId)
                if (existing == null) {
                    productDao.insertProduct(product)
                } else {
                    productDao.updateProduct(product.copy(addedAt = existing.addedAt))
                }

                priceHistoryDao.insertPrice(
                    PriceHistoryEntity(
                        productId = articleId,
                        basicPrice = basicPrice,
                        sellerPrice = sellerPrice,
                        walletPrice = walletPrice,
                        isInStock = isInStock
                    )
                )

                if (rating != null || reviewsCount != null) {
                    reviewSnapshotDao.insertSnapshot(
                        ReviewSnapshotEntity(
                            productId = articleId,
                            rating = rating ?: 0.0,
                            reviewsCount = reviewsCount ?: 0
                        )
                    )
                }
            }

            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(formatRussianError(e))
        }
    }

    override suspend fun stopTracking(articleId: Long) {
        productDao.stopTracking(articleId)
    }

    override suspend fun toggleFavorite(articleId: Long) {
        productDao.toggleFavorite(articleId)
    }
}

