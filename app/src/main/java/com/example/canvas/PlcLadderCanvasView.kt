package com.example.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FitScreen
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LadderElement
import com.example.model.LadderElementType
import com.example.model.LadderRung
import com.example.util.PersianUtils
import kotlin.math.abs

/**
 * محاسبات ابعاد، چیدمان و انطباق (Fit) دیاگرام نردبانی PLC
 */
object PlcLadderLayoutCalculator {
    const val MIN_RUNG_WIDTH_DP = 780f
    const val SYMBOL_WIDTH_DP = 76f
    const val SYMBOL_SPACING_DP = 8f
    const val RAIL_AND_PADDING_DP = 160f
    const val DEFAULT_RUNG_HEIGHT_DP = 100f
    const val BRANCH_RUNG_HEIGHT_DP = 140f
    const val RUNG_SPACING_DP = 14f
    const val PADDING_VERTICAL_DP = 32f

    fun calculateRungWidthDp(rung: LadderRung, hasActiveTool: Boolean): Float {
        val numContacts = rung.contacts.size + (if (hasActiveTool) 1 else 0)
        val numBranches = rung.branchContacts.size + (if (hasActiveTool) 1 else 0)
        val maxInputCols = maxOf(numContacts, numBranches, 1)
        val numCoils = rung.coils.size + (if (hasActiveTool) 1 else 0)

        val inputsWidth = maxInputCols * (SYMBOL_WIDTH_DP + SYMBOL_SPACING_DP)
        val outputsWidth = maxOf(numCoils, 1) * (SYMBOL_WIDTH_DP + SYMBOL_SPACING_DP)
        val calculated = inputsWidth + outputsWidth + RAIL_AND_PADDING_DP + 120f
        return maxOf(MIN_RUNG_WIDTH_DP, calculated)
    }

    fun calculateTotalContentWidthDp(rungs: List<LadderRung>, hasActiveTool: Boolean): Float {
        if (rungs.isEmpty()) return MIN_RUNG_WIDTH_DP
        val maxRungW = rungs.maxOf { calculateRungWidthDp(it, hasActiveTool) }
        return maxOf(MIN_RUNG_WIDTH_DP, maxRungW)
    }

    fun calculateTotalContentHeightDp(rungs: List<LadderRung>, hasActiveTool: Boolean): Float {
        if (rungs.isEmpty()) return 200f
        var totalH = PADDING_VERTICAL_DP
        rungs.forEachIndexed { index, rung ->
            val hasBranches = rung.branchContacts.isNotEmpty() || rung.parallelBranches.isNotEmpty()
            val h = if (hasBranches || hasActiveTool) BRANCH_RUNG_HEIGHT_DP else DEFAULT_RUNG_HEIGHT_DP
            totalH += h
            if (index < rungs.size - 1) {
                totalH += RUNG_SPACING_DP
            }
        }
        return totalH
    }

    fun calculateFit(
        contentWidthPx: Float,
        contentHeightPx: Float,
        viewportWidthPx: Float,
        viewportHeightPx: Float,
        marginPx: Float = 40f
    ): Pair<Float, Offset> {
        if (contentWidthPx <= 0f || contentHeightPx <= 0f || viewportWidthPx <= 0f || viewportHeightPx <= 0f) {
            return Pair(1.0f, Offset.Zero)
        }
        val availableW = (viewportWidthPx - marginPx * 2f).coerceAtLeast(100f)
        val availableH = (viewportHeightPx - marginPx * 2f).coerceAtLeast(100f)
        val scaleX = availableW / contentWidthPx
        val scaleY = availableH / contentHeightPx
        val fitScale = minOf(scaleX, scaleY).coerceIn(0.2f, 2.5f)

        val fittedW = contentWidthPx * fitScale
        val fittedH = contentHeightPx * fitScale
        val panX = (viewportWidthPx - fittedW) / 2f
        val panY = maxOf(marginPx, (viewportHeightPx - fittedH) / 2f)
        return Pair(fitScale, Offset(panX, panY))
    }
}

private val OffsetSaver = Saver<Offset, List<Float>>(
    save = { listOf(it.x, it.y) },
    restore = { Offset(it[0], it[1]) }
)

/**
 * بوم ترسیم دیاگرام نردبانی PLC (PLC Ladder Canvas) بر اساس استاندارد بین‌المللی IEC 61131-3:
 * - اسکرول روان و دو بعدی در تمام جهت‌ها (Omnidirectional 2D Pan X & Y)
 * - بزرگ‌نمایی و کوچک‌نمایی روان (Pinch-to-zoom و دکمه‌های کنترل زوم)
 * - قابلیت تمام صفحه شدن بوم (Fullscreen mode)
 * - محاسبه Fit و نمایش کل برنامه با ابعاد نامحدود (Unbounded Constraints)
 * - ریل‌های تغذیه چپ و راست (Power Rails در ساختار مهندسی LTR)
 * - خطوط افقی سیم‌بندی رانگ با نمایش جریان زنده (Live Power Flow Highlighting)
 * - نمادهای استاندارد مهندسی PLC و شاخه‌های موازی خودنگهدار
 */
@Composable
fun PlcLadderCanvasView(
    rungs: List<LadderRung>,
    isPlcRunning: Boolean,
    activeTool: LadderElementType? = null,
    isFullscreen: Boolean = false,
    onToggleFullscreen: (() -> Unit)? = null,
    onContactToggled: (String) -> Unit,
    onDeleteRung: (String) -> Unit = {},
    onMoveRungUp: (String) -> Unit = {},
    onMoveRungDown: (String) -> Unit = {},
    onAddElementClick: (rungId: String) -> Unit = {},
    onAddBranchClick: (rungId: String) -> Unit = {},
    onElementClick: (rungId: String, element: LadderElement) -> Unit = { _, _ -> },
    onPlaceSymbol: (rungId: String, type: LadderElementType, isBranch: Boolean) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    var scale by rememberSaveable { mutableFloatStateOf(1.0f) }
    var panOffset by rememberSaveable(stateSaver = OffsetSaver) { mutableStateOf(Offset.Zero) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var contentSize by remember { mutableStateOf(IntSize.Zero) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .clipToBounds()
                .background(Color(0xFF0B1019))
                .onSizeChanged { viewportSize = it }
                .pointerInput(Unit) {
                    detectLadderPanAndZoom(
                        onPan = { panDelta ->
                            panOffset += panDelta
                        },
                        onZoomAndPan = { centroid, panDelta, zoomFactor ->
                            val oldScale = scale
                            val newScale = (oldScale * zoomFactor).coerceIn(0.2f, 2.8f)
                            panOffset = centroid - (centroid - panOffset) * (newScale / oldScale) + panDelta
                            scale = newScale
                        }
                    )
                }
                .semantics { contentDescription = "بوم ترسیم منطق نردبانی PLC Ladder با قابلیت بزرگنمایی و اسکرول به تمام جهات" }
        ) {
            // محتوای گرافیکی بوم با تبدیل شتاب‌یافته سختی‌افزاری GPU و اندازه‌گیری نامحدود (Unbounded Constraint)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = panOffset.x
                        translationY = panOffset.y
                        transformOrigin = TransformOrigin(0f, 0f)
                    }
                    .wrapContentSize(unbounded = true, align = Alignment.TopStart)
            ) {
                val contentWidthDp = remember(rungs, activeTool) {
                    PlcLadderLayoutCalculator.calculateTotalContentWidthDp(rungs, activeTool != null).dp
                }

                Column(
                    modifier = Modifier
                        .width(contentWidthDp)
                        .onSizeChanged { contentSize = it }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    for (rung in rungs) {
                        LadderRungCanvasRow(
                            rung = rung,
                            isPlcRunning = isPlcRunning,
                            activeTool = activeTool,
                            onContactToggled = onContactToggled,
                            onDeleteRung = { onDeleteRung(rung.id) },
                            onMoveRungUp = { onMoveRungUp(rung.id) },
                            onMoveRungDown = { onMoveRungDown(rung.id) },
                            onAddElementClick = { onAddElementClick(rung.id) },
                            onAddBranchClick = { onAddBranchClick(rung.id) },
                            onElementClick = { elem -> onElementClick(rung.id, elem) },
                            onPlaceSymbol = { type, isBranch -> onPlaceSymbol(rung.id, type, isBranch) }
                        )
                    }
                }
            }

            // پنل شناور کنترل بوم (تمام‌صفحه، نمایش کل برنامه Fit، بزرگ‌نمایی، کوچک‌نمایی و مقیاس ۱:۱)
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(2.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (onToggleFullscreen != null) {
                        IconButton(
                            onClick = onToggleFullscreen,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = if (isFullscreen) Icons.Outlined.FullscreenExit else Icons.Outlined.Fullscreen,
                                contentDescription = if (isFullscreen) "خروج از تمام صفحه" else "تمام صفحه شدن بوم لدر",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            val (fitScale, fitPan) = PlcLadderLayoutCalculator.calculateFit(
                                contentWidthPx = contentSize.width.toFloat(),
                                contentHeightPx = contentSize.height.toFloat(),
                                viewportWidthPx = viewportSize.width.toFloat(),
                                viewportHeightPx = viewportSize.height.toFloat()
                            )
                            scale = fitScale
                            panOffset = fitPan
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FitScreen,
                            contentDescription = "نمایش کل برنامه نردبانی (Fit)",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            val center = if (viewportSize.width > 0 && viewportSize.height > 0) {
                                Offset(viewportSize.width / 2f, viewportSize.height / 2f)
                            } else Offset.Zero
                            val oldScale = scale
                            val newScale = (oldScale * 1.25f).coerceAtMost(2.8f)
                            panOffset = center - (center - panOffset) * (newScale / oldScale)
                            scale = newScale
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = "بزرگ‌نمایی بوم نردبانی",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            val center = if (viewportSize.width > 0 && viewportSize.height > 0) {
                                Offset(viewportSize.width / 2f, viewportSize.height / 2f)
                            } else Offset.Zero
                            val oldScale = scale
                            val newScale = (oldScale * 0.8f).coerceAtLeast(0.2f)
                            panOffset = center - (center - panOffset) * (newScale / oldScale)
                            scale = newScale
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Remove,
                            contentDescription = "کوچک‌نمایی بوم نردبانی",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            scale = 1.0f
                            panOffset = Offset.Zero
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Text(
                            text = "1:1",
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.semantics { contentDescription = "بازنشانی دید به مقیاس ۱۰۰٪ (1:1)" }
                        )
                    }
                    Text(
                        text = "${PersianUtils.toPersianDigits((scale * 100).toInt())}٪",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }
        }
    }
}

/**
 * مدیریت ژست لمسی دو بعدی برای اسکرول روان به تمام جهات (X & Y) و بزرگنمایی دو انگشتی
 * با استفاده از PointerEventPass.Initial تا هم اسکرول تک‌انگشتی بدون قطعی روی کارت‌ها کار کند
 * و هم تپ سریع روی المان‌ها برای تعامل کاربر فعال بماند.
 */
private suspend fun PointerInputScope.detectLadderPanAndZoom(
    onPan: (panDelta: Offset) -> Unit,
    onZoomAndPan: (centroid: Offset, panDelta: Offset, zoomFactor: Float) -> Unit
) {
    awaitEachGesture {
        var isDragging = false
        var downPos = Offset.Zero
        val touchSlop = viewConfiguration.touchSlop

        val firstDown = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        downPos = firstDown.position

        do {
            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
            val activePointers = event.changes.filter { it.pressed }

            if (activePointers.size >= 2) {
                // دو یا چند انگشت: زوم و جابه‌جایی همزمان
                val zoomChange = event.calculateZoom()
                val panChange = event.calculatePan()
                val centroid = event.calculateCentroid()

                if (zoomChange != 1f || panChange != Offset.Zero) {
                    onZoomAndPan(centroid, panChange, zoomChange)
                    event.changes.forEach { it.consume() }
                }
            } else if (activePointers.size == 1) {
                // یک انگشت: اسکرول روان به تمام جهات
                val change = activePointers.first()
                val moveDelta = change.position - change.previousPosition

                if (!isDragging) {
                    val totalDistance = (change.position - downPos).getDistance()
                    if (totalDistance > touchSlop) {
                        isDragging = true
                    }
                }

                if (isDragging) {
                    if (moveDelta != Offset.Zero) {
                        onPan(moveDelta)
                        change.consume()
                    }
                }
            }
        } while (event.changes.any { it.pressed })
    }
}

@Composable
private fun LadderRungCanvasRow(
    rung: LadderRung,
    isPlcRunning: Boolean,
    activeTool: LadderElementType?,
    onContactToggled: (String) -> Unit,
    onDeleteRung: () -> Unit,
    onMoveRungUp: () -> Unit,
    onMoveRungDown: () -> Unit,
    onAddElementClick: () -> Unit,
    onAddBranchClick: () -> Unit,
    onElementClick: (LadderElement) -> Unit,
    onPlaceSymbol: (LadderElementType, Boolean) -> Unit
) {
    val hasBranches = rung.branchContacts.isNotEmpty() || rung.parallelBranches.isNotEmpty()
    val isPlacementActive = activeTool != null
    val rungHeight = if (hasBranches || isPlacementActive) 140.dp else 100.dp

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // ۱. هدر رانگ با شماره، توضیحات و ابزارهای مدیریت رانگ با تاچ تارگت حداقل ۴۸dp
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = "Rung ${rung.rungNumber}",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (rung.comment.isNotBlank()) {
                        Text(
                            text = "// ${rung.comment}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(
                        onClick = onAddBranchClick,
                        modifier = Modifier
                            .size(48.dp)
                            .semantics { contentDescription = "افزودن شاخه موازی خودنگهدار OR به رانگ شماره ${rung.rungNumber}" }
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "+OR",
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onAddElementClick,
                        modifier = Modifier
                            .size(48.dp)
                            .semantics { contentDescription = "افزودن المان جدید به رانگ شماره ${rung.rungNumber}" }
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onMoveRungUp,
                        modifier = Modifier
                            .size(48.dp)
                            .semantics { contentDescription = "انتقال رانگ شماره ${rung.rungNumber} به بالا" }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowUpward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onMoveRungDown,
                        modifier = Modifier
                            .size(48.dp)
                            .semantics { contentDescription = "انتقال رانگ شماره ${rung.rungNumber} به پایین" }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowDownward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDeleteRung,
                        modifier = Modifier
                            .size(48.dp)
                            .semantics { contentDescription = "حذف رانگ شماره ${rung.rungNumber}" }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ۲. بوم ترسیم دیاگرام نردبانی Rung Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(rungHeight)
                    .background(Color(0xFF0C131F), RoundedCornerShape(6.dp))
                    .border(1.dp, Color(0xFF1E2838), RoundedCornerShape(6.dp))
            ) {
                // الف) رسم ریل‌های تغذیه و هادی‌های جریان روی Canvas
                val powerFlowing = rung.isPowerFlowing
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    val railColor = if (isPlcRunning) Color(0xFF00E676) else Color(0xFF455A64)
                    val neutralRail = Color(0xFF37474F)

                    // ریل چپ (Power Rail تغذیه ۲۴V / L)
                    drawLine(railColor, Offset(14f, 0f), Offset(14f, h), strokeWidth = 6f)
                    // ریل راست (Neutral / 0V Rail)
                    drawLine(neutralRail, Offset(w - 14f, 0f), Offset(w - 14f, h), strokeWidth = 6f)

                    // خط انتقال جریان افقی اصلی
                    val wireColor = if (powerFlowing && isPlcRunning) Color(0xFF00E676) else Color(0xFF455A64)
                    val mainWireY = if (hasBranches || isPlacementActive) h * 0.32f else h * 0.5f

                    // سیم اصلی سرتاسری از ریل چپ تا ریل راست
                    drawLine(wireColor, Offset(14f, mainWireY), Offset(w - 14f, mainWireY), strokeWidth = 2.5f)

                    // شاخه موازی خودنگهدار (Parallel OR branch)
                    if (hasBranches || isPlacementActive) {
                        val branchY = h * 0.78f
                        val branchLeft = 40f
                        val branchRight = w * 0.52f

                        // سیم اتصال عمودی ورودی به شاخه
                        drawLine(wireColor, Offset(branchLeft, mainWireY), Offset(branchLeft, branchY), strokeWidth = 2f)
                        // سیم افقی شاخه موازی
                        drawLine(wireColor, Offset(branchLeft, branchY), Offset(branchRight, branchY), strokeWidth = 2f)
                        // سیم اتصال عمودی بازگشت از شاخه به سیم اصلی
                        drawLine(wireColor, Offset(branchRight, branchY), Offset(branchRight, mainWireY), strokeWidth = 2f)

                        // گره‌های اتصال (Junction Dots)
                        drawCircle(wireColor, radius = 4f, center = Offset(branchLeft, mainWireY))
                        drawCircle(wireColor, radius = 4f, center = Offset(branchRight, mainWireY))
                    }
                }

                // ب) محتوای تعاملی نمادهای استاندارد PLC و اسلات‌های درج
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 24.dp, end = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ستون کنتاکت‌ها و شاخه موازی (سمت چپ/میانی رانگ)
                    Column(
                        verticalArrangement = if (hasBranches || isPlacementActive) Arrangement.spacedBy(14.dp) else Arrangement.Center,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        // ۱) ردیف اصلی کنتاکت‌ها
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (contact in rung.contacts) {
                                StandardPlcSymbolCanvas(
                                    element = contact,
                                    isRunning = isPlcRunning,
                                    onClick = {
                                        if (isPlcRunning && contact.address.startsWith("I")) {
                                            onContactToggled(contact.address)
                                        } else {
                                            onElementClick(contact)
                                        }
                                    }
                                )
                            }

                            // اسلات قرار دادن کنتاکت جدید در صورت انتخاب ابزار
                            if (activeTool != null && (activeTool == LadderElementType.NO_CONTACT || activeTool == LadderElementType.NC_CONTACT)) {
                                CanvasPlacementSlot(
                                    title = "+ ${activeTool.symbol}",
                                    color = Color(0xFF00E5FF),
                                    onClick = { onPlaceSymbol(activeTool, false) }
                                )
                            }
                        }

                        // ۲) ردیف شاخه موازی خودنگهدار OR
                        if (hasBranches || (activeTool != null && rung.branchContacts.isEmpty())) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                for (branch in rung.branchContacts) {
                                    StandardPlcSymbolCanvas(
                                        element = branch,
                                        isRunning = isPlcRunning,
                                        onClick = {
                                            if (isPlcRunning && branch.address.startsWith("I")) {
                                                onContactToggled(branch.address)
                                            } else {
                                                onElementClick(branch)
                                            }
                                        }
                                    )
                                }

                                if (activeTool != null) {
                                    CanvasPlacementSlot(
                                        title = "+ OR ${activeTool.symbol}",
                                        color = Color(0xFFFFD600),
                                        onClick = { onPlaceSymbol(activeTool, true) }
                                    )
                                }
                            }
                        }
                    }

                    // ستون بوبین‌ها و توابع خروجی (سمت راست رانگ متصل به ریل نول)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (coil in rung.coils) {
                            StandardPlcSymbolCanvas(
                                element = coil,
                                isRunning = isPlcRunning,
                                onClick = { onElementClick(coil) }
                            )
                        }

                        // اسلات قرار دادن کویل/تایمر/کانتر جدید
                        if (activeTool != null && activeTool != LadderElementType.NO_CONTACT && activeTool != LadderElementType.NC_CONTACT) {
                            CanvasPlacementSlot(
                                title = "+ ${activeTool.symbol}",
                                color = Color(0xFF69F0AE),
                                onClick = { onPlaceSymbol(activeTool, false) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * ترسیم‌گر نمادهای استاندارد IEC 61131-3 روی بوم:
 * - کنتاکت باز: —| |—
 * - کنتاکت بسته: —|/|—
 * - کویل خروجی: —( )—
 * - کویل ست: —(S)—
 * - کویل ریست: —(R)—
 * - بلوک‌های تابعی TON/TOF و CTU/CTD
 */
@Composable
fun StandardPlcSymbolCanvas(
    element: LadderElement,
    isRunning: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val active = element.isActive && isRunning
    val isFunctionBlock = element.type in listOf(
        LadderElementType.TIMER_TON,
        LadderElementType.TIMER_TOF,
        LadderElementType.COUNTER_CTU,
        LadderElementType.COUNTER_CTD,
        LadderElementType.COUNTER_CTUD,
        LadderElementType.CMP_EQ,
        LadderElementType.CMP_NE,
        LadderElementType.CMP_GT,
        LadderElementType.CMP_GE,
        LadderElementType.CMP_LT,
        LadderElementType.CMP_LE,
        LadderElementType.MATH_ADD,
        LadderElementType.MATH_SUB,
        LadderElementType.MATH_MUL,
        LadderElementType.MATH_DIV
    )

    val boxWidth = if (isFunctionBlock) 74.dp else 56.dp
    val boxHeight = if (isFunctionBlock) 54.dp else 42.dp

    val symbolColor = when {
        active -> Color(0xFF00E676)
        isFunctionBlock -> Color(0xFF00E5FF)
        else -> Color(0xFFB0BEC5)
    }

    Box(
        modifier = modifier
            .size(width = boxWidth, height = boxHeight)
            .clickable { onClick() }
            .semantics { contentDescription = "${element.type.persianTitle} با آدرس ${element.address}" }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val midY = h / 2f
            val strokeW = if (active) 3f else 2.2f

            when (element.type) {
                LadderElementType.NO_CONTACT -> {
                    // سیم ورودی چپ
                    drawLine(symbolColor, Offset(0f, midY), Offset(w * 0.35f, midY), strokeWidth = strokeW)
                    // تیغه عمودی اول
                    drawLine(symbolColor, Offset(w * 0.35f, midY - 14f), Offset(w * 0.35f, midY + 14f), strokeWidth = strokeW)
                    // تیغه عمودی دوم
                    drawLine(symbolColor, Offset(w * 0.65f, midY - 14f), Offset(w * 0.65f, midY + 14f), strokeWidth = strokeW)
                    // سیم خروجی راست
                    drawLine(symbolColor, Offset(w * 0.65f, midY), Offset(w, midY), strokeWidth = strokeW)

                    if (active) {
                        drawCircle(Color(0xFF00E676).copy(alpha = 0.3f), radius = 16f, center = Offset(w / 2f, midY))
                    }
                }

                LadderElementType.NC_CONTACT -> {
                    // سیم ورودی چپ
                    drawLine(symbolColor, Offset(0f, midY), Offset(w * 0.35f, midY), strokeWidth = strokeW)
                    // تیغه عمودی اول
                    drawLine(symbolColor, Offset(w * 0.35f, midY - 14f), Offset(w * 0.35f, midY + 14f), strokeWidth = strokeW)
                    // خط مورب قطعی استاندارد NC
                    drawLine(symbolColor, Offset(w * 0.32f, midY + 15f), Offset(w * 0.68f, midY - 15f), strokeWidth = strokeW)
                    // تیغه عمودی دوم
                    drawLine(symbolColor, Offset(w * 0.65f, midY - 14f), Offset(w * 0.65f, midY + 14f), strokeWidth = strokeW)
                    // سیم خروجی راست
                    drawLine(symbolColor, Offset(w * 0.65f, midY), Offset(w, midY), strokeWidth = strokeW)
                }

                LadderElementType.EDGE_POS, LadderElementType.EDGE_NEG -> {
                    // سیم ورودی چپ
                    drawLine(symbolColor, Offset(0f, midY), Offset(w * 0.35f, midY), strokeWidth = strokeW)
                    // تیغه عمودی اول
                    drawLine(symbolColor, Offset(w * 0.35f, midY - 14f), Offset(w * 0.35f, midY + 14f), strokeWidth = strokeW)
                    // تیغه عمودی دوم
                    drawLine(symbolColor, Offset(w * 0.65f, midY - 14f), Offset(w * 0.65f, midY + 14f), strokeWidth = strokeW)
                    // سیم خروجی راست
                    drawLine(symbolColor, Offset(w * 0.65f, midY), Offset(w, midY), strokeWidth = strokeW)

                    val edgeLetter = if (element.type == LadderElementType.EDGE_POS) "P" else "N"
                    val paint = android.graphics.Paint().apply {
                        color = symbolColor.hashCode()
                        textSize = 22f
                        isFakeBoldText = true
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                    drawContext.canvas.nativeCanvas.drawText(edgeLetter, w / 2f, midY + 8f, paint)

                    if (active) {
                        drawCircle(Color(0xFF00E676).copy(alpha = 0.3f), radius = 16f, center = Offset(w / 2f, midY))
                    }
                }

                LadderElementType.NORMAL_COIL, LadderElementType.SET_COIL, LadderElementType.RESET_COIL -> {
                    // سیم ورودی چپ
                    drawLine(symbolColor, Offset(0f, midY), Offset(w * 0.25f, midY), strokeWidth = strokeW)

                    // پرانتز چپ (کمان)
                    val arcLeftPath = Path().apply {
                        moveTo(w * 0.38f, midY - 14f)
                        cubicTo(w * 0.26f, midY - 7f, w * 0.26f, midY + 7f, w * 0.38f, midY + 14f)
                    }
                    drawPath(arcLeftPath, symbolColor, style = Stroke(width = strokeW))

                    // پرانتز راست (کمان)
                    val arcRightPath = Path().apply {
                        moveTo(w * 0.62f, midY - 14f)
                        cubicTo(w * 0.74f, midY - 7f, w * 0.74f, midY + 7f, w * 0.62f, midY + 14f)
                    }
                    drawPath(arcRightPath, symbolColor, style = Stroke(width = strokeW))

                    // سیم خروجی راست
                    drawLine(symbolColor, Offset(w * 0.75f, midY), Offset(w, midY), strokeWidth = strokeW)

                    // نمایش حرف S یا R در مرکز کویل
                    if (element.type == LadderElementType.SET_COIL || element.type == LadderElementType.RESET_COIL) {
                        val textLetter = if (element.type == LadderElementType.SET_COIL) "S" else "R"
                        val paint = android.graphics.Paint().apply {
                            color = symbolColor.hashCode()
                            textSize = 28f
                            isFakeBoldText = true
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                        drawContext.canvas.nativeCanvas.drawText(textLetter, w / 2f, midY + 10f, paint)
                    }

                    if (active) {
                        drawCircle(Color(0xFF00E676).copy(alpha = 0.25f), radius = 18f, center = Offset(w / 2f, midY))
                    }
                }

                else -> {
                    // رسم مستطیل استاندارد بلوک تابعی IEC برای تایمرها، شمارنده‌ها، مقایسه‌گرها و محاسبات ریاضی
                    val rectLeft = w * 0.12f
                    val rectTop = 4f
                    val rectW = w * 0.76f
                    val rectH = h - 8f

                    // سیم ورودی و خروجی به بلوک
                    drawLine(symbolColor, Offset(0f, midY), Offset(rectLeft, midY), strokeWidth = strokeW)
                    drawLine(symbolColor, Offset(rectLeft + rectW, midY), Offset(w, midY), strokeWidth = strokeW)

                    // بدنه بلوک
                    drawRoundRect(
                        color = if (active) Color(0xFF00E676).copy(alpha = 0.2f) else Color(0xFF162233),
                        topLeft = Offset(rectLeft, rectTop),
                        size = Size(rectW, rectH),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                    drawRoundRect(
                        color = symbolColor,
                        topLeft = Offset(rectLeft, rectTop),
                        size = Size(rectW, rectH),
                        cornerRadius = CornerRadius(6f, 6f),
                        style = Stroke(width = strokeW)
                    )

                    // نام بلوک در مرکز
                    val blockTitle = when (element.type) {
                        LadderElementType.TIMER_TON -> "TON"
                        LadderElementType.TIMER_TOF -> "TOF"
                        LadderElementType.COUNTER_CTU -> "CTU"
                        LadderElementType.COUNTER_CTD -> "CTD"
                        LadderElementType.COUNTER_CTUD -> "CTUD"
                        LadderElementType.CMP_EQ -> "=="
                        LadderElementType.CMP_NE -> "<>"
                        LadderElementType.CMP_GT -> ">"
                        LadderElementType.CMP_GE -> ">="
                        LadderElementType.CMP_LT -> "<"
                        LadderElementType.CMP_LE -> "<="
                        LadderElementType.MATH_ADD -> "ADD"
                        LadderElementType.MATH_SUB -> "SUB"
                        LadderElementType.MATH_MUL -> "MUL"
                        LadderElementType.MATH_DIV -> "DIV"
                        else -> "FB"
                    }
                    val paintTitle = android.graphics.Paint().apply {
                        color = symbolColor.hashCode()
                        textSize = 22f
                        isFakeBoldText = true
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                    drawContext.canvas.nativeCanvas.drawText(blockTitle, w / 2f, midY + 8f, paintTitle)
                }
            }
        }

        // نمایش برچسب آدرس در بالای نماد و مقادیر فعلی/تنظیمی در پایین
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = element.address,
                color = if (active) Color(0xFF00E676) else Color(0xFFECEFF1),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            Spacer(modifier = Modifier.weight(1f))

            if (isFunctionBlock) {
                Text(
                    text = "${element.currentValue}/${element.presetValue}",
                    color = Color(0xFFFFD600),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * اسلات تعاملی برای قرار دادن نماد انتخابی روی رانگ با خط‌چین
 */
@Composable
private fun CanvasPlacementSlot(
    title: String,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(width = 54.dp, height = 36.dp)
            .clickable { onClick() }
            .semantics { contentDescription = "درج نماد در این موقعیت: $title" }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(
                width = 2.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
            )
            drawRoundRect(
                color = color.copy(alpha = 0.7f),
                cornerRadius = CornerRadius(6f, 6f),
                style = stroke
            )
        }

        Text(
            text = title,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )
    }
}
