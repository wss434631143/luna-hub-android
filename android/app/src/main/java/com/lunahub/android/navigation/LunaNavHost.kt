package com.lunahub.android.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lunahub.android.feature.camera.CameraConnectRoute
import com.lunahub.android.feature.download.DownloadRoute
import com.lunahub.android.feature.home.HomeRoute
import com.lunahub.android.feature.library.LibraryRoute
import com.lunahub.android.feature.preview.PreviewRoute
import com.lunahub.android.feature.settings.SettingsRoute

object LunaRoutes {
    const val Home = "home"
    const val Connect = "camera/connect"
    const val Library = "library"
    const val Preview = "preview/{mediaId}"
    const val Download = "download"
    const val Settings = "settings"

    fun preview(mediaId: String): String = "preview/$mediaId"
}

private data class BottomDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val BottomDestinations = listOf(
    BottomDestination(LunaRoutes.Home, "首页", Icons.Outlined.Home),
    BottomDestination(LunaRoutes.Library, "素材", Icons.Outlined.PhotoLibrary),
    BottomDestination(LunaRoutes.Download, "下载", Icons.Outlined.CloudDownload),
    BottomDestination(LunaRoutes.Settings, "设置", Icons.Outlined.Settings),
)

@Composable
fun LunaNavHost(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = BottomDestinations.any { destination ->
        currentDestination?.hierarchy?.any { it.route == destination.route } == true
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                ) {
                    BottomDestinations.forEach { destination ->
                        val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (destination.route == LunaRoutes.Home) {
                                    navController.navigate(LunaRoutes.Home) {
                                        popUpTo(LunaRoutes.Home) {
                                            inclusive = false
                                        }
                                        launchSingleTop = true
                                    }
                                } else {
                                    navController.navigate(destination.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = LunaRoutes.Home,
            modifier = Modifier.padding(padding),
        ) {
            composable(LunaRoutes.Home) {
                HomeRoute(
                    onConnectClick = { navController.navigate(LunaRoutes.Connect) },
                    onLibraryClick = { navController.navigate(LunaRoutes.Library) },
                    onDownloadsClick = { navController.navigate(LunaRoutes.Download) },
                    onSettingsClick = { navController.navigate(LunaRoutes.Settings) },
                )
            }
            composable(LunaRoutes.Connect) {
                CameraConnectRoute(onOpenLibrary = { navController.navigate(LunaRoutes.Library) })
            }
            composable(LunaRoutes.Library) {
                LibraryRoute(onMediaClick = { mediaId -> navController.navigate(LunaRoutes.preview(mediaId)) })
            }
            composable(LunaRoutes.Preview) {
                PreviewRoute(onBack = { navController.popBackStack() })
            }
            composable(LunaRoutes.Download) {
                DownloadRoute()
            }
            composable(LunaRoutes.Settings) {
                SettingsRoute()
            }
        }
    }
}
