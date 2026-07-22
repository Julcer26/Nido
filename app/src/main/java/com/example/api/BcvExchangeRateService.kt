package com.example.api

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

object BcvExchangeRateService {
    private const val TAG = "BcvExchangeRate"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // Attempts to fetch the Venezuelan Bolívar official exchange rate (BCV rate)
    suspend fun fetchOfficialRate(): Double = withContext(Dispatchers.IO) {
        // We attempt a sequence of reliable free public APIs
        val urls = listOf(
            "https://ve.dolarapi.com/v1/dolares/oficial",
            "https://pydolarvenezuela-api.vercel.app/api/v1/dollar?page=bcv",
            "https://ve.bcvapi.com/v1/exchange",
            "https://bcv-api.vercel.app/api/v1/dollar"
        )

        for (url in urls) {
            try {
                Log.d(TAG, "Requesting rate from: $url")
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyStr = response.body?.string() ?: ""
                        Log.d(TAG, "Response from $url: $bodyStr")
                        if (bodyStr.isNotEmpty()) {
                            val json = JSONObject(bodyStr)
                            
                            // Try parsing different formats depending on url provider
                            if (url.contains("dolarapi.com")) {
                                val compra = json.optDouble("compra", 0.0)
                                val venta = json.optDouble("venta", 0.0)
                                val price = json.optDouble("promedio", 0.0)
                                val finalPrice = if (price > 0.0) price else {
                                    if (compra > 0.0 && venta > 0.0) (compra + venta) / 2.0 
                                    else if (compra > 0.0) compra 
                                    else venta
                                }
                                if (finalPrice > 0.0) return@withContext finalPrice
                            } else if (url.contains("pydolarvenezuela")) {
                                val monitors = json.optJSONObject("monitors")
                                if (monitors != null) {
                                    val bcvObj = monitors.optJSONObject("bcv") ?: monitors.optJSONObject("bcv_oficial")
                                    if (bcvObj != null) {
                                        val price = bcvObj.optDouble("price", 0.0)
                                        if (price > 0.0) return@withContext price
                                    }
                                }
                            } else if (url.contains("bcvapi")) {
                                val dataObj = json.optJSONObject("data")
                                if (dataObj != null) {
                                    val price = dataObj.optDouble("price", 0.0)
                                    if (price > 0.0) return@withContext price
                                } else {
                                    val price = json.optDouble("price", 0.0)
                                    if (price > 0.0) return@withContext price
                                }
                            } else {
                                // Default simple fallback lookups
                                val price = json.optDouble("price", 0.0)
                                if (price > 0.0) return@withContext price
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching from $url: ${e.localizedMessage}")
            }
        }
        
        // Return 0.0 indicating fetch failed, so the app will safely rely on its user-editable local database rate
        return@withContext 0.0
    }

    /**
     * Fetches the official BCV exchange rate for a specific past date (timestamp in ms).
     * If the date matches today's date, it returns the current official rate.
     * Otherwise it queries the dolarapi.com historical rates.
     */
    suspend fun fetchRateForDate(timestamp: Long): Double = withContext(Dispatchers.IO) {
        val caracasZone = java.util.TimeZone.getTimeZone("America/Caracas")
        
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).apply {
            timeZone = caracasZone
        }
        
        val todayStr = sdf.format(java.util.Date())
        val targetDateStr = sdf.format(java.util.Date(timestamp))
        
        Log.d(TAG, "fetchRateForDate: Target date is $targetDateStr, Today is $todayStr")
        
        if (targetDateStr == todayStr) {
            val currentRate = fetchOfficialRate()
            if (currentRate > 0.0) return@withContext currentRate
        }
        
        val url = "https://ve.dolarapi.com/v1/historicos/dolares/oficial"
        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    if (bodyStr.isNotEmpty()) {
                        val arr = org.json.JSONArray(bodyStr)
                        var closestRate = 0.0
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            val fecha = obj.optString("fecha", "") // e.g. "2026-07-02"
                            if (fecha.isNotEmpty() && fecha <= targetDateStr) {
                                val rate = obj.optDouble("promedio", 0.0)
                                if (rate > 0.0) {
                                    closestRate = rate
                                }
                            }
                        }
                        if (closestRate > 0.0) {
                            Log.d(TAG, "Found historic/closest rate for $targetDateStr: $closestRate")
                            return@withContext closestRate
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching historic rate for $targetDateStr: ${e.localizedMessage}")
        }
        
        return@withContext 0.0
    }
}
