package com.example.simulation

import com.example.model.EDUCATIONAL_SIMULATION_DISCLAIMER
import kotlin.math.exp
import kotlin.math.max

/**
 * مدل حرارتی و دینامیک تریپ رله اضافه‌بار حرارتی (بی‌متال)
 *
 * استاندارد مرجع: IEC 60947-4-1 (جدول ۲: حد کارکرد رله‌های اضافه‌بار در حالت تعادل فازها - بندهای ۵.۷.۲ و ۸.۲.۱.۵.۱)
 *
 * مشخصات نقاط تریپ استاندارد:
 * - در جریان 1.05 x Ie: بدون تریپ تا حداقل ۲ ساعت (از حالت سرد)
 * - در جریان 1.2 x Ie: تریپ در کمتر از ۲ ساعت (پس از کارکرد در 1.05 x Ie)
 * - در جریان 1.5 x Ie (حالت گرم):
 *    * کلاس 10A: زمان تریپ کمتر از ۱۲۰ ثانیه (۲ دقیقه)
 *    * کلاس 10: زمان تریپ کمتر از ۲۴۰ ثانیه (۴ دقیقه)
 *    * کلاس 20: زمان تریپ کمتر از ۴۸۰ ثانیه (۸ دقیقه)
 *    * کلاس 30: زمان تریپ کمتر از ۷۲۰ ثانیه (۱۲ دقیقه)
 * - در جریان 7.2 x Ie (روتور قفل / راه‌اندازی سنگین از حالت سرد):
 *    * کلاس 10A: بین ۲ تا ۱۰ ثانیه (نقطه مبنای شبیه‌سازی: ۵.۵ ثانیه)
 *    * کلاس 10: بین ۴ تا ۱۰ ثانیه (نقطه مبنای شبیه‌سازی: ۷.۰ ثانیه)
 *    * کلاس 20: بین ۶ تا ۲۰ ثانیه (نقطه مبنای شبیه‌سازی: ۱۳.۵ ثانیه)
 *    * کلاس 30: بین ۹ تا ۳۰ ثانیه (نقطه مبنای شبیه‌سازی: ۲۱.۰ ثانیه)
 *
 * برچسب الزامی: "شبیه‌سازی آموزشی؛ جایگزین محاسبات و تست میدانی نیست"
 */
object BimetalThermalModel {

    const val DISCLAIMER: String = EDUCATIONAL_SIMULATION_DISCLAIMER
    const val STANDARD_REFERENCE: String = "IEC 60947-4-1 Table 2 (Trip limits of overload relays)"

    /**
     * محاسبه زمان تریپ تئوری بر حسب ثانیه بر مبنای جدول ۲ استاندارد IEC 60947-4-1
     * @param overloadRatio نسبت جریان عبوری به جریان تنظیمی I / Ie
     * @param tripClass کلاس تریپ (10A, 10, 20, 30)
     * @return زمان تریپ استاندارد (ثانیه)، یا null اگر تریپ رخ ندهد (I <= 1.05 x Ie)
     */
    fun calculateIecTripTime(overloadRatio: Float, tripClass: String): Float? {
        // مقادیر غیرمتناهی می‌توانند به زمان تریپ مصنوعی و گمراه‌کننده منجر شوند.
        if (!overloadRatio.isFinite()) return null
        if (overloadRatio <= 1.05f) {
            // طبق بند جدول ۲ استاندارد: در 1.05 x Ie به مدت حداقل ۲ ساعت تریپ رخ نمی‌دهد
            return null
        }

        // ضرایب کلاس تریپ در نقطه 7.2 x Ie طبق استاندارد IEC 60947-4-1 جدول 2
        val (t72Nominal, t15Nominal) = when (tripClass.trim().uppercase()) {
            "10A" -> Pair(5.5f, 100f)   // 2s < Tp <= 10s در 7.2 | < 120s در 1.5
            "10" -> Pair(7.0f, 180f)    // 4s < Tp <= 10s در 7.2 | < 240s در 1.5
            "20" -> Pair(13.5f, 360f)   // 6s < Tp <= 20s در 7.2 | < 480s در 1.5
            "30" -> Pair(21.0f, 540f)   // 9s < Tp <= 30s در 7.2 | < 720s در 1.5
            else -> Pair(7.0f, 180f)    // پیش‌فرض کلاس 10 صنعتی
        }

        return when {
            overloadRatio >= 7.2f -> {
                // نقطه روتور قفل استاندارد
                val scale = (7.2f / overloadRatio)
                max(1.5f, t72Nominal * scale * scale)
            }
            overloadRatio >= 1.5f -> {
                // درون‌یابی لگاریتمی پیوسته بین نقطه 1.5 و 7.2 طبق منحنی مشخصه I^2*t
                val ratioFactor = (7.2f - overloadRatio) / (7.2f - 1.5f)
                t72Nominal + ratioFactor * (t15Nominal - t72Nominal)
            }
            overloadRatio > 1.05f -> {
                // در بازه ۱.۰۵ تا ۱.۵ (در 1.2 برابر باید کمتر از ۲ ساعت باشد)
                // نقطه ۱.۲ حدود ۶۰۰ تا ۱۲۰۰ ثانیه (کمتر از ۷۲۰۰ ثانیه طبق استاندارد)
                val t12Nominal = t15Nominal * 3.5f // مثلاً برای کلاس 10 حدود ۶۳۰ ثانیه (< 7200s)
                val fraction = (1.5f - overloadRatio) / (1.5f - 1.05f)
                t15Nominal + fraction * t12Nominal * 2.5f
            }
            else -> null
        }
    }

    /**
     * پیشروی گام به گام مدل حرارتی I^2*t با حافظه گرمایی (Heating / Cooling)
     * @param currentThermalState وضعیت حرارتی فعلی بین 0.0 (کاملاً سرد) تا 1.0 (آستانه تریپ)
     * @param overloadRatio نسبت جریان عبوری به جریان تنظیمی I / Ie
     * @param tripClass کلاس تریپ (10A, 10, 20, 30)
     * @param deltaSeconds زمان سپری شده در این تیک شبیه‌سازی (ثانیه)
     * @param timeAccelerationFactor ضریب شتاب زمان آموزشی جهت آزمایش سریع در رابط کاربری (پیش‌فرض ۱.۰x)
     * @return جفت (وضعیت حرارتی جدید بین 0 تا 1، آیا تریپ رخ داد؟)
     */
    fun stepThermalModel(
        currentThermalState: Float,
        overloadRatio: Float,
        tripClass: String,
        deltaSeconds: Float,
        timeAccelerationFactor: Float = 1.0f
    ): Pair<Float, Boolean> {
        // وضعیت بازیابی‌شده یا ورودی UI ممکن است خراب باشد؛ وضعیت حرارتی را به بازه معتبر محدود می‌کنیم.
        val safeThermalState = currentThermalState.takeIf { it.isFinite() }?.coerceIn(0.0f, 1.0f) ?: 0.0f
        // زمان منفی/غیرمتناهی یا ضریب شتاب نامعتبر نباید مدل را به NaN/Infinity ببرد.
        val effectiveDelta = if (deltaSeconds.isFinite() && deltaSeconds >= 0f &&
            timeAccelerationFactor.isFinite() && timeAccelerationFactor >= 0f) {
            (deltaSeconds * timeAccelerationFactor).takeIf { it.isFinite() } ?: 0f
        } else {
            0f
        }

        // نسبت جریان نامعتبر قابل تفسیر فیزیکی نیست؛ بدون پیشروی حرارتی پاسخ امن برگردان.
        if (!overloadRatio.isFinite()) return Pair(safeThermalState, false)

        if (overloadRatio > 1.05f) {
            // فرآیند گرمایش (Heating): نرخ تجمع حرارت متناسب با I^2 و زمان مشخصه تریپ
            val tripTime = calculateIecTripTime(overloadRatio, tripClass) ?: 3600f
            val heatRate = (1.0f / tripTime) * effectiveDelta
            val nextState = (safeThermalState + heatRate).coerceIn(0.0f, 1.0f)
            val didTrip = nextState >= 1.0f
            return Pair(nextState, didTrip)
        } else {
            // فرآیند سرمایش با حافظه گرمایی (Cooling exponential decay)
            // ثابت زمانی خنک شدن طبیعی بی‌متال استاندارد معمولاً چند دقیقه است
            val coolingTau = 60.0f // ثابت زمانی خنک‌سازی آموزشی (۶۰ ثانیه)
            val decay = exp(-effectiveDelta / coolingTau).toFloat()
            val nextState = (safeThermalState * decay).coerceIn(0.0f, 1.0f)
            return Pair(nextState, false)
        }
    }
}
