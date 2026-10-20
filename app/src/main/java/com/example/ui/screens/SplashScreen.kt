package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SimYarDarkBase
import com.example.ui.theme.SimYarDarkBorderSubtle
import com.example.ui.theme.SimYarPrimary
import com.example.ui.theme.SimYarTextMutedDark
import com.example.ui.theme.SimYarTextSecondaryDark
import kotlinx.coroutines.delay

/**
 * صفحه اسپلش مدرن و پریمیوم سیمیار (زمان اجرا ≤ ۱.۸ ثانیه با لوگوی اختصاصی مهندسی Canvas)
 */
@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    val scale = remember { Animatable(0.6f) }
    val alpha = remember { Animatable(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "circuit_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseGlow"
    )

    LaunchedEffect(Unit) {
        // انیمیشن نرم ورود با زمان بهینه
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
        )
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 400)
        )
        // کل زمان نمایش ۱.۶ ثانیه (مطابق الزام ≤ ۱.۸ ثانیه)
        delay(1100)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SimYarDarkBase),
        contentAlignment = Alignment.Center
    ) {
        // ۱. شبکه خطوط مهندسی پس‌زمینه (Background Technical Grid)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 48.dp.toPx()
            val gridColor = SimYarDarkBorderSubtle.copy(alpha = 0.35f)
            var x = 0f
            while (x <= size.width) {
                drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 0.8f)
                x += step
            }
            var y = 0f
            while (y <= size.height) {
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 0.8f)
                y += step
            }

            // هاله نورانی کانونی در مرکز
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        SimYarPrimary.copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.width * 0.45f
                ),
                radius = size.width * 0.45f,
                center = center
            )
        }

        // ۲. کانتینر مرکزی لوگو و تایپوگرافی
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // لوگوی اختصاصی و لوکس مهندسی رسم‌شده با Canvas
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .scale(scale.value),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val c = Offset(size.width / 2f, size.height / 2f)
                    val r = 54f

                    // حلقه نورانی گردان با هاله جریان الکتریکی
                    rotate(rotation, pivot = c) {
                        drawCircle(
                            brush = Brush.sweepGradient(
                                listOf(
                                    SimYarPrimary,
                                    Color(0xFF00E676),
                                    Color(0xFFFFD600),
                                    SimYarPrimary.copy(alpha = 0.05f),
                                    SimYarPrimary
                                )
                            ),
                            radius = r * pulseGlow,
                            center = c,
                            style = Stroke(width = 3.2f, cap = StrokeCap.Round)
                        )
                    }

                    // کادر شش‌ضلعی مهندسی (Hexagon Core)
                    val hexPath = Path()
                    val hexRadius = 42f
                    for (i in 0..5) {
                        val angleDeg = 60.0 * i - 30.0
                        val rad = Math.toRadians(angleDeg)
                        val px = c.x + (hexRadius * Math.cos(rad)).toFloat()
                        val py = c.y + (hexRadius * Math.sin(rad)).toFloat()
                        if (i == 0) hexPath.moveTo(px, py) else hexPath.lineTo(px, py)
                    }
                    hexPath.close()

                    drawPath(
                        path = hexPath,
                        color = Color(0xFF0F1724),
                        style = androidx.compose.ui.graphics.drawscope.Fill
                    )
                    drawPath(
                        path = hexPath,
                        color = SimYarPrimary.copy(alpha = 0.6f),
                        style = Stroke(width = 2f)
                    )

                    // گره‌های مداری و خطوط هادی الکتریکی (Conductive Bus Tracks)
                    drawLine(
                        color = SimYarPrimary,
                        start = Offset(c.x - 24f, c.y),
                        end = Offset(c.x + 24f, c.y),
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = SimYarPrimary,
                        start = Offset(c.x, c.y - 24f),
                        end = Offset(c.x, c.y + 24f),
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )

                    // نماد توان الکتریکی و انرژی در قلب لوگو
                    drawCircle(
                        color = Color(0xFFFFD600),
                        radius = 6f,
                        center = c
                    )
                    drawCircle(
                        color = SimYarPrimary,
                        radius = 12f,
                        center = c,
                        style = Stroke(width = 2.5f)
                    )

                    // نقاط تماسی مداری پیرامونی
                    val nodeOffsets = listOf(
                        Offset(c.x - 24f, c.y),
                        Offset(c.x + 24f, c.y),
                        Offset(c.x, c.y - 24f),
                        Offset(c.x, c.y + 24f)
                    )
                    for (node in nodeOffsets) {
                        drawCircle(color = SimYarPrimary, radius = 3.5f, center = node)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // برندینگ و عنوان برنامه
            Text(
                text = "سیمیار",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = SimYarPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "طراحی تخصصی مدار فرمان و شبیه‌ساز PLC",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = SimYarTextSecondaryDark
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "استاندارد برق صنعتی IEC 60947 و IEC 61131-3",
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = SimYarTextMutedDark
            )
        }

        // ۳. پانوشت توسعه و نسخه در پایین صفحه
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .height(68.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "ابزار مهندسی برق ردلاین سافت",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF94A3B8)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "نسخه ۱.۱.۰",
                fontSize = 10.sp,
                color = Color(0xFF475569)
            )
        }
    }
}
