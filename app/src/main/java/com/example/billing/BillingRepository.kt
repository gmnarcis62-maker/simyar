package com.example.billing

import android.app.Activity
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import com.android.vending.billing.IInAppBillingService
import com.example.model.ComponentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.util.UUID

interface BillingRepository {
    val isVipUnlocked: StateFlow<Boolean>
    val productPrice: StateFlow<String>
    val isPriceLoading: StateFlow<Boolean>
    val isPriceError: StateFlow<Boolean>
    val isServiceConnected: StateFlow<Boolean>
    val isBillingOperationInProgress: StateFlow<Boolean>

    fun checkVipStatusOnAppStart(context: Context)
    fun refreshPurchasesFromMyket(context: Context)
    fun refreshPrice()
    fun bindBillingService(context: Context)
    fun unbindBillingService(context: Context)
    fun launchPurchaseFlow(
        activity: Activity,
        requestCode: Int,
        onLaunched: (Boolean, String?) -> Unit
    )
    fun handleActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        expectedRequestCode: Int,
        onResult: (Boolean, String) -> Unit
    ): Boolean
    fun restorePurchases(context: Context, onResult: (Boolean, String) -> Unit)
    fun setVipUnlocked(context: Context, unlocked: Boolean)

    // محدودیت‌های پلن رایگان
    fun isComponentTypeFree(type: ComponentType): Boolean
    fun isLessonFree(lessonIndex: Int): Boolean
    fun canCreateNewProject(currentCount: Int): Boolean
}

/**
 * پیاده‌سازی امن و رسمی خرید درون‌برنامه‌ای مایکت (Myket In-App Billing)
 * SKU: red.line.bargh_pro
 * کلید RSA عمومی مایکت برای اعتبارسنجی آفلاین امضای خرید
 */
class MyketBillingRepository(context: Context? = null) : BillingRepository {

    companion object {
        private const val TAG = "MyketBilling"

        const val SKU_VIP = "red.line.bargh_pro"
        const val ITEM_TYPE_INAPP = "inapp"
        const val BILLING_API_VERSION = 3
        const val MYKET_PACKAGE = "ir.mservices.market"
        const val MYKET_BIND_ACTION = "ir.mservices.market.InAppBillingService.BIND"

        // کلید RSA عمومی مایکت
        const val MYKET_RSA_PUBLIC_KEY =
            "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCIz0z2eLRrvFgqSenhbmCGvqWr/lGKlF5Fp4ARDwryAxLmiWwKNb3QCxmbGVCRR6S4vNfUZNlHv6IdO787d239gQiAhx/EIAfjIglsJSlrj7PKfxsHajUZzMaxYLVyh1PIIyt6VOG9HSgle1Ty1zHyAmof2FSg8xs8x4HxkgEePQIDAQAB"

        // کدهای پاسخ استاندارد IAB
        const val BILLING_RESPONSE_RESULT_OK = 0
        const val BILLING_RESPONSE_RESULT_USER_CANCELED = 1
        const val BILLING_RESPONSE_RESULT_SERVICE_UNAVAILABLE = 2
        const val BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE = 3
        const val BILLING_RESPONSE_RESULT_ITEM_UNAVAILABLE = 4
        const val BILLING_RESPONSE_RESULT_DEVELOPER_ERROR = 5
        const val BILLING_RESPONSE_RESULT_ERROR = 6
        const val BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED = 7
        const val BILLING_RESPONSE_RESULT_ITEM_NOT_OWNED = 8

        const val RESPONSE_CODE = "RESPONSE_CODE"
        const val RESPONSE_GET_SKU_DETAILS_LIST = "DETAILS_LIST"
        const val RESPONSE_BUY_INTENT = "BUY_INTENT"
        const val RESPONSE_INAPP_PURCHASE_DATA = "INAPP_PURCHASE_DATA"
        const val RESPONSE_INAPP_SIGNATURE = "INAPP_DATA_SIGNATURE"
        const val RESPONSE_INAPP_ITEM_LIST = "INAPP_PURCHASE_ITEM_LIST"
        const val RESPONSE_INAPP_PURCHASE_DATA_LIST = "INAPP_PURCHASE_DATA_LIST"
        const val RESPONSE_INAPP_SIGNATURE_LIST = "INAPP_DATA_SIGNATURE_LIST"

        /**
         * تابع خالص برای تصمیم‌گیری وضعیت VIP:
         * VIP فقط زمانی پس گرفته می‌شود (false) که:
         * ۱. استعلام با موفقیت انجام شده باشد (isQueryResultSuccessful == true)
         * ۲. حداقل ۱ خرید موفق و قطعی قبلی وجود نداشته باشد یا تأیید شود که لغو شده است (hasValidReceipt == false)
         * در صورت بروز خطا در استعلام، کش قبلی حفظ می‌شود.
         */
        fun decideVipState(
            currentVipState: Boolean,
            isQueryResultSuccessful: Boolean,
            hasValidReceipt: Boolean
        ): Boolean {
            return when {
                !isQueryResultSuccessful -> currentVipState
                hasValidReceipt -> true
                else -> false
            }
        }
    }

    // اسکوپ متمرکز Coroutine برای ریپازیتوری با SupervisorJob (مسئله ۹)
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isVipUnlocked = MutableStateFlow(false)
    override val isVipUnlocked: StateFlow<Boolean> = _isVipUnlocked.asStateFlow()

    private val _productPrice = MutableStateFlow("")
    override val productPrice: StateFlow<String> = _productPrice.asStateFlow()

    private val _isPriceLoading = MutableStateFlow(false)
    override val isPriceLoading: StateFlow<Boolean> = _isPriceLoading.asStateFlow()

    private val _isPriceError = MutableStateFlow(false)
    override val isPriceError: StateFlow<Boolean> = _isPriceError.asStateFlow()

    private val _isServiceConnected = MutableStateFlow(false)
    override val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

    private val _isBillingOperationInProgress = MutableStateFlow(false)
    override val isBillingOperationInProgress: StateFlow<Boolean> = _isBillingOperationInProgress.asStateFlow()

    private var billingService: IInAppBillingService? = null
    private var isBound = false
    private var lastDeveloperPayload: String = ""
    private var appContext: Context? = context?.applicationContext

    private val PREF_NAME = "simyar_billing_prefs"
    private val KEY_IS_VIP = "key_is_vip_unlocked"

    // ۱۰ قطعه اساسی و رایگان برق صنعتی
    private val freeBaseComponents = setOf(
        ComponentType.AC_PHASE,
        ComponentType.AC_NEUTRAL,
        ComponentType.CIRCUIT_BREAKER_MCB,
        ComponentType.EMERGENCY_STOP,
        ComponentType.PUSH_BUTTON_NO,
        ComponentType.PUSH_BUTTON_NC,
        ComponentType.CONTACTOR_COIL,
        ComponentType.CONTACTOR_NO_AUX,
        ComponentType.PILOT_LAMP_GREEN,
        ComponentType.MOTOR_3PHASE
    )

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "اتصال با سرویس خرید درون‌برنامه‌ای مایکت برقرار شد.")
            billingService = IInAppBillingService.Stub.asInterface(service)
            _isServiceConnected.value = true

            repositoryScope.launch {
                querySkuDetails()
                appContext?.let { queryOwnedPurchases(it) }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "اتصال با سرویس مایکت قطع شد.")
            billingService = null
            _isServiceConnected.value = false
        }
    }

    init {
        context?.let {
            appContext = it.applicationContext
            checkVipStatusOnAppStart(it)
            bindBillingService(it)
        }
    }

    override fun checkVipStatusOnAppStart(context: Context) {
        appContext = context.applicationContext
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val savedVip = prefs.getBoolean(KEY_IS_VIP, false)
        _isVipUnlocked.value = savedVip

        if (billingService != null) {
            repositoryScope.launch {
                queryOwnedPurchases(context)
            }
        } else {
            bindBillingService(context)
        }
    }

    override fun refreshPurchasesFromMyket(context: Context) {
        appContext = context.applicationContext
        if (billingService != null) {
            repositoryScope.launch {
                queryOwnedPurchases(context)
            }
        } else {
            bindBillingService(context)
        }
    }

    override fun refreshPrice() {
        if (billingService != null) {
            repositoryScope.launch {
                querySkuDetails()
            }
        } else {
            appContext?.let { bindBillingService(it) }
        }
    }

    override fun bindBillingService(context: Context) {
        appContext = context.applicationContext
        if (isBound && billingService != null) return
        try {
            val serviceIntent = Intent(MYKET_BIND_ACTION).apply {
                setPackage(MYKET_PACKAGE)
            }
            val intentServices = context.packageManager.queryIntentServices(serviceIntent, 0)
            if (intentServices.isNotEmpty()) {
                isBound = context.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
                Log.d(TAG, "درخواست bindService مایکت ارسال شد. نتیجه: $isBound")
            } else {
                Log.w(TAG, "برنامه مایکت روی دستگاه نصب نیست یا قابل مشاهده نیست.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "خطا در اتصال به سرویس مایکت: ${e.message}", e)
        }
    }

    override fun unbindBillingService(context: Context) {
        if (isBound) {
            try {
                context.unbindService(serviceConnection)
            } catch (e: Exception) {
                Log.e(TAG, "خطا در قطع اتصال سرویس مایکت: ${e.message}")
            }
            isBound = false
            billingService = null
            _isServiceConnected.value = false
        }
    }

    /**
     * انتظار هوشمند برای اتصال به سرویس تا سقف ۵ ثانیه (مسئله ۷)
     */
    private suspend fun awaitServiceConnection(timeoutMs: Long = 5000L): IInAppBillingService? {
        val currentService = billingService
        if (currentService != null) return currentService

        appContext?.let { bindBillingService(it) }

        return withTimeoutOrNull(timeoutMs) {
            _isServiceConnected.filter { it && billingService != null }.first()
            billingService
        }
    }

    private suspend fun querySkuDetails() {
        val service = billingService
        if (service == null) {
            _isPriceError.value = true
            _isPriceLoading.value = false
            return
        }
        try {
            _isPriceLoading.value = true
            _isPriceError.value = false
            val skuList = ArrayList<String>().apply { add(SKU_VIP) }
            val queryBundle = Bundle().apply {
                putStringArrayList("ITEM_ID_LIST", skuList)
            }

            val packageName = appContext?.packageName ?: "red.line.bargh"
            val skuDetails = withContext(Dispatchers.IO) {
                service.getSkuDetails(BILLING_API_VERSION, packageName, ITEM_TYPE_INAPP, queryBundle)
            }

            val response = skuDetails.getInt(RESPONSE_CODE)
            if (response == BILLING_RESPONSE_RESULT_OK) {
                val responseList = skuDetails.getStringArrayList(RESPONSE_GET_SKU_DETAILS_LIST)
                if (!responseList.isNullOrEmpty()) {
                    val firstItem = JSONObject(responseList[0])
                    val price = firstItem.optString("price", "")
                    if (price.isNotEmpty()) {
                        _productPrice.value = price
                        _isPriceError.value = false
                        Log.d(TAG, "قیمت استعلام شده از مایکت: $price")
                    } else {
                        _isPriceError.value = true
                    }
                } else {
                    _isPriceError.value = true
                }
            } else {
                _isPriceError.value = true
                Log.w(TAG, "خطا در دریافت قیمت از مایکت: کد $response")
            }
        } catch (e: Exception) {
            _isPriceError.value = true
            Log.e(TAG, "خطا در استعلام قیمت مایکت: ${e.message}")
        } finally {
            _isPriceLoading.value = false
        }
    }

    /**
     * استعلام خریدها از مایکت و اعتبارسنجی امضا
     * مسئله ۱: اگر پاسخ RESPONSE_CODE=0 باشد و رسید معتبری نباشد، VIP پس گرفته می‌شود (setVipUnlocked(false)).
     * در خطای شبکه یا عدم پاسخ سرویس، کش دست‌نخورده باقی می‌ماند.
     * مسئله ۴: در صورت خطای پارس JSON، خرید رد می‌شود.
     * مسئله ۵: تطابق packageName با applicationId و غیرخالی بودن purchaseToken بررسی می‌شود.
     */
    private suspend fun queryOwnedPurchases(context: Context): Boolean {
        val service = billingService ?: return false
        val expectedPackageName = context.packageName
        try {
            var continuationToken: String? = null
            var hasVip = false
            var isQueryResultSuccessful = false

            do {
                val ownedItems = withContext(Dispatchers.IO) {
                    service.getPurchases(
                        BILLING_API_VERSION,
                        expectedPackageName,
                        ITEM_TYPE_INAPP,
                        continuationToken
                    )
                }

                val response = ownedItems.getInt(RESPONSE_CODE)
                if (response == BILLING_RESPONSE_RESULT_OK) {
                    isQueryResultSuccessful = true
                    val ownedSkus = ownedItems.getStringArrayList(RESPONSE_INAPP_ITEM_LIST)
                    val purchaseDataList = ownedItems.getStringArrayList(RESPONSE_INAPP_PURCHASE_DATA_LIST)
                    val signatureList = ownedItems.getStringArrayList(RESPONSE_INAPP_SIGNATURE_LIST)

                    if (!ownedSkus.isNullOrEmpty() && !purchaseDataList.isNullOrEmpty() && !signatureList.isNullOrEmpty()) {
                        for (i in ownedSkus.indices) {
                            val sku = ownedSkus.getOrNull(i) ?: ""
                            val pData = purchaseDataList.getOrNull(i) ?: ""
                            val sign = signatureList.getOrNull(i) ?: ""

                            if (sku == SKU_VIP && Security.verifyPurchase(MYKET_RSA_PUBLIC_KEY, pData, sign)) {
                                try {
                                    val jo = JSONObject(pData)
                                    val purchaseState = jo.optInt("purchaseState", -1)
                                    val pkgName = jo.optString("packageName", "")
                                    val token = jo.optString("purchaseToken", "")

                                    // مسئله ۴ و ۵: اعتبارسنجی دقیق پکیج، وضعیت خرید و توکن
                                    if (purchaseState == 0 &&
                                        pkgName == expectedPackageName &&
                                        token.isNotBlank()
                                    ) {
                                        hasVip = true
                                        break
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "خطا در پارس رسید خرید: ${e.message}")
                                    // رد خرید در صورت بروز خطای پارس (مسئله ۴)
                                }
                            }
                        }
                    }
                    continuationToken = ownedItems.getString("INAPP_CONTINUATION_TOKEN")
                } else {
                    break
                }
            } while (!hasVip && !continuationToken.isNullOrEmpty())

            withContext(Dispatchers.Main) {
                val newVipState = decideVipState(
                    currentVipState = _isVipUnlocked.value,
                    isQueryResultSuccessful = isQueryResultSuccessful,
                    hasValidReceipt = hasVip
                )
                if (isQueryResultSuccessful) {
                    setVipUnlocked(context, newVipState)
                }
            }
            return hasVip
        } catch (e: Exception) {
            Log.e(TAG, "خطا در بررسی وضعیت خریدها: ${e.message}")
            // در خطای شبکه/استثنا، کش دست‌نخورده می‌ماند
            return false
        }
    }

    override fun launchPurchaseFlow(
        activity: Activity,
        requestCode: Int,
        onLaunched: (Boolean, String?) -> Unit
    ) {
        _isBillingOperationInProgress.value = true

        repositoryScope.launch {
            try {
                // مسئله ۷: صبر هوشمند تا ۵ ثانیه برای اتصال سرویس
                val service = awaitServiceConnection(5000L)
                if (service == null) {
                    withContext(Dispatchers.Main) {
                        _isBillingOperationInProgress.value = false
                        onLaunched(false, "ارتباط با برنامه مایکت برقرار نشد. لطفاً از نصب و باز بودن مایکت مطمئن شوید.")
                    }
                    return@launch
                }

                lastDeveloperPayload = UUID.randomUUID().toString()
                val buyIntentBundle = withContext(Dispatchers.IO) {
                    service.getBuyIntent(
                        BILLING_API_VERSION,
                        activity.packageName,
                        SKU_VIP,
                        ITEM_TYPE_INAPP,
                        lastDeveloperPayload
                    )
                }

                val responseCode = buyIntentBundle.getInt(RESPONSE_CODE)
                if (responseCode == BILLING_RESPONSE_RESULT_OK) {
                    // مسئله ۸: استخراج ایمن PendingIntent با شرط SDK ۳۳+
                    val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        buyIntentBundle.getParcelable(RESPONSE_BUY_INTENT, PendingIntent::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        buyIntentBundle.getParcelable<PendingIntent>(RESPONSE_BUY_INTENT)
                    }

                    if (pendingIntent != null) {
                        withContext(Dispatchers.Main) {
                            _isBillingOperationInProgress.value = false
                            activity.startIntentSenderForResult(
                                pendingIntent.intentSender,
                                requestCode,
                                Intent(),
                                0,
                                0,
                                0
                            )
                            onLaunched(true, null)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            _isBillingOperationInProgress.value = false
                            onLaunched(false, "اطلاعات پرداخت از مایکت دریافت نشد.")
                        }
                    }
                } else if (responseCode == BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED) {
                    // مسئله ۲: به جای باز کردن مستقیم، استعلام واقعی و تأیید امضا
                    val verified = queryOwnedPurchases(activity)
                    withContext(Dispatchers.Main) {
                        _isBillingOperationInProgress.value = false
                        if (verified) {
                            onLaunched(true, "این محصول قبلاً خریداری شده است. اشتراک VIP فعال شد.")
                        } else {
                            onLaunched(false, "محصول در مایکت خریداری شده، اما رسید معتبری تایید نگردید.")
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        _isBillingOperationInProgress.value = false
                        val message = when (responseCode) {
                            BILLING_RESPONSE_RESULT_USER_CANCELED -> "خرید لغو شد."
                            BILLING_RESPONSE_RESULT_SERVICE_UNAVAILABLE -> "سرویس مایکت در دسترس نیست یا اتصال اینترنت قطع است."
                            BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE -> "پرداخت در مایکت در دسترس نیست. لطفاً وارد حساب مایکت شوید."
                            BILLING_RESPONSE_RESULT_ITEM_UNAVAILABLE -> "محصول در مایکت تعریف نشده یا یافت نشد."
                            BILLING_RESPONSE_RESULT_DEVELOPER_ERROR -> "خطای توسعه‌دهنده در درخواست خرید."
                            BILLING_RESPONSE_RESULT_ERROR -> "خطا در انجام تراکنش در مایکت."
                            else -> "خطا در ارتباط با مایکت (کد: $responseCode)"
                        }
                        onLaunched(false, message)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _isBillingOperationInProgress.value = false
                    onLaunched(false, "خطا در شروع فرایند خرید: ${e.message}")
                }
            }
        }
    }

    override fun handleActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        expectedRequestCode: Int,
        onResult: (Boolean, String) -> Unit
    ): Boolean {
        if (requestCode != expectedRequestCode) return false

        if (resultCode != Activity.RESULT_OK || data == null) {
            onResult(false, "عملیات خرید توسط کاربر لغو شد.")
            return true
        }

        val responseCode = data.getIntExtra(RESPONSE_CODE, BILLING_RESPONSE_RESULT_OK)
        val purchaseData = data.getStringExtra(RESPONSE_INAPP_PURCHASE_DATA)
        val dataSignature = data.getStringExtra(RESPONSE_INAPP_SIGNATURE)

        if (responseCode == BILLING_RESPONSE_RESULT_OK && purchaseData != null && dataSignature != null) {
            val isSignatureValid = Security.verifyPurchase(MYKET_RSA_PUBLIC_KEY, purchaseData, dataSignature)
            if (isSignatureValid) {
                try {
                    val jo = JSONObject(purchaseData)
                    val sku = jo.getString("productId")
                    val purchaseState = jo.optInt("purchaseState", -1) // 0 = PURCHASED
                    val payload = jo.optString("developerPayload", "")
                    val pkgName = jo.optString("packageName", "")
                    val purchaseToken = jo.optString("purchaseToken", "")

                    if (sku != SKU_VIP) {
                        onResult(false, "شناسه محصول خریداری شده نامطابق است.")
                        return true
                    }

                    if (purchaseState != 0) {
                        onResult(false, "وضعیت خرید نامعتبر است (غیر از خریداری شده).")
                        return true
                    }

                    // مسئله ۵: تطابق packageName با applicationId و غیرخالی بودن purchaseToken
                    val expectedPackageName = appContext?.packageName ?: "red.line.bargh"
                    if (pkgName != expectedPackageName || purchaseToken.isBlank()) {
                        onResult(false, "رسید خرید متعلق به این برنامه نیست یا فاقد توکن معتبر است.")
                        return true
                    }

                    // مسئله ۳: رد خرید در صورت عدم تطابق developerPayload
                    if (lastDeveloperPayload.isNotEmpty() && payload.isNotEmpty() && payload != lastDeveloperPayload) {
                        Log.e(TAG, "عدم تطابق developerPayload: ارسال شده=$lastDeveloperPayload، دریافتی=$payload")
                        onResult(false, "اعتبارسنجی امنیتی خرید به دلیل عدم تطابق نشانه تراکنش رد شد.")
                        return true
                    }

                    appContext?.let { ctx ->
                        setVipUnlocked(ctx, true)
                        // استعلام نهایی جهت ثبت در سوابق
                        repositoryScope.launch {
                            queryOwnedPurchases(ctx)
                        }
                    }
                    onResult(true, "خرید با موفقیت انجام شد! اشتراک VIP فعال گردید.")
                    return true
                } catch (e: Exception) {
                    onResult(false, "خطا در تحلیل دیتای خرید مایکت.")
                    return true
                }
            } else {
                onResult(false, "امضای دیجیتال خرید نامعتبر است! تأیید امنیتی مایکت با شکست مواجه شد.")
                return true
            }
        } else if (responseCode == BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED) {
            // مسئله ۲: در حالت ITEM_ALREADY_OWNED نیز اعتبارسنجی واقعی امضا
            appContext?.let { ctx ->
                repositoryScope.launch {
                    val verified = queryOwnedPurchases(ctx)
                    withContext(Dispatchers.Main) {
                        if (verified) {
                            onResult(true, "این محصول قبلاً خریداری شده است. اشتراک VIP فعال شد.")
                        } else {
                            onResult(false, "محصول قبلاً ثبت شده، اما رسید معتبر یافت نشد.")
                        }
                    }
                }
            } ?: run {
                onResult(false, "خطا در دسترسی به محیط برنامه.")
            }
            return true
        } else {
            val message = when (responseCode) {
                BILLING_RESPONSE_RESULT_USER_CANCELED -> "خرید لغو گردید."
                BILLING_RESPONSE_RESULT_SERVICE_UNAVAILABLE -> "سرویس مایکت موقتاً در دسترس نیست یا اینترنت متصل نیست."
                BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE -> "امکان پرداخت مایکت در دسترس نیست. لطفاً وارد حساب مایکت شوید."
                BILLING_RESPONSE_RESULT_ITEM_UNAVAILABLE -> "محصول یافت نشد."
                else -> "تراکنش ناموفق بود (کد: $responseCode)"
            }
            onResult(false, message)
            return true
        }
    }

    override fun restorePurchases(context: Context, onResult: (Boolean, String) -> Unit) {
        _isBillingOperationInProgress.value = true

        repositoryScope.launch {
            try {
                // مسئله ۷: صبر تا ۵ ثانیه برای اتصال سرویس مایکت
                val service = awaitServiceConnection(5000L)
                if (service == null) {
                    withContext(Dispatchers.Main) {
                        _isBillingOperationInProgress.value = false
                        onResult(false, "سرویس مایکت در دسترس نیست. لطفاً از اتصال اینترنت و نصب مایکت مطمئن شوید.")
                    }
                    return@launch
                }

                val hasVip = queryOwnedPurchases(context)

                withContext(Dispatchers.Main) {
                    _isBillingOperationInProgress.value = false
                    if (hasVip) {
                        onResult(true, "خرید پیشین شما از مایکت با موفقیت بازیابی شد و اشتراک طلایی فعال گردید.")
                    } else {
                        onResult(false, "هیچ خرید معتبری برای این حساب در مایکت یافت نشد.")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _isBillingOperationInProgress.value = false
                    onResult(false, "خطا در استعلام خریدها از مایکت: ${e.message}")
                }
            }
        }
    }

    override fun setVipUnlocked(context: Context, unlocked: Boolean) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_IS_VIP, unlocked).apply()
        _isVipUnlocked.value = unlocked
    }

    override fun isComponentTypeFree(type: ComponentType): Boolean {
        if (_isVipUnlocked.value) return true
        return freeBaseComponents.contains(type)
    }

    override fun isLessonFree(lessonIndex: Int): Boolean {
        if (_isVipUnlocked.value) return true
        return lessonIndex < 5 // ۵ درس اول کاملا رایگان
    }

    override fun canCreateNewProject(currentCount: Int): Boolean {
        if (_isVipUnlocked.value) return true
        return currentCount < 3 // حداکثر ۳ پروژه در نسخه رایگان
    }
}
