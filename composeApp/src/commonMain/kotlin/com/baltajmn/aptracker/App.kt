package com.baltajmn.aptracker

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.baltajmn.aptracker.core.di.appModules
import com.baltajmn.aptracker.core.navigation.AddRoomRoute
import com.baltajmn.aptracker.core.navigation.AuthRoute
import com.baltajmn.aptracker.core.navigation.NotificationPrefsRoute
import com.baltajmn.aptracker.core.navigation.RoomDetailRoute
import com.baltajmn.aptracker.core.navigation.RoomListRoute
import com.baltajmn.aptracker.core.navigation.SettingsRoute
import com.baltajmn.aptracker.core.navigation.SlotDetailRoute
import com.baltajmn.aptracker.core.ui.AppTheme
import com.baltajmn.aptracker.feature.auth.AuthScreen
import com.baltajmn.aptracker.feature.notifications.NotificationPrefsScreen
import com.baltajmn.aptracker.feature.rooms.AddRoomScreen
import com.baltajmn.aptracker.feature.rooms.RoomDetailScreen
import com.baltajmn.aptracker.feature.rooms.RoomListScreen
import com.baltajmn.aptracker.feature.settings.SettingsScreen
import com.baltajmn.aptracker.feature.slots.SlotDetailScreen
import org.koin.compose.KoinApplication

@Composable
fun App() {
    KoinApplication(application = { modules(appModules) }) {
        AppTheme {
            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            val showBottomBar = currentDestination?.let { dest ->
                dest.hasRoute(RoomListRoute::class) ||
                        dest.hasRoute(NotificationPrefsRoute::class) ||
                        dest.hasRoute(SettingsRoute::class)
            } ?: false

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    if (showBottomBar) {
                        AppBottomBar(navController, currentDestination)
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = AuthRoute,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    enterTransition = { slideInHorizontally(tween(220)) { it / 3 } + fadeIn(tween(220)) },
                    exitTransition = { slideOutHorizontally(tween(220)) { -it / 3 } + fadeOut(tween(220)) },
                    popEnterTransition = { slideInHorizontally(tween(220)) { -it / 3 } + fadeIn(tween(220)) },
                    popExitTransition = { slideOutHorizontally(tween(220)) { it / 3 } + fadeOut(tween(220)) }
                ) {
                    composable<AuthRoute> { AuthScreen(navController) }
                    composable<RoomListRoute> { RoomListScreen(navController) }
                    composable<RoomDetailRoute> {
                        RoomDetailScreen(navController)
                    }
                    composable<AddRoomRoute> {
                        AddRoomScreen(navController)
                    }
                    composable<SlotDetailRoute> {
                        SlotDetailScreen(navController)
                    }
                    composable<NotificationPrefsRoute> { NotificationPrefsScreen(navController) }
                    composable<SettingsRoute> { SettingsScreen(navController) }
                }
            }
        }
    }
}

@Composable
private fun AppBottomBar(
    navController: NavController,
    currentDestination: NavDestination?
) {
    val navItemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.primary,
        selectedTextColor = MaterialTheme.colorScheme.primary,
        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Rooms") },
            label = { Text("Rooms") },
            selected = currentDestination?.hasRoute(RoomListRoute::class) == true,
            onClick = {
                navController.navigate(RoomListRoute) {
                    popUpTo(RoomListRoute) { inclusive = true }
                }
            },
            colors = navItemColors
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Notifications, contentDescription = "Notifications") },
            label = { Text("Notifications") },
            selected = currentDestination?.hasRoute(NotificationPrefsRoute::class) == true,
            onClick = {
                navController.navigate(NotificationPrefsRoute) {
                    popUpTo(RoomListRoute)
                }
            },
            colors = navItemColors
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings") },
            selected = currentDestination?.hasRoute(SettingsRoute::class) == true,
            onClick = {
                navController.navigate(SettingsRoute) {
                    popUpTo(RoomListRoute)
                }
            },
            colors = navItemColors
        )
    }
}
