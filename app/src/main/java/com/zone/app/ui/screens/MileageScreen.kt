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
import com.zone.app.data.Fill
import com.zone.app.data.Vehicle
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
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.ui.text.input.KeyboardType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private val VEHICLE_TYPES = listOf("bike", "scooter", "car", "suv", "truck")
private val FUEL_TYPES = listOf("petrol", "diesel")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MileageScreen() {
    val palette = zonePalette()
    val context = LocalContext.current

    var fills by remember {
        mutableStateOf(ZoneStore.readTyped<List<Fill>>(ZoneStore.KEY_TRIPS, emptyList()))
    }
    var vehicles by remember {
        mutableStateOf(ZoneStore.readTyped<List<Vehicle>>(ZoneStore.KEY_VEHICLES, emptyList()))
    }
    var selectedVehicle by remember { mutableStateOf<String?>(null) } // null = fleet
    var query by remember { mutableStateOf("") }
    var showFillDialog by remember { mutableStateOf(false) }
    var editingFill by remember { mutableStateOf<Fill?>(null) }
    var showVehicleDialog by remember { mutableStateOf(false) }
    var editingVehicle by remember { mutableStateOf<Vehicle?>(null) }
    var deletingFill by remember { mutableStateOf<Fill?>(null) }
    var deletingVehicle by remember { mutableStateOf<Vehicle?>(null) }

    val settings = remember { ZoneStore.settings() }
    val currency = settings.currency

    fun persist(newFills: List<Fill>, action: String, title: String, newVehicles: List<Vehicle> = vehicles) {
        val relinked = relinkChains(newFills)
        fills = relinked
        vehicles = newVehicles
        ZoneStore.writeTyped(ZoneStore.KEY_TRIPS, relinked)
        ZoneStore.writeTyped(ZoneStore.KEY_VEHICLES, newVehicles)
        ZoneStore.logAudit("mileage", action, title)
    }

    fun upsertFill(fill: Fill) {
        val exists = fills.any { it.id == fill.id }
        // Auto-create the vehicle if it's new.
        val name = fill.vehicle.trim()
        val known = vehicles.any { it.name.equals(name, ignoreCase = true) }
        val nextVehicles = if (!known && name.isNotEmpty()) {
            vehicles + Vehicle(
                id = ZoneFormat.uid("vehicle"),
                name = name.take(40),
                type = inferVehicleType(name),
                fuel = "petrol",
                createdAt = ZoneFormat.todayISO()
            )
        } else vehicles
        val list = if (exists) fills.map { if (it.id == fill.id) fill else it } else fills + fill
        persist(list, if (exists) "update" else "add", "${name} · ${ZoneFormat.dateShort(fill.date)}", nextVehicles)
    }

    val visibleFills = fills
        .filter { fill ->
            (selectedVehicle == null || fill.vehicle.equals(selectedVehicle, ignoreCase = true)) &&
                (query.isBlank() ||
                    fill.vehicle.lowercase().contains(query.trim().lowercase()) ||
                    fill.note.lowercase().contains(query.trim().lowercase()) ||
                    fill.date.contains(query.trim()))
        }
        .sortedWith(compareByDescending<Fill> { it.date }.thenByDescending { it.end })

    val validFills = fills.filter { it.end > it.start && it.price > 0 && it.paid > 0 }
    val fleetDistance = validFills.sumOf { it.end - it.start }
    val fleetLitres = validFills.sumOf { it.paid / it.price }
    val fleetAvg = if (fleetLitres > 0) fleetDistance / fleetLitres else 0.0
    val fleetCostPerKm = if (fleetDistance > 0) validFills.sumOf { it.paid } / fleetDistance else 0.0
    val fleetPaid = validFills.sumOf { it.paid }
    val bestMileage = validFills.map { (it.end - it.start) / (it.paid / it.price) }.maxOrNull() ?: 0.0

    val perVehicle = (vehicles.map { it.name } + fills.map { it.vehicle })
        .distinctBy { it.lowercase() }
        .map { name ->
            val vf = validFills.filter { it.vehicle.equals(name, ignoreCase = true) }
            val distance = vf.sumOf { it.end - it.start }
            val litres = vf.sumOf { it.paid / it.price }
            Triple(name, if (litres > 0) distance / litres else 0.0, distance)
        }
        .sortedByDescending { it.third }

    ZoneScreen(
        title = "Mileage",
        subtitle = "Distance, fuel, cost per kilometre.",
        actions = {
            IconButton(onClick = { exportMileageCsv(context, fills) }) {
                Icon(Icons.Rounded.Share, contentDescription = "Export CSV", tint = palette.accent)
            }
            Spacer(Modifier.size(4.dp))
            IconButton(
                onClick = { showVehicleDialog = true; editingVehicle = null },
                modifier = Modifier.size(36.dp).clip(CircleShape).background(palette.grouped)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add vehicle", tint = palette.accent)
            }
        }
    ) {
        item {
            // Fleet pulse hero
            ZoneCard(modifier = Modifier.padding(horizontal = 20.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (selectedVehicle == null) "FLEET AVERAGE" else selectedVehicle!!.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.secondary
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            if (fleetAvg > 0) "${ZoneFormat.num(fleetAvg, 1)} km/l" else "— km/l",
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.label
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${ZoneFormat.num(fleetDistance, 0)} km logged", style = MaterialTheme.typography.bodySmall, color = palette.secondary)
                        Text("${ZoneFormat.num(fleetLitres, 0)} L of fuel", style = MaterialTheme.typography.bodySmall, color = palette.secondary)
                        Text(ZoneFormat.money(fleetPaid, currency) + " spent", style = MaterialTheme.typography.bodySmall, color = palette.secondary)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Best fill", style = MaterialTheme.typography.bodySmall, color = palette.secondary)
                    Spacer(Modifier.weight(1f))
                    Text(
                        if (bestMileage > 0) "${ZoneFormat.num(bestMileage, 1)} km/l" else "—",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.green
                    )
                }
            }
        }
        item {
            Row(
                Modifier.padding(horizontal = 20.dp).padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatTile(
                    value = if (fleetCostPerKm > 0) ZoneFormat.money(fleetCostPerKm, currency) + "/km" else "—",
                    label = "Cost per kilometre",
                    modifier = Modifier.weight(1f)
                )
                StatTile(value = "${validFills.size}", label = "Complete tanks", modifier = Modifier.weight(1f))
                StatTile(value = "${fills.size}", label = "Fill-ups logged", modifier = Modifier.weight(1f))
            }
        }
        item {
            SectionLabel("Garage")
            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedVehicle == null,
                    onClick = { selectedVehicle = null },
                    label = { Text("All vehicles") },
                    shape = RoundedCornerShape(20.dp)
                )
                (vehicles.map { it.name } + fills.map { it.vehicle })
                    .distinctBy { it.lowercase() }
                    .forEach { name ->
                        val selected = selectedVehicle.equals(name, ignoreCase = true)
                        FilterChip(
                            selected = selected,
                            onClick = { selectedVehicle = if (selected) null else name },
                            label = { Text(name) },
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
            }
            if (perVehicle.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Column(Modifier.padding(horizontal = 20.dp)) {
                    perVehicle.take(4).forEach { (name, avg, distance) ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                            Text(name, style = MaterialTheme.typography.bodyMedium, color = palette.label, modifier = Modifier.weight(1f), maxLines = 1)
                            Text(
                                if (avg > 0) "${ZoneFormat.num(avg, 1)} km/l · ${ZoneFormat.num(distance, 0)} km" else "— km/l",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.secondary
                            )
                            IconButton(
                                onClick = { deletingVehicle = vehicles.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: Vehicle(ZoneFormat.uid("vehicle"), name) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.DeleteOutline,
                                    contentDescription = "Remove $name",
                                    tint = palette.red,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        item {
            Row(
                Modifier.padding(horizontal = 20.dp).padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.Text(
                    "LOGBOOK",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.secondary
                )
                Spacer(Modifier.weight(1f))
                androidx.compose.material3.TextButton(onClick = { showFillDialog = true; editingFill = null }) {
                    Text("+ Log a fill", color = palette.accent, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search vehicle, note or date", color = palette.secondary) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = palette.secondary) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = zoneFieldColors(),
                modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth()
            )
        }
        if (visibleFills.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Rounded.DirectionsCar,
                    title = if (fills.isEmpty()) "No fill-ups yet" else "Nothing matches",
                    subtitle = if (fills.isEmpty())
                        "Log your first tank — Zone chains the odometer and does the mileage math."
                    else
                        "Try a different search or clear the vehicle filter."
                )
            }
        } else {
            items(visibleFills.take(60), key = { it.id }) { fill ->
                FillRow(
                    fill = fill,
                    currency = currency,
                    onEdit = { editingFill = fill; showFillDialog = true },
                    onDelete = { deletingFill = fill }
                )
            }
        }
        item { Spacer(Modifier.height(28.dp)) }
    }

    if (showFillDialog) {
        FillDialog(
            initial = editingFill,
            knownVehicles = vehicles.map { it.name },
            lastEndFor = { name ->
                fills.filter { it.vehicle.equals(name, ignoreCase = true) }.maxOfOrNull { it.end }
            },
            onDismiss = { showFillDialog = false; editingFill = null },
            onSave = { fill ->
                upsertFill(fill)
                showFillDialog = false
                editingFill = null
            }
        )
    }

    if (showVehicleDialog) {
        VehicleDialog(
            onDismiss = { showVehicleDialog = false; editingVehicle = null },
            onSave = { vehicle ->
                val list = vehicles.toMutableList()
                val index = list.indexOfFirst { it.id == vehicle.id }
                if (index >= 0) list[index] = vehicle else list.add(vehicle)
                ZoneStore.writeTyped(ZoneStore.KEY_VEHICLES, list)
                vehicles = list
                ZoneStore.logAudit("mileage", if (index >= 0) "update" else "add", vehicle.name)
                showVehicleDialog = false
                editingVehicle = null
            }
        )
    }

    deletingFill?.let { target ->
        ConfirmDialog(
            title = "Delete fill-up?",
            message = "${target.vehicle} · ${ZoneFormat.dateMedium(target.date)} · ${target.end - target.start} km",
            confirmLabel = "Delete",
            onConfirm = {
                persist(fills.filterNot { it.id == target.id }, "delete", target.vehicle)
            },
            onDismiss = { deletingFill = null }
        )
    }

    deletingVehicle?.let { target ->
        ConfirmDialog(
            title = "Remove vehicle?",
            message = "“${target.name}” and its fill-ups will be deleted.",
            confirmLabel = "Remove",
            onConfirm = {
                val remainingFills = fills.filterNot { it.vehicle.equals(target.name, ignoreCase = true) }
                val remainingVehicles = vehicles.filterNot { it.id == target.id }
                persist(remainingFills, "delete", "Vehicle: ${target.name}", remainingVehicles)
                if (selectedVehicle.equals(target.name, ignoreCase = true)) selectedVehicle = null
            },
            onDismiss = { deletingVehicle = null }
        )
    }
}

/** Re-link each vehicle's odometer chain by date: every fill's start becomes the previous fill's end. */
private fun relinkChains(fills: List<Fill>): List<Fill> {
    val sorted = fills.sortedWith(compareBy({ it.vehicle.lowercase() }, { it.date }, { it.start }))
    val result = mutableListOf<Fill>()
    var previousByVehicle = mutableMapOf<String, Fill>()
    for (fill in sorted) {
        val key = fill.vehicle.lowercase()
        val previous = previousByVehicle[key]
        val adjusted = if (previous != null && fill.start <= previous.end && fill.id != previous.id) {
            fill.copy(start = previous.end)
        } else {
            fill
        }
        result += adjusted
        previousByVehicle[key] = adjusted
    }
    return result
}

private fun inferVehicleType(name: String): String {
    val n = name.lowercase()
    return when {
        n.contains("bike") || n.contains("cycle") -> "bike"
        n.contains("scooter") || n.contains("scooty") -> "scooter"
        n.contains("truck") -> "truck"
        n.contains("suv") || n.contains("creta") || n.contains("fortuner") -> "suv"
        else -> "car"
    }
}

@Composable
private fun FillRow(fill: Fill, currency: String, onEdit: () -> Unit, onDelete: () -> Unit) {
    val palette = zonePalette()
    val distance = fill.end - fill.start
    val litres = if (fill.price > 0) fill.paid / fill.price else 0.0
    val mileage = if (distance > 0 && litres > 0) distance / litres else 0.0
    val costPerKm = if (distance > 0) fill.paid / distance else 0.0
    ZoneCard(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        fill.vehicle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = palette.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        ZoneFormat.dateShort(fill.date),
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.secondary
                    )
                }
                Text(
                    "${ZoneFormat.num(distance, 1)} km · ${ZoneFormat.num(litres, 2)} L · ${ZoneFormat.money(fill.paid, currency)} paid",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.secondary
                )
                Text(
                    if (mileage > 0)
                        "${ZoneFormat.num(mileage, 1)} km/l · ${ZoneFormat.money(costPerKm, currency)}/km${if (fill.note.isNotBlank()) " · ${fill.note}" else ""}"
                    else
                        fill.note.ifBlank { "Needs distance, price and paid > 0" },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (mileage > 0) palette.secondary else palette.orange
                )
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Rounded.Edit, contentDescription = "Edit", tint = palette.secondary, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = palette.red, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FillDialog(
    initial: Fill?,
    knownVehicles: List<String>,
    lastEndFor: (String) -> Double?,
    onDismiss: () -> Unit,
    onSave: (Fill) -> Unit
) {
    val palette = zonePalette()
    var vehicle by remember { mutableStateOf(initial?.vehicle ?: knownVehicles.firstOrNull() ?: "") }
    var date by remember { mutableStateOf(initial?.date ?: ZoneFormat.todayISO()) }
    var start by remember { mutableStateOf(initial?.start?.toString()?.removeSuffix(".0") ?: lastEndFor(initial?.vehicle ?: knownVehicles.firstOrNull() ?: "")?.toString()?.removeSuffix(".0") ?: "") }
    var end by remember { mutableStateOf(initial?.end?.toString()?.removeSuffix(".0") ?: "") }
    var price by remember { mutableStateOf(initial?.price?.toString()?.removeSuffix(".0") ?: "") }
    var paid by remember { mutableStateOf(initial?.paid?.toString()?.removeSuffix(".0") ?: "") }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }

    val startNum = start.toDoubleOrNull()
    val endNum = end.toDoubleOrNull()
    val priceNum = price.toDoubleOrNull()
    val paidNum = paid.toDoubleOrNull()
    val distanceOk = startNum != null && endNum != null && endNum > startNum
    val litres = if (paidNum != null && priceNum != null && priceNum > 0) paidNum / priceNum else null
    val liveMileage = if (distanceOk && litres != null && litres > 0) (endNum!! - startNum!!) / litres else null

    FormDialog(
        title = if (initial == null) "Log a fill-up" else "Edit fill-up",
        confirmLabel = if (initial == null) "Log fill" else "Save",
        confirmEnabled = distanceOk && (priceNum ?: 0.0) > 0.0 && (paidNum ?: 0.0) > 0.0 && vehicle.trim().isNotEmpty(),
        onConfirm = {
            onSave(
                Fill(
                    id = initial?.id ?: ZoneFormat.uid("trip"),
                    vehicle = vehicle.trim().take(40),
                    date = date,
                    start = startNum ?: 0.0,
                    end = endNum ?: 0.0,
                    price = priceNum ?: 0.0,
                    paid = paidNum ?: 0.0,
                    note = note.trim().take(90)
                )
            )
        },
        onDismiss = onDismiss
    ) {
        ZoneTextField(value = vehicle, onValueChange = { vehicle = it }, label = "Vehicle")
        if (knownVehicles.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                knownVehicles.forEach { name ->
                    FilterChip(
                        selected = vehicle.equals(name, ignoreCase = true),
                        onClick = {
                            vehicle = name
                            if (start.isBlank()) {
                                lastEndFor(name)?.let { start = it.toString().removeSuffix(".0") }
                            }
                        },
                        label = { Text(name) },
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ZoneTextField(value = start, onValueChange = { start = it }, label = "Start km", keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1f))
            ZoneTextField(value = end, onValueChange = { end = it }, label = "End km", keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ZoneTextField(value = price, onValueChange = { price = it }, label = "Price / L", keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1f))
            ZoneTextField(value = paid, onValueChange = { paid = it }, label = "Total paid", keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1f))
        }
        ZoneTextField(value = note, onValueChange = { note = it }, label = "Note (optional)")
        Text(
            when {
                liveMileage != null -> "≈ ${ZoneFormat.num(litres!!, 2)} L · ${ZoneFormat.num(liveMileage, 1)} km/l"
                !distanceOk -> "End odometer must be beyond the start."
                else -> "Enter price per litre and total paid for the mileage read."
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (liveMileage != null) palette.green else palette.secondary
        )
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VehicleDialog(onDismiss: () -> Unit, onSave: (Vehicle) -> Unit) {
    val palette = zonePalette()
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("car") }
    var fuel by remember { mutableStateOf("petrol") }

    FormDialog(
        title = "Add vehicle",
        confirmLabel = "Add",
        confirmEnabled = name.trim().isNotEmpty(),
        onConfirm = {
            onSave(Vehicle(id = ZoneFormat.uid("vehicle"), name = name.trim().take(40), type = type, fuel = fuel, createdAt = ZoneFormat.todayISO()))
        },
        onDismiss = onDismiss
    ) {
        ZoneTextField(value = name, onValueChange = { name = it }, label = "Vehicle name")
        Column {
            Text("Type", style = MaterialTheme.typography.labelSmall, color = palette.secondary)
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                VEHICLE_TYPES.forEach { t ->
                    FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t.replaceFirstChar { it.uppercase() }) }, shape = RoundedCornerShape(20.dp))
                }
            }
        }
        Column {
            Text("Fuel", style = MaterialTheme.typography.labelSmall, color = palette.secondary)
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FUEL_TYPES.forEach { t ->
                    FilterChip(selected = fuel == t, onClick = { fuel = t }, label = { Text(t.replaceFirstChar { it.uppercase() }) }, shape = RoundedCornerShape(20.dp))
                }
            }
        }
    }
}

private fun exportMileageCsv(context: android.content.Context, fills: List<Fill>) {
    val header = "Date,Vehicle,Start km,End km,Distance km,Price per litre,Total paid,Mileage km/l,Note"
    val rows = fills.sortedBy { it.date }.map { fill ->
        val distance = fill.end - fill.start
        val litres = if (fill.price > 0) fill.paid / fill.price else 0.0
        val mileage = if (litres > 0 && distance > 0) distance / litres else 0.0
        listOf(
            fill.date, fill.vehicle, ZoneFormat.num(fill.start, 0), ZoneFormat.num(fill.end, 0),
            ZoneFormat.num(distance, 1), ZoneFormat.num(fill.price, 2), ZoneFormat.num(fill.paid, 2),
            ZoneFormat.num(mileage, 2), fill.note
        ).joinToString(",") { ZoneFormat.csvEscape(it) }
    }
    ZoneFormat.shareText(context, "Zone — mileage logbook", (listOf(header) + rows).joinToString("\n"), "text/csv")
}
