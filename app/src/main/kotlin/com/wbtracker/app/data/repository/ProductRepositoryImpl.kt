package com.wbtracker.app.data.repository

import com.wbtracker.app.data.local.WbDatabase
import com.wbtracker.app.data.local.dao.PriceHistoryDao
import com.wbtracker.app.data.local.dao.ProductDao
import com.wbtracker.app.data.local.dao.ReviewSnapshotDao
import com.wbtracker.app.data.local.entity.PriceHistoryEntity
import com.wbtracker.app.data.local.entity.ProductEntity
import com.wbtracker.app.data.local.entity.ReviewSnapshotEntity
import com.wbtracker.app.data.remote.WbApiService
import com.wbtracker.app.data.remote.WbCardResult
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import com.wbtracker.app.domain.model.PricePoint
import com.wbtracker.app.domain.model.PriceStats
import com.wbtracker.app.domain.model.Product
import com.wbtracker.app.domain.model.ReviewPoint
import com.wbtracker.app.domain.model.ReviewStats
import com.wbtracker.app.domain.repository.ProductRepository
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject

import com.wbtracker.app.data.local.dao.NotificationRuleDao
import com.wbtracker.app.data.local.entity.NotificationRuleEntity

class ProductRepositoryImpl @Inject constructor(
    private val database: WbDatabase,
    private val productDao: ProductDao,
    private val priceHistoryDao: PriceHistoryDao,
    private val reviewSnapshotDao: ReviewSnapshotDao,
    private val notificationRuleDao: NotificationRuleDao,
    private val wbApiService: WbApiService,
    private val userPreferencesRepository: UserPreferencesRepository
) : ProductRepository {

    override suspend fun addProduct(articleId: Long): Result<Unit> {
        return refreshProduct(articleId)
    }

    override fun getTrackedProducts(): Flow<List<Product>> {
        return productDao.getAllTrackedProducts().map { entities ->
            entities.map { entity ->
                val latestPrice = priceHistoryDao.getLatestPrice(entity.id)
                val latestReview = reviewSnapshotDao.getLatestSnapshot(entity.id)
                val rule = notificationRuleDao.getRuleForProduct(entity.id)
                val sPrice = latestPrice?.sellerPrice ?: 0.0
                val wPrice = latestPrice?.walletPrice?.takeIf { it > 0 } ?: sPrice
                val trackingMode = userPreferencesRepository.priceTrackingMode.value
                val pPrice = latestPrice?.primaryPrice?.takeIf { it > 0.0 }
                    ?: (if (trackingMode == "wallet" && wPrice > 0) wPrice else sPrice)
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
                    initialWalletPrice = entity.initialWalletPrice.takeIf { it > 0.0 } ?: wPrice,
                    primaryPrice = pPrice,
                    rating = latestReview?.rating,
                    reviewsCount = latestReview?.reviewsCount,
                    isInStock = latestPrice?.isInStock ?: true,
                    lastUpdatedAt = entity.lastUpdatedAt,
                    isFavorite = entity.isFavorite,
                    targetPrice = rule?.targetPrice,
                    targetEnabled = rule?.isActive ?: false
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

        val trackingMode = userPreferencesRepository.priceTrackingMode.value
        val points = history.map {
            PricePoint(
                timestamp = it.timestamp,
                sellerPrice = it.sellerPrice,
                walletPrice = it.walletPrice,
                isInStock = it.isInStock,
                primaryPrice = it.primaryPrice.takeIf { p -> p > 0.0 } ?: (if (trackingMode == "wallet" && it.walletPrice > 0) it.walletPrice else it.sellerPrice)
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

    override suspend fun refreshProduct(articleId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val (cardResultData, prodObj) = coroutineScope {
                val cdnDeferred = async(Dispatchers.IO) {
                    withTimeoutOrNull(6000L) {
                        wbApiService.fetchCardInfo(articleId).getOrNull()
                    }
                }
                val detailDeferred = async(Dispatchers.IO) {
                    withTimeoutOrNull(6000L) {
                        wbApiService.fetchProductDetail(articleId).getOrNull()
                    }
                }

                val cardData = cdnDeferred.await()
                val detailResult = detailDeferred.await()

                var prod: JSONObject? = null
                if (detailResult != null) {
                    val array = detailResult.optJSONArray("products")
                        ?: detailResult.optJSONObject("data")?.optJSONArray("products")
                    if (array != null && array.length() > 0) {
                        prod = array.getJSONObject(0)
                    }
                }

                Pair(cardData, prod)
            }

            if (prodObj == null && cardResultData == null) {
                return@withContext Result.failure(Exception("Товар $articleId не найден в сети Wildberries"))
            }

            Log.d("ProductRepositoryImpl", "Product $articleId successfully retrieved in parallel (CDN: ${cardResultData != null}, Detail: ${prodObj != null})")

            var nameFromDetail = "Товар $articleId"
            var brandFromDetail = "Не указан"
            var ratingFromDetail: Double? = null
            var feedbacksFromDetail: Int? = null
            var basicPrice = 0.0
            var sellerPrice = 0.0
            var walletPrice = 0.0

            if (prodObj != null) {
                nameFromDetail = prodObj.optString("name", "Товар $articleId")
                brandFromDetail = prodObj.optString("brand", "Не указан")
                if (prodObj.has("reviewRating") || prodObj.has("rating")) {
                    val r = prodObj.optDouble("reviewRating", prodObj.optDouble("rating", 0.0))
                    if (r > 0) ratingFromDetail = r
                }
                if (prodObj.has("feedbacks")) {
                    val fc = prodObj.optInt("feedbacks", 0)
                    if (fc > 0) feedbacksFromDetail = fc
                }

                val sizesArray = prodObj.optJSONArray("sizes")
                if (sizesArray != null) {
                    for (i in 0 until sizesArray.length()) {
                        val sizeObj = sizesArray.optJSONObject(i) ?: continue
                        val priceObj = sizeObj.optJSONObject("price") ?: sizeObj
                        val productKopecks = priceObj.optDouble("product", priceObj.optDouble("priceU", 0.0))
                        val totalKopecks = priceObj.optDouble("total", priceObj.optDouble("salePriceU", 0.0))
                        val basicKopecks = priceObj.optDouble("basic", 0.0)

                        val rawSeller = if (totalKopecks > 0) totalKopecks else productKopecks
                        if (rawSeller > 0) {
                            val walletKp = extractWalletPriceKopecks(priceObj).let {
                                if (it > 0) it else extractWalletPriceKopecks(sizeObj)
                            }.let {
                                if (it > 0) it else extractWalletPriceKopecks(prodObj)
                            }
                            val rawWallet = if (walletKp > 0) walletKp else rawSeller
                            val rawBasic = maxOf(
                                if (basicKopecks > 0) basicKopecks else if (productKopecks > 0) productKopecks else rawSeller,
                                rawSeller
                            )

                            basicPrice = rawBasic / 100.0
                            sellerPrice = rawSeller / 100.0
                            walletPrice = rawWallet / 100.0
                            break
                        }
                    }
                }

                if (sellerPrice == 0.0) {
                    val salePriceU = prodObj.optDouble("salePriceU", prodObj.optDouble("total", 0.0))
                    val priceU = prodObj.optDouble("priceU", prodObj.optDouble("product", 0.0))
                    val rawSeller = if (salePriceU > 0) salePriceU else priceU
                    if (rawSeller > 0) {
                        val basicKopecks = prodObj.optDouble("basic", 0.0)
                        val rawBasic = maxOf(
                            if (basicKopecks > 0) basicKopecks else if (priceU > 0) priceU else rawSeller,
                            rawSeller
                        )
                        val walletKp = extractWalletPriceKopecks(prodObj)
                        val rawWallet = if (walletKp > 0) walletKp else rawSeller

                        sellerPrice = rawSeller / 100.0
                        basicPrice = rawBasic / 100.0
                        walletPrice = rawWallet / 100.0
                    }
                }
            } else if (cardResultData != null) {
                val cardJson = cardResultData.json
                nameFromDetail = cardJson.optString("imt_name")
                    .takeIf { it.isNotBlank() }
                    ?: cardJson.optString("subj_name")
                    .takeIf { it.isNotBlank() }
                    ?: "Товар $articleId"
                val selling = cardJson.optJSONObject("selling")
                brandFromDetail = selling?.optString("brand_name")?.takeIf { it.isNotBlank() } ?: "Не указан"
            }

            val actualBasketNum = cardResultData?.basketNum ?: wbApiService.getBasketNumber(articleId)
            val cardInfo = cardResultData?.json

            val sellerInfo = withTimeoutOrNull(1000L) {
                wbApiService.fetchSellerInfo(articleId, actualBasketNum).getOrNull()
            }

            val title = cardInfo?.optString("imt_name")?.takeIf { it.isNotBlank() }
                ?: nameFromDetail
            val sellingObj = cardInfo?.optJSONObject("selling")
            val brand = sellingObj?.optString("brand_name")?.takeIf { it.isNotBlank() }
                ?: brandFromDetail.takeIf { it.isNotBlank() }
                ?: "Не указан"

            var seller = brand
            var sellerId = sellingObj?.optLong("supplier_id") ?: 0L

            if (sellerInfo != null) {
                seller = sellerInfo.optString("supplierName").takeIf { !it.isNullOrEmpty() }
                    ?: sellerInfo.optString("supplierFullName").takeIf { !it.isNullOrEmpty() }
                    ?: seller
                val sId = sellerInfo.optLong("supplierId", 0L)
                if (sId != 0L) sellerId = sId
            }

            val category = cardInfo?.optString("subj_name", "") ?: ""
            val rootCategory = cardInfo?.optString("subj_root_name", "") ?: ""
            val vendorCode = cardInfo?.optString("vendor_code", "") ?: ""
            val description = cardInfo?.optString("description", "")?.takeIf { it.isNotBlank() }
                ?: "Описание недоступно"

            val mediaObj = cardInfo?.optJSONObject("media")
            val photoCount = mediaObj?.optInt("photo_count", 1) ?: 1
            val vol = articleId / 100000
            val part = articleId / 1000
            val thumbnailUrl = "https://basket-$actualBasketNum.wbbasket.ru/vol$vol/part$part/$articleId/images/big/1.webp"

            val rating = ratingFromDetail
            val reviewsCount = feedbacksFromDetail
            val isInStock = (sellerPrice > 0) || (prodObj != null) || (cardResultData != null)

            database.withTransaction {
                val existing = productDao.getProductById(articleId)
                val initWallet = if (existing == null || existing.initialWalletPrice <= 0.0) {
                    if (walletPrice > 0.0) walletPrice else sellerPrice
                } else {
                    existing.initialWalletPrice
                }

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
                    initialWalletPrice = initWallet,
                    addedAt = existing?.addedAt ?: System.currentTimeMillis(),
                    lastUpdatedAt = System.currentTimeMillis(),
                    isTracking = true,
                    isFavorite = existing?.isFavorite ?: false
                )

                if (existing == null) {
                    productDao.insertProduct(product)
                } else {
                    productDao.updateProduct(product)
                }
                val lastPrice = priceHistoryDao.getLatestPrice(articleId)
                val finalSellerPrice = if (sellerPrice > 0) sellerPrice else (lastPrice?.sellerPrice?.takeIf { it > 0 } ?: 0.0)
                val finalWalletPrice = if (walletPrice > 0) walletPrice else (lastPrice?.walletPrice?.takeIf { it > 0 } ?: 0.0)

                val trackingMode = userPreferencesRepository.priceTrackingMode.value
                val primary = when (trackingMode) {
                    "wallet" -> {
                        if (walletPrice > 0) walletPrice
                        else lastPrice?.primaryPrice?.takeIf { it > 0 } ?: finalSellerPrice
                    }
                    else -> finalSellerPrice
                }
                priceHistoryDao.insertPrice(
                    PriceHistoryEntity(
                        productId = articleId,
                        basicPrice = if (basicPrice > 0) basicPrice else (lastPrice?.basicPrice ?: finalSellerPrice),
                        sellerPrice = finalSellerPrice,
                        walletPrice = finalWalletPrice,
                        isInStock = isInStock,
                        primaryPrice = primary
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

            return@withContext Result.success(Unit)
        } catch (e: Exception) {
            return@withContext Result.failure(formatRussianError(e))
        }
    }

    override suspend fun stopTracking(articleId: Long) {
        productDao.stopTracking(articleId)
    }

    override suspend fun restoreProduct(articleId: Long) {
        productDao.setTracking(articleId, true)
    }

    override suspend fun toggleFavorite(articleId: Long) {
        productDao.toggleFavorite(articleId)
    }

    override suspend fun setFavorite(articleId: Long, isFavorite: Boolean) {
        productDao.setFavorite(articleId, isFavorite)
    }

    override suspend fun setTargetPrice(articleId: Long, price: Double, enabled: Boolean) {
        if (enabled && (price < 1.0 || price > 1_000_000.0)) {
            throw IllegalArgumentException("Целевая цена должна быть от 1 до 1 000 000 ₽")
        }
        val existing = notificationRuleDao.getRuleForProduct(articleId)
        if (existing != null) {
            notificationRuleDao.upsertRule(existing.copy(targetPrice = price, isActive = enabled))
        } else {
            notificationRuleDao.upsertRule(
                NotificationRuleEntity(
                    productId = articleId,
                    targetPrice = price,
                    targetDiscountPercent = null,
                    isActive = enabled
                )
            )
        }
    }

    override suspend fun importBackupJson(jsonString: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val trimmed = jsonString.trim()
            if (trimmed.isEmpty()) {
                return@withContext Result.failure(Exception("Файл резервной копии пуст"))
            }

            val productsArray: JSONArray = try {
                if (trimmed.startsWith("{")) {
                    val root = JSONObject(trimmed)
                    root.optJSONArray("products") ?: JSONArray()
                } else if (trimmed.startsWith("[")) {
                    JSONArray(trimmed)
                } else {
                    return@withContext Result.failure(Exception("Некорректный формат JSON"))
                }
            } catch (e: Exception) {
                return@withContext Result.failure(Exception("Ошибка парсинга JSON: ${e.localizedMessage}"))
            }

            var importedCount = 0

            database.withTransaction {
                for (i in 0 until productsArray.length()) {
                    val pObj = productsArray.optJSONObject(i) ?: continue
                    val idStr = pObj.optString("id", "").trim()
                    val id = idStr.toLongOrNull() ?: pObj.optLong("id", -1L)
                    if (id <= 0L) continue

                    val title = pObj.optString("title", pObj.optString("name", "Товар $id"))
                    val brand = pObj.optString("brand", "Не указан")
                    val seller = pObj.optString("seller", brand)
                    val category = pObj.optString("category", "")
                    val thumbnailUrl = pObj.optString("thumbnailUrl", pObj.optString("image", ""))
                    val walletPrice = pObj.optDouble("walletPrice", pObj.optDouble("price", 0.0))
                    val sellerPrice = pObj.optDouble("sellerPrice", pObj.optDouble("currentPrice", walletPrice))
                    val basicPrice = pObj.optDouble("basicPrice", pObj.optDouble("oldPrice", sellerPrice))
                    val initialWalletPrice = pObj.optDouble("initialWalletPrice", if (walletPrice > 0.0) walletPrice else sellerPrice)
                    val isFavorite = pObj.optBoolean("isFavorite", pObj.optBoolean("favorite", false))
                    val isInStock = pObj.optBoolean("isInStock", true)
                    val lastUpdatedAt = pObj.optLong("lastUpdatedAt", System.currentTimeMillis())

                    val existing = productDao.getProductById(id)
                    val entity = ProductEntity(
                        id = id,
                        title = title,
                        brand = brand,
                        seller = seller,
                        sellerId = existing?.sellerId ?: 0L,
                        category = category,
                        rootCategory = existing?.rootCategory ?: "",
                        vendorCode = existing?.vendorCode ?: "",
                        description = existing?.description ?: "Описание недоступно",
                        thumbnailUrl = thumbnailUrl.ifEmpty { existing?.thumbnailUrl ?: "" },
                        imagesCount = existing?.imagesCount ?: 1,
                        initialWalletPrice = if (existing != null && existing.initialWalletPrice > 0) existing.initialWalletPrice else initialWalletPrice,
                        addedAt = existing?.addedAt ?: System.currentTimeMillis(),
                        lastUpdatedAt = lastUpdatedAt,
                        isTracking = true,
                        isFavorite = isFavorite
                    )

                    if (existing == null) {
                        productDao.insertProduct(entity)
                    } else {
                        productDao.updateProduct(entity)
                    }

                    val historyArray = pObj.optJSONArray("priceHistory")
                    val trackingMode = userPreferencesRepository.priceTrackingMode.value
                    if (historyArray != null && historyArray.length() > 0) {
                        for (hIdx in 0 until historyArray.length()) {
                            val hObj = historyArray.optJSONObject(hIdx) ?: continue
                            val hWallet = hObj.optDouble("walletPrice", 0.0)
                            val hSeller = hObj.optDouble("sellerPrice", hWallet)
                            val hTimestamp = hObj.optLong("timestamp", System.currentTimeMillis())
                            val hPrimary = when (trackingMode) {
                                "wallet" -> if (hWallet > 0) hWallet else hSeller
                                else -> hSeller
                            }
                            priceHistoryDao.insertPrice(
                                PriceHistoryEntity(
                                    productId = id,
                                    basicPrice = basicPrice,
                                    sellerPrice = hSeller,
                                    walletPrice = hWallet,
                                    isInStock = isInStock,
                                    timestamp = hTimestamp,
                                    primaryPrice = hPrimary
                                )
                            )
                        }
                    } else if (walletPrice > 0.0 || sellerPrice > 0.0) {
                        val latest = priceHistoryDao.getLatestPrice(id)
                        if (latest == null) {
                            val primary = when (trackingMode) {
                                "wallet" -> if (walletPrice > 0) walletPrice else sellerPrice
                                else -> sellerPrice
                            }
                            priceHistoryDao.insertPrice(
                                PriceHistoryEntity(
                                    productId = id,
                                    basicPrice = basicPrice,
                                    sellerPrice = sellerPrice,
                                    walletPrice = walletPrice,
                                    isInStock = isInStock,
                                    timestamp = lastUpdatedAt,
                                    primaryPrice = primary
                                )
                            )
                        }
                    }

                    if (pObj.has("targetPrice") && !pObj.isNull("targetPrice")) {
                        val targetPrice = pObj.optDouble("targetPrice", 0.0)
                        val targetEnabled = pObj.optBoolean("targetEnabled", true)
                        if (targetPrice > 0.0) {
                            notificationRuleDao.upsertRule(
                                NotificationRuleEntity(
                                    productId = id,
                                    targetPrice = targetPrice,
                                    targetDiscountPercent = null,
                                    isActive = targetEnabled
                                )
                            )
                        }
                    }

                    importedCount++
                }
            }

            Result.success(importedCount)
        } catch (e: Exception) {
            Result.failure(Exception("Ошибка импорта: ${e.localizedMessage}"))
        }
    }

    private fun extractWalletPriceKopecks(priceObj: JSONObject): Double {
        val walletKeys = arrayOf(
            "wallet",
            "cpay",
            "walletPriceU",
            "cpayPriceU",
            "priceWithWallet",
            "walletPrice",
            "cpayPrice",
            "priceWithWalletU"
        )
        for (key in walletKeys) {
            if (priceObj.has(key) && !priceObj.isNull(key)) {
                val valKp = priceObj.optDouble(key, 0.0)
                if (valKp > 0) return valKp
            }
        }
        return 0.0
    }
}

