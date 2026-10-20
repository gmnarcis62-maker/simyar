package com.example.util

import java.util.Calendar
import java.util.TimeZone

object PersianUtils {

    private val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

    fun toPersianDigits(input: String): String {
        val sb = StringBuilder()
        for (ch in input) {
            if (ch in '0'..'9') {
                sb.append(persianDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun toPersianDigits(number: Number): String {
        return toPersianDigits(number.toString())
    }

    fun getPersianDateString(): String {
        return toJalaliDate(System.currentTimeMillis())
    }

    /**
     * تبدیل میلی‌ثانیه زمان به تاریخ شمسی با در نظر گرفتن منطقه زمانی رسمی ایران (Asia/Tehran)
     * تا از بروز خطای عقب افتادن تاریخ در ساعات ۲۰:۳۰ تا ۲۴:۰۰ به وقت ایران جلوگیری شود.
     */
    fun toJalaliDate(timestamp: Long): String {
        val tehranTz = TimeZone.getTimeZone("Asia/Tehran")
        val calendar = Calendar.getInstance(tehranTz).apply {
            timeInMillis = timestamp
        }

        val gYear = calendar.get(Calendar.YEAR)
        val gMonth = calendar.get(Calendar.MONTH) + 1
        val gDay = calendar.get(Calendar.DAY_OF_MONTH)

        val (jYear, jMonth, jDay) = gregorianToJalaliExact(gYear, gMonth, gDay)

        val monthNames = arrayOf(
            "فروردین", "اردیبهشت", "خرداد",
            "تیر", "مرداد", "شهریور",
            "مهر", "آبان", "آذر",
            "دی", "بهمن", "اسفند"
        )
        val monthName = monthNames.getOrElse(jMonth - 1) { "" }
        return "${toPersianDigits(jDay)} $monthName ${toPersianDigits(jYear)}"
    }

    /**
     * الگوریتم استاندارد و بسیار دقیق تبدیل تقویم میلادی به جلالی
     */
    fun gregorianToJalaliExact(gy: Int, gm: Int, gd: Int): Triple<Int, Int, Int> {
        val gDaysInMonth = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        val gy2 = if (gm > 2) gy + 1 else gy
        var gDayCount = 355666 + (365 * gy) + ((gy2 + 3) / 4) - ((gy2 + 99) / 100) + ((gy2 + 399) / 400) + gd + gDaysInMonth[gm - 1]
        var jy = -1595 + (33 * (gDayCount / 12053))
        gDayCount %= 12053
        jy += 4 * (gDayCount / 1461)
        gDayCount %= 1461
        if (gDayCount > 365) {
            jy += (gDayCount - 1) / 365
            gDayCount = (gDayCount - 1) % 365
        }
        val jm: Int
        val jd: Int
        if (gDayCount < 186) {
            jm = 1 + (gDayCount / 31)
            jd = 1 + (gDayCount % 31)
        } else {
            jm = 7 + ((gDayCount - 186) / 30)
            jd = 1 + ((gDayCount - 186) % 30)
        }
        return Triple(jy, jm, jd)
    }
}
