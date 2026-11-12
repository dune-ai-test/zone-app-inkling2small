package com.zone.app.ui.components

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyColumnScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zone.app.ui.theme.ZonePalette
import com.zone.app.ui.theme.zonePalette

/** Screen scaffold: scrollable content under a large iOS-style title. */
@Composable
fun ZoneScreen(
    title: String,
    subtitle: String? = null,
    actions: (@Composable () -> Unit)? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    content: LazyColumnScope.() -> Unit
) {
    val palette = zonePalette()
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.background),
        content = {
            item(key = "__header__") {
                LargeTitle(title = title, subtitle = subtitle, actions = actions, navigationIcon = navigationIcon)
            }
            content()
            item(key = "__footer__") {
                Spacer(Modifier.height(48.dp))
            }
        }
    )
}

@Composable
fun LargeTitle(
    title: String,
    subtitle: String? = null,
    actions: (@Composable () -> Unit)? = null,
    navigationIcon: (@Composable () -> Unit)? = null
) {
    val palette = zonePalette()
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 14.dp, bottom = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (navigationIcon != null) {
                Box(Modifier.size(38.dp), contentAlignment = Alignment.CenterStart) {
                    navigationIcon()
                }
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = title,
                style = androidx.compose.material3.MaterialTheme.typography.displaySmall,
                color = palette.label,
                modifier = Modifier.weight(1f),
                maxLines = 2
            )
            actions?.let {
                Row(verticalAlignment = Alignment.CenterVertically) { it() }
            }
        }
        if (!subtitle.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                color = palette.secondary
            )
        }
    }
}

/** White card with a hairline border — the iOS inset-grouped building block. */
@Composable
fun ZoneCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val palette = zonePalette()
    val base = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(palette.surface)
        .border(0.5.dp, palette.separator, RoundedCornerShape(18.dp))
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .padding(16.dp)
    Box(base.then(modifier)) { content() }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    val palette = zonePalette()
    Text(
        text = text.uppercase(),
        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
        color = palette.secondary,
        modifier = modifier
            .padding(horizontal = 20.dp)
            .padding(top = 18.dp, bottom = 6.dp)
    )
}

@Composable
fun EmptyState(icon: ImageVector, title: String, subtitle: String, modifier: Modifier = Modifier) {
    val palette = zonePalette()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(palette.grouped),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = palette.secondary, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text(title, style = androidx.compose.material3.MaterialTheme.typography.titleMedium, color = palette.label)
        Spacer(Modifier.height(4.dp))
        Text(
            subtitle,
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            color = palette.secondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun StatTile(value: String, label: String, modifier: Modifier = Modifier, tint: Color? = null) {
    val palette = zonePalette()
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(palette.surface)
            .border(0.5.dp, palette.separator, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = tint ?: palette.label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(label, style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = palette.secondary)
    }
}

/** Coloured rounded icon well — iOS settings-style glyph container. */
@Composable
fun IconWell(icon: ImageVector, tint: Color, modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 34.dp) {
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(9.dp))
            .background(tint),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.56f))
    }
}

/** iOS segmented control built on a single rounded surface. */
@Composable
fun SegmentedControl(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = zonePalette()
    Row(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(palette.grouped)
            .padding(2.dp)
    ) {
        options.forEachIndexed { index, label ->
            val isSelected = index == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) palette.surface else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = palette.label,
                    maxLines = 1
                )
            }
        }
    }
}

/** Hairline separator inside grouped cards. */
@Composable
fun HairLine(modifier: Modifier = Modifier) {
    val palette = zonePalette()
    Box(
        modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(palette.separator)
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String = "Delete",
    destructive: Boolean = true,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = zonePalette()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message, color = palette.secondary) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text(confirmLabel, color = if (destructive) palette.red else palette.accent, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = palette.accent) }
        },
        containerColor = palette.surface
    )
}

@Composable
fun FormDialog(
    title: String,
    confirmLabel: String = "Save",
    confirmEnabled: Boolean = true,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    val palette = zonePalette()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                content()
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = confirmEnabled) {
                Text(confirmLabel, color = palette.accent, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = palette.secondary) }
        },
        containerColor = palette.surface
    )
}

@Composable
fun zoneFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
    unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
    focusedContainerColor = zonePalette().grouped,
    unfocusedContainerColor = zonePalette().grouped,
    focusedTextColor = zonePalette().label,
    unfocusedTextColor = zonePalette().label,
    cursorColor = zonePalette().accent,
    focusedLabelColor = zonePalette().secondary,
    unfocusedLabelColor = zonePalette().secondary
)

@Composable
fun ZoneTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    keyboardType: androidx.compose.ui.text.input.KeyboardType = androidx.compose.ui.text.input.KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(12.dp),
        colors = zoneFieldColors(),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun ZonePrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = zonePalette().accent,
            contentColor = Color.White
        )
    ) {
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

/** Small round "+" button for toolbars / FABs. */
@Composable
fun RoundAddButton(onClick: () -> Unit, contentDescription: String = "Add") {
    val palette = zonePalette()
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(palette.accent)
    ) {
        Icon(
            androidx.compose.material.icons.Icons.Rounded.Add,
            contentDescription = contentDescription,
            tint = Color.White
        )
    }
}
