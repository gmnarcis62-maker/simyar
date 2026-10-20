package com.example.ui.timing

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.canvas.TimingDiagramView
import com.example.util.PersianUtils
import com.example.viewmodel.SimYarViewModel

/**
 * صفحه و دیالوگ کامل نمودار زمانی لاجیک سیگنال‌ها (Timing Diagram)
 */
@Composable
fun SignalTimingDialog(
    viewModel: SimYarViewModel,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current

    // وضعیت‌های دریافتی از ViewModel
    val selectedSignals by viewModel.selectedTimingSignals.collectAsState()
    val availableSignals by viewModel.availableTimingSignals.collectAsState()
    val isRecordingPaused by viewModel.isTimingRecordingPaused.collectAsState()
    val simCurrentTime by viewModel.timingCurrentTimeSeconds.collectAsState()
    val signalBuffer = viewModel.timingSignalBuffer

    // متغیرهای نمایشی زوم، پیمایش و مکان‌نما
    var timeScalePxPerSec by remember { mutableFloatStateOf(80f) }
    var panOffsetSeconds by remember { mutableFloatStateOf(0f) }
    var cursorTimeSeconds by remember { mutableStateOf<Float?>(null) }

    // دیالوگ انتخاب سیگنال‌ها
    var showSignalPickerDialog by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF0A0F1D)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // ۱. نوار ابزار بالا (Header)
                Surface(
                    color = Color(0xFF131C2E),
                    tonalElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // عنوان و مشخصات
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF00E5FF).copy(alpha = 0.2f), CircleShape)
                                    .border(1.dp, Color(0xFF00E5FF), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timeline,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "نمودار زمانی سیگنال‌ها (Timing Diagram)",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = if (!isRecordingPaused) Color(0xFF00E676).copy(alpha = 0.2f) else Color(0xFFFF9100).copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = if (!isRecordingPaused) "در حال ثبت ۶۰fps" else "ثبت موقتاً متوقف",
                                            color = if (!isRecordingPaused) Color(0xFF00E676) else Color(0xFFFF9100),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "بافر حلقوی ۶۰ ثانیه‌ای | مانیتورینگ بلادرنگ لدر PLC و مدار فرمان",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 10.sp
                                )
                            }
                        }

                        // دکمه‌های کنترل
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // دکمه توقف / ادامه ضبط (Pause / Resume)
                            IconButton(
                                onClick = { viewModel.toggleTimingRecordingPause() },
                                modifier = Modifier.testTag("toggle_recording_button")
                            ) {
                                Icon(
                                    imageVector = if (isRecordingPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                    contentDescription = if (isRecordingPaused) "ادامه ضبط" else "توقف ضبط",
                                    tint = if (isRecordingPaused) Color(0xFF00E676) else Color(0xFFFFB74D)
                                )
                            }

                            // دکمه پاکسازی بافر
                            IconButton(
                                onClick = {
                                    viewModel.clearTimingBuffer()
                                    cursorTimeSeconds = null
                                    panOffsetSeconds = 0f
                                },
                                modifier = Modifier.testTag("clear_timing_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "پاک کردن داده‌ها",
                                    tint = Color(0xFFFF5252)
                                )
                            }

                            // بزرگ‌نمایی زمان
                            IconButton(
                                onClick = { timeScalePxPerSec = (timeScalePxPerSec * 1.3f).coerceAtMost(600f) },
                                modifier = Modifier.testTag("zoom_in_button")
                            ) {
                                Icon(Icons.Default.ZoomIn, contentDescription = "بزرگ‌نمایی زمان", tint = Color(0xFF38BDF8))
                            }

                            // کوچک‌نمایی زمان
                            IconButton(
                                onClick = { timeScalePxPerSec = (timeScalePxPerSec / 1.3f).coerceAtLeast(20f) },
                                modifier = Modifier.testTag("zoom_out_button")
                            ) {
                                Icon(Icons.Default.ZoomOut, contentDescription = "کوچک‌نمایی زمان", tint = Color(0xFF38BDF8))
                            }

                            // ریست ویو و بازگشت به زمان حال
                            IconButton(
                                onClick = {
                                    timeScalePxPerSec = 80f
                                    panOffsetSeconds = (simCurrentTime - 8f).coerceAtLeast(0f)
                                }
                            ) {
                                Icon(Icons.Outlined.RestartAlt, contentDescription = "ریست نما", tint = Color(0xFF94A3B8))
                            }

                            // استخراج تصویر PNG
                            Button(
                                onClick = {
                                    TimingDiagramExporter.exportToPng(
                                        context = context,
                                        selectedSignals = selectedSignals,
                                        buffer = signalBuffer,
                                        totalDurationSeconds = simCurrentTime
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("export_png_button")
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("خروجی PNG", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }

                            // دکمه بستن
                            IconButton(onClick = onDismissRequest) {
                                Icon(Icons.Default.Close, contentDescription = "بستن", tint = Color.White)
                            }
                        }
                    }
                }

                // ۲. نوار چیپ‌های سیگنال‌های فعال و دکمه افزودن سیگنال
                Surface(
                    color = Color(0xFF0F172A),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { showSignalPickerDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("open_signal_picker_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "انتخاب سیگنال‌ها (${PersianUtils.toPersianDigits(selectedSignals.size)}/۱۲)",
                                color = Color(0xFF00E5FF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(selectedSignals, key = { it.id }) { sig ->
                                val chipColor = when (sig.type) {
                                    SignalSourceType.PLC_OUTPUT -> Color(0xFF00E5FF)
                                    SignalSourceType.PLC_INPUT -> Color(0xFFFFD600)
                                    SignalSourceType.CIRCUIT_COIL -> Color(0xFF00E676)
                                    SignalSourceType.PLC_TIMER_DONE -> Color(0xFFFF9100)
                                    SignalSourceType.PLC_COUNTER_DONE -> Color(0xFFE040FB)
                                    else -> Color(0xFF64B5F6)
                                }

                                Surface(
                                    color = Color(0xFF1E293B),
                                    shape = RoundedCornerShape(16.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, chipColor.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(chipColor, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = sig.displayName,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "حذف سیگنال",
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable { viewModel.removeTimingSignal(sig.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ۳. بوم ترسیم نمودار زمانی لاجیک آنالایزر
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (selectedSignals.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Timeline, contentDescription = null, tint = Color(0xFF475569), modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "هیچ سیگنالی برای نمایش انتخاب نشده است",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { showSignalPickerDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                                ) {
                                    Text("افزودن سیگنال‌های مدار و PLC", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        TimingDiagramView(
                            selectedSignals = selectedSignals,
                            buffer = signalBuffer,
                            currentTimeSeconds = simCurrentTime,
                            timeScalePxPerSec = timeScalePxPerSec,
                            panOffsetSeconds = panOffsetSeconds,
                            cursorTimeSeconds = cursorTimeSeconds,
                            onCursorMoved = { cursorTimeSeconds = it },
                            onPanChanged = { panOffsetSeconds = it.coerceAtLeast(0f) },
                            onScaleChanged = { timeScalePxPerSec = it },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // ۴. نوار وضعیت و قرائت مکان‌نما (Footer)
                Surface(
                    color = Color(0xFF131C2E),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "زمان جاری شبیه‌سازی: ${PersianUtils.toPersianDigits(String.format("%.2f", simCurrentTime))} s",
                                color = Color(0xFF38BDF8),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            if (cursorTimeSeconds != null) {
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = "مکان‌نما: ${PersianUtils.toPersianDigits(String.format("t = %.2f s", cursorTimeSeconds))}",
                                    color = Color(0xFFFF5252),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "(برای حذف مکان‌نما ضربه بزنید)",
                                    color = Color(0xFF64748B),
                                    fontSize = 9.sp,
                                    modifier = Modifier.clickable { cursorTimeSeconds = null }
                                )
                            }
                        }

                        Text(
                            text = "راهنما: کشیدن افقی برای پیمایش زمان | دو انگشت برای بزرگ‌نمایی | ضربه برای مکان‌نما",
                            color = Color(0xFF64748B),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }

    // دیالوگ انتخاب سیگنال‌ها با کادر جستجو و سقف ۱۲ سیگنال
    if (showSignalPickerDialog) {
        SignalPickerModal(
            availableSignals = availableSignals,
            selectedSignalIds = selectedSignals.map { it.id }.toSet(),
            onToggleSignal = { sig ->
                if (selectedSignals.any { it.id == sig.id }) {
                    viewModel.removeTimingSignal(sig.id)
                } else {
                    if (selectedSignals.size < 12) {
                        viewModel.addTimingSignal(sig)
                    }
                }
            },
            onDismiss = { showSignalPickerDialog = false }
        )
    }
}

/**
 * مودال انتخاب سیگنال‌ها با قابلیت جستجو، فیلتر دسته‌بندی و سقف ۱۲ سیگنال
 */
@Composable
private fun SignalPickerModal(
    availableSignals: List<TrackableSignal>,
    selectedSignalIds: Set<String>,
    onToggleSignal: (TrackableSignal) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<SignalSourceType?>(null) }

    val filteredList = availableSignals.filter { sig ->
        (selectedCategory == null || sig.type == selectedCategory) &&
                (searchQuery.isBlank() ||
                        sig.displayName.contains(searchQuery, ignoreCase = true) ||
                        sig.addressOrTag.contains(searchQuery, ignoreCase = true) ||
                        sig.type.displayName.contains(searchQuery, ignoreCase = true))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "انتخاب سیگنال‌های مانیتورینگ",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "${PersianUtils.toPersianDigits(selectedSignalIds.size)} از ۱۲ سیگنال",
                    color = if (selectedSignalIds.size >= 12) Color(0xFFFF5252) else Color(0xFF00E5FF),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // کادر جستجو
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("جستجو بر اساس آدرس (I0.0, Q0.0) یا نام قطعه...", color = Color(0xFF64748B), fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF00E5FF)) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "پاک کردن", tint = Color.White)
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // چیپ‌های فیلتر دسته‌بندی
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            title = "همه",
                            isSelected = selectedCategory == null,
                            onClick = { selectedCategory = null }
                        )
                    }
                    items(SignalSourceType.values()) { type ->
                        FilterChip(
                            title = type.displayName,
                            isSelected = selectedCategory == type,
                            onClick = { selectedCategory = type }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // هشدار سقف ۱۲ سیگنال در صورت تکمیل ظرفیت
                if (selectedSignalIds.size >= 12) {
                    Surface(
                        color = Color(0xFFFF5252).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = "حداکثر سقف مجاز (۱۲ سیگنال) تکمیل شده است. جهت افزودن سیگنال جدید، ابتدا یکی از موارد قبلی را غیرفعال کنید.",
                            color = Color(0xFFFF8A80),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                // لیست سیگنال‌ها
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    if (filteredList.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "هیچ سیگنالی منطبق بر جستجو یافت نشد",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    items(filteredList, key = { it.id }) { sig ->
                        val isSelected = sig.id in selectedSignalIds
                        val canSelect = isSelected || selectedSignalIds.size < 12

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFF1E293B) else Color(0xFF131C2E)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFF00E5FF) else Color(0xFF2E3D52)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = canSelect) { onToggleSignal(sig) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { if (canSelect) onToggleSignal(sig) },
                                        enabled = canSelect,
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = Color(0xFF00E5FF),
                                            uncheckedColor = Color(0xFF64748B)
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = sig.displayName,
                                            color = if (canSelect) Color.White else Color(0xFF64748B),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = "${sig.type.displayName} • ${sig.addressOrTag}",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 10.sp
                                        )
                                    }
                                }

                                if (sig.description.isNotBlank()) {
                                    Text(
                                        text = sig.description,
                                        color = Color(0xFF64748B),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
            ) {
                Text("تأیید و اعمال", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color(0xFF131C2E)
    )
}

@Composable
private fun FilterChip(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) Color(0xFF00E5FF) else Color(0xFF1E293B),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = title,
            color = if (isSelected) Color.Black else Color(0xFFECEFF1),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}
