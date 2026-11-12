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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zone.app.data.Habit
import com.zone.app.data.ZoneStore
import com.zone.app.ui.components.ConfirmDialog
import com.zone.app.ui.components.EmptyState
import com.zone.app.ui.components.FormDialog
import com.zone.app.ui.components.LargeTitle
import com.zone.app.ui.components.SectionLabel
import com.zone.app.ui.components.ZoneCard
import com.zone.app.ui.components.ZoneScreen
import com.zone.app.ui.components.ZoneTextField
import com.zone.app.ui.theme.zonePalette
import com.zone.app.util.ZoneFormat
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Share
import androidx.compose.ui.graphics.Color
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HabitsScreen() {
    val palette = zonePalette()
    val context = LocalContext.current

    var habits by remember {
        mutableStateOf(ZoneStore.readTyped<List<Habit>>(ZoneStore.KEY_HABITS, emptyList()))
    }
    var weekOffset by remember { mutableStateOf(0) }
    var expanded by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<Habit?>(null) }
    var showNew by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Habit?>(null) }

    fun persist(list: List<Habit>, action: String, title: String) {
        habits = list
        ZoneStore.writeTyped(ZoneStore.KEY_HABITS, list)
        ZoneStore.logAudit("habits", action, title)
    }

    val today = LocalDate.now()
    val week = ZoneFormat.weekDates(weekOffset)
    val weekLabel = week.first().let { start ->
        val end = week.last()
        val f = DateTimeFormatter.ofPattern("d MMM")
        "${start.format(f)} – ${end.format(f)}"
    }

    val todayISO = ZoneFormat.toISO(today)
    val doneToday = habits.count { it.checks[todayISO] == true }
    val weekPct = weekCompletionPct(habits, week, today)
    val bestStreak = habits.maxOfOrNull { longestStreak(it) } ?: 0
    val consistency = habits.map { eightWeekConsistency(it) }.let { list ->
        if (list.isEmpty()) 0 else list.sum() / list.size
    }

    ZoneScreen(
        title = "Habits",
        subtitle = "Small repetitions, made visible.",
        actions = {
            IconButton(onClick = { exportHabitsCsv(context, habits) }) {
                Icon(Icons.Rounded.Share, contentDescription = "Export history", tint = palette.accent)
            }
            Spacer(Modifier.size(4.dp))
            IconButton(
                onClick = { showNew = true },
                modifier = Modifier.size(36.dp).clip(CircleShape).background(palette.accent)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "New habit", tint = Color.White)
            }
        }
    ) {
        item {
            Row(
                Modifier.padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HabitMetric(value = "$doneToday/${habits.size}", label = "Today", modifier = Modifier.weight(1f))
                HabitMetric(value = "$weekPct%", label = "This week", modifier = Modifier.weight(1f))
                HabitMetric(value = "$bestStreak", label = "Best streak", modifier = Modifier.weight(1f))
                HabitMetric(value = "$consistency%", label = "8-week pulse", modifier = Modifier.weight(1f))
            }
        }
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { weekOffset -= 1 }) {
                    Icon(Icons.Rounded.ChevronLeft, contentDescription = "Previous week", tint = palette.label)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (weekOffset == 0) "This week" else weekLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.label
                    )
                    Text(
                        if (weekOffset == 0) weekLabel else "Past week",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondary
                    )
                }
                IconButton(
                    onClick = { if (weekOffset < 0) weekOffset += 1 },
                    enabled = weekOffset < 0
                ) {
                    Icon(
                        Icons.Rounded.ChevronRight,
                        contentDescription = "Next week",
                        tint = if (weekOffset < 0) palette.label else palette.separator
                    )
                }
            }
        }
        if (habits.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Rounded.Repeat,
                    title = "No habits yet",
                    subtitle = "Pick one small thing worth repeating. It compounds."
                )
            }
        } else {
            items(habits, key = { it.id }) { habit ->
                HabitCard(
                    habit = habit,
                    week = week,
                    today = today,
                    expanded = expanded == habit.id,
                    onToggleDay = { date ->
                        if (!date.isAfter(today)) {
                            toggleCheck(habit, ZoneFormat.toISO(date)) { updated ->
                                persist(habits.map { if (it.id == habit.id) updated else it }, "update", updated.name)
                            }
                        }
                    },
                    onToggleExpand = { expanded = if (expanded == habit.id) null else habit.id },
                    onEdit = { editing = habit },
                    onDelete = { deleting = habit }
                )
            }
        }
        item { Spacer(Modifier.height(28.dp)) }
    }

    if (showNew || editing != null) {
        HabitDialog(
            initial = editing,
            onDismiss = { showNew = false; editing = null },
            onSave = { name, target ->
                val isNew = editing == null
                val habit = editing?.copy(name = name, target = target)
                    ?: Habit(id = ZoneFormat.uid("habit"), name = name, target = target, createdAt = ZoneFormat.todayISO())
                val list = if (isNew) habits + habit else habits.map { if (it.id == habit.id) habit else it }
                persist(list, if (isNew) "add" else "update", name)
                showNew = false
                editing = null
            }
        )
    }

    deleting?.let { target ->
        ConfirmDialog(
            title = "Delete habit?",
            message = "“${target.name}” and its check-in history will be removed.",
            confirmLabel = "Delete",
            onConfirm = { persist(habits.filterNot { it.id == target.id }, "delete", target.name) },
            onDismiss = { deleting = null }
        )
    }
}

@Composable
private fun HabitMetric(value: String, label: String, modifier: Modifier = Modifier) {
    val palette = zonePalette()
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(palette.surface)
            .border(0.5.dp, palette.separator, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = palette.label, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp), color = palette.secondary, maxLines = 1)
    }
}

@Composable
private fun HabitCard(
    habit: Habit,
    week: List<LocalDate>,
    today: LocalDate,
    expanded: Boolean,
    onToggleDay: (LocalDate) -> Unit,
    onToggleExpand: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val palette = zonePalette()
    val dayLetters = listOf("M", "T", "W", "T", "F", "S", "S")
    val streak = currentStreak(habit)
    val doneThisWeek = week.count { habit.checks[ZoneFormat.toISO(it)] == true }

    ZoneCard(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    habit.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "🔥 $streak day streak · ${habit.target}×/week",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.secondary
                )
            }
            TextButton(onClick = onToggleExpand) {
                Text(if (expanded) "Less" else "Details", color = palette.accent)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            week.forEachIndexed { index, date ->
                val iso = ZoneFormat.toISO(date)
                val checked = habit.checks[iso] == true
                val isFuture = date.isAfter(today)
                val isToday = iso == ZoneFormat.toISO(today)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        dayLetters[index],
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isToday) palette.accent else palette.secondary
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    checked -> palette.green
                                    isFuture -> palette.grouped.copy(alpha = 0.5f)
                                    else -> palette.grouped
                                }
                            )
                            .clickable(enabled = !isFuture) { onToggleDay(date) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (checked) {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        } else if (isToday) {
                            Box(Modifier.size(7.dp).clip(CircleShape).background(palette.accent))
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "$doneThisWeek of ${habit.target} check-ins this week",
            style = MaterialTheme.typography.bodySmall,
            color = palette.secondary
        )

        if (expanded) {
            Spacer(Modifier.height(12.dp))
            androidx.compose.material3.HorizontalDivider(color = palette.separator, thickness = 0.5.dp)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                HabitFact("Current", "${currentStreak(habit)}d")
                HabitFact("Longest", "${longestStreak(habit)}d")
                HabitFact("This month", "${monthCount(habit)}×")
                HabitFact("8-week", "${eightWeekConsistency(habit)}%")
            }
            Spacer(Modifier.height(10.dp))
            HeatGrid(habit)
            Spacer(Modifier.height(8.dp))
            Row {
                TextButton(onClick = onEdit) { Text("Edit habit", color = palette.accent) }
                Spacer(Modifier.size(4.dp))
                TextButton(onClick = onDelete) { Text("Delete", color = palette.red) }
            }
        }
    }
}

@Composable
private fun HabitFact(label: String, value: String) {
    val palette = zonePalette()
    Column {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = palette.label)
        Text(label, style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp), color = palette.secondary)
    }
}

@Composable
private fun HeatGrid(habit: Habit) {
    val palette = zonePalette()
    val days = (55 downTo 0).map { LocalDate.now().minusDays(it.toLong()) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        days.chunked(14).forEach { rowDays ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                rowDays.forEach { date ->
                    val checked = habit.checks[ZoneFormat.toISO(date)] == true
                    Box(
                        Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (checked) palette.green else palette.grouped)
                    )
                }
            }
        }
        Text(
            "Last 56 days",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
            color = palette.secondary
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HabitDialog(initial: Habit?, onDismiss: () -> Unit, onSave: (String, Int) -> Unit) {
    val palette = zonePalette()
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var target by remember { mutableStateOf(initial?.target ?: 7) }

    FormDialog(
        title = if (initial == null) "New habit" else "Edit habit",
        confirmLabel = if (initial == null) "Create" else "Save",
        confirmEnabled = name.trim().isNotEmpty(),
        onConfirm = { onSave(name.trim().take(60), target) },
        onDismiss = onDismiss
    ) {
        ZoneTextField(value = name, onValueChange = { name = it }, label = "Habit")
        Column {
            Text("Target days per week", style = MaterialTheme.typography.labelSmall, color = palette.secondary)
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (1..7).forEach { n ->
                    FilterChip(
                        selected = target == n,
                        onClick = { target = n },
                        label = { Text("$n") },
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }
        }
    }
}

private fun toggleCheck(habit: Habit, iso: String, apply: (Habit) -> Unit) {
    val next = habit.checks.toMutableMap()
    if (next[iso] == true) next.remove(iso) else next[iso] = true
    apply(habit.copy(checks = next))
}

private fun currentStreak(habit: Habit): Int {
    val dates = habit.checks.filterValues { it }.keys
    var count = 0
    var day = LocalDate.now()
    if (!dates.contains(ZoneFormat.toISO(day))) day = day.minusDays(1)
    while (dates.contains(ZoneFormat.toISO(day))) {
        count += 1
        day = day.minusDays(1)
    }
    return count
}

private fun longestStreak(habit: Habit): Int {
    val dates = habit.checks.filterValues { it }.keys.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.sorted()
    var best = 0
    var run = 0
    var prev: LocalDate? = null
    for (d in dates) {
        run = if (prev != null && d == prev.plusDays(1)) run + 1 else 1
        if (run > best) best = run
        prev = d
    }
    return best
}

private fun monthCount(habit: Habit): Int {
    val prefix = ZoneFormat.currentMonth()
    return habit.checks.count { it.value && it.key.startsWith(prefix) }
}

private fun eightWeekConsistency(habit: Habit): Int {
    val days = (0 until 56).map { ZoneFormat.toISO(LocalDate.now().minusDays(it.toLong())) }
    val checked = days.count { habit.checks[it] == true }
    val possible = habit.target * 8
    return if (possible <= 0) 0 else ((checked.toFloat() / possible) * 100).toInt().coerceIn(0, 100)
}

private fun weekCompletionPct(habits: List<Habit>, week: List<LocalDate>, today: LocalDate): Int {
    if (habits.isEmpty()) return 0
    val elapsed = week.count { !it.isAfter(today) }.coerceAtLeast(1)
    var earned = 0.0
    var possible = 0.0
    habits.forEach { habit ->
        val cap = minOf(habit.target, elapsed)
        val checked = week.count { !it.isAfter(today) && habit.checks[ZoneFormat.toISO(it)] == true }
        earned += minOf(checked, cap)
        possible += cap
    }
    return if (possible <= 0) 0 else ((earned / possible) * 100).toInt()
}

private fun exportHabitsCsv(context: android.content.Context, habits: List<Habit>) {
    val header = "Habit,Target per week,Created,Checked dates"
    val rows = habits.map { habit ->
        listOf(
            habit.name,
            habit.target.toString(),
            habit.createdAt,
            habit.checks.filterValues { it }.keys.sorted().joinToString("; ")
        ).joinToString(",") { ZoneFormat.csvEscape(it) }
    }
    ZoneFormat.shareText(context, "Zone — habits history", (listOf(header) + rows).joinToString("\n"), "text/csv")
}
