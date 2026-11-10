package com.example.api

import android.util.Base64

object GeminiConfig {
    // Lista de claves API principales y de respaldo en base64
    private val ENCODED_KEYS = listOf(
        "QVEuQWI4Uk42S3hTeVZuU1QyTGpEZ1ZKcW1SQWI0ZXhGNlhIaGQtS0tkT29iVzNLR2sxLWc="
    )

    fun getApiKeys(customUserKey: String? = null): List<String> {
        val keys = mutableListOf<String>()
        if (!customUserKey.isNullOrBlank()) {
            keys.add(customUserKey.trim())
        }
        for (encoded in ENCODED_KEYS) {
            try {
                val decoded = String(Base64.decode(encoded, Base64.DEFAULT)).trim()
                if (decoded.isNotBlank() && !keys.contains(decoded)) {
                    keys.add(decoded)
                }
            } catch (e: Exception) {
                // Ignore invalid key
            }
        }
        return keys
    }

    val API_KEY: String
        get() = getApiKeys().firstOrNull() ?: ""
}
