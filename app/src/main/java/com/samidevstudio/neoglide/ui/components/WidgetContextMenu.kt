package com.samidevstudio.neoglide.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

@Composable
fun WidgetContextMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onRemove: () -> Unit,
    onOpenApp: () -> Unit,
    offset: DpOffset = DpOffset(0.dp, 0.dp)
) {
    if (expanded) {
        val density = LocalDensity.current
        val offsetPx = with(density) {
            androidx.compose.ui.unit.IntOffset(
                offset.x.roundToPx(),
                offset.y.roundToPx()
            )
        }

        Popup(
            offset = offsetPx,
            onDismissRequest = onDismissRequest,
            properties = PopupProperties(focusable = false)
        ) {
            Surface(
                modifier = Modifier.width(IntrinsicSize.Max),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    DropdownMenuItem(
                        text = { Text("Open App", style = MaterialTheme.typography.labelLarge) },
                        onClick = {
                            onDismissRequest()
                            onOpenApp()
                        },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Launch, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Remove", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            onDismissRequest()
                            onRemove()
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                    )
                }
            }
        }
    }
}
