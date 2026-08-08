package com.wbtracker.app.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import android.util.Log

data class WbCardResult(
    val json: JSONObject,
    val basketNum: String
)

class WbApiService(private val client: OkHttpClient) {

    fun getBasketNumber(article: Long): String {
        val vol = article / 100000
        val basket = when {
            vol in 0..143 -> 1
            vol in 144..287 -> 2
            vol in 288..431 -> 3
            vol in 432..719 -> 4
            vol in 720..1007 -> 5
            vol in 1008..1061 -> 6
            vol in 1062..1115 -> 7
            vol in 1116..1169 -> 8
            vol in 1170..1313 -> 9
            vol in 1314..1601 -> 10
            vol in 1602..1655 -> 11
            vol in 1656..1919 -> 12
            vol in 1920..2045 -> 13
            vol in 2046..2189 -> 14
            vol in 2190..2405 -> 15
            vol in 2406..2621 -> 16
            vol in 2622..2837 -> 17
            vol in 2838..3053 -> 18
            vol in 3054..3269 -> 19
            vol in 3270..3485 -> 20
            else -> 21 + (vol - 3486) / 216
        }
        return "%02d".format(basket)
    }

    private val fastClient = client.newBuilder()
        .connectTimeout(6, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(6, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(6, java.util.concurrent.TimeUnit.SECONDS)
        .callTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    suspend fun fetchProductDetailV1(articleId: Long): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val url = "https://card.wb.ru/cards/v1/detail?appType=1&curr=rub&dest=-1257786&spp=30&nm=$articleId"
            val request = Request.Builder().url(url).build()
            fastClient.newCall(request).execute().use { response ->
                val responseCode = response.code
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP $responseCode"))
                }
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                Result.success(JSONObject(body))
            }
        } catch (e: Exception) {
            Log.e("WbApiService", "fetchProductDetailV1 error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun fetchProductDetailV2(articleId: Long): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val url = "https://card.wb.ru/cards/v2/detail?appType=1&curr=rub&nm=$articleId"
            val request = Request.Builder().url(url).build()
            fastClient.newCall(request).execute().use { response ->
                val responseCode = response.code
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP $responseCode"))
                }
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                Result.success(JSONObject(body))
            }
        } catch (e: Exception) {
            Log.e("WbApiService", "fetchProductDetailV2 error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun fetchProductDetailV4(articleId: Long): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val url = "https://card.wb.ru/cards/v4/detail?appType=1&curr=rub&dest=-1257786&spp=30&nm=$articleId"
            val request = Request.Builder().url(url).build()
            fastClient.newCall(request).execute().use { response ->
                val responseCode = response.code
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP $responseCode"))
                }
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                Result.success(JSONObject(body))
            }
        } catch (e: Exception) {
            Log.e("WbApiService", "fetchProductDetailV4 error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun fetchProductDetail(articleId: Long): Result<JSONObject> = withTimeoutOrNull(6000L) {
        withContext(Dispatchers.IO) {
            val v1Res = fetchProductDetailV1(articleId)
            if (v1Res.isSuccess) return@withContext v1Res

            val v2Res = fetchProductDetailV2(articleId)
            if (v2Res.isSuccess) return@withContext v2Res

            fetchProductDetailV4(articleId)
        }
    } ?: Result.failure(Exception("Таймаут API цен WB (6с)"))

    suspend fun fetchCardInfo(
        articleId: Long,
        initialBasketNum: String = getBasketNumber(articleId)
    ): Result<WbCardResult> = withTimeoutOrNull(6000L) {
        withContext(Dispatchers.IO) {
            val vol = articleId / 100000
            val part = articleId / 1000

            // 1. Primary attempt on calculated basket
            val primaryUrl = "https://basket-$initialBasketNum.wbbasket.ru/vol$vol/part$part/$articleId/info/ru/card.json"
            val primaryJson = tryFetchCardJson(primaryUrl)
            if (primaryJson != null && isValidCardJson(primaryJson)) {
                return@withContext Result.success(WbCardResult(primaryJson, initialBasketNum))
            }

            // 2. Parallel fallback probe across baskets 1..45
            val initialInt = initialBasketNum.toIntOrNull() ?: 1
            val deferreds = (1..45).filter { it != initialInt }.map { b ->
                val probeBasket = "%02d".format(b)
                async(Dispatchers.IO) {
                    val probeUrl = "https://basket-$probeBasket.wbbasket.ru/vol$vol/part$part/$articleId/info/ru/card.json"
                    val json = tryFetchCardJson(probeUrl)
                    if (json != null && isValidCardJson(json)) {
                        WbCardResult(json, probeBasket)
                    } else null
                }
            }
            val found = deferreds.awaitAll().firstOrNull { it != null }
            if (found != null) {
                Log.d("WbApiService", "Fallback basket found: ${found.basketNum} for article: $articleId")
                return@withContext Result.success(found)
            }

            Result.failure(Exception("card.json недоступен для артикула $articleId"))
        }
    } ?: Result.failure(Exception("Таймаут CDN карточки WB (6с)"))

    private fun tryFetchCardJson(url: String): JSONObject? {
        return try {
            val request = Request.Builder().url(url).build()
            fastClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrEmpty()) {
                        return JSONObject(body)
                    }
                } else {
                    Log.w("WbApiService", "Card JSON fetch returned HTTP ${response.code} for $url")
                }
                null
            }
        } catch (e: Exception) {
            Log.w("WbApiService", "Card JSON fetch error for $url: ${e.message}")
            null
        }
    }

    private fun isValidCardJson(json: JSONObject): Boolean {
        return json.has("imt_name") || json.has("selling") || json.has("subj_name")
    }

    suspend fun fetchSellerInfo(articleId: Long, basketNum: String): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val vol = articleId / 100000
            val part = articleId / 1000
            val url = "https://basket-$basketNum.wbbasket.ru/vol$vol/part$part/$articleId/info/sellers.json"
            val request = Request.Builder().url(url).build()
            fastClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP ${response.code}"))
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                Result.success(JSONObject(body))
            }
        } catch (e: Exception) {
            Log.e("WbApiService", "fetchSellerInfo error: ${e.message}")
            Result.failure(e)
        }
    }
}


