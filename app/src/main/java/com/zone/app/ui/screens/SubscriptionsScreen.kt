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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.zone.app.data.Subscription
import com.zone.app.data.ZoneStore
import com.zone.app.ui.components.ConfirmDialog
import com.zone.app.ui.components.EmptyState
import com.zone.app.ui.components.FormDialog
import com.zone.app.ui.components.SectionLabel
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
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.ui.text.input.KeyboardType
import java.time.Instant
import java.time.ZoneOffset

private val CYCLES = listOf("Monthly", "Yearly", "Weekly")
private val CATEGORIES = listOf("Software", "Streaming", "Music", "News", "Fitness", "Cloud", "Utilities", "Entertainment", "Education", "Other")

private fun monthlyOf(sub: Subscription): Double = when (sub.cycle) {
    "Yearly" -> sub.amount / 12.0
    "Weekly" -> sub.amount * 52.0 / 12.0
    else -> sub.amount
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SubscriptionsScreen() {
    val palette = zonePalette()
    val context = LocalContext.current

    var subs by remember {
        mutableStateOf(ZoneStore.readTyped<List<Subscription>>(ZoneStore.KEY_SUBSCRIPTIONS, emptyList()))
    }
    var query by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("all") } // all | Active | Paused
    var editing by remember { mutableStateOf<Subscription?>(null) }
    var showComposer by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Subscription?>(null) }

    val settings = remember { ZoneStore.settings() }
    val currency = settings.currency

    fun persist(list: List<Subscription>, action: String, title: String) {
        subs = list.sortedBy { it.next }
        ZoneStore.writeTyped(ZoneStore.KEY_SUBSCRIPTIONS, subs)
        ZoneStore.logAudit("subscriptions", action, title)
    }

    val active = subs.filter { it.status == "Active" }
    val monthlyBurn = active.sumOf { monthlyOf(it) }
    val annualBurn = monthlyBurn * 12
    val dueIn7 = active.filter { ZoneFormat.daysUntil(it.next) in 0..7 }.sumOf { it.amount }
    val averagePlan = if (active.isNotEmpty()) monthlyBurn / active.size else 0.0
    val categoryTotals = active
        .groupBy { it.category }
        .mapValues { (_, list) -> list.sumOf { monthlyOf(it) } }
        .entries.sortedByDescending { it.value }
    val biggestCategory = categoryTotals.firstOrNull()?.key ?: "—"
    val nextCharge = active.minByOrNull { it.next }

    val visible = subs.filter { sub ->
        val q = query.trim().lowercase()
        val queryOk = q.isBlank() ||
            sub.name.lowercase().contains(q) ||
            sub.category.lowercase().contains(q) ||
            sub.cycle.lowercase().contains(q) ||
            sub.next.contains(q)
        val statusOk = statusFilter == "all" || sub.status == statusFilter
        queryOk && statusOk
    }.sortedWith(compareByDescending<Subscription> { it.status == "Active" }.thenBy { it.next })

    ZoneScreen(
        title = "Subscriptions",
        subtitle = "The quiet monthly cost of it all.",
        actions = {
            IconButton(onClick = { exportSubscriptionsCsv(context, subs) }) {
                Icon(Icons.Rounded.Share, contentDescription = "Export CSV", tint = palette.accent)
            }
            Spacer(Modifier.size(4.dp))
            IconButton(
                onClick = { showComposer = true; editing = null },
                modifier = Modifier.size(36.dp).clip(CircleShape).background(palette.accent)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add subscription", tint = Color.White)
            }
        }
    ) {
        item {
            // Normalised burn hero
            ZoneCard(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text("NORMALISED BURN", style = MaterialTheme.typography.labelSmall, color = palette.secondary)
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        ZoneFormat.money(monthlyBurn, currency),
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.label
                    )
                    Text(
                        " /mo",
                        fontSize = 17.sp,
                        color = palette.secondary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    "${ZoneFormat.money(annualBurn, currency)} a year · ${active.size} active plan${if (active.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.secondary
                )
                if (nextCharge != null) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(palette.accent))
                        Spacer(Modifier.size(6.dp))
                        Text(
                            "Next: ${nextCharge.name} · ${dueLabel(nextCharge.next)} · ${ZoneFormat.money(nextCharge.amount, currency)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        item {
            Row(
                Modifier.padding(horizontal = 20.dp).padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatTile(value = ZoneFormat.money(dueIn7, currency), label = "Due in 7 days", modifier = Modifier.weight(1f))
                StatTile(value = ZoneFormat.money(averagePlan, currency), label = "Average plan", modifier = Modifier.weight(1f))
                StatTile(value = biggestCategory, label = "Biggest category", modifier = Modifier.weight(1f))
            }
        }
        if (categoryTotals.isNotEmpty()) {
            item {
                ZoneCard(modifier = Modifier.padding(horizontal = 20.dp).padding(top = 10.dp)) {
                    Text("WHERE IT GOES", style = MaterialTheme.typography.labelSmall, color = palette.secondary)
                    Spacer(Modifier.height(10.dp))
                    val maxCategory = categoryTotals.maxOf { it.value }
                    categoryTotals.take(6).forEach { (category, total) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(category, style = MaterialTheme.typography.bodyMedium, color = palette.label, modifier = Modifier.width(112.dp), maxLines = 1)
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(palette.grouped)
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxWidth(if (maxCategory > 0.0) (total / maxCategory).toFloat().coerceIn(0.05f, 1f) else 0.05f)
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(palette.accent.copy(alpha = 0.7f))
                                )
                            }
                            Spacer(Modifier.size(8.dp))
                            Text(ZoneFormat.money(total, currency), style = MaterialTheme.typography.bodySmall, color = palette.secondary)
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
        }
        item {
            Row(
                Modifier.padding(horizontal = 20.dp).padding(top = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionLabelInlineSub("Card catalogue — ${visible.size}" + if (visible.size != subs.size) " of ${subs.size}" else "")
                Spacer(Modifier.weight(1f))
            }
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search subscriptions", color = palette.secondary) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = palette.secondary) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = zoneFieldColors(),
                modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            FlowRow(
                modifier = Modifier.padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("all" to "All", "Active" to "Active", "Paused" to "Paused").forEach { (id, label) ->
                    FilterChip(
                        selected = statusFilter == id,
                        onClick = { statusFilter = id },
                        label = { Text(label) },
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }
        }
        if (visible.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Rounded.CreditCard,
                    title = if (subs.isEmpty()) "The quiet ledger is empty" else "Nothing matches",
                    subtitle = if (subs.isEmpty())
                        "Add the first plan and Zone will keep the monthly honest."
                    else
                        "Try a different search or filter."
                )
            }
        } else {
            items(visible, key = { it.id }) { sub ->
                SubscriptionCard(
                    sub = sub,
                    currency = currency,
                    onClick = { editing = sub; showComposer = true },
                    onTogglePause = {
                        val next = if (sub.status == "Active") "Paused" else "Active"
                        persist(subs.map { if (it.id == sub.id) it.copy(status = next) else it }, "update", "${sub.name} → $next")
                    },
                    onDelete = { deleting = sub }
                )
            }
        }
        item { Spacer(Modifier.height(28.dp)) }
    }

    if (showComposer) {
        SubscriptionComposer(
            initial = editing,
            onDismiss = { showComposer = false; editing = null },
            onSave = { sub ->
                val list = subs.toMutableList()
                val index = list.indexOfFirst { it.id == sub.id }
                if (index >= 0) list[index] = sub else list.add(sub)
                persist(list, if (index >= 0) "update" else "add", sub.name)
                showComposer = false
                editing = null
            },
            onDelete = editing?.let { target -> { showComposer = false; editing = null; deleting = target } }
        )
    }

    deleting?.let { target ->
        ConfirmDialog(
            title = "Cancel subscription?",
            message = "“${target.name}” will be removed from the catalogue.",
            confirmLabel = "Remove",
            onConfirm = { persist(subs.filterNot { it.id == target.id }, "delete", target.name) },
            onDismiss = { deleting = null }
        )
    }
}

private fun dueLabel(isoDate: String): String {
    val days = ZoneFormat.daysUntil(isoDate)
    return when {
        days == 0L -> "due today"
        days == 1L -> "due tomorrow"
        days in 2..30 -> "in $days days"
        days < 0 -> "${-days} days late"
        else -> ZoneFormat.dateMedium(isoDate)
    }
}

@Composable
private fun SectionLabelInlineSub(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = zonePalette().secondary
    )
}

@Composable
private fun SubscriptionCard(
    sub: Subscription,
    currency: String,
    onClick: () -> Unit,
    onTogglePause: () -> Unit,
    onDelete: () -> Unit
) {
    val palette = zonePalette()
    val isActive = sub.status == "Active"
    val monthly = monthlyOf(sub)
    val days = ZoneFormat.daysUntil(sub.next)
    val dueColor = when {
        !isActive -> palette.secondary
        days < 0 -> palette.red
        days <= 7 -> palette.orange
        else -> palette.secondary
    }

    ZoneCard(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(palette.grouped),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    sub.name.take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    color = if (isActive) palette.accent else palette.secondary
                )
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    sub.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isActive) palette.label else palette.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${sub.category} · ${sub.cycle}",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.secondary
                )
                Spacer(Modifier.height(3.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(dueColor.copy(alpha = 0.14f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        if (isActive) "Next charge ${dueLabel(sub.next)}" else "Paused",
                        style = MaterialTheme.typography.labelSmall,
                        color = dueColor
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    ZoneFormat.money(sub.amount, currency),
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.label
                )
                Text(
                    "≈ ${ZoneFormat.money(monthly, currency)}/mo",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.secondary
                )
                Row {
                    IconButton(onClick = onTogglePause, modifier = Modifier.size(30.dp)) {
                        Icon(
                            if (isActive) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isActive) "Pause" else "Resume",
                            tint = palette.secondary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove", tint = palette.red, modifier = Modifier.size(17.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SubscriptionComposer(
    initial: Subscription?,
    onDismiss: () -> Unit,
    onSave: (Subscription) -> Unit,
    onDelete: (() -> Unit)?
) {
    val palette = zonePalette()
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var amount by remember { mutableStateOf(initial?.amount?.toString()?.removeSuffix(".0") ?: "") }
    var cycle by remember { mutableStateOf(initial?.cycle ?: "Monthly") }
    var next by remember { mutableStateOf(initial?.next ?: ZoneFormat.todayISO()) }
    var category by remember { mutableStateOf(initial?.category ?: "Software") }
    var showDatePicker by remember { mutableStateOf(false) }

    val amountNum = amount.toDoubleOrNull() ?: 0.0
    val monthlyPreview = when (cycle) {
        "Yearly" -> amountNum / 12.0
        "Weekly" -> amountNum * 52.0 / 12.0
        else -> amountNum
    }

    FormDialog(
        title = if (initial == null) "Add subscription" else "Edit subscription",
        confirmLabel = if (initial == null) "Add" else "Save",
        confirmEnabled = name.trim().isNotEmpty() && amountNum > 0.0,
        onConfirm = {
            onSave(
                Subscription(
                    id = initial?.id ?: ZoneFormat.uid("sub"),
                    name = name.trim().take(80),
                    amount = amountNum,
                    cycle = cycle,
                    next = next,
                    category = category,
                    status = initial?.status ?: "Active"
                )
            )
        },
        onDismiss = onDismiss
    ) {
        ZoneTextField(value = name, onValueChange = { name = it }, label = "Plan name")
        ZoneTextField(value = amount, onValueChange = { amount = it }, label = "Amount per charge", keyboardType = KeyboardType.Decimal)
        Column {
            Text("Cycle", style = MaterialTheme.typography.labelSmall, color = palette.secondary)
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CYCLES.forEach { c ->
                    FilterChip(selected = cycle == c, onClick = { cycle = c }, label = { Text(c) }, shape = RoundedCornerShape(20.dp))
                }
            }
        }
        Column {
            Text("Category", style = MaterialTheme.typography.labelSmall, color = palette.secondary)
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CATEGORIES.forEach { c ->
                    FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c) }, shape = RoundedCornerShape(20.dp))
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Next charge", style = MaterialTheme.typography.labelSmall, color = palette.secondary)
                Text(ZoneFormat.dateMedium(next), style = MaterialTheme.typography.bodyMedium, color = palette.label)
            }
            TextButton(onClick = { showDatePicker = true }) { Text("Change", color = palette.accent) }
        }
        if (amountNum > 0) {
            Text(
                "≈ ${ZoneFormat.money(monthlyPreview, currencyOf())}/month · ${ZoneFormat.money(monthlyPreview * 12, currencyOf())}/year",
                style = MaterialTheme.typography.bodySmall,
                color = palette.accent
            )
        }
        if (onDelete != null) {
            TextButton(onClick = onDelete) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = palette.red)
                Spacer(Modifier.size(4.dp))
                Text("Remove from catalogue", color = palette.red)
            }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = ZoneFormat.parseISO(next).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        next = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().let(ZoneFormat::toISO)
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

/** Reads the display currency from the settings store (cheap, cached). */
private fun currencyOf(): String = ZoneStore.settings().currency

private fun exportSubscriptionsCsv(context: android.content.Context, subs: List<Subscription>) {
    val header = "Name,Amount,Cycle,Next charge,Category,Status,Monthly equivalent"
    val rows = subs.sortedBy { it.next }.map { sub ->
        listOf(
            sub.name, sub.amount.toString(), sub.cycle, sub.next, sub.category, sub.status,
            "%.2f".format(monthlyOf(sub))
        ).joinToString(",") { ZoneFormat.csvEscape(it) }
    }
    ZoneFormat.shareText(context, "Zone — subscriptions", (listOf(header) + rows).joinToString("\n"), "text/csv")
}
