package com.example.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.sp
import com.example.ui.screens.CameraViewScreen
import com.example.ui.screens.GalleryScreen
import com.example.ui.screens.SecurityDashboardScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.LiveRed
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.viewmodel.SecurityViewModel

enum class NavTab(val title: String, val icon: ImageVector, val tag: String) {
    CAMERA("Camera", Icons.Default.Videocam, "nav_tab_camera"),
    DASHBOARD("Dashboard", Icons.Default.Dashboard, "nav_tab_dashboard"),
    GALLERY("Gallery", Icons.Default.PhotoLibrary, "nav_tab_gallery"),
    SETTINGS("Settings", Icons.Default.Settings, "nav_tab_settings")
}

@Composable
fun MainNavContainer(
    viewModel: SecurityViewModel
) {
    var selectedTab by remember { mutableStateOf(NavTab.CAMERA) }

    val uiState by viewModel.uiState.collectAsState()
    val mediaItems by viewModel.mediaItems.collectAsState()
    val motionEvents by viewModel.motionEvents.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = SlateCard,
                contentColor = TextPrimary
            ) {
                NavTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        icon = {
                            if (tab == NavTab.DASHBOARD && uiState.isMotionDetected) {
                                BadgedBox(
                                    badge = {
                                        Badge(containerColor = LiveRed)
                                    }
                                ) {
                                    Icon(imageVector = tab.icon, contentDescription = tab.title)
                                }
                            } else if (tab == NavTab.GALLERY && mediaItems.isNotEmpty()) {
                                BadgedBox(
                                    badge = {
                                        Badge(containerColor = CyanAccent) {
                                            Text(mediaItems.size.toString(), fontSize = 9.sp, color = SlateDark)
                                        }
                                    }
                                ) {
                                    Icon(imageVector = tab.icon, contentDescription = tab.title)
                                }
                            } else {
                                Icon(imageVector = tab.icon, contentDescription = tab.title)
                            }
                        },
                        label = {
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = SlateDark,
                            selectedTextColor = CyanAccent,
                            indicatorColor = CyanAccent,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        ),
                        modifier = Modifier.testTag(tab.tag)
                    )
                }
            }
        }
    ) { innerPadding ->
        val screenModifier = Modifier.padding(innerPadding)

        when (selectedTab) {
            NavTab.CAMERA -> CameraViewScreen(
                viewModel = viewModel,
                uiState = uiState,
                modifier = screenModifier
            )
            NavTab.DASHBOARD -> SecurityDashboardScreen(
                viewModel = viewModel,
                uiState = uiState,
                motionEvents = motionEvents,
                modifier = screenModifier
            )
            NavTab.GALLERY -> GalleryScreen(
                viewModel = viewModel,
                uiState = uiState,
                mediaItems = mediaItems,
                modifier = screenModifier
            )
            NavTab.SETTINGS -> SettingsScreen(
                viewModel = viewModel,
                uiState = uiState,
                modifier = screenModifier
            )
        }
    }
}
