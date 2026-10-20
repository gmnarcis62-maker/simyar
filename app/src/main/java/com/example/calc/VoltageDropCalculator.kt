package com.example.calc

import kotlin.math.sin
import kotlin.math.sqrt

/**
 * ابزار ۵: افت ولتاژ خطوط و کابل‌های قدرت
 * مطابق با استاندارد IEC 60364-5-52 و مبحث ۱۳ مقررات ملی ساختمان
 */
object VoltageDropCalculator {

    const val STANDARD_REF = "استاندارد بین‌المللی IEC 60364-5-52 پیوست G و مقررات ملی ساختمان ایران (مبحث سیزدهم، بند ۱۳-۴-۱-۵)"

    // مقاومت ویژه الکتریکی در دمای ۲۰ درجه سانتی‌گراد (استاندارد IEC 60228 و مبحث ۱۳)
    const val RHO_20_COPPER = 0.0175 // مس (Ohm · mm² / m)
    const val RHO_20_ALUMINUM = 0.0282 // آلومینیوم (Ohm · mm² / m)

    // ضریب حرارتی مقاومت بر حسب 1/°C (استاندارد IEC 60228)
    const val ALPHA_COPPER = 0.00393
    const val ALPHA_ALUMINUM = 0.00403

    // راکتانس القایی پیش‌فرض در فرکانس ۵۰ هرتز (Ohm / km) برای کابل‌های چندرشته‌ای (مرجع: IEC 60364-5-52)
    const val DEFAULT_REACTANCE_PER_KM = 0.08

    enum class ConductorMaterial(
        val persianTitle: String,
        val rho20: Double,
        val alpha: Double,
        val standardRef: String
    ) {
        COPPER("مس (Copper)", RHO_20_COPPER, ALPHA_COPPER, "IEC 60228 و مبحث ۱۳: ρ۲۰ = ۰.۰۱۷۵ Ω·mm²/m و α = ۰.۰۰۳۹۳"),
        ALUMINUM("آلومینیوم (Aluminum)", RHO_20_ALUMINUM, ALPHA_ALUMINUM, "IEC 60228 و مبحث ۱۳: ρ۲۰ = ۰.۰۲۸۲ Ω·mm²/m و α = ۰.۰۰۴۰۳")
    }

    enum class CircuitApplication(
        val persianTitle: String,
        val allowableDropPercent: Double,
        val nationalCodeRef: String
    ) {
        MOTOR_AND_POWER("مدارهای موتوری و مصارف قدرت (سقف ۵٪)", 5.0, "مبحث ۱۳ بند ۱۳-۴-۱-۵ و IEC 60364-5-52 جدول G.52.1"),
        LIGHTING("مدارهای روشنایی (سقف ۳٪)", 3.0, "مبحث ۱۳ بند ۱۳-۴-۱-۵ و IEC 60364-5-52 جدول G.52.1"),
        CUSTOM("حد مجاز دلخواه کاربر", 5.0, "ورودی دلخواه کاربر")
    }

    /**
     * مقاطع استاندارد کابل بر حسب mm²
     */
    val STANDARD_CROSS_SECTIONS = listOf(
        1.5, 2.5, 4.0, 6.0, 10.0, 16.0, 25.0, 35.0, 50.0, 70.0, 95.0, 120.0, 150.0, 185.0, 240.0, 300.0
    )

    data class VoltageDropInput(
        val isThreePhase: Boolean = true,
        val currentAmp: Double,
        val nominalVoltageVolt: Double = if (isThreePhase) 400.0 else 230.0,
        val lengthMeters: Double,
        val crossSectionMm2: Double,
        val material: ConductorMaterial = ConductorMaterial.COPPER,
        val conductorTemperatureCelsius: Double = 70.0, // ۷۰ درجه برای PVC و ۹۰ درجه برای XLPE (مرجع استاندارد)
        val cosPhi: Double = 0.85,
        val reactancePerKm: Double = DEFAULT_REACTANCE_PER_KM,
        val application: CircuitApplication = CircuitApplication.MOTOR_AND_POWER,
        val customAllowablePercent: Double? = null
    )

    data class VoltageDropResult(
        val deltaUVolt: Double,
        val deltaUPercent: Double,
        val allowablePercent: Double,
        val isCompliant: Boolean,
        val resistanceAtTempOhm: Double,
        val reactanceTotalOhm: Double,
        val formulaUsed: String,
        val recommendedMinSectionMm2: Double?,
        val technicalAssumptions: List<String>,
        val standardRef: String = STANDARD_REF
    )

    fun calculateVoltageDrop(input: VoltageDropInput): Result<VoltageDropResult> {
        if (!input.currentAmp.isFinite() || input.currentAmp <= 0.0) {
            return Result.failure(IllegalArgumentException("جریان خط باید عددی محدود و بزرگتر از صفر باشد."))
        }
        if (!input.nominalVoltageVolt.isFinite() || input.nominalVoltageVolt <= 0.0) {
            return Result.failure(IllegalArgumentException("ولتاژ نامی باید عددی محدود و بزرگتر از صفر ولت باشد."))
        }
        if (!input.lengthMeters.isFinite() || input.lengthMeters <= 0.0) {
            return Result.failure(IllegalArgumentException("طول کابل باید عددی محدود و بزرگتر از صفر متر باشد."))
        }
        if (!input.crossSectionMm2.isFinite() || input.crossSectionMm2 <= 0.0) {
            return Result.failure(IllegalArgumentException("سطح مقطع کابل باید عددی محدود و بزرگتر از صفر میلی‌متر مربع باشد."))
        }
        if (!input.cosPhi.isFinite() || input.cosPhi <= 0.0 || input.cosPhi > 1.0) {
            return Result.failure(IllegalArgumentException("ضریب توان باید عددی محدود و بین ۰ و ۱ باشد."))
        }
        if (!input.conductorTemperatureCelsius.isFinite()) {
            return Result.failure(IllegalArgumentException("دمای هادی باید یک عدد معتبر و محدود باشد."))
        }
        val temperatureResistanceFactor = 1.0 + input.material.alpha * (input.conductorTemperatureCelsius - 20.0)
        if (!temperatureResistanceFactor.isFinite() || temperatureResistanceFactor <= 0.0) {
            return Result.failure(IllegalArgumentException("دمای واردشده باعث ضریب مقاومت نامعتبر می‌شود؛ دمای هادی را بررسی کنید."))
        }
        if (!input.reactancePerKm.isFinite() || input.reactancePerKm < 0.0) {
            return Result.failure(IllegalArgumentException("راکتانس کابل باید عددی محدود و نامنفی باشد."))
        }
        val maxAllowedInput = input.customAllowablePercent
        if (maxAllowedInput != null && (!maxAllowedInput.isFinite() || maxAllowedInput <= 0.0)) {
            return Result.failure(IllegalArgumentException("حد مجاز افت ولتاژ سفارشی باید عددی محدود و بزرگتر از صفر باشد."))
        }

        // ۱. محاسبه مقاومت در دمای ۲۰ درجه: R20 = ρ20 · L / S
        val r20 = (input.material.rho20 * input.lengthMeters) / input.crossSectionMm2

        // ۲. تصحیح دما: R_θ = R20 · [1 + α · (θ - 20)]
        val rTheta = r20 * (1.0 + input.material.alpha * (input.conductorTemperatureCelsius - 20.0))

        // ۳. راکتانس کل کابل: X = x0 · (L / 1000)
        val xTotal = input.reactancePerKm * (input.lengthMeters / 1000.0)

        // ۴. sinφ = √(1 - cos²φ)
        val sinPhi = sqrt((1.0 - input.cosPhi * input.cosPhi).coerceAtLeast(0.0))

        // ۵. محاسبه افت ولتاژ ولتی
        val deltaUVolt = if (input.isThreePhase) {
            // ΔU = √3 · I · (R · cosφ + X · sinφ)
            sqrt(3.0) * input.currentAmp * (rTheta * input.cosPhi + xTotal * sinPhi)
        } else {
            // ΔU = 2 · I · (R · cosφ + X · sinφ)
            2.0 * input.currentAmp * (rTheta * input.cosPhi + xTotal * sinPhi)
        }

        // ۶. افت ولتاژ درصدی
        val deltaUPercent = (deltaUVolt / input.nominalVoltageVolt) * 100.0

        val maxAllowed = input.customAllowablePercent ?: input.application.allowableDropPercent
        val isCompliant = deltaUPercent <= maxAllowed

        // تعیین حداقل سطح مقطع استاندارد پیشنهادی در صورت عدم انطباق یا بهینه‌سازی
        val recommendedSection = STANDARD_CROSS_SECTIONS.firstOrNull { sCandidate ->
            val r20Cand = (input.material.rho20 * input.lengthMeters) / sCandidate
            val rThCand = r20Cand * (1.0 + input.material.alpha * (input.conductorTemperatureCelsius - 20.0))
            val dU = if (input.isThreePhase) {
                sqrt(3.0) * input.currentAmp * (rThCand * input.cosPhi + xTotal * sinPhi)
            } else {
                2.0 * input.currentAmp * (rThCand * input.cosPhi + xTotal * sinPhi)
            }
            val dUPct = (dU / input.nominalVoltageVolt) * 100.0
            dUPct <= maxAllowed
        }

        val formulaStr = if (input.isThreePhase) {
            "ΔU = √3 · I · L · (R·cosφ + X·sinφ) / L  ==>  ΔU = √3 · I · (R_θ·cosφ + X·sinφ)"
        } else {
            "ΔU = 2 · I · (R_θ·cosφ + X·sinφ)"
        }

        val assumptions = listOf(
            "هادی: ${input.material.persianTitle} (${input.material.standardRef})",
            "دمای کاربری هادی: ${input.conductorTemperatureCelsius}°C (تصحیح مقاومت با ضریب α)",
            "راکتانس کابل: ${input.reactancePerKm} Ω/km (مرجع: IEC 60364-5-52 پیوست G)",
            "سقف مجاز افت ولتاژ: $maxAllowed% (${input.application.nationalCodeRef})"
        )

        return Result.success(
            VoltageDropResult(
                deltaUVolt = deltaUVolt,
                deltaUPercent = deltaUPercent,
                allowablePercent = maxAllowed,
                isCompliant = isCompliant,
                resistanceAtTempOhm = rTheta,
                reactanceTotalOhm = xTotal,
                formulaUsed = formulaStr,
                recommendedMinSectionMm2 = recommendedSection,
                technicalAssumptions = assumptions
            )
        )
    }
}
