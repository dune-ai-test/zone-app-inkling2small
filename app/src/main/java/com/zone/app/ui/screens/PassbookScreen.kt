package com.zone.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
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
import com.zone.app.data.Account
import com.zone.app.data.PassbookData
import com.zone.app.data.PassbookEntry
import com.zone.app.data.ZoneStore
import com.zone.app.ui.components.ConfirmDialog
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
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.ui.text.input.KeyboardType
import java.time.Instant
import java.time.ZoneOffset

private val ACCOUNT_TYPES = listOf("Bank", "Savings", "Wallet", "Card", "Cash", "Other")
private val DEPOSIT_CATEGORIES = listOf("Salary", "Freelance", "Investments", "Refund", "Transfer", "Other")
private val WITHDRAW_CATEGORIES = listOf("Rent", "Groceries", "Food", "Transport", "Utilities", "Shopping", "Health", "Entertainment", "Travel", "Transfer", "Other")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PassbookScreen() {
    val palette = zonePalette()
    val context = LocalContext.current

    var data by remember { mutableStateOf(ZoneStore.readTyped(ZoneStore.KEY_PASSBOOK, PassbookData())) }
    var selectedAccount by remember { mutableStateOf<String?>(null) } // null = combined
    var query by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf("all") } // all | deposit | withdraw
    var monthFilter by remember { mutableStateOf("all") }
    var showAccountDialog by remember { mutableStateOf(false) }
    var showEntryDialog by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<Account?>(null) }
    var editingEntry by remember { mutableStateOf<PassbookEntry?>(null) }
    var deletingEntry by remember { mutableStateOf<PassbookEntry?>(null) }


    val settings = remember { ZoneStore.settings() }

    fun persist(updated: PassbookData, action: String, title: String) {
        data = updated
        ZoneStore.writeTyped(ZoneStore.KEY_PASSBOOK, updated)
        ZoneStore.logAudit("passbook", action, title)
    }

    fun accountBalance(account: Account): Double =
        account.opening + data.transactions
            .filter { it.accountId == account.id }
            .sumOf { if (it.type == "deposit") it.amount else -it.amount }

    val combinedBalance = data.accounts.sumOf { it.opening } +
        data.transactions.sumOf { if (it.type == "deposit") it.amount else -it.amount }
    val thisMonth = ZoneFormat.currentMonth()
    val monthIn = data.transactions.filter { it.date.startsWith(thisMonth) && it.type == "deposit" }.sumOf { it.amount }
    val monthOut = data.transactions.filter { it.date.startsWith(thisMonth) && it.type == "withdraw" }.sumOf { it.amount }

    val months = data.transactions.map { it.date.take(7) }.distinct().sortedDescending()

    val scopedEntries = data.transactions.filter { entry ->
        (selectedAccount == null || entry.accountId == selectedAccount) &&
            (monthFilter == "all" || entry.date.startsWith(monthFilter)) &&
            (typeFilter == "all" || entry.type == typeFilter) &&
            (query.isBlank() ||
                entry.description.lowercase().contains(query.trim().lowercase()) ||
                entry.category.lowercase().contains(query.trim().lowercase()))
    }

    // Running balance per account over all-time (date, id) order — filters only visibility.
    val runningById: Map<String, Double> = remember(data, selectedAccount) {
        val result = mutableMapOf<String, Double>()
        if (selectedAccount == null) {
            // Combined running balance: cumulative net across all accounts.
            var running = data.accounts.sumOf { it.opening }
            data.transactions.sortedWith(compareBy({ it.date }, { it.id })).forEach { entry ->
                running += if (entry.type == "deposit") entry.amount else -entry.amount
                result[entry.id] = running
            }
        } else {
            val account = data.accounts.firstOrNull { it.id == selectedAccount }
            var running = account?.opening ?: 0.0
            data.transactions
                .filter { it.accountId == selectedAccount }
                .sortedWith(compareBy({ it.date }, { it.id }))
                .forEach { entry ->
                    running += if (entry.type == "deposit") entry.amount else -entry.amount
                    result[entry.id] = running
                }
        }
        result
    }

    ZoneScreen(
        title = "Passbook",
        subtitle = "Every account, one statement.",
        actions = {
            IconButton(onClick = { exportPassbookCsv(context, data, settings.currency) }) {
                Icon(Icons.Rounded.Share, contentDescription = "Export CSV", tint = palette.accent)
            }
            Spacer(Modifier.size(4.dp))
            IconButton(
                onClick = { showEntryDialog = true },
                modifier = Modifier.size(36.dp).clip(CircleShape).background(palette.accent),
                enabled = data.accounts.isNotEmpty()
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add entry", tint = Color.White)
            }
        }
    ) {
        item {
            // Combined hero
            ZoneCard(modifier = Modifier.padding(horizontal = 20.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Text("ACROSS EVERY ACCOUNT", style = MaterialTheme.typography.labelSmall, color = palette.secondary)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            ZoneFormat.money(combinedBalance, settings.currency),
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.label
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("In  +${ZoneFormat.money(monthIn, settings.currency)}", style = MaterialTheme.typography.bodySmall, color = palette.green)
                        Text("Out −${ZoneFormat.money(monthOut, settings.currency)}", style = MaterialTheme.typography.bodySmall, color = palette.red)
                    }
                }
            }
        }
        item {
            SectionLabel("Accounts")
            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AccountCard(
                    name = "Combined",
                    subtitle = "${data.accounts.size} account${if (data.accounts.size == 1) "" else "s"}",
                    balance = combinedBalance,
                    currency = settings.currency,
                    selected = selectedAccount == null,
                    onClick = { selectedAccount = null }
                )
                data.accounts.forEach { account ->
                    AccountCard(
                        name = account.name,
                        subtitle = account.type + if (account.number.isNotBlank()) " · ${account.number}" else "",
                        balance = accountBalance(account),
                        currency = settings.currency,
                        selected = selectedAccount == account.id,
                        onClick = { selectedAccount = account.id }
                    )
                }
                AddAccountCard(onClick = { showAccountDialog = true })
            }
        }
        item {
            Row(
                Modifier.padding(horizontal = 20.dp).padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionLabelInlinePassbook(
                    if (selectedAccount == null) "Statement — all accounts"
                    else "Statement — ${data.accounts.firstOrNull { it.id == selectedAccount }?.name.orEmpty()}"
                )
                Spacer(Modifier.weight(1f))
                if (selectedAccount != null) {
                    TextButton(onClick = { editingAccount = data.accounts.firstOrNull { it.id == selectedAccount } }) {
                        Text("Edit account", color = palette.accent)
                    }
                }
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
                modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(selected = typeFilter == "all", onClick = { typeFilter = "all" }, label = { Text("All") }, shape = RoundedCornerShape(20.dp))
                FilterChip(selected = typeFilter == "deposit", onClick = { typeFilter = "deposit" }, label = { Text("In") }, shape = RoundedCornerShape(20.dp))
                FilterChip(selected = typeFilter == "withdraw", onClick = { typeFilter = "withdraw" }, label = { Text("Out") }, shape = RoundedCornerShape(20.dp))
            }
            Spacer(Modifier.height(6.dp))
            FlowRow(
                modifier = Modifier.padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(selected = monthFilter == "all", onClick = { monthFilter = "all" }, label = { Text("All time") }, shape = RoundedCornerShape(20.dp))
                months.take(6).forEach { month ->
                    FilterChip(
                        selected = monthFilter == month,
                        onClick = { monthFilter = if (monthFilter == month) "all" else month },
                        label = { Text(ZoneFormat.monthLabel(month).take(3) + " ’" + month.substring(2, 4)) },
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }
        }
        if (scopedEntries.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Rounded.AccountBalance,
                    title = if (data.accounts.isEmpty()) "No accounts yet" else "Nothing to show",
                    subtitle = if (data.accounts.isEmpty())
                        "Create your first account, then log deposits and withdrawals."
                    else
                        "Add an entry with the + button, or loosen the filters."
                )
            }
        } else {
            items(scopedEntries.sortedWith(compareByDescending<PassbookEntry> { it.date }.thenByDescending { it.id }), key = { it.id }) { entry ->
                EntryRow(
                    entry = entry,
                    accountName = data.accounts.firstOrNull { it.id == entry.accountId }?.name ?: "—",
                    running = runningById[entry.id],
                    currency = settings.currency,
                    onEdit = { editingEntry = entry; showEntryDialog = true },
                    onDelete = { deletingEntry = entry }
                )
            }
        }
        item { Spacer(Modifier.height(28.dp)) }
    }

    // --- dialogs ---

    if (showAccountDialog || editingAccount != null) {
        AccountDialog(
            initial = editingAccount,
            onDismiss = { showAccountDialog = false; editingAccount = null },
            onSave = { account ->
                val list = data.accounts.toMutableList()
                val index = list.indexOfFirst { it.id == account.id }
                if (index >= 0) list[index] = account else list.add(account)
                persist(data.copy(accounts = list), if (index >= 0) "update" else "add", account.name)
                showAccountDialog = false
                editingAccount = null
            },
            onDelete = editingAccount?.let { target ->
                {
                    persist(
                        PassbookData(
                            accounts = data.accounts.filterNot { it.id == target.id },
                            transactions = data.transactions.filterNot { it.accountId == target.id }
                        ),
                        "delete",
                        "Account & entries: ${target.name}"
                    )
                    if (selectedAccount == target.id) selectedAccount = null
                    showAccountDialog = false
                    editingAccount = null
                }
            }
        )
    }

    if (showEntryDialog) {
        EntryDialog(
            initial = editingEntry,
            accounts = data.accounts,
            defaultAccount = selectedAccount ?: data.accounts.firstOrNull()?.id.orEmpty(),
            onDismiss = { showEntryDialog = false; editingEntry = null },
            onSave = { entry ->
                val list = data.transactions.toMutableList()
                val index = list.indexOfFirst { it.id == entry.id }
                if (index >= 0) list[index] = entry else list.add(entry)
                persist(data.copy(transactions = list), if (index >= 0) "update" else "add", entry.description.ifBlank { entry.category })
                showEntryDialog = false
                editingEntry = null
            }
        )
    }

    deletingEntry?.let { target ->
        ConfirmDialog(
            title = "Delete entry?",
            message = "${target.description.ifBlank { target.category }} · ${ZoneFormat.money(target.amount, settings.currency)}",
            confirmLabel = "Delete",
            onConfirm = { persist(data.copy(transactions = data.transactions.filterNot { it.id == target.id }), "delete", target.category) },
            onDismiss = { deletingEntry = null }
        )
    }

}

@Composable
private fun SectionLabelInlinePassbook(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = zonePalette().secondary
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountCard(
    name: String,
    subtitle: String,
    balance: Double,
    currency: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val palette = zonePalette()
    Column(
        Modifier
            .width(150.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) palette.accent.copy(alpha = 0.12f) else palette.surface)
            .border(if (selected) 1.5.dp else 0.5.dp, if (selected) palette.accent else palette.separator, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Text(name, style = MaterialTheme.typography.titleMedium, color = palette.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(subtitle, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = palette.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(10.dp))
        Text(
            ZoneFormat.money(balance, currency),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = palette.label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AddAccountCard(onClick: () -> Unit) {
    val palette = zonePalette()
    Box(
        Modifier
            .width(110.dp)
            .height(128.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(palette.grouped)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.Add, contentDescription = "Add account", tint = palette.accent, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(6.dp))
            Text("New account", style = MaterialTheme.typography.bodySmall, color = palette.accent)
        }
    }
}

@Composable
private fun EntryRow(
    entry: PassbookEntry,
    accountName: String,
    running: Double?,
    currency: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val palette = zonePalette()
    val isDeposit = entry.type == "deposit"
    ZoneCard(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        entry.description.ifBlank { entry.category },
                        style = MaterialTheme.typography.bodyLarge,
                        color = palette.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                Text(
                    "${ZoneFormat.dateShort(entry.date)} · $accountName · ${entry.category}",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.secondary
                )
                if (running != null) {
                    Text(
                        "Balance ${ZoneFormat.money(running, currency)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.secondary
                    )
                }
            }
            Spacer(Modifier.size(8.dp))
            Text(
                (if (isDeposit) "+" else "−") + ZoneFormat.money(entry.amount, currency),
                style = MaterialTheme.typography.titleMedium,
                color = if (isDeposit) palette.green else palette.label
            )
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Rounded.ChevronRight, contentDescription = "Edit entry", tint = palette.secondary)
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = palette.red, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccountDialog(
    initial: Account?,
    onDismiss: () -> Unit,
    onSave: (Account) -> Unit,
    onDelete: (() -> Unit)?
) {
    val palette = zonePalette()
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var type by remember { mutableStateOf(initial?.type ?: "Bank") }
    var number by remember { mutableStateOf(initial?.number ?: "") }
    var opening by remember { mutableStateOf(initial?.opening?.toString() ?: "0") }

    FormDialog(
        title = if (initial == null) "New account" else "Edit account",
        confirmLabel = if (initial == null) "Create" else "Save",
        confirmEnabled = name.trim().isNotEmpty() && opening.toDoubleOrNull() != null,
        onConfirm = {
            onSave(
                Account(
                    id = initial?.id ?: ZoneFormat.uid("acc"),
                    name = name.trim().take(60),
                    type = type,
                    number = number.trim().take(40),
                    opening = opening.toDoubleOrNull() ?: 0.0
                )
            )
        },
        onDismiss = onDismiss
    ) {
        ZoneTextField(value = name, onValueChange = { name = it }, label = "Account name")
        Column {
            Text("Type", style = MaterialTheme.typography.labelSmall, color = palette.secondary)
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ACCOUNT_TYPES.forEach { t ->
                    FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t) }, shape = RoundedCornerShape(20.dp))
                }
            }
        }
        ZoneTextField(value = number, onValueChange = { number = it }, label = "Number / note (optional)")
        ZoneTextField(value = opening, onValueChange = { opening = it }, label = "Opening balance", keyboardType = KeyboardType.Decimal)
        if (onDelete != null) {
            TextButton(onClick = onDelete) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = palette.red)
                Spacer(Modifier.size(4.dp))
                Text("Remove account and its entries", color = palette.red)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun EntryDialog(
    initial: PassbookEntry?,
    accounts: List<Account>,
    defaultAccount: String,
    onDismiss: () -> Unit,
    onSave: (PassbookEntry) -> Unit
) {
    val palette = zonePalette()
    var type by remember { mutableStateOf(initial?.type ?: "deposit") }
    var amount by remember { mutableStateOf(initial?.amount?.toString()?.removeSuffix(".0") ?: "") }
    var accountId by remember { mutableStateOf(initial?.accountId ?: defaultAccount) }
    var category by remember { mutableStateOf(initial?.category ?: if (type == "deposit") "Salary" else "Groceries") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var date by remember { mutableStateOf(initial?.date ?: ZoneFormat.todayISO()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val categories = if (type == "deposit") DEPOSIT_CATEGORIES else WITHDRAW_CATEGORIES

    FormDialog(
        title = if (initial == null) "Add entry" else "Edit entry",
        confirmLabel = if (initial == null) "Add" else "Save",
        confirmEnabled = (amount.toDoubleOrNull() ?: 0.0) > 0.0 && accountId.isNotBlank(),
        onConfirm = {
            onSave(
                PassbookEntry(
                    id = initial?.id ?: ZoneFormat.uid("pb"),
                    accountId = accountId,
                    type = type,
                    amount = amount.toDoubleOrNull() ?: 0.0,
                    category = category,
                    description = description.trim().take(140),
                    date = date
                )
            )
        },
        onDismiss = onDismiss
    ) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf("deposit" to "Deposit", "withdraw" to "Withdraw").forEachIndexed { index, (id, label) ->
                SegmentedButton(
                    selected = type == id,
                    onClick = {
                        type = id
                        category = if (id == "deposit") "Salary" else "Groceries"
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = 2)
                ) { Text(label) }
            }
        }
        ZoneTextField(value = amount, onValueChange = { amount = it }, label = "Amount", keyboardType = KeyboardType.Decimal)
        Column {
            Text("Account", style = MaterialTheme.typography.labelSmall, color = palette.secondary)
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                accounts.forEach { account ->
                    FilterChip(
                        selected = accountId == account.id,
                        onClick = { accountId = account.id },
                        label = { Text(account.name) },
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }
        }
        Column {
            Text("Category", style = MaterialTheme.typography.labelSmall, color = palette.secondary)
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                categories.forEach { c ->
                    FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c) }, shape = RoundedCornerShape(20.dp))
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
        ZoneTextField(value = description, onValueChange = { description = it }, label = "Description (optional)")
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

private fun exportPassbookCsv(context: android.content.Context, data: PassbookData, currency: String) {
    val header = "Date,Account,Description,Category,Type,Amount,Currency"
    val rows = data.transactions.sortedBy { it.date }.map { entry ->
        val account = data.accounts.firstOrNull { it.id == entry.accountId }?.name ?: "—"
        listOf(entry.date, account, entry.description, entry.category, entry.type, entry.amount.toString(), currency)
            .joinToString(",") { ZoneFormat.csvEscape(it) }
    }
    ZoneFormat.shareText(context, "Zone — passbook statement", (listOf(header) + rows).joinToString("\n"), "text/csv")
}
