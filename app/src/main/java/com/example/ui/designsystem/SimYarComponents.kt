package com.example.ui.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SimYarGoldVip
import com.example.ui.theme.SimYarGoldVipSecondary
import com.example.ui.theme.SimYarPrimary
import com.example.ui.theme.SimYarRadii
import com.example.ui.theme.SimYarSpacing

enum class SimYarButtonVariant {
    PRIMARY,
    SECONDARY,
    GHOST,
    DANGER
}

/**
 * دکمه اختصاصی سیستم طراحی سیمیار با حداقل ارتفاع لمسی ۴۸dp، گوشه‌های ۱۲dp و استایل فنی
 */
@Composable
fun SimYarButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: SimYarButtonVariant = SimYarButtonVariant.PRIMARY,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = SimYarSpacing.lg, vertical = SimYarSpacing.md),
    testTag: String? = null
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    val containerColor = when (variant) {
        SimYarButtonVariant.PRIMARY -> MaterialTheme.colorScheme.primary
        SimYarButtonVariant.SECONDARY -> MaterialTheme.colorScheme.surfaceVariant
        SimYarButtonVariant.GHOST -> Color.Transparent
        SimYarButtonVariant.DANGER -> MaterialTheme.colorScheme.error
    }

    val contentColor = when (variant) {
        SimYarButtonVariant.PRIMARY -> MaterialTheme.colorScheme.onPrimary
        SimYarButtonVariant.SECONDARY -> MaterialTheme.colorScheme.onSurface
        SimYarButtonVariant.GHOST -> MaterialTheme.colorScheme.primary
        SimYarButtonVariant.DANGER -> MaterialTheme.colorScheme.onError
    }

    val border = when (variant) {
        SimYarButtonVariant.SECONDARY -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        SimYarButtonVariant.GHOST -> null
        else -> null
    }

    val baseModifier = modifier
        .defaultMinSize(minHeight = 48.dp)
        .let { if (testTag != null) it.testTag(testTag) else it }

    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(SimYarRadii.md),
        border = border,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        ),
        contentPadding = contentPadding,
        modifier = baseModifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(SimYarSpacing.sm))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * دکمه آیکونی با تاچ‌تارگت حداقل ۴۸x۴۸ dp طبق استانداردهای Google Accessibility
 */
@Composable
fun SimYarIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    containerColor: Color = Color.Transparent,
    borderColor: Color? = null,
    testTag: String? = null
) {
    val baseModifier = modifier
        .size(48.dp)
        .let { if (testTag != null) it.testTag(testTag) else it }
        .semantics {
            this.contentDescription = contentDescription
            this.role = Role.Button
        }

    val shape = RoundedCornerShape(SimYarRadii.md)
    val border = borderColor?.let { BorderStroke(1.dp, it) }

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        color = containerColor,
        border = border,
        modifier = baseModifier
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null, // در سطح Surface ارائه شده
                tint = if (enabled) tint else tint.copy(alpha = 0.38f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * کارت مهندسی با حاشیه ظریف ۱dp، لایه SurfaceElevated و پس‌زمینه تمیز
 */
@Composable
fun SimYarCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(SimYarRadii.lg),
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    elevation: Dp = 0.dp,
    contentPadding: PaddingValues = PaddingValues(SimYarSpacing.lg),
    content: @Composable () -> Unit
) {
    val clickableModifier = if (onClick != null) {
        modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    } else modifier

    Surface(
        shape = shape,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = elevation,
        modifier = clickableModifier
    ) {
        Box(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}

/**
 * چیپ تعاملی یا برچسب وضعیت فشرده
 */
@Composable
fun SimYarChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    badgeText: String? = null
) {
    val containerColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        animationSpec = tween(150),
        label = "chip_bg"
    )
    val contentColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(150),
        label = "chip_content"
    )
    val borderColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        animationSpec = tween(150),
        label = "chip_border"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(SimYarRadii.sm),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier.defaultMinSize(minHeight = 36.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = SimYarSpacing.md, vertical = SimYarSpacing.xs)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(SimYarSpacing.xs))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
            if (badgeText != null) {
                Spacer(modifier = Modifier.width(SimYarSpacing.xs))
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

/**
 * کپسول وضعیت (Status Pill) برای نمایش حالت شبیه‌ساز (آماده / در حال اجرا / خطا / توقف)
 */
@Composable
fun StatusPill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    hasGlowDot: Boolean = true
) {
    Surface(
        shape = RoundedCornerShape(SimYarRadii.pill),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = SimYarSpacing.md, vertical = 6.dp)
        ) {
            if (hasGlowDot) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(color, CircleShape)
                )
                Spacer(modifier = Modifier.width(SimYarSpacing.sm))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * نشانگر LED برای ورودی‌ها و خروجی‌های PLC و پایانه‌های برقدار
 */
@Composable
fun LedIndicator(
    active: Boolean,
    activeColor: Color = SimYarPrimary,
    inactiveColor: Color = MaterialTheme.colorScheme.outline,
    modifier: Modifier = Modifier,
    size: Dp = 10.dp
) {
    val color = if (active) activeColor else inactiveColor
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
            .border(1.dp, color.copy(alpha = 0.5f), CircleShape)
    )
}

/**
 * نشان طلایی پریمیوم VIP با گرادیان ظریف
 */
@Composable
fun VipBadge(
    modifier: Modifier = Modifier,
    text: String = "VIP"
) {
    val goldBrush = Brush.linearGradient(
        colors = listOf(SimYarGoldVip, SimYarGoldVipSecondary)
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(SimYarRadii.sm))
            .background(goldBrush)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.Star,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }
    }
}

/**
 * سرتیتر بخش‌ها (Section Header) با عنوان و اکشن اختیاری
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = SimYarSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (actionText != null && onActionClick != null) {
            Text(
                text = actionText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable(onClick = onActionClick)
                    .padding(SimYarSpacing.xs)
            )
        }
    }
}

/**
 * جعبه Skeleton برای وضعیت در حال بارگذاری بدون بار گرافیکی سنگین
 */
@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(SimYarRadii.sm)
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    )
}

/**
 * نوار پیشرفت مدرن برای محدودیت پروژه‌ها یا درس‌ها
 */
@Composable
fun ProgressMeter(
    current: Int,
    max: Int,
    modifier: Modifier = Modifier,
    label: String? = null,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val progress = (current.toFloat() / max.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)

    Column(modifier = modifier.fillMaxWidth()) {
        if (label != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$current / $max",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(SimYarSpacing.xs))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}
