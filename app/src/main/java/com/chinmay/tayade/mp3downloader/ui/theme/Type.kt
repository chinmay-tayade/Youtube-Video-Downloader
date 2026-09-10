package com.chinmay.tayade.mp3downloader.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.chinmay.tayade.mp3downloader.R

val Jakarta = FontFamily(
    Font(R.font.plusjakarta_medium, FontWeight.Normal),
    Font(R.font.plusjakarta_medium, FontWeight.Medium),
    Font(R.font.plussansjakarta_semibold, FontWeight.SemiBold),
    Font(R.font.plusjakarta_sans_bold, FontWeight.Bold),
)

private val base = Typography()

val AppTypography = Typography(
    displaySmall = base.displaySmall.copy(fontFamily = Jakarta),
    headlineLarge = base.headlineLarge.copy(fontFamily = Jakarta),
    headlineMedium = base.headlineMedium.copy(fontFamily = Jakarta),
    headlineSmall = base.headlineSmall.copy(fontFamily = Jakarta, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Bold, fontSize = 22.sp),
    titleMedium = base.titleMedium.copy(fontFamily = Jakarta, fontWeight = FontWeight.SemiBold),
    titleSmall = base.titleSmall.copy(fontFamily = Jakarta, fontWeight = FontWeight.SemiBold),
    bodyLarge = base.bodyLarge.copy(fontFamily = Jakarta),
    bodyMedium = base.bodyMedium.copy(fontFamily = Jakarta),
    bodySmall = base.bodySmall.copy(fontFamily = Jakarta),
    labelLarge = base.labelLarge.copy(fontFamily = Jakarta, fontWeight = FontWeight.Bold),
    labelMedium = base.labelMedium.copy(fontFamily = Jakarta),
    labelSmall = base.labelSmall.copy(fontFamily = Jakarta),
)
