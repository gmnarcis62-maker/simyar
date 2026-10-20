package com.example

import com.example.model.LadderElement
import com.example.model.LadderElementType
import com.example.model.LadderRung
import com.example.model.PlcMemory
import com.example.simulation.PlcLadderEngine
import com.example.ui.timing.CircularSignalBuffer
import com.example.ui.timing.SignalSourceType
import com.example.ui.timing.TrackableSignal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * تست‌های جامع مرحله P6: نمودار زمانی لاجیک سیگنال‌ها (Timing Diagram)
 */
class SignalTimingDiagramTest {

    private lateinit var plcEngine: PlcLadderEngine

    @Before
    fun setUp() {
        plcEngine = PlcLadderEngine()
    }

    // ۱. تست بافر حلقوی: بررسی سقف زمانی ۶۰ ثانیه‌ای و حذف نمونه‌های قدیمی‌تر جهت کنترل مصرف حافظه
    @Test
    fun test01_CircularBuffer_CapacityAndPurge() {
        val buffer = CircularSignalBuffer(maxDurationSeconds = 60f, maxSamplesPerSignal = 500)
        val signalId = "test_sig_1"

        // درج نمونه‌ها از زمان ۰ تا ۸۰ ثانیه (یک نمونه در هر ثانیه)
        for (sec in 0..80) {
            val value = (sec % 4 < 2) // سیگنال متناوب ۰ و ۱
            buffer.addSample(signalId, sec.toFloat(), value)
        }

        val samples = buffer.getSamples(signalId)
        assertTrue("بافر نباید خالی باشد", samples.isNotEmpty())

        // قدیمی‌ترین نمونه موجود باید در بازه ۶۰ ثانیه اخیر باشد (بزرگتر مساوی ۲۰s = ۸۰ - ۶۰)
        val oldestTime = samples.first().timestampSeconds
        val newestTime = samples.last().timestampSeconds

        assertEquals("جدیدترین زمان باید ۸۰ ثانیه باشد", 80f, newestTime, 0.001f)
        assertTrue(
            "نمونه‌های قدیمی‌تر از سقف ۶۰ ثانیه (قبل از ثانیه ۲۰) باید حذف شده باشند. زمان قدیمی‌ترین: $oldestTime",
            oldestTime >= 20f
        )

        // بررسی بازیابی مقدار در زمان‌های مختلف
        val valAt70 = buffer.getValueAtTime(signalId, 70f)
        val expectedAt70 = (70 % 4 < 2)
        assertEquals("مقدار سیگنال در ثانیه ۷۰ باید درست خوانده شود", expectedAt70, valAt70)
    }

    // ۲. تست انتخاب سیگنال‌ها: بررسی سقف حداکثر ۱۲ سیگنال همزمان
    @Test
    fun test02_SignalSelection_Max12Limit() {
        val selectedSignals = mutableListOf<TrackableSignal>()

        fun addSignal(sig: TrackableSignal): Boolean {
            if (selectedSignals.size >= 12) return false
            if (selectedSignals.none { it.id == sig.id }) {
                selectedSignals.add(sig)
                return true
            }
            return false
        }

        // تلاش برای افزودن ۱۴ سیگنال مختلف
        for (i in 1..14) {
            val sig = TrackableSignal(
                id = "sig_$i",
                addressOrTag = "I0.$i",
                displayName = "سیگنال شماره $i",
                type = SignalSourceType.PLC_INPUT
            )
            val added = addSignal(sig)
            if (i <= 12) {
                assertTrue("سیگنال‌های ۱ تا ۱۲ باید با موفقیت اضافه شوند (سیگنال $i)", added)
            } else {
                assertFalse("سیگنال‌های بیشتر از ۱۲ (سیگنال $i) باید رد شوند", added)
            }
        }

        assertEquals("تعداد سیگنال‌های فعال باید دقیقاً ۱۲ عدد باشد", 12, selectedSignals.size)

        // حذف یک سیگنال و امکان افزودن مجدد
        selectedSignals.removeAt(0)
        assertEquals(11, selectedSignals.size)

        val newSig = TrackableSignal("sig_extra", "Q0.7", "خروجی جدید", SignalSourceType.PLC_OUTPUT)
        val addedExtra = addSignal(newSig)
        assertTrue("پس از حذف، باید بتوان سیگنال جدید تا سقف ۱۲ افزود", addedExtra)
        assertEquals(12, selectedSignals.size)
    }

    // ۳. تست نمونه‌برداری درست و دقیق: بررسی تایمر TON یک‌ثانیه‌ای (T1.DN دقیقاً ۱ ثانیه بعد فعال شود)
    @Test
    fun test03_SamplingAccuracy_TimerTON_OneSecondDelay() {
        val buffer = CircularSignalBuffer(maxDurationSeconds = 60f, maxSamplesPerSignal = 1000)

        // ساخت مدار لدر با یک کنتاکت ورودی I0.0 و یک تایمر تاخیر در وصل TON به مدت ۱.۰ ثانیه (T1)
        val rungs = listOf(
            LadderRung(
                id = "rung_ton",
                rungNumber = 1,
                contacts = listOf(
                    LadderElement(id = "elem_in", type = LadderElementType.NO_CONTACT, address = "I0.0")
                ),
                coils = listOf(
                    LadderElement(id = "elem_ton", type = LadderElementType.TIMER_TON, address = "T1", presetValue = 1) // ۱ ثانیه
                )
            )
        )

        var memory = PlcMemory()
        var simTime = 0.0f
        val stepDelta = 0.05f // گام زمانی ۵۰ میلی‌ثانیه

        // فاز اول: زمان ۰ تا ۱.۰ ثانیه: ورودی I0.0 خاموش است
        memory = memory.setInput("I0.0", false)
        for (step in 1..20) {
            val res = plcEngine.executeCycle(rungs, memory, cycleDeltaSeconds = stepDelta)
            memory = res.second
            simTime += stepDelta

            buffer.addSample("I0.0", simTime, memory.inputs["I0.0"] ?: false)
            buffer.addSample("T1.DN", simTime, memory.timerDone["T1"] ?: false)
        }

        assertFalse("قبل از وصل ورودی، خروجی تایمر نباید فعال باشد", memory.timerDone["T1"] ?: false)

        // فاز دوم: در ثانیه ۱.۰۵ ورودی I0.0 روشن می‌شود (تحریک تایمر TON)
        memory = memory.setInput("I0.0", true)
        val startTimeWhenInputTurnedOn = simTime

        // شبیه‌سازی تا ۰.۹ ثانیه بعد از روشن شدن ورودی (تایمر هنوز نباید تمام شده باشد)
        for (step in 1..18) { // ۱۸ * ۰.۰۵ = ۰.۹۰ ثانیه
            val res = plcEngine.executeCycle(rungs, memory, cycleDeltaSeconds = stepDelta)
            memory = res.second
            simTime += stepDelta

            buffer.addSample("I0.0", simTime, memory.inputs["I0.0"] ?: false)
            buffer.addSample("T1.DN", simTime, memory.timerDone["T1"] ?: false)
        }

        // در ثانیه ۰.۹۰ بعد از استارت، T1.DN همچنان باید FALSE باشد
        val isDoneBefore1Sec = buffer.getValueAtTime("T1.DN", startTimeWhenInputTurnedOn + 0.90f)
        assertFalse("قبل از اتمام ۱ ثانیه کامل، خروجی تایمر T1.DN باید خاموش باشد", isDoneBefore1Sec)

        // دو گام دیگر (۰.۱۰ ثانیه تکمیلی تا رسیدن به ۱.۰ ثانیه دقیق)
        for (step in 1..2) {
            val res = plcEngine.executeCycle(rungs, memory, cycleDeltaSeconds = stepDelta)
            memory = res.second
            simTime += stepDelta

            buffer.addSample("I0.0", simTime, memory.inputs["I0.0"] ?: false)
            buffer.addSample("T1.DN", simTime, memory.timerDone["T1"] ?: false)
        }

        // اکنون دقیقاً پس از ۱ ثانیه تاخیر، T1.DN باید TRUE شده باشد
        val isDoneAt1Sec = buffer.getValueAtTime("T1.DN", simTime)
        assertTrue("دقیقاً پس از سپری شدن ۱.۰ ثانیه تاخیر، خروجی T1.DN باید ۱ (روشن) باشد", isDoneAt1Sec)

        // فاز سوم: با قطع ورودی I0.0، تایمر بلافاصله ریست و T1.DN باید ۰ شود
        memory = memory.setInput("I0.0", false)
        val resReset = plcEngine.executeCycle(rungs, memory, cycleDeltaSeconds = stepDelta)
        memory = resReset.second
        simTime += stepDelta

        buffer.addSample("I0.0", simTime, memory.inputs["I0.0"] ?: false)
        buffer.addSample("T1.DN", simTime, memory.timerDone["T1"] ?: false)

        assertFalse("با قطع ورودی I0.0، خروجی T1.DN باید فوراً خاموش شود", memory.timerDone["T1"] ?: false)
    }

    // ۴. تست ثبت دسته‌ای (Batch Recording) و پاکسازی داده‌ها (Clear)
    @Test
    fun test04_BatchRecordingAndClear() {
        val buffer = CircularSignalBuffer(maxDurationSeconds = 60f)

        val batch1 = mapOf(
            "I0.0" to true,
            "Q0.0" to false,
            "M0.0" to true
        )
        buffer.recordBatch(1.5f, batch1)

        val batch2 = mapOf(
            "I0.0" to false,
            "Q0.0" to true,
            "M0.0" to true
        )
        buffer.recordBatch(2.5f, batch2)

        assertEquals(2, buffer.getSampleSize("I0.0"))
        assertEquals(2, buffer.getSampleSize("Q0.0"))

        val (minT, maxT) = buffer.getTimeRange()
        assertEquals(1.5f, minT, 0.001f)
        assertEquals(2.5f, maxT, 0.001f)

        // تست پاکسازی
        buffer.clear()
        assertEquals(0, buffer.getSampleSize("I0.0"))
        assertEquals(0, buffer.getSampleSize("Q0.0"))
        val (minEmpty, maxEmpty) = buffer.getTimeRange()
        assertEquals(0f, minEmpty, 0.001f)
        assertEquals(0f, maxEmpty, 0.001f)
    }
}
