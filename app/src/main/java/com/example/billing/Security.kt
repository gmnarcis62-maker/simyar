package com.example.billing

import android.util.Base64
import android.util.Log
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

/**
 * اعتبارسنجی امن امضای دیجیتال خریدهای درون‌برنامه‌ای مایکت با الگوریتم SHA1withRSA
 */
object Security {
    private const val TAG = "IABSecurity"
    private const val KEY_FACTORY_ALGORITHM = "RSA"
    private const val SIGNATURE_ALGORITHM = "SHA1withRSA"

    /**
     * بررسی امضای تراکنش خرید مایکت بر اساس کلید عمومی Base64
     */
    fun verifyPurchase(base64PublicKey: String, signedData: String, signature: String): Boolean {
        if (signedData.isEmpty() || base64PublicKey.isEmpty() || signature.isEmpty()) {
            Log.e(TAG, "اطلاعات بررسی امضا ناقص است.")
            return false
        }
        val key = generatePublicKey(base64PublicKey) ?: return false
        return verify(key, signedData, signature)
    }

    private fun generatePublicKey(encodedPublicKey: String): PublicKey? {
        return try {
            val decodedKey = Base64.decode(encodedPublicKey, Base64.DEFAULT)
            val keyFactory = KeyFactory.getInstance(KEY_FACTORY_ALGORITHM)
            keyFactory.generatePublic(X509EncodedKeySpec(decodedKey))
        } catch (e: Exception) {
            Log.e(TAG, "خطا در تولید کلید عمومی مایکت: ${e.message}")
            null
        }
    }

    private fun verify(publicKey: PublicKey, signedData: String, signature: String): Boolean {
        return try {
            val sig = Signature.getInstance(SIGNATURE_ALGORITHM)
            sig.initVerify(publicKey)
            sig.update(signedData.toByteArray(Charsets.UTF_8))
            if (!sig.verify(Base64.decode(signature, Base64.DEFAULT))) {
                Log.e(TAG, "امضای خرید نامعتبر است!")
                false
            } else {
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "خطا در اعتبارسنجی امضا: ${e.message}")
            false
        }
    }
}
