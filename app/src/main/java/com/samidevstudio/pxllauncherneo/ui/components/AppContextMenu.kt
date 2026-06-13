package com.samidevstudio.pxllauncherneo.ui.components

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
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
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    onShortcutClick: (AppShortcut) -> Unit = {},
    onHideToggle: () -> Unit = {},
    onRemove: (() -> Unit)? = null
) {
    if (expanded) {
        val context = LocalContext.current
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
                        color = Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onDismissRequest()
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", packageName, null)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                Icons.Default.Info,
                                contentDescription = "App Info",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
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
                        text = { Text("Hide App") },
                        onClick = {
                            onHideToggle()
                            onDismissRequest()
                        },
                        leadingIcon = { Icon(Icons.Default.VisibilityOff, contentDescription = null) }
                    )

                    if (onRemove != null) {
                        DropdownMenuItem(
                            text = { Text("Remove from Home") },
                            onClick = {
                                onRemove()
                                onDismissRequest()
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = "Remove") }
                        )
                    }

                    DropdownMenuItem(
                        text = { Text("Uninstall", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            onDismissRequest()
                            try {
                                val packageUri = Uri.fromParts("package", packageName, null)
                                val intent = Intent(Intent.ACTION_DELETE, packageUri).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                try {
                                    val intent = Intent(Intent.ACTION_UNINSTALL_PACKAGE).apply {
                                        data = Uri.parse("package:$packageName")
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (e2: Exception) {
                                    android.widget.Toast.makeText(context, "Could not uninstall app", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                    )
                }
            }
        }
    }
}
