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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zone.app.data.ListItem
import com.zone.app.data.ListStep
import com.zone.app.data.ZoneList
import com.zone.app.data.ZoneStore
import com.zone.app.ui.components.EmptyState
import com.zone.app.ui.components.FormDialog
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.List
import androidx.compose.material.icons.rounded.KeyboardArrowRight

private val TEMPLATES = linkedMapOf(
    "Blank" to emptyList(),
    "Weekend reset" to listOf("Change the sheets", "Clear the fridge", "Laundry, two loads", "Plan the week's meals"),
    "Trip packing" to listOf("Passport and tickets", "Chargers", "Toiletries", "One less top than you think"),
    "Launch checklist" to listOf("Write the announcement", "Update the changelog", "Tag the release", "Post and monitor"),
    "Grocery run" to listOf("Rice", "Dal", "Seasonal vegetables", "Coffee"),
    "Reading queue" to listOf("The current novel", "That essay in the tab", "The newsletter backlog")
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ListsScreen(onOpenList: (String) -> Unit) {
    val palette = zonePalette()

    var lists by remember {
        mutableStateOf(ZoneStore.readTyped<List<ZoneList>>(ZoneStore.KEY_LISTS, emptyList()))
    }
    var showNew by remember { mutableStateOf(false) }

    fun persist(list: List<ZoneList>, action: String, title: String) {
        lists = list
        ZoneStore.writeTyped(ZoneStore.KEY_LISTS, list)
        ZoneStore.logAudit("lists", action, title)
    }

    val openCount = lists.sumOf { l ->
        l.items.count { !it.done } + l.items.sumOf { it.children.count { child -> !child.done } }
    }
    val doneCount = lists.sumOf { l ->
        l.items.count { it.done } + l.items.sumOf { it.children.count { child -> child.done } }
    }

    ZoneScreen(
        title = "Lists",
        subtitle = "Things with steps, kept in order.",
        actions = {
            IconButton(
                onClick = { showNew = true },
                modifier = Modifier.size(36.dp).clip(CircleShape).background(palette.accent)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "New list", tint = Color.White)
            }
        }
    ) {
        item {
            Row(
                Modifier.padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ListMetric(value = "$openCount", label = "Still open", modifier = Modifier.weight(1f))
                ListMetric(value = "$doneCount", label = "Crossed off", modifier = Modifier.weight(1f))
                ListMetric(value = "${lists.size}", label = "Lists", modifier = Modifier.weight(1f))
            }
        }
        if (lists.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Rounded.List,
                    title = "No lists yet",
                    subtitle = "Start a blank list, or borrow a template from the + button."
                )
            }
        } else {
            item { SectionLabel("Your lists") }
            items(lists, key = { it.id }) { list ->
                ListRow(list = list, onClick = { onOpenList(list.id) })
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    if (showNew) {
        NewListDialog(
            onDismiss = { showNew = false },
            onCreate = { title, template ->
                val items = TEMPLATES[template].orEmpty().mapIndexed { index, text ->
                    ListItem(id = ZoneFormat.uid("li"), text = text)
                }
                val list = ZoneList(
                    id = ZoneFormat.uid("list"),
                    title = title,
                    createdAt = ZoneFormat.todayISO(),
                    items = items
                )
                persist(lists + list, "add", title)
                showNew = false
            }
        )
    }
}

@Composable
private fun ListMetric(value: String, label: String, modifier: Modifier = Modifier) {
    val palette = zonePalette()
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(palette.surface)
            .border(0.5.dp, palette.separator, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 19.sp, color = palette.label)
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = palette.secondary)
    }
}

internal fun listProgress(list: ZoneList): Pair<Int, Int> {
    val total = list.items.size + list.items.sumOf { it.children.size }
    val done = list.items.count { it.done } + list.items.sumOf { item -> item.children.count { it.done } }
    return done to total
}

@Composable
private fun ListRow(list: ZoneList, onClick: () -> Unit) {
    val palette = zonePalette()
    val (done, total) = listProgress(list)
    val pct = if (total == 0) 0f else done.toFloat() / total
    ZoneCard(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(if (pct >= 1f) palette.green.copy(alpha = 0.18f) else palette.grouped),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (pct >= 1f) "✓" else "${(pct * 100).toInt()}%",
                    fontWeight = FontWeight.Bold,
                    fontSize = if (pct >= 1f) 20.sp else 12.sp,
                    color = if (pct >= 1f) palette.green else palette.accent
                )
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    list.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                LinearProgressIndicator(
                    progress = { pct },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (pct >= 1f) palette.green else palette.accent,
                    trackColor = palette.grouped
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "$done of $total done · ${list.items.size} items",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = palette.secondary
                )
            }
            Spacer(Modifier.size(4.dp))
            Icon(Icons.Rounded.KeyboardArrowRight, contentDescription = "Open", tint = palette.secondary)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NewListDialog(
    onDismiss: () -> Unit,
    onCreate: (title: String, template: String) -> Unit
) {
    val palette = zonePalette()
    var title by remember { mutableStateOf("") }
    var template by remember { mutableStateOf("Blank") }

    FormDialog(
        title = "New list",
        confirmLabel = "Create",
        confirmEnabled = title.trim().isNotEmpty() || template != "Blank",
        onConfirm = {
            val resolved = title.trim().ifEmpty { template }
            onCreate(resolved.take(80), template)
        },
        onDismiss = onDismiss
    ) {
        ZoneTextField(value = title, onValueChange = { title = it }, label = "List title")
        Column {
            Text("Start from", style = MaterialTheme.typography.labelSmall, color = palette.secondary)
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TEMPLATES.keys.forEach { name ->
                    FilterChip(
                        selected = template == name,
                        onClick = { template = name },
                        label = { Text(name) },
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }
        }
    }
}
