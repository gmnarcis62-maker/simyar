package com.example.calc

/**
 * ابزار ۶: تعیین سطح مقطع کابل و ظرفیت جریان‌دهی پیوسته (Iz)
 * مطابق با استاندارد بین‌المللی IEC 60364-5-52 و مقررات ملی ساختمان (مبحث سیزدهم)
 */
object CableSizingCalculator {

    const val STANDARD_REF = "استاندارد بین‌المللی IEC 60364-5-52 جدول B.52.4 و مبحث ۱۳ مقررات ملی ساختمان (جدول پ ۶-۱ و پ ۶-۷ و پ ۶-۹)"

    enum class CableInsulation(
        val persianTitle: String,
        val maxOperatingTempCelsius: Double
    ) {
        PVC("عایق پی‌وی‌سی PVC (حداکثر دمای هادی ۷۰°C)", 70.0),
        XLPE("عایق پلی‌اتیلن کراس‌لینک XLPE / EPR (حداکثر دمای هادی ۹۰°C)", 90.0)
    }

    enum class ConductorType(val persianTitle: String) {
        COPPER("مس (Copper)"),
        ALUMINUM("آلومینیوم (Aluminum)")
    }

    /**
     * روش‌های مرجع نصب طبق IEC 60364-5-52
     */
    enum class InstallationMethod(val code: String, val persianTitle: String) {
        METHOD_C("C", "روش C: کابل چندرشته‌ای نصب‌شده روی دیوار گچی/بتونی یا سینی کابل بدون سوراخ"),
        METHOD_E("E", "روش E: کابل چندرشته‌ای در هوای آزاد روی نردبان کابل یا سینی مشبک"),
        METHOD_B2("B2", "روش B2: کابل چندرشته‌ای در لوله توکار یا داکت بسته")
    }

    data class CableCapacityEntry(
        val sectionMm2: Double,
        val currentAmp: Double
    )

    /**
     * جدول رسمی ظرفیت جریان‌دهی پیوسته هادی‌های مسی و آلومینیومی در هوای ۳۰°C (روش نصب C - مبحث ۱۳ جدول پ ۶-۱ و IEC 60364-5-52 جدول B.52.4)
     */
    val OFFICIAL_IEC_COPPER_PVC_METHOD_C = listOf(
        CableCapacityEntry(1.5, 19.5),
        CableCapacityEntry(2.5, 27.0),
        CableCapacityEntry(4.0, 36.0),
        CableCapacityEntry(6.0, 46.0),
        CableCapacityEntry(10.0, 63.0),
        CableCapacityEntry(16.0, 85.0),
        CableCapacityEntry(25.0, 112.0),
        CableCapacityEntry(35.0, 138.0),
        CableCapacityEntry(50.0, 168.0),
        CableCapacityEntry(70.0, 213.0),
        CableCapacityEntry(95.0, 258.0),
        CableCapacityEntry(120.0, 299.0),
        CableCapacityEntry(150.0, 344.0),
        CableCapacityEntry(185.0, 392.0),
        CableCapacityEntry(240.0, 461.0),
        CableCapacityEntry(300.0, 530.0)
    )

    val OFFICIAL_IEC_COPPER_XLPE_METHOD_C = listOf(
        CableCapacityEntry(1.5, 24.0),
        CableCapacityEntry(2.5, 33.0),
        CableCapacityEntry(4.0, 45.0),
        CableCapacityEntry(6.0, 58.0),
        CableCapacityEntry(10.0, 80.0),
        CableCapacityEntry(16.0, 107.0),
        CableCapacityEntry(25.0, 138.0),
        CableCapacityEntry(35.0, 171.0),
        CableCapacityEntry(50.0, 209.0),
        CableCapacityEntry(70.0, 269.0),
        CableCapacityEntry(95.0, 328.0),
        CableCapacityEntry(120.0, 382.0),
        CableCapacityEntry(150.0, 441.0),
        CableCapacityEntry(185.0, 506.0),
        CableCapacityEntry(240.0, 599.0),
        CableCapacityEntry(300.0, 693.0)
    )

    /**
     * جدول ضرایب تصحیح دما k_temp برای هوای محیط غیر از ۳۰ درجه (IEC 60364-5-52 جدول B.52.14 و مبحث ۱۳ جدول پ ۶-۷)
     */
    fun getTemperatureCorrectionFactor(ambientTempCelsius: Double, insulation: CableInsulation): Double {
        return when (insulation) {
            CableInsulation.PVC -> when {
                ambientTempCelsius <= 20.0 -> 1.12
                ambientTempCelsius <= 25.0 -> 1.06
                ambientTempCelsius <= 30.0 -> 1.00
                ambientTempCelsius <= 35.0 -> 0.94
                ambientTempCelsius <= 40.0 -> 0.87
                ambientTempCelsius <= 45.0 -> 0.79
                ambientTempCelsius <= 50.0 -> 0.71
                ambientTempCelsius <= 55.0 -> 0.61
                else -> 0.50
            }
            CableInsulation.XLPE -> when {
                ambientTempCelsius <= 20.0 -> 1.08
                ambientTempCelsius <= 25.0 -> 1.04
                ambientTempCelsius <= 30.0 -> 1.00
                ambientTempCelsius <= 35.0 -> 0.96
                ambientTempCelsius <= 40.0 -> 0.91
                ambientTempCelsius <= 45.0 -> 0.87
                ambientTempCelsius <= 50.0 -> 0.82
                ambientTempCelsius <= 55.0 -> 0.76
                else -> 0.67
            }
        }
    }

    /**
     * ضریب همجواری / گروهبندی کابل‌ها k_group (IEC 60364-5-52 جدول B.52.17)
     */
    fun getGroupingFactor(numberOfCircuitsOrCables: Int): Double {
        return when (numberOfCircuitsOrCables) {
            1 -> 1.00
            2 -> 0.80
            3 -> 0.70
            4 -> 0.65
            5 -> 0.60
            6 -> 0.57
            7 -> 0.54
            8 -> 0.52
            in 9..12 -> 0.50
            else -> 0.45
        }
    }

    data class CableSizingInput(
        val designCurrentIbAmp: Double,
        val insulation: CableInsulation = CableInsulation.PVC,
        val conductor: ConductorType = ConductorType.COPPER,
        val ambientTemperatureCelsius: Double = 35.0, // دمای هوای محیط
        val groupedCircuitsCount: Int = 1, // تعداد مدارهای همجوار
        val useCustomCapacityTable: Boolean = false,
        val customCapacityTable: List<CableCapacityEntry> = emptyList()
    )

    data class CableSizingResult(
        val designCurrentIbAmp: Double,
        val requiredDeratedCapacityAmp: Double,
        val selectedSectionMm2: Double,
        val baseCapacityIzAmp: Double,
        val effectiveCapacityIzPrimeAmp: Double,
        val kTemp: Double,
        val kGroup: Double,
        val totalDeratingFactor: Double,
        val formulaUsed: String,
        val tableReference: String,
        val technicalAssumptions: List<String>,
        val isUserCustomTable: Boolean,
        val standardRef: String = STANDARD_REF
    )

    fun calculateCableSize(input: CableSizingInput): Result<CableSizingResult> {
        if (!input.designCurrentIbAmp.isFinite() || input.designCurrentIbAmp <= 0.0) {
            return Result.failure(IllegalArgumentException("جریان طراحی مدار (Ib) باید یک عدد محدود و بزرگتر از صفر باشد."))
        }
        if (!input.ambientTemperatureCelsius.isFinite()) {
            return Result.failure(IllegalArgumentException("دمای محیط باید یک عدد معتبر و محدود باشد."))
        }
        if (input.ambientTemperatureCelsius >= 65.0) {
            return Result.failure(IllegalArgumentException("دمای محیط بسیار بالاست و کابل در این دما مجاز نیست."))
        }
        if (input.groupedCircuitsCount < 1) {
            return Result.failure(IllegalArgumentException("تعداد مدارهای همجوار باید حداقل ۱ باشد."))
        }
        if (input.useCustomCapacityTable) {
            if (input.customCapacityTable.isEmpty()) {
                return Result.failure(IllegalArgumentException("جدول سفارشی فعال است اما هیچ ردیفی ندارد؛ جدول رسمی به‌صورت خودکار جایگزین نمی‌شود."))
            }
            if (input.customCapacityTable.any { entry ->
                    !entry.sectionMm2.isFinite() || entry.sectionMm2 <= 0.0 ||
                        !entry.currentAmp.isFinite() || entry.currentAmp <= 0.0
                }) {
                return Result.failure(IllegalArgumentException("تمام ردیف‌های جدول سفارشی باید سطح مقطع و ظرفیت جریان محدود و بزرگتر از صفر داشته باشند."))
            }
        }

        val kTemp = getTemperatureCorrectionFactor(input.ambientTemperatureCelsius, input.insulation)
        val kGroup = getGroupingFactor(input.groupedCircuitsCount.coerceAtLeast(1))
        val totalDerating = kTemp * kGroup

        // جریان پایه مورد نیاز از جدول: Iz_base >= Ib / (k_temp * k_group)
        val requiredBaseCurrent = input.designCurrentIbAmp / totalDerating

        val (capacityTable, tableRefName) = if (input.useCustomCapacityTable && input.customCapacityTable.isNotEmpty()) {
            Pair(input.customCapacityTable.sortedBy { it.sectionMm2 }, "جدول سفارشی / نمونه واردشده توسط کاربر (تأییدنشده رسمی)")
        } else {
            val baseTable = if (input.insulation == CableInsulation.PVC) {
                OFFICIAL_IEC_COPPER_PVC_METHOD_C
            } else {
                OFFICIAL_IEC_COPPER_XLPE_METHOD_C
            }
            // اگر آلومینیوم باشد، نسبت به مس ظرفیت حدود ۰.۷۸ برابر می‌گردد
            val adaptedTable = if (input.conductor == ConductorType.ALUMINUM) {
                baseTable.map { CableCapacityEntry(it.sectionMm2, it.currentAmp * 0.78) }
            } else baseTable

            Pair(
                adaptedTable,
                "جدول B.52.4 استاندارد IEC 60364-5-52 و جدول پ ۶-۱ مبحث ۱۳ مقررات ملی (روش C، هوای ۳۰°C)"
            )
        }

        // یافتن اولین سطح مقطعی که ظرفیت پایه آن پاسخگوی جریان باشد
        val selectedEntry = capacityTable.firstOrNull { it.currentAmp >= requiredBaseCurrent }
            ?: return Result.failure(
                IllegalStateException(
                    "هیچ سطح مقطعی در جدول برای جریان مورد نیاز (${String.format("%.1f", requiredBaseCurrent)} A با احتساب ضرایب تصحیح) یافت نشد. استفاده از کابل‌های موازی را بررسی کنید."
                )
            )

        val effectiveCapacity = selectedEntry.currentAmp * totalDerating

        val formulaStr = "Iz' = Iz_base · k_temp · k_group ≥ Ib  ==>  Iz_base ≥ Ib / (k_temp · k_group)"

        val assumptions = listOf(
            "نوع هادی و عایق: ${input.conductor.persianTitle} با ${input.insulation.persianTitle}",
            "دمای محیط: ${input.ambientTemperatureCelsius}°C (ضریب تصحیح دما k_temp = $kTemp طبق جدول B.52.14)",
            "تعداد مدارهای همجوار: ${input.groupedCircuitsCount} (ضریب همجواری k_group = $kGroup طبق جدول B.52.17)",
            "ضریب تصحیح کل: ${String.format("%.3f", totalDerating)}",
            "جریان مبنای مورد نیاز از جدول: ${String.format("%.1f", requiredBaseCurrent)} A",
            "مرجع جدول: $tableRefName"
        )

        return Result.success(
            CableSizingResult(
                designCurrentIbAmp = input.designCurrentIbAmp,
                requiredDeratedCapacityAmp = requiredBaseCurrent,
                selectedSectionMm2 = selectedEntry.sectionMm2,
                baseCapacityIzAmp = selectedEntry.currentAmp,
                effectiveCapacityIzPrimeAmp = effectiveCapacity,
                kTemp = kTemp,
                kGroup = kGroup,
                totalDeratingFactor = totalDerating,
                formulaUsed = formulaStr,
                tableReference = tableRefName,
                technicalAssumptions = assumptions,
                isUserCustomTable = input.useCustomCapacityTable
            )
        )
    }
}
