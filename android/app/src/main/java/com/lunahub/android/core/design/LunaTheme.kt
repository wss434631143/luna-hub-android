package com.lunahub.android.core.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object LunaColors {
    val LightBackground = Color(0xFFF5F5F7)
    val LightSurface = Color(0xFFFFFFFF)
    val LightSurfaceVariant = Color(0xFFF0F1F5)
    val LightTextPrimary = Color(0xFF1D1D1F)
    val LightTextSecondary = Color(0xFF6E6E73)
    val LightPrimary = Color(0xFF1677FF)
    val LightError = Color(0xFFE5484D)

    val DarkBackground = Color(0xFF0F1014)
    val DarkSurface = Color(0xFF1C1D22)
    val DarkSurfaceVariant = Color(0xFF27282F)
    val DarkTextPrimary = Color(0xFFF4F4F5)
    val DarkTextSecondary = Color(0xFFB8B8BE)
    val DarkPrimary = Color(0xFF4C9DFF)
    val DarkError = Color(0xFFFF6B70)
}

object LunaSpacing {
    val PageHorizontal = 20.dp
    val PageTop = 24.dp
    val CardPadding = 18.dp
    val CardRadius = 22.dp
    val SmallRadius = 16.dp
    val SectionGap = 16.dp
    val ItemGap = 12.dp
    val ButtonHeight = 48.dp
}

private val LightScheme = lightColorScheme(
    primary = LunaColors.LightPrimary,
    background = LunaColors.LightBackground,
    surface = LunaColors.LightSurface,
    surfaceVariant = LunaColors.LightSurfaceVariant,
    onPrimary = Color.White,
    onBackground = LunaColors.LightTextPrimary,
    onSurface = LunaColors.LightTextPrimary,
    onSurfaceVariant = LunaColors.LightTextSecondary,
    error = LunaColors.LightError,
)

private val DarkScheme = darkColorScheme(
    primary = LunaColors.DarkPrimary,
    background = LunaColors.DarkBackground,
    surface = LunaColors.DarkSurface,
    surfaceVariant = LunaColors.DarkSurfaceVariant,
    onPrimary = Color.White,
    onBackground = LunaColors.DarkTextPrimary,
    onSurface = LunaColors.DarkTextPrimary,
    onSurfaceVariant = LunaColors.DarkTextSecondary,
    error = LunaColors.DarkError,
)

private val LunaTypography = Typography(
    headlineLarge = Typography().headlineLarge.copy(fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.SemiBold),
    headlineMedium = Typography().headlineMedium.copy(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = Typography().titleMedium.copy(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = Typography().bodyLarge.copy(fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = Typography().bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = Typography().labelLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
)

@Composable
fun LunaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = LunaTypography,
        shapes = Shapes(
            small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            medium = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
            large = androidx.compose.foundation.shape.RoundedCornerShape(LunaSpacing.CardRadius),
        ),
        content = content,
    )
}
