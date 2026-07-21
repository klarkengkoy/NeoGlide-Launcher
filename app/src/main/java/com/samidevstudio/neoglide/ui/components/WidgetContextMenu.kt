package com.samidevstudio.neoglide.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
    onAppInfo: () -> Unit,
    label: String,
    shortcuts: List<com.samidevstudio.neoglide.domain.model.AppShortcut> = emptyList(),
    onShortcutClick: (com.samidevstudio.neoglide.domain.model.AppShortcut) -> Unit = {},
    isRightAligned: Boolean = false,
    isBottomAligned: Boolean = false,
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
            alignment = when {
                isBottomAligned && isRightAligned -> Alignment.BottomEnd
                isBottomAligned -> Alignment.BottomStart
                isRightAligned -> Alignment.TopEnd
                else -> Alignment.TopStart
            },
            onDismissRequest = onDismissRequest,
            properties = PopupProperties(focusable = false)
        ) {
            WidgetContextMenuContent(
                label = label,
                isRightAligned = isRightAligned,
                shortcuts = shortcuts,
                onShortcutClick = onShortcutClick,
                onDismissRequest = onDismissRequest,
                onRemove = onRemove,
                onOpenApp = onOpenApp,
                onAppInfo = onAppInfo
            )
        }
    }
}

@Composable
fun WidgetContextMenuContent(
    label: String,
    isRightAligned: Boolean,
    shortcuts: List<com.samidevstudio.neoglide.domain.model.AppShortcut>,
    onShortcutClick: (com.samidevstudio.neoglide.domain.model.AppShortcut) -> Unit,
    onDismissRequest: () -> Unit,
    onRemove: () -> Unit,
    onOpenApp: () -> Unit,
    onAppInfo: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.width(IntrinsicSize.Max),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = if (isRightAligned) Alignment.End else Alignment.Start
    ) {
        // TOP ACTION ROW (Label & Info)
        MenuSegment {
            Row(
                modifier = Modifier
                    .width(IntrinsicSize.Max)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable {
                        onDismissRequest()
                        onAppInfo()
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
        if (shortcuts.isNotEmpty()) {
            MenuSegment {
                Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                    shortcuts.forEachIndexed { index, shortcut ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDismissRequest()
                                    onShortcutClick(shortcut)
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = if (isRightAligned) Arrangement.End else Arrangement.Start
                        ) {
                            if (!isRightAligned) {
                                coil.compose.AsyncImage(
                                    model = shortcut.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                            Text(
                                text = shortcut.label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isRightAligned) {
                                Spacer(modifier = Modifier.width(12.dp))
                                coil.compose.AsyncImage(
                                    model = shortcut.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        if (index < shortcuts.size - 1) {
                            androidx.compose.material3.HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }
        }

        // PRIMARY ACTION (Open App)
        MenuSegment(
            onClick = {
                onDismissRequest()
                onOpenApp()
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
                        Icons.AutoMirrored.Filled.Launch,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Text(
                    text = "Open App",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = if (isRightAligned) TextAlign.End else TextAlign.Start
                )
                if (isRightAligned) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.Launch,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // BOTTOM REMOVE SEGMENT
        MenuSegment(
            onClick = {
                onDismissRequest()
                onRemove()
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
                        Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Text(
                    text = "Remove",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = if (isRightAligned) TextAlign.End else TextAlign.Start
                )
                if (isRightAligned) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurface
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
