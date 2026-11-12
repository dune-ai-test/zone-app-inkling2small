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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import com.zone.app.data.MoneyData
import com.zone.app.data.Txn
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
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.ui.text.input.KeyboardType
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val EXPENSE_CATEGORIES = listOf("Rent", "Groceries", "Food", "Transport", "Utilities", "Shopping", "Health", "Entertainment", "Travel", "Education", "Gifts", "Other")
private val INCOME_CATEGORIES = listOf("Salary", "Freelance", "Investments", "Refund", "Other")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MoneyScreen() {
    val palette = zonePalette()
    val context = LocalContext.current

    var money by remember { mutableStateOf(ZoneStore.readTyped(ZoneStore.KEY_MONEY, MoneyData())) }
    var selectedMonth by remember { mutableStateOf(ZoneFormat.currentMonth()) }
    var typeFilter by remember { mutableStateOf("all") } // all | expense | income
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<Txn?>(null) }
    var showComposer by remember { mutableStateOf(false) }
    var showBudget by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Txn?>(null) }

    val settings = remember { ZoneStore.settings() }

    fun persist(updated: MoneyData, action: String, title: String) {
        money = updated
        ZoneStore.writeTyped(ZoneStore.KEY_MONEY, updated)
        ZoneStore.logAudit("money", action, title)
    }

    val monthTxns = money.transactions.filter { it.date.startsWith(selectedMonth) }
    val income = monthTxns.filter { it.type == "income" }.sumOf { it.amount }
    val expense = monthTxns.filter { it.type == "expense" }.sumOf { it.amount }
    val net = income - expense
    val budgetPct = if (money.budget > 0) ((expense / money.budget) * 100).toInt() else 0
    val budgetLeft = money.budget - expense
    val keptPct = if (income > 0) ((net / income) * 100).toInt() else 0
    val isCurrentMonth = selectedMonth == ZoneFormat.currentMonth()
    val elapsedDays = if (isCurrentMonth) LocalDate.now().dayOfMonth else YearMonth.parse(selectedMonth).lengthOfMonth()
    val perDay = if (elapsedDays > 0) expense / elapsedDays else 0.0

    val visible = monthTxns
        .filter { txn ->
            val typeOk = typeFilter == "all" || txn.type == typeFilter
            val q = query.trim().lowercase()
            val queryOk = q.isBlank() ||
                txn.note.lowercase().contains(q) ||
                txn.category.lowercase().contains(q) ||
                txn.date.contains(q) ||
                txn.amount.toString().contains(q)
            typeOk && queryOk
        }
        .sortedByDescending { it.date }

    val topCategories = monthTxns
        .filter { it.type == "expense" }
        .groupBy { it.category }
        .mapValues { (_, list) -> list.sumOf { it.amount } }
        .entries
        .sortedByDescending { it.value }
        .take(4)
    val maxCategory = topCategories.maxOfOrNull { it.value } ?: 1.0

    val monthSeries = (5 downTo 0).map { back ->
        val key = ZoneFormat.addMonths(selectedMonth, -back)
        val txns = money.transactions.filter { it.date.startsWith(key) }
        Triple(
            key,
            txns.filter { it.type == "income" }.sumOf { it.amount },
            txns.filter { it.type == "expense" }.sumOf { it.amount }
        )
    }
    val seriesMax = monthSeries.maxOf { maxOf(it.second, it.third) }.takeIf { it > 0 } ?: 1.0

    ZoneScreen(
        title = "Money",
        subtitle = "Where the month goes.",
        actions = {
            IconButton(onClick = { exportMoneyCsv(context, money, settings.currency) }) {
                Icon(Icons.Rounded.Share, contentDescription = "Export CSV", tint = palette.accent)
            }
            Spacer(Modifier.size(4.dp))
            ZoneTextFieldButton(label = "Budget", onClick = { showBudget = true })
        }
    ) {
        item {
            // Month navigation
            Row(
                Modifier
                    .padding(horizontal = 20.dp)
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { selectedMonth = ZoneFormat.addMonths(selectedMonth, -1) }) {
                    Icon(Icons.Rounded.ChevronLeft, contentDescription = "Previous month", tint = palette.label)
                }
                Text(
                    ZoneFormat.monthLabel(selectedMonth),
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.label
                )
                if (isCurrentMonth) {
                    IconButton(onClick = {}) {
                        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = palette.separator)
                    }
                } else {
                    IconButton(onClick = { selectedMonth = ZoneFormat.addMonths(selectedMonth, 1) }) {
                        Icon(Icons.Rounded.ChevronRight, contentDescription = "Next month", tint = palette.label)
                    }
                }
            }
        }
        item {
            // Month hero card
            ZoneCard(modifier = Modifier.padding(horizontal = 20.dp).padding(top = 6.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "KEPT THIS MONTH",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.secondary
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            ZoneFormat.money(net, settings.currency),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (net >= 0) palette.green else palette.red
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "In  ${ZoneFormat.money(income, settings.currency)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.secondary
                        )
                        Text(
                            "Out ${ZoneFormat.money(expense, settings.currency)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.secondary
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Budget used",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondary
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "$budgetPct% of ${ZoneFormat.money(money.budget, settings.currency)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (budgetPct > 90) palette.red else palette.secondary
                    )
                }
                Spacer(Modifier.height(6.dp))
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { (budgetPct / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
                    color = if (budgetPct > 90) palette.red else palette.accent,
                    trackColor = palette.grouped
                )
            }
        }
        item {
            Row(
                Modifier
                    .padding(horizontal = 20.dp)
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatTile(value = ZoneFormat.money(expense, settings.currency), label = "Spent · ${ZoneFormat.money(perDay, settings.currency)}/day", modifier = Modifier.weight(1f))
                StatTile(value = ZoneFormat.money(income, settings.currency), label = "Income", modifier = Modifier.weight(1f))
            }
            Row(
                Modifier
                    .padding(horizontal = 20.dp)
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatTile(value = "$keptPct%", label = "Of income kept", modifier = Modifier.weight(1f))
                StatTile(
                    value = ZoneFormat.money(budgetLeft, settings.currency),
                    label = if (budgetLeft >= 0) "Budget remaining" else "Over budget",
                    modifier = Modifier.weight(1f),
                    tint = if (budgetLeft >= 0) palette.label else palette.red
                )
            }
        }
        item {
            // Six-month pulse chart
            ZoneCard(modifier = Modifier.padding(horizontal = 20.dp).padding(top = 10.dp)) {
                Text("SIX-MONTH PULSE", style = MaterialTheme.typography.labelSmall, color = palette.secondary)
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    monthSeries.forEach { (key, inc, exp) ->
                        val isSelected = key == selectedMonth
                        Column(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) palette.grouped else Color.Transparent)
                                .clickable { selectedMonth = key }
                                .padding(vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
                                Box(
                                    Modifier
                                        .width(9.dp)
                                        .height((44 * (inc / seriesMax)).dp.coerceAtLeast(2.dp))
                                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                        .background(palette.green)
                                )
                                Box(
                                    Modifier
                                        .width(9.dp)
                                        .height((44 * (exp / seriesMax)).dp.coerceAtLeast(2.dp))
                                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                        .background(palette.orange)
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                YearMonth.parse(key).month.getDisplayName(java.time.format.TextStyle.NARROW, java.util.Locale.getDefault()),
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = if (isSelected) palette.accent else palette.secondary
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row {
                    Dot(palette.green); Text(" Income", style = MaterialTheme.typography.bodySmall, color = palette.secondary)
                    Spacer(Modifier.size(14.dp))
                    Dot(palette.orange); Text(" Expenses", style = MaterialTheme.typography.bodySmall, color = palette.secondary)
                }
            }
        }
        if (topCategories.isNotEmpty()) {
            item {
                ZoneCard(modifier = Modifier.padding(horizontal = 20.dp).padding(top = 10.dp)) {
                    Text("TOP CATEGORIES", style = MaterialTheme.typography.labelSmall, color = palette.secondary)
                    Spacer(Modifier.height(10.dp))
                    topCategories.forEach { (category, amount) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(category, style = MaterialTheme.typography.bodyMedium, color = palette.label, modifier = Modifier.width(110.dp), maxLines = 1)
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(palette.grouped)
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxWidth((amount / maxCategory).toFloat().coerceIn(0.05f, 1f))
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(palette.accent.copy(alpha = 0.75f))
                                )
                            }
                            Spacer(Modifier.size(8.dp))
                            Text(ZoneFormat.money(amount, settings.currency), style = MaterialTheme.typography.bodySmall, color = palette.secondary)
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
        }
        item {
            Row(
                Modifier.padding(horizontal = 20.dp).padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionLabelInline("Ledger — ${ZoneFormat.monthLabel(selectedMonth)}")
                Spacer(Modifier.weight(1f))
                WindowButton("+ Add") { showComposer = true; editing = null }
            }
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search transactions", color = palette.secondary) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = palette.secondary) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = zoneFieldColors(),
                modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("all" to "All", "expense" to "Expenses", "income" to "Income").forEach { (id, label) ->
                    FilterChip(
                        selected = typeFilter == id,
                        onClick = { typeFilter = id },
                        label = { Text(label) },
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }
        }
        if (visible.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Rounded.Edit,
                    title = "A quiet ledger",
                    subtitle = if (monthTxns.isEmpty()) "No entries in ${ZoneFormat.monthLabel(selectedMonth)} yet." else "Nothing matches the search."
                )
            }
        } else {
            items(visible, key = { it.id }) { txn ->
                TxnRow(
                    txn = txn,
                    currency = settings.currency,
                    onEdit = { editing = txn; showComposer = true },
                    onDelete = { deleting = txn }
                )
            }
        }
        item { Spacer(Modifier.height(28.dp)) }
    }

    if (showComposer) {
        TxnDialog(
            initial = editing,
            onDismiss = { showComposer = false; editing = null },
            onSave = { txn ->
                val list = money.transactions.toMutableList()
                val index = list.indexOfFirst { it.id == txn.id }
                if (index >= 0) list[index] = txn else list.add(txn)
                persist(money.copy(transactions = list), if (index >= 0) "update" else "add", txn.note.ifBlank { txn.category })
                showComposer = false
                editing = null
            }
        )
    }

    if (showBudget) {
        var text by remember { mutableStateOf(money.budget.toLong().toString()) }
        FormDialog(
            title = "Monthly envelope",
            confirmLabel = "Save",
            confirmEnabled = text.toDoubleOrNull() != null,
            onConfirm = {
                text.toDoubleOrNull()?.let { value ->
                    persist(money.copy(budget = value), "update", "Budget set to $value")
                }
                showBudget = false
            },
            onDismiss = { showBudget = false }
        ) {
            ZoneTextField(value = text, onValueChange = { text = it }, label = "Budget per month", keyboardType = KeyboardType.Number)
            Text(
                "Displayed in ${settings.currency}. Zone never converts between currencies — the number stays exactly what you type.",
                style = MaterialTheme.typography.bodySmall,
                color = palette.secondary
            )
        }
    }

    deleting?.let { target ->
        ConfirmDialog(
            title = "Delete entry?",
            message = "${target.category} · ${ZoneFormat.money(target.amount, settings.currency)} · ${ZoneFormat.dateMedium(target.date)}",
            confirmLabel = "Delete",
            onConfirm = {
                persist(money.copy(transactions = money.transactions.filterNot { it.id == target.id }), "delete", target.category)
            },
            onDismiss = { deleting = null }
        )
    }
}

@Composable
private fun WindowButton(label: String, onClick: () -> Unit) {
    val palette = zonePalette()
    TextButton(onClick = onClick) {
        Text(label, color = palette.accent, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ZoneTextFieldButton(label: String, onClick: () -> Unit) {
    val palette = zonePalette()
    androidx.compose.material3.OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, palette.separator)
    ) {
        Text(label, color = palette.label)
    }
}

@Composable
private fun SectionLabelInline(text: String) {
    val palette = zonePalette()
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = palette.secondary
    )
}

@Composable
private fun Dot(color: Color) {
    Box(Modifier.size(8.dp).clip(CircleShape).background(color))
}

@Composable
private fun TxnRow(txn: Txn, currency: String, onEdit: () -> Unit, onDelete: () -> Unit) {
    val palette = zonePalette()
    val isExpense = txn.type == "expense"
    ZoneCard(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (isExpense) palette.orange.copy(alpha = 0.16f) else palette.green.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    txn.category.take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    color = if (isExpense) palette.orange else palette.green
                )
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    txn.note.ifBlank { txn.category },
                    style = MaterialTheme.typography.bodyLarge,
                    color = palette.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${txn.category} · ${ZoneFormat.dateShort(txn.date)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.secondary
                )
            }
            Text(
                (if (isExpense) "−" else "+") + ZoneFormat.money(txn.amount, currency),
                style = MaterialTheme.typography.titleMedium,
                color = if (isExpense) palette.label else palette.green
            )
            IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Rounded.Edit, contentDescription = "Edit", tint = palette.secondary, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = palette.red, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TxnDialog(initial: Txn?, onDismiss: () -> Unit, onSave: (Txn) -> Unit) {
    val palette = zonePalette()
    var type by remember { mutableStateOf(initial?.type ?: "expense") }
    var amount by remember { mutableStateOf(initial?.amount?.toString()?.removeSuffix(".0") ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: "Groceries") }
    var date by remember { mutableStateOf(initial?.date ?: ZoneFormat.todayISO()) }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }

    val categories = if (type == "expense") EXPENSE_CATEGORIES else INCOME_CATEGORIES

    FormDialog(
        title = if (initial == null) "Add entry" else "Edit entry",
        confirmLabel = if (initial == null) "Add" else "Save",
        confirmEnabled = (amount.toDoubleOrNull() ?: 0.0) > 0.0,
        onConfirm = {
            onSave(
                Txn(
                    id = initial?.id ?: ZoneFormat.uid("txn"),
                    type = type,
                    amount = amount.toDoubleOrNull() ?: 0.0,
                    category = category,
                    date = date,
                    note = note.trim().take(140)
                )
            )
        },
        onDismiss = onDismiss
    ) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf("expense" to "Expense", "income" to "Income").forEachIndexed { index, (id, label) ->
                SegmentedButton(
                    selected = type == id,
                    onClick = {
                        type = id
                        category = if (id == "expense") "Groceries" else "Salary"
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = 2)
                ) {
                    Text(label)
                }
            }
        }

        ZoneTextField(value = amount, onValueChange = { amount = it }, label = "Amount", keyboardType = KeyboardType.Decimal)

        Column {
            Text("Category", style = MaterialTheme.typography.labelSmall, color = palette.secondary)
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                categories.forEach { c ->
                    FilterChip(
                        selected = category == c,
                        onClick = { category = c },
                        label = { Text(c) },
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

        ZoneTextField(value = note, onValueChange = { note = it }, label = "Note (optional)")
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

private fun exportMoneyCsv(context: android.content.Context, money: MoneyData, currency: String) {
    val header = "Date,Type,Category,Amount,Currency,Note"
    val rows = money.transactions.sortedBy { it.date }.map { txn ->
        listOf(txn.date, txn.type, txn.category, txn.amount.toString(), currency, txn.note)
            .joinToString(",") { ZoneFormat.csvEscape(it) }
    }
    ZoneFormat.shareText(context, "Zone — money ledger", (listOf(header) + rows).joinToString("\n"), "text/csv")
}
