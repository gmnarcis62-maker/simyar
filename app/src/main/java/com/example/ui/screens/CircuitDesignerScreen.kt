package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ElectricalServices
import androidx.compose.material.icons.outlined.FitScreen
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Redo
import androidx.compose.material.icons.outlined.RotateRight
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CopyAll
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FormatAlignCenter
import androidx.compose.material.icons.outlined.FormatAlignLeft
import androidx.compose.material.icons.outlined.FormatAlignRight
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.VerticalAlignBottom
import androidx.compose.material.icons.outlined.VerticalAlignCenter
import androidx.compose.material.icons.outlined.VerticalAlignTop
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.AltRoute
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material.icons.outlined.ZoomOut
import com.example.viewmodel.AlignmentGuide
import com.example.model.CircuitLayer
import com.example.model.CircuitSheet
import com.example.util.CableListItem
import com.example.util.ExportUtils
import com.example.util.NetNumberingManager
import com.example.viewmodel.AlignmentType
import com.example.viewmodel.DistributionType
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.canvas.CircuitCanvasView
import com.example.model.ComponentType
import com.example.ui.components.ComponentQuickActionMenu
import com.example.ui.components.IndustrialLibraryBottomSheet
import com.example.ui.components.LoadProjectDialog
import com.example.ui.components.VipUpgradeDialog
import com.example.ui.timing.SignalTimingDialog
import androidx.compose.material.icons.filled.Timeline
import com.example.util.PersianUtils
import com.example.viewmodel.SimYarViewModel

enum class ToolbarTab(val title: String) {
    EDIT("ویرایش"),
    VIEW("نمایش"),
    SIMULATION("شبیه‌سازی")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircuitDesignerScreen(
    viewModel: SimYarViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val components by viewModel.components.collectAsState()
    val wires by viewModel.wires.collectAsState()
    val selectedId by viewModel.selectedComponentId.collectAsState()
    val selectedWireId by viewModel.selectedWireId.collectAsState()
    val isSimRunning by viewModel.isCircuitSimRunning.collectAsState()
    val statusMsg by viewModel.simulationStatusMessage.collectAsState()
    val projectTitle by viewModel.currentProjectTitle.collectAsState()
    val savedProjects by viewModel.savedProjects.collectAsState()
    val isVipUnlocked by viewModel.billingRepository.isVipUnlocked.collectAsState()

    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    val canvasScale by viewModel.canvasScale.collectAsState()
    val canvasPanX by viewModel.canvasPanX.collectAsState()
    val canvasPanY by viewModel.canvasPanY.collectAsState()
    val isSnapToGrid by viewModel.isSnapToGrid.collectAsState()
    val simSpeed by viewModel.simulationSpeedMultiplier.collectAsState()
    val isDirty by viewModel.isDirty.collectAsState()
    val pendingUnsavedAction by viewModel.pendingUnsavedAction.collectAsState()
    val draftRecoveryPrompt by viewModel.draftRecoveryPrompt.collectAsState()
    val showWireNumbers by viewModel.showWireNumbers.collectAsState()

    val sheets by viewModel.sheets.collectAsState()
    val currentSheetId by viewModel.currentSheetId.collectAsState()
    val layers by viewModel.layers.collectAsState()
    val isMultiSelectMode by viewModel.isMultiSelectMode.collectAsState()
    val selectedComponentIds by viewModel.selectedComponentIds.collectAsState()
    val isSmartGuidesEnabled by viewModel.isSmartGuidesEnabled.collectAsState()
    val activeGuides by viewModel.activeGuides.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val highlightedComponentId by viewModel.highlightedComponentId.collectAsState()
    val projectLoadValidationWarning by viewModel.projectLoadValidationWarning.collectAsState()
    val projectLoadValidationIssues by viewModel.projectLoadValidationIssues.collectAsState()

    var activeToolbarTab by remember { mutableStateOf(ToolbarTab.EDIT) }
    var showLibrarySheet by remember { mutableStateOf(false) }
    var showCableListDialog by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showLoadDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showVipDialog by remember { mutableStateOf(false) }
    var showTemplatesMenu by remember { mutableStateOf(false) }
    var showQuickContextMenu by remember { mutableStateOf(false) }
    var showLayersDialog by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var showRenameSheetDialog by remember { mutableStateOf<CircuitSheet?>(null) }
    var showDeleteSheetConfirm by remember { mutableStateOf<CircuitSheet?>(null) }
    var showSheetOptionsSheetId by remember { mutableStateOf<String?>(null) }
    var renameSheetInput by remember { mutableStateOf("") }
    var showAlignmentMenu by remember { mutableStateOf(false) }
    var saveTitleInput by remember { mutableStateOf(projectTitle) }
    var saveDescInput by remember { mutableStateOf("") }
    var isFullscreenMode by remember { mutableStateOf(false) }
    var showTimingDialog by remember { mutableStateOf(false) }

    val selectedComponent = components.find { it.id == selectedId }

    // هشدار اعتبارسنجی پس از بارگذاری پروژه؛ فقط اطلاع‌رسانی است و داده‌ها را تغییر نمی‌دهد.
    if (projectLoadValidationWarning != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissProjectLoadValidationWarning() },
            title = { Text("بررسی اتصال‌های پروژه") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(projectLoadValidationWarning.orEmpty())
                    if (projectLoadValidationIssues.isNotEmpty()) {
                        Text(
                            text = "جزئیات موارد شناسایی‌شده:",
                            style = MaterialTheme.typography.titleSmall
                        )
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 240.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(projectLoadValidationIssues) { issue ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                                        Text(issue.message, style = MaterialTheme.typography.bodyMedium)
                                        val reference = listOfNotNull(
                                            issue.wireId?.let { "سیم: $it" },
                                            issue.componentId?.let { "قطعه: $it" }
                                        ).joinToString(" • ")
                                        if (reference.isNotBlank()) {
                                            Text(
                                                reference,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissProjectLoadValidationWarning() }) {
                    Text("متوجه شدم")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isTabletLandscape = maxWidth > 680.dp

        Column(modifier = Modifier.fillMaxSize()) {
            if (!isFullscreenMode) {
                // ==========================================
                // ⭐️ نوار بالایی کنترل و پروژه‌ها (فشرده‌شده برای افزایش فضای بوم)
                // ==========================================
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = projectTitle,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    if (isDirty) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Box(modifier = Modifier.size(6.dp).background(Color(0xFFFF9100), CircleShape))
                                    }
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(0.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { showExportDialog = true },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Outlined.PictureAsPdf, contentDescription = "خروجی", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = {
                                        if (!isVipUnlocked && !viewModel.billingRepository.canCreateNewProject(savedProjects.size)) {
                                            showVipDialog = true
                                        } else {
                                            saveTitleInput = projectTitle
                                            showSaveDialog = true
                                        }
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Outlined.Save, contentDescription = "ذخیره", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = { showLoadDialog = true },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Outlined.FolderOpen, contentDescription = "پروژه‌ها", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                                }
                                Box {
                                    IconButton(
                                        onClick = { showTemplatesMenu = true },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Outlined.ElectricalServices, contentDescription = "قالب‌ها", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                                    }
                                    DropdownMenu(
                                        expanded = showTemplatesMenu,
                                        onDismissRequest = { showTemplatesMenu = false },
                                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("پروژه جدید", fontSize = 12.sp) },
                                            onClick = { viewModel.newProject(); showTemplatesMenu = false }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("راه‌اندازی مستقیم (DOL)", fontSize = 12.sp) },
                                            onClick = { viewModel.checkUnsavedChangesBeforeAction { viewModel.loadTemplateDol() }; showTemplatesMenu = false }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("ستاره - مثلث", fontSize = 12.sp) },
                                            onClick = { viewModel.checkUnsavedChangesBeforeAction { viewModel.loadTemplateStarDelta() }; showTemplatesMenu = false }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("چپگرد / راستگرد", fontSize = 12.sp) },
                                            onClick = { viewModel.checkUnsavedChangesBeforeAction { viewModel.loadTemplateReverse() }; showTemplatesMenu = false }
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                ToolbarTab.values().forEach { tab ->
                                    val isSelected = activeToolbarTab == tab
                                    Box(
                                        modifier = Modifier
                                            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                                            .clickable { activeToolbarTab = tab }
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(tab.title, color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()).padding(start = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(0.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                when (activeToolbarTab) {
                                    ToolbarTab.EDIT -> {
                                        IconButton(onClick = { viewModel.undo() }, enabled = canUndo, modifier = Modifier.size(28.dp)) { Icon(Icons.Outlined.Undo, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp)) }
                                        IconButton(onClick = { viewModel.redo() }, enabled = canRedo, modifier = Modifier.size(28.dp)) { Icon(Icons.Outlined.Redo, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp)) }
                                        IconButton(onClick = { viewModel.copySelectedComponent() }, enabled = selectedId != null, modifier = Modifier.size(28.dp)) { Icon(Icons.Outlined.ContentCopy, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp)) }
                                        IconButton(onClick = { viewModel.pasteComponent() }, modifier = Modifier.size(28.dp)) { Icon(Icons.Outlined.ContentPaste, contentDescription = null, tint = Color(0xFF00E676), modifier = Modifier.size(16.dp)) }
                                        IconButton(onClick = { viewModel.rotateSelectedComponent() }, enabled = selectedId != null, modifier = Modifier.size(28.dp)) { Icon(Icons.Outlined.RotateRight, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp)) }
                                        IconButton(onClick = { viewModel.toggleLockSelectedComponent() }, enabled = selectedId != null, modifier = Modifier.size(28.dp)) { Icon(if (selectedComponent?.isLocked == true) Icons.Outlined.Lock else Icons.Outlined.LockOpen, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp)) }
                                        IconButton(onClick = { viewModel.toggleMultiSelectMode() }, modifier = Modifier.size(28.dp)) { Icon(Icons.Outlined.SelectAll, contentDescription = null, tint = if (isMultiSelectMode) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp)) }
                                        if (selectedId != null || selectedWireId != null) {
                                            IconButton(onClick = { if (selectedId != null) viewModel.deleteSelectedComponent() else viewModel.deleteSelectedWire() }, modifier = Modifier.size(28.dp)) { Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp)) }
                                        }
                                    }
                                    ToolbarTab.VIEW -> {
                                        IconButton(onClick = { viewModel.fitToScreen() }, modifier = Modifier.size(28.dp)) { Icon(Icons.Outlined.FitScreen, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp)) }
                                        IconButton(onClick = { viewModel.toggleSnapToGrid() }, modifier = Modifier.size(28.dp)) { Icon(Icons.Outlined.GridOn, contentDescription = null, tint = if (isSnapToGrid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.size(16.dp)) }
                                        IconButton(onClick = { viewModel.toggleSmartGuides() }, modifier = Modifier.size(28.dp)) { Icon(Icons.Outlined.Straighten, contentDescription = null, tint = if (isSmartGuidesEnabled) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.size(16.dp)) }
                                        IconButton(onClick = { isFullscreenMode = !isFullscreenMode }, modifier = Modifier.size(28.dp)) { Icon(if (isFullscreenMode) Icons.Outlined.FullscreenExit else Icons.Outlined.Fullscreen, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp)) }
                                        IconButton(onClick = { viewModel.toggleShowWireNumbers() }, modifier = Modifier.size(28.dp)) { Icon(Icons.Outlined.Numbers, contentDescription = null, tint = if (showWireNumbers) Color(0xFF00E676) else MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.size(16.dp)) }
                                        IconButton(onClick = { showCableListDialog = true }, modifier = Modifier.size(28.dp)) { Icon(Icons.Outlined.FormatListNumbered, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp)) }
                                    }
                                    ToolbarTab.SIMULATION -> {
                                        IconButton(onClick = { viewModel.executeSingleStepSimulation() }, modifier = Modifier.size(28.dp)) { Icon(Icons.Outlined.SkipNext, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp)) }
                                        IconButton(onClick = { viewModel.refreshAvailableTimingSignals(); showTimingDialog = true }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Timeline, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp)) }
                                        Text(statusMsg, color = if (isSimRunning) Color(0xFF00E676) else MaterialTheme.colorScheme.tertiary, fontSize = 9.sp, maxLines = 1, modifier = Modifier.padding(start = 4.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!isFullscreenMode) {
                // نوار برگه‌ها (Sheets Bar) و ابزارهای سریع (لایه‌ها، جستجو، حالت انتخاب)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // فهرست برگه‌ها
                        LazyRow(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(sheets) { sheet ->
                                val isSelected = sheet.id == currentSheetId
                                Surface(
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(6.dp),
                                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                                    modifier = Modifier
                                        .clickable {
                                            if (isSelected) {
                                                showSheetOptionsSheetId = sheet.id
                                            } else {
                                                viewModel.selectSheet(sheet.id)
                                            }
                                        }
                                        .semantics { contentDescription = "برگه ${sheet.name}" }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = sheet.name,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 10.sp
                                        )
                                        if (isSelected) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                Icons.Outlined.MoreVert,
                                                contentDescription = "گزینه‌های برگه",
                                                tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            item {
                                IconButton(
                                    onClick = { viewModel.addSheet() },
                                    modifier = Modifier
                                        .size(24.dp)
                                        .semantics { contentDescription = "افزودن برگه جدید به مدار" }
                                ) {
                                    Icon(Icons.Outlined.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                }
                            }
                        }

                        // ابزارهای سریع: جستجوی بوم، مدیریت لایه‌ها، انتخاب چندتایی
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { showSearchDialog = true },
                                modifier = Modifier
                                    .size(24.dp)
                                    .semantics { contentDescription = "جستجوی قطعه روی بوم" }
                            ) {
                                Icon(Icons.Outlined.Search, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(14.dp))
                            }

                            IconButton(
                                onClick = { showLayersDialog = true },
                                modifier = Modifier
                                    .size(24.dp)
                                    .semantics { contentDescription = "مدیریت لایه‌ها" }
                            ) {
                                Icon(Icons.Outlined.Layers, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                            }

                            Surface(
                                color = if (isMultiSelectMode) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(4.dp),
                                border = if (isMultiSelectMode) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary) else null,
                                modifier = Modifier
                                    .clickable { viewModel.toggleMultiSelectMode() }
                                    .semantics { contentDescription = if (isMultiSelectMode) "خروج از انتخاب چندتایی" else "فعال‌سازی انتخاب چندتایی" }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.SelectAll,
                                        contentDescription = null,
                                        tint = if (isMultiSelectMode) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (isMultiSelectMode && selectedComponentIds.isNotEmpty())
                                            "انتخاب (${PersianUtils.toPersianDigits(selectedComponentIds.size)})"
                                        else
                                            "انتخاب",
                                        color = if (isMultiSelectMode) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // بنر عملیات گروهی در حالت انتخاب چندتایی
                if (isMultiSelectMode) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 4.dp,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.selectAllInCurrentSheet() },
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text("انتخاب همه", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp)
                            }

                            Box {
                                Button(
                                    onClick = { showAlignmentMenu = true },
                                    enabled = selectedComponentIds.size >= 2,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(4.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 1.dp)
                                ) {
                                    Text("هم‌ترازی", color = MaterialTheme.colorScheme.onPrimary, fontSize = 10.sp)
                                }
                                DropdownMenu(
                                    expanded = showAlignmentMenu,
                                    onDismissRequest = { showAlignmentMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("هم‌ترازی چپ") },
                                        leadingIcon = { Icon(Icons.Outlined.FormatAlignLeft, contentDescription = null) },
                                        onClick = {
                                            viewModel.alignSelectedComponents(AlignmentType.LEFT)
                                            showAlignmentMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("هم‌ترازی مرکز افقی") },
                                        leadingIcon = { Icon(Icons.Outlined.FormatAlignCenter, contentDescription = null) },
                                        onClick = {
                                            viewModel.alignSelectedComponents(AlignmentType.CENTER_X)
                                            showAlignmentMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("هم‌ترازی راست") },
                                        leadingIcon = { Icon(Icons.Outlined.FormatAlignRight, contentDescription = null) },
                                        onClick = {
                                            viewModel.alignSelectedComponents(AlignmentType.RIGHT)
                                            showAlignmentMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("هم‌ترازی بالا") },
                                        leadingIcon = { Icon(Icons.Outlined.VerticalAlignTop, contentDescription = null) },
                                        onClick = {
                                            viewModel.alignSelectedComponents(AlignmentType.TOP)
                                            showAlignmentMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("هم‌ترازی وسط عمودی") },
                                        leadingIcon = { Icon(Icons.Outlined.VerticalAlignCenter, contentDescription = null) },
                                        onClick = {
                                            viewModel.alignSelectedComponents(AlignmentType.MIDDLE_Y)
                                            showAlignmentMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("هم‌ترازی پایین") },
                                        leadingIcon = { Icon(Icons.Outlined.VerticalAlignBottom, contentDescription = null) },
                                        onClick = {
                                            viewModel.alignSelectedComponents(AlignmentType.BOTTOM)
                                            showAlignmentMenu = false
                                        }
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = { viewModel.distributeSelectedComponents(DistributionType.HORIZONTAL) },
                                enabled = selectedComponentIds.size >= 3,
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text("توزیع افقی", color = MaterialTheme.colorScheme.tertiary, fontSize = 10.sp)
                            }
                            OutlinedButton(
                                onClick = { viewModel.distributeSelectedComponents(DistributionType.VERTICAL) },
                                enabled = selectedComponentIds.size >= 3,
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text("توزیع عمودی", color = MaterialTheme.colorScheme.tertiary, fontSize = 10.sp)
                            }

                            IconButton(
                                onClick = { viewModel.copySelectedComponents() },
                                enabled = selectedComponentIds.isNotEmpty(),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Outlined.CopyAll, contentDescription = "کپی گروهی", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            }
                            IconButton(
                                onClick = { viewModel.pasteComponents() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Outlined.ContentPaste, contentDescription = "جای‌گذاری گروهی", tint = Color(0xFF00E676), modifier = Modifier.size(16.dp))
                            }
                            IconButton(
                                onClick = { viewModel.rotateSelectedComponents() },
                                enabled = selectedComponentIds.isNotEmpty(),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Outlined.RotateRight, contentDescription = "چرخش گروهی", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                            }
                            IconButton(
                                onClick = { viewModel.toggleLockSelectedComponents() },
                                enabled = selectedComponentIds.isNotEmpty(),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Outlined.Lock, contentDescription = "قفل/باز گروهی", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                            }
                            IconButton(
                                onClick = { viewModel.deleteSelectedBatch() },
                                enabled = selectedComponentIds.isNotEmpty(),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Outlined.Delete, contentDescription = "حذف گروهی", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            }

                            IconButton(
                                onClick = { viewModel.setMultiSelectMode(false) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Outlined.Close, contentDescription = "خروج از انتخاب چندتایی", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            // بدنه اصلی بوم مدار
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // بوم مدار با دریافت اندازه واقعی و تراکم پیکسلی
                    val localDensity = LocalDensity.current.density
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .onSizeChanged { intSize ->
                                if (intSize.width > 0 && intSize.height > 0) {
                                    viewModel.setViewportSize(
                                        width = intSize.width.toFloat(),
                                        height = intSize.height.toFloat(),
                                        density = localDensity
                                    )
                                }
                            }
                    ) {
                        CircuitCanvasView(
                            components = components,
                            wires = wires,
                            selectedComponentId = selectedId,
                            selectedWireId = selectedWireId,
                            isSimulationRunning = isSimRunning,
                            isSnapToGridEnabled = isSnapToGrid,
                            showWireNumbers = showWireNumbers,
                            scaleFactor = canvasScale,
                            panOffsetX = canvasPanX,
                            panOffsetY = canvasPanY,
                            selectedComponentIds = selectedComponentIds,
                            isMultiSelectMode = isMultiSelectMode,
                            isSmartGuidesEnabled = isSmartGuidesEnabled,
                            highlightedComponentId = highlightedComponentId,
                            currentSheetId = currentSheetId,
                            visibleLayerIds = layers.filter { it.isVisible }.map { it.id }.toSet(),
                            lockedLayerIds = layers.filter { it.isLocked }.map { it.id }.toSet(),
                            activeGuides = activeGuides,
                            onToggleMultiSelectComponent = { id -> viewModel.toggleComponentSelectionInMultiSelect(id) },
                            onBoxSelectionFinished = { ids -> viewModel.setMultiSelection(ids) },
                            onMoveSelectedComponentsBy = { dx, dy -> viewModel.moveSelectedComponentsBy(dx, dy) },
                            onBatchMoveFinished = { viewModel.recordBatchMoveFinished() },
                            onSnapWithGuides = { id, x, y -> viewModel.snapWithSmartGuides(id, x, y) },
                            onTransformChange = { s, px, py -> viewModel.updateTransform(s, px, py) },
                            onSelectComponent = { id -> viewModel.selectComponent(id) },
                            onSelectWire = { wireId -> viewModel.selectWire(wireId) },
                            onLongPressComponent = { showQuickContextMenu = true },
                            onMoveComponent = { id, x, y -> viewModel.moveComponent(id, x, y) },
                            onMoveFinished = { viewModel.recordMoveFinished() },
                            onToggleComponentState = { id -> viewModel.toggleComponentInteractiveState(id) },
                            onSetComponentPressed = { id, pressed -> viewModel.setComponentPressed(id, pressed) },
                            onAddWire = { fc, ft, tc, tt -> viewModel.addWire(fc, ft, tc, tt) },
                            onDeleteWire = { wireId -> viewModel.deleteWire(wireId) },
                            isFullscreen = isFullscreenMode,
                            onToggleFullscreen = { isFullscreenMode = !isFullscreenMode },
                            onZoomIn = { viewModel.zoomIn() },
                            onZoomOut = { viewModel.zoomOut() },
                            onFitToScreen = { viewModel.fitToScreen() }
                        )

                        // منوی لمس طولانی
                        if (showQuickContextMenu && selectedComponent != null) {
                            ComponentQuickActionMenu(
                                component = selectedComponent,
                                onCopy = {
                                    viewModel.copySelectedComponent()
                                    showQuickContextMenu = false
                                },
                                onRotate = {
                                    viewModel.rotateSelectedComponent()
                                    showQuickContextMenu = false
                                },
                                onToggleLock = {
                                    viewModel.toggleLockSelectedComponent()
                                    showQuickContextMenu = false
                                },
                                onDelete = {
                                    viewModel.deleteSelectedComponent()
                                    showQuickContextMenu = false
                                },
                                onShowSpecs = {
                                    showQuickContextMenu = false
                                },
                                onDismiss = { showQuickContextMenu = false },
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }

                        // کارت مشخصات و تگ قطعه
                        if (!isTabletLandscape && selectedComponent != null && !isSimRunning && !showQuickContextMenu) {
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(10.dp)
                                    .width(280.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${selectedComponent.label} - ${selectedComponent.type.persianName.take(15)}",
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                        IconButton(onClick = { viewModel.deleteSelectedComponent() }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Outlined.Delete, contentDescription = "حذف قطعه", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }

                                    if (selectedComponent.type in listOf(
                                            ComponentType.CONTACTOR_COIL,
                                            ComponentType.CONTACTOR_NO_AUX,
                                            ComponentType.CONTACTOR_NC_AUX,
                                            ComponentType.TIMER_ON_DELAY_COIL,
                                            ComponentType.TIMER_OFF_DELAY_COIL,
                                            ComponentType.TIMER_NO_CONTACT,
                                            ComponentType.TIMER_NC_CONTACT,
                                            ComponentType.MAIN_POWER_CONTACTS_3P,
                                            ComponentType.MOTOR_3PHASE
                                        )
                                    ) {
                                        Text(text = "تگ اتصال کویل (KM1, KM2, KT1):", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                        OutlinedTextField(
                                            value = selectedComponent.tagBinding,
                                            onValueChange = { viewModel.updateSelectedComponentTag(it) },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    if (selectedComponent.type == ComponentType.CONTACTOR_COIL) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = "اینترلاک متقابل حفاظتی (مثلاً با KM2):", color = MaterialTheme.colorScheme.tertiary, fontSize = 10.sp)
                                        OutlinedTextField(
                                            value = selectedComponent.interlockTag,
                                            onValueChange = { viewModel.updateSelectedComponentInterlockTag(it) },
                                            singleLine = true,
                                            placeholder = { Text("مثلاً KM2", color = MaterialTheme.colorScheme.outline, fontSize = 10.sp) },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    if (selectedComponent.type in listOf(ComponentType.PUSH_BUTTON_NO, ComponentType.PUSH_BUTTON_NC, ComponentType.EMERGENCY_STOP)) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = if (selectedComponent.isMaintained) "کلید قفل‌شونده (ماندگار)" else "شستی لحظه‌ای (بازگشت‌فنر)",
                                                color = if (selectedComponent.isMaintained) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                                                fontSize = 11.sp
                                            )
                                            Switch(
                                                checked = selectedComponent.isMaintained,
                                                onCheckedChange = { viewModel.toggleComponentMaintained(selectedComponent.id) }
                                            )
                                        }
                                    }

                                    if (selectedComponent.type == ComponentType.BIMETAL_OVERLOAD) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Button(
                                            onClick = { viewModel.toggleBimetalTrip(selectedComponent.id) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (selectedComponent.isBimetalTripped) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant
                                            ),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = if (selectedComponent.isBimetalTripped) "وضعیت: تریپ خورده (قطع ۹۵-۹۶) - کلیک برای وصل" else "شبیه‌سازی تریپ اضافه بار بی‌متال",
                                                color = if (selectedComponent.isBimetalTripped) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "اتصال به PLC (ورودی I یا خروجی Q):", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp)
                                    OutlinedTextField(
                                        value = selectedComponent.plcTagBinding,
                                        onValueChange = { viewModel.updateSelectedComponentPlcTag(it) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    if (selectedComponent.type in listOf(ComponentType.TIMER_ON_DELAY_COIL, ComponentType.TIMER_OFF_DELAY_COIL)) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = "زمان تاخیر: ${PersianUtils.toPersianDigits(selectedComponent.delaySeconds.toInt())} ثانیه", color = MaterialTheme.colorScheme.tertiary, fontSize = 10.sp)
                                        Slider(
                                            value = selectedComponent.delaySeconds,
                                            onValueChange = { viewModel.updateSelectedComponentTimerDelay(it) },
                                            valueRange = 1f..15f,
                                            steps = 14
                                        )
                                    }
                                }
                            }
                        }

                        // پنل مشخصات سیم انتخاب‌شده
                        val selectedWire = wires.find { it.id == selectedWireId }
                        if (!isTabletLandscape && selectedWire != null && selectedComponent == null && !isSimRunning) {
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(10.dp)
                                    .width(290.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .background(
                                                        color = try { Color(android.graphics.Color.parseColor(selectedWire.wireColorHex)) } catch (e: Exception) { MaterialTheme.colorScheme.primary },
                                                        shape = CircleShape
                                                    )
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "مشخصات سیم و نِت",
                                                color = MaterialTheme.colorScheme.tertiary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                        IconButton(
                                            onClick = { viewModel.deleteSelectedWire() },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Outlined.Delete, contentDescription = "حذف سیم", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "شماره نِت / سیم (اتصال الکتریکی):", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                    OutlinedTextField(
                                        value = selectedWire.wireNumber,
                                        onValueChange = { viewModel.updateWireNumber(selectedWire.id, it) },
                                        singleLine = true,
                                        placeholder = { Text("شماره سیم (مثلاً 1, 101, L1)", color = MaterialTheme.colorScheme.outline, fontSize = 10.sp) },
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(text = "پریست استاندارد رنگ سیم:", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)

                                    var colorCategory by remember { mutableStateOf("IEC_60204") }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        listOf(
                                            Pair("IEC_60204", "IEC 60204-1"),
                                            Pair("IEC_60445", "IEC 60445"),
                                            Pair("MANUAL", "دستی")
                                        ).forEach { (catKey, catLabel) ->
                                            val isCatSel = colorCategory == catKey
                                            Surface(
                                                color = if (isCatSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                shape = RoundedCornerShape(4.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable { colorCategory = catKey }
                                            ) {
                                                Text(
                                                    text = catLabel,
                                                    color = if (isCatSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                    modifier = Modifier.padding(vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    val filteredOptions = when (colorCategory) {
                                        "IEC_60445" -> NetNumberingManager.COLOR_PRESETS.filter { it.standardCode == "IEC 60445" }
                                        "IEC_60204" -> NetNumberingManager.COLOR_PRESETS.filter { it.standardCode == "IEC 60204-1" }
                                        else -> NetNumberingManager.COLOR_PRESETS.filter { it.standardCode == "دستی" }
                                    }

                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                    ) {
                                        items(filteredOptions) { opt ->
                                            val isCurr = selectedWire.wireColorHex.equals(opt.colorHex, ignoreCase = true)
                                            Surface(
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                border = androidx.compose.foundation.BorderStroke(
                                                    1.dp,
                                                    if (isCurr) MaterialTheme.colorScheme.primary else Color.Transparent
                                                ),
                                                shape = RoundedCornerShape(4.dp),
                                                modifier = Modifier.clickable {
                                                    viewModel.updateWireColor(selectedWire.id, opt.colorHex, opt.id, opt.standardClause)
                                                }
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .background(
                                                                color = try { Color(android.graphics.Color.parseColor(opt.colorHex)) } catch (e: Exception) { Color.Gray },
                                                                shape = CircleShape
                                                            )
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = opt.nameFa.take(12),
                                                        color = if (isCurr) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                        fontSize = 9.sp
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    val currentPreset = NetNumberingManager.findPresetByHex(selectedWire.wireColorHex)
                                    val clauseText = currentPreset?.let { "${it.standardCode} ${it.standardClause}" } ?: selectedWire.colorStandardRef.ifBlank { "نامشخص" }
                                    Text(
                                        text = "استناد: $clauseText",
                                        color = MaterialTheme.colorScheme.tertiary,
                                        fontSize = 9.sp
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "سطح مقطع سیم (IEC 60228):", color = MaterialTheme.colorScheme.secondary, fontSize = 10.sp)
                                    val commonSections = listOf(0.75, 1.0, 1.5, 2.5, 4.0, 6.0, 10.0, 16.0)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                                        items(commonSections) { cs ->
                                            val isChosen = selectedWire.crossSectionMm2 == cs
                                            Surface(
                                                color = if (isChosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                shape = RoundedCornerShape(4.dp),
                                                modifier = Modifier.clickable {
                                                    viewModel.updateWireCrossSection(selectedWire.id, cs)
                                                }
                                            ) {
                                                Text(
                                                    text = "$cs",
                                                    color = if (isChosen) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "طول تقریبی: ${selectedWire.wireLengthMm.toInt()} mm",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 10.sp
                                        )
                                        Button(
                                            onClick = { viewModel.rerouteSingleWire(selectedWire.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                                            shape = RoundedCornerShape(4.dp),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Outlined.AltRoute, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("مسیریابی مجدد A*", color = MaterialTheme.colorScheme.onSecondaryContainer, fontSize = 9.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // ⭐️ جعبه ابزار قطعات صنعتی در پایین صفحه (فشرده‌شده)
            // ==========================================
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { showLibrarySheet = true },
                        modifier = Modifier
                            .size(30.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                            .testTag("open_library_button")
                    ) {
                        Icon(Icons.Outlined.ElectricalServices, contentDescription = "کتابخانه", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        val quickComponents = listOf(
                            ComponentType.AC_PHASE,
                            ComponentType.AC_NEUTRAL,
                            ComponentType.CIRCUIT_BREAKER_MCB,
                            ComponentType.EMERGENCY_STOP,
                            ComponentType.BIMETAL_OVERLOAD,
                            ComponentType.PUSH_BUTTON_NC,
                            ComponentType.PUSH_BUTTON_NO,
                            ComponentType.CONTACTOR_COIL,
                            ComponentType.CONTACTOR_NO_AUX,
                            ComponentType.CONTACTOR_NC_AUX,
                            ComponentType.TIMER_ON_DELAY_COIL,
                            ComponentType.PILOT_LAMP_GREEN,
                            ComponentType.PILOT_LAMP_RED,
                            ComponentType.MOTOR_3PHASE
                        )

                        items(quickComponents) { cType ->
                            val isFree = viewModel.billingRepository.isComponentTypeFree(cType)
                            ComponentChip(
                                type = cType,
                                isFree = isFree,
                                onAdd = {
                                    if (isFree) {
                                        viewModel.addComponent(cType)
                                    } else {
                                        showVipDialog = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // دکمه شناور شبیه‌سازی (منتقل شده به راست و کمی کوچک‌تر)
        FloatingActionButton(
            onClick = { viewModel.toggleCircuitSimulation() },
            containerColor = if (isSimRunning) Color(0xFFFF1744) else Color(0xFF00E676),
            contentColor = if (isSimRunning) Color.White else Color.Black,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 76.dp, end = 16.dp)
                .size(48.dp)
                .testTag("toggle_simulation_button")
                .semantics { contentDescription = if (isSimRunning) "توقف شبیه‌سازی مدار" else "اجرای زنده شبیه‌سازی مدار" }
        ) {
            Icon(
                imageVector = if (isSimRunning) Icons.Outlined.Stop else Icons.Outlined.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        }
    }

    // ==========================================
    // دیالوگ‌ها و کامپوزبل‌های پایین فایل (بدون هیچ تغییری)
    // ==========================================

    if (showLibrarySheet) {
        IndustrialLibraryBottomSheet(
            onDismissRequest = { showLibrarySheet = false },
            onComponentSelected = { type, specs ->
                if (viewModel.billingRepository.isComponentTypeFree(type)) {
                    viewModel.addComponent(type, specs)
                    Toast.makeText(context, "${type.persianName} به مدار اضافه شد", Toast.LENGTH_SHORT).show()
                } else {
                    showVipDialog = true
                }
            }
        )
    }

    if (showLoadDialog) {
        LoadProjectDialog(
            projects = savedProjects,
            onSelectProject = { proj ->
                viewModel.checkUnsavedChangesBeforeAction {
                    viewModel.loadProject(proj)
                    Toast.makeText(context, "پروژه بارگذاری شد", Toast.LENGTH_SHORT).show()
                }
            },
            onDeleteProject = { id ->
                viewModel.deleteProject(id)
                Toast.makeText(context, "پروژه حذف شد", Toast.LENGTH_SHORT).show()
            },
            onDismissRequest = { showLoadDialog = false }
        )
    }

    if (showVipDialog) {
        VipUpgradeDialog(
            billingRepository = viewModel.billingRepository,
            onDismissRequest = { showVipDialog = false }
        )
    }

    if (showCableListDialog) {
        val cableItems = viewModel.getCableList()
        CableListDialog(
            cableItems = cableItems,
            onDismiss = { showCableListDialog = false },
            onExportCsv = {
                val csvContent = viewModel.exportCableListCsv()
                ExportUtils.shareText(context, csvContent, "خروجی لیست کابل و اتصالات مدار")
                Toast.makeText(context, "لیست کابل با فرمت CSV آماده اشتراک‌گذاری شد", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = {
                Text("خروجی رسمی نقشه مهندسی", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = "نقشه مدار با استاندارد IEC همراه با کادر مهندسی، جدول مشخصات (Title Block) و لیست قطعات (BOM) تولید می‌شود.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                try {
                                    viewModel.shareExport(context, isPdf = true)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "خطا در خروجی PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                                showExportDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("خروجی PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                try {
                                    viewModel.shareExport(context, isPdf = false)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "خطا در خروجی PNG: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                                showExportDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Outlined.Share, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تصویر PNG", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("بستن")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("ذخیره پروژه مدار (پایگاه داده Room)", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column {
                    OutlinedTextField(
                        value = saveTitleInput,
                        onValueChange = { saveTitleInput = it },
                        label = { Text("نام مدار") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = saveDescInput,
                        onValueChange = { saveDescInput = it },
                        label = { Text("توضیحات و مشخصات کاربردی") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveCurrentProject(saveTitleInput, saveDescInput) {
                            Toast.makeText(context, "پروژه با موفقیت در Room ذخیره شد", Toast.LENGTH_SHORT).show()
                        }
                        showSaveDialog = false
                    }
                ) {
                    Text("تایید و ذخیره")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("انصراف")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    pendingUnsavedAction?.let { action ->
        SimYarUnsavedChangesDialog(
            action = action,
            onDismissRequest = { viewModel.dismissUnsavedChangesDialog() }
        )
    }

    draftRecoveryPrompt?.let { prompt ->
        SimYarDraftRecoveryDialog(
            prompt = prompt,
            onRestore = { viewModel.restoreDraft(prompt) },
            onDiscard = { viewModel.discardDraft(prompt.draftKey) }
        )
    }

    showSheetOptionsSheetId?.let { targetSheetId ->
        val targetSheet = sheets.find { it.id == targetSheetId }
        if (targetSheet != null) {
            AlertDialog(
                onDismissRequest = { showSheetOptionsSheetId = null },
                title = { Text("مدیریت برگه: ${targetSheet.name}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                renameSheetInput = targetSheet.name
                                showRenameSheetDialog = targetSheet
                                showSheetOptionsSheetId = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تغییر نام برگه")
                        }

                        Button(
                            onClick = {
                                viewModel.duplicateSheet(targetSheet.id)
                                showSheetOptionsSheetId = null
                                Toast.makeText(context, "برگه با موفقیت تکثیر شد", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Outlined.CopyAll, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تکثیر برگه و اجزا", color = MaterialTheme.colorScheme.primary)
                        }

                        if (sheets.size > 1) {
                            Button(
                                onClick = {
                                    showDeleteSheetConfirm = targetSheet
                                    showSheetOptionsSheetId = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("حذف این برگه", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showSheetOptionsSheetId = null }) {
                        Text("انصراف")
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface
            )
        }
    }

    showRenameSheetDialog?.let { targetSheet ->
        AlertDialog(
            onDismissRequest = { showRenameSheetDialog = null },
            title = { Text("تغییر نام برگه", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameSheetInput,
                    onValueChange = { renameSheetInput = it },
                    label = { Text("نام جدید برگه") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameSheetInput.isNotBlank()) {
                            viewModel.renameSheet(targetSheet.id, renameSheetInput.trim())
                        }
                        showRenameSheetDialog = null
                    }
                ) {
                    Text("تأیید")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameSheetDialog = null }) {
                    Text("انصراف")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    showDeleteSheetConfirm?.let { targetSheet ->
        AlertDialog(
            onDismissRequest = { showDeleteSheetConfirm = null },
            title = { Text("حذف برگه «${targetSheet.name}»", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "آیا مطمئن هستید؟ تمام قطعات و سیم‌های موجود در این برگه حذف خواهند شد. این عملیات قابل بازگردانی (Undo) است.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val deleted = viewModel.deleteSheet(targetSheet.id)
                        if (deleted) {
                            Toast.makeText(context, "برگه حذف شد", Toast.LENGTH_SHORT).show()
                        }
                        showDeleteSheetConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف قطعی", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSheetConfirm = null }) {
                    Text("انصراف")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showSearchDialog) {
        AlertDialog(
            onDismissRequest = {
                showSearchDialog = false
                viewModel.clearSearch()
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Search, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("جستجوی قطعه روی بوم", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("برچسب (مثلاً KM1)، تگ، یا نوع قطعه...", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline) },
                        singleLine = true,
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { viewModel.clearSearch() }) {
                                    Icon(Icons.Outlined.Close, contentDescription = "پاک کردن", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (searchQuery.isNotBlank() && searchResults.isEmpty()) {
                        Text(
                            text = "قطعه‌ای با این مشخصات یافت نشد",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else if (searchResults.isNotEmpty()) {
                        Text(
                            text = "${PersianUtils.toPersianDigits(searchResults.size)} نتیجه یافت شد (برای رفتن به قطعه لمس کنید):",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                            items(searchResults) { comp ->
                                val compSheet = sheets.find { it.id == comp.sheetId }?.name ?: "برگه ۱"
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                        .clickable {
                                            viewModel.focusOnComponent(comp.id)
                                            showSearchDialog = false
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "${comp.label} - ${comp.type.persianName}",
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                            if (comp.tagBinding.isNotBlank()) {
                                                Text(
                                                    text = "تگ اتصال: ${comp.tagBinding}",
                                                    color = MaterialTheme.colorScheme.tertiary,
                                                    fontSize = 9.sp
                                                )
                                            }
                                        }
                                        Surface(
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = compSheet,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontSize = 9.sp,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = {
                    showSearchDialog = false
                    viewModel.clearSearch()
                }) {
                    Text("بستن")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showLayersDialog) {
        AlertDialog(
            onDismissRequest = { showLayersDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Layers, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("مدیریت لایه‌های مدار", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "لایه‌ها فقط جهت تفکیک گرافیکی مدار هستند؛ لایه‌های مخفی نیز در شبیه‌سازی اجرا می‌شوند.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    layers.forEach { layer ->
                        val layerColor = try {
                            Color(android.graphics.Color.parseColor(layer.colorHex))
                        } catch (e: Exception) {
                            MaterialTheme.colorScheme.primary
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, layerColor.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .background(layerColor, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "لایه ${layer.name}",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = { viewModel.toggleLayerVisibility(layer.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (layer.isVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                            contentDescription = if (layer.isVisible) "مخفی کردن لایه" else "نمایش لایه",
                                            tint = if (layer.isVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.toggleLayerLock(layer.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (layer.isLocked) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
                                            contentDescription = if (layer.isLocked) "باز کردن قفل لایه" else "قفل کردن لایه",
                                            tint = if (layer.isLocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (selectedComponent != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "اختصاص قطعه «${selectedComponent.label}» به لایه:",
                            color = MaterialTheme.colorScheme.tertiary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            layers.forEach { l ->
                                val isAssigned = selectedComponent.layerId == l.id
                                Surface(
                                    color = if (isAssigned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { viewModel.setComponentLayer(selectedComponent.id, l.id) }
                                ) {
                                    Text(
                                        text = l.name,
                                        color = if (isAssigned) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLayersDialog = false }) {
                    Text("بستن")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showTimingDialog) {
        SignalTimingDialog(
            viewModel = viewModel,
            onDismissRequest = { showTimingDialog = false }
        )
    }
}

@Composable
private fun ComponentChip(
    type: ComponentType,
    isFree: Boolean = true,
    onAdd: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isFree) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)),
        modifier = Modifier
            .clickable { onAdd() }
            .semantics { contentDescription = "افزودن ${type.persianName} به بوم" }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(if (isFree) Color(0xFF00E676) else Color(0xFFFFD600), CircleShape)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = type.defaultPrefix,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = type.persianName.take(10),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp
            )
            if (!isFree) {
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "VIP",
                    color = MaterialTheme.colorScheme.tertiary,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
fun SimYarUnsavedChangesDialog(
    action: SimYarViewModel.UnsavedChangesAction,
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = action.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        },
        text = {
            Text(
                text = action.message,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                lineHeight = 20.sp
            )
        },
        confirmButton = {
            Button(
                onClick = action.onSave,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("ذخیره", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = action.onDiscard,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("دور انداختن")
                }
                TextButton(
                    onClick = action.onCancel,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Text("انصراف")
                }
            }
        }
    )
}

@Composable
fun SimYarDraftRecoveryDialog(
    prompt: SimYarViewModel.DraftRecoveryPrompt,
    onRestore: () -> Unit,
    onDiscard: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "بازیابی تغییرات ذخیره‌نشده؟",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "یک پیش‌نویس ذخیره‌نشده از پروژه «${prompt.draftTitle}» یافت شد. آیا مایل به بازیابی این تغییرات هستید؟",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
                prompt.warning?.let { warning ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = warning,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "تعداد قطعات: ${PersianUtils.toPersianDigits(prompt.components.size)} | سیم‌ها: ${PersianUtils.toPersianDigits(prompt.wires.size)} | خطوط لدر: ${PersianUtils.toPersianDigits(prompt.ladderRungs.size)}",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 11.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onRestore,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("بازیابی", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDiscard,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("دور انداختن")
            }
        }
    )
}

@Composable
fun CableListDialog(
    cableItems: List<CableListItem>,
    onDismiss: () -> Unit,
    onExportCsv: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(cableItems, searchQuery) {
        if (searchQuery.isBlank()) cableItems
        else cableItems.filter {
            it.wireNumber.contains(searchQuery, ignoreCase = true) ||
                    it.fromComponentLabel.contains(searchQuery, ignoreCase = true) ||
                    it.toComponentLabel.contains(searchQuery, ignoreCase = true) ||
                    it.colorPresetName.contains(searchQuery, ignoreCase = true) ||
                    it.standardClause.contains(searchQuery, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.FormatListNumbered,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "لیست کابل و اتصالات سیم‌کشی",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().height(380.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("جستجو بر اساس شماره سیم، قطعه یا رنگ...", color = MaterialTheme.colorScheme.outline, fontSize = 11.sp) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (filtered.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (cableItems.isEmpty()) "هیچ سیمی در مدار رسم نشده است." else "موردی با این عبارت یافت نشد.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                            items(filtered) { item ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                    shape = RoundedCornerShape(4.dp),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                                                ) {
                                                    Text(
                                                        text = "سیم: ${item.wireNumber}",
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .size(10.dp)
                                                        .background(
                                                            color = try { Color(android.graphics.Color.parseColor(item.colorHex)) } catch (e: Exception) { Color.Gray },
                                                            shape = CircleShape
                                                        )
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = item.colorPresetName,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 10.sp
                                                )
                                            }

                                            Text(
                                                text = "${item.crossSectionMm2} mm²",
                                                color = MaterialTheme.colorScheme.tertiary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "مسیر: ${item.fromComponentLabel}:${item.fromTerminalId}  ←  ${item.toComponentLabel}:${item.toTerminalId}",
                                                color = MaterialTheme.colorScheme.secondary,
                                                fontSize = 10.sp
                                            )
                                            Text(
                                                text = "${item.lengthMm.toInt()} mm",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 10.sp
                                            )
                                        }

                                        if (item.standardClause.isNotBlank() && item.standardClause != "نامشخص") {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "بند استاندارد: ${item.standardClause}",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 9.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onExportCsv,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Outlined.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("خروجی CSV", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
            ) {
                Text("بستن")
            }
        }
    )
}