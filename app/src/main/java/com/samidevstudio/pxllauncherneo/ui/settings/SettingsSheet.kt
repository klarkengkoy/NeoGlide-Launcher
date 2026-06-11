package com.samidevstudio.pxllauncherneo.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samidevstudio.pxllauncherneo.data.repository.*
import com.samidevstudio.pxllauncherneo.ui.components.AppIcon
import com.samidevstudio.pxllauncherneo.ui.utils.HapticEngine
import com.samidevstudio.pxllauncherneo.ui.utils.rememberHapticFeedback
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    onDismiss: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val preferences by viewModel.userPreferences.collectAsStateWithLifecycle()
    val hapticFeedback = rememberHapticFeedback(preferences)
    val isNotifEnabled by viewModel.isNotificationServiceEnabled.collectAsStateWithLifecycle()
    val isAuthForHidden by viewModel.isUserAuthenticatedForHiddenApps.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    // Reset authentication when the settings sheet is closed
    DisposableEffect(Unit) {
        onDispose {
            viewModel.setUserAuthenticatedForHiddenApps(authenticated = false)
        }
    }

    var activeDialog by remember { mutableStateOf<String?>(null) }
    var pendingCategoryBarType by remember { mutableStateOf<CategoryBarType?>(null) }

    val showHiddenAppsWithAuth = {
        if (isAuthForHidden) {
            activeDialog = "hidden_apps"
        } else {
            BiometricHelper.showBiometricPrompt(
                activity = context as FragmentActivity,
                onSuccess = {
                    viewModel.setUserAuthenticatedForHiddenApps(true)
                    activeDialog = "hidden_apps"
                },
                onNoSecurityEnrolled = {
                    activeDialog = "security_warning"
                }
            )
        }
    }

    // DIALOG DISPATCHER
    when (activeDialog) {
        "security_warning" -> AlertDialog(
            onDismissRequest = { activeDialog = null },
            title = { Text("Unprotected Vault") },
            text = { Text("Your device has no lock set. Anyone can access this vault. Are you sure you want to continue without security?") },
            confirmButton = {
                TextButton(onClick = { 
                    viewModel.setUserAuthenticatedForHiddenApps(true)
                    activeDialog = "hidden_apps" 
                }) { Text("Continue") }
            },
            dismissButton = {
                TextButton(onClick = {
                    try {
                        val intent = android.content.Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS)
                        context.startActivity(intent)
                    } catch (_: Exception) {}
                    activeDialog = null
                }) { Text("Set up Lock") }
            }
        )
        "category" -> SelectionDialog(
            title = "Category bar",
            description = "Change the position or visibility of the category selection rail.",
            options = listOf(
                DialogOption("Left side", CategoryBarType.LEFT, preferences.categoryBarType == CategoryBarType.LEFT),
                DialogOption("Right side", CategoryBarType.RIGHT, preferences.categoryBarType == CategoryBarType.RIGHT),
                DialogOption("Bottom bar", CategoryBarType.BOTTOM, preferences.categoryBarType == CategoryBarType.BOTTOM),
                DialogOption("Hidden", CategoryBarType.NONE, preferences.categoryBarType == CategoryBarType.NONE)
            ),
            onDismiss = { activeDialog = null },
            autoDismiss = false,
            onSelect = { 
                val newType = it as CategoryBarType
                if (newType == CategoryBarType.NONE) {
                    pendingCategoryBarType = newType
                    activeDialog = "category_hide_warning"
                } else {
                    viewModel.setCategoryBarType(newType)
                    activeDialog = null
                }
            }
        )
        "anchor" -> AnchorSettingsDialog(
            verticalAnchor = preferences.verticalAnchor,
            horizontalAnchor = preferences.horizontalAnchor,
            onDismiss = { activeDialog = null },
            onSelectVertical = { viewModel.setVerticalAnchor(it) },
            onSelectHorizontal = { viewModel.setHorizontalAnchor(it) }
        )
        "category_hide_warning" -> AlertDialog(
            onDismissRequest = { activeDialog = null },
            title = { Text("Hide Category Bar?") },
            text = { Text("Hiding the category bar will group all your apps into a single list. You won't be able to filter by category until you re-enable it.") },
            confirmButton = {
                TextButton(onClick = { 
                    pendingCategoryBarType?.let { viewModel.setCategoryBarType(it) }
                    activeDialog = null 
                }) { Text("Hide") }
            },
            dismissButton = {
                TextButton(onClick = { activeDialog = "category" }) { Text("Cancel") }
            }
        )
        "sorting" -> SelectionDialog(
            title = "Sorting",
            description = "Change the order in which apps appear in the drawer.",
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
            description = "Customize the number of rows and columns on your home screen.",
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
            description = "Choose which engine to use for web search suggestions.",
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
        "hidden_apps" -> HiddenAppsDialog(
            onDismiss = { activeDialog = null },
            viewModel = viewModel
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
                        SettingsItem(
                            icon = Icons.Default.Anchor,
                            title = "Anchor",
                            onClick = { activeDialog = "anchor" },
                            trailing = { ValueLabel("${preferences.verticalAnchor.name.formatLabel()} / ${preferences.horizontalAnchor.name.formatLabel()}") }
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
                            onClick = { showHiddenAppsWithAuth() }
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
                            onHapticFeedback = hapticFeedback,
                            onCheckedChange = { viewModel.setLockLayout(it) }
                        )
                    }
                }

                item {
                    SettingsGroup(title = "SYSTEM & FEEDBACK") {
                        ToggleSettingsItem(
                            icon = Icons.Default.Vibration,
                            title = "Haptic feedback",
                            checked = preferences.hapticsEnabled,
                            onHapticFeedback = hapticFeedback,
                            onCheckedChange = { viewModel.setHapticsEnabled(it) }
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
                            title = "App labels",
                            onClick = { activeDialog = "label_settings" }
                        )
                        SettingsItem(
                            icon = Icons.Default.Wallpaper, 
                            title = "Wallpaper", 
                            onClick = { }
                        )
                        SettingsItem(
                            icon = Icons.Default.Palette, 
                            title = "Appearance", 
                            onClick = { }
                        )
                        SettingsItem(
                            icon = Icons.Default.Backup, 
                            title = "Backup", 
                            onClick = { }
                        )
                    }
                }

                item {
                    SettingsGroup(title = "OTHER") {
                        SettingsItem(
                            icon = Icons.Default.WorkspacePremium, 
                            title = "Premium features", 
                            onClick = { }, 
                            trailing = { Icon(Icons.Default.WorkspacePremium, null, tint = MaterialTheme.colorScheme.tertiary) }
                        )
                        SettingsItem(
                            icon = Icons.Default.VerifiedUser, 
                            title = "Security and privacy", 
                            onClick = { }
                        )
                        SettingsItem(
                            icon = Icons.Default.Build, 
                            title = "Troubleshooting", 
                            onClick = { activeDialog = "trouble" }
                        )
                        SettingsItem(
                            icon = Icons.Default.RateReview, 
                            title = "Review Pxl Launcher", 
                            onClick = { }
                        )
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
    val context = LocalContext.current

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiddenAppsDialog(
    onDismiss: () -> Unit,
    viewModel: SettingsViewModel
) {
    val preferences by viewModel.userPreferences.collectAsStateWithLifecycle()
    val allApps by viewModel.allApps.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf(com.samidevstudio.pxllauncherneo.ui.components.PickerSortMode.ALPHABETICAL) }

    val filteredApps = remember(allApps, searchQuery, sortMode, preferences.hiddenPackages) {
        val baseList = allApps.filter { it.label.contains(searchQuery, ignoreCase = true) }
        
        when (sortMode) {
            com.samidevstudio.pxllauncherneo.ui.components.PickerSortMode.ALPHABETICAL -> {
                baseList.sortedBy { it.label }
            }
            com.samidevstudio.pxllauncherneo.ui.components.PickerSortMode.RECENT -> {
                baseList.sortedByDescending { it.lastUsedTime }
            }
            else -> baseList.sortedBy { it.label }
        }
    }

    val (hiddenInFiltered, unhiddenInFiltered) = remember(filteredApps, preferences.hiddenPackages) {
        filteredApps.partition { it.packageName in preferences.hiddenPackages }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Manage Hidden Apps",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Manage apps that are hidden from the main drawer view.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search apps to hide...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        com.samidevstudio.pxllauncherneo.ui.components.PickerSortMode.ALPHABETICAL,
                        com.samidevstudio.pxllauncherneo.ui.components.PickerSortMode.RECENT
                    ).forEach { mode ->
                        FilterChip(
                            selected = sortMode == mode,
                            onClick = { sortMode = mode },
                            label = { Text(mode.label) },
                            leadingIcon = if (sortMode == mode) {
                                { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        },
        text = {
            Box(modifier = Modifier.heightIn(max = 450.dp)) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    if (hiddenInFiltered.isNotEmpty()) {
                        item(key = "header_hidden") {
                            Text(
                                text = "Hidden Apps",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp, start = 8.dp)
                            )
                        }
                        items(
                            items = hiddenInFiltered,
                            key = { "hidden_${it.packageName}" }
                        ) { app ->
                            HiddenAppPickerItem(
                                app = app,
                                isHidden = true,
                                onToggle = { viewModel.unhideApp(app.packageName) }
                            )
                        }
                        
                        if (unhiddenInFiltered.isNotEmpty()) {
                            item {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }

                    if (unhiddenInFiltered.isNotEmpty()) {
                        item(key = "header_available") {
                            Text(
                                text = "Available Apps",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp, start = 8.dp)
                            )
                        }
                        items(
                            items = unhiddenInFiltered,
                            key = { "available_${it.packageName}" }
                        ) { app ->
                            HiddenAppPickerItem(
                                app = app,
                                isHidden = false,
                                onToggle = { viewModel.hideApp(app.packageName) }
                            )
                        }
                    }

                    if (hiddenInFiltered.isEmpty() && unhiddenInFiltered.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No apps found",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done", fontWeight = FontWeight.Bold) }
        }
    )
}

@Composable
private fun HiddenAppPickerItem(
    app: com.samidevstudio.pxllauncherneo.domain.model.AppModel,
    isHidden: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Surface(
        onClick = { onToggle(!isHidden) },
        shape = RoundedCornerShape(12.dp),
        color = if (isHidden) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (isHidden) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        RoundedCornerShape(10.dp)
                    )
                    .padding(6.dp)
            ) {
                AppIcon(
                    packageName = app.packageName,
                    contentDescription = null
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = app.label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isHidden) FontWeight.Bold else FontWeight.Normal,
                color = if (isHidden) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Checkbox(
                checked = isHidden,
                onCheckedChange = onToggle
            )
        }
    }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnchorSettingsDialog(
    verticalAnchor: VerticalAnchor,
    horizontalAnchor: HorizontalAnchor,
    onDismiss: () -> Unit,
    onSelectVertical: (VerticalAnchor) -> Unit,
    onSelectHorizontal: (HorizontalAnchor) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Anchor, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Anchor Settings")
            }
        },
        text = {
            Column {
                Text(
                    "Control how your app grid fills the space. Vertical anchoring moves the entire stack, while horizontal anchoring shifts the alignment of the partial row.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Vertical Anchor:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VerticalAnchor.entries.forEach { anchor ->
                        FilterChip(
                            selected = verticalAnchor == anchor,
                            onClick = { onSelectVertical(anchor) },
                            label = { Text(anchor.name.formatLabel()) },
                            leadingIcon = if (verticalAnchor == anchor) {
                                { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Horizontal Anchor:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalAnchor.entries.forEach { anchor ->
                        FilterChip(
                            selected = horizontalAnchor == anchor,
                            onClick = { onSelectHorizontal(anchor) },
                            label = { Text(anchor.name.formatLabel()) },
                            leadingIcon = if (horizontalAnchor == anchor) {
                                { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done", fontWeight = FontWeight.Bold) }
        }
    )
}

data class DialogOption(val label: String, val value: Any, val isSelected: Boolean)

@Composable
fun SelectionDialog(
    title: String,
    description: String? = null,
    options: List<DialogOption>,
    onDismiss: () -> Unit,
    autoDismiss: Boolean = true,
    onSelect: (Any) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                if (description != null) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .selectable(
                                selected = option.isSelected,
                                onClick = { 
                                    onSelect(option.value)
                                    if (autoDismiss) onDismiss()
                                }
                            )
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
fun ToggleSettingsItem(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    description: String? = null,
    onHapticFeedback: (HapticEngine.HapticType) -> Unit = {},
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { 
                onHapticFeedback(HapticEngine.HapticType.TOGGLE)
                onCheckedChange(!checked) 
            }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked, 
            onCheckedChange = {
                onHapticFeedback(HapticEngine.HapticType.TOGGLE)
                onCheckedChange(it)
            },
            modifier = Modifier.scale(0.8f)
        )
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector, 
    title: String, 
    description: String? = null,
    onClick: (() -> Unit)? = null, 
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (trailing != null) trailing()
        else if (onClick != null) Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.outline)
    }
}
