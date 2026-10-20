package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.model.CircuitComponent
import com.example.model.WireConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object ExportUtils {

    /**
     * رندر نقشه شماتیک استاندارد به همراه کادر مهندسی و جدول مشخصات با پشتیبانی از نام طراح پویا
     */
    fun renderCircuitToBitmap(
        components: List<CircuitComponent>,
        wires: List<WireConnection>,
        projectTitle: String,
        designerName: String = "مهندس برق",
        width: Int = 1600,
        height: Int = 1100
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        canvas.drawColor(android.graphics.Color.WHITE)

        val borderPaint = Paint().apply {
            color = android.graphics.Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }

        val thinPaint = Paint().apply {
            color = android.graphics.Color.DKGRAY
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }

        val smallTextPaint = Paint().apply {
            color = android.graphics.Color.DKGRAY
            textSize = 14f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        // ۱. کادر استاندارد نقشه‌کشی IEC
        val margin = 30f
        canvas.drawRect(margin, margin, width - margin, height - margin, borderPaint)
        canvas.drawRect(margin + 10f, margin + 10f, width - margin - 10f, height - margin - 10f, thinPaint)

        // ۲. جدول مشخصات نقشه (Title Block)
        val titleBlockWidth = 460f
        val titleBlockHeight = 160f
        val tbLeft = width - margin - 10f - titleBlockWidth
        val tbTop = height - margin - 10f - titleBlockHeight
        val tbRight = width - margin - 10f
        val tbBottom = height - margin - 10f

        val tbBgPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#F8FAFC")
            style = Paint.Style.FILL
        }
        canvas.drawRect(tbLeft, tbTop, tbRight, tbBottom, tbBgPaint)
        canvas.drawRect(tbLeft, tbTop, tbRight, tbBottom, borderPaint)

        canvas.drawLine(tbLeft, tbTop + 45f, tbRight, tbTop + 45f, thinPaint)
        canvas.drawLine(tbLeft, tbTop + 85f, tbRight, tbTop + 85f, thinPaint)
        canvas.drawLine(tbLeft, tbTop + 125f, tbRight, tbTop + 125f, thinPaint)
        canvas.drawLine(tbLeft + 150f, tbTop + 45f, tbLeft + 150f, tbBottom, thinPaint)

        val dateStr = PersianUtils.getPersianDateString()

        canvas.drawText("پروژه: $projectTitle", tbLeft + 15f, tbTop + 30f, textPaint)
        canvas.drawText("طراح و ترسیم:", tbLeft + 15f, tbTop + 70f, smallTextPaint)
        canvas.drawText(designerName.ifBlank { "مهندس برق" }, tbLeft + 160f, tbTop + 70f, textPaint)
        canvas.drawText("تاریخ رسم:", tbLeft + 15f, tbTop + 110f, smallTextPaint)
        canvas.drawText(dateStr, tbLeft + 160f, tbTop + 110f, textPaint)
        canvas.drawText("استاندارد / نرم‌افزار:", tbLeft + 15f, tbTop + 148f, smallTextPaint)
        canvas.drawText("IEC 60947 | سیم‌یار (SimYar)", tbLeft + 160f, tbTop + 148f, smallTextPaint)

        // ۳. فهرست کامل قطعات (BOM) بدون برش نامناسب
        val bomLeft = width - margin - 10f - 400f
        val bomTop = margin + 15f
        var bomCurrentY = bomTop + 25f

        canvas.drawText("فهرست تجهیزات (BOM):", bomLeft, bomTop + 15f, textPaint)
        val groupedComps = components.groupBy { it.type.persianName }
        var rowIdx = 1
        for ((name, list) in groupedComps.entries) {
            val countStr = PersianUtils.toPersianDigits(list.size)
            canvas.drawText("$rowIdx. $name: $countStr عدد", bomLeft, bomCurrentY + 20f, smallTextPaint)
            bomCurrentY += 22f
            rowIdx++
            if (bomCurrentY > tbTop - 30f) break
        }

        // ۴. رسم سیم‌ها و اتصالات مدار
        val wirePaint = Paint().apply {
            color = android.graphics.Color.parseColor("#0284C7")
            style = Paint.Style.STROKE
            strokeWidth = 3f
            strokeCap = Paint.Cap.ROUND
            isAntiAlias = true
        }

        val wireNodePaint = Paint().apply {
            color = android.graphics.Color.parseColor("#0284C7")
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val minX = components.minOfOrNull { it.x } ?: 0f
        val maxX = components.maxOfOrNull { it.x + 100f } ?: 100f
        val minY = components.minOfOrNull { it.y } ?: 0f
        val maxY = components.maxOfOrNull { it.y + 80f } ?: 80f

        val cW = (maxX - minX).coerceAtLeast(100f)
        val cH = (maxY - minY).coerceAtLeast(80f)

        val availW = width - 520f
        val availH = height - 240f

        val scale = (availW / cW).coerceAtMost(availH / cH).coerceIn(0.6f, 1.4f)
        val drawOffsetX = 60f + (availW - cW * scale) / 2f - minX * scale
        val drawOffsetY = 60f + (availH - cH * scale) / 2f - minY * scale

        fun toMapX(x: Float) = x * scale + drawOffsetX
        fun toMapY(y: Float) = y * scale + drawOffsetY

        for (wire in wires) {
            val fromComp = components.find { it.id == wire.fromComponentId }
            val toComp = components.find { it.id == wire.toComponentId }
            if (fromComp != null && toComp != null) {
                val fromTerm = fromComp.getTerminals().find { it.id == wire.fromTerminalId }
                val toTerm = toComp.getTerminals().find { it.id == wire.toTerminalId }
                if (fromTerm != null && toTerm != null) {
                    val sx = toMapX(fromComp.x + fromTerm.relX)
                    val sy = toMapY(fromComp.y + fromTerm.relY)
                    val ex = toMapX(toComp.x + toTerm.relX)
                    val ey = toMapY(toComp.y + toTerm.relY)
                    val mx = (sx + ex) / 2f

                    canvas.drawLine(sx, sy, mx, sy, wirePaint)
                    canvas.drawLine(mx, sy, mx, ey, wirePaint)
                    canvas.drawLine(mx, ey, ex, ey, wirePaint)

                    canvas.drawCircle(sx, sy, 4.5f, wireNodePaint)
                    canvas.drawCircle(ex, ey, 4.5f, wireNodePaint)
                }
            }
        }

        // ۵. رسم قطعات با اطلاعات کامل
        val compPaint = Paint().apply {
            color = android.graphics.Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }

        val tagPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#0F172A")
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }

        for (comp in components) {
            val cx = toMapX(comp.x)
            val cy = toMapY(comp.y)
            val w = 90f * scale
            val h = 70f * scale

            canvas.drawRect(cx, cy, cx + w, cy + h, compPaint)
            canvas.drawText(comp.label, cx + 10f, cy - 8f, tagPaint)

            for (term in comp.getTerminals()) {
                val tx = toMapX(comp.x + term.relX)
                val ty = toMapY(comp.y + term.relY)
                canvas.drawCircle(tx, ty, 5f, borderPaint)
                canvas.drawText(term.name, tx + 6f, ty - 6f, smallTextPaint)
            }
        }

        return bitmap
    }

    /**
     * ذخیره فایل PNG در پس‌زمینه با Dispatchers.IO جهت جلوگیری از بلاک شدن ترد اصلی
     */
    suspend fun exportToPng(
        context: Context,
        components: List<CircuitComponent>,
        wires: List<WireConnection>,
        projectTitle: String,
        designerName: String = "مهندس برق"
    ): File = withContext(Dispatchers.IO) {
        val bitmap = renderCircuitToBitmap(components, wires, projectTitle, designerName)
        val file = File(context.cacheDir, "SimYar_${projectTitle.replace(" ", "_")}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        file
    }

    /**
     * تولید فایل رسمی PDF در پس‌زمینه با Dispatchers.IO
     */
    suspend fun exportToPdf(
        context: Context,
        components: List<CircuitComponent>,
        wires: List<WireConnection>,
        projectTitle: String,
        designerName: String = "مهندس برق"
    ): File = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(1600, 1100, 1).create()
        val page = pdfDocument.startPage(pageInfo)

        val bitmap = renderCircuitToBitmap(components, wires, projectTitle, designerName, width = 1600, height = 1100)
        page.canvas.drawBitmap(bitmap, 0f, 0f, null)

        pdfDocument.finishPage(page)

        val file = File(context.cacheDir, "SimYar_${projectTitle.replace(" ", "_")}.pdf")
        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        file
    }

    fun shareExportedFile(context: Context, file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "ارسال و اشتراک نقشه مهندسی"))
    }

    fun shareText(context: Context, text: String, title: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, title))
    }
}
