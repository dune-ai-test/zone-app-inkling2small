package com.zone.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zone.app.data.ZoneStore
import com.zone.app.ui.components.HairLine
import com.zone.app.ui.components.IconWell
import com.zone.app.ui.components.SectionLabel
import com.zone.app.ui.components.ZoneScreen
import com.zone.app.ui.theme.zonePalette
import com.zone.app.util.ZoneFormat
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Brightness4
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.Workspaces

private val THEMES = listOf(
    Triple("light", "Light", Icons.Rounded.LightMode),
    Triple("dark", "Dark", Icons.Rounded.DarkMode),
    Triple("system", "System", Icons.Rounded.Brightness4)
)

private val CURRENCIES = listOf(
    Triple("INR", "₹", "Indian Rupee"),
    Triple("USD", "$", "US Dollar"),
    Triple("EUR", "€", "Euro"),
    Triple("GBP", "£", "British Pound"),
    Triple("AED", "د.إ", "UAE Dirham"),
    Triple("AUD", "A$", "Australian Dollar"),
    Triple("CAD", "C$", "Canadian Dollar"),
    Triple("SGD", "S$", "Singapore Dollar")
)

@Composable
fun SettingsScreen(onThemeChanged: (String) -> Unit) {
    val palette = zonePalette()

    var settings by remember { mutableStateOf(ZoneStore.settings()) }
    val recordCount = remember { ZoneStore.recordCounts().sumOf { it.second } }

    fun save(patch: com.zone.app.data.AppSettings) {
        settings = patch
        ZoneStore.saveSettings(patch)
    }

    ZoneScreen(
        title = "Settings",
        subtitle = "Appearance and display currency."
    ) {
        item { SectionLabel("Appearance") }
        item {
            Column(
                Modifier
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(palette.surface)
            ) {
                THEMES.forEachIndexed { index, (id, label, icon) ->
                    val selected = settings.theme == id
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                save(settings.copy(theme = id))
                                onThemeChanged(id)
                            }
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(icon, contentDescription = null, tint = palette.accent)
                        Spacer(Modifier.size(12.dp))
                        Text(label, style = MaterialTheme.typography.bodyLarge, color = palette.label, modifier = Modifier.weight(1f))
                        if (selected) {
                            Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = palette.accent)
                        }
                    }
                    if (index != THEMES.lastIndex) HairLine()
                }
            }
        }

        item { SectionLabel("Money & currency") }
        item {
            Column(
                Modifier
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(palette.surface)
            ) {
                CURRENCIES.forEachIndexed { index, (code, symbol, label) ->
                    val selected = settings.currency == code
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { save(settings.copy(currency = code)) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (selected) palette.accent else palette.grouped),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                symbol,
                                color = if (selected) Color.White else palette.label,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(Modifier.size(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(code, style = MaterialTheme.typography.bodyLarge, color = palette.label)
                            Text(label, style = MaterialTheme.typography.bodySmall, color = palette.secondary)
                        }
                        if (selected) {
                            Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = palette.accent)
                        }
                    }
                    if (index != CURRENCIES.lastIndex) HairLine()
                }
            }
        }
        item {
            Column(Modifier.padding(horizontal = 20.dp).padding(top = 8.dp)) {
                Text(
                    "Display only — Zone never converts. A number is shown with this symbol and locale; what you typed in is what you get back.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.secondary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "How it reads:  ${ZoneFormat.money(1234567.89, settings.currency)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.label,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        item { SectionLabel("About") }
        item {
            Column(
                Modifier
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(palette.surface)
            ) {
                AboutRow(icon = Icons.Rounded.Info, tint = Color(0xFF007AFF), label = "Version", value = "1.0 (build 1)")
                HairLine()
                AboutRow(icon = Icons.Rounded.Workspaces, tint = Color(0xFFAF52DE), label = "Records on this device", value = "$recordCount")
                HairLine()
                AboutRow(icon = Icons.Rounded.MonetizationOn, tint = Color(0xFF34C759), label = "Currency", value = settings.currency)
            }
        }
        item {
            Text(
                "Zone for Android — a native port of the Zone personal desk (Cloudflare Pages + D1). Your records never leave this device unless you export a backup yourself.",
                style = MaterialTheme.typography.bodySmall,
                color = palette.secondary,
                modifier = Modifier.padding(horizontal = 20.dp).padding(top = 18.dp)
            )
        }
    }
}

@Composable
private fun AboutRow(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, label: String, value: String) {
    val palette = zonePalette()
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconWell(icon = icon, tint = tint, size = 30.dp)
        Spacer(Modifier.size(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = palette.label, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = palette.secondary)
    }
}
