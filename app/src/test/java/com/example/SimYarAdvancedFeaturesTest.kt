package com.example

import com.example.canvas.CanvasFitCalculator
import com.example.canvas.CircuitBoundingBox
import com.example.model.CircuitComponent
import com.example.model.ComponentType
import com.example.model.LadderElement
import com.example.model.LadderElementType
import com.example.model.LadderRung
import com.example.model.MotorDirection
import com.example.model.PlcMemory
import com.example.model.WireConnection
import com.example.simulation.CircuitSimulationEngine
import com.example.simulation.PlcLadderEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * مجموعه ۱۰ تست واحد جدید و پیشرفته برای نیازمندی‌های صنعتی سیم‌یار:
 * ۱ تا ۳: مدار ستاره-مثلث با تایمر و اینترلاک دوطرفه
 * ۴ تا ۷: مدار چپگرد / راستگرد با اینترلاک متقابل
 * ۸ و ۹: حفاظت رله حرارتی بی‌متال (قطع و ریست)
 * ۱۰: عملکرد کویل‌های Set و Reset در شبیه‌ساز PLC نردبانی
 */
class SimYarAdvancedFeaturesTest {

    private lateinit var circuitEngine: CircuitSimulationEngine
    private lateinit var plcEngine: PlcLadderEngine

    @Before
    fun setUp() {
        circuitEngine = CircuitSimulationEngine()
        plcEngine = PlcLadderEngine()
    }

    // ۱. تست ستاره-مثلث: در ابتدا بوبین خط (KM1) و ستاره (KM2) وصل هستند
    @Test
    fun test01_StarDelta_InitialStateInStarMode() {
        val components = listOf(
            CircuitComponent("l", ComponentType.AC_PHASE, "L", 100f, 100f),
            CircuitComponent("sb_start", ComponentType.PUSH_BUTTON_NO, "Start", 100f, 200f, isClosedOrPressed = true),
            CircuitComponent("km1", ComponentType.CONTACTOR_COIL, "KM1", 100f, 300f, tagBinding = "KM1"),
            CircuitComponent("km2", ComponentType.CONTACTOR_COIL, "KM2_STAR", 250f, 300f, tagBinding = "KM2"),
            CircuitComponent("km3", ComponentType.CONTACTOR_COIL, "KM3_DELTA", 400f, 300f, tagBinding = "KM3", interlockTag = "KM2"),
            CircuitComponent("kt1", ComponentType.TIMER_ON_DELAY_COIL, "KT1", 100f, 400f, tagBinding = "KT1", delaySeconds = 2f),
            CircuitComponent("n", ComponentType.AC_NEUTRAL, "N", 100f, 500f)
        )
        val wires = listOf(
            WireConnection("w1", "l", "L", "sb_start", "13"),
            WireConnection("w2", "sb_start", "14", "km1", "A1"),
            WireConnection("w3", "sb_start", "14", "km2", "A1"),
            WireConnection("w4", "sb_start", "14", "kt1", "A1"),
            WireConnection("w5", "km1", "A2", "n", "N"),
            WireConnection("w6", "km2", "A2", "n", "N"),
            WireConnection("w7", "kt1", "A2", "n", "N")
        )

        val result = circuitEngine.stepSimulation(components, wires, deltaSeconds = 0.1f)
        assertTrue("بوبین خط KM1 باید روشن باشد", result.energizedCoilTags.contains("KM1"))
        assertTrue("بوبین ستاره KM2 باید روشن باشد", result.energizedCoilTags.contains("KM2"))
        assertFalse("بوبین مثلث KM3 در ابتدا نباید روشن باشد", result.energizedCoilTags.contains("KM3"))
    }

    // ۲. تست ستاره-مثلث: پس از پایان زمان تایمر، ستاره قطع و مثلث وصل می‌شود
    @Test
    fun test02_StarDelta_TimerCountsAndSwitchesToDeltaMode() {
        val timerCoil = CircuitComponent("kt1", ComponentType.TIMER_ON_DELAY_COIL, "KT1", 100f, 300f, tagBinding = "KT1", delaySeconds = 1f, isEnergized = true)
        val timerNO = CircuitComponent("kt_no", ComponentType.TIMER_NO_CONTACT, "KT1_NO", 100f, 200f, tagBinding = "KT1")
        val km3Coil = CircuitComponent("km3", ComponentType.CONTACTOR_COIL, "KM3", 100f, 400f, tagBinding = "KM3")
        val l = CircuitComponent("l", ComponentType.AC_PHASE, "L", 100f, 100f)
        val n = CircuitComponent("n", ComponentType.AC_NEUTRAL, "N", 100f, 500f)

        val comps = listOf(l, timerCoil, timerNO, km3Coil, n)
        val wires = listOf(
            WireConnection("w1", "l", "L", "kt1", "A1"),
            WireConnection("w2", "kt1", "A2", "n", "N"),
            WireConnection("w3", "l", "L", "kt_no", "17"),
            WireConnection("w4", "kt_no", "18", "km3", "A1"),
            WireConnection("w5", "km3", "A2", "n", "N")
        )

        // اجرای ۱.۲ ثانیه شبیه‌سازی تا پایان زمان تایمر ۱ ثانیه‌ای
        var currentComps = comps
        var currentWires = wires
        for (i in 1..12) {
            val step = circuitEngine.stepSimulation(currentComps, currentWires, deltaSeconds = 0.1f)
            currentComps = step.updatedComponents
            currentWires = step.updatedWires
        }

        val updatedKm3 = currentComps.find { it.id == "km3" }
        assertNotNull("کنتاکتور مثلث باید موجود باشد", updatedKm3)
        assertTrue("پس از اتمام زمان تایمر، بوبین مثلث KM3 باید فعال شود", updatedKm3!!.isEnergized)
    }

    // ۳. تست اینترلاک ستاره-مثلث: ستاره و مثلث نباید همزمان وصل شوند
    @Test
    fun test03_StarDelta_MutualInterlockPreventsSimultaneousStarAndDelta() {
        val l = CircuitComponent("l", ComponentType.AC_PHASE, "L", 100f, 100f)
        val km2 = CircuitComponent("km2", ComponentType.CONTACTOR_COIL, "KM2_STAR", 100f, 250f, tagBinding = "KM2", interlockTag = "KM3")
        val km3 = CircuitComponent("km3", ComponentType.CONTACTOR_COIL, "KM3_DELTA", 300f, 250f, tagBinding = "KM3", interlockTag = "KM2")
        val n = CircuitComponent("n", ComponentType.AC_NEUTRAL, "N", 100f, 400f)

        // وصل کردن هر دو بوبین به فاز و نول
        val wires = listOf(
            WireConnection("w1", "l", "L", "km2", "A1"),
            WireConnection("w2", "km2", "A2", "n", "N"),
            WireConnection("w3", "l", "L", "km3", "A1"),
            WireConnection("w4", "km3", "A2", "n", "N")
        )

        val result = circuitEngine.stepSimulation(listOf(l, km2, km3, n), wires, deltaSeconds = 0.1f)
        assertTrue("اتصال همزمان دو بوبین دارای اینترلاک متقابل باید خطای نقض اینترلاک ثبت کند", result.isInterlockViolation)
        assertFalse("نباید هر دو بوبین ستاره و مثلث همزمان برق‌دار باشند",
            result.energizedCoilTags.contains("KM2") && result.energizedCoilTags.contains("KM3"))
    }

    // ۴. تست چپگرد/راستگرد: با زدن استارت راستگرد، بوبین KM1 فعال شده و جهت ساعتگرد است
    @Test
    fun test04_ForwardReverse_ForwardEngagementEnergizesKm1() {
        val l = CircuitComponent("l", ComponentType.AC_PHASE, "L", 100f, 100f)
        val startFwd = CircuitComponent("sb_fwd", ComponentType.PUSH_BUTTON_NO, "StartFwd", 100f, 200f, isClosedOrPressed = true)
        val km1Coil = CircuitComponent("km1", ComponentType.CONTACTOR_COIL, "KM1", 100f, 320f, tagBinding = "KM1", interlockTag = "KM2")
        val motor = CircuitComponent("m1", ComponentType.MOTOR_3PHASE, "M1", 100f, 450f, tagBinding = "KM1")
        val n = CircuitComponent("n", ComponentType.AC_NEUTRAL, "N", 100f, 600f)

        val wires = listOf(
            WireConnection("w1", "l", "L", "sb_fwd", "13"),
            WireConnection("w2", "sb_fwd", "14", "km1", "A1"),
            WireConnection("w3", "km1", "A2", "n", "N"),
            WireConnection("w4", "km1", "A1", "m1", "U"),
            WireConnection("w5", "m1", "W", "n", "N")
        )

        val result = circuitEngine.stepSimulation(listOf(l, startFwd, km1Coil, motor, n), wires, deltaSeconds = 0.1f)
        assertTrue("کویل راستگرد KM1 باید روشن شود", result.energizedCoilTags.contains("KM1"))
        val updatedMotor = result.updatedComponents.find { it.id == "m1" }
        assertEquals("جهت چرخش موتور باید ساعتگرد (CLOCKWISE) باشد", MotorDirection.CLOCKWISE, updatedMotor?.motorRotationDirection)
    }

    // ۵. تست اینترلاک چپگرد/راستگرد: وقتی راستگرد وصل است، چپگرد وصل نمی‌شود
    @Test
    fun test05_ForwardReverse_ElectricalInterlockPreventsReverseWhenForwardIsActive() {
        val l = CircuitComponent("l", ComponentType.AC_PHASE, "L", 100f, 100f)
        // تیغه بسته کمکی KM1 (21-22) در مسیر کویل KM2
        val km1AuxNc = CircuitComponent("km1_nc", ComponentType.CONTACTOR_NC_AUX, "KM1_NC", 300f, 200f, tagBinding = "KM1")
        val startRev = CircuitComponent("sb_rev", ComponentType.PUSH_BUTTON_NO, "StartRev", 300f, 100f, isClosedOrPressed = true)
        val km2Coil = CircuitComponent("km2", ComponentType.CONTACTOR_COIL, "KM2", 300f, 320f, tagBinding = "KM2", interlockTag = "KM1")
        val km1Coil = CircuitComponent("km1", ComponentType.CONTACTOR_COIL, "KM1", 100f, 320f, tagBinding = "KM1", isEnergized = true)
        val n = CircuitComponent("n", ComponentType.AC_NEUTRAL, "N", 100f, 500f)

        val wires = listOf(
            WireConnection("w1", "l", "L", "startRev", "13"),
            WireConnection("w2", "startRev", "14", "km1_nc", "21"),
            WireConnection("w3", "km1_nc", "22", "km2", "A1"),
            WireConnection("w4", "km2", "A2", "n", "N")
        )

        val result = circuitEngine.stepSimulation(listOf(l, km1AuxNc, startRev, km2Coil, km1Coil, n), wires, deltaSeconds = 0.1f)
        assertFalse("به دلیل باز شدن تیغه کمکی KM1 NC، کویل KM2 نباید بتواند فعال شود", result.energizedCoilTags.contains("KM2"))
    }

    // ۶. تست چپگرد پس از قطع راستگرد: بوبین چپگرد KM2 فعال شده و جهت پادساعتگرد است
    @Test
    fun test06_ForwardReverse_StopAndReverseEngagementEnergizesKm2() {
        val l = CircuitComponent("l", ComponentType.AC_PHASE, "L", 100f, 100f)
        val startRev = CircuitComponent("sb_rev", ComponentType.PUSH_BUTTON_NO, "StartRev", 100f, 200f, isClosedOrPressed = true)
        val km2Coil = CircuitComponent("km2", ComponentType.CONTACTOR_COIL, "KM2", 100f, 320f, tagBinding = "KM2")
        val motor = CircuitComponent("m1", ComponentType.MOTOR_3PHASE, "M1", 100f, 450f, tagBinding = "KM2")
        val n = CircuitComponent("n", ComponentType.AC_NEUTRAL, "N", 100f, 600f)

        val wires = listOf(
            WireConnection("w1", "l", "L", "sb_rev", "13"),
            WireConnection("w2", "sb_rev", "14", "km2", "A1"),
            WireConnection("w3", "km2", "A2", "n", "N"),
            WireConnection("w4", "km2", "A1", "m1", "W"),
            WireConnection("w5", "m1", "U", "n", "N")
        )

        val result = circuitEngine.stepSimulation(listOf(l, startRev, km2Coil, motor, n), wires, deltaSeconds = 0.1f)
        assertTrue("کویل چپگرد KM2 باید روشن شود", result.energizedCoilTags.contains("KM2"))
        val updatedMotor = result.updatedComponents.find { it.id == "m1" }
        assertEquals("جهت چرخش موتور باید پادساعتگرد (COUNTER_CLOCKWISE) باشد", MotorDirection.COUNTER_CLOCKWISE, updatedMotor?.motorRotationDirection)
    }

    // ۷. تست اینترلاک متقابل: وقتی چپگرد وصل است، راستگرد وصل نمی‌شود
    @Test
    fun test07_ForwardReverse_ElectricalInterlockPreventsForwardWhenReverseIsActive() {
        val l = CircuitComponent("l", ComponentType.AC_PHASE, "L", 100f, 100f)
        // تیغه بسته کمکی KM2 در مسیر کویل KM1
        val km2AuxNc = CircuitComponent("km2_nc", ComponentType.CONTACTOR_NC_AUX, "KM2_NC", 100f, 200f, tagBinding = "KM2")
        val startFwd = CircuitComponent("sb_fwd", ComponentType.PUSH_BUTTON_NO, "StartFwd", 100f, 100f, isClosedOrPressed = true)
        val km1Coil = CircuitComponent("km1", ComponentType.CONTACTOR_COIL, "KM1", 100f, 320f, tagBinding = "KM1", interlockTag = "KM2")
        val km2Coil = CircuitComponent("km2", ComponentType.CONTACTOR_COIL, "KM2", 300f, 320f, tagBinding = "KM2", isEnergized = true)
        val n = CircuitComponent("n", ComponentType.AC_NEUTRAL, "N", 100f, 500f)

        val wires = listOf(
            WireConnection("w1", "l", "L", "startFwd", "13"),
            WireConnection("w2", "startFwd", "14", "km2_nc", "21"),
            WireConnection("w3", "km2_nc", "22", "km1", "A1"),
            WireConnection("w4", "km1", "A2", "n", "N")
        )

        val result = circuitEngine.stepSimulation(listOf(l, km2AuxNc, startFwd, km1Coil, km2Coil, n), wires, deltaSeconds = 0.1f)
        assertFalse("وقتی KM2 روشن است، تیغه KM2 NC باز شده و KM1 نباید بتواند وصل شود", result.energizedCoilTags.contains("KM1"))
    }

    // ۸. تست رله حرارتی بی‌متال: با عمل کردن بی‌متال، کل مدار فرمان قطع می‌شود
    @Test
    fun test08_ThermalOverload_BimetalTripCutsControlCircuit() {
        val l = CircuitComponent("l", ComponentType.AC_PHASE, "L", 100f, 100f)
        // بی‌متال تریپ کرده (کنتاکت ۹۵-۹۶ باز شده است)
        val bimetal = CircuitComponent("fr1", ComponentType.BIMETAL_OVERLOAD, "FR1", 100f, 200f, isClosedOrPressed = true)
        val sbStart = CircuitComponent("sb1", ComponentType.PUSH_BUTTON_NO, "Start", 100f, 300f, isClosedOrPressed = true)
        val km1 = CircuitComponent("km1", ComponentType.CONTACTOR_COIL, "KM1", 100f, 400f, tagBinding = "KM1")
        val n = CircuitComponent("n", ComponentType.AC_NEUTRAL, "N", 100f, 500f)

        val wires = listOf(
            WireConnection("w1", "l", "L", "fr1", "95"),
            WireConnection("w2", "fr1", "96", "sb1", "13"),
            WireConnection("w3", "sb1", "14", "km1", "A1"),
            WireConnection("w4", "km1", "A2", "n", "N")
        )

        val result = circuitEngine.stepSimulation(listOf(l, bimetal, sbStart, km1, n), wires, deltaSeconds = 0.1f)
        assertFalse("هنگام تریپ بی‌متال، جریان قطع است و کویل کنتاکتور نباید روشن شود", result.energizedCoilTags.contains("KM1"))
    }

    // ۹. تست ریست رله حرارتی: با ریست شدن بی‌متال، مدار مجددا با شستی استارت وصل می‌شود
    @Test
    fun test09_ThermalOverload_ResetRestoresCircuitReadiness() {
        val l = CircuitComponent("l", ComponentType.AC_PHASE, "L", 100f, 100f)
        // بی‌متال در حالت عادی بسته (ریست شده)
        val bimetal = CircuitComponent("fr1", ComponentType.BIMETAL_OVERLOAD, "FR1", 100f, 200f, isClosedOrPressed = false)
        val sbStart = CircuitComponent("sb1", ComponentType.PUSH_BUTTON_NO, "Start", 100f, 300f, isClosedOrPressed = true)
        val km1 = CircuitComponent("km1", ComponentType.CONTACTOR_COIL, "KM1", 100f, 400f, tagBinding = "KM1")
        val n = CircuitComponent("n", ComponentType.AC_NEUTRAL, "N", 100f, 500f)

        val wires = listOf(
            WireConnection("w1", "l", "L", "fr1", "95"),
            WireConnection("w2", "fr1", "96", "sb1", "13"),
            WireConnection("w3", "sb1", "14", "km1", "A1"),
            WireConnection("w4", "km1", "A2", "n", "N")
        )

        val result = circuitEngine.stepSimulation(listOf(l, bimetal, sbStart, km1, n), wires, deltaSeconds = 0.1f)
        assertTrue("پس از ریست بی‌متال، فشردن شستی استارت بوبین KM1 را روشن می‌کند", result.energizedCoilTags.contains("KM1"))
    }

    // ۱۰. تست شبیه‌ساز PLC نردبانی: بررسی رفتار کویل‌های Set و Reset در سیکل‌های اسکن
    @Test
    fun test10_PlcLadder_SetAndResetCoilMemoryPersistence() {
        val memory = PlcMemory()
        // رانگ ۱: با فعال شدن I0.0، کویل Q0.0 ست می‌شود
        val setRung = LadderRung(
            id = "r1",
            rungNumber = 1,
            contacts = listOf(LadderElement("c1", LadderElementType.NO_CONTACT, "I0.0")),
            coils = listOf(LadderElement("q1", LadderElementType.SET_COIL, "Q0.0"))
        )

        // رانگ ۲: با فعال شدن I0.1، کویل Q0.0 ریست می‌شود
        val resetRung = LadderRung(
            id = "r2",
            rungNumber = 2,
            contacts = listOf(LadderElement("c2", LadderElementType.NO_CONTACT, "I0.1")),
            coils = listOf(LadderElement("q2", LadderElementType.RESET_COIL, "Q0.0"))
        )

        val rungs = listOf(setRung, resetRung)

        // سیکل ۱: فعال کردن ورودی I0.0 جهت Set کردن
        val memWithI0 = memory.setInput("I0.0", true).setInput("I0.1", false)
        val (rungsAfterSet, memAfterSet) = plcEngine.executeCycle(rungs, memWithI0)
        assertTrue("خروجی Q0.0 باید Set شده و ۱ باشد", memAfterSet.outputs["Q0.0"] == true)

        // سیکل ۲: قطع کردن ورودی I0.0 (خروجی Set شده باید ۱ باقی بماند)
        val memReleaseI0 = memAfterSet.setInput("I0.0", false)
        val (_, memAfterHold) = plcEngine.executeCycle(rungsAfterSet, memReleaseI0)
        assertTrue("خروجی Q0.0 پس از قطع ورودی استارت همچنان باید روشن و Latch بماند", memAfterHold.outputs["Q0.0"] == true)

        // سیکل ۳: فعال کردن ورودی Reset (I0.1) جهت خاموش کردن خروجی
        val memTriggerReset = memAfterHold.setInput("I0.1", true)
        val (_, memAfterReset) = plcEngine.executeCycle(rungs, memTriggerReset)
        assertFalse("با اجرای کویل ریست، خروجی Q0.0 باید صفر و خاموش شود", memAfterReset.outputs["Q0.0"] == true)
    }

    // ۱۱. تست محاسبه انطباق مدار روی بوم (CanvasFitCalculator.calculateFit) با در نظر گرفتن Density
    @Test
    fun test11_CanvasFitCalculator_CalculateFitWithDensity() {
        val bbox = CircuitBoundingBox(
            minX = 40f,
            maxX = 400f,
            minY = 60f,
            maxY = 800f
        )
        val viewportWidth = 1080f
        val viewportHeight = 1920f
        val density = 2.5f

        val result = CanvasFitCalculator.calculateFit(
            bbox = bbox,
            viewportWidth = viewportWidth,
            viewportHeight = viewportHeight,
            density = density
        )

        assertTrue("مقیاس باید در بازه مجاز ضربدر چگالی باشد", result.scale in (CanvasFitCalculator.MIN_SCALE * density)..(CanvasFitCalculator.MAX_SCALE * density))
        // مرکز مدار در مختصات افقی: (۴۰ + ۴۰۰) / ۲ = ۲۲۰. panX = 1080/2 - 220 * scale = 540 - 220 * scale
        val expectedCenterX = (bbox.minX + bbox.maxX) / 2f
        val expectedPanX = (viewportWidth / 2f) - (expectedCenterX * result.scale)
        assertEquals("جابجایی افقی باید دقیقاً مرکز مدار را در مرکز بوم قرار دهد", expectedPanX, result.panX, 0.01f)

        // تست با کادر خالی
        val emptyResult = CanvasFitCalculator.calculateFit(null, viewportWidth, viewportHeight, density)
        assertEquals("در صورت نبود قطعه، مقیاس پیش‌فرض برابر با چگالی خواهد بود", density, emptyResult.scale, 0.01f)
    }

    // ۱۲. تست محاسبه شعاع لمسی ترمینال با جبران مقیاس و چگالی
    @Test
    fun test12_CanvasFitCalculator_TerminalTouchRadiusClamping() {
        val density = 2.75f

        // در زوم بسیار بالا (مقیاس ۵)، شعاع در واحدهای بوم کوچک‌تر می‌شود اما به حداقل ۱۴ کلمپ می‌شود
        val radiusHighZoom = CanvasFitCalculator.calculateTerminalTouchRadius(scale = 5.0f, density = density, targetDpRadius = 22f)
        assertTrue("شعاع ترمینال نباید کمتر از ۱۴ واحد بوم شود", radiusHighZoom >= 14f)

        // در زوم بسیار کم (مقیاس ۰.۲)، شعاع در واحدهای بوم نباید بیش از ۴۰ واحد شود تا بدنه قطعه را نپوشاند
        val radiusLowZoom = CanvasFitCalculator.calculateTerminalTouchRadius(scale = 0.2f, density = density, targetDpRadius = 22f)
        assertTrue("شعاع ترمینال نباید بیشتر از ۴۰ واحد بوم شود", radiusLowZoom <= 40f)

        // در مقیاس عادی ۱
        val normalRadius = CanvasFitCalculator.calculateTerminalTouchRadius(scale = 1.0f, density = 1f, targetDpRadius = 22f)
        assertEquals(22f, normalRadius, 0.1f)
    }

    // ۱۳. تست پایانه‌های جدید رله حرارتی بی‌متال (97-98 و قدرت 1-2, 3-4, 5-6)
    @Test
    fun test13_BimetalOverload_NewTerminalsPresence() {
        val bimetal = CircuitComponent("fr1", ComponentType.BIMETAL_OVERLOAD, "FR1", 100f, 100f)
        val termIds = bimetal.getTerminals().map { it.id }
        assertTrue("ترمینال ۹۵ باید موجود باشد", termIds.contains("95"))
        assertTrue("ترمینال ۹۶ باید موجود باشد", termIds.contains("96"))
        assertTrue("ترمینال ۹۷ باید موجود باشد", termIds.contains("97"))
        assertTrue("ترمینال ۹۸ باید موجود باشد", termIds.contains("98"))
        assertTrue("ترمینال‌های ورودی قدرت 1, 3, 5 باید موجود باشند", termIds.containsAll(listOf("1", "3", "5")))
        assertTrue("ترمینال‌های خروجی قدرت 2, 4, 6 باید موجود باشند", termIds.containsAll(listOf("2", "4", "6")))
    }

    // ۱۴. تست پایانه‌های ۶ گانه تخته کلمپ موتور سه‌فاز (U1, V1, W1 و U2, V2, W2)
    @Test
    fun test14_Motor3Phase_SixTerminalsPresence() {
        val motor = CircuitComponent("m1", ComponentType.MOTOR_3PHASE, "M1", 100f, 100f)
        val termIds = motor.getTerminals().map { it.id }
        assertTrue("پایانه‌های ابتدایی U, V, W باید موجود باشند", termIds.containsAll(listOf("U", "V", "W")))
        assertTrue("پایانه‌های انتهایی U2, V2, W2 باید موجود باشند", termIds.containsAll(listOf("U2", "V2", "W2")))
        assertTrue("ترمینال ارت PE باید موجود باشد", termIds.contains("PE"))
    }

    // ۱۵. تست پایانه‌های جدید میکروسوئیچ و تیغه‌های تایمر
    @Test
    fun test15_LimitSwitchAndTimer_StandardTerminalNumbering() {
        val lsNO = CircuitComponent("sq_no", ComponentType.LIMIT_SWITCH_NO, "SQ1", 100f, 100f)
        val lsNC = CircuitComponent("sq_nc", ComponentType.LIMIT_SWITCH_NC, "SQ2", 100f, 100f)
        val timerNO = CircuitComponent("kt_no", ComponentType.TIMER_NO_CONTACT, "KT_NO", 100f, 100f)
        val timerNC = CircuitComponent("kt_nc", ComponentType.TIMER_NC_CONTACT, "KT_NC", 100f, 100f)

        assertTrue("میکروسوئیچ NO باید دارای ترمینال‌های ۱۳ و ۱۴ باشد", lsNO.getTerminals().map { it.id }.containsAll(listOf("13", "14")))
        assertTrue("میکروسوئیچ NC باید دارای ترمینال‌های ۱۱ و ۱۲ باشد", lsNC.getTerminals().map { it.id }.containsAll(listOf("11", "12")))
        assertTrue("تیغه باز تایمر باید دارای ترمینال‌های ۱۵ و ۱۸ باشد", timerNO.getTerminals().map { it.id }.containsAll(listOf("15", "18")))
        assertTrue("تیغه بسته تایمر باید دارای ترمینال‌های ۱۵ و ۱۶ باشد", timerNC.getTerminals().map { it.id }.containsAll(listOf("15", "16")))
    }

    // ۱۶. تست قالب راه‌اندازی ستاره-مثلث واقعی و کارکرد آن
    @Test
    fun test16_StarDeltaTemplate_FullPowerAndControlSimulation() {
        val template = com.example.model.DefaultCircuitTemplates.createStarDeltaStarter()
        assertNotNull("قالب ستاره مثلث باید دارای قطعات باشد", template.components)
        assertTrue("باید حداقل ۱۲ قطعه در مدار قدرت و فرمان موجود باشد", template.components.size >= 12)

        // شبیه‌سازی گام اول با فشردن شستی استارت
        val pressedComps = template.components.map {
            if (it.id == "sb2") it.copy(isClosedOrPressed = true) else it
        }
        val step1 = circuitEngine.stepSimulation(pressedComps, template.wires, deltaSeconds = 0.1f)
        assertTrue("کنتاکتور خط اصلی KM1 باید روشن شود", step1.energizedCoilTags.contains("KM1"))
        assertTrue("کنتاکتور ستاره KM2 باید روشن شود", step1.energizedCoilTags.contains("KM2"))
        assertFalse("کنتاکتور مثلث KM3 در ابتدا نباید روشن باشد", step1.energizedCoilTags.contains("KM3"))
    }

    // ۱۷. تست قرار دادن نمادهای استاندارد PLC روی رانگ بوم لدر
    @Test
    fun test17_PlcLadder_PlaceStandardSymbolsOnRung() {
        val initialRung = LadderRung(
            id = "test_rung_1",
            rungNumber = 1,
            comment = "تست قرار دادن نمادها",
            contacts = listOf(
                LadderElement("elem_1", LadderElementType.NO_CONTACT, "I0.0", comment = "استارت")
            ),
            coils = listOf(
                LadderElement("elem_coil", LadderElementType.NORMAL_COIL, "Q0.0", comment = "خروجی")
            )
        )

        // قرار دادن کنتاکت جدید روی رانگ
        val addedContact = LadderElement("elem_2", LadderElementType.NC_CONTACT, "I0.1", comment = "استپ")
        val updatedRungContacts = initialRung.copy(contacts = initialRung.contacts + addedContact)
        assertEquals(2, updatedRungContacts.contacts.size)
        assertEquals("I0.1", updatedRungContacts.contacts[1].address)
        assertEquals(LadderElementType.NC_CONTACT, updatedRungContacts.contacts[1].type)

        // قرار دادن شاخه موازی خودنگهدار (Parallel OR branch)
        val branchContact = LadderElement("elem_branch", LadderElementType.NO_CONTACT, "Q0.0", comment = "خودنگهدار")
        val updatedRungBranch = updatedRungContacts.copy(branchContacts = listOf(branchContact))
        assertEquals(1, updatedRungBranch.branchContacts.size)
        assertEquals("Q0.0", updatedRungBranch.branchContacts[0].address)

        // قرار دادن تایمر TON روی خروجی
        val timerCoil = LadderElement("elem_timer", LadderElementType.TIMER_TON, "T1", comment = "تایمر", presetValue = 3)
        val updatedRungTimer = updatedRungBranch.copy(coils = updatedRungBranch.coils + timerCoil)
        assertEquals(2, updatedRungTimer.coils.size)
        assertEquals("T1", updatedRungTimer.coils[1].address)
        assertEquals(3, updatedRungTimer.coils[1].presetValue)
    }

    // ۱۸. تست اجرای شبیه‌سازی با مدار لدر ایجاد شده توسط نمادهای استاندارد
    @Test
    fun test18_PlcLadder_ExecutionWithPlacedSymbols() {
        val testRung = LadderRung(
            id = "test_cuj_rung",
            rungNumber = 1,
            comment = "مدار استپ/استارت و خودنگهدار",
            contacts = listOf(
                LadderElement("nc_stop", LadderElementType.NC_CONTACT, "I0.0"),
                LadderElement("no_start", LadderElementType.NO_CONTACT, "I0.1")
            ),
            branchContacts = listOf(
                LadderElement("no_latch", LadderElementType.NO_CONTACT, "Q0.0")
            ),
            coils = listOf(
                LadderElement("coil_q0", LadderElementType.NORMAL_COIL, "Q0.0")
            )
        )

        var mem = com.example.model.PlcMemory()
        // حالت ۱: قبل از استارت -> Q0.0 خاموش
        val res1 = plcEngine.executeCycle(listOf(testRung), mem, cycleDeltaSeconds = 0.05f)
        assertFalse("خروجی ابتدا باید خاموش باشد", res1.second.outputs["Q0.0"] ?: false)

        // حالت ۲: فعال کردن استارت I0.1 -> Q0.0 روشن می‌شود
        mem = mem.setInput("I0.1", true)
        val res2 = plcEngine.executeCycle(listOf(testRung), mem, cycleDeltaSeconds = 0.05f)
        assertTrue("با فشردن استارت خروجی Q0.0 باید روشن شود", res2.second.outputs["Q0.0"] ?: false)

        // حالت ۳: رها کردن استارت I0.1 -> به دلیل شاخه خودنگهدار Q0.0، خروجی همچنان روشن می‌ماند
        mem = mem.setInput("I0.1", false)
        val res3 = plcEngine.executeCycle(listOf(testRung), res2.second, cycleDeltaSeconds = 0.05f)
        assertTrue("خودنگهدار باید Q0.0 را روشن نگه دارد", res3.second.outputs["Q0.0"] ?: false)
    }
}
