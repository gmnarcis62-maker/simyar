package com.example.calc

import kotlinx.serialization.Serializable

/**
 * انواع ابزارهای محاسباتی برق صنعتی سیمیار (P8)
 */
enum class CalculatorToolType(
    val id: String,
    val titlePersian: String,
    val subtitlePersian: String,
    val standardReference: String,
    val isVipOnly: Boolean
) {
    MOTOR_CURRENT_POWER(
        id = "motor_current_power",
        titlePersian = "جریان و توان موتور الکتریکی",
        subtitlePersian = "محاسبه جریان نامی، توان اکتیو/راکتیو، ضریب توان و راندمان (سه فاز و تک فاز)",
        standardReference = "استاندارد IEC 60034-1 و مبحث ۱۳ مقررات ملی ساختمان",
        isVipOnly = false // ابزار اول رایگان
    ),
    CONTACTOR_SELECTION(
        id = "contactor_selection",
        titlePersian = "انتخاب کنتاکتور (رده کاری AC-3)",
        subtitlePersian = "تعیین جریان عملکردی نامی Ie کنتاکتور در راه‌اندازی مستقیم و ستاره-مثلث",
        standardReference = "استاندارد IEC 60947-4-1 (دستگاه‌های کلیدزنی و راه‌اندازها)",
        isVipOnly = true
    ),
    BIMETAL_SETTING(
        id = "bimetal_setting",
        titlePersian = "تنظیم رله اضافه‌بار حرارتی (بی‌متال)",
        subtitlePersian = "محاسبه جریان تنظیمی بر اساس موقعیت قرارگیری رله در مدار مستقیم و ستاره-مثلث",
        standardReference = "استاندارد IEC 60947-4-1 و روش مرجع سیمیار",
        isVipOnly = true
    ),
    PROTECTION_COORDINATION(
        id = "protection_coordination",
        titlePersian = "هماهنگی فیوز و کلید مینیاتوری (MCB)",
        subtitlePersian = "ارزیابی شرایط دوگانه حفاظت اضافه‌بار کابل و هماهنگی جریانی",
        standardReference = "استاندارد IEC 60364-4-43 بند ۴۳۳-۱",
        isVipOnly = true
    ),
    VOLTAGE_DROP(
        id = "voltage_drop",
        titlePersian = "افت ولتاژ خطوط و کابل‌ها",
        subtitlePersian = "محاسبه افت ولتاژ ولتی و درصدی با تصحیح حرارتی مقاومت و راکتانس هادی",
        standardReference = "استاندارد IEC 60364-5-52 و بند ۱۳-۴-۱-۵ مبحث ۱۳ مقررات ملی",
        isVipOnly = true
    ),
    CABLE_SIZING(
        id = "cable_sizing",
        titlePersian = "تعیین سطح مقطع کابل و ظرفیت جریان‌دهی",
        subtitlePersian = "انتخاب هادی بر اساس ظرفیت جریان‌دهی پیوسته و ضرایب تصحیح دما و همجواری",
        standardReference = "استاندارد IEC 60364-5-52 جدول B.52.4 و جدول پ ۶-۱ مبحث ۱۳",
        isVipOnly = true
    )
}

/**
 * آیتم تاریخچه محاسبات جهت ذخیره‌سازی در پروژه
 */
@Serializable
data class CalculationHistoryItem(
    val id: String,
    val toolType: CalculatorToolType,
    val timestamp: Long = System.currentTimeMillis(),
    val projectId: String? = null,
    val projectTitle: String? = null,
    val summaryTitle: String,
    val inputValuesSummary: Map<String, String>,
    val outputResultsSummary: Map<String, String>,
    val formulaUsed: String,
    val standardRef: String,
    val notes: String = ""
)

/**
 * هشدار عمومی قانونی برای تمامی ابزارها
 */
const val ENGINEERING_CALC_DISCLAIMER =
    "این ابزار صرفاً یک کمک‌محاسبه مهندسی است و محاسبات اولیه را ارائه می‌دهد؛ طراحی و کنترل نهایی سیستم همواره بر عهده مهندس محاسب و طراح ذی‌صلاح تاسیسات برق می‌باشد."
