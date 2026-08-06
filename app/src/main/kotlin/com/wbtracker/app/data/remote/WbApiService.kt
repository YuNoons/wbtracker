package com.wbtracker.app.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
        return when {
            vol in 0..143 -> "01"
            vol in 144..287 -> "02"
            vol in 288..431 -> "03"
            vol in 432..719 -> "04"
            vol in 720..1007 -> "05"
            vol in 1008..1061 -> "06"
            vol in 1062..1115 -> "07"
            vol in 1116..1169 -> "08"
            vol in 1170..1313 -> "09"
            vol in 1314..1601 -> "10"
            vol in 1602..1655 -> "11"
            vol in 1656..1919 -> "12"
            vol in 1920..2045 -> "13"
            vol in 2046..2189 -> "14"
            vol in 2190..2405 -> "15"
            vol in 2406..2621 -> "16"
            vol in 2622..2837 -> "17"
            vol in 2838..3053 -> "18"
            vol in 3054..3269 -> "19"
            vol in 3270..3485 -> "20"
            vol in 3486..3701 -> "21"
            vol in 3702..3917 -> "22"
            vol in 3918..4133 -> "23"
            vol in 4134..4349 -> "24"
            vol in 4350..4565 -> "25"
            vol in 4566..4781 -> "26"
            vol in 4782..4997 -> "27"
            vol in 4998..5213 -> "28"
            vol in 5214..5429 -> "29"
            vol in 5430..5645 -> "30"
            vol in 5646..5861 -> "31"
            vol in 5862..6077 -> "32"
            vol in 6078..6293 -> "33"
            vol in 6294..6509 -> "34"
            else -> {
                val bIdx = 35 + (vol - 6510) / 216
                "%02d".format(bIdx)
            }
        }
    }

    suspend fun fetchProductDetail(articleId: Long): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val url = "https://card.wb.ru/cards/v4/detail?appType=1&curr=rub&dest=-1257786&spp=30&nm=$articleId"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                val responseCode = response.code
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP $responseCode"))
                }
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                Result.success(JSONObject(body))
            }
        } catch (e: Exception) {
            Log.e("WbApiService", "fetchProductDetail error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun fetchCardInfo(
        articleId: Long,
        initialBasketNum: String = getBasketNumber(articleId)
    ): Result<WbCardResult> = withContext(Dispatchers.IO) {
        val vol = articleId / 100000
        val part = articleId / 1000

        // 1. Try initial calculated basket
        val primaryUrl = "https://basket-$initialBasketNum.wbbasket.ru/vol$vol/part$part/$articleId/info/ru/card.json"
        val primaryJson = tryFetchCardJson(primaryUrl)
        if (primaryJson != null && isValidCardJson(primaryJson)) {
            return@withContext Result.success(WbCardResult(primaryJson, initialBasketNum))
        }

        // 2. Fallback probe across baskets 01..45
        for (i in 1..45) {
            val probeBasket = "%02d".format(i)
            if (probeBasket == initialBasketNum) continue

            val probeUrl = "https://basket-$probeBasket.wbbasket.ru/vol$vol/part$part/$articleId/info/ru/card.json"
            val probeJson = tryFetchCardJson(probeUrl)
            if (probeJson != null && isValidCardJson(probeJson)) {
                Log.d("WbApiService", "Fallback basket found: $probeBasket for article: $articleId")
                return@withContext Result.success(WbCardResult(probeJson, probeBasket))
            }
        }

        Result.failure(Exception("Товар $articleId не найден в каталоге WB"))
    }

    private fun tryFetchCardJson(url: String): JSONObject? {
        return try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
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
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP ${response.code}"))
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                Result.success(JSONObject(body))
            }
        } catch (e: Exception) {
            Log.e("WbApiService", "fetchSellerInfo error: ${e.message}", e)
            Result.failure(e)
        }
    }
}

