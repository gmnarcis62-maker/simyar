package com.example.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * فواصل، ابعاد و انحناهای استاندارد در سیستم طراحی سیمیار (Design Tokens)
 * مبتنی بر شبکه فاصله‌گذاری ۴dp
 */
object SimYarSpacing {
    val none: Dp = 0.dp
    val xxs: Dp = 2.dp
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 20.dp
    val xxl: Dp = 24.dp
    val xxxl: Dp = 32.dp
    val huge: Dp = 48.dp
}

object SimYarRadii {
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp     // چیپ‌ها و ورودی‌های فشرده
    val md: Dp = 12.dp    // کارت‌های کوچک و دکمه‌ها
    val lg: Dp = 16.dp    // کارت‌های استاندارد
    val xl: Dp = 24.dp    // باتم شیت‌ها و دیالوگ‌ها
    val pill: Dp = 999.dp // وضعیت‌ها و نشان‌ها
}

object SimYarElevation {
    val none: Dp = 0.dp
    val subtle: Dp = 2.dp
    val medium: Dp = 6.dp
    val high: Dp = 12.dp
}
