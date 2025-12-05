package com.smartstudy.utils

import androidx.compose.ui.graphics.Color

/**
 * Helper function to parse hex color string to Color
 */
fun hexToColor(hex: String): Color {
    return try {
        val cleanHex = hex.removePrefix("#")
        when (cleanHex.length) {
            6 -> {
                val r = cleanHex.substring(0, 2).toInt(16)
                val g = cleanHex.substring(2, 4).toInt(16)
                val b = cleanHex.substring(4, 6).toInt(16)
                Color(r, g, b)
            }
            8 -> {
                val a = cleanHex.substring(0, 2).toInt(16)
                val r = cleanHex.substring(2, 4).toInt(16)
                val g = cleanHex.substring(4, 6).toInt(16)
                val b = cleanHex.substring(6, 8).toInt(16)
                Color(r, g, b, a)
            }
            else -> Color(0xFF3498DB) // Default blue
        }
    } catch (e: Exception) {
        Color(0xFF3498DB) // Default blue on error
    }
}

