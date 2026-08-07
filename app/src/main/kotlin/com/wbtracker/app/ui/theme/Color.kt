package com.wbtracker.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Палитра "Пульс" v3.0 ("Пульс 1-в-1 ЭТАЛОН")
val PulseGradientStart = Color(0xFF7C3AED)
val PulseGradientMiddle = Color(0xFFA855F7)
val PulseGradientEnd = Color(0xFFDB2777)

// Тёмная тема v3.0
val DarkBackground = Color(0xFF0A0A0E)
val DarkSurface = Color(0xFF17171D)
val DarkCardBackground = Color(0xFF1E1E26)
val DarkOnSurface = Color(0xFFF5F5F7)
val DarkSubtitle = Color(0xFF8E8E98)
val DarkBorder = Color(0x12FFFFFF)

// Светлая тема v3.0
val LightBackground = Color(0xFFF5F5F7)
val LightSurface = Color(0xFFFFFFFF)
val LightCardBackground = Color(0xFFF0F0F5)
val LightOnSurface = Color(0xFF1D1D1F)
val LightSubtitle = Color(0xFF86868B)
val LightBorder = Color(0x0F000000)

val PriceDropGreen = Color(0xFF0F9D58)
val PriceRiseRed = Color(0xFFE5484D)
val WbWalletBlue = Color(0xFF00A3FF)

// UI Brush для градиентов
val PulseGradientBrush = Brush.linearGradient(
    colors = listOf(PulseGradientStart, PulseGradientMiddle, PulseGradientEnd)
)

// Обратная совместимость
val WbPurple = PulseGradientStart
val WbPurpleLight = PulseGradientMiddle
val WbPurpleDark = Color(0xFF4A2670)

val PriceDown = PriceDropGreen
val PriceDownContainer = Color(0xFF0D5C35)

val PriceUp = PriceRiseRed
val PriceUpContainer = Color(0xFF8B2018)

val SurfaceDark = DarkBackground
val SurfaceContainerDark = DarkSurface
val SurfaceVariantDark = DarkCardBackground
val OnSurfaceDark = DarkOnSurface

val SurfaceLight = LightBackground
val SurfaceContainerLight = LightSurface
val SurfaceVariantLight = LightCardBackground
val OnSurfaceLight = LightOnSurface

val WbPurpleGradientStart = PulseGradientStart
val WbPurpleGradientEnd = PulseGradientEnd
val PriceDownLight = Color(0xFFE8F5E9)
val PriceUpLight = Color(0xFFFFEBEE)
val CardBackgroundLight = LightCardBackground
val CardBackgroundDark = DarkCardBackground
val ChartLineColor = PulseGradientMiddle
val ChartFillColor = Color(0x337C3AED)
val ReviewColor = Color(0xFFFFB800)
val SkeletonShimmer = Color(0xFF2A2A3A)
