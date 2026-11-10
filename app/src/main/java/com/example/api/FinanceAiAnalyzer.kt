package com.example.api

import android.util.Log
import com.example.database.CategoryDetail
import com.example.database.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

data class CategorySpendBreakdown(
    val name: String,
    val amountUsd: Double,
    val percentage: Int,
    val colorHex: String = ""
)

data class FinanceAiReport(
    val summary: String,
    val topSpendingCategory: String,
    val topSpendingAmountUsd: Double,
    val topSpendingPercentage: Int,
    val budgetHealth: String, // "Saludable", "Precaución", "Sobregirado"
    val topExpensesSummary: List<String>,
    val moneyLeaks: List<String>,
    val savingsTips: List<String>,
    val categoryBreakdown: List<CategorySpendBreakdown> = emptyList()
)

object FinanceAiAnalyzer {
    private const val TAG = "FinanceAiAnalyzer"

    private val MODELS = listOf(
        "gemini-3.5-flash-lite",
        "gemini-flash-lite-latest",
        "gemini-3-flash-preview",
        "gemini-2.5-flash"
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(22, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun analyze(
        transactions: List<Transaction>,
        categories: List<CategoryDetail>,
        monthName: String,
        customUserKey: String? = null
    ): Result<FinanceAiReport> = withContext(Dispatchers.IO) {
        try {
            val expenses = transactions.filter { !it.isIncome }
            if (expenses.isEmpty()) {
                return@withContext Result.success(
                    FinanceAiReport(
                        summary = "No se encontraron gastos registrados en este periodo ($monthName). Comienza a registrar tus compras para obtener un diagnóstico inteligente.",
                        topSpendingCategory = "Sin gastos",
                        topSpendingAmountUsd = 0.0,
                        topSpendingPercentage = 0,
                        budgetHealth = "Saludable",
                        topExpensesSummary = emptyList(),
                        moneyLeaks = emptyList(),
                        savingsTips = listOf("Registra tus compras diarias para poder proyectar tus ahorros.")
                    )
                )
            }

            val totalSpentUsd = expenses.sumOf { it.amount }

            // Group by category
            val categoryTotals = mutableMapOf<String, Double>()
            for (tx in expenses) {
                if (tx.items.isNotEmpty()) {
                    for (item in tx.items) {
                        val cat = item.category.ifBlank { tx.category }
                        val isBs = tx.currency.equals("VES", ignoreCase = true) || tx.currency.equals("bs", ignoreCase = true)
                        val rate = if (tx.bcvRateUsed > 0.0) tx.bcvRateUsed else 1.0
                        val amtUsd = if (isBs && rate > 0.0) item.amount / rate else item.amount
                        categoryTotals[cat] = (categoryTotals[cat] ?: 0.0) + amtUsd
                    }
                } else {
                    categoryTotals[tx.category] = (categoryTotals[tx.category] ?: 0.0) + tx.amount
                }
            }

            val topCategoryEntry = categoryTotals.maxByOrNull { it.value }
            val topCategoryName = topCategoryEntry?.key ?: "General"
            val topCategoryAmount = topCategoryEntry?.value ?: 0.0
            val topCategoryPercent = if (totalSpentUsd > 0.0) ((topCategoryAmount / totalSpentUsd) * 100).roundToInt() else 0

            val sampleItems = expenses.take(30).map { tx ->
                val itemsSummary = if (tx.items.isNotEmpty()) {
                    " [Items: " + tx.items.take(5).joinToString { "${it.name} (${it.amount})" } + "]"
                } else ""
                "- ${tx.description} (${tx.category}): ${String.format("%.2f", tx.amount)} USD$itemsSummary"
            }.joinToString("\n")

            val categoriesBreakdownStr = categoryTotals.entries.sortedByDescending { it.value }
                .joinToString("\n") { entry ->
                    val percent = if (totalSpentUsd > 0.0) ((entry.value / totalSpentUsd) * 100).roundToInt() else 0
                    "- ${entry.key}: $${String.format("%.2f", entry.value)} USD ($percent%)"
                }

            val breakdownList = categoryTotals.entries.sortedByDescending { it.value }.map { entry ->
                val percent = if (totalSpentUsd > 0.0) ((entry.value / totalSpentUsd) * 100).roundToInt() else 0
                val catColor = categories.find { it.categoryName.equals(entry.key, ignoreCase = true) }?.colorHex ?: "#6366F1"
                CategorySpendBreakdown(
                    name = entry.key,
                    amountUsd = entry.value,
                    percentage = percent,
                    colorHex = catColor
                )
            }

            val fallbackReport = FinanceAiReport(
                summary = "En $monthName has consumido un total de $${String.format("%.2f", totalSpentUsd)} USD. La categoría en la que más gastas es '$topCategoryName' representando el $topCategoryPercent% de tu dinero.",
                topSpendingCategory = topCategoryName,
                topSpendingAmountUsd = topCategoryAmount,
                topSpendingPercentage = topCategoryPercent,
                budgetHealth = if (topCategoryPercent > 50) "Precaución" else "Saludable",
                topExpensesSummary = listOf(
                    "La categoría '$topCategoryName' es el principal centro de costo con $${String.format("%.2f", topCategoryAmount)} USD ($topCategoryPercent%).",
                    "Total acumulado del mes: $${String.format("%.2f", totalSpentUsd)} USD distribuidos en ${categoryTotals.size} categorías."
                ),
                moneyLeaks = listOf(
                    "Revisa los ítems individuales de compras en '$topCategoryName' para detectar compras recurrentes no esenciales."
                ),
                savingsTips = listOf(
                    "Establece un tope semanal específico para $topCategoryName para no sobrepasar el presupuesto mensual.",
                    "Compara precios de víveres y charcutería antes de realizar compras grandes en un solo comercio.",
                    "Registra los recibos el mismo día para mantener el indicador de disponible al día."
                ),
                categoryBreakdown = breakdownList
            )

            val systemInstruction = """
                Eres un asesor financiero familiar inteligente y cercano, especializado en la economía de hogares colaborativos en Venezuela y Latinoamérica (manejando USD y Bolívares con tasa BCV).
                Tu objetivo es analizar los datos de gastos provistos y generar un diagnóstico preciso, claro y accionable en un ÚNICO objeto JSON estricto (nunca un array).
                
                Debes responder ÚNICAMENTE con un objeto JSON válido con la siguiente estructura:
                {
                  "summary": "Resumen ejecutivo en 2 o 3 oraciones destacando en qué se fue la mayor parte del dinero y el estado del mes.",
                  "budgetHealth": "Saludable" o "Precaución" o "Sobregirado",
                  "topExpensesSummary": [
                    "Detalle o producto/categoría con mayor impacto (ej: 'Carne & Charcutería absorbió el 35% del presupuesto total')",
                    "Segundo punto de gasto relevante con cifras"
                  ],
                  "moneyLeaks": [
                    "Detección de posibles gastos hormiga, pagos duplicados o compras menores frecuentes",
                    "Observación sobre comercios o productos secundarios"
                  ],
                  "savingsTips": [
                    "Consejo práctico #1 para reducir el gasto más alto",
                    "Consejo práctico #2 sobre planificación familiar de compras",
                    "Consejo práctico #3 para el siguiente mes"
                  ]
                }
            """.trimIndent()

            val userPrompt = """
                Mes: $monthName
                Gasto Total: $${String.format("%.2f", totalSpentUsd)} USD
                Categoría con más gasto: $topCategoryName ($${String.format("%.2f", topCategoryAmount)} USD, $topCategoryPercent% del total)
                
                Distribución por Categorías:
                $categoriesBreakdownStr
                
                Muestra de transacciones e ítems:
                $sampleItems
                
                Analiza estos gastos y genera el reporte financiero en JSON.
            """.trimIndent()

            val jsonPayload = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", userPrompt) })
                        })
                    })
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", systemInstruction) })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.2)
                    put("maxOutputTokens", 1536)
                })
            }

            val apiKeys = GeminiConfig.getApiKeys(customUserKey)
            val requestBody = jsonPayload.toString().toRequestBody("application/json".toMediaType())
            var rawResponse: String? = null

            for (apiKey in apiKeys) {
                for (model in MODELS) {
                    val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
                    try {
                        val request = Request.Builder()
                            .url(url)
                            .post(requestBody)
                            .build()

                        client.newCall(request).execute().use { response ->
                            if (response.isSuccessful) {
                                val body = response.body?.string()
                                if (!body.isNullOrEmpty()) {
                                    rawResponse = body
                                    break
                                }
                            } else {
                                Log.w(TAG, "Model $model returned code ${response.code}")
                            }
                        }
                        if (rawResponse != null) break
                    } catch (e: Exception) {
                        Log.w(TAG, "Error querying $model", e)
                    }
                }
                if (rawResponse != null) break
            }

            val responseStr = rawResponse ?: return@withContext Result.success(fallbackReport)
            val resultJson = try {
                GeminiParser.extractResultJsonObject(responseStr)
            } catch (e: Exception) {
                null
            } ?: return@withContext Result.success(fallbackReport)

            val summary = resultJson.optString("summary", "").takeIf { it.isNotBlank() && it != "null" }
                ?: fallbackReport.summary
            val budgetHealth = resultJson.optString("budgetHealth", "").takeIf { it.isNotBlank() && it != "null" }
                ?: fallbackReport.budgetHealth

            val topExpensesList = mutableListOf<String>()
            val topExpArray = resultJson.optJSONArray("topExpensesSummary")
            if (topExpArray != null) {
                for (i in 0 until topExpArray.length()) {
                    val item = topExpArray.optString(i).trim()
                    if (item.isNotBlank() && item != "null") topExpensesList.add(item)
                }
            }
            if (topExpensesList.isEmpty()) {
                topExpensesList.addAll(fallbackReport.topExpensesSummary)
            }

            val moneyLeaksList = mutableListOf<String>()
            val leaksArray = resultJson.optJSONArray("moneyLeaks")
            if (leaksArray != null) {
                for (i in 0 until leaksArray.length()) {
                    val item = leaksArray.optString(i).trim()
                    if (item.isNotBlank() && item != "null") moneyLeaksList.add(item)
                }
            }
            if (moneyLeaksList.isEmpty()) {
                moneyLeaksList.addAll(fallbackReport.moneyLeaks)
            }

            val savingsTipsList = mutableListOf<String>()
            val tipsArray = resultJson.optJSONArray("savingsTips")
            if (tipsArray != null) {
                for (i in 0 until tipsArray.length()) {
                    val item = tipsArray.optString(i).trim()
                    if (item.isNotBlank() && item != "null") savingsTipsList.add(item)
                }
            }
            if (savingsTipsList.isEmpty()) {
                savingsTipsList.addAll(fallbackReport.savingsTips)
            }

            Result.success(
                FinanceAiReport(
                    summary = summary,
                    topSpendingCategory = topCategoryName,
                    topSpendingAmountUsd = topCategoryAmount,
                    topSpendingPercentage = topCategoryPercent,
                    budgetHealth = budgetHealth,
                    topExpensesSummary = topExpensesList,
                    moneyLeaks = moneyLeaksList,
                    savingsTips = savingsTipsList,
                    categoryBreakdown = breakdownList
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error in financial AI analysis", e)
            Result.failure(e)
        }
    }
}
