package com.example.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.example.ui.timing.CircularSignalBuffer
import com.example.ui.timing.SignalSourceType
import com.example.ui.timing.TrackableSignal
import com.example.util.PersianUtils
import kotlin.math.max
import kotlin.math.min

/**
 * بوم ترسیم نمودار زمانی لاجیک سیگنال‌ها با کارایی بالا (60fps)
 */
@Composable
fun TimingDiagramView(
    selectedSignals: List<TrackableSignal>,
    buffer: CircularSignalBuffer,
    currentTimeSeconds: Float,
    timeScalePxPerSec: Float, // مقیاس افقی (پیکسل بر ثانیه)
    panOffsetSeconds: Float, // آفست پیمایش زمانی
    cursorTimeSeconds: Float?, // موقعیت خط‌کش زمانی
    onCursorMoved: (Float?) -> Unit,
    onPanChanged: (Float) -> Unit,
    onScaleChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val path = remember { Path() }

    // رنگ‌بندی‌های بهینه بدون allocation در حلقه رسم
    val bgDiagramColor = Color(0xFF0F172A)
    val headerBgColor = Color(0xFF1E293B)
    val gridLineColor = Color(0xFF334155)
    val timeAxisColor = Color(0xFF64748B)
    val cursorLineColor = Color(0xFFFF5252)
    val cursorBadgeBg = Color(0xFFFF1744)

    val labelWidth = 240f // عرض ستون اطلاعات و برچسب در سمت چپ (پیکسل)
    val laneHeight = 85f  // ارتفاع هر نوار سیگنال
    val timeHeaderHeight = 60f

    val textStyleLabel = remember {
        TextStyle(
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
    val textStyleSub = remember {
        TextStyle(
            color = Color(0xFF94A3B8),
            fontSize = 10.sp
        )
    }
    val textStyleTime = remember {
        TextStyle(
            color = Color(0xFF38BDF8),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
    }
    val textStyleVal = remember {
        TextStyle(
            color = Color(0xFF00E676),
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace
        )
    }

    Box(
        modifier = modifier
            .background(bgDiagramColor)
            .pointerInput(Unit) {
                // ژست دوانگشتی برای زوم افقی و جابجایی
                detectTransformGestures { _, pan, zoom, _ ->
                    if (zoom != 1.0f) {
                        val newScale = (timeScalePxPerSec * zoom).coerceIn(20f, 600f)
                        onScaleChanged(newScale)
                    }
                    if (pan.x != 0f) {
                        val dt = -pan.x / timeScalePxPerSec
                        onPanChanged(panOffsetSeconds + dt)
                    }
                }
            }
            .pointerInput(timeScalePxPerSec, panOffsetSeconds, labelWidth) {
                // ژست تک انگشتی برای کلیک یا درگ خط‌کش مکان‌نما (Cursor)
                detectDragGestures(
                    onDragStart = { offset ->
                        if (offset.x >= labelWidth) {
                            val time = panOffsetSeconds + (offset.x - labelWidth) / timeScalePxPerSec
                            onCursorMoved(time.coerceAtLeast(0f))
                        }
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val offset = change.position
                        if (offset.x >= labelWidth) {
                            val time = panOffsetSeconds + (offset.x - labelWidth) / timeScalePxPerSec
                            onCursorMoved(time.coerceAtLeast(0f))
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val totalWidth = size.width
            val totalHeight = size.height
            val waveAreaWidth = (totalWidth - labelWidth).coerceAtLeast(10f)

            // ۱. محاسبه بازه زمانی نمایشی در صفحه
            val visibleStartSec = panOffsetSeconds
            val visibleEndSec = panOffsetSeconds + (waveAreaWidth / timeScalePxPerSec)

            // ۲. رسم پس‌زمینه و خطوط شبکه زمان (Grid)
            drawRect(
                color = bgDiagramColor,
                topLeft = Offset(labelWidth, 0f),
                size = Size(waveAreaWidth, totalHeight)
            )

            // تعیین فواصل خطوط شطرنجی زمان متناسب با زوم
            val stepSec = when {
                timeScalePxPerSec >= 250f -> 0.2f
                timeScalePxPerSec >= 120f -> 0.5f
                timeScalePxPerSec >= 60f -> 1.0f
                timeScalePxPerSec >= 30f -> 2.0f
                else -> 5.0f
            }

            val firstTick = (Math.floor((visibleStartSec / stepSec).toDouble()) * stepSec).toFloat()
            var t = firstTick
            while (t <= visibleEndSec + stepSec) {
                if (t >= 0f) {
                    val x = labelWidth + (t - panOffsetSeconds) * timeScalePxPerSec
                    if (x in labelWidth..totalWidth) {
                        // خط عمودی زمان
                        drawLine(
                            color = gridLineColor,
                            start = Offset(x, timeHeaderHeight),
                            end = Offset(x, totalHeight),
                            strokeWidth = 1f
                        )
                        // نشانه‌گذاری مقدار ثانیه در نوار بالای نمودار
                        val secFormatted = PersianUtils.toPersianDigits(String.format("%.1fs", t))
                        drawText(
                            textMeasurer = textMeasurer,
                            text = secFormatted,
                            topLeft = Offset(x - 16f, 15f),
                            style = textStyleTime
                        )
                        // تیک کوچک روی محور
                        drawLine(
                            color = timeAxisColor,
                            start = Offset(x, timeHeaderHeight - 8f),
                            end = Offset(x, timeHeaderHeight),
                            strokeWidth = 2f
                        )
                    }
                }
                t += stepSec
            }

            // خط افقی تفکیک نوار زمان
            drawLine(
                color = timeAxisColor,
                start = Offset(0f, timeHeaderHeight),
                end = Offset(totalWidth, timeHeaderHeight),
                strokeWidth = 2f
            )

            // ۳. رسم نوارهای هر سیگنال (Lanes)
            selectedSignals.forEachIndexed { index, sig ->
                val topY = timeHeaderHeight + index * laneHeight
                val bottomY = topY + laneHeight

                if (bottomY <= totalHeight + laneHeight) {
                    // الف) خطوط راهنمای افقی نوار
                    drawLine(
                        color = gridLineColor,
                        start = Offset(0f, bottomY),
                        end = Offset(totalWidth, bottomY),
                        strokeWidth = 1f
                    )

                    // سطوح ۰ و ۱ در این نوار
                    val highY = topY + 16f
                    val lowY = bottomY - 16f

                    // ب) خط چین ضعیف برای سطح ۱
                    drawLine(
                        color = Color(0xFF1E293B),
                        start = Offset(labelWidth, highY),
                        end = Offset(totalWidth, highY),
                        strokeWidth = 1f
                    )

                    // ج) استخراج و رسم شکل‌موج سیگنال
                    val samples = buffer.getSamples(sig.id)
                    val signalColor = when (sig.type) {
                        SignalSourceType.PLC_OUTPUT -> Color(0xFF00E5FF) // فیروزه‌ای
                        SignalSourceType.PLC_INPUT -> Color(0xFFFFD600)  // زرد
                        SignalSourceType.CIRCUIT_COIL -> Color(0xFF00E676) // سبز
                        SignalSourceType.PLC_TIMER_DONE -> Color(0xFFFF9100) // نارنجی
                        SignalSourceType.PLC_COUNTER_DONE -> Color(0xFFE040FB) // بنفش
                        else -> Color(0xFF64B5F6) // آبی
                    }

                    if (samples.isNotEmpty()) {
                        path.reset()

                        // تعیین مقدار سیگنال در ابتدای بازه دید
                        var curVal = buffer.getValueAtTime(sig.id, visibleStartSec)
                        var prevX = labelWidth
                        var prevY = if (curVal) highY else lowY

                        path.moveTo(prevX, prevY)

                        // پیمایش نمونه‌های داخل یا مجاور صفحه دید
                        for (sample in samples) {
                            if (sample.timestampSeconds < visibleStartSec) {
                                curVal = sample.value
                                prevY = if (curVal) highY else lowY
                                path.moveTo(prevX, prevY)
                                continue
                            }
                            if (sample.timestampSeconds > visibleEndSec + 1f) {
                                break
                            }

                            val sampleX = labelWidth + (sample.timestampSeconds - panOffsetSeconds) * timeScalePxPerSec
                            val sampleY = if (sample.value) highY else lowY

                            if (sample.value != curVal) {
                                // جهش عمودی (Transition)
                                path.lineTo(sampleX, prevY)
                                path.lineTo(sampleX, sampleY)
                                curVal = sample.value
                                prevY = sampleY
                            } else {
                                path.lineTo(sampleX, sampleY)
                            }
                            prevX = sampleX
                        }

                        // امتداد شکل‌موج تا انتهای لبه صفحه یا زمان حال شبیه‌سازی
                        val finalTime = min(visibleEndSec, currentTimeSeconds)
                        val finalX = labelWidth + (finalTime - panOffsetSeconds) * timeScalePxPerSec
                        if (finalX > prevX) {
                            path.lineTo(finalX.coerceAtMost(totalWidth), prevY)
                        }

                        // رسم شکل‌موج لاجیک دیجیتال
                        drawPath(
                            path = path,
                            color = signalColor,
                            style = Stroke(width = 3f, cap = StrokeCap.Square)
                        )
                    }

                    // د) نوار پس‌زمینه ستون برچسب چپ (Label Header)
                    drawRect(
                        color = headerBgColor,
                        topLeft = Offset(0f, topY),
                        size = Size(labelWidth, laneHeight)
                    )
                    // خط عمودی تفکیک ستون چپ از نمودار
                    drawLine(
                        color = Color(0xFF475569),
                        start = Offset(labelWidth, topY),
                        end = Offset(labelWidth, bottomY),
                        strokeWidth = 2f
                    )

                    // دایره نشانگر وضعیت رنگی سیگنال
                    drawCircle(
                        color = signalColor,
                        radius = 4f,
                        center = Offset(16f, topY + 22f)
                    )

                    // متن عنوان سیگنال
                    drawText(
                        textMeasurer = textMeasurer,
                        text = sig.displayName,
                        topLeft = Offset(28f, topY + 12f),
                        style = textStyleLabel
                    )

                    // متن نوع سیگنال
                    drawText(
                        textMeasurer = textMeasurer,
                        text = sig.type.displayName,
                        topLeft = Offset(28f, topY + 36f),
                        style = textStyleSub
                    )

                    // نمایش مقدار لحظه‌ای یا مقدار در مکان‌نما
                    val activeTime = cursorTimeSeconds ?: currentTimeSeconds
                    val valueAtCursor = buffer.getValueAtTime(sig.id, activeTime)
                    val valText = if (valueAtCursor) "۱ (HIGH)" else "۰ (LOW)"
                    val valColor = if (valueAtCursor) Color(0xFF00E676) else Color(0xFF94A3B8)

                    drawText(
                        textMeasurer = textMeasurer,
                        text = valText,
                        topLeft = Offset(labelWidth - 65f, topY + 24f),
                        style = textStyleVal.copy(color = valColor)
                    )
                }
            }

            // ۴. رسم مکان‌نما (Cursor) در صورت فعال بودن
            cursorTimeSeconds?.let { cTime ->
                val cursorX = labelWidth + (cTime - panOffsetSeconds) * timeScalePxPerSec
                if (cursorX in labelWidth..totalWidth) {
                    // خط عمودی قرمز رنگ مکان‌نما
                    drawLine(
                        color = cursorLineColor,
                        start = Offset(cursorX, 0f),
                        end = Offset(cursorX, totalHeight),
                        strokeWidth = 2f
                    )

                    // بج بالای مکان‌نما نشان‌دهنده زمان دقیق
                    val timeStr = PersianUtils.toPersianDigits(String.format("t = %.2f s", cTime))
                    val badgeWidth = 110f
                    val badgeHeight = 24f
                    val badgeLeft = (cursorX - badgeWidth / 2f).coerceIn(labelWidth, totalWidth - badgeWidth)

                    drawRoundRect(
                        color = cursorBadgeBg,
                        topLeft = Offset(badgeLeft, 8f),
                        size = Size(badgeWidth, badgeHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                    )

                    drawText(
                        textMeasurer = textMeasurer,
                        text = timeStr,
                        topLeft = Offset(badgeLeft + 12f, 12f),
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
            }
        }
    }
}
