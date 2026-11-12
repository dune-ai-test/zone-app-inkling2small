package com.zone.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyColumnScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zone.app.data.DiaryEntry
import com.zone.app.data.Fill
import com.zone.app.data.MoneyData
import com.zone.app.data.ZoneList
import com.zone.app.data.ZoneStore
import com.zone.app.ui.components.LargeTitle
import com.zone.app.ui.components.StatTile
import com.zone.app.ui.components.ZoneCard
import com.zone.app.ui.navigation.Desk
import com.zone.app.ui.theme.zonePalette
import com.zone.app.util.ZoneFormat
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun OverviewScreen(onNavigate: (String) -> Unit) {
    val palette = zonePalette()
    var group by remember { mutableStateOf("All") }

    val settings = remember { ZoneStore.settings() }
    val money = remember { ZoneStore.readTyped(ZoneStore.KEY_MONEY, MoneyData()) }
    val diary = remember { ZoneStore.readTyped<List<DiaryEntry>>(ZoneStore.KEY_DIARY, emptyList()) }
    val lists = remember { ZoneStore.readTyped<List<ZoneList>>(ZoneStore.KEY_LISTS, emptyList()) }
    val fills = remember { ZoneStore.readTyped<List<Fill>>(ZoneStore.KEY_TRIPS, emptyList()) }

    val thisMonth = ZoneFormat.currentMonth()
    val monthExpense = money.transactions
        .filter { it.type == "expense" && it.date.startsWith(thisMonth) }
        .sumOf { it.amount }
    val budgetPct = if (money.budget > 0) ((monthExpense / money.budget) * 100).toInt() else 0
    val diaryStreak = remember(diary) { diaryStreak(diary) }
    val diaryWords = remember(diary) { diary.sumOf { ZoneFormat.words(it.body) } }
    val openItems = lists.sumOf { list ->
        list.items.count { !it.done } + list.items.sumOf { item -> item.children.count { !it.done } }
    }
    val validFills = fills.filter { it.end > it.start && it.price > 0 && it.paid > 0 }
    val totalDistance = validFills.sumOf { it.end - it.start }
    val totalLitres = validFills.sumOf { it.paid / it.price }
    val avgMileage = if (totalLitres > 0) totalDistance / totalLitres else 0.0

    LazyColumn(
        modifier = Modifier.fillMaxWidth().background(palette.background)
    ) {
        item(key = "header") {
            val greeting = when (LocalDateTime.now().hour) {
                in 5..11 -> "Good morning."
                in 12..16 -> "Good afternoon."
                else -> "Good evening."
            }
            LargeTitle(
                title = "Zone",
                subtitle = "$greeting Your everyday tools, one quiet desk."
            )
        }
        item(key = "now") {
            NowCard()
        }
        item(key = "stats") {
            Row(
                Modifier
                    .padding(horizontal = 20.dp)
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatTile(
                    value = ZoneFormat.money(monthExpense, settings.currency),
                    label = "Spent this month · $budgetPct% of budget",
                    modifier = Modifier.weight(1f),
                    tint = if (budgetPct > 90) palette.red else palette.label
                )
                StatTile(
                    value = "$diaryStreak",
                    label = "Diary streak · $diaryWords words",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatTile(
                    value = "$openItems",
                    label = "Open list items",
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    value = if (avgMileage > 0) ZoneFormat.num(avgMileage, 1) + " km/l" else "—",
                    label = "Average mileage",
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item(key = "desk-label") {
            Text(
                "THE FULL DESK",
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                color = palette.secondary,
                modifier = Modifier.padding(horizontal = 20.dp).padding(top = 22.dp, bottom = 2.dp)
            )
            Text(
                "Everything, at a glance.",
                style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                color = palette.label,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
        item(key = "desk-filter") {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.padding(horizontal = 20.dp).padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Desk.GROUPS.forEach { g ->
                    val selected = g == group
                    androidx.compose.material3.FilterChip(
                        selected = selected,
                        onClick = { group = g },
                        label = {
                            Text(
                                g,
                                style = androidx.compose.material3.MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp)
                            )
                        },
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }
        }
        val tools = Desk.TOOLS.filter { group == "All" || it.group == group }
        val rows = tools.chunked(2)
        items(rows.size, key = { "row-$it" }) { rowIndex ->
            val row = rows[rowIndex]
            Row(
                Modifier.padding(horizontal = 20.dp).padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { tool ->
                    DeskTile(
                        label = tool.label,
                        desc = tool.desc,
                        icon = tool.icon,
                        tint = tool.tint,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(tool.route) }
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        item(key = "privacy") {
            ZoneCard(modifier = Modifier.padding(horizontal = 20.dp).padding(top = 18.dp)) {
                Text(
                    "PRIVATE BY CONSTRUCTION",
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    color = palette.secondary
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "One workspace. Everything stays on this device.",
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                    color = palette.label
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Zone keeps its records in a private on-device store. No account, no analytics, no third parties. Export a backup from the admin back office whenever you like.",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = palette.secondary
                )
            }
        }
        item(key = "footer") { Spacer(Modifier.height(48.dp)) }
    }
}

@Composable
private fun NowCard() {
    val palette = zonePalette()
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(1000)
        }
    }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE, d MMMM") }
    ZoneCard(modifier = Modifier.padding(horizontal = 20.dp).padding(top = 6.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    now.format(timeFormatter),
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.label
                )
                Text(
                    now.format(dateFormatter),
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = palette.secondary
                )
            }
            Box(
                Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(palette.accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    ZoneFormat.todayISO().take(4),
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    color = palette.accent
                )
            }
        }
    }
}

@Composable
private fun DeskTile(
    label: String,
    desc: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val palette = zonePalette()
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(palette.surface)
            .border(0.5.dp, palette.separator, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        IconWellInline(icon = icon, tint = tint)
        Spacer(Modifier.height(10.dp))
        Text(label, style = androidx.compose.material3.MaterialTheme.typography.titleMedium, color = palette.label)
        Spacer(Modifier.height(2.dp))
        Text(
            desc,
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            color = palette.secondary,
            maxLines = 2
        )
    }
}

@Composable
private fun IconWellInline(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: androidx.compose.ui.graphics.Color) {
    Box(
        Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(tint),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(20.dp))
    }
}

/** Consecutive-day streak ending today (or yesterday) with at least one diary page. */
private fun diaryStreak(entries: List<DiaryEntry>): Int {
    val dates = entries.map { it.date }.toSet()
    var streak = 0
    var day = LocalDate.now()
    if (!dates.contains(ZoneFormat.toISO(day))) {
        day = day.minusDays(1)
    }
    while (dates.contains(ZoneFormat.toISO(day))) {
        streak += 1
        day = day.minusDays(1)
    }
    return streak
}
