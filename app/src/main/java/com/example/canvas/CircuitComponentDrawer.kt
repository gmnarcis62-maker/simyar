package com.example.canvas

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import com.example.model.CircuitComponent
import com.example.model.ComponentType
import com.example.model.MotorDirection

object CircuitComponentDrawer {

    fun drawComponent(
        scope: DrawScope,
        comp: CircuitComponent,
        isSelected: Boolean = false,
        activeTerminalId: String? = null,
        currentScale: Float = 1f,
        density: Float = 1f
    ) {
        with(scope) {
            val ox = comp.x
            val oy = comp.y

            val strokeColor = if (isSelected) Color(0xFF00E5FF) else Color(0xFFCFD8DC)
            val activeColor = Color(0xFF00E676)
            val normalStroke = Stroke(width = 3.5f, cap = StrokeCap.Round)

            // کادر انتخاب واضح با گوشه‌های نرم
            if (isSelected) {
                drawRect(
                    color = Color(0x3300E5FF),
                    topLeft = Offset(ox - 10f, oy - 10f),
                    size = androidx.compose.ui.geometry.Size(120f, 100f),
                    style = Stroke(width = 2.5f)
                )
            }

            // نماد قفل در صورت قفل بودن قطعه
            if (comp.isLocked) {
                drawCircle(Color(0xFFFFD600), radius = 5f, center = Offset(ox + 98f, oy + 8f))
            }

            // رسم برچسب نام قطعه با کادر کپسولی (Pill) نیمه‌شفاف و جبران مقیاس
            // اگر زوم خیلی پایین باشد (کمتر از 0.5 برابر چگالی) برای جلوگیری از شلوغی پنهان می‌شود
            val minScaleThreshold = 0.45f * (if (density > 0.1f) density else 1f)
            val shouldShowLabel = currentScale >= minScaleThreshold

            if (shouldShowLabel && comp.label.isNotBlank()) {
                // اندازه فونت با حداقل معادل 10sp واقعی (حداقل 13 واحد)
                val baseTextSize = 13f
                // در زوم‌های کمتر، مقداری اندازه متن را در مختصات بوم تقویت می‌کنیم تا در صفحه خوانا بماند
                val scaleCompensation = if (currentScale < 1.0f) (1.0f / currentScale.coerceAtLeast(0.5f)).coerceAtMost(1.4f) else 1.0f
                val effectiveTextSize = baseTextSize * scaleCompensation

                drawIntoCanvas { canvas ->
                    val textPaint = Paint().apply {
                        color = if (isSelected) android.graphics.Color.parseColor("#00E5FF") else android.graphics.Color.parseColor("#ECEFF1")
                        textSize = effectiveTextSize
                        typeface = Typeface.DEFAULT_BOLD
                        isAntiAlias = true
                    }

                    val textWidth = textPaint.measureText(comp.label)
                    val fontMetrics = textPaint.fontMetrics
                    val textHeight = fontMetrics.bottom - fontMetrics.top

                    val pillPaddingX = 8f
                    val pillPaddingY = 4f
                    val pillLeft = ox + 15f - pillPaddingX
                    val pillTop = oy - 12f + fontMetrics.top - pillPaddingY
                    val pillRight = ox + 15f + textWidth + pillPaddingX
                    val pillBottom = oy - 12f + fontMetrics.bottom + pillPaddingY

                    // رسم کپسول زمینه نیمه‌شفاف تیره
                    val pillPaint = Paint().apply {
                        color = android.graphics.Color.argb(200, 10, 25, 45)
                        style = Paint.Style.FILL
                        isAntiAlias = true
                    }
                    val pillStrokePaint = Paint().apply {
                        color = if (isSelected) android.graphics.Color.argb(180, 0, 229, 255) else android.graphics.Color.argb(100, 80, 110, 140)
                        style = Paint.Style.STROKE
                        strokeWidth = 1.5f
                        isAntiAlias = true
                    }

                    val pillRect = android.graphics.RectF(pillLeft, pillTop, pillRight, pillBottom)
                    val cornerRadius = (pillBottom - pillTop) / 2f
                    canvas.nativeCanvas.drawRoundRect(pillRect, cornerRadius, cornerRadius, pillPaint)
                    canvas.nativeCanvas.drawRoundRect(pillRect, cornerRadius, cornerRadius, pillStrokePaint)

                    // رسم متن برچسب
                    canvas.nativeCanvas.drawText(comp.label, ox + 15f, oy - 12f, textPaint)
                }
            }

            // رسم متناسب با زاویه چرخش قطعه
            rotate(degrees = comp.rotation, pivot = Offset(ox + 50f, oy + 40f)) {
                when (comp.type) {
                    ComponentType.THREE_PHASE_SOURCE -> {
                        // نماد شبکه سه‌فاز: ۳ فاز L1 (قرمز)، L2 (زرد)، L3 (آبی)
                        drawCircle(color = Color(0xFFFF5252), radius = 10f, center = Offset(ox + 20f, oy + 60f))
                        drawCircle(color = Color(0xFFFFD600), radius = 10f, center = Offset(ox + 50f, oy + 60f))
                        drawCircle(color = Color(0xFF448AFF), radius = 10f, center = Offset(ox + 80f, oy + 60f))
                        drawLine(strokeColor, Offset(ox + 12f, oy + 32f), Offset(ox + 88f, oy + 32f), strokeWidth = 3f)
                    }

                    ComponentType.AC_PHASE -> {
                        drawCircle(color = Color(0xFFFF5252), radius = 18f, center = Offset(ox + 40f, oy + 40f))
                        drawCircle(color = Color(0xFFFF8A80), radius = 7f, center = Offset(ox + 40f, oy + 60f))
                    }

                    ComponentType.AC_NEUTRAL -> {
                        drawCircle(color = Color(0xFF448AFF), radius = 18f, center = Offset(ox + 40f, oy + 40f))
                        drawCircle(color = Color(0xFF82B1FF), radius = 7f, center = Offset(ox + 40f, oy + 60f))
                    }

                    ComponentType.DC_PLUS -> {
                        drawRect(color = Color(0xFFFF1744), topLeft = Offset(ox + 22f, oy + 22f), size = androidx.compose.ui.geometry.Size(36f, 36f))
                    }

                    ComponentType.DC_MINUS -> {
                        drawRect(color = Color(0xFF37474F), topLeft = Offset(ox + 22f, oy + 22f), size = androidx.compose.ui.geometry.Size(36f, 36f))
                    }

                    ComponentType.EARTH -> {
                        drawLine(strokeColor, Offset(ox + 40f, oy + 28f), Offset(ox + 40f, oy + 50f), strokeWidth = 3.5f)
                        drawLine(strokeColor, Offset(ox + 24f, oy + 50f), Offset(ox + 56f, oy + 50f), strokeWidth = 3.5f)
                        drawLine(strokeColor, Offset(ox + 29f, oy + 58f), Offset(ox + 51f, oy + 58f), strokeWidth = 3f)
                        drawLine(strokeColor, Offset(ox + 34f, oy + 66f), Offset(ox + 46f, oy + 66f), strokeWidth = 2.5f)
                    }

                    ComponentType.PUSH_BUTTON_NO, ComponentType.CONTACTOR_NO_AUX, ComponentType.TIMER_NO_CONTACT -> {
                        drawLine(strokeColor, Offset(ox + 18f, oy + 40f), Offset(ox + 38f, oy + 40f), strokeWidth = 3.5f)
                        drawLine(strokeColor, Offset(ox + 62f, oy + 40f), Offset(ox + 82f, oy + 40f), strokeWidth = 3.5f)
                        drawCircle(strokeColor, radius = 5f, center = Offset(ox + 38f, oy + 40f), style = normalStroke)
                        drawCircle(strokeColor, radius = 5f, center = Offset(ox + 62f, oy + 40f), style = normalStroke)

                        val contactY = if (comp.isClosedOrPressed) 40f else 24f
                        val contactColor = if (comp.isClosedOrPressed) activeColor else strokeColor
                        drawLine(contactColor, Offset(ox + 35f, oy + contactY), Offset(ox + 65f, oy + contactY), strokeWidth = 4f)

                        if (comp.type == ComponentType.PUSH_BUTTON_NO) {
                            drawLine(strokeColor, Offset(ox + 50f, oy + contactY), Offset(ox + 50f, oy + 10f), strokeWidth = 2.5f)
                            drawLine(strokeColor, Offset(ox + 40f, oy + 10f), Offset(ox + 60f, oy + 10f), strokeWidth = 3.5f)
                        }
                    }

                    ComponentType.PUSH_BUTTON_NC, ComponentType.CONTACTOR_NC_AUX, ComponentType.TIMER_NC_CONTACT -> {
                        drawLine(strokeColor, Offset(ox + 18f, oy + 40f), Offset(ox + 38f, oy + 40f), strokeWidth = 3.5f)
                        drawLine(strokeColor, Offset(ox + 62f, oy + 40f), Offset(ox + 82f, oy + 40f), strokeWidth = 3.5f)
                        drawCircle(strokeColor, radius = 5f, center = Offset(ox + 38f, oy + 40f), style = normalStroke)
                        drawCircle(strokeColor, radius = 5f, center = Offset(ox + 62f, oy + 40f), style = normalStroke)

                        val contactClosed = !comp.isClosedOrPressed
                        val contactY = if (contactClosed) 40f else 24f
                        val contactColor = if (contactClosed) activeColor else Color(0xFFFF5252)
                        drawLine(contactColor, Offset(ox + 35f, oy + contactY), Offset(ox + 65f, oy + contactY), strokeWidth = 4f)

                        if (comp.type == ComponentType.PUSH_BUTTON_NC) {
                            drawLine(strokeColor, Offset(ox + 50f, oy + contactY), Offset(ox + 50f, oy + 10f), strokeWidth = 2.5f)
                            drawLine(strokeColor, Offset(ox + 40f, oy + 10f), Offset(ox + 60f, oy + 10f), strokeWidth = 3.5f)
                        }
                    }

                    ComponentType.EMERGENCY_STOP -> {
                        drawLine(strokeColor, Offset(ox + 18f, oy + 40f), Offset(ox + 38f, oy + 40f), strokeWidth = 3.5f)
                        drawLine(strokeColor, Offset(ox + 62f, oy + 40f), Offset(ox + 82f, oy + 40f), strokeWidth = 3.5f)
                        val contactY = if (comp.isClosedOrPressed) 22f else 40f
                        drawLine(if (!comp.isClosedOrPressed) activeColor else Color(0xFFFF1744), Offset(ox + 35f, oy + contactY), Offset(ox + 65f, oy + contactY), strokeWidth = 4f)
                        // شستی قارچی قرمز
                        drawArc(
                            color = Color(0xFFFF1744),
                            startAngle = 180f,
                            sweepAngle = 180f,
                            useCenter = true,
                            topLeft = Offset(ox + 36f, oy + 6f),
                            size = androidx.compose.ui.geometry.Size(28f, 18f)
                        )
                    }

                    ComponentType.CIRCUIT_BREAKER_MCB -> {
                        drawLine(strokeColor, Offset(ox + 20f, oy + 40f), Offset(ox + 38f, oy + 40f), strokeWidth = 3.5f)
                        drawLine(strokeColor, Offset(ox + 62f, oy + 40f), Offset(ox + 80f, oy + 40f), strokeWidth = 3.5f)
                        val mcbClosed = comp.isClosedOrPressed
                        val mcbColor = if (mcbClosed) activeColor else strokeColor
                        val endY = if (mcbClosed) 40f else 22f
                        drawLine(mcbColor, Offset(ox + 38f, oy + 40f), Offset(ox + 62f, oy + endY), strokeWidth = 4f)
                        // نماد ضربدر حفاظت جریانی
                        drawLine(Color(0xFFFF9100), Offset(ox + 56f, oy + 18f), Offset(ox + 66f, oy + 28f), strokeWidth = 2.5f)
                        drawLine(Color(0xFFFF9100), Offset(ox + 66f, oy + 18f), Offset(ox + 56f, oy + 28f), strokeWidth = 2.5f)
                    }

                    ComponentType.CIRCUIT_BREAKER_MCB_3P -> {
                        val isClosed = comp.isClosedOrPressed
                        val mcbColor = if (isClosed) activeColor else strokeColor
                        val xPositions = listOf(20f, 50f, 80f)
                        for (px in xPositions) {
                            drawLine(strokeColor, Offset(ox + px, oy + 15f), Offset(ox + px, oy + 28f), strokeWidth = 3f)
                            drawLine(strokeColor, Offset(ox + px, oy + 52f), Offset(ox + px, oy + 65f), strokeWidth = 3f)
                            val endX = if (isClosed) px else px + 12f
                            val endY = if (isClosed) 52f else 32f
                            drawLine(mcbColor, Offset(ox + px, oy + 28f), Offset(ox + endX, oy + endY), strokeWidth = 3.5f)
                            drawLine(Color(0xFFFF9100), Offset(ox + px - 4f, oy + 22f), Offset(ox + px + 4f, oy + 22f), strokeWidth = 2f)
                            drawLine(Color(0xFFFF9100), Offset(ox + px - 4f, oy + 22f), Offset(ox + px - 4f, oy + 26f), strokeWidth = 2f)
                        }
                        drawLine(strokeColor, Offset(ox + 20f, oy + 40f), Offset(ox + 80f, oy + 40f), strokeWidth = 1.5f)
                    }

                    ComponentType.CIRCUIT_BREAKER_MCCB_3P -> {
                        val isClosed = comp.isClosedOrPressed
                        val mccbColor = if (isClosed) activeColor else strokeColor
                        drawRoundRect(
                            color = Color(0x33455A64),
                            topLeft = Offset(ox + 10f, oy + 22f),
                            size = androidx.compose.ui.geometry.Size(80f, 36f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                        )
                        drawRoundRect(
                            color = strokeColor,
                            topLeft = Offset(ox + 10f, oy + 22f),
                            size = androidx.compose.ui.geometry.Size(80f, 36f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
                            style = Stroke(width = 2f)
                        )
                        val xPositions = listOf(20f, 50f, 80f)
                        for (px in xPositions) {
                            drawLine(strokeColor, Offset(ox + px, oy + 15f), Offset(ox + px, oy + 26f), strokeWidth = 3f)
                            drawLine(strokeColor, Offset(ox + px, oy + 54f), Offset(ox + px, oy + 65f), strokeWidth = 3f)
                            val endX = if (isClosed) px else px + 10f
                            val endY = if (isClosed) 54f else 34f
                            drawLine(mccbColor, Offset(ox + px, oy + 26f), Offset(ox + endX, oy + endY), strokeWidth = 3.5f)
                            drawCircle(Color(0xFFFF9100), radius = 3f, center = Offset(ox + px, oy + 22f))
                        }
                        val handleColor = if (isClosed) activeColor else Color(0xFFFF5252)
                        drawCircle(handleColor, radius = 4f, center = Offset(ox + 50f, oy + 40f))
                    }

                    ComponentType.FUSE_3P -> {
                        val xPositions = listOf(20f, 50f, 80f)
                        for (px in xPositions) {
                            drawLine(strokeColor, Offset(ox + px, oy + 15f), Offset(ox + px, oy + 26f), strokeWidth = 3f)
                            drawLine(strokeColor, Offset(ox + px, oy + 54f), Offset(ox + px, oy + 65f), strokeWidth = 3f)
                            drawRect(
                                color = strokeColor,
                                topLeft = Offset(ox + px - 7f, oy + 26f),
                                size = androidx.compose.ui.geometry.Size(14f, 28f),
                                style = Stroke(width = 2.5f)
                            )
                            drawLine(strokeColor, Offset(ox + px, oy + 26f), Offset(ox + px, oy + 54f), strokeWidth = 2f)
                        }
                        drawLine(Color(0x8890A4AE), Offset(ox + 13f, oy + 40f), Offset(ox + 87f, oy + 40f), strokeWidth = 1.5f)
                    }

                    ComponentType.RCD_4P -> {
                        val isClosed = comp.isClosedOrPressed
                        val rcdColor = if (isClosed) activeColor else strokeColor
                        val xPositions = listOf(15f, 38f, 62f, 85f)
                        for (px in xPositions) {
                            drawLine(strokeColor, Offset(ox + px, oy + 15f), Offset(ox + px, oy + 28f), strokeWidth = 2.5f)
                            drawLine(strokeColor, Offset(ox + px, oy + 52f), Offset(ox + px, oy + 65f), strokeWidth = 2.5f)
                            val endX = if (isClosed) px else px + 8f
                            val endY = if (isClosed) 52f else 34f
                            drawLine(rcdColor, Offset(ox + px, oy + 28f), Offset(ox + endX, oy + endY), strokeWidth = 3f)
                        }
                        drawRoundRect(
                            color = Color(0xFF00E5FF),
                            topLeft = Offset(ox + 10f, oy + 32f),
                            size = androidx.compose.ui.geometry.Size(80f, 16f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f),
                            style = Stroke(width = 2f)
                        )
                        drawCircle(Color(0xFFFFD600), radius = 5f, center = Offset(ox + 50f, oy + 40f))
                    }

                    ComponentType.CONTROL_RELAY_1CO -> {
                        val isEnergized = comp.isEnergized
                        val rColor = if (isEnergized) activeColor else strokeColor
                        drawLine(strokeColor, Offset(ox + 15f, oy + 25f), Offset(ox + 15f, oy + 30f), strokeWidth = 2.5f)
                        drawLine(strokeColor, Offset(ox + 15f, oy + 50f), Offset(ox + 15f, oy + 55f), strokeWidth = 2.5f)
                        drawRect(
                            color = if (isEnergized) activeColor else strokeColor,
                            topLeft = Offset(ox + 7f, oy + 30f),
                            size = androidx.compose.ui.geometry.Size(16f, 20f),
                            style = normalStroke
                        )
                        drawLine(if (isEnergized) activeColor else strokeColor, Offset(ox + 7f, oy + 30f), Offset(ox + 23f, oy + 50f), strokeWidth = 2f)

                        drawLine(strokeColor, Offset(ox + 50f, oy + 40f), Offset(ox + 58f, oy + 40f), strokeWidth = 3f)
                        drawLine(strokeColor, Offset(ox + 75f, oy + 25f), Offset(ox + 85f, oy + 25f), strokeWidth = 3f)
                        drawLine(strokeColor, Offset(ox + 75f, oy + 55f), Offset(ox + 85f, oy + 55f), strokeWidth = 3f)
                        drawCircle(strokeColor, radius = 3.5f, center = Offset(ox + 58f, oy + 40f), style = normalStroke)
                        drawCircle(strokeColor, radius = 3.5f, center = Offset(ox + 75f, oy + 25f), style = normalStroke)
                        drawCircle(strokeColor, radius = 3.5f, center = Offset(ox + 75f, oy + 55f), style = normalStroke)

                        val targetY = if (isEnergized) 55f else 25f
                        drawLine(rColor, Offset(ox + 58f, oy + 40f), Offset(ox + 75f, oy + targetY), strokeWidth = 3.5f)
                        drawLine(Color(0x8890A4AE), Offset(ox + 23f, oy + 40f), Offset(ox + 55f, oy + 40f), strokeWidth = 1.5f)
                    }

                    ComponentType.CONTROL_RELAY_2CO -> {
                        val isEnergized = comp.isEnergized
                        val rColor = if (isEnergized) activeColor else strokeColor
                        drawLine(strokeColor, Offset(ox + 15f, oy + 25f), Offset(ox + 15f, oy + 30f), strokeWidth = 2.5f)
                        drawLine(strokeColor, Offset(ox + 15f, oy + 50f), Offset(ox + 15f, oy + 55f), strokeWidth = 2.5f)
                        drawRect(
                            color = if (isEnergized) activeColor else strokeColor,
                            topLeft = Offset(ox + 7f, oy + 30f),
                            size = androidx.compose.ui.geometry.Size(16f, 20f),
                            style = normalStroke
                        )
                        drawLine(if (isEnergized) activeColor else strokeColor, Offset(ox + 7f, oy + 30f), Offset(ox + 23f, oy + 50f), strokeWidth = 2f)

                        drawLine(strokeColor, Offset(ox + 50f, oy + 25f), Offset(ox + 58f, oy + 25f), strokeWidth = 2.5f)
                        drawLine(strokeColor, Offset(ox + 75f, oy + 15f), Offset(ox + 85f, oy + 15f), strokeWidth = 2.5f)
                        drawLine(strokeColor, Offset(ox + 75f, oy + 32f), Offset(ox + 85f, oy + 32f), strokeWidth = 2.5f)
                        val targetY1 = if (isEnergized) 32f else 15f
                        drawLine(rColor, Offset(ox + 58f, oy + 25f), Offset(ox + 75f, oy + targetY1), strokeWidth = 3f)

                        drawLine(strokeColor, Offset(ox + 50f, oy + 55f), Offset(ox + 58f, oy + 55f), strokeWidth = 2.5f)
                        drawLine(strokeColor, Offset(ox + 75f, oy + 48f), Offset(ox + 85f, oy + 48f), strokeWidth = 2.5f)
                        drawLine(strokeColor, Offset(ox + 75f, oy + 65f), Offset(ox + 85f, oy + 65f), strokeWidth = 2.5f)
                        val targetY2 = if (isEnergized) 65f else 48f
                        drawLine(rColor, Offset(ox + 58f, oy + 55f), Offset(ox + 75f, oy + targetY2), strokeWidth = 3f)
                    }

                    ComponentType.PHASE_SEQUENCE_RELAY -> {
                        val isHealthy = comp.isEnergized
                        val relayColor = if (isHealthy) activeColor else strokeColor
                        drawRoundRect(
                            color = Color(0x331E2A38),
                            topLeft = Offset(ox + 12f, oy + 22f),
                            size = androidx.compose.ui.geometry.Size(76f, 32f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                        )
                        drawRoundRect(
                            color = strokeColor,
                            topLeft = Offset(ox + 12f, oy + 22f),
                            size = androidx.compose.ui.geometry.Size(76f, 32f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
                            style = Stroke(width = 2f)
                        )
                        drawLine(strokeColor, Offset(ox + 20f, oy + 15f), Offset(ox + 20f, oy + 22f), strokeWidth = 2.5f)
                        drawLine(strokeColor, Offset(ox + 50f, oy + 15f), Offset(ox + 50f, oy + 22f), strokeWidth = 2.5f)
                        drawLine(strokeColor, Offset(ox + 80f, oy + 15f), Offset(ox + 80f, oy + 22f), strokeWidth = 2.5f)

                        val ledColor = if (isHealthy) Color(0xFF00E676) else Color(0xFFFF1744)
                        drawCircle(ledColor, radius = 4f, center = Offset(ox + 50f, oy + 38f))

                        drawLine(strokeColor, Offset(ox + 30f, oy + 54f), Offset(ox + 30f, oy + 65f), strokeWidth = 2.5f)
                        drawLine(strokeColor, Offset(ox + 55f, oy + 54f), Offset(ox + 55f, oy + 65f), strokeWidth = 2.5f)
                        drawLine(strokeColor, Offset(ox + 80f, oy + 54f), Offset(ox + 80f, oy + 65f), strokeWidth = 2.5f)

                        val targetX = if (isHealthy) 80f else 55f
                        drawLine(relayColor, Offset(ox + 30f, oy + 54f), Offset(ox + targetX, oy + 54f), strokeWidth = 3f)
                    }

                    ComponentType.FUSE -> {
                        drawLine(strokeColor, Offset(ox + 15f, oy + 40f), Offset(ox + 30f, oy + 40f), strokeWidth = 3.5f)
                        drawLine(strokeColor, Offset(ox + 70f, oy + 40f), Offset(ox + 85f, oy + 40f), strokeWidth = 3.5f)
                        drawRect(
                            color = strokeColor,
                            topLeft = Offset(ox + 30f, oy + 30f),
                            size = androidx.compose.ui.geometry.Size(40f, 20f),
                            style = normalStroke
                        )
                        drawLine(strokeColor, Offset(ox + 30f, oy + 40f), Offset(ox + 70f, oy + 40f), strokeWidth = 2.5f)
                    }

                    ComponentType.BIMETAL_OVERLOAD -> {
                        drawLine(strokeColor, Offset(ox + 18f, oy + 40f), Offset(ox + 36f, oy + 40f), strokeWidth = 3.5f)
                        drawLine(strokeColor, Offset(ox + 64f, oy + 40f), Offset(ox + 82f, oy + 40f), strokeWidth = 3.5f)
                        val isTripped = comp.isBimetalTripped || comp.isClosedOrPressed
                        val bimColor = if (!isTripped) activeColor else Color(0xFFFF1744)
                        val path = Path().apply {
                            moveTo(ox + 36f, oy + 40f)
                            cubicTo(ox + 44f, oy + 28f, ox + 56f, oy + 52f, ox + 64f, oy + 40f)
                        }
                        drawPath(path, bimColor, style = Stroke(width = 4f, cap = StrokeCap.Round))
                        if (isTripped) {
                            drawLine(Color(0xFFFF1744), Offset(ox + 44f, oy + 22f), Offset(ox + 56f, oy + 34f), strokeWidth = 2.5f)
                            drawLine(Color(0xFFFF1744), Offset(ox + 56f, oy + 22f), Offset(ox + 44f, oy + 34f), strokeWidth = 2.5f)
                        }
                    }

                    ComponentType.MAIN_POWER_CONTACTS_3P -> {
                        val isClosed = comp.isClosedOrPressed
                        val pwrColor = if (isClosed) activeColor else strokeColor
                        val yOffsets = listOf(20f, 40f, 60f)
                        for (py in yOffsets) {
                            drawLine(strokeColor, Offset(ox + 18f, oy + py), Offset(ox + 38f, oy + py), strokeWidth = 3f)
                            drawLine(strokeColor, Offset(ox + 62f, oy + py), Offset(ox + 82f, oy + py), strokeWidth = 3f)
                            val endY = if (isClosed) py else py - 12f
                            drawLine(pwrColor, Offset(ox + 38f, oy + py), Offset(ox + 62f, oy + endY), strokeWidth = 3.5f)
                        }
                    }

                    ComponentType.PROXIMITY_SENSOR -> {
                        drawRect(strokeColor, Offset(ox + 25f, oy + 20f), androidx.compose.ui.geometry.Size(50f, 40f), style = normalStroke)
                        val diamondPath = Path().apply {
                            moveTo(ox + 50f, oy + 25f)
                            lineTo(ox + 65f, oy + 40f)
                            lineTo(ox + 50f, oy + 55f)
                            lineTo(ox + 35f, oy + 40f)
                            close()
                        }
                        drawPath(diamondPath, if (comp.isEnergized) activeColor else strokeColor, style = Stroke(width = 2.5f))
                        if (comp.isEnergized) {
                            drawCircle(Color(0xFFFFD600), radius = 5f, center = Offset(ox + 50f, oy + 40f))
                        }
                    }

                    ComponentType.CONTACTOR_COIL, ComponentType.TIMER_ON_DELAY_COIL, ComponentType.TIMER_OFF_DELAY_COIL -> {
                        val coilBg = if (comp.isEnergized) Color(0x3300E676) else Color(0x1A00E5FF)
                        val coilBorder = if (comp.isEnergized) activeColor else strokeColor
                        drawRect(
                            color = coilBg,
                            topLeft = Offset(ox + 25f, oy + 20f),
                            size = androidx.compose.ui.geometry.Size(50f, 40f)
                        )
                        drawRect(
                            color = coilBorder,
                            topLeft = Offset(ox + 25f, oy + 20f),
                            size = androidx.compose.ui.geometry.Size(50f, 40f),
                            style = normalStroke
                        )
                        if (comp.type == ComponentType.TIMER_ON_DELAY_COIL) {
                            drawLine(coilBorder, Offset(ox + 25f, oy + 20f), Offset(ox + 40f, oy + 60f), strokeWidth = 2.5f)
                        } else if (comp.type == ComponentType.TIMER_OFF_DELAY_COIL) {
                            drawLine(coilBorder, Offset(ox + 60f, oy + 20f), Offset(ox + 75f, oy + 60f), strokeWidth = 2.5f)
                        }
                    }

                    ComponentType.PILOT_LAMP_GREEN, ComponentType.PILOT_LAMP_RED, ComponentType.PILOT_LAMP_YELLOW -> {
                        val lampColor = when (comp.type) {
                            ComponentType.PILOT_LAMP_GREEN -> Color(0xFF00E676)
                            ComponentType.PILOT_LAMP_RED -> Color(0xFFFF1744)
                            else -> Color(0xFFFFEA00)
                        }
                        val center = Offset(ox + 50f, oy + 40f)
                        drawCircle(strokeColor, radius = 22f, center = center, style = normalStroke)
                        if (comp.isEnergized) {
                            drawCircle(lampColor.copy(alpha = 0.4f), radius = 32f, center = center)
                            drawCircle(lampColor, radius = 20f, center = center)
                        }
                        drawLine(strokeColor, Offset(center.x - 14f, center.y - 14f), Offset(center.x + 14f, center.y + 14f), strokeWidth = 3f)
                        drawLine(strokeColor, Offset(center.x + 14f, center.y - 14f), Offset(center.x - 14f, center.y + 14f), strokeWidth = 3f)
                    }

                    ComponentType.MOTOR_3PHASE, ComponentType.MOTOR_1PHASE -> {
                        val motorCenter = Offset(ox + 55f, oy + 45f)
                        val motorColor = if (comp.isEnergized) activeColor else strokeColor
                        drawCircle(motorColor, radius = 28f, center = motorCenter, style = normalStroke)

                        if (comp.isEnergized) {
                            drawCircle(Color(0x3300E676), radius = 38f, center = motorCenter)
                            // انیمیشن چرخش جهت موتور
                            if (comp.motorRotationDirection == MotorDirection.CLOCKWISE) {
                                drawArc(
                                    color = Color(0xFF00E676),
                                    startAngle = 45f,
                                    sweepAngle = 270f,
                                    useCenter = false,
                                    topLeft = Offset(motorCenter.x - 16f, motorCenter.y - 16f),
                                    size = androidx.compose.ui.geometry.Size(32f, 32f),
                                    style = Stroke(width = 3f)
                                )
                            } else if (comp.motorRotationDirection == MotorDirection.COUNTER_CLOCKWISE) {
                                drawArc(
                                    color = Color(0xFFFFAB00),
                                    startAngle = 225f,
                                    sweepAngle = -270f,
                                    useCenter = false,
                                    topLeft = Offset(motorCenter.x - 16f, motorCenter.y - 16f),
                                    size = androidx.compose.ui.geometry.Size(32f, 32f),
                                    style = Stroke(width = 3f)
                                )
                            }
                        }
                    }

                    else -> {
                        drawRect(
                            color = strokeColor,
                            topLeft = Offset(ox + 25f, oy + 25f),
                            size = androidx.compose.ui.geometry.Size(50f, 50f),
                            style = normalStroke
                        )
                    }
                }
            }

            // رسم پایانه‌ها (Terminals) با ابعاد بزرگتر و وضوح صنعتی
            for (term in comp.getTerminals().distinctBy { "${it.relX.toInt()}_${it.relY.toInt()}" }) {
                val tx = comp.x + term.relX
                val ty = comp.y + term.relY
                val isActive = activeTerminalId == term.id

                // در حالت لمس یا فعال بودن، هاله‌ی درخشان و بزرگ
                if (isActive) {
                    drawCircle(
                        color = Color(0x66FFD600),
                        radius = 16f,
                        center = Offset(tx, ty)
                    )
                    drawCircle(
                        color = Color(0xFFFFD600),
                        radius = 11f,
                        center = Offset(tx, ty),
                        style = Stroke(width = 3f)
                    )
                }

                // ترمینال فیزیکی اصلی
                drawCircle(
                    color = if (isActive) Color(0xFFFFD600) else Color(0xFFFFAB00),
                    radius = 7.5f,
                    center = Offset(tx, ty)
                )
                drawCircle(
                    color = Color.Black,
                    radius = 3f,
                    center = Offset(tx, ty)
                )

                // برچسب شماره ترمینال (A1, A2, 13, 14, 95, 96 ...)
                drawIntoCanvas { canvas ->
                    val termTextPaint = Paint().apply {
                        color = if (isActive) android.graphics.Color.parseColor("#FFD600") else android.graphics.Color.parseColor("#CFD8DC")
                        textSize = 12f
                        typeface = Typeface.DEFAULT_BOLD
                        isAntiAlias = true
                    }
                    val labelOffsetX = if (term.relX < 30f) -24f else 12f
                    val labelOffsetY = if (term.relY < 30f) -8f else 16f
                    canvas.nativeCanvas.drawText(term.name, tx + labelOffsetX, ty + labelOffsetY, termTextPaint)
                }
            }
        }
    }
}
