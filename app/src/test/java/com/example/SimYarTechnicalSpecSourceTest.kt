package com.example

import com.example.model.IndustrialComponentLibrary
import com.example.model.SpecSource
import com.example.model.SpecSourceType
import com.example.model.TechnicalSpecs
import com.example.util.TechnicalReportExporter
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * تست‌های واحد بند ۴ مرحله P12:
 * اعتبارسنجی منابع داده‌های فنی (SpecSource) و گزارش بازبینی برقکار
 * شکست در صورت نبود منبع برای هر فیلد فنی یا STANDARD بودن بدون ref
 */
class SimYarTechnicalSpecSourceTest {

    // ۱. تست عدم وجود فیلد بدون SpecSource و اجباری بودن ref برای هر منبع STANDARD
    @Test
    fun test01_AllLibraryItemsMustHaveValidSpecSources_AndStandardMustHaveRef() {
        val items = IndustrialComponentLibrary.items
        assertTrue("کتابخانه قطعات صنعتی نباید خالی باشد", items.isNotEmpty())

        for (item in items) {
            val specs = item.defaultSpecs
            val fieldSources = specs.allFieldSources()
            assertTrue("کلیه فیلدهای مشخصات فنی برای ${item.displayName} باید استخراج شوند", fieldSources.isNotEmpty())

            for ((fieldName, pair) in fieldSources) {
                val (value, source) = pair
                assertNotNull("فیلد $fieldName برای ${item.displayName} نباید منبع null داشته باشد", source)

                // اگر نوع منبع STANDARD باشد، ref (شماره و بند استاندارد) الزامی است
                if (source.type == SpecSourceType.STANDARD) {
                    assertTrue(
                        "خطا: در قطعه ${item.displayName}، فیلد $fieldName به عنوان STANDARD تعریف شده اما فاقد مرجع استاندارد (ref) است!",
                        source.ref.isNotBlank()
                    )
                }

                // اگر نوع منبع DATASHEET باشد، لینک معتبر الزامی است
                if (source.type == SpecSourceType.DATASHEET) {
                    assertTrue(
                        "خطا: در قطعه ${item.displayName}، فیلد $fieldName به عنوان DATASHEET تعریف شده اما فاقد لینک دیتاشیت است!",
                        source.ref.isNotBlank() && (source.ref.startsWith("http://") || source.ref.startsWith("https://"))
                    )
                }
            }
        }
    }

    // ۲. تست اینکه داده‌های قدیمی به‌صورت پیش‌فرض SAMPLE یا UNKNOWN هستند و هرگز STANDARD نیستند
    @Test
    fun test02_LegacyDefaultTechnicalSpecs_MustDefaultToSampleOrUnknown_NeverStandard() {
        val legacySpecs = TechnicalSpecs()

        assertFalse("پیش‌فرض ولتاژ نباید STANDARD باشد", legacySpecs.voltageSource.type == SpecSourceType.STANDARD)
        assertFalse("پیش‌فرض جریان نباید STANDARD باشد", legacySpecs.currentSource.type == SpecSourceType.STANDARD)
        assertFalse("پیش‌فرض توان نباید STANDARD باشد", legacySpecs.powerSource.type == SpecSourceType.STANDARD)
        assertFalse("پیش‌فرض کد استاندارد نباید STANDARD باشد", legacySpecs.standardCodeSource.type == SpecSourceType.STANDARD)
        assertFalse("پیش‌فرض مدل سازنده نباید STANDARD باشد", legacySpecs.modelSource.type == SpecSourceType.STANDARD)
        assertFalse("پیش‌فرض کنتاکت‌ها نباید STANDARD باشد", legacySpecs.contactsSource.type == SpecSourceType.STANDARD)

        assertTrue(
            "پیش‌فرض ولتاژ باید SAMPLE یا UNKNOWN باشد",
            legacySpecs.voltageSource.type == SpecSourceType.SAMPLE || legacySpecs.voltageSource.type == SpecSourceType.UNKNOWN
        )
        assertTrue(
            "پیش‌فرض جریان باید SAMPLE یا UNKNOWN باشد",
            legacySpecs.currentSource.type == SpecSourceType.SAMPLE || legacySpecs.currentSource.type == SpecSourceType.UNKNOWN
        )
    }

    // ۳. تست اعتبارسنجی شکست در صورت تعریف منبع STANDARD بدون ref
    @Test
    fun test03_FailureWhenStandardSourceHasBlankRef() {
        fun validateSpecSource(source: SpecSource): Boolean {
            if (source.type == SpecSourceType.STANDARD && source.ref.isBlank()) {
                return false
            }
            return true
        }

        val invalidStandardSource = SpecSource(type = SpecSourceType.STANDARD, ref = "")
        assertFalse("منبع STANDARD با ref خالی باید نامعتبر باشد و شکست بخورد", validateSpecSource(invalidStandardSource))

        val invalidBlankRefSource = SpecSource(type = SpecSourceType.STANDARD, ref = "   ")
        assertFalse("منبع STANDARD با ref متشکل از فاصله باید نامعتبر باشد", validateSpecSource(invalidBlankRefSource))

        val validStandardSource = SpecSource(type = SpecSourceType.STANDARD, ref = "IEC 60947-4-1 بند 1")
        assertTrue("منبع STANDARD با بند مشخص معتبر است", validateSpecSource(validStandardSource))
    }

    // ۴. تست تولید کامل گزارش بازبینی فنی با تمامی جداول و ستون‌های بازبینی برقکار
    @Test
    fun test04_TechnicalReportExporter_ContainsAllRequiredSectionsAndColumns() {
        val report = TechnicalReportExporter.generateMarkdownReport()
        assertTrue("گزارش بازبینی نباید خالی باشد", report.isNotBlank())

        // بررسی ستون‌های بازبینی برقکار
        assertTrue("گزارش باید شامل ستون وضعیت بازبینی باشد", report.contains("وضعیت بازبینی"))
        assertTrue("گزارش باید شامل ستون یادداشت برقکار باشد", report.contains("یادداشت برقکار"))
        assertTrue("گزارش باید حاوی چک‌باکس تایید/اصلاح باشد", report.contains("[ ] تأیید") || report.contains("[ ] اصلاح"))

        // بررسی حضور قطعات، قالب‌ها، دروس و دانشنامه
        assertTrue("گزارش باید شامل جدول قطعات کتابخانه باشد", report.contains("جدول مشخصات فنی و استانداردهای قطعات کتابخانه صنعتی"))
        assertTrue("گزارش باید شامل قالب‌های مدارات باشد", report.contains("جدول قالب‌های استاندارد صنعتی"))
        assertTrue("گزارش باید شامل دروس آکادمی باشد", report.contains("جدول سرفصل‌های آموزشی آکادمی"))
        assertTrue("گزارش باید شامل دانشنامه IEC باشد", report.contains("دانشنامه برق صنعتی و تطابق با استانداردهای IEC"))
        assertTrue("گزارش باید بخش موارد تاییدنشده و مقادیر نمونه را داشته باشد", report.contains("فهرست صریح موارد تأییدنشده و مقادیر نمونه"))

        // بررسی نمونه قطعات کلیدی
        assertTrue(report.contains("کنتاکتور"))
        assertTrue(report.contains("بی‌متال"))
        assertTrue(report.contains("IEC 60947"))
        assertTrue(report.contains("Schneider"))
    }
}
