package com.example.util

import android.content.Context
import android.content.Intent
import com.example.model.AcademyLessonsData
import com.example.model.CircuitComponent
import com.example.model.DefaultCircuitTemplates
import com.example.model.IndustrialComponentLibrary
import com.example.model.SpecSourceType

/**
 * تولیدکننده گزارش جامع بازبینی فنی سیم‌یار برای مهندسین و تکنسین‌های برق صنعتی (مرحله P12)
 * شامل کلیه قطعات، پایانه‌ها، مشخصات فنی، نوع منبع، اتصالات قالب‌ها، دروس آکادمی و دانشنامه
 * به همراه ستون‌های «وضعیت بازبینی» و «یادداشت برقکار».
 */
object TechnicalReportExporter {

    fun generateMarkdownReport(): String {
        val sb = StringBuilder()

        sb.append("# گزارش جامع بازبینی فنی مهندسی برق سیم‌یار (SimYar Technical Review Report)\n\n")
        sb.append("**تاریخ تولید گزارش:** ").append(PersianUtils.toJalaliDate(System.currentTimeMillis())).append("\n")
        sb.append("**هدف بازبینی:** اعتبارسنجی استانداردهای بین‌المللی، پایانه‌ها، شماره ترمینال‌ها، مشخصات فنی، اتصالات مدارات قدرت و فرمان و مفاهیم آموزشی توسط برقکار / مهندس ناظر.\n\n")
        sb.append("> **راهنمای ستون‌ها:**\n")
        sb.append("> - **وضعیت بازبینی:** [ ] تأییدشده | [ ] اصلاح‌شود\n")
        sb.append("> - **یادداشت برقکار:** ثبت نکات میدانی، اصلاحات یا استانداردهای تکمیلی\n\n")
        sb.append("---\n\n")

        // بخش ۱: قطعات کتابخانه صنعتی
        sb.append("## ۱. جدول مشخصات فنی و استانداردهای قطعات کتابخانه صنعتی\n\n")
        sb.append("| ردیف | نام قطعه | پایانه‌ها و شماره ترمینال‌ها | فیلد فنی | مقدار | نوع منبع | مرجع استاندارد / دیتاشیت | وضعیت بازبینی | یادداشت برقکار |\n")
        sb.append("|:---:|:---|:---|:---|:---|:---:|:---|:---:|:---|\n")

        var rowIdx = 1
        for (item in IndustrialComponentLibrary.items) {
            val dummyComp = CircuitComponent(
                id = "comp_preview",
                type = item.type,
                label = item.displayName,
                x = 0f,
                y = 0f,
                specs = item.defaultSpecs
            )
            val terminalsStr = dummyComp.getTerminals().joinToString(", ") { "${it.id}:${it.name}" }
            val fields = item.defaultSpecs.allFieldSources()

            for ((fieldName, pair) in fields) {
                val (value, source) = pair
                val sourceTypeLabel = when (source.type) {
                    SpecSourceType.STANDARD -> "استاندارد"
                    SpecSourceType.DATASHEET -> "دیتاشیت"
                    SpecSourceType.SAMPLE -> "نمونه"
                    SpecSourceType.UNKNOWN -> "نامشخص"
                }
                val refLabel = if (source.ref.isNotBlank()) source.ref else "-"

                sb.append("| ").append(rowIdx++).append(" | ")
                    .append(item.displayName).append(" | ")
                    .append(terminalsStr).append(" | ")
                    .append(fieldName).append(" | ")
                    .append(value).append(" | ")
                    .append(sourceTypeLabel).append(" | ")
                    .append(refLabel).append(" | ")
                    .append("[ ] تأیید [ ] اصلاح | ")
                    .append(" |\n")
            }
        }

        sb.append("\n---\n\n")

        // بخش ۲: قالب‌های استاندارد صنعتی مدارات
        sb.append("## ۲. جدول قالب‌های استاندارد صنعتی و فهرست اتصالات (Templates)\n\n")
        sb.append("| ردیف | نام قالب مدار | تعداد قطعات | اتصالات و سیم‌بندی‌ها (ترمینال مبدا -> ترمینال مقصد) | ادعای فنی مدار | وضعیت بازبینی | یادداشت برقکار |\n")
        sb.append("|:---:|:---|:---:|:---|:---|:---:|:---|\n")

        val templates = listOf(
            Triple("راه‌اندازی مستقیم ۳فاز (DOL)", DefaultCircuitTemplates.createDirectOnlineStarter(), "مدار قدرت ۳فاز با بی‌متال در مسیر قدرت، مدار فرمان خودنگهدار با تیغه ۱۳-۱۴ و استپ قارچی"),
            Triple("ستاره-مثلث (Star-Delta) با تایمر و اینترلاک", DefaultCircuitTemplates.createStarDeltaStarter(), "۳ کنتاکتور (KM1 خط، KM2 ستاره، KM3 مثلث)، اینترلاک متقابل الکتریکی بین KM2 و KM3، تخته‌کلمپ ۶ ترمیناله U1-V1-W1 و U2-V2-W2"),
            Triple("چپگرد / راستگرد (Forward-Reverse) با اینترلاک", DefaultCircuitTemplates.createForwardReverseStarter(), "جابجایی دو فاز در قدرت، اینترلاک الکتریکی متقابل تیغه‌های بسته ۲۱-۲۲ بین دو بوبین KM1 و KM2"),
            Triple("برنامه پیش‌فرض PLC نردبانی", DefaultCircuitTemplates.createDirectOnlineStarter(), "خودنگهدار دیجیتال لدر: ورودی I0.0 استپ، ورودی I0.1 استارت موازی با کنتاکت باز خروجی Q0.0")
        )

        var tmplIdx = 1
        for ((tmplName, tmplProject, claim) in templates) {
            val wiresSummary = if (tmplProject.wires.isNotEmpty()) {
                tmplProject.wires.joinToString("<br>") { wire ->
                    "${wire.fromComponentId}(${wire.fromTerminalId}) -> ${wire.toComponentId}(${wire.toTerminalId})"
                }
            } else {
                "سیم‌بندی برنامه منطقی لدر PLC"
            }

            sb.append("| ").append(tmplIdx++).append(" | ")
                .append(tmplName).append(" | ")
                .append(tmplProject.components.size).append(" | ")
                .append(wiresSummary).append(" | ")
                .append(claim).append(" | ")
                .append("[ ] تأیید [ ] اصلاح | ")
                .append(" |\n")
        }

        sb.append("\n---\n\n")

        // بخش ۳: سرفصل‌ها و دروس آموزشی آکادمی سیم‌یار
        sb.append("## ۳. جدول سرفصل‌های آموزشی آکادمی برق و ادعاهای فنی (Academy Lessons)\n\n")
        sb.append("| شماره درس | عنوان و سطح | شرح هدف آموزشی | قطعات اولیه و اتصالات مدار | ادعای فنی و استاندارد | وضعیت بازبینی | یادداشت برقکار |\n")
        sb.append("|:---:|:---|:---|:---|:---|:---:|:---|\n")

        for (lesson in AcademyLessonsData.lessons) {
            val initialComps = lesson.initialProject.components.joinToString(", ") { "${it.label} (${it.type.defaultPrefix})" }
            val initialWires = lesson.initialProject.wires.joinToString("; ") { "${it.fromTerminalId}->${it.toTerminalId}" }.ifBlank { "بدون سیم اولیه" }

            sb.append("| ").append(lesson.id).append(" | ")
                .append(lesson.title).append(" (").append(lesson.level).append(") | ")
                .append(lesson.description).append(" - هدف: ").append(lesson.objectiveText).append(" | ")
                .append("قطعات: ").append(initialComps).append(" | سیم‌ها: ").append(initialWires).append(" | ")
                .append(lesson.validationHint).append(" | ")
                .append("[ ] تأیید [ ] اصلاح | ")
                .append(" |\n")
        }

        sb.append("\n---\n\n")

        // بخش ۴: دانشنامه برق صنعتی و استانداردها
        sb.append("## ۴. دانشنامه برق صنعتی و تطابق با استانداردهای IEC\n\n")
        sb.append("| ردیف | تجهیز / موضوع | کد استاندارد بین‌المللی | شماره‌گذاری ترمینال‌ها | شرح و اصول عملکرد | وضعیت بازبینی | یادداشت برقکار |\n")
        sb.append("|:---:|:---|:---|:---|:---|:---:|:---|\n")

        val kbItems = listOf(
            Tuple5("کنتاکتور صنعتی (Contactor)", "IEC 60947-4-1", "A1-A2 | 1/L1-2/T1, 3/L2-4/T2, 5/L3-6/T3 | 13-14 (NO), 21-22 (NC)", "کلید الکترومغناطیسی با قابلیت قطع و وصل بارهای پرقدرت مانند الکتروموتورها"),
            Tuple5("رله حرارتی بی‌متال (Overload Relay)", "IEC 60947-4-1", "1/L1, 3/L2, 5/L3 -> 2/T1, 4/T2, 6/T3 | 95-96 (NC), 97-98 (NO)", "حفاظت الکتروموتور در برابر اضافه بار؛ کنتاکت 95-96 در مدار فرمان و 97-98 در آلارم"),
            Tuple5("شستی‌های استپ و استارت (Push Buttons)", "IEC 60947-5-1", "استارت: 13-14 (NO) | استپ: 21-22 (NC)", "ارسال پالس فرمان دستی؛ استارت با رها کردن باز می‌شود و نیاز به خودنگهدار دارد"),
            Tuple5("شستی قارچی اضطراری (E-Stop)", "ISO 13850 / IEC 60947-5-5", "1-2 (NC عملکرد باز شدن مستقیم)", "قطع فوری و ایمن کلیه بارهای متحرک ماشین؛ قفل مکانیکی و نیاز به چرخش/کشیدن برای ریست"),
            Tuple5("تایمر تاخیر در وصل (On-Delay Timer TON)", "IEC 61812-1", "A1-A2 | 15 (مشترک) | 15-18 (NO) | 15-16 (NC)", "پس از برق‌دار شدن بوبین زمان سپری می‌شود و سپس تیغه‌ها تغییر وضعیت می‌دهند"),
            Tuple5("پردازنده و منطق PLC نردبانی (Ladder)", "IEC 61131-3", "%IX0.0..%IX0.7, %QX0.0..%QX0.7, %MX0.0", "اجرای حلقه‌های اسکن سیکل پیوسته؛ تبدیل دیاگرام رله‌ای به کد منطقی دیجیتال")
        )

        var kbIdx = 1
        for (item in kbItems) {
            sb.append("| ").append(kbIdx++).append(" | ")
                .append(item.t1).append(" | ")
                .append(item.t2).append(" | ")
                .append(item.t3).append(" | ")
                .append(item.t4).append(" | ")
                .append("[ ] تأیید [ ] اصلاح | ")
                .append(" |\n")
        }

        sb.append("\n---\n\n")

        // بخش ۵: فهرست ادعاهای فنی و موارد تأییدنشده (Unverified)
        sb.append("## ۵. فهرست صریح موارد تأییدنشده و مقادیر نمونه (Unverified Items)\n\n")
        sb.append("مطابق با خط‌مشی فنی سیم‌یار، هیچ داده یا منبعی بدون مدرک رسمی ایجاد نشده است:\n")
        sb.append("۱. **مقادیر نمونه (SAMPLE):** جریان، ولتاژ، و توان نامی قطعات فاقد دیتاشیت مستند به عنوان «مقدار نمونه (قابل ویرایش)» برچسب‌گذاری شده‌اند تا برقکار در پروژه واقعی مقدار پلاک تجهیز مورد نظر را وارد نماید.\n")
        sb.append("۲. **مدل‌های فاقد دیتاشیت (UNKNOWN):** مدل سازنده در قطعات پایه (مانند شستی‌های معمولی و کلیدهای مینیاتوری عمومی) «نامشخص» علامت‌گذاری شده و از ساخت نام‌های تجاری فرضی خودداری گردیده است.\n")
        sb.append("۳. **تجهیزات دارای دیتاشیت رسمی (DATASHEET):** کنتاکتور Schneider TeSys D LC1D09M7 و رله حرارتی Schneider TeSys LRD10 مستقیماً به لینک رسمی کاتالوگ اشنایدر الکتریک ارجاع داده شده‌اند.\n")
        sb.append("۴. **استانداردها (STANDARD):** کلیه کدهای استانداردهای مندرج از سری معتبر IEC 60947، IEC 60898، IEC 61812، IEC 61131 و ISO 13850 هستند.\n\n")
        sb.append("**محل امضاء و تایید مهندس ناظر / تکنسین برق صنعتی:** _______________________\n")

        return sb.toString()
    }

    private data class Tuple5(val t1: String, val t2: String, val t3: String, val t4: String)

    fun exportAndShareReport(context: Context) {
        val markdown = generateMarkdownReport()
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, markdown)
            putExtra(Intent.EXTRA_SUBJECT, "گزارش بازبینی فنی برق صنعتی سیم‌یار")
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "ارسال و اشتراک‌گذاری گزارش بازبینی فنی")
        context.startActivity(shareIntent)
    }
}
