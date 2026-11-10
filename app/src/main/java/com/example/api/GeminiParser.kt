package com.example.api

import android.graphics.Bitmap
import android.util.Log
import com.example.database.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ParsedReceiptItem(
    val name: String,
    val amount: Double,
    val category: String = ""
)

data class ParsedReceipt(
    val transaction: Transaction,
    val items: List<ParsedReceiptItem> = emptyList()
)

object GeminiParser {
    private const val TAG = "GeminiParser"

    // Ordered by speed & availability on the active gateway
    private val MODELS = listOf(
        "gemini-3.5-flash-lite",
        "gemini-flash-lite-latest",
        "gemini-3-flash-preview",
        "gemini-2.5-flash"
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private fun scaleBitmapForReceipt(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val targetWidth = 1024

        if (width > targetWidth) {
            val ratio = height.toFloat() / width.toFloat()
            val newHeight = (targetWidth * ratio).toInt().coerceAtMost(3200)
            return Bitmap.createScaledBitmap(bitmap, targetWidth, newHeight, true)
        } else if (width < 500 && width > 0) {
            val ratio = height.toFloat() / width.toFloat()
            val newWidth = 800
            val newHeight = (newWidth * ratio).toInt().coerceAtMost(2800)
            return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        }
        return bitmap
    }

    /**
     * Extracts the non-thought text from a Gemini candidate and normalizes it into a JSONObject,
     * automatically unwrapping JSON arrays (`[{...}]`) or markdown code fences (` ```json ... ``` `).
     */
    fun extractResultJsonObject(rawResponse: String): JSONObject? {
        val root = JSONObject(rawResponse)
        val candidates = root.optJSONArray("candidates") ?: return null
        if (candidates.length() == 0) return null

        val parts = candidates.getJSONObject(0)
            .optJSONObject("content")
            ?.optJSONArray("parts") ?: return null
        if (parts.length() == 0) return null

        var chosenText = ""
        for (i in 0 until parts.length()) {
            val part = parts.optJSONObject(i) ?: continue
            val isThought = part.optBoolean("thought", false)
            val txt = part.optString("text", "").trim()
            if (!isThought && txt.isNotEmpty()) {
                chosenText = txt
            }
        }
        if (chosenText.isEmpty()) {
            chosenText = parts.optJSONObject(parts.length() - 1)?.optString("text", "")?.trim() ?: ""
        }
        if (chosenText.isEmpty()) return null

        var text = chosenText
        if (text.startsWith("```json", ignoreCase = true)) {
            text = text.substring(7).trim()
        } else if (text.startsWith("```")) {
            text = text.substring(3).trim()
        }
        if (text.endsWith("```")) {
            text = text.removeSuffix("```").trim()
        }

        // If wrapped in a JSON array [ { ... } ], unwrap first object
        if (text.startsWith("[")) {
            val arr = JSONArray(text)
            if (arr.length() > 0) {
                return arr.optJSONObject(0)
            }
        }

        val firstBrace = text.indexOf('{')
        val lastBrace = text.lastIndexOf('}')
        if (firstBrace != -1 && lastBrace > firstBrace) {
            text = text.substring(firstBrace, lastBrace + 1)
        }

        return JSONObject(text)
    }

    private fun JSONObject.optNonNullString(key: String, default: String = ""): String {
        if (isNull(key)) return default
        val v = optString(key, default).trim()
        return if (v.isEmpty() || v.equals("null", ignoreCase = true)) default else v
    }

    private fun JSONObject.optFlexibleDouble(key: String, default: Double = 0.0): Double {
        if (isNull(key)) return default
        val num = optDouble(key, Double.NaN)
        if (!num.isNaN()) return num
        val rawStr = optString(key, "").replace("[^0-9.,]".toRegex(), "").replace(",", ".").trim()
        return rawStr.toDoubleOrNull() ?: default
    }

    /**
     * Executes request across available API keys and active models with fast failover.
     */
    private suspend fun executeGeminiRequest(
        jsonPayload: JSONObject,
        customUserKey: String? = null
    ): String? = withContext(Dispatchers.IO) {
        val apiKeys = GeminiConfig.getApiKeys(customUserKey)
        val requestBody = jsonPayload.toString().toRequestBody("application/json".toMediaType())

        for (apiKey in apiKeys) {
            for (model in MODELS) {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
                var retries = 0
                val maxRetries = 2

                while (retries < maxRetries) {
                    try {
                        val request = Request.Builder()
                            .url(url)
                            .post(requestBody)
                            .build()

                        client.newCall(request).execute().use { response ->
                            if (response.isSuccessful) {
                                val body = response.body?.string()
                                if (!body.isNullOrEmpty()) {
                                    return@withContext body
                                }
                            } else if (response.code == 429 || response.code == 503) {
                                Log.w(TAG, "Model $model status ${response.code} (attempt ${retries + 1}/$maxRetries)")
                                retries++
                                if (retries < maxRetries) {
                                    delay(450L * retries)
                                }
                            } else {
                                Log.e(TAG, "Model $model HTTP ${response.code}: ${response.body?.string()?.take(200)}")
                                break // Try next model immediately
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Exception calling Gemini $model", e)
                        break // Fail over to next model immediately on timeout/network error
                    }
                }
            }
        }
        return@withContext null
    }

    suspend fun parseTextToTransaction(
        userInput: String,
        user: String,
        availableCategories: List<String>,
        customUserKey: String? = null
    ): Triple<Transaction?, String?, String> = withContext(Dispatchers.IO) {
        val validCats = availableCategories.ifEmpty { listOf("Alimentos", "Servicios", "Transporte", "Hogar", "Otros") }
        val categoriesStr = validCats.joinToString { "\"$it\"" }
        val systemInstruction = """
            Eres un asistente de finanzas familiares en español para un hogar colaborativo.
            Tu única tarea es analizar la frase del usuario que describe un gasto, compra, pago o ingreso y extraer los datos en un objeto JSON estricto (NUNCA devuelvas un array, devuelve siempre un único objeto JSON {...}).
            Clasifica el gasto en UNA de las siguientes categorías válidas: [ $categoriesStr ]. Selecciona la que mejor corresponda.
            
            Detecta la moneda:
            - Si el texto menciona "bs", "bs.", "bolivares", "bolívares", "bov", "ves" o similares, el campo "currency" debe ser "VES" y "amount" el valor en Bolívares.
            - De lo contrario, por defecto "currency" es "USD".
            - Si el usuario indica que recibió un ingreso, cobro, sueldo o abono, incluye "isIncome": true (por defecto false).
            
            Estructura JSON obligatoria (un único objeto JSON):
            {
              "amount": 45.0,
              "currency": "USD",
              "category": "${validCats.first()}",
              "description": "Descripción breve en español",
              "isIncome": false,
              "date": null
            }
            Si la frase no tiene ningún monto numérico, devuelve: { "error": "Por favor incluye el monto del gasto (ej: Gasté 25$ en supermercado)" }
        """.trimIndent()

        val jsonRequest = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", userInput) })
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
                put("temperature", 0.1)
                put("maxOutputTokens", 1024)
            })
        }

        val rawResponse = executeGeminiRequest(jsonRequest, customUserKey)
            ?: return@withContext Triple(
                null,
                "El servicio de IA está ocupado temporalmente. Intenta de nuevo en unos segundos.",
                ""
            )

        try {
            val resultJson = extractResultJsonObject(rawResponse)
                ?: return@withContext Triple(null, "No se recibió respuesta válida de la IA.", "")

            val errorField = resultJson.optNonNullString("error")
            if (errorField.isNotEmpty()) {
                return@withContext Triple(null, errorField, "")
            }

            val amount = resultJson.optFlexibleDouble("amount", 0.0)
            if (amount <= 0.0) {
                return@withContext Triple(null, "No se detectó un monto válido en tu mensaje. Ejemplo: 'Gasté 20$ en farmacia'.", "")
            }

            val rawCurrency = resultJson.optNonNullString("currency", "USD").uppercase()
            val currency = if (rawCurrency.contains("BS") || rawCurrency.contains("VES") || rawCurrency.contains("BOL")) "VES" else "USD"
            val rawCat = resultJson.optNonNullString("category", validCats.first())
            val matchedCategory = validCats.find { it.equals(rawCat, ignoreCase = true) } ?: validCats.first()
            val description = resultJson.optNonNullString("description", userInput.trim()).replace(Regex("[\\r\\n]+"), " ").trim()
            val isIncome = resultJson.optBoolean("isIncome", false)
            val dateStr = resultJson.optNonNullString("date", "")
            val parsedDate = if (dateStr.isNotBlank()) {
                try {
                    java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse(dateStr)?.time ?: System.currentTimeMillis()
                } catch (e: Exception) {
                    System.currentTimeMillis()
                }
            } else {
                System.currentTimeMillis()
            }

            val tx = Transaction(
                amount = amount,
                category = matchedCategory,
                description = description,
                date = parsedDate,
                registeredBy = user,
                currency = currency,
                originalAmount = amount,
                isIncome = isIncome
            )

            val symbol = if (currency == "USD") "$" else "Bs."
            return@withContext Triple(tx, null, "Gasto detectado: $amount $symbol en '$description' [$matchedCategory]")
        } catch (e: Exception) {
            Log.e(TAG, "JSON parse error in parseTextToTransaction", e)
            return@withContext Triple(null, "No se pudo interpretar la respuesta: ${e.localizedMessage}", "")
        }
    }

    suspend fun parseReceiptImage(
        bitmap: Bitmap,
        user: String,
        availableCategories: List<String>,
        customUserKey: String? = null
    ): Triple<ParsedReceipt?, String?, String> = withContext(Dispatchers.IO) {
        val base64Image = try {
            val scaled = scaleBitmapForReceipt(bitmap)
            val outputStream = java.io.ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            android.util.Base64.encodeToString(outputStream.toByteArray(), android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Error encoding image", e)
            return@withContext Triple(null, "No se pudo procesar la foto del recibo", "")
        }

        val validCats = availableCategories.ifEmpty { listOf("Alimentos", "Servicios", "Transporte", "Hogar", "Otros") }
        val categoriesStr = validCats.joinToString { "\"$it\"" }
        val systemInstruction = """
            Eres un extractor ultra-preciso de recibos, facturas y comprobantes de pago en español para un hogar colaborativo.
            Analiza la imagen proporcionada y extrae los datos en un ÚNICO objeto JSON estricto (NUNCA devuelvas un array JSON, devuelve siempre un objeto {...}):
            - "amount": Monto total final pagado (Double).
            - "currency": "USD" o "VES". Si menciona Bolívares/Bs/Bs.S/Bs.D o precios típicos en bolívares es "VES". Si menciona $/USD/Dólares es "USD".
            - "category": Clasifica el gasto en una categoría de: [ $categoriesStr ].
            - "description": Nombre del comercio, tienda, banco o concepto general (ej. "Supermercado", "Farmatodo", "Pago Móvil").
            - "date": "YYYY-MM-DD" o null.
            - "items": Lista desglosada con los productos comprados (si aparecen en la imagen).
              REGLA DE IVA / IMPUESTOS:
              - NO incluyas IVA, IGTF o impuestos como líneas separadas.
              - Para productos gravados con IVA (16%), calcula su precio final multiplicando por 1.16.
              - Para productos exentos, mantén su precio exacto.
              Cada ítem contiene:
                * "name": Nombre del producto
                * "amount": Precio final con IVA incluido (Double)
                * "category": Categoría sugerida de [ $categoriesStr ]

            Estructura JSON obligatoria (un único objeto JSON):
            {
              "amount": 0.0,
              "currency": "USD",
              "category": "${validCats.first()}",
              "description": "Nombre tienda o concepto",
              "date": null,
              "items": [
                { "name": "Producto", "amount": 0.0, "category": "${validCats.first()}" }
              ]
            }
            Si la imagen no contiene ningún recibo, factura ni monto legible, devuelve: { "error": "No se pudo identificar un recibo o monto legible en la imagen" }
        """.trimIndent()

        val jsonRequest = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("inlineData", JSONObject().apply {
                                put("mimeType", "image/jpeg")
                                put("data", base64Image)
                            })
                        })
                        put(JSONObject().apply {
                            put("text", "Lee esta factura o comprobante y devuelve únicamente el objeto JSON con el total, moneda, comercio, fecha e ítems.")
                        })
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
                put("temperature", 0.1)
                put("maxOutputTokens", 4096)
            })
        }

        val rawResponse = executeGeminiRequest(jsonRequest, customUserKey)
            ?: return@withContext Triple(
                null,
                "El lector de facturas con IA está ocupado temporalmente. Intenta de nuevo en unos segundos.",
                ""
            )

        try {
            val resultJson = extractResultJsonObject(rawResponse)
                ?: return@withContext Triple(null, "No se recibió respuesta válida del lector de recibos.", "")

            val errorField = resultJson.optNonNullString("error")
            if (errorField.isNotEmpty()) {
                return@withContext Triple(null, errorField, "")
            }

            val rawCategory = resultJson.optNonNullString("category", validCats.first())
            val matchedCategory = validCats.find { it.equals(rawCategory, ignoreCase = true) } ?: validCats.first()

            val itemsList = mutableListOf<ParsedReceiptItem>()
            val itemsArray = resultJson.optJSONArray("items")
            if (itemsArray != null) {
                for (i in 0 until itemsArray.length()) {
                    val itemObj = itemsArray.optJSONObject(i) ?: continue
                    val name = itemObj.optNonNullString("name", "").trim()
                    val itemAmt = itemObj.optFlexibleDouble("amount", 0.0)
                    val itemCatRaw = itemObj.optNonNullString("category", matchedCategory)
                    val validCat = validCats.find { it.equals(itemCatRaw, ignoreCase = true) } ?: matchedCategory
                    if (name.isNotBlank() && itemAmt > 0.0) {
                        itemsList.add(ParsedReceiptItem(name = name, amount = itemAmt, category = validCat))
                    }
                }
            }

            var amount = resultJson.optFlexibleDouble("amount", 0.0)
            if (amount <= 0.0 && itemsList.isNotEmpty()) {
                amount = itemsList.sumOf { it.amount }
            }
            if (amount <= 0.0) {
                return@withContext Triple(null, "No se detectó un monto total legible en la imagen. Asegúrate de que la factura esté bien enfocada.", "")
            }

            val rawCurrency = resultJson.optNonNullString("currency", "USD").uppercase()
            val currency = if (rawCurrency.contains("BS") || rawCurrency.contains("VES") || rawCurrency.contains("BOL")) "VES" else "USD"
            val description = resultJson.optNonNullString("description", "Recibo escaneado")
                .replace(Regex("[\\r\\n]+"), " ")
                .trim()
                .ifBlank { "Recibo escaneado" }

            val dateStr = resultJson.optNonNullString("date", "")
            val parsedDate = if (dateStr.isNotBlank()) {
                try {
                    java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse(dateStr)?.time ?: System.currentTimeMillis()
                } catch (e: Exception) {
                    System.currentTimeMillis()
                }
            } else {
                System.currentTimeMillis()
            }

            val tx = Transaction(
                amount = amount,
                category = matchedCategory,
                description = description,
                date = parsedDate,
                registeredBy = user,
                currency = currency,
                originalAmount = amount
            )

            val parsedReceipt = ParsedReceipt(tx, itemsList)
            val symbol = if (currency == "USD") "$" else "Bs."
            val itemsDesc = if (itemsList.isNotEmpty()) " con ${itemsList.size} ítems desglosados" else ""
            val successMsg = "Factura procesada: $amount $symbol en '$description'$itemsDesc"
            return@withContext Triple(parsedReceipt, null, successMsg)
        } catch (e: Exception) {
            Log.e(TAG, "Receipt parse exception", e)
            return@withContext Triple(null, "Error al interpretar la factura: ${e.localizedMessage}", "")
        }
    }
}
