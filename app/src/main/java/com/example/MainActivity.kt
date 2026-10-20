package com.example

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
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

@Composable
fun SimYarBottomBar(
    currentScreen: AppNavScreen,
    onSelectScreen: (AppNavScreen) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    NavigationBar(
        containerColor = colorScheme.surface,
        contentColor = colorScheme.onSurface,
        tonalElevation = 3.dp
    ) {
        NavigationBarItem(
            selected = currentScreen == AppNavScreen.PROJECTS_LIST,
            onClick = { onSelectScreen(AppNavScreen.PROJECTS_LIST) },
            icon = { Icon(Icons.Outlined.Folder, contentDescription = "پروژه‌ها") },
            label = { Text("پروژه‌ها", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = colorScheme.onPrimary,
                selectedTextColor = colorScheme.primary,
                indicatorColor = colorScheme.primary,
                unselectedIconColor = colorScheme.onSurfaceVariant,
                unselectedTextColor = colorScheme.onSurfaceVariant
            )
        )

        NavigationBarItem(
            selected = currentScreen == AppNavScreen.CIRCUIT_DESIGNER,
            onClick = { onSelectScreen(AppNavScreen.CIRCUIT_DESIGNER) },
            icon = { Icon(Icons.Outlined.AccountTree, contentDescription = "طراحی مدار") },
            label = { Text("ویرایشگر", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = colorScheme.onPrimary,
                selectedTextColor = colorScheme.primary,
                indicatorColor = colorScheme.primary,
                unselectedIconColor = colorScheme.onSurfaceVariant,
                unselectedTextColor = colorScheme.onSurfaceVariant
            )
        )

        NavigationBarItem(
            selected = currentScreen == AppNavScreen.LADDER_PLC,
            onClick = { onSelectScreen(AppNavScreen.LADDER_PLC) },
            icon = { Icon(Icons.Outlined.Memory, contentDescription = "PLC") },
            label = { Text("پی‌ال‌سی", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = colorScheme.onPrimary,
                selectedTextColor = colorScheme.primary,
                indicatorColor = colorScheme.primary,
                unselectedIconColor = colorScheme.onSurfaceVariant,
                unselectedTextColor = colorScheme.onSurfaceVariant
            )
        )

        NavigationBarItem(
            selected = currentScreen == AppNavScreen.ACADEMY,
            onClick = { onSelectScreen(AppNavScreen.ACADEMY) },
            icon = { Icon(Icons.Outlined.School, contentDescription = "آموزشگاه") },
            label = { Text("آموزشگاه", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = colorScheme.onPrimary,
                selectedTextColor = colorScheme.primary,
                indicatorColor = colorScheme.primary,
                unselectedIconColor = colorScheme.onSurfaceVariant,
                unselectedTextColor = colorScheme.onSurfaceVariant
            )
        )

        NavigationBarItem(
            selected = currentScreen == AppNavScreen.SETTINGS,
            onClick = { onSelectScreen(AppNavScreen.SETTINGS) },
            icon = { Icon(Icons.Outlined.Settings, contentDescription = "تنظیمات") },
            label = { Text("تنظیمات", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = colorScheme.onPrimary,
                selectedTextColor = colorScheme.primary,
                indicatorColor = colorScheme.primary,
                unselectedIconColor = colorScheme.onSurfaceVariant,
                unselectedTextColor = colorScheme.onSurfaceVariant
            )
        )
    }
}
