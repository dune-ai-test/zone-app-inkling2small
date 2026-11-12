package com.zone.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.List
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Note
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ShieldMoon
import androidx.compose.material.icons.rounded.CallSplit
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/* ==========================================================================
   The desk — the tool registry, grouped like the web app's bento board.
   ========================================================================== */

data class DeskTool(
    val id: String,
    val route: String,
    val label: String,
    val desc: String,
    val icon: ImageVector,
    val tint: Color,
    val group: String
)

object Desk {
    val TOOLS = listOf(
        DeskTool("diary", "diary", "Diary", "A daily record of what happened", Icons.Rounded.Book, Color(0xFFFF9500), "Personal"),
        DeskTool("lists", "lists", "Lists", "Checklists with steps", Icons.Rounded.List, Color(0xFF34C759), "Personal"),
        DeskTool("habits", "habits", "Habits", "Small repetitions, made visible", Icons.Rounded.Repeat, Color(0xFFAF52DE), "Personal"),
        DeskTool("notes", "notes", "Notes", "A scratchpad with a memory", Icons.Rounded.Note, Color(0xFFFFCC00), "Personal"),
        DeskTool("money", "money", "Money", "Where the month goes", Icons.Rounded.Wallet, Color(0xFF007AFF), "Money"),
        DeskTool("passbook", "passbook", "Passbook", "Every account, one statement", Icons.Rounded.MenuBook, Color(0xFF5856D6), "Money"),
        DeskTool("split", "split", "Split", "Shared costs, no arithmetic", Icons.Rounded.CallSplit, Color(0xFFFF3B30), "Money"),
        DeskTool("mileage", "mileage", "Mileage", "Distance, fuel, cost per km", Icons.Rounded.DirectionsCar, Color(0xFFFF2D55), "Money"),
        DeskTool("watchlist", "watchlist", "Watchlist", "What to watch next", Icons.Rounded.Movie, Color(0xFF5AC8FA), "Library"),
        DeskTool("links", "links", "Links", "The good corners of the web", Icons.Rounded.Link, Color(0xFF64D2FF), "Library"),
        DeskTool("subscriptions", "subscriptions", "Subscriptions", "The quiet monthly cost", Icons.Rounded.CreditCard, Color(0xFFFF9F0A), "Library"),
        DeskTool("settings", "settings", "Settings", "Appearance and display currency", Icons.Rounded.Settings, Color(0xFF8E8E93), "System"),
        DeskTool("admin", "admin", "Admin", "Activity, backups, back office", Icons.Rounded.ShieldMoon, Color(0xFF636366), "System")
    )

    val GROUPS = listOf("All", "Personal", "Money", "Library", "System")
    val TODAY = Icons.Rounded.CalendarToday
}
