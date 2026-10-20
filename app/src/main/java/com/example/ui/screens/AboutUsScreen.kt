package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AboutUsScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isEmailRevealed by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C131F))
            .verticalScroll(rememberScrollState())
    ) {
        // سربرگ درباره ما
        Surface(
            color = Color(0xFF141D2B),
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color(0xFF00E5FF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color.Black
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "درباره اپلیکیشن سیمیار",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Text(
                        text = "نرم‌افزار مهندسی برق صنعتی و اتوماسیون ایران",
                        color = Color(0xFF90A4AE),
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // کارت معرفی و توضیحات برنامه
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF162132)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "سیمیار؛ دستیار هوشمند برقکاران و مهندسان اتوماسیون",
                    color = Color(0xFF00E5FF),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "«سیمیار» یک محیط کارگاهی پیشرفته و قابل حمل بر روی گوشی و تبلت شماست که امکان طراحی، عیب‌یابی و شبیه‌سازی زنده مدارهای فرمان و قدرت الکتروموتورهای سه‌فاز و تک‌فاز را با استانداردهای برق ۳۸۰ ولت و ۵۰ هرتز ایران فراهم می‌کند.\n\nهمچنین با شبیه‌ساز زبان نردبانی PLC (Ladder Logic) می‌توانید قبل از پیاده‌سازی روی تابلوهای واقعی، عملکرد منطقی ورودی‌ها و خروجی‌ها را تست نموده و از اتلاف وقت و آسیب به تجهیزات گران‌قیمت صنعتی جلوگیری کنید.",
                    color = Color(0xFFCFD8DC),
                    fontSize = 13.sp,
                    lineHeight = 22.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // اطلاعات تیم سازنده و مدیریت
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF162132)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "مشخصات تیم توسعه و پشتیبانی",
                    color = Color(0xFFFFD600),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                InfoRow(
                    icon = Icons.Default.Business,
                    label = "توسعه‌دهنده:",
                    value = "تیم نرم‌افزاری ردلاین سافت البرز"
                )

                InfoRow(
                    icon = Icons.Default.Person,
                    label = "مدیریت:",
                    value = "مهندس مهدی رضایی"
                )

                // پشتیبانی با قابلیت مخفی‌سازی ایمیل
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = Color(0xFF80CBC4),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "پشتیبانی:",
                            color = Color(0xFF90A4AE),
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isEmailRevealed) "gmnarcis@gmail.com" else "gmn***@gmail.com",
                            color = Color(0xFFECEFF1),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    IconButton(
                        onClick = { isEmailRevealed = !isEmailRevealed },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isEmailRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (isEmailRevealed) "مخفی‌سازی ایمیل" else "نمایش ایمیل",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // دکمه‌های مایکت
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // دکمه ثبت نظر در مایکت
            Button(
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("myket://comment?id=red.line.bargh")
                            setPackage("ir.mservices.market")
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        try {
                            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://myket.ir/app/red.line.bargh"))
                            context.startActivity(webIntent)
                        } catch (ex: Exception) {
                            Toast.makeText(context, "برنامه مایکت در دستگاه شما یافت نشد", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Star, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ثبت نظر و امتیاز در مایکت",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            // دکمه دیگر برنامه‌های ما در مایکت
            OutlinedButton(
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://myket.ir/developer/dev-36089"))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "امکان باز کردن مرورگر وجود ندارد", Toast.LENGTH_SHORT).show()
                    }
                },
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Apps, contentDescription = null, tint = Color(0xFF00E5FF))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "دیگر برنامه‌های ما در مایکت",
                    color = Color(0xFF00E5FF),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF80CBC4),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            color = Color(0xFF90A4AE),
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = value,
            color = Color(0xFFECEFF1),
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp
        )
    }
}
