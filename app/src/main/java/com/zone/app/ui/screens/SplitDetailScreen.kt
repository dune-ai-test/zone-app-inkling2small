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
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zone.app.data.SplitExpense
import com.zone.app.data.SplitGroup
import com.zone.app.data.SplitMember
import com.zone.app.data.ZoneStore
import com.zone.app.ui.components.ConfirmDialog
import com.zone.app.ui.components.EmptyState
import com.zone.app.ui.components.FormDialog
import com.zone.app.ui.components.HairLine
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
import androidx.compose.material.icons.rounded.CallSplit
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import java.time.Instant
import java.time.ZoneOffset
import kotlin.math.abs
import kotlin.math.max

private val EXPENSE_CATEGORIES = listOf("home", "food", "travel", "bills", "fun", "other")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SplitDetailScreen(groupId: String, onBack: () -> Unit) {
    val palette = zonePalette()
    val context = LocalContext.current

    var groups by remember {
        mutableStateOf(ZoneStore.readTyped<List<SplitGroup>>(ZoneStore.KEY_SPLIT, emptyList()))
    }
    val group = groups.firstOrNull { it.id == groupId }
    val settings = remember { ZoneStore.settings() }

    var query by remember { mutableStateOf("") }
    var categoryFilter by remember { mutableStateOf("all") }
    var showExpenseDialog by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<SplitExpense?>(null) }
    var showMemberDialog by remember { mutableStateOf(false) }
    var confirmDeleteGroup by remember { mutableStateOf(false) }
    var confirmDeleteExpense by remember { mutableStateOf<SplitExpense?>(null) }

    fun persist(updated: SplitGroup, action: String, title: String) {
        groups = groups.map { if (it.id == updated.id) updated else it }
        ZoneStore.writeTyped(ZoneStore.KEY_SPLIT, groups)
        ZoneStore.logAudit("split", action, title)
    }

    if (group == null) {
        ZoneScreen(
            title = "Split",
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ChevronLeft, contentDescription = "Back", tint = palette.accent)
                }
            }
        ) {
            item {
                EmptyState(
                    icon = Icons.Rounded.CallSplit,
                    title = "This table is gone",
                    subtitle = "It may have been removed from another device or the back office."
                )
            }
        }
        return
    }

    val nets = splitNets(group)
    val yourNet = splitYourNet(group)
    val sharedTotal = splitSharedTotal(group)
    val settlements = group.expenses.filter { it.category == "settlement" }.sumOf { it.amount }
    val maxPaid = (group.members.maxOfOrNull { m ->
        group.expenses.filter { it.paidBy == m.id }.sumOf { it.amount }
    } ?: 1.0).coerceAtLeast(1.0)
    val transfers = ZoneFormat.settlePlan(group.members, nets)
    val memberName = remember(group.members) { group.members.associate { it.id to it.name } }

    fun recordSettlements(plan: List<Triple<String, String, Double>>) {
        if (plan.isEmpty()) return
        val fresh = plan.map { (from, to, amount) ->
            SplitExpense(
                id = ZoneFormat.uid("gx"),
                description = "Settlement",
                amount = amount,
                paidBy = from,
                date = ZoneFormat.todayISO(),
                sharedWith = listOf(to),
                category = "settlement"
            )
        }
        persist(group.copy(expenses = group.expenses + fresh), "update", "Settled ${plan.size} transfer${if (plan.size == 1) "" else "s"}")
    }

    val presentCategories = (listOf("all") + (group.expenses.map { it.category }.distinct().sorted()))

    val visibleExpenses = group.expenses
        .filter { expense ->
            val q = query.trim().lowercase()
            val categoryOk = categoryFilter == "all" || expense.category == categoryFilter
            val queryOk = q.isBlank() ||
                expense.description.lowercase().contains(q) ||
                expense.category.lowercase().contains(q) ||
                (memberName[expense.paidBy]?.lowercase()?.contains(q) == true) ||
                expense.date.contains(q) ||
                expense.amount.toString().contains(q)
            categoryOk && queryOk
        }
        .sortedByDescending { it.date }
    val monthGroups = visibleExpenses.groupBy { it.date.take(7) }.toSortedMap(compareByDescending { it })

    ZoneScreen(
        title = group.name,
        subtitle = "${group.members.size} people · ${group.expenses.size} entries",
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ChevronLeft, contentDescription = "Back", tint = palette.accent)
            }
        },
        actions = {
            IconButton(onClick = { shareGroupSummary(context, group, settings.currency) }) {
                Icon(Icons.Rounded.Share, contentDescription = "Copy summary", tint = palette.accent)
            }
            IconButton(
                onClick = { showExpenseDialog = true; editingExpense = null },
                modifier = Modifier.size(36.dp).clip(CircleShape).background(palette.accent)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add expense", tint = Color.White)
            }
        }
    ) {
        // --- position hero
        item {
            ZoneCard(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text("TABLE POSITION", style = MaterialTheme.typography.labelSmall, color = palette.secondary)
                Spacer(Modifier.height(2.dp))
                Text(
                    when {
                        yourNet == null -> "${ZoneFormat.money(sharedTotal, settings.currency)} shared"
                        yourNet > 0.005 -> "You're owed ${ZoneFormat.money(yourNet, settings.currency)}"
                        yourNet < -0.005 -> "You owe ${ZoneFormat.money(-yourNet, settings.currency)}"
                        else -> "You're square"
                    },
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        yourNet == null -> palette.label
                        yourNet > 0.005 -> palette.green
                        yourNet < -0.005 -> palette.red
                        else -> palette.label
                    }
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${ZoneFormat.money(sharedTotal, settings.currency)} across ${group.expenses.count { it.category != "settlement" }} entries" +
                        if (settlements > 0) " · ${ZoneFormat.money(settlements, settings.currency)} settled" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.secondary
                )
            }
        }
        // --- balance sheet
        item {
            SectionLabel("Balance sheet")
            ZoneCard(modifier = Modifier.padding(horizontal = 20.dp)) {
                Row {
                    Text("NAME", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = palette.secondary, modifier = Modifier.weight(1.2f))
                    Text("PAID", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = palette.secondary, modifier = Modifier.weight(1f))
                    Text("SHARE", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = palette.secondary, modifier = Modifier.weight(1f))
                    Text("NET", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = palette.secondary, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(6.dp))
                group.members.forEachIndexed { index, member ->
                    val paid = group.expenses.filter { it.paidBy == member.id }.sumOf { it.amount }
                    val share = paid - (nets[member.id] ?: 0.0)
                    val net = nets[member.id] ?: 0.0
                    Column(Modifier.padding(vertical = 6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                member.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.label,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1.2f)
                            )
                            Text(ZoneFormat.money(paid, settings.currency), style = MaterialTheme.typography.bodySmall, color = palette.secondary, modifier = Modifier.weight(1f))
                            Text(ZoneFormat.money(share, settings.currency), style = MaterialTheme.typography.bodySmall, color = palette.secondary, modifier = Modifier.weight(1f))
                            Text(
                                (if (net >= 0) "+" else "−") + ZoneFormat.money(abs(net), settings.currency),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (net >= 0.005) palette.green else if (net <= -0.005) palette.red else palette.secondary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(palette.grouped)
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth((paid / maxPaid).toFloat().coerceIn(0f, 1f))
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(palette.accent.copy(alpha = 0.7f))
                            )
                        }
                    }
                    if (index != group.members.lastIndex) HairLine()
                }
            }
        }
        // --- settle up
        if (transfers.isNotEmpty()) {
            item {
                Row(
                    Modifier.padding(horizontal = 20.dp).padding(top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionLabelInline("Settle up")
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { recordSettlements(transfers) }) {
                        Text("Record all", color = palette.accent, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            items(transfers, key = { "${it.first}->${it.second}" }) { transfer ->
                val (from, to, amount) = transfer
                ZoneCard(modifier = Modifier.padding(horizontal = 20.dp, vertical = 3.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${memberName[from] ?: "?"} → ${memberName[to] ?: "?"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.label,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            ZoneFormat.money(amount, settings.currency),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.label
                        )
                        Spacer(Modifier.size(6.dp))
                        TextButton(onClick = { recordSettlements(listOf(transfer)) }) {
                            Text("Record", color = palette.accent)
                        }
                    }
                }
            }
        }
        // --- members
        item {
            Row(
                Modifier.padding(horizontal = 20.dp).padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionLabelInline("Members")
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { showMemberDialog = true }) {
                    Text("+ Add", color = palette.accent)
                }
            }
        }
        item {
            FlowRow(
                modifier = Modifier.padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                group.members.forEach { member ->
                    androidx.compose.material3.AssistChip(
                        onClick = {},
                        label = { Text(member.name) },
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }
        }
        // --- ledger
        item {
            Row(
                Modifier.padding(horizontal = 20.dp).padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionLabelInline("Ledger")
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { confirmDeleteGroup = true }) {
                    Text("Remove table", color = palette.red)
                }
            }
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search this table", color = palette.secondary) },
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
                presentCategories.forEach { category ->
                    FilterChip(
                        selected = categoryFilter == category,
                        onClick = { categoryFilter = category },
                        label = { Text(category.replaceFirstChar { it.uppercase() }) },
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }
        }
        if (visibleExpenses.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Rounded.CallSplit,
                    title = if (group.expenses.isEmpty()) "First entry, anyone?" else "Nothing matches",
                    subtitle = if (group.expenses.isEmpty())
                        "Tap the + button. Zone does the arithmetic from there."
                    else
                        "Try clearing the search or the category filter."
                )
            }
        } else {
            monthGroups.forEach { (month, expenses) ->
                item(key = "month-$month") { SectionLabel(ZoneFormat.monthLabel(month)) }
                items(expenses, key = { it.id }) { expense ->
                    ExpenseRow(
                        expense = expense,
                        payerName = memberName[expense.paidBy] ?: "?",
                        sharedCount = expense.sharedWith.size,
                        currency = settings.currency,
                        onEdit = { editingExpense = expense; showExpenseDialog = true },
                        onDelete = { confirmDeleteExpense = expense }
                    )
                }
            }
        }
        item { Spacer(Modifier.height(28.dp)) }
    }

    // ---------- dialogs ----------

    if (showExpenseDialog) {
        ExpenseDialog(
            group = group,
            initial = editingExpense,
            onDismiss = { showExpenseDialog = false; editingExpense = null },
            onSave = { expense ->
                val list = group.expenses.toMutableList()
                val index = list.indexOfFirst { it.id == expense.id }
                if (index >= 0) list[index] = expense else list.add(expense)
                persist(group.copy(expenses = list), if (index >= 0) "update" else "add", expense.description.ifBlank { "Entry" })
                showExpenseDialog = false
                editingExpense = null
            }
        )
    }

    if (showMemberDialog) {
        var name by remember { mutableStateOf("") }
        FormDialog(
            title = "Add member",
            confirmLabel = "Add",
            confirmEnabled = name.trim().isNotEmpty() && group.members.size < 12,
            onConfirm = {
                val member = SplitMember(id = ZoneFormat.uid("sm"), name = name.trim().take(60))
                persist(group.copy(members = group.members + member), "update", "Added ${member.name}")
                showMemberDialog = false
            },
            onDismiss = { showMemberDialog = false }
        ) {
            ZoneTextField(value = name, onValueChange = { name = it }, label = "Member name")
            Text(
                "${group.members.size} of 12 seats taken.",
                style = MaterialTheme.typography.bodySmall,
                color = palette.secondary
            )
        }
    }

    confirmDeleteExpense?.let { target ->
        ConfirmDialog(
            title = "Delete entry?",
            message = "“${target.description}” · ${ZoneFormat.money(target.amount, settings.currency)}",
            confirmLabel = "Delete",
            onConfirm = {
                persist(group.copy(expenses = group.expenses.filterNot { it.id == target.id }), "delete", target.description)
            },
            onDismiss = { confirmDeleteExpense = null }
        )
    }

    if (confirmDeleteGroup) {
        ConfirmDialog(
            title = "Remove this table?",
            message = "“${group.name}” and all of its ${group.expenses.size} entries will be deleted.",
            confirmLabel = "Remove",
            onConfirm = {
                groups = groups.filterNot { it.id == group.id }
                ZoneStore.writeTyped(ZoneStore.KEY_SPLIT, groups)
                ZoneStore.logAudit("split", "delete", group.name)
                onBack()
            },
            onDismiss = { confirmDeleteGroup = false }
        )
    }
}

@Composable
private fun SectionLabelInline(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = zonePalette().secondary
    )
}

@Composable
private fun ExpenseRow(
    expense: SplitExpense,
    payerName: String,
    sharedCount: Int,
    currency: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val palette = zonePalette()
    val isSettlement = expense.category == "settlement"
    val dotColor = when (expense.category) {
        "home" -> palette.indigo
        "food" -> palette.orange
        "travel" -> palette.teal
        "bills" -> palette.purple
        "fun" -> palette.pink
        "settlement" -> palette.green
        else -> palette.secondary
    }
    ZoneCard(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(dotColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    expense.category.take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    color = dotColor
                )
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    expense.description.ifBlank { expense.category.replaceFirstChar { it.uppercase() } },
                    style = MaterialTheme.typography.bodyLarge,
                    color = palette.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${ZoneFormat.dateShort(expense.date)} · $payerName paid · split $sharedCount way${if (sharedCount == 1) "" else "s"}${if (isSettlement) " · settlement" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.secondary
                )
            }
            Text(
                ZoneFormat.money(expense.amount, currency),
                style = MaterialTheme.typography.titleMedium,
                color = palette.label
            )
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Rounded.Edit, contentDescription = "Edit", tint = palette.secondary, modifier = Modifier.size(17.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = palette.red, modifier = Modifier.size(17.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ExpenseDialog(
    group: SplitGroup,
    initial: SplitExpense?,
    onDismiss: () -> Unit,
    onSave: (SplitExpense) -> Unit
) {
    val palette = zonePalette()
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var amount by remember { mutableStateOf(initial?.amount?.toString()?.removeSuffix(".0") ?: "") }
    var category by remember { mutableStateOf(initial?.category?.takeIf { it != "settlement" } ?: "food") }
    var date by remember { mutableStateOf(initial?.date ?: ZoneFormat.todayISO()) }
    var paidBy by remember { mutableStateOf(initial?.paidBy ?: group.members.firstOrNull()?.id.orEmpty()) }
    var sharedWith by remember {
        mutableStateOf((initial?.sharedWith ?: group.members.map { it.id }).toSet())
    }
    var showDatePicker by remember { mutableStateOf(false) }

    FormDialog(
        title = if (initial == null) "Add expense" else "Edit expense",
        confirmLabel = if (initial == null) "Add" else "Save",
        confirmEnabled = (amount.toDoubleOrNull() ?: 0.0) > 0.0 && description.trim().isNotEmpty() && paidBy.isNotBlank() && sharedWith.isNotEmpty(),
        onConfirm = {
            onSave(
                SplitExpense(
                    id = initial?.id ?: ZoneFormat.uid("gx"),
                    description = description.trim().take(140),
                    amount = amount.toDoubleOrNull() ?: 0.0,
                    paidBy = paidBy,
                    date = date,
                    sharedWith = group.members.map { it.id }.filter { it in sharedWith },
                    category = category
                )
            )
        },
        onDismiss = onDismiss
    ) {
        ZoneTextField(value = description, onValueChange = { description = it }, label = "What was it for?")
        ZoneTextField(value = amount, onValueChange = { amount = it }, label = "Amount", keyboardType = KeyboardType.Decimal)

        Column {
            Text("Category", style = MaterialTheme.typography.labelSmall, color = palette.secondary)
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                EXPENSE_CATEGORIES.forEach { c ->
                    FilterChip(
                        selected = category == c,
                        onClick = { category = c },
                        label = { Text(c.replaceFirstChar { it.uppercase() }) },
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Date", style = MaterialTheme.typography.labelSmall, color = palette.secondary)
                Text(ZoneFormat.dateMedium(date), style = MaterialTheme.typography.bodyMedium, color = palette.label)
            }
            TextButton(onClick = { showDatePicker = true }) { Text("Change", color = palette.accent) }
        }

        Column {
            Text("Paid by", style = MaterialTheme.typography.labelSmall, color = palette.secondary)
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                group.members.forEach { member ->
                    FilterChip(
                        selected = paidBy == member.id,
                        onClick = { paidBy = member.id },
                        label = { Text(member.name) },
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }
        }

        Column {
            Text("Split equally across", style = MaterialTheme.typography.labelSmall, color = palette.secondary)
            group.members.forEach { member ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable {
                    sharedWith = if (member.id in sharedWith) sharedWith - member.id else sharedWith + member.id
                }) {
                    Checkbox(
                        checked = member.id in sharedWith,
                        onCheckedChange = null
                    )
                    Text(member.name, style = MaterialTheme.typography.bodyMedium, color = palette.label)
                }
            }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = ZoneFormat.parseISO(date).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
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

private fun shareGroupSummary(context: android.content.Context, group: SplitGroup, currency: String) {
    val nets = splitNets(group)
    val sb = StringBuilder()
    sb.appendLine("Zone — ${group.name}")
    sb.appendLine()
    group.members.forEach { member ->
        val net = nets[member.id] ?: 0.0
        val paid = group.expenses.filter { it.paidBy == member.id }.sumOf { it.amount }
        sb.appendLine("${member.name}: paid ${ZoneFormat.money(paid, currency)} · net ${if (net >= 0) "+" else ""}${ZoneFormat.money(net, currency)}")
    }
    sb.appendLine()
    val plan = ZoneFormat.settlePlan(group.members, nets)
    if (plan.isEmpty()) {
        sb.appendLine("Everyone is square.")
    } else {
        sb.appendLine("To settle:")
        plan.forEach { (from, to, amount) ->
            val fromName = group.members.firstOrNull { it.id == from }?.name ?: from
            val toName = group.members.firstOrNull { it.id == to }?.name ?: to
            sb.appendLine("$fromName → $toName: ${ZoneFormat.money(amount, currency)}")
        }
    }
    ZoneFormat.shareText(context, "Zone — ${group.name}", sb.toString())
}
