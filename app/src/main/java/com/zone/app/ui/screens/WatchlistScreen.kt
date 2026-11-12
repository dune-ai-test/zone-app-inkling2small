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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zone.app.data.WatchItem
import com.zone.app.data.ZoneStore
import com.zone.app.ui.components.ConfirmDialog
import com.zone.app.ui.components.EmptyState
import com.zone.app.ui.components.FormDialog
import com.zone.app.ui.components.StatTile
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
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Tv
import kotlin.math.roundToInt

private val STATUSES = listOf("Queued", "Watching", "Watched", "Dropped")
private val TYPES = listOf("Film", "Series")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WatchlistScreen() {
    val palette = zonePalette()
    val context = LocalContext.current

    var items by remember {
        mutableStateOf(ZoneStore.readTyped<List<WatchItem>>(ZoneStore.KEY_WATCHLIST, emptyList()))
    }
    var statusFilter by remember { mutableStateOf("All") }
    var typeFilter by remember { mutableStateOf("All") }
    var query by remember { mutableStateOf("") }
    var favoritesOnly by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<WatchItem?>(null) }
    var showComposer by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<WatchItem?>(null) }
    var spotlight by remember { mutableStateOf<WatchItem?>(null) }

    fun persist(list: List<WatchItem>, action: String, title: String) {
        items = list
        ZoneStore.writeTyped(ZoneStore.KEY_WATCHLIST, list)
        ZoneStore.logAudit("watchlist", action, title)
    }

    val queued = items.count { it.status == "Queued" }
    val watching = items.count { it.status == "Watching" }
    val watched = items.count { it.status == "Watched" }
    val rated = items.filter { it.rating > 0 }
    val avgRating = if (rated.isNotEmpty()) rated.sumOf { it.rating } / rated.size else 0.0

    val visible = items.filter { item ->
        val statusOk = statusFilter == "All" || item.status == statusFilter
        val typeOk = typeFilter == "All" || item.type == typeFilter
        val favoriteOk = !favoritesOnly || item.favorite
        val q = query.trim().lowercase()
        val queryOk = q.isBlank() ||
            item.title.lowercase().contains(q) ||
            item.genre.lowercase().contains(q) ||
            item.service.lowercase().contains(q) ||
            item.note.lowercase().contains(q) ||
            item.year.contains(q)
        statusOk && typeOk && favoriteOk && queryOk
    }

    fun shuffleSpotlight() {
        val queuedItems = items.filter { it.status == "Queued" }
        if (queuedItems.isNotEmpty()) {
            spotlight = queuedItems.random()
        }
    }

    ZoneScreen(
        title = "Watchlist",
        subtitle = "Not the algorithm — your queue.",
        actions = {
            IconButton(onClick = { exportWatchlistCsv(context, items) }) {
                Icon(Icons.Rounded.Share, contentDescription = "Export CSV", tint = palette.accent)
            }
            IconButton(onClick = { shuffleSpotlight() }) {
                Icon(Icons.Rounded.Shuffle, contentDescription = "Tonight's pick", tint = palette.accent)
            }
            Spacer(Modifier.size(4.dp))
            IconButton(
                onClick = { showComposer = true; editing = null },
                modifier = Modifier.size(36.dp).clip(CircleShape).background(palette.accent)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add to watchlist", tint = Color.White)
            }
        }
    ) {
        item {
            Row(
                Modifier.padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatTile(value = "$queued", label = "In queue", modifier = Modifier.weight(1f))
                StatTile(value = "$watching", label = "Watching", modifier = Modifier.weight(1f))
                StatTile(value = "$watched", label = "Watched", modifier = Modifier.weight(1f))
                StatTile(value = if (avgRating > 0) "%.1f".format(avgRating) else "—", label = "Avg rating", modifier = Modifier.weight(1f))
            }
        }
        spotlight?.let { pick ->
            item(key = "spotlight") {
                ZoneCard(modifier = Modifier.padding(horizontal = 20.dp).padding(top = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (pick.type == "Film") Icons.Rounded.Movie else Icons.Rounded.Tv,
                            contentDescription = null,
                            tint = palette.accent,
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(Modifier.size(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("TONIGHT'S PICK", style = MaterialTheme.typography.labelSmall, color = palette.secondary)
                            Text(
                                pick.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.label,
                                maxLines = 2
                            )
                            Text(
                                listOfNotNull(pick.year.ifBlank { null }, pick.genre.ifBlank { null }, pick.service.ifBlank { null }).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.secondary
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            TextButton(onClick = { shuffleSpotlight() }) { Text("Again", color = palette.accent) }
                            TextButton(onClick = {
                                persist(items.map { if (it.id == pick.id) it.copy(status = "Watching") else it }, "update", pick.title)
                                spotlight = null
                            }) { Text("Start", color = palette.green, fontWeight = FontWeight.SemiBold) }
                        }
                    }
                }
            }
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search title, genre, service", color = palette.secondary) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = palette.secondary) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = zoneFieldColors(),
                modifier = Modifier.padding(horizontal = 20.dp).padding(top = 12.dp).fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                (listOf("All") + STATUSES).forEach { status ->
                    FilterChip(
                        selected = statusFilter == status,
                        onClick = { statusFilter = status },
                        label = { Text(status) },
                        shape = RoundedCornerShape(20.dp)
                    )
                }
                FilterChip(
                    selected = favoritesOnly,
                    onClick = { favoritesOnly = !favoritesOnly },
                    label = { Text("★ Favourites") },
                    shape = RoundedCornerShape(20.dp)
                )
            }
            Spacer(Modifier.height(4.dp))
            FlowRow(
                modifier = Modifier.padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                (listOf("All") + TYPES).forEach { type ->
                    FilterChip(
                        selected = typeFilter == type,
                        onClick = { typeFilter = type },
                        label = { Text(type) },
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }
        }
        if (visible.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Rounded.Movie,
                    title = if (items.isEmpty()) "A queue of none" else "Nothing matches",
                    subtitle = if (items.isEmpty())
                        "Add the first thing worth watching. Be selective."
                    else
                        "Try a different search or filter."
                )
            }
        } else {
            items(visible, key = { it.id }) { watch ->
                WatchCard(
                    item = watch,
                    onClick = { editing = watch; showComposer = true },
                    onAdvance = { delta ->
                        persist(items.map { if (it.id == watch.id) advanceWatch(it, delta) else it }, "update", watch.title)
                    },
                    onToggleFavorite = {
                        persist(items.map { if (it.id == watch.id) it.copy(favorite = !it.favorite) else it }, "update", watch.title)
                    }
                )
            }
        }
        item { Spacer(Modifier.height(28.dp)) }
    }

    if (showComposer) {
        WatchComposer(
            initial = editing,
            onDismiss = { showComposer = false; editing = null },
            onSave = { watch ->
                val list = items.toMutableList()
                val index = list.indexOfFirst { it.id == watch.id }
                if (index >= 0) list[index] = watch else list.add(watch)
                persist(list, if (index >= 0) "update" else "add", watch.title)
                showComposer = false
                editing = null
            },
            onDelete = editing?.let { target -> { showComposer = false; editing = null; deleting = target } }
        )
    }

    deleting?.let { target ->
        ConfirmDialog(
            title = "Remove from watchlist?",
            message = "“${target.title}” will be removed.",
            confirmLabel = "Remove",
            onConfirm = { persist(items.filterNot { it.id == target.id }, "delete", target.title) },
            onDismiss = { deleting = null }
        )
    }
}

/** Advance (or rewind) series progress; flips status at the end / start. */
private fun advanceWatch(item: WatchItem, delta: Int): WatchItem {
    if (item.type != "Series" || item.total <= 0) return item
    val next = (item.watched + delta).coerceIn(0, item.total)
    val status = when {
        next >= item.total -> "Watched"
        next > 0 && item.status == "Queued" -> "Watching"
        next > 0 && item.status == "Dropped" -> "Watching"
        else -> item.status
    }
    return item.copy(watched = next, status = status)
}

@Composable
private fun WatchCard(
    item: WatchItem,
    onClick: () -> Unit,
    onAdvance: (Int) -> Unit,
    onToggleFavorite: () -> Unit
) {
    val palette = zonePalette()
    val statusColor = when (item.status) {
        "Watching" -> palette.accent
        "Watched" -> palette.green
        "Dropped" -> palette.secondary
        else -> palette.orange
    }
    val progress = if (item.total > 0) item.watched.toFloat() / item.total else 0f

    ZoneCard(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(palette.grouped),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (item.type == "Film") Icons.Rounded.Movie else Icons.Rounded.Tv,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        item.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onToggleFavorite, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Rounded.Star,
                            contentDescription = "Favourite",
                            tint = if (item.favorite) palette.yellow else palette.separator,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Text(
                    listOfNotNull(item.year.ifBlank { null }, item.genre.ifBlank { null }, item.service.ifBlank { null }).joinToString(" · ").ifBlank { "—" },
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(statusColor.copy(alpha = 0.14f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(item.status, style = MaterialTheme.typography.labelSmall, color = statusColor)
                    }
                    if (item.rating > 0) {
                        Spacer(Modifier.size(8.dp))
                        Text(
                            "★ %.1f".format(item.rating),
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.secondary
                        )
                    }
                }
                if (item.type == "Series" && item.total > 0) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LinearProgressIndicator(
                            progress = { progress.coerceIn(0f, 1f) },
                            modifier = Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(3.dp)),
                            color = statusColor,
                            trackColor = palette.grouped
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            "${item.watched}/${item.total}",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.secondary
                        )
                        IconButton(onClick = { onAdvance(-1) }, modifier = Modifier.size(26.dp)) {
                            Text("−", color = palette.secondary, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = { onAdvance(+1) }, modifier = Modifier.size(26.dp)) {
                            Text("+", color = palette.accent, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WatchComposer(
    initial: WatchItem?,
    onDismiss: () -> Unit,
    onSave: (WatchItem) -> Unit,
    onDelete: (() -> Unit)?
) {
    val palette = zonePalette()
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var type by remember { mutableStateOf(initial?.type ?: "Film") }
    var year by remember { mutableStateOf(initial?.year ?: "") }
    var genre by remember { mutableStateOf(initial?.genre ?: "") }
    var service by remember { mutableStateOf(initial?.service ?: "") }
    var status by remember { mutableStateOf(initial?.status ?: "Queued") }
    var rating by remember { mutableStateOf(initial?.rating ?: 0.0) }
    var total by remember { mutableStateOf(initial?.total?.takeIf { it > 0 }?.toString() ?: "") }
    var watched by remember { mutableStateOf(initial?.watched?.takeIf { it > 0 }?.toString() ?: "") }
    var favorite by remember { mutableStateOf(initial?.favorite ?: false) }
    var note by remember { mutableStateOf(initial?.note ?: "") }

    FormDialog(
        title = if (initial == null) "Add to watchlist" else "Edit watch item",
        confirmLabel = if (initial == null) "Add" else "Save",
        confirmEnabled = title.trim().isNotEmpty(),
        onConfirm = {
            val totalNum = total.toIntOrNull() ?: 0
            val watchedNum = (watched.toIntOrNull() ?: 0).coerceIn(0, maxOf(totalNum, 0))
            onSave(
                WatchItem(
                    id = initial?.id ?: ZoneFormat.uid("watch"),
                    title = title.trim().take(120),
                    type = type,
                    year = year.trim().take(10),
                    genre = genre.trim().take(40),
                    status = status,
                    rating = rating,
                    watched = watchedNum,
                    total = totalNum,
                    service = service.trim().take(40),
                    favorite = favorite,
                    note = note.trim().take(300),
                    added = initial?.added ?: ZoneFormat.todayISO()
                )
            )
        },
        onDismiss = onDismiss
    ) {
        ZoneTextField(value = title, onValueChange = { title = it }, label = "Title")
        Column {
            Text("Type", style = MaterialTheme.typography.labelSmall, color = palette.secondary)
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TYPES.forEach { t ->
                    FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t) }, shape = RoundedCornerShape(20.dp))
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ZoneTextField(value = year, onValueChange = { year = it }, label = "Year", keyboardType = androidx.compose.ui.text.input.KeyboardType.Number, modifier = Modifier.weight(1f))
            ZoneTextField(value = genre, onValueChange = { genre = it }, label = "Genre", modifier = Modifier.weight(1f))
        }
        ZoneTextField(value = service, onValueChange = { service = it }, label = "Service (Netflix, Prime…)")
        Column {
            Text("Status", style = MaterialTheme.typography.labelSmall, color = palette.secondary)
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                STATUSES.forEach { s ->
                    FilterChip(selected = status == s, onClick = { status = s }, label = { Text(s) }, shape = RoundedCornerShape(20.dp))
                }
            }
        }
        if (type == "Series") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ZoneTextField(value = watched, onValueChange = { watched = it }, label = "Watched", keyboardType = androidx.compose.ui.text.input.KeyboardType.Number, modifier = Modifier.weight(1f))
                ZoneTextField(value = total, onValueChange = { total = it }, label = "Episodes", keyboardType = androidx.compose.ui.text.input.KeyboardType.Number, modifier = Modifier.weight(1f))
            }
        }
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Rating", style = MaterialTheme.typography.labelSmall, color = palette.secondary)
                Text(
                    if (rating <= 0.0) "Unrated" else "%.1f / 10".format(rating),
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.accent
                )
            }
            Slider(
                value = rating.toFloat(),
                onValueChange = { rating = (it * 2).roundToInt() / 2.0 },
                valueRange = 0f..10f,
                steps = 19
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.Checkbox(checked = favorite, onCheckedChange = { favorite = it })
            Text("Favourite", style = MaterialTheme.typography.bodyMedium, color = palette.label)
        }
        ZoneTextField(value = note, onValueChange = { note = it }, label = "Note (optional)", singleLine = false)
        if (onDelete != null) {
            TextButton(onClick = onDelete) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = palette.red)
                Spacer(Modifier.size(4.dp))
                Text("Remove from watchlist", color = palette.red)
            }
        }
    }
}

private fun exportWatchlistCsv(context: android.content.Context, items: List<WatchItem>) {
    val header = "Title,Type,Year,Genre,Service,Status,Rating,Watched,Total,Favourite,Added,Note"
    val rows = items.sortedBy { it.added }.map { item ->
        listOf(
            item.title, item.type, item.year, item.genre, item.service, item.status,
            item.rating.toString(), item.watched.toString(), item.total.toString(),
            if (item.favorite) "yes" else "no", item.added, item.note
        ).joinToString(",") { ZoneFormat.csvEscape(it) }
    }
    ZoneFormat.shareText(context, "Zone — watchlist", (listOf(header) + rows).joinToString("\n"), "text/csv")
}
