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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AcademyLesson
import com.example.model.AcademyLessonsData
import com.example.navigation.AppNavScreen
import com.example.ui.components.VipUpgradeDialog
import com.example.util.PersianUtils
import com.example.viewmodel.SimYarViewModel

@Composable
fun AcademyScreen(
    viewModel: SimYarViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isVipUnlocked by viewModel.billingRepository.isVipUnlocked.collectAsState()
    var selectedLessonForReview by remember { mutableStateOf<AcademyLesson?>(null) }
    var showVipDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C131F))
    ) {
        // هدر آموزشگاه
        Surface(
            color = Color(0xFF141D2B),
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
                        .background(Color(0xFFFFD600), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = "آیکون آموزشگاه",
                        tint = Color.Black
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "آموزشگاه گام‌به‌گام مدار فرمان صنعتی",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "۱۵ درس تخصصی (۵ درس اول رایگان + ۱۰ درس پیشرفته VIP)",
                        color = Color(0xFF90A4AE),
                        fontSize = 11.sp
                    )
                }
            }
        }

        // لیست دروس مرحله‌ای
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(AcademyLessonsData.lessons, key = { it.id }) { lesson ->
                val isLocked = lesson.isVipOnly && !isVipUnlocked

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isLocked) Color(0xFF151C26) else Color(0xFF182335)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isLocked) Color(0xFF2E3A4A) else Color(0xFF00E5FF).copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "درس ${lesson.id}: ${lesson.title} - ${if (isLocked) "نیازمند اشتراک VIP" else "رایگان"}" }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = lesson.title,
                                color = if (isLocked) Color(0xFF90A4AE) else Color(0xFF00E5FF),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )

                            if (isLocked) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(Color(0xFF37474F), RoundedCornerShape(12.dp))
                                        .clickable { showVipDialog = true }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "نسخه ویژه",
                                        tint = Color(0xFFFFD600),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "VIP",
                                        color = Color(0xFFFFD600),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0x2200E676), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = lesson.level,
                                        color = Color(0xFF00E676),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = lesson.description,
                            color = Color(0xFFCFD8DC),
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "هدف درس: ${lesson.objectiveText}",
                            color = Color(0xFF80CBC4),
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // دکمه‌های اکشن
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isLocked) {
                                Button(
                                    onClick = { showVipDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD600)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("ارتقا به VIP مایکت", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.loadProjectFromLesson(lesson)
                                        viewModel.navigateTo(AppNavScreen.CIRCUIT_DESIGNER)
                                        Toast.makeText(context, "مدار درس '${lesson.title}' روی بوم باز شد", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color(0xFF00E5FF),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تمرین روی بوم", color = Color(0xFF00E5FF), fontSize = 11.sp)
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
}
