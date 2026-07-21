package com.samidevstudio.neoglide.ui.components

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddHome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import coil.compose.AsyncImage
import com.samidevstudio.neoglide.domain.model.AppShortcut
import com.samidevstudio.neoglide.ui.theme.NeoGlideLauncherTheme

@Composable
fun AppContextMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    packageName: String,
    label: String,
    shortcuts: List<AppShortcut> = emptyList(),
    isHidden: Boolean = false,
    isPremium: Boolean = false,
    isRightAligned: Boolean = false,
    isBottomAligned: Boolean = false,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    onShortcutClick: (AppShortcut) -> Unit = {},
    onHideToggle: () -> Unit = {},
    onAddToHome: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
    onShowPaywall: () -> Unit = {}
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
            alignment = when {
                isBottomAligned && isRightAligned -> Alignment.BottomEnd
                isBottomAligned -> Alignment.BottomStart
                isRightAligned -> Alignment.TopEnd
                else -> Alignment.TopStart
            },
            onDismissRequest = onDismissRequest,
            properties = PopupProperties(focusable = true)
        ) {
            AppContextMenuContent(
                packageName = packageName,
                label = label,
                shortcuts = shortcuts,
                isHidden = isHidden,
                isPremium = isPremium,
                isRightAligned = isRightAligned,
                onDismissRequest = onDismissRequest,
                onShortcutClick = onShortcutClick,
                onHideToggle = onHideToggle,
                onAddToHome = onAddToHome,
                onRemove = onRemove,
                onShowPaywall = onShowPaywall
            )
        }
    }
}

@Composable
fun AppContextMenuContent(
    packageName: String,
    label: String,
    shortcuts: List<AppShortcut>,
    isHidden: Boolean,
    isPremium: Boolean,
    isRightAligned: Boolean,
    onDismissRequest: () -> Unit,
    onShortcutClick: (AppShortcut) -> Unit,
    onHideToggle: () -> Unit,
    onAddToHome: (() -> Unit)?,
    onRemove: (() -> Unit)?,
    onShowPaywall: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.width(IntrinsicSize.Max),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = if (isRightAligned) Alignment.End else Alignment.Start
    ) {
        // TOP ACTION ROW (App Label & Info)
        MenuSegment {
            Row(
                modifier = Modifier
                    .width(IntrinsicSize.Max)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable {
                        onDismissRequest()
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", packageName, null)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (isRightAligned) Arrangement.End else Arrangement.Start
            ) {
                if (!isRightAligned) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "App Info",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (isRightAligned) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "App Info",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // SHORTCUTS SECTION
        if (shortcuts.isNotEmpty() || onAddToHome != null) {
            MenuSegment {
                Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                    shortcuts.forEachIndexed { index, shortcut ->
                        MenuItem(
                            text = shortcut.label,
                            icon = shortcut.icon,
                            isRightAligned = isRightAligned,
                            onClick = {
                                onDismissRequest()
                                onShortcutClick(shortcut)
                            }
                        )
                        if (index < shortcuts.size - 1 || onAddToHome != null) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                        }
                    }
                    
                    if (onAddToHome != null) {
                        MenuItem(
                            text = "Add to Home",
                            vectorIcon = Icons.Default.AddHome,
                            isRightAligned = isRightAligned,
                            onClick = {
                                onDismissRequest()
                                onAddToHome()
                            }
                        )
                    }
                }
            }
        }

        // SECONDARY ACTIONS (Hide)
        MenuSegment(
            onClick = {
                if (isPremium) {
                    onHideToggle()
                    onDismissRequest()
                } else {
                    onShowPaywall()
                }
            }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (isRightAligned) Arrangement.End else Arrangement.Start
            ) {
                if (!isRightAligned) {
                    Icon(
                        Icons.Default.VisibilityOff,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                
                Text(
                    text = if (isHidden) "Show App" else "Hide App",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!isPremium) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.Default.WorkspacePremium,
                        contentDescription = "Premium",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                }

                if (isRightAligned) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        Icons.Default.VisibilityOff,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // BOTTOM REMOVE/UNINSTALL SEGMENT
        MenuSegment(
            color = if (onRemove != null) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
            onClick = {
                onDismissRequest()
                if (onRemove != null) {
                    onRemove()
                } else {
                    try {
                        val packageUri = Uri.fromParts("package", packageName, null)
                        val intent = Intent(Intent.ACTION_DELETE, packageUri).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {}
                }
            }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (isRightAligned) Arrangement.End else Arrangement.Start
            ) {
                if (!isRightAligned) {
                    Icon(
                        if (onRemove != null) Icons.Default.Close else Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (onRemove != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Text(
                    text = if (onRemove != null) "Remove" else "Uninstall",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (onRemove != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                    textAlign = if (isRightAligned) TextAlign.End else TextAlign.Start
                )
                if (isRightAligned) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        if (onRemove != null) Icons.Default.Close else Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (onRemove != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuSegment(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surface,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .width(IntrinsicSize.Max)
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                shape = RoundedCornerShape(26.dp)
            )
            .then(
                if (onClick != null) Modifier.clip(RoundedCornerShape(26.dp)).clickable(onClick = onClick)
                else Modifier
            ),
        shape = RoundedCornerShape(26.dp),
        color = color,
        tonalElevation = 8.dp,
        shadowElevation = 12.dp
    ) {
        content()
    }
}

@Composable
private fun MenuItem(
    text: String,
    icon: Any? = null,
    vectorIcon: ImageVector? = null,
    isRightAligned: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (isRightAligned) Arrangement.End else Arrangement.Start
    ) {
        if (!isRightAligned) {
            if (vectorIcon != null) {
                Icon(vectorIcon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            } else if (icon != null) {
                AsyncImage(model = icon, contentDescription = null, modifier = Modifier.size(24.dp))
            } else {
                Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.width(12.dp))
        }

        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = if (isRightAligned) TextAlign.End else TextAlign.Start
        )

        if (isRightAligned) {
            Spacer(modifier = Modifier.width(12.dp))
            if (vectorIcon != null) {
                Icon(vectorIcon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            } else if (icon != null) {
                AsyncImage(model = icon, contentDescription = null, modifier = Modifier.size(24.dp))
            } else {
                Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AppContextMenuPreview() {
    NeoGlideLauncherTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            AppContextMenuContent(
                packageName = "com.example.app",
                label = "Example App",
                shortcuts = listOf(
                    AppShortcut("1", "New Chat", "com.example.app"),
                    AppShortcut("2", "Contacts", "com.example.app")
                ),
                isHidden = false,
                isPremium = false,
                isRightAligned = false,
                onDismissRequest = {},
                onShortcutClick = {},
                onHideToggle = {},
                onAddToHome = {},
                onRemove = null,
                onShowPaywall = {}
            )
        }
    }
}
