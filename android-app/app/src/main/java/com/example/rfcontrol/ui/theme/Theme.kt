package com.example.rfcontrol.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val RfBg = Color(0xFFFFF8FC)
val RfPanel = Color(0xFFFFFFFF)
val RfPanelStrong = Color(0xFFFFECF6)
val RfLine = Color(0xFFF4CDE1)
val RfLineSoft = Color(0xFFF8DFEC)
val RfText = Color(0xFF3E2544)
val RfMuted = Color(0xFF8A637D)
val RfSubtle = Color(0xFFB28AA5)
val RfTeal = Color(0xFFFF6FAE)
val RfAmber = Color(0xFFFFC94A)
val RfRed = Color(0xFFE85B75)
val RfBlue = Color(0xFF9B6DFF)
val RfGreen = Color(0xFFB55CFF)

private val RfColorScheme = lightColorScheme(
    primary = RfTeal,
    secondary = RfBlue,
    tertiary = RfAmber,
    background = RfBg,
    surface = RfPanel,
    onPrimary = Color.White,
    onSecondary = RfText,
    onTertiary = RfText,
    onBackground = RfText,
    onSurface = RfText,
    error = RfRed,
    onError = Color.White
)

@Composable
fun RfControlTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RfColorScheme,
        content = content
    )
}
