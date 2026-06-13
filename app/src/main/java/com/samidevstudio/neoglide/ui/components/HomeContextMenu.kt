package com.samidevstudio.neoglide.ui.components

import android.content.ComponentName
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

@Composable
fun HomeContextMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    onOpenWidgets: () -> Unit,
    onAddApp: () -> Unit,
    onOpenLauncherSettings: () -> Unit
) {
    val context = LocalContext.current

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        offset = offset,
        shape = RoundedCornerShape(24.dp)
    ) {
        DropdownMenuItem(
            text = { Text("Add application") },
            onClick = {
                onDismissRequest()
                onAddApp()
            },
            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) }
        )
        DropdownMenuItem(
            text = { Text("Add Widgets") },
            onClick = {
                onDismissRequest()
                onOpenWidgets()
            },
            leadingIcon = { Icon(Icons.Default.Widgets, contentDescription = null) }
        )
        DropdownMenuItem(
            text = { Text("Wallpapers") },
            onClick = {
                onDismissRequest()
                try {
                    val intent = Intent(Intent.ACTION_SET_WALLPAPER)
                    context.startActivity(Intent.createChooser(intent, "Select Wallpaper"))
                } catch (_: Exception) {
                    // Fallback to system wallpaper picker
                    val intent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_LAUNCHER)
                        component = ComponentName("com.android.settings", "com.android.settings.Settings\$WallpaperSettingsActivity")
                    }
                    try {
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        // Final fallback to display settings
                        context.startActivity(Intent(Settings.ACTION_SETTINGS))
                    }
                }
            },
            leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null) }
        )
        DropdownMenuItem(
            text = { Text("Launcher settings") },
            onClick = {
                onDismissRequest()
                onOpenLauncherSettings()
            },
            leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null) }
        )
        DropdownMenuItem(
            text = { Text("System settings") },
            onClick = {
                onDismissRequest()
                context.startActivity(Intent(Settings.ACTION_SETTINGS))
            },
            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) }
        )
    }
}
