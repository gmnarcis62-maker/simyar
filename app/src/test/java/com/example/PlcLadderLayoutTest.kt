package com.example

import androidx.compose.ui.geometry.Offset
import com.example.canvas.PlcLadderLayoutCalculator
import com.example.model.LadderElement
import com.example.model.LadderElementType
import com.example.model.LadderRung
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * تست‌های واحد بند L8 برای محاسبه‌ی ابعاد، چیدمان و Fit بوم لدر PLC
 */
class PlcLadderLayoutTest {

    @Test
    fun test01_SingleRungWidthCalculation() {
        val rung = LadderRung(
            id = "r1",
            rungNumber = 1,
            contacts = listOf(
                LadderElement(id = "e1", type = LadderElementType.NO_CONTACT, address = "I0.0"),
                LadderElement(id = "e2", type = LadderElementType.NC_CONTACT, address = "I0.1")
            ),
            coils = listOf(
                LadderElement(id = "e3", type = LadderElementType.NORMAL_COIL, address = "Q0.0")
            )
        )

        val widthWithoutTool = PlcLadderLayoutCalculator.calculateRungWidthDp(rung, hasActiveTool = false)
        assertTrue("عرض رانگ باید حداقل برابر مقدار پایه ۷۸۰dp باشد", widthWithoutTool >= PlcLadderLayoutCalculator.MIN_RUNG_WIDTH_DP)

        val widthWithTool = PlcLadderLayoutCalculator.calculateRungWidthDp(rung, hasActiveTool = true)
        assertTrue("با داشتن ابزار فعال باید عرض رانگ برای اسلات درج بزرگتر یا مساوی حالت عادی باشد", widthWithTool >= widthWithoutTool)
    }

    @Test
    fun test02_WideRungWithMultipleBranchesExpandsWidth() {
        val wideRung = LadderRung(
            id = "r_wide",
            rungNumber = 2,
            contacts = (1..6).map { i ->
                LadderElement(id = "c$i", type = LadderElementType.NO_CONTACT, address = "I0.$i")
            },
            branchContacts = (1..5).map { i ->
                LadderElement(id = "b$i", type = LadderElementType.NO_CONTACT, address = "M0.$i")
            },
            coils = listOf(
                LadderElement(id = "q1", type = LadderElementType.NORMAL_COIL, address = "Q0.0"),
                LadderElement(id = "t1", type = LadderElementType.TIMER_TON, address = "T1", presetValue = 10)
            )
        )

        val rungWidth = PlcLadderLayoutCalculator.calculateRungWidthDp(wideRung, hasActiveTool = false)
        assertTrue("رانگ با ۶ کنتاکت، ۵ شاخه و ۲ کویل باید عریض‌تر از رانگ پایه ۷۸۰dp باشد", rungWidth > 850f)
    }

    @Test
    fun test03_HeightCalculationWith15And20Rungs() {
        // ایجاد ۱۵ رانگ لدر
        val fifteenRungs = (1..15).map { i ->
            LadderRung(
                id = "rung_$i",
                rungNumber = i,
                contacts = listOf(LadderElement(id = "in_$i", type = LadderElementType.NO_CONTACT, address = "I0.0")),
                coils = listOf(LadderElement(id = "out_$i", type = LadderElementType.NORMAL_COIL, address = "Q0.0"))
            )
        }

        val height15 = PlcLadderLayoutCalculator.calculateTotalContentHeightDp(fifteenRungs, hasActiveTool = false)
        assertTrue("ارتفاع کل ۱۵ رانگ باید به مراتب بیشتر از ۱۰۰۰dp باشد تا امکان اسکرول کامل نامحدود فراهم شود", height15 >= 1500f)

        // ایجاد ۲۰ رانگ لدر
        val twentyRungs = (1..20).map { i ->
            LadderRung(
                id = "rung_$i",
                rungNumber = i,
                contacts = listOf(LadderElement(id = "in_$i", type = LadderElementType.NO_CONTACT, address = "I0.0")),
                branchContacts = if (i % 2 == 0) listOf(LadderElement(id = "br_$i", type = LadderElementType.NO_CONTACT, address = "M0.0")) else emptyList(),
                coils = listOf(LadderElement(id = "out_$i", type = LadderElementType.NORMAL_COIL, address = "Q0.0"))
            )
        }

        val height20 = PlcLadderLayoutCalculator.calculateTotalContentHeightDp(twentyRungs, hasActiveTool = false)
        assertTrue("ارتفاع کل ۲۰ رانگ با شاخه‌ها باید بیش از ۲۰۰۰dp باشد", height20 > height15)
        assertTrue("ارتفاع ۲۰ رانگ باید محاسبه شود", height20 >= 2200f)
    }

    @Test
    fun test04_FitCalculationForTallProgram() {
        // برنامه با ۲۰ رانگ (عرض ۱۰۰۰ پیکسل، ارتفاع ۲۵۰۰ پیکسل)
        val contentW = 1000f
        val contentH = 2500f
        val viewportW = 1080f
        val viewportH = 1920f

        val (fitScale, panOffset) = PlcLadderLayoutCalculator.calculateFit(
            contentWidthPx = contentW,
            contentHeightPx = contentH,
            viewportWidthPx = viewportW,
            viewportHeightPx = viewportH
        )

        // مقیاس برای ارتفاع ۲۵۰۰ پیکسل روی صفحه ۱۹۲۰ پیکسلی باید کمتر از ۱ باشد
        assertTrue("مقیاس Fit برای برنامه طویل باید کمتر از ۱ باشد تا کل برنامه در کادر جا شود", fitScale < 1.0f)
        assertTrue("مقیاس Fit نباید کمتر از حد مجاز ۰.۲ باشد", fitScale >= 0.2f)

        // انطباق باید افقی را در مرکز قرار دهد
        val fittedW = contentW * fitScale
        val expectedPanX = (viewportW - fittedW) / 2f
        assertEquals("جابجایی افقی باید محتوا را در مرکز عرض پنجره تراز کند", expectedPanX, panOffset.x, 1.0f)
    }

    @Test
    fun test05_FitCalculationForWideProgram() {
        // برنامه عریض (عرض ۲۵۰۰ پیکسل، ارتفاع ۵۰۰ پیکسل)
        val contentW = 2500f
        val contentH = 500f
        val viewportW = 1080f
        val viewportH = 1920f

        val (fitScale, panOffset) = PlcLadderLayoutCalculator.calculateFit(
            contentWidthPx = contentW,
            contentHeightPx = contentH,
            viewportWidthPx = viewportW,
            viewportHeightPx = viewportH
        )

        assertTrue("مقیاس Fit برای برنامه عریض باید کوچک شود تا در عرض ۱۰۸۰ جا شود", fitScale < 0.5f)
        val fittedW = contentW * fitScale
        assertTrue("عرض اندازه بعد از زوم باید در پنجره بگنجد", fittedW <= viewportW)
    }

    @Test
    fun test06_FitCalculationEdgeCases() {
        // حالت ابعاد نامعتبر یا صفر
        val (scaleZeroContent, panZeroContent) = PlcLadderLayoutCalculator.calculateFit(0f, 0f, 1080f, 1920f)
        assertEquals(1.0f, scaleZeroContent, 0.001f)
        assertEquals(Offset.Zero, panZeroContent)

        val (scaleZeroViewport, panZeroViewport) = PlcLadderLayoutCalculator.calculateFit(1000f, 1000f, 0f, 0f)
        assertEquals(1.0f, scaleZeroViewport, 0.001f)
        assertEquals(Offset.Zero, panZeroViewport)
    }
}
