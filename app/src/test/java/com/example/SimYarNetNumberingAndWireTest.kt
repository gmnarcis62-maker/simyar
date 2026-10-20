package com.example

import androidx.compose.ui.geometry.Offset
import com.example.canvas.WireRouter
import com.example.model.CircuitComponent
import com.example.model.ComponentType
import com.example.model.WireConnection
import com.example.util.CableListItem
import com.example.util.CircuitNet
import com.example.util.NetNumberingManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * تست‌های واحد بندهای مرحله P3:
 * ۱. نِت‌ها، Union-Find و عدم عبور از داخل قطعات
 * ۲. پایداری شماره‌ها و تخصیص شماره بعدی آزاد
 * ۳. ادغام دو نِت و حفظ شماره کوچکتر
 * ۴. جدا شدن نِت و تخصیص شماره به بخش جدید
 * ۵. مسیریابی خودکار A* با دور زدن موانع قطعات
 * ۶. اعتبارسنجی پریست‌های IEC 60445 / IEC 60204-1 و مقاطع IEC 60228
 * ۷. لیست کابل و خروجی CSV
 */
class SimYarNetNumberingAndWireTest {

    // ۱. تست Union-Find و عدم اتصال پایانه‌ها از داخل قطعات (فقط سیم‌ها اتصال می‌سازند)
    @Test
    fun test01_NetUnionFind_OnlyConnectsViaWires_NeverThroughComponents() {
        val wires = listOf(
            WireConnection(
                id = "w1",
                fromComponentId = "km1",
                fromTerminalId = "13",
                toComponentId = "sb1",
                toTerminalId = "14"
            ),
            WireConnection(
                id = "w2",
                fromComponentId = "sb1",
                fromTerminalId = "13",
                toComponentId = "q1",
                toTerminalId = "2"
            )
        )

        val nets = NetNumberingManager.computeNetsWithStableNumbering(wires)
        assertEquals("باید دو نِت مجزا تشکیل شود چون ترمینال‌های 13 و 14 قطعه sb1 از داخل قطعه به هم متصل نیستند", 2, nets.size)

        val net1 = nets.find { it.terminals.contains("km1:13") }
        val net2 = nets.find { it.terminals.contains("q1:2") }

        assertNotNull(net1)
        assertNotNull(net2)
        assertTrue("نِت ۱ باید شامل sb1:14 باشد", net1!!.terminals.contains("sb1:14"))
        assertFalse("نِت ۱ نباید شامل sb1:13 باشد زیرا سیم از داخل قطعه رد نمی‌شود", net1.terminals.contains("sb1:13"))
        assertTrue("نِت ۲ باید شامل sb1:13 باشد", net2!!.terminals.contains("sb1:13"))
    }

    // ۲. تست پایداری شماره نِت‌ها با تغییر مدار
    @Test
    fun test02_StableNetNumbering_PreservesNumbersAndAssignsNextFree() {
        val initialWires = listOf(
            WireConnection(id = "w1", fromComponentId = "c1", fromTerminalId = "t1", toComponentId = "c2", toTerminalId = "t2")
        )
        val initialNets = NetNumberingManager.computeNetsWithStableNumbering(initialWires)
        assertEquals(1, initialNets.size)
        val netNumber1 = initialNets.first().netNumber

        // اضافه شدن سیم جدید در مدار مستقل دیگر
        val newWires = initialWires + WireConnection(
            id = "w2", fromComponentId = "c3", fromTerminalId = "t3", toComponentId = "c4", toTerminalId = "t4"
        )
        val updatedNets = NetNumberingManager.computeNetsWithStableNumbering(newWires, initialNets)
        assertEquals(2, updatedNets.size)

        val existingNet = updatedNets.find { it.terminals.contains("c1:t1") }
        val newNet = updatedNets.find { it.terminals.contains("c3:t3") }

        assertNotNull(existingNet)
        assertNotNull(newNet)
        assertEquals("نِت قبلی باید شماره پایدار خود را حفظ کند", netNumber1, existingNet!!.netNumber)
        assertTrue("نِت جدید باید شماره آزاد بعدی را دریافت کند", newNet!!.netNumber > 0 && newNet.netNumber != netNumber1)
    }

    // ۳. تست ادغام دو نِت و حفظ شماره‌ی کوچکتر
    @Test
    fun test03_MergeNets_KeepsSmallerNumber() {
        // نِت A با شماره ۲ و نِت B با شماره ۵
        val netA = CircuitNet(netNumber = 2, terminals = setOf("a1:t1", "a2:t2"), wireIds = setOf("w_a"))
        val netB = CircuitNet(netNumber = 5, terminals = setOf("b1:t1", "b2:t2"), wireIds = setOf("w_b"))
        val existingNets = listOf(netA, netB)

        // سیم ادغام‌کننده دو نِت بین a2 و b1
        val mergedWires = listOf(
            WireConnection(id = "w_a", fromComponentId = "a1", fromTerminalId = "t1", toComponentId = "a2", toTerminalId = "t2"),
            WireConnection(id = "w_b", fromComponentId = "b1", fromTerminalId = "t1", toComponentId = "b2", toTerminalId = "t2"),
            WireConnection(id = "w_merge", fromComponentId = "a2", fromTerminalId = "t2", toComponentId = "b1", toTerminalId = "t1")
        )

        val resultNets = NetNumberingManager.computeNetsWithStableNumbering(mergedWires, existingNets)
        assertEquals("پس از ادغام باید یک نِت واحد تشکیل شود", 1, resultNets.size)
        assertEquals("هنگام ادغام دو نت، باید شماره‌ی کوچکتر (۲) باقی بماند", 2, resultNets.first().netNumber)
    }

    // ۴. تست تفکیک نِت و تخصیص شماره به بخش جداشده
    @Test
    fun test04_SplitNets_AllocatesNewNumberForDisconnectedPart() {
        val initialNet = CircuitNet(netNumber = 1, terminals = setOf("a1:t1", "a2:t2", "b1:t1", "b2:t2"), wireIds = setOf("w1", "w2", "w3"))

        // حذف سیم رابط بین a و b
        val separatedWires = listOf(
            WireConnection(id = "w1", fromComponentId = "a1", fromTerminalId = "t1", toComponentId = "a2", toTerminalId = "t2"),
            WireConnection(id = "w2", fromComponentId = "b1", fromTerminalId = "t1", toComponentId = "b2", toTerminalId = "t2")
        )

        val resultNets = NetNumberingManager.computeNetsWithStableNumbering(separatedWires, listOf(initialNet))
        assertEquals("پس از تفکیک باید ۲ نِت وجود داشته باشد", 2, resultNets.size)

        val netNumbers = resultNets.map { it.netNumber }
        assertTrue("یکی از بخش‌ها باید شماره قبلی (۱) را حفظ کند", netNumbers.contains(1))
        assertTrue("بخش دیگر باید شماره آزاد جدید بگیرد", netNumbers.any { it != 1 })
    }

    // ۵. تست مسیریابی خودکار A* با دور زدن موانع قطعات
    @Test
    fun test05_WireRouter_AvoidsObstacleComponent() {
        // قطعه مانع دقیقاً در مرکز بین مبدأ و مقصد
        val obstacle = CircuitComponent(
            id = "obs1",
            type = ComponentType.CONTACTOR_COIL,
            label = "KM1",
            x = 200f,
            y = 100f
        )
        val comps = listOf(obstacle)

        val start = Offset(100f, 140f) // سمت چپ مانع
        val end = Offset(350f, 140f)   // سمت راست مانع

        val route = WireRouter.routeWire(
            start = start,
            end = end,
            fromCompId = "comp_start",
            toCompId = "comp_end",
            components = comps
        )

        assertTrue("مسیر باید حداقل ۲ نقطه داشته باشد", route.size >= 2)
        assertEquals(start.x, route.first().x, 0.1f)
        assertEquals(start.y, route.first().y, 0.1f)
        assertEquals(end.x, route.last().x, 0.1f)
        assertEquals(end.y, route.last().y, 0.1f)

        // بررسی اینکه آیا مسیر از روی مرکز مانع رد شده یا آن را دور زده است
        val obsBoxLeft = obstacle.x - 4f
        val obsBoxRight = obstacle.x + CircuitComponent.COMPONENT_WIDTH + 4f
        val obsBoxTop = obstacle.y - 4f
        val obsBoxBottom = obstacle.y + CircuitComponent.COMPONENT_HEIGHT + 4f

        for (i in 0 until route.size - 1) {
            val p1 = route[i]
            val p2 = route[i + 1]
            // اگر هر دو سر یک پاره‌خط کاملاً درون باکس مانع باشند، خطا است
            val p1Inside = p1.x > obsBoxLeft && p1.x < obsBoxRight && p1.y > obsBoxTop && p1.y < obsBoxBottom
            val p2Inside = p2.x > obsBoxLeft && p2.x < obsBoxRight && p2.y > obsBoxTop && p2.y < obsBoxBottom
            assertFalse("مسیر نباید از داخل جعبه قطعه مانع عبور کند", p1Inside && p2Inside)
        }
    }

    // ۶. تست پریست‌های استاندارد رنگ و مقاطع IEC 60228
    @Test
    fun test06_ColorPresetsAndCrossSections_ValidateClauses() {
        val presets = NetNumberingManager.COLOR_PRESETS
        assertTrue("پریست‌های رنگ نباید خالی باشند", presets.isNotEmpty())

        val iec60445 = presets.filter { it.standardCode == "IEC 60445" }
        assertTrue("باید پریست‌های IEC 60445 موجود باشند", iec60445.isNotEmpty())
        iec60445.forEach {
            assertTrue("بند استاندارد IEC 60445 باید مشخص باشد", it.standardClause.isNotBlank() && it.standardClause != "نامشخص")
        }

        val iec60204 = presets.filter { it.standardCode == "IEC 60204-1" }
        assertTrue("باید پریست‌های IEC 60204-1 موجود باشند", iec60204.isNotEmpty())
        iec60204.forEach {
            assertTrue("بند استاندارد IEC 60204-1 باید مشخص باشد", it.standardClause.isNotBlank() && it.standardClause != "نامشخص")
        }

        val manualPresets = presets.filter { it.standardCode == "دستی" }
        assertTrue("پریست‌های دستی باید با بند نامشخص مشخص شده باشند", manualPresets.all { it.standardClause == "نامشخص" })

        // بررسی ۱۹ سطح مقطع استاندارد IEC 60228
        val crossSections = NetNumberingManager.IEC_60228_CROSS_SECTIONS
        assertEquals(19, crossSections.size)
        assertTrue(crossSections.contains(0.5))
        assertTrue(crossSections.contains(1.5))
        assertTrue(crossSections.contains(2.5))
        assertTrue(crossSections.contains(300.0))
    }

    // ۷. تست ساخت لیست کابل و خروجی معتبر CSV
    @Test
    fun test07_CableList_BuildAndCsvExport() {
        val c1 = CircuitComponent(id = "km1", type = ComponentType.CONTACTOR_COIL, label = "KM1", x = 50f, y = 50f)
        val c2 = CircuitComponent(id = "sb1", type = ComponentType.PUSH_BUTTON_NO, label = "SB1", x = 150f, y = 150f)

        val wire = WireConnection(
            id = "w1",
            fromComponentId = "km1",
            fromTerminalId = "A1",
            toComponentId = "sb1",
            toTerminalId = "14",
            wireNumber = "101",
            wireColorHex = "#D50000",
            crossSectionMm2 = 1.5,
            colorStandardRef = "IEC 60204-1 بند 13.2.4"
        )

        val cableList = NetNumberingManager.buildCableList(listOf(wire), listOf(c1, c2))
        assertEquals(1, cableList.size)
        val item = cableList.first()

        assertEquals("101", item.wireNumber)
        assertEquals("KM1", item.fromComponentLabel)
        assertEquals("A1", item.fromTerminalId)
        assertEquals("SB1", item.toComponentLabel)
        assertEquals("14", item.toTerminalId)
        assertEquals(1.5, item.crossSectionMm2, 0.001)

        val csv = NetNumberingManager.exportCableListToCsv(cableList)
        assertTrue("خروجی CSV باید شامل ستون شماره سیم باشد", csv.contains("شماره سیم"))
        assertTrue("خروجی CSV باید شامل ردیف سیم ۱۰۱ باشد", csv.contains("\"101\""))
        assertTrue("خروجی CSV باید شامل ترمینال مبدأ A1 باشد", csv.contains("\"A1\""))
        assertTrue("خروجی CSV باید شامل سطح مقطع ۱.۵ باشد", csv.contains("1.5"))
    }
}
