package com.example

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.MYKET_PURCHASE_REQUEST_CODE
import com.example.ui.screens.AboutUsScreen
import com.example.ui.screens.AcademyScreen
import com.example.ui.screens.CircuitDesignerScreen
import com.example.ui.screens.KnowledgeBaseScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PlcLadderScreen
import com.example.ui.screens.ProjectsListScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.navigation.AppNavScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.SimYarViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: SimYarViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()
            MyApplicationTheme(darkTheme = isDarkTheme) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    val context = LocalContext.current
                    val currentScreen by viewModel.currentScreen.collectAsState()
                    val isDirty by viewModel.isDirty.collectAsState()
                    val pendingUnsavedAction by viewModel.pendingUnsavedAction.collectAsState()

                    var lastBackPressTime by remember { mutableLongStateOf(0L) }

                    // بررسی تغییرات ذخیره‌نشده و خروج با ۲ بار فشردن دکمه بازگشت
                    BackHandler {
                        if (pendingUnsavedAction != null) {
                            viewModel.dismissUnsavedChangesDialog()
                        } else if (isDirty) {
                            viewModel.checkUnsavedChangesBeforeAction {
                                if (currentScreen != AppNavScreen.CIRCUIT_DESIGNER && currentScreen != AppNavScreen.SPLASH && currentScreen != AppNavScreen.ONBOARDING) {
                                    viewModel.navigateTo(AppNavScreen.CIRCUIT_DESIGNER)
                                } else {
                                    finish()
                                }
                            }
                        } else if (currentScreen != AppNavScreen.CIRCUIT_DESIGNER && currentScreen != AppNavScreen.SPLASH && currentScreen != AppNavScreen.ONBOARDING) {
                            viewModel.navigateTo(AppNavScreen.CIRCUIT_DESIGNER)
                        } else if (currentScreen == AppNavScreen.CIRCUIT_DESIGNER) {
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastBackPressTime < 2000L) {
                                finish()
                            } else {
                                lastBackPressTime = currentTime
                                Toast.makeText(context, "برای خروج از برنامه، مجدداً دکمه بازگشت را بزنید", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    when (currentScreen) {
                        AppNavScreen.SPLASH -> {
                            SplashScreen(
                                onSplashFinished = {
                                    viewModel.finishSplash()
                                }
                            )
                        }
                        AppNavScreen.ONBOARDING -> {
                            OnboardingScreen(
                                onFinishOnboarding = {
                                    viewModel.finishOnboarding()
                                }
                            )
                        }
                        else -> {
                            Scaffold(
                                modifier = Modifier.fillMaxSize(),
                                containerColor = MaterialTheme.colorScheme.background,
                                bottomBar = {
                                    // استفاده از نوار شناور مدرن
                                    SimYarBottomBar(
                                        currentScreen = currentScreen,
                                        onSelectScreen = { viewModel.navigateTo(it) }
                                    )
                                }
                            ) { innerPadding ->
                                when (currentScreen) {
                                    AppNavScreen.CIRCUIT_DESIGNER -> {
                                        CircuitDesignerScreen(
                                            viewModel = viewModel,
                                            modifier = Modifier.padding(innerPadding)
                                        )
                                    }
                                    AppNavScreen.LADDER_PLC -> {
                                        PlcLadderScreen(
                                            viewModel = viewModel,
                                            modifier = Modifier.padding(innerPadding)
                                        )
                                    }
                                    AppNavScreen.ACADEMY -> {
                                        AcademyScreen(
                                            viewModel = viewModel,
                                            modifier = Modifier.padding(innerPadding)
                                        )
                                    }
                                    AppNavScreen.PROJECTS_LIST -> {
                                        ProjectsListScreen(
                                            viewModel = viewModel,
                                            modifier = Modifier.padding(innerPadding)
                                        )
                                    }
                                    AppNavScreen.SETTINGS -> {
                                        SettingsScreen(
                                            viewModel = viewModel,
                                            modifier = Modifier.padding(innerPadding)
                                        )
                                    }
                                    AppNavScreen.KNOWLEDGE_BASE -> {
                                        KnowledgeBaseScreen(
                                            modifier = Modifier.padding(innerPadding)
                                        )
                                    }
                                    AppNavScreen.ABOUT_US -> {
                                        AboutUsScreen(
                                            modifier = Modifier.padding(innerPadding)
                                        )
                                    }
                                    else -> Unit
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.billingRepository.bindBillingService(applicationContext)
        viewModel.billingRepository.checkVipStatusOnAppStart(applicationContext)
    }

    override fun onStop() {
        super.onStop()
        viewModel.saveDraftImmediate()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val handled = viewModel.billingRepository.handleActivityResult(
            requestCode = requestCode,
            resultCode = resultCode,
            data = data,
            expectedRequestCode = MYKET_PURCHASE_REQUEST_CODE
        ) { success, message ->
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
        if (!handled) {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            viewModel.billingRepository.unbindBillingService(applicationContext)
        }
    }
}

// =========================================================================
// نوار ناوبری شناور و مدرن (Floating Glassmorphism Bottom Bar)
// =========================================================================
@Composable
fun SimYarBottomBar(
    currentScreen: AppNavScreen,
    onSelectScreen: (AppNavScreen) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp) // فاصله از لبه‌های صفحه برای حالت شناور
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp), // گوشه‌های کاملاً گرد
            color = colorScheme.surface.copy(alpha = 0.92f), // شفافیت برای افکت شیشه‌ای
            border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.5f)),
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SimYarBottomNavItem(
                    screen = AppNavScreen.PROJECTS_LIST,
                    icon = Icons.Outlined.Folder,
                    label = "پروژه‌ها",
                    isSelected = currentScreen == AppNavScreen.PROJECTS_LIST,
                    onClick = { onSelectScreen(AppNavScreen.PROJECTS_LIST) }
                )
                SimYarBottomNavItem(
                    screen = AppNavScreen.CIRCUIT_DESIGNER,
                    icon = Icons.Outlined.AccountTree,
                    label = "ویرایشگر",
                    isSelected = currentScreen == AppNavScreen.CIRCUIT_DESIGNER,
                    onClick = { onSelectScreen(AppNavScreen.CIRCUIT_DESIGNER) }
                )
                SimYarBottomNavItem(
                    screen = AppNavScreen.LADDER_PLC,
                    icon = Icons.Outlined.Memory,
                    label = "PLC",
                    isSelected = currentScreen == AppNavScreen.LADDER_PLC,
                    onClick = { onSelectScreen(AppNavScreen.LADDER_PLC) }
                )
                SimYarBottomNavItem(
                    screen = AppNavScreen.ACADEMY,
                    icon = Icons.Outlined.School,
                    label = "آموزشگاه",
                    isSelected = currentScreen == AppNavScreen.ACADEMY,
                    onClick = { onSelectScreen(AppNavScreen.ACADEMY) }
                )
                SimYarBottomNavItem(
                    screen = AppNavScreen.SETTINGS,
                    icon = Icons.Outlined.Settings,
                    label = "تنظیمات",
                    isSelected = currentScreen == AppNavScreen.SETTINGS,
                    onClick = { onSelectScreen(AppNavScreen.SETTINGS) }
                )
            }
        }
    }
}

@Composable
fun SimYarBottomNavItem(
    screen: AppNavScreen,
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val activeColor = colorScheme.primary
    val inactiveColor = colorScheme.onSurfaceVariant

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) activeColor else inactiveColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = if (isSelected) activeColor else inactiveColor,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
        Spacer(modifier = Modifier.height(2.dp))
        // نقطه نشانگر زیر دکمه فعال
        Box(
            modifier = Modifier
                .size(4.dp)
                .background(
                    if (isSelected) activeColor else Color.Transparent,
                    CircleShape
                )
        )
    }
}