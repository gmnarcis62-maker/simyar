package com.example.calc

import kotlin.math.sqrt

/**
 * ابزار ۳: تنظیم رله اضافه‌بار حرارتی (بی‌متال / رله حرارتی الکترونیکی)
 * مطابق با استاندارد IEC 60947-4-1
 */
object BimetalCalculator {

    const val STANDARD_REF = "استاندارد IEC 60947-4-1 بند ۵-۷ (Thermal overload relays) و استاندارد نقشه‌کشی سیمیار"

    enum class RelayPlacement(
        val persianTitle: String,
        val formulaDescription: String
    ) {
        DOL_LINE(
            persianTitle = "راه‌اندازی مستقیم (DOL) - قرارگیری در مسیر خط اصلی",
            formulaDescription = "I_set = In"
        ),
        STAR_DELTA_IN_PHASE_WINDING(
            persianTitle = "ستاره-مثلث - قرارگیری در مسیر سیم‌پیچ فاز (زیر KM1 یا KM2)",
            formulaDescription = "I_set = In / √3 ≈ 0.58 · In (قالب استاندارد سیمیار)"
        ),
        STAR_DELTA_IN_MAIN_LINE(
            persianTitle = "ستاره-مثلث - قرارگیری در خط ورودی مشترک (قبل از تقسیم KM1)",
            formulaDescription = "I_set = In"
        )
    }

    data class BimetalInput(
        val motorNominalCurrentAmp: Double,
        val placement: RelayPlacement = RelayPlacement.STAR_DELTA_IN_PHASE_WINDING,
        val serviceFactor: Double = 1.0 // ضریب سرویس پلاک موتور (معمولاً ۱.۰ یا ۱.۱۵)
    )

    data class BimetalResult(
        val motorNominalCurrent: Double,
        val placement: RelayPlacement,
        val settingCurrentAmp: Double,
        val recommendedRangeMinAmp: Double,
        val recommendedRangeMaxAmp: Double,
        val formulaUsed: String,
        val technicalExplanation: String,
        val wiringDiagramNote: String,
        val standardRef: String = STANDARD_REF
    )

    fun calculateBimetalSetting(input: BimetalInput): Result<BimetalResult> {
        if (input.motorNominalCurrentAmp <= 0.0) {
            return Result.failure(IllegalArgumentException("جریان نامی موتور باید عددی مثبت باشد."))
        }
        if (input.serviceFactor <= 0.0) {
            return Result.failure(IllegalArgumentException("ضریب سرویس باید عددی مثبت باشد."))
        }

        val baseCurrent = input.motorNominalCurrentAmp * input.serviceFactor

        val (settingCurrent, formula, explanation, diagramNote) = when (input.placement) {
            RelayPlacement.DOL_LINE -> {
                val setI = baseCurrent
                Tuple4(
                    setI,
                    "I_set = In · SF = ${String.format("%.2f", setI)} A",
                    "در راه‌اندازی مستقیم (DOL)، تمام جریان مصرفی موتور از بی‌متال عبور می‌کند. بنابراین جریان تنظیمی مستقیماً برابر با جریان نامی پلاک موتور (ضربدر ضریب سرویس) تنظیم می‌شود.",
                    "محل نصب: دقیقاً در خروجی کنتاکتور خط اصلی به سمت ترمینال‌های موتور U1, V1, W1."
                )
            }

            RelayPlacement.STAR_DELTA_IN_PHASE_WINDING -> {
                val setI = baseCurrent / sqrt(3.0)
                Tuple4(
                    setI,
                    "I_set = (In / √3) · SF ≈ 0.577 · In · SF = ${String.format("%.2f", setI)} A",
                    "در این آرایش (که قالب پیش‌فرض پروژه و نقشه‌های استاندارد صنعتی سیمیار است)، بی‌متال زیر کنتاکتور اصلی (KM1) قرار می‌گیرد و تنها جریان یک فاز سیم‌پیچ موتور مثلث (I_phase = In / √3) از آن عبور می‌کند. در صورت تنظیم اشتباه روی In، موتور در اضافه‌بار بدون حفاظت مانده و می‌سوزد!",
                    "محل نصب استاندارد سیمیار: زیر تیغه‌های قدرت کنتاکتور اصلی KM1 و در مدار سیم‌پیچ داخلی موتور."
                )
            }

            RelayPlacement.STAR_DELTA_IN_MAIN_LINE -> {
                val setI = baseCurrent
                Tuple4(
                    setI,
                    "I_set = In · SF = ${String.format("%.2f", setI)} A",
                    "در صورتی که بی‌متال در خط ورودی ۳ فاز تغذیه اصلی (قبل از انشعاب کنتاکتور اصلی KM1 و مثلث KM2) قرار داده شود، جریان خط را حس می‌کند و باید روی جریان نامی کامل In تنظیم شود. (نکته: در این حالت در زمان استارت ستاره نیز بی‌متال جریان را می‌سنجد).",
                    "محل نصب: بعد از کلید اصلی یا فیوز و قبل از تغذیه کنتاکتورهای KM1 و KM2."
                )
            }
        }

        // رنج تنظیمی استاندارد بی‌متال معمولاً باید طوری انتخاب شود که جریان تنظیمی در وسط رنج قرار گیرد
        val recommendedMin = settingCurrent * 0.75
        val recommendedMax = settingCurrent * 1.25

        return Result.success(
            BimetalResult(
                motorNominalCurrent = input.motorNominalCurrentAmp,
                placement = input.placement,
                settingCurrentAmp = settingCurrent,
                recommendedRangeMinAmp = recommendedMin,
                recommendedRangeMaxAmp = recommendedMax,
                formulaUsed = formula,
                technicalExplanation = explanation,
                wiringDiagramNote = diagramNote
            )
        )
    }

    private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
}
