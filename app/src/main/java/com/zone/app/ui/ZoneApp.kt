package com.zone.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.List
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.zone.app.data.ZoneStore
import com.zone.app.ui.screens.AdminScreen
import com.zone.app.ui.screens.DiaryScreen
import com.zone.app.ui.screens.HabitsScreen
import com.zone.app.ui.screens.LinksScreen
import com.zone.app.ui.screens.ListDetailScreen
import com.zone.app.ui.screens.ListsScreen
import com.zone.app.ui.screens.MileageScreen
import com.zone.app.ui.screens.MoneyScreen
import com.zone.app.ui.screens.MoreScreen
import com.zone.app.ui.screens.NotesScreen
import com.zone.app.ui.screens.OverviewScreen
import com.zone.app.ui.screens.PassbookScreen
import com.zone.app.ui.screens.SettingsScreen
import com.zone.app.ui.screens.SplitDetailScreen
import com.zone.app.ui.screens.SplitScreen
import com.zone.app.ui.screens.SubscriptionsScreen
import com.zone.app.ui.screens.WatchlistScreen
import com.zone.app.ui.theme.ZoneTheme
import com.zone.app.ui.theme.zonePalette

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val TABS = listOf(
    Tab("home", "Overview", Icons.Rounded.GridView),
    Tab("diary", "Diary", Icons.Rounded.Book),
    Tab("lists", "Lists", Icons.Rounded.List),
    Tab("money", "Money", Icons.Rounded.Wallet),
    Tab("more", "More", Icons.Rounded.Apps)
)

@Composable
fun ZoneApp() {
    var theme by rememberSaveable { mutableStateOf(ZoneStore.settings().theme) }

    ZoneTheme(theme = theme) {
        val palette = zonePalette()
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route.orEmpty()
        val baseRoute = currentRoute.substringBefore('/')
        val showBottomBar = baseRoute in TABS.map { it.route }

        Scaffold(
            modifier = Modifier.fillMaxSize().background(palette.background),
            containerColor = palette.background,
            bottomBar = {
                if (showBottomBar) {
                    ZoneBottomBar(
                        currentRoute = baseRoute,
                        onTabSelected = { route ->
                            navController.navigate(route) {
                                popUpTo("home") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("home") { OverviewScreen(onNavigate = { route -> navController.navigate(route) }) }
                composable("diary") { DiaryScreen() }
                composable("lists") { ListsScreen(onOpenList = { id -> navController.navigate("lists/$id") }) }
                composable(
                    "lists/{listId}",
                    arguments = listOf(navArgument("listId") { type = NavType.StringType })
                ) { entry ->
                    val listId = entry.arguments?.getString("listId").orEmpty()
                    ListDetailScreen(listId = listId, onBack = { navController.popBackStack() })
                }
                composable("money") { MoneyScreen() }
                composable("more") { MoreScreen(onNavigate = { route -> navController.navigate(route) }) }
                composable("habits") { HabitsScreen() }
                composable("notes") { NotesScreen() }
                composable("passbook") { PassbookScreen() }
                composable("split") { SplitScreen(onOpenGroup = { id -> navController.navigate("split/$id") }) }
                composable(
                    "split/{groupId}",
                    arguments = listOf(navArgument("groupId") { type = NavType.StringType })
                ) { entry ->
                    val groupId = entry.arguments?.getString("groupId").orEmpty()
                    SplitDetailScreen(groupId = groupId, onBack = { navController.popBackStack() })
                }
                composable("mileage") { MileageScreen() }
                composable("watchlist") { WatchlistScreen() }
                composable("links") { LinksScreen() }
                composable("subscriptions") { SubscriptionsScreen() }
                composable("settings") {
                    SettingsScreen(onThemeChanged = { selected ->
                        theme = selected
                    })
                }
                composable("admin") { AdminScreen() }
            }
        }
    }
}

@Composable
private fun ZoneBottomBar(currentRoute: String, onTabSelected: (String) -> Unit) {
    val palette = zonePalette()
    NavigationBar(
        containerColor = palette.surface,
        tonalElevation = 0.dp
    ) {
        TABS.forEach { tab ->
            val selected = tab.route == currentRoute
            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(tab.route) },
                icon = {
                    Icon(
                        tab.icon,
                        contentDescription = tab.label,
                        modifier = Modifier
                    )
                },
                label = { Text(tab.label, style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = palette.accent,
                    selectedTextColor = palette.accent,
                    unselectedIconColor = palette.secondary,
                    unselectedTextColor = palette.secondary,
                    indicatorColor = palette.accent.copy(alpha = 0.12f)
                )
            )
        }
    }
}
