package com.wbtracker.app.bridge

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.os.Environment
import androidx.core.content.FileProvider
import com.wbtracker.app.data.local.dao.AlertHistoryDao
import com.wbtracker.app.data.local.entity.AlertHistoryEntity
import com.wbtracker.app.data.repository.UserPreferencesRepository
import com.wbtracker.app.domain.repository.ProductRepository
import com.wbtracker.app.domain.repository.SyncScheduler
import com.wbtracker.app.util.WbArticleExtractor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.lang.ref.WeakReference
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WbBridge @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: ProductRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val syncScheduler: SyncScheduler,
    private val alertHistoryDao: AlertHistoryDao
) {
    private var webViewRef: WeakReference<WebView>? = null

    var onRequestNotificationPermission: (() -> Unit)? = null
    var onImportBackupRequested: (() -> Unit)? = null

    @JavascriptInterface
    fun isBatteryOptimizationIgnored(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            return powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: true
        }
        return true
    }

    @JavascriptInterface
    fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (powerManager != null && !powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                    try {
                        val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(fallbackIntent)
                    } catch (fallbackEx: Exception) {
                        fallbackEx.printStackTrace()
                    }
                }
            }
        }
    }

    fun attachWebView(webView: WebView) {
        webViewRef = WeakReference(webView)
    }

    private fun evaluateJavascriptInWebView(script: String) {
        webViewRef?.get()?.post {
            webViewRef?.get()?.evaluateJavascript(script, null)
        }
    }

    // Formatting Utilities for Strict View-Only Architecture
    private fun formatMoney(amount: Double): String {
        val rounded = Math.round(amount)
        val formatter = NumberFormat.getNumberInstance(Locale("ru", "RU"))
        return "${formatter.format(rounded)} ₽"
    }

    private fun formatProductCount(count: Int): String {
        val rem100 = count % 100
        val rem10 = count % 10
        val word = when {
            rem100 in 11..19 -> "товаров"
            rem10 == 1 -> "товар"
            rem10 in 2..4 -> "товара"
            else -> "товаров"
        }
        return "$count $word"
    }

    private fun formatDropCount(count: Int): String {
        val rem100 = count % 100
        val rem10 = count % 10
        val word = when {
            rem100 in 11..19 -> "снижений"
            rem10 == 1 -> "снижение"
            rem10 in 2..4 -> "снижения"
            else -> "снижений"
        }
        return "$count $word"
    }

    private fun formatInsightPriceDropText(count: Int): String {
        val rem100 = count % 100
        val rem10 = count % 10
        val word = when {
            rem100 in 11..19 -> "товаров"
            rem10 == 1 -> "товар"
            rem10 in 2..4 -> "товара"
            else -> "товаров"
        }
        return "Снижение цены на $count $word"
    }

    private fun formatTimestamp(timestamp: Long): String {
        if (timestamp <= 0) return "Н/Д"
        val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale("ru", "RU"))
        return sdf.format(Date(timestamp))
    }

    private fun formatDateShort(timestamp: Long): String {
        if (timestamp <= 0) return ""
        val sdf = SimpleDateFormat("dd MMM", Locale("ru", "RU"))
        return sdf.format(Date(timestamp))
    }

    @JavascriptInterface
    fun getProductsJson(): String = runBlocking(Dispatchers.IO) {
        try {
            val products = repository.getTrackedProducts().first()
            val array = JSONArray()

            for (p in products) {
                val wPrice = p.walletPrice
                val sPrice = p.currentPrice
                val bPrice = p.basicPrice
                val iPrice = p.initialWalletPrice.takeIf { it > 0.0 } ?: wPrice

                val hasDiscount = bPrice > wPrice && bPrice > 0
                val discountPctFormatted = if (hasDiscount) {
                    val pct = Math.round(((bPrice - wPrice) / bPrice) * 100.0)
                    "-$pct%"
                } else ""

                val priceDeltaFormatted = if (iPrice > 0 && wPrice < iPrice) {
                    val deltaVal = wPrice - iPrice
                    val pct = Math.round((wPrice - iPrice) / iPrice * 100.0)
                    "${formatMoney(deltaVal)} (${pct}%)"
                } else if (iPrice > 0 && wPrice > iPrice) {
                    val deltaVal = wPrice - iPrice
                    val pct = Math.round((wPrice - iPrice) / iPrice * 100.0)
                    "+${formatMoney(deltaVal)} (+${pct}%)"
                } else ""

                val itemSavings = maxOf(0.0, iPrice - wPrice)

                val obj = JSONObject().apply {
                    put("id", p.id.toString())
                    put("url", "https://www.wildberries.ru/catalog/${p.id}/detail.aspx")
                    put("name", p.title)
                    put("title", p.title)
                    put("brand", p.brand)
                    put("seller", p.seller)
                    put("category", p.category)
                    put("image", p.thumbnailUrl)
                    put("thumbnailUrl", p.thumbnailUrl)
                    
                    // Raw numerical fields
                    put("price", p.primaryPrice)
                    put("primaryPrice", p.primaryPrice)
                    put("walletPrice", wPrice)
                    put("sellerPrice", sPrice)
                    put("currentPrice", sPrice)
                    put("oldPrice", bPrice)
                    put("basicPrice", bPrice)
                    put("initialWalletPrice", iPrice)
                    put("itemSavings", itemSavings)
                    put("rating", p.rating ?: 0.0)
                    put("reviewsCount", p.reviewsCount ?: 0)
                    put("favorite", p.isFavorite)
                    put("isFavorite", p.isFavorite)
                    put("targetPrice", p.targetPrice ?: JSONObject.NULL)
                    put("targetEnabled", p.targetEnabled)
                    put("isInStock", p.isInStock)
                    put("updatedAt", p.lastUpdatedAt)

                    // Strictly Pre-formatted Strings for View-Only Frontend
                    put("primaryPriceFormatted", formatMoney(p.primaryPrice))
                    put("walletPriceFormatted", formatMoney(wPrice))
                    put("sellerPriceFormatted", if (sPrice > wPrice) formatMoney(sPrice) else "")
                    put("basicPriceFormatted", if (bPrice > wPrice) formatMoney(bPrice) else "")
                    put("initialWalletPriceFormatted", formatMoney(iPrice))
                    put("itemSavingsFormatted", formatMoney(itemSavings))
                    put("priceDeltaFormatted", priceDeltaFormatted)
                    put("discountPercentFormatted", discountPctFormatted)
                    put("hasDiscount", hasDiscount)
                    put("ratingFormatted", String.format(Locale.US, "%.1f", p.rating ?: 0.0))
                    put("reviewsCountFormatted", "${p.reviewsCount ?: 0} отзывов")
                    put("lastUpdatedAtFormatted", formatTimestamp(p.lastUpdatedAt))
                }
                array.put(obj)
            }

            JSONObject().apply {
                put("status", "success")
                put("data", array)
            }.toString()
        } catch (e: Exception) {
            JSONObject().apply {
                put("status", "error")
                put("message", e.message ?: "Ошибка получения товаров")
            }.toString()
        }
    }

    @JavascriptInterface
    fun addProductAsync(urlOrArticle: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val rawInput = try {
                    val json = JSONObject(urlOrArticle)
                    json.optString("url", urlOrArticle)
                } catch (_: Exception) {
                    urlOrArticle
                }

                val articleId = WbArticleExtractor.extractArticleId(rawInput)
                    ?: run {
                        withContext(Dispatchers.Main) {
                            evaluateJavascriptInWebView("window.onProductAddError('Неверный артикул WB. Артикул должен содержать от 5 до 12 цифр.');")
                        }
                        return@launch
                    }

                val result = repository.addProduct(articleId)
                if (result.isSuccess) {
                    val products = repository.getTrackedProducts().first()
                    val added = products.find { it.id == articleId }
                    val title = added?.title ?: "Товар $articleId"
                    val escapedTitle = JSONObject.quote(title)
                    withContext(Dispatchers.Main) {
                        evaluateJavascriptInWebView("window.onProductAddSuccess($escapedTitle);")
                    }
                } else {
                    val err = result.exceptionOrNull()?.message ?: "Не удалось загрузить товар"
                    val escapedErr = JSONObject.quote(err)
                    withContext(Dispatchers.Main) {
                        evaluateJavascriptInWebView("window.onProductAddError($escapedErr);")
                    }
                }
            } catch (e: Exception) {
                val err = e.localizedMessage ?: "Ошибка добавления товара"
                val escapedErr = JSONObject.quote(err)
                withContext(Dispatchers.Main) {
                    evaluateJavascriptInWebView("window.onProductAddError($escapedErr);")
                }
            }
        }
    }

    @JavascriptInterface
    fun addProductByUrl(urlJson: String): String = runBlocking(Dispatchers.IO) {
        try {
            val rawInput = try {
                val json = JSONObject(urlJson)
                json.optString("url", urlJson)
            } catch (_: Exception) {
                urlJson
            }

            val articleId = WbArticleExtractor.extractArticleId(rawInput)
                ?: return@runBlocking JSONObject().apply {
                    put("status", "error")
                    put("message", "Неверный артикул WB. Артикул должен содержать от 5 до 12 цифр.")
                }.toString()

            val result = repository.addProduct(articleId)

            if (result.isSuccess) {
                val products = repository.getTrackedProducts().first()
                val added = products.find { it.id == articleId }

                JSONObject().apply {
                    put("status", "success")
                    put("message", "Товар успешно добавлен")
                    if (added != null) {
                        put("data", JSONObject().apply {
                            put("id", added.id.toString())
                            put("title", added.title)
                            put("walletPriceFormatted", formatMoney(added.walletPrice))
                        })
                    }
                }.toString()
            } else {
                val err = result.exceptionOrNull()?.message ?: "Не удалось загрузить товар"
                JSONObject().apply {
                    put("status", "error")
                    put("message", err)
                }.toString()
            }
        } catch (e: Exception) {
            JSONObject().apply {
                put("status", "error")
                put("message", e.message ?: "Ошибка добавления товара")
            }.toString()
        }
    }

    @JavascriptInterface
    fun deleteProduct(idJson: String): String = runBlocking(Dispatchers.IO) {
        try {
            val idStr = try {
                val json = JSONObject(idJson)
                json.optString("id", idJson)
            } catch (_: Exception) {
                idJson
            }
            val id = idStr.trim().toLongOrNull()
                ?: return@runBlocking JSONObject().apply {
                    put("status", "error")
                    put("message", "Некорректный ID")
                }.toString()

            repository.stopTracking(id)

            JSONObject().apply {
                put("status", "success")
                put("id", id.toString())
            }.toString()
        } catch (e: Exception) {
            JSONObject().apply {
                put("status", "error")
                put("message", e.message ?: "Ошибка удаления")
            }.toString()
        }
    }

    @JavascriptInterface
    fun restoreProduct(productIdJson: String): String = runBlocking(Dispatchers.IO) {
        try {
            val idStr = try {
                val json = JSONObject(productIdJson)
                json.optString("id", productIdJson)
            } catch (_: Exception) {
                productIdJson
            }
            val id = idStr.trim().toLongOrNull()
                ?: return@runBlocking JSONObject().apply {
                    put("status", "error")
                    put("message", "Некорректный ID")
                }.toString()

            repository.restoreProduct(id)

            JSONObject().apply {
                put("status", "success")
                put("id", id.toString())
            }.toString()
        } catch (e: Exception) {
            JSONObject().apply {
                put("status", "error")
                put("message", e.message ?: "Ошибка восстановления товара")
            }.toString()
        }
    }

    @JavascriptInterface
    fun toggleFavorite(idJson: String, fav: Boolean): String = runBlocking(Dispatchers.IO) {
        try {
            val idStr = try {
                val json = JSONObject(idJson)
                json.optString("id", idJson)
            } catch (_: Exception) {
                idJson
            }
            val id = idStr.trim().toLongOrNull()
                ?: return@runBlocking JSONObject().apply {
                    put("status", "error")
                    put("message", "Некорректный ID")
                }.toString()

            repository.setFavorite(id, fav)

            JSONObject().apply {
                put("status", "success")
                put("id", id.toString())
                put("favorite", fav)
                put("isFavorite", fav)
            }.toString()
        } catch (e: Exception) {
            JSONObject().apply {
                put("status", "error")
                put("message", e.message ?: "Ошибка изменения избранного")
            }.toString()
        }
    }

    @JavascriptInterface
    fun toggleFavorite(idJson: String): String = runBlocking(Dispatchers.IO) {
        try {
            var explicitFav: Boolean? = null
            val idStr = try {
                val json = JSONObject(idJson)
                if (json.has("favorite")) explicitFav = json.getBoolean("favorite")
                else if (json.has("isFavorite")) explicitFav = json.getBoolean("isFavorite")
                json.optString("id", idJson)
            } catch (_: Exception) {
                idJson
            }
            val id = idStr.trim().toLongOrNull()
                ?: return@runBlocking JSONObject().apply {
                    put("status", "error")
                    put("message", "Некорректный ID")
                }.toString()

            if (explicitFav != null) {
                repository.setFavorite(id, explicitFav)
            } else {
                repository.toggleFavorite(id)
            }

            val products = repository.getTrackedProducts().first()
            val item = products.find { it.id == id }
            val isFav = item?.isFavorite ?: false

            JSONObject().apply {
                put("status", "success")
                put("id", id.toString())
                put("favorite", isFav)
                put("isFavorite", isFav)
            }.toString()
        } catch (e: Exception) {
            JSONObject().apply {
                put("status", "error")
                put("message", e.message ?: "Ошибка изменения избранного")
            }.toString()
        }
    }

    @JavascriptInterface
    fun setTargetPrice(idJson: String, price: Double, enabled: Boolean): String = runBlocking(Dispatchers.IO) {
        try {
            val idStr = try {
                val json = JSONObject(idJson)
                json.optString("id", idJson)
            } catch (_: Exception) {
                idJson
            }
            val id = idStr.trim().toLongOrNull()
                ?: return@runBlocking JSONObject().apply {
                    put("status", "error")
                    put("message", "Некорректный ID")
                }.toString()

            if (enabled && (price < 1.0 || price > 1_000_000.0)) {
                return@runBlocking JSONObject().apply {
                    put("status", "error")
                    put("message", "Целевая цена должна быть от 1 до 1 000 000 ₽")
                }.toString()
            }

            repository.setTargetPrice(id, price, enabled)

            JSONObject().apply {
                put("status", "success")
                put("id", id.toString())
                put("targetPrice", price)
                put("targetEnabled", enabled)
            }.toString()
        } catch (e: Exception) {
            JSONObject().apply {
                put("status", "error")
                put("message", e.message ?: "Ошибка установки целевой цены")
            }.toString()
        }
    }

    @JavascriptInterface
    fun setTargetPrice(idJson: String): String = runBlocking(Dispatchers.IO) {
        try {
            val json = JSONObject(idJson)
            val idStr = json.optString("id", "")
            val price = json.optDouble("price", json.optDouble("targetPrice", 0.0))
            val enabled = json.optBoolean("enabled", json.optBoolean("targetEnabled", true))

            val id = idStr.trim().toLongOrNull()
                ?: return@runBlocking JSONObject().apply {
                    put("status", "error")
                    put("message", "Некорректный ID")
                }.toString()

            if (enabled && (price < 1.0 || price > 1_000_000.0)) {
                return@runBlocking JSONObject().apply {
                    put("status", "error")
                    put("message", "Целевая цена должна быть от 1 до 1 000 000 ₽")
                }.toString()
            }

            repository.setTargetPrice(id, price, enabled)

            JSONObject().apply {
                put("status", "success")
                put("id", id.toString())
                put("targetPrice", price)
                put("targetEnabled", enabled)
            }.toString()
        } catch (e: Exception) {
            JSONObject().apply {
                put("status", "error")
                put("message", e.message ?: "Ошибка установки целевой цены")
            }.toString()
        }
    }

    @JavascriptInterface
    fun getPriceHistory(idJson: String, days: Int): String {
        return getPriceHistoryInternal(idJson, days)
    }

    @JavascriptInterface
    fun getPriceHistory(idJson: String): String {
        return getPriceHistoryInternal(idJson, 30)
    }

    private fun getPriceHistoryInternal(idJson: String, daysParam: Int): String = runBlocking(Dispatchers.IO) {
        try {
            val (idStr, _) = try {
                val json = JSONObject(idJson)
                Pair(json.optString("id", idJson), json.optInt("days", daysParam))
            } catch (_: Exception) {
                Pair(idJson, daysParam)
            }
            val id = idStr.trim().toLongOrNull()
                ?: return@runBlocking JSONObject().apply {
                    put("status", "error")
                    put("message", "Некорректный ID")
                }.toString()

            val stats = repository.getPriceStats(id)
            val pointsArray = JSONArray()

            if (stats != null && stats.priceHistory.isNotEmpty()) {
                val chronHistory = stats.priceHistory.reversed()
                val prices = chronHistory.map { if (it.walletPrice > 0) it.walletPrice else it.sellerPrice }
                val minPrice = prices.minOrNull() ?: 0.0
                val maxPrice = prices.maxOrNull() ?: 0.0
                val count = chronHistory.size

                for ((idx, p) in chronHistory.withIndex()) {
                    val dateStr = formatDateShort(p.timestamp)
                    val priceVal = if (p.walletPrice > 0) p.walletPrice else p.sellerPrice

                    val xPct = if (count > 1) (idx.toDouble() / (count - 1)) * 100.0 else 50.0
                    val yPct = if (maxPrice > minPrice) 100.0 - ((priceVal - minPrice) / (maxPrice - minPrice)) * 100.0 else 50.0

                    pointsArray.put(JSONObject().apply {
                        put("xPercent", Math.round(xPct * 10.0) / 10.0)
                        put("yPercent", Math.round(yPct * 10.0) / 10.0)
                        put("priceFormatted", formatMoney(priceVal))
                        put("dateFormatted", dateStr)
                        put("price", priceVal)
                        put("timestamp", p.timestamp)
                    })
                }
            }

            JSONObject().apply {
                put("status", "success")
                put("hasData", pointsArray.length() > 0)
                put("points", pointsArray)
            }.toString()
        } catch (e: Exception) {
            JSONObject().apply {
                put("status", "error")
                put("message", e.message ?: "Ошибка получения истории цен")
            }.toString()
        }
    }

    @JavascriptInterface
    fun getReviews(idJson: String): String = runBlocking(Dispatchers.IO) {
        try {
            val idStr = try {
                val json = JSONObject(idJson)
                json.optString("id", idJson)
            } catch (_: Exception) {
                idJson
            }
            val id = idStr.trim().toLongOrNull()
                ?: return@runBlocking JSONObject().apply {
                    put("status", "error")
                    put("message", "Некорректный ID")
                }.toString()

            val products = repository.getTrackedProducts().first()
            val prod = products.find { it.id == id }
            val rating = prod?.rating ?: 0.0
            val count = prod?.reviewsCount ?: 0

            JSONObject().apply {
                put("status", "success")
                put("rating", rating)
                put("reviewsCount", count)
                put("ratingFormatted", if (rating > 0) String.format(Locale.US, "%.1f", rating) else "0.0")
                put("reviewsCountFormatted", "$count отзывов")
                put("items", JSONArray())
            }.toString()
        } catch (e: Exception) {
            JSONObject().apply {
                put("status", "error")
                put("message", e.message ?: "Ошибка получения отзывов")
            }.toString()
        }
    }

    @JavascriptInterface
    fun getAnalyticsSummary(): String = runBlocking(Dispatchers.IO) {
        try {
            val products = repository.getTrackedProducts().first()
            val hasProducts = products.isNotEmpty()

            if (!hasProducts) {
                return@runBlocking JSONObject().apply {
                    put("status", "success")
                    put("hasProducts", false)
                    put("totalSavingsFormatted", "0 ₽")
                    put("avgPriceFormatted", "0 ₽")
                    put("trackedCountFormatted", "0 товаров")
                    put("priceDropCountFormatted", "0 снижений")
                    put("chartData", JSONObject().apply {
                        put("points", JSONArray())
                        put("minPriceFormatted", "0 ₽")
                        put("maxPriceFormatted", "0 ₽")
                    })
                    put("insights", JSONArray())
                    put("savingsSparkline", JSONArray())
                }.toString()
            }

            // Calculation of savings strictly per Specification v5.1:
            // max(0, initialWalletPrice - currentWalletPrice)
            var totalSavings = 0.0
            var priceSum = 0.0
            var priceDropCount = 0

            for (p in products) {
                val currentWb = p.walletPrice
                val initWb = p.initialWalletPrice.takeIf { it > 0.0 } ?: currentWb

                priceSum += currentWb

                if (currentWb < initWb) {
                    val savings = initWb - currentWb
                    totalSavings += savings
                    priceDropCount++
                }
            }

            val avgPrice = priceSum / products.size
            val chartPoints = JSONArray()
            var minP: Double
            var maxP: Double

            val productHistories = products.mapNotNull { p ->
                val stats = repository.getPriceStats(p.id)
                val points = stats?.priceHistory?.reversed() ?: emptyList()
                if (points.isNotEmpty()) p to points else null
            }

            if (productHistories.isEmpty()) {
                val now = System.currentTimeMillis()
                chartPoints.put(JSONObject().apply {
                    put("xPercent", 50.0)
                    put("yPercent", 50.0)
                    put("priceFormatted", formatMoney(priceSum))
                    put("dateFormatted", formatDateShort(now))
                    put("price", priceSum)
                    put("timestamp", now)
                })
                minP = priceSum
                maxP = priceSum
            } else {
                val dayMs = 86400000L
                val allDayBuckets = productHistories.flatMap { (_, points) ->
                    points.map { it.timestamp / dayMs }
                }.distinct().sorted()

                val dayPrices = allDayBuckets.map { dayBucket ->
                    val dayTimestamp = dayBucket * dayMs
                    val activeProducts = productHistories.filter { (_, points) ->
                        val firstPoint = points.firstOrNull()
                        firstPoint != null && firstPoint.timestamp <= dayTimestamp + dayMs
                    }

                    if (activeProducts.isEmpty()) return@map null

                    val totalPriceOnDay = activeProducts.sumOf { (p, points) ->
                        val latestPointOnDay = points.lastOrNull { it.timestamp <= dayTimestamp + dayMs }
                            ?: points.firstOrNull()
                        latestPointOnDay?.let { if (it.walletPrice > 0) it.walletPrice else it.sellerPrice }
                            ?: p.walletPrice
                    }
                    dayTimestamp to totalPriceOnDay
                }.filterNotNull()

                if (dayPrices.size <= 1) {
                    val singlePair = dayPrices.firstOrNull()
                    val time = singlePair?.first ?: (System.currentTimeMillis() / dayMs * dayMs)
                    val totalDayPrice = singlePair?.second ?: priceSum
                    chartPoints.put(JSONObject().apply {
                        put("xPercent", 50.0)
                        put("yPercent", 50.0)
                        put("priceFormatted", formatMoney(totalDayPrice))
                        put("dateFormatted", formatDateShort(time))
                        put("price", totalDayPrice)
                        put("timestamp", time)
                    })
                    minP = totalDayPrice
                    maxP = totalDayPrice
                } else {
                    val pricesOnly = dayPrices.map { it.second }
                    minP = pricesOnly.minOrNull() ?: priceSum
                    maxP = pricesOnly.maxOrNull() ?: priceSum

                    val count = dayPrices.size
                    for ((idx, pair) in dayPrices.withIndex()) {
                        val time = pair.first
                        val priceVal = pair.second
                        val xPct = (idx.toDouble() / (count - 1)) * 100.0
                        val yPct = if (maxP > minP) 100.0 - ((priceVal - minP) / (maxP - minP)) * 100.0 else 50.0

                        chartPoints.put(JSONObject().apply {
                            put("xPercent", Math.round(xPct * 10.0) / 10.0)
                            put("yPercent", Math.round(yPct * 10.0) / 10.0)
                            put("priceFormatted", formatMoney(priceVal))
                            put("dateFormatted", formatDateShort(time))
                            put("price", priceVal)
                            put("timestamp", time)
                        })
                    }
                }
            }

            val savingsSparklineArray = JSONArray()
            if (productHistories.isNotEmpty()) {
                val dayMs = 86400000L
                val allDayBuckets = productHistories.flatMap { (_, points) ->
                    points.map { it.timestamp / dayMs }
                }.distinct().sorted()

                if (allDayBuckets.size >= 2) {
                    val last7Days = allDayBuckets.takeLast(7)
                    for (dayBucket in last7Days) {
                        val savingsForDay = productHistories.sumOf { (p, points) ->
                            val latestPointOnDay = points.lastOrNull { (it.timestamp / dayMs) <= dayBucket }
                            if (latestPointOnDay != null) {
                                val priceVal = if (latestPointOnDay.walletPrice > 0) latestPointOnDay.walletPrice else latestPointOnDay.sellerPrice
                                val initWb = p.initialWalletPrice.takeIf { it > 0.0 } ?: p.walletPrice
                                maxOf(0.0, initWb - priceVal)
                            } else {
                                0.0
                            }
                        }
                        savingsSparklineArray.put(Math.round(savingsForDay * 100.0) / 100.0)
                    }
                }
            }

            val insightsArray = JSONArray().apply {
                if (priceDropCount > 0) {
                    put(JSONObject().apply {
                        put("icon", "🔥")
                        put("text", formatInsightPriceDropText(priceDropCount))
                        put("time", "Сегодня")
                    })
                } else {
                    put(JSONObject().apply {
                        put("icon", "📈")
                        put("text", "Цены на отслеживаемые товары стабильны")
                        put("time", "Сегодня")
                    })
                }
                put(JSONObject().apply {
                    put("icon", "💡")
                    put("text", "Экономия за счет WB Кошелька вычисляется от начальной цены добавления")
                    put("time", "Вчера")
                })
            }

            JSONObject().apply {
                put("status", "success")
                put("hasProducts", true)
                put("totalSavingsFormatted", formatMoney(totalSavings))
                put("avgPriceFormatted", formatMoney(avgPrice))
                put("trackedCountFormatted", formatProductCount(products.size))
                put("priceDropCountFormatted", formatDropCount(priceDropCount))
                put("trackedCount", products.size)
                put("priceDropCount", priceDropCount)
                put("savedTotal", Math.round(totalSavings * 100.0) / 100.0)
                put("savingsSparkline", savingsSparklineArray)
                put("chartData", JSONObject().apply {
                    put("points", chartPoints)
                    put("minPriceFormatted", formatMoney(minP))
                    put("maxPriceFormatted", formatMoney(maxP))
                })
                put("insights", insightsArray)
            }.toString()
        } catch (e: Exception) {
            JSONObject().apply {
                put("status", "error")
                put("hasProducts", false)
                put("savingsSparkline", JSONArray())
                put("message", e.message ?: "Ошибка аналитики")
            }.toString()
        }
    }

    @JavascriptInterface
    fun setSyncInterval(jsonStr: String): String = runBlocking(Dispatchers.IO) {
        try {
            val hours: Long = try {
                val json = JSONObject(jsonStr)
                when {
                    json.has("hours") -> json.getLong("hours")
                    json.has("interval") -> json.getLong("interval")
                    json.has("syncInterval") -> json.getLong("syncInterval")
                    else -> jsonStr.filter { it.isDigit() }.toLongOrNull() ?: 6L
                }
            } catch (_: Exception) {
                jsonStr.filter { it.isDigit() }.toLongOrNull() ?: 6L
            }

            val validHours = when (hours.toInt()) {
                1 -> 1L
                3 -> 3L
                6 -> 6L
                12 -> 12L
                24 -> 24L
                else -> if (hours in 1..24) hours else 6L
            }

            val rem100 = validHours % 100
            val rem10 = validHours % 10
            val word = when {
                rem100 in 11L..19L -> "часов"
                rem10 == 1L -> "час"
                rem10 in 2L..4L -> "часа"
                else -> "часов"
            }
            val formattedText = "$validHours $word"

            userPreferencesRepository.setSyncInterval(formattedText)
            syncScheduler.schedulePeriodicUpdate(validHours)

            JSONObject().apply {
                put("status", "success")
                put("hours", validHours)
                put("syncInterval", formattedText)
                put("message", "Интервал фонового обновления изменен: $formattedText")
            }.toString()
        } catch (e: Exception) {
            JSONObject().apply {
                put("status", "error")
                put("message", e.message ?: "Ошибка сохранения интервала")
            }.toString()
        }
    }

    @JavascriptInterface
    fun getProfile(): String = runBlocking(Dispatchers.IO) {
        try {
            val darkTheme = userPreferencesRepository.isDarkTheme.value
            val pushEnabled = userPreferencesRepository.notificationsEnabled.value
            val interval = userPreferencesRepository.syncInterval.value

            val notificationIntervalFormatted = if (interval.startsWith("Каждые")) interval else "Каждые $interval"

            JSONObject().apply {
                put("status", "success")
                put("name", "Пользователь")
                put("initials", "П")
                put("plan", "Free")
                put("darkTheme", darkTheme)
                put("notificationIntervalFormatted", notificationIntervalFormatted)
                put("syncInterval", interval)
                put("pushEnabled", pushEnabled)
                put("priceTrackingMode", userPreferencesRepository.priceTrackingMode.value)
            }.toString()
        } catch (e: Exception) {
            JSONObject().apply {
                put("status", "error")
                put("message", e.message ?: "Ошибка профиля")
            }.toString()
        }
    }

    @JavascriptInterface
    fun getDarkTheme(): Boolean = runBlocking(Dispatchers.IO) {
        try {
            userPreferencesRepository.isDarkTheme.value
        } catch (_: Exception) {
            true
        }
    }

    @JavascriptInterface
    fun setDarkTheme(enabledJson: String): String = runBlocking(Dispatchers.IO) {
        try {
            val enabled = try {
                val json = JSONObject(enabledJson)
                json.optBoolean("enabled", true)
            } catch (_: Exception) {
                enabledJson.toBooleanStrictOrNull() ?: true
            }

            userPreferencesRepository.setDarkTheme(enabled)

            JSONObject().apply {
                put("status", "success")
                put("darkTheme", enabled)
            }.toString()
        } catch (e: Exception) {
            JSONObject().apply {
                put("status", "error")
                put("message", e.message ?: "Ошибка сохранения темы")
            }.toString()
        }
    }

    @JavascriptInterface
    fun setPriceTrackingMode(mode: String) {
        userPreferencesRepository.setPriceTrackingMode(mode)
    }

    @JavascriptInterface
    fun getPriceTrackingMode(): String {
        return userPreferencesRepository.priceTrackingMode.value
    }

    @JavascriptInterface
    fun setNotificationsEnabled(enabledJson: String): String = runBlocking(Dispatchers.IO) {
        try {
            val enabled = try {
                val json = JSONObject(enabledJson)
                json.optBoolean("enabled", true)
            } catch (_: Exception) {
                enabledJson.toBooleanStrictOrNull() ?: true
            }

            userPreferencesRepository.setNotificationsEnabled(enabled)

            JSONObject().apply {
                put("status", "success")
                put("notificationsEnabled", enabled)
            }.toString()
        } catch (e: Exception) {
            JSONObject().apply {
                put("status", "error")
                put("message", e.message ?: "Ошибка изменения уведомлений")
            }.toString()
        }
    }

    // --- Onboarding API ---

    @JavascriptInterface
    fun isOnboardingCompleted(): Boolean {
        return userPreferencesRepository.isOnboardingCompleted.value
    }

    @JavascriptInterface
    fun completeOnboarding() {
        userPreferencesRepository.setOnboardingCompleted(true)
    }

    // --- Alerts Center API ---

    @JavascriptInterface
    fun getAlerts(): String = runBlocking(Dispatchers.IO) {
        try {
            val alerts = alertHistoryDao.getAllAlerts()
            val array = JSONArray()
            for (a in alerts) {
                array.put(JSONObject().apply {
                    put("id", a.id)
                    put("productId", a.productId)
                    put("productTitle", a.productTitle)
                    put("thumbnailUrl", a.thumbnailUrl)
                    put("triggeredPrice", a.triggeredPrice)
                    put("targetPrice", a.targetPrice)
                    put("alertType", a.alertType)
                    put("timestamp", a.timestamp)
                    put("isRead", a.isRead)
                    put("triggeredPriceFormatted", formatMoney(a.triggeredPrice))
                    put("targetPriceFormatted", formatMoney(a.targetPrice))
                    put("oldPrice", a.oldPrice ?: JSONObject.NULL)
                    put("oldPriceFormatted", a.oldPrice?.let { formatMoney(it) } ?: JSONObject.NULL)
                    put("timestampFormatted", formatTimestamp(a.timestamp))
                })
            }
            JSONObject().apply {
                put("status", "success")
                put("data", array)
            }.toString()
        } catch (e: Exception) {
            JSONObject().apply {
                put("status", "error")
                put("message", e.message ?: "Ошибка получения уведомлений")
            }.toString()
        }
    }

    @JavascriptInterface
    fun getAlertsAsync(requestId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val jsonResult = getAlerts()
            withContext(Dispatchers.Main) {
                val escapedReq = JSONObject.quote(requestId)
                evaluateJavascriptInWebView("if (typeof window.onBridgeResult === 'function') { window.onBridgeResult($escapedReq, $jsonResult); }")
            }
        }
    }

    @JavascriptInterface
    fun markAlertRead(id: Long): String = runBlocking(Dispatchers.IO) {
        try {
            alertHistoryDao.markAsRead(id)
            JSONObject().apply {
                put("status", "success")
                put("id", id)
            }.toString()
        } catch (e: Exception) {
            JSONObject().apply {
                put("status", "error")
                put("message", e.message ?: "Ошибка обновления статуса уведомления")
            }.toString()
        }
    }

    @JavascriptInterface
    fun markAlertRead(idStr: String): String {
        val id = idStr.toLongOrNull() ?: try { JSONObject(idStr).optLong("id", -1L) } catch (_: Exception) { -1L }
        if (id != -1L) return markAlertRead(id)
        return JSONObject().apply {
            put("status", "error")
            put("message", "Некорректный ID")
        }.toString()
    }

    @JavascriptInterface
    fun clearAllAlerts(): String = runBlocking(Dispatchers.IO) {
        try {
            alertHistoryDao.clearAll()
            JSONObject().apply {
                put("status", "success")
            }.toString()
        } catch (e: Exception) {
            JSONObject().apply {
                put("status", "error")
                put("message", e.message ?: "Ошибка очистки уведомлений")
            }.toString()
        }
    }

    // --- Data Export & Backup API ---

    private fun saveFileAndShare(fileName: String, content: String, mimeType: String, title: String): String {
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            val file = File(downloadsDir, fileName)
            file.writeText(content, Charsets.UTF_8)

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(shareIntent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)

            JSONObject().apply {
                put("status", "success")
                put("message", "Файл $fileName сохранен в Downloads")
                put("filePath", file.absolutePath)
            }.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            JSONObject().apply {
                put("status", "error")
                put("message", e.message ?: "Ошибка экспорта файла")
            }.toString()
        }
    }

    @JavascriptInterface
    fun exportCsv(): String = runBlocking(Dispatchers.IO) {
        try {
            val products = repository.getTrackedProducts().first()
            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val fileName = "wb_tracker_export_${sdf.format(Date())}.csv"

            val sb = StringBuilder()
            sb.append('\uFEFF')
            sb.append("Артикул;Название;Бренд;Продавец;Категория;Цена WB;Базовая цена;Исходная цена;В наличии;Избранное;Дата обновления\n")

            val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

            for (p in products) {
                val titleEscaped = p.title.replace(";", ",").replace("\n", " ")
                val brandEscaped = p.brand.replace(";", ",").replace("\n", " ")
                val sellerEscaped = p.seller.replace(";", ",").replace("\n", " ")
                val categoryEscaped = p.category.replace(";", ",").replace("\n", " ")
                val updatedDate = dateFmt.format(Date(p.lastUpdatedAt))

                sb.append("${p.id};\"$titleEscaped\";\"$brandEscaped\";\"$sellerEscaped\";\"$categoryEscaped\";${p.walletPrice};${p.basicPrice};${p.initialWalletPrice};${if (p.isInStock) "Да" else "Нет"};${if (p.isFavorite) "Да" else "Нет"};\"$updatedDate\"\n")
            }

            saveFileAndShare(fileName, sb.toString(), "text/csv", "Экспорт товаров WB Tracker (CSV)")
        } catch (e: Exception) {
            JSONObject().apply {
                put("status", "error")
                put("message", e.message ?: "Ошибка экспорта CSV")
            }.toString()
        }
    }

    @JavascriptInterface
    fun exportJson(): String = runBlocking(Dispatchers.IO) {
        try {
            val products = repository.getTrackedProducts().first()
            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val fileName = "wb_tracker_backup_${sdf.format(Date())}.json"

            val root = JSONObject()
            root.put("version", 1)
            root.put("exportTimestamp", System.currentTimeMillis())

            val productsArray = JSONArray()
            for (p in products) {
                val pObj = JSONObject().apply {
                    put("id", p.id)
                    put("title", p.title)
                    put("brand", p.brand)
                    put("seller", p.seller)
                    put("category", p.category)
                    put("thumbnailUrl", p.thumbnailUrl)
                    put("walletPrice", p.walletPrice)
                    put("currentPrice", p.currentPrice)
                    put("basicPrice", p.basicPrice)
                    put("initialWalletPrice", p.initialWalletPrice)
                    put("targetPrice", p.targetPrice ?: JSONObject.NULL)
                    put("targetEnabled", p.targetEnabled)
                    put("isFavorite", p.isFavorite)
                    put("isInStock", p.isInStock)
                    put("lastUpdatedAt", p.lastUpdatedAt)

                    val stats = repository.getPriceStats(p.id)
                    val historyArray = JSONArray()
                    if (stats != null) {
                        for (h in stats.priceHistory) {
                            historyArray.put(JSONObject().apply {
                                put("walletPrice", h.walletPrice)
                                put("sellerPrice", h.sellerPrice)
                                put("timestamp", h.timestamp)
                            })
                        }
                    }
                    put("priceHistory", historyArray)
                }
                productsArray.put(pObj)
            }
            root.put("products", productsArray)

            saveFileAndShare(fileName, root.toString(2), "application/json", "Резервная копия WB Tracker (JSON)")
        } catch (e: Exception) {
            JSONObject().apply {
                put("status", "error")
                put("message", e.message ?: "Ошибка экспорта JSON")
            }.toString()
        }
    }

    // --- Shortcuts Action API ---

    @JavascriptInterface
    fun openProductById(productId: String): String {
        return try {
            val idStr = try {
                val json = JSONObject(productId)
                json.optString("id", productId)
            } catch (_: Exception) {
                productId
            }.trim().filter { it.isDigit() }

            val idToUse = if (idStr.isNotEmpty()) idStr else productId.trim()
            val url = "https://www.wildberries.ru/catalog/$idToUse/detail.aspx"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)

            JSONObject().apply {
                put("status", "success")
                put("url", url)
            }.toString()
        } catch (e: Exception) {
            JSONObject().apply {
                put("status", "error")
                put("message", e.message ?: "Ошибка открытия товара")
            }.toString()
        }
    }

    @JavascriptInterface
    fun openProductUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @JavascriptInterface
    fun openShortcutAction(action: String) {
        val escapedAction = JSONObject.quote(action)
        evaluateJavascriptInWebView("if (typeof handleShortcutAction === 'function') { handleShortcutAction($escapedAction); } else if (window.WbNative && typeof window.WbNative.handleShortcut === 'function') { window.WbNative.handleShortcut($escapedAction); }")
    }

    // --- Permissions & Notifications API ---

    @JavascriptInterface
    fun requestNotificationPermission() {
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        handler.post {
            onRequestNotificationPermission?.invoke() ?: run {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    try {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    // --- Theme & Style Packs API ---

    @JavascriptInterface
    fun getActiveTheme(): String {
        return userPreferencesRepository.activeTheme.value
    }

    @JavascriptInterface
    fun setActiveTheme(themeId: String): String {
        userPreferencesRepository.setActiveTheme(themeId)
        return JSONObject().apply {
            put("status", "success")
            put("activeTheme", themeId)
        }.toString()
    }

    @JavascriptInterface
    fun importBackup(): String {
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        handler.post {
            onImportBackupRequested?.invoke()
        }
        return JSONObject().apply {
            put("status", "info")
            put("message", "Выбор файла резервной копии...")
        }.toString()
    }
}
