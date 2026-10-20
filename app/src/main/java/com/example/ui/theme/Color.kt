package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * پالت رنگی لوکس مهندسی سیمیار (SimYar Design System)
 * الهام‌گرفته از ابزارهای مهندسی دقیق (Figma, Linear, Notion, JetBrains)
 * سطوح با کنتراست عمیق، مرزهای ظریف، و رنگ‌های معنایی مشخص.
 */

// --- سطوح تیره (Dark Palette - Primary Mode) ---
val SimYarDarkBase = Color(0xFF061225)           // عمیق‌ترین پس‌زمینه (Background)
val SimYarDarkSurface = Color(0xFF0B1B32)        // لایه رویی اول (Surface Level 1)
val SimYarDarkSurfaceElevated = Color(0xFF122642) // لایه کارت‌ها و شیت‌ها (Level 2)
val SimYarDarkSurfaceHover = Color(0xFF1B3555)    // حالت هاور یا برجسته (Level 3)
val SimYarDarkBorderSubtle = Color(0xFF254365)    // خط مرز خیلی ظریف (1dp)
val SimYarDarkBorderStrong = Color(0xFF35618B)    // مرز متمرکز یا هایلایت

// --- سطوح روشن مهندسی (Light Palette - Clean Technical) ---
val SimYarLightBase = Color(0xFFF3F8FF)          // پس‌زمینه روشن خاکستری-سفید
val SimYarLightSurface = Color(0xFFFFFFFF)       // سطح کارت‌ها و المان‌ها
val SimYarLightSurfaceElevated = Color(0xFFEAF2FC)
val SimYarLightSurfaceHover = Color(0xFFDCE8F7)
val SimYarLightBorderSubtle = Color(0xFFDCE8F7)
val SimYarLightBorderStrong = Color(0xFFB9CDE6)

// --- رنگ اصلی و متمرکز (Electric Cyan / Technical Blue) ---
val SimYarPrimary = Color(0xFF00C8F0)            // رنگ امضای سیمیار (Cyan المهندسی)
val SimYarPrimaryDark = Color(0xFF0878F9)        // رنگ اصلی در تم روشن
val SimYarPrimaryMuted = Color(0xFF123A60)       // پس‌زمینه کانتینر برای دکمه‌های ملایم
val SimYarPrimaryGlow = Color(0x3300C8F0)        // هاله جریان برقدار

// --- رنگ‌های معنایی (Semantic Tokens) ---
val SimYarSuccess = Color(0xFF00D49A)            // سبز سالم / در حال کار
val SimYarSuccessContainer = Color(0xFF064E3B)
val SimYarWarning = Color(0xFFE6A82E)            // کهربایی / احتیاط و اینترلاک
val SimYarWarningContainer = Color(0xFF78350F)
val SimYarDanger = Color(0xFFEF4444)             // قرمز خطا / اتصال کوتاه
val SimYarDangerContainer = Color(0xFF7F1D1D)
val SimYarGoldVip = Color(0xFFFFC94A)            // طلایی پریمیوم VIP
val SimYarGoldVipSecondary = Color(0xFFE6A82E)

// --- متن و آیکون (Typography & Icons) ---
val SimYarTextPrimaryDark = Color(0xFFEAF2FC)    // متن اصلی تم تیره
val SimYarTextSecondaryDark = Color(0xFF94A3B8)  // متن ثانویه تم تیره
val SimYarTextMutedDark = Color(0xFF64748B)      // توضیحات و راهنما
val SimYarTextPrimaryLight = Color(0xFF0F172A)   // متن اصلی تم روشن
val SimYarTextSecondaryLight = Color(0xFF475569) // متن ثانویه تم روشن
val SimYarTextMutedLight = Color(0xFF94A3B8)

// =========================================================================
// نام‌های قدیمی جهت حفظ سازگاری ۱۰۰٪ با کدهای موجود (Backward Compatibility Aliases)
// =========================================================================
val SimYarDarkBackground = SimYarDarkBase
// SimYarDarkSurface در بالا تعریف شده
val SimYarDarkCard = SimYarDarkSurfaceElevated
val SimYarDarkBorder = SimYarDarkBorderSubtle

val SimYarElectricCyan = SimYarPrimary
val SimYarNeonGreen = SimYarSuccess
val SimYarIndustrialAmber = SimYarWarning
val SimYarCriticalRed = SimYarDanger

val SimYarTextPrimary = SimYarTextPrimaryDark
val SimYarTextSecondary = SimYarTextSecondaryDark
val SimYarTextMuted = SimYarTextMutedDark

val SimYarLightBackground = SimYarLightBase
// SimYarLightSurface در بالا تعریف شده
val SimYarLightCard = SimYarLightSurfaceElevated
val SimYarLightBorder = SimYarLightBorderSubtle
// SimYarLightPrimary در بالا تعریف شده
