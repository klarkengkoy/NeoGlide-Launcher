package com.samidevstudio.pxllauncherneo.ui.components

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.samidevstudio.pxllauncherneo.domain.model.AppShortcut

@Composable
fun AppContextMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    packageName: String,
    label: String,
    shortcuts: List<AppShortcut> = emptyList(),
    isHidden: Boolean = false,
    onShortcutClick: (AppShortcut) -> Unit = {},
    onHideToggle: () -> Unit = {}
) {
    val context = LocalContext.current

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest
    ) {
        DropdownMenuItem(
            text = { Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) },
            onClick = {},
            enabled = false
        )
        
        if (shortcuts.isNotEmpty()) {
            HorizontalDivider()
            shortcuts.forEach { shortcut ->
                DropdownMenuItem(
                    text = { Text(shortcut.label) },
                    onClick = {
                        onDismissRequest()
                        onShortcutClick(shortcut)
                    },
                    leadingIcon = {
                        if (shortcut.icon != null) {
                            AsyncImage(
                                model = shortcut.icon,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Icon(Icons.Default.Link, contentDescription = null)
                        }
                    }
                )
            }
        }

        HorizontalDivider()
        
        DropdownMenuItem(
            text = { Text(if (isHidden) "Unhide App" else "Hide App") },
            onClick = {
                onDismissRequest()
                onHideToggle()
            },
            leadingIcon = { Icon(Icons.Default.VisibilityOff, contentDescription = null) }
        )

        DropdownMenuItem(
            text = { Text("App Info") },
            onClick = {
                onDismissRequest()
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            },
            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
        )
        DropdownMenuItem(
            text = { Text("Uninstall", color = MaterialTheme.colorScheme.error) },
            onClick = {
                onDismissRequest()
                val intent = Intent(Intent.ACTION_DELETE).apply {
                    data = Uri.fromParts("package", packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            },
            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
        )
    }
}
