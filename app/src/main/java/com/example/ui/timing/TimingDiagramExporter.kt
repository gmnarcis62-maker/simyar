package com.example.ui.timing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Environment
import android.widget.Toast
import com.example.util.PersianUtils
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * مدیریت خروجی تصویر PNG از نمودار زمانی لاجیک سیگنال‌ها
 */
object TimingDiagramExporter {

    /**
     * رسم و ذخیره تصویر باکیفیت PNG از نمودار زمانی
     */
    fun exportToPng(
        context: Context,
        selectedSignals: List<TrackableSignal>,
        buffer: CircularSignalBuffer,
        totalDurationSeconds: Float
    ): File? {
        if (selectedSignals.isEmpty()) {
            Toast.makeText(context, "هیچ سیگنالی برای استخراج انتخاب نشده است", Toast.LENGTH_SHORT).show()
            return null
        }

        try {
            val width = 1400
            val laneHeight = 90
            val headerHeight = 120
            val footerHeight = 60
            val height = headerHeight + selectedSignals.size * laneHeight + footerHeight

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // پس‌زمینه تیره باکیفیت صنعتی
            canvas.drawColor(Color.parseColor("#0F172A"))

            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

            // ۱. سربرگ و عنوان
            paint.color = Color.parseColor("#1E293B")
            canvas.drawRect(0f, 0f, width.toFloat(), headerHeight.toFloat(), paint)

            textPaint.color = Color.WHITE
            textPaint.textSize = 28f
            textPaint.isFakeBoldText = true
            canvas.drawText("نمودار زمانی سیگنال‌های دیجیتال (Logic Timing Diagram)", 30f, 45f, textPaint)

            textPaint.color = Color.parseColor("#94A3B8")
            textPaint.textSize = 18f
            textPaint.isFakeBoldText = false
            val timeRangeText = "بازه زمانی ثبت‌شده: ${PersianUtils.toPersianDigits(String.format(Locale.US, "%.1f", totalDurationSeconds))} ثانیه | نرخ نمونه‌برداری: ۶۰Hz"
            canvas.drawText(timeRangeText, 30f, 80f, textPaint)

            // ۲. خطوط محور زمان
            val labelWidth = 280f
            val waveWidth = width - labelWidth - 40f
            val (minT, maxT) = buffer.getTimeRange()
            val startTime = if (maxT - minT > 0f) minT else 0f
            val endTime = if (maxT > startTime) maxT else 10f
            val duration = (endTime - startTime).coerceAtLeast(1f)
            val pxPerSec = waveWidth / duration

            // خطوط تیک محور زمان
            paint.color = Color.parseColor("#334155")
            paint.strokeWidth = 2f
            canvas.drawLine(labelWidth, headerHeight.toFloat(), width - 30f, headerHeight.toFloat(), paint)

            val step = if (duration > 30f) 5f else if (duration > 10f) 2f else 1f
            var curT = 0f
            textPaint.color = Color.parseColor("#38BDF8")
            textPaint.textSize = 16f
            while (curT <= duration) {
                val x = labelWidth + curT * pxPerSec
                canvas.drawLine(x, headerHeight - 10f, x, (height - footerHeight).toFloat(), paint)
                val label = PersianUtils.toPersianDigits(String.format(Locale.US, "%.1fs", startTime + curT))
                canvas.drawText(label, x - 20f, headerHeight - 16f, textPaint)
                curT += step
            }

            // ۳. رسم هر نوار سیگنال
            selectedSignals.forEachIndexed { i, sig ->
                val topY = headerHeight + i * laneHeight
                val bottomY = topY + laneHeight

                // پس‌زمینه ستون برچسب
                paint.color = Color.parseColor("#1E293B")
                canvas.drawRect(0f, topY.toFloat(), labelWidth, bottomY.toFloat(), paint)

                // خط افقی نوار
                paint.color = Color.parseColor("#334155")
                paint.strokeWidth = 1f
                canvas.drawLine(0f, bottomY.toFloat(), width.toFloat(), bottomY.toFloat(), paint)

                // برچسب و نام سیگنال
                textPaint.color = Color.WHITE
                textPaint.textSize = 20f
                textPaint.isFakeBoldText = true
                canvas.drawText(sig.displayName, 40f, topY + 38f, textPaint)

                textPaint.color = Color.parseColor("#94A3B8")
                textPaint.textSize = 14f
                textPaint.isFakeBoldText = false
                canvas.drawText(sig.type.displayName, 40f, topY + 68f, textPaint)

                // رنگ شکل‌موج
                val waveColor = when (sig.type) {
                    SignalSourceType.PLC_OUTPUT -> Color.parseColor("#00E5FF")
                    SignalSourceType.PLC_INPUT -> Color.parseColor("#FFD600")
                    SignalSourceType.CIRCUIT_COIL -> Color.parseColor("#00E676")
                    SignalSourceType.PLC_TIMER_DONE -> Color.parseColor("#FF9100")
                    SignalSourceType.PLC_COUNTER_DONE -> Color.parseColor("#E040FB")
                    else -> Color.parseColor("#64B5F6")
                }

                // دایره رنگی نماد
                paint.color = waveColor
                canvas.drawCircle(22f, topY + 45f, 6f, paint)

                // شکل‌موج سیگنال
                val highY = topY + 22f
                val lowY = bottomY - 22f

                paint.color = waveColor
                paint.strokeWidth = 4f
                paint.style = Paint.Style.STROKE

                val samples = buffer.getSamples(sig.id)
                if (samples.isNotEmpty()) {
                    var curVal = buffer.getValueAtTime(sig.id, startTime)
                    var prevX = labelWidth
                    var prevY = if (curVal) highY else lowY

                    for (s in samples) {
                        if (s.timestampSeconds < startTime) {
                            curVal = s.value
                            prevY = if (curVal) highY else lowY
                            continue
                        }
                        val sx = labelWidth + (s.timestampSeconds - startTime) * pxPerSec
                        val sy = if (s.value) highY else lowY

                        if (s.value != curVal) {
                            canvas.drawLine(prevX, prevY, sx, prevY, paint)
                            canvas.drawLine(sx, prevY, sx, sy, paint)
                            curVal = s.value
                            prevY = sy
                        } else {
                            canvas.drawLine(prevX, prevY, sx, sy, paint)
                        }
                        prevX = sx
                    }
                    val finalX = labelWidth + duration * pxPerSec
                    if (finalX > prevX) {
                        canvas.drawLine(prevX, prevY, finalX, prevY, paint)
                    }
                } else {
                    // سیگنال بدون تغییر در سطح صفر
                    canvas.drawLine(labelWidth, lowY, labelWidth + duration * pxPerSec, lowY, paint)
                }
                paint.style = Paint.Style.FILL
            }

            // ۴. پانوشت تصویر
            paint.color = Color.parseColor("#141D2B")
            canvas.drawRect(0f, (height - footerHeight).toFloat(), width.toFloat(), height.toFloat(), paint)

            textPaint.color = Color.parseColor("#64748B")
            textPaint.textSize = 14f
            val stamp = "تولیدشده توسط نرم‌افزار سیم‌یار (SimYar) - ${SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date())}"
            canvas.drawText(PersianUtils.toPersianDigits(stamp), 30f, height - 22f, textPaint)

            // ۵. ذخیره فایل در حافظه موقت / دانلودها
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "SimYar_TimingDiagram_$timeStamp.png"
            val outputDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.cacheDir
            val destFile = File(outputDir, fileName)

            FileOutputStream(destFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            Toast.makeText(context, "نمودار زمانی با فرمت PNG ذخیره شد:\n${destFile.name}", Toast.LENGTH_LONG).show()
            return destFile
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "خطا در استخراج نمودار زمانی: ${e.message}", Toast.LENGTH_SHORT).show()
            return null
        }
    }
}
