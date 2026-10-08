package com.parlantecobros.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Paleta Sofisticada Neo-Fintech (Inspirada en Revolut / CashApp / Dribbble)
val LuxuryBackground = Color(0xFFF4F5F8)
val LuxurySurface = Color(0xFFFFFFFF)
val LuxurySurfaceSubtle = Color(0xFFF9FAFB)
val LuxuryBorder = Color(0xFFE5E7EB)
val LuxuryBorderSubtle = Color(0xFFF1F2F5)

// Obsidian Dark para la tarjeta Hero
val ObsidianDark = Color(0xFF10141A)
val ObsidianCard = Color(0xFF181D24)
val ObsidianBorder = Color(0xFF262C36)

// Textos
val TextMain = Color(0xFF111827)
val TextMuted = Color(0xFF6B7280)
val TextLight = Color(0xFF9CA3AF)

// Acentos de Estado y Transacción
val EmeraldGreen = Color(0xFF10B981)
val EmeraldGlow = Color(0x2610B981)
val CoralRed = Color(0xFFEF4444)
val AppleBlue = Color(0xFF0071E3)

// Billeteras Digitales
val YapeBrand = Color(0xFF732282)
val PlinBrand = Color(0xFF00A3E0)
val BcpBrand = Color(0xFF002A8F)
val InterbankBrand = Color(0xFF009B3A)
val BbvaBrand = Color(0xFF004481)

private val LuxuryColorScheme = lightColorScheme(
    primary = ObsidianDark,
    onPrimary = Color.White,
    background = LuxuryBackground,
    onBackground = TextMain,
    surface = LuxurySurface,
    onSurface = TextMain,
    outline = LuxuryBorder
)

@Composable
fun ParlanteCobrosTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LuxuryColorScheme,
        content = content
    )
}
