package com.example.calc

import kotlin.math.cos
import kotlin.math.sqrt

/**
 * ابزار ۱: جریان و توان موتور الکتریکی
 * مطابق با استاندارد IEC 60034-1
 */
object MotorPowerCalculator {

    const val FORMULA_3PHASE_CURRENT = "I = P / (√3 · U · cosφ · η)"
    const val FORMULA_1PHASE_CURRENT = "I = P / (U · cosφ · η)"
    const val FORMULA_3PHASE_POWER = "P = √3 · U · I · cosφ · η"
    const val FORMULA_1PHASE_POWER = "P = U · I · cosφ · η"
    const val STANDARD_REF = "استاندارد IEC 60034-1 (Rotating electrical machines - Rating and performance) و مبحث ۱۳ مقررات ملی ساختمان"

    const val HP_TO_KW_FACTOR = 0.7457

    data class MotorCurrentInput(
        val isThreePhase: Boolean = true,
        val powerValue: Double,
        val isHorsepower: Boolean = false,
        val voltage: Double = if (isThreePhase) 400.0 else 230.0,
        val cosPhi: Double = 0.85, // مقدار نمونه
        val efficiency: Double = 0.88 // مقدار نمونه (۸۸٪)
    )

    data class MotorPowerFromCurrentInput(
        val isThreePhase: Boolean = true,
        val currentAmp: Double,
        val voltage: Double = if (isThreePhase) 400.0 else 230.0,
        val cosPhi: Double = 0.85,
        val efficiency: Double = 0.88
    )

    data class MotorResult(
        val powerKw: Double,
        val powerHp: Double,
        val currentAmp: Double,
        val apparentPowerKva: Double,
        val reactivePowerKvar: Double,
        val formulaUsed: String,
        val assumptions: List<String>,
        val standardRef: String = STANDARD_REF
    )

    /**
     * تبدیل اسب بخار به کیلووات
     */
    fun hpToKw(hp: Double): Double = hp * HP_TO_KW_FACTOR

    /**
     * تبدیل کیلووات به اسب بخار
     */
    fun kwToHp(kw: Double): Double = if (kw <= 0.0) 0.0 else kw / HP_TO_KW_FACTOR

    /**
     * محاسبه جریان نامی موتور از روی توان مکانیکی نامی
     */
    fun calculateCurrentFromPower(input: MotorCurrentInput): Result<MotorResult> {
        if (input.powerValue <= 0.0) {
            return Result.failure(IllegalArgumentException("توان موتور باید بزرگتر از صفر باشد."))
        }
        if (input.voltage <= 0.0) {
            return Result.failure(IllegalArgumentException("ولتاژ شبکه باید بزرگتر از صفر ولت باشد."))
        }
        if (input.cosPhi <= 0.0 || input.cosPhi > 1.0) {
            return Result.failure(IllegalArgumentException("ضریب توان (cosφ) باید عددی مثبت و حداکثر ۱.۰ باشد."))
        }
        if (input.efficiency <= 0.0 || input.efficiency > 1.0) {
            return Result.failure(IllegalArgumentException("راندمان موتور (η) باید عددی مثبت و حداکثر ۱.۰ (۱۰۰٪) باشد."))
        }

        val powerKw = if (input.isHorsepower) hpToKw(input.powerValue) else input.powerValue
        val powerHp = if (input.isHorsepower) input.powerValue else kwToHp(input.powerValue)
        val powerWatts = powerKw * 1000.0

        val currentAmp = if (input.isThreePhase) {
            // I = P / (√3 · U · cosφ · η)
            powerWatts / (sqrt(3.0) * input.voltage * input.cosPhi * input.efficiency)
        } else {
            // I = P / (U · cosφ · η)
            powerWatts / (input.voltage * input.cosPhi * input.efficiency)
        }

        val apparentPowerKva = if (input.isThreePhase) {
            (sqrt(3.0) * input.voltage * currentAmp) / 1000.0
        } else {
            (input.voltage * currentAmp) / 1000.0
        }

        // توان الکتریکی ورودی به موتور: P_in = P_out / η
        val activeElectricKw = powerKw / input.efficiency
        val reactivePowerKvar = sqrt((apparentPowerKva * apparentPowerKva - activeElectricKw * activeElectricKw).coerceAtLeast(0.0))

        val formula = if (input.isThreePhase) FORMULA_3PHASE_CURRENT else FORMULA_1PHASE_CURRENT
        val assumptions = listOf(
            "نوع سیستم: ${if (input.isThreePhase) "سه فاز متقارن" else "تک فاز متناوب"}",
            "ولتاژ خط: ${input.voltage} V",
            "ضریب توان فرضی: ${input.cosPhi} (ورودی کاربر / نمونه)",
            "راندمان فرضی: ${(input.efficiency * 100).toInt()}% (ورودی کاربر / نمونه)",
            "توان نامی پلاک: ${String.format("%.2f", powerKw)} kW (${String.format("%.2f", powerHp)} HP)"
        )

        return Result.success(
            MotorResult(
                powerKw = powerKw,
                powerHp = powerHp,
                currentAmp = currentAmp,
                apparentPowerKva = apparentPowerKva,
                reactivePowerKvar = reactivePowerKvar,
                formulaUsed = formula,
                assumptions = assumptions
            )
        )
    }

    /**
     * محاسبه توان موتور از روی جریان اندازه‌گیری‌شده
     */
    fun calculatePowerFromCurrent(input: MotorPowerFromCurrentInput): Result<MotorResult> {
        if (input.currentAmp <= 0.0) {
            return Result.failure(IllegalArgumentException("جریان موتور باید بزرگتر از صفر آمپر باشد."))
        }
        if (input.voltage <= 0.0) {
            return Result.failure(IllegalArgumentException("ولتاژ باید بزرگتر از صفر ولت باشد."))
        }
        if (input.cosPhi <= 0.0 || input.cosPhi > 1.0) {
            return Result.failure(IllegalArgumentException("ضریب توان باید بین ۰ و ۱ باشد."))
        }
        if (input.efficiency <= 0.0 || input.efficiency > 1.0) {
            return Result.failure(IllegalArgumentException("راندمان باید بین ۰ و ۱ باشد."))
        }

        val powerWatts = if (input.isThreePhase) {
            sqrt(3.0) * input.voltage * input.currentAmp * input.cosPhi * input.efficiency
        } else {
            input.voltage * input.currentAmp * input.cosPhi * input.efficiency
        }

        val powerKw = powerWatts / 1000.0
        val powerHp = kwToHp(powerKw)

        val apparentPowerKva = if (input.isThreePhase) {
            (sqrt(3.0) * input.voltage * input.currentAmp) / 1000.0
        } else {
            (input.voltage * input.currentAmp) / 1000.0
        }

        val activeElectricKw = powerKw / input.efficiency
        val reactivePowerKvar = sqrt((apparentPowerKva * apparentPowerKva - activeElectricKw * activeElectricKw).coerceAtLeast(0.0))

        val formula = if (input.isThreePhase) FORMULA_3PHASE_POWER else FORMULA_1PHASE_POWER
        val assumptions = listOf(
            "سیستم: ${if (input.isThreePhase) "سه فاز" else "تک فاز"}",
            "جریان فاز: ${input.currentAmp} A",
            "ضریب توان نمونه: ${input.cosPhi}",
            "راندمان نمونه: ${(input.efficiency * 100).toInt()}%"
        )

        return Result.success(
            MotorResult(
                powerKw = powerKw,
                powerHp = powerHp,
                currentAmp = input.currentAmp,
                apparentPowerKva = apparentPowerKva,
                reactivePowerKvar = reactivePowerKvar,
                formulaUsed = formula,
                assumptions = assumptions
            )
        )
    }
}
