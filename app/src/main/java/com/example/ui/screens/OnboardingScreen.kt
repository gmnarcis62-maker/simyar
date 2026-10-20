package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SimYarDarkBase
import com.example.ui.theme.SimYarDarkBorderStrong
import com.example.ui.theme.SimYarDarkBorderSubtle
import com.example.ui.theme.SimYarDarkSurface
import com.example.ui.theme.SimYarPrimary
import com.example.ui.theme.SimYarSuccess
import com.example.ui.theme.SimYarTextMutedDark
import com.example.ui.theme.SimYarTextPrimaryDark
import com.example.ui.theme.SimYarTextSecondaryDark
import com.example.ui.theme.SimYarWarning

/**
 * مدل داده هر گام آشنایی
 */
private data class OnboardingStepData(
    val title: String,
    val subtitle: String,
    val description: String,
    val accentColor: Color
)

/**
 * صفحه آنبوردینگ پریمیوم سیمیار با تصویرسازی‌های مهندسی بر روی Canvas
 */
@Composable
fun OnboardingScreen(
    onFinishOnboarding: () -> Unit
) {
    val steps = remember {
        listOf(
            OnboardingStepData(
                title = "طراحی آزاد مدار فرمان و قدرت",
                subtitle = "بوم نامحدود با تجهیزات استاندارد تابلو برق IEC",
                description = "قطعات صنعتی را با سیم‌کشی ارتوگونال هوشمند و چسبیدن به شبکه رسم کنید. کلیدها، بی‌متال، کنتاکتورها و کابل‌ها با استانداردهای رسمی بین‌المللی آماده اتصال هستند.",
                accentColor = SimYarPrimary
            ),
            OnboardingStepData(
                title = "شبیه‌سازی زنده و عیب‌یابی آنی",
                subtitle = "تجسم ۶۰fps شارش جریان، اتصال کوتاه و تریپ",
                description = "جریان پیوسته الکتریسیته، چرخش واقعی موتورهای سه‌فاز و تریپ بی‌متال را با فیدبک گرافیکی و صوتی ببینید و مدارهای خود را پیش از ساخت در تابلو تست کنید.",
                accentColor = SimYarSuccess
            ),
            OnboardingStepData(
                title = "پردازشگر PLC نردبانی و آکادمی",
                subtitle = "همگام‌سازی دوطرفه لدر با ورودی/خروجی‌های مدار",
                description = "شستی‌های مدار را به ورودی‌های I و بوبین‌ها را به خروجی‌های Q متصل کرده، زمان اسکن را پایش کنید و در آکادمی تعاملی پروژه‌های صنعتی واقعی بسازید.",
                accentColor = SimYarWarning
            )
        )
    }

    var currentStepIndex by remember { mutableIntStateOf(0) }
    val currentStep = steps[currentStepIndex]

    val infiniteTransition = rememberInfiniteTransition(label = "onboarding_anim")
    val pulseAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SimYarDarkBase)
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        // ۱. نوار رد شدن بالای صفحه (Skip)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = SimYarDarkSurface,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SimYarDarkBorderSubtle)
            ) {
                Text(
                    text = "سیمیار | SimYar",
                    color = SimYarPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            TextButton(
                onClick = onFinishOnboarding,
                modifier = Modifier.height(48.dp)
            ) {
                Text(
                    text = "رد شدن",
                    color = SimYarTextMutedDark,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // ۲. بخش مرکزی: تصویرسازی بوم + متون
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 60.dp, bottom = 90.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // تصویرسازی اختصاصی مهندسی بر روی Canvas برای هر گام
            Box(
                modifier = Modifier
                    .size(width = 280.dp, height = 180.dp)
                    .background(SimYarDarkSurface, RoundedCornerShape(20.dp))
                    .border(1.dp, SimYarDarkBorderStrong, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                when (currentStepIndex) {
                    0 -> CircuitIllustrationCanvas(pulse = pulseAnim)
                    1 -> SimulationIllustrationCanvas(pulse = pulseAnim)
                    2 -> PlcLadderIllustrationCanvas(pulse = pulseAnim)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // عنوان اصلی گام
            Text(
                text = currentStep.title,
                color = SimYarTextPrimaryDark,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            // زیرعنوان مشخصه
            Text(
                text = currentStep.subtitle,
                color = currentStep.accentColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            // متن تشریحی
            Text(
                text = currentStep.description,
                color = SimYarTextSecondaryDark,
                fontSize = 13.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // نشانگرهای نقاط صفحات (Page Indicators)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                steps.indices.forEach { index ->
                    val isSelected = index == currentStepIndex
                    Box(
                        modifier = Modifier
                            .size(
                                width = if (isSelected) 28.dp else 8.dp,
                                height = 8.dp
                            )
                            .background(
                                color = if (isSelected) currentStep.accentColor else SimYarDarkBorderSubtle,
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                }
            }
        }

        // ۳. نوار دکمه‌های ناوبری پایین صفحه با ارتفاع لمسی استاندارد (≥ 48dp)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentStepIndex > 0) {
                OutlinedButton(
                    onClick = { currentStepIndex-- },
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SimYarDarkBorderSubtle),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(
                        text = "مرحله قبلی",
                        color = SimYarTextSecondaryDark,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }

            Button(
                onClick = {
                    if (currentStepIndex < steps.lastIndex) {
                        currentStepIndex++
                    } else {
                        onFinishOnboarding()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = currentStep.accentColor
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Text(
                    text = if (currentStepIndex == steps.lastIndex) "ورود به میز کار مهندسی" else "مرحله بعدی",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

/**
 * تصویرسازی گام اول: بوم طراحی مدار، نمادهای الکتریکی و سیم‌بندی
 */
@Composable
private fun CircuitIllustrationCanvas(pulse: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // شبکه پس‌زمینه نقشه مهندسی
        val gridStep = 24.dp.toPx()
        var gx = 0f
        while (gx <= w) {
            drawLine(Color(0xFF1E2D42).copy(alpha = 0.4f), Offset(gx, 0f), Offset(gx, h), strokeWidth = 0.8f)
            gx += gridStep
        }
        var gy = 0f
        while (gy <= h) {
            drawLine(Color(0xFF1E2D42).copy(alpha = 0.4f), Offset(0f, gy), Offset(w, gy), strokeWidth = 0.8f)
            gy += gridStep
        }

        // نماد شستی استارت (SB1)
        val sbX = w * 0.28f
        val sbY = h * 0.45f
        drawLine(Color(0xFF00D1FF), Offset(sbX - 35f, sbY), Offset(sbX - 12f, sbY), strokeWidth = 2.5f)
        drawLine(Color(0xFF00D1FF), Offset(sbX + 12f, sbY), Offset(sbX + 35f, sbY), strokeWidth = 2.5f)
        drawCircle(Color(0xFF00D1FF), radius = 3.5f, center = Offset(sbX - 12f, sbY))
        drawCircle(Color(0xFF00D1FF), radius = 3.5f, center = Offset(sbX + 12f, sbY))
        drawLine(Color(0xFF00D1FF), Offset(sbX - 14f, sbY - 10f), Offset(sbX + 14f, sbY - 10f), strokeWidth = 3f)
        drawLine(Color(0xFF00D1FF), Offset(sbX, sbY - 10f), Offset(sbX, sbY - 22f), strokeWidth = 2f)

        // نماد کویل کنتاکتور (KM1)
        val kmX = w * 0.72f
        val kmY = h * 0.45f
        drawRoundRect(
            color = Color(0xFF00D1FF),
            topLeft = Offset(kmX - 22f, kmY - 18f),
            size = Size(44f, 36f),
            cornerRadius = CornerRadius(4f, 4f),
            style = Stroke(width = 2.5f)
        )
        drawLine(Color(0xFF00D1FF), Offset(kmX - 22f, kmY - 18f), Offset(kmX + 22f, kmY + 18f), strokeWidth = 1.5f)

        // سیم‌کشی ارتوگونال بین دو قطعه
        val wirePath = Path().apply {
            moveTo(sbX + 35f, sbY)
            lineTo(w * 0.5f, sbY)
            lineTo(w * 0.5f, kmY)
            lineTo(kmX - 22f, kmY)
        }
        drawPath(wirePath, color = Color(0xFF00D1FF), style = Stroke(width = 2.5f))

        // هاله پالس متحرک روی سیم
        val pulseX = sbX + 35f + (kmX - 22f - (sbX + 35f)) * pulse
        drawCircle(Color(0xFF00D1FF), radius = 4f, center = Offset(pulseX, sbY))
        drawCircle(Color(0x5500D1FF), radius = 8f, center = Offset(pulseX, sbY))
    }
}

/**
 * تصویرسازی گام دوم: شبیه‌سازی زنده، موتور سه‌فاز و جریان برق‌دار
 */
@Composable
private fun SimulationIllustrationCanvas(pulse: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val c = Offset(w * 0.5f, h * 0.5f)

        // چرخش روتور موتور سه‌فاز M
        rotate(pulse * 360f, pivot = c) {
            // بدنه دایره‌ای موتور
            drawCircle(Color(0xFF10B981).copy(alpha = 0.15f), radius = 48f, center = c)
            drawCircle(Color(0xFF10B981), radius = 48f, center = c, style = Stroke(width = 3f))

            // تیغه‌های روتور در حال چرخش
            for (i in 0..2) {
                val rad = Math.toRadians(i * 120.0)
                val ex = c.x + (42f * Math.cos(rad)).toFloat()
                val ey = c.y + (42f * Math.sin(rad)).toFloat()
                drawLine(Color(0xFF10B981), c, Offset(ex, ey), strokeWidth = 3f, cap = StrokeCap.Round)
            }
        }

        // حرف M در مرکز
        drawCircle(Color(0xFF090D14), radius = 18f, center = c)
        drawCircle(Color(0xFF10B981), radius = 18f, center = c, style = Stroke(width = 1.8f))

        // خطوط سه‌فاز برق‌دار متصل به موتور با رنگ سبز پرانرژی
        val phaseY = listOf(h * 0.25f, h * 0.5f, h * 0.75f)
        phaseY.forEachIndexed { i, py ->
            drawLine(
                color = Color(0xFF10B981),
                start = Offset(w * 0.08f, py),
                end = Offset(w * 0.28f, py),
                strokeWidth = 2.5f
            )
            drawCircle(Color(0xFF10B981), radius = 3.5f, center = Offset(w * 0.28f, py))
        }

        // چراغ سیگنال سبز روشن در سمت راست
        val lampC = Offset(w * 0.85f, h * 0.5f)
        drawCircle(Color(0xFF10B981).copy(alpha = 0.25f), radius = 22f, center = lampC)
        drawCircle(Color(0xFF10B981), radius = 14f, center = lampC)
        drawLine(Color.White, Offset(lampC.x - 8f, lampC.y - 8f), Offset(lampC.x + 8f, lampC.y + 8f), strokeWidth = 2f)
        drawLine(Color.White, Offset(lampC.x - 8f, lampC.y + 8f), Offset(lampC.x + 8f, lampC.y - 8f), strokeWidth = 2f)
    }
}

/**
 * تصویرسازی گام سوم: لدر نردبانی PLC، تایمر و نشانگرهای LED
 */
@Composable
private fun PlcLadderIllustrationCanvas(pulse: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // ریل‌های تغذیه لدر (چپ و راست)
        val leftRailX = w * 0.16f
        val rightRailX = w * 0.84f
        drawLine(Color(0xFFF59E0B), Offset(leftRailX, h * 0.15f), Offset(leftRailX, h * 0.85f), strokeWidth = 3.5f)
        drawLine(Color(0xFFF59E0B), Offset(rightRailX, h * 0.15f), Offset(rightRailX, h * 0.85f), strokeWidth = 3.5f)

        // رانگ اول لدر: کنتاکت I0.0 به کویل Q0.0
        val rung1Y = h * 0.38f
        drawLine(Color(0xFFF59E0B), Offset(leftRailX, rung1Y), Offset(rightRailX, rung1Y), strokeWidth = 2f)

        // کنتاکت ورودی -[ ]-
        val cX = w * 0.36f
        drawRect(Color(0xFF0F1724), topLeft = Offset(cX - 14f, rung1Y - 12f), size = Size(28f, 24f))
        drawLine(Color(0xFFF59E0B), Offset(cX - 8f, rung1Y - 10f), Offset(cX - 8f, rung1Y + 10f), strokeWidth = 2.5f)
        drawLine(Color(0xFFF59E0B), Offset(cX + 8f, rung1Y - 10f), Offset(cX + 8f, rung1Y + 10f), strokeWidth = 2.5f)

        // کویل خروجی -( )-
        val coilX = w * 0.66f
        drawRect(Color(0xFF0F1724), topLeft = Offset(coilX - 16f, rung1Y - 12f), size = Size(32f, 24f))
        drawArc(
            color = Color(0xFFF59E0B),
            startAngle = 90f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(coilX - 12f, rung1Y - 10f),
            size = Size(10f, 20f),
            style = Stroke(width = 2.5f)
        )
        drawArc(
            color = Color(0xFFF59E0B),
            startAngle = 270f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(coilX + 2f, rung1Y - 10f),
            size = Size(10f, 20f),
            style = Stroke(width = 2.5f)
        )

        // رانگ دوم: بلوک تایمر TON
        val rung2Y = h * 0.68f
        drawLine(Color(0xFFF59E0B), Offset(leftRailX, rung2Y), Offset(rightRailX, rung2Y), strokeWidth = 2f)

        val tonX = w * 0.5f
        drawRoundRect(
            color = Color(0xFF0F1724),
            topLeft = Offset(tonX - 35f, rung2Y - 16f),
            size = Size(70f, 32f),
            cornerRadius = CornerRadius(4f, 4f)
        )
        drawRoundRect(
            color = Color(0xFFF59E0B),
            topLeft = Offset(tonX - 35f, rung2Y - 16f),
            size = Size(70f, 32f),
            cornerRadius = CornerRadius(4f, 4f),
            style = Stroke(width = 2f)
        )

        // خط پیشرفت شارژ تایمر متحرک
        val pBarW = 50f * pulse
        drawLine(Color(0xFFF59E0B), Offset(tonX - 25f, rung2Y + 4f), Offset(tonX - 25f + pBarW, rung2Y + 4f), strokeWidth = 3f)

        // نشانگر LED فعال سبز بالای تایمر
        drawCircle(Color(0xFF10B981), radius = 3.5f, center = Offset(tonX + 22f, rung2Y - 8f))
    }
}
