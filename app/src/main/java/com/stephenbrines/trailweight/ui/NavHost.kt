package com.stephenbrines.trailweight.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.stephenbrines.trailweight.ui.gear.GearListScreen
import com.stephenbrines.trailweight.ui.settings.SyncSettingsScreen
import com.stephenbrines.trailweight.ui.trips.PackListScreen
import com.stephenbrines.trailweight.ui.trips.TripDetailScreen
import com.stephenbrines.trailweight.ui.trips.TripListScreen
import com.stephenbrines.trailweight.ui.trips.TripViewModel
import com.stephenbrines.trailweight.ui.weight.WeightDashboardScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.PaddingValues

sealed class Screen(val route: String, val label: String) {
    object Gear : Screen("gear", "Gear")
    object Trips : Screen("trips", "Trips")
    object Weight : Screen("weight", "Weight")
    object Sync : Screen("sync", "Sync")
}

@Composable
fun TrailWeightNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = navBackStackEntry?.destination

    val tabs = listOf(Screen.Gear, Screen.Trips, Screen.Weight, Screen.Sync)
    val tabIcons = listOf(Icons.Default.Backpack, Icons.Default.Map, Icons.Default.Scale, Icons.Default.CloudSync)

    // Only show bottom nav on top-level destinations
    val topLevelRoutes = tabs.map { it.route }.toSet()
    val showBottomBar = currentDest?.route in topLevelRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    tabs.forEachIndexed { index, screen ->
                        NavigationBarItem(
                            icon = { Icon(tabIcons[index], contentDescription = screen.label) },
                            label = { Text(screen.label) },
                            selected = currentDest?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
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
        NavHost(navController = navController, startDestination = Screen.Gear.route) {
            composable(Screen.Gear.route) {
                GearListScreen(navController, padding)
            }
            composable(Screen.Trips.route) {
                TripListScreen(navController, padding)
            }
            composable(Screen.Weight.route) {
                WeightDashboardScreen(padding)
            }
            composable(Screen.Sync.route) {
                SyncSettingsScreen()
            }
            composable(
                route = "trip/{tripId}",
                arguments = listOf(navArgument("tripId") { type = NavType.StringType })
            ) { backStack ->
                val tripId = backStack.arguments?.getString("tripId") ?: return@composable
                val viewModel: TripViewModel = hiltViewModel()
                val trips by viewModel.state.collectAsStateWithLifecycle()
                val trip = trips.trips.firstOrNull { it.id == tripId } ?: return@composable
                TripDetailScreen(trip = trip, navController = navController)
            }
            composable(
                route = "packlist/{packListId}",
                arguments = listOf(navArgument("packListId") { type = NavType.StringType })
            ) { backStack ->
                val packListId = backStack.arguments?.getString("packListId") ?: return@composable
                PackListScreen(packListId = packListId, navController = navController)
            }
        }
    }
}
