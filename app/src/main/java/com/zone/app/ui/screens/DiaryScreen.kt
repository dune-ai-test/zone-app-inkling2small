package com.zone.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zone.app.data.DiaryEntry
import com.zone.app.data.ZoneStore
import com.zone.app.ui.components.ConfirmDialog
import com.zone.app.ui.components.EmptyState
import com.zone.app.ui.components.FormDialog
import com.zone.app.ui.components.LargeTitle
import com.zone.app.ui.components.SectionLabel
import com.zone.app.ui.components.ZoneCard
import com.zone.app.ui.components.ZoneScreen
import com.zone.app.ui.components.ZoneTextField
import com.zone.app.ui.components.zoneFieldColors
import com.zone.app.ui.theme.zonePalette
import com.zone.app.util.ZoneFormat
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val MOODS = listOf(
    Triple("low", "Low", Color(0xFFFF9500)),
    Triple("flat", "Flat", Color(0xFF8E8E93)),
    Triple("good", "Good", Color(0xFF34C759)),
    Triple("bright", "Bright", Color(0xFFFFCC00)),
    Triple("peak", "Peak", Color(0xFFAF52DE))
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DiaryScreen() {
    val palette = zonePalette()
    val context = LocalContext.current

    var entries by remember {
        mutableStateOf(
            ZoneStore.readTyped<List<DiaryEntry>>(ZoneStore.KEY_DIARY, emptyList())
                .sortedByDescending { it.date }
        )
    }
    var query by remember { mutableStateOf("") }
    var moodFilter by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<DiaryEntry?>(null) }
    var composing by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<DiaryEntry?>(null) }

    fun persist(list: List<DiaryEntry>, action: String, title: String) {
        entries = list.sortedByDescending { it.date }
        ZoneStore.writeTyped(ZoneStore.KEY_DIARY, entries)
        ZoneStore.logAudit("diary", action, title)
    }

    val visible = entries.filter { entry ->
        val matchesMood = moodFilter == null || entry.mood == moodFilter
        val q = query.trim().lowercase()
        val matchesQuery = q.isBlank() ||
            entry.title.lowercase().contains(q) ||
            entry.body.lowercase().contains(q) ||
            entry.tags.any { it.lowercase().contains(q) } ||
            entry.date.contains(q)
        matchesMood && matchesQuery
    }
    val words = entries.sumOf { ZoneFormat.words(it.body) }
    val streak = remember(entries) {
        val dates = entries.map { it.date }.toSet()
        var count = 0
        var day = LocalDate.now()
        if (!dates.contains(ZoneFormat.toISO(day))) day = day.minusDays(1)
        while (dates.contains(ZoneFormat.toISO(day))) {
            count += 1
            day = day.minusDays(1)
        }
        count
    }
    val dominantMood = entries.groupingBy { it.mood }.eachCount().maxByOrNull { it.value }?.key ?: "—"

    ZoneScreen(
        title = "Diary",
        subtitle = "The days, kept close.",
        actions = {
            IconButton(onClick = { exportDiaryCsv(context, entries) }) {
                Icon(Icons.Rounded.Share, contentDescription = "Export journal", tint = palette.accent)
            }
            Spacer(Modifier.size(4.dp))
            IconButton(
                onClick = { composing = true },
                modifier = Modifier.size(36.dp).clip(CircleShape).background(palette.accent)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Write an entry", tint = Color.White)
            }
        }
    ) {
        item {
            Row(
                Modifier.padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DiaryStat(value = "${entries.size}", label = "Pages filled", modifier = Modifier.weight(1f))
                DiaryStat(value = "$streak", label = "Day streak", modifier = Modifier.weight(1f))
                DiaryStat(value = "$words", label = "Words kept", modifier = Modifier.weight(1f))
                DiaryStat(value = dominantMood.replaceFirstChar { it.uppercase() }, label = "Weather", modifier = Modifier.weight(1f))
            }
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search entries", color = palette.secondary) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = palette.secondary) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = zoneFieldColors(),
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(top = 14.dp)
                    .fillMaxWidth()
            )
        }
        item {
            FlowRow(
                modifier = Modifier.padding(horizontal = 20.dp).padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val allSelected = moodFilter == null
                FilterChip(
                    selected = allSelected,
                    onClick = { moodFilter = null },
                    label = { Text("All") },
                    shape = RoundedCornerShape(20.dp)
                )
                MOODS.forEach { (id, label, color) ->
                    FilterChip(
                        selected = moodFilter == id,
                        onClick = { moodFilter = if (moodFilter == id) null else id },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(8.dp).clip(CircleShape).background(color))
                                Spacer(Modifier.size(6.dp))
                                Text(label)
                            }
                        },
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }
        }
        if (visible.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Rounded.Book,
                    title = if (entries.isEmpty()) "No pages yet" else "Nothing matches",
                    subtitle = if (entries.isEmpty())
                        "Write the first entry — one honest paragraph is enough."
                    else
                        "Try a different search or clear the mood filter."
                )
            }
        } else {
            val grouped = visible.groupBy { it.date.take(7) }
            grouped.toSortedMap(compareByDescending { it }).forEach { (month, monthEntries) ->
                item(key = "label-$month") { SectionLabel(ZoneFormat.monthLabel(month)) }
                items(monthEntries, key = { it.id }) { entry ->
                    DiaryRow(entry = entry, onClick = {
                        editing = entry
                        composing = true
                    })
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    if (composing) {
        DiaryComposer(
            initial = editing,
            onDismiss = { composing = false; editing = null },
            onSave = { entry ->
                val list = entries.toMutableList()
                val index = list.indexOfFirst { it.id == entry.id }
                if (index >= 0) list[index] = entry else list.add(entry)
                persist(list, if (index >= 0) "update" else "add", entry.title.ifBlank { "Untitled entry" })
                composing = false
                editing = null
            },
            onDelete = editing?.let { target -> { composing = false; editing = null; deleting = target } }
        )
    }

    deleting?.let { target ->
        ConfirmDialog(
            title = "Delete this page?",
            message = "“${target.title.ifBlank { "Untitled entry" }}” will be removed from the diary.",
            confirmLabel = "Delete",
            onConfirm = {
                persist(entries.filterNot { it.id == target.id }, "delete", target.title.ifBlank { "Untitled entry" })
            },
            onDismiss = { deleting = null }
        )
    }
}

@Composable
private fun DiaryStat(value: String, label: String, modifier: Modifier = Modifier) {
    val palette = zonePalette()
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(palette.surface)
            .border(0.5.dp, palette.separator, RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = palette.label, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = palette.secondary, maxLines = 1)
    }
}

@Composable
private fun DiaryRow(entry: DiaryEntry, onClick: () -> Unit) {
    val palette = zonePalette()
    val moodColor = MOODS.firstOrNull { it.first == entry.mood }?.third ?: palette.secondary
    val date = remember(entry.date) { ZoneFormat.parseISO(entry.date) }
    ZoneCard(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.grouped),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(date.format(DateTimeFormatter.ofPattern("d")), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = palette.label)
                Text(date.format(DateTimeFormatter.ofPattern("MMM")).uppercase(), fontSize = 10.sp, color = palette.secondary, letterSpacing = 1.sp)
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        entry.title.ifBlank { "Untitled entry" },
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.size(8.dp))
                    Box(Modifier.size(8.dp).clip(CircleShape).background(moodColor))
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    entry.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.secondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        ZoneFormat.relDate(entry.date),
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.accent
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        "${ZoneFormat.words(entry.body)} words",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.secondary
                    )
                    if (entry.tags.isNotEmpty()) {
                        Spacer(Modifier.size(8.dp))
                        Text(
                            entry.tags.joinToString("  ") { "#$it" },
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun DiaryComposer(
    initial: DiaryEntry?,
    onDismiss: () -> Unit,
    onSave: (DiaryEntry) -> Unit,
    onDelete: (() -> Unit)?
) {
    val palette = zonePalette()
    var date by remember { mutableStateOf(initial?.date ?: ZoneFormat.todayISO()) }
    var mood by remember { mutableStateOf(initial?.mood ?: "good") }
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var body by remember { mutableStateOf(initial?.body ?: "") }
    var tags by remember { mutableStateOf(initial?.tags?.joinToString(", ") ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }

    FormDialog(
        title = if (initial == null) "Write today" else "Edit page",
        confirmLabel = if (initial == null) "Add page" else "Save",
        confirmEnabled = date.isNotBlank() && (title.isNotBlank() || body.isNotBlank()),
        onConfirm = {
            val parsedTags = tags.split(",")
                .map { it.trim().removePrefix("#") }
                .filter { it.isNotEmpty() }
                .distinctBy { it.lowercase() }
                .take(8)
            onSave(
                DiaryEntry(
                    id = initial?.id ?: ZoneFormat.uid("diary"),
                    date = date,
                    mood = mood,
                    title = title.trim().take(90),
                    body = body.trim(),
                    tags = parsedTags
                )
            )
        },
        onDismiss = onDismiss
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Date", style = MaterialTheme.typography.labelSmall, color = palette.secondary)
                Text(ZoneFormat.dateLong(date), style = MaterialTheme.typography.bodyMedium, color = palette.label)
            }
            TextButton(onClick = { showDatePicker = true }) { Text("Change", color = palette.accent) }
        }

        Column {
            Text("Mood", style = MaterialTheme.typography.labelSmall, color = palette.secondary)
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MOODS.forEach { (id, label, color) ->
                    val selected = mood == id
                    FilterChip(
                        selected = selected,
                        onClick = { mood = id },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(7.dp).clip(CircleShape).background(color))
                                Spacer(Modifier.size(5.dp))
                                Text(label)
                            }
                        },
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }
        }

        ZoneTextField(value = title, onValueChange = { title = it }, label = "Title")
        ZoneTextField(value = body, onValueChange = { body = it }, label = "What happened?", singleLine = false)
        ZoneTextField(value = tags, onValueChange = { tags = it }, label = "Threads (comma separated)")
        Text(
            "${ZoneFormat.words(body)} words · about ${ZoneFormat.readingMinutes(body)} min read",
            style = MaterialTheme.typography.labelSmall,
            color = palette.secondary
        )
        if (onDelete != null) {
            TextButton(onClick = onDelete) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = palette.red)
                Spacer(Modifier.size(4.dp))
                Text("Delete page", color = palette.red)
            }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = ZoneFormat.parseISO(date)
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().let(ZoneFormat::toISO)
                    }
                    showDatePicker = false
                }) { Text("Set", color = palette.accent) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel", color = palette.secondary) } }
        ) {
            DatePicker(state = state)
        }
    }
}

private fun exportDiaryCsv(context: android.content.Context, entries: List<DiaryEntry>) {
    val header = "Date,Mood,Title,Threads,Words,Entry"
    val rows = entries.sortedBy { it.date }.map { entry ->
        listOf(
            entry.date,
            entry.mood,
            entry.title.ifBlank { "Untitled entry" },
            entry.tags.joinToString("; "),
            ZoneFormat.words(entry.body).toString(),
            entry.body.replace("\n", " ")
        ).joinToString(",") { ZoneFormat.csvEscape(it) }
    }
    ZoneFormat.shareText(context, "Zone — diary export", (listOf(header) + rows).joinToString("\n"), "text/csv")
}
