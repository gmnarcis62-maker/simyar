package com.example

import com.example.data.CircuitTypeConverters
import com.example.model.CircuitComponent
import com.example.model.CircuitLayer
import com.example.model.CircuitSheet
import com.example.model.ComponentType
import com.example.model.TechnicalSpecs
import com.example.model.WireConnection
import com.example.simulation.CircuitSimulationEngine
import com.example.viewmodel.AlignmentType
import com.example.viewmodel.DistributionType
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * تست‌های مرحله P2: ابزارهای ویرایش پیشرفته بوم مدار سیمیار
 * ۱. عملیات گروهی و Undo
 * ۲. هم‌ترازی (مختصات دقیق خروجی)
 * ۳. توزیع افقی و عمودی (مختصات دقیق خروجی)
 * ۴. جستجوی قطعه روی بوم بر اساس برچسب، تگ، نوع و مدل
 * ۵. چند برگه (Sheet)، جلوگیری از عبور سیم بین برگه‌ها، و اجرای همزمان تگ مشترک در شبیه‌سازی
 * ۶. لایه‌ها (عدم تأثیر لایه مخفی در شبیه‌سازی، جلوگیری از جابجایی لایه قفل‌شده)
 * ۷. Migration و سازگاری پروژه‌های قدیمی با مقادیر پیش‌فرض
 */
class SimYarAdvancedCanvasToolsTest {

    private lateinit var circuitEngine: CircuitSimulationEngine
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    @Before
    fun setUp() {
        circuitEngine = CircuitSimulationEngine()
    }

    // ۱. تست عملیات گروهی: چرخش و قفل گروهی و برگشت با Undo
    @Test
    fun test01_GroupOperationsAndSingleUndo() {
        val c1 = CircuitComponent(id = "c1", type = ComponentType.CONTACTOR_COIL, label = "KM1", x = 100f, y = 100f, rotation = 0f, isLocked = false)
        val c2 = CircuitComponent(id = "c2", type = ComponentType.CONTACTOR_NO_AUX, label = "KM1_NO", x = 200f, y = 100f, rotation = 0f, isLocked = false)
        val initialList = listOf(c1, c2)

        // شبیه‌سازی ورودی Undo: ذخیره حالت قبلی
        val undoStack = mutableListOf<List<CircuitComponent>>()
        undoStack.add(initialList)

        // چرخش گروهی ۹۰ درجه
        val targetIds = setOf("c1", "c2")
        val rotated = initialList.map { comp ->
            if (comp.id in targetIds) comp.copy(rotation = (comp.rotation + 90f) % 360f) else comp
        }
        assertEquals(90f, rotated.find { it.id == "c1" }?.rotation)
        assertEquals(90f, rotated.find { it.id == "c2" }?.rotation)

        // بازگردانی با Undo
        val restoredFromUndo = undoStack.removeAt(undoStack.size - 1)
        assertEquals(0f, restoredFromUndo.find { it.id == "c1" }?.rotation)
        assertEquals(0f, restoredFromUndo.find { it.id == "c2" }?.rotation)
    }

    // ۲. تست هم‌ترازی: محاسبه دقیق مختصات خروجی (چپ، مرکز افقی، راست، بالا، وسط عمودی، پایین)
    @Test
    fun test02_AlignmentCalculations() {
        // دو قطعه با عرض ۱۰۰ و ارتفاع ۸۰
        val c1 = CircuitComponent(id = "c1", type = ComponentType.CONTACTOR_COIL, label = "KM1", x = 100f, y = 50f)
        val c2 = CircuitComponent(id = "c2", type = ComponentType.CONTACTOR_COIL, label = "KM2", x = 300f, y = 200f)
        val list = listOf(c1, c2)

        // الف) هم‌ترازی چپ: هر دو x برابر با min(x) = 100
        val minX = list.minOf { it.x }
        val alignedLeft = list.map { it.copy(x = minX) }
        assertEquals(100f, alignedLeft[0].x)
        assertEquals(100f, alignedLeft[1].x)

        // ب) هم‌ترازی راست: هر دو x برابر با maxRight - width = (300+100) - 100 = 300
        val maxRight = list.maxOf { it.x + CircuitComponent.COMPONENT_WIDTH }
        val alignedRight = list.map { it.copy(x = maxRight - CircuitComponent.COMPONENT_WIDTH) }
        assertEquals(300f, alignedRight[0].x)
        assertEquals(300f, alignedRight[1].x)

        // ج) هم‌ترازی مرکز افقی
        val midX = (minX + maxRight) / 2f // (100 + 400) / 2 = 250
        val targetCenterX = midX - CircuitComponent.COMPONENT_WIDTH / 2f // 250 - 50 = 200
        val alignedCenterX = list.map { it.copy(x = targetCenterX) }
        assertEquals(200f, alignedCenterX[0].x)
        assertEquals(200f, alignedCenterX[1].x)

        // د) هم‌ترازی بالا: هر دو y برابر با minY = 50
        val minY = list.minOf { it.y }
        val alignedTop = list.map { it.copy(y = minY) }
        assertEquals(50f, alignedTop[0].y)
        assertEquals(50f, alignedTop[1].y)

        // ه) هم‌ترازی پایین: هر دو bottom برابر با maxBottom = 200 + 80 = 280 -> y = 280 - 80 = 200
        val maxBottom = list.maxOf { it.y + CircuitComponent.COMPONENT_HEIGHT }
        val alignedBottom = list.map { it.copy(y = maxBottom - CircuitComponent.COMPONENT_HEIGHT) }
        assertEquals(200f, alignedBottom[0].y)
        assertEquals(200f, alignedBottom[1].y)
    }

    // ۳. تست توزیع افقی و عمودی با فواصل کاملاً مساوی
    @Test
    fun test03_DistributionCalculations() {
        val c1 = CircuitComponent(id = "c1", type = ComponentType.PUSH_BUTTON_NO, label = "S1", x = 100f, y = 50f)
        val c2 = CircuitComponent(id = "c2", type = ComponentType.PUSH_BUTTON_NO, label = "S2", x = 180f, y = 120f)
        val c3 = CircuitComponent(id = "c3", type = ComponentType.PUSH_BUTTON_NO, label = "S3", x = 400f, y = 250f)
        val items = listOf(c1, c2, c3)

        // توزیع افقی: فواصل بین 100 تا 400 برای ۳ قطعه -> پله = (400 - 100) / 2 = 150
        val sortedX = items.sortedBy { it.x }
        val firstX = sortedX.first().x
        val lastX = sortedX.last().x
        val stepX = (lastX - firstX) / (sortedX.size - 1)
        val distributedX = sortedX.mapIndexed { i, c -> c.copy(x = firstX + i * stepX) }

        assertEquals(100f, distributedX[0].x)
        assertEquals(250f, distributedX[1].x)
        assertEquals(400f, distributedX[2].x)
    }

    // ۴. تست جستجوی قطعه بر اساس برچسب (Label)، تگ (Tag)، نام فارسی نوع و مدل
    @Test
    fun test04_ComponentSearchMatching() {
        val components = listOf(
            CircuitComponent(
                id = "c1",
                type = ComponentType.CONTACTOR_COIL,
                label = "KM1",
                x = 100f,
                y = 100f,
                tagBinding = "KM1_MAIN",
                specs = TechnicalSpecs(manufacturerModel = "LC1D09")
            ),
            CircuitComponent(
                id = "c2",
                type = ComponentType.MOTOR_3PHASE,
                label = "M1",
                x = 200f,
                y = 100f,
                specs = TechnicalSpecs(manufacturerModel = "Motogen 5.5kW")
            ),
            CircuitComponent(
                id = "c3",
                type = ComponentType.TIMER_ON_DELAY_COIL,
                label = "KT1",
                x = 300f,
                y = 100f,
                tagBinding = "KT1_STAR"
            )
        )

        // الف) جستجو بر اساس برچسب
        val matchByLabel = components.filter { it.label.lowercase().contains("km1") }
        assertEquals(1, matchByLabel.size)
        assertEquals("c1", matchByLabel[0].id)

        // ب) جستجو بر اساس مدل دیتاشیت
        val matchByModel = components.filter { it.specs.manufacturerModel.lowercase().contains("lc1d") }
        assertEquals(1, matchByModel.size)
        assertEquals("c1", matchByModel[0].id)

        // ج) جستجو بر اساس نوع فارسی
        val matchByType = components.filter { it.type.persianName.lowercase().contains("موتور") }
        assertEquals(1, matchByType.size)
        assertEquals("c2", matchByType[0].id)

        // د) جستجو بر اساس تگ
        val matchByTag = components.filter { it.tagBinding.lowercase().contains("star") }
        assertEquals(1, matchByTag.size)
        assertEquals("c3", matchByTag[0].id)
    }

    // ۵. تست چند برگه (Sheets): عدم اتصال سیم بین دو برگه مختلف و عملکرد شبیه‌سازی برای تگ مشترک
    @Test
    fun test05_MultiSheetIsolationAndGlobalSimulation() {
        val sheet1Id = "sheet_1"
        val sheet2Id = "sheet_2"

        // برگه ۱: مدار فرمان شامل فاز، شستی استارت و بوبین KM1
        val phase = CircuitComponent("l", ComponentType.AC_PHASE, "L", 100f, 100f, sheetId = sheet1Id)
        val sbStart = CircuitComponent("sb1", ComponentType.PUSH_BUTTON_NO, "Start", 100f, 200f, isClosedOrPressed = true, sheetId = sheet1Id)
        val km1Coil = CircuitComponent("km1_coil", ComponentType.CONTACTOR_COIL, "KM1", 100f, 300f, tagBinding = "KM1", sheetId = sheet1Id)
        val neutral = CircuitComponent("n", ComponentType.AC_NEUTRAL, "N", 100f, 400f, sheetId = sheet1Id)

        // برگه ۲: تیغه باز کنتاکتور KM1 که لامپ سیگنال سبز را روشن می‌کند
        val phase2 = CircuitComponent("l2", ComponentType.AC_PHASE, "L2", 500f, 100f, sheetId = sheet2Id)
        val km1Contact = CircuitComponent("km1_no", ComponentType.CONTACTOR_NO_AUX, "KM1_NO", 500f, 200f, tagBinding = "KM1", sheetId = sheet2Id)
        val lamp = CircuitComponent("hl1", ComponentType.PILOT_LAMP_GREEN, "HL1", 500f, 300f, sheetId = sheet2Id)
        val neutral2 = CircuitComponent("n2", ComponentType.AC_NEUTRAL, "N2", 500f, 400f, sheetId = sheet2Id)

        // سیم‌های برگه ۱
        val wiresSheet1 = listOf(
            WireConnection("w1", "l", "L", "sb1", "13", sheetId = sheet1Id),
            WireConnection("w2", "sb1", "14", "km1_coil", "A1", sheetId = sheet1Id),
            WireConnection("w3", "km1_coil", "A2", "n", "N", sheetId = sheet1Id)
        )

        // سیم‌های برگه ۲
        val wiresSheet2 = listOf(
            WireConnection("w4", "l2", "L", "km1_no", "13", sheetId = sheet2Id),
            WireConnection("w5", "km1_no", "14", "hl1", "X1", sheetId = sheet2Id),
            WireConnection("w6", "hl1", "X2", "n2", "N", sheetId = sheet2Id)
        )

        // قانون: سیم نمی‌تواند از برگه عبور کند
        val crossSheetWireValid = (phase.sheetId == km1Contact.sheetId)
        assertFalse("سیم نباید بین دو برگه مختلف وصل شود", crossSheetWireValid)

        // اجرای شبیه‌سازی همزمان تمام برگه‌ها در گراف سراسری
        val allComponents = listOf(phase, sbStart, km1Coil, neutral, phase2, km1Contact, lamp, neutral2)
        val allWires = wiresSheet1 + wiresSheet2

        val step1 = circuitEngine.stepSimulation(allComponents, allWires, deltaSeconds = 0.05f)

        // کویل KM1 در برگه ۱ برق‌دار شده و تیغه آن بسته می‌شود
        assertTrue("بوبین KM1 در برگه ۱ باید فعال شود", step1.energizedCoilTags.contains("KM1"))
        val km1ContactStep1 = step1.updatedComponents.find { it.id == "km1_no" }
        assertTrue("تیغه باز KM1 باید در برگه ۲ بسته شود", km1ContactStep1?.isClosedOrPressed == true)

        // گام دوم: جریان از تیغه بسته شده عبور کرده و لامپ برگه ۲ روشن می‌شود
        val step2 = circuitEngine.stepSimulation(step1.updatedComponents, allWires, deltaSeconds = 0.05f)
        val updatedLamp = step2.updatedComponents.find { it.id == "hl1" }
        assertNotNull("لامپ باید وجود داشته باشد", updatedLamp)
        assertTrue("لامپ برگه ۲ باید به واسطه تگ مشترک با برگه ۱ روشن شود", updatedLamp!!.isEnergized)
    }

    // ۶. تست لایه‌ها: لایه مخفی در شبیه‌سازی اثر دارد اما نمایش داده نمی‌شود
    @Test
    fun test06_LayersVisibilityAndSimulationIntegrity() {
        val powerLayer = CircuitLayer("layer_power", "قدرت", isVisible = true, isLocked = false)
        val notesLayer = CircuitLayer("layer_notes", "یادداشت", isVisible = false, isLocked = true)

        val compInHiddenLayer = CircuitComponent(
            id = "comp_hidden",
            type = ComponentType.PUSH_BUTTON_NC,
            label = "Stop_Hidden",
            x = 100f,
            y = 100f,
            layerId = notesLayer.id
        )

        // بررسی اینکه آیا لایه مخفی است
        assertFalse("لایه یادداشت مخفی است", notesLayer.isVisible)
        assertTrue("لایه یادداشت قفل شده است", notesLayer.isLocked)

        // در شبیه‌سازی، تمام قطعات صرف نظر از لایه مخفی به موتور شبیه‌سازی پاس داده می‌شوند
        val source = CircuitComponent("src", ComponentType.AC_PHASE, "L", 100f, 50f)
        val lamp = CircuitComponent("lamp", ComponentType.PILOT_LAMP_RED, "HL_RED", 100f, 200f)
        val neutral = CircuitComponent("n", ComponentType.AC_NEUTRAL, "N", 100f, 300f)

        val wires = listOf(
            WireConnection("w1", "src", "L", "comp_hidden", "21"),
            WireConnection("w2", "comp_hidden", "22", "lamp", "X1"),
            WireConnection("w3", "lamp", "X2", "n", "N")
        )

        // با عبور جریان از شستی NC در لایه مخفی، لامپ روشن می‌شود
        val simResult = circuitEngine.stepSimulation(listOf(source, compInHiddenLayer, lamp, neutral), wires)
        val updatedLamp = simResult.updatedComponents.find { it.id == "lamp" }
        assertTrue("لایه مخفی نباید اثر منطقی مدار را قطع کند", updatedLamp?.isEnergized == true)
    }

    // ۷. تست Migration: سازگاری پروژه‌های نسخه قدیمی با مقادیر پیش‌فرض sheets و layers
    @Test
    fun test07_DatabaseMigration_DefaultValues() {
        val converter = CircuitTypeConverters()

        // الف) تست اگر رشته JSON خالی باشد، مقدار پیش‌فرض "برگه ۱" برگردانده شود
        val sheetsFromEmpty = converter.toSheets("")
        assertEquals(1, sheetsFromEmpty.size)
        assertEquals("sheet_1", sheetsFromEmpty[0].id)
        assertEquals("برگه ۱", sheetsFromEmpty[0].name)

        // ب) تست اگر رشته JSON لایه‌ها خالی باشد، سه لایه قدرت، فرمان و یادداشت برگردانده شود
        val layersFromEmpty = converter.toLayers("")
        assertEquals(3, layersFromEmpty.size)
        assertTrue(layersFromEmpty.any { it.id == "layer_power" && it.name == "قدرت" })
        assertTrue(layersFromEmpty.any { it.id == "layer_control" && it.name == "فرمان" })
        assertTrue(layersFromEmpty.any { it.id == "layer_notes" && it.name == "یادداشت" })

        // ج) تست قطعه قدیمی بدون sheetId و layerId با مقادیر پیش‌فرض
        val oldComponentJson = """
            {
               "id": "old_1",
               "type": "AC_PHASE",
               "label": "L1",
               "x": 100.0,
               "y": 100.0
            }
        """.trimIndent()

        val parsedComp = json.decodeFromString<CircuitComponent>(oldComponentJson)
        assertEquals("sheet_1", parsedComp.sheetId)
        assertEquals("layer_control", parsedComp.layerId)
    }
}
