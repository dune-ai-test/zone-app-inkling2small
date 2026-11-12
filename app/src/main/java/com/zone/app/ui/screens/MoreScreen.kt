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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zone.app.ui.components.HairLine
import com.zone.app.ui.components.IconWell
import com.zone.app.ui.components.LargeTitle
import com.zone.app.ui.components.ZoneScreen
import com.zone.app.ui.navigation.Desk
import com.zone.app.ui.navigation.DeskTool
import com.zone.app.ui.theme.zonePalette
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight

private data class MoreGroup(val title: String, val ids: List<String>)

private val MORE_GROUPS = listOf(
    MoreGroup("Personal", listOf("habits", "notes")),
    MoreGroup("Money", listOf("passbook", "split", "mileage")),
    MoreGroup("Library", listOf("watchlist", "links", "subscriptions")),
    MoreGroup("System", listOf("admin", "settings"))
)

@Composable
fun MoreScreen(onNavigate: (String) -> Unit) {
    val palette = zonePalette()
    val toolsById = Desk.TOOLS.associateBy { it.id }

    ZoneScreen(
        title = "More",
        subtitle = "The rest of the desk."
    ) {
        MORE_GROUPS.forEach { group ->
            if (group.title != "System") {
                item(key = "label-${group.title}") {
                    SectionLabelLocal(group.title)
                }
            } else {
                item(key = "label-${group.title}") {
                    SectionLabelLocal(group.title)
                }
            }
            item(key = "group-${group.title}") {
                Column(
                    Modifier
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(palette.surface)
                ) {
                    val groupTools = group.ids.mapNotNull { toolsById[it] }
                    groupTools.forEachIndexed { index, tool ->
                        MoreRow(tool = tool, onClick = { onNavigate(tool.route) })
                        if (index != groupTools.lastIndex) HairLine()
                    }
                }
            }
        }
        item(key = "colophon") {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(palette.accent),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Z",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text("Zone", style = MaterialTheme.typography.titleMedium, color = palette.label)
                Text(
                    "The everyday, kept · v1.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.secondary
                )
            }
        }
    }
}

@Composable
private fun SectionLabelLocal(text: String) {
    Row(Modifier.padding(horizontal = 20.dp).padding(top = 14.dp, bottom = 6.dp)) {
        Text(
            text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = zonePalette().secondary
        )
    }
}

@Composable
private fun MoreRow(tool: DeskTool, onClick: () -> Unit) {
    val palette = zonePalette()
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconWell(icon = tool.icon, tint = tool.tint)
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                tool.label,
                style = MaterialTheme.typography.titleMedium,
                color = palette.label
            )
            Text(
                tool.desc,
                style = MaterialTheme.typography.bodySmall,
                color = palette.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = palette.secondary)
    }
}
