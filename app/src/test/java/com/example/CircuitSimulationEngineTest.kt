package com.example

import com.example.model.CircuitComponent
import com.example.model.ComponentType
import com.example.model.MotorDirection
import com.example.model.WireConnection
import com.example.simulation.CircuitSimulationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * مجموعه یونیت‌تست‌های جامع و صنعتی موتور شبیه‌سازی مدار الکتریکی سیمیار (حداقل ۲۰ سناریوی واقعی)
 */
class CircuitSimulationEngineTest {

    private lateinit var engine: CircuitSimulationEngine

    @Before
    fun setUp() {
        engine = CircuitSimulationEngine()
    }

    // ۱. تست بوم خالی بدون قطعه و سیم
    @Test
    fun test01_EmptyCircuit() {
        val result = engine.stepSimulation(emptyList(), emptyList())
        assertTrue("بوم خالی نباید خطای اتصال کوتاه دهد", !result.isShortCircuit)
        assertTrue("بوم خالی نباید کویل فعالی داشته باشد", result.energizedCoilTags.isEmpty())
        assertFalse("نباید اینترلاک نقض شود", result.isInterlockViolation)
    }

    // زمان نامعتبر نباید تایمر را جلو ببرد یا وضعیت پیشرفت را خراب کند.
    @Test
    fun testInvalidDeltaSecondsDoesNotAdvanceTimer() {
        val components = listOf(
            CircuitComponent("l", ComponentType.AC_PHASE, "L", 100f, 100f),
            CircuitComponent("kt", ComponentType.TIMER_ON_DELAY_COIL, "KT1", 100f, 200f, delaySeconds = 3f),
            CircuitComponent("n", ComponentType.AC_NEUTRAL, "N", 100f, 300f)
        )
        val wires = listOf(
            WireConnection("w1", "l", "L", "kt", "A1"),
            WireConnection("w2", "kt", "A2", "n", "N")
        )

        for (invalidDelta in listOf(Float.NaN, Float.POSITIVE_INFINITY, -1f)) {
            val result = engine.stepSimulation(components, wires, deltaSeconds = invalidDelta)
            val timer = result.updatedComponents.first { it.id == "kt" }
            assertEquals("زمان نامعتبر نباید پیشرفت تایمر را تغییر دهد", 0f, timer.currentTimerProgress, 0.0001f)
            assertFalse("زمان نامعتبر نباید تایمر TON را کامل کند", timer.isEnergized)
        }
    }

    // ۲. تست منبع فاز و نول بدون اتصال
    @Test
    fun test02_SourcesWithoutWires() {
        val components = listOf(
            CircuitComponent("l", ComponentType.AC_PHASE, "L", 100f, 100f),
            CircuitComponent("n", ComponentType.AC_NEUTRAL, "N", 100f, 200f)
        )
        val result = engine.stepSimulation(components, emptyList())
        assertFalse("بدون سیم اتصال کوتاه رخ نمی‌دهد", result.isShortCircuit)
        assertEquals("هیچ بوبینی نباید روشن شود", 0, result.energizedCoilTags.size)
    }

    // ۳. تست اتصال کوتاه مستقیم فاز به نول
    @Test
    fun test03_DirectShortCircuit() {
        val components = listOf(
            CircuitComponent("l", ComponentType.AC_PHASE, "L", 100f, 100f),
            CircuitComponent("n", ComponentType.AC_NEUTRAL, "N", 100f, 200f)
        )
        val wires = listOf(
            WireConnection("w1", "l", "L", "n", "N")
        )
        val result = engine.stepSimulation(components, wires)
        assertTrue("اتصال مستقیم فاز به نول باید به عنوان اتصال کوتاه شناخته شود", result.isShortCircuit)
    }

    // ۴. تست روشن شدن چراغ سیگنال
    @Test
    fun test04_PilotLampActivation() {
        val components = listOf(
            CircuitComponent("l", ComponentType.AC_PHASE, "L", 100f, 100f),
            CircuitComponent("hl", ComponentType.PILOT_LAMP_GREEN, "HL1", 100f, 200f),
            CircuitComponent("n", ComponentType.AC_NEUTRAL, "N", 100f, 300f)
        )
        val wires = listOf(
            WireConnection("w1", "l", "L", "hl", "X1"),
            WireConnection("w2", "hl", "X2", "n", "N")
        )
        val result = engine.stepSimulation(components, wires)
        val lamp = result.updatedComponents.find { it.id == "hl" }
        assertNotNull("چراغ باید وجود داشته باشد", lamp)
        assertTrue("چراغ متصل به فاز و نول باید برق‌دار و روشن شود", lamp!!.isEnergized)
    }

    // ۵. تست شستی استارت NO در حالت باز
    @Test
    fun test05_StartButtonOpen() {
        val components = listOf(
            CircuitComponent("l", ComponentType.AC_PHASE, "L", 100f, 100f),
            CircuitComponent("sb", ComponentType.PUSH_BUTTON_NO, "SB1", 100f, 200f, isClosedOrPressed = false),
            CircuitComponent("km", ComponentType.CONTACTOR_COIL, "KM1", 100f, 300f, tagBinding = "KM1"),
            CircuitComponent("n", ComponentType.AC_NEUTRAL, "N", 100f, 400f)
        )
        val wires = listOf(
            WireConnection("w1", "l", "L", "sb", "13"),
            WireConnection("w2", "sb", "14", "km", "A1"),
            WireConnection("w3", "km", "A2", "n", "N")
        )
        val result = engine.stepSimulation(components, wires)
        assertFalse("وقتی شستی استارت باز است کویل نباید فعال شود", result.energizedCoilTags.contains("KM1"))
    }

    // ۶. تست شستی استارت NO در حالت فشرده
    @Test
    fun test06_StartButtonPressed() {
        val components = listOf(
            CircuitComponent("l", ComponentType.AC_PHASE, "L", 100f, 100f),
            CircuitComponent("sb", ComponentType.PUSH_BUTTON_NO, "SB1", 100f, 200f, isClosedOrPressed = true),
            CircuitComponent("km", ComponentType.CONTACTOR_COIL, "KM1", 100f, 300f, tagBinding = "KM1"),
            CircuitComponent("n", ComponentType.AC_NEUTRAL, "N", 100f, 400f)
        )
        val wires = listOf(
            WireConnection("w1", "l", "L", "sb", "13"),
            WireConnection("w2", "sb", "14", "km", "A1"),
            WireConnection("w3", "km", "A2", "n", "N")
        )
        val result = engine.stepSimulation(components, wires)
        assertTrue("با فشردن استارت باید کویل KM1 برق‌دار شود", result.energizedCoilTags.contains("KM1"))
    }

    // ۷. تست قطع مدار توسط شستی استپ NC
    @Test
    fun test07_StopButtonPressedBreaksCircuit() {
        val components = listOf(
            CircuitComponent("l", ComponentType.AC_PHASE, "L", 100f, 100f),
            CircuitComponent("sb_stop", ComponentType.PUSH_BUTTON_NC, "Stop", 100f, 200f, isClosedOrPressed = true), // فشرده یعنی قطع
            CircuitComponent("km", ComponentType.CONTACTOR_COIL, "KM1", 100f, 300f, tagBinding = "KM1"),
            CircuitComponent("n", ComponentType.AC_NEUTRAL, "N", 100f, 400f)
        )
        val wires = listOf(
            WireConnection("w1", "l", "L", "sb_stop", "21"),
            WireConnection("w2", "sb_stop", "22", "km", "A1"),
            WireConnection("w3", "km", "A2", "n", "N")
        )
        val result = engine.stepSimulation(components, wires)
        assertFalse("با فشردن شستی استپ مدار باید قطع گردد", result.energizedCoilTags.contains("KM1"))
    }

    // ۸. تست مدار خودنگهدار کامل (Self-Hold Latch)
    @Test
    fun test08_FullSelfHoldContactor() {
        val components = listOf(
            CircuitComponent("l", ComponentType.AC_PHASE, "L", 100f, 100f),
            CircuitComponent("sb_start", ComponentType.PUSH_BUTTON_NO, "Start", 100f, 200f, isClosedOrPressed = true),
            CircuitComponent("km_aux", ComponentType.CONTACTOR_NO_AUX, "KM1 Aux", 200f, 200f, tagBinding = "KM1"),
            CircuitComponent("km_coil", ComponentType.CONTACTOR_COIL, "KM1 Coil", 100f, 300f, tagBinding = "KM1"),
            CircuitComponent("n", ComponentType.AC_NEUTRAL, "N", 100f, 400f)
        )
        val wires = listOf(
            WireConnection("w1", "l", "L", "sb_start", "13"),
            WireConnection("w2", "l", "L", "km_aux", "13"),
            WireConnection("w3", "sb_start", "14", "km_coil", "A1"),
            WireConnection("w4", "km_aux", "14", "km_coil", "A1"),
            WireConnection("w5", "km_coil", "A2", "n", "N")
        )
        val step1 = engine.stepSimulation(components, wires)
        assertTrue(step1.energizedCoilTags.contains("KM1"))

        // رها کردن شستی استارت
        val step2Comps = step1.updatedComponents.map {
            if (it.id == "sb_start") it.copy(isClosedOrPressed = false) else it
        }
        val step2 = engine.stepSimulation(step2Comps, wires)
        assertTrue("کویل باید از طریق تیغه خودنگهدار روشن باقی بماند", step2.energizedCoilTags.contains("KM1"))
    }

    // ۹. تست تریپ رله حرارتی بی‌متال (Overload Trip)
    @Test
    fun test09_BimetalOverloadTrip() {
        val components = listOf(
            CircuitComponent("l", ComponentType.AC_PHASE, "L", 100f, 100f),
            CircuitComponent("fr", ComponentType.BIMETAL_OVERLOAD, "FR1", 100f, 200f, isClosedOrPressed = true), // تریپ خورده
            CircuitComponent("km", ComponentType.CONTACTOR_COIL, "KM1", 100f, 300f, tagBinding = "KM1"),
            CircuitComponent("n", ComponentType.AC_NEUTRAL, "N", 100f, 400f)
        )
        val wires = listOf(
            WireConnection("w1", "l", "L", "fr", "95"),
            WireConnection("w2", "fr", "96", "km", "A1"),
            WireConnection("w3", "km", "A2", "n", "N")
        )
        val result = engine.stepSimulation(components, wires)
        assertFalse("با تریپ بی‌متال کنتاکتور باید قطع شود", result.energizedCoilTags.contains("KM1"))
    }

    // ۱۰. تست کنتاکت اعلام تریپ بی‌متال ۹۷-۹۸ در کنار کنتاکت NC فرمان ۹۵-۹۶
    @Test
    fun testBimetalTripAlarmContactChangesWithTripState() {
        val l = CircuitComponent("l", ComponentType.AC_PHASE, "L", 100f, 100f)
        val n = CircuitComponent("n", ComponentType.AC_NEUTRAL, "N", 100f, 400f)
        val bimetalNormal = CircuitComponent("fr", ComponentType.BIMETAL_OVERLOAD, "FR1", 100f, 200f)
        val normalLamp = CircuitComponent("normal", ComponentType.PILOT_LAMP_GREEN, "Normal", 100f, 300f)
        val tripLamp = CircuitComponent("trip", ComponentType.PILOT_LAMP_RED, "Trip", 250f, 300f)
        val wires = listOf(
            WireConnection("feed95", "l", "L", "fr", "95"),
            WireConnection("feed97", "l", "L", "fr", "97"),
            WireConnection("normalFeed", "fr", "96", "normal", "X1"),
            WireConnection("tripFeed", "fr", "98", "trip", "X1"),
            WireConnection("normalReturn", "normal", "X2", "n", "N"),
            WireConnection("tripReturn", "trip", "X2", "n", "N")
        )

        val normalResult = engine.stepSimulation(listOf(l, n, bimetalNormal, normalLamp, tripLamp), wires)
        assertTrue("در حالت عادی کنتاکت ۹۵-۹۶ باید لامپ نرمال را روشن کند",
            normalResult.updatedComponents.find { it.id == "normal" }?.isEnergized == true)
        assertFalse("در حالت عادی کنتاکت ۹۷-۹۸ باید باز بماند",
            normalResult.updatedComponents.find { it.id == "trip" }?.isEnergized == true)

        val trippedComponents = listOf(l, n, bimetalNormal.copy(isBimetalTripped = true), normalLamp, tripLamp)
        val trippedResult = engine.stepSimulation(trippedComponents, wires)
        assertFalse("پس از تریپ، کنتاکت ۹۵-۹۶ باید باز شود",
            trippedResult.updatedComponents.find { it.id == "normal" }?.isEnergized == true)
        assertTrue("پس از تریپ، کنتاکت اعلام ۹۷-۹۸ باید لامپ هشدار را روشن کند",
            trippedResult.updatedComponents.find { it.id == "trip" }?.isEnergized == true)
    }

    // ۱۰. تست شستی قارچی اضطراری (E-Stop)
    @Test
    fun test10_EmergencyStopActivation() {
        val components = listOf(
            CircuitComponent("l", ComponentType.AC_PHASE, "L", 100f, 100f),
            CircuitComponent("estop", ComponentType.EMERGENCY_STOP, "E-Stop", 100f, 200f, isClosedOrPressed = true),
            CircuitComponent("km", ComponentType.CONTACTOR_COIL, "KM1", 100f, 300f, tagBinding = "KM1"),
            CircuitComponent("n", ComponentType.AC_NEUTRAL, "N", 100f, 400f)
        )
        val wires = listOf(
            WireConnection("w1", "l", "L", "estop", "1"),
            WireConnection("w2", "estop", "2", "km", "A1"),
            WireConnection("w3", "km", "A2", "n", "N")
        )
        val result = engine.stepSimulation(components, wires)
        assertFalse("شستی قارچی فشرده باید جریان را آنی قطع کند", result.energizedCoilTags.contains("KM1"))
    }

    // ۱۱. تست کلید مینیاتوری MCB در حالت قطع
    @Test
    fun test11_CircuitBreakerOpen() {
        val components = listOf(
            CircuitComponent("l", ComponentType.AC_PHASE, "L", 100f, 100f),
            CircuitComponent("mcb", ComponentType.CIRCUIT_BREAKER_MCB, "QF1", 100f, 200f, isClosedOrPressed = false),
            CircuitComponent("hl", ComponentType.PILOT_LAMP_GREEN, "HL1", 100f, 300f),
            CircuitComponent("n", ComponentType.AC_NEUTRAL, "N", 100f, 400f)
        )
        val wires = listOf(
            WireConnection("w1", "l", "L", "mcb", "IN"),
            WireConnection("w2", "mcb", "OUT", "hl", "X1"),
            WireConnection("w3", "hl", "X2", "n", "N")
        )
        val result = engine.stepSimulation(components, wires)
        val lamp = result.updatedComponents.find { it.id == "hl" }
        assertFalse("با مینیاتوری قطع، چراغ نباید روشن شود", lamp!!.isEnergized)
    }

    // ۱۲. تست کلید مینیاتوری MCB در حالت وصل
    @Test
    fun test12_CircuitBreakerClosed() {
        val components = listOf(
            CircuitComponent("l", ComponentType.AC_PHASE, "L", 100f, 100f),
            CircuitComponent("mcb", ComponentType.CIRCUIT_BREAKER_MCB, "QF1", 100f, 200f, isClosedOrPressed = true),
            CircuitComponent("hl", ComponentType.PILOT_LAMP_GREEN, "HL1", 100f, 300f),
            CircuitComponent("n", ComponentType.AC_NEUTRAL, "N", 100f, 400f)
        )
        val wires = listOf(
            WireConnection("w1", "l", "L", "mcb", "IN"),
            WireConnection("w2", "mcb", "OUT", "hl", "X1"),
            WireConnection("w3", "hl", "X2", "n", "N")
        )
        val result = engine.stepSimulation(components, wires)
        val lamp = result.updatedComponents.find { it.id == "hl" }
        assertTrue("با مینیاتوری وصل، چراغ باید روشن شود", lamp!!.isEnergized)
    }

    // ۱۳. تست نقض اینترلاک الکتریکی چپگرد/راستگرد با interlockTag
    @Test
    fun test13_InterlockViolationDetection() {
        val components = listOf(
            CircuitComponent("l", ComponentType.AC_PHASE, "L", 100f, 100f),
            CircuitComponent("km1", ComponentType.CONTACTOR_COIL, "KM1", 100f, 200f, tagBinding = "KM1", interlockTag = "KM2"),
            CircuitComponent("km2", ComponentType.CONTACTOR_COIL, "KM2", 200f, 200f, tagBinding = "KM2", interlockTag = "KM1"),
            CircuitComponent("n", ComponentType.AC_NEUTRAL, "N", 150f, 300f)
        )
        val wires = listOf(
            WireConnection("w1", "l", "L", "km1", "A1"),
            WireConnection("w2", "km1", "A2", "n", "N"),
            WireConnection("w3", "l", "L", "km2", "A1"),
            WireConnection("w4", "km2", "A2", "n", "N")
        )
        val result = engine.stepSimulation(components, wires)
        assertTrue("برق‌دار شدن همزمان دو کنتاکتور متضاد با interlockTag باید خطای اینترلاک بدهد", result.isInterlockViolation)
    }

    // ۱۴. تست عملکرد تایمر تاخیر در وصل (ON-Delay Timer TON)
    @Test
    fun test14_TimerOnDelayAccumulation() {
        val timer = CircuitComponent(
            id = "kt",
            type = ComponentType.TIMER_ON_DELAY_COIL,
            label = "KT1",
            x = 100f,
            y = 200f,
            tagBinding = "KT1",
            delaySeconds = 3f,
            currentTimerProgress = 0f
        )
        val components = listOf(
            CircuitComponent("l", ComponentType.AC_PHASE, "L", 100f, 100f),
            timer,
            CircuitComponent("n", ComponentType.AC_NEUTRAL, "N", 100f, 300f)
        )
        val wires = listOf(
            WireConnection("w1", "l", "L", "kt", "A1"),
            WireConnection("w2", "kt", "A2", "n", "N")
        )
        // گام ۱ ثانیه
        val step1 = engine.stepSimulation(components, wires, deltaSeconds = 1f)
        val t1 = step1.updatedComponents.find { it.id == "kt" }!!
        assertEquals(1f, t1.currentTimerProgress, 0.01f)
        assertFalse("قبل از رسیدن به ۳ ثانیه نباید تایم اوت شود", t1.isEnergized)

        // گام ۲.۵ ثانیه بیشتر (مجموع ۳.۵ ثانیه >= ۳)
        val step2 = engine.stepSimulation(step1.updatedComponents, wires, deltaSeconds = 2.5f)
        val t2 = step2.updatedComponents.find { it.id == "kt" }!!
        assertTrue("پس از اتمام زمان تاخیر، تایمر باید فعال شود", t2.isEnergized)
        assertTrue(step2.energizedCoilTags.contains("KT1"))
    }

    // ۱۵. تست ریست شدن تایمر تاخیر در وصل با قطع برق
    @Test
    fun test15_TimerOnDelayResetOnDeenergize() {
        val timer = CircuitComponent(
            id = "kt",
            type = ComponentType.TIMER_ON_DELAY_COIL,
            label = "KT1",
            x = 100f,
            y = 200f,
            tagBinding = "KT1",
            delaySeconds = 3f,
            currentTimerProgress = 2f
        )
        // تغذیه قطع است
        val components = listOf(
            CircuitComponent("l", ComponentType.AC_PHASE, "L", 100f, 100f),
            timer,
            CircuitComponent("n", ComponentType.AC_NEUTRAL, "N", 100f, 300f)
        )
        val result = engine.stepSimulation(components, emptyList(), deltaSeconds = 0.1f)
        val t = result.updatedComponents.find { it.id == "kt" }!!
        assertEquals("تایمر با قطع برق باید ریست شود به ۰", 0f, t.currentTimerProgress, 0.01f)
    }

    // ۱۶. تست میکروسوئیچ باز (Limit Switch NO)
    @Test
    fun test16_LimitSwitchNO() {
        val components = listOf(
            CircuitComponent("l", ComponentType.AC_PHASE, "L", 100f, 100f),
            CircuitComponent("sq", ComponentType.LIMIT_SWITCH_NO, "SQ1", 100f, 200f, isClosedOrPressed = true),
            CircuitComponent("hl", ComponentType.PILOT_LAMP_GREEN, "HL1", 100f, 300f),
            CircuitComponent("n", ComponentType.AC_NEUTRAL, "N", 100f, 400f)
        )
        val wires = listOf(
            WireConnection("w1", "l", "L", "sq", "IN"),
            WireConnection("w2", "sq", "OUT", "hl", "X1"),
            WireConnection("w3", "hl", "X2", "n", "N")
        )
        val result = engine.stepSimulation(components, wires)
        val lamp = result.updatedComponents.find { it.id == "hl" }!!
        assertTrue("با تحریک میکروسوئیچ NO باید مدار وصل شود", lamp.isEnergized)
    }

    // ۱۷. تست کلید سلکتوری دوحالته
    @Test
    fun test17_SelectorSwitch2Pos() {
        val components = listOf(
            CircuitComponent("l", ComponentType.AC_PHASE, "L", 100f, 100f),
            CircuitComponent("sa", ComponentType.SELECTOR_SWITCH_2POS, "SA1", 100f, 200f, isClosedOrPressed = true),
            CircuitComponent("km", ComponentType.CONTACTOR_COIL, "KM1", 100f, 300f, tagBinding = "KM1"),
            CircuitComponent("n", ComponentType.AC_NEUTRAL, "N", 100f, 400f)
        )
        val wires = listOf(
            WireConnection("w1", "l", "L", "sa", "IN"),
            WireConnection("w2", "sa", "OUT", "km", "A1"),
            WireConnection("w3", "km", "A2", "n", "N")
        )
        val result = engine.stepSimulation(components, wires)
        assertTrue("سلکتور در حالت وصل باید کویل را برق‌دار کند", result.energizedCoilTags.contains("KM1"))
    }

    // ۱۸. تست جهت چرخش الکتروموتور سه‌فاز از روی ترتیب فازهای ورودی (راستگرد CW با L1-L2-L3)
    @Test
    fun test18_MotorClockwiseRotation() {
        val components = listOf(
            CircuitComponent("src", ComponentType.THREE_PHASE_SOURCE, "3P", 100f, 100f),
            CircuitComponent("m", ComponentType.MOTOR_3PHASE, "M1", 100f, 200f)
        )
        val wires = listOf(
            WireConnection("w1", "src", "L1", "m", "U"),
            WireConnection("w2", "src", "L2", "m", "V"),
            WireConnection("w3", "src", "L3", "m", "W")
        )
        val result = engine.stepSimulation(components, wires)
        val motor = result.updatedComponents.find { it.id == "m" }!!
        assertTrue("موتور با اتصال سه فاز کامل باید روشن شود", motor.isEnergized)
        assertEquals("جهت چرخش برای توالی L1-L2-L3 باید راستگرد (CW) باشد", MotorDirection.CLOCKWISE, motor.motorRotationDirection)
    }

    // ۱۹. تست جهت چرخش الکتروموتور سه‌فاز در حالت معکوس با جابجایی دو فاز (چپگرد CCW با جابجایی L1 و L3)
    @Test
    fun test19_MotorCounterClockwiseRotation() {
        val components = listOf(
            CircuitComponent("src", ComponentType.THREE_PHASE_SOURCE, "3P", 100f, 100f),
            CircuitComponent("m", ComponentType.MOTOR_3PHASE, "M1", 100f, 200f)
        )
        // جابجایی فاز ۱ و ۳: L3 به U و L1 به W
        val wires = listOf(
            WireConnection("w1", "src", "L3", "m", "U"),
            WireConnection("w2", "src", "L2", "m", "V"),
            WireConnection("w3", "src", "L1", "m", "W")
        )
        val result = engine.stepSimulation(components, wires)
        val motor = result.updatedComponents.find { it.id == "m" }!!
        assertTrue("موتور با سه فاز باید روشن شود", motor.isEnergized)
        assertEquals("با جابجایی دو فاز جهت چرخش باید چپگرد (CCW) شود", MotorDirection.COUNTER_CLOCKWISE, motor.motorRotationDirection)
    }

    // ۲۰. تست تغذیه مستقیم ۲۴ ولت DC
    @Test
    fun test20_DCPowerSupplyLoop() {
        val components = listOf(
            CircuitComponent("dc_plus", ComponentType.DC_PLUS, "+24V", 100f, 100f),
            CircuitComponent("hl", ComponentType.PILOT_LAMP_YELLOW, "HL1", 100f, 200f),
            CircuitComponent("dc_minus", ComponentType.DC_MINUS, "0V", 100f, 300f)
        )
        val wires = listOf(
            WireConnection("w1", "dc_plus", "+24V", "hl", "X1"),
            WireConnection("w2", "hl", "X2", "dc_minus", "0V")
        )
        val result = engine.stepSimulation(components, wires)
        val lamp = result.updatedComponents.find { it.id == "hl" }!!
        assertTrue("حلقه کامل DC باید لامپ را روشن کند", lamp.isEnergized)
    }

    // ۲۱. تست سیم معلق و مدار باز (Open Circuit)
    @Test
    fun test21_HangingWireOpenCircuit() {
        val components = listOf(
            CircuitComponent("l", ComponentType.AC_PHASE, "L", 100f, 100f),
            CircuitComponent("hl", ComponentType.PILOT_LAMP_RED, "HL1", 100f, 200f)
        )
        // سیم فقط به فاز و لامپ وصل است اما نول وجود ندارد
        val wires = listOf(
            WireConnection("w1", "l", "L", "hl", "X1")
        )
        val result = engine.stepSimulation(components, wires)
        val lamp = result.updatedComponents.find { it.id == "hl" }!!
        assertFalse("بدون برگشت نول، لامپ نباید روشن شود", lamp.isEnergized)
        assertTrue("باید وضعیت مدار باز تشخیص داده شود", result.isOpenCircuit)
    }

    // ۲۲. تست اتصال کوتاه فاز به فاز (L1 با L2)
    @Test
    fun test22_PhaseToPhaseShortCircuit() {
        val components = listOf(
            CircuitComponent("src", ComponentType.THREE_PHASE_SOURCE, "3P", 100f, 100f)
        )
        // اتصال مستقیم بین پایانه‌های L1 و L2 منبع سه‌فاز
        val wires = listOf(
            WireConnection("w1", "src", "L1", "src", "L2")
        )
        val result = engine.stepSimulation(components, wires)
        assertTrue("اتصال بین دو فاز L1 و L2 باید اتصال کوتاه فاز-به-فاز تشخیص داده شود", result.isShortCircuit)
    }

    // ۲۳. تست عملکرد واقعی تایمر تاخیر در قطع (TOF)
    @Test
    fun test23_TimerOffDelayTrueOperation() {
        val tof = CircuitComponent(
            id = "kt_tof",
            type = ComponentType.TIMER_OFF_DELAY_COIL,
            label = "KT_TOF",
            x = 100f,
            y = 200f,
            tagBinding = "KT2",
            delaySeconds = 2f
        )
        val l = CircuitComponent("l", ComponentType.AC_PHASE, "L", 100f, 100f)
        val n = CircuitComponent("n", ComponentType.AC_NEUTRAL, "N", 100f, 300f)
        val wires = listOf(
            WireConnection("w1", "l", "L", "kt_tof", "A1"),
            WireConnection("w2", "kt_tof", "A2", "n", "N")
        )

        // ۱. با وصل برق، بوبین TOF فوراً وصل می‌شود
        val step1 = engine.stepSimulation(listOf(l, tof, n), wires, deltaSeconds = 0.1f)
        val t1 = step1.updatedComponents.find { it.id == "kt_tof" }!!
        assertTrue("تایمر TOF با رسیدن ولتاژ باید فوراً فعال شود", t1.isEnergized)

        // ۲. قطع برق ورودی، گام ۱ ثانیه (کمتر از تاخیر ۲ ثانیه): همچنان روشن می‌ماند
        val step2 = engine.stepSimulation(listOf(l, t1, n), emptyList(), deltaSeconds = 1f)
        val t2 = step2.updatedComponents.find { it.id == "kt_tof" }!!
        assertTrue("تایمر TOF تا پایان زمان تاخیر باید روشن باقی بماند", t2.isEnergized)

        // ۳. گذشت ۱.۵ ثانیه دیگر (مجموع ۲.۵ ثانیه >= ۲): خروجی قطع می‌شود
        val step3 = engine.stepSimulation(listOf(l, t2, n), emptyList(), deltaSeconds = 1.5f)
        val t3 = step3.updatedComponents.find { it.id == "kt_tof" }!!
        assertFalse("پس از اتمام زمان تاخیر در قطع، تایمر TOF باید خاموش شود", t3.isEnergized)
    }

    // ۲۴. تست رفتار شبیه‌سازی سنسور القایی مجاورتی (Proximity Sensor)
    @Test
    fun test24_ProximitySensorBehavior() {
        val dcPlus = CircuitComponent("dc_plus", ComponentType.DC_PLUS, "+24V", 100f, 100f)
        val prox = CircuitComponent("prox", ComponentType.PROXIMITY_SENSOR, "BK1", 100f, 200f, isClosedOrPressed = true)
        val lamp = CircuitComponent("lamp", ComponentType.PILOT_LAMP_GREEN, "HL1", 100f, 300f)
        val dcMinus = CircuitComponent("dc_minus", ComponentType.DC_MINUS, "0V", 100f, 400f)

        val wires = listOf(
            WireConnection("w1", "dc_plus", "+24V", "prox", "V+"),
            WireConnection("w2", "dc_minus", "0V", "prox", "GND"),
            WireConnection("w3", "prox", "OUT", "lamp", "X1"),
            WireConnection("w4", "lamp", "X2", "dc_minus", "0V")
        )

        val result = engine.stepSimulation(listOf(dcPlus, prox, lamp, dcMinus), wires)
        val updatedProx = result.updatedComponents.find { it.id == "prox" }!!
        val updatedLamp = result.updatedComponents.find { it.id == "lamp" }!!
        assertTrue("سنسور القایی تحریک شده باید فعال باشد", updatedProx.isEnergized)
        assertTrue("خروجی سنسور باید لامپ را روشن کند", updatedLamp.isEnergized)
    }

    // ۲۵. تست قالب آماده راه‌اندازی مستقیم (DOL) بدون خطای کاذب
    @Test
    fun test25_TemplateDolStarterExecution() {
        val project = com.example.model.DefaultCircuitTemplates.createDirectOnlineStarter()
        // حالت اولیه قبل از زدن استارت: نباید هیچ خطای اتصال کوتاه، اینترلاک یا مدار باز کاذب داشته باشد
        val initialStep = engine.stepSimulation(project.components, project.wires)
        assertFalse("مدار DOL نباید خطای اتصال کوتاه دهد", initialStep.isShortCircuit)
        assertFalse("مدار DOL قبل از استارت نباید مدار باز کاذب گزارش کند", initialStep.isOpenCircuit)

        // زدن شستی استارت SB2
        val pressedStart = project.components.map {
            if (it.id == "sb2") it.copy(isClosedOrPressed = true) else it
        }
        val step1 = engine.stepSimulation(pressedStart, project.wires, deltaSeconds = 0.05f)
        assertTrue("کویل KM1 باید برق‌دار شود", step1.energizedCoilTags.contains("KM1"))

        // رها شدن استارت (خودنگهدار KM1 مدار را وصل نگه می‌دارد)
        val releasedStart = step1.updatedComponents.map {
            if (it.id == "sb2") it.copy(isClosedOrPressed = false) else it
        }
        val step2 = engine.stepSimulation(releasedStart, project.wires, deltaSeconds = 0.05f)
        assertTrue("کویل KM1 باید با خودنگهدار روشن بماند", step2.energizedCoilTags.contains("KM1"))
        val motor = step2.updatedComponents.find { it.id == "m1" }!!
        assertTrue("موتور سه‌فاز باید با کنتاکت قدرت روشن شده باشد", motor.isEnergized)
        assertEquals("جهت چرخش موتور باید ساعتگرد (راستگرد) باشد", MotorDirection.CLOCKWISE, motor.motorRotationDirection)
    }

    // ۲۶. تست قالب آماده ستاره-مثلث (Star-Delta) با سوئیچ زماندار
    @Test
    fun test26_TemplateStarDeltaExecution() {
        val project = com.example.model.DefaultCircuitTemplates.createStarDeltaStarter()
        val initialStep = engine.stepSimulation(project.components, project.wires)
        assertFalse("مدار ستاره-مثلث نباید خطای اتصال کوتاه دهد", initialStep.isShortCircuit)
        assertFalse("مدار ستاره-مثلث نباید مدار باز کاذب دهد", initialStep.isOpenCircuit)

        // فشرده شدن استارت: حالت ستاره (KM1 اصلی + KM2 ستاره)
        val pressedStart = project.components.map {
            if (it.id == "sb2") it.copy(isClosedOrPressed = true) else it
        }
        val stepStar = engine.stepSimulation(pressedStart, project.wires, deltaSeconds = 0.1f)
        assertTrue("کنتاکتور اصلی KM1 باید روشن باشد", stepStar.energizedCoilTags.contains("KM1"))
        assertTrue("کنتاکتور ستاره KM2 باید روشن باشد", stepStar.energizedCoilTags.contains("KM2"))
        assertFalse("کنتاکتور مثلث KM3 در فاز اول نباید روشن باشد", stepStar.energizedCoilTags.contains("KM3"))

        // گذشت ۴.۵ ثانیه (تایمر ۴ ثانیه تمام می‌شود): سوئیچ به مثلث (KM1 اصلی + KM3 مثلث)
        var currentComps = stepStar.updatedComponents.map {
            if (it.id == "sb2") it.copy(isClosedOrPressed = false) else it
        }
        for (i in 1..45) {
            val s = engine.stepSimulation(currentComps, project.wires, deltaSeconds = 0.1f)
            currentComps = s.updatedComponents
        }
        val finalKm1 = currentComps.find { it.id == "km1_coil" }!!
        val finalKm2 = currentComps.find { it.id == "km2_coil" }!!
        val finalKm3 = currentComps.find { it.id == "km3_coil" }!!

        assertTrue("کنتاکتور اصلی KM1 همچنان روشن است", finalKm1.isEnergized)
        assertFalse("کنتاکتور ستاره KM2 باید قطع شده باشد", finalKm2.isEnergized)
        assertTrue("کنتاکتور مثلث KM3 باید وصل شده باشد", finalKm3.isEnergized)
    }

    // ۲۷. تست قالب آماده چپگرد/راستگرد (Forward-Reverse) با تغییر جهت چرخش واقعی
    @Test
    fun test27_TemplateForwardReverseExecution() {
        val project = com.example.model.DefaultCircuitTemplates.createForwardReverseStarter()
        val initialStep = engine.stepSimulation(project.components, project.wires)
        assertFalse("نباید اتصال کوتاه داشته باشد", initialStep.isShortCircuit)
        assertFalse("نباید مدار باز کاذب داشته باشد", initialStep.isOpenCircuit)

        // ۱. استارت راستگرد
        val startFwd = project.components.map {
            if (it.id == "sb_fwd") it.copy(isClosedOrPressed = true) else it
        }
        val stepFwd1 = engine.stepSimulation(startFwd, project.wires, deltaSeconds = 0.05f)
        assertTrue(stepFwd1.energizedCoilTags.contains("KM1"))
        // گام دوم: با وصل بودن خودنگهدار و بسته شدن کنتاکت قدرت، موتور روشن می‌شود
        val stepFwd = engine.stepSimulation(stepFwd1.updatedComponents, project.wires, deltaSeconds = 0.05f)
        val motorFwd = stepFwd.updatedComponents.find { it.id == "m1" }!!
        assertTrue(motorFwd.isEnergized)
        assertEquals("راستگرد باید ساعتگرد (CW) باشد", MotorDirection.CLOCKWISE, motorFwd.motorRotationDirection)

        // ۲. استپ مدار
        val stopped = stepFwd.updatedComponents.map {
            if (it.id == "sb_fwd") it.copy(isClosedOrPressed = false)
            else if (it.id == "sb1") it.copy(isClosedOrPressed = true)
            else it
        }
        val stepStop = engine.stepSimulation(stopped, project.wires, deltaSeconds = 0.05f)
        assertFalse(stepStop.energizedCoilTags.contains("KM1"))

        // ۳. استارت چپگرد
        val startRev = stepStop.updatedComponents.map {
            if (it.id == "sb1") it.copy(isClosedOrPressed = false)
            else if (it.id == "sb_rev") it.copy(isClosedOrPressed = true)
            else it
        }
        val stepRev1 = engine.stepSimulation(startRev, project.wires, deltaSeconds = 0.05f)
        assertTrue(stepRev1.energizedCoilTags.contains("KM2"))
        val stepRev = engine.stepSimulation(stepRev1.updatedComponents, project.wires, deltaSeconds = 0.05f)
        val motorRev = stepRev.updatedComponents.find { it.id == "m1" }!!
        assertTrue(motorRev.isEnergized)
        assertEquals("چپگرد با جابجایی دو فاز باید پادساعتگرد (CCW) باشد", MotorDirection.COUNTER_CLOCKWISE, motorRev.motorRotationDirection)
    }

    // ۲۸. تست خودنگهدار واقعی برنامه نردبانی لدر پیش‌فرض PLC
    @Test
    fun test28_DefaultPlcLadderSelfHold() {
        val plcEngine = com.example.simulation.PlcLadderEngine()
        val defaultRungs = com.example.model.DefaultCircuitTemplates.createDefaultLadderProgram()
        val memory = com.example.model.PlcMemory()

        // استپ وصل است (I0.0 = false در NC)، زدن استارت (I0.1 = true)
        var currentMemory = memory.setInput("I0.1", true)
        val step1 = plcEngine.executeCycle(defaultRungs, currentMemory)
        assertTrue("با زدن استارت خروجی Q0.0 باید روشن شود", step1.second.outputs["Q0.0"] == true)

        // رها شدن استارت (I0.1 = false): به دلیل شاخه موازی خودنگهدار Q0.0، خروجی باید روشن بماند
        currentMemory = step1.second.setInput("I0.1", false)
        val step2 = plcEngine.executeCycle(step1.first, currentMemory)
        assertTrue("با رها شدن شستی استارت خروجی Q0.0 با خودنگهدار باید روشن بماند", step2.second.outputs["Q0.0"] == true)

        // فعال شدن شستی استپ (I0.0 = true): تیغه NC باز شده و خروجی قطع می‌شود
        currentMemory = step2.second.setInput("I0.0", true)
        val step3 = plcEngine.executeCycle(step2.first, currentMemory)
        assertFalse("با زدن استپ خروجی Q0.0 باید قطع شود", step3.second.outputs["Q0.0"] == true)
    }
}
