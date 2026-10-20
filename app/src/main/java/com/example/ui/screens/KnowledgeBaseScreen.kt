package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.model.ComponentType
import com.example.model.IndustrialComponentLibrary
import com.example.model.SpecSource
import com.example.model.SpecSourceType
import com.example.model.TechnicalSpecs
import com.example.util.TechnicalReportExporter

data class IndustrialComponentInfo(
    val title: String,
    val standardCode: String,
    val terminalLabels: String,
    val description: String,
    val function: String,
    val specs: TechnicalSpecs? = null
)

@Composable
fun KnowledgeBaseScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val contactorSpecs = IndustrialComponentLibrary.items.find { it.type == ComponentType.CONTACTOR_COIL }?.defaultSpecs
    val bimetalSpecs = IndustrialComponentLibrary.items.find { it.type == ComponentType.BIMETAL_OVERLOAD }?.defaultSpecs
    val buttonSpecs = IndustrialComponentLibrary.items.find { it.type == ComponentType.PUSH_BUTTON_NO }?.defaultSpecs
    val estopSpecs = IndustrialComponentLibrary.items.find { it.type == ComponentType.EMERGENCY_STOP }?.defaultSpecs
    val timerSpecs = IndustrialComponentLibrary.items.find { it.type == ComponentType.TIMER_ON_DELAY_COIL }?.defaultSpecs

    val items = listOf(
        IndustrialComponentInfo(
            title = "کنتاکتور صنعتی (Contactor)",
            standardCode = "IEC 60947-4-1 | نماد: KM",
            terminalLabels = "بوبین: A1 - A2 | قدرت: 1/L1-2/T1, 3/L2-4/T2, 5/L3-6/T3 | کمکی: 13-14 (NO), 21-22 (NC)",
            description = "کلید الکترومغناطیسی با قابلیت قطع و وصل بارهای پرقدرت مانند الکتروموتورها از راه دور.",
            function = "با اعمال ولتاژ به بوبین (A1-A2)، هسته مغناطیسی جذب شده و کنتاکت‌های قدرت و کمکی همزمان بسته یا باز می‌شوند.",
            specs = contactorSpecs
        ),
        IndustrialComponentInfo(
            title = "رله حرارتی بی‌متال (Overload Relay)",
            standardCode = "IEC 60947-4-1 | نماد: FR (یا F)",
            terminalLabels = "ورودی قدرت: 1/L1, 3/L2, 5/L3 | خروجی قدرت: 2/T1, 4/T2, 6/T3 | فرمان: 95-96 (NC), 97-98 (NO)",
            description = "حفاظت الکتروموتور در برابر اضافه بار ناشی از قفل کردن مکانیکی یا کشیدن جریان بیش از حد نامی.",
            function = "کنتاکت 95-96 در مسیر تغذیه بوبین کنتاکتور قرار می‌گیرد تا هنگام داغ شدن نوار دوفلزی، مدار فرمان را قطع کند و کنتاکت 97-98 مدار آلارم و سیگنال خطا را وصل می‌نماید.",
            specs = bimetalSpecs
        ),
        IndustrialComponentInfo(
            title = "شستی‌های استپ و استارت (Push Buttons)",
            standardCode = "IEC 60947-5-1 | نماد: SB",
            terminalLabels = "استارت (سبز): 13-14 (NO) | استپ (قرمز): 21-22 (NC)",
            description = "تجهیزات ارسال پالس فرمان دستی توسط کاربر جهت راه‌اندازی یا توقف ماشین‌آلات صنعتی.",
            function = "شستی استارت با فشار دادن وصل و با رها کردن باز می‌شود؛ به همین دلیل به تیغه خودنگهدار کنتاکتور (13-14) نیاز دارد.",
            specs = buttonSpecs
        ),
        IndustrialComponentInfo(
            title = "شستی قارچی اضطراری (E-Stop)",
            standardCode = "ISO 13850 / IEC 60947-5-5 | نماد: S0 (یا SB0)",
            terminalLabels = "پایانه‌ها: 1-2 (NC با عملکرد باز شدن مستقیم)",
            description = "قطع فوری و ایمن کلیه بارهای متحرک ماشین هنگام بروز خطر برای اپراتور یا تجهیزات.",
            function = "با ضربه زدن به کله‌قارچی قرمز رنگ، کنتاکت در حالت قطع قفل شده و مدار را قطع نگه می‌دارد. ریست مکانیکی با چرخاندن (Turn to Reset)، کشیدن (Pull to Reset) یا کلید اختصاصی (Key Release) امکان‌پذیر است.",
            specs = estopSpecs
        ),
        IndustrialComponentInfo(
            title = "تایمر الکترونیکی تاخیر در وصل (On-Delay Timer TON)",
            standardCode = "IEC 61812-1 | نماد: KT",
            terminalLabels = "تغذیه: A1 - A2 | تیغه مشترک: 15 | تیغه باز: 15-18 (NO) | تیغه بسته: 15-16 (NC)",
            description = "عملکرد تأخیر در وصل: پس از برق‌دار شدن بوبین (A1-A2)، زمان تنظیم‌شده سپری می‌شود و سپس تیغه‌های خروجی تغییر وضعیت می‌دهند.",
            function = "کاربرد ویژه در مدارهای راه‌اندازی ستاره-مثلث برای تغییر اتصال موتور از ستاره به مثلث پس از سپری شدن زمان شتاب‌گیری موتور.",
            specs = timerSpecs
        ),
        IndustrialComponentInfo(
            title = "شبیه‌ساز منطق PLC نردبانی (Ladder Logic)",
            standardCode = "IEC 61131-3",
            terminalLabels = "نمادگذاری رایج (زیمنس): I0.0 تا I0.7 و Q0.0 تا Q0.7 | معادل استاندارد IEC: %IX0.0 و %QX0.0 | حافظه: M0.0 (%MX0.0)",
            description = "زبان برنامه‌نویسی گرافیکی استاندارد PLC بر پایه منطق دیاگرام‌های رله‌ای برق صنعتی.",
            function = "اجرای حلقه‌های پیوسته Scan Cycle شامل خواندن ورودی‌ها، پردازش رانگ‌های لدر، و فعال‌سازی خروجی‌های دیجیتال.",
            specs = null
        )
    )

    Column(modifier = modifier.fillMaxSize().background(Color(0xFF0D1420))) {
        Surface(
            color = Color(0xFF141D2B),
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "دانشنامه قطعات برق صنعتی و اتوماسیون",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "استاندارد IEC، شماره‌گذاری ترمینال‌ها و اصول عملکرد مدارهای قدرت و فرمان",
                    color = Color(0xFF90A4AE),
                    fontSize = 12.sp
                )

                // دکمه خروجی گزارش بازبینی فنی فقط در حالت DEBUG (مرحله P12)
                if (BuildConfig.DEBUG) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            TechnicalReportExporter.exportAndShareReport(context)
                            Toast.makeText(context, "گزارش بازبینی فنی آماده اشتراک‌گذاری شد", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF00E5FF)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E5FF))
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("خروجی گزارش بازبینی فنی (Debug)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(items) { item ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF172233)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.title,
                                color = Color(0xFF00E5FF),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = item.standardCode,
                                color = Color(0xFFFFD600),
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "پایانه‌ها: ${item.terminalLabels}",
                            color = Color(0xFF80CBC4),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = item.description,
                            color = Color(0xFFECEFF1),
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "عملکرد: ${item.function}",
                            color = Color(0xFFB0BEC5),
                            fontSize = 11.sp
                        )

                        // نمایش منبع مشخصات فنی قطعه با برچسب و آیکون (مرحله P12 بند ۲)
                        item.specs?.let { specs ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = Color(0xFF0F1824),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = "مشخصات فنی و استناد به منبع (Spec Sources):",
                                        color = Color(0xFF00E5FF),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    SpecSourceItemRow("ولتاژ نامی", specs.ratedVoltage, specs.voltageSource)
                                    SpecSourceItemRow("جریان نامی", specs.ratedCurrent, specs.currentSource)
                                    if (specs.powerRating != "-") {
                                        SpecSourceItemRow("توان نامی", specs.powerRating, specs.powerSource)
                                    }
                                    SpecSourceItemRow("کد استاندارد", specs.standardCode, specs.standardCodeSource)
                                    if (specs.manufacturerModel.isNotBlank()) {
                                        SpecSourceItemRow("مدل تجهیز", specs.manufacturerModel, specs.modelSource)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpecSourceBadge(source: SpecSource) {
    val (bgColor, textColor, label) = when (source.type) {
        SpecSourceType.STANDARD -> Triple(Color(0xFF00E676).copy(alpha = 0.15f), Color(0xFF00E676), if (source.ref.isNotBlank()) "استاندارد: ${source.ref}" else "استاندارد")
        SpecSourceType.DATASHEET -> Triple(Color(0xFF00B0FF).copy(alpha = 0.15f), Color(0xFF00B0FF), "دیتاشیت")
        SpecSourceType.SAMPLE -> Triple(Color(0xFFFF9100).copy(alpha = 0.15f), Color(0xFFFFB74D), "نمونه")
        SpecSourceType.UNKNOWN -> Triple(Color(0xFF78909C).copy(alpha = 0.15f), Color(0xFFCFD8DC), "نامشخص")
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, textColor.copy(alpha = 0.4f))
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun SpecSourceItemRow(name: String, value: String, source: SpecSource) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$name: $value",
            color = Color(0xFFECEFF1),
            fontSize = 10.sp
        )
        SpecSourceBadge(source)
    }
}
