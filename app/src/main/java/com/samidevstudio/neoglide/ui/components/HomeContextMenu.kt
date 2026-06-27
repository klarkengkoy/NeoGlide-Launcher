package com.samidevstudio.neoglide.ui.components

import android.content.ComponentName
import android.content.Intent
import android.provider.Settings
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Widgets
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
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

@Composable
fun HomeContextMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    isRightAligned: Boolean = false,
    isBottomAligned: Boolean = false,
    onOpenWidgets: () -> Unit,
    onAddApp: () -> Unit,
    onOpenLauncherSettings: () -> Unit
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
            HomeContextMenuContent(
                isRightAligned = isRightAligned,
                onDismissRequest = onDismissRequest,
                onOpenWidgets = onOpenWidgets,
                onAddApp = onAddApp,
                onOpenLauncherSettings = onOpenLauncherSettings
            )
        }
    }
}

@Composable
fun HomeContextMenuContent(
    isRightAligned: Boolean,
    onDismissRequest: () -> Unit,
    onOpenWidgets: () -> Unit,
    onAddApp: () -> Unit,
    onOpenLauncherSettings: () -> Unit
) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier.width(IntrinsicSize.Max),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = if (isRightAligned) Alignment.End else Alignment.Start
    ) {
        // QUICK ADD SEGMENT
        MenuSegment {
            Column {
                HomeMenuItem(
                    text = "Add Application",
                    icon = Icons.Default.Add,
                    isRightAligned = isRightAligned,
                    onClick = {
                        onDismissRequest()
                        onAddApp()
                    }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
                HomeMenuItem(
                    text = "Add Widgets",
                    icon = Icons.Default.Widgets,
                    isRightAligned = isRightAligned,
                    onClick = {
                        onDismissRequest()
                        onOpenWidgets()
                    }
                )
            }
        }

        // CUSTOMIZATION SEGMENT
        MenuSegment(
            onClick = {
                onDismissRequest()
                try {
                    val intent = Intent(Intent.ACTION_SET_WALLPAPER)
                    context.startActivity(Intent.createChooser(intent, "Select Wallpaper"))
                } catch (_: Exception) {
                    val intent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_LAUNCHER)
                        component = ComponentName("com.android.settings", "com.android.settings.Settings\$WallpaperSettingsActivity")
                    }
                    try {
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        context.startActivity(Intent(Settings.ACTION_SETTINGS))
                    }
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
                        Icons.Default.Palette, 
                        contentDescription = null, 
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Text(
                    text = "Wallpapers", 
                    style = MaterialTheme.typography.bodyMedium, 
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = if (isRightAligned) TextAlign.End else TextAlign.Start
                )
                if (isRightAligned) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        Icons.Default.Palette, 
                        contentDescription = null, 
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // SETTINGS SEGMENT
        MenuSegment {
            Column {
                HomeMenuItem(
                    text = "Launcher Settings",
                    icon = Icons.Default.Tune,
                    isRightAligned = isRightAligned,
                    onClick = {
                        onDismissRequest()
                        onOpenLauncherSettings()
                    }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
                HomeMenuItem(
                    text = "System Settings",
                    icon = Icons.Default.Settings,
                    isRightAligned = isRightAligned,
                    onClick = {
                        onDismissRequest()
                        context.startActivity(Intent(Settings.ACTION_SETTINGS))
                    }
                )
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
private fun HomeMenuItem(
    text: String,
    icon: ImageVector,
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
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = if (isRightAligned) TextAlign.End else TextAlign.Start
        )
        if (isRightAligned) {
            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
