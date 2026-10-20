package com.example.calc

/**
 * ابزار ۴: هماهنگی فیوز و کلید مینیاتوری (MCB)
 * مطابق با استاندارد IEC 60364-4-43 (حفاظت در برابر اضافه‌بار)
 */
object ProtectionCoordinationCalculator {

    const val STANDARD_REF = "استاندارد بین‌المللی IEC 60364-4-43 بند ۴۳۳-۱ (Protection against overload) و مبحث ۱۳ مقررات ملی ساختمان"

    enum class ProtectiveDeviceType(
        val persianTitle: String,
        val standardDeviceRef: String,
        val conventionalTripMultiplier: Double
    ) {
        MCB_TYPE_B("کلید مینیاتوری تیپ B (مصارف روشنایی بدون جریان هجومی - ۳ تا ۵ برابر In)", "IEC 60898-1", 1.45),
        MCB_TYPE_C("کلید مینیاتوری تیپ C (موتورهای معمولی و بارهای القایی - ۵ تا ۱۰ برابر In)", "IEC 60898-1", 1.45),
        MCB_TYPE_D("کلید مینیاتوری تیپ D (راه‌اندازی موتورهای سنگین و ترانس‌ها - ۱۰ تا ۲۰ برابر In)", "IEC 60898-1", 1.45),
        FUSE_GG("فیوز فشنگی/چاقویی نوع gG (کاربرد عمومی و کابل‌ها)", "IEC 60269-1", 1.60),
        FUSE_AM("فیوز حفاظت موتوری نوع aM (صرفاً برای اتصال کوتاه همراه با رله حرارتی)", "IEC 60269-2", 1.45)
    }

    /**
     * رده‌های مرسوم استاندارد جریان نامی کلیدها و فیوزها
     */
    val STANDARD_PROTECTIVE_RATINGS = listOf(
        6.0, 10.0, 16.0, 20.0, 25.0, 32.0, 40.0, 50.0, 63.0, 80.0, 100.0, 125.0, 160.0, 200.0, 250.0
    )

    data class ProtectionInput(
        val designCurrentIbAmp: Double, // جریان بار طراحی (Ib)
        val selectedRatingInAmp: Double, // جریان نامی کلید/فیوز انتخابی کاربر (In)
        val cableCapacityIzAmp: Double, // جریان مجاز کابل در شرایط نصب (Iz)
        val deviceType: ProtectiveDeviceType = ProtectiveDeviceType.MCB_TYPE_C
    )

    data class ConditionEvaluation(
        val name: String,
        val formula: String,
        val leftValue: Double,
        val rightValue: Double,
        val isPassed: Boolean,
        val messagePersian: String
    )

    data class ProtectionResult(
        val isOverallCompliant: Boolean,
        val condition1: ConditionEvaluation, // Ib <= In <= Iz
        val condition2: ConditionEvaluation, // I2 <= 1.45 * Iz
        val conventionalTripCurrentI2Amp: Double,
        val limitForCondition2Amp: Double,
        val recommendedRatings: List<Double>,
        val deviceType: ProtectiveDeviceType,
        val notes: List<String>,
        val standardRef: String = STANDARD_REF
    )

    fun evaluateCoordination(input: ProtectionInput): Result<ProtectionResult> {
        if (input.designCurrentIbAmp <= 0.0) {
            return Result.failure(IllegalArgumentException("جریان بار طراحی (Ib) باید بزرگتر از صفر باشد."))
        }
        if (input.selectedRatingInAmp <= 0.0) {
            return Result.failure(IllegalArgumentException("جریان نامی حفاظتی (In) باید بزرگتر از صفر باشد."))
        }
        if (input.cableCapacityIzAmp <= 0.0) {
            return Result.failure(IllegalArgumentException("جریان مجاز کابل (Iz) باید بزرگتر از صفر باشد."))
        }

        val ib = input.designCurrentIbAmp
        val inVal = input.selectedRatingInAmp
        val iz = input.cableCapacityIzAmp

        // شرط ۱: Ib <= In <= Iz
        val cond1LeftPassed = ib <= inVal
        val cond1RightPassed = inVal <= iz
        val cond1Passed = cond1LeftPassed && cond1RightPassed

        val cond1Msg = when {
            !cond1LeftPassed -> "عدم تطابق شرط ۱: جریان نامی وسیله حفاظتی (In = $inVal A) کوچکتر از جریان بار (Ib = $ib A) است؛ باعث قطع بی‌مورد کلید می‌شود."
            !cond1RightPassed -> "عدم تطابق شرط ۱: جریان نامی وسیله حفاظتی (In = $inVal A) بزرگتر از ظرفیت کابل (Iz = $iz A) است؛ کابل در برابر اضافه‌بار محافظت نمی‌شود و خطر حریق دارد!"
            else -> "شرط ۱ کاملاً برقرار است: Ib ($ib A) ≤ In ($inVal A) ≤ Iz ($iz A)."
        }

        val condition1 = ConditionEvaluation(
            name = "شرط اول هماهنگی (Ib ≤ In ≤ Iz)",
            formula = "Ib ≤ In ≤ Iz",
            leftValue = inVal,
            rightValue = iz,
            isPassed = cond1Passed,
            messagePersian = cond1Msg
        )

        // شرط ۲: I2 <= 1.45 * Iz
        // I2 جریان عملکرد تضمین شده در زمان معهود است
        val i2Multiplier = if (input.deviceType == ProtectiveDeviceType.FUSE_GG && inVal <= 16.0) 1.90 else input.deviceType.conventionalTripMultiplier
        val i2 = inVal * i2Multiplier
        val maxAllowableI2 = 1.45 * iz
        val cond2Passed = i2 <= maxAllowableI2

        val cond2Msg = if (cond2Passed) {
            "شرط دوم کاملاً برقرار است: I2 ($i2 A) ≤ 1.45 · Iz (${String.format("%.1f", maxAllowableI2)} A). کابل پیش از رسیدن به دمای بحرانی قطع خواهد شد."
        } else {
            "عدم تطابق شرط ۲: جریان قطع تضمین‌شده I2 ($i2 A) بیشتر از سقف مجاز ۱.۴۵ برابری کابل (${String.format("%.1f", maxAllowableI2)} A) است. احتمال ذوب عایق کابل در اضافه بار طولانی وجود دارد."
        }

        val condition2 = ConditionEvaluation(
            name = "شرط دوم هماهنگی اضافه بار کابل (I2 ≤ 1.45 · Iz)",
            formula = "I2 ≤ 1.45 · Iz",
            leftValue = i2,
            rightValue = maxAllowableI2,
            isPassed = cond2Passed,
            messagePersian = cond2Msg
        )

        val overallCompliant = cond1Passed && cond2Passed

        // رنج‌های استاندارد مناسب بین Ib و Iz که شرط ۲ را نیز ارضا کنند
        val recommended = STANDARD_PROTECTIVE_RATINGS.filter { candidateIn ->
            val candI2 = candidateIn * (if (input.deviceType == ProtectiveDeviceType.FUSE_GG && candidateIn <= 16.0) 1.90 else input.deviceType.conventionalTripMultiplier)
            candidateIn >= ib && candidateIn <= iz && candI2 <= (1.45 * iz)
        }

        val notes = listOf(
            "نوع قطعه حفاظتی: ${input.deviceType.persianTitle}",
            "ضریب جریان قطع معهود (I2): $i2Multiplier برابر جریان نامی",
            "معیار راه‌اندازی موتور: انتخاب نوع منحنی (C یا D) بدون حدس ضریب انجام شده و بر عهده طراح است."
        )

        return Result.success(
            ProtectionResult(
                isOverallCompliant = overallCompliant,
                condition1 = condition1,
                condition2 = condition2,
                conventionalTripCurrentI2Amp = i2,
                limitForCondition2Amp = maxAllowableI2,
                recommendedRatings = recommended,
                deviceType = input.deviceType,
                notes = notes
            )
        )
    }
}
