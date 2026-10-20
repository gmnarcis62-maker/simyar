package com.example.canvas

import kotlin.math.max
import kotlin.math.min

/**
 * نتیجه محاسبه انطباق مدار روی بوم (Fit-to-Screen)
 */
data class FitTransformResult(
    val scale: Float,
    val panX: Float,
    val panY: Float
)

/**
 * کادر حد اشغال‌شده مدار (Bounding Box)
 */
data class CircuitBoundingBox(
    val minX: Float,
    val maxX: Float,
    val minY: Float,
    val maxY: Float
)

object CanvasFitCalculator {

    const val MIN_SCALE = 0.35f
    const val MAX_SCALE = 2.8f
    const val DEFAULT_PADDING = 48f

    /**
     * محاسبه مقیاس و جابجایی مناسب برای وسط‌چین کردن کامل مدار درون صفحه نمایش
     *
     * @param bbox کادر حد فیزیکی مدار
     * @param viewportWidth عرض صفحه یا بوم بر حسب پیکسل
     * @param viewportHeight ارتفاع صفحه یا بوم بر حسب پیکسل
     * @param density ضریب تراکم صفحه نمایش (نمایشگرهای امروزی معمولاً بین ۲.۰ تا ۳.۵)
     * @param padding حاشیه امن پیرامون مدار بر حسب dp
     */
    fun calculateFit(
        bbox: CircuitBoundingBox?,
        viewportWidth: Float,
        viewportHeight: Float,
        density: Float = 1f,
        padding: Float = DEFAULT_PADDING
    ): FitTransformResult {
        val safeDensity = if (density > 0.1f) density else 1f
        if (bbox == null || viewportWidth <= 0f || viewportHeight <= 0f) {
            // در صورت نبود قطعه، مقیاس پیش‌فرض برابر با چگالی صفحه تنظیم می‌شود
            return FitTransformResult(scale = safeDensity, panX = 0f, panY = 0f)
        }

        val paddingPx = padding * safeDensity
        val contentWidthPx = ((bbox.maxX - bbox.minX) * safeDensity + (paddingPx * 2)).coerceAtLeast(80f * safeDensity)
        val contentHeightPx = ((bbox.maxY - bbox.minY) * safeDensity + (paddingPx * 2)).coerceAtLeast(80f * safeDensity)

        val scaleX = viewportWidth / contentWidthPx
        val scaleY = viewportHeight / contentHeightPx
        val rawScale = min(scaleX, scaleY)

        // مقیاس نهایی به صورت ضرب در چگالی برای هماهنگی با سیستم رسم
        val minAllowed = MIN_SCALE * safeDensity
        val maxAllowed = MAX_SCALE * safeDensity
        val fittedScale = (rawScale * safeDensity).coerceIn(minAllowed, maxAllowed)

        val centerX = (bbox.minX + bbox.maxX) / 2f
        val centerY = (bbox.minY + bbox.maxY) / 2f

        val panX = (viewportWidth / 2f) - (centerX * fittedScale)
        val panY = (viewportHeight / 2f) - (centerY * fittedScale)

        return FitTransformResult(
            scale = fittedScale,
            panX = panX,
            panY = panY
        )
    }

    /**
     * محاسبه شعاع لمسی ترمینال بر حسب واحدهای بوم با جبران ضریب زوم و تراکم پیکسلی
     * تضمین حداقل اندازه لمسی استاندارد (حدود ۲۲dp) در زوم‌های مختلف و جلوگیری از پوشاندن بدنه در زوم بالا
     *
     * @param scale ضریب مقیاس فعلی بوم
     * @param density تراکم پیکسلی صفحه نمایش
     * @param targetDpRadius شعاع هدف بر حسب dp (پیش‌فرض ۲۲dp برای مساحت لمسی ۴۴dp)
     * @return شعاع بر حسب واحدهای مختصات بوم (clamp بین 14 تا 40)
     */
    fun calculateTerminalTouchRadius(
        scale: Float,
        density: Float = 1f,
        targetDpRadius: Float = 22f
    ): Float {
        val safeDensity = if (density > 0.1f) density else 1f
        val safeScale = if (scale > 0.05f) scale else 1f

        // شعاع به پیکسل تقسیم بر مقیاس بوم برای به دست آوردن واحد بوم
        val targetPixelRadius = targetDpRadius * safeDensity
        val radiusInCanvasUnits = targetPixelRadius / safeScale

        return radiusInCanvasUnits.coerceIn(14f, 40f)
    }
}

