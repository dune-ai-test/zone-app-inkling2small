package com.zone.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zone.app.data.AuditEvent
import com.zone.app.data.ZoneStore
import com.zone.app.ui.components.ConfirmDialog
import com.zone.app.ui.components.HairLine
import com.zone.app.ui.components.SectionLabel
import com.zone.app.ui.components.ZoneCard
import com.zone.app.ui.components.ZoneScreen
import com.zone.app.ui.theme.zonePalette
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.ShieldMoon
import androidx.compose.material.icons.rounded.Workspaces

@Composable
fun AdminScreen() {
    val palette = zonePalette()
    val context = LocalContext.current

    // `bump` re-reads the store after destructive actions.
    var bump by remember { mutableStateOf(0) }
    val counts = remember(bump) { ZoneStore.recordCounts() }
    val totalRecords = counts.sumOf { it.second }
    val audit = remember(bump) { ZoneStore.auditEvents().take(50) }
    var confirmReset by remember { mutableStateOf(false) }
    var confirmClearAudit by remember { mutableStateOf(false) }
    var importResult by remember { mutableStateOf<String?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (text != null) {
                    val restored = ZoneStore.importBackup(text)
                    importResult = if (restored > 0) "Restored $restored workspace keys." else "That file is not a Zone backup."
                } else {
                    importResult = "Couldn't read that file."
                }
            } catch (_: Exception) {
                importResult = "Import failed — the file couldn't be parsed."
            }
            bump += 1
        }
    }

    ZoneScreen(
        title = "Admin",
        subtitle = "The back office — activity, backups, workspace."
    ) {
        item {
            ZoneCard(modifier = Modifier.padding(horizontal = 20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(44.dp).clip(CircleShape).background(palette.accent.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.ShieldMoon, contentDescription = null, tint = palette.accent)
                    }
                    Spacer(Modifier.size(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "On-device back office",
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.label
                        )
                        Text(
                            "The web build gates this page with a server secret. Here, your device's own lock screen is the gate — everything stays on-device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.secondary
                        )
                    }
                }
            }
        }

        item { SectionLabel("The register") }
        item {
            Column(
                Modifier
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(palette.surface)
            ) {
                counts.forEachIndexed { index, (tool, count) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(tool, style = MaterialTheme.typography.bodyMedium, color = palette.label, modifier = Modifier.weight(1f))
                        Text(
                            "$count",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (count > 0) palette.label else palette.secondary
                        )
                    }
                    if (index != counts.lastIndex) HairLine()
                }
                HairLine()
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(palette.grouped)
                        .padding(horizontal = 16.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "TOTAL RECORDS",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.secondary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "$totalRecords",
                        fontWeight = FontWeight.Bold,
                        color = palette.accent,
                        fontSize = 16.sp
                    )
                }
            }
        }

        item { SectionLabel("Backup & restore") }
        item {
            Column(
                Modifier
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(palette.surface)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Workspaces, contentDescription = null, tint = palette.secondary)
                    Spacer(Modifier.size(10.dp))
                    Text(
                        "Every record lives in one workspace. Export it whole, or restore from a backup file.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondary,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilledTonalButton(
                        onClick = {
                            val json = ZoneStore.exportBackup()
                            com.zone.app.util.ZoneFormat.shareText(context, "Zone backup", json, "application/json")
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = palette.accent.copy(alpha = 0.12f),
                            contentColor = palette.accent
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Export")
                    }
                    FilledTonalButton(
                        onClick = { importLauncher.launch("application/json") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Restore")
                    }
                }
                importResult?.let { result ->
                    Text(result, style = MaterialTheme.typography.bodySmall, color = palette.accent)
                }
            }
        }

        item {
            Row(
                Modifier.padding(horizontal = 20.dp).padding(top = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionLabelInlineAdmin("Activity — last ${audit.size}")
                Spacer(Modifier.weight(1f))
                if (audit.isNotEmpty()) {
                    TextButton(onClick = { confirmClearAudit = true }) {
                        Text("Clear", color = palette.red)
                    }
                }
            }
        }
        if (audit.isEmpty()) {
            item {
                Text(
                    "Nothing on the paper trail yet. Every add, edit and delete is logged here as you work.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.secondary,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        } else {
            item {
                Column(
                    Modifier
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(palette.surface)
                ) {
                    audit.forEachIndexed { index, event ->
                        AuditRow(event)
                        if (index != audit.lastIndex) HairLine()
                    }
                }
            }
        }

        item { SectionLabel("Danger zone") }
        item {
            Column(
                Modifier
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(palette.surface)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Reset the workspace",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.red
                )
                Text(
                    "Erases every record, note, entry and setting on this device. Export a backup first — there is no undo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.secondary
                )
                TextButton(onClick = { confirmReset = true }) {
                    Icon(Icons.Rounded.Restore, contentDescription = null, tint = palette.red)
                    Spacer(Modifier.size(4.dp))
                    Text("Reset everything", color = palette.red, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (confirmReset) {
        ConfirmDialog(
            title = "Reset the workspace?",
            message = "All $totalRecords records will be permanently erased from this device.",
            confirmLabel = "Erase everything",
            onConfirm = {
                ZoneStore.clearWorkspace()
                bump += 1
            },
            onDismiss = { confirmReset = false }
        )
    }

    if (confirmClearAudit) {
        ConfirmDialog(
            title = "Clear the paper trail?",
            message = "The activity log will be emptied. Your records are not affected.",
            confirmLabel = "Clear",
            onConfirm = {
                ZoneStore.clearAudit()
                bump += 1
            },
            onDismiss = { confirmClearAudit = false }
        )
    }
}

@Composable
private fun SectionLabelInlineAdmin(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = zonePalette().secondary
    )
}

@Composable
private fun AuditRow(event: AuditEvent) {
    val palette = zonePalette()
    val actionColor = when (event.action) {
        "add" -> palette.green
        "update" -> palette.accent
        "delete" -> palette.red
        "restore" -> palette.purple
        else -> palette.secondary
    }
    Row(
        Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(actionColor)
        )
        Spacer(Modifier.size(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                event.title,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.label,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                "${event.tool} · ${event.action} · ${event.at.take(10)}",
                style = MaterialTheme.typography.labelSmall,
                color = palette.secondary
            )
        }
    }
}
