package com.example.calc

import kotlin.math.sqrt

/**
 * ابزار ۲: انتخاب کنتاکتور (رده کاری AC-3)
 * مطابق با استاندارد IEC 60947-4-1
 */
object ContactorCalculator {

    const val STANDARD_REF = "استاندارد IEC 60947-4-1 (Contactors and motor-starters - Electromechanical contactors and motor-starters)"

    enum class StarterType(val persianTitle: String) {
        DOL("راه‌اندازی مستقیم (Direct On-Line DOL)"),
        STAR_DELTA("راه‌اندازی ستاره-مثلث (Star-Delta Y-Δ)"),
        REVERSING("راه‌اندازی چپگرد-راستگرد با اینترلاک (Reversing)"),
        JOGGING_AC4("قطع و وصل درجا و ترمز معکوس (Jogging / Inching - رده AC-4)")
    }

    /**
     * رده‌های استاندارد جریان نامی عملکردی Ie در دسته AC-3 طبق رنج‌های استاندارد بین‌المللی سازندگان معتبر
     */
    val STANDARD_AC3_RATINGS = listOf(
        9.0, 12.0, 18.0, 25.0, 32.0, 38.0, 40.0, 50.0, 65.0, 80.0,
        95.0, 115.0, 150.0, 185.0, 225.0, 265.0, 330.0, 400.0, 500.0, 630.0
    )

    data class ContactorInput(
        val motorNominalCurrentAmp: Double,
        val starterType: StarterType = StarterType.DOL,
        val safetyFactor: Double = 1.0 // ضریب اطمینان (اختیاری توسط کاربر، پیش‌فرض ۱.۰)
    )

    data class ContactorRequirement(
        val contactorRole: String, // مثلاً "کنتاکتور اصلی (KM1)"
        val minRequiredCurrentIeAmp: Double,
        val recommendedStandardSizeAmp: Double?,
        val formulaUsed: String,
        val explanation: String
    )

    data class ContactorResult(
        val motorNominalCurrent: Double,
        val starterType: StarterType,
        val requirements: List<ContactorRequirement>,
        val criteriaDescription: String,
        val standardRef: String = STANDARD_REF
    )

    fun calculateContactor(input: ContactorInput): Result<ContactorResult> {
        if (input.motorNominalCurrentAmp <= 0.0) {
            return Result.failure(IllegalArgumentException("جریان نامی موتور باید عددی مثبت و بزرگتر از صفر باشد."))
        }
        val safeCurrent = input.motorNominalCurrentAmp * input.safetyFactor.coerceAtLeast(1.0)

        val requirements = mutableListOf<ContactorRequirement>()

        when (input.starterType) {
            StarterType.DOL -> {
                val minIe = safeCurrent
                val stdSize = STANDARD_AC3_RATINGS.firstOrNull { it >= minIe }
                requirements.add(
                    ContactorRequirement(
                        contactorRole = "کنتاکتور خط اصلی (KM1)",
                        minRequiredCurrentIeAmp = minIe,
                        recommendedStandardSizeAmp = stdSize,
                        formulaUsed = "Ie(AC-3) ≥ In",
                        explanation = "در راه‌اندازی مستقیم، جریان نامی عملکردی کنتاکتور در رده AC-3 باید حداقل برابر با جریان نامی موتور باشد."
                    )
                )
            }

            StarterType.STAR_DELTA -> {
                // کنتاکتور اصلی KM1 و مثلث KM2: In / √3 ≈ 0.577 * In
                val minIeMainDelta = safeCurrent / sqrt(3.0)
                val stdMainDelta = STANDARD_AC3_RATINGS.firstOrNull { it >= minIeMainDelta }
                requirements.add(
                    ContactorRequirement(
                        contactorRole = "کنتاکتور اصلی (KM1) و کنتاکتور مثلث (KM2)",
                        minRequiredCurrentIeAmp = minIeMainDelta,
                        recommendedStandardSizeAmp = stdMainDelta,
                        formulaUsed = "Ie(AC-3) ≥ In / √3 ≈ 0.58 · In",
                        explanation = "جریان عبوری از هر یک از کنتاکتورهای اصلی و مثلث برابر با جریان فاز موتور است."
                    )
                )

                // کنتاکتور ستاره KM3: In / 3 ≈ 0.33 * In
                val minIeStar = safeCurrent / 3.0
                val stdStar = STANDARD_AC3_RATINGS.firstOrNull { it >= minIeStar }
                requirements.add(
                    ContactorRequirement(
                        contactorRole = "کنتاکتور ستاره (KM3)",
                        minRequiredCurrentIeAmp = minIeStar,
                        recommendedStandardSizeAmp = stdStar,
                        formulaUsed = "Ie(AC-3) ≥ In / 3 ≈ 0.33 · In",
                        explanation = "کنتاکتور ستاره فقط در زمان شروع به کار فعال بوده و جریان آن یک‌سوم جریان نامی موتور است."
                    )
                )
            }

            StarterType.REVERSING -> {
                val minIe = safeCurrent
                val stdSize = STANDARD_AC3_RATINGS.firstOrNull { it >= minIe }
                requirements.add(
                    ContactorRequirement(
                        contactorRole = "کنتاکتورهای راستگرد و چپگرد (KM1 و KM2)",
                        minRequiredCurrentIeAmp = minIe,
                        recommendedStandardSizeAmp = stdSize,
                        formulaUsed = "Ie(AC-3) ≥ In (الزام اینترلاک مکانیکی و الکتریکی)",
                        explanation = "هر دو کنتاکتور باید جریان کامل نامی را تحمل کرده و با اینترلاک مکانیکی و الکتریکی محافظت شوند."
                    )
                )
            }

            StarterType.JOGGING_AC4 -> {
                // رده AC-4 برای شرایط وصل و قطع جریان راه‌اندازی شدید (Jogging/Plugging)
                val minIe = safeCurrent * 1.5 // ضریب بهره‌برداری AC-4
                val stdSize = STANDARD_AC3_RATINGS.firstOrNull { it >= minIe }
                requirements.add(
                    ContactorRequirement(
                        contactorRole = "کنتاکتور رده کاری سنگین AC-4",
                        minRequiredCurrentIeAmp = minIe,
                        recommendedStandardSizeAmp = stdSize,
                        formulaUsed = "Ie(AC-4) ≥ In (معادل حدود ۱.۵ برابر در رنج AC-3)",
                        explanation = "در رده AC-4 به دلیل قطع جریان در اوج راه‌اندازی، سایز کنتاکتور باید بر اساس ظرفیت AC-4 دیتاشیت رسمی انتخاب شود."
                    )
                )
            }
        }

        val criteriaDescription = "معیار پذیرش IEC 60947-4-1: جریان نامی عملکردی کنتاکتور (Ie) در ولتاژ کاری مورد نظر باید از جریان نامی بار در رده کاری مربوطه (AC-3 یا AC-4) بیشتر یا مساوی باشد. مقادیر پیشنهادی بر اساس سایزهای مرسوم استاندارد است."

        return Result.success(
            ContactorResult(
                motorNominalCurrent = input.motorNominalCurrentAmp,
                starterType = input.starterType,
                requirements = requirements,
                criteriaDescription = criteriaDescription
            )
        )
    }
}
