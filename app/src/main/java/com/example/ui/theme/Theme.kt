package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * تم جامع Material 3 برای اپلیکیشن مهندسی سیمیار
 * تضمین کنتراست WCAG AA در هر دو حالت تاریک و روشن
 */
private val SimYarDarkColorScheme = darkColorScheme(
    primary = SimYarPrimary,
    onPrimary = Color(0xFF002330),
    primaryContainer = SimYarPrimaryMuted,
    onPrimaryContainer = SimYarPrimary,
    inversePrimary = SimYarPrimaryDark,

    secondary = SimYarSuccess,
    onSecondary = Color(0xFF002213),
    secondaryContainer = SimYarSuccessContainer,
    onSecondaryContainer = Color(0xFF6EE7B7),

    tertiary = SimYarWarning,
    onTertiary = Color(0xFF3B1A00),
    tertiaryContainer = SimYarWarningContainer,
    onTertiaryContainer = Color(0xFFFDE68A),

    background = SimYarDarkBase,
    onBackground = SimYarTextPrimaryDark,

    surface = SimYarDarkSurface,
    onSurface = SimYarTextPrimaryDark,
    surfaceVariant = SimYarDarkSurfaceElevated,
    onSurfaceVariant = SimYarTextSecondaryDark,
    surfaceTint = SimYarPrimary,

    outline = SimYarDarkBorderSubtle,
    outlineVariant = SimYarDarkBorderStrong,

    error = SimYarDanger,
    onError = Color.White,
    errorContainer = SimYarDangerContainer,
    onErrorContainer = Color(0xFFFECACA)
)

private val SimYarLightColorScheme = lightColorScheme(
    primary = SimYarPrimaryDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    inversePrimary = SimYarPrimary,

    secondary = Color(0xFF059669),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF065F46),

    tertiary = Color(0xFFB7791F),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF92400E),

    background = SimYarLightBase,
    onBackground = SimYarTextPrimaryLight,

    surface = SimYarLightSurface,
    onSurface = SimYarTextPrimaryLight,
    surfaceVariant = SimYarLightSurfaceElevated,
    onSurfaceVariant = SimYarTextSecondaryLight,
    surfaceTint = SimYarPrimaryDark,

    outline = SimYarLightBorderSubtle,
    outlineVariant = SimYarLightBorderStrong,

    error = SimYarDanger,
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // تم پیش‌فرض تیره مهندسی (Dark Mode First)
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) SimYarDarkColorScheme else SimYarLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}