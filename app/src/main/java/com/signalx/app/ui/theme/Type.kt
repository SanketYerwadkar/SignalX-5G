package com.signalx.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Sans = FontFamily.SansSerif

val SxTypography = Typography(
    displayLarge  = TextStyle(fontFamily = Sans, fontSize = 44.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp),
    headlineSmall = TextStyle(fontFamily = Sans, fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
    titleMedium   = TextStyle(fontFamily = Sans, fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge     = TextStyle(fontFamily = Sans, fontSize = 15.sp),
    bodyMedium    = TextStyle(fontFamily = Sans, fontSize = 13.sp),
    labelLarge    = TextStyle(fontFamily = Sans, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.3.sp),
    labelSmall    = TextStyle(fontFamily = Sans, fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.2.sp)
)

/** Technical readouts (dBm, dB, cell IDs) use tabular monospace. */
val SxMono = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp, fontWeight = FontWeight.Medium)
