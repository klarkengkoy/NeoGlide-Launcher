package com.samidevstudio.pxllauncherneo.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samidevstudio.pxllauncherneo.data.repository.*
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    onDismiss: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val preferences by viewModel.userPreferences.collectAsStateWithLifecycle()
    val isNotifEnabled by viewModel.isNotificationServiceEnabled.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    var activeDialog by remember { mutableStateOf<String?>(null) }

    // DIALOG DISPATCHER
    when (activeDialog) {
        "category" -> SelectionDialog(
            title = "Category bar",
            options = listOf(
                DialogOption("Left side", CategoryBarType.LEFT, preferences.categoryBarType == CategoryBarType.LEFT),
                DialogOption("Right side", CategoryBarType.RIGHT, preferences.categoryBarType == CategoryBarType.RIGHT),
                DialogOption("Bottom bar", CategoryBarType.BOTTOM, preferences.categoryBarType == CategoryBarType.BOTTOM),
                DialogOption("Hidden", CategoryBarType.NONE, preferences.categoryBarType == CategoryBarType.NONE)
            ),
            onDismiss = { activeDialog = null },
            onSelect = { viewModel.setCategoryBarType(it as CategoryBarType) }
        )
        "sorting" -> SelectionDialog(
            title = "Sorting",
            options = listOf(
                DialogOption("Alphabetical (A-Z)", SortingMode.ALPHABETICAL, preferences.sortingMode == SortingMode.ALPHABETICAL),
                DialogOption("Installation Time", SortingMode.INSTALL_TIME, preferences.sortingMode == SortingMode.INSTALL_TIME),
                DialogOption("Last Used", SortingMode.LAST_USED, preferences.sortingMode == SortingMode.LAST_USED),
                DialogOption("App Icon Color", SortingMode.ICON_COLOR, preferences.sortingMode == SortingMode.ICON_COLOR)
            ),
            onDismiss = { activeDialog = null },
            onSelect = { viewModel.setSortingMode(it as SortingMode) }
        )
        "grid" -> SelectionDialog(
            title = "Home screen grid",
            options = listOf(
                DialogOption("4 x 5 (Standard)", GridSize.GRID_4X5, preferences.gridSize == GridSize.GRID_4X5),
                DialogOption("5 x 5 (Dense)", GridSize.GRID_5X5, preferences.gridSize == GridSize.GRID_5X5),
                DialogOption("6 x 6 (Expert)", GridSize.GRID_6X6, preferences.gridSize == GridSize.GRID_6X6)
            ),
            onDismiss = { activeDialog = null },
            onSelect = { viewModel.setGridSize(it as GridSize) }
        )
        "search" -> SelectionDialog(
            title = "Search provider",
            options = listOf(
                DialogOption("Google", SearchProvider.GOOGLE, preferences.searchProvider == SearchProvider.GOOGLE),
                DialogOption("DuckDuckGo", SearchProvider.DUCKDUCKGO, preferences.searchProvider == SearchProvider.DUCKDUCKGO),
                DialogOption("Local Only", SearchProvider.LOCAL_ONLY, preferences.searchProvider == SearchProvider.LOCAL_ONLY)
            ),
            onDismiss = { activeDialog = null },
            onSelect = { viewModel.setSearchProvider(it as SearchProvider) }
        )
        "notif" -> SelectionDialog(
            title = "Notification dots",
            options = listOf(
                DialogOption("App Icon", NotificationDotMode.APP_ICON, preferences.notificationDotMode == NotificationDotMode.APP_ICON),
                DialogOption("Category Bar", NotificationDotMode.CATEGORY_BAR, preferences.notificationDotMode == NotificationDotMode.CATEGORY_BAR),
                DialogOption("Both", NotificationDotMode.BOTH, preferences.notificationDotMode == NotificationDotMode.BOTH),
                DialogOption("None", NotificationDotMode.NONE, preferences.notificationDotMode == NotificationDotMode.NONE)
            ),
            onDismiss = { activeDialog = null },
            onSelect = { viewModel.setNotificationDotMode(it as NotificationDotMode) }
        )
        "trouble" -> AlertDialog(
            onDismissRequest = { activeDialog = null },
            title = { Text("Troubleshooting") },
            text = { Text("Choose an action to resolve launcher issues.") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearIconCache(); activeDialog = null }) { Text("Clear Icon Cache") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.resetLayout(); activeDialog = null }) { Text("Reset Layout") }
            }
        )
        "about" -> AboutDialog(onDismiss = { activeDialog = null })
        "notif_settings" -> NotificationSettingsDialog(
            isNotifEnabled = isNotifEnabled,
            dotMode = preferences.notificationDotMode,
            onDismiss = { activeDialog = null },
            onSelectDotMode = { viewModel.setNotificationDotMode(it) }
        )
        "label_settings" -> AppLabelSettingsDialog(
            labelMode = preferences.appLabelMode,
            onDismiss = { activeDialog = null },
            onSelectLabelMode = { viewModel.setAppLabelMode(it) }
        )
    }
    
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.checkNotificationPermission()
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        QuickActionItem(Icons.Default.Add, "Shortcut") { }
                        QuickActionItem(Icons.Default.CreateNewFolder, "Folder") { }
                        QuickActionItem(Icons.Default.Category, "Category") { }
                    }
                }

                item {
                    SettingsGroup(title = "APPLICATION DRAWER") {
                        SettingsItem(
                            icon = Icons.Default.ViewStream,
                            title = "Category bar",
                            onClick = { activeDialog = "category" },
                            trailing = { 
                                val label = when(preferences.categoryBarType) {
                                    CategoryBarType.LEFT -> "Left"
                                    CategoryBarType.RIGHT -> "Right"
                                    CategoryBarType.BOTTOM -> "Bottom"
                                    CategoryBarType.NONE -> "Hidden"
                                }
                                ValueLabel(label) 
                            }
                        )
                        ToggleSettingsItem(
                            icon = Icons.Default.Anchor,
                            title = "Bottom-anchored",
                            checked = preferences.isBottomAnchored,
                            onCheckedChange = { viewModel.setIsBottomAnchored(it) }
                        )
                        SettingsItem(
                            icon = Icons.Default.SortByAlpha,
                            title = "Sorting",
                            onClick = { activeDialog = "sorting" },
                            trailing = { ValueLabel(preferences.sortingMode.name.formatLabel()) }
                        )
                        SettingsItem(
                            icon = Icons.Default.VisibilityOff,
                            title = "Hidden apps",
                            onClick = { /* TODO */ }
                        )
                    }
                }

                item {
                    SettingsGroup(title = "HOME SCREEN") {
                        SettingsItem(
                            icon = Icons.Default.Smartphone, 
                            title = "Home grid", 
                            onClick = { activeDialog = "grid" },
                            trailing = { ValueLabel(preferences.gridSize.name.split("_").last()) }
                        ) 
                        ToggleSettingsItem(
                            icon = Icons.Default.Lock,
                            title = "Prevent changes",
                            checked = preferences.lockLayout,
                            onCheckedChange = { viewModel.setLockLayout(it) }
                        )
                    }
                }

                item {
                    SettingsGroup(title = "GLOBAL") {
                        SettingsItem(
                            icon = Icons.Default.Notifications,
                            title = "Notifications",
                            onClick = { activeDialog = "notif_settings" }
                        )
                        SettingsItem(
                            icon = Icons.Default.Search, 
                            title = "Search provider", 
                            onClick = { activeDialog = "search" },
                            trailing = { ValueLabel(preferences.searchProvider.name.formatLabel()) }
                        )
                        SettingsItem(
                            icon = Icons.Default.FontDownload,
                            title = "App names",
                            onClick = { activeDialog = "label_settings" }
                        )
                        SettingsItem(Icons.Default.Wallpaper, "Wallpaper", onClick = { })
                        SettingsItem(Icons.Default.Palette, "Appearance", onClick = { })
                        SettingsItem(Icons.Default.Backup, "Backup", onClick = { })
                    }
                }

                item {
                    SettingsGroup(title = "OTHER") {
                        SettingsItem(Icons.Default.WorkspacePremium, "Premium features", onClick = { }, trailing = { Icon(Icons.Default.WorkspacePremium, null, tint = MaterialTheme.colorScheme.tertiary) })
                        SettingsItem(Icons.Default.VerifiedUser, "Security and privacy", onClick = { })
                        SettingsItem(Icons.Default.Build, "Troubleshooting", onClick = { activeDialog = "trouble" })
                        SettingsItem(
                            icon = Icons.Default.BugReport,
                            title = "Trigger Test Crash",
                            onClick = { throw RuntimeException("Test Crash") }
                        )
                        SettingsItem(Icons.Default.RateReview, "Support us with a review", onClick = { })
                        SettingsItem(
                            icon = Icons.Default.Info,
                            title = "About Pxl Launcher",
                            onClick = { activeDialog = "about" },
                            trailing = { Text("v1.0-neo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        )
                    }
                }
                
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

private fun String.formatLabel(): String = this.replace("_", " ")
    .lowercase(Locale.getDefault())
    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsDialog(
    isNotifEnabled: Boolean,
    dotMode: NotificationDotMode,
    onDismiss: () -> Unit,
    onSelectDotMode: (NotificationDotMode) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Notifications, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Notifications")
            }
        },
        text = {
            Column {
                Text(
                    "Stay organized with notification badges. Enable access to see counts on your icons and in the app drawer.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(24.dp))

                // Permission Item
                Surface(
                    onClick = {
                        try {
                            val intent = android.content.Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            android.widget.Toast.makeText(context, "Cannot open settings", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = if (!isNotifEnabled) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)) else null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (isNotifEnabled) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (isNotifEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Enable Notification Badges",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isNotifEnabled) "Active • Counting notifications" else "Tap to enable numeric badges",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isNotifEnabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Display badges on:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                // CHIP GROUP FOR MODERN SELECTION
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val showOnIcons = dotMode == NotificationDotMode.APP_ICON || dotMode == NotificationDotMode.BOTH
                    val showOnRail = dotMode == NotificationDotMode.CATEGORY_BAR || dotMode == NotificationDotMode.BOTH

                    FilterChip(
                        selected = showOnIcons,
                        onClick = {
                            val newMode = when {
                                !showOnIcons && showOnRail -> NotificationDotMode.BOTH
                                !showOnIcons && !showOnRail -> NotificationDotMode.APP_ICON
                                showOnIcons && showOnRail -> NotificationDotMode.CATEGORY_BAR
                                else -> NotificationDotMode.NONE
                            }
                            onSelectDotMode(newMode)
                        },
                        label = { Text("App Icons") },
                        leadingIcon = if (showOnIcons) {
                            { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        shape = RoundedCornerShape(12.dp)
                    )

                    FilterChip(
                        selected = showOnRail,
                        onClick = {
                            val newMode = when {
                                !showOnRail && showOnIcons -> NotificationDotMode.BOTH
                                !showOnRail && !showOnIcons -> NotificationDotMode.CATEGORY_BAR
                                showOnRail && showOnIcons -> NotificationDotMode.APP_ICON
                                else -> NotificationDotMode.NONE
                            }
                            onSelectDotMode(newMode)
                        },
                        label = { Text("Category Rail") },
                        leadingIcon = if (showOnRail) {
                            { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Choose where you want to see notification counts. Changes apply instantly.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done", fontWeight = FontWeight.Bold) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLabelSettingsDialog(
    labelMode: AppLabelMode,
    onDismiss: () -> Unit,
    onSelectLabelMode: (AppLabelMode) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FontDownload, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text("App Names")
            }
        },
        text = {
            Column {
                Text(
                    "Control the visibility of application labels across different areas of the launcher.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(24.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Display names on:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))

                        // CHIP GROUP FOR MODERN SELECTION
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val showOnHome = labelMode == AppLabelMode.HOME_ONLY || labelMode == AppLabelMode.BOTH
                            val showOnDrawer = labelMode == AppLabelMode.DRAWER_ONLY || labelMode == AppLabelMode.BOTH

                            FilterChip(
                                selected = showOnHome,
                                onClick = {
                                    val newMode = when {
                                        !showOnHome && showOnDrawer -> AppLabelMode.BOTH
                                        !showOnHome && !showOnDrawer -> AppLabelMode.HOME_ONLY
                                        showOnHome && showOnDrawer -> AppLabelMode.DRAWER_ONLY
                                        else -> AppLabelMode.NONE
                                    }
                                    onSelectLabelMode(newMode)
                                },
                                label = { Text("Home Screen") },
                                leadingIcon = if (showOnHome) {
                                    { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                shape = RoundedCornerShape(12.dp)
                            )

                            FilterChip(
                                selected = showOnDrawer,
                                onClick = {
                                    val newMode = when {
                                        !showOnDrawer && showOnHome -> AppLabelMode.BOTH
                                        !showOnDrawer && !showOnHome -> AppLabelMode.DRAWER_ONLY
                                        showOnDrawer && showOnHome -> AppLabelMode.HOME_ONLY
                                        else -> AppLabelMode.NONE
                                    }
                                    onSelectLabelMode(newMode)
                                },
                                label = { Text("App Drawer") },
                                leadingIcon = if (showOnDrawer) {
                                    { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Toggling these will instantly hide or show labels on your icons. You can mix and match for a cleaner workspace.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done", fontWeight = FontWeight.Bold) }
        }
    )
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text("About Pxl Launcher Neo")
            }
        },
        text = {
            Column {
                Text(
                    "Pxl Launcher Neo is an ultra-lightweight Android launcher optimized for responsiveness and clean organization.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Version: 1.0.0-neo", fontWeight = FontWeight.Bold)
                Text("Developer: SamiDev Studio")
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "This project is built with Jetpack Compose and modern Android architecture.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = { /* Open Privacy Policy URL */ },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Privacy Policy", style = MaterialTheme.typography.labelLarge)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

data class DialogOption(val label: String, val value: Any, val isSelected: Boolean)

@Composable
fun SelectionDialog(title: String, options: List<DialogOption>, onDismiss: () -> Unit, onSelect: (Any) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .selectable(selected = option.isSelected, onClick = { onSelect(option.value); onDismiss() })
                            .padding(vertical = 12.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = option.isSelected, onClick = null)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(text = option.label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        SectionHeader(title)
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
    )
}

@Composable
private fun ValueLabel(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
}

@Composable
fun QuickActionItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Surface(modifier = Modifier.size(48.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ToggleSettingsItem(icon: ImageVector, title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(
            checked = checked, 
            onCheckedChange = onCheckedChange,
            modifier = Modifier.scale(0.8f)
        )
    }
}

@Composable
fun SettingsItem(icon: ImageVector, title: String, onClick: (() -> Unit)? = null, trailing: @Composable (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (trailing != null) trailing()
        else if (onClick != null) Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.outline)
    }
}
