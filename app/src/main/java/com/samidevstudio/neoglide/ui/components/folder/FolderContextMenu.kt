package com.samidevstudio.neoglide.ui.components.folder

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

@Composable
fun FolderContextMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    label: String,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    onEditName: () -> Unit = {},
    onRemove: () -> Unit = {}
) {
    if (expanded) {
        val density = LocalDensity.current
        val offsetPx = with(density) {
            IntOffset(
                offset.x.roundToPx(),
                offset.y.roundToPx()
            )
        }

        Popup(
            offset = offsetPx,
            onDismissRequest = onDismissRequest,
            properties = PopupProperties(focusable = true)
        ) {
            Surface(
                modifier = Modifier.width(IntrinsicSize.Max),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp).width(IntrinsicSize.Max)) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp).clip(RoundedCornerShape(12.dp))
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                    
                    DropdownMenuItem(
                        text = { Text("Edit Name") },
                        onClick = {
                            onEditName()
                            onDismissRequest()
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                    )

                    DropdownMenuItem(
                        text = { Text("Remove Folder") },
                        onClick = {
                            onRemove()
                            onDismissRequest()
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = "Remove") }
                    )
                }
            }
        }
    }
}
