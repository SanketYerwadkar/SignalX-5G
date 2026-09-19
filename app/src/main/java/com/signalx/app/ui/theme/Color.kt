package com.signalx.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** SignalX design tokens - dark technical telecom palette. */
object SxColor {
    val Background      = Color(0xFF05070A)
    val Surface         = Color(0xFF0B0F15)
    val SurfaceElevated = Color(0xFF121821)
    val Outline         = Color(0xFF1E2733)

    val Cyan            = Color(0xFF22D3EE)
    val CyanBright      = Color(0xFF67E8F9)
    val Blue            = Color(0xFF2563EB)
    val BlueDeep        = Color(0xFF1E3A8A)

    val TextPrimary     = Color(0xFFF2F6FA)
    val TextSecondary   = Color(0xFF9AA7B4)
    val TextTertiary    = Color(0xFF64707D)

    val Success         = Color(0xFF34D399)
    val Warning         = Color(0xFFFBBF24)
    val Danger          = Color(0xFFF87171)

    val LBackground     = Color(0xFFF6F8FB)
    val LSurface        = Color(0xFFFFFFFF)
    val LSurfaceVariant = Color(0xFFEDF1F6)
    val LOutline        = Color(0xFFDCE3EC)
    val LTextPrimary    = Color(0xFF0B1220)
    val LTextSecondary  = Color(0xFF5A6675)
    val BlueInk         = Color(0xFF0E7490)

    val XGradient      = Brush.linearGradient(listOf(CyanBright, Cyan, Blue))
    val AccentGradient = Brush.horizontalGradient(listOf(Cyan, Blue))
    val CardGradient   = Brush.verticalGradient(listOf(Color(0xFF13202C), Color(0xFF0B1118)))
}
