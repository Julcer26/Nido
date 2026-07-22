package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// Light Mode Colors
val LightBackground = Color(0xFFFAFAFA) // Soft off-white to reduce glare
val LightSurface = Color(0xFFFFFFFF)    // Clean card surface
val LightPrimary = Color(0xFF6366F1)    // Premium Indigo
val LightSecondary = Color(0xFF0EA5E9)  // Sky blue
val LightOnBackground = Color(0xFF0F172A) // Sleek slate for text
val LightOnSurfaceVariant = Color(0xFF64748B) // Subtitle text
val LightOutline = Color(0xFFE2E8F0)    // Gentle border

// Dark Mode Colors
val DarkBackground = Color(0xFF09090B)  // Premium ultra-dark grey (Zinc 950)
val DarkSurface = Color(0xFF18181B)     // Slightly elevated dark (Zinc 900)
val DarkPrimary = Color(0xFF818CF8)     // Brightened Indigo for contrast
val DarkSecondary = Color(0xFF38BDF8)   // Brightened Sky Blue
val DarkOnBackground = Color(0xFFFAFAFA)
val DarkOnSurfaceVariant = Color(0xFFA1A1AA)
val DarkOutline = Color(0xFF27272A)     // Subtle dark borders

val OutlineGrey = LightOutline          // Alias for legacy fallback

val SubtitleText: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onSurfaceVariant

// Auxiliary avatars & indicators
val ColorJulio = Color(0xFF3B82F6)          
val ColorAna = Color(0xFFEC4899)            
val AlertRed = Color(0xFFEF4444)            
val AlertRedBg = Color(0xFFFEF2F2)

// Glassmorphism & Mesh Background Colors
val MeshDarkPurple = Color(0xFF1E1B4B)
val MeshDarkBlue = Color(0xFF172554)
val MeshDarkTeal = Color(0xFF064E3B)

val MeshLightPurple = Color(0xFFE0E7FF)
val MeshLightBlue = Color(0xFFDBEAFE)
val MeshLightTeal = Color(0xFFD1FAE5)

val GlassBorderLight = Color(0xFFFFFFFF).copy(alpha = 0.4f)
val GlassBorderDark = Color(0xFFFFFFFF).copy(alpha = 0.1f)
val GlassSurfaceLight = Color(0xFFFFFFFF).copy(alpha = 0.35f)
val GlassSurfaceDark = Color(0xFF000000).copy(alpha = 0.35f)
