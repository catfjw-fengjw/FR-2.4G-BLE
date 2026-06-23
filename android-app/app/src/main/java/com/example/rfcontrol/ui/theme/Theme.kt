package com.example.rfcontrol.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val RfBg = Color(0xFF10100F)
val RfPanel = Color(0xFF1A1A18)
val RfPanelStrong = Color(0xFF22221F)
val RfLine = Color(0xFF34342F)
val RfLineSoft = Color(0xFF272722)
val RfText = Color(0xFFF3F0E8)
val RfMuted = Color(0xFFA9A394)
val RfSubtle = Color(0xFF746F64)
val RfTeal = Color(0xFF28D6B0)
val RfAmber = Color(0xFFF2B84B)
val RfRed = Color(0xFFF06B63)
val RfBlue = Color(0xFF78A6FF)
val RfGreen = Color(0xFF82D66D)

private val RfColorScheme = darkColorScheme(
    primary = RfTeal,
    secondary = RfBlue,
    tertiary = RfAmber,
    background = RfBg,
    surface = RfPanel,
    onPrimary = Color(0xFF07110E),
    onSecondary = RfText,
    onTertiary = RfBg,
    onBackground = RfText,
    onSurface = RfText,
    error = RfRed,
    onError = RfText
)

@Composable
fun RfControlTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RfColorScheme,
        content = content
    )
}
