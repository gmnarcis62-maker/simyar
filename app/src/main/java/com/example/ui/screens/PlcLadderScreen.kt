package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timeline
import com.example.ui.timing.SignalTimingDialog
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.canvas.PlcLadderCanvasView
import com.example.model.LadderElement
import com.example.model.LadderElementType
import com.example.util.PersianUtils
import com.example.viewmodel.SimYarViewModel
import java.util.UUID

private data class PlcToolItem(
    val type: LadderElementType?,
    val isBranch: Boolean = false,
    val title: String,
    val symbolDisplay: String,
    val description: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlcLadderScreen(
    viewModel: SimYarViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val rungs by viewModel.ladderRungs.collectAsState()
    val isPlcRunning by viewModel.isPlcRunning.collectAsState()
    val plcMemory by viewModel.plcMemory.collectAsState()
    val scanTimeMs by viewModel.plcScanTimeMs.collectAsState()

    // حالت تمام صفحه شدن بوم
    var isFullscreen by rememberSaveable { mutableStateOf(false) }
    var showTimingDialog by rememberSaveable { mutableStateOf(false) }

    val isDirty by viewModel.isDirty.collectAsState()
    val pendingUnsavedAction by viewModel.pendingUnsavedAction.collectAsState()
    val draftRecoveryPrompt by viewModel.draftRecoveryPrompt.collectAsState()

    // کنترل کلید بازگشت اندروید برای خروج از حالت تمام‌صفحه
    BackHandler(enabled = isFullscreen) {
        isFullscreen = false
    }

    // ابزار فعال جهت درج روی بوم
    var activeToolItem by remember { mutableStateOf<PlcToolItem?>(null) }

    var showAddElementDialog by remember { mutableStateOf(false) }
    var selectedRungIdForElement by remember { mutableStateOf("") }
    var isAddingBranch by remember { mutableStateOf(false) }
    var selectedElementType by remember { mutableStateOf(LadderElementType.NO_CONTACT) }
    var addressInput by remember { mutableStateOf("I0.1") }
    var presetInput by remember { mutableIntStateOf(5) }
    var elementDropdownExpanded by remember { mutableStateOf(false) }

    // دیالوگ ویرایش/حذف المان موجود
    var showEditElementDialog by remember { mutableStateOf(false) }
    var editingRungId by remember { mutableStateOf("") }
    var editingElement by remember { mutableStateOf<LadderElement?>(null) }
    var editAddressInput by remember { mutableStateOf("") }
    var editTypeInput by remember { mutableStateOf(LadderElementType.NO_CONTACT) }
    var editPresetInput by remember { mutableIntStateOf(5) }
    var editDropdownExpanded by remember { mutableStateOf(false) }

    val toolsList = remember {
        listOf(
            PlcToolItem(null, false, "انتخاب", "👆", "حالت لمس و تعامل با المان‌ها"),
            PlcToolItem(LadderElementType.NO_CONTACT, false, "کنتاکت باز NO", "-[ ]-", "Normally Open Contact"),
            PlcToolItem(LadderElementType.NC_CONTACT, false, "کنتاکت بسته NC", "-[/]-", "Normally Closed Contact"),
            PlcToolItem(LadderElementType.NO_CONTACT, true, "شاخه موازی OR", "+OR", "Parallel OR Branch"),
            PlcToolItem(LadderElementType.NORMAL_COIL, false, "کویل خروجی", "-( )-", "Normal Output Coil"),
            PlcToolItem(LadderElementType.SET_COIL, false, "کویل ست (S)", "-(S)-", "Set Latch Coil"),
            PlcToolItem(LadderElementType.RESET_COIL, false, "کویل ریست (R)", "-(R)-", "Reset Unlatch Coil"),
            PlcToolItem(LadderElementType.TIMER_TON, false, "تایمر TON", "[TON]", "On-Delay Timer"),
            PlcToolItem(LadderElementType.TIMER_TOF, false, "تایمر TOF", "[TOF]", "Off-Delay Timer"),
            PlcToolItem(LadderElementType.COUNTER_CTU, false, "کانتر CTU", "[CTU]", "Up Counter")
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (!isFullscreen) {
            // ۱. نوار بالای کنترل پردازنده PLC و وضعیت سخت‌افزار
            Surface(
                color = Color(0xFF141D2B),
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "شبیه‌ساز و بوم ترسیم PLC نردبانی (IEC 61131-3)",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            if (isDirty) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color(0xFFFF9100).copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF9100))
                                ) {
                                    Text(
                                        text = "ذخیره‌نشده",
                                        color = Color(0xFFFFB74D),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "رانگ‌ها: ${PersianUtils.toPersianDigits(rungs.size)} | زمان اسکن: ${PersianUtils.toPersianDigits(scanTimeMs)} ms",
                            color = Color(0xFF00E5FF),
                            fontSize = 11.sp
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { viewModel.togglePlcSimulation() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isPlcRunning) Color(0xFFD50000) else Color(0xFF00E676)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .testTag("toggle_plc_button")
                                .semantics { contentDescription = if (isPlcRunning) "توقف پردازشگر PLC" else "اجرای برنامه نردبانی PLC" }
                        ) {
                            Icon(
                                imageVector = if (isPlcRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = if (isPlcRunning) Color.White else Color.Black
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isPlcRunning) "PLC STOP" else "PLC RUN",
                                color = if (isPlcRunning) Color.White else Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.addLadderRung()
                                Toast.makeText(context, "رانگ لدر جدید اضافه شد", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.semantics { contentDescription = "افزودن رانگ جدید به برنامه لدر" }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("افزودن رانگ", color = Color.White, fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                viewModel.refreshAvailableTimingSignals()
                                showTimingDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .testTag("open_plc_timing_button")
                                .semantics { contentDescription = "مشاهده نمودار زمانی سیگنال‌های PLC" }
                        ) {
                            Icon(Icons.Default.Timeline, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("نمودار زمانی", color = Color(0xFF00E5FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        IconButton(
                            onClick = { isFullscreen = !isFullscreen },
                            modifier = Modifier
                                .size(36.dp)
                                .semantics { contentDescription = if (isFullscreen) "خروج از تمام صفحه" else "تمام صفحه شدن بوم لدر" }
                        ) {
                            Icon(
                                imageVector = if (isFullscreen) Icons.Outlined.FullscreenExit else Icons.Outlined.Fullscreen,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF)
                            )
                        }
                    }
                }

                // نوار وضعیت ورودی‌ها و خروجی‌های دیجیتال
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "جدول تگ‌های سخت‌افزاری PLC (ورودی‌های I و خروجی‌های Q) - کلیک برای تغییر دستی:",
                    color = Color(0xFFB0BEC5),
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val inputList = listOf("I0.0", "I0.1", "I0.2", "I0.3")
                    items(inputList) { inp ->
                        val active = plcMemory.inputs[inp] ?: false
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (active) Color(0xFF00E676) else Color(0xFF263238)
                            ),
                            modifier = Modifier
                                .clickable { viewModel.togglePlcInput(inp) }
                                .semantics { contentDescription = "ورودی سخت‌افزاری $inp: ${if (active) "یک منطقی" else "صفر منطقی"}" }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(if (active) Color.White else Color(0xFF546E7A), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$inp (${if (active) "۱" else "۰"})",
                                    color = if (active) Color.Black else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    val outputList = listOf("Q0.0", "Q0.1", "Q0.2")
                    items(outputList) { out ->
                        val active = plcMemory.outputs[out] ?: false
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (active) Color(0xFFFF5252) else Color(0xFF1E2838)
                            ),
                            modifier = Modifier.semantics { contentDescription = "خروجی PLC $out: ${if (active) "فعال" else "غیرفعال"}" }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(if (active) Color.White else Color(0xFF455A64), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$out (${if (active) "روشن" else "خاموش"})",
                                    color = if (active) Color.White else Color.Blue,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // ۲. نوار ابزار پالت نمادهای استاندارد PLC (PLC Ladder Symbols Palette)
        Surface(
            color = Color(0xFF0F1724),
            modifier = Modifier
                .fillMaxWidth()
                .border(width = 1.dp, color = Color(0xFF1B283C))
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp)) {
                Text(
                    text = "پالت نمادهای استاندارد PLC — نماد را انتخاب و روی بوم قرار دهید:",
                    color = Color(0xFF80D8FF),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(toolsList) { tool ->
                        val isSelected = activeToolItem == tool
                        val itemBg = if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.25f) else Color(0xFF162030)
                        val itemBorder = if (isSelected) Color(0xFF00E5FF) else Color(0xFF29394D)

                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = itemBg),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, itemBorder),
                            modifier = Modifier
                                .clickable {
                                    activeToolItem = if (isSelected) null else tool
                                }
                                .semantics { contentDescription = "ابزار ${tool.title}" }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = tool.symbolDisplay,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isSelected) Color(0xFF00E5FF) else Color(0xFFFFD600)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tool.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else Color(0xFFCFD8DC)
                                )
                            }
                        }
                    }
                }

                // راهنمای ابزار انتخابی فعال
                AnimatedVisibility(visible = activeToolItem != null && activeToolItem?.type != null) {
                    val activeTool = activeToolItem!!
                    Surface(
                        color = Color(0xFF00E5FF).copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.TouchApp,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "نماد فعال: «${activeTool.title}» — روی اسلات (+) رانگ تپ کنید تا اضافه شود.",
                                    color = Color(0xFF80D8FF),
                                    fontSize = 11.sp
                                )
                            }

                            IconButton(
                                onClick = { activeToolItem = null },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "لغو ابزار", tint = Color(0xFF90A4AE), modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }
        }

        // ۳. بوم ترسیم نردبانی Ladder Canvas با قابلیت قرار دادن نمادها و اسکرول و بزرگنمایی دو بعدی
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            PlcLadderCanvasView(
                rungs = rungs,
                isPlcRunning = isPlcRunning,
                activeTool = activeToolItem?.type,
                isFullscreen = isFullscreen,
                onToggleFullscreen = { isFullscreen = !isFullscreen },
                onContactToggled = { address -> viewModel.togglePlcInput(address) },
                onDeleteRung = { rungId ->
                    viewModel.deleteLadderRung(rungId)
                    Toast.makeText(context, "رانگ حذف شد", Toast.LENGTH_SHORT).show()
                },
                onMoveRungUp = { rungId -> viewModel.moveLadderRungUp(rungId) },
                onMoveRungDown = { rungId -> viewModel.moveLadderRungDown(rungId) },
                onAddElementClick = { rungId ->
                    selectedRungIdForElement = rungId
                    isAddingBranch = false
                    selectedElementType = activeToolItem?.type ?: LadderElementType.NO_CONTACT
                    addressInput = "I0.2"
                    presetInput = 5
                    showAddElementDialog = true
                },
                onAddBranchClick = { rungId ->
                    selectedRungIdForElement = rungId
                    isAddingBranch = true
                    selectedElementType = LadderElementType.NO_CONTACT
                    addressInput = "Q0.0"
                    presetInput = 5
                    showAddElementDialog = true
                },
                onElementClick = { rungId, element ->
                    editingRungId = rungId
                    editingElement = element
                    editAddressInput = element.address
                    editTypeInput = element.type
                    editPresetInput = element.presetValue
                    showEditElementDialog = true
                },
                onPlaceSymbol = { rungId, type, isBranch ->
                    val branchParam = isBranch || (activeToolItem?.isBranch == true)
                    viewModel.placeSymbolOnRung(
                        rungId = rungId,
                        type = type,
                        isBranch = branchParam
                    )
                    Toast.makeText(context, "نماد ${type.symbol} در مدار قرار گرفت", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxSize()
            )

            if (isFullscreen) {
                Surface(
                    color = Color(0xDD0F1724),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF)),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .clickable { isFullscreen = false }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FullscreenExit,
                            contentDescription = "خروج از تمام‌صفحه",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("خروج از تمام‌صفحه", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // ۴. دیالوگ افزودن المان یا شاخه جدید به رانگ
    if (showAddElementDialog) {
        AlertDialog(
            onDismissRequest = { showAddElementDialog = false },
            title = {
                Text(
                    text = if (isAddingBranch) "افزودن شاخه موازی خودنگهدار (OR)" else "افزودن المان به رانگ",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("نوع المان استاندارد PLC:", color = Color(0xFF00E5FF), fontSize = 12.sp)

                    ExposedDropdownMenuBox(
                        expanded = elementDropdownExpanded,
                        onExpandedChange = { elementDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedElementType.persianTitle,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = elementDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = elementDropdownExpanded,
                            onDismissRequest = { elementDropdownExpanded = false }
                        ) {
                            LadderElementType.values().forEach { type ->
                                DropdownMenuItem(
                                    text = { Text("${type.symbol} ${type.persianTitle}") },
                                    onClick = {
                                        selectedElementType = type
                                        elementDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = addressInput,
                        onValueChange = { addressInput = it },
                        label = { Text("آدرس (مثل I0.1, Q0.0, T1, C1, M0.0)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (selectedElementType in listOf(LadderElementType.TIMER_TON, LadderElementType.TIMER_TOF, LadderElementType.COUNTER_CTU, LadderElementType.COUNTER_CTD)) {
                        OutlinedTextField(
                            value = presetInput.toString(),
                            onValueChange = { presetInput = it.toIntOrNull() ?: 5 },
                            label = { Text("مقدار Preset (ثانیه برای تایمر / عدد برای کانتر)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val isContact = selectedElementType == LadderElementType.NO_CONTACT || selectedElementType == LadderElementType.NC_CONTACT
                        if (isAddingBranch) {
                            val newBranchElem = LadderElement(
                                id = "br_${UUID.randomUUID().toString().take(5)}",
                                type = selectedElementType,
                                address = addressInput,
                                comment = "شاخه موازی خودنگهدار",
                                presetValue = presetInput
                            )
                            viewModel.addBranchToRung(selectedRungIdForElement, newBranchElem)
                            Toast.makeText(context, "شاخه موازی خودنگهدار اضافه شد", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.addElementToLadderRung(selectedRungIdForElement, selectedElementType, addressInput, isContact, presetInput)
                            Toast.makeText(context, "المان اضافه شد", Toast.LENGTH_SHORT).show()
                        }
                        showAddElementDialog = false
                    }
                ) {
                    Text("افزودن")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddElementDialog = false }) {
                    Text("انصراف")
                }
            },
            containerColor = Color(0xFF1E2838)
        )
    }

    // ۵. دیالوگ ویرایش یا حذف المان
    if (showEditElementDialog && editingElement != null) {
        val elem = editingElement!!
        AlertDialog(
            onDismissRequest = { showEditElementDialog = false },
            title = { Text("ویرایش المان ${elem.address}", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editAddressInput,
                        onValueChange = { editAddressInput = it },
                        label = { Text("آدرس تگ سخت‌افزاری") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    ExposedDropdownMenuBox(
                        expanded = editDropdownExpanded,
                        onExpandedChange = { editDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = editTypeInput.persianTitle,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = editDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = editDropdownExpanded,
                            onDismissRequest = { editDropdownExpanded = false }
                        ) {
                            LadderElementType.values().forEach { type ->
                                DropdownMenuItem(
                                    text = { Text("${type.symbol} ${type.persianTitle}") },
                                    onClick = {
                                        editTypeInput = type
                                        editDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (editTypeInput in listOf(LadderElementType.TIMER_TON, LadderElementType.TIMER_TOF, LadderElementType.COUNTER_CTU, LadderElementType.COUNTER_CTD)) {
                        OutlinedTextField(
                            value = editPresetInput.toString(),
                            onValueChange = { editPresetInput = it.toIntOrNull() ?: 5 },
                            label = { Text("مقدار Preset") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(
                        onClick = {
                            viewModel.deleteLadderElement(editingRungId, elem.id)
                            showEditElementDialog = false
                            Toast.makeText(context, "المان حذف شد", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1744))
                    ) {
                        Text("حذف")
                    }
                    Button(
                        onClick = {
                            viewModel.updateLadderElement(editingRungId, elem.id, editAddressInput, editTypeInput, editPresetInput)
                            showEditElementDialog = false
                            Toast.makeText(context, "المان به‌روزرسانی شد", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("ذخیره")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditElementDialog = false }) {
                    Text("انصراف")
                }
            },
            containerColor = Color(0xFF1E2838)
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

    if (showTimingDialog) {
        SignalTimingDialog(
            viewModel = viewModel,
            onDismissRequest = { showTimingDialog = false }
        )
    }
}
