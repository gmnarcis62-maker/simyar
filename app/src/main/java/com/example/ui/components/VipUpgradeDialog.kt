package com.example.ui.components

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.billing.BillingRepository

const val MYKET_PURCHASE_REQUEST_CODE = 1001

@Composable
fun VipUpgradeDialog(
    billingRepository: BillingRepository,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val dynamicPrice by billingRepository.productPrice.collectAsState()
    val isPriceLoading by billingRepository.isPriceLoading.collectAsState()
    val isPriceError by billingRepository.isPriceError.collectAsState()
    val isBillingProgress by billingRepository.isBillingOperationInProgress.collectAsState()

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFFFD600), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Diamond,
                        contentDescription = "آیکون اشتراک ویژه",
                        tint = Color.Black,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "ارتقا به نسخه حرفه‌ای (VIP)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "دسترسی مادام‌العمر به امکانات مهندسی",
                        color = Color(0xFFFFD600),
                        fontSize = 11.sp
                    )
                }
            }
        },
        text = {
            Column {
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "امکانات اختصاصی اشتراک طلایی سیم‌یار:",
                            color = Color(0xFF00F0FF),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        VipFeatureItem("پروژه‌های نامحدود (پلن رایگان: حداکثر ۳ پروژه)")
                        VipFeatureItem("کتابخانه کامل تمام قطعات صنعتی (تایمرها، بی‌متال، سنسورها، لدر)")
                        VipFeatureItem("دسترسی به تمامی دروس آکادمی برق صنعتی (پلن رایگان: ۵ درس)")
                        VipFeatureItem("خروجی رسمی PDF مهندسی و تصویر PNG با کادر و جدول نقشه")
                        VipFeatureItem("پشتیبانی فنی و دسترسی به الگوهای ستاره-مثلث و چپگرد-راستگرد")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "هزینه اشتراک (درگاه مایکت):",
                            color = Color(0xFF90CAF9),
                            fontSize = 10.sp
                        )
                        if (isPriceLoading) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color(0xFFFFD600))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("در حال دریافت قیمت...", color = Color.White, fontSize = 11.sp)
                            }
                        } else if (isPriceError || dynamicPrice.isEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "دریافت قیمت ممکن نشد",
                                    color = Color(0xFFFF5252),
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                TextButton(
                                    onClick = { billingRepository.refreshPrice() },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                ) {
                                    Text("تلاش دوباره", color = Color(0xFFFFD600), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Text(
                                text = dynamicPrice,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            billingRepository.restorePurchases(context) { unlocked, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                if (unlocked) {
                                    onDismissRequest()
                                }
                            }
                        },
                        enabled = !isBillingProgress,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        if (isBillingProgress) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color(0xFFFFD600))
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "بازیابی خرید قبلی", modifier = Modifier.size(14.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("بازیابی خرید", fontSize = 10.sp)
                    }
                }

                // بخش تست اختصاصی فقط در حالت دیباگ (BuildConfig.DEBUG) جهت جلوگیری از آلودگی نسخه انتشار
                if (BuildConfig.DEBUG) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2E1A1A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.BugReport, contentDescription = null, tint = Color(0xFFFF8A80), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تست دیباگ (BuildConfig.DEBUG)", color = Color(0xFFFF8A80), fontSize = 10.sp)
                            }
                            Button(
                                onClick = {
                                    billingRepository.setVipUnlocked(context, true)
                                    Toast.makeText(context, "حالت VIP آزمایشی در حالت Debug فعال شد", Toast.LENGTH_SHORT).show()
                                    onDismissRequest()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("فعالسازی تست", color = Color.White, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val activity = context as? Activity
                    if (activity != null) {
                        billingRepository.launchPurchaseFlow(
                            activity = activity,
                            requestCode = MYKET_PURCHASE_REQUEST_CODE
                        ) { launched, msg ->
                            if (!launched && msg != null) {
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        }
                    } else {
                        Toast.makeText(context, "خطا در دسترسی به پنجره پرداخت", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = !isBillingProgress,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD600)),
                modifier = Modifier.semantics { contentDescription = "خرید از مایکت و ارتقا به VIP" }
            ) {
                if (isBillingProgress) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("در حال ارتباط با مایکت...", color = Color.Black, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.LockOpen, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("خرید درون‌برنامه‌ای مایکت", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
                modifier = Modifier.semantics { contentDescription = "بستن پنجره ارتقا" }
            ) {
                Text("انصراف")
            }
        },
        containerColor = Color(0xFF1A2434)
    )
}

@Composable
private fun VipFeatureItem(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = Color(0xFF00E676),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            color = Color(0xFFE2E8F0),
            fontSize = 11.sp
        )
    }
}
