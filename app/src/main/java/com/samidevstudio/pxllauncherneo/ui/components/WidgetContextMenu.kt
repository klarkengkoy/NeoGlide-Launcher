package com.samidevstudio.pxllauncherneo.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

@Composable
fun WidgetContextMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onRemove: () -> Unit,
    onOpenApp: () -> Unit,
    offset: DpOffset = DpOffset(0.dp, 0.dp)
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        offset = offset
    ) {
        DropdownMenuItem(
            text = { Text("Open App") },
            onClick = {
                onDismissRequest()
                onOpenApp()
            },
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Launch, contentDescription = null) }
        )
        DropdownMenuItem(
            text = { Text("Remove", color = MaterialTheme.colorScheme.error) },
            onClick = {
                onDismissRequest()
                onRemove()
            },
            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
        )
    }
}
