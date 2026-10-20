package com.example.ui.screens

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
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.navigation.AppNavScreen
import com.example.ui.components.VipUpgradeDialog
import com.example.viewmodel.SimYarViewModel

@Composable
fun SettingsScreen(
    viewModel: SimYarViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val usePersianDigits by viewModel.usePersianDigits.collectAsState()
    val isSnapToGrid by viewModel.isSnapToGrid.collectAsState()
    val isVipUnlocked by viewModel.billingRepository.isVipUnlocked.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()

    var isHapticEnabled by remember { mutableStateOf(true) }
    var showVipDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // هدر تنظیمات
        Surface(
            color = MaterialTheme.colorScheme.surface,
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
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "تنظیمات",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "تنظیمات نرم‌افزار سیمیار",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "شخصی‌سازی تم روشن/تیره، فونت، شبکه بوم و وضعیت لایسنس",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // وضعیت لایسنس و VIP
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Diamond,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isVipUnlocked) "اشتراک طلایی VIP فعال است" else "پلن پایه و رایگان",
                            color = if (isVipUnlocked) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = if (isVipUnlocked) "دسترسی کامل به تمام قطعات، پروژه‌های نامحدود و خروجی‌ها" else "۳ پروژه، ۱۰ قطعه پایه، ۵ درس آکادمی",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }
                }

                if (!isVipUnlocked) {
                    OutlinedButton(
                        onClick = { showVipDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary)
                    ) {
                        Text("ارتقا به VIP", color = MaterialTheme.colorScheme.tertiary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // لیست گزینه‌های تنظیمات
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {

                // سوئیچ تم تیره / روشن
                SettingSwitchRow(
                    icon = Icons.Outlined.ColorLens,
                    title = "حالت تم تیره (Dark Mode)",
                    subtitle = "تغییر بین تم تیره مهندسی (#061426) و تم روشن استاندارد",
                    checked = isDarkTheme,
                    isDarkTheme = isDarkTheme,
                    onCheckedChange = { viewModel.setDarkTheme(it) }
                )

                // سوئیچ اعداد فارسی
                SettingSwitchRow(
                    icon = Icons.Outlined.FormatListNumbered,
                    title = "نمایش ارقام و اعداد به فارسی",
                    subtitle = "تبدیل تمام اعداد در متون و برچسب‌ها به ارقام فارسی",
                    checked = usePersianDigits,
                    isDarkTheme = isDarkTheme,
                    onCheckedChange = { viewModel.togglePersianDigits() }
                )

                // سوئیچ Snap to Grid
                SettingSwitchRow(
                    icon = Icons.Outlined.GridOn,
                    title = "چسبیدن قطعات به شبکه (Snap to Grid)",
                    subtitle = "تراز دقیق و خودکار پایانه‌ها روی گرید ۲۰ پیکسل",
                    checked = isSnapToGrid,
                    isDarkTheme = isDarkTheme,
                    onCheckedChange = { viewModel.toggleSnapToGrid() }
                )

                // سوئیچ بازخورد لمسی (Haptic)
                SettingSwitchRow(
                    icon = Icons.Outlined.Vibration,
                    title = "بازخورد لمسی (ویبره Haptic)",
                    subtitle = "لرزش خفیف هنگام اتصال سیم، لمس طولانی و چفت شدن به گرید",
                    checked = isHapticEnabled,
                    isDarkTheme = isDarkTheme,
                    onCheckedChange = { isHapticEnabled = it }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // درباره ما و توسعه‌دهنده ردلاین سافت
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clickable { viewModel.navigateTo(AppNavScreen.ABOUT_US) }
                .semantics { contentDescription = "مشاهده اطلاعات درباره ما و توسعه‌دهنده ردلاین سافت البرز" }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "درباره ما و تیم ردلاین سافت البرز",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "مدیریت مهندس مهدی رضایی | نسخه ۱.۰",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showVipDialog) {
        VipUpgradeDialog(
            billingRepository = viewModel.billingRepository,
            onDismissRequest = { showVipDialog = false }
        )
    }
}

@Composable
private fun SettingSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    isDarkTheme: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF00E5FF),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    color = if (isDarkTheme) Color(0xFFECEFF1) else Color(0xFF0F172A),
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
                Text(
                    text = subtitle,
                    color = Color(0xFF78909C),
                    fontSize = 10.sp
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = Color(0xFF00E5FF)
            ),
            modifier = Modifier.semantics { contentDescription = "$title: ${if (checked) "فعال" else "غیرفعال"}" }
        )
    }
}
