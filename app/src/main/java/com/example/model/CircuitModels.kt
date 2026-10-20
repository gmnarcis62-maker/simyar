package com.example.model

import kotlinx.serialization.Serializable

@Serializable
enum class ComponentType(
    val persianName: String,
    val category: String,
    val defaultPrefix: String
) {
    // تغذیه و ورودی
    THREE_PHASE_SOURCE("منبع سه‌فاز (3-Phase Power L1-L2-L3)", "منابع تغذیه", "L3P"),
    AC_PHASE("فاز متناوب (Phase L)", "منابع تغذیه", "L"),
    AC_NEUTRAL("نول متناوب (Neutral N)", "منابع تغذیه", "N"),
    DC_PLUS("مثبت ۲۴ ولت (+24V DC)", "منابع تغذیه", "+24V"),
    DC_MINUS("منفی ۰ ولت (0V DC)", "منابع تغذیه", "0V"),
    EARTH("ارت حفاظتی (PE)", "منابع تغذیه", "PE"),

    // تجهیزات حفاظتی
    CIRCUIT_BREAKER_MCB("کلید مینیاتوری تک‌پل (MCB)", "تجهیزات حفاظتی", "QF"),
    CIRCUIT_BREAKER_MCB_3P("کلید مینیاتوری سه‌پل (3P MCB)", "تجهیزات حفاظتی", "QF"),
    CIRCUIT_BREAKER_MCCB_3P("کلید اتوماتیک کمپکت سه‌پل (3P MCCB)", "تجهیزات حفاظتی", "QF"),
    FUSE("فیوز مینیاتوری (Fuse)", "تجهیزات حفاظتی", "FU"),
    FUSE_3P("پایه فیوز سه‌پل (3P Fuse Base)", "تجهیزات حفاظتی", "FU"),
    RCD_4P("کلید محافظ جان ۴ پل (4P RCD/RCCB)", "تجهیزات حفاظتی", "Q"),
    PHASE_SEQUENCE_RELAY("رله کنترل فاز و توالی فاز (Phase Relay)", "تجهیزات حفاظتی", "KF"),
    BIMETAL_OVERLOAD("رله حرارتی بی‌متال (Overload Relay)", "تجهیزات حفاظتی", "FR"),
    EMERGENCY_STOP("شستی قارچی اضطراری (E-Stop)", "تجهیزات حفاظتی", "SB0"),

    // کلیدها و شستی‌ها
    PUSH_BUTTON_NO("شستی استارت (Start NO)", "شستی و کنترل", "SB"),
    PUSH_BUTTON_NC("شستی استپ (Stop NC)", "شستی و کنترل", "SB"),
    SELECTOR_SWITCH_2POS("کلید سلکتوری دوحالته (Selector Switch)", "شستی و کنترل", "SA"),

    // سوئیچ‌ها و حسگرها
    LIMIT_SWITCH_NO("میکروسوئیچ باز (Limit Switch NO)", "سنسورها", "SQ"),
    LIMIT_SWITCH_NC("میکروسوئیچ بسته (Limit Switch NC)", "سنسورها", "SQ"),
    PROXIMITY_SENSOR("سنسور القایی مجاورتی (Proximity)", "سنسورها", "B"),

    // کنتاکتور و بوبین‌ها
    CONTACTOR_COIL("کویل / بوبین کنتاکتور (Contactor Coil)", "کنتاکتور و رله", "KM"),
    CONTACTOR_NO_AUX("کنتاکت باز کنتاکتور (Aux NO 13-14)", "کنتاکتور و رله", "KM"),
    CONTACTOR_NC_AUX("کنتاکت بسته کنتاکتور (Aux NC 21-22)", "کنتاکتور و رله", "KM"),
    MAIN_POWER_CONTACTS_3P("کنتاکت‌های قدرت ۳ فاز (Main Contacts)", "کنتاکتور و رله", "KM"),
    CONTROL_RELAY_1CO("رله کمکی ۱ کنتاکت تبدیلی (1-CO Relay)", "کنتاکتور و رله", "KA"),
    CONTROL_RELAY_2CO("رله کمکی ۲ کنتاکت تبدیلی (2-CO Relay)", "کنتاکتور و رله", "KA"),

    // تایمرها
    TIMER_ON_DELAY_COIL("تایمر تاخیر در وصل (Timer TON)", "تایمرها", "KT"),
    TIMER_OFF_DELAY_COIL("تایمر تاخیر در قطع (Timer TOF)", "تایمرها", "KT"),
    TIMER_NO_CONTACT("کنتاکت باز تایمر (Timer Contact NO)", "تایمرها", "KT"),
    TIMER_NC_CONTACT("کنتاکت بسته تایمر (Timer Contact NC)", "تایمرها", "KT"),

    // نشانگرها و بارها
    PILOT_LAMP_GREEN("چراغ سیگنال سبز (Pilot Lamp Green)", "خروجی و نشانگر", "HL"),
    PILOT_LAMP_RED("چراغ سیگنال قرمز (Pilot Lamp Red)", "خروجی و نشانگر", "HL"),
    PILOT_LAMP_YELLOW("چراغ سیگنال زرد (Pilot Lamp Yellow)", "خروجی و نشانگر", "HL"),
    ALARM_BUZZER("آژیر هشدار (Alarm Buzzer)", "خروجی و نشانگر", "HA"),
    MOTOR_3PHASE("الکتروموتور سه‌فاز القایی (3-Phase Motor)", "مصرف‌کننده‌ها", "M"),
    MOTOR_1PHASE("الکتروموتور تک‌فاز (Single Phase Motor)", "مصرف‌کننده‌ها", "M")
}

@Serializable
data class Terminal(
    val id: String,
    val name: String,
    val relX: Float,
    val relY: Float,
    val isSource: Boolean = false,
    val voltageType: VoltageType = VoltageType.CONTROL_AC
)

@Serializable
enum class VoltageType {
    CONTROL_AC,
    POWER_3P,
    DC_24V,
    NEUTRAL,
    GROUND
}

@Serializable
enum class SpecSourceType {
    STANDARD,
    DATASHEET,
    SAMPLE,
    UNKNOWN
}

@Serializable
data class SpecSource(
    val type: SpecSourceType = SpecSourceType.SAMPLE,
    val ref: String = ""
) {
    val displayBadge: String
        get() = when (type) {
            SpecSourceType.STANDARD -> if (ref.isNotBlank()) "استاندارد: $ref" else "استاندارد"
            SpecSourceType.DATASHEET -> if (ref.isNotBlank()) "دیتاشیت" else "دیتاشیت"
            SpecSourceType.SAMPLE -> "نمونه"
            SpecSourceType.UNKNOWN -> "نامشخص"
        }
}

@Serializable
data class TechnicalSpecs(
    val ratedVoltage: String = "۳۸۰/۲۲۰V AC (۵۰Hz)",
    val ratedCurrent: String = "۱۰A",
    val contactsNO: Int = 1,
    val contactsNC: Int = 0,
    val powerRating: String = "-",
    val standardCode: String = "IEC 60947",
    val manufacturerModel: String = "",
    val isSampleValue: Boolean = false,
    val datasheetUrl: String = "",
    val voltageSource: SpecSource = SpecSource(SpecSourceType.SAMPLE, "مقدار نمونه (قابل ویرایش)"),
    val currentSource: SpecSource = SpecSource(SpecSourceType.SAMPLE, "مقدار نمونه (قابل ویرایش)"),
    val powerSource: SpecSource = SpecSource(SpecSourceType.SAMPLE, "مقدار نمونه (قابل ویرایش)"),
    val standardCodeSource: SpecSource = SpecSource(SpecSourceType.SAMPLE, "مقدار نمونه (قابل ویرایش)"),
    val modelSource: SpecSource = SpecSource(SpecSourceType.SAMPLE, "مقدار نمونه (قابل ویرایش)"),
    val contactsSource: SpecSource = SpecSource(SpecSourceType.SAMPLE, "مقدار نمونه (قابل ویرایش)")
) {
    fun allFieldSources(): Map<String, Pair<String, SpecSource>> = mapOf(
        "ولتاژ نامی" to Pair(ratedVoltage, voltageSource),
        "جریان نامی" to Pair(ratedCurrent, currentSource),
        "توان نامی" to Pair(powerRating, powerSource),
        "کد استاندارد" to Pair(standardCode, standardCodeSource),
        "مدل سازنده" to Pair(manufacturerModel.ifBlank { "نامشخص" }, modelSource),
        "کنتاکت‌ها" to Pair("NO:$contactsNO / NC:$contactsNC", contactsSource)
    )
}

const val EDUCATIONAL_SIMULATION_DISCLAIMER = "شبیه‌سازی آموزشی؛ جایگزین محاسبات و تست میدانی نیست"

@Serializable
enum class ComponentFault(val faName: String, val description: String) {
    NONE("سالم (بدون خطا)", "عملکرد کاملاً استاندارد و نرمال"),
    WELDED_CONTACT("تیغه چسبیده بسته (Welded Contact)", "تیغه‌ها به دلیل جریان شدید اتصال کوتاه به یکدیگر جوش خورده و قطع نمی‌شوند"),
    CONTACT_STUCK_OPEN("تیغه باز شونده/وصل نشونده (Stuck Open)", "تیغه به دلیل آسیب مکانیکی یا دوده گرفتگی وصل نمی‌شود"),
    BURNED_COIL("سیم‌پیچ کویل سوخته (Burned/Open Coil)", "بوبین دچار سوختگی یا قطعی داخلی شده و مگنت ایجاد نمی‌کند"),
    LOOSE_CONNECTION("اتصال شل / قطعی سیم (Loose Connection)", "عدم اتصال الکتریکی پایانه به مدار به دلیل شل بودن پیچ"),
    LOCKED_ROTOR("روتور قفل شده (Locked Rotor)", "روتور موتور در اثر گیرپاژ مکانیکی قفل شده و جریان اتصال کوتاه/راه‌اندازی بالا می‌کشد"),
    EARTH_LEAKAGE("نشتی جریان به ارت (Earth Leakage)", "اتصال مقاومتی ضعیف هادی فاز به بدنه یا زمین که منجر به تریپ کلید محافظ جان RCD می‌شود")
}

@Serializable
enum class MultimeterMode(val faName: String, val unit: String) {
    VOLTAGE_AC("ولتاژ متناوب (AC V)", "V~"),
    VOLTAGE_DC("ولتاژ مستقیم (DC V)", "V="),
    CONTINUITY("تست بوق و پیوستگی (Buzzer)", "Ω")
}

@Serializable
data class MultimeterReading(
    val displayValue: String = "0.0",
    val unit: String = "V",
    val isBeeping: Boolean = false,
    val statusText: String = "آماده اندازه‌گیری",
    val educationalNote: String = EDUCATIONAL_SIMULATION_DISCLAIMER
)

@Serializable
data class MultimeterProbe(
    val componentId: String,
    val terminalId: String
)

@Serializable
data class CircuitComponent(
    val id: String,
    val type: ComponentType,
    val label: String,
    val x: Float,
    val y: Float,
    val rotation: Float = 0f, // زاویه چرخش: 0, 90, 180, 270
    val isLocked: Boolean = false, // قفل موقعیت قطعه
    val isClosedOrPressed: Boolean = false,
    val isBimetalTripped: Boolean = false, // وضعیت تریپ رله حرارتی بی‌متال (قطع مدار با اضافه بار)
    val isMaintained: Boolean = false, // حالت ماندگار (کلید قفل شونده) در برابر لحظه‌ای (بازگشت فنر)
    val isEnergized: Boolean = false,
    val tagBinding: String = "", // اتصال بین کنتاکت و کویل (KM1, KM2, KT1)
    val plcTagBinding: String = "", // اتصال مستقیم به ورودی/خروجی PLC مثل I0.0 یا Q0.0
    val interlockTag: String = "", // برای اینترلاک حفاظتی مثل منع وصل همزمان KM2 با KM1
    val delaySeconds: Float = 3f,
    val currentTimerProgress: Float = 0f,
    val motorRotationDirection: MotorDirection = MotorDirection.STOPPED,
    val specs: TechnicalSpecs = TechnicalSpecs(),
    val sheetId: String = "sheet_1",
    val layerId: String = "layer_control",
    // فیلدهای مرحله P5: تریپ بیمتال زماندار، راه‌اندازی موتور و خطاهای تزریقی
    val bimetalSettingIe: Float = 5.0f, // جریان نامی تنظیمی بی‌متال بر حسب آمپر (Ie)
    val bimetalTripClass: String = "10", // کلاس تریپ: 10A, 10, 20, 30 طبق IEC 60947-4-1
    val bimetalOverloadRatio: Float = 1.0f, // ضریب اضافه بار فعلی I / Ie (بین 1.0 تا 7.2)
    val bimetalThermalState: Float = 0.0f, // حافظه گرمایی ساده I^2*t (بین 0.0 تا 1.0؛ در 1.0 تریپ رخ می‌دهد)
    val bimetalIsAutoReset: Boolean = false, // ریست خودکار در برابر دستی
    val motorStartingCurrentRatio: Float = 6.0f, // ضریب جریان راه‌اندازی (k x In) - مقدار نمونه قابل ویرایش
    val motorStartingTimeSeconds: Float = 3.0f, // زمان راه‌اندازی به ثانیه - مقدار نمونه قابل ویرایش
    val motorElapsedTime: Float = 0.0f, // زمان سپری شده از لحظه وصل موتور
    val isStarConnected: Boolean = false, // آیا در وضعیت راه‌اندازی ستاره است؟ (جریان و گشتاور ۱/۳ مثلث)
    val injectedFault: ComponentFault = ComponentFault.NONE // خطای تزریقی فعال در سناریوی عیب‌یابی
) {
    companion object {
        const val COMPONENT_WIDTH = 100f
        const val COMPONENT_HEIGHT = 80f
        const val TERMINAL_RADIUS = 26f
    }

    fun getTerminals(): List<Terminal> {
        val baseList = when (type) {
            ComponentType.THREE_PHASE_SOURCE -> listOf(
                Terminal("L1", "L1", 20f, 60f, isSource = true, voltageType = VoltageType.POWER_3P),
                Terminal("L2", "L2", 50f, 60f, isSource = true, voltageType = VoltageType.POWER_3P),
                Terminal("L3", "L3", 80f, 60f, isSource = true, voltageType = VoltageType.POWER_3P),
                Terminal("PE", "PE", 50f, 20f, isSource = false, voltageType = VoltageType.GROUND)
            )
            ComponentType.AC_PHASE -> listOf(
                Terminal("L", "L", 40f, 60f, isSource = true, voltageType = VoltageType.CONTROL_AC)
            )
            ComponentType.AC_NEUTRAL -> listOf(
                Terminal("N", "N", 40f, 60f, isSource = false, voltageType = VoltageType.NEUTRAL)
            )
            ComponentType.DC_PLUS -> listOf(
                Terminal("+24V", "+24V", 40f, 60f, isSource = true, voltageType = VoltageType.DC_24V)
            )
            ComponentType.DC_MINUS -> listOf(
                Terminal("0V", "0V", 40f, 60f, isSource = false, voltageType = VoltageType.NEUTRAL)
            )
            ComponentType.EARTH -> listOf(
                Terminal("PE", "PE", 40f, 60f, isSource = false, voltageType = VoltageType.GROUND)
            )
            ComponentType.PUSH_BUTTON_NO, ComponentType.CONTACTOR_NO_AUX -> listOf(
                Terminal("13", "13", 20f, 40f),
                Terminal("14", "14", 80f, 40f)
            )
            ComponentType.PUSH_BUTTON_NC, ComponentType.CONTACTOR_NC_AUX -> listOf(
                Terminal("21", "21", 20f, 40f),
                Terminal("22", "22", 80f, 40f)
            )
            ComponentType.TIMER_NO_CONTACT -> listOf(
                Terminal("15", "15", 20f, 40f),
                Terminal("18", "18", 80f, 40f),
                Terminal("13", "13", 20f, 25f), // سازگاری با پروژه‌ها/تست‌های قبلی
                Terminal("14", "14", 80f, 25f),
                Terminal("17", "17", 20f, 55f)  // سازگاری با تست‌های قبلی
            )
            ComponentType.TIMER_NC_CONTACT -> listOf(
                Terminal("15", "15", 20f, 40f),
                Terminal("16", "16", 80f, 40f),
                Terminal("21", "21", 20f, 25f), // سازگاری با پروژه‌ها/تست‌های قبلی
                Terminal("22", "22", 80f, 25f)
            )
            ComponentType.EMERGENCY_STOP -> listOf(
                Terminal("1", "1", 20f, 40f),
                Terminal("2", "2", 80f, 40f)
            )
            ComponentType.CIRCUIT_BREAKER_MCB, ComponentType.FUSE, ComponentType.SELECTOR_SWITCH_2POS -> listOf(
                Terminal("IN", "1", 20f, 40f),
                Terminal("OUT", "2", 80f, 40f)
            )
            ComponentType.LIMIT_SWITCH_NO -> listOf(
                Terminal("13", "13", 20f, 40f),
                Terminal("14", "14", 80f, 40f),
                Terminal("IN", "13", 20f, 25f), // سازگاری با سیم‌بندی‌های قبلی IN/OUT
                Terminal("OUT", "14", 80f, 25f)
            )
            ComponentType.LIMIT_SWITCH_NC -> listOf(
                Terminal("11", "11", 20f, 40f),
                Terminal("12", "12", 80f, 40f),
                Terminal("IN", "11", 20f, 25f), // سازگاری با سیم‌بندی‌های قبلی IN/OUT
                Terminal("OUT", "12", 80f, 25f)
            )
            ComponentType.BIMETAL_OVERLOAD -> listOf(
                // کنتاکت‌های فرمان
                Terminal("95", "95", 20f, 50f),
                Terminal("96", "96", 45f, 50f),
                Terminal("97", "97", 65f, 50f),
                Terminal("98", "98", 85f, 50f),
                // کنتاکت‌های قدرت ورودی و خروجی (عنصر حرارتی قدرت)
                Terminal("1", "1/L1", 20f, 20f),
                Terminal("2", "2/T1", 20f, 75f),
                Terminal("3", "3/L2", 50f, 20f),
                Terminal("4", "4/T2", 50f, 75f),
                Terminal("5", "5/L3", 80f, 20f),
                Terminal("6", "6/T3", 80f, 75f)
            )
            ComponentType.CONTACTOR_COIL, ComponentType.TIMER_ON_DELAY_COIL, ComponentType.TIMER_OFF_DELAY_COIL -> listOf(
                Terminal("A1", "A1", 20f, 40f),
                Terminal("A2", "A2", 80f, 40f)
            )
            ComponentType.PILOT_LAMP_GREEN, ComponentType.PILOT_LAMP_RED,
            ComponentType.PILOT_LAMP_YELLOW, ComponentType.ALARM_BUZZER -> listOf(
                Terminal("X1", "X1", 20f, 40f),
                Terminal("X2", "X2", 80f, 40f)
            )
            ComponentType.PROXIMITY_SENSOR -> listOf(
                Terminal("V+", "+", 20f, 25f),
                Terminal("OUT", "OUT", 80f, 40f),
                Terminal("GND", "-", 20f, 55f)
            )
            ComponentType.MAIN_POWER_CONTACTS_3P -> listOf(
                Terminal("L1", "1", 20f, 20f),
                Terminal("T1", "2", 80f, 20f),
                Terminal("L2", "3", 20f, 40f),
                Terminal("T2", "4", 80f, 40f),
                Terminal("L3", "5", 20f, 60f),
                Terminal("T3", "6", 80f, 60f)
            )
            ComponentType.MOTOR_3PHASE -> listOf(
                // تخته کلمپ استاندارد ۶ ترمیناله موتور ۳ فاز
                Terminal("U", "U1", 20f, 25f),
                Terminal("V", "V1", 20f, 45f),
                Terminal("W", "W1", 20f, 65f),
                Terminal("U2", "U2", 80f, 25f),
                Terminal("V2", "V2", 80f, 45f),
                Terminal("W2", "W2", 80f, 65f),
                Terminal("PE", "PE", 50f, 75f)
            )
            ComponentType.MOTOR_1PHASE -> listOf(
                Terminal("L", "L", 20f, 30f),
                Terminal("N", "N", 20f, 60f)
            )
            ComponentType.CIRCUIT_BREAKER_MCB_3P, ComponentType.CIRCUIT_BREAKER_MCCB_3P -> listOf(
                Terminal("1", "1/L1", 20f, 15f),
                Terminal("3", "3/L2", 50f, 15f),
                Terminal("5", "5/L3", 80f, 15f),
                Terminal("2", "2/T1", 20f, 65f),
                Terminal("4", "4/T2", 50f, 65f),
                Terminal("6", "6/T3", 80f, 65f),
                Terminal("L1", "1/L1", 20f, 15f),
                Terminal("L2", "3/L2", 50f, 15f),
                Terminal("L3", "5/L3", 80f, 15f),
                Terminal("T1", "2/T1", 20f, 65f),
                Terminal("T2", "4/T2", 50f, 65f),
                Terminal("T3", "6/T3", 80f, 65f)
            )
            ComponentType.FUSE_3P -> listOf(
                Terminal("1", "1", 20f, 15f),
                Terminal("3", "3", 50f, 15f),
                Terminal("5", "5", 80f, 15f),
                Terminal("2", "2", 20f, 65f),
                Terminal("4", "4", 50f, 65f),
                Terminal("6", "6", 80f, 65f),
                Terminal("L1", "1", 20f, 15f),
                Terminal("L2", "3", 50f, 15f),
                Terminal("L3", "5", 80f, 15f),
                Terminal("T1", "2", 20f, 65f),
                Terminal("T2", "4", 50f, 65f),
                Terminal("T3", "6", 80f, 65f)
            )
            ComponentType.RCD_4P -> listOf(
                Terminal("1", "1/L1", 15f, 15f),
                Terminal("3", "3/L2", 38f, 15f),
                Terminal("5", "5/L3", 62f, 15f),
                Terminal("N", "N", 85f, 15f),
                Terminal("2", "2/T1", 15f, 65f),
                Terminal("4", "4/T2", 38f, 65f),
                Terminal("6", "6/T3", 62f, 65f),
                Terminal("N_OUT", "N", 85f, 65f),
                Terminal("TEST", "T", 50f, 40f),
                Terminal("L1", "1/L1", 15f, 15f),
                Terminal("L2", "3/L2", 38f, 15f),
                Terminal("L3", "5/L3", 62f, 15f),
                Terminal("T1", "2/T1", 15f, 65f),
                Terminal("T2", "4/T2", 38f, 65f),
                Terminal("T3", "6/T3", 62f, 65f)
            )
            ComponentType.CONTROL_RELAY_1CO -> listOf(
                Terminal("A1", "A1", 15f, 25f),
                Terminal("A2", "A2", 15f, 55f),
                Terminal("11", "11", 50f, 40f),
                Terminal("12", "12", 85f, 25f),
                Terminal("14", "14", 85f, 55f)
            )
            ComponentType.CONTROL_RELAY_2CO -> listOf(
                Terminal("A1", "A1", 15f, 25f),
                Terminal("A2", "A2", 15f, 55f),
                Terminal("11", "11", 50f, 25f),
                Terminal("12", "12", 85f, 15f),
                Terminal("14", "14", 85f, 32f),
                Terminal("21", "21", 50f, 55f),
                Terminal("22", "22", 85f, 48f),
                Terminal("24", "24", 85f, 65f)
            )
            ComponentType.PHASE_SEQUENCE_RELAY -> listOf(
                Terminal("L1", "L1", 20f, 15f),
                Terminal("L2", "L2", 50f, 15f),
                Terminal("L3", "L3", 80f, 15f),
                Terminal("15", "15", 30f, 65f),
                Terminal("16", "16", 55f, 65f),
                Terminal("18", "18", 80f, 65f)
            )
        }

        // در صورت چرخش، پایانه‌ها متناسب با زاویه در کادر ۱۰۰x۸۰ چرخش می‌یابند
        if (rotation == 0f) return baseList
        val cx = 50f
        val cy = 40f
        val rad = Math.toRadians(rotation.toDouble())
        val cos = Math.cos(rad).toFloat()
        val sin = Math.sin(rad).toFloat()

        return baseList.map { term ->
            val dx = term.relX - cx
            val dy = term.relY - cy
            val nx = cx + (dx * cos - dy * sin)
            val ny = cy + (dx * sin + dy * cos)
            term.copy(relX = nx, relY = ny)
        }
    }
}

@Serializable
enum class MotorDirection {
    STOPPED,
    CLOCKWISE,       // راستگرد CW
    COUNTER_CLOCKWISE // چپگرد CCW
}

@Serializable
data class WirePoint(
    val x: Float,
    val y: Float
)

@Serializable
data class WireConnection(
    val id: String,
    val fromComponentId: String,
    val fromTerminalId: String,
    val toComponentId: String,
    val toTerminalId: String,
    val isEnergized: Boolean = false,
    val wireColorHex: String = "#00E5FF",
    val wireNumber: String = "",
    val wireColorPreset: String = "MANUAL",
    val colorStandardRef: String = "",
    val crossSectionMm2: Double = 1.5,
    val waypoints: List<WirePoint> = emptyList(),
    val wireLengthMm: Double = 0.0,
    val sheetId: String = "sheet_1",
    val layerId: String = "layer_control"
)

@Serializable
data class CircuitSheet(
    val id: String = "sheet_1",
    val name: String = "برگه ۱",
    val order: Int = 0
)

@Serializable
data class CircuitLayer(
    val id: String,
    val name: String,
    val isVisible: Boolean = true,
    val isLocked: Boolean = false,
    val colorHex: String = "#00E5FF"
)

@Serializable
data class CircuitProject(
    val id: String,
    val title: String,
    val description: String,
    val components: List<CircuitComponent>,
    val wires: List<WireConnection>,
    val ladderRungs: List<LadderRung> = emptyList(),
    val sheets: List<CircuitSheet> = listOf(CircuitSheet("sheet_1", "برگه ۱", 0)),
    val layers: List<CircuitLayer> = listOf(
        CircuitLayer("layer_power", "قدرت", isVisible = true, isLocked = false, colorHex = "#FF5722"),
        CircuitLayer("layer_control", "فرمان", isVisible = true, isLocked = false, colorHex = "#00E5FF"),
        CircuitLayer("layer_notes", "یادداشت", isVisible = true, isLocked = false, colorHex = "#FFD600")
    ),
    val designerName: String = "مهندس برق",
    val createdAt: Long = System.currentTimeMillis()
)
