package com.example.model

import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

/**
 * انواع المان‌ها و دستورات لدر PLC مطابق با استاندارد IEC 61131-3
 */
@Serializable
enum class LadderElementType(val persianTitle: String, val symbol: String) {
    // کنتاکت‌های بولی
    NO_CONTACT("کنتاکت باز (Normally Open NO)", "-[ ]-"),
    NC_CONTACT("کنتاکت بسته (Normally Closed NC)", "-[/]-"),
    EDGE_POS("کنتاکت لبه بالارونده (Positive Edge P / R_TRIG)", "-[P]-"),
    EDGE_NEG("کنتاکت لبه پایین‌رونده (Negative Edge N / F_TRIG)", "-[N]-"),

    // کویل‌های خروجی
    NORMAL_COIL("کویل / بوبین خروجی (Output Coil)", "-( )-"),
    SET_COIL("کویل ست (Set Coil S)", "-(S)-"),
    RESET_COIL("کویل ریست (Reset Coil R)", "-(R)-"),

    // تایمرها
    TIMER_TON("تایمر تاخیر در وصل (Timer ON-Delay TON)", "[TON]"),
    TIMER_TOF("تایمر تاخیر در قطع (Timer OFF-Delay TOF)", "[TOF]"),

    // کانترها
    COUNTER_CTU("کانتر / شمارنده صعودی (Up Counter CTU)", "[CTU]"),
    COUNTER_CTD("کانتر / شمارنده نزولی (Down Counter CTD)", "[CTD]"),
    COUNTER_CTUD("کانتر صعودی/نزولی دوطرفه (Up/Down Counter CTUD)", "[CTUD]"),

    // دستورات مقایسه‌ای
    CMP_EQ("مقایسه برابر (Equal EQ ==)", "[==]"),
    CMP_NE("مقایسه نابرابر (Not Equal NE <>)", "[<>]"),
    CMP_GT("مقایسه بزرگتر (Greater Than GT >)", "[>]"),
    CMP_GE("مقایسه بزرگتر مساوی (Greater/Equal GE >=)", "[>=]"),
    CMP_LT("مقایسه کوچکتر (Less Than LT <)", "[<]"),
    CMP_LE("مقایسه کوچکتر مساوی (Less/Equal LE <=)", "[<=]"),

    // دستورات ریاضی
    MATH_ADD("جمع ریاضی (Addition ADD)", "[ADD]"),
    MATH_SUB("تفریق ریاضی (Subtraction SUB)", "[SUB]"),
    MATH_MUL("ضرب ریاضی (Multiplication MUL)", "[MUL]"),
    MATH_DIV("تقسیم ریاضی (Division DIV)", "[DIV]")
}

/**
 * انواع داده استاندارد IEC 61131-3
 */
@Serializable
enum class PlcDataType(val typeName: String, val iecPrefix: String) {
    BOOL("BOOL (بیت منطقی)", "%X"),
    INT("INT (عدد صحیح ۱۶ بیتی)", "%W"),
    TIME("TIME (زمان بر حسب ثانیه)", "T#")
}

/**
 * تگ نمادین در جدول تگ‌های PLC
 */
@Serializable
data class PlcTag(
    val name: String,
    val dataType: PlcDataType,
    val address: String,
    val comment: String = "",
    val circuitBinding: String = "" // ارتباط با تگ قطعه مدار فرمان
)

/**
 * المان موجود در رانگ لدر
 */
@Serializable
data class LadderElement(
    val id: String,
    val type: LadderElementType,
    val address: String, // مثلاً I0.0, %IX0.0, T1, C1, MW0 یا نام تگ نمادین
    val comment: String = "",
    val presetValue: Int = 5, // مقدار تنظیمی زمان (ثانیه) یا کانتر
    val currentValue: Int = 0,
    val isActive: Boolean = false,
    val operand2: String = "", // عملوند دوم دستورهای مقایسه یا ریاضی (عدد ثابت مثل "10" یا آدرس مثل "MW1")
    val destinationAddress: String = "" // آدرس ذخیره نتیجه دستورهای ریاضی مثل "MW2"
)

/**
 * گره‌های درخت شبکه منطقی لدر (Series/Parallel Network Tree)
 */
@Serializable
sealed interface LadderNode {
    @Serializable
    data class Leaf(val element: LadderElement) : LadderNode

    @Serializable
    data class Series(val children: List<LadderNode>) : LadderNode

    @Serializable
    data class Parallel(val children: List<LadderNode>) : LadderNode
}

/**
 * شاخه موازی قدیمی (جهت حفظ سازگاری ۱۰۰٪ با کدهای پیشین)
 */
@Serializable
data class LadderBranch(
    val id: String,
    val contacts: List<LadderElement> = emptyList()
)

/**
 * رانگ کامل نردبان لدر
 */
@Serializable
data class LadderRung(
    val id: String,
    val rungNumber: Int,
    val comment: String = "",
    val contacts: List<LadderElement> = emptyList(),
    val branchContacts: List<LadderElement> = emptyList(), // شاخه موازی خودنگهدار اول
    val parallelBranches: List<LadderBranch> = emptyList(), // شاخه‌های موازی عمومی OR
    val coils: List<LadderElement> = emptyList(),
    val isPowerFlowing: Boolean = false,
    val networkTree: LadderNode? = null // درخت شبکه سری/موازی تعمیم‌یافته
) {
    /**
     * تبدیل خودکار ساختار رانگ به درخت سری/موازی با حفظ دقیق رفتار کدهای قبلی (رگرسیون کامل)
     */
    fun toNetworkTree(): LadderNode {
        if (networkTree != null) return networkTree

        val ncLeaves = contacts.filter { it.type == LadderElementType.NC_CONTACT }.map { LadderNode.Leaf(it) }
        val noLeaves = contacts.filter { it.type != LadderElementType.NC_CONTACT }.map { LadderNode.Leaf(it) }

        val parallelBranchesList = mutableListOf<LadderNode>()
        if (noLeaves.isNotEmpty()) {
            parallelBranchesList.add(if (noLeaves.size == 1) noLeaves.first() else LadderNode.Series(noLeaves))
        }
        if (branchContacts.isNotEmpty()) {
            val bLeaves = branchContacts.map { LadderNode.Leaf(it) }
            parallelBranchesList.add(if (bLeaves.size == 1) bLeaves.first() else LadderNode.Series(bLeaves))
        }
        parallelBranches.forEach { branch ->
            if (branch.contacts.isNotEmpty()) {
                val pLeaves = branch.contacts.map { LadderNode.Leaf(it) }
                parallelBranchesList.add(if (pLeaves.size == 1) pLeaves.first() else LadderNode.Series(pLeaves))
            }
        }

        val orNode: LadderNode? = when {
            parallelBranchesList.isEmpty() -> null
            parallelBranchesList.size == 1 -> parallelBranchesList.first()
            else -> LadderNode.Parallel(parallelBranchesList)
        }

        val seriesList = mutableListOf<LadderNode>()
        seriesList.addAll(ncLeaves)
        if (orNode != null) {
            seriesList.add(orNode)
        }

        return when {
            seriesList.isEmpty() -> LadderNode.Series(emptyList())
            seriesList.size == 1 -> seriesList.first()
            else -> LadderNode.Series(seriesList)
        }
    }
}

/**
 * حافظه شبیه‌ساز PLC (شامل ورودی، خروجی، مارکر، مقادیر عددی ۱۶ بیتی و جدول تگ‌ها)
 */
@Serializable
data class PlcMemory(
    val inputs: Map<String, Boolean> = emptyMap(),
    val outputs: Map<String, Boolean> = emptyMap(),
    val markers: Map<String, Boolean> = emptyMap(),
    val intMemory: Map<String, Int> = emptyMap(), // حافظه عددی ۱۶ بیتی (MW0, MW1, ...)
    val timers: Map<String, Float> = emptyMap(), // زمان سپری شده به ثانیه (Float)
    val timerDone: Map<String, Boolean> = emptyMap(),
    val counters: Map<String, Int> = emptyMap(),
    val counterDone: Map<String, Boolean> = emptyMap(),
    val prevRungConditions: Map<String, Boolean> = emptyMap(), // وضعیت قبلی رانگ‌ها
    val prevContactStates: Map<String, Boolean> = emptyMap(), // وضعیت قبلی کنتاکت‌ها برای تشخیص لبه P و N
    val tags: List<PlcTag> = emptyList() // جدول تگ‌های برنامه
) {
    fun setInput(address: String, value: Boolean): PlcMemory {
        val norm = PlcAddressUtils.normalize(address)
        val next = inputs.toMutableMap()
        next[norm] = value
        return copy(inputs = next)
    }

    fun toggleInput(address: String): PlcMemory {
        val norm = PlcAddressUtils.normalize(address)
        val next = inputs.toMutableMap()
        val cur = next[norm] ?: false
        next[norm] = !cur
        return copy(inputs = next)
    }

    /**
     * پیدا کردن آدرس فیزیکی متناظر با یک نام تگ یا آدرس مستقیم
     */
    fun resolveAddress(nameOrAddress: String): String {
        val trimmed = nameOrAddress.trim()
        val tag = tags.find { it.name.equals(trimmed, ignoreCase = true) }
        return if (tag != null) PlcAddressUtils.normalize(tag.address) else PlcAddressUtils.normalize(trimmed)
    }

    /**
     * خواندن سیگنال بولی از حافظه با پشتیبانی از نام تگ و آدرس‌های IEC/زیمنس
     */
    fun readAddress(addressOrTag: String): Boolean {
        val addr = resolveAddress(addressOrTag)
        return when {
            addr.startsWith("I") -> inputs[addr] ?: false
            addr.startsWith("Q") -> outputs[addr] ?: false
            addr.startsWith("M") && !addr.startsWith("MW") -> markers[addr] ?: false
            addr.startsWith("T") -> timerDone[addr] ?: false
            addr.startsWith("C") -> counterDone[addr] ?: false
            else -> false
        }
    }

    /**
     * خواندن مقدار عددی ۱۶ بیتی از حافظه (ثابت، تایمر، کانتر یا رجیستر MW)
     */
    fun readInt(operand: String): Int {
        val trimmed = operand.trim()
        trimmed.toIntOrNull()?.let { return it }

        val addr = resolveAddress(trimmed)
        return when {
            addr.startsWith("MW") -> intMemory[addr] ?: 0
            addr.startsWith("C") -> counters[addr] ?: 0
            addr.startsWith("T") -> (timers[addr] ?: 0f).roundToInt()
            else -> intMemory[addr] ?: 0
        }
    }

    /**
     * نوشتن مقدار عددی ۱۶ بیتی در حافظه با محافظت در برابر سرریز (-32768 تا 32767)
     */
    fun writeInt(addressOrTag: String, value: Int): PlcMemory {
        val addr = resolveAddress(addressOrTag)
        val next = intMemory.toMutableMap()
        val clamped = value.coerceIn(-32768, 32767)
        next[addr] = clamped
        return copy(intMemory = next)
    }
}

/**
 * ابزارهای کمکی نرمال‌سازی و تبدیل آدرس‌های استاندارد IEC 61131-3 و نمادگذاری زیمنس
 */
object PlcAddressUtils {
    /**
     * نرمال‌سازی هر فرمت آدرس ورودی به فرمت استاندارد داخلی (مثلاً %IX0.0 -> I0.0, %QX0.0 -> Q0.0, %MW0 -> MW0)
     */
    fun normalize(raw: String): String {
        val s = raw.trim().uppercase()
        return when {
            s.startsWith("%IX") -> "I" + s.removePrefix("%IX")
            s.startsWith("%I") && !s.startsWith("%IX") -> "I" + s.removePrefix("%I")
            s.startsWith("%QX") -> "Q" + s.removePrefix("%QX")
            s.startsWith("%Q") && !s.startsWith("%QX") -> "Q" + s.removePrefix("%Q")
            s.startsWith("%MX") -> "M" + s.removePrefix("%MX")
            s.startsWith("%MW") -> "MW" + s.removePrefix("%MW")
            s.startsWith("%M") && !s.startsWith("%MX") && !s.startsWith("%MW") -> "M" + s.removePrefix("%M")
            else -> s
        }
    }

    /**
     * تبدیل آدرس به فرمت نمایش IEC 61131-3
     */
    fun toIec(address: String): String {
        val norm = normalize(address)
        return when {
            norm.startsWith("I") -> "%IX" + norm.removePrefix("I")
            norm.startsWith("Q") -> "%QX" + norm.removePrefix("Q")
            norm.startsWith("MW") -> "%MW" + norm.removePrefix("MW")
            norm.startsWith("M") -> "%MX" + norm.removePrefix("M")
            else -> norm
        }
    }

    /**
     * اعتبارسنجی ساختار آدرس
     */
    fun isValidAddress(raw: String): Boolean {
        val n = normalize(raw)
        return n.matches(Regex("""^(I|Q|M)\d+\.\d+$""")) ||
                n.matches(Regex("""^(MW|T|C)\d+$"""))
    }

    /**
     * اعتبارسنجی جدول تگ‌ها (بررسی نام‌های تکراری و آدرس‌های تکراری)
     */
    fun validateTags(tags: List<PlcTag>): Pair<Boolean, String> {
        val names = mutableSetOf<String>()
        val addresses = mutableSetOf<String>()

        for (tag in tags) {
            if (tag.name.isBlank()) {
                return Pair(false, "نام نمادین تگ نمی‌تواند خالی باشد.")
            }
            if (!names.add(tag.name.lowercase())) {
                return Pair(false, "نام تگ تکراری است: «${tag.name}»")
            }
            val normAddr = normalize(tag.address)
            if (!isValidAddress(normAddr)) {
                return Pair(false, "فرمت آدرس نامعتبر است: «${tag.address}»")
            }
            if (!addresses.add(normAddr)) {
                return Pair(false, "آدرس تگ تکراری است: «${tag.address}»")
            }
        }
        return Pair(true, "")
    }
}

/**
 * پکیج کامل ذخیره و بازیابی برنامه لدر و جدول تگ‌ها جهت Import/Export
 */
@Serializable
data class PlcLadderProgramPackage(
    val schemaVersion: Int = 2,
    val title: String = "برنامه لدر PLC",
    val description: String = "",
    val rungs: List<LadderRung>,
    val tags: List<PlcTag> = emptyList()
)
