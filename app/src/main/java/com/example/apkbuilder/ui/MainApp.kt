package com.example.apkbuilder.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.apkbuilder.ui.screens.ApkLibraryScreen
import com.example.apkbuilder.ui.screens.BuildScreen
import com.example.apkbuilder.ui.screens.HomeScreen
import com.example.apkbuilder.ui.screens.SettingsScreen
import com.example.apkbuilder.viewmodel.ApkBuilderViewModel

enum class AppTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    BUILDER("Builder", Icons.Filled.Build, Icons.Outlined.Build),
    TERMINAL("Terminal", Icons.Filled.Terminal, Icons.Outlined.Terminal),
    APKS("APKs", Icons.Filled.Android, Icons.Outlined.Android),
    SETTINGS("Optionen", Icons.Filled.Settings, Icons.Outlined.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    viewModel: ApkBuilderViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(AppTab.BUILDER) }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                AppTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.title
                            )
                        },
                        label = { Text(tab.title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                AppTab.BUILDER -> HomeScreen(
                    viewModel = viewModel,
                    onNavigateToTerminal = { selectedTab = AppTab.TERMINAL }
                )
                AppTab.TERMINAL -> BuildScreen(viewModel = viewModel)
                AppTab.APKS -> ApkLibraryScreen(
                    viewModel = viewModel,
                    onNavigateToHome = { selectedTab = AppTab.BUILDER }
                )
                AppTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
            }
        }
    }
}
