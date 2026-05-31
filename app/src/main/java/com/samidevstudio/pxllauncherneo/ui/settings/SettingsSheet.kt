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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val context = androidx.compose.ui.platform.LocalContext.current

    var activeDialog by remember { mutableStateOf<String?>(null) }

    // DIALOG DISPATCHER
    when (activeDialog) {
        "category" -> SelectionDialog(
            title = "Category bar",
            options = listOf(
                DialogOption("Left handed", CategoryBarType.LEFT, preferences.categoryBarType == CategoryBarType.LEFT),
                DialogOption("Right handed", CategoryBarType.RIGHT, preferences.categoryBarType == CategoryBarType.RIGHT),
                DialogOption("Bottom priority", CategoryBarType.BOTTOM, preferences.categoryBarType == CategoryBarType.BOTTOM),
                DialogOption("None", CategoryBarType.NONE, preferences.categoryBarType == CategoryBarType.NONE)
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
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
            Text(
                text = "Launcher Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                item { SectionHeader("APPLICATION DRAWER") }
                item {
                    SettingsItem(
                        icon = Icons.Default.ViewStream,
                        title = "Category bar",
                        onClick = { activeDialog = "category" },
                        trailing = { ValueLabel(preferences.categoryBarType.name.formatLabel()) }
                    )
                }
                item {
                    SettingsItem(
                        icon = Icons.Default.SortByAlpha,
                        title = "Sorting",
                        onClick = { activeDialog = "sorting" },
                        trailing = { ValueLabel(preferences.sortingMode.name.formatLabel()) }
                    )
                }
                item {
                    SettingsItem(
                        icon = Icons.Default.Circle,
                        title = "Notification dots",
                        onClick = { activeDialog = "notif" },
                        trailing = { ValueLabel(preferences.notificationDotMode.name.formatLabel()) }
                    )
                }
                item {
                    SettingsItem(
                        icon = Icons.Default.VisibilityOff,
                        title = "Hidden apps",
                        onClick = { /* TODO */ }
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        QuickActionItem(Icons.Default.Add, "Add shortcut") { }
                        QuickActionItem(Icons.Default.CreateNewFolder, "Add folder") { }
                        QuickActionItem(Icons.Default.Category, "Add category") { }
                    }
                }

                item { Divider() }
                item { SectionHeader("GLOBAL") }
                item { SettingsItem(Icons.Default.Wallpaper, "Wallpaper", onClick = { }) }
                item { SettingsItem(Icons.Default.Palette, "Global appearance", onClick = { }) }
                item { 
                    SettingsItem(
                        icon = Icons.Default.Gesture, 
                        title = "Gestures and hot keys", 
                        onClick = { 
                            android.widget.Toast.makeText(context, "Coming Soon", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    ) 
                }
                item { SettingsItem(Icons.Default.Backup, "Backup", onClick = { }) }

                item { Spacer(modifier = Modifier.height(16.dp)) }
                item { SectionHeader("PAGES") }
                item { 
                    SettingsItem(
                        icon = Icons.Default.Smartphone, 
                        title = "Home screen", 
                        onClick = { activeDialog = "grid" },
                        trailing = { ValueLabel(preferences.gridSize.name.split("_").last()) }
                    ) 
                }
                item {
                    ToggleSettingsItem(
                        icon = Icons.Default.Lock,
                        title = "Lock layout",
                        checked = preferences.lockLayout,
                        onCheckedChange = { viewModel.setLockLayout(it) }
                    )
                }
                item { 
                    SettingsItem(
                        icon = Icons.Default.Search, 
                        title = "Search provider", 
                        onClick = { activeDialog = "search" },
                        trailing = { ValueLabel(preferences.searchProvider.name.formatLabel()) }
                    ) 
                }
                item { SettingsItem(Icons.Default.Layers, "Page manager", onClick = { }, trailing = { ProBadge() }) }

                item { Spacer(modifier = Modifier.height(16.dp)) }
                item { SectionHeader("OTHER") }
                item { SettingsItem(Icons.Default.Star, "Premium features", onClick = { }, trailing = { Icon(Icons.Default.WorkspacePremium, null, tint = MaterialTheme.colorScheme.tertiary) }) }
                item { SettingsItem(Icons.Default.VerifiedUser, "Security and privacy", onClick = { }) }
                item { SettingsItem(Icons.Default.Build, "Troubleshooting", onClick = { activeDialog = "trouble" }) }
                item { SettingsItem(Icons.Default.RateReview, "Support us with a review", onClick = { }) }
                item {
                    SettingsItem(
                        icon = Icons.Default.Info,
                        title = "About Pxl Launcher",
                        onClick = { activeDialog = "about" },
                        trailing = { Text("v1.0-neo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    )
                }
                item { Spacer(modifier = Modifier.height(48.dp)) }
            }
        }
    }
}

private fun String.formatLabel(): String = this.replace("_", " ")
    .lowercase(Locale.getDefault())
    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

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
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
    )
}

@Composable
private fun ValueLabel(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun Divider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    )
}

@Composable
private fun ProBadge() {
    Text(
        text = "PRO",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.tertiary,
        modifier = Modifier.background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape).padding(horizontal = 8.dp, vertical = 2.dp)
    )
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
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 10.dp), // Adjusted horizontal padding to account for outer padding
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(
            checked = checked, 
            onCheckedChange = onCheckedChange,
            modifier = Modifier.scale(0.85f) // Scale down the switch slightly to feel less "bulky"
        )
    }
}

@Composable
fun SettingsItem(icon: ImageVector, title: String, onClick: (() -> Unit)? = null, trailing: @Composable (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (trailing != null) trailing()
        else if (onClick != null) Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.outline)
    }
}
