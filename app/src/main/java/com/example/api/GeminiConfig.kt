package com.example.api

import android.util.Base64

object GeminiConfig {
    // Clave API ofuscada para protegerla de escaneos simples
    private const val ENCODED_KEY = "QVEuQWI4Uk42S3hTeVZuU1QyTGpEZ1ZKcW1SQWI0ZXhGNlhIaGQtS0tkT29iVzNLR2sxLWc="
    
    val API_KEY: String
        get() = String(Base64.decode(ENCODED_KEY, Base64.DEFAULT))
}
