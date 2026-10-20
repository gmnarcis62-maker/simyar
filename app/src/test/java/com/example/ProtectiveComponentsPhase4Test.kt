package com.example

import com.example.model.CircuitComponent
import com.example.model.ComponentType
import com.example.model.IndustrialComponentLibrary
import com.example.model.SpecSourceType
import com.example.model.WireConnection
import com.example.simulation.CircuitSimulationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * تست‌های جامع مرحله P4 (دسته A: تجهیزات حفاظتی و رله‌های تبدیلی)
 */
class ProtectiveComponentsPhase4Test {

    private lateinit var engine: CircuitSimulationEngine

    @Before
    fun setUp() {
        engine = CircuitSimulationEngine()
    }

    // ۱. تست کلید مینیاتوری سه‌پل (3P MCB): قطع و وصل همزمان هر سه فاز
    @Test
    fun test01_MCB_3Pole_SimultaneousSwitching() {
        val src = CircuitComponent("src", ComponentType.THREE_PHASE_SOURCE, "3P_SRC", 100f, 100f)
        val mcbOpen = CircuitComponent("mcb1", ComponentType.CIRCUIT_BREAKER_MCB_3P, "QF1", 100f, 200f, isClosedOrPressed = false)
        val motor = CircuitComponent("m1", ComponentType.MOTOR_3PHASE, "M1", 100f, 300f)

        val wires = listOf(
            WireConnection("w1", "src", "L1", "mcb1", "1"),
            WireConnection("w2", "src", "L2", "mcb1", "3"),
            WireConnection("w3", "src", "L3", "mcb1", "5"),
            WireConnection("w4", "mcb1", "2", "m1", "U"),
            WireConnection("w5", "mcb1", "4", "m1", "V"),
            WireConnection("w6", "mcb1", "6", "m1", "W")
        )

        // الف) وقتی کلید مینیاتوری باز است، هیچ فازی عبور نمی‌کند و موتور خاموش است
        val resultOpen = engine.stepSimulation(listOf(src, mcbOpen, motor), wires)
        val motorWhenOpen = resultOpen.updatedComponents.find { it.id == "m1" }
        assertFalse("با قطع کلید مینیاتوری سه‌پل موتور باید خاموش باشد", motorWhenOpen?.isEnergized == true)

        // ب) با بستن کلید مینیاتوری (isClosedOrPressed = true)، هر سه فاز همزمان وصل شده و موتور روشن می‌شود
        val mcbClosed = mcbOpen.copy(isClosedOrPressed = true)
        val resultClosed = engine.stepSimulation(listOf(src, mcbClosed, motor), wires)
        val motorWhenClosed = resultClosed.updatedComponents.find { it.id == "m1" }
        assertTrue("با وصل کلید مینیاتوری سه‌پل موتور سه‌فاز باید روشن شود", motorWhenClosed?.isEnergized == true)
    }

    // ۲. تست کلید اتوماتیک کمپکت سه‌پل (3P MCCB): قطع و وصل ۳ فاز و بازه جریان نمونه قابل تنظیم
    @Test
    fun test02_MCCB_3Pole_BehaviorAndSpecs() {
        val src = CircuitComponent("src", ComponentType.THREE_PHASE_SOURCE, "3P_SRC", 100f, 100f)
        val mccb = CircuitComponent("mccb1", ComponentType.CIRCUIT_BREAKER_MCCB_3P, "QF_MAIN", 100f, 200f, isClosedOrPressed = true)
        val motor = CircuitComponent("m1", ComponentType.MOTOR_3PHASE, "M1", 100f, 300f)

        val wires = listOf(
            WireConnection("w1", "src", "L1", "mccb1", "1"),
            WireConnection("w2", "src", "L2", "mccb1", "3"),
            WireConnection("w3", "src", "L3", "mccb1", "5"),
            WireConnection("w4", "mccb1", "2", "m1", "U"),
            WireConnection("w5", "mccb1", "4", "m1", "V"),
            WireConnection("w6", "mccb1", "6", "m1", "W")
        )

        val result = engine.stepSimulation(listOf(src, mccb, motor), wires)
        val motorResult = result.updatedComponents.find { it.id == "m1" }
        assertTrue("کلید اتوماتیک کمپکت باید هر ۳ فاز را به موتور متصل کند", motorResult?.isEnergized == true)

        // بررسی مشخصات فنی کلید اتوماتیک در کتابخانه قطعات
        val mccbLibItem = IndustrialComponentLibrary.items.find { it.type == ComponentType.CIRCUIT_BREAKER_MCCB_3P }
        assertNotNull("کلید اتوماتیک سه‌پل باید در کتابخانه قطعات موجود باشد", mccbLibItem)
        assertTrue("بازه جریان باید نمونه و قابل تنظیم باشد", mccbLibItem!!.defaultSpecs.isSampleValue)
        assertEquals(SpecSourceType.SAMPLE, mccbLibItem.defaultSpecs.currentSource.type)
        assertEquals(SpecSourceType.STANDARD, mccbLibItem.defaultSpecs.standardCodeSource.type)
    }

    // ۳. تست پایه فیوز سه‌پل (3P Fuse Base) با پایانه‌های ۱-۲، ۳-۴، ۵-۶
    @Test
    fun test03_Fuse3P_Conduction() {
        val src = CircuitComponent("src", ComponentType.THREE_PHASE_SOURCE, "3P_SRC", 100f, 100f)
        val fuse = CircuitComponent("fu1", ComponentType.FUSE_3P, "FU1", 100f, 200f, isClosedOrPressed = true)
        val motor = CircuitComponent("m1", ComponentType.MOTOR_3PHASE, "M1", 100f, 300f)

        val wires = listOf(
            WireConnection("w1", "src", "L1", "fu1", "1"),
            WireConnection("w2", "src", "L2", "fu1", "3"),
            WireConnection("w3", "src", "L3", "fu1", "5"),
            WireConnection("w4", "fu1", "2", "m1", "U"),
            WireConnection("w5", "fu1", "4", "m1", "V"),
            WireConnection("w6", "fu1", "6", "m1", "W")
        )

        val result = engine.stepSimulation(listOf(src, fuse, motor), wires)
        val motorResult = result.updatedComponents.find { it.id == "m1" }
        assertTrue("پایه فیوز سه‌پل باید مسیرهای ۱-۲، ۳-۴، ۵-۶ را عبور دهد", motorResult?.isEnergized == true)
    }

    // ۴. تست کلید محافظ جان ۴ پل (4P RCD/RCCB): ورودی و خروجی فازها و نول
    @Test
    fun test04_RCD_4P_Switching() {
        val l = CircuitComponent("l", ComponentType.AC_PHASE, "L", 100f, 50f)
        val n = CircuitComponent("n", ComponentType.AC_NEUTRAL, "N", 100f, 70f)
        val rcdClosed = CircuitComponent("rcd1", ComponentType.RCD_4P, "Q_RCD", 100f, 150f, isClosedOrPressed = true)
        val lamp = CircuitComponent("hl", ComponentType.PILOT_LAMP_GREEN, "HL1", 100f, 250f)

        val wires = listOf(
            WireConnection("w1", "l", "L", "rcd1", "1"),
            WireConnection("w2", "n", "N", "rcd1", "N"),
            WireConnection("w3", "rcd1", "2", "hl", "X1"),
            WireConnection("w4", "rcd1", "N_OUT", "hl", "X2")
        )

        // در حالت وصل بودن کلید محافظ جان
        val resultClosed = engine.stepSimulation(listOf(l, n, rcdClosed, lamp), wires)
        val lampClosed = resultClosed.updatedComponents.find { it.id == "hl" }
        assertTrue("با وصل بودن RCD لامپ سیگنال باید روشن شود", lampClosed?.isEnergized == true)

        // با قطع دستی کلید محافظ جان
        val rcdOpen = rcdClosed.copy(isClosedOrPressed = false)
        val resultOpen = engine.stepSimulation(listOf(l, n, rcdOpen, lamp), wires)
        val lampOpen = resultOpen.updatedComponents.find { it.id == "hl" }
        assertFalse("با قطع دستی RCD مدار باید خاموش شود", lampOpen?.isEnergized == true)

        // اطمینان از وجود ترمینال دکمه تست
        val terminals = rcdClosed.getTerminals()
        assertTrue("کلید محافظ جان باید دارای پایانه دکمه تست باشد", terminals.any { it.id == "TEST" })
    }

    // ۵. تست رله کمکی ۱ و ۲ کنتاکت تبدیلی: کویل A1-A2 و کنتاکت‌های ۱۱-۱۲-۱۴ و ۲۱-۲۲-۲۴
    @Test
    fun test05_ControlRelay_ChangeoverContacts() {
        val l = CircuitComponent("l", ComponentType.AC_PHASE, "L", 100f, 50f)
        val n = CircuitComponent("n", ComponentType.AC_NEUTRAL, "N", 100f, 70f)

        // رله ۲ کنتاکت تبدیلی KA1
        val relay = CircuitComponent("ka1", ComponentType.CONTROL_RELAY_2CO, "KA1", 100f, 150f)
        val lampNC = CircuitComponent("hl_nc", ComponentType.PILOT_LAMP_RED, "HL_OFF", 100f, 300f)
        val lampNO = CircuitComponent("hl_no", ComponentType.PILOT_LAMP_GREEN, "HL_ON", 200f, 300f)

        // سیم‌بندی کنتاکت اول تبدیلی ۱۱-۱۲-۱۴
        val contactWires = listOf(
            WireConnection("w_feed", "l", "L", "ka1", "11"),       // فاز ورودی به پایه مشترک ۱۱
            WireConnection("w_nc", "ka1", "12", "hl_nc", "X1"),   // پایه بسته ۱۲ به لامپ قرمز
            WireConnection("w_no", "ka1", "14", "hl_no", "X1"),   // پایه باز ۱۴ به لامپ سبز
            WireConnection("w_n1", "hl_nc", "X2", "n", "N"),
            WireConnection("w_n2", "hl_no", "X2", "n", "N")
        )

        // الف) وقتی بوبین A1-A2 برق‌دار نیست: کنتاکت بسته ۱۱-۱۲ وصل و لامپ قرمز روشن است؛ لامپ سبز خاموش است
        val resDeenergized = engine.stepSimulation(listOf(l, n, relay, lampNC, lampNO), contactWires)
        val redLamp1 = resDeenergized.updatedComponents.find { it.id == "hl_nc" }
        val greenLamp1 = resDeenergized.updatedComponents.find { it.id == "hl_no" }
        assertTrue("در حالت خاموش بودن رله، کنتاکت NC (11-12) باید وصل باشد", redLamp1?.isEnergized == true)
        assertFalse("در حالت خاموش بودن رله، کنتاکت NO (11-14) باید قطع باشد", greenLamp1?.isEnergized == true)

        // ب) برق‌دار کردن بوبین A1-A2 رله
        val coilWires = contactWires + listOf(
            WireConnection("w_a1", "l", "L", "ka1", "A1"),
            WireConnection("w_a2", "ka1", "A2", "n", "N")
        )

        // گام اول: تحریک بوبین رله
        val step1 = engine.stepSimulation(listOf(l, n, relay, lampNC, lampNO), coilWires)
        val updatedRelay = step1.updatedComponents.find { it.id == "ka1" }
        assertTrue("بوبین رله با اعمال فاز و نول باید برق‌دار شود", updatedRelay?.isEnergized == true)

        // گام دوم: انتشار تغییر وضعیت کنتاکت تبدیلی (۱۱ به ۱۴ متصل و ۱۱ از ۱۲ قطع می‌شود)
        val step2 = engine.stepSimulation(step1.updatedComponents, coilWires)
        val redLamp2 = step2.updatedComponents.find { it.id == "hl_nc" }
        val greenLamp2 = step2.updatedComponents.find { it.id == "hl_no" }
        assertFalse("با تحریک بوبین، کنتاکت NC باز شده و لامپ قرمز باید خاموش شود", redLamp2?.isEnergized == true)
        assertTrue("با تحریک بوبین، کنتاکت NO بسته شده و لامپ سبز باید روشن شود", greenLamp2?.isEnergized == true)
    }

    // ۶. تست رله کنترل فاز: تشخیص حضور هر سه فاز و صحت توالی فازها (L1-L2-L3)
    @Test
    fun test06_PhaseSequenceRelay_HealthyAndFaultConditions() {
        val src = CircuitComponent("src", ComponentType.THREE_PHASE_SOURCE, "3P_SRC", 100f, 50f)
        val neutral = CircuitComponent("n", ComponentType.AC_NEUTRAL, "N", 100f, 70f)
        val phaseRelay = CircuitComponent("kf1", ComponentType.PHASE_SEQUENCE_RELAY, "KF1", 100f, 150f)
        val lampNormal = CircuitComponent("hl_norm", ComponentType.PILOT_LAMP_GREEN, "HL_NORM", 100f, 300f)
        val lampFault = CircuitComponent("hl_flt", ComponentType.PILOT_LAMP_RED, "HL_FAULT", 200f, 300f)

        // سیم‌بندی کنتاکت خروجی تبدیلی ۱۵ (مشترک)، ۱۶ (خطا NC)، ۱۸ (سالم NO)
        val baseContactWires = listOf(
            WireConnection("w_c15", "src", "L1", "kf1", "15"),          // تغذیه مشترک ۱۵ با فاز L1
            WireConnection("w_c16", "kf1", "16", "hl_flt", "X1"),       // کنتاکت خطا ۱۶ به لامپ قرمز
            WireConnection("w_c18", "kf1", "18", "hl_norm", "X1"),
            WireConnection("w_n1", "hl_flt", "X2", "n", "N"),
            WireConnection("w_n2", "hl_norm", "X2", "n", "N")
        )

        // سناریوی الف: وضعیت کاملاً نرمال (هر سه فاز L1-L2-L3 با توالی مستقیم متصلند)
        val healthyWires = baseContactWires + listOf(
            WireConnection("w_l1", "src", "L1", "kf1", "L1"),
            WireConnection("w_l2", "src", "L2", "kf1", "L2"),
            WireConnection("w_l3", "src", "L3", "kf1", "L3")
        )

        val step1Healthy = engine.stepSimulation(listOf(src, neutral, phaseRelay, lampNormal, lampFault), healthyWires)
        val relayStateHealthy = step1Healthy.updatedComponents.find { it.id == "kf1" }
        assertTrue("با حضور هر ۳ فاز در توالی صحیح، رله کنترل فاز باید فعال شود", relayStateHealthy?.isEnergized == true)

        // گام دوم: رله فعال است بنابراین کنتاکت ۱۵ به ۱۸ وصل شده و لامپ نرمال سبز روشن می‌شود
        val step2Healthy = engine.stepSimulation(step1Healthy.updatedComponents, healthyWires)
        val greenResult = step2Healthy.updatedComponents.find { it.id == "hl_norm" }
        val redResult = step2Healthy.updatedComponents.find { it.id == "hl_flt" }
        assertTrue("لامپ وضعیت نرمال باید روشن شود", greenResult?.isEnergized == true)
        assertFalse("لامپ خطا نباید روشن باشد", redResult?.isEnergized == true)

        // سناریوی ب: خطای قطع فاز (دوفاز شدن - قطع فاز L2)
        val missingPhaseWires = baseContactWires + listOf(
            WireConnection("w_l1", "src", "L1", "kf1", "L1"),
            WireConnection("w_l3", "src", "L3", "kf1", "L3")
            // فاز L2 متصل نیست
        )

        val stepMissingPhase = engine.stepSimulation(listOf(src, neutral, phaseRelay, lampNormal, lampFault), missingPhaseWires)
        val relayStateMissing = stepMissingPhase.updatedComponents.find { it.id == "kf1" }
        assertFalse("در صورت قطع یک فاز، رله کنترل فاز باید قطع شود", relayStateMissing?.isEnergized == true)

        // سناریوی ج: خطای جابجایی و توالی غلط فازها (L1 و L2 جابجا وصل شده‌اند)
        val invertedSequenceWires = baseContactWires + listOf(
            WireConnection("w_l1_rev", "src", "L2", "kf1", "L1"), // فاز L2 به ورودی L1 وصل شده
            WireConnection("w_l2_rev", "src", "L1", "kf1", "L2"), // فاز L1 به ورودی L2 وصل شده
            WireConnection("w_l3_rev", "src", "L3", "kf1", "L3")
        )

        val stepInverted = engine.stepSimulation(listOf(src, neutral, phaseRelay, lampNormal, lampFault), invertedSequenceWires)
        val relayStateInverted = stepInverted.updatedComponents.find { it.id == "kf1" }
        assertFalse("در صورت توالی نادرست فازها، رله کنترل فاز نباید فعال شود", relayStateInverted?.isEnergized == true)
    }

    // ۷. تست اعتبارسنجی SpecSource برای تمامی قطعات حفاظتی جدید در کتابخانه قطعات
    @Test
    fun test07_AllProtectiveItemsHaveSpecSource() {
        val protectiveTypes = listOf(
            ComponentType.CIRCUIT_BREAKER_MCB_3P,
            ComponentType.CIRCUIT_BREAKER_MCCB_3P,
            ComponentType.FUSE_3P,
            ComponentType.RCD_4P,
            ComponentType.CONTROL_RELAY_1CO,
            ComponentType.CONTROL_RELAY_2CO,
            ComponentType.PHASE_SEQUENCE_RELAY
        )

        for (type in protectiveTypes) {
            val item = IndustrialComponentLibrary.items.find { it.type == type }
            assertNotNull("قطعه ${type.name} باید در کتابخانه قطعات ثبت شده باشد", item)
            val specs = item!!.defaultSpecs
            assertNotNull("مشخصه voltageSource نباید تهی باشد", specs.voltageSource)
            assertNotNull("مشخصه currentSource نباید تهی باشد", specs.currentSource)
            assertNotNull("مشخصه standardCodeSource نباید تهی باشد", specs.standardCodeSource)
            assertTrue("کد استاندارد باید معتبر باشد", specs.standardCode.isNotBlank())
        }
    }
}
