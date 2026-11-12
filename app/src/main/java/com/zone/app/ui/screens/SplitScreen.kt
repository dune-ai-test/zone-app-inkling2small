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
import com.zone.app.data.SplitExpense
import com.zone.app.data.SplitGroup
import com.zone.app.data.SplitMember
import com.zone.app.data.ZoneStore
import com.zone.app.ui.components.EmptyState
import com.zone.app.ui.components.FormDialog
import com.zone.app.ui.components.IconWell
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
import androidx.compose.material.icons.rounded.CallSplit
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Share

private val GROUP_KINDS = listOf("Home", "Trip", "Work", "Other")

private fun kindTint(kind: String, palette: com.zone.app.ui.theme.ZonePalette) = when (kind) {
    "home", "Home" -> palette.indigo
    "trip", "Trip" -> palette.teal
    "work", "Work" -> palette.orange
    else -> palette.secondary
}

/** Per-member net: positive = paid more than their share, negative = owes. */
internal fun splitNets(group: SplitGroup): Map<String, Double> {
    val memberIds = group.members.map { it.id }.toSet()
    val paid = group.members.associate { it.id to 0.0 }.toMutableMap()
    val share = group.members.associate { it.id to 0.0 }.toMutableMap()
    group.expenses.forEach { expense ->
        val participants = expense.sharedWith.filter { it in memberIds }.ifEmpty { return@forEach }
        if (expense.paidBy in memberIds) {
            paid[expense.paidBy] = (paid[expense.paidBy] ?: 0.0) + expense.amount
        }
        val each = expense.amount / participants.size
        participants.forEach { id -> share[id] = (share[id] ?: 0.0) + each }
    }
    return group.members.associate { it.id to ((paid[it.id] ?: 0.0) - (share[it.id] ?: 0.0)) }
}

internal fun splitSharedTotal(group: SplitGroup): Double =
    group.expenses.filter { it.category != "settlement" }.sumOf { it.amount }

internal fun splitYourNet(group: SplitGroup): Double? {
    val nets = splitNets(group)
    val youIds = group.members.filter { it.name.equals("you", ignoreCase = true) }.map { it.id }
    if (youIds.isEmpty()) return null
    return youIds.sumOf { nets[it] ?: 0.0 }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SplitScreen(onOpenGroup: (String) -> Unit) {
    val palette = zonePalette()
    val context = androidx.compose.ui.platform.LocalContext.current

    var groups by remember {
        mutableStateOf(ZoneStore.readTyped<List<SplitGroup>>(ZoneStore.KEY_SPLIT, emptyList()))
    }
    var showNew by remember { mutableStateOf(false) }

    val settings = remember { ZoneStore.settings() }

    fun persist(list: List<SplitGroup>, action: String, title: String) {
        groups = list
        ZoneStore.writeTyped(ZoneStore.KEY_SPLIT, list)
        ZoneStore.logAudit("split", action, title)
    }

    val allNets = groups.map { splitYourNet(it) }
    val haveYou = allNets.all { it != null } && groups.isNotEmpty()
    val yourTotal = allNets.filterNotNull().sum()
    val totalEntries = groups.sumOf { it.expenses.size }
    val totalPeople = groups.flatMap { it.members }.map { it.name.lowercase() }.distinct().size

    ZoneScreen(
        title = "Split",
        subtitle = "Shared costs, without the arithmetic.",
        actions = {
            IconButton(onClick = { exportSplitCsv(context, groups) }) {
                Icon(Icons.Rounded.Share, contentDescription = "Export CSV", tint = palette.accent)
            }
            Spacer(Modifier.size(4.dp))
            IconButton(
                onClick = { showNew = true },
                modifier = Modifier.size(36.dp).clip(CircleShape).background(palette.accent)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Start a table", tint = Color.White)
            }
        }
    ) {
        item {
            ZoneCard(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text("YOUR POSITION, EVERY TABLE", style = MaterialTheme.typography.labelSmall, color = palette.secondary)
                Spacer(Modifier.height(2.dp))
                if (haveYou) {
                    Text(
                        when {
                            yourTotal > 0.005 -> "You're owed ${ZoneFormat.money(yourTotal, settings.currency)}"
                            yourTotal < -0.005 -> "You owe ${ZoneFormat.money(-yourTotal, settings.currency)}"
                            else -> "Square across ${groups.size} table${if (groups.size == 1) "" else "s"}"
                        },
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            yourTotal > 0.005 -> palette.green
                            yourTotal < -0.005 -> palette.red
                            else -> palette.label
                        }
                    )
                } else {
                    Text(
                        "${groups.size} table${if (groups.size == 1) "" else "s"} · $totalPeople people · $totalEntries entries",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.label
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Name a member “you” in a table to track your net across the desk.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.secondary
                )
            }
        }
        if (groups.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Rounded.CallSplit,
                    title = "No tables yet",
                    subtitle = "Start a table for the flat, the trip, the launch dinner — Zone tracks who owes whom."
                )
            }
        } else {
            item { SectionLabel("Your tables") }
            items(groups, key = { it.id }) { group ->
                SplitGroupRow(
                    group = group,
                    currency = settings.currency,
                    onClick = { onOpenGroup(group.id) }
                )
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    if (showNew) {
        NewGroupDialog(
            onDismiss = { showNew = false },
            onCreate = { name, kind, memberNames ->
                val members = memberNames.mapIndexed { index, n ->
                    SplitMember(id = ZoneFormat.uid("sm"), name = n.take(60))
                }
                val group = SplitGroup(
                    id = ZoneFormat.uid("group"),
                    name = name.take(80),
                    kind = kind.lowercase(),
                    members = members
                )
                persist(groups + group, "add", group.name)
                showNew = false
            }
        )
    }
}

@Composable
private fun SplitGroupRow(group: SplitGroup, currency: String, onClick: () -> Unit) {
    val palette = zonePalette()
    val yourNet = splitYourNet(group)
    val sharedTotal = splitSharedTotal(group)
    ZoneCard(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconWell(icon = Icons.Rounded.CallSplit, tint = kindTint(group.kind, palette))
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    group.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${group.members.size} people · ${group.expenses.size} entries · ${ZoneFormat.money(sharedTotal, currency)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.secondary
                )
                if (yourNet != null && kotlin.math.abs(yourNet) > 0.005) {
                    Text(
                        if (yourNet > 0) "+${ZoneFormat.money(yourNet, currency)} to you" else "−${ZoneFormat.money(-yourNet, currency)} from you",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (yourNet > 0) palette.green else palette.red
                    )
                }
            }
            Icon(Icons.Rounded.KeyboardArrowRight, contentDescription = "Open", tint = palette.secondary)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NewGroupDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, kind: String, memberNames: List<String>) -> Unit
) {
    val palette = zonePalette()
    var name by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf("Home") }
    var members by remember { mutableStateOf("you, ") }

    val parsedMembers = members.split(Regex("[,\n]")).map { it.trim() }.filter { it.isNotEmpty() }.distinct().take(12)

    FormDialog(
        title = "Start a table",
        confirmLabel = "Create",
        confirmEnabled = name.trim().isNotEmpty() && parsedMembers.size >= 2,
        onConfirm = { onCreate(name.trim(), kind, parsedMembers) },
        onDismiss = onDismiss
    ) {
        ZoneTextField(value = name, onValueChange = { name = it }, label = "Table name")
        Column {
            Text("Kind", style = MaterialTheme.typography.labelSmall, color = palette.secondary)
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                GROUP_KINDS.forEach { k ->
                    FilterChip(selected = kind == k, onClick = { kind = k }, label = { Text(k) }, shape = RoundedCornerShape(20.dp))
                }
            }
        }
        ZoneTextField(
            value = members,
            onValueChange = { members = it },
            label = "Members (comma separated)",
            singleLine = false
        )
        Text(
            "${parsedMembers.size} of 12 members · one of them must be “you” to track your net.",
            style = MaterialTheme.typography.bodySmall,
            color = palette.secondary
        )
    }
}

private fun exportSplitCsv(context: android.content.Context, groups: List<SplitGroup>) {
    val header = "Table,Kind,Date,Description,Category,Paid by,Amount,Shared with,Each share"
    val rows = groups.flatMap { group ->
        group.expenses.sortedBy { it.date }.map { expense ->
            val payer = group.members.firstOrNull { it.id == expense.paidBy }?.name ?: "?"
            val sharedNames = expense.sharedWith.mapNotNull { id -> group.members.firstOrNull { it.id == id }?.name }
            val each = if (sharedNames.isNotEmpty()) expense.amount / sharedNames.size else 0.0
            listOf(
                group.name, group.kind, expense.date, expense.description, expense.category,
                payer, expense.amount.toString(), sharedNames.joinToString("; "), String.format("%.2f", each)
            ).joinToString(",") { ZoneFormat.csvEscape(it) }
        }
    }
    ZoneFormat.shareText(context, "Zone — split tables", (listOf(header) + rows).joinToString("\n"), "text/csv")
}
