package com.example.util

import com.example.model.CircuitComponent
import com.example.model.WireConnection
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * مدل نمایانگر یک نِت (Net) الکتریکی
 * نِت: مجموعه‌ای از ترمینال‌های متصل به هم صرفاً از طریق سیم‌ها (بدون عبور از داخل بدنه قطعات)
 */
data class CircuitNet(
    val netNumber: Int,
    val terminals: Set<String>, // مجموعه "componentId:terminalId"
    val wireIds: Set<String>
)

/**
 * گزینه‌های استاندارد رنگ سیم بر اساس استانداردهای بین‌المللی IEC
 */
data class WireColorPresetOption(
    val id: String,
    val nameFa: String,
    val colorHex: String,
    val standardCode: String,
    val standardClause: String, // ذکر دقیق بند استاندارد
    val roleDescription: String
)

/**
 * آیتم لیست کابل و اتصالات سیم‌کشی
 */
data class CableListItem(
    val wireId: String,
    val wireNumber: String,
    val fromComponentLabel: String,
    val fromTerminalId: String,
    val toComponentLabel: String,
    val toTerminalId: String,
    val colorHex: String,
    val colorPresetName: String,
    val standardClause: String,
    val crossSectionMm2: Double,
    val lengthMm: Double
)

object NetNumberingManager {

    /**
     * فهرست استاندارد سطوح مقطع سیم بر اساس استاندارد IEC 60228
     * (ظرفیت جریان‌دهی هیچ‌جا حدس زده نمی‌شود)
     */
    val IEC_60228_CROSS_SECTIONS: List<Double> = listOf(
        0.5, 0.75, 1.0, 1.5, 2.5, 4.0, 6.0, 10.0,
        16.0, 25.0, 35.0, 50.0, 70.0, 95.0, 120.0,
        150.0, 185.0, 240.0, 300.0
    )

    /**
     * پریست‌های رنگ استاندارد سیم
     * با استناد دقیق به بندهای استاندارد IEC و علامت‌گذاری صریح موارد نامشخص
     */
    val COLOR_PRESETS: List<WireColorPresetOption> = listOf(
        // ۱. استاندارد IEC 60445 (تأسیسات الکتریکی ساختمان و تابلوهای توزیع)
        WireColorPresetOption(
            id = "IEC_60445_L1",
            nameFa = "فاز ۱ (L1) - قهوه‌ای",
            colorHex = "#8B4513",
            standardCode = "IEC 60445",
            standardClause = "بند 6.2.2",
            roleDescription = "هادی خط فاز اول در سیستم سه‌فاز"
        ),
        WireColorPresetOption(
            id = "IEC_60445_L2",
            nameFa = "فاز ۲ (L2) - سیاه",
            colorHex = "#212121",
            standardCode = "IEC 60445",
            standardClause = "بند 6.2.2",
            roleDescription = "هادی خط فاز دوم در سیستم سه‌فاز"
        ),
        WireColorPresetOption(
            id = "IEC_60445_L3",
            nameFa = "فاز ۳ (L3) - خاکستری",
            colorHex = "#757575",
            standardCode = "IEC 60445",
            standardClause = "بند 6.2.2",
            roleDescription = "هادی خط فاز سوم در سیستم سه‌فاز"
        ),
        WireColorPresetOption(
            id = "IEC_60445_N",
            nameFa = "نول (N) - آبی روشن",
            colorHex = "#00B0FF",
            standardCode = "IEC 60445",
            standardClause = "بند 6.2.2",
            roleDescription = "هادی خنثی / نول"
        ),
        WireColorPresetOption(
            id = "IEC_60445_PE",
            nameFa = "ارت حفاظتی (PE) - سبز/زرد",
            colorHex = "#76FF03",
            standardCode = "IEC 60445",
            standardClause = "بند 6.3.2",
            roleDescription = "هادی حفاظتی زمین (Green/Yellow)"
        ),

        // ۲. استاندارد IEC 60204-1 (ایمنی تجهیزات الکتریکی ماشین‌آلات صنعتی)
        WireColorPresetOption(
            id = "IEC_60204_POWER",
            nameFa = "مدار قدرت (Power) - سیاه",
            colorHex = "#212121",
            standardCode = "IEC 60204-1",
            standardClause = "بند 13.2.4",
            roleDescription = "مدارهای اصلی قدرت AC و DC ماشین‌آلات"
        ),
        WireColorPresetOption(
            id = "IEC_60204_CTRL_AC",
            nameFa = "مدار فرمان AC - قرمز",
            colorHex = "#D50000",
            standardCode = "IEC 60204-1",
            standardClause = "بند 13.2.4",
            roleDescription = "مدارهای کنترل و فرمان جریان متناوب (AC)"
        ),
        WireColorPresetOption(
            id = "IEC_60204_CTRL_DC",
            nameFa = "مدار فرمان DC - آبی",
            colorHex = "#2979FF",
            standardCode = "IEC 60204-1",
            standardClause = "بند 13.2.4",
            roleDescription = "مدارهای کنترل و فرمان جریان مستقیم (DC)"
        ),
        WireColorPresetOption(
            id = "IEC_60204_PE",
            nameFa = "هادی حفاظتی (PE) - سبز/زرد",
            colorHex = "#76FF03",
            standardCode = "IEC 60204-1",
            standardClause = "بند 13.2.4.2",
            roleDescription = "هادی همبندی حفاظتی ماشین"
        ),
        WireColorPresetOption(
            id = "IEC_60204_INTERLOCKED",
            nameFa = "مدار با منبع خارجی - نارنجی",
            colorHex = "#FF6D00",
            standardCode = "IEC 60204-1",
            standardClause = "بند 13.2.4",
            roleDescription = "مدارهایی که حتی با قطع کلید اصلی ماشین برق‌دار می‌مانند"
        ),

        // ۳. حالت دستی (MANUAL)
        WireColorPresetOption(
            id = "MANUAL_CYAN",
            nameFa = "دستی - فیروزه‌ای شبیه‌ساز",
            colorHex = "#00E5FF",
            standardCode = "دستی",
            standardClause = "نامشخص",
            roleDescription = "رنگ پیش‌فرض شبیه‌ساز مدار"
        ),
        WireColorPresetOption(
            id = "MANUAL_YELLOW",
            nameFa = "دستی - زرد",
            colorHex = "#FFD600",
            standardCode = "دستی",
            standardClause = "نامشخص",
            roleDescription = "سیم‌کشی سفارشی"
        ),
        WireColorPresetOption(
            id = "MANUAL_WHITE",
            nameFa = "دستی - سفید",
            colorHex = "#FFFFFF",
            standardCode = "دستی",
            standardClause = "نامشخص",
            roleDescription = "سیم‌کشی سفارشی"
        ),
        WireColorPresetOption(
            id = "MANUAL_GREEN",
            nameFa = "دستی - سبز",
            colorHex = "#00E676",
            standardCode = "دستی",
            standardClause = "نامشخص",
            roleDescription = "سیم‌کشی سفارشی"
        )
    )

    /**
     * پیدا کردن پریست رنگ بر اساس شناسه یا کد هگز
     */
    fun findPresetByHex(hex: String): WireColorPresetOption? {
        val normalized = hex.trim().uppercase()
        return COLOR_PRESETS.find { it.colorHex.equals(normalized, ignoreCase = true) }
    }

    fun findPresetById(id: String): WireColorPresetOption? {
        return COLOR_PRESETS.find { it.id == id }
    }

    /**
     * محاسبه نِت‌ها با الگوریتم Union-Find بدون عبور از داخل بدنه قطعات
     * پایداری شماره‌ها:
     * - نتی که قبلاً شماره داشته همان را نگه می‌دارد.
     * - ادغام دو نت -> شماره کوچکتر باقی می‌ماند.
     * - نت جدید -> شماره بعدی آزاد (کوچکترین عدد مثبت آزاد) را می‌گیرد.
     */
    fun computeNetsWithStableNumbering(
        wires: List<WireConnection>,
        existingNets: List<CircuitNet> = emptyList()
    ): List<CircuitNet> {
        if (wires.isEmpty()) return emptyList()

        // ۱. ساخت گراف اتصالات ترمینال‌ها
        val parent = mutableMapOf<String, String>()

        fun find(node: String): String {
            var root = node
            while (parent.getOrDefault(root, root) != root) {
                root = parent.getOrDefault(root, root)
            }
            // فشرده‌سازی مسیر (Path Compression)
            var curr = node
            while (curr != root) {
                val next = parent.getOrDefault(curr, curr)
                parent[curr] = root
                curr = next
            }
            return root
        }

        fun union(n1: String, n2: String) {
            val r1 = find(n1)
            val r2 = find(n2)
            if (r1 != r2) {
                parent[r1] = r2
            }
        }

        // مقداردهی اولیه همه پایانه‌های سیم‌ها
        for (wire in wires) {
            val t1 = "${wire.fromComponentId}:${wire.fromTerminalId}"
            val t2 = "${wire.toComponentId}:${wire.toTerminalId}"
            parent.putIfAbsent(t1, t1)
            parent.putIfAbsent(t2, t2)
            union(t1, t2)
        }

        // ۲. گروه‌بندی پایانه‌ها و سیم‌ها در کامپوننت‌های همبند (Netها)
        val rootToTerminals = mutableMapOf<String, MutableSet<String>>()
        for (t in parent.keys) {
            val root = find(t)
            rootToTerminals.getOrPut(root) { mutableSetOf() }.add(t)
        }

        val netGroups = rootToTerminals.values.toList()

        // ۳. نگاشت سیم‌ها به هر گروه
        val groupWireMap = netGroups.map { termGroup ->
            val wIds = wires.filter { wire ->
                val t1 = "${wire.fromComponentId}:${wire.fromTerminalId}"
                val t2 = "${wire.toComponentId}:${wire.toTerminalId}"
                termGroup.contains(t1) || termGroup.contains(t2)
            }.map { it.id }.toSet()
            Pair(termGroup, wIds)
        }

        // ۴. پایداری شماره‌ها (Stability)
        val allocatedNumbers = mutableSetOf<Int>()
        val result = mutableListOf<CircuitNet>()

        // ساخت نگاشت سریع برای ترمینال‌های نت‌های قبلی
        val terminalToOldNetNumber = mutableMapOf<String, Int>()
        for (oldNet in existingNets) {
            for (term in oldNet.terminals) {
                terminalToOldNetNumber[term] = oldNet.netNumber
            }
        }

        // گروه‌هایی که سابقه دارند
        val unassignedGroups = mutableListOf<Pair<MutableSet<String>, Set<String>>>()

        for (pair in groupWireMap) {
            val termGroup = pair.first
            val wIds = pair.second

            // بررسی شماره‌های قبلی متقاطع با این نت
            val matchedOldNumbers = termGroup.mapNotNull { terminalToOldNetNumber[it] }.distinct()

            if (matchedOldNumbers.isNotEmpty()) {
                // اگر دو یا چند نت ادغام شده باشند، شماره‌ی کوچکتر باقی می‌ماند
                val minOldNumber = matchedOldNumbers.minOrNull() ?: 1
                if (!allocatedNumbers.contains(minOldNumber)) {
                    allocatedNumbers.add(minOldNumber)
                    result.add(CircuitNet(minOldNumber, termGroup, wIds))
                } else {
                    // شماره قبلاً توسط گروه دیگری برداشته شده، باید شماره جدید بگیرد
                    unassignedGroups.add(pair)
                }
            } else {
                unassignedGroups.add(pair)
            }
        }

        // ۵. تخصیص شماره‌های آزاد بعدی به گروه‌های جدید
        var nextFreeNumber = 1
        for (pair in unassignedGroups) {
            while (allocatedNumbers.contains(nextFreeNumber)) {
                nextFreeNumber++
            }
            allocatedNumbers.add(nextFreeNumber)
            result.add(CircuitNet(nextFreeNumber, pair.first, pair.second))
            nextFreeNumber++
        }

        return result.sortedBy { it.netNumber }
    }

    /**
     * تطبیق و تزریق شماره نت پایدار به سیم‌ها
     */
    fun assignNumbersToWires(
        wires: List<WireConnection>,
        nets: List<CircuitNet>
    ): List<WireConnection> {
        val wireToNetNumber = mutableMapOf<String, Int>()
        for (net in nets) {
            for (wId in net.wireIds) {
                wireToNetNumber[wId] = net.netNumber
            }
        }

        return wires.map { wire ->
            val netNum = wireToNetNumber[wire.id]
            val finalWireNum = if (wire.wireNumber.isNotBlank()) {
                wire.wireNumber
            } else if (netNum != null) {
                netNum.toString()
            } else {
                ""
            }
            wire.copy(wireNumber = finalWireNum)
        }
    }

    /**
     * ساخت لیست کابل جهت نمایش در جدول و استخراج CSV
     */
    fun buildCableList(
        wires: List<WireConnection>,
        components: List<CircuitComponent>
    ): List<CableListItem> {
        val compMap = components.associateBy { it.id }

        return wires.map { wire ->
            val fromComp = compMap[wire.fromComponentId]
            val toComp = compMap[wire.toComponentId]

            val fromLabel = fromComp?.label ?: wire.fromComponentId
            val toLabel = toComp?.label ?: wire.toComponentId

            val preset = findPresetByHex(wire.wireColorHex)
            val presetName = preset?.nameFa ?: "سفارشی (${wire.wireColorPreset})"
            val clause = if (wire.colorStandardRef.isNotBlank()) {
                wire.colorStandardRef
            } else {
                preset?.standardClause ?: "نامشخص"
            }

            // محاسبه طول تقریبی بر اساس نقاط عطف و پایانه‌ها
            val lengthMm = if (wire.wireLengthMm > 0.0) {
                wire.wireLengthMm
            } else {
                calculateEstimatedLengthMm(wire, fromComp, toComp)
            }

            CableListItem(
                wireId = wire.id,
                wireNumber = wire.wireNumber.ifBlank { "-" },
                fromComponentLabel = fromLabel,
                fromTerminalId = wire.fromTerminalId,
                toComponentLabel = toLabel,
                toTerminalId = wire.toTerminalId,
                colorHex = wire.wireColorHex,
                colorPresetName = presetName,
                standardClause = clause,
                crossSectionMm2 = wire.crossSectionMm2,
                lengthMm = (lengthMm * 10.0).roundToInt() / 10.0
            )
        }
    }

    private fun calculateEstimatedLengthMm(
        wire: WireConnection,
        fromComp: CircuitComponent?,
        toComp: CircuitComponent?
    ): Double {
        if (fromComp == null || toComp == null) return 0.0

        val fromTerm = fromComp.getTerminals().find { it.id == wire.fromTerminalId }
        val toTerm = toComp.getTerminals().find { it.id == wire.toTerminalId }

        val startX = fromComp.x + (fromTerm?.relX ?: 0f)
        val startY = fromComp.y + (fromTerm?.relY ?: 0f)
        val endX = toComp.x + (toTerm?.relX ?: 0f)
        val endY = toComp.y + (toTerm?.relY ?: 0f)

        if (wire.waypoints.isNotEmpty()) {
            var totalDist = 0.0
            var prevX = startX
            var prevY = startY
            for (wp in wire.waypoints) {
                val dx = (wp.x - prevX).toDouble()
                val dy = (wp.y - prevY).toDouble()
                totalDist += sqrt(dx * dx + dy * dy)
                prevX = wp.x
                prevY = wp.y
            }
            val dx = (endX - prevX).toDouble()
            val dy = (endY - prevY).toDouble()
            totalDist += sqrt(dx * dx + dy * dy)
            // مقیاس تقریبی: هر واحد بوم = ۲ میلی‌متر در تابلو صنعتی
            return totalDist * 2.0
        } else {
            // مسیر منهتن اورتوگونال
            val manhattanDist = abs(endX - startX) + abs(endY - startY)
            return manhattanDist.toDouble() * 2.0
        }
    }

    /**
     * تولید خروجی استاندارد CSV از جدول کابل‌ها
     */
    fun exportCableListToCsv(items: List<CableListItem>): String {
        val sb = StringBuilder()
        // هدر جدول با UTF-8 BOM برای باز شدن صحیح در اکسل
        sb.append('\uFEFF')
        sb.append("شماره سیم,مبدأ,ترمینال مبدأ,مقصد,ترمینال مقصد,رنگ سیم,کد رنگ,سطح مقطع (mm²),بند استاندارد,طول تقریبی (mm)\n")

        for (item in items) {
            sb.append("\"${item.wireNumber}\",")
            sb.append("\"${item.fromComponentLabel}\",")
            sb.append("\"${item.fromTerminalId}\",")
            sb.append("\"${item.toComponentLabel}\",")
            sb.append("\"${item.toTerminalId}\",")
            sb.append("\"${item.colorPresetName}\",")
            sb.append("\"${item.colorHex}\",")
            sb.append("${item.crossSectionMm2},")
            sb.append("\"${item.standardClause}\",")
            sb.append("${item.lengthMm}\n")
        }
        return sb.toString()
    }
}
