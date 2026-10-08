package com.parlantecobros.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val AppleBackground = Color(0xFFF5F5F7)
val AppleCard = Color(0xFFFFFFFF)
val AppleTextPrimary = Color(0xFF1D1D1F)
val AppleTextSecondary = Color(0xFF86868B)
val AppleTextTertiary = Color(0xFFA1A1A6)
val AppleBorder = Color(0xFFE5E5EA)
val AppleBlue = Color(0xFF0071E3)
val AppleGreen = Color(0xFF34C759)
val AppleRed = Color(0xFFFF3B30)
val AppleOrange = Color(0xFFFF9500)

val YapeColor = Color(0xFF732282)
val PlinColor = Color(0xFF00A3E0)
val BcpColor = Color(0xFF002A8F)
val InterbankColor = Color(0xFF009B3A)

private val AppleColorScheme = lightColorScheme(
    primary = AppleBlue,
    onPrimary = Color.White,
    background = AppleBackground,
    onBackground = AppleTextPrimary,
    surface = AppleCard,
    onSurface = AppleTextPrimary,
    surfaceVariant = Color(0xFFF2F2F7),
    onSurfaceVariant = AppleTextSecondary,
    outline = AppleBorder
)

@Composable
fun ParlanteCobrosTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AppleColorScheme,
        content = content
    )
}
