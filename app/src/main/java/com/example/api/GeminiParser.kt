package com.example.api

import android.util.Log
import android.graphics.Bitmap
import com.example.database.Transaction
import com.example.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

object GeminiParser {

    private const val TAG = "GeminiParser"
    private const val MODEL = "gemini-2.5-flash"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Parses user text into Transaction structured data dynamically.
     * Returns a Triple where:
     * - first is Transaction? (parsed successfully)
     * - second is String? (error message if any)
     * - third is String (visual success description)
     */
    suspend fun parseTextToTransaction(
        userInput: String, 
        user: String, 
        availableCategories: List<String>
    ): Triple<Transaction?, String?, String> = withContext(Dispatchers.IO) {
        val finalApiKey = GeminiConfig.API_KEY
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent?key=$finalApiKey"
        
        // Dynamic category list formatting
        val categoriesStr = availableCategories.joinToString { "\"$it\"" }
        
        val systemInstruction = """
            Eres un asistente de finanzas familiares en español para un hogar colaborativo (con perfiles como Julio, Ana, etc.). 
            Tu única tarea es analizar la frase del usuario que describe un gasto, compra, o pago y extraer los datos en un JSON estricto.
            Debes clasificar el gasto en UNA de las siguientes categorías válidas: [ $categoriesStr ]. Selecciona la que mejor corresponda. Si ninguna coincide exactamente, utiliza una apropiada de la lista.
            
            Además, debes detectar la moneda:
            - Si el texto menciona explícitamente "bs", "bs.", "bolivares", "bolívares", "bov", "bolas" o similares, el campo "currency" debe ser "VES". El campo "amount" debe ser el valor numérico en Bolívares que indicó el usuario.
            - De lo contrario, por defecto la moneda siempre es "USD" y el campo "currency" debe ser "USD". El campo "amount" será el valor numérico en Dólares.
            
            Debes devolver un JSON únicamente, sin formato Markdown ```json ... ```, sin espacios ni textos extras de explicación.
            La estructura JSON de respuesta exitosa es:
            {
              "amount": double,
              "currency": "USD" o "VES",
              "category": "Debe coincidir EXACTAMENTE con alguna de las categorías del sistema",
              "description": "Una breve descripción corta y descriptiva en español",
              "date": "YYYY-MM-DD" o null (Busca activamente si el texto describe una fecha, como por ejemplo 'ayer', 'hace una semana', 'el lunes pasado', 'el 25 de junio', etc. Calcula la fecha exacta correspondiente en formato YYYY-MM-DD basándote en la fecha de hoy, que es 2026-07-03. Si no se menciona o no indica fecha pasada, devuelve null)
            }
            Si la frase no tiene sentido financiero, no indica gasto o falta valor numérico, devuelve:
            {
              "error": "Explicación breve del error en español"
            }
        """.trimIndent()

        val jsonRequest = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", userInput)
                        })
                    })
                })
            })
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", systemInstruction)
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.1)
            })
        }

        val requestBody = jsonRequest.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e(TAG, "API failed: ${response.code} $errBody")
                    return@withContext Triple(
                        null,
                        "Error del servidor de Inteligencia Artificial (código ${response.code})",
                        ""
                    )
                }

                val responseBodyStr = response.body?.string()
                if (responseBodyStr.isNullOrEmpty()) {
                    return@withContext Triple(null, "Respuesta vacía o nula del servidor de IA", "")
                }

                Log.d(TAG, "Response: $responseBodyStr")

                // Parse standard Gemini structure
                val root = JSONObject(responseBodyStr)
                val candidates = root.optJSONArray("candidates")
                if (candidates == null || candidates.length() == 0) {
                    return@withContext Triple(null, "No se recibió respuesta válida del modelo de IA", "")
                }

                val firstCandidate = candidates.getJSONObject(0)
                val textResponse = firstCandidate
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
                    .trim()

                Log.d(TAG, "Extracted JSON text: $textResponse")

                val resultJson = JSONObject(textResponse)
                if (resultJson.has("error")) {
                    return@withContext Triple(null, resultJson.getString("error"), "")
                }

                val amount = resultJson.getDouble("amount")
                val currency = resultJson.optString("currency", "USD").uppercase()
                val category = resultJson.getString("category")
                val description = resultJson.getString("description")
                val dateStr = if (resultJson.has("date")) resultJson.optString("date", "") else ""
                val parsedDate = if (!dateStr.isNullOrBlank() && dateStr != "null") {
                    try {
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                        sdf.parse(dateStr)?.time ?: System.currentTimeMillis()
                    } catch (e: Exception) {
                        System.currentTimeMillis()
                    }
                } else {
                    System.currentTimeMillis()
                }

                // Return transaction obj
                val tx = Transaction(
                    amount = amount, // Temporary amount, repository will recalculate according to current rates
                    category = category,
                    description = description,
                    date = parsedDate,
                    registeredBy = user,
                    currency = currency,
                    originalAmount = amount
                )

                val currencySymbol = if (currency == "USD") "$" else "bs."
                val successMsg = "Se detectó exitosamente: $amount $currencySymbol en '$description' [$category]"
                return@withContext Triple(tx, null, successMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception calling Gemini", e)
            return@withContext Triple(null, "Error al comunicar con la IA: ${e.localizedMessage}", "")
        }
    }

    /**
     * Parses a receipt image into Transaction structured data dynamically.
     * Returns a Triple where:
     * - first is Transaction? (parsed successfully)
     * - second is String? (error message if any)
     * - third is String (visual success description)
     */
    suspend fun parseReceiptImage(
        bitmap: Bitmap,
        user: String,
        availableCategories: List<String>
    ): Triple<Transaction?, String?, String> = withContext(Dispatchers.IO) {
        val finalApiKey = GeminiConfig.API_KEY
        // Convert bitmap to Base64
        val base64Image = try {
            val outputStream = java.io.ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            android.util.Base64.encodeToString(outputStream.toByteArray(), android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Error encoding image", e)
            return@withContext Triple(null, "No se pudo procesar la imagen seleccionada", "")
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent?key=$finalApiKey"
        
        // Dynamic category list formatting
        val categoriesStr = availableCategories.joinToString { "\"$it\"" }
        
        val systemInstruction = """
            Eres un extractor inteligente de recibos y facturas en español para un hogar colaborativo.
            Se te proporcionará una imagen de un recibo o factura de compra. Debes analizarla cuidadosamente y extraer los siguientes datos en un formato JSON estricto:
            - "amount": El monto total pagado (intenta buscar el total/monto final). Debe ser un número decimal (Double).
            - "currency": La moneda ("USD" o "VES"). Si la factura contiene referencias a Bolívares, Bs., Bs.S, o similar, es "VES". Si contiene $, USD, dólares o el contexto sugiere dólares, es "USD".
            - "category": Clasifica el gasto en una de las siguientes categorías válidas: [ $categoriesStr ]. Selecciona la que mejor corresponda de la lista.
            - "description": Una breve descripción resumida (como el nombre del comercio, tienda o concepto general del recibo, ej. "Compra en Farmatodo", "Supermercado El Plaza", "Factura de Restaurant", etc.).
            - "date": "YYYY-MM-DD" o null (Busca la fecha de emisión del recibo o factura impresa en la imagen. Si encuentras una fecha, devuélvela en formato estricto YYYY-MM-DD. Si no se puede leer o no se encuentra, devuelve null)

            Debes devolver un JSON únicamente, sin formato Markdown ```json ... ```, sin espacios ni textos extras de explicación.
            La estructura JSON de respuesta exitosa es:
            {
              "amount": double,
              "currency": "USD" o "VES",
              "category": "Debe coincidir EXACTAMENTE con alguna de las categorías del sistema",
              "description": "Nombre de tienda o concepto",
              "date": "YYYY-MM-DD" o null
            }
            Si la imagen no parece ser un recibo, factura de pago o no tiene datos de gastos comprensibles, devuelve:
            {
              "error": "No se pudo identificar una factura o recibo válido en la imagen"
            }
        """.trimIndent()

        val jsonRequest = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        // Image part
                        put(JSONObject().apply {
                            put("inlineData", JSONObject().apply {
                                put("mimeType", "image/jpeg")
                                put("data", base64Image)
                            })
                        })
                        // Prompt text part
                        put(JSONObject().apply {
                            put("text", "Analiza este recibo o factura de compra y extrae la información solicitada.")
                        })
                    })
                })
            })
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", systemInstruction)
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.1)
            })
        }

        val requestBody = jsonRequest.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e(TAG, "API failed: ${response.code} $errBody")
                    return@withContext Triple(
                        null,
                        "Error del servidor de Inteligencia Artificial (código ${response.code})",
                        ""
                    )
                }

                val responseBodyStr = response.body?.string()
                if (responseBodyStr.isNullOrEmpty()) {
                    return@withContext Triple(null, "Respuesta vacía o nula del servidor de IA", "")
                }

                Log.d(TAG, "Response: $responseBodyStr")

                // Parse standard Gemini structure
                val root = JSONObject(responseBodyStr)
                val candidates = root.optJSONArray("candidates")
                if (candidates == null || candidates.length() == 0) {
                    return@withContext Triple(null, "No se recibió respuesta válida del modelo de IA", "")
                }

                val firstCandidate = candidates.getJSONObject(0)
                val textResponse = firstCandidate
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
                    .trim()

                Log.d(TAG, "Extracted JSON text: $textResponse")

                val resultJson = JSONObject(textResponse)
                if (resultJson.has("error")) {
                    return@withContext Triple(null, resultJson.getString("error"), "")
                }

                val amount = resultJson.getDouble("amount")
                val currency = resultJson.optString("currency", "USD").uppercase()
                val category = resultJson.getString("category")
                val description = resultJson.getString("description")
                val dateStr = if (resultJson.has("date")) resultJson.optString("date", "") else ""
                val parsedDate = if (!dateStr.isNullOrBlank() && dateStr != "null") {
                    try {
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                        sdf.parse(dateStr)?.time ?: System.currentTimeMillis()
                    } catch (e: Exception) {
                        System.currentTimeMillis()
                    }
                } else {
                    System.currentTimeMillis()
                }

                // Return transaction obj
                val tx = Transaction(
                    amount = amount,
                    category = category,
                    description = description,
                    date = parsedDate,
                    registeredBy = user,
                    currency = currency,
                    originalAmount = amount
                )

                val currencySymbolSuffix = if (currency == "USD") "$" else "bs."
                val successMsg = "Recibo escaneado: $amount $currencySymbolSuffix en '$description'"
                return@withContext Triple(tx, null, successMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception calling Gemini", e)
            return@withContext Triple(null, "Error al comunicar con la IA: ${e.localizedMessage}", "")
        }
    }
}
