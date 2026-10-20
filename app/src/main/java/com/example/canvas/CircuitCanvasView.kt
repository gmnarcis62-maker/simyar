package com.example.canvas

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FitScreen
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.model.CircuitComponent
import com.example.model.CircuitComponent.Companion.COMPONENT_HEIGHT
import com.example.model.CircuitComponent.Companion.COMPONENT_WIDTH
import com.example.model.Terminal
import com.example.model.WireConnection
import com.example.viewmodel.AlignmentGuide
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

data class SelectedTerminal(
    val componentId: String,
    val terminal: Terminal
)

@Composable
fun CircuitCanvasView(
    components: List<CircuitComponent>,
    wires: List<WireConnection>,
    selectedComponentId: String?,
    selectedWireId: String? = null,
    isSimulationRunning: Boolean,
    isSnapToGridEnabled: Boolean = true,
    showWireNumbers: Boolean = true,
    scaleFactor: Float = 1f,
    panOffsetX: Float = 0f,
    panOffsetY: Float = 0f,
    selectedComponentIds: Set<String> = emptySet(),
    isMultiSelectMode: Boolean = false,
    isSmartGuidesEnabled: Boolean = true,
    highlightedComponentId: String? = null,
    currentSheetId: String = "sheet_1",
    visibleLayerIds: Set<String> = emptySet(),
    lockedLayerIds: Set<String> = emptySet(),
    activeGuides: List<AlignmentGuide> = emptyList(),
    onToggleMultiSelectComponent: ((String) -> Unit)? = null,
    onBoxSelectionFinished: ((Set<String>) -> Unit)? = null,
    onMoveSelectedComponentsBy: ((Float, Float) -> Unit)? = null,
    onBatchMoveFinished: (() -> Unit)? = null,
    onSnapWithGuides: ((String, Float, Float) -> Triple<Float, Float, List<AlignmentGuide>>)? = null,
    onTransformChange: (scale: Float, panX: Float, panY: Float) -> Unit = { _, _, _ -> },
    onSelectComponent: (String?) -> Unit,
    onSelectWire: (String?) -> Unit = {},
    onLongPressComponent: (String) -> Unit = {},
    onMoveComponent: (String, Float, Float) -> Unit,
    onMoveFinished: () -> Unit = {},
    onToggleComponentState: (String) -> Unit,
    onSetComponentPressed: (String, Boolean) -> Unit = { _, _ -> },
    onAddWire: (fromComp: String, fromTerm: String, toComp: String, toTerm: String) -> Unit,
    onDeleteWire: (String) -> Unit = {},
    isFullscreen: Boolean = false,
    onToggleFullscreen: (() -> Unit)? = null,
    onZoomIn: (() -> Unit)? = null, 
    onZoomOut: (() -> Unit)? = null, 
    onFitToScreen: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var pendingTerminal by remember { mutableStateOf<SelectedTerminal?>(null) }
    var draggingComponentId by remember { mutableStateOf<String?>(null) }
    var hasMovedDuringDrag by remember { mutableStateOf(false) }
    var isBatchDragging by remember { mutableStateOf(false) }
    var selectionBoxStart by remember { mutableStateOf<Offset?>(null) }
    var selectionBoxCurrent by remember { mutableStateOf<Offset?>(null) }

    val currentComponents by rememberUpdatedState(components)
    val currentWires by rememberUpdatedState(wires)
    val currentScale by rememberUpdatedState(scaleFactor)
    val currentPanX by rememberUpdatedState(panOffsetX)
    val currentPanY by rememberUpdatedState(panOffsetY)
    val isSimRunningState by rememberUpdatedState(isSimulationRunning)
    val currentSelectedIds by rememberUpdatedState(selectedComponentIds)
    val isMultiSelectModeState by rememberUpdatedState(isMultiSelectMode)
    val isSmartGuidesEnabledState by rememberUpdatedState(isSmartGuidesEnabled)
    val currentLockedLayerIds by rememberUpdatedState(lockedLayerIds)

    val infiniteTransition = rememberInfiniteTransition(label = "current_flow")
    val flowPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 40f, // طول مسیر بیشتر برای افکت بهتر
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val haloPulse by infiniteTransition.animateFloat(
        initialValue = 20f,
        targetValue = 30f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo"
    )

    val density = LocalDensity.current.density
    val safeDensity = if (density > 0.1f) density else 1f
    val minAllowedScale = CanvasFitCalculator.MIN_SCALE * safeDensity
    val maxAllowedScale = CanvasFitCalculator.MAX_SCALE * safeDensity

    val gridSize = 20f

    fun snap(value: Float): Float {
        return if (isSnapToGridEnabled) {
            (value / gridSize).roundToInt() * gridSize
        } else {
            value
        }
    }

    fun hitTestComponent(comp: CircuitComponent, canvasPoint: Offset): Boolean {
        val cx = comp.x + COMPONENT_WIDTH / 2f
        val cy = comp.y + COMPONENT_HEIGHT / 2f
        val rad = -Math.toRadians(comp.rotation.toDouble()).toFloat()
        val cosA = cos(rad)
        val sinA = sin(rad)
        val dx = canvasPoint.x - cx
        val dy = canvasPoint.y - cy
        val localX = cx + (dx * cosA - dy * sinA)
        val localY = cy + (dx * sinA + dy * cosA)

        return localX in comp.x..(comp.x + COMPONENT_WIDTH) &&
                localY in comp.y..(comp.y + COMPONENT_HEIGHT)
    }

    fun distanceToSegment(p: Offset, a: Offset, b: Offset): Float {
        val l2 = (b.x - a.x) * (b.x - a.x) + (b.y - a.y) * (b.y - a.y)
        if (l2 == 0f) return (p - a).getDistance()
        val t = ((p.x - a.x) * (b.x - a.x) + (p.y - a.y) * (b.y - a.y)) / l2
        val clampedT = t.coerceIn(0f, 1f)
        val projection = Offset(a.x + clampedT * (b.x - a.x), a.y + clampedT * (b.y - a.y))
        return (p - projection).getDistance()
    }

    val sheetComponents = currentComponents.filter {
        it.sheetId == currentSheetId && (visibleLayerIds.isEmpty() || it.layerId in visibleLayerIds)
    }
    val sheetWires = currentWires.filter {
        it.sheetId == currentSheetId && (visibleLayerIds.isEmpty() || it.layerId in visibleLayerIds)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF0F1C2E), Color(0xFF040A12)),
                    center = Offset.Unspecified,
                    radius = 2500f
                )
            )
            .semantics { contentDescription = "بوم طراحی و شبیه‌سازی مدار الکتریکی سیمیار" }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val oldScale = currentScale
                        val newScale = (oldScale * zoom).coerceIn(minAllowedScale, maxAllowedScale)
                        val newPanX = centroid.x - (centroid.x - currentPanX) * (newScale / oldScale) + pan.x
                        val newPanY = centroid.y - (centroid.y - currentPanY) * (newScale / oldScale) + pan.y
                        onTransformChange(newScale, newPanX, newPanY)
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = { rawTapOffset ->
                            val tapOffset = Offset(
                                (rawTapOffset.x - currentPanX) / currentScale,
                                (rawTapOffset.y - currentPanY) / currentScale
                            )
                            val tapped = sheetComponents.find { comp -> hitTestComponent(comp, tapOffset) }
                            if (isSimRunningState && tapped != null) {
                                val isMomentary = !tapped.isMaintained && (tapped.type in listOf(
                                    com.example.model.ComponentType.PUSH_BUTTON_NO,
                                    com.example.model.ComponentType.PUSH_BUTTON_NC,
                                    com.example.model.ComponentType.EMERGENCY_STOP
                                ))
                                if (isMomentary) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSetComponentPressed(tapped.id, true)
                                    try {
                                        tryAwaitRelease()
                                    } finally {
                                        onSetComponentPressed(tapped.id, false)
                                    }
                                } else {
                                    val released = tryAwaitRelease()
                                    if (released) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onToggleComponentState(tapped.id)
                                    }
                                }
                            }
                        },
                        onLongPress = { rawTapOffset ->
                            val tapOffset = Offset(
                                (rawTapOffset.x - currentPanX) / currentScale,
                                (rawTapOffset.y - currentPanY) / currentScale
                            )
                            val tapped = sheetComponents.find { comp -> hitTestComponent(comp, tapOffset) }
                            if (tapped != null) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSelectComponent(tapped.id)
                                onLongPressComponent(tapped.id)
                            }
                        },
                        onTap = { rawTapOffset ->
                            val tapOffset = Offset(
                                (rawTapOffset.x - currentPanX) / currentScale,
                                (rawTapOffset.y - currentPanY) / currentScale
                            )

                            if (!isSimRunningState) {
                                if (isMultiSelectModeState) {
                                    val tapped = sheetComponents.find { comp -> hitTestComponent(comp, tapOffset) }
                                    if (tapped != null) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onToggleMultiSelectComponent?.invoke(tapped.id)
                                        onSelectWire(null)
                                        pendingTerminal = null
                                    } else {
                                        onSelectComponent(null)
                                        onSelectWire(null)
                                        pendingTerminal = null
                                    }
                                    return@detectTapGestures
                                }

                                var nearestTerminal: SelectedTerminal? = null
                                val terminalRadius = CanvasFitCalculator.calculateTerminalTouchRadius(
                                    scale = currentScale,
                                    density = safeDensity,
                                    targetDpRadius = 22f
                                )
                                var minTerminalDist = terminalRadius

                                for (comp in sheetComponents) {
                                    for (term in comp.getTerminals()) {
                                        val tx = comp.x + term.relX
                                        val ty = comp.y + term.relY
                                        val dist = Offset(tapOffset.x - tx, tapOffset.y - ty).getDistance()
                                        if (dist < minTerminalDist) {
                                            minTerminalDist = dist
                                            nearestTerminal = SelectedTerminal(comp.id, term)
                                        }
                                    }
                                }

                                if (nearestTerminal != null) {
                                    val currentPending = pendingTerminal
                                    if (currentPending == null) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        pendingTerminal = nearestTerminal
                                    } else {
                                        if (currentPending.componentId != nearestTerminal.componentId) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onAddWire(
                                                currentPending.componentId,
                                                currentPending.terminal.id,
                                                nearestTerminal.componentId,
                                                nearestTerminal.terminal.id
                                            )
                                        }
                                        pendingTerminal = null
                                    }
                                    onSelectWire(null)
                                } else {
                                    val tapped = sheetComponents.find { comp -> hitTestComponent(comp, tapOffset) }
                                    if (tapped != null) {
                                        onSelectComponent(tapped.id)
                                        onSelectWire(null)
                                        pendingTerminal = null
                                    } else {
                                        var clickedWire: WireConnection? = null
                                        val wireThreshold = 18f
                                        for (wire in sheetWires) {
                                            val c1 = sheetComponents.find { it.id == wire.fromComponentId }
                                            val c2 = sheetComponents.find { it.id == wire.toComponentId }
                                            if (c1 != null && c2 != null) {
                                                val t1 = c1.getTerminals().find { it.id == wire.fromTerminalId }
                                                val t2 = c2.getTerminals().find { it.id == wire.toTerminalId }
                                                if (t1 != null && t2 != null) {
                                                    val start = Offset(c1.x + t1.relX, c1.y + t1.relY)
                                                    val end = Offset(c2.x + t2.relX, c2.y + t2.relY)
                                                    val offsets: List<Offset> = if (wire.waypoints.size >= 2) {
                                                        wire.waypoints.map { Offset(it.x, it.y) }
                                                    } else {
                                                        val midX = (start.x + end.x) / 2f
                                                        listOf(start, Offset(midX, start.y), Offset(midX, end.y), end)
                                                    }
                                                    var minD = Float.MAX_VALUE
                                                    for (i in 0 until offsets.size - 1) {
                                                        val segD = distanceToSegment(tapOffset, offsets[i], offsets[i + 1])
                                                        if (segD < minD) minD = segD
                                                    }
                                                    if (minD < wireThreshold) {
                                                        clickedWire = wire
                                                        break
                                                    }
                                                }
                                            }
                                        }

                                        if (clickedWire != null) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onSelectWire(clickedWire.id)
                                            onSelectComponent(null)
                                        } else {
                                            onSelectComponent(null)
                                            onSelectWire(null)
                                        }
                                        pendingTerminal = null
                                    }
                                }
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { rawStartOffset ->
                            if (!isSimRunningState) {
                                val startOffset = Offset(
                                    (rawStartOffset.x - currentPanX) / currentScale,
                                    (rawStartOffset.y - currentPanY) / currentScale
                                )
                                val hit = sheetComponents.find { comp ->
                                    !comp.isLocked && comp.layerId !in currentLockedLayerIds && hitTestComponent(comp, startOffset)
                                }
                                if (isMultiSelectModeState) {
                                    if (hit != null) {
                                        if (hit.id !in currentSelectedIds) {
                                            onToggleMultiSelectComponent?.invoke(hit.id)
                                        }
                                        isBatchDragging = true
                                        hasMovedDuringDrag = false
                                    } else {
                                        isBatchDragging = false
                                        selectionBoxStart = startOffset
                                        selectionBoxCurrent = startOffset
                                    }
                                } else {
                                    draggingComponentId = hit?.id
                                    hasMovedDuringDrag = false
                                    if (hit != null) {
                                        onSelectComponent(hit.id)
                                        onSelectWire(null)
                                    }
                                }
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            if (!isSimRunningState) {
                                if (isMultiSelectModeState) {
                                    if (isBatchDragging) {
                                        hasMovedDuringDrag = true
                                        val dx = dragAmount.x / currentScale
                                        val dy = dragAmount.y / currentScale
                                        onMoveSelectedComponentsBy?.invoke(dx, dy)
                                    } else if (selectionBoxStart != null) {
                                        val curr = selectionBoxCurrent ?: selectionBoxStart!!
                                        selectionBoxCurrent = Offset(
                                            curr.x + dragAmount.x / currentScale,
                                            curr.y + dragAmount.y / currentScale
                                        )
                                    }
                                } else {
                                    if (draggingComponentId != null) {
                                        val comp = sheetComponents.find { it.id == draggingComponentId }
                                        if (comp != null && !comp.isLocked && comp.layerId !in currentLockedLayerIds) {
                                            val newRawX = comp.x + (dragAmount.x / currentScale)
                                            val newRawY = comp.y + (dragAmount.y / currentScale)
                                            val (snappedX, snappedY) = if (isSmartGuidesEnabledState && onSnapWithGuides != null) {
                                                val (sx, sy, _) = onSnapWithGuides(comp.id, newRawX, newRawY)
                                                Pair(sx.coerceAtLeast(10f), sy.coerceAtLeast(10f))
                                            } else {
                                                val nx = snap(newRawX).coerceAtLeast(10f)
                                                val ny = snap(newRawY).coerceAtLeast(10f)
                                                Pair(nx, ny)
                                            }
                                            hasMovedDuringDrag = true
                                            onMoveComponent(draggingComponentId!!, snappedX, snappedY)
                                        }
                                    } else {
                                        onTransformChange(
                                            currentScale,
                                            currentPanX + dragAmount.x,
                                            currentPanY + dragAmount.y
                                        )
                                    }
                                }
                            } else {
                                onTransformChange(
                                    currentScale,
                                    currentPanX + dragAmount.x,
                                    currentPanY + dragAmount.y
                                )
                            }
                        },
                        onDragEnd = {
                            if (isMultiSelectModeState) {
                                if (isBatchDragging && hasMovedDuringDrag) {
                                    onBatchMoveFinished?.invoke()
                                } else if (selectionBoxStart != null && selectionBoxCurrent != null) {
                                    val bStart = selectionBoxStart!!
                                    val bCurr = selectionBoxCurrent!!
                                    val boxLeft = min(bStart.x, bCurr.x)
                                    val boxTop = min(bStart.y, bCurr.y)
                                    val boxRight = kotlin.math.max(bStart.x, bCurr.x)
                                    val boxBottom = kotlin.math.max(bStart.y, bCurr.y)

                                    val boxedIds = sheetComponents.filter { comp ->
                                        val cRight = comp.x + COMPONENT_WIDTH
                                        val cBottom = comp.y + COMPONENT_HEIGHT
                                        comp.x < boxRight && cRight > boxLeft && comp.y < boxBottom && cBottom > boxTop
                                    }.map { it.id }.toSet()

                                    onBoxSelectionFinished?.invoke(boxedIds)
                                }
                                selectionBoxStart = null
                                selectionBoxCurrent = null
                                isBatchDragging = false
                                hasMovedDuringDrag = false
                            } else {
                                if (draggingComponentId != null && hasMovedDuringDrag) {
                                    onMoveFinished()
                                }
                                draggingComponentId = null
                                hasMovedDuringDrag = false
                            }
                        },
                        onDragCancel = {
                            selectionBoxStart = null
                            selectionBoxCurrent = null
                            isBatchDragging = false
                            draggingComponentId = null
                            hasMovedDuringDrag = false
                        }
                    )
                }
        ) {
            translate(panOffsetX, panOffsetY) {
                scale(scaleFactor, pivot = Offset.Zero) {

                    // ⭐️ ۱. گرید حرفه‌ای مهندسی (Blueprint Style)
                    val majorGridStep = 100f
                    val minorGridStep = 20f
                    val majorGridColor = Color(0x2500E5FF)
                    val minorGridColor = Color(0x0AFFFFFF)

                    val visibleLeft = ((-panOffsetX) / scaleFactor) - 100f
                    val visibleTop = ((-panOffsetY) / scaleFactor) - 100f
                    val visibleRight = ((size.width - panOffsetX) / scaleFactor) + 100f
                    val visibleBottom = ((size.height - panOffsetY) / scaleFactor) + 100f

                    // رسم نقاط ریز
                    val startXMinor = (visibleLeft / minorGridStep).toInt() * minorGridStep
                    val startYMinor = (visibleTop / minorGridStep).toInt() * minorGridStep
                    var gxMin = startXMinor
                    while (gxMin <= visibleRight) {
                        var gyMin = startYMinor
                        while (gyMin <= visibleBottom) {
                            drawCircle(color = minorGridColor, radius = 1.5f, center = Offset(gxMin, gyMin))
                            gyMin += minorGridStep
                        }
                        gxMin += minorGridStep
                    }

                    // رسم خطوط اصلی
                    val startXMajor = (visibleLeft / majorGridStep).toInt() * majorGridStep
                    val startYMajor = (visibleTop / majorGridStep).toInt() * majorGridStep
                    var gxMaj = startXMajor
                    while (gxMaj <= visibleRight) {
                        drawLine(majorGridColor, Offset(gxMaj, visibleTop), Offset(gxMaj, visibleBottom), strokeWidth = 1f)
                        gxMaj += majorGridStep
                    }
                    var gyMaj = startYMajor
                    while (gyMaj <= visibleBottom) {
                        drawLine(majorGridColor, Offset(visibleLeft, gyMaj), Offset(visibleRight, gyMaj), strokeWidth = 1f)
                        gyMaj += majorGridStep
                    }

                    // ⭐️ ۲. سیم‌ها با افکت نئون و سایه (Glow & Shadow)
                    for (wire in sheetWires) {
                        val fromComp = sheetComponents.find { it.id == wire.fromComponentId }
                        val toComp = sheetComponents.find { it.id == wire.toComponentId }
                        if (fromComp != null && toComp != null) {
                            val fromTerm = fromComp.getTerminals().find { it.id == wire.fromTerminalId }
                            val toTerm = toComp.getTerminals().find { it.id == wire.toTerminalId }

                            if (fromTerm != null && toTerm != null) {
                                val start = Offset(fromComp.x + fromTerm.relX, fromComp.y + fromTerm.relY)
                                val end = Offset(toComp.x + toTerm.relX, toComp.y + toTerm.relY)

                                val isLive = isSimulationRunning && wire.isEnergized
                                val isSelectedWire = wire.id == selectedWireId

                                val parsedWireColor = try {
                                    Color(android.graphics.Color.parseColor(wire.wireColorHex))
                                } catch (e: Exception) {
                                    Color(0xFF00E5FF)
                                }

                                val wireColor = when {
                                    isSelectedWire -> Color(0xFFFF9100)
                                    isLive -> Color(0xFF00E676)
                                    else -> parsedWireColor
                                }

                                val wireOffsets: List<Offset> = if (wire.waypoints.size >= 2) {
                                    wire.waypoints.map { Offset(it.x, it.y) }
                                } else {
                                    val midX = (start.x + end.x) / 2f
                                    listOf(start, Offset(midX, start.y), Offset(midX, end.y), end)
                                }

                                val path = Path().apply {
                                    moveTo(wireOffsets.first().x, wireOffsets.first().y)
                                    for (i in 1 until wireOffsets.size) {
                                        lineTo(wireOffsets[i].x, wireOffsets[i].y)
                                    }
                                }

                                // سایه زیر سیم
                                drawPath(
                                    path = path,
                                    color = Color(0x66000000),
                                    style = Stroke(width = if (isSelectedWire) 7f else 5f, cap = StrokeCap.Round)
                                )

                                if (isLive) {
                                    drawPath(
                                        path = path,
                                        color = Color(0x4000E676),
                                        style = Stroke(width = 12f, cap = StrokeCap.Round)
                                    )
                                    drawPath(
                                        path = path,
                                        color = Color(0x8000E676),
                                        style = Stroke(width = 6f, cap = StrokeCap.Round)
                                    )
                                    drawPath(
                                        path = path,
                                        color = Color.White,
                                        style = Stroke(
                                            width = 2f,
                                            cap = StrokeCap.Round,
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 15f), flowPhase)
                                        )
                                    )
                                }

                                drawPath(
                                    path = path,
                                    color = wireColor,
                                    style = Stroke(width = if (isSelectedWire) 5f else 3f, cap = StrokeCap.Round)
                                )

                                drawCircle(color = wireColor, radius = if (isSelectedWire) 7f else 5f, center = start)
                                drawCircle(color = Color.White.copy(alpha = 0.8f), radius = if (isSelectedWire) 3f else 2f, center = start)
                                drawCircle(color = wireColor, radius = if (isSelectedWire) 7f else 5f, center = end)
                                drawCircle(color = Color.White.copy(alpha = 0.8f), radius = if (isSelectedWire) 3f else 2f, center = end)

                                if (showWireNumbers && wire.wireNumber.isNotBlank() && wireOffsets.size >= 2) {
                                    val midIdx = wireOffsets.size / 2
                                    val pillCenter = if (wireOffsets.size % 2 == 1) {
                                        wireOffsets[midIdx]
                                    } else {
                                        Offset(
                                            (wireOffsets[midIdx - 1].x + wireOffsets[midIdx].x) / 2f,
                                            (wireOffsets[midIdx - 1].y + wireOffsets[midIdx].y) / 2f
                                        )
                                    }

                                    drawIntoCanvas { canvas ->
                                        val text = wire.wireNumber
                                        val textPaint = Paint().apply {
                                            color = android.graphics.Color.WHITE
                                            textSize = 10f
                                            typeface = Typeface.DEFAULT_BOLD
                                            textAlign = Paint.Align.CENTER
                                            isAntiAlias = true
                                        }
                                        val textBounds = android.graphics.Rect()
                                        textPaint.getTextBounds(text, 0, text.length, textBounds)
                                        val pillWidth = maxOf(textBounds.width() + 12f, 20f)
                                        val pillHeight = 14f

                                        val bgPaint = Paint().apply {
                                            color = android.graphics.Color.parseColor("#0F172A")
                                            style = Paint.Style.FILL
                                            isAntiAlias = true
                                        }
                                        val borderPaint = Paint().apply {
                                            color = if (isSelectedWire) android.graphics.Color.parseColor("#FF9100") else android.graphics.Color.parseColor("#00E5FF")
                                            style = Paint.Style.STROKE
                                            strokeWidth = 1f
                                            isAntiAlias = true
                                        }

                                        val left = pillCenter.x - pillWidth / 2f
                                        val top = pillCenter.y - pillHeight / 2f
                                        val right = pillCenter.x + pillWidth / 2f
                                        val bottom = pillCenter.y + pillHeight / 2f

                                        val rectF = android.graphics.RectF(left, top, right, bottom)
                                        canvas.nativeCanvas.drawRoundRect(rectF, 4f, 4f, bgPaint)
                                        canvas.nativeCanvas.drawRoundRect(rectF, 4f, 4f, borderPaint)

                                        val textBaseline = pillCenter.y + (textBounds.height() / 2f) - 1f
                                        canvas.nativeCanvas.drawText(text, pillCenter.x, textBaseline, textPaint)
                                    }
                                }
                            }
                        }
                    }

                    for (guide in activeGuides) {
                        val guideColor = Color(0xFFFF4081)
                        if (guide.isVertical) {
                            drawLine(
                                color = guideColor,
                                start = Offset(guide.position, guide.startCoord),
                                end = Offset(guide.position, guide.endCoord),
                                strokeWidth = 1f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                            )
                        } else {
                            drawLine(
                                color = guideColor,
                                start = Offset(guide.startCoord, guide.position),
                                end = Offset(guide.endCoord, guide.position),
                                strokeWidth = 1f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                            )
                        }
                    }

                    pendingTerminal?.let { pending ->
                        val pComp = sheetComponents.find { it.id == pending.componentId }
                        if (pComp != null) {
                            val pTerm = pComp.getTerminals().find { it.id == pending.terminal.id }
                            if (pTerm != null) {
                                val start = Offset(pComp.x + pTerm.relX, pComp.y + pTerm.relY)
                                drawCircle(
                                    color = Color(0x33FFD600),
                                    radius = haloPulse,
                                    center = start
                                )
                                drawCircle(
                                    color = Color(0xFFFFD600),
                                    radius = 10f,
                                    center = start,
                                    style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)))
                                )
                            }
                        }
                    }

                    // ⭐️ ۳. قطعات با سایه 3D
                    for (comp in sheetComponents) {
                        val activeTermId = if (pendingTerminal?.componentId == comp.id) pendingTerminal?.terminal?.id else null
                        val isCompSelected = (comp.id == selectedComponentId) || (comp.id in currentSelectedIds)

                        drawRoundRect(
                            color = Color(0x44000000),
                            topLeft = Offset(comp.x - 2f, comp.y - 2f),
                            size = Size(COMPONENT_WIDTH + 4f, COMPONENT_HEIGHT + 4f),
                            cornerRadius = CornerRadius(8f, 8f)
                        )

                        CircuitComponentDrawer.drawComponent(
                            scope = this,
                            comp = comp,
                            isSelected = isCompSelected,
                            activeTerminalId = activeTermId,
                            currentScale = currentScale,
                            density = safeDensity
                        )

                        if (comp.id == highlightedComponentId) {
                            drawRoundRect(
                                color = Color(0xFFFFD600),
                                topLeft = Offset(comp.x - 6f, comp.y - 6f),
                                size = Size(COMPONENT_WIDTH + 12f, COMPONENT_HEIGHT + 12f),
                                cornerRadius = CornerRadius(10f, 10f),
                                style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f)))
                            )
                        }
                    }

                    // ⭐️ ۴. کادر انتخاب حرفه‌ای
                    if (selectionBoxStart != null && selectionBoxCurrent != null) {
                        val bStart = selectionBoxStart!!
                        val bCurr = selectionBoxCurrent!!
                        val left = min(bStart.x, bCurr.x)
                        val top = min(bStart.y, bCurr.y)
                        val width = kotlin.math.abs(bCurr.x - bStart.x)
                        val height = kotlin.math.abs(bCurr.y - bStart.y)

                        drawRect(
                            color = Color(0x2000E5FF),
                            topLeft = Offset(left, top),
                            size = Size(width, height)
                        )
                        drawRect(
                            color = Color(0xFF00E5FF),
                            topLeft = Offset(left, top),
                            size = Size(width, height),
                            style = Stroke(width = 1.5f)
                        )
                        drawCircle(color = Color.White, radius = 3f, center = Offset(left, top))
                        drawCircle(color = Color.White, radius = 3f, center = Offset(left + width, top))
                        drawCircle(color = Color.White, radius = 3f, center = Offset(left, top + height))
                        drawCircle(color = Color.White, radius = 3f, center = Offset(left + width, top + height))
                    }
                }
            }
        }

        // ⭐️ ۵. پنل شیشه‌ای شناور (Glassmorphism HUD)
        Surface(
            color = Color.White.copy(alpha = 0.05f),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onToggleFullscreen != null) {
                    IconButton(
                        onClick = onToggleFullscreen,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isFullscreen) Icons.Outlined.FullscreenExit else Icons.Outlined.Fullscreen,
                            contentDescription = if (isFullscreen) "خروج از تمام صفحه" else "تمام صفحه شدن بوم",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                if (onFitToScreen != null) {
                    IconButton(
                        onClick = onFitToScreen,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FitScreen,
                            contentDescription = "نمایش کل مدار (Fit)",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}