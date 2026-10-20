package com.example.ui.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SimYarDanger
import com.example.ui.theme.SimYarPrimary
import com.example.ui.theme.SimYarSpacing
import com.example.ui.theme.SimYarSuccess
import com.example.ui.theme.SimYarWarning

/**
 * گالری قطعات سیستم طراحی سیمیار (Design Gallery)
 * جهت نمایش و اعتبارسنجی بصری تمامی کامپوننت‌های سیستم طراحی در حالت‌های تیره و روشن
 */
@Composable
fun DesignGalleryScreen(
    onBack: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var chipSelected by remember { mutableStateOf(true) }
    var switchState by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        SimYarTopBar(
            title = "گالری سیستم طراحی سیمیار",
            subtitle = "Design System Components Gallery",
            onBackClick = onBack
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(SimYarSpacing.lg)
        ) {
            // ۱. دکمه‌ها
            SectionHeader(title = "دکمه‌های اصلی و ثانویه (Buttons)", subtitle = "حداقل ارتفاع لمسی ۴۸dp")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SimYarSpacing.md)
            ) {
                SimYarButton(
                    text = "اجرای شبیه‌سازی",
                    icon = Icons.Outlined.PlayArrow,
                    onClick = {},
                    variant = SimYarButtonVariant.PRIMARY,
                    modifier = Modifier.weight(1f)
                )
                SimYarButton(
                    text = "تنظیمات مدار",
                    icon = Icons.Outlined.Settings,
                    onClick = {},
                    variant = SimYarButtonVariant.SECONDARY,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(SimYarSpacing.sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SimYarSpacing.md)
            ) {
                SimYarButton(
                    text = "حذف پروژه",
                    onClick = {},
                    variant = SimYarButtonVariant.DANGER,
                    modifier = Modifier.weight(1f)
                )
                SimYarButton(
                    text = "انصراف",
                    onClick = {},
                    variant = SimYarButtonVariant.GHOST,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(SimYarSpacing.xl))

            // ۲. چیپ‌ها و بج‌ها
            SectionHeader(title = "چیپ‌ها و نشان‌ها (Chips & Badges)")
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SimYarSpacing.sm)
            ) {
                SimYarChip(
                    text = "کنتاکتورها",
                    selected = chipSelected,
                    onClick = { chipSelected = !chipSelected }
                )
                SimYarChip(
                    text = "سنسورها",
                    selected = !chipSelected,
                    onClick = { chipSelected = !chipSelected },
                    badgeText = "جدید"
                )
                VipBadge()
            }

            Spacer(modifier = Modifier.height(SimYarSpacing.xl))

            // ۳. کپسول‌های وضعیت و LED
            SectionHeader(title = "وضعیت شبیه‌سازی و شاخص‌های LED")
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SimYarSpacing.sm)
            ) {
                StatusPill(text = "در حال اجرا (RUN)", color = SimYarSuccess)
                StatusPill(text = "هشدار اینترلاک", color = SimYarWarning)
                StatusPill(text = "اتصال کوتاه", color = SimYarDanger)
            }
            Spacer(modifier = Modifier.height(SimYarSpacing.sm))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SimYarSpacing.md)
            ) {
                Text("ورودی‌های دیجیتال PLC:", style = MaterialTheme.typography.bodyMedium)
                LedIndicator(active = true, activeColor = SimYarPrimary)
                LedIndicator(active = false)
                LedIndicator(active = true, activeColor = SimYarSuccess)
            }

            Spacer(modifier = Modifier.height(SimYarSpacing.xl))

            // ۴. کارت‌ها و پیشرفت
            SectionHeader(title = "کارت‌ها و نشانگر پیشرفت (Cards & Progress)")
            SimYarCard {
                Column {
                    Text(
                        text = "مدار راه‌اندازی مستقیم ستاره-مثلث",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(SimYarSpacing.xs))
                    Text(
                        text = "دارای کنتاکتور اصلی KM1، ستاره KM2 و مثلث KM3 با تایمر KT1",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(SimYarSpacing.md))
                    ProgressMeter(
                        current = 2,
                        max = 3,
                        label = "استفاده از ظرفیت پروژه‌های رایگان"
                    )
                }
            }

            Spacer(modifier = Modifier.height(SimYarSpacing.xl))

            // ۵. ردیف تنظیمات
            SectionHeader(title = "ردیف‌های تنظیمات (Settings Rows)")
            SimYarCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                Column {
                    SettingsRow(
                        title = "چسبیدن به شبکه شطرنجی (Snap to Grid)",
                        subtitle = "تراز اتوماتیک پایه‌ها روی شبکه ۱۰ پیکسلی",
                        icon = Icons.Outlined.Check,
                        checked = switchState,
                        onCheckedChange = { switchState = it }
                    )
                    SettingsRow(
                        title = "ارقام فارسی در نقشه‌کشی",
                        subtitle = "نمایش متون و شماره المان‌ها با رسم‌الخط فارسی",
                        icon = Icons.Outlined.Settings,
                        checked = false,
                        onCheckedChange = {}
                    )
                }
            }

            Spacer(modifier = Modifier.height(SimYarSpacing.xl))

            // ۶. اسنک‌بار و پیام‌های سیستم
            SectionHeader(title = "پیام‌های اطلاع‌رسانی سیستم (Snackbars)")
            SimYarSnackbar(
                message = "تغییرات پروژه با موفقیت در پایگاه‌داده محلی ذخیره شد.",
                isError = false,
                actionText = "مشاهده"
            )
            Spacer(modifier = Modifier.height(SimYarSpacing.xs))
            SimYarSnackbar(
                message = "خطا در برقراری اتصال سیم: پایانه مقصد پر است.",
                isError = true,
                actionText = "نادیده گرفتن"
            )
        }

        // ناوبری پایین صفحه برای نمایش کامپوننت BottomBar
        SimYarBottomBar(
            items = listOf(
                SimYarNavItem("پروژه‌ها", Icons.Outlined.Folder, selectedTab == 0, { selectedTab = 0 }),
                SimYarNavItem("ویرایشگر", Icons.Outlined.AccountTree, selectedTab == 1, { selectedTab = 1 }),
                SimYarNavItem("تنظیمات", Icons.Outlined.Settings, selectedTab == 2, { selectedTab = 2 })
            )
        )
    }
}

// -------------------------------------------------------------
// پیش‌نمایش‌های دوگانه (Dark / Light) و مقیاس فونت ۱.۰ و ۱.۳
// -------------------------------------------------------------

@Preview(name = "Dark Theme Standard", showBackground = true)
@Composable
private fun DesignGalleryDarkPreview() {
    MyApplicationTheme(darkTheme = true) {
        Surface {
            DesignGalleryScreen()
        }
    }
}

@Preview(name = "Light Theme Standard", showBackground = true)
@Composable
private fun DesignGalleryLightPreview() {
    MyApplicationTheme(darkTheme = false) {
        Surface {
            DesignGalleryScreen()
        }
    }
}

@Preview(name = "Dark Theme Large Font (1.3x)", fontScale = 1.3f, showBackground = true)
@Composable
private fun DesignGalleryLargeFontPreview() {
    MyApplicationTheme(darkTheme = true) {
        Surface {
            DesignGalleryScreen()
        }
    }
}
