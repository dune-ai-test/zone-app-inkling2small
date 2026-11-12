package com.zone.app.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.zone.app.data.LinkItem
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
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Star

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LinksScreen() {
    val palette = zonePalette()
    val context = LocalContext.current

    var links by remember {
        mutableStateOf(ZoneStore.readTyped<List<LinkItem>>(ZoneStore.KEY_LINKS, emptyList()))
    }
    var query by remember { mutableStateOf("") }
    var favoritesOnly by remember { mutableStateOf(false) }
    var tagFilter by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<LinkItem?>(null) }
    var showComposer by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<LinkItem?>(null) }
    var spotlightId by remember { mutableStateOf<String?>(null) }

    fun persist(list: List<LinkItem>, action: String, title: String) {
        links = list.sortedByDescending { it.saved }
        ZoneStore.writeTyped(ZoneStore.KEY_LINKS, links)
        ZoneStore.logAudit("links", action, title)
    }

    val allTags = links.flatMap { it.tags }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }
    val domains = links.mapNotNull { domainOf(it.url) }.distinctBy { it.lowercase() }
    val favorites = links.count { it.favorite }

    val visible = links.filter { link ->
        val q = query.trim().lowercase()
        val queryOk = q.isBlank() ||
            link.title.lowercase().contains(q) ||
            link.url.lowercase().contains(q) ||
            link.note.lowercase().contains(q) ||
            link.tags.any { it.lowercase().contains(q) } ||
            (domainOf(link.url)?.lowercase()?.contains(q) == true)
        val favoriteOk = !favoritesOnly || link.favorite
        val tagOk = tagFilter == null || link.tags.any { it.equals(tagFilter, ignoreCase = true) }
        queryOk && favoriteOk && tagOk
    }

    // Spotlight: explicit pick, else newest favourite, else newest.
    val spotlight: LinkItem? = spotlightId?.let { id -> links.firstOrNull { it.id == id } }
        ?: links.filter { it.favorite }.maxByOrNull { it.saved }
        ?: links.maxByOrNull { it.saved }

    ZoneScreen(
        title = "Links",
        subtitle = "The good corners of the web.",
        actions = {
            IconButton(onClick = { exportLinksCsv(context, links) }) {
                Icon(Icons.Rounded.Share, contentDescription = "Export CSV", tint = palette.accent)
            }
            IconButton(onClick = {
                if (links.isNotEmpty()) {
                    val others = links.filter { it.id != spotlight?.id }.ifEmpty { links }
                    spotlightId = others.random().id
                }
            }) {
                Icon(Icons.Rounded.Shuffle, contentDescription = "Surprise me", tint = palette.accent)
            }
            Spacer(Modifier.size(4.dp))
            IconButton(
                onClick = { showComposer = true; editing = null },
                modifier = Modifier.size(36.dp).clip(CircleShape).background(palette.accent)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Save a link", tint = Color.White)
            }
        }
    ) {
        item {
            Row(
                Modifier.padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatTile(value = "${links.size}", label = "In library", modifier = Modifier.weight(1f))
                StatTile(value = "${allTags.size}", label = "Subjects", modifier = Modifier.weight(1f))
                StatTile(value = "${domains.size}", label = "Domains", modifier = Modifier.weight(1f))
                StatTile(value = "$favorites", label = "Favourites", modifier = Modifier.weight(1f))
            }
        }
        spotlight?.let { pick ->
            item(key = "spotlight") {
                SpotlightCard(
                    link = pick,
                    index = visible.indexOfFirst { it.id == pick.id }.let { if (it >= 0) it + 1 else null },
                    count = visible.size,
                    onOpen = { openUrl(context, pick.url) },
                    onCopy = { ZoneFormat.copyToClipboard(context, "Zone link", pick.url) },
                    onEdit = { editing = pick; showComposer = true },
                    onToggleFavorite = { persist(links.map { if (it.id == pick.id) it.copy(favorite = !it.favorite) else it }, "update", pick.title) }
                )
            }
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search links, tags, domains", color = palette.secondary) },
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
                FilterChip(
                    selected = favoritesOnly,
                    onClick = { favoritesOnly = !favoritesOnly },
                    label = { Text("★ Favourites") },
                    shape = RoundedCornerShape(20.dp)
                )
                FilterChip(
                    selected = tagFilter == null,
                    onClick = { tagFilter = null },
                    label = { Text("All subjects") },
                    shape = RoundedCornerShape(20.dp)
                )
                allTags.take(8).forEach { (tag, count) ->
                    FilterChip(
                        selected = tagFilter.equals(tag, ignoreCase = true),
                        onClick = { tagFilter = if (tagFilter.equals(tag, ignoreCase = true)) null else tag },
                        label = { Text("$tag · $count") },
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }
        }
        if (visible.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Rounded.Link,
                    title = if (links.isEmpty()) "Nothing shelved yet" else "Nothing matches",
                    subtitle = if (links.isEmpty())
                        "Save the first link worth keeping."
                    else
                        "Try a different search, tag or favourite filter."
                )
            }
        } else {
            items(visible, key = { it.id }) { link ->
                LinkCard(
                    link = link,
                    onOpen = { openUrl(context, link.url) },
                    onCopy = { ZoneFormat.copyToClipboard(context, "Zone link", link.url) },
                    onClick = { editing = link; showComposer = true },
                    onToggleFavorite = { persist(links.map { if (it.id == link.id) it.copy(favorite = !it.favorite) else it }, "update", link.title) },
                    onDelete = { deleting = link }
                )
            }
        }
        item { Spacer(Modifier.height(28.dp)) }
    }

    if (showComposer) {
        LinkComposer(
            initial = editing,
            knownTags = allTags.map { it.key },
            onDismiss = { showComposer = false; editing = null },
            onSave = { link ->
                val list = links.toMutableList()
                val index = list.indexOfFirst { it.id == link.id }
                if (index >= 0) list[index] = link else list.add(link)
                persist(list, if (index >= 0) "update" else "add", link.title)
                showComposer = false
                editing = null
            },
            onDelete = editing?.let { target -> { showComposer = false; editing = null; deleting = target } }
        )
    }

    deleting?.let { target ->
        ConfirmDialog(
            title = "Remove link?",
            message = "“${target.title}” will be removed from the shelf.",
            confirmLabel = "Remove",
            onConfirm = {
                persist(links.filterNot { it.id == target.id }, "delete", target.title)
                if (spotlightId == target.id) spotlightId = null
            },
            onDismiss = { deleting = null }
        )
    }
}

@Composable
private fun MonogramCircle(domain: String?, size: androidx.compose.ui.unit.Dp) {
    val palette = zonePalette()
    val tone = listOf(
        Color(0xFFFF9500), Color(0xFF34C759), Color(0xFF007AFF),
        Color(0xFFAF52DE), Color(0xFFFF3B30), Color(0xFF5AC8FA)
    )
    val color = domain?.let { tone[ZoneFormat.toneFor(it) % tone.size] } ?: palette.secondary
    val letters = domain?.removePrefix("www.")?.split(".")?.firstOrNull()?.take(2)?.uppercase() ?: "•"
    Box(
        Modifier.size(size).clip(CircleShape).background(color.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        Text(letters, fontWeight = FontWeight.Bold, color = color, fontSize = (size.value * 0.36f).sp)
    }
}

@Composable
private fun SpotlightCard(
    link: LinkItem,
    index: Int?,
    count: Int,
    onOpen: () -> Unit,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val palette = zonePalette()
    ZoneCard(modifier = Modifier.padding(horizontal = 20.dp).padding(top = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MonogramCircle(domainOf(link.url), 52.dp)
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "ON THE DESK" + (index?.let { if (count > 0) " · No. $it/$count" else "" } ?: ""),
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.secondary
                )
                Text(
                    link.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.label,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    domainOf(link.url) ?: link.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onToggleFavorite, modifier = Modifier.size(34.dp)) {
                Icon(
                    Icons.Rounded.Star,
                    contentDescription = "Favourite",
                    tint = if (link.favorite) palette.yellow else palette.separator
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            androidx.compose.material3.FilledTonalButton(
                onClick = onOpen,
                shape = RoundedCornerShape(12.dp),
                colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                    containerColor = palette.accent.copy(alpha = 0.12f),
                    contentColor = palette.accent
                )
            ) {
                Icon(Icons.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.size(6.dp))
                Text("Open")
            }
            androidx.compose.material3.OutlinedButton(onClick = onCopy, shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.size(6.dp))
                Text("Copy")
            }
            androidx.compose.material3.TextButton(onClick = onEdit) {
                Text("Edit", color = palette.accent)
            }
        }
    }
}

@Composable
private fun LinkCard(
    link: LinkItem,
    onOpen: () -> Unit,
    onCopy: () -> Unit,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    val palette = zonePalette()
    ZoneCard(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MonogramCircle(domainOf(link.url), 44.dp)
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    link.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    link.url.removePrefix("https://").removePrefix("http://"),
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (link.tags.isNotEmpty()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        link.tags.joinToString("  ") { "#${it.lowercase()}" },
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.accent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (link.note.isNotBlank()) {
                    Text(
                        link.note,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = palette.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(onClick = onToggleFavorite, modifier = Modifier.size(30.dp)) {
                Icon(
                    Icons.Rounded.Star,
                    contentDescription = "Favourite",
                    tint = if (link.favorite) palette.yellow else palette.separator,
                    modifier = Modifier.size(17.dp)
                )
            }
            IconButton(onClick = onOpen, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Rounded.OpenInNew, contentDescription = "Open", tint = palette.secondary, modifier = Modifier.size(17.dp))
            }
            IconButton(onClick = onCopy, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy URL", tint = palette.secondary, modifier = Modifier.size(17.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove", tint = palette.red, modifier = Modifier.size(17.dp))
            }
        }
    }
}

@Composable
private fun LinkComposer(
    initial: LinkItem?,
    knownTags: List<String>,
    onDismiss: () -> Unit,
    onSave: (LinkItem) -> Unit,
    onDelete: (() -> Unit)?
) {
    val palette = zonePalette()
    var url by remember { mutableStateOf(initial?.url ?: "") }
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var tags by remember { mutableStateOf(initial?.tags?.joinToString(", ") ?: "") }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var favorite by remember { mutableStateOf(initial?.favorite ?: false) }

    val normalized = normalizeUrl(url)
    val urlValid = normalized != null

    FormDialog(
        title = if (initial == null) "Save a link" else "Edit link",
        confirmLabel = "Keep",
        confirmEnabled = urlValid && title.trim().isNotEmpty(),
        onConfirm = {
            val parsedTags = tags.split(",")
                .map { it.trim().removePrefix("#") }
                .filter { it.isNotEmpty() }
                .distinctBy { it.lowercase() }
                .take(6)
            onSave(
                LinkItem(
                    id = initial?.id ?: ZoneFormat.uid("link"),
                    title = title.trim().take(120),
                    url = normalized!!,
                    tags = parsedTags,
                    note = note.trim().take(300),
                    favorite = favorite,
                    saved = initial?.saved ?: ZoneFormat.todayISO()
                )
            )
        },
        onDismiss = onDismiss
    ) {
        ZoneTextField(value = url, onValueChange = { url = it }, label = "URL", keyboardType = androidx.compose.ui.text.input.KeyboardType.Uri)
        if (url.isNotBlank() && !urlValid) {
            Text(
                "That doesn't look like a valid http(s) address.",
                style = MaterialTheme.typography.bodySmall,
                color = palette.red
            )
        }
        ZoneTextField(value = title, onValueChange = { title = it }, label = "Title")
        ZoneTextField(value = tags, onValueChange = { tags = it }, label = "Subjects (comma separated)")
        if (knownTags.isNotEmpty()) {
            Text(
                "Used before: ${knownTags.take(6).joinToString(", ")}",
                style = MaterialTheme.typography.bodySmall,
                color = palette.secondary
            )
        }
        ZoneTextField(value = note, onValueChange = { note = it }, label = "Why keep it? (optional)", singleLine = false)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = favorite, onCheckedChange = { favorite = it })
            Text("Favourite", style = MaterialTheme.typography.bodyMedium, color = palette.label)
        }
        if (onDelete != null) {
            TextButton(onClick = onDelete) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = palette.red)
                Spacer(Modifier.size(4.dp))
                Text("Remove from shelf", color = palette.red)
            }
        }
    }
}

private fun normalizeUrl(raw: String): String? {
    var value = raw.trim()
    if (value.isEmpty()) return null
    if (!value.contains("://")) value = "https://$value"
    return try {
        val uri = java.net.URI(value)
        val scheme = uri.scheme?.lowercase()
        val host = uri.host
        if (scheme != "http" && scheme != "https") return null
        if (host.isNullOrBlank() || !host.contains(".")) null else uri.toString()
    } catch (_: Exception) {
        null
    }
}

private fun domainOf(url: String): String? = try {
    val host = java.net.URI(url).host
    host?.removePrefix("www.")
} catch (_: Exception) {
    null
}

private fun openUrl(context: android.content.Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {
        ZoneFormat.copyToClipboard(context, "Zone link", url)
    }
}

private fun exportLinksCsv(context: android.content.Context, links: List<LinkItem>) {
    val header = "Title,URL,Subjects,Favourite,Note,Saved"
    val rows = links.sortedBy { it.saved }.map { link ->
        listOf(
            link.title, link.url, link.tags.joinToString("; "),
            if (link.favorite) "yes" else "no", link.note, link.saved
        ).joinToString(",") { ZoneFormat.csvEscape(it) }
    }
    ZoneFormat.shareText(context, "Zone — links", (listOf(header) + rows).joinToString("\n"), "text/csv")
}
