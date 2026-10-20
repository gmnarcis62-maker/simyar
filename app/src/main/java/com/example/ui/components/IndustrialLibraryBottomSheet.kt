package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ComponentType
import com.example.model.IndustrialComponentLibrary
import com.example.model.TechnicalSpecs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndustrialLibraryBottomSheet(
    onDismissRequest: () -> Unit,
    onComponentSelected: (ComponentType, TechnicalSpecs) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val categories = IndustrialComponentLibrary.getByCategory()
    val categoryNames = listOf("همه") + categories.keys.toList()
    var selectedCategory by remember { mutableStateOf("همه") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedItemForDetail by remember { mutableStateOf<IndustrialComponentLibrary.LibraryItem?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // هدر کتابخانه قطعات
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ElectricalServices,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "کتابخانه قطعات برق صنعتی",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "مشخصات فنی، استاندارد IEC و ترمینال‌های صنعتی",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }

                IconButton(onClick = onDismissRequest) {
                    Icon(Icons.Default.Close, contentDescription = "بستن", tint = MaterialTheme.colorScheme.onSurface)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // نوار دسته‌بندی‌ها
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categoryNames) { cat ->
                    val isSelected = cat == selectedCategory
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.clickable { selectedCategory = cat }
                    ) {
                        Text(
                            text = cat,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // لیست قطعات فیلتر شده
            val filteredItems = IndustrialComponentLibrary.items.filter { item ->
                (selectedCategory == "همه" || item.category == selectedCategory) &&
                        (item.displayName.contains(searchQuery, ignoreCase = true) ||
                                item.category.contains(searchQuery, ignoreCase = true) ||
                                item.defaultSpecs.standardCode.contains(searchQuery, ignoreCase = true))
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(filteredItems, key = { it.type.name }) { item ->
                    ComponentLibraryCard(
                        item = item,
                        onAdd = {
                            onComponentSelected(item.type, item.defaultSpecs)
                            onDismissRequest()
                        },
                        onShowDetail = {
                            selectedItemForDetail = item
                        }
                    )
                }
            }
        }
    }

    // دیالوگ جزئیات فنی قطعه
    selectedItemForDetail?.let { item ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { selectedItemForDetail = null },
            title = {
                Text(
                    text = item.displayName,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = item.description,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            SpecRow("کد استاندارد:", item.defaultSpecs.standardCode)
                            SpecRow("ولتاژ نامی:", item.defaultSpecs.ratedVoltage)
                            SpecRow("جریان نامی:", item.defaultSpecs.ratedCurrent)
                            SpecRow("کنتاکت‌های باز (NO):", "${item.defaultSpecs.contactsNO}")
                            SpecRow("کنتاکت‌های بسته (NC):", "${item.defaultSpecs.contactsNC}")
                            if (item.defaultSpecs.powerRating != "-") {
                                SpecRow("توان / قدرت:", item.defaultSpecs.powerRating)
                            }
                            if (item.defaultSpecs.isSampleValue) {
                                SpecRow("وضعیت مقادیر:", "مقدار نمونه (قابل ویرایش)")
                            }
                            if (item.defaultSpecs.manufacturerModel.isNotBlank()) {
                                SpecRow("مدل تجهیز سازنده:", item.defaultSpecs.manufacturerModel)
                            }
                            if (item.defaultSpecs.datasheetUrl.isNotBlank()) {
                                SpecRow("لینک دیتاشیت رسمی:", item.defaultSpecs.datasheetUrl)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onComponentSelected(item.type, item.defaultSpecs)
                        selectedItemForDetail = null
                        onDismissRequest()
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("افزودن به بوم مدار")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { selectedItemForDetail = null }) {
                    Text("بستن")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
private fun ComponentLibraryCard(
    item: IndustrialComponentLibrary.LibraryItem,
    onAdd: () -> Unit,
    onShowDetail: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // نماد و توضیحات
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // آیکون / نماد مداری
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.symbolAscii,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = item.displayName,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "${item.category} • ${item.defaultSpecs.standardCode}",
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "Vn: ${item.defaultSpecs.ratedVoltage} | In: ${item.defaultSpecs.ratedCurrent}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }

            // اکشن‌ها
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onShowDetail) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "مشخصات فنی",
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                }

                Button(
                    onClick = onAdd,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("درج", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun SpecRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        Text(text = value, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}
