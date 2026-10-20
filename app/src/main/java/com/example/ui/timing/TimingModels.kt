package com.example.ui.timing

import java.util.concurrent.ConcurrentHashMap

/**
 * منبع سیگنال دیجیتال
 */
enum class SignalSourceType(val displayName: String) {
    PLC_INPUT("ورودی PLC (I)"),
    PLC_OUTPUT("خروجی PLC (Q)"),
    PLC_MARKER("حافظه میانی (M)"),
    PLC_TIMER_DONE("اتمام تایمر (T.DN)"),
    PLC_COUNTER_DONE("اتمام کانتر (C.DN)"),
    CIRCUIT_COIL("کویل / بوبین مدار"),
    CIRCUIT_COMPONENT("تجهیز / کلید مدار")
}

/**
 * تعریف سیگنال قابل مانیتور
 */
data class TrackableSignal(
    val id: String,
    val addressOrTag: String,
    val displayName: String,
    val type: SignalSourceType,
    val description: String = ""
)

/**
 * نمونه نقطه‌ای ثبت‌شده در زمان t
 */
data class SignalSample(
    val timestampSeconds: Float,
    val value: Boolean
)

/**
 * بافر حلقوی بهینه‌شده برای ذخیره تاریخچه زمانی سیگنال‌ها
 * با سقف زمانی (مثلاً ۶۰ ثانیه) جهت جلوگیری از رشد حافظه
 */
class CircularSignalBuffer(
    val maxDurationSeconds: Float = 60f,
    val maxSamplesPerSignal: Int = 1200
) {
    // نگه‌داری نمونه‌ها برای هر سیگنال در ساختار thread-safe
    private val signalMap = ConcurrentHashMap<String, ArrayDeque<SignalSample>>()

    /**
     * ثبت نمونه جدید برای یک سیگنال در زمان t
     */
    @Synchronized
    fun addSample(signalId: String, timeSeconds: Float, value: Boolean) {
        val deque = signalMap.getOrPut(signalId) { ArrayDeque(maxSamplesPerSignal) }
        
        // فیلتر کردن نمونه‌های تکراری در همان زمان اگر میلی‌ثانیه‌ای ثبت شوند
        val last = deque.lastOrNull()
        if (last != null && last.timestampSeconds == timeSeconds && last.value == value) {
            return
        }

        deque.addLast(SignalSample(timeSeconds, value))

        // ۱. پاک‌سازی نمونه‌های قدیمی‌تر از سقف زمان ۶۰ ثانیه‌ای
        val cutoffTime = timeSeconds - maxDurationSeconds
        while (deque.isNotEmpty() && deque.first().timestampSeconds < cutoffTime) {
            deque.removeFirst()
        }

        // ۲. محدود کردن تعداد کل نمونه‌ها جهت امنیت کامل حافظه
        while (deque.size > maxSamplesPerSignal) {
            deque.removeFirst()
        }
    }

    /**
     * ثبت یک گام زمانی مشترک برای مجموعه‌ای از سیگنال‌ها
     */
    @Synchronized
    fun recordBatch(timeSeconds: Float, snapshot: Map<String, Boolean>) {
        snapshot.forEach { (signalId, value) ->
            addSample(signalId, timeSeconds, value)
        }
    }

    /**
     * خواندن تاریخچه نمونه‌های یک سیگنال به صورت لیست تغییرناپذیر
     */
    @Synchronized
    fun getSamples(signalId: String): List<SignalSample> {
        return signalMap[signalId]?.toList() ?: emptyList()
    }

    /**
     * پیدا کردن مقدار یک سیگنال در یک زمان خاص (نزدیک‌ترین نمونه قبلی یا هم‌زمان)
     */
    @Synchronized
    fun getValueAtTime(signalId: String, timeSeconds: Float): Boolean {
        val list = signalMap[signalId] ?: return false
        if (list.isEmpty()) return false
        
        // نمونه‌های قبل از زمان مقصد را جستجو می‌کنیم
        var lastVal = list.first().value
        for (sample in list) {
            if (sample.timestampSeconds > timeSeconds) {
                break
            }
            lastVal = sample.value
        }
        return lastVal
    }

    /**
     * دریافت بازه زمانی کلی موجود در بافر (کمترین و بیشترین زمان)
     */
    @Synchronized
    fun getTimeRange(): Pair<Float, Float> {
        var minTime = Float.MAX_VALUE
        var maxTime = Float.MIN_VALUE

        signalMap.values.forEach { deque ->
            val first = deque.firstOrNull()
            val last = deque.lastOrNull()
            if (first != null && first.timestampSeconds < minTime) {
                minTime = first.timestampSeconds
            }
            if (last != null && last.timestampSeconds > maxTime) {
                maxTime = last.timestampSeconds
            }
        }

        return if (minTime <= maxTime) Pair(minTime, maxTime) else Pair(0f, 0f)
    }

    /**
     * پاک کردن تمام داده‌ها
     */
    @Synchronized
    fun clear() {
        signalMap.clear()
    }

    /**
     * اندازه نمونه‌های ذخیره‌شده برای یک سیگنال
     */
    @Synchronized
    fun getSampleSize(signalId: String): Int {
        return signalMap[signalId]?.size ?: 0
    }
}
