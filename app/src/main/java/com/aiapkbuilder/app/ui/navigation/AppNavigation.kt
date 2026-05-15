package com.aiapkbuilder.app.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aiapkbuilder.app.ui.screens.*

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Generate : Screen("generate")
    object FeatureSelection : Screen("feature_selection")
    object Preview : Screen("preview")
    object Projects : Screen("projects")
    object ProjectDetail : Screen("project/{projectId}") {
        fun createRoute(id: String) = "project/$id"
    }
    object BuildLog : Screen("buildlog/{projectId}") {
        fun createRoute(id: String) = "buildlog/$id"
    }
    object Settings : Screen("settings")
    object Templates : Screen("templates")
    object Downloads : Screen("downloads")
    object History : Screen("history/{projectId}") {
        fun createRoute(id: String) = "history/$id"
    }
    object Storage : Screen("storage")
}

data class BottomNavItem(
    val label: String,
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem("Home", Screen.Home.route, Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem("Generate", Screen.Generate.route, Icons.Filled.Add, Icons.Outlined.Add),
    BottomNavItem("Projects", Screen.Projects.route, Icons.Filled.Terminal, Icons.Outlined.Terminal),
    BottomNavItem("Settings", Screen.Settings.route, Icons.Filled.Settings, Icons.Outlined.Settings)
)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = bottomNavItems.any { it.route == currentDestination?.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) },
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(padding),
            enterTransition = { fadeIn(tween(280)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(280)) },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(280)) },
            popExitTransition = { fadeOut(tween(200)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(280)) }
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToGenerate = { navController.navigate(Screen.Generate.route) },
                    onNavigateToProject = { navController.navigate(Screen.ProjectDetail.createRoute(it)) },
                    onNavigateToTemplates = { navController.navigate(Screen.Templates.route) }
                )
            }
            composable(Screen.Generate.route) {
                GenerateScreen(
                    onNavigateToProject = { navController.navigate(Screen.ProjectDetail.createRoute(it)) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToFeatures = { navController.navigate(Screen.FeatureSelection.route) }
                )
            }
            composable(Screen.Projects.route) {
                ProjectsScreen(
                    onNavigateToProject = { navController.navigate(Screen.ProjectDetail.createRoute(it)) }
                )
            }
            composable(
                Screen.ProjectDetail.route,
                arguments = listOf(navArgument("projectId") { type = NavType.StringType })
            ) {
                val id = it.arguments?.getString("projectId") ?: return@composable
                ProjectDetailScreen(
                    projectId = id,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToBuildLog = { navController.navigate(Screen.BuildLog.createRoute(id)) },
                    onNavigateToDownloads = { navController.navigate(Screen.Downloads.route) },
                    onNavigateToHistory = { projectId -> navController.navigate(Screen.History.createRoute(projectId)) }
                )
            }
            composable(
                Screen.BuildLog.route,
                arguments = listOf(navArgument("projectId") { type = NavType.StringType })
            ) {
                val id = it.arguments?.getString("projectId") ?: return@composable
                BuildLogScreen(projectId = id, onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.Settings.route) {
                SettingsScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.Templates.route) {
                TemplatesScreen(
                    onNavigateToGenerate = { navController.navigate(Screen.Generate.route) },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.FeatureSelection.route) {
                FeatureSelectionScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPreview = { navController.navigate(Screen.Preview.route) }
                )
            }
            composable(Screen.Preview.route) {
                PreviewScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onGenerate = {
                        navController.popBackStack(Screen.Generate.route, false)
                    }
                )
            }
            composable(Screen.Downloads.route) {
                DownloadScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(
                Screen.History.route,
                arguments = listOf(navArgument("projectId") { type = NavType.StringType })
            ) {
                val id = it.arguments?.getString("projectId") ?: return@composable
                HistoryScreen(
                    projectId = id,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Storage.route) {
                StorageScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}