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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.zone.app.data.Note
import com.zone.app.data.ZoneStore
import com.zone.app.ui.components.ConfirmDialog
import com.zone.app.ui.components.EmptyState
import com.zone.app.ui.components.FormDialog
import com.zone.app.ui.components.RoundAddButton
import com.zone.app.ui.components.ZoneScreen
import com.zone.app.ui.components.ZoneTextField
import com.zone.app.ui.components.zoneFieldColors
import com.zone.app.ui.theme.notePaper
import com.zone.app.ui.theme.zonePalette
import com.zone.app.util.ZoneFormat
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Note as NoteIcon
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Add

private val PAPER_COLORS = listOf("plain", "sand", "sage", "clay")

@Composable
fun NotesScreen() {
    val palette = zonePalette()
    val context = LocalContext.current

    var notes by remember {
        mutableStateOf(ZoneStore.readTyped<List<Note>>(ZoneStore.KEY_NOTES, emptyList()))
    }
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<Note?>(null) }
    var composing by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Note?>(null) }

    fun persist(list: List<Note>, action: String, title: String) {
        notes = sortNotes(list)
        ZoneStore.writeTyped(ZoneStore.KEY_NOTES, notes)
        ZoneStore.logAudit("notes", action, title)
    }

    val visible = notes.filter { note ->
        val q = query.trim().lowercase()
        q.isBlank() || note.title.lowercase().contains(q) || note.body.lowercase().contains(q)
    }
    val pinnedCount = notes.count { it.pinned }
    val totalWords = notes.sumOf { ZoneFormat.words(it.body) }

    ZoneScreen(
        title = "Notes",
        subtitle = "${notes.size} kept · $pinnedCount pinned · $totalWords words",
        actions = {
            IconButton(onClick = { exportNotesMarkdown(context, notes) }) {
                Icon(Icons.Rounded.Share, contentDescription = "Export as Markdown", tint = palette.accent)
            }
            Spacer(Modifier.size(4.dp))
            RoundAddButton(onClick = { composing = true }, contentDescription = "New note")
        }
    ) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search notes", color = palette.secondary) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = palette.secondary) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = zoneFieldColors(),
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
            )
        }
        if (visible.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Rounded.NoteIcon,
                    title = if (notes.isEmpty()) "A blank scratchpad" else "Nothing matches",
                    subtitle = if (notes.isEmpty())
                        "Jot the thing down before it escapes you."
                    else
                        "Try a different search."
                )
            }
        } else {
            items(visible, key = { it.id }) { note ->
                NoteCard(
                    note = note,
                    onClick = { editing = note; composing = true },
                    onTogglePin = { persist(notes.map { if (it.id == note.id) it.copy(pinned = !it.pinned) else it }, "update", note.title.ifBlank { "Untitled note" }) }
                )
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }

    if (composing) {
        NoteComposer(
            initial = editing,
            onDismiss = { composing = false; editing = null },
            onSave = { note ->
                val list = notes.toMutableList()
                val index = list.indexOfFirst { it.id == note.id }
                if (index >= 0) list[index] = note else list.add(note)
                persist(list, if (index >= 0) "update" else "add", note.title.ifBlank { "Untitled note" })
                composing = false
                editing = null
            },
            onDelete = editing?.let { target -> { composing = false; editing = null; deleting = target } }
        )
    }

    deleting?.let { target ->
        ConfirmDialog(
            title = "Delete note?",
            message = "“${target.title.ifBlank { "Untitled note" }}” will be removed.",
            confirmLabel = "Delete",
            onConfirm = { persist(notes.filterNot { it.id == target.id }, "delete", target.title.ifBlank { "Untitled note" }) },
            onDismiss = { deleting = null }
        )
    }
}

private fun sortNotes(list: List<Note>): List<Note> =
    list.sortedWith(compareByDescending<Note> { it.pinned }.thenByDescending { it.updated })

@Composable
private fun NoteCard(note: Note, onClick: () -> Unit, onTogglePin: () -> Unit) {
    val palette = zonePalette()
    val paper = notePaper(note.color)
    Column(
        Modifier
            .padding(horizontal = 20.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(paper)
            .border(0.5.dp, palette.separator, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(
                    note.title.ifBlank { "Untitled note" },
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.label,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onTogglePin, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Rounded.PushPin,
                    contentDescription = if (note.pinned) "Unpin" else "Pin",
                    tint = if (note.pinned) palette.accent else palette.secondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        if (note.body.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                note.body,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.label.copy(alpha = 0.85f),
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Edited ${ZoneFormat.relDate(note.updated)}",
                style = MaterialTheme.typography.labelSmall,
                color = palette.secondary
            )
            Spacer(Modifier.size(10.dp))
            Text(
                "${ZoneFormat.words(note.body)} words",
                style = MaterialTheme.typography.labelSmall,
                color = palette.secondary
            )
        }
    }
}

@Composable
private fun NoteComposer(
    initial: Note?,
    onDismiss: () -> Unit,
    onSave: (Note) -> Unit,
    onDelete: (() -> Unit)?
) {
    val palette = zonePalette()
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var body by remember { mutableStateOf(initial?.body ?: "") }
    var color by remember { mutableStateOf(initial?.color ?: "plain") }
    var pinned by remember { mutableStateOf(initial?.pinned ?: false) }

    FormDialog(
        title = if (initial == null) "New note" else "Note",
        confirmLabel = "Keep",
        confirmEnabled = title.trim().isNotEmpty() || body.trim().isNotEmpty(),
        onConfirm = {
            val now = ZoneFormat.todayISO()
            onSave(
                Note(
                    id = initial?.id ?: ZoneFormat.uid("note"),
                    title = title.trim().take(90),
                    body = body.trim(),
                    color = color,
                    pinned = pinned,
                    created = initial?.created ?: now,
                    updated = now
                )
            )
        },
        onDismiss = onDismiss
    ) {
        ZoneTextField(value = title, onValueChange = { title = it }, label = "Title")
        ZoneTextField(value = body, onValueChange = { body = it }, label = "Body", singleLine = false)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Paper", style = MaterialTheme.typography.labelSmall, color = palette.secondary)
            Spacer(Modifier.size(10.dp))
            PAPER_COLORS.forEach { c ->
                val selected = color == c
                Box(
                    Modifier
                        .padding(end = 8.dp)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(notePaper(c))
                        .border(if (selected) 2.dp else 0.5.dp, if (selected) palette.accent else palette.separator, CircleShape)
                        .clickable { color = c }
                )
            }
            Spacer(Modifier.size(10.dp))
            IconButton(onClick = { pinned = !pinned }) {
                Icon(
                    Icons.Rounded.PushPin,
                    contentDescription = "Pin",
                    tint = if (pinned) palette.accent else palette.secondary
                )
            }
        }
        Text(
            "${ZoneFormat.words(body)} words · ${ZoneFormat.readingMinutes(body)} min read",
            style = MaterialTheme.typography.labelSmall,
            color = palette.secondary
        )
        if (onDelete != null) {
            androidx.compose.material3.TextButton(onClick = onDelete) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = palette.red)
                Spacer(Modifier.size(4.dp))
                Text("Delete note", color = palette.red)
            }
        }
    }
}

private fun exportNotesMarkdown(context: android.content.Context, notes: List<Note>) {
    val pinnedFirst = notes.sortedWith(compareByDescending<Note> { it.pinned }.thenByDescending { it.updated })
    val sb = StringBuilder()
    sb.appendLine("# Zone — notes")
    sb.appendLine()
    pinnedFirst.forEach { note ->
        sb.appendLine("## ${if (note.pinned) "📌 " else ""}${note.title.ifBlank { "Untitled note" }}")
        sb.appendLine()
        sb.appendLine(note.body)
        sb.appendLine()
        sb.appendLine("_Edited ${note.updated}_")
        sb.appendLine()
    }
    ZoneFormat.shareText(context, "Zone — notes", sb.toString())
}
