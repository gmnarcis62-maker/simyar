package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CircuitProjectEntity
import com.example.navigation.AppNavScreen
import com.example.ui.components.VipUpgradeDialog
import com.example.util.PersianUtils
import com.example.viewmodel.SimYarViewModel

@Composable
fun ProjectsListScreen(
    viewModel: SimYarViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val savedProjects by viewModel.savedProjects.collectAsState()
    val isVipUnlocked by viewModel.billingRepository.isVipUnlocked.collectAsState()
    val pendingUnsavedAction by viewModel.pendingUnsavedAction.collectAsState()
    val draftRecoveryPrompt by viewModel.draftRecoveryPrompt.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }
    var showVipDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // هدر
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "مدیریت پروژه‌ها و مستندسازی نقشه",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = if (isVipUnlocked) "نسخه نامحدود مهندسی VIP فعال است" else "نسخه پایه (تا ۳ پروژه ذخیره‌شده)",
                        color = if (isVipUnlocked) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // ایجاد پروژه جدید
                    IconButton(onClick = {
                        viewModel.newProject()
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "پروژه جدید", tint = MaterialTheme.colorScheme.secondary)
                    }

                    // اشتراک‌گذاری متنی / فایل پروژه
                    IconButton(onClick = {
                        val json = viewModel.exportProjectAsJson()
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "فایل پروژه نقشه برق صنعتی سیمیار")
                            putExtra(Intent.EXTRA_TEXT, json)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "اشتراک‌گذاری پروژه سیمیار"))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "اشتراک‌گذاری", tint = MaterialTheme.colorScheme.primary)
                    }

                    // ورود JSON
                    IconButton(onClick = { showImportDialog = true }) {
                        Icon(Icons.Default.FileUpload, contentDescription = "ورود JSON", tint = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }

        // فیلد جستجو در پروژه‌ها
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("جستجو در نام و توضیحات پروژه‌ها...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        )

        // پروژه‌های فیلتر شده
        val filteredProjects = savedProjects.filter {
            it.title.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true)
        }

        if (filteredProjects.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) "موردی با این عبارت یافت نشد" else "هنوز پروژه‌ای ذخیره نکرده‌اید",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredProjects, key = { it.id }) { proj ->
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2637)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.checkUnsavedChangesBeforeAction {
                                    viewModel.loadProject(proj)
                                    Toast.makeText(context, "پروژه بارگذاری شد", Toast.LENGTH_SHORT).show()
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = proj.title,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                if (proj.description.isNotBlank()) {
                                    Text(
                                        text = proj.description,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp
                                    )
                                }
                                val jalali = PersianUtils.toPersianDigits(PersianUtils.toJalaliDate(proj.updatedAt))
                                Text(
                                    text = "تاریخ شمسی: $jalali | طراح: مهندس برق",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 10.sp
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // تکثیر پروژه (Duplicate)
                                IconButton(onClick = {
                                    if (!isVipUnlocked && savedProjects.size >= 3) {
                                        showVipDialog = true
                                    } else {
                                        viewModel.duplicateProject(proj)
                                        Toast.makeText(context, "پروژه تکثیر شد", Toast.LENGTH_SHORT).show()
                                    }
                                }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "تکثیر", tint = Color(0xFFFFD600), modifier = Modifier.size(18.dp))
                                }

                                // حذف پروژه
                                IconButton(onClick = { viewModel.deleteProject(proj.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف پروژه", tint = Color(0xFFFF5252), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showVipDialog) {
        VipUpgradeDialog(
            billingRepository = viewModel.billingRepository,
            onDismissRequest = { showVipDialog = false }
        )
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("وارد کردن پروژه از JSON", color = Color.White) },
            text = {
                Column {
                    Text(
                        text = "کد متنی JSON پروژه را در زیر جای‌گذاری کنید:",
                        color = Color(0xFFB0BEC5),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (!isVipUnlocked && !viewModel.billingRepository.canCreateNewProject(savedProjects.size)) {
                        showImportDialog = false
                        showVipDialog = true
                    } else {
                        viewModel.checkUnsavedChangesBeforeAction {
                            val success = viewModel.importProjectFromJson(importJsonText)
                            if (success) {
                                showImportDialog = false
                                viewModel.navigateTo(AppNavScreen.CIRCUIT_DESIGNER)
                                Toast.makeText(context, "پروژه با موفقیت وارد شد", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "خطا در قالب فایل JSON", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }) {
                    Text("وارد کردن")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
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
}
