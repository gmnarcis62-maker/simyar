package com.example.billing

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.example.model.ComponentType
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class MyketBillingSecurityUnitTest {

    private lateinit var context: Context
    private lateinit var billingRepo: MyketBillingRepository

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        billingRepo = MyketBillingRepository(context)
        billingRepo.setVipUnlocked(context, false)
    }

    // ۱. تست اعتبارسنجی امضای معتبر و نامعتبر
    @Test
    fun testSignatureVerification() {
        val testData = "{\"productId\":\"red.line.bargh_pro\",\"purchaseState\":0}"
        val invalidSignature = "invalid_base64_signature"

        // امضای نامعتبر باید رد شود
        val isValid = Security.verifyPurchase(
            MyketBillingRepository.MYKET_RSA_PUBLIC_KEY,
            testData,
            invalidSignature
        )
        assertFalse("امضای تقلبی یا نامعتبر باید رد شود", isValid)

        // دیتای خالی باید رد شود
        assertFalse(Security.verifyPurchase(MyketBillingRepository.MYKET_RSA_PUBLIC_KEY, "", "sig"))
        assertFalse(Security.verifyPurchase("", testData, "sig"))
        assertFalse(Security.verifyPurchase(MyketBillingRepository.MYKET_RSA_PUBLIC_KEY, testData, ""))
    }

    // ۲. تست عدم تطابق SKU در دیتای خرید
    @Test
    fun testMismatchedSkuRejection() {
        val mismatchedPurchaseData = JSONObject().apply {
            put("productId", "other_unknown_sku")
            put("purchaseState", 0)
            put("packageName", context.packageName)
            put("purchaseToken", "valid_token_123")
        }.toString()

        val dataIntent = Intent().apply {
            putExtra(MyketBillingRepository.RESPONSE_CODE, MyketBillingRepository.BILLING_RESPONSE_RESULT_OK)
            putExtra(MyketBillingRepository.RESPONSE_INAPP_PURCHASE_DATA, mismatchedPurchaseData)
            putExtra(MyketBillingRepository.RESPONSE_INAPP_SIGNATURE, "dummy_sig")
        }

        var resultSuccess = true
        var resultMessage = ""
        val handled = billingRepo.handleActivityResult(
            requestCode = 1001,
            resultCode = Activity.RESULT_OK,
            data = dataIntent,
            expectedRequestCode = 1001
        ) { success, msg ->
            resultSuccess = success
            resultMessage = msg
        }

        assertTrue(handled)
        assertFalse("خرید با SKU غیر از red.line.bargh_pro یا امضای نامعتبر باید رد شود", resultSuccess)
        assertFalse("VIP نباید باز شود", billingRepo.isVipUnlocked.value)
    }

    // ۳. تست عدم تطابق packageName یا خالی بودن purchaseToken (مسئله ۵)
    @Test
    fun testInvalidPackageNameOrEmptyTokenRejection() {
        val foreignPurchaseData = JSONObject().apply {
            put("productId", MyketBillingRepository.SKU_VIP)
            put("purchaseState", 0)
            put("packageName", "com.hacker.fakeapp") // پکیج غیرمجاز
            put("purchaseToken", "") // توکن خالی
        }.toString()

        val dataIntent = Intent().apply {
            putExtra(MyketBillingRepository.RESPONSE_CODE, MyketBillingRepository.BILLING_RESPONSE_RESULT_OK)
            putExtra(MyketBillingRepository.RESPONSE_INAPP_PURCHASE_DATA, foreignPurchaseData)
            putExtra(MyketBillingRepository.RESPONSE_INAPP_SIGNATURE, "dummy_sig")
        }

        var resultSuccess = true
        billingRepo.handleActivityResult(
            requestCode = 1001,
            resultCode = Activity.RESULT_OK,
            data = dataIntent,
            expectedRequestCode = 1001
        ) { success, _ ->
            resultSuccess = success
        }

        assertFalse("خرید با پکیج نامطابق یا توکن خالی باید رد شود", resultSuccess)
        assertFalse(billingRepo.isVipUnlocked.value)
    }

    // ۴. تست پس‌گرفتن VIP (مسئله ۱)
    @Test
    fun testRevokeVipStatus() {
        billingRepo.setVipUnlocked(context, true)
        assertTrue(billingRepo.isVipUnlocked.value)

        billingRepo.setVipUnlocked(context, false)
        assertFalse("VIP باید با موفقیت پس گرفته شود", billingRepo.isVipUnlocked.value)
    }

    // ۵. تست محدودیت‌های پلن رایگان و ارتقا به VIP
    @Test
    fun testFreeTierLimits() {
        billingRepo.setVipUnlocked(context, false)

        // قطعات رایگان
        assertTrue(billingRepo.isComponentTypeFree(ComponentType.AC_PHASE))
        assertTrue(billingRepo.isComponentTypeFree(ComponentType.PUSH_BUTTON_NO))
        assertTrue(billingRepo.isComponentTypeFree(ComponentType.MOTOR_3PHASE))

        // قطعات پولی در حالت رایگان
        assertFalse(billingRepo.isComponentTypeFree(ComponentType.TIMER_ON_DELAY_COIL))
        assertFalse(billingRepo.isComponentTypeFree(ComponentType.PROXIMITY_SENSOR))
        assertFalse(billingRepo.isComponentTypeFree(ComponentType.BIMETAL_OVERLOAD))

        // محدودیت پروژه (حداکثر ۳ پروژه رایگان)
        assertTrue(billingRepo.canCreateNewProject(0))
        assertTrue(billingRepo.canCreateNewProject(2))
        assertFalse("در ۳ پروژه نباید اجازه ساخت پروژه چهارم داده شود", billingRepo.canCreateNewProject(3))

        // محدودیت درس‌ها (۵ درس اول رایگان)
        assertTrue(billingRepo.isLessonFree(0))
        assertTrue(billingRepo.isLessonFree(4))
        assertFalse("درس ۶ پولی است", billingRepo.isLessonFree(5))

        // آنلاک VIP
        billingRepo.setVipUnlocked(context, true)
        assertTrue(billingRepo.isComponentTypeFree(ComponentType.TIMER_ON_DELAY_COIL))
        assertTrue(billingRepo.isComponentTypeFree(ComponentType.PROXIMITY_SENSOR))
        assertTrue(billingRepo.isComponentTypeFree(ComponentType.BIMETAL_OVERLOAD))
        assertTrue(billingRepo.canCreateNewProject(10))
        assertTrue(billingRepo.isLessonFree(10))
    }

    // ۶. تست تابع خالص decideVipState (راهکار A2)
    @Test
    fun testDecideVipStatePureFunction() {
        // حالت ۱: کاربر قبلاً VIP بوده و استعلام ناموفق است (مثلاً خطای شبکه) -> کش نباید تغییر کند
        assertTrue(
            "در خطای شبکه وضعیت VIP قبلی باید حفظ شود",
            MyketBillingRepository.decideVipState(
                currentVipState = true,
                isQueryResultSuccessful = false,
                hasValidReceipt = false
            )
        )

        // حالت ۲: کاربر قبلاً رایگان بوده و استعلام ناموفق است -> وضعیت رایگان می‌ماند
        assertFalse(
            MyketBillingRepository.decideVipState(
                currentVipState = false,
                isQueryResultSuccessful = false,
                hasValidReceipt = false
            )
        )

        // حالت ۳: استعلام موفق و رسید معتبر موجود است -> VIP فعال می‌شود
        assertTrue(
            MyketBillingRepository.decideVipState(
                currentVipState = false,
                isQueryResultSuccessful = true,
                hasValidReceipt = true
            )
        )

        // حالت ۴: استعلام موفق ولی هیچ رسیدی وجود ندارد -> VIP پس گرفته می‌شود
        assertFalse(
            "هنگامی که استعلام موفق بوده اما رسیدی نیست باید VIP پس گرفته شود",
            MyketBillingRepository.decideVipState(
                currentVipState = true,
                isQueryResultSuccessful = true,
                hasValidReceipt = false
            )
        )
    }
}
