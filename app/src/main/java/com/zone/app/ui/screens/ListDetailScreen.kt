package com.zone.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zone.app.data.ListItem
import com.zone.app.data.ListStep
import com.zone.app.data.ZoneList
import com.zone.app.data.ZoneStore
import com.zone.app.ui.components.ConfirmDialog
import com.zone.app.ui.components.EmptyState
import com.zone.app.ui.components.FormDialog
import com.zone.app.ui.components.HairLine
import com.zone.app.ui.components.ZoneCard
import com.zone.app.ui.components.ZoneScreen
import com.zone.app.ui.components.ZoneTextField
import com.zone.app.ui.components.zoneFieldColors
import com.zone.app.ui.theme.zonePalette
import com.zone.app.util.ZoneFormat
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ViewAgenda

@Composable
fun ListDetailScreen(listId: String, onBack: () -> Unit) {
    val palette = zonePalette()
    val context = LocalContext.current

    var lists by remember {
        mutableStateOf(ZoneStore.readTyped<List<ZoneList>>(ZoneStore.KEY_LISTS, emptyList()))
    }
    val list = lists.firstOrNull { it.id == listId }

    var newItem by remember { mutableStateOf("") }
    var editingItem by remember { mutableStateOf<ListItem?>(null) }
    var editingStep by remember { mutableStateOf<Pair<String, ListStep>?>(null) } // itemId to step
    var addingStepTo by remember { mutableStateOf<String?>(null) }
    var stepText by remember { mutableStateOf("") }
    var confirmDeleteList by remember { mutableStateOf(false) }
    var confirmDeleteItem by remember { mutableStateOf<ListItem?>(null) }

    fun persist(updated: ZoneList, action: String, title: String) {
        lists = lists.map { if (it.id == updated.id) updated else it }
        ZoneStore.writeTyped(ZoneStore.KEY_LISTS, lists)
        ZoneStore.logAudit("lists", action, title)
    }

    if (list == null) {
        ZoneScreen(title = "Lists", subtitle = null, actions = null) {
            item {
                EmptyState(
                    icon = Icons.Rounded.ViewAgenda,
                    title = "This list is gone",
                    subtitle = "It may have been deleted on another device or from the back office."
                )
            }
        }
        return
    }

    val (done, total) = listProgress(list)
    val pct = if (total == 0) 0f else done.toFloat() / total
    val nextUp = list.items.firstOrNull { !it.done }?.children?.firstOrNull { !it.done }?.text
        ?: list.items.firstOrNull { !it.done }?.text

    fun toggleItem(target: ListItem) {
        val updatedItems = list.items.map { item ->
            if (item.id != target.id) item
            else {
                val nowDone = !item.done
                item.copy(done = nowDone, children = item.children.map { it.copy(done = nowDone) })
            }
        }
        persist(list.copy(items = updatedItems), "update", list.title)
    }

    fun toggleStep(itemId: String, step: ListStep) {
        val updatedItems = list.items.map { item ->
            if (item.id != itemId) item
            else {
                val children = item.children.map { s -> if (s.id == step.id) s.copy(done = !s.done) else s }
                item.copy(children = children, done = children.isNotEmpty() && children.all { it.done })
            }
        }
        persist(list.copy(items = updatedItems), "update", list.title)
    }

    fun moveItem(item: ListItem, delta: Int) {
        val index = list.items.indexOfFirst { it.id == item.id }
        if (index < 0) return
        val target = index + delta
        if (target < 0 || target >= list.items.size) return
        val reordered = list.items.toMutableList()
        val tmp = reordered[index]
        reordered[index] = reordered[target]
        reordered[target] = tmp
        persist(list.copy(items = reordered), "update", list.title)
    }

    fun addItem(text: String) {
        val item = ListItem(id = ZoneFormat.uid("li"), text = text.take(140))
        persist(list.copy(items = list.items + item), "add", text.take(80))
    }

    fun addStep(itemId: String, text: String) {
        val updated = list.items.map { item ->
            if (item.id == itemId) item.copy(children = item.children + ListStep(id = ZoneFormat.uid("lc"), text = text.take(140)))
            else item
        }
        // A parent with a fresh open step is no longer complete.
        val fixed = updated.map { if (it.id == itemId) it.copy(done = false) else it }
        persist(list.copy(items = fixed), "add", text.take(80))
    }

    ZoneScreen(
        title = list.title,
        subtitle = "$done of $total done",
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    androidx.compose.material.icons.Icons.Rounded.ChevronLeft,
                    contentDescription = "Back to lists",
                    tint = palette.accent
                )
            }
        },
        actions = {
            IconButton(onClick = { shareList(context, list) }) {
                Icon(Icons.Rounded.Share, contentDescription = "Copy as text", tint = palette.accent)
            }
            TextButton(onClick = { confirmDeleteList = true }) {
                Text("Delete", color = palette.red)
            }
        }
    ) {
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                LinearProgressIndicator(
                    progress = { pct },
                    modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
                    color = if (pct >= 1f) palette.green else palette.accent,
                    trackColor = palette.grouped
                )
                if (nextUp != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Next up: $nextUp",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        if (list.items.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Rounded.ViewAgenda,
                    title = "A clean page",
                    subtitle = "Add the first item below. Big items can carry steps of their own."
                )
            }
        } else {
            items(list.items, key = { it.id }) { item ->
                ListItemCard(
                    item = item,
                    onToggle = { toggleItem(item) },
                    onToggleStep = { step -> toggleStep(item.id, step) },
                    onAddStep = { addingStepTo = item.id; stepText = "" },
                    onEditItem = { editingItem = item },
                    onEditStep = { step -> editingStep = item.id to step },
                    onDeleteItem = { confirmDeleteItem = item },
                    onMoveUp = { moveItem(item, -1) },
                    onMoveDown = { moveItem(item, +1) }
                )
            }
        }
        item {
            Row(
                Modifier
                    .padding(horizontal = 20.dp)
                    .padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newItem,
                    onValueChange = { newItem = it },
                    placeholder = { Text("Add an item…", color = palette.secondary) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = zoneFieldColors(),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.size(8.dp))
                IconButton(
                    onClick = {
                        val text = newItem.trim()
                        if (text.isNotEmpty()) {
                            addItem(text)
                            newItem = ""
                        }
                    },
                    modifier = Modifier.size(46.dp).clip(CircleShape).background(palette.accent)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add item", tint = androidx.compose.ui.graphics.Color.White)
                }
            }
        }
        item { Spacer(Modifier.height(32.dp)) }
    }

    // --- dialogs ---

    editingItem?.let { target ->
        var text by remember(target.id) { mutableStateOf(target.text) }
        FormDialog(
            title = "Rename item",
            confirmLabel = "Save",
            confirmEnabled = text.trim().isNotEmpty(),
            onConfirm = {
                val updated = list.items.map { if (it.id == target.id) it.copy(text = text.trim().take(140)) else it }
                persist(list.copy(items = updated), "update", text.trim())
                editingItem = null
            },
            onDismiss = { editingItem = null }
        ) {
            ZoneTextField(value = text, onValueChange = { text = it }, label = "Item")
        }
    }

    editingStep?.let { (itemId, target) ->
        var text by remember(target.id) { mutableStateOf(target.text) }
        FormDialog(
            title = "Rename step",
            confirmLabel = "Save",
            confirmEnabled = text.trim().isNotEmpty(),
            onConfirm = {
                val updated = list.items.map { item ->
                    if (item.id == itemId) item.copy(children = item.children.map { s -> if (s.id == target.id) s.copy(text = text.trim().take(140)) else s })
                    else item
                }
                persist(list.copy(items = updated), "update", text.trim())
                editingStep = null
            },
            onDismiss = { editingStep = null }
        ) {
            ZoneTextField(value = text, onValueChange = { text = it }, label = "Step")
        }
    }

    addingStepTo?.let { itemId ->
        FormDialog(
            title = "Add a step",
            confirmLabel = "Add",
            confirmEnabled = stepText.trim().isNotEmpty(),
            onConfirm = {
                addStep(itemId, stepText.trim())
                addingStepTo = null
                stepText = ""
            },
            onDismiss = { addingStepTo = null }
        ) {
            ZoneTextField(value = stepText, onValueChange = { stepText = it }, label = "Step")
        }
    }

    confirmDeleteItem?.let { target ->
        ConfirmDialog(
            title = "Delete item?",
            message = "“${target.text}” and its steps will be removed.",
            confirmLabel = "Delete",
            onConfirm = {
                persist(list.copy(items = list.items.filterNot { it.id == target.id }), "delete", target.text)
            },
            onDismiss = { confirmDeleteItem = null }
        )
    }

    if (confirmDeleteList) {
        ConfirmDialog(
            title = "Delete this list?",
            message = "“${list.title}” will be permanently removed.",
            confirmLabel = "Delete",
            onConfirm = {
                lists = lists.filterNot { it.id == list.id }
                ZoneStore.writeTyped(ZoneStore.KEY_LISTS, lists)
                ZoneStore.logAudit("lists", "delete", list.title)
                onBack()
            },
            onDismiss = { confirmDeleteList = false }
        )
    }
}

@Composable
private fun ListItemCard(
    item: ListItem,
    onToggle: () -> Unit,
    onToggleStep: (ListStep) -> Unit,
    onAddStep: () -> Unit,
    onEditItem: () -> Unit,
    onEditStep: (ListStep) -> Unit,
    onDeleteItem: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val palette = zonePalette()
    ZoneCard(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundCheckbox(checked = item.done, onClick = onToggle, accent = palette.green)
            Spacer(Modifier.size(10.dp))
            Text(
                item.text,
                style = MaterialTheme.typography.bodyLarge,
                color = if (item.done) palette.secondary else palette.label,
                textDecoration = if (item.done) TextDecoration.LineThrough else null,
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onEditItem)
            )
            IconButton(onClick = onMoveUp, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Rounded.ArrowUpward, contentDescription = "Move up", tint = palette.secondary, modifier = Modifier.size(17.dp))
            }
            IconButton(onClick = onMoveDown, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Rounded.ArrowDownward, contentDescription = "Move down", tint = palette.secondary, modifier = Modifier.size(17.dp))
            }
            IconButton(onClick = onAddStep, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Rounded.Add, contentDescription = "Add step", tint = palette.accent, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDeleteItem, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = palette.red, modifier = Modifier.size(18.dp))
            }
        }
        if (item.children.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            item.children.forEach { step ->
                Row(
                    modifier = Modifier.padding(start = 46.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RoundCheckbox(checked = step.done, onClick = { onToggleStep(step) }, accent = palette.green, small = true)
                    Spacer(Modifier.size(8.dp))
                    Text(
                        step.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (step.done) palette.secondary else palette.label,
                        textDecoration = if (step.done) TextDecoration.LineThrough else null,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onEditStep(step) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RoundCheckbox(checked: Boolean, onClick: () -> Unit, accent: androidx.compose.ui.graphics.Color, small: Boolean = false) {
    val palette = zonePalette()
    val size = if (small) 20.dp else 24.dp
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (checked) accent else palette.grouped)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier.size(size * 0.68f)
            )
        }
    }
}

private fun shareList(context: android.content.Context, list: ZoneList) {
    val sb = StringBuilder()
    sb.appendLine(list.title)
    sb.appendLine()
    list.items.forEach { item ->
        sb.appendLine("[${if (item.done) "x" else " "}] ${item.text}")
        item.children.forEach { step ->
            sb.appendLine("    [${if (step.done) "x" else " "}] ${step.text}")
        }
    }
    ZoneFormat.shareText(context, "Zone — ${list.title}", sb.toString())
}
